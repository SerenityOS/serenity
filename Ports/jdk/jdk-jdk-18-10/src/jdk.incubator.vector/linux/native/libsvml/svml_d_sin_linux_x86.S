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
	.file "svml_d_sin.c"
	.text
..TXTST0:
.L_2__routine_start___svml_sin2_ha_e9_0:
# -- Begin  __svml_sin2_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin2_ha_e9
# --- __svml_sin2_ha_e9(__m128d)
__svml_sin2_ha_e9:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2782.1
        .byte     15                                            #3141.18
        .byte     30                                            #3141.18
        .byte     250                                           #3141.18
	.cfi_startproc
..___tag_value___svml_sin2_ha_e9.1:
..L2:
                                                          #2782.1
        pushq     %rbp                                          #2782.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2782.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2782.1
        subq      $192, %rsp                                    #2782.1
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rax   #2849.383
        movups    16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm1 #2829.50
        xorl      %edx, %edx                                    #2828.1
        movups    16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #2835.25
        movaps    %xmm1, %xmm2                                  #2830.17
        andps     %xmm0, %xmm1                                  #2831.12
        andnps    %xmm0, %xmm2                                  #2830.17
        mulpd     %xmm1, %xmm12                                 #2835.25
        movups    16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm6 #2834.51
        movaps    %xmm1, %xmm7                                  #2838.14
        addpd     %xmm6, %xmm12                                 #2835.13
        movaps    %xmm12, %xmm10                                #2836.13
        subpd     %xmm6, %xmm10                                 #2836.13
        movups    16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #2837.48
        mulpd     %xmm10, %xmm8                                 #2838.36
        movups    16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #2840.19
        mulpd     %xmm10, %xmm9                                 #2840.19
        subpd     %xmm8, %xmm7                                  #2838.14
        movaps    %xmm7, %xmm5                                  #2841.17
        andps     16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #2848.23
        subpd     %xmm9, %xmm5                                  #2841.17
        movd      %xmm12, %ecx                                  #2849.128
        movaps    %xmm5, %xmm3                                  #2853.22
        movaps    %xmm5, %xmm15                                 #2852.19
        shll      $5, %ecx                                      #2849.298
        pextrd    $2, %xmm12, %esi                              #2849.190
        shll      $5, %esi                                      #2849.349
        movq      8(%rcx,%rax), %xmm8                           #2850.614
        movhpd    8(%rsi,%rax), %xmm8                           #2850.582
        mulpd     %xmm8, %xmm3                                  #2853.22
        subpd     %xmm5, %xmm7                                  #2842.17
        movups    16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #2844.50
        movq      (%rcx,%rax), %xmm4                            #2849.612
        mulpd     %xmm10, %xmm11                                #2845.32
        subpd     %xmm9, %xmm7                                  #2843.17
        movq      16(%rcx,%rax), %xmm10                         #2851.612
        movaps    %xmm5, %xmm9                                  #2861.15
        movhpd    (%rsi,%rax), %xmm4                            #2849.580
        movhpd    16(%rsi,%rax), %xmm10                         #2851.580
        mulpd     %xmm4, %xmm15                                 #2852.19
        mulpd     %xmm5, %xmm9                                  #2861.15
        addpd     %xmm8, %xmm4                                  #2867.13
        subpd     %xmm11, %xmm7                                 #2845.13
        movaps    %xmm10, %xmm14                                #2854.22
        movaps    %xmm10, %xmm13                                #2856.18
        addpd     %xmm3, %xmm14                                 #2854.22
        movaps    %xmm14, %xmm6                                 #2855.21
        addpd     %xmm15, %xmm6                                 #2855.21
        subpd     %xmm14, %xmm13                                #2856.18
        subpd     %xmm6, %xmm14                                 #2858.18
        addpd     %xmm13, %xmm3                                 #2857.18
        addpd     %xmm14, %xmm15                                #2859.18
        movups    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm14 #2871.26
        mulpd     %xmm9, %xmm14                                 #2871.26
        addpd     %xmm15, %xmm3                                 #2860.21
        addpd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm14 #2871.14
        movups    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #2864.26
        mulpd     %xmm9, %xmm13                                 #2864.26
        mulpd     %xmm9, %xmm14                                 #2873.26
        addpd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #2864.14
        addpd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm14 #2873.14
        mulpd     %xmm9, %xmm13                                 #2865.14
        mulpd     %xmm14, %xmm9                                 #2874.14
        mulpd     %xmm5, %xmm13                                 #2866.14
        mulpd     %xmm10, %xmm9                                 #2875.29
        mulpd     %xmm5, %xmm10                                 #2876.32
        mulpd     %xmm4, %xmm13                                 #2868.29
        subpd     %xmm10, %xmm4                                 #2876.13
        addpd     %xmm13, %xmm3                                 #2868.17
        mulpd     %xmm4, %xmm7                                  #2878.28
        addpd     %xmm9, %xmm3                                  #2875.17
        movq      24(%rcx,%rax), %xmm5                          #2877.612
        movhpd    24(%rsi,%rax), %xmm5                          #2877.580
        addpd     %xmm7, %xmm5                                  #2878.16
        addpd     %xmm5, %xmm3                                  #2879.17
        addpd     %xmm3, %xmm6                                  #2880.14
        movaps    %xmm1, %xmm3                                  #2882.26
        pxor      %xmm2, %xmm6                                  #2881.14
        cmpnlepd  16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm3 #2882.26
        movmskpd  %xmm3, %edi                                   #2884.78
        testl     %edi, %edi                                    #2885.66
        jne       ..B1.12       # Prob 5%                       #2885.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm3 xmm6
..B1.2:                         # Preds ..B1.12 ..B1.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #3144.52
        jne       ..B1.4        # Prob 5%                       #3144.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm6
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        movaps    %xmm6, %xmm0                                  #3147.12
        movq      %rbp, %rsp                                    #3147.12
        popq      %rbp                                          #3147.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3147.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #3144.197
        movups    %xmm6, 128(%rsp)                              #3144.270
        je        ..B1.3        # Prob 95%                      #3144.374
                                # LOE rbx r12 r13 r14 r15 edx xmm6
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #3144.454
        movq      %r12, 8(%rsp)                                 #3144.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #3144.454
        movq      %r13, (%rsp)                                  #3144.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #3144.454
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #3144.517
        jc        ..B1.11       # Prob 5%                       #3144.517
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #3144.470
        cmpl      $2, %r12d                                     #3144.465
        jl        ..B1.8        # Prob 82%                      #3144.465
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm6                              #3144.673
        jmp       ..B1.3        # Prob 100%                     #3144.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm6
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #3144.546
        lea       128(%rsp,%r12,8), %rsi                        #3144.546
..___tag_value___svml_sin2_ha_e9.19:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #3144.546
..___tag_value___svml_sin2_ha_e9.20:
        jmp       ..B1.9        # Prob 100%                     #3144.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.35(%rip), %xmm10           #2978.22
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %r9 #2980.385
        pand      %xmm1, %xmm10                                 #2978.22
        psrlq     $52, %xmm10                                   #2979.22
        movups    16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #2888.55
        movd      %xmm10, %ecx                                  #2980.124
        movups    %xmm0, 32(%rsp)                               #[spill]
        movaps    %xmm8, %xmm0                                  #2889.20
        movups    %xmm3, (%rsp)                                 #[spill]
        andps     %xmm1, %xmm0                                  #2889.20
        pextrd    $2, %xmm10, %edi                              #2980.189
        movdqu    .L_2il0floatpacket.36(%rip), %xmm3            #2988.25
        lea       (%rcx,%rcx,2), %esi                           #2980.300
        cmpeqpd   %xmm8, %xmm0                                  #2890.25
        pand      %xmm1, %xmm3                                  #2988.25
        lea       (%rdi,%rdi,2), %r8d                           #2980.351
        shll      $3, %esi                                      #2980.300
        paddq     .L_2il0floatpacket.37(%rip), %xmm3            #2989.25
        movmskpd  %xmm0, %edx                                   #2892.48
        movq      16(%rsi,%r9), %xmm15                          #2982.639
        movdqa    %xmm3, %xmm7                                  #2997.16
        movups    %xmm6, 16(%rsp)                               #[spill]
        psrlq     $32, %xmm7                                    #2997.16
        shll      $3, %r8d                                      #2980.351
        movdqa    %xmm7, %xmm0                                  #3003.17
        movhpd    16(%r8,%r9), %xmm15                           #2982.607
        movdqu    .L_2il0floatpacket.38(%rip), %xmm6            #2990.21
        movaps    %xmm15, %xmm8                                 #2995.16
        movq      8(%rsi,%r9), %xmm13                           #2981.640
        psrlq     $32, %xmm8                                    #2995.16
        pand      %xmm6, %xmm3                                  #2998.16
        pand      %xmm6, %xmm15                                 #2996.16
        movhpd    8(%r8,%r9), %xmm13                            #2981.608
        movdqa    %xmm3, %xmm11                                 #3008.17
        pmuludq   %xmm8, %xmm0                                  #3003.17
        movaps    %xmm13, %xmm14                                #2993.16
        pmuludq   %xmm3, %xmm8                                  #3009.17
        pmuludq   %xmm7, %xmm15                                 #3004.17
        pand      %xmm6, %xmm13                                 #2994.16
        psrlq     $32, %xmm8                                    #3011.17
        pmuludq   %xmm13, %xmm11                                #3008.17
        psrlq     $32, %xmm14                                   #2993.16
        pmuludq   %xmm7, %xmm13                                 #3002.17
        paddq     %xmm8, %xmm0                                  #3015.17
        movdqa    %xmm3, %xmm12                                 #3007.17
        movdqa    %xmm6, %xmm10                                 #3019.17
        pmuludq   %xmm14, %xmm12                                #3007.17
        pand      %xmm11, %xmm10                                #3019.17
        pmuludq   %xmm7, %xmm14                                 #3001.17
        paddq     %xmm0, %xmm10                                 #3024.17
        movq      (%rsi,%r9), %xmm9                             #2980.639
        movdqa    %xmm6, %xmm4                                  #2992.16
        movhpd    (%r8,%r9), %xmm9                              #2980.607
        psrlq     $32, %xmm15                                   #3029.17
        psrlq     $32, %xmm11                                   #3012.17
        pand      %xmm9, %xmm4                                  #2992.16
        movdqa    %xmm3, %xmm5                                  #3006.17
        movdqa    %xmm6, %xmm8                                  #3020.17
        paddq     %xmm10, %xmm15                                #3030.17
        paddq     %xmm11, %xmm13                                #3016.17
        pmuludq   %xmm4, %xmm5                                  #3006.17
        pmuludq   %xmm4, %xmm7                                  #3000.17
        pand      %xmm12, %xmm8                                 #3020.17
        movaps    %xmm15, %xmm0                                 #3031.17
        paddq     %xmm13, %xmm8                                 #3025.17
        psrlq     $32, %xmm0                                    #3031.17
        psrlq     $32, %xmm12                                   #3013.17
        psrlq     $32, %xmm9                                    #2991.16
        movdqa    %xmm6, %xmm11                                 #3021.17
        paddq     %xmm8, %xmm0                                  #3032.17
        paddq     %xmm12, %xmm14                                #3017.17
        pmuludq   %xmm9, %xmm3                                  #3005.17
        pand      %xmm5, %xmm11                                 #3021.17
        movdqa    %xmm0, %xmm13                                 #3033.17
        paddq     %xmm14, %xmm11                                #3026.17
        psrlq     $32, %xmm13                                   #3033.17
        psrlq     $32, %xmm5                                    #3014.17
        paddq     %xmm11, %xmm13                                #3034.17
        paddq     %xmm5, %xmm7                                  #3018.17
        pand      %xmm6, %xmm3                                  #3022.17
        movdqa    %xmm13, %xmm10                                #3035.17
        paddq     %xmm7, %xmm3                                  #3027.17
        psrlq     $32, %xmm10                                   #3035.17
        pand      %xmm6, %xmm13                                 #3038.17
        paddq     %xmm3, %xmm10                                 #3036.17
        psllq     $32, %xmm10                                   #3039.23
        pand      %xmm6, %xmm15                                 #3037.17
        paddq     %xmm13, %xmm10                                #3041.23
        movdqu    .L_2il0floatpacket.39(%rip), %xmm5            #3044.21
        movdqa    %xmm10, %xmm7                                 #3047.24
        movdqu    .L_2il0floatpacket.40(%rip), %xmm12           #3046.26
        pand      %xmm1, %xmm5                                  #3044.21
        movups    .L_2il0floatpacket.41(%rip), %xmm14           #3050.15
        psrlq     $12, %xmm7                                    #3047.24
        pxor      %xmm5, %xmm12                                 #3046.26
        movaps    %xmm14, %xmm9                                 #3051.19
        por       %xmm12, %xmm7                                 #3048.24
        psllq     $32, %xmm0                                    #3040.23
        addpd     %xmm7, %xmm9                                  #3051.19
        paddq     %xmm15, %xmm0                                 #3042.23
        movaps    %xmm9, %xmm6                                  #3052.9
        subpd     %xmm14, %xmm6                                 #3052.9
        movdqu    .L_2il0floatpacket.46(%rip), %xmm3            #3069.24
        movdqu    .L_2il0floatpacket.44(%rip), %xmm8            #3060.24
        pand      %xmm10, %xmm3                                 #3069.24
        movdqu    .L_2il0floatpacket.43(%rip), %xmm4            #3058.26
        pand      %xmm0, %xmm8                                  #3060.24
        psllq     $40, %xmm3                                    #3070.24
        psrlq     $24, %xmm0                                    #3071.23
        pxor      %xmm5, %xmm4                                  #3058.26
        por       %xmm0, %xmm3                                  #3072.24
        pxor      .L_2il0floatpacket.45(%rip), %xmm5            #3067.26
        psllq     $28, %xmm8                                    #3061.24
        subpd     %xmm6, %xmm7                                  #3053.17
        por       %xmm5, %xmm3                                  #3073.24
        movaps    %xmm7, %xmm0                                  #3077.11
        subpd     %xmm5, %xmm3                                  #3076.18
        addpd     %xmm3, %xmm0                                  #3077.11
        subpd     %xmm0, %xmm7                                  #3078.17
        por       %xmm4, %xmm8                                  #3062.24
        subpd     %xmm4, %xmm8                                  #3065.17
        addpd     %xmm7, %xmm3                                  #3079.18
        movups    .L_2il0floatpacket.50(%rip), %xmm10           #3085.16
        andps     %xmm0, %xmm10                                 #3085.16
        addpd     %xmm3, %xmm8                                  #3080.11
        subpd     %xmm10, %xmm0                                 #3086.17
        mulpd     .L_2il0floatpacket.47(%rip), %xmm8            #3090.12
        movups    .L_2il0floatpacket.48(%rip), %xmm15           #3082.16
        movaps    %xmm10, %xmm11                                #3087.16
        movups    .L_2il0floatpacket.49(%rip), %xmm6            #3083.17
        mulpd     %xmm15, %xmm11                                #3087.16
        mulpd     %xmm0, %xmm15                                 #3088.12
        mulpd     %xmm6, %xmm10                                 #3089.12
        mulpd     %xmm0, %xmm6                                  #3091.12
        addpd     %xmm10, %xmm15                                #3092.13
        addpd     %xmm6, %xmm8                                  #3093.13
        addpd     %xmm8, %xmm15                                 #3094.13
        movups    .L_2il0floatpacket.52(%rip), %xmm4            #3099.16
        movaps    %xmm15, %xmm8                                 #3095.13
        movups    .L_2il0floatpacket.51(%rip), %xmm7            #3100.11
        movaps    %xmm4, %xmm6                                  #3101.17
        andps     %xmm1, %xmm7                                  #3100.11
        addpd     %xmm11, %xmm8                                 #3095.13
        cmpltpd   %xmm7, %xmm6                                  #3101.17
        cmplepd   %xmm4, %xmm7                                  #3102.20
        subpd     %xmm8, %xmm11                                 #3096.16
        andps     .L_2il0floatpacket.42(%rip), %xmm9            #3056.19
        andps     %xmm1, %xmm7                                  #3103.24
        movd      %xmm9, %r10d                                  #3108.132
        andps     %xmm6, %xmm8                                  #3104.25
        orps      %xmm8, %xmm7                                  #3105.9
        movaps    %xmm7, %xmm1                                  #3112.26
        movaps    %xmm7, %xmm14                                 #3120.19
        shll      $5, %r10d                                     #3108.302
        movaps    %xmm7, %xmm13                                 #3111.23
        pextrd    $2, %xmm9, %r11d                              #3108.194
        shll      $5, %r11d                                     #3108.353
        movq      8(%r10,%rax), %xmm0                           #3109.618
        movhpd    8(%r11,%rax), %xmm0                           #3109.586
        mulpd     %xmm0, %xmm1                                  #3112.26
        mulpd     %xmm7, %xmm14                                 #3120.19
        addpd     %xmm11, %xmm15                                #3097.13
        movq      16(%r10,%rax), %xmm3                          #3110.616
        andps     %xmm15, %xmm6                                 #3106.9
        movhpd    16(%r11,%rax), %xmm3                          #3110.584
        movaps    %xmm3, %xmm12                                 #3113.26
        movaps    %xmm3, %xmm9                                  #3115.22
        addpd     %xmm1, %xmm12                                 #3113.26
        subpd     %xmm12, %xmm9                                 #3115.22
        addpd     %xmm9, %xmm1                                  #3116.22
        movups    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #3123.30
        movaps    %xmm12, %xmm5                                 #3114.25
        movups    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #3130.30
        movq      (%r10,%rax), %xmm4                            #3108.616
        mulpd     %xmm14, %xmm9                                 #3123.30
        mulpd     %xmm14, %xmm10                                #3130.30
        movhpd    (%r11,%rax), %xmm4                            #3108.584
        mulpd     %xmm4, %xmm13                                 #3111.23
        addpd     %xmm0, %xmm4                                  #3126.17
        addpd     %xmm13, %xmm5                                 #3114.25
        movups    16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3123.18
        movups    17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #3130.18
        addpd     %xmm9, %xmm8                                  #3123.18
        addpd     %xmm10, %xmm11                                #3130.18
        subpd     %xmm5, %xmm12                                 #3117.22
        mulpd     %xmm14, %xmm8                                 #3124.18
        mulpd     %xmm14, %xmm11                                #3132.30
        addpd     %xmm12, %xmm13                                #3118.22
        mulpd     %xmm7, %xmm8                                  #3125.18
        mulpd     %xmm3, %xmm7                                  #3135.36
        mulpd     %xmm4, %xmm8                                  #3127.33
        subpd     %xmm7, %xmm4                                  #3135.17
        addpd     %xmm13, %xmm1                                 #3119.25
        mulpd     %xmm4, %xmm6                                  #3137.32
        addpd     %xmm8, %xmm1                                  #3127.21
        movups    17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #3132.18
        addpd     %xmm11, %xmm12                                #3132.18
        mulpd     %xmm12, %xmm14                                #3133.18
        mulpd     %xmm3, %xmm14                                 #3134.33
        movq      24(%r10,%rax), %xmm3                          #3136.616
        movhpd    24(%r11,%rax), %xmm3                          #3136.584
        addpd     %xmm14, %xmm1                                 #3134.21
        addpd     %xmm6, %xmm3                                  #3137.20
        addpd     %xmm3, %xmm1                                  #3138.21
        addpd     %xmm1, %xmm5                                  #3139.24
        movups    (%rsp), %xmm0                                 #3141.18[spill]
        pxor      %xmm2, %xmm5                                  #3140.24
        movups    16(%rsp), %xmm6                               #3141.18[spill]
        blendvpd  %xmm0, %xmm5, %xmm6                           #3141.18
        movups    32(%rsp), %xmm0                               #3141.18[spill]
        jmp       ..B1.2        # Prob 100%                     #3141.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm6
	.cfi_endproc
# mark_end;
	.type	__svml_sin2_ha_e9,@function
	.size	__svml_sin2_ha_e9,.-__svml_sin2_ha_e9
..LN__svml_sin2_ha_e9.0:
	.data
# -- End  __svml_sin2_ha_e9
	.text
.L_2__routine_start___svml_sin4_ha_l9_1:
# -- Begin  __svml_sin4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin4_ha_l9
# --- __svml_sin4_ha_l9(__m256d)
__svml_sin4_ha_l9:
# parameter 1: %ymm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3152.1
        .byte     15                                            #3495.18
        .byte     30                                            #3495.18
        .byte     250                                           #3495.18
	.cfi_startproc
..___tag_value___svml_sin4_ha_l9.30:
..L31:
                                                         #3152.1
        pushq     %rbp                                          #3152.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3152.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3152.1
        subq      $192, %rsp                                    #3152.1
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r8    #3218.728
        vmovupd   16384+__svml_dsin_ha_data_internal_ha(%rip), %ymm13 #3199.53
        xorl      %esi, %esi                                    #3198.1
        vmovupd   16576+__svml_dsin_ha_data_internal_ha(%rip), %ymm11 #3203.49
        vmovupd   16640+__svml_dsin_ha_data_internal_ha(%rip), %ymm7 #3204.54
        vmovupd   16768+__svml_dsin_ha_data_internal_ha(%rip), %ymm8 #3207.51
        vmovupd   16832+__svml_dsin_ha_data_internal_ha(%rip), %ymm9 #3209.51
        vmovapd   %ymm0, %ymm6                                  #3152.1
        vandnpd   %ymm6, %ymm13, %ymm14                         #3200.17
        vandpd    %ymm13, %ymm6, %ymm13                         #3201.12
        vfmadd213pd %ymm7, %ymm13, %ymm11                       #3205.13
        vsubpd    %ymm7, %ymm11, %ymm5                          #3206.13
        vfnmadd213pd %ymm13, %ymm5, %ymm8                       #3208.14
        vmovapd   %ymm9, %ymm4                                  #3210.17
        vfnmadd213pd %ymm8, %ymm5, %ymm4                        #3210.17
        vandps    16704+__svml_dsin_ha_data_internal_ha(%rip), %ymm11, %ymm12 #3217.19
        vsubpd    %ymm4, %ymm8, %ymm10                          #3211.17
        vfnmadd231pd %ymm5, %ymm9, %ymm10                       #3212.17
        vfnmadd132pd 16896+__svml_dsin_ha_data_internal_ha(%rip), %ymm10, %ymm5 #3214.13
        vextractf128 $1, %ymm12, %xmm0                          #3218.326
        vmovd     %xmm12, %eax                                  #3218.128
        vmovd     %xmm0, %ecx                                   #3218.307
        shll      $5, %eax                                      #3218.539
        vpextrd   $2, %xmm12, %edx                              #3218.216
        shll      $5, %ecx                                      #3218.641
        vpextrd   $2, %xmm0, %edi                               #3218.400
        shll      $5, %edx                                      #3218.590
        vmovq     (%rax,%r8), %xmm15                            #3218.1227
        shll      $5, %edi                                      #3218.692
        vmovq     (%rcx,%r8), %xmm3                             #3218.1427
        vmovhpd   (%rdx,%r8), %xmm15, %xmm1                     #3218.1195
        vmovq     16(%rax,%r8), %xmm11                          #3220.1227
        vmovhpd   (%rdi,%r8), %xmm3, %xmm2                      #3218.1395
        vmovq     16(%rcx,%r8), %xmm15                          #3220.1427
        vmovq     8(%rax,%r8), %xmm7                            #3219.1229
        vmovhpd   16(%rdx,%r8), %xmm11, %xmm12                  #3220.1195
        vmovq     8(%rcx,%r8), %xmm9                            #3219.1429
        vmovhpd   8(%rdx,%r8), %xmm7, %xmm8                     #3219.1197
        vmovhpd   8(%rdi,%r8), %xmm9, %xmm10                    #3219.1397
        vinsertf128 $1, %xmm2, %ymm1, %ymm3                     #3218.1149
        vmovhpd   16(%rdi,%r8), %xmm15, %xmm1                   #3220.1395
        vinsertf128 $1, %xmm1, %ymm12, %ymm0                    #3220.1149
        vmovaps   %ymm0, %ymm7                                  #3221.22
        vmovaps   %ymm3, %ymm1                                  #3222.21
        vinsertf128 $1, %xmm10, %ymm8, %ymm2                    #3219.1151
        vfmadd231pd %ymm4, %ymm2, %ymm7                         #3221.22
        vmovupd   16960+__svml_dsin_ha_data_internal_ha(%rip), %ymm10 #3231.14
        vfmadd213pd %ymm7, %ymm4, %ymm1                         #3222.21
        vsubpd    %ymm7, %ymm0, %ymm8                           #3223.18
        vsubpd    %ymm1, %ymm7, %ymm9                           #3225.18
        vmulpd    %ymm4, %ymm4, %ymm7                           #3228.15
        vfmadd231pd %ymm4, %ymm2, %ymm8                         #3224.18
        vfmadd231pd %ymm3, %ymm4, %ymm9                         #3226.18
        vaddpd    %ymm2, %ymm3, %ymm3                           #3234.13
        vfmadd231pd 17024+__svml_dsin_ha_data_internal_ha(%rip), %ymm7, %ymm10 #3231.14
        vmovupd   17152+__svml_dsin_ha_data_internal_ha(%rip), %ymm2 #3238.14
        vaddpd    %ymm9, %ymm8, %ymm15                          #3227.21
        vmulpd    %ymm10, %ymm7, %ymm11                         #3232.14
        vfmadd231pd 17216+__svml_dsin_ha_data_internal_ha(%rip), %ymm7, %ymm2 #3238.14
        vmulpd    %ymm11, %ymm4, %ymm8                          #3233.14
        vfmadd213pd 17088+__svml_dsin_ha_data_internal_ha(%rip), %ymm7, %ymm2 #3240.14
        vfnmadd213pd %ymm3, %ymm0, %ymm4                        #3243.13
        vfmadd213pd %ymm15, %ymm3, %ymm8                        #3235.17
        vmulpd    %ymm2, %ymm7, %ymm7                           #3241.14
        vmovq     24(%rcx,%r8), %xmm3                           #3244.1427
        vfmadd213pd %ymm8, %ymm0, %ymm7                         #3242.17
        vmovq     24(%rax,%r8), %xmm0                           #3244.1227
        vmovhpd   24(%rdx,%r8), %xmm0, %xmm0                    #3244.1195
        vmovhpd   24(%rdi,%r8), %xmm3, %xmm8                    #3244.1395
        vinsertf128 $1, %xmm8, %ymm0, %ymm2                     #3244.1149
        vfmadd213pd %ymm2, %ymm4, %ymm5                         #3245.16
        vaddpd    %ymm5, %ymm7, %ymm4                           #3246.17
        vaddpd    %ymm4, %ymm1, %ymm5                           #3247.14
        vcmpnle_uqpd 16448+__svml_dsin_ha_data_internal_ha(%rip), %ymm13, %ymm1 #3249.26
        vmovmskpd %ymm1, %r9d                                   #3251.78
        vxorpd    %ymm14, %ymm5, %ymm0                          #3248.14
        testl     %r9d, %r9d                                    #3252.66
        jne       ..B2.12       # Prob 5%                       #3252.66
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm1 ymm6 ymm13 ymm14
..B2.2:                         # Preds ..B2.12 ..B2.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #3498.52
        jne       ..B2.4        # Prob 5%                       #3498.52
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm6
..B2.3:                         # Preds ..B2.4 ..B2.10 ..B2.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #3501.12
        popq      %rbp                                          #3501.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3501.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm6, 64(%rsp)                               #3498.200
        vmovupd   %ymm0, 128(%rsp)                              #3498.276
        je        ..B2.3        # Prob 95%                      #3498.380
                                # LOE rbx r12 r13 r14 r15 esi ymm0
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #3498.460
                                # LOE rbx r12 r13 r14 r15 eax esi
