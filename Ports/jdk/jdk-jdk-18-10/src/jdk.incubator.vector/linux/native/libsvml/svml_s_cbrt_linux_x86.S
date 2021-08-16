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
	.file "svml_s_cbrt.c"
	.text
..TXTST0:
.L_2__routine_start___svml_cbrtf4_ha_l9_0:
# -- Begin  __svml_cbrtf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf4_ha_l9
# --- __svml_cbrtf4_ha_l9(__m128)
__svml_cbrtf4_ha_l9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #869.1
        .byte     15                                            #978.540
        .byte     30                                            #978.540
        .byte     250                                           #978.540
	.cfi_startproc
..___tag_value___svml_cbrtf4_ha_l9.1:
..L2:
                                                          #869.1
        pushq     %rbp                                          #869.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #869.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #869.1
        subq      $192, %rsp                                    #869.1
        vmovaps   %xmm0, %xmm5                                  #869.1
        vpsrld    $16, %xmm5, %xmm12                            #932.13
        lea       __svml_scbrt_ha_data_internal(%rip), %rdx     #934.69
        vpand     1408+__svml_scbrt_ha_data_internal(%rip), %xmm12, %xmm4 #933.18
        vmovd     %xmm4, %eax                                   #934.157
        vandps    1792+__svml_scbrt_ha_data_internal(%rip), %xmm5, %xmm13 #937.14
        vandps    1152+__svml_scbrt_ha_data_internal(%rip), %xmm5, %xmm15 #941.9
        vpextrd   $1, %xmm4, %ecx                               #934.284
        movslq    %eax, %rax                                    #934.69
        movslq    %ecx, %rcx                                    #934.196
        vpextrd   $2, %xmm4, %esi                               #934.435
        vpextrd   $3, %xmm4, %edi                               #934.565
        movslq    %esi, %rsi                                    #934.347
        movslq    %edi, %rdi                                    #934.477
        vandps    1216+__svml_scbrt_ha_data_internal(%rip), %xmm5, %xmm1 #942.10
        vpsubd    1856+__svml_scbrt_ha_data_internal(%rip), %xmm13, %xmm14 #938.14
        vorps     1280+__svml_scbrt_ha_data_internal(%rip), %xmm15, %xmm0 #943.9
        vorps     1344+__svml_scbrt_ha_data_internal(%rip), %xmm1, %xmm13 #944.10
        vpcmpgtd  1920+__svml_scbrt_ha_data_internal(%rip), %xmm14, %xmm2 #939.19
        vsubps    %xmm13, %xmm0, %xmm14                         #945.9
        vmovd     (%rdx,%rax), %xmm6                            #934.69
        vmovd     (%rdx,%rcx), %xmm7                            #934.196
        vpunpcklqdq %xmm7, %xmm6, %xmm10                        #934.49
        vpsrld    $7, %xmm12, %xmm7                             #935.25
        vmovd     (%rdx,%rsi), %xmm8                            #934.347
        vmovd     (%rdx,%rdi), %xmm9                            #934.477
        vpunpcklqdq %xmm9, %xmm8, %xmm11                        #934.327
        vpand     1472+__svml_scbrt_ha_data_internal(%rip), %xmm7, %xmm3 #936.22
        vshufps   $136, %xmm11, %xmm10, %xmm6                   #934.29
        vpmulld   1728+__svml_scbrt_ha_data_internal(%rip), %xmm3, %xmm15 #948.16
        vpsubd    1664+__svml_scbrt_ha_data_internal(%rip), %xmm3, %xmm3 #950.24
        vmulps    %xmm14, %xmm6, %xmm1                          #946.9
        vpsrld    $12, %xmm15, %xmm6                            #951.11
        vmovmskps %xmm2, %eax                                   #940.40
        vpsubd    %xmm6, %xmm3, %xmm11                          #952.11
        vpsubd    %xmm6, %xmm11, %xmm12                         #953.11
        vpsubd    %xmm6, %xmm12, %xmm3                          #954.11
        vpand     1536+__svml_scbrt_ha_data_internal(%rip), %xmm7, %xmm9 #949.14
        vpslld    $7, %xmm3, %xmm7                              #955.11
        vpaddd    %xmm7, %xmm4, %xmm4                           #961.19
        vpandn    %xmm4, %xmm2, %xmm2                           #961.19
        vpslld    $1, %xmm2, %xmm2                              #962.19
        vmovd     %xmm2, %r8d                                   #963.151
        vpaddd    1600+__svml_scbrt_ha_data_internal(%rip), %xmm6, %xmm8 #957.11
        vpor      %xmm9, %xmm8, %xmm10                          #957.11
        vpextrd   $1, %xmm2, %r9d                               #963.341
        vpslld    $23, %xmm10, %xmm0                            #958.11
        vpextrd   $2, %xmm2, %r10d                              #963.534
        vpextrd   $3, %xmm2, %r11d                              #963.727
        movslq    %r8d, %r8                                     #963.57
        movslq    %r9d, %r9                                     #963.247
        movslq    %r10d, %r10                                   #963.440
        movslq    %r11d, %r11                                   #963.633
        vmovq     128(%rdx,%r8), %xmm3                          #963.57
        vmovq     128(%rdx,%r9), %xmm4                          #963.247
        vmovq     128(%rdx,%r10), %xmm6                         #963.440
        vmovq     128(%rdx,%r11), %xmm7                         #963.633
        vunpcklps %xmm4, %xmm3, %xmm8                           #963.779
        vunpcklps %xmm7, %xmm6, %xmm9                           #963.814
        vmovlhps  %xmm9, %xmm8, %xmm10                          #963.858
        vshufps   $238, %xmm9, %xmm8, %xmm11                    #963.906
        vmulps    %xmm10, %xmm0, %xmm14                         #964.17
        vmulps    %xmm11, %xmm0, %xmm12                         #965.17
        vmovups   896+__svml_scbrt_ha_data_internal(%rip), %xmm0 #966.40
        vfmadd213ps 960+__svml_scbrt_ha_data_internal(%rip), %xmm1, %xmm0 #968.9
        vfmadd213ps 1024+__svml_scbrt_ha_data_internal(%rip), %xmm1, %xmm0 #970.9
        vfmadd213ps 1088+__svml_scbrt_ha_data_internal(%rip), %xmm1, %xmm0 #972.9
        vmulps    %xmm14, %xmm1, %xmm1                          #973.15
        vmulps    %xmm1, %xmm0, %xmm13                          #974.9
        vaddps    %xmm13, %xmm12, %xmm15                        #975.9
        vaddps    %xmm15, %xmm14, %xmm0                         #976.10
        testl     %eax, %eax                                    #978.52
        jne       ..B1.3        # Prob 5%                       #978.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm5
..B1.2:                         # Preds ..B1.3 ..B1.9 ..B1.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #981.12
        popq      %rbp                                          #981.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #981.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.3:                         # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm5, 64(%rsp)                               #978.193
        vmovups   %xmm0, 128(%rsp)                              #978.264
        je        ..B1.2        # Prob 95%                      #978.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B1.6:                         # Preds ..B1.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #978.448
        movq      %r12, 8(%rsp)                                 #978.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #978.448
        movq      %r13, (%rsp)                                  #978.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #978.448
                                # LOE rbx r12 r14 r15 r13d
..B1.7:                         # Preds ..B1.8 ..B1.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #978.511
        jc        ..B1.10       # Prob 5%                       #978.511
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.10 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #978.464
        cmpl      $4, %r12d                                     #978.459
        jl        ..B1.7        # Prob 82%                      #978.459
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #978.666
        jmp       ..B1.2        # Prob 100%                     #978.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.10:                        # Preds ..B1.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #978.540
        lea       128(%rsp,%r12,4), %rsi                        #978.540
..___tag_value___svml_cbrtf4_ha_l9.19:
#       __svml_scbrt_ha_cout_rare_internal(const float *, float *)
        call      __svml_scbrt_ha_cout_rare_internal            #978.540
..___tag_value___svml_cbrtf4_ha_l9.20:
        jmp       ..B1.8        # Prob 100%                     #978.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf4_ha_l9,@function
	.size	__svml_cbrtf4_ha_l9,.-__svml_cbrtf4_ha_l9
..LN__svml_cbrtf4_ha_l9.0:
	.data
# -- End  __svml_cbrtf4_ha_l9
	.text
.L_2__routine_start___svml_cbrtf4_ha_ex_1:
# -- Begin  __svml_cbrtf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf4_ha_ex
# --- __svml_cbrtf4_ha_ex(__m128)
__svml_cbrtf4_ha_ex:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #986.1
        .byte     15                                            #1095.540
        .byte     30                                            #1095.540
        .byte     250                                           #1095.540
	.cfi_startproc
