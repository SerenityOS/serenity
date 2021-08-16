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
	.file "svml_s_atan2.c"
	.text
..TXTST0:
.L_2__routine_start___svml_atan2f4_ha_l9_0:
# -- Begin  __svml_atan2f4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f4_ha_l9
# --- __svml_atan2f4_ha_l9(__m128, __m128)
__svml_atan2f4_ha_l9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #682.1
        .byte     15                                            #974.14
        .byte     30                                            #974.14
        .byte     250                                           #974.14
	.cfi_startproc
..___tag_value___svml_atan2f4_ha_l9.1:
..L2:
                                                          #682.1
        pushq     %rbp                                          #682.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #682.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #682.1
        subq      $256, %rsp                                    #682.1
        vmovaps   %xmm1, %xmm12                                 #682.1
        vmovups   192+__svml_satan2_ha_data_internal(%rip), %xmm11 #795.50
        xorl      %edx, %edx                                    #810.5
        vmovups   1728+__svml_satan2_ha_data_internal(%rip), %xmm6 #803.26
        vandps    %xmm11, %xmm12, %xmm10                        #796.14
        vmovups   1792+__svml_satan2_ha_data_internal(%rip), %xmm3 #804.26
        vpsubd    %xmm6, %xmm10, %xmm8                          #805.18
        vpcmpgtd  %xmm3, %xmm8, %xmm4                           #807.32
        vpcmpeqd  %xmm3, %xmm8, %xmm2                           #807.83
        vpor      %xmm2, %xmm4, %xmm14                          #807.18
        vandps    %xmm11, %xmm0, %xmm11                         #797.14
        vmulps    576+__svml_satan2_ha_data_internal(%rip), %xmm10, %xmm2 #819.16
        vpsubd    %xmm6, %xmm11, %xmm5                          #806.18
        vmulps    512+__svml_satan2_ha_data_internal(%rip), %xmm10, %xmm4 #818.16
        vmulps    448+__svml_satan2_ha_data_internal(%rip), %xmm10, %xmm8 #817.16
        vmulps    384+__svml_satan2_ha_data_internal(%rip), %xmm10, %xmm6 #816.16
        vpcmpgtd  %xmm3, %xmm5, %xmm13                          #808.32
        vpcmpeqd  %xmm3, %xmm5, %xmm15                          #808.83
        vxorps    %xmm10, %xmm12, %xmm7                         #799.14
        vxorps    %xmm11, %xmm0, %xmm9                          #800.14
        vmovups   %xmm7, 16(%rsp)                               #799.14[spill]
        vpor      %xmm15, %xmm13, %xmm7                         #808.18
        vmovups   %xmm9, 32(%rsp)                               #800.14[spill]
        vpor      %xmm7, %xmm14, %xmm9                          #809.19
        vpcmpgtd  %xmm2, %xmm11, %xmm7                          #828.22
        vpcmpgtd  %xmm11, %xmm4, %xmm5                          #827.22
        vpxor     1600+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm3 #833.22
        vpcmpgtd  %xmm11, %xmm6, %xmm1                          #825.22
        vmovdqu   %xmm9, 48(%rsp)                               #809.19[spill]
        vpandn    %xmm3, %xmm5, %xmm9                           #834.23
        vmovups   640+__svml_satan2_ha_data_internal(%rip), %xmm2 #841.47
        vandps    %xmm1, %xmm11, %xmm14                         #844.18
        vmovups   704+__svml_satan2_ha_data_internal(%rip), %xmm3 #842.47
        vmovaps   %xmm2, %xmm13                                 #845.18
        vmovaps   %xmm3, %xmm4                                  #847.18
        vfnmadd213ps %xmm11, %xmm10, %xmm13                     #845.18
        vfnmadd213ps %xmm11, %xmm10, %xmm4                      #847.18
        vmovups   %xmm0, (%rsp)                                 #682.1[spill]
        vpcmpgtd  %xmm11, %xmm8, %xmm0                          #826.22
        vpandn    %xmm0, %xmm1, %xmm8                           #830.23
        vpandn    %xmm5, %xmm0, %xmm6                           #832.23
        vxorps    128+__svml_satan2_ha_data_internal(%rip), %xmm10, %xmm5 #851.18
        vandps    %xmm8, %xmm13, %xmm15                         #846.18
        vandps    %xmm7, %xmm5, %xmm5                           #852.18
        vandps    %xmm6, %xmm4, %xmm13                          #848.18
        vorps     %xmm15, %xmm14, %xmm15                        #853.18
        vmovups   768+__svml_satan2_ha_data_internal(%rip), %xmm4 #849.18
        vmovaps   %xmm4, %xmm0                                  #849.18
        vfnmadd213ps %xmm11, %xmm10, %xmm0                      #849.18
        vfmadd213ps %xmm10, %xmm11, %xmm2                       #858.18
        vfmadd213ps %xmm10, %xmm11, %xmm3                       #860.18
        vfmadd213ps %xmm10, %xmm11, %xmm4                       #862.18
        vandps    %xmm9, %xmm0, %xmm0                           #850.18
        vorps     %xmm0, %xmm13, %xmm14                         #854.18
        vandps    %xmm7, %xmm11, %xmm0                          #864.18
        vorps     %xmm14, %xmm15, %xmm13                        #855.18
        vandps    %xmm1, %xmm10, %xmm14                         #857.18
        vandps    %xmm8, %xmm2, %xmm15                          #859.18
        vandps    %xmm6, %xmm3, %xmm1                           #861.18
        vandps    %xmm9, %xmm4, %xmm3                           #863.18
        vorps     %xmm5, %xmm13, %xmm5                          #856.17
        vorps     %xmm15, %xmm14, %xmm13                        #865.18
        vorps     %xmm3, %xmm1, %xmm15                          #866.18
        vorps     %xmm15, %xmm13, %xmm14                        #867.18
        vandps    832+__svml_satan2_ha_data_internal(%rip), %xmm8, %xmm1 #873.18
        vorps     %xmm0, %xmm14, %xmm13                         #868.17
        vandps    960+__svml_satan2_ha_data_internal(%rip), %xmm6, %xmm2 #874.18
        vorps     %xmm2, %xmm1, %xmm15                          #877.18
        vrcpps    %xmm13, %xmm2                                 #880.18
        vmovups   64+__svml_satan2_ha_data_internal(%rip), %xmm0 #881.25
        vfnmadd231ps %xmm2, %xmm13, %xmm0                       #881.25
        vandps    1088+__svml_satan2_ha_data_internal(%rip), %xmm9, %xmm4 #875.18
        vandps    1216+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm3 #876.18
        vorps     %xmm3, %xmm4, %xmm14                          #878.18
        vorps     %xmm14, %xmm15, %xmm3                         #879.17
        vmovaps   %xmm13, %xmm15                                #885.19
        vfmadd213ps %xmm0, %xmm0, %xmm0                         #882.25
        vandps    1024+__svml_satan2_ha_data_internal(%rip), %xmm6, %xmm6 #894.18
        vandps    1280+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm7 #896.18
        vfmadd213ps %xmm2, %xmm0, %xmm2                         #883.22
        vcmpltps  __svml_satan2_ha_data_internal(%rip), %xmm12, %xmm0 #904.20
        vmulps    %xmm2, %xmm5, %xmm1                           #884.19
        vfnmadd213ps %xmm5, %xmm1, %xmm15                       #885.19
        vfmadd213ps %xmm1, %xmm2, %xmm15                        #886.19
        vmovups   1408+__svml_satan2_ha_data_internal(%rip), %xmm1 #910.47
        vfnmadd213ps %xmm5, %xmm15, %xmm13                      #887.19
        vmulps    %xmm15, %xmm15, %xmm4                         #907.14
        vmulps    %xmm13, %xmm2, %xmm14                         #888.19
        vmulps    %xmm4, %xmm4, %xmm2                           #908.14
        vandps    896+__svml_satan2_ha_data_internal(%rip), %xmm8, %xmm5 #893.18
        vandps    1152+__svml_satan2_ha_data_internal(%rip), %xmm9, %xmm8 #895.18
        vorps     %xmm6, %xmm5, %xmm9                           #897.18
        vmovups   1344+__svml_satan2_ha_data_internal(%rip), %xmm6 #913.14
        vorps     %xmm7, %xmm8, %xmm5                           #898.18
        vfmadd213ps 1472+__svml_satan2_ha_data_internal(%rip), %xmm2, %xmm6 #913.14
        vorps     %xmm5, %xmm9, %xmm13                          #899.17
        vfmadd213ps 1536+__svml_satan2_ha_data_internal(%rip), %xmm1, %xmm2 #914.14
        vaddps    %xmm14, %xmm13, %xmm9                         #900.19
        vfmadd213ps %xmm2, %xmm4, %xmm6                         #915.13
        vandps    320+__svml_satan2_ha_data_internal(%rip), %xmm0, %xmm7 #906.20
        vmovups   16(%rsp), %xmm5                               #917.16[spill]
        vxorps    %xmm5, %xmm7, %xmm8                           #917.16
        vandps    256+__svml_satan2_ha_data_internal(%rip), %xmm0, %xmm13 #905.20
        vaddps    %xmm9, %xmm8, %xmm14                          #918.19
        vmulps    %xmm6, %xmm4, %xmm0                           #916.13
        vfmadd213ps %xmm14, %xmm15, %xmm0                       #919.14
        vaddps    %xmm0, %xmm15, %xmm15                         #920.14
        vaddps    %xmm15, %xmm3, %xmm3                          #921.18
        vorps     %xmm5, %xmm3, %xmm3                           #922.14
        vaddps    %xmm3, %xmm13, %xmm0                          #923.18
        vmovups   32(%rsp), %xmm3                               #924.14[spill]
        vorps     %xmm3, %xmm0, %xmm4                           #924.14
        vmovdqu   48(%rsp), %xmm0                               #811.58[spill]
        vmovmskps %xmm0, %eax                                   #811.58
        testl     %eax, %eax                                    #925.52
        jne       ..B1.12       # Prob 5%                       #925.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm3 xmm4 xmm5 xmm10 xmm11 xmm12
..B1.2:                         # Preds ..B1.12 ..B1.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #977.52
        jne       ..B1.4        # Prob 5%                       #977.52
                                # LOE rbx r12 r13 r14 r15 edx xmm4 xmm12
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #980.12
        movq      %rbp, %rsp                                    #980.12
        popq      %rbp                                          #980.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #980.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %xmm1                                 #977.236[spill]
        vmovups   %xmm1, 64(%rsp)                               #977.236
        vmovups   %xmm12, 128(%rsp)                             #977.307
        vmovups   %xmm4, 192(%rsp)                              #977.378
        je        ..B1.3        # Prob 95%                      #977.482
                                # LOE rbx r12 r13 r14 r15 edx xmm4
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #977.562
        movq      %r12, 8(%rsp)                                 #977.562[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #977.562
        movq      %r13, (%rsp)                                  #977.562[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #977.562
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #977.625
        jc        ..B1.11       # Prob 5%                       #977.625
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #977.578
        cmpl      $4, %r12d                                     #977.573
        jl        ..B1.8        # Prob 82%                      #977.573
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm4                              #977.794
        jmp       ..B1.3        # Prob 100%                     #977.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm4
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #977.654
        lea       128(%rsp,%r12,4), %rsi                        #977.654
        lea       192(%rsp,%r12,4), %rdx                        #977.654
..___tag_value___svml_atan2f4_ha_l9.27:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #977.654
..___tag_value___svml_atan2f4_ha_l9.28:
        jmp       ..B1.9        # Prob 100%                     #977.654
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %xmm6                                 #947.19[spill]
        vcmpordps %xmm12, %xmm12, %xmm7                         #946.19
        vcmpordps %xmm6, %xmm6, %xmm8                           #947.19
        vmovups   __svml_satan2_ha_data_internal(%rip), %xmm2   #951.20
        vandps    %xmm8, %xmm7, %xmm15                          #948.20
        vpcmpeqd  %xmm2, %xmm10, %xmm9                          #951.20
        vpcmpeqd  %xmm2, %xmm11, %xmm13                         #952.20
        vpor      %xmm13, %xmm9, %xmm14                         #953.22
        vpand     %xmm15, %xmm14, %xmm1                         #954.28
        vpandn    %xmm0, %xmm1, %xmm6                           #955.21
        vcmpltps  %xmm10, %xmm11, %xmm0                         #960.21
        vmovmskps %xmm6, %edx                                   #956.44
        blendvps  %xmm0, %xmm10, %xmm11                         #962.21
        vandnps   1856+__svml_satan2_ha_data_internal(%rip), %xmm0, %xmm10 #963.24
        vcmpeqps  %xmm2, %xmm11, %xmm0                          #964.17
        blendvps  %xmm0, %xmm2, %xmm10                          #965.16
        vmovaps   %xmm1, %xmm0                                  #974.14
        vorps     %xmm5, %xmm10, %xmm11                         #966.13
        vpcmpgtd  %xmm12, %xmm2, %xmm5                          #968.22
        vandps    1920+__svml_satan2_ha_data_internal(%rip), %xmm5, %xmm2 #970.18
        vaddps    %xmm2, %xmm11, %xmm5                          #971.17
        vorps     %xmm3, %xmm5, %xmm3                           #972.19
        blendvps  %xmm0, %xmm3, %xmm4                           #974.14
        jmp       ..B1.2        # Prob 100%                     #974.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm4 xmm12
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f4_ha_l9,@function
	.size	__svml_atan2f4_ha_l9,.-__svml_atan2f4_ha_l9
..LN__svml_atan2f4_ha_l9.0:
	.data
# -- End  __svml_atan2f4_ha_l9
	.text
.L_2__routine_start___svml_atan2f4_ha_e9_1:
# -- Begin  __svml_atan2f4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f4_ha_e9
# --- __svml_atan2f4_ha_e9(__m128, __m128)
__svml_atan2f4_ha_e9:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #985.1
        .byte     15                                            #1282.14
        .byte     30                                            #1282.14
        .byte     250                                           #1282.14
	.cfi_startproc
..___tag_value___svml_atan2f4_ha_e9.33:
..L34:
                                                         #985.1
        pushq     %rbp                                          #985.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #985.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #985.1
        subq      $256, %rsp                                    #985.1
        vmovaps   %xmm1, %xmm13                                 #985.1
        vmovups   192+__svml_satan2_ha_data_internal(%rip), %xmm12 #1098.50
        xorl      %edx, %edx                                    #1113.5
        vmovups   1728+__svml_satan2_ha_data_internal(%rip), %xmm8 #1106.30
        vandps    %xmm12, %xmm13, %xmm11                        #1099.14
        vmovups   1792+__svml_satan2_ha_data_internal(%rip), %xmm10 #1107.30
        vandps    %xmm12, %xmm0, %xmm12                         #1100.14
        vpsubd    %xmm8, %xmm11, %xmm5                          #1108.22
        vxorps    %xmm12, %xmm0, %xmm6                          #1103.14
        vmovups   %xmm0, (%rsp)                                 #985.1[spill]
        vpsubd    %xmm8, %xmm12, %xmm7                          #1109.22
        vmulps    384+__svml_satan2_ha_data_internal(%rip), %xmm11, %xmm8 #1119.16
        vpcmpgtd  %xmm10, %xmm5, %xmm4                          #1110.36
        vmulps    448+__svml_satan2_ha_data_internal(%rip), %xmm11, %xmm0 #1120.16
        vpcmpeqd  %xmm10, %xmm5, %xmm2                          #1110.95
        vpcmpgtd  %xmm10, %xmm7, %xmm3                          #1111.36
        vmulps    512+__svml_satan2_ha_data_internal(%rip), %xmm11, %xmm5 #1121.16
        vpcmpeqd  %xmm10, %xmm7, %xmm15                         #1111.95
        vxorps    %xmm11, %xmm13, %xmm9                         #1102.14
        vpor      %xmm2, %xmm4, %xmm14                          #1110.22
        vmovups   %xmm9, 16(%rsp)                               #1102.14[spill]
        vpor      %xmm15, %xmm3, %xmm9                          #1111.22
        vpcmpgtd  %xmm12, %xmm8, %xmm1                          #1128.26
        vpcmpgtd  %xmm12, %xmm0, %xmm2                          #1129.26
        vpcmpgtd  %xmm12, %xmm5, %xmm10                         #1130.26
        vpandn    %xmm2, %xmm1, %xmm8                           #1133.27
        vmovups   %xmm6, 32(%rsp)                               #1103.14[spill]
        vpor      %xmm9, %xmm14, %xmm6                          #1112.23
        vmovdqu   %xmm6, 48(%rsp)                               #1112.23[spill]
        vpandn    %xmm10, %xmm2, %xmm6                          #1135.27
        vmovups   640+__svml_satan2_ha_data_internal(%rip), %xmm2 #1144.47
        vandps    %xmm1, %xmm12, %xmm0                          #1147.18
        vmulps    576+__svml_satan2_ha_data_internal(%rip), %xmm11, %xmm4 #1122.16
        vmulps    %xmm2, %xmm11, %xmm5                          #1148.38
        vpcmpgtd  %xmm4, %xmm12, %xmm7                          #1131.26
        vsubps    %xmm5, %xmm12, %xmm4                          #1148.18
        vpxor     1600+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm3 #1136.26
        vandps    %xmm8, %xmm4, %xmm14                          #1149.18
        vpandn    %xmm3, %xmm10, %xmm9                          #1137.27
        vorps     %xmm14, %xmm0, %xmm14                         #1156.18
        vmovups   704+__svml_satan2_ha_data_internal(%rip), %xmm3 #1145.47
        vandps    %xmm1, %xmm11, %xmm0                          #1160.18
        vmulps    %xmm3, %xmm11, %xmm15                         #1150.38
        vmulps    %xmm2, %xmm12, %xmm1                          #1161.30
        vmulps    %xmm3, %xmm12, %xmm3                          #1163.30
        vsubps    %xmm15, %xmm12, %xmm5                         #1150.18
        vaddps    %xmm1, %xmm11, %xmm2                          #1161.18
        vmovups   768+__svml_satan2_ha_data_internal(%rip), %xmm4 #1152.38
        vandps    %xmm6, %xmm5, %xmm15                          #1151.18
        vmulps    %xmm4, %xmm11, %xmm10                         #1152.38
        vandps    %xmm8, %xmm2, %xmm1                           #1162.18
        vaddps    %xmm3, %xmm11, %xmm2                          #1163.18
        vmulps    %xmm4, %xmm12, %xmm4                          #1165.30
        vsubps    %xmm10, %xmm12, %xmm5                         #1152.18
        vaddps    %xmm4, %xmm11, %xmm3                          #1165.18
        vandps    %xmm9, %xmm5, %xmm10                          #1153.18
        vandps    %xmm7, %xmm12, %xmm4                          #1167.18
        vxorps    128+__svml_satan2_ha_data_internal(%rip), %xmm11, %xmm5 #1154.18
        vorps     %xmm10, %xmm15, %xmm10                        #1157.18
        vandps    %xmm7, %xmm5, %xmm5                           #1155.18
        vorps     %xmm10, %xmm14, %xmm15                        #1158.18
        vorps     %xmm5, %xmm15, %xmm5                          #1159.17
        vandps    %xmm6, %xmm2, %xmm10                          #1164.18
        vandps    %xmm9, %xmm3, %xmm15                          #1166.18
        vorps     %xmm1, %xmm0, %xmm14                          #1168.18
        vorps     %xmm15, %xmm10, %xmm10                        #1169.18
        vorps     %xmm10, %xmm14, %xmm0                         #1170.18
        vandps    832+__svml_satan2_ha_data_internal(%rip), %xmm8, %xmm1 #1176.18
        vorps     %xmm4, %xmm0, %xmm14                          #1171.17
        vandps    960+__svml_satan2_ha_data_internal(%rip), %xmm6, %xmm4 #1177.18
        vandps    1088+__svml_satan2_ha_data_internal(%rip), %xmm9, %xmm2 #1178.18
        vorps     %xmm4, %xmm1, %xmm3                           #1180.18
        vandps    1216+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm10 #1179.18
        vorps     %xmm10, %xmm2, %xmm15                         #1181.18
        vorps     %xmm15, %xmm3, %xmm10                         #1182.17
        vcvtps2pd %xmm5, %ymm15                                 #1183.23
        vcvtps2pd %xmm14, %ymm2                                 #1184.23
        vandps    896+__svml_satan2_ha_data_internal(%rip), %xmm8, %xmm8 #1201.18
        vandps    1024+__svml_satan2_ha_data_internal(%rip), %xmm6, %xmm6 #1202.18
        vandps    1152+__svml_satan2_ha_data_internal(%rip), %xmm9, %xmm9 #1203.18
        vrcpps    %xmm14, %xmm5                                 #1185.22
        vmovupd   1664+__svml_satan2_ha_data_internal(%rip), %ymm4 #1187.57
        vandps    1280+__svml_satan2_ha_data_internal(%rip), %xmm7, %xmm7 #1204.18
        vcvtps2pd %xmm5, %ymm3                                  #1186.28
        vmulpd    %ymm3, %ymm2, %ymm5                           #1188.49
        vorps     %xmm7, %xmm9, %xmm2                           #1206.18
        vsubpd    %ymm5, %ymm4, %ymm0                           #1188.25
        vmulpd    %ymm0, %ymm3, %ymm14                          #1189.40
        vmulpd    %ymm0, %ymm0, %ymm1                           #1190.36
        vaddpd    %ymm14, %ymm3, %ymm5                          #1189.25
        vaddpd    %ymm1, %ymm4, %ymm4                           #1190.21
        vmulpd    %ymm4, %ymm5, %ymm5                           #1191.21
        vmulpd    %ymm5, %ymm15, %ymm15                         #1192.21
        vcvtpd2ps %ymm15, %xmm3                                 #1193.19
        vmulps    %xmm3, %xmm3, %xmm14                          #1215.14
        vcvtps2pd %xmm3, %ymm5                                  #1194.25
        vmulps    %xmm14, %xmm14, %xmm1                         #1216.14
        vsubpd    %ymm5, %ymm15, %ymm4                          #1195.19
        vmulps    1344+__svml_satan2_ha_data_internal(%rip), %xmm1, %xmm0 #1221.26
        vcvtpd2ps %ymm4, %xmm5                                  #1196.19
        vorps     %xmm6, %xmm8, %xmm4                           #1205.18
        vaddps    1472+__svml_satan2_ha_data_internal(%rip), %xmm0, %xmm8 #1221.14
        vmulps    1408+__svml_satan2_ha_data_internal(%rip), %xmm1, %xmm6 #1222.26
        vorps     %xmm2, %xmm4, %xmm7                           #1207.17
        vaddps    1536+__svml_satan2_ha_data_internal(%rip), %xmm6, %xmm9 #1222.14
        vmulps    %xmm8, %xmm14, %xmm4                          #1223.25
        vaddps    %xmm7, %xmm5, %xmm2                           #1208.19
        vmovups   __svml_satan2_ha_data_internal(%rip), %xmm5   #1209.46
        vaddps    %xmm4, %xmm9, %xmm0                           #1223.13
        vcmpltps  %xmm5, %xmm13, %xmm15                         #1212.20
        vmulps    %xmm0, %xmm14, %xmm0                          #1224.13
        vandps    256+__svml_satan2_ha_data_internal(%rip), %xmm15, %xmm7 #1213.20
        vandps    320+__svml_satan2_ha_data_internal(%rip), %xmm15, %xmm15 #1214.20
        vmovups   16(%rsp), %xmm4                               #1225.16[spill]
        vxorps    %xmm4, %xmm15, %xmm14                         #1225.16
        vmulps    %xmm0, %xmm3, %xmm1                           #1227.26
        vaddps    %xmm2, %xmm14, %xmm2                          #1226.19
        vaddps    %xmm1, %xmm2, %xmm6                           #1227.14
        vaddps    %xmm6, %xmm3, %xmm3                           #1228.14
        vaddps    %xmm3, %xmm10, %xmm10                         #1229.18
        vorps     %xmm4, %xmm10, %xmm2                          #1230.14
        vaddps    %xmm2, %xmm7, %xmm0                           #1231.18
        vmovups   32(%rsp), %xmm2                               #1232.14[spill]
        vorps     %xmm2, %xmm0, %xmm3                           #1232.14
        vmovdqu   48(%rsp), %xmm0                               #1114.58[spill]
        vmovmskps %xmm0, %eax                                   #1114.58
        testl     %eax, %eax                                    #1233.52
        jne       ..B2.12       # Prob 5%                       #1233.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2 xmm3 xmm4 xmm5 xmm11 xmm12 xmm13
