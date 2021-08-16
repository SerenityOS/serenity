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
	.file "svml_d_asin.c"
	.text
..TXTST0:
.L_2__routine_start___svml_asin2_ha_e9_0:
# -- Begin  __svml_asin2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin2_ha_e9
# --- __svml_asin2_ha_e9(__m128d)
__svml_asin2_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #880.1
        .byte     15                                            #974.546
        .byte     30                                            #974.546
        .byte     250                                           #974.546
	.cfi_startproc
..___tag_value___svml_asin2_ha_e9.1:
..L2:
                                                          #880.1
        pushq     %rbp                                          #880.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #880.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #880.1
        subq      $192, %rsp                                    #880.1
        movaps    %xmm0, %xmm7                                  #880.1
        movups    __svml_dasin_ha_data_internal(%rip), %xmm5    #917.45
        movups    64+__svml_dasin_ha_data_internal(%rip), %xmm10 #918.45
        movaps    %xmm5, %xmm6                                  #920.8
        andps     %xmm7, %xmm6                                  #920.8
        movaps    %xmm10, %xmm8                                 #922.32
        mulpd     %xmm6, %xmm8                                  #922.32
        movaps    %xmm10, %xmm0                                 #922.8
        movaps    %xmm6, %xmm4                                  #923.9
        subpd     %xmm8, %xmm0                                  #922.8
        mulpd     %xmm6, %xmm4                                  #923.9
        cvtpd2ps  %xmm0, %xmm11                                 #929.546
        minpd     %xmm0, %xmm4                                  #924.9
        movlhps   %xmm11, %xmm11                                #929.530
        movaps    %xmm0, %xmm8                                  #929.717
        rsqrtps   %xmm11, %xmm12                                #929.517
        cmpltpd   128+__svml_dasin_ha_data_internal(%rip), %xmm8 #929.717
        addpd     %xmm0, %xmm0                                  #929.810
        cvtps2pd  %xmm12, %xmm13                                #929.504
        movups    320+__svml_dasin_ha_data_internal(%rip), %xmm2 #929.1113
        andnps    %xmm13, %xmm8                                 #929.760
        andps     %xmm0, %xmm2                                  #929.1113
        movaps    %xmm8, %xmm14                                 #929.1275
        subpd     %xmm2, %xmm0                                  #929.1159
        mulpd     %xmm8, %xmm2                                  #929.1197
        mulpd     %xmm8, %xmm0                                  #929.1235
        mulpd     %xmm2, %xmm14                                 #929.1275
        mulpd     %xmm0, %xmm8                                  #929.1315
        subpd     384+__svml_dasin_ha_data_internal(%rip), %xmm14 #929.1356
        addpd     %xmm14, %xmm8                                 #929.1401
        movups    448+__svml_dasin_ha_data_internal(%rip), %xmm15 #929.1869
        movaps    %xmm2, %xmm1                                  #929.1444
        mulpd     %xmm8, %xmm15                                 #929.1869
        addpd     %xmm0, %xmm1                                  #929.1444
        addpd     512+__svml_dasin_ha_data_internal(%rip), %xmm15 #929.1857
        mulpd     %xmm8, %xmm1                                  #929.1927
        mulpd     %xmm8, %xmm15                                 #929.1985
        addpd     576+__svml_dasin_ha_data_internal(%rip), %xmm15 #929.1973
        mulpd     %xmm15, %xmm8                                 #929.2060
        addpd     640+__svml_dasin_ha_data_internal(%rip), %xmm8 #929.2048
        mulpd     %xmm8, %xmm1                                  #929.2130
        subpd     %xmm0, %xmm1                                  #929.2118
        movups    704+__svml_dasin_ha_data_internal(%rip), %xmm0 #934.24
        movaps    %xmm4, %xmm13                                 #936.13
        mulpd     %xmm4, %xmm0                                  #934.24
        mulpd     %xmm4, %xmm13                                 #936.13
        addpd     768+__svml_dasin_ha_data_internal(%rip), %xmm0 #934.12
        movups    256+__svml_dasin_ha_data_internal(%rip), %xmm9 #925.13
        movaps    %xmm6, %xmm3                                  #928.14
        cmpltpd   %xmm6, %xmm9                                  #925.13
        cmpnltpd  %xmm10, %xmm3                                 #928.14
        mulpd     %xmm13, %xmm0                                 #943.24
        movmskpd  %xmm9, %edx                                   #927.40
        movups    832+__svml_dasin_ha_data_internal(%rip), %xmm11 #935.27
        movaps    %xmm13, %xmm10                                #945.13
        movups    960+__svml_dasin_ha_data_internal(%rip), %xmm9 #939.26
        movaps    %xmm1, %xmm8                                  #959.12
        mulpd     %xmm4, %xmm11                                 #935.27
        mulpd     %xmm4, %xmm9                                  #939.26
        addpd     896+__svml_dasin_ha_data_internal(%rip), %xmm11 #935.15
        addpd     1024+__svml_dasin_ha_data_internal(%rip), %xmm9 #939.14
        subpd     %xmm2, %xmm8                                  #959.12
        mulpd     %xmm13, %xmm10                                #945.13
        mulpd     %xmm13, %xmm9                                 #944.26
        addpd     %xmm0, %xmm11                                 #943.12
        movups    1088+__svml_dasin_ha_data_internal(%rip), %xmm12 #942.26
        andps     %xmm3, %xmm8                                  #964.12
        mulpd     %xmm4, %xmm12                                 #942.26
        mulpd     %xmm10, %xmm11                                #949.24
        addpd     1152+__svml_dasin_ha_data_internal(%rip), %xmm12 #942.14
        addpd     %xmm9, %xmm12                                 #944.14
        movups    1216+__svml_dasin_ha_data_internal(%rip), %xmm14 #948.26
        andnps    %xmm7, %xmm5                                  #921.12
        mulpd     %xmm4, %xmm14                                 #948.26
        addpd     %xmm11, %xmm12                                #949.12
        addpd     1280+__svml_dasin_ha_data_internal(%rip), %xmm14 #948.14
        mulpd     %xmm12, %xmm13                                #950.24
        addpd     %xmm13, %xmm14                                #950.12
        mulpd     %xmm4, %xmm14                                 #953.24
        addpd     1344+__svml_dasin_ha_data_internal(%rip), %xmm14 #953.12
        movups    1472+__svml_dasin_ha_data_internal(%rip), %xmm15 #956.46
        movaps    %xmm15, %xmm0                                 #958.15
        subpd     %xmm2, %xmm0                                  #958.15
        mulpd     %xmm4, %xmm14                                 #954.24
        subpd     %xmm0, %xmm15                                 #960.14
        addpd     1408+__svml_dasin_ha_data_internal(%rip), %xmm14 #954.12
        subpd     %xmm15, %xmm2                                 #962.14
        mulpd     %xmm14, %xmm4                                 #955.12
        movups    1536+__svml_dasin_ha_data_internal(%rip), %xmm9 #961.14
        andps     %xmm3, %xmm0                                  #966.15
        addpd     %xmm1, %xmm9                                  #961.14
        subpd     %xmm2, %xmm9                                  #963.14
        andps     %xmm3, %xmm9                                  #965.14
        andnps    %xmm6, %xmm3                                  #967.12
        orps      %xmm3, %xmm8                                  #968.12
        orps      %xmm3, %xmm0                                  #969.15
        mulpd     %xmm4, %xmm8                                  #970.24
        addpd     %xmm8, %xmm9                                  #970.12
        addpd     %xmm9, %xmm0                                  #971.15
        pxor      %xmm5, %xmm0                                  #972.14
        testl     %edx, %edx                                    #974.52
        jne       ..B1.3        # Prob 5%                       #974.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B1.2:                         # Preds ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #977.12
        popq      %rbp                                          #977.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #977.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #974.197
        movups    %xmm0, 128(%rsp)                              #974.270
                                # LOE rbx r12 r13 r14 r15 edx
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #974.454
        movq      %r12, 8(%rsp)                                 #974.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #974.454
        movq      %r13, (%rsp)                                  #974.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #974.454
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #974.517
        jc        ..B1.10       # Prob 5%                       #974.517
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #974.470
        cmpl      $2, %r12d                                     #974.465
        jl        ..B1.7        # Prob 82%                      #974.465
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #974.674
        jmp       ..B1.2        # Prob 100%                     #974.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #974.546
        lea       128(%rsp,%r12,8), %rsi                        #974.546
..___tag_value___svml_asin2_ha_e9.19:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #974.546
..___tag_value___svml_asin2_ha_e9.20:
        jmp       ..B1.8        # Prob 100%                     #974.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asin2_ha_e9,@function
	.size	__svml_asin2_ha_e9,.-__svml_asin2_ha_e9
..LN__svml_asin2_ha_e9.0:
	.data
# -- End  __svml_asin2_ha_e9
	.text
.L_2__routine_start___svml_asin2_ha_l9_1:
# -- Begin  __svml_asin2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin2_ha_l9
# --- __svml_asin2_ha_l9(__m128d)
__svml_asin2_ha_l9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #982.1
        .byte     15                                            #1076.546
        .byte     30                                            #1076.546
        .byte     250                                           #1076.546
	.cfi_startproc
..___tag_value___svml_asin2_ha_l9.22:
..L23:
                                                         #982.1
        pushq     %rbp                                          #982.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #982.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #982.1
        subq      $192, %rsp                                    #982.1
        vmovapd   %xmm0, %xmm7                                  #982.1
        vmovupd   __svml_dasin_ha_data_internal(%rip), %xmm8    #1019.45
        vmovupd   64+__svml_dasin_ha_data_internal(%rip), %xmm12 #1020.45
        vandpd    %xmm7, %xmm8, %xmm6                           #1022.8
        vmovapd   %xmm12, %xmm1                                 #1024.8
        vandnpd   %xmm7, %xmm8, %xmm5                           #1023.12
        vfnmadd231pd %xmm6, %xmm12, %xmm1                       #1024.8
        vmovupd   256+__svml_dasin_ha_data_internal(%rip), %xmm10 #1021.41
        vmulpd    %xmm6, %xmm6, %xmm9                           #1025.9
        vcvtpd2ps %xmm1, %xmm13                                 #1031.546
        vcmpltpd  %xmm6, %xmm10, %xmm11                         #1027.13
        vcmpltpd  128+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm0 #1031.717
        vminpd    %xmm1, %xmm9, %xmm4                           #1026.9
        vcmpnltpd %xmm12, %xmm6, %xmm3                          #1030.14
        vmovmskpd %xmm11, %edx                                  #1029.40
        vaddpd    %xmm1, %xmm1, %xmm11                          #1031.810
        vmovlhps  %xmm13, %xmm13, %xmm14                        #1031.530
        vrsqrtps  %xmm14, %xmm15                                #1031.517
        vcvtps2pd %xmm15, %xmm2                                 #1031.504
        vmulpd    %xmm4, %xmm4, %xmm15                          #1038.13
        vandnpd   %xmm2, %xmm0, %xmm8                           #1031.760
        vmulpd    %xmm8, %xmm8, %xmm9                           #1031.1068
        vmulpd    %xmm11, %xmm8, %xmm2                          #1031.1163
        vfmsub213pd 384+__svml_dasin_ha_data_internal(%rip), %xmm11, %xmm9 #1031.1110
        vmovupd   832+__svml_dasin_ha_data_internal(%rip), %xmm12 #1037.15
        vmovupd   960+__svml_dasin_ha_data_internal(%rip), %xmm0 #1041.14
        vmovupd   1088+__svml_dasin_ha_data_internal(%rip), %xmm13 #1044.14
        vmovupd   448+__svml_dasin_ha_data_internal(%rip), %xmm1 #1031.1644
        vfmsub213pd %xmm2, %xmm8, %xmm11                        #1031.1202
        vmovupd   704+__svml_dasin_ha_data_internal(%rip), %xmm8 #1036.12
        vfmadd213pd 768+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm8 #1036.12
        vfmadd213pd 896+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm12 #1037.15
        vfmadd213pd 1024+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm0 #1041.14
        vfmadd213pd 1152+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm13 #1044.14
        vfmadd213pd 512+__svml_dasin_ha_data_internal(%rip), %xmm9, %xmm1 #1031.1644
        vmulpd    %xmm15, %xmm15, %xmm14                        #1047.13
        vfmadd213pd %xmm12, %xmm15, %xmm8                       #1045.12
        vfmadd213pd %xmm13, %xmm15, %xmm0                       #1046.14
        vmovupd   1216+__svml_dasin_ha_data_internal(%rip), %xmm12 #1050.14
        vfmadd213pd 1280+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm12 #1050.14
        vfmadd213pd 576+__svml_dasin_ha_data_internal(%rip), %xmm9, %xmm1 #1031.1748
        vmulpd    %xmm2, %xmm9, %xmm10                          #1031.1702
        vfmadd213pd %xmm0, %xmm14, %xmm8                        #1051.12
        vfmadd213pd 640+__svml_dasin_ha_data_internal(%rip), %xmm9, %xmm1 #1031.1811
        vfmadd213pd %xmm12, %xmm15, %xmm8                       #1052.12
        vfmsub213pd %xmm11, %xmm10, %xmm1                       #1031.1869
        vfmadd213pd 1344+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm8 #1055.12
        vfmadd213pd 1408+__svml_dasin_ha_data_internal(%rip), %xmm4, %xmm8 #1056.12
        vmulpd    %xmm8, %xmm4, %xmm0                           #1057.12
        vsubpd    %xmm2, %xmm1, %xmm8                           #1061.12
        vaddpd    1536+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm1 #1063.14
        vmovupd   1472+__svml_dasin_ha_data_internal(%rip), %xmm4 #1058.46
        vandpd    %xmm3, %xmm8, %xmm11                          #1066.12
        vsubpd    %xmm2, %xmm4, %xmm10                          #1060.15
        vsubpd    %xmm10, %xmm4, %xmm4                          #1062.14
        vsubpd    %xmm4, %xmm2, %xmm2                           #1064.14
        vsubpd    %xmm2, %xmm1, %xmm9                           #1065.14
        vandpd    %xmm3, %xmm9, %xmm13                          #1067.14
        vandpd    %xmm3, %xmm10, %xmm12                         #1068.15
        vandnpd   %xmm6, %xmm3, %xmm3                           #1069.12
        vorpd     %xmm3, %xmm11, %xmm6                          #1070.12
        vorpd     %xmm3, %xmm12, %xmm1                          #1071.15
        vfmadd213pd %xmm13, %xmm6, %xmm0                        #1072.12
        vaddpd    %xmm0, %xmm1, %xmm0                           #1073.15
        vxorpd    %xmm5, %xmm0, %xmm0                           #1074.14
        testl     %edx, %edx                                    #1076.52
        jne       ..B2.3        # Prob 5%                       #1076.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B2.2:                         # Preds ..B2.3 ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1079.12
        popq      %rbp                                          #1079.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1079.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm7, 64(%rsp)                               #1076.197
        vmovupd   %xmm0, 128(%rsp)                              #1076.270
        je        ..B2.2        # Prob 95%                      #1076.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1076.454
        movq      %r12, 8(%rsp)                                 #1076.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1076.454
        movq      %r13, (%rsp)                                  #1076.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1076.454
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1076.517
        jc        ..B2.10       # Prob 5%                       #1076.517
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1076.470
        cmpl      $2, %r12d                                     #1076.465
        jl        ..B2.7        # Prob 82%                      #1076.465
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1076.674
        jmp       ..B2.2        # Prob 100%                     #1076.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1076.546
        lea       128(%rsp,%r12,8), %rsi                        #1076.546
