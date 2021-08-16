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
	.file "svml_s_tan.c"
	.text
..TXTST0:
.L_2__routine_start___svml_tanf4_ha_ex_0:
# -- Begin  __svml_tanf4_ha_ex
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf4_ha_ex
# --- __svml_tanf4_ha_ex(__m128)
__svml_tanf4_ha_ex:
# parameter 1: %xmm0
..B1.1:                         # Preds ..B1.0
                                # Execution count [1.00e+00]
        .byte     243                                           #1964.1
        .byte     15                                            #2395.22
        .byte     30                                            #2395.22
        .byte     250                                           #2395.22
	.cfi_startproc
..___tag_value___svml_tanf4_ha_ex.1:
..L2:
                                                          #1964.1
        pushq     %rbp                                          #1964.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #1964.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #1964.1
        subq      $256, %rsp                                    #1964.1
        xorl      %eax, %eax                                    #2008.9
        movups    960+__svml_stan_ha_data_internal(%rip), %xmm10 #2009.53
        pxor      %xmm14, %xmm14                                #2024.23
        movaps    %xmm10, %xmm15                                #2010.20
        andnps    %xmm0, %xmm10                                 #2011.21
        andps     %xmm0, %xmm15                                 #2010.20
        movups    832+__svml_stan_ha_data_internal(%rip), %xmm12 #2017.51
        movaps    %xmm15, %xmm11                                #2016.83
        cvtps2pd  %xmm15, %xmm9                                 #2016.26
        movhlps   %xmm15, %xmm11                                #2016.83
        movaps    %xmm15, %xmm3                                 #2013.34
        mulps     %xmm12, %xmm15                                #2019.29
        cvtps2pd  %xmm11, %xmm7                                 #2016.69
        cmpnleps  1216+__svml_stan_ha_data_internal(%rip), %xmm3 #2013.34
        movups    1088+__svml_stan_ha_data_internal(%rip), %xmm13 #2018.54
        movups    1280+__svml_stan_ha_data_internal(%rip), %xmm1 #2028.52
        addps     %xmm13, %xmm15                                #2019.17
        movmskps  %xmm3, %edx                                   #2015.86
        movaps    %xmm15, %xmm8                                 #2020.38
        movaps    %xmm1, %xmm2                                  #2029.43
        movups    1344+__svml_stan_ha_data_internal(%rip), %xmm11 #2030.52
        pslld     $31, %xmm8                                    #2020.38
        pcmpeqd   %xmm8, %xmm14                                 #2024.23
        pxor      %xmm10, %xmm8                                 #2045.20
        pshufd    $80, %xmm14, %xmm6                            #2025.29
        subps     %xmm13, %xmm15                                #2021.17
        cvtps2pd  %xmm15, %xmm4                                 #2027.23
        movhlps   %xmm15, %xmm15                                #2027.74
        cvtps2pd  %xmm15, %xmm12                                #2027.60
        mulpd     %xmm4, %xmm2                                  #2029.43
        mulpd     %xmm12, %xmm1                                 #2029.112
        mulpd     %xmm11, %xmm4                                 #2031.40
        mulpd     %xmm11, %xmm12                                #2031.106
        subpd     %xmm2, %xmm9                                  #2029.19
        subpd     %xmm1, %xmm7                                  #2029.88
        subpd     %xmm4, %xmm9                                  #2031.19
        subpd     %xmm12, %xmm7                                 #2031.85
        movaps    %xmm9, %xmm1                                  #2032.20
        movaps    %xmm7, %xmm4                                  #2032.63
        mulpd     %xmm9, %xmm1                                  #2032.20
        mulpd     %xmm7, %xmm4                                  #2032.63
        movups    1664+__svml_stan_ha_data_internal(%rip), %xmm15 #2036.51
        movaps    %xmm15, %xmm11                                #2038.31
        mulpd     %xmm1, %xmm11                                 #2038.31
        mulpd     %xmm4, %xmm15                                 #2038.99
        movups    1472+__svml_stan_ha_data_internal(%rip), %xmm13 #2033.51
        pshufd    $250, %xmm14, %xmm5                           #2025.105
        movaps    %xmm13, %xmm14                                #2034.19
        mulpd     %xmm9, %xmm14                                 #2034.19
        mulpd     %xmm7, %xmm13                                 #2034.62
        mulpd     %xmm1, %xmm14                                 #2035.31
        mulpd     %xmm4, %xmm13                                 #2035.97
        addpd     %xmm14, %xmm9                                 #2035.19
        addpd     %xmm13, %xmm7                                 #2035.85
        movups    1600+__svml_stan_ha_data_internal(%rip), %xmm2 #2037.51
        movdqa    %xmm6, %xmm14                                 #2041.32
        addpd     %xmm2, %xmm11                                 #2038.19
        addpd     %xmm15, %xmm2                                 #2038.87
        mulpd     %xmm11, %xmm1                                 #2040.31
        mulpd     %xmm2, %xmm4                                  #2040.98
        movups    1536+__svml_stan_ha_data_internal(%rip), %xmm12 #2039.51
        movdqa    %xmm5, %xmm15                                 #2041.135
        addpd     %xmm12, %xmm1                                 #2040.19
        movaps    %xmm12, %xmm2                                 #2040.86
        andnps    %xmm1, %xmm14                                 #2041.32
        addpd     %xmm4, %xmm2                                  #2040.86
        movdqa    %xmm6, %xmm4                                  #2041.73
        movdqa    %xmm5, %xmm11                                 #2041.176
        movdqa    %xmm6, %xmm13                                 #2042.32
        andps     %xmm6, %xmm1                                  #2042.73
        movdqa    %xmm5, %xmm6                                  #2042.135
        andps     %xmm9, %xmm4                                  #2041.73
        andnps    %xmm2, %xmm15                                 #2041.135
        andps     %xmm7, %xmm11                                 #2041.176
        andnps    %xmm9, %xmm13                                 #2042.32
        andnps    %xmm7, %xmm6                                  #2042.135
        andps     %xmm5, %xmm2                                  #2042.176
        orps      %xmm4, %xmm14                                 #2041.21
        orps      %xmm11, %xmm15                                #2041.124
        orps      %xmm1, %xmm13                                 #2042.21
        orps      %xmm2, %xmm6                                  #2042.124
        divpd     %xmm13, %xmm14                                #2043.21
        divpd     %xmm6, %xmm15                                 #2043.69
        cvtpd2ps  %xmm14, %xmm2                                 #2044.35
        cvtpd2ps  %xmm15, %xmm5                                 #2044.66
        movlhps   %xmm5, %xmm2                                  #2044.19
        pxor      %xmm8, %xmm2                                  #2046.18
        testl     %edx, %edx                                    #2047.70
        jne       ..B1.12       # Prob 5%                       #2047.70
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2 xmm3
..B1.2:                         # Preds ..B1.15 ..B1.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2398.52
        jne       ..B1.4        # Prob 5%                       #2398.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2
..B1.3:                         # Preds ..B1.4 ..B1.10 ..B1.2
                                # Execution count [1.00e+00]
        movaps    %xmm2, %xmm0                                  #2401.12
        movq      %rbp, %rsp                                    #2401.12
        popq      %rbp                                          #2401.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2401.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B1.4:                         # Preds ..B1.2
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 64(%rsp)                               #2398.193
        movups    %xmm2, 128(%rsp)                              #2398.264
        je        ..B1.3        # Prob 95%                      #2398.368
                                # LOE rbx r12 r13 r14 r15 eax xmm2
..B1.7:                         # Preds ..B1.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2398.448
        movq      %r12, 8(%rsp)                                 #2398.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #2398.448
        movq      %r13, (%rsp)                                  #2398.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #2398.448
                                # LOE rbx r12 r14 r15 r13d
..B1.8:                         # Preds ..B1.9 ..B1.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2398.511
        jc        ..B1.11       # Prob 5%                       #2398.511
                                # LOE rbx r12 r14 r15 r13d
..B1.9:                         # Preds ..B1.11 ..B1.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2398.464
        cmpl      $4, %r12d                                     #2398.459
        jl        ..B1.8        # Prob 82%                      #2398.459
                                # LOE rbx r12 r14 r15 r13d
..B1.10:                        # Preds ..B1.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm2                              #2398.665
        jmp       ..B1.3        # Prob 100%                     #2398.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x08, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x00, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm2
..B1.11:                        # Preds ..B1.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2398.540
        lea       128(%rsp,%r12,4), %rsi                        #2398.540
..___tag_value___svml_tanf4_ha_ex.19:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #2398.540
..___tag_value___svml_tanf4_ha_ex.20:
        jmp       ..B1.9        # Prob 100%                     #2398.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B1.12:                        # Preds ..B1.1
                                # Execution count [5.00e-02]: Infreq
        movl      $2139095040, %eax                             #2117.49
        movl      $-2147483648, %edx                            #2123.50
        movups    %xmm3, 64(%rsp)                               #[spill]
        movaps    %xmm0, %xmm13                                 #2124.29
        movups    %xmm2, 80(%rsp)                               #[spill]
        movaps    %xmm0, %xmm2                                  #2118.25
        lea       __svml_stan_ha_reduction_data_internal(%rip), %r9 #2213.803
        movl      $8388607, %r10d                               #2219.21
        movd      %eax, %xmm15                                  #2117.49
        movd      %edx, %xmm3                                   #2123.50
        pshufd    $0, %xmm15, %xmm11                            #2117.49
        movl      $8388608, %r11d                               #2220.20
        pshufd    $0, %xmm3, %xmm10                             #2123.50
        andps     %xmm11, %xmm2                                 #2118.25
        movdqu    %xmm10, 96(%rsp)                              #2123.50[spill]
        andps     %xmm10, %xmm13                                #2124.29
        andnps    %xmm0, %xmm10                                 #2125.28
        movl      $65535, %edx                                  #2223.17
        cmpeqps   %xmm11, %xmm2                                 #2120.35
        pand      %xmm10, %xmm11                                #2211.16
        psrld     $23, %xmm11                                   #2212.16
        movdqa    %xmm11, %xmm12                                #2213.232
        pslld     $1, %xmm12                                    #2213.232
        paddd     %xmm11, %xmm12                                #2213.355
        pslld     $2, %xmm12                                    #2213.355
        pshufd    $2, %xmm12, %xmm6                             #2213.608
        pshufd    $3, %xmm12, %xmm5                             #2213.712
        pshufd    $1, %xmm12, %xmm4                             #2213.504
        movd      %xmm6, %edi                                   #2213.589
        movd      %xmm5, %r8d                                   #2213.693
        movd      %xmm12, %ecx                                  #2213.423
        movd      %r11d, %xmm12                                 #2220.20
        movd      %xmm4, %esi                                   #2213.485
        movd      %r10d, %xmm4                                  #2219.21
        movups    %xmm13, (%rsp)                                #2124.29[spill]
        movd      8(%rdi,%r9), %xmm11                           #2215.1517
        movd      8(%r8,%r9), %xmm13                            #2215.1600
        movl      $511, %r10d                                   #2301.20
        movd      (%r8,%r9), %xmm8                              #2213.1600
        movl      $1086918619, %r11d                            #2314.38
        movd      (%rcx,%r9), %xmm9                             #2213.1330
        movd      (%rsi,%r9), %xmm14                            #2213.1413
        movd      (%rdi,%r9), %xmm7                             #2213.1517
        punpcklqdq %xmm13, %xmm11                               #2215.1497
        pshufd    $0, %xmm4, %xmm13                             #2219.21
        punpcklqdq %xmm14, %xmm9                                #2213.1311
        pand      %xmm10, %xmm13                                #2221.19
        punpcklqdq %xmm8, %xmm7                                 #2213.1497
        movd      %edx, %xmm14                                  #2223.17
        pshufd    $0, %xmm12, %xmm5                             #2220.20
        movl      $1086918656, %edx                             #2315.38
        shufps    $136, %xmm7, %xmm9                            #2213.1291
        paddd     %xmm5, %xmm13                                 #2222.19
        pshufd    $0, %xmm14, %xmm8                             #2223.17
        movdqa    %xmm13, %xmm12                                #2230.10
        movups    %xmm10, 112(%rsp)                             #2125.28[spill]
        movaps    %xmm9, %xmm10                                 #2224.10
        pand      %xmm8, %xmm13                                 #2231.10
        psrld     $16, %xmm10                                   #2224.10
        movdqu    %xmm10, 128(%rsp)                             #2224.10[spill]
        pand      %xmm8, %xmm9                                  #2225.10
        movdqa    %xmm13, %xmm10                                #2238.93
        movaps    %xmm9, %xmm14                                 #2233.135
        movd      4(%rcx,%r9), %xmm6                            #2214.1331
        psrlq     $32, %xmm10                                   #2238.93
        movd      4(%rsi,%r9), %xmm15                           #2214.1414
        psrlq     $32, %xmm14                                   #2233.135
        punpcklqdq %xmm15, %xmm6                                #2214.1312
        movdqa    %xmm13, %xmm7                                 #2239.177
        movdqa    %xmm10, %xmm15                                #2239.221
        psrld     $16, %xmm12                                   #2230.10
        movups    %xmm0, 48(%rsp)                               #[spill]
        movd      4(%rdi,%r9), %xmm0                            #2214.1518
        movl      $679477248, %edi                              #2290.22
        movd      4(%r8,%r9), %xmm1                             #2214.1601
        movl      $262143, %r8d                                 #2292.20
        pmuludq   %xmm9, %xmm7                                  #2239.177
        pmuludq   %xmm14, %xmm15                                #2239.221
        movmskps  %xmm2, %eax                                   #2122.56
        punpcklqdq %xmm1, %xmm0                                 #2214.1498
        psllq     $32, %xmm15                                   #2239.283
        shufps    $136, %xmm0, %xmm6                            #2214.1292
        movaps    %xmm6, %xmm0                                  #2226.10
        pand      %xmm8, %xmm6                                  #2227.10
        movups    %xmm9, 144(%rsp)                              #2225.10[spill]
        psrld     $16, %xmm0                                    #2226.10
        movdqu    .L_2il0floatpacket.26(%rip), %xmm9            #2239.251
        movdqa    %xmm0, %xmm1                                  #2234.135
        pand      %xmm9, %xmm7                                  #2239.251
        psrlq     $32, %xmm1                                    #2234.135
        por       %xmm15, %xmm7                                 #2239.318
        movaps    %xmm6, %xmm5                                  #2235.135
        movdqu    %xmm14, 176(%rsp)                             #2233.135[spill]
        movdqa    %xmm10, %xmm14                                #2240.221
        movdqu    %xmm7, 208(%rsp)                              #2239.318[spill]
        movdqa    %xmm13, %xmm7                                 #2240.177
        movdqu    %xmm0, 160(%rsp)                              #2226.10[spill]
        psrlq     $32, %xmm5                                    #2235.135
        movdqu    %xmm1, 192(%rsp)                              #2234.135[spill]
        pmuludq   %xmm0, %xmm7                                  #2240.177
        movdqa    %xmm13, %xmm0                                 #2241.177
        pmuludq   %xmm1, %xmm14                                 #2240.221
        pmuludq   %xmm6, %xmm0                                  #2241.177
        pmuludq   %xmm12, %xmm6                                 #2235.177
        movdqa    %xmm10, %xmm1                                 #2241.221
        pand      %xmm9, %xmm0                                  #2241.251
        pmuludq   %xmm5, %xmm1                                  #2241.221
        pand      %xmm9, %xmm7                                  #2240.251
        movd      8(%rcx,%r9), %xmm3                            #2215.1330
        psllq     $32, %xmm1                                    #2241.283
        movd      8(%rsi,%r9), %xmm2                            #2215.1413
        por       %xmm1, %xmm0                                  #2241.318
        punpcklqdq %xmm2, %xmm3                                 #2215.1311
        movdqa    %xmm12, %xmm1                                 #2237.177
        shufps    $136, %xmm11, %xmm3                           #2215.1291
        movdqa    %xmm12, %xmm11                                #2233.93
        movaps    %xmm3, %xmm2                                  #2228.10
        pand      %xmm8, %xmm3                                  #2229.10
        pmuludq   %xmm3, %xmm1                                  #2237.177
        psrlq     $32, %xmm11                                   #2233.93
        pmuludq   %xmm11, %xmm5                                 #2235.221
        psrlq     $32, %xmm3                                    #2237.135
        psrld     $16, %xmm2                                    #2228.10
        pmuludq   %xmm11, %xmm3                                 #2237.221
        movdqa    %xmm2, %xmm4                                  #2236.135
        psllq     $32, %xmm14                                   #2240.283
        pand      %xmm9, %xmm1                                  #2237.251
        psllq     $32, %xmm3                                    #2237.283
        psrlq     $32, %xmm4                                    #2236.135
        por       %xmm14, %xmm7                                 #2240.318
        por       %xmm3, %xmm1                                  #2237.318
        movdqa    %xmm12, %xmm14                                #2236.177
        movdqa    %xmm11, %xmm3                                 #2236.221
        pmuludq   %xmm2, %xmm14                                 #2236.177
        pand      %xmm9, %xmm6                                  #2235.251
        pmuludq   %xmm4, %xmm3                                  #2236.221
        pmuludq   %xmm13, %xmm2                                 #2242.177
        pmuludq   %xmm10, %xmm4                                 #2242.221
        pand      %xmm9, %xmm2                                  #2242.251
        psllq     $32, %xmm4                                    #2242.283
        psllq     $32, %xmm5                                    #2235.283
        movdqa    %xmm0, %xmm15                                 #2252.11
        por       %xmm4, %xmm2                                  #2242.318
        movdqa    %xmm7, %xmm4                                  #2253.11
        por       %xmm5, %xmm6                                  #2235.318
        psrld     $16, %xmm0                                    #2245.11
        pand      %xmm8, %xmm4                                  #2253.11
        paddd     %xmm0, %xmm6                                  #2249.11
        movdqu    160(%rsp), %xmm0                              #2234.177[spill]
        paddd     %xmm6, %xmm4                                  #2258.11
        movdqu    192(%rsp), %xmm6                              #2234.221[spill]
        pand      %xmm9, %xmm14                                 #2236.251
        pmuludq   %xmm12, %xmm0                                 #2234.177
        psllq     $32, %xmm3                                    #2236.283
        pmuludq   %xmm11, %xmm6                                 #2234.221
        pmuludq   144(%rsp), %xmm12                             #2233.177[spill]
        pmuludq   176(%rsp), %xmm11                             #2233.221[spill]
        por       %xmm3, %xmm14                                 #2236.318
        psrld     $16, %xmm2                                    #2244.11
        pand      %xmm9, %xmm0                                  #2234.251
        psllq     $32, %xmm6                                    #2234.283
        pand      %xmm8, %xmm15                                 #2252.11
        paddd     %xmm2, %xmm14                                 #2248.11
        por       %xmm6, %xmm0                                  #2234.318
        psrld     $16, %xmm1                                    #2262.11
        movdqu    128(%rsp), %xmm6                              #2238.177[spill]
        paddd     %xmm14, %xmm15                                #2257.11
        pmuludq   %xmm6, %xmm13                                 #2238.177
        psrlq     $32, %xmm6                                    #2238.135
        pmuludq   %xmm6, %xmm10                                 #2238.221
        paddd     %xmm15, %xmm1                                 #2263.11
        psrld     $16, %xmm7                                    #2246.11
        movdqa    %xmm1, %xmm14                                 #2264.11
        paddd     %xmm7, %xmm0                                  #2250.11
        movdqu    208(%rsp), %xmm5                              #2254.11[spill]
        psrld     $16, %xmm14                                   #2264.11
        paddd     %xmm4, %xmm14                                 #2265.11
        movdqa    %xmm5, %xmm4                                  #2254.11
        movdqa    %xmm14, %xmm15                                #2266.11
        pand      %xmm8, %xmm4                                  #2254.11
        psrld     $16, %xmm15                                   #2266.11
        paddd     %xmm0, %xmm4                                  #2259.11
        pand      %xmm9, %xmm13                                 #2238.251
        psllq     $32, %xmm10                                   #2238.283
        pand      %xmm9, %xmm12                                 #2233.251
        psllq     $32, %xmm11                                   #2233.283
        paddd     %xmm4, %xmm15                                 #2267.11
        por       %xmm10, %xmm13                                #2238.318
        por       %xmm11, %xmm12                                #2233.318
        psrld     $16, %xmm5                                    #2247.11
        movdqa    %xmm15, %xmm7                                 #2268.11
        pand      %xmm8, %xmm13                                 #2255.11
        paddd     %xmm5, %xmm12                                 #2272.16
        movl      $1065353216, %ecx                             #2278.22
        psrld     $16, %xmm7                                    #2268.11
        paddd     %xmm12, %xmm13                                #2272.16
        paddd     %xmm13, %xmm7                                 #2272.16
        pand      %xmm8, %xmm15                                 #2271.11
        pslld     $16, %xmm7                                    #2272.16
        movl      $1195376640, %esi                             #2283.33
        movdqu    96(%rsp), %xmm11                              #2277.15[spill]
        paddd     %xmm15, %xmm7                                 #2274.16
        pand      %xmm8, %xmm1                                  #2270.11
        movd      %ecx, %xmm9                                   #2278.22
        movups    112(%rsp), %xmm8                              #2277.15[spill]
        movdqa    %xmm7, %xmm12                                 #2280.18
        pshufd    $0, %xmm9, %xmm13                             #2278.22
        pand      %xmm8, %xmm11                                 #2277.15
        psrld     $9, %xmm12                                    #2280.18
        pxor      %xmm11, %xmm13                                #2279.20
        por       %xmm13, %xmm12                                #2281.18
        movd      %esi, %xmm4                                   #2283.33
        pshufd    $0, %xmm4, %xmm6                              #2283.33
        movdqa    %xmm12, %xmm5                                 #2284.19
        movl      $872415232, %r9d                              #2299.22
        movd      %r10d, %xmm3                                  #2301.20
        pshufd    $0, %xmm3, %xmm4                              #2301.20
        pslld     $16, %xmm14                                   #2273.17
        movd      %r8d, %xmm15                                  #2292.20
        paddd     %xmm1, %xmm14                                 #2275.17
        pshufd    $0, %xmm15, %xmm13                            #2292.20
        movd      %edi, %xmm9                                   #2290.22
        movd      %r9d, %xmm1                                   #2299.22
        pand      %xmm7, %xmm4                                  #2302.18
        pshufd    $0, %xmm9, %xmm0                              #2290.22
        pand      %xmm14, %xmm13                                #2293.18
        pshufd    $0, %xmm1, %xmm2                              #2299.22
        pslld     $14, %xmm4                                    #2303.18
        psrld     $18, %xmm14                                   #2304.17
        pxor      %xmm11, %xmm0                                 #2291.20
        pxor      %xmm2, %xmm11                                 #2300.20
        por       %xmm14, %xmm4                                 #2305.18
        por       %xmm11, %xmm4                                 #2306.18
        movl      $-4096, %esi                                  #2317.38
        pslld     $5, %xmm13                                    #2294.18
        movl      $-1214941318, %ecx                            #2316.39
        por       %xmm0, %xmm13                                 #2295.18
        movl      $2147483647, %edi                             #2331.33
        movl      $897581056, %r8d                              #2332.34
        addps     %xmm6, %xmm5                                  #2284.19
        subps     %xmm11, %xmm4                                 #2309.18
        subps     %xmm0, %xmm13                                 #2298.17
        movaps    %xmm5, %xmm10                                 #2285.9
        movd      %esi, %xmm7                                   #2317.38
        pshufd    $0, %xmm7, %xmm15                             #2317.38
        movd      %r11d, %xmm11                                 #2314.38
        movd      %ecx, %xmm14                                  #2316.39
        movd      %r8d, %xmm9                                   #2332.34
        pshufd    $0, %xmm9, %xmm7                              #2332.34
        subps     %xmm6, %xmm10                                 #2285.9
        movl      $255, %r9d                                    #2288.15
        subps     %xmm10, %xmm12                                #2286.17
        movaps    %xmm12, %xmm6                                 #2310.11
        movl      $127, %r10d                                   #2342.41
        pshufd    $0, %xmm11, %xmm10                            #2314.38
        movdqa    %xmm15, %xmm11                                #2318.20
        addps     %xmm4, %xmm6                                  #2310.11
        andps     %xmm6, %xmm11                                 #2318.20
        subps     %xmm6, %xmm12                                 #2311.17
        subps     %xmm11, %xmm6                                 #2319.21
        addps     %xmm12, %xmm4                                 #2312.18
        movd      %edx, %xmm12                                  #2315.38
        addps     %xmm4, %xmm13                                 #2313.11
        pshufd    $0, %xmm12, %xmm4                             #2315.38
        pshufd    $0, %xmm14, %xmm12                            #2316.39
        movdqa    %xmm4, %xmm14                                 #2320.20
        mulps     %xmm11, %xmm14                                #2320.20
        mulps     %xmm6, %xmm4                                  #2321.16
        mulps     %xmm12, %xmm11                                #2322.16
        mulps     %xmm13, %xmm10                                #2323.16
        mulps     %xmm6, %xmm12                                 #2324.16
        addps     %xmm11, %xmm4                                 #2325.17
        addps     %xmm12, %xmm10                                #2326.17
        movd      %edi, %xmm13                                  #2331.33
        movdqa    %xmm7, %xmm11                                 #2334.17
        pshufd    $0, %xmm13, %xmm13                            #2331.33
        movd      %r10d, %xmm6                                  #2342.41
        andps     %xmm8, %xmm13                                 #2333.11
        addps     %xmm10, %xmm4                                 #2327.17
        cmpltps   %xmm13, %xmm11                                #2334.17
        cmpleps   %xmm7, %xmm13                                 #2335.20
        movaps    %xmm4, %xmm0                                  #2328.17
        andps     %xmm8, %xmm13                                 #2336.24
        movd      %r9d, %xmm8                                   #2288.15
        addps     %xmm14, %xmm0                                 #2328.17
        pshufd    $0, %xmm6, %xmm12                             #2342.41
        subps     %xmm0, %xmm14                                 #2329.20
        andps     %xmm11, %xmm0                                 #2337.25
        addps     %xmm14, %xmm4                                 #2330.17
        andps     %xmm4, %xmm11                                 #2339.9
        orps      %xmm0, %xmm13                                 #2338.9
        pshufd    $0, %xmm8, %xmm4                              #2288.15
        lea       1764+__svml_stan_ha_data_internal(%rip), %rdi #2344.817
        pand      %xmm4, %xmm5                                  #2289.13
        pand      %xmm12, %xmm5                                 #2343.29
        movaps    %xmm5, %xmm14                                 #2344.252
        pslld     $2, %xmm14                                    #2344.252
        paddd     %xmm5, %xmm14                                 #2344.369
        pslld     $3, %xmm14                                    #2344.369
        pshufd    $1, %xmm14, %xmm5                             #2344.518
        pshufd    $2, %xmm14, %xmm4                             #2344.622
        pshufd    $3, %xmm14, %xmm6                             #2344.726
        movd      %xmm14, %r11d                                 #2344.437
        movd      %xmm5, %edx                                   #2344.499
        movd      %xmm4, %ecx                                   #2344.603
        movd      %xmm6, %esi                                   #2344.707
        movd      -36(%r11,%rdi), %xmm4                         #2344.1308
        movd      -36(%rdx,%rdi), %xmm12                        #2344.1391
        movd      -36(%rcx,%rdi), %xmm8                         #2344.1495
        movd      -36(%rsi,%rdi), %xmm5                         #2344.1578
        movd      -32(%rsi,%rdi), %xmm9                         #2345.1578
        punpcklqdq %xmm12, %xmm4                                #2344.1289
        punpcklqdq %xmm5, %xmm8                                 #2344.1475
        movd      -32(%r11,%rdi), %xmm6                         #2345.1308
        movd      -32(%rdx,%rdi), %xmm10                        #2345.1391
        movd      -32(%rcx,%rdi), %xmm7                         #2345.1495
        shufps    $136, %xmm8, %xmm4                            #2344.1269
        movd      -24(%rcx,%rdi), %xmm8                         #2347.1496
        punpcklqdq %xmm10, %xmm6                                #2345.1289
        punpcklqdq %xmm9, %xmm7                                 #2345.1475
        movd      -24(%rsi,%rdi), %xmm14                        #2347.1579
        movd      -20(%rsi,%rdi), %xmm9                         #2348.1579
        shufps    $136, %xmm7, %xmm6                            #2345.1269
        punpcklqdq %xmm14, %xmm8                                #2347.1476
        movd      -20(%r11,%rdi), %xmm14                        #2348.1309
        movd      -20(%rdx,%rdi), %xmm10                        #2348.1392
        movd      -20(%rcx,%rdi), %xmm7                         #2348.1496
        movd      -28(%rcx,%rdi), %xmm2                         #2346.1494
        movd      -28(%r11,%rdi), %xmm12                        #2346.1307
        movd      -24(%r11,%rdi), %xmm5                         #2347.1309
        movd      -28(%rdx,%rdi), %xmm0                         #2346.1390
        movd      -24(%rdx,%rdi), %xmm3                         #2347.1392
        movd      -28(%rsi,%rdi), %xmm1                         #2346.1577
        punpcklqdq %xmm10, %xmm14                               #2348.1290
        punpcklqdq %xmm9, %xmm7                                 #2348.1476
        movd      -12(%rsi,%rdi), %xmm10                        #2350.1579
        punpcklqdq %xmm0, %xmm12                                #2346.1288
        punpcklqdq %xmm1, %xmm2                                 #2346.1474
        punpcklqdq %xmm3, %xmm5                                 #2347.1290
        shufps    $136, %xmm7, %xmm14                           #2348.1270
        movd      -12(%r11,%rdi), %xmm7                         #2350.1309
        movd      -12(%rdx,%rdi), %xmm3                         #2350.1392
        movd      -12(%rcx,%rdi), %xmm9                         #2350.1496
        shufps    $136, %xmm2, %xmm12                           #2346.1268
        movd      -16(%rcx,%rdi), %xmm2                         #2349.1496
        shufps    $136, %xmm8, %xmm5                            #2347.1270
        movd      -16(%r11,%rdi), %xmm8                         #2349.1309
        movd      -16(%rdx,%rdi), %xmm0                         #2349.1392
        movd      -16(%rsi,%rdi), %xmm1                         #2349.1579
        punpcklqdq %xmm3, %xmm7                                 #2350.1290
        punpcklqdq %xmm10, %xmm9                                #2350.1476
        punpcklqdq %xmm0, %xmm8                                 #2349.1290
        punpcklqdq %xmm1, %xmm2                                 #2349.1476
        movd      -8(%rsi,%rdi), %xmm1                          #2351.1576
        shufps    $136, %xmm9, %xmm7                            #2350.1270
        movd      -8(%r11,%rdi), %xmm10                         #2351.1306
        movd      -8(%rdx,%rdi), %xmm0                          #2351.1389
        movd      -8(%rcx,%rdi), %xmm9                          #2351.1493
        punpcklqdq %xmm0, %xmm10                                #2351.1287
        punpcklqdq %xmm1, %xmm9                                 #2351.1473
        movups    %xmm7, 32(%rsp)                               #2350.1270[spill]
        shufps    $136, %xmm2, %xmm8                            #2349.1270
        shufps    $136, %xmm9, %xmm10                           #2351.1267
        movd      -4(%r11,%rdi), %xmm9                          #2352.1306
        movd      -4(%rdx,%rdi), %xmm2                          #2352.1389
        movd      -4(%rcx,%rdi), %xmm7                          #2352.1493
        movd      -4(%rsi,%rdi), %xmm3                          #2352.1576
        punpcklqdq %xmm2, %xmm9                                 #2352.1287
        punpcklqdq %xmm3, %xmm7                                 #2352.1473
        movdqa    %xmm15, %xmm3                                 #2356.28
        shufps    $136, %xmm7, %xmm9                            #2352.1267
        movd      (%r11,%rdi), %xmm7                            #2353.1306
        movd      (%rdx,%rdi), %xmm0                            #2353.1389
        punpcklqdq %xmm0, %xmm7                                 #2353.1287
        movaps    %xmm4, %xmm0                                  #2354.30
        movd      (%rcx,%rdi), %xmm2                            #2353.1493
        subps     %xmm13, %xmm0                                 #2354.30
        andps     %xmm0, %xmm3                                  #2356.28
        subps     %xmm0, %xmm4                                  #2357.28
        subps     %xmm3, %xmm0                                  #2360.30
        subps     %xmm13, %xmm4                                 #2358.28
        addps     %xmm0, %xmm6                                  #2361.30
        subps     %xmm11, %xmm4                                 #2359.28
        movd      (%rsi,%rdi), %xmm1                            #2353.1576
        addps     %xmm6, %xmm4                                  #2362.28
        rcpps     %xmm3, %xmm6                                  #2363.32
        andps     %xmm15, %xmm6                                 #2364.32
        mulps     %xmm6, %xmm3                                  #2366.47
        punpcklqdq %xmm1, %xmm2                                 #2353.1473
        movups    1152+__svml_stan_ha_data_internal(%rip), %xmm1 #2366.26
        shufps    $136, %xmm2, %xmm7                            #2353.1267
        subps     %xmm3, %xmm1                                  #2366.26
        movups    %xmm1, 16(%rsp)                               #2366.26[spill]
        mulps     %xmm1, %xmm1                                  #2367.26
        movups    80(%rsp), %xmm2                               #2367.26[spill]
        movups    64(%rsp), %xmm3                               #2367.26[spill]
        movups    48(%rsp), %xmm0                               #2367.26[spill]
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm3 xmm4 xmm5 xmm6 xmm7 xmm8 xmm9 xmm10 xmm11 xmm12 xmm13 xmm14
..B1.15:                        # Preds ..B1.12
                                # Execution count [5.00e-02]: Infreq
        movups    %xmm0, 48(%rsp)                               #[spill]
        movaps    %xmm6, %xmm0                                  #2368.40
        movups    16(%rsp), %xmm15                              #2368.40[spill]
        mulps     %xmm15, %xmm0                                 #2368.40
        mulps     %xmm13, %xmm7                                 #2386.38
        mulps     %xmm13, %xmm10                                #2385.38
        addps     %xmm6, %xmm0                                  #2368.28
        mulps     %xmm12, %xmm6                                 #2374.36
        addps     %xmm7, %xmm9                                  #2386.26
        mulps     %xmm0, %xmm1                                  #2369.44
        movaps    %xmm3, %xmm7                                  #2395.33
        addps     %xmm1, %xmm0                                  #2369.32
        mulps     %xmm0, %xmm4                                  #2370.28
        andnps    %xmm2, %xmm7                                  #2395.33
        movaps    %xmm4, %xmm1                                  #2371.27
        movaps    %xmm7, %xmm2                                  #2395.22
        mulps     %xmm4, %xmm4                                  #2372.44
        subps     %xmm15, %xmm1                                 #2371.27
        movaps    %xmm13, %xmm15                                #2376.30
        subps     %xmm1, %xmm4                                  #2372.32
        mulps     %xmm8, %xmm15                                 #2376.30
        mulps     %xmm4, %xmm0                                  #2373.32
        movaps    %xmm5, %xmm1                                  #2377.30
        mulps     %xmm0, %xmm12                                 #2375.36
        addps     %xmm15, %xmm1                                 #2377.30
        movaps    %xmm1, %xmm4                                  #2379.30
        subps     %xmm1, %xmm5                                  #2378.30
        addps     %xmm6, %xmm4                                  #2379.30
        addps     %xmm5, %xmm15                                 #2380.30
        subps     %xmm4, %xmm6                                  #2381.30
        addps     %xmm15, %xmm12                                #2382.30
        addps     %xmm6, %xmm1                                  #2383.30
        movups    32(%rsp), %xmm5                               #2385.26[spill]
        movaps    %xmm13, %xmm6                                 #2387.26
        mulps     %xmm13, %xmm6                                 #2387.26
        addps     %xmm5, %xmm8                                  #2388.26
        addps     %xmm1, %xmm12                                 #2384.30
        addps     %xmm5, %xmm10                                 #2385.26
        mulps     %xmm6, %xmm9                                  #2389.38
        mulps     %xmm8, %xmm11                                 #2390.38
        addps     %xmm9, %xmm10                                 #2389.26
        addps     %xmm11, %xmm12                                #2390.26
        mulps     %xmm10, %xmm13                                #2392.39
        addps     %xmm12, %xmm14                                #2391.26
        movups    48(%rsp), %xmm0                               #2395.22[spill]
        addps     %xmm13, %xmm14                                #2392.27
        addps     %xmm14, %xmm4                                 #2393.32
        xorps     (%rsp), %xmm4                                 #2394.28[spill]
        andps     %xmm3, %xmm4                                  #2395.82
        orps      %xmm4, %xmm2                                  #2395.22
        jmp       ..B1.2        # Prob 100%                     #2395.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm2
	.cfi_endproc