..B2.2:                         # Preds ..B2.12 ..B2.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #1285.52
        jne       ..B2.4        # Prob 5%                       #1285.52
                                # LOE rbx r12 r13 r14 r15 edx xmm3 xmm13
..B2.15:                        # Preds ..B2.4 ..B2.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm3
..B2.3:                         # Preds ..B2.10 ..B2.15
                                # Execution count [1.00e+00]
        movaps    %xmm3, %xmm0                                  #1288.12
        movq      %rbp, %rsp                                    #1288.12
        popq      %rbp                                          #1288.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1288.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %xmm1                                 #1285.236[spill]
        vmovups   %xmm1, 64(%rsp)                               #1285.236
        vmovups   %xmm13, 128(%rsp)                             #1285.307
        vmovups   %xmm3, 192(%rsp)                              #1285.378
        je        ..B2.15       # Prob 95%                      #1285.482
                                # LOE rbx r12 r13 r14 r15 edx xmm3
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1285.562
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.17:                        # Preds ..B2.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.9 ..B2.17
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1285.625
        jc        ..B2.11       # Prob 5%                       #1285.625
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1285.578
        cmpl      $4, %r12d                                     #1285.573
        jl        ..B2.8        # Prob 82%                      #1285.573
                                # LOE rbx r12 r14 r15 r13d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm3                              #1285.794
        jmp       ..B2.3        # Prob 100%                     #1285.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm3
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1285.654
        lea       128(%rsp,%r12,4), %rsi                        #1285.654
        lea       192(%rsp,%r12,4), %rdx                        #1285.654
..___tag_value___svml_atan2f4_ha_e9.59:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #1285.654
..___tag_value___svml_atan2f4_ha_e9.60:
        jmp       ..B2.9        # Prob 100%                     #1285.654
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B2.12:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %xmm6                                 #1255.19[spill]
        vpcmpeqd  %xmm5, %xmm11, %xmm9                          #1259.24
        vcmpordps %xmm13, %xmm13, %xmm7                         #1254.19
        vcmpordps %xmm6, %xmm6, %xmm8                           #1255.19
        vpcmpeqd  %xmm5, %xmm12, %xmm10                         #1260.24
        vandps    %xmm8, %xmm7, %xmm15                          #1256.20
        vpor      %xmm10, %xmm9, %xmm14                         #1261.26
        vpand     %xmm15, %xmm14, %xmm1                         #1262.32
        vpandn    %xmm0, %xmm1, %xmm6                           #1263.25
        vcmpltps  %xmm11, %xmm12, %xmm0                         #1268.21
        vmovmskps %xmm6, %edx                                   #1264.44
        blendvps  %xmm0, %xmm11, %xmm12                         #1270.21
        vandnps   1856+__svml_satan2_ha_data_internal(%rip), %xmm0, %xmm11 #1271.24
        vcmpeqps  %xmm5, %xmm12, %xmm0                          #1272.17
        blendvps  %xmm0, %xmm5, %xmm11                          #1273.16
        vpcmpgtd  %xmm13, %xmm5, %xmm5                          #1276.26
        vandps    1920+__svml_satan2_ha_data_internal(%rip), %xmm5, %xmm12 #1278.18
        vorps     %xmm4, %xmm11, %xmm4                          #1274.13
        vmovaps   %xmm1, %xmm0                                  #1282.14
        vaddps    %xmm12, %xmm4, %xmm4                          #1279.17
        vorps     %xmm2, %xmm4, %xmm2                           #1280.19
        blendvps  %xmm0, %xmm2, %xmm3                           #1282.14
        jmp       ..B2.2        # Prob 100%                     #1282.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm3 xmm13
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f4_ha_e9,@function
	.size	__svml_atan2f4_ha_e9,.-__svml_atan2f4_ha_e9
..LN__svml_atan2f4_ha_e9.1:
	.data
# -- End  __svml_atan2f4_ha_e9
	.text
.L_2__routine_start___svml_atan2f16_ha_z0_2:
# -- Begin  __svml_atan2f16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f16_ha_z0
# --- __svml_atan2f16_ha_z0(__m512, __m512)
__svml_atan2f16_ha_z0:
# parameter 1: %zmm0
# parameter 2: %zmm1
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1293.1
        .byte     15                                            #1543.35
        .byte     30                                            #1543.35
        .byte     250                                           #1543.35
	.cfi_startproc
..___tag_value___svml_atan2f16_ha_z0.65:
..L66:
                                                         #1293.1
        pushq     %rbp                                          #1293.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1293.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1293.1
        subq      $256, %rsp                                    #1293.1
        xorl      %esi, %esi                                    #1424.5
        vmovups   192+__svml_satan2_ha_data_internal(%rip), %zmm2 #1409.52
        vmovups   1728+__svml_satan2_ha_data_internal(%rip), %zmm12 #1417.45
        vmovups   1792+__svml_satan2_ha_data_internal(%rip), %zmm14 #1418.45
        vmovups   448+__svml_satan2_ha_data_internal(%rip), %zmm6 #1427.50
        vmovups   576+__svml_satan2_ha_data_internal(%rip), %zmm10 #1429.50
        vmovups   384+__svml_satan2_ha_data_internal(%rip), %zmm7 #1426.50
        vmovups   512+__svml_satan2_ha_data_internal(%rip), %zmm9 #1428.50
        vandps    %zmm2, %zmm0, %zmm4                           #1411.14
        vpternlogd $255, %zmm8, %zmm8, %zmm8                    #1422.16
        vpsubd    %zmm12, %zmm4, %zmm15                         #1420.16
        vmovaps   %zmm1, %zmm5                                  #1293.1
        vandps    %zmm2, %zmm5, %zmm3                           #1410.14
        vxorps    %zmm4, %zmm0, %zmm1                           #1414.14
        vpcmpgtd  %zmm15, %zmm14, %k2                           #1422.83
        vmulps    {rn-sae}, %zmm7, %zmm3, %zmm11                #1430.18
        vmovups   768+__svml_satan2_ha_data_internal(%rip), %zmm7 #1441.49
        vpsubd    %zmm12, %zmm3, %zmm13                         #1419.16
        vpternlogd $255, %zmm12, %zmm12, %zmm12                 #1421.16
        vxorps    %zmm3, %zmm5, %zmm2                           #1413.14
        vpcmpgtd  %zmm13, %zmm14, %k1                           #1421.83
        vcmpps    $17, {sae}, %zmm4, %zmm11, %k6                #1434.21
        vmulps    {rn-sae}, %zmm9, %zmm3, %zmm14                #1432.18
        vmovups   64+__svml_satan2_ha_data_internal(%rip), %zmm11 #1438.47
        vcmpps    $17, {sae}, %zmm4, %zmm14, %k5                #1436.21
        vmovups   832+__svml_satan2_ha_data_internal(%rip), %zmm14 #1451.57
        vpandnd   %zmm13, %zmm13, %zmm12{%k1}                   #1421.16
        vpandnd   %zmm15, %zmm15, %zmm8{%k2}                    #1422.16
        vmulps    {rn-sae}, %zmm6, %zmm3, %zmm13                #1431.18
        vmulps    {rn-sae}, %zmm10, %zmm3, %zmm15               #1433.18
        vcmpps    $17, {sae}, %zmm4, %zmm13, %k3                #1435.21
        vcmpps    $17, {sae}, %zmm4, %zmm15, %k4                #1437.21
        vmovups   1088+__svml_satan2_ha_data_internal(%rip), %zmm15 #1453.57
        vblendmps %zmm11, %zmm7, %zmm9{%k4}                     #1443.23
        vblendmps 1216+__svml_satan2_ha_data_internal(%rip), %zmm15, %zmm7{%k4} #1456.18
        vmovups   1344+__svml_satan2_ha_data_internal(%rip), %zmm15 #1480.49
        vmovaps   %zmm3, %zmm10                                 #1449.17
        vxorps    %zmm3, %zmm3, %zmm10{%k4}                     #1449.17
        vpord     %zmm8, %zmm12, %zmm12                         #1423.17
        vmovups   640+__svml_satan2_ha_data_internal(%rip), %zmm8 #1439.49
        vptestmd  %zmm12, %zmm12, %k0                           #1425.47
        vblendmps 704+__svml_satan2_ha_data_internal(%rip), %zmm8, %zmm6{%k3} #1442.23
        vblendmps 960+__svml_satan2_ha_data_internal(%rip), %zmm14, %zmm8{%k3} #1455.18
        vmovups   __svml_satan2_ha_data_internal(%rip), %zmm14  #1474.48
        vblendmps %zmm9, %zmm6, %zmm13{%k5}                     #1444.20
        vblendmps %zmm7, %zmm8, %zmm8{%k5}                      #1457.17
        vcmpps    $17, {sae}, %zmm14, %zmm5, %k7                #1477.20
        vfmadd231ps {rn-sae}, %zmm4, %zmm13, %zmm10{%k6}        #1450.17
        kmovw     %k0, %eax                                     #1425.47
        vmovaps   %zmm4, %zmm9                                  #1446.17
        vxorps    %zmm4, %zmm4, %zmm9{%k4}                      #1446.17
        vfnmadd231ps {rn-sae}, %zmm3, %zmm13, %zmm9{%k6}        #1447.17
        vrcp14ps  %zmm10, %zmm13                                #1458.18
        vfnmadd231ps {rn-sae}, %zmm13, %zmm10, %zmm11           #1459.25
        vfmadd213ps {rn-sae}, %zmm13, %zmm11, %zmm13            #1460.22
        vmulps    {rn-sae}, %zmm9, %zmm13, %zmm6                #1461.21
        vmovaps   %zmm10, %zmm7                                 #1462.19
        vfnmadd213ps {rn-sae}, %zmm9, %zmm6, %zmm7              #1462.19
        vfmadd213ps {rn-sae}, %zmm6, %zmm13, %zmm7              #1463.19
        vfnmadd213ps {rn-sae}, %zmm9, %zmm7, %zmm10             #1464.19
        vmovups   1152+__svml_satan2_ha_data_internal(%rip), %zmm9 #1468.57
        vmulps    {rn-sae}, %zmm7, %zmm7, %zmm14                #1478.16
        vmulps    {rn-sae}, %zmm13, %zmm10, %zmm6               #1465.21
        vmovups   896+__svml_satan2_ha_data_internal(%rip), %zmm10 #1466.57
        vblendmps 1280+__svml_satan2_ha_data_internal(%rip), %zmm9, %zmm11{%k4} #1471.18
        vmovups   1408+__svml_satan2_ha_data_internal(%rip), %zmm9 #1481.49
        vblendmps 1024+__svml_satan2_ha_data_internal(%rip), %zmm10, %zmm10{%k3} #1470.18
        vblendmps %zmm11, %zmm10, %zmm13{%k5}                   #1472.17
        vmulps    {rn-sae}, %zmm14, %zmm14, %zmm11              #1479.16
        vmovups   1536+__svml_satan2_ha_data_internal(%rip), %zmm10 #1483.49
        vaddps    {rn-sae}, %zmm13, %zmm6, %zmm6{%k6}           #1473.21
        vmovups   1472+__svml_satan2_ha_data_internal(%rip), %zmm13 #1484.14
        vfmadd231ps {rn-sae}, %zmm11, %zmm15, %zmm13            #1484.14
        vfmadd213ps {rn-sae}, %zmm10, %zmm9, %zmm11             #1485.14
        vxorps    320+__svml_satan2_ha_data_internal(%rip), %zmm2, %zmm15 #1488.16
        vfmadd213ps {rn-sae}, %zmm11, %zmm14, %zmm13            #1486.13
        vaddps    {rn-sae}, %zmm15, %zmm6, %zmm6{%k7}           #1489.21
        vmulps    {rn-sae}, %zmm14, %zmm13, %zmm9               #1487.15
        vfmadd213ps {rn-sae}, %zmm6, %zmm7, %zmm9               #1490.14
        vaddps    {rn-sae}, %zmm7, %zmm9, %zmm6                 #1491.16
        vmovups   256+__svml_satan2_ha_data_internal(%rip), %zmm7 #1494.20
        vaddps    {rn-sae}, %zmm8, %zmm6, %zmm6{%k6}            #1492.20
        vorps     %zmm2, %zmm6, %zmm8                           #1493.14
        vaddps    {rn-sae}, %zmm7, %zmm8, %zmm8{%k7}            #1494.20
        vorps     %zmm1, %zmm8, %zmm9                           #1495.14
        testl     %eax, %eax                                    #1496.52
        jne       ..B3.17       # Prob 5%                       #1496.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm2 zmm3 zmm4 zmm5 zmm9 zmm12
..B3.2:                         # Preds ..B3.17 ..B3.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #1546.52
        jne       ..B3.4        # Prob 5%                       #1546.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm5 zmm9
..B3.3:                         # Preds ..B3.10 ..B3.8 ..B3.2
                                # Execution count [1.00e+00]
        vmovaps   %zmm9, %zmm0                                  #1549.12
        movq      %rbp, %rsp                                    #1549.12
        popq      %rbp                                          #1549.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1549.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1546.293
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm5 zmm9
..B3.5:                         # Preds ..B3.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #1546.293
        movl      %edx, %eax                                    #1546.347
        orl       $8064, %eax                                   #1546.347
        cmpl      %eax, %edx                                    #1546.376
        je        ..B3.7        # Prob 78%                      #1546.376
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm5 zmm9
..B3.6:                         # Preds ..B3.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #1546.388
        vldmxcsr  32(%rsp)                                      #1546.388
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm5 zmm9
..B3.7:                         # Preds ..B3.6 ..B3.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm0, 64(%rsp)                               #1546.448
        vmovups   %zmm5, 128(%rsp)                              #1546.522
        vmovups   %zmm9, 192(%rsp)                              #1546.596
        testl     %esi, %esi                                    #1546.704
        jne       ..B3.12       # Prob 5%                       #1546.704
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm9
..B3.8:                         # Preds ..B3.15 ..B3.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #1546.1064
        je        ..B3.3        # Prob 78%                      #1546.1064
                                # LOE rbx r12 r13 r14 r15 edx zmm9
..B3.9:                         # Preds ..B3.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #1546.1089
        movl      32(%rsp), %eax                                #1546.1089
                                # LOE rbx r12 r13 r14 r15 eax edx zmm9
..B3.10:                        # Preds ..B3.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #1546.1076
        orl       %edx, %eax                                    #1546.1076
        movl      %eax, 32(%rsp)                                #1546.1076
        vldmxcsr  32(%rsp)                                      #1546.1076
        jmp       ..B3.3        # Prob 100%                     #1546.1076
                                # LOE rbx r12 r13 r14 r15 zmm9
..B3.12:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #1546.786
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B3.20:                        # Preds ..B3.12
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
..B3.13:                        # Preds ..B3.14 ..B3.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #1546.849
        jc        ..B3.16       # Prob 5%                       #1546.849
                                # LOE rbx r15 r12d r13d r14d
..B3.14:                        # Preds ..B3.16 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #1546.802
        cmpl      $16, %ebx                                     #1546.797
        jl        ..B3.13       # Prob 82%                      #1546.797
                                # LOE rbx r15 r12d r13d r14d
..B3.15:                        # Preds ..B3.14
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
        vmovups   192(%rsp), %zmm9                              #1546.1019
        jmp       ..B3.8        # Prob 100%                     #1546.1019
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x18, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x10, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm9
..B3.16:                        # Preds ..B3.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #1546.878
        lea       128(%rsp,%rbx,4), %rsi                        #1546.878
        lea       192(%rsp,%rbx,4), %rdx                        #1546.878