..___tag_value___svml_asin2_ha_l9.40:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1076.546
..___tag_value___svml_asin2_ha_l9.41:
        jmp       ..B2.8        # Prob 100%                     #1076.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asin2_ha_l9,@function
	.size	__svml_asin2_ha_l9,.-__svml_asin2_ha_l9
..LN__svml_asin2_ha_l9.1:
	.data
# -- End  __svml_asin2_ha_l9
	.text
.L_2__routine_start___svml_asin4_ha_l9_2:
# -- Begin  __svml_asin4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin4_ha_l9
# --- __svml_asin4_ha_l9(__m256d)
__svml_asin4_ha_l9:
# parameter 1: %ymm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1084.1
        .byte     15                                            #1178.552
        .byte     30                                            #1178.552
        .byte     250                                           #1178.552
	.cfi_startproc
..___tag_value___svml_asin4_ha_l9.43:
..L44:
                                                         #1084.1
        pushq     %rbp                                          #1084.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1084.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1084.1
        subq      $192, %rsp                                    #1084.1
        vmovupd   __svml_dasin_ha_data_internal(%rip), %ymm8    #1121.48
        vmovupd   64+__svml_dasin_ha_data_internal(%rip), %ymm12 #1122.48
        vmovupd   256+__svml_dasin_ha_data_internal(%rip), %ymm10 #1123.44
        vmovapd   %ymm0, %ymm7                                  #1084.1
        vandpd    %ymm7, %ymm8, %ymm6                           #1124.8
        vmovapd   %ymm12, %ymm2                                 #1126.8
        vfnmadd231pd %ymm6, %ymm12, %ymm2                       #1126.8
        vmulpd    %ymm6, %ymm6, %ymm9                           #1127.9
        vcmplt_oqpd %ymm6, %ymm10, %ymm11                       #1129.13
        vcmplt_oqpd 128+__svml_dasin_ha_data_internal(%rip), %ymm2, %ymm15 #1133.666
        vaddpd    %ymm2, %ymm2, %ymm10                          #1133.775
        vminpd    %ymm2, %ymm9, %ymm4                           #1128.9
        vcmpnlt_uqpd %ymm12, %ymm6, %ymm3                       #1132.14
        vcvtpd2ps %ymm2, %xmm13                                 #1133.533
        vmovupd   1088+__svml_dasin_ha_data_internal(%rip), %ymm12 #1146.14
        vrsqrtps  %xmm13, %xmm14                                #1133.520
        vfmadd213pd 1152+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm12 #1146.14
        vcvtps2pd %xmm14, %ymm0                                 #1133.504
        vmovupd   960+__svml_dasin_ha_data_internal(%rip), %ymm14 #1143.14
        vandnpd   %ymm0, %ymm15, %ymm1                          #1133.722
        vandnpd   %ymm7, %ymm8, %ymm5                           #1125.12
        vmulpd    %ymm1, %ymm1, %ymm8                           #1133.1042
        vmulpd    %ymm10, %ymm1, %ymm2                          #1133.1143
        vfmadd213pd 1024+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm14 #1143.14
        vmulpd    %ymm4, %ymm4, %ymm15                          #1140.13
        vmovupd   1216+__svml_dasin_ha_data_internal(%rip), %ymm0 #1152.14
        vfmsub213pd 384+__svml_dasin_ha_data_internal(%rip), %ymm10, %ymm8 #1133.1087
        vfmsub213pd %ymm2, %ymm1, %ymm10                        #1133.1185
        vmovupd   448+__svml_dasin_ha_data_internal(%rip), %ymm1 #1133.1642
        vfmadd213pd %ymm12, %ymm15, %ymm14                      #1148.14
        vmulpd    %ymm15, %ymm15, %ymm13                        #1149.13
        vmulpd    %ymm2, %ymm8, %ymm9                           #1133.1703
        vfmadd213pd 1280+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm0 #1152.14
        vfmadd213pd 512+__svml_dasin_ha_data_internal(%rip), %ymm8, %ymm1 #1133.1642
        vfmadd213pd 576+__svml_dasin_ha_data_internal(%rip), %ymm8, %ymm1 #1133.1752
        vfmadd213pd 640+__svml_dasin_ha_data_internal(%rip), %ymm8, %ymm1 #1133.1818
        vmovupd   704+__svml_dasin_ha_data_internal(%rip), %ymm8 #1138.12
        vfmsub213pd %ymm10, %ymm9, %ymm1                        #1133.1879
        vfmadd213pd 768+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm8 #1138.12
        vmovmskpd %ymm11, %edx                                  #1131.40
        vmovupd   832+__svml_dasin_ha_data_internal(%rip), %ymm11 #1139.15
        vfmadd213pd 896+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm11 #1139.15
        vfmadd213pd %ymm11, %ymm15, %ymm8                       #1147.12
        vfmadd213pd %ymm14, %ymm13, %ymm8                       #1153.12
        vfmadd213pd %ymm0, %ymm15, %ymm8                        #1154.12
        vfmadd213pd 1344+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm8 #1157.12
        vfmadd213pd 1408+__svml_dasin_ha_data_internal(%rip), %ymm4, %ymm8 #1158.12
        vmulpd    %ymm8, %ymm4, %ymm0                           #1159.12
        vmovupd   1472+__svml_dasin_ha_data_internal(%rip), %ymm4 #1160.49
        vsubpd    %ymm2, %ymm1, %ymm8                           #1163.12
        vaddpd    1536+__svml_dasin_ha_data_internal(%rip), %ymm1, %ymm1 #1165.14
        vsubpd    %ymm2, %ymm4, %ymm10                          #1162.15
        vsubpd    %ymm10, %ymm4, %ymm4                          #1164.14
        vsubpd    %ymm4, %ymm2, %ymm2                           #1166.14
        vsubpd    %ymm2, %ymm1, %ymm9                           #1167.14
        vandpd    %ymm3, %ymm8, %ymm11                          #1168.12
        vandpd    %ymm3, %ymm9, %ymm13                          #1169.14
        vandpd    %ymm3, %ymm10, %ymm12                         #1170.15
        vandnpd   %ymm6, %ymm3, %ymm3                           #1171.12
        vorpd     %ymm3, %ymm11, %ymm6                          #1172.12
        vfmadd213pd %ymm13, %ymm6, %ymm0                        #1174.12
        vorpd     %ymm3, %ymm12, %ymm1                          #1173.15
        vaddpd    %ymm0, %ymm1, %ymm0                           #1175.15
        vxorpd    %ymm5, %ymm0, %ymm0                           #1176.14
        testl     %edx, %edx                                    #1178.52
        jne       ..B3.3        # Prob 5%                       #1178.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm7
..B3.2:                         # Preds ..B3.3 ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1181.12
        popq      %rbp                                          #1181.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1181.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm7, 64(%rsp)                               #1178.200
        vmovupd   %ymm0, 128(%rsp)                              #1178.276
        je        ..B3.2        # Prob 95%                      #1178.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1178.460
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
        btl       %r12d, %r13d                                  #1178.523
        jc        ..B3.10       # Prob 5%                       #1178.523
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1178.476
        cmpl      $4, %r12d                                     #1178.471
        jl        ..B3.7        # Prob 82%                      #1178.471
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1178.683
        jmp       ..B3.2        # Prob 100%                     #1178.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1178.552
        lea       128(%rsp,%r12,8), %rsi                        #1178.552
..___tag_value___svml_asin4_ha_l9.61:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1178.552
..___tag_value___svml_asin4_ha_l9.62:
        jmp       ..B3.8        # Prob 100%                     #1178.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asin4_ha_l9,@function
	.size	__svml_asin4_ha_l9,.-__svml_asin4_ha_l9
..LN__svml_asin4_ha_l9.2:
	.data
# -- End  __svml_asin4_ha_l9
	.text
.L_2__routine_start___svml_asin1_ha_ex_3:
# -- Begin  __svml_asin1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin1_ha_ex
# --- __svml_asin1_ha_ex(__m128d)
__svml_asin1_ha_ex:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1186.1
        .byte     15                                            #1280.546
        .byte     30                                            #1280.546
        .byte     250                                           #1280.546
	.cfi_startproc