..B2.15:                        # Preds ..B2.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %esi, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.9 ..B2.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #3498.523
        jc        ..B2.11       # Prob 5%                       #3498.523
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #3498.476
        cmpl      $4, %r12d                                     #3498.471
        jl        ..B2.8        # Prob 82%                      #3498.471
                                # LOE rbx r12 r14 r15 r13d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #3498.682
        jmp       ..B2.3        # Prob 100%                     #3498.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #3498.552
        lea       128(%rsp,%r12,8), %rsi                        #3498.552
..___tag_value___svml_sin4_ha_l9.48:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #3498.552
..___tag_value___svml_sin4_ha_l9.49:
        jmp       ..B2.9        # Prob 100%                     #3498.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B2.12:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.53(%rip), %ymm13, %ymm7    #3345.18
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rcx #3347.736
        vmovupd   %ymm6, 64(%rsp)                               #[spill]
        vmovupd   %ymm1, (%rsp)                                 #[spill]
        vmovupd   16512+__svml_dsin_ha_data_internal_ha(%rip), %ymm12 #3255.58
        vmovupd   %ymm0, 32(%rsp)                               #[spill]
        vpsrlq    $52, %ymm7, %ymm6                             #3346.18
        vandpd    %ymm13, %ymm12, %ymm8                         #3256.20
        vcmpeqpd  %ymm12, %ymm8, %ymm9                          #3257.25
        vmovmskpd %ymm9, %esi                                   #3259.48
        vextractf128 $1, %ymm6, %xmm1                           #3347.328
        vmovd     %xmm6, %edi                                   #3347.124
        vmovd     %xmm1, %r9d                                   #3347.309
        vpextrd   $2, %xmm6, %r8d                               #3347.215
        lea       (%rdi,%rdi,2), %edx                           #3347.547
        vpextrd   $2, %xmm1, %r11d                              #3347.405
        lea       (%r9,%r9,2), %r10d                            #3347.649
        shll      $3, %edx                                      #3347.547
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r9    #3464.732
        shll      $3, %r10d                                     #3347.649
        vmovq     16(%rdx,%rcx), %xmm11                         #3349.1284
        lea       (%r8,%r8,2), %eax                             #3347.598
        shll      $3, %eax                                      #3347.598
        lea       (%r11,%r11,2), %edi                           #3347.700
        vpand     .L_2il0floatpacket.54(%rip), %ymm13, %ymm6    #3355.21
        vmovq     16(%r10,%rcx), %xmm9                          #3349.1484
        vmovhpd   16(%rax,%rcx), %xmm11, %xmm12                 #3349.1252
        vmovq     (%rdx,%rcx), %xmm3                            #3347.1284
        shll      $3, %edi                                      #3347.700
        vmovhpd   16(%rdi,%rcx), %xmm9, %xmm7                   #3349.1452
        vpaddq    .L_2il0floatpacket.55(%rip), %ymm6, %ymm1     #3356.21
        vmovq     (%r10,%rcx), %xmm4                            #3347.1484
        vmovhpd   (%rax,%rcx), %xmm3, %xmm10                    #3347.1252
        vmovhpd   (%rdi,%rcx), %xmm4, %xmm0                     #3347.1452
        vpsrlq    $32, %ymm1, %ymm9                             #3364.12
        vmovq     8(%rdx,%rcx), %xmm2                           #3348.1285
        vmovq     8(%r10,%rcx), %xmm8                           #3348.1485
        vmovhpd   8(%rax,%rcx), %xmm2, %xmm15                   #3348.1253
        vmovhpd   8(%rdi,%rcx), %xmm8, %xmm5                    #3348.1453
        vinsertf128 $1, %xmm7, %ymm12, %ymm11                   #3349.1206
        vmovdqu   .L_2il0floatpacket.56(%rip), %ymm12           #3357.17
        vpand     %ymm12, %ymm1, %ymm6                          #3365.12
        vinsertf128 $1, %xmm0, %ymm10, %ymm10                   #3347.1206
        vpsrlq    $32, %ymm11, %ymm0                            #3362.12
        vpand     %ymm12, %ymm10, %ymm7                         #3359.12
        vpsrlq    $32, %ymm10, %ymm10                           #3358.12
        vpmuludq  %ymm7, %ymm6, %ymm8                           #3373.13
        vinsertf128 $1, %xmm5, %ymm15, %ymm3                    #3348.1207
        vpand     %ymm12, %ymm3, %ymm2                          #3361.12
        vpand     %ymm12, %ymm11, %ymm15                        #3363.12
        vpsrlq    $32, %ymm3, %ymm4                             #3360.12
        vpmuludq  %ymm0, %ymm9, %ymm11                          #3370.13
        vpmuludq  %ymm0, %ymm6, %ymm0                           #3376.13
        vpsrlq    $32, %ymm0, %ymm0                             #3378.13
        vpmuludq  %ymm2, %ymm6, %ymm1                           #3375.13
        vpmuludq  %ymm15, %ymm9, %ymm5                          #3371.13
        vpand     %ymm12, %ymm1, %ymm15                         #3386.13
        vpaddq    %ymm0, %ymm11, %ymm11                         #3382.13
        vpsrlq    $32, %ymm5, %ymm5                             #3396.13
        vpsrlq    $32, %ymm1, %ymm1                             #3379.13
        vpaddq    %ymm11, %ymm15, %ymm15                        #3391.13
        vpmuludq  %ymm4, %ymm6, %ymm3                           #3374.13
        vpmuludq  %ymm2, %ymm9, %ymm2                           #3369.13
        vpaddq    %ymm15, %ymm5, %ymm11                         #3397.13
        vpand     %ymm12, %ymm3, %ymm15                         #3387.13
        vpaddq    %ymm1, %ymm2, %ymm0                           #3383.13
        vpand     %ymm12, %ymm8, %ymm2                          #3388.13
        vpsrlq    $32, %ymm11, %ymm5                            #3398.13
        vpsrlq    $32, %ymm8, %ymm8                             #3381.13
        vpaddq    %ymm0, %ymm15, %ymm15                         #3392.13
        vpand     %ymm12, %ymm11, %ymm11                        #3404.13
        vpaddq    %ymm15, %ymm5, %ymm5                          #3399.13
        vpsrlq    $32, %ymm3, %ymm15                            #3380.13
        vpsrlq    $32, %ymm5, %ymm0                             #3400.13
        vpsllq    $32, %ymm5, %ymm5                             #3407.19
        vpmuludq  %ymm4, %ymm9, %ymm4                           #3368.13
        vpaddq    %ymm15, %ymm4, %ymm3                          #3384.13
        vpaddq    %ymm3, %ymm2, %ymm1                           #3393.13
        vpmuludq  %ymm10, %ymm6, %ymm15                         #3372.13
        vpmuludq  %ymm7, %ymm9, %ymm9                           #3367.13
        vpaddq    %ymm1, %ymm0, %ymm1                           #3401.13
        vpand     %ymm12, %ymm15, %ymm6                         #3389.13
        vpaddq    %ymm8, %ymm9, %ymm7                           #3385.13
        vpsrlq    $32, %ymm1, %ymm4                             #3402.13
        vpand     %ymm12, %ymm1, %ymm1                          #3405.13
        vpaddq    %ymm7, %ymm6, %ymm3                           #3394.13
        vpaddq    %ymm11, %ymm5, %ymm6                          #3409.19
        vpaddq    %ymm3, %ymm4, %ymm10                          #3403.13
        vpand     .L_2il0floatpacket.62(%rip), %ymm6, %ymm11    #3427.20
        vpsrlq    $24, %ymm6, %ymm6                             #3438.19
        vmovupd   .L_2il0floatpacket.59(%rip), %ymm4            #3417.15
        vpsllq    $32, %ymm10, %ymm0                            #3406.19
        vpand     .L_2il0floatpacket.57(%rip), %ymm13, %ymm10   #3411.17
        vpaddq    %ymm1, %ymm0, %ymm3                           #3408.19
        vpxor     .L_2il0floatpacket.58(%rip), %ymm10, %ymm2    #3413.22
        vpxor     .L_2il0floatpacket.61(%rip), %ymm10, %ymm1    #3425.22
        vpsrlq    $12, %ymm3, %ymm12                            #3414.20
        vpand     .L_2il0floatpacket.64(%rip), %ymm3, %ymm3     #3436.20
        vpor      %ymm2, %ymm12, %ymm15                         #3415.20
        vpsllq    $40, %ymm3, %ymm2                             #3437.20
        vpsllq    $28, %ymm11, %ymm12                           #3428.20
        vaddpd    %ymm4, %ymm15, %ymm5                          #3418.19
        vpor      %ymm1, %ymm12, %ymm0                          #3429.20
        vsubpd    %ymm4, %ymm5, %ymm8                           #3419.9
        vpor      %ymm6, %ymm2, %ymm4                           #3439.20
        vsubpd    %ymm8, %ymm15, %ymm7                          #3420.17
        vsubpd    %ymm1, %ymm0, %ymm15                          #3432.17
        vpxor     .L_2il0floatpacket.63(%rip), %ymm10, %ymm8    #3434.22
        vmovupd   .L_2il0floatpacket.68(%rip), %ymm0            #3455.16
        vpor      %ymm8, %ymm4, %ymm10                          #3440.20
        vandps    .L_2il0floatpacket.60(%rip), %ymm5, %ymm9     #3423.15
        vsubpd    %ymm8, %ymm10, %ymm5                          #3443.18
        vaddpd    %ymm5, %ymm7, %ymm11                          #3444.11
        vsubpd    %ymm11, %ymm7, %ymm7                          #3445.17
        vaddpd    %ymm7, %ymm5, %ymm8                           #3446.18
        vmovupd   .L_2il0floatpacket.65(%rip), %ymm5            #3448.16
        vaddpd    %ymm8, %ymm15, %ymm4                          #3447.11
        vmulpd    %ymm11, %ymm5, %ymm2                          #3450.13
        vmovapd   %ymm5, %ymm15                                 #3451.13
        vfmsub213pd %ymm2, %ymm11, %ymm15                       #3451.13
        vandpd    .L_2il0floatpacket.67(%rip), %ymm13, %ymm12   #3456.11
        vcmpgt_oqpd %ymm0, %ymm12, %ymm3                        #3457.17
        vcmple_oqpd %ymm0, %ymm12, %ymm1                        #3458.20
        vfmadd132pd .L_2il0floatpacket.66(%rip), %ymm15, %ymm11 #3452.13
        vfmadd213pd %ymm11, %ymm5, %ymm4                        #3453.13
        vandpd    %ymm13, %ymm1, %ymm13                         #3459.24
        vandpd    %ymm2, %ymm3, %ymm15                          #3460.25
        vorpd     %ymm15, %ymm13, %ymm8                         #3461.9
        vandpd    %ymm4, %ymm3, %ymm5                           #3462.9
        vmovapd   %ymm8, %ymm3                                  #3467.26
        vextractf128 $1, %ymm9, %xmm12                          #3464.330
        vmovd     %xmm9, %eax                                   #3464.132
        vmovd     %xmm12, %ecx                                  #3464.311
        shll      $5, %eax                                      #3464.543
        vpextrd   $2, %xmm9, %edx                               #3464.220
        shll      $5, %ecx                                      #3464.645
        vpextrd   $2, %xmm12, %r8d                              #3464.404
        shll      $5, %edx                                      #3464.594
        shll      $5, %r8d                                      #3464.696
        vmovq     (%rax,%r9), %xmm13                            #3464.1231
        vmovq     (%rcx,%r9), %xmm0                             #3464.1431
        vmovhpd   (%rdx,%r9), %xmm13, %xmm11                    #3464.1199
        vmovhpd   (%r8,%r9), %xmm0, %xmm1                       #3464.1399
        vmovq     8(%rax,%r9), %xmm2                            #3465.1233
        vmovq     16(%rax,%r9), %xmm15                          #3466.1231
        vmovq     8(%rcx,%r9), %xmm7                            #3465.1433
        vmovq     16(%rcx,%r9), %xmm0                           #3466.1431
        vmovhpd   8(%rdx,%r9), %xmm2, %xmm6                     #3465.1201
        vmovhpd   16(%rdx,%r9), %xmm15, %xmm13                  #3466.1199
        vmovhpd   8(%r8,%r9), %xmm7, %xmm10                     #3465.1401
        vmulpd    %ymm8, %ymm8, %ymm2                           #3474.19
        vinsertf128 $1, %xmm1, %ymm11, %ymm11                   #3464.1153
        vmovhpd   16(%r8,%r9), %xmm0, %xmm1                     #3466.1399
        vmovaps   %ymm11, %ymm15                                #3468.25
        vinsertf128 $1, %xmm10, %ymm6, %ymm12                   #3465.1155
        vinsertf128 $1, %xmm1, %ymm13, %ymm13                   #3466.1153
        vfmadd213pd %ymm13, %ymm12, %ymm3                       #3467.26
        vmovupd   16960+__svml_dsin_ha_data_internal_ha(%rip), %ymm1 #3477.18
        vmovq     24(%rax,%r9), %xmm6                           #3490.1231
        vmovq     24(%rcx,%r9), %xmm10                          #3490.1431
        vmovhpd   24(%rdx,%r9), %xmm6, %xmm7                    #3490.1199
        vfmadd213pd %ymm3, %ymm8, %ymm15                        #3468.25
        vfmadd231pd 17024+__svml_dsin_ha_data_internal_ha(%rip), %ymm2, %ymm1 #3477.18
        vmovupd   64(%rsp), %ymm6                               #3495.18[spill]
        vsubpd    %ymm15, %ymm3, %ymm0                          #3471.22
        vsubpd    %ymm3, %ymm13, %ymm4                          #3469.22
        vmulpd    %ymm1, %ymm2, %ymm9                           #3478.18
        vfmadd231pd %ymm11, %ymm8, %ymm0                        #3472.22
        vfmadd231pd %ymm8, %ymm12, %ymm4                        #3470.22
        vaddpd    %ymm12, %ymm11, %ymm1                         #3480.17
        vmovhpd   24(%r8,%r9), %xmm10, %xmm12                   #3490.1399
        vmovupd   17152+__svml_dsin_ha_data_internal_ha(%rip), %ymm11 #3484.18
        vmulpd    %ymm9, %ymm8, %ymm3                           #3479.18
        vfnmadd213pd %ymm1, %ymm13, %ymm8                       #3489.17
        vaddpd    %ymm0, %ymm4, %ymm0                           #3473.25
        vfmadd231pd 17216+__svml_dsin_ha_data_internal_ha(%rip), %ymm2, %ymm11 #3484.18
        vfmadd213pd %ymm0, %ymm1, %ymm3                         #3481.21
        vfmadd213pd 17088+__svml_dsin_ha_data_internal_ha(%rip), %ymm2, %ymm11 #3486.18
        vmulpd    %ymm11, %ymm2, %ymm0                          #3487.18
        vfmadd213pd %ymm3, %ymm13, %ymm0                        #3488.21
        vinsertf128 $1, %xmm12, %ymm7, %ymm1                    #3490.1153
        vfmadd213pd %ymm1, %ymm8, %ymm5                         #3491.20
        vmovupd   (%rsp), %ymm1                                 #3495.18[spill]
        vaddpd    %ymm5, %ymm0, %ymm5                           #3492.21
        vmovupd   32(%rsp), %ymm0                               #3495.18[spill]
        vaddpd    %ymm5, %ymm15, %ymm8                          #3493.24
        vxorpd    %ymm14, %ymm8, %ymm14                         #3494.24
        vblendvpd %ymm1, %ymm14, %ymm0, %ymm0                   #3495.18
        jmp       ..B2.2        # Prob 100%                     #3495.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm6
	.cfi_endproc
# mark_end;
	.type	__svml_sin4_ha_l9,@function
	.size	__svml_sin4_ha_l9,.-__svml_sin4_ha_l9
..LN__svml_sin4_ha_l9.1:
	.data
# -- End  __svml_sin4_ha_l9
	.text
.L_2__routine_start___svml_sin1_ha_e9_2:
# -- Begin  __svml_sin1_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin1_ha_e9
# --- __svml_sin1_ha_e9(__m128d)
__svml_sin1_ha_e9:
# parameter 1: %xmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3506.1
        .byte     15                                            #3865.18
        .byte     30                                            #3865.18
        .byte     250                                           #3865.18
	.cfi_startproc
..___tag_value___svml_sin1_ha_e9.59:
..L60:
                                                         #3506.1
        pushq     %rbp                                          #3506.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3506.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3506.1
        subq      $128, %rsp                                    #3506.1
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rax   #3573.371
        movsd     16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm2 #3553.19
        xorl      %edx, %edx                                    #3552.1
        movaps    %xmm2, %xmm3                                  #3554.17
        andps     %xmm0, %xmm2                                  #3555.12
        movaps    %xmm2, %xmm10                                 #3559.25
        movaps    %xmm2, %xmm5                                  #3562.14
        mulsd     16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #3559.25
        andnps    %xmm0, %xmm3                                  #3554.17
        movsd     16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm1 #3558.20
        movq      16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #3571.27
        addsd     %xmm1, %xmm10                                 #3559.13
        movaps    %xmm10, %xmm8                                 #3560.13
        pand      %xmm9, %xmm10                                 #3572.23
        movd      %xmm10, %ecx                                  #3573.128
        subsd     %xmm1, %xmm8                                  #3560.13
        movaps    %xmm8, %xmm6                                  #3562.36
        movaps    %xmm8, %xmm7                                  #3564.19
        mulsd     16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm6 #3562.36
        mulsd     16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm7 #3564.19
        mulsd     16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3569.32
        subsd     %xmm6, %xmm5                                  #3562.14
        movaps    %xmm5, %xmm4                                  #3565.17
        shll      $5, %ecx                                      #3573.231
        subsd     %xmm7, %xmm4                                  #3565.17
        movq      8(%rax,%rcx), %xmm14                          #3574.373
        movaps    %xmm4, %xmm11                                 #3577.22
        mulsd     %xmm14, %xmm11                                #3577.22
        subsd     %xmm4, %xmm5                                  #3566.17
        movq      16(%rax,%rcx), %xmm6                          #3575.371
        subsd     %xmm7, %xmm5                                  #3567.17
        movq      (%rcx,%rax), %xmm7                            #3573.371
        movdqa    %xmm6, %xmm15                                 #3578.22
        movdqa    %xmm7, %xmm12                                 #3576.19
        movdqa    %xmm6, %xmm13                                 #3580.18
        mulsd     %xmm4, %xmm12                                 #3576.19
        addsd     %xmm11, %xmm15                                #3578.22
        subsd     %xmm8, %xmm5                                  #3569.13
        addsd     %xmm14, %xmm7                                 #3591.13
        subsd     %xmm15, %xmm13                                #3580.18
        movaps    %xmm12, %xmm1                                 #3579.21
        addsd     %xmm11, %xmm13                                #3581.18
        addsd     %xmm15, %xmm1                                 #3579.21
        movaps    %xmm4, %xmm11                                 #3585.15
        subsd     %xmm1, %xmm15                                 #3582.18
        mulsd     %xmm4, %xmm11                                 #3585.15
        addsd     %xmm12, %xmm15                                #3583.18
        movsd     17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #3588.26
        addsd     %xmm13, %xmm15                                #3584.21
        mulsd     %xmm11, %xmm12                                #3588.26
        movsd     17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3595.26
        mulsd     %xmm11, %xmm8                                 #3595.26
        addsd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #3588.14
        mulsd     %xmm11, %xmm12                                #3589.14
        addsd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3595.14
        mulsd     %xmm11, %xmm8                                 #3597.26
        mulsd     %xmm4, %xmm12                                 #3590.14
        mulsd     %xmm6, %xmm4                                  #3600.32
        addsd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3597.14
        mulsd     %xmm7, %xmm12                                 #3592.29
        subsd     %xmm4, %xmm7                                  #3600.13
        mulsd     %xmm11, %xmm8                                 #3598.14
        addsd     %xmm15, %xmm12                                #3592.17
        mulsd     %xmm7, %xmm5                                  #3602.28
        mulsd     %xmm6, %xmm8                                  #3599.29
        addsd     24(%rax,%rcx), %xmm5                          #3602.16
        addsd     %xmm12, %xmm8                                 #3599.17
        movaps    %xmm2, %xmm4                                  #3606.26
        addsd     %xmm5, %xmm8                                  #3603.17
        cmpnlesd  16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm4 #3606.26
        addsd     %xmm8, %xmm1                                  #3604.14
        movmskpd  %xmm4, %esi                                   #3608.78
        pxor      %xmm3, %xmm1                                  #3605.14
        testl     $1, %esi                                      #3609.55
        jne       ..B3.8        # Prob 5%                       #3609.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm3 xmm4
..B3.2:                         # Preds ..B3.8 ..B3.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #3868.52
        jne       ..B3.4        # Prob 5%                       #3868.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm1
..B3.3:                         # Preds ..B3.5 ..B3.2
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #3871.12
        movq      %rbp, %rsp                                    #3871.12
        popq      %rbp                                          #3871.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3871.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm0, (%rsp)                                 #3868.160
        movsd     %xmm1, 64(%rsp)                               #3868.233
        jne       ..B3.6        # Prob 5%                       #3868.374
                                # LOE rbx r12 r13 r14 r15 edx
..B3.5:                         # Preds ..B3.7 ..B3.6 ..B3.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm1                               #3868.625
        jmp       ..B3.3        # Prob 100%                     #3868.625
                                # LOE rbx r12 r13 r14 r15 xmm1
..B3.6:                         # Preds ..B3.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B3.5        # Prob 95%                      #3868.517
                                # LOE rbx r12 r13 r14 r15
..B3.7:                         # Preds ..B3.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #3868.546
        lea       64(%rsp), %rsi                                #3868.546
..___tag_value___svml_sin1_ha_e9.68:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #3868.546
..___tag_value___svml_sin1_ha_e9.69:
        jmp       ..B3.5        # Prob 100%                     #3868.546
                                # LOE rbx r12 r13 r14 r15
