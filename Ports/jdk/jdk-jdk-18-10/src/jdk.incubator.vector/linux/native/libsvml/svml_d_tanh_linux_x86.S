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
	.file "svml_d_tanh.c"
	.text
..TXTST0:
.L_2__routine_start___svml_tanh4_ha_e9_0:
# -- Begin  __svml_tanh4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh4_ha_e9
# --- __svml_tanh4_ha_e9(__m256d)
__svml_tanh4_ha_e9:
# parameter 1: %ymm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1241.1
        .byte     15                                            #1335.552
        .byte     30                                            #1335.552
        .byte     250                                           #1335.552
	.cfi_startproc
..___tag_value___svml_tanh4_ha_e9.1:
..L2:
                                                          #1241.1
        pushq     %rbp                                          #1241.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1241.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1241.1
        subq      $192, %rsp                                    #1241.1
        lea       2720+__svml_dtanh_ha_data_internal(%rip), %r8 #1295.45
        vmovupd   %ymm0, (%rsp)                                 #1241.1[spill]
        vpxor     %xmm3, %xmm3, %xmm3                           #1290.23
        vmovups   10688+__svml_dtanh_ha_data_internal(%rip), %xmm13 #1277.28
        vmovups   10752+__svml_dtanh_ha_data_internal(%rip), %xmm8 #1285.28
        vandpd    10432+__svml_dtanh_ha_data_internal(%rip), %ymm0, %ymm1 #1279.12
        vandpd    10368+__svml_dtanh_ha_data_internal(%rip), %ymm0, %ymm12 #1280.13
        vmovupd   %ymm12, 32(%rsp)                              #1280.13[spill]
        vextractf128 $1, %ymm0, %xmm10                          #1281.101
        vshufps   $221, %xmm10, %xmm0, %xmm9                    #1282.33
        vpand     10496+__svml_dtanh_ha_data_internal(%rip), %xmm9, %xmm4 #1283.22
        vpsubd    10624+__svml_dtanh_ha_data_internal(%rip), %xmm4, %xmm11 #1289.19
        vpcmpgtd  %xmm4, %xmm8, %xmm6                           #1286.31
        vpcmpgtd  %xmm3, %xmm11, %xmm2                          #1290.23
        vpcmpgtd  %xmm13, %xmm11, %xmm0                         #1291.23
        vpand     %xmm2, %xmm11, %xmm14                         #1292.23
        blendvps  %xmm0, %xmm13, %xmm14                         #1293.41
        vpsrld    $10, %xmm14, %xmm13                           #1294.23
        vmovd     %xmm13, %edx                                  #1295.113
        vpcmpgtd  10560+__svml_dtanh_ha_data_internal(%rip), %xmm4, %xmm7 #1284.23
        vpor      %xmm6, %xmm7, %xmm5                           #1287.27
        vpextrd   $1, %xmm13, %ecx                              #1295.254
        movslq    %edx, %rdx                                    #1295.113
        vpextrd   $2, %xmm13, %esi                              #1295.398
        movslq    %ecx, %rcx                                    #1295.186
        vpextrd   $3, %xmm13, %edi                              #1295.542
        movslq    %esi, %rsi                                    #1295.330
        movslq    %edi, %rdi                                    #1295.474
        vmovupd   -96(%rdx,%r8), %xmm15                         #1295.45
        vmovupd   -80(%rdx,%r8), %xmm7                          #1296.45
        vmovupd   -64(%rdx,%r8), %xmm3                          #1297.45
        vmovupd   -96(%rcx,%r8), %xmm12                         #1295.186
        vmovupd   -80(%rcx,%r8), %xmm6                          #1296.186
        vmovupd   -64(%rcx,%r8), %xmm2                          #1297.186
        vmovmskps %xmm5, %eax                                   #1288.40
        vinsertf128 $1, -96(%rsi,%r8), %ymm15, %ymm9            #1295.596
        vmovupd   -48(%rdx,%r8), %xmm15                         #1298.45
        vinsertf128 $1, -96(%rdi,%r8), %ymm12, %ymm8            #1295.677
        vinsertf128 $1, -80(%rdi,%r8), %ymm6, %ymm4             #1296.677
        vinsertf128 $1, -64(%rdi,%r8), %ymm2, %ymm14            #1297.677
        vinsertf128 $1, -80(%rsi,%r8), %ymm7, %ymm5             #1296.596
        vinsertf128 $1, -64(%rsi,%r8), %ymm3, %ymm11            #1297.596
        vmovupd   -48(%rcx,%r8), %xmm6                          #1298.186
        vmovupd   -32(%rcx,%r8), %xmm2                          #1299.186
        vmovupd   -32(%rdx,%r8), %xmm3                          #1299.45
        vunpcklpd %ymm8, %ymm9, %ymm10                          #1295.759
        vunpckhpd %ymm8, %ymm9, %ymm13                          #1295.810
        vunpcklpd %ymm4, %ymm5, %ymm12                          #1296.759
        vunpckhpd %ymm4, %ymm5, %ymm9                           #1296.810
        vunpcklpd %ymm14, %ymm11, %ymm8                         #1297.759
        vunpckhpd %ymm14, %ymm11, %ymm7                         #1297.810
        vmovupd   -16(%rdx,%r8), %xmm14                         #1300.45
        vinsertf128 $1, -48(%rsi,%r8), %ymm15, %ymm5            #1298.596
        vmovupd   -16(%rcx,%r8), %xmm15                         #1300.187
        vinsertf128 $1, -48(%rdi,%r8), %ymm6, %ymm4             #1298.677
        vinsertf128 $1, -32(%rdi,%r8), %ymm2, %ymm0             #1299.677
        vinsertf128 $1, -32(%rsi,%r8), %ymm3, %ymm11            #1299.596
        vunpcklpd %ymm4, %ymm5, %ymm6                           #1298.759
        vunpckhpd %ymm4, %ymm5, %ymm5                           #1298.810
        vunpcklpd %ymm0, %ymm11, %ymm4                          #1299.759
        vunpckhpd %ymm0, %ymm11, %ymm3                          #1299.810
        vinsertf128 $1, -16(%rsi,%r8), %ymm14, %ymm11           #1300.600
        vinsertf128 $1, -16(%rdi,%r8), %ymm15, %ymm0            #1300.681
        vunpcklpd %ymm0, %ymm11, %ymm2                          #1300.763
        vunpckhpd %ymm0, %ymm11, %ymm11                         #1300.814
        vmovupd   (%rdx,%r8), %xmm14                            #1301.45
        vmovupd   (%rcx,%r8), %xmm0                             #1301.187
        vinsertf128 $1, (%rsi,%r8), %ymm14, %ymm15              #1301.600
        vinsertf128 $1, (%rdi,%r8), %ymm0, %ymm0                #1301.681
        vunpcklpd %ymm0, %ymm15, %ymm14                         #1301.763
        vaddpd    %ymm14, %ymm1, %ymm15                         #1302.19
        vmulpd    %ymm11, %ymm15, %ymm1                         #1303.30
        vaddpd    %ymm2, %ymm1, %ymm14                          #1303.15
        vmulpd    %ymm14, %ymm15, %ymm0                         #1304.30
        vaddpd    %ymm3, %ymm0, %ymm1                           #1304.15
        vmulpd    %ymm1, %ymm15, %ymm2                          #1305.30
        vaddpd    %ymm4, %ymm2, %ymm3                           #1305.15
        vmulpd    %ymm3, %ymm15, %ymm4                          #1306.30
        vaddpd    %ymm5, %ymm4, %ymm5                           #1306.15
        vmulpd    %ymm5, %ymm15, %ymm11                         #1307.30
        vaddpd    %ymm6, %ymm11, %ymm6                          #1307.15
        vmulpd    %ymm6, %ymm15, %ymm14                         #1308.30
        vaddpd    %ymm7, %ymm14, %ymm7                          #1308.15
        vmulpd    %ymm7, %ymm15, %ymm14                         #1309.30
        vaddpd    %ymm8, %ymm14, %ymm8                          #1309.15
        vmulpd    %ymm8, %ymm15, %ymm14                         #1310.30
        vaddpd    %ymm9, %ymm14, %ymm9                          #1310.15
        vmulpd    %ymm9, %ymm15, %ymm14                         #1317.19
        vmulpd    %ymm14, %ymm15, %ymm0                         #1318.34
        vaddpd    %ymm10, %ymm0, %ymm2                          #1318.19
        vmovupd   10304+__svml_dtanh_ha_data_internal(%rip), %ymm10 #1319.63
        vandpd    %ymm10, %ymm12, %ymm0                         #1320.22
        vandpd    %ymm10, %ymm15, %ymm10                        #1322.22
        vmulpd    %ymm10, %ymm0, %ymm14                         #1324.21
        vsubpd    %ymm0, %ymm12, %ymm1                          #1321.22
        vsubpd    %ymm10, %ymm15, %ymm15                        #1323.22
        vaddpd    %ymm13, %ymm14, %ymm7                         #1329.188
        vmulpd    %ymm15, %ymm1, %ymm12                         #1325.21
        vmulpd    %ymm15, %ymm0, %ymm0                          #1326.36
        vmulpd    %ymm1, %ymm10, %ymm15                         #1327.36
        vsubpd    %ymm7, %ymm14, %ymm4                          #1329.238
        vaddpd    %ymm0, %ymm12, %ymm12                         #1326.21
        vaddpd    %ymm4, %ymm7, %ymm5                           #1329.294
        vaddpd    %ymm15, %ymm12, %ymm3                         #1327.21
        vaddpd    %ymm13, %ymm4, %ymm13                         #1329.358
        vsubpd    %ymm5, %ymm14, %ymm4                          #1329.416
        vaddpd    %ymm3, %ymm2, %ymm6                           #1328.17
        vaddpd    %ymm4, %ymm13, %ymm5                          #1329.472
        vaddpd    %ymm5, %ymm6, %ymm0                           #1330.17
        vaddpd    %ymm0, %ymm7, %ymm1                           #1331.19
        vorpd     32(%rsp), %ymm1, %ymm0                        #1333.10[spill]
        testl     %eax, %eax                                    #1335.52
        jne       ..B1.3        # Prob 5%                       #1335.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1338.12
        popq      %rbp                                          #1338.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1338.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm1                                 #1335.200[spill]
        vmovupd   %ymm0, 128(%rsp)                              #1335.276
        vmovupd   %ymm1, 64(%rsp)                               #1335.200
        je        ..B1.2        # Prob 95%                      #1335.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1335.460
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
        btl       %r12d, %r13d                                  #1335.523
        jc        ..B1.10       # Prob 5%                       #1335.523
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1335.476
        cmpl      $4, %r12d                                     #1335.471
        jl        ..B1.7        # Prob 82%                      #1335.471
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1335.683
        jmp       ..B1.2        # Prob 100%                     #1335.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1335.552
        lea       128(%rsp,%r12,8), %rsi                        #1335.552
..___tag_value___svml_tanh4_ha_e9.23:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1335.552
..___tag_value___svml_tanh4_ha_e9.24:
        jmp       ..B1.8        # Prob 100%                     #1335.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh4_ha_e9,@function
	.size	__svml_tanh4_ha_e9,.-__svml_tanh4_ha_e9
..LN__svml_tanh4_ha_e9.0:
	.data
# -- End  __svml_tanh4_ha_e9
	.text
.L_2__routine_start___svml_tanh4_ha_l9_1:
# -- Begin  __svml_tanh4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh4_ha_l9
# --- __svml_tanh4_ha_l9(__m256d)
__svml_tanh4_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1343.1
        .byte     15                                            #1427.552
        .byte     30                                            #1427.552
        .byte     250                                           #1427.552
	.cfi_startproc
..___tag_value___svml_tanh4_ha_l9.26:
..L27:
                                                         #1343.1
        pushq     %rbp                                          #1343.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1343.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1343.1
        subq      $192, %rsp                                    #1343.1
        lea       2720+__svml_dtanh_ha_data_internal(%rip), %r8 #1394.45
        vmovupd   %ymm0, (%rsp)                                 #1343.1[spill]
        vpxor     %xmm10, %xmm10, %xmm10                        #1389.19
        vmovups   10688+__svml_dtanh_ha_data_internal(%rip), %xmm7 #1379.24
        vandpd    10432+__svml_dtanh_ha_data_internal(%rip), %ymm0, %ymm1 #1381.12
        vandpd    10368+__svml_dtanh_ha_data_internal(%rip), %ymm0, %ymm15 #1382.13
        vmovupd   %ymm15, 32(%rsp)                              #1382.13[spill]
        vextractf128 $1, %ymm0, %xmm14                          #1384.118
        vshufps   $221, %xmm14, %xmm0, %xmm13                   #1384.29
        vpand     10496+__svml_dtanh_ha_data_internal(%rip), %xmm13, %xmm11 #1385.18
        vpsubd    10624+__svml_dtanh_ha_data_internal(%rip), %xmm11, %xmm8 #1388.15
        vpcmpgtd  %xmm10, %xmm8, %xmm9                          #1389.19
        vpcmpgtd  %xmm7, %xmm8, %xmm0                           #1390.19
        vpand     %xmm9, %xmm8, %xmm6                           #1391.19
        blendvps  %xmm0, %xmm7, %xmm6                           #1392.37
        vpsrld    $10, %xmm6, %xmm5                             #1393.19
        vmovd     %xmm5, %edx                                   #1394.113
        vpcmpgtd  10560+__svml_dtanh_ha_data_internal(%rip), %xmm11, %xmm12 #1386.19
        vmovmskps %xmm12, %eax                                  #1387.40
        vpextrd   $1, %xmm5, %ecx                               #1394.254
        movslq    %edx, %rdx                                    #1394.113
        movslq    %ecx, %rcx                                    #1394.186
        vpextrd   $2, %xmm5, %esi                               #1394.398
        vpextrd   $3, %xmm5, %edi                               #1394.542
        movslq    %esi, %rsi                                    #1394.330
        movslq    %edi, %rdi                                    #1394.474
        vmovupd   -96(%rdx,%r8), %xmm2                          #1394.45
        vmovupd   -96(%rcx,%r8), %xmm3                          #1394.186
        vmovupd   -48(%rcx,%r8), %xmm5                          #1397.186
        vmovupd   -48(%rdx,%r8), %xmm6                          #1397.45
        vmovupd   -80(%rdx,%r8), %xmm14                         #1395.45
        vmovupd   -64(%rdx,%r8), %xmm10                         #1396.45
        vmovupd   -80(%rcx,%r8), %xmm13                         #1395.186
        vmovupd   -64(%rcx,%r8), %xmm9                          #1396.186
        vinsertf128 $1, -96(%rsi,%r8), %ymm2, %ymm4             #1394.596
        vinsertf128 $1, -96(%rdi,%r8), %ymm3, %ymm15            #1394.677
        vunpcklpd %ymm15, %ymm4, %ymm0                          #1394.759
        vunpckhpd %ymm15, %ymm4, %ymm15                         #1394.810
        vmovupd   -32(%rdx,%r8), %xmm4                          #1398.45
        vinsertf128 $1, -48(%rsi,%r8), %ymm6, %ymm2             #1397.596
        vinsertf128 $1, -48(%rdi,%r8), %ymm5, %ymm3             #1397.677
        vmovupd   -16(%rdx,%r8), %xmm5                          #1399.45
        vinsertf128 $1, -80(%rsi,%r8), %ymm14, %ymm12           #1395.596
        vinsertf128 $1, -64(%rsi,%r8), %ymm10, %ymm8            #1396.596
        vinsertf128 $1, -80(%rdi,%r8), %ymm13, %ymm11           #1395.677
        vinsertf128 $1, -64(%rdi,%r8), %ymm9, %ymm7             #1396.677
        vunpcklpd %ymm3, %ymm2, %ymm10                          #1397.759
        vunpckhpd %ymm3, %ymm2, %ymm9                           #1397.810
        vmovupd   -16(%rcx,%r8), %xmm2                          #1399.187
        vunpcklpd %ymm11, %ymm12, %ymm14                        #1395.759
        vunpckhpd %ymm11, %ymm12, %ymm13                        #1395.810
        vunpcklpd %ymm7, %ymm8, %ymm12                          #1396.759
        vunpckhpd %ymm7, %ymm8, %ymm11                          #1396.810
        vmovupd   -32(%rcx,%r8), %xmm8                          #1398.186
        vinsertf128 $1, -16(%rsi,%r8), %ymm5, %ymm3             #1399.600
        vinsertf128 $1, -32(%rsi,%r8), %ymm4, %ymm7             #1398.596
        vmovupd   (%rcx,%r8), %xmm4                             #1400.187
        vinsertf128 $1, -16(%rdi,%r8), %ymm2, %ymm5             #1399.681
        vmovupd   (%rdx,%r8), %xmm2                             #1400.45
        vinsertf128 $1, -32(%rdi,%r8), %ymm8, %ymm6             #1398.677
        vunpcklpd %ymm6, %ymm7, %ymm8                           #1398.759
        vunpckhpd %ymm6, %ymm7, %ymm7                           #1398.810
        vunpcklpd %ymm5, %ymm3, %ymm6                           #1399.763
        vunpckhpd %ymm5, %ymm3, %ymm5                           #1399.814
        vinsertf128 $1, (%rsi,%r8), %ymm2, %ymm3                #1400.600
        vinsertf128 $1, (%rdi,%r8), %ymm4, %ymm4                #1400.681
        vunpcklpd %ymm4, %ymm3, %ymm2                           #1400.763
        vaddpd    %ymm2, %ymm1, %ymm1                           #1401.19
        vfmadd213pd %ymm6, %ymm1, %ymm5                         #1402.15
        vfmadd213pd %ymm7, %ymm1, %ymm5                         #1403.15
        vfmadd213pd %ymm8, %ymm1, %ymm5                         #1404.15
        vfmadd213pd %ymm9, %ymm1, %ymm5                         #1405.15
        vfmadd213pd %ymm10, %ymm1, %ymm5                        #1406.15
        vfmadd213pd %ymm11, %ymm1, %ymm5                        #1407.15
        vfmadd213pd %ymm12, %ymm1, %ymm5                        #1408.15
        vfmadd213pd %ymm13, %ymm1, %ymm5                        #1409.15
        vmulpd    %ymm5, %ymm1, %ymm2                           #1416.19
        vfmadd213pd %ymm0, %ymm1, %ymm2                         #1417.19
        vmulpd    %ymm14, %ymm1, %ymm0                          #1418.21
        vaddpd    %ymm15, %ymm0, %ymm7                          #1421.188
        vfmsub213pd %ymm0, %ymm1, %ymm14                        #1419.21
        vsubpd    %ymm7, %ymm0, %ymm1                           #1421.238
        vaddpd    %ymm14, %ymm2, %ymm5                          #1420.17
        vaddpd    %ymm15, %ymm1, %ymm15                         #1421.358
        vaddpd    %ymm1, %ymm7, %ymm3                           #1421.294
        vsubpd    %ymm3, %ymm0, %ymm4                           #1421.416
        vaddpd    %ymm4, %ymm15, %ymm6                          #1421.472
        vaddpd    %ymm6, %ymm5, %ymm8                           #1422.17
        vaddpd    %ymm8, %ymm7, %ymm9                           #1423.19
        vorpd     32(%rsp), %ymm9, %ymm0                        #1425.10[spill]
        testl     %eax, %eax                                    #1427.52
        jne       ..B2.3        # Prob 5%                       #1427.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1430.12
        popq      %rbp                                          #1430.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1430.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %ymm1                                 #1427.200[spill]
        vmovupd   %ymm0, 128(%rsp)                              #1427.276
        vmovupd   %ymm1, 64(%rsp)                               #1427.200
        je        ..B2.2        # Prob 95%                      #1427.380
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1427.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.13:                        # Preds ..B2.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1427.523
        jc        ..B2.10       # Prob 5%                       #1427.523
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1427.476
        cmpl      $4, %r12d                                     #1427.471
        jl        ..B2.7        # Prob 82%                      #1427.471
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1427.683
        jmp       ..B2.2        # Prob 100%                     #1427.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1427.552
        lea       128(%rsp,%r12,8), %rsi                        #1427.552