..___tag_value___svml_cbrtf4_ha_ex.22:
..L23:
                                                         #986.1
        pushq     %rbp                                          #986.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #986.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #986.1
        subq      $192, %rsp                                    #986.1
        movaps    %xmm0, %xmm5                                  #986.1
        movaps    %xmm5, %xmm2                                  #1049.11
        lea       __svml_scbrt_ha_data_internal(%rip), %rdx     #1051.68
        movdqu    1408+__svml_scbrt_ha_data_internal(%rip), %xmm3 #1050.16
        psrld     $16, %xmm2                                    #1049.11
        pand      %xmm2, %xmm3                                  #1050.16
        psrld     $7, %xmm2                                     #1052.23
        pshufd    $1, %xmm3, %xmm6                              #1051.299
        movd      %xmm3, %eax                                   #1051.156
        movd      %xmm6, %ecx                                   #1051.281
        pshufd    $2, %xmm3, %xmm8                              #1051.484
        pshufd    $3, %xmm3, %xmm9                              #1051.649
        movslq    %eax, %rax                                    #1051.68
        movslq    %ecx, %rcx                                    #1051.193
        movd      %xmm8, %esi                                   #1051.466
        movd      %xmm9, %edi                                   #1051.631
        movdqu    1472+__svml_scbrt_ha_data_internal(%rip), %xmm1 #1053.20
        movups    1152+__svml_scbrt_ha_data_internal(%rip), %xmm13 #1058.9
        pand      %xmm2, %xmm1                                  #1053.20
        movups    1216+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1059.10
        andps     %xmm5, %xmm13                                 #1058.9
        movdqu    1728+__svml_scbrt_ha_data_internal(%rip), %xmm15 #1064.46
        andps     %xmm5, %xmm12                                 #1059.10
        movslq    %esi, %rsi                                    #1051.378
        movdqa    %xmm15, %xmm6                                 #1065.188
        movslq    %edi, %rdi                                    #1051.543
        psrlq     $32, %xmm15                                   #1065.145
        movd      (%rdx,%rax), %xmm4                            #1051.68
        movd      (%rdx,%rcx), %xmm7                            #1051.193
        punpcklqdq %xmm7, %xmm4                                 #1051.49
        movdqa    %xmm1, %xmm7                                  #1067.22
        orps      1280+__svml_scbrt_ha_data_internal(%rip), %xmm13 #1060.9
        orps      1344+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1061.10
        pmuludq   %xmm1, %xmm6                                  #1065.188
        psrlq     $32, %xmm1                                    #1065.93
        subps     %xmm12, %xmm13                                #1062.9
        pmuludq   %xmm15, %xmm1                                 #1065.243
        movd      (%rdx,%rsi), %xmm11                           #1051.378
        psllq     $32, %xmm1                                    #1065.305
        movd      (%rdx,%rdi), %xmm10                           #1051.543
        punpcklqdq %xmm10, %xmm11                               #1051.359
        shufps    $136, %xmm11, %xmm4                           #1051.29
        mulps     %xmm13, %xmm4                                 #1063.9
        pand      .L_2il0floatpacket.46(%rip), %xmm6            #1065.273
        por       %xmm1, %xmm6                                  #1065.343
        psubd     1664+__svml_scbrt_ha_data_internal(%rip), %xmm7 #1067.22
        psrld     $12, %xmm6                                    #1068.9
        psubd     %xmm6, %xmm7                                  #1069.9
        movups    896+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1083.40
        psubd     %xmm6, %xmm7                                  #1070.9
        movdqu    1792+__svml_scbrt_ha_data_internal(%rip), %xmm0 #1054.12
        psubd     %xmm6, %xmm7                                  #1071.9
        mulps     %xmm4, %xmm12                                 #1085.21
        pand      %xmm5, %xmm0                                  #1054.12
        psubd     1856+__svml_scbrt_ha_data_internal(%rip), %xmm0 #1055.12
        pslld     $7, %xmm7                                     #1072.9
        pcmpgtd   1920+__svml_scbrt_ha_data_internal(%rip), %xmm0 #1056.17
        paddd     %xmm7, %xmm3                                  #1078.17
        movmskps  %xmm0, %eax                                   #1057.40
        pandn     %xmm3, %xmm0                                  #1078.17
        movdqu    1536+__svml_scbrt_ha_data_internal(%rip), %xmm14 #1066.12
        pslld     $1, %xmm0                                     #1079.17
        movdqu    1600+__svml_scbrt_ha_data_internal(%rip), %xmm1 #1074.9
        pand      %xmm14, %xmm2                                 #1066.12
        paddd     %xmm6, %xmm1                                  #1074.9
        addps     960+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1085.9
        movd      %xmm0, %r8d                                   #1080.151
        por       %xmm2, %xmm1                                  #1074.9
        pshufd    $1, %xmm0, %xmm2                              #1080.357
        pslld     $23, %xmm1                                    #1075.9
        movd      %xmm2, %r9d                                   #1080.339
        pshufd    $2, %xmm0, %xmm3                              #1080.585
        pshufd    $3, %xmm0, %xmm0                              #1080.813
        movd      %xmm3, %r10d                                  #1080.567
        movd      %xmm0, %r11d                                  #1080.795
        mulps     %xmm4, %xmm12                                 #1087.21
        movslq    %r8d, %r8                                     #1080.57
        addps     1024+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1087.9
        movslq    %r9d, %r9                                     #1080.245
        movslq    %r10d, %r10                                   #1080.473
        movslq    %r11d, %r11                                   #1080.701
        movq      128(%rdx,%r8), %xmm11                         #1080.57
        movq      128(%rdx,%r9), %xmm8                          #1080.245
        movq      128(%rdx,%r10), %xmm10                        #1080.473
        movq      128(%rdx,%r11), %xmm9                         #1080.701
        unpcklps  %xmm8, %xmm11                                 #1080.882
        unpcklps  %xmm9, %xmm10                                 #1080.917
        movaps    %xmm11, %xmm0                                 #1080.961
        movlhps   %xmm10, %xmm0                                 #1080.961
        mulps     %xmm1, %xmm0                                  #1081.17
        mulps     %xmm4, %xmm12                                 #1089.21
        mulps     %xmm0, %xmm4                                  #1090.15
        addps     1088+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1089.9
        shufps    $238, %xmm10, %xmm11                          #1080.1009
        mulps     %xmm11, %xmm1                                 #1082.17
        mulps     %xmm4, %xmm12                                 #1091.9
        addps     %xmm12, %xmm1                                 #1092.9
        addps     %xmm1, %xmm0                                  #1093.10
        testl     %eax, %eax                                    #1095.52
        jne       ..B2.3        # Prob 5%                       #1095.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm5
..B2.2:                         # Preds ..B2.9 ..B2.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1098.12
        popq      %rbp                                          #1098.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1098.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.3:                         # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm5, 64(%rsp)                               #1095.193
        movups    %xmm0, 128(%rsp)                              #1095.264
                                # LOE rbx r12 r13 r14 r15 eax
..B2.6:                         # Preds ..B2.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1095.448
        movq      %r12, 8(%rsp)                                 #1095.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1095.448
        movq      %r13, (%rsp)                                  #1095.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1095.448
                                # LOE rbx r12 r14 r15 r13d
..B2.7:                         # Preds ..B2.8 ..B2.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1095.511
        jc        ..B2.10       # Prob 5%                       #1095.511
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.10 ..B2.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1095.464
        cmpl      $4, %r12d                                     #1095.459
        jl        ..B2.7        # Prob 82%                      #1095.459
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #1095.666
        jmp       ..B2.2        # Prob 100%                     #1095.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B2.10:                        # Preds ..B2.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1095.540
        lea       128(%rsp,%r12,4), %rsi                        #1095.540
..___tag_value___svml_cbrtf4_ha_ex.40:
#       __svml_scbrt_ha_cout_rare_internal(const float *, float *)
        call      __svml_scbrt_ha_cout_rare_internal            #1095.540
..___tag_value___svml_cbrtf4_ha_ex.41:
        jmp       ..B2.8        # Prob 100%                     #1095.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf4_ha_ex,@function
	.size	__svml_cbrtf4_ha_ex,.-__svml_cbrtf4_ha_ex
..LN__svml_cbrtf4_ha_ex.1:
	.data
# -- End  __svml_cbrtf4_ha_ex
	.text
.L_2__routine_start___svml_cbrtf4_ha_e9_2:
# -- Begin  __svml_cbrtf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf4_ha_e9
# --- __svml_cbrtf4_ha_e9(__m128)
__svml_cbrtf4_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1103.1
        .byte     15                                            #1212.540
        .byte     30                                            #1212.540
        .byte     250                                           #1212.540
	.cfi_startproc