..___tag_value___svml_asin1_ha_ex.64:
..L65:
                                                         #1186.1
        pushq     %rbp                                          #1186.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1186.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1186.1
        subq      $128, %rsp                                    #1186.1
        movaps    %xmm0, %xmm8                                  #1186.1
        movsd     __svml_dasin_ha_data_internal(%rip), %xmm6    #1223.14
        movaps    %xmm6, %xmm7                                  #1226.8
        andnps    %xmm8, %xmm6                                  #1227.12
        andps     %xmm8, %xmm7                                  #1226.8
        movsd     64+__svml_dasin_ha_data_internal(%rip), %xmm12 #1224.14
        movaps    %xmm7, %xmm9                                  #1228.32
        mulsd     %xmm12, %xmm9                                 #1228.32
        movaps    %xmm12, %xmm0                                 #1228.8
        movsd     320+__svml_dasin_ha_data_internal(%rip), %xmm3 #1235.1113
        movaps    %xmm7, %xmm5                                  #1229.9
        mulsd     %xmm7, %xmm5                                  #1229.9
        subsd     %xmm9, %xmm0                                  #1228.8
        cvtpd2ps  %xmm0, %xmm13                                 #1235.546
        shufps    $0, %xmm13, %xmm13                            #1235.530
        movaps    %xmm0, %xmm10                                 #1230.72
        rsqrtps   %xmm13, %xmm14                                #1235.517
        cvtps2pd  %xmm14, %xmm15                                #1235.504
        movaps    %xmm0, %xmm2                                  #1235.717
        movaps    %xmm7, %xmm4                                  #1234.14
        unpcklpd  %xmm0, %xmm10                                 #1230.72
        cmpltsd   128+__svml_dasin_ha_data_internal(%rip), %xmm2 #1235.717
        addsd     %xmm0, %xmm0                                  #1235.810
        cmpnltsd  %xmm12, %xmm4                                 #1234.14
        andnps    %xmm15, %xmm2                                 #1235.760
        andps     %xmm0, %xmm3                                  #1235.1113
        unpcklpd  %xmm5, %xmm5                                  #1230.21
        subsd     %xmm3, %xmm0                                  #1235.1159
        mulsd     %xmm2, %xmm3                                  #1235.1197
        minpd     %xmm10, %xmm5                                 #1230.9
        mulsd     %xmm2, %xmm0                                  #1235.1235
        movaps    %xmm3, %xmm9                                  #1235.1275
        movaps    %xmm0, %xmm1                                  #1235.1315
        mulsd     %xmm2, %xmm9                                  #1235.1275
        movaps    %xmm3, %xmm10                                 #1235.1444
        addsd     %xmm0, %xmm10                                 #1235.1444
        mulsd     %xmm2, %xmm1                                  #1235.1315
        subsd     384+__svml_dasin_ha_data_internal(%rip), %xmm9 #1235.1356
        movsd     448+__svml_dasin_ha_data_internal(%rip), %xmm2 #1235.1869
        movaps    %xmm5, %xmm14                                 #1242.13
        movsd     256+__svml_dasin_ha_data_internal(%rip), %xmm11 #1231.13
        movaps    %xmm5, %xmm13                                 #1245.26
        mulsd     %xmm5, %xmm14                                 #1242.13
        addsd     %xmm1, %xmm9                                  #1235.1401
        cmpltsd   %xmm7, %xmm11                                 #1231.13
        mulsd     960+__svml_dasin_ha_data_internal(%rip), %xmm13 #1245.26
        mulsd     %xmm9, %xmm2                                  #1235.1869
        mulsd     %xmm9, %xmm10                                 #1235.1927
        addsd     1024+__svml_dasin_ha_data_internal(%rip), %xmm13 #1245.14
        movmskpd  %xmm11, %eax                                  #1233.40
        addsd     512+__svml_dasin_ha_data_internal(%rip), %xmm2 #1235.1857
        mulsd     %xmm14, %xmm13                                #1250.26
        mulsd     %xmm9, %xmm2                                  #1235.1985
        movaps    %xmm5, %xmm1                                  #1240.24
        movaps    %xmm5, %xmm11                                 #1248.26
        mulsd     704+__svml_dasin_ha_data_internal(%rip), %xmm1 #1240.24
        movaps    %xmm14, %xmm12                                #1251.13
        addsd     576+__svml_dasin_ha_data_internal(%rip), %xmm2 #1235.1973
        mulsd     1088+__svml_dasin_ha_data_internal(%rip), %xmm11 #1248.26
        mulsd     %xmm14, %xmm12                                #1251.13
        addsd     768+__svml_dasin_ha_data_internal(%rip), %xmm1 #1240.12
        mulsd     %xmm9, %xmm2                                  #1235.2060
        addsd     1152+__svml_dasin_ha_data_internal(%rip), %xmm11 #1248.14
        mulsd     %xmm14, %xmm1                                 #1249.24
        addsd     640+__svml_dasin_ha_data_internal(%rip), %xmm2 #1235.2048
        addsd     %xmm11, %xmm13                                #1250.14
        mulsd     %xmm10, %xmm2                                 #1235.2130
        movsd     1536+__svml_dasin_ha_data_internal(%rip), %xmm10 #1267.14
        subsd     %xmm0, %xmm2                                  #1235.2118
        movaps    %xmm5, %xmm0                                  #1241.27
        movaps    %xmm2, %xmm9                                  #1265.12
        mulsd     832+__svml_dasin_ha_data_internal(%rip), %xmm0 #1241.27
        subsd     %xmm3, %xmm9                                  #1265.12
        addsd     %xmm2, %xmm10                                 #1267.14
        addsd     896+__svml_dasin_ha_data_internal(%rip), %xmm0 #1241.15
        andps     %xmm4, %xmm9                                  #1270.12
        addsd     %xmm0, %xmm1                                  #1249.12
        mulsd     %xmm12, %xmm1                                 #1255.24
        movaps    %xmm5, %xmm0                                  #1254.26
        mulsd     1216+__svml_dasin_ha_data_internal(%rip), %xmm0 #1254.26
        addsd     %xmm13, %xmm1                                 #1255.12
        addsd     1280+__svml_dasin_ha_data_internal(%rip), %xmm0 #1254.14
        mulsd     %xmm14, %xmm1                                 #1256.24
        addsd     %xmm0, %xmm1                                  #1256.12
        mulsd     %xmm5, %xmm1                                  #1259.24
        addsd     1344+__svml_dasin_ha_data_internal(%rip), %xmm1 #1259.12
        mulsd     %xmm5, %xmm1                                  #1260.24
        addsd     1408+__svml_dasin_ha_data_internal(%rip), %xmm1 #1260.12
        mulsd     %xmm5, %xmm1                                  #1261.12
        movsd     1472+__svml_dasin_ha_data_internal(%rip), %xmm5 #1262.15
        movaps    %xmm5, %xmm0                                  #1264.15
        subsd     %xmm3, %xmm0                                  #1264.15
        subsd     %xmm0, %xmm5                                  #1266.14
        andps     %xmm4, %xmm0                                  #1272.15
        subsd     %xmm5, %xmm3                                  #1268.14
        subsd     %xmm3, %xmm10                                 #1269.14
        andps     %xmm4, %xmm10                                 #1271.14
        andnps    %xmm7, %xmm4                                  #1273.12
        orps      %xmm4, %xmm9                                  #1274.12
        orps      %xmm4, %xmm0                                  #1275.15
        mulsd     %xmm9, %xmm1                                  #1276.24
        addsd     %xmm10, %xmm1                                 #1276.12
        addsd     %xmm1, %xmm0                                  #1277.15
        pxor      %xmm6, %xmm0                                  #1278.14
        andl      $1, %eax                                      #1233.91
        jne       ..B4.3        # Prob 5%                       #1280.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B4.2:                         # Preds ..B4.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1283.12
        popq      %rbp                                          #1283.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1283.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.3:                         # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm8, (%rsp)                                 #1280.160
        movsd     %xmm0, 64(%rsp)                               #1280.233
        jne       ..B4.6        # Prob 5%                       #1280.374
                                # LOE rbx r12 r13 r14 r15
..B4.4:                         # Preds ..B4.6 ..B4.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1280.626
        movq      %rbp, %rsp                                    #1280.626
        popq      %rbp                                          #1280.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1280.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B4.6:                         # Preds ..B4.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1280.546
        lea       64(%rsp), %rsi                                #1280.546
..___tag_value___svml_asin1_ha_ex.77:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1280.546
..___tag_value___svml_asin1_ha_ex.78:
        jmp       ..B4.4        # Prob 100%                     #1280.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_asin1_ha_ex,@function
	.size	__svml_asin1_ha_ex,.-__svml_asin1_ha_ex
..LN__svml_asin1_ha_ex.3:
	.data
# -- End  __svml_asin1_ha_ex
	.text
.L_2__routine_start___svml_asin4_ha_e9_4:
# -- Begin  __svml_asin4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin4_ha_e9
# --- __svml_asin4_ha_e9(__m256d)
__svml_asin4_ha_e9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1288.1
        .byte     15                                            #1382.552
        .byte     30                                            #1382.552
        .byte     250                                           #1382.552
	.cfi_startproc
..___tag_value___svml_asin4_ha_e9.80:
..L81:
                                                         #1288.1
        pushq     %rbp                                          #1288.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1288.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1288.1
        subq      $192, %rsp                                    #1288.1
        vmovupd   __svml_dasin_ha_data_internal(%rip), %ymm11   #1325.48
        vmovupd   64+__svml_dasin_ha_data_internal(%rip), %ymm4 #1326.48
        vmovupd   256+__svml_dasin_ha_data_internal(%rip), %ymm14 #1327.44
        vmovapd   %ymm0, %ymm10                                 #1288.1
        vandpd    %ymm10, %ymm11, %ymm9                         #1328.8
        vmulpd    %ymm4, %ymm9, %ymm12                          #1330.35
        vmulpd    %ymm9, %ymm9, %ymm13                          #1331.9
        vcmplt_oqpd %ymm9, %ymm14, %ymm15                       #1333.13
        vsubpd    %ymm12, %ymm4, %ymm6                          #1330.8
        vcvtpd2ps %ymm6, %xmm2                                  #1337.533
        vminpd    %ymm6, %ymm13, %ymm7                          #1332.9
        vcmplt_oqpd 128+__svml_dasin_ha_data_internal(%rip), %ymm6, %ymm3 #1337.666
        vaddpd    %ymm6, %ymm6, %ymm6                           #1337.775
        vrsqrtps  %xmm2, %xmm1                                  #1337.520
        vandnpd   %ymm10, %ymm11, %ymm8                         #1329.12
        vcvtps2pd %xmm1, %ymm11                                 #1337.504
        vandnpd   %ymm11, %ymm3, %ymm14                         #1337.722
        vandpd    320+__svml_dasin_ha_data_internal(%rip), %ymm6, %ymm12 #1337.1087
        vsubpd    %ymm12, %ymm6, %ymm13                         #1337.1136
        vmulpd    %ymm13, %ymm14, %ymm3                         #1337.1218
        vmulpd    %ymm3, %ymm14, %ymm2                          #1337.1304
        vextractf128 $1, %ymm15, %xmm0                          #1334.114
        vshufps   $221, %xmm0, %xmm15, %xmm5                    #1335.57
        vmovmskps %xmm5, %edx                                   #1335.40
        vcmpnlt_uqpd %ymm4, %ymm9, %ymm5                        #1336.14
        vmulpd    %ymm12, %ymm14, %ymm4                         #1337.1177
        vmulpd    %ymm4, %ymm14, %ymm0                          #1337.1261
        vaddpd    %ymm3, %ymm4, %ymm11                          #1337.1442
        vsubpd    384+__svml_dasin_ha_data_internal(%rip), %ymm0, %ymm1 #1337.1348
        vaddpd    %ymm1, %ymm2, %ymm14                          #1337.1396
        vmulpd    448+__svml_dasin_ha_data_internal(%rip), %ymm14, %ymm6 #1337.1885
        vmulpd    %ymm11, %ymm14, %ymm2                         #1337.1946
        vaddpd    512+__svml_dasin_ha_data_internal(%rip), %ymm6, %ymm12 #1337.1870
        vmulpd    %ymm12, %ymm14, %ymm13                        #1337.2010
        vaddpd    576+__svml_dasin_ha_data_internal(%rip), %ymm13, %ymm15 #1337.1995
        vmulpd    %ymm15, %ymm14, %ymm0                         #1337.2091
        vaddpd    640+__svml_dasin_ha_data_internal(%rip), %ymm0, %ymm1 #1337.2076
        vmulpd    832+__svml_dasin_ha_data_internal(%rip), %ymm7, %ymm0 #1343.30
        vmulpd    %ymm1, %ymm2, %ymm6                           #1337.2167
        vmulpd    %ymm7, %ymm7, %ymm1                           #1344.13
        vaddpd    896+__svml_dasin_ha_data_internal(%rip), %ymm0, %ymm12 #1343.15
        vsubpd    %ymm3, %ymm6, %ymm2                           #1337.2152
        vmulpd    704+__svml_dasin_ha_data_internal(%rip), %ymm7, %ymm3 #1342.27
        vmulpd    1088+__svml_dasin_ha_data_internal(%rip), %ymm7, %ymm6 #1350.29
        vaddpd    768+__svml_dasin_ha_data_internal(%rip), %ymm3, %ymm11 #1342.12
        vaddpd    1152+__svml_dasin_ha_data_internal(%rip), %ymm6, %ymm15 #1350.14
        vmulpd    960+__svml_dasin_ha_data_internal(%rip), %ymm7, %ymm3 #1347.29
        vmulpd    %ymm1, %ymm11, %ymm13                         #1351.27
        vmulpd    %ymm1, %ymm1, %ymm6                           #1353.13
        vaddpd    1024+__svml_dasin_ha_data_internal(%rip), %ymm3, %ymm14 #1347.14
        vaddpd    %ymm13, %ymm12, %ymm3                         #1351.12
        vmulpd    %ymm14, %ymm1, %ymm0                          #1352.29
        vmulpd    %ymm6, %ymm3, %ymm12                          #1357.27
        vmulpd    1216+__svml_dasin_ha_data_internal(%rip), %ymm7, %ymm14 #1356.29
        vaddpd    %ymm0, %ymm15, %ymm11                         #1352.14
        vaddpd    1280+__svml_dasin_ha_data_internal(%rip), %ymm14, %ymm15 #1356.14
        vaddpd    %ymm12, %ymm11, %ymm13                        #1357.12
        vmulpd    %ymm13, %ymm1, %ymm1                          #1358.27
        vaddpd    %ymm1, %ymm15, %ymm0                          #1358.12
        vmulpd    %ymm0, %ymm7, %ymm1                           #1361.27
        vaddpd    1344+__svml_dasin_ha_data_internal(%rip), %ymm1, %ymm3 #1361.12
        vsubpd    %ymm4, %ymm2, %ymm1                           #1367.12
        vaddpd    1536+__svml_dasin_ha_data_internal(%rip), %ymm2, %ymm2 #1369.14
        vmulpd    %ymm3, %ymm7, %ymm6                           #1362.27
        vaddpd    1408+__svml_dasin_ha_data_internal(%rip), %ymm6, %ymm11 #1362.12
        vmulpd    %ymm11, %ymm7, %ymm0                          #1363.12
        vmovupd   1472+__svml_dasin_ha_data_internal(%rip), %ymm7 #1364.49
        vsubpd    %ymm4, %ymm7, %ymm6                           #1366.15
        vsubpd    %ymm6, %ymm7, %ymm7                           #1368.14
        vsubpd    %ymm7, %ymm4, %ymm4                           #1370.14
        vsubpd    %ymm4, %ymm2, %ymm3                           #1371.14
        vandpd    %ymm5, %ymm1, %ymm11                          #1372.12
        vandnpd   %ymm9, %ymm5, %ymm9                           #1375.12
        vandpd    %ymm5, %ymm3, %ymm13                          #1373.14
        vandpd    %ymm5, %ymm6, %ymm12                          #1374.15
        vorpd     %ymm9, %ymm11, %ymm5                          #1376.12
        vmulpd    %ymm0, %ymm5, %ymm0                           #1378.27
        vaddpd    %ymm0, %ymm13, %ymm2                          #1378.12
        vorpd     %ymm9, %ymm12, %ymm1                          #1377.15
        vaddpd    %ymm2, %ymm1, %ymm3                           #1379.15
        vxorpd    %ymm8, %ymm3, %ymm0                           #1380.14
        testl     %edx, %edx                                    #1382.52
        jne       ..B5.3        # Prob 5%                       #1382.52
                                # LOE rbx r12 r13 r14 r15 edx ymm0 ymm10
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1385.12
        popq      %rbp                                          #1385.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1385.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm10, 64(%rsp)                              #1382.200
        vmovupd   %ymm0, 128(%rsp)                              #1382.276
        je        ..B5.2        # Prob 95%                      #1382.380
                                # LOE rbx r12 r13 r14 r15 edx ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1382.460
                                # LOE rbx r12 r13 r14 r15 eax edx