..___tag_value___svml_tanh4_ha_l9.48:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1427.552
..___tag_value___svml_tanh4_ha_l9.49:
        jmp       ..B2.8        # Prob 100%                     #1427.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh4_ha_l9,@function
	.size	__svml_tanh4_ha_l9,.-__svml_tanh4_ha_l9
..LN__svml_tanh4_ha_l9.1:
	.data
# -- End  __svml_tanh4_ha_l9
	.text
.L_2__routine_start___svml_tanh1_ha_ex_2:
# -- Begin  __svml_tanh1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh1_ha_ex
# --- __svml_tanh1_ha_ex(__m128d)
__svml_tanh1_ha_ex:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1435.1
        .byte     15                                            #1529.546
        .byte     30                                            #1529.546
        .byte     250                                           #1529.546
	.cfi_startproc
..___tag_value___svml_tanh1_ha_ex.51:
..L52:
                                                         #1435.1
        pushq     %rbp                                          #1435.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1435.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1435.1
        subq      $128, %rsp                                    #1435.1
        movl      $2147352576, %edx                             #1477.16
        movl      $535822336, %esi                              #1480.25
        movl      $1069416448, %edi                             #1483.13
        movl      $7733248, %eax                                #1471.22
        movl      $2145386496, %ecx                             #1478.17
        pshufd    $85, %xmm0, %xmm12                            #1476.9
        pxor      %xmm10, %xmm10                                #1484.17
        movd      %edx, %xmm5                                   #1477.16
        lea       2728+__svml_dtanh_ha_data_internal(%rip), %r9 #1489.28
        pand      %xmm5, %xmm12                                 #1477.16
        movd      %esi, %xmm7                                   #1480.25
        movd      %edi, %xmm9                                   #1483.13
        movdqa    %xmm12, %xmm8                                 #1478.17
        pcmpgtd   %xmm12, %xmm7                                 #1480.25
        psubd     %xmm9, %xmm12                                 #1483.13
        movd      %eax, %xmm14                                  #1471.22
        movdqa    %xmm12, %xmm13                                #1485.17
        movdqa    %xmm12, %xmm11                                #1484.17
        pcmpgtd   %xmm14, %xmm13                                #1485.17
        pcmpgtd   %xmm10, %xmm11                                #1484.17
        movdqa    %xmm13, %xmm15                                #1487.46
        pand      %xmm11, %xmm12                                #1486.17
        andps     %xmm13, %xmm14                                #1487.123
        andnps    %xmm12, %xmm15                                #1487.46
        movd      %ecx, %xmm6                                   #1478.17
        orps      %xmm14, %xmm15                                #1487.35
        pcmpgtd   %xmm6, %xmm8                                  #1478.17
        psrld     $10, %xmm15                                   #1488.17
        por       %xmm7, %xmm8                                  #1481.21
        movd      %xmm15, %r8d                                  #1489.119
        movsd     10432+__svml_dtanh_ha_data_internal(%rip), %xmm3 #1473.12
        andps     %xmm0, %xmm3                                  #1473.12
        movslq    %r8d, %r8                                     #1489.119
        movsd     10368+__svml_dtanh_ha_data_internal(%rip), %xmm4 #1474.13
        movups    %xmm0, 16(%rsp)                               #1435.1[spill]
        andps     %xmm0, %xmm4                                  #1474.13
        movmskps  %xmm8, %eax                                   #1482.40
        movups    -8(%r9,%r8), %xmm6                            #1495.72
        movsd     -16(%r9,%r8), %xmm0                           #1494.72
        addsd     %xmm6, %xmm3                                  #1496.19
        unpcklpd  %xmm0, %xmm0                                  #1494.175
        mulsd     %xmm3, %xmm0                                  #1497.27
        movups    -24(%r9,%r8), %xmm7                           #1494.72
        movsd     -32(%r9,%r8), %xmm8                           #1493.72
        addsd     %xmm7, %xmm0                                  #1497.15
        mulsd     %xmm3, %xmm0                                  #1498.27
        unpcklpd  %xmm8, %xmm8                                  #1493.174
        movups    -40(%r9,%r8), %xmm9                           #1493.72
        addsd     %xmm8, %xmm0                                  #1498.15
        mulsd     %xmm3, %xmm0                                  #1499.27
        movsd     -48(%r9,%r8), %xmm10                          #1492.72
        addsd     %xmm9, %xmm0                                  #1499.15
        mulsd     %xmm3, %xmm0                                  #1500.27
        unpcklpd  %xmm10, %xmm10                                #1492.174
        movups    -56(%r9,%r8), %xmm11                          #1492.72
        addsd     %xmm10, %xmm0                                 #1500.15
        mulsd     %xmm3, %xmm0                                  #1501.27
        movsd     -64(%r9,%r8), %xmm12                          #1491.72
        addsd     %xmm11, %xmm0                                 #1501.15
        mulsd     %xmm3, %xmm0                                  #1502.27
        unpcklpd  %xmm12, %xmm12                                #1491.174
        movups    -72(%r9,%r8), %xmm13                          #1491.72
        addsd     %xmm12, %xmm0                                 #1502.15
        mulsd     %xmm3, %xmm0                                  #1503.27
        movsd     -80(%r9,%r8), %xmm14                          #1490.72
        addsd     %xmm13, %xmm0                                 #1503.15
        mulsd     %xmm3, %xmm0                                  #1504.27
        unpcklpd  %xmm14, %xmm14                                #1490.174
        addsd     %xmm14, %xmm0                                 #1504.15
        mulsd     %xmm3, %xmm0                                  #1511.19
        movsd     10304+__svml_dtanh_ha_data_internal(%rip), %xmm5 #1513.29
        movups    -88(%r9,%r8), %xmm2                           #1490.72
        movaps    %xmm5, %xmm7                                  #1514.22
        andps     %xmm2, %xmm7                                  #1514.22
        andps     %xmm3, %xmm5                                  #1516.22
        subsd     %xmm7, %xmm2                                  #1515.22
        mulsd     %xmm3, %xmm0                                  #1512.31
        subsd     %xmm5, %xmm3                                  #1517.22
        movaps    %xmm2, %xmm6                                  #1519.21
        movaps    %xmm7, %xmm8                                  #1518.21
        mulsd     %xmm3, %xmm6                                  #1519.21
        mulsd     %xmm3, %xmm7                                  #1520.33
        mulsd     %xmm5, %xmm2                                  #1521.33
        mulsd     %xmm5, %xmm8                                  #1518.21
        addsd     %xmm6, %xmm7                                  #1520.21
        movups    -104(%r8,%r9), %xmm15                         #1489.72
        movaps    %xmm8, %xmm3                                  #1523.235
        movsd     -96(%r8,%r9), %xmm1                           #1489.72
        addsd     %xmm15, %xmm0                                 #1512.19
        addsd     %xmm7, %xmm2                                  #1521.21
        unpcklpd  %xmm1, %xmm1                                  #1489.174
        addsd     %xmm0, %xmm2                                  #1522.17
        movaps    %xmm8, %xmm0                                  #1523.188
        addsd     %xmm1, %xmm0                                  #1523.188
        movaps    %xmm0, %xmm5                                  #1523.288
        subsd     %xmm0, %xmm3                                  #1523.235
        addsd     %xmm3, %xmm5                                  #1523.288
        addsd     %xmm3, %xmm1                                  #1523.349
        subsd     %xmm5, %xmm8                                  #1523.404
        addsd     %xmm8, %xmm1                                  #1523.457
        addsd     %xmm1, %xmm2                                  #1524.17
        addsd     %xmm2, %xmm0                                  #1525.19
        orps      %xmm4, %xmm0                                  #1527.10
        andl      $1, %eax                                      #1482.94
        jne       ..B3.3        # Prob 5%                       #1529.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B3.2:                         # Preds ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1532.12
        popq      %rbp                                          #1532.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1532.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm1                               #1529.160[spill]
        movsd     %xmm1, (%rsp)                                 #1529.160
        movsd     %xmm0, 64(%rsp)                               #1529.233
        jne       ..B3.6        # Prob 5%                       #1529.374
                                # LOE rbx r12 r13 r14 r15
..B3.4:                         # Preds ..B3.6 ..B3.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1529.626
        movq      %rbp, %rsp                                    #1529.626
        popq      %rbp                                          #1529.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1529.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1529.546
        lea       64(%rsp), %rsi                                #1529.546
..___tag_value___svml_tanh1_ha_ex.66:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1529.546
..___tag_value___svml_tanh1_ha_ex.67:
        jmp       ..B3.4        # Prob 100%                     #1529.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_tanh1_ha_ex,@function
	.size	__svml_tanh1_ha_ex,.-__svml_tanh1_ha_ex
..LN__svml_tanh1_ha_ex.2:
	.data
# -- End  __svml_tanh1_ha_ex
	.text
.L_2__routine_start___svml_tanh2_ha_l9_3:
# -- Begin  __svml_tanh2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh2_ha_l9
# --- __svml_tanh2_ha_l9(__m128d)
__svml_tanh2_ha_l9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1537.1
        .byte     15                                            #1621.546
        .byte     30                                            #1621.546
        .byte     250                                           #1621.546
	.cfi_startproc
..___tag_value___svml_tanh2_ha_l9.69:
..L70:
                                                         #1537.1
        pushq     %rbp                                          #1537.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1537.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1537.1
        subq      $192, %rsp                                    #1537.1
        lea       2720+__svml_dtanh_ha_data_internal(%rip), %rsi #1588.67
        vmovq     10496+__svml_dtanh_ha_data_internal(%rip), %xmm11 #1571.25
        vpxor     %xmm8, %xmm8, %xmm8                           #1583.19
        vpshufd   $221, %xmm0, %xmm12                           #1578.11
        vmovq     10624+__svml_dtanh_ha_data_internal(%rip), %xmm7 #1572.27
        vpand     %xmm11, %xmm12, %xmm10                        #1579.18
        vpsubd    %xmm7, %xmm10, %xmm6                          #1582.15
        vmovq     10688+__svml_dtanh_ha_data_internal(%rip), %xmm3 #1573.24
        vpcmpgtd  %xmm8, %xmm6, %xmm5                           #1583.19
        vmovupd   %xmm0, (%rsp)                                 #1537.1[spill]
        vpand     %xmm5, %xmm6, %xmm4                           #1585.19
        vandpd    10432+__svml_dtanh_ha_data_internal(%rip), %xmm0, %xmm14 #1575.12
        vandpd    10368+__svml_dtanh_ha_data_internal(%rip), %xmm0, %xmm15 #1576.13
        vpcmpgtd  %xmm3, %xmm6, %xmm0                           #1584.19
        blendvps  %xmm0, %xmm3, %xmm4                           #1586.37
        vpsrld    $10, %xmm4, %xmm2                             #1587.19
        vmovd     %xmm2, %eax                                   #1588.135
        vmovq     10560+__svml_dtanh_ha_data_internal(%rip), %xmm13 #1570.17
        vmovupd   %xmm15, 16(%rsp)                              #1576.13[spill]
        vpcmpgtd  %xmm13, %xmm10, %xmm9                         #1580.19
        vpextrd   $1, %xmm2, %ecx                               #1588.298
        movslq    %eax, %rax                                    #1588.135
        movslq    %ecx, %rcx                                    #1588.230
        vmovmskps %xmm9, %edx                                   #1581.40
        vmovupd   -96(%rax,%rsi), %xmm1                         #1588.67
        vmovupd   -96(%rcx,%rsi), %xmm15                        #1588.230
        vmovupd   -80(%rcx,%rsi), %xmm10                        #1589.230
        vmovupd   -64(%rcx,%rsi), %xmm8                         #1590.230
        vmovupd   -48(%rcx,%rsi), %xmm6                         #1591.230
        vmovupd   -32(%rcx,%rsi), %xmm4                         #1592.230
        vunpcklpd %xmm15, %xmm1, %xmm13                         #1588.353
        vunpckhpd %xmm15, %xmm1, %xmm12                         #1588.399
        vmovupd   -16(%rcx,%rsi), %xmm1                         #1593.231
        vmovupd   (%rcx,%rsi), %xmm15                           #1594.231
        vmovupd   -80(%rax,%rsi), %xmm9                         #1589.67
        vmovupd   -64(%rax,%rsi), %xmm7                         #1590.67
        vmovupd   -48(%rax,%rsi), %xmm5                         #1591.67
        vmovupd   -32(%rax,%rsi), %xmm3                         #1592.67
        vmovupd   -16(%rax,%rsi), %xmm2                         #1593.67
        vmovupd   (%rax,%rsi), %xmm0                            #1594.67
        vunpcklpd %xmm10, %xmm9, %xmm11                         #1589.353
        vunpckhpd %xmm10, %xmm9, %xmm10                         #1589.399
        vunpcklpd %xmm8, %xmm7, %xmm9                           #1590.353
        vunpckhpd %xmm8, %xmm7, %xmm8                           #1590.399
        vunpcklpd %xmm6, %xmm5, %xmm7                           #1591.353
        vunpckhpd %xmm6, %xmm5, %xmm6                           #1591.399
        vunpcklpd %xmm4, %xmm3, %xmm5                           #1592.353
        vunpckhpd %xmm4, %xmm3, %xmm4                           #1592.399
        vunpcklpd %xmm1, %xmm2, %xmm3                           #1593.355
        vunpckhpd %xmm1, %xmm2, %xmm2                           #1593.401
        vunpcklpd %xmm15, %xmm0, %xmm1                          #1594.355
        vaddpd    %xmm1, %xmm14, %xmm14                         #1595.19
        vfmadd213pd %xmm3, %xmm14, %xmm2                        #1596.15
        vfmadd213pd %xmm4, %xmm14, %xmm2                        #1597.15
        vfmadd213pd %xmm5, %xmm14, %xmm2                        #1598.15
        vfmadd213pd %xmm6, %xmm14, %xmm2                        #1599.15
        vmulpd    %xmm11, %xmm14, %xmm1                         #1612.21
        vfmadd213pd %xmm7, %xmm14, %xmm2                        #1600.15
        vfmsub213pd %xmm1, %xmm14, %xmm11                       #1613.21
        vaddpd    %xmm12, %xmm1, %xmm6                          #1615.188
        vfmadd213pd %xmm8, %xmm14, %xmm2                        #1601.15
        vfmadd213pd %xmm9, %xmm14, %xmm2                        #1602.15
        vfmadd213pd %xmm10, %xmm14, %xmm2                       #1603.15
        vmulpd    %xmm2, %xmm14, %xmm0                          #1610.19
        vfmadd213pd %xmm13, %xmm14, %xmm0                       #1611.19
        vaddpd    %xmm11, %xmm0, %xmm4                          #1614.17
        vsubpd    %xmm6, %xmm1, %xmm11                          #1615.235
        vaddpd    %xmm11, %xmm6, %xmm2                          #1615.288
        vaddpd    %xmm12, %xmm11, %xmm12                        #1615.349
        vsubpd    %xmm2, %xmm1, %xmm3                           #1615.404
        vaddpd    %xmm3, %xmm12, %xmm5                          #1615.457
        vaddpd    %xmm5, %xmm4, %xmm7                           #1616.17
        vaddpd    %xmm7, %xmm6, %xmm8                           #1617.19
        vorpd     16(%rsp), %xmm8, %xmm0                        #1619.10[spill]
        andl      $3, %edx                                      #1581.94
        jne       ..B4.3        # Prob 5%                       #1621.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.2:                         # Preds ..B4.3 ..B4.9 ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1624.12
        popq      %rbp                                          #1624.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1624.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   (%rsp), %xmm1                                 #1621.197[spill]
        vmovupd   %xmm1, 64(%rsp)                               #1621.197
        vmovupd   %xmm0, 128(%rsp)                              #1621.270
        je        ..B4.2        # Prob 95%                      #1621.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1621.454
        movq      %r12, 8(%rsp)                                 #1621.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1621.454
        movq      %r13, (%rsp)                                  #1621.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1621.454
                                # LOE rbx r12 r14 r15 r13d