..___tag_value___svml_cbrtf4_ha_e9.43:
..L44:
                                                         #1103.1
        pushq     %rbp                                          #1103.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1103.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1103.1
        subq      $192, %rsp                                    #1103.1
        movaps    %xmm0, %xmm8                                  #1166.17
        movups    1408+__svml_scbrt_ha_data_internal(%rip), %xmm11 #1167.22
        psrld     $16, %xmm8                                    #1166.17
        pand      %xmm8, %xmm11                                 #1167.22
        lea       __svml_scbrt_ha_data_internal(%rip), %rdx     #1168.69
        movd      %xmm11, %eax                                  #1168.157
        psrld     $7, %xmm8                                     #1169.29
        movups    1152+__svml_scbrt_ha_data_internal(%rip), %xmm6 #1175.9
        movups    1216+__svml_scbrt_ha_data_internal(%rip), %xmm5 #1176.10
        andps     %xmm0, %xmm6                                  #1175.9
        pextrd    $1, %xmm11, %ecx                              #1168.284
        andps     %xmm0, %xmm5                                  #1176.10
        pextrd    $2, %xmm11, %esi                              #1168.435
        pextrd    $3, %xmm11, %edi                              #1168.565
        movslq    %eax, %rax                                    #1168.69
        movslq    %ecx, %rcx                                    #1168.196
        movslq    %esi, %rsi                                    #1168.347
        movslq    %edi, %rdi                                    #1168.477
        orps      1280+__svml_scbrt_ha_data_internal(%rip), %xmm6 #1177.9
        orps      1344+__svml_scbrt_ha_data_internal(%rip), %xmm5 #1178.10
        movd      (%rdx,%rax), %xmm1                            #1168.69
        subps     %xmm5, %xmm6                                  #1179.9
        movd      (%rdx,%rcx), %xmm2                            #1168.196
        movd      (%rdx,%rsi), %xmm4                            #1168.347
        movd      (%rdx,%rdi), %xmm3                            #1168.477
        movups    1472+__svml_scbrt_ha_data_internal(%rip), %xmm10 #1170.26
        punpcklqdq %xmm2, %xmm1                                 #1168.49
        pand      %xmm8, %xmm10                                 #1170.26
        punpcklqdq %xmm3, %xmm4                                 #1168.327
        movups    1728+__svml_scbrt_ha_data_internal(%rip), %xmm9 #1182.20
        shufps    $136, %xmm4, %xmm1                            #1168.29
        pmulld    %xmm10, %xmm9                                 #1182.20
        mulps     %xmm6, %xmm1                                  #1180.9
        psrld     $12, %xmm9                                    #1185.15
        psubd     1664+__svml_scbrt_ha_data_internal(%rip), %xmm10 #1184.28
        psubd     %xmm9, %xmm10                                 #1186.15
        movups    1600+__svml_scbrt_ha_data_internal(%rip), %xmm2 #1191.15
        psubd     %xmm9, %xmm10                                 #1187.15
        paddd     %xmm9, %xmm2                                  #1191.15
        psubd     %xmm9, %xmm10                                 #1188.15
        movups    896+__svml_scbrt_ha_data_internal(%rip), %xmm9 #1200.40
        pslld     $7, %xmm10                                    #1189.15
        mulps     %xmm1, %xmm9                                  #1202.21
        paddd     %xmm10, %xmm11                                #1195.23
        movups    1792+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1171.18
        andps     %xmm0, %xmm12                                 #1171.18
        addps     960+__svml_scbrt_ha_data_internal(%rip), %xmm9 #1202.9
        psubd     1856+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1172.18
        pcmpgtd   1920+__svml_scbrt_ha_data_internal(%rip), %xmm12 #1173.23
        movmskps  %xmm12, %eax                                  #1174.40
        pandn     %xmm11, %xmm12                                #1195.23
        pslld     $1, %xmm12                                    #1196.23
        movups    1536+__svml_scbrt_ha_data_internal(%rip), %xmm7 #1155.22
        movd      %xmm12, %r8d                                  #1197.151
        pand      %xmm7, %xmm8                                  #1183.18
        por       %xmm8, %xmm2                                  #1191.15
        pslld     $23, %xmm2                                    #1192.15
        pextrd    $1, %xmm12, %r9d                              #1197.341
        mulps     %xmm1, %xmm9                                  #1204.21
        movslq    %r8d, %r8                                     #1197.57
        addps     1024+__svml_scbrt_ha_data_internal(%rip), %xmm9 #1204.9
        movslq    %r9d, %r9                                     #1197.247
        pextrd    $2, %xmm12, %r10d                             #1197.534
        pextrd    $3, %xmm12, %r11d                             #1197.727
        movslq    %r10d, %r10                                   #1197.440
        movslq    %r11d, %r11                                   #1197.633
        movq      128(%rdx,%r8), %xmm8                          #1197.57
        movq      128(%rdx,%r9), %xmm13                         #1197.247
        movq      128(%rdx,%r10), %xmm15                        #1197.440
        movq      128(%rdx,%r11), %xmm14                        #1197.633
        unpcklps  %xmm13, %xmm8                                 #1197.779
        unpcklps  %xmm14, %xmm15                                #1197.814
        movaps    %xmm8, %xmm3                                  #1197.858
        movlhps   %xmm15, %xmm3                                 #1197.858
        mulps     %xmm2, %xmm3                                  #1198.17
        mulps     %xmm1, %xmm9                                  #1206.21
        mulps     %xmm3, %xmm1                                  #1207.15
        addps     1088+__svml_scbrt_ha_data_internal(%rip), %xmm9 #1206.9
        shufps    $238, %xmm15, %xmm8                           #1197.906
        mulps     %xmm8, %xmm2                                  #1199.17
        mulps     %xmm1, %xmm9                                  #1208.9
        addps     %xmm9, %xmm2                                  #1209.9
        addps     %xmm2, %xmm3                                  #1210.10
        testl     %eax, %eax                                    #1212.52
        jne       ..B3.3        # Prob 5%                       #1212.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm3
..B3.2:                         # Preds ..B3.9 ..B3.1
                                # Execution count [1.00e+00]
        movaps    %xmm3, %xmm0                                  #1215.12
        movq      %rbp, %rsp                                    #1215.12
        popq      %rbp                                          #1215.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1215.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.3:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #1212.193
        movups    %xmm3, 128(%rsp)                              #1212.264
                                # LOE rbx r12 r13 r14 r15 eax
..B3.6:                         # Preds ..B3.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1212.448
        movq      %r12, 8(%rsp)                                 #1212.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #1212.448
        movq      %r13, (%rsp)                                  #1212.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #1212.448
                                # LOE rbx r12 r14 r15 r13d
..B3.7:                         # Preds ..B3.8 ..B3.6
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1212.511
        jc        ..B3.10       # Prob 5%                       #1212.511
                                # LOE rbx r12 r14 r15 r13d
..B3.8:                         # Preds ..B3.10 ..B3.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1212.464
        cmpl      $4, %r12d                                     #1212.459
        jl        ..B3.7        # Prob 82%                      #1212.459
                                # LOE rbx r12 r14 r15 r13d
..B3.9:                         # Preds ..B3.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm3                              #1212.666
        jmp       ..B3.2        # Prob 100%                     #1212.666
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm3
..B3.10:                        # Preds ..B3.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1212.540
        lea       128(%rsp,%r12,4), %rsi                        #1212.540
..___tag_value___svml_cbrtf4_ha_e9.61:
#       __svml_scbrt_ha_cout_rare_internal(const float *, float *)
        call      __svml_scbrt_ha_cout_rare_internal            #1212.540
..___tag_value___svml_cbrtf4_ha_e9.62:
        jmp       ..B3.8        # Prob 100%                     #1212.540
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf4_ha_e9,@function
	.size	__svml_cbrtf4_ha_e9,.-__svml_cbrtf4_ha_e9
..LN__svml_cbrtf4_ha_e9.2:
	.data
# -- End  __svml_cbrtf4_ha_e9
	.text
.L_2__routine_start___svml_cbrtf16_ha_z0_3:
# -- Begin  __svml_cbrtf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf16_ha_z0
# --- __svml_cbrtf16_ha_z0(__m512)
__svml_cbrtf16_ha_z0:
# parameter 1: %zmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1220.1
        .byte     15                                            #1303.12
        .byte     30                                            #1303.12
        .byte     250                                           #1303.12
	.cfi_startproc
..___tag_value___svml_cbrtf16_ha_z0.64:
..L65:
                                                         #1220.1
        vgetexpps {sae}, %zmm0, %zmm4                           #1262.25
        vmovups   384+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm5 #1263.55
        vgetmantps $0, {sae}, %zmm0, %zmm11                     #1261.28
        vmovups   512+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm6 #1268.58
        vmovups   576+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm7 #1269.55
        vmovups   640+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm8 #1272.55
        vmovups   256+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm15 #1283.294
        vmovups   768+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm14 #1286.62
        vrcp14ps  %zmm11, %zmm10                                #1267.24
        vaddps    {rn-sae}, %zmm5, %zmm4, %zmm9                 #1264.25
        vandps    448+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm0, %zmm3 #1266.21
        vrndscaleps $88, {sae}, %zmm10, %zmm12                  #1277.24
        vfmsub231ps {rn-sae}, %zmm9, %zmm6, %zmm7               #1270.26
        vmovups   704+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm4 #1279.19
        vmovups   832+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm5 #1288.23
        vpsrld    $19, %zmm12, %zmm13                           #1280.46
        vrndscaleps $9, {sae}, %zmm7, %zmm2                     #1271.26
        vfmsub231ps {rn-sae}, %zmm12, %zmm11, %zmm4             #1279.19
        vmovups   128+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm11 #1282.294
        vpermt2ps 320+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm13, %zmm15 #1283.294
        vfnmadd231ps {rn-sae}, %zmm2, %zmm8, %zmm9              #1273.26
        vfmadd231ps {rn-sae}, %zmm4, %zmm14, %zmm5              #1288.23
        vmovups   896+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm14 #1290.62
        vpermps   __svml_scbrt_ha_data_internal_avx512(%rip), %zmm9, %zmm0 #1275.151
        vpermt2ps 192+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm13, %zmm11 #1282.294
        vpermps   64+__svml_scbrt_ha_data_internal_avx512(%rip), %zmm9, %zmm1 #1276.151
        vfmadd213ps {rn-sae}, %zmm14, %zmm4, %zmm5              #1291.23
        vmulps    {rn-sae}, %zmm11, %zmm0, %zmm6                #1284.27
        vmovaps   %zmm0, %zmm13                                 #1285.22
        vfmsub213ps {rn-sae}, %zmm6, %zmm11, %zmm13             #1285.22
        vfmadd213ps {rn-sae}, %zmm13, %zmm15, %zmm0             #1289.22
        vfmadd213ps {rn-sae}, %zmm0, %zmm11, %zmm1              #1292.22
        vmulps    {rn-sae}, %zmm4, %zmm6, %zmm0                 #1293.26
        vfmadd213ps {rn-sae}, %zmm1, %zmm5, %zmm0               #1294.22
        vaddps    {rn-sae}, %zmm6, %zmm0, %zmm1                 #1296.24
        vscalefps {rn-sae}, %zmm2, %zmm1, %zmm2                 #1297.22
        vorps     %zmm3, %zmm2, %zmm0                           #1298.22
        ret                                                     #1303.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf16_ha_z0,@function
	.size	__svml_cbrtf16_ha_z0,.-__svml_cbrtf16_ha_z0
