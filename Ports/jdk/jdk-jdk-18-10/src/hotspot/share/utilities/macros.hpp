/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_MACROS_HPP
#define SHARE_UTILITIES_MACROS_HPP

// Use this to mark code that needs to be cleaned up (for development only)
#define NEEDS_CLEANUP

// Makes a string of the argument (which is not macro-expanded)
#define STR(a)  #a

// Makes a string of the macro expansion of a
#define XSTR(a) STR(a)

// Allow commas in macro arguments.
#define COMMA ,

// Apply pre-processor token pasting to the expansions of x and y.
// The token pasting operator (##) prevents its arguments from being
// expanded.  This macro allows expansion of its arguments before the
// concatenation is performed.  Note: One auxilliary level ought to be
// sufficient, but two are used because of bugs in some preprocesors.
#define PASTE_TOKENS(x, y) PASTE_TOKENS_AUX(x, y)
#define PASTE_TOKENS_AUX(x, y) PASTE_TOKENS_AUX2(x, y)
#define PASTE_TOKENS_AUX2(x, y) x ## y

// -DINCLUDE_<something>=0 | 1 can be specified on the command line to include
// or exclude functionality.

#ifndef INCLUDE_JVMTI
#define INCLUDE_JVMTI 1
#endif  // INCLUDE_JVMTI

#if INCLUDE_JVMTI
#define JVMTI_ONLY(x) x
#define NOT_JVMTI(x)
#define NOT_JVMTI_RETURN
#define NOT_JVMTI_RETURN_(code) /* next token must be ; */
#else
#define JVMTI_ONLY(x)
#define NOT_JVMTI(x) x
#define NOT_JVMTI_RETURN { return; }
#define NOT_JVMTI_RETURN_(code) { return code; }
#endif // INCLUDE_JVMTI

#ifndef INCLUDE_VM_STRUCTS
#define INCLUDE_VM_STRUCTS 1
#endif

#if INCLUDE_VM_STRUCTS
#define NOT_VM_STRUCTS_RETURN        /* next token must be ; */
#define NOT_VM_STRUCTS_RETURN_(code) /* next token must be ; */
#else
#define NOT_VM_STRUCTS_RETURN           {}
#define NOT_VM_STRUCTS_RETURN_(code) { return code; }
#endif // INCLUDE_VM_STRUCTS

#ifndef INCLUDE_JNI_CHECK
#define INCLUDE_JNI_CHECK 1
#endif

#if INCLUDE_JNI_CHECK
#define NOT_JNI_CHECK_RETURN        /* next token must be ; */
#define NOT_JNI_CHECK_RETURN_(code) /* next token must be ; */
#else
#define NOT_JNI_CHECK_RETURN            {}
#define NOT_JNI_CHECK_RETURN_(code) { return code; }
#endif // INCLUDE_JNI_CHECK

#ifndef INCLUDE_SERVICES
#define INCLUDE_SERVICES 1
#endif

#if INCLUDE_SERVICES
#define NOT_SERVICES_RETURN        /* next token must be ; */
#define NOT_SERVICES_RETURN_(code) /* next token must be ; */
#else
#define NOT_SERVICES_RETURN             {}
#define NOT_SERVICES_RETURN_(code) { return code; }
#endif // INCLUDE_SERVICES

#ifndef INCLUDE_CDS
#define INCLUDE_CDS 1
#endif

#if INCLUDE_CDS
#define CDS_ONLY(x) x
#define NOT_CDS(x)
#define NOT_CDS_RETURN        /* next token must be ; */
#define NOT_CDS_RETURN0       /* next token must be ; */
#define NOT_CDS_RETURN_(code) /* next token must be ; */
#else
#define CDS_ONLY(x)
#define NOT_CDS(x) x
#define NOT_CDS_RETURN        {}
#define NOT_CDS_RETURN0       { return 0; }
#define NOT_CDS_RETURN_(code) { return code; }
#endif // INCLUDE_CDS