# mark_end;
	.type	__svml_tanf4_ha_ex,@function
	.size	__svml_tanf4_ha_ex,.-__svml_tanf4_ha_ex
..LN__svml_tanf4_ha_ex.0:
	.data
# -- End  __svml_tanf4_ha_ex
	.text
.L_2__routine_start___svml_tanf4_ha_e9_1:
# -- Begin  __svml_tanf4_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf4_ha_e9
# --- __svml_tanf4_ha_e9(__m128)
__svml_tanf4_ha_e9:
# parameter 1: %xmm0
..B2.1:                         # Preds ..B2.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2438.1
        .byte     15                                            #2869.22
        .byte     30                                            #2869.22
        .byte     250                                           #2869.22
	.cfi_startproc
..___tag_value___svml_tanf4_ha_e9.54:
..L55:
                                                         #2438.1
        pushq     %rbp                                          #2438.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2438.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2438.1
        subq      $192, %rsp                                    #2438.1
        vmovaps   %xmm0, %xmm5                                  #2438.1
        vmovups   960+__svml_stan_ha_data_internal(%rip), %xmm4 #2483.53
        xorl      %eax, %eax                                    #2482.9
        vandps    %xmm4, %xmm5, %xmm6                           #2484.20
        vandnps   %xmm5, %xmm4, %xmm3                           #2485.21
        vmulps    832+__svml_stan_ha_data_internal(%rip), %xmm6, %xmm7 #2493.29
        vpxor     %xmm10, %xmm10, %xmm10                        #2498.29
        vcmpnleps 1216+__svml_stan_ha_data_internal(%rip), %xmm6, %xmm4 #2487.34
        vcvtps2pd %xmm6, %ymm2                                  #2490.26
        vmovups   1088+__svml_stan_ha_data_internal(%rip), %xmm9 #2492.54
        vmovmskps %xmm4, %edx                                   #2489.86
        vaddps    %xmm7, %xmm9, %xmm8                           #2493.17
        vpslld    $31, %xmm8, %xmm1                             #2494.38
        vsubps    %xmm9, %xmm8, %xmm14                          #2495.17
        vcvtps2pd %xmm14, %ymm6                                 #2501.23
        vpcmpeqd  %xmm10, %xmm1, %xmm12                         #2498.29
        vpshufd   $80, %xmm12, %xmm11                           #2499.29
        vxorps    %xmm3, %xmm1, %xmm1                           #2519.20
        vpshufd   $250, %xmm12, %xmm13                          #2499.111
        vmulpd    1280+__svml_stan_ha_data_internal(%rip), %ymm6, %ymm15 #2503.42
        vmulpd    1344+__svml_stan_ha_data_internal(%rip), %ymm6, %ymm7 #2505.39
        vsubpd    %ymm15, %ymm2, %ymm2                          #2503.17
        vsubpd    %ymm7, %ymm2, %ymm9                           #2505.17
        vmulpd    %ymm9, %ymm9, %ymm12                          #2506.18
        vmulpd    1472+__svml_stan_ha_data_internal(%rip), %ymm9, %ymm8 #2508.17
        vmulpd    %ymm8, %ymm12, %ymm10                         #2509.32
        vaddpd    %ymm10, %ymm9, %ymm15                         #2509.17
        vinsertf128 $1, %xmm13, %ymm11, %ymm0                   #2500.23
        vmulpd    1664+__svml_stan_ha_data_internal(%rip), %ymm12, %ymm11 #2512.32
        vaddpd    1600+__svml_stan_ha_data_internal(%rip), %ymm11, %ymm13 #2512.17
        vmulpd    %ymm13, %ymm12, %ymm14                        #2514.32
        vaddpd    1536+__svml_stan_ha_data_internal(%rip), %ymm14, %ymm2 #2514.17
        vblendvpd %ymm0, %ymm15, %ymm2, %ymm6                   #2515.19
        vblendvpd %ymm0, %ymm2, %ymm15, %ymm0                   #2516.19
        vdivpd    %ymm0, %ymm6, %ymm0                           #2517.19
        vcvtpd2ps %ymm0, %xmm2                                  #2518.19
        vxorps    %xmm1, %xmm2, %xmm1                           #2520.18
        testl     %edx, %edx                                    #2521.70
        jne       ..B2.12       # Prob 5%                       #2521.70
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm4 xmm5
..B2.2:                         # Preds ..B2.12 ..B2.1
                                # Execution count [1.00e+00]
        testl     %eax, %eax                                    #2872.52
        jne       ..B2.4        # Prob 5%                       #2872.52
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm5
..B2.15:                        # Preds ..B2.4 ..B2.2
                                # Execution count [9.50e-01]
        vzeroupper                                              #
                                # LOE rbx r12 r13 r14 r15 xmm1
..B2.3:                         # Preds ..B2.10 ..B2.15
                                # Execution count [1.00e+00]
        movaps    %xmm1, %xmm0                                  #2875.12
        movq      %rbp, %rsp                                    #2875.12
        popq      %rbp                                          #2875.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #2875.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B2.4:                         # Preds ..B2.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm5, 64(%rsp)                               #2872.193
        vmovups   %xmm1, 128(%rsp)                              #2872.264
        je        ..B2.15       # Prob 95%                      #2872.368
                                # LOE rbx r12 r13 r14 r15 eax xmm1
..B2.7:                         # Preds ..B2.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #2872.448
                                # LOE rbx r12 r13 r14 r15 eax edx
..B2.17:                        # Preds ..B2.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, 8(%rsp)                                 #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B2.8:                         # Preds ..B2.9 ..B2.17
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #2872.511
        jc        ..B2.11       # Prob 5%                       #2872.511
                                # LOE rbx r12 r14 r15 r13d
..B2.9:                         # Preds ..B2.11 ..B2.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #2872.464
        cmpl      $4, %r12d                                     #2872.459
        jl        ..B2.8        # Prob 82%                      #2872.459
                                # LOE rbx r12 r14 r15 r13d
..B2.10:                        # Preds ..B2.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm1                              #2872.665
        jmp       ..B2.3        # Prob 100%                     #2872.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm1
..B2.11:                        # Preds ..B2.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #2872.540
        lea       128(%rsp,%r12,4), %rsi                        #2872.540
..___tag_value___svml_tanf4_ha_e9.72:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #2872.540
..___tag_value___svml_tanf4_ha_e9.73:
        jmp       ..B2.9        # Prob 100%                     #2872.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B2.12:                        # Preds ..B2.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   .L_2il0floatpacket.27(%rip), %xmm0            #2591.31
        movl      $2139095040, %edx                             #2684.24
        vandps    %xmm0, %xmm5, %xmm12                          #2592.25
        lea       __svml_stan_ha_reduction_data_internal(%rip), %r9 #2687.752
        vcmpeqps  %xmm0, %xmm12, %xmm12                         #2594.35
        vmovups   .L_2il0floatpacket.28(%rip), %xmm7            #2597.32
        movl      $8388607, %r10d                               #2693.27
        vmovups   %xmm5, 32(%rsp)                               #[spill]
        vandnps   %xmm5, %xmm7, %xmm0                           #2599.28
        vmovmskps %xmm12, %eax                                  #2596.56
        vandps    %xmm7, %xmm5, %xmm12                          #2598.29
        vmovd     %edx, %xmm5                                   #2684.24
        vpshufd   $0, %xmm5, %xmm2                              #2684.24
        vmovups   %xmm4, (%rsp)                                 #[spill]
        vpand     %xmm0, %xmm2, %xmm4                           #2685.22
        vpsrld    $23, %xmm4, %xmm10                            #2686.22
        movl      $8388608, %r11d                               #2694.26
        vpslld    $1, %xmm10, %xmm6                             #2687.238
        movl      $65535, %edx                                  #2697.23
        vpaddd    %xmm10, %xmm6, %xmm15                         #2687.391
        vpslld    $2, %xmm15, %xmm14                            #2687.391
        vmovd     %xmm14, %ecx                                  #2687.465
        vmovups   %xmm1, 16(%rsp)                               #[spill]
        vpextrd   $1, %xmm14, %esi                              #2687.533
        vpextrd   $2, %xmm14, %edi                              #2687.604
        vpextrd   $3, %xmm14, %r8d                              #2687.675
        vmovd     4(%rcx,%r9), %xmm2                            #2688.1281
        vmovd     4(%rsi,%r9), %xmm4                            #2688.1364
        vmovd     (%rsi,%r9), %xmm7                             #2687.1363
        vmovd     (%rcx,%r9), %xmm9                             #2687.1280
        vmovd     (%rdi,%r9), %xmm1                             #2687.1467
        vmovd     4(%rdi,%r9), %xmm11                           #2688.1468
        vmovd     (%r8,%r9), %xmm8                              #2687.1550
        vmovd     4(%r8,%r9), %xmm10                            #2688.1551
        vpunpcklqdq %xmm4, %xmm2, %xmm13                        #2688.1261
        vmovd     %r10d, %xmm2                                  #2693.27
        vpunpcklqdq %xmm7, %xmm9, %xmm3                         #2687.1260
        movl      $511, %r10d                                   #2775.26
        vpunpcklqdq %xmm8, %xmm1, %xmm5                         #2687.1447
        vmovd     8(%r8,%r9), %xmm1                             #2689.1550
        movl      $262143, %r8d                                 #2766.26
        vpunpcklqdq %xmm10, %xmm11, %xmm15                      #2688.1448
        vmovd     %r11d, %xmm11                                 #2694.26
        vmovd     8(%rcx,%r9), %xmm14                           #2689.1280
        movl      $-2147483648, %ecx                            #2750.23
        vmovd     8(%rsi,%r9), %xmm9                            #2689.1363
        movl      $1065353216, %esi                             #2752.28
        vmovd     8(%rdi,%r9), %xmm7                            #2689.1467
        movl      $679477248, %edi                              #2764.28
        vpshufd   $0, %xmm2, %xmm4                              #2693.27
        movl      $872415232, %r9d                              #2773.28
        vshufps   $136, %xmm5, %xmm3, %xmm6                     #2687.1240
        vpand     %xmm0, %xmm4, %xmm10                          #2695.25
        vshufps   $136, %xmm15, %xmm13, %xmm3                   #2688.1241
        vmovd     %edx, %xmm15                                  #2697.23
        vpunpcklqdq %xmm9, %xmm14, %xmm8                        #2689.1260
        movl      $255, %r11d                                   #2762.21
        vpunpcklqdq %xmm1, %xmm7, %xmm5                         #2689.1447
        vpsrld    $16, %xmm3, %xmm7                             #2700.16
        vpshufd   $0, %xmm11, %xmm13                            #2694.26
        movl      $127, %edx                                    #2816.47
        vshufps   $136, %xmm5, %xmm8, %xmm1                     #2689.1240
        vpaddd    %xmm13, %xmm10, %xmm14                        #2696.25
        vpshufd   $0, %xmm15, %xmm10                            #2697.23
        vpsrld    $16, %xmm1, %xmm13                            #2702.16
        vpand     %xmm10, %xmm3, %xmm15                         #2701.16
        vpsrld    $16, %xmm14, %xmm4                            #2704.16
        vpand     %xmm10, %xmm14, %xmm3                         #2705.16
        vpand     %xmm10, %xmm1, %xmm1                          #2703.16
        vpmulld   %xmm1, %xmm4, %xmm8                           #2711.17
        vpand     %xmm10, %xmm6, %xmm5                          #2699.16
        vpmulld   %xmm13, %xmm4, %xmm1                          #2710.17
        vpsrld    $16, %xmm8, %xmm11                            #2736.17
        vpmulld   %xmm13, %xmm3, %xmm13                         #2716.17
        vpsrld    $16, %xmm6, %xmm6                             #2698.16
        vpmulld   %xmm15, %xmm3, %xmm14                         #2715.17
        vpsrld    $16, %xmm13, %xmm13                           #2718.17
        vpand     %xmm10, %xmm14, %xmm8                         #2726.17
        vpaddd    %xmm13, %xmm1, %xmm1                          #2722.17
        vpmulld   %xmm7, %xmm3, %xmm9                           #2714.17
        vpaddd    %xmm1, %xmm8, %xmm8                           #2731.17
        vpmulld   %xmm15, %xmm4, %xmm15                         #2709.17
        vpsrld    $16, %xmm14, %xmm14                           #2719.17
        vpaddd    %xmm8, %xmm11, %xmm1                          #2737.17
        vpand     %xmm10, %xmm9, %xmm8                          #2727.17
        vpaddd    %xmm14, %xmm15, %xmm13                        #2723.17
        vpsrld    $16, %xmm1, %xmm11                            #2738.17
        vpmulld   %xmm5, %xmm3, %xmm2                           #2713.17
        vpaddd    %xmm13, %xmm8, %xmm8                          #2732.17
        vpmulld   %xmm7, %xmm4, %xmm7                           #2708.17
        vpsrld    $16, %xmm9, %xmm9                             #2720.17
        vpaddd    %xmm8, %xmm11, %xmm8                          #2739.17
        vpand     %xmm10, %xmm2, %xmm11                         #2728.17
        vpaddd    %xmm9, %xmm7, %xmm14                          #2724.17
        vpsrld    $16, %xmm8, %xmm13                            #2740.17
        vpmulld   %xmm6, %xmm3, %xmm7                           #2712.17
        vpaddd    %xmm14, %xmm11, %xmm11                        #2733.17
        vpmulld   %xmm5, %xmm4, %xmm3                           #2707.17
        vpsrld    $16, %xmm2, %xmm5                             #2721.17
        vpaddd    %xmm11, %xmm13, %xmm9                         #2741.17
        vpand     %xmm10, %xmm7, %xmm6                          #2729.17
        vpaddd    %xmm5, %xmm3, %xmm2                           #2746.22
        vpsrld    $16, %xmm9, %xmm14                            #2742.17
        vpaddd    %xmm2, %xmm6, %xmm4                           #2746.22
        vpslld    $16, %xmm8, %xmm7                             #2747.23
        vpaddd    %xmm4, %xmm14, %xmm14                         #2746.22
        vmovd     %ecx, %xmm8                                   #2750.23
        vpshufd   $0, %xmm8, %xmm3                              #2750.23
        vpslld    $16, %xmm14, %xmm11                           #2746.22
        vpand     %xmm10, %xmm9, %xmm9                          #2745.17
        vpand     %xmm10, %xmm1, %xmm1                          #2744.17
        vmovd     %esi, %xmm10                                  #2752.28
        vpaddd    %xmm9, %xmm11, %xmm6                          #2748.22
        vpshufd   $0, %xmm10, %xmm14                            #2752.28
        vpand     %xmm0, %xmm3, %xmm3                           #2751.21
        vpaddd    %xmm1, %xmm7, %xmm2                           #2749.23
        vpsrld    $9, %xmm6, %xmm9                              #2754.24
        vpxor     %xmm14, %xmm3, %xmm7                          #2753.26
        vmovd     %r8d, %xmm13                                  #2766.26
        vmovups   .L_2il0floatpacket.29(%rip), %xmm1            #2757.15
        vpor      %xmm7, %xmm9, %xmm8                           #2755.24
        vpshufd   $0, %xmm13, %xmm15                            #2766.26
        vmovd     %edi, %xmm11                                  #2764.28
        vpshufd   $0, %xmm11, %xmm10                            #2764.28
        vpand     %xmm2, %xmm15, %xmm14                         #2767.24
        vpslld    $5, %xmm14, %xmm9                             #2768.24
        vpsrld    $18, %xmm2, %xmm2                             #2778.23
        vaddps    %xmm1, %xmm8, %xmm5                           #2758.19
        vsubps    %xmm1, %xmm5, %xmm4                           #2759.9
        vpxor     %xmm10, %xmm3, %xmm1                          #2765.26
        vpor      %xmm1, %xmm9, %xmm7                           #2769.24
        vsubps    %xmm4, %xmm8, %xmm8                           #2760.17
        vsubps    %xmm1, %xmm7, %xmm7                           #2772.17
        vmovd     %r9d, %xmm4                                   #2773.28
        lea       1764+__svml_stan_ha_data_internal(%rip), %r9  #2818.766
        vpshufd   $0, %xmm4, %xmm11                             #2773.28
        vpxor     %xmm11, %xmm3, %xmm9                          #2774.26
        vmovd     %r10d, %xmm3                                  #2775.26
        vpshufd   $0, %xmm3, %xmm10                             #2775.26
        vpand     %xmm6, %xmm10, %xmm6                          #2776.24
        vpslld    $14, %xmm6, %xmm13                            #2777.24
        vpor      %xmm2, %xmm13, %xmm15                         #2779.24
        vpor      %xmm9, %xmm15, %xmm14                         #2780.24
        vmovups   .L_2il0floatpacket.33(%rip), %xmm11           #2791.20
        vsubps    %xmm9, %xmm14, %xmm1                          #2783.18
        vmovups   .L_2il0floatpacket.32(%rip), %xmm2            #2790.21
        vaddps    %xmm1, %xmm8, %xmm3                           #2784.11
        vsubps    %xmm3, %xmm8, %xmm8                           #2785.17
        vaddps    %xmm8, %xmm1, %xmm14                          #2786.18
        vandps    %xmm11, %xmm3, %xmm1                          #2792.20
        vmulps    %xmm1, %xmm2, %xmm10                          #2796.16
        vsubps    %xmm1, %xmm3, %xmm4                           #2793.21
        vaddps    %xmm14, %xmm7, %xmm8                          #2787.11
        vmulps    %xmm4, %xmm2, %xmm15                          #2798.16
        vmulps    .L_2il0floatpacket.30(%rip), %xmm8, %xmm13    #2797.16
        vmovups   .L_2il0floatpacket.31(%rip), %xmm7            #2789.20
        vmulps    %xmm4, %xmm7, %xmm6                           #2795.16
        vmulps    %xmm1, %xmm7, %xmm9                           #2794.20
        vaddps    %xmm15, %xmm13, %xmm7                         #2800.17
        vaddps    %xmm10, %xmm6, %xmm14                         #2799.17
        vaddps    %xmm7, %xmm14, %xmm1                          #2801.17
        vandps    .L_2il0floatpacket.34(%rip), %xmm0, %xmm14    #2807.11
        vaddps    %xmm1, %xmm9, %xmm8                           #2802.17
        vsubps    %xmm8, %xmm9, %xmm9                           #2803.20
        vaddps    %xmm9, %xmm1, %xmm2                           #2804.17
        vmovups   .L_2il0floatpacket.35(%rip), %xmm9            #2806.16
        vcmpltps  %xmm14, %xmm9, %xmm3                          #2808.17
        vcmpleps  %xmm9, %xmm14, %xmm7                          #2809.20
        vandps    %xmm0, %xmm7, %xmm0                           #2810.24
        vandps    %xmm8, %xmm3, %xmm14                          #2811.25
        vorps     %xmm14, %xmm0, %xmm9                          #2812.9
        vmovd     %r11d, %xmm0                                  #2762.21
        vpshufd   $0, %xmm0, %xmm7                              #2762.21
        vmovd     %edx, %xmm1                                   #2816.47
        vpshufd   $0, %xmm1, %xmm4                              #2816.47
        vpand     %xmm7, %xmm5, %xmm5                           #2763.19
        vpand     %xmm4, %xmm5, %xmm10                          #2817.35
        vandps    %xmm2, %xmm3, %xmm8                           #2813.9
        vpslld    $2, %xmm10, %xmm6                             #2818.258
        vpaddd    %xmm10, %xmm6, %xmm13                         #2818.405
        vpslld    $3, %xmm13, %xmm15                            #2818.405
        vmovd     %xmm15, %ecx                                  #2818.479
        vpextrd   $1, %xmm15, %esi                              #2818.547
        vpextrd   $2, %xmm15, %edi                              #2818.618
        vpextrd   $3, %xmm15, %r8d                              #2818.689
        vmovd     -36(%rcx,%r9), %xmm0                          #2818.1258
        vmovd     -36(%rsi,%r9), %xmm14                         #2818.1341
        vmovd     -36(%rdi,%r9), %xmm7                          #2818.1445
        vmovd     -36(%r8,%r9), %xmm1                           #2818.1528
        vpunpcklqdq %xmm14, %xmm0, %xmm3                        #2818.1238
        vpunpcklqdq %xmm1, %xmm7, %xmm5                         #2818.1425
        vmovd     -32(%r8,%r9), %xmm10                          #2819.1528
        vshufps   $136, %xmm5, %xmm3, %xmm14                    #2818.1218
        vmovd     -28(%r8,%r9), %xmm3                           #2820.1527
        vmovd     -32(%rcx,%r9), %xmm2                          #2819.1258
        vmovd     -28(%rcx,%r9), %xmm0                          #2820.1257
        vmovd     -32(%rsi,%r9), %xmm4                          #2819.1341
        vmovd     -28(%rsi,%r9), %xmm7                          #2820.1340
        vmovd     -32(%rdi,%r9), %xmm6                          #2819.1445
        vmovd     -28(%rdi,%r9), %xmm1                          #2820.1444
        vpunpcklqdq %xmm4, %xmm2, %xmm13                        #2819.1238
        vpunpcklqdq %xmm10, %xmm6, %xmm15                       #2819.1425
        vpunpcklqdq %xmm7, %xmm0, %xmm5                         #2820.1237
        vpunpcklqdq %xmm3, %xmm1, %xmm2                         #2820.1424
        vmovd     -24(%rcx,%r9), %xmm4                          #2821.1259
        vmovd     -24(%rsi,%r9), %xmm6                          #2821.1342
        vshufps   $136, %xmm15, %xmm13, %xmm13                  #2819.1218
        vshufps   $136, %xmm2, %xmm5, %xmm10                    #2820.1217
        vmovd     -20(%rsi,%r9), %xmm5                          #2822.1342
        vpunpcklqdq %xmm6, %xmm4, %xmm7                         #2821.1239
        vmovd     -24(%rdi,%r9), %xmm15                         #2821.1446
        vmovd     -20(%rdi,%r9), %xmm2                          #2822.1446
        vmovd     -24(%r8,%r9), %xmm0                           #2821.1529
        vmovd     -20(%r8,%r9), %xmm4                           #2822.1529
        vmovd     -20(%rcx,%r9), %xmm3                          #2822.1259
        vpunpcklqdq %xmm0, %xmm15, %xmm1                        #2821.1426
        vmovd     -16(%rcx,%r9), %xmm0                          #2823.1259
        vpunpcklqdq %xmm5, %xmm3, %xmm6                         #2822.1239
        vpunpcklqdq %xmm4, %xmm2, %xmm15                        #2822.1426
        vmovd     -16(%rsi,%r9), %xmm3                          #2823.1342
        vmovd     -16(%rdi,%r9), %xmm5                          #2823.1446
        vmovd     -16(%r8,%r9), %xmm2                           #2823.1529
        vshufps   $136, %xmm1, %xmm7, %xmm1                     #2821.1219
        vshufps   $136, %xmm15, %xmm6, %xmm7                    #2822.1219
        vpunpcklqdq %xmm3, %xmm0, %xmm4                         #2823.1239
        vpunpcklqdq %xmm2, %xmm5, %xmm6                         #2823.1426
        vmovd     -12(%r8,%r9), %xmm2                           #2824.1529
        vmovd     -12(%rcx,%r9), %xmm15                         #2824.1259
        vmovd     -12(%rsi,%r9), %xmm0                          #2824.1342
        vmovd     -12(%rdi,%r9), %xmm5                          #2824.1446
        vshufps   $136, %xmm6, %xmm4, %xmm3                     #2823.1219
        vpunpcklqdq %xmm0, %xmm15, %xmm4                        #2824.1239
        vpunpcklqdq %xmm2, %xmm5, %xmm6                         #2824.1426
        vmovd     -8(%rdi,%r9), %xmm2                           #2825.1443
        vshufps   $136, %xmm6, %xmm4, %xmm5                     #2824.1219
        vmovd     -8(%rcx,%r9), %xmm15                          #2825.1256
        vmovd     -8(%rsi,%r9), %xmm0                           #2825.1339
        vmovd     -8(%r8,%r9), %xmm4                            #2825.1526
        vpunpcklqdq %xmm0, %xmm15, %xmm6                        #2825.1236
        vpunpcklqdq %xmm4, %xmm2, %xmm15                        #2825.1423
        vshufps   $136, %xmm15, %xmm6, %xmm6                    #2825.1216
        vmovd     -4(%r8,%r9), %xmm15                           #2826.1526
        vmovd     -4(%rcx,%r9), %xmm0                           #2826.1256
        vmovd     -4(%rsi,%r9), %xmm2                           #2826.1339
        vmovd     -4(%rdi,%r9), %xmm4                           #2826.1443
        vpunpcklqdq %xmm2, %xmm0, %xmm0                         #2826.1236
        vpunpcklqdq %xmm15, %xmm4, %xmm2                        #2826.1423
        vmovd     (%rcx,%r9), %xmm4                             #2827.1256
        vmovd     (%rsi,%r9), %xmm15                            #2827.1339
        vshufps   $136, %xmm2, %xmm0, %xmm2                     #2826.1216
        vpunpcklqdq %xmm15, %xmm4, %xmm4                        #2827.1236
        vmovd     (%rdi,%r9), %xmm0                             #2827.1443
        vmovd     (%r8,%r9), %xmm15                             #2827.1526
        vpunpcklqdq %xmm15, %xmm0, %xmm0                        #2827.1423
        vsubps    %xmm9, %xmm14, %xmm15                         #2828.30
        vmulps    %xmm6, %xmm9, %xmm6                           #2859.38
        vsubps    %xmm15, %xmm14, %xmm14                        #2831.28
        vaddps    %xmm6, %xmm5, %xmm6                           #2859.26
        vaddps    %xmm5, %xmm3, %xmm5                           #2862.26
        vsubps    %xmm9, %xmm14, %xmm14                         #2832.28
        vshufps   $136, %xmm0, %xmm4, %xmm4                     #2827.1216
        vandps    %xmm11, %xmm15, %xmm0                         #2830.28
        vmulps    %xmm4, %xmm9, %xmm4                           #2860.38
        vsubps    %xmm0, %xmm15, %xmm15                         #2834.30
        vsubps    %xmm8, %xmm14, %xmm14                         #2833.28
        vmulps    %xmm5, %xmm8, %xmm8                           #2864.38
        vaddps    %xmm15, %xmm13, %xmm13                        #2835.30
        vaddps    %xmm4, %xmm2, %xmm2                           #2860.26
        vaddps    %xmm13, %xmm14, %xmm15                        #2836.28
        vrcpps    %xmm0, %xmm14                                 #2837.32
        vandps    %xmm11, %xmm14, %xmm13                        #2838.32
        vmulps    %xmm13, %xmm0, %xmm0                          #2840.47
        vmovups   1152+__svml_stan_ha_data_internal(%rip), %xmm11 #2839.57
        vmovups   32(%rsp), %xmm5                               #2869.22[spill]
        vsubps    %xmm0, %xmm11, %xmm11                         #2840.26
        vmulps    %xmm11, %xmm13, %xmm0                         #2842.40
        vmulps    %xmm11, %xmm11, %xmm14                        #2841.26
        vaddps    %xmm0, %xmm13, %xmm0                          #2842.28
        vmulps    %xmm0, %xmm14, %xmm14                         #2843.44
        vaddps    %xmm14, %xmm0, %xmm14                         #2843.32
        vmulps    %xmm14, %xmm15, %xmm15                        #2844.28
        vsubps    %xmm11, %xmm15, %xmm0                         #2845.27
        vmulps    %xmm15, %xmm15, %xmm11                        #2846.44
        vsubps    %xmm0, %xmm11, %xmm11                         #2846.32
        vmulps    %xmm13, %xmm10, %xmm0                         #2848.36
        vmulps    %xmm3, %xmm9, %xmm13                          #2850.30
        vmulps    %xmm11, %xmm14, %xmm14                        #2847.32
        vaddps    %xmm13, %xmm1, %xmm11                         #2851.30
        vmulps    %xmm14, %xmm10, %xmm10                        #2849.36
        vsubps    %xmm11, %xmm1, %xmm1                          #2852.30
        vaddps    %xmm11, %xmm0, %xmm14                         #2853.30
        vaddps    %xmm1, %xmm13, %xmm1                          #2854.30
        vsubps    %xmm14, %xmm0, %xmm0                          #2855.30
        vaddps    %xmm1, %xmm10, %xmm10                         #2856.30
        vaddps    %xmm0, %xmm11, %xmm0                          #2857.30
        vaddps    %xmm0, %xmm10, %xmm1                          #2858.30
        vmulps    %xmm9, %xmm9, %xmm0                           #2861.26
        vmulps    %xmm0, %xmm2, %xmm3                           #2863.38
        vaddps    %xmm8, %xmm1, %xmm0                           #2864.26
        vaddps    %xmm3, %xmm6, %xmm2                           #2863.26
        vaddps    %xmm0, %xmm7, %xmm1                           #2865.26
        vmulps    %xmm2, %xmm9, %xmm9                           #2866.39
        vaddps    %xmm9, %xmm1, %xmm0                           #2866.27
        vaddps    %xmm0, %xmm14, %xmm1                          #2867.32
        vmovups   (%rsp), %xmm0                                 #2869.22[spill]
        vxorps    %xmm12, %xmm1, %xmm12                         #2868.28
        vmovups   16(%rsp), %xmm1                               #2869.22[spill]
        blendvps  %xmm0, %xmm12, %xmm1                          #2869.22
        jmp       ..B2.2        # Prob 100%                     #2869.22
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm5
	.cfi_endproc