..B3.8:                         # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.35(%rip), %xmm9            #3702.22
        movaps    %xmm2, %xmm7                                  #3613.20
        pand      %xmm2, %xmm9                                  #3702.22
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rdi #3704.383
        psrlq     $52, %xmm9                                    #3703.22
        movd      %xmm9, %ecx                                   #3704.124
        movups    %xmm1, 16(%rsp)                               #[spill]
        movsd     16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm1 #3612.24
        movdqu    .L_2il0floatpacket.36(%rip), %xmm6            #3712.25
        andps     %xmm1, %xmm7                                  #3613.20
        pand      %xmm2, %xmm6                                  #3712.25
        lea       (%rcx,%rcx,2), %esi                           #3704.230
        paddq     .L_2il0floatpacket.37(%rip), %xmm6            #3713.25
        cmpeqsd   %xmm1, %xmm7                                  #3614.25
        shll      $3, %esi                                      #3704.230
        movups    %xmm0, 32(%rsp)                               #[spill]
        movdqa    %xmm6, %xmm0                                  #3721.16
        movq      16(%rdi,%rsi), %xmm14                         #3706.383
        psrlq     $32, %xmm0                                    #3721.16
        movdqu    .L_2il0floatpacket.38(%rip), %xmm15           #3714.21
        movdqa    %xmm14, %xmm1                                 #3719.16
        movmskpd  %xmm7, %edx                                   #3616.48
        psrlq     $32, %xmm1                                    #3719.16
        pand      %xmm15, %xmm6                                 #3722.16
        movdqa    %xmm0, %xmm7                                  #3727.17
        movq      8(%rdi,%rsi), %xmm12                          #3705.384
        movdqa    %xmm6, %xmm10                                 #3732.17
        pmuludq   %xmm1, %xmm7                                  #3727.17
        movdqa    %xmm12, %xmm13                                #3717.16
        pmuludq   %xmm6, %xmm1                                  #3733.17
        pand      %xmm15, %xmm12                                #3718.16
        pand      %xmm15, %xmm14                                #3720.16
        pmuludq   %xmm12, %xmm10                                #3732.17
        psrlq     $32, %xmm1                                    #3735.17
        pmuludq   %xmm0, %xmm14                                 #3728.17
        pmuludq   %xmm0, %xmm12                                 #3726.17
        paddq     %xmm1, %xmm7                                  #3739.17
        psrlq     $32, %xmm13                                   #3717.16
        movdqa    %xmm6, %xmm11                                 #3731.17
        movdqa    %xmm15, %xmm9                                 #3743.17
        psrlq     $32, %xmm14                                   #3753.17
        pmuludq   %xmm13, %xmm11                                #3731.17
        pand      %xmm10, %xmm9                                 #3743.17
        pmuludq   %xmm0, %xmm13                                 #3725.17
        paddq     %xmm7, %xmm9                                  #3748.17
        movups    %xmm4, (%rsp)                                 #[spill]
        movdqa    %xmm15, %xmm4                                 #3716.16
        movq      (%rsi,%rdi), %xmm8                            #3704.383
        psrlq     $32, %xmm10                                   #3736.17
        pand      %xmm8, %xmm4                                  #3716.16
        movdqa    %xmm6, %xmm5                                  #3730.17
        paddq     %xmm9, %xmm14                                 #3754.17
        paddq     %xmm10, %xmm12                                #3740.17
        pmuludq   %xmm4, %xmm5                                  #3730.17
        pmuludq   %xmm4, %xmm0                                  #3724.17
        movdqa    %xmm15, %xmm7                                 #3744.17
        movdqa    %xmm14, %xmm1                                 #3755.17
        pand      %xmm11, %xmm7                                 #3744.17
        psrlq     $32, %xmm1                                    #3755.17
        paddq     %xmm12, %xmm7                                 #3749.17
        psrlq     $32, %xmm11                                   #3737.17
        psrlq     $32, %xmm8                                    #3715.16
        paddq     %xmm7, %xmm1                                  #3756.17
        paddq     %xmm11, %xmm13                                #3741.17
        pmuludq   %xmm8, %xmm6                                  #3729.17
        movdqa    %xmm15, %xmm10                                #3745.17
        movdqa    %xmm1, %xmm12                                 #3757.17
        pand      %xmm5, %xmm10                                 #3745.17
        psrlq     $32, %xmm12                                   #3757.17
        paddq     %xmm13, %xmm10                                #3750.17
        psrlq     $32, %xmm5                                    #3738.17
        pand      %xmm15, %xmm6                                 #3746.17
        paddq     %xmm10, %xmm12                                #3758.17
        paddq     %xmm5, %xmm0                                  #3742.17
        movdqa    %xmm12, %xmm9                                 #3759.17
        pand      %xmm15, %xmm12                                #3762.17
        paddq     %xmm0, %xmm6                                  #3751.17
        psrlq     $32, %xmm9                                    #3759.17
        psllq     $32, %xmm1                                    #3764.23
        paddq     %xmm6, %xmm9                                  #3760.17
        psllq     $32, %xmm9                                    #3763.23
        pand      %xmm15, %xmm14                                #3761.17
        paddq     %xmm12, %xmm9                                 #3765.23
        paddq     %xmm14, %xmm1                                 #3766.23
        movdqu    .L_2il0floatpacket.39(%rip), %xmm5            #3768.21
        movdqa    %xmm9, %xmm0                                  #3771.24
        movdqu    .L_2il0floatpacket.40(%rip), %xmm11           #3770.26
        pand      %xmm2, %xmm5                                  #3768.21
        movups    .L_2il0floatpacket.41(%rip), %xmm13           #3774.15
        psrlq     $12, %xmm0                                    #3771.24
        pxor      %xmm5, %xmm11                                 #3770.26
        movaps    %xmm13, %xmm8                                 #3775.19
        por       %xmm11, %xmm0                                 #3772.24
        andl      $1, %edx                                      #3616.102
        movdqu    .L_2il0floatpacket.46(%rip), %xmm7            #3793.24
        addsd     %xmm0, %xmm8                                  #3775.19
        movaps    %xmm8, %xmm14                                 #3776.9
        pand      %xmm9, %xmm7                                  #3793.24
        movdqu    .L_2il0floatpacket.44(%rip), %xmm6            #3784.24
        psllq     $40, %xmm7                                    #3794.24
        movdqu    .L_2il0floatpacket.43(%rip), %xmm4            #3782.26
        pand      %xmm1, %xmm6                                  #3784.24
        psrlq     $24, %xmm1                                    #3795.23
        pxor      %xmm5, %xmm4                                  #3782.26
        pxor      .L_2il0floatpacket.45(%rip), %xmm5            #3791.26
        por       %xmm1, %xmm7                                  #3796.24
        por       %xmm5, %xmm7                                  #3797.24
        psllq     $28, %xmm6                                    #3785.24
        por       %xmm4, %xmm6                                  #3786.24
        subsd     %xmm13, %xmm14                                #3776.9
        subsd     %xmm5, %xmm7                                  #3800.18
        subsd     %xmm4, %xmm6                                  #3789.17
        subsd     %xmm14, %xmm0                                 #3777.17
        movaps    %xmm0, %xmm9                                  #3801.11
        movups    .L_2il0floatpacket.50(%rip), %xmm1            #3809.16
        addsd     %xmm7, %xmm9                                  #3801.11
        andps     %xmm9, %xmm1                                  #3809.16
        subsd     %xmm9, %xmm0                                  #3802.17
        subsd     %xmm1, %xmm9                                  #3810.17
        addsd     %xmm0, %xmm7                                  #3803.18
        movups    .L_2il0floatpacket.49(%rip), %xmm4            #3807.17
        addsd     %xmm6, %xmm7                                  #3804.11
        movups    .L_2il0floatpacket.48(%rip), %xmm15           #3806.16
        movaps    %xmm4, %xmm10                                 #3813.12
        movups    .L_2il0floatpacket.47(%rip), %xmm11           #3814.12
        movaps    %xmm15, %xmm6                                 #3811.16
        mulsd     %xmm9, %xmm15                                 #3812.12
        mulsd     %xmm1, %xmm10                                 #3813.12
        mulsd     %xmm7, %xmm11                                 #3814.12
        mulsd     %xmm9, %xmm4                                  #3815.12
        addsd     %xmm10, %xmm15                                #3816.13
        mulsd     %xmm1, %xmm6                                  #3811.16
        addsd     %xmm4, %xmm11                                 #3817.13
        movaps    %xmm6, %xmm12                                 #3819.13
        addsd     %xmm11, %xmm15                                #3818.13
        movups    .L_2il0floatpacket.52(%rip), %xmm0            #3823.16
        addsd     %xmm15, %xmm12                                #3819.13
        movups    .L_2il0floatpacket.51(%rip), %xmm4            #3824.11
        movaps    %xmm0, %xmm7                                  #3825.17
        andps     %xmm2, %xmm4                                  #3824.11
        subsd     %xmm12, %xmm6                                 #3820.16
        cmpltsd   %xmm4, %xmm7                                  #3825.17
        addsd     %xmm15, %xmm6                                 #3821.13
        movaps    %xmm4, %xmm5                                  #3825.17
        cmplesd   %xmm0, %xmm4                                  #3826.20
        andps     .L_2il0floatpacket.42(%rip), %xmm8            #3780.19
        movsd     %xmm7, %xmm5                                  #3825.17
        movd      %xmm8, %r8d                                   #3832.132
        andps     %xmm2, %xmm4                                  #3827.24
        andps     %xmm5, %xmm12                                 #3828.25
        andps     %xmm6, %xmm5                                  #3830.9
        orps      %xmm12, %xmm4                                 #3829.9
        shll      $5, %r8d                                      #3832.235
        movaps    %xmm4, %xmm2                                  #3836.26
        movaps    %xmm4, %xmm7                                  #3844.19
        mulsd     8(%rax,%r8), %xmm2                            #3836.26
        mulsd     %xmm4, %xmm7                                  #3844.19
        movq      (%r8,%rax), %xmm10                            #3832.375
        movq      16(%rax,%r8), %xmm9                           #3834.375
        movdqa    %xmm10, %xmm0                                 #3835.23
        mulsd     %xmm4, %xmm0                                  #3835.23
        movdqa    %xmm9, %xmm6                                  #3837.26
        addsd     %xmm2, %xmm6                                  #3837.26
        addsd     8(%rax,%r8), %xmm10                           #3850.17
        movsd     17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3847.30
        movaps    %xmm0, %xmm12                                 #3838.25
        movsd     17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #3854.30
        movdqa    %xmm9, %xmm1                                  #3839.22
        mulsd     %xmm7, %xmm8                                  #3847.30
        addsd     %xmm6, %xmm12                                 #3838.25
        subsd     %xmm6, %xmm1                                  #3839.22
        mulsd     %xmm7, %xmm11                                 #3854.30
        addsd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #3847.18
        subsd     %xmm12, %xmm6                                 #3841.22
        addsd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #3854.18
        addsd     %xmm2, %xmm1                                  #3840.22
        mulsd     %xmm7, %xmm8                                  #3848.18
        addsd     %xmm0, %xmm6                                  #3842.22
        mulsd     %xmm7, %xmm11                                 #3856.30
        mulsd     %xmm4, %xmm8                                  #3849.18
        addsd     %xmm1, %xmm6                                  #3843.25
        mulsd     %xmm9, %xmm4                                  #3859.36
        addsd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #3856.18
        mulsd     %xmm10, %xmm8                                 #3851.33
        mulsd     %xmm7, %xmm11                                 #3857.18
        subsd     %xmm4, %xmm10                                 #3859.17
        addsd     %xmm6, %xmm8                                  #3851.21
        mulsd     %xmm9, %xmm11                                 #3858.33
        mulsd     %xmm10, %xmm5                                 #3861.32
        addsd     %xmm8, %xmm11                                 #3858.21
        addsd     24(%rax,%r8), %xmm5                           #3861.20
        movups    (%rsp), %xmm0                                 #3865.18[spill]
        addsd     %xmm5, %xmm11                                 #3862.21
        movups    16(%rsp), %xmm1                               #3865.18[spill]
        addsd     %xmm11, %xmm12                                #3863.24
        pxor      %xmm3, %xmm12                                 #3864.24
        blendvpd  %xmm0, %xmm12, %xmm1                          #3865.18
        movups    32(%rsp), %xmm0                               #3865.18[spill]
        jmp       ..B3.2        # Prob 100%                     #3865.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm1
	.cfi_endproc
# mark_end;
	.type	__svml_sin1_ha_e9,@function
	.size	__svml_sin1_ha_e9,.-__svml_sin1_ha_e9
..LN__svml_sin1_ha_e9.2:
	.data
# -- End  __svml_sin1_ha_e9
	.text
.L_2__routine_start___svml_sin4_ha_e9_3:
# -- Begin  __svml_sin4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin4_ha_e9
# --- __svml_sin4_ha_e9(__m256d)
__svml_sin4_ha_e9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3876.1
        .byte     15                                            #4235.18
        .byte     30                                            #4235.18
        .byte     250                                           #4235.18
	.cfi_startproc
..___tag_value___svml_sin4_ha_e9.77:
..L78:
                                                         #3876.1
        pushq     %rbp                                          #3876.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3876.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3876.1
        subq      $384, %rsp                                    #3876.1
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r8    #3943.613
        vmovupd   16384+__svml_dsin_ha_data_internal_ha(%rip), %ymm13 #3923.53
        xorl      %esi, %esi                                    #3922.1
        vmovupd   16640+__svml_dsin_ha_data_internal_ha(%rip), %ymm14 #3928.54
        vmovupd   16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm6 #3941.27
        vmovapd   %ymm0, %ymm11                                 #3876.1
        vandnpd   %ymm11, %ymm13, %ymm10                        #3924.17
        vandpd    %ymm13, %ymm11, %ymm13                        #3925.12
        vmulpd    16576+__svml_dsin_ha_data_internal_ha(%rip), %ymm13, %ymm12 #3929.28
        vaddpd    %ymm12, %ymm14, %ymm2                         #3929.13
        vsubpd    %ymm14, %ymm2, %ymm1                          #3930.13
        vmulpd    16768+__svml_dsin_ha_data_internal_ha(%rip), %ymm1, %ymm15 #3932.39
        vmulpd    16832+__svml_dsin_ha_data_internal_ha(%rip), %ymm1, %ymm8 #3934.19
        vmulpd    16896+__svml_dsin_ha_data_internal_ha(%rip), %ymm1, %ymm3 #3939.35
        vsubpd    %ymm15, %ymm13, %ymm5                         #3932.14
        vsubpd    %ymm8, %ymm5, %ymm9                           #3935.17
        vsubpd    %ymm9, %ymm5, %ymm0                           #3936.17
        vsubpd    %ymm8, %ymm0, %ymm4                           #3937.17
        vsubpd    %ymm3, %ymm4, %ymm8                           #3939.13
        vandps    %xmm6, %xmm2, %xmm12                          #3942.23
        vextractf128 $1, %ymm2, %xmm7                           #3940.104
        vmovd     %xmm12, %eax                                  #3943.128
        vandps    %xmm6, %xmm7, %xmm5                           #3942.92
        vmovd     %xmm5, %ecx                                   #3943.255
        shll      $5, %eax                                      #3943.424
        vpextrd   $2, %xmm12, %edx                              #3943.190
        shll      $5, %ecx                                      #3943.526
        vpextrd   $2, %xmm5, %edi                               #3943.316
        shll      $5, %edx                                      #3943.475
        shll      $5, %edi                                      #3943.577
        vmovq     8(%rax,%r8), %xmm4                            #3944.1114
        vmovq     8(%rcx,%r8), %xmm2                            #3944.1314
        vmovq     (%rax,%r8), %xmm14                            #3943.1112
        vmovhpd   8(%rdx,%r8), %xmm4, %xmm3                     #3944.1082
        vmovq     (%rcx,%r8), %xmm0                             #3943.1312
        vmovhpd   8(%rdi,%r8), %xmm2, %xmm6                     #3944.1282
        vmovhpd   (%rdx,%r8), %xmm14, %xmm15                    #3943.1080
        vmovhpd   (%rdi,%r8), %xmm0, %xmm1                      #3943.1280
        vmovq     16(%rax,%r8), %xmm12                          #3945.1112
        vmovq     16(%rcx,%r8), %xmm5                           #3945.1312
        vmovhpd   16(%rdx,%r8), %xmm12, %xmm14                  #3945.1080
        vmovhpd   16(%rdi,%r8), %xmm5, %xmm0                    #3945.1280
        vinsertf128 $1, %xmm6, %ymm3, %ymm6                     #3944.1036
        vinsertf128 $1, %xmm1, %ymm15, %ymm7                    #3943.1034
        vmulpd    %ymm9, %ymm6, %ymm1                           #3947.22
        vmulpd    %ymm9, %ymm7, %ymm12                          #3946.19
        vinsertf128 $1, %xmm0, %ymm14, %ymm4                    #3945.1034
        vaddpd    %ymm1, %ymm4, %ymm2                           #3948.22
        vsubpd    %ymm2, %ymm4, %ymm3                           #3950.18
        vaddpd    %ymm2, %ymm12, %ymm5                          #3949.21
        vaddpd    %ymm3, %ymm1, %ymm0                           #3951.18
        vsubpd    %ymm5, %ymm2, %ymm15                          #3952.18
        vmulpd    %ymm9, %ymm9, %ymm3                           #3955.15
        vaddpd    %ymm15, %ymm12, %ymm1                         #3953.18
        vmulpd    17024+__svml_dsin_ha_data_internal_ha(%rip), %ymm3, %ymm12 #3958.29
        vaddpd    %ymm1, %ymm0, %ymm2                           #3954.21
        vaddpd    %ymm6, %ymm7, %ymm1                           #3961.13
        vaddpd    16960+__svml_dsin_ha_data_internal_ha(%rip), %ymm12, %ymm14 #3958.14
        vmulpd    %ymm14, %ymm3, %ymm15                         #3959.14
        vmulpd    %ymm15, %ymm9, %ymm0                          #3960.14
        vmulpd    %ymm9, %ymm4, %ymm9                           #3970.35
        vmulpd    %ymm1, %ymm0, %ymm7                           #3962.32
        vmulpd    17216+__svml_dsin_ha_data_internal_ha(%rip), %ymm3, %ymm0 #3965.29
        vaddpd    %ymm7, %ymm2, %ymm12                          #3962.17
        vaddpd    17152+__svml_dsin_ha_data_internal_ha(%rip), %ymm0, %ymm2 #3965.14
        vmulpd    %ymm2, %ymm3, %ymm6                           #3967.29
        vaddpd    17088+__svml_dsin_ha_data_internal_ha(%rip), %ymm6, %ymm7 #3967.14
        vsubpd    %ymm9, %ymm1, %ymm6                           #3970.13
        vmovq     24(%rax,%r8), %xmm1                           #3971.1112
        vmovhpd   24(%rdx,%r8), %xmm1, %xmm2                    #3971.1080
        vmulpd    %ymm7, %ymm3, %ymm3                           #3968.14
        vmulpd    %ymm6, %ymm8, %ymm8                           #3972.31
        vmulpd    %ymm3, %ymm4, %ymm14                          #3969.32
        vmovq     24(%rcx,%r8), %xmm3                           #3971.1312
        vmovhpd   24(%rdi,%r8), %xmm3, %xmm4                    #3971.1280
        vaddpd    %ymm14, %ymm12, %ymm0                         #3969.17
        vcmpnle_uqpd 16448+__svml_dsin_ha_data_internal_ha(%rip), %ymm13, %ymm12 #3976.26
        vinsertf128 $1, %xmm4, %ymm2, %ymm7                     #3971.1034
        vaddpd    %ymm8, %ymm7, %ymm9                           #3972.16
        vaddpd    %ymm9, %ymm0, %ymm0                           #3973.17
        vaddpd    %ymm0, %ymm5, %ymm5                           #3974.14
        vxorpd    %ymm10, %ymm5, %ymm0                          #3975.14
        vextractf128 $1, %ymm12, %xmm14                         #3977.151
        vshufps   $221, %xmm14, %xmm12, %xmm15                  #3978.95
        vmovmskps %xmm15, %r9d                                  #3978.78
        testl     %r9d, %r9d                                    #3979.66
        jne       ..B4.12       # Prob 5%                       #3979.66
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm10 ymm11 ymm12 ymm13
..B4.2:                         # Preds ..B4.12 ..B4.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #4238.52
        jne       ..B4.4        # Prob 5%                       #4238.52
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm11
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #4241.12
        popq      %rbp                                          #4241.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4241.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %ymm11, 64(%rsp)                              #4238.200
        vmovupd   %ymm0, 128(%rsp)                              #4238.276
        je        ..B4.3        # Prob 95%                      #4238.380
                                # LOE rbx r12 r13 r14 r15 esi ymm0
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #4238.460
                                # LOE rbx r12 r13 r14 r15 eax esi
..B4.15:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x88, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x80, 0xfe, 0xff, 0xff, 0x22
        movl      %esi, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4238.523
        jc        ..B4.11       # Prob 5%                       #4238.523
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4238.476
        cmpl      $4, %r12d                                     #4238.471
        jl        ..B4.8        # Prob 82%                      #4238.471
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovupd   128(%rsp), %ymm0                              #4238.682
        jmp       ..B4.3        # Prob 100%                     #4238.682
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x88, 0xfe, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x80, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 ymm0
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #4238.552
        lea       128(%rsp,%r12,8), %rsi                        #4238.552
