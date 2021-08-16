/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef NSK_JNI_TOOLS_DEFINED
#define NSK_JNI_TOOLS_DEFINED

/*************************************************************/

#include "jni.h"

/*************************************************************/

#include "nsk_tools.h"

/*************************************************************/

/* printf format specifier for jlong */
#ifdef _WIN32

#define LL "I64"
#include <STDDEF.H>

#else // !_WIN32

#include <stdint.h>

#ifdef _LP64
#define LL "l"
#else
#define LL "ll"
#endif

#endif // !_WIN32

/**
 * Additional Java basic types
 */

#ifdef _WIN32
    typedef unsigned __int64 julong;
#else
    typedef unsigned long long julong;
#endif

/*************************************************************/

/**
 * Execute action with JNI call, check result for true and
 * pending exception and complain error if required.
 * Also trace action execution if tracing mode enabled.
 */
#define NSK_JNI_VERIFY(jni, action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        nsk_jni_lverify(NSK_TRUE,jni,action,__FILE__,__LINE__,"%s\n",#action))

/**
 * Execute action with JNI call, check result for false and
 * pending exception and complain error if required.
 * Also trace action execution if tracing mode enabled.
 */
#define NSK_JNI_VERIFY_NEGATIVE(jni,action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        nsk_jni_lverify(NSK_FALSE,jni,action,__FILE__,__LINE__,"%s\n",#action))

/**
 * Execute action with JNI call, check result for
 * pending exception and complain error if required.
 * Also trace action execution if tracing mode enabled.
 */
#define NSK_JNI_VERIFY_VOID(jni,action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        action, \
        nsk_jni_lverify_void(jni, __FILE__,__LINE__,"%s\n",#action))

/*************************************************************/

extern "C" {

/*************************************************************/

/**
 * If positive, assert status is true; or
 * if !positive, assert status is not true.
 * Assert means: complain if the assertion is false.
 * Return the assertion value, either NSK_TRUE or NSK_FALSE.
 * Anyway, trace if "nsk_tools" mode is verbose and
 * print information about pending exceptions if status is false.
 */
int nsk_jni_lverify(int positive, JNIEnv* jni, int status,
                        const char file[], int line, const char format[], ...);

/**
 * If positive, assert status is true; or
 * if !positive, assert status is not true.
 * Assert means: complain if the assertion is false.
 * Return the assertion value, either NSK_TRUE or NSK_FALSE.
 * Anyway, trace if "nsk_tools" mode is verbose and
 * print information about pending exceptions if status is false.
 */
int nsk_jni_lverify_void(JNIEnv* jni, const char file[], int line,
                            const char format[], ...);

/**
 * Checks if pending exception exists and then prints error message
 * with exception description, clears pending exception amd return 1.
 * Otherwise, does noting and returns 0,
 */
int nsk_jni_check_exception(JNIEnv* jni, const char file[], int line);

/**
 * Convert the digits of the given value argument to a null-terminated
 * character string and store the result (up to 32 bytes) in string.
 * If value is negative, the first character of the stored string is
 * the minus sign (-). The function returns a pointer to the begining
 * of the result string.
 */
char *jlong_to_string(jlong value, char *string);

/**
 * Convert the digits of the given value argument to a null-terminated
 * character string and store the result (up to 32 bytes) in string.
 * The function returns a pointer to the begining of the result string.
 */
char *julong_to_string(julong value, char *string);

/**
 * Sleep for given number of milliseconds.
 */
void mssleep(long millis);

/**
 * Create JavaVMOption array of size 'size' and fills
 * first argsCnt elements from args[].
 * Callee is responsible to free JavaVMOption*.
 * No other memory deallocations are required.
 */
JavaVMOption* jni_create_vmoptions(int size, char *args[], int argsCnt);

/**
 * Print JavaVMInitArgs values to stdout.
 */
void jni_print_vmargs(JavaVMInitArgs vmargs);

/*************************************************************/

}

/*************************************************************/

#endif