# mark_end;
	.type	__svml_tanf4_ha_e9,@function
	.size	__svml_tanf4_ha_e9,.-__svml_tanf4_ha_e9
..LN__svml_tanf4_ha_e9.1:
	.data
# -- End  __svml_tanf4_ha_e9
	.text
.L_2__routine_start___svml_tanf16_ha_z0_2:
# -- Begin  __svml_tanf16_ha_z0
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf16_ha_z0
# --- __svml_tanf16_ha_z0(__m512)
__svml_tanf16_ha_z0:
# parameter 1: %zmm0
..B3.1:                         # Preds ..B3.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2881.1
        .byte     15                                            #3188.18
        .byte     30                                            #3188.18
        .byte     250                                           #3188.18
	.cfi_startproc
..___tag_value___svml_tanf16_ha_z0.83:
..L84:
                                                         #2881.1
        pushq     %rbp                                          #2881.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #2881.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #2881.1
        subq      $192, %rsp                                    #2881.1
        xorl      %esi, %esi                                    #2937.5
        vmovups   768+__svml_stan_ha_data_internal(%rip), %zmm9 #2940.61
        vmovups   1088+__svml_stan_ha_data_internal(%rip), %zmm1 #2945.56
        vmovups   64+__svml_stan_ha_data_internal(%rip), %zmm2  #2948.51
        vmovups   256+__svml_stan_ha_data_internal(%rip), %zmm4 #2949.55
        vmovups   320+__svml_stan_ha_data_internal(%rip), %zmm5 #2950.55
        vmovaps   %zmm0, %zmm11                                 #2881.1
        vandps    960+__svml_stan_ha_data_internal(%rip), %zmm11, %zmm10 #2939.16
        vcmpps    $22, {sae}, %zmm9, %zmm10, %k6                #2941.34
        vmovups   __svml_stan_ha_data_internal(%rip), %zmm9     #2946.17
        kmovw     %k6, %eax                                     #2942.79
        vfmadd213ps {rn-sae}, %zmm1, %zmm11, %zmm9              #2946.17
        vsubps    {rn-sae}, %zmm1, %zmm9, %zmm8                 #2947.19
        vfnmadd213ps {rn-sae}, %zmm11, %zmm8, %zmm2             #2951.18
        vmovaps   %zmm4, %zmm3                                  #2952.18
        vfnmadd213ps {rn-sae}, %zmm2, %zmm8, %zmm3              #2952.18
        vmovaps   %zmm5, %zmm0                                  #2953.17
        vfnmadd213ps {rn-sae}, %zmm3, %zmm8, %zmm0              #2953.17
        vsubps    {rn-sae}, %zmm3, %zmm2, %zmm7                 #2954.25
        vsubps    {rn-sae}, %zmm3, %zmm0, %zmm6                 #2955.25
        vfnmadd231ps {rn-sae}, %zmm8, %zmm4, %zmm7              #2956.22
        vfmadd213ps {rn-sae}, %zmm6, %zmm5, %zmm8               #2957.23
        vsubps    {rn-sae}, %zmm8, %zmm7, %zmm1                 #2958.24
        testl     %eax, %eax                                    #2960.66
        jne       ..B3.17       # Prob 5%                       #2960.66
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm9 zmm10 zmm11 k6
..B3.2:                         # Preds ..B3.17 ..B3.1
                                # Execution count [1.00e+00]
        vmulps    {rn-sae}, %zmm0, %zmm0, %zmm6                 #3193.16
        vmovups   704+__svml_stan_ha_data_internal(%rip), %zmm4 #3194.53
        vmovups   640+__svml_stan_ha_data_internal(%rip), %zmm5 #3195.53
        vmovups   384+__svml_stan_ha_data_internal(%rip), %zmm3 #3191.271
        vmovups   1152+__svml_stan_ha_data_internal(%rip), %zmm14 #3207.47
        vmovups   512+__svml_stan_ha_data_internal(%rip), %zmm2 #3192.275
        vfmadd231ps {rn-sae}, %zmm6, %zmm4, %zmm5               #3196.20
        vpermt2ps 448+__svml_stan_ha_data_internal(%rip), %zmm9, %zmm3 #3191.271
        vpermt2ps 576+__svml_stan_ha_data_internal(%rip), %zmm9, %zmm2 #3192.275
        vmulps    {rn-sae}, %zmm0, %zmm5, %zmm8                 #3197.22
        vfnmsub213ps {rn-sae}, %zmm1, %zmm6, %zmm8              #3198.20
        vsubps    {rn-sae}, %zmm8, %zmm0, %zmm1                 #3199.19
        vmulps    {rn-sae}, %zmm1, %zmm3, %zmm4                 #3208.17
        vsubps    {rn-sae}, %zmm1, %zmm0, %zmm7                 #3200.22
        vaddps    {rn-sae}, %zmm3, %zmm1, %zmm0                 #3202.23
        vsubps    {rn-sae}, %zmm4, %zmm14, %zmm6                #3209.25
        vsubps    {rn-sae}, %zmm8, %zmm7, %zmm9                 #3201.22
        vsubps    {rn-sae}, %zmm3, %zmm0, %zmm10                #3203.22
        vsubps    {rn-sae}, %zmm14, %zmm6, %zmm15               #3211.21
        vsubps    {rn-sae}, %zmm10, %zmm1, %zmm12               #3204.22
        vaddps    {rn-sae}, %zmm4, %zmm15, %zmm14               #3212.22
        vaddps    {rn-sae}, %zmm2, %zmm12, %zmm13               #3205.22
        vmovaps   %zmm3, %zmm5                                  #3210.22
        vfnmadd213ps {rn-sae}, %zmm4, %zmm1, %zmm5              #3210.22
        vaddps    {rn-sae}, %zmm9, %zmm13, %zmm7                #3206.22
        vsubps    {rn-sae}, %zmm5, %zmm14, %zmm15               #3213.22
        vfmadd213ps {rn-sae}, %zmm15, %zmm9, %zmm3              #3214.20
        vfmadd213ps {rn-sae}, %zmm3, %zmm1, %zmm2               #3215.20
        vrcp14ps  %zmm6, %zmm1                                  #3216.15
        vmulps    {rn-sae}, %zmm0, %zmm1, %zmm3                 #3217.26
        vfmsub213ps {rn-sae}, %zmm0, %zmm3, %zmm6               #3218.18
        vfnmadd213ps {rn-sae}, %zmm6, %zmm3, %zmm2              #3219.22
        vsubps    {rn-sae}, %zmm7, %zmm2, %zmm0                 #3220.24
        vfnmadd213ps {rn-sae}, %zmm3, %zmm1, %zmm0              #3221.18
        testl     %esi, %esi                                    #3223.52
        jne       ..B3.4        # Prob 5%                       #3223.52
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B3.3:                         # Preds ..B3.10 ..B3.8 ..B3.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #3226.12
        popq      %rbp                                          #3226.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3226.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B3.4:                         # Preds ..B3.2
                                # Execution count [5.00e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3223.249
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm11
..B3.5:                         # Preds ..B3.4
                                # Execution count [5.00e-02]: Infreq
        movzwl    32(%rsp), %edx                                #3223.249
        movl      %edx, %eax                                    #3223.303
        orl       $8064, %eax                                   #3223.303
        cmpl      %eax, %edx                                    #3223.332
        je        ..B3.7        # Prob 78%                      #3223.332
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B3.6:                         # Preds ..B3.5
                                # Execution count [1.10e-02]: Infreq
        movl      %eax, 32(%rsp)                                #3223.344
        vldmxcsr  32(%rsp)                                      #3223.344
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0 zmm11
..B3.7:                         # Preds ..B3.6 ..B3.5
                                # Execution count [5.00e-02]: Infreq
        vmovups   %zmm11, 64(%rsp)                              #3223.404
        vmovups   %zmm0, 128(%rsp)                              #3223.478
        testl     %esi, %esi                                    #3223.586
        jne       ..B3.12       # Prob 5%                       #3223.586
                                # LOE rbx r12 r13 r14 r15 eax edx esi zmm0
..B3.8:                         # Preds ..B3.15 ..B3.7
                                # Execution count [5.00e-02]: Infreq
        cmpl      %eax, %edx                                    #3223.931
        je        ..B3.3        # Prob 78%                      #3223.931
                                # LOE rbx r12 r13 r14 r15 edx zmm0
..B3.9:                         # Preds ..B3.8
                                # Execution count [1.10e-02]: Infreq
        vstmxcsr  32(%rsp)                                      #3223.956
        movl      32(%rsp), %eax                                #3223.956
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.10:                        # Preds ..B3.9
                                # Execution count [1.10e-02]: Infreq
        andl      $-8065, %eax                                  #3223.943
        orl       %edx, %eax                                    #3223.943
        movl      %eax, 32(%rsp)                                #3223.943
        vldmxcsr  32(%rsp)                                      #3223.943
        jmp       ..B3.3        # Prob 100%                     #3223.943
                                # LOE rbx r12 r13 r14 r15 zmm0
..B3.12:                        # Preds ..B3.7
                                # Execution count [2.25e-03]: Infreq
        xorl      %ecx, %ecx                                    #3223.668
                                # LOE rbx r12 r13 r14 r15 eax edx ecx esi
..B3.20:                        # Preds ..B3.12
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
..B3.13:                        # Preds ..B3.14 ..B3.20
                                # Execution count [1.25e-02]: Infreq
        btl       %ebx, %r14d                                   #3223.731
        jc        ..B3.16       # Prob 5%                       #3223.731
                                # LOE rbx r15 r12d r13d r14d
..B3.14:                        # Preds ..B3.16 ..B3.13
                                # Execution count [1.25e-02]: Infreq
        incl      %ebx                                          #3223.684
        cmpl      $16, %ebx                                     #3223.679
        jl        ..B3.13       # Prob 82%                      #3223.679
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
        vmovups   128(%rsp), %zmm0                              #3223.886
        jmp       ..B3.8        # Prob 100%                     #3223.886
	.cfi_escape 0x10, 0x03, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x58, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x50, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0e, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 eax edx zmm0
..B3.16:                        # Preds ..B3.13
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%rbx,4), %rdi                         #3223.760
        lea       128(%rsp,%rbx,4), %rsi                        #3223.760
..___tag_value___svml_tanf16_ha_z0.111:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #3223.760
..___tag_value___svml_tanf16_ha_z0.112:
        jmp       ..B3.14       # Prob 100%                     #3223.760
	.cfi_restore 3
	.cfi_restore 12
	.cfi_restore 13
	.cfi_restore 14
                                # LOE r15 ebx r12d r13d r14d
..B3.17:                        # Preds ..B3.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   1024+__svml_stan_ha_data_internal(%rip), %zmm6 #2962.56
        lea       __svml_stan_ha_reduction_data_internal(%rip), %rax #3052.328
        vmovups   %zmm1, 64(%rsp)                               #[spill]
        vmovups   %zmm0, (%rsp)                                 #[spill]
        vandps    %zmm10, %zmm6, %zmm8                          #2963.17
        vcmpps    $0, {sae}, %zmm6, %zmm8, %k0                  #2964.29
        vmovups   .L_2il0floatpacket.39(%rip), %zmm6            #3062.15
        kxnorw    %k0, %k0, %k3                                 #3054.328
        kxnorw    %k0, %k0, %k1                                 #3052.328
        kxnorw    %k0, %k0, %k2                                 #3053.329
        kmovw     %k0, %esi                                     #2965.49
        vpandd    .L_2il0floatpacket.36(%rip), %zmm11, %zmm7    #3050.16
        vpsrld    $23, %zmm7, %zmm4                             #3051.18
        vpslld    $1, %zmm4, %zmm3                              #3052.140
        vpaddd    %zmm4, %zmm3, %zmm15                          #3052.200
        vpslld    $2, %zmm15, %zmm2                             #3052.268
        vpxord    %zmm8, %zmm8, %zmm8                           #3054.328
        vgatherdps 8(%rax,%zmm2), %zmm8{%k3}                    #3054.328
        vpxord    %zmm1, %zmm1, %zmm1                           #3052.328
        vpxord    %zmm12, %zmm12, %zmm12                        #3053.329
        vgatherdps (%rax,%zmm2), %zmm1{%k1}                     #3052.328
        vgatherdps 4(%rax,%zmm2), %zmm12{%k2}                   #3053.329
        vpsrld    $16, %zmm1, %zmm10                            #3063.12
        vpsrld    $16, %zmm12, %zmm0                            #3065.12
        vpandd    .L_2il0floatpacket.37(%rip), %zmm11, %zmm13   #3060.19
        vpaddd    .L_2il0floatpacket.38(%rip), %zmm13, %zmm14   #3061.19
        vpsrld    $16, %zmm8, %zmm13                            #3067.12
        vpsrld    $16, %zmm14, %zmm7                            #3069.12
        vpandd    %zmm6, %zmm14, %zmm14                         #3070.10
        vpandd    %zmm6, %zmm1, %zmm5                           #3064.10
        vpmulld   %zmm13, %zmm7, %zmm1                          #3075.13
        vpmulld   %zmm13, %zmm14, %zmm13                        #3081.13
        vpsrld    $16, %zmm13, %zmm13                           #3083.13
        vpaddd    %zmm13, %zmm1, %zmm1                          #3087.11
        vpandd    %zmm6, %zmm12, %zmm12                         #3066.10
        vpandd    %zmm6, %zmm8, %zmm15                          #3068.10
        vpmulld   %zmm5, %zmm7, %zmm4                           #3072.13
        vpmulld   %zmm0, %zmm7, %zmm3                           #3073.13
        vpmulld   %zmm12, %zmm7, %zmm2                          #3074.13
        vpmulld   %zmm15, %zmm7, %zmm8                          #3076.13
        vpmulld   %zmm10, %zmm14, %zmm7                         #3077.13
        vpmulld   %zmm5, %zmm14, %zmm5                          #3078.13
        vpmulld   %zmm0, %zmm14, %zmm10                         #3079.13
        vpmulld   %zmm12, %zmm14, %zmm0                         #3080.13
        vpsrld    $16, %zmm0, %zmm14                            #3084.13
        vpsrld    $16, %zmm10, %zmm15                           #3085.13
        vpsrld    $16, %zmm5, %zmm12                            #3086.13
        vpsrld    $16, %zmm8, %zmm8                             #3101.13
        vpaddd    %zmm14, %zmm2, %zmm13                         #3088.11
        vpaddd    %zmm15, %zmm3, %zmm2                          #3089.11
        vpaddd    %zmm12, %zmm4, %zmm15                         #3090.11
        vpandd    %zmm6, %zmm0, %zmm0                           #3091.11
        vpaddd    %zmm1, %zmm0, %zmm4                           #3096.11
        vpandd    %zmm6, %zmm10, %zmm10                         #3092.11
        vpaddd    %zmm13, %zmm10, %zmm1                         #3097.11
        vpaddd    %zmm4, %zmm8, %zmm10                          #3102.11
        vpandd    %zmm6, %zmm5, %zmm5                           #3093.11
        vpaddd    %zmm2, %zmm5, %zmm14                          #3098.11
        vpsrld    $16, %zmm10, %zmm2                            #3103.13
        vpandd    %zmm6, %zmm7, %zmm3                           #3094.11
        vpaddd    %zmm15, %zmm3, %zmm0                          #3099.11
        vpaddd    %zmm1, %zmm2, %zmm3                           #3104.11
        vpsrld    $16, %zmm3, %zmm12                            #3105.13
        vpaddd    %zmm14, %zmm12, %zmm7                         #3106.11
        vpsrld    $16, %zmm7, %zmm8                             #3107.13
        vpaddd    %zmm0, %zmm8, %zmm5                           #3108.11
        vpslld    $16, %zmm3, %zmm0                             #3112.19
        vpandd    %zmm6, %zmm10, %zmm4                          #3109.11
        vpandd    %zmm6, %zmm7, %zmm8                           #3110.11
        vpslld    $16, %zmm5, %zmm6                             #3111.18
        vmovups   .L_2il0floatpacket.42(%rip), %zmm7            #3122.15
        vpaddd    %zmm8, %zmm6, %zmm3                           #3113.16
        vpaddd    %zmm4, %zmm0, %zmm0                           #3114.17
        vpsrld    $9, %zmm3, %zmm6                              #3119.20
        vpandd    .L_2il0floatpacket.40(%rip), %zmm11, %zmm4    #3116.15
        vpxord    .L_2il0floatpacket.41(%rip), %zmm4, %zmm10    #3118.20
        vpord     %zmm10, %zmm6, %zmm5                          #3120.18
        vaddps    {rn-sae}, %zmm5, %zmm7, %zmm2                 #3123.21
        vpsrld    $18, %zmm0, %zmm6                             #3143.19
        vsubps    {rn-sae}, %zmm7, %zmm2, %zmm1                 #3124.11
        vsubps    {rn-sae}, %zmm1, %zmm5, %zmm5                 #3125.19
        vpandd    .L_2il0floatpacket.47(%rip), %zmm3, %zmm3     #3141.18
        vpslld    $14, %zmm3, %zmm8                             #3142.20
        vpandd    .L_2il0floatpacket.45(%rip), %zmm0, %zmm12    #3132.18
        vpslld    $5, %zmm12, %zmm13                            #3133.20
        vpxord    .L_2il0floatpacket.46(%rip), %zmm4, %zmm1     #3139.20
        vpord     %zmm6, %zmm8, %zmm0                           #3144.18
        vpord     %zmm1, %zmm0, %zmm3                           #3145.18
        vpandd    .L_2il0floatpacket.43(%rip), %zmm2, %zmm10    #3128.13
        vsubps    {rn-sae}, %zmm1, %zmm3, %zmm2                 #3148.20
        vandps    .L_2il0floatpacket.50(%rip), %zmm11, %zmm8    #3161.11
        vpternlogd $255, %zmm6, %zmm6, %zmm6                    #3162.63
        vpxord    .L_2il0floatpacket.44(%rip), %zmm4, %zmm15    #3130.20
        vpord     %zmm15, %zmm13, %zmm14                        #3134.18
        vmovups   .L_2il0floatpacket.48(%rip), %zmm13           #3153.20
        vsubps    {rn-sae}, %zmm15, %zmm14, %zmm7               #3137.19
        vaddps    {rn-sae}, %zmm2, %zmm5, %zmm14                #3149.13
        vmovups   .L_2il0floatpacket.51(%rip), %zmm15           #3160.16
        vmulps    {rn-sae}, %zmm13, %zmm14, %zmm0               #3155.19
        vcmpps    $26, {sae}, %zmm15, %zmm8, %k4                #3162.106
        vcmpps    $22, {sae}, %zmm15, %zmm8, %k5                #3163.109
        vsubps    {rn-sae}, %zmm14, %zmm5, %zmm5                #3150.19
        vaddps    {rn-sae}, %zmm5, %zmm2, %zmm4                 #3151.20
        vmovups   .L_2il0floatpacket.52(%rip), %zmm2            #3172.21
        vaddps    {rn-sae}, %zmm7, %zmm4, %zmm1                 #3152.13
        vmovups   .L_2il0floatpacket.49(%rip), %zmm7            #3154.21
        vmovaps   %zmm13, %zmm12                                #3156.17
        vfmsub213ps {rn-sae}, %zmm0, %zmm14, %zmm12             #3156.17
        vfmadd213ps {rn-sae}, %zmm12, %zmm7, %zmm14             #3157.17
        vmovaps   %zmm6, %zmm12                                 #3162.38
        vfmadd213ps {rn-sae}, %zmm14, %zmm13, %zmm1             #3158.17
        vpandnd   %zmm8, %zmm8, %zmm12{%k4}                     #3162.38
        vpandnd   %zmm8, %zmm8, %zmm6{%k5}                      #3163.41
        vandps    %zmm11, %zmm6, %zmm15                         #3164.24
        vandps    %zmm0, %zmm12, %zmm7                          #3165.25
        vandps    %zmm1, %zmm12, %zmm8                          #3167.9
        vmovups   .L_2il0floatpacket.53(%rip), %zmm0            #3179.19
        vorps     %zmm7, %zmm15, %zmm6                          #3166.9
        vpsrld    $31, %zmm6, %zmm3                             #3171.22
        vpsubd    %zmm3, %zmm2, %zmm4                           #3172.21
        vpaddd    %zmm4, %zmm10, %zmm5                          #3173.21
        vpsrld    $2, %zmm5, %zmm14                             #3174.23
        vpslld    $2, %zmm14, %zmm13                            #3176.23
        vblendmps %zmm14, %zmm9, %zmm9{%k6}                     #3186.17
        vpsubd    %zmm13, %zmm10, %zmm10                        #3177.21
        vcvtdq2ps {rn-sae}, %zmm10, %zmm2                       #3178.17
        vmovups   .L_2il0floatpacket.54(%rip), %zmm10           #3180.19
        vmovaps   %zmm0, %zmm1                                  #3181.26
        vfmadd213ps {rn-sae}, %zmm6, %zmm2, %zmm1               #3181.26
        vfnmadd213ps {rn-sae}, %zmm1, %zmm2, %zmm0              #3182.27
        vsubps    {rn-sae}, %zmm0, %zmm6, %zmm6                 #3183.29
        vmovups   (%rsp), %zmm0                                 #3187.17[spill]
        vfmadd213ps {rn-sae}, %zmm6, %zmm10, %zmm2              #3184.27
        vblendmps %zmm1, %zmm0, %zmm0{%k6}                      #3187.17
        vmovups   64(%rsp), %zmm1                               #3188.18[spill]
        vaddps    {rn-sae}, %zmm8, %zmm2, %zmm1{%k6}            #3188.18
        jmp       ..B3.2        # Prob 100%                     #3188.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 esi zmm0 zmm1 zmm9 zmm11
	.cfi_endproc
# mark_end;
	.type	__svml_tanf16_ha_z0,@function
	.size	__svml_tanf16_ha_z0,.-__svml_tanf16_ha_z0
..LN__svml_tanf16_ha_z0.2:
	.data
# -- End  __svml_tanf16_ha_z0
	.text
.L_2__routine_start___svml_tanf8_ha_e9_3:
# -- Begin  __svml_tanf8_ha_e9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf8_ha_e9
# --- __svml_tanf8_ha_e9(__m256)
__svml_tanf8_ha_e9:
# parameter 1: %ymm0
..B4.1:                         # Preds ..B4.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3232.1
        .byte     15                                            #3663.22
        .byte     30                                            #3663.22
        .byte     250                                           #3663.22
	.cfi_startproc