..___tag_value___svml_sin4_ha_e9.95:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #4238.552
..___tag_value___svml_sin4_ha_e9.96:
        jmp       ..B4.9        # Prob 100%                     #4238.552
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovupd   16512+__svml_dsin_ha_data_internal_ha(%rip), %ymm9 #3982.58
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rcx #4074.621
        vmovupd   %ymm13, (%rsp)                                #[spill]
        vmovupd   %ymm11, 128(%rsp)                             #[spill]
        vmovdqu   .L_2il0floatpacket.35(%rip), %xmm7            #4071.22
        vmovupd   %ymm0, 96(%rsp)                               #[spill]
        vmovupd   %ymm10, 32(%rsp)                              #[spill]
        vmovupd   %ymm12, 64(%rsp)                              #[spill]
        vandpd    %ymm13, %ymm9, %ymm11                         #3983.20
        vcmpeqpd  %ymm9, %ymm11, %ymm2                          #3984.25
        vpand     %xmm13, %xmm7, %xmm8                          #4072.22
        vextractf128 $1, %ymm13, %xmm14                         #4070.111
        vpsrlq    $52, %xmm8, %xmm3                             #4073.22
        vmovd     %xmm3, %edi                                   #4074.124
        vmovdqu   .L_2il0floatpacket.36(%rip), %xmm8            #4080.25
        vpand     %xmm14, %xmm7, %xmm4                          #4072.97
        vpsrlq    $52, %xmm4, %xmm0                             #4073.85
        vmovd     %xmm0, %r9d                                   #4074.257
        lea       (%rdi,%rdi,2), %edx                           #4074.432
        vmovups   %xmm13, 160(%rsp)                             #4070.37[spill]
        vpand     %xmm13, %xmm8, %xmm13                         #4082.25
        vmovups   %xmm14, 176(%rsp)                             #4070.111[spill]
        vpand     %xmm14, %xmm8, %xmm14                         #4082.106
        vpextrd   $2, %xmm0, %r11d                              #4074.321
        vpextrd   $2, %xmm3, %r8d                               #4074.189
        lea       (%r9,%r9,2), %r10d                            #4074.534
        shll      $3, %r10d                                     #4074.534
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r9    #4202.617
        vmovq     (%r10,%rcx), %xmm11                           #4074.1369
        vmovdqu   .L_2il0floatpacket.37(%rip), %xmm7            #4081.24
        lea       (%r11,%r11,2), %edi                           #4074.585
        shll      $3, %edi                                      #4074.585
        lea       (%r8,%r8,2), %eax                             #4074.483
        shll      $3, %edx                                      #4074.432
        vmovq     8(%r10,%rcx), %xmm6                           #4075.1370
        shll      $3, %eax                                      #4074.483
        vmovq     (%rdx,%rcx), %xmm5                            #4074.1169
        vmovhpd   8(%rdi,%rcx), %xmm6, %xmm9                    #4075.1338
        vmovq     8(%rdx,%rcx), %xmm1                           #4075.1170
        vmovq     16(%rdx,%rcx), %xmm12                         #4076.1169
        vmovhpd   (%rax,%rcx), %xmm5, %xmm4                     #4074.1137
        vpsrlq    $32, %xmm9, %xmm5                             #4087.70
        vmovdqu   %xmm5, 272(%rsp)                              #4087.70[spill]
        vmovupd   %xmm4, 192(%rsp)                              #4074.1137[spill]
        vextractf128 $1, %ymm2, %xmm10                          #3985.132
        vshufps   $221, %xmm10, %xmm2, %xmm15                   #3986.65
        vmovhpd   (%rdi,%rcx), %xmm11, %xmm10                   #4074.1337
        vpaddq    %xmm7, %xmm13, %xmm11                         #4083.25
        vpaddq    %xmm7, %xmm14, %xmm7                          #4083.111
        vmovmskps %xmm15, %esi                                  #3986.48
        vmovdqu   .L_2il0floatpacket.38(%rip), %xmm13           #4084.21
        vpand     %xmm13, %xmm10, %xmm14                        #4086.83
        vpand     %xmm13, %xmm9, %xmm0                          #4088.84
        vpsrlq    $32, %xmm11, %xmm9                            #4091.16
        vpand     %xmm13, %xmm11, %xmm8                         #4092.16
        vpand     %xmm13, %xmm7, %xmm11                         #4092.90
        vmovhpd   8(%rax,%rcx), %xmm1, %xmm2                    #4075.1138
        vmovhpd   16(%rax,%rcx), %xmm12, %xmm1                  #4076.1137
        vpsrlq    $32, %xmm2, %xmm12                            #4087.16
        vmovdqu   %xmm14, 240(%rsp)                             #4086.83[spill]
        vpand     %xmm13, %xmm2, %xmm6                          #4088.16
        vpmuludq  %xmm14, %xmm11, %xmm14                        #4100.78
        vpsrlq    $32, %xmm1, %xmm2                             #4089.16
        vmovupd   %xmm10, 208(%rsp)                             #4074.1337[spill]
        vpsrlq    $32, %xmm7, %xmm10                            #4091.76
        vmovdqu   %xmm0, 288(%rsp)                              #4088.84[spill]
        vpmuludq  %xmm0, %xmm11, %xmm7                          #4102.78
        vpand     %xmm13, %xmm1, %xmm0                          #4090.16
        vpmuludq  %xmm0, %xmm9, %xmm1                           #4098.17
        vpmuludq  %xmm2, %xmm9, %xmm0                           #4097.17
        vpmuludq  %xmm2, %xmm8, %xmm2                           #4103.17
        vmovq     16(%r10,%rcx), %xmm15                         #4076.1369
        vpsrlq    $32, %xmm2, %xmm2                             #4105.17
        vmovhpd   16(%rdi,%rcx), %xmm15, %xmm3                  #4076.1337
        vpand     %xmm13, %xmm4, %xmm15                         #4086.16
        vmovdqu   %xmm14, 320(%rsp)                             #4100.78[spill]
        vpsrlq    $32, %xmm3, %xmm4                             #4089.69
        vpmuludq  %xmm5, %xmm11, %xmm14                         #4101.78
        vpand     %xmm13, %xmm3, %xmm3                          #4090.83
        vpmuludq  %xmm6, %xmm8, %xmm5                           #4102.17
        vpmuludq  %xmm6, %xmm9, %xmm6                           #4096.17
        vpaddq    %xmm2, %xmm0, %xmm0                           #4109.17
        vmovdqu   %xmm15, 224(%rsp)                             #4086.16[spill]
        vpmuludq  %xmm15, %xmm8, %xmm15                         #4100.17
        vmovdqu   %xmm15, 304(%rsp)                             #4100.17[spill]
        vpsrlq    $32, %xmm1, %xmm15                            #4123.17
        vpand     %xmm13, %xmm5, %xmm1                          #4113.17
        vpsrlq    $32, %xmm5, %xmm5                             #4106.17
        vpaddq    %xmm0, %xmm1, %xmm1                           #4118.17
        vpaddq    %xmm5, %xmm6, %xmm5                           #4110.17
        vpaddq    %xmm1, %xmm15, %xmm2                          #4124.17
        vpmuludq  %xmm4, %xmm10, %xmm0                          #4097.78
        vpmuludq  %xmm4, %xmm11, %xmm4                          #4103.78
        vpmuludq  %xmm3, %xmm10, %xmm15                         #4098.78
        vpsrlq    $32, %xmm4, %xmm4                             #4105.70
        vpand     %xmm13, %xmm7, %xmm1                          #4113.84
        vpaddq    %xmm4, %xmm0, %xmm3                           #4109.80
        vpaddq    %xmm3, %xmm1, %xmm0                           #4118.80
        vmovdqu   %xmm12, 256(%rsp)                             #4087.16[spill]
        vpsrlq    $32, %xmm15, %xmm15                           #4123.70
        vpmuludq  %xmm12, %xmm8, %xmm12                         #4101.17
        vpsrlq    $32, %xmm7, %xmm7                             #4106.70
        vpaddq    %xmm0, %xmm15, %xmm4                          #4124.80
        vpmuludq  288(%rsp), %xmm10, %xmm0                      #4096.78[spill]
        vpand     %xmm13, %xmm12, %xmm15                        #4114.17
        vpand     %xmm13, %xmm14, %xmm1                         #4114.84
        vpaddq    %xmm5, %xmm15, %xmm15                         #4119.17
        vpaddq    %xmm7, %xmm0, %xmm6                           #4110.80
        vpmuludq  256(%rsp), %xmm9, %xmm5                       #4095.17[spill]
        vpmuludq  224(%rsp), %xmm9, %xmm9                       #4094.17[spill]
        vpaddq    %xmm6, %xmm1, %xmm7                           #4119.80
        vpmuludq  272(%rsp), %xmm10, %xmm1                      #4095.78[spill]
        vpmuludq  240(%rsp), %xmm10, %xmm10                     #4094.78[spill]
        vpsrlq    $32, %xmm2, %xmm3                             #4125.17
        vpsrlq    $32, %xmm12, %xmm12                           #4107.17
        vpaddq    %xmm15, %xmm3, %xmm15                         #4126.17
        vpaddq    %xmm12, %xmm5, %xmm5                          #4111.17
        vpsrlq    $32, %xmm4, %xmm3                             #4125.70
        vpsrlq    $32, %xmm15, %xmm6                            #4127.17
        vpaddq    %xmm7, %xmm3, %xmm7                           #4126.80
        vmovdqu   304(%rsp), %xmm3                              #4115.17[spill]
        vpsrlq    $32, %xmm14, %xmm14                           #4107.70
        vpand     %xmm13, %xmm3, %xmm0                          #4115.17
        vpsrlq    $32, %xmm3, %xmm3                             #4108.17
        vpaddq    %xmm5, %xmm0, %xmm0                           #4120.17
        vpaddq    %xmm14, %xmm1, %xmm1                          #4111.80
        vpaddq    %xmm0, %xmm6, %xmm6                           #4128.17
        vpaddq    %xmm3, %xmm9, %xmm9                           #4112.17
        vmovdqu   320(%rsp), %xmm0                              #4115.84[spill]
        vpsrlq    $32, %xmm7, %xmm5                             #4127.70
        vpand     %xmm13, %xmm0, %xmm12                         #4115.84
        vpsllq    $32, %xmm15, %xmm15                           #4134.23
        vpaddq    %xmm1, %xmm12, %xmm12                         #4120.80
        vmovupd   192(%rsp), %xmm14                             #4085.16[spill]
        vpand     %xmm13, %xmm2, %xmm2                          #4131.17
        vpaddq    %xmm12, %xmm5, %xmm1                          #4128.80
        vpsrlq    $32, %xmm14, %xmm12                           #4085.16
        vpsrlq    $32, %xmm6, %xmm5                             #4129.17
        vpmuludq  %xmm12, %xmm8, %xmm8                          #4099.17
        vpand     %xmm13, %xmm6, %xmm6                          #4132.17
        vpand     %xmm13, %xmm8, %xmm8                          #4116.17
        vpsllq    $32, %xmm7, %xmm7                             #4134.82
        vpaddq    %xmm9, %xmm8, %xmm8                           #4121.17
        vmovupd   208(%rsp), %xmm12                             #4085.69[spill]
        vpsrlq    $32, %xmm1, %xmm9                             #4129.70
        vpaddq    %xmm8, %xmm5, %xmm5                           #4130.17
        vpsrlq    $32, %xmm12, %xmm8                            #4085.69
        vpsllq    $32, %xmm5, %xmm14                            #4133.23
        vpmuludq  %xmm8, %xmm11, %xmm11                         #4099.78
        vpsrlq    $32, %xmm0, %xmm8                             #4108.70
        vpaddq    %xmm8, %xmm10, %xmm10                         #4112.80
        vpaddq    %xmm6, %xmm14, %xmm6                          #4135.23
        vpand     %xmm13, %xmm11, %xmm3                         #4116.84
        vpand     %xmm13, %xmm1, %xmm11                         #4132.84
        vpaddq    %xmm10, %xmm3, %xmm5                          #4121.80
        vmovupd   .L_2il0floatpacket.59(%rip), %ymm1            #4144.15
        vpaddq    %xmm5, %xmm9, %xmm9                           #4130.80
        vpsllq    $32, %xmm9, %xmm0                             #4133.82
        vpand     %xmm13, %xmm4, %xmm13                         #4131.84
        vpaddq    %xmm2, %xmm15, %xmm9                          #4136.23
        vpaddq    %xmm11, %xmm0, %xmm8                          #4135.98
        vpaddq    %xmm13, %xmm7, %xmm7                          #4136.98
        vmovdqu   .L_2il0floatpacket.39(%rip), %xmm2            #4137.21
        vpsrlq    $12, %xmm6, %xmm10                            #4141.24
        vpand     160(%rsp), %xmm2, %xmm15                      #4138.21[spill]
        vpsrlq    $12, %xmm8, %xmm5                             #4141.90
        vmovdqu   .L_2il0floatpacket.40(%rip), %xmm3            #4139.26
        vpand     176(%rsp), %xmm2, %xmm2                       #4138.94[spill]
        vpxor     %xmm3, %xmm15, %xmm13                         #4140.26
        vpxor     %xmm3, %xmm2, %xmm0                           #4140.111
        vpor      %xmm13, %xmm10, %xmm4                         #4142.24
        vpor      %xmm0, %xmm5, %xmm11                          #4142.109
        vmovups   .L_2il0floatpacket.42(%rip), %xmm3            #4149.19
        vmovdqu   .L_2il0floatpacket.43(%rip), %xmm0            #4151.26
        vinsertf128 $1, %xmm11, %ymm4, %ymm14                   #4143.17
        vaddpd    %ymm1, %ymm14, %ymm10                         #4145.19
        vsubpd    %ymm1, %ymm10, %ymm12                         #4146.9
        vsubpd    %ymm12, %ymm14, %ymm5                         #4147.17
        vpxor     %xmm0, %xmm15, %xmm12                         #4152.26
        vpxor     %xmm0, %xmm2, %xmm14                          #4152.111
        vandps    %xmm3, %xmm10, %xmm13                         #4150.19
        vextractf128 $1, %ymm10, %xmm4                          #4148.118
        vmovd     %xmm13, %eax                                  #4202.132
        vandps    %xmm3, %xmm4, %xmm10                          #4150.88
        vmovdqu   .L_2il0floatpacket.44(%rip), %xmm3            #4153.24
        vpand     %xmm9, %xmm3, %xmm11                          #4154.24
        vpand     %xmm7, %xmm3, %xmm0                           #4154.107
        vpsllq    $28, %xmm11, %xmm1                            #4155.24
        vpsllq    $28, %xmm0, %xmm11                            #4155.91
        vpor      %xmm12, %xmm1, %xmm4                          #4156.24
        vpor      %xmm14, %xmm11, %xmm1                         #4156.109
        vpsrlq    $24, %xmm9, %xmm9                             #4165.23
        vpsrlq    $24, %xmm7, %xmm7                             #4165.88
        vmovd     %xmm10, %ecx                                  #4202.259
        shll      $5, %eax                                      #4202.428
        vpextrd   $2, %xmm13, %edx                              #4202.194
        shll      $5, %ecx                                      #4202.530
        vpextrd   $2, %xmm10, %r8d                              #4202.320
        shll      $5, %edx                                      #4202.479
        shll      $5, %r8d                                      #4202.581
        vinsertf128 $1, %xmm1, %ymm4, %ymm4                     #4157.17
        vinsertf128 $1, %xmm14, %ymm12, %ymm14                  #4158.15
        vsubpd    %ymm14, %ymm4, %ymm3                          #4159.17
        vmovdqu   .L_2il0floatpacket.45(%rip), %xmm4            #4160.26
        vmovdqu   .L_2il0floatpacket.46(%rip), %xmm1            #4162.24
        vpxor     %xmm4, %xmm2, %xmm0                           #4161.111
        vpand     %xmm6, %xmm1, %xmm2                           #4163.24
        vpand     %xmm8, %xmm1, %xmm8                           #4163.107
        vpxor     %xmm4, %xmm15, %xmm11                         #4161.26
        vpsllq    $40, %xmm2, %xmm15                            #4164.24
        vpsllq    $40, %xmm8, %xmm2                             #4164.91
        vpor      %xmm9, %xmm15, %xmm6                          #4166.24
        vpor      %xmm7, %xmm2, %xmm2                           #4166.106
        vpor      %xmm11, %xmm6, %xmm9                          #4167.24
        vpor      %xmm0, %xmm2, %xmm15                          #4167.109
        vinsertf128 $1, %xmm15, %ymm9, %ymm8                    #4168.18
        vinsertf128 $1, %xmm0, %ymm11, %ymm4                    #4169.15
        vsubpd    %ymm4, %ymm8, %ymm0                           #4170.18
        vmovupd   .L_2il0floatpacket.70(%rip), %ymm9            #4177.17
        vmovupd   .L_2il0floatpacket.68(%rip), %ymm11           #4193.16
        vaddpd    %ymm0, %ymm5, %ymm6                           #4171.11
        vsubpd    %ymm6, %ymm5, %ymm5                           #4172.17
        vaddpd    %ymm5, %ymm0, %ymm1                           #4173.18
        vaddpd    %ymm1, %ymm3, %ymm14                          #4174.11
        vmovupd   .L_2il0floatpacket.69(%rip), %ymm3            #4176.16
        vmulpd    .L_2il0floatpacket.65(%rip), %ymm14, %ymm7    #4184.12
        vandpd    .L_2il0floatpacket.71(%rip), %ymm6, %ymm12    #4179.16
        vsubpd    %ymm12, %ymm6, %ymm2                          #4180.17
        vmulpd    %ymm12, %ymm9, %ymm8                          #4183.12
        vmulpd    %ymm12, %ymm3, %ymm0                          #4181.16
        vmulpd    %ymm2, %ymm3, %ymm15                          #4182.12
        vmulpd    %ymm2, %ymm9, %ymm4                           #4185.12
        vmovupd   (%rsp), %ymm9                                 #4194.11[spill]
        vaddpd    %ymm8, %ymm15, %ymm3                          #4186.13
        vaddpd    %ymm4, %ymm7, %ymm12                          #4187.13
        vaddpd    %ymm12, %ymm3, %ymm1                          #4188.13
        vmovq     16(%rcx,%r9), %xmm3                           #4204.1316
        vaddpd    %ymm1, %ymm0, %ymm2                           #4189.13
        vandpd    .L_2il0floatpacket.67(%rip), %ymm9, %ymm6     #4194.11
        vsubpd    %ymm2, %ymm0, %ymm5                           #4190.16
        vcmple_oqpd %ymm11, %ymm6, %ymm14                       #4196.20
        vcmpgt_oqpd %ymm11, %ymm6, %ymm7                        #4195.17
        vaddpd    %ymm5, %ymm1, %ymm4                           #4191.13
        vmovq     (%rax,%r9), %xmm0                             #4202.1116
        vmovhpd   (%rdx,%r9), %xmm0, %xmm13                     #4202.1084
        vandpd    %ymm9, %ymm14, %ymm15                         #4197.24
        vmovq     (%rcx,%r9), %xmm9                             #4202.1316
        vandpd    %ymm2, %ymm7, %ymm8                           #4198.25
        vmovhpd   (%r8,%r9), %xmm9, %xmm10                      #4202.1284
        vmovq     8(%rax,%r9), %xmm9                            #4203.1118
        vorpd     %ymm8, %ymm15, %ymm15                         #4199.9
        vmovq     8(%rcx,%r9), %xmm8                            #4203.1318
        vandpd    %ymm4, %ymm7, %ymm2                           #4200.9
        vmovq     16(%rax,%r9), %xmm0                           #4204.1116
        vmovhpd   16(%rdx,%r9), %xmm0, %xmm1                    #4204.1084
        vmovhpd   16(%r8,%r9), %xmm3, %xmm5                     #4204.1284
        vinsertf128 $1, %xmm10, %ymm13, %ymm7                   #4202.1038
        vmovhpd   8(%rdx,%r9), %xmm9, %xmm10                    #4203.1086
        vmovhpd   8(%r8,%r9), %xmm8, %xmm13                     #4203.1286
        vmulpd    %ymm7, %ymm15, %ymm14                         #4205.23
        vinsertf128 $1, %xmm13, %ymm10, %ymm4                   #4203.1040
        vmulpd    %ymm4, %ymm15, %ymm6                          #4206.26
        vinsertf128 $1, %xmm5, %ymm1, %ymm8                     #4204.1038
        vaddpd    %ymm6, %ymm8, %ymm12                          #4207.26
        vaddpd    %ymm12, %ymm14, %ymm9                         #4208.25
        vsubpd    %ymm12, %ymm8, %ymm11                         #4209.22
        vsubpd    %ymm9, %ymm12, %ymm10                         #4211.22
        vaddpd    %ymm11, %ymm6, %ymm13                         #4210.22
        vaddpd    %ymm10, %ymm14, %ymm0                         #4212.22
        vaddpd    %ymm4, %ymm7, %ymm10                          #4220.17
        vaddpd    %ymm0, %ymm13, %ymm11                         #4213.25
        vmulpd    %ymm15, %ymm15, %ymm13                        #4214.19
        vmulpd    17024+__svml_dsin_ha_data_internal_ha(%rip), %ymm13, %ymm1 #4217.33
        vmulpd    17216+__svml_dsin_ha_data_internal_ha(%rip), %ymm13, %ymm7 #4224.33
        vaddpd    16960+__svml_dsin_ha_data_internal_ha(%rip), %ymm1, %ymm3 #4217.18
        vaddpd    17152+__svml_dsin_ha_data_internal_ha(%rip), %ymm7, %ymm12 #4224.18
        vmulpd    %ymm3, %ymm13, %ymm5                          #4218.18
        vmulpd    %ymm12, %ymm13, %ymm14                        #4226.33
        vmulpd    %ymm5, %ymm15, %ymm6                          #4219.18
        vmulpd    %ymm8, %ymm15, %ymm15                         #4229.39
        vmovupd   64(%rsp), %ymm5                               #4235.18[spill]
        vaddpd    17088+__svml_dsin_ha_data_internal_ha(%rip), %ymm14, %ymm7 #4226.18
        vmulpd    %ymm10, %ymm6, %ymm4                          #4221.36
        vmulpd    %ymm7, %ymm13, %ymm13                         #4227.18
        vaddpd    %ymm4, %ymm11, %ymm0                          #4221.21
        vmovupd   128(%rsp), %ymm11                             #4235.18[spill]
        vmulpd    %ymm13, %ymm8, %ymm1                          #4228.36
        vmovq     24(%rax,%r9), %xmm8                           #4230.1116
        vaddpd    %ymm1, %ymm0, %ymm4                           #4228.21
        vsubpd    %ymm15, %ymm10, %ymm1                         #4229.17
        vmovq     24(%rcx,%r9), %xmm15                          #4230.1316
        vmovhpd   24(%rdx,%r9), %xmm8, %xmm10                   #4230.1084
        vmulpd    %ymm1, %ymm2, %ymm2                           #4231.35
        vmovhpd   24(%r8,%r9), %xmm15, %xmm0                    #4230.1284
        vinsertf128 $1, %xmm0, %ymm10, %ymm3                    #4230.1038
        vaddpd    %ymm2, %ymm3, %ymm0                           #4231.20
        vaddpd    %ymm0, %ymm4, %ymm1                           #4232.21
        vmovupd   96(%rsp), %ymm0                               #4235.18[spill]
        vaddpd    %ymm1, %ymm9, %ymm2                           #4233.24
        vxorpd    32(%rsp), %ymm2, %ymm3                        #4234.24[spill]
        vblendvpd %ymm5, %ymm3, %ymm0, %ymm0                    #4235.18
        jmp       ..B4.2        # Prob 100%                     #4235.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi ymm0 ymm11
	.cfi_endproc
# mark_end;
	.type	__svml_sin4_ha_e9,@function
	.size	__svml_sin4_ha_e9,.-__svml_sin4_ha_e9
..LN__svml_sin4_ha_e9.3:
	.data
# -- End  __svml_sin4_ha_e9
	.text
.L_2__routine_start___svml_sin1_ha_ex_4:
# -- Begin  __svml_sin1_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin1_ha_ex
# --- __svml_sin1_ha_ex(__m128d)
__svml_sin1_ha_ex:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4246.1
        .byte     15                                            #4605.18
        .byte     30                                            #4605.18
        .byte     250                                           #4605.18
	.cfi_startproc
..___tag_value___svml_sin1_ha_ex.132:
..L133:
                                                        #4246.1
        pushq     %rbp                                          #4246.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4246.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4246.1
        subq      $128, %rsp                                    #4246.1
        movaps    %xmm0, %xmm5                                  #4246.1
        movsd     16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm1 #4293.19
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rax   #4313.365
        movaps    %xmm1, %xmm2                                  #4294.17
        andps     %xmm5, %xmm1                                  #4295.12
        movaps    %xmm1, %xmm11                                 #4299.25
        movaps    %xmm1, %xmm4                                  #4302.14
        mulsd     16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #4299.25
        andnps    %xmm5, %xmm2                                  #4294.17
        movsd     16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm6 #4298.20
        xorl      %edx, %edx                                    #4292.1
        movq      16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #4311.21
        addsd     %xmm6, %xmm11                                 #4299.13
        movaps    %xmm11, %xmm9                                 #4300.13
        pand      %xmm10, %xmm11                                #4312.17
        movd      %xmm11, %ecx                                  #4313.128
        subsd     %xmm6, %xmm9                                  #4300.13
        movaps    %xmm9, %xmm7                                  #4302.36
        movaps    %xmm9, %xmm8                                  #4304.19
        mulsd     16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm7 #4302.36
        mulsd     16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #4304.19
        mulsd     16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #4309.32
        subsd     %xmm7, %xmm4                                  #4302.14
        movaps    %xmm4, %xmm3                                  #4305.17
        shll      $5, %ecx                                      #4313.225
        subsd     %xmm8, %xmm3                                  #4305.17
        movq      8(%rax,%rcx), %xmm15                          #4314.367
        movaps    %xmm3, %xmm12                                 #4317.22
        mulsd     %xmm15, %xmm12                                #4317.22
        subsd     %xmm3, %xmm4                                  #4306.17
        movq      16(%rax,%rcx), %xmm7                          #4315.365
        subsd     %xmm8, %xmm4                                  #4307.17
        movq      (%rcx,%rax), %xmm8                            #4313.365
        movdqa    %xmm7, %xmm6                                  #4318.22
        movdqa    %xmm8, %xmm13                                 #4316.19
        movdqa    %xmm7, %xmm14                                 #4320.18
        mulsd     %xmm3, %xmm13                                 #4316.19
        addsd     %xmm12, %xmm6                                 #4318.22
        subsd     %xmm9, %xmm4                                  #4309.13
        addsd     %xmm15, %xmm8                                 #4331.13
        subsd     %xmm6, %xmm14                                 #4320.18
        movaps    %xmm13, %xmm0                                 #4319.21
        addsd     %xmm12, %xmm14                                #4321.18
        addsd     %xmm6, %xmm0                                  #4319.21
        movaps    %xmm3, %xmm12                                 #4325.15
        subsd     %xmm0, %xmm6                                  #4322.18
        mulsd     %xmm3, %xmm12                                 #4325.15
        addsd     %xmm13, %xmm6                                 #4323.18
        movsd     17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #4328.26
        addsd     %xmm14, %xmm6                                 #4324.21
        mulsd     %xmm12, %xmm13                                #4328.26
        movsd     17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #4335.26
        mulsd     %xmm12, %xmm9                                 #4335.26
        addsd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #4328.14
        mulsd     %xmm12, %xmm13                                #4329.14
        addsd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #4335.14
        mulsd     %xmm12, %xmm9                                 #4337.26
        mulsd     %xmm3, %xmm13                                 #4330.14
        mulsd     %xmm7, %xmm3                                  #4340.32
        addsd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #4337.14
        mulsd     %xmm8, %xmm13                                 #4332.29
        subsd     %xmm3, %xmm8                                  #4340.13
        mulsd     %xmm12, %xmm9                                 #4338.14
        addsd     %xmm6, %xmm13                                 #4332.17
        mulsd     %xmm8, %xmm4                                  #4342.28
        mulsd     %xmm7, %xmm9                                  #4339.29
        addsd     24(%rax,%rcx), %xmm4                          #4342.16
        addsd     %xmm13, %xmm9                                 #4339.17
        movaps    %xmm1, %xmm3                                  #4346.26
        addsd     %xmm4, %xmm9                                  #4343.17
        cmpnlesd  16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm3 #4346.26
        addsd     %xmm9, %xmm0                                  #4344.14
        movmskpd  %xmm3, %esi                                   #4348.78
        pxor      %xmm2, %xmm0                                  #4345.14
        testl     $1, %esi                                      #4349.55
        jne       ..B5.8        # Prob 5%                       #4349.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm3 xmm5
..B5.2:                         # Preds ..B5.8 ..B5.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #4608.52
        jne       ..B5.4        # Prob 5%                       #4608.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm5
..B5.3:                         # Preds ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #4611.12
        popq      %rbp                                          #4611.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4611.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        movsd     %xmm5, (%rsp)                                 #4608.160
        movsd     %xmm0, 64(%rsp)                               #4608.233
        jne       ..B5.6        # Prob 5%                       #4608.374
                                # LOE rbx r12 r13 r14 r15 edx
..B5.5:                         # Preds ..B5.7 ..B5.6 ..B5.4
                                # Execution count [5.00e-02]: Infreq
        movsd     64(%rsp), %xmm0                               #4608.625
        movq      %rbp, %rsp                                    #4608.625
        popq      %rbp                                          #4608.625
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4608.625
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.6:                         # Preds ..B5.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B5.5        # Prob 95%                      #4608.517
                                # LOE rbx r12 r13 r14 r15
..B5.7:                         # Preds ..B5.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #4608.546
        lea       64(%rsp), %rsi                                #4608.546
..___tag_value___svml_sin1_ha_ex.145:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #4608.546
..___tag_value___svml_sin1_ha_ex.146:
        jmp       ..B5.5        # Prob 100%                     #4608.546
                                # LOE rbx r12 r13 r14 r15