..___tag_value___svml_atan2f16_ha_z0.93:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #1546.878
..___tag_value___svml_atan2f16_ha_z0.94:
        jmp       ..B3.14       # Prob 100%                     #1546.878
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B3.17:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   __svml_satan2_ha_data_internal(%rip), %zmm8   #1522.85
        vmovups   1920+__svml_satan2_ha_data_internal(%rip), %zmm6 #1528.50
        vcmpps    $17, {sae}, %zmm3, %zmm4, %k5                 #1531.21
        vcmpps    $3, {sae}, %zmm5, %zmm5, %k1                  #1517.108
        vcmpps    $3, {sae}, %zmm0, %zmm0, %k2                  #1518.108
        vpcmpd    $4, %zmm8, %zmm3, %k3                         #1522.85
        vpcmpd    $4, %zmm8, %zmm4, %k4                         #1523.85
        vpcmpgtd  %zmm5, %zmm8, %k7                             #1539.20
        vpternlogd $255, %zmm13, %zmm13, %zmm13                 #1522.18
        vpternlogd $255, %zmm10, %zmm10, %zmm10                 #1517.40
        vpternlogd $255, %zmm11, %zmm11, %zmm11                 #1518.40
        vpternlogd $255, %zmm14, %zmm14, %zmm14                 #1523.18
        vpandnd   %zmm3, %zmm3, %zmm13{%k3}                     #1522.18
        vblendmps %zmm3, %zmm4, %zmm3{%k5}                      #1533.21
        vcmpps    $4, {sae}, %zmm8, %zmm3, %k6                  #1535.106
        vpandnd   %zmm5, %zmm5, %zmm10{%k1}                     #1517.40
        vpandnd   %zmm0, %zmm0, %zmm11{%k2}                     #1518.40
        vandps    %zmm11, %zmm10, %zmm7                         #1519.20
        vmovups   1856+__svml_satan2_ha_data_internal(%rip), %zmm10 #1529.52
        vxorps    %zmm10, %zmm10, %zmm10{%k5}                   #1534.24
        vpandnd   %zmm4, %zmm4, %zmm14{%k4}                     #1523.18
        vpternlogd $255, %zmm4, %zmm4, %zmm4                    #1535.38
        vpandnd   %zmm3, %zmm3, %zmm4{%k6}                      #1535.38
        vpandnd   %zmm10, %zmm4, %zmm3                          #1536.54
        vpandd    %zmm4, %zmm8, %zmm4                           #1536.144
        vpord     %zmm4, %zmm3, %zmm10                          #1536.37
        vorps     %zmm2, %zmm10, %zmm2                          #1537.13
        vaddps    {rn-sae}, %zmm6, %zmm2, %zmm2{%k7}            #1540.19
        vorps     %zmm1, %zmm2, %zmm1                           #1541.19
        vpord     %zmm14, %zmm13, %zmm15                        #1524.20
        vpandd    %zmm7, %zmm15, %zmm7                          #1525.26
        vpandnd   %zmm12, %zmm7, %zmm12                         #1526.19
        vptestmd  %zmm12, %zmm12, %k0                           #1527.33
        kmovw     %k0, %esi                                     #1527.33
        vpandnd   %zmm9, %zmm7, %zmm9                           #1543.52
        vpandd    %zmm7, %zmm1, %zmm1                           #1543.149
        vpord     %zmm1, %zmm9, %zmm9                           #1543.35
        jmp       ..B3.2        # Prob 100%                     #1543.35
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm5 zmm9
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f16_ha_z0,@function
	.size	__svml_atan2f16_ha_z0,.-__svml_atan2f16_ha_z0
..LN__svml_atan2f16_ha_z0.2:
	.data
# -- End  __svml_atan2f16_ha_z0
	.text
.L_2__routine_start___svml_atan2f8_ha_e9_3:
# -- Begin  __svml_atan2f8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f8_ha_e9
# --- __svml_atan2f8_ha_e9(__m256, __m256)
__svml_atan2f8_ha_e9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1554.1
        .byte     15                                            #1851.14
        .byte     30                                            #1851.14
        .byte     250                                           #1851.14
	.cfi_startproc
..___tag_value___svml_atan2f8_ha_e9.100:
..L101:
                                                        #1554.1
        pushq     %rbp                                          #1554.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1554.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1554.1
        subq      $256, %rsp                                    #1554.1
        xorl      %edx, %edx                                    #1682.5
        vmovups   192+__svml_satan2_ha_data_internal(%rip), %ymm2 #1667.53
        vmovups   1728+__svml_satan2_ha_data_internal(%rip), %xmm11 #1675.30
        vmovups   %ymm0, (%rsp)                                 #1554.1[spill]
        vmovaps   %ymm1, %ymm4                                  #1554.1
        vandps    %ymm2, %ymm4, %ymm3                           #1668.14
        vandps    %ymm2, %ymm0, %ymm2                           #1669.14
        vxorps    %ymm3, %ymm4, %ymm14                          #1671.14
        vxorps    %ymm2, %ymm0, %ymm7                           #1672.14
        vmovups   1792+__svml_satan2_ha_data_internal(%rip), %xmm0 #1676.30
        vmovups   %ymm14, 64(%rsp)                              #1671.14[spill]
        vmovups   %ymm7, 32(%rsp)                               #1672.14[spill]
        vpsubd    %xmm11, %xmm3, %xmm9                          #1677.22
        vextractf128 $1, %ymm3, %xmm13                          #1673.107
        vpcmpgtd  %xmm0, %xmm9, %xmm5                           #1679.36
        vpcmpeqd  %xmm0, %xmm9, %xmm12                          #1679.95
        vpor      %xmm12, %xmm5, %xmm7                          #1679.22
        vpsubd    %xmm11, %xmm13, %xmm10                        #1677.94
        vpcmpgtd  %xmm0, %xmm10, %xmm9                          #1679.186
        vpcmpeqd  %xmm0, %xmm10, %xmm10                         #1679.245
        vpor      %xmm10, %xmm9, %xmm12                         #1679.172
        vmovups   %xmm13, 112(%rsp)                             #1673.107[spill]
        vmovups   %xmm3, 96(%rsp)                               #1673.38[spill]
        vpsubd    %xmm11, %xmm2, %xmm6                          #1678.22
        vextractf128 $1, %ymm2, %xmm8                           #1674.107
        vpcmpgtd  %xmm0, %xmm6, %xmm15                          #1680.36
        vpcmpeqd  %xmm0, %xmm6, %xmm14                          #1680.95
        vpor      %xmm14, %xmm15, %xmm13                        #1680.22
        vpsubd    %xmm11, %xmm8, %xmm1                          #1678.94
        vpor      %xmm13, %xmm7, %xmm11                         #1681.23
        vpcmpgtd  %xmm0, %xmm1, %xmm5                           #1680.186
        vpcmpeqd  %xmm0, %xmm1, %xmm0                           #1680.245
        vpor      %xmm0, %xmm5, %xmm1                           #1680.172
        vmulps    384+__svml_satan2_ha_data_internal(%rip), %ymm3, %ymm15 #1688.16
        vpor      %xmm1, %xmm12, %xmm6                          #1681.89
        vmulps    576+__svml_satan2_ha_data_internal(%rip), %ymm3, %ymm1 #1691.16
        vmovdqu   %xmm11, 160(%rsp)                             #1681.23[spill]
        vmovdqu   %xmm6, 128(%rsp)                              #1681.89[spill]
        vmovups   %xmm8, 144(%rsp)                              #1674.107[spill]
        vmulps    512+__svml_satan2_ha_data_internal(%rip), %ymm3, %ymm11 #1690.16
        vmulps    448+__svml_satan2_ha_data_internal(%rip), %ymm3, %ymm0 #1689.16
        vpcmpgtd  %xmm2, %xmm15, %xmm5                          #1697.26
        vextractf128 $1, %ymm15, %xmm10                         #1692.117
        vpcmpgtd  %xmm8, %xmm10, %xmm7                          #1697.92
        vpcmpgtd  %xmm1, %xmm2, %xmm6                           #1700.26
        vextractf128 $1, %ymm1, %xmm9                           #1695.117
        vpcmpgtd  %xmm2, %xmm0, %xmm10                          #1698.26
        vextractf128 $1, %ymm11, %xmm13                         #1694.117
        vpcmpgtd  %xmm2, %xmm11, %xmm11                         #1699.26
        vextractf128 $1, %ymm0, %xmm14                          #1693.117
        vpcmpgtd  %xmm9, %xmm8, %xmm0                           #1700.92
        vmovups   1600+__svml_satan2_ha_data_internal(%rip), %xmm9 #1705.26
        vpcmpgtd  %xmm8, %xmm13, %xmm15                         #1699.92
        vpxor     %xmm9, %xmm6, %xmm13                          #1705.26
        vpcmpgtd  %xmm8, %xmm14, %xmm14                         #1698.92
        vpandn    %xmm10, %xmm5, %xmm8                          #1702.27
        vinsertf128 $1, %xmm7, %ymm5, %ymm12                    #1707.21
        vpxor     %xmm9, %xmm0, %xmm5                           #1705.96
        vpandn    %xmm11, %xmm10, %xmm10                        #1704.27
        vpandn    %xmm13, %xmm11, %xmm11                        #1706.27
        vpandn    %xmm14, %xmm7, %xmm7                          #1702.97
        vpandn    %xmm15, %xmm14, %xmm14                        #1704.97
        vpandn    %xmm5, %xmm15, %xmm15                         #1706.97
        vinsertf128 $1, %xmm15, %ymm11, %ymm13                  #1710.21
        vmovups   640+__svml_satan2_ha_data_internal(%rip), %ymm15 #1713.50
        vinsertf128 $1, %xmm14, %ymm10, %ymm14                  #1709.21
        vmulps    %ymm15, %ymm3, %ymm10                         #1717.41
        vinsertf128 $1, %xmm7, %ymm8, %ymm1                     #1708.21
        vinsertf128 $1, %xmm0, %ymm6, %ymm11                    #1711.21
        vmovups   704+__svml_satan2_ha_data_internal(%rip), %ymm0 #1714.50
        vsubps    %ymm10, %ymm2, %ymm7                          #1717.18
        vmovups   768+__svml_satan2_ha_data_internal(%rip), %ymm10 #1721.41
        vmulps    %ymm0, %ymm3, %ymm8                           #1719.41
        vmulps    %ymm0, %ymm2, %ymm0                           #1732.33
        vsubps    %ymm8, %ymm2, %ymm9                           #1719.18
        vaddps    %ymm0, %ymm3, %ymm0                           #1732.18
        vandps    %ymm1, %ymm7, %ymm5                           #1718.18
        vmulps    %ymm10, %ymm3, %ymm7                          #1721.41
        vmulps    %ymm10, %ymm2, %ymm10                         #1734.33
        vsubps    %ymm7, %ymm2, %ymm8                           #1721.18
        vandps    %ymm12, %ymm2, %ymm6                          #1716.18
        vandps    %ymm14, %ymm9, %ymm9                          #1720.18
        vandps    %ymm13, %ymm8, %ymm7                          #1722.18
        vorps     %ymm5, %ymm6, %ymm5                           #1725.18
        vorps     %ymm7, %ymm9, %ymm6                           #1726.18
        vorps     %ymm6, %ymm5, %ymm7                           #1727.18
        vandps    %ymm12, %ymm3, %ymm6                          #1729.18
        vmulps    %ymm15, %ymm2, %ymm12                         #1730.33
        vxorps    128+__svml_satan2_ha_data_internal(%rip), %ymm3, %ymm8 #1723.18
        vandps    %ymm11, %ymm8, %ymm8                          #1724.18
        vorps     %ymm8, %ymm7, %ymm8                           #1728.17
        vaddps    %ymm12, %ymm3, %ymm7                          #1730.18
        vandps    %ymm1, %ymm7, %ymm12                          #1731.18
        vaddps    %ymm10, %ymm3, %ymm7                          #1734.18
        vandps    %ymm14, %ymm0, %ymm9                          #1733.18
        vandps    %ymm13, %ymm7, %ymm5                          #1735.18
        vorps     %ymm12, %ymm6, %ymm12                         #1737.18
        vorps     %ymm5, %ymm9, %ymm9                           #1738.18
        vandps    %ymm11, %ymm2, %ymm15                         #1736.18
        vorps     %ymm9, %ymm12, %ymm6                          #1739.18
        vorps     %ymm15, %ymm6, %ymm5                          #1740.17
        vandps    1088+__svml_satan2_ha_data_internal(%rip), %ymm13, %ymm7 #1747.18
        vandps    1216+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm9 #1748.18
        vorps     %ymm9, %ymm7, %ymm6                           #1750.18
        vandps    832+__svml_satan2_ha_data_internal(%rip), %ymm1, %ymm10 #1745.18
        vandps    960+__svml_satan2_ha_data_internal(%rip), %ymm14, %ymm0 #1746.18
        vorps     %ymm0, %ymm10, %ymm12                         #1749.18
        vorps     %ymm6, %ymm12, %ymm10                         #1751.17
        vandps    896+__svml_satan2_ha_data_internal(%rip), %ymm1, %ymm1 #1770.18
        vandps    1024+__svml_satan2_ha_data_internal(%rip), %ymm14, %ymm14 #1771.18
        vandps    1152+__svml_satan2_ha_data_internal(%rip), %ymm13, %ymm13 #1772.18
        vandps    1280+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm11 #1773.18
        vcvtps2pd %xmm5, %ymm7                                  #1753.23
        vextractf128 $1, %ymm5, %xmm15                          #1753.105
        vrcpps    %ymm5, %ymm5                                  #1754.22
        vcvtps2pd %xmm15, %ymm12                                #1753.88
        vextractf128 $1, %ymm8, %xmm0                           #1752.105
        vcvtps2pd %xmm8, %ymm9                                  #1752.23
        vcvtps2pd %xmm0, %ymm8                                  #1752.88
        vcvtps2pd %xmm5, %ymm0                                  #1755.28
        vextractf128 $1, %ymm5, %xmm6                           #1755.112
        vmovupd   1664+__svml_satan2_ha_data_internal(%rip), %ymm5 #1756.59
        vmulpd    %ymm0, %ymm7, %ymm7                           #1757.53
        vcvtps2pd %xmm6, %ymm6                                  #1755.95
        vsubpd    %ymm7, %ymm5, %ymm7                           #1757.27
        vmulpd    %ymm6, %ymm12, %ymm12                         #1757.130
        vsubpd    %ymm12, %ymm5, %ymm15                         #1757.104
        vmulpd    %ymm7, %ymm0, %ymm12                          #1758.42
        vmulpd    %ymm7, %ymm7, %ymm7                           #1759.38
        vaddpd    %ymm12, %ymm0, %ymm12                         #1758.27
        vmulpd    %ymm15, %ymm6, %ymm0                          #1758.123
        vmulpd    %ymm15, %ymm15, %ymm15                        #1759.119
        vaddpd    %ymm0, %ymm6, %ymm0                           #1758.108
        vaddpd    %ymm7, %ymm5, %ymm6                           #1759.23
        vaddpd    %ymm15, %ymm5, %ymm7                          #1759.104
        vmulpd    %ymm6, %ymm12, %ymm5                          #1760.23
        vmulpd    %ymm7, %ymm0, %ymm0                           #1760.72
        vmulpd    %ymm5, %ymm9, %ymm5                           #1761.23
        vmulpd    %ymm0, %ymm8, %ymm9                           #1761.68
        vcvtpd2ps %ymm5, %xmm8                                  #1762.65
        vcvtpd2ps %ymm9, %xmm0                                  #1762.99
        vcvtps2pd %xmm0, %ymm12                                 #1763.94
        vinsertf128 $1, %xmm0, %ymm8, %ymm7                     #1762.19
        vsubpd    %ymm12, %ymm9, %ymm0                          #1764.70
        vcvtps2pd %xmm8, %ymm6                                  #1763.25
        vcvtpd2ps %ymm0, %xmm8                                  #1765.101
        vsubpd    %ymm6, %ymm5, %ymm5                           #1764.21
        vorps     %ymm14, %ymm1, %ymm0                          #1774.18
        vorps     %ymm11, %ymm13, %ymm14                        #1775.18
        vorps     %ymm14, %ymm0, %ymm13                         #1776.17
        vmulps    %ymm7, %ymm7, %ymm0                           #1784.14
        vcvtpd2ps %ymm5, %xmm9                                  #1765.65
        vcmplt_oqps __svml_satan2_ha_data_internal(%rip), %ymm4, %ymm1 #1781.20
        vmulps    %ymm0, %ymm0, %ymm6                           #1785.14
        vmulps    1344+__svml_satan2_ha_data_internal(%rip), %ymm6, %ymm5 #1790.29
        vandps    256+__svml_satan2_ha_data_internal(%rip), %ymm1, %ymm14 #1782.20
        vandps    320+__svml_satan2_ha_data_internal(%rip), %ymm1, %ymm11 #1783.20
        vinsertf128 $1, %xmm8, %ymm9, %ymm8                     #1765.19
        vaddps    %ymm13, %ymm8, %ymm13                         #1777.19
        vaddps    1472+__svml_satan2_ha_data_internal(%rip), %ymm5, %ymm9 #1790.14
        vmulps    1408+__svml_satan2_ha_data_internal(%rip), %ymm6, %ymm8 #1791.29
        vmulps    %ymm9, %ymm0, %ymm15                          #1792.28
        vmovups   64(%rsp), %ymm9                               #1794.16[spill]
        vaddps    1536+__svml_satan2_ha_data_internal(%rip), %ymm8, %ymm12 #1791.14
        vaddps    %ymm15, %ymm12, %ymm1                         #1792.13
        vmulps    %ymm1, %ymm0, %ymm5                           #1793.13
        vmulps    %ymm5, %ymm7, %ymm6                           #1796.29
        vpxor     %xmm5, %xmm5, %xmm5                           #1683.76
        vxorps    %ymm9, %ymm11, %ymm11                         #1794.16
        vaddps    %ymm13, %ymm11, %ymm13                        #1795.19
        vaddps    %ymm6, %ymm13, %ymm8                          #1796.14
        vaddps    %ymm8, %ymm7, %ymm7                           #1797.14
        vaddps    %ymm7, %ymm10, %ymm10                         #1798.18
        vorps     %ymm9, %ymm10, %ymm0                          #1799.14
        vaddps    %ymm0, %ymm14, %ymm10                         #1800.18
        vmovups   144(%rsp), %xmm9                              #1802.52[spill]
        vorps     32(%rsp), %ymm10, %ymm0                       #1801.14[spill]
        vmovdqu   160(%rsp), %xmm10                             #1683.93[spill]
        vpackssdw 128(%rsp), %xmm10, %xmm1                      #1683.93[spill]
        vpacksswb %xmm5, %xmm1, %xmm6                           #1683.76
        vpmovmskb %xmm6, %eax                                   #1683.57
        vmovaps   %xmm2, %xmm1                                  #1802.52
        testb     %al, %al                                      #1802.52
        jne       ..B4.12       # Prob 5%                       #1802.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm9 xmm10 ymm0 ymm2 ymm3 ymm4
..B4.2:                         # Preds ..B4.12 ..B4.1
                                # Execution count [1.00e+00]
        testb     %dl, %dl                                      #1854.52
        jne       ..B4.4        # Prob 5%                       #1854.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm4
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1857.12
        popq      %rbp                                          #1857.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1857.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm1                                 #1854.239[spill]
        vmovups   %ymm4, 128(%rsp)                              #1854.313
        vmovups   %ymm0, 192(%rsp)                              #1854.387
        vmovups   %ymm1, 64(%rsp)                               #1854.239
        testl     %edx, %edx                                    #1854.491
        je        ..B4.3        # Prob 95%                      #1854.491
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1854.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B4.15:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1854.634
        jc        ..B4.11       # Prob 5%                       #1854.634
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1854.587
        cmpl      $8, %r12d                                     #1854.582
        jl        ..B4.8        # Prob 82%                      #1854.582
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm0                              #1854.806
        jmp       ..B4.3        # Prob 100%                     #1854.806
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1854.663
        lea       128(%rsp,%r12,4), %rsi                        #1854.663
        lea       192(%rsp,%r12,4), %rdx                        #1854.663