..___tag_value___svml_tanf8_ha_e9.122:
..L123:
                                                        #3232.1
        pushq     %rbp                                          #3232.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3232.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3232.1
        pushq     %r12                                          #3232.1
        subq      $376, %rsp                                    #3232.1
        xorl      %eax, %eax                                    #3276.9
        vmovups   960+__svml_stan_ha_data_internal(%rip), %ymm1 #3277.56
        vandps    %ymm1, %ymm0, %ymm10                          #3278.20
        vandnps   %ymm0, %ymm1, %ymm9                           #3279.21
        vcmpnle_uqps 1216+__svml_stan_ha_data_internal(%rip), %ymm10, %ymm1 #3281.34
        vextractf128 $1, %ymm10, %xmm12                         #3284.114
        vmulps    832+__svml_stan_ha_data_internal(%rip), %ymm10, %ymm2 #3287.32
        vcvtps2pd %xmm12, %ymm13                                #3284.97
        vcvtps2pd %xmm10, %ymm3                                 #3284.26
        vmovups   1088+__svml_stan_ha_data_internal(%rip), %ymm12 #3286.57
        vaddps    %ymm2, %ymm12, %ymm4                          #3287.17
        vpslld    $31, %xmm4, %xmm5                             #3288.84
        vextractf128 $1, %ymm4, %xmm8                           #3288.215
        vsubps    %ymm12, %ymm4, %ymm6                          #3289.17
        vpxor     %xmm12, %xmm12, %xmm12                        #3292.29
        vpslld    $31, %xmm8, %xmm7                             #3288.181
        vpcmpeqd  %xmm12, %xmm7, %xmm8                          #3292.100
        vpshufd   $80, %xmm8, %xmm14                            #3293.193
        vpshufd   $250, %xmm8, %xmm15                           #3293.275
        vpcmpeqd  %xmm12, %xmm5, %xmm4                          #3292.29
        vinsertf128 $1, %xmm15, %ymm14, %ymm8                   #3294.176
        vmovupd   1280+__svml_stan_ha_data_internal(%rip), %ymm15 #3296.55
        vpshufd   $80, %xmm4, %xmm2                             #3293.29
        vinsertf128 $1, %xmm7, %ymm5, %ymm10                    #3288.20
        vpshufd   $250, %xmm4, %xmm5                            #3293.111
        vxorps    %ymm9, %ymm10, %ymm9                          #3313.20
        vcvtps2pd %xmm6, %ymm14                                 #3295.23
        vmulpd    %ymm15, %ymm14, %ymm4                         #3297.46
        vinsertf128 $1, %xmm5, %ymm2, %ymm7                     #3294.25
        vextractf128 $1, %ymm6, %xmm2                           #3295.105
        vcvtps2pd %xmm2, %ymm5                                  #3295.88
        vsubpd    %ymm4, %ymm3, %ymm2                           #3297.19
        vmulpd    %ymm15, %ymm5, %ymm3                          #3297.121
        vsubpd    %ymm3, %ymm13, %ymm6                          #3297.94
        vmovupd   1344+__svml_stan_ha_data_internal(%rip), %ymm3 #3298.55
        vmulpd    %ymm3, %ymm14, %ymm13                         #3299.43
        vsubpd    %ymm13, %ymm2, %ymm4                          #3299.19
        vmulpd    %ymm3, %ymm5, %ymm2                           #3299.115
        vmovupd   1472+__svml_stan_ha_data_internal(%rip), %ymm5 #3301.54
        vmulpd    %ymm4, %ymm4, %ymm14                          #3300.20
        vmovupd   1600+__svml_stan_ha_data_internal(%rip), %ymm3 #3305.54
        vsubpd    %ymm2, %ymm6, %ymm15                          #3299.91
        vmulpd    %ymm4, %ymm5, %ymm6                           #3302.19
        vmulpd    %ymm15, %ymm15, %ymm2                         #3300.66
        vmulpd    %ymm15, %ymm5, %ymm5                          #3302.65
        vmulpd    %ymm6, %ymm14, %ymm13                         #3303.34
        vaddpd    %ymm13, %ymm4, %ymm6                          #3303.19
        vmovupd   1664+__svml_stan_ha_data_internal(%rip), %ymm13 #3304.54
        vmulpd    %ymm5, %ymm2, %ymm4                           #3303.106
        vaddpd    %ymm4, %ymm15, %ymm5                          #3303.91
        vmulpd    %ymm14, %ymm13, %ymm15                        #3306.34
        vmulpd    %ymm2, %ymm13, %ymm4                          #3306.108
        vaddpd    %ymm15, %ymm3, %ymm15                         #3306.19
        vaddpd    %ymm4, %ymm3, %ymm3                           #3306.93
        vmovupd   1536+__svml_stan_ha_data_internal(%rip), %ymm4 #3307.54
        vmulpd    %ymm15, %ymm14, %ymm14                        #3308.34
        vmulpd    %ymm3, %ymm2, %ymm2                           #3308.107
        vaddpd    %ymm14, %ymm4, %ymm14                         #3308.19
        vaddpd    %ymm2, %ymm4, %ymm3                           #3308.92
        vblendvpd %ymm7, %ymm6, %ymm14, %ymm4                   #3309.21
        vblendvpd %ymm7, %ymm14, %ymm6, %ymm7                   #3310.21
        vblendvpd %ymm8, %ymm5, %ymm3, %ymm2                    #3309.86
        vblendvpd %ymm8, %ymm3, %ymm5, %ymm8                    #3310.86
        vdivpd    %ymm7, %ymm4, %ymm3                           #3311.21
        vdivpd    %ymm8, %ymm2, %ymm4                           #3311.72
        vcvtpd2ps %ymm3, %xmm2                                  #3312.65
        vcvtpd2ps %ymm4, %xmm5                                  #3312.101
        vextractf128 $1, %ymm1, %xmm11                          #3282.159
        vpackssdw %xmm11, %xmm1, %xmm11                         #3283.121
        vinsertf128 $1, %xmm5, %ymm2, %ymm6                     #3312.19
        vpacksswb %xmm12, %xmm11, %xmm2                         #3283.104
        vpmovmskb %xmm2, %edx                                   #3283.85
        vxorps    %ymm9, %ymm6, %ymm4                           #3314.18
        testb     %dl, %dl                                      #3315.70
        jne       ..B4.12       # Prob 5%                       #3315.70
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r13 r14 r15 eax xmm12 ymm0 ymm1 ymm4
..B4.2:                         # Preds ..B4.16 ..B4.1
                                # Execution count [1.00e+00]
        testb     %al, %al                                      #3666.52
        jne       ..B4.4        # Prob 5%                       #3666.52
                                # LOE rbx r13 r14 r15 eax ymm0 ymm4
..B4.3:                         # Preds ..B4.4 ..B4.10 ..B4.2
                                # Execution count [1.00e+00]
        vmovaps   %ymm4, %ymm0                                  #3669.12
        addq      $376, %rsp                                    #3669.12
	.cfi_restore 12
        popq      %r12                                          #3669.12
        movq      %rbp, %rsp                                    #3669.12
        popq      %rbp                                          #3669.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #3669.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B4.4:                         # Preds ..B4.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, 64(%rsp)                               #3666.196
        vmovups   %ymm4, 128(%rsp)                              #3666.270
        testl     %eax, %eax                                    #3666.374
        je        ..B4.3        # Prob 95%                      #3666.374
                                # LOE rbx r13 r14 r15 eax ymm4
..B4.7:                         # Preds ..B4.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %r12d, %r12d                                  #3666.454
                                # LOE rbx r12 r13 r14 r15 eax
..B4.15:                        # Preds ..B4.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r13, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x80, 0xfe, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #
                                # LOE rbx r12 r14 r15 r13d
..B4.8:                         # Preds ..B4.9 ..B4.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #3666.517
        jc        ..B4.11       # Prob 5%                       #3666.517
                                # LOE rbx r12 r14 r15 r13d
..B4.9:                         # Preds ..B4.11 ..B4.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #3666.470
        cmpl      $8, %r12d                                     #3666.465
        jl        ..B4.8        # Prob 82%                      #3666.465
                                # LOE rbx r12 r14 r15 r13d
..B4.10:                        # Preds ..B4.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        vmovups   128(%rsp), %ymm4                              #3666.674
        jmp       ..B4.3        # Prob 100%                     #3666.674
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x80, 0xfe, 0xff, 0xff, 0x22
                                # LOE rbx r13 r14 r15 ymm4
..B4.11:                        # Preds ..B4.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #3666.546
        lea       128(%rsp,%r12,4), %rsi                        #3666.546
..___tag_value___svml_tanf8_ha_e9.138:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #3666.546
..___tag_value___svml_tanf8_ha_e9.139:
        jmp       ..B4.9        # Prob 100%                     #3666.546
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B4.12:                        # Preds ..B4.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm4, 96(%rsp)                               #[spill]
        movl      $2139095040, %r12d                            #3478.24
        vmovups   .L_2il0floatpacket.55(%rip), %ymm4            #3385.31
        vmovups   .L_2il0floatpacket.56(%rip), %ymm6            #3391.32
        vmovups   %ymm0, 32(%rsp)                               #[spill]
        vmovups   %ymm1, 64(%rsp)                               #[spill]
        vandps    %ymm4, %ymm0, %ymm13                          #3386.25
        lea       __svml_stan_ha_reduction_data_internal(%rip), %rdx #3481.1256
        vcmpeqps  %ymm4, %ymm13, %ymm9                          #3388.35
        vandps    %ymm6, %ymm0, %ymm7                           #3392.29
        vmovups   %ymm7, (%rsp)                                 #3392.29[spill]
        vextractf128 $1, %ymm9, %xmm2                           #3389.146
        vpackssdw %xmm2, %xmm9, %xmm15                          #3390.91
        vpacksswb %xmm12, %xmm15, %xmm12                        #3390.74
        vandnps   %ymm0, %ymm6, %ymm15                          #3393.28
        vmovd     %r12d, %xmm0                                  #3478.24
        vmovups   %ymm15, 128(%rsp)                             #3393.28[spill]
        vpshufd   $0, %xmm0, %xmm10                             #3478.24
        vpmovmskb %xmm12, %eax                                  #3390.55
        vpand     %xmm15, %xmm10, %xmm8                         #3479.22
        vextractf128 $1, %ymm15, %xmm11                         #3477.111
        vpsrld    $23, %xmm8, %xmm1                             #3480.22
        vpslld    $1, %xmm1, %xmm3                              #3481.238
        vpaddd    %xmm1, %xmm3, %xmm14                          #3481.543
        vpand     %xmm11, %xmm10, %xmm5                         #3479.97
        vpslld    $2, %xmm14, %xmm4                             #3481.543
        vpsrld    $23, %xmm5, %xmm13                            #3480.85
        vpslld    $1, %xmm13, %xmm12                            #3481.304
        vpaddd    %xmm13, %xmm12, %xmm0                         #3481.612
        vpslld    $2, %xmm0, %xmm5                              #3481.612
        vmovd     %xmm4, %ecx                                   #3481.686
        vmovd     %xmm5, %r9d                                   #3481.967
        vmovups   %xmm11, 176(%rsp)                             #3477.111[spill]
        vmovd     (%rcx,%rdx), %xmm9                            #3481.2344
        vpextrd   $1, %xmm4, %esi                               #3481.754
        vpextrd   $2, %xmm4, %edi                               #3481.825
        vpextrd   $3, %xmm4, %r8d                               #3481.896
        vpextrd   $1, %xmm5, %r11d                              #3481.1035
        vpextrd   $2, %xmm5, %r10d                              #3481.1106
        vpextrd   $3, %xmm5, %r12d                              #3481.1177
        vmovd     (%rsi,%rdx), %xmm2                            #3481.2433
        vmovd     (%rdi,%rdx), %xmm7                            #3481.2543
        vmovd     (%r8,%rdx), %xmm6                             #3481.2632
        vmovd     (%r9,%rdx), %xmm3                             #3481.2785
        vmovd     (%r11,%rdx), %xmm1                            #3481.2874
        vmovd     (%r10,%rdx), %xmm14                           #3481.2984
        vmovd     (%r12,%rdx), %xmm13                           #3481.3073
        vpunpcklqdq %xmm2, %xmm9, %xmm8                         #3481.2324
        vpunpcklqdq %xmm6, %xmm7, %xmm10                        #3481.2523
        vpunpcklqdq %xmm1, %xmm3, %xmm0                         #3481.2765
        vpunpcklqdq %xmm13, %xmm14, %xmm4                       #3481.2964
        vmovd     4(%r12,%rdx), %xmm14                          #3482.3074
        vmovd     4(%rcx,%rdx), %xmm9                           #3482.2345
        vmovd     4(%rsi,%rdx), %xmm2                           #3482.2434
        vmovd     4(%rdi,%rdx), %xmm7                           #3482.2544
        vmovd     4(%r8,%rdx), %xmm6                            #3482.2633
        vmovd     4(%r9,%rdx), %xmm5                            #3482.2786
        vmovd     4(%r11,%rdx), %xmm3                           #3482.2875
        vmovd     4(%r10,%rdx), %xmm1                           #3482.2985
        vshufps   $136, %xmm10, %xmm8, %xmm12                   #3481.2304
        vshufps   $136, %xmm4, %xmm0, %xmm13                    #3481.2745
        vpunpcklqdq %xmm2, %xmm9, %xmm8                         #3482.2325
        vpunpcklqdq %xmm6, %xmm7, %xmm10                        #3482.2524
        vpunpcklqdq %xmm3, %xmm5, %xmm0                         #3482.2766
        vpunpcklqdq %xmm14, %xmm1, %xmm4                        #3482.2965
        vmovd     8(%rcx,%rdx), %xmm2                           #3483.2344
        movl      $262143, %ecx                                 #3560.26
        vmovd     8(%rsi,%rdx), %xmm7                           #3483.2433
        movl      $679477248, %esi                              #3558.28
        vshufps   $136, %xmm10, %xmm8, %xmm10                   #3482.2305
        vshufps   $136, %xmm4, %xmm0, %xmm9                     #3482.2746
        vpunpcklqdq %xmm7, %xmm2, %xmm5                         #3483.2324
        vmovd     8(%rdi,%rdx), %xmm6                           #3483.2543
        movl      $255, %edi                                    #3556.21
        vmovd     8(%r8,%rdx), %xmm8                            #3483.2632
        movl      $1065353216, %r8d                             #3546.28
        vmovd     8(%r9,%rdx), %xmm14                           #3483.2785
        movl      $-2147483648, %r9d                            #3544.23
        vmovd     8(%r11,%rdx), %xmm0                           #3483.2874
        movl      $65535, %r11d                                 #3491.23
        vmovd     8(%r10,%rdx), %xmm4                           #3483.2984
        movl      $8388608, %r10d                               #3488.26
        vmovd     8(%r12,%rdx), %xmm2                           #3483.3073
        movl      $8388607, %edx                                #3487.27
        vpunpcklqdq %xmm8, %xmm6, %xmm3                         #3483.2523
        movl      $511, %r12d                                   #3569.26
        vshufps   $136, %xmm3, %xmm5, %xmm1                     #3483.2304
        vpunpcklqdq %xmm0, %xmm14, %xmm7                        #3483.2765
        vmovd     %r10d, %xmm5                                  #3488.26
        vmovd     %edx, %xmm8                                   #3487.27
        movl      $872415232, %edx                              #3567.28
        vpshufd   $0, %xmm8, %xmm0                              #3487.27
        vpsrld    $16, %xmm9, %xmm8                             #3494.70
        vpunpcklqdq %xmm2, %xmm4, %xmm6                         #3483.2964
        vpand     %xmm11, %xmm0, %xmm11                         #3489.106
        vpshufd   $0, %xmm5, %xmm4                              #3488.26
        vpsrld    $16, %xmm1, %xmm2                             #3496.16
        vmovups   %xmm15, 160(%rsp)                             #3477.37[spill]
        vpand     %xmm15, %xmm0, %xmm15                         #3489.25
        vshufps   $136, %xmm6, %xmm7, %xmm3                     #3483.2745
        vpaddd    %xmm4, %xmm11, %xmm6                          #3490.111
        vmovd     %r11d, %xmm11                                 #3491.23
        vpaddd    %xmm4, %xmm15, %xmm14                         #3490.25
        vpshufd   $0, %xmm11, %xmm15                            #3491.23
        vpsrld    $16, %xmm10, %xmm11                           #3494.16
        vmovups   %xmm12, 192(%rsp)                             #3481.2304[spill]
        vpand     %xmm15, %xmm12, %xmm12                        #3493.16
        vpand     %xmm15, %xmm9, %xmm0                          #3495.84
        vpand     %xmm15, %xmm14, %xmm9                         #3499.16
        vpand     %xmm15, %xmm13, %xmm7                         #3493.83
        vpand     %xmm15, %xmm10, %xmm5                         #3495.16
        vpsrld    $16, %xmm14, %xmm10                           #3498.16
        vpand     %xmm15, %xmm6, %xmm14                         #3499.90
        vmovdqu   %xmm12, 224(%rsp)                             #3493.16[spill]
        vpand     %xmm15, %xmm1, %xmm1                          #3497.16
        vpmulld   %xmm12, %xmm9, %xmm12                         #3507.17
        vpsrld    $16, %xmm3, %xmm4                             #3496.69
        vmovdqu   %xmm7, 240(%rsp)                              #3493.83[spill]
        vpand     %xmm15, %xmm3, %xmm3                          #3497.83
        vmovdqu   %xmm12, 304(%rsp)                             #3507.17[spill]
        movl      $127, %r10d                                   #3610.47
        vpmulld   %xmm7, %xmm14, %xmm12                         #3507.80
        vpmulld   %xmm2, %xmm10, %xmm7                          #3504.17
        vpmulld   %xmm2, %xmm9, %xmm2                           #3510.17
        vmovups   %xmm13, 208(%rsp)                             #3481.2745[spill]
        vpsrld    $16, %xmm6, %xmm13                            #3498.76
        vmovdqu   %xmm8, 272(%rsp)                              #3494.70[spill]
        vpsrld    $16, %xmm2, %xmm2                             #3512.17
        vmovdqu   %xmm0, 288(%rsp)                              #3495.84[spill]
        vpaddd    %xmm2, %xmm7, %xmm7                           #3516.17
        vmovdqu   %xmm12, 320(%rsp)                             #3507.80[spill]
        vpmulld   %xmm8, %xmm14, %xmm12                         #3508.80
        vpmulld   %xmm5, %xmm9, %xmm6                           #3509.17
        vpmulld   %xmm0, %xmm14, %xmm8                          #3509.80
        vpmulld   %xmm1, %xmm10, %xmm0                          #3505.17
        vpsrld    $16, %xmm0, %xmm1                             #3530.17
        vpand     %xmm15, %xmm6, %xmm0                          #3520.17
        vpaddd    %xmm7, %xmm0, %xmm0                           #3525.17
        vpsrld    $16, %xmm6, %xmm6                             #3513.17
        vpaddd    %xmm0, %xmm1, %xmm7                           #3531.17
        vpand     %xmm15, %xmm8, %xmm1                          #3520.84
        vpmulld   %xmm3, %xmm13, %xmm0                          #3505.80
        vpsrld    $16, %xmm8, %xmm8                             #3513.70
        vpmulld   %xmm4, %xmm13, %xmm3                          #3504.80
        vpsrld    $16, %xmm0, %xmm0                             #3530.70
        vpmulld   %xmm4, %xmm14, %xmm4                          #3510.80
        vpsrld    $16, %xmm4, %xmm4                             #3512.70
        vpaddd    %xmm4, %xmm3, %xmm2                           #3516.80
        vmovdqu   %xmm11, 256(%rsp)                             #3494.16[spill]
        vpaddd    %xmm2, %xmm1, %xmm1                           #3525.80
        vpmulld   %xmm11, %xmm9, %xmm11                         #3508.17
        vpaddd    %xmm1, %xmm0, %xmm4                           #3531.80
        vpmulld   %xmm5, %xmm10, %xmm5                          #3503.17
        vpand     %xmm15, %xmm11, %xmm0                         #3521.17
        vpaddd    %xmm6, %xmm5, %xmm6                           #3517.17
        vpsrld    $16, %xmm7, %xmm2                             #3532.17
        vpaddd    %xmm6, %xmm0, %xmm0                           #3526.17
        vpand     %xmm15, %xmm12, %xmm5                         #3521.84
        vpaddd    %xmm0, %xmm2, %xmm6                           #3533.17
        vpsrld    $16, %xmm4, %xmm3                             #3532.70
        vpmulld   288(%rsp), %xmm13, %xmm2                      #3503.80[spill]
        vpsrld    $16, %xmm11, %xmm11                           #3514.17
        vpaddd    %xmm8, %xmm2, %xmm0                           #3517.80
        vpsrld    $16, %xmm6, %xmm1                             #3534.17
        vpaddd    %xmm0, %xmm5, %xmm2                           #3526.80
        vpsrld    $16, %xmm12, %xmm12                           #3514.70
        vpmulld   256(%rsp), %xmm10, %xmm0                      #3502.17[spill]
        vpaddd    %xmm2, %xmm3, %xmm8                           #3533.80
        vmovdqu   304(%rsp), %xmm3                              #3522.17[spill]
        vpaddd    %xmm11, %xmm0, %xmm11                         #3518.17
        vpand     %xmm15, %xmm3, %xmm5                          #3522.17
        vpsrld    $16, %xmm8, %xmm2                             #3534.70
        vpaddd    %xmm11, %xmm5, %xmm0                          #3527.17
        vpslld    $16, %xmm6, %xmm6                             #3541.23
        vpmulld   272(%rsp), %xmm13, %xmm11                     #3502.80[spill]
        vpaddd    %xmm0, %xmm1, %xmm5                           #3535.17
        vmovdqu   320(%rsp), %xmm1                              #3522.84[spill]
        vpaddd    %xmm12, %xmm11, %xmm12                        #3518.80
        vpand     %xmm15, %xmm1, %xmm0                          #3522.84
        vpand     %xmm15, %xmm7, %xmm7                          #3538.17
        vpaddd    %xmm12, %xmm0, %xmm11                         #3527.80
        vpsrld    $16, %xmm5, %xmm12                            #3536.17
        vmovups   192(%rsp), %xmm0                              #3492.16[spill]
        vpaddd    %xmm11, %xmm2, %xmm2                          #3535.80
        vpsrld    $16, %xmm0, %xmm11                            #3492.16
        vpsrld    $16, %xmm3, %xmm0                             #3515.17
        vpmulld   224(%rsp), %xmm10, %xmm10                     #3501.17[spill]
        vpand     %xmm15, %xmm5, %xmm5                          #3539.17
        vpmulld   %xmm11, %xmm9, %xmm9                          #3506.17
        vpslld    $16, %xmm8, %xmm8                             #3541.82
        vpand     %xmm15, %xmm9, %xmm11                         #3523.17
        vpaddd    %xmm0, %xmm10, %xmm9                          #3540.22
        vpaddd    %xmm9, %xmm11, %xmm11                         #3540.22
        vpsrld    $16, %xmm2, %xmm0                             #3536.70
        vmovups   208(%rsp), %xmm3                              #3492.69[spill]
        vpaddd    %xmm11, %xmm12, %xmm12                        #3540.22
        vpslld    $16, %xmm12, %xmm10                           #3540.22
        vpsrld    $16, %xmm3, %xmm12                            #3492.69
        vpmulld   240(%rsp), %xmm13, %xmm13                     #3501.80[spill]
        vpaddd    %xmm5, %xmm10, %xmm11                         #3542.22
        vpmulld   %xmm12, %xmm14, %xmm14                        #3506.80
        vpsrld    $16, %xmm1, %xmm12                            #3515.70
        vpand     %xmm15, %xmm14, %xmm9                         #3523.84
        vpaddd    %xmm12, %xmm13, %xmm13                        #3540.80
        vpaddd    %xmm13, %xmm9, %xmm10                         #3540.80
        vpand     %xmm15, %xmm2, %xmm2                          #3539.84
        vpaddd    %xmm10, %xmm0, %xmm0                          #3540.80
        vpand     %xmm15, %xmm4, %xmm15                         #3538.84
        vpslld    $16, %xmm0, %xmm5                             #3540.80
        vmovd     %r9d, %xmm0                                   #3544.23
        vpshufd   $0, %xmm0, %xmm4                              #3544.23
        vmovd     %r8d, %xmm9                                   #3546.28
        vmovups   .L_2il0floatpacket.57(%rip), %ymm0            #3551.15
        vpaddd    %xmm2, %xmm5, %xmm12                          #3542.95
        vpshufd   $0, %xmm9, %xmm5                              #3546.28
        vpaddd    %xmm7, %xmm6, %xmm10                          #3543.23
        vpand     160(%rsp), %xmm4, %xmm6                       #3545.21[spill]
        vpaddd    %xmm15, %xmm8, %xmm13                         #3543.98
        vpand     176(%rsp), %xmm4, %xmm7                       #3545.94[spill]
        vpsrld    $9, %xmm11, %xmm2                             #3548.24
        vpxor     %xmm5, %xmm6, %xmm15                          #3547.26
        vpsrld    $9, %xmm12, %xmm3                             #3548.88
        vpxor     %xmm5, %xmm7, %xmm1                           #3547.111
        vpor      %xmm15, %xmm2, %xmm8                          #3549.24
        vpor      %xmm1, %xmm3, %xmm14                          #3549.109
        vmovd     %esi, %xmm5                                   #3558.28
        vpshufd   $0, %xmm5, %xmm1                              #3558.28
        vpxor     %xmm1, %xmm6, %xmm3                           #3559.26
        vpxor     %xmm1, %xmm7, %xmm5                           #3559.111
        vinsertf128 $1, %xmm14, %ymm8, %ymm4                    #3550.17
        vmovd     %ecx, %xmm14                                  #3560.26
        vaddps    %ymm0, %ymm4, %ymm9                           #3552.19
        vmovd     %edi, %xmm8                                   #3556.21
        vpshufd   $0, %xmm14, %xmm14                            #3560.26
        vsubps    %ymm0, %ymm9, %ymm2                           #3553.9
        vsubps    %ymm2, %ymm4, %ymm15                          #3554.17
        vpand     %xmm10, %xmm14, %xmm2                         #3561.24
        vpand     %xmm13, %xmm14, %xmm14                        #3561.107
        vpsrld    $18, %xmm13, %xmm13                           #3572.88
        vpshufd   $0, %xmm8, %xmm4                              #3556.21
        vpslld    $5, %xmm2, %xmm8                              #3562.24
        vpslld    $5, %xmm14, %xmm2                             #3562.90
        vpor      %xmm3, %xmm8, %xmm1                           #3563.24
        vpor      %xmm5, %xmm2, %xmm8                           #3563.109
        vinsertf128 $1, %xmm8, %ymm1, %ymm1                     #3564.17
        vmovd     %edx, %xmm8                                   #3567.28
        vinsertf128 $1, %xmm5, %ymm3, %ymm2                     #3565.15
        vmovd     %r12d, %xmm5                                  #3569.26
        vpshufd   $0, %xmm8, %xmm3                              #3567.28
        vpshufd   $0, %xmm5, %xmm8                              #3569.26
        vpxor     %xmm3, %xmm6, %xmm6                           #3568.26
        vpand     %xmm11, %xmm8, %xmm11                         #3570.24
        vpand     %xmm12, %xmm8, %xmm12                         #3570.106
        vsubps    %ymm2, %ymm1, %ymm2                           #3566.17
        vpslld    $14, %xmm11, %xmm11                           #3571.24
        vpsrld    $18, %xmm10, %xmm1                            #3572.23
        vpslld    $14, %xmm12, %xmm8                            #3571.91
        vpxor     %xmm3, %xmm7, %xmm7                           #3568.111
        vpor      %xmm1, %xmm11, %xmm3                          #3573.24
        vpor      %xmm13, %xmm8, %xmm10                         #3573.106
        vpor      %xmm6, %xmm3, %xmm5                           #3574.24
        vpor      %xmm7, %xmm10, %xmm14                         #3574.109
        vinsertf128 $1, %xmm14, %ymm5, %ymm12                   #3575.18
        vinsertf128 $1, %xmm7, %ymm6, %ymm7                     #3576.15
        vsubps    %ymm7, %ymm12, %ymm12                         #3577.18
        vmovups   .L_2il0floatpacket.60(%rip), %ymm6            #3584.21
        vaddps    %ymm12, %ymm15, %ymm1                         #3578.11
        vsubps    %ymm1, %ymm15, %ymm13                         #3579.17
        vaddps    %ymm13, %ymm12, %ymm11                        #3580.18
        vaddps    %ymm11, %ymm2, %ymm5                          #3581.11
        vmovups   .L_2il0floatpacket.59(%rip), %ymm2            #3583.20
        vmulps    .L_2il0floatpacket.58(%rip), %ymm5, %ymm14    #3591.16
        vandps    .L_2il0floatpacket.61(%rip), %ymm1, %ymm3     #3586.20
        vsubps    %ymm3, %ymm1, %ymm7                           #3587.21
        vmulps    %ymm3, %ymm6, %ymm10                          #3590.16
        vmulps    %ymm3, %ymm2, %ymm13                          #3588.20
        vmulps    %ymm7, %ymm2, %ymm8                           #3589.16
        vmulps    %ymm7, %ymm6, %ymm15                          #3592.16
        vmovups   128(%rsp), %ymm6                              #3601.11[spill]
        vmovups   .L_2il0floatpacket.63(%rip), %ymm3            #3600.16
        vaddps    %ymm10, %ymm8, %ymm12                         #3593.17
        vaddps    %ymm15, %ymm14, %ymm11                        #3594.17
        vaddps    %ymm11, %ymm12, %ymm1                         #3595.17
        vmovd     %r10d, %xmm11                                 #3610.47
        vaddps    %ymm1, %ymm13, %ymm7                          #3596.17
        lea       1764+__svml_stan_ha_data_internal(%rip), %r10 #3612.1264
        vandps    .L_2il0floatpacket.62(%rip), %ymm6, %ymm2     #3601.11
        vsubps    %ymm7, %ymm13, %ymm13                         #3597.20
        vcmpgt_oqps %ymm3, %ymm2, %ymm14                        #3602.17
        vcmple_oqps %ymm3, %ymm2, %ymm5                         #3603.20
        vaddps    %ymm13, %ymm1, %ymm15                         #3598.17
        vpshufd   $0, %xmm11, %xmm1                             #3610.47
        vandps    %ymm15, %ymm14, %ymm13                        #3607.9
        vandps    %ymm6, %ymm5, %ymm8                           #3604.24
        vandps    %ymm7, %ymm14, %ymm10                         #3605.25
        vorps     %ymm10, %ymm8, %ymm12                         #3606.9
        vextractf128 $1, %ymm9, %xmm0                           #3555.118
        vpand     %xmm4, %xmm9, %xmm9                           #3557.19
        vpand     %xmm1, %xmm9, %xmm11                          #3611.35
        vpand     %xmm4, %xmm0, %xmm0                           #3557.88
        vpslld    $2, %xmm11, %xmm4                             #3612.258
        vpand     %xmm1, %xmm0, %xmm1                           #3611.114
        vpaddd    %xmm11, %xmm4, %xmm4                          #3612.551
        vpslld    $2, %xmm1, %xmm0                              #3612.321
        vpslld    $3, %xmm4, %xmm3                              #3612.551
        vpaddd    %xmm1, %xmm0, %xmm2                           #3612.620
        vpslld    $3, %xmm2, %xmm15                             #3612.620
        vmovd     %xmm3, %r11d                                  #3612.694
        vmovd     %xmm15, %esi                                  #3612.975
        vmovd     -36(%r11,%r10), %xmm5                         #3612.2280
        vpextrd   $1, %xmm3, %r9d                               #3612.762
        vpextrd   $2, %xmm3, %r8d                               #3612.833
        vpextrd   $3, %xmm3, %edi                               #3612.904
        vpextrd   $1, %xmm15, %ecx                              #3612.1043
        vpextrd   $2, %xmm15, %edx                              #3612.1114
        vpextrd   $3, %xmm15, %r12d                             #3612.1185
        vmovd     -36(%r9,%r10), %xmm6                          #3612.2369
        vmovd     -36(%r8,%r10), %xmm7                          #3612.2479
        vmovd     -36(%rdi,%r10), %xmm8                         #3612.2568
        vmovd     -36(%rsi,%r10), %xmm11                        #3612.2721
        vmovd     -36(%rcx,%r10), %xmm15                        #3612.2810
        vmovd     -36(%rdx,%r10), %xmm0                         #3612.2920
        vmovd     -36(%r12,%r10), %xmm1                         #3612.3009
        vpunpcklqdq %xmm6, %xmm5, %xmm9                         #3612.2260
        vpunpcklqdq %xmm8, %xmm7, %xmm10                        #3612.2459
        vpunpcklqdq %xmm15, %xmm11, %xmm2                       #3612.2701
        vpunpcklqdq %xmm1, %xmm0, %xmm3                         #3612.2900
        vshufps   $136, %xmm10, %xmm9, %xmm14                   #3612.2240
        vshufps   $136, %xmm3, %xmm2, %xmm4                     #3612.2681
        vmovd     -32(%r11,%r10), %xmm5                         #3613.2280
        vmovd     -32(%r9,%r10), %xmm6                          #3613.2369
        vmovd     -32(%r8,%r10), %xmm7                          #3613.2479
        vmovd     -32(%rdi,%r10), %xmm8                         #3613.2568
        vpunpcklqdq %xmm6, %xmm5, %xmm9                         #3613.2260
        vpunpcklqdq %xmm8, %xmm7, %xmm10                        #3613.2459
        vmovd     -32(%rsi,%r10), %xmm0                         #3613.2721
        vmovd     -32(%rcx,%r10), %xmm1                         #3613.2810
        vmovd     -32(%rdx,%r10), %xmm3                         #3613.2920
        vshufps   $136, %xmm10, %xmm9, %xmm2                    #3613.2240
        vpunpcklqdq %xmm1, %xmm0, %xmm5                         #3613.2701
        vmovups   64(%rsp), %ymm1                               #3613.2681[spill]
        vmovups   32(%rsp), %ymm0                               #3613.2681[spill]
        vinsertf128 $1, %xmm4, %ymm14, %ymm11                   #3612.2176
        vmovd     -32(%r12,%r10), %xmm4                         #3613.3009
        vpunpcklqdq %xmm4, %xmm3, %xmm6                         #3613.2900
        vshufps   $136, %xmm6, %xmm5, %xmm3                     #3613.2681
        vmovups   96(%rsp), %ymm4                               #3613.2681[spill]
                                # LOE rdx rcx rbx rsi rdi r8 r9 r10 r11 r12 r13 r14 r15 eax xmm3 ymm0 ymm1 ymm2 ymm4 ymm11 ymm12 ymm13