..B5.8:                         # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.35(%rip), %xmm8            #4442.16
        movaps    %xmm1, %xmm6                                  #4353.20
        pand      %xmm1, %xmm8                                  #4442.16
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rdi #4444.377
        psrlq     $52, %xmm8                                    #4443.16
        movd      %xmm8, %ecx                                   #4444.124
        movups    %xmm0, 16(%rsp)                               #[spill]
        movsd     16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm0 #4352.24
        movups    %xmm5, 32(%rsp)                               #[spill]
        andps     %xmm0, %xmm6                                  #4353.20
        movdqu    .L_2il0floatpacket.36(%rip), %xmm5            #4452.19
        lea       (%rcx,%rcx,2), %esi                           #4444.224
        pand      %xmm1, %xmm5                                  #4452.19
        cmpeqsd   %xmm0, %xmm6                                  #4354.25
        paddq     .L_2il0floatpacket.37(%rip), %xmm5            #4453.19
        movmskpd  %xmm6, %edx                                   #4356.48
        movdqa    %xmm5, %xmm4                                  #4461.10
        movdqu    .L_2il0floatpacket.38(%rip), %xmm14           #4454.15
        psrlq     $32, %xmm4                                    #4461.10
        shll      $3, %esi                                      #4444.224
        pand      %xmm14, %xmm5                                 #4462.10
        movq      16(%rdi,%rsi), %xmm13                         #4446.377
        movdqa    %xmm4, %xmm6                                  #4467.11
        movdqa    %xmm13, %xmm0                                 #4459.10
        movdqa    %xmm5, %xmm9                                  #4472.11
        psrlq     $32, %xmm0                                    #4459.10
        pand      %xmm14, %xmm13                                #4460.10
        movq      8(%rdi,%rsi), %xmm11                          #4445.378
        movdqa    %xmm5, %xmm10                                 #4471.11
        pmuludq   %xmm0, %xmm6                                  #4467.11
        movdqa    %xmm11, %xmm12                                #4457.10
        pmuludq   %xmm5, %xmm0                                  #4473.11
        pmuludq   %xmm4, %xmm13                                 #4468.11
        pand      %xmm14, %xmm11                                #4458.10
        psrlq     $32, %xmm0                                    #4475.11
        pmuludq   %xmm11, %xmm9                                 #4472.11
        psrlq     $32, %xmm12                                   #4457.10
        pmuludq   %xmm4, %xmm11                                 #4466.11
        pmuludq   %xmm12, %xmm10                                #4471.11
        pmuludq   %xmm4, %xmm12                                 #4465.11
        paddq     %xmm0, %xmm6                                  #4479.11
        movdqa    %xmm14, %xmm8                                 #4483.11
        movdqa    %xmm14, %xmm15                                #4456.10
        pand      %xmm9, %xmm8                                  #4483.11
        psrlq     $32, %xmm13                                   #4493.11
        paddq     %xmm6, %xmm8                                  #4488.11
        movq      (%rsi,%rdi), %xmm7                            #4444.377
        psrlq     $32, %xmm9                                    #4476.11
        movups    %xmm3, (%rsp)                                 #[spill]
        pand      %xmm7, %xmm15                                 #4456.10
        movdqa    %xmm5, %xmm3                                  #4470.11
        movdqa    %xmm14, %xmm6                                 #4484.11
        paddq     %xmm8, %xmm13                                 #4494.11
        paddq     %xmm9, %xmm11                                 #4480.11
        pmuludq   %xmm15, %xmm3                                 #4470.11
        pmuludq   %xmm15, %xmm4                                 #4464.11
        pand      %xmm10, %xmm6                                 #4484.11
        movdqa    %xmm13, %xmm0                                 #4495.11
        paddq     %xmm11, %xmm6                                 #4489.11
        psrlq     $32, %xmm0                                    #4495.11
        psrlq     $32, %xmm10                                   #4477.11
        psrlq     $32, %xmm7                                    #4455.10
        movdqa    %xmm14, %xmm9                                 #4485.11
        paddq     %xmm6, %xmm0                                  #4496.11
        paddq     %xmm10, %xmm12                                #4481.11
        pmuludq   %xmm7, %xmm5                                  #4469.11
        pand      %xmm3, %xmm9                                  #4485.11
        movdqa    %xmm0, %xmm11                                 #4497.11
        paddq     %xmm12, %xmm9                                 #4490.11
        psrlq     $32, %xmm11                                   #4497.11
        psrlq     $32, %xmm3                                    #4478.11
        paddq     %xmm9, %xmm11                                 #4498.11
        paddq     %xmm3, %xmm4                                  #4482.11
        pand      %xmm14, %xmm5                                 #4486.11
        movdqa    %xmm11, %xmm8                                 #4499.11
        paddq     %xmm4, %xmm5                                  #4491.11
        psrlq     $32, %xmm8                                    #4499.11
        pand      %xmm14, %xmm11                                #4502.11
        paddq     %xmm5, %xmm8                                  #4500.11
        psllq     $32, %xmm8                                    #4503.17
        psllq     $32, %xmm0                                    #4504.17
        paddq     %xmm11, %xmm8                                 #4505.17
        movdqu    .L_2il0floatpacket.39(%rip), %xmm3            #4508.15
        movdqa    %xmm8, %xmm4                                  #4511.18
        movdqu    .L_2il0floatpacket.40(%rip), %xmm10           #4510.20
        pand      %xmm1, %xmm3                                  #4508.15
        movups    .L_2il0floatpacket.41(%rip), %xmm12           #4514.15
        psrlq     $12, %xmm4                                    #4511.18
        pxor      %xmm3, %xmm10                                 #4510.20
        movaps    %xmm12, %xmm7                                 #4515.19
        por       %xmm10, %xmm4                                 #4512.18
        pand      %xmm14, %xmm13                                #4501.11
        paddq     %xmm13, %xmm0                                 #4506.17
        addsd     %xmm4, %xmm7                                  #4515.19
        movaps    %xmm7, %xmm13                                 #4516.9
        andl      $1, %edx                                      #4356.102
        movdqu    .L_2il0floatpacket.46(%rip), %xmm6            #4533.18
        subsd     %xmm12, %xmm13                                #4516.9
        movdqu    .L_2il0floatpacket.44(%rip), %xmm5            #4524.18
        pand      %xmm8, %xmm6                                  #4533.18
        movdqu    .L_2il0floatpacket.43(%rip), %xmm14           #4522.20
        pand      %xmm0, %xmm5                                  #4524.18
        psllq     $40, %xmm6                                    #4534.18
        psrlq     $24, %xmm0                                    #4535.17
        pxor      %xmm3, %xmm14                                 #4522.20
        por       %xmm0, %xmm6                                  #4536.18
        pxor      .L_2il0floatpacket.45(%rip), %xmm3            #4531.20
        psllq     $28, %xmm5                                    #4525.18
        por       %xmm3, %xmm6                                  #4537.18
        por       %xmm14, %xmm5                                 #4526.18
        movups    .L_2il0floatpacket.50(%rip), %xmm0            #4549.16
        subsd     %xmm13, %xmm4                                 #4517.17
        subsd     %xmm3, %xmm6                                  #4540.18
        subsd     %xmm14, %xmm5                                 #4529.17
        movaps    %xmm4, %xmm8                                  #4541.11
        movups    .L_2il0floatpacket.48(%rip), %xmm15           #4546.16
        addsd     %xmm6, %xmm8                                  #4541.11
        andps     %xmm8, %xmm0                                  #4549.16
        subsd     %xmm8, %xmm4                                  #4542.17
        subsd     %xmm0, %xmm8                                  #4550.17
        addsd     %xmm4, %xmm6                                  #4543.18
        movups    .L_2il0floatpacket.49(%rip), %xmm4            #4547.17
        addsd     %xmm5, %xmm6                                  #4544.11
        movups    .L_2il0floatpacket.47(%rip), %xmm10           #4554.12
        movaps    %xmm4, %xmm9                                  #4553.12
        mulsd     %xmm0, %xmm9                                  #4553.12
        movaps    %xmm15, %xmm5                                 #4551.16
        mulsd     %xmm8, %xmm15                                 #4552.12
        mulsd     %xmm6, %xmm10                                 #4554.12
        mulsd     %xmm8, %xmm4                                  #4555.12
        addsd     %xmm9, %xmm15                                 #4556.13
        mulsd     %xmm0, %xmm5                                  #4551.16
        addsd     %xmm4, %xmm10                                 #4557.13
        movups    .L_2il0floatpacket.52(%rip), %xmm11           #4563.16
        movaps    %xmm5, %xmm12                                 #4559.13
        movups    .L_2il0floatpacket.51(%rip), %xmm3            #4564.11
        movaps    %xmm11, %xmm6                                 #4565.17
        andps     %xmm1, %xmm3                                  #4564.11
        addsd     %xmm10, %xmm15                                #4558.13
        cmpltsd   %xmm3, %xmm6                                  #4565.17
        addsd     %xmm15, %xmm12                                #4559.13
        movaps    %xmm3, %xmm4                                  #4565.17
        cmplesd   %xmm11, %xmm3                                 #4566.20
        subsd     %xmm12, %xmm5                                 #4560.16
        andps     .L_2il0floatpacket.42(%rip), %xmm7            #4520.13
        movsd     %xmm6, %xmm4                                  #4565.17
        movd      %xmm7, %r8d                                   #4572.132
        andps     %xmm1, %xmm3                                  #4567.24
        andps     %xmm4, %xmm12                                 #4568.25
        addsd     %xmm15, %xmm5                                 #4561.13
        orps      %xmm12, %xmm3                                 #4569.9
        andps     %xmm5, %xmm4                                  #4570.9
        shll      $5, %r8d                                      #4572.229
        movaps    %xmm3, %xmm1                                  #4576.26
        movaps    %xmm3, %xmm7                                  #4584.19
        mulsd     8(%rax,%r8), %xmm1                            #4576.26
        mulsd     %xmm3, %xmm7                                  #4584.19
        movq      (%r8,%rax), %xmm10                            #4572.369
        movq      16(%rax,%r8), %xmm9                           #4574.369
        movdqa    %xmm10, %xmm0                                 #4575.23
        mulsd     %xmm3, %xmm0                                  #4575.23
        movdqa    %xmm9, %xmm6                                  #4577.26
        addsd     %xmm1, %xmm6                                  #4577.26
        addsd     8(%rax,%r8), %xmm10                           #4590.17
        movsd     17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #4587.30
        movaps    %xmm0, %xmm12                                 #4578.25
        movsd     17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #4594.30
        movdqa    %xmm9, %xmm5                                  #4579.22
        mulsd     %xmm7, %xmm8                                  #4587.30
        addsd     %xmm6, %xmm12                                 #4578.25
        subsd     %xmm6, %xmm5                                  #4579.22
        mulsd     %xmm7, %xmm11                                 #4594.30
        addsd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #4587.18
        subsd     %xmm12, %xmm6                                 #4581.22
        addsd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #4594.18
        addsd     %xmm1, %xmm5                                  #4580.22
        mulsd     %xmm7, %xmm8                                  #4588.18
        addsd     %xmm0, %xmm6                                  #4582.22
        mulsd     %xmm7, %xmm11                                 #4596.30
        mulsd     %xmm3, %xmm8                                  #4589.18
        addsd     %xmm5, %xmm6                                  #4583.25
        mulsd     %xmm9, %xmm3                                  #4599.36
        addsd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #4596.18
        mulsd     %xmm10, %xmm8                                 #4591.33
        mulsd     %xmm7, %xmm11                                 #4597.18
        subsd     %xmm3, %xmm10                                 #4599.17
        addsd     %xmm6, %xmm8                                  #4591.21
        mulsd     %xmm9, %xmm11                                 #4598.33
        mulsd     %xmm10, %xmm4                                 #4601.32
        addsd     %xmm8, %xmm11                                 #4598.21
        addsd     24(%rax,%r8), %xmm4                           #4601.20
        movups    32(%rsp), %xmm5                               #4605.18[spill]
        addsd     %xmm4, %xmm11                                 #4602.21
        addsd     %xmm11, %xmm12                                #4603.24
        pxor      %xmm2, %xmm12                                 #4604.24
        movups    (%rsp), %xmm2                                 #4605.29[spill]
        movaps    %xmm2, %xmm0                                  #4605.29
        andps     %xmm2, %xmm12                                 #4605.78
        andnps    16(%rsp), %xmm0                               #4605.29[spill]
        orps      %xmm12, %xmm0                                 #4605.18
        jmp       ..B5.2        # Prob 100%                     #4605.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm5
	.cfi_endproc
# mark_end;
	.type	__svml_sin1_ha_ex,@function
	.size	__svml_sin1_ha_ex,.-__svml_sin1_ha_ex
..LN__svml_sin1_ha_ex.4:
	.data
# -- End  __svml_sin1_ha_ex
	.text
.L_2__routine_start___svml_sin2_ha_l9_5:
# -- Begin  __svml_sin2_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin2_ha_l9
# --- __svml_sin2_ha_l9(__m128d)
__svml_sin2_ha_l9:
# parameter 1: %xmm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4616.1
        .byte     15                                            #4959.18
        .byte     30                                            #4959.18
        .byte     250                                           #4959.18
	.cfi_startproc
..___tag_value___svml_sin2_ha_l9.154:
..L155:
                                                        #4616.1
        pushq     %rbp                                          #4616.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4616.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4616.1
        subq      $192, %rsp                                    #4616.1
        vmovapd   %xmm0, %xmm6                                  #4616.1
        vmovupd   16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #4663.50
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rsi   #4682.383
        vmovupd   16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #4667.46
        vandnpd   %xmm6, %xmm13, %xmm14                         #4664.17
        vmovupd   16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm7 #4668.51
        vandpd    %xmm13, %xmm6, %xmm13                         #4665.12
        vfmadd213pd %xmm7, %xmm13, %xmm11                       #4669.13
        xorl      %eax, %eax                                    #4662.1
        vmovupd   16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #4671.48
        vmovupd   16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #4673.48
        vsubpd    %xmm7, %xmm11, %xmm4                          #4670.13
        vfnmadd213pd %xmm13, %xmm4, %xmm8                       #4672.14
        vandps    16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm11, %xmm12 #4681.19
        vmovd     %xmm12, %edx                                  #4682.128
        vmovapd   %xmm8, %xmm5                                  #4674.17
        vfnmadd231pd %xmm4, %xmm9, %xmm5                        #4674.17
        shll      $5, %edx                                      #4682.298
        vmovapd   %xmm5, %xmm7                                  #4685.22
        vpextrd   $2, %xmm12, %ecx                              #4682.190
        vsubpd    %xmm5, %xmm8, %xmm10                          #4675.17
        shll      $5, %ecx                                      #4682.349
        vmovq     8(%rdx,%rsi), %xmm1                           #4683.614
        vmovq     16(%rdx,%rsi), %xmm0                          #4684.612
        vmovhpd   8(%rcx,%rsi), %xmm1, %xmm2                    #4683.582
        vmovhpd   16(%rcx,%rsi), %xmm0, %xmm0                   #4684.580
        vmovq     (%rdx,%rsi), %xmm15                           #4682.612
        vfmadd213pd %xmm0, %xmm2, %xmm7                         #4685.22
        vmovhpd   (%rcx,%rsi), %xmm15, %xmm3                    #4682.580
        vmulpd    %xmm5, %xmm5, %xmm15                          #4692.15
        vfnmadd231pd %xmm4, %xmm9, %xmm10                       #4676.17
        vmovapd   %xmm7, %xmm1                                  #4686.21
        vfmadd231pd %xmm3, %xmm5, %xmm1                         #4686.21
        vsubpd    %xmm7, %xmm0, %xmm8                           #4687.18
        vfnmadd132pd 16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm10, %xmm4 #4678.13
        vsubpd    %xmm1, %xmm7, %xmm9                           #4689.18
        vfmadd231pd %xmm5, %xmm2, %xmm8                         #4688.18
        vmovupd   16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #4695.14
        vfmadd231pd 17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm15, %xmm10 #4695.14
        vfmadd231pd %xmm3, %xmm5, %xmm9                         #4690.18
        vaddpd    %xmm2, %xmm3, %xmm3                           #4698.13
        vmulpd    %xmm10, %xmm15, %xmm11                        #4696.14
        vaddpd    %xmm9, %xmm8, %xmm12                          #4691.21
        vmulpd    %xmm11, %xmm5, %xmm7                          #4697.14
        vfnmadd213pd %xmm3, %xmm0, %xmm5                        #4707.13
        vmovupd   17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm2 #4702.14
        vfmadd231pd 17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm15, %xmm2 #4702.14
        vfmadd213pd %xmm12, %xmm3, %xmm7                        #4699.17
        vfmadd213pd 17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm15, %xmm2 #4704.14
        vmulpd    %xmm2, %xmm15, %xmm2                          #4705.14
        vfmadd213pd %xmm7, %xmm0, %xmm2                         #4706.17
        vmovq     24(%rdx,%rsi), %xmm0                          #4708.612
        vmovhpd   24(%rcx,%rsi), %xmm0, %xmm3                   #4708.580
        vfmadd213pd %xmm3, %xmm5, %xmm4                         #4709.16
        vaddpd    %xmm4, %xmm2, %xmm4                           #4710.17
        vaddpd    %xmm4, %xmm1, %xmm5                           #4711.14
        vcmpnlepd 16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm13, %xmm1 #4713.26
        vmovmskpd %xmm1, %edi                                   #4715.78
        vxorpd    %xmm14, %xmm5, %xmm2                          #4712.14
        testl     %edi, %edi                                    #4716.66
        jne       ..B6.12       # Prob 5%                       #4716.66
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm2 xmm6 xmm13 xmm14
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #4962.52
        jne       ..B6.4        # Prob 5%                       #4962.52
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm6
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #4965.12
        movq      %rbp, %rsp                                    #4965.12
        popq      %rbp                                          #4965.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4965.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        vmovupd   %xmm6, 64(%rsp)                               #4962.197
        vmovupd   %xmm2, 128(%rsp)                              #4962.270
        je        ..B6.3        # Prob 95%                      #4962.374
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4962.454
        movq      %r12, 8(%rsp)                                 #4962.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #4962.454
        movq      %r13, (%rsp)                                  #4962.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #4962.454
                                # LOE rbx r12 r14 r15 r13d
..B6.8:                         # Preds ..B6.9 ..B6.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4962.517
        jc        ..B6.11       # Prob 5%                       #4962.517
                                # LOE rbx r12 r14 r15 r13d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4962.470
        cmpl      $2, %r12d                                     #4962.465
        jl        ..B6.8        # Prob 82%                      #4962.465
                                # LOE rbx r12 r14 r15 r13d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #4962.673
        jmp       ..B6.3        # Prob 100%                     #4962.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #4962.546
        lea       128(%rsp,%r12,8), %rsi                        #4962.546
..___tag_value___svml_sin2_ha_l9.172:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #4962.546
..___tag_value___svml_sin2_ha_l9.173:
        jmp       ..B6.9        # Prob 100%                     #4962.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.35(%rip), %xmm13, %xmm11   #4809.18
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %r8 #4811.385
        vpsrlq    $52, %xmm11, %xmm12                           #4810.18
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r11   #4928.387
        vmovd     %xmm12, %edx                                  #4811.124
        vpand     .L_2il0floatpacket.36(%rip), %xmm13, %xmm9    #4819.21
        vmovupd   16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm0 #4719.55
        vpextrd   $2, %xmm12, %esi                              #4811.189
        vandpd    %xmm13, %xmm0, %xmm10                         #4720.20
        vmovupd   %xmm6, 32(%rsp)                               #[spill]
        lea       (%rdx,%rdx,2), %ecx                           #4811.300
        shll      $3, %ecx                                      #4811.300
        vmovupd   %xmm1, (%rsp)                                 #[spill]
        vmovq     16(%rcx,%r8), %xmm6                           #4813.639
        lea       (%rsi,%rsi,2), %edi                           #4811.351
        shll      $3, %edi                                      #4811.351
        vpaddq    .L_2il0floatpacket.37(%rip), %xmm9, %xmm1     #4820.21
        vcmpeqpd  %xmm0, %xmm10, %xmm15                         #4721.25
        vmovhpd   16(%rdi,%r8), %xmm6, %xmm11                   #4813.607
        vpsrlq    $32, %xmm1, %xmm9                             #4828.12
        vmovdqu   .L_2il0floatpacket.38(%rip), %xmm12           #4821.17
        vpsrlq    $32, %xmm11, %xmm0                            #4826.12
        vmovq     8(%rcx,%r8), %xmm7                            #4812.640
        vpand     %xmm12, %xmm1, %xmm6                          #4829.12
        vmovhpd   8(%rdi,%r8), %xmm7, %xmm3                     #4812.608
        vpand     %xmm12, %xmm11, %xmm5                         #4827.12
        vpmuludq  %xmm0, %xmm9, %xmm11                          #4834.13
        vpsrlq    $32, %xmm3, %xmm4                             #4824.12
        vpmuludq  %xmm0, %xmm6, %xmm0                           #4840.13
        vmovmskpd %xmm15, %eax                                  #4723.48
        vpmuludq  %xmm5, %xmm9, %xmm15                          #4835.13
        vmovupd   %xmm2, 16(%rsp)                               #[spill]
        vpand     %xmm12, %xmm3, %xmm2                          #4825.12
        vpmuludq  %xmm2, %xmm6, %xmm1                           #4839.13
        vpsrlq    $32, %xmm0, %xmm0                             #4842.13
        vpmuludq  %xmm2, %xmm9, %xmm2                           #4833.13
        vpmuludq  %xmm4, %xmm6, %xmm3                           #4838.13
        vpmuludq  %xmm4, %xmm9, %xmm4                           #4832.13
        vpaddq    %xmm0, %xmm11, %xmm11                         #4846.13
        vpand     %xmm12, %xmm1, %xmm5                          #4850.13
        vpsrlq    $32, %xmm15, %xmm15                           #4860.13
        vpaddq    %xmm11, %xmm5, %xmm5                          #4855.13
        vmovq     (%rcx,%r8), %xmm8                             #4811.639
        vpand     %xmm12, %xmm3, %xmm0                          #4851.13
        vpaddq    %xmm5, %xmm15, %xmm11                         #4861.13
        vpsrlq    $32, %xmm1, %xmm5                             #4843.13
        vpsrlq    $32, %xmm11, %xmm15                           #4862.13
        vmovhpd   (%rdi,%r8), %xmm8, %xmm10                     #4811.607
        vpand     %xmm12, %xmm11, %xmm11                        #4868.13
        vpaddq    %xmm5, %xmm2, %xmm2                           #4847.13
        vpand     %xmm12, %xmm10, %xmm7                         #4823.12
        vpsrlq    $32, %xmm10, %xmm10                           #4822.12
        vpmuludq  %xmm7, %xmm6, %xmm8                           #4837.13
        vpaddq    %xmm2, %xmm0, %xmm0                           #4856.13
        vpaddq    %xmm0, %xmm15, %xmm5                          #4863.13
        vpsrlq    $32, %xmm3, %xmm0                             #4844.13
        vpand     %xmm12, %xmm8, %xmm1                          #4852.13
        vpaddq    %xmm0, %xmm4, %xmm15                          #4848.13
        vpmuludq  %xmm10, %xmm6, %xmm0                          #4836.13
        vpaddq    %xmm15, %xmm1, %xmm4                          #4857.13
        vpmuludq  %xmm7, %xmm9, %xmm15                          #4831.13
        vpsrlq    $32, %xmm8, %xmm8                             #4845.13
        vpsrlq    $32, %xmm5, %xmm2                             #4864.13
        vpaddq    %xmm8, %xmm15, %xmm7                          #4849.13
        vpaddq    %xmm4, %xmm2, %xmm4                           #4865.13
        vpand     %xmm12, %xmm0, %xmm6                          #4853.13
        vpsrlq    $32, %xmm4, %xmm3                             #4866.13
        vpaddq    %xmm7, %xmm6, %xmm9                           #4858.13
        vpaddq    %xmm9, %xmm3, %xmm1                           #4867.13
        vpsllq    $32, %xmm1, %xmm2                             #4870.19
        vpand     %xmm12, %xmm4, %xmm3                          #4869.13
        vpaddq    %xmm3, %xmm2, %xmm6                           #4872.19
        vpand     .L_2il0floatpacket.39(%rip), %xmm13, %xmm9    #4875.17
        vpsrlq    $12, %xmm6, %xmm12                            #4878.20
        vpxor     .L_2il0floatpacket.40(%rip), %xmm9, %xmm4     #4877.22
        vpsllq    $32, %xmm5, %xmm5                             #4871.19
        vmovups   .L_2il0floatpacket.41(%rip), %xmm10           #4881.15
        vpor      %xmm4, %xmm12, %xmm0                          #4879.20
        vpaddq    %xmm11, %xmm5, %xmm7                          #4873.19
        vaddpd    %xmm10, %xmm0, %xmm11                         #4882.19
        vsubpd    %xmm10, %xmm11, %xmm15                        #4883.9
        vpand     .L_2il0floatpacket.46(%rip), %xmm6, %xmm6     #4900.20
        vpand     .L_2il0floatpacket.44(%rip), %xmm7, %xmm12    #4891.20
        vpsllq    $40, %xmm6, %xmm4                             #4901.20
        vpsrlq    $24, %xmm7, %xmm7                             #4902.19
        vpsllq    $28, %xmm12, %xmm1                            #4892.20
        vsubpd    %xmm15, %xmm0, %xmm5                          #4884.17
        vpxor     .L_2il0floatpacket.43(%rip), %xmm9, %xmm3     #4889.22
        vpxor     .L_2il0floatpacket.45(%rip), %xmm9, %xmm0     #4898.22
        vpor      %xmm7, %xmm4, %xmm9                           #4903.20
        vpor      %xmm0, %xmm9, %xmm10                          #4904.20
        vpor      %xmm3, %xmm1, %xmm2                           #4893.20
        vandps    .L_2il0floatpacket.42(%rip), %xmm11, %xmm8    #4887.15
        vsubpd    %xmm0, %xmm10, %xmm11                         #4907.18
        vsubpd    %xmm3, %xmm2, %xmm15                          #4896.17
        vmovd     %xmm8, %r9d                                   #4928.132
        vaddpd    %xmm11, %xmm5, %xmm12                         #4908.11
        vsubpd    %xmm12, %xmm5, %xmm5                          #4909.17
        vaddpd    %xmm5, %xmm11, %xmm0                          #4910.18
        vmovups   .L_2il0floatpacket.47(%rip), %xmm11           #4912.16
        vmulpd    %xmm12, %xmm11, %xmm4                         #4914.13
        vaddpd    %xmm0, %xmm15, %xmm6                          #4911.11
        vmovaps   %xmm11, %xmm15                                #4915.13
        vfmsub213pd %xmm4, %xmm12, %xmm15                       #4915.13
        vmovups   .L_2il0floatpacket.52(%rip), %xmm2            #4919.16
        vandpd    .L_2il0floatpacket.51(%rip), %xmm13, %xmm1    #4920.11
        vcmpltpd  %xmm1, %xmm2, %xmm5                           #4921.17
        vcmplepd  %xmm2, %xmm1, %xmm3                           #4922.20
        vfmadd132pd .L_2il0floatpacket.72(%rip), %xmm15, %xmm12 #4916.13
        vandpd    %xmm13, %xmm3, %xmm13                         #4923.24
        vandpd    %xmm4, %xmm5, %xmm0                           #4924.25
        vfmadd213pd %xmm12, %xmm11, %xmm6                       #4917.13
        vorpd     %xmm0, %xmm13, %xmm11                         #4925.9
        vmovupd   16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm3 #4941.18
        shll      $5, %r9d                                      #4928.302
        vandpd    %xmm6, %xmm5, %xmm12                          #4926.9
        vpextrd   $2, %xmm8, %r10d                              #4928.194
        vmovapd   %xmm11, %xmm8                                 #4931.26
        shll      $5, %r10d                                     #4928.353
        vmovq     8(%r9,%r11), %xmm13                           #4929.618
        vmovq     16(%r9,%r11), %xmm1                           #4930.616
        vmovhpd   8(%r10,%r11), %xmm13, %xmm4                   #4929.586
        vmovhpd   16(%r10,%r11), %xmm1, %xmm13                  #4930.584
        vmulpd    %xmm11, %xmm11, %xmm6                         #4938.19
        vfmadd213pd %xmm13, %xmm4, %xmm8                        #4931.26
        vmovq     (%r9,%r11), %xmm15                            #4928.616
        vmovhpd   (%r10,%r11), %xmm15, %xmm0                    #4928.584
        vmovapd   %xmm11, %xmm15                                #4932.25
        vmovq     24(%r9,%r11), %xmm10                          #4954.616
        vfmadd231pd 17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm6, %xmm3 #4941.18
        vfmadd132pd %xmm0, %xmm8, %xmm15                        #4932.25
        vsubpd    %xmm8, %xmm13, %xmm1                          #4933.22
        vaddpd    %xmm4, %xmm0, %xmm9                           #4944.17
        vsubpd    %xmm15, %xmm8, %xmm2                          #4935.22
        vmulpd    %xmm3, %xmm6, %xmm8                           #4942.18
        vfmadd231pd %xmm0, %xmm11, %xmm2                        #4936.22
        vmovupd   17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm0 #4948.18
        vfmadd231pd 17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm6, %xmm0 #4948.18
        vfmadd231pd %xmm11, %xmm4, %xmm1                        #4934.22
        vmulpd    %xmm8, %xmm11, %xmm7                          #4943.18
        vfmadd213pd 17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm6, %xmm0 #4950.18
        vfnmadd213pd %xmm9, %xmm13, %xmm11                      #4953.17
        vaddpd    %xmm2, %xmm1, %xmm5                           #4937.25
        vmulpd    %xmm0, %xmm6, %xmm1                           #4951.18
        vfmadd213pd %xmm5, %xmm9, %xmm7                         #4945.21
        vmovhpd   24(%r10,%r11), %xmm10, %xmm0                  #4954.584
        vmovupd   16(%rsp), %xmm2                               #4959.18[spill]
        vmovupd   32(%rsp), %xmm6                               #4959.18[spill]
        vfmadd213pd %xmm0, %xmm11, %xmm12                       #4955.20
        vfmadd213pd %xmm7, %xmm13, %xmm1                        #4952.21
        vmovupd   (%rsp), %xmm0                                 #4959.18[spill]
        vaddpd    %xmm12, %xmm1, %xmm11                         #4956.21
        vaddpd    %xmm11, %xmm15, %xmm12                        #4957.24
        vxorpd    %xmm14, %xmm12, %xmm14                        #4958.24
        blendvpd  %xmm0, %xmm14, %xmm2                          #4959.18
        jmp       ..B6.2        # Prob 100%                     #4959.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm6
	.cfi_endproc