..___tag_value___svml_atan2f8_ha_e9.132:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #1854.663
..___tag_value___svml_atan2f8_ha_e9.133:
        jmp       ..B4.9        # Prob 100%                     #1854.663
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm11                                #1824.19[spill]
        vmovups   __svml_satan2_ha_data_internal(%rip), %ymm7   #1827.40
        vcmpordps %ymm4, %ymm4, %ymm12                          #1823.19
        vcmpordps %ymm11, %ymm11, %ymm13                        #1824.19
        vandps    %ymm13, %ymm12, %ymm14                        #1825.20
        vcmplt_oqps %ymm3, %ymm2, %ymm12                        #1837.21
        vblendvps %ymm12, %ymm3, %ymm2, %ymm2                   #1839.21
        vandnps   1856+__svml_satan2_ha_data_internal(%rip), %ymm12, %ymm3 #1840.24
        vpcmpeqd  %xmm7, %xmm1, %xmm1                           #1829.24
        vextractf128 $1, %ymm7, %xmm8                           #1827.113
        vpcmpeqd  96(%rsp), %xmm7, %xmm15                       #1828.24[spill]
        vpor      %xmm1, %xmm15, %xmm5                          #1830.26
        vpcmpeqd  112(%rsp), %xmm8, %xmm11                      #1828.92[spill]
        vpcmpeqd  %xmm8, %xmm9, %xmm9                           #1829.92
        vpor      %xmm9, %xmm11, %xmm1                          #1830.99
        vcmpeqps  %ymm7, %ymm2, %ymm2                           #1841.17
        vblendvps %ymm2, %ymm7, %ymm3, %ymm3                    #1842.16
        vpand     %xmm14, %xmm5, %xmm5                          #1831.32
        vextractf128 $1, %ymm14, %xmm6                          #1826.125
        vpandn    %xmm10, %xmm5, %xmm10                         #1832.25
        vpand     %xmm6, %xmm1, %xmm1                           #1831.116
        vpandn    128(%rsp), %xmm1, %xmm6                       #1832.108[spill]
        vpackssdw %xmm6, %xmm10, %xmm9                          #1833.79
        vpxor     %xmm10, %xmm10, %xmm10                        #1833.62
        vpacksswb %xmm10, %xmm9, %xmm11                         #1833.62
        vorps     64(%rsp), %ymm3, %ymm10                       #1843.13[spill]
        vpmovmskb %xmm11, %edx                                  #1833.43
        vpcmpgtd  %xmm4, %xmm7, %xmm7                           #1845.26
        vextractf128 $1, %ymm4, %xmm6                           #1844.111
        vpcmpgtd  %xmm6, %xmm8, %xmm8                           #1845.94
        vinsertf128 $1, %xmm8, %ymm7, %ymm9                     #1846.20
        vandps    1920+__svml_satan2_ha_data_internal(%rip), %ymm9, %ymm11 #1847.18
        vaddps    %ymm11, %ymm10, %ymm12                        #1848.17
        vorps     32(%rsp), %ymm12, %ymm13                      #1849.19[spill]
        vinsertf128 $1, %xmm1, %ymm5, %ymm1                     #1850.26
        vblendvps %ymm1, %ymm13, %ymm0, %ymm0                   #1851.14
        jmp       ..B4.2        # Prob 100%                     #1851.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm4
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f8_ha_e9,@function
	.size	__svml_atan2f8_ha_e9,.-__svml_atan2f8_ha_e9
..LN__svml_atan2f8_ha_e9.3:
	.data
# -- End  __svml_atan2f8_ha_e9
	.text
.L_2__routine_start___svml_atan2f4_ha_ex_4:
# -- Begin  __svml_atan2f4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f4_ha_ex
# --- __svml_atan2f4_ha_ex(__m128, __m128)
__svml_atan2f4_ha_ex:
# parameter 1: %xmm0
# parameter 2: %xmm1
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1862.1
        .byte     15                                            #2159.14
        .byte     30                                            #2159.14
        .byte     250                                           #2159.14
	.cfi_startproc
..___tag_value___svml_atan2f4_ha_ex.143:
..L144:
                                                        #1862.1
        pushq     %rbp                                          #1862.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1862.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1862.1
        subq      $256, %rsp                                    #1862.1
        movaps    %xmm1, %xmm12                                 #1862.1
        movups    192+__svml_satan2_ha_data_internal(%rip), %xmm9 #1975.50
        xorl      %edx, %edx                                    #1990.5
        movaps    %xmm9, %xmm10                                 #1976.14
        andps     %xmm0, %xmm9                                  #1977.14
        andps     %xmm12, %xmm10                                #1976.14
        movaps    %xmm9, %xmm4                                  #1986.16
        movdqu    1728+__svml_satan2_ha_data_internal(%rip), %xmm6 #1983.59
        movaps    %xmm10, %xmm8                                 #1985.16
        movups    640+__svml_satan2_ha_data_internal(%rip), %xmm15 #2021.47
        psubd     %xmm6, %xmm8                                  #1985.16
        psubd     %xmm6, %xmm4                                  #1986.16
        movaps    %xmm9, %xmm5                                  #1980.14
        movaps    %xmm15, %xmm3                                 #2025.38
        movdqa    %xmm8, %xmm1                                  #1987.30
        movups    %xmm0, (%rsp)                                 #1862.1[spill]
        movdqa    %xmm4, %xmm2                                  #1988.30
        movdqu    1792+__svml_satan2_ha_data_internal(%rip), %xmm11 #1984.59
        pxor      %xmm0, %xmm5                                  #1980.14
        movups    576+__svml_satan2_ha_data_internal(%rip), %xmm0 #1999.16
        pcmpgtd   %xmm11, %xmm1                                 #1987.30
        mulps     %xmm10, %xmm3                                 #2025.38
        pcmpeqd   %xmm11, %xmm8                                 #1987.77
        mulps     %xmm10, %xmm0                                 #1999.16
        mulps     %xmm9, %xmm15                                 #2038.30
        pcmpgtd   %xmm11, %xmm2                                 #1988.30
        pcmpeqd   %xmm11, %xmm4                                 #1988.77
        por       %xmm8, %xmm1                                  #1987.16
        por       %xmm4, %xmm2                                  #1988.16
        por       %xmm2, %xmm1                                  #1989.17
        movaps    %xmm10, %xmm7                                 #1979.14
        movdqu    %xmm1, 48(%rsp)                               #1989.17[spill]
        movaps    %xmm9, %xmm2                                  #2025.18
        movups    704+__svml_satan2_ha_data_internal(%rip), %xmm1 #2022.47
        pxor      %xmm12, %xmm7                                 #1979.14
        movups    %xmm7, 32(%rsp)                               #1979.14[spill]
        movaps    %xmm9, %xmm7                                  #2008.20
        movaps    %xmm1, %xmm11                                 #2027.38
        pcmpgtd   %xmm0, %xmm7                                  #2008.20
        mulps     %xmm10, %xmm11                                #2027.38
        subps     %xmm3, %xmm2                                  #2025.18
        addps     %xmm10, %xmm15                                #2038.18
        mulps     %xmm9, %xmm1                                  #2040.30
        movups    768+__svml_satan2_ha_data_internal(%rip), %xmm3 #2029.38
        movaps    %xmm9, %xmm4                                  #2027.18
        movaps    %xmm3, %xmm0                                  #2029.38
        xorl      %eax, %eax                                    #1990.5
        mulps     %xmm10, %xmm0                                 #2029.38
        subps     %xmm11, %xmm4                                 #2027.18
        addps     %xmm10, %xmm1                                 #2040.18
        mulps     %xmm9, %xmm3                                  #2042.30
        movups    384+__svml_satan2_ha_data_internal(%rip), %xmm14 #1996.16
        movaps    %xmm9, %xmm11                                 #2029.18
        movups    %xmm5, 16(%rsp)                               #1980.14[spill]
        subps     %xmm0, %xmm11                                 #2029.18
        mulps     %xmm10, %xmm14                                #1996.16
        addps     %xmm10, %xmm3                                 #2042.18
        movups    448+__svml_satan2_ha_data_internal(%rip), %xmm6 #1997.16
        pcmpgtd   %xmm9, %xmm14                                 #2005.20
        movups    512+__svml_satan2_ha_data_internal(%rip), %xmm5 #1998.16
        movaps    %xmm14, %xmm8                                 #2010.21
        mulps     %xmm10, %xmm6                                 #1997.16
        mulps     %xmm10, %xmm5                                 #1998.16
        movdqu    1600+__svml_satan2_ha_data_internal(%rip), %xmm13 #2013.20
        pcmpgtd   %xmm9, %xmm6                                  #2006.20
        pcmpgtd   %xmm9, %xmm5                                  #2007.20
        pxor      %xmm7, %xmm13                                 #2013.20
        pandn     %xmm6, %xmm8                                  #2010.21
        pandn     %xmm5, %xmm6                                  #2012.21
        pandn     %xmm13, %xmm5                                 #2014.21
        movaps    %xmm9, %xmm13                                 #2024.18
        andps     %xmm14, %xmm13                                #2024.18
        andps     %xmm6, %xmm4                                  #2028.18
        andps     %xmm5, %xmm11                                 #2030.18
        andps     %xmm10, %xmm14                                #2037.18
        andps     %xmm8, %xmm15                                 #2039.18
        andps     %xmm6, %xmm1                                  #2041.18
        andps     %xmm5, %xmm3                                  #2043.18
        andps     %xmm8, %xmm2                                  #2026.18
        movups    128+__svml_satan2_ha_data_internal(%rip), %xmm0 #2031.18
        orps      %xmm11, %xmm4                                 #2034.18
        movaps    %xmm9, %xmm11                                 #2044.18
        orps      %xmm15, %xmm14                                #2045.18
        orps      %xmm3, %xmm1                                  #2046.18
        pxor      %xmm10, %xmm0                                 #2031.18
        orps      %xmm2, %xmm13                                 #2033.18
        andps     %xmm7, %xmm11                                 #2044.18
        orps      %xmm1, %xmm14                                 #2047.18
        andps     %xmm7, %xmm0                                  #2032.18
        orps      %xmm4, %xmm13                                 #2035.18
        orps      %xmm11, %xmm14                                #2048.17
        movups    832+__svml_satan2_ha_data_internal(%rip), %xmm11 #2053.18
        orps      %xmm0, %xmm13                                 #2036.17
        movups    960+__svml_satan2_ha_data_internal(%rip), %xmm4 #2054.18
        andps     %xmm8, %xmm11                                 #2053.18
        andps     %xmm6, %xmm4                                  #2054.18
        orps      %xmm4, %xmm11                                 #2057.18
        cvtps2pd  %xmm13, %xmm4                                 #2060.23
        cvtps2pd  %xmm14, %xmm1                                 #2061.23
        movhlps   %xmm13, %xmm13                                #2060.74
        cvtps2pd  %xmm13, %xmm3                                 #2060.60
        movaps    %xmm14, %xmm13                                #2061.74
        movhlps   %xmm14, %xmm13                                #2061.74
        rcpps     %xmm14, %xmm14                                #2062.22
        movups    1088+__svml_satan2_ha_data_internal(%rip), %xmm0 #2055.18
        movups    1216+__svml_satan2_ha_data_internal(%rip), %xmm2 #2056.18
        andps     %xmm5, %xmm0                                  #2055.18
        andps     %xmm7, %xmm2                                  #2056.18
        orps      %xmm2, %xmm0                                  #2058.18
        cvtps2pd  %xmm13, %xmm2                                 #2061.60
        cvtps2pd  %xmm14, %xmm13                                #2063.28
        movhlps   %xmm14, %xmm14                                #2063.81
        orps      %xmm0, %xmm11                                 #2059.17
        cvtps2pd  %xmm14, %xmm0                                 #2063.67
        mulpd     %xmm13, %xmm1                                 #2065.50
        mulpd     %xmm0, %xmm2                                  #2065.121
        movups    1664+__svml_satan2_ha_data_internal(%rip), %xmm15 #2064.56
        movaps    %xmm15, %xmm14                                #2065.27
        subpd     %xmm1, %xmm14                                 #2065.27
        movaps    %xmm15, %xmm1                                 #2065.98
        subpd     %xmm2, %xmm1                                  #2065.98
        movaps    %xmm14, %xmm2                                 #2066.39
        mulpd     %xmm13, %xmm2                                 #2066.39
        mulpd     %xmm14, %xmm14                                #2067.35
        addpd     %xmm2, %xmm13                                 #2066.27
        addpd     %xmm15, %xmm14                                #2067.23
        movaps    %xmm1, %xmm2                                  #2066.114
        mulpd     %xmm0, %xmm2                                  #2066.114
        mulpd     %xmm1, %xmm1                                  #2067.110
        addpd     %xmm2, %xmm0                                  #2066.102
        addpd     %xmm1, %xmm15                                 #2067.98
        mulpd     %xmm14, %xmm13                                #2068.23
        mulpd     %xmm15, %xmm0                                 #2068.69
        mulpd     %xmm13, %xmm4                                 #2069.23
        mulpd     %xmm0, %xmm3                                  #2069.65
        cvtpd2ps  %xmm4, %xmm2                                  #2070.35
        cvtpd2ps  %xmm3, %xmm0                                  #2070.64
        movlhps   %xmm0, %xmm2                                  #2070.19
        movaps    %xmm2, %xmm1                                  #2071.80
        movhlps   %xmm2, %xmm1                                  #2071.80
        cvtps2pd  %xmm2, %xmm13                                 #2071.25
        cvtps2pd  %xmm1, %xmm14                                 #2071.66
        subpd     %xmm13, %xmm4                                 #2072.21
        subpd     %xmm14, %xmm3                                 #2072.67
        cvtpd2ps  %xmm4, %xmm4                                  #2073.35
        cvtpd2ps  %xmm3, %xmm3                                  #2073.66
        movups    896+__svml_satan2_ha_data_internal(%rip), %xmm0 #2078.18
        andps     %xmm8, %xmm0                                  #2078.18
        movups    1024+__svml_satan2_ha_data_internal(%rip), %xmm8 #2079.18
        andps     %xmm6, %xmm8                                  #2079.18
        orps      %xmm8, %xmm0                                  #2082.18
        movaps    %xmm2, %xmm8                                  #2092.14
        mulps     %xmm2, %xmm8                                  #2092.14
        movlhps   %xmm3, %xmm4                                  #2073.19
        movaps    %xmm8, %xmm3                                  #2093.14
        mulps     %xmm8, %xmm3                                  #2093.14
        movups    1152+__svml_satan2_ha_data_internal(%rip), %xmm6 #2080.18
        andps     %xmm5, %xmm6                                  #2080.18
        movups    1280+__svml_satan2_ha_data_internal(%rip), %xmm5 #2081.18
        andps     %xmm7, %xmm5                                  #2081.18
        movaps    %xmm12, %xmm7                                 #2089.20
        orps      %xmm5, %xmm6                                  #2083.18
        movups    1344+__svml_satan2_ha_data_internal(%rip), %xmm5 #2098.26
        orps      %xmm6, %xmm0                                  #2084.17
        mulps     %xmm3, %xmm5                                  #2098.26
        addps     %xmm0, %xmm4                                  #2085.19
        movups    1408+__svml_satan2_ha_data_internal(%rip), %xmm6 #2099.26
        mulps     %xmm3, %xmm6                                  #2099.26
        addps     1472+__svml_satan2_ha_data_internal(%rip), %xmm5 #2098.14
        mulps     %xmm8, %xmm5                                  #2100.25
        addps     1536+__svml_satan2_ha_data_internal(%rip), %xmm6 #2099.14
        movups    __svml_satan2_ha_data_internal(%rip), %xmm1   #2086.46
        addps     %xmm5, %xmm6                                  #2100.13
        cmpltps   %xmm1, %xmm7                                  #2089.20
        mulps     %xmm6, %xmm8                                  #2101.13
        movups    256+__svml_satan2_ha_data_internal(%rip), %xmm0 #2090.20
        movups    320+__svml_satan2_ha_data_internal(%rip), %xmm13 #2091.20
        andps     %xmm7, %xmm0                                  #2090.20
        andps     %xmm7, %xmm13                                 #2091.20
        movups    32(%rsp), %xmm7                               #2102.16[spill]
        pxor      %xmm7, %xmm13                                 #2102.16
        mulps     %xmm2, %xmm8                                  #2104.26
        addps     %xmm4, %xmm13                                 #2103.19
        movdqu    48(%rsp), %xmm5                               #1991.58[spill]
        addps     %xmm8, %xmm13                                 #2104.14
        movmskps  %xmm5, %ecx                                   #1991.58
        addps     %xmm13, %xmm2                                 #2105.14
        addps     %xmm2, %xmm11                                 #2106.18
        orps      %xmm7, %xmm11                                 #2107.14
        addps     %xmm11, %xmm0                                 #2108.18
        orps      16(%rsp), %xmm0                               #2109.14[spill]
        testl     %ecx, %ecx                                    #2110.52
        jne       ..B5.12       # Prob 5%                       #2110.52
                                # LOE rbx r12 r13 r14 r15 eax edx xmm0 xmm1 xmm5 xmm9 xmm10 xmm12
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #2162.52
        jne       ..B5.4        # Prob 5%                       #2162.52
                                # LOE rbx r12 r13 r14 r15 eax edx xmm0 xmm12
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2165.12
        popq      %rbp                                          #2165.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2165.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm1                                 #2162.236[spill]
        movups    %xmm1, 64(%rsp)                               #2162.236
        movups    %xmm12, 128(%rsp)                             #2162.307
        movups    %xmm0, 192(%rsp)                              #2162.378
        je        ..B5.3        # Prob 95%                      #2162.482
                                # LOE rbx r12 r13 r14 r15 eax edx xmm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2162.625
        jc        ..B5.11       # Prob 5%                       #2162.625
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2162.578
        cmpl      $4, %r12d                                     #2162.573
        jl        ..B5.8        # Prob 82%                      #2162.573
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    192(%rsp), %xmm0                              #2162.794
        jmp       ..B5.3        # Prob 100%                     #2162.794
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2162.654
        lea       128(%rsp,%r12,4), %rsi                        #2162.654
        lea       192(%rsp,%r12,4), %rdx                        #2162.654
..___tag_value___svml_atan2f4_ha_ex.169:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #2162.654
..___tag_value___svml_atan2f4_ha_ex.170:
        jmp       ..B5.9        # Prob 100%                     #2162.654
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movups    (%rsp), %xmm2                                 #2132.19[spill]
        movaps    %xmm12, %xmm4                                 #2131.19
        cmpordps  %xmm12, %xmm4                                 #2131.19
        cmpordps  %xmm2, %xmm2                                  #2132.19
        movaps    %xmm1, %xmm8                                  #2136.18
        movaps    %xmm1, %xmm3                                  #2137.18
        pcmpeqd   %xmm10, %xmm8                                 #2136.18
        pcmpeqd   %xmm9, %xmm3                                  #2137.18
        andps     %xmm2, %xmm4                                  #2133.20
        por       %xmm3, %xmm8                                  #2138.20
        andps     %xmm4, %xmm8                                  #2139.26
        movaps    %xmm8, %xmm6                                  #2140.19
        pandn     %xmm5, %xmm6                                  #2140.19
        movaps    %xmm9, %xmm5                                  #2145.21
        cmpltps   %xmm10, %xmm5                                 #2145.21
        movmskps  %xmm6, %edx                                   #2141.44
        movaps    %xmm5, %xmm7                                  #2147.32
        andps     %xmm5, %xmm10                                 #2147.64
        andnps    %xmm9, %xmm7                                  #2147.32
        movaps    %xmm8, %xmm9                                  #2159.25
        orps      %xmm10, %xmm7                                 #2147.21
        andnps    %xmm0, %xmm9                                  #2159.25
        cmpeqps   %xmm1, %xmm7                                  #2149.17
        andnps    1856+__svml_satan2_ha_data_internal(%rip), %xmm5 #2148.24
        movaps    %xmm7, %xmm10                                 #2150.27
        andnps    %xmm5, %xmm10                                 #2150.27
        andps     %xmm1, %xmm7                                  #2150.65
        orps      %xmm7, %xmm10                                 #2150.16
        pcmpgtd   %xmm12, %xmm1                                 #2153.20
        andps     1920+__svml_satan2_ha_data_internal(%rip), %xmm1 #2155.18
        movaps    %xmm9, %xmm0                                  #2159.14
        orps      32(%rsp), %xmm10                              #2151.13[spill]
        addps     %xmm1, %xmm10                                 #2156.17
        orps      16(%rsp), %xmm10                              #2157.19[spill]
        andps     %xmm8, %xmm10                                 #2159.70
        orps      %xmm10, %xmm0                                 #2159.14
        jmp       ..B5.2        # Prob 100%                     #2159.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax edx xmm0 xmm12
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f4_ha_ex,@function
	.size	__svml_atan2f4_ha_ex,.-__svml_atan2f4_ha_ex