..B4.16:                        # Preds ..B4.12
                                # Execution count [5.00e-02]: Infreq
        vmovd     -28(%r11,%r10), %xmm9                         #3614.2279
        vmovd     -28(%r9,%r10), %xmm5                          #3614.2368
        vmovups   %ymm4, 96(%rsp)                               #[spill]
        vmovd     -28(%rsi,%r10), %xmm4                         #3614.2720
        vmovd     -28(%rcx,%r10), %xmm6                         #3614.2809
        vmovd     -28(%rdx,%r10), %xmm8                         #3614.2919
        vpunpcklqdq %xmm6, %xmm4, %xmm14                        #3614.2700
        vinsertf128 $1, %xmm3, %ymm2, %ymm15                    #3613.2176
        vpunpcklqdq %xmm5, %xmm9, %xmm3                         #3614.2259
        vmovd     -28(%r8,%r10), %xmm2                          #3614.2478
        vmovd     -28(%rdi,%r10), %xmm9                         #3614.2567
        vmovd     -28(%r12,%r10), %xmm10                        #3614.3008
        vpunpcklqdq %xmm9, %xmm2, %xmm5                         #3614.2458
        vmovups   %ymm0, 32(%rsp)                               #[spill]
        vpunpcklqdq %xmm10, %xmm8, %xmm0                        #3614.2899
        vshufps   $136, %xmm5, %xmm3, %xmm7                     #3614.2239
        vshufps   $136, %xmm0, %xmm14, %xmm2                    #3614.2680
        vmovd     -24(%r11,%r10), %xmm9                         #3615.2281
        vmovd     -24(%r9,%r10), %xmm3                          #3615.2370
        vmovd     -24(%r8,%r10), %xmm5                          #3615.2480
        vmovd     -24(%rdi,%r10), %xmm4                         #3615.2569
        vpunpcklqdq %xmm3, %xmm9, %xmm6                         #3615.2261
        vmovd     -24(%rsi,%r10), %xmm0                         #3615.2722
        vmovd     -24(%rdx,%r10), %xmm9                         #3615.2921
        vmovd     -24(%r12,%r10), %xmm3                         #3615.3010
        vpunpcklqdq %xmm4, %xmm5, %xmm8                         #3615.2460
        vshufps   $136, %xmm8, %xmm6, %xmm14                    #3615.2241
        vmovd     -20(%r11,%r10), %xmm6                         #3616.2281
        vmovd     -20(%r9,%r10), %xmm8                          #3616.2370
        vinsertf128 $1, %xmm2, %ymm7, %ymm10                    #3614.2175
        vmovd     -24(%rcx,%r10), %xmm2                         #3615.2811
        vpunpcklqdq %xmm2, %xmm0, %xmm5                         #3615.2702
        vpunpcklqdq %xmm3, %xmm9, %xmm7                         #3615.2901
        vshufps   $136, %xmm7, %xmm5, %xmm4                     #3615.2682
        vmovd     -20(%r8,%r10), %xmm0                          #3616.2480
        vmovd     -20(%rdi,%r10), %xmm2                         #3616.2569
        vpunpcklqdq %xmm8, %xmm6, %xmm9                         #3616.2261
        vmovd     -20(%rcx,%r10), %xmm6                         #3616.2811
        vmovd     -20(%rdx,%r10), %xmm8                         #3616.2921
        vpunpcklqdq %xmm2, %xmm0, %xmm5                         #3616.2460
        vshufps   $136, %xmm5, %xmm9, %xmm7                     #3616.2241
        vmovd     -16(%r11,%r10), %xmm5                         #3617.2281
        vinsertf128 $1, %xmm4, %ymm14, %ymm3                    #3615.2177
        vmovd     -20(%rsi,%r10), %xmm4                         #3616.2722
        vmovd     -20(%r12,%r10), %xmm14                        #3616.3010
        vpunpcklqdq %xmm6, %xmm4, %xmm0                         #3616.2702
        vpunpcklqdq %xmm14, %xmm8, %xmm2                        #3616.2901
        vshufps   $136, %xmm2, %xmm0, %xmm9                     #3616.2682
        vmovd     -16(%r9,%r10), %xmm4                          #3617.2370
        vmovd     -16(%r8,%r10), %xmm6                          #3617.2480
        vmovd     -16(%rdi,%r10), %xmm8                         #3617.2569
        vpunpcklqdq %xmm4, %xmm5, %xmm14                        #3617.2261
        vpunpcklqdq %xmm8, %xmm6, %xmm0                         #3617.2460
        vmovd     -16(%rsi,%r10), %xmm5                         #3617.2722
        vmovd     -16(%rdx,%r10), %xmm4                         #3617.2921
        vmovd     -16(%r12,%r10), %xmm6                         #3617.3010
        vshufps   $136, %xmm0, %xmm14, %xmm2                    #3617.2241
        vpunpcklqdq %xmm6, %xmm4, %xmm14                        #3617.2901
        vmovd     -12(%r8,%r10), %xmm4                          #3618.2480
        vmovd     -12(%rdi,%r10), %xmm6                         #3618.2569
        vinsertf128 $1, %xmm9, %ymm7, %ymm9                     #3616.2177
        vmovd     -16(%rcx,%r10), %xmm7                         #3617.2811
        vpunpcklqdq %xmm7, %xmm5, %xmm8                         #3617.2702
        vshufps   $136, %xmm14, %xmm8, %xmm0                    #3617.2682
        vmovd     -12(%r9,%r10), %xmm7                          #3618.2370
        vpunpcklqdq %xmm6, %xmm4, %xmm14                        #3618.2460
        vmovd     -12(%rdx,%r10), %xmm4                         #3618.2921
        vmovd     -12(%r12,%r10), %xmm6                         #3618.3010
        vinsertf128 $1, %xmm0, %ymm2, %ymm5                     #3617.2177
        vmovd     -12(%r11,%r10), %xmm2                         #3618.2281
        vpunpcklqdq %xmm7, %xmm2, %xmm8                         #3618.2261
        vmovd     -12(%rsi,%r10), %xmm0                         #3618.2722
        vmovd     -12(%rcx,%r10), %xmm2                         #3618.2811
        vshufps   $136, %xmm14, %xmm8, %xmm7                    #3618.2241
        vpunpcklqdq %xmm2, %xmm0, %xmm8                         #3618.2702
        vpunpcklqdq %xmm6, %xmm4, %xmm14                        #3618.2901
        vshufps   $136, %xmm14, %xmm8, %xmm0                    #3618.2682
        vmovd     -8(%r11,%r10), %xmm2                          #3619.2278
        vmovd     -8(%r9,%r10), %xmm4                           #3619.2367
        vmovd     -8(%r8,%r10), %xmm6                           #3619.2477
        vmovd     -8(%rdi,%r10), %xmm8                          #3619.2566
        vpunpcklqdq %xmm4, %xmm2, %xmm14                        #3619.2258
        vmovd     -8(%rsi,%r10), %xmm2                          #3619.2719
        vmovd     -8(%rcx,%r10), %xmm4                          #3619.2808
        vinsertf128 $1, %xmm0, %ymm7, %ymm7                     #3618.2177
        vpunpcklqdq %xmm8, %xmm6, %xmm0                         #3619.2457
        vshufps   $136, %xmm0, %xmm14, %xmm8                    #3619.2238
        vmovd     -8(%rdx,%r10), %xmm6                          #3619.2918
        vmovd     -8(%r12,%r10), %xmm14                         #3619.3007
        vpunpcklqdq %xmm4, %xmm2, %xmm0                         #3619.2699
        vpunpcklqdq %xmm14, %xmm6, %xmm2                        #3619.2898
        vshufps   $136, %xmm2, %xmm0, %xmm4                     #3619.2679
        vmovd     -4(%r11,%r10), %xmm6                          #3620.2278
        vmovd     -4(%r9,%r10), %xmm14                          #3620.2367
        vmovd     -4(%r8,%r10), %xmm0                           #3620.2477
        vmovd     -4(%rdi,%r10), %xmm2                          #3620.2566
        vinsertf128 $1, %xmm4, %ymm8, %ymm8                     #3619.2174
        vpunpcklqdq %xmm14, %xmm6, %xmm4                        #3620.2258
        vpunpcklqdq %xmm2, %xmm0, %xmm6                         #3620.2457
        vshufps   $136, %xmm6, %xmm4, %xmm4                     #3620.2238
        vmovd     -4(%rsi,%r10), %xmm14                         #3620.2719
        vmovd     -4(%rcx,%r10), %xmm0                          #3620.2808
        vmovd     -4(%rdx,%r10), %xmm2                          #3620.2898
        vmovd     -4(%r12,%r10), %xmm6                          #3620.2898
        vpunpcklqdq %xmm0, %xmm14, %xmm14                       #3620.2699
        vpunpcklqdq %xmm6, %xmm2, %xmm0                         #3620.2898
        vshufps   $136, %xmm0, %xmm14, %xmm2                    #3620.2679
        vmovd     (%r11,%r10), %xmm6                            #3621.2278
        vmovd     (%r9,%r10), %xmm14                            #3621.2367
        vmovd     (%r8,%r10), %xmm0                             #3621.2477
        vpunpcklqdq %xmm14, %xmm6, %xmm6                        #3621.2258
        vmulps    %ymm8, %ymm12, %ymm8                          #3653.41
        vinsertf128 $1, %xmm2, %ymm4, %ymm4                     #3620.2174
        vmovd     (%rdi,%r10), %xmm2                            #3621.2566
        vpunpcklqdq %xmm2, %xmm0, %xmm14                        #3621.2457
        vshufps   $136, %xmm14, %xmm6, %xmm2                    #3621.2238
        vmovd     (%rsi,%r10), %xmm0                            #3621.2719
        vmovd     (%rcx,%r10), %xmm6                            #3621.2808
        vpunpcklqdq %xmm6, %xmm0, %xmm6                         #3621.2699
        vmovd     (%rdx,%r10), %xmm14                           #3621.2918
        vmovd     (%r12,%r10), %xmm0                            #3621.3007
        vpunpcklqdq %xmm0, %xmm14, %xmm14                       #3621.2898
        vshufps   $136, %xmm14, %xmm6, %xmm6                    #3621.2679
        vsubps    %ymm12, %ymm11, %ymm14                        #3622.30
        vmovups   .L_2il0floatpacket.61(%rip), %ymm0            #3624.28
        vsubps    %ymm14, %ymm11, %ymm11                        #3625.28
        vsubps    %ymm12, %ymm11, %ymm11                        #3626.28
        vsubps    %ymm13, %ymm11, %ymm11                        #3627.28
        vinsertf128 $1, %xmm6, %ymm2, %ymm6                     #3621.2174
        vandps    %ymm0, %ymm14, %ymm2                          #3624.28
        vsubps    %ymm2, %ymm14, %ymm14                         #3628.30
        vmulps    %ymm6, %ymm12, %ymm6                          #3654.41
        vaddps    %ymm14, %ymm15, %ymm15                        #3629.30
        vmovups   1152+__svml_stan_ha_data_internal(%rip), %ymm14 #3633.60
        vaddps    %ymm15, %ymm11, %ymm15                        #3630.28
        vaddps    %ymm6, %ymm4, %ymm4                           #3654.26
        vrcpps    %ymm2, %ymm11                                 #3631.32
        vmulps    %ymm12, %ymm12, %ymm6                         #3655.26
        vandps    %ymm0, %ymm11, %ymm0                          #3632.32
        vmulps    %ymm0, %ymm2, %ymm2                           #3634.50
        vsubps    %ymm2, %ymm14, %ymm11                         #3634.26
        vmulps    %ymm11, %ymm0, %ymm14                         #3636.43
        vmulps    %ymm11, %ymm11, %ymm2                         #3635.26
        vaddps    %ymm14, %ymm0, %ymm14                         #3636.28
        vmulps    %ymm14, %ymm2, %ymm2                          #3637.47
        vaddps    %ymm2, %ymm14, %ymm2                          #3637.32
        vmulps    %ymm2, %ymm15, %ymm15                         #3638.28
        vsubps    %ymm11, %ymm15, %ymm14                        #3639.27
        vmulps    %ymm15, %ymm15, %ymm11                        #3640.47
        vsubps    %ymm14, %ymm11, %ymm15                        #3640.32
        vmulps    %ymm0, %ymm10, %ymm14                         #3642.36
        vmulps    %ymm5, %ymm12, %ymm0                          #3644.30
        vmulps    %ymm15, %ymm2, %ymm2                          #3641.32
        vaddps    %ymm0, %ymm3, %ymm11                          #3645.30
        vmulps    %ymm2, %ymm10, %ymm10                         #3643.36
        vsubps    %ymm11, %ymm3, %ymm3                          #3646.30
        vaddps    %ymm11, %ymm14, %ymm2                         #3647.30
        vaddps    %ymm3, %ymm0, %ymm3                           #3648.30
        vsubps    %ymm2, %ymm14, %ymm0                          #3649.30
        vaddps    %ymm3, %ymm10, %ymm10                         #3650.30
        vaddps    %ymm0, %ymm11, %ymm3                          #3651.30
        vaddps    %ymm8, %ymm7, %ymm0                           #3653.26
        vaddps    %ymm7, %ymm5, %ymm7                           #3656.26
        vaddps    %ymm3, %ymm10, %ymm3                          #3652.30
        vmulps    %ymm6, %ymm4, %ymm5                           #3657.41
        vmulps    %ymm7, %ymm13, %ymm13                         #3658.41
        vaddps    %ymm5, %ymm0, %ymm0                           #3657.26
        vaddps    %ymm13, %ymm3, %ymm3                          #3658.26
        vmulps    %ymm0, %ymm12, %ymm12                         #3660.42
        vaddps    %ymm3, %ymm9, %ymm4                           #3659.26
        vaddps    %ymm12, %ymm4, %ymm0                          #3660.27
        vmovups   96(%rsp), %ymm4                               #3663.22[spill]
        vaddps    %ymm0, %ymm2, %ymm2                           #3661.32
        vmovups   32(%rsp), %ymm0                               #3663.22[spill]
        vxorps    (%rsp), %ymm2, %ymm3                          #3662.28[spill]
        vblendvps %ymm1, %ymm3, %ymm4, %ymm4                    #3663.22
        jmp       ..B4.2        # Prob 100%                     #3663.22
        .align    16,0x90
                                # LOE rbx r13 r14 r15 eax ymm0 ymm4
	.cfi_endproc
# mark_end;
	.type	__svml_tanf8_ha_e9,@function
	.size	__svml_tanf8_ha_e9,.-__svml_tanf8_ha_e9
..LN__svml_tanf8_ha_e9.3:
	.data
# -- End  __svml_tanf8_ha_e9
	.text
.L_2__routine_start___svml_tanf4_ha_l9_4:
# -- Begin  __svml_tanf4_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf4_ha_l9
# --- __svml_tanf4_ha_l9(__m128)
__svml_tanf4_ha_l9:
# parameter 1: %xmm0
..B5.1:                         # Preds ..B5.0
                                # Execution count [1.00e+00]
        .byte     243                                           #3675.1
        .byte     15                                            #3990.18
        .byte     30                                            #3990.18
        .byte     250                                           #3990.18
	.cfi_startproc
..___tag_value___svml_tanf4_ha_l9.178:
..L179:
                                                        #3675.1
        pushq     %rbp                                          #3675.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #3675.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #3675.1
        subq      $192, %rsp                                    #3675.1
        vmovaps   %xmm0, %xmm14                                 #3675.1
        vmovups   1088+__svml_stan_ha_data_internal(%rip), %xmm3 #3743.54
        xorl      %eax, %eax                                    #3731.5
        vmovups   __svml_stan_ha_data_internal(%rip), %xmm1     #3744.17
        vfmadd213ps %xmm3, %xmm14, %xmm1                        #3744.17
        vmovups   64+__svml_stan_ha_data_internal(%rip), %xmm2  #3746.49
        vmovups   256+__svml_stan_ha_data_internal(%rip), %xmm5 #3747.53
        vmovups   320+__svml_stan_ha_data_internal(%rip), %xmm6 #3748.53
        vandps    960+__svml_stan_ha_data_internal(%rip), %xmm14, %xmm10 #3733.16
        vcmpnleps 768+__svml_stan_ha_data_internal(%rip), %xmm10, %xmm0 #3737.34
        vsubps    %xmm3, %xmm1, %xmm9                           #3745.17
        vmovmskps %xmm0, %edx                                   #3739.82
        vfnmadd213ps %xmm14, %xmm9, %xmm2                       #3749.18
        vmovaps   %xmm2, %xmm4                                  #3750.18
        vfnmadd231ps %xmm9, %xmm5, %xmm4                        #3750.18
        vmovaps   %xmm4, %xmm3                                  #3751.17
        vsubps    %xmm4, %xmm2, %xmm8                           #3752.23
        vfnmadd231ps %xmm9, %xmm6, %xmm3                        #3751.17
        vfnmadd231ps %xmm9, %xmm5, %xmm8                        #3754.22
        vsubps    %xmm4, %xmm3, %xmm7                           #3753.23
        vfmadd213ps %xmm7, %xmm6, %xmm9                         #3755.23
        vsubps    %xmm9, %xmm8, %xmm2                           #3756.22
        testl     %edx, %edx                                    #3758.66
        jne       ..B5.12       # Prob 5%                       #3758.66
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm1 xmm2 xmm3 xmm10 xmm14
..B5.2:                         # Preds ..B5.12 ..B5.1
                                # Execution count [1.00e+00]
        vpslld    $2, %xmm1, %xmm7                              #3992.216
        lea       512+__svml_stan_ha_data_internal(%rip), %r8   #3993.72
        vpand     .L_2il0floatpacket.77(%rip), %xmm7, %xmm8     #3992.265
        vmovd     %xmm8, %edx                                   #3993.179
        vpextrd   $1, %xmm8, %ecx                               #3993.322
        movslq    %edx, %rdx                                    #3993.179
        movslq    %ecx, %rcx                                    #3993.322
        vpextrd   $2, %xmm8, %esi                               #3993.489
        vpextrd   $3, %xmm8, %edi                               #3993.635
        vmovd     -128(%rdx,%r8), %xmm9                         #3993.72
        vmovd     -128(%rcx,%r8), %xmm10                        #3993.215
        vpunpcklqdq %xmm10, %xmm9, %xmm13                       #3993.52
        vmulps    %xmm3, %xmm3, %xmm10                          #3995.14
        vmovups   704+__svml_stan_ha_data_internal(%rip), %xmm9 #3996.51
        movslq    %esi, %rsi                                    #3993.489
        movslq    %edi, %rdi                                    #3993.635
        vfmadd213ps 640+__svml_stan_ha_data_internal(%rip), %xmm10, %xmm9 #3998.20
        vmovd     (%rdx,%r8), %xmm5                             #3994.76
        vmovd     -128(%rsi,%r8), %xmm11                        #3993.382
        vmovd     -128(%rdi,%r8), %xmm12                        #3993.528
        vpunpcklqdq %xmm12, %xmm11, %xmm15                      #3993.362
        vmulps    %xmm9, %xmm3, %xmm11                          #3999.20
        vfnmsub213ps %xmm2, %xmm10, %xmm11                      #4000.20
        vmovd     (%rsi,%r8), %xmm1                             #3994.386
        vmovd     (%rdi,%r8), %xmm4                             #3994.532
        vpunpcklqdq %xmm4, %xmm1, %xmm8                         #3994.366
        vshufps   $136, %xmm15, %xmm13, %xmm6                   #3993.32
        vmovd     (%rcx,%r8), %xmm0                             #3994.219
        vmovaps   %xmm6, %xmm10                                 #4012.22
        vpunpcklqdq %xmm0, %xmm5, %xmm7                         #3994.56
        vsubps    %xmm11, %xmm3, %xmm4                          #4001.17
        vshufps   $136, %xmm8, %xmm7, %xmm5                     #3994.36
        vsubps    %xmm4, %xmm3, %xmm3                           #4002.20
        vmulps    %xmm4, %xmm6, %xmm7                           #4010.15
        vsubps    %xmm11, %xmm3, %xmm2                          #4003.20
        vfnmadd213ps %xmm7, %xmm4, %xmm10                       #4012.22
        vaddps    %xmm4, %xmm6, %xmm3                           #4004.21
        vsubps    %xmm6, %xmm3, %xmm12                          #4005.20
        vsubps    %xmm12, %xmm4, %xmm13                         #4006.20
        vmovups   1152+__svml_stan_ha_data_internal(%rip), %xmm12 #4009.45
        vsubps    %xmm7, %xmm12, %xmm1                          #4011.23
        vaddps    %xmm13, %xmm5, %xmm15                         #4007.20
        vsubps    %xmm12, %xmm1, %xmm8                          #4013.19
        vaddps    %xmm15, %xmm2, %xmm0                          #4008.20
        vaddps    %xmm8, %xmm7, %xmm9                           #4014.20
        vsubps    %xmm10, %xmm9, %xmm11                         #4015.20
        vfmadd213ps %xmm11, %xmm2, %xmm6                        #4016.20
        vrcpps    %xmm1, %xmm2                                  #4018.15
        vfmadd213ps %xmm6, %xmm4, %xmm5                         #4017.20
        vfnmadd231ps %xmm2, %xmm1, %xmm12                       #4019.17
        vfmadd231ps %xmm2, %xmm5, %xmm12                        #4020.21
        vfmadd213ps %xmm2, %xmm12, %xmm2                        #4021.19
        vmulps    %xmm2, %xmm3, %xmm6                           #4022.24
        vfmsub213ps %xmm3, %xmm6, %xmm1                         #4023.18
        vfnmadd213ps %xmm1, %xmm6, %xmm5                        #4024.22
        vsubps    %xmm0, %xmm5, %xmm0                           #4025.22
        vfnmadd213ps %xmm6, %xmm2, %xmm0                        #4026.18
        testl     %eax, %eax                                    #4028.52
        jne       ..B5.4        # Prob 5%                       #4028.52
                                # LOE rbx r12 r13 r14 r15 eax xmm0 xmm14
..B5.3:                         # Preds ..B5.4 ..B5.10 ..B5.2
                                # Execution count [1.00e+00]
        movq      %rbp, %rsp                                    #4031.12
        popq      %rbp                                          #4031.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4031.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
                                # LOE
..B5.4:                         # Preds ..B5.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %xmm14, 64(%rsp)                              #4028.193
        vmovups   %xmm0, 128(%rsp)                              #4028.264
        je        ..B5.3        # Prob 95%                      #4028.368
                                # LOE rbx r12 r13 r14 r15 eax xmm0
..B5.7:                         # Preds ..B5.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %edx, %edx                                    #4028.448
        movq      %r12, 8(%rsp)                                 #4028.448[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
        movl      %edx, %r12d                                   #4028.448
        movq      %r13, (%rsp)                                  #4028.448[spill]
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r13d                                   #4028.448
                                # LOE rbx r12 r14 r15 r13d
..B5.8:                         # Preds ..B5.9 ..B5.7
                                # Execution count [1.25e-02]: Infreq
        btl       %r12d, %r13d                                  #4028.511
        jc        ..B5.11       # Prob 5%                       #4028.511
                                # LOE rbx r12 r14 r15 r13d
..B5.9:                         # Preds ..B5.11 ..B5.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r12d                                         #4028.464
        cmpl      $4, %r12d                                     #4028.459
        jl        ..B5.8        # Prob 82%                      #4028.459
                                # LOE rbx r12 r14 r15 r13d
..B5.10:                        # Preds ..B5.9
                                # Execution count [2.25e-03]: Infreq
        movq      8(%rsp), %r12                                 #[spill]
	.cfi_restore 12
        movq      (%rsp), %r13                                  #[spill]
	.cfi_restore 13
        movups    128(%rsp), %xmm0                              #4028.665
        jmp       ..B5.3        # Prob 100%                     #4028.665
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x48, 0xff, 0xff, 0xff, 0x22
	.cfi_escape 0x10, 0x0d, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 r15 xmm0
..B5.11:                        # Preds ..B5.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r12,4), %rdi                         #4028.540
        lea       128(%rsp,%r12,4), %rsi                        #4028.540
..___tag_value___svml_tanf4_ha_l9.196:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #4028.540
..___tag_value___svml_tanf4_ha_l9.197:
        jmp       ..B5.9        # Prob 100%                     #4028.540
	.cfi_restore 12
	.cfi_restore 13
                                # LOE rbx r14 r15 r12d r13d
