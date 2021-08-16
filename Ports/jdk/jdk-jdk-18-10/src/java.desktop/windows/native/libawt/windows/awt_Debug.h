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

#if !defined(_AWT_DEBUG_H)
#define _AWT_DEBUG_H

#include "debug_assert.h"
#include "debug_trace.h"

#if defined(DEBUG)
    #if defined(new)
        #error new has already been defined!
    #endif
    class AwtDebugSupport {
        public:
            AwtDebugSupport();
            ~AwtDebugSupport();

            static void AssertCallback(const char * expr, const char * file,
                                       int line);
            /* This method signals that the VM is exiting cleanly, and thus
               the debug memory manager should dump a leaks report when the
               VM has finished exiting. This method should not be called for
               termination exits (such as <CTRL>-C) */
            static void GenerateLeaksReport();
    };

    extern void * operator new(size_t size, const char * filename, int linenumber);
    extern void * operator new[](size_t size, const char * filename, int linenumber);

    extern void operator delete(void *ptr, const char*, int);
    extern void operator delete[](void *ptr, const char*, int);

    extern void operator delete(void *ptr) throw();
    extern void DumpClipRectangle(const char * file, int line, int argc, const char * fmt, va_list arglist);
    extern void DumpUpdateRectangle(const char * file, int line, int argc, const char * fmt, va_list arglist);

    #define AWT_DUMP_UPDATE_RECTANGLE(_msg, _hwnd) \
        _DTrace_Template(DumpUpdateRectangle, 2, "", (_msg), (_hwnd), 0, 0, 0, 0, 0, 0)

    #define AWT_DUMP_CLIP_RECTANGLE(_msg, _hwnd) \
        _DTrace_Template(DumpClipRectangle, 2, "", (_msg), (_hwnd), 0, 0, 0, 0, 0, 0)

    #define new         new(__FILE__, __LINE__)

    #define VERIFY(exp)         DASSERT(exp)
    #define UNIMPLEMENTED()     DASSERT(FALSE)

    /* Disable inlining. */
    #define INLINE
#else
    #define AWT_DUMP_UPDATE_RECTANGLE(_msg, _hwnd) ((void)0)
    #define AWT_DUMP_CLIP_RECTANGLE(_msg, _hwnd) ((void)0)

    #define UNIMPLEMENTED() \
        SignalError(0, JAVAPKG "NullPointerException","unimplemented");

    /*
    * VERIFY macro -- assertion where expression is always evaluated
    * (normally used for BOOL functions).
    */
    #define VERIFY(exp) ((void)(exp))

    /* Enable inlining. */
    #define INLINE inline
#endif // DEBUG

#endif // _AWT_DEBUG_H