..B4.7:                         # Preds ..B4.8 ..B4.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1621.517
        jc        ..B4.10       # Prob 5%                       #1621.517
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.10 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1621.470
        cmpl      $2, %r12d                                     #1621.465
        jl        ..B4.7        # Prob 82%                      #1621.465
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1621.674
        jmp       ..B4.2        # Prob 100%                     #1621.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.10:                        # Preds ..B4.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1621.546
        lea       128(%rsp,%r12,8), %rsi                        #1621.546
..___tag_value___svml_tanh2_ha_l9.91:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1621.546
..___tag_value___svml_tanh2_ha_l9.92:
        jmp       ..B4.8        # Prob 100%                     #1621.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh2_ha_l9,@function
	.size	__svml_tanh2_ha_l9,.-__svml_tanh2_ha_l9
..LN__svml_tanh2_ha_l9.3:
	.data
# -- End  __svml_tanh2_ha_l9
	.text
.L_2__routine_start___svml_tanh1_ha_l9_4:
# -- Begin  __svml_tanh1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh1_ha_l9
# --- __svml_tanh1_ha_l9(__m128d)
__svml_tanh1_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1629.1
        .byte     15                                            #1713.546
        .byte     30                                            #1713.546
        .byte     250                                           #1713.546
	.cfi_startproc
..___tag_value___svml_tanh1_ha_l9.94:
..L95:
                                                         #1629.1
        pushq     %rbp                                          #1629.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1629.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1629.1
        subq      $128, %rsp                                    #1629.1
        movl      $2147352576, %edx                             #1671.18
        movl      $1069416448, %esi                             #1674.15
        movl      $7733248, %eax                                #1665.24
        vpshufd   $85, %xmm0, %xmm15                            #1670.11
        lea       2728+__svml_dtanh_ha_data_internal(%rip), %r8 #1680.22
        vpxor     %xmm8, %xmm8, %xmm8                           #1675.19
        movl      $2145386496, %ecx                             #1672.19
        vmovd     %edx, %xmm11                                  #1671.18
        vpand     %xmm11, %xmm15, %xmm10                        #1671.18
        vmovd     %esi, %xmm7                                   #1674.15
        vpsubd    %xmm7, %xmm10, %xmm6                          #1674.15
        vmovd     %eax, %xmm14                                  #1665.24
        vmovsd    10368+__svml_dtanh_ha_data_internal(%rip), %xmm13 #1660.17
        vpcmpgtd  %xmm8, %xmm6, %xmm5                           #1675.19
        vmovsd    10432+__svml_dtanh_ha_data_internal(%rip), %xmm2 #1661.16
        vpand     %xmm5, %xmm6, %xmm4                           #1677.19
        vmovupd   %xmm0, 16(%rsp)                               #1629.1[spill]
        vandpd    %xmm2, %xmm0, %xmm1                           #1667.12
        vandpd    %xmm13, %xmm0, %xmm2                          #1668.13
        vpcmpgtd  %xmm14, %xmm6, %xmm0                          #1676.19
        blendvps  %xmm0, %xmm14, %xmm4                          #1678.37
        vmovd     %ecx, %xmm12                                  #1672.19
        vpsrld    $10, %xmm4, %xmm3                             #1679.19
        vpcmpgtd  %xmm12, %xmm10, %xmm9                         #1672.19
        vmovd     %xmm3, %edi                                   #1680.113
        vmovmskps %xmm9, %eax                                   #1673.40
        movslq    %edi, %rdi                                    #1680.113
        vmovupd   -8(%r8,%rdi), %xmm3                           #1686.66
        vmovsd    -32(%r8,%rdi), %xmm5                          #1684.66
        vaddsd    %xmm3, %xmm1, %xmm1                           #1687.19
        vmovsd    -16(%r8,%rdi), %xmm4                          #1685.66
        vunpcklpd %xmm5, %xmm5, %xmm6                           #1684.164
        vmovupd   -24(%r8,%rdi), %xmm5                          #1685.66
        vunpcklpd %xmm4, %xmm4, %xmm4                           #1685.165
        vfmadd213sd %xmm5, %xmm1, %xmm4                         #1688.15
        vmovsd    -48(%r8,%rdi), %xmm7                          #1683.66
        vunpcklpd %xmm7, %xmm7, %xmm8                           #1683.164
        vmovupd   -40(%r8,%rdi), %xmm7                          #1684.66
        vmovsd    -64(%r8,%rdi), %xmm9                          #1682.66
        vfmadd213sd %xmm6, %xmm1, %xmm4                         #1689.15
        vunpcklpd %xmm9, %xmm9, %xmm10                          #1682.164
        vmovupd   -56(%r8,%rdi), %xmm9                          #1683.66
        vmovsd    -80(%r8,%rdi), %xmm11                         #1681.66
        vunpcklpd %xmm11, %xmm11, %xmm12                        #1681.164
        vfmadd213sd %xmm7, %xmm1, %xmm4                         #1690.15
        vmovupd   -72(%r8,%rdi), %xmm11                         #1682.66
        vmovupd   -88(%r8,%rdi), %xmm13                         #1681.66
        vmovsd    -96(%rdi,%r8), %xmm14                         #1680.66
        vfmadd213sd %xmm8, %xmm1, %xmm4                         #1691.15
        vmulsd    %xmm1, %xmm13, %xmm3                          #1704.21
        vfmadd213sd %xmm9, %xmm1, %xmm4                         #1692.15
        vunpcklpd %xmm14, %xmm14, %xmm14                        #1680.164
        vaddsd    %xmm14, %xmm3, %xmm8                          #1707.188
        vfmadd213sd %xmm10, %xmm1, %xmm4                        #1693.15
        vfmadd213sd %xmm11, %xmm1, %xmm4                        #1694.15
        vmovupd   -104(%rdi,%r8), %xmm15                        #1680.66
        vfmadd213sd %xmm12, %xmm1, %xmm4                        #1695.15
        vfmsub213sd %xmm3, %xmm1, %xmm13                        #1705.21
        vmulsd    %xmm1, %xmm4, %xmm0                           #1702.19
        vfmadd213sd %xmm15, %xmm1, %xmm0                        #1703.19
        vsubsd    %xmm8, %xmm3, %xmm1                           #1707.235
        vaddsd    %xmm1, %xmm8, %xmm4                           #1707.288
        vaddsd    %xmm1, %xmm14, %xmm14                         #1707.349
        vaddsd    %xmm0, %xmm13, %xmm6                          #1706.17
        vsubsd    %xmm4, %xmm3, %xmm5                           #1707.404
        vaddsd    %xmm5, %xmm14, %xmm7                          #1707.457
        vaddsd    %xmm7, %xmm6, %xmm9                           #1708.17
        vaddsd    %xmm9, %xmm8, %xmm10                          #1709.19
        vorpd     %xmm2, %xmm10, %xmm0                          #1711.10
        andl      $1, %eax                                      #1673.94
        jne       ..B5.3        # Prob 5%                       #1713.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B5.2:                         # Preds ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1716.12
        popq      %rbp                                          #1716.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1716.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   16(%rsp), %xmm1                               #1713.160[spill]
        vmovsd    %xmm1, (%rsp)                                 #1713.160
        vmovsd    %xmm0, 64(%rsp)                               #1713.233
        jne       ..B5.5        # Prob 5%                       #1713.374
                                # LOE rbx r12 r13 r14 r15 eax
..B5.4:                         # Preds ..B5.6 ..B5.5 ..B5.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1713.626
        movq      %rbp, %rsp                                    #1713.626
        popq      %rbp                                          #1713.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1713.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.5:                         # Preds ..B5.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B5.4        # Prob 95%                      #1713.517
                                # LOE rbx r12 r13 r14 r15
..B5.6:                         # Preds ..B5.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1713.546
        lea       64(%rsp), %rsi                                #1713.546
..___tag_value___svml_tanh1_ha_l9.109:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1713.546
..___tag_value___svml_tanh1_ha_l9.110:
        jmp       ..B5.4        # Prob 100%                     #1713.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_tanh1_ha_l9,@function
	.size	__svml_tanh1_ha_l9,.-__svml_tanh1_ha_l9
..LN__svml_tanh1_ha_l9.4:
	.data
# -- End  __svml_tanh1_ha_l9
	.text
.L_2__routine_start___svml_tanh2_ha_ex_5:
# -- Begin  __svml_tanh2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh2_ha_ex
# --- __svml_tanh2_ha_ex(__m128d)
__svml_tanh2_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1721.1
        .byte     15                                            #1815.546
        .byte     30                                            #1815.546
        .byte     250                                           #1815.546
	.cfi_startproc
..___tag_value___svml_tanh2_ha_ex.112:
..L113:
                                                        #1721.1
        pushq     %rbp                                          #1721.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1721.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1721.1
        subq      $192, %rsp                                    #1721.1
        lea       2720+__svml_dtanh_ha_data_internal(%rip), %rsi #1775.67
        movq      10496+__svml_dtanh_ha_data_internal(%rip), %xmm14 #1755.23
        pxor      %xmm10, %xmm10                                #1770.17
        pshufd    $221, %xmm0, %xmm6                            #1762.9
        movq      10624+__svml_dtanh_ha_data_internal(%rip), %xmm9 #1756.25
        pand      %xmm14, %xmm6                                 #1763.16
        movq      10752+__svml_dtanh_ha_data_internal(%rip), %xmm11 #1766.25
        movdqa    %xmm6, %xmm1                                  #1764.17
        pcmpgtd   %xmm6, %xmm11                                 #1766.25
        psubd     %xmm9, %xmm6                                  #1769.13
        movq      10688+__svml_dtanh_ha_data_internal(%rip), %xmm7 #1757.22
        movdqa    %xmm6, %xmm4                                  #1771.17
        movdqa    %xmm6, %xmm8                                  #1770.17
        pcmpgtd   %xmm7, %xmm4                                  #1771.17
        pcmpgtd   %xmm10, %xmm8                                 #1770.17
        movdqa    %xmm4, %xmm5                                  #1773.46
        pand      %xmm8, %xmm6                                  #1772.17
        andps     %xmm4, %xmm7                                  #1773.123
        andnps    %xmm6, %xmm5                                  #1773.46
        orps      %xmm7, %xmm5                                  #1773.35
        psrld     $10, %xmm5                                    #1774.17
        movd      %xmm5, %eax                                   #1775.135
        pshufd    $1, %xmm5, %xmm2                              #1775.314
        movq      10560+__svml_dtanh_ha_data_internal(%rip), %xmm15 #1754.15
        movd      %xmm2, %ecx                                   #1775.296
        pcmpgtd   %xmm15, %xmm1                                 #1764.17
        por       %xmm11, %xmm1                                 #1767.21
        movups    10368+__svml_dtanh_ha_data_internal(%rip), %xmm13 #1760.13
        movslq    %eax, %rax                                    #1775.135
        andps     %xmm0, %xmm13                                 #1760.13
        movslq    %ecx, %rcx                                    #1775.228
        movups    10432+__svml_dtanh_ha_data_internal(%rip), %xmm12 #1759.12
        movups    %xmm0, (%rsp)                                 #1721.1[spill]
        andps     %xmm0, %xmm12                                 #1759.12
        movups    -96(%rax,%rsi), %xmm11                        #1775.67
        movups    -80(%rax,%rsi), %xmm8                         #1776.67
        movaps    %xmm11, %xmm10                                #1775.392
        movups    -64(%rax,%rsi), %xmm6                         #1777.67
        movaps    %xmm8, %xmm9                                  #1776.392
        movups    -32(%rax,%rsi), %xmm2                         #1779.67
        movaps    %xmm6, %xmm7                                  #1777.392
        movups    -96(%rcx,%rsi), %xmm3                         #1775.228
        movups    %xmm13, 16(%rsp)                              #1760.13[spill]
        movmskps  %xmm1, %edx                                   #1768.40
        unpcklpd  %xmm3, %xmm10                                 #1775.392
        unpckhpd  %xmm3, %xmm11                                 #1775.444
        movaps    %xmm2, %xmm3                                  #1779.392
        movups    -80(%rcx,%rsi), %xmm0                         #1776.228
        movups    -64(%rcx,%rsi), %xmm13                        #1777.228
        movups    -32(%rcx,%rsi), %xmm15                        #1779.228
        movups    -48(%rax,%rsi), %xmm4                         #1778.67
        movups    -16(%rax,%rsi), %xmm1                         #1780.67
        movaps    %xmm4, %xmm5                                  #1778.392
        unpcklpd  %xmm0, %xmm9                                  #1776.392
        unpckhpd  %xmm0, %xmm8                                  #1776.444
        movaps    %xmm1, %xmm0                                  #1780.394
        unpcklpd  %xmm13, %xmm7                                 #1777.392
        unpckhpd  %xmm13, %xmm6                                 #1777.444
        movups    -48(%rcx,%rsi), %xmm14                        #1778.228
        movups    -16(%rcx,%rsi), %xmm13                        #1780.229
        unpcklpd  %xmm15, %xmm3                                 #1779.392
        unpckhpd  %xmm15, %xmm2                                 #1779.444
        movups    (%rax,%rsi), %xmm15                           #1781.67
        unpcklpd  %xmm14, %xmm5                                 #1778.392
        unpckhpd  %xmm14, %xmm4                                 #1778.444
        unpcklpd  %xmm13, %xmm0                                 #1780.394
        unpckhpd  %xmm13, %xmm1                                 #1780.446
        movaps    %xmm15, %xmm13                                #1781.394
        movups    (%rcx,%rsi), %xmm14                           #1781.229
        unpcklpd  %xmm14, %xmm13                                #1781.394
        addpd     %xmm13, %xmm12                                #1782.19
        mulpd     %xmm12, %xmm1                                 #1783.27
        addpd     %xmm0, %xmm1                                  #1783.15
        mulpd     %xmm12, %xmm1                                 #1784.27
        addpd     %xmm2, %xmm1                                  #1784.15
        mulpd     %xmm12, %xmm1                                 #1785.27
        addpd     %xmm3, %xmm1                                  #1785.15
        mulpd     %xmm12, %xmm1                                 #1786.27
        addpd     %xmm4, %xmm1                                  #1786.15
        mulpd     %xmm12, %xmm1                                 #1787.27
        addpd     %xmm5, %xmm1                                  #1787.15
        mulpd     %xmm12, %xmm1                                 #1788.27
        addpd     %xmm6, %xmm1                                  #1788.15
        mulpd     %xmm12, %xmm1                                 #1789.27
        addpd     %xmm7, %xmm1                                  #1789.15
        mulpd     %xmm12, %xmm1                                 #1790.27
        addpd     %xmm8, %xmm1                                  #1790.15
        mulpd     %xmm12, %xmm1                                 #1797.19
        movups    10304+__svml_dtanh_ha_data_internal(%rip), %xmm0 #1799.60
        movaps    %xmm0, %xmm2                                  #1800.22
        andps     %xmm9, %xmm2                                  #1800.22
        andps     %xmm12, %xmm0                                 #1802.22
        subpd     %xmm2, %xmm9                                  #1801.22
        mulpd     %xmm12, %xmm1                                 #1798.31
        subpd     %xmm0, %xmm12                                 #1803.22
        addpd     %xmm10, %xmm1                                 #1798.19
        movaps    %xmm9, %xmm3                                  #1805.21
        movaps    %xmm2, %xmm4                                  #1804.21
        mulpd     %xmm12, %xmm3                                 #1805.21
        mulpd     %xmm12, %xmm2                                 #1806.33
        mulpd     %xmm0, %xmm4                                  #1804.21
        mulpd     %xmm9, %xmm0                                  #1807.33
        addpd     %xmm2, %xmm3                                  #1806.21
        addpd     %xmm0, %xmm3                                  #1807.21
        movaps    %xmm11, %xmm0                                 #1809.188
        movaps    %xmm4, %xmm2                                  #1809.235
        addpd     %xmm4, %xmm0                                  #1809.188
        addpd     %xmm3, %xmm1                                  #1808.17
        subpd     %xmm0, %xmm2                                  #1809.235
        movaps    %xmm0, %xmm12                                 #1809.288
        addpd     %xmm2, %xmm12                                 #1809.288
        addpd     %xmm11, %xmm2                                 #1809.349
        subpd     %xmm12, %xmm4                                 #1809.404
        addpd     %xmm4, %xmm2                                  #1809.457
        addpd     %xmm2, %xmm1                                  #1810.17
        addpd     %xmm1, %xmm0                                  #1811.19
        orps      16(%rsp), %xmm0                               #1813.10[spill]
        andl      $3, %edx                                      #1768.94
        jne       ..B6.3        # Prob 5%                       #1815.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1818.12
        popq      %rbp                                          #1818.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1818.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm1                                 #1815.197[spill]
        movups    %xmm1, 64(%rsp)                               #1815.197
        movups    %xmm0, 128(%rsp)                              #1815.270
        je        ..B6.2        # Prob 95%                      #1815.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1815.454
        movq      %r12, 8(%rsp)                                 #1815.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1815.454
        movq      %r13, (%rsp)                                  #1815.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1815.454
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1815.517
        jc        ..B6.10       # Prob 5%                       #1815.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1815.470
        cmpl      $2, %r12d                                     #1815.465
        jl        ..B6.7        # Prob 82%                      #1815.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1815.674
        jmp       ..B6.2        # Prob 100%                     #1815.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1815.546
        lea       128(%rsp,%r12,8), %rsi                        #1815.546