..B5.12:                        # Preds ..B5.1
                                # Execution count [5.00e-02]: Infreq
        vpand     .L_2il0floatpacket.27(%rip), %xmm14, %xmm11   #3852.18
        lea       __svml_stan_ha_reduction_data_internal(%rip), %r8 #3854.708
        vmovups   %xmm3, 16(%rsp)                               #[spill]
        vpsrld    $23, %xmm11, %xmm3                            #3853.18
        vmovups   1024+__svml_stan_ha_data_internal(%rip), %xmm7 #3760.54
        vmovups   %xmm1, (%rsp)                                 #[spill]
        vpslld    $1, %xmm3, %xmm1                              #3854.234
        vandps    %xmm10, %xmm7, %xmm8                          #3761.17
        vmovups   %xmm2, 32(%rsp)                               #[spill]
        vpaddd    %xmm3, %xmm1, %xmm2                           #3854.367
        vcmpeqps  %xmm7, %xmm8, %xmm10                          #3764.29
        vpslld    $2, %xmm2, %xmm15                             #3854.367
        vmovd     %xmm15, %edx                                  #3854.437
        vmovmskps %xmm10, %eax                                  #3766.52
        vmovd     (%rdx,%r8), %xmm13                            #3854.1236
        vpextrd   $1, %xmm15, %ecx                              #3854.501
        vpextrd   $2, %xmm15, %esi                              #3854.568
        vpextrd   $3, %xmm15, %edi                              #3854.635
        vmovd     (%rcx,%r8), %xmm12                            #3854.1319
        vmovd     (%rsi,%r8), %xmm9                             #3854.1423
        vmovd     (%rdi,%r8), %xmm6                             #3854.1506
        vmovd     4(%rdi,%r8), %xmm1                            #3855.1507
        vpunpcklqdq %xmm12, %xmm13, %xmm5                       #3854.1216
        vpunpcklqdq %xmm6, %xmm9, %xmm4                         #3854.1403
        vmovd     8(%rdi,%r8), %xmm9                            #3856.1506
        vmovd     4(%rdx,%r8), %xmm8                            #3855.1237
        vmovd     8(%rdx,%r8), %xmm15                           #3856.1236
        vmovd     4(%rcx,%r8), %xmm10                           #3855.1320
        vmovd     8(%rcx,%r8), %xmm13                           #3856.1319
        vmovd     4(%rsi,%r8), %xmm7                            #3855.1424
        vmovd     8(%rsi,%r8), %xmm12                           #3856.1423
        vshufps   $136, %xmm4, %xmm5, %xmm11                    #3854.1196
        vpunpcklqdq %xmm10, %xmm8, %xmm3                        #3855.1217
        vpunpcklqdq %xmm1, %xmm7, %xmm2                         #3855.1404
        vpunpcklqdq %xmm13, %xmm15, %xmm6                       #3856.1216
        vpunpcklqdq %xmm9, %xmm12, %xmm5                        #3856.1403
        vpand     .L_2il0floatpacket.64(%rip), %xmm14, %xmm4    #3862.21
        vshufps   $136, %xmm2, %xmm3, %xmm10                    #3855.1197
        vshufps   $136, %xmm5, %xmm6, %xmm12                    #3856.1196
        vpsrld    $16, %xmm10, %xmm5                            #3867.12
        vpaddd    .L_2il0floatpacket.65(%rip), %xmm4, %xmm7     #3863.21
        vpsrld    $16, %xmm12, %xmm1                            #3869.12
        vmovdqu   .L_2il0floatpacket.66(%rip), %xmm13           #3864.17
        vpand     %xmm13, %xmm10, %xmm3                         #3868.12
        vpsrld    $16, %xmm7, %xmm10                            #3871.12
        vpand     %xmm13, %xmm12, %xmm15                        #3870.12
        vpand     %xmm13, %xmm7, %xmm7                          #3872.12
        vpmulld   %xmm15, %xmm10, %xmm12                        #3878.13
        vpand     %xmm13, %xmm11, %xmm8                         #3866.12
        vpsrld    $16, %xmm12, %xmm15                           #3903.13
        vpsrld    $16, %xmm11, %xmm11                           #3865.12
        vpmulld   %xmm1, %xmm10, %xmm12                         #3877.13
        vpmulld   %xmm1, %xmm7, %xmm1                           #3883.13
        vpmulld   %xmm3, %xmm7, %xmm2                           #3882.13
        vpsrld    $16, %xmm1, %xmm1                             #3885.13
        vpand     %xmm13, %xmm2, %xmm6                          #3893.13
        vpaddd    %xmm1, %xmm12, %xmm12                         #3889.13
        vpmulld   %xmm5, %xmm7, %xmm4                           #3881.13
        vpaddd    %xmm12, %xmm6, %xmm6                          #3898.13
        vpmulld   %xmm3, %xmm10, %xmm3                          #3876.13
        vpsrld    $16, %xmm2, %xmm2                             #3886.13
        vpaddd    %xmm6, %xmm15, %xmm12                         #3904.13
        vpand     %xmm13, %xmm4, %xmm1                          #3894.13
        vpaddd    %xmm2, %xmm3, %xmm3                           #3890.13
        vpsrld    $16, %xmm12, %xmm15                           #3905.13
        vpmulld   %xmm8, %xmm7, %xmm9                           #3880.13
        vpaddd    %xmm3, %xmm1, %xmm1                           #3899.13
        vpmulld   %xmm5, %xmm10, %xmm5                          #3875.13
        vpsrld    $16, %xmm4, %xmm4                             #3887.13
        vpaddd    %xmm1, %xmm15, %xmm6                          #3906.13
        vpand     %xmm13, %xmm9, %xmm2                          #3895.13
        vpaddd    %xmm4, %xmm5, %xmm1                           #3891.13
        vpsrld    $16, %xmm6, %xmm3                             #3907.13
        vpaddd    %xmm1, %xmm2, %xmm2                           #3900.13
        vpslld    $16, %xmm6, %xmm6                             #3914.19
        vpmulld   %xmm11, %xmm7, %xmm1                          #3879.13
        vpaddd    %xmm2, %xmm3, %xmm5                           #3908.13
        vpmulld   %xmm8, %xmm10, %xmm3                          #3874.13
        vpsrld    $16, %xmm9, %xmm2                             #3888.13
        vpand     %xmm13, %xmm1, %xmm15                         #3896.13
        vpaddd    %xmm2, %xmm3, %xmm9                           #3913.18
        vpsrld    $16, %xmm5, %xmm4                             #3909.13
        vpaddd    %xmm9, %xmm15, %xmm7                          #3913.18
        vpaddd    %xmm7, %xmm4, %xmm4                           #3913.18
        vpand     %xmm13, %xmm5, %xmm5                          #3912.13
        vpslld    $16, %xmm4, %xmm8                             #3913.18
        vpand     %xmm13, %xmm12, %xmm13                        #3911.13
        vpaddd    %xmm5, %xmm8, %xmm9                           #3915.18
        vpaddd    %xmm13, %xmm6, %xmm12                         #3916.19
        vpand     .L_2il0floatpacket.28(%rip), %xmm14, %xmm6    #3918.17
        vpsrld    $9, %xmm9, %xmm10                             #3921.20
        vpxor     .L_2il0floatpacket.67(%rip), %xmm6, %xmm11    #3920.22
        vmovups   .L_2il0floatpacket.29(%rip), %xmm1            #3924.15
        vpor      %xmm11, %xmm10, %xmm3                         #3922.20
        vpand     .L_2il0floatpacket.72(%rip), %xmm9, %xmm9     #3943.20
        vaddps    %xmm1, %xmm3, %xmm13                          #3925.19
        vpxor     .L_2il0floatpacket.69(%rip), %xmm6, %xmm8     #3932.22
        vsubps    %xmm1, %xmm13, %xmm2                          #3926.9
        vpand     .L_2il0floatpacket.70(%rip), %xmm12, %xmm4    #3934.20
        vpsrld    $18, %xmm12, %xmm12                           #3945.19
        vpxor     .L_2il0floatpacket.71(%rip), %xmm6, %xmm10    #3941.22
        vpslld    $14, %xmm9, %xmm6                             #3944.20
        vpslld    $5, %xmm4, %xmm5                              #3935.20
        vsubps    %xmm2, %xmm3, %xmm15                          #3927.17
        vpor      %xmm12, %xmm6, %xmm3                          #3946.20
        vpor      %xmm8, %xmm5, %xmm7                           #3936.20
        vpor      %xmm10, %xmm3, %xmm2                          #3947.20
        vsubps    %xmm8, %xmm7, %xmm1                           #3939.17
        vsubps    %xmm10, %xmm2, %xmm4                          #3950.18
        vmovups   .L_2il0floatpacket.30(%rip), %xmm6            #3955.20
        vaddps    %xmm4, %xmm15, %xmm7                          #3951.11
        vmulps    %xmm7, %xmm6, %xmm12                          #3957.17
        vsubps    %xmm7, %xmm15, %xmm15                         #3952.17
        vmovups   .L_2il0floatpacket.35(%rip), %xmm9            #3962.16
        vaddps    %xmm15, %xmm4, %xmm5                          #3953.18
        vandps    .L_2il0floatpacket.34(%rip), %xmm14, %xmm8    #3963.11
        vaddps    %xmm5, %xmm1, %xmm4                           #3954.11
        vcmpltps  %xmm8, %xmm9, %xmm2                           #3964.17
        vcmpleps  %xmm9, %xmm8, %xmm11                          #3965.20
        vmovaps   %xmm6, %xmm1                                  #3958.17
        vandps    %xmm12, %xmm2, %xmm3                          #3967.25
        vfmsub213ps %xmm12, %xmm7, %xmm1                        #3958.17
        vmovdqu   .L_2il0floatpacket.74(%rip), %xmm5            #3974.23
        vpand     .L_2il0floatpacket.68(%rip), %xmm13, %xmm13   #3930.15
        vfmadd132ps .L_2il0floatpacket.73(%rip), %xmm1, %xmm7   #3959.17
        vandps    %xmm14, %xmm11, %xmm1                         #3966.24
        vorps     %xmm3, %xmm1, %xmm15                          #3968.9
        vmovups   .L_2il0floatpacket.75(%rip), %xmm1            #3981.19
        vfmadd213ps %xmm7, %xmm6, %xmm4                         #3960.17
        vpsrld    $31, %xmm15, %xmm6                            #3973.22
        vpsubd    %xmm6, %xmm5, %xmm7                           #3974.23
        vpaddd    %xmm7, %xmm13, %xmm8                          #3976.23
        vpsrld    $2, %xmm8, %xmm11                             #3976.23
        vpslld    $2, %xmm11, %xmm9                             #3978.23
        vpsubd    %xmm9, %xmm13, %xmm13                         #3979.23
        vandps    %xmm4, %xmm2, %xmm10                          #3969.9
        vcvtdq2ps %xmm13, %xmm2                                 #3980.17
        vmovaps   %xmm15, %xmm4                                 #3983.26
        vfmadd231ps %xmm2, %xmm1, %xmm4                         #3983.26
        vfnmadd213ps %xmm4, %xmm2, %xmm1                        #3984.27
        vsubps    %xmm1, %xmm15, %xmm3                          #3985.27
        vmovups   (%rsp), %xmm1                                 #3988.17[spill]
        vfmadd132ps .L_2il0floatpacket.76(%rip), %xmm3, %xmm2   #3986.27
        vmovups   16(%rsp), %xmm3                               #3989.17[spill]
        blendvps  %xmm0, %xmm11, %xmm1                          #3988.17
        vaddps    %xmm2, %xmm10, %xmm5                          #3987.27
        vmovups   32(%rsp), %xmm2                               #3990.18[spill]
        blendvps  %xmm0, %xmm4, %xmm3                           #3989.17
        blendvps  %xmm0, %xmm5, %xmm2                           #3990.18
        jmp       ..B5.2        # Prob 100%                     #3990.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 r15 eax xmm1 xmm2 xmm3 xmm14
	.cfi_endproc
# mark_end;
	.type	__svml_tanf4_ha_l9,@function
	.size	__svml_tanf4_ha_l9,.-__svml_tanf4_ha_l9
..LN__svml_tanf4_ha_l9.4:
	.data
# -- End  __svml_tanf4_ha_l9
	.text
.L_2__routine_start___svml_tanf8_ha_l9_5:
# -- Begin  __svml_tanf8_ha_l9
	.text
# mark_begin;
       .align    16,0x90
	.globl __svml_tanf8_ha_l9
# --- __svml_tanf8_ha_l9(__m256)
__svml_tanf8_ha_l9:
# parameter 1: %ymm0
..B6.1:                         # Preds ..B6.0
                                # Execution count [1.00e+00]
        .byte     243                                           #4037.1
        .byte     15                                            #4352.18
        .byte     30                                            #4352.18
        .byte     250                                           #4352.18
	.cfi_startproc
..___tag_value___svml_tanf8_ha_l9.207:
..L208:
                                                        #4037.1
        pushq     %rbp                                          #4037.1
	.cfi_def_cfa_offset 16
        movq      %rsp, %rbp                                    #4037.1
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
        andq      $-64, %rsp                                    #4037.1
        pushq     %r15                                          #4037.1
        subq      $184, %rsp                                    #4037.1
        xorl      %eax, %eax                                    #4093.5
        vmovaps   %ymm0, %ymm14                                 #4037.1
        vmovups   1088+__svml_stan_ha_data_internal(%rip), %ymm2 #4105.57
        vmovups   __svml_stan_ha_data_internal(%rip), %ymm0     #4106.17
        vmovups   64+__svml_stan_ha_data_internal(%rip), %ymm1  #4108.52
        vmovups   256+__svml_stan_ha_data_internal(%rip), %ymm4 #4109.56
        vmovups   320+__svml_stan_ha_data_internal(%rip), %ymm5 #4110.56
        vfmadd213ps %ymm2, %ymm14, %ymm0                        #4106.17
        vsubps    %ymm2, %ymm0, %ymm8                           #4107.17
        vfnmadd213ps %ymm14, %ymm8, %ymm1                       #4111.18
        vmovaps   %ymm4, %ymm3                                  #4112.18
        vfnmadd213ps %ymm1, %ymm8, %ymm3                        #4112.18
        vmovaps   %ymm5, %ymm2                                  #4113.17
        vfnmadd213ps %ymm3, %ymm8, %ymm2                        #4113.17
        vsubps    %ymm3, %ymm1, %ymm7                           #4114.23
        vsubps    %ymm3, %ymm2, %ymm6                           #4115.23
        vfnmadd231ps %ymm8, %ymm4, %ymm7                        #4116.22
        vfmadd213ps %ymm6, %ymm5, %ymm8                         #4117.23
        vandps    960+__svml_stan_ha_data_internal(%rip), %ymm14, %ymm9 #4095.16
        vcmpnle_uqps 768+__svml_stan_ha_data_internal(%rip), %ymm9, %ymm13 #4099.34
        vsubps    %ymm8, %ymm7, %ymm1                           #4118.22
        vmovmskps %ymm13, %edx                                  #4101.83
        testl     %edx, %edx                                    #4120.66
        jne       ..B6.12       # Prob 5%                       #4120.66
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 eax ymm0 ymm1 ymm2 ymm9 ymm13 ymm14
..B6.2:                         # Preds ..B6.12 ..B6.1
                                # Execution count [1.00e+00]
        vpslld    $2, %ymm0, %ymm7                              #4354.222
        lea       512+__svml_stan_ha_data_internal(%rip), %r11  #4355.118
        vpand     .L_2il0floatpacket.93(%rip), %ymm7, %ymm8     #4354.274
        vextractf128 $1, %ymm8, %xmm3                           #4355.1022
        vmovd     %xmm8, %r10d                                  #4355.225
        vmovd     %xmm3, %esi                                   #4355.1003
        vpextrd   $1, %xmm8, %r9d                               #4355.396
        vpextrd   $2, %xmm8, %r8d                               #4355.591
        vpextrd   $3, %xmm8, %edi                               #4355.765
        vpextrd   $1, %xmm3, %ecx                               #4355.1179
        vpextrd   $2, %xmm3, %edx                               #4355.1379
        vpextrd   $3, %xmm3, %r15d                              #4355.1558
        movslq    %r10d, %r10                                   #4355.225
        movslq    %r9d, %r9                                     #4355.396
        movslq    %r8d, %r8                                     #4355.591
        movslq    %edi, %rdi                                    #4355.765
        movslq    %esi, %rsi                                    #4355.1003
        movslq    %ecx, %rcx                                    #4355.1179
        movslq    %edx, %rdx                                    #4355.1379
        movslq    %r15d, %r15                                   #4355.1558
        vmovd     -128(%r10,%r11), %xmm9                        #4355.118
        vmovd     -128(%r9,%r11), %xmm10                        #4355.289
        vmovd     -128(%r8,%r11), %xmm11                        #4355.484
        vmovd     -128(%rdi,%r11), %xmm12                       #4355.658
        vmovd     -128(%rsi,%r11), %xmm0                        #4355.896
        vmovd     -128(%rcx,%r11), %xmm6                        #4355.1072
        vmovd     -128(%rdx,%r11), %xmm4                        #4355.1272
        vmovd     -128(%r15,%r11), %xmm7                        #4355.1451
        vpunpcklqdq %xmm10, %xmm9, %xmm13                       #4355.98
        vpunpcklqdq %xmm12, %xmm11, %xmm15                      #4355.464
        vpunpcklqdq %xmm6, %xmm0, %xmm9                         #4355.876
        vpunpcklqdq %xmm7, %xmm4, %xmm10                        #4355.1252
        vshufps   $136, %xmm15, %xmm13, %xmm5                   #4355.78
        vshufps   $136, %xmm10, %xmm9, %xmm11                   #4355.856
        vmovd     (%r10,%r11), %xmm12                           #4356.122
        vmovd     (%r9,%r11), %xmm13                            #4356.293
        vmovd     (%r8,%r11), %xmm15                            #4356.488
        vpunpcklqdq %xmm13, %xmm12, %xmm3                       #4356.102
        vmovd     (%rsi,%r11), %xmm7                            #4356.900
        vmovd     (%rcx,%r11), %xmm8                            #4356.1076
        vmovd     (%rdx,%r11), %xmm9                            #4356.1276
        vmovd     (%r15,%r11), %xmm10                           #4356.1455
        vpunpcklqdq %xmm10, %xmm9, %xmm12                       #4356.1256
        vinsertf128 $1, %xmm11, %ymm5, %ymm6                    #4355.14
        vmovd     (%rdi,%r11), %xmm5                            #4356.662
        vpunpcklqdq %xmm5, %xmm15, %xmm0                        #4356.468
        vshufps   $136, %xmm0, %xmm3, %xmm4                     #4356.82
        vmulps    %ymm2, %ymm2, %ymm15                          #4357.14
        vmovups   704+__svml_stan_ha_data_internal(%rip), %ymm3 #4358.54
        vpunpcklqdq %xmm8, %xmm7, %xmm11                        #4356.880
        vshufps   $136, %xmm12, %xmm11, %xmm13                  #4356.860
        vfmadd213ps 640+__svml_stan_ha_data_internal(%rip), %ymm15, %ymm3 #4360.20
        vmulps    %ymm3, %ymm2, %ymm0                           #4361.20
        vfnmsub213ps %ymm1, %ymm15, %ymm0                       #4362.20
        vinsertf128 $1, %xmm13, %ymm4, %ymm5                    #4356.18
        vsubps    %ymm0, %ymm2, %ymm4                           #4363.17
        vmovups   1152+__svml_stan_ha_data_internal(%rip), %ymm13 #4371.48
        vmulps    %ymm4, %ymm6, %ymm9                           #4372.15
        vsubps    %ymm4, %ymm2, %ymm1                           #4364.20
        vaddps    %ymm4, %ymm6, %ymm3                           #4366.21
        vsubps    %ymm0, %ymm1, %ymm2                           #4365.20
        vsubps    %ymm6, %ymm3, %ymm1                           #4367.20
        vsubps    %ymm1, %ymm4, %ymm7                           #4368.20
        vsubps    %ymm9, %ymm13, %ymm1                          #4373.23
        vaddps    %ymm7, %ymm5, %ymm8                           #4369.20
        vsubps    %ymm13, %ymm1, %ymm10                         #4375.19
        vaddps    %ymm8, %ymm2, %ymm0                           #4370.20
        vaddps    %ymm10, %ymm9, %ymm11                         #4376.20
        vmovaps   %ymm6, %ymm7                                  #4374.22
        vfnmadd213ps %ymm9, %ymm4, %ymm7                        #4374.22
        vsubps    %ymm7, %ymm11, %ymm12                         #4377.20
        vfmadd213ps %ymm12, %ymm2, %ymm6                        #4378.20
        vrcpps    %ymm1, %ymm2                                  #4380.15
        vfmadd213ps %ymm6, %ymm4, %ymm5                         #4379.20
        vfnmadd231ps %ymm2, %ymm1, %ymm13                       #4381.17
        vfmadd231ps %ymm2, %ymm5, %ymm13                        #4382.21
        vfmadd213ps %ymm2, %ymm13, %ymm2                        #4383.19
        vmulps    %ymm2, %ymm3, %ymm4                           #4384.24
        vfmsub213ps %ymm3, %ymm4, %ymm1                         #4385.18
        vfnmadd213ps %ymm1, %ymm4, %ymm5                        #4386.22
        vsubps    %ymm0, %ymm5, %ymm0                           #4387.22
        vfnmadd213ps %ymm4, %ymm2, %ymm0                        #4388.18
        testl     %eax, %eax                                    #4390.52
        jne       ..B6.4        # Prob 5%                       #4390.52
                                # LOE rbx r12 r13 r14 eax ymm0 ymm14
..B6.3:                         # Preds ..B6.4 ..B6.10 ..B6.2
                                # Execution count [1.00e+00]
        addq      $184, %rsp                                    #4393.12
	.cfi_restore 15
        popq      %r15                                          #4393.12
        movq      %rbp, %rsp                                    #4393.12
        popq      %rbp                                          #4393.12
	.cfi_def_cfa 7, 8
	.cfi_restore 6
        ret                                                     #4393.12
	.cfi_def_cfa 6, 16
	.cfi_offset 6, -16
	.cfi_escape 0x10, 0x0f, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0xf8, 0xff, 0xff, 0xff, 0x22
                                # LOE
..B6.4:                         # Preds ..B6.2
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm14, 64(%rsp)                              #4390.196
        vmovups   %ymm0, 128(%rsp)                              #4390.270
        je        ..B6.3        # Prob 95%                      #4390.374
                                # LOE rbx r12 r13 r14 eax ymm0
..B6.7:                         # Preds ..B6.4
                                # Execution count [2.25e-03]: Infreq
        xorl      %r15d, %r15d                                  #4390.454
                                # LOE rbx r12 r13 r14 r15 eax
..B6.15:                        # Preds ..B6.7
                                # Execution count [2.25e-03]: Infreq
        vzeroupper                                              #
        movq      %r12, (%rsp)                                  #[spill]
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
        movl      %eax, %r12d                                   #
                                # LOE rbx r13 r14 r15 r12d
..B6.8:                         # Preds ..B6.9 ..B6.15
                                # Execution count [1.25e-02]: Infreq
        btl       %r15d, %r12d                                  #4390.517
        jc        ..B6.11       # Prob 5%                       #4390.517
                                # LOE rbx r13 r14 r15 r12d
..B6.9:                         # Preds ..B6.11 ..B6.8
                                # Execution count [1.25e-02]: Infreq
        incl      %r15d                                         #4390.470
        cmpl      $8, %r15d                                     #4390.465
        jl        ..B6.8        # Prob 82%                      #4390.465
                                # LOE rbx r13 r14 r15 r12d
..B6.10:                        # Preds ..B6.9
                                # Execution count [2.25e-03]: Infreq
        movq      (%rsp), %r12                                  #[spill]
	.cfi_restore 12
        vmovups   128(%rsp), %ymm0                              #4390.674
        jmp       ..B6.3        # Prob 100%                     #4390.674
	.cfi_escape 0x10, 0x0c, 0x0e, 0x38, 0x1c, 0x0d, 0xc0, 0xff, 0xff, 0xff, 0x1a, 0x0d, 0x40, 0xff, 0xff, 0xff, 0x22
                                # LOE rbx r12 r13 r14 ymm0
..B6.11:                        # Preds ..B6.8
                                # Execution count [6.25e-04]: Infreq
        lea       64(%rsp,%r15,4), %rdi                         #4390.546
        lea       128(%rsp,%r15,4), %rsi                        #4390.546
..___tag_value___svml_tanf8_ha_l9.223:
#       __svml_stan_ha_cout_rare_internal(const float *, float *)
        call      __svml_stan_ha_cout_rare_internal             #4390.546
..___tag_value___svml_tanf8_ha_l9.224:
        jmp       ..B6.9        # Prob 100%                     #4390.546
	.cfi_restore 12
                                # LOE rbx r13 r14 r12d r15d
..B6.12:                        # Preds ..B6.1
                                # Execution count [5.00e-02]: Infreq
        vmovups   %ymm0, (%rsp)                                 #[spill]
        lea       __svml_stan_ha_reduction_data_internal(%rip), %r11 #4216.1212
        vmovups   %ymm1, 64(%rsp)                               #[spill]
        vmovups   %ymm2, 32(%rsp)                               #[spill]
        vmovups   1024+__svml_stan_ha_data_internal(%rip), %ymm6 #4122.57
        vpand     .L_2il0floatpacket.78(%rip), %ymm14, %ymm0    #4214.18
        vpsrld    $23, %ymm0, %ymm1                             #4215.18
        vpslld    $1, %ymm1, %ymm2                              #4216.234
        vpaddd    %ymm1, %ymm2, %ymm15                          #4216.296
        vpslld    $2, %ymm15, %ymm12                            #4216.373
        vandps    %ymm9, %ymm6, %ymm7                           #4123.17
        vcmpeqps  %ymm6, %ymm7, %ymm10                          #4126.29
        vmovmskps %ymm10, %eax                                  #4128.53
        vextractf128 $1, %ymm12, %xmm6                          #4216.834
        vmovd     %xmm12, %r10d                                 #4216.446
        vmovd     %xmm6, %esi                                   #4216.815
        vmovd     (%r10,%r11), %xmm11                           #4216.2300
        vpextrd   $1, %xmm12, %r9d                              #4216.536
        vpextrd   $2, %xmm12, %r8d                              #4216.629
        vpextrd   $3, %xmm12, %edi                              #4216.722
        vpextrd   $1, %xmm6, %ecx                               #4216.910
        vpextrd   $2, %xmm6, %edx                               #4216.1008
        vpextrd   $3, %xmm6, %r15d                              #4216.1106
        vmovd     (%r9,%r11), %xmm8                             #4216.2389
        vmovd     (%r8,%r11), %xmm5                             #4216.2499
        vmovd     (%rdi,%r11), %xmm4                            #4216.2588
        vpunpcklqdq %xmm8, %xmm11, %xmm3                        #4216.2280
        vpunpcklqdq %xmm4, %xmm5, %xmm9                         #4216.2479
        vmovd     (%rsi,%r11), %xmm10                           #4216.2741
        vmovd     (%rcx,%r11), %xmm0                            #4216.2830
        vmovd     (%rdx,%r11), %xmm2                            #4216.2940
        vmovd     (%r15,%r11), %xmm1                            #4216.3029
        vshufps   $136, %xmm9, %xmm3, %xmm7                     #4216.2260
        vpunpcklqdq %xmm0, %xmm10, %xmm15                       #4216.2721
        vpunpcklqdq %xmm1, %xmm2, %xmm12                        #4216.2920
        vmovd     4(%r10,%r11), %xmm8                           #4217.2301
        vmovd     4(%r9,%r11), %xmm5                            #4217.2390
        vmovd     4(%r8,%r11), %xmm4                            #4217.2500
        vmovd     4(%rdi,%r11), %xmm3                           #4217.2589
        vshufps   $136, %xmm12, %xmm15, %xmm11                  #4216.2701
        vpunpcklqdq %xmm5, %xmm8, %xmm9                         #4217.2281
        vpunpcklqdq %xmm3, %xmm4, %xmm6                         #4217.2480
        vshufps   $136, %xmm6, %xmm9, %xmm0                     #4217.2261
        vmovd     8(%rdi,%r11), %xmm6                           #4218.2588
        vmovd     4(%rsi,%r11), %xmm2                           #4217.2742
        vmovd     4(%rcx,%r11), %xmm1                           #4217.2831
        vmovd     4(%rdx,%r11), %xmm15                          #4217.2941
        vmovd     4(%r15,%r11), %xmm12                          #4217.3030
        vpunpcklqdq %xmm12, %xmm15, %xmm8                       #4217.2921
        vmovd     8(%rsi,%r11), %xmm15                          #4218.2741
        vmovd     8(%rcx,%r11), %xmm12                          #4218.2830
        vmovd     8(%r10,%r11), %xmm4                           #4218.2300
        vmovd     8(%r9,%r11), %xmm3                            #4218.2389
        vinsertf128 $1, %xmm11, %ymm7, %ymm10                   #4216.2196
        vmovd     8(%r8,%r11), %xmm7                            #4218.2499
        vpunpcklqdq %xmm1, %xmm2, %xmm11                        #4217.2722
        vpunpcklqdq %xmm6, %xmm7, %xmm2                         #4218.2479
        vpand     .L_2il0floatpacket.79(%rip), %ymm14, %ymm7    #4224.21
        vshufps   $136, %xmm8, %xmm11, %xmm5                    #4217.2702
        vmovd     8(%rdx,%r11), %xmm11                          #4218.2940
        vmovd     8(%r15,%r11), %xmm8                           #4218.3029
        vpaddd    .L_2il0floatpacket.80(%rip), %ymm7, %ymm6     #4225.21
        vinsertf128 $1, %xmm5, %ymm0, %ymm9                     #4217.2197
        vpunpcklqdq %xmm12, %xmm15, %xmm5                       #4218.2721
        vmovdqu   .L_2il0floatpacket.81(%rip), %ymm12           #4226.17
        vpunpcklqdq %xmm3, %xmm4, %xmm0                         #4218.2280
        vpunpcklqdq %xmm8, %xmm11, %xmm4                        #4218.2920
        vshufps   $136, %xmm2, %xmm0, %xmm1                     #4218.2260
        vshufps   $136, %xmm4, %xmm5, %xmm3                     #4218.2701
        vpsrld    $16, %ymm9, %ymm4                             #4229.12
        vpand     %ymm12, %ymm9, %ymm2                          #4230.12
        vpsrld    $16, %ymm6, %ymm9                             #4233.12
        vpand     %ymm12, %ymm6, %ymm6                          #4234.12
        vpand     %ymm12, %ymm10, %ymm7                         #4228.12
        vpsrld    $16, %ymm10, %ymm10                           #4227.12
        vpmulld   %ymm7, %ymm6, %ymm8                           #4242.13
        vinsertf128 $1, %xmm3, %ymm1, %ymm11                    #4218.2196
        vpand     %ymm12, %ymm11, %ymm15                        #4232.12
        vpsrld    $16, %ymm11, %ymm0                            #4231.12
        vpmulld   %ymm15, %ymm9, %ymm11                         #4240.13
        vpsrld    $16, %ymm11, %ymm15                           #4265.13
        vpmulld   %ymm0, %ymm9, %ymm11                          #4239.13
        vpmulld   %ymm0, %ymm6, %ymm0                           #4245.13
        vpsrld    $16, %ymm0, %ymm0                             #4247.13
        vpmulld   %ymm2, %ymm6, %ymm1                           #4244.13
        vpand     %ymm12, %ymm1, %ymm5                          #4255.13
        vpaddd    %ymm0, %ymm11, %ymm11                         #4251.13
        vpsrld    $16, %ymm1, %ymm1                             #4248.13
        vpaddd    %ymm11, %ymm5, %ymm5                          #4260.13
        vpmulld   %ymm4, %ymm6, %ymm3                           #4243.13
        vpmulld   %ymm2, %ymm9, %ymm2                           #4238.13
        vpaddd    %ymm5, %ymm15, %ymm11                         #4266.13
        vpand     %ymm12, %ymm3, %ymm0                          #4256.13
        vpaddd    %ymm1, %ymm2, %ymm2                           #4252.13
        vpsrld    $16, %ymm3, %ymm3                             #4249.13
        vpsrld    $16, %ymm11, %ymm15                           #4267.13
        vpand     %ymm12, %ymm8, %ymm1                          #4257.13
        vpaddd    %ymm2, %ymm0, %ymm0                           #4261.13
        vpmulld   %ymm4, %ymm9, %ymm4                           #4237.13
        vpaddd    %ymm0, %ymm15, %ymm5                          #4268.13
        vpaddd    %ymm3, %ymm4, %ymm0                           #4253.13
        vpsrld    $16, %ymm5, %ymm2                             #4269.13
        vpaddd    %ymm0, %ymm1, %ymm1                           #4262.13
        vpslld    $16, %ymm5, %ymm5                             #4276.19
        vpaddd    %ymm1, %ymm2, %ymm4                           #4270.13
        vpsrld    $16, %ymm8, %ymm1                             #4250.13
        vpsrld    $16, %ymm4, %ymm3                             #4271.13
        vpand     %ymm12, %ymm4, %ymm4                          #4274.13
        vpmulld   %ymm10, %ymm6, %ymm0                          #4241.13
        vpmulld   %ymm7, %ymm9, %ymm2                           #4236.13
        vpand     %ymm12, %ymm0, %ymm15                         #4258.13
        vpaddd    %ymm1, %ymm2, %ymm8                           #4254.13
        vpand     %ymm12, %ymm11, %ymm12                        #4273.13
        vmovups   .L_2il0floatpacket.57(%rip), %ymm0            #4286.15
        vpaddd    %ymm8, %ymm15, %ymm6                          #4263.13
        vpaddd    %ymm12, %ymm5, %ymm11                         #4278.19
        vpand     .L_2il0floatpacket.82(%rip), %ymm14, %ymm5    #4280.17
        vpaddd    %ymm6, %ymm3, %ymm3                           #4272.13
        vpxor     .L_2il0floatpacket.83(%rip), %ymm5, %ymm10    #4282.22
        vpslld    $16, %ymm3, %ymm7                             #4275.18
        vpand     .L_2il0floatpacket.86(%rip), %ymm11, %ymm3    #4296.20
        vpsrld    $18, %ymm11, %ymm11                           #4307.19
        vpaddd    %ymm4, %ymm7, %ymm8                           #4277.18
        vpxor     .L_2il0floatpacket.85(%rip), %ymm5, %ymm7     #4294.22
        vpslld    $5, %ymm3, %ymm4                              #4297.20
        vpsrld    $9, %ymm8, %ymm9                              #4283.20
        vpand     .L_2il0floatpacket.88(%rip), %ymm8, %ymm8     #4305.20
        vpor      %ymm7, %ymm4, %ymm6                           #4298.20
        vpor      %ymm10, %ymm9, %ymm2                          #4284.20
        vpxor     .L_2il0floatpacket.87(%rip), %ymm5, %ymm9     #4303.22
        vpslld    $14, %ymm8, %ymm5                             #4306.20
        vmovups   .L_2il0floatpacket.63(%rip), %ymm8            #4324.16
        vaddps    %ymm0, %ymm2, %ymm12                          #4287.19
        vsubps    %ymm0, %ymm12, %ymm1                          #4288.9
        vsubps    %ymm7, %ymm6, %ymm0                           #4301.17
        vpand     .L_2il0floatpacket.84(%rip), %ymm12, %ymm12   #4292.15
        vsubps    %ymm1, %ymm2, %ymm15                          #4289.17
        vpor      %ymm11, %ymm5, %ymm2                          #4308.20
        vmovups   .L_2il0floatpacket.58(%rip), %ymm5            #4317.20
        vpor      %ymm9, %ymm2, %ymm1                           #4309.20
        vsubps    %ymm9, %ymm1, %ymm3                           #4312.18
        vaddps    %ymm3, %ymm15, %ymm6                          #4313.11
        vsubps    %ymm6, %ymm15, %ymm15                         #4314.17
        vmulps    %ymm6, %ymm5, %ymm11                          #4319.17
        vaddps    %ymm15, %ymm3, %ymm4                          #4315.18
        vaddps    %ymm4, %ymm0, %ymm3                           #4316.11
        vmovdqu   .L_2il0floatpacket.90(%rip), %ymm4            #4336.23
        vmovaps   %ymm5, %ymm0                                  #4320.17
        vfmsub213ps %ymm11, %ymm6, %ymm0                        #4320.17
        vandps    .L_2il0floatpacket.62(%rip), %ymm14, %ymm7    #4325.11
        vcmpgt_oqps %ymm8, %ymm7, %ymm1                         #4326.17
        vcmple_oqps %ymm8, %ymm7, %ymm10                        #4327.20
        vfmadd132ps .L_2il0floatpacket.89(%rip), %ymm0, %ymm6   #4321.17
        vandps    %ymm14, %ymm10, %ymm0                         #4328.24
        vandps    %ymm11, %ymm1, %ymm2                          #4329.25
        vorps     %ymm2, %ymm0, %ymm15                          #4330.9
        vfmadd213ps %ymm6, %ymm5, %ymm3                         #4322.17
        vmovups   .L_2il0floatpacket.91(%rip), %ymm0            #4343.19
        vpsrld    $31, %ymm15, %ymm5                            #4335.22
        vpsubd    %ymm5, %ymm4, %ymm6                           #4336.23
        vpaddd    %ymm6, %ymm12, %ymm7                          #4337.23
        vpsrld    $2, %ymm7, %ymm10                             #4338.23
        vpslld    $2, %ymm10, %ymm8                             #4340.23
        vpsubd    %ymm8, %ymm12, %ymm12                         #4341.23
        vandps    %ymm3, %ymm1, %ymm9                           #4331.9
        vcvtdq2ps %ymm12, %ymm1                                 #4342.17
        vmovaps   %ymm15, %ymm3                                 #4345.26
        vfmadd231ps %ymm1, %ymm0, %ymm3                         #4345.26
        vfnmadd213ps %ymm3, %ymm1, %ymm0                        #4346.27
        vsubps    %ymm0, %ymm15, %ymm2                          #4347.27
        vmovups   (%rsp), %ymm0                                 #4350.17[spill]
        vfmadd132ps .L_2il0floatpacket.92(%rip), %ymm2, %ymm1   #4348.27
        vmovups   32(%rsp), %ymm2                               #4351.17[spill]
        vblendvps %ymm13, %ymm10, %ymm0, %ymm0                  #4350.17
        vblendvps %ymm13, %ymm3, %ymm2, %ymm2                   #4351.17
        vaddps    %ymm1, %ymm9, %ymm4                           #4349.27
        vmovups   64(%rsp), %ymm1                               #4352.18[spill]
        vblendvps %ymm13, %ymm4, %ymm1, %ymm1                   #4352.18
        jmp       ..B6.2        # Prob 100%                     #4352.18
        .align    16,0x90
                                # LOE rbx r12 r13 r14 eax ymm0 ymm1 ymm2 ymm14
	.cfi_endproc