..LN__svml_cbrtf16_ha_z0.3:
	.data
# -- End  __svml_cbrtf16_ha_z0
	.text
.L_2__routine_start___svml_cbrtf8_ha_l9_4:
# -- Begin  __svml_cbrtf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf8_ha_l9
# --- __svml_cbrtf8_ha_l9(__m256)
__svml_cbrtf8_ha_l9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1308.1
        .byte     15                                            #1417.546
        .byte     30                                            #1417.546
        .byte     250                                           #1417.546
	.cfi_startproc
..___tag_value___svml_cbrtf8_ha_l9.67:
..L68:
                                                         #1308.1
        pushq     %rbp                                          #1308.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1308.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1308.1
        subq      $192, %rsp                                    #1308.1
        lea       __svml_scbrt_ha_data_internal(%rip), %rdx     #1373.115
        vmovaps   %ymm0, %ymm7                                  #1308.1
        vpsrld    $16, %ymm7, %ymm5                             #1371.13
        vpand     1408+__svml_scbrt_ha_data_internal(%rip), %ymm5, %ymm6 #1372.18
        vextractf128 $1, %ymm6, %xmm0                           #1373.936
        vmovd     %xmm6, %eax                                   #1373.203
        vmovd     %xmm0, %r8d                                   #1373.917
        vpextrd   $1, %xmm0, %r9d                               #1373.1077
        vpextrd   $2, %xmm0, %r10d                              #1373.1261
        vpextrd   $3, %xmm0, %r11d                              #1373.1424
        vpextrd   $1, %xmm6, %ecx                               #1373.358
        movslq    %r8d, %r8                                     #1373.829
        movslq    %r9d, %r9                                     #1373.989
        movslq    %r10d, %r10                                   #1373.1173
        movslq    %r11d, %r11                                   #1373.1336
        movslq    %eax, %rax                                    #1373.115
        movslq    %ecx, %rcx                                    #1373.270
        vmovd     (%rdx,%r8), %xmm15                            #1373.829
        vmovd     (%rdx,%r9), %xmm1                             #1373.989
        vmovd     (%rdx,%r10), %xmm3                            #1373.1173
        vmovd     (%rdx,%r11), %xmm4                            #1373.1336
        vmovd     (%rdx,%rax), %xmm8                            #1373.115
        vmovd     (%rdx,%rcx), %xmm9                            #1373.270
        vpunpcklqdq %xmm1, %xmm15, %xmm2                        #1373.809
        vpunpcklqdq %xmm4, %xmm3, %xmm15                        #1373.1153
        vpunpcklqdq %xmm9, %xmm8, %xmm12                        #1373.95
        vshufps   $136, %xmm15, %xmm2, %xmm8                    #1373.789
        vpsrld    $7, %ymm5, %ymm2                              #1374.25
        vpextrd   $2, %xmm6, %esi                               #1373.537
        vpextrd   $3, %xmm6, %edi                               #1373.695
        movslq    %esi, %rsi                                    #1373.449
        movslq    %edi, %rdi                                    #1373.607
        vpand     1472+__svml_scbrt_ha_data_internal(%rip), %ymm2, %ymm4 #1375.22
        vpand     1536+__svml_scbrt_ha_data_internal(%rip), %ymm2, %ymm2 #1388.14
        vmovd     (%rdx,%rsi), %xmm10                           #1373.449
        vmovd     (%rdx,%rdi), %xmm11                           #1373.607
        vpunpcklqdq %xmm11, %xmm10, %xmm13                      #1373.429
        vandps    1792+__svml_scbrt_ha_data_internal(%rip), %ymm7, %ymm5 #1376.14
        vandps    1152+__svml_scbrt_ha_data_internal(%rip), %ymm7, %ymm10 #1380.9
        vandps    1216+__svml_scbrt_ha_data_internal(%rip), %ymm7, %ymm11 #1381.10
        vpsubd    1856+__svml_scbrt_ha_data_internal(%rip), %ymm5, %ymm9 #1377.14
        vshufps   $136, %xmm13, %xmm12, %xmm14                  #1373.75
        vorps     1280+__svml_scbrt_ha_data_internal(%rip), %ymm10, %ymm12 #1382.9
        vorps     1344+__svml_scbrt_ha_data_internal(%rip), %ymm11, %ymm13 #1383.10
        vpmulld   1728+__svml_scbrt_ha_data_internal(%rip), %ymm4, %ymm5 #1387.16
        vpsubd    1664+__svml_scbrt_ha_data_internal(%rip), %ymm4, %ymm4 #1389.24
        vsubps    %ymm13, %ymm12, %ymm0                         #1384.9
        vpsrld    $12, %ymm5, %ymm12                            #1390.11
        vpsubd    %ymm12, %ymm4, %ymm10                         #1391.11
        vpsubd    %ymm12, %ymm10, %ymm11                        #1392.11
        vpsubd    %ymm12, %ymm11, %ymm13                        #1393.11
        vpcmpgtd  1920+__svml_scbrt_ha_data_internal(%rip), %ymm9, %ymm3 #1378.19
        vmovmskps %ymm3, %eax                                   #1379.41
        vinsertf128 $1, %xmm8, %ymm14, %ymm1                    #1373.11
        vpslld    $7, %ymm13, %ymm14                            #1394.11
        vpaddd    1600+__svml_scbrt_ha_data_internal(%rip), %ymm12, %ymm8 #1395.11
        vmulps    %ymm0, %ymm1, %ymm1                           #1385.9
        vpaddd    %ymm14, %ymm6, %ymm6                          #1399.19
        vpor      %ymm2, %ymm8, %ymm9                           #1396.11
        vpandn    %ymm6, %ymm3, %ymm2                           #1400.19
        vpslld    $23, %ymm9, %ymm0                             #1397.11
        vpslld    $1, %ymm2, %ymm3                              #1401.19
        vmovd     %xmm3, %ecx                                   #1402.154
        vextractf128 $1, %ymm3, %xmm4                           #1402.1066
        vpextrd   $1, %xmm3, %esi                               #1402.375
        movslq    %ecx, %rcx                                    #1402.60
        movslq    %esi, %rsi                                    #1402.281
        vmovd     %xmm4, %r9d                                   #1402.1047
        vmovq     128(%rdx,%rcx), %xmm5                         #1402.60
        vpextrd   $2, %xmm3, %edi                               #1402.599
        vpextrd   $3, %xmm3, %r8d                               #1402.823
        vmovq     128(%rdx,%rsi), %xmm8                         #1402.281
        vpextrd   $1, %xmm4, %r10d                              #1402.1273
        vpextrd   $2, %xmm4, %ecx                               #1402.1502
        vpextrd   $3, %xmm4, %esi                               #1402.1731
        movslq    %edi, %rdi                                    #1402.505
        movslq    %r8d, %r8                                     #1402.729
        movslq    %r9d, %r9                                     #1402.953
        movslq    %r10d, %r10                                   #1402.1179
        movslq    %ecx, %rcx                                    #1402.1408
        movslq    %esi, %rsi                                    #1402.1637
        vmovq     128(%rdx,%rdi), %xmm6                         #1402.505
        vmovq     128(%rdx,%r8), %xmm9                          #1402.729
        vmovq     128(%rdx,%r9), %xmm10                         #1402.953
        vmovq     128(%rdx,%r10), %xmm12                        #1402.1179
        vmovq     128(%rdx,%rcx), %xmm11                        #1402.1408
        vmovq     128(%rdx,%rsi), %xmm13                        #1402.1637
        vunpcklps %xmm6, %xmm5, %xmm14                          #1402.1830
        vunpcklps %xmm9, %xmm8, %xmm2                           #1402.1885
        vunpcklps %xmm11, %xmm10, %xmm15                        #1402.1940
        vunpcklps %xmm13, %xmm12, %xmm3                         #1402.1995
        vinsertf128 $1, %xmm15, %ymm14, %ymm4                   #1402.2058
        vinsertf128 $1, %xmm3, %ymm2, %ymm5                     #1402.2166
        vunpcklps %ymm5, %ymm4, %ymm6                           #1402.2261
        vunpckhps %ymm5, %ymm4, %ymm8                           #1402.2339
        vmulps    %ymm6, %ymm0, %ymm9                           #1403.17
        vmulps    %ymm8, %ymm0, %ymm8                           #1404.17
        vmovups   896+__svml_scbrt_ha_data_internal(%rip), %ymm0 #1405.43
        vfmadd213ps 960+__svml_scbrt_ha_data_internal(%rip), %ymm1, %ymm0 #1407.9
        vfmadd213ps 1024+__svml_scbrt_ha_data_internal(%rip), %ymm1, %ymm0 #1409.9
        vfmadd213ps 1088+__svml_scbrt_ha_data_internal(%rip), %ymm1, %ymm0 #1411.9
        vmulps    %ymm9, %ymm1, %ymm1                           #1412.15
        vmulps    %ymm1, %ymm0, %ymm0                           #1413.9
        vaddps    %ymm0, %ymm8, %ymm1                           #1414.9
        vaddps    %ymm1, %ymm9, %ymm0                           #1415.10
        testl     %eax, %eax                                    #1417.52
        jne       ..B5.3        # Prob 5%                       #1417.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm7