..___tag_value___svml_tanh2_ha_ex.134:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1815.546
..___tag_value___svml_tanh2_ha_ex.135:
        jmp       ..B6.8        # Prob 100%                     #1815.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh2_ha_ex,@function
	.size	__svml_tanh2_ha_ex,.-__svml_tanh2_ha_ex
..LN__svml_tanh2_ha_ex.5:
	.data
# -- End  __svml_tanh2_ha_ex
	.text
.L_2__routine_start___svml_tanh2_ha_e9_6:
# -- Begin  __svml_tanh2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh2_ha_e9
# --- __svml_tanh2_ha_e9(__m128d)
__svml_tanh2_ha_e9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1823.1
        .byte     15                                            #1917.546
        .byte     30                                            #1917.546
        .byte     250                                           #1917.546
	.cfi_startproc
..___tag_value___svml_tanh2_ha_e9.137:
..L138:
                                                        #1823.1
        pushq     %rbp                                          #1823.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1823.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1823.1
        subq      $192, %rsp                                    #1823.1
        lea       2720+__svml_dtanh_ha_data_internal(%rip), %rsi #1877.67
        movups    10432+__svml_dtanh_ha_data_internal(%rip), %xmm13 #1861.12
        pxor      %xmm9, %xmm9                                  #1872.23
        movups    10368+__svml_dtanh_ha_data_internal(%rip), %xmm14 #1862.13
        andps     %xmm0, %xmm13                                 #1861.12
        movups    %xmm0, (%rsp)                                 #1823.1[spill]
        andps     %xmm0, %xmm14                                 #1862.13
        movq      10496+__svml_dtanh_ha_data_internal(%rip), %xmm15 #1857.29
        pshufd    $221, %xmm0, %xmm0                            #1864.15
        movq      10624+__svml_dtanh_ha_data_internal(%rip), %xmm11 #1858.31
        pand      %xmm15, %xmm0                                 #1865.22
        movq      10752+__svml_dtanh_ha_data_internal(%rip), %xmm2 #1868.31
        movdqa    %xmm0, %xmm10                                 #1866.23
        pcmpgtd   %xmm0, %xmm2                                  #1868.31
        psubd     %xmm11, %xmm0                                 #1871.19
        movdqa    %xmm0, %xmm8                                  #1872.23
        movq      10688+__svml_dtanh_ha_data_internal(%rip), %xmm7 #1859.28
        pcmpgtd   %xmm9, %xmm8                                  #1872.23
        pand      %xmm0, %xmm8                                  #1874.23
        pcmpgtd   %xmm7, %xmm0                                  #1873.23
        blendvps  %xmm0, %xmm7, %xmm8                           #1875.41
        psrld     $10, %xmm8                                    #1876.23
        movd      %xmm8, %eax                                   #1877.135
        movq      10560+__svml_dtanh_ha_data_internal(%rip), %xmm12 #1856.21
        pcmpgtd   %xmm12, %xmm10                                #1866.23
        movslq    %eax, %rax                                    #1877.135
        por       %xmm2, %xmm10                                 #1869.27
        pextrd    $1, %xmm8, %ecx                               #1877.298
        movslq    %ecx, %rcx                                    #1877.230
        movmskps  %xmm10, %edx                                  #1870.40
        movups    -96(%rax,%rsi), %xmm12                        #1877.67
        movups    -80(%rax,%rsi), %xmm9                         #1878.67
        movaps    %xmm12, %xmm11                                #1877.353
        movups    -64(%rax,%rsi), %xmm7                         #1879.67
        movaps    %xmm9, %xmm10                                 #1878.353
        movups    -96(%rcx,%rsi), %xmm5                         #1877.230
        movaps    %xmm7, %xmm8                                  #1879.353
        movups    -80(%rcx,%rsi), %xmm6                         #1878.230
        movups    -64(%rcx,%rsi), %xmm3                         #1879.230
        unpcklpd  %xmm5, %xmm11                                 #1877.353
        unpckhpd  %xmm5, %xmm12                                 #1877.399
        movups    -48(%rax,%rsi), %xmm5                         #1880.67
        unpcklpd  %xmm6, %xmm10                                 #1878.353
        unpckhpd  %xmm6, %xmm9                                  #1878.399
        movaps    %xmm5, %xmm6                                  #1880.353
        unpcklpd  %xmm3, %xmm8                                  #1879.353
        unpckhpd  %xmm3, %xmm7                                  #1879.399
        movups    -32(%rax,%rsi), %xmm3                         #1881.67
        movups    -48(%rcx,%rsi), %xmm4                         #1880.230
        movups    -32(%rcx,%rsi), %xmm1                         #1881.230
        unpcklpd  %xmm4, %xmm6                                  #1880.353
        unpckhpd  %xmm4, %xmm5                                  #1880.399
        movaps    %xmm3, %xmm4                                  #1881.353
        movups    -16(%rax,%rsi), %xmm2                         #1882.67
        movups    %xmm14, 16(%rsp)                              #1862.13[spill]
        unpcklpd  %xmm1, %xmm4                                  #1881.353
        unpckhpd  %xmm1, %xmm3                                  #1881.399
        movaps    %xmm2, %xmm1                                  #1882.355
        movups    -16(%rcx,%rsi), %xmm14                        #1882.231
        movups    (%rax,%rsi), %xmm0                            #1883.67
        unpcklpd  %xmm14, %xmm1                                 #1882.355
        unpckhpd  %xmm14, %xmm2                                 #1882.401
        movaps    %xmm0, %xmm14                                 #1883.355
        movups    (%rcx,%rsi), %xmm15                           #1883.231
        unpcklpd  %xmm15, %xmm14                                #1883.355
        addpd     %xmm14, %xmm13                                #1884.19
        mulpd     %xmm13, %xmm2                                 #1885.27
        addpd     %xmm1, %xmm2                                  #1885.15
        mulpd     %xmm13, %xmm2                                 #1886.27
        addpd     %xmm3, %xmm2                                  #1886.15
        mulpd     %xmm13, %xmm2                                 #1887.27
        addpd     %xmm4, %xmm2                                  #1887.15
        mulpd     %xmm13, %xmm2                                 #1888.27
        addpd     %xmm5, %xmm2                                  #1888.15
        mulpd     %xmm13, %xmm2                                 #1889.27
        addpd     %xmm6, %xmm2                                  #1889.15
        mulpd     %xmm13, %xmm2                                 #1890.27
        addpd     %xmm7, %xmm2                                  #1890.15
        mulpd     %xmm13, %xmm2                                 #1891.27
        addpd     %xmm8, %xmm2                                  #1891.15
        mulpd     %xmm13, %xmm2                                 #1892.27
        addpd     %xmm9, %xmm2                                  #1892.15
        mulpd     %xmm13, %xmm2                                 #1899.19
        movups    10304+__svml_dtanh_ha_data_internal(%rip), %xmm1 #1901.60
        movaps    %xmm1, %xmm3                                  #1902.22
        andps     %xmm10, %xmm3                                 #1902.22
        andps     %xmm13, %xmm1                                 #1904.22
        subpd     %xmm3, %xmm10                                 #1903.22
        mulpd     %xmm13, %xmm2                                 #1900.31
        subpd     %xmm1, %xmm13                                 #1905.22
        addpd     %xmm11, %xmm2                                 #1900.19
        movaps    %xmm10, %xmm4                                 #1907.21
        movaps    %xmm3, %xmm5                                  #1906.21
        mulpd     %xmm13, %xmm4                                 #1907.21
        mulpd     %xmm13, %xmm3                                 #1908.33
        mulpd     %xmm1, %xmm5                                  #1906.21
        mulpd     %xmm10, %xmm1                                 #1909.33
        addpd     %xmm3, %xmm4                                  #1908.21
        movaps    %xmm12, %xmm0                                 #1911.188
        addpd     %xmm5, %xmm0                                  #1911.188
        addpd     %xmm1, %xmm4                                  #1909.21
        movaps    %xmm5, %xmm1                                  #1911.235
        movaps    %xmm0, %xmm13                                 #1911.288
        subpd     %xmm0, %xmm1                                  #1911.235
        addpd     %xmm4, %xmm2                                  #1910.17
        addpd     %xmm1, %xmm13                                 #1911.288
        addpd     %xmm12, %xmm1                                 #1911.349
        subpd     %xmm13, %xmm5                                 #1911.404
        addpd     %xmm5, %xmm1                                  #1911.457
        addpd     %xmm1, %xmm2                                  #1912.17
        addpd     %xmm2, %xmm0                                  #1913.19
        orps      16(%rsp), %xmm0                               #1915.10[spill]
        andl      $3, %edx                                      #1870.94
        jne       ..B7.3        # Prob 5%                       #1917.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B7.2:                         # Preds ..B7.3 ..B7.9 ..B7.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1920.12
        popq      %rbp                                          #1920.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1920.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm1                                 #1917.197[spill]
        movups    %xmm1, 64(%rsp)                               #1917.197
        movups    %xmm0, 128(%rsp)                              #1917.270
        je        ..B7.2        # Prob 95%                      #1917.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B7.6:                         # Preds ..B7.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1917.454
        movq      %r12, 8(%rsp)                                 #1917.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1917.454
        movq      %r13, (%rsp)                                  #1917.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1917.454
                                # LOE rbx r12 r14 r15 r13d
..B7.7:                         # Preds ..B7.8 ..B7.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1917.517
        jc        ..B7.10       # Prob 5%                       #1917.517
                                # LOE rbx r12 r14 r15 r13d
..B7.8:                         # Preds ..B7.10 ..B7.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1917.470
        cmpl      $2, %r12d                                     #1917.465
        jl        ..B7.7        # Prob 82%                      #1917.465
                                # LOE rbx r12 r14 r15 r13d
..B7.9:                         # Preds ..B7.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1917.674
        jmp       ..B7.2        # Prob 100%                     #1917.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.10:                        # Preds ..B7.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1917.546
        lea       128(%rsp,%r12,8), %rsi                        #1917.546
..___tag_value___svml_tanh2_ha_e9.159:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #1917.546
..___tag_value___svml_tanh2_ha_e9.160:
        jmp       ..B7.8        # Prob 100%                     #1917.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh2_ha_e9,@function
	.size	__svml_tanh2_ha_e9,.-__svml_tanh2_ha_e9
..LN__svml_tanh2_ha_e9.6:
	.data
# -- End  __svml_tanh2_ha_e9
	.text
.L_2__routine_start___svml_tanh1_ha_e9_7:
# -- Begin  __svml_tanh1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh1_ha_e9
# --- __svml_tanh1_ha_e9(__m128d)
__svml_tanh1_ha_e9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1925.1
        .byte     15                                            #2019.546
        .byte     30                                            #2019.546
        .byte     250                                           #2019.546
	.cfi_startproc