# mark_end;
	.type	__svml_tanf8_ha_l9,@function
	.size	__svml_tanf8_ha_l9,.-__svml_tanf8_ha_l9
..LN__svml_tanf8_ha_l9.5:
	.data
# -- End  __svml_tanf8_ha_l9
	.text
.L_2__routine_start___svml_stan_ha_cout_rare_internal_6:
# -- Begin  __svml_stan_ha_cout_rare_internal
	.text
# mark_begin;
       .align    16,0x90
	.hidden __svml_stan_ha_cout_rare_internal
	.globl __svml_stan_ha_cout_rare_internal
# --- __svml_stan_ha_cout_rare_internal(const float *, float *)
__svml_stan_ha_cout_rare_internal:
# parameter 1: %rdi
# parameter 2: %rsi
..B7.1:                         # Preds ..B7.0
                                # Execution count [1.00e+00]
        .byte     243                                           #2414.1
        .byte     15                                            #2428.19
        .byte     30                                            #2428.19
        .byte     250                                           #2428.19
	.cfi_startproc
..___tag_value___svml_stan_ha_cout_rare_internal.233:
..L234:
                                                        #2414.1
        xorl      %eax, %eax                                    #2415.14
        movl      (%rdi), %edx                                  #2417.19
        movzwl    2(%rdi), %ecx                                 #2419.37
        movl      %edx, -8(%rsp)                                #2417.9
        andl      $32640, %ecx                                  #2419.37
        shrl      $24, %edx                                     #2418.30
        andl      $127, %edx                                    #2418.30
        movb      %dl, -5(%rsp)                                 #2418.30
        cmpl      $32640, %ecx                                  #2419.63
        je        ..B7.3        # Prob 16%                      #2419.63
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.2:                         # Preds ..B7.1
                                # Execution count [1.00e+00]
        ret                                                     #2431.16
                                # LOE
..B7.3:                         # Preds ..B7.1
                                # Execution count [1.60e-01]: Infreq
        cmpl      $2139095040, -8(%rsp)                         #2421.91
        jne       ..B7.5        # Prob 50%                      #2421.91
                                # LOE rbx rbp rsi rdi r12 r13 r14 r15 eax
..B7.4:                         # Preds ..B7.3
                                # Execution count [8.00e-02]: Infreq
        movss     (%rdi), %xmm0                                 #2423.34
        movl      $1, %eax                                      #2424.17
        mulss     _vmlsTanHATab(%rip), %xmm0                    #2423.53
        movss     %xmm0, (%rsi)                                 #2423.19
        ret                                                     #2423.19
                                # LOE rbx rbp r12 r13 r14 r15 eax
..B7.5:                         # Preds ..B7.3
                                # Execution count [8.00e-02]: Infreq
        movss     (%rdi), %xmm0                                 #2428.34
        mulss     (%rdi), %xmm0                                 #2428.41
        movss     %xmm0, (%rsi)                                 #2428.19
        ret                                                     #2428.19
        .align    16,0x90
                                # LOE rbx rbp r12 r13 r14 r15 eax
	.cfi_endproc
# mark_end;
	.type	__svml_stan_ha_cout_rare_internal,@function
	.size	__svml_stan_ha_cout_rare_internal,.-__svml_stan_ha_cout_rare_internal
..LN__svml_stan_ha_cout_rare_internal.6:
	.data
# -- End  __svml_stan_ha_cout_rare_internal
	.section .rodata, "a"
	.align 64
	.align 64
.L_2il0floatpacket.36:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.36,@object
	.size	.L_2il0floatpacket.36,64
	.align 64
.L_2il0floatpacket.37:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.37,@object
	.size	.L_2il0floatpacket.37,64
	.align 64
.L_2il0floatpacket.38:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.38,@object
	.size	.L_2il0floatpacket.38,64
	.align 64
.L_2il0floatpacket.39:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.39,@object
	.size	.L_2il0floatpacket.39,64
	.align 64
.L_2il0floatpacket.40:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.40,@object
	.size	.L_2il0floatpacket.40,64
	.align 64
.L_2il0floatpacket.41:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.41,@object
	.size	.L_2il0floatpacket.41,64
	.align 64
.L_2il0floatpacket.42:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.42,@object
	.size	.L_2il0floatpacket.42,64
	.align 64
.L_2il0floatpacket.43:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.43,@object
	.size	.L_2il0floatpacket.43,64
	.align 64
.L_2il0floatpacket.44:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.44,@object
	.size	.L_2il0floatpacket.44,64
	.align 64
.L_2il0floatpacket.45:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.45,@object
	.size	.L_2il0floatpacket.45,64
	.align 64
.L_2il0floatpacket.46:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.46,@object
	.size	.L_2il0floatpacket.46,64
	.align 64
.L_2il0floatpacket.47:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.47,@object
	.size	.L_2il0floatpacket.47,64
	.align 64
.L_2il0floatpacket.48:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.48,@object
	.size	.L_2il0floatpacket.48,64
	.align 64
.L_2il0floatpacket.49:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.49,@object
	.size	.L_2il0floatpacket.49,64
	.align 64
.L_2il0floatpacket.50:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.50,@object
	.size	.L_2il0floatpacket.50,64
	.align 64
.L_2il0floatpacket.51:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.51,@object
	.size	.L_2il0floatpacket.51,64
	.align 64
.L_2il0floatpacket.52:
	.long	0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002
	.type	.L_2il0floatpacket.52,@object
	.size	.L_2il0floatpacket.52,64
	.align 64
.L_2il0floatpacket.53:
	.long	0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb
	.type	.L_2il0floatpacket.53,@object
	.size	.L_2il0floatpacket.53,64
	.align 64
.L_2il0floatpacket.54:
	.long	0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e
	.type	.L_2il0floatpacket.54,@object
	.size	.L_2il0floatpacket.54,64
	.align 64
	.hidden __svml_stan_ha_reduction_data_internal
	.globl __svml_stan_ha_reduction_data_internal
__svml_stan_ha_reduction_data_internal:
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
	.long	0
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
	.long	2
	.long	0
	.long	0
	.long	5
	.long	0
	.long	0
	.long	10
	.long	0
	.long	0
	.long	20
	.long	0
	.long	0
	.long	40
	.long	0
	.long	0
	.long	81
	.long	0
	.long	0
	.long	162
	.long	0
	.long	0
	.long	325
	.long	0
	.long	0
	.long	651
	.long	0
	.long	0
	.long	1303
	.long	0
	.long	0
	.long	2607
	.long	0
	.long	0
	.long	5215
	.long	0
	.long	0
	.long	10430
	.long	0
	.long	0
	.long	20860
	.long	0
	.long	0
	.long	41721
	.long	0
	.long	0
	.long	83443
	.long	0
	.long	0
	.long	166886
	.long	0
	.long	0
	.long	333772
	.long	0
	.long	0
	.long	667544
	.long	0
	.long	0
	.long	1335088
	.long	0
	.long	0
	.long	2670176
	.long	0
	.long	0
	.long	5340353
	.long	0
	.long	0
	.long	10680707
	.long	0
	.long	0
	.long	21361414
	.long	0
	.long	0
	.long	42722829
	.long	0
	.long	0
	.long	85445659
	.long	0
	.long	0
	.long	170891318
	.long	0
	.long	0
	.long	341782637
	.long	0
	.long	0
	.long	683565275
	.long	0
	.long	0
	.long	1367130551
	.long	0
	.long	0
	.long	2734261102
	.long	0
	.long	1
	.long	1173554908
	.long	0
	.long	2
	.long	2347109817
	.long	0
	.long	5
	.long	399252338
	.long	0
	.long	10
	.long	798504676
	.long	0
	.long	20
	.long	1597009353
	.long	0
	.long	40
	.long	3194018707
	.long	0
	.long	81
	.long	2093070119
	.long	0
	.long	162
	.long	4186140238
	.long	0
	.long	325
	.long	4077313180
	.long	0
	.long	651
	.long	3859659065
	.long	0
	.long	1303
	.long	3424350834
	.long	0
	.long	2607
	.long	2553734372
	.long	0
	.long	5215
	.long	812501448
	.long	0
	.long	10430
	.long	1625002897
	.long	0
	.long	20860
	.long	3250005794
	.long	0
	.long	41721
	.long	2205044292
	.long	0
	.long	83443
	.long	115121288
	.long	0
	.long	166886
	.long	230242576
	.long	0
	.long	333772
	.long	460485152
	.long	0
	.long	667544
	.long	920970305
	.long	0
	.long	1335088
	.long	1841940610
	.long	0
	.long	2670176
	.long	3683881221
	.long	0
	.long	5340353
	.long	3072795146
	.long	0
	.long	10680707
	.long	1850622997
	.long	0
	.long	21361414
	.long	3701245994
	.long	0
	.long	42722829
	.long	3107524692
	.long	0
	.long	85445659
	.long	1920082089
	.long	0
	.long	170891318
	.long	3840164178
	.long	0
	.long	341782637
	.long	3385361061
	.long	0
	.long	683565275
	.long	2475754826
	.long	0
	.long	1367130551
	.long	656542356
	.long	0
	.long	2734261102
	.long	1313084713
	.long	1
	.long	1173554908
	.long	2626169427
	.long	2
	.long	2347109817
	.long	957371559
	.long	5
	.long	399252338
	.long	1914743119
	.long	10
	.long	798504676
	.long	3829486239
	.long	20
	.long	1597009353
	.long	3364005183
	.long	40
	.long	3194018707
	.long	2433043071
	.long	81
	.long	2093070119
	.long	571118846
	.long	162
	.long	4186140238
	.long	1142237692
	.long	325
	.long	4077313180
	.long	2284475384
	.long	651
	.long	3859659065
	.long	273983472
	.long	1303
	.long	3424350834
	.long	547966945
	.long	2607
	.long	2553734372
	.long	1095933890
	.long	5215
	.long	812501448
	.long	2191867780
	.long	10430
	.long	1625002897
	.long	88768265
	.long	20860
	.long	3250005794
	.long	177536531
	.long	41721
	.long	2205044292
	.long	355073063
	.long	83443
	.long	115121288
	.long	710146126
	.long	166886
	.long	230242576
	.long	1420292253
	.long	333772
	.long	460485152
	.long	2840584506
	.long	667544
	.long	920970305
	.long	1386201717
	.long	1335088
	.long	1841940610
	.long	2772403434
	.long	2670176
	.long	3683881221
	.long	1249839573
	.long	5340353
	.long	3072795146
	.long	2499679147
	.long	10680707
	.long	1850622997
	.long	704390999
	.long	21361414
	.long	3701245994
	.long	1408781999
	.long	42722829
	.long	3107524692
	.long	2817563999
	.long	85445659
	.long	1920082089
	.long	1340160702
	.long	170891318
	.long	3840164178
	.long	2680321405
	.long	341782637
	.long	3385361061
	.long	1065675514
	.long	683565275
	.long	2475754826
	.long	2131351028
	.long	1367130551
	.long	656542356
	.long	4262702056
	.long	2734261102
	.long	1313084713
	.long	4230436817
	.long	1173554908
	.long	2626169427
	.long	4165906339
	.long	2347109817
	.long	957371559
	.long	4036845383
	.long	399252338
	.long	1914743119
	.long	3778723471
	.long	798504676
	.long	3829486239
	.long	3262479647
	.long	1597009353
	.long	3364005183
	.long	2229991998
	.long	3194018707
	.long	2433043071
	.long	165016701
	.long	2093070119
	.long	571118846
	.long	330033402
	.long	4186140238
	.long	1142237692
	.long	660066805
	.long	4077313180
	.long	2284475384
	.long	1320133610
	.long	3859659065
	.long	273983472
	.long	2640267220
	.long	3424350834
	.long	547966945
	.long	985567145
	.long	2553734372
	.long	1095933890
	.long	1971134291
	.long	812501448
	.long	2191867780
	.long	3942268582
	.long	1625002897
	.long	88768265
	.long	3589569869
	.long	3250005794
	.long	177536531
	.long	2884172442
	.long	2205044292
	.long	355073063
	.long	1473377588
	.long	115121288
	.long	710146126
	.long	2946755177
	.long	230242576
	.long	1420292253
	.long	1598543059
	.long	460485152
	.long	2840584506
	.long	3197086118
	.long	920970305
	.long	1386201717
	.long	2099204941
	.long	1841940610
	.long	2772403434
	.long	4198409883
	.long	3683881221
	.long	1249839573
	.long	4101852471
	.long	3072795146
	.long	2499679147
	.long	3908737646
	.long	1850622997
	.long	704390999
	.long	3522507997
	.long	3701245994
	.long	1408781999
	.long	2750048699
	.long	3107524692
	.long	2817563999
	.long	1205130103
	.long	1920082089
	.long	1340160702
	.long	2410260206
	.long	3840164178
	.long	2680321405
	.long	525553116
	.long	3385361061
	.long	1065675514
	.long	1051106232
	.long	2475754826
	.long	2131351028
	.long	2102212464
	.long	656542356
	.long	4262702056
	.long	4204424928
	.long	1313084713
	.long	4230436817
	.long	4113882560
	.long	2626169427
	.long	4165906339
	.long	3932797825
	.long	957371559
	.long	4036845383
	.long	3570628355
	.long	1914743119
	.long	3778723471
	.long	2846289414
	.long	3829486239
	.long	3262479647
	.long	1397611533
	.long	3364005183
	.long	2229991998
	.long	2795223067
	.long	2433043071
	.long	165016701
	.long	1295478838
	.long	571118846
	.long	330033402
	.long	2590957677
	.long	1142237692
	.long	660066805
	.long	886948059
	.long	2284475384
	.long	1320133610
	.long	1773896118
	.long	273983472
	.long	2640267220
	.long	3547792237
	.long	547966945
	.long	985567145
	.long	2800617179
	.long	1095933890
	.long	1971134291
	.long	1306267062
	.long	2191867780
	.long	3942268582
	.long	2612534124
	.long	88768265
	.long	3589569869
	.long	930100952
	.long	177536531
	.long	2884172442
	.long	1860201905
	.long	355073063
	.long	1473377588
	.long	3720403810
	.long	710146126
	.long	2946755177
	.long	3145840325
	.long	1420292253
	.long	1598543059
	.long	1996713354
	.long	2840584506
	.long	3197086118
	.long	3993426708
	.long	1386201717
	.long	2099204941
	.long	3691886121
	.long	2772403434
	.long	4198409883
	.long	3088804946
	.long	1249839573
	.long	4101852471
	.long	1882642597
	.long	2499679147
	.long	3908737646
	.long	3765285194
	.long	704390999
	.long	3522507997
	.long	3235603093
	.long	1408781999
	.long	2750048699
	.long	2176238891
	.long	2817563999
	.long	1205130103
	.long	57510486
	.long	1340160702
	.long	2410260206
	.long	115020972
	.long	2680321405
	.long	525553116
	.long	230041945
	.long	1065675514
	.long	1051106232
	.long	460083891
	.long	2131351028
	.long	2102212464
	.long	920167782
	.long	4262702056
	.long	4204424928
	.long	1840335564
	.long	4230436817
	.long	4113882560
	.long	3680671129
	.long	4165906339
	.long	3932797825
	.long	3066374962
	.long	4036845383
	.long	3570628355
	.long	1837782628
	.long	3778723471
	.long	2846289414
	.long	3675565257
	.long	3262479647
	.long	1397611533
	.long	3056163219
	.long	2229991998
	.long	2795223067
	.long	1817359143
	.long	165016701
	.long	1295478838
	.long	3634718287
	.long	330033402
	.long	2590957677
	.long	2974469278
	.long	660066805
	.long	886948059
	.long	1653971260
	.long	1320133610
	.long	1773896118
	.long	3307942520
	.long	2640267220
	.long	3547792237
	.long	2320917745
	.long	985567145
	.long	2800617179
	.long	346868194
	.long	1971134291
	.long	1306267062
	.long	693736388
	.long	3942268582
	.long	2612534124
	.long	1387472776
	.long	3589569869
	.long	930100952
	.long	2774945552
	.long	2884172442
	.long	1860201905
	.long	1254923809
	.long	1473377588
	.long	3720403810
	.long	2509847619
	.long	2946755177
	.long	3145840325
	.long	724727943
	.long	1598543059
	.long	1996713354
	.long	1449455886
	.long	3197086118
	.long	3993426708
	.long	2898911772
	.long	2099204941
	.long	3691886121
	.long	1502856249
	.long	4198409883
	.long	3088804946
	.long	3005712498
	.long	4101852471
	.long	1882642597
	.long	1716457700
	.long	3908737646
	.long	3765285194
	.long	3432915400
	.long	3522507997
	.long	3235603093
	.long	2570863504
	.long	2750048699
	.long	2176238891
	.long	846759712
	.long	1205130103
	.long	57510486
	.long	1693519425
	.long	2410260206
	.long	115020972
	.long	3387038850
	.long	525553116
	.long	230041945
	.long	2479110404
	.long	1051106232
	.long	460083891
	.long	663253512
	.long	2102212464
	.long	920167782
	.long	1326507024
	.long	4204424928
	.long	1840335564
	.long	2653014048
	.long	4113882560
	.long	3680671129
	.long	1011060801
	.long	3932797825
	.long	3066374962
	.long	2022121603
	.long	3570628355
	.long	1837782628
	.long	4044243207
	.long	2846289414
	.long	3675565257
	.long	3793519119
	.long	1397611533
	.long	3056163219
	.long	3292070943
	.long	2795223067
	.long	1817359143
	.long	2289174591
	.long	1295478838
	.long	3634718287
	.long	283381887
	.long	2590957677
	.long	2974469278
	.long	566763775
	.type	__svml_stan_ha_reduction_data_internal,@object
	.size	__svml_stan_ha_reduction_data_internal,3072
	.align 64
	.hidden __svml_stan_ha_data_internal
	.globl __svml_stan_ha_data_internal