..B5.13:                        # Preds ..B5.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.7:                         # Preds ..B5.8 ..B5.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1382.523
        jc        ..B5.10       # Prob 5%                       #1382.523
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1382.476
        cmpl      $4, %r12d                                     #1382.471
        jl        ..B5.7        # Prob 82%                      #1382.471
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #1382.683
        jmp       ..B5.2        # Prob 100%                     #1382.683
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1382.552
        lea       128(%rsp,%r12,8), %rsi                        #1382.552
..___tag_value___svml_asin4_ha_e9.98:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1382.552
..___tag_value___svml_asin4_ha_e9.99:
        jmp       ..B5.8        # Prob 100%                     #1382.552
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asin4_ha_e9,@function
	.size	__svml_asin4_ha_e9,.-__svml_asin4_ha_e9
..LN__svml_asin4_ha_e9.4:
	.data
# -- End  __svml_asin4_ha_e9
	.text
.L_2__routine_start___svml_asin2_ha_ex_5:
# -- Begin  __svml_asin2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin2_ha_ex
# --- __svml_asin2_ha_ex(__m128d)
__svml_asin2_ha_ex:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1390.1
        .byte     15                                            #1484.546
        .byte     30                                            #1484.546
        .byte     250                                           #1484.546
	.cfi_startproc
..___tag_value___svml_asin2_ha_ex.101:
..L102:
                                                        #1390.1
        pushq     %rbp                                          #1390.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1390.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1390.1
        subq      $192, %rsp                                    #1390.1
        movaps    %xmm0, %xmm7                                  #1390.1
        movups    __svml_dasin_ha_data_internal(%rip), %xmm5    #1427.45
        movups    64+__svml_dasin_ha_data_internal(%rip), %xmm10 #1428.45
        movaps    %xmm5, %xmm6                                  #1430.8
        andps     %xmm7, %xmm6                                  #1430.8
        movaps    %xmm10, %xmm8                                 #1432.32
        mulpd     %xmm6, %xmm8                                  #1432.32
        movaps    %xmm10, %xmm0                                 #1432.8
        movaps    %xmm6, %xmm4                                  #1433.9
        subpd     %xmm8, %xmm0                                  #1432.8
        mulpd     %xmm6, %xmm4                                  #1433.9
        cvtpd2ps  %xmm0, %xmm11                                 #1439.546
        minpd     %xmm0, %xmm4                                  #1434.9
        movlhps   %xmm11, %xmm11                                #1439.530
        movaps    %xmm0, %xmm8                                  #1439.717
        rsqrtps   %xmm11, %xmm12                                #1439.517
        cmpltpd   128+__svml_dasin_ha_data_internal(%rip), %xmm8 #1439.717
        addpd     %xmm0, %xmm0                                  #1439.810
        cvtps2pd  %xmm12, %xmm13                                #1439.504
        movups    320+__svml_dasin_ha_data_internal(%rip), %xmm2 #1439.1113
        andnps    %xmm13, %xmm8                                 #1439.760
        andps     %xmm0, %xmm2                                  #1439.1113
        movaps    %xmm8, %xmm14                                 #1439.1275
        subpd     %xmm2, %xmm0                                  #1439.1159
        mulpd     %xmm8, %xmm2                                  #1439.1197
        mulpd     %xmm8, %xmm0                                  #1439.1235
        mulpd     %xmm2, %xmm14                                 #1439.1275
        mulpd     %xmm0, %xmm8                                  #1439.1315
        subpd     384+__svml_dasin_ha_data_internal(%rip), %xmm14 #1439.1356
        addpd     %xmm14, %xmm8                                 #1439.1401
        movups    448+__svml_dasin_ha_data_internal(%rip), %xmm15 #1439.1869
        movaps    %xmm2, %xmm1                                  #1439.1444
        mulpd     %xmm8, %xmm15                                 #1439.1869
        addpd     %xmm0, %xmm1                                  #1439.1444
        addpd     512+__svml_dasin_ha_data_internal(%rip), %xmm15 #1439.1857
        mulpd     %xmm8, %xmm1                                  #1439.1927
        mulpd     %xmm8, %xmm15                                 #1439.1985
        addpd     576+__svml_dasin_ha_data_internal(%rip), %xmm15 #1439.1973
        mulpd     %xmm15, %xmm8                                 #1439.2060
        addpd     640+__svml_dasin_ha_data_internal(%rip), %xmm8 #1439.2048
        mulpd     %xmm8, %xmm1                                  #1439.2130
        subpd     %xmm0, %xmm1                                  #1439.2118
        movups    704+__svml_dasin_ha_data_internal(%rip), %xmm0 #1444.24
        movaps    %xmm4, %xmm13                                 #1446.13
        mulpd     %xmm4, %xmm0                                  #1444.24
        mulpd     %xmm4, %xmm13                                 #1446.13
        addpd     768+__svml_dasin_ha_data_internal(%rip), %xmm0 #1444.12
        movups    256+__svml_dasin_ha_data_internal(%rip), %xmm9 #1435.13
        movaps    %xmm6, %xmm3                                  #1438.14
        cmpltpd   %xmm6, %xmm9                                  #1435.13
        cmpnltpd  %xmm10, %xmm3                                 #1438.14
        mulpd     %xmm13, %xmm0                                 #1453.24
        movmskpd  %xmm9, %edx                                   #1437.40
        movups    832+__svml_dasin_ha_data_internal(%rip), %xmm11 #1445.27
        movaps    %xmm13, %xmm10                                #1455.13
        movups    960+__svml_dasin_ha_data_internal(%rip), %xmm9 #1449.26
        movaps    %xmm1, %xmm8                                  #1469.12
        mulpd     %xmm4, %xmm11                                 #1445.27
        mulpd     %xmm4, %xmm9                                  #1449.26
        addpd     896+__svml_dasin_ha_data_internal(%rip), %xmm11 #1445.15
        addpd     1024+__svml_dasin_ha_data_internal(%rip), %xmm9 #1449.14
        subpd     %xmm2, %xmm8                                  #1469.12
        mulpd     %xmm13, %xmm10                                #1455.13
        mulpd     %xmm13, %xmm9                                 #1454.26
        addpd     %xmm0, %xmm11                                 #1453.12
        movups    1088+__svml_dasin_ha_data_internal(%rip), %xmm12 #1452.26
        andps     %xmm3, %xmm8                                  #1474.12
        mulpd     %xmm4, %xmm12                                 #1452.26
        mulpd     %xmm10, %xmm11                                #1459.24
        addpd     1152+__svml_dasin_ha_data_internal(%rip), %xmm12 #1452.14
        addpd     %xmm9, %xmm12                                 #1454.14
        movups    1216+__svml_dasin_ha_data_internal(%rip), %xmm14 #1458.26
        andnps    %xmm7, %xmm5                                  #1431.12
        mulpd     %xmm4, %xmm14                                 #1458.26
        addpd     %xmm11, %xmm12                                #1459.12
        addpd     1280+__svml_dasin_ha_data_internal(%rip), %xmm14 #1458.14
        mulpd     %xmm12, %xmm13                                #1460.24
        addpd     %xmm13, %xmm14                                #1460.12
        mulpd     %xmm4, %xmm14                                 #1463.24
        addpd     1344+__svml_dasin_ha_data_internal(%rip), %xmm14 #1463.12
        movups    1472+__svml_dasin_ha_data_internal(%rip), %xmm15 #1466.46
        movaps    %xmm15, %xmm0                                 #1468.15
        subpd     %xmm2, %xmm0                                  #1468.15
        mulpd     %xmm4, %xmm14                                 #1464.24
        subpd     %xmm0, %xmm15                                 #1470.14
        addpd     1408+__svml_dasin_ha_data_internal(%rip), %xmm14 #1464.12
        subpd     %xmm15, %xmm2                                 #1472.14
        mulpd     %xmm14, %xmm4                                 #1465.12
        movups    1536+__svml_dasin_ha_data_internal(%rip), %xmm9 #1471.14
        andps     %xmm3, %xmm0                                  #1476.15
        addpd     %xmm1, %xmm9                                  #1471.14
        subpd     %xmm2, %xmm9                                  #1473.14
        andps     %xmm3, %xmm9                                  #1475.14
        andnps    %xmm6, %xmm3                                  #1477.12
        orps      %xmm3, %xmm8                                  #1478.12
        orps      %xmm3, %xmm0                                  #1479.15
        mulpd     %xmm4, %xmm8                                  #1480.24
        addpd     %xmm8, %xmm9                                  #1480.12
        addpd     %xmm9, %xmm0                                  #1481.15
        pxor      %xmm5, %xmm0                                  #1482.14
        testl     %edx, %edx                                    #1484.52
        jne       ..B6.3        # Prob 5%                       #1484.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B6.2:                         # Preds ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1487.12
        popq      %rbp                                          #1487.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1487.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #1484.197
        movups    %xmm0, 128(%rsp)                              #1484.270
                                # LOE rbx r12 r13 r14 r15 edx
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #1484.454
        movq      %r12, 8(%rsp)                                 #1484.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #1484.454
        movq      %r13, (%rsp)                                  #1484.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #1484.454
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1484.517
        jc        ..B6.10       # Prob 5%                       #1484.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1484.470
        cmpl      $2, %r12d                                     #1484.465
        jl        ..B6.7        # Prob 82%                      #1484.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1484.674
        jmp       ..B6.2        # Prob 100%                     #1484.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #1484.546
        lea       128(%rsp,%r12,8), %rsi                        #1484.546
..___tag_value___svml_asin2_ha_ex.119:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1484.546
..___tag_value___svml_asin2_ha_ex.120:
        jmp       ..B6.8        # Prob 100%                     #1484.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_asin2_ha_ex,@function
	.size	__svml_asin2_ha_ex,.-__svml_asin2_ha_ex
..LN__svml_asin2_ha_ex.5:
	.data
# -- End  __svml_asin2_ha_ex
	.text
.L_2__routine_start___svml_asin8_ha_z0_6:
# -- Begin  __svml_asin8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin8_ha_z0
# --- __svml_asin8_ha_z0(__m512d)
__svml_asin8_ha_z0:
# parameter 1: %zmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1492.1
        .byte     15                                            #1587.12
        .byte     30                                            #1587.12
        .byte     250                                           #1587.12
	.cfi_startproc
