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
	.file "svml_d_log1p.c"
	.text
..TXTST0:
.L_2__routine_start___svml_log1p4_ha_l9_0:
# -- Begin  __svml_log1p4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p4_ha_l9
# --- __svml_log1p4_ha_l9(__m256d)
__svml_log1p4_ha_l9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1244.1
        .byte     15                                            #1365.552
        .byte     30                                            #1365.552
        .byte     250                                           #1365.552
	.cfi_startproc
..___tag_value___svml_log1p4_ha_l9.1:
..L2:
                                                          #1244.1
        pushq     %rbp                                          #1244.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1244.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1244.1
        subq      $192, %rsp                                    #1244.1
        lea       -8454144+__svml_dlog1p_ha_data_internal(%rip), %r8 #1338.68
        vmovupd   13056+__svml_dlog1p_ha_data_internal(%rip), %ymm15 #1297.52
        vmovupd   12992+__svml_dlog1p_ha_data_internal(%rip), %ymm10 #1296.44
        vmovupd   13632+__svml_dlog1p_ha_data_internal(%rip), %ymm5 #1317.17
        vmovapd   %ymm0, %ymm12                                 #1244.1
        vandpd    %ymm15, %ymm12, %ymm13                        #1302.15
        vcmplt_oqpd 13120+__svml_dlog1p_ha_data_internal(%rip), %ymm13, %ymm14 #1303.16
        vaddpd    %ymm10, %ymm12, %ymm3                         #1300.8
        vmaxpd    %ymm12, %ymm10, %ymm8                         #1301.8
        vminpd    %ymm12, %ymm10, %ymm9                         #1306.8
        vorpd     13184+__svml_dlog1p_ha_data_internal(%rip), %ymm14, %ymm0 #1304.17
        vandpd    %ymm0, %ymm3, %ymm7                           #1307.12
        vandpd    12672+__svml_dlog1p_ha_data_internal(%rip), %ymm7, %ymm1 #1312.15
        vsubpd    %ymm7, %ymm8, %ymm4                           #1308.9
        vcmplt_oqpd 12800+__svml_dlog1p_ha_data_internal(%rip), %ymm12, %ymm0 #1325.14
        vpsrlq    $20, %ymm7, %ymm14                            #1323.14
        vaddpd    %ymm4, %ymm9, %ymm9                           #1309.9
        vorpd     12736+__svml_dlog1p_ha_data_internal(%rip), %ymm1, %ymm8 #1313.15
        vcvtpd2ps %ymm8, %xmm2                                  #1318.40
        vcmpnle_uqpd 12864+__svml_dlog1p_ha_data_internal(%rip), %ymm12, %ymm1 #1326.14
        vrcpps    %xmm2, %xmm6                                  #1318.29
        vcvtps2pd %xmm6, %ymm13                                 #1318.13
        vandps    13568+__svml_dlog1p_ha_data_internal(%rip), %ymm7, %ymm7 #1320.13
        vpsubq    %ymm7, %ymm5, %ymm5                           #1321.13
        vroundpd  $0, %ymm13, %ymm6                             #1322.13
        vfmsub213pd %ymm10, %ymm6, %ymm8                        #1327.13
        vmulpd    %ymm5, %ymm6, %ymm4                           #1329.14
        vorpd     %ymm1, %ymm0, %ymm10                          #1331.14
        vpsrlq    $39, %ymm6, %ymm0                             #1335.18
        vmulpd    %ymm4, %ymm9, %ymm4                           #1344.13
        vmovupd   12544+__svml_dlog1p_ha_data_internal(%rip), %ymm9 #1350.58
        vandnpd   %ymm12, %ymm15, %ymm11                        #1305.19
        vmovmskpd %ymm10, %eax                                  #1333.40
        vextractf128 $1, %ymm0, %xmm5                           #1338.567
        vmovd     %xmm0, %edx                                   #1338.145
        vmovd     %xmm5, %esi                                   #1338.548
        vpextrd   $2, %xmm0, %ecx                               #1338.345
        vpextrd   $2, %xmm5, %edi                               #1338.753
        movslq    %edx, %rdx                                    #1338.68
        movslq    %ecx, %rcx                                    #1338.268
        movslq    %esi, %rsi                                    #1338.471
        movslq    %edi, %rdi                                    #1338.676
        vmovupd   (%r8,%rdx), %xmm2                             #1338.68
        vmovupd   (%r8,%rcx), %xmm10                            #1338.268
        vmovupd   (%r8,%rsi), %xmm1                             #1338.471
        vmovupd   (%r8,%rdi), %xmm13                            #1338.676
        vunpcklpd %xmm10, %xmm2, %xmm6                          #1338.881
        vunpckhpd %xmm10, %xmm2, %xmm2                          #1338.1008
        vunpcklpd %xmm13, %xmm1, %xmm7                          #1338.914
        vunpckhpd %xmm13, %xmm1, %xmm1                          #1338.1041
        vextractf128 $1, %ymm14, %xmm15                         #1324.125
        vshufps   $221, %xmm15, %xmm14, %xmm3                   #1324.33
        vaddpd    %ymm4, %ymm8, %ymm15                          #1345.12
        vcvtdq2pd %xmm3, %ymm3                                  #1330.20
        vsubpd    %ymm8, %ymm15, %ymm8                          #1346.14
        vfmadd213pd 12608+__svml_dlog1p_ha_data_internal(%rip), %ymm15, %ymm9 #1353.14
        vinsertf128 $1, %xmm1, %ymm2, %ymm14                    #1338.962
        vsubpd    %ymm8, %ymm4, %ymm1                           #1347.13
        vmovupd   12416+__svml_dlog1p_ha_data_internal(%rip), %ymm4 #1348.58
        vmulpd    %ymm15, %ymm15, %ymm2                         #1354.13
        vfmadd213pd 12480+__svml_dlog1p_ha_data_internal(%rip), %ymm15, %ymm4 #1352.14
        vfmadd213pd %ymm9, %ymm2, %ymm4                         #1355.12
        vinsertf128 $1, %xmm7, %ymm6, %ymm0                     #1338.835
        vfmadd231pd 13248+__svml_dlog1p_ha_data_internal(%rip), %ymm3, %ymm0 #1342.13
        vfmadd132pd 13312+__svml_dlog1p_ha_data_internal(%rip), %ymm14, %ymm3 #1343.13
        vaddpd    %ymm15, %ymm0, %ymm5                          #1356.17
        vaddpd    %ymm1, %ymm3, %ymm3                           #1359.13
        vsubpd    %ymm0, %ymm5, %ymm0                           #1357.13
        vfmadd213pd %ymm3, %ymm2, %ymm4                         #1360.12
        vsubpd    %ymm0, %ymm15, %ymm15                         #1358.14
        vaddpd    %ymm4, %ymm15, %ymm0                          #1361.13
        vaddpd    %ymm0, %ymm5, %ymm1                           #1362.14
        vorpd     %ymm11, %ymm1, %ymm0                          #1363.14
        testl     %eax, %eax                                    #1365.52
        jne       ..B1.3        # Prob 5%                       #1365.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm12
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1368.12
        popq      %rbp                                          #1368.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1368.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm12, 64(%rsp)                              #1365.200
        vmovupd   %ymm0, 128(%rsp)                              #1365.276
        je        ..B1.2        # Prob 95%                      #1365.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1365.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B1.13:                        # Preds ..B1.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1365.523
        jc        ..B1.10       # Prob 5%                       #1365.523
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1365.476
        cmpl      $4, %r12d                                     #1365.471
        jl        ..B1.7        # Prob 82%                      #1365.471
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1365.684
        jmp       ..B1.2        # Prob 100%                     #1365.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1365.552
        lea       128(%rsp,%r12,8), %rsi                        #1365.552
..___tag_value___svml_log1p4_ha_l9.19:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #1365.552
..___tag_value___svml_log1p4_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #1365.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p4_ha_l9,@function
	.size	__svml_log1p4_ha_l9,.-__svml_log1p4_ha_l9
..LN__svml_log1p4_ha_l9.0:
	.data
# -- End  __svml_log1p4_ha_l9
	.text
.L_2__routine_start___svml_log1p2_ha_ex_1:
# -- Begin  __svml_log1p2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p2_ha_ex
# --- __svml_log1p2_ha_ex(__m128d)
__svml_log1p2_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1373.1
        .byte     15                                            #1494.546
        .byte     30                                            #1494.546
        .byte     250                                           #1494.546
	.cfi_startproc
..___tag_value___svml_log1p2_ha_ex.22:
..L23:
                                                         #1373.1
        pushq     %rbp                                          #1373.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1373.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1373.1
        subq      $192, %rsp                                    #1373.1
        movaps    %xmm0, %xmm8                                  #1373.1
        movups    13056+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #1426.49
        movaps    %xmm8, %xmm1                                  #1454.14
        movaps    %xmm7, %xmm9                                  #1431.15
        movaps    %xmm8, %xmm2                                  #1455.14
        movups    12992+__svml_dlog1p_ha_data_internal(%rip), %xmm3 #1425.41
        andps     %xmm8, %xmm9                                  #1431.15
        cmpltpd   13120+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #1432.16
        cmpltpd   12800+__svml_dlog1p_ha_data_internal(%rip), %xmm1 #1454.14
        cmpnlepd  12864+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1455.14
        movaps    %xmm3, %xmm4                                  #1429.8
        movaps    %xmm3, %xmm10                                 #1430.8
        addpd     %xmm8, %xmm4                                  #1429.8
        maxpd     %xmm8, %xmm10                                 #1430.8
        orps      13184+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #1433.17
        movaps    %xmm3, %xmm6                                  #1435.8
        movups    12672+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #1441.15
        andps     %xmm9, %xmm4                                  #1436.12
        andps     %xmm4, %xmm5                                  #1441.15
        lea       -8454144+__svml_dlog1p_ha_data_internal(%rip), %rsi #1467.67
        orps      12736+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #1442.15
        orps      %xmm2, %xmm1                                  #1460.14
        cvtpd2ps  %xmm5, %xmm11                                 #1447.53
        minpd     %xmm8, %xmm6                                  #1435.8
        subpd     %xmm4, %xmm10                                 #1437.9
        movmskpd  %xmm1, %edx                                   #1462.40
        movlhps   %xmm11, %xmm11                                #1447.37
        andnps    %xmm8, %xmm7                                  #1434.19
        rcpps     %xmm11, %xmm12                                #1447.26
        addpd     %xmm10, %xmm6                                 #1438.9
        cvtps2pd  %xmm12, %xmm9                                 #1447.13
        movups    .L_2il0floatpacket.31(%rip), %xmm14           #1451.103
        addpd     %xmm14, %xmm9                                 #1451.293
        subpd     %xmm14, %xmm9                                 #1451.343
        movdqu    13568+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #1449.11
        movdqu    13632+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #1450.11
        pand      %xmm4, %xmm13                                 #1449.11
        psubq     %xmm13, %xmm0                                 #1450.11
        mulpd     %xmm9, %xmm5                                  #1456.25
        mulpd     %xmm9, %xmm0                                  #1458.14
        subpd     %xmm3, %xmm5                                  #1456.13
        mulpd     %xmm0, %xmm6                                  #1473.13
        psrlq     $39, %xmm9                                    #1464.16
        psrlq     $20, %xmm4                                    #1452.12
        movd      %xmm9, %eax                                   #1467.144
        pshufd    $2, %xmm9, %xmm3                              #1467.331
        pshufd    $221, %xmm4, %xmm15                           #1453.13
        movd      %xmm3, %ecx                                   #1467.313
        movups    12544+__svml_dlog1p_ha_data_internal(%rip), %xmm14 #1482.26
        movups    13248+__svml_dlog1p_ha_data_internal(%rip), %xmm10 #1471.25
        movslq    %eax, %rax                                    #1467.67
        movslq    %ecx, %rcx                                    #1467.236
        cvtdq2pd  %xmm15, %xmm12                                #1459.20
        movups    (%rsi,%rax), %xmm2                            #1467.67
        movups    (%rsi,%rcx), %xmm4                            #1467.236
        movaps    %xmm2, %xmm1                                  #1467.397
        unpcklpd  %xmm4, %xmm1                                  #1467.397
        unpckhpd  %xmm4, %xmm2                                  #1467.438
        movaps    %xmm5, %xmm4                                  #1474.12
        addpd     %xmm6, %xmm4                                  #1474.12
        mulpd     %xmm12, %xmm10                                #1471.25
        mulpd     %xmm4, %xmm14                                 #1482.26
        addpd     %xmm10, %xmm1                                 #1471.13
        addpd     12608+__svml_dlog1p_ha_data_internal(%rip), %xmm14 #1482.14
        movaps    %xmm4, %xmm0                                  #1475.14
        movaps    %xmm4, %xmm3                                  #1483.13
        subpd     %xmm5, %xmm0                                  #1475.14
        mulpd     %xmm4, %xmm3                                  #1483.13
        subpd     %xmm0, %xmm6                                  #1476.13
        movups    12416+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #1481.26
        movaps    %xmm1, %xmm0                                  #1485.17
        mulpd     %xmm4, %xmm5                                  #1481.26
        addpd     %xmm4, %xmm0                                  #1485.17
        addpd     12480+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #1481.14
        mulpd     %xmm3, %xmm5                                  #1484.24
        movups    13312+__svml_dlog1p_ha_data_internal(%rip), %xmm11 #1470.45
        movaps    %xmm0, %xmm13                                 #1486.13
        mulpd     %xmm11, %xmm12                                #1472.25
        addpd     %xmm5, %xmm14                                 #1484.12
        addpd     %xmm12, %xmm2                                 #1472.13
        subpd     %xmm1, %xmm13                                 #1486.13
        mulpd     %xmm14, %xmm3                                 #1489.24
        addpd     %xmm6, %xmm2                                  #1488.13
        subpd     %xmm13, %xmm4                                 #1487.14
        addpd     %xmm3, %xmm2                                  #1489.12
        addpd     %xmm2, %xmm4                                  #1490.13
        addpd     %xmm4, %xmm0                                  #1491.14
        orps      %xmm7, %xmm0                                  #1492.14
        testl     %edx, %edx                                    #1494.52
        jne       ..B2.3        # Prob 5%                       #1494.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1497.12
        popq      %rbp                                          #1497.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1497.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm8, 64(%rsp)                               #1494.197
        movups    %xmm0, 128(%rsp)                              #1494.270
                                # LOE rbx r12 r13 r14 r15 edx
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1494.454
        movq      %r12, 8(%rsp)                                 #1494.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1494.454
        movq      %r13, (%rsp)                                  #1494.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1494.454
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1494.517
        jc        ..B2.10       # Prob 5%                       #1494.517
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1494.470
        cmpl      $2, %r12d                                     #1494.465
        jl        ..B2.7        # Prob 82%                      #1494.465
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1494.675
        jmp       ..B2.2        # Prob 100%                     #1494.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1494.546
        lea       128(%rsp,%r12,8), %rsi                        #1494.546
..___tag_value___svml_log1p2_ha_ex.40:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #1494.546
..___tag_value___svml_log1p2_ha_ex.41:
        jmp       ..B2.8        # Prob 100%                     #1494.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p2_ha_ex,@function
	.size	__svml_log1p2_ha_ex,.-__svml_log1p2_ha_ex
..LN__svml_log1p2_ha_ex.1:
	.data
# -- End  __svml_log1p2_ha_ex
	.text
.L_2__routine_start___svml_log1p1_ha_l9_2:
# -- Begin  __svml_log1p1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p1_ha_l9
# --- __svml_log1p1_ha_l9(__m128d)
__svml_log1p1_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1502.1
        .byte     15                                            #1623.546
        .byte     30                                            #1623.546
        .byte     250                                           #1623.546
	.cfi_startproc
..___tag_value___svml_log1p1_ha_l9.43:
..L44:
                                                         #1502.1
        pushq     %rbp                                          #1502.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1502.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1502.1
        subq      $128, %rsp                                    #1502.1
        vmovapd   %xmm0, %xmm11                                 #1502.1
        vmovsd    13056+__svml_dlog1p_ha_data_internal(%rip), %xmm15 #1555.18
        lea       -8454136+__svml_dlog1p_ha_data_internal(%rip), %rdx #1596.17
        vandpd    %xmm15, %xmm11, %xmm12                        #1560.15
        vandnpd   %xmm11, %xmm15, %xmm9                         #1563.19
        vmovsd    12992+__svml_dlog1p_ha_data_internal(%rip), %xmm10 #1554.10
        vmovsd    13184+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #1557.17
        vaddsd    %xmm11, %xmm10, %xmm6                         #1558.8
        vcmpltsd  13120+__svml_dlog1p_ha_data_internal(%rip), %xmm12, %xmm14 #1561.16
        vorpd     %xmm14, %xmm13, %xmm7                         #1562.17
        vmovsd    12672+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #1568.14
        vandpd    %xmm7, %xmm6, %xmm8                           #1565.12
        vmovsd    12736+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #1569.12
        vandpd    %xmm5, %xmm8, %xmm12                          #1570.15
        vorpd     %xmm13, %xmm12, %xmm6                         #1571.15
        vcmpltsd  12800+__svml_dlog1p_ha_data_internal(%rip), %xmm11, %xmm5 #1583.14
        vcvtpd2ps %xmm6, %xmm14                                 #1576.53
        vunpcklpd %xmm10, %xmm10, %xmm1                         #1559.20
        vunpcklpd %xmm11, %xmm11, %xmm0                         #1559.73
        vmovlhps  %xmm14, %xmm14, %xmm15                        #1576.37
        vmaxpd    %xmm0, %xmm1, %xmm2                           #1559.8
        vminpd    %xmm0, %xmm1, %xmm3                           #1564.8
        vrcpps    %xmm15, %xmm1                                 #1576.26
        vsubsd    %xmm8, %xmm2, %xmm4                           #1566.9
        vcvtps2pd %xmm1, %xmm0                                  #1576.13
        vaddsd    %xmm3, %xmm4, %xmm7                           #1567.9
        vcmpnlesd 12864+__svml_dlog1p_ha_data_internal(%rip), %xmm11, %xmm3 #1584.14
        vmovq     13568+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1574.17
        vorpd     %xmm3, %xmm5, %xmm3                           #1589.14
        vroundsd  $0, %xmm0, %xmm0, %xmm4                       #1580.13
        vmovq     13632+__svml_dlog1p_ha_data_internal(%rip), %xmm12 #1575.17
        vfmsub213sd %xmm10, %xmm4, %xmm6                        #1585.13
        vpand     %xmm2, %xmm8, %xmm10                          #1578.13
        vpsubq    %xmm10, %xmm12, %xmm1                         #1579.13
        vpsrlq    $20, %xmm8, %xmm8                             #1581.14
        vmulsd    %xmm4, %xmm1, %xmm2                           #1587.14
        vpsrlq    $39, %xmm4, %xmm4                             #1593.18
        vmovd     %xmm4, %eax                                   #1596.117
        vmovmskpd %xmm3, %ecx                                   #1591.40
        vpshufd   $85, %xmm8, %xmm1                             #1582.15
        vpshufd   $0, %xmm1, %xmm0                              #1588.37
        vmulsd    %xmm2, %xmm7, %xmm2                           #1602.13
        vcvtdq2pd %xmm0, %xmm1                                  #1588.20
        vaddsd    %xmm2, %xmm6, %xmm12                          #1603.12
        movslq    %eax, %rax                                    #1596.106
        vmovapd   %xmm1, %xmm0                                  #1600.13
        vmovsd    13248+__svml_dlog1p_ha_data_internal(%rip), %xmm8 #1598.14
        vsubsd    %xmm6, %xmm12, %xmm6                          #1604.14
        vmulsd    %xmm12, %xmm12, %xmm15                        #1612.13
        vfmadd213sd -8(%rdx,%rax), %xmm8, %xmm0                 #1600.13
        vsubsd    %xmm6, %xmm2, %xmm14                          #1605.13
        vaddsd    %xmm12, %xmm0, %xmm3                          #1614.17
        vmovsd    (%rdx,%rax), %xmm5                            #1596.106
        vsubsd    %xmm0, %xmm3, %xmm13                          #1615.13
        vunpcklpd %xmm5, %xmm5, %xmm10                          #1596.162
        vsubsd    %xmm13, %xmm12, %xmm0                         #1616.14
        vfmadd132sd 13312+__svml_dlog1p_ha_data_internal(%rip), %xmm10, %xmm1 #1601.13
        vmovsd    12416+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1606.24
        vmovsd    12544+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #1608.24
        vaddsd    %xmm14, %xmm1, %xmm1                          #1617.13
        vfmadd213sd 12480+__svml_dlog1p_ha_data_internal(%rip), %xmm12, %xmm2 #1610.14
        vfmadd213sd 12608+__svml_dlog1p_ha_data_internal(%rip), %xmm12, %xmm7 #1611.14
        vfmadd213sd %xmm7, %xmm15, %xmm2                        #1613.12
        vfmadd213sd %xmm1, %xmm15, %xmm2                        #1618.12
        vaddsd    %xmm2, %xmm0, %xmm12                          #1619.13
        vaddsd    %xmm3, %xmm12, %xmm4                          #1620.14
        vorpd     %xmm9, %xmm4, %xmm0                           #1621.14
        andl      $1, %ecx                                      #1591.90
        jne       ..B3.3        # Prob 5%                       #1623.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm11