..B5.2:                         # Preds ..B5.3 ..B5.9 ..B5.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1420.12
        popq      %rbp                                          #1420.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1420.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.3:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm7, 64(%rsp)                               #1417.196
        vmovups   %ymm0, 128(%rsp)                              #1417.270
        je        ..B5.2        # Prob 95%                      #1417.374
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B5.6:                         # Preds ..B5.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1417.454
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
        btl       %r12d, %r13d                                  #1417.517
        jc        ..B5.10       # Prob 5%                       #1417.517
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.10 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1417.470
        cmpl      $8, %r12d                                     #1417.465
        jl        ..B5.7        # Prob 82%                      #1417.465
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1417.675
        jmp       ..B5.2        # Prob 100%                     #1417.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.10:                        # Preds ..B5.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1417.546
        lea       128(%rsp,%r12,4), %rsi                        #1417.546
..___tag_value___svml_cbrtf8_ha_l9.85:
#       __svml_scbrt_ha_cout_rare_internal(const float *, float *)
        call      __svml_scbrt_ha_cout_rare_internal            #1417.546
..___tag_value___svml_cbrtf8_ha_l9.86:
        jmp       ..B5.8        # Prob 100%                     #1417.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf8_ha_l9,@function
	.size	__svml_cbrtf8_ha_l9,.-__svml_cbrtf8_ha_l9
..LN__svml_cbrtf8_ha_l9.4:
	.data
# -- End  __svml_cbrtf8_ha_l9
	.text
.L_2__routine_start___svml_cbrtf8_ha_e9_5:
# -- Begin  __svml_cbrtf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cbrtf8_ha_e9
# --- __svml_cbrtf8_ha_e9(__m256)
__svml_cbrtf8_ha_e9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1425.1
        .byte     15                                            #1534.546
        .byte     30                                            #1534.546
        .byte     250                                           #1534.546
	.cfi_startproc
..___tag_value___svml_cbrtf8_ha_e9.88:
..L89:
                                                         #1425.1
        pushq     %rbp                                          #1425.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1425.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1425.1
        subq      $192, %rsp                                    #1425.1
        lea       __svml_scbrt_ha_data_internal(%rip), %rdx     #1490.115
        vmovaps   %ymm0, %ymm15                                 #1425.1
        vmovups   1408+__svml_scbrt_ha_data_internal(%rip), %xmm10 #1475.26
        vmovups   1472+__svml_scbrt_ha_data_internal(%rip), %xmm0 #1476.22
        vmovups   1792+__svml_scbrt_ha_data_internal(%rip), %xmm3 #1484.21
        vmovups   1856+__svml_scbrt_ha_data_internal(%rip), %xmm4 #1485.22
        vmovups   1920+__svml_scbrt_ha_data_internal(%rip), %xmm5 #1486.22
        vmovups   1536+__svml_scbrt_ha_data_internal(%rip), %xmm1 #1477.22
        vmovups   1600+__svml_scbrt_ha_data_internal(%rip), %xmm2 #1479.18
        vpsrld    $16, %xmm15, %xmm7                            #1488.17
        vextractf128 $1, %ymm15, %xmm6                          #1487.101
        vpand     %xmm10, %xmm7, %xmm9                          #1489.22
        vmovd     %xmm9, %eax                                   #1490.203
        vpsrld    $16, %xmm6, %xmm8                             #1488.68
        vpextrd   $1, %xmm9, %ecx                               #1490.336
        vpand     %xmm10, %xmm8, %xmm10                         #1489.99
        movslq    %eax, %rax                                    #1490.115
        vpsrld    $7, %xmm8, %xmm8                              #1491.93
        movslq    %ecx, %rcx                                    #1490.248
        vpextrd   $2, %xmm9, %esi                               #1490.493
        vpextrd   $3, %xmm9, %edi                               #1490.629
        vmovd     %xmm10, %r8d                                  #1490.829
        vmovd     (%rdx,%rax), %xmm14                           #1490.115
        vmovd     (%rdx,%rcx), %xmm12                           #1490.248
        movslq    %esi, %rsi                                    #1490.405
        movslq    %edi, %rdi                                    #1490.541
        vpextrd   $1, %xmm10, %r9d                              #1490.962
        movslq    %r8d, %r8                                     #1490.741
        movslq    %r9d, %r9                                     #1490.874
        vpextrd   $2, %xmm10, %r10d                             #1490.1119
        vpextrd   $3, %xmm10, %r11d                             #1490.1255
        movslq    %r10d, %r10                                   #1490.1031
        movslq    %r11d, %r11                                   #1490.1167
        vpunpcklqdq %xmm12, %xmm14, %xmm13                      #1490.95
        vmovd     (%rdx,%rsi), %xmm11                           #1490.405
        vmovd     (%rdx,%rdi), %xmm14                           #1490.541
        vpunpcklqdq %xmm14, %xmm11, %xmm12                      #1490.385
        vshufps   $136, %xmm12, %xmm13, %xmm11                  #1490.75
        vmovd     (%rdx,%r8), %xmm13                            #1490.741
        vmovd     (%rdx,%r9), %xmm14                            #1490.874
        vpunpcklqdq %xmm14, %xmm13, %xmm12                      #1490.721
        vmovd     (%rdx,%r10), %xmm13                           #1490.1031
        vmovd     (%rdx,%r11), %xmm14                           #1490.1167
        vpunpcklqdq %xmm14, %xmm13, %xmm13                      #1490.1011
        vshufps   $136, %xmm13, %xmm12, %xmm12                  #1490.701
        vpand     %xmm0, %xmm8, %xmm13                          #1492.115
        vpand     %xmm1, %xmm8, %xmm8                           #1505.99
        vinsertf128 $1, %xmm12, %ymm11, %ymm11                  #1490.11
        vpsrld    $7, %xmm7, %xmm12                             #1491.29
        vpand     %xmm0, %xmm12, %xmm7                          #1492.26
        vpand     %xmm3, %xmm15, %xmm0                          #1493.18
        vandps    %xmm3, %xmm6, %xmm3                           #1493.84
        vpsubd    %xmm4, %xmm0, %xmm0                           #1494.18
        vpsubd    %xmm4, %xmm3, %xmm6                           #1494.88
        vpcmpgtd  %xmm5, %xmm0, %xmm0                           #1495.23
        vpcmpgtd  %xmm5, %xmm6, %xmm6                           #1495.100
        vpand     %xmm1, %xmm12, %xmm12                         #1505.18
        vpackssdw %xmm6, %xmm0, %xmm4                           #1496.75
        vpxor     %xmm5, %xmm5, %xmm5                           #1496.58
        vpacksswb %xmm5, %xmm4, %xmm3                           #1496.58
        vandps    1152+__svml_scbrt_ha_data_internal(%rip), %ymm15, %ymm4 #1497.9
        vandps    1216+__svml_scbrt_ha_data_internal(%rip), %ymm15, %ymm5 #1498.10
        vorps     1280+__svml_scbrt_ha_data_internal(%rip), %ymm4, %ymm14 #1499.9
        vpmovmskb %xmm3, %eax                                   #1496.39
        vorps     1344+__svml_scbrt_ha_data_internal(%rip), %ymm5, %ymm3 #1500.10
        vsubps    %ymm3, %ymm14, %ymm4                          #1501.9
        vmulps    %ymm4, %ymm11, %ymm3                          #1502.9
        vmovups   1728+__svml_scbrt_ha_data_internal(%rip), %xmm4 #1503.17
        vpmulld   %xmm4, %xmm13, %xmm14                         #1504.97
        vpmulld   %xmm4, %xmm7, %xmm11                          #1504.20
        vpsrld    $12, %xmm14, %xmm4                            #1507.69
        vpsrld    $12, %xmm11, %xmm5                            #1507.15
        vpaddd    %xmm4, %xmm2, %xmm1                           #1513.74
        vpaddd    %xmm2, %xmm5, %xmm11                          #1513.15
        vpor      %xmm8, %xmm1, %xmm2                           #1513.74
        vpslld    $23, %xmm2, %xmm1                             #1514.64
        vpor      %xmm12, %xmm11, %xmm14                        #1513.15
        vmovups   1664+__svml_scbrt_ha_data_internal(%rip), %xmm2 #1506.28
        vpslld    $23, %xmm14, %xmm14                           #1514.15
        vpsubd    %xmm2, %xmm7, %xmm7                           #1506.28
        vpsubd    %xmm5, %xmm7, %xmm7                           #1508.15
        vpsubd    %xmm5, %xmm7, %xmm8                           #1509.15
        vpsubd    %xmm5, %xmm8, %xmm5                           #1510.15
        vpslld    $7, %xmm5, %xmm11                             #1511.15
        vpaddd    %xmm11, %xmm9, %xmm9                          #1517.23
        vpandn    %xmm9, %xmm0, %xmm9                           #1517.23
        vpsubd    %xmm2, %xmm13, %xmm0                          #1506.111
        vpsubd    %xmm4, %xmm0, %xmm2                           #1508.85
        vpslld    $1, %xmm9, %xmm9                              #1518.23
        vpsubd    %xmm4, %xmm2, %xmm12                          #1509.72
        vpsubd    %xmm4, %xmm12, %xmm4                          #1510.72
        vmovd     %xmm9, %ecx                                   #1519.132
        vpslld    $7, %xmm4, %xmm13                             #1511.63
        vpaddd    %xmm13, %xmm10, %xmm10                        #1517.107
        vpandn    %xmm10, %xmm6, %xmm0                          #1517.107
        vpextrd   $1, %xmm9, %esi                               #1519.309
        vpslld    $1, %xmm0, %xmm2                              #1518.87
        movslq    %ecx, %rcx                                    #1519.38
        movslq    %esi, %rsi                                    #1519.215
        vmovd     %xmm2, %r9d                                   #1519.849
        vmovq     128(%rdx,%rcx), %xmm4                         #1519.38
        vpextrd   $2, %xmm9, %edi                               #1519.489
        vpextrd   $3, %xmm9, %r8d                               #1519.669
        vmovq     128(%rdx,%rsi), %xmm6                         #1519.215
        vpextrd   $1, %xmm2, %r10d                              #1519.1026
        vpextrd   $2, %xmm2, %ecx                               #1519.1206
        vpextrd   $3, %xmm2, %esi                               #1519.1386
        movslq    %edi, %rdi                                    #1519.395
        movslq    %r8d, %r8                                     #1519.575
        movslq    %r9d, %r9                                     #1519.755
        movslq    %r10d, %r10                                   #1519.932
        movslq    %ecx, %rcx                                    #1519.1112
        movslq    %esi, %rsi                                    #1519.1292
        vmovq     128(%rdx,%rdi), %xmm5                         #1519.395
        vmovq     128(%rdx,%r8), %xmm7                          #1519.575
        vmovq     128(%rdx,%r9), %xmm8                          #1519.755
        vmovq     128(%rdx,%r10), %xmm10                        #1519.932
        vmovq     128(%rdx,%rcx), %xmm9                         #1519.1112
        vmovq     128(%rdx,%rsi), %xmm11                        #1519.1292
        vunpcklps %xmm5, %xmm4, %xmm12                          #1519.1458
        vunpcklps %xmm9, %xmm8, %xmm13                          #1519.1568
        vunpcklps %xmm11, %xmm10, %xmm0                         #1519.1623
        vinsertf128 $1, %xmm1, %ymm14, %ymm1                    #1515.10
        vunpcklps %xmm7, %xmm6, %xmm14                          #1519.1513
        vinsertf128 $1, %xmm13, %ymm12, %ymm2                   #1519.1686
        vinsertf128 $1, %xmm0, %ymm14, %ymm4                    #1519.1794
        vunpcklps %ymm4, %ymm2, %ymm5                           #1519.1889
        vunpckhps %ymm4, %ymm2, %ymm6                           #1519.1967
        vmulps    %ymm5, %ymm1, %ymm7                           #1520.17
        vmulps    %ymm6, %ymm1, %ymm5                           #1521.17
        vmulps    896+__svml_scbrt_ha_data_internal(%rip), %ymm3, %ymm1 #1524.24
        vaddps    960+__svml_scbrt_ha_data_internal(%rip), %ymm1, %ymm1 #1524.9
        vmulps    %ymm1, %ymm3, %ymm6                           #1526.24
        vaddps    1024+__svml_scbrt_ha_data_internal(%rip), %ymm6, %ymm0 #1526.9
        vmulps    %ymm0, %ymm3, %ymm2                           #1528.24
        vmulps    %ymm7, %ymm3, %ymm3                           #1529.15
        vaddps    1088+__svml_scbrt_ha_data_internal(%rip), %ymm2, %ymm4 #1528.9
        vmulps    %ymm3, %ymm4, %ymm0                           #1530.9
        vaddps    %ymm0, %ymm5, %ymm1                           #1531.9
        vaddps    %ymm1, %ymm7, %ymm0                           #1532.10
        testb     %al, %al                                      #1534.52
        jne       ..B6.3        # Prob 5%                       #1534.52
                                # LOE rbx r12 r13 r14 r15 eax ymm0 ymm15