..___tag_value___svml_asin8_ha_z0.122:
..L123:
                                                        #1492.1
        vmovups   64+__svml_dasin_ha_data_internal(%rip), %zmm7 #1531.46
        vmovups   128+__svml_dasin_ha_data_internal(%rip), %zmm9 #1543.575
        vmovups   384+__svml_dasin_ha_data_internal(%rip), %zmm15 #1543.1222
        vmovups   448+__svml_dasin_ha_data_internal(%rip), %zmm13 #1543.1599
        vmovups   512+__svml_dasin_ha_data_internal(%rip), %zmm1 #1543.1940
        vmovups   256+__svml_dasin_ha_data_internal(%rip), %zmm8 #1532.42
        vmovups   576+__svml_dasin_ha_data_internal(%rip), %zmm14 #1543.1785
        vandpd    __svml_dasin_ha_data_internal(%rip), %zmm0, %zmm5 #1533.8
        vmovaps   %zmm7, %zmm10                                 #1534.8
        vfnmadd231pd {rn-sae}, %zmm5, %zmm7, %zmm10             #1534.8
        vmulpd    {rn-sae}, %zmm5, %zmm5, %zmm6                 #1535.11
        vrsqrt14pd %zmm10, %zmm12                               #1543.505
        vcmppd    $17, {sae}, %zmm9, %zmm10, %k2                #1543.636
        vcmppd    $17, {sae}, %zmm5, %zmm8, %k1                 #1538.13
        vcmppd    $21, {sae}, %zmm7, %zmm5, %k3                 #1539.14
        vmovups   704+__svml_dasin_ha_data_internal(%rip), %zmm7 #1544.57
        vmovups   832+__svml_dasin_ha_data_internal(%rip), %zmm8 #1546.57
        vmovups   960+__svml_dasin_ha_data_internal(%rip), %zmm9 #1551.56
        vminpd    {sae}, %zmm10, %zmm6, %zmm3                   #1537.11
        vxorpd    %zmm12, %zmm12, %zmm12{%k2}                   #1543.722
        vaddpd    {rn-sae}, %zmm10, %zmm10, %zmm6               #1543.794
        vxorpd    %zmm0, %zmm5, %zmm4                           #1536.12
        vorpd     1728+__svml_dasin_ha_data_internal(%rip), %zmm3, %zmm3{%k1} #1542.11
        vmulpd    {rn-sae}, %zmm12, %zmm12, %zmm11              #1543.1118
        vmulpd    {rn-sae}, %zmm12, %zmm6, %zmm2                #1543.1335
        vmovups   640+__svml_dasin_ha_data_internal(%rip), %zmm0 #1543.1878
        vmovups   1088+__svml_dasin_ha_data_internal(%rip), %zmm10 #1554.56
        vfmsub231pd {rn-sae}, %zmm6, %zmm11, %zmm15             #1543.1222
        vfmsub213pd {rn-sae}, %zmm2, %zmm12, %zmm6              #1543.1436
        vmovups   896+__svml_dasin_ha_data_internal(%rip), %zmm12 #1549.15
        vfmadd231pd {rn-sae}, %zmm15, %zmm13, %zmm1             #1543.1940
        vmovups   1152+__svml_dasin_ha_data_internal(%rip), %zmm13 #1556.14
        vfmadd231pd {rn-sae}, %zmm3, %zmm8, %zmm12              #1549.15
        vmovups   1280+__svml_dasin_ha_data_internal(%rip), %zmm8 #1562.14
        vmulpd    {rn-sae}, %zmm15, %zmm2, %zmm11               #1543.2058
        vfmadd213pd {rn-sae}, %zmm14, %zmm15, %zmm1             #1543.2166
        vfmadd231pd {rn-sae}, %zmm3, %zmm10, %zmm13             #1556.14
        vmovups   1216+__svml_dasin_ha_data_internal(%rip), %zmm14 #1560.56
        vmovups   1408+__svml_dasin_ha_data_internal(%rip), %zmm10 #1566.56
        vfmadd213pd {rn-sae}, %zmm0, %zmm15, %zmm1              #1543.2287
        vmovups   768+__svml_dasin_ha_data_internal(%rip), %zmm0 #1548.12
        vfmadd231pd {rn-sae}, %zmm3, %zmm14, %zmm8              #1562.14
        vfmsub213pd {rn-sae}, %zmm6, %zmm11, %zmm1              #1543.2403
        vfmadd231pd {rn-sae}, %zmm3, %zmm7, %zmm0               #1548.12
        vmovups   1024+__svml_dasin_ha_data_internal(%rip), %zmm6 #1553.14
        vmulpd    {rn-sae}, %zmm3, %zmm3, %zmm7                 #1550.15
        vfmadd231pd {rn-sae}, %zmm3, %zmm9, %zmm6               #1553.14
        vmovups   1344+__svml_dasin_ha_data_internal(%rip), %zmm9 #1565.56
        vsubpd    {rn-sae}, %zmm2, %zmm1, %zmm5{%k3}            #1574.14
        vmulpd    {rn-sae}, %zmm7, %zmm7, %zmm15                #1559.15
        vfmadd213pd {rn-sae}, %zmm12, %zmm7, %zmm0              #1557.12
        vfmadd213pd {rn-sae}, %zmm13, %zmm7, %zmm6              #1558.14
        vfmadd213pd {rn-sae}, %zmm6, %zmm15, %zmm0              #1563.12
        vmovups   1600+__svml_dasin_ha_data_internal(%rip), %zmm6 #1572.46
        vfmadd213pd {rn-sae}, %zmm8, %zmm7, %zmm0               #1564.12
        vfmadd213pd {rn-sae}, %zmm9, %zmm3, %zmm0               #1567.12
        vfmadd213pd {rn-sae}, %zmm10, %zmm3, %zmm0              #1568.12
        vmulpd    {rn-sae}, %zmm3, %zmm0, %zmm8                 #1569.14
        vmovups   1472+__svml_dasin_ha_data_internal(%rip), %zmm3 #1570.47
        vmovups   1536+__svml_dasin_ha_data_internal(%rip), %zmm0 #1571.47
        vsubpd    {rn-sae}, %zmm2, %zmm3, %zmm7                 #1573.17
        vaddpd    {rn-sae}, %zmm1, %zmm0, %zmm1                 #1576.17
        vsubpd    {rn-sae}, %zmm7, %zmm3, %zmm3                 #1575.16
        vsubpd    {rn-sae}, %zmm3, %zmm2, %zmm2                 #1577.16
        vsubpd    {rn-sae}, %zmm2, %zmm1, %zmm6{%k3}            #1578.16
        vfmadd213pd {rn-sae}, %zmm6, %zmm5, %zmm8               #1579.12
        vblendmpd %zmm7, %zmm5, %zmm5{%k3}                      #1580.15
        vaddpd    {rn-sae}, %zmm8, %zmm5, %zmm0                 #1581.17
        vxorpd    %zmm4, %zmm0, %zmm0                           #1582.14
        ret                                                     #1587.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_asin8_ha_z0,@function
	.size	__svml_asin8_ha_z0,.-__svml_asin8_ha_z0
..LN__svml_asin8_ha_z0.6:
	.data
# -- End  __svml_asin8_ha_z0
	.text
.L_2__routine_start___svml_asin1_ha_l9_7:
# -- Begin  __svml_asin1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin1_ha_l9
# --- __svml_asin1_ha_l9(__m128d)
__svml_asin1_ha_l9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1592.1
        .byte     15                                            #1686.546
        .byte     30                                            #1686.546
        .byte     250                                           #1686.546
	.cfi_startproc
..___tag_value___svml_asin1_ha_l9.125:
..L126:
                                                        #1592.1
        pushq     %rbp                                          #1592.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1592.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1592.1
        subq      $128, %rsp                                    #1592.1
        vmovapd   %xmm0, %xmm12                                 #1592.1
        vmovsd    __svml_dasin_ha_data_internal(%rip), %xmm13   #1629.14
        vandpd    %xmm12, %xmm13, %xmm11                        #1632.8
        vandnpd   %xmm12, %xmm13, %xmm10                        #1633.12
        vmovsd    64+__svml_dasin_ha_data_internal(%rip), %xmm6 #1630.14
        vmovapd   %xmm11, %xmm7                                 #1634.8
        vfnmadd213sd %xmm6, %xmm6, %xmm7                        #1634.8
        vmovsd    256+__svml_dasin_ha_data_internal(%rip), %xmm8 #1631.10
        vmulsd    %xmm11, %xmm11, %xmm14                        #1635.9
        vcmpltsd  %xmm11, %xmm8, %xmm0                          #1637.13
        vcmpnltsd %xmm6, %xmm11, %xmm8                          #1640.14
        vcvtpd2ps %xmm7, %xmm5                                  #1641.546
        vmovmskpd %xmm0, %eax                                   #1639.40
        vcmpltsd  128+__svml_dasin_ha_data_internal(%rip), %xmm7, %xmm3 #1641.717
        vaddsd    %xmm7, %xmm7, %xmm0                           #1641.810
        vshufps   $0, %xmm5, %xmm5, %xmm4                       #1641.530
        vandnpd   %xmm11, %xmm8, %xmm11                         #1679.12
        vrsqrtps  %xmm4, %xmm2                                  #1641.517
        vcvtps2pd %xmm2, %xmm13                                 #1641.504
        vunpcklpd %xmm14, %xmm14, %xmm15                        #1636.21
        vandnpd   %xmm13, %xmm3, %xmm14                         #1641.760
        vunpcklpd %xmm7, %xmm7, %xmm1                           #1636.72
        vmulsd    %xmm14, %xmm14, %xmm7                         #1641.1068
        vminpd    %xmm1, %xmm15, %xmm9                          #1636.9
        vmulsd    %xmm14, %xmm0, %xmm6                          #1641.1163
        vmulsd    %xmm9, %xmm9, %xmm3                           #1648.13
        vmovapd   %xmm0, %xmm1                                  #1641.1110
        vmovapd   %xmm9, %xmm4                                  #1646.12
        vfmsub213sd 384+__svml_dasin_ha_data_internal(%rip), %xmm7, %xmm1 #1641.1110
        vmovsd    448+__svml_dasin_ha_data_internal(%rip), %xmm5 #1641.1644
        vmovsd    832+__svml_dasin_ha_data_internal(%rip), %xmm2 #1644.25
        vmovsd    960+__svml_dasin_ha_data_internal(%rip), %xmm7 #1649.24
        vmovsd    1088+__svml_dasin_ha_data_internal(%rip), %xmm13 #1652.24
        vfmadd213sd 512+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm5 #1641.1644
        vfmsub213sd %xmm6, %xmm14, %xmm0                        #1641.1202
        vmulsd    %xmm1, %xmm6, %xmm15                          #1641.1702
        vfmadd213sd 576+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm5 #1641.1748
        vmovsd    1216+__svml_dasin_ha_data_internal(%rip), %xmm14 #1658.24
        vfmadd213sd 640+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm5 #1641.1811
        vmovsd    704+__svml_dasin_ha_data_internal(%rip), %xmm1 #1642.25
        vfmadd213sd 768+__svml_dasin_ha_data_internal(%rip), %xmm1, %xmm4 #1646.12
        vmovapd   %xmm9, %xmm1                                  #1647.15
        vfmadd213sd 896+__svml_dasin_ha_data_internal(%rip), %xmm2, %xmm1 #1647.15
        vmovapd   %xmm9, %xmm2                                  #1651.14
        vfmadd213sd 1024+__svml_dasin_ha_data_internal(%rip), %xmm7, %xmm2 #1651.14
        vmovapd   %xmm9, %xmm7                                  #1660.14
        vfmsub213sd %xmm0, %xmm15, %xmm5                        #1641.1869
        vmovapd   %xmm9, %xmm0                                  #1654.14
        vfmadd213sd 1152+__svml_dasin_ha_data_internal(%rip), %xmm13, %xmm0 #1654.14
        vmulsd    %xmm3, %xmm3, %xmm15                          #1657.13
        vfmadd213sd %xmm1, %xmm3, %xmm4                         #1655.12
        vfmadd213sd 1280+__svml_dasin_ha_data_internal(%rip), %xmm14, %xmm7 #1660.14
        vfmadd213sd %xmm0, %xmm3, %xmm2                         #1656.14
        vfmadd213sd %xmm2, %xmm15, %xmm4                        #1661.12
        vmovsd    1536+__svml_dasin_ha_data_internal(%rip), %xmm2 #1669.15
        vfmadd213sd %xmm7, %xmm3, %xmm4                         #1662.12
        vsubsd    %xmm6, %xmm5, %xmm3                           #1671.12
        vaddsd    %xmm5, %xmm2, %xmm5                           #1673.14
        vfmadd213sd 1344+__svml_dasin_ha_data_internal(%rip), %xmm9, %xmm4 #1665.12
        vandpd    %xmm8, %xmm3, %xmm7                           #1676.12
        vorpd     %xmm11, %xmm7, %xmm7                          #1680.12
        vfmadd213sd 1408+__svml_dasin_ha_data_internal(%rip), %xmm9, %xmm4 #1666.12
        vmulsd    %xmm9, %xmm4, %xmm1                           #1667.12
        vmovsd    1472+__svml_dasin_ha_data_internal(%rip), %xmm9 #1668.15
        vsubsd    %xmm6, %xmm9, %xmm0                           #1670.15
        vsubsd    %xmm0, %xmm9, %xmm9                           #1672.14
        vandpd    %xmm8, %xmm0, %xmm0                           #1678.15
        vorpd     %xmm11, %xmm0, %xmm0                          #1681.15
        vsubsd    %xmm9, %xmm6, %xmm6                           #1674.14
        vsubsd    %xmm6, %xmm5, %xmm4                           #1675.14
        vandpd    %xmm8, %xmm4, %xmm13                          #1677.14
        vfmadd213sd %xmm13, %xmm7, %xmm1                        #1682.12
        vaddsd    %xmm1, %xmm0, %xmm1                           #1683.15
        vxorpd    %xmm10, %xmm1, %xmm0                          #1684.14
        andl      $1, %eax                                      #1639.91
        jne       ..B8.3        # Prob 5%                       #1686.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm12
..B8.2:                         # Preds ..B8.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1689.12
        popq      %rbp                                          #1689.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1689.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.3:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm12, (%rsp)                                #1686.160
        vmovsd    %xmm0, 64(%rsp)                               #1686.233
        jne       ..B8.5        # Prob 5%                       #1686.374
                                # LOE rbx r12 r13 r14 r15 eax
..B8.4:                         # Preds ..B8.6 ..B8.5 ..B8.3
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm0                               #1686.626
        movq      %rbp, %rsp                                    #1686.626
        popq      %rbp                                          #1686.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1686.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.5:                         # Preds ..B8.3
                                # Execution count [2.50e-03]: Infreq
        je        ..B8.4        # Prob 95%                      #1686.517
                                # LOE rbx r12 r13 r14 r15
..B8.6:                         # Preds ..B8.5
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1686.546
        lea       64(%rsp), %rsi                                #1686.546
..___tag_value___svml_asin1_ha_l9.138:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1686.546
..___tag_value___svml_asin1_ha_l9.139:
        jmp       ..B8.4        # Prob 100%                     #1686.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_asin1_ha_l9,@function
	.size	__svml_asin1_ha_l9,.-__svml_asin1_ha_l9
..LN__svml_asin1_ha_l9.7:
	.data
# -- End  __svml_asin1_ha_l9
	.text
.L_2__routine_start___svml_asin1_ha_e9_8:
# -- Begin  __svml_asin1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_asin1_ha_e9
# --- __svml_asin1_ha_e9(__m128d)
__svml_asin1_ha_e9:
# parameter 1: %xmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1694.1
        .byte     15                                            #1788.546
        .byte     30                                            #1788.546
        .byte     250                                           #1788.546
	.cfi_startproc