#ifndef INCLUDE_MANAGEMENT
#define INCLUDE_MANAGEMENT 1
#endif // INCLUDE_MANAGEMENT

#if INCLUDE_MANAGEMENT
#define NOT_MANAGEMENT_RETURN        /* next token must be ; */
#define NOT_MANAGEMENT_RETURN_(code) /* next token must be ; */
#else
#define NOT_MANAGEMENT_RETURN        {}
#define NOT_MANAGEMENT_RETURN_(code) { return code; }
#endif // INCLUDE_MANAGEMENT

#ifndef INCLUDE_EPSILONGC
#define INCLUDE_EPSILONGC 1
#endif // INCLUDE_EPSILONGC

#if INCLUDE_EPSILONGC
#define EPSILONGC_ONLY(x) x
#define EPSILONGC_ONLY_ARG(arg) arg,
#define NOT_EPSILONGC(x)
#define NOT_EPSILONGC_RETURN        /* next token must be ; */
#define NOT_EPSILONGC_RETURN_(code) /* next token must be ; */
#else
#define EPSILONGC_ONLY(x)
#define EPSILONGC_ONLY_ARG(arg)
#define NOT_EPSILONGC(x) x
#define NOT_EPSILONGC_RETURN        {}
#define NOT_EPSILONGC_RETURN_(code) { return code; }
#endif // INCLUDE_EPSILONGC

#ifndef INCLUDE_G1GC
#define INCLUDE_G1GC 1
#endif // INCLUDE_G1GC

#if INCLUDE_G1GC
#define G1GC_ONLY(x) x
#define G1GC_ONLY_ARG(arg) arg,
#define NOT_G1GC(x)
#define NOT_G1GC_RETURN        /* next token must be ; */
#define NOT_G1GC_RETURN_(code) /* next token must be ; */
#else
#define G1GC_ONLY(x)
#define G1GC_ONLY_ARG(arg)
#define NOT_G1GC(x) x
#define NOT_G1GC_RETURN        {}
#define NOT_G1GC_RETURN_(code) { return code; }
#endif // INCLUDE_G1GC

#ifndef INCLUDE_PARALLELGC
#define INCLUDE_PARALLELGC 1
#endif // INCLUDE_PARALLELGC

#if INCLUDE_PARALLELGC
#define PARALLELGC_ONLY(x) x
#define PARALLELGC_ONLY_ARG(arg) arg,
#define NOT_PARALLELGC(x)
#define NOT_PARALLELGC_RETURN        /* next token must be ; */
#define NOT_PARALLELGC_RETURN_(code) /* next token must be ; */
#else
#define PARALLELGC_ONLY(x)
#define PARALLELGC_ONLY_ARG(arg)
#define NOT_PARALLELGC(x) x
#define NOT_PARALLELGC_RETURN        {}
#define NOT_PARALLELGC_RETURN_(code) { return code; }
#endif // INCLUDE_PARALLELGC

#ifndef INCLUDE_SERIALGC
#define INCLUDE_SERIALGC 1
#endif // INCLUDE_SERIALGC

#if INCLUDE_SERIALGC
#define SERIALGC_ONLY(x) x
#define SERIALGC_ONLY_ARG(arg) arg,
#define NOT_SERIALGC(x)
#define NOT_SERIALGC_RETURN        /* next token must be ; */
#define NOT_SERIALGC_RETURN_(code) /* next token must be ; */
#else
#define SERIALGC_ONLY(x)
#define SERIALGC_ONLY_ARG(arg)
#define NOT_SERIALGC(x) x
#define NOT_SERIALGC_RETURN        {}
#define NOT_SERIALGC_RETURN_(code) { return code; }
#endif // INCLUDE_SERIALGC

#ifndef INCLUDE_SHENANDOAHGC
#define INCLUDE_SHENANDOAHGC 1
#endif // INCLUDE_SHENANDOAHGC