..B6.2:                         # Preds ..B6.3 ..B6.9 ..B6.1
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #1537.12
        popq      %rbp                                          #1537.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #1537.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.3:                         # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm15, 64(%rsp)                              #1534.196
        vmovups   %ymm0, 128(%rsp)                              #1534.270
        testl     %eax, %eax                                    #1534.374
        je        ..B6.2        # Prob 95%                      #1534.374
                                # LOE rbx r12 r13 r14 r15 eax ymm0
..B6.6:                         # Preds ..B6.3
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #1534.454
                                # LOE rbx r12 r13 r14 r15 eax edx
..B6.13:                        # Preds ..B6.6
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B6.7:                         # Preds ..B6.8 ..B6.13
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #1534.517
        jc        ..B6.10       # Prob 5%                       #1534.517
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.10 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #1534.470
        cmpl      $8, %r12d                                     #1534.465
        jl        ..B6.7        # Prob 82%                      #1534.465
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.8
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm0                              #1534.675
        jmp       ..B6.2        # Prob 100%                     #1534.675
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B6.10:                        # Preds ..B6.7
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #1534.546
        lea       128(%rsp,%r12,4), %rsi                        #1534.546
..___tag_value___svml_cbrtf8_ha_e9.106:
#       __svml_scbrt_ha_cout_rare_internal(const float *, float *)
        call      __svml_scbrt_ha_cout_rare_internal            #1534.546
..___tag_value___svml_cbrtf8_ha_e9.107:
        jmp       ..B6.8        # Prob 100%                     #1534.546
        .align    16,0x90
                                # LOE rbx r14 r15 r12d r13d
	.cfi_endproc
# mark_end;
	.type	__svml_cbrtf8_ha_e9,@function
	.size	__svml_cbrtf8_ha_e9,.-__svml_cbrtf8_ha_e9
..LN__svml_cbrtf8_ha_e9.5:
	.data
# -- End  __svml_cbrtf8_ha_e9
	.text
.L_2__routine_start___svml_scbrt_ha_cout_rare_internal_6:
# -- Begin  __svml_scbrt_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_scbrt_ha_cout_rare_internal
	.globl __svml_scbrt_ha_cout_rare_internal
# --- __svml_scbrt_ha_cout_rare_internal(const float *, float *)
__svml_scbrt_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #740.1
        .byte     15                                            #847.15
        .byte     30                                            #847.15
        .byte     250                                           #847.15
	.cfi_startproc
..___tag_value___svml_scbrt_ha_cout_rare_internal.109:
..L110:
                                                        #740.1
        movq      %rsi, %r9                                     #740.1
        movss     (%rdi), %xmm0                                 #743.21
        movss     ione(%rip), %xmm1                             #743.24
        mulss     %xmm0, %xmm1                                  #743.24
        movss     %xmm1, -4(%rsp)                               #743.5
        movzwl    -2(%rsp), %eax                                #744.36
        andl      $32640, %eax                                  #744.36
        shrl      $7, %eax                                      #744.36
        cmpl      $255, %eax                                    #744.61
        je        ..B7.9        # Prob 16%                      #744.61
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm0 xmm1
..B7.2:                         # Preds ..B7.1
                                # Execution count [8.40e-01]
        pxor      %xmm0, %xmm0                                  #746.13
        ucomiss   %xmm0, %xmm1                                  #746.25
        jp        ..B7.3        # Prob 0%                       #746.25
        je        ..B7.8        # Prob 16%                      #746.25
                                # LOE rbx rbp r9 r12 r13 r14 r15 eax xmm1