..___tag_value___svml_tanh1_ha_e9.162:
..L163:
                                                        #1925.1
        pushq     %rbp                                          #1925.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1925.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1925.1
        subq      $128, %rsp                                    #1925.1
        movl      $2147352576, %edx                             #1967.22
        movl      $535822336, %esi                              #1970.31
        movl      $1069416448, %edi                             #1973.19
        movsd     10432+__svml_dtanh_ha_data_internal(%rip), %xmm4 #1963.12
        movl      $7733248, %eax                                #1961.28
        movsd     10368+__svml_dtanh_ha_data_internal(%rip), %xmm5 #1964.13
        andps     %xmm0, %xmm4                                  #1963.12
        movups    %xmm0, 16(%rsp)                               #1925.1[spill]
        andps     %xmm0, %xmm5                                  #1964.13
        pshufd    $85, %xmm0, %xmm0                             #1966.15
        movd      %edx, %xmm6                                   #1967.22
        pand      %xmm6, %xmm0                                  #1967.22
        movd      %esi, %xmm8                                   #1970.31
        movd      %edi, %xmm10                                  #1973.19
        movdqa    %xmm0, %xmm9                                  #1968.23
        pcmpgtd   %xmm0, %xmm8                                  #1970.31
        psubd     %xmm10, %xmm0                                 #1973.19
        movdqa    %xmm0, %xmm13                                 #1974.23
        movd      %eax, %xmm12                                  #1961.28
        pxor      %xmm11, %xmm11                                #1974.23
        lea       2728+__svml_dtanh_ha_data_internal(%rip), %r9 #1979.22
        pcmpgtd   %xmm11, %xmm13                                #1974.23
        movl      $2145386496, %ecx                             #1968.23
        pand      %xmm0, %xmm13                                 #1976.23
        pcmpgtd   %xmm12, %xmm0                                 #1975.23
        blendvps  %xmm0, %xmm12, %xmm13                         #1977.41
        psrld     $10, %xmm13                                   #1978.23
        movd      %xmm13, %r8d                                  #1979.113
        movd      %ecx, %xmm7                                   #1968.23
        pcmpgtd   %xmm7, %xmm9                                  #1968.23
        por       %xmm8, %xmm9                                  #1971.27
        movslq    %r8d, %r8                                     #1979.113
        movmskps  %xmm9, %eax                                   #1972.40
        movups    -8(%r9,%r8), %xmm15                           #1985.66
        movsd     -16(%r9,%r8), %xmm1                           #1984.66
        addsd     %xmm15, %xmm4                                 #1986.19
        unpcklpd  %xmm1, %xmm1                                  #1984.165
        mulsd     %xmm4, %xmm1                                  #1987.27
        movups    -24(%r9,%r8), %xmm0                           #1984.66
        movsd     -32(%r9,%r8), %xmm6                           #1983.66
        addsd     %xmm0, %xmm1                                  #1987.15
        mulsd     %xmm4, %xmm1                                  #1988.27
        unpcklpd  %xmm6, %xmm6                                  #1983.164
        movups    -40(%r9,%r8), %xmm7                           #1983.66
        addsd     %xmm6, %xmm1                                  #1988.15
        mulsd     %xmm4, %xmm1                                  #1989.27
        movsd     -48(%r9,%r8), %xmm8                           #1982.66
        addsd     %xmm7, %xmm1                                  #1989.15
        mulsd     %xmm4, %xmm1                                  #1990.27
        unpcklpd  %xmm8, %xmm8                                  #1982.164
        movups    -56(%r9,%r8), %xmm9                           #1982.66
        addsd     %xmm8, %xmm1                                  #1990.15
        mulsd     %xmm4, %xmm1                                  #1991.27
        movsd     -64(%r9,%r8), %xmm10                          #1981.66
        addsd     %xmm9, %xmm1                                  #1991.15
        mulsd     %xmm4, %xmm1                                  #1992.27
        unpcklpd  %xmm10, %xmm10                                #1981.164
        movups    -72(%r9,%r8), %xmm11                          #1981.66
        addsd     %xmm10, %xmm1                                 #1992.15
        mulsd     %xmm4, %xmm1                                  #1993.27
        movsd     -80(%r9,%r8), %xmm12                          #1980.66
        addsd     %xmm11, %xmm1                                 #1993.15
        mulsd     %xmm4, %xmm1                                  #1994.27
        unpcklpd  %xmm12, %xmm12                                #1980.164
        addsd     %xmm12, %xmm1                                 #1994.15
        mulsd     %xmm4, %xmm1                                  #2001.19
        movsd     10304+__svml_dtanh_ha_data_internal(%rip), %xmm14 #2003.29
        movups    -88(%r9,%r8), %xmm3                           #1980.66
        movaps    %xmm14, %xmm6                                 #2004.22
        andps     %xmm3, %xmm6                                  #2004.22
        andps     %xmm4, %xmm14                                 #2006.22
        subsd     %xmm6, %xmm3                                  #2005.22
        mulsd     %xmm4, %xmm1                                  #2002.31
        subsd     %xmm14, %xmm4                                 #2007.22
        movaps    %xmm3, %xmm0                                  #2009.21
        movaps    %xmm6, %xmm7                                  #2008.21
        mulsd     %xmm4, %xmm0                                  #2009.21
        mulsd     %xmm4, %xmm6                                  #2010.33
        mulsd     %xmm14, %xmm7                                 #2008.21
        mulsd     %xmm14, %xmm3                                 #2011.33
        addsd     %xmm0, %xmm6                                  #2010.21
        movups    -104(%r8,%r9), %xmm13                         #1979.66
        movaps    %xmm7, %xmm0                                  #2013.188
        movsd     -96(%r8,%r9), %xmm2                           #1979.66
        addsd     %xmm13, %xmm1                                 #2002.19
        addsd     %xmm6, %xmm3                                  #2011.21
        unpcklpd  %xmm2, %xmm2                                  #1979.164
        addsd     %xmm1, %xmm3                                  #2012.17
        addsd     %xmm2, %xmm0                                  #2013.188
        movaps    %xmm7, %xmm1                                  #2013.235
        movaps    %xmm0, %xmm4                                  #2013.288
        subsd     %xmm0, %xmm1                                  #2013.235
        addsd     %xmm1, %xmm4                                  #2013.288
        addsd     %xmm1, %xmm2                                  #2013.349
        subsd     %xmm4, %xmm7                                  #2013.404
        addsd     %xmm7, %xmm2                                  #2013.457
        addsd     %xmm2, %xmm3                                  #2014.17
        addsd     %xmm3, %xmm0                                  #2015.19
        orps      %xmm5, %xmm0                                  #2017.10
        andl      $1, %eax                                      #1972.94
        jne       ..B8.3        # Prob 5%                       #2019.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B8.2:                         # Preds ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2022.12
        popq      %rbp                                          #2022.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2022.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movups    16(%rsp), %xmm1                               #2019.160[spill]
        movsd     %xmm1, (%rsp)                                 #2019.160
        movsd     %xmm0, 64(%rsp)                               #2019.233
        jne       ..B8.6        # Prob 5%                       #2019.374
                                # LOE rbx r12 r13 r14 r15
..B8.4:                         # Preds ..B8.6 ..B8.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #2019.626
        movq      %rbp, %rsp                                    #2019.626
        popq      %rbp                                          #2019.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2019.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.6:                         # Preds ..B8.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #2019.546
        lea       64(%rsp), %rsi                                #2019.546
..___tag_value___svml_tanh1_ha_e9.177:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #2019.546
..___tag_value___svml_tanh1_ha_e9.178:
        jmp       ..B8.4        # Prob 100%                     #2019.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_tanh1_ha_e9,@function
	.size	__svml_tanh1_ha_e9,.-__svml_tanh1_ha_e9
..LN__svml_tanh1_ha_e9.7:
	.data
# -- End  __svml_tanh1_ha_e9
	.text
.L_2__routine_start___svml_tanh8_ha_z0_8:
# -- Begin  __svml_tanh8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanh8_ha_z0
# --- __svml_tanh8_ha_z0(__m512d)
__svml_tanh8_ha_z0:
# parameter 1: %zmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2027.1
        .byte     15                                            #2117.759
        .byte     30                                            #2117.759
        .byte     250                                           #2117.759
	.cfi_startproc
..___tag_value___svml_tanh8_ha_z0.180:
..L181:
                                                        #2027.1
        pushq     %rbp                                          #2027.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2027.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2027.1
        subq      $320, %rsp                                    #2027.1
        vpsrlq    $32, %zmm0, %zmm4                             #2068.31
        vmovups   %zmm0, 192(%rsp)                              #2027.1[spill]
        vmovups   __svml_dtanh_ha_data_internal(%rip), %zmm14   #2077.257
        vmovups   128+__svml_dtanh_ha_data_internal(%rip), %zmm15 #2079.267
        vpmovqd   %zmm4, %ymm5                                  #2068.9
        vandpd    10432+__svml_dtanh_ha_data_internal(%rip), %zmm0, %zmm13 #2065.12
        vandpd    10368+__svml_dtanh_ha_data_internal(%rip), %zmm0, %zmm3 #2066.13
        vpand     2432+__svml_dtanh_ha_data_internal(%rip), %ymm5, %ymm7 #2069.16
        vmovups   384+__svml_dtanh_ha_data_internal(%rip), %zmm0 #2080.267
        vmovups   2176+__svml_dtanh_ha_data_internal(%rip), %zmm4 #2094.274
        vmovups   2048+__svml_dtanh_ha_data_internal(%rip), %zmm5 #2093.274
        vmovups   %zmm3, (%rsp)                                 #2066.13[spill]
        vmovups   512+__svml_dtanh_ha_data_internal(%rip), %zmm3 #2081.267
        vpsubd    2496+__svml_dtanh_ha_data_internal(%rip), %ymm7, %ymm8 #2072.13
        vxorps    %ymm9, %ymm9, %ymm9                           #2073.17
        vpmaxsd   %ymm9, %ymm8, %ymm10                          #2073.17
        vpminsd   2560+__svml_dtanh_ha_data_internal(%rip), %ymm10, %ymm11 #2074.17
        vpsrld    $19, %ymm11, %ymm12                           #2075.23
        vmovups   1664+__svml_dtanh_ha_data_internal(%rip), %zmm8 #2090.274
        vmovups   1536+__svml_dtanh_ha_data_internal(%rip), %zmm9 #2089.270
        vmovups   1408+__svml_dtanh_ha_data_internal(%rip), %zmm10 #2088.270
        vmovups   1280+__svml_dtanh_ha_data_internal(%rip), %zmm11 #2087.267
        vpmovzxdq %ymm12, %zmm2                                 #2076.23
        vmovups   1152+__svml_dtanh_ha_data_internal(%rip), %zmm12 #2086.267
        vpermt2pd 448+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm0 #2080.267
        vpermt2pd 64+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm14 #2077.257
        vpermt2pd 2240+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm4 #2094.274
        vpermt2pd 2112+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm5 #2093.274
        vsubpd    {rn-sae}, %zmm14, %zmm13, %zmm1               #2078.21
        vpermt2pd 1728+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm8 #2090.274
        vpermt2pd 1600+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm9 #2089.270
        vpermt2pd 1472+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm10 #2088.270
        vpermt2pd 1344+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm11 #2087.267
        vpermt2pd 1216+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm12 #2086.267
        vpermt2pd 576+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm3 #2081.267
        vpermt2pd 192+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm15 #2079.267
        vmovups   %zmm0, 128(%rsp)                              #2080.267[spill]
        vmovups   2304+__svml_dtanh_ha_data_internal(%rip), %zmm0 #2095.274
        vmovups   1024+__svml_dtanh_ha_data_internal(%rip), %zmm13 #2085.267
        vmovups   896+__svml_dtanh_ha_data_internal(%rip), %zmm14 #2084.267
        vmovups   %zmm3, 256(%rsp)                              #2081.267[spill]
        vmovups   768+__svml_dtanh_ha_data_internal(%rip), %zmm3 #2083.267
        vmovups   %zmm15, 64(%rsp)                              #2079.267[spill]
        vmovups   640+__svml_dtanh_ha_data_internal(%rip), %zmm15 #2082.267
        vpermt2pd 2368+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm0 #2095.274
        vpermt2pd 1088+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm13 #2085.267
        vpermt2pd 960+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm14 #2084.267
        vpermt2pd 832+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm3 #2083.267
        vpermt2pd 704+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm15 #2082.267
        vfmadd213pd {rn-sae}, %zmm4, %zmm1, %zmm0               #2096.19
        vmovups   256+__svml_dtanh_ha_data_internal(%rip), %zmm4 #2110.271
        vfmadd213pd {rn-sae}, %zmm5, %zmm1, %zmm0               #2097.19
        vpermt2pd 320+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm4 #2110.271
        vpcmpgtd  10560+__svml_dtanh_ha_data_internal(%rip), %ymm7, %ymm6 #2070.19
        vmovmskps %ymm6, %esi                                   #2071.40
        vmovups   1920+__svml_dtanh_ha_data_internal(%rip), %zmm6 #2092.274
        vmovups   1792+__svml_dtanh_ha_data_internal(%rip), %zmm7 #2091.274
        vpermt2pd 1984+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm6 #2092.274
        vpermt2pd 1856+__svml_dtanh_ha_data_internal(%rip), %zmm2, %zmm7 #2091.274
        vfmadd213pd {rn-sae}, %zmm6, %zmm1, %zmm0               #2098.19
        vfmadd213pd {rn-sae}, %zmm7, %zmm1, %zmm0               #2099.19
        vfmadd213pd {rn-sae}, %zmm8, %zmm1, %zmm0               #2100.19
        vfmadd213pd {rn-sae}, %zmm9, %zmm1, %zmm0               #2101.19
        vfmadd213pd {rn-sae}, %zmm10, %zmm1, %zmm0              #2102.19
        vfmadd213pd {rn-sae}, %zmm11, %zmm1, %zmm0              #2103.15
        vfmadd213pd {rn-sae}, %zmm12, %zmm1, %zmm0              #2104.15
        vfmadd213pd {rn-sae}, %zmm13, %zmm1, %zmm0              #2105.15
        vfmadd213pd {rn-sae}, %zmm14, %zmm1, %zmm0              #2106.15
        vfmadd213pd {rn-sae}, %zmm3, %zmm1, %zmm0               #2107.15
        vmovups   256(%rsp), %zmm3                              #2109.15[spill]
        vfmadd213pd {rn-sae}, %zmm15, %zmm1, %zmm0              #2108.15
        vfmadd213pd {rn-sae}, %zmm3, %zmm1, %zmm0               #2109.15
        vmulpd    {rn-sae}, %zmm1, %zmm0, %zmm2                 #2111.21
        vmovups   128(%rsp), %zmm0                              #2113.19[spill]
        vfmadd213pd {rn-sae}, %zmm4, %zmm1, %zmm2               #2112.19
        vfmadd213pd {rn-sae}, %zmm2, %zmm1, %zmm0               #2113.19
        vmovups   64(%rsp), %zmm1                               #2114.21[spill]
        vaddpd    {rn-sae}, %zmm1, %zmm0, %zmm0                 #2114.21
        vorpd     (%rsp), %zmm0, %zmm0                          #2115.10[spill]
        testl     %esi, %esi                                    #2117.52
        jne       ..B9.3        # Prob 5%                       #2117.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0
..B9.2:                         # Preds ..B9.9 ..B9.7 ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2120.12
        popq      %rbp                                          #2120.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2120.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2117.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0