#if INCLUDE_SHENANDOAHGC
#define SHENANDOAHGC_ONLY(x) x
#define SHENANDOAHGC_ONLY_ARG(arg) arg,
#define NOT_SHENANDOAHGC(x)
#define NOT_SHENANDOAHGC_RETURN        /* next token must be ; */
#define NOT_SHENANDOAHGC_RETURN_(code) /* next token must be ; */
#else
#define SHENANDOAHGC_ONLY(x)
#define SHENANDOAHGC_ONLY_ARG(arg)
#define NOT_SHENANDOAHGC(x) x
#define NOT_SHENANDOAHGC_RETURN        {}
#define NOT_SHENANDOAHGC_RETURN_(code) { return code; }
#endif // INCLUDE_SHENANDOAHGC

#ifndef INCLUDE_ZGC
#define INCLUDE_ZGC 1
#endif // INCLUDE_ZGC

#if INCLUDE_ZGC
#define ZGC_ONLY(x) x
#define ZGC_ONLY_ARG(arg) arg,
#define NOT_ZGC(x)
#define NOT_ZGC_RETURN        /* next token must be ; */
#define NOT_ZGC_RETURN_(code) /* next token must be ; */
#else
#define ZGC_ONLY(x)
#define ZGC_ONLY_ARG(arg)
#define NOT_ZGC(x) x
#define NOT_ZGC_RETURN        {}
#define NOT_ZGC_RETURN_(code) { return code; }
#endif // INCLUDE_ZGC

#ifndef INCLUDE_NMT
#define INCLUDE_NMT 1
#endif // INCLUDE_NMT

#if INCLUDE_NMT
#define NOT_NMT_RETURN        /* next token must be ; */
#define NOT_NMT_RETURN_(code) /* next token must be ; */
#define NMT_ONLY(x) x
#define NOT_NMT(x)
#else
#define NOT_NMT_RETURN        {}
#define NOT_NMT_RETURN_(code) { return code; }
#define NMT_ONLY(x)
#define NOT_NMT(x) x
#endif // INCLUDE_NMT

#ifndef INCLUDE_JFR
#define INCLUDE_JFR 1
#endif

#if INCLUDE_JFR
#define JFR_ONLY(code) code
#define NOT_JFR_RETURN()      /* next token must be ; */
#define NOT_JFR_RETURN_(code) /* next token must be ; */
#else
#define JFR_ONLY(code)
#define NOT_JFR_RETURN()      {}
#define NOT_JFR_RETURN_(code) { return code; }
#endif

#ifndef INCLUDE_JVMCI
#define INCLUDE_JVMCI 1
#endif

#if INCLUDE_JVMCI
#define JVMCI_ONLY(code) code
#define NOT_JVMCI(code)
#define NOT_JVMCI_RETURN /* next token must be ; */
#else
#define JVMCI_ONLY(code)
#define NOT_JVMCI(code) code
#define NOT_JVMCI_RETURN {}
#endif // INCLUDE_JVMCI

// COMPILER1 variant
#ifdef COMPILER1
#define COMPILER1_PRESENT(code) code
#define NOT_COMPILER1(code)
#else // COMPILER1
#define COMPILER1_PRESENT(code)
#define NOT_COMPILER1(code) code
#endif // COMPILER1

// COMPILER2 variant
#ifdef COMPILER2
#define COMPILER2_PRESENT(code) code
#define NOT_COMPILER2(code)
#else // COMPILER2
#define COMPILER2_PRESENT(code)
#define NOT_COMPILER2(code) code
#endif // COMPILER2