# mark_end;
	.type	__svml_sin2_ha_l9,@function
	.size	__svml_sin2_ha_l9,.-__svml_sin2_ha_l9
..LN__svml_sin2_ha_l9.5:
	.data
# -- End  __svml_sin2_ha_l9
	.text
.L_2__routine_start___svml_sin1_ha_l9_6:
# -- Begin  __svml_sin1_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin1_ha_l9
# --- __svml_sin1_ha_l9(__m128d)
__svml_sin1_ha_l9:
# parameter 1: %xmm0
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4970.1
        .byte     15                                            #5313.18
        .byte     30                                            #5313.18
        .byte     250                                           #5313.18
	.cfi_startproc
..___tag_value___svml_sin1_ha_l9.183:
..L184:
                                                        #4970.1
        pushq     %rbp                                          #4970.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4970.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4970.1
        subq      $128, %rsp                                    #4970.1
        vmovapd   %xmm0, %xmm6                                  #4970.1
        vmovsd    16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #5017.19
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rcx   #5036.371
        vandnpd   %xmm6, %xmm13, %xmm14                         #5018.17
        vandpd    %xmm13, %xmm6, %xmm13                         #5019.12
        vmovsd    16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm7 #5022.20
        vmovapd   %xmm13, %xmm12                                #5023.13
        vfmadd132sd 16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm7, %xmm12 #5023.13
        xorl      %eax, %eax                                    #5016.1
        vmovq     16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm15 #5034.23
        vmovsd    16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #5027.17
        vpand     %xmm15, %xmm12, %xmm3                         #5035.19
        vsubsd    %xmm7, %xmm12, %xmm5                          #5024.13
        vmovd     %xmm3, %edx                                   #5036.128
        vmovapd   %xmm5, %xmm8                                  #5026.14
        vmovapd   %xmm5, %xmm4                                  #5028.17
        vfnmadd132sd 16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm13, %xmm8 #5026.14
        vmovapd   %xmm5, %xmm11                                 #5030.17
        vmovsd    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #5049.14
        vfnmadd213sd %xmm8, %xmm9, %xmm4                        #5028.17
        shll      $5, %edx                                      #5036.231
        vsubsd    %xmm4, %xmm8, %xmm10                          #5029.17
        vmovq     8(%rcx,%rdx), %xmm0                           #5037.373
        vmovapd   %xmm4, %xmm8                                  #5039.22
        vmovq     16(%rcx,%rdx), %xmm1                          #5038.371
        vfmadd213sd %xmm1, %xmm0, %xmm8                         #5039.22
        vmovq     (%rdx,%rcx), %xmm2                            #5036.371
        vmovdqa   %xmm2, %xmm3                                  #5040.21
        vfnmadd213sd %xmm10, %xmm9, %xmm11                      #5030.17
        vmovdqa   %xmm2, %xmm10                                 #5044.18
        vaddsd    %xmm0, %xmm2, %xmm2                           #5052.13
        vfmadd213sd %xmm8, %xmm4, %xmm3                         #5040.21
        vsubsd    %xmm8, %xmm1, %xmm7                           #5041.18
        vfnmadd132sd 16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm11, %xmm5 #5032.13
        vmovapd   %xmm4, %xmm11                                 #5042.18
        vsubsd    %xmm3, %xmm8, %xmm9                           #5043.18
        vmulsd    %xmm4, %xmm4, %xmm8                           #5046.15
        vfmadd213sd %xmm7, %xmm0, %xmm11                        #5042.18
        vfmadd213sd %xmm9, %xmm4, %xmm10                        #5044.18
        vfmadd213sd 16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm8, %xmm12 #5049.14
        vmovsd    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm0 #5056.14
        vfmadd213sd 17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm8, %xmm0 #5056.14
        vaddsd    %xmm11, %xmm10, %xmm7                         #5045.21
        vmulsd    %xmm8, %xmm12, %xmm15                         #5050.14
        vfmadd213sd 17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm8, %xmm0 #5058.14
        vmulsd    %xmm4, %xmm15, %xmm9                          #5051.14
        vfnmadd213sd %xmm2, %xmm1, %xmm4                        #5061.13
        vmulsd    %xmm8, %xmm0, %xmm0                           #5059.14
        vfmadd213sd %xmm7, %xmm2, %xmm9                         #5053.17
        vfmadd213sd 24(%rcx,%rdx), %xmm4, %xmm5                 #5063.16
        vfmadd213sd %xmm9, %xmm1, %xmm0                         #5060.17
        vaddsd    %xmm5, %xmm0, %xmm1                           #5064.17
        vaddsd    %xmm1, %xmm3, %xmm4                           #5065.14
        vcmpnlesd 16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm13, %xmm1 #5067.26
        vmovmskpd %xmm1, %esi                                   #5069.78
        vxorpd    %xmm14, %xmm4, %xmm2                          #5066.14
        testl     $1, %esi                                      #5070.55
        jne       ..B7.8        # Prob 5%                       #5070.66
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm2 xmm6 xmm13 xmm14
..B7.2:                         # Preds ..B7.8 ..B7.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #5316.52
        jne       ..B7.4        # Prob 5%                       #5316.52
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm6
..B7.3:                         # Preds ..B7.5 ..B7.2
                                # Execution count [1.00e+00]
        vmovapd   %xmm2, %xmm0                                  #5319.12
        movq      %rbp, %rsp                                    #5319.12
        popq      %rbp                                          #5319.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5319.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B7.4:                         # Preds ..B7.2
                                # Execution count [5.00e-02]: Infreq
        vmovsd    %xmm6, (%rsp)                                 #5316.160
        vmovsd    %xmm2, 64(%rsp)                               #5316.233
        jne       ..B7.6        # Prob 5%                       #5316.374
                                # LOE rbx r12 r13 r14 r15 eax
..B7.5:                         # Preds ..B7.7 ..B7.6 ..B7.4
                                # Execution count [5.00e-02]: Infreq
        vmovsd    64(%rsp), %xmm2                               #5316.625
        jmp       ..B7.3        # Prob 100%                     #5316.625
                                # LOE rbx r12 r13 r14 r15 xmm2
..B7.6:                         # Preds ..B7.4
                                # Execution count [2.50e-03]: Infreq
        je        ..B7.5        # Prob 95%                      #5316.517
                                # LOE rbx r12 r13 r14 r15
..B7.7:                         # Preds ..B7.6
                                # Execution count [6.25e-04]: Infreq
        lea       (%rsp), %rdi                                  #5316.546
        lea       64(%rsp), %rsi                                #5316.546
..___tag_value___svml_sin1_ha_l9.192:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #5316.546
..___tag_value___svml_sin1_ha_l9.193:
        jmp       ..B7.5        # Prob 100%                     #5316.546
                                # LOE rbx r12 r13 r14 r15
..B7.8:                         # Preds ..B7.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.35(%rip), %xmm13, %xmm15   #5163.18
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rsi #5165.383
        vpsrlq    $52, %xmm15, %xmm12                           #5164.18
        lea       __svml_dsin_ha_data_internal_ha(%rip), %r8    #5282.375
        vmovd     %xmm12, %edx                                  #5165.124
        vmovsd    16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5073.24
        vmovupd   %xmm6, 32(%rsp)                               #[spill]
        vandpd    %xmm13, %xmm10, %xmm6                         #5074.20
        vpand     .L_2il0floatpacket.36(%rip), %xmm13, %xmm7    #5173.21
        vcmpeqsd  %xmm10, %xmm6, %xmm0                          #5075.25
        vpaddq    .L_2il0floatpacket.37(%rip), %xmm7, %xmm8     #5174.21
        vmovmskpd %xmm0, %eax                                   #5077.48
        vmovdqu   .L_2il0floatpacket.38(%rip), %xmm12           #5175.17
        vpsrlq    $32, %xmm8, %xmm9                             #5182.12
        vpand     %xmm12, %xmm8, %xmm6                          #5183.12
        vmovupd   %xmm2, 16(%rsp)                               #[spill]
        lea       (%rdx,%rdx,2), %ecx                           #5165.230
        shll      $3, %ecx                                      #5165.230
        andl      $1, %eax                                      #5077.102
        vmovq     16(%rsi,%rcx), %xmm11                         #5167.383
        vpand     %xmm12, %xmm11, %xmm15                        #5181.12
        vpsrlq    $32, %xmm11, %xmm0                            #5180.12
        vpmuludq  %xmm15, %xmm9, %xmm11                         #5189.13
        vmovq     8(%rsi,%rcx), %xmm5                           #5166.384
        vpsrlq    $32, %xmm11, %xmm15                           #5214.13
        vpmuludq  %xmm0, %xmm9, %xmm11                          #5188.13
        vpand     %xmm12, %xmm5, %xmm2                          #5179.12
        vpmuludq  %xmm0, %xmm6, %xmm0                           #5194.13
        vmovupd   %xmm1, (%rsp)                                 #[spill]
        vpsrlq    $32, %xmm0, %xmm0                             #5196.13
        vpmuludq  %xmm2, %xmm6, %xmm1                           #5193.13
        vpsrlq    $32, %xmm5, %xmm4                             #5178.12
        vpmuludq  %xmm2, %xmm9, %xmm2                           #5187.13
        vpmuludq  %xmm4, %xmm6, %xmm3                           #5192.13
        vpmuludq  %xmm4, %xmm9, %xmm4                           #5186.13
        vpaddq    %xmm0, %xmm11, %xmm11                         #5200.13
        vpand     %xmm12, %xmm1, %xmm5                          #5204.13
        vpsrlq    $32, %xmm1, %xmm1                             #5197.13
        vmovq     (%rcx,%rsi), %xmm10                           #5165.383
        vpand     %xmm12, %xmm3, %xmm0                          #5205.13
        vpaddq    %xmm11, %xmm5, %xmm5                          #5209.13
        vpand     %xmm12, %xmm10, %xmm7                         #5177.12
        vpsrlq    $32, %xmm10, %xmm10                           #5176.12
        vpaddq    %xmm5, %xmm15, %xmm11                         #5215.13
        vpaddq    %xmm1, %xmm2, %xmm5                           #5201.13
        vpmuludq  %xmm7, %xmm6, %xmm8                           #5191.13
        vpmuludq  %xmm7, %xmm9, %xmm9                           #5185.13
        vpmuludq  %xmm10, %xmm6, %xmm6                          #5190.13
        vpaddq    %xmm5, %xmm0, %xmm0                           #5210.13
        vpsrlq    $32, %xmm11, %xmm15                           #5216.13
        vpsrlq    $32, %xmm3, %xmm3                             #5198.13
        vpaddq    %xmm0, %xmm15, %xmm5                          #5217.13
        vpaddq    %xmm3, %xmm4, %xmm0                           #5202.13
        vpand     %xmm12, %xmm8, %xmm1                          #5206.13
        vpsrlq    $32, %xmm5, %xmm2                             #5218.13
        vpaddq    %xmm0, %xmm1, %xmm15                          #5211.13
        vpsrlq    $32, %xmm8, %xmm0                             #5199.13
        vpsllq    $32, %xmm5, %xmm5                             #5225.19
        vpaddq    %xmm0, %xmm9, %xmm7                           #5203.13
        vpaddq    %xmm15, %xmm2, %xmm4                          #5219.13
        vpand     %xmm12, %xmm6, %xmm15                         #5207.13
        vpsrlq    $32, %xmm4, %xmm3                             #5220.13
        vpaddq    %xmm7, %xmm15, %xmm9                          #5212.13
        vpaddq    %xmm9, %xmm3, %xmm8                           #5221.13
        vpsllq    $32, %xmm8, %xmm1                             #5224.19
        vpand     %xmm12, %xmm4, %xmm2                          #5223.13
        vpaddq    %xmm2, %xmm1, %xmm9                           #5226.19
        vpand     .L_2il0floatpacket.39(%rip), %xmm13, %xmm8    #5229.17
        vpand     %xmm12, %xmm11, %xmm12                        #5222.13
        vpxor     .L_2il0floatpacket.40(%rip), %xmm8, %xmm3     #5231.22
        vpsrlq    $12, %xmm9, %xmm11                            #5232.20
        vmovups   .L_2il0floatpacket.41(%rip), %xmm4            #5235.15
        vpor      %xmm3, %xmm11, %xmm6                          #5233.20
        vpaddq    %xmm12, %xmm5, %xmm5                          #5227.19
        vaddsd    %xmm6, %xmm4, %xmm7                           #5236.19
        vpand     .L_2il0floatpacket.46(%rip), %xmm9, %xmm9     #5254.20
        vsubsd    %xmm4, %xmm7, %xmm10                          #5237.9
        vpand     .L_2il0floatpacket.44(%rip), %xmm5, %xmm0     #5245.20
        vpsllq    $40, %xmm9, %xmm2                             #5255.20
        vpsrlq    $24, %xmm5, %xmm5                             #5256.19
        vpsllq    $28, %xmm0, %xmm15                            #5246.20
        vpor      %xmm5, %xmm2, %xmm3                           #5257.20
        vsubsd    %xmm10, %xmm6, %xmm11                         #5238.17
        vpxor     .L_2il0floatpacket.45(%rip), %xmm8, %xmm6     #5252.22
        vpor      %xmm6, %xmm3, %xmm4                           #5258.20
        vpxor     .L_2il0floatpacket.43(%rip), %xmm8, %xmm1     #5243.22
        vsubsd    %xmm6, %xmm4, %xmm8                           #5261.18
        vpor      %xmm1, %xmm15, %xmm12                         #5247.20
        vmovups   .L_2il0floatpacket.52(%rip), %xmm3            #5273.16
        vsubsd    %xmm1, %xmm12, %xmm15                         #5250.17
        vaddsd    %xmm8, %xmm11, %xmm12                         #5262.11
        vmovapd   %xmm12, %xmm0                                 #5269.13
        vsubsd    %xmm12, %xmm11, %xmm11                        #5263.17
        vandpd    .L_2il0floatpacket.51(%rip), %xmm13, %xmm2    #5274.11
        vaddsd    %xmm11, %xmm8, %xmm10                         #5264.18
        vcmpltsd  %xmm2, %xmm3, %xmm1                           #5275.17
        vcmplesd  %xmm3, %xmm2, %xmm4                           #5276.20
        vaddsd    %xmm15, %xmm10, %xmm8                         #5265.11
        vmovups   .L_2il0floatpacket.47(%rip), %xmm15           #5266.16
        vmovsd    %xmm1, %xmm2, %xmm6                           #5275.17
        vmulsd    %xmm15, %xmm12, %xmm5                         #5268.13
        vandpd    %xmm13, %xmm4, %xmm13                         #5277.24
        vfmsub213sd %xmm5, %xmm15, %xmm0                        #5269.13
        vmovsd    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm4 #5295.18
        vmovsd    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #5302.18
        vfmadd132sd .L_2il0floatpacket.72(%rip), %xmm0, %xmm12  #5270.13
        vandpd    %xmm5, %xmm6, %xmm0                           #5278.25
        vfmadd213sd %xmm12, %xmm15, %xmm8                       #5271.13
        vorpd     %xmm0, %xmm13, %xmm12                         #5279.9
        vandps    .L_2il0floatpacket.42(%rip), %xmm7, %xmm15    #5241.15
        vmovapd   %xmm12, %xmm1                                 #5285.26
        vmovapd   %xmm12, %xmm3                                 #5288.22
        vmovd     %xmm15, %edi                                  #5282.132
        vmulsd    %xmm12, %xmm12, %xmm10                        #5292.19
        shll      $5, %edi                                      #5282.235
        vandpd    %xmm8, %xmm6, %xmm0                           #5280.9
        vmovq     16(%r8,%rdi), %xmm13                          #5284.375
        vfmadd132sd 8(%r8,%rdi), %xmm13, %xmm1                  #5285.26
        vmovq     (%rdi,%r8), %xmm6                             #5282.375
        vmovdqa   %xmm6, %xmm15                                 #5286.25
        vmovdqa   %xmm6, %xmm2                                  #5290.22
        vfmadd213sd 16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm10, %xmm4 #5295.18
        vfmadd213sd 17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm10, %xmm9 #5302.18
        vfmadd213sd %xmm1, %xmm12, %xmm15                       #5286.25
        vsubsd    %xmm1, %xmm13, %xmm7                          #5287.22
        vmulsd    %xmm10, %xmm4, %xmm5                          #5296.18
        vfmadd213sd 17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm10, %xmm9 #5304.18
        vfmadd132sd 8(%r8,%rdi), %xmm7, %xmm3                   #5288.22
        vsubsd    %xmm15, %xmm1, %xmm7                          #5289.22
        vaddsd    8(%r8,%rdi), %xmm6, %xmm1                     #5298.17
        vmulsd    %xmm12, %xmm5, %xmm11                         #5297.18
        vfmadd213sd %xmm7, %xmm12, %xmm2                        #5290.22
        vfnmadd213sd %xmm1, %xmm13, %xmm12                      #5307.17
        vmovupd   32(%rsp), %xmm6                               #5313.18[spill]
        vaddsd    %xmm3, %xmm2, %xmm8                           #5291.25
        vmulsd    %xmm10, %xmm9, %xmm2                          #5305.18
        vfmadd213sd 24(%r8,%rdi), %xmm12, %xmm0                 #5309.20
        vfmadd213sd %xmm8, %xmm1, %xmm11                        #5299.21
        vfmadd213sd %xmm11, %xmm13, %xmm2                       #5306.21
        vaddsd    %xmm0, %xmm2, %xmm12                          #5310.21
        vmovupd   (%rsp), %xmm0                                 #5313.18[spill]
        vmovupd   16(%rsp), %xmm2                               #5313.18[spill]
        vaddsd    %xmm12, %xmm15, %xmm13                        #5311.24
        vxorpd    %xmm14, %xmm13, %xmm14                        #5312.24
        blendvpd  %xmm0, %xmm14, %xmm2                          #5313.18
        jmp       ..B7.2        # Prob 100%                     #5313.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm2 xmm6
	.cfi_endproc
# mark_end;
	.type	__svml_sin1_ha_l9,@function
	.size	__svml_sin1_ha_l9,.-__svml_sin1_ha_l9
..LN__svml_sin1_ha_l9.6:
	.data
# -- End  __svml_sin1_ha_l9
	.text
.L_2__routine_start___svml_sin2_ha_ex_7:
# -- Begin  __svml_sin2_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin2_ha_ex
# --- __svml_sin2_ha_ex(__m128d)
__svml_sin2_ha_ex:
# parameter 1: %xmm0
..B8.1:                         # Preds ..B8.0
                                # Execution count [1.00e+00]
        .byte     243                                           #5324.1
        .byte     15                                            #5683.18
        .byte     30                                            #5683.18
        .byte     250                                           #5683.18
	.cfi_startproc
..___tag_value___svml_sin2_ha_ex.201:
..L202:
                                                        #5324.1
        pushq     %rbp                                          #5324.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #5324.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #5324.1
        subq      $192, %rsp                                    #5324.1
        movaps    %xmm0, %xmm7                                  #5324.1
        movups    16384+__svml_dsin_ha_data_internal_ha(%rip), %xmm1 #5371.50
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rax   #5391.370
        movups    16576+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #5377.25
        movaps    %xmm1, %xmm2                                  #5372.17
        andps     %xmm7, %xmm1                                  #5373.12
        andnps    %xmm7, %xmm2                                  #5372.17
        mulpd     %xmm1, %xmm13                                 #5377.25
        movups    16640+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #5376.51
        movaps    %xmm1, %xmm6                                  #5380.14
        addpd     %xmm8, %xmm13                                 #5377.13
        movaps    %xmm13, %xmm11                                #5378.13
        xorl      %edx, %edx                                    #5370.1
        subpd     %xmm8, %xmm11                                 #5378.13
        movups    16768+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #5379.48
        mulpd     %xmm11, %xmm9                                 #5380.36
        movups    16832+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5382.19
        mulpd     %xmm11, %xmm10                                #5382.19
        subpd     %xmm9, %xmm6                                  #5380.14
        movaps    %xmm6, %xmm5                                  #5383.17
        subpd     %xmm10, %xmm5                                 #5383.17
        andps     16704+__svml_dsin_ha_data_internal_ha(%rip), %xmm13 #5390.17
        movaps    %xmm5, %xmm3                                  #5395.22
        movd      %xmm13, %ecx                                  #5391.128
        movaps    %xmm5, %xmm8                                  #5394.19
        movups    16896+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #5386.50
        subpd     %xmm5, %xmm6                                  #5384.17
        mulpd     %xmm11, %xmm12                                #5387.32
        pextrw    $4, %xmm13, %esi                              #5391.184
        subpd     %xmm10, %xmm6                                 #5385.17
        shll      $5, %ecx                                      #5391.285
        movaps    %xmm5, %xmm11                                 #5403.15
        shll      $5, %esi                                      #5391.336
        movq      8(%rcx,%rax), %xmm9                           #5392.601
        mulpd     %xmm5, %xmm11                                 #5403.15
        subpd     %xmm12, %xmm6                                 #5387.13
        movhpd    8(%rsi,%rax), %xmm9                           #5392.569
        mulpd     %xmm9, %xmm3                                  #5395.22
        movq      16(%rcx,%rax), %xmm12                         #5393.599
        movups    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5413.26
        movhpd    16(%rsi,%rax), %xmm12                         #5393.567
        mulpd     %xmm11, %xmm10                                #5413.26
        movaps    %xmm12, %xmm15                                #5396.22
        movaps    %xmm12, %xmm14                                #5398.18
        addpd     %xmm3, %xmm15                                 #5396.22
        addpd     17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5413.14
        subpd     %xmm15, %xmm14                                #5398.18
        mulpd     %xmm11, %xmm10                                #5415.26
        addpd     %xmm14, %xmm3                                 #5399.18
        addpd     17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5415.14
        movups    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm14 #5406.26
        movaps    %xmm15, %xmm0                                 #5397.21
        mulpd     %xmm11, %xmm14                                #5406.26
        movq      (%rcx,%rax), %xmm4                            #5391.599
        movhpd    (%rsi,%rax), %xmm4                            #5391.567
        mulpd     %xmm4, %xmm8                                  #5394.19
        addpd     16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm14 #5406.14
        addpd     %xmm8, %xmm0                                  #5397.21
        addpd     %xmm9, %xmm4                                  #5409.13
        mulpd     %xmm11, %xmm14                                #5407.14
        mulpd     %xmm10, %xmm11                                #5416.14
        subpd     %xmm0, %xmm15                                 #5400.18
        mulpd     %xmm5, %xmm14                                 #5408.14
        mulpd     %xmm12, %xmm11                                #5417.29
        mulpd     %xmm5, %xmm12                                 #5418.32
        addpd     %xmm15, %xmm8                                 #5401.18
        mulpd     %xmm4, %xmm14                                 #5410.29
        subpd     %xmm12, %xmm4                                 #5418.13
        addpd     %xmm8, %xmm3                                  #5402.21
        mulpd     %xmm4, %xmm6                                  #5420.28
        addpd     %xmm14, %xmm3                                 #5410.17
        movq      24(%rcx,%rax), %xmm5                          #5419.599
        movhpd    24(%rsi,%rax), %xmm5                          #5419.567
        addpd     %xmm11, %xmm3                                 #5417.17
        addpd     %xmm6, %xmm5                                  #5420.16
        addpd     %xmm5, %xmm3                                  #5421.17
        addpd     %xmm3, %xmm0                                  #5422.14
        movaps    %xmm1, %xmm3                                  #5424.26
        pxor      %xmm2, %xmm0                                  #5423.14
        cmpnlepd  16448+__svml_dsin_ha_data_internal_ha(%rip), %xmm3 #5424.26
        movmskpd  %xmm3, %edi                                   #5426.78
        testl     %edi, %edi                                    #5427.66
        jne       ..B8.12       # Prob 5%                       #5427.66
                                # LOE rax rbx r12 r13 r14 r15 edx xmm0 xmm1 xmm2 xmm3 xmm7