..LN__svml_atan2f4_ha_ex.4:
	.data
# -- End  __svml_atan2f4_ha_ex
	.text
.L_2__routine_start___svml_atan2f8_ha_l9_5:
# -- Begin  __svml_atan2f8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_atan2f8_ha_l9
# --- __svml_atan2f8_ha_l9(__m256, __m256)
__svml_atan2f8_ha_l9:
# parameter 1: %ymm0
# parameter 2: %ymm1
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2170.1
        .byte     15                                            #2462.14
        .byte     30                                            #2462.14
        .byte     250                                           #2462.14
	.cfi_startproc
..___tag_value___svml_atan2f8_ha_l9.177:
..L178:
                                                        #2170.1
        pushq     %rbp                                          #2170.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2170.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2170.1
        subq      $256, %rsp                                    #2170.1
        xorl      %edx, %edx                                    #2298.5
        vmovups   192+__svml_satan2_ha_data_internal(%rip), %ymm10 #2283.53
        vmovups   1728+__svml_satan2_ha_data_internal(%rip), %ymm9 #2291.26
        vmovups   1792+__svml_satan2_ha_data_internal(%rip), %ymm2 #2292.26
        vmovups   %ymm0, (%rsp)                                 #2170.1[spill]
        vmovaps   %ymm1, %ymm12                                 #2170.1
        vandps    %ymm10, %ymm12, %ymm11                        #2284.14
        vandps    %ymm10, %ymm0, %ymm10                         #2285.14
        vxorps    %ymm11, %ymm12, %ymm3                         #2287.14
        vmovups   %ymm3, 32(%rsp)                               #2287.14[spill]
        vmulps    576+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm5 #2307.16
        vpsubd    %ymm9, %ymm11, %ymm6                          #2293.18
        vpsubd    %ymm9, %ymm10, %ymm3                          #2294.18
        vmulps    384+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm9 #2304.16
        vpcmpgtd  %ymm2, %ymm6, %ymm8                           #2295.35
        vpcmpeqd  %ymm2, %ymm6, %ymm4                           #2295.89
        vpcmpgtd  %ymm2, %ymm3, %ymm14                          #2296.35
        vpcmpeqd  %ymm2, %ymm3, %ymm1                           #2296.89
        vpor      %ymm4, %ymm8, %ymm13                          #2295.18
        vpor      %ymm1, %ymm14, %ymm15                         #2296.18
        vmulps    448+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm6 #2305.16
        vmulps    512+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm8 #2306.16
        vmovups   640+__svml_satan2_ha_data_internal(%rip), %ymm2 #2329.50
        vxorps    %ymm10, %ymm0, %ymm7                          #2288.14
        vmovups   %ymm7, 64(%rsp)                               #2288.14[spill]
        vpor      %ymm15, %ymm13, %ymm7                         #2297.19
        vmovdqu   %ymm7, 96(%rsp)                               #2297.19[spill]
        vpcmpgtd  %ymm5, %ymm10, %ymm7                          #2316.22
        vpxor     1600+__svml_satan2_ha_data_internal(%rip), %ymm7, %ymm3 #2321.22
        vpcmpgtd  %ymm10, %ymm6, %ymm0                          #2314.22
        vpcmpgtd  %ymm10, %ymm8, %ymm4                          #2315.22
        vpcmpgtd  %ymm10, %ymm9, %ymm1                          #2313.22
        vpandn    %ymm4, %ymm0, %ymm6                           #2320.23
        vpandn    %ymm3, %ymm4, %ymm9                           #2322.23
        vpandn    %ymm0, %ymm1, %ymm8                           #2318.23
        vmovups   704+__svml_satan2_ha_data_internal(%rip), %ymm3 #2330.50
        vmovups   768+__svml_satan2_ha_data_internal(%rip), %ymm4 #2337.18
        vmovaps   %ymm2, %ymm14                                 #2333.18
        vmovaps   %ymm3, %ymm15                                 #2335.18
        vmovaps   %ymm4, %ymm5                                  #2337.18
        vfnmadd213ps %ymm10, %ymm11, %ymm14                     #2333.18
        vfnmadd213ps %ymm10, %ymm11, %ymm15                     #2335.18
        vfnmadd213ps %ymm10, %ymm11, %ymm5                      #2337.18
        vfmadd213ps %ymm11, %ymm10, %ymm2                       #2346.18
        vfmadd213ps %ymm11, %ymm10, %ymm3                       #2348.18
        vfmadd213ps %ymm11, %ymm10, %ymm4                       #2350.18
        vandps    %ymm1, %ymm10, %ymm13                         #2332.18
        vandps    %ymm8, %ymm14, %ymm14                         #2334.18
        vandps    %ymm6, %ymm15, %ymm0                          #2336.18
        vandps    %ymm9, %ymm5, %ymm5                           #2338.18
        vxorps    128+__svml_satan2_ha_data_internal(%rip), %ymm11, %ymm15 #2339.18
        vorps     %ymm14, %ymm13, %ymm13                        #2341.18
        vorps     %ymm5, %ymm0, %ymm14                          #2342.18
        vandps    %ymm7, %ymm15, %ymm15                         #2340.18
        vorps     %ymm14, %ymm13, %ymm13                        #2343.18
        vorps     %ymm15, %ymm13, %ymm5                         #2344.17
        vandps    %ymm1, %ymm11, %ymm13                         #2345.18
        vandps    %ymm8, %ymm2, %ymm14                          #2347.18
        vandps    %ymm6, %ymm3, %ymm1                           #2349.18
        vandps    %ymm9, %ymm4, %ymm4                           #2351.18
        vorps     %ymm14, %ymm13, %ymm3                         #2353.18
        vorps     %ymm4, %ymm1, %ymm2                           #2354.18
        vandps    %ymm7, %ymm10, %ymm15                         #2352.18
        vorps     %ymm2, %ymm3, %ymm14                          #2355.18
        vorps     %ymm15, %ymm14, %ymm4                         #2356.17
        vandps    1088+__svml_satan2_ha_data_internal(%rip), %ymm9, %ymm0 #2363.18
        vandps    1216+__svml_satan2_ha_data_internal(%rip), %ymm7, %ymm3 #2364.18
        vorps     %ymm3, %ymm0, %ymm14                          #2366.18
        vrcpps    %ymm4, %ymm3                                  #2368.18
        vandps    832+__svml_satan2_ha_data_internal(%rip), %ymm8, %ymm1 #2361.18
        vandps    960+__svml_satan2_ha_data_internal(%rip), %ymm6, %ymm13 #2362.18
        vorps     %ymm13, %ymm1, %ymm2                          #2365.18
        vmovups   64+__svml_satan2_ha_data_internal(%rip), %ymm13 #2369.25
        vfnmadd231ps %ymm3, %ymm4, %ymm13                       #2369.25
        vfmadd213ps %ymm13, %ymm13, %ymm13                      #2370.25
        vfmadd213ps %ymm3, %ymm13, %ymm3                        #2371.22
        vmulps    %ymm3, %ymm5, %ymm15                          #2372.19
        vorps     %ymm14, %ymm2, %ymm1                          #2367.17
        vmovaps   %ymm4, %ymm2                                  #2373.19
        vfnmadd213ps %ymm5, %ymm15, %ymm2                       #2373.19
        vfmadd213ps %ymm15, %ymm3, %ymm2                        #2374.19
        vfnmadd213ps %ymm5, %ymm2, %ymm4                        #2375.19
        vmovups   1408+__svml_satan2_ha_data_internal(%rip), %ymm5 #2398.50
        vmulps    %ymm4, %ymm3, %ymm15                          #2376.19
        vandps    1152+__svml_satan2_ha_data_internal(%rip), %ymm9, %ymm9 #2383.18
        vandps    1280+__svml_satan2_ha_data_internal(%rip), %ymm7, %ymm7 #2384.18
        vorps     %ymm7, %ymm9, %ymm3                           #2386.18
        vmulps    %ymm2, %ymm2, %ymm7                           #2395.14
        vandps    896+__svml_satan2_ha_data_internal(%rip), %ymm8, %ymm8 #2381.18
        vandps    1024+__svml_satan2_ha_data_internal(%rip), %ymm6, %ymm6 #2382.18
        vorps     %ymm6, %ymm8, %ymm4                           #2385.18
        vmulps    %ymm7, %ymm7, %ymm6                           #2396.14
        vmovups   1344+__svml_satan2_ha_data_internal(%rip), %ymm8 #2401.14
        vfmadd213ps 1472+__svml_satan2_ha_data_internal(%rip), %ymm6, %ymm8 #2401.14
        vfmadd213ps 1536+__svml_satan2_ha_data_internal(%rip), %ymm5, %ymm6 #2402.14
        vorps     %ymm3, %ymm4, %ymm14                          #2387.17
        vmovups   __svml_satan2_ha_data_internal(%rip), %ymm4   #2389.49
        vfmadd213ps %ymm6, %ymm7, %ymm8                         #2403.13
        vmovups   32(%rsp), %ymm3                               #2405.16[spill]
        vcmplt_oqps %ymm4, %ymm12, %ymm0                        #2392.20
        vaddps    %ymm15, %ymm14, %ymm13                        #2388.19
        vandps    320+__svml_satan2_ha_data_internal(%rip), %ymm0, %ymm9 #2394.20
        vandps    256+__svml_satan2_ha_data_internal(%rip), %ymm0, %ymm14 #2393.20
        vmulps    %ymm8, %ymm7, %ymm0                           #2404.13
        vxorps    %ymm3, %ymm9, %ymm15                          #2405.16
        vaddps    %ymm13, %ymm15, %ymm13                        #2406.19
        vmovdqu   96(%rsp), %ymm15                              #2299.59[spill]
        vfmadd213ps %ymm13, %ymm2, %ymm0                        #2407.14
        vaddps    %ymm0, %ymm2, %ymm2                           #2408.14
        vaddps    %ymm2, %ymm1, %ymm1                           #2409.18
        vorps     %ymm3, %ymm1, %ymm2                           #2410.14
        vaddps    %ymm2, %ymm14, %ymm0                          #2411.18
        vmovups   64(%rsp), %ymm2                               #2412.14[spill]
        vmovmskps %ymm15, %eax                                  #2299.59
        vorps     %ymm2, %ymm0, %ymm0                           #2412.14
        testl     %eax, %eax                                    #2413.52
        jne       ..B6.12       # Prob 5%                       #2413.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm2 ymm3 ymm4 ymm10 ymm11 ymm12 ymm15
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #2465.52
        jne       ..B6.4        # Prob 5%                       #2465.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm12
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #2468.12
        popq      %rbp                                          #2468.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2468.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm1                                 #2465.239[spill]
        vmovups   %ymm12, 128(%rsp)                             #2465.313
        vmovups   %ymm0, 192(%rsp)                              #2465.387
        vmovups   %ymm1, 64(%rsp)                               #2465.239
        je        ..B6.3        # Prob 95%                      #2465.491
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #2465.571
                                # LOE rbx r12 r13 r14 r15 eax edx
..B6.15:                        # Preds ..B6.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2465.634
        jc        ..B6.11       # Prob 5%                       #2465.634
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2465.587
        cmpl      $8, %r12d                                     #2465.582
        jl        ..B6.8        # Prob 82%                      #2465.582
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   192(%rsp), %ymm0                              #2465.806
        jmp       ..B6.3        # Prob 100%                     #2465.806
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2465.663
        lea       128(%rsp,%r12,4), %rsi                        #2465.663
        lea       192(%rsp,%r12,4), %rdx                        #2465.663
..___tag_value___svml_atan2f8_ha_l9.203:
#       __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
        call      __svml_satan2_ha_cout_rare_internal           #2465.663
..___tag_value___svml_atan2f8_ha_l9.204:
        jmp       ..B6.9        # Prob 100%                     #2465.663
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   (%rsp), %ymm5                                 #2435.19[spill]
        vcmpordps %ymm12, %ymm12, %ymm6                         #2434.19
        vcmpordps %ymm5, %ymm5, %ymm7                           #2435.19
        vcmplt_oqps %ymm11, %ymm10, %ymm5                       #2448.21
        vpcmpeqd  %ymm4, %ymm11, %ymm8                          #2439.20
        vpcmpeqd  %ymm4, %ymm10, %ymm9                          #2440.20
        vblendvps %ymm5, %ymm11, %ymm10, %ymm10                 #2450.21
        vpor      %ymm9, %ymm8, %ymm13                          #2441.22
        vandnps   1856+__svml_satan2_ha_data_internal(%rip), %ymm5, %ymm11 #2451.24
        vcmpeqps  %ymm4, %ymm10, %ymm5                          #2452.17
        vandps    %ymm7, %ymm6, %ymm14                          #2436.20
        vpand     %ymm14, %ymm13, %ymm1                         #2442.28
        vblendvps %ymm5, %ymm4, %ymm11, %ymm6                   #2453.16
        vpandn    %ymm15, %ymm1, %ymm15                         #2443.21
        vpcmpgtd  %ymm12, %ymm4, %ymm4                          #2456.22
        vandps    1920+__svml_satan2_ha_data_internal(%rip), %ymm4, %ymm4 #2458.18
        vorps     %ymm3, %ymm6, %ymm3                           #2454.13
        vaddps    %ymm4, %ymm3, %ymm3                           #2459.17
        vorps     %ymm2, %ymm3, %ymm2                           #2460.19
        vmovmskps %ymm15, %edx                                  #2444.45
        vblendvps %ymm1, %ymm2, %ymm0, %ymm0                    #2462.14
        jmp       ..B6.2        # Prob 100%                     #2462.14
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm12
	.cfi_endproc
# mark_end;
	.type	__svml_atan2f8_ha_l9,@function
	.size	__svml_atan2f8_ha_l9,.-__svml_atan2f8_ha_l9
..LN__svml_atan2f8_ha_l9.5:
	.data
# -- End  __svml_atan2f8_ha_l9
	.text
.L_2__routine_start___svml_satan2_ha_cout_rare_internal_6:
# -- Begin  __svml_satan2_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_satan2_ha_cout_rare_internal
	.globl __svml_satan2_ha_cout_rare_internal
# --- __svml_satan2_ha_cout_rare_internal(const float *, const float *, float *)
__svml_satan2_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
# parameter 3: %rdx
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #391.1
        .byte     15                                            #614.79
        .byte     30                                            #614.79
        .byte     250                                           #614.79
	.cfi_startproc
..___tag_value___svml_satan2_ha_cout_rare_internal.209:
..L210:
                                                        #391.1
        pxor      %xmm1, %xmm1                                  #404.12
        movss     ione(%rip), %xmm0                             #401.26
        movq      %rdx, %r8                                     #391.1
        movss     (%rdi), %xmm3                                 #401.20
        movss     (%rsi), %xmm2                                 #402.20
        mulss     %xmm0, %xmm3                                  #401.26
        mulss     %xmm0, %xmm2                                  #402.26
        pxor      %xmm0, %xmm0                                  #403.12
        cvtss2sd  %xmm3, %xmm0                                  #403.12
        cvtss2sd  %xmm2, %xmm1                                  #404.12
        movss     %xmm3, -32(%rsp)                              #401.5
        movss     %xmm2, -28(%rsp)                              #402.5
        movsd     %xmm0, -48(%rsp)                              #403.5
        movsd     %xmm1, -40(%rsp)                              #404.5
        movzwl    -30(%rsp), %edi                               #409.32
        andl      $32640, %edi                                  #409.32
        movb      -25(%rsp), %dl                                #406.35
        movzwl    -42(%rsp), %eax                               #407.32
        andb      $-128, %dl                                    #406.35
        movzwl    -34(%rsp), %r9d                               #408.32
        andl      $32752, %eax                                  #407.32
        andl      $32752, %r9d                                  #408.32
        shrl      $7, %edi                                      #409.32
        movb      -29(%rsp), %cl                                #405.35
        shrb      $7, %cl                                       #405.35
        shrb      $7, %dl                                       #406.35
        shrl      $4, %eax                                      #407.32
        shrl      $4, %r9d                                      #408.32
        cmpl      $255, %edi                                    #409.55
        je        ..B7.35       # Prob 16%                      #409.55
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax edi r9d dl cl xmm0 xmm1 xmm2 xmm3
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        movzwl    -26(%rsp), %esi                               #409.86
        andl      $32640, %esi                                  #409.86
        cmpl      $32640, %esi                                  #409.109
        je        ..B7.35       # Prob 16%                      #409.109
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax edi r9d dl cl xmm0 xmm1 xmm2 xmm3
..B7.3:                         # Preds ..B7.2
                                # Execution count [7.06e-01]
        testl     %eax, %eax                                    #411.21
        jne       ..B7.5        # Prob 50%                      #411.21
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl xmm0 xmm1
..B7.4:                         # Preds ..B7.3
                                # Execution count [3.53e-01]
        testl     $8388607, -32(%rsp)                           #411.50
        je        ..B7.30       # Prob 50%                      #411.76
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl xmm0 xmm1
..B7.5:                         # Preds ..B7.4 ..B7.3
                                # Execution count [5.29e-01]
        testl     %r9d, %r9d                                    #412.24
        jne       ..B7.7        # Prob 50%                      #412.24
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl xmm0 xmm1
..B7.6:                         # Preds ..B7.5
                                # Execution count [2.65e-01]
        testl     $8388607, -28(%rsp)                           #412.53
        je        ..B7.27       # Prob 50%                      #412.79
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl xmm0 xmm1
..B7.7:                         # Preds ..B7.6 ..B7.5
                                # Execution count [3.97e-01]
        negl      %r9d                                          #418.18
        movsd     %xmm0, -48(%rsp)                              #414.13
        addl      %eax, %r9d                                    #418.18
        movsd     %xmm1, -40(%rsp)                              #416.13
        movb      -41(%rsp), %dil                               #415.34
        movb      -33(%rsp), %sil                               #417.34
        andb      $127, %dil                                    #415.34
        andb      $127, %sil                                    #417.34
        cmpl      $-54, %r9d                                    #418.30
        jle       ..B7.22       # Prob 50%                      #418.30
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl sil dil xmm1
..B7.8:                         # Preds ..B7.7
                                # Execution count [1.98e-01]
        cmpl      $54, %r9d                                     #420.34
        jge       ..B7.19       # Prob 50%                      #420.34
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax r9d dl cl sil dil xmm1
..B7.9:                         # Preds ..B7.8
                                # Execution count [9.92e-02]
        movb      %sil, -33(%rsp)                               #417.34
        movb      %dil, -41(%rsp)                               #415.34
        testb     %dl, %dl                                      #422.36
        jne       ..B7.11       # Prob 50%                      #422.36
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl
..B7.10:                        # Preds ..B7.9
                                # Execution count [4.96e-02]
        movsd     1976+_vmldAtanHATab(%rip), %xmm1              #424.51
        movaps    %xmm1, %xmm0                                  #425.25
        jmp       ..B7.12       # Prob 100%                     #425.25
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl xmm0 xmm1
..B7.11:                        # Preds ..B7.9
                                # Execution count [4.96e-02]
        movsd     1936+_vmldAtanHATab(%rip), %xmm1              #429.51
        movsd     1944+_vmldAtanHATab(%rip), %xmm0              #430.51
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl xmm0 xmm1
..B7.12:                        # Preds ..B7.10 ..B7.11
                                # Execution count [9.92e-02]
        movsd     -48(%rsp), %xmm4                              #432.29
        movsd     -40(%rsp), %xmm2                              #432.36
        movaps    %xmm4, %xmm5                                  #432.36
        divsd     %xmm2, %xmm5                                  #432.36
        movzwl    -42(%rsp), %esi                               #432.29
        movsd     %xmm5, -16(%rsp)                              #432.21
        testl     %eax, %eax                                    #433.32
        jle       ..B7.34       # Prob 16%                      #433.32
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax esi dl cl xmm0 xmm1 xmm2 xmm4 xmm5
..B7.13:                        # Preds ..B7.12
                                # Execution count [8.33e-02]
        cmpl      $2046, %eax                                   #435.36
        jge       ..B7.15       # Prob 50%                      #435.36
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax esi dl cl xmm0 xmm1 xmm2 xmm4 xmm5
..B7.14:                        # Preds ..B7.13
                                # Execution count [4.17e-02]
        andl      $-32753, %esi                                 #438.50
        addl      $-1023, %eax                                  #439.42
        movsd     %xmm4, -48(%rsp)                              #437.29
        addl      $16368, %esi                                  #438.50
        movw      %si, -42(%rsp)                                #438.50
        jmp       ..B7.16       # Prob 100%                     #438.50
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl xmm0 xmm1 xmm2 xmm5
..B7.15:                        # Preds ..B7.13
                                # Execution count [4.17e-02]
        movsd     1992+_vmldAtanHATab(%rip), %xmm3              #443.61
        movl      $1022, %eax                                   #444.29
        mulsd     %xmm3, %xmm4                                  #443.61
        movsd     %xmm4, -48(%rsp)                              #443.29
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl xmm0 xmm1 xmm2 xmm5
..B7.16:                        # Preds ..B7.14 ..B7.15 ..B7.34
                                # Execution count [9.92e-02]
        negl      %eax                                          #453.78
        movq      1888+_vmldAtanHATab(%rip), %rsi               #452.50
        addl      $1023, %eax                                   #453.78
        movq      %rsi, -40(%rsp)                               #452.21
        andl      $2047, %eax                                   #453.42
        shrq      $48, %rsi                                     #453.42
        shll      $4, %eax                                      #453.42
        andl      $-32753, %esi                                 #453.42
        orl       %eax, %esi                                    #453.42
        movw      %si, -34(%rsp)                                #453.42
        movsd     -40(%rsp), %xmm3                              #454.36
        mulsd     %xmm3, %xmm2                                  #454.36
        comisd    1880+_vmldAtanHATab(%rip), %xmm5              #455.52
        jb        ..B7.18       # Prob 50%                      #455.52
                                # LOE rbx rbp r8 r12 r13 r14 r15 dl cl xmm0 xmm1 xmm2 xmm5
