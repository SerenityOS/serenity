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

#ifndef NSK_TOOLS_DEFINED
#define NSK_TOOLS_DEFINED

/*************************************************************/

#include <stdarg.h>
#include <inttypes.h>

/*************************************************************/

#if defined(_LP64) && defined(__APPLE__)
#define JLONG_FORMAT "%ld"
#else  // _LP64 && __APPLE__
#define JLONG_FORMAT "%" PRId64
#endif // _LP64 && __APPLE__


/*************************************************************/

/**
 * Use examples:
 *
 *     NSK_DISPLAY("Test started.\n");
 *     NSK_COMPLAIN("Test FAILED: %s\n",reason);
 *
 *
 */

#define NSK_DISPLAY0(format)  nsk_ldisplay(__FILE__,__LINE__,format)
#define NSK_DISPLAY1(format,a)  nsk_ldisplay(__FILE__,__LINE__,format,a)
#define NSK_DISPLAY2(format,a,b)  nsk_ldisplay(__FILE__,__LINE__,format,a,b)
#define NSK_DISPLAY3(format,a,b,c)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c)
#define NSK_DISPLAY4(format,a,b,c,d)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d)
#define NSK_DISPLAY5(format,a,b,c,d,e)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d,e)
#define NSK_DISPLAY6(format,a,b,c,d,e,f)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d,e,f)
#define NSK_DISPLAY7(format,a,b,c,d,e,f,g)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d,e,f,g)
#define NSK_DISPLAY8(format,a,b,c,d,e,f,g,h)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d,e,f,g,h)
#define NSK_DISPLAY9(format,a,b,c,d,e,f,g,h,i)  nsk_ldisplay(__FILE__,__LINE__,format,a,b,c,d,e,f,g,h,i)

#define NSK_COMPLAIN0(format)  nsk_lcomplain(__FILE__,__LINE__,format)
#define NSK_COMPLAIN1(format,a)  nsk_lcomplain(__FILE__,__LINE__,format,a)
#define NSK_COMPLAIN2(format,a,b)  nsk_lcomplain(__FILE__,__LINE__,format,a,b)
#define NSK_COMPLAIN3(format,a,b,c)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c)
#define NSK_COMPLAIN4(format,a,b,c,d)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d)
#define NSK_COMPLAIN5(format,a,b,c,d,e)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d,e)
#define NSK_COMPLAIN6(format,a,b,c,d,e,f)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d,e,f)
#define NSK_COMPLAIN7(format,a,b,c,d,e,f,g)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d,e,f,g)
#define NSK_COMPLAIN8(format,a,b,c,d,e,f,g,h)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d,e,f,g,h)
#define NSK_COMPLAIN9(format,a,b,c,d,e,f,g,h,i)  nsk_lcomplain(__FILE__,__LINE__,format,a,b,c,d,e,f,g,h,i)

#define NSK_VERIFY(action)  (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
                                nsk_lverify(!!(action),__FILE__,__LINE__,"%s\n",#action))
#define NSK_TRACE(action)   {nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action); \
                            (void)(action); \
                            nsk_ltrace(NSK_TRACE_AFTER,__FILE__,__LINE__,"%s\n",#action);}
#define NSK_BEFORE_TRACE(action) nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action); \
                                 (void)(action)

/*************************************************************/

extern "C" {

#define NSK_TRUE  1
#define NSK_FALSE 0

#define NSK_TRACE_NONE    0
#define NSK_TRACE_BEFORE  1
#define NSK_TRACE_AFTER   2
#define NSK_TRACE_ALL     (NSK_TRACE_BEFORE | NSK_TRACE_AFTER)

#define NSK_MAX_COMPLAINS_NON_VERBOSE 665

/**
 * Mode is verbose iff "verbose" isn't NSK_FALSE.
 */
void nsk_setVerboseMode(int verbose);
int  nsk_getVerboseMode();

/**
 * Trace mode can be any combination of NSK_TRACE_* flags.
 */
void nsk_setTraceMode(int mode);
int  nsk_getTraceMode();

/**
 * Display the message if current mode is verbose.
 */
void nsk_display(const char format[], ...);
void nsk_vdisplay(const char format[], va_list args);
/**
 * Add a prompt to point the file[] and line location.
 */
void nsk_ldisplay(const char file[], int line, const char format[], ...);
void nsk_lvdisplay(const char file[], int line, const char format[], va_list args);

/**
 * Complain the error message; add an "ERROR" prompt.
 * No matter, is current mode verbose or not.
 */
void nsk_complain(const char format[], ...);
void nsk_vcomplain(const char format[], va_list args);
/**
 * Add a prompt to point the file[] and line location.
 */
void nsk_lcomplain(const char file[], int line, const char format[], ...);
void nsk_lvcomplain(const char file[], int line, const char format[], va_list args);

/**
 * Trace executable actions,
 */
void nsk_ltrace(int mode, const char file[], int line, const char format[], ...);
void nsk_lvtrace(int mode, const char file[], int line, const char format[], va_list args);

/**
 * Complain the message as an error if value==0; return !!value.
 * Add a prompt to point the file[] and line location.
 * Display anyway if verbose.
 */
int nsk_lverify(int value, const char file[], int line, const char format[], ...);

/**
 * Same as printf() or vprintf(); but we may redefine this later.
 */
int nsk_vprintf(const char format[], va_list args);
int nsk_printf(const char format[], ...);

/**
 * Print given bytes array as hex numbers in multiple strings, each
 * started with 'indent' prefix and offset info, followed by 'columns' bytes
 * as hex numbers, then followed by the same bytes as ASCII chars where
 * non-printable chars are replaced by '.', and terminated with new line char.
 * Typically columns number is 16 and should not be greater than 255.
 */
void nsk_printHexBytes(const char indent[], int columns,
                                    size_t size, const unsigned char bytes[]);

/*************************************************************/

/**
 * Returns str or "<NULL>" if str is NULL; useful for printing strings.
 */
const char* nsk_null_string(const char* str);

/*************************************************************/

}

/*************************************************************/

#endif