..B7.3:                         # Preds ..B7.2
                                # Execution count [7.06e-01]
        testl     %eax, %eax                                    #757.72
        jne       ..B7.5        # Prob 50%                      #757.72
                                # LOE rbx rbp r9 r12 r13 r14 r15 xmm1
..B7.4:                         # Preds ..B7.3
                                # Execution count [3.53e-01]
        movl      $2122317824, -24(%rsp)                        #759.37
        movl      $713031680, -20(%rsp)                         #760.37
        jmp       ..B7.6        # Prob 100%                     #760.37
                                # LOE rbx rbp r9 r12 r13 r14 r15 xmm1
..B7.5:                         # Preds ..B7.3
                                # Execution count [3.53e-01]
        movl      $1065353216, %eax                             #764.37
        movl      %eax, -24(%rsp)                               #764.37
        movl      %eax, -20(%rsp)                               #765.37
                                # LOE rbx rbp r9 r12 r13 r14 r15 xmm1
..B7.6:                         # Preds ..B7.4 ..B7.5
                                # Execution count [7.06e-01]
        movss     -24(%rsp), %xmm0                              #767.29
        lea       vscbrt_ha_cout_data(%rip), %rsi               #783.49
        mulss     %xmm0, %xmm1                                  #767.29
        movd      %xmm1, %ecx                                   #781.46
        movss     %xmm1, -4(%rsp)                               #767.21
        movl      %ecx, %r10d                                   #789.81
        movl      %ecx, %edi                                    #784.66
        andl      $8388607, %r10d                               #789.81
        movl      %ecx, %r11d                                   #790.82
        shrl      $23, %edi                                     #784.66
        andl      $8257536, %r11d                               #790.82
        orl       $-1082130432, %r10d                           #791.83
        orl       $-1081999360, %r11d                           #792.85
        movl      %r10d, -16(%rsp)                              #791.35
        movl      %ecx, %edx                                    #781.52
        movzbl    %dil, %r8d                                    #785.65
        andl      $2147483647, %ecx                             #786.43
        movl      %r11d, -12(%rsp)                              #792.35
        andl      $-256, %edi                                   #808.57
        movss     -16(%rsp), %xmm1                              #793.35
        addl      $2139095040, %ecx                             #786.43
        shrl      $16, %edx                                     #781.52
        subss     -12(%rsp), %xmm1                              #793.40
        andl      $124, %edx                                    #782.49
        lea       (%r8,%r8,4), %r10d                            #797.46
        mulss     (%rsi,%rdx), %xmm1                            #794.40
        lea       (%r10,%r10), %r11d                            #798.47
        movss     .L_2il0floatpacket.54(%rip), %xmm4            #826.45
        lea       (%r11,%r11), %eax                             #799.48
        addl      %eax, %eax                                    #800.48
        lea       (%r10,%r11,8), %r10d                          #802.48
        addl      %eax, %eax                                    #801.48
        decl      %r8d                                          #809.48
        mulss     %xmm1, %xmm4                                  #828.50
        shll      $7, %r8d                                      #814.50
        lea       (%r10,%rax,8), %r11d                          #807.50
        lea       (%r11,%rax,8), %r10d                          #806.50
        shrl      $12, %r10d                                    #810.55
        addss     .L_2il0floatpacket.53(%rip), %xmm4            #828.57
        mulss     %xmm1, %xmm4                                  #830.49
        lea       85(%r10), %eax                                #815.40
        orl       %edi, %eax                                    #816.40
        addss     .L_2il0floatpacket.52(%rip), %xmm4            #830.56
        xorl      %edi, %edi                                    #786.43
        cmpl      $-16777217, %ecx                              #821.21
        setg      %dil                                          #821.21
        shll      $7, %r10d                                     #814.50
        negl      %edi                                          #788.91
        subl      %r10d, %r8d                                   #814.50
        addl      %r10d, %r10d                                  #814.50
        subl      %r10d, %r8d                                   #814.50
        notl      %edi                                          #820.44
        addl      %r8d, %edx                                    #819.55
        andl      %edx, %edi                                    #820.59
        shll      $23, %eax                                     #817.50
        addl      %edi, %edi                                    #821.66
        movl      %eax, -8(%rsp)                                #818.35
        movss     128(%rdi,%rsi), %xmm5                         #822.55
        movss     -8(%rsp), %xmm2                               #824.56
        mulss     %xmm1, %xmm4                                  #832.49
        mulss     %xmm2, %xmm5                                  #824.56
        addss     .L_2il0floatpacket.51(%rip), %xmm4            #832.56
        mulss     %xmm5, %xmm1                                  #833.54
        movss     132(%rsi,%rdi), %xmm3                         #823.55
        mulss     %xmm1, %xmm4                                  #834.40
        mulss     %xmm2, %xmm3                                  #825.56
        addss     %xmm3, %xmm4                                  #835.40
        addss     %xmm4, %xmm5                                  #836.40
        mulss     -20(%rsp), %xmm5                              #837.29
        movss     %xmm5, (%r9)                                  #838.23
                                # LOE rbx rbp r12 r13 r14 r15
..B7.7:                         # Preds ..B7.6 ..B7.8 ..B7.9
                                # Execution count [1.00e+00]
        xorl      %eax, %eax                                    #849.12
        ret                                                     #849.12
                                # LOE
..B7.8:                         # Preds ..B7.2
                                # Execution count [1.34e-01]: Infreq
        movss     %xmm1, (%r9)                                  #842.19
        jmp       ..B7.7        # Prob 100%                     #842.19
                                # LOE rbx rbp r12 r13 r14 r15
..B7.9:                         # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        addss     %xmm0, %xmm0                                  #847.29
        movss     %xmm0, (%r9)                                  #847.15
        jmp       ..B7.7        # Prob 100%                     #847.15
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15
	.cfi_endproc
# mark_end;
	.type	__svml_scbrt_ha_cout_rare_internal,@function
	.size	__svml_scbrt_ha_cout_rare_internal,.-__svml_scbrt_ha_cout_rare_internal
..LN__svml_scbrt_ha_cout_rare_internal.6:
	.data
# -- End  __svml_scbrt_ha_cout_rare_internal
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
vscbrt_ha_cout_data:
	.long	3212578753
	.long	3212085645
	.long	3211621124
	.long	3211182772
	.long	3210768440
	.long	3210376206
	.long	3210004347
	.long	3209651317
	.long	3209315720
	.long	3208996296
	.long	3208691905
	.long	3208401508
	.long	3208124163
	.long	3207859009
	.long	3207605259
	.long	3207362194
	.long	3207129151
	.long	3206905525
	.long	3206690755
	.long	3206484326
	.long	3206285761
	.long	3206094618
	.long	3205910490
	.long	3205732998
	.long	3205561788
	.long	3205396533
	.long	3205236929
	.long	3205082689
	.long	3204933547
	.long	3204789256
	.long	3204649583
	.long	3204514308
	.long	1065396681
	.long	839340838
	.long	1065482291
	.long	867750258
	.long	1065566215
	.long	851786446
	.long	1065648532
	.long	853949398
	.long	1065729317
	.long	864938789
	.long	1065808640
	.long	864102364
	.long	1065886565
	.long	864209792
	.long	1065963152
	.long	865422805
	.long	1066038457
	.long	867593594
	.long	1066112533
	.long	854482593
	.long	1066185428
	.long	848298042
	.long	1066257188
	.long	860064854
	.long	1066327857
	.long	844792593
	.long	1066397474
	.long	870701309
	.long	1066466079
	.long	872023170
	.long	1066533708
	.long	860255342
	.long	1066600394
	.long	849966899
	.long	1066666169
	.long	863561479
	.long	1066731064
	.long	869115319
	.long	1066795108
	.long	871961375
	.long	1066858329
	.long	859537336
	.long	1066920751
	.long	871954398
	.long	1066982401
	.long	863817578
	.long	1067043301
	.long	861687921
	.long	1067103474
	.long	849594757
	.long	1067162941
	.long	816486846
	.long	1067221722
	.long	858183533
	.long	1067279837
	.long	864500406
	.long	1067337305
	.long	850523240
	.long	1067394143
	.long	808125243
	.long	1067450368
	.long	0
	.long	1067505996
	.long	861173761
	.long	1067588354
	.long	859000219
	.long	1067696217
	.long	823158129
	.long	1067801953
	.long	871826232
	.long	1067905666
	.long	871183196
	.long	1068007450
	.long	839030530
	.long	1068107390
	.long	867690638
	.long	1068205570
	.long	840440923
	.long	1068302063
	.long	868033274
	.long	1068396942
	.long	855856030
	.long	1068490271
	.long	865094453
	.long	1068582113
	.long	860418487
	.long	1068672525
	.long	866225006
	.long	1068761562
	.long	866458226
	.long	1068849275
	.long	865124659
	.long	1068935712
	.long	864837702
	.long	1069020919
	.long	811742505
	.long	1069104937
	.long	869432099
	.long	1069187809
	.long	864584201
	.long	1069269572
	.long	864183978
	.long	1069350263
	.long	844810573
	.long	1069429915
	.long	869245699
	.long	1069508563
	.long	859556409
	.long	1069586236
	.long	870675446
	.long	1069662966
	.long	814190139
	.long	1069738778
	.long	870686941
	.long	1069813702
	.long	861800510
	.long	1069887762
	.long	855649163
	.long	1069960982
	.long	869347119
	.long	1070033387
	.long	864252033
	.long	1070104998
	.long	867276215
	.long	1070175837
	.long	868189817
	.long	1070245925
	.long	849541095
	.long	1070349689
	.long	866633177
	.long	1070485588
	.long	843967686
	.long	1070618808
	.long	857522493
	.long	1070749478
	.long	862339487
	.long	1070877717
	.long	850054662
	.long	1071003634
	.long	864048556
	.long	1071127332
	.long	868027089
	.long	1071248907
	.long	848093931
	.long	1071368446
	.long	865355299
	.long	1071486034
	.long	848111485
	.long	1071601747
	.long	865557362
	.long	1071715659
	.long	870297525
	.long	1071827839
	.long	863416216
	.long	1071938350
	.long	869675693
	.long	1072047254
	.long	865888071
	.long	1072154608
	.long	825332584
	.long	1072260465
	.long	843309506
	.long	1072364876
	.long	870885636
	.long	1072467891
	.long	869119784
	.long	1072569555
	.long	865466648
	.long	1072669911
	.long	867459244
	.long	1072769001
	.long	861192764
	.long	1072866863
	.long	871247716
	.long	1072963536
	.long	864927982
	.long	1073059054
	.long	869195129
	.long	1073153452
	.long	864849564
	.long	1073246762
	.long	840005936
	.long	1073339014
	.long	852579258
	.long	1073430238
	.long	860852782
	.long	1073520462
	.long	869711141
	.long	1073609714
	.long	862506141
	.long	1073698019
	.long	837959274
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
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
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
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
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
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
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.type	vscbrt_ha_cout_data,@object
	.size	vscbrt_ha_cout_data,1920
	.align 64
	.hidden __svml_scbrt_ha_data_internal_avx512
	.globl __svml_scbrt_ha_data_internal_avx512