..B8.2:                         # Preds ..B8.12 ..B8.1
                                # Execution count [1.00e+00]
        testl     %edx, %edx                                    #5686.52
        jne       ..B8.4        # Prob 5%                       #5686.52
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
..B8.3:                         # Preds ..B8.4 ..B8.10 ..B8.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #5689.12
        popq      %rbp                                          #5689.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #5689.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B8.4:                         # Preds ..B8.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm7, 64(%rsp)                               #5686.197
        movups    %xmm0, 128(%rsp)                              #5686.270
        je        ..B8.3        # Prob 95%                      #5686.374
                                # LOE rbx r12 r13 r14 r15 edx xmm0
..B8.7:                         # Preds ..B8.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %eax, %eax                                    #5686.454
        movq      %r12, 8(%rsp)                                 #5686.454[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #5686.454
        movq      %r13, (%rsp)                                  #5686.454[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r13d                                   #5686.454
                                # LOE rbx r12 r14 r15 r13d
..B8.8:                         # Preds ..B8.9 ..B8.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #5686.517
        jc        ..B8.11       # Prob 5%                       #5686.517
                                # LOE rbx r12 r14 r15 r13d
..B8.9:                         # Preds ..B8.11 ..B8.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #5686.470
        cmpl      $2, %r12d                                     #5686.465
        jl        ..B8.8        # Prob 82%                      #5686.465
                                # LOE rbx r12 r14 r15 r13d
..B8.10:                        # Preds ..B8.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #5686.673
        jmp       ..B8.3        # Prob 100%                     #5686.673
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B8.11:                        # Preds ..B8.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,8), %rdi                         #5686.546
        lea       128(%rsp,%r12,8), %rsi                        #5686.546
..___tag_value___svml_sin2_ha_ex.219:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #5686.546
..___tag_value___svml_sin2_ha_ex.220:
        jmp       ..B8.9        # Prob 100%                     #5686.546
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B8.12:                        # Preds ..B8.1
                                # Execution count [5.00e-02]: Infreq
        movdqu    .L_2il0floatpacket.35(%rip), %xmm10           #5520.16
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %r9 #5522.372
        pand      %xmm1, %xmm10                                 #5520.16
        psrlq     $52, %xmm10                                   #5521.16
        movups    16512+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #5430.55
        movd      %xmm10, %ecx                                  #5522.124
        movups    %xmm0, 16(%rsp)                               #[spill]
        movaps    %xmm8, %xmm0                                  #5431.20
        movups    %xmm3, (%rsp)                                 #[spill]
        andps     %xmm1, %xmm0                                  #5431.20
        pextrw    $4, %xmm10, %edi                              #5522.183
        cmpeqpd   %xmm8, %xmm0                                  #5432.25
        movdqu    .L_2il0floatpacket.36(%rip), %xmm3            #5530.19
        lea       (%rcx,%rcx,2), %esi                           #5522.287
        pand      %xmm1, %xmm3                                  #5530.19
        lea       (%rdi,%rdi,2), %r8d                           #5522.338
        shll      $3, %esi                                      #5522.287
        paddq     .L_2il0floatpacket.37(%rip), %xmm3            #5531.19
        movmskpd  %xmm0, %edx                                   #5434.48
        movq      16(%rsi,%r9), %xmm15                          #5524.626
        movups    %xmm7, 32(%rsp)                               #[spill]
        movdqa    %xmm3, %xmm7                                  #5539.10
        shll      $3, %r8d                                      #5522.338
        psrlq     $32, %xmm7                                    #5539.10
        movhpd    16(%r8,%r9), %xmm15                           #5524.594
        movdqa    %xmm7, %xmm0                                  #5545.11
        movdqu    .L_2il0floatpacket.38(%rip), %xmm5            #5532.15
        movaps    %xmm15, %xmm8                                 #5537.10
        movq      8(%rsi,%r9), %xmm13                           #5523.627
        psrlq     $32, %xmm8                                    #5537.10
        pand      %xmm5, %xmm3                                  #5540.10
        pand      %xmm5, %xmm15                                 #5538.10
        movhpd    8(%r8,%r9), %xmm13                            #5523.595
        movdqa    %xmm3, %xmm11                                 #5550.11
        pmuludq   %xmm8, %xmm0                                  #5545.11
        movaps    %xmm13, %xmm14                                #5535.10
        pmuludq   %xmm3, %xmm8                                  #5551.11
        pmuludq   %xmm7, %xmm15                                 #5546.11
        pand      %xmm5, %xmm13                                 #5536.10
        psrlq     $32, %xmm8                                    #5553.11
        pmuludq   %xmm13, %xmm11                                #5550.11
        psrlq     $32, %xmm14                                   #5535.10
        pmuludq   %xmm7, %xmm13                                 #5544.11
        paddq     %xmm8, %xmm0                                  #5557.11
        movdqa    %xmm3, %xmm12                                 #5549.11
        movdqa    %xmm5, %xmm10                                 #5561.11
        pmuludq   %xmm14, %xmm12                                #5549.11
        pand      %xmm11, %xmm10                                #5561.11
        pmuludq   %xmm7, %xmm14                                 #5543.11
        paddq     %xmm0, %xmm10                                 #5566.11
        movq      (%rsi,%r9), %xmm9                             #5522.626
        movdqa    %xmm5, %xmm6                                  #5534.10
        movhpd    (%r8,%r9), %xmm9                              #5522.594
        psrlq     $32, %xmm15                                   #5571.11
        psrlq     $32, %xmm11                                   #5554.11
        pand      %xmm9, %xmm6                                  #5534.10
        movdqa    %xmm3, %xmm4                                  #5548.11
        movdqa    %xmm5, %xmm8                                  #5562.11
        paddq     %xmm10, %xmm15                                #5572.11
        paddq     %xmm11, %xmm13                                #5558.11
        pmuludq   %xmm6, %xmm4                                  #5548.11
        pmuludq   %xmm6, %xmm7                                  #5542.11
        pand      %xmm12, %xmm8                                 #5562.11
        movaps    %xmm15, %xmm0                                 #5573.11
        paddq     %xmm13, %xmm8                                 #5567.11
        psrlq     $32, %xmm0                                    #5573.11
        psrlq     $32, %xmm12                                   #5555.11
        psrlq     $32, %xmm9                                    #5533.10
        movdqa    %xmm5, %xmm11                                 #5563.11
        paddq     %xmm8, %xmm0                                  #5574.11
        paddq     %xmm12, %xmm14                                #5559.11
        pmuludq   %xmm9, %xmm3                                  #5547.11
        pand      %xmm4, %xmm11                                 #5563.11
        movdqa    %xmm0, %xmm13                                 #5575.11
        paddq     %xmm14, %xmm11                                #5568.11
        psrlq     $32, %xmm13                                   #5575.11
        psrlq     $32, %xmm4                                    #5556.11
        paddq     %xmm11, %xmm13                                #5576.11
        paddq     %xmm4, %xmm7                                  #5560.11
        pand      %xmm5, %xmm3                                  #5564.11
        movdqa    %xmm13, %xmm10                                #5577.11
        paddq     %xmm7, %xmm3                                  #5569.11
        psrlq     $32, %xmm10                                   #5577.11
        pand      %xmm5, %xmm13                                 #5580.11
        paddq     %xmm3, %xmm10                                 #5578.11
        psllq     $32, %xmm10                                   #5581.17
        pand      %xmm5, %xmm15                                 #5579.11
        paddq     %xmm13, %xmm10                                #5583.17
        movdqu    .L_2il0floatpacket.39(%rip), %xmm4            #5586.15
        movdqa    %xmm10, %xmm7                                 #5589.18
        movdqu    .L_2il0floatpacket.40(%rip), %xmm12           #5588.20
        pand      %xmm1, %xmm4                                  #5586.15
        movups    .L_2il0floatpacket.41(%rip), %xmm14           #5592.15
        psrlq     $12, %xmm7                                    #5589.18
        pxor      %xmm4, %xmm12                                 #5588.20
        movaps    %xmm14, %xmm9                                 #5593.19
        por       %xmm12, %xmm7                                 #5590.18
        psllq     $32, %xmm0                                    #5582.17
        addpd     %xmm7, %xmm9                                  #5593.19
        paddq     %xmm15, %xmm0                                 #5584.17
        movaps    %xmm9, %xmm5                                  #5594.9
        subpd     %xmm14, %xmm5                                 #5594.9
        movdqu    .L_2il0floatpacket.46(%rip), %xmm3            #5611.18
        movdqu    .L_2il0floatpacket.44(%rip), %xmm8            #5602.18
        pand      %xmm10, %xmm3                                 #5611.18
        movdqu    .L_2il0floatpacket.43(%rip), %xmm6            #5600.20
        pand      %xmm0, %xmm8                                  #5602.18
        psllq     $40, %xmm3                                    #5612.18
        psrlq     $24, %xmm0                                    #5613.17
        pxor      %xmm4, %xmm6                                  #5600.20
        por       %xmm0, %xmm3                                  #5614.18
        pxor      .L_2il0floatpacket.45(%rip), %xmm4            #5609.20
        psllq     $28, %xmm8                                    #5603.18
        subpd     %xmm5, %xmm7                                  #5595.17
        por       %xmm4, %xmm3                                  #5615.18
        movaps    %xmm7, %xmm0                                  #5619.11
        subpd     %xmm4, %xmm3                                  #5618.18
        addpd     %xmm3, %xmm0                                  #5619.11
        subpd     %xmm0, %xmm7                                  #5620.17
        por       %xmm6, %xmm8                                  #5604.18
        subpd     %xmm6, %xmm8                                  #5607.17
        addpd     %xmm7, %xmm3                                  #5621.18
        movups    .L_2il0floatpacket.50(%rip), %xmm10           #5627.16
        andps     %xmm0, %xmm10                                 #5627.16
        addpd     %xmm3, %xmm8                                  #5622.11
        subpd     %xmm10, %xmm0                                 #5628.17
        mulpd     .L_2il0floatpacket.47(%rip), %xmm8            #5632.12
        movups    .L_2il0floatpacket.48(%rip), %xmm15           #5624.16
        movaps    %xmm10, %xmm11                                #5629.16
        movups    .L_2il0floatpacket.49(%rip), %xmm5            #5625.17
        mulpd     %xmm15, %xmm11                                #5629.16
        mulpd     %xmm0, %xmm15                                 #5630.12
        mulpd     %xmm5, %xmm10                                 #5631.12
        mulpd     %xmm0, %xmm5                                  #5633.12
        addpd     %xmm10, %xmm15                                #5634.13
        addpd     %xmm5, %xmm8                                  #5635.13
        addpd     %xmm8, %xmm15                                 #5636.13
        movups    .L_2il0floatpacket.52(%rip), %xmm4            #5641.16
        movaps    %xmm15, %xmm8                                 #5637.13
        movups    .L_2il0floatpacket.51(%rip), %xmm7            #5642.11
        movaps    %xmm4, %xmm6                                  #5643.17
        andps     %xmm1, %xmm7                                  #5642.11
        addpd     %xmm11, %xmm8                                 #5637.13
        cmpltpd   %xmm7, %xmm6                                  #5643.17
        cmplepd   %xmm4, %xmm7                                  #5644.20
        subpd     %xmm8, %xmm11                                 #5638.16
        andps     .L_2il0floatpacket.42(%rip), %xmm9            #5598.13
        andps     %xmm1, %xmm7                                  #5645.24
        movd      %xmm9, %r10d                                  #5650.132
        andps     %xmm6, %xmm8                                  #5646.25
        orps      %xmm8, %xmm7                                  #5647.9
        movaps    %xmm7, %xmm1                                  #5654.26
        movaps    %xmm7, %xmm14                                 #5662.19
        pextrw    $4, %xmm9, %r11d                              #5650.188
        mulpd     %xmm7, %xmm14                                 #5662.19
        addpd     %xmm11, %xmm15                                #5639.13
        shll      $5, %r10d                                     #5650.289
        movaps    %xmm7, %xmm13                                 #5653.23
        shll      $5, %r11d                                     #5650.340
        andps     %xmm15, %xmm6                                 #5648.9
        movq      8(%r10,%rax), %xmm0                           #5651.605
        movhpd    8(%r11,%rax), %xmm0                           #5651.573
        mulpd     %xmm0, %xmm1                                  #5654.26
        movq      16(%r10,%rax), %xmm3                          #5652.603
        movhpd    16(%r11,%rax), %xmm3                          #5652.571
        movaps    %xmm3, %xmm12                                 #5655.26
        movaps    %xmm3, %xmm9                                  #5657.22
        addpd     %xmm1, %xmm12                                 #5655.26
        subpd     %xmm12, %xmm9                                 #5657.22
        addpd     %xmm9, %xmm1                                  #5658.22
        movups    17024+__svml_dsin_ha_data_internal_ha(%rip), %xmm9 #5665.30
        movaps    %xmm12, %xmm5                                 #5656.25
        movups    17216+__svml_dsin_ha_data_internal_ha(%rip), %xmm10 #5672.30
        movq      (%r10,%rax), %xmm4                            #5650.603
        mulpd     %xmm14, %xmm9                                 #5665.30
        mulpd     %xmm14, %xmm10                                #5672.30
        movhpd    (%r11,%rax), %xmm4                            #5650.571
        mulpd     %xmm4, %xmm13                                 #5653.23
        addpd     %xmm0, %xmm4                                  #5668.17
        addpd     %xmm13, %xmm5                                 #5656.25
        movups    16960+__svml_dsin_ha_data_internal_ha(%rip), %xmm8 #5665.18
        movups    17152+__svml_dsin_ha_data_internal_ha(%rip), %xmm11 #5672.18
        addpd     %xmm9, %xmm8                                  #5665.18
        addpd     %xmm10, %xmm11                                #5672.18
        subpd     %xmm5, %xmm12                                 #5659.22
        mulpd     %xmm14, %xmm8                                 #5666.18
        mulpd     %xmm14, %xmm11                                #5674.30
        addpd     %xmm12, %xmm13                                #5660.22
        mulpd     %xmm7, %xmm8                                  #5667.18
        mulpd     %xmm3, %xmm7                                  #5677.36
        mulpd     %xmm4, %xmm8                                  #5669.33
        subpd     %xmm7, %xmm4                                  #5677.17
        addpd     %xmm13, %xmm1                                 #5661.25
        mulpd     %xmm4, %xmm6                                  #5679.32
        addpd     %xmm8, %xmm1                                  #5669.21
        movups    17088+__svml_dsin_ha_data_internal_ha(%rip), %xmm12 #5674.18
        addpd     %xmm11, %xmm12                                #5674.18
        mulpd     %xmm12, %xmm14                                #5675.18
        mulpd     %xmm3, %xmm14                                 #5676.33
        movq      24(%r10,%rax), %xmm3                          #5678.603
        movhpd    24(%r11,%rax), %xmm3                          #5678.571
        addpd     %xmm14, %xmm1                                 #5676.21
        addpd     %xmm6, %xmm3                                  #5679.20
        addpd     %xmm3, %xmm1                                  #5680.21
        addpd     %xmm1, %xmm5                                  #5681.24
        pxor      %xmm2, %xmm5                                  #5682.24
        movups    (%rsp), %xmm2                                 #5683.29[spill]
        movaps    %xmm2, %xmm0                                  #5683.29
        andps     %xmm2, %xmm5                                  #5683.78
        andnps    16(%rsp), %xmm0                               #5683.29[spill]
        movups    32(%rsp), %xmm7                               #5683.18[spill]
        orps      %xmm5, %xmm0                                  #5683.18
        jmp       ..B8.2        # Prob 100%                     #5683.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 edx xmm0 xmm7
	.cfi_endproc
# mark_end;
	.type	__svml_sin2_ha_ex,@function
	.size	__svml_sin2_ha_ex,.-__svml_sin2_ha_ex
..LN__svml_sin2_ha_ex.7:
	.data
# -- End  __svml_sin2_ha_ex
	.text
.L_2__routine_start___svml_sin8_ha_z0_8:
# -- Begin  __svml_sin8_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_sin8_ha_z0
# --- __svml_sin8_ha_z0(__m512d)
__svml_sin8_ha_z0:
# parameter 1: %zmm0
..B9.1:                         # Preds ..B9.0
                                # Execution count [1.00e+00]
        .byte     243                                           #5694.1
        .byte     15                                            #6081.18
        .byte     30                                            #6081.18
        .byte     250                                           #6081.18
	.cfi_startproc
..___tag_value___svml_sin8_ha_z0.230:
..L231:
                                                        #5694.1
        pushq     %rbp                                          #5694.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #5694.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #5694.1
        subq      $192, %rsp                                    #5694.1
        xorl      %esi, %esi                                    #5775.5
        vmovups   192+__svml_dsin_ha_data_internal(%rip), %zmm9 #5780.52
        vmovups   128+__svml_dsin_ha_data_internal(%rip), %zmm7 #5781.13
        vmovups   256+__svml_dsin_ha_data_internal(%rip), %zmm10 #5788.51
        vmovups   320+__svml_dsin_ha_data_internal(%rip), %zmm1 #5789.51
        vmovups   384+__svml_dsin_ha_data_internal(%rip), %zmm15 #5790.51
        vmovups   64+__svml_dsin_ha_data_internal(%rip), %zmm5  #5777.61
        vmovups   640+__svml_dsin_ha_data_internal(%rip), %zmm2 #5794.270
        vmovups   512+__svml_dsin_ha_data_internal(%rip), %zmm3 #5793.270
        vmovups   768+__svml_dsin_ha_data_internal(%rip), %zmm6 #5795.273
        vmovaps   %zmm0, %zmm8                                  #5694.1
        vfmadd213pd {rn-sae}, %zmm9, %zmm8, %zmm7               #5781.13
        vmovups   1024+__svml_dsin_ha_data_internal(%rip), %zmm0 #5805.50
        vpcmpeqq  896+__svml_dsin_ha_data_internal(%rip), %zmm8, %k2 #5787.19
        vsubpd    {rn-sae}, %zmm9, %zmm7, %zmm14                #5784.15
        vandpd    __svml_dsin_ha_data_internal(%rip), %zmm8, %zmm11 #5778.16
        vpermt2pd 704+__svml_dsin_ha_data_internal(%rip), %zmm7, %zmm2 #5794.270
        vpermt2pd 576+__svml_dsin_ha_data_internal(%rip), %zmm7, %zmm3 #5793.270
        vpermt2pd 832+__svml_dsin_ha_data_internal(%rip), %zmm7, %zmm6 #5795.273
        vxorpd    960+__svml_dsin_ha_data_internal(%rip), %zmm7, %zmm7{%k2} #5797.17
        vfnmadd213pd {rn-sae}, %zmm8, %zmm14, %zmm10            #5791.18
        vcmppd    $22, {sae}, %zmm5, %zmm11, %k1                #5782.30
        vpsrlq    $4, %zmm7, %zmm7                              #5831.44
        kmovw     %k1, %eax                                     #5783.75
        vmovaps   %zmm1, %zmm13                                 #5798.18
        vfnmadd213pd {rn-sae}, %zmm10, %zmm14, %zmm13           #5798.18
        vmovaps   %zmm15, %zmm4                                 #5799.17
        vfnmadd213pd {rn-sae}, %zmm13, %zmm14, %zmm4            #5799.17
        vsubpd    {rn-sae}, %zmm13, %zmm10, %zmm12              #5800.21
        vmovups   1088+__svml_dsin_ha_data_internal(%rip), %zmm10 #5807.21
        vmulpd    {rn-sae}, %zmm4, %zmm4, %zmm5                 #5801.21
        vfnmadd231pd {rn-sae}, %zmm14, %zmm1, %zmm12            #5803.18
        vsubpd    {rn-sae}, %zmm13, %zmm4, %zmm13               #5802.21
        vfmadd231pd {rn-sae}, %zmm5, %zmm0, %zmm10              #5807.21
        vmovups   1216+__svml_dsin_ha_data_internal(%rip), %zmm0 #5811.21
        vfmadd213pd {rn-sae}, %zmm13, %zmm15, %zmm14            #5808.19
        vmovups   1152+__svml_dsin_ha_data_internal(%rip), %zmm15 #5809.50
        vmovups   1280+__svml_dsin_ha_data_internal(%rip), %zmm13 #5813.50
        vsubpd    {rn-sae}, %zmm14, %zmm12, %zmm14              #5815.20
        vfmadd231pd {rn-sae}, %zmm5, %zmm15, %zmm0              #5811.21
        vfmadd213pd {rn-sae}, %zmm13, %zmm5, %zmm10             #5814.21
        vmovups   1408+__svml_dsin_ha_data_internal(%rip), %zmm15 #5820.50
        vmovups   1344+__svml_dsin_ha_data_internal(%rip), %zmm12 #5816.50
        vpsllq    $32, %zmm6, %zmm13                            #5819.39
        vfmadd213pd {rn-sae}, %zmm15, %zmm5, %zmm10             #5821.21
        vfmadd213pd {rn-sae}, %zmm12, %zmm5, %zmm0              #5817.21
        vmovups   1472+__svml_dsin_ha_data_internal(%rip), %zmm15 #5823.50
        vmovaps   %zmm2, %zmm9                                  #5804.20
        vfmadd213pd {rn-sae}, %zmm3, %zmm4, %zmm9               #5804.20
        vfmadd213pd {rn-sae}, %zmm15, %zmm5, %zmm0              #5824.21
        vsubpd    {rn-sae}, %zmm3, %zmm9, %zmm1                 #5812.23
        vmovaps   %zmm3, %zmm12                                 #5822.23
        vfmsub231pd {rn-sae}, %zmm2, %zmm4, %zmm1               #5818.21
        vfnmadd213pd {rn-sae}, %zmm2, %zmm4, %zmm12             #5822.23
        vmulpd    {rn-sae}, %zmm4, %zmm2, %zmm2                 #5826.21
        vmulpd    {rn-sae}, %zmm10, %zmm3, %zmm3                #5828.26
        vfmadd213pd {rn-sae}, %zmm1, %zmm4, %zmm6               #5825.19
        vfmadd213pd {rn-sae}, %zmm13, %zmm14, %zmm12            #5827.18
        vpsllq    $63, %zmm7, %zmm1                             #5832.44
        vfmadd213pd {rn-sae}, %zmm3, %zmm2, %zmm0               #5830.20
        vaddpd    {rn-sae}, %zmm12, %zmm6, %zmm4                #5829.21
        vfmadd213pd {rn-sae}, %zmm4, %zmm5, %zmm0               #5833.20
        vaddpd    {rn-sae}, %zmm0, %zmm9, %zmm6                 #5834.22
        vxorpd    %zmm1, %zmm6, %zmm0                           #5835.18
        testl     %eax, %eax                                    #5836.66
        jne       ..B9.17       # Prob 5%                       #5836.66
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm8 zmm11 k1
..B9.2:                         # Preds ..B9.17 ..B9.1
                                # Execution count [1.00e+00]
        testl     %esi, %esi                                    #6084.52
        jne       ..B9.4        # Prob 5%                       #6084.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm8
..B9.3:                         # Preds ..B9.10 ..B9.8 ..B9.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #6087.12
        popq      %rbp                                          #6087.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #6087.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B9.4:                         # Preds ..B9.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #6084.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm8
..B9.5:                         # Preds ..B9.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #6084.249
        movl      %edx, %eax                                    #6084.303
        orl       $8064, %eax                                   #6084.303
        cmpl      %eax, %edx                                    #6084.332
        je        ..B9.7        # Prob 78%                      #6084.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm8
..B9.6:                         # Preds ..B9.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #6084.344
        vldmxcsr  32(%rsp)                                      #6084.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm8
..B9.7:                         # Preds ..B9.6 ..B9.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm8, 64(%rsp)                               #6084.406
        vmovups   %zmm0, 128(%rsp)                              #6084.481
        testl     %esi, %esi                                    #6084.587
        jne       ..B9.12       # Prob 5%                       #6084.587
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B9.8:                         # Preds ..B9.15 ..B9.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #6084.930
        je        ..B9.3        # Prob 78%                      #6084.930
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B9.9:                         # Preds ..B9.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #6084.955
        movl      32(%rsp), %eax                                #6084.955
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.10:                        # Preds ..B9.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #6084.942
        orl       %edx, %eax                                    #6084.942
        movl      %eax, 32(%rsp)                                #6084.942
        vldmxcsr  32(%rsp)                                      #6084.942
        jmp       ..B9.3        # Prob 100%                     #6084.942
                                # LOE rbx r12 r13 r14 r15 zmm0
..B9.12:                        # Preds ..B9.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #6084.667
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B9.20:                        # Preds ..B9.12
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
..B9.13:                        # Preds ..B9.14 ..B9.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #6084.730
        jc        ..B9.16       # Prob 5%                       #6084.730
                                # LOE rbx r15 r12d r13d r14d
..B9.14:                        # Preds ..B9.16 ..B9.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #6084.683
        cmpl      $8, %ebx                                      #6084.678
        jl        ..B9.13       # Prob 82%                      #6084.678
                                # LOE rbx r15 r12d r13d r14d
