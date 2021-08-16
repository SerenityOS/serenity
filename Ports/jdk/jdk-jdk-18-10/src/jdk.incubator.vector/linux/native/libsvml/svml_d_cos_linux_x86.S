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
	.file "svml_d_cos.c"
	.text
..TXTST0:
.L_2__routine_start___svml_cos2_ha_ex_0:
# -- Begin  __svml_cos2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos2_ha_ex
# --- __svml_cos2_ha_ex(__m128d)
__svml_cos2_ha_ex:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2777.1
        .byte     15                                            #3139.18
        .byte     30                                            #3139.18
        .byte     250                                           #3139.18
	.cfi_startproc
..___tag_value___svml_cos2_ha_ex.1:
..L2:
                                                          #2777.1
        pushq     %rbp                                          #2777.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2777.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2777.1
        subq      $192, %rsp                                    #2777.1
        movaps    %xmm0, %xmm2                                  #2777.1
        movups    16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #2837.25
        movaps    %xmm2, %xmm6                                  #2840.14
        mulpd     %xmm2, %xmm13                                 #2837.25
        movups    16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #2836.51
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rax   #2851.370
        addpd     %xmm8, %xmm13                                 #2837.13
        movaps    %xmm13, %xmm11                                #2838.13
        xorl      %edx, %edx                                    #2829.1
        subpd     %xmm8, %xmm11                                 #2838.13
        movups    16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #2839.48
        mulpd     %xmm11, %xmm9                                 #2840.34
        movups    16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #2842.19
        mulpd     %xmm11, %xmm10                                #2842.19
        subpd     %xmm9, %xmm6                                  #2840.14
        movaps    %xmm6, %xmm5                                  #2843.17
        subpd     %xmm10, %xmm5                                 #2843.17
        andps     16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #2850.17
        movaps    %xmm5, %xmm3                                  #2855.22
        movd      %xmm13, %ecx                                  #2851.128
        movaps    %xmm5, %xmm8                                  #2854.19
        movups    16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #2846.50
        movups    16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #2832.13
        subpd     %xmm5, %xmm6                                  #2844.17
        mulpd     %xmm11, %xmm12                                #2847.32
        pextrw    $4, %xmm13, %esi                              #2851.184
        subpd     %xmm10, %xmm6                                 #2845.17
        shll      $5, %ecx                                      #2851.285
        movaps    %xmm5, %xmm11                                 #2863.15
        shll      $5, %esi                                      #2851.336
        andps     %xmm2, %xmm7                                  #2832.13
        movq      8(%rcx,%rax), %xmm9                           #2853.601
        movaps    %xmm7, %xmm1                                  #2833.26
        mulpd     %xmm5, %xmm11                                 #2863.15
        subpd     %xmm12, %xmm6                                 #2847.13
        cmpnlepd  16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm1 #2833.26
        movhpd    8(%rsi,%rax), %xmm9                           #2853.569
        mulpd     %xmm9, %xmm3                                  #2855.22
        movmskpd  %xmm1, %edi                                   #2883.78
        movq      16(%rcx,%rax), %xmm12                         #2852.599
        movups    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #2873.26
        movhpd    16(%rsi,%rax), %xmm12                         #2852.567
        mulpd     %xmm11, %xmm10                                #2873.26
        movaps    %xmm12, %xmm15                                #2856.22
        movaps    %xmm12, %xmm14                                #2858.18
        addpd     %xmm3, %xmm15                                 #2856.22
        addpd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #2873.14
        subpd     %xmm15, %xmm14                                #2858.18
        mulpd     %xmm11, %xmm10                                #2875.26
        addpd     %xmm14, %xmm3                                 #2859.18
        addpd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #2875.14
        movups    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #2866.26
        movaps    %xmm15, %xmm0                                 #2857.21
        mulpd     %xmm11, %xmm14                                #2866.26
        movq      (%rcx,%rax), %xmm4                            #2851.599
        movhpd    (%rsi,%rax), %xmm4                            #2851.567
        mulpd     %xmm4, %xmm8                                  #2854.19
        addpd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #2866.14
        addpd     %xmm8, %xmm0                                  #2857.21
        addpd     %xmm9, %xmm4                                  #2869.13
        mulpd     %xmm11, %xmm14                                #2867.14
        mulpd     %xmm10, %xmm11                                #2876.14
        subpd     %xmm0, %xmm15                                 #2860.18
        mulpd     %xmm5, %xmm14                                 #2868.14
        mulpd     %xmm12, %xmm11                                #2877.29
        mulpd     %xmm5, %xmm12                                 #2878.32
        addpd     %xmm15, %xmm8                                 #2861.18
        mulpd     %xmm4, %xmm14                                 #2870.29
        subpd     %xmm12, %xmm4                                 #2878.13
        addpd     %xmm8, %xmm3                                  #2862.21
        mulpd     %xmm4, %xmm6                                  #2880.28
        addpd     %xmm14, %xmm3                                 #2870.17
        movq      24(%rcx,%rax), %xmm5                          #2879.599
        movhpd    24(%rsi,%rax), %xmm5                          #2879.567
        addpd     %xmm11, %xmm3                                 #2877.17
        addpd     %xmm6, %xmm5                                  #2880.16
        addpd     %xmm5, %xmm3                                  #2881.17
        addpd     %xmm3, %xmm0                                  #2882.14
        testl     %edi, %edi                                    #2884.66
        jne       ..B1.12       # Prob 5%                       #2884.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm7
..B1.2:                         # Preds ..B1.12 ..B1.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #3142.52
        jne       ..B1.4        # Prob 5%                       #3142.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #3145.12
        popq      %rbp                                          #3145.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3145.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm2, 64(%rsp)                               #3142.197
        movups    %xmm0, 128(%rsp)                              #3142.270
        je        ..B1.3        # Prob 95%                      #3142.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #3142.454
        movq      %r12, 8(%rsp)                                 #3142.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #3142.454
        movq      %r13, (%rsp)                                  #3142.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #3142.454
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #3142.517
        jc        ..B1.11       # Prob 5%                       #3142.517
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #3142.470
        cmpl      $2, %r12d                                     #3142.465
        jl        ..B1.8        # Prob 82%                      #3142.465
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #3142.673
        jmp       ..B1.3        # Prob 100%                     #3142.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #3142.546
        lea       128(%rsp,%r12,8), %rsi                        #3142.546
..___tag_value___svml_cos2_ha_ex.19:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #3142.546
..___tag_value___svml_cos2_ha_ex.20:
        jmp       ..B1.9        # Prob 100%                     #3142.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, (%rsp)                                 #[spill]
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %r9 #2979.372
        movdqu    .L_2il0floatpacket.34(%rip), %xmm0            #2977.16
        pand      %xmm2, %xmm0                                  #2977.16
        psrlq     $52, %xmm0                                    #2978.16
        movups    16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #2887.55
        movd      %xmm0, %ecx                                   #2979.124
        andps     %xmm9, %xmm7                                  #2888.17
        movdqu    .L_2il0floatpacket.35(%rip), %xmm4            #2987.19
        pand      %xmm2, %xmm4                                  #2987.19
        pextrw    $4, %xmm0, %edi                               #2979.183
        cmpeqpd   %xmm9, %xmm7                                  #2889.25
        paddq     .L_2il0floatpacket.36(%rip), %xmm4            #2988.19
        movmskpd  %xmm7, %edx                                   #2891.48
        movdqa    %xmm4, %xmm7                                  #2996.10
        movdqu    .L_2il0floatpacket.37(%rip), %xmm8            #2989.15
        psrlq     $32, %xmm7                                    #2996.10
        pand      %xmm8, %xmm4                                  #2997.10
        lea       (%rcx,%rcx,2), %esi                           #2979.287
        shll      $3, %esi                                      #2979.287
        lea       (%rdi,%rdi,2), %r8d                           #2979.338
        shll      $3, %r8d                                      #2979.338
        movdqa    %xmm7, %xmm9                                  #3002.11
        movq      16(%rsi,%r9), %xmm6                           #2981.626
        movdqa    %xmm4, %xmm12                                 #3007.11
        movhpd    16(%r8,%r9), %xmm6                            #2981.594
        movdqa    %xmm4, %xmm13                                 #3006.11
        movaps    %xmm6, %xmm11                                 #2994.10
        pand      %xmm8, %xmm6                                  #2995.10
        movq      8(%rsi,%r9), %xmm14                           #2980.627
        psrlq     $32, %xmm11                                   #2994.10
        movhpd    8(%r8,%r9), %xmm14                            #2980.595
        movdqa    %xmm8, %xmm0                                  #3018.11
        pmuludq   %xmm11, %xmm9                                 #3002.11
        movaps    %xmm14, %xmm15                                #2992.10
        pmuludq   %xmm4, %xmm11                                 #3008.11
        pmuludq   %xmm7, %xmm6                                  #3003.11
        pand      %xmm8, %xmm14                                 #2993.10
        psrlq     $32, %xmm11                                   #3010.11
        pmuludq   %xmm14, %xmm12                                #3007.11
        psrlq     $32, %xmm15                                   #2992.10
        pmuludq   %xmm7, %xmm14                                 #3001.11
        pmuludq   %xmm15, %xmm13                                #3006.11
        pmuludq   %xmm7, %xmm15                                 #3000.11
        paddq     %xmm11, %xmm9                                 #3014.11
        pand      %xmm12, %xmm0                                 #3018.11
        movdqa    %xmm8, %xmm3                                  #2991.10
        movq      (%rsi,%r9), %xmm10                            #2979.626
        psrlq     $32, %xmm6                                    #3028.11
        paddq     %xmm9, %xmm0                                  #3023.11
        movhpd    (%r8,%r9), %xmm10                             #2979.594
        psrlq     $32, %xmm12                                   #3011.11
        pand      %xmm10, %xmm3                                 #2991.10
        movdqa    %xmm4, %xmm5                                  #3005.11
        paddq     %xmm0, %xmm6                                  #3029.11
        paddq     %xmm12, %xmm14                                #3015.11
        pmuludq   %xmm3, %xmm5                                  #3005.11
        pmuludq   %xmm3, %xmm7                                  #2999.11
        movdqa    %xmm8, %xmm11                                 #3019.11
        movaps    %xmm6, %xmm0                                  #3030.11
        pand      %xmm13, %xmm11                                #3019.11
        psrlq     $32, %xmm0                                    #3030.11
        paddq     %xmm14, %xmm11                                #3024.11
        psrlq     $32, %xmm13                                   #3012.11
        psrlq     $32, %xmm10                                   #2990.10
        paddq     %xmm11, %xmm0                                 #3031.11
        paddq     %xmm13, %xmm15                                #3016.11
        pmuludq   %xmm10, %xmm4                                 #3004.11
        movdqa    %xmm8, %xmm12                                 #3020.11
        movdqa    %xmm0, %xmm14                                 #3032.11
        pand      %xmm5, %xmm12                                 #3020.11
        psrlq     $32, %xmm14                                   #3032.11
        paddq     %xmm15, %xmm12                                #3025.11
        psrlq     $32, %xmm5                                    #3013.11
        pand      %xmm8, %xmm4                                  #3021.11
        paddq     %xmm12, %xmm14                                #3033.11
        paddq     %xmm5, %xmm7                                  #3017.11
        movdqa    %xmm14, %xmm11                                #3034.11
        pand      %xmm8, %xmm14                                 #3037.11
        paddq     %xmm7, %xmm4                                  #3026.11
        psrlq     $32, %xmm11                                   #3034.11
        psllq     $32, %xmm0                                    #3039.17
        paddq     %xmm4, %xmm11                                 #3035.11
        psllq     $32, %xmm11                                   #3038.17
        pand      %xmm8, %xmm6                                  #3036.11
        paddq     %xmm14, %xmm11                                #3040.17
        paddq     %xmm6, %xmm0                                  #3041.17
        movdqu    .L_2il0floatpacket.38(%rip), %xmm3            #3043.15
        movdqa    %xmm11, %xmm5                                 #3046.18
        movdqu    .L_2il0floatpacket.39(%rip), %xmm13           #3045.20
        pand      %xmm2, %xmm3                                  #3043.15
        movups    .L_2il0floatpacket.40(%rip), %xmm15           #3049.15
        psrlq     $12, %xmm5                                    #3046.18
        pxor      %xmm3, %xmm13                                 #3045.20
        movaps    %xmm15, %xmm10                                #3050.19
        por       %xmm13, %xmm5                                 #3047.18
        addpd     %xmm5, %xmm10                                 #3050.19
        movaps    %xmm10, %xmm6                                 #3051.9
        subpd     %xmm15, %xmm6                                 #3051.9
        movdqu    .L_2il0floatpacket.45(%rip), %xmm7            #3068.18
        movdqu    .L_2il0floatpacket.43(%rip), %xmm9            #3059.18
        pand      %xmm11, %xmm7                                 #3068.18
        movdqu    .L_2il0floatpacket.42(%rip), %xmm8            #3057.20
        pand      %xmm0, %xmm9                                  #3059.18
        psllq     $40, %xmm7                                    #3069.18
        psrlq     $24, %xmm0                                    #3070.17
        pxor      %xmm3, %xmm8                                  #3057.20
        por       %xmm0, %xmm7                                  #3071.18
        pxor      .L_2il0floatpacket.44(%rip), %xmm3            #3066.20
        psllq     $28, %xmm9                                    #3060.18
        subpd     %xmm6, %xmm5                                  #3052.17
        por       %xmm3, %xmm7                                  #3072.18
        movaps    %xmm5, %xmm11                                 #3076.11
        subpd     %xmm3, %xmm7                                  #3075.18
        addpd     %xmm7, %xmm11                                 #3076.11
        subpd     %xmm11, %xmm5                                 #3077.17
        por       %xmm8, %xmm9                                  #3061.18
        subpd     %xmm8, %xmm9                                  #3064.17
        addpd     %xmm5, %xmm7                                  #3078.18
        movups    .L_2il0floatpacket.49(%rip), %xmm12           #3084.16
        andps     %xmm11, %xmm12                                #3084.16
        addpd     %xmm7, %xmm9                                  #3079.11
        subpd     %xmm12, %xmm11                                #3085.17
        mulpd     .L_2il0floatpacket.46(%rip), %xmm9            #3089.12
        movups    .L_2il0floatpacket.47(%rip), %xmm4            #3081.16
        movaps    %xmm12, %xmm14                                #3086.16
        movups    .L_2il0floatpacket.48(%rip), %xmm0            #3082.17
        mulpd     %xmm4, %xmm14                                 #3086.16
        mulpd     %xmm11, %xmm4                                 #3087.12
        mulpd     %xmm0, %xmm12                                 #3088.12
        mulpd     %xmm11, %xmm0                                 #3090.12
        addpd     %xmm12, %xmm4                                 #3091.13
        addpd     %xmm0, %xmm9                                  #3092.13
        addpd     %xmm9, %xmm4                                  #3093.13
        movaps    %xmm4, %xmm6                                  #3094.13
        addpd     %xmm14, %xmm6                                 #3094.13
        movups    .L_2il0floatpacket.51(%rip), %xmm9            #3098.16
        movups    .L_2il0floatpacket.50(%rip), %xmm7            #3099.11
        movaps    %xmm9, %xmm8                                  #3100.17
        andps     %xmm2, %xmm7                                  #3099.11
        cmpltpd   %xmm7, %xmm8                                  #3100.17
        cmplepd   %xmm9, %xmm7                                  #3101.20
        subpd     %xmm6, %xmm14                                 #3095.16
        andps     .L_2il0floatpacket.41(%rip), %xmm10           #3055.13
        andps     %xmm2, %xmm7                                  #3102.24
        movd      %xmm10, %r10d                                 #3107.132
        andps     %xmm8, %xmm6                                  #3103.25
        orps      %xmm6, %xmm7                                  #3104.9
        movaps    %xmm7, %xmm3                                  #3111.26
        movaps    %xmm7, %xmm9                                  #3110.23
        pextrw    $4, %xmm10, %r11d                             #3107.188
        addpd     %xmm14, %xmm4                                 #3096.13
        shll      $5, %r10d                                     #3107.289
        andps     %xmm4, %xmm8                                  #3105.9
        shll      $5, %r11d                                     #3107.340
        movq      8(%r10,%rax), %xmm12                          #3109.605
        movhpd    8(%r11,%rax), %xmm12                          #3109.573
        mulpd     %xmm12, %xmm3                                 #3111.26
        movq      (%r10,%rax), %xmm5                            #3107.603
        movq      16(%r10,%rax), %xmm4                          #3108.603
        movhpd    (%r11,%rax), %xmm5                            #3107.571
        movhpd    16(%r11,%rax), %xmm4                          #3108.571
        mulpd     %xmm5, %xmm9                                  #3110.23
        addpd     %xmm12, %xmm5                                 #3125.17
        movaps    %xmm4, %xmm0                                  #3112.26
        movaps    %xmm4, %xmm10                                 #3114.22
        addpd     %xmm3, %xmm0                                  #3112.26
        movaps    %xmm0, %xmm6                                  #3113.25
        addpd     %xmm9, %xmm6                                  #3113.25
        subpd     %xmm0, %xmm10                                 #3114.22
        subpd     %xmm6, %xmm0                                  #3116.22
        addpd     %xmm10, %xmm3                                 #3115.22
        addpd     %xmm0, %xmm9                                  #3117.22
        movaps    %xmm7, %xmm0                                  #3119.19
        mulpd     %xmm7, %xmm0                                  #3119.19
        addpd     %xmm9, %xmm3                                  #3118.25
        movups    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #3122.30
        movups    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #3129.30
        mulpd     %xmm0, %xmm11                                 #3122.30
        mulpd     %xmm0, %xmm14                                 #3129.30
        movups    17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm15 #3129.18
        movups    16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #3122.18
        addpd     %xmm11, %xmm13                                #3122.18
        addpd     %xmm14, %xmm15                                #3129.18
        mulpd     %xmm0, %xmm13                                 #3123.18
        mulpd     %xmm0, %xmm15                                 #3131.30
        mulpd     %xmm7, %xmm13                                 #3124.18
        mulpd     %xmm4, %xmm7                                  #3134.36
        mulpd     %xmm5, %xmm13                                 #3126.33
        subpd     %xmm7, %xmm5                                  #3134.17
        addpd     %xmm13, %xmm3                                 #3126.21
        mulpd     %xmm5, %xmm8                                  #3136.32
        movups    17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #3131.18
        addpd     %xmm15, %xmm11                                #3131.18
        mulpd     %xmm11, %xmm0                                 #3132.18
        mulpd     %xmm4, %xmm0                                  #3133.33
        movq      24(%r10,%rax), %xmm4                          #3135.603
        movhpd    24(%r11,%rax), %xmm4                          #3135.571
        addpd     %xmm0, %xmm3                                  #3133.21
        addpd     %xmm8, %xmm4                                  #3136.20
        addpd     %xmm4, %xmm3                                  #3137.21
        addpd     %xmm3, %xmm6                                  #3138.24
        movaps    %xmm1, %xmm0                                  #3139.29
        andps     %xmm1, %xmm6                                  #3139.78
        andnps    (%rsp), %xmm0                                 #3139.29[spill]
        orps      %xmm6, %xmm0                                  #3139.18
        jmp       ..B1.2        # Prob 100%                     #3139.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
	.cfi_endproc
# mark_end;
	.type	__svml_cos2_ha_ex,@function
	.size	__svml_cos2_ha_ex,.-__svml_cos2_ha_ex
..LN__svml_cos2_ha_ex.0:
	.data
# -- End  __svml_cos2_ha_ex
	.text
.L_2__routine_start___svml_cos8_ha_z0_1:
# -- Begin  __svml_cos8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos8_ha_z0
# --- __svml_cos8_ha_z0(__m512d)
__svml_cos8_ha_z0:
# parameter 1: %zmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3150.1
        .byte     15                                            #3528.18
        .byte     30                                            #3528.18
        .byte     250                                           #3528.18
	.cfi_startproc
..___tag_value___svml_cos8_ha_z0.26:
..L27:
                                                         #3150.1
        pushq     %rbp                                          #3150.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3150.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3150.1
        subq      $192, %rsp                                    #3150.1
        xorl      %esi, %esi                                    #3229.5
        vmovups   192+__svml_dcos_ha_data_internal(%rip), %zmm13 #3234.52
        vmovups   128+__svml_dcos_ha_data_internal(%rip), %zmm6 #3235.13
        vmovups   256+__svml_dcos_ha_data_internal(%rip), %zmm8 #3239.51
        vmovups   320+__svml_dcos_ha_data_internal(%rip), %zmm10 #3240.51
        vmovups   384+__svml_dcos_ha_data_internal(%rip), %zmm14 #3241.51
        vmovups   64+__svml_dcos_ha_data_internal(%rip), %zmm5  #3231.61
        vmovups   896+__svml_dcos_ha_data_internal(%rip), %zmm12 #3254.50
        vmovups   512+__svml_dcos_ha_data_internal(%rip), %zmm2 #3244.270
        vmovups   640+__svml_dcos_ha_data_internal(%rip), %zmm1 #3245.270
        vmovaps   %zmm0, %zmm11                                 #3150.1
        vfmadd213pd {rn-sae}, %zmm13, %zmm11, %zmm6             #3235.13
        vmovups   768+__svml_dcos_ha_data_internal(%rip), %zmm0 #3246.273
        vandpd    __svml_dcos_ha_data_internal(%rip), %zmm11, %zmm4 #3232.16
        vsubpd    {rn-sae}, %zmm13, %zmm6, %zmm9                #3238.15
        vpermt2pd 576+__svml_dcos_ha_data_internal(%rip), %zmm6, %zmm2 #3244.270
        vpermt2pd 704+__svml_dcos_ha_data_internal(%rip), %zmm6, %zmm1 #3245.270
        vpermt2pd 832+__svml_dcos_ha_data_internal(%rip), %zmm6, %zmm0 #3246.273
        vfnmadd213pd {rn-sae}, %zmm11, %zmm9, %zmm8             #3242.18
        vcmppd    $22, {sae}, %zmm5, %zmm4, %k1                 #3236.30
        vpsrlq    $4, %zmm6, %zmm6                              #3280.44
        kmovw     %k1, %eax                                     #3237.75
        vmovaps   %zmm10, %zmm7                                 #3247.18
        vfnmadd213pd {rn-sae}, %zmm8, %zmm9, %zmm7              #3247.18
        vmovaps   %zmm14, %zmm3                                 #3248.17
        vfnmadd213pd {rn-sae}, %zmm7, %zmm9, %zmm3              #3248.17
        vsubpd    {rn-sae}, %zmm7, %zmm8, %zmm15                #3249.21
        vmovups   1152+__svml_dcos_ha_data_internal(%rip), %zmm8 #3262.50
        vmulpd    {rn-sae}, %zmm3, %zmm3, %zmm4                 #3250.21
        vfnmadd231pd {rn-sae}, %zmm9, %zmm10, %zmm15            #3252.18
        vsubpd    {rn-sae}, %zmm7, %zmm3, %zmm13                #3251.21
        vmovups   960+__svml_dcos_ha_data_internal(%rip), %zmm7 #3256.21
        vfmadd213pd {rn-sae}, %zmm13, %zmm14, %zmm9             #3257.19
        vmovups   1024+__svml_dcos_ha_data_internal(%rip), %zmm14 #3258.50
        vmovups   1088+__svml_dcos_ha_data_internal(%rip), %zmm13 #3260.21
        vfmadd231pd {rn-sae}, %zmm4, %zmm12, %zmm7              #3256.21
        vsubpd    {rn-sae}, %zmm9, %zmm15, %zmm10               #3264.20
        vfmadd231pd {rn-sae}, %zmm4, %zmm14, %zmm13             #3260.21
        vfmadd213pd {rn-sae}, %zmm8, %zmm4, %zmm7               #3263.21
        vmovups   1280+__svml_dcos_ha_data_internal(%rip), %zmm8 #3269.50
        vmovups   1216+__svml_dcos_ha_data_internal(%rip), %zmm15 #3265.50
        vmovups   1344+__svml_dcos_ha_data_internal(%rip), %zmm14 #3272.50
        vpsllq    $32, %zmm0, %zmm9                             #3268.39
        vfmadd213pd {rn-sae}, %zmm8, %zmm4, %zmm7               #3270.21
        vfmadd213pd {rn-sae}, %zmm15, %zmm4, %zmm13             #3266.21
        vmovaps   %zmm2, %zmm5                                  #3253.20
        vfnmadd213pd {rn-sae}, %zmm1, %zmm3, %zmm5              #3253.20
        vfmadd213pd {rn-sae}, %zmm14, %zmm4, %zmm13             #3273.21
        vsubpd    {rn-sae}, %zmm5, %zmm1, %zmm12                #3261.23
        vmovaps   %zmm1, %zmm8                                  #3271.23
        vfmsub231pd {rn-sae}, %zmm2, %zmm3, %zmm12              #3267.21
        vfmadd213pd {rn-sae}, %zmm2, %zmm3, %zmm8               #3271.23
        vmulpd    {rn-sae}, %zmm3, %zmm2, %zmm2                 #3275.21
        vmulpd    {rn-sae}, %zmm7, %zmm1, %zmm1                 #3277.26
        vfmadd213pd {rn-sae}, %zmm12, %zmm3, %zmm9              #3274.19
        vfnmadd213pd {rn-sae}, %zmm0, %zmm10, %zmm8             #3276.18
        vpsllq    $63, %zmm6, %zmm0                             #3281.44
        vfnmadd213pd {rn-sae}, %zmm1, %zmm2, %zmm13             #3279.20
        vsubpd    {rn-sae}, %zmm9, %zmm8, %zmm3                 #3278.21
        vfmadd213pd {rn-sae}, %zmm3, %zmm4, %zmm13              #3282.20
        vaddpd    {rn-sae}, %zmm13, %zmm5, %zmm3                #3283.22
        vxorpd    %zmm0, %zmm3, %zmm0                           #3284.18
        testl     %eax, %eax                                    #3285.70
        jne       ..B2.17       # Prob 5%                       #3285.70
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11 k1
..B2.2:                         # Preds ..B2.17 ..B2.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #3531.52
        jne       ..B2.4        # Prob 5%                       #3531.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B2.3:                         # Preds ..B2.10 ..B2.8 ..B2.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #3534.12
        popq      %rbp                                          #3534.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3534.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3531.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B2.5:                         # Preds ..B2.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #3531.249
        movl      %edx, %eax                                    #3531.303
        orl       $8064, %eax                                   #3531.303
        cmpl      %eax, %edx                                    #3531.332
        je        ..B2.7        # Prob 78%                      #3531.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B2.6:                         # Preds ..B2.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #3531.344
        vldmxcsr  32(%rsp)                                      #3531.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B2.7:                         # Preds ..B2.6 ..B2.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm11, 64(%rsp)                              #3531.406
        vmovups   %zmm0, 128(%rsp)                              #3531.481
        testl     %esi, %esi                                    #3531.587
        jne       ..B2.12       # Prob 5%                       #3531.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B2.8:                         # Preds ..B2.15 ..B2.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #3531.930
        je        ..B2.3        # Prob 78%                      #3531.930
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B2.9:                         # Preds ..B2.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3531.955
        movl      32(%rsp), %eax                                #3531.955
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B2.10:                        # Preds ..B2.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #3531.942
        orl       %edx, %eax                                    #3531.942
        movl      %eax, 32(%rsp)                                #3531.942
        vldmxcsr  32(%rsp)                                      #3531.942
        jmp       ..B2.3        # Prob 100%                     #3531.942
                                # LOE rbx r12 r13 r14 r15 zmm0
..B2.12:                        # Preds ..B2.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #3531.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B2.20:                        # Preds ..B2.12
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
..B2.13:                        # Preds ..B2.14 ..B2.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #3531.730
        jc        ..B2.16       # Prob 5%                       #3531.730
                                # LOE rbx r15 r12d r13d r14d
..B2.14:                        # Preds ..B2.16 ..B2.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #3531.683
        cmpl      $8, %ebx                                      #3531.678
        jl        ..B2.13       # Prob 82%                      #3531.678
                                # LOE rbx r15 r12d r13d r14d
..B2.15:                        # Preds ..B2.14
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
        vmovups   128(%rsp), %zmm0                              #3531.886
        jmp       ..B2.8        # Prob 100%                     #3531.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B2.16:                        # Preds ..B2.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #3531.759
        lea       128(%rsp,%rbx,8), %rsi                        #3531.759
..___tag_value___svml_cos8_ha_z0.54:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #3531.759
..___tag_value___svml_cos8_ha_z0.55:
        jmp       ..B2.14       # Prob 100%                     #3531.759
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B2.17:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   16512+__svml_dcos_ha_data_internal_ha(%rip), %zmm7 #3289.56
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rax #3381.406
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vpbroadcastq .L_2il0floatpacket.52(%rip), %zmm9         #3291.71
        vandpd    %zmm11, %zmm7, %zmm8                          #3290.17
        vcmppd    $4, {sae}, %zmm7, %zmm8, %k2                  #3291.122
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rdx   #3498.210
        vmovaps   %zmm9, %zmm6                                  #3291.46
        vpandq    .L_2il0floatpacket.53(%rip){1to8}, %zmm11, %zmm3 #3379.16
        vpsrlq    $52, %zmm3, %zmm12                            #3380.18
        vpsllq    $1, %zmm12, %zmm1                             #3381.134
        vpaddq    %zmm12, %zmm1, %zmm10                         #3381.194
        vpsllq    $3, %zmm10, %zmm4                             #3381.262
        vpmovqd   %zmm4, %ymm0                                  #3381.355
        vpandnq   %zmm8, %zmm8, %zmm6{%k2}                      #3291.46
        vcmppd    $3, {sae}, %zmm6, %zmm6, %k0                  #3293.37
        vpbroadcastq .L_2il0floatpacket.56(%rip), %zmm6         #3391.15
        kxnorw    %k0, %k0, %k4                                 #3382.407
        kxnorw    %k0, %k0, %k5                                 #3383.406
        kxnorw    %k0, %k0, %k3                                 #3381.406
        kxnorw    %k0, %k0, %k2                                 #3498.210
        kmovw     %k0, %esi                                     #3293.37
        vpxord    %zmm8, %zmm8, %zmm8                           #3382.407
        vpxord    %zmm2, %zmm2, %zmm2                           #3383.406
        vgatherdpd 8(%rax,%ymm0), %zmm8{%k4}                    #3382.407
        vgatherdpd 16(%rax,%ymm0), %zmm2{%k5}                   #3383.406
        kxnorw    %k0, %k0, %k4                                 #3500.212
        kxnorw    %k0, %k0, %k5                                 #3524.210
        vpsrlq    $32, %zmm8, %zmm15                            #3394.12
        vpsrlq    $32, %zmm2, %zmm13                            #3396.12
        vpxord    %zmm7, %zmm7, %zmm7                           #3381.406
        vgatherdpd (%rax,%ymm0), %zmm7{%k3}                     #3381.406
        kxnorw    %k0, %k0, %k3                                 #3499.210
        vpsrlq    $32, %zmm7, %zmm0                             #3392.12
        vpandq    .L_2il0floatpacket.54(%rip){1to8}, %zmm11, %zmm5 #3389.19
        vpaddq    .L_2il0floatpacket.55(%rip){1to8}, %zmm5, %zmm3 #3390.19
        vpsrlq    $32, %zmm3, %zmm10                            #3398.12
        vpmullq   %zmm13, %zmm10, %zmm1                         #3404.13
        vpandq    %zmm6, %zmm7, %zmm5                           #3393.10
        vpandq    %zmm6, %zmm8, %zmm12                          #3395.10
        vpandq    %zmm6, %zmm2, %zmm7                           #3397.10
        vpandq    %zmm6, %zmm3, %zmm14                          #3399.10
        vpmullq   %zmm5, %zmm10, %zmm4                          #3401.13
        vpmullq   %zmm15, %zmm10, %zmm3                         #3402.13
        vpmullq   %zmm12, %zmm10, %zmm2                         #3403.13
        vpmullq   %zmm7, %zmm10, %zmm8                          #3405.13
        vpmullq   %zmm15, %zmm14, %zmm10                        #3408.13
        vpmullq   %zmm13, %zmm14, %zmm15                        #3410.13
        vpmullq   %zmm0, %zmm14, %zmm7                          #3406.13
        vpmullq   %zmm12, %zmm14, %zmm0                         #3409.13
        vpmullq   %zmm5, %zmm14, %zmm5                          #3407.13
        vpsrlq    $32, %zmm15, %zmm13                           #3412.13
        vpsrlq    $32, %zmm0, %zmm14                            #3413.13
        vpsrlq    $32, %zmm5, %zmm12                            #3415.13
        vpsrlq    $32, %zmm10, %zmm15                           #3414.13
        vpaddq    %zmm13, %zmm1, %zmm1                          #3416.11
        vpaddq    %zmm14, %zmm2, %zmm13                         #3417.11
        vpaddq    %zmm12, %zmm4, %zmm4                          #3419.11
        vpaddq    %zmm15, %zmm3, %zmm2                          #3418.11
        vpandq    %zmm6, %zmm0, %zmm14                          #3420.11
        vpandq    %zmm6, %zmm10, %zmm12                         #3421.11
        vpandq    %zmm6, %zmm5, %zmm10                          #3422.11
        vpaddq    %zmm1, %zmm14, %zmm5                          #3425.11
        vpaddq    %zmm2, %zmm10, %zmm2                          #3427.11
        vpsrlq    $32, %zmm8, %zmm1                             #3430.13
        vpaddq    %zmm5, %zmm1, %zmm8                           #3431.11
        vpsrlq    $32, %zmm8, %zmm10                            #3432.13
        vpandq    %zmm6, %zmm7, %zmm0                           #3423.11
        vpaddq    %zmm13, %zmm12, %zmm7                         #3426.11
        vpaddq    %zmm4, %zmm0, %zmm3                           #3428.11
        vpaddq    %zmm7, %zmm10, %zmm14                         #3433.11
        vpbroadcastq .L_2il0floatpacket.59(%rip), %zmm7         #3451.36
        vpsrlq    $32, %zmm14, %zmm4                            #3434.13
        vpaddq    %zmm2, %zmm4, %zmm15                          #3435.11
        vpsllq    $32, %zmm14, %zmm4                            #3441.19
        vpsrlq    $32, %zmm15, %zmm0                            #3436.13
        vpaddq    %zmm3, %zmm0, %zmm13                          #3437.11
        vpandq    %zmm6, %zmm8, %zmm1                           #3438.11
        vpandq    %zmm6, %zmm15, %zmm10                         #3439.11
        vpsllq    $32, %zmm13, %zmm6                            #3440.19
        vpaddq    %zmm1, %zmm4, %zmm13                          #3443.17
        vpaddq    %zmm10, %zmm6, %zmm10                         #3442.17
        vpsrlq    $12, %zmm10, %zmm0                            #3448.20
        vpandq    .L_2il0floatpacket.62(%rip){1to8}, %zmm13, %zmm3 #3461.18
        vpsllq    $28, %zmm3, %zmm12                            #3462.20
        vpsrlq    $24, %zmm13, %zmm13                           #3472.19
        vpandq    .L_2il0floatpacket.57(%rip){1to8}, %zmm11, %zmm14 #3445.15
        vpxorq    .L_2il0floatpacket.58(%rip){1to8}, %zmm14, %zmm5 #3447.20
        vporq     %zmm5, %zmm0, %zmm8                           #3449.18
        vaddpd    {rn-sae}, %zmm8, %zmm7, %zmm2                 #3452.21
        vsubpd    {rn-sae}, %zmm7, %zmm2, %zmm6                 #3453.11
        vsubpd    {rn-sae}, %zmm6, %zmm8, %zmm15                #3454.19
        vpbroadcastq .L_2il0floatpacket.68(%rip), %zmm8         #3489.37
        vandpd    .L_2il0floatpacket.67(%rip){1to8}, %zmm11, %zmm6 #3490.11
        vcmppd    $26, {sae}, %zmm8, %zmm6, %k6                 #3491.114
        vcmppd    $22, {sae}, %zmm8, %zmm6, %k7                 #3492.117
        vpxorq    .L_2il0floatpacket.61(%rip){1to8}, %zmm14, %zmm0 #3459.20
        vporq     %zmm0, %zmm12, %zmm4                          #3463.18
        vsubpd    {rn-sae}, %zmm0, %zmm4, %zmm3                 #3466.19
        vpxorq    .L_2il0floatpacket.63(%rip){1to8}, %zmm14, %zmm0 #3468.20
        vpandq    .L_2il0floatpacket.64(%rip){1to8}, %zmm10, %zmm14 #3470.18
        vpsllq    $40, %zmm14, %zmm10                           #3471.20
        vmovups   17152+__svml_dcos_ha_data_internal_ha(%rip), %zmm14 #3517.50
        vporq     %zmm13, %zmm10, %zmm10                        #3473.18
        vporq     %zmm0, %zmm10, %zmm4                          #3474.18
        vsubpd    {rn-sae}, %zmm0, %zmm4, %zmm5                 #3477.20
        vpbroadcastq .L_2il0floatpacket.66(%rip), %zmm4         #3483.38
        vaddpd    {rn-sae}, %zmm5, %zmm15, %zmm7                #3478.13
        vsubpd    {rn-sae}, %zmm7, %zmm15, %zmm15               #3479.19
        vaddpd    {rn-sae}, %zmm15, %zmm5, %zmm10               #3480.20
        vpbroadcastq .L_2il0floatpacket.65(%rip), %zmm5         #3482.37
        vaddpd    {rn-sae}, %zmm3, %zmm10, %zmm12               #3481.13
        vmulpd    {rn-sae}, %zmm5, %zmm7, %zmm1                 #3484.15
        vmovaps   %zmm5, %zmm0                                  #3485.13
        vfmsub213pd {rn-sae}, %zmm1, %zmm7, %zmm0               #3485.13
        vfmadd213pd {rn-sae}, %zmm0, %zmm4, %zmm7               #3486.13
        vfmadd213pd {rn-sae}, %zmm7, %zmm5, %zmm12              #3487.13
        vmovaps   %zmm9, %zmm3                                  #3491.38
        vpandq    .L_2il0floatpacket.60(%rip){1to8}, %zmm2, %zmm2 #3457.13
        vpsllq    $5, %zmm2, %zmm5                              #3498.78
        vpmovqd   %zmm5, %ymm5                                  #3498.160
        vpandnq   %zmm6, %zmm6, %zmm3{%k6}                      #3491.38
        vpandnq   %zmm6, %zmm6, %zmm9{%k7}                      #3492.41
        vpxord    %zmm7, %zmm7, %zmm7                           #3499.210
        vpxord    %zmm6, %zmm6, %zmm6                           #3500.212
        vgatherdpd 16(%rdx,%ymm5), %zmm7{%k3}                   #3499.210
        vgatherdpd 8(%rdx,%ymm5), %zmm6{%k4}                    #3500.212
        vandpd    %zmm11, %zmm9, %zmm10                         #3493.24
        vandpd    %zmm1, %zmm3, %zmm4                           #3494.25
        vandpd    %zmm12, %zmm3, %zmm0                          #3496.9
        vmovups   16960+__svml_dcos_ha_data_internal_ha(%rip), %zmm3 #3510.50
        vorpd     %zmm4, %zmm10, %zmm4                          #3495.9
        vmovaps   %zmm4, %zmm2                                  #3501.26
        vfmadd213pd {rn-sae}, %zmm7, %zmm6, %zmm2               #3501.26
        vmovaps   %zmm4, %zmm10                                 #3502.25
        vsubpd    {rn-sae}, %zmm2, %zmm7, %zmm1                 #3503.24
        vfmadd231pd {rn-sae}, %zmm4, %zmm6, %zmm1               #3504.22
        vpxord    %zmm9, %zmm9, %zmm9                           #3498.210
        vgatherdpd (%rdx,%ymm5), %zmm9{%k2}                     #3498.210
        vfmadd132pd {rn-sae}, %zmm9, %zmm2, %zmm10              #3502.25
        vsubpd    {rn-sae}, %zmm10, %zmm2, %zmm8                #3505.24
        vmovups   17024+__svml_dcos_ha_data_internal_ha(%rip), %zmm2 #3509.50
        vfmadd231pd {rn-sae}, %zmm9, %zmm4, %zmm8               #3506.22
        vaddpd    {rn-sae}, %zmm1, %zmm8, %zmm13                #3507.27
        vaddpd    {rn-sae}, %zmm6, %zmm9, %zmm1                 #3514.19
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm8                 #3508.21
        vmovups   17216+__svml_dcos_ha_data_internal_ha(%rip), %zmm6 #3516.50
        vmovups   17088+__svml_dcos_ha_data_internal_ha(%rip), %zmm9 #3519.50
        vfmadd231pd {rn-sae}, %zmm8, %zmm2, %zmm3               #3511.18
        vfmadd231pd {rn-sae}, %zmm8, %zmm6, %zmm14              #3518.18
        vmulpd    {rn-sae}, %zmm8, %zmm3, %zmm12                #3512.20
        vfmadd213pd {rn-sae}, %zmm9, %zmm8, %zmm14              #3520.18
        vmulpd    {rn-sae}, %zmm4, %zmm12, %zmm15               #3513.20
        vmulpd    {rn-sae}, %zmm8, %zmm14, %zmm2                #3521.20
        vfnmadd213pd {rn-sae}, %zmm1, %zmm7, %zmm4              #3523.17
        vfmadd213pd {rn-sae}, %zmm13, %zmm1, %zmm15             #3515.21
        vfmadd213pd {rn-sae}, %zmm15, %zmm7, %zmm2              #3522.21
        vpxord    %zmm7, %zmm7, %zmm7                           #3524.210
        vgatherdpd 24(%rdx,%ymm5), %zmm7{%k5}                   #3524.210
        vfmadd213pd {rn-sae}, %zmm7, %zmm4, %zmm0               #3525.20
        vaddpd    {rn-sae}, %zmm0, %zmm2, %zmm1                 #3526.23
        vmovups   (%rsp), %zmm0                                 #3528.18[spill]
        vaddpd    {rn-sae}, %zmm1, %zmm10, %zmm0{%k1}           #3528.18
        jmp       ..B2.2        # Prob 100%                     #3528.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
	.cfi_endproc
# mark_end;
	.type	__svml_cos8_ha_z0,@function
	.size	__svml_cos8_ha_z0,.-__svml_cos8_ha_z0
..LN__svml_cos8_ha_z0.1:
	.data
# -- End  __svml_cos8_ha_z0
	.text
.L_2__routine_start___svml_cos1_ha_l9_2:
# -- Begin  __svml_cos1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos1_ha_l9
# --- __svml_cos1_ha_l9(__m128d)
__svml_cos1_ha_l9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3539.1
        .byte     15                                            #3885.18
        .byte     30                                            #3885.18
        .byte     250                                           #3885.18
	.cfi_startproc
..___tag_value___svml_cos1_ha_l9.63:
..L64:
                                                         #3539.1
        pushq     %rbp                                          #3539.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3539.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3539.1
        subq      $128, %rsp                                    #3539.1
        vmovapd   %xmm0, %xmm14                                 #3539.1
        vmovsd    16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #3598.20
        vmovapd   %xmm14, %xmm13                                #3599.13
        vfmadd132sd 16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm8, %xmm13 #3599.13
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rcx   #3612.371
        vmovq     16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm15 #3610.23
        vmovsd    16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #3603.17
        vmovsd    16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #3592.19
        vandpd    %xmm7, %xmm14, %xmm7                          #3594.13
        vpand     %xmm15, %xmm13, %xmm4                         #3611.19
        xorl      %eax, %eax                                    #3591.1
        vmovd     %xmm4, %edx                                   #3612.128
        vsubsd    %xmm8, %xmm13, %xmm6                          #3600.13
        vcmpnlesd 16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm7, %xmm0 #3595.26
        vmovapd   %xmm6, %xmm9                                  #3602.14
        vmovapd   %xmm6, %xmm5                                  #3604.17
        vfnmadd132sd 16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm14, %xmm9 #3602.14
        vmovapd   %xmm6, %xmm12                                 #3606.17
        vmovsd    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #3625.14
        vmovmskpd %xmm0, %esi                                   #3642.78
        vfnmadd213sd %xmm9, %xmm10, %xmm5                       #3604.17
        shll      $5, %edx                                      #3612.231
        vsubsd    %xmm5, %xmm9, %xmm11                          #3605.17
        vmovq     16(%rcx,%rdx), %xmm2                          #3613.371
        vmovapd   %xmm5, %xmm9                                  #3615.22
        vmovq     8(%rcx,%rdx), %xmm1                           #3614.373
        vfmadd213sd %xmm2, %xmm1, %xmm9                         #3615.22
        vmovq     (%rdx,%rcx), %xmm3                            #3612.371
        vmovdqa   %xmm3, %xmm4                                  #3616.21
        vfnmadd213sd %xmm11, %xmm10, %xmm12                     #3606.17
        vmovdqa   %xmm3, %xmm11                                 #3620.18
        vaddsd    %xmm1, %xmm3, %xmm3                           #3628.13
        vfmadd213sd %xmm9, %xmm5, %xmm4                         #3616.21
        vsubsd    %xmm9, %xmm2, %xmm8                           #3617.18
        vfnmadd132sd 16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm12, %xmm6 #3608.13
        vmovapd   %xmm5, %xmm12                                 #3618.18
        vsubsd    %xmm4, %xmm9, %xmm10                          #3619.18
        vmulsd    %xmm5, %xmm5, %xmm9                           #3622.15
        vfmadd213sd %xmm8, %xmm1, %xmm12                        #3618.18
        vfmadd213sd %xmm10, %xmm5, %xmm11                       #3620.18
        vfmadd213sd 16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm9, %xmm13 #3625.14
        vmovsd    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm1 #3632.14
        vfmadd213sd 17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm9, %xmm1 #3632.14
        vaddsd    %xmm12, %xmm11, %xmm8                         #3621.21
        vmulsd    %xmm9, %xmm13, %xmm15                         #3626.14
        vfmadd213sd 17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm9, %xmm1 #3634.14
        vmulsd    %xmm5, %xmm15, %xmm10                         #3627.14
        vfnmadd213sd %xmm3, %xmm2, %xmm5                        #3637.13
        vmulsd    %xmm9, %xmm1, %xmm1                           #3635.14
        vfmadd213sd %xmm8, %xmm3, %xmm10                        #3629.17
        vfmadd213sd 24(%rcx,%rdx), %xmm5, %xmm6                 #3639.16
        vfmadd213sd %xmm10, %xmm2, %xmm1                        #3636.17
        vaddsd    %xmm6, %xmm1, %xmm2                           #3640.17
        vaddsd    %xmm2, %xmm4, %xmm1                           #3641.14
        testl     $1, %esi                                      #3643.55
        jne       ..B3.8        # Prob 5%                       #3643.66
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm7 xmm14
..B3.2:                         # Preds ..B3.8 ..B3.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #3888.52
        jne       ..B3.4        # Prob 5%                       #3888.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
..B3.3:                         # Preds ..B3.5 ..B3.2
                                # Execution count [1.00e+00]
        vmovapd   %xmm1, %xmm0                                  #3891.12
        movq      %rbp, %rsp                                    #3891.12
        popq      %rbp                                          #3891.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3891.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm14, (%rsp)                                #3888.160
        vmovsd    %xmm1, 64(%rsp)                               #3888.233
        jne       ..B3.6        # Prob 5%                       #3888.374
                                # LOE rbx r12 r13 r14 r15 eax
..B3.5:                         # Preds ..B3.7 ..B3.6 ..B3.4
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm1                               #3888.625
        jmp       ..B3.3        # Prob 100%                     #3888.625
                                # LOE rbx r12 r13 r14 r15 xmm1
..B3.6:                         # Preds ..B3.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B3.5        # Prob 95%                      #3888.517
                                # LOE rbx r12 r13 r14 r15
..B3.7:                         # Preds ..B3.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #3888.546
        lea       64(%rsp), %rsi                                #3888.546
..___tag_value___svml_cos1_ha_l9.72:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #3888.546
..___tag_value___svml_cos1_ha_l9.73:
        jmp       ..B3.5        # Prob 100%                     #3888.546
                                # LOE rbx r12 r13 r14 r15
..B3.8:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.34(%rip), %xmm14, %xmm8    #3736.18
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rsi #3738.383
        vpsrlq    $52, %xmm8, %xmm10                            #3737.18
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r8    #3855.375
        vmovd     %xmm10, %edx                                  #3738.124
        vmovsd    16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #3646.24
        vpand     .L_2il0floatpacket.35(%rip), %xmm14, %xmm4    #3746.21
        vandpd    %xmm7, %xmm13, %xmm9                          #3647.17
        vpaddq    .L_2il0floatpacket.36(%rip), %xmm4, %xmm2     #3747.21
        vcmpeqsd  %xmm13, %xmm9, %xmm11                         #3648.25
        vmovdqu   .L_2il0floatpacket.37(%rip), %xmm13           #3748.17
        lea       (%rdx,%rdx,2), %ecx                           #3738.230
        shll      $3, %ecx                                      #3738.230
        vpsrlq    $32, %xmm2, %xmm10                            #3755.12
        vmovq     8(%rsi,%rcx), %xmm7                           #3739.384
        vmovq     16(%rsi,%rcx), %xmm12                         #3740.383
        vpsrlq    $32, %xmm7, %xmm5                             #3751.12
        vmovupd   %xmm1, (%rsp)                                 #[spill]
        vpand     %xmm13, %xmm7, %xmm3                          #3752.12
        vpsrlq    $32, %xmm12, %xmm1                            #3753.12
        vpand     %xmm13, %xmm2, %xmm7                          #3756.12
        vpand     %xmm13, %xmm12, %xmm12                        #3754.12
        vpmuludq  %xmm12, %xmm10, %xmm6                         #3762.13
        vpmuludq  %xmm1, %xmm10, %xmm12                         #3761.13
        vpmuludq  %xmm1, %xmm7, %xmm1                           #3767.13
        vpmuludq  %xmm3, %xmm7, %xmm2                           #3766.13
        vpmuludq  %xmm3, %xmm10, %xmm3                          #3760.13
        vpmuludq  %xmm5, %xmm7, %xmm4                           #3765.13
        vpmuludq  %xmm5, %xmm10, %xmm5                          #3759.13
        vmovmskpd %xmm11, %eax                                  #3650.48
        vpsrlq    $32, %xmm1, %xmm1                             #3769.13
        vpsrlq    $32, %xmm6, %xmm15                            #3787.13
        vpaddq    %xmm1, %xmm12, %xmm12                         #3773.13
        vpand     %xmm13, %xmm2, %xmm6                          #3777.13
        vpsrlq    $32, %xmm2, %xmm2                             #3770.13
        vpaddq    %xmm12, %xmm6, %xmm6                          #3782.13
        vpaddq    %xmm2, %xmm3, %xmm1                           #3774.13
        vpaddq    %xmm6, %xmm15, %xmm12                         #3788.13
        vmovq     (%rcx,%rsi), %xmm11                           #3738.383
        vpand     %xmm13, %xmm4, %xmm6                          #3778.13
        vpand     %xmm13, %xmm11, %xmm8                         #3750.12
        vpsrlq    $32, %xmm12, %xmm15                           #3789.13
        vpmuludq  %xmm8, %xmm7, %xmm9                           #3764.13
        vpsrlq    $32, %xmm11, %xmm11                           #3749.12
        vpmuludq  %xmm8, %xmm10, %xmm8                          #3758.13
        vpmuludq  %xmm11, %xmm7, %xmm7                          #3763.13
        vpaddq    %xmm1, %xmm6, %xmm6                           #3783.13
        vpaddq    %xmm6, %xmm15, %xmm6                          #3790.13
        vpsrlq    $32, %xmm4, %xmm15                            #3771.13
        vpand     %xmm13, %xmm9, %xmm3                          #3779.13
        vpaddq    %xmm15, %xmm5, %xmm4                          #3775.13
        vpaddq    %xmm4, %xmm3, %xmm2                           #3784.13
        vpsrlq    $32, %xmm9, %xmm9                             #3772.13
        vpsrlq    $32, %xmm6, %xmm1                             #3791.13
        vpaddq    %xmm9, %xmm8, %xmm10                          #3776.13
        vpaddq    %xmm2, %xmm1, %xmm2                           #3792.13
        vpand     %xmm13, %xmm7, %xmm3                          #3780.13
        vpsrlq    $32, %xmm2, %xmm5                             #3793.13
        vpaddq    %xmm10, %xmm3, %xmm9                          #3785.13
        vpaddq    %xmm9, %xmm5, %xmm1                           #3794.13
        vpsllq    $32, %xmm1, %xmm5                             #3797.19
        vpand     %xmm13, %xmm2, %xmm9                          #3796.13
        vpaddq    %xmm9, %xmm5, %xmm4                           #3799.19
        vpand     .L_2il0floatpacket.38(%rip), %xmm14, %xmm7    #3802.17
        vpsllq    $32, %xmm6, %xmm6                             #3798.19
        vpand     %xmm13, %xmm12, %xmm12                        #3795.13
        vpsrlq    $12, %xmm4, %xmm13                            #3805.20
        vpaddq    %xmm12, %xmm6, %xmm10                         #3800.19
        vpxor     .L_2il0floatpacket.39(%rip), %xmm7, %xmm12    #3804.22
        andl      $1, %eax                                      #3650.102
        vmovups   .L_2il0floatpacket.40(%rip), %xmm9            #3808.15
        vpor      %xmm12, %xmm13, %xmm6                         #3806.20
        vpand     .L_2il0floatpacket.45(%rip), %xmm4, %xmm4     #3827.20
        vaddsd    %xmm6, %xmm9, %xmm11                          #3809.19
        vpand     .L_2il0floatpacket.43(%rip), %xmm10, %xmm15   #3818.20
        vpsllq    $40, %xmm4, %xmm5                             #3828.20
        vpsrlq    $24, %xmm10, %xmm10                           #3829.19
        vpsllq    $28, %xmm15, %xmm1                            #3819.20
        vpxor     .L_2il0floatpacket.42(%rip), %xmm7, %xmm3     #3816.22
        vsubsd    %xmm9, %xmm11, %xmm13                         #3810.9
        vpxor     .L_2il0floatpacket.44(%rip), %xmm7, %xmm9     #3825.22
        vpor      %xmm10, %xmm5, %xmm7                          #3830.20
        vpor      %xmm9, %xmm7, %xmm12                          #3831.20
        vpor      %xmm3, %xmm1, %xmm2                           #3820.20
        vandpd    .L_2il0floatpacket.50(%rip), %xmm14, %xmm1    #3847.11
        vsubsd    %xmm13, %xmm6, %xmm8                          #3811.17
        vsubsd    %xmm9, %xmm12, %xmm6                          #3834.18
        vsubsd    %xmm3, %xmm2, %xmm15                          #3823.17
        vaddsd    %xmm6, %xmm8, %xmm13                          #3835.11
        vmovapd   %xmm13, %xmm9                                 #3842.13
        vsubsd    %xmm13, %xmm8, %xmm8                          #3836.17
        vmovups   .L_2il0floatpacket.51(%rip), %xmm2            #3846.16
        vaddsd    %xmm8, %xmm6, %xmm12                          #3837.18
        vcmplesd  %xmm2, %xmm1, %xmm3                           #3849.20
        vaddsd    %xmm15, %xmm12, %xmm10                        #3838.11
        vcmpltsd  %xmm1, %xmm2, %xmm15                          #3848.17
        vmovups   .L_2il0floatpacket.46(%rip), %xmm6            #3839.16
        vmovsd    %xmm15, %xmm1, %xmm8                          #3848.17
        vmulsd    %xmm6, %xmm13, %xmm4                          #3841.13
        vandpd    %xmm14, %xmm3, %xmm5                          #3850.24
        vfmsub213sd %xmm4, %xmm6, %xmm9                         #3842.13
        vandpd    %xmm4, %xmm8, %xmm7                           #3851.25
        vandps    .L_2il0floatpacket.41(%rip), %xmm11, %xmm11   #3814.15
        vmovd     %xmm11, %edi                                  #3855.132
        vfmadd132sd .L_2il0floatpacket.69(%rip), %xmm9, %xmm13  #3843.13
        vorpd     %xmm7, %xmm5, %xmm9                           #3852.9
        vmovapd   %xmm9, %xmm2                                  #3858.26
        vmovapd   %xmm9, %xmm5                                  #3861.22
        vmovsd    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #3868.18
        vmulsd    %xmm9, %xmm9, %xmm11                          #3865.19
        vfmadd213sd %xmm13, %xmm6, %xmm10                       #3844.13
        vfmadd213sd 16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm11, %xmm7 #3868.18
        vandpd    %xmm10, %xmm8, %xmm6                          #3853.9
        shll      $5, %edi                                      #3855.235
        vmovq     16(%r8,%rdi), %xmm13                          #3856.375
        vfmadd132sd 8(%r8,%rdi), %xmm13, %xmm2                  #3858.26
        vmovq     (%rdi,%r8), %xmm15                            #3855.375
        vmovdqa   %xmm15, %xmm12                                #3859.25
        vmovdqa   %xmm15, %xmm4                                 #3863.22
        vmulsd    %xmm11, %xmm7, %xmm8                          #3869.18
        vfmadd213sd %xmm2, %xmm9, %xmm12                        #3859.25
        vsubsd    %xmm2, %xmm13, %xmm1                          #3860.22
        vfmadd132sd 8(%r8,%rdi), %xmm1, %xmm5                   #3861.22
        vsubsd    %xmm12, %xmm2, %xmm3                          #3862.22
        vmulsd    %xmm9, %xmm8, %xmm1                           #3870.18
        vaddsd    8(%r8,%rdi), %xmm15, %xmm2                    #3871.17
        vfmadd213sd %xmm3, %xmm9, %xmm4                         #3863.22
        vmovsd    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm15 #3875.18
        vfmadd213sd 17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm11, %xmm15 #3875.18
        vfnmadd213sd %xmm2, %xmm13, %xmm9                       #3880.17
        vaddsd    %xmm5, %xmm4, %xmm10                          #3864.25
        vfmadd213sd 17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm11, %xmm15 #3877.18
        vfmadd213sd 24(%r8,%rdi), %xmm9, %xmm6                  #3882.20
        vfmadd213sd %xmm10, %xmm2, %xmm1                        #3872.21
        vmulsd    %xmm11, %xmm15, %xmm3                         #3878.18
        vfmadd213sd %xmm1, %xmm13, %xmm3                        #3879.21
        vmovupd   (%rsp), %xmm1                                 #3885.18[spill]
        vaddsd    %xmm6, %xmm3, %xmm6                           #3883.21
        vaddsd    %xmm6, %xmm12, %xmm9                          #3884.24
        blendvpd  %xmm0, %xmm9, %xmm1                           #3885.18
        jmp       ..B3.2        # Prob 100%                     #3885.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_cos1_ha_l9,@function
	.size	__svml_cos1_ha_l9,.-__svml_cos1_ha_l9
..LN__svml_cos1_ha_l9.2:
	.data
# -- End  __svml_cos1_ha_l9
	.text
.L_2__routine_start___svml_cos2_ha_e9_3:
# -- Begin  __svml_cos2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos2_ha_e9
# --- __svml_cos2_ha_e9(__m128d)
__svml_cos2_ha_e9:
# parameter 1: %xmm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3896.1
        .byte     15                                            #4258.18
        .byte     30                                            #4258.18
        .byte     250                                           #4258.18
	.cfi_startproc
..___tag_value___svml_cos2_ha_e9.77:
..L78:
                                                         #3896.1
        pushq     %rbp                                          #3896.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3896.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3896.1
        subq      $192, %rsp                                    #3896.1
        movaps    %xmm0, %xmm2                                  #3896.1
        movups    16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #3956.25
        movaps    %xmm2, %xmm6                                  #3959.14
        mulpd     %xmm2, %xmm12                                 #3956.25
        movups    16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm4 #3955.51
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rax   #3970.383
        addpd     %xmm4, %xmm12                                 #3956.13
        movaps    %xmm12, %xmm10                                #3957.13
        xorl      %edx, %edx                                    #3948.1
        subpd     %xmm4, %xmm10                                 #3957.13
        movups    16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #3958.48
        mulpd     %xmm10, %xmm8                                 #3959.34
        movups    16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #3961.19
        mulpd     %xmm10, %xmm9                                 #3961.19
        subpd     %xmm8, %xmm6                                  #3959.14
        movaps    %xmm6, %xmm5                                  #3962.17
        andps     16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #3969.23
        subpd     %xmm9, %xmm5                                  #3962.17
        movd      %xmm12, %ecx                                  #3970.128
        movaps    %xmm5, %xmm1                                  #3974.22
        movaps    %xmm5, %xmm15                                 #3973.19
        shll      $5, %ecx                                      #3970.298
        pextrd    $2, %xmm12, %esi                              #3970.190
        shll      $5, %esi                                      #3970.349
        movq      8(%rcx,%rax), %xmm8                           #3972.614
        movhpd    8(%rsi,%rax), %xmm8                           #3972.582
        mulpd     %xmm8, %xmm1                                  #3974.22
        subpd     %xmm5, %xmm6                                  #3963.17
        movups    16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #3965.50
        movq      (%rcx,%rax), %xmm3                            #3970.612
        mulpd     %xmm10, %xmm11                                #3966.32
        subpd     %xmm9, %xmm6                                  #3964.17
        movq      16(%rcx,%rax), %xmm10                         #3971.612
        movaps    %xmm5, %xmm9                                  #3982.15
        movhpd    (%rsi,%rax), %xmm3                            #3970.580
        movhpd    16(%rsi,%rax), %xmm10                         #3971.580
        mulpd     %xmm3, %xmm15                                 #3973.19
        mulpd     %xmm5, %xmm9                                  #3982.15
        addpd     %xmm8, %xmm3                                  #3988.13
        subpd     %xmm11, %xmm6                                 #3966.13
        movaps    %xmm10, %xmm14                                #3975.22
        movaps    %xmm10, %xmm13                                #3977.18
        addpd     %xmm1, %xmm14                                 #3975.22
        movaps    %xmm14, %xmm4                                 #3976.21
        addpd     %xmm15, %xmm4                                 #3976.21
        subpd     %xmm14, %xmm13                                #3977.18
        subpd     %xmm4, %xmm14                                 #3979.18
        addpd     %xmm13, %xmm1                                 #3978.18
        addpd     %xmm14, %xmm15                                #3980.18
        movups    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #3992.26
        mulpd     %xmm9, %xmm14                                 #3992.26
        addpd     %xmm15, %xmm1                                 #3981.21
        addpd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #3992.14
        movups    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #3985.26
        mulpd     %xmm9, %xmm13                                 #3985.26
        mulpd     %xmm9, %xmm14                                 #3994.26
        addpd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #3985.14
        addpd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #3994.14
        mulpd     %xmm9, %xmm13                                 #3986.14
        mulpd     %xmm14, %xmm9                                 #3995.14
        mulpd     %xmm5, %xmm13                                 #3987.14
        mulpd     %xmm10, %xmm9                                 #3996.29
        mulpd     %xmm5, %xmm10                                 #3997.32
        mulpd     %xmm3, %xmm13                                 #3989.29
        subpd     %xmm10, %xmm3                                 #3997.13
        addpd     %xmm13, %xmm1                                 #3989.17
        mulpd     %xmm3, %xmm6                                  #3999.28
        addpd     %xmm9, %xmm1                                  #3996.17
        movq      24(%rcx,%rax), %xmm5                          #3998.612
        movhpd    24(%rsi,%rax), %xmm5                          #3998.580
        movups    16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #3951.13
        addpd     %xmm6, %xmm5                                  #3999.16
        andps     %xmm2, %xmm7                                  #3951.13
        movaps    %xmm7, %xmm0                                  #3952.26
        cmpnlepd  16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm0 #3952.26
        addpd     %xmm5, %xmm1                                  #4000.17
        movmskpd  %xmm0, %edi                                   #4002.78
        addpd     %xmm1, %xmm4                                  #4001.14
        testl     %edi, %edi                                    #4003.66
        jne       ..B4.12       # Prob 5%                       #4003.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm2 xmm4 xmm7
..B4.2:                         # Preds ..B4.12 ..B4.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #4261.52
        jne       ..B4.4        # Prob 5%                       #4261.52
                                # LOE rbx r12 r13 r14 r15 edx xmm2 xmm4
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        movaps    %xmm4, %xmm0                                  #4264.12
        movq      %rbp, %rsp                                    #4264.12
        popq      %rbp                                          #4264.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4264.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm2, 64(%rsp)                               #4261.197
        movups    %xmm4, 128(%rsp)                              #4261.270
        je        ..B4.3        # Prob 95%                      #4261.374
                                # LOE rbx r12 r13 r14 r15 edx xmm4
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #4261.454
        movq      %r12, 8(%rsp)                                 #4261.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #4261.454
        movq      %r13, (%rsp)                                  #4261.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #4261.454
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4261.517
        jc        ..B4.11       # Prob 5%                       #4261.517
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4261.470
        cmpl      $2, %r12d                                     #4261.465
        jl        ..B4.8        # Prob 82%                      #4261.465
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm4                              #4261.673
        jmp       ..B4.3        # Prob 100%                     #4261.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm4
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #4261.546
        lea       128(%rsp,%r12,8), %rsi                        #4261.546
..___tag_value___svml_cos2_ha_e9.95:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #4261.546
..___tag_value___svml_cos2_ha_e9.96:
        jmp       ..B4.9        # Prob 100%                     #4261.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.34(%rip), %xmm1            #4096.22
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %r9 #4098.385
        pand      %xmm2, %xmm1                                  #4096.22
        psrlq     $52, %xmm1                                    #4097.22
        movups    16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #4006.55
        movd      %xmm1, %ecx                                   #4098.124
        andps     %xmm9, %xmm7                                  #4007.17
        movups    %xmm4, (%rsp)                                 #[spill]
        movdqu    .L_2il0floatpacket.35(%rip), %xmm4            #4106.25
        pextrd    $2, %xmm1, %edi                               #4098.189
        pand      %xmm2, %xmm4                                  #4106.25
        cmpeqpd   %xmm9, %xmm7                                  #4008.25
        paddq     .L_2il0floatpacket.36(%rip), %xmm4            #4107.25
        movmskpd  %xmm7, %edx                                   #4010.48
        movdqa    %xmm4, %xmm7                                  #4115.16
        movdqu    .L_2il0floatpacket.37(%rip), %xmm3            #4108.21
        psrlq     $32, %xmm7                                    #4115.16
        pand      %xmm3, %xmm4                                  #4116.16
        lea       (%rcx,%rcx,2), %esi                           #4098.300
        shll      $3, %esi                                      #4098.300
        lea       (%rdi,%rdi,2), %r8d                           #4098.351
        shll      $3, %r8d                                      #4098.351
        movdqa    %xmm7, %xmm9                                  #4121.17
        movq      16(%rsi,%r9), %xmm8                           #4100.639
        movdqa    %xmm4, %xmm12                                 #4126.17
        movhpd    16(%r8,%r9), %xmm8                            #4100.607
        movdqa    %xmm4, %xmm13                                 #4125.17
        movaps    %xmm8, %xmm11                                 #4113.16
        pand      %xmm3, %xmm8                                  #4114.16
        movq      8(%rsi,%r9), %xmm14                           #4099.640
        psrlq     $32, %xmm11                                   #4113.16
        movhpd    8(%r8,%r9), %xmm14                            #4099.608
        movdqa    %xmm3, %xmm1                                  #4137.17
        pmuludq   %xmm11, %xmm9                                 #4121.17
        movaps    %xmm14, %xmm15                                #4111.16
        pmuludq   %xmm4, %xmm11                                 #4127.17
        pmuludq   %xmm7, %xmm8                                  #4122.17
        pand      %xmm3, %xmm14                                 #4112.16
        psrlq     $32, %xmm11                                   #4129.17
        pmuludq   %xmm14, %xmm12                                #4126.17
        psrlq     $32, %xmm15                                   #4111.16
        pmuludq   %xmm7, %xmm14                                 #4120.17
        pmuludq   %xmm15, %xmm13                                #4125.17
        pmuludq   %xmm7, %xmm15                                 #4119.17
        paddq     %xmm11, %xmm9                                 #4133.17
        pand      %xmm12, %xmm1                                 #4137.17
        movdqa    %xmm3, %xmm5                                  #4110.16
        movq      (%rsi,%r9), %xmm10                            #4098.639
        psrlq     $32, %xmm8                                    #4147.17
        paddq     %xmm9, %xmm1                                  #4142.17
        movhpd    (%r8,%r9), %xmm10                             #4098.607
        psrlq     $32, %xmm12                                   #4130.17
        pand      %xmm10, %xmm5                                 #4110.16
        movdqa    %xmm4, %xmm6                                  #4124.17
        paddq     %xmm1, %xmm8                                  #4148.17
        paddq     %xmm12, %xmm14                                #4134.17
        pmuludq   %xmm5, %xmm6                                  #4124.17
        pmuludq   %xmm5, %xmm7                                  #4118.17
        movdqa    %xmm3, %xmm11                                 #4138.17
        movaps    %xmm8, %xmm1                                  #4149.17
        pand      %xmm13, %xmm11                                #4138.17
        psrlq     $32, %xmm1                                    #4149.17
        paddq     %xmm14, %xmm11                                #4143.17
        psrlq     $32, %xmm13                                   #4131.17
        psrlq     $32, %xmm10                                   #4109.16
        paddq     %xmm11, %xmm1                                 #4150.17
        paddq     %xmm13, %xmm15                                #4135.17
        pmuludq   %xmm10, %xmm4                                 #4123.17
        movdqa    %xmm3, %xmm12                                 #4139.17
        movdqa    %xmm1, %xmm14                                 #4151.17
        pand      %xmm6, %xmm12                                 #4139.17
        psrlq     $32, %xmm14                                   #4151.17
        paddq     %xmm15, %xmm12                                #4144.17
        psrlq     $32, %xmm6                                    #4132.17
        pand      %xmm3, %xmm4                                  #4140.17
        paddq     %xmm12, %xmm14                                #4152.17
        paddq     %xmm6, %xmm7                                  #4136.17
        movdqa    %xmm14, %xmm11                                #4153.17
        pand      %xmm3, %xmm14                                 #4156.17
        paddq     %xmm7, %xmm4                                  #4145.17
        psrlq     $32, %xmm11                                   #4153.17
        psllq     $32, %xmm1                                    #4158.23
        paddq     %xmm4, %xmm11                                 #4154.17
        psllq     $32, %xmm11                                   #4157.23
        pand      %xmm3, %xmm8                                  #4155.17
        paddq     %xmm14, %xmm11                                #4159.23
        paddq     %xmm8, %xmm1                                  #4160.23
        movdqu    .L_2il0floatpacket.38(%rip), %xmm5            #4162.21
        movdqa    %xmm11, %xmm6                                 #4165.24
        movdqu    .L_2il0floatpacket.39(%rip), %xmm13           #4164.26
        pand      %xmm2, %xmm5                                  #4162.21
        movups    .L_2il0floatpacket.40(%rip), %xmm15           #4168.15
        psrlq     $12, %xmm6                                    #4165.24
        pxor      %xmm5, %xmm13                                 #4164.26
        movaps    %xmm15, %xmm10                                #4169.19
        por       %xmm13, %xmm6                                 #4166.24
        addpd     %xmm6, %xmm10                                 #4169.19
        movaps    %xmm10, %xmm8                                 #4170.9
        subpd     %xmm15, %xmm8                                 #4170.9
        movdqu    .L_2il0floatpacket.45(%rip), %xmm7            #4187.24
        movdqu    .L_2il0floatpacket.43(%rip), %xmm9            #4178.24
        pand      %xmm11, %xmm7                                 #4187.24
        movdqu    .L_2il0floatpacket.42(%rip), %xmm3            #4176.26
        pand      %xmm1, %xmm9                                  #4178.24
        psllq     $40, %xmm7                                    #4188.24
        psrlq     $24, %xmm1                                    #4189.23
        pxor      %xmm5, %xmm3                                  #4176.26
        por       %xmm1, %xmm7                                  #4190.24
        pxor      .L_2il0floatpacket.44(%rip), %xmm5            #4185.26
        psllq     $28, %xmm9                                    #4179.24
        subpd     %xmm8, %xmm6                                  #4171.17
        por       %xmm5, %xmm7                                  #4191.24
        movaps    %xmm6, %xmm11                                 #4195.11
        subpd     %xmm5, %xmm7                                  #4194.18
        addpd     %xmm7, %xmm11                                 #4195.11
        subpd     %xmm11, %xmm6                                 #4196.17
        por       %xmm3, %xmm9                                  #4180.24
        subpd     %xmm3, %xmm9                                  #4183.17
        addpd     %xmm6, %xmm7                                  #4197.18
        movups    .L_2il0floatpacket.49(%rip), %xmm12           #4203.16
        andps     %xmm11, %xmm12                                #4203.16
        addpd     %xmm7, %xmm9                                  #4198.11
        subpd     %xmm12, %xmm11                                #4204.17
        mulpd     .L_2il0floatpacket.46(%rip), %xmm9            #4208.12
        movups    .L_2il0floatpacket.47(%rip), %xmm4            #4200.16
        movaps    %xmm12, %xmm14                                #4205.16
        movups    .L_2il0floatpacket.48(%rip), %xmm1            #4201.17
        mulpd     %xmm4, %xmm14                                 #4205.16
        mulpd     %xmm11, %xmm4                                 #4206.12
        mulpd     %xmm1, %xmm12                                 #4207.12
        mulpd     %xmm11, %xmm1                                 #4209.12
        addpd     %xmm12, %xmm4                                 #4210.13
        addpd     %xmm1, %xmm9                                  #4211.13
        addpd     %xmm9, %xmm4                                  #4212.13
        movaps    %xmm4, %xmm3                                  #4213.13
        addpd     %xmm14, %xmm3                                 #4213.13
        movups    .L_2il0floatpacket.51(%rip), %xmm9            #4217.16
        movups    .L_2il0floatpacket.50(%rip), %xmm7            #4218.11
        movaps    %xmm9, %xmm8                                  #4219.17
        andps     %xmm2, %xmm7                                  #4218.11
        cmpltpd   %xmm7, %xmm8                                  #4219.17
        cmplepd   %xmm9, %xmm7                                  #4220.20
        subpd     %xmm3, %xmm14                                 #4214.16
        andps     .L_2il0floatpacket.41(%rip), %xmm10           #4174.19
        andps     %xmm2, %xmm7                                  #4221.24
        movd      %xmm10, %r10d                                 #4226.132
        andps     %xmm8, %xmm3                                  #4222.25
        orps      %xmm3, %xmm7                                  #4223.9
        movaps    %xmm7, %xmm3                                  #4230.26
        movaps    %xmm7, %xmm9                                  #4229.23
        shll      $5, %r10d                                     #4226.302
        pextrd    $2, %xmm10, %r11d                             #4226.194
        shll      $5, %r11d                                     #4226.353
        movq      8(%r10,%rax), %xmm12                          #4228.618
        movhpd    8(%r11,%rax), %xmm12                          #4228.586
        addpd     %xmm14, %xmm4                                 #4215.13
        mulpd     %xmm12, %xmm3                                 #4230.26
        movq      (%r10,%rax), %xmm5                            #4226.616
        andps     %xmm4, %xmm8                                  #4224.9
        movq      16(%r10,%rax), %xmm4                          #4227.616
        movhpd    (%r11,%rax), %xmm5                            #4226.584
        movhpd    16(%r11,%rax), %xmm4                          #4227.584
        mulpd     %xmm5, %xmm9                                  #4229.23
        addpd     %xmm12, %xmm5                                 #4244.17
        movaps    %xmm4, %xmm1                                  #4231.26
        movaps    %xmm4, %xmm10                                 #4233.22
        addpd     %xmm3, %xmm1                                  #4231.26
        movaps    %xmm1, %xmm6                                  #4232.25
        addpd     %xmm9, %xmm6                                  #4232.25
        subpd     %xmm1, %xmm10                                 #4233.22
        subpd     %xmm6, %xmm1                                  #4235.22
        addpd     %xmm10, %xmm3                                 #4234.22
        addpd     %xmm1, %xmm9                                  #4236.22
        movaps    %xmm7, %xmm1                                  #4238.19
        mulpd     %xmm7, %xmm1                                  #4238.19
        addpd     %xmm9, %xmm3                                  #4237.25
        movups    17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #4241.30
        movups    17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm14 #4248.30
        mulpd     %xmm1, %xmm11                                 #4241.30
        mulpd     %xmm1, %xmm14                                 #4248.30
        movups    17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm15 #4248.18
        movups    16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #4241.18
        addpd     %xmm11, %xmm13                                #4241.18
        addpd     %xmm14, %xmm15                                #4248.18
        mulpd     %xmm1, %xmm13                                 #4242.18
        mulpd     %xmm1, %xmm15                                 #4250.30
        mulpd     %xmm7, %xmm13                                 #4243.18
        mulpd     %xmm4, %xmm7                                  #4253.36
        mulpd     %xmm5, %xmm13                                 #4245.33
        subpd     %xmm7, %xmm5                                  #4253.17
        addpd     %xmm13, %xmm3                                 #4245.21
        mulpd     %xmm5, %xmm8                                  #4255.32
        movups    17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #4250.18
        addpd     %xmm15, %xmm11                                #4250.18
        mulpd     %xmm11, %xmm1                                 #4251.18
        mulpd     %xmm4, %xmm1                                  #4252.33
        movq      24(%r10,%rax), %xmm4                          #4254.616
        movhpd    24(%r11,%rax), %xmm4                          #4254.584
        addpd     %xmm1, %xmm3                                  #4252.21
        addpd     %xmm8, %xmm4                                  #4255.20
        addpd     %xmm4, %xmm3                                  #4256.21
        addpd     %xmm3, %xmm6                                  #4257.24
        movups    (%rsp), %xmm4                                 #4258.18[spill]
        blendvpd  %xmm0, %xmm6, %xmm4                           #4258.18
        jmp       ..B4.2        # Prob 100%                     #4258.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm2 xmm4
	.cfi_endproc
# mark_end;
	.type	__svml_cos2_ha_e9,@function
	.size	__svml_cos2_ha_e9,.-__svml_cos2_ha_e9
..LN__svml_cos2_ha_e9.3:
	.data
# -- End  __svml_cos2_ha_e9
	.text
.L_2__routine_start___svml_cos4_ha_l9_4:
# -- Begin  __svml_cos4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos4_ha_l9
# --- __svml_cos4_ha_l9(__m256d)
__svml_cos4_ha_l9:
# parameter 1: %ymm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4269.1
        .byte     15                                            #4615.18
        .byte     30                                            #4615.18
        .byte     250                                           #4615.18
	.cfi_startproc
..___tag_value___svml_cos4_ha_l9.102:
..L103:
                                                        #4269.1
        pushq     %rbp                                          #4269.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4269.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4269.1
        subq      $192, %rsp                                    #4269.1
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r8    #4342.728
        vmovupd   16576+__svml_dcos_ha_data_internal_ha(%rip), %ymm11 #4327.49
        xorl      %esi, %esi                                    #4321.1
        vmovupd   16640+__svml_dcos_ha_data_internal_ha(%rip), %ymm7 #4328.54
        vmovupd   16768+__svml_dcos_ha_data_internal_ha(%rip), %ymm8 #4331.51
        vmovupd   16832+__svml_dcos_ha_data_internal_ha(%rip), %ymm9 #4333.51
        vmovapd   %ymm0, %ymm14                                 #4269.1
        vfmadd213pd %ymm7, %ymm14, %ymm11                       #4329.13
        vsubpd    %ymm7, %ymm11, %ymm5                          #4330.13
        vfnmadd213pd %ymm14, %ymm5, %ymm8                       #4332.14
        vmovapd   %ymm9, %ymm4                                  #4334.17
        vfnmadd213pd %ymm8, %ymm5, %ymm4                        #4334.17
        vandps    16704+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm12 #4341.19
        vsubpd    %ymm4, %ymm8, %ymm10                          #4335.17
        vfnmadd231pd %ymm5, %ymm9, %ymm10                       #4336.17
        vfnmadd132pd 16896+__svml_dcos_ha_data_internal_ha(%rip), %ymm10, %ymm5 #4338.13
        vandpd    16384+__svml_dcos_ha_data_internal_ha(%rip), %ymm14, %ymm6 #4324.13
        vcmpnle_uqpd 16448+__svml_dcos_ha_data_internal_ha(%rip), %ymm6, %ymm13 #4325.26
        vmovmskpd %ymm13, %r9d                                  #4372.78
        vextractf128 $1, %ymm12, %xmm2                          #4342.326
        vmovd     %xmm12, %eax                                  #4342.128
        vmovd     %xmm2, %ecx                                   #4342.307
        shll      $5, %eax                                      #4342.539
        vpextrd   $2, %xmm12, %edx                              #4342.216
        shll      $5, %ecx                                      #4342.641
        vpextrd   $2, %xmm2, %edi                               #4342.400
        shll      $5, %edx                                      #4342.590
        vmovq     (%rax,%r8), %xmm15                            #4342.1227
        vmovq     16(%rax,%r8), %xmm7                           #4343.1227
        shll      $5, %edi                                      #4342.692
        vmovq     (%rcx,%r8), %xmm3                             #4342.1427
        vmovq     16(%rcx,%r8), %xmm9                           #4343.1427
        vmovhpd   (%rdx,%r8), %xmm15, %xmm1                     #4342.1195
        vmovhpd   16(%rdx,%r8), %xmm7, %xmm8                    #4343.1195
        vmovq     8(%rax,%r8), %xmm11                           #4344.1229
        vmovhpd   (%rdi,%r8), %xmm3, %xmm0                      #4342.1395
        vmovhpd   16(%rdi,%r8), %xmm9, %xmm10                   #4343.1395
        vmovq     8(%rcx,%r8), %xmm15                           #4344.1429
        vmovhpd   8(%rdx,%r8), %xmm11, %xmm12                   #4344.1197
        vinsertf128 $1, %xmm0, %ymm1, %ymm3                     #4342.1149
        vinsertf128 $1, %xmm10, %ymm8, %ymm2                    #4343.1149
        vmovhpd   8(%rdi,%r8), %xmm15, %xmm1                    #4344.1397
        vmovaps   %ymm2, %ymm7                                  #4345.22
        vmovupd   16960+__svml_dcos_ha_data_internal_ha(%rip), %ymm10 #4355.14
        vinsertf128 $1, %xmm1, %ymm12, %ymm0                    #4344.1151
        vfmadd231pd %ymm4, %ymm0, %ymm7                         #4345.22
        vmovaps   %ymm3, %ymm1                                  #4346.21
        vfmadd213pd %ymm7, %ymm4, %ymm1                         #4346.21
        vsubpd    %ymm7, %ymm2, %ymm8                           #4347.18
        vsubpd    %ymm1, %ymm7, %ymm9                           #4349.18
        vmulpd    %ymm4, %ymm4, %ymm7                           #4352.15
        vfmadd231pd %ymm4, %ymm0, %ymm8                         #4348.18
        vfmadd231pd %ymm3, %ymm4, %ymm9                         #4350.18
        vaddpd    %ymm0, %ymm3, %ymm3                           #4358.13
        vfmadd231pd 17024+__svml_dcos_ha_data_internal_ha(%rip), %ymm7, %ymm10 #4355.14
        vmovupd   17152+__svml_dcos_ha_data_internal_ha(%rip), %ymm0 #4362.14
        vaddpd    %ymm9, %ymm8, %ymm15                          #4351.21
        vmulpd    %ymm10, %ymm7, %ymm11                         #4356.14
        vfmadd231pd 17216+__svml_dcos_ha_data_internal_ha(%rip), %ymm7, %ymm0 #4362.14
        vmulpd    %ymm11, %ymm4, %ymm8                          #4357.14
        vfmadd213pd 17088+__svml_dcos_ha_data_internal_ha(%rip), %ymm7, %ymm0 #4364.14
        vfnmadd213pd %ymm3, %ymm2, %ymm4                        #4367.13
        vfmadd213pd %ymm15, %ymm3, %ymm8                        #4359.17
        vmulpd    %ymm0, %ymm7, %ymm7                           #4365.14
        vmovq     24(%rcx,%r8), %xmm3                           #4368.1427
        vfmadd213pd %ymm8, %ymm2, %ymm7                         #4366.17
        vmovq     24(%rax,%r8), %xmm2                           #4368.1227
        vmovhpd   24(%rdx,%r8), %xmm2, %xmm2                    #4368.1195
        vmovhpd   24(%rdi,%r8), %xmm3, %xmm8                    #4368.1395
        vinsertf128 $1, %xmm8, %ymm2, %ymm0                     #4368.1149
        vfmadd213pd %ymm0, %ymm4, %ymm5                         #4369.16
        vaddpd    %ymm5, %ymm7, %ymm4                           #4370.17
        vaddpd    %ymm4, %ymm1, %ymm0                           #4371.14
        testl     %r9d, %r9d                                    #4373.66
        jne       ..B5.12       # Prob 5%                       #4373.66
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm6 ymm13 ymm14
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #4618.52
        jne       ..B5.4        # Prob 5%                       #4618.52
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm14
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #4621.12
        popq      %rbp                                          #4621.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4621.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm14, 64(%rsp)                              #4618.200
        vmovupd   %ymm0, 128(%rsp)                              #4618.276
        je        ..B5.3        # Prob 95%                      #4618.380
                                # LOE rbx r12 r13 r14 r15 esi ymm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #4618.460
                                # LOE rbx r12 r13 r14 r15 eax esi
..B5.15:                        # Preds ..B5.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %esi, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4618.523
        jc        ..B5.11       # Prob 5%                       #4618.523
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4618.476
        cmpl      $4, %r12d                                     #4618.471
        jl        ..B5.8        # Prob 82%                      #4618.471
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #4618.682
        jmp       ..B5.3        # Prob 100%                     #4618.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #4618.552
        lea       128(%rsp,%r12,8), %rsi                        #4618.552
..___tag_value___svml_cos4_ha_l9.120:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #4618.552
..___tag_value___svml_cos4_ha_l9.121:
        jmp       ..B5.9        # Prob 100%                     #4618.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   16512+__svml_dcos_ha_data_internal_ha(%rip), %ymm12 #4376.58
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rcx #4468.736
        vmovupd   %ymm0, (%rsp)                                 #[spill]
        vpand     .L_2il0floatpacket.70(%rip), %ymm14, %ymm7    #4466.18
        vandpd    %ymm6, %ymm12, %ymm8                          #4377.17
        vpsrlq    $52, %ymm7, %ymm6                             #4467.18
        vcmpeqpd  %ymm12, %ymm8, %ymm9                          #4378.25
        vmovmskpd %ymm9, %esi                                   #4380.48
        vextractf128 $1, %ymm6, %xmm1                           #4468.328
        vmovd     %xmm6, %edi                                   #4468.124
        vmovd     %xmm1, %r9d                                   #4468.309
        vpextrd   $2, %xmm6, %r8d                               #4468.215
        lea       (%rdi,%rdi,2), %edx                           #4468.547
        vpextrd   $2, %xmm1, %r11d                              #4468.405
        lea       (%r9,%r9,2), %r10d                            #4468.649
        vpand     .L_2il0floatpacket.71(%rip), %ymm14, %ymm6    #4476.21
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r9    #4585.732
        shll      $3, %edx                                      #4468.547
        lea       (%r8,%r8,2), %eax                             #4468.598
        shll      $3, %eax                                      #4468.598
        lea       (%r11,%r11,2), %edi                           #4468.700
        vmovq     16(%rdx,%rcx), %xmm12                         #4470.1284
        vmovhpd   16(%rax,%rcx), %xmm12, %xmm11                 #4470.1252
        shll      $3, %r10d                                     #4468.649
        vmovdqu   .L_2il0floatpacket.73(%rip), %ymm12           #4478.17
        vmovq     16(%r10,%rcx), %xmm9                          #4470.1484
        vpaddq    .L_2il0floatpacket.72(%rip), %ymm6, %ymm1     #4477.21
        vmovq     (%rdx,%rcx), %xmm3                            #4468.1284
        vmovq     (%r10,%rcx), %xmm4                            #4468.1484
        vmovhpd   (%rax,%rcx), %xmm3, %xmm10                    #4468.1252
        shll      $3, %edi                                      #4468.700
        vmovhpd   16(%rdi,%rcx), %xmm9, %xmm7                   #4470.1452
        vmovhpd   (%rdi,%rcx), %xmm4, %xmm0                     #4468.1452
        vpsrlq    $32, %ymm1, %ymm9                             #4485.12
        vpand     %ymm12, %ymm1, %ymm6                          #4486.12
        vmovq     8(%rdx,%rcx), %xmm2                           #4469.1285
        vmovq     8(%r10,%rcx), %xmm8                           #4469.1485
        vmovhpd   8(%rax,%rcx), %xmm2, %xmm15                   #4469.1253
        vmovhpd   8(%rdi,%rcx), %xmm8, %xmm5                    #4469.1453
        vinsertf128 $1, %xmm7, %ymm11, %ymm11                   #4470.1206
        vinsertf128 $1, %xmm0, %ymm10, %ymm10                   #4468.1206
        vpsrlq    $32, %ymm11, %ymm0                            #4483.12
        vpand     %ymm12, %ymm10, %ymm7                         #4480.12
        vpsrlq    $32, %ymm10, %ymm10                           #4479.12
        vpmuludq  %ymm7, %ymm6, %ymm8                           #4494.13
        vinsertf128 $1, %xmm5, %ymm15, %ymm3                    #4469.1207
        vpand     %ymm12, %ymm3, %ymm2                          #4482.12
        vpand     %ymm12, %ymm11, %ymm15                        #4484.12
        vpsrlq    $32, %ymm3, %ymm4                             #4481.12
        vpmuludq  %ymm0, %ymm9, %ymm11                          #4491.13
        vpmuludq  %ymm0, %ymm6, %ymm0                           #4497.13
        vpsrlq    $32, %ymm0, %ymm0                             #4499.13
        vpmuludq  %ymm2, %ymm6, %ymm1                           #4496.13
        vpmuludq  %ymm15, %ymm9, %ymm5                          #4492.13
        vpand     %ymm12, %ymm1, %ymm15                         #4507.13
        vpaddq    %ymm0, %ymm11, %ymm11                         #4503.13
        vpsrlq    $32, %ymm5, %ymm5                             #4517.13
        vpsrlq    $32, %ymm1, %ymm1                             #4500.13
        vpaddq    %ymm11, %ymm15, %ymm15                        #4512.13
        vpmuludq  %ymm4, %ymm6, %ymm3                           #4495.13
        vpmuludq  %ymm2, %ymm9, %ymm2                           #4490.13
        vpaddq    %ymm15, %ymm5, %ymm11                         #4518.13
        vpand     %ymm12, %ymm3, %ymm15                         #4508.13
        vpaddq    %ymm1, %ymm2, %ymm0                           #4504.13
        vpand     %ymm12, %ymm8, %ymm2                          #4509.13
        vpsrlq    $32, %ymm11, %ymm5                            #4519.13
        vpsrlq    $32, %ymm8, %ymm8                             #4502.13
        vpaddq    %ymm0, %ymm15, %ymm15                         #4513.13
        vpaddq    %ymm15, %ymm5, %ymm5                          #4520.13
        vpsrlq    $32, %ymm3, %ymm15                            #4501.13
        vpsrlq    $32, %ymm5, %ymm0                             #4521.13
        vpsllq    $32, %ymm5, %ymm5                             #4528.19
        vpmuludq  %ymm4, %ymm9, %ymm4                           #4489.13
        vpaddq    %ymm15, %ymm4, %ymm3                          #4505.13
        vpaddq    %ymm3, %ymm2, %ymm1                           #4514.13
        vpmuludq  %ymm10, %ymm6, %ymm15                         #4493.13
        vpmuludq  %ymm7, %ymm9, %ymm9                           #4488.13
        vpaddq    %ymm1, %ymm0, %ymm1                           #4522.13
        vpand     %ymm12, %ymm15, %ymm6                         #4510.13
        vpaddq    %ymm8, %ymm9, %ymm7                           #4506.13
        vpsrlq    $32, %ymm1, %ymm4                             #4523.13
        vpand     %ymm12, %ymm1, %ymm1                          #4526.13
        vpaddq    %ymm7, %ymm6, %ymm3                           #4515.13
        vpand     %ymm12, %ymm11, %ymm12                        #4525.13
        vpaddq    %ymm3, %ymm4, %ymm10                          #4524.13
        vpaddq    %ymm12, %ymm5, %ymm6                          #4530.19
        vmovupd   .L_2il0floatpacket.76(%rip), %ymm4            #4538.15
        vpsllq    $32, %ymm10, %ymm0                            #4527.19
        vpand     .L_2il0floatpacket.74(%rip), %ymm14, %ymm10   #4532.17
        vpand     .L_2il0floatpacket.79(%rip), %ymm6, %ymm12    #4548.20
        vpsrlq    $24, %ymm6, %ymm6                             #4559.19
        vpaddq    %ymm1, %ymm0, %ymm3                           #4529.19
        vpxor     .L_2il0floatpacket.75(%rip), %ymm10, %ymm2    #4534.22
        vpxor     .L_2il0floatpacket.78(%rip), %ymm10, %ymm1    #4546.22
        vpsrlq    $12, %ymm3, %ymm11                            #4535.20
        vpand     .L_2il0floatpacket.81(%rip), %ymm3, %ymm3     #4557.20
        vpor      %ymm2, %ymm11, %ymm15                         #4536.20
        vpsllq    $40, %ymm3, %ymm2                             #4558.20
        vpsllq    $28, %ymm12, %ymm11                           #4549.20
        vaddpd    %ymm4, %ymm15, %ymm5                          #4539.19
        vpor      %ymm1, %ymm11, %ymm0                          #4550.20
        vsubpd    %ymm4, %ymm5, %ymm8                           #4540.9
        vpor      %ymm6, %ymm2, %ymm4                           #4560.20
        vsubpd    %ymm8, %ymm15, %ymm7                          #4541.17
        vsubpd    %ymm1, %ymm0, %ymm15                          #4553.17
        vpxor     .L_2il0floatpacket.80(%rip), %ymm10, %ymm8    #4555.22
        vmovupd   .L_2il0floatpacket.85(%rip), %ymm0            #4576.16
        vpor      %ymm8, %ymm4, %ymm10                          #4561.20
        vandps    .L_2il0floatpacket.77(%rip), %ymm5, %ymm9     #4544.15
        vsubpd    %ymm8, %ymm10, %ymm5                          #4564.18
        vaddpd    %ymm5, %ymm7, %ymm12                          #4565.11
        vsubpd    %ymm12, %ymm7, %ymm7                          #4566.17
        vaddpd    %ymm7, %ymm5, %ymm8                           #4567.18
        vmovupd   .L_2il0floatpacket.82(%rip), %ymm5            #4569.16
        vaddpd    %ymm8, %ymm15, %ymm10                         #4568.11
        vmulpd    %ymm12, %ymm5, %ymm2                          #4571.13
        vmovapd   %ymm5, %ymm15                                 #4572.13
        vfmsub213pd %ymm2, %ymm12, %ymm15                       #4572.13
        vfmadd132pd .L_2il0floatpacket.83(%rip), %ymm15, %ymm12 #4573.13
        vandpd    .L_2il0floatpacket.84(%rip), %ymm14, %ymm11   #4577.11
        vcmpgt_oqpd %ymm0, %ymm11, %ymm6                        #4578.17
        vcmple_oqpd %ymm0, %ymm11, %ymm1                        #4579.20
        vfmadd213pd %ymm12, %ymm5, %ymm10                       #4574.13
        vandpd    %ymm14, %ymm1, %ymm3                          #4580.24
        vandpd    %ymm2, %ymm6, %ymm4                           #4581.25
        vorpd     %ymm4, %ymm3, %ymm8                           #4582.9
        vandpd    %ymm10, %ymm6, %ymm5                          #4583.9
        vextractf128 $1, %ymm9, %xmm11                          #4585.330
        vmovd     %xmm9, %eax                                   #4585.132
        vmovd     %xmm11, %ecx                                  #4585.311
        shll      $5, %eax                                      #4585.543
        vpextrd   $2, %xmm9, %edx                               #4585.220
        shll      $5, %ecx                                      #4585.645
        vpextrd   $2, %xmm11, %r8d                              #4585.404
        shll      $5, %edx                                      #4585.594
        vmovq     (%rax,%r9), %xmm15                            #4585.1231
        vmovq     16(%rax,%r9), %xmm2                           #4586.1231
        vmovq     8(%rax,%r9), %xmm7                            #4587.1233
        shll      $5, %r8d                                      #4585.696
        vmovq     (%rcx,%r9), %xmm0                             #4585.1431
        vmovq     16(%rcx,%r9), %xmm4                           #4586.1431
        vmovq     8(%rcx,%r9), %xmm10                           #4587.1433
        vmovhpd   (%rdx,%r9), %xmm15, %xmm12                    #4585.1199
        vmovhpd   16(%rdx,%r9), %xmm2, %xmm3                    #4586.1199
        vmovhpd   8(%rdx,%r9), %xmm7, %xmm9                     #4587.1201
        vmovhpd   (%r8,%r9), %xmm0, %xmm1                       #4585.1399
        vmovhpd   16(%r8,%r9), %xmm4, %xmm6                     #4586.1399
        vmovhpd   8(%r8,%r9), %xmm10, %xmm15                    #4587.1401
        vmovapd   %ymm8, %ymm0                                  #4588.26
        vmulpd    %ymm8, %ymm8, %ymm10                          #4595.19
        vinsertf128 $1, %xmm1, %ymm12, %ymm11                   #4585.1153
        vinsertf128 $1, %xmm6, %ymm3, %ymm12                    #4586.1153
        vinsertf128 $1, %xmm15, %ymm9, %ymm6                    #4587.1155
        vfmadd213pd %ymm12, %ymm6, %ymm0                        #4588.26
        vmovupd   16960+__svml_dcos_ha_data_internal_ha(%rip), %ymm3 #4598.18
        vsubpd    %ymm0, %ymm12, %ymm1                          #4590.22
        vfmadd231pd 17024+__svml_dcos_ha_data_internal_ha(%rip), %ymm10, %ymm3 #4598.18
        vfmadd231pd %ymm8, %ymm6, %ymm1                         #4591.22
        vmulpd    %ymm3, %ymm10, %ymm4                          #4599.18
        vmovaps   %ymm11, %ymm15                                #4589.25
        vfmadd213pd %ymm0, %ymm8, %ymm15                        #4589.25
        vsubpd    %ymm15, %ymm0, %ymm2                          #4592.22
        vmulpd    %ymm4, %ymm8, %ymm0                           #4600.18
        vfmadd231pd %ymm11, %ymm8, %ymm2                        #4593.22
        vaddpd    %ymm2, %ymm1, %ymm7                           #4594.25
        vaddpd    %ymm6, %ymm11, %ymm1                          #4601.17
        vmovupd   17152+__svml_dcos_ha_data_internal_ha(%rip), %ymm11 #4605.18
        vfmadd213pd %ymm7, %ymm1, %ymm0                         #4602.21
        vfnmadd213pd %ymm1, %ymm12, %ymm8                       #4610.17
        vmovq     24(%rcx,%r9), %xmm1                           #4611.1431
        vfmadd231pd 17216+__svml_dcos_ha_data_internal_ha(%rip), %ymm10, %ymm11 #4605.18
        vfmadd213pd 17088+__svml_dcos_ha_data_internal_ha(%rip), %ymm10, %ymm11 #4607.18
        vmulpd    %ymm11, %ymm10, %ymm3                         #4608.18
        vfmadd213pd %ymm0, %ymm12, %ymm3                        #4609.21
        vmovq     24(%rax,%r9), %xmm12                          #4611.1231
        vmovhpd   24(%rdx,%r9), %xmm12, %xmm0                   #4611.1199
        vmovhpd   24(%r8,%r9), %xmm1, %xmm12                    #4611.1399
        vinsertf128 $1, %xmm12, %ymm0, %ymm2                    #4611.1153
        vfmadd213pd %ymm2, %ymm8, %ymm5                         #4612.20
        vmovupd   (%rsp), %ymm0                                 #4615.18[spill]
        vaddpd    %ymm5, %ymm3, %ymm5                           #4613.21
        vaddpd    %ymm5, %ymm15, %ymm8                          #4614.24
        vblendvpd %ymm13, %ymm8, %ymm0, %ymm0                   #4615.18
        jmp       ..B5.2        # Prob 100%                     #4615.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_cos4_ha_l9,@function
	.size	__svml_cos4_ha_l9,.-__svml_cos4_ha_l9
..LN__svml_cos4_ha_l9.4:
	.data
# -- End  __svml_cos4_ha_l9
	.text
.L_2__routine_start___svml_cos2_ha_l9_5:
# -- Begin  __svml_cos2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos2_ha_l9
# --- __svml_cos2_ha_l9(__m128d)
__svml_cos2_ha_l9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4626.1
        .byte     15                                            #4972.18
        .byte     30                                            #4972.18
        .byte     250                                           #4972.18
	.cfi_startproc
..___tag_value___svml_cos2_ha_l9.127:
..L128:
                                                        #4626.1
        pushq     %rbp                                          #4626.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4626.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4626.1
        subq      $192, %rsp                                    #4626.1
        vmovapd   %xmm0, %xmm14                                 #4626.1
        vmovupd   16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #4684.46
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rsi   #4699.383
        vmovupd   16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #4685.51
        xorl      %eax, %eax                                    #4678.1
        vfmadd213pd %xmm8, %xmm14, %xmm12                       #4686.13
        vmovupd   16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #4688.48
        vmovupd   16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #4690.48
        vandpd    16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm14, %xmm7 #4681.13
        vcmpnlepd 16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm7, %xmm0 #4682.26
        vsubpd    %xmm8, %xmm12, %xmm5                          #4687.13
        vmovmskpd %xmm0, %edi                                   #4729.78
        vandps    16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm12, %xmm13 #4698.19
        vfnmadd213pd %xmm14, %xmm5, %xmm9                       #4689.14
        vmovd     %xmm13, %edx                                  #4699.128
        vmovapd   %xmm9, %xmm6                                  #4691.17
        vfnmadd231pd %xmm5, %xmm10, %xmm6                       #4691.17
        shll      $5, %edx                                      #4699.298
        vmovapd   %xmm6, %xmm8                                  #4702.22
        vpextrd   $2, %xmm13, %ecx                              #4699.190
        vsubpd    %xmm6, %xmm9, %xmm11                          #4692.17
        shll      $5, %ecx                                      #4699.349
        vmovq     16(%rdx,%rsi), %xmm2                          #4700.612
        vmovq     8(%rdx,%rsi), %xmm1                           #4701.614
        vmovhpd   16(%rcx,%rsi), %xmm2, %xmm3                   #4700.580
        vmovhpd   8(%rcx,%rsi), %xmm1, %xmm1                    #4701.582
        vmovq     (%rdx,%rsi), %xmm15                           #4699.612
        vfmadd213pd %xmm3, %xmm1, %xmm8                         #4702.22
        vmovhpd   (%rcx,%rsi), %xmm15, %xmm4                    #4699.580
        vmulpd    %xmm6, %xmm6, %xmm15                          #4709.15
        vfnmadd231pd %xmm5, %xmm10, %xmm11                      #4693.17
        vmovapd   %xmm8, %xmm2                                  #4703.21
        vfmadd231pd %xmm4, %xmm6, %xmm2                         #4703.21
        vsubpd    %xmm8, %xmm3, %xmm9                           #4704.18
        vfnmadd132pd 16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm11, %xmm5 #4695.13
        vsubpd    %xmm2, %xmm8, %xmm10                          #4706.18
        vfmadd231pd %xmm6, %xmm1, %xmm9                         #4705.18
        vaddpd    %xmm1, %xmm4, %xmm1                           #4715.13
        vfmadd231pd %xmm4, %xmm6, %xmm10                        #4707.18
        vmovupd   16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #4712.14
        vmovupd   17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm4 #4719.14
        vfmadd231pd 17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm15, %xmm11 #4712.14
        vfmadd231pd 17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm15, %xmm4 #4719.14
        vaddpd    %xmm10, %xmm9, %xmm13                         #4708.21
        vmulpd    %xmm11, %xmm15, %xmm12                        #4713.14
        vfmadd213pd 17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm15, %xmm4 #4721.14
        vmulpd    %xmm12, %xmm6, %xmm8                          #4714.14
        vmulpd    %xmm4, %xmm15, %xmm9                          #4722.14
        vfmadd213pd %xmm13, %xmm1, %xmm8                        #4716.17
        vfnmadd213pd %xmm1, %xmm3, %xmm6                        #4724.13
        vfmadd213pd %xmm8, %xmm3, %xmm9                         #4723.17
        vmovq     24(%rdx,%rsi), %xmm3                          #4725.612
        vmovhpd   24(%rcx,%rsi), %xmm3, %xmm1                   #4725.580
        vfmadd213pd %xmm1, %xmm6, %xmm5                         #4726.16
        vaddpd    %xmm5, %xmm9, %xmm5                           #4727.17
        vaddpd    %xmm5, %xmm2, %xmm1                           #4728.14
        testl     %edi, %edi                                    #4730.66
        jne       ..B6.12       # Prob 5%                       #4730.66
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm7 xmm14
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #4975.52
        jne       ..B6.4        # Prob 5%                       #4975.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #4978.12
        movq      %rbp, %rsp                                    #4978.12
        popq      %rbp                                          #4978.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4978.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm14, 64(%rsp)                              #4975.197
        vmovupd   %xmm1, 128(%rsp)                              #4975.270
        je        ..B6.3        # Prob 95%                      #4975.374
                                # LOE rbx r12 r13 r14 r15 eax xmm1
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4975.454
        movq      %r12, 8(%rsp)                                 #4975.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #4975.454
        movq      %r13, (%rsp)                                  #4975.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #4975.454
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4975.517
        jc        ..B6.11       # Prob 5%                       #4975.517
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4975.470
        cmpl      $2, %r12d                                     #4975.465
        jl        ..B6.8        # Prob 82%                      #4975.465
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #4975.673
        jmp       ..B6.3        # Prob 100%                     #4975.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #4975.546
        lea       128(%rsp,%r12,8), %rsi                        #4975.546
..___tag_value___svml_cos2_ha_l9.145:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #4975.546
..___tag_value___svml_cos2_ha_l9.146:
        jmp       ..B6.9        # Prob 100%                     #4975.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #4733.55
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %r8 #4825.385
        vpand     .L_2il0floatpacket.34(%rip), %xmm14, %xmm8    #4823.18
        vandpd    %xmm7, %xmm13, %xmm9                          #4734.17
        vpsrlq    $52, %xmm8, %xmm7                             #4824.18
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r11   #4942.387
        vmovd     %xmm7, %edx                                   #4825.124
        vmovupd   %xmm1, (%rsp)                                 #[spill]
        vpand     .L_2il0floatpacket.35(%rip), %xmm14, %xmm1    #4833.21
        vpextrd   $2, %xmm7, %esi                               #4825.189
        vcmpeqpd  %xmm13, %xmm9, %xmm10                         #4735.25
        vpaddq    .L_2il0floatpacket.36(%rip), %xmm1, %xmm15    #4834.21
        vmovmskpd %xmm10, %eax                                  #4737.48
        vmovdqu   .L_2il0floatpacket.37(%rip), %xmm13           #4835.17
        vpsrlq    $32, %xmm15, %xmm10                           #4842.12
        vpand     %xmm13, %xmm15, %xmm7                         #4843.12
        lea       (%rdx,%rdx,2), %ecx                           #4825.300
        shll      $3, %ecx                                      #4825.300
        lea       (%rsi,%rsi,2), %edi                           #4825.351
        shll      $3, %edi                                      #4825.351
        vmovq     16(%rcx,%r8), %xmm5                           #4827.639
        vmovhpd   16(%rdi,%r8), %xmm5, %xmm12                   #4827.607
        vpand     %xmm13, %xmm12, %xmm6                         #4841.12
        vpsrlq    $32, %xmm12, %xmm1                            #4840.12
        vpmuludq  %xmm6, %xmm10, %xmm12                         #4849.13
        vmovq     8(%rcx,%r8), %xmm2                            #4826.640
        vpsrlq    $32, %xmm12, %xmm6                            #4874.13
        vmovhpd   8(%rdi,%r8), %xmm2, %xmm3                     #4826.608
        vpmuludq  %xmm1, %xmm10, %xmm12                         #4848.13
        vpsrlq    $32, %xmm3, %xmm5                             #4838.12
        vpmuludq  %xmm1, %xmm7, %xmm1                           #4854.13
        vpand     %xmm13, %xmm3, %xmm3                          #4839.12
        vpsrlq    $32, %xmm1, %xmm1                             #4856.13
        vpmuludq  %xmm3, %xmm7, %xmm2                           #4853.13
        vpmuludq  %xmm3, %xmm10, %xmm3                          #4847.13
        vpaddq    %xmm1, %xmm12, %xmm12                         #4860.13
        vmovq     (%rcx,%r8), %xmm4                             #4825.639
        vpand     %xmm13, %xmm2, %xmm15                         #4864.13
        vmovhpd   (%rdi,%r8), %xmm4, %xmm11                     #4825.607
        vpsrlq    $32, %xmm2, %xmm2                             #4857.13
        vpmuludq  %xmm5, %xmm7, %xmm4                           #4852.13
        vpand     %xmm13, %xmm11, %xmm8                         #4837.12
        vpmuludq  %xmm5, %xmm10, %xmm5                          #4846.13
        vpmuludq  %xmm8, %xmm7, %xmm9                           #4851.13
        vpmuludq  %xmm8, %xmm10, %xmm10                         #4845.13
        vpaddq    %xmm12, %xmm15, %xmm15                        #4869.13
        vpaddq    %xmm2, %xmm3, %xmm2                           #4861.13
        vpaddq    %xmm15, %xmm6, %xmm12                         #4875.13
        vpand     %xmm13, %xmm4, %xmm15                         #4865.13
        vpsrlq    $32, %xmm12, %xmm6                            #4876.13
        vpaddq    %xmm2, %xmm15, %xmm15                         #4870.13
        vpsrlq    $32, %xmm4, %xmm4                             #4858.13
        vpand     %xmm13, %xmm9, %xmm3                          #4866.13
        vpaddq    %xmm15, %xmm6, %xmm6                          #4877.13
        vpaddq    %xmm4, %xmm5, %xmm15                          #4862.13
        vpsrlq    $32, %xmm11, %xmm11                           #4836.12
        vpsrlq    $32, %xmm9, %xmm9                             #4859.13
        vpaddq    %xmm15, %xmm3, %xmm4                          #4871.13
        vpaddq    %xmm9, %xmm10, %xmm8                          #4863.13
        vpmuludq  %xmm11, %xmm7, %xmm15                         #4850.13
        vpsrlq    $32, %xmm6, %xmm1                             #4878.13
        vpand     %xmm13, %xmm15, %xmm7                         #4867.13
        vpaddq    %xmm4, %xmm1, %xmm2                           #4879.13
        vpaddq    %xmm8, %xmm7, %xmm4                           #4872.13
        vpsrlq    $32, %xmm2, %xmm5                             #4880.13
        vpand     %xmm13, %xmm2, %xmm2                          #4883.13
        vpaddq    %xmm4, %xmm5, %xmm11                          #4881.13
        vpsllq    $32, %xmm11, %xmm1                            #4884.19
        vpsllq    $32, %xmm6, %xmm6                             #4885.19
        vpaddq    %xmm2, %xmm1, %xmm4                           #4886.19
        vpand     .L_2il0floatpacket.38(%rip), %xmm14, %xmm11   #4889.17
        vpand     %xmm13, %xmm12, %xmm13                        #4882.13
        vpxor     .L_2il0floatpacket.39(%rip), %xmm11, %xmm9    #4891.22
        vpsrlq    $12, %xmm4, %xmm15                            #4892.20
        vpaddq    %xmm13, %xmm6, %xmm7                          #4887.19
        vmovups   .L_2il0floatpacket.40(%rip), %xmm6            #4895.15
        vpor      %xmm9, %xmm15, %xmm13                         #4893.20
        vaddpd    %xmm6, %xmm13, %xmm10                         #4896.19
        vsubpd    %xmm6, %xmm10, %xmm12                         #4897.9
        vpand     .L_2il0floatpacket.45(%rip), %xmm4, %xmm9     #4914.20
        vsubpd    %xmm12, %xmm13, %xmm8                         #4898.17
        vpand     .L_2il0floatpacket.43(%rip), %xmm7, %xmm1     #4905.20
        vpsllq    $40, %xmm9, %xmm6                             #4915.20
        vpsrlq    $24, %xmm7, %xmm13                            #4916.19
        vpsllq    $28, %xmm1, %xmm2                             #4906.20
        vpxor     .L_2il0floatpacket.44(%rip), %xmm11, %xmm1    #4912.22
        vpor      %xmm13, %xmm6, %xmm12                         #4917.20
        vpxor     .L_2il0floatpacket.42(%rip), %xmm11, %xmm5    #4903.22
        vpor      %xmm1, %xmm12, %xmm4                          #4918.20
        vpor      %xmm5, %xmm2, %xmm3                           #4907.20
        vsubpd    %xmm1, %xmm4, %xmm2                           #4921.18
        vsubpd    %xmm5, %xmm3, %xmm15                          #4910.17
        vaddpd    %xmm2, %xmm8, %xmm7                           #4922.11
        vsubpd    %xmm7, %xmm8, %xmm8                           #4923.17
        vmovups   .L_2il0floatpacket.46(%rip), %xmm5            #4926.16
        vmulpd    %xmm7, %xmm5, %xmm6                           #4928.13
        vaddpd    %xmm8, %xmm2, %xmm3                           #4924.18
        vaddpd    %xmm3, %xmm15, %xmm1                          #4925.11
        vmovaps   %xmm5, %xmm15                                 #4929.13
        vfmsub213pd %xmm6, %xmm7, %xmm15                        #4929.13
        vandpd    .L_2il0floatpacket.50(%rip), %xmm14, %xmm11   #4934.11
        vandps    .L_2il0floatpacket.41(%rip), %xmm10, %xmm10   #4901.15
        vmovd     %xmm10, %r9d                                  #4942.132
        vmovupd   16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm4 #4955.18
        vfmadd132pd .L_2il0floatpacket.69(%rip), %xmm15, %xmm7  #4930.13
        vmovups   .L_2il0floatpacket.51(%rip), %xmm15           #4933.16
        vcmplepd  %xmm15, %xmm11, %xmm9                         #4936.20
        vfmadd213pd %xmm7, %xmm5, %xmm1                         #4931.13
        vcmpltpd  %xmm11, %xmm15, %xmm5                         #4935.17
        shll      $5, %r9d                                      #4942.302
        vandpd    %xmm14, %xmm9, %xmm13                         #4937.24
        vpextrd   $2, %xmm10, %r10d                             #4942.194
        vandpd    %xmm6, %xmm5, %xmm12                          #4938.25
        shll      $5, %r10d                                     #4942.353
        vorpd     %xmm12, %xmm13, %xmm9                         #4939.9
        vmovq     16(%r9,%r11), %xmm10                          #4943.616
        vandpd    %xmm1, %xmm5, %xmm6                           #4940.9
        vmovq     8(%r9,%r11), %xmm15                           #4944.618
        vmovapd   %xmm9, %xmm1                                  #4945.26
        vmovhpd   16(%r10,%r11), %xmm10, %xmm13                 #4943.584
        vmovhpd   8(%r10,%r11), %xmm15, %xmm7                   #4944.586
        vmovapd   %xmm9, %xmm15                                 #4946.25
        vmulpd    %xmm9, %xmm9, %xmm10                          #4952.19
        vfmadd213pd %xmm13, %xmm7, %xmm1                        #4945.26
        vmovq     (%r9,%r11), %xmm2                             #4942.616
        vmovhpd   (%r10,%r11), %xmm2, %xmm12                    #4942.584
        vfmadd231pd 17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm10, %xmm4 #4955.18
        vfmadd132pd %xmm12, %xmm1, %xmm15                       #4946.25
        vsubpd    %xmm1, %xmm13, %xmm2                          #4947.22
        vmulpd    %xmm4, %xmm10, %xmm5                          #4956.18
        vsubpd    %xmm15, %xmm1, %xmm3                          #4949.22
        vaddpd    %xmm7, %xmm12, %xmm1                          #4958.17
        vmulpd    %xmm5, %xmm9, %xmm11                          #4957.18
        vfmadd231pd %xmm12, %xmm9, %xmm3                        #4950.22
        vmovupd   17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #4962.18
        vfmadd231pd 17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm10, %xmm12 #4962.18
        vfmadd231pd %xmm9, %xmm7, %xmm2                         #4948.22
        vfnmadd213pd %xmm1, %xmm13, %xmm9                       #4967.17
        vfmadd213pd 17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm10, %xmm12 #4964.18
        vaddpd    %xmm3, %xmm2, %xmm8                           #4951.25
        vmulpd    %xmm12, %xmm10, %xmm2                         #4965.18
        vfmadd213pd %xmm8, %xmm1, %xmm11                        #4959.21
        vfmadd213pd %xmm11, %xmm13, %xmm2                       #4966.21
        vmovq     24(%r9,%r11), %xmm13                          #4968.616
        vmovhpd   24(%r10,%r11), %xmm13, %xmm1                  #4968.584
        vfmadd213pd %xmm1, %xmm9, %xmm6                         #4969.20
        vmovupd   (%rsp), %xmm1                                 #4972.18[spill]
        vaddpd    %xmm6, %xmm2, %xmm6                           #4970.21
        vaddpd    %xmm6, %xmm15, %xmm9                          #4971.24
        blendvpd  %xmm0, %xmm9, %xmm1                           #4972.18
        jmp       ..B6.2        # Prob 100%                     #4972.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_cos2_ha_l9,@function
	.size	__svml_cos2_ha_l9,.-__svml_cos2_ha_l9
..LN__svml_cos2_ha_l9.5:
	.data
# -- End  __svml_cos2_ha_l9
	.text
.L_2__routine_start___svml_cos1_ha_ex_6:
# -- Begin  __svml_cos1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos1_ha_ex
# --- __svml_cos1_ha_ex(__m128d)
__svml_cos1_ha_ex:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4983.1
        .byte     15                                            #5345.18
        .byte     30                                            #5345.18
        .byte     250                                           #5345.18
	.cfi_startproc
..___tag_value___svml_cos1_ha_ex.152:
..L153:
                                                        #4983.1
        pushq     %rbp                                          #4983.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4983.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4983.1
        subq      $128, %rsp                                    #4983.1
        movaps    %xmm0, %xmm2                                  #4983.1
        movaps    %xmm2, %xmm11                                 #5043.25
        movaps    %xmm2, %xmm4                                  #5046.14
        mulsd     16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm11 #5043.25
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rax   #5057.365
        movsd     16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5042.20
        xorl      %edx, %edx                                    #5035.1
        movq      16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #5055.21
        addsd     %xmm6, %xmm11                                 #5043.13
        movaps    %xmm11, %xmm9                                 #5044.13
        pand      %xmm10, %xmm11                                #5056.17
        movd      %xmm11, %ecx                                  #5057.128
        subsd     %xmm6, %xmm9                                  #5044.13
        movaps    %xmm9, %xmm7                                  #5046.34
        movaps    %xmm9, %xmm8                                  #5048.19
        mulsd     16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #5046.34
        mulsd     16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #5048.19
        mulsd     16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5053.32
        subsd     %xmm7, %xmm4                                  #5046.14
        movaps    %xmm4, %xmm3                                  #5049.17
        shll      $5, %ecx                                      #5057.225
        subsd     %xmm8, %xmm3                                  #5049.17
        movq      8(%rax,%rcx), %xmm15                          #5059.367
        movaps    %xmm3, %xmm12                                 #5061.22
        mulsd     %xmm15, %xmm12                                #5061.22
        subsd     %xmm3, %xmm4                                  #5050.17
        movq      16(%rax,%rcx), %xmm7                          #5058.365
        subsd     %xmm8, %xmm4                                  #5051.17
        movq      (%rcx,%rax), %xmm8                            #5057.365
        movdqa    %xmm7, %xmm6                                  #5062.22
        movdqa    %xmm8, %xmm13                                 #5060.19
        movdqa    %xmm7, %xmm14                                 #5064.18
        mulsd     %xmm3, %xmm13                                 #5060.19
        addsd     %xmm12, %xmm6                                 #5062.22
        subsd     %xmm9, %xmm4                                  #5053.13
        addsd     %xmm15, %xmm8                                 #5075.13
        subsd     %xmm6, %xmm14                                 #5064.18
        movaps    %xmm13, %xmm0                                 #5063.21
        addsd     %xmm12, %xmm14                                #5065.18
        addsd     %xmm6, %xmm0                                  #5063.21
        movaps    %xmm3, %xmm12                                 #5069.15
        subsd     %xmm0, %xmm6                                  #5066.18
        mulsd     %xmm3, %xmm12                                 #5069.15
        addsd     %xmm13, %xmm6                                 #5067.18
        movsd     17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #5072.26
        addsd     %xmm14, %xmm6                                 #5068.21
        mulsd     %xmm12, %xmm13                                #5072.26
        movsd     17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5079.26
        mulsd     %xmm12, %xmm9                                 #5079.26
        addsd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm13 #5072.14
        mulsd     %xmm12, %xmm13                                #5073.14
        addsd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5079.14
        mulsd     %xmm12, %xmm9                                 #5081.26
        mulsd     %xmm3, %xmm13                                 #5074.14
        mulsd     %xmm7, %xmm3                                  #5084.32
        addsd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5081.14
        mulsd     %xmm8, %xmm13                                 #5076.29
        subsd     %xmm3, %xmm8                                  #5084.13
        mulsd     %xmm12, %xmm9                                 #5082.14
        addsd     %xmm6, %xmm13                                 #5076.17
        mulsd     %xmm8, %xmm4                                  #5086.28
        mulsd     %xmm7, %xmm9                                  #5083.29
        addsd     24(%rax,%rcx), %xmm4                          #5086.16
        addsd     %xmm13, %xmm9                                 #5083.17
        movsd     16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm5 #5038.13
        addsd     %xmm4, %xmm9                                  #5087.17
        andps     %xmm2, %xmm5                                  #5038.13
        addsd     %xmm9, %xmm0                                  #5088.14
        movaps    %xmm5, %xmm1                                  #5039.26
        cmpnlesd  16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm1 #5039.26
        movmskpd  %xmm1, %esi                                   #5089.78
        testl     $1, %esi                                      #5090.55
        jne       ..B7.8        # Prob 5%                       #5090.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm5
..B7.2:                         # Preds ..B7.8 ..B7.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #5348.52
        jne       ..B7.4        # Prob 5%                       #5348.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
..B7.3:                         # Preds ..B7.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #5351.12
        popq      %rbp                                          #5351.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5351.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.4:                         # Preds ..B7.2
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm2, (%rsp)                                 #5348.160
        movsd     %xmm0, 64(%rsp)                               #5348.233
        jne       ..B7.6        # Prob 5%                       #5348.374
                                # LOE rbx r12 r13 r14 r15 edx
..B7.5:                         # Preds ..B7.7 ..B7.6 ..B7.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #5348.625
        movq      %rbp, %rsp                                    #5348.625
        popq      %rbp                                          #5348.625
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5348.625
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B7.6:                         # Preds ..B7.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B7.5        # Prob 95%                      #5348.517
                                # LOE rbx r12 r13 r14 r15
..B7.7:                         # Preds ..B7.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #5348.546
        lea       64(%rsp), %rsi                                #5348.546
..___tag_value___svml_cos1_ha_ex.165:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #5348.546
..___tag_value___svml_cos1_ha_ex.166:
        jmp       ..B7.5        # Prob 100%                     #5348.546
                                # LOE rbx r12 r13 r14 r15
..B7.8:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, (%rsp)                                 #[spill]
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rdi #5185.377
        movsd     16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm0 #5093.24
        andps     %xmm0, %xmm5                                  #5094.17
        movdqu    .L_2il0floatpacket.34(%rip), %xmm6            #5183.16
        cmpeqsd   %xmm0, %xmm5                                  #5095.25
        pand      %xmm2, %xmm6                                  #5183.16
        psrlq     $52, %xmm6                                    #5184.16
        movd      %xmm6, %ecx                                   #5185.124
        movmskpd  %xmm5, %edx                                   #5097.48
        movdqu    .L_2il0floatpacket.35(%rip), %xmm5            #5193.19
        pand      %xmm2, %xmm5                                  #5193.19
        paddq     .L_2il0floatpacket.36(%rip), %xmm5            #5194.19
        movdqa    %xmm5, %xmm4                                  #5202.10
        lea       (%rcx,%rcx,2), %esi                           #5185.224
        shll      $3, %esi                                      #5185.224
        psrlq     $32, %xmm4                                    #5202.10
        movq      16(%rdi,%rsi), %xmm13                         #5187.377
        movdqa    %xmm4, %xmm0                                  #5208.11
        movdqu    .L_2il0floatpacket.37(%rip), %xmm14           #5195.15
        movdqa    %xmm13, %xmm8                                 #5200.10
        psrlq     $32, %xmm8                                    #5200.10
        pand      %xmm14, %xmm5                                 #5203.10
        movq      8(%rdi,%rsi), %xmm11                          #5186.378
        movdqa    %xmm5, %xmm9                                  #5213.11
        pmuludq   %xmm8, %xmm0                                  #5208.11
        movdqa    %xmm11, %xmm12                                #5198.10
        pmuludq   %xmm5, %xmm8                                  #5214.11
        pand      %xmm14, %xmm11                                #5199.10
        pand      %xmm14, %xmm13                                #5201.10
        pmuludq   %xmm11, %xmm9                                 #5213.11
        psrlq     $32, %xmm8                                    #5216.11
        pmuludq   %xmm4, %xmm13                                 #5209.11
        pmuludq   %xmm4, %xmm11                                 #5207.11
        paddq     %xmm8, %xmm0                                  #5220.11
        psrlq     $32, %xmm12                                   #5198.10
        movdqa    %xmm5, %xmm10                                 #5212.11
        movdqa    %xmm14, %xmm6                                 #5224.11
        movdqa    %xmm14, %xmm15                                #5197.10
        pmuludq   %xmm12, %xmm10                                #5212.11
        pand      %xmm9, %xmm6                                  #5224.11
        pmuludq   %xmm4, %xmm12                                 #5206.11
        paddq     %xmm0, %xmm6                                  #5229.11
        movq      (%rsi,%rdi), %xmm7                            #5185.377
        psrlq     $32, %xmm13                                   #5234.11
        psrlq     $32, %xmm9                                    #5217.11
        pand      %xmm7, %xmm15                                 #5197.10
        movdqa    %xmm5, %xmm3                                  #5211.11
        movdqa    %xmm14, %xmm8                                 #5225.11
        paddq     %xmm6, %xmm13                                 #5235.11
        paddq     %xmm9, %xmm11                                 #5221.11
        pmuludq   %xmm15, %xmm3                                 #5211.11
        pmuludq   %xmm15, %xmm4                                 #5205.11
        pand      %xmm10, %xmm8                                 #5225.11
        movdqa    %xmm13, %xmm0                                 #5236.11
        paddq     %xmm11, %xmm8                                 #5230.11
        psrlq     $32, %xmm0                                    #5236.11
        psrlq     $32, %xmm10                                   #5218.11
        psrlq     $32, %xmm7                                    #5196.10
        movdqa    %xmm14, %xmm9                                 #5226.11
        paddq     %xmm8, %xmm0                                  #5237.11
        paddq     %xmm10, %xmm12                                #5222.11
        pmuludq   %xmm7, %xmm5                                  #5210.11
        pand      %xmm3, %xmm9                                  #5226.11
        movdqa    %xmm0, %xmm11                                 #5238.11
        paddq     %xmm12, %xmm9                                 #5231.11
        psrlq     $32, %xmm11                                   #5238.11
        psrlq     $32, %xmm3                                    #5219.11
        paddq     %xmm9, %xmm11                                 #5239.11
        paddq     %xmm3, %xmm4                                  #5223.11
        pand      %xmm14, %xmm5                                 #5227.11
        movdqa    %xmm11, %xmm8                                 #5240.11
        paddq     %xmm4, %xmm5                                  #5232.11
        psrlq     $32, %xmm8                                    #5240.11
        pand      %xmm14, %xmm11                                #5243.11
        paddq     %xmm5, %xmm8                                  #5241.11
        psllq     $32, %xmm8                                    #5244.17
        psllq     $32, %xmm0                                    #5245.17
        paddq     %xmm11, %xmm8                                 #5246.17
        movdqu    .L_2il0floatpacket.38(%rip), %xmm3            #5249.15
        movdqa    %xmm8, %xmm4                                  #5252.18
        movdqu    .L_2il0floatpacket.39(%rip), %xmm10           #5251.20
        pand      %xmm2, %xmm3                                  #5249.15
        movups    .L_2il0floatpacket.40(%rip), %xmm12           #5255.15
        psrlq     $12, %xmm4                                    #5252.18
        pxor      %xmm3, %xmm10                                 #5251.20
        movaps    %xmm12, %xmm7                                 #5256.19
        por       %xmm10, %xmm4                                 #5253.18
        pand      %xmm14, %xmm13                                #5242.11
        paddq     %xmm13, %xmm0                                 #5247.17
        addsd     %xmm4, %xmm7                                  #5256.19
        movaps    %xmm7, %xmm13                                 #5257.9
        andl      $1, %edx                                      #5097.102
        movdqu    .L_2il0floatpacket.45(%rip), %xmm6            #5274.18
        subsd     %xmm12, %xmm13                                #5257.9
        movdqu    .L_2il0floatpacket.43(%rip), %xmm5            #5265.18
        pand      %xmm8, %xmm6                                  #5274.18
        movdqu    .L_2il0floatpacket.42(%rip), %xmm14           #5263.20
        pand      %xmm0, %xmm5                                  #5265.18
        psllq     $40, %xmm6                                    #5275.18
        psrlq     $24, %xmm0                                    #5276.17
        pxor      %xmm3, %xmm14                                 #5263.20
        por       %xmm0, %xmm6                                  #5277.18
        pxor      .L_2il0floatpacket.44(%rip), %xmm3            #5272.20
        psllq     $28, %xmm5                                    #5266.18
        por       %xmm3, %xmm6                                  #5278.18
        por       %xmm14, %xmm5                                 #5267.18
        movups    .L_2il0floatpacket.49(%rip), %xmm0            #5290.16
        subsd     %xmm13, %xmm4                                 #5258.17
        subsd     %xmm3, %xmm6                                  #5281.18
        subsd     %xmm14, %xmm5                                 #5270.17
        movaps    %xmm4, %xmm8                                  #5282.11
        movups    .L_2il0floatpacket.47(%rip), %xmm15           #5287.16
        addsd     %xmm6, %xmm8                                  #5282.11
        andps     %xmm8, %xmm0                                  #5290.16
        subsd     %xmm8, %xmm4                                  #5283.17
        subsd     %xmm0, %xmm8                                  #5291.17
        addsd     %xmm4, %xmm6                                  #5284.18
        movups    .L_2il0floatpacket.48(%rip), %xmm4            #5288.17
        addsd     %xmm5, %xmm6                                  #5285.11
        movups    .L_2il0floatpacket.46(%rip), %xmm10           #5295.12
        movaps    %xmm4, %xmm9                                  #5294.12
        mulsd     %xmm0, %xmm9                                  #5294.12
        movaps    %xmm15, %xmm5                                 #5292.16
        mulsd     %xmm8, %xmm15                                 #5293.12
        mulsd     %xmm6, %xmm10                                 #5295.12
        mulsd     %xmm8, %xmm4                                  #5296.12
        addsd     %xmm9, %xmm15                                 #5297.13
        mulsd     %xmm0, %xmm5                                  #5292.16
        addsd     %xmm4, %xmm10                                 #5298.13
        movaps    %xmm5, %xmm12                                 #5300.13
        addsd     %xmm10, %xmm15                                #5299.13
        movups    .L_2il0floatpacket.51(%rip), %xmm11           #5304.16
        addsd     %xmm15, %xmm12                                #5300.13
        movups    .L_2il0floatpacket.50(%rip), %xmm3            #5305.11
        movaps    %xmm11, %xmm6                                 #5306.17
        andps     %xmm2, %xmm3                                  #5305.11
        subsd     %xmm12, %xmm5                                 #5301.16
        cmpltsd   %xmm3, %xmm6                                  #5306.17
        addsd     %xmm15, %xmm5                                 #5302.13
        movaps    %xmm3, %xmm4                                  #5306.17
        cmplesd   %xmm11, %xmm3                                 #5307.20
        andps     .L_2il0floatpacket.41(%rip), %xmm7            #5261.13
        movsd     %xmm6, %xmm4                                  #5306.17
        movd      %xmm7, %r8d                                   #5313.132
        andps     %xmm2, %xmm3                                  #5308.24
        andps     %xmm4, %xmm12                                 #5309.25
        andps     %xmm5, %xmm4                                  #5311.9
        orps      %xmm12, %xmm3                                 #5310.9
        shll      $5, %r8d                                      #5313.229
        movaps    %xmm3, %xmm7                                  #5317.26
        mulsd     8(%rax,%r8), %xmm7                            #5317.26
        movq      16(%rax,%r8), %xmm14                          #5314.369
        movdqa    %xmm14, %xmm5                                 #5318.26
        movdqa    %xmm14, %xmm0                                 #5320.22
        movq      (%r8,%rax), %xmm8                             #5313.369
        addsd     %xmm7, %xmm5                                  #5318.26
        movdqa    %xmm8, %xmm13                                 #5316.23
        subsd     %xmm5, %xmm0                                  #5320.22
        mulsd     %xmm3, %xmm13                                 #5316.23
        addsd     8(%rax,%r8), %xmm8                            #5331.17
        addsd     %xmm7, %xmm0                                  #5321.22
        movaps    %xmm3, %xmm7                                  #5325.19
        movaps    %xmm13, %xmm10                                #5319.25
        mulsd     %xmm3, %xmm7                                  #5325.19
        addsd     %xmm5, %xmm10                                 #5319.25
        movsd     17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5328.30
        subsd     %xmm10, %xmm5                                 #5322.22
        mulsd     %xmm7, %xmm6                                  #5328.30
        addsd     %xmm13, %xmm5                                 #5323.22
        addsd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5328.18
        addsd     %xmm0, %xmm5                                  #5324.25
        mulsd     %xmm7, %xmm6                                  #5329.18
        movsd     17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5335.30
        movaps    %xmm1, %xmm0                                  #5345.29
        mulsd     %xmm7, %xmm9                                  #5335.30
        mulsd     %xmm3, %xmm6                                  #5330.18
        mulsd     %xmm14, %xmm3                                 #5340.36
        addsd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5335.18
        mulsd     %xmm8, %xmm6                                  #5332.33
        subsd     %xmm3, %xmm8                                  #5340.17
        mulsd     %xmm7, %xmm9                                  #5337.30
        addsd     %xmm5, %xmm6                                  #5332.21
        mulsd     %xmm8, %xmm4                                  #5342.32
        addsd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5337.18
        addsd     24(%rax,%r8), %xmm4                           #5342.20
        mulsd     %xmm7, %xmm9                                  #5338.18
        mulsd     %xmm14, %xmm9                                 #5339.33
        andnps    (%rsp), %xmm0                                 #5345.29[spill]
        addsd     %xmm6, %xmm9                                  #5339.21
        addsd     %xmm4, %xmm9                                  #5343.21
        addsd     %xmm9, %xmm10                                 #5344.24
        andps     %xmm1, %xmm10                                 #5345.78
        orps      %xmm10, %xmm0                                 #5345.18
        jmp       ..B7.2        # Prob 100%                     #5345.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm2
	.cfi_endproc
# mark_end;
	.type	__svml_cos1_ha_ex,@function
	.size	__svml_cos1_ha_ex,.-__svml_cos1_ha_ex
..LN__svml_cos1_ha_ex.6:
	.data
# -- End  __svml_cos1_ha_ex
	.text
.L_2__routine_start___svml_cos1_ha_e9_7:
# -- Begin  __svml_cos1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos1_ha_e9
# --- __svml_cos1_ha_e9(__m128d)
__svml_cos1_ha_e9:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #5356.1
        .byte     15                                            #5718.18
        .byte     30                                            #5718.18
        .byte     250                                           #5718.18
	.cfi_startproc
..___tag_value___svml_cos1_ha_e9.170:
..L171:
                                                        #5356.1
        pushq     %rbp                                          #5356.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #5356.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #5356.1
        subq      $128, %rsp                                    #5356.1
        movaps    %xmm0, %xmm2                                  #5356.1
        movaps    %xmm2, %xmm10                                 #5416.25
        movaps    %xmm2, %xmm4                                  #5419.14
        mulsd     16576+__svml_dcos_ha_data_internal_ha(%rip), %xmm10 #5416.25
        lea       __svml_dcos_ha_data_internal_ha(%rip), %rax   #5430.371
        movsd     16640+__svml_dcos_ha_data_internal_ha(%rip), %xmm1 #5415.20
        xorl      %edx, %edx                                    #5408.1
        movq      16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5428.27
        addsd     %xmm1, %xmm10                                 #5416.13
        movaps    %xmm10, %xmm8                                 #5417.13
        pand      %xmm9, %xmm10                                 #5429.23
        movd      %xmm10, %ecx                                  #5430.128
        subsd     %xmm1, %xmm8                                  #5417.13
        movaps    %xmm8, %xmm6                                  #5419.34
        movaps    %xmm8, %xmm7                                  #5421.19
        mulsd     16768+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5419.34
        mulsd     16832+__svml_dcos_ha_data_internal_ha(%rip), %xmm7 #5421.19
        mulsd     16896+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #5426.32
        subsd     %xmm6, %xmm4                                  #5419.14
        movaps    %xmm4, %xmm3                                  #5422.17
        shll      $5, %ecx                                      #5430.231
        subsd     %xmm7, %xmm3                                  #5422.17
        movq      8(%rax,%rcx), %xmm14                          #5432.373
        movaps    %xmm3, %xmm11                                 #5434.22
        mulsd     %xmm14, %xmm11                                #5434.22
        subsd     %xmm3, %xmm4                                  #5423.17
        movq      16(%rax,%rcx), %xmm6                          #5431.371
        subsd     %xmm7, %xmm4                                  #5424.17
        movq      (%rcx,%rax), %xmm7                            #5430.371
        movdqa    %xmm6, %xmm15                                 #5435.22
        movdqa    %xmm7, %xmm12                                 #5433.19
        movdqa    %xmm6, %xmm13                                 #5437.18
        mulsd     %xmm3, %xmm12                                 #5433.19
        addsd     %xmm11, %xmm15                                #5435.22
        subsd     %xmm8, %xmm4                                  #5426.13
        addsd     %xmm14, %xmm7                                 #5448.13
        subsd     %xmm15, %xmm13                                #5437.18
        movaps    %xmm12, %xmm1                                 #5436.21
        addsd     %xmm11, %xmm13                                #5438.18
        addsd     %xmm15, %xmm1                                 #5436.21
        movaps    %xmm3, %xmm11                                 #5442.15
        subsd     %xmm1, %xmm15                                 #5439.18
        mulsd     %xmm3, %xmm11                                 #5442.15
        addsd     %xmm12, %xmm15                                #5440.18
        movsd     17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #5445.26
        addsd     %xmm13, %xmm15                                #5441.21
        mulsd     %xmm11, %xmm12                                #5445.26
        movsd     17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #5452.26
        mulsd     %xmm11, %xmm8                                 #5452.26
        addsd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm12 #5445.14
        mulsd     %xmm11, %xmm12                                #5446.14
        addsd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #5452.14
        mulsd     %xmm11, %xmm8                                 #5454.26
        mulsd     %xmm3, %xmm12                                 #5447.14
        mulsd     %xmm6, %xmm3                                  #5457.32
        addsd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm8 #5454.14
        mulsd     %xmm7, %xmm12                                 #5449.29
        subsd     %xmm3, %xmm7                                  #5457.13
        mulsd     %xmm11, %xmm8                                 #5455.14
        addsd     %xmm15, %xmm12                                #5449.17
        mulsd     %xmm7, %xmm4                                  #5459.28
        mulsd     %xmm6, %xmm8                                  #5456.29
        addsd     24(%rax,%rcx), %xmm4                          #5459.16
        addsd     %xmm12, %xmm8                                 #5456.17
        movsd     16384+__svml_dcos_ha_data_internal_ha(%rip), %xmm5 #5411.13
        addsd     %xmm4, %xmm8                                  #5460.17
        andps     %xmm2, %xmm5                                  #5411.13
        addsd     %xmm8, %xmm1                                  #5461.14
        movaps    %xmm5, %xmm0                                  #5412.26
        cmpnlesd  16448+__svml_dcos_ha_data_internal_ha(%rip), %xmm0 #5412.26
        movmskpd  %xmm0, %esi                                   #5462.78
        testl     $1, %esi                                      #5463.55
        jne       ..B8.8        # Prob 5%                       #5463.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm5
..B8.2:                         # Preds ..B8.8 ..B8.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #5721.52
        jne       ..B8.4        # Prob 5%                       #5721.52
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm2
..B8.3:                         # Preds ..B8.5 ..B8.2
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #5724.12
        movq      %rbp, %rsp                                    #5724.12
        popq      %rbp                                          #5724.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5724.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.4:                         # Preds ..B8.2
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm2, (%rsp)                                 #5721.160
        movsd     %xmm1, 64(%rsp)                               #5721.233
        jne       ..B8.6        # Prob 5%                       #5721.374
                                # LOE rbx r12 r13 r14 r15 edx
..B8.5:                         # Preds ..B8.7 ..B8.6 ..B8.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm1                               #5721.625
        jmp       ..B8.3        # Prob 100%                     #5721.625
                                # LOE rbx r12 r13 r14 r15 xmm1
..B8.6:                         # Preds ..B8.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B8.5        # Prob 95%                      #5721.517
                                # LOE rbx r12 r13 r14 r15
..B8.7:                         # Preds ..B8.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #5721.546
        lea       64(%rsp), %rsi                                #5721.546
..___tag_value___svml_cos1_ha_e9.179:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #5721.546
..___tag_value___svml_cos1_ha_e9.180:
        jmp       ..B8.5        # Prob 100%                     #5721.546
                                # LOE rbx r12 r13 r14 r15
..B8.8:                         # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm1, (%rsp)                                 #[spill]
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rdi #5558.383
        movsd     16512+__svml_dcos_ha_data_internal_ha(%rip), %xmm1 #5466.24
        andps     %xmm1, %xmm5                                  #5467.17
        movdqu    .L_2il0floatpacket.34(%rip), %xmm6            #5556.22
        cmpeqsd   %xmm1, %xmm5                                  #5468.25
        pand      %xmm2, %xmm6                                  #5556.22
        psrlq     $52, %xmm6                                    #5557.22
        movd      %xmm6, %ecx                                   #5558.124
        movmskpd  %xmm5, %edx                                   #5470.48
        movdqu    .L_2il0floatpacket.35(%rip), %xmm5            #5566.25
        pand      %xmm2, %xmm5                                  #5566.25
        paddq     .L_2il0floatpacket.36(%rip), %xmm5            #5567.25
        movdqa    %xmm5, %xmm4                                  #5575.16
        lea       (%rcx,%rcx,2), %esi                           #5558.230
        shll      $3, %esi                                      #5558.230
        psrlq     $32, %xmm4                                    #5575.16
        movq      16(%rdi,%rsi), %xmm13                         #5560.383
        movdqa    %xmm4, %xmm1                                  #5581.17
        movdqu    .L_2il0floatpacket.37(%rip), %xmm14           #5568.21
        movdqa    %xmm13, %xmm8                                 #5573.16
        psrlq     $32, %xmm8                                    #5573.16
        pand      %xmm14, %xmm5                                 #5576.16
        movq      8(%rdi,%rsi), %xmm11                          #5559.384
        movdqa    %xmm5, %xmm9                                  #5586.17
        pmuludq   %xmm8, %xmm1                                  #5581.17
        movdqa    %xmm11, %xmm12                                #5571.16
        pmuludq   %xmm5, %xmm8                                  #5587.17
        pand      %xmm14, %xmm11                                #5572.16
        pand      %xmm14, %xmm13                                #5574.16
        pmuludq   %xmm11, %xmm9                                 #5586.17
        psrlq     $32, %xmm8                                    #5589.17
        pmuludq   %xmm4, %xmm13                                 #5582.17
        pmuludq   %xmm4, %xmm11                                 #5580.17
        paddq     %xmm8, %xmm1                                  #5593.17
        psrlq     $32, %xmm12                                   #5571.16
        movdqa    %xmm5, %xmm10                                 #5585.17
        movdqa    %xmm14, %xmm6                                 #5597.17
        movdqa    %xmm14, %xmm15                                #5570.16
        pmuludq   %xmm12, %xmm10                                #5585.17
        pand      %xmm9, %xmm6                                  #5597.17
        pmuludq   %xmm4, %xmm12                                 #5579.17
        paddq     %xmm1, %xmm6                                  #5602.17
        movq      (%rsi,%rdi), %xmm7                            #5558.383
        psrlq     $32, %xmm13                                   #5607.17
        psrlq     $32, %xmm9                                    #5590.17
        pand      %xmm7, %xmm15                                 #5570.16
        movdqa    %xmm5, %xmm3                                  #5584.17
        movdqa    %xmm14, %xmm8                                 #5598.17
        paddq     %xmm6, %xmm13                                 #5608.17
        paddq     %xmm9, %xmm11                                 #5594.17
        pmuludq   %xmm15, %xmm3                                 #5584.17
        pmuludq   %xmm15, %xmm4                                 #5578.17
        pand      %xmm10, %xmm8                                 #5598.17
        movdqa    %xmm13, %xmm1                                 #5609.17
        paddq     %xmm11, %xmm8                                 #5603.17
        psrlq     $32, %xmm1                                    #5609.17
        psrlq     $32, %xmm10                                   #5591.17
        psrlq     $32, %xmm7                                    #5569.16
        movdqa    %xmm14, %xmm9                                 #5599.17
        paddq     %xmm8, %xmm1                                  #5610.17
        paddq     %xmm10, %xmm12                                #5595.17
        pmuludq   %xmm7, %xmm5                                  #5583.17
        pand      %xmm3, %xmm9                                  #5599.17
        movdqa    %xmm1, %xmm11                                 #5611.17
        paddq     %xmm12, %xmm9                                 #5604.17
        psrlq     $32, %xmm11                                   #5611.17
        psrlq     $32, %xmm3                                    #5592.17
        paddq     %xmm9, %xmm11                                 #5612.17
        paddq     %xmm3, %xmm4                                  #5596.17
        pand      %xmm14, %xmm5                                 #5600.17
        movdqa    %xmm11, %xmm8                                 #5613.17
        paddq     %xmm4, %xmm5                                  #5605.17
        psrlq     $32, %xmm8                                    #5613.17
        pand      %xmm14, %xmm11                                #5616.17
        paddq     %xmm5, %xmm8                                  #5614.17
        psllq     $32, %xmm8                                    #5617.23
        psllq     $32, %xmm1                                    #5618.23
        paddq     %xmm11, %xmm8                                 #5619.23
        movdqu    .L_2il0floatpacket.38(%rip), %xmm3            #5622.21
        movdqa    %xmm8, %xmm4                                  #5625.24
        movdqu    .L_2il0floatpacket.39(%rip), %xmm10           #5624.26
        pand      %xmm2, %xmm3                                  #5622.21
        movups    .L_2il0floatpacket.40(%rip), %xmm12           #5628.15
        psrlq     $12, %xmm4                                    #5625.24
        pxor      %xmm3, %xmm10                                 #5624.26
        movaps    %xmm12, %xmm7                                 #5629.19
        por       %xmm10, %xmm4                                 #5626.24
        pand      %xmm14, %xmm13                                #5615.17
        paddq     %xmm13, %xmm1                                 #5620.23
        addsd     %xmm4, %xmm7                                  #5629.19
        movaps    %xmm7, %xmm13                                 #5630.9
        andl      $1, %edx                                      #5470.102
        movdqu    .L_2il0floatpacket.45(%rip), %xmm6            #5647.24
        subsd     %xmm12, %xmm13                                #5630.9
        movdqu    .L_2il0floatpacket.43(%rip), %xmm5            #5638.24
        pand      %xmm8, %xmm6                                  #5647.24
        movdqu    .L_2il0floatpacket.42(%rip), %xmm14           #5636.26
        pand      %xmm1, %xmm5                                  #5638.24
        psllq     $40, %xmm6                                    #5648.24
        psrlq     $24, %xmm1                                    #5649.23
        pxor      %xmm3, %xmm14                                 #5636.26
        por       %xmm1, %xmm6                                  #5650.24
        pxor      .L_2il0floatpacket.44(%rip), %xmm3            #5645.26
        psllq     $28, %xmm5                                    #5639.24
        por       %xmm3, %xmm6                                  #5651.24
        por       %xmm14, %xmm5                                 #5640.24
        movups    .L_2il0floatpacket.49(%rip), %xmm1            #5663.16
        subsd     %xmm13, %xmm4                                 #5631.17
        subsd     %xmm3, %xmm6                                  #5654.18
        subsd     %xmm14, %xmm5                                 #5643.17
        movaps    %xmm4, %xmm8                                  #5655.11
        movups    .L_2il0floatpacket.47(%rip), %xmm15           #5660.16
        addsd     %xmm6, %xmm8                                  #5655.11
        andps     %xmm8, %xmm1                                  #5663.16
        subsd     %xmm8, %xmm4                                  #5656.17
        subsd     %xmm1, %xmm8                                  #5664.17
        addsd     %xmm4, %xmm6                                  #5657.18
        movups    .L_2il0floatpacket.48(%rip), %xmm4            #5661.17
        addsd     %xmm5, %xmm6                                  #5658.11
        movups    .L_2il0floatpacket.46(%rip), %xmm10           #5668.12
        movaps    %xmm4, %xmm9                                  #5667.12
        mulsd     %xmm1, %xmm9                                  #5667.12
        movaps    %xmm15, %xmm5                                 #5665.16
        mulsd     %xmm8, %xmm15                                 #5666.12
        mulsd     %xmm6, %xmm10                                 #5668.12
        mulsd     %xmm8, %xmm4                                  #5669.12
        addsd     %xmm9, %xmm15                                 #5670.13
        mulsd     %xmm1, %xmm5                                  #5665.16
        addsd     %xmm4, %xmm10                                 #5671.13
        movaps    %xmm5, %xmm12                                 #5673.13
        addsd     %xmm10, %xmm15                                #5672.13
        movups    .L_2il0floatpacket.51(%rip), %xmm11           #5677.16
        addsd     %xmm15, %xmm12                                #5673.13
        movups    .L_2il0floatpacket.50(%rip), %xmm3            #5678.11
        movaps    %xmm11, %xmm6                                 #5679.17
        andps     %xmm2, %xmm3                                  #5678.11
        subsd     %xmm12, %xmm5                                 #5674.16
        cmpltsd   %xmm3, %xmm6                                  #5679.17
        addsd     %xmm15, %xmm5                                 #5675.13
        movaps    %xmm3, %xmm4                                  #5679.17
        cmplesd   %xmm11, %xmm3                                 #5680.20
        andps     .L_2il0floatpacket.41(%rip), %xmm7            #5634.19
        movsd     %xmm6, %xmm4                                  #5679.17
        movd      %xmm7, %r8d                                   #5686.132
        andps     %xmm2, %xmm3                                  #5681.24
        andps     %xmm4, %xmm12                                 #5682.25
        andps     %xmm5, %xmm4                                  #5684.9
        orps      %xmm12, %xmm3                                 #5683.9
        shll      $5, %r8d                                      #5686.235
        movaps    %xmm3, %xmm7                                  #5690.26
        mulsd     8(%rax,%r8), %xmm7                            #5690.26
        movq      16(%rax,%r8), %xmm14                          #5687.375
        movdqa    %xmm14, %xmm5                                 #5691.26
        movdqa    %xmm14, %xmm1                                 #5693.22
        movq      (%r8,%rax), %xmm8                             #5686.375
        addsd     %xmm7, %xmm5                                  #5691.26
        movdqa    %xmm8, %xmm13                                 #5689.23
        subsd     %xmm5, %xmm1                                  #5693.22
        mulsd     %xmm3, %xmm13                                 #5689.23
        addsd     8(%rax,%r8), %xmm8                            #5704.17
        addsd     %xmm7, %xmm1                                  #5694.22
        movaps    %xmm3, %xmm7                                  #5698.19
        movaps    %xmm13, %xmm10                                #5692.25
        mulsd     %xmm3, %xmm7                                  #5698.19
        addsd     %xmm5, %xmm10                                 #5692.25
        movsd     17024+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5701.30
        subsd     %xmm10, %xmm5                                 #5695.22
        mulsd     %xmm7, %xmm6                                  #5701.30
        addsd     %xmm13, %xmm5                                 #5696.22
        addsd     16960+__svml_dcos_ha_data_internal_ha(%rip), %xmm6 #5701.18
        addsd     %xmm1, %xmm5                                  #5697.25
        mulsd     %xmm7, %xmm6                                  #5702.18
        movsd     17216+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5708.30
        mulsd     %xmm7, %xmm9                                  #5708.30
        mulsd     %xmm3, %xmm6                                  #5703.18
        mulsd     %xmm14, %xmm3                                 #5713.36
        addsd     17152+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5708.18
        mulsd     %xmm8, %xmm6                                  #5705.33
        subsd     %xmm3, %xmm8                                  #5713.17
        mulsd     %xmm7, %xmm9                                  #5710.30
        addsd     %xmm5, %xmm6                                  #5705.21
        mulsd     %xmm8, %xmm4                                  #5715.32
        addsd     17088+__svml_dcos_ha_data_internal_ha(%rip), %xmm9 #5710.18
        addsd     24(%rax,%r8), %xmm4                           #5715.20
        mulsd     %xmm7, %xmm9                                  #5711.18
        mulsd     %xmm14, %xmm9                                 #5712.33
        movups    (%rsp), %xmm1                                 #5718.18[spill]
        addsd     %xmm6, %xmm9                                  #5712.21
        addsd     %xmm4, %xmm9                                  #5716.21
        addsd     %xmm9, %xmm10                                 #5717.24
        blendvpd  %xmm0, %xmm10, %xmm1                          #5718.18
        jmp       ..B8.2        # Prob 100%                     #5718.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm1 xmm2
	.cfi_endproc
# mark_end;
	.type	__svml_cos1_ha_e9,@function
	.size	__svml_cos1_ha_e9,.-__svml_cos1_ha_e9
..LN__svml_cos1_ha_e9.7:
	.data
# -- End  __svml_cos1_ha_e9
	.text
.L_2__routine_start___svml_cos4_ha_e9_8:
# -- Begin  __svml_cos4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_cos4_ha_e9
# --- __svml_cos4_ha_e9(__m256d)
__svml_cos4_ha_e9:
# parameter 1: %ymm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #5729.1
        .byte     15                                            #6091.18
        .byte     30                                            #6091.18
        .byte     250                                           #6091.18
	.cfi_startproc
..___tag_value___svml_cos4_ha_e9.184:
..L185:
                                                        #5729.1
        pushq     %rbp                                          #5729.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #5729.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #5729.1
        subq      $320, %rsp                                    #5729.1
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r8    #5803.613
        vmovapd   %ymm0, %ymm6                                  #5729.1
        xorl      %esi, %esi                                    #5781.1
        vmovupd   16640+__svml_dcos_ha_data_internal_ha(%rip), %ymm7 #5788.54
        vmulpd    16576+__svml_dcos_ha_data_internal_ha(%rip), %ymm6, %ymm5 #5789.28
        vaddpd    %ymm5, %ymm7, %ymm13                          #5789.13
        vmovupd   16704+__svml_dcos_ha_data_internal_ha(%rip), %xmm5 #5801.27
        vsubpd    %ymm7, %ymm13, %ymm11                         #5790.13
        vmulpd    16768+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm3 #5792.37
        vmulpd    16832+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm8 #5794.19
        vmulpd    16896+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm14 #5799.35
        vsubpd    %ymm3, %ymm6, %ymm9                           #5792.14
        vsubpd    %ymm8, %ymm9, %ymm0                           #5795.17
        vsubpd    %ymm0, %ymm9, %ymm4                           #5796.17
        vsubpd    %ymm8, %ymm4, %ymm10                          #5797.17
        vsubpd    %ymm14, %ymm10, %ymm3                         #5799.13
        vandpd    16384+__svml_dcos_ha_data_internal_ha(%rip), %ymm6, %ymm1 #5784.13
        vcmpnle_uqpd 16448+__svml_dcos_ha_data_internal_ha(%rip), %ymm1, %ymm15 #5785.26
        vandps    %xmm5, %xmm13, %xmm7                          #5802.23
        vextractf128 $1, %ymm13, %xmm12                         #5800.104
        vmovd     %xmm7, %eax                                   #5803.128
        vandps    %xmm5, %xmm12, %xmm8                          #5802.92
        vmovd     %xmm8, %ecx                                   #5803.255
        shll      $5, %eax                                      #5803.424
        vpextrd   $2, %xmm7, %edx                               #5803.190
        shll      $5, %ecx                                      #5803.526
        vpextrd   $2, %xmm8, %edi                               #5803.316
        shll      $5, %edx                                      #5803.475
        vmovq     (%rax,%r8), %xmm9                             #5803.1112
        shll      $5, %edi                                      #5803.577
        vmovq     (%rcx,%r8), %xmm11                            #5803.1312
        vmovhpd   (%rdx,%r8), %xmm9, %xmm4                      #5803.1080
        vmovhpd   (%rdi,%r8), %xmm11, %xmm10                    #5803.1280
        vmovq     8(%rax,%r8), %xmm9                            #5805.1114
        vmovq     8(%rcx,%r8), %xmm11                           #5805.1314
        vmovq     16(%rax,%r8), %xmm13                          #5804.1112
        vmovhpd   8(%rdx,%r8), %xmm9, %xmm8                     #5805.1082
        vmovq     16(%rcx,%r8), %xmm5                           #5804.1312
        vmovhpd   16(%rdx,%r8), %xmm13, %xmm12                  #5804.1080
        vmovhpd   16(%rdi,%r8), %xmm5, %xmm7                    #5804.1280
        vinsertf128 $1, %xmm10, %ymm4, %ymm14                   #5803.1034
        vmovhpd   8(%rdi,%r8), %xmm11, %xmm10                   #5805.1282
        vinsertf128 $1, %xmm10, %ymm8, %ymm13                   #5805.1036
        vinsertf128 $1, %xmm7, %ymm12, %ymm4                    #5804.1034
        vmulpd    %ymm0, %ymm13, %ymm7                          #5807.22
        vmulpd    %ymm0, %ymm14, %ymm12                         #5806.19
        vaddpd    %ymm7, %ymm4, %ymm8                           #5808.22
        vaddpd    %ymm8, %ymm12, %ymm5                          #5809.21
        vsubpd    %ymm8, %ymm4, %ymm9                           #5810.18
        vsubpd    %ymm5, %ymm8, %ymm11                          #5812.18
        vaddpd    %ymm9, %ymm7, %ymm10                          #5811.18
        vaddpd    %ymm11, %ymm12, %ymm12                        #5813.18
        vmulpd    %ymm0, %ymm0, %ymm11                          #5815.15
        vaddpd    %ymm12, %ymm10, %ymm10                        #5814.21
        vmulpd    17024+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm7 #5818.29
        vaddpd    16960+__svml_dcos_ha_data_internal_ha(%rip), %ymm7, %ymm9 #5818.14
        vmulpd    17216+__svml_dcos_ha_data_internal_ha(%rip), %ymm11, %ymm7 #5825.29
        vmulpd    %ymm9, %ymm11, %ymm8                          #5819.14
        vaddpd    %ymm13, %ymm14, %ymm9                         #5821.13
        vmulpd    %ymm8, %ymm0, %ymm12                          #5820.14
        vmulpd    %ymm0, %ymm4, %ymm0                           #5830.35
        vmulpd    %ymm9, %ymm12, %ymm14                         #5822.32
        vaddpd    %ymm14, %ymm10, %ymm8                         #5822.17
        vaddpd    17152+__svml_dcos_ha_data_internal_ha(%rip), %ymm7, %ymm10 #5825.14
        vmulpd    %ymm10, %ymm11, %ymm12                        #5827.29
        vsubpd    %ymm0, %ymm9, %ymm10                          #5830.13
        vmovq     24(%rax,%r8), %xmm0                           #5831.1112
        vaddpd    17088+__svml_dcos_ha_data_internal_ha(%rip), %ymm12, %ymm13 #5827.14
        vmulpd    %ymm10, %ymm3, %ymm3                          #5832.31
        vmulpd    %ymm13, %ymm11, %ymm11                        #5828.14
        vmulpd    %ymm11, %ymm4, %ymm14                         #5829.32
        vmovhpd   24(%rdx,%r8), %xmm0, %xmm4                    #5831.1080
        vaddpd    %ymm14, %ymm8, %ymm7                          #5829.17
        vmovq     24(%rcx,%r8), %xmm8                           #5831.1312
        vmovhpd   24(%rdi,%r8), %xmm8, %xmm9                    #5831.1280
        vextractf128 $1, %ymm15, %xmm2                          #5786.151
        vshufps   $221, %xmm2, %xmm15, %xmm2                    #5835.95
        vmovmskps %xmm2, %r9d                                   #5835.78
        vinsertf128 $1, %xmm9, %ymm4, %ymm11                    #5831.1034
        vaddpd    %ymm3, %ymm11, %ymm0                          #5832.16
        vaddpd    %ymm0, %ymm7, %ymm3                           #5833.17
        vaddpd    %ymm3, %ymm5, %ymm0                           #5834.14
        testl     %r9d, %r9d                                    #5836.66
        jne       ..B9.12       # Prob 5%                       #5836.66
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm1 ymm6 ymm15
..B9.2:                         # Preds ..B9.12 ..B9.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #6094.52
        jne       ..B9.4        # Prob 5%                       #6094.52
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm6
..B9.3:                         # Preds ..B9.4 ..B9.10 ..B9.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #6097.12
        popq      %rbp                                          #6097.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #6097.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.4:                         # Preds ..B9.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #6094.200
        vmovupd   %ymm0, 128(%rsp)                              #6094.276
        je        ..B9.3        # Prob 95%                      #6094.380
                                # LOE rbx r12 r13 r14 r15 esi ymm0
..B9.7:                         # Preds ..B9.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #6094.460
                                # LOE rbx r12 r13 r14 r15 eax esi
..B9.15:                        # Preds ..B9.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc8, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
        movl      %esi, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B9.8:                         # Preds ..B9.9 ..B9.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #6094.523
        jc        ..B9.11       # Prob 5%                       #6094.523
                                # LOE rbx r12 r14 r15 r13d
..B9.9:                         # Preds ..B9.11 ..B9.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #6094.476
        cmpl      $4, %r12d                                     #6094.471
        jl        ..B9.8        # Prob 82%                      #6094.471
                                # LOE rbx r12 r14 r15 r13d
..B9.10:                        # Preds ..B9.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #6094.682
        jmp       ..B9.3        # Prob 100%                     #6094.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc8, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xc0, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B9.11:                        # Preds ..B9.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #6094.552
        lea       128(%rsp,%r12,8), %rsi                        #6094.552
..___tag_value___svml_cos4_ha_e9.202:
#       __svml_dcos_ha_cout_rare_internal(const double *, double *)
        call      __svml_dcos_ha_cout_rare_internal             #6094.552
..___tag_value___svml_cos4_ha_e9.203:
        jmp       ..B9.9        # Prob 100%                     #6094.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B9.12:                        # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   16512+__svml_dcos_ha_data_internal_ha(%rip), %ymm4 #5839.58
        lea       __svml_dcos_ha_reduction_data_internal(%rip), %rcx #5931.621
        vmovupd   %ymm15, 32(%rsp)                              #[spill]
        vmovupd   %ymm6, (%rsp)                                 #[spill]
        vmovdqu   .L_2il0floatpacket.34(%rip), %xmm7            #5928.22
        vmovupd   %ymm0, 64(%rsp)                               #[spill]
        vandpd    %ymm1, %ymm4, %ymm11                          #5840.17
        vcmpeqpd  %ymm4, %ymm11, %ymm9                          #5841.25
        vextractf128 $1, %ymm9, %xmm15                          #5842.132
        vshufps   $221, %xmm15, %xmm9, %xmm13                   #5843.65
        vmovmskps %xmm13, %esi                                  #5843.48
        vmovdqu   .L_2il0floatpacket.35(%rip), %xmm15           #5937.25
        vpand     %xmm6, %xmm7, %xmm1                           #5929.22
        vextractf128 $1, %ymm6, %xmm13                          #5927.109
        vpsrlq    $52, %xmm1, %xmm2                             #5930.22
        vmovd     %xmm2, %edi                                   #5931.124
        vmovups   %xmm6, 96(%rsp)                               #5927.37[spill]
        vpand     %xmm6, %xmm15, %xmm6                          #5939.25
        vpand     %xmm13, %xmm7, %xmm8                          #5929.97
        vpsrlq    $52, %xmm8, %xmm14                            #5930.85
        vmovd     %xmm14, %r9d                                  #5931.257
        lea       (%rdi,%rdi,2), %edx                           #5931.432
        vmovdqu   .L_2il0floatpacket.36(%rip), %xmm7            #5938.24
        vmovups   %xmm13, 112(%rsp)                             #5927.109[spill]
        vpextrd   $2, %xmm14, %r11d                             #5931.321
        vpextrd   $2, %xmm2, %r8d                               #5931.189
        lea       (%r9,%r9,2), %r10d                            #5931.534
        shll      $3, %r10d                                     #5931.534
        lea       __svml_dcos_ha_data_internal_ha(%rip), %r9    #6059.617
        vmovq     (%r10,%rcx), %xmm11                           #5931.1369
        shll      $3, %edx                                      #5931.432
        lea       (%r11,%r11,2), %edi                           #5931.585
        shll      $3, %edi                                      #5931.585
        lea       (%r8,%r8,2), %eax                             #5931.483
        vmovhpd   (%rdi,%rcx), %xmm11, %xmm2                    #5931.1337
        vpaddq    %xmm7, %xmm6, %xmm11                          #5940.25
        vpand     %xmm13, %xmm15, %xmm6                         #5939.106
        shll      $3, %eax                                      #5931.483
        vmovq     8(%rdx,%rcx), %xmm3                           #5932.1170
        vpaddq    %xmm7, %xmm6, %xmm7                           #5940.111
        vmovq     16(%rdx,%rcx), %xmm12                         #5933.1169
        vmovhpd   8(%rax,%rcx), %xmm3, %xmm9                    #5932.1138
        vmovdqu   .L_2il0floatpacket.37(%rip), %xmm13           #5941.21
        vmovq     8(%r10,%rcx), %xmm0                           #5932.1370
        vpand     %xmm13, %xmm2, %xmm14                         #5943.83
        vmovhpd   16(%rax,%rcx), %xmm12, %xmm1                  #5933.1137
        vpsrlq    $32, %xmm9, %xmm12                            #5944.16
        vpand     %xmm13, %xmm9, %xmm6                          #5945.16
        vpsrlq    $32, %xmm11, %xmm9                            #5948.16
        vpand     %xmm13, %xmm11, %xmm8                         #5949.16
        vpand     %xmm13, %xmm7, %xmm11                         #5949.90
        vmovq     (%rdx,%rcx), %xmm10                           #5931.1169
        vmovhpd   8(%rdi,%rcx), %xmm0, %xmm4                    #5932.1338
        vmovdqu   %xmm14, 176(%rsp)                             #5943.83[spill]
        vpand     %xmm13, %xmm4, %xmm0                          #5945.84
        vpmuludq  %xmm14, %xmm11, %xmm14                        #5957.78
        vmovhpd   (%rax,%rcx), %xmm10, %xmm10                   #5931.1137
        vmovupd   %xmm2, 144(%rsp)                              #5931.1337[spill]
        vpsrlq    $32, %xmm1, %xmm2                             #5946.16
        vpand     %xmm13, %xmm1, %xmm1                          #5947.16
        vpand     %xmm13, %xmm10, %xmm15                        #5943.16
        vmovq     16(%r10,%rcx), %xmm5                          #5933.1369
        vmovupd   %xmm10, 128(%rsp)                             #5931.1137[spill]
        vpsrlq    $32, %xmm7, %xmm10                            #5948.76
        vmovdqu   %xmm0, 224(%rsp)                              #5945.84[spill]
        vpmuludq  %xmm0, %xmm11, %xmm7                          #5959.78
        vpmuludq  %xmm1, %xmm9, %xmm0                           #5955.17
        vpmuludq  %xmm2, %xmm9, %xmm1                           #5954.17
        vpmuludq  %xmm2, %xmm8, %xmm2                           #5960.17
        vmovhpd   16(%rdi,%rcx), %xmm5, %xmm3                   #5933.1337
        vpsrlq    $32, %xmm4, %xmm5                             #5944.70
        vmovdqu   %xmm5, 208(%rsp)                              #5944.70[spill]
        vpsrlq    $32, %xmm2, %xmm2                             #5962.17
        vmovdqu   %xmm14, 256(%rsp)                             #5957.78[spill]
        vpsrlq    $32, %xmm3, %xmm4                             #5946.69
        vpmuludq  %xmm5, %xmm11, %xmm14                         #5958.78
        vpand     %xmm13, %xmm3, %xmm3                          #5947.83
        vpmuludq  %xmm6, %xmm8, %xmm5                           #5959.17
        vpmuludq  %xmm6, %xmm9, %xmm6                           #5953.17
        vpaddq    %xmm2, %xmm1, %xmm1                           #5966.17
        vmovdqu   %xmm15, 160(%rsp)                             #5943.16[spill]
        vpmuludq  %xmm15, %xmm8, %xmm15                         #5957.17
        vmovdqu   %xmm15, 240(%rsp)                             #5957.17[spill]
        vpsrlq    $32, %xmm0, %xmm15                            #5980.17
        vpand     %xmm13, %xmm5, %xmm0                          #5970.17
        vpsrlq    $32, %xmm5, %xmm5                             #5963.17
        vpaddq    %xmm1, %xmm0, %xmm0                           #5975.17
        vpaddq    %xmm5, %xmm6, %xmm6                           #5967.17
        vpaddq    %xmm0, %xmm15, %xmm2                          #5981.17
        vpmuludq  %xmm3, %xmm10, %xmm15                         #5955.78
        vpmuludq  %xmm4, %xmm10, %xmm3                          #5954.78
        vpmuludq  %xmm4, %xmm11, %xmm4                          #5960.78
        vpsrlq    $32, %xmm4, %xmm4                             #5962.70
        vpand     %xmm13, %xmm7, %xmm0                          #5970.84
        vmovdqu   %xmm12, 192(%rsp)                             #5944.16[spill]
        vpsrlq    $32, %xmm15, %xmm15                           #5980.70
        vpmuludq  %xmm12, %xmm8, %xmm12                         #5958.17
        vpsrlq    $32, %xmm7, %xmm7                             #5963.70
        vpaddq    %xmm4, %xmm3, %xmm1                           #5966.80
        vpaddq    %xmm1, %xmm0, %xmm3                           #5975.80
        vpaddq    %xmm3, %xmm15, %xmm4                          #5981.80
        vpand     %xmm13, %xmm12, %xmm15                        #5971.17
        vpsrlq    $32, %xmm2, %xmm1                             #5982.17
        vpaddq    %xmm6, %xmm15, %xmm15                         #5976.17
        vpaddq    %xmm15, %xmm1, %xmm15                         #5983.17
        vpmuludq  224(%rsp), %xmm10, %xmm1                      #5953.78[spill]
        vpaddq    %xmm7, %xmm1, %xmm5                           #5967.80
        vpmuludq  192(%rsp), %xmm9, %xmm1                       #5952.17[spill]
        vpmuludq  160(%rsp), %xmm9, %xmm9                       #5951.17[spill]
        vpand     %xmm13, %xmm14, %xmm0                         #5971.84
        vpsrlq    $32, %xmm12, %xmm12                           #5964.17
        vpaddq    %xmm5, %xmm0, %xmm6                           #5976.80
        vpaddq    %xmm12, %xmm1, %xmm1                          #5968.17
        vpmuludq  208(%rsp), %xmm10, %xmm12                     #5952.78[spill]
        vpmuludq  176(%rsp), %xmm10, %xmm10                     #5951.78[spill]
        vpsrlq    $32, %xmm4, %xmm3                             #5982.70
        vpsrlq    $32, %xmm15, %xmm0                            #5984.17
        vpaddq    %xmm6, %xmm3, %xmm7                           #5983.80
        vmovdqu   240(%rsp), %xmm3                              #5972.17[spill]
        vpsrlq    $32, %xmm14, %xmm14                           #5964.70
        vpand     %xmm13, %xmm3, %xmm6                          #5972.17
        vpsrlq    $32, %xmm3, %xmm3                             #5965.17
        vpaddq    %xmm1, %xmm6, %xmm6                           #5977.17
        vpaddq    %xmm14, %xmm12, %xmm12                        #5968.80
        vpaddq    %xmm6, %xmm0, %xmm6                           #5985.17
        vpaddq    %xmm3, %xmm9, %xmm9                           #5969.17
        vmovdqu   256(%rsp), %xmm0                              #5972.84[spill]
        vpsrlq    $32, %xmm7, %xmm1                             #5984.70
        vpand     %xmm13, %xmm0, %xmm5                          #5972.84
        vpsrlq    $32, %xmm6, %xmm14                            #5986.17
        vpaddq    %xmm12, %xmm5, %xmm5                          #5977.80
        vmovupd   128(%rsp), %xmm12                             #5942.16[spill]
        vpand     %xmm13, %xmm6, %xmm6                          #5989.17
        vpaddq    %xmm5, %xmm1, %xmm1                           #5985.80
        vpsrlq    $32, %xmm12, %xmm5                            #5942.16
        vpand     %xmm13, %xmm2, %xmm2                          #5988.17
        vpmuludq  %xmm5, %xmm8, %xmm8                           #5956.17
        vpsllq    $32, %xmm15, %xmm15                           #5991.23
        vpand     %xmm13, %xmm8, %xmm8                          #5973.17
        vpsllq    $32, %xmm7, %xmm7                             #5991.82
        vpaddq    %xmm9, %xmm8, %xmm8                           #5978.17
        vmovupd   144(%rsp), %xmm5                              #5942.69[spill]
        vpsrlq    $32, %xmm1, %xmm9                             #5986.70
        vpaddq    %xmm8, %xmm14, %xmm14                         #5987.17
        vpsrlq    $32, %xmm5, %xmm8                             #5942.69
        vpsllq    $32, %xmm14, %xmm12                           #5990.23
        vpmuludq  %xmm8, %xmm11, %xmm11                         #5956.78
        vpsrlq    $32, %xmm0, %xmm8                             #5965.70
        vpaddq    %xmm8, %xmm10, %xmm10                         #5969.80
        vpaddq    %xmm6, %xmm12, %xmm6                          #5992.23
        vmovupd   .L_2il0floatpacket.76(%rip), %ymm12           #6001.15
        vpand     %xmm13, %xmm11, %xmm14                        #5973.84
        vpand     %xmm13, %xmm1, %xmm1                          #5989.84
        vpaddq    %xmm10, %xmm14, %xmm11                        #5978.80
        vpaddq    %xmm2, %xmm15, %xmm10                         #5993.23
        vpaddq    %xmm11, %xmm9, %xmm9                          #5987.80
        vpsllq    $32, %xmm9, %xmm3                             #5990.82
        vpand     %xmm13, %xmm4, %xmm13                         #5988.84
        vmovdqu   .L_2il0floatpacket.38(%rip), %xmm4            #5994.21
        vpsrlq    $12, %xmm6, %xmm9                             #5998.24
        vpaddq    %xmm1, %xmm3, %xmm8                           #5992.98
        vpaddq    %xmm13, %xmm7, %xmm7                          #5993.98
        vpand     96(%rsp), %xmm4, %xmm1                        #5995.21[spill]
        vpsrlq    $12, %xmm8, %xmm11                            #5998.90
        vmovdqu   .L_2il0floatpacket.39(%rip), %xmm14           #5996.26
        vpand     112(%rsp), %xmm4, %xmm15                      #5995.94[spill]
        vpxor     %xmm14, %xmm1, %xmm13                         #5997.26
        vpxor     %xmm14, %xmm15, %xmm3                         #5997.111
        vpor      %xmm13, %xmm9, %xmm2                          #5999.24
        vpor      %xmm3, %xmm11, %xmm0                          #5999.109
        vmovups   .L_2il0floatpacket.41(%rip), %xmm14           #6006.19
        vmovdqu   .L_2il0floatpacket.42(%rip), %xmm11           #6008.26
        vinsertf128 $1, %xmm0, %ymm2, %ymm5                     #6000.17
        vaddpd    %ymm12, %ymm5, %ymm9                          #6002.19
        vsubpd    %ymm12, %ymm9, %ymm4                          #6003.9
        vpxor     %xmm11, %xmm15, %xmm12                        #6009.111
        vsubpd    %ymm4, %ymm5, %ymm4                           #6004.17
        vpxor     %xmm11, %xmm1, %xmm5                          #6009.26
        vandps    %xmm14, %xmm9, %xmm13                         #6007.19
        vextractf128 $1, %ymm9, %xmm2                           #6005.118
        vmovd     %xmm13, %eax                                  #6059.132
        vandps    %xmm14, %xmm2, %xmm9                          #6007.88
        vmovdqu   .L_2il0floatpacket.43(%rip), %xmm14           #6010.24
        vpand     %xmm10, %xmm14, %xmm3                         #6011.24
        vpand     %xmm7, %xmm14, %xmm11                         #6011.107
        vpsllq    $28, %xmm3, %xmm0                             #6012.24
        vpsllq    $28, %xmm11, %xmm3                            #6012.91
        vpor      %xmm5, %xmm0, %xmm2                           #6013.24
        vpor      %xmm12, %xmm3, %xmm0                          #6013.109
        vpsrlq    $24, %xmm7, %xmm7                             #6022.88
        vmovd     %xmm9, %ecx                                   #6059.259
        shll      $5, %eax                                      #6059.428
        vpextrd   $2, %xmm13, %edx                              #6059.194
        shll      $5, %ecx                                      #6059.530
        vpextrd   $2, %xmm9, %r8d                               #6059.320
        shll      $5, %edx                                      #6059.479
        shll      $5, %r8d                                      #6059.581
        vinsertf128 $1, %xmm0, %ymm2, %ymm2                     #6014.17
        vinsertf128 $1, %xmm12, %ymm5, %ymm12                   #6015.15
        vmovdqu   .L_2il0floatpacket.45(%rip), %xmm0            #6019.24
        vsubpd    %ymm12, %ymm2, %ymm14                         #6016.17
        vpand     %xmm6, %xmm0, %xmm6                           #6020.24
        vmovdqu   .L_2il0floatpacket.44(%rip), %xmm2            #6017.26
        vpand     %xmm8, %xmm0, %xmm8                           #6020.107
        vpxor     %xmm2, %xmm15, %xmm11                         #6018.111
        vpsllq    $40, %xmm6, %xmm15                            #6021.24
        vpsrlq    $24, %xmm10, %xmm6                            #6022.23
        vpsllq    $40, %xmm8, %xmm10                            #6021.91
        vpxor     %xmm2, %xmm1, %xmm3                           #6018.26
        vpor      %xmm6, %xmm15, %xmm1                          #6023.24
        vpor      %xmm7, %xmm10, %xmm0                          #6023.106
        vpor      %xmm3, %xmm1, %xmm2                           #6024.24
        vpor      %xmm11, %xmm0, %xmm5                          #6024.109
        vmovupd   .L_2il0floatpacket.87(%rip), %ymm1            #6034.17
        vinsertf128 $1, %xmm5, %ymm2, %ymm12                    #6025.18
        vinsertf128 $1, %xmm11, %ymm3, %ymm11                   #6026.15
        vsubpd    %ymm11, %ymm12, %ymm6                         #6027.18
        vaddpd    %ymm6, %ymm4, %ymm15                          #6028.11
        vsubpd    %ymm15, %ymm4, %ymm4                          #6029.17
        vaddpd    %ymm4, %ymm6, %ymm6                           #6030.18
        vaddpd    %ymm6, %ymm14, %ymm4                          #6031.11
        vmovupd   .L_2il0floatpacket.86(%rip), %ymm14           #6033.16
        vmulpd    .L_2il0floatpacket.82(%rip), %ymm4, %ymm2     #6041.12
        vandpd    .L_2il0floatpacket.88(%rip), %ymm15, %ymm6    #6036.16
        vsubpd    %ymm6, %ymm15, %ymm15                         #6037.17
        vmulpd    %ymm6, %ymm1, %ymm8                           #6040.12
        vmulpd    %ymm6, %ymm14, %ymm3                          #6038.16
        vmulpd    %ymm15, %ymm14, %ymm7                         #6039.12
        vmulpd    %ymm15, %ymm1, %ymm10                         #6042.12
        vmovupd   (%rsp), %ymm6                                 #6051.11[spill]
        vmovupd   .L_2il0floatpacket.90(%rip), %ymm15           #6050.16
        vaddpd    %ymm8, %ymm7, %ymm14                          #6043.13
        vaddpd    %ymm10, %ymm2, %ymm0                          #6044.13
        vaddpd    %ymm0, %ymm14, %ymm5                          #6045.13
        vmovq     8(%rcx,%r9), %xmm0                            #6061.1318
        vaddpd    %ymm5, %ymm3, %ymm7                           #6046.13
        vandpd    .L_2il0floatpacket.89(%rip), %ymm6, %ymm4     #6051.11
        vsubpd    %ymm7, %ymm3, %ymm11                          #6047.16
        vcmpgt_oqpd %ymm15, %ymm4, %ymm10                       #6052.17
        vcmple_oqpd %ymm15, %ymm4, %ymm1                        #6053.20
        vaddpd    %ymm11, %ymm5, %ymm12                         #6048.13
        vmovq     (%rax,%r9), %xmm4                             #6059.1116
        vmovhpd   8(%r8,%r9), %xmm0, %xmm3                      #6061.1286
        vandpd    %ymm6, %ymm1, %ymm8                           #6054.24
        vandpd    %ymm7, %ymm10, %ymm2                          #6055.25
        vorpd     %ymm2, %ymm8, %ymm1                           #6056.9
        vmovq     8(%rax,%r9), %xmm8                            #6061.1118
        vandpd    %ymm12, %ymm10, %ymm15                        #6057.9
        vmovhpd   8(%rdx,%r9), %xmm8, %xmm10                    #6061.1086
        vmovhpd   (%rdx,%r9), %xmm4, %xmm13                     #6059.1084
        vmovq     (%rcx,%r9), %xmm4                             #6059.1316
        vmovhpd   (%r8,%r9), %xmm4, %xmm9                       #6059.1284
        vmovq     16(%rax,%r9), %xmm4                           #6060.1116
        vinsertf128 $1, %xmm3, %ymm10, %ymm10                   #6061.1040
        vmulpd    %ymm10, %ymm1, %ymm5                          #6063.26
        vinsertf128 $1, %xmm9, %ymm13, %ymm2                    #6059.1038
        vmovq     16(%rcx,%r9), %xmm13                          #6060.1316
        vmovhpd   16(%rdx,%r9), %xmm4, %xmm9                    #6060.1084
        vmovhpd   16(%r8,%r9), %xmm13, %xmm7                    #6060.1284
        vmulpd    %ymm2, %ymm1, %ymm14                          #6062.23
        vinsertf128 $1, %xmm7, %ymm9, %ymm7                     #6060.1038
        vaddpd    %ymm5, %ymm7, %ymm12                          #6064.26
        vaddpd    %ymm12, %ymm14, %ymm4                         #6065.25
        vsubpd    %ymm12, %ymm7, %ymm11                         #6066.22
        vsubpd    %ymm4, %ymm12, %ymm9                          #6068.22
        vaddpd    %ymm11, %ymm5, %ymm13                         #6067.22
        vaddpd    %ymm9, %ymm14, %ymm8                          #6069.22
        vaddpd    %ymm8, %ymm13, %ymm12                         #6070.25
        vaddpd    %ymm10, %ymm2, %ymm13                         #6077.17
        vmulpd    %ymm1, %ymm1, %ymm8                           #6071.19
        vmulpd    17024+__svml_dcos_ha_data_internal_ha(%rip), %ymm8, %ymm0 #6074.33
        vmulpd    17216+__svml_dcos_ha_data_internal_ha(%rip), %ymm8, %ymm9 #6081.33
        vaddpd    16960+__svml_dcos_ha_data_internal_ha(%rip), %ymm0, %ymm3 #6074.18
        vaddpd    17152+__svml_dcos_ha_data_internal_ha(%rip), %ymm9, %ymm0 #6081.18
        vmulpd    %ymm3, %ymm8, %ymm5                           #6075.18
        vmulpd    %ymm5, %ymm1, %ymm11                          #6076.18
        vmulpd    %ymm7, %ymm1, %ymm1                           #6086.39
        vmulpd    %ymm13, %ymm11, %ymm2                         #6078.36
        vaddpd    %ymm2, %ymm12, %ymm5                          #6078.21
        vmulpd    %ymm0, %ymm8, %ymm2                           #6083.33
        vmovq     24(%rax,%r9), %xmm0                           #6087.1116
        vaddpd    17088+__svml_dcos_ha_data_internal_ha(%rip), %ymm2, %ymm3 #6083.18
        vmovq     24(%rcx,%r9), %xmm2                           #6087.1316
        vmulpd    %ymm3, %ymm8, %ymm8                           #6084.18
        vmovhpd   24(%r8,%r9), %xmm2, %xmm3                     #6087.1284
        vmulpd    %ymm8, %ymm7, %ymm10                          #6085.36
        vaddpd    %ymm10, %ymm5, %ymm9                          #6085.21
        vsubpd    %ymm1, %ymm13, %ymm5                          #6086.17
        vmovhpd   24(%rdx,%r9), %xmm0, %xmm1                    #6087.1084
        vmulpd    %ymm5, %ymm15, %ymm15                         #6088.35
        vinsertf128 $1, %xmm3, %ymm1, %ymm7                     #6087.1038
        vaddpd    %ymm15, %ymm7, %ymm0                          #6088.20
        vmovupd   32(%rsp), %ymm3                               #6091.18[spill]
        vaddpd    %ymm0, %ymm9, %ymm1                           #6089.21
        vmovupd   64(%rsp), %ymm0                               #6091.18[spill]
        vaddpd    %ymm1, %ymm4, %ymm2                           #6090.24
        vblendvpd %ymm3, %ymm2, %ymm0, %ymm0                    #6091.18
        jmp       ..B9.2        # Prob 100%                     #6091.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm6
	.cfi_endproc
# mark_end;
	.type	__svml_cos4_ha_e9,@function
	.size	__svml_cos4_ha_e9,.-__svml_cos4_ha_e9
..LN__svml_cos4_ha_e9.8:
	.data
# -- End  __svml_cos4_ha_e9
	.text
.L_2__routine_start___svml_dcos_ha_cout_rare_internal_9:
# -- Begin  __svml_dcos_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dcos_ha_cout_rare_internal
	.globl __svml_dcos_ha_cout_rare_internal
# --- __svml_dcos_ha_cout_rare_internal(const double *, double *)
__svml_dcos_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2739.1
        .byte     15                                            #2758.12
        .byte     30                                            #2758.12
        .byte     250                                           #2758.12
	.cfi_startproc
..___tag_value___svml_dcos_ha_cout_rare_internal.235:
..L236:
                                                        #2739.1
        movzwl    6(%rdi), %eax                                 #2744.36
        andl      $32752, %eax                                  #2744.36
        movsd     (%rdi), %xmm1                                 #2742.18
        movb      7(%rdi), %dl                                  #2742.18
        andb      $127, %dl                                     #2743.30
        movsd     %xmm1, -8(%rsp)                               #2742.9
        cmpl      $32752, %eax                                  #2744.62
        jne       ..B10.6       # Prob 67%                      #2744.62
                                # LOE rbx rbp rsi r12 r13 r14 r15 dl xmm1
..B10.2:                        # Preds ..B10.1
                                # Execution count [3.29e-01]
        cmpl      $0, -8(%rsp)                                  #2746.92
        jne       ..B10.5       # Prob 28%                      #2746.92
                                # LOE rbx rbp rsi r12 r13 r14 r15 dl xmm1
..B10.3:                        # Preds ..B10.2
                                # Execution count [2.37e-01]
        movb      %dl, -1(%rsp)                                 #2743.30
        cmpl      $2146435072, -4(%rsp)                         #2746.200
        jne       ..B10.5       # Prob 50%                      #2746.200
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm1
..B10.4:                        # Preds ..B10.3
                                # Execution count [1.18e-01]
        movsd     _vmldCosHATab(%rip), %xmm0                    #2748.46
        movl      $1, %eax                                      #2750.24
        mulsd     %xmm0, %xmm1                                  #2748.46
        movsd     %xmm1, (%rsi)                                 #2748.19
        ret                                                     #2750.24
                                # LOE
..B10.5:                        # Preds ..B10.3 ..B10.2
                                # Execution count [2.10e-01]
        mulsd     %xmm1, %xmm1                                  #2754.33
        xorl      %eax, %eax                                    #2755.24
        movsd     %xmm1, (%rsi)                                 #2754.19
        ret                                                     #2755.24
                                # LOE
..B10.6:                        # Preds ..B10.1
                                # Execution count [6.71e-01]
        xorl      %eax, %eax                                    #2758.12
        ret                                                     #2758.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_dcos_ha_cout_rare_internal,@function
	.size	__svml_dcos_ha_cout_rare_internal,.-__svml_dcos_ha_cout_rare_internal
..LN__svml_dcos_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dcos_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dcos_ha_reduction_data_internal
	.globl __svml_dcos_ha_reduction_data_internal
__svml_dcos_ha_reduction_data_internal:
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
	.long	1
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	20
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	40
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	81
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	162
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	325
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	651
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1173554908
	.long	1
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2347109817
	.long	2
	.long	0
	.long	0
	.long	0
	.long	0
	.long	399252338
	.long	5
	.long	0
	.long	0
	.long	0
	.long	0
	.long	798504676
	.long	10
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1597009353
	.long	20
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3194018707
	.long	40
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2093070119
	.long	81
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4186140238
	.long	162
	.long	0
	.long	0
	.long	0
	.long	0
	.long	4077313180
	.long	325
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3859659065
	.long	651
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3424350834
	.long	1303
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2553734372
	.long	2607
	.long	0
	.long	0
	.long	0
	.long	0
	.long	812501448
	.long	5215
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1625002897
	.long	10430
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3250005794
	.long	20860
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2205044292
	.long	41721
	.long	0
	.long	0
	.long	0
	.long	0
	.long	115121288
	.long	83443
	.long	0
	.long	0
	.long	0
	.long	0
	.long	230242576
	.long	166886
	.long	0
	.long	0
	.long	0
	.long	0
	.long	460485152
	.long	333772
	.long	0
	.long	0
	.long	0
	.long	0
	.long	920970305
	.long	667544
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1841940610
	.long	1335088
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3683881221
	.long	2670176
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3072795146
	.long	5340353
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1850622997
	.long	10680707
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3701245994
	.long	21361414
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3107524692
	.long	42722829
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1920082089
	.long	85445659
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3840164178
	.long	170891318
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3385361061
	.long	341782637
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2475754826
	.long	683565275
	.long	0
	.long	0
	.long	0
	.long	0
	.long	656542356
	.long	1367130551
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1313084713
	.long	2734261102
	.long	0
	.long	0
	.long	1
	.long	0
	.long	2626169427
	.long	1173554908
	.long	0
	.long	0
	.long	2
	.long	0
	.long	957371559
	.long	2347109817
	.long	0
	.long	0
	.long	5
	.long	0
	.long	1914743119
	.long	399252338
	.long	0
	.long	0
	.long	10
	.long	0
	.long	3829486239
	.long	798504676
	.long	0
	.long	0
	.long	20
	.long	0
	.long	3364005183
	.long	1597009353
	.long	0
	.long	0
	.long	40
	.long	0
	.long	2433043071
	.long	3194018707
	.long	0
	.long	0
	.long	81
	.long	0
	.long	571118846
	.long	2093070119
	.long	0
	.long	0
	.long	162
	.long	0
	.long	1142237692
	.long	4186140238
	.long	0
	.long	0
	.long	325
	.long	0
	.long	2284475384
	.long	4077313180
	.long	0
	.long	0
	.long	651
	.long	0
	.long	273983472
	.long	3859659065
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	547966945
	.long	3424350834
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	1095933890
	.long	2553734372
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	2191867780
	.long	812501448
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	88768265
	.long	1625002897
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	177536531
	.long	3250005794
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	355073063
	.long	2205044292
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	710146126
	.long	115121288
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	1420292253
	.long	230242576
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	2840584506
	.long	460485152
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	1386201717
	.long	920970305
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	2772403434
	.long	1841940610
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	1249839573
	.long	3683881221
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	2499679147
	.long	3072795146
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	704390999
	.long	1850622997
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	1408781999
	.long	3701245994
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	2817563999
	.long	3107524692
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	1340160702
	.long	1920082089
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	2680321405
	.long	3840164178
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	1065675514
	.long	3385361061
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	2131351028
	.long	2475754826
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	4262702056
	.long	656542356
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	4230436817
	.long	1313084713
	.long	0
	.long	0
	.long	1173554908
	.long	1
	.long	4165906339
	.long	2626169427
	.long	0
	.long	0
	.long	2347109817
	.long	2
	.long	4036845383
	.long	957371559
	.long	0
	.long	0
	.long	399252338
	.long	5
	.long	3778723471
	.long	1914743119
	.long	0
	.long	0
	.long	798504676
	.long	10
	.long	3262479647
	.long	3829486239
	.long	0
	.long	0
	.long	1597009353
	.long	20
	.long	2229991998
	.long	3364005183
	.long	0
	.long	0
	.long	3194018707
	.long	40
	.long	165016701
	.long	2433043071
	.long	0
	.long	0
	.long	2093070119
	.long	81
	.long	330033402
	.long	571118846
	.long	0
	.long	0
	.long	4186140238
	.long	162
	.long	660066805
	.long	1142237692
	.long	0
	.long	0
	.long	4077313180
	.long	325
	.long	1320133610
	.long	2284475384
	.long	0
	.long	0
	.long	3859659065
	.long	651
	.long	2640267220
	.long	273983472
	.long	0
	.long	0
	.long	3424350834
	.long	1303
	.long	985567145
	.long	547966945
	.long	0
	.long	0
	.long	2553734372
	.long	2607
	.long	1971134291
	.long	1095933890
	.long	0
	.long	0
	.long	812501448
	.long	5215
	.long	3942268582
	.long	2191867780
	.long	0
	.long	0
	.long	1625002897
	.long	10430
	.long	3589569869
	.long	88768265
	.long	0
	.long	0
	.long	3250005794
	.long	20860
	.long	2884172442
	.long	177536531
	.long	0
	.long	0
	.long	2205044292
	.long	41721
	.long	1473377588
	.long	355073063
	.long	0
	.long	0
	.long	115121288
	.long	83443
	.long	2946755177
	.long	710146126
	.long	0
	.long	0
	.long	230242576
	.long	166886
	.long	1598543059
	.long	1420292253
	.long	0
	.long	0
	.long	460485152
	.long	333772
	.long	3197086118
	.long	2840584506
	.long	0
	.long	0
	.long	920970305
	.long	667544
	.long	2099204941
	.long	1386201717
	.long	0
	.long	0
	.long	1841940610
	.long	1335088
	.long	4198409883
	.long	2772403434
	.long	0
	.long	0
	.long	3683881221
	.long	2670176
	.long	4101852471
	.long	1249839573
	.long	0
	.long	0
	.long	3072795146
	.long	5340353
	.long	3908737646
	.long	2499679147
	.long	0
	.long	0
	.long	1850622997
	.long	10680707
	.long	3522507997
	.long	704390999
	.long	0
	.long	0
	.long	3701245994
	.long	21361414
	.long	2750048699
	.long	1408781999
	.long	0
	.long	0
	.long	3107524692
	.long	42722829
	.long	1205130103
	.long	2817563999
	.long	0
	.long	0
	.long	1920082089
	.long	85445659
	.long	2410260206
	.long	1340160702
	.long	0
	.long	0
	.long	3840164178
	.long	170891318
	.long	525553116
	.long	2680321405
	.long	0
	.long	0
	.long	3385361061
	.long	341782637
	.long	1051106232
	.long	1065675514
	.long	0
	.long	0
	.long	2475754826
	.long	683565275
	.long	2102212464
	.long	2131351028
	.long	0
	.long	0
	.long	656542356
	.long	1367130551
	.long	4204424928
	.long	4262702056
	.long	0
	.long	0
	.long	1313084713
	.long	2734261102
	.long	4113882560
	.long	4230436817
	.long	1
	.long	0
	.long	2626169427
	.long	1173554908
	.long	3932797825
	.long	4165906339
	.long	2
	.long	0
	.long	957371559
	.long	2347109817
	.long	3570628355
	.long	4036845383
	.long	5
	.long	0
	.long	1914743119
	.long	399252338
	.long	2846289414
	.long	3778723471
	.long	10
	.long	0
	.long	3829486239
	.long	798504676
	.long	1397611533
	.long	3262479647
	.long	20
	.long	0
	.long	3364005183
	.long	1597009353
	.long	2795223067
	.long	2229991998
	.long	40
	.long	0
	.long	2433043071
	.long	3194018707
	.long	1295478838
	.long	165016701
	.long	81
	.long	0
	.long	571118846
	.long	2093070119
	.long	2590957677
	.long	330033402
	.long	162
	.long	0
	.long	1142237692
	.long	4186140238
	.long	886948059
	.long	660066805
	.long	325
	.long	0
	.long	2284475384
	.long	4077313180
	.long	1773896118
	.long	1320133610
	.long	651
	.long	0
	.long	273983472
	.long	3859659065
	.long	3547792237
	.long	2640267220
	.long	1303
	.long	0
	.long	547966945
	.long	3424350834
	.long	2800617179
	.long	985567145
	.long	2607
	.long	0
	.long	1095933890
	.long	2553734372
	.long	1306267062
	.long	1971134291
	.long	5215
	.long	0
	.long	2191867780
	.long	812501448
	.long	2612534124
	.long	3942268582
	.long	10430
	.long	0
	.long	88768265
	.long	1625002897
	.long	930100952
	.long	3589569869
	.long	20860
	.long	0
	.long	177536531
	.long	3250005794
	.long	1860201905
	.long	2884172442
	.long	41721
	.long	0
	.long	355073063
	.long	2205044292
	.long	3720403810
	.long	1473377588
	.long	83443
	.long	0
	.long	710146126
	.long	115121288
	.long	3145840325
	.long	2946755177
	.long	166886
	.long	0
	.long	1420292253
	.long	230242576
	.long	1996713354
	.long	1598543059
	.long	333772
	.long	0
	.long	2840584506
	.long	460485152
	.long	3993426708
	.long	3197086118
	.long	667544
	.long	0
	.long	1386201717
	.long	920970305
	.long	3691886121
	.long	2099204941
	.long	1335088
	.long	0
	.long	2772403434
	.long	1841940610
	.long	3088804946
	.long	4198409883
	.long	2670176
	.long	0
	.long	1249839573
	.long	3683881221
	.long	1882642597
	.long	4101852471
	.long	5340353
	.long	0
	.long	2499679147
	.long	3072795146
	.long	3765285194
	.long	3908737646
	.long	10680707
	.long	0
	.long	704390999
	.long	1850622997
	.long	3235603093
	.long	3522507997
	.long	21361414
	.long	0
	.long	1408781999
	.long	3701245994
	.long	2176238891
	.long	2750048699
	.long	42722829
	.long	0
	.long	2817563999
	.long	3107524692
	.long	57510486
	.long	1205130103
	.long	85445659
	.long	0
	.long	1340160702
	.long	1920082089
	.long	115020972
	.long	2410260206
	.long	170891318
	.long	0
	.long	2680321405
	.long	3840164178
	.long	230041945
	.long	525553116
	.long	341782637
	.long	0
	.long	1065675514
	.long	3385361061
	.long	460083891
	.long	1051106232
	.long	683565275
	.long	0
	.long	2131351028
	.long	2475754826
	.long	920167782
	.long	2102212464
	.long	1367130551
	.long	0
	.long	4262702056
	.long	656542356
	.long	1840335564
	.long	4204424928
	.long	2734261102
	.long	0
	.long	4230436817
	.long	1313084713
	.long	3680671129
	.long	4113882560
	.long	1173554908
	.long	1
	.long	4165906339
	.long	2626169427
	.long	3066374962
	.long	3932797825
	.long	2347109817
	.long	2
	.long	4036845383
	.long	957371559
	.long	1837782628
	.long	3570628355
	.long	399252338
	.long	5
	.long	3778723471
	.long	1914743119
	.long	3675565257
	.long	2846289414
	.long	798504676
	.long	10
	.long	3262479647
	.long	3829486239
	.long	3056163219
	.long	1397611533
	.long	1597009353
	.long	20
	.long	2229991998
	.long	3364005183
	.long	1817359143
	.long	2795223067
	.long	3194018707
	.long	40
	.long	165016701
	.long	2433043071
	.long	3634718287
	.long	1295478838
	.long	2093070119
	.long	81
	.long	330033402
	.long	571118846
	.long	2974469278
	.long	2590957677
	.long	4186140238
	.long	162
	.long	660066805
	.long	1142237692
	.long	1653971260
	.long	886948059
	.long	4077313180
	.long	325
	.long	1320133610
	.long	2284475384
	.long	3307942520
	.long	1773896118
	.long	3859659065
	.long	651
	.long	2640267220
	.long	273983472
	.long	2320917745
	.long	3547792237
	.long	3424350834
	.long	1303
	.long	985567145
	.long	547966945
	.long	346868194
	.long	2800617179
	.long	2553734372
	.long	2607
	.long	1971134291
	.long	1095933890
	.long	693736388
	.long	1306267062
	.long	812501448
	.long	5215
	.long	3942268582
	.long	2191867780
	.long	1387472776
	.long	2612534124
	.long	1625002897
	.long	10430
	.long	3589569869
	.long	88768265
	.long	2774945552
	.long	930100952
	.long	3250005794
	.long	20860
	.long	2884172442
	.long	177536531
	.long	1254923809
	.long	1860201905
	.long	2205044292
	.long	41721
	.long	1473377588
	.long	355073063
	.long	2509847619
	.long	3720403810
	.long	115121288
	.long	83443
	.long	2946755177
	.long	710146126
	.long	724727943
	.long	3145840325
	.long	230242576
	.long	166886
	.long	1598543059
	.long	1420292253
	.long	1449455886
	.long	1996713354
	.long	460485152
	.long	333772
	.long	3197086118
	.long	2840584506
	.long	2898911772
	.long	3993426708
	.long	920970305
	.long	667544
	.long	2099204941
	.long	1386201717
	.long	1502856249
	.long	3691886121
	.long	1841940610
	.long	1335088
	.long	4198409883
	.long	2772403434
	.long	3005712498
	.long	3088804946
	.long	3683881221
	.long	2670176
	.long	4101852471
	.long	1249839573
	.long	1716457700
	.long	1882642597
	.long	3072795146
	.long	5340353
	.long	3908737646
	.long	2499679147
	.long	3432915400
	.long	3765285194
	.long	1850622997
	.long	10680707
	.long	3522507997
	.long	704390999
	.long	2570863504
	.long	3235603093
	.long	3701245994
	.long	21361414
	.long	2750048699
	.long	1408781999
	.long	846759712
	.long	2176238891
	.long	3107524692
	.long	42722829
	.long	1205130103
	.long	2817563999
	.long	1693519425
	.long	57510486
	.long	1920082089
	.long	85445659
	.long	2410260206
	.long	1340160702
	.long	3387038850
	.long	115020972
	.long	3840164178
	.long	170891318
	.long	525553116
	.long	2680321405
	.long	2479110404
	.long	230041945
	.long	3385361061
	.long	341782637
	.long	1051106232
	.long	1065675514
	.long	663253512
	.long	460083891
	.long	2475754826
	.long	683565275
	.long	2102212464
	.long	2131351028
	.long	1326507024
	.long	920167782
	.long	656542356
	.long	1367130551
	.long	4204424928
	.long	4262702056
	.long	2653014048
	.long	1840335564
	.long	1313084713
	.long	2734261102
	.long	4113882560
	.long	4230436817
	.long	1011060801
	.long	3680671129
	.long	2626169427
	.long	1173554908
	.long	3932797825
	.long	4165906339
	.long	2022121603
	.long	3066374962
	.long	957371559
	.long	2347109817
	.long	3570628355
	.long	4036845383
	.long	4044243207
	.long	1837782628
	.long	1914743119
	.long	399252338
	.long	2846289414
	.long	3778723471
	.long	3793519119
	.long	3675565257
	.long	3829486239
	.long	798504676
	.long	1397611533
	.long	3262479647
	.long	3292070943
	.long	3056163219
	.long	3364005183
	.long	1597009353
	.long	2795223067
	.long	2229991998
	.long	2289174591
	.long	1817359143
	.long	2433043071
	.long	3194018707
	.long	1295478838
	.long	165016701
	.long	283381887
	.long	3634718287
	.long	571118846
	.long	2093070119
	.long	2590957677
	.long	330033402
	.long	566763775
	.long	2974469278
	.long	1142237692
	.long	4186140238
	.long	886948059
	.long	660066805
	.long	1133527550
	.long	1653971260
	.long	2284475384
	.long	4077313180
	.long	1773896118
	.long	1320133610
	.long	2267055100
	.long	3307942520
	.long	273983472
	.long	3859659065
	.long	3547792237
	.long	2640267220
	.long	239142905
	.long	2320917745
	.long	547966945
	.long	3424350834
	.long	2800617179
	.long	985567145
	.long	478285810
	.long	346868194
	.long	1095933890
	.long	2553734372
	.long	1306267062
	.long	1971134291
	.long	956571621
	.long	693736388
	.long	2191867780
	.long	812501448
	.long	2612534124
	.long	3942268582
	.long	1913143242
	.long	1387472776
	.long	88768265
	.long	1625002897
	.long	930100952
	.long	3589569869
	.long	3826286484
	.long	2774945552
	.long	177536531
	.long	3250005794
	.long	1860201905
	.long	2884172442
	.long	3357605672
	.long	1254923809
	.long	355073063
	.long	2205044292
	.long	3720403810
	.long	1473377588
	.long	2420244049
	.long	2509847619
	.long	710146126
	.long	115121288
	.long	3145840325
	.long	2946755177
	.long	545520802
	.long	724727943
	.long	1420292253
	.long	230242576
	.long	1996713354
	.long	1598543059
	.long	1091041605
	.long	1449455886
	.long	2840584506
	.long	460485152
	.long	3993426708
	.long	3197086118
	.long	2182083211
	.long	2898911772
	.long	1386201717
	.long	920970305
	.long	3691886121
	.long	2099204941
	.long	69199126
	.long	1502856249
	.long	2772403434
	.long	1841940610
	.long	3088804946
	.long	4198409883
	.long	138398252
	.long	3005712498
	.long	1249839573
	.long	3683881221
	.long	1882642597
	.long	4101852471
	.long	276796504
	.long	1716457700
	.long	2499679147
	.long	3072795146
	.long	3765285194
	.long	3908737646
	.long	553593009
	.long	3432915400
	.long	704390999
	.long	1850622997
	.long	3235603093
	.long	3522507997
	.long	1107186019
	.long	2570863504
	.long	1408781999
	.long	3701245994
	.long	2176238891
	.long	2750048699
	.long	2214372039
	.long	846759712
	.long	2817563999
	.long	3107524692
	.long	57510486
	.long	1205130103
	.long	133776782
	.long	1693519425
	.long	1340160702
	.long	1920082089
	.long	115020972
	.long	2410260206
	.long	267553565
	.long	3387038850
	.long	2680321405
	.long	3840164178
	.long	230041945
	.long	525553116
	.long	535107130
	.long	2479110404
	.long	1065675514
	.long	3385361061
	.long	460083891
	.long	1051106232
	.long	1070214261
	.long	663253512
	.long	2131351028
	.long	2475754826
	.long	920167782
	.long	2102212464
	.long	2140428522
	.long	1326507024
	.long	4262702056
	.long	656542356
	.long	1840335564
	.long	4204424928
	.long	4280857045
	.long	2653014048
	.long	4230436817
	.long	1313084713
	.long	3680671129
	.long	4113882560
	.long	4266746795
	.long	1011060801
	.long	4165906339
	.long	2626169427
	.long	3066374962
	.long	3932797825
	.long	4238526295
	.long	2022121603
	.long	4036845383
	.long	957371559
	.long	1837782628
	.long	3570628355
	.long	4182085295
	.long	4044243207
	.long	3778723471
	.long	1914743119
	.long	3675565257
	.long	2846289414
	.long	4069203294
	.long	3793519119
	.long	3262479647
	.long	3829486239
	.long	3056163219
	.long	1397611533
	.long	3843439293
	.long	3292070943
	.long	2229991998
	.long	3364005183
	.long	1817359143
	.long	2795223067
	.long	3391911291
	.long	2289174591
	.long	165016701
	.long	2433043071
	.long	3634718287
	.long	1295478838
	.long	2488855287
	.long	283381887
	.long	330033402
	.long	571118846
	.long	2974469278
	.long	2590957677
	.long	682743279
	.long	566763775
	.long	660066805
	.long	1142237692
	.long	1653971260
	.long	886948059
	.long	1365486558
	.long	1133527550
	.long	1320133610
	.long	2284475384
	.long	3307942520
	.long	1773896118
	.long	2730973117
	.long	2267055100
	.long	2640267220
	.long	273983472
	.long	2320917745
	.long	3547792237
	.long	1166978938
	.long	239142905
	.long	985567145
	.long	547966945
	.long	346868194
	.long	2800617179
	.long	2333957877
	.long	478285810
	.long	1971134291
	.long	1095933890
	.long	693736388
	.long	1306267062
	.long	372948459
	.long	956571621
	.long	3942268582
	.long	2191867780
	.long	1387472776
	.long	2612534124
	.long	745896919
	.long	1913143242
	.long	3589569869
	.long	88768265
	.long	2774945552
	.long	930100952
	.long	1491793838
	.long	3826286484
	.long	2884172442
	.long	177536531
	.long	1254923809
	.long	1860201905
	.long	2983587677
	.long	3357605672
	.long	1473377588
	.long	355073063
	.long	2509847619
	.long	3720403810
	.long	1672208059
	.long	2420244049
	.long	2946755177
	.long	710146126
	.long	724727943
	.long	3145840325
	.long	3344416119
	.long	545520802
	.long	1598543059
	.long	1420292253
	.long	1449455886
	.long	1996713354
	.long	2393864943
	.long	1091041605
	.long	3197086118
	.long	2840584506
	.long	2898911772
	.long	3993426708
	.long	492762590
	.long	2182083211
	.long	2099204941
	.long	1386201717
	.long	1502856249
	.long	3691886121
	.long	985525180
	.long	69199126
	.long	4198409883
	.long	2772403434
	.long	3005712498
	.long	3088804946
	.long	1971050360
	.long	138398252
	.long	4101852471
	.long	1249839573
	.long	1716457700
	.long	1882642597
	.long	3942100721
	.long	276796504
	.long	3908737646
	.long	2499679147
	.long	3432915400
	.long	3765285194
	.long	3589234146
	.long	553593009
	.long	3522507997
	.long	704390999
	.long	2570863504
	.long	3235603093
	.long	2883500997
	.long	1107186019
	.long	2750048699
	.long	1408781999
	.long	846759712
	.long	2176238891
	.long	1472034698
	.long	2214372039
	.long	1205130103
	.long	2817563999
	.long	1693519425
	.long	57510486
	.long	2944069397
	.long	133776782
	.long	2410260206
	.long	1340160702
	.long	3387038850
	.long	115020972
	.long	1593171499
	.long	267553565
	.long	525553116
	.long	2680321405
	.long	2479110404
	.long	230041945
	.long	3186342998
	.long	535107130
	.long	1051106232
	.long	1065675514
	.long	663253512
	.long	460083891
	.long	2077718700
	.long	1070214261
	.long	2102212464
	.long	2131351028
	.long	1326507024
	.long	920167782
	.long	4155437400
	.long	2140428522
	.long	4204424928
	.long	4262702056
	.long	2653014048
	.long	1840335564
	.long	4015907504
	.long	4280857045
	.long	4113882560
	.long	4230436817
	.long	1011060801
	.long	3680671129
	.long	3736847713
	.long	4266746795
	.long	3932797825
	.long	4165906339
	.long	2022121603
	.long	3066374962
	.long	3178728131
	.long	4238526295
	.long	3570628355
	.long	4036845383
	.long	4044243207
	.long	1837782628
	.long	2062488966
	.long	4182085295
	.long	2846289414
	.long	3778723471
	.long	3793519119
	.long	3675565257
	.long	4124977933
	.long	4069203294
	.long	1397611533
	.long	3262479647
	.long	3292070943
	.long	3056163219
	.long	3954988571
	.long	3843439293
	.long	2795223067
	.long	2229991998
	.long	2289174591
	.long	1817359143
	.long	3615009846
	.long	3391911291
	.long	1295478838
	.long	165016701
	.long	283381887
	.long	3634718287
	.long	2935052397
	.long	2488855287
	.long	2590957677
	.long	330033402
	.long	566763775
	.long	2974469278
	.long	1575137499
	.long	682743279
	.long	886948059
	.long	660066805
	.long	1133527550
	.long	1653971260
	.long	3150274999
	.long	1365486558
	.long	1773896118
	.long	1320133610
	.long	2267055100
	.long	3307942520
	.long	2005582702
	.long	2730973117
	.long	3547792237
	.long	2640267220
	.long	239142905
	.long	2320917745
	.long	4011165404
	.long	1166978938
	.long	2800617179
	.long	985567145
	.long	478285810
	.long	346868194
	.long	3727363513
	.long	2333957877
	.long	1306267062
	.long	1971134291
	.long	956571621
	.long	693736388
	.long	3159759730
	.long	372948459
	.long	2612534124
	.long	3942268582
	.long	1913143242
	.long	1387472776
	.long	2024552164
	.long	745896919
	.long	930100952
	.long	3589569869
	.long	3826286484
	.long	2774945552
	.long	4049104329
	.long	1491793838
	.long	1860201905
	.long	2884172442
	.long	3357605672
	.long	1254923809
	.long	3803241362
	.long	2983587677
	.long	3720403810
	.long	1473377588
	.long	2420244049
	.long	2509847619
	.long	3311515428
	.long	1672208059
	.long	3145840325
	.long	2946755177
	.long	545520802
	.long	724727943
	.long	2328063560
	.long	3344416119
	.long	1996713354
	.long	1598543059
	.long	1091041605
	.long	1449455886
	.long	361159825
	.long	2393864943
	.long	3993426708
	.long	3197086118
	.long	2182083211
	.long	2898911772
	.long	722319651
	.long	492762590
	.long	3691886121
	.long	2099204941
	.long	69199126
	.long	1502856249
	.long	1444639302
	.long	985525180
	.long	3088804946
	.long	4198409883
	.long	138398252
	.long	3005712498
	.long	2889278605
	.long	1971050360
	.long	1882642597
	.long	4101852471
	.long	276796504
	.long	1716457700
	.long	1483589915
	.long	3942100721
	.long	3765285194
	.long	3908737646
	.long	553593009
	.long	3432915400
	.long	2967179831
	.long	3589234146
	.long	3235603093
	.long	3522507997
	.long	1107186019
	.long	2570863504
	.long	1639392366
	.long	2883500997
	.long	2176238891
	.long	2750048699
	.long	2214372039
	.long	846759712
	.long	3278784732
	.long	1472034698
	.long	57510486
	.long	1205130103
	.long	133776782
	.long	1693519425
	.long	2262602168
	.long	2944069397
	.long	115020972
	.long	2410260206
	.long	267553565
	.long	3387038850
	.long	230237041
	.long	1593171499
	.long	230041945
	.long	525553116
	.long	535107130
	.long	2479110404
	.long	460474083
	.long	3186342998
	.long	460083891
	.long	1051106232
	.long	1070214261
	.long	663253512
	.long	920948167
	.long	2077718700
	.long	920167782
	.long	2102212464
	.long	2140428522
	.long	1326507024
	.long	1841896334
	.long	4155437400
	.long	1840335564
	.long	4204424928
	.long	4280857045
	.long	2653014048
	.long	3683792669
	.long	4015907504
	.long	3680671129
	.long	4113882560
	.long	4266746795
	.long	1011060801
	.long	3072618042
	.long	3736847713
	.long	3066374962
	.long	3932797825
	.long	4238526295
	.long	2022121603
	.long	1850268788
	.long	3178728131
	.long	1837782628
	.long	3570628355
	.long	4182085295
	.long	4044243207
	.long	3700537577
	.long	2062488966
	.long	3675565257
	.long	2846289414
	.long	4069203294
	.long	3793519119
	.long	3106107858
	.long	4124977933
	.long	3056163219
	.long	1397611533
	.long	3843439293
	.long	3292070943
	.long	1917248420
	.long	3954988571
	.long	1817359143
	.long	2795223067
	.long	3391911291
	.long	2289174591
	.long	3834496840
	.long	3615009846
	.long	3634718287
	.long	1295478838
	.long	2488855287
	.long	283381887
	.long	3374026384
	.long	2935052397
	.long	2974469278
	.long	2590957677
	.long	682743279
	.long	566763775
	.long	2453085473
	.long	1575137499
	.long	1653971260
	.long	886948059
	.long	1365486558
	.long	1133527550
	.long	611203650
	.long	3150274999
	.long	3307942520
	.long	1773896118
	.long	2730973117
	.long	2267055100
	.long	1222407300
	.long	2005582702
	.long	2320917745
	.long	3547792237
	.long	1166978938
	.long	239142905
	.long	2444814601
	.long	4011165404
	.long	346868194
	.long	2800617179
	.long	2333957877
	.long	478285810
	.long	594661906
	.long	3727363513
	.long	693736388
	.long	1306267062
	.long	372948459
	.long	956571621
	.long	1189323812
	.long	3159759730
	.long	1387472776
	.long	2612534124
	.long	745896919
	.long	1913143242
	.long	2378647625
	.long	2024552164
	.long	2774945552
	.long	930100952
	.long	1491793838
	.long	3826286484
	.long	462327955
	.long	4049104329
	.long	1254923809
	.long	1860201905
	.long	2983587677
	.long	3357605672
	.long	924655910
	.long	3803241362
	.long	2509847619
	.long	3720403810
	.long	1672208059
	.long	2420244049
	.long	1849311821
	.long	3311515428
	.long	724727943
	.long	3145840325
	.long	3344416119
	.long	545520802
	.long	3698623643
	.long	2328063560
	.long	1449455886
	.long	1996713354
	.long	2393864943
	.long	1091041605
	.long	3102279991
	.long	361159825
	.long	2898911772
	.long	3993426708
	.long	492762590
	.long	2182083211
	.long	1909592686
	.long	722319651
	.long	1502856249
	.long	3691886121
	.long	985525180
	.long	69199126
	.long	3819185373
	.long	1444639302
	.long	3005712498
	.long	3088804946
	.long	1971050360
	.long	138398252
	.long	3343403450
	.long	2889278605
	.long	1716457700
	.long	1882642597
	.long	3942100721
	.long	276796504
	.long	2391839604
	.long	1483589915
	.long	3432915400
	.long	3765285194
	.long	3589234146
	.long	553593009
	.long	488711913
	.long	2967179831
	.long	2570863504
	.long	3235603093
	.long	2883500997
	.long	1107186019
	.long	977423826
	.long	1639392366
	.long	846759712
	.long	2176238891
	.long	1472034698
	.long	2214372039
	.long	1954847653
	.long	3278784732
	.long	1693519425
	.long	57510486
	.long	2944069397
	.long	133776782
	.long	3909695307
	.long	2262602168
	.long	3387038850
	.long	115020972
	.long	1593171499
	.long	267553565
	.long	3524423319
	.long	230237041
	.long	2479110404
	.long	230041945
	.long	3186342998
	.long	535107130
	.long	2753879342
	.long	460474083
	.long	663253512
	.long	460083891
	.long	2077718700
	.long	1070214261
	.long	1212791388
	.long	920948167
	.long	1326507024
	.long	920167782
	.long	4155437400
	.long	2140428522
	.long	2425582776
	.long	1841896334
	.long	2653014048
	.long	1840335564
	.long	4015907504
	.long	4280857045
	.long	556198256
	.long	3683792669
	.long	1011060801
	.long	3680671129
	.long	3736847713
	.long	4266746795
	.long	1112396512
	.long	3072618042
	.long	2022121603
	.long	3066374962
	.long	3178728131
	.long	4238526295
	.long	2224793024
	.long	1850268788
	.long	4044243207
	.long	1837782628
	.long	2062488966
	.long	4182085295
	.long	154618752
	.long	3700537577
	.long	3793519119
	.long	3675565257
	.long	4124977933
	.long	4069203294
	.long	309237504
	.long	3106107858
	.long	3292070943
	.long	3056163219
	.long	3954988571
	.long	3843439293
	.long	618475008
	.long	1917248420
	.long	2289174591
	.long	1817359143
	.long	3615009846
	.long	3391911291
	.long	1236950016
	.long	3834496840
	.long	283381887
	.long	3634718287
	.long	2935052397
	.long	2488855287
	.long	2473900033
	.long	3374026384
	.long	566763775
	.long	2974469278
	.long	1575137499
	.long	682743279
	.long	652832771
	.long	2453085473
	.long	1133527550
	.long	1653971260
	.long	3150274999
	.long	1365486558
	.long	1305665542
	.long	611203650
	.long	2267055100
	.long	3307942520
	.long	2005582702
	.long	2730973117
	.long	2611331084
	.long	1222407300
	.long	239142905
	.long	2320917745
	.long	4011165404
	.long	1166978938
	.long	927694873
	.long	2444814601
	.long	478285810
	.long	346868194
	.long	3727363513
	.long	2333957877
	.long	1855389746
	.long	594661906
	.long	956571621
	.long	693736388
	.long	3159759730
	.long	372948459
	.long	3710779492
	.long	1189323812
	.long	1913143242
	.long	1387472776
	.long	2024552164
	.long	745896919
	.long	3126591689
	.long	2378647625
	.long	3826286484
	.long	2774945552
	.long	4049104329
	.long	1491793838
	.long	1958216082
	.long	462327955
	.long	3357605672
	.long	1254923809
	.long	3803241362
	.long	2983587677
	.long	3916432164
	.long	924655910
	.long	2420244049
	.long	2509847619
	.long	3311515428
	.long	1672208059
	.long	3537897033
	.long	1849311821
	.long	545520802
	.long	724727943
	.long	2328063560
	.long	3344416119
	.long	2780826770
	.long	3698623643
	.long	1091041605
	.long	1449455886
	.long	361159825
	.long	2393864943
	.long	1266686244
	.long	3102279991
	.long	2182083211
	.long	2898911772
	.long	722319651
	.long	492762590
	.long	2533372489
	.long	1909592686
	.long	69199126
	.long	1502856249
	.long	1444639302
	.long	985525180
	.long	771777682
	.long	3819185373
	.long	138398252
	.long	3005712498
	.long	2889278605
	.long	1971050360
	.long	1543555365
	.long	3343403450
	.long	276796504
	.long	1716457700
	.long	1483589915
	.long	3942100721
	.long	3087110731
	.long	2391839604
	.long	553593009
	.long	3432915400
	.long	2967179831
	.long	3589234146
	.long	1879254167
	.long	488711913
	.long	1107186019
	.long	2570863504
	.long	1639392366
	.long	2883500997
	.long	3758508334
	.long	977423826
	.long	2214372039
	.long	846759712
	.long	3278784732
	.long	1472034698
	.long	3222049373
	.long	1954847653
	.long	133776782
	.long	1693519425
	.long	2262602168
	.long	2944069397
	.long	2149131451
	.long	3909695307
	.long	267553565
	.long	3387038850
	.long	230237041
	.long	1593171499
	.long	3295607
	.long	3524423319
	.long	535107130
	.long	2479110404
	.long	460474083
	.long	3186342998
	.long	6591214
	.long	2753879342
	.long	1070214261
	.long	663253512
	.long	920948167
	.long	2077718700
	.long	13182429
	.long	1212791388
	.long	2140428522
	.long	1326507024
	.long	1841896334
	.long	4155437400
	.long	26364858
	.long	2425582776
	.long	4280857045
	.long	2653014048
	.long	3683792669
	.long	4015907504
	.long	52729717
	.long	556198256
	.long	4266746795
	.long	1011060801
	.long	3072618042
	.long	3736847713
	.long	105459434
	.long	1112396512
	.long	4238526295
	.long	2022121603
	.long	1850268788
	.long	3178728131
	.long	210918868
	.long	2224793024
	.long	4182085295
	.long	4044243207
	.long	3700537577
	.long	2062488966
	.long	421837736
	.long	154618752
	.long	4069203294
	.long	3793519119
	.long	3106107858
	.long	4124977933
	.long	843675472
	.long	309237504
	.long	3843439293
	.long	3292070943
	.long	1917248420
	.long	3954988571
	.long	1687350944
	.long	618475008
	.long	3391911291
	.long	2289174591
	.long	3834496840
	.long	3615009846
	.long	3374701889
	.long	1236950016
	.long	2488855287
	.long	283381887
	.long	3374026384
	.long	2935052397
	.long	2454436482
	.long	2473900033
	.long	682743279
	.long	566763775
	.long	2453085473
	.long	1575137499
	.long	613905668
	.long	652832771
	.long	1365486558
	.long	1133527550
	.long	611203650
	.long	3150274999
	.long	1227811337
	.long	1305665542
	.long	2730973117
	.long	2267055100
	.long	1222407300
	.long	2005582702
	.long	2455622675
	.long	2611331084
	.long	1166978938
	.long	239142905
	.long	2444814601
	.long	4011165404
	.long	616278055
	.long	927694873
	.long	2333957877
	.long	478285810
	.long	594661906
	.long	3727363513
	.long	1232556110
	.long	1855389746
	.long	372948459
	.long	956571621
	.long	1189323812
	.long	3159759730
	.long	2465112221
	.long	3710779492
	.long	745896919
	.long	1913143242
	.long	2378647625
	.long	2024552164
	.long	635257146
	.long	3126591689
	.long	1491793838
	.long	3826286484
	.long	462327955
	.long	4049104329
	.long	1270514292
	.long	1958216082
	.long	2983587677
	.long	3357605672
	.long	924655910
	.long	3803241362
	.long	2541028584
	.long	3916432164
	.long	1672208059
	.long	2420244049
	.long	1849311821
	.long	3311515428
	.long	787089873
	.long	3537897033
	.long	3344416119
	.long	545520802
	.long	3698623643
	.long	2328063560
	.long	1574179747
	.long	2780826770
	.long	2393864943
	.long	1091041605
	.long	3102279991
	.long	361159825
	.long	3148359494
	.long	1266686244
	.long	492762590
	.long	2182083211
	.long	1909592686
	.long	722319651
	.long	2001751692
	.long	2533372489
	.long	985525180
	.long	69199126
	.long	3819185373
	.long	1444639302
	.long	4003503385
	.long	771777682
	.long	1971050360
	.long	138398252
	.long	3343403450
	.long	2889278605
	.long	3712039474
	.long	1543555365
	.long	3942100721
	.long	276796504
	.long	2391839604
	.long	1483589915
	.long	3129111652
	.long	3087110731
	.long	3589234146
	.long	553593009
	.long	488711913
	.long	2967179831
	.long	1963256009
	.long	1879254167
	.long	2883500997
	.long	1107186019
	.long	977423826
	.long	1639392366
	.long	3926512018
	.long	3758508334
	.long	1472034698
	.long	2214372039
	.long	1954847653
	.long	3278784732
	.long	3558056740
	.long	3222049373
	.long	2944069397
	.long	133776782
	.long	3909695307
	.long	2262602168
	.long	2821146184
	.long	2149131451
	.long	1593171499
	.long	267553565
	.long	3524423319
	.long	230237041
	.long	1347325072
	.long	3295607
	.long	3186342998
	.long	535107130
	.long	2753879342
	.long	460474083
	.long	2694650145
	.long	6591214
	.long	2077718700
	.long	1070214261
	.long	1212791388
	.long	920948167
	.long	1094332995
	.long	13182429
	.long	4155437400
	.long	2140428522
	.long	2425582776
	.long	1841896334
	.long	2188665991
	.long	26364858
	.long	4015907504
	.long	4280857045
	.long	556198256
	.long	3683792669
	.long	82364686
	.long	52729717
	.long	3736847713
	.long	4266746795
	.long	1112396512
	.long	3072618042
	.long	164729372
	.long	105459434
	.long	3178728131
	.long	4238526295
	.long	2224793024
	.long	1850268788
	.long	329458745
	.long	210918868
	.long	2062488966
	.long	4182085295
	.long	154618752
	.long	3700537577
	.long	658917491
	.long	421837736
	.long	4124977933
	.long	4069203294
	.long	309237504
	.long	3106107858
	.long	1317834983
	.long	843675472
	.long	3954988571
	.long	3843439293
	.long	618475008
	.long	1917248420
	.long	2635669967
	.long	1687350944
	.long	3615009846
	.long	3391911291
	.long	1236950016
	.long	3834496840
	.long	976372639
	.long	3374701889
	.long	2935052397
	.long	2488855287
	.long	2473900033
	.long	3374026384
	.long	1952745279
	.long	2454436482
	.long	1575137499
	.long	682743279
	.long	652832771
	.long	2453085473
	.long	3905490559
	.long	613905668
	.long	3150274999
	.long	1365486558
	.long	1305665542
	.long	611203650
	.long	3516013822
	.long	1227811337
	.long	2005582702
	.long	2730973117
	.long	2611331084
	.long	1222407300
	.long	2737060348
	.long	2455622675
	.long	4011165404
	.long	1166978938
	.long	927694873
	.long	2444814601
	.long	1179153400
	.long	616278055
	.long	3727363513
	.long	2333957877
	.long	1855389746
	.long	594661906
	.long	2358306800
	.long	1232556110
	.long	3159759730
	.long	372948459
	.long	3710779492
	.long	1189323812
	.long	421646305
	.long	2465112221
	.long	2024552164
	.long	745896919
	.long	3126591689
	.long	2378647625
	.long	843292611
	.long	635257146
	.long	4049104329
	.long	1491793838
	.long	1958216082
	.long	462327955
	.long	1686585223
	.long	1270514292
	.long	3803241362
	.long	2983587677
	.long	3916432164
	.long	924655910
	.long	3373170446
	.long	2541028584
	.long	3311515428
	.long	1672208059
	.long	3537897033
	.long	1849311821
	.long	2451373597
	.long	787089873
	.long	2328063560
	.long	3344416119
	.long	2780826770
	.long	3698623643
	.long	607779899
	.long	1574179747
	.long	361159825
	.long	2393864943
	.long	1266686244
	.long	3102279991
	.long	1215559799
	.long	3148359494
	.long	722319651
	.long	492762590
	.long	2533372489
	.long	1909592686
	.long	2431119599
	.long	2001751692
	.long	1444639302
	.long	985525180
	.long	771777682
	.long	3819185373
	.long	567271902
	.long	4003503385
	.long	2889278605
	.long	1971050360
	.long	1543555365
	.long	3343403450
	.long	1134543805
	.long	3712039474
	.long	1483589915
	.long	3942100721
	.long	3087110731
	.long	2391839604
	.long	2269087610
	.long	3129111652
	.long	2967179831
	.long	3589234146
	.long	1879254167
	.long	488711913
	.long	243207925
	.long	1963256009
	.long	1639392366
	.long	2883500997
	.long	3758508334
	.long	977423826
	.long	486415851
	.long	3926512018
	.long	3278784732
	.long	1472034698
	.long	3222049373
	.long	1954847653
	.long	972831702
	.long	3558056740
	.long	2262602168
	.long	2944069397
	.long	2149131451
	.long	3909695307
	.long	1945663404
	.long	2821146184
	.long	230237041
	.long	1593171499
	.long	3295607
	.long	3524423319
	.long	3891326808
	.long	1347325072
	.long	460474083
	.long	3186342998
	.long	6591214
	.long	2753879342
	.long	3487686321
	.long	2694650145
	.long	920948167
	.long	2077718700
	.long	13182429
	.long	1212791388
	.long	2680405347
	.long	1094332995
	.long	1841896334
	.long	4155437400
	.long	26364858
	.long	2425582776
	.long	1065843399
	.long	2188665991
	.long	3683792669
	.long	4015907504
	.long	52729717
	.long	556198256
	.long	2131686798
	.long	82364686
	.long	3072618042
	.long	3736847713
	.long	105459434
	.long	1112396512
	.long	4263373596
	.long	164729372
	.long	1850268788
	.long	3178728131
	.long	210918868
	.long	2224793024
	.long	4231779897
	.long	329458745
	.long	3700537577
	.long	2062488966
	.long	421837736
	.long	154618752
	.long	4168592498
	.long	658917491
	.long	3106107858
	.long	4124977933
	.long	843675472
	.long	309237504
	.long	4042217701
	.long	1317834983
	.long	1917248420
	.long	3954988571
	.long	1687350944
	.long	618475008
	.long	3789468107
	.long	2635669967
	.long	3834496840
	.long	3615009846
	.long	3374701889
	.long	1236950016
	.long	3283968918
	.long	976372639
	.long	3374026384
	.long	2935052397
	.long	2454436482
	.long	2473900033
	.long	2272970540
	.long	1952745279
	.long	2453085473
	.long	1575137499
	.long	613905668
	.long	652832771
	.long	250973784
	.long	3905490559
	.long	611203650
	.long	3150274999
	.long	1227811337
	.long	1305665542
	.long	501947569
	.long	3516013822
	.long	1222407300
	.long	2005582702
	.long	2455622675
	.long	2611331084
	.long	1003895138
	.long	2737060348
	.long	2444814601
	.long	4011165404
	.long	616278055
	.long	927694873
	.long	2007790276
	.long	1179153400
	.long	594661906
	.long	3727363513
	.long	1232556110
	.long	1855389746
	.long	4015580553
	.long	2358306800
	.long	1189323812
	.long	3159759730
	.long	2465112221
	.long	3710779492
	.long	3736193810
	.long	421646305
	.long	2378647625
	.long	2024552164
	.long	635257146
	.long	3126591689
	.long	3177420325
	.long	843292611
	.long	462327955
	.long	4049104329
	.long	1270514292
	.long	1958216082
	.long	2059873354
	.long	1686585223
	.long	924655910
	.long	3803241362
	.long	2541028584
	.long	3916432164
	.long	4119746708
	.long	3373170446
	.long	1849311821
	.long	3311515428
	.long	787089873
	.long	3537897033
	.long	3944526121
	.long	2451373597
	.long	3698623643
	.long	2328063560
	.long	1574179747
	.long	2780826770
	.long	3594084947
	.long	607779899
	.long	3102279991
	.long	361159825
	.long	3148359494
	.long	1266686244
	.long	2893202598
	.long	1215559799
	.long	1909592686
	.long	722319651
	.long	2001751692
	.long	2533372489
	.long	1491437901
	.long	2431119599
	.long	3819185373
	.long	1444639302
	.long	4003503385
	.long	771777682
	.long	2982875802
	.long	567271902
	.long	3343403450
	.long	2889278605
	.long	3712039474
	.long	1543555365
	.long	1670784308
	.long	1134543805
	.long	2391839604
	.long	1483589915
	.long	3129111652
	.long	3087110731
	.long	3341568617
	.long	2269087610
	.long	488711913
	.long	2967179831
	.long	1963256009
	.long	1879254167
	.long	2388169939
	.long	243207925
	.long	977423826
	.long	1639392366
	.long	3926512018
	.long	3758508334
	.long	481372583
	.long	486415851
	.long	1954847653
	.long	3278784732
	.long	3558056740
	.long	3222049373
	.long	962745166
	.long	972831702
	.long	3909695307
	.long	2262602168
	.long	2821146184
	.long	2149131451
	.long	1925490332
	.long	1945663404
	.long	3524423319
	.long	230237041
	.long	1347325072
	.long	3295607
	.long	3850980665
	.long	3891326808
	.long	2753879342
	.long	460474083
	.long	2694650145
	.long	6591214
	.long	3406994035
	.long	3487686321
	.long	1212791388
	.long	920948167
	.long	1094332995
	.long	13182429
	.long	2519020775
	.long	2680405347
	.long	2425582776
	.long	1841896334
	.long	2188665991
	.long	26364858
	.long	743074255
	.long	1065843399
	.long	556198256
	.long	3683792669
	.long	82364686
	.long	52729717
	.long	1486148511
	.long	2131686798
	.long	1112396512
	.long	3072618042
	.long	164729372
	.long	105459434
	.long	2972297022
	.long	4263373596
	.long	2224793024
	.long	1850268788
	.long	329458745
	.long	210918868
	.long	1649626749
	.long	4231779897
	.long	154618752
	.long	3700537577
	.long	658917491
	.long	421837736
	.long	3299253499
	.long	4168592498
	.long	309237504
	.long	3106107858
	.long	1317834983
	.long	843675472
	.long	2303539703
	.long	4042217701
	.long	618475008
	.long	1917248420
	.long	2635669967
	.long	1687350944
	.long	312112110
	.long	3789468107
	.long	1236950016
	.long	3834496840
	.long	976372639
	.long	3374701889
	.long	624224221
	.long	3283968918
	.long	2473900033
	.long	3374026384
	.long	1952745279
	.long	2454436482
	.long	1248448442
	.long	2272970540
	.long	652832771
	.long	2453085473
	.long	3905490559
	.long	613905668
	.long	2496896884
	.long	250973784
	.long	1305665542
	.long	611203650
	.long	3516013822
	.long	1227811337
	.long	698826472
	.long	501947569
	.long	2611331084
	.long	1222407300
	.long	2737060348
	.long	2455622675
	.long	1397652945
	.long	1003895138
	.long	927694873
	.long	2444814601
	.long	1179153400
	.long	616278055
	.long	2795305890
	.long	2007790276
	.long	1855389746
	.long	594661906
	.long	2358306800
	.long	1232556110
	.long	1295644484
	.long	4015580553
	.long	3710779492
	.long	1189323812
	.long	421646305
	.long	2465112221
	.long	2591288968
	.long	3736193810
	.long	3126591689
	.long	2378647625
	.long	843292611
	.long	635257146
	.long	887610640
	.long	3177420325
	.long	1958216082
	.long	462327955
	.long	1686585223
	.long	1270514292
	.long	1775221280
	.long	2059873354
	.long	3916432164
	.long	924655910
	.long	3373170446
	.long	2541028584
	.long	3550442561
	.long	4119746708
	.long	3537897033
	.long	1849311821
	.long	2451373597
	.long	787089873
	.long	2805917826
	.long	3944526121
	.long	2780826770
	.long	3698623643
	.long	607779899
	.long	1574179747
	.long	1316868356
	.long	3594084947
	.long	1266686244
	.long	3102279991
	.long	1215559799
	.long	3148359494
	.long	2633736712
	.long	2893202598
	.long	2533372489
	.long	1909592686
	.long	2431119599
	.long	2001751692
	.long	972506129
	.long	1491437901
	.long	771777682
	.long	3819185373
	.long	567271902
	.long	4003503385
	.long	1945012259
	.long	2982875802
	.long	1543555365
	.long	3343403450
	.long	1134543805
	.long	3712039474
	.long	3890024518
	.long	1670784308
	.long	3087110731
	.long	2391839604
	.long	2269087610
	.long	3129111652
	.long	3485081741
	.long	3341568617
	.long	1879254167
	.long	488711913
	.long	243207925
	.long	1963256009
	.long	2675196186
	.long	2388169939
	.long	3758508334
	.long	977423826
	.long	486415851
	.long	3926512018
	.long	1055425077
	.long	481372583
	.long	3222049373
	.long	1954847653
	.long	972831702
	.long	3558056740
	.long	2110850155
	.long	962745166
	.long	2149131451
	.long	3909695307
	.long	1945663404
	.long	2821146184
	.long	4221700311
	.long	1925490332
	.long	3295607
	.long	3524423319
	.long	3891326808
	.long	1347325072
	.long	4148433327
	.long	3850980665
	.long	6591214
	.long	2753879342
	.long	3487686321
	.long	2694650145
	.long	4001899359
	.long	3406994035
	.long	13182429
	.long	1212791388
	.long	2680405347
	.long	1094332995
	.long	3708831422
	.long	2519020775
	.long	26364858
	.long	2425582776
	.long	1065843399
	.long	2188665991
	.long	3122695549
	.long	743074255
	.long	52729717
	.long	556198256
	.long	2131686798
	.long	82364686
	.long	1950423802
	.long	1486148511
	.long	105459434
	.long	1112396512
	.long	4263373596
	.long	164729372
	.long	3900847605
	.long	2972297022
	.long	210918868
	.long	2224793024
	.long	4231779897
	.long	329458745
	.long	3506727914
	.long	1649626749
	.long	421837736
	.long	154618752
	.long	4168592498
	.long	658917491
	.long	2718488532
	.long	3299253499
	.long	843675472
	.long	309237504
	.long	4042217701
	.long	1317834983
	.long	1142009769
	.long	2303539703
	.long	1687350944
	.long	618475008
	.long	3789468107
	.long	2635669967
	.long	2284019538
	.long	312112110
	.long	3374701889
	.long	1236950016
	.long	3283968918
	.long	976372639
	.long	273071781
	.long	624224221
	.long	2454436482
	.long	2473900033
	.long	2272970540
	.long	1952745279
	.long	546143563
	.long	1248448442
	.long	613905668
	.long	652832771
	.long	250973784
	.long	3905490559
	.long	1092287127
	.long	2496896884
	.long	1227811337
	.long	1305665542
	.long	501947569
	.long	3516013822
	.long	2184574254
	.long	698826472
	.long	2455622675
	.long	2611331084
	.long	1003895138
	.long	2737060348
	.long	74181213
	.long	1397652945
	.long	616278055
	.long	927694873
	.long	2007790276
	.long	1179153400
	.long	148362426
	.long	2795305890
	.long	1232556110
	.long	1855389746
	.long	4015580553
	.long	2358306800
	.long	296724853
	.long	1295644484
	.long	2465112221
	.long	3710779492
	.long	3736193810
	.long	421646305
	.long	593449707
	.long	2591288968
	.long	635257146
	.long	3126591689
	.long	3177420325
	.long	843292611
	.long	1186899415
	.long	887610640
	.long	1270514292
	.long	1958216082
	.long	2059873354
	.long	1686585223
	.long	2373798830
	.long	1775221280
	.long	2541028584
	.long	3916432164
	.long	4119746708
	.long	3373170446
	.long	452630365
	.long	3550442561
	.long	787089873
	.long	3537897033
	.long	3944526121
	.long	2451373597
	.long	905260731
	.long	2805917826
	.long	1574179747
	.long	2780826770
	.long	3594084947
	.long	607779899
	.long	1810521462
	.long	1316868356
	.long	3148359494
	.long	1266686244
	.long	2893202598
	.long	1215559799
	.long	3621042925
	.long	2633736712
	.long	2001751692
	.long	2533372489
	.long	1491437901
	.long	2431119599
	.long	2947118554
	.long	972506129
	.long	4003503385
	.long	771777682
	.long	2982875802
	.long	567271902
	.long	1599269812
	.long	1945012259
	.long	3712039474
	.long	1543555365
	.long	1670784308
	.long	1134543805
	.long	3198539624
	.long	3890024518
	.long	3129111652
	.long	3087110731
	.long	3341568617
	.long	2269087610
	.long	2102111953
	.long	3485081741
	.long	1963256009
	.long	1879254167
	.long	2388169939
	.long	243207925
	.long	4204223906
	.long	2675196186
	.long	3926512018
	.long	3758508334
	.long	481372583
	.long	486415851
	.long	4113480516
	.long	1055425077
	.long	3558056740
	.long	3222049373
	.long	962745166
	.long	972831702
	.long	3931993737
	.long	2110850155
	.long	2821146184
	.long	2149131451
	.long	1925490332
	.long	1945663404
	.long	3569020178
	.long	4221700311
	.long	1347325072
	.long	3295607
	.long	3850980665
	.long	3891326808
	.long	2843073060
	.long	4148433327
	.long	2694650145
	.long	6591214
	.long	3406994035
	.long	3487686321
	.long	1391178824
	.long	4001899359
	.long	1094332995
	.long	13182429
	.long	2519020775
	.long	2680405347
	.long	2782357648
	.long	3708831422
	.long	2188665991
	.long	26364858
	.long	743074255
	.long	1065843399
	.long	1269748001
	.long	3122695549
	.long	82364686
	.long	52729717
	.long	1486148511
	.long	2131686798
	.long	2539496002
	.long	1950423802
	.long	164729372
	.long	105459434
	.long	2972297022
	.long	4263373596
	.long	784024708
	.long	3900847605
	.long	329458745
	.long	210918868
	.long	1649626749
	.long	4231779897
	.long	1568049417
	.long	3506727914
	.long	658917491
	.long	421837736
	.long	3299253499
	.long	4168592498
	.long	3136098835
	.long	2718488532
	.long	1317834983
	.long	843675472
	.long	2303539703
	.long	4042217701
	.long	1977230375
	.long	1142009769
	.long	2635669967
	.long	1687350944
	.long	312112110
	.long	3789468107
	.long	3954460750
	.long	2284019538
	.long	976372639
	.long	3374701889
	.long	624224221
	.long	3283968918
	.long	3613954205
	.long	273071781
	.long	1952745279
	.long	2454436482
	.long	1248448442
	.long	2272970540
	.long	2932941114
	.long	546143563
	.long	3905490559
	.long	613905668
	.long	2496896884
	.long	250973784
	.long	1570914932
	.long	1092287127
	.long	3516013822
	.long	1227811337
	.long	698826472
	.long	501947569
	.long	3141829865
	.long	2184574254
	.long	2737060348
	.long	2455622675
	.long	1397652945
	.long	1003895138
	.long	1988692435
	.long	74181213
	.long	1179153400
	.long	616278055
	.long	2795305890
	.long	2007790276
	.long	3977384870
	.long	148362426
	.long	2358306800
	.long	1232556110
	.long	1295644484
	.long	4015580553
	.long	3659802444
	.long	296724853
	.long	421646305
	.long	2465112221
	.long	2591288968
	.long	3736193810
	.long	3024637593
	.long	593449707
	.long	843292611
	.long	635257146
	.long	887610640
	.long	3177420325
	.long	1754307891
	.long	1186899415
	.long	1686585223
	.long	1270514292
	.long	1775221280
	.long	2059873354
	.long	3508615783
	.long	2373798830
	.long	3373170446
	.long	2541028584
	.long	3550442561
	.long	4119746708
	.long	2722264270
	.long	452630365
	.long	2451373597
	.long	787089873
	.long	2805917826
	.long	3944526121
	.long	1149561244
	.long	905260731
	.long	607779899
	.long	1574179747
	.long	1316868356
	.long	3594084947
	.long	2299122488
	.long	1810521462
	.long	1215559799
	.long	3148359494
	.long	2633736712
	.long	2893202598
	.long	303277681
	.long	3621042925
	.long	2431119599
	.long	2001751692
	.long	972506129
	.long	1491437901
	.long	606555363
	.long	2947118554
	.long	567271902
	.long	4003503385
	.long	1945012259
	.long	2982875802
	.long	1213110727
	.long	1599269812
	.long	1134543805
	.long	3712039474
	.long	3890024518
	.long	1670784308
	.long	2426221454
	.long	3198539624
	.long	2269087610
	.long	3129111652
	.long	3485081741
	.long	3341568617
	.long	557475612
	.long	2102111953
	.long	243207925
	.long	1963256009
	.long	2675196186
	.long	2388169939
	.long	1114951224
	.long	4204223906
	.long	486415851
	.long	3926512018
	.long	1055425077
	.long	481372583
	.long	2229902448
	.long	4113480516
	.long	972831702
	.long	3558056740
	.long	2110850155
	.long	962745166
	.long	164837600
	.long	3931993737
	.long	1945663404
	.long	2821146184
	.long	4221700311
	.long	1925490332
	.long	329675200
	.long	3569020178
	.long	3891326808
	.long	1347325072
	.long	4148433327
	.long	3850980665
	.long	659350401
	.long	2843073060
	.long	3487686321
	.long	2694650145
	.long	4001899359
	.long	3406994035
	.long	1318700802
	.long	1391178824
	.long	2680405347
	.long	1094332995
	.long	3708831422
	.long	2519020775
	.long	2637401604
	.long	2782357648
	.long	1065843399
	.long	2188665991
	.long	3122695549
	.long	743074255
	.long	979835913
	.long	1269748001
	.long	2131686798
	.long	82364686
	.long	1950423802
	.long	1486148511
	.long	1959671827
	.long	2539496002
	.long	4263373596
	.long	164729372
	.long	3900847605
	.long	2972297022
	.long	3919343654
	.long	784024708
	.long	4231779897
	.long	329458745
	.long	3506727914
	.long	1649626749
	.long	3543720013
	.long	1568049417
	.long	4168592498
	.long	658917491
	.long	2718488532
	.long	3299253499
	.long	2792472730
	.long	3136098835
	.long	4042217701
	.long	1317834983
	.long	1142009769
	.long	2303539703
	.long	1289978165
	.long	1977230375
	.long	3789468107
	.long	2635669967
	.long	2284019538
	.long	312112110
	.long	2579956331
	.long	3954460750
	.long	3283968918
	.long	976372639
	.long	273071781
	.long	624224221
	.long	864945366
	.long	3613954205
	.long	2272970540
	.long	1952745279
	.long	546143563
	.long	1248448442
	.long	1729890733
	.long	2932941114
	.long	250973784
	.long	3905490559
	.long	1092287127
	.long	2496896884
	.long	3459781466
	.long	1570914932
	.long	501947569
	.long	3516013822
	.long	2184574254
	.long	698826472
	.long	2624595636
	.long	3141829865
	.long	1003895138
	.long	2737060348
	.long	74181213
	.long	1397652945
	.long	954223976
	.long	1988692435
	.long	2007790276
	.long	1179153400
	.long	148362426
	.long	2795305890
	.long	1908447953
	.long	3977384870
	.long	4015580553
	.long	2358306800
	.long	296724853
	.long	1295644484
	.long	3816895906
	.long	3659802444
	.long	3736193810
	.long	421646305
	.long	593449707
	.long	2591288968
	.long	3338824517
	.long	3024637593
	.long	3177420325
	.long	843292611
	.long	1186899415
	.long	887610640
	.long	2382681739
	.long	1754307891
	.long	2059873354
	.long	1686585223
	.long	2373798830
	.long	1775221280
	.long	470396183
	.long	3508615783
	.long	4119746708
	.long	3373170446
	.long	452630365
	.long	3550442561
	.long	940792367
	.long	2722264270
	.long	3944526121
	.long	2451373597
	.long	905260731
	.long	2805917826
	.long	1881584735
	.long	1149561244
	.long	3594084947
	.long	607779899
	.long	1810521462
	.long	1316868356
	.long	3763169470
	.long	2299122488
	.long	2893202598
	.long	1215559799
	.long	3621042925
	.long	2633736712
	.long	3231371645
	.long	303277681
	.long	1491437901
	.long	2431119599
	.long	2947118554
	.long	972506129
	.long	2167775995
	.long	606555363
	.long	2982875802
	.long	567271902
	.long	1599269812
	.long	1945012259
	.long	40584695
	.long	1213110727
	.long	1670784308
	.long	1134543805
	.long	3198539624
	.long	3890024518
	.long	81169391
	.long	2426221454
	.long	3341568617
	.long	2269087610
	.long	2102111953
	.long	3485081741
	.long	162338783
	.long	557475612
	.long	2388169939
	.long	243207925
	.long	4204223906
	.long	2675196186
	.long	324677567
	.long	1114951224
	.long	481372583
	.long	486415851
	.long	4113480516
	.long	1055425077
	.long	649355134
	.long	2229902448
	.long	962745166
	.long	972831702
	.long	3931993737
	.long	2110850155
	.long	1298710268
	.long	164837600
	.long	1925490332
	.long	1945663404
	.long	3569020178
	.long	4221700311
	.long	2597420537
	.long	329675200
	.long	3850980665
	.long	3891326808
	.long	2843073060
	.long	4148433327
	.long	899873778
	.long	659350401
	.long	3406994035
	.long	3487686321
	.long	1391178824
	.long	4001899359
	.long	1799747556
	.long	1318700802
	.long	2519020775
	.long	2680405347
	.long	2782357648
	.long	3708831422
	.long	3599495112
	.long	2637401604
	.long	743074255
	.long	1065843399
	.long	1269748001
	.long	3122695549
	.long	2904022928
	.long	979835913
	.long	1486148511
	.long	2131686798
	.long	2539496002
	.long	1950423802
	.long	1513078560
	.long	1959671827
	.long	2972297022
	.long	4263373596
	.long	784024708
	.long	3900847605
	.long	3026157121
	.long	3919343654
	.long	1649626749
	.long	4231779897
	.long	1568049417
	.long	3506727914
	.long	1757346946
	.long	3543720013
	.long	3299253499
	.long	4168592498
	.long	3136098835
	.long	2718488532
	.long	3514693892
	.long	2792472730
	.long	2303539703
	.long	4042217701
	.long	1977230375
	.long	1142009769
	.long	2734420489
	.long	1289978165
	.long	312112110
	.long	3789468107
	.long	3954460750
	.long	2284019538
	.long	1173873683
	.long	2579956331
	.long	624224221
	.long	3283968918
	.long	3613954205
	.long	273071781
	.long	2347747367
	.long	864945366
	.long	1248448442
	.long	2272970540
	.long	2932941114
	.long	546143563
	.long	400527438
	.long	1729890733
	.long	2496896884
	.long	250973784
	.long	1570914932
	.long	1092287127
	.long	801054876
	.long	3459781466
	.long	698826472
	.long	501947569
	.long	3141829865
	.long	2184574254
	.long	1602109753
	.long	2624595636
	.long	1397652945
	.long	1003895138
	.long	1988692435
	.long	74181213
	.long	3204219507
	.long	954223976
	.long	2795305890
	.long	2007790276
	.long	3977384870
	.long	148362426
	.long	2113471718
	.long	1908447953
	.long	1295644484
	.long	4015580553
	.long	3659802444
	.long	296724853
	.long	4226943436
	.long	3816895906
	.long	2591288968
	.long	3736193810
	.long	3024637593
	.long	593449707
	.long	4158919577
	.long	3338824517
	.long	887610640
	.long	3177420325
	.long	1754307891
	.long	1186899415
	.long	4022871858
	.long	2382681739
	.long	1775221280
	.long	2059873354
	.long	3508615783
	.long	2373798830
	.long	3750776420
	.long	470396183
	.long	3550442561
	.long	4119746708
	.long	2722264270
	.long	452630365
	.long	3206585544
	.long	940792367
	.long	2805917826
	.long	3944526121
	.long	1149561244
	.long	905260731
	.long	2118203793
	.long	1881584735
	.long	1316868356
	.long	3594084947
	.long	2299122488
	.long	1810521462
	.long	4236407587
	.long	3763169470
	.long	2633736712
	.long	2893202598
	.long	303277681
	.long	3621042925
	.long	4177847879
	.long	3231371645
	.long	972506129
	.long	1491437901
	.long	606555363
	.long	2947118554
	.long	4060728462
	.long	2167775995
	.long	1945012259
	.long	2982875802
	.long	1213110727
	.long	1599269812
	.long	3826489629
	.long	40584695
	.long	3890024518
	.long	1670784308
	.long	2426221454
	.long	3198539624
	.long	3358011962
	.long	81169391
	.long	3485081741
	.long	3341568617
	.long	557475612
	.long	2102111953
	.long	2421056629
	.long	162338783
	.long	2675196186
	.long	2388169939
	.long	1114951224
	.long	4204223906
	.long	547145963
	.long	324677567
	.long	1055425077
	.long	481372583
	.long	2229902448
	.long	4113480516
	.long	1094291926
	.long	649355134
	.long	2110850155
	.long	962745166
	.long	164837600
	.long	3931993737
	.long	2188583852
	.long	1298710268
	.long	4221700311
	.long	1925490332
	.long	329675200
	.long	3569020178
	.long	82200408
	.long	2597420537
	.long	4148433327
	.long	3850980665
	.long	659350401
	.long	2843073060
	.long	164400817
	.long	899873778
	.long	4001899359
	.long	3406994035
	.long	1318700802
	.long	1391178824
	.long	328801635
	.long	1799747556
	.long	3708831422
	.long	2519020775
	.long	2637401604
	.long	2782357648
	.long	657603271
	.long	3599495112
	.long	3122695549
	.long	743074255
	.long	979835913
	.long	1269748001
	.long	1315206542
	.long	2904022928
	.long	1950423802
	.long	1486148511
	.long	1959671827
	.long	2539496002
	.long	2630413084
	.long	1513078560
	.long	3900847605
	.long	2972297022
	.long	3919343654
	.long	784024708
	.long	965858873
	.long	3026157121
	.long	3506727914
	.long	1649626749
	.long	3543720013
	.long	1568049417
	.long	1931717747
	.long	1757346946
	.long	2718488532
	.long	3299253499
	.long	2792472730
	.long	3136098835
	.long	3863435494
	.long	3514693892
	.long	1142009769
	.long	2303539703
	.long	1289978165
	.long	1977230375
	.long	3431903692
	.long	2734420489
	.long	2284019538
	.long	312112110
	.long	2579956331
	.long	3954460750
	.long	2568840088
	.long	1173873683
	.long	273071781
	.long	624224221
	.long	864945366
	.long	3613954205
	.long	842712880
	.long	2347747367
	.long	546143563
	.long	1248448442
	.long	1729890733
	.long	2932941114
	.long	1685425760
	.long	400527438
	.long	1092287127
	.long	2496896884
	.long	3459781466
	.long	1570914932
	.long	3370851521
	.long	801054876
	.long	2184574254
	.long	698826472
	.long	2624595636
	.long	3141829865
	.long	2446735747
	.long	1602109753
	.long	74181213
	.long	1397652945
	.long	954223976
	.long	1988692435
	.long	598504198
	.long	3204219507
	.long	148362426
	.long	2795305890
	.long	1908447953
	.long	3977384870
	.long	1197008397
	.long	2113471718
	.long	296724853
	.long	1295644484
	.long	3816895906
	.long	3659802444
	.long	2394016794
	.long	4226943436
	.long	593449707
	.long	2591288968
	.long	3338824517
	.long	3024637593
	.long	493066293
	.long	4158919577
	.long	1186899415
	.long	887610640
	.long	2382681739
	.long	1754307891
	.long	986132586
	.long	4022871858
	.long	2373798830
	.long	1775221280
	.long	470396183
	.long	3508615783
	.long	1972265172
	.long	3750776420
	.long	452630365
	.long	3550442561
	.long	940792367
	.long	2722264270
	.long	3944530345
	.long	3206585544
	.long	905260731
	.long	2805917826
	.long	1881584735
	.long	1149561244
	.long	3594093395
	.long	2118203793
	.long	1810521462
	.long	1316868356
	.long	3763169470
	.long	2299122488
	.long	2893219494
	.long	4236407587
	.long	3621042925
	.long	2633736712
	.long	3231371645
	.long	303277681
	.long	1491471692
	.long	4177847879
	.long	2947118554
	.long	972506129
	.long	2167775995
	.long	606555363
	.long	2982943385
	.long	4060728462
	.long	1599269812
	.long	1945012259
	.long	40584695
	.long	1213110727
	.long	1670919475
	.long	3826489629
	.long	3198539624
	.long	3890024518
	.long	81169391
	.long	2426221454
	.long	3341838951
	.long	3358011962
	.long	2102111953
	.long	3485081741
	.long	162338783
	.long	557475612
	.long	2388710606
	.long	2421056629
	.long	4204223906
	.long	2675196186
	.long	324677567
	.long	1114951224
	.long	482453916
	.long	547145963
	.long	4113480516
	.long	1055425077
	.long	649355134
	.long	2229902448
	.long	964907833
	.long	1094291926
	.long	3931993737
	.long	2110850155
	.long	1298710268
	.long	164837600
	.long	1929815667
	.long	2188583852
	.long	3569020178
	.long	4221700311
	.long	2597420537
	.long	329675200
	.long	3859631335
	.long	82200408
	.long	2843073060
	.long	4148433327
	.long	899873778
	.long	659350401
	.long	3424295375
	.long	164400817
	.long	1391178824
	.long	4001899359
	.long	1799747556
	.long	1318700802
	.long	2553623455
	.long	328801635
	.long	2782357648
	.long	3708831422
	.long	3599495112
	.long	2637401604
	.long	812279614
	.long	657603271
	.long	1269748001
	.long	3122695549
	.long	2904022928
	.long	979835913
	.long	1624559229
	.long	1315206542
	.long	2539496002
	.long	1950423802
	.long	1513078560
	.long	1959671827
	.long	3249118458
	.long	2630413084
	.long	784024708
	.long	3900847605
	.long	3026157121
	.long	3919343654
	.long	2203269620
	.long	965858873
	.long	1568049417
	.long	3506727914
	.long	1757346946
	.long	3543720013
	.long	111571945
	.long	1931717747
	.long	3136098835
	.long	2718488532
	.long	3514693892
	.long	2792472730
	.long	223143890
	.long	3863435494
	.long	1977230375
	.long	1142009769
	.long	2734420489
	.long	1289978165
	.long	446287780
	.long	3431903692
	.long	3954460750
	.long	2284019538
	.long	1173873683
	.long	2579956331
	.long	892575561
	.long	2568840088
	.long	3613954205
	.long	273071781
	.long	2347747367
	.long	864945366
	.long	1785151123
	.long	842712880
	.long	2932941114
	.long	546143563
	.long	400527438
	.long	1729890733
	.long	3570302247
	.long	1685425760
	.long	1570914932
	.long	1092287127
	.long	801054876
	.long	3459781466
	.long	2845637198
	.long	3370851521
	.long	3141829865
	.long	2184574254
	.long	1602109753
	.long	2624595636
	.long	1396307100
	.long	2446735747
	.long	1988692435
	.long	74181213
	.long	3204219507
	.long	954223976
	.long	2792614201
	.long	598504198
	.long	3977384870
	.long	148362426
	.long	2113471718
	.long	1908447953
	.long	1290261106
	.long	1197008397
	.long	3659802444
	.long	296724853
	.long	4226943436
	.long	3816895906
	.long	2580522212
	.long	2394016794
	.long	3024637593
	.long	593449707
	.long	4158919577
	.long	3338824517
	.long	866077128
	.long	493066293
	.long	1754307891
	.long	1186899415
	.long	4022871858
	.long	2382681739
	.long	1732154256
	.long	986132586
	.long	3508615783
	.long	2373798830
	.long	3750776420
	.long	470396183
	.long	3464308513
	.long	1972265172
	.long	2722264270
	.long	452630365
	.long	3206585544
	.long	940792367
	.long	2633649730
	.long	3944530345
	.long	1149561244
	.long	905260731
	.long	2118203793
	.long	1881584735
	.long	972332164
	.long	3594093395
	.long	2299122488
	.long	1810521462
	.long	4236407587
	.long	3763169470
	.long	1944664328
	.long	2893219494
	.long	303277681
	.long	3621042925
	.long	4177847879
	.long	3231371645
	.long	3889328657
	.long	1491471692
	.long	606555363
	.long	2947118554
	.long	4060728462
	.long	2167775995
	.long	3483690018
	.long	2982943385
	.long	1213110727
	.long	1599269812
	.long	3826489629
	.long	40584695
	.long	2672412741
	.long	1670919475
	.long	2426221454
	.long	3198539624
	.long	3358011962
	.long	81169391
	.long	1049858187
	.long	3341838951
	.long	557475612
	.long	2102111953
	.long	2421056629
	.long	162338783
	.long	2099716375
	.long	2388710606
	.long	1114951224
	.long	4204223906
	.long	547145963
	.long	324677567
	.long	4199432751
	.long	482453916
	.long	2229902448
	.long	4113480516
	.long	1094291926
	.long	649355134
	.long	4103898207
	.long	964907833
	.long	164837600
	.long	3931993737
	.long	2188583852
	.long	1298710268
	.long	3912829119
	.long	1929815667
	.long	329675200
	.long	3569020178
	.long	82200408
	.long	2597420537
	.long	3530690942
	.long	3859631335
	.long	659350401
	.long	2843073060
	.long	164400817
	.long	899873778
	.long	2766414588
	.long	3424295375
	.long	1318700802
	.long	1391178824
	.long	328801635
	.long	1799747556
	.long	1237861880
	.long	2553623455
	.long	2637401604
	.long	2782357648
	.long	657603271
	.long	3599495112
	.long	2475723761
	.long	812279614
	.long	979835913
	.long	1269748001
	.long	1315206542
	.long	2904022928
	.long	656480226
	.long	1624559229
	.long	1959671827
	.long	2539496002
	.long	2630413084
	.long	1513078560
	.long	1312960453
	.long	3249118458
	.long	3919343654
	.long	784024708
	.long	965858873
	.long	3026157121
	.long	2625920907
	.long	2203269620
	.long	3543720013
	.long	1568049417
	.long	1931717747
	.long	1757346946
	.long	956874519
	.long	111571945
	.long	2792472730
	.long	3136098835
	.long	3863435494
	.long	3514693892
	.long	1913749038
	.long	223143890
	.long	1289978165
	.long	1977230375
	.long	3431903692
	.long	2734420489
	.long	3827498077
	.long	446287780
	.long	2579956331
	.long	3954460750
	.long	2568840088
	.long	1173873683
	.long	3360028859
	.long	892575561
	.long	864945366
	.long	3613954205
	.long	842712880
	.long	2347747367
	.long	2425090423
	.long	1785151123
	.long	1729890733
	.long	2932941114
	.long	1685425760
	.long	400527438
	.long	555213551
	.long	3570302247
	.long	3459781466
	.long	1570914932
	.long	3370851521
	.long	801054876
	.long	1110427102
	.long	2845637198
	.long	2624595636
	.long	3141829865
	.long	2446735747
	.long	1602109753
	.long	2220854205
	.long	1396307100
	.long	954223976
	.long	1988692435
	.long	598504198
	.long	3204219507
	.long	146741115
	.long	2792614201
	.long	1908447953
	.long	3977384870
	.long	1197008397
	.long	2113471718
	.long	293482231
	.long	1290261106
	.long	3816895906
	.long	3659802444
	.long	2394016794
	.long	4226943436
	.long	586964463
	.long	2580522212
	.long	3338824517
	.long	3024637593
	.long	493066293
	.long	4158919577
	.long	1173928927
	.long	866077128
	.long	2382681739
	.long	1754307891
	.long	986132586
	.long	4022871858
	.long	2347857855
	.long	1732154256
	.long	470396183
	.long	3508615783
	.long	1972265172
	.long	3750776420
	.long	400748414
	.long	3464308513
	.long	940792367
	.long	2722264270
	.long	3944530345
	.long	3206585544
	.long	801496828
	.long	2633649730
	.long	1881584735
	.long	1149561244
	.long	3594093395
	.long	2118203793
	.long	1602993657
	.long	972332164
	.long	3763169470
	.long	2299122488
	.long	2893219494
	.long	4236407587
	.long	3205987314
	.long	1944664328
	.long	3231371645
	.long	303277681
	.long	1491471692
	.long	4177847879
	.long	2117007332
	.long	3889328657
	.long	2167775995
	.long	606555363
	.long	2982943385
	.long	4060728462
	.long	4234014665
	.long	3483690018
	.long	40584695
	.long	1213110727
	.long	1670919475
	.long	3826489629
	.long	4173062034
	.long	2672412741
	.long	81169391
	.long	2426221454
	.long	3341838951
	.long	3358011962
	.long	4051156773
	.long	1049858187
	.long	162338783
	.long	557475612
	.long	2388710606
	.long	2421056629
	.long	3807346250
	.long	2099716375
	.long	324677567
	.long	1114951224
	.long	482453916
	.long	547145963
	.long	3319725204
	.long	4199432751
	.long	649355134
	.long	2229902448
	.long	964907833
	.long	1094291926
	.long	2344483112
	.long	4103898207
	.long	1298710268
	.long	164837600
	.long	1929815667
	.long	2188583852
	.long	393998928
	.long	3912829119
	.long	2597420537
	.long	329675200
	.long	3859631335
	.long	82200408
	.long	787997856
	.long	3530690942
	.long	899873778
	.long	659350401
	.long	3424295375
	.long	164400817
	.long	1575995713
	.long	2766414588
	.long	1799747556
	.long	1318700802
	.long	2553623455
	.long	328801635
	.long	3151991427
	.long	1237861880
	.long	3599495112
	.long	2637401604
	.long	812279614
	.long	657603271
	.long	2009015559
	.long	2475723761
	.long	2904022928
	.long	979835913
	.long	1624559229
	.long	1315206542
	.long	4018031118
	.long	656480226
	.long	1513078560
	.long	1959671827
	.long	3249118458
	.long	2630413084
	.long	3741094941
	.long	1312960453
	.long	3026157121
	.long	3919343654
	.long	2203269620
	.long	965858873
	.long	3187222587
	.long	2625920907
	.long	1757346946
	.long	3543720013
	.long	111571945
	.long	1931717747
	.long	2079477878
	.long	956874519
	.long	3514693892
	.long	2792472730
	.long	223143890
	.long	3863435494
	.long	4158955756
	.long	1913749038
	.long	2734420489
	.long	1289978165
	.long	446287780
	.long	3431903692
	.long	4022944216
	.long	3827498077
	.long	1173873683
	.long	2579956331
	.long	892575561
	.long	2568840088
	.long	3750921137
	.long	3360028859
	.long	2347747367
	.long	864945366
	.long	1785151123
	.long	842712880
	.long	3206874979
	.long	2425090423
	.long	400527438
	.long	1729890733
	.long	3570302247
	.long	1685425760
	.long	2118782663
	.long	555213551
	.long	801054876
	.long	3459781466
	.long	2845637198
	.long	3370851521
	.long	4237565327
	.long	1110427102
	.long	1602109753
	.long	2624595636
	.long	1396307100
	.long	2446735747
	.long	4180163359
	.long	2220854205
	.long	3204219507
	.long	954223976
	.long	2792614201
	.long	598504198
	.long	4065359423
	.long	146741115
	.long	2113471718
	.long	1908447953
	.long	1290261106
	.long	1197008397
	.long	3835751551
	.long	293482231
	.long	4226943436
	.long	3816895906
	.long	2580522212
	.long	2394016794
	.long	3376535807
	.long	586964463
	.long	4158919577
	.long	3338824517
	.long	866077128
	.long	493066293
	.long	2458104319
	.long	1173928927
	.long	4022871858
	.long	2382681739
	.long	1732154256
	.long	986132586
	.long	621241343
	.long	2347857855
	.long	3750776420
	.long	470396183
	.long	3464308513
	.long	1972265172
	.long	1242482686
	.long	400748414
	.long	3206585544
	.long	940792367
	.long	2633649730
	.long	3944530345
	.long	2484965372
	.long	801496828
	.long	2118203793
	.long	1881584735
	.long	972332164
	.long	3594093395
	.long	674963448
	.long	1602993657
	.long	4236407587
	.long	3763169470
	.long	1944664328
	.long	2893219494
	.long	1349926897
	.long	3205987314
	.long	4177847879
	.long	3231371645
	.long	3889328657
	.long	1491471692
	.long	2699853794
	.long	2117007332
	.long	4060728462
	.long	2167775995
	.long	3483690018
	.long	2982943385
	.long	1104740292
	.long	4234014665
	.long	3826489629
	.long	40584695
	.long	2672412741
	.long	1670919475
	.long	2209480585
	.long	4173062034
	.long	3358011962
	.long	81169391
	.long	1049858187
	.long	3341838951
	.long	123993874
	.long	4051156773
	.long	2421056629
	.long	162338783
	.long	2099716375
	.long	2388710606
	.long	247987749
	.long	3807346250
	.long	547145963
	.long	324677567
	.long	4199432751
	.long	482453916
	.long	495975499
	.long	3319725204
	.long	1094291926
	.long	649355134
	.long	4103898207
	.long	964907833
	.long	991950999
	.long	2344483112
	.long	2188583852
	.long	1298710268
	.long	3912829119
	.long	1929815667
	.long	1983901999
	.long	393998928
	.long	82200408
	.long	2597420537
	.long	3530690942
	.long	3859631335
	.long	3967803999
	.long	787997856
	.long	164400817
	.long	899873778
	.long	2766414588
	.long	3424295375
	.long	3640640703
	.long	1575995713
	.long	328801635
	.long	1799747556
	.long	1237861880
	.long	2553623455
	.long	2986314111
	.long	3151991427
	.long	657603271
	.long	3599495112
	.long	2475723761
	.long	812279614
	.long	1677660927
	.long	2009015559
	.long	1315206542
	.long	2904022928
	.long	656480226
	.long	1624559229
	.long	3355321855
	.long	4018031118
	.long	2630413084
	.long	1513078560
	.long	1312960453
	.long	3249118458
	.long	2415676415
	.long	3741094941
	.long	965858873
	.long	3026157121
	.long	2625920907
	.long	2203269620
	.long	536385535
	.long	3187222587
	.long	1931717747
	.long	1757346946
	.long	956874519
	.long	111571945
	.long	1072771071
	.long	2079477878
	.long	3863435494
	.long	3514693892
	.long	1913749038
	.long	223143890
	.long	2145542143
	.long	4158955756
	.long	3431903692
	.long	2734420489
	.long	3827498077
	.long	446287780
	.long	4291084286
	.long	4022944216
	.long	2568840088
	.long	1173873683
	.long	3360028859
	.long	892575561
	.long	4287201277
	.long	3750921137
	.long	842712880
	.long	2347747367
	.long	2425090423
	.long	1785151123
	.long	4279435259
	.long	3206874979
	.long	1685425760
	.long	400527438
	.long	555213551
	.long	3570302247
	.long	4263903223
	.long	2118782663
	.long	3370851521
	.long	801054876
	.long	1110427102
	.long	2845637198
	.long	4232839151
	.long	4237565327
	.long	2446735747
	.long	1602109753
	.long	2220854205
	.long	1396307100
	.long	4170711006
	.long	4180163359
	.long	598504198
	.long	3204219507
	.long	146741115
	.long	2792614201
	.long	4046454716
	.long	4065359423
	.long	1197008397
	.long	2113471718
	.long	293482231
	.long	1290261106
	.long	3797942136
	.long	3835751551
	.long	2394016794
	.long	4226943436
	.long	586964463
	.long	2580522212
	.long	3300916976
	.long	3376535807
	.long	493066293
	.long	4158919577
	.long	1173928927
	.long	866077128
	.long	2306866656
	.long	2458104319
	.long	986132586
	.long	4022871858
	.long	2347857855
	.long	1732154256
	.long	318766016
	.long	621241343
	.long	1972265172
	.long	3750776420
	.long	400748414
	.long	3464308513
	.long	637532033
	.long	1242482686
	.long	3944530345
	.long	3206585544
	.long	801496828
	.long	2633649730
	.long	1275064066
	.long	2484965372
	.long	3594093395
	.long	2118203793
	.long	1602993657
	.long	972332164
	.long	2550128133
	.long	674963448
	.long	2893219494
	.long	4236407587
	.long	3205987314
	.long	1944664328
	.long	805288971
	.long	1349926897
	.long	1491471692
	.long	4177847879
	.long	2117007332
	.long	3889328657
	.long	1610577942
	.long	2699853794
	.long	2982943385
	.long	4060728462
	.long	4234014665
	.long	3483690018
	.long	3221155884
	.long	1104740292
	.long	1670919475
	.long	3826489629
	.long	4173062034
	.long	2672412741
	.long	2147344473
	.long	2209480585
	.long	3341838951
	.long	3358011962
	.long	4051156773
	.long	1049858187
	.long	4294688947
	.long	123993874
	.long	2388710606
	.long	2421056629
	.long	3807346250
	.long	2099716375
	.long	4294410598
	.long	247987749
	.long	482453916
	.long	547145963
	.long	3319725204
	.long	4199432751
	.long	4293853900
	.long	495975499
	.long	964907833
	.long	1094291926
	.long	2344483112
	.long	4103898207
	.long	4292740504
	.long	991950999
	.long	1929815667
	.long	2188583852
	.long	393998928
	.long	3912829119
	.long	4290513712
	.long	1983901999
	.long	3859631335
	.long	82200408
	.long	787997856
	.long	3530690942
	.long	4286060128
	.long	3967803999
	.long	3424295375
	.long	164400817
	.long	1575995713
	.long	2766414588
	.long	4277152960
	.long	3640640703
	.long	2553623455
	.long	328801635
	.long	3151991427
	.long	1237861880
	.long	4259338624
	.long	2986314111
	.long	812279614
	.long	657603271
	.long	2009015559
	.long	2475723761
	.long	4223709953
	.long	1677660927
	.long	1624559229
	.long	1315206542
	.long	4018031118
	.long	656480226
	.long	4152452611
	.long	3355321855
	.long	3249118458
	.long	2630413084
	.long	3741094941
	.long	1312960453
	.long	4009937927
	.long	2415676415
	.long	2203269620
	.long	965858873
	.long	3187222587
	.long	2625920907
	.long	3724908559
	.long	536385535
	.long	111571945
	.long	1931717747
	.long	2079477878
	.long	956874519
	.long	3154849823
	.long	1072771071
	.long	223143890
	.long	3863435494
	.long	4158955756
	.long	1913749038
	.long	2014732351
	.long	2145542143
	.long	446287780
	.long	3431903692
	.long	4022944216
	.long	3827498077
	.long	4029464703
	.long	4291084286
	.long	892575561
	.long	2568840088
	.long	3750921137
	.long	3360028859
	.long	3763962110
	.long	4287201277
	.long	1785151123
	.long	842712880
	.long	3206874979
	.long	2425090423
	.long	3232956925
	.long	4279435259
	.long	3570302247
	.long	1685425760
	.long	2118782663
	.long	555213551
	.long	2170946555
	.long	4263903223
	.long	2845637198
	.long	3370851521
	.long	4237565327
	.long	1110427102
	.long	46925815
	.long	4232839151
	.long	1396307100
	.long	2446735747
	.long	4180163359
	.long	2220854205
	.long	93851631
	.long	4170711006
	.long	2792614201
	.long	598504198
	.long	4065359423
	.long	146741115
	.long	187703262
	.long	4046454716
	.long	1290261106
	.long	1197008397
	.long	3835751551
	.long	293482231
	.long	375406524
	.long	3797942136
	.long	2580522212
	.long	2394016794
	.long	3376535807
	.long	586964463
	.long	750813049
	.long	3300916976
	.long	866077128
	.long	493066293
	.long	2458104319
	.long	1173928927
	.long	1501626098
	.long	2306866656
	.long	1732154256
	.long	986132586
	.long	621241343
	.long	2347857855
	.long	3003252197
	.long	318766016
	.long	3464308513
	.long	1972265172
	.long	1242482686
	.long	400748414
	.long	1711537099
	.long	637532033
	.long	2633649730
	.long	3944530345
	.long	2484965372
	.long	801496828
	.long	3423074199
	.long	1275064066
	.long	972332164
	.long	3594093395
	.long	674963448
	.long	1602993657
	.long	2551181103
	.long	2550128133
	.long	1944664328
	.long	2893219494
	.long	1349926897
	.long	3205987314
	.long	807394910
	.long	805288971
	.long	3889328657
	.long	1491471692
	.long	2699853794
	.long	2117007332
	.long	1614789820
	.long	1610577942
	.long	3483690018
	.long	2982943385
	.long	1104740292
	.long	4234014665
	.long	3229579640
	.long	3221155884
	.long	2672412741
	.long	1670919475
	.long	2209480585
	.long	4173062034
	.long	2164191985
	.long	2147344473
	.long	1049858187
	.long	3341838951
	.long	123993874
	.long	4051156773
	.long	33416674
	.long	4294688947
	.long	2099716375
	.long	2388710606
	.long	247987749
	.long	3807346250
	.long	66833348
	.long	4294410598
	.long	4199432751
	.long	482453916
	.long	495975499
	.long	3319725204
	.long	133666696
	.long	4293853900
	.long	4103898207
	.long	964907833
	.long	991950999
	.long	2344483112
	.long	267333393
	.long	4292740504
	.long	3912829119
	.long	1929815667
	.long	1983901999
	.long	393998928
	.long	534666787
	.long	4290513712
	.long	3530690942
	.long	3859631335
	.long	3967803999
	.long	787997856
	.long	1069333574
	.long	4286060128
	.long	2766414588
	.long	3424295375
	.long	3640640703
	.long	1575995713
	.long	2138667148
	.long	4277152960
	.long	1237861880
	.long	2553623455
	.long	2986314111
	.long	3151991427
	.long	4277334296
	.long	4259338624
	.long	2475723761
	.long	812279614
	.long	1677660927
	.long	2009015559
	.long	4259701297
	.long	4223709953
	.long	656480226
	.long	1624559229
	.long	3355321855
	.long	4018031118
	.long	4224435298
	.long	4152452611
	.long	1312960453
	.long	3249118458
	.long	2415676415
	.long	3741094941
	.long	4153903301
	.long	4009937927
	.long	2625920907
	.long	2203269620
	.long	536385535
	.long	3187222587
	.long	4012839307
	.long	3724908559
	.long	956874519
	.long	111571945
	.long	1072771071
	.long	2079477878
	.long	3730711318
	.long	3154849823
	.long	1913749038
	.long	223143890
	.long	2145542143
	.long	4158955756
	.long	3166455341
	.long	2014732351
	.long	3827498077
	.long	446287780
	.long	4291084286
	.long	4022944216
	.long	2037943386
	.long	4029464703
	.long	3360028859
	.long	892575561
	.long	4287201277
	.long	3750921137
	.long	4075886773
	.long	3763962110
	.long	2425090423
	.long	1785151123
	.long	4279435259
	.long	3206874979
	.long	3856806251
	.long	3232956925
	.long	555213551
	.long	3570302247
	.long	4263903223
	.long	2118782663
	.long	3418645206
	.long	2170946555
	.long	1110427102
	.long	2845637198
	.long	4232839151
	.long	4237565327
	.long	2542323117
	.long	46925815
	.long	2220854205
	.long	1396307100
	.long	4170711006
	.long	4180163359
	.long	789678938
	.long	93851631
	.long	146741115
	.long	2792614201
	.long	4046454716
	.long	4065359423
	.long	1579357876
	.long	187703262
	.long	293482231
	.long	1290261106
	.long	3797942136
	.long	3835751551
	.long	3158715752
	.long	375406524
	.long	586964463
	.long	2580522212
	.long	3300916976
	.long	3376535807
	.long	2022464208
	.long	750813049
	.long	1173928927
	.long	866077128
	.long	2306866656
	.long	2458104319
	.long	4044928416
	.long	1501626098
	.long	2347857855
	.long	1732154256
	.long	318766016
	.long	621241343
	.long	3794889537
	.long	3003252197
	.long	400748414
	.long	3464308513
	.long	637532033
	.long	1242482686
	.long	3294811778
	.long	1711537099
	.long	801496828
	.long	2633649730
	.long	1275064066
	.long	2484965372
	.long	2294656261
	.long	3423074199
	.long	1602993657
	.long	972332164
	.long	2550128133
	.long	674963448
	.long	294345226
	.long	2551181103
	.long	3205987314
	.long	1944664328
	.long	805288971
	.long	1349926897
	.long	588690452
	.long	807394910
	.long	2117007332
	.long	3889328657
	.long	1610577942
	.long	2699853794
	.long	1177380905
	.long	1614789820
	.long	4234014665
	.long	3483690018
	.long	3221155884
	.long	1104740292
	.long	2354761811
	.long	3229579640
	.long	4173062034
	.long	2672412741
	.long	2147344473
	.long	2209480585
	.long	414556326
	.long	2164191985
	.long	4051156773
	.long	1049858187
	.long	4294688947
	.long	123993874
	.long	829112653
	.long	33416674
	.long	3807346250
	.long	2099716375
	.long	4294410598
	.long	247987749
	.long	1658225307
	.long	66833348
	.long	3319725204
	.long	4199432751
	.long	4293853900
	.long	495975499
	.long	3316450614
	.long	133666696
	.long	2344483112
	.long	4103898207
	.long	4292740504
	.long	991950999
	.long	2337933933
	.long	267333393
	.long	393998928
	.long	3912829119
	.long	4290513712
	.long	1983901999
	.long	380900570
	.long	534666787
	.long	787997856
	.long	3530690942
	.long	4286060128
	.long	3967803999
	.long	761801140
	.long	1069333574
	.long	1575995713
	.long	2766414588
	.long	4277152960
	.long	3640640703
	.long	1523602280
	.long	2138667148
	.long	3151991427
	.long	1237861880
	.long	4259338624
	.long	2986314111
	.long	3047204561
	.long	4277334296
	.long	2009015559
	.long	2475723761
	.long	4223709953
	.long	1677660927
	.long	1799441827
	.long	4259701297
	.long	4018031118
	.long	656480226
	.long	4152452611
	.long	3355321855
	.long	3598883655
	.long	4224435298
	.long	3741094941
	.long	1312960453
	.long	4009937927
	.long	2415676415
	.long	2902800015
	.long	4153903301
	.long	3187222587
	.long	2625920907
	.long	3724908559
	.long	536385535
	.long	1510632735
	.long	4012839307
	.long	2079477878
	.long	956874519
	.long	3154849823
	.long	1072771071
	.long	3021265470
	.long	3730711318
	.long	4158955756
	.long	1913749038
	.long	2014732351
	.long	2145542143
	.long	1747563645
	.long	3166455341
	.long	4022944216
	.long	3827498077
	.long	4029464703
	.long	4291084286
	.long	3495127291
	.long	2037943386
	.long	3750921137
	.long	3360028859
	.long	3763962110
	.long	4287201277
	.long	2695287286
	.long	4075886773
	.long	3206874979
	.long	2425090423
	.long	3232956925
	.long	4279435259
	.long	1095607277
	.long	3856806251
	.long	2118782663
	.long	555213551
	.long	2170946555
	.long	4263903223
	.long	2191214555
	.long	3418645206
	.long	4237565327
	.long	1110427102
	.long	46925815
	.long	4232839151
	.long	87461814
	.long	2542323117
	.long	4180163359
	.long	2220854205
	.long	93851631
	.long	4170711006
	.long	174923629
	.long	789678938
	.long	4065359423
	.long	146741115
	.long	187703262
	.long	4046454716
	.long	349847258
	.long	1579357876
	.long	3835751551
	.long	293482231
	.long	375406524
	.long	3797942136
	.long	699694516
	.long	3158715752
	.long	3376535807
	.long	586964463
	.long	750813049
	.long	3300916976
	.long	1399389033
	.long	2022464208
	.long	2458104319
	.long	1173928927
	.long	1501626098
	.long	2306866656
	.long	2798778067
	.long	4044928416
	.long	621241343
	.long	2347857855
	.long	3003252197
	.long	318766016
	.long	1302588838
	.long	3794889537
	.long	1242482686
	.long	400748414
	.long	1711537099
	.long	637532033
	.long	2605177677
	.long	3294811778
	.long	2484965372
	.long	801496828
	.long	3423074199
	.long	1275064066
	.long	915388059
	.long	2294656261
	.long	674963448
	.long	1602993657
	.long	2551181103
	.long	2550128133
	.long	1830776118
	.long	294345226
	.long	1349926897
	.long	3205987314
	.long	807394910
	.long	805288971
	.long	3661552236
	.long	588690452
	.long	2699853794
	.long	2117007332
	.long	1614789820
	.long	1610577942
	.long	3028137177
	.long	1177380905
	.long	1104740292
	.long	4234014665
	.long	3229579640
	.long	3221155884
	.long	1761307059
	.long	2354761811
	.long	2209480585
	.long	4173062034
	.long	2164191985
	.long	2147344473
	.long	3522614119
	.long	414556326
	.long	123993874
	.long	4051156773
	.long	33416674
	.long	4294688947
	.long	2750260943
	.long	829112653
	.long	247987749
	.long	3807346250
	.long	66833348
	.long	4294410598
	.long	1205554591
	.long	1658225307
	.long	495975499
	.long	3319725204
	.long	133666696
	.long	4293853900
	.long	2411109183
	.long	3316450614
	.long	991950999
	.long	2344483112
	.long	267333393
	.long	4292740504
	.long	527251070
	.long	2337933933
	.long	1983901999
	.long	393998928
	.long	534666787
	.long	4290513712
	.long	1054502141
	.long	380900570
	.long	3967803999
	.long	787997856
	.long	1069333574
	.long	4286060128
	.long	2109004283
	.long	761801140
	.long	3640640703
	.long	1575995713
	.long	2138667148
	.long	4277152960
	.long	4218008566
	.long	1523602280
	.long	2986314111
	.long	3151991427
	.long	4277334296
	.long	4259338624
	.long	4141049836
	.long	3047204561
	.long	1677660927
	.long	2009015559
	.long	4259701297
	.long	4223709953
	.long	3987132377
	.long	1799441827
	.long	3355321855
	.long	4018031118
	.long	4224435298
	.long	4152452611
	.long	3679297459
	.long	3598883655
	.long	2415676415
	.long	3741094941
	.long	4153903301
	.long	4009937927
	.long	3063627623
	.long	2902800015
	.long	536385535
	.long	3187222587
	.long	4012839307
	.long	3724908559
	.long	1832287951
	.long	1510632735
	.long	1072771071
	.long	2079477878
	.long	3730711318
	.long	3154849823
	.long	3664575902
	.long	3021265470
	.long	2145542143
	.long	4158955756
	.long	3166455341
	.long	2014732351
	.long	3034184508
	.long	1747563645
	.long	4291084286
	.long	4022944216
	.long	2037943386
	.long	4029464703
	.long	1773401721
	.long	3495127291
	.long	4287201277
	.long	3750921137
	.long	4075886773
	.long	3763962110
	.long	3546803442
	.long	2695287286
	.long	4279435259
	.long	3206874979
	.long	3856806251
	.long	3232956925
	.long	2798639588
	.long	1095607277
	.long	4263903223
	.long	2118782663
	.long	3418645206
	.long	2170946555
	.long	1302311881
	.long	2191214555
	.long	4232839151
	.long	4237565327
	.long	2542323117
	.long	46925815
	.long	2604623763
	.long	87461814
	.long	4170711006
	.long	4180163359
	.long	789678938
	.long	93851631
	.long	914280231
	.long	174923629
	.long	4046454716
	.long	4065359423
	.long	1579357876
	.long	187703262
	.long	1828560463
	.long	349847258
	.long	3797942136
	.long	3835751551
	.long	3158715752
	.long	375406524
	.long	3657120927
	.long	699694516
	.long	3300916976
	.long	3376535807
	.long	2022464208
	.long	750813049
	.long	3019274558
	.long	1399389033
	.long	2306866656
	.long	2458104319
	.long	4044928416
	.long	1501626098
	.long	1743581820
	.long	2798778067
	.long	318766016
	.long	621241343
	.long	3794889537
	.long	3003252197
	.long	3487163641
	.long	1302588838
	.long	637532033
	.long	1242482686
	.long	3294811778
	.long	1711537099
	.long	2679359986
	.long	2605177677
	.long	1275064066
	.long	2484965372
	.long	2294656261
	.long	3423074199
	.long	1063752677
	.long	915388059
	.long	2550128133
	.long	674963448
	.long	294345226
	.long	2551181103
	.long	2127505355
	.long	1830776118
	.long	805288971
	.long	1349926897
	.long	588690452
	.long	807394910
	.long	4255010710
	.long	3661552236
	.long	1610577942
	.long	2699853794
	.long	1177380905
	.long	1614789820
	.long	4215054124
	.long	3028137177
	.long	3221155884
	.long	1104740292
	.long	2354761811
	.long	3229579640
	.long	4135140952
	.long	1761307059
	.long	2147344473
	.long	2209480585
	.long	414556326
	.long	2164191985
	.long	3975314608
	.long	3522614119
	.long	4294688947
	.long	123993874
	.long	829112653
	.long	33416674
	.long	3655661921
	.long	2750260943
	.long	4294410598
	.long	247987749
	.long	1658225307
	.long	66833348
	.long	3016356546
	.long	1205554591
	.long	4293853900
	.long	495975499
	.long	3316450614
	.long	133666696
	.long	1737745796
	.long	2411109183
	.long	4292740504
	.long	991950999
	.long	2337933933
	.long	267333393
	.long	3475491593
	.long	527251070
	.long	4290513712
	.long	1983901999
	.long	380900570
	.long	534666787
	.long	2656015891
	.long	1054502141
	.long	4286060128
	.long	3967803999
	.long	761801140
	.long	1069333574
	.long	1017064486
	.long	2109004283
	.long	4277152960
	.long	3640640703
	.long	1523602280
	.long	2138667148
	.long	2034128973
	.long	4218008566
	.long	4259338624
	.long	2986314111
	.long	3047204561
	.long	4277334296
	.long	4068257947
	.long	4141049836
	.long	4223709953
	.long	1677660927
	.long	1799441827
	.long	4259701297
	.long	3841548598
	.long	3987132377
	.long	4152452611
	.long	3355321855
	.long	3598883655
	.long	4224435298
	.long	3388129901
	.long	3679297459
	.long	4009937927
	.long	2415676415
	.long	2902800015
	.long	4153903301
	.long	2481292507
	.long	3063627623
	.long	3724908559
	.long	536385535
	.long	1510632735
	.long	4012839307
	.long	667617719
	.long	1832287951
	.long	3154849823
	.long	1072771071
	.long	3021265470
	.long	3730711318
	.long	1335235438
	.long	3664575902
	.long	2014732351
	.long	2145542143
	.long	1747563645
	.long	3166455341
	.long	2670470877
	.long	3034184508
	.long	4029464703
	.long	4291084286
	.long	3495127291
	.long	2037943386
	.long	1045974458
	.long	1773401721
	.long	3763962110
	.long	4287201277
	.long	2695287286
	.long	4075886773
	.long	2091948916
	.long	3546803442
	.long	3232956925
	.long	4279435259
	.long	1095607277
	.long	3856806251
	.long	4183897833
	.long	2798639588
	.long	2170946555
	.long	4263903223
	.long	2191214555
	.long	3418645206
	.long	4072828371
	.long	1302311881
	.long	46925815
	.long	4232839151
	.long	87461814
	.long	2542323117
	.long	3850689447
	.long	2604623763
	.long	93851631
	.long	4170711006
	.long	174923629
	.long	789678938
	.long	3406411599
	.long	914280231
	.long	187703262
	.long	4046454716
	.long	349847258
	.long	1579357876
	.long	2517855902
	.long	1828560463
	.long	375406524
	.long	3797942136
	.long	699694516
	.long	3158715752
	.long	740744509
	.long	3657120927
	.long	750813049
	.long	3300916976
	.long	1399389033
	.long	2022464208
	.long	1481489018
	.long	3019274558
	.long	1501626098
	.long	2306866656
	.long	2798778067
	.long	4044928416
	.long	2962978036
	.long	1743581820
	.long	3003252197
	.long	318766016
	.long	1302588838
	.long	3794889537
	.long	1630988776
	.long	3487163641
	.long	1711537099
	.long	637532033
	.long	2605177677
	.long	3294811778
	.long	3261977553
	.long	2679359986
	.long	3423074199
	.long	1275064066
	.long	915388059
	.long	2294656261
	.long	2228987811
	.long	1063752677
	.long	2551181103
	.long	2550128133
	.long	1830776118
	.long	294345226
	.long	163008326
	.long	2127505355
	.long	807394910
	.long	805288971
	.long	3661552236
	.long	588690452
	.long	326016652
	.long	4255010710
	.long	1614789820
	.long	1610577942
	.long	3028137177
	.long	1177380905
	.long	652033304
	.long	4215054124
	.long	3229579640
	.long	3221155884
	.long	1761307059
	.long	2354761811
	.long	1304066609
	.long	4135140952
	.long	2164191985
	.long	2147344473
	.long	3522614119
	.long	414556326
	.long	2608133219
	.long	3975314608
	.long	33416674
	.long	4294688947
	.long	2750260943
	.long	829112653
	.long	921299143
	.long	3655661921
	.long	66833348
	.long	4294410598
	.long	1205554591
	.long	1658225307
	.long	1842598287
	.long	3016356546
	.long	133666696
	.long	4293853900
	.long	2411109183
	.long	3316450614
	.long	3685196575
	.long	1737745796
	.long	267333393
	.long	4292740504
	.long	527251070
	.long	2337933933
	.long	3075425855
	.long	3475491593
	.long	534666787
	.long	4290513712
	.long	1054502141
	.long	380900570
	.long	1855884414
	.long	2656015891
	.long	1069333574
	.long	4286060128
	.long	2109004283
	.long	761801140
	.long	3711768829
	.long	1017064486
	.long	2138667148
	.long	4277152960
	.long	4218008566
	.long	1523602280
	.long	3128570363
	.long	2034128973
	.long	4277334296
	.long	4259338624
	.long	4141049836
	.long	3047204561
	.long	1962173430
	.long	4068257947
	.long	4259701297
	.long	4223709953
	.long	3987132377
	.long	1799441827
	.long	3924346860
	.long	3841548598
	.long	4224435298
	.long	4152452611
	.long	3679297459
	.long	3598883655
	.long	3553726425
	.long	3388129901
	.long	4153903301
	.long	4009937927
	.long	3063627623
	.long	2902800015
	.long	2812485555
	.long	2481292507
	.long	4012839307
	.long	3724908559
	.long	1832287951
	.long	1510632735
	.long	1330003814
	.long	667617719
	.long	3730711318
	.long	3154849823
	.long	3664575902
	.long	3021265470
	.long	2660007629
	.long	1335235438
	.long	3166455341
	.long	2014732351
	.long	3034184508
	.long	1747563645
	.long	1025047962
	.long	2670470877
	.long	2037943386
	.long	4029464703
	.long	1773401721
	.long	3495127291
	.long	2050095924
	.long	1045974458
	.long	4075886773
	.long	3763962110
	.long	3546803442
	.long	2695287286
	.long	4100191849
	.long	2091948916
	.long	3856806251
	.long	3232956925
	.long	2798639588
	.long	1095607277
	.long	3905416403
	.long	4183897833
	.long	3418645206
	.long	2170946555
	.long	1302311881
	.long	2191214555
	.long	3515865511
	.long	4072828371
	.long	2542323117
	.long	46925815
	.long	2604623763
	.long	87461814
	.long	2736763727
	.long	3850689447
	.long	789678938
	.long	93851631
	.long	914280231
	.long	174923629
	.long	1178560158
	.long	3406411599
	.long	1579357876
	.long	187703262
	.long	1828560463
	.long	349847258
	.long	2357120316
	.long	2517855902
	.long	3158715752
	.long	375406524
	.long	3657120927
	.long	699694516
	.long	419273337
	.long	740744509
	.long	2022464208
	.long	750813049
	.long	3019274558
	.long	1399389033
	.long	838546674
	.long	1481489018
	.long	4044928416
	.long	1501626098
	.long	1743581820
	.long	2798778067
	.long	1677093349
	.long	2962978036
	.long	3794889537
	.long	3003252197
	.long	3487163641
	.long	1302588838
	.long	3354186699
	.long	1630988776
	.long	3294811778
	.long	1711537099
	.long	2679359986
	.long	2605177677
	.long	2413406103
	.long	3261977553
	.long	2294656261
	.long	3423074199
	.long	1063752677
	.long	915388059
	.long	531844911
	.long	2228987811
	.long	294345226
	.long	2551181103
	.long	2127505355
	.long	1830776118
	.long	1063689823
	.long	163008326
	.long	588690452
	.long	807394910
	.long	4255010710
	.long	3661552236
	.long	2127379647
	.long	326016652
	.long	1177380905
	.long	1614789820
	.long	4215054124
	.long	3028137177
	.long	4254759295
	.long	652033304
	.long	2354761811
	.long	3229579640
	.long	4135140952
	.long	1761307059
	.long	4214551295
	.long	1304066609
	.long	414556326
	.long	2164191985
	.long	3975314608
	.long	3522614119
	.long	4134135294
	.long	2608133219
	.long	829112653
	.long	33416674
	.long	3655661921
	.long	2750260943
	.long	3973303293
	.long	921299143
	.long	1658225307
	.long	66833348
	.long	3016356546
	.long	1205554591
	.long	3651639290
	.long	1842598287
	.long	3316450614
	.long	133666696
	.long	1737745796
	.long	2411109183
	.long	3008311285
	.long	3685196575
	.long	2337933933
	.long	267333393
	.long	3475491593
	.long	527251070
	.long	1721655274
	.long	3075425855
	.long	380900570
	.long	534666787
	.long	2656015891
	.long	1054502141
	.long	3443310548
	.long	1855884414
	.long	761801140
	.long	1069333574
	.long	1017064486
	.long	2109004283
	.long	2591653800
	.long	3711768829
	.long	1523602280
	.long	2138667148
	.long	2034128973
	.long	4218008566
	.long	888340305
	.long	3128570363
	.long	3047204561
	.long	4277334296
	.long	4068257947
	.long	4141049836
	.long	1776680610
	.long	1962173430
	.long	1799441827
	.long	4259701297
	.long	3841548598
	.long	3987132377
	.long	3553361221
	.long	3924346860
	.long	3598883655
	.long	4224435298
	.long	3388129901
	.long	3679297459
	.long	2811755147
	.long	3553726425
	.long	2902800015
	.long	4153903301
	.long	2481292507
	.long	3063627623
	.long	1328542998
	.long	2812485555
	.long	1510632735
	.long	4012839307
	.long	667617719
	.long	1832287951
	.long	2657085997
	.long	1330003814
	.long	3021265470
	.long	3730711318
	.long	1335235438
	.long	3664575902
	.long	1019204698
	.long	2660007629
	.long	1747563645
	.long	3166455341
	.long	2670470877
	.long	3034184508
	.long	2038409397
	.long	1025047962
	.long	3495127291
	.long	2037943386
	.long	1045974458
	.long	1773401721
	.long	4076818795
	.long	2050095924
	.long	2695287286
	.long	4075886773
	.long	2091948916
	.long	3546803442
	.long	3858670295
	.long	4100191849
	.long	1095607277
	.long	3856806251
	.long	4183897833
	.long	2798639588
	.long	3422373294
	.long	3905416403
	.long	2191214555
	.long	3418645206
	.long	4072828371
	.long	1302311881
	.long	2549779293
	.long	3515865511
	.long	87461814
	.long	2542323117
	.long	3850689447
	.long	2604623763
	.long	804591290
	.long	2736763727
	.long	174923629
	.long	789678938
	.long	3406411599
	.long	914280231
	.long	1609182581
	.long	1178560158
	.long	349847258
	.long	1579357876
	.long	2517855902
	.long	1828560463
	.long	3218365162
	.long	2357120316
	.long	699694516
	.long	3158715752
	.long	740744509
	.long	3657120927
	.long	2141763028
	.long	419273337
	.long	1399389033
	.long	2022464208
	.long	1481489018
	.long	3019274558
	.long	4283526057
	.long	838546674
	.long	2798778067
	.long	4044928416
	.long	2962978036
	.long	1743581820
	.long	4272084818
	.long	1677093349
	.long	1302588838
	.long	3794889537
	.long	1630988776
	.long	3487163641
	.long	4249202340
	.long	3354186699
	.long	2605177677
	.long	3294811778
	.long	3261977553
	.long	2679359986
	.long	4203437385
	.long	2413406103
	.long	915388059
	.long	2294656261
	.long	2228987811
	.long	1063752677
	.long	4111907475
	.long	531844911
	.long	1830776118
	.long	294345226
	.long	163008326
	.long	2127505355
	.long	3928847655
	.long	1063689823
	.long	3661552236
	.long	588690452
	.long	326016652
	.long	4255010710
	.long	3562728015
	.long	2127379647
	.long	3028137177
	.long	1177380905
	.long	652033304
	.long	4215054124
	.long	2830488734
	.long	4254759295
	.long	1761307059
	.long	2354761811
	.long	1304066609
	.long	4135140952
	.long	1366010173
	.long	4214551295
	.long	3522614119
	.long	414556326
	.long	2608133219
	.long	3975314608
	.long	2732020347
	.long	4134135294
	.long	2750260943
	.long	829112653
	.long	921299143
	.long	3655661921
	.long	1169073399
	.long	3973303293
	.long	1205554591
	.long	1658225307
	.long	1842598287
	.long	3016356546
	.long	2338146798
	.long	3651639290
	.long	2411109183
	.long	3316450614
	.long	3685196575
	.long	1737745796
	.long	381326301
	.long	3008311285
	.long	527251070
	.long	2337933933
	.long	3075425855
	.long	3475491593
	.long	762652602
	.long	1721655274
	.long	1054502141
	.long	380900570
	.long	1855884414
	.long	2656015891
	.long	1525305205
	.long	3443310548
	.long	2109004283
	.long	761801140
	.long	3711768829
	.long	1017064486
	.long	3050610411
	.long	2591653800
	.long	4218008566
	.long	1523602280
	.long	3128570363
	.long	2034128973
	.long	1806253526
	.long	888340305
	.long	4141049836
	.long	3047204561
	.long	1962173430
	.long	4068257947
	.long	3612507052
	.long	1776680610
	.long	3987132377
	.long	1799441827
	.long	3924346860
	.long	3841548598
	.long	2930046808
	.long	3553361221
	.long	3679297459
	.long	3598883655
	.long	3553726425
	.long	3388129901
	.long	1565126321
	.long	2811755147
	.long	3063627623
	.long	2902800015
	.long	2812485555
	.long	2481292507
	.long	3130252643
	.long	1328542998
	.long	1832287951
	.long	1510632735
	.long	1330003814
	.long	667617719
	.long	1965537991
	.long	2657085997
	.long	3664575902
	.long	3021265470
	.long	2660007629
	.long	1335235438
	.long	3931075983
	.long	1019204698
	.long	3034184508
	.long	1747563645
	.long	1025047962
	.long	2670470877
	.long	3567184671
	.long	2038409397
	.long	1773401721
	.long	3495127291
	.long	2050095924
	.long	1045974458
	.long	2839402047
	.long	4076818795
	.long	3546803442
	.long	2695287286
	.long	4100191849
	.long	2091948916
	.long	1383836798
	.long	3858670295
	.long	2798639588
	.long	1095607277
	.long	3905416403
	.long	4183897833
	.long	2767673597
	.long	3422373294
	.long	1302311881
	.long	2191214555
	.long	3515865511
	.long	4072828371
	.long	1240379898
	.long	2549779293
	.long	2604623763
	.long	87461814
	.long	2736763727
	.long	3850689447
	.long	2480759797
	.long	804591290
	.long	914280231
	.long	174923629
	.long	1178560158
	.long	3406411599
	.long	666552299
	.long	1609182581
	.long	1828560463
	.long	349847258
	.long	2357120316
	.long	2517855902
	.long	1333104599
	.long	3218365162
	.long	3657120927
	.long	699694516
	.long	419273337
	.long	740744509
	.long	2666209199
	.long	2141763028
	.long	3019274558
	.long	1399389033
	.long	838546674
	.long	1481489018
	.long	1037451103
	.long	4283526057
	.long	1743581820
	.long	2798778067
	.long	1677093349
	.long	2962978036
	.long	2074902206
	.long	4272084818
	.long	3487163641
	.long	1302588838
	.long	3354186699
	.long	1630988776
	.long	4149804412
	.long	4249202340
	.long	2679359986
	.long	2605177677
	.long	2413406103
	.long	3261977553
	.long	4004641529
	.long	4203437385
	.long	1063752677
	.long	915388059
	.long	531844911
	.long	2228987811
	.long	3714315762
	.long	4111907475
	.long	2127505355
	.long	1830776118
	.long	1063689823
	.long	163008326
	.long	3133664229
	.long	3928847655
	.long	4255010710
	.long	3661552236
	.long	2127379647
	.long	326016652
	.long	1972361163
	.long	3562728015
	.long	4215054124
	.long	3028137177
	.long	4254759295
	.long	652033304
	.long	3944722327
	.long	2830488734
	.long	4135140952
	.long	1761307059
	.long	4214551295
	.long	1304066609
	.long	3594477359
	.long	1366010173
	.long	3975314608
	.long	3522614119
	.long	4134135294
	.long	2608133219
	.long	2893987423
	.long	2732020347
	.long	3655661921
	.long	2750260943
	.long	3973303293
	.long	921299143
	.long	1493007550
	.long	1169073399
	.long	3016356546
	.long	1205554591
	.long	3651639290
	.long	1842598287
	.long	2986015100
	.long	2338146798
	.long	1737745796
	.long	2411109183
	.long	3008311285
	.long	3685196575
	.long	1677062904
	.long	381326301
	.long	3475491593
	.long	527251070
	.long	1721655274
	.long	3075425855
	.long	3354125809
	.long	762652602
	.long	2656015891
	.long	1054502141
	.long	3443310548
	.long	1855884414
	.long	2413284322
	.long	1525305205
	.long	1017064486
	.long	2109004283
	.long	2591653800
	.long	3711768829
	.long	531601349
	.long	3050610411
	.long	2034128973
	.long	4218008566
	.long	888340305
	.long	3128570363
	.long	1063202699
	.long	1806253526
	.long	4068257947
	.long	4141049836
	.long	1776680610
	.long	1962173430
	.long	2126405399
	.long	3612507052
	.long	3841548598
	.long	3987132377
	.long	3553361221
	.long	3924346860
	.long	4252810799
	.long	2930046808
	.long	3388129901
	.long	3679297459
	.long	2811755147
	.long	3553726425
	.long	4210654302
	.long	1565126321
	.long	2481292507
	.long	3063627623
	.long	1328542998
	.long	2812485555
	.long	4126341309
	.long	3130252643
	.long	667617719
	.long	1832287951
	.long	2657085997
	.long	1330003814
	.long	3957715323
	.long	1965537991
	.long	1335235438
	.long	3664575902
	.long	1019204698
	.long	2660007629
	.long	3620463350
	.long	3931075983
	.long	2670470877
	.long	3034184508
	.long	2038409397
	.long	1025047962
	.long	2945959404
	.long	3567184671
	.long	1045974458
	.long	1773401721
	.long	4076818795
	.long	2050095924
	.long	1596951513
	.long	2839402047
	.long	2091948916
	.long	3546803442
	.long	3858670295
	.long	4100191849
	.long	3193903027
	.long	1383836798
	.long	4183897833
	.long	2798639588
	.long	3422373294
	.long	3905416403
	.long	2092838759
	.long	2767673597
	.long	4072828371
	.long	1302311881
	.long	2549779293
	.long	3515865511
	.long	4185677519
	.long	1240379898
	.long	3850689447
	.long	2604623763
	.long	804591290
	.long	2736763727
	.long	4076387742
	.long	2480759797
	.long	3406411599
	.long	914280231
	.long	1609182581
	.long	1178560158
	.long	3857808189
	.long	666552299
	.long	2517855902
	.long	1828560463
	.long	3218365162
	.long	2357120316
	.long	3420649082
	.long	1333104599
	.long	740744509
	.long	3657120927
	.long	2141763028
	.long	419273337
	.long	2546330868
	.long	2666209199
	.long	1481489018
	.long	3019274558
	.long	4283526057
	.long	838546674
	.long	797694440
	.long	1037451103
	.long	2962978036
	.long	1743581820
	.long	4272084818
	.long	1677093349
	.long	1595388880
	.long	2074902206
	.long	1630988776
	.long	3487163641
	.long	4249202340
	.long	3354186699
	.long	3190777760
	.long	4149804412
	.long	3261977553
	.long	2679359986
	.long	4203437385
	.long	2413406103
	.long	2086588225
	.long	4004641529
	.long	2228987811
	.long	1063752677
	.long	4111907475
	.long	531844911
	.long	4173176451
	.long	3714315762
	.long	163008326
	.long	2127505355
	.long	3928847655
	.long	1063689823
	.long	4051385607
	.long	3133664229
	.long	326016652
	.long	4255010710
	.long	3562728015
	.long	2127379647
	.long	3807803918
	.long	1972361163
	.long	652033304
	.long	4215054124
	.long	2830488734
	.long	4254759295
	.long	3320640540
	.long	3944722327
	.long	1304066609
	.long	4135140952
	.long	1366010173
	.long	4214551295
	.long	2346313785
	.long	3594477359
	.long	2608133219
	.long	3975314608
	.long	2732020347
	.long	4134135294
	.long	397660275
	.long	2893987423
	.long	921299143
	.long	3655661921
	.long	1169073399
	.long	3973303293
	.long	795320551
	.long	1493007550
	.long	1842598287
	.long	3016356546
	.long	2338146798
	.long	3651639290
	.long	1590641102
	.long	2986015100
	.long	3685196575
	.long	1737745796
	.long	381326301
	.long	3008311285
	.long	3181282204
	.long	1677062904
	.long	3075425855
	.long	3475491593
	.long	762652602
	.long	1721655274
	.long	2067597113
	.long	3354125809
	.long	1855884414
	.long	2656015891
	.long	1525305205
	.long	3443310548
	.long	4135194227
	.long	2413284322
	.long	3711768829
	.long	1017064486
	.long	3050610411
	.long	2591653800
	.long	3975421159
	.long	531601349
	.long	3128570363
	.long	2034128973
	.long	1806253526
	.long	888340305
	.long	3655875023
	.long	1063202699
	.long	1962173430
	.long	4068257947
	.long	3612507052
	.long	1776680610
	.long	3016782751
	.long	2126405399
	.long	3924346860
	.long	3841548598
	.long	2930046808
	.long	3553361221
	.long	1738598206
	.long	4252810799
	.long	3553726425
	.long	3388129901
	.long	1565126321
	.long	2811755147
	.long	3477196413
	.long	4210654302
	.long	2812485555
	.long	2481292507
	.long	3130252643
	.long	1328542998
	.long	2659425531
	.long	4126341309
	.long	1330003814
	.long	667617719
	.long	1965537991
	.long	2657085997
	.long	1023883767
	.long	3957715323
	.long	2660007629
	.long	1335235438
	.long	3931075983
	.long	1019204698
	.long	2047767535
	.long	3620463350
	.long	1025047962
	.long	2670470877
	.long	3567184671
	.long	2038409397
	.long	4095535070
	.long	2945959404
	.long	2050095924
	.long	1045974458
	.long	2839402047
	.long	4076818795
	.long	3896102844
	.long	1596951513
	.long	4100191849
	.long	2091948916
	.long	1383836798
	.long	3858670295
	.long	3497238392
	.long	3193903027
	.long	3905416403
	.long	4183897833
	.long	2767673597
	.long	3422373294
	.long	2699509489
	.long	2092838759
	.long	3515865511
	.long	4072828371
	.long	1240379898
	.long	2549779293
	.long	1104051682
	.long	4185677519
	.long	2736763727
	.long	3850689447
	.long	2480759797
	.long	804591290
	.long	2208103365
	.long	4076387742
	.long	1178560158
	.long	3406411599
	.long	666552299
	.long	1609182581
	.long	121239434
	.long	3857808189
	.long	2357120316
	.long	2517855902
	.long	1333104599
	.long	3218365162
	.long	242478868
	.long	3420649082
	.long	419273337
	.long	740744509
	.long	2666209199
	.long	2141763028
	.long	484957737
	.long	2546330868
	.long	838546674
	.long	1481489018
	.long	1037451103
	.long	4283526057
	.long	969915474
	.long	797694440
	.long	1677093349
	.long	2962978036
	.long	2074902206
	.long	4272084818
	.long	1939830949
	.long	1595388880
	.long	3354186699
	.long	1630988776
	.long	4149804412
	.long	4249202340
	.long	3879661898
	.long	3190777760
	.long	2413406103
	.long	3261977553
	.long	4004641529
	.long	4203437385
	.long	3464356500
	.long	2086588225
	.long	531844911
	.long	2228987811
	.long	3714315762
	.long	4111907475
	.long	2633745705
	.long	4173176451
	.long	1063689823
	.long	163008326
	.long	3133664229
	.long	3928847655
	.long	972524114
	.long	4051385607
	.long	2127379647
	.long	326016652
	.long	1972361163
	.long	3562728015
	.long	1945048229
	.long	3807803918
	.long	4254759295
	.long	652033304
	.long	3944722327
	.long	2830488734
	.long	3890096458
	.long	3320640540
	.long	4214551295
	.long	1304066609
	.long	3594477359
	.long	1366010173
	.long	3485225620
	.long	2346313785
	.long	4134135294
	.long	2608133219
	.long	2893987423
	.long	2732020347
	.long	2675483945
	.long	397660275
	.long	3973303293
	.long	921299143
	.long	1493007550
	.long	1169073399
	.long	1056000594
	.long	795320551
	.long	3651639290
	.long	1842598287
	.long	2986015100
	.long	2338146798
	.long	2112001188
	.long	1590641102
	.long	3008311285
	.long	3685196575
	.long	1677062904
	.long	381326301
	.long	4224002377
	.long	3181282204
	.long	1721655274
	.long	3075425855
	.long	3354125809
	.long	762652602
	.long	4153037458
	.long	2067597113
	.long	3443310548
	.long	1855884414
	.long	2413284322
	.long	1525305205
	.long	4011107621
	.long	4135194227
	.long	2591653800
	.long	3711768829
	.long	531601349
	.long	3050610411
	.long	3727247947
	.long	3975421159
	.long	888340305
	.long	3128570363
	.long	1063202699
	.long	1806253526
	.long	3159528599
	.long	3655875023
	.long	1776680610
	.long	1962173430
	.long	2126405399
	.long	3612507052
	.long	2024089902
	.long	3016782751
	.long	3553361221
	.long	3924346860
	.long	4252810799
	.long	2930046808
	.long	4048179805
	.long	1738598206
	.long	2811755147
	.long	3553726425
	.long	4210654302
	.long	1565126321
	.long	3801392314
	.long	3477196413
	.long	1328542998
	.long	2812485555
	.long	4126341309
	.long	3130252643
	.long	3307817333
	.long	2659425531
	.long	2657085997
	.long	1330003814
	.long	3957715323
	.long	1965537991
	.long	2320667370
	.long	1023883767
	.long	1019204698
	.long	2660007629
	.long	3620463350
	.long	3931075983
	.long	346367444
	.long	2047767535
	.long	2038409397
	.long	1025047962
	.long	2945959404
	.long	3567184671
	.long	692734889
	.long	4095535070
	.long	4076818795
	.long	2050095924
	.long	1596951513
	.long	2839402047
	.long	1385469779
	.long	3896102844
	.long	3858670295
	.long	4100191849
	.long	3193903027
	.long	1383836798
	.long	2770939558
	.long	3497238392
	.long	3422373294
	.long	3905416403
	.long	2092838759
	.long	2767673597
	.long	1246911821
	.long	2699509489
	.long	2549779293
	.long	3515865511
	.long	4185677519
	.long	1240379898
	.long	2493823642
	.long	1104051682
	.long	804591290
	.long	2736763727
	.long	4076387742
	.long	2480759797
	.long	692679989
	.long	2208103365
	.long	1609182581
	.long	1178560158
	.long	3857808189
	.long	666552299
	.long	1385359979
	.long	121239434
	.long	3218365162
	.long	2357120316
	.long	3420649082
	.long	1333104599
	.long	2770719959
	.long	242478868
	.long	2141763028
	.long	419273337
	.long	2546330868
	.long	2666209199
	.long	1246472623
	.long	484957737
	.long	4283526057
	.long	838546674
	.long	797694440
	.long	1037451103
	.long	2492945247
	.long	969915474
	.long	4272084818
	.long	1677093349
	.long	1595388880
	.long	2074902206
	.long	690923199
	.long	1939830949
	.long	4249202340
	.long	3354186699
	.long	3190777760
	.long	4149804412
	.long	1381846399
	.long	3879661898
	.long	4203437385
	.long	2413406103
	.long	2086588225
	.long	4004641529
	.long	2763692798
	.long	3464356500
	.long	4111907475
	.long	531844911
	.long	4173176451
	.long	3714315762
	.long	1232418301
	.long	2633745705
	.long	3928847655
	.long	1063689823
	.long	4051385607
	.long	3133664229
	.long	2464836603
	.long	972524114
	.long	3562728015
	.long	2127379647
	.long	3807803918
	.long	1972361163
	.long	634705910
	.long	1945048229
	.long	2830488734
	.long	4254759295
	.long	3320640540
	.long	3944722327
	.long	1269411821
	.long	3890096458
	.long	1366010173
	.long	4214551295
	.long	2346313785
	.long	3594477359
	.long	2538823642
	.long	3485225620
	.long	2732020347
	.long	4134135294
	.long	397660275
	.long	2893987423
	.long	782679989
	.long	2675483945
	.long	1169073399
	.long	3973303293
	.long	795320551
	.long	1493007550
	.long	1565359979
	.long	1056000594
	.long	2338146798
	.long	3651639290
	.long	1590641102
	.long	2986015100
	.long	3130719959
	.long	2112001188
	.long	381326301
	.long	3008311285
	.long	3181282204
	.long	1677062904
	.long	1966472623
	.long	4224002377
	.long	762652602
	.long	1721655274
	.long	2067597113
	.long	3354125809
	.long	3932945247
	.long	4153037458
	.long	1525305205
	.long	3443310548
	.long	4135194227
	.long	2413284322
	.long	3570923199
	.long	4011107621
	.long	3050610411
	.long	2591653800
	.long	3975421159
	.long	531601349
	.long	2846879102
	.long	3727247947
	.long	1806253526
	.long	888340305
	.long	3655875023
	.long	1063202699
	.long	1398790909
	.long	3159528599
	.long	3612507052
	.long	1776680610
	.long	3016782751
	.long	2126405399
	.long	2797581819
	.long	2024089902
	.long	2930046808
	.long	3553361221
	.long	1738598206
	.long	4252810799
	.long	1300196342
	.long	4048179805
	.long	1565126321
	.long	2811755147
	.long	3477196413
	.long	4210654302
	.long	2600392684
	.long	3801392314
	.long	3130252643
	.long	1328542998
	.long	2659425531
	.long	4126341309
	.long	905818072
	.long	3307817333
	.long	1965537991
	.long	2657085997
	.long	1023883767
	.long	3957715323
	.long	1811636145
	.long	2320667370
	.long	3931075983
	.long	1019204698
	.long	2047767535
	.long	3620463350
	.long	3623272290
	.long	346367444
	.long	3567184671
	.long	2038409397
	.long	4095535070
	.long	2945959404
	.long	2951577284
	.long	692734889
	.long	2839402047
	.long	4076818795
	.long	3896102844
	.long	1596951513
	.long	1608187272
	.long	1385469779
	.long	1383836798
	.long	3858670295
	.long	3497238392
	.long	3193903027
	.long	3216374545
	.long	2770939558
	.long	2767673597
	.long	3422373294
	.long	2699509489
	.long	2092838759
	.long	2137781795
	.long	1246911821
	.long	1240379898
	.long	2549779293
	.long	1104051682
	.long	4185677519
	.long	4275563591
	.long	2493823642
	.long	2480759797
	.long	804591290
	.long	2208103365
	.long	4076387742
	.long	4256159887
	.long	692679989
	.long	666552299
	.long	1609182581
	.long	121239434
	.long	3857808189
	.long	4217352479
	.long	1385359979
	.long	1333104599
	.long	3218365162
	.long	242478868
	.long	3420649082
	.long	4139737663
	.long	2770719959
	.long	2666209199
	.long	2141763028
	.long	484957737
	.long	2546330868
	.long	3984508030
	.long	1246472623
	.long	1037451103
	.long	4283526057
	.long	969915474
	.long	797694440
	.long	3674048764
	.long	2492945247
	.long	2074902206
	.long	4272084818
	.long	1939830949
	.long	1595388880
	.long	3053130232
	.long	690923199
	.long	4149804412
	.long	4249202340
	.long	3879661898
	.long	3190777760
	.long	1811293169
	.long	1381846399
	.long	4004641529
	.long	4203437385
	.long	3464356500
	.long	2086588225
	.long	3622586339
	.long	2763692798
	.long	3714315762
	.long	4111907475
	.long	2633745705
	.long	4173176451
	.long	2950205382
	.long	1232418301
	.long	3133664229
	.long	3928847655
	.long	972524114
	.long	4051385607
	.long	1605443469
	.long	2464836603
	.long	1972361163
	.long	3562728015
	.long	1945048229
	.long	3807803918
	.long	3210886938
	.long	634705910
	.long	3944722327
	.long	2830488734
	.long	3890096458
	.long	3320640540
	.long	2126806581
	.long	1269411821
	.long	3594477359
	.long	1366010173
	.long	3485225620
	.long	2346313785
	.long	4253613162
	.long	2538823642
	.long	2893987423
	.long	2732020347
	.long	2675483945
	.long	397660275
	.long	4212259029
	.long	782679989
	.long	1493007550
	.long	1169073399
	.long	1056000594
	.long	795320551
	.long	4129550763
	.long	1565359979
	.long	2986015100
	.long	2338146798
	.long	2112001188
	.long	1590641102
	.long	3964134231
	.long	3130719959
	.long	1677062904
	.long	381326301
	.long	4224002377
	.long	3181282204
	.long	3633301166
	.long	1966472623
	.long	3354125809
	.long	762652602
	.long	4153037458
	.long	2067597113
	.long	2971635037
	.long	3932945247
	.long	2413284322
	.long	1525305205
	.long	4011107621
	.long	4135194227
	.long	1648302778
	.long	3570923199
	.long	531601349
	.long	3050610411
	.long	3727247947
	.long	3975421159
	.long	3296605556
	.long	2846879102
	.long	1063202699
	.long	1806253526
	.long	3159528599
	.long	3655875023
	.long	2298243816
	.long	1398790909
	.long	2126405399
	.long	3612507052
	.long	2024089902
	.long	3016782751
	.long	301520336
	.long	2797581819
	.long	4252810799
	.long	2930046808
	.long	4048179805
	.long	1738598206
	.long	603040673
	.long	1300196342
	.long	4210654302
	.long	1565126321
	.long	3801392314
	.long	3477196413
	.long	1206081346
	.long	2600392684
	.long	4126341309
	.long	3130252643
	.long	3307817333
	.long	2659425531
	.long	2412162692
	.long	905818072
	.long	3957715323
	.long	1965537991
	.long	2320667370
	.long	1023883767
	.long	529358088
	.long	1811636145
	.long	3620463350
	.long	3931075983
	.long	346367444
	.long	2047767535
	.long	1058716176
	.long	3623272290
	.long	2945959404
	.long	3567184671
	.long	692734889
	.long	4095535070
	.long	2117432353
	.long	2951577284
	.long	1596951513
	.long	2839402047
	.long	1385469779
	.long	3896102844
	.long	4234864706
	.long	1608187272
	.long	3193903027
	.long	1383836798
	.long	2770939558
	.long	3497238392
	.long	4174762117
	.long	3216374545
	.long	2092838759
	.long	2767673597
	.long	1246911821
	.long	2699509489
	.long	4054556938
	.long	2137781795
	.long	4185677519
	.long	1240379898
	.long	2493823642
	.long	1104051682
	.long	3814146581
	.long	4275563591
	.long	4076387742
	.long	2480759797
	.long	692679989
	.long	2208103365
	.long	3333325867
	.long	4256159887
	.long	3857808189
	.long	666552299
	.long	1385359979
	.long	121239434
	.long	2371684438
	.long	4217352479
	.long	3420649082
	.long	1333104599
	.long	2770719959
	.long	242478868
	.long	448401580
	.long	4139737663
	.long	2546330868
	.long	2666209199
	.long	1246472623
	.long	484957737
	.long	896803160
	.long	3984508030
	.type	__svml_dcos_ha_reduction_data_internal,@object
	.size	__svml_dcos_ha_reduction_data_internal,49152
	.space 512, 0x00 	# pad
	.align 64
	.hidden __svml_dcos_ha_data_internal_ha
	.globl __svml_dcos_ha_data_internal_ha
__svml_dcos_ha_data_internal_ha:
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	210035182
	.long	1064007864
	.long	0
	.long	3213885440
	.long	379294427
	.long	1072693090
	.long	3221225472
	.long	1015405987
	.long	561145352
	.long	1065056936
	.long	0
	.long	3214934016
	.long	1619315981
	.long	1072692616
	.long	1073741824
	.long	3162584397
	.long	2896342633
	.long	3212231467
	.long	0
	.long	3214934016
	.long	4026464093
	.long	1072691826
	.long	3221225472
	.long	3163075789
	.long	1003003824
	.long	1066107496
	.long	0
	.long	3215982592
	.long	3816385918
	.long	1072690721
	.long	536870912
	.long	3161216956
	.long	3237733378
	.long	1062425136
	.long	0
	.long	3215982592
	.long	1703834029
	.long	1072689301
	.long	3758096384
	.long	1015028364
	.long	2523975598
	.long	3213273348
	.long	0
	.long	3215982592
	.long	2902558778
	.long	1072687565
	.long	3221225472
	.long	1013260780
	.long	711626851
	.long	3214400320
	.long	0
	.long	3215982592
	.long	4240268041
	.long	1072685514
	.long	536870912
	.long	1013588963
	.long	257503056
	.long	1067164005
	.long	0
	.long	3217031168
	.long	2748392742
	.long	1072683149
	.long	536870912
	.long	3163061750
	.long	3272206680
	.long	1066288081
	.long	0
	.long	3217031168
	.long	4251821691
	.long	1072680469
	.long	2147483648
	.long	3161841007
	.long	718380601
	.long	1063597621
	.long	0
	.long	3217031168
	.long	1893834707
	.long	1072677476
	.long	1610612736
	.long	1015147654
	.long	3987691371
	.long	3213074188
	.long	0
	.long	3217031168
	.long	1905645377
	.long	1072674169
	.long	1610612736
	.long	1014087596
	.long	1953696209
	.long	3214295171
	.long	0
	.long	3217031168
	.long	2131272983
	.long	1072670549
	.long	3221225472
	.long	3163005096
	.long	3299146087
	.long	3215021530
	.long	0
	.long	3217031168
	.long	617154971
	.long	1072666617
	.long	3758096384
	.long	3163433696
	.long	304938777
	.long	3215427665
	.long	0
	.long	3217031168
	.long	4201728937
	.long	1072662372
	.long	1610612736
	.long	1014674006
	.long	2034433221
	.long	3215832935
	.long	0
	.long	3217031168
	.long	2745245815
	.long	1072657817
	.long	3221225472
	.long	1015566129
	.long	240740309
	.long	1068244255
	.long	0
	.long	3218079744
	.long	3489094832
	.long	1072652951
	.long	536870912
	.long	1014325783
	.long	1765797566
	.long	1067840895
	.long	0
	.long	3218079744
	.long	990588717
	.long	1072647776
	.long	3758096384
	.long	3162041857
	.long	2886567144
	.long	1067426796
	.long	0
	.long	3218079744
	.long	2892293513
	.long	1072642291
	.long	2684354560
	.long	3162340271
	.long	3587565429
	.long	1066624384
	.long	0
	.long	3218079744
	.long	4151720666
	.long	1072636498
	.long	0
	.long	3162639544
	.long	2614369450
	.long	1065140432
	.long	0
	.long	3218079744
	.long	4220661975
	.long	1072630398
	.long	3221225472
	.long	1014312058
	.long	2382326656
	.long	3212045827
	.long	0
	.long	3218079744
	.long	2749658050
	.long	1072623992
	.long	1610612736
	.long	1015427338
	.long	723478390
	.long	3213958438
	.long	0
	.long	3218079744
	.long	3882371427
	.long	1072617280
	.long	2147483648
	.long	1014923975
	.long	4107121629
	.long	3214750787
	.long	0
	.long	3218079744
	.long	3370060660
	.long	1072610264
	.long	1610612736
	.long	1014787212
	.long	1991047213
	.long	3215237169
	.long	0
	.long	3218079744
	.long	1455828442
	.long	1072602945
	.long	3758096384
	.long	1015505073
	.long	386665102
	.long	3215630470
	.long	0
	.long	3218079744
	.long	2873938189
	.long	1072595323
	.long	1610612736
	.long	3162311422
	.long	1348311045
	.long	3216002418
	.long	0
	.long	3218079744
	.long	3964199186
	.long	1072587400
	.long	3758096384
	.long	3160501078
	.long	165911977
	.long	3216197513
	.long	0
	.long	3218079744
	.long	1261158782
	.long	1072579178
	.long	2147483648
	.long	1015711544
	.long	3601629044
	.long	3216391785
	.long	0
	.long	3218079744
	.long	83265253
	.long	1072570657
	.long	1610612736
	.long	3162510535
	.long	1962712856
	.long	3216585207
	.long	0
	.long	3218079744
	.long	1647164971
	.long	1072561838
	.long	2147483648
	.long	3162838250
	.long	3286832763
	.long	3216777748
	.long	0
	.long	3218079744
	.long	3066872380
	.long	1072552723
	.long	3221225472
	.long	1015561271
	.long	3296146332
	.long	3216969380
	.long	0
	.long	3218079744
	.long	1647878299
	.long	1072543314
	.long	3758096384
	.long	3162169063
	.long	1699043957
	.long	1069418613
	.long	0
	.long	3219128320
	.long	3476196678
	.long	1072533611
	.long	0
	.long	1014257638
	.long	1896722595
	.long	1069228886
	.long	0
	.long	3219128320
	.long	1942611595
	.long	1072523617
	.long	3221225472
	.long	3162410081
	.long	4049357271
	.long	1069040154
	.long	0
	.long	3219128320
	.long	3511535930
	.long	1072513332
	.long	2147483648
	.long	3162828626
	.long	1380647130
	.long	1068852447
	.long	0
	.long	3219128320
	.long	1950234076
	.long	1072502759
	.long	536870912
	.long	3159425498
	.long	3631910143
	.long	1068665791
	.long	0
	.long	3219128320
	.long	4097623923
	.long	1072491898
	.long	2684354560
	.long	3160308587
	.long	1073154251
	.long	1068461489
	.long	0
	.long	3219128320
	.long	4093443164
	.long	1072480752
	.long	536870912
	.long	1014912865
	.long	913431823
	.long	1068092555
	.long	0
	.long	3219128320
	.long	557060597
	.long	1072469323
	.long	0
	.long	3159554934
	.long	600368053
	.long	1067725893
	.long	0
	.long	3219128320
	.long	881357723
	.long	1072457611
	.long	3221225472
	.long	1013810890
	.long	2140183630
	.long	1067272748
	.long	0
	.long	3219128320
	.long	4051746225
	.long	1072445618
	.long	1610612736
	.long	3161907377
	.long	3506991783
	.long	1066548842
	.long	0
	.long	3219128320
	.long	645027145
	.long	1072433348
	.long	3221225472
	.long	1012602239
	.long	31019393
	.long	1065162441
	.long	0
	.long	3219128320
	.long	2892993834
	.long	1072420800
	.long	536870912
	.long	3163039143
	.long	1279178457
	.long	3211590791
	.long	0
	.long	3219128320
	.long	1731563730
	.long	1072407978
	.long	536870912
	.long	1015540302
	.long	1220541286
	.long	3213681126
	.long	0
	.long	3219128320
	.long	1159294526
	.long	1072394883
	.long	536870912
	.long	3163276959
	.long	2880516564
	.long	3214487049
	.long	0
	.long	3219128320
	.long	1056266002
	.long	1072381517
	.long	2684354560
	.long	3163469496
	.long	1102361128
	.long	3215059765
	.long	0
	.long	3219128320
	.long	1477771776
	.long	1072367882
	.long	536870912
	.long	3163104986
	.long	1038988426
	.long	3215406294
	.long	0
	.long	3219128320
	.long	2653017361
	.long	1072353980
	.long	0
	.long	3162800062
	.long	2598800519
	.long	3215750067
	.long	0
	.long	3219128320
	.long	688824739
	.long	1072339814
	.long	3758096384
	.long	1010431536
	.long	3382663878
	.long	3216036812
	.long	0
	.long	3219128320
	.long	453180130
	.long	1072325385
	.long	1073741824
	.long	3162411281
	.long	1726519029
	.long	3216205866
	.long	0
	.long	3219128320
	.long	2688952194
	.long	1072310695
	.long	0
	.long	3161532603
	.long	802660176
	.long	3216373465
	.long	0
	.long	3219128320
	.long	4012486548
	.long	1072295747
	.long	2684354560
	.long	1014701564
	.long	3876290983
	.long	3216539583
	.long	0
	.long	3219128320
	.long	1207142209
	.long	1072280544
	.long	2147483648
	.long	3162705634
	.long	2285031318
	.long	3216704197
	.long	0
	.long	3219128320
	.long	4106737474
	.long	1072265086
	.long	2684354560
	.long	3162592377
	.long	1224902090
	.long	3216867281
	.long	0
	.long	3219128320
	.long	1234330619
	.long	1072249378
	.long	2684354560
	.long	1015272473
	.long	2586490530
	.long	3217028810
	.long	0
	.long	3219128320
	.long	2750387213
	.long	1072233420
	.long	2147483648
	.long	3160858537
	.long	2485417816
	.long	3217109964
	.long	0
	.long	3219128320
	.long	1796544321
	.long	1072217216
	.long	536870912
	.long	3162686945
	.long	1852581091
	.long	3217189138
	.long	0
	.long	3219128320
	.long	263859903
	.long	1072200768
	.long	0
	.long	3162692284
	.long	4019165092
	.long	3217267498
	.long	0
	.long	3219128320
	.long	201299822
	.long	1072184078
	.long	3758096384
	.long	3161912352
	.long	1250776663
	.long	3217345034
	.long	0
	.long	3219128320
	.long	3814135665
	.long	1072167148
	.long	3221225472
	.long	3163146456
	.long	3526603391
	.long	3217421732
	.long	0
	.long	3219128320
	.long	577417135
	.long	1072149983
	.long	2684354560
	.long	3161519415
	.long	4187227697
	.long	3217497582
	.long	0
	.long	3219128320
	.long	1594061409
	.long	1072132583
	.long	2147483648
	.long	1014353870
	.long	1416934267
	.long	3217572573
	.long	0
	.long	3219128320
	.long	938475414
	.long	1072114952
	.long	0
	.long	1015076079
	.long	2546065654
	.long	3217646692
	.long	0
	.long	3219128320
	.long	1424664751
	.long	1072097092
	.long	3221225472
	.long	3163405315
	.long	2583490354
	.long	3217719929
	.long	0
	.long	3219128320
	.long	1719614413
	.long	1072079006
	.long	0
	.long	3163282740
	.long	1403691706
	.long	3217792273
	.long	0
	.long	3219128320
	.long	636516413
	.long	1072060697
	.long	3758096384
	.long	1015823716
	.long	3753901657
	.long	3217863712
	.long	0
	.long	3219128320
	.long	1427975391
	.long	1072042167
	.long	1073741824
	.long	3161608627
	.long	2081278341
	.long	3217934237
	.long	0
	.long	3219128320
	.long	3194290572
	.long	1072023419
	.long	2147483648
	.long	1015048682
	.long	2309668539
	.long	3218003836
	.long	0
	.long	3219128320
	.long	881650848
	.long	1072004457
	.long	2147483648
	.long	3162986272
	.long	2371640770
	.long	3218072499
	.long	0
	.long	3219128320
	.long	2460177814
	.long	1071985282
	.long	2147483648
	.long	3162116843
	.long	3489768009
	.long	1070535623
	.long	0
	.long	3220176896
	.long	3152274921
	.long	1071965898
	.long	3758096384
	.long	1013170835
	.long	3239527685
	.long	1070468863
	.long	0
	.long	3220176896
	.long	2610628921
	.long	1071946308
	.long	1610612736
	.long	3162668769
	.long	1403757309
	.long	1070403070
	.long	0
	.long	3220176896
	.long	621354454
	.long	1071926515
	.long	536870912
	.long	1013450602
	.long	1883353036
	.long	1070338253
	.long	0
	.long	3220176896
	.long	1397053140
	.long	1071906521
	.long	1610612736
	.long	1015245078
	.long	3652612115
	.long	1070274422
	.long	0
	.long	3220176896
	.long	689983673
	.long	1071886330
	.long	3758096384
	.long	1014906405
	.long	752919289
	.long	1070211588
	.long	0
	.long	3220176896
	.long	2969983475
	.long	1071865944
	.long	2684354560
	.long	3162635443
	.long	3761365757
	.long	1070149758
	.long	0
	.long	3220176896
	.long	4242632757
	.long	1071845367
	.long	536870912
	.long	3161390278
	.long	1129922991
	.long	1070088944
	.long	0
	.long	3220176896
	.long	637203434
	.long	1071824603
	.long	3221225472
	.long	3159443841
	.long	2129093113
	.long	1070029153
	.long	0
	.long	3220176896
	.long	1289556703
	.long	1071803653
	.long	536870912
	.long	3162888820
	.long	2482246777
	.long	1069970395
	.long	0
	.long	3220176896
	.long	2570316633
	.long	1071782521
	.long	1610612736
	.long	3162744753
	.long	1539668340
	.long	1069912679
	.long	0
	.long	3220176896
	.long	967731400
	.long	1071761211
	.long	536870912
	.long	1015752157
	.long	2272832445
	.long	1069856013
	.long	0
	.long	3220176896
	.long	1675549513
	.long	1071739725
	.long	3221225472
	.long	3163025138
	.long	2678847490
	.long	1069800406
	.long	0
	.long	3220176896
	.long	1411074851
	.long	1071718067
	.long	1610612736
	.long	3163484731
	.long	69903290
	.long	1069745867
	.long	0
	.long	3220176896
	.long	1297975695
	.long	1071696240
	.long	3221225472
	.long	3163083735
	.long	3952756490
	.long	1069692402
	.long	0
	.long	3220176896
	.long	2569207790
	.long	1071674247
	.long	1073741824
	.long	3163183617
	.long	1663679671
	.long	1069640022
	.long	0
	.long	3220176896
	.long	2269920951
	.long	1071652092
	.long	3758096384
	.long	3161672245
	.long	1312926554
	.long	1069588733
	.long	0
	.long	3220176896
	.long	3690502842
	.long	1071614884
	.long	3221225472
	.long	3157453855
	.long	3429717432
	.long	1069529566
	.long	0
	.long	3220176896
	.long	1391425750
	.long	1071569945
	.long	3221225472
	.long	3162362103
	.long	1945768569
	.long	1069431400
	.long	0
	.long	3220176896
	.long	939980347
	.long	1071524701
	.long	0
	.long	1012796809
	.long	1539072807
	.long	1069335462
	.long	0
	.long	3220176896
	.long	1535566729
	.long	1071479159
	.long	2147483648
	.long	1014321388
	.long	4133449816
	.long	1069241766
	.long	0
	.long	3220176896
	.long	2570175582
	.long	1071433326
	.long	1073741824
	.long	1012303118
	.long	1612193054
	.long	1069150328
	.long	0
	.long	3220176896
	.long	3623952103
	.long	1071387209
	.long	2684354560
	.long	3161985962
	.long	1578746984
	.long	1069061160
	.long	0
	.long	3220176896
	.long	165764288
	.long	1071340816
	.long	3221225472
	.long	1013298018
	.long	1577996576
	.long	1068974276
	.long	0
	.long	3220176896
	.long	728580042
	.long	1071294152
	.long	1610612736
	.long	3162332944
	.long	1972483635
	.long	1068889689
	.long	0
	.long	3220176896
	.long	1135144330
	.long	1071247225
	.long	536870912
	.long	1013373569
	.long	1638976426
	.long	1068807412
	.long	0
	.long	3220176896
	.long	1673302999
	.long	1071200042
	.long	2147483648
	.long	1013351473
	.long	2255197647
	.long	1068727457
	.long	0
	.long	3220176896
	.long	2796464483
	.long	1071152610
	.long	3221225472
	.long	3160878317
	.long	3996842794
	.long	1068649836
	.long	0
	.long	3220176896
	.long	824036583
	.long	1071104937
	.long	0
	.long	1008313330
	.long	1234824733
	.long	1068574562
	.long	0
	.long	3220176896
	.long	821708191
	.long	1071057029
	.long	3758096384
	.long	3161738009
	.long	4002549419
	.long	1068501644
	.long	0
	.long	3220176896
	.long	3711903686
	.long	1071008893
	.long	1073741824
	.long	1011944747
	.long	2962723252
	.long	1068363247
	.long	0
	.long	3220176896
	.long	1974149085
	.long	1070960538
	.long	3221225472
	.long	3156147648
	.long	1296295961
	.long	1068226907
	.long	0
	.long	3220176896
	.long	1115219818
	.long	1070911970
	.long	0
	.long	1013072242
	.long	997322465
	.long	1068095345
	.long	0
	.long	3220176896
	.long	2484561409
	.long	1070863196
	.long	1073741824
	.long	1014024572
	.long	1261629236
	.long	1067968581
	.long	0
	.long	3220176896
	.long	3269558610
	.long	1070814224
	.long	536870912
	.long	3161705216
	.long	2476548698
	.long	1067846634
	.long	0
	.long	3220176896
	.long	785751814
	.long	1070765062
	.long	2684354560
	.long	3161838221
	.long	1913604284
	.long	1067729523
	.long	0
	.long	3220176896
	.long	1062001470
	.long	1070715716
	.long	0
	.long	3160875220
	.long	2306566604
	.long	1067617265
	.long	0
	.long	3220176896
	.long	1655830135
	.long	1070666194
	.long	0
	.long	1014506690
	.long	3250111450
	.long	1067509877
	.long	0
	.long	3220176896
	.long	238550446
	.long	1070616504
	.long	2684354560
	.long	3154383565
	.long	2377770267
	.long	1067364384
	.long	0
	.long	3220176896
	.long	2065784603
	.long	1070537209
	.long	3758096384
	.long	3160681966
	.long	288924873
	.long	1067169185
	.long	0
	.long	3220176896
	.long	3846521617
	.long	1070437199
	.long	536870912
	.long	1013406610
	.long	1935888103
	.long	1066983818
	.long	0
	.long	3220176896
	.long	1786662755
	.long	1070336898
	.long	2147483648
	.long	3155882307
	.long	2660899430
	.long	1066808312
	.long	0
	.long	3220176896
	.long	632292433
	.long	1070236320
	.long	3758096384
	.long	3160963333
	.long	18115067
	.long	1066642694
	.long	0
	.long	3220176896
	.long	1013556747
	.long	1070135480
	.long	2684354560
	.long	3160567065
	.long	2346447124
	.long	1066486987
	.long	0
	.long	3220176896
	.long	3729833777
	.long	1070034393
	.long	3758096384
	.long	1013044718
	.long	1672287667
	.long	1066280641
	.long	0
	.long	3220176896
	.long	1149976518
	.long	1069933076
	.long	1610612736
	.long	1013264895
	.long	3451754846
	.long	1066009014
	.long	0
	.long	3220176896
	.long	2972270170
	.long	1069831542
	.long	1073741824
	.long	3159524770
	.long	1037482584
	.long	1065757344
	.long	0
	.long	3220176896
	.long	1854824762
	.long	1069729808
	.long	2684354560
	.long	1011953664
	.long	2592747447
	.long	1065525667
	.long	0
	.long	3220176896
	.long	3470456183
	.long	1069627888
	.long	1073741824
	.long	3160744771
	.long	2402293340
	.long	1065274823
	.long	0
	.long	3220176896
	.long	1453945614
	.long	1069504078
	.long	1610612736
	.long	1011492612
	.long	1227670166
	.long	1064891649
	.long	0
	.long	3220176896
	.long	3349070549
	.long	1069299589
	.long	3758096384
	.long	3156538269
	.long	393047345
	.long	1064548654
	.long	0
	.long	3220176896
	.long	3156849708
	.long	1069094822
	.long	3221225472
	.long	3158189848
	.long	1118107366
	.long	1064187139
	.long	0
	.long	3220176896
	.long	177906713
	.long	1068889808
	.long	3221225472
	.long	3159310370
	.long	4269262315
	.long	1063662162
	.long	0
	.long	3220176896
	.long	2462980598
	.long	1068684576
	.long	2684354560
	.long	3158941832
	.long	3810305407
	.long	1063179572
	.long	0
	.long	3220176896
	.long	2046304480
	.long	1068459374
	.long	3758096384
	.long	3156402667
	.long	220529721
	.long	1062452281
	.long	0
	.long	3220176896
	.long	4044216340
	.long	1068048229
	.long	0
	.long	3156808381
	.long	69373323
	.long	1061565504
	.long	0
	.long	3220176896
	.long	1972655565
	.long	1067636837
	.long	1073741824
	.long	1009157878
	.long	3650591271
	.long	1060355323
	.long	0
	.long	3220176896
	.long	4154680958
	.long	1067000149
	.long	0
	.long	3153796451
	.long	2376373521
	.long	1058258220
	.long	0
	.long	3220176896
	.long	4242458500
	.long	1065951697
	.long	2684354560
	.long	1009354638
	.long	0
	.long	0
	.long	0
	.long	3220176896
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2376373521
	.long	1058258220
	.long	0
	.long	3220176896
	.long	4242458500
	.long	3213435345
	.long	2684354560
	.long	3156838286
	.long	3650591271
	.long	1060355323
	.long	0
	.long	3220176896
	.long	4154680958
	.long	3214483797
	.long	0
	.long	1006312803
	.long	69373323
	.long	1061565504
	.long	0
	.long	3220176896
	.long	1972655565
	.long	3215120485
	.long	1073741824
	.long	3156641526
	.long	220529721
	.long	1062452281
	.long	0
	.long	3220176896
	.long	4044216340
	.long	3215531877
	.long	0
	.long	1009324733
	.long	3810305407
	.long	1063179572
	.long	0
	.long	3220176896
	.long	2046304480
	.long	3215943022
	.long	3758096384
	.long	1008919019
	.long	4269262315
	.long	1063662162
	.long	0
	.long	3220176896
	.long	2462980598
	.long	3216168224
	.long	2684354560
	.long	1011458184
	.long	1118107366
	.long	1064187139
	.long	0
	.long	3220176896
	.long	177906713
	.long	3216373456
	.long	3221225472
	.long	1011826722
	.long	393047345
	.long	1064548654
	.long	0
	.long	3220176896
	.long	3156849708
	.long	3216578470
	.long	3221225472
	.long	1010706200
	.long	1227670166
	.long	1064891649
	.long	0
	.long	3220176896
	.long	3349070549
	.long	3216783237
	.long	3758096384
	.long	1009054621
	.long	2402293340
	.long	1065274823
	.long	0
	.long	3220176896
	.long	1453945614
	.long	3216987726
	.long	1610612736
	.long	3158976260
	.long	2592747447
	.long	1065525667
	.long	0
	.long	3220176896
	.long	3470456183
	.long	3217111536
	.long	1073741824
	.long	1013261123
	.long	1037482584
	.long	1065757344
	.long	0
	.long	3220176896
	.long	1854824762
	.long	3217213456
	.long	2684354560
	.long	3159437312
	.long	3451754846
	.long	1066009014
	.long	0
	.long	3220176896
	.long	2972270170
	.long	3217315190
	.long	1073741824
	.long	1012041122
	.long	1672287667
	.long	1066280641
	.long	0
	.long	3220176896
	.long	1149976518
	.long	3217416724
	.long	1610612736
	.long	3160748543
	.long	2346447124
	.long	1066486987
	.long	0
	.long	3220176896
	.long	3729833777
	.long	3217518041
	.long	3758096384
	.long	3160528366
	.long	18115067
	.long	1066642694
	.long	0
	.long	3220176896
	.long	1013556747
	.long	3217619128
	.long	2684354560
	.long	1013083417
	.long	2660899430
	.long	1066808312
	.long	0
	.long	3220176896
	.long	632292433
	.long	3217719968
	.long	3758096384
	.long	1013479685
	.long	1935888103
	.long	1066983818
	.long	0
	.long	3220176896
	.long	1786662755
	.long	3217820546
	.long	2147483648
	.long	1008398659
	.long	288924873
	.long	1067169185
	.long	0
	.long	3220176896
	.long	3846521617
	.long	3217920847
	.long	536870912
	.long	3160890258
	.long	2377770267
	.long	1067364384
	.long	0
	.long	3220176896
	.long	2065784603
	.long	3218020857
	.long	3758096384
	.long	1013198318
	.long	3250111450
	.long	1067509877
	.long	0
	.long	3220176896
	.long	238550446
	.long	3218100152
	.long	2684354560
	.long	1006899917
	.long	2306566604
	.long	1067617265
	.long	0
	.long	3220176896
	.long	1655830135
	.long	3218149842
	.long	0
	.long	3161990338
	.long	1913604284
	.long	1067729523
	.long	0
	.long	3220176896
	.long	1062001470
	.long	3218199364
	.long	0
	.long	1013391572
	.long	2476548698
	.long	1067846634
	.long	0
	.long	3220176896
	.long	785751814
	.long	3218248710
	.long	2684354560
	.long	1014354573
	.long	1261629236
	.long	1067968581
	.long	0
	.long	3220176896
	.long	3269558610
	.long	3218297872
	.long	536870912
	.long	1014221568
	.long	997322465
	.long	1068095345
	.long	0
	.long	3220176896
	.long	2484561409
	.long	3218346844
	.long	1073741824
	.long	3161508220
	.long	1296295961
	.long	1068226907
	.long	0
	.long	3220176896
	.long	1115219818
	.long	3218395618
	.long	0
	.long	3160555890
	.long	2962723252
	.long	1068363247
	.long	0
	.long	3220176896
	.long	1974149085
	.long	3218444186
	.long	3221225472
	.long	1008664000
	.long	4002549419
	.long	1068501644
	.long	0
	.long	3220176896
	.long	3711903686
	.long	3218492541
	.long	1073741824
	.long	3159428395
	.long	1234824733
	.long	1068574562
	.long	0
	.long	3220176896
	.long	821708191
	.long	3218540677
	.long	3758096384
	.long	1014254361
	.long	3996842794
	.long	1068649836
	.long	0
	.long	3220176896
	.long	824036583
	.long	3218588585
	.long	0
	.long	3155796978
	.long	2255197647
	.long	1068727457
	.long	0
	.long	3220176896
	.long	2796464483
	.long	3218636258
	.long	3221225472
	.long	1013394669
	.long	1638976426
	.long	1068807412
	.long	0
	.long	3220176896
	.long	1673302999
	.long	3218683690
	.long	2147483648
	.long	3160835121
	.long	1972483635
	.long	1068889689
	.long	0
	.long	3220176896
	.long	1135144330
	.long	3218730873
	.long	536870912
	.long	3160857217
	.long	1577996576
	.long	1068974276
	.long	0
	.long	3220176896
	.long	728580042
	.long	3218777800
	.long	1610612736
	.long	1014849296
	.long	1578746984
	.long	1069061160
	.long	0
	.long	3220176896
	.long	165764288
	.long	3218824464
	.long	3221225472
	.long	3160781666
	.long	1612193054
	.long	1069150328
	.long	0
	.long	3220176896
	.long	3623952103
	.long	3218870857
	.long	2684354560
	.long	1014502314
	.long	4133449816
	.long	1069241766
	.long	0
	.long	3220176896
	.long	2570175582
	.long	3218916974
	.long	1073741824
	.long	3159786766
	.long	1539072807
	.long	1069335462
	.long	0
	.long	3220176896
	.long	1535566729
	.long	3218962807
	.long	2147483648
	.long	3161805036
	.long	1945768569
	.long	1069431400
	.long	0
	.long	3220176896
	.long	939980347
	.long	3219008349
	.long	0
	.long	3160280457
	.long	3429717432
	.long	1069529566
	.long	0
	.long	3220176896
	.long	1391425750
	.long	3219053593
	.long	3221225472
	.long	1014878455
	.long	1312926554
	.long	1069588733
	.long	0
	.long	3220176896
	.long	3690502842
	.long	3219098532
	.long	3221225472
	.long	1009970207
	.long	1663679671
	.long	1069640022
	.long	0
	.long	3220176896
	.long	2269920951
	.long	3219135740
	.long	3758096384
	.long	1014188597
	.long	3952756490
	.long	1069692402
	.long	0
	.long	3220176896
	.long	2569207790
	.long	3219157895
	.long	1073741824
	.long	1015699969
	.long	69903290
	.long	1069745867
	.long	0
	.long	3220176896
	.long	1297975695
	.long	3219179888
	.long	3221225472
	.long	1015600087
	.long	2678847490
	.long	1069800406
	.long	0
	.long	3220176896
	.long	1411074851
	.long	3219201715
	.long	1610612736
	.long	1016001083
	.long	2272832445
	.long	1069856013
	.long	0
	.long	3220176896
	.long	1675549513
	.long	3219223373
	.long	3221225472
	.long	1015541490
	.long	1539668340
	.long	1069912679
	.long	0
	.long	3220176896
	.long	967731400
	.long	3219244859
	.long	536870912
	.long	3163235805
	.long	2482246777
	.long	1069970395
	.long	0
	.long	3220176896
	.long	2570316633
	.long	3219266169
	.long	1610612736
	.long	1015261105
	.long	2129093113
	.long	1070029153
	.long	0
	.long	3220176896
	.long	1289556703
	.long	3219287301
	.long	536870912
	.long	1015405172
	.long	1129922991
	.long	1070088944
	.long	0
	.long	3220176896
	.long	637203434
	.long	3219308251
	.long	3221225472
	.long	1011960193
	.long	3761365757
	.long	1070149758
	.long	0
	.long	3220176896
	.long	4242632757
	.long	3219329015
	.long	536870912
	.long	1013906630
	.long	752919289
	.long	1070211588
	.long	0
	.long	3220176896
	.long	2969983475
	.long	3219349592
	.long	2684354560
	.long	1015151795
	.long	3652612115
	.long	1070274422
	.long	0
	.long	3220176896
	.long	689983673
	.long	3219369978
	.long	3758096384
	.long	3162390053
	.long	1883353036
	.long	1070338253
	.long	0
	.long	3220176896
	.long	1397053140
	.long	3219390169
	.long	1610612736
	.long	3162728726
	.long	1403757309
	.long	1070403070
	.long	0
	.long	3220176896
	.long	621354454
	.long	3219410163
	.long	536870912
	.long	3160934250
	.long	3239527685
	.long	1070468863
	.long	0
	.long	3220176896
	.long	2610628921
	.long	3219429956
	.long	1610612736
	.long	1015185121
	.long	3489768009
	.long	1070535623
	.long	0
	.long	3220176896
	.long	3152274921
	.long	3219449546
	.long	3758096384
	.long	3160654483
	.long	2371640770
	.long	3218072499
	.long	0
	.long	3219128320
	.long	2460177814
	.long	3219468930
	.long	2147483648
	.long	1014633195
	.long	2309668539
	.long	3218003836
	.long	0
	.long	3219128320
	.long	881650848
	.long	3219488105
	.long	2147483648
	.long	1015502624
	.long	2081278341
	.long	3217934237
	.long	0
	.long	3219128320
	.long	3194290572
	.long	3219507067
	.long	2147483648
	.long	3162532330
	.long	3753901657
	.long	3217863712
	.long	0
	.long	3219128320
	.long	1427975391
	.long	3219525815
	.long	1073741824
	.long	1014124979
	.long	1403691706
	.long	3217792273
	.long	0
	.long	3219128320
	.long	636516413
	.long	3219544345
	.long	3758096384
	.long	3163307364
	.long	2583490354
	.long	3217719929
	.long	0
	.long	3219128320
	.long	1719614413
	.long	3219562654
	.long	0
	.long	1015799092
	.long	2546065654
	.long	3217646692
	.long	0
	.long	3219128320
	.long	1424664751
	.long	3219580740
	.long	3221225472
	.long	1015921667
	.long	1416934267
	.long	3217572573
	.long	0
	.long	3219128320
	.long	938475414
	.long	3219598600
	.long	0
	.long	3162559727
	.long	4187227697
	.long	3217497582
	.long	0
	.long	3219128320
	.long	1594061409
	.long	3219616231
	.long	2147483648
	.long	3161837518
	.long	3526603391
	.long	3217421732
	.long	0
	.long	3219128320
	.long	577417135
	.long	3219633631
	.long	2684354560
	.long	1014035767
	.long	1250776663
	.long	3217345034
	.long	0
	.long	3219128320
	.long	3814135665
	.long	3219650796
	.long	3221225472
	.long	1015662808
	.long	4019165092
	.long	3217267498
	.long	0
	.long	3219128320
	.long	201299822
	.long	3219667726
	.long	3758096384
	.long	1014428704
	.long	1852581091
	.long	3217189138
	.long	0
	.long	3219128320
	.long	263859903
	.long	3219684416
	.long	0
	.long	1015208636
	.long	2485417816
	.long	3217109964
	.long	0
	.long	3219128320
	.long	1796544321
	.long	3219700864
	.long	536870912
	.long	1015203297
	.long	2586490530
	.long	3217028810
	.long	0
	.long	3219128320
	.long	2750387213
	.long	3219717068
	.long	2147483648
	.long	1013374889
	.long	1224902090
	.long	3216867281
	.long	0
	.long	3219128320
	.long	1234330619
	.long	3219733026
	.long	2684354560
	.long	3162756121
	.long	2285031318
	.long	3216704197
	.long	0
	.long	3219128320
	.long	4106737474
	.long	3219748734
	.long	2684354560
	.long	1015108729
	.long	3876290983
	.long	3216539583
	.long	0
	.long	3219128320
	.long	1207142209
	.long	3219764192
	.long	2147483648
	.long	1015221986
	.long	802660176
	.long	3216373465
	.long	0
	.long	3219128320
	.long	4012486548
	.long	3219779395
	.long	2684354560
	.long	3162185212
	.long	1726519029
	.long	3216205866
	.long	0
	.long	3219128320
	.long	2688952194
	.long	3219794343
	.long	0
	.long	1014048955
	.long	3382663878
	.long	3216036812
	.long	0
	.long	3219128320
	.long	453180130
	.long	3219809033
	.long	1073741824
	.long	1014927633
	.long	2598800519
	.long	3215750067
	.long	0
	.long	3219128320
	.long	688824739
	.long	3219823462
	.long	3758096384
	.long	3157915184
	.long	1038988426
	.long	3215406294
	.long	0
	.long	3219128320
	.long	2653017361
	.long	3219837628
	.long	0
	.long	1015316414
	.long	1102361128
	.long	3215059765
	.long	0
	.long	3219128320
	.long	1477771776
	.long	3219851530
	.long	536870912
	.long	1015621338
	.long	2880516564
	.long	3214487049
	.long	0
	.long	3219128320
	.long	1056266002
	.long	3219865165
	.long	2684354560
	.long	1015985848
	.long	1220541286
	.long	3213681126
	.long	0
	.long	3219128320
	.long	1159294526
	.long	3219878531
	.long	536870912
	.long	1015793311
	.long	1279178457
	.long	3211590791
	.long	0
	.long	3219128320
	.long	1731563730
	.long	3219891626
	.long	536870912
	.long	3163023950
	.long	31019393
	.long	1065162441
	.long	0
	.long	3219128320
	.long	2892993834
	.long	3219904448
	.long	536870912
	.long	1015555495
	.long	3506991783
	.long	1066548842
	.long	0
	.long	3219128320
	.long	645027145
	.long	3219916996
	.long	3221225472
	.long	3160085887
	.long	2140183630
	.long	1067272748
	.long	0
	.long	3219128320
	.long	4051746225
	.long	3219929266
	.long	1610612736
	.long	1014423729
	.long	600368053
	.long	1067725893
	.long	0
	.long	3219128320
	.long	881357723
	.long	3219941259
	.long	3221225472
	.long	3161294538
	.long	913431823
	.long	1068092555
	.long	0
	.long	3219128320
	.long	557060597
	.long	3219952971
	.long	0
	.long	1012071286
	.long	1073154251
	.long	1068461489
	.long	0
	.long	3219128320
	.long	4093443164
	.long	3219964400
	.long	536870912
	.long	3162396513
	.long	3631910143
	.long	1068665791
	.long	0
	.long	3219128320
	.long	4097623923
	.long	3219975546
	.long	2684354560
	.long	1012824939
	.long	1380647130
	.long	1068852447
	.long	0
	.long	3219128320
	.long	1950234076
	.long	3219986407
	.long	536870912
	.long	1011941850
	.long	4049357271
	.long	1069040154
	.long	0
	.long	3219128320
	.long	3511535930
	.long	3219996980
	.long	2147483648
	.long	1015344978
	.long	1896722595
	.long	1069228886
	.long	0
	.long	3219128320
	.long	1942611595
	.long	3220007265
	.long	3221225472
	.long	1014926433
	.long	1699043957
	.long	1069418613
	.long	0
	.long	3219128320
	.long	3476196678
	.long	3220017259
	.long	0
	.long	3161741286
	.long	3296146332
	.long	3216969380
	.long	0
	.long	3218079744
	.long	1647878299
	.long	3220026962
	.long	3758096384
	.long	1014685415
	.long	3286832763
	.long	3216777748
	.long	0
	.long	3218079744
	.long	3066872380
	.long	3220036371
	.long	3221225472
	.long	3163044919
	.long	1962712856
	.long	3216585207
	.long	0
	.long	3218079744
	.long	1647164971
	.long	3220045486
	.long	2147483648
	.long	1015354602
	.long	3601629044
	.long	3216391785
	.long	0
	.long	3218079744
	.long	83265253
	.long	3220054305
	.long	1610612736
	.long	1015026887
	.long	165911977
	.long	3216197513
	.long	0
	.long	3218079744
	.long	1261158782
	.long	3220062826
	.long	2147483648
	.long	3163195192
	.long	1348311045
	.long	3216002418
	.long	0
	.long	3218079744
	.long	3964199186
	.long	3220071048
	.long	3758096384
	.long	1013017430
	.long	386665102
	.long	3215630470
	.long	0
	.long	3218079744
	.long	2873938189
	.long	3220078971
	.long	1610612736
	.long	1014827774
	.long	1991047213
	.long	3215237169
	.long	0
	.long	3218079744
	.long	1455828442
	.long	3220086593
	.long	3758096384
	.long	3162988721
	.long	4107121629
	.long	3214750787
	.long	0
	.long	3218079744
	.long	3370060660
	.long	3220093912
	.long	1610612736
	.long	3162270860
	.long	723478390
	.long	3213958438
	.long	0
	.long	3218079744
	.long	3882371427
	.long	3220100928
	.long	2147483648
	.long	3162407623
	.long	2382326656
	.long	3212045827
	.long	0
	.long	3218079744
	.long	2749658050
	.long	3220107640
	.long	1610612736
	.long	3162910986
	.long	2614369450
	.long	1065140432
	.long	0
	.long	3218079744
	.long	4220661975
	.long	3220114046
	.long	3221225472
	.long	3161795706
	.long	3587565429
	.long	1066624384
	.long	0
	.long	3218079744
	.long	4151720666
	.long	3220120146
	.long	0
	.long	1015155896
	.long	2886567144
	.long	1067426796
	.long	0
	.long	3218079744
	.long	2892293513
	.long	3220125939
	.long	2684354560
	.long	1014856623
	.long	1765797566
	.long	1067840895
	.long	0
	.long	3218079744
	.long	990588717
	.long	3220131424
	.long	3758096384
	.long	1014558209
	.long	240740309
	.long	1068244255
	.long	0
	.long	3218079744
	.long	3489094832
	.long	3220136599
	.long	536870912
	.long	3161809431
	.long	2034433221
	.long	3215832935
	.long	0
	.long	3217031168
	.long	2745245815
	.long	3220141465
	.long	3221225472
	.long	3163049777
	.long	304938777
	.long	3215427665
	.long	0
	.long	3217031168
	.long	4201728937
	.long	3220146020
	.long	1610612736
	.long	3162157654
	.long	3299146087
	.long	3215021530
	.long	0
	.long	3217031168
	.long	617154971
	.long	3220150265
	.long	3758096384
	.long	1015950048
	.long	1953696209
	.long	3214295171
	.long	0
	.long	3217031168
	.long	2131272983
	.long	3220154197
	.long	3221225472
	.long	1015521448
	.long	3987691371
	.long	3213074188
	.long	0
	.long	3217031168
	.long	1905645377
	.long	3220157817
	.long	1610612736
	.long	3161571244
	.long	718380601
	.long	1063597621
	.long	0
	.long	3217031168
	.long	1893834707
	.long	3220161124
	.long	1610612736
	.long	3162631302
	.long	3272206680
	.long	1066288081
	.long	0
	.long	3217031168
	.long	4251821691
	.long	3220164117
	.long	2147483648
	.long	1014357359
	.long	257503056
	.long	1067164005
	.long	0
	.long	3217031168
	.long	2748392742
	.long	3220166797
	.long	536870912
	.long	1015578102
	.long	711626851
	.long	3214400320
	.long	0
	.long	3215982592
	.long	4240268041
	.long	3220169162
	.long	536870912
	.long	3161072611
	.long	2523975598
	.long	3213273348
	.long	0
	.long	3215982592
	.long	2902558778
	.long	3220171213
	.long	3221225472
	.long	3160744428
	.long	3237733378
	.long	1062425136
	.long	0
	.long	3215982592
	.long	1703834029
	.long	3220172949
	.long	3758096384
	.long	3162512012
	.long	1003003824
	.long	1066107496
	.long	0
	.long	3215982592
	.long	3816385918
	.long	3220174369
	.long	536870912
	.long	1013733308
	.long	2896342633
	.long	3212231467
	.long	0
	.long	3214934016
	.long	4026464093
	.long	3220175474
	.long	3221225472
	.long	1015592141
	.long	561145352
	.long	1065056936
	.long	0
	.long	3214934016
	.long	1619315981
	.long	3220176264
	.long	1073741824
	.long	1015100749
	.long	210035182
	.long	1064007864
	.long	0
	.long	3213885440
	.long	379294427
	.long	3220176738
	.long	3221225472
	.long	3162889635
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	3220176896
	.long	0
	.long	0
	.long	210035182
	.long	3211491512
	.long	0
	.long	1066401792
	.long	379294427
	.long	3220176738
	.long	3221225472
	.long	3162889635
	.long	561145352
	.long	3212540584
	.long	0
	.long	1067450368
	.long	1619315981
	.long	3220176264
	.long	1073741824
	.long	1015100749
	.long	2896342633
	.long	1064747819
	.long	0
	.long	1067450368
	.long	4026464093
	.long	3220175474
	.long	3221225472
	.long	1015592141
	.long	1003003824
	.long	3213591144
	.long	0
	.long	1068498944
	.long	3816385918
	.long	3220174369
	.long	536870912
	.long	1013733308
	.long	3237733378
	.long	3209908784
	.long	0
	.long	1068498944
	.long	1703834029
	.long	3220172949
	.long	3758096384
	.long	3162512012
	.long	2523975598
	.long	1065789700
	.long	0
	.long	1068498944
	.long	2902558778
	.long	3220171213
	.long	3221225472
	.long	3160744428
	.long	711626851
	.long	1066916672
	.long	0
	.long	1068498944
	.long	4240268041
	.long	3220169162
	.long	536870912
	.long	3161072611
	.long	257503056
	.long	3214647653
	.long	0
	.long	1069547520
	.long	2748392742
	.long	3220166797
	.long	536870912
	.long	1015578102
	.long	3272206680
	.long	3213771729
	.long	0
	.long	1069547520
	.long	4251821691
	.long	3220164117
	.long	2147483648
	.long	1014357359
	.long	718380601
	.long	3211081269
	.long	0
	.long	1069547520
	.long	1893834707
	.long	3220161124
	.long	1610612736
	.long	3162631302
	.long	3987691371
	.long	1065590540
	.long	0
	.long	1069547520
	.long	1905645377
	.long	3220157817
	.long	1610612736
	.long	3161571244
	.long	1953696209
	.long	1066811523
	.long	0
	.long	1069547520
	.long	2131272983
	.long	3220154197
	.long	3221225472
	.long	1015521448
	.long	3299146087
	.long	1067537882
	.long	0
	.long	1069547520
	.long	617154971
	.long	3220150265
	.long	3758096384
	.long	1015950048
	.long	304938777
	.long	1067944017
	.long	0
	.long	1069547520
	.long	4201728937
	.long	3220146020
	.long	1610612736
	.long	3162157654
	.long	2034433221
	.long	1068349287
	.long	0
	.long	1069547520
	.long	2745245815
	.long	3220141465
	.long	3221225472
	.long	3163049777
	.long	240740309
	.long	3215727903
	.long	0
	.long	1070596096
	.long	3489094832
	.long	3220136599
	.long	536870912
	.long	3161809431
	.long	1765797566
	.long	3215324543
	.long	0
	.long	1070596096
	.long	990588717
	.long	3220131424
	.long	3758096384
	.long	1014558209
	.long	2886567144
	.long	3214910444
	.long	0
	.long	1070596096
	.long	2892293513
	.long	3220125939
	.long	2684354560
	.long	1014856623
	.long	3587565429
	.long	3214108032
	.long	0
	.long	1070596096
	.long	4151720666
	.long	3220120146
	.long	0
	.long	1015155896
	.long	2614369450
	.long	3212624080
	.long	0
	.long	1070596096
	.long	4220661975
	.long	3220114046
	.long	3221225472
	.long	3161795706
	.long	2382326656
	.long	1064562179
	.long	0
	.long	1070596096
	.long	2749658050
	.long	3220107640
	.long	1610612736
	.long	3162910986
	.long	723478390
	.long	1066474790
	.long	0
	.long	1070596096
	.long	3882371427
	.long	3220100928
	.long	2147483648
	.long	3162407623
	.long	4107121629
	.long	1067267139
	.long	0
	.long	1070596096
	.long	3370060660
	.long	3220093912
	.long	1610612736
	.long	3162270860
	.long	1991047213
	.long	1067753521
	.long	0
	.long	1070596096
	.long	1455828442
	.long	3220086593
	.long	3758096384
	.long	3162988721
	.long	386665102
	.long	1068146822
	.long	0
	.long	1070596096
	.long	2873938189
	.long	3220078971
	.long	1610612736
	.long	1014827774
	.long	1348311045
	.long	1068518770
	.long	0
	.long	1070596096
	.long	3964199186
	.long	3220071048
	.long	3758096384
	.long	1013017430
	.long	165911977
	.long	1068713865
	.long	0
	.long	1070596096
	.long	1261158782
	.long	3220062826
	.long	2147483648
	.long	3163195192
	.long	3601629044
	.long	1068908137
	.long	0
	.long	1070596096
	.long	83265253
	.long	3220054305
	.long	1610612736
	.long	1015026887
	.long	1962712856
	.long	1069101559
	.long	0
	.long	1070596096
	.long	1647164971
	.long	3220045486
	.long	2147483648
	.long	1015354602
	.long	3286832763
	.long	1069294100
	.long	0
	.long	1070596096
	.long	3066872380
	.long	3220036371
	.long	3221225472
	.long	3163044919
	.long	3296146332
	.long	1069485732
	.long	0
	.long	1070596096
	.long	1647878299
	.long	3220026962
	.long	3758096384
	.long	1014685415
	.long	1699043957
	.long	3216902261
	.long	0
	.long	1071644672
	.long	3476196678
	.long	3220017259
	.long	0
	.long	3161741286
	.long	1896722595
	.long	3216712534
	.long	0
	.long	1071644672
	.long	1942611595
	.long	3220007265
	.long	3221225472
	.long	1014926433
	.long	4049357271
	.long	3216523802
	.long	0
	.long	1071644672
	.long	3511535930
	.long	3219996980
	.long	2147483648
	.long	1015344978
	.long	1380647130
	.long	3216336095
	.long	0
	.long	1071644672
	.long	1950234076
	.long	3219986407
	.long	536870912
	.long	1011941850
	.long	3631910143
	.long	3216149439
	.long	0
	.long	1071644672
	.long	4097623923
	.long	3219975546
	.long	2684354560
	.long	1012824939
	.long	1073154251
	.long	3215945137
	.long	0
	.long	1071644672
	.long	4093443164
	.long	3219964400
	.long	536870912
	.long	3162396513
	.long	913431823
	.long	3215576203
	.long	0
	.long	1071644672
	.long	557060597
	.long	3219952971
	.long	0
	.long	1012071286
	.long	600368053
	.long	3215209541
	.long	0
	.long	1071644672
	.long	881357723
	.long	3219941259
	.long	3221225472
	.long	3161294538
	.long	2140183630
	.long	3214756396
	.long	0
	.long	1071644672
	.long	4051746225
	.long	3219929266
	.long	1610612736
	.long	1014423729
	.long	3506991783
	.long	3214032490
	.long	0
	.long	1071644672
	.long	645027145
	.long	3219916996
	.long	3221225472
	.long	3160085887
	.long	31019393
	.long	3212646089
	.long	0
	.long	1071644672
	.long	2892993834
	.long	3219904448
	.long	536870912
	.long	1015555495
	.long	1279178457
	.long	1064107143
	.long	0
	.long	1071644672
	.long	1731563730
	.long	3219891626
	.long	536870912
	.long	3163023950
	.long	1220541286
	.long	1066197478
	.long	0
	.long	1071644672
	.long	1159294526
	.long	3219878531
	.long	536870912
	.long	1015793311
	.long	2880516564
	.long	1067003401
	.long	0
	.long	1071644672
	.long	1056266002
	.long	3219865165
	.long	2684354560
	.long	1015985848
	.long	1102361128
	.long	1067576117
	.long	0
	.long	1071644672
	.long	1477771776
	.long	3219851530
	.long	536870912
	.long	1015621338
	.long	1038988426
	.long	1067922646
	.long	0
	.long	1071644672
	.long	2653017361
	.long	3219837628
	.long	0
	.long	1015316414
	.long	2598800519
	.long	1068266419
	.long	0
	.long	1071644672
	.long	688824739
	.long	3219823462
	.long	3758096384
	.long	3157915184
	.long	3382663878
	.long	1068553164
	.long	0
	.long	1071644672
	.long	453180130
	.long	3219809033
	.long	1073741824
	.long	1014927633
	.long	1726519029
	.long	1068722218
	.long	0
	.long	1071644672
	.long	2688952194
	.long	3219794343
	.long	0
	.long	1014048955
	.long	802660176
	.long	1068889817
	.long	0
	.long	1071644672
	.long	4012486548
	.long	3219779395
	.long	2684354560
	.long	3162185212
	.long	3876290983
	.long	1069055935
	.long	0
	.long	1071644672
	.long	1207142209
	.long	3219764192
	.long	2147483648
	.long	1015221986
	.long	2285031318
	.long	1069220549
	.long	0
	.long	1071644672
	.long	4106737474
	.long	3219748734
	.long	2684354560
	.long	1015108729
	.long	1224902090
	.long	1069383633
	.long	0
	.long	1071644672
	.long	1234330619
	.long	3219733026
	.long	2684354560
	.long	3162756121
	.long	2586490530
	.long	1069545162
	.long	0
	.long	1071644672
	.long	2750387213
	.long	3219717068
	.long	2147483648
	.long	1013374889
	.long	2485417816
	.long	1069626316
	.long	0
	.long	1071644672
	.long	1796544321
	.long	3219700864
	.long	536870912
	.long	1015203297
	.long	1852581091
	.long	1069705490
	.long	0
	.long	1071644672
	.long	263859903
	.long	3219684416
	.long	0
	.long	1015208636
	.long	4019165092
	.long	1069783850
	.long	0
	.long	1071644672
	.long	201299822
	.long	3219667726
	.long	3758096384
	.long	1014428704
	.long	1250776663
	.long	1069861386
	.long	0
	.long	1071644672
	.long	3814135665
	.long	3219650796
	.long	3221225472
	.long	1015662808
	.long	3526603391
	.long	1069938084
	.long	0
	.long	1071644672
	.long	577417135
	.long	3219633631
	.long	2684354560
	.long	1014035767
	.long	4187227697
	.long	1070013934
	.long	0
	.long	1071644672
	.long	1594061409
	.long	3219616231
	.long	2147483648
	.long	3161837518
	.long	1416934267
	.long	1070088925
	.long	0
	.long	1071644672
	.long	938475414
	.long	3219598600
	.long	0
	.long	3162559727
	.long	2546065654
	.long	1070163044
	.long	0
	.long	1071644672
	.long	1424664751
	.long	3219580740
	.long	3221225472
	.long	1015921667
	.long	2583490354
	.long	1070236281
	.long	0
	.long	1071644672
	.long	1719614413
	.long	3219562654
	.long	0
	.long	1015799092
	.long	1403691706
	.long	1070308625
	.long	0
	.long	1071644672
	.long	636516413
	.long	3219544345
	.long	3758096384
	.long	3163307364
	.long	3753901657
	.long	1070380064
	.long	0
	.long	1071644672
	.long	1427975391
	.long	3219525815
	.long	1073741824
	.long	1014124979
	.long	2081278341
	.long	1070450589
	.long	0
	.long	1071644672
	.long	3194290572
	.long	3219507067
	.long	2147483648
	.long	3162532330
	.long	2309668539
	.long	1070520188
	.long	0
	.long	1071644672
	.long	881650848
	.long	3219488105
	.long	2147483648
	.long	1015502624
	.long	2371640770
	.long	1070588851
	.long	0
	.long	1071644672
	.long	2460177814
	.long	3219468930
	.long	2147483648
	.long	1014633195
	.long	3489768009
	.long	3218019271
	.long	0
	.long	1072693248
	.long	3152274921
	.long	3219449546
	.long	3758096384
	.long	3160654483
	.long	3239527685
	.long	3217952511
	.long	0
	.long	1072693248
	.long	2610628921
	.long	3219429956
	.long	1610612736
	.long	1015185121
	.long	1403757309
	.long	3217886718
	.long	0
	.long	1072693248
	.long	621354454
	.long	3219410163
	.long	536870912
	.long	3160934250
	.long	1883353036
	.long	3217821901
	.long	0
	.long	1072693248
	.long	1397053140
	.long	3219390169
	.long	1610612736
	.long	3162728726
	.long	3652612115
	.long	3217758070
	.long	0
	.long	1072693248
	.long	689983673
	.long	3219369978
	.long	3758096384
	.long	3162390053
	.long	752919289
	.long	3217695236
	.long	0
	.long	1072693248
	.long	2969983475
	.long	3219349592
	.long	2684354560
	.long	1015151795
	.long	3761365757
	.long	3217633406
	.long	0
	.long	1072693248
	.long	4242632757
	.long	3219329015
	.long	536870912
	.long	1013906630
	.long	1129922991
	.long	3217572592
	.long	0
	.long	1072693248
	.long	637203434
	.long	3219308251
	.long	3221225472
	.long	1011960193
	.long	2129093113
	.long	3217512801
	.long	0
	.long	1072693248
	.long	1289556703
	.long	3219287301
	.long	536870912
	.long	1015405172
	.long	2482246777
	.long	3217454043
	.long	0
	.long	1072693248
	.long	2570316633
	.long	3219266169
	.long	1610612736
	.long	1015261105
	.long	1539668340
	.long	3217396327
	.long	0
	.long	1072693248
	.long	967731400
	.long	3219244859
	.long	536870912
	.long	3163235805
	.long	2272832445
	.long	3217339661
	.long	0
	.long	1072693248
	.long	1675549513
	.long	3219223373
	.long	3221225472
	.long	1015541490
	.long	2678847490
	.long	3217284054
	.long	0
	.long	1072693248
	.long	1411074851
	.long	3219201715
	.long	1610612736
	.long	1016001083
	.long	69903290
	.long	3217229515
	.long	0
	.long	1072693248
	.long	1297975695
	.long	3219179888
	.long	3221225472
	.long	1015600087
	.long	3952756490
	.long	3217176050
	.long	0
	.long	1072693248
	.long	2569207790
	.long	3219157895
	.long	1073741824
	.long	1015699969
	.long	1663679671
	.long	3217123670
	.long	0
	.long	1072693248
	.long	2269920951
	.long	3219135740
	.long	3758096384
	.long	1014188597
	.long	1312926554
	.long	3217072381
	.long	0
	.long	1072693248
	.long	3690502842
	.long	3219098532
	.long	3221225472
	.long	1009970207
	.long	3429717432
	.long	3217013214
	.long	0
	.long	1072693248
	.long	1391425750
	.long	3219053593
	.long	3221225472
	.long	1014878455
	.long	1945768569
	.long	3216915048
	.long	0
	.long	1072693248
	.long	939980347
	.long	3219008349
	.long	0
	.long	3160280457
	.long	1539072807
	.long	3216819110
	.long	0
	.long	1072693248
	.long	1535566729
	.long	3218962807
	.long	2147483648
	.long	3161805036
	.long	4133449816
	.long	3216725414
	.long	0
	.long	1072693248
	.long	2570175582
	.long	3218916974
	.long	1073741824
	.long	3159786766
	.long	1612193054
	.long	3216633976
	.long	0
	.long	1072693248
	.long	3623952103
	.long	3218870857
	.long	2684354560
	.long	1014502314
	.long	1578746984
	.long	3216544808
	.long	0
	.long	1072693248
	.long	165764288
	.long	3218824464
	.long	3221225472
	.long	3160781666
	.long	1577996576
	.long	3216457924
	.long	0
	.long	1072693248
	.long	728580042
	.long	3218777800
	.long	1610612736
	.long	1014849296
	.long	1972483635
	.long	3216373337
	.long	0
	.long	1072693248
	.long	1135144330
	.long	3218730873
	.long	536870912
	.long	3160857217
	.long	1638976426
	.long	3216291060
	.long	0
	.long	1072693248
	.long	1673302999
	.long	3218683690
	.long	2147483648
	.long	3160835121
	.long	2255197647
	.long	3216211105
	.long	0
	.long	1072693248
	.long	2796464483
	.long	3218636258
	.long	3221225472
	.long	1013394669
	.long	3996842794
	.long	3216133484
	.long	0
	.long	1072693248
	.long	824036583
	.long	3218588585
	.long	0
	.long	3155796978
	.long	1234824733
	.long	3216058210
	.long	0
	.long	1072693248
	.long	821708191
	.long	3218540677
	.long	3758096384
	.long	1014254361
	.long	4002549419
	.long	3215985292
	.long	0
	.long	1072693248
	.long	3711903686
	.long	3218492541
	.long	1073741824
	.long	3159428395
	.long	2962723252
	.long	3215846895
	.long	0
	.long	1072693248
	.long	1974149085
	.long	3218444186
	.long	3221225472
	.long	1008664000
	.long	1296295961
	.long	3215710555
	.long	0
	.long	1072693248
	.long	1115219818
	.long	3218395618
	.long	0
	.long	3160555890
	.long	997322465
	.long	3215578993
	.long	0
	.long	1072693248
	.long	2484561409
	.long	3218346844
	.long	1073741824
	.long	3161508220
	.long	1261629236
	.long	3215452229
	.long	0
	.long	1072693248
	.long	3269558610
	.long	3218297872
	.long	536870912
	.long	1014221568
	.long	2476548698
	.long	3215330282
	.long	0
	.long	1072693248
	.long	785751814
	.long	3218248710
	.long	2684354560
	.long	1014354573
	.long	1913604284
	.long	3215213171
	.long	0
	.long	1072693248
	.long	1062001470
	.long	3218199364
	.long	0
	.long	1013391572
	.long	2306566604
	.long	3215100913
	.long	0
	.long	1072693248
	.long	1655830135
	.long	3218149842
	.long	0
	.long	3161990338
	.long	3250111450
	.long	3214993525
	.long	0
	.long	1072693248
	.long	238550446
	.long	3218100152
	.long	2684354560
	.long	1006899917
	.long	2377770267
	.long	3214848032
	.long	0
	.long	1072693248
	.long	2065784603
	.long	3218020857
	.long	3758096384
	.long	1013198318
	.long	288924873
	.long	3214652833
	.long	0
	.long	1072693248
	.long	3846521617
	.long	3217920847
	.long	536870912
	.long	3160890258
	.long	1935888103
	.long	3214467466
	.long	0
	.long	1072693248
	.long	1786662755
	.long	3217820546
	.long	2147483648
	.long	1008398659
	.long	2660899430
	.long	3214291960
	.long	0
	.long	1072693248
	.long	632292433
	.long	3217719968
	.long	3758096384
	.long	1013479685
	.long	18115067
	.long	3214126342
	.long	0
	.long	1072693248
	.long	1013556747
	.long	3217619128
	.long	2684354560
	.long	1013083417
	.long	2346447124
	.long	3213970635
	.long	0
	.long	1072693248
	.long	3729833777
	.long	3217518041
	.long	3758096384
	.long	3160528366
	.long	1672287667
	.long	3213764289
	.long	0
	.long	1072693248
	.long	1149976518
	.long	3217416724
	.long	1610612736
	.long	3160748543
	.long	3451754846
	.long	3213492662
	.long	0
	.long	1072693248
	.long	2972270170
	.long	3217315190
	.long	1073741824
	.long	1012041122
	.long	1037482584
	.long	3213240992
	.long	0
	.long	1072693248
	.long	1854824762
	.long	3217213456
	.long	2684354560
	.long	3159437312
	.long	2592747447
	.long	3213009315
	.long	0
	.long	1072693248
	.long	3470456183
	.long	3217111536
	.long	1073741824
	.long	1013261123
	.long	2402293340
	.long	3212758471
	.long	0
	.long	1072693248
	.long	1453945614
	.long	3216987726
	.long	1610612736
	.long	3158976260
	.long	1227670166
	.long	3212375297
	.long	0
	.long	1072693248
	.long	3349070549
	.long	3216783237
	.long	3758096384
	.long	1009054621
	.long	393047345
	.long	3212032302
	.long	0
	.long	1072693248
	.long	3156849708
	.long	3216578470
	.long	3221225472
	.long	1010706200
	.long	1118107366
	.long	3211670787
	.long	0
	.long	1072693248
	.long	177906713
	.long	3216373456
	.long	3221225472
	.long	1011826722
	.long	4269262315
	.long	3211145810
	.long	0
	.long	1072693248
	.long	2462980598
	.long	3216168224
	.long	2684354560
	.long	1011458184
	.long	3810305407
	.long	3210663220
	.long	0
	.long	1072693248
	.long	2046304480
	.long	3215943022
	.long	3758096384
	.long	1008919019
	.long	220529721
	.long	3209935929
	.long	0
	.long	1072693248
	.long	4044216340
	.long	3215531877
	.long	0
	.long	1009324733
	.long	69373323
	.long	3209049152
	.long	0
	.long	1072693248
	.long	1972655565
	.long	3215120485
	.long	1073741824
	.long	3156641526
	.long	3650591271
	.long	3207838971
	.long	0
	.long	1072693248
	.long	4154680958
	.long	3214483797
	.long	0
	.long	1006312803
	.long	2376373521
	.long	3205741868
	.long	0
	.long	1072693248
	.long	4242458500
	.long	3213435345
	.long	2684354560
	.long	3156838286
	.long	0
	.long	0
	.long	0
	.long	1072693248
	.long	0
	.long	0
	.long	0
	.long	0
	.long	2376373521
	.long	3205741868
	.long	0
	.long	1072693248
	.long	4242458500
	.long	1065951697
	.long	2684354560
	.long	1009354638
	.long	3650591271
	.long	3207838971
	.long	0
	.long	1072693248
	.long	4154680958
	.long	1067000149
	.long	0
	.long	3153796451
	.long	69373323
	.long	3209049152
	.long	0
	.long	1072693248
	.long	1972655565
	.long	1067636837
	.long	1073741824
	.long	1009157878
	.long	220529721
	.long	3209935929
	.long	0
	.long	1072693248
	.long	4044216340
	.long	1068048229
	.long	0
	.long	3156808381
	.long	3810305407
	.long	3210663220
	.long	0
	.long	1072693248
	.long	2046304480
	.long	1068459374
	.long	3758096384
	.long	3156402667
	.long	4269262315
	.long	3211145810
	.long	0
	.long	1072693248
	.long	2462980598
	.long	1068684576
	.long	2684354560
	.long	3158941832
	.long	1118107366
	.long	3211670787
	.long	0
	.long	1072693248
	.long	177906713
	.long	1068889808
	.long	3221225472
	.long	3159310370
	.long	393047345
	.long	3212032302
	.long	0
	.long	1072693248
	.long	3156849708
	.long	1069094822
	.long	3221225472
	.long	3158189848
	.long	1227670166
	.long	3212375297
	.long	0
	.long	1072693248
	.long	3349070549
	.long	1069299589
	.long	3758096384
	.long	3156538269
	.long	2402293340
	.long	3212758471
	.long	0
	.long	1072693248
	.long	1453945614
	.long	1069504078
	.long	1610612736
	.long	1011492612
	.long	2592747447
	.long	3213009315
	.long	0
	.long	1072693248
	.long	3470456183
	.long	1069627888
	.long	1073741824
	.long	3160744771
	.long	1037482584
	.long	3213240992
	.long	0
	.long	1072693248
	.long	1854824762
	.long	1069729808
	.long	2684354560
	.long	1011953664
	.long	3451754846
	.long	3213492662
	.long	0
	.long	1072693248
	.long	2972270170
	.long	1069831542
	.long	1073741824
	.long	3159524770
	.long	1672287667
	.long	3213764289
	.long	0
	.long	1072693248
	.long	1149976518
	.long	1069933076
	.long	1610612736
	.long	1013264895
	.long	2346447124
	.long	3213970635
	.long	0
	.long	1072693248
	.long	3729833777
	.long	1070034393
	.long	3758096384
	.long	1013044718
	.long	18115067
	.long	3214126342
	.long	0
	.long	1072693248
	.long	1013556747
	.long	1070135480
	.long	2684354560
	.long	3160567065
	.long	2660899430
	.long	3214291960
	.long	0
	.long	1072693248
	.long	632292433
	.long	1070236320
	.long	3758096384
	.long	3160963333
	.long	1935888103
	.long	3214467466
	.long	0
	.long	1072693248
	.long	1786662755
	.long	1070336898
	.long	2147483648
	.long	3155882307
	.long	288924873
	.long	3214652833
	.long	0
	.long	1072693248
	.long	3846521617
	.long	1070437199
	.long	536870912
	.long	1013406610
	.long	2377770267
	.long	3214848032
	.long	0
	.long	1072693248
	.long	2065784603
	.long	1070537209
	.long	3758096384
	.long	3160681966
	.long	3250111450
	.long	3214993525
	.long	0
	.long	1072693248
	.long	238550446
	.long	1070616504
	.long	2684354560
	.long	3154383565
	.long	2306566604
	.long	3215100913
	.long	0
	.long	1072693248
	.long	1655830135
	.long	1070666194
	.long	0
	.long	1014506690
	.long	1913604284
	.long	3215213171
	.long	0
	.long	1072693248
	.long	1062001470
	.long	1070715716
	.long	0
	.long	3160875220
	.long	2476548698
	.long	3215330282
	.long	0
	.long	1072693248
	.long	785751814
	.long	1070765062
	.long	2684354560
	.long	3161838221
	.long	1261629236
	.long	3215452229
	.long	0
	.long	1072693248
	.long	3269558610
	.long	1070814224
	.long	536870912
	.long	3161705216
	.long	997322465
	.long	3215578993
	.long	0
	.long	1072693248
	.long	2484561409
	.long	1070863196
	.long	1073741824
	.long	1014024572
	.long	1296295961
	.long	3215710555
	.long	0
	.long	1072693248
	.long	1115219818
	.long	1070911970
	.long	0
	.long	1013072242
	.long	2962723252
	.long	3215846895
	.long	0
	.long	1072693248
	.long	1974149085
	.long	1070960538
	.long	3221225472
	.long	3156147648
	.long	4002549419
	.long	3215985292
	.long	0
	.long	1072693248
	.long	3711903686
	.long	1071008893
	.long	1073741824
	.long	1011944747
	.long	1234824733
	.long	3216058210
	.long	0
	.long	1072693248
	.long	821708191
	.long	1071057029
	.long	3758096384
	.long	3161738009
	.long	3996842794
	.long	3216133484
	.long	0
	.long	1072693248
	.long	824036583
	.long	1071104937
	.long	0
	.long	1008313330
	.long	2255197647
	.long	3216211105
	.long	0
	.long	1072693248
	.long	2796464483
	.long	1071152610
	.long	3221225472
	.long	3160878317
	.long	1638976426
	.long	3216291060
	.long	0
	.long	1072693248
	.long	1673302999
	.long	1071200042
	.long	2147483648
	.long	1013351473
	.long	1972483635
	.long	3216373337
	.long	0
	.long	1072693248
	.long	1135144330
	.long	1071247225
	.long	536870912
	.long	1013373569
	.long	1577996576
	.long	3216457924
	.long	0
	.long	1072693248
	.long	728580042
	.long	1071294152
	.long	1610612736
	.long	3162332944
	.long	1578746984
	.long	3216544808
	.long	0
	.long	1072693248
	.long	165764288
	.long	1071340816
	.long	3221225472
	.long	1013298018
	.long	1612193054
	.long	3216633976
	.long	0
	.long	1072693248
	.long	3623952103
	.long	1071387209
	.long	2684354560
	.long	3161985962
	.long	4133449816
	.long	3216725414
	.long	0
	.long	1072693248
	.long	2570175582
	.long	1071433326
	.long	1073741824
	.long	1012303118
	.long	1539072807
	.long	3216819110
	.long	0
	.long	1072693248
	.long	1535566729
	.long	1071479159
	.long	2147483648
	.long	1014321388
	.long	1945768569
	.long	3216915048
	.long	0
	.long	1072693248
	.long	939980347
	.long	1071524701
	.long	0
	.long	1012796809
	.long	3429717432
	.long	3217013214
	.long	0
	.long	1072693248
	.long	1391425750
	.long	1071569945
	.long	3221225472
	.long	3162362103
	.long	1312926554
	.long	3217072381
	.long	0
	.long	1072693248
	.long	3690502842
	.long	1071614884
	.long	3221225472
	.long	3157453855
	.long	1663679671
	.long	3217123670
	.long	0
	.long	1072693248
	.long	2269920951
	.long	1071652092
	.long	3758096384
	.long	3161672245
	.long	3952756490
	.long	3217176050
	.long	0
	.long	1072693248
	.long	2569207790
	.long	1071674247
	.long	1073741824
	.long	3163183617
	.long	69903290
	.long	3217229515
	.long	0
	.long	1072693248
	.long	1297975695
	.long	1071696240
	.long	3221225472
	.long	3163083735
	.long	2678847490
	.long	3217284054
	.long	0
	.long	1072693248
	.long	1411074851
	.long	1071718067
	.long	1610612736
	.long	3163484731
	.long	2272832445
	.long	3217339661
	.long	0
	.long	1072693248
	.long	1675549513
	.long	1071739725
	.long	3221225472
	.long	3163025138
	.long	1539668340
	.long	3217396327
	.long	0
	.long	1072693248
	.long	967731400
	.long	1071761211
	.long	536870912
	.long	1015752157
	.long	2482246777
	.long	3217454043
	.long	0
	.long	1072693248
	.long	2570316633
	.long	1071782521
	.long	1610612736
	.long	3162744753
	.long	2129093113
	.long	3217512801
	.long	0
	.long	1072693248
	.long	1289556703
	.long	1071803653
	.long	536870912
	.long	3162888820
	.long	1129922991
	.long	3217572592
	.long	0
	.long	1072693248
	.long	637203434
	.long	1071824603
	.long	3221225472
	.long	3159443841
	.long	3761365757
	.long	3217633406
	.long	0
	.long	1072693248
	.long	4242632757
	.long	1071845367
	.long	536870912
	.long	3161390278
	.long	752919289
	.long	3217695236
	.long	0
	.long	1072693248
	.long	2969983475
	.long	1071865944
	.long	2684354560
	.long	3162635443
	.long	3652612115
	.long	3217758070
	.long	0
	.long	1072693248
	.long	689983673
	.long	1071886330
	.long	3758096384
	.long	1014906405
	.long	1883353036
	.long	3217821901
	.long	0
	.long	1072693248
	.long	1397053140
	.long	1071906521
	.long	1610612736
	.long	1015245078
	.long	1403757309
	.long	3217886718
	.long	0
	.long	1072693248
	.long	621354454
	.long	1071926515
	.long	536870912
	.long	1013450602
	.long	3239527685
	.long	3217952511
	.long	0
	.long	1072693248
	.long	2610628921
	.long	1071946308
	.long	1610612736
	.long	3162668769
	.long	3489768009
	.long	3218019271
	.long	0
	.long	1072693248
	.long	3152274921
	.long	1071965898
	.long	3758096384
	.long	1013170835
	.long	2371640770
	.long	1070588851
	.long	0
	.long	1071644672
	.long	2460177814
	.long	1071985282
	.long	2147483648
	.long	3162116843
	.long	2309668539
	.long	1070520188
	.long	0
	.long	1071644672
	.long	881650848
	.long	1072004457
	.long	2147483648
	.long	3162986272
	.long	2081278341
	.long	1070450589
	.long	0
	.long	1071644672
	.long	3194290572
	.long	1072023419
	.long	2147483648
	.long	1015048682
	.long	3753901657
	.long	1070380064
	.long	0
	.long	1071644672
	.long	1427975391
	.long	1072042167
	.long	1073741824
	.long	3161608627
	.long	1403691706
	.long	1070308625
	.long	0
	.long	1071644672
	.long	636516413
	.long	1072060697
	.long	3758096384
	.long	1015823716
	.long	2583490354
	.long	1070236281
	.long	0
	.long	1071644672
	.long	1719614413
	.long	1072079006
	.long	0
	.long	3163282740
	.long	2546065654
	.long	1070163044
	.long	0
	.long	1071644672
	.long	1424664751
	.long	1072097092
	.long	3221225472
	.long	3163405315
	.long	1416934267
	.long	1070088925
	.long	0
	.long	1071644672
	.long	938475414
	.long	1072114952
	.long	0
	.long	1015076079
	.long	4187227697
	.long	1070013934
	.long	0
	.long	1071644672
	.long	1594061409
	.long	1072132583
	.long	2147483648
	.long	1014353870
	.long	3526603391
	.long	1069938084
	.long	0
	.long	1071644672
	.long	577417135
	.long	1072149983
	.long	2684354560
	.long	3161519415
	.long	1250776663
	.long	1069861386
	.long	0
	.long	1071644672
	.long	3814135665
	.long	1072167148
	.long	3221225472
	.long	3163146456
	.long	4019165092
	.long	1069783850
	.long	0
	.long	1071644672
	.long	201299822
	.long	1072184078
	.long	3758096384
	.long	3161912352
	.long	1852581091
	.long	1069705490
	.long	0
	.long	1071644672
	.long	263859903
	.long	1072200768
	.long	0
	.long	3162692284
	.long	2485417816
	.long	1069626316
	.long	0
	.long	1071644672
	.long	1796544321
	.long	1072217216
	.long	536870912
	.long	3162686945
	.long	2586490530
	.long	1069545162
	.long	0
	.long	1071644672
	.long	2750387213
	.long	1072233420
	.long	2147483648
	.long	3160858537
	.long	1224902090
	.long	1069383633
	.long	0
	.long	1071644672
	.long	1234330619
	.long	1072249378
	.long	2684354560
	.long	1015272473
	.long	2285031318
	.long	1069220549
	.long	0
	.long	1071644672
	.long	4106737474
	.long	1072265086
	.long	2684354560
	.long	3162592377
	.long	3876290983
	.long	1069055935
	.long	0
	.long	1071644672
	.long	1207142209
	.long	1072280544
	.long	2147483648
	.long	3162705634
	.long	802660176
	.long	1068889817
	.long	0
	.long	1071644672
	.long	4012486548
	.long	1072295747
	.long	2684354560
	.long	1014701564
	.long	1726519029
	.long	1068722218
	.long	0
	.long	1071644672
	.long	2688952194
	.long	1072310695
	.long	0
	.long	3161532603
	.long	3382663878
	.long	1068553164
	.long	0
	.long	1071644672
	.long	453180130
	.long	1072325385
	.long	1073741824
	.long	3162411281
	.long	2598800519
	.long	1068266419
	.long	0
	.long	1071644672
	.long	688824739
	.long	1072339814
	.long	3758096384
	.long	1010431536
	.long	1038988426
	.long	1067922646
	.long	0
	.long	1071644672
	.long	2653017361
	.long	1072353980
	.long	0
	.long	3162800062
	.long	1102361128
	.long	1067576117
	.long	0
	.long	1071644672
	.long	1477771776
	.long	1072367882
	.long	536870912
	.long	3163104986
	.long	2880516564
	.long	1067003401
	.long	0
	.long	1071644672
	.long	1056266002
	.long	1072381517
	.long	2684354560
	.long	3163469496
	.long	1220541286
	.long	1066197478
	.long	0
	.long	1071644672
	.long	1159294526
	.long	1072394883
	.long	536870912
	.long	3163276959
	.long	1279178457
	.long	1064107143
	.long	0
	.long	1071644672
	.long	1731563730
	.long	1072407978
	.long	536870912
	.long	1015540302
	.long	31019393
	.long	3212646089
	.long	0
	.long	1071644672
	.long	2892993834
	.long	1072420800
	.long	536870912
	.long	3163039143
	.long	3506991783
	.long	3214032490
	.long	0
	.long	1071644672
	.long	645027145
	.long	1072433348
	.long	3221225472
	.long	1012602239
	.long	2140183630
	.long	3214756396
	.long	0
	.long	1071644672
	.long	4051746225
	.long	1072445618
	.long	1610612736
	.long	3161907377
	.long	600368053
	.long	3215209541
	.long	0
	.long	1071644672
	.long	881357723
	.long	1072457611
	.long	3221225472
	.long	1013810890
	.long	913431823
	.long	3215576203
	.long	0
	.long	1071644672
	.long	557060597
	.long	1072469323
	.long	0
	.long	3159554934
	.long	1073154251
	.long	3215945137
	.long	0
	.long	1071644672
	.long	4093443164
	.long	1072480752
	.long	536870912
	.long	1014912865
	.long	3631910143
	.long	3216149439
	.long	0
	.long	1071644672
	.long	4097623923
	.long	1072491898
	.long	2684354560
	.long	3160308587
	.long	1380647130
	.long	3216336095
	.long	0
	.long	1071644672
	.long	1950234076
	.long	1072502759
	.long	536870912
	.long	3159425498
	.long	4049357271
	.long	3216523802
	.long	0
	.long	1071644672
	.long	3511535930
	.long	1072513332
	.long	2147483648
	.long	3162828626
	.long	1896722595
	.long	3216712534
	.long	0
	.long	1071644672
	.long	1942611595
	.long	1072523617
	.long	3221225472
	.long	3162410081
	.long	1699043957
	.long	3216902261
	.long	0
	.long	1071644672
	.long	3476196678
	.long	1072533611
	.long	0
	.long	1014257638
	.long	3296146332
	.long	1069485732
	.long	0
	.long	1070596096
	.long	1647878299
	.long	1072543314
	.long	3758096384
	.long	3162169063
	.long	3286832763
	.long	1069294100
	.long	0
	.long	1070596096
	.long	3066872380
	.long	1072552723
	.long	3221225472
	.long	1015561271
	.long	1962712856
	.long	1069101559
	.long	0
	.long	1070596096
	.long	1647164971
	.long	1072561838
	.long	2147483648
	.long	3162838250
	.long	3601629044
	.long	1068908137
	.long	0
	.long	1070596096
	.long	83265253
	.long	1072570657
	.long	1610612736
	.long	3162510535
	.long	165911977
	.long	1068713865
	.long	0
	.long	1070596096
	.long	1261158782
	.long	1072579178
	.long	2147483648
	.long	1015711544
	.long	1348311045
	.long	1068518770
	.long	0
	.long	1070596096
	.long	3964199186
	.long	1072587400
	.long	3758096384
	.long	3160501078
	.long	386665102
	.long	1068146822
	.long	0
	.long	1070596096
	.long	2873938189
	.long	1072595323
	.long	1610612736
	.long	3162311422
	.long	1991047213
	.long	1067753521
	.long	0
	.long	1070596096
	.long	1455828442
	.long	1072602945
	.long	3758096384
	.long	1015505073
	.long	4107121629
	.long	1067267139
	.long	0
	.long	1070596096
	.long	3370060660
	.long	1072610264
	.long	1610612736
	.long	1014787212
	.long	723478390
	.long	1066474790
	.long	0
	.long	1070596096
	.long	3882371427
	.long	1072617280
	.long	2147483648
	.long	1014923975
	.long	2382326656
	.long	1064562179
	.long	0
	.long	1070596096
	.long	2749658050
	.long	1072623992
	.long	1610612736
	.long	1015427338
	.long	2614369450
	.long	3212624080
	.long	0
	.long	1070596096
	.long	4220661975
	.long	1072630398
	.long	3221225472
	.long	1014312058
	.long	3587565429
	.long	3214108032
	.long	0
	.long	1070596096
	.long	4151720666
	.long	1072636498
	.long	0
	.long	3162639544
	.long	2886567144
	.long	3214910444
	.long	0
	.long	1070596096
	.long	2892293513
	.long	1072642291
	.long	2684354560
	.long	3162340271
	.long	1765797566
	.long	3215324543
	.long	0
	.long	1070596096
	.long	990588717
	.long	1072647776
	.long	3758096384
	.long	3162041857
	.long	240740309
	.long	3215727903
	.long	0
	.long	1070596096
	.long	3489094832
	.long	1072652951
	.long	536870912
	.long	1014325783
	.long	2034433221
	.long	1068349287
	.long	0
	.long	1069547520
	.long	2745245815
	.long	1072657817
	.long	3221225472
	.long	1015566129
	.long	304938777
	.long	1067944017
	.long	0
	.long	1069547520
	.long	4201728937
	.long	1072662372
	.long	1610612736
	.long	1014674006
	.long	3299146087
	.long	1067537882
	.long	0
	.long	1069547520
	.long	617154971
	.long	1072666617
	.long	3758096384
	.long	3163433696
	.long	1953696209
	.long	1066811523
	.long	0
	.long	1069547520
	.long	2131272983
	.long	1072670549
	.long	3221225472
	.long	3163005096
	.long	3987691371
	.long	1065590540
	.long	0
	.long	1069547520
	.long	1905645377
	.long	1072674169
	.long	1610612736
	.long	1014087596
	.long	718380601
	.long	3211081269
	.long	0
	.long	1069547520
	.long	1893834707
	.long	1072677476
	.long	1610612736
	.long	1015147654
	.long	3272206680
	.long	3213771729
	.long	0
	.long	1069547520
	.long	4251821691
	.long	1072680469
	.long	2147483648
	.long	3161841007
	.long	257503056
	.long	3214647653
	.long	0
	.long	1069547520
	.long	2748392742
	.long	1072683149
	.long	536870912
	.long	3163061750
	.long	711626851
	.long	1066916672
	.long	0
	.long	1068498944
	.long	4240268041
	.long	1072685514
	.long	536870912
	.long	1013588963
	.long	2523975598
	.long	1065789700
	.long	0
	.long	1068498944
	.long	2902558778
	.long	1072687565
	.long	3221225472
	.long	1013260780
	.long	3237733378
	.long	3209908784
	.long	0
	.long	1068498944
	.long	1703834029
	.long	1072689301
	.long	3758096384
	.long	1015028364
	.long	1003003824
	.long	3213591144
	.long	0
	.long	1068498944
	.long	3816385918
	.long	1072690721
	.long	536870912
	.long	3161216956
	.long	2896342633
	.long	1064747819
	.long	0
	.long	1067450368
	.long	4026464093
	.long	1072691826
	.long	3221225472
	.long	3163075789
	.long	561145352
	.long	3212540584
	.long	0
	.long	1067450368
	.long	1619315981
	.long	1072692616
	.long	1073741824
	.long	3162584397
	.long	210035182
	.long	3211491512
	.long	0
	.long	1066401792
	.long	379294427
	.long	1072693090
	.long	3221225472
	.long	1015405987
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
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
	.long	239972940
	.long	1089470464
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
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
	.long	1841940611
	.long	1079271216
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
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	511
	.long	0
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	1413480448
	.long	1065951739
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	442499072
	.long	1029747809
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	771977331
	.long	993204618
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	1431651400
	.long	3217380693
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	2880656668
	.long	1065423119
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	0
	.long	3219128320
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	1431653583
	.long	1067799893
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.long	2976101079
	.long	3210133866
	.type	__svml_dcos_ha_data_internal_ha,@object
	.size	__svml_dcos_ha_data_internal_ha,17280
	.align 64
	.hidden __svml_dcos_ha_data_internal
	.globl __svml_dcos_ha_data_internal
__svml_dcos_ha_data_internal:
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
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	0
	.long	1097859072
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
	.long	1841940611
	.long	1075076912
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
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	1413754136
	.long	1070146043
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	855638016
	.long	1013032486
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
	.long	3773204808
	.long	979655686
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
	.long	0
	.long	0
	.long	1013556747
	.long	1070135480
	.long	2796464483
	.long	1071152610
	.long	967731400
	.long	1071761211
	.long	1719614413
	.long	1072079006
	.long	688824739
	.long	1072339814
	.long	3476196678
	.long	1072533611
	.long	3489094832
	.long	1072652951
	.long	0
	.long	1072693248
	.long	3489094832
	.long	1072652951
	.long	3476196678
	.long	1072533611
	.long	688824739
	.long	1072339814
	.long	1719614413
	.long	1072079006
	.long	967731400
	.long	1071761211
	.long	2796464483
	.long	1071152610
	.long	1013556747
	.long	1070135480
	.long	0
	.long	1072693248
	.long	3489094832
	.long	1072652951
	.long	3476196678
	.long	1072533611
	.long	688824739
	.long	1072339814
	.long	1719614413
	.long	1072079006
	.long	967731400
	.long	1071761211
	.long	2796464483
	.long	1071152610
	.long	1013556747
	.long	1070135480
	.long	0
	.long	0
	.long	1013556747
	.long	3217619128
	.long	2796464483
	.long	3218636258
	.long	967731400
	.long	3219244859
	.long	1719614413
	.long	3219562654
	.long	688824739
	.long	3219823462
	.long	3476196678
	.long	3220017259
	.long	3489094832
	.long	3220136599
	.long	0
	.long	0
	.long	3160567065
	.long	1014325783
	.long	3160878317
	.long	1014257638
	.long	1015752157
	.long	1010431536
	.long	3163282740
	.long	3163282740
	.long	1010431536
	.long	1015752157
	.long	1014257638
	.long	3160878317
	.long	1014325783
	.long	3160567065
	.long	0
	.long	0
	.long	1014325783
	.long	1013083417
	.long	1014257638
	.long	1013394669
	.long	1010431536
	.long	3163235805
	.long	3163282740
	.long	1015799092
	.long	1015752157
	.long	3157915184
	.long	3160878317
	.long	3161741286
	.long	3160567065
	.long	3161809431
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	3654764988
	.long	1056571351
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	123565048
	.long	3210133868
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	3525997533
	.long	1053236375
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	248520414
	.long	3207201184
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	1431627990
	.long	1067799893
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	286321053
	.long	1065423121
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	4294967292
	.long	3219128319
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.long	1431655765
	.long	3217380693
	.type	__svml_dcos_ha_data_internal,@object
	.size	__svml_dcos_ha_data_internal,1408
	.align 32
.L_2il0floatpacket.70:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,32
	.align 32
.L_2il0floatpacket.71:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,32
	.align 32
.L_2il0floatpacket.72:
	.long	0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,32
	.align 32
.L_2il0floatpacket.73:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,32
	.align 32
.L_2il0floatpacket.74:
	.long	0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,32
	.align 32
.L_2il0floatpacket.75:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.75,@object
	.size	.L_2il0floatpacket.75,32
	.align 32
.L_2il0floatpacket.76:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.76,@object
	.size	.L_2il0floatpacket.76,32
	.align 32
.L_2il0floatpacket.77:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.77,@object
	.size	.L_2il0floatpacket.77,32
	.align 32
.L_2il0floatpacket.78:
	.long	0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.78,@object
	.size	.L_2il0floatpacket.78,32
	.align 32
.L_2il0floatpacket.79:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.79,@object
	.size	.L_2il0floatpacket.79,32
	.align 32
.L_2il0floatpacket.80:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.80,@object
	.size	.L_2il0floatpacket.80,32
	.align 32
.L_2il0floatpacket.81:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.81,@object
	.size	.L_2il0floatpacket.81,32
	.align 32
.L_2il0floatpacket.82:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.82,@object
	.size	.L_2il0floatpacket.82,32
	.align 32
.L_2il0floatpacket.83:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.83,@object
	.size	.L_2il0floatpacket.83,32
	.align 32
.L_2il0floatpacket.84:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.84,@object
	.size	.L_2il0floatpacket.84,32
	.align 32
.L_2il0floatpacket.85:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.85,@object
	.size	.L_2il0floatpacket.85,32
	.align 32
.L_2il0floatpacket.86:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.86,@object
	.size	.L_2il0floatpacket.86,32
	.align 32
.L_2il0floatpacket.87:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.87,@object
	.size	.L_2il0floatpacket.87,32
	.align 32
.L_2il0floatpacket.88:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.88,@object
	.size	.L_2il0floatpacket.88,32
	.align 32
.L_2il0floatpacket.89:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.89,@object
	.size	.L_2il0floatpacket.89,32
	.align 32
.L_2il0floatpacket.90:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.90,@object
	.size	.L_2il0floatpacket.90,32
	.align 16
.L_2il0floatpacket.34:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,16
	.align 16
.L_2il0floatpacket.35:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,16
	.align 16
.L_2il0floatpacket.36:
	.long	0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,16
	.align 16
.L_2il0floatpacket.37:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,16
	.align 16
.L_2il0floatpacket.38:
	.long	0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,16
	.align 16
.L_2il0floatpacket.39:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,16
	.align 16
.L_2il0floatpacket.40:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,16
	.align 16
.L_2il0floatpacket.41:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,16
	.align 16
.L_2il0floatpacket.42:
	.long	0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,16
	.align 16
.L_2il0floatpacket.43:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,16
	.align 16
.L_2il0floatpacket.44:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,16
	.align 16
.L_2il0floatpacket.45:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,16
	.align 16
.L_2il0floatpacket.46:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,16
	.align 16
.L_2il0floatpacket.47:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,16
	.align 16
.L_2il0floatpacket.48:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,16
	.align 16
.L_2il0floatpacket.49:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,16
	.align 16
.L_2il0floatpacket.50:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,16
	.align 16
.L_2il0floatpacket.51:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,16
	.align 16
.L_2il0floatpacket.69:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,16
	.align 8
.L_2il0floatpacket.52:
	.long	0xffffffff,0xffffffff
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,8
	.align 8
.L_2il0floatpacket.53:
	.long	0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,8
	.align 8
.L_2il0floatpacket.54:
	.long	0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,8
	.align 8
.L_2il0floatpacket.55:
	.long	0x00000000,0x00100000
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,8
	.align 8
.L_2il0floatpacket.56:
	.long	0xffffffff,0x00000000
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,8
	.align 8
.L_2il0floatpacket.57:
	.long	0x00000000,0x80000000
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,8
	.align 8
.L_2il0floatpacket.58:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,8
	.align 8
.L_2il0floatpacket.59:
	.long	0x00000000,0x42a80000
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,8
	.align 8
.L_2il0floatpacket.60:
	.long	0x000001ff,0x00000000
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,8
	.align 8
.L_2il0floatpacket.61:
	.long	0x00000000,0x39700000
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,8
	.align 8
.L_2il0floatpacket.62:
	.long	0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,8
	.align 8
.L_2il0floatpacket.63:
	.long	0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,8
	.align 8
.L_2il0floatpacket.64:
	.long	0x00000fff,0x00000000
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,8
	.align 8
.L_2il0floatpacket.65:
	.long	0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,8
	.align 8
.L_2il0floatpacket.66:
	.long	0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,8
	.align 8
.L_2il0floatpacket.67:
	.long	0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,8
	.align 8
.L_2il0floatpacket.68:
	.long	0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,8
	.align 8
_vmldCosHATab:
	.long	0
	.long	0
	.long	0
	.long	2146435072
	.type	_vmldCosHATab,@object
	.size	_vmldCosHATab,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