__svml_scbrt_ha_data_internal_avx512:
	.long	1065353216
	.long	1067533592
	.long	1070280693
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
	.long	2999865775
	.long	849849800
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
	.long	1067533592
	.long	1067322155
	.long	1067126683
	.long	1066945178
	.long	1066775983
	.long	1066617708
	.long	1066469175
	.long	1066329382
	.long	1066197466
	.long	1066072682
	.long	1065954382
	.long	1065841998
	.long	1065735031
	.long	1065633040
	.long	1065535634
	.long	1065442463
	.long	1065353216
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
	.long	2999865775
	.long	849353281
	.long	2992093760
	.long	858369405
	.long	861891413
	.long	3001900484
	.long	2988845984
	.long	3009185201
	.long	3001209163
	.long	847824101
	.long	839380496
	.long	845124191
	.long	851391835
	.long	856440803
	.long	2989578734
	.long	852890174
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
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1249902592
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
	.long	1077936128
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
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	1031603580
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	3185812323
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.long	1051372202
	.type	__svml_scbrt_ha_data_internal_avx512,@object
	.size	__svml_scbrt_ha_data_internal_avx512,960
	.align 64
	.hidden __svml_scbrt_ha_data_internal
	.globl __svml_scbrt_ha_data_internal
__svml_scbrt_ha_data_internal:
	.long	3212578753
	.long	3212085645
	.long	3211621124
	.long	3211182772
	.long	3210768440
	.long	3210376206
	.long	3210004347
	.long	3209651317
	.long	3209315720
	.long	3208996296
	.long	3208691905
	.long	3208401508
	.long	3208124163
	.long	3207859009
	.long	3207605259
	.long	3207362194
	.long	3207129151
	.long	3206905525
	.long	3206690755
	.long	3206484326
	.long	3206285761
	.long	3206094618
	.long	3205910490
	.long	3205732998
	.long	3205561788
	.long	3205396533
	.long	3205236929
	.long	3205082689
	.long	3204933547
	.long	3204789256
	.long	3204649583
	.long	3204514308
	.long	1065396681
	.long	839340838
	.long	1065482291
	.long	867750258
	.long	1065566215
	.long	851786446
	.long	1065648532
	.long	853949398
	.long	1065729317
	.long	864938789
	.long	1065808640
	.long	864102364
	.long	1065886565
	.long	864209792
	.long	1065963152
	.long	865422805
	.long	1066038457
	.long	867593594
	.long	1066112533
	.long	854482593
	.long	1066185428
	.long	848298042
	.long	1066257188
	.long	860064854
	.long	1066327857
	.long	844792593
	.long	1066397474
	.long	870701309
	.long	1066466079
	.long	872023170
	.long	1066533708
	.long	860255342
	.long	1066600394
	.long	849966899
	.long	1066666169
	.long	863561479
	.long	1066731064
	.long	869115319
	.long	1066795108
	.long	871961375
	.long	1066858329
	.long	859537336
	.long	1066920751
	.long	871954398
	.long	1066982401
	.long	863817578
	.long	1067043301
	.long	861687921
	.long	1067103474
	.long	849594757
	.long	1067162941
	.long	816486846
	.long	1067221722
	.long	858183533
	.long	1067279837
	.long	864500406
	.long	1067337305
	.long	850523240
	.long	1067394143
	.long	808125243
	.long	1067450368
	.long	0
	.long	1067505996
	.long	861173761
	.long	1067588354
	.long	859000219
	.long	1067696217
	.long	823158129
	.long	1067801953
	.long	871826232
	.long	1067905666
	.long	871183196
	.long	1068007450
	.long	839030530
	.long	1068107390
	.long	867690638
	.long	1068205570
	.long	840440923
	.long	1068302063
	.long	868033274
	.long	1068396942
	.long	855856030
	.long	1068490271
	.long	865094453
	.long	1068582113
	.long	860418487
	.long	1068672525
	.long	866225006
	.long	1068761562
	.long	866458226
	.long	1068849275
	.long	865124659
	.long	1068935712
	.long	864837702
	.long	1069020919
	.long	811742505
	.long	1069104937
	.long	869432099
	.long	1069187809
	.long	864584201
	.long	1069269572
	.long	864183978
	.long	1069350263
	.long	844810573
	.long	1069429915
	.long	869245699
	.long	1069508563
	.long	859556409
	.long	1069586236
	.long	870675446
	.long	1069662966
	.long	814190139
	.long	1069738778
	.long	870686941
	.long	1069813702
	.long	861800510
	.long	1069887762
	.long	855649163
	.long	1069960982
	.long	869347119
	.long	1070033387
	.long	864252033
	.long	1070104998
	.long	867276215
	.long	1070175837
	.long	868189817
	.long	1070245925
	.long	849541095
	.long	1070349689
	.long	866633177
	.long	1070485588
	.long	843967686
	.long	1070618808
	.long	857522493
	.long	1070749478
	.long	862339487
	.long	1070877717
	.long	850054662
	.long	1071003634
	.long	864048556
	.long	1071127332
	.long	868027089
	.long	1071248907
	.long	848093931
	.long	1071368446
	.long	865355299
	.long	1071486034
	.long	848111485
	.long	1071601747
	.long	865557362
	.long	1071715659
	.long	870297525
	.long	1071827839
	.long	863416216
	.long	1071938350
	.long	869675693
	.long	1072047254
	.long	865888071
	.long	1072154608
	.long	825332584
	.long	1072260465
	.long	843309506
	.long	1072364876
	.long	870885636
	.long	1072467891
	.long	869119784
	.long	1072569555
	.long	865466648
	.long	1072669911
	.long	867459244
	.long	1072769001
	.long	861192764
	.long	1072866863
	.long	871247716
	.long	1072963536
	.long	864927982
	.long	1073059054
	.long	869195129
	.long	1073153452
	.long	864849564
	.long	1073246762
	.long	840005936
	.long	1073339014
	.long	852579258
	.long	1073430238
	.long	860852782
	.long	1073520462
	.long	869711141
	.long	1073609714
	.long	862506141
	.long	1073698019
	.long	837959274
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	3173551943
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	1031591658
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	3185806905
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	1051372203
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8388607
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
	.long	8257536
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
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
	.long	3212967936
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
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	256
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	85
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
	.long	1365
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
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	2155872256
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.long	4278190079
	.type	__svml_scbrt_ha_data_internal,@object
	.size	__svml_scbrt_ha_data_internal,1984
	.align 16
.L_2il0floatpacket.46:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,16
	.align 4
.L_2il0floatpacket.47:
	.long	0x007fffff
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,4
	.align 4
.L_2il0floatpacket.48:
	.long	0x007e0000
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,4
	.align 4
.L_2il0floatpacket.49:
	.long	0xbf800000
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,4
	.align 4
.L_2il0floatpacket.50:
	.long	0xbf820000
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,4
	.align 4
.L_2il0floatpacket.51:
	.long	0x3eaaaaab
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,4
	.align 4
.L_2il0floatpacket.52:
	.long	0xbde38e39
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,4
	.align 4
.L_2il0floatpacket.53:
	.long	0x3d7cd6ea
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,4
	.align 4
.L_2il0floatpacket.54:
	.long	0xbd288f47
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,4
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