..B7.17:                        # Preds ..B7.16
                                # Execution count [4.96e-02]
        movsd     2000+_vmldAtanHATab(%rip), %xmm12             #466.72
        movaps    %xmm2, %xmm3                                  #466.206
        mulsd     %xmm2, %xmm12                                 #466.72
        movsd     %xmm12, -72(%rsp)                             #466.25
        movsd     -72(%rsp), %xmm13                             #466.116
        movsd     %xmm5, -24(%rsp)                              #463.25
        subsd     %xmm2, %xmm13                                 #466.126
        movsd     %xmm13, -64(%rsp)                             #466.97
        movsd     -72(%rsp), %xmm15                             #466.156
        movsd     -64(%rsp), %xmm14                             #466.166
        movl      -20(%rsp), %edi                               #465.91
        movl      %edi, %r9d                                    #465.115
        andl      $-524288, %edi                                #465.182
        andl      $-1048576, %r9d                               #465.115
        addl      $262144, %edi                                 #465.193
        subsd     %xmm14, %xmm15                                #466.166
        movsd     %xmm15, -72(%rsp)                             #466.137
        andl      $1048575, %edi                                #465.204
        movsd     -72(%rsp), %xmm4                              #466.206
        orl       %edi, %r9d                                    #465.204
        movl      $0, -24(%rsp)                                 #464.46
        subsd     %xmm4, %xmm3                                  #466.206
        movl      %r9d, -20(%rsp)                               #465.46
        movsd     %xmm3, -64(%rsp)                              #466.177
        movsd     -72(%rsp), %xmm5                              #466.225
        movsd     -24(%rsp), %xmm11                             #467.42
        movsd     -64(%rsp), %xmm9                              #466.242
        mulsd     %xmm11, %xmm5                                 #467.42
        mulsd     %xmm11, %xmm9                                 #468.42
        movsd     1968+_vmldAtanHATab(%rip), %xmm8              #469.60
        mulsd     %xmm8, %xmm5                                  #469.60
        mulsd     %xmm8, %xmm9                                  #470.60
        movaps    %xmm5, %xmm7                                  #471.55
        movzwl    -10(%rsp), %esi                               #457.51
        addsd     %xmm9, %xmm7                                  #471.55
        movsd     %xmm7, -72(%rsp)                              #471.25
        andl      $32752, %esi                                  #457.51
        movsd     -72(%rsp), %xmm6                              #471.96
        shrl      $4, %esi                                      #457.51
        subsd     %xmm6, %xmm5                                  #471.96
        movl      -12(%rsp), %eax                               #459.56
        addsd     %xmm5, %xmm9                                  #471.135
        movsd     %xmm9, -64(%rsp)                              #471.107
        andl      $1048575, %eax                                #459.56
        movsd     -48(%rsp), %xmm9                              #472.44
        movsd     -72(%rsp), %xmm3                              #471.156
        movaps    %xmm9, %xmm12                                 #472.54
        movsd     -64(%rsp), %xmm10                             #471.174
        movaps    %xmm9, %xmm14                                 #472.95
        movaps    %xmm9, %xmm6                                  #472.216
        addsd     %xmm3, %xmm12                                 #472.54
        movsd     %xmm12, -72(%rsp)                             #472.25
        movsd     -72(%rsp), %xmm13                             #472.95
        shll      $20, %esi                                     #458.36
        subsd     %xmm13, %xmm14                                #472.95
        movsd     %xmm14, -64(%rsp)                             #472.66
        orl       %eax, %esi                                    #459.56
        movsd     -72(%rsp), %xmm4                              #472.125
        addl      $-1069547520, %esi                            #460.35
        movsd     -64(%rsp), %xmm15                             #472.135
        movl      $113, %eax                                    #462.25
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #474.72
        addsd     %xmm15, %xmm4                                 #472.135
        movsd     %xmm4, -56(%rsp)                              #472.106
        movsd     -64(%rsp), %xmm8                              #472.176
        sarl      $19, %esi                                     #461.36
        addsd     %xmm3, %xmm8                                  #472.176
        movsd     %xmm8, -64(%rsp)                              #472.146
        cmpl      $113, %esi                                    #462.25
        movsd     -56(%rsp), %xmm7                              #472.216
        cmovl     %esi, %eax                                    #462.25
        subsd     %xmm7, %xmm6                                  #472.216
        movsd     %xmm6, -56(%rsp)                              #472.187
        addl      %eax, %eax                                    #499.92
        movsd     -64(%rsp), %xmm12                             #472.246
        lea       _vmldAtanHATab(%rip), %rsi                    #499.72
        movsd     -56(%rsp), %xmm5                              #472.256
        movslq    %eax, %rax                                    #499.72
        addsd     %xmm5, %xmm12                                 #472.256
        movsd     %xmm12, -56(%rsp)                             #472.227
        movsd     -72(%rsp), %xmm7                              #472.275
        mulsd     %xmm7, %xmm13                                 #474.72
        movsd     -56(%rsp), %xmm8                              #472.292
        movsd     %xmm13, -72(%rsp)                             #474.25
        addsd     %xmm10, %xmm8                                 #473.41
        movsd     -72(%rsp), %xmm4                              #474.116
        movaps    %xmm9, %xmm10                                 #476.72
        mulsd     2000+_vmldAtanHATab(%rip), %xmm10             #476.72
        subsd     %xmm7, %xmm4                                  #474.126
        movsd     %xmm4, -64(%rsp)                              #474.97
        movsd     -72(%rsp), %xmm3                              #474.156
        movsd     -64(%rsp), %xmm14                             #474.166
        subsd     %xmm14, %xmm3                                 #474.166
        movsd     %xmm3, -72(%rsp)                              #474.137
        movsd     -72(%rsp), %xmm15                             #474.206
        subsd     %xmm15, %xmm7                                 #474.206
        movsd     %xmm7, -64(%rsp)                              #474.177
        movsd     -72(%rsp), %xmm7                              #474.225
        movsd     -64(%rsp), %xmm4                              #474.243
        movsd     %xmm10, -72(%rsp)                             #476.25
        movaps    %xmm2, %xmm10                                 #480.95
        addsd     %xmm4, %xmm8                                  #475.41
        movsd     -72(%rsp), %xmm4                              #476.116
        subsd     -48(%rsp), %xmm4                              #476.126
        movsd     %xmm4, -64(%rsp)                              #476.97
        movsd     -72(%rsp), %xmm6                              #476.156
        movsd     -64(%rsp), %xmm3                              #476.166
        subsd     %xmm3, %xmm6                                  #476.166
        movaps    %xmm2, %xmm3                                  #480.54
        movsd     %xmm6, -72(%rsp)                              #476.137
        movsd     -72(%rsp), %xmm5                              #476.206
        subsd     %xmm5, %xmm9                                  #476.206
        movsd     %xmm9, -64(%rsp)                              #476.177
        movsd     -72(%rsp), %xmm12                             #476.225
        movsd     -64(%rsp), %xmm9                              #476.242
        mulsd     %xmm11, %xmm12                                #477.42
        mulsd     %xmm11, %xmm9                                 #478.42
        movaps    %xmm12, %xmm11                                #479.55
        addsd     %xmm9, %xmm11                                 #479.55
        movsd     %xmm11, -72(%rsp)                             #479.25
        movsd     -72(%rsp), %xmm4                              #479.96
        subsd     %xmm4, %xmm12                                 #479.96
        addsd     %xmm9, %xmm12                                 #479.135
        movsd     %xmm12, -64(%rsp)                             #479.107
        movsd     -72(%rsp), %xmm15                             #479.156
        movsd     -64(%rsp), %xmm6                              #479.174
        addsd     %xmm15, %xmm3                                 #480.54
        movsd     %xmm3, -72(%rsp)                              #480.25
        movsd     -72(%rsp), %xmm5                              #480.95
        movsd     2000+_vmldAtanHATab(%rip), %xmm3              #482.72
        subsd     %xmm5, %xmm10                                 #480.95
        movsd     %xmm10, -64(%rsp)                             #480.66
        movsd     -72(%rsp), %xmm13                             #480.125
        movsd     -64(%rsp), %xmm11                             #480.135
        addsd     %xmm11, %xmm13                                #480.135
        movsd     %xmm13, -56(%rsp)                             #480.106
        movsd     -64(%rsp), %xmm14                             #480.176
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #484.137
        addsd     %xmm14, %xmm15                                #480.176
        movsd     %xmm15, -64(%rsp)                             #480.146
        movsd     -56(%rsp), %xmm4                              #480.216
        movsd     1888+_vmldAtanHATab(%rip), %xmm14             #484.83
        subsd     %xmm4, %xmm2                                  #480.216
        movsd     %xmm2, -56(%rsp)                              #480.187
        movsd     -64(%rsp), %xmm4                              #480.246
        movsd     -56(%rsp), %xmm2                              #480.256
        addsd     %xmm2, %xmm4                                  #480.256
        movsd     %xmm4, -56(%rsp)                              #480.227
        movsd     -72(%rsp), %xmm12                             #480.275
        mulsd     %xmm12, %xmm3                                 #482.72
        movsd     -56(%rsp), %xmm5                              #480.292
        movsd     %xmm3, -72(%rsp)                              #482.25
        addsd     %xmm6, %xmm5                                  #481.41
        movsd     -72(%rsp), %xmm9                              #482.116
        subsd     %xmm12, %xmm9                                 #482.126
        movsd     %xmm9, -64(%rsp)                              #482.97
        movsd     -72(%rsp), %xmm10                             #482.156
        movsd     -64(%rsp), %xmm2                              #482.166
        subsd     %xmm2, %xmm10                                 #482.166
        movsd     %xmm10, -72(%rsp)                             #482.137
        movsd     -72(%rsp), %xmm11                             #482.206
        subsd     %xmm11, %xmm12                                #482.206
        movsd     %xmm12, -64(%rsp)                             #482.177
        movsd     -72(%rsp), %xmm9                              #482.225
        divsd     %xmm9, %xmm14                                 #484.83
        mulsd     %xmm14, %xmm13                                #484.137
        movsd     -64(%rsp), %xmm10                             #482.243
        movsd     %xmm13, -64(%rsp)                             #484.92
        addsd     %xmm10, %xmm5                                 #483.41
        movsd     -64(%rsp), %xmm15                             #484.180
        movsd     1888+_vmldAtanHATab(%rip), %xmm12             #484.335
        subsd     %xmm14, %xmm15                                #484.190
        movsd     %xmm15, -56(%rsp)                             #484.161
        movsd     -64(%rsp), %xmm2                              #484.219
        movsd     -56(%rsp), %xmm4                              #484.229
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #494.77
        subsd     %xmm4, %xmm2                                  #484.229
        movsd     %xmm2, -56(%rsp)                              #484.200
        movsd     -56(%rsp), %xmm3                              #484.266
        mulsd     %xmm3, %xmm9                                  #484.266
        movsd     -56(%rsp), %xmm11                             #484.372
        subsd     %xmm9, %xmm12                                 #484.335
        mulsd     %xmm11, %xmm5                                 #484.372
        movsd     %xmm5, -64(%rsp)                              #484.345
        movsd     -64(%rsp), %xmm5                              #484.411
        subsd     %xmm5, %xmm12                                 #484.411
        movsd     %xmm12, -64(%rsp)                             #484.383
        movsd     -64(%rsp), %xmm2                              #484.480
        movq      -56(%rsp), %r10                               #484.499
        movsd     -64(%rsp), %xmm6                              #484.535
        movsd     -56(%rsp), %xmm4                              #484.572
        movq      %r10, -40(%rsp)                               #484.491
        movsd     -40(%rsp), %xmm3                              #485.53
        movaps    %xmm3, %xmm5                                  #485.53
        addsd     1888+_vmldAtanHATab(%rip), %xmm2              #484.480
        mulsd     %xmm7, %xmm5                                  #485.53
        mulsd     %xmm6, %xmm2                                  #484.535
        mulsd     %xmm4, %xmm2                                  #484.572
        mulsd     %xmm2, %xmm7                                  #485.140
        mulsd     %xmm8, %xmm2                                  #485.92
        mulsd     %xmm3, %xmm8                                  #485.189
        addsd     %xmm2, %xmm7                                  #485.140
        movsd     1872+_vmldAtanHATab(%rip), %xmm3              #488.83
        addsd     %xmm8, %xmm7                                  #485.189
        movsd     %xmm7, -72(%rsp)                              #485.151
        movaps    %xmm5, %xmm7                                  #486.54
        movsd     -72(%rsp), %xmm4                              #485.224
        movsd     2000+_vmldAtanHATab(%rip), %xmm6              #492.72
        addsd     %xmm4, %xmm7                                  #486.54
        movsd     %xmm7, -72(%rsp)                              #486.25
        movsd     -72(%rsp), %xmm8                              #486.93
        subsd     %xmm8, %xmm5                                  #486.93
        addsd     %xmm4, %xmm5                                  #486.132
        movsd     %xmm5, -64(%rsp)                              #486.104
        movsd     -72(%rsp), %xmm11                             #486.151
        movaps    %xmm11, %xmm2                                 #487.38
        mulsd     %xmm11, %xmm2                                 #487.38
        mulsd     %xmm11, %xmm6                                 #492.72
        mulsd     %xmm2, %xmm3                                  #488.83
        movsd     -64(%rsp), %xmm4                              #486.168
        movsd     %xmm6, -72(%rsp)                              #492.25
        movsd     -72(%rsp), %xmm7                              #492.116
        addsd     1864+_vmldAtanHATab(%rip), %xmm3              #488.107
        subsd     %xmm11, %xmm7                                 #492.126
        mulsd     %xmm2, %xmm3                                  #488.129
        movsd     %xmm7, -64(%rsp)                              #492.97
        movsd     -72(%rsp), %xmm9                              #492.156
        movsd     -64(%rsp), %xmm8                              #492.166
        addsd     1856+_vmldAtanHATab(%rip), %xmm3              #489.62
        subsd     %xmm8, %xmm9                                  #492.166
        mulsd     %xmm2, %xmm3                                  #489.84
        movsd     %xmm9, -72(%rsp)                              #492.137
        movsd     -72(%rsp), %xmm10                             #492.206
        addsd     1848+_vmldAtanHATab(%rip), %xmm3              #489.108
        subsd     %xmm10, %xmm11                                #492.206
        mulsd     %xmm2, %xmm3                                  #489.130
        movsd     %xmm11, -64(%rsp)                             #492.177
        addsd     1840+_vmldAtanHATab(%rip), %xmm3              #490.62
        mulsd     %xmm2, %xmm3                                  #490.84
        addsd     1832+_vmldAtanHATab(%rip), %xmm3              #490.108
        mulsd     %xmm2, %xmm3                                  #490.130
        addsd     1824+_vmldAtanHATab(%rip), %xmm3              #490.154
        mulsd     %xmm2, %xmm3                                  #491.51
        mulsd     %xmm3, %xmm13                                 #494.77
        movsd     -72(%rsp), %xmm2                              #492.225
        movsd     -64(%rsp), %xmm12                             #492.243
        movsd     %xmm13, -72(%rsp)                             #494.25
        addsd     %xmm12, %xmm4                                 #493.41
        movsd     -72(%rsp), %xmm14                             #494.121
        subsd     %xmm3, %xmm14                                 #494.131
        movsd     %xmm14, -64(%rsp)                             #494.102
        movsd     -72(%rsp), %xmm5                              #494.166
        movsd     -64(%rsp), %xmm15                             #494.176
        subsd     %xmm15, %xmm5                                 #494.176
        movsd     %xmm5, -72(%rsp)                              #494.147
        movsd     -72(%rsp), %xmm6                              #494.221
        subsd     %xmm6, %xmm3                                  #494.221
        movsd     %xmm3, -64(%rsp)                              #494.187
        movsd     -72(%rsp), %xmm6                              #494.247
        movsd     -64(%rsp), %xmm5                              #494.271
        movaps    %xmm6, %xmm12                                 #495.60
        movaps    %xmm5, %xmm3                                  #495.106
        mulsd     %xmm4, %xmm6                                  #495.161
        mulsd     %xmm4, %xmm3                                  #495.106
        mulsd     %xmm2, %xmm5                                  #495.217
        mulsd     %xmm2, %xmm12                                 #495.60
        addsd     %xmm3, %xmm6                                  #495.161
        movaps    %xmm12, %xmm7                                 #496.54
        movaps    %xmm12, %xmm8                                 #496.94
        addsd     %xmm5, %xmm6                                  #495.217
        addsd     %xmm2, %xmm7                                  #496.54
        movsd     %xmm6, -72(%rsp)                              #495.172
        movsd     -72(%rsp), %xmm5                              #495.252
        movsd     %xmm7, -72(%rsp)                              #496.25
        movsd     -72(%rsp), %xmm3                              #496.94
        subsd     %xmm3, %xmm8                                  #496.94
        movsd     %xmm8, -64(%rsp)                              #496.65
        movsd     -72(%rsp), %xmm10                             #496.124
        movsd     -64(%rsp), %xmm9                              #496.134
        addsd     %xmm9, %xmm10                                 #496.134
        movsd     %xmm10, -56(%rsp)                             #496.105
        movsd     -64(%rsp), %xmm11                             #496.174
        addsd     %xmm11, %xmm2                                 #496.174
        movsd     %xmm2, -64(%rsp)                              #496.145
        movsd     -56(%rsp), %xmm2                              #496.214
        subsd     %xmm2, %xmm12                                 #496.214
        movsd     %xmm12, -56(%rsp)                             #496.185
        movsd     -64(%rsp), %xmm14                             #496.244
        movsd     -56(%rsp), %xmm13                             #496.254
        addsd     %xmm13, %xmm14                                #496.254
        movsd     %xmm14, -56(%rsp)                             #496.225
        movq      -72(%rsp), %r11                               #496.273
        movsd     -56(%rsp), %xmm15                             #496.290
        movq      %r11, -40(%rsp)                               #496.265
        addsd     %xmm15, %xmm4                                 #497.41
        movsd     -40(%rsp), %xmm8                              #499.44
        addsd     %xmm5, %xmm4                                  #498.41
        movsd     %xmm4, -32(%rsp)                              #498.25
        movaps    %xmm8, %xmm4                                  #499.72
        movaps    %xmm8, %xmm2                                  #499.133
        addsd     (%rsi,%rax,8), %xmm4                          #499.72
        movsd     %xmm4, -72(%rsp)                              #499.25
        movsd     -72(%rsp), %xmm4                              #499.133
        subsd     %xmm4, %xmm2                                  #499.133
        movsd     %xmm2, -64(%rsp)                              #499.104
        movsd     -72(%rsp), %xmm5                              #499.163
        movsd     -64(%rsp), %xmm3                              #499.173
        addsd     %xmm3, %xmm5                                  #499.173
        movsd     %xmm5, -56(%rsp)                              #499.144
        movsd     -64(%rsp), %xmm6                              #499.252
        addsd     (%rsi,%rax,8), %xmm6                          #499.252
        movsd     %xmm6, -64(%rsp)                              #499.184
        movsd     -56(%rsp), %xmm7                              #499.292
        subsd     %xmm7, %xmm8                                  #499.292
        movsd     %xmm8, -56(%rsp)                              #499.263
        movsd     -64(%rsp), %xmm10                             #499.322
        movsd     -56(%rsp), %xmm9                              #499.332
        addsd     %xmm9, %xmm10                                 #499.332
        movsd     %xmm10, -56(%rsp)                             #499.303
        movq      -72(%rsp), %rdi                               #499.351
        movq      %rdi, -40(%rsp)                               #499.343
                                # LOE rax rbx rbp rsi rdi r8 r12 r13 r14 r15 dl cl xmm0 xmm1