..B9.4:                         # Preds ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #2117.249
        movl      %edx, %eax                                    #2117.303
        orl       $8064, %eax                                   #2117.303
        cmpl      %eax, %edx                                    #2117.332
        je        ..B9.6        # Prob 78%                      #2117.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.5:                         # Preds ..B9.4
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #2117.344
        vldmxcsr  32(%rsp)                                      #2117.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.6:                         # Preds ..B9.5 ..B9.4
                                # Execution count [5.00e-02]: Infreq
        vmovups   192(%rsp), %zmm1                              #2117.406[spill]
        vmovups   %zmm0, 128(%rsp)                              #2117.481
        vmovups   %zmm1, 64(%rsp)                               #2117.406
        testl     %esi, %esi                                    #2117.587
        jne       ..B9.11       # Prob 5%                       #2117.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.7:                         # Preds ..B9.14 ..B9.6
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #2117.931
        je        ..B9.2        # Prob 78%                      #2117.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B9.8:                         # Preds ..B9.7
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #2117.956
        movl      32(%rsp), %eax                                #2117.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.9:                         # Preds ..B9.8
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #2117.943
        orl       %edx, %eax                                    #2117.943
        movl      %eax, 32(%rsp)                                #2117.943
        vldmxcsr  32(%rsp)                                      #2117.943
        jmp       ..B9.2        # Prob 100%                     #2117.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B9.11:                        # Preds ..B9.6
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #2117.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B9.18:                        # Preds ..B9.11
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 16(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xd0, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc8, 0xfe, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
        movq      %r14, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
        movl      %esi, %r14d                                   #
        movq      %rbx, 24(%rsp)                                #[spill]
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xd8, 0xfe, 0xff, 0xff, 0x22
        movl      %ecx, %ebx                                    #
                                # LOE rbx r15 r12d r13d r14d
..B9.12:                        # Preds ..B9.13 ..B9.18
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #2117.730
        jc        ..B9.15       # Prob 5%                       #2117.730
                                # LOE rbx r15 r12d r13d r14d
..B9.13:                        # Preds ..B9.15 ..B9.12
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #2117.683
        cmpl      $8, %ebx                                      #2117.678
        jl        ..B9.12       # Prob 82%                      #2117.678
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
        vmovups   128(%rsp), %zmm0                              #2117.887
        jmp       ..B9.7        # Prob 100%                     #2117.887
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xd8, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xd0, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc8, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.15:                        # Preds ..B9.12
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #2117.759
        lea       128(%rsp,%rbx,8), %rsi                        #2117.759
..___tag_value___svml_tanh8_ha_z0.218:
#       __svml_dtanh_ha_cout_rare_internal(const double *, double *)
        call      __svml_dtanh_ha_cout_rare_internal            #2117.759
..___tag_value___svml_tanh8_ha_z0.219:
        jmp       ..B9.13       # Prob 100%                     #2117.759
        .align    16,0x90
                                # LOE r15 ebx r12d r13d r14d
	.cfi_endproc
# mark_end;
	.type	__svml_tanh8_ha_z0,@function
	.size	__svml_tanh8_ha_z0,.-__svml_tanh8_ha_z0
..LN__svml_tanh8_ha_z0.8:
	.data
# -- End  __svml_tanh8_ha_z0
	.text
.L_2__routine_start___svml_dtanh_ha_cout_rare_internal_9:
# -- Begin  __svml_dtanh_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dtanh_ha_cout_rare_internal
	.globl __svml_dtanh_ha_cout_rare_internal
# --- __svml_dtanh_ha_cout_rare_internal(const double *, double *)
__svml_dtanh_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1196.1
        .byte     15                                            #1219.9
        .byte     30                                            #1219.9
        .byte     250                                           #1219.9
	.cfi_startproc
..___tag_value___svml_dtanh_ha_cout_rare_internal.221:
..L222:
                                                        #1196.1
        lea       _imldTanhHATab(%rip), %rdx                    #1200.30
        movb      7(%rdi), %al                                  #1200.68
        andb      $-128, %al                                    #1200.68
        shrb      $7, %al                                       #1200.68
        movzbl    %al, %ecx                                     #1200.30
        movzwl    6(%rdi), %eax                                 #1201.32
        andl      $32752, %eax                                  #1201.32
        shrl      $4, %eax                                      #1201.32
        movq      (%rdx,%rcx,8), %rdx                           #1200.30
        cmpl      $2047, %eax                                   #1201.57
        je        ..B10.6       # Prob 16%                      #1201.57
                                # LOE rdx rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        cmpl      $2046, %eax                                   #1204.22
        jne       ..B10.4       # Prob 50%                      #1204.22
                                # LOE rdx rbx rbp rsi rdi r12 r13 r14 r15
..B10.3:                        # Preds ..B10.7 ..B10.2
                                # Execution count [4.20e-01]
        movq      %rdx, (%rsi)                                  #1206.13
        jmp       ..B10.5       # Prob 100%                     #1206.13
                                # LOE rbx rbp r12 r13 r14 r15
..B10.4:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     (%rdi), %xmm1                                 #1210.20
        movsd     _imldTanhHATab(%rip), %xmm0                   #1210.67
        addsd     %xmm1, %xmm0                                  #1210.67
        mulsd     %xmm0, %xmm1                                  #1210.67
        movsd     %xmm1, (%rsi)                                 #1210.13
                                # LOE rbx rbp r12 r13 r14 r15
..B10.5:                        # Preds ..B10.3 ..B10.4 ..B10.9
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #1221.12
        ret                                                     #1221.12
                                # LOE
..B10.6:                        # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        testl     $1048575, 4(%rdi)                             #1213.37
        jne       ..B10.9       # Prob 50%                      #1213.68
                                # LOE rdx rbx rbp rsi rdi r12 r13 r14 r15
..B10.7:                        # Preds ..B10.6
                                # Execution count [8.00e-02]: Infreq
        cmpl      $0, (%rdi)                                    #1213.127
        je        ..B10.3       # Prob 50%                      #1213.127
                                # LOE rdx rbx rbp rsi rdi r12 r13 r14 r15
..B10.9:                        # Preds ..B10.7 ..B10.6
                                # Execution count [1.20e-01]: Infreq
        movsd     (%rdi), %xmm0                                 #1219.16
        addsd     %xmm0, %xmm0                                  #1219.23
        movsd     %xmm0, (%rsi)                                 #1219.9
        jmp       ..B10.5       # Prob 100%                     #1219.9
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_dtanh_ha_cout_rare_internal,@function
	.size	__svml_dtanh_ha_cout_rare_internal,.-__svml_dtanh_ha_cout_rare_internal
..LN__svml_dtanh_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dtanh_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dtanh_ha_data_internal
	.globl __svml_dtanh_ha_data_internal
__svml_dtanh_ha_data_internal:
	.long	0
	.long	0
	.long	0
	.long	1070333952
	.long	0
	.long	1070858240
	.long	0
	.long	1071382528
	.long	0
	.long	1071906816
	.long	0
	.long	1072431104
	.long	0
	.long	1072955392
	.long	0
	.long	1073479680
	.long	0
	.long	1074003968
	.long	0
	.long	1074528256
	.long	0
	.long	1075052544
	.long	0
	.long	1075576832
	.long	0
	.long	1076101120
	.long	0
	.long	1076625408
	.long	0
	.long	1077149696
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1097497746
	.long	1070305232
	.long	2699715854
	.long	1070817176
	.long	3997728823
	.long	1071273769
	.long	3938160533
	.long	1071759175
	.long	193732629
	.long	1072072293
	.long	3144363502
	.long	1072375075
	.long	2548249895
	.long	1072570303
	.long	638218690
	.long	1072665176
	.long	3325726839
	.long	1072689426
	.long	2523455249
	.long	1072693057
	.long	2200391922
	.long	1072693244
	.long	4257836853
	.long	1072693247
	.long	4294954840
	.long	1072693247
	.long	0
	.long	1072693248
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	3167993022
	.long	1013310014
	.long	2889539328
	.long	3159121931
	.long	394645568
	.long	1013728265
	.long	2799920971
	.long	1015161303
	.long	754366556
	.long	1013577885
	.long	3960180508
	.long	1014215657
	.long	69774042
	.long	3162475851
	.long	2161066944
	.long	3162264280
	.long	923362432
	.long	1014879086
	.long	2160345802
	.long	1014714211
	.long	3729195158
	.long	3161626453
	.long	675671997
	.long	1015336837
	.long	1141527416
	.long	1013957883
	.long	2209699952
	.long	3160643600
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	2750448946
	.long	1072596012
	.long	1257606939
	.long	1072501079
	.long	2619663609
	.long	1072338011
	.long	863303701
	.long	1072048204
	.long	2694810990
	.long	1071654144
	.long	837003456
	.long	1070723665
	.long	472162617
	.long	1069359818
	.long	532307062
	.long	1067137790
	.long	431106849
	.long	1064162173
	.long	329904022
	.long	1059572972
	.long	4080160942
	.long	1053550275
	.long	1402271583
	.long	1044493446
	.long	467296040
	.long	1032344560
	.long	26734424
	.long	1014207804
	.long	0
	.long	0
	.long	1071622681
	.long	3153114090
	.long	3948133409
	.long	3217705130
	.long	4096956694
	.long	3218184481
	.long	2616164369
	.long	3218465008
	.long	2620770434
	.long	3218641845
	.long	3075958072
	.long	3218520700
	.long	1950184789
	.long	3217978001
	.long	2686385024
	.long	3216731525
	.long	1489441818
	.long	3214597550
	.long	1992796697
	.long	3211642259
	.long	1952936976
	.long	3207056478
	.long	3904162951
	.long	3201033920
	.long	1382745445
	.long	3191977094
	.long	468673444
	.long	3179828208
	.long	3843669553
	.long	3161691483
	.long	0
	.long	0
	.long	1431655765
	.long	3218429269
	.long	3264395793
	.long	3218178991
	.long	60595194
	.long	3217824331
	.long	1837960166
	.long	3217124894
	.long	1964918946
	.long	3214033516
	.long	2998587684
	.long	1068822866
	.long	3421467326
	.long	1069267520
	.long	952003133
	.long	1068506016
	.long	1215426504
	.long	1066495476
	.long	2650572912
	.long	1063503494
	.long	4178958376
	.long	1059043758
	.long	3799805457
	.long	1052940753
	.long	3704238924
	.long	1043831645
	.long	2076585114
	.long	1031813109
	.long	1352847351
	.long	1013591371
	.long	0
	.long	0
	.long	3830371894
	.long	3169224254
	.long	74893150
	.long	1069567437
	.long	1235750664
	.long	1069825332
	.long	916043751
	.long	1069924975
	.long	4138411315
	.long	1069655686
	.long	829220656
	.long	1068441660
	.long	663246204
	.long	3213477792
	.long	652268865
	.long	3214415230
	.long	3251098232
	.long	3212868437
	.long	1480792335
	.long	3209929101
	.long	3365608027
	.long	3205478075
	.long	3330582883
	.long	3199375817
	.long	3573234945
	.long	3190266717
	.long	2061970086
	.long	3178248181
	.long	3148320390
	.long	3160026274
	.long	0
	.long	0
	.long	286337717
	.long	1069617425
	.long	4261152941
	.long	1068876190
	.long	3694459820
	.long	1067928728
	.long	2399079031
	.long	3212381546
	.long	394897286
	.long	3215735810
	.long	3073534041
	.long	3215860118
	.long	301960234
	.long	3214124960
	.long	3353887502
	.long	1064191753
	.long	4200665425
	.long	1063741482
	.long	2003926207
	.long	1061090030
	.long	2196865207
	.long	1056528964
	.long	2107880963
	.long	1050526402
	.long	3656860478
	.long	1041425071
	.long	881228218
	.long	1029305120
	.long	2612840768
	.long	1011136029
	.long	0
	.long	0
	.long	3722294196
	.long	3181505049
	.long	2573001951
	.long	3216029919
	.long	4095639908
	.long	3216151900
	.long	3272210374
	.long	3215957253
	.long	3439280785
	.long	3214441767
	.long	628273304
	.long	1066085542
	.long	3354767370
	.long	1066343670
	.long	300170709
	.long	1063270296
	.long	1870156670
	.long	3209139074
	.long	1580606479
	.long	3206808937
	.long	3818710870
	.long	3202408589
	.long	3126817102
	.long	3196311305
	.long	3320734688
	.long	3187224127
	.long	821170446
	.long	3175173312
	.long	3906480775
	.long	3156975650
	.long	0
	.long	0
	.long	428888587
	.long	3215696314
	.long	3125999356
	.long	3214336891
	.long	407196569
	.long	1053816799
	.long	886258254
	.long	1066874408
	.long	2149075781
	.long	1067351939
	.long	3888390356
	.long	1065784643
	.long	1226056234
	.long	3211994813
	.long	178100474
	.long	3210723675
	.long	4201249718
	.long	1058328572
	.long	3854015760
	.long	1057324616
	.long	3621220964
	.long	1052982118
	.long	1468766992
	.long	1046916174
	.long	2369608770
	.long	1037879115
	.long	327127732
	.long	1025754505
	.long	747046817
	.long	1007714190
	.long	0
	.long	0
	.long	4036362527
	.long	3191151783
	.long	839661649
	.long	1067363059
	.long	4269154241
	.long	1067251747
	.long	391446303
	.long	1066410535
	.long	2090623151
	.long	3211993063
	.long	1751510141
	.long	3213141508
	.long	4072216875
	.long	3209470961
	.long	3769618983
	.long	1061976030
	.long	3462945146
	.long	1057228123
	.long	3030849095
	.long	3202531084
	.long	4269010901
	.long	3198361258
	.long	742615277
	.long	3192302512
	.long	3397417437
	.long	3183265609
	.long	943110610
	.long	3171141000
	.long	371608300
	.long	3153099348
	.long	0
	.long	0
	.long	1315619150
	.long	1066820857
	.long	1001273821
	.long	3214201652
	.long	3859675203
	.long	3212560200
	.long	725858949
	.long	3213658423
	.long	2464052346
	.long	3212913056
	.long	1297319750
	.long	1063307355
	.long	563735576
	.long	1062988089
	.long	2756222736
	.long	3207203944
	.long	31207338
	.long	3204466214
	.long	434022900
	.long	1052227234
	.long	2370591882
	.long	1048628172
	.long	588930601
	.long	1042556347
	.long	3198977634
	.long	1033474724
	.long	1590950759
	.long	1021415866
	.long	195904708
	.long	1003000389
	.long	0
	.long	0
	.long	4252521214
	.long	3198731457
	.long	2969857811
	.long	3217870358
	.long	575387574
	.long	3212943727
	.long	67550217
	.long	3210922992
	.long	2955736731
	.long	1064678043
	.long	4193848343
	.long	1063288304
	.long	1845975253
	.long	3209397546
	.long	1025213509
	.long	3204479174
	.long	3713384058
	.long	1055658730
	.long	4120057883
	.long	3194988032
	.long	1573797757
	.long	3193584787
	.long	2514726550
	.long	3187678317
	.long	15343571
	.long	3178526042
	.long	104576940
	.long	3166444652
	.long	1147207168
	.long	3148070554
	.long	0
	.long	0
	.long	1467656669
	.long	3212977156
	.long	4154993315
	.long	1077775111
	.long	2465966858
	.long	3214886059
	.long	71777642
	.long	1068690118
	.long	2419763912
	.long	1061550205
	.long	1896047360
	.long	3210612806
	.long	3723555648
	.long	1058651288
	.long	3163703016
	.long	1057833732
	.long	1601936705
	.long	3201383489
	.long	2535509424
	.long	3195153293
	.long	3610885824
	.long	1043474022
	.long	1031698712
	.long	1037527637
	.long	1497459257
	.long	1028514042
	.long	3476455860
	.long	1016366870
	.long	758110873
	.long	998719391
	.long	0
	.long	0
	.long	572446067
	.long	3204307354
	.long	926268084
	.long	1081104698
	.long	1313112926
	.long	3217861477
	.long	3660716
	.long	1070677720
	.long	124568711
	.long	3210757561
	.long	2123022704
	.long	1059096046
	.long	576783408
	.long	1059279430
	.long	1651052980
	.long	3204387494
	.long	3164866735
	.long	1051430920
	.long	409335328
	.long	1046695415
	.long	3481520755
	.long	3188046619
	.long	1140549474
	.long	3182373569
	.long	708689751
	.long	3173247717
	.long	2627769694
	.long	3161153086
	.long	3804346926
	.long	3143551592
	.long	0
	.long	0
	.long	2302818369
	.long	1064188902
	.long	526101185
	.long	3235013457
	.long	2975776348
	.long	1075224435
	.long	1103981749
	.long	3223699933
	.long	4261798097
	.long	3210280329
	.long	30781306
	.long	1064564655
	.long	3939597931
	.long	3206430909
	.long	1816466405
	.long	1055007949
	.long	3868125859
	.long	3190076997
	.long	4218600579
	.long	3192569835
	.long	4167655123
	.long	1037376568
	.long	952533803
	.long	1032000428
	.long	895641221
	.long	1022851193
	.long	1237761065
	.long	1010835452
	.long	2902086315
	.long	3133082401
	.long	0
	.long	0
	.long	1899646778
	.long	3207205638
	.long	2434183270
	.long	3238288976
	.long	621380814
	.long	1078065849
	.long	247717525
	.long	3225783561
	.long	1611742563
	.long	3212088477
	.long	537725662
	.long	1065131990
	.long	3769436831
	.long	1057148224
	.long	3759797009
	.long	3196422840
	.long	842759416
	.long	3195613094
	.long	1736926210
	.long	1043198029
	.long	3915271468
	.long	3180709675
	.long	807416070
	.long	3176507548
	.long	3147759461
	.long	3167409843
	.long	3443382404
	.long	3155325020
	.long	1202615797
	.long	3129870924
	.long	0
	.long	0
	.long	1841653873
	.long	3210074087
	.long	2157744327
	.long	1095928888
	.long	3038317314
	.long	3229013375
	.long	2291108570
	.long	1082519711
	.long	707775397
	.long	1067599411
	.long	445214669
	.long	3216153989
	.long	3815354898
	.long	1054410330
	.long	1285070896
	.long	3199787450
	.long	1722630166
	.long	1047526663
	.long	2672844635
	.long	3188483010
	.long	1805520457
	.long	3179260705
	.long	542550567
	.long	1026041526
	.long	392361251
	.long	1017320419
	.long	562647833
	.long	1005205418
	.long	4253488278
	.long	988137457
	.long	0
	.long	0
	.long	3077187303
	.long	1060497018
	.long	1652392454
	.long	1099206368
	.long	414484972
	.long	3231848150
	.long	130592591
	.long	1084602513
	.long	1951534810
	.long	1068932483
	.long	2677544726
	.long	3216895313
	.long	2338009969
	.long	3203411240
	.long	302629286
	.long	1051889816
	.long	1274412910
	.long	3193339538
	.long	3731558070
	.long	1038350327
	.long	3135499196
	.long	1030352152
	.long	1329461873
	.long	3170325324
	.long	4058709792
	.long	3161656179
	.long	2167788642
	.long	3149540607
	.long	1602064437
	.long	3132466971
	.long	0
	.long	0
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	2146959360
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	1069547520
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	7864320
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	3616958675
	.long	3172564458
	.long	1431547708
	.long	3218429269
	.long	3390261318
	.long	3188010876
	.long	1446529494
	.long	1069617425
	.long	913571762
	.long	3199219810
	.long	1583612462
	.long	3215695720
	.long	2995724807
	.long	3207222498
	.long	847913742
	.long	1066913721
	.long	1634876930
	.long	3212167789
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1694159514
	.long	1010239653
	.long	966318664
	.long	1069606551
	.long	2992375944
	.long	1072656686
	.long	1870405289
	.long	3217070889
	.long	1411308967
	.long	3218333047
	.long	4096824853
	.long	1068863484
	.long	2220740425
	.long	1069365950
	.long	1401698298
	.long	3215430111
	.long	4137473768
	.long	3215259762
	.long	2666938667
	.long	1066889956
	.long	121190665
	.long	1066187784
	.long	821637913
	.long	3213226090
	.long	0
	.long	3217096704
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1463410348
	.long	1012925678
	.long	2389577058
	.long	1069735062
	.long	1617794783
	.long	1072647710
	.long	1889094329
	.long	3217191869
	.long	1210518828
	.long	3218309813
	.long	1479174953
	.long	1069010221
	.long	3435917531
	.long	1069290104
	.long	291210913
	.long	3215575029
	.long	464478606
	.long	3215159746
	.long	1063797118
	.long	1067014292
	.long	3489481042
	.long	1065955541
	.long	2916293494
	.long	3213319415
	.long	0
	.long	3217227776
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1308961311
	.long	1014934498
	.long	2204208241
	.long	1069862983
	.long	2945950899
	.long	1072637797
	.long	1107689125
	.long	3217310565
	.long	545938327
	.long	3218284334
	.long	3174275192
	.long	1069150773
	.long	3754729793
	.long	1069207728
	.long	1611554958
	.long	3215708601
	.long	2936527704
	.long	3215052478
	.long	2983784402
	.long	1067121823
	.long	1327150338
	.long	1065710404
	.long	3371320326
	.long	3213391099
	.long	0
	.long	3217358848
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3546392464
	.long	1015001497
	.long	631120375
	.long	1069990256
	.long	1201634405
	.long	1072626967
	.long	266657677
	.long	3217426771
	.long	1567732958
	.long	3218256710
	.long	883708059
	.long	1069284653
	.long	1008115966
	.long	1069119372
	.long	2657338981
	.long	3215830093
	.long	3402640736
	.long	3214939036
	.long	1000796573
	.long	1067211764
	.long	53805889
	.long	1065455799
	.long	1736607114
	.long	3213440608
	.long	0
	.long	3217489920
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2741128528
	.long	1013617020
	.long	3111451323
	.long	1070116823
	.long	1649040643
	.long	1072615239
	.long	3411009101
	.long	3217540290
	.long	3408666525
	.long	3218227049
	.long	60831764
	.long	1069411415
	.long	64016149
	.long	1069025616
	.long	1202785467
	.long	3215938891
	.long	1072151579
	.long	3214707060
	.long	1534357116
	.long	1067283570
	.long	4218468492
	.long	1065037194
	.long	2285827787
	.long	3213467810
	.long	0
	.long	3217620992
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2659584459
	.long	1014368295
	.long	3537749475
	.long	1070242630
	.long	3072983457
	.long	1072602635
	.long	3507245872
	.long	3217650938
	.long	3434758212
	.long	3218195466
	.long	3801643091
	.long	1069530660
	.long	1128653951
	.long	1068927067
	.long	3580298628
	.long	3216008547
	.long	1645082338
	.long	3214462237
	.long	1048857889
	.long	1067336943
	.long	21547694
	.long	1064510970
	.long	1433152914
	.long	3213472968
	.long	0
	.long	3217752064
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2718912981
	.long	1013914074
	.long	1823051285
	.long	1070367623
	.long	1714227149
	.long	1072589179
	.long	2128046799
	.long	3217758540
	.long	2655098722
	.long	3218162081
	.long	1690074008
	.long	1069594780
	.long	353091525
	.long	1068824353
	.long	4206393496
	.long	3216049578
	.long	824478721
	.long	3214211899
	.long	3850924188
	.long	1067371825
	.long	2738209029
	.long	1063668369
	.long	853664366
	.long	3213456718
	.long	0
	.long	3217883136
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2312638749
	.long	1013630664
	.long	4267025360
	.long	1070491748
	.long	3776362539
	.long	1072574894
	.long	3063840907
	.long	3217862932
	.long	2436606365
	.long	3218127019
	.long	582931594
	.long	1069646387
	.long	3079837843
	.long	1068718114
	.long	3430470362
	.long	3216083715
	.long	1015897693
	.long	3213958348
	.long	765047087
	.long	1067388396
	.long	2337193368
	.long	1061824569
	.long	3002775972
	.long	3213420044
	.long	0
	.long	3218014208
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2001712669
	.long	1015665334
	.long	1330879460
	.long	1070636148
	.long	2956987714
	.long	1072551971
	.long	4281360332
	.long	3218013175
	.long	3304213057
	.long	3218063389
	.long	3261945160
	.long	1069715874
	.long	3866284424
	.long	1068553570
	.long	3423706630
	.long	3216121886
	.long	259493169
	.long	3213268437
	.long	4223676832
	.long	1067379852
	.long	2765317642
	.long	3210752240
	.long	2292494069
	.long	3213329490
	.long	0
	.long	3218145280
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1632443533
	.long	1015769771
	.long	3788472163
	.long	1070757367
	.long	2406795724
	.long	1072518757
	.long	1173083542
	.long	3218140352
	.long	3726086528
	.long	3217906251
	.long	1205028711
	.long	1069793280
	.long	2231197855
	.long	1068156878
	.long	2368637763
	.long	3216148628
	.long	2866127296
	.long	3211617797
	.long	2424606359
	.long	1067309831
	.long	2444940724
	.long	3212180962
	.long	3308128888
	.long	3213151909
	.long	0
	.long	3218276352
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	478834383
	.long	1014053288
	.long	1342399495
	.long	1070876422
	.long	2650660168
	.long	1072482726
	.long	976724127
	.long	3218226669
	.long	962417089
	.long	3217740546
	.long	1060150306
	.long	1069852926
	.long	411739190
	.long	1067700577
	.long	3846786712
	.long	3216148687
	.long	4007187252
	.long	1064073475
	.long	3455779574
	.long	1067180067
	.long	1865169557
	.long	3212900393
	.long	1200620699
	.long	3212923615
	.long	0
	.long	3218407424
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	267289397
	.long	1014452734
	.long	302318249
	.long	1070993143
	.long	1373990511
	.long	1072444121
	.long	1606419704
	.long	3218305061
	.long	3955669825
	.long	3217568496
	.long	2701083439
	.long	1069894809
	.long	3425188888
	.long	1067047616
	.long	2305426029
	.long	3216123827
	.long	1692531481
	.long	1065641523
	.long	232815703
	.long	1067000535
	.long	3949954748
	.long	3213214884
	.long	558890519
	.long	3212487521
	.long	0
	.long	3218538496
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1719941932
	.long	1006528498
	.long	368840470
	.long	1071107377
	.long	1825320027
	.long	1072403193
	.long	538136722
	.long	3218375283
	.long	1431312010
	.long	3217392305
	.long	2586725425
	.long	1069919291
	.long	2680871675
	.long	1065941593
	.long	4123661982
	.long	3216076488
	.long	4235496382
	.long	1066406926
	.long	2618960092
	.long	1066782660
	.long	2333865044
	.long	3213444845
	.long	1545458959
	.long	3211934181
	.long	0
	.long	3218669568
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	26286236
	.long	1013191219
	.long	990113957
	.long	1071218987
	.long	3284199501
	.long	1072360200
	.long	2981906127
	.long	3218437190
	.long	3154396333
	.long	3217214106
	.long	4182117656
	.long	1069927061
	.long	903677379
	.long	3207343530
	.long	384743261
	.long	3216009637
	.long	1679228359
	.long	1066734193
	.long	3407026595
	.long	1066538544
	.long	784962854
	.long	3213588186
	.long	1956733412
	.long	3210979700
	.long	0
	.long	3218800640
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1468217372
	.long	1015658399
	.long	2570814109
	.long	1071327852
	.long	532296332
	.long	1072315404
	.long	318213600
	.long	3218490738
	.long	3661105766
	.long	3217035931
	.long	4085840862
	.long	1069919095
	.long	2798312316
	.long	3213370099
	.long	1668326589
	.long	3215870599
	.long	3158013712
	.long	1066998409
	.long	673205579
	.long	1066158659
	.long	486665227
	.long	3213647762
	.long	3317145528
	.long	3208570948
	.long	0
	.long	3218931712
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2060955015
	.long	1014233667
	.long	2567098181
	.long	1071433868
	.long	1159081245
	.long	1072269064
	.long	1492598184
	.long	3218535971
	.long	1567055841
	.long	3216688180
	.long	2821222425
	.long	1069896605
	.long	3691290783
	.long	3214336992
	.long	343679101
	.long	3215679175
	.long	1878686296
	.long	1067197462
	.long	125933636
	.long	1065636281
	.long	421076939
	.long	3213630573
	.long	3748848474
	.long	1062499186
	.long	0
	.long	3219062784
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1860475216
	.long	1015320544
	.long	439764829
	.long	1071587361
	.long	3870821058
	.long	1072197223
	.long	1430736283
	.long	3218588540
	.long	1597812790
	.long	3216174065
	.long	2398544810
	.long	1069838732
	.long	961452807
	.long	3215095800
	.long	716310499
	.long	3215360049
	.long	2337792646
	.long	1067375770
	.long	3863538422
	.long	1064417477
	.long	2203480844
	.long	3213482785
	.long	2389621902
	.long	1063978354
	.long	0
	.long	3219193856
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	517838046
	.long	1015294339
	.long	2114713104
	.long	1071713012
	.long	2046328558
	.long	1072098392
	.long	29352448
	.long	3218631376
	.long	1533416325
	.long	3215079684
	.long	765247815
	.long	1069724759
	.long	24381189
	.long	3215564623
	.long	1213155449
	.long	3214886044
	.long	379420126
	.long	1067409218
	.long	3127061143
	.long	3210809777
	.long	3193663073
	.long	3213128287
	.long	2557278876
	.long	1064581282
	.long	0
	.long	3219324928
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	201012485
	.long	1013704982
	.long	4147262407
	.long	1071803766
	.long	3048814903
	.long	1071997795
	.long	1260857726
	.long	3218645540
	.long	270462819
	.long	3209873967
	.long	874660781
	.long	1069580732
	.long	1251156804
	.long	3215866075
	.long	3568210118
	.long	3214014484
	.long	3784557811
	.long	1067255146
	.long	47772576
	.long	3212562613
	.long	2075700783
	.long	3212539455
	.long	1121561449
	.long	1064698735
	.long	0
	.long	3219456000
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	72370286
	.long	1015768239
	.long	2345366573
	.long	1071888223
	.long	3155310239
	.long	1071897123
	.long	4105462806
	.long	3218634383
	.long	2329529114
	.long	1067280331
	.long	3078782452
	.long	1069291148
	.long	2210998062
	.long	3215997483
	.long	1498585052
	.long	3212353515
	.long	3032692199
	.long	1066974465
	.long	809329973
	.long	3213081308
	.long	2713838579
	.long	3211547879
	.long	1266611175
	.long	1064568889
	.long	0
	.long	3219587072
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2137918782
	.long	1013598293
	.long	2511343836
	.long	1071966424
	.long	4205808243
	.long	1071797842
	.long	2776384587
	.long	3218601667
	.long	3824787134
	.long	1068202086
	.long	4101819712
	.long	1068956189
	.long	3547601806
	.long	3216004360
	.long	4156237724
	.long	1064058621
	.long	3714924071
	.long	1066627770
	.long	2925917146
	.long	3213234133
	.long	4211598888
	.long	3209111151
	.long	2569808389
	.long	1064277859
	.long	0
	.long	3219718144
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2979324843
	.long	1013692066
	.long	2595126306
	.long	1072038496
	.long	4263058559
	.long	1071701178
	.long	2217257467
	.long	3218551298
	.long	2310932059
	.long	1068685603
	.long	3368327571
	.long	1068627625
	.long	3037419246
	.long	3215935424
	.long	3509936675
	.long	1065724141
	.long	1557247226
	.long	1066131548
	.long	3830787958
	.long	3213205743
	.long	1781883284
	.long	1062575914
	.long	3918078093
	.long	1063614197
	.long	0
	.long	3219849216
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3630992244
	.long	1015034507
	.long	461360001
	.long	1072104635
	.long	2866201612
	.long	1071571556
	.long	1021729265
	.long	3218487113
	.long	1039036234
	.long	1068940858
	.long	1685105679
	.long	1068140011
	.long	1856275853
	.long	3215769620
	.long	2211306181
	.long	1066373046
	.long	3739405201
	.long	1065456917
	.long	3870269089
	.long	3213053509
	.long	427599213
	.long	1063593231
	.long	40698732
	.long	1062709094
	.long	0
	.long	3219980288
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4017840557
	.long	1014067070
	.long	2764932206
	.long	1072165089
	.long	3362970633
	.long	1071394124
	.long	2677206355
	.long	3218412713
	.long	1827861303
	.long	1069122666
	.long	2476388705
	.long	1067583638
	.long	523365901
	.long	3215556224
	.long	1203249285
	.long	1066574111
	.long	4264074292
	.long	1064402288
	.long	3556167213
	.long	3212827889
	.long	3894081206
	.long	1063908871
	.long	2161178761
	.long	1061130844
	.long	0
	.long	3220111360
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3622330478
	.long	1016273425
	.long	923638641
	.long	1072245755
	.long	239419665
	.long	1071146518
	.long	4085259706
	.long	3218288969
	.long	1284806809
	.long	1069276013
	.long	2806747971
	.long	1066232498
	.long	75259250
	.long	3215197393
	.long	2597116185
	.long	1066648701
	.long	1680670491
	.long	3208755029
	.long	446818184
	.long	3212096816
	.long	1529495144
	.long	1063888972
	.long	808619025
	.long	3208443000
	.long	0
	.long	3220242432
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	462410646
	.long	1015565639
	.long	3157363066
	.long	1072336316
	.long	87541994
	.long	1070853747
	.long	2905067058
	.long	3218115077
	.long	1081050294
	.long	1069306453
	.long	4130581086
	.long	3212259234
	.long	1279737796
	.long	3214531982
	.long	901138781
	.long	1066501065
	.long	978916480
	.long	3211806490
	.long	1307294116
	.long	3210755549
	.long	1154728319
	.long	1063409950
	.long	983243444
	.long	3209435485
	.long	0
	.long	3220373504
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2617212772
	.long	1016126748
	.long	2762378177
	.long	1072409936
	.long	1926160805
	.long	1070604218
	.long	4131898582
	.long	3217810482
	.long	3068505203
	.long	1069203346
	.long	2854543895
	.long	3214027139
	.long	1276437050
	.long	3213652513
	.long	523800203
	.long	1066060621
	.long	3030576699
	.long	3212054264
	.long	210618624
	.long	3205409267
	.long	3515290542
	.long	1062456384
	.long	1613351841
	.long	3209185464
	.long	0
	.long	3220504576
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2178033281
	.long	1016051223
	.long	859883711
	.long	1072469258
	.long	4248327203
	.long	1070195167
	.long	4170103331
	.long	3217497647
	.long	3497702842
	.long	1069026027
	.long	669705965
	.long	3214426190
	.long	548733038
	.long	3212258725
	.long	1756337187
	.long	1065503890
	.long	1830841059
	.long	3211930343
	.long	1445563742
	.long	1061912703
	.long	2113494314
	.long	1060991234
	.long	1734825467
	.long	3208559895
	.long	0
	.long	3220635648
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1674478116
	.long	1016412476
	.long	1178764976
	.long	1072516719
	.long	1119346206
	.long	1069851736
	.long	1526584272
	.long	3217221512
	.long	3575463915
	.long	1068817773
	.long	2913683612
	.long	3214542291
	.long	1135909212
	.long	3207879094
	.long	1952394810
	.long	1064725296
	.long	508910559
	.long	3211537545
	.long	225204077
	.long	1062311155
	.long	1009857186
	.long	1056234420
	.long	2872841632
	.long	3207480811
	.long	0
	.long	3220766720
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1768234410
	.long	1014305046
	.long	1496797583
	.long	1072554475
	.long	3351833521
	.long	1069572393
	.long	68183265
	.long	3216938851
	.long	4178655528
	.long	1068606905
	.long	60791550
	.long	3214483781
	.long	1856281737
	.long	1063701265
	.long	4260560897
	.long	1063778674
	.long	2539586291
	.long	3210979253
	.long	2272785608
	.long	1062198907
	.long	1986161572
	.long	3206910344
	.long	1016667904
	.long	3205797138
	.long	0
	.long	3220897792
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3074583847
	.long	1017063845
	.long	4092570620
	.long	1072584374
	.long	3645618684
	.long	1069147119
	.long	1980755111
	.long	3216542681
	.long	889928399
	.long	1068320928
	.long	1360064809
	.long	3214330986
	.long	2266432388
	.long	1064407878
	.long	4147854841
	.long	1062471610
	.long	1812350685
	.long	3210287970
	.long	3710399832
	.long	1061728481
	.long	2458127659
	.long	3207279138
	.long	287360833
	.long	3197756422
	.long	0
	.long	3221028864
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	68970445
	.long	1013643458
	.long	3990219923
	.long	1072607967
	.long	97498680
	.long	1068787106
	.long	4136450559
	.long	3216216395
	.long	147179316
	.long	1067971098
	.long	1625987424
	.long	3214138005
	.long	3965878798
	.long	1064539455
	.long	3777445436
	.long	1059539413
	.long	3029913178
	.long	3209512624
	.long	2162291908
	.long	1061245910
	.long	351053474
	.long	3207087984
	.long	3766283083
	.long	1056943188
	.long	0
	.long	3221159936
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3139580402
	.long	1014663922
	.long	3748810696
	.long	1072634260
	.long	3154943320
	.long	1068262833
	.long	3181856712
	.long	3215694135
	.long	3656356636
	.long	1067539266
	.long	3897588284
	.long	3213798616
	.long	1461831298
	.long	1064461217
	.long	2900114226
	.long	3208814642
	.long	2606420357
	.long	3207868903
	.long	1741152094
	.long	1060222230
	.long	1469206701
	.long	3206514441
	.long	3518993813
	.long	1057090958
	.long	0
	.long	3221291008
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3285908565
	.long	1015477239
	.long	1797956315
	.long	1072657271
	.long	3302471936
	.long	1067543167
	.long	2151339553
	.long	3215007235
	.long	362228095
	.long	1066797401
	.long	3986406156
	.long	3213131380
	.long	388353381
	.long	1064042359
	.long	4147910906
	.long	3209239839
	.long	1739922885
	.long	1056259812
	.long	3188561056
	.long	1058406709
	.long	489122368
	.long	3205182155
	.long	202560853
	.long	1056234231
	.long	0
	.long	3221422080
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3806705628
	.long	1008327668
	.long	884432573
	.long	1072671353
	.long	137405484
	.long	1066747168
	.long	3531994812
	.long	3214216262
	.long	3217445183
	.long	1066105333
	.long	2910288024
	.long	3212464301
	.long	3196212707
	.long	1063467545
	.long	3156563895
	.long	3208963593
	.long	3591285453
	.long	1058733242
	.long	2889132271
	.long	1055392886
	.long	1038377961
	.long	3203561698
	.long	4084672077
	.long	1055001082
	.long	0
	.long	3221553152
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1881957148
	.long	1016569186
	.long	3451706443
	.long	1072679940
	.long	143468186
	.long	1066002557
	.long	553724800
	.long	3213475431
	.long	1049442771
	.long	1065415105
	.long	3378521943
	.long	3211821787
	.long	3176008209
	.long	1062800361
	.long	4016898691
	.long	3208498219
	.long	1548390021
	.long	1058670598
	.long	2097418483
	.long	3202689041
	.long	2756703589
	.long	3201351283
	.long	506736184
	.long	1053405377
	.long	0
	.long	3221684224
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	450339788
	.long	1015040915
	.long	2959639058
	.long	1072685166
	.long	2289443405
	.long	1065320893
	.long	3331959139
	.long	3212796584
	.long	724199976
	.long	1064616734
	.long	938566183
	.long	3211030741
	.long	1640535667
	.long	1062186735
	.long	187996035
	.long	3207841256
	.long	822311531
	.long	1058246461
	.long	160890851
	.long	3203087480
	.long	3163291388
	.long	1050479733
	.long	578249940
	.long	1051474021
	.long	0
	.long	3221815296
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	539445753
	.long	1012635531
	.long	3085578536
	.long	1072688342
	.long	2019637246
	.long	1064510347
	.long	2901018414
	.long	3211991061
	.long	2171427566
	.long	1063868144
	.long	678185093
	.long	3210287638
	.long	2685165718
	.long	1061401571
	.long	710336199
	.long	3207152667
	.long	2733135798
	.long	1057659331
	.long	886948177
	.long	3202751664
	.long	3345834247
	.long	1052218043
	.long	908728048
	.long	1047925874
	.long	0
	.long	3221946368
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4240302093
	.long	1016867082
	.long	1832260410
	.long	1072690271
	.long	154153694
	.long	1063730412
	.long	2094548181
	.long	3211211898
	.long	1392727515
	.long	1063180837
	.long	3132890025
	.long	3209604411
	.long	483611698
	.long	1060651750
	.long	4246355421
	.long	3206519479
	.long	1424637421
	.long	1057044161
	.long	2138185318
	.long	3202290304
	.long	2276282642
	.long	1052095798
	.long	4227780935
	.long	3196067472
	.long	0
	.long	3222077440
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2485733495
	.long	1017000498
	.long	484092514
	.long	1072691442
	.long	1653085170
	.long	1063007344
	.long	2893019346
	.long	3210489400
	.long	878866243
	.long	1062388018
	.long	2113174452
	.long	3208818852
	.long	2654141437
	.long	1059959432
	.long	3578550869
	.long	3205727739
	.long	315005006
	.long	1056288680
	.long	3246956604
	.long	3201593495
	.long	2197286540
	.long	1051718329
	.long	3044885069
	.long	3196227269
	.long	0
	.long	3222208512
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2794994668
	.long	1016833037
	.long	3287420517
	.long	1072692394
	.long	4133778764
	.long	1061857404
	.long	689562148
	.long	3209340342
	.long	1404089106
	.long	1061273627
	.long	1292441425
	.long	3207706805
	.long	93671116
	.long	1058816787
	.long	2903327974
	.long	3204626398
	.long	4279279273
	.long	1055202414
	.long	134688023
	.long	3200552187
	.long	3315379764
	.long	1050761310
	.long	2945780649
	.long	3195568939
	.long	0
	.long	3222339584
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2135621136
	.long	1016053539
	.long	309961636
	.long	1072692934
	.long	672792810
	.long	1060347512
	.long	2960305506
	.long	3207830967
	.long	1703867620
	.long	1059726750
	.long	824905914
	.long	3206160796
	.long	3036017847
	.long	1057284422
	.long	923304464
	.long	3203122673
	.long	1848642304
	.long	1053791859
	.long	2215350763
	.long	3199158388
	.long	3049848127
	.long	1049324210
	.long	3861893815
	.long	3194293196
	.long	0
	.long	3222470656
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2236028675
	.long	1016891036
	.long	2177293363
	.long	1072693132
	.long	776830057
	.long	1058856794
	.long	4190004158
	.long	3206340337
	.long	209955488
	.long	1058225857
	.long	845130443
	.long	3204660651
	.long	4204313304
	.long	1055835544
	.long	364525198
	.long	3201597210
	.long	3889299905
	.long	1052205563
	.long	1514389355
	.long	3197586647
	.long	1706817756
	.long	1047834665
	.long	3817417318
	.long	3192934132
	.long	0
	.long	3222601728
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1074033072
	.long	1013200912
	.long	2197899301
	.long	1072693205
	.long	1097614282
	.long	1057308273
	.long	209489097
	.long	3204791893
	.long	3641526339
	.long	1056723664
	.long	1792794946
	.long	3203158586
	.long	584598707
	.long	1054254910
	.long	253996240
	.long	3200135633
	.long	642640562
	.long	1050754580
	.long	3147361740
	.long	3196139610
	.long	1167319222
	.long	1046395158
	.long	3488053038
	.long	3191370264
	.long	0
	.long	3222732800
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	871679747
	.long	1016222468
	.long	1586311569
	.long	1072693232
	.long	4199724405
	.long	1055867613
	.long	3041006250
	.long	3203351246
	.long	482130003
	.long	1055184672
	.long	1689676855
	.long	3201619703
	.long	116121201
	.long	1052814264
	.long	4166318198
	.long	3198564764
	.long	388552649
	.long	1049191609
	.long	1384400086
	.long	3194577312
	.long	135589376
	.long	1044819515
	.long	2497367318
	.long	3189906305
	.long	0
	.long	3222863872
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	752986014
	.long	1014740322
	.long	1072834312
	.long	1072693242
	.long	1389704451
	.long	1054277685
	.long	817998738
	.long	3201761329
	.long	996777029
	.long	1053731553
	.long	811547911
	.long	3200166603
	.long	1604093935
	.long	1051232383
	.long	2381858127
	.long	3197131472
	.long	806055999
	.long	1047703656
	.long	443662424
	.long	3193089938
	.long	2855612429
	.long	1043379518
	.long	3671581230
	.long	3188373004
	.long	0
	.long	3222994944
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4161630806
	.long	1015796140
	.long	3799386689
	.long	1072693245
	.long	516062805
	.long	1052830799
	.long	6188716
	.long	3200314446
	.long	79447568
	.long	1052151909
	.long	223529141
	.long	3198586975
	.long	1557009707
	.long	1049758991
	.long	1527834451
	.long	3195539792
	.long	3841571054
	.long	1046184222
	.long	3228035136
	.long	3191570603
	.long	2497745717
	.long	1041799395
	.long	3127975351
	.long	3186863029
	.long	0
	.long	3223126016
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3013228433
	.long	1015734301
	.long	952591976
	.long	1072693247
	.long	3849195912
	.long	1051256594
	.long	1248135057
	.long	3198740242
	.long	1658384276
	.long	1050712587
	.long	3312197895
	.long	3197147657
	.long	2426751598
	.long	1048219658
	.long	2075412918
	.long	3194074453
	.long	1194511818
	.long	1044659399
	.long	2861395540
	.long	3190045864
	.long	1105252788
	.long	1040325059
	.long	278204179
	.long	3185374362
	.long	0
	.long	3223257088
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4084683796
	.long	1014352664
	.long	3549182448
	.long	1072693247
	.long	4170486715
	.long	1048984034
	.long	3652359522
	.long	3196467682
	.long	1780445294
	.long	1048420995
	.long	3329441198
	.long	3194856066
	.long	663245309
	.long	1045935418
	.long	1918070306
	.long	3191839818
	.long	4225866973
	.long	1042419329
	.long	1974315224
	.long	3187805832
	.long	847480060
	.long	1038120500
	.long	2386310431
	.long	3183105031
	.long	0
	.long	3223388160
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	951119343
	.long	1016641415
	.long	4194036288
	.long	1072693247
	.long	4207053894
	.long	1045958742
	.long	4131013457
	.long	3193442390
	.long	2503178506
	.long	1045433060
	.long	2309798544
	.long	3191868132
	.long	1503762043
	.long	1042918157
	.long	762244907
	.long	3188792499
	.long	3745081608
	.long	1039371450
	.long	3106729171
	.long	3184757959
	.long	3799011378
	.long	1035063995
	.long	693003136
	.long	3180102041
	.long	0
	.long	3223519232
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1712896388
	.long	1016000193
	.long	4281307769
	.long	1072693247
	.long	3495080264
	.long	1042943408
	.long	3483806577
	.long	3190427056
	.long	3577360645
	.long	1042374261
	.long	3557467263
	.long	3188809333
	.long	3692227868
	.long	1039911516
	.long	1459944482
	.long	3185739496
	.long	624248087
	.long	1036331657
	.long	3930021706
	.long	3181718167
	.long	439009527
	.long	1032014849
	.long	3184212578
	.long	3177110789
	.long	0
	.long	3223650304
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1958475666
	.long	1013700788
	.long	4293118680
	.long	1072693247
	.long	3829159519
	.long	1039938855
	.long	3827364885
	.long	3187422503
	.long	2374004141
	.long	1039322650
	.long	2380228874
	.long	3185757722
	.long	853065064
	.long	1036916376
	.long	3897809499
	.long	3182694159
	.long	2467115425
	.long	1033300621
	.long	2966460473
	.long	3178687133
	.long	4249027489
	.long	1028973684
	.long	3391824522
	.long	3174085926
	.long	0
	.long	3223781376
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1028808661
	.long	1012328597
	.long	4294717113
	.long	1072693247
	.long	2759857858
	.long	1036945975
	.long	2759440340
	.long	3184429623
	.long	217750550
	.long	1036278821
	.long	228557927
	.long	3182713893
	.long	868996329
	.long	1033914811
	.long	130294465
	.long	3179657124
	.long	3979034581
	.long	1030279068
	.long	1690522291
	.long	3175665582
	.long	141102418
	.long	1025941166
	.long	3942643114
	.long	3171030731
	.long	0
	.long	3223912448
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2461075155
	.long	1015729939
	.long	4294933437
	.long	1072693247
	.long	2559161110
	.long	1033930834
	.long	2559046852
	.long	3181414482
	.long	3206412049
	.long	1033243416
	.long	3218709064
	.long	3179678488
	.long	2091270467
	.long	1030857342
	.long	245853585
	.long	3176629075
	.long	1555900931
	.long	1027267783
	.long	1186881303
	.long	3172654298
	.long	1695278520
	.long	1022918007
	.long	1853146834
	.long	3167983022
	.long	0
	.long	3224043520
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2502502488
	.long	1016572066
	.long	4294962713
	.long	1072693247
	.long	3137376149
	.long	1030874690
	.long	3137268820
	.long	3178358338
	.long	1097103169
	.long	1030217134
	.long	1110504267
	.long	3176652206
	.long	1068377398
	.long	1027807171
	.long	222176953
	.long	3173610756
	.long	3440315131
	.long	1024267613
	.long	1199778592
	.long	3169654130
	.long	257981480
	.long	1019904983
	.long	1388437918
	.long	3164943417
	.long	0
	.long	3224174592
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3418697838
	.long	1016821878
	.long	4294966675
	.long	1072693247
	.long	3798207862
	.long	1027825953
	.long	3798094058
	.long	3175309601
	.long	3391459718
	.long	1027200727
	.long	3405981646
	.long	3173635799
	.long	3694208074
	.long	1024764900
	.long	2192272311
	.long	3170602971
	.long	1464408928
	.long	1021279479
	.long	2201370875
	.long	3166665997
	.long	4139632468
	.long	1016902930
	.long	2981161402
	.long	3161912586
	.long	0
	.long	3224305664
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3589768515
	.long	1013972501
	.long	4294967265
	.long	1072693247
	.long	2293865510
	.long	1023336450
	.long	2097549026
	.long	3170820098
	.long	2721138850
	.long	1022661962
	.long	1571631120
	.long	3169097035
	.long	3716649917
	.long	1020295299
	.long	3146231247
	.long	3166041588
	.long	84506245
	.long	1016656297
	.long	2231398946
	.long	3162043093
	.long	3305646943
	.long	1012441980
	.long	402214167
	.long	3157503609
	.long	0
	.long	3224436736
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2152156943
	.long	1016184605
	.long	4294967294
	.long	1072693247
	.long	1074684533
	.long	1018634353
	.long	1074437943
	.long	3166118001
	.long	967276073
	.long	1018090988
	.long	995296768
	.long	3164526060
	.long	4275132894
	.long	1015589675
	.long	304133116
	.long	3161485853
	.long	1232215992
	.long	1012058464
	.long	559363548
	.long	3157444977
	.long	1487618473
	.long	1007759094
	.long	2355811294
	.long	3152771929
	.long	3381626085
	.long	3224537056
	.long	0
	.long	1072693248
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
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
	.long	0
	.long	4294967295
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
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2147352576
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	2145386496
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	1069416448
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	7733248
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.long	535822336
	.type	__svml_dtanh_ha_data_internal,@object
	.size	__svml_dtanh_ha_data_internal,10816
	.align 8
_imldTanhHATab:
	.long	0
	.long	1072693248
	.long	0
	.long	3220176896
	.type	_imldTanhHATab,@object
	.size	_imldTanhHATab,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