..B3.2:                         # Preds ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1626.12
        popq      %rbp                                          #1626.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1626.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm11, (%rsp)                                #1623.160
        vmovsd    %xmm0, 64(%rsp)                               #1623.233
        jne       ..B3.5        # Prob 5%                       #1623.374
                                # LOE rbx r12 r13 r14 r15 ecx
..B3.4:                         # Preds ..B3.6 ..B3.5 ..B3.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1623.627
        movq      %rbp, %rsp                                    #1623.627
        popq      %rbp                                          #1623.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1623.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.5:                         # Preds ..B3.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B3.4        # Prob 95%                      #1623.517
                                # LOE rbx r12 r13 r14 r15
..B3.6:                         # Preds ..B3.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1623.546
        lea       64(%rsp), %rsi                                #1623.546
..___tag_value___svml_log1p1_ha_l9.56:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #1623.546
..___tag_value___svml_log1p1_ha_l9.57:
        jmp       ..B3.4        # Prob 100%                     #1623.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log1p1_ha_l9,@function
	.size	__svml_log1p1_ha_l9,.-__svml_log1p1_ha_l9
..LN__svml_log1p1_ha_l9.2:
	.data
# -- End  __svml_log1p1_ha_l9
	.text
.L_2__routine_start___svml_log1p1_ha_e9_3:
# -- Begin  __svml_log1p1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p1_ha_e9
# --- __svml_log1p1_ha_e9(__m128d)
__svml_log1p1_ha_e9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1631.1
        .byte     15                                            #1752.546
        .byte     30                                            #1752.546
        .byte     250                                           #1752.546
	.cfi_startproc
..___tag_value___svml_log1p1_ha_e9.59:
..L60:
                                                         #1631.1
        pushq     %rbp                                          #1631.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1631.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1631.1
        subq      $128, %rsp                                    #1631.1
        movaps    %xmm0, %xmm6                                  #1631.1
        movsd     13056+__svml_dlog1p_ha_data_internal(%rip), %xmm4 #1684.18
        movaps    %xmm6, %xmm8                                  #1688.73
        movaps    %xmm4, %xmm7                                  #1689.15
        lea       -8454136+__svml_dlog1p_ha_data_internal(%rip), %rdx #1725.17
        movsd     12992+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #1683.10
        andps     %xmm6, %xmm7                                  #1689.15
        movaps    %xmm0, %xmm3                                  #1687.8
        movaps    %xmm0, %xmm10                                 #1688.20
        movsd     13184+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #1691.17
        andnps    %xmm6, %xmm4                                  #1692.19
        movsd     12672+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1699.15
        addsd     %xmm6, %xmm3                                  #1687.8
        cmpltsd   13120+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #1690.16
        orps      %xmm7, %xmm9                                  #1691.17
        movaps    %xmm6, %xmm7                                  #1713.14
        andps     %xmm9, %xmm3                                  #1694.12
        cmpnlesd  12864+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #1713.14
        movsd     12736+__svml_dlog1p_ha_data_internal(%rip), %xmm11 #1698.12
        andps     %xmm3, %xmm2                                  #1699.15
        orps      %xmm11, %xmm2                                 #1700.15
        cvtpd2ps  %xmm2, %xmm12                                 #1705.53
        movlhps   %xmm12, %xmm12                                #1705.37
        rcpps     %xmm12, %xmm13                                #1705.26
        cvtps2pd  %xmm13, %xmm9                                 #1705.13
        unpcklpd  %xmm0, %xmm10                                 #1688.20
        unpcklpd  %xmm6, %xmm8                                  #1688.73
        movaps    %xmm10, %xmm5                                 #1688.8
        maxpd     %xmm8, %xmm5                                  #1688.8
        minpd     %xmm8, %xmm10                                 #1693.8
        subsd     %xmm3, %xmm5                                  #1695.9
        roundsd   $0, %xmm9, %xmm9                              #1709.13
        movaps    %xmm6, %xmm8                                  #1712.14
        mulsd     %xmm9, %xmm2                                  #1714.25
        addsd     %xmm10, %xmm5                                 #1696.9
        cmpltsd   12800+__svml_dlog1p_ha_data_internal(%rip), %xmm8 #1712.14
        subsd     %xmm0, %xmm2                                  #1714.13
        movq      13568+__svml_dlog1p_ha_data_internal(%rip), %xmm14 #1707.17
        orps      %xmm7, %xmm8                                  #1718.14
        movq      13632+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #1708.17
        pand      %xmm3, %xmm14                                 #1707.17
        psubq     %xmm14, %xmm0                                 #1708.17
        movmskpd  %xmm8, %ecx                                   #1720.40
        psrlq     $20, %xmm3                                    #1710.18
        pshufd    $85, %xmm3, %xmm15                            #1711.19
        mulsd     %xmm9, %xmm0                                  #1716.14
        psrlq     $39, %xmm9                                    #1722.22
        movd      %xmm9, %eax                                   #1725.117
        pshufd    $0, %xmm15, %xmm1                             #1717.37
        mulsd     %xmm0, %xmm5                                  #1731.13
        movaps    %xmm2, %xmm0                                  #1732.12
        cvtdq2pd  %xmm1, %xmm1                                  #1717.20
        addsd     %xmm5, %xmm0                                  #1732.12
        movsd     12416+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #1739.26
        movaps    %xmm1, %xmm10                                 #1729.25
        mulsd     %xmm0, %xmm13                                 #1739.26
        movaps    %xmm0, %xmm3                                  #1733.14
        subsd     %xmm2, %xmm3                                  #1733.14
        mulsd     13248+__svml_dlog1p_ha_data_internal(%rip), %xmm10 #1729.25
        mulsd     13312+__svml_dlog1p_ha_data_internal(%rip), %xmm1 #1730.25
        addsd     12480+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #1739.14
        subsd     %xmm3, %xmm5                                  #1734.13
        movslq    %eax, %rax                                    #1725.106
        movaps    %xmm0, %xmm12                                 #1741.13
        movsd     12544+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1740.26
        mulsd     %xmm0, %xmm2                                  #1740.26
        mulsd     %xmm0, %xmm12                                 #1741.13
        addsd     -8(%rdx,%rax), %xmm10                         #1729.13
        addsd     (%rdx,%rax), %xmm1                            #1730.13
        addsd     12608+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #1740.14
        mulsd     %xmm12, %xmm13                                #1742.24
        addsd     %xmm5, %xmm1                                  #1746.13
        addsd     %xmm2, %xmm13                                 #1742.12
        movaps    %xmm10, %xmm14                                #1743.17
        mulsd     %xmm12, %xmm13                                #1747.24
        addsd     %xmm0, %xmm14                                 #1743.17
        movaps    %xmm14, %xmm11                                #1744.13
        addsd     %xmm1, %xmm13                                 #1747.12
        subsd     %xmm10, %xmm11                                #1744.13
        subsd     %xmm11, %xmm0                                 #1745.14
        addsd     %xmm13, %xmm0                                 #1748.13
        addsd     %xmm14, %xmm0                                 #1749.14
        orps      %xmm4, %xmm0                                  #1750.14
        andl      $1, %ecx                                      #1720.90
        jne       ..B4.3        # Prob 5%                       #1752.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm6
..B4.2:                         # Preds ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1755.12
        popq      %rbp                                          #1755.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1755.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm6, (%rsp)                                 #1752.160
        movsd     %xmm0, 64(%rsp)                               #1752.233
        jne       ..B4.6        # Prob 5%                       #1752.374
                                # LOE rbx r12 r13 r14 r15
..B4.4:                         # Preds ..B4.6 ..B4.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1752.627
        movq      %rbp, %rsp                                    #1752.627
        popq      %rbp                                          #1752.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1752.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1752.546
        lea       64(%rsp), %rsi                                #1752.546
..___tag_value___svml_log1p1_ha_e9.72:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #1752.546
..___tag_value___svml_log1p1_ha_e9.73:
        jmp       ..B4.4        # Prob 100%                     #1752.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log1p1_ha_e9,@function
	.size	__svml_log1p1_ha_e9,.-__svml_log1p1_ha_e9
..LN__svml_log1p1_ha_e9.3:
	.data
# -- End  __svml_log1p1_ha_e9
	.text
.L_2__routine_start___svml_log1p4_ha_e9_4:
# -- Begin  __svml_log1p4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p4_ha_e9
# --- __svml_log1p4_ha_e9(__m256d)
__svml_log1p4_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1760.1
        .byte     15                                            #1881.552
        .byte     30                                            #1881.552
        .byte     250                                           #1881.552
	.cfi_startproc
..___tag_value___svml_log1p4_ha_e9.75:
..L76:
                                                         #1760.1
        pushq     %rbp                                          #1760.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1760.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1760.1
        subq      $192, %rsp                                    #1760.1
        lea       -8454144+__svml_dlog1p_ha_data_internal(%rip), %r8 #1854.68
        vmovupd   13056+__svml_dlog1p_ha_data_internal(%rip), %ymm8 #1813.52
        vmovupd   12992+__svml_dlog1p_ha_data_internal(%rip), %ymm1 #1812.44
        vmovupd   13568+__svml_dlog1p_ha_data_internal(%rip), %xmm4 #1832.21
        vmovupd   13632+__svml_dlog1p_ha_data_internal(%rip), %xmm6 #1833.21
        vmovapd   %ymm0, %ymm3                                  #1760.1
        vandpd    %ymm8, %ymm3, %ymm2                           #1818.15
        vcmplt_oqpd 13120+__svml_dlog1p_ha_data_internal(%rip), %ymm2, %ymm9 #1819.16
        vaddpd    %ymm1, %ymm3, %ymm7                           #1816.8
        vmaxpd    %ymm3, %ymm1, %ymm10                          #1817.8
        vminpd    %ymm3, %ymm1, %ymm11                          #1822.8
        vorpd     13184+__svml_dlog1p_ha_data_internal(%rip), %ymm9, %ymm15 #1820.17
        vandpd    %ymm15, %ymm7, %ymm14                         #1823.12
        vandpd    12672+__svml_dlog1p_ha_data_internal(%rip), %ymm14, %ymm13 #1828.15
        vorpd     12736+__svml_dlog1p_ha_data_internal(%rip), %ymm13, %ymm5 #1829.15
        vcvtpd2ps %ymm5, %xmm9                                  #1834.40
        vsubpd    %ymm14, %ymm10, %ymm12                        #1824.9
        vrcpps    %xmm9, %xmm9                                  #1834.29
        vaddpd    %ymm12, %ymm11, %ymm0                         #1825.9
        vcmplt_oqpd 12800+__svml_dlog1p_ha_data_internal(%rip), %ymm3, %ymm12 #1841.14
        vcmpnle_uqpd 12864+__svml_dlog1p_ha_data_internal(%rip), %ymm3, %ymm11 #1842.14
        vandnpd   %ymm3, %ymm8, %ymm2                           #1821.19
        vcvtps2pd %xmm9, %ymm8                                  #1834.13
        vroundpd  $0, %ymm8, %ymm15                             #1838.13
        vmulpd    %ymm15, %ymm5, %ymm5                          #1843.28
        vorpd     %ymm11, %ymm12, %ymm11                        #1847.14
        vsubpd    %ymm1, %ymm5, %ymm1                           #1843.13
        vpand     %xmm4, %xmm14, %xmm9                          #1836.17
        vextractf128 $1, %ymm14, %xmm13                         #1835.105
        vpsrlq    $20, %xmm14, %xmm7                            #1839.18
        vpsubq    %xmm9, %xmm6, %xmm14                          #1837.17
        vandps    %xmm4, %xmm13, %xmm4                          #1836.85
        vpsrlq    $20, %xmm13, %xmm10                           #1839.76
        vpsubq    %xmm4, %xmm6, %xmm6                           #1837.84
        vshufps   $221, %xmm10, %xmm7, %xmm10                   #1840.37
        vcvtdq2pd %xmm10, %ymm8                                 #1846.20
        vextractf128 $1, %ymm11, %xmm12                         #1848.113
        vshufps   $221, %xmm12, %xmm11, %xmm5                   #1849.57
        vmovmskps %xmm5, %eax                                   #1849.40
        vinsertf128 $1, %xmm6, %ymm14, %ymm13                   #1844.13
        vmulpd    %ymm13, %ymm15, %ymm9                         #1845.14
        vmulpd    %ymm9, %ymm0, %ymm0                           #1860.13
        vpsrlq    $39, %xmm15, %xmm4                            #1851.22
        vextractf128 $1, %ymm15, %xmm5                          #1850.110
        vmovd     %xmm4, %edx                                   #1854.145
        vpsrlq    $39, %xmm5, %xmm6                             #1851.81
        vmovd     %xmm6, %esi                                   #1854.504
        vpextrd   $2, %xmm4, %ecx                               #1854.323
        vpextrd   $2, %xmm6, %edi                               #1854.682
        movslq    %edx, %rdx                                    #1854.68
        movslq    %ecx, %rcx                                    #1854.246
        movslq    %esi, %rsi                                    #1854.427
        movslq    %edi, %rdi                                    #1854.605
        vmulpd    13248+__svml_dlog1p_ha_data_internal(%rip), %ymm8, %ymm4 #1858.28
        vmovupd   (%r8,%rdx), %xmm12                            #1854.68
        vmovupd   (%r8,%rcx), %xmm13                            #1854.246
        vmovupd   (%r8,%rsi), %xmm7                             #1854.427
        vmovupd   (%r8,%rdi), %xmm15                            #1854.605
        vunpcklpd %xmm13, %xmm12, %xmm10                        #1854.783
        vunpcklpd %xmm15, %xmm7, %xmm11                         #1854.816
        vmulpd    13312+__svml_dlog1p_ha_data_internal(%rip), %ymm8, %ymm8 #1859.28
        vunpckhpd %xmm13, %xmm12, %xmm14                        #1854.910
        vunpckhpd %xmm15, %xmm7, %xmm7                          #1854.943
        vinsertf128 $1, %xmm11, %ymm10, %ymm5                   #1854.737
        vaddpd    %ymm4, %ymm5, %ymm6                           #1858.13
        vaddpd    %ymm0, %ymm1, %ymm4                           #1861.12
        vsubpd    %ymm1, %ymm4, %ymm1                           #1862.14
        vaddpd    %ymm4, %ymm6, %ymm15                          #1872.17
        vmulpd    %ymm4, %ymm4, %ymm11                          #1870.13
        vsubpd    %ymm1, %ymm0, %ymm10                          #1863.13
        vsubpd    %ymm6, %ymm15, %ymm6                          #1873.13
        vmulpd    12416+__svml_dlog1p_ha_data_internal(%rip), %ymm4, %ymm0 #1868.29
        vmulpd    12544+__svml_dlog1p_ha_data_internal(%rip), %ymm4, %ymm1 #1869.29
        vsubpd    %ymm6, %ymm4, %ymm13                          #1874.14
        vinsertf128 $1, %xmm7, %ymm14, %ymm7                    #1854.864
        vaddpd    %ymm8, %ymm7, %ymm5                           #1859.13
        vaddpd    12480+__svml_dlog1p_ha_data_internal(%rip), %ymm0, %ymm7 #1868.14
        vaddpd    12608+__svml_dlog1p_ha_data_internal(%rip), %ymm1, %ymm8 #1869.14
        vaddpd    %ymm10, %ymm5, %ymm5                          #1875.13
        vmulpd    %ymm11, %ymm7, %ymm9                          #1871.27
        vaddpd    %ymm9, %ymm8, %ymm12                          #1871.12
        vmulpd    %ymm12, %ymm11, %ymm4                         #1876.27
        vaddpd    %ymm4, %ymm5, %ymm14                          #1876.12
        vaddpd    %ymm14, %ymm13, %ymm0                         #1877.13
        vaddpd    %ymm0, %ymm15, %ymm1                          #1878.14
        vorpd     %ymm2, %ymm1, %ymm0                           #1879.14
        testl     %eax, %eax                                    #1881.52
        jne       ..B5.3        # Prob 5%                       #1881.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm3
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1884.12
        popq      %rbp                                          #1884.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1884.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm3, 64(%rsp)                               #1881.200
        vmovupd   %ymm0, 128(%rsp)                              #1881.276
        je        ..B5.2        # Prob 95%                      #1881.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1881.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1881.523
        jc        ..B5.10       # Prob 5%                       #1881.523
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1881.476
        cmpl      $4, %r12d                                     #1881.471
        jl        ..B5.7        # Prob 82%                      #1881.471
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1881.684
        jmp       ..B5.2        # Prob 100%                     #1881.684
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1881.552
        lea       128(%rsp,%r12,8), %rsi                        #1881.552
..___tag_value___svml_log1p4_ha_e9.93:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #1881.552
..___tag_value___svml_log1p4_ha_e9.94:
        jmp       ..B5.8        # Prob 100%                     #1881.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p4_ha_e9,@function
	.size	__svml_log1p4_ha_e9,.-__svml_log1p4_ha_e9
..LN__svml_log1p4_ha_e9.4:
	.data
# -- End  __svml_log1p4_ha_e9
	.text
.L_2__routine_start___svml_log1p1_ha_ex_5:
# -- Begin  __svml_log1p1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p1_ha_ex
# --- __svml_log1p1_ha_ex(__m128d)
__svml_log1p1_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2193.1
        .byte     15                                            #2314.546
        .byte     30                                            #2314.546
        .byte     250                                           #2314.546
	.cfi_startproc