..B7.51:                        # Preds ..B7.17
                                # Execution count [4.96e-02]
        movsd     -56(%rsp), %xmm2                              #499.369
        movaps    %xmm1, %xmm3                                  #505.54
        shrq      $56, %rdi                                     #502.46
        addsd     -32(%rsp), %xmm2                              #500.41
        shlb      $7, %cl                                       #509.46
        addsd     8(%rsi,%rax,8), %xmm2                         #501.58
        movb      %dl, %al                                      #502.46
        andb      $127, %dil                                    #502.46
        shlb      $7, %al                                       #502.46
        movsd     %xmm2, -32(%rsp)                              #501.25
        orb       %al, %dil                                     #502.46
        movb      %dil, -33(%rsp)                               #502.46
        movsd     -40(%rsp), %xmm9                              #505.44
        movaps    %xmm9, %xmm5                                  #505.95
        addsd     %xmm9, %xmm3                                  #505.54
        movsd     %xmm3, -72(%rsp)                              #505.25
        movsd     -72(%rsp), %xmm4                              #505.95
        movb      -25(%rsp), %sil                               #504.46
        movb      %sil, %r9b                                    #504.46
        shrb      $7, %sil                                      #503.63
        subsd     %xmm4, %xmm5                                  #505.95
        movsd     %xmm5, -64(%rsp)                              #505.66
        movsd     -72(%rsp), %xmm7                              #505.125
        movsd     -64(%rsp), %xmm6                              #505.135
        xorb      %sil, %dl                                     #503.63
        andb      $127, %r9b                                    #504.46
        shlb      $7, %dl                                       #504.46
        addsd     %xmm6, %xmm7                                  #505.135
        movsd     %xmm7, -56(%rsp)                              #505.106
        movsd     -64(%rsp), %xmm8                              #505.176
        addsd     %xmm8, %xmm1                                  #505.176
        movsd     %xmm1, -64(%rsp)                              #505.146
        orb       %dl, %r9b                                     #504.46
        movsd     -56(%rsp), %xmm1                              #505.216
        movb      %r9b, -25(%rsp)                               #504.46
        subsd     %xmm1, %xmm9                                  #505.216
        movsd     %xmm9, -56(%rsp)                              #505.187
        movsd     -64(%rsp), %xmm11                             #505.246
        movsd     -56(%rsp), %xmm10                             #505.256
        addsd     %xmm10, %xmm11                                #505.256
        movsd     %xmm11, -56(%rsp)                             #505.227
        movq      -72(%rsp), %rdx                               #505.275
        movsd     -56(%rsp), %xmm12                             #505.293
        movq      %rdx, -40(%rsp)                               #505.267
        addsd     %xmm12, %xmm0                                 #507.41
        movsd     -40(%rsp), %xmm13                             #508.33
        addsd     -32(%rsp), %xmm0                              #506.41
        movsd     %xmm0, -32(%rsp)                              #507.25
        addsd     %xmm0, %xmm13                                 #508.41
        movsd     %xmm13, -24(%rsp)                             #508.25
        movb      -17(%rsp), %r10b                              #509.46
        andb      $127, %r10b                                   #509.46
        orb       %cl, %r10b                                    #509.46
        movb      %r10b, -17(%rsp)                              #509.46
        movsd     -24(%rsp), %xmm0                              #510.27
        cvtsd2ss  %xmm0, %xmm0                                  #510.27
        movss     %xmm0, (%r8)                                  #510.27
        jmp       ..B7.33       # Prob 100%                     #510.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.18:                        # Preds ..B7.16
                                # Execution count [4.96e-02]
        movsd     -48(%rsp), %xmm12                             #514.44
        movb      %dl, %dil                                     #531.46
        movaps    %xmm12, %xmm7                                 #514.72
        mulsd     2000+_vmldAtanHATab(%rip), %xmm7              #514.72
        shlb      $7, %dil                                      #531.46
        shlb      $7, %cl                                       #538.46
        movsd     %xmm7, -72(%rsp)                              #514.25
        movsd     -72(%rsp), %xmm8                              #514.116
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #515.72
        movsd     1888+_vmldAtanHATab(%rip), %xmm7              #516.83
        mulsd     %xmm2, %xmm13                                 #515.72
        subsd     -48(%rsp), %xmm8                              #514.126
        movsd     %xmm8, -64(%rsp)                              #514.97
        movsd     -72(%rsp), %xmm10                             #514.156
        movsd     -64(%rsp), %xmm9                              #514.166
        subsd     %xmm9, %xmm10                                 #514.166
        movsd     %xmm10, -72(%rsp)                             #514.137
        movsd     -72(%rsp), %xmm11                             #514.206
        subsd     %xmm11, %xmm12                                #514.206
        movsd     %xmm12, -64(%rsp)                             #514.177
        movsd     -72(%rsp), %xmm6                              #514.225
        movsd     -64(%rsp), %xmm5                              #514.242
        movsd     %xmm13, -72(%rsp)                             #515.25
        movsd     -72(%rsp), %xmm14                             #515.116
        subsd     %xmm2, %xmm14                                 #515.126
        movsd     %xmm14, -64(%rsp)                             #515.97
        movsd     -72(%rsp), %xmm4                              #515.156
        movsd     -64(%rsp), %xmm15                             #515.166
        subsd     %xmm15, %xmm4                                 #515.166
        movsd     %xmm4, -72(%rsp)                              #515.137
        movsd     -72(%rsp), %xmm3                              #515.206
        movsd     1888+_vmldAtanHATab(%rip), %xmm4              #516.335
        subsd     %xmm3, %xmm2                                  #515.206
        movsd     %xmm2, -64(%rsp)                              #515.177
        movsd     -72(%rsp), %xmm12                             #515.225
        divsd     %xmm12, %xmm7                                 #516.83
        movsd     2000+_vmldAtanHATab(%rip), %xmm2              #516.137
        mulsd     %xmm7, %xmm2                                  #516.137
        movsd     -64(%rsp), %xmm14                             #515.242
        movsd     %xmm2, -64(%rsp)                              #516.92
        movsd     -64(%rsp), %xmm8                              #516.180
        subsd     %xmm7, %xmm8                                  #516.190
        movsd     %xmm8, -56(%rsp)                              #516.161
        movsd     -64(%rsp), %xmm10                             #516.219
        movsd     -56(%rsp), %xmm9                              #516.229
        subsd     %xmm9, %xmm10                                 #516.229
        movsd     %xmm10, -56(%rsp)                             #516.200
        movsd     -56(%rsp), %xmm11                             #516.266
        mulsd     %xmm11, %xmm12                                #516.266
        movsd     -56(%rsp), %xmm13                             #516.372
        subsd     %xmm12, %xmm4                                 #516.335
        mulsd     %xmm13, %xmm14                                #516.372
        movsd     %xmm14, -64(%rsp)                             #516.345
        movsd     -64(%rsp), %xmm15                             #516.411
        movsd     2000+_vmldAtanHATab(%rip), %xmm13             #526.77
        subsd     %xmm15, %xmm4                                 #516.411
        movsd     %xmm4, -64(%rsp)                              #516.383
        movsd     -64(%rsp), %xmm7                              #516.480
        movq      -56(%rsp), %rax                               #516.499
        movsd     -64(%rsp), %xmm2                              #516.535
        movsd     -56(%rsp), %xmm3                              #516.572
        movq      %rax, -40(%rsp)                               #516.491
        movsd     -40(%rsp), %xmm8                              #517.53
        movaps    %xmm8, %xmm9                                  #517.53
        addsd     1888+_vmldAtanHATab(%rip), %xmm7              #516.480
        mulsd     %xmm6, %xmm9                                  #517.53
        mulsd     %xmm5, %xmm8                                  #517.189
        mulsd     %xmm2, %xmm7                                  #516.535
        movsd     -16(%rsp), %xmm2                              #519.32
        mulsd     %xmm2, %xmm2                                  #519.38
        mulsd     %xmm3, %xmm7                                  #516.572
        movsd     1872+_vmldAtanHATab(%rip), %xmm3              #520.83
        mulsd     %xmm2, %xmm3                                  #520.83
        mulsd     %xmm7, %xmm6                                  #517.140
        mulsd     %xmm5, %xmm7                                  #517.92
        addsd     1864+_vmldAtanHATab(%rip), %xmm3              #520.107
        addsd     %xmm7, %xmm6                                  #517.140
        mulsd     %xmm2, %xmm3                                  #520.129
        addsd     %xmm8, %xmm6                                  #517.189
        addsd     1856+_vmldAtanHATab(%rip), %xmm3              #521.62
        mulsd     %xmm2, %xmm3                                  #521.84
        movaps    %xmm9, %xmm5                                  #518.54
        movsd     %xmm6, -72(%rsp)                              #517.151
        movsd     -72(%rsp), %xmm4                              #517.224
        addsd     1848+_vmldAtanHATab(%rip), %xmm3              #521.108
        addsd     %xmm4, %xmm5                                  #518.54
        mulsd     %xmm2, %xmm3                                  #521.130
        movsd     %xmm5, -72(%rsp)                              #518.25
        movsd     -72(%rsp), %xmm6                              #518.93
        movsd     2000+_vmldAtanHATab(%rip), %xmm5              #524.72
        subsd     %xmm6, %xmm9                                  #518.93
        addsd     1840+_vmldAtanHATab(%rip), %xmm3              #522.62
        addsd     %xmm4, %xmm9                                  #518.132
        mulsd     %xmm2, %xmm3                                  #522.84
        movsd     %xmm9, -64(%rsp)                              #518.104
        movsd     -72(%rsp), %xmm11                             #518.151
        mulsd     %xmm11, %xmm5                                 #524.72
        addsd     1832+_vmldAtanHATab(%rip), %xmm3              #522.108
        movsd     -64(%rsp), %xmm4                              #518.168
        movsd     %xmm5, -72(%rsp)                              #524.25
        movsd     -72(%rsp), %xmm7                              #524.116
        mulsd     %xmm2, %xmm3                                  #522.130
        subsd     %xmm11, %xmm7                                 #524.126
        movsd     %xmm7, -64(%rsp)                              #524.97
        movsd     -72(%rsp), %xmm8                              #524.156
        movsd     -64(%rsp), %xmm6                              #524.166
        addsd     1824+_vmldAtanHATab(%rip), %xmm3              #522.154
        subsd     %xmm6, %xmm8                                  #524.166
        mulsd     %xmm2, %xmm3                                  #523.51
        movsd     %xmm8, -72(%rsp)                              #524.137
        movsd     -72(%rsp), %xmm10                             #524.206
        mulsd     %xmm3, %xmm13                                 #526.77
        subsd     %xmm10, %xmm11                                #524.206
        movsd     %xmm11, -64(%rsp)                             #524.177
        movsd     -72(%rsp), %xmm2                              #524.225
        movsd     -64(%rsp), %xmm12                             #524.243
        movsd     %xmm13, -72(%rsp)                             #526.25
        addsd     %xmm12, %xmm4                                 #525.41
        movsd     -72(%rsp), %xmm14                             #526.121
        subsd     %xmm3, %xmm14                                 #526.131
        movsd     %xmm14, -64(%rsp)                             #526.102
        movsd     -72(%rsp), %xmm5                              #526.166
        movsd     -64(%rsp), %xmm15                             #526.176
        subsd     %xmm15, %xmm5                                 #526.176
        movsd     %xmm5, -72(%rsp)                              #526.147
        movsd     -72(%rsp), %xmm6                              #526.221
        subsd     %xmm6, %xmm3                                  #526.221
        movsd     %xmm3, -64(%rsp)                              #526.187
        movsd     -72(%rsp), %xmm6                              #526.247
        movsd     -64(%rsp), %xmm5                              #526.271
        movaps    %xmm6, %xmm12                                 #527.60
        movaps    %xmm5, %xmm3                                  #527.106
        mulsd     %xmm4, %xmm6                                  #527.161
        mulsd     %xmm4, %xmm3                                  #527.106
        mulsd     %xmm2, %xmm5                                  #527.217
        mulsd     %xmm2, %xmm12                                 #527.60
        addsd     %xmm3, %xmm6                                  #527.161
        movaps    %xmm12, %xmm7                                 #528.54
        movaps    %xmm12, %xmm8                                 #528.94
        addsd     %xmm5, %xmm6                                  #527.217
        addsd     %xmm2, %xmm7                                  #528.54
        movsd     %xmm6, -72(%rsp)                              #527.172
        movsd     -72(%rsp), %xmm5                              #527.252
        movsd     %xmm7, -72(%rsp)                              #528.25
        movsd     -72(%rsp), %xmm3                              #528.94
        subsd     %xmm3, %xmm8                                  #528.94
        movsd     %xmm8, -64(%rsp)                              #528.65
        movsd     -72(%rsp), %xmm10                             #528.124
        movsd     -64(%rsp), %xmm9                              #528.134
        addsd     %xmm9, %xmm10                                 #528.134
        movsd     %xmm10, -56(%rsp)                             #528.105
        movsd     -64(%rsp), %xmm11                             #528.174
        addsd     %xmm11, %xmm2                                 #528.174
        movsd     %xmm2, -64(%rsp)                              #528.145
        movsd     -56(%rsp), %xmm2                              #528.214
        subsd     %xmm2, %xmm12                                 #528.214
        movsd     %xmm12, -56(%rsp)                             #528.185
        movsd     -64(%rsp), %xmm14                             #528.244
        movsd     -56(%rsp), %xmm13                             #528.254
        addsd     %xmm13, %xmm14                                #528.254
        movsd     %xmm14, -56(%rsp)                             #528.225
        movq      -72(%rsp), %rsi                               #528.273
        movsd     -56(%rsp), %xmm15                             #528.290
        movq      %rsi, -40(%rsp)                               #528.265
        addsd     %xmm15, %xmm4                                 #529.41
        shrq      $56, %rsi                                     #531.46
        addsd     %xmm5, %xmm4                                  #530.41
        andb      $127, %sil                                    #531.46
        orb       %dil, %sil                                    #531.46
        movb      %sil, -33(%rsp)                               #531.46
        movsd     %xmm4, -32(%rsp)                              #530.25
        movaps    %xmm1, %xmm4                                  #534.54
        movsd     -40(%rsp), %xmm7                              #534.44
        movaps    %xmm7, %xmm2                                  #534.95
        addsd     %xmm7, %xmm4                                  #534.54
        movsd     %xmm4, -72(%rsp)                              #534.25
        movsd     -72(%rsp), %xmm4                              #534.95
        movb      -25(%rsp), %r9b                               #533.46
        movb      %r9b, %r10b                                   #533.46
        shrb      $7, %r9b                                      #532.63
        subsd     %xmm4, %xmm2                                  #534.95
        movsd     %xmm2, -64(%rsp)                              #534.66
        movsd     -72(%rsp), %xmm5                              #534.125
        movsd     -64(%rsp), %xmm3                              #534.135
        xorb      %r9b, %dl                                     #532.63
        andb      $127, %r10b                                   #533.46
        shlb      $7, %dl                                       #533.46
        addsd     %xmm3, %xmm5                                  #534.135
        movsd     %xmm5, -56(%rsp)                              #534.106
        movsd     -64(%rsp), %xmm6                              #534.176
        addsd     %xmm6, %xmm1                                  #534.176
        movsd     %xmm1, -64(%rsp)                              #534.146
        orb       %dl, %r10b                                    #533.46
        movsd     -56(%rsp), %xmm1                              #534.216
        movb      %r10b, -25(%rsp)                              #533.46
        subsd     %xmm1, %xmm7                                  #534.216
        movsd     %xmm7, -56(%rsp)                              #534.187
        movsd     -64(%rsp), %xmm2                              #534.246
        movsd     -56(%rsp), %xmm1                              #534.256
        addsd     %xmm1, %xmm2                                  #534.256
        movsd     %xmm2, -56(%rsp)                              #534.227
        movq      -72(%rsp), %rdx                               #534.275
        movsd     -56(%rsp), %xmm3                              #534.293
        movq      %rdx, -40(%rsp)                               #534.267
        addsd     %xmm3, %xmm0                                  #536.41
        movsd     -40(%rsp), %xmm4                              #537.33
        addsd     -32(%rsp), %xmm0                              #535.41
        movsd     %xmm0, -32(%rsp)                              #536.25
        addsd     %xmm0, %xmm4                                  #537.41
        movsd     %xmm4, -24(%rsp)                              #537.25
        movb      -17(%rsp), %r11b                              #538.46
        andb      $127, %r11b                                   #538.46
        orb       %cl, %r11b                                    #538.46
        movb      %r11b, -17(%rsp)                              #538.46
        movsd     -24(%rsp), %xmm0                              #539.27
        cvtsd2ss  %xmm0, %xmm0                                  #539.27
        movss     %xmm0, (%r8)                                  #539.27
        jmp       ..B7.33       # Prob 100%                     #539.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.19:                        # Preds ..B7.8
                                # Execution count [9.92e-02]
        cmpl      $74, %r9d                                     #542.39
        jge       ..B7.21       # Prob 50%                      #542.39
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl dil xmm1
..B7.20:                        # Preds ..B7.19
                                # Execution count [4.96e-02]
        movb      %dil, -41(%rsp)                               #415.34
        divsd     -48(%rsp), %xmm1                              #544.35
        movsd     1928+_vmldAtanHATab(%rip), %xmm0              #545.46
        shlb      $7, %cl                                       #547.42
        subsd     %xmm1, %xmm0                                  #545.69
        addsd     1920+_vmldAtanHATab(%rip), %xmm0              #546.54
        movsd     %xmm0, -24(%rsp)                              #546.21
        movb      -17(%rsp), %al                                #547.42
        andb      $127, %al                                     #547.42
        orb       %cl, %al                                      #547.42
        movb      %al, -17(%rsp)                                #547.42
        movsd     -24(%rsp), %xmm1                              #548.23
        cvtsd2ss  %xmm1, %xmm1                                  #548.23
        movss     %xmm1, (%r8)                                  #548.23
        jmp       ..B7.33       # Prob 100%                     #548.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.21:                        # Preds ..B7.37 ..B7.27 ..B7.28 ..B7.19
                                # Execution count [4.96e-02]
        movsd     1920+_vmldAtanHATab(%rip), %xmm0              #552.46
        shlb      $7, %cl                                       #553.42
        addsd     1928+_vmldAtanHATab(%rip), %xmm0              #552.86
        movsd     %xmm0, -24(%rsp)                              #552.21
        movb      -17(%rsp), %al                                #553.42
        andb      $127, %al                                     #553.42
        orb       %cl, %al                                      #553.42
        movb      %al, -17(%rsp)                                #553.42
        movsd     -24(%rsp), %xmm1                              #554.23
        cvtsd2ss  %xmm1, %xmm1                                  #554.23
        movss     %xmm1, (%r8)                                  #554.23
        jmp       ..B7.33       # Prob 100%                     #554.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.22:                        # Preds ..B7.7
                                # Execution count [1.98e-01]
        testb     %dl, %dl                                      #559.32
        jne       ..B7.32       # Prob 50%                      #559.32
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl sil dil
..B7.23:                        # Preds ..B7.22
                                # Execution count [9.92e-02]
        movb      %dil, -41(%rsp)                               #415.34
        pxor      %xmm0, %xmm0                                  #562.21
        movb      %sil, -33(%rsp)                               #417.34
        movsd     -48(%rsp), %xmm2                              #561.29
        divsd     -40(%rsp), %xmm2                              #561.36
        cvtsd2ss  %xmm2, %xmm0                                  #562.21
        movss     %xmm0, -8(%rsp)                               #562.21
        movzwl    -6(%rsp), %eax                                #563.47
        movsd     %xmm2, -24(%rsp)                              #561.21
        testl     $32640, %eax                                  #563.47
        je        ..B7.25       # Prob 50%                      #563.75
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl xmm2
..B7.24:                        # Preds ..B7.23
                                # Execution count [4.96e-02]
        movsd     1888+_vmldAtanHATab(%rip), %xmm0              #565.52
        shlb      $7, %cl                                       #567.46
        addsd     %xmm2, %xmm0                                  #565.75
        movsd     %xmm0, -72(%rsp)                              #565.25
        movsd     -72(%rsp), %xmm1                              #566.41
        mulsd     %xmm1, %xmm2                                  #566.41
        movsd     %xmm2, -24(%rsp)                              #566.25
        movb      -17(%rsp), %al                                #567.46
        andb      $127, %al                                     #567.46
        orb       %cl, %al                                      #567.46
        movb      %al, -17(%rsp)                                #567.46
        movsd     -24(%rsp), %xmm2                              #568.27
        cvtsd2ss  %xmm2, %xmm2                                  #568.27
        movss     %xmm2, (%r8)                                  #568.27
        jmp       ..B7.33       # Prob 100%                     #568.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.25:                        # Preds ..B7.23
                                # Execution count [4.96e-02]
        movsd     -24(%rsp), %xmm0                              #572.25
        cvtsd2ss  %xmm0, %xmm0                                  #572.25
        shlb      $7, %cl                                       #575.46
        movss     %xmm0, -8(%rsp)                               #572.25
        movss     -8(%rsp), %xmm2                               #573.35
        movss     -8(%rsp), %xmm1                               #573.45
        mulss     %xmm1, %xmm2                                  #573.45
        movss     %xmm2, -8(%rsp)                               #573.25
        movss     -8(%rsp), %xmm3                               #574.41
        cvtss2sd  %xmm3, %xmm3                                  #574.41
        addsd     -24(%rsp), %xmm3                              #574.41
        movsd     %xmm3, -24(%rsp)                              #574.25
        movb      -17(%rsp), %al                                #575.46
        andb      $127, %al                                     #575.46
        orb       %cl, %al                                      #575.46
        movb      %al, -17(%rsp)                                #575.46
        movsd     -24(%rsp), %xmm4                              #576.27
        cvtsd2ss  %xmm4, %xmm4                                  #576.27
        movss     %xmm4, (%r8)                                  #576.27
        jmp       ..B7.33       # Prob 100%                     #576.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.27:                        # Preds ..B7.6
                                # Execution count [1.32e-01]
        testl     %eax, %eax                                    #589.24
        jne       ..B7.21       # Prob 50%                      #589.24
                                # LOE rbx rbp r8 r12 r13 r14 r15 dl cl