..___tag_value___svml_asin1_ha_e9.141:
..L142:
                                                        #1694.1
        pushq     %rbp                                          #1694.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1694.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1694.1
        subq      $128, %rsp                                    #1694.1
        movaps    %xmm0, %xmm8                                  #1694.1
        movsd     __svml_dasin_ha_data_internal(%rip), %xmm6    #1731.14
        movaps    %xmm6, %xmm7                                  #1734.8
        andnps    %xmm8, %xmm6                                  #1735.12
        andps     %xmm8, %xmm7                                  #1734.8
        movsd     64+__svml_dasin_ha_data_internal(%rip), %xmm12 #1732.14
        movaps    %xmm7, %xmm9                                  #1736.32
        mulsd     %xmm12, %xmm9                                 #1736.32
        movaps    %xmm12, %xmm0                                 #1736.8
        movsd     320+__svml_dasin_ha_data_internal(%rip), %xmm3 #1743.1113
        movaps    %xmm7, %xmm5                                  #1737.9
        mulsd     %xmm7, %xmm5                                  #1737.9
        subsd     %xmm9, %xmm0                                  #1736.8
        cvtpd2ps  %xmm0, %xmm13                                 #1743.546
        shufps    $0, %xmm13, %xmm13                            #1743.530
        movaps    %xmm0, %xmm10                                 #1738.72
        rsqrtps   %xmm13, %xmm14                                #1743.517
        cvtps2pd  %xmm14, %xmm15                                #1743.504
        movaps    %xmm0, %xmm2                                  #1743.717
        movaps    %xmm7, %xmm4                                  #1742.14
        unpcklpd  %xmm0, %xmm10                                 #1738.72
        cmpltsd   128+__svml_dasin_ha_data_internal(%rip), %xmm2 #1743.717
        addsd     %xmm0, %xmm0                                  #1743.810
        cmpnltsd  %xmm12, %xmm4                                 #1742.14
        andnps    %xmm15, %xmm2                                 #1743.760
        andps     %xmm0, %xmm3                                  #1743.1113
        unpcklpd  %xmm5, %xmm5                                  #1738.21
        subsd     %xmm3, %xmm0                                  #1743.1159
        mulsd     %xmm2, %xmm3                                  #1743.1197
        minpd     %xmm10, %xmm5                                 #1738.9
        mulsd     %xmm2, %xmm0                                  #1743.1235
        movaps    %xmm3, %xmm9                                  #1743.1275
        movaps    %xmm0, %xmm1                                  #1743.1315
        mulsd     %xmm2, %xmm9                                  #1743.1275
        movaps    %xmm3, %xmm10                                 #1743.1444
        addsd     %xmm0, %xmm10                                 #1743.1444
        mulsd     %xmm2, %xmm1                                  #1743.1315
        subsd     384+__svml_dasin_ha_data_internal(%rip), %xmm9 #1743.1356
        movsd     448+__svml_dasin_ha_data_internal(%rip), %xmm2 #1743.1869
        movaps    %xmm5, %xmm14                                 #1750.13
        movsd     256+__svml_dasin_ha_data_internal(%rip), %xmm11 #1739.13
        movaps    %xmm5, %xmm13                                 #1753.26
        mulsd     %xmm5, %xmm14                                 #1750.13
        addsd     %xmm1, %xmm9                                  #1743.1401
        cmpltsd   %xmm7, %xmm11                                 #1739.13
        mulsd     960+__svml_dasin_ha_data_internal(%rip), %xmm13 #1753.26
        mulsd     %xmm9, %xmm2                                  #1743.1869
        mulsd     %xmm9, %xmm10                                 #1743.1927
        addsd     1024+__svml_dasin_ha_data_internal(%rip), %xmm13 #1753.14
        movmskpd  %xmm11, %eax                                  #1741.40
        addsd     512+__svml_dasin_ha_data_internal(%rip), %xmm2 #1743.1857
        mulsd     %xmm14, %xmm13                                #1758.26
        mulsd     %xmm9, %xmm2                                  #1743.1985
        movaps    %xmm5, %xmm1                                  #1748.24
        movaps    %xmm5, %xmm11                                 #1756.26
        mulsd     704+__svml_dasin_ha_data_internal(%rip), %xmm1 #1748.24
        movaps    %xmm14, %xmm12                                #1759.13
        addsd     576+__svml_dasin_ha_data_internal(%rip), %xmm2 #1743.1973
        mulsd     1088+__svml_dasin_ha_data_internal(%rip), %xmm11 #1756.26
        mulsd     %xmm14, %xmm12                                #1759.13
        addsd     768+__svml_dasin_ha_data_internal(%rip), %xmm1 #1748.12
        mulsd     %xmm9, %xmm2                                  #1743.2060
        addsd     1152+__svml_dasin_ha_data_internal(%rip), %xmm11 #1756.14
        mulsd     %xmm14, %xmm1                                 #1757.24
        addsd     640+__svml_dasin_ha_data_internal(%rip), %xmm2 #1743.2048
        addsd     %xmm11, %xmm13                                #1758.14
        mulsd     %xmm10, %xmm2                                 #1743.2130
        movsd     1536+__svml_dasin_ha_data_internal(%rip), %xmm10 #1775.14
        subsd     %xmm0, %xmm2                                  #1743.2118
        movaps    %xmm5, %xmm0                                  #1749.27
        movaps    %xmm2, %xmm9                                  #1773.12
        mulsd     832+__svml_dasin_ha_data_internal(%rip), %xmm0 #1749.27
        subsd     %xmm3, %xmm9                                  #1773.12
        addsd     %xmm2, %xmm10                                 #1775.14
        addsd     896+__svml_dasin_ha_data_internal(%rip), %xmm0 #1749.15
        andps     %xmm4, %xmm9                                  #1778.12
        addsd     %xmm0, %xmm1                                  #1757.12
        mulsd     %xmm12, %xmm1                                 #1763.24
        movaps    %xmm5, %xmm0                                  #1762.26
        mulsd     1216+__svml_dasin_ha_data_internal(%rip), %xmm0 #1762.26
        addsd     %xmm13, %xmm1                                 #1763.12
        addsd     1280+__svml_dasin_ha_data_internal(%rip), %xmm0 #1762.14
        mulsd     %xmm14, %xmm1                                 #1764.24
        addsd     %xmm0, %xmm1                                  #1764.12
        mulsd     %xmm5, %xmm1                                  #1767.24
        addsd     1344+__svml_dasin_ha_data_internal(%rip), %xmm1 #1767.12
        mulsd     %xmm5, %xmm1                                  #1768.24
        addsd     1408+__svml_dasin_ha_data_internal(%rip), %xmm1 #1768.12
        mulsd     %xmm5, %xmm1                                  #1769.12
        movsd     1472+__svml_dasin_ha_data_internal(%rip), %xmm5 #1770.15
        movaps    %xmm5, %xmm0                                  #1772.15
        subsd     %xmm3, %xmm0                                  #1772.15
        subsd     %xmm0, %xmm5                                  #1774.14
        andps     %xmm4, %xmm0                                  #1780.15
        subsd     %xmm5, %xmm3                                  #1776.14
        subsd     %xmm3, %xmm10                                 #1777.14
        andps     %xmm4, %xmm10                                 #1779.14
        andnps    %xmm7, %xmm4                                  #1781.12
        orps      %xmm4, %xmm9                                  #1782.12
        orps      %xmm4, %xmm0                                  #1783.15
        mulsd     %xmm9, %xmm1                                  #1784.24
        addsd     %xmm10, %xmm1                                 #1784.12
        addsd     %xmm1, %xmm0                                  #1785.15
        pxor      %xmm6, %xmm0                                  #1786.14
        andl      $1, %eax                                      #1741.91
        jne       ..B9.3        # Prob 5%                       #1788.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm8
..B9.2:                         # Preds ..B9.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1791.12
        popq      %rbp                                          #1791.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1791.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.3:                         # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm8, (%rsp)                                 #1788.160
        movsd     %xmm0, 64(%rsp)                               #1788.233
        jne       ..B9.6        # Prob 5%                       #1788.374
                                # LOE rbx r12 r13 r14 r15
..B9.4:                         # Preds ..B9.6 ..B9.3
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #1788.626
        movq      %rbp, %rsp                                    #1788.626
        popq      %rbp                                          #1788.626
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1788.626
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B9.6:                         # Preds ..B9.3
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #1788.546
        lea       64(%rsp), %rsi                                #1788.546
..___tag_value___svml_asin1_ha_e9.154:
#       __svml_dasin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dasin_ha_cout_rare_internal            #1788.546
..___tag_value___svml_asin1_ha_e9.155:
        jmp       ..B9.4        # Prob 100%                     #1788.546
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_asin1_ha_e9,@function
	.size	__svml_asin1_ha_e9,.-__svml_asin1_ha_e9
..LN__svml_asin1_ha_e9.8:
	.data
# -- End  __svml_asin1_ha_e9
	.text
.L_2__routine_start___svml_dasin_ha_cout_rare_internal_9:
# -- Begin  __svml_dasin_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dasin_ha_cout_rare_internal
	.globl __svml_dasin_ha_cout_rare_internal
# --- __svml_dasin_ha_cout_rare_internal(const double *, double *)
__svml_dasin_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #701.1
        .byte     15                                            #858.17
        .byte     30                                            #858.17
        .byte     250                                           #858.17
	.cfi_startproc
