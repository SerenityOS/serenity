/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#if !defined(_DEBUG_TRACE_H)
#define _DEBUG_TRACE_H

#if defined(__cplusplus)
extern "C" {
#endif

#if defined(DEBUG)

#include "debug_util.h"

typedef int     dtrace_id;
enum {
    UNDEFINED_TRACE_ID = -1 /* indicates trace point has not been registered yet */
};

/* prototype for client provided output callback function */
typedef void (JNICALL *DTRACE_OUTPUT_CALLBACK)(const char * msg);

/* prototype for client provided print callback function */
typedef void (JNICALL *DTRACE_PRINT_CALLBACK)(const char * file, int line, int argc, const char * fmt, va_list arglist);

extern void DTrace_EnableAll(dbool_t enabled);
extern void DTrace_EnableFile(const char * file, dbool_t enabled);
extern void DTrace_EnableLine(const char * file, int linenum, dbool_t enabled);
extern void DTrace_SetOutputCallback(DTRACE_OUTPUT_CALLBACK pfn);
extern void DTrace_Initialize();
extern void DTrace_Shutdown();
void DTrace_DisableMutex();
extern void DTrace_VPrintImpl(const char * fmt, va_list arglist);
extern void DTrace_PrintImpl(const char * fmt, ...);
/* JNIEXPORT because these functions are also called from libawt_xawt */
JNIEXPORT void JNICALL DTrace_PrintFunction(DTRACE_PRINT_CALLBACK pfn, dtrace_id * pFileTraceId, dtrace_id * pTraceId, const char * file, int line, int argc, const char * fmt, ...);

/* these functions are exported only for use in macros-- do not call them directly!!! */
JNIEXPORT void JNICALL DTrace_VPrint(const char * file, int line, int argc, const char * fmt, va_list arglist);
JNIEXPORT void JNICALL DTrace_VPrintln(const char * file, int line, int argc, const char * fmt, va_list arglist);

/* each file includes this flag indicating module trace status */
static dtrace_id        _Dt_FileTraceId = UNDEFINED_TRACE_ID;

/* not meant to be called from client code--
 * it's just a template for the other macros
 */
#define _DTrace_Template(_func, _ac, _f, _a1, _a2, _a3, _a4, _a5, _a6, _a7, _a8) \
{ \
    static dtrace_id _dt_lineid_ = UNDEFINED_TRACE_ID; \
    DTrace_PrintFunction((_func), &_Dt_FileTraceId, &_dt_lineid_, __FILE__, __LINE__, (_ac), (_f), (_a1), (_a2), (_a3), (_a4), (_a5), (_a6), (_a7), (_a8) ); \
}

/* printf style trace macros */
#define DTRACE_PRINT(_fmt) \
        _DTrace_Template(DTrace_VPrint, 0, (_fmt), 0, 0, 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINT1(_fmt, _arg1) \
        _DTrace_Template(DTrace_VPrint, 1, (_fmt), (_arg1), 0, 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINT2(_fmt, _arg1, _arg2) \
        _DTrace_Template(DTrace_VPrint, 2, (_fmt), (_arg1), (_arg2), 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINT3(_fmt, _arg1, _arg2, _arg3) \
        _DTrace_Template(DTrace_VPrint, 3, (_fmt), (_arg1), (_arg2), (_arg3), 0, 0, 0, 0, 0)
#define DTRACE_PRINT4(_fmt, _arg1, _arg2, _arg3, _arg4) \
        _DTrace_Template(DTrace_VPrint, 4, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), 0, 0, 0, 0)
#define DTRACE_PRINT5(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5) \
        _DTrace_Template(DTrace_VPrint, 5, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), 0, 0, 0)
#define DTRACE_PRINT6(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6) \
        _DTrace_Template(DTrace_VPrint, 6, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), 0, 0)
#define DTRACE_PRINT7(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7) \
        _DTrace_Template(DTrace_VPrint, 7, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), (_arg7), 0)
#define DTRACE_PRINT8(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8) \
        _DTrace_Template(DTrace_VPrint, 8, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), (_arg7), (_arg8))

/* printf style trace macros that automatically output a newline */
#define DTRACE_PRINTLN(_fmt) \
        _DTrace_Template(DTrace_VPrintln, 0, (_fmt), 0, 0, 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINTLN1(_fmt, _arg1) \
        _DTrace_Template(DTrace_VPrintln, 1, (_fmt), (_arg1), 0, 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINTLN2(_fmt, _arg1, _arg2) \
        _DTrace_Template(DTrace_VPrintln, 2, (_fmt), (_arg1), (_arg2), 0, 0, 0, 0, 0, 0)
#define DTRACE_PRINTLN3(_fmt, _arg1, _arg2, _arg3) \
        _DTrace_Template(DTrace_VPrintln, 3, (_fmt), (_arg1), (_arg2), (_arg3), 0, 0, 0, 0, 0)
#define DTRACE_PRINTLN4(_fmt, _arg1, _arg2, _arg3, _arg4) \
        _DTrace_Template(DTrace_VPrintln, 4, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), 0, 0, 0, 0)
#define DTRACE_PRINTLN5(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5) \
        _DTrace_Template(DTrace_VPrintln, 5, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), 0, 0, 0)
#define DTRACE_PRINTLN6(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6) \
        _DTrace_Template(DTrace_VPrintln, 6, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), 0, 0)
#define DTRACE_PRINTLN7(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7) \
        _DTrace_Template(DTrace_VPrintln, 7, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), (_arg7), 0)
#define DTRACE_PRINTLN8(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8) \
        _DTrace_Template(DTrace_VPrintln, 8, (_fmt), (_arg1), (_arg2), (_arg3), (_arg4), (_arg5), (_arg6), (_arg7), (_arg8))

#else /* else DEBUG not defined */

/* printf style trace macros */
#define DTRACE_PRINT(_fmt)
#define DTRACE_PRINT1(_fmt, _arg1)
#define DTRACE_PRINT2(_fmt, _arg1, _arg2)
#define DTRACE_PRINT3(_fmt, _arg1, _arg2, _arg3)
#define DTRACE_PRINT4(_fmt, _arg1, _arg2, _arg3, _arg4)
#define DTRACE_PRINT5(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5)
#define DTRACE_PRINT6(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6)
#define DTRACE_PRINT7(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7)
#define DTRACE_PRINT8(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8)

/* printf style trace macros that automatically output a newline */
#define DTRACE_PRINTLN(_fmt)
#define DTRACE_PRINTLN1(_fmt, _arg1)
#define DTRACE_PRINTLN2(_fmt, _arg1, _arg2)
#define DTRACE_PRINTLN3(_fmt, _arg1, _arg2, _arg3)
#define DTRACE_PRINTLN4(_fmt, _arg1, _arg2, _arg3, _arg4)
#define DTRACE_PRINTLN5(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5)
#define DTRACE_PRINTLN6(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6)
#define DTRACE_PRINTLN7(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7)
#define DTRACE_PRINTLN8(_fmt, _arg1, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8)

#endif /* endif DEBUG defined */

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /* _DEBUG_TRACE_H */