// COMPILER2 or JVMCI
#if defined(COMPILER2) || INCLUDE_JVMCI
#define COMPILER2_OR_JVMCI 1
#define COMPILER2_OR_JVMCI_PRESENT(code) code
#define NOT_COMPILER2_OR_JVMCI(code)
#define NOT_COMPILER2_OR_JVMCI_RETURN        /* next token must be ; */
#define NOT_COMPILER2_OR_JVMCI_RETURN_(code) /* next token must be ; */
#else
#define COMPILER2_OR_JVMCI 0
#define COMPILER2_OR_JVMCI_PRESENT(code)
#define NOT_COMPILER2_OR_JVMCI(code) code
#define NOT_COMPILER2_OR_JVMCI_RETURN {}
#define NOT_COMPILER2_OR_JVMCI_RETURN_(code) { return code; }
#endif

// COMPILER1 and COMPILER2
#if defined(COMPILER1) && defined(COMPILER2)
#define COMPILER1_AND_COMPILER2 1
#define COMPILER1_AND_COMPILER2_PRESENT(code) code
#else
#define COMPILER1_AND_COMPILER2 0
#define COMPILER1_AND_COMPILER2_PRESENT(code)
#endif

// COMPILER1 or COMPILER2
#if defined(COMPILER1) || defined(COMPILER2)
#define COMPILER1_OR_COMPILER2 1
#define COMPILER1_OR_COMPILER2_PRESENT(code) code
#else
#define COMPILER1_OR_COMPILER2 0
#define COMPILER1_OR_COMPILER2_PRESENT(code)
#endif


// PRODUCT variant
#ifdef PRODUCT
#define PRODUCT_ONLY(code) code
#define NOT_PRODUCT(code)
#define NOT_PRODUCT_ARG(arg)
#define PRODUCT_RETURN  {}
#define PRODUCT_RETURN0 { return 0; }
#define PRODUCT_RETURN_(code) { code }
#else // PRODUCT
#define PRODUCT_ONLY(code)
#define NOT_PRODUCT(code) code
#define NOT_PRODUCT_ARG(arg) arg,
#define PRODUCT_RETURN  /*next token must be ;*/
#define PRODUCT_RETURN0 /*next token must be ;*/
#define PRODUCT_RETURN_(code)  /*next token must be ;*/
#endif // PRODUCT

#ifdef CHECK_UNHANDLED_OOPS
#define CHECK_UNHANDLED_OOPS_ONLY(code) code
#define NOT_CHECK_UNHANDLED_OOPS(code)
#else
#define CHECK_UNHANDLED_OOPS_ONLY(code)
#define NOT_CHECK_UNHANDLED_OOPS(code)  code
#endif // CHECK_UNHANDLED_OOPS

#ifdef ASSERT
#define DEBUG_ONLY(code) code
#define NOT_DEBUG(code)
#define NOT_DEBUG_RETURN  /*next token must be ;*/
// Historical.
#define debug_only(code) code
#else // ASSERT
#define DEBUG_ONLY(code)
#define NOT_DEBUG(code) code
#define NOT_DEBUG_RETURN {}
#define debug_only(code)
#endif // ASSERT

#ifdef  _LP64
#define LP64_ONLY(code) code
#define NOT_LP64(code)
#else  // !_LP64
#define LP64_ONLY(code)
#define NOT_LP64(code) code
#endif // _LP64

#ifdef LINUX
#define LINUX_ONLY(code) code
#define NOT_LINUX(code)
#else
#define LINUX_ONLY(code)
#define NOT_LINUX(code) code
#endif

#ifdef __APPLE__
#define MACOS_ONLY(code) code
#define NOT_MACOS(code)
#else
#define MACOS_ONLY(code)
#define NOT_MACOS(code) code
#endif

#ifdef AIX
#define AIX_ONLY(code) code
#define NOT_AIX(code)
#else
#define AIX_ONLY(code)
#define NOT_AIX(code) code
#endif

#ifdef _WINDOWS
#define WINDOWS_ONLY(code) code
#define NOT_WINDOWS(code)
#else
#define WINDOWS_ONLY(code)
#define NOT_WINDOWS(code) code
#endif