..___tag_value___svml_dasin_ha_cout_rare_internal.157:
..L158:
                                                        #701.1
        xorl      %eax, %eax                                    #714.14
        movzwl    6(%rdi), %edx                                 #715.35
        andl      $32752, %edx                                  #715.35
        cmpl      $32752, %edx                                  #715.59
        je        ..B10.12      # Prob 16%                      #715.59
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B10.2:                        # Preds ..B10.1
                                # Execution count [8.40e-01]
        movq      (%rdi), %rdx                                  #717.23
        movq      %rdx, -32(%rsp)                               #717.13
        shrq      $56, %rdx                                     #718.34
        movsd     4160+_vmldASinHATab(%rip), %xmm0              #719.45
        andl      $127, %edx                                    #718.34
        movb      %dl, -25(%rsp)                                #718.34
        movsd     -32(%rsp), %xmm12                             #719.18
        comisd    %xmm12, %xmm0                                 #719.45
        jb        ..B10.10      # Prob 50%                      #719.45
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B10.3:                        # Preds ..B10.2
                                # Execution count [4.20e-01]
        movsd     4168+_vmldASinHATab(%rip), %xmm1              #721.48
        comisd    %xmm12, %xmm1                                 #721.48
        jbe       ..B10.9       # Prob 50%                      #721.48
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm1 xmm12
..B10.4:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        comisd    4136+_vmldASinHATab(%rip), %xmm12             #723.52
        jbe       ..B10.6       # Prob 50%                      #723.52
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B10.5:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        movsd     4104+_vmldASinHATab(%rip), %xmm13             #725.68
        movaps    %xmm12, %xmm5                                 #725.174
        mulsd     %xmm12, %xmm13                                #725.68
        movaps    %xmm12, %xmm7                                 #727.44
        movsd     %xmm13, -48(%rsp)                             #725.25
        movsd     -48(%rsp), %xmm14                             #725.107
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #732.68
        subsd     -32(%rsp), %xmm14                             #725.112
        movsd     %xmm14, -56(%rsp)                             #725.93
        movsd     -48(%rsp), %xmm8                              #725.138
        movsd     -56(%rsp), %xmm15                             #725.143
        subsd     %xmm15, %xmm8                                 #725.143
        movsd     %xmm8, -48(%rsp)                              #725.124
        movsd     -48(%rsp), %xmm6                              #725.174
        movsd     4296+_vmldASinHATab(%rip), %xmm8              #729.81
        subsd     %xmm6, %xmm5                                  #725.174
        movsd     %xmm5, -56(%rsp)                              #725.149
        movaps    %xmm12, %xmm5                                 #734.48
        movsd     -48(%rsp), %xmm9                              #725.188
        movaps    %xmm9, %xmm13                                 #726.40
        addsd     %xmm9, %xmm7                                  #727.44
        mulsd     %xmm9, %xmm13                                 #726.40
        movsd     -56(%rsp), %xmm11                             #725.200
        movaps    %xmm13, %xmm10                                #728.39
        mulsd     %xmm11, %xmm7                                 #727.53
        mulsd     %xmm13, %xmm1                                 #732.68
        addsd     %xmm7, %xmm10                                 #728.39
        mulsd     %xmm10, %xmm8                                 #729.81
        movsd     %xmm1, -48(%rsp)                              #732.25
        movsd     -48(%rsp), %xmm2                              #732.107
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #735.67
        subsd     %xmm13, %xmm2                                 #732.112
        addsd     4288+_vmldASinHATab(%rip), %xmm8              #729.103
        mulsd     %xmm10, %xmm8                                 #729.125
        movsd     %xmm2, -56(%rsp)                              #732.93
        movsd     -48(%rsp), %xmm3                              #732.138
        movsd     -56(%rsp), %xmm0                              #732.143
        addsd     4280+_vmldASinHATab(%rip), %xmm8              #729.147
        subsd     %xmm0, %xmm3                                  #732.143
        mulsd     %xmm10, %xmm8                                 #729.169
        movsd     %xmm3, -48(%rsp)                              #732.124
        movsd     -48(%rsp), %xmm4                              #732.174
        addsd     4272+_vmldASinHATab(%rip), %xmm8              #729.191
        subsd     %xmm4, %xmm13                                 #732.174
        mulsd     %xmm10, %xmm8                                 #729.213
        movsd     %xmm13, -56(%rsp)                             #732.149
        movsd     -48(%rsp), %xmm6                              #732.188
        mulsd     %xmm6, %xmm9                                  #733.39
        addsd     4264+_vmldASinHATab(%rip), %xmm8              #730.49
        mulsd     %xmm6, %xmm11                                 #734.63
        mulsd     %xmm10, %xmm8                                 #730.71
        mulsd     %xmm9, %xmm1                                  #735.67
        addsd     4256+_vmldASinHATab(%rip), %xmm8              #730.93
        mulsd     %xmm10, %xmm8                                 #730.115
        movaps    %xmm9, %xmm3                                  #735.171
        movsd     -56(%rsp), %xmm14                             #732.200
        movaps    %xmm6, %xmm4                                  #736.39
        movsd     %xmm1, -48(%rsp)                              #735.25
        addsd     %xmm14, %xmm7                                 #734.40
        addsd     4248+_vmldASinHATab(%rip), %xmm8              #730.137
        mulsd     %xmm7, %xmm5                                  #734.48
        mulsd     %xmm10, %xmm8                                 #730.159
        addsd     %xmm5, %xmm11                                 #734.63
        addsd     4240+_vmldASinHATab(%rip), %xmm8              #730.181
        movsd     -48(%rsp), %xmm2                              #735.106
        mulsd     %xmm10, %xmm8                                 #730.203
        subsd     %xmm9, %xmm2                                  #735.111
        addsd     %xmm11, %xmm9                                 #737.77
        addsd     4232+_vmldASinHATab(%rip), %xmm8              #730.225
        mulsd     %xmm9, %xmm7                                  #737.77
        mulsd     %xmm10, %xmm8                                 #730.247
        movsd     %xmm2, -56(%rsp)                              #735.92
        movsd     -48(%rsp), %xmm0                              #735.136
        movsd     -56(%rsp), %xmm15                             #735.141
        subsd     %xmm15, %xmm0                                 #735.141
        addsd     4224+_vmldASinHATab(%rip), %xmm8              #731.49
        movsd     %xmm0, -48(%rsp)                              #735.122
        movsd     -48(%rsp), %xmm5                              #735.171
        mulsd     %xmm10, %xmm8                                 #731.71
        subsd     %xmm5, %xmm3                                  #735.171
        movsd     %xmm3, -56(%rsp)                              #735.147
        movsd     -48(%rsp), %xmm3                              #735.185
        movsd     -56(%rsp), %xmm5                              #735.197
        movaps    %xmm5, %xmm13                                 #737.46
        addsd     4216+_vmldASinHATab(%rip), %xmm8              #731.93
        mulsd     %xmm3, %xmm4                                  #736.39
        addsd     %xmm11, %xmm13                                #737.46
        mulsd     %xmm10, %xmm8                                 #731.115
        mulsd     %xmm13, %xmm6                                 #737.46
        addsd     4208+_vmldASinHATab(%rip), %xmm8              #731.137
        addsd     %xmm7, %xmm6                                  #737.77
        mulsd     %xmm10, %xmm8                                 #739.25
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #738.67
        movaps    %xmm4, %xmm13                                 #738.171
        mulsd     %xmm4, %xmm7                                  #738.67
        addsd     4200+_vmldASinHATab(%rip), %xmm8              #741.25
        movsd     %xmm7, -48(%rsp)                              #738.25
        movsd     -48(%rsp), %xmm1                              #738.106
        movsd     4184+_vmldASinHATab(%rip), %xmm10             #740.80
        subsd     %xmm4, %xmm1                                  #738.111
        mulsd     %xmm9, %xmm10                                 #740.80
        addsd     %xmm6, %xmm4                                  #740.134
        movsd     %xmm1, -56(%rsp)                              #738.92
        movaps    %xmm12, %xmm9                                 #744.50
        movsd     -48(%rsp), %xmm0                              #738.136
        movsd     -56(%rsp), %xmm2                              #738.141
        movsd     4176+_vmldASinHATab(%rip), %xmm1              #741.59
        subsd     %xmm2, %xmm0                                  #738.141
        mulsd     %xmm1, %xmm3                                  #742.55
        mulsd     %xmm1, %xmm11                                 #741.59
        mulsd     %xmm4, %xmm8                                  #741.25
        addsd     %xmm3, %xmm9                                  #744.50
        mulsd     %xmm1, %xmm5                                  #743.59
        addsd     %xmm11, %xmm10                                #741.25
        movsd     %xmm0, -48(%rsp)                              #738.122
        addsd     %xmm10, %xmm8                                 #741.25
        movsd     -48(%rsp), %xmm7                              #738.171
        addsd     %xmm5, %xmm8                                  #743.25
        subsd     %xmm7, %xmm13                                 #738.171
        movsd     %xmm13, -56(%rsp)                             #738.147
        movsd     -48(%rsp), %xmm0                              #738.185
        movsd     -56(%rsp), %xmm2                              #738.197
        movsd     %xmm9, -48(%rsp)                              #744.25
        addsd     %xmm2, %xmm6                                  #752.43
        movsd     -48(%rsp), %xmm11                             #744.88
        movsd     4192+_vmldASinHATab(%rip), %xmm4              #746.59
        subsd     %xmm11, %xmm12                                #744.88
        mulsd     %xmm4, %xmm0                                  #747.55
        mulsd     %xmm4, %xmm6                                  #752.43
        addsd     %xmm12, %xmm3                                 #744.116
        movsd     %xmm3, -56(%rsp)                              #744.94
        movsd     -48(%rsp), %xmm3                              #744.133
        movaps    %xmm3, %xmm12                                 #749.48
        movsd     -56(%rsp), %xmm7                              #744.144
        addsd     %xmm0, %xmm12                                 #749.48
        addsd     %xmm7, %xmm8                                  #745.25
        movsd     %xmm12, -48(%rsp)                             #749.25
        movsd     -48(%rsp), %xmm12                             #749.84
        subsd     %xmm12, %xmm3                                 #749.84
        addsd     %xmm3, %xmm0                                  #749.112
        movsd     %xmm0, -56(%rsp)                              #749.90
        movsd     -48(%rsp), %xmm9                              #749.129
        movsd     -56(%rsp), %xmm0                              #749.140
        movb      7(%rdi), %dil                                 #753.86
        addsd     %xmm0, %xmm8                                  #751.25
        andb      $-128, %dil                                   #753.46
        addsd     %xmm8, %xmm9                                  #752.43
        addsd     %xmm9, %xmm6                                  #752.43
        movsd     %xmm6, -40(%rsp)                              #752.25
        movb      -33(%rsp), %dl                                #753.46
        andb      $127, %dl                                     #753.46
        orb       %dil, %dl                                     #753.46
        movb      %dl, -33(%rsp)                                #753.46
        movq      -40(%rsp), %rcx                               #754.30
        movq      %rcx, (%rsi)                                  #754.26
        ret                                                     #754.26
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.6:                        # Preds ..B10.4
                                # Execution count [1.05e-01]
        comisd    4128+_vmldASinHATab(%rip), %xmm12             #758.57
        jb        ..B10.8       # Prob 50%                      #758.57
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax xmm0 xmm12
..B10.7:                        # Preds ..B10.6
                                # Execution count [5.25e-02]
        addsd     %xmm0, %xmm12                                 #760.74
        movsd     %xmm12, -48(%rsp)                             #760.29
        movsd     -48(%rsp), %xmm0                              #761.46
        mulsd     -32(%rsp), %xmm0                              #761.46
        movsd     %xmm0, -40(%rsp)                              #761.29
        movb      -33(%rsp), %dl                                #762.50
        movb      7(%rdi), %dil                                 #762.90
        andb      $127, %dl                                     #762.50
        andb      $-128, %dil                                   #762.50
        orb       %dil, %dl                                     #762.50
        movb      %dl, -33(%rsp)                                #762.50
        movq      -40(%rsp), %rcx                               #763.34
        movq      %rcx, (%rsi)                                  #763.30
        ret                                                     #763.30
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.8:                        # Preds ..B10.6
                                # Execution count [5.25e-02]
        mulsd     %xmm12, %xmm12                                #767.41
        movsd     %xmm12, -48(%rsp)                             #767.29
        movsd     -48(%rsp), %xmm0                              #768.46
        movb      7(%rdi), %dil                                 #769.90
        andb      $-128, %dil                                   #769.50
        addsd     -32(%rsp), %xmm0                              #768.46
        movsd     %xmm0, -40(%rsp)                              #768.29
        movb      -33(%rsp), %dl                                #769.50
        andb      $127, %dl                                     #769.50
        orb       %dil, %dl                                     #769.50
        movb      %dl, -33(%rsp)                                #769.50
        movq      -40(%rsp), %rcx                               #770.34
        movq      %rcx, (%rsi)                                  #770.30
        ret                                                     #770.30
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.9:                        # Preds ..B10.3
                                # Execution count [2.10e-01]
        movaps    %xmm0, %xmm9                                  #776.66
        movzwl    4166+_vmldASinHATab(%rip), %r9d               #780.42
        pxor      %xmm15, %xmm15                                #803.56
        andl      $-32753, %r9d                                 #780.42
        subsd     %xmm12, %xmm9                                 #776.66
        subsd     %xmm0, %xmm15                                 #803.56
        mulsd     %xmm9, %xmm1                                  #777.45
        movsd     %xmm1, -32(%rsp)                              #777.21
        movzwl    -26(%rsp), %ecx                               #778.48
        andl      $32752, %ecx                                  #778.48
        shrl      $4, %ecx                                      #778.48
        addl      $-1023, %ecx                                  #778.70
        movl      %ecx, %r8d                                    #780.81
        movl      %ecx, %edx                                    #782.32
        negl      %r8d                                          #780.81
        addl      $1023, %r8d                                   #780.81
        andl      $2047, %r8d                                   #780.42
        shll      $4, %r8d                                      #780.42
        movsd     %xmm0, -24(%rsp)                              #779.21
        orl       %r8d, %r9d                                    #780.42
        movw      %r9w, -18(%rsp)                               #780.42
        andl      $1, %edx                                      #782.32
        movsd     -24(%rsp), %xmm10                             #781.33
        movl      %edx, %r10d                                   #799.40
        mulsd     %xmm10, %xmm1                                 #781.33
        lea       _vmldASinHATab(%rip), %r8                     #800.45
        movaps    %xmm1, %xmm6                                  #786.39
        movsd     4112+_vmldASinHATab(%rip), %xmm7              #792.53
        addsd     %xmm1, %xmm6                                  #786.39
        jne       ..L159        # Prob 50%                      #790.25
        movaps    %xmm1, %xmm6                                  #790.25