..___tag_value___svml_log1p1_ha_ex.96:
..L97:
                                                         #2193.1
        pushq     %rbp                                          #2193.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2193.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2193.1
        subq      $128, %rsp                                    #2193.1
        movaps    %xmm0, %xmm6                                  #2193.1
        movsd     13056+__svml_dlog1p_ha_data_internal(%rip), %xmm4 #2246.18
        movaps    %xmm6, %xmm8                                  #2250.73
        movaps    %xmm4, %xmm7                                  #2251.15
        lea       -8454136+__svml_dlog1p_ha_data_internal(%rip), %rdx #2287.17
        movsd     12992+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #2245.10
        andps     %xmm6, %xmm7                                  #2251.15
        movaps    %xmm0, %xmm3                                  #2249.8
        movaps    %xmm0, %xmm10                                 #2250.20
        movsd     13184+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #2253.17
        andnps    %xmm6, %xmm4                                  #2254.19
        movsd     12672+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #2261.15
        addsd     %xmm6, %xmm3                                  #2249.8
        cmpltsd   13120+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #2252.16
        orps      %xmm7, %xmm9                                  #2253.17
        movaps    %xmm6, %xmm7                                  #2274.14
        andps     %xmm9, %xmm3                                  #2256.12
        cmpltsd   12800+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #2274.14
        movsd     12736+__svml_dlog1p_ha_data_internal(%rip), %xmm11 #2260.12
        andps     %xmm3, %xmm2                                  #2261.15
        orps      %xmm11, %xmm2                                 #2262.15
        cvtpd2ps  %xmm2, %xmm12                                 #2267.53
        movlhps   %xmm12, %xmm12                                #2267.37
        rcpps     %xmm12, %xmm13                                #2267.26
        unpcklpd  %xmm0, %xmm10                                 #2250.20
        unpcklpd  %xmm6, %xmm8                                  #2250.73
        movaps    %xmm10, %xmm5                                 #2250.8
        maxpd     %xmm8, %xmm5                                  #2250.8
        minpd     %xmm8, %xmm10                                 #2255.8
        cvtps2pd  %xmm13, %xmm8                                 #2267.13
        subsd     %xmm3, %xmm5                                  #2257.9
        movups    .L_2il0floatpacket.31(%rip), %xmm14           #2271.103
        addsd     %xmm10, %xmm5                                 #2258.9
        addpd     %xmm14, %xmm8                                 #2271.293
        subpd     %xmm14, %xmm8                                 #2271.343
        mulsd     %xmm8, %xmm2                                  #2276.25
        movaps    %xmm6, %xmm14                                 #2275.14
        cmpnlesd  12864+__svml_dlog1p_ha_data_internal(%rip), %xmm14 #2275.14
        subsd     %xmm0, %xmm2                                  #2276.13
        movq      13568+__svml_dlog1p_ha_data_internal(%rip), %xmm15 #2269.11
        orps      %xmm14, %xmm7                                 #2280.14
        movq      13632+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #2270.11
        pand      %xmm3, %xmm15                                 #2269.11
        psubq     %xmm15, %xmm0                                 #2270.11
        movmskpd  %xmm7, %ecx                                   #2282.40
        psrlq     $20, %xmm3                                    #2272.12
        pshufd    $85, %xmm3, %xmm1                             #2273.13
        mulsd     %xmm8, %xmm0                                  #2278.14
        psrlq     $39, %xmm8                                    #2284.16
        movd      %xmm8, %eax                                   #2287.117
        pshufd    $0, %xmm1, %xmm3                              #2279.37
        mulsd     %xmm0, %xmm5                                  #2293.13
        movaps    %xmm2, %xmm0                                  #2294.12
        cvtdq2pd  %xmm3, %xmm1                                  #2279.20
        addsd     %xmm5, %xmm0                                  #2294.12
        movsd     12416+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #2301.26
        movaps    %xmm0, %xmm9                                  #2295.14
        mulsd     %xmm0, %xmm13                                 #2301.26
        subsd     %xmm2, %xmm9                                  #2295.14
        movsd     12544+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #2302.26
        movaps    %xmm1, %xmm10                                 #2291.25
        movaps    %xmm0, %xmm12                                 #2303.13
        addsd     12480+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #2301.14
        mulsd     13248+__svml_dlog1p_ha_data_internal(%rip), %xmm10 #2291.25
        subsd     %xmm9, %xmm5                                  #2296.13
        mulsd     %xmm0, %xmm2                                  #2302.26
        mulsd     %xmm0, %xmm12                                 #2303.13
        mulsd     13312+__svml_dlog1p_ha_data_internal(%rip), %xmm1 #2292.25
        addsd     12608+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #2302.14
        mulsd     %xmm12, %xmm13                                #2304.24
        movslq    %eax, %rax                                    #2287.106
        addsd     %xmm2, %xmm13                                 #2304.12
        addsd     -8(%rdx,%rax), %xmm10                         #2291.13
        mulsd     %xmm12, %xmm13                                #2309.24
        addsd     (%rdx,%rax), %xmm1                            #2292.13
        movaps    %xmm10, %xmm2                                 #2305.17
        addsd     %xmm5, %xmm1                                  #2308.13
        addsd     %xmm0, %xmm2                                  #2305.17
        addsd     %xmm1, %xmm13                                 #2309.12
        movaps    %xmm2, %xmm11                                 #2306.13
        subsd     %xmm10, %xmm11                                #2306.13
        subsd     %xmm11, %xmm0                                 #2307.14
        addsd     %xmm13, %xmm0                                 #2310.13
        addsd     %xmm2, %xmm0                                  #2311.14
        orps      %xmm4, %xmm0                                  #2312.14
        andl      $1, %ecx                                      #2282.90
        jne       ..B6.3        # Prob 5%                       #2314.52
                                # LOE rbx r12 r13 r14 r15 ecx xmm0 xmm6
..B6.2:                         # Preds ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2317.12
        popq      %rbp                                          #2317.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2317.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm6, (%rsp)                                 #2314.160
        movsd     %xmm0, 64(%rsp)                               #2314.233
        jne       ..B6.6        # Prob 5%                       #2314.374
                                # LOE rbx r12 r13 r14 r15
..B6.4:                         # Preds ..B6.6 ..B6.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #2314.627
        movq      %rbp, %rsp                                    #2314.627
        popq      %rbp                                          #2314.627
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2314.627
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #2314.546
        lea       64(%rsp), %rsi                                #2314.546
..___tag_value___svml_log1p1_ha_ex.109:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #2314.546
..___tag_value___svml_log1p1_ha_ex.110:
        jmp       ..B6.4        # Prob 100%                     #2314.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_log1p1_ha_ex,@function
	.size	__svml_log1p1_ha_ex,.-__svml_log1p1_ha_ex
..LN__svml_log1p1_ha_ex.5:
	.data
# -- End  __svml_log1p1_ha_ex
	.text
.L_2__routine_start___svml_log1p2_ha_e9_6:
# -- Begin  __svml_log1p2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p2_ha_e9
# --- __svml_log1p2_ha_e9(__m128d)
__svml_log1p2_ha_e9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2322.1
        .byte     15                                            #2443.546
        .byte     30                                            #2443.546
        .byte     250                                           #2443.546
	.cfi_startproc
..___tag_value___svml_log1p2_ha_e9.112:
..L113:
                                                        #2322.1
        pushq     %rbp                                          #2322.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2322.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2322.1
        subq      $192, %rsp                                    #2322.1
        movaps    %xmm0, %xmm8                                  #2322.1
        movups    13056+__svml_dlog1p_ha_data_internal(%rip), %xmm7 #2375.49
        movaps    %xmm8, %xmm1                                  #2403.14
        movaps    %xmm7, %xmm9                                  #2380.15
        movaps    %xmm8, %xmm2                                  #2404.14
        movups    12992+__svml_dlog1p_ha_data_internal(%rip), %xmm3 #2374.41
        andps     %xmm8, %xmm9                                  #2380.15
        cmpltpd   13120+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #2381.16
        cmpltpd   12800+__svml_dlog1p_ha_data_internal(%rip), %xmm1 #2403.14
        cmpnlepd  12864+__svml_dlog1p_ha_data_internal(%rip), %xmm2 #2404.14
        movaps    %xmm3, %xmm4                                  #2378.8
        movaps    %xmm3, %xmm10                                 #2379.8
        addpd     %xmm8, %xmm4                                  #2378.8
        maxpd     %xmm8, %xmm10                                 #2379.8
        orps      13184+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #2382.17
        movaps    %xmm3, %xmm6                                  #2384.8
        movups    12672+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #2390.15
        andps     %xmm9, %xmm4                                  #2385.12
        andps     %xmm4, %xmm5                                  #2390.15
        lea       -8454144+__svml_dlog1p_ha_data_internal(%rip), %rsi #2416.67
        orps      12736+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #2391.15
        orps      %xmm2, %xmm1                                  #2409.14
        cvtpd2ps  %xmm5, %xmm11                                 #2396.53
        minpd     %xmm8, %xmm6                                  #2384.8
        subpd     %xmm4, %xmm10                                 #2386.9
        movmskpd  %xmm1, %edx                                   #2411.40
        movlhps   %xmm11, %xmm11                                #2396.37
        andnps    %xmm8, %xmm7                                  #2383.19
        rcpps     %xmm11, %xmm12                                #2396.26
        addpd     %xmm10, %xmm6                                 #2387.9
        cvtps2pd  %xmm12, %xmm14                                #2396.13
        movdqu    13568+__svml_dlog1p_ha_data_internal(%rip), %xmm13 #2398.17
        movdqu    13632+__svml_dlog1p_ha_data_internal(%rip), %xmm0 #2399.17
        pand      %xmm4, %xmm13                                 #2398.17
        psubq     %xmm13, %xmm0                                 #2399.17
        roundpd   $0, %xmm14, %xmm14                            #2400.13
        psrlq     $20, %xmm4                                    #2401.18
        mulpd     %xmm14, %xmm0                                 #2407.14
        mulpd     %xmm14, %xmm5                                 #2405.25
        mulpd     %xmm0, %xmm6                                  #2422.13
        subpd     %xmm3, %xmm5                                  #2405.13
        psrlq     $39, %xmm14                                   #2413.22
        movd      %xmm14, %eax                                  #2416.144
        pshufd    $221, %xmm4, %xmm15                           #2402.19
        movups    12544+__svml_dlog1p_ha_data_internal(%rip), %xmm12 #2431.26
        movslq    %eax, %rax                                    #2416.67
        pextrd    $2, %xmm14, %ecx                              #2416.315
        movslq    %ecx, %rcx                                    #2416.238
        cvtdq2pd  %xmm15, %xmm10                                #2408.20
        movups    (%rsi,%rax), %xmm2                            #2416.67
        movups    (%rsi,%rcx), %xmm3                            #2416.238
        movaps    %xmm2, %xmm1                                  #2416.364
        unpcklpd  %xmm3, %xmm1                                  #2416.364
        unpckhpd  %xmm3, %xmm2                                  #2416.405
        movaps    %xmm5, %xmm3                                  #2423.12
        addpd     %xmm6, %xmm3                                  #2423.12
        movaps    %xmm3, %xmm0                                  #2424.14
        movaps    %xmm3, %xmm13                                 #2432.13
        subpd     %xmm5, %xmm0                                  #2424.14
        mulpd     %xmm3, %xmm13                                 #2432.13
        mulpd     %xmm3, %xmm12                                 #2431.26
        subpd     %xmm0, %xmm6                                  #2425.13
        addpd     12608+__svml_dlog1p_ha_data_internal(%rip), %xmm12 #2431.14
        movups    12416+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #2430.26
        mulpd     %xmm3, %xmm5                                  #2430.26
        movups    13248+__svml_dlog1p_ha_data_internal(%rip), %xmm4 #2420.25
        addpd     12480+__svml_dlog1p_ha_data_internal(%rip), %xmm5 #2430.14
        mulpd     %xmm10, %xmm4                                 #2420.25
        mulpd     %xmm13, %xmm5                                 #2433.24
        addpd     %xmm4, %xmm1                                  #2420.13
        addpd     %xmm5, %xmm12                                 #2433.12
        movups    13312+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #2419.45
        movaps    %xmm1, %xmm0                                  #2434.17
        mulpd     %xmm9, %xmm10                                 #2421.25
        addpd     %xmm3, %xmm0                                  #2434.17
        addpd     %xmm10, %xmm2                                 #2421.13
        mulpd     %xmm12, %xmm13                                #2438.24
        addpd     %xmm6, %xmm2                                  #2437.13
        movaps    %xmm0, %xmm11                                 #2435.13
        subpd     %xmm1, %xmm11                                 #2435.13
        addpd     %xmm13, %xmm2                                 #2438.12
        subpd     %xmm11, %xmm3                                 #2436.14
        addpd     %xmm2, %xmm3                                  #2439.13
        addpd     %xmm3, %xmm0                                  #2440.14
        orps      %xmm7, %xmm0                                  #2441.14
        testl     %edx, %edx                                    #2443.52
        jne       ..B7.3        # Prob 5%                       #2443.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8
..B7.2:                         # Preds ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2446.12
        popq      %rbp                                          #2446.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2446.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm8, 64(%rsp)                               #2443.197
        movups    %xmm0, 128(%rsp)                              #2443.270
                                # LOE rbx r12 r13 r14 r15 edx
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #2443.454
        movq      %r12, 8(%rsp)                                 #2443.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #2443.454
        movq      %r13, (%rsp)                                  #2443.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #2443.454
                                # LOE rbx r12 r14 r15 r13d
..B7.7:                         # Preds ..B7.8 ..B7.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2443.517
        jc        ..B7.10       # Prob 5%                       #2443.517
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2443.470
        cmpl      $2, %r12d                                     #2443.465
        jl        ..B7.7        # Prob 82%                      #2443.465
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #2443.675
        jmp       ..B7.2        # Prob 100%                     #2443.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #2443.546
        lea       128(%rsp,%r12,8), %rsi                        #2443.546
..___tag_value___svml_log1p2_ha_e9.130:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #2443.546
..___tag_value___svml_log1p2_ha_e9.131:
        jmp       ..B7.8        # Prob 100%                     #2443.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p2_ha_e9,@function
	.size	__svml_log1p2_ha_e9,.-__svml_log1p2_ha_e9
..LN__svml_log1p2_ha_e9.6:
	.data
# -- End  __svml_log1p2_ha_e9
	.text
.L_2__routine_start___svml_log1p2_ha_l9_7:
# -- Begin  __svml_log1p2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p2_ha_l9
# --- __svml_log1p2_ha_l9(__m128d)
__svml_log1p2_ha_l9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2451.1
        .byte     15                                            #2572.546
        .byte     30                                            #2572.546
        .byte     250                                           #2572.546
	.cfi_startproc
..___tag_value___svml_log1p2_ha_l9.133:
..L134:
                                                        #2451.1
        pushq     %rbp                                          #2451.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2451.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2451.1
        subq      $192, %rsp                                    #2451.1
        vmovapd   %xmm0, %xmm8                                  #2451.1
        vmovupd   13056+__svml_dlog1p_ha_data_internal(%rip), %xmm11 #2504.49
        lea       -8454144+__svml_dlog1p_ha_data_internal(%rip), %rsi #2545.67
        vandpd    %xmm11, %xmm8, %xmm9                          #2509.15
        vandnpd   %xmm8, %xmm11, %xmm7                          #2512.19
        vmovupd   12992+__svml_dlog1p_ha_data_internal(%rip), %xmm6 #2503.41
        vcmpltpd  13120+__svml_dlog1p_ha_data_internal(%rip), %xmm9, %xmm10 #2510.16
        vaddpd    %xmm6, %xmm8, %xmm12                          #2507.8
        vmaxpd    %xmm8, %xmm6, %xmm14                          #2508.8
        vminpd    %xmm8, %xmm6, %xmm15                          #2513.8
        vorpd     13184+__svml_dlog1p_ha_data_internal(%rip), %xmm10, %xmm13 #2511.17
        vandpd    %xmm13, %xmm12, %xmm5                         #2514.12
        vandpd    12672+__svml_dlog1p_ha_data_internal(%rip), %xmm5, %xmm0 #2519.15
        vorpd     12736+__svml_dlog1p_ha_data_internal(%rip), %xmm0, %xmm3 #2520.15
        vcvtpd2ps %xmm3, %xmm2                                  #2525.53
        vsubpd    %xmm5, %xmm14, %xmm1                          #2515.9
        vcmpnlepd 12864+__svml_dlog1p_ha_data_internal(%rip), %xmm8, %xmm0 #2533.14
        vaddpd    %xmm1, %xmm15, %xmm4                          #2516.9
        vcmpltpd  12800+__svml_dlog1p_ha_data_internal(%rip), %xmm8, %xmm15 #2532.14
        vmovlhps  %xmm2, %xmm2, %xmm9                           #2525.37
        vrcpps    %xmm9, %xmm10                                 #2525.26
        vcvtps2pd %xmm10, %xmm13                                #2525.13
        vpand     13568+__svml_dlog1p_ha_data_internal(%rip), %xmm5, %xmm12 #2527.13
        vpsrlq    $20, %xmm5, %xmm5                             #2530.14
        vmovdqu   13632+__svml_dlog1p_ha_data_internal(%rip), %xmm11 #2528.13
        vpsubq    %xmm12, %xmm11, %xmm14                        #2528.13
        vroundpd  $0, %xmm13, %xmm13                            #2529.13
        vmulpd    %xmm14, %xmm13, %xmm2                         #2536.14
        vfmsub213pd %xmm6, %xmm13, %xmm3                        #2534.13
        vpshufd   $221, %xmm5, %xmm6                            #2531.15
        vorpd     %xmm0, %xmm15, %xmm5                          #2538.14
        vmulpd    %xmm2, %xmm4, %xmm4                           #2551.13
        vcvtdq2pd %xmm6, %xmm1                                  #2537.20
        vaddpd    %xmm4, %xmm3, %xmm2                           #2552.12
        vmovmskpd %xmm5, %edx                                   #2540.40
        vpsrlq    $39, %xmm13, %xmm6                            #2542.18
        vmovd     %xmm6, %eax                                   #2545.144
        vsubpd    %xmm3, %xmm2, %xmm3                           #2553.14
        vmulpd    %xmm2, %xmm2, %xmm5                           #2561.13
        vsubpd    %xmm3, %xmm4, %xmm3                           #2554.13
        vpextrd   $2, %xmm6, %ecx                               #2545.315
        movslq    %eax, %rax                                    #2545.67
        movslq    %ecx, %rcx                                    #2545.238
        vmovupd   12544+__svml_dlog1p_ha_data_internal(%rip), %xmm4 #2557.55
        vfmadd213pd 12608+__svml_dlog1p_ha_data_internal(%rip), %xmm2, %xmm4 #2560.14
        vmovupd   (%rsi,%rax), %xmm9                            #2545.67
        vmovupd   (%rsi,%rcx), %xmm10                           #2545.238
        vunpcklpd %xmm10, %xmm9, %xmm0                          #2545.364
        vunpckhpd %xmm10, %xmm9, %xmm11                         #2545.405
        vfmadd231pd 13248+__svml_dlog1p_ha_data_internal(%rip), %xmm1, %xmm0 #2549.13
        vmovupd   12416+__svml_dlog1p_ha_data_internal(%rip), %xmm9 #2555.55
        vfmadd132pd 13312+__svml_dlog1p_ha_data_internal(%rip), %xmm11, %xmm1 #2550.13
        vfmadd213pd 12480+__svml_dlog1p_ha_data_internal(%rip), %xmm2, %xmm9 #2559.14
        vaddpd    %xmm2, %xmm0, %xmm10                          #2563.17
        vaddpd    %xmm3, %xmm1, %xmm1                           #2566.13
        vfmadd213pd %xmm4, %xmm5, %xmm9                         #2562.12
        vsubpd    %xmm0, %xmm10, %xmm0                          #2564.13
        vfmadd213pd %xmm1, %xmm5, %xmm9                         #2567.12
        vsubpd    %xmm0, %xmm2, %xmm6                           #2565.14
        vaddpd    %xmm9, %xmm6, %xmm11                          #2568.13
        vaddpd    %xmm11, %xmm10, %xmm12                        #2569.14
        vorpd     %xmm7, %xmm12, %xmm0                          #2570.14
        testl     %edx, %edx                                    #2572.52
        jne       ..B8.3        # Prob 5%                       #2572.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm8