#if defined(__FreeBSD__) || defined(__NetBSD__) || defined(__OpenBSD__) || defined(__APPLE__)
#ifndef BSD
#define BSD
#endif // BSD defined in <sys/param.h>
#define BSD_ONLY(code) code
#define NOT_BSD(code)
#else
#define BSD_ONLY(code)
#define NOT_BSD(code) code
#endif

#ifdef _WIN64
#define WIN64_ONLY(code) code
#define NOT_WIN64(code)
#else
#define WIN64_ONLY(code)
#define NOT_WIN64(code) code
#endif

#if defined(ZERO)
#define ZERO_ONLY(code) code
#define NOT_ZERO(code)
#define NOT_ZERO_RETURN {}
#else
#define ZERO_ONLY(code)
#define NOT_ZERO(code) code
#define NOT_ZERO_RETURN
#endif

#if defined(IA32) || defined(AMD64)
#define X86
#define X86_ONLY(code) code
#define NOT_X86(code)
#else
#undef X86
#define X86_ONLY(code)
#define NOT_X86(code) code
#endif

#ifdef IA32
#define IA32_ONLY(code) code
#define NOT_IA32(code)
#else
#define IA32_ONLY(code)
#define NOT_IA32(code) code
#endif

// This is a REALLY BIG HACK, but on AIX <sys/systemcfg.h> unconditionally defines IA64.
// At least on AIX 7.1 this is a real problem because 'systemcfg.h' is indirectly included
// by 'pthread.h' and other common system headers.

#if defined(IA64) && !defined(AIX)
#define IA64_ONLY(code) code
#define NOT_IA64(code)
#else
#define IA64_ONLY(code)
#define NOT_IA64(code) code
#endif

#ifdef AMD64
#define AMD64_ONLY(code) code
#define NOT_AMD64(code)
#else
#define AMD64_ONLY(code)
#define NOT_AMD64(code) code
#endif

#ifdef S390
#define S390_ONLY(code) code
#define NOT_S390(code)
#else
#define S390_ONLY(code)
#define NOT_S390(code) code
#endif

#if defined(PPC32) || defined(PPC64)
#ifndef PPC
#define PPC
#endif
#define PPC_ONLY(code) code
#define NOT_PPC(code)
#else
#undef PPC
#define PPC_ONLY(code)
#define NOT_PPC(code) code
#endif

#ifdef PPC32
#define PPC32_ONLY(code) code
#define NOT_PPC32(code)
#else
#define PPC32_ONLY(code)
#define NOT_PPC32(code) code
#endif

#ifdef PPC64
#define PPC64_ONLY(code) code
#define NOT_PPC64(code)
#else
#define PPC64_ONLY(code)
#define NOT_PPC64(code) code
#endif

#ifdef E500V2
#define E500V2_ONLY(code) code
#define NOT_E500V2(code)
#else
#define E500V2_ONLY(code)
#define NOT_E500V2(code) code
#endif

// Note: There are two ARM ports. They set the following in the makefiles:
// 1. 32-bit port:   -DARM -DARM32 -DTARGET_ARCH_arm
// 2. 64-bit port:   -DAARCH64 -D_LP64 -DTARGET_ARCH_aaarch64
#ifdef ARM
#define ARM_ONLY(code) code
#define NOT_ARM(code)
#else
#define ARM_ONLY(code)
#define NOT_ARM(code) code
#endif

#ifdef ARM32
#define ARM32_ONLY(code) code
#define NOT_ARM32(code)
#else
#define ARM32_ONLY(code)
#define NOT_ARM32(code) code
#endif

#ifdef AARCH64
#define AARCH64_ONLY(code) code
#define NOT_AARCH64(code)
#else
#define AARCH64_ONLY(code)
#define NOT_AARCH64(code) code
#endif

#define MACOS_AARCH64_ONLY(x) MACOS_ONLY(AARCH64_ONLY(x))