__svml_stan_ha_data_internal:
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1092811139
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	1036586970
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	832708968
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	633484485
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	832708608
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	708075802
	.long	2147483648
	.long	1036629468
	.long	1045147567
	.long	1050366018
	.long	1054086093
	.long	1057543609
	.long	1059786177
	.long	1062344705
	.long	1065353216
	.long	1067186156
	.long	1069519047
	.long	1072658590
	.long	1075479162
	.long	1079179983
	.long	1084284919
	.long	1092776803
	.long	4286578687
	.long	3240260451
	.long	3231768567
	.long	3226663631
	.long	3222962810
	.long	3220142238
	.long	3217002695
	.long	3214669804
	.long	3212836864
	.long	3209828353
	.long	3207269825
	.long	3205027257
	.long	3201569741
	.long	3197849666
	.long	3192631215
	.long	3184113116
	.long	2147483648
	.long	826651354
	.long	791306928
	.long	2989111746
	.long	2982175258
	.long	2992568675
	.long	850100121
	.long	850281093
	.long	0
	.long	861435400
	.long	840342808
	.long	3003924160
	.long	3016492578
	.long	865099790
	.long	856723932
	.long	3025444934
	.long	4085252096
	.long	877961286
	.long	3004207580
	.long	3012583438
	.long	869008930
	.long	856440512
	.long	2987826456
	.long	3008919048
	.long	0
	.long	2997764741
	.long	2997583769
	.long	845085027
	.long	834691610
	.long	841628098
	.long	2938790576
	.long	2974135002
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1051372198
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1040758920
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1174470656
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
	.long	1059256707
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
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
	.long	2139095040
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
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1176256512
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1413742592
	.long	1073291771
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
	.long	1280075305
	.long	1032227875
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
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
	.long	116221458
	.long	3216541303
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
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	3891301393
	.long	3218831093
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	2796173298
	.long	1065608814
	.long	1070141403
	.long	3007036718
	.long	0
	.long	0
	.long	0
	.long	1065353216
	.long	0
	.long	0
	.long	1051372765
	.long	0
	.long	1069935515
	.long	853435276
	.long	0
	.long	1019812401
	.long	797871386
	.long	1065353216
	.long	975043072
	.long	1019820333
	.long	1051400329
	.long	1015569723
	.long	1069729628
	.long	2999697034
	.long	0
	.long	1028208956
	.long	816029531
	.long	1065353216
	.long	991832832
	.long	1028240852
	.long	1051479824
	.long	1023251493
	.long	1069523740
	.long	860164016
	.long	0
	.long	1033310670
	.long	827321128
	.long	1065353216
	.long	1001540608
	.long	1033364538
	.long	1051617929
	.long	1028458464
	.long	1069317853
	.long	2977958621
	.long	0
	.long	1036629468
	.long	826649990
	.long	1065353216
	.long	1008660256
	.long	1036757738
	.long	1051807326
	.long	1032162226
	.long	1069111966
	.long	3009745511
	.long	0
	.long	1039964354
	.long	2964214364
	.long	1065353216
	.long	1014578464
	.long	1040201797
	.long	1052059423
	.long	1034708638
	.long	1068906078
	.long	848017692
	.long	0
	.long	1041753444
	.long	2982519524
	.long	1065353216
	.long	1018446032
	.long	1041972480
	.long	1052374628
	.long	1037453248
	.long	1068700191
	.long	3004118141
	.long	0
	.long	1043443277
	.long	2985501265
	.long	1065353216
	.long	1022797056
	.long	1043793882
	.long	1052746889
	.long	1039915463
	.long	1068494303
	.long	857455223
	.long	0
	.long	1045147567
	.long	791292384
	.long	1065353216
	.long	1025642520
	.long	1045675728
	.long	1053195814
	.long	1041590498
	.long	1068288416
	.long	2992986704
	.long	0
	.long	1046868583
	.long	833925599
	.long	1065353216
	.long	1028557712
	.long	1047628490
	.long	1053716836
	.long	1043186017
	.long	1068082528
	.long	863082593
	.long	0
	.long	1048592340
	.long	2988940902
	.long	1065353216
	.long	1031831496
	.long	1049119700
	.long	1054310701
	.long	1044788971
	.long	1067876641
	.long	837040812
	.long	0
	.long	1049473154
	.long	2972885556
	.long	1065353216
	.long	1033689040
	.long	1050184288
	.long	1054999523
	.long	1046698028
	.long	1067670754
	.long	3006826934
	.long	0
	.long	1050366018
	.long	2989112046
	.long	1065353216
	.long	1035760784
	.long	1051302645
	.long	1055777031
	.long	1048635818
	.long	1067464866
	.long	853854846
	.long	0
	.long	1051272279
	.long	817367088
	.long	1065353216
	.long	1038057984
	.long	1052482025
	.long	1056656040
	.long	1049723582
	.long	1067258979
	.long	2999277465
	.long	0
	.long	1052193360
	.long	2986510371
	.long	1065353216
	.long	1040390392
	.long	1053730424
	.long	1057307751
	.long	1050943059
	.long	1067053091
	.long	860373800
	.long	0
	.long	1053130765
	.long	2987705281
	.long	1065353216
	.long	1041784404
	.long	1055056706
	.long	1057868403
	.long	1052298273
	.long	1066847204
	.long	2974604846
	.long	0
	.long	1054086093
	.long	2982175058
	.long	1065353216
	.long	1043312844
	.long	1056470731
	.long	1058502663
	.long	1053852727
	.long	1066641317
	.long	3009535726
	.long	0
	.long	1055061049
	.long	2985572766
	.long	1065353216
	.long	1044984860
	.long	1057474074
	.long	1059214863
	.long	1055565854
	.long	1066435429
	.long	848437261
	.long	0
	.long	1056057456
	.long	844263924
	.long	1065353216
	.long	1046810746
	.long	1058286064
	.long	1060014844
	.long	1057227928
	.long	1066229542
	.long	3003908357
	.long	0
	.long	1057020941
	.long	2987700082
	.long	1065353216
	.long	1048689044
	.long	1059160627
	.long	1060914481
	.long	1058313864
	.long	1066023654
	.long	857665008
	.long	0
	.long	1057543609
	.long	2992568718
	.long	1065353216
	.long	1049773965
	.long	1060105673
	.long	1061932376
	.long	1059565214
	.long	1065817767
	.long	2992147565
	.long	0
	.long	1058080175
	.long	854607280
	.long	1065353216
	.long	1050955490
	.long	1061130203
	.long	1063075792
	.long	1060964899
	.long	1065611879
	.long	863292377
	.long	0
	.long	1058631876
	.long	848316488
	.long	1065353216
	.long	1052241912
	.long	1062244476
	.long	1064374250
	.long	1062608877
	.long	1065405992
	.long	838719090
	.long	0
	.long	1059200055
	.long	2987155932
	.long	1065353216
	.long	1053642609
	.long	1063460266
	.long	1065596017
	.long	1064468970
	.long	1065046993
	.long	848647046
	.long	0
	.long	1059786177
	.long	850099898
	.long	1065353216
	.long	1055168194
	.long	1064791104
	.long	1066427841
	.long	1065988022
	.long	1064635218
	.long	854274415
	.long	0
	.long	1060391849
	.long	2998448362
	.long	1065353216
	.long	1056830711
	.long	1065802920
	.long	1067373883
	.long	1067237086
	.long	1064223444
	.long	2998857895
	.long	0
	.long	1061018831
	.long	852661766
	.long	1073741824
	.long	3202769007
	.long	1066608086
	.long	1068453481
	.long	1068697612
	.long	1063811669
	.long	2991727995
	.long	0
	.long	1061669068
	.long	2986407194
	.long	1073741824
	.long	3200789612
	.long	1067498217
	.long	1069688111
	.long	1070408903
	.long	1063399894
	.long	2971248290
	.long	0
	.long	1062344705
	.long	850280824
	.long	1073741824
	.long	3198626104
	.long	1068485666
	.long	1071103306
	.long	1072410651
	.long	1062988119
	.long	839209514
	.long	0
	.long	1063048126
	.long	826671880
	.long	1073741824
	.long	3196257989
	.long	1069584946
	.long	1072731698
	.long	1074256640
	.long	1062576344
	.long	848856831
	.long	0
	.long	1063781982
	.long	845614362
	.long	1073741824
	.long	3191263702
	.long	1070813191
	.long	1074178145
	.long	1075661786
	.long	1062164569
	.long	854484200
	.long	0
	.long	1064549237
	.long	855412877
	.long	1073741824
	.long	3183449264
	.long	1072190735
	.long	1075269479
	.long	1077331464
	.long	1061752795
	.long	2998648110
	.long	1065353216
	.long	3196839438
	.long	839748996
	.long	1056964608
	.long	3187152817
	.long	3179496939
	.long	1025375660
	.long	3159543663
	.long	1061341020
	.long	2991308426
	.long	1065353216
	.long	3196528703
	.long	2993207654
	.long	1056964608
	.long	3187565865
	.long	3178961235
	.long	1025040649
	.long	3158667440
	.long	1060929245
	.long	2969570013
	.long	1065353216
	.long	3196220448
	.long	839617357
	.long	1048576000
	.long	1039897640
	.long	3178234548
	.long	1024731756
	.long	3157936127
	.long	1060517470
	.long	839629084
	.long	1065353216
	.long	3195769474
	.long	2972943314
	.long	1048576000
	.long	1039520273
	.long	3177530035
	.long	1024452069
	.long	3157392148
	.long	1060105695
	.long	849066615
	.long	1065353216
	.long	3195162227
	.long	824230882
	.long	1048576000
	.long	1039159939
	.long	3176846430
	.long	1024176063
	.long	3156719803
	.long	1059693920
	.long	854693985
	.long	1065353216
	.long	3194559300
	.long	837912886
	.long	1048576000
	.long	1038816139
	.long	3176182519
	.long	1023917626
	.long	3156100775
	.long	1059282146
	.long	2998438326
	.long	1065353216
	.long	3193960492
	.long	2976936506
	.long	1048576000
	.long	1038488404
	.long	3175537158
	.long	1023672824
	.long	3155484691
	.long	1058870371
	.long	2990888857
	.long	1065353216
	.long	3193365611
	.long	837021697
	.long	1048576000
	.long	1038176293
	.long	3174909264
	.long	1023428141
	.long	3154717848
	.long	1058458596
	.long	2966216238
	.long	1065353216
	.long	3192774465
	.long	2981011604
	.long	1048576000
	.long	1037879388
	.long	3174297790
	.long	1023026096
	.long	3154246903
	.long	1058046821
	.long	840048653
	.long	1065353216
	.long	3192186872
	.long	2982847435
	.long	1048576000
	.long	1037597300
	.long	3173701765
	.long	1022609285
	.long	3153191752
	.long	1057635046
	.long	849276400
	.long	1065353216
	.long	3191602652
	.long	2972865050
	.long	1048576000
	.long	1037329660
	.long	3173120241
	.long	1022242934
	.long	3152466531
	.long	1057223271
	.long	854903769
	.long	1065353216
	.long	3191021630
	.long	838792638
	.long	1048576000
	.long	1037076124
	.long	3172552332
	.long	1021893801
	.long	3151682133
	.long	1056658385
	.long	840258438
	.long	1065353216
	.long	3190443633
	.long	2979855596
	.long	1048576000
	.long	1036836369
	.long	3171997189
	.long	1021543079
	.long	3150495127
	.long	1055834836
	.long	2990469287
	.long	1065353216
	.long	3189868496
	.long	2981611511
	.long	1048576000
	.long	1036610091
	.long	3171453986
	.long	1021220110
	.long	3149437649
	.long	1055011286
	.long	2962859682
	.long	1065353216
	.long	3189296055
	.long	2950857776
	.long	1048576000
	.long	1036397006
	.long	3170921933
	.long	1020942892
	.long	3148919762
	.long	1054187736
	.long	840468223
	.long	1065353216
	.long	3188726149
	.long	2955915960
	.long	1048576000
	.long	1036196851
	.long	3169906765
	.long	1020660679
	.long	3147905210
	.long	1053364187
	.long	2990259502
	.long	1065353216
	.long	3188158621
	.long	2978622083
	.long	1048576000
	.long	1036009378
	.long	3168882838
	.long	1020421234
	.long	3147436656
	.long	1052540637
	.long	2961181405
	.long	1065353216
	.long	3187515595
	.long	789904544
	.long	1048576000
	.long	1035834359
	.long	3167876891
	.long	1020189995
	.long	3146799430
	.long	1051717087
	.long	840678007
	.long	1065353216
	.long	3186389132
	.long	2974324164
	.long	1048576000
	.long	1035671582
	.long	3166887590
	.long	1019957287
	.long	3145677161
	.long	1050893538
	.long	2990049718
	.long	1065353216
	.long	3185266517
	.long	821445502
	.long	1048576000
	.long	1035520850
	.long	3165913616
	.long	1019751749
	.long	3143905397
	.long	1050069988
	.long	2957827630
	.long	1065353216
	.long	3184147455
	.long	823956970
	.long	1048576000
	.long	1035381982
	.long	3164953691
	.long	1019591684
	.long	3143870825
	.long	1049246438
	.long	840887792
	.long	1065353216
	.long	3183031657
	.long	2948197632
	.long	1048576000
	.long	1035254815
	.long	3164006661
	.long	1019406069
	.long	3141406886
	.long	1048269777
	.long	831869830
	.long	1065353216
	.long	3181918839
	.long	829265530
	.long	1048576000
	.long	1035139196
	.long	3163071263
	.long	1019275107
	.long	3141473894
	.long	1046622678
	.long	2954471074
	.long	1065353216
	.long	3180808717
	.long	2974758491
	.long	1048576000
	.long	1035034991
	.long	3161787608
	.long	1019131285
	.long	3139614851
	.long	1044975579
	.long	2981870894
	.long	1065353216
	.long	3179701015
	.long	2951749952
	.long	1048576000
	.long	1034942077
	.long	3159956688
	.long	1019002541
	.long	3137649644
	.long	1043328479
	.long	832289399
	.long	1065353216
	.long	3177908479
	.long	2968441398
	.long	1048576000
	.long	1034860345
	.long	3158142289
	.long	1018906717
	.long	3137336762
	.long	1041681380
	.long	2949439022
	.long	1065353216
	.long	3175701100
	.long	2963548093
	.long	1048576000
	.long	1034789701
	.long	3156342344
	.long	1018810804
	.long	3133887847
	.long	1039881169
	.long	823481222
	.long	1065353216
	.long	3173496918
	.long	2969038481
	.long	1048576000
	.long	1034730062
	.long	3154554595
	.long	1018750428
	.long	3136028910
	.long	1036586971
	.long	2973482286
	.long	1065353216
	.long	3171295395
	.long	2968300352
	.long	1048576000
	.long	1034681361
	.long	3151437839
	.long	1018664053
	.long	3123383004
	.long	1033292772
	.long	2941050414
	.long	1065353216
	.long	3167298168
	.long	808398440
	.long	1048576000
	.long	1034643540
	.long	3147899215
	.long	1018610153
	.long	943964915
	.long	1028198363
	.long	2965093678
	.long	1065353216
	.long	3162902549
	.long	2950073902
	.long	1048576000
	.long	1034616555
	.long	3143016255
	.long	1018603598
	.long	3133555092
	.long	1019809755
	.long	2956705070
	.long	1065353216
	.long	3154512883
	.long	803361198
	.long	1048576000
	.long	1034600377
	.long	3134618720
	.long	1018580133
	.long	3134056577
	.long	0
	.long	0
	.long	1065353216
	.long	0
	.long	0
	.long	1048576000
	.long	1034594987
	.long	0
	.long	1018552971
	.long	0
	.long	3167293403
	.long	809221422
	.long	1065353216
	.long	1007029235
	.long	2950844846
	.long	1048576000
	.long	1034600377
	.long	987135072
	.long	1018580133
	.long	986572929
	.long	3175682011
	.long	817610030
	.long	1065353216
	.long	1015418901
	.long	802590254
	.long	1048576000
	.long	1034616555
	.long	995532607
	.long	1018603598
	.long	986071444
	.long	3180776420
	.long	793566766
	.long	1065353216
	.long	1019814520
	.long	2955882088
	.long	1048576000
	.long	1034643540
	.long	1000415567
	.long	1018610153
	.long	3091448562
	.long	3184070619
	.long	825998638
	.long	1065353216
	.long	1023811747
	.long	820816704
	.long	1048576000
	.long	1034681361
	.long	1003954191
	.long	1018664053
	.long	975899356
	.long	3187364817
	.long	2970964870
	.long	1065353216
	.long	1026013270
	.long	821554833
	.long	1048576000
	.long	1034730062
	.long	1007070947
	.long	1018750428
	.long	988545262
	.long	3189165028
	.long	801955374
	.long	1065353216
	.long	1028217452
	.long	816064445
	.long	1048576000
	.long	1034789701
	.long	1008858696
	.long	1018810804
	.long	986404199
	.long	3190812127
	.long	2979773047
	.long	1065353216
	.long	1030424831
	.long	820957750
	.long	1048576000
	.long	1034860345
	.long	1010658641
	.long	1018906717
	.long	989853114
	.long	3192459227
	.long	834387246
	.long	1065353216
	.long	1032217367
	.long	804266304
	.long	1048576000
	.long	1034942077
	.long	1012473040
	.long	1019002541
	.long	990165996
	.long	3194106326
	.long	806987426
	.long	1065353216
	.long	1033325069
	.long	827274843
	.long	1048576000
	.long	1035034991
	.long	1014303960
	.long	1019131285
	.long	992131203
	.long	3195753425
	.long	2979353478
	.long	1065353216
	.long	1034435191
	.long	2976749178
	.long	1048576000
	.long	1035139196
	.long	1015587615
	.long	1019275107
	.long	993990246
	.long	3196730086
	.long	2988371440
	.long	1065353216
	.long	1035548009
	.long	800713984
	.long	1048576000
	.long	1035254815
	.long	1016523013
	.long	1019406069
	.long	993923238
	.long	3197553636
	.long	810343982
	.long	1065353216
	.long	1036663807
	.long	2971440618
	.long	1048576000
	.long	1035381982
	.long	1017470043
	.long	1019591684
	.long	996387177
	.long	3198377186
	.long	842566070
	.long	1065353216
	.long	1037782869
	.long	2968929150
	.long	1048576000
	.long	1035520850
	.long	1018429968
	.long	1019751749
	.long	996421749
	.long	3199200735
	.long	2988161655
	.long	1065353216
	.long	1038905484
	.long	826840516
	.long	1048576000
	.long	1035671582
	.long	1019403942
	.long	1019957287
	.long	998193513
	.long	3200024285
	.long	813697757
	.long	1065353216
	.long	1040031947
	.long	2937388192
	.long	1048576000
	.long	1035834359
	.long	1020393243
	.long	1020189995
	.long	999315782
	.long	3200847835
	.long	842775854
	.long	1065353216
	.long	1040674973
	.long	831138435
	.long	1048576000
	.long	1036009378
	.long	1021399190
	.long	1020421234
	.long	999953008
	.long	3201671384
	.long	2987951871
	.long	1065353216
	.long	1041242501
	.long	808432312
	.long	1048576000
	.long	1036196851
	.long	1022423117
	.long	1020660679
	.long	1000421562
	.long	3202494934
	.long	815376034
	.long	1065353216
	.long	1041812407
	.long	803374128
	.long	1048576000
	.long	1036397006
	.long	1023438285
	.long	1020942892
	.long	1001436114
	.long	3203318484
	.long	842985639
	.long	1065353216
	.long	1042384848
	.long	834127863
	.long	1048576000
	.long	1036610091
	.long	1023970338
	.long	1021220110
	.long	1001954001
	.long	3204142033
	.long	2987742086
	.long	1065353216
	.long	1042959985
	.long	832371948
	.long	1048576000
	.long	1036836369
	.long	1024513541
	.long	1021543079
	.long	1003011479
	.long	3204706919
	.long	3002387417
	.long	1065353216
	.long	1043537982
	.long	2986276286
	.long	1048576000
	.long	1037076124
	.long	1025068684
	.long	1021893801
	.long	1004198485
	.long	3205118694
	.long	2996760048
	.long	1065353216
	.long	1044119004
	.long	825381402
	.long	1048576000
	.long	1037329660
	.long	1025636593
	.long	1022242934
	.long	1004982883
	.long	3205530469
	.long	2987532301
	.long	1065353216
	.long	1044703224
	.long	835363787
	.long	1048576000
	.long	1037597300
	.long	1026218117
	.long	1022609285
	.long	1005708104
	.long	3205942244
	.long	818732590
	.long	1065353216
	.long	1045290817
	.long	833527956
	.long	1048576000
	.long	1037879388
	.long	1026814142
	.long	1023026096
	.long	1006763255
	.long	3206354019
	.long	843405209
	.long	1065353216
	.long	1045881963
	.long	2984505345
	.long	1048576000
	.long	1038176293
	.long	1027425616
	.long	1023428141
	.long	1007234200
	.long	3206765794
	.long	850954678
	.long	1065353216
	.long	1046476844
	.long	829452858
	.long	1048576000
	.long	1038488404
	.long	1028053510
	.long	1023672824
	.long	1008001043
	.long	3207177568
	.long	3002177633
	.long	1065353216
	.long	1047075652
	.long	2985396534
	.long	1048576000
	.long	1038816139
	.long	1028698871
	.long	1023917626
	.long	1008617127
	.long	3207589343
	.long	2996550263
	.long	1065353216
	.long	1047678579
	.long	2971714530
	.long	1048576000
	.long	1039159939
	.long	1029362782
	.long	1024176063
	.long	1009236155
	.long	3208001118
	.long	2987112732
	.long	1065353216
	.long	1048285826
	.long	825459666
	.long	1048576000
	.long	1039520273
	.long	1030046387
	.long	1024452069
	.long	1009908500
	.long	3208412893
	.long	822086365
	.long	1065353216
	.long	1048736800
	.long	2987101005
	.long	1048576000
	.long	1039897640
	.long	1030750900
	.long	1024731756
	.long	1010452479
	.long	3208824668
	.long	843824778
	.long	1065353216
	.long	1049045055
	.long	845724006
	.long	1056964608
	.long	3187565865
	.long	1031477587
	.long	1025040649
	.long	1011183792
	.long	3209236443
	.long	851164462
	.long	0
	.long	3212836864
	.long	725680128
	.long	1073741824
	.long	3003121664
	.long	3221225472
	.long	1076541384
	.long	3226821083
	.long	3209648217
	.long	3001967848
	.long	0
	.long	3212032885
	.long	3002896525
	.long	1073741824
	.long	3183449264
	.long	3219674383
	.long	1075269479
	.long	3224815112
	.long	3210059992
	.long	2996340479
	.long	0
	.long	3211265630
	.long	2993098010
	.long	1073741824
	.long	3191263702
	.long	3218296839
	.long	1074178145
	.long	3223145434
	.long	3210471767
	.long	2986693162
	.long	0
	.long	3210531774
	.long	2974155528
	.long	1073741824
	.long	3196257989
	.long	3217068594
	.long	1072731698
	.long	3221740288
	.long	3210883542
	.long	823764642
	.long	0
	.long	3209828353
	.long	2997764472
	.long	1073741824
	.long	3198626104
	.long	3215969314
	.long	1071103306
	.long	3219894299
	.long	3211295317
	.long	844244347
	.long	0
	.long	3209152716
	.long	838923546
	.long	1073741824
	.long	3200789612
	.long	3214981865
	.long	1069688111
	.long	3217892551
	.long	3211707092
	.long	851374247
	.long	0
	.long	3208502479
	.long	3000145414
	.long	1073741824
	.long	3202769007
	.long	3214091734
	.long	1068453481
	.long	3216181260
	.long	3212118866
	.long	3001758063
	.long	0
	.long	3207875497
	.long	850964714
	.long	1065353216
	.long	1056830711
	.long	3213286568
	.long	1067373883
	.long	3214720734
	.long	3212530641
	.long	2996130694
	.long	0
	.long	3207269825
	.long	2997583546
	.long	1065353216
	.long	1055168194
	.long	3212274752
	.long	1066427841
	.long	3213471670
	.long	3212889640
	.long	2986202738
	.long	0
	.long	3206683703
	.long	839672284
	.long	1065353216
	.long	1053642609
	.long	3210943914
	.long	1065596017
	.long	3211952618
	.long	3213095527
	.long	3010776025
	.long	0
	.long	3206115524
	.long	2995800136
	.long	1065353216
	.long	1052241912
	.long	3209728124
	.long	1064374250
	.long	3210092525
	.long	3213301415
	.long	844663917
	.long	0
	.long	3205563823
	.long	3002090928
	.long	1065353216
	.long	1050955490
	.long	3208613851
	.long	1063075792
	.long	3208448547
	.long	3213507302
	.long	3005148656
	.long	0
	.long	3205027257
	.long	845085070
	.long	1065353216
	.long	1049773965
	.long	3207589321
	.long	1061932376
	.long	3207048862
	.long	3213713190
	.long	856424709
	.long	0
	.long	3204504589
	.long	840216434
	.long	1065353216
	.long	1048689044
	.long	3206644275
	.long	1060914481
	.long	3205797512
	.long	3213919077
	.long	2995920909
	.long	0
	.long	3203541104
	.long	2991747572
	.long	1065353216
	.long	1046810746
	.long	3205769712
	.long	1060014844
	.long	3204711576
	.long	3214124965
	.long	862052078
	.long	0
	.long	3202544697
	.long	838089118
	.long	1065353216
	.long	1044984860
	.long	3204957722
	.long	1059214863
	.long	3203049502
	.long	3214330852
	.long	827121198
	.long	0
	.long	3201569741
	.long	834691410
	.long	1065353216
	.long	1043312844
	.long	3203954379
	.long	1058502663
	.long	3201336375
	.long	3214536739
	.long	3007857448
	.long	0
	.long	3200614413
	.long	840221633
	.long	1065353216
	.long	1041784404
	.long	3202540354
	.long	1057868403
	.long	3199781921
	.long	3214742627
	.long	851793817
	.long	0
	.long	3199677008
	.long	839026723
	.long	1065353216
	.long	1040390392
	.long	3201214072
	.long	1057307751
	.long	3198426707
	.long	3214948514
	.long	3001338494
	.long	0
	.long	3198755927
	.long	2964850736
	.long	1065353216
	.long	1038057984
	.long	3199965673
	.long	1056656040
	.long	3197207230
	.long	3215154402
	.long	859343286
	.long	0
	.long	3197849666
	.long	841628398
	.long	1065353216
	.long	1035760784
	.long	3198786293
	.long	1055777031
	.long	3196119466
	.long	3215360289
	.long	2984524460
	.long	0
	.long	3196956802
	.long	825401908
	.long	1065353216
	.long	1033689040
	.long	3197667936
	.long	1054999523
	.long	3194181676
	.long	3215566176
	.long	3010566241
	.long	0
	.long	3196075988
	.long	841457254
	.long	1065353216
	.long	1031831496
	.long	3196603348
	.long	1054310701
	.long	3192272619
	.long	3215772064
	.long	845503056
	.long	0
	.long	3194352231
	.long	2981409247
	.long	1065353216
	.long	1028557712
	.long	3195112138
	.long	1053716836
	.long	3190669665
	.long	3215977951
	.long	3004938871
	.long	0
	.long	3192631215
	.long	2938776032
	.long	1065353216
	.long	1025642520
	.long	3193159376
	.long	1053195814
	.long	3189074146
	.long	3216183839
	.long	856634493
	.long	0
	.long	3190926925
	.long	838017617
	.long	1065353216
	.long	1022797056
	.long	3191277530
	.long	1052746889
	.long	3187399111
	.long	3216389726
	.long	2995501340
	.long	0
	.long	3189237092
	.long	835035876
	.long	1065353216
	.long	1018446032
	.long	3189456128
	.long	1052374628
	.long	3184936896
	.long	3216595614
	.long	862261863
	.long	0
	.long	3187448002
	.long	816730716
	.long	1065353216
	.long	1014578464
	.long	3187685445
	.long	1052059423
	.long	3182192286
	.long	3216801501
	.long	830474973
	.long	0
	.long	3184113116
	.long	2974133638
	.long	1065353216
	.long	1008660256
	.long	3184241386
	.long	1051807326
	.long	3179645874
	.long	3217007388
	.long	3007647664
	.long	0
	.long	3180794318
	.long	2974804776
	.long	1065353216
	.long	1001540608
	.long	3180848186
	.long	1051617929
	.long	3175942112
	.long	3217213276
	.long	852213386
	.long	0
	.long	3175692604
	.long	2963513179
	.long	1065353216
	.long	991832832
	.long	3175724500
	.long	1051479824
	.long	3170735141
	.long	3217419163
	.long	3000918924
	.long	0
	.long	3167296049
	.long	2945355034
	.long	1065353216
	.long	975043072
	.long	3167303981
	.long	1051400329
	.long	3163053371
	.type	__svml_stan_ha_data_internal,@object
	.size	__svml_stan_ha_data_internal,6848
	.align 32
.L_2il0floatpacket.55:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.55,@object
	.size	.L_2il0floatpacket.55,32
	.align 32
.L_2il0floatpacket.56:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.56,@object
	.size	.L_2il0floatpacket.56,32
	.align 32
.L_2il0floatpacket.57:
	.long	0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.57,@object
	.size	.L_2il0floatpacket.57,32
	.align 32
.L_2il0floatpacket.58:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.58,@object
	.size	.L_2il0floatpacket.58,32
	.align 32
.L_2il0floatpacket.59:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.59,@object
	.size	.L_2il0floatpacket.59,32
	.align 32
.L_2il0floatpacket.60:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.60,@object
	.size	.L_2il0floatpacket.60,32
	.align 32
.L_2il0floatpacket.61:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.61,@object
	.size	.L_2il0floatpacket.61,32
	.align 32
.L_2il0floatpacket.62:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.62,@object
	.size	.L_2il0floatpacket.62,32
	.align 32
.L_2il0floatpacket.63:
	.long	0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.63,@object
	.size	.L_2il0floatpacket.63,32
	.align 32
.L_2il0floatpacket.78:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.78,@object
	.size	.L_2il0floatpacket.78,32
	.align 32
.L_2il0floatpacket.79:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.79,@object
	.size	.L_2il0floatpacket.79,32
	.align 32
.L_2il0floatpacket.80:
	.long	0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.80,@object
	.size	.L_2il0floatpacket.80,32
	.align 32
.L_2il0floatpacket.81:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.81,@object
	.size	.L_2il0floatpacket.81,32
	.align 32
.L_2il0floatpacket.82:
	.long	0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.82,@object
	.size	.L_2il0floatpacket.82,32
	.align 32
.L_2il0floatpacket.83:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.83,@object
	.size	.L_2il0floatpacket.83,32
	.align 32
.L_2il0floatpacket.84:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.84,@object
	.size	.L_2il0floatpacket.84,32
	.align 32
.L_2il0floatpacket.85:
	.long	0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.85,@object
	.size	.L_2il0floatpacket.85,32
	.align 32
.L_2il0floatpacket.86:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.86,@object
	.size	.L_2il0floatpacket.86,32
	.align 32
.L_2il0floatpacket.87:
	.long	0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.87,@object
	.size	.L_2il0floatpacket.87,32
	.align 32
.L_2il0floatpacket.88:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.88,@object
	.size	.L_2il0floatpacket.88,32
	.align 32
.L_2il0floatpacket.89:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.89,@object
	.size	.L_2il0floatpacket.89,32
	.align 32
.L_2il0floatpacket.90:
	.long	0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002,0x00000002
	.type	.L_2il0floatpacket.90,@object
	.size	.L_2il0floatpacket.90,32
	.align 32
.L_2il0floatpacket.91:
	.long	0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb
	.type	.L_2il0floatpacket.91,@object
	.size	.L_2il0floatpacket.91,32
	.align 32
.L_2il0floatpacket.92:
	.long	0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e
	.type	.L_2il0floatpacket.92,@object
	.size	.L_2il0floatpacket.92,32
	.align 32
.L_2il0floatpacket.93:
	.long	0x0000007c,0x0000007c,0x0000007c,0x0000007c,0x0000007c,0x0000007c,0x0000007c,0x0000007c
	.type	.L_2il0floatpacket.93,@object
	.size	.L_2il0floatpacket.93,32
	.align 16
.L_2il0floatpacket.26:
	.long	0xffffffff,0x00000000,0xffffffff,0x00000000
	.type	.L_2il0floatpacket.26,@object
	.size	.L_2il0floatpacket.26,16
	.align 16
.L_2il0floatpacket.27:
	.long	0x7f800000,0x7f800000,0x7f800000,0x7f800000
	.type	.L_2il0floatpacket.27,@object
	.size	.L_2il0floatpacket.27,16
	.align 16
.L_2il0floatpacket.28:
	.long	0x80000000,0x80000000,0x80000000,0x80000000
	.type	.L_2il0floatpacket.28,@object
	.size	.L_2il0floatpacket.28,16
	.align 16
.L_2il0floatpacket.29:
	.long	0x47400000,0x47400000,0x47400000,0x47400000
	.type	.L_2il0floatpacket.29,@object
	.size	.L_2il0floatpacket.29,16
	.align 16
.L_2il0floatpacket.30:
	.long	0x40c90fdb,0x40c90fdb,0x40c90fdb,0x40c90fdb
	.type	.L_2il0floatpacket.30,@object
	.size	.L_2il0floatpacket.30,16
	.align 16
.L_2il0floatpacket.31:
	.long	0x40c91000,0x40c91000,0x40c91000,0x40c91000
	.type	.L_2il0floatpacket.31,@object
	.size	.L_2il0floatpacket.31,16
	.align 16
.L_2il0floatpacket.32:
	.long	0xb795777a,0xb795777a,0xb795777a,0xb795777a
	.type	.L_2il0floatpacket.32,@object
	.size	.L_2il0floatpacket.32,16
	.align 16
.L_2il0floatpacket.33:
	.long	0xfffff000,0xfffff000,0xfffff000,0xfffff000
	.type	.L_2il0floatpacket.33,@object
	.size	.L_2il0floatpacket.33,16
	.align 16
.L_2il0floatpacket.34:
	.long	0x7fffffff,0x7fffffff,0x7fffffff,0x7fffffff
	.type	.L_2il0floatpacket.34,@object
	.size	.L_2il0floatpacket.34,16
	.align 16
.L_2il0floatpacket.35:
	.long	0x35800000,0x35800000,0x35800000,0x35800000
	.type	.L_2il0floatpacket.35,@object
	.size	.L_2il0floatpacket.35,16
	.align 16
.L_2il0floatpacket.64:
	.long	0x007fffff,0x007fffff,0x007fffff,0x007fffff
	.type	.L_2il0floatpacket.64,@object
	.size	.L_2il0floatpacket.64,16
	.align 16
.L_2il0floatpacket.65:
	.long	0x00800000,0x00800000,0x00800000,0x00800000
	.type	.L_2il0floatpacket.65,@object
	.size	.L_2il0floatpacket.65,16
	.align 16
.L_2il0floatpacket.66:
	.long	0x0000ffff,0x0000ffff,0x0000ffff,0x0000ffff
	.type	.L_2il0floatpacket.66,@object
	.size	.L_2il0floatpacket.66,16
	.align 16
.L_2il0floatpacket.67:
	.long	0x3f800000,0x3f800000,0x3f800000,0x3f800000
	.type	.L_2il0floatpacket.67,@object
	.size	.L_2il0floatpacket.67,16
	.align 16
.L_2il0floatpacket.68:
	.long	0x000000ff,0x000000ff,0x000000ff,0x000000ff
	.type	.L_2il0floatpacket.68,@object
	.size	.L_2il0floatpacket.68,16
	.align 16
.L_2il0floatpacket.69:
	.long	0x28800000,0x28800000,0x28800000,0x28800000
	.type	.L_2il0floatpacket.69,@object
	.size	.L_2il0floatpacket.69,16
	.align 16
.L_2il0floatpacket.70:
	.long	0x0003ffff,0x0003ffff,0x0003ffff,0x0003ffff
	.type	.L_2il0floatpacket.70,@object
	.size	.L_2il0floatpacket.70,16
	.align 16
.L_2il0floatpacket.71:
	.long	0x34000000,0x34000000,0x34000000,0x34000000
	.type	.L_2il0floatpacket.71,@object
	.size	.L_2il0floatpacket.71,16
	.align 16
.L_2il0floatpacket.72:
	.long	0x000001ff,0x000001ff,0x000001ff,0x000001ff
	.type	.L_2il0floatpacket.72,@object
	.size	.L_2il0floatpacket.72,16
	.align 16
.L_2il0floatpacket.73:
	.long	0xb43bbd2e,0xb43bbd2e,0xb43bbd2e,0xb43bbd2e
	.type	.L_2il0floatpacket.73,@object
	.size	.L_2il0floatpacket.73,16
	.align 16
.L_2il0floatpacket.74:
	.long	0x00000002,0x00000002,0x00000002,0x00000002
	.type	.L_2il0floatpacket.74,@object
	.size	.L_2il0floatpacket.74,16
	.align 16
.L_2il0floatpacket.75:
	.long	0x3cc90fdb,0x3cc90fdb,0x3cc90fdb,0x3cc90fdb
	.type	.L_2il0floatpacket.75,@object
	.size	.L_2il0floatpacket.75,16
	.align 16
.L_2il0floatpacket.76:
	.long	0xb03bbd2e,0xb03bbd2e,0xb03bbd2e,0xb03bbd2e
	.type	.L_2il0floatpacket.76,@object
	.size	.L_2il0floatpacket.76,16
	.align 16
.L_2il0floatpacket.77:
	.long	0x0000007c,0x0000007c,0x0000007c,0x0000007c
	.type	.L_2il0floatpacket.77,@object
	.size	.L_2il0floatpacket.77,16
	.align 4
_vmlsTanHATab:
	.long	0
	.long	2139095040
	.type	_vmlsTanHATab,@object
	.size	_vmlsTanHATab,8
	.data
	.section .note.GNU-stack, ""
// -- Begin DWARF2 SEGMENT .eh_frame
	.section .eh_frame,"a",@progbits
.eh_frame_seg:
	.align 1
#endif
# End