..B8.2:                         # Preds ..B8.3 ..B8.9 ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2575.12
        popq      %rbp                                          #2575.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2575.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm8, 64(%rsp)                               #2572.197
        vmovupd   %xmm0, 128(%rsp)                              #2572.270
        je        ..B8.2        # Prob 95%                      #2572.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #2572.454
        movq      %r12, 8(%rsp)                                 #2572.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #2572.454
        movq      %r13, (%rsp)                                  #2572.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #2572.454
                                # LOE rbx r12 r14 r15 r13d
..B8.7:                         # Preds ..B8.8 ..B8.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2572.517
        jc        ..B8.10       # Prob 5%                       #2572.517
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.10 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2572.470
        cmpl      $2, %r12d                                     #2572.465
        jl        ..B8.7        # Prob 82%                      #2572.465
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #2572.675
        jmp       ..B8.2        # Prob 100%                     #2572.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.10:                        # Preds ..B8.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #2572.546
        lea       128(%rsp,%r12,8), %rsi                        #2572.546
..___tag_value___svml_log1p2_ha_l9.151:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #2572.546
..___tag_value___svml_log1p2_ha_l9.152:
        jmp       ..B8.8        # Prob 100%                     #2572.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p2_ha_l9,@function
	.size	__svml_log1p2_ha_l9,.-__svml_log1p2_ha_l9
..LN__svml_log1p2_ha_l9.7:
	.data
# -- End  __svml_log1p2_ha_l9
	.text
.L_2__routine_start___svml_log1p8_ha_z0_8:
# -- Begin  __svml_log1p8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_log1p8_ha_z0
# --- __svml_log1p8_ha_z0(__m512d)
__svml_log1p8_ha_z0:
# parameter 1: %zmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2580.1
        .byte     15                                            #2693.759
        .byte     30                                            #2693.759
        .byte     250                                           #2693.759
	.cfi_startproc
..___tag_value___svml_log1p8_ha_z0.154:
..L155:
                                                        #2580.1
        pushq     %rbp                                          #2580.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2580.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2580.1
        subq      $192, %rsp                                    #2580.1
        vmovups   256+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm8 #2634.54
        vmovups   320+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm3 #2639.57
        vmovaps   %zmm0, %zmm10                                 #2580.1
        vmaxpd    {sae}, %zmm10, %zmm8, %zmm12                  #2636.22
        vminpd    {sae}, %zmm10, %zmm8, %zmm14                  #2637.22
        vaddpd    {rn-sae}, %zmm10, %zmm8, %zmm13               #2635.23
        vandpd    %zmm3, %zmm10, %zmm11                         #2640.22
        vgetmantpd $8, {sae}, %zmm13, %zmm7                     #2638.28
        vgetexppd {sae}, %zmm13, %zmm9                          #2642.25
        vsubpd    {rn-sae}, %zmm12, %zmm13, %zmm15              #2641.22
        vrcp14pd  %zmm7, %zmm2                                  #2643.24
        vmovups   384+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm13 #2649.54
        vsubpd    {rn-sae}, %zmm15, %zmm14, %zmm0               #2644.22
        vxorpd    %zmm3, %zmm9, %zmm6                           #2646.24
        vrndscalepd $88, {sae}, %zmm2, %zmm12                   #2647.24
        vmovups   576+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm14 #2663.61
        vmovups   512+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm3 #2665.21
        vmovups   960+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm15 #2668.53
        vscalefpd {rn-sae}, %zmm6, %zmm0, %zmm4                 #2648.20
        vcmppd    $17, {sae}, %zmm13, %zmm12, %k1               #2650.26
        vfmsub213pd {rn-sae}, %zmm8, %zmm12, %zmm7              #2651.20
        vmovups   128+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm0 #2660.296
        vmulpd    {rn-sae}, %zmm4, %zmm12, %zmm1                #2652.22
        vmovups   __svml_dlog1p_ha_data_internal_avx512(%rip), %zmm4 #2659.296
        vaddpd    {rn-sae}, %zmm8, %zmm9, %zmm9{%k1}            #2658.25
        vaddpd    {rn-sae}, %zmm1, %zmm7, %zmm6                 #2653.21
        vmovups   448+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm8 #2661.61
        vmulpd    {rn-sae}, %zmm6, %zmm6, %zmm2                 #2666.22
        vfmadd231pd {rn-sae}, %zmm6, %zmm8, %zmm3               #2665.21
        vmovups   896+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm8 #2676.21
        vsubpd    {rn-sae}, %zmm7, %zmm6, %zmm5                 #2654.23
        vpsrlq    $48, %zmm12, %zmm7                            #2656.45
        vmovups   768+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm12 #2675.21
        vsubpd    {rn-sae}, %zmm5, %zmm1, %zmm5                 #2655.22
        vpermt2pd 192+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm7, %zmm0 #2660.296
        vpermt2pd 64+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm7, %zmm4 #2659.296
        vmovups   640+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm1 #2667.21
        vmovups   832+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm7 #2673.61
        vfmadd231pd {rn-sae}, %zmm9, %zmm15, %zmm4              #2669.20
        vaddpd    {rn-sae}, %zmm5, %zmm0, %zmm0                 #2670.22
        vfmadd231pd {rn-sae}, %zmm6, %zmm14, %zmm1              #2667.21
        vmovups   704+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm5 #2671.61
        vfmadd231pd {rn-sae}, %zmm6, %zmm7, %zmm8               #2676.21
        vaddpd    {rn-sae}, %zmm6, %zmm4, %zmm14                #2678.24
        vfmadd213pd {rn-sae}, %zmm1, %zmm2, %zmm3               #2677.21
        vfmadd231pd {rn-sae}, %zmm6, %zmm5, %zmm12              #2675.21
        vmulpd    {rn-sae}, %zmm2, %zmm2, %zmm1                 #2679.22
        vfmadd213pd {rn-sae}, %zmm8, %zmm2, %zmm12              #2680.21
        vsubpd    {rn-sae}, %zmm4, %zmm14, %zmm4                #2681.22
        vfmadd213pd {rn-sae}, %zmm12, %zmm1, %zmm3              #2682.21
        vsubpd    {rn-sae}, %zmm4, %zmm6, %zmm13                #2683.21
        vmovups   1024+__svml_dlog1p_ha_data_internal_avx512(%rip), %zmm6 #2684.53
        vfmadd213pd {rn-sae}, %zmm13, %zmm2, %zmm3              #2686.19
        vcmppd    $4, {sae}, %zmm13, %zmm13, %k0                #2687.27
        vfmadd213pd {rn-sae}, %zmm0, %zmm6, %zmm9               #2685.20
        kmovw     %k0, %esi                                     #2688.48
        vaddpd    {rn-sae}, %zmm9, %zmm3, %zmm9                 #2689.21
        vaddpd    {rn-sae}, %zmm9, %zmm14, %zmm0                #2690.24
        vorpd     %zmm11, %zmm0, %zmm0                          #2691.21
        testl     %esi, %esi                                    #2693.52
        jne       ..B9.3        # Prob 5%                       #2693.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm10
..B9.2:                         # Preds ..B9.9 ..B9.7 ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2696.12
        popq      %rbp                                          #2696.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2696.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2693.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm10
..B9.4:                         # Preds ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #2693.249
        movl      %edx, %eax                                    #2693.303
        orl       $8064, %eax                                   #2693.303
        cmpl      %eax, %edx                                    #2693.332
        je        ..B9.6        # Prob 78%                      #2693.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm10
..B9.5:                         # Preds ..B9.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #2693.344
        vldmxcsr  32(%rsp)                                      #2693.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm10
..B9.6:                         # Preds ..B9.5 ..B9.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm10, 64(%rsp)                              #2693.406
        vmovups   %zmm0, 128(%rsp)                              #2693.481
        testl     %esi, %esi                                    #2693.587
        jne       ..B9.11       # Prob 5%                       #2693.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.7:                         # Preds ..B9.14 ..B9.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #2693.932
        je        ..B9.2        # Prob 78%                      #2693.932
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B9.8:                         # Preds ..B9.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2693.957
        movl      32(%rsp), %eax                                #2693.957
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.9:                         # Preds ..B9.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #2693.944
        orl       %edx, %eax                                    #2693.944
        movl      %eax, 32(%rsp)                                #2693.944
        vldmxcsr  32(%rsp)                                      #2693.944
        jmp       ..B9.2        # Prob 100%                     #2693.944
                                # LOE rbx r12 r13 r14 r15 zmm0
..B9.11:                        # Preds ..B9.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #2693.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B9.18:                        # Preds ..B9.11
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
..B9.12:                        # Preds ..B9.13 ..B9.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #2693.730
        jc        ..B9.15       # Prob 5%                       #2693.730
                                # LOE rbx r15 r12d r13d r14d
..B9.13:                        # Preds ..B9.15 ..B9.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #2693.683
        cmpl      $8, %ebx                                      #2693.678
        jl        ..B9.12       # Prob 82%                      #2693.678
                                # LOE rbx r15 r12d r13d r14d
..B9.14:                        # Preds ..B9.13
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
        vmovups   128(%rsp), %zmm0                              #2693.888
        jmp       ..B9.7        # Prob 100%                     #2693.888
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.15:                        # Preds ..B9.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #2693.759
        lea       128(%rsp,%rbx,8), %rsi                        #2693.759
..___tag_value___svml_log1p8_ha_z0.182:
#       __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
        call      __svml_dlog1p_ha_cout_rare_internal           #2693.759
..___tag_value___svml_log1p8_ha_z0.183:
        jmp       ..B9.13       # Prob 100%                     #2693.759
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_log1p8_ha_z0,@function
	.size	__svml_log1p8_ha_z0,.-__svml_log1p8_ha_z0
..LN__svml_log1p8_ha_z0.8:
	.data
# -- End  __svml_log1p8_ha_z0
	.text
.L_2__routine_start___svml_dlog1p_ha_cout_rare_internal_9:
# -- Begin  __svml_dlog1p_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dlog1p_ha_cout_rare_internal
	.globl __svml_dlog1p_ha_cout_rare_internal
# --- __svml_dlog1p_ha_cout_rare_internal(const double *, double *)
__svml_dlog1p_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2101.1
        .byte     15                                            #2180.17
        .byte     30                                            #2180.17
        .byte     250                                           #2180.17
	.cfi_startproc
..___tag_value___svml_dlog1p_ha_cout_rare_internal.185:
..L186:
                                                        #2101.1
        xorl      %eax, %eax                                    #2111.14
        movsd     .L_2il0floatpacket.97(%rip), %xmm3            #2112.7
        movsd     (%rdi), %xmm2                                 #2112.14
        addsd     %xmm3, %xmm2                                  #2112.20
        movsd     %xmm2, -16(%rsp)                              #2102.29
        movzwl    -10(%rsp), %edx                               #2113.36
        andl      $32752, %edx                                  #2113.36
        cmpl      $32752, %edx                                  #2113.61
        je        ..B10.12      # Prob 16%                      #2113.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm2 xmm3
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movsd     %xmm2, -8(%rsp)                               #2115.13
        xorl      %ecx, %ecx                                    #2116.13
        movzwl    -2(%rsp), %edx                                #2117.39
        testl     $32752, %edx                                  #2117.39
        jne       ..B10.4       # Prob 50%                      #2117.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2 xmm3
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        mulsd     1600+_imldLnHATab(%rip), %xmm2                #2119.17
        movl      $-60, %ecx                                    #2120.17
        movsd     %xmm2, -8(%rsp)                               #2119.17
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm2 xmm3
..B10.4:                        # Preds ..B10.3 ..B10.2
                                # Execution count [8.40e-01]
        movsd     1608+_imldLnHATab(%rip), %xmm0                #2122.39
        comisd    %xmm0, %xmm2                                  #2122.39
        jbe       ..B10.8       # Prob 50%                      #2122.39
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm0 xmm2 xmm3
..B10.5:                        # Preds ..B10.4
                                # Execution count [4.20e-01]
        movaps    %xmm2, %xmm1                                  #2124.34
        subsd     %xmm3, %xmm1                                  #2124.34
        movsd     %xmm1, -16(%rsp)                              #2125.17
        andb      $127, -9(%rsp)                                #2126.38
        movsd     -16(%rsp), %xmm0                              #2127.22
        comisd    1592+_imldLnHATab(%rip), %xmm0                #2127.48
        jbe       ..B10.7       # Prob 50%                      #2127.48
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax ecx xmm1 xmm2 xmm3
..B10.6:                        # Preds ..B10.5
                                # Execution count [2.10e-01]
        movsd     %xmm2, -16(%rsp)                              #2133.21
        pxor      %xmm7, %xmm7                                  #2130.35
        movzwl    -10(%rsp), %edi                               #2134.42
        lea       _imldLnHATab(%rip), %r10                      #2137.48
        andl      $-32753, %edi                                 #2134.42
        addl      $16368, %edi                                  #2134.42
        movw      %di, -10(%rsp)                                #2134.42
        movsd     -16(%rsp), %xmm4                              #2135.38
        movaps    %xmm4, %xmm1                                  #2135.59
        movaps    %xmm4, %xmm2                                  #2142.59
        movsd     1672+_imldLnHATab(%rip), %xmm8                #2148.49
        movzwl    -2(%rsp), %edx                                #2129.48
        andl      $32752, %edx                                  #2129.48
        addsd     1576+_imldLnHATab(%rip), %xmm1                #2135.59
        addsd     1584+_imldLnHATab(%rip), %xmm2                #2142.59
        movsd     %xmm1, -24(%rsp)                              #2135.21
        movl      -24(%rsp), %r8d                               #2136.46
        movsd     %xmm2, -24(%rsp)                              #2142.21
        andl      $127, %r8d                                    #2136.77
        movsd     -24(%rsp), %xmm5                              #2143.38
        movsd     1560+_imldLnHATab(%rip), %xmm9                #2131.64
        movsd     1568+_imldLnHATab(%rip), %xmm0                #2132.64
        shrl      $4, %edx                                      #2129.48
        subsd     1584+_imldLnHATab(%rip), %xmm5                #2143.63
        lea       (%r8,%r8,2), %r9d                             #2137.64
        movsd     (%r10,%r9,8), %xmm6                           #2137.48
        lea       -1023(%rcx,%rdx), %ecx                        #2129.21
        cvtsi2sd  %ecx, %xmm7                                   #2130.35
        subsd     %xmm5, %xmm4                                  #2144.42
        mulsd     %xmm6, %xmm5                                  #2145.57
        mulsd     %xmm7, %xmm9                                  #2131.64
        subsd     %xmm3, %xmm5                                  #2145.67
        mulsd     %xmm4, %xmm6                                  #2146.48
        mulsd     %xmm0, %xmm7                                  #2132.64
        addsd     8(%r10,%r9,8), %xmm9                          #2140.49
        addsd     16(%r10,%r9,8), %xmm7                         #2141.49
        addsd     %xmm5, %xmm9                                  #2150.48
        addsd     %xmm6, %xmm7                                  #2151.57
        movaps    %xmm5, %xmm3                                  #2147.42
        addsd     %xmm6, %xmm3                                  #2147.42
        mulsd     %xmm3, %xmm8                                  #2148.68
        addsd     1664+_imldLnHATab(%rip), %xmm8                #2148.87
        mulsd     %xmm3, %xmm8                                  #2148.107
        addsd     1656+_imldLnHATab(%rip), %xmm8                #2148.128
        mulsd     %xmm3, %xmm8                                  #2148.148
        addsd     1648+_imldLnHATab(%rip), %xmm8                #2148.169
        mulsd     %xmm3, %xmm8                                  #2148.189
        addsd     1640+_imldLnHATab(%rip), %xmm8                #2148.210
        mulsd     %xmm3, %xmm8                                  #2148.230
        addsd     1632+_imldLnHATab(%rip), %xmm8                #2148.251
        mulsd     %xmm3, %xmm8                                  #2148.271
        addsd     1624+_imldLnHATab(%rip), %xmm8                #2148.292
        mulsd     %xmm3, %xmm8                                  #2149.40
        mulsd     %xmm3, %xmm8                                  #2149.42
        addsd     %xmm7, %xmm8                                  #2151.67
        addsd     %xmm8, %xmm9                                  #2152.48
        movsd     %xmm9, (%rsi)                                 #2152.21
        ret                                                     #2152.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.7:                        # Preds ..B10.5
                                # Execution count [2.10e-01]
        movsd     1672+_imldLnHATab(%rip), %xmm0                #2156.89
        mulsd     %xmm1, %xmm0                                  #2156.108
        addsd     1664+_imldLnHATab(%rip), %xmm0                #2156.129
        mulsd     %xmm1, %xmm0                                  #2156.150
        addsd     1656+_imldLnHATab(%rip), %xmm0                #2156.171
        mulsd     %xmm1, %xmm0                                  #2156.192
        addsd     1648+_imldLnHATab(%rip), %xmm0                #2156.213
        mulsd     %xmm1, %xmm0                                  #2156.234
        addsd     1640+_imldLnHATab(%rip), %xmm0                #2156.255
        mulsd     %xmm1, %xmm0                                  #2156.276
        addsd     1632+_imldLnHATab(%rip), %xmm0                #2156.297
        mulsd     %xmm1, %xmm0                                  #2156.318
        addsd     1624+_imldLnHATab(%rip), %xmm0                #2156.339
        mulsd     %xmm1, %xmm0                                  #2157.40
        mulsd     %xmm1, %xmm0                                  #2157.42
        addsd     %xmm1, %xmm0                                  #2158.40
        movsd     %xmm0, (%rsi)                                 #2159.21
        ret                                                     #2159.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.4
                                # Execution count [4.20e-01]
        ucomisd   %xmm0, %xmm2                                  #2164.44
        jp        ..B10.9       # Prob 0%                       #2164.44
        je        ..B10.11      # Prob 16%                      #2164.44
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm0
..B10.9:                        # Preds ..B10.8
                                # Execution count [3.53e-01]
        divsd     %xmm0, %xmm0                                  #2171.83
        movsd     %xmm0, (%rsi)                                 #2171.21
        movl      $1, %eax                                      #2172.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.9
                                # Execution count [1.00e+00]
        ret                                                     #2188.12
                                # LOE
..B10.11:                       # Preds ..B10.8
                                # Execution count [6.72e-02]: Infreq
        movsd     1616+_imldLnHATab(%rip), %xmm1                #2166.46
        movl      $2, %eax                                      #2167.21
        xorps     .L_2il0floatpacket.96(%rip), %xmm1            #2166.46
        divsd     %xmm0, %xmm1                                  #2166.84
        movsd     %xmm1, (%rsi)                                 #2166.21
        ret                                                     #2166.21
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.12:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        movb      -9(%rsp), %dl                                 #2178.40
        andb      $-128, %dl                                    #2178.40
        cmpb      $-128, %dl                                    #2178.61
        je        ..B10.14      # Prob 16%                      #2178.61
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm2
..B10.13:                       # Preds ..B10.15 ..B10.12 ..B10.14
                                # Execution count [1.54e-01]: Infreq
        mulsd     %xmm2, %xmm2                                  #2185.31
        movsd     %xmm2, (%rsi)                                 #2185.17
        ret                                                     #2185.17
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.14:                       # Preds ..B10.12
                                # Execution count [2.56e-02]: Infreq
        testl     $1048575, -12(%rsp)                           #2178.90
        jne       ..B10.13      # Prob 50%                      #2178.121
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm2
..B10.15:                       # Preds ..B10.14
                                # Execution count [1.28e-02]: Infreq
        cmpl      $0, -16(%rsp)                                 #2178.180
        jne       ..B10.13      # Prob 50%                      #2178.180
                                # LOE rbx rbp rsi r12 r13 r14 r15 eax xmm2