..L159:                                                         #
        mulsd     %xmm6, %xmm7                                  #792.53
        movaps    %xmm6, %xmm8                                  #795.36
        movaps    %xmm7, %xmm11                                 #793.37
        subl      %edx, %ecx                                    #783.27
        movsd     4120+_vmldASinHATab(%rip), %xmm13             #797.54
        subsd     %xmm6, %xmm11                                 #793.37
        addsd     %xmm1, %xmm13                                 #797.54
        movsd     %xmm11, -56(%rsp)                             #793.21
        movsd     -56(%rsp), %xmm12                             #794.37
        movsd     %xmm13, -16(%rsp)                             #797.21
        subsd     %xmm12, %xmm7                                 #794.37
        movl      -16(%rsp), %r11d                              #798.48
        movaps    %xmm7, %xmm4                                  #805.36
        shll      $8, %r10d                                     #799.40
        andl      $511, %r11d                                   #798.80
        addl      %r10d, %r11d                                  #799.40
        subsd     %xmm7, %xmm8                                  #795.36
        movsd     (%r8,%r11,8), %xmm3                           #800.45
        addsd     %xmm8, %xmm4                                  #805.36
        mulsd     %xmm3, %xmm7                                  #810.40
        movaps    %xmm3, %xmm14                                 #801.35
        mulsd     %xmm3, %xmm14                                 #801.35
        mulsd     %xmm14, %xmm4                                 #805.36
        movsd     4496+_vmldASinHATab(%rip), %xmm5              #806.71
        addsd     %xmm15, %xmm4                                 #805.36
        mulsd     %xmm4, %xmm5                                  #806.71
        shrl      $1, %ecx                                      #783.38
        addsd     4488+_vmldASinHATab(%rip), %xmm5              #806.94
        mulsd     %xmm4, %xmm5                                  #806.116
        addl      $1023, %ecx                                   #809.73
        andl      $2047, %ecx                                   #809.42
        addsd     4480+_vmldASinHATab(%rip), %xmm5              #806.139
        mulsd     %xmm4, %xmm5                                  #806.161
        movzwl    4166+_vmldASinHATab(%rip), %r9d               #809.42
        shll      $4, %ecx                                      #809.42
        andl      $-32753, %r9d                                 #809.42
        movsd     %xmm0, -8(%rsp)                               #808.21
        orl       %ecx, %r9d                                    #809.42
        movw      %r9w, -2(%rsp)                                #809.42
        movsd     -8(%rsp), %xmm12                              #811.41
        mulsd     %xmm12, %xmm7                                 #811.41
        addsd     4472+_vmldASinHATab(%rip), %xmm5              #806.184
        mulsd     %xmm4, %xmm5                                  #806.206
        movsd     4104+_vmldASinHATab(%rip), %xmm1              #819.66
        mulsd     %xmm7, %xmm1                                  #819.66
        addsd     4464+_vmldASinHATab(%rip), %xmm5              #807.57
        mulsd     %xmm4, %xmm5                                  #807.79
        movsd     %xmm1, -48(%rsp)                              #819.21
        movsd     -48(%rsp), %xmm0                              #819.105
        movsd     4336+_vmldASinHATab(%rip), %xmm13             #825.256
        subsd     %xmm7, %xmm0                                  #819.110
        addsd     4456+_vmldASinHATab(%rip), %xmm5              #807.102
        addsd     4344+_vmldASinHATab(%rip), %xmm13             #825.256
        mulsd     %xmm4, %xmm5                                  #807.124
        mulsd     %xmm9, %xmm13                                 #825.278
        addsd     4448+_vmldASinHATab(%rip), %xmm5              #807.147
        mulsd     %xmm4, %xmm5                                  #812.39
        mulsd     %xmm6, %xmm5                                  #815.41
        movsd     %xmm0, -56(%rsp)                              #819.91
        addsd     %xmm5, %xmm8                                  #817.41
        movsd     4440+_vmldASinHATab(%rip), %xmm5              #822.77
        mulsd     %xmm9, %xmm5                                  #822.77
        mulsd     %xmm3, %xmm8                                  #817.41
        addsd     4432+_vmldASinHATab(%rip), %xmm5              #822.98
        mulsd     %xmm12, %xmm8                                 #818.41
        mulsd     %xmm9, %xmm5                                  #822.120
        movsd     -48(%rsp), %xmm10                             #819.138
        movsd     -56(%rsp), %xmm2                              #819.143
        movsd     4320+_vmldASinHATab(%rip), %xmm14             #825.176
        subsd     %xmm2, %xmm10                                 #819.143
        addsd     4424+_vmldASinHATab(%rip), %xmm5              #822.141
        addsd     4328+_vmldASinHATab(%rip), %xmm14             #825.176
        mulsd     %xmm9, %xmm5                                  #822.163
        addsd     %xmm13, %xmm14                                #825.278
        addsd     4416+_vmldASinHATab(%rip), %xmm5              #822.184
        mulsd     %xmm9, %xmm14                                 #825.281
        mulsd     %xmm9, %xmm5                                  #822.206
        movsd     %xmm10, -48(%rsp)                             #819.124
        movaps    %xmm9, %xmm2                                  #827.155
        movsd     -48(%rsp), %xmm11                             #819.176
        movsd     4104+_vmldASinHATab(%rip), %xmm3              #827.59
        subsd     %xmm11, %xmm7                                 #819.176
        mulsd     %xmm9, %xmm3                                  #827.59
        addsd     4408+_vmldASinHATab(%rip), %xmm5              #823.45
        mulsd     %xmm9, %xmm5                                  #823.67
        movsd     4304+_vmldASinHATab(%rip), %xmm4              #825.96
        movsd     %xmm7, -56(%rsp)                              #819.149
        movsd     -48(%rsp), %xmm6                              #819.190
        movsd     -56(%rsp), %xmm7                              #819.202
        movsd     %xmm3, -48(%rsp)                              #827.21
        addsd     %xmm7, %xmm8                                  #821.21
        addsd     4400+_vmldASinHATab(%rip), %xmm5              #823.88
        addsd     4312+_vmldASinHATab(%rip), %xmm4              #825.96
        mulsd     %xmm9, %xmm5                                  #823.110
        addsd     %xmm14, %xmm4                                 #825.281
        addsd     4392+_vmldASinHATab(%rip), %xmm5              #823.131
        mulsd     %xmm9, %xmm5                                  #823.153
        movsd     -48(%rsp), %xmm1                              #827.98
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #830.63
        subsd     %xmm9, %xmm1                                  #827.103
        addsd     4384+_vmldASinHATab(%rip), %xmm5              #823.174
        mulsd     %xmm9, %xmm5                                  #823.196
        movsd     %xmm1, -56(%rsp)                              #827.84
        movsd     -48(%rsp), %xmm0                              #827.124
        movsd     -56(%rsp), %xmm15                             #827.129
        addsd     4376+_vmldASinHATab(%rip), %xmm5              #823.217
        subsd     %xmm15, %xmm0                                 #827.129
        mulsd     %xmm9, %xmm5                                  #823.239
        movsd     %xmm0, -48(%rsp)                              #827.110
        movsd     4320+_vmldASinHATab(%rip), %xmm0              #831.68
        addsd     4368+_vmldASinHATab(%rip), %xmm5              #823.260
        mulsd     %xmm9, %xmm5                                  #823.282
        addsd     4360+_vmldASinHATab(%rip), %xmm5              #824.45
        mulsd     %xmm9, %xmm5                                  #824.67
        addsd     4352+_vmldASinHATab(%rip), %xmm5              #824.88
        mulsd     %xmm9, %xmm5                                  #824.110
        mulsd     %xmm9, %xmm5                                  #824.112
        mulsd     %xmm9, %xmm5                                  #824.114
        addsd     %xmm5, %xmm4                                  #826.25
        mulsd     %xmm4, %xmm8                                  #826.25
        movsd     -48(%rsp), %xmm4                              #827.155
        subsd     %xmm4, %xmm2                                  #827.155
        movsd     %xmm2, -56(%rsp)                              #827.135
        movsd     -48(%rsp), %xmm4                              #827.169
        movaps    %xmm4, %xmm13                                 #828.35
        mulsd     %xmm4, %xmm13                                 #828.35
        mulsd     %xmm4, %xmm0                                  #831.68
        addsd     %xmm9, %xmm4                                  #829.45
        mulsd     %xmm13, %xmm7                                 #830.63
        movsd     -56(%rsp), %xmm3                              #827.181
        movsd     %xmm7, -48(%rsp)                              #830.21
        movsd     -48(%rsp), %xmm10                             #830.102
        mulsd     %xmm3, %xmm4                                  #829.45
        subsd     %xmm13, %xmm10                                #830.107
        movsd     %xmm10, -56(%rsp)                             #830.88
        movsd     -48(%rsp), %xmm11                             #830.132
        movsd     -56(%rsp), %xmm1                              #830.137
        movsd     4304+_vmldASinHATab(%rip), %xmm10             #834.77
        subsd     %xmm1, %xmm11                                 #830.137
        movsd     %xmm11, -48(%rsp)                             #830.118
        movaps    %xmm0, %xmm1                                  #833.45
        movsd     -48(%rsp), %xmm12                             #830.167
        movsd     4304+_vmldASinHATab(%rip), %xmm11             #834.146
        subsd     %xmm12, %xmm13                                #830.167
        movsd     %xmm13, -56(%rsp)                             #830.143
        movsd     -48(%rsp), %xmm7                              #830.181
        mulsd     4336+_vmldASinHATab(%rip), %xmm7              #832.68
        movsd     -56(%rsp), %xmm2                              #830.193
        addsd     %xmm7, %xmm1                                  #833.45
        addsd     %xmm2, %xmm4                                  #836.155
        movsd     %xmm1, -48(%rsp)                              #833.21
        movsd     -48(%rsp), %xmm14                             #833.85
        movsd     %xmm7, -16(%rsp)                              #832.21
        subsd     %xmm14, %xmm0                                 #833.85
        mulsd     4336+_vmldASinHATab(%rip), %xmm4              #836.155
        addsd     %xmm0, %xmm7                                  #833.114
        movsd     %xmm7, -56(%rsp)                              #833.91
        movsd     -48(%rsp), %xmm12                             #833.133
        movsd     -56(%rsp), %xmm1                              #833.144
        addsd     %xmm12, %xmm10                                #834.77
        movsd     %xmm10, -48(%rsp)                             #834.21
        movsd     -48(%rsp), %xmm15                             #834.146
        movsd     4104+_vmldASinHATab(%rip), %xmm7              #835.62
        subsd     %xmm15, %xmm11                                #834.146
        movsd     4320+_vmldASinHATab(%rip), %xmm2              #836.102
        addsd     %xmm11, %xmm12                                #834.174
        mulsd     %xmm3, %xmm2                                  #836.102
        movsd     %xmm12, -56(%rsp)                             #834.152
        movsd     -48(%rsp), %xmm15                             #834.191
        mulsd     %xmm15, %xmm7                                 #835.62
        movsd     -56(%rsp), %xmm0                              #834.202
        movsd     %xmm7, -48(%rsp)                              #835.21
        movsd     -48(%rsp), %xmm10                             #835.101
        movsd     4344+_vmldASinHATab(%rip), %xmm3              #837.140
        subsd     %xmm15, %xmm10                                #835.106
        mulsd     %xmm9, %xmm3                                  #837.161
        movsd     %xmm10, -56(%rsp)                             #835.87
        movsd     -48(%rsp), %xmm11                             #835.130
        movsd     -56(%rsp), %xmm13                             #835.135
        subsd     %xmm13, %xmm11                                #835.135
        addsd     4328+_vmldASinHATab(%rip), %xmm3              #837.161
        movsd     %xmm11, -48(%rsp)                             #835.116
        movsd     -48(%rsp), %xmm14                             #835.164
        mulsd     %xmm9, %xmm3                                  #837.164
        subsd     %xmm14, %xmm15                                #835.164
        movsd     %xmm15, -56(%rsp)                             #835.141
        movsd     -48(%rsp), %xmm10                             #835.178
        movsd     -56(%rsp), %xmm7                              #835.190
        addsd     %xmm7, %xmm4                                  #836.50
        addsd     4312+_vmldASinHATab(%rip), %xmm3              #837.164
        addsd     %xmm2, %xmm4                                  #836.57
        addsd     %xmm5, %xmm3                                  #837.168
        addsd     %xmm1, %xmm4                                  #836.102
        addsd     %xmm0, %xmm4                                  #836.155
        movsd     4144+_vmldASinHATab(%rip), %xmm0              #839.53
        addsd     %xmm3, %xmm4                                  #837.21
        mulsd     %xmm6, %xmm4                                  #837.21
        mulsd     %xmm10, %xmm6                                 #838.39
        addsd     %xmm8, %xmm4                                  #837.21
        movaps    %xmm0, %xmm8                                  #839.53
        subsd     %xmm6, %xmm8                                  #839.53
        movsd     %xmm8, -48(%rsp)                              #839.21
        movsd     -48(%rsp), %xmm9                              #839.147
        subsd     %xmm9, %xmm0                                  #839.147
        subsd     %xmm6, %xmm0                                  #839.167
        movsd     %xmm0, -56(%rsp)                              #839.153
        movsd     -48(%rsp), %xmm2                              #839.196
        movsd     -56(%rsp), %xmm1                              #839.207
        movb      7(%rdi), %dil                                 #843.82
        andb      $-128, %dil                                   #843.42
        addsd     4152+_vmldASinHATab(%rip), %xmm1              #841.21
        subsd     %xmm4, %xmm1                                  #840.71
        addsd     %xmm1, %xmm2                                  #842.39
        movsd     %xmm2, -40(%rsp)                              #842.21
        movb      -33(%rsp), %dl                                #843.42
        andb      $127, %dl                                     #843.42
        orb       %dil, %dl                                     #843.42
        movb      %dl, -33(%rsp)                                #843.42
        movq      -40(%rsp), %rcx                               #844.26
        movq      %rcx, (%rsi)                                  #844.22
        ret                                                     #844.22
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.10:                       # Preds ..B10.2
                                # Execution count [4.20e-01]
        movl      $1, %eax                                      #850.17
        pxor      %xmm1, %xmm1                                  #849.41
        pxor      %xmm0, %xmm0                                  #849.41
        divsd     %xmm0, %xmm1                                  #849.41
        movsd     %xmm1, (%rsi)                                 #849.18
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B10.11:                       # Preds ..B10.10 ..B10.12
                                # Execution count [1.00e+00]
        ret                                                     #861.12
                                # LOE
..B10.12:                       # Preds ..B10.1
                                # Execution count [1.60e-01]: Infreq
        movsd     (%rdi), %xmm0                                 #855.19
        divsd     %xmm0, %xmm0                                  #855.24
        movsd     %xmm0, (%rsi)                                 #855.14
        testl     $1048575, 4(%rdi)                             #856.40
        jne       ..B10.11      # Prob 50%                      #856.70
                                # LOE rbx rbp rdi r12 r13 r14 r15 eax
..B10.13:                       # Preds ..B10.12
                                # Execution count [8.00e-02]: Infreq
        cmpl      $1, (%rdi)                                    #858.17
        setb      %al                                           #858.17
        ret                                                     #858.17
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_dasin_ha_cout_rare_internal,@function
	.size	__svml_dasin_ha_cout_rare_internal,.-__svml_dasin_ha_cout_rare_internal
..LN__svml_dasin_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dasin_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dasin_ha_data_internal
	.globl __svml_dasin_ha_data_internal
__svml_dasin_ha_data_internal:
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
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	1071644672
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	805306368
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
	.long	0
	.long	4294967040
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
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	0
	.long	1073741824
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	2570790083
	.long	3213983744
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	1869665325
	.long	1067712512
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294966935
	.long	3216506879
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	4294967197
	.long	1070596095
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	3339630857
	.long	1067480352
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	4158370029
	.long	3213949719
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	3216784302
	.long	1066680132
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	2513723093
	.long	1064982579
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	721210070
	.long	1065941212
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	3414736215
	.long	1066167739
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	4114132270
	.long	1066518236
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3957258973
	.long	1066854556
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3778730174
	.long	1067392114
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	3014936056
	.long	1067899757
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	859300062
	.long	1068708659
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1431655068
	.long	1069897045
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	1413754136
	.long	1073291771
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	856972295
	.long	1016178214
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	0
	.long	4294705152
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	4131758366
	.long	1067674714
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	3936260738
	.long	1066197319
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	354394453
	.long	1067472564
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	709676628
	.long	1067895021
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	3958922090
	.long	1068708761
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
	.long	2160605765
	.long	1069897044
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
	.type	__svml_dasin_ha_data_internal,@object
	.size	__svml_dasin_ha_data_internal,2368
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