..B9.15:                        # Preds ..B9.14
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
        vmovups   128(%rsp), %zmm0                              #6084.886
        jmp       ..B9.8        # Prob 100%                     #6084.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B9.16:                        # Preds ..B9.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,8), %rdi                         #6084.759
        lea       128(%rsp,%rbx,8), %rsi                        #6084.759
..___tag_value___svml_sin8_ha_z0.258:
#       __svml_dsin_ha_cout_rare_internal(const double *, double *)
        call      __svml_dsin_ha_cout_rare_internal             #6084.759
..___tag_value___svml_sin8_ha_z0.259:
        jmp       ..B9.14       # Prob 100%                     #6084.759
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B9.17:                        # Preds ..B9.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   16512+__svml_dsin_ha_data_internal_ha(%rip), %zmm1 #5841.56
        lea       __svml_dsin_ha_reduction_data_internal(%rip), %rax #5933.406
        vmovups   %zmm8, 64(%rsp)                               #[spill]
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vandpd    %zmm11, %zmm1, %zmm6                          #5842.20
        vxorpd    %zmm8, %zmm11, %zmm8                          #5840.17
        vpbroadcastq .L_2il0floatpacket.73(%rip), %zmm10        #5843.71
        vcmppd    $4, {sae}, %zmm1, %zmm6, %k2                  #5843.122
        vmovups   %zmm8, 128(%rsp)                              #5840.17[spill]
        vmovaps   %zmm10, %zmm5                                 #5843.46
        lea       __svml_dsin_ha_data_internal_ha(%rip), %rdx   #6050.210
        vpandq    .L_2il0floatpacket.74(%rip){1to8}, %zmm11, %zmm4 #5931.16
        vpandnq   %zmm6, %zmm6, %zmm5{%k2}                      #5843.46
        vpsrlq    $52, %zmm4, %zmm3                             #5932.18
        vcmppd    $3, {sae}, %zmm5, %zmm5, %k0                  #5845.37
        vpsllq    $1, %zmm3, %zmm15                             #5933.134
        vpbroadcastq .L_2il0floatpacket.77(%rip), %zmm6         #5943.15
        kxnorw    %k0, %k0, %k4                                 #5934.407
        kxnorw    %k0, %k0, %k5                                 #5935.406
        kxnorw    %k0, %k0, %k3                                 #5933.406
        kxnorw    %k0, %k0, %k2                                 #6050.210
        kmovw     %k0, %esi                                     #5845.37
        vpaddq    %zmm3, %zmm15, %zmm2                          #5933.194
        vpsllq    $3, %zmm2, %zmm14                             #5933.262
        vpmovqd   %zmm14, %ymm12                                #5933.355
        vpxord    %zmm5, %zmm5, %zmm5                           #5934.407
        vpxord    %zmm4, %zmm4, %zmm4                           #5935.406
        vgatherdpd 8(%rax,%ymm12), %zmm5{%k4}                   #5934.407
        vgatherdpd 16(%rax,%ymm12), %zmm4{%k5}                  #5935.406
        kxnorw    %k0, %k0, %k4                                 #6052.210
        kxnorw    %k0, %k0, %k5                                 #6076.210
        vpsrlq    $32, %zmm5, %zmm9                             #5946.12
        vpsrlq    $32, %zmm4, %zmm13                            #5948.12
        vpxord    %zmm1, %zmm1, %zmm1                           #5933.406
        vgatherdpd (%rax,%ymm12), %zmm1{%k3}                    #5933.406
        kxnorw    %k0, %k0, %k3                                 #6051.212
        vpsrlq    $32, %zmm1, %zmm7                             #5944.12
        vpandq    .L_2il0floatpacket.75(%rip){1to8}, %zmm11, %zmm8 #5941.19
        vpaddq    .L_2il0floatpacket.76(%rip){1to8}, %zmm8, %zmm15 #5942.19
        vpsrlq    $32, %zmm15, %zmm8                            #5950.12
        vpmullq   %zmm9, %zmm8, %zmm3                           #5954.13
        vpandq    %zmm6, %zmm1, %zmm0                           #5945.10
        vpandq    %zmm6, %zmm5, %zmm12                          #5947.10
        vpandq    %zmm6, %zmm4, %zmm5                           #5949.10
        vpandq    %zmm6, %zmm15, %zmm14                         #5951.10
        vpmullq   %zmm0, %zmm8, %zmm4                           #5953.13
        vpmullq   %zmm12, %zmm8, %zmm2                          #5955.13
        vpmullq   %zmm13, %zmm8, %zmm1                          #5956.13
        vpmullq   %zmm5, %zmm8, %zmm8                           #5957.13
        vpmullq   %zmm0, %zmm14, %zmm5                          #5959.13
        vpmullq   %zmm12, %zmm14, %zmm0                         #5961.13
        vpmullq   %zmm13, %zmm14, %zmm13                        #5962.13
        vpmullq   %zmm7, %zmm14, %zmm7                          #5958.13
        vpmullq   %zmm9, %zmm14, %zmm9                          #5960.13
        vpsrlq    $32, %zmm13, %zmm14                           #5964.13
        vpsrlq    $32, %zmm0, %zmm13                            #5965.13
        vpsrlq    $32, %zmm5, %zmm12                            #5967.13
        vpsrlq    $32, %zmm9, %zmm15                            #5966.13
        vpaddq    %zmm14, %zmm1, %zmm1                          #5968.11
        vpaddq    %zmm13, %zmm2, %zmm14                         #5969.11
        vpaddq    %zmm12, %zmm4, %zmm2                          #5971.11
        vpaddq    %zmm15, %zmm3, %zmm13                         #5970.11
        vpandq    %zmm6, %zmm7, %zmm7                           #5975.11
        vpaddq    %zmm2, %zmm7, %zmm4                           #5980.11
        vpsrlq    $32, %zmm8, %zmm2                             #5982.13
        vpandq    %zmm6, %zmm0, %zmm0                           #5972.11
        vpaddq    %zmm1, %zmm0, %zmm1                           #5977.11
        vpaddq    %zmm1, %zmm2, %zmm15                          #5983.11
        vpsrlq    $32, %zmm15, %zmm8                            #5984.13
        vpandq    %zmm6, %zmm9, %zmm9                           #5973.11
        vpaddq    %zmm14, %zmm9, %zmm0                          #5978.11
        vpaddq    %zmm0, %zmm8, %zmm12                          #5985.11
        vpbroadcastq .L_2il0floatpacket.80(%rip), %zmm0         #6003.36
        vpsrlq    $32, %zmm12, %zmm7                            #5986.13
        vpandq    %zmm6, %zmm5, %zmm3                           #5974.11
        vpaddq    %zmm13, %zmm3, %zmm5                          #5979.11
        vpaddq    %zmm5, %zmm7, %zmm3                           #5987.11
        vpsllq    $32, %zmm12, %zmm7                            #5993.19
        vpsrlq    $32, %zmm3, %zmm9                             #5988.13
        vpaddq    %zmm4, %zmm9, %zmm2                           #5989.11
        vpandq    %zmm6, %zmm15, %zmm13                         #5990.11
        vpandq    %zmm6, %zmm3, %zmm8                           #5991.11
        vpsllq    $32, %zmm2, %zmm6                             #5992.19
        vpaddq    %zmm13, %zmm7, %zmm4                          #5995.17
        vpbroadcastq .L_2il0floatpacket.89(%rip), %zmm13        #6041.37
        vpaddq    %zmm8, %zmm6, %zmm15                          #5994.17
        vpsrlq    $12, %zmm15, %zmm1                            #6000.20
        vpandq    .L_2il0floatpacket.78(%rip){1to8}, %zmm11, %zmm3 #5997.15
        vpxorq    .L_2il0floatpacket.79(%rip){1to8}, %zmm3, %zmm6 #5999.20
        vporq     %zmm6, %zmm1, %zmm5                           #6001.18
        vaddpd    {rn-sae}, %zmm5, %zmm0, %zmm2                 #6004.21
        vsubpd    {rn-sae}, %zmm0, %zmm2, %zmm9                 #6005.11
        vsubpd    {rn-sae}, %zmm9, %zmm5, %zmm9                 #6006.19
        vpandq    .L_2il0floatpacket.83(%rip){1to8}, %zmm4, %zmm12 #6013.18
        vpsllq    $28, %zmm12, %zmm14                           #6014.20
        vpsrlq    $24, %zmm4, %zmm4                             #6024.19
        vpandq    .L_2il0floatpacket.85(%rip){1to8}, %zmm15, %zmm15 #6022.18
        vpxorq    .L_2il0floatpacket.82(%rip){1to8}, %zmm3, %zmm7 #6011.20
        vpxorq    .L_2il0floatpacket.84(%rip){1to8}, %zmm3, %zmm1 #6020.20
        vpsllq    $40, %zmm15, %zmm3                            #6023.20
        vporq     %zmm7, %zmm14, %zmm8                          #6015.18
        vsubpd    {rn-sae}, %zmm7, %zmm8, %zmm5                 #6018.19
        vandpd    .L_2il0floatpacket.88(%rip){1to8}, %zmm11, %zmm14 #6042.11
        vcmppd    $22, {sae}, %zmm13, %zmm14, %k7               #6044.117
        vcmppd    $26, {sae}, %zmm13, %zmm14, %k6               #6043.114
        vporq     %zmm4, %zmm3, %zmm8                           #6025.18
        vporq     %zmm1, %zmm8, %zmm7                           #6026.18
        vsubpd    {rn-sae}, %zmm1, %zmm7, %zmm6                 #6029.20
        vpbroadcastq .L_2il0floatpacket.86(%rip), %zmm4         #6034.37
        vaddpd    {rn-sae}, %zmm6, %zmm9, %zmm12                #6030.13
        vmulpd    {rn-sae}, %zmm4, %zmm12, %zmm15               #6036.15
        vsubpd    {rn-sae}, %zmm12, %zmm9, %zmm9                #6031.19
        vmovaps   %zmm4, %zmm3                                  #6037.13
        vfmsub213pd {rn-sae}, %zmm15, %zmm12, %zmm3             #6037.13
        vmovaps   %zmm10, %zmm1                                 #6043.38
        vpandq    .L_2il0floatpacket.81(%rip){1to8}, %zmm2, %zmm0 #6009.13
        vaddpd    {rn-sae}, %zmm9, %zmm6, %zmm2                 #6032.20
        vaddpd    {rn-sae}, %zmm5, %zmm2, %zmm6                 #6033.13
        vpbroadcastq .L_2il0floatpacket.87(%rip), %zmm5         #6035.38
        vfmadd213pd {rn-sae}, %zmm3, %zmm5, %zmm12              #6038.13
        vfmadd213pd {rn-sae}, %zmm12, %zmm4, %zmm6              #6039.13
        vmovups   16960+__svml_dsin_ha_data_internal_ha(%rip), %zmm4 #6062.50
        vpandnq   %zmm14, %zmm14, %zmm10{%k7}                   #6044.41
        vandpd    %zmm11, %zmm10, %zmm11                        #6045.24
        vpsllq    $5, %zmm0, %zmm10                             #6050.78
        vpandnq   %zmm14, %zmm14, %zmm1{%k6}                    #6043.38
        vandpd    %zmm15, %zmm1, %zmm8                          #6046.25
        vandpd    %zmm6, %zmm1, %zmm1                           #6048.9
        vpmovqd   %zmm10, %ymm6                                 #6050.160
        vorpd     %zmm8, %zmm11, %zmm7                          #6047.9
        vmovups   17216+__svml_dsin_ha_data_internal_ha(%rip), %zmm14 #6068.50
        vmovups   17088+__svml_dsin_ha_data_internal_ha(%rip), %zmm15 #6071.50
        vmulpd    {rn-sae}, %zmm7, %zmm7, %zmm11                #6060.21
        vmovaps   %zmm7, %zmm0                                  #6053.26
        vmovaps   %zmm7, %zmm8                                  #6054.25
        vpxord    %zmm12, %zmm12, %zmm12                        #6051.212
        vpxord    %zmm10, %zmm10, %zmm10                        #6052.210
        vgatherdpd 8(%rdx,%ymm6), %zmm12{%k3}                   #6051.212
        vgatherdpd 16(%rdx,%ymm6), %zmm10{%k4}                  #6052.210
        vfmadd213pd {rn-sae}, %zmm10, %zmm12, %zmm0             #6053.26
        vsubpd    {rn-sae}, %zmm0, %zmm10, %zmm3                #6055.24
        vfmadd231pd {rn-sae}, %zmm7, %zmm12, %zmm3              #6056.22
        vpxord    %zmm9, %zmm9, %zmm9                           #6050.210
        vgatherdpd (%rdx,%ymm6), %zmm9{%k2}                     #6050.210
        vfmadd132pd {rn-sae}, %zmm9, %zmm0, %zmm8               #6054.25
        vsubpd    {rn-sae}, %zmm8, %zmm0, %zmm2                 #6057.24
        vmovups   17024+__svml_dsin_ha_data_internal_ha(%rip), %zmm0 #6061.50
        vfmadd231pd {rn-sae}, %zmm9, %zmm7, %zmm2               #6058.22
        vfmadd231pd {rn-sae}, %zmm11, %zmm0, %zmm4              #6063.18
        vaddpd    {rn-sae}, %zmm3, %zmm2, %zmm13                #6059.27
        vaddpd    {rn-sae}, %zmm12, %zmm9, %zmm2                #6066.19
        vmulpd    {rn-sae}, %zmm11, %zmm4, %zmm5                #6064.20
        vmulpd    {rn-sae}, %zmm7, %zmm5, %zmm0                 #6065.20
        vfnmadd213pd {rn-sae}, %zmm2, %zmm10, %zmm7             #6075.17
        vfmadd213pd {rn-sae}, %zmm13, %zmm2, %zmm0              #6067.21
        vmovups   17152+__svml_dsin_ha_data_internal_ha(%rip), %zmm13 #6069.50
        vfmadd231pd {rn-sae}, %zmm11, %zmm14, %zmm13            #6070.18
        vfmadd213pd {rn-sae}, %zmm15, %zmm11, %zmm13            #6072.18
        vmulpd    {rn-sae}, %zmm11, %zmm13, %zmm3               #6073.20
        vfmadd213pd {rn-sae}, %zmm0, %zmm10, %zmm3              #6074.21
        vmovups   (%rsp), %zmm0                                 #6081.18[spill]
        vpxord    %zmm10, %zmm10, %zmm10                        #6076.210
        vgatherdpd 24(%rdx,%ymm6), %zmm10{%k5}                  #6076.210
        vfmadd213pd {rn-sae}, %zmm10, %zmm7, %zmm1              #6077.20
        vaddpd    {rn-sae}, %zmm1, %zmm3, %zmm7                 #6078.23
        vaddpd    {rn-sae}, %zmm7, %zmm8, %zmm11                #6079.22
        vmovups   64(%rsp), %zmm8                               #6081.18[spill]
        vxorpd    128(%rsp), %zmm11, %zmm0{%k1}                 #6081.18[spill]
        jmp       ..B9.2        # Prob 100%                     #6081.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm8
	.cfi_endproc
# mark_end;
	.type	__svml_sin8_ha_z0,@function
	.size	__svml_sin8_ha_z0,.-__svml_sin8_ha_z0
..LN__svml_sin8_ha_z0.8:
	.data
# -- End  __svml_sin8_ha_z0
	.text
.L_2__routine_start___svml_dsin_ha_cout_rare_internal_9:
# -- Begin  __svml_dsin_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_dsin_ha_cout_rare_internal
	.globl __svml_dsin_ha_cout_rare_internal
# --- __svml_dsin_ha_cout_rare_internal(const double *, double *)
__svml_dsin_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B10.1:                        # Preds ..B10.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2743.1
        .byte     15                                            #2762.12
        .byte     30                                            #2762.12
        .byte     250                                           #2762.12
	.cfi_startproc
..___tag_value___svml_dsin_ha_cout_rare_internal.271:
..L272:
                                                        #2743.1
        movzwl    6(%rdi), %eax                                 #2748.32
        andl      $32752, %eax                                  #2748.32
        movsd     (%rdi), %xmm1                                 #2746.14
        movb      7(%rdi), %dl                                  #2746.14
        andb      $127, %dl                                     #2747.26
        movsd     %xmm1, -8(%rsp)                               #2746.5
        cmpl      $32752, %eax                                  #2748.58
        jne       ..B10.6       # Prob 67%                      #2748.58
                                # LOE rbx rbp rsi r12 r13 r14 r15 dl xmm1
..B10.2:                        # Preds ..B10.1
                                # Execution count [3.29e-01]
        cmpl      $0, -8(%rsp)                                  #2750.88
        jne       ..B10.5       # Prob 28%                      #2750.88
                                # LOE rbx rbp rsi r12 r13 r14 r15 dl xmm1
..B10.3:                        # Preds ..B10.2
                                # Execution count [2.37e-01]
        movb      %dl, -1(%rsp)                                 #2747.26
        cmpl      $2146435072, -4(%rsp)                         #2750.196
        jne       ..B10.5       # Prob 50%                      #2750.196
                                # LOE rbx rbp rsi r12 r13 r14 r15 xmm1
..B10.4:                        # Preds ..B10.3
                                # Execution count [1.18e-01]
        movsd     _vmldSinHATab(%rip), %xmm0                    #2752.42
        movl      $1, %eax                                      #2754.20
        mulsd     %xmm0, %xmm1                                  #2752.42
        movsd     %xmm1, (%rsi)                                 #2752.15
        ret                                                     #2754.20
                                # LOE
..B10.5:                        # Preds ..B10.3 ..B10.2
                                # Execution count [2.10e-01]
        mulsd     %xmm1, %xmm1                                  #2758.29
        xorl      %eax, %eax                                    #2759.20
        movsd     %xmm1, (%rsi)                                 #2758.15
        ret                                                     #2759.20
                                # LOE
..B10.6:                        # Preds ..B10.1
                                # Execution count [6.71e-01]
        xorl      %eax, %eax                                    #2762.12
        ret                                                     #2762.12
        .align    16,0x90
                                # LOE
	.cfi_endproc
# mark_end;
	.type	__svml_dsin_ha_cout_rare_internal,@function
	.size	__svml_dsin_ha_cout_rare_internal,.-__svml_dsin_ha_cout_rare_internal
..LN__svml_dsin_ha_cout_rare_internal.9:
	.data
# -- End  __svml_dsin_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
	.hidden __svml_dsin_ha_reduction_data_internal
	.globl __svml_dsin_ha_reduction_data_internal
__svml_dsin_ha_reduction_data_internal:
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.type	__svml_dsin_ha_reduction_data_internal,@object
	.size	__svml_dsin_ha_reduction_data_internal,49152
	.space 512, 0x00 	# pad
	.align 64
	.hidden __svml_dsin_ha_data_internal_ha
	.globl __svml_dsin_ha_data_internal_ha
__svml_dsin_ha_data_internal_ha:
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
	.type	__svml_dsin_ha_data_internal_ha,@object
	.size	__svml_dsin_ha_data_internal_ha,17280
	.align 64
	.hidden __svml_dsin_ha_data_internal
	.globl __svml_dsin_ha_data_internal
__svml_dsin_ha_data_internal:
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
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
	.long	16
	.long	0
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
	.type	__svml_dsin_ha_data_internal,@object
	.size	__svml_dsin_ha_data_internal,1536
	.align 32
.L_2il0floatpacket.53:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,32
	.align 32
.L_2il0floatpacket.54:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,32
	.align 32
.L_2il0floatpacket.55:
	.long	0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,32
	.align 32
.L_2il0floatpacket.56:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,32
	.align 32
.L_2il0floatpacket.57:
	.long	0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,32
	.align 32
.L_2il0floatpacket.58:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,32
	.align 32
.L_2il0floatpacket.59:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,32
	.align 32
.L_2il0floatpacket.60:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,32
	.align 32
.L_2il0floatpacket.61:
	.long	0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,32
	.align 32
.L_2il0floatpacket.62:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,32
	.align 32
.L_2il0floatpacket.63:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,32
	.align 32
.L_2il0floatpacket.64:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,32
	.align 32
.L_2il0floatpacket.65:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,32
	.align 32
.L_2il0floatpacket.66:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,32
	.align 32
.L_2il0floatpacket.67:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,32
	.align 32
.L_2il0floatpacket.68:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,32
	.align 32
.L_2il0floatpacket.69:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,32
	.align 32
.L_2il0floatpacket.70:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,32
	.align 32
.L_2il0floatpacket.71:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,32
	.align 16
.L_2il0floatpacket.35:
	.long	0x00000000,0x7ff00000,0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,16
	.align 16
.L_2il0floatpacket.36:
	.long	0xffffffff,0x000fffff,0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,16
	.align 16
.L_2il0floatpacket.37:
	.long	0x00000000,0x00100000,0x00000000,0x00100000
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,16
	.align 16
.L_2il0floatpacket.38:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,16
	.align 16
.L_2il0floatpacket.39:
	.long	0x00000000,0x80000000,0x00000000,0x80000000
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,16
	.align 16
.L_2il0floatpacket.40:
	.long	0x00000000,0x3ff00000,0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,16
	.align 16
.L_2il0floatpacket.41:
	.long	0x00000000,0x42a80000,0x00000000,0x42a80000
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,16
	.align 16
.L_2il0floatpacket.42:
	.long	0x000001ff,0x00000000,0x000001ff,0x00000000
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,16
	.align 16
.L_2il0floatpacket.43:
	.long	0x00000000,0x39700000,0x00000000,0x39700000
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,16
	.align 16
.L_2il0floatpacket.44:
	.long	0x00ffffff,0x00000000,0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,16
	.align 16
.L_2il0floatpacket.45:
	.long	0x00000000,0x3cb00000,0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,16
	.align 16
.L_2il0floatpacket.46:
	.long	0x00000fff,0x00000000,0x00000fff,0x00000000
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,16
	.align 16
.L_2il0floatpacket.47:
	.long	0x54442d18,0x401921fb,0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,16
	.align 16
.L_2il0floatpacket.48:
	.long	0x54000000,0x401921fb,0x54000000,0x401921fb
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,16
	.align 16
.L_2il0floatpacket.49:
	.long	0x11a62633,0x3e310b46,0x11a62633,0x3e310b46
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,16
	.align 16
.L_2il0floatpacket.50:
	.long	0xf8000000,0xffffffff,0xf8000000,0xffffffff
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,16
	.align 16
.L_2il0floatpacket.51:
	.long	0xffffffff,0x7fffffff,0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,16
	.align 16
.L_2il0floatpacket.52:
	.long	0x00000000,0x3eb00000,0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,16
	.align 16
.L_2il0floatpacket.72:
	.long	0x33145c07,0x3cb1a626,0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,16
	.align 8
.L_2il0floatpacket.73:
	.long	0xffffffff,0xffffffff
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,8
	.align 8
.L_2il0floatpacket.74:
	.long	0x00000000,0x7ff00000
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,8
	.align 8
.L_2il0floatpacket.75:
	.long	0xffffffff,0x000fffff
	.type	.L_2il0floatpacket.75,@object
	.size	.L_2il0floatpacket.75,8
	.align 8
.L_2il0floatpacket.76:
	.long	0x00000000,0x00100000
	.type	.L_2il0floatpacket.76,@object
	.size	.L_2il0floatpacket.76,8
	.align 8
.L_2il0floatpacket.77:
	.long	0xffffffff,0x00000000
	.type	.L_2il0floatpacket.77,@object
	.size	.L_2il0floatpacket.77,8
	.align 8
.L_2il0floatpacket.78:
	.long	0x00000000,0x80000000
	.type	.L_2il0floatpacket.78,@object
	.size	.L_2il0floatpacket.78,8
	.align 8
.L_2il0floatpacket.79:
	.long	0x00000000,0x3ff00000
	.type	.L_2il0floatpacket.79,@object
	.size	.L_2il0floatpacket.79,8
	.align 8
.L_2il0floatpacket.80:
	.long	0x00000000,0x42a80000
	.type	.L_2il0floatpacket.80,@object
	.size	.L_2il0floatpacket.80,8
	.align 8
.L_2il0floatpacket.81:
	.long	0x000001ff,0x00000000
	.type	.L_2il0floatpacket.81,@object
	.size	.L_2il0floatpacket.81,8
	.align 8
.L_2il0floatpacket.82:
	.long	0x00000000,0x39700000
	.type	.L_2il0floatpacket.82,@object
	.size	.L_2il0floatpacket.82,8
	.align 8
.L_2il0floatpacket.83:
	.long	0x00ffffff,0x00000000
	.type	.L_2il0floatpacket.83,@object
	.size	.L_2il0floatpacket.83,8
	.align 8
.L_2il0floatpacket.84:
	.long	0x00000000,0x3cb00000
	.type	.L_2il0floatpacket.84,@object
	.size	.L_2il0floatpacket.84,8
	.align 8
.L_2il0floatpacket.85:
	.long	0x00000fff,0x00000000
	.type	.L_2il0floatpacket.85,@object
	.size	.L_2il0floatpacket.85,8
	.align 8
.L_2il0floatpacket.86:
	.long	0x54442d18,0x401921fb
	.type	.L_2il0floatpacket.86,@object
	.size	.L_2il0floatpacket.86,8
	.align 8
.L_2il0floatpacket.87:
	.long	0x33145c07,0x3cb1a626
	.type	.L_2il0floatpacket.87,@object
	.size	.L_2il0floatpacket.87,8
	.align 8
.L_2il0floatpacket.88:
	.long	0xffffffff,0x7fffffff
	.type	.L_2il0floatpacket.88,@object
	.size	.L_2il0floatpacket.88,8
	.align 8
.L_2il0floatpacket.89:
	.long	0x00000000,0x3eb00000
	.type	.L_2il0floatpacket.89,@object
	.size	.L_2il0floatpacket.89,8
	.align 8
_vmldSinHATab:
	.long	0
	.long	0
	.long	0
	.long	2146435072
	.type	_vmldSinHATab,@object
	.size	_vmldSinHATab,16
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