..B10.16:                       # Preds ..B10.15
                                # Execution count [6.40e-03]: Infreq
        movsd     1608+_imldLnHATab(%rip), %xmm0                #2180.79
        movl      $1, %eax                                      #2181.17
        divsd     %xmm0, %xmm0                                  #2180.79
        movsd     %xmm0, (%rsi)                                 #2180.17
        ret                                                     #2180.17
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dlog1p_ha_cout_rare_internal,@function
	.size	__svml_dlog1p_ha_cout_rare_internal,.-__svml_dlog1p_ha_cout_rare_internal
..LN__svml_dlog1p_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dlog1p_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dlog1p_ha_data_internal_avx512
	.globl __svml_dlog1p_ha_data_internal_avx512
__svml_dlog1p_ha_data_internal_avx512:
	.long	0
	.long	0
	.long	3222405120
	.long	3215919664
	.long	1848311808
	.long	3216910087
	.long	1890025472
	.long	3217424176
	.long	3348791296
	.long	3217854455
	.long	2880159744
	.long	3218171740
	.long	3256631296
	.long	3218366859
	.long	4139499520
	.long	3218553303
	.long	288669696
	.long	1070754146
	.long	1823703040
	.long	1070569756
	.long	1015742464
	.long	1070240749
	.long	1800667136
	.long	1069924160
	.long	2183659520
	.long	1069619086
	.long	1566113792
	.long	1069101918
	.long	3047030784
	.long	1068533144
	.long	2313682944
	.long	1067467101
	.long	0
	.long	0
	.long	3496399314
	.long	1028893491
	.long	720371772
	.long	1026176044
	.long	1944193543
	.long	3175338952
	.long	634920691
	.long	3175752108
	.long	1664625295
	.long	1029304828
	.long	192624563
	.long	3177103997
	.long	3796653051
	.long	3176138396
	.long	1005287031
	.long	1029411448
	.long	3433090547
	.long	1029291198
	.long	1779090477
	.long	3176336968
	.long	3242817150
	.long	1029626109
	.long	3430202884
	.long	3175842849
	.long	2270669051
	.long	1028809259
	.long	1375653371
	.long	1028457284
	.long	4246237509
	.long	3176626033
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
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	815627376
	.long	1069318605
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2123607906
	.long	3217033013
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	2632876527
	.long	1069697314
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	4213758726
	.long	3217380691
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	2580324188
	.long	1070176665
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	787901
	.long	3218079744
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	1431655526
	.long	1070945621
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4294967238
	.long	3219128319
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
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
	.type	__svml_dlog1p_ha_data_internal_avx512,@object
	.size	__svml_dlog1p_ha_data_internal_avx512,1280
	.align 64
	.hidden __svml_dlog1p_ha_data_internal
	.globl __svml_dlog1p_ha_data_internal