#ifdef VM_LITTLE_ENDIAN
#define LITTLE_ENDIAN_ONLY(code) code
#define BIG_ENDIAN_ONLY(code)
#else
#define LITTLE_ENDIAN_ONLY(code)
#define BIG_ENDIAN_ONLY(code) code
#endif

#define define_pd_global(type, name, value) const type pd_##name = value;

// Helper macros for constructing file names for includes.
#define CPU_HEADER_STEM(basename) PASTE_TOKENS(basename, INCLUDE_SUFFIX_CPU)
#define OS_HEADER_STEM(basename) PASTE_TOKENS(basename, INCLUDE_SUFFIX_OS)
#define OS_CPU_HEADER_STEM(basename) PASTE_TOKENS(basename, PASTE_TOKENS(INCLUDE_SUFFIX_OS, INCLUDE_SUFFIX_CPU))
#define COMPILER_HEADER_STEM(basename) PASTE_TOKENS(basename, INCLUDE_SUFFIX_COMPILER)

// Include platform dependent files.
//
// This macro constructs from basename and INCLUDE_SUFFIX_OS /
// INCLUDE_SUFFIX_CPU / INCLUDE_SUFFIX_COMPILER, which are set on
// the command line, the name of platform dependent files to be included.
// Example: INCLUDE_SUFFIX_OS=_linux / INCLUDE_SUFFIX_CPU=_x86
//   CPU_HEADER_INLINE(macroAssembler) --> macroAssembler_x86.inline.hpp
//   OS_CPU_HEADER(vmStructs)          --> vmStructs_linux_x86.hpp
//
// basename<cpu>.hpp / basename<cpu>.inline.hpp
#define CPU_HEADER_H(basename)         XSTR(CPU_HEADER_STEM(basename).h)
#define CPU_HEADER(basename)           XSTR(CPU_HEADER_STEM(basename).hpp)
#define CPU_HEADER_INLINE(basename)    XSTR(CPU_HEADER_STEM(basename).inline.hpp)
// basename<os>.hpp / basename<os>.inline.hpp
#define OS_HEADER_H(basename)          XSTR(OS_HEADER_STEM(basename).h)
#define OS_HEADER(basename)            XSTR(OS_HEADER_STEM(basename).hpp)
#define OS_HEADER_INLINE(basename)     XSTR(OS_HEADER_STEM(basename).inline.hpp)
// basename<os><cpu>.hpp / basename<os><cpu>.inline.hpp
#define OS_CPU_HEADER(basename)        XSTR(OS_CPU_HEADER_STEM(basename).hpp)
#define OS_CPU_HEADER_INLINE(basename) XSTR(OS_CPU_HEADER_STEM(basename).inline.hpp)
// basename<compiler>.hpp / basename<compiler>.inline.hpp
#define COMPILER_HEADER(basename)        XSTR(COMPILER_HEADER_STEM(basename).hpp)
#define COMPILER_HEADER_INLINE(basename) XSTR(COMPILER_HEADER_STEM(basename).inline.hpp)

#if INCLUDE_CDS && INCLUDE_G1GC && defined(_LP64) && !defined(_WINDOWS)
#define INCLUDE_CDS_JAVA_HEAP 1
#define CDS_JAVA_HEAP_ONLY(x) x
#define NOT_CDS_JAVA_HEAP(x)
#define NOT_CDS_JAVA_HEAP_RETURN
#define NOT_CDS_JAVA_HEAP_RETURN_(code)
#else
#define INCLUDE_CDS_JAVA_HEAP 0
#define CDS_JAVA_HEAP_ONLY(x)
#define NOT_CDS_JAVA_HEAP(x) x
#define NOT_CDS_JAVA_HEAP_RETURN        {}
#define NOT_CDS_JAVA_HEAP_RETURN_(code) { return code; }
#endif

#endif // SHARE_UTILITIES_MACROS_HPP