..B7.28:                        # Preds ..B7.27
                                # Execution count [6.61e-02]
        testl     $8388607, -32(%rsp)                           #589.53
        jne       ..B7.21       # Prob 50%                      #589.79
                                # LOE rbx rbp r8 r12 r13 r14 r15 dl cl
..B7.30:                        # Preds ..B7.4 ..B7.28
                                # Execution count [7.72e-02]
        testb     %dl, %dl                                      #597.32
        jne       ..B7.32       # Prob 50%                      #597.32
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl
..B7.31:                        # Preds ..B7.40 ..B7.30
                                # Execution count [3.86e-02]
        shlb      $7, %cl                                       #600.42
        movq      1976+_vmldAtanHATab(%rip), %rax               #599.46
        movq      %rax, -24(%rsp)                               #599.21
        shrq      $56, %rax                                     #600.42
        andb      $127, %al                                     #600.42
        orb       %cl, %al                                      #600.42
        movb      %al, -17(%rsp)                                #600.42
        movsd     -24(%rsp), %xmm0                              #601.23
        cvtsd2ss  %xmm0, %xmm0                                  #601.23
        movss     %xmm0, (%r8)                                  #601.23
        jmp       ..B7.33       # Prob 100%                     #601.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.32:                        # Preds ..B7.40 ..B7.22 ..B7.30
                                # Execution count [3.86e-02]
        movsd     1936+_vmldAtanHATab(%rip), %xmm0              #605.46
        shlb      $7, %cl                                       #606.42
        addsd     1944+_vmldAtanHATab(%rip), %xmm0              #605.86
        movsd     %xmm0, -24(%rsp)                              #605.21
        movb      -17(%rsp), %al                                #606.42
        andb      $127, %al                                     #606.42
        orb       %cl, %al                                      #606.42
        movb      %al, -17(%rsp)                                #606.42
        movsd     -24(%rsp), %xmm1                              #607.23
        cvtsd2ss  %xmm1, %xmm1                                  #607.23
        movss     %xmm1, (%r8)                                  #607.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.33:                        # Preds ..B7.51 ..B7.18 ..B7.20 ..B7.21 ..B7.24
                                #       ..B7.25 ..B7.31 ..B7.32 ..B7.47 ..B7.44
                                #       ..B7.45
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #662.12
        ret                                                     #662.12
                                # LOE
..B7.34:                        # Preds ..B7.12
                                # Execution count [1.59e-02]: Infreq
        movsd     1984+_vmldAtanHATab(%rip), %xmm3              #449.57
        movl      $-1022, %eax                                  #450.25
        mulsd     %xmm3, %xmm4                                  #449.57
        movsd     %xmm4, -48(%rsp)                              #449.25
        jmp       ..B7.16       # Prob 100%                     #449.25
                                # LOE rbx rbp r8 r12 r13 r14 r15 eax dl cl xmm0 xmm1 xmm2 xmm5
..B7.35:                        # Preds ..B7.1 ..B7.2
                                # Execution count [2.94e-01]: Infreq
        cmpl      $2047, %eax                                   #614.20
        je        ..B7.48       # Prob 16%                      #614.20
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi r9d dl cl xmm2 xmm3
..B7.36:                        # Preds ..B7.35 ..B7.48
                                # Execution count [2.71e-01]: Infreq
        cmpl      $2047, %r9d                                   #615.23
        je        ..B7.46       # Prob 16%                      #615.23
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi dl cl xmm2 xmm3
..B7.37:                        # Preds ..B7.36 ..B7.46
                                # Execution count [2.49e-01]: Infreq
        movzwl    -26(%rsp), %eax                               #621.40
        andl      $32640, %eax                                  #621.40
        cmpl      $32640, %eax                                  #621.63
        jne       ..B7.21       # Prob 84%                      #621.63
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi dl cl
..B7.39:                        # Preds ..B7.37
                                # Execution count [3.99e-02]: Infreq
        cmpl      $255, %edi                                    #629.67
        je        ..B7.43       # Prob 16%                      #629.67
                                # LOE rbx rbp r8 r12 r13 r14 r15 dl cl
..B7.40:                        # Preds ..B7.39
                                # Execution count [3.35e-02]: Infreq
        testb     %dl, %dl                                      #631.36
        je        ..B7.31       # Prob 50%                      #631.36
        jmp       ..B7.32       # Prob 100%                     #631.36
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl
..B7.43:                        # Preds ..B7.39
                                # Execution count [6.38e-03]: Infreq
        testb     %dl, %dl                                      #646.36
        jne       ..B7.45       # Prob 50%                      #646.36
                                # LOE rbx rbp r8 r12 r13 r14 r15 cl
..B7.44:                        # Preds ..B7.43
                                # Execution count [3.19e-03]: Infreq
        movsd     1904+_vmldAtanHATab(%rip), %xmm0              #648.50
        shlb      $7, %cl                                       #649.46
        addsd     1912+_vmldAtanHATab(%rip), %xmm0              #648.90
        movsd     %xmm0, -24(%rsp)                              #648.25
        movb      -17(%rsp), %al                                #649.46
        andb      $127, %al                                     #649.46
        orb       %cl, %al                                      #649.46
        movb      %al, -17(%rsp)                                #649.46
        movsd     -24(%rsp), %xmm1                              #650.27
        cvtsd2ss  %xmm1, %xmm1                                  #650.27
        movss     %xmm1, (%r8)                                  #650.27
        jmp       ..B7.33       # Prob 100%                     #650.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.45:                        # Preds ..B7.43
                                # Execution count [3.19e-03]: Infreq
        movsd     1952+_vmldAtanHATab(%rip), %xmm0              #654.50
        shlb      $7, %cl                                       #655.46
        addsd     1960+_vmldAtanHATab(%rip), %xmm0              #654.90
        movsd     %xmm0, -24(%rsp)                              #654.25
        movb      -17(%rsp), %al                                #655.46
        andb      $127, %al                                     #655.46
        orb       %cl, %al                                      #655.46
        movb      %al, -17(%rsp)                                #655.46
        movsd     -24(%rsp), %xmm1                              #656.27
        cvtsd2ss  %xmm1, %xmm1                                  #656.27
        movss     %xmm1, (%r8)                                  #656.27
        jmp       ..B7.33       # Prob 100%                     #656.27
                                # LOE rbx rbp r12 r13 r14 r15
..B7.46:                        # Preds ..B7.36
                                # Execution count [4.33e-02]: Infreq
        testl     $8388607, -28(%rsp)                           #615.56
        je        ..B7.37       # Prob 50%                      #615.82
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi dl cl xmm2 xmm3
..B7.47:                        # Preds ..B7.48 ..B7.46
                                # Execution count [4.52e-02]: Infreq
        addss     %xmm2, %xmm3                                  #617.25
        movss     %xmm3, (%r8)                                  #617.15
        jmp       ..B7.33       # Prob 100%                     #617.15
                                # LOE rbx rbp r12 r13 r14 r15
..B7.48:                        # Preds ..B7.35
                                # Execution count [4.71e-02]: Infreq
        testl     $8388607, -32(%rsp)                           #614.53
        jne       ..B7.47       # Prob 50%                      #614.79
        jmp       ..B7.36       # Prob 100%                     #614.79
        .align    16,0x90
                                # LOE rbx rbp r8 r12 r13 r14 r15 edi r9d dl cl xmm2 xmm3
	.cfi_endproc
# mark_end;
	.type	__svml_satan2_ha_cout_rare_internal,@function
	.size	__svml_satan2_ha_cout_rare_internal,.-__svml_satan2_ha_cout_rare_internal
..LN__svml_satan2_ha_cout_rare_internal.6:
	.data
# -- End  __svml_satan2_ha_cout_rare_internal
	.data
	.align 64
	.align 64
ione:
	.long	1065353216
	.long	-1082130432
	.type	ione,@object
	.size	ione,8
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_satan2_ha_data_internal
	.globl __svml_satan2_ha_data_internal
__svml_satan2_ha_data_internal:
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	1078530016
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	3047930835
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1052770304
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1060655596
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1071644672
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
	.long	1076625408
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
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	1055744824
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	833369960
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	1061752792
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	875071576
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	1066252048
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	3034196096
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	1070141400
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	883460184
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	1034818345
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	3188674126
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	1045211707
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	3198855788
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
	.long	4294967295
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
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	2172649472
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
	.long	4219469824
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
	.type	__svml_satan2_ha_data_internal,@object
	.size	__svml_satan2_ha_data_internal,1984
	.align 32
_vmldAtanHATab:
	.long	3892314112
	.long	1069799150
	.long	2332892550
	.long	1039715405
	.long	1342177280
	.long	1070305495
	.long	270726690
	.long	1041535749
	.long	939524096
	.long	1070817911
	.long	2253973841
	.long	3188654726
	.long	3221225472
	.long	1071277294
	.long	3853927037
	.long	1043226911
	.long	2818572288
	.long	1071767563
	.long	2677759107
	.long	1044314101
	.long	3355443200
	.long	1072103591
	.long	1636578514
	.long	3191094734
	.long	1476395008
	.long	1072475260
	.long	1864703685
	.long	3188646936
	.long	805306368
	.long	1072747407
	.long	192551812
	.long	3192726267
	.long	2013265920
	.long	1072892781
	.long	2240369452
	.long	1043768538
	.long	0
	.long	1072999953
	.long	3665168337
	.long	3192705970
	.long	402653184
	.long	1073084787
	.long	1227953434
	.long	3192313277
	.long	2013265920
	.long	1073142981
	.long	3853283127
	.long	1045277487
	.long	805306368
	.long	1073187261
	.long	1676192264
	.long	3192868861
	.long	134217728
	.long	1073217000
	.long	4290763938
	.long	1042034855
	.long	671088640
	.long	1073239386
	.long	994303084
	.long	3189643768
	.long	402653184
	.long	1073254338
	.long	1878067156
	.long	1042652475
	.long	1610612736
	.long	1073265562
	.long	670314820
	.long	1045138554
	.long	3221225472
	.long	1073273048
	.long	691126919
	.long	3189987794
	.long	3489660928
	.long	1073278664
	.long	1618990832
	.long	3188194509
	.long	1207959552
	.long	1073282409
	.long	2198872939
	.long	1044806069
	.long	3489660928
	.long	1073285217
	.long	2633982383
	.long	1042307894
	.long	939524096
	.long	1073287090
	.long	1059367786
	.long	3189114230
	.long	2281701376
	.long	1073288494
	.long	3158525533
	.long	1044484961
	.long	3221225472
	.long	1073289430
	.long	286581777
	.long	1044893263
	.long	4026531840
	.long	1073290132
	.long	2000245215
	.long	3191647611
	.long	134217728
	.long	1073290601
	.long	4205071590
	.long	1045035927
	.long	536870912
	.long	1073290952
	.long	2334392229
	.long	1043447393
	.long	805306368
	.long	1073291186
	.long	2281458177
	.long	3188885569
	.long	3087007744
	.long	1073291361
	.long	691611507
	.long	1044733832
	.long	3221225472
	.long	1073291478
	.long	1816229550
	.long	1044363390
	.long	2281701376
	.long	1073291566
	.long	1993843750
	.long	3189837440
	.long	134217728
	.long	1073291625
	.long	3654754496
	.long	1044970837
	.long	4026531840
	.long	1073291668
	.long	3224300229
	.long	3191935390
	.long	805306368
	.long	1073291698
	.long	2988777976
	.long	3188950659
	.long	536870912
	.long	1073291720
	.long	1030371341
	.long	1043402665
	.long	3221225472
	.long	1073291734
	.long	1524463765
	.long	1044361356
	.long	3087007744
	.long	1073291745
	.long	2754295320
	.long	1044731036
	.long	134217728
	.long	1073291753
	.long	3099629057
	.long	1044970710
	.long	2281701376
	.long	1073291758
	.long	962914160
	.long	3189838838
	.long	805306368
	.long	1073291762
	.long	3543908206
	.long	3188950786
	.long	4026531840
	.long	1073291764
	.long	1849909620
	.long	3191935434
	.long	3221225472
	.long	1073291766
	.long	1641333636
	.long	1044361352
	.long	536870912
	.long	1073291768
	.long	1373968792
	.long	1043402654
	.long	134217728
	.long	1073291769
	.long	2033191599
	.long	1044970710
	.long	3087007744
	.long	1073291769
	.long	4117947437
	.long	1044731035
	.long	805306368
	.long	1073291770
	.long	315378368
	.long	3188950787
	.long	2281701376
	.long	1073291770
	.long	2428571750
	.long	3189838838
	.long	3221225472
	.long	1073291770
	.long	1608007466
	.long	1044361352
	.long	4026531840
	.long	1073291770
	.long	1895711420
	.long	3191935434
	.long	134217728
	.long	1073291771
	.long	2031108713
	.long	1044970710
	.long	536870912
	.long	1073291771
	.long	1362518342
	.long	1043402654
	.long	805306368
	.long	1073291771
	.long	317461253
	.long	3188950787
	.long	939524096
	.long	1073291771
	.long	4117231784
	.long	1044731035
	.long	1073741824
	.long	1073291771
	.long	1607942376
	.long	1044361352
	.long	1207959552
	.long	1073291771
	.long	2428929577
	.long	3189838838
	.long	1207959552
	.long	1073291771
	.long	2031104645
	.long	1044970710
	.long	1342177280
	.long	1073291771
	.long	1895722602
	.long	3191935434
	.long	1342177280
	.long	1073291771
	.long	317465322
	.long	3188950787
	.long	1342177280
	.long	1073291771
	.long	1362515546
	.long	1043402654
	.long	1342177280
	.long	1073291771
	.long	1607942248
	.long	1044361352
	.long	1342177280
	.long	1073291771
	.long	4117231610
	.long	1044731035
	.long	1342177280
	.long	1073291771
	.long	2031104637
	.long	1044970710
	.long	1342177280
	.long	1073291771
	.long	1540251232
	.long	1045150466
	.long	1342177280
	.long	1073291771
	.long	2644671394
	.long	1045270303
	.long	1342177280
	.long	1073291771
	.long	2399244691
	.long	1045360181
	.long	1342177280
	.long	1073291771
	.long	803971124
	.long	1045420100
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192879152
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192849193
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192826724
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192811744
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192800509
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192793019
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192787402
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192783657
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192780848
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192778976
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192777572
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192776635
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192775933
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192775465
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192775114
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774880
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774704
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774587
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774500
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774441
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774397
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774368
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774346
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774331
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774320
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774313
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774308
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774304
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774301
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774299
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774298
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774297
	.long	1476395008
	.long	1073291771
	.long	3613709523
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	177735686
	.long	3192774296
	.long	1476395008
	.long	1073291771
	.long	3490996172
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	2754716064
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	2263862659
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1895722605
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1650295902
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1466225875
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1343512524
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1251477510
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1190120835
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1144103328
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1113424990
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1090416237
	.long	3192774295
	.long	1476395008
	.long	1073291771
	.long	1075077068
	.long	3192774295
	.long	1431655765
	.long	3218429269
	.long	2576978363
	.long	1070176665
	.long	2453154343
	.long	3217180964
	.long	4189149139
	.long	1069314502
	.long	1775019125
	.long	3216459198
	.long	273199057
	.long	1068739452
	.long	874748308
	.long	3215993277
	.long	0
	.long	1069547520
	.long	0
	.long	1072693248
	.long	0
	.long	1073741824
	.long	1413754136
	.long	1072243195
	.long	856972295
	.long	1015129638
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	1413754136
	.long	1074340347
	.long	856972295
	.long	1017226790
	.long	2134057426
	.long	1073928572
	.long	1285458442
	.long	1016756537
	.long	0
	.long	3220176896
	.long	0
	.long	0
	.long	0
	.long	2144337920
	.long	0
	.long	1048576
	.long	33554432
	.long	1101004800
	.type	_vmldAtanHATab,@object
	.size	_vmldAtanHATab,2008
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