__svml_dlog1p_ha_data_internal:
	.long	3715793664
	.long	3230016299
	.long	4013928704
	.long	3189565726
	.long	3699038248
	.long	3230016303
	.long	4255595370
	.long	3189567439
	.long	3648859040
	.long	3230016307
	.long	1732833791
	.long	3189567296
	.long	3565385952
	.long	3230016311
	.long	2485752410
	.long	3189567274
	.long	3448748152
	.long	3230016315
	.long	3514744523
	.long	3189567800
	.long	3299074072
	.long	3230016319
	.long	3686511262
	.long	3189567474
	.long	3116491376
	.long	3230016323
	.long	716063611
	.long	3189567908
	.long	2901127016
	.long	3230016327
	.long	354437295
	.long	3189567291
	.long	2653107184
	.long	3230016331
	.long	3777462366
	.long	3189567264
	.long	2372557360
	.long	3230016335
	.long	3590663412
	.long	3189567498
	.long	2059602304
	.long	3230016339
	.long	1680795031
	.long	3189567428
	.long	1714366048
	.long	3230016343
	.long	2010344982
	.long	3189567986
	.long	1336971936
	.long	3230016347
	.long	166254822
	.long	3189567184
	.long	927542568
	.long	3230016351
	.long	59552610
	.long	3189567972
	.long	486199888
	.long	3230016355
	.long	2268824919
	.long	3189567664
	.long	13065112
	.long	3230016359
	.long	3005381643
	.long	3189567835
	.long	3803226080
	.long	3230016362
	.long	4018973165
	.long	3189567825
	.long	3266868056
	.long	3230016366
	.long	3693925101
	.long	3189567457
	.long	2699077504
	.long	3230016370
	.long	4005905743
	.long	3189567697
	.long	2099972936
	.long	3230016374
	.long	3580935118
	.long	3189567215
	.long	1469672176
	.long	3230016378
	.long	2928784295
	.long	3189567129
	.long	808292392
	.long	3230016382
	.long	674298833
	.long	3189567553
	.long	115950104
	.long	3230016386
	.long	2555938320
	.long	3189567208
	.long	3687728456
	.long	3230016389
	.long	532458218
	.long	3189567130
	.long	2933808064
	.long	3230016393
	.long	3888913110
	.long	3189567192
	.long	2149270784
	.long	3230016397
	.long	2365955494
	.long	3189567756
	.long	1334230552
	.long	3230016401
	.long	3325722981
	.long	3189567206
	.long	488800640
	.long	3230016405
	.long	1496133617
	.long	3189567683
	.long	3908061016
	.long	3230016408
	.long	1824836379
	.long	3189567490
	.long	3002189112
	.long	3230016412
	.long	1222174320
	.long	3189567837
	.long	2066263648
	.long	3230016416
	.long	2472769660
	.long	3189567285
	.long	1100395408
	.long	3230016420
	.long	447932460
	.long	3189567455
	.long	104694584
	.long	3230016424
	.long	1006807684
	.long	3189567460
	.long	3374238048
	.long	3230016427
	.long	4121834935
	.long	3189567556
	.long	2319200184
	.long	3230016431
	.long	3019906587
	.long	3189567664
	.long	1234656664
	.long	3230016435
	.long	3882744783
	.long	3189567930
	.long	120715280
	.long	3230016439
	.long	1927363170
	.long	3189567242
	.long	3272450504
	.long	3230016442
	.long	8203115
	.long	3189567876
	.long	2100034368
	.long	3230016446
	.long	1260625560
	.long	3189567855
	.long	898540192
	.long	3230016450
	.long	1791679111
	.long	3189567639
	.long	3963040720
	.long	3230016453
	.long	831133439
	.long	3189567590
	.long	2703706240
	.long	3230016457
	.long	2803140504
	.long	3189567482
	.long	1415608352
	.long	3230016461
	.long	2955844462
	.long	3189568058
	.long	98850816
	.long	3230016465
	.long	4017967070
	.long	3189567455
	.long	3048504096
	.long	3230016468
	.long	3974385451
	.long	3189567871
	.long	1674736240
	.long	3230016472
	.long	2424114556
	.long	3189567887
	.long	272616624
	.long	3230016476
	.long	4261883032
	.long	3189567073
	.long	3137214056
	.long	3230016479
	.long	3716750895
	.long	3189567522
	.long	1678694944
	.long	3230016483
	.long	498025304
	.long	3189567180
	.long	192127016
	.long	3230016487
	.long	1011852160
	.long	3189567511
	.long	2972577480
	.long	3230016490
	.long	558940916
	.long	3189567850
	.long	1430211120
	.long	3230016494
	.long	2550865739
	.long	3189567935
	.long	4155061376
	.long	3230016497
	.long	1930847170
	.long	3189567371
	.long	2557291968
	.long	3230016501
	.long	1716354804
	.long	3189567130
	.long	931967984
	.long	3230016505
	.long	2729403392
	.long	3189567981
	.long	3574154016
	.long	3230016508
	.long	1067280599
	.long	3189567890
	.long	1894012232
	.long	3230016512
	.long	2094260282
	.long	3189567604
	.long	186606176
	.long	3230016516
	.long	3897933165
	.long	3189568046
	.long	2746998904
	.long	3230016519
	.long	1935720459
	.long	3189567701
	.long	985351064
	.long	3230016523
	.long	2170344821
	.long	3189567164
	.long	3491691984
	.long	3230016526
	.long	4220979537
	.long	3189567547
	.long	1676181328
	.long	3230016530
	.long	1953924393
	.long	3189567854
	.long	4128847448
	.long	3230016533
	.long	1689829067
	.long	3189567468
	.long	2259849008
	.long	3230016537
	.long	2367753130
	.long	3189567619
	.long	364246088
	.long	3230016541
	.long	2263309073
	.long	3189567722
	.long	2737098280
	.long	3230016544
	.long	2366756869
	.long	3189567856
	.long	788562816
	.long	3230016548
	.long	2294653975
	.long	3189567145
	.long	3108665616
	.long	3230016551
	.long	1786494117
	.long	3189567254
	.long	1107562960
	.long	3230016555
	.long	3655887416
	.long	3189567689
	.long	3375279848
	.long	3230016558
	.long	1178948625
	.long	3189567241
	.long	1321971608
	.long	3230016562
	.long	1620080441
	.long	3189567466
	.long	3537662304
	.long	3230016565
	.long	154455618
	.long	3189567981
	.long	1432506360
	.long	3230016569
	.long	3128192125
	.long	3189567889
	.long	3596526920
	.long	3230016572
	.long	2026842750
	.long	3189567212
	.long	1439877480
	.long	3230016576
	.long	1384431897
	.long	3189567284
	.long	3552580280
	.long	3230016579
	.long	1999983129
	.long	3189568079
	.long	1344787944
	.long	3230016583
	.long	4035436820
	.long	3189567579
	.long	3406521808
	.long	3230016586
	.long	77799817
	.long	3189567285
	.long	1147933592
	.long	3230016590
	.long	1261060207
	.long	3189567477
	.long	3159043768
	.long	3230016593
	.long	2102717775
	.long	3189567605
	.long	850003184
	.long	3230016597
	.long	161472469
	.long	3189567688
	.long	2810831440
	.long	3230016600
	.long	1937336881
	.long	3189567665
	.long	451678520
	.long	3230016604
	.long	2268353549
	.long	3189567773
	.long	2362563168
	.long	3230016607
	.long	1785871595
	.long	3189567889
	.long	4248601816
	.long	3230016610
	.long	2434669991
	.long	3189567898
	.long	1814910472
	.long	3230016614
	.long	2666999375
	.long	3189568058
	.long	3651506624
	.long	3230016617
	.long	1033139811
	.long	3189567312
	.long	1168538136
	.long	3230016621
	.long	4004618418
	.long	3189567740
	.long	2956021672
	.long	3230016624
	.long	3799928764
	.long	3189567751
	.long	424104288
	.long	3230016628
	.long	3650233137
	.long	3189567549
	.long	2162801816
	.long	3230016631
	.long	437313833
	.long	3189567438
	.long	3877227800
	.long	3230016634
	.long	2138293494
	.long	3189567140
	.long	1272495376
	.long	3230016638
	.long	2891370752
	.long	3189567168
	.long	2938619176
	.long	3230016641
	.long	1704142730
	.long	3189567114
	.long	285744240
	.long	3230016645
	.long	500817607
	.long	3189568035
	.long	1903884424
	.long	3230016648
	.long	3102571944
	.long	3189567668
	.long	3498151272
	.long	3230016651
	.long	2374193154
	.long	3189567933
	.long	773655960
	.long	3230016655
	.long	2898331636
	.long	3189568089
	.long	2320411168
	.long	3230016658
	.long	1983295500
	.long	3189567113
	.long	3843527280
	.long	3230016661
	.long	2132853542
	.long	3189567093
	.long	1048114320
	.long	3230016665
	.long	3551155364
	.long	3189567407
	.long	2524183824
	.long	3230016668
	.long	2564000663
	.long	3189567090
	.long	3976845048
	.long	3230016671
	.long	361581803
	.long	3189567196
	.long	1111206880
	.long	3230016675
	.long	696765230
	.long	3189568040
	.long	2517279744
	.long	3230016678
	.long	941542475
	.long	3189567463
	.long	3900171776
	.long	3230016681
	.long	2377895552
	.long	3189567282
	.long	964990760
	.long	3230016685
	.long	1173605489
	.long	3189567454
	.long	2301746008
	.long	3230016688
	.long	3096720313
	.long	3189567400
	.long	3615544576
	.long	3230016691
	.long	1942008121
	.long	3189567335
	.long	611493160
	.long	3230016695
	.long	3375481119
	.long	3189567537
	.long	1879599992
	.long	3230016698
	.long	4103239143
	.long	3189567624
	.long	3124971056
	.long	3230016701
	.long	3773474925
	.long	3189567869
	.long	52711992
	.long	3230016705
	.long	2465315445
	.long	3189567444
	.long	1252829960
	.long	3230016708
	.long	1041581158
	.long	3189567781
	.long	2430429904
	.long	3230016711
	.long	166620501
	.long	3189567739
	.long	3585583704
	.long	3230016714
	.long	2379237630
	.long	3189567984
	.long	423395616
	.long	3230016718
	.long	134834559
	.long	3189567178
	.long	1533871416
	.long	3230016721
	.long	1449318760
	.long	3189567373
	.long	2622114680
	.long	3230016724
	.long	3042372984
	.long	3189567127
	.long	3688195920
	.long	3230016727
	.long	1265290101
	.long	3189567921
	.long	437218048
	.long	3230016731
	.long	2248549362
	.long	3189567262
	.long	1459185496
	.long	3230016734
	.long	1293250365
	.long	3189567151
	.long	2459200496
	.long	3230016737
	.long	313786605
	.long	3189567180
	.long	3437332240
	.long	3230016740
	.long	3289151823
	.long	3189567872
	.long	98682312
	.long	3230016744
	.long	2693758821
	.long	3189567877
	.long	1033253848
	.long	3230016747
	.long	4237443192
	.long	3189567301
	.long	1946147760
	.long	3230016750
	.long	4099404586
	.long	3189567972
	.long	2837431968
	.long	3230016753
	.long	343640666
	.long	3189567552
	.long	3707174040
	.long	3230016756
	.long	4238401458
	.long	3189567983
	.long	260473960
	.long	3230016760
	.long	21423548
	.long	3189567559
	.long	1087333264
	.long	3230016763
	.long	3515278593
	.long	3189567309
	.long	1892851296
	.long	3230016766
	.long	629106375
	.long	3189567166
	.long	2677094384
	.long	3230016769
	.long	2048670044
	.long	3189567227
	.long	3440128544
	.long	3230016772
	.long	3739217292
	.long	3189568009
	.long	4182019504
	.long	3230016775
	.long	3662039444
	.long	3189567618
	.long	607865368
	.long	3230016779
	.long	1766486991
	.long	3189567112
	.long	1307665824
	.long	3230016782
	.long	512689457
	.long	3189567670
	.long	1986518392
	.long	3230016785
	.long	1704577378
	.long	3189567760
	.long	2644487576
	.long	3230016788
	.long	4001935135
	.long	3189567473
	.long	3281637576
	.long	3230016791
	.long	1184452342
	.long	3189567735
	.long	3898032312
	.long	3230016794
	.long	2714793956
	.long	3189567464
	.long	198768096
	.long	3230016798
	.long	3657654418
	.long	3189567905
	.long	773842856
	.long	3230016801
	.long	2809688013
	.long	3189567736
	.long	1328352328
	.long	3230016804
	.long	390727591
	.long	3189567393
	.long	1862359256
	.long	3230016807
	.long	1135237925
	.long	3189567248
	.long	2375926096
	.long	3230016810
	.long	1377572509
	.long	3189567837
	.long	2869115032
	.long	3230016813
	.long	3148383775
	.long	3189568032
	.long	3341987960
	.long	3230016816
	.long	3809536738
	.long	3189567313
	.long	3794606472
	.long	3230016819
	.long	4243445934
	.long	3189568034
	.long	4227031920
	.long	3230016822
	.long	1302642088
	.long	3189567451
	.long	344358048
	.long	3230016826
	.long	3239125361
	.long	3189567151
	.long	736580224
	.long	3230016829
	.long	3753161736
	.long	3189567129
	.long	1108791648
	.long	3230016832
	.long	624357483
	.long	3189568045
	.long	1461052568
	.long	3230016835
	.long	3153257064
	.long	3189567338
	.long	1793422920
	.long	3230016838
	.long	2705820149
	.long	3189567635
	.long	2105962400
	.long	3230016841
	.long	911564860
	.long	3189567737
	.long	2398730424
	.long	3230016844
	.long	1086958332
	.long	3189567948
	.long	2671786152
	.long	3230016847
	.long	1598984535
	.long	3189567210
	.long	2925188456
	.long	3230016850
	.long	4113385879
	.long	3189567401
	.long	3158995968
	.long	3230016853
	.long	2426812671
	.long	3189567396
	.long	3373267048
	.long	3230016856
	.long	3480845679
	.long	3189567359
	.long	3568059792
	.long	3230016859
	.long	3306230729
	.long	3189567924
	.long	3743432056
	.long	3230016862
	.long	3853821277
	.long	3189567317
	.long	3899441408
	.long	3230016865
	.long	2049309148
	.long	3189567699
	.long	4036145192
	.long	3230016868
	.long	2667321817
	.long	3189567166
	.long	4153600472
	.long	3230016871
	.long	3540670389
	.long	3189567114
	.long	4251864072
	.long	3230016874
	.long	404207883
	.long	3189567310
	.long	36025272
	.long	3230016878
	.long	2837617046
	.long	3189567103
	.long	96074976
	.long	3230016881
	.long	1231280453
	.long	3189567669
	.long	137101976
	.long	3230016884
	.long	792492437
	.long	3189567071
	.long	159162080
	.long	3230016887
	.long	2385525292
	.long	3189567618
	.long	162310880
	.long	3230016890
	.long	3272051015
	.long	3189567858
	.long	146603712
	.long	3230016893
	.long	623573538
	.long	3189567881
	.long	112095672
	.long	3230016896
	.long	2151835939
	.long	3189567451
	.long	58841608
	.long	3230016899
	.long	958006130
	.long	3189567217
	.long	4281863424
	.long	3230016901
	.long	549948691
	.long	3189567863
	.long	4191280920
	.long	3230016904
	.long	4177689987
	.long	3189567217
	.long	4082115512
	.long	3230016907
	.long	2895765193
	.long	3189567599
	.long	3954421112
	.long	3230016910
	.long	1055606208
	.long	3189567799
	.long	3808251392
	.long	3230016913
	.long	1966521217
	.long	3189567351
	.long	3643659776
	.long	3230016916
	.long	1017382758
	.long	3189567732
	.long	3460699480
	.long	3230016919
	.long	3485890155
	.long	3189567410
	.long	3259423472
	.long	3230016922
	.long	2657419170
	.long	3189567140
	.long	3039884488
	.long	3230016925
	.long	4198858220
	.long	3189568080
	.long	2802135064
	.long	3230016928
	.long	2343740494
	.long	3189567869
	.long	2546227480
	.long	3230016931
	.long	2423084215
	.long	3189567957
	.long	2272213808
	.long	3230016934
	.long	3887273856
	.long	3189567631
	.long	1980145888
	.long	3230016937
	.long	3992755473
	.long	3189567251
	.long	1670075336
	.long	3230016940
	.long	722498073
	.long	3189567389
	.long	1342053552
	.long	3230016943
	.long	3573506400
	.long	3189567965
	.long	996131728
	.long	3230016946
	.long	714695829
	.long	3189567392
	.long	632360808
	.long	3230016949
	.long	2816704229
	.long	3189567846
	.long	250791552
	.long	3230016952
	.long	386951333
	.long	3189567243
	.long	4146441768
	.long	3230016954
	.long	2155319939
	.long	3189567580
	.long	3729427184
	.long	3230016957
	.long	4227840137
	.long	3189567931
	.long	3294765200
	.long	3230016960
	.long	1718354212
	.long	3189567672
	.long	2842505696
	.long	3230016963
	.long	4243880582
	.long	3189567656
	.long	2372698352
	.long	3230016966
	.long	3784093842
	.long	3189567302
	.long	1885392624
	.long	3230016969
	.long	2318733051
	.long	3189567813
	.long	1380637784
	.long	3230016972
	.long	497929746
	.long	3189567210
	.long	858482864
	.long	3230016975
	.long	3218883783
	.long	3189567649
	.long	318976720
	.long	3230016978
	.long	3492652862
	.long	3189567382
	.long	4057135272
	.long	3230016980
	.long	473724673
	.long	3189568069
	.long	3483072376
	.long	3230016983
	.long	150485010
	.long	3189567757
	.long	2891803552
	.long	3230016986
	.long	3028900958
	.long	3189567173
	.long	2283376816
	.long	3230016989
	.long	321242596
	.long	3189567822
	.long	1657840008
	.long	3230016992
	.long	151305746
	.long	3189568029
	.long	1015240760
	.long	3230016995
	.long	2208026401
	.long	3189567184
	.long	355626480
	.long	3230016998
	.long	2487781842
	.long	3189567931
	.long	3974011720
	.long	3230017000
	.long	2391142094
	.long	3189567091
	.long	3280508896
	.long	3230017003
	.long	734457618
	.long	3189568092
	.long	2570132168
	.long	3230017006
	.long	3070902701
	.long	3189567794
	.long	1842928168
	.long	3230017009
	.long	1110877923
	.long	3189567896
	.long	1098943352
	.long	3230017012
	.long	3880360185
	.long	3189567898
	.long	338223984
	.long	3230017015
	.long	391995809
	.long	3189567294
	.long	3855783416
	.long	3230017017
	.long	4186345433
	.long	3189567745
	.long	3061732952
	.long	3230017020
	.long	3682852050
	.long	3189567081
	.long	2251085560
	.long	3230017023
	.long	3057037137
	.long	3189567617
	.long	1423886760
	.long	3230017026
	.long	3352664894
	.long	3189567074
	.long	580181848
	.long	3230017029
	.long	1170568982
	.long	3189567927
	.long	4014983272
	.long	3230017031
	.long	671148579
	.long	3189567293
	.long	3138401368
	.long	3230017034
	.long	267496658
	.long	3189567304
	.long	2245448192
	.long	3230017037
	.long	1285167087
	.long	3189568066
	.long	1336168336
	.long	3230017040
	.long	4259909589
	.long	3189567788
	.long	410606184
	.long	3230017043
	.long	1494078902
	.long	3189568031
	.long	3763773256
	.long	3230017045
	.long	261935754
	.long	3189567685
	.long	2805778992
	.long	3230017048
	.long	3679585605
	.long	3189567195
	.long	1831634536
	.long	3230017051
	.long	809066513
	.long	3189567664
	.long	841383576
	.long	3230017054
	.long	385945238
	.long	3189567897
	.long	4130036912
	.long	3230017056
	.long	825579909
	.long	3189567603
	.long	3107703272
	.long	3230017059
	.long	3911295427
	.long	3189567514
	.long	2069393104
	.long	3230017062
	.long	3473784588
	.long	3189567461
	.long	1015149384
	.long	3230017065
	.long	629861200
	.long	3189567515
	.long	4239982216
	.long	3230017067
	.long	2659142760
	.long	3189567083
	.long	3153999632
	.long	3230017070
	.long	1251680713
	.long	3189567080
	.long	2052211384
	.long	3230017073
	.long	3168705163
	.long	3189567992
	.long	934659776
	.long	3230017076
	.long	1674797624
	.long	3189567952
	.long	4096354216
	.long	3230017078
	.long	437847902
	.long	3189567971
	.long	2947402064
	.long	3230017081
	.long	4024401533
	.long	3189567960
	.long	1782812400
	.long	3230017084
	.long	786902458
	.long	3189567873
	.long	602626840
	.long	3230017087
	.long	2593792633
	.long	3189567812
	.long	3701854136
	.long	3230017089
	.long	3989721221
	.long	3189567126
	.long	2490600968
	.long	3230017092
	.long	2545746866
	.long	3189567591
	.long	1263875768
	.long	3230017095
	.long	3115843521
	.long	3189567377
	.long	21719488
	.long	3230017098
	.long	1730226063
	.long	3189567307
	.long	3059140216
	.long	3230017100
	.long	690350922
	.long	3189567892
	.long	1786244008
	.long	3230017103
	.long	3350998585
	.long	3189567394
	.long	498038616
	.long	3230017106
	.long	1499123379
	.long	3189568082
	.long	3489531672
	.long	3230017108
	.long	2598665922
	.long	3189567118
	.long	2170828712
	.long	3230017111
	.long	2436376826
	.long	3189567935
	.long	836937048
	.long	3230017114
	.long	1977004902
	.long	3189568052
	.long	3782863816
	.long	3230017116
	.long	4027224619
	.long	3189567373
	.long	2418714096
	.long	3230017119
	.long	2616869856
	.long	3189567271
	.long	1039494712
	.long	3230017122
	.long	3775628133
	.long	3189567616
	.long	3940212336
	.long	3230017124
	.long	419018052
	.long	3189567909
	.long	2530971600
	.long	3230017127
	.long	150919657
	.long	3189567379
	.long	1106778856
	.long	3230017130
	.long	2926254537
	.long	3189567139
	.long	3962640312
	.long	3230017132
	.long	1610863810
	.long	3189567216
	.long	2508660136
	.long	3230017135
	.long	1166015244
	.long	3189567672
	.long	1039844240
	.long	3230017138
	.long	645159638
	.long	3189567685
	.long	3851198376
	.long	3230017140
	.long	2449439581
	.long	3189567695
	.long	2352826264
	.long	3230017143
	.long	1619104640
	.long	3189567458
	.long	839733360
	.long	3230017146
	.long	2427070371
	.long	3189567165
	.long	3606924968
	.long	3230017148
	.long	2166480890
	.long	3189567544
	.long	2064504368
	.long	3230017151
	.long	2283025905
	.long	3189567907
	.long	507476584
	.long	3230017154
	.long	3923356754
	.long	3189567272
	.long	3230846472
	.long	3230017156
	.long	805331677
	.long	3189567533
	.long	1644716880
	.long	3230017159
	.long	3790469685
	.long	3189567406
	.long	44092384
	.long	3230017162
	.long	2689788214
	.long	3189567653
	.long	2723977432
	.long	3230017164
	.long	2762250221
	.long	3189567072
	.long	1094474416
	.long	3230017167
	.long	2781925193
	.long	3189567716
	.long	3745554800
	.long	3230017169
	.long	593120878
	.long	3189567817
	.long	2087320704
	.long	3230017172
	.long	403414988
	.long	3189568021
	.long	414776008
	.long	3230017175
	.long	359307328
	.long	3189567387
	.long	3022924432
	.long	3230017177
	.long	3373547966
	.long	3189567572
	.long	1321867688
	.long	3230017180
	.long	3978711340
	.long	3189567805
	.long	3901576528
	.long	3230017182
	.long	2765663238
	.long	3189568046
	.long	2172152384
	.long	3230017185
	.long	1181150766
	.long	3189568053
	.long	428598440
	.long	3230017188
	.long	2473022131
	.long	3189567471
	.long	2965917728
	.long	3230017190
	.long	1621744488
	.long	3189567971
	.long	1194211288
	.long	3230017193
	.long	2931299013
	.long	3189567190
	.long	3703449168
	.long	3230017195
	.long	873154950
	.long	3189568039
	.long	1903732144
	.long	3230017198
	.long	1510784525
	.long	3189567521
	.long	90062704
	.long	3230017201
	.long	2918231590
	.long	3189568086
	.long	2557443240
	.long	3230017203
	.long	3329944838
	.long	3189567475
	.long	715974088
	.long	3230017206
	.long	2679715410
	.long	3189568021
	.long	3155624672
	.long	3230017208
	.long	1841110143
	.long	3189567518
	.long	1286495072
	.long	3230017211
	.long	3881638924
	.long	3189567497
	.long	3698554440
	.long	3230017213
	.long	167906931
	.long	3189567169
	.long	1801902600
	.long	3230017216
	.long	1468264167
	.long	3189567597
	.long	4186508448
	.long	3230017218
	.long	2400624638
	.long	3189567691
	.long	2262471560
	.long	3230017221
	.long	3796182064
	.long	3189567357
	.long	324793264
	.long	3230017224
	.long	2801238958
	.long	3189567581
	.long	2668474776
	.long	3230017226
	.long	2273142813
	.long	3189567436
	.long	703615288
	.long	3230017229
	.long	831867571
	.long	3189567237
	.long	3020183048
	.long	3230017231
	.long	2238048829
	.long	3189567595
	.long	1028277008
	.long	3230017234
	.long	1163763070
	.long	3189567452
	.long	3317865168
	.long	3230017236
	.long	1206450788
	.long	3189567228
	.long	1299046216
	.long	3230017239
	.long	737162226
	.long	3189567879
	.long	3561787920
	.long	3230017241
	.long	2433253277
	.long	3189567901
	.long	1516188728
	.long	3230017244
	.long	3561277099
	.long	3189567506
	.long	3752216144
	.long	3230017246
	.long	1385095507
	.long	3189567674
	.long	1679968384
	.long	3230017249
	.long	829462408
	.long	3189567156
	.long	3889412704
	.long	3230017251
	.long	2939266725
	.long	3189567673
	.long	1790647080
	.long	3230017254
	.long	4214100827
	.long	3189567848
	.long	3973638544
	.long	3230017256
	.long	2537954451
	.long	3189567376
	.long	1848484816
	.long	3230017259
	.long	1128740312
	.long	3189567098
	.long	4005152680
	.long	3230017261
	.long	1932232434
	.long	3189568053
	.long	1853739648
	.long	3230017264
	.long	1359710694
	.long	3189567457
	.long	3984212256
	.long	3230017266
	.long	1268482287
	.long	3189567990
	.long	1806667776
	.long	3230017269
	.long	839689017
	.long	3189567631
	.long	3911072520
	.long	3230017271
	.long	3727165601
	.long	3189567921
	.long	1707523520
	.long	3230017274
	.long	1836550952
	.long	3189567895
	.long	3785986872
	.long	3230017276
	.long	172951407
	.long	3189567220
	.long	1556559360
	.long	3230017279
	.long	1166449891
	.long	3189567294
	.long	3609206856
	.long	3230017281
	.long	28625366
	.long	3189567222
	.long	1354025920
	.long	3230017284
	.long	4038426582
	.long	3189567978
	.long	3380982208
	.long	3230017286
	.long	2756284659
	.long	3189567362
	.long	1100172048
	.long	3230017289
	.long	848830252
	.long	3189567230
	.long	3101560856
	.long	3230017291
	.long	1868170309
	.long	3189567423
	.long	795244752
	.long	3230017294
	.long	3128260908
	.long	3189567885
	.long	2771188936
	.long	3230017296
	.long	1691261279
	.long	3189567706
	.long	439489304
	.long	3230017299
	.long	2886711298
	.long	3189567236
	.long	2390110824
	.long	3230017301
	.long	1311431101
	.long	3189567133
	.long	33149176
	.long	3230017304
	.long	3042183093
	.long	3189567401
	.long	1958569120
	.long	3230017306
	.long	3174208880
	.long	3189567462
	.long	3871433416
	.long	3230017308
	.long	302374021
	.long	3189567243
	.long	1476804712
	.long	3230017311
	.long	712531519
	.long	3189567241
	.long	3364647440
	.long	3230017313
	.long	3705318448
	.long	3189567568
	.long	945056744
	.long	3230017316
	.long	2424171915
	.long	3189568016
	.long	2807996856
	.long	3230017318
	.long	199511961
	.long	3189567094
	.long	363562688
	.long	3230017321
	.long	3766234060
	.long	3189567214
	.long	2201718256
	.long	3230017323
	.long	1956195829
	.long	3189567592
	.long	4027525584
	.long	3230017325
	.long	2891794585
	.long	3189567375
	.long	1546046576
	.long	3230017328
	.long	843173017
	.long	3189567760
	.long	3347244936
	.long	3230017330
	.long	1609532414
	.long	3189567955
	.long	841215080
	.long	3230017333
	.long	3356704285
	.long	3189567294
	.long	2617920488
	.long	3230017335
	.long	1641729240
	.long	3189567346
	.long	87455368
	.long	3230017338
	.long	1455753222
	.long	3189567878
	.long	1839783016
	.long	3230017340
	.long	1936589978
	.long	3189567944
	.long	3579964728
	.long	3230017342
	.long	1006586129
	.long	3189567993
	.long	1013061704
	.long	3230017345
	.long	2766825484
	.long	3189567880
	.long	2729036928
	.long	3230017347
	.long	1888685184
	.long	3189567954
	.long	137984104
	.long	3230017350
	.long	848046306
	.long	3189568090
	.long	1829866024
	.long	3230017352
	.long	4178224056
	.long	3189567750
	.long	3509743488
	.long	3230017354
	.long	2637072474
	.long	3189567070
	.long	882677184
	.long	3230017357
	.long	3133305384
	.long	3189567936
	.long	2538629624
	.long	3230017359
	.long	1343200028
	.long	3189567905
	.long	4182661312
	.long	3230017361
	.long	107195460
	.long	3189567427
	.long	1519832648
	.long	3230017364
	.long	602154914
	.long	3189567857
	.long	3140105848
	.long	3230017366
	.long	659160740
	.long	3189567417
	.long	453573824
	.long	3230017369
	.long	236889930
	.long	3189567396
	.long	2050198592
	.long	3230017371
	.long	1175634921
	.long	3189567087
	.long	3635040168
	.long	3230017373
	.long	1241855527
	.long	3189567941
	.long	913158504
	.long	3230017376
	.long	3947904414
	.long	3189567477
	.long	2474515312
	.long	3230017378
	.long	1206979822
	.long	3189567534
	.long	4024170344
	.long	3230017380
	.long	1847397805
	.long	3189567152
	.long	1267183240
	.long	3230017383
	.long	2916539301
	.long	3189567754
	.long	2793515456
	.long	3230017385
	.long	2568213263
	.long	3189568078
	.long	13259168
	.long	3230017388
	.long	3003245330
	.long	3189567303
	.long	1516375624
	.long	3230017390
	.long	3472633477
	.long	3189567153
	.long	3007924104
	.long	3230017392
	.long	532406289
	.long	3189567855
	.long	192963816
	.long	3230017395
	.long	4149310663
	.long	3189567165
	.long	1661455728
	.long	3230017397
	.long	946970842
	.long	3189567598
	.long	3118458864
	.long	3230017399
	.long	2040517972
	.long	3189567254
	.long	269032128
	.long	3230017402
	.long	3669971831
	.long	3189568072
	.long	1703136256
	.long	3230017404
	.long	2375197389
	.long	3189567687
	.long	3125829976
	.long	3230017406
	.long	296757744
	.long	3189567673
	.long	242171944
	.long	3230017409
	.long	611424102
	.long	3189567452
	.long	1642122608
	.long	3230017411
	.long	1700968423
	.long	3189567419
	.long	3030740440
	.long	3230017413
	.long	2947762668
	.long	3189567968
	.long	113083840
	.long	3230017416
	.long	690901756
	.long	3189567496
	.long	1479112984
	.long	3230017418
	.long	121139903
	.long	3189567571
	.long	2833886088
	.long	3230017420
	.long	173386055
	.long	3189567866
	.long	4177428584
	.long	3230017422
	.long	2964718132
	.long	3189567231
	.long	1214798496
	.long	3230017425
	.long	541562024
	.long	3189567842
	.long	2535955680
	.long	3230017427
	.long	3368765612
	.long	3189568076
	.long	3845958008
	.long	3230017429
	.long	3194541507
	.long	3189567689
	.long	849863256
	.long	3230017432
	.long	3443356552
	.long	3189567858
	.long	2137631024
	.long	3230017434
	.long	1180679397
	.long	3189567138
	.long	3414318912
	.long	3230017436
	.long	2261929999
	.long	3189567651
	.long	384984464
	.long	3230017439
	.long	954057098
	.long	3189567971
	.long	1639587024
	.long	3230017441
	.long	1346995839
	.long	3189567263
	.long	2883183944
	.long	3230017443
	.long	3253373727
	.long	3189567382
	.long	4115799808
	.long	3230017445
	.long	4143791142
	.long	3189567800
	.long	1042491824
	.long	3230017448
	.long	4240432954
	.long	3189567700
	.long	2253218992
	.long	3230017450
	.long	1891708033
	.long	3189568072
	.long	3453038360
	.long	3230017452
	.long	710292621
	.long	3189567662
	.long	347006880
	.long	3230017455
	.long	2712254611
	.long	3189567116
	.long	1525083304
	.long	3230017457
	.long	349583280
	.long	3189568004
	.long	2692324448
	.long	3230017459
	.long	1803227864
	.long	3189567740
	.long	3848754312
	.long	3230017461
	.long	3644606976
	.long	3189567826
	.long	699429536
	.long	3230017464
	.long	1067153346
	.long	3189567751
	.long	1834308568
	.long	3230017466
	.long	1335350692
	.long	3189567081
	.long	2958447872
	.long	3230017468
	.long	1803696599
	.long	3189567557
	.long	4071871152
	.long	3230017470
	.long	1232152547
	.long	3189568017
	.long	879634736
	.long	3230017473
	.long	1455437901
	.long	3189567512
	.long	1971696744
	.long	3230017475
	.long	3708110750
	.long	3189567399
	.long	3053113344
	.long	3230017477
	.long	3907324819
	.long	3189567290
	.long	4123907920
	.long	3230017479
	.long	4080000314
	.long	3189567141
	.long	889136480
	.long	3230017482
	.long	1286519001
	.long	3189567298
	.long	1938756848
	.long	3230017484
	.long	1292347423
	.long	3189567513
	.long	2977824880
	.long	3230017486
	.long	1159725249
	.long	3189568015
	.long	4006363664
	.long	3230017488
	.long	895621348
	.long	3189567502
	.long	729428896
	.long	3230017491
	.long	1212894155
	.long	3189567280
	.long	1736978096
	.long	3230017493
	.long	296117275
	.long	3189567210
	.long	2734066816
	.long	3230017495
	.long	2463481211
	.long	3189567796
	.long	3720717848
	.long	3230017497
	.long	76524950
	.long	3189567159
	.long	401986584
	.long	3230017500
	.long	1718551671
	.long	3189567216
	.long	1367830248
	.long	3230017502
	.long	3568874946
	.long	3189567584
	.long	2323304104
	.long	3230017504
	.long	2763456396
	.long	3189567666
	.long	3268430632
	.long	3230017506
	.long	1208592501
	.long	3189567715
	.long	4203232240
	.long	3230017508
	.long	4148835618
	.long	3189567851
	.long	832763976
	.long	3230017511
	.long	610418007
	.long	3189567082
	.long	1746982680
	.long	3230017513
	.long	3035714190
	.long	3189567431
	.long	2650943256
	.long	3230017515
	.long	85931336
	.long	3189567843
	.long	3544667832
	.long	3230017517
	.long	3006685957
	.long	3189567286
	.long	133211144
	.long	3230017520
	.long	878859600
	.long	3189567873
	.long	1006529776
	.long	3230017522
	.long	854333969
	.long	3189567724
	.long	1869678336
	.long	3230017524
	.long	3058646748
	.long	3189567155
	.long	2722678648
	.long	3230017526
	.long	360733640
	.long	3189567693
	.long	3565552488
	.long	3230017528
	.long	3015564990
	.long	3189568013
	.long	103354256
	.long	3230017531
	.long	2286095909
	.long	3189568081
	.long	926040176
	.long	3230017533
	.long	1899270393
	.long	3189567137
	.long	1738664488
	.long	3230017535
	.long	1377641292
	.long	3189567833
	.long	2541248696
	.long	3230017537
	.long	4266686523
	.long	3189568077
	.long	3333814224
	.long	3230017539
	.long	4018775563
	.long	3189567218
	.long	4116382408
	.long	3230017541
	.long	3373913644
	.long	3189567103
	.long	594007232
	.long	3230017544
	.long	2818265402
	.long	3189568021
	.long	1356644520
	.long	3230017546
	.long	1075601108
	.long	3189567716
	.long	2109348112
	.long	3230017548
	.long	3342127702
	.long	3189567566
	.long	2852139088
	.long	3230017550
	.long	2910355434
	.long	3189567504
	.long	3585038464
	.long	3230017552
	.long	611858749
	.long	3189567074
	.long	13099880
	.long	3230017555
	.long	1954698831
	.long	3189567516
	.long	726278816
	.long	3230017557
	.long	550712630
	.long	3189567710
	.long	1429628792
	.long	3230017559
	.long	3672474493
	.long	3189567278
	.long	2123170544
	.long	3230017561
	.long	1160875209
	.long	3189567651
	.long	2806924760
	.long	3230017563
	.long	2242524344
	.long	3189568000
	.long	3480912064
	.long	3230017565
	.long	2823138691
	.long	3189567327
	.long	4145152992
	.long	3230017567
	.long	3521079748
	.long	3189567568
	.long	504700744
	.long	3230017570
	.long	1136670137
	.long	3189567486
	.long	1149510328
	.long	3230017572
	.long	806933442
	.long	3189567823
	.long	1784634816
	.long	3230017574
	.long	1606084669
	.long	3189567242
	.long	2410094488
	.long	3230017576
	.long	2841342625
	.long	3189567480
	.long	3025909584
	.long	3230017578
	.long	984188059
	.long	3189567239
	.long	3632100264
	.long	3230017580
	.long	3011730434
	.long	3189567337
	.long	4228686632
	.long	3230017582
	.long	328432041
	.long	3189567676
	.long	520721440
	.long	3230017585
	.long	2057225622
	.long	3189567268
	.long	1098159248
	.long	3230017587
	.long	3150727306
	.long	3189567348
	.long	1666052680
	.long	3230017589
	.long	3291380622
	.long	3189567307
	.long	2224421584
	.long	3230017591
	.long	3570771984
	.long	3189567798
	.long	2773285760
	.long	3230017593
	.long	2427604440
	.long	3189567698
	.long	3312664936
	.long	3230017595
	.long	3733848165
	.long	3189567211
	.long	3842578768
	.long	3230017597
	.long	2673912350
	.long	3189567904
	.long	68079584
	.long	3230017600
	.long	960995892
	.long	3189567616
	.long	579121512
	.long	3230017602
	.long	4150493479
	.long	3189567659
	.long	1080756744
	.long	3230017604
	.long	1054941272
	.long	3189567735
	.long	1573004704
	.long	3230017606
	.long	2574365292
	.long	3189568007
	.long	2055884768
	.long	3230017608
	.long	2507498673
	.long	3189567092
	.long	2529416224
	.long	3230017610
	.long	2082176323
	.long	3189567207
	.long	2993618320
	.long	3230017612
	.long	3719982601
	.long	3189568058
	.long	3448510256
	.long	3230017614
	.long	4104247904
	.long	3189567897
	.long	3894111152
	.long	3230017616
	.long	3735557011
	.long	3189567647
	.long	35472776
	.long	3230017619
	.long	2914260983
	.long	3189567862
	.long	462548736
	.long	3230017621
	.long	1334094042
	.long	3189567734
	.long	880390688
	.long	3230017623
	.long	1291097379
	.long	3189567171
	.long	1289017512
	.long	3230017625
	.long	1627119803
	.long	3189567853
	.long	1688448064
	.long	3230017627
	.long	3886873723
	.long	3189567117
	.long	2078701104
	.long	3230017629
	.long	627887757
	.long	3189567206
	.long	2459795360
	.long	3230017631
	.long	1722013861
	.long	3189567101
	.long	2831749488
	.long	3230017633
	.long	4163262354
	.long	3189567678
	.long	3194582104
	.long	3230017635
	.long	4170237688
	.long	3189567639
	.long	3548311752
	.long	3230017637
	.long	47563772
	.long	3189567637
	.long	3892956928
	.long	3230017639
	.long	2990136007
	.long	3189567231
	.long	4228536056
	.long	3230017641
	.long	4294532248
	.long	3189567995
	.long	260100240
	.long	3230017644
	.long	2060709623
	.long	3189567397
	.long	577602376
	.long	3230017646
	.long	1488525373
	.long	3189568044
	.long	886093456
	.long	3230017648
	.long	1657840200
	.long	3189567474
	.long	1185591680
	.long	3230017650
	.long	3155771782
	.long	3189567399
	.long	1476115208
	.long	3230017652
	.long	2195162329
	.long	3189567591
	.long	1757682144
	.long	3230017654
	.long	1292806663
	.long	3189567956
	.long	2030310544
	.long	3230017656
	.long	2151451208
	.long	3189567540
	.long	2294018392
	.long	3230017658
	.long	2224122735
	.long	3189567630
	.long	2548823632
	.long	3230017660
	.long	3554417607
	.long	3189567684
	.long	2794744152
	.long	3230017662
	.long	3831650627
	.long	3189567409
	.long	3031797776
	.long	3230017664
	.long	4024246224
	.long	3189567811
	.long	3260002296
	.long	3230017666
	.long	2530219738
	.long	3189567128
	.long	3479375416
	.long	3230017668
	.long	1618145996
	.long	3189568024
	.long	3689934832
	.long	3230017670
	.long	392379700
	.long	3189567378
	.long	3891698144
	.long	3230017672
	.long	55874694
	.long	3189567574
	.long	4084682928
	.long	3230017674
	.long	4294251475
	.long	3189567312
	.long	4268906688
	.long	3230017676
	.long	1899705040
	.long	3189567783
	.long	149419600
	.long	3230017679
	.long	357699389
	.long	3189567568
	.long	316173656
	.long	3230017681
	.long	3025711727
	.long	3189567790
	.long	474218920
	.long	3230017683
	.long	2921891821
	.long	3189568046
	.long	623572704
	.long	3230017685
	.long	1716968749
	.long	3189567453
	.long	764252248
	.long	3230017687
	.long	2393171526
	.long	3189567748
	.long	896274768
	.long	3230017689
	.long	1343317993
	.long	3189567172
	.long	1019657400
	.long	3230017691
	.long	1388235839
	.long	3189567661
	.long	1134417256
	.long	3230017693
	.long	2766147125
	.long	3189567706
	.long	1240571384
	.long	3230017695
	.long	2162068842
	.long	3189567499
	.long	1338136776
	.long	3230017697
	.long	4075455588
	.long	3189567932
	.long	1427130392
	.long	3230017699
	.long	1579647664
	.long	3189567555
	.long	1507569120
	.long	3230017701
	.long	3785749868
	.long	3189567715
	.long	1579469816
	.long	3230017703
	.long	2639950365
	.long	3189567469
	.long	1642849272
	.long	3230017705
	.long	3611501026
	.long	3189567698
	.long	1697724240
	.long	3230017707
	.long	15422953
	.long	3189568066
	.long	1744111424
	.long	3230017709
	.long	2642213241
	.long	3189568062
	.long	1782027472
	.long	3230017711
	.long	143606300
	.long	3189568058
	.long	1811488992
	.long	3230017713
	.long	501328690
	.long	3189567276
	.long	1832512520
	.long	3230017715
	.long	3258054578
	.long	3189567915
	.long	1845114576
	.long	3230017717
	.long	1297851139
	.long	3189568007
	.long	1849311616
	.long	3230017719
	.long	3589200000
	.long	3189567580
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
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
	.long	2147483648
	.long	2846560486
	.long	3210737666
	.long	2729511872
	.long	3211784202
	.long	3956813460
	.long	3212307985
	.long	1796237193
	.long	3212828714
	.long	1562748889
	.long	3213092649
	.long	1602292343
	.long	3213352007
	.long	745814092
	.long	3213610865
	.long	2969306084
	.long	3213869224
	.long	3953060839
	.long	3214006263
	.long	1128703349
	.long	3214134948
	.long	1371067273
	.long	3214263386
	.long	131445019
	.long	3214391579
	.long	1427948820
	.long	3214519527
	.long	665817496
	.long	3214647232
	.long	1817459908
	.long	3214774694
	.long	242758400
	.long	3214901915
	.long	2082038134
	.long	3214981455
	.long	2300828032
	.long	3215044825
	.long	571795571
	.long	3215108076
	.long	3120479584
	.long	3215171207
	.long	3276640540
	.long	3215234220
	.long	2949178581
	.long	3215297115
	.long	4036279704
	.long	3215359892
	.long	4130528508
	.long	3215422552
	.long	813954767
	.long	3215485096
	.long	248046571
	.long	3215547523
	.long	4288926261
	.long	3215609833
	.long	1897492927
	.long	3215672029
	.long	3499237165
	.long	3215734109
	.long	2329611090
	.long	3215796075
	.long	203907094
	.long	3215857927
	.long	3222364838
	.long	3215919664
	.long	295408387
	.long	3215981289
	.long	899212401
	.long	3216012696
	.long	2606049022
	.long	3216043395
	.long	4003691233
	.long	3216074038
	.long	1675303236
	.long	3216104626
	.long	789189655
	.long	3216135158
	.long	2213928479
	.long	3216165634
	.long	2518405585
	.long	3216196055
	.long	2561783534
	.long	3216226421
	.long	3198568176
	.long	3216256732
	.long	983674944
	.long	3216286989
	.long	1057364031
	.long	3216317191
	.long	4260371496
	.long	3216347338
	.long	2838974660
	.long	3216377432
	.long	1919860988
	.long	3216407472
	.long	2330291020
	.long	3216437458
	.long	598130195
	.long	3216467391
	.long	1836782285
	.long	3216497270
	.long	2565351466
	.long	3216527096
	.long	3593575201
	.long	3216556869
	.long	1431920350
	.long	3216586590
	.long	1176515501
	.long	3216616258
	.long	3624279252
	.long	3216645873
	.long	977982819
	.long	3216675437
	.long	2616083454
	.long	3216704948
	.long	733015457
	.long	3216734408
	.long	403990373
	.long	3216763816
	.long	2405156675
	.long	3216793172
	.long	3213628385
	.long	3216822477
	.long	3597448029
	.long	3216851731
	.long	25680167
	.long	3216880935
	.long	1848308454
	.long	3216910087
	.long	1231492199
	.long	3216939189
	.long	3222364838
	.long	3216968240
	.long	4274224607
	.long	3216997241
	.long	836496059
	.long	3217026193
	.long	1119829323
	.long	3217043131
	.long	315251054
	.long	3217057557
	.long	518461891
	.long	3217071958
	.long	2094042713
	.long	3217086334
	.long	1109735857
	.long	3217100686
	.long	2221359788
	.long	3217115013
	.long	1492952593
	.long	3217129316
	.long	3576653728
	.long	3217143594
	.long	237880003
	.long	3217157849
	.long	420109003
	.long	3217172079
	.long	180120274
	.long	3217186285
	.long	4162843939
	.long	3217200466
	.long	4126536273
	.long	3217214624
	.long	417628115
	.long	3217228759
	.long	1970736719
	.long	3217242869
	.long	538873716
	.long	3217256956
	.long	758227830
	.long	3217271019
	.long	2968307242
	.long	3217285058
	.long	3211951040
	.long	3217299074
	.long	1825275166
	.long	3217313067
	.long	3437683677
	.long	3217327036
	.long	4086978021
	.long	3217340982
	.long	4104270002
	.long	3217354905
	.long	3819025464
	.long	3217368805
	.long	3559075183
	.long	3217382682
	.long	3650625670
	.long	3217396536
	.long	123302585
	.long	3217410368
	.long	1890030548
	.long	3217424176
	.long	682272600
	.long	3217437962
	.long	1114811729
	.long	3217451725
	.long	3505892050
	.long	3217465465
	.long	3877229085
	.long	3217479183
	.long	2543954549
	.long	3217492879
	.long	4114626469
	.long	3217506552
	.long	311370023
	.long	3217520204
	.long	34658569
	.long	3217533833
	.long	3593519732
	.long	3217547439
	.long	2705545195
	.long	3217561024
	.long	1971736889
	.long	3217574587
	.long	1696647441
	.long	3217588128
	.long	2183357019
	.long	3217601647
	.long	3733482817
	.long	3217615144
	.long	2352221150
	.long	3217628620
	.long	2633258676
	.long	3217642074
	.long	578912458
	.long	3217655507
	.long	780008323
	.long	3217668918
	.long	3530988084
	.long	3217682307
	.long	534951267
	.long	3217695676
	.long	673467853
	.long	3217709023
	.long	4236783385
	.long	3217722348
	.long	2923827788
	.long	3217735653
	.long	1317060595
	.long	3217748937
	.long	3997577740
	.long	3217762199
	.long	2660218276
	.long	3217775441
	.long	1883376699
	.long	3217788662
	.long	1949142230
	.long	3217801862
	.long	3138274530
	.long	3217815041
	.long	1435244737
	.long	3217828200
	.long	1413145637
	.long	3217841338
	.long	3348797986
	.long	3217854455
	.long	3222758658
	.long	3217867552
	.long	1309263324
	.long	3217880629
	.long	2176234471
	.long	3217893685
	.long	1800387473
	.long	3217906721
	.long	452140382
	.long	3217919737
	.long	2695621757
	.long	3217932732
	.long	208809263
	.long	3217945708
	.long	1848308454
	.long	3217958663
	.long	3584589359
	.long	3217971598
	.long	1386895968
	.long	3217984514
	.long	4108155659
	.long	3217997409
	.long	3420215606
	.long	3218010285
	.long	3878621277
	.long	3218023141
	.long	1447787319
	.long	3218035978
	.long	680874053
	.long	3218048795
	.long	1834892843
	.long	3218061592
	.long	870713292
	.long	3218074370
	.long	1168986141
	.long	3218083436
	.long	3245089583
	.long	3218089805
	.long	2494877761
	.long	3218096165
	.long	3338922950
	.long	3218102515
	.long	1607293197
	.long	3218108856
	.long	1719424943
	.long	3218115187
	.long	3799224547
	.long	3218121508
	.long	3675071677
	.long	3218127820
	.long	1469757259
	.long	3218134123
	.long	1600486820
	.long	3218140416
	.long	4188949205
	.long	3218146699
	.long	766352567
	.long	3218152974
	.long	43231408
	.long	3218159239
	.long	2139646042
	.long	3218165494
	.long	2880153102
	.long	3218171740
	.long	2383743325
	.long	3218177977
	.long	768877423
	.long	3218184205
	.long	2448456521
	.long	3218190423
	.long	3244923387
	.long	3218196632
	.long	3275167422
	.long	3218202832
	.long	2655560435
	.long	3218209023
	.long	1501959692
	.long	3218215205
	.long	4224678246
	.long	3218221377
	.long	2348618756
	.long	3218227541
	.long	283080250
	.long	3218233696
	.long	2436859197
	.long	3218239841
	.long	333350563
	.long	3218245978
	.long	2675321801
	.long	3218252105
	.long	985177381
	.long	3218258224
	.long	3964700035
	.long	3218264333
	.long	3135315249
	.long	3218270434
	.long	2902865164
	.long	3218276526
	.long	3377742213
	.long	3218282609
	.long	374891912
	.long	3218288684
	.long	2593684821
	.long	3218294749
	.long	1553148222
	.long	3218300806
	.long	1656739931
	.long	3218306854
	.long	3012481825
	.long	3218312893
	.long	1432962538
	.long	3218318924
	.long	1320242026
	.long	3218324946
	.long	2780952336
	.long	3218330959
	.long	1626300240
	.long	3218336964
	.long	2256971748
	.long	3218342960
	.long	483265518
	.long	3218348948
	.long	704964628
	.long	3218354927
	.long	3026437244
	.long	3218360897
	.long	3256639170
	.long	3218366859
	.long	1499050969
	.long	3218372813
	.long	2151680465
	.long	3218378758
	.long	1022163351
	.long	3218384695
	.long	2507634849
	.long	3218390623
	.long	2414862975
	.long	3218396543
	.long	845152874
	.long	3218402455
	.long	2194349235
	.long	3218408358
	.long	2267936813
	.long	3218414253
	.long	1165944702
	.long	3218420140
	.long	3282948707
	.long	3218426018
	.long	128204514
	.long	3218431889
	.long	390421100
	.long	3218437751
	.long	4167959282
	.long	3218443604
	.long	2968834018
	.long	3218449450
	.long	1185553177
	.long	3218455288
	.long	3210217930
	.long	3218461117
	.long	549623114
	.long	3218466939
	.long	1890030548
	.long	3218472752
	.long	3032400188
	.long	3218478557
	.long	4072261525
	.long	3218484354
	.long	3450874911
	.long	1070995813
	.long	2331021804
	.long	1070990032
	.long	1029453230
	.long	1070984259
	.long	3747176240
	.long	1070978493
	.long	1800682930
	.long	1070972736
	.long	3686719393
	.long	1070966986
	.long	722545241
	.long	1070961245
	.long	1405669883
	.long	1070955511
	.long	1349079372
	.long	1070949785
	.long	461103529
	.long	1070944067
	.long	2945413886
	.long	1070938356
	.long	121152472
	.long	1070932654
	.long	487700860
	.long	1070926959
	.long	3954874384
	.long	1070921271
	.long	1842920138
	.long	1070915592
	.long	2652318780
	.long	1070909920
	.long	1999011482
	.long	1070904256
	.long	4089234463
	.long	1070898599
	.long	244680560
	.long	1070892951
	.long	3262235675
	.long	1070887309
	.long	169336595
	.long	1070881676
	.long	3763576649
	.long	1070876049
	.long	1073096258
	.long	1070870431
	.long	601221319
	.long	1070864820
	.long	2261690268
	.long	1070859216
	.long	1673619518
	.long	1070853620
	.long	3046403514
	.long	1070848031
	.long	1999843716
	.long	1070842450
	.long	2744015967
	.long	1070836876
	.long	899399508
	.long	1070831310
	.long	676744370
	.long	1070825751
	.long	1992167703
	.long	1070820199
	.long	467152011
	.long	1070814655
	.long	313445281
	.long	1070809118
	.long	1448157352
	.long	1070803588
	.long	3788725476
	.long	1070798065
	.long	2957945300
	.long	1070792550
	.long	3168871046
	.long	1070787042
	.long	44944622
	.long	1070781542
	.long	2094830420
	.long	1070776048
	.long	647642559
	.long	1070770562
	.long	4212681596
	.long	1070765082
	.long	4119694493
	.long	1070759610
	.long	288676754
	.long	1070754146
	.long	1229870791
	.long	1070748688
	.long	2568927817
	.long	1070743237
	.long	4226775419
	.long	1070737793
	.long	1829681364
	.long	1070732357
	.long	3889121191
	.long	1070726927
	.long	1737005556
	.long	1070721505
	.long	3885417029
	.long	1070716089
	.long	1666870165
	.long	1070710681
	.long	3594048319
	.long	1070705279
	.long	1000063738
	.long	1070699885
	.long	2398194399
	.long	1070694497
	.long	3417111411
	.long	1070689116
	.long	3980746691
	.long	1070683742
	.long	4013324163
	.long	1070678375
	.long	3439358267
	.long	1070673015
	.long	2183652476
	.long	1070667662
	.long	171297820
	.long	1070662316
	.long	1622638721
	.long	1070656976
	.long	2168369647
	.long	1070651643
	.long	1734435555
	.long	1070646317
	.long	247063161
	.long	1070640998
	.long	1927726804
	.long	1070635685
	.long	2408245143
	.long	1070630379
	.long	1615681634
	.long	1070625080
	.long	3772343130
	.long	1070619787
	.long	215909304
	.long	1070614502
	.long	3759169634
	.long	1070609222
	.long	1445381777
	.long	1070603950
	.long	1792910451
	.long	1070598684
	.long	870975437
	.long	1070590754
	.long	3194096222
	.long	1070580248
	.long	1823716217
	.long	1070569756
	.long	913800688
	.long	1070559277
	.long	323875369
	.long	1070548811
	.long	4208958430
	.long	1070538357
	.long	3839688678
	.long	1070527917
	.long	3372126730
	.long	1070517490
	.long	2667883253
	.long	1070507076
	.long	1589083690
	.long	1070496675
	.long	4293333010
	.long	1070486286
	.long	2053843986
	.long	1070475911
	.long	3324205749
	.long	1070465548
	.long	3673610217
	.long	1070455198
	.long	2966718784
	.long	1070444861
	.long	1068692560
	.long	1070434537
	.long	2140157205
	.long	1070424225
	.long	1752298597
	.long	1070413926
	.long	4066729595
	.long	1070403639
	.long	360651144
	.long	1070393366
	.long	3386588242
	.long	1070383104
	.long	127747308
	.long	1070372856
	.long	3337621361
	.long	1070362619
	.long	380113
	.long	1070352396
	.long	2870475188
	.long	1070342184
	.long	3227997535
	.long	1070331985
	.long	943446189
	.long	1070321799
	.long	182758681
	.long	1070311625
	.long	817374162
	.long	1070301463
	.long	2719198427
	.long	1070291313
	.long	1465634365
	.long	1070281176
	.long	1224481606
	.long	1070271051
	.long	1869032404
	.long	1070260938
	.long	3273036717
	.long	1070250837
	.long	1015732707
	.long	1070240749
	.long	3561713742
	.long	1070230672
	.long	2196155141
	.long	1070220608
	.long	1089583089
	.long	1070210556
	.long	118003300
	.long	1070200516
	.long	3452833475
	.long	1070190487
	.long	2381031992
	.long	1070180471
	.long	1074899573
	.long	1070170467
	.long	3707175295
	.long	1070160474
	.long	1566132617
	.long	1070150494
	.long	3120348377
	.long	1070140525
	.long	3953929663
	.long	1070130568
	.long	3946380940
	.long	1070120623
	.long	2977634725
	.long	1070110690
	.long	928049556
	.long	1070100769
	.long	1973375279
	.long	1070090859
	.long	1699849154
	.long	1070080961
	.long	4284063055
	.long	1070071074
	.long	1018125008
	.long	1070061200
	.long	374428300
	.long	1070051337
	.long	2235845747
	.long	1070041485
	.long	2190695046
	.long	1070031645
	.long	122671437
	.long	1070021817
	.long	210845780
	.long	1070012000
	.long	2339728060
	.long	1070002194
	.long	2099265479
	.long	1069992400
	.long	3669742467
	.long	1069982617
	.long	2641909617
	.long	1069972846
	.long	3196851002
	.long	1069963086
	.long	926113142
	.long	1069953338
	.long	11572342
	.long	1069943601
	.long	340530969
	.long	1069933875
	.long	1800682930
	.long	1069924160
	.long	4280111857
	.long	1069914456
	.long	3372322013
	.long	1069904764
	.long	3261138386
	.long	1069895083
	.long	3835803021
	.long	1069885413
	.long	690973253
	.long	1069875755
	.long	2306589128
	.long	1069866107
	.long	4278067876
	.long	1069856470
	.long	2201204065
	.long	1069846845
	.long	262102455
	.long	1069837231
	.long	2647241692
	.long	1069827627
	.long	658570708
	.long	1069818035
	.long	2778278092
	.long	1069808453
	.long	309052031
	.long	1069798883
	.long	1733817001
	.long	1069789323
	.long	2650961017
	.long	1069779774
	.long	2954203158
	.long	1069770236
	.long	2537624622
	.long	1069760709
	.long	1295667077
	.long	1069751193
	.long	3418098326
	.long	1069741687
	.long	210141498
	.long	1069732193
	.long	157244505
	.long	1069722709
	.long	3155274661
	.long	1069713235
	.long	510517079
	.long	1069703773
	.long	709476866
	.long	1069694321
	.long	3649073763
	.long	1069684879
	.long	636640577
	.long	1069675449
	.long	159725394
	.long	1069666029
	.long	2116286250
	.long	1069656619
	.long	2109656884
	.long	1069647220
	.long	38479793
	.long	1069637832
	.long	96704707
	.long	1069628454
	.long	2183652476
	.long	1069619086
	.long	1904013563
	.long	1069609729
	.long	3452748429
	.long	1069600382
	.long	2435216853
	.long	1069591046
	.long	3047045639
	.long	1069581720
	.long	894257944
	.long	1069572405
	.long	173141005
	.long	1069563100
	.long	785342784
	.long	1069553805
	.long	970708326
	.long	1069541521
	.long	2645916898
	.long	1069522972
	.long	2106590683
	.long	1069504444
	.long	3453588195
	.long	1069485936
	.long	2198475040
	.long	1069467449
	.long	2443390270
	.long	1069448982
	.long	3996141691
	.long	1069430535
	.long	2370203065
	.long	1069412109
	.long	1669613218
	.long	1069393703
	.long	1704071394
	.long	1069375317
	.long	2283901796
	.long	1069356951
	.long	3220050856
	.long	1069338605
	.long	29117219
	.long	1069320280
	.long	1113218225
	.long	1069301974
	.long	1990183441
	.long	1069283688
	.long	2473421172
	.long	1069265422
	.long	2376948508
	.long	1069247176
	.long	1515388672
	.long	1069228950
	.long	3998935692
	.long	1069210743
	.long	1053482595
	.long	1069192557
	.long	1085389880
	.long	1069174390
	.long	3911679148
	.long	1069156242
	.long	760030536
	.long	1069138115
	.long	38583924
	.long	1069120007
	.long	1566132617
	.long	1069101918
	.long	867088104
	.long	1069083849
	.long	2056379433
	.long	1069065799
	.long	659581512
	.long	1069047769
	.long	792781808
	.long	1069029758
	.long	2277675977
	.long	1069011766
	.long	641565395
	.long	1068993794
	.long	2256601
	.long	1068975841
	.long	183156961
	.long	1068957907
	.long	1008239542
	.long	1068939992
	.long	2302040698
	.long	1068922096
	.long	3889657667
	.long	1068904219
	.long	1301778891
	.long	1068886362
	.long	2954550821
	.long	1068868523
	.long	84804484
	.long	1068850704
	.long	1109791498
	.long	1068832903
	.long	1562410668
	.long	1068815121
	.long	1271074840
	.long	1068797358
	.long	64741293
	.long	1068779614
	.long	2067876735
	.long	1068761888
	.long	2815553124
	.long	1068744181
	.long	2138347284
	.long	1068726493
	.long	4162338635
	.long	1068708823
	.long	129237761
	.long	1068691173
	.long	2756122533
	.long	1068673540
	.long	3285762921
	.long	1068655926
	.long	1551389854
	.long	1068638331
	.long	1681725707
	.long	1068620754
	.long	3511047527
	.long	1068603195
	.long	2579184841
	.long	1068585655
	.long	3016419377
	.long	1068568133
	.long	363613719
	.long	1068550630
	.long	3047045639
	.long	1068533144
	.long	2313634884
	.long	1068515677
	.long	296456961
	.long	1068497513
	.long	1367328035
	.long	1068462651
	.long	3221665916
	.long	1068427825
	.long	1242258971
	.long	1068393036
	.long	3697798734
	.long	1068358282
	.long	1678104687
	.long	1068323565
	.long	3453858508
	.long	1068288883
	.long	116861598
	.long	1068254238
	.long	4234736672
	.long	1068219627
	.long	2606283437
	.long	1068185053
	.long	3506114812
	.long	1068150514
	.long	2324914545
	.long	1068116011
	.long	3044269692
	.long	1068081543
	.long	1056797460
	.long	1068047111
	.long	346010435
	.long	1068012714
	.long	601410756
	.long	1067978352
	.long	1513453498
	.long	1067944025
	.long	2773542775
	.long	1067909733
	.long	4074027864
	.long	1067875476
	.long	813232056
	.long	1067841255
	.long	1275317999
	.long	1067807068
	.long	860480111
	.long	1067772916
	.long	3559777258
	.long	1067738798
	.long	480292498
	.long	1067704716
	.long	4204867691
	.long	1067670667
	.long	1547459502
	.long	1067636654
	.long	797775935
	.long	1067602675
	.long	1656501562
	.long	1067568730
	.long	3825228429
	.long	1067534819
	.long	2711485098
	.long	1067500943
	.long	2313634884
	.long	1067467101
	.long	376973398
	.long	1067416219
	.long	672386641
	.long	1067348671
	.long	630691842
	.long	1067281191
	.long	3960777458
	.long	1067213778
	.long	1488394470
	.long	1067146434
	.long	1220920383
	.long	1067079157
	.long	2577548405
	.long	1067011947
	.long	684247743
	.long	1066944805
	.long	3553625811
	.long	1066877729
	.long	2020150230
	.long	1066810721
	.long	4099880293
	.long	1066743779
	.long	630721729
	.long	1066676905
	.long	3927125538
	.long	1066610096
	.long	535440936
	.long	1066543355
	.long	2773516144
	.long	1066476679
	.long	1486051399
	.long	1066410070
	.long	808661260
	.long	1066285262
	.long	2227658502
	.long	1066152307
	.long	1813447778
	.long	1066019484
	.long	2743640746
	.long	1065886792
	.long	3904192454
	.long	1065754231
	.long	4184355568
	.long	1065621801
	.long	2476667369
	.long	1065489502
	.long	1971904113
	.long	1065357333
	.long	3138267131
	.long	1065097372
	.long	341338964
	.long	1064833554
	.long	1954052322
	.long	1064569994
	.long	1498975029
	.long	1064306693
	.long	2190050752
	.long	1063782660
	.long	1440057502
	.long	1063257089
	.long	1432705161
	.long	1062208000
	.long	0
	.long	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.byte	0
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	2900053258
	.long	1070176668
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1208323809
	.long	3218079745
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	1431651269
	.long	1070945621
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
	.long	4294965279
	.long	3219128319
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
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294967295
	.long	3220176895
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
	.long	4294963200
	.long	2134900735
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
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	0
	.long	1040187392
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4294966272
	.long	4294967295
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	4277796864
	.long	1072049730
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
	.long	3164471296
	.long	1031600026
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
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
	.long	0
	.long	2134900736
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
	.type	__svml_dlog1p_ha_data_internal,@object
	.size	__svml_dlog1p_ha_data_internal,13952
	.align 32
_imldLnHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072660480
	.long	1486880768
	.long	1066410070
	.long	1813744607
	.long	3179892593
	.long	0
	.long	1072629760
	.long	377487360
	.long	1067416219
	.long	919019713
	.long	3179241129
	.long	0
	.long	1072599040
	.long	1513619456
	.long	1067944025
	.long	874573033
	.long	3178512940
	.long	0
	.long	1072570368
	.long	3221749760
	.long	1068427825
	.long	4181665006
	.long	3177478212
	.long	0
	.long	1072541696
	.long	4162322432
	.long	1068708823
	.long	627020255
	.long	1028629941
	.long	0
	.long	1072513024
	.long	183107584
	.long	1068957907
	.long	2376703469
	.long	1030233118
	.long	0
	.long	1072486400
	.long	1053425664
	.long	1069192557
	.long	696277142
	.long	1030474863
	.long	0
	.long	1072459776
	.long	3996123136
	.long	1069430535
	.long	2630798680
	.long	1028792016
	.long	0
	.long	1072435200
	.long	3452764160
	.long	1069600382
	.long	624954044
	.long	3177101741
	.long	0
	.long	1072409600
	.long	207650816
	.long	1069717971
	.long	3272735636
	.long	3175176575
	.long	0
	.long	1072386048
	.long	2647228416
	.long	1069827627
	.long	3594228712
	.long	1029303785
	.long	0
	.long	1072362496
	.long	2712010752
	.long	1069938736
	.long	3653242769
	.long	3176839013
	.long	0
	.long	1072338944
	.long	374439936
	.long	1070051337
	.long	4072775574
	.long	3176577495
	.long	0
	.long	1072316416
	.long	3707174912
	.long	1070160474
	.long	1486946159
	.long	1023930920
	.long	0
	.long	1072294912
	.long	1443954688
	.long	1070265993
	.long	293532967
	.long	3176278277
	.long	0
	.long	1072273408
	.long	127762432
	.long	1070372856
	.long	3404145447
	.long	3177023955
	.long	0
	.long	1072252928
	.long	2053832704
	.long	1070475911
	.long	1575076358
	.long	1029048544
	.long	0
	.long	1072232448
	.long	3194093568
	.long	1070580248
	.long	1864169120
	.long	1026866084
	.long	0
	.long	1072212992
	.long	3917201408
	.long	1070638340
	.long	2362145246
	.long	3175606197
	.long	0
	.long	1072193536
	.long	3417112576
	.long	1070689116
	.long	70087871
	.long	3174183577
	.long	0
	.long	1072175104
	.long	4226777088
	.long	1070737793
	.long	1620410586
	.long	3174700065
	.long	0
	.long	1072156672
	.long	3168870400
	.long	1070787042
	.long	311238082
	.long	1025781772
	.long	0
	.long	1072139264
	.long	2150580224
	.long	1070834092
	.long	1664262457
	.long	3175299224
	.long	0
	.long	1072120832
	.long	4095672320
	.long	1070884491
	.long	1657121015
	.long	3174674199
	.long	0
	.long	1072104448
	.long	2595577856
	.long	1070929805
	.long	2014006823
	.long	3175423830
	.long	0
	.long	1072087040
	.long	3747176448
	.long	1070978493
	.long	144991708
	.long	3171552042
	.long	0
	.long	1072070656
	.long	1050435584
	.long	1071024840
	.long	3386227432
	.long	1027876916
	.long	0
	.long	1072055296
	.long	255516672
	.long	1071068760
	.long	2637594316
	.long	1028049573
	.long	0
	.long	1072038912
	.long	1640783872
	.long	1071116120
	.long	893247007
	.long	1028452162
	.long	0
	.long	1072023552
	.long	2940411904
	.long	1071161011
	.long	813240633
	.long	1027664048
	.long	0
	.long	1072009216
	.long	882917376
	.long	1071203348
	.long	2376597551
	.long	3175828767
	.long	0
	.long	1071993856
	.long	213966848
	.long	1071249188
	.long	2977204125
	.long	1028350609
	.long	0
	.long	1071979520
	.long	2921504768
	.long	1071292428
	.long	523218347
	.long	1028007004
	.long	0
	.long	1071965184
	.long	3186655232
	.long	1071336119
	.long	2352907891
	.long	1026967097
	.long	0
	.long	1071951872
	.long	2653364224
	.long	1071377101
	.long	2453418583
	.long	3174349512
	.long	0
	.long	1071938560
	.long	3759783936
	.long	1071418487
	.long	3685870403
	.long	3175415611
	.long	0
	.long	1071925248
	.long	2468364288
	.long	1071460286
	.long	1578908842
	.long	3175510517
	.long	0
	.long	1071911936
	.long	81903616
	.long	1071502506
	.long	770710269
	.long	1026742353
	.long	0
	.long	1071899648
	.long	2799321088
	.long	1071541858
	.long	3822266185
	.long	1028434427
	.long	0
	.long	1071886336
	.long	2142265344
	.long	1071584911
	.long	175901806
	.long	3173871540
	.long	0
	.long	1071874048
	.long	2944024576
	.long	1071625048
	.long	2747360403
	.long	1027672159
	.long	0
	.long	1071862784
	.long	3434301440
	.long	1071653426
	.long	4194662196
	.long	3173893003
	.long	0
	.long	1071850496
	.long	1547755520
	.long	1071673870
	.long	4248764681
	.long	3172759087
	.long	0
	.long	1071839232
	.long	4246986752
	.long	1071692786
	.long	2840205638
	.long	3174430911
	.long	0
	.long	1071826944
	.long	3418390528
	.long	1071713619
	.long	3041880823
	.long	1025440860
	.long	0
	.long	1071816704
	.long	4143093760
	.long	1071731139
	.long	2727587401
	.long	3173965207
	.long	0
	.long	1071805440
	.long	3121326080
	.long	1071750582
	.long	3173887692
	.long	3174190163
	.long	0
	.long	1071794176
	.long	1852893184
	.long	1071770207
	.long	3951060252
	.long	1027348295
	.long	0
	.long	1071783936
	.long	3636379648
	.long	1071788208
	.long	1684924001
	.long	3174777086
	.long	0
	.long	1071773696
	.long	516505600
	.long	1071806366
	.long	429181199
	.long	3173211033
	.long	0
	.long	1071763456
	.long	4186185728
	.long	1071824681
	.long	2044904577
	.long	3174967132
	.long	0
	.long	1071753216
	.long	877596672
	.long	1071843159
	.long	1396318105
	.long	3173959727
	.long	0
	.long	1071742976
	.long	2912784384
	.long	1071861800
	.long	448136789
	.long	3174814192
	.long	0
	.long	1071733760
	.long	3722825728
	.long	1071878720
	.long	714165913
	.long	3173439560
	.long	0
	.long	1071723520
	.long	2522374144
	.long	1071897682
	.long	3227240353
	.long	3173394323
	.long	0
	.long	1071714304
	.long	4165410816
	.long	1071914895
	.long	1365684961
	.long	3174365060
	.long	0
	.long	1071705088
	.long	3477135360
	.long	1071932251
	.long	368482985
	.long	3174140821
	.long	0
	.long	1071695872
	.long	2079455232
	.long	1071949752
	.long	1320576317
	.long	1026822714
	.long	0
	.long	1071687680
	.long	851795968
	.long	1071965432
	.long	3702467026
	.long	1025224125
	.long	0
	.long	1071678464
	.long	647743488
	.long	1071983213
	.long	772992109
	.long	3174038459
	.long	0
	.long	1071670272
	.long	26537984
	.long	1071999146
	.long	2360214276
	.long	3174861275
	.long	0
	.long	1071661056
	.long	1547061248
	.long	1072017216
	.long	2886781435
	.long	1026423395
	.long	0
	.long	1071652864
	.long	2854492160
	.long	1072033410
	.long	215631550
	.long	1025638968
	.long	0
	.long	1071644672
	.long	4277811200
	.long	1072049730
	.long	2479318832
	.long	1026487127
	.long	4277811200
	.long	1072049730
	.long	2479318832
	.long	1026487127
	.long	64
	.long	1120927744
	.long	0
	.long	1094713344
	.long	0
	.long	1065615360
	.long	0
	.long	1135607808
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	3219128320
	.long	1431655955
	.long	1070945621
	.long	610
	.long	3218079744
	.long	2545118337
	.long	1070176665
	.long	1378399119
	.long	3217380693
	.long	612435357
	.long	1069697472
	.long	94536557
	.long	3217031348
	.type	_imldLnHATab,@object
	.size	_imldLnHATab,1680
	.align 16
.L_2il0floatpacket.31:
	.long	0x00000000,0x43380000,0x00000000,0x43380000
	.type	.L_2il0floatpacket.31,@object
	.size	.L_2il0floatpacket.31,16
	.align 16
.L_2il0floatpacket.96:
	.long	0x00000000,0x80000000,0x00000000,0x00000000
	.type	.L_2il0floatpacket.96,@object
	.size	.L_2il0floatpacket.96,16
	.align 8
.L_2il0floatpacket.97:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.97,@object
	.size	.L_2il0floatpacket.97,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
