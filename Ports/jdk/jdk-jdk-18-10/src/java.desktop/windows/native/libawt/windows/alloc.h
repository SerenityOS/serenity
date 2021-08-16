/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _ALLOC_H_
#define _ALLOC_H_

#include "stdhdrs.h"

// By defining std::bad_alloc in a local header file instead of including
// the Standard C++ <new> header file, we avoid making awt.dll dependent
// on msvcp50.dll. This reduces the size of the JRE by 500kb.
namespace std {
    class bad_alloc {};
}

#define SIZECALC_ALLOC_THROWING_BAD_ALLOC
#include "sizecalc.h"

class awt_toolkit_shutdown {};

// Disable "C++ Exception Specification ignored" warnings.
// These warnings are generated because VC++ 5.0 allows, but does not enforce,
// exception specifications. This #pragma can be safely removed when VC++
// is updated to enforce exception specifications.
#pragma warning(disable : 4290)

#ifdef TRY
#error Multiple definitions of TRY
#endif

#ifdef TRY_NO_VERIFY
#error Multiple definitions of TRY_NO_VERIFY
#endif

#ifdef CATCH_BAD_ALLOC
#error Multiple definitions of CATCH_BAD_ALLOC
#endif

#ifdef CATCH_BAD_ALLOC_RET
#error Multiple defintions of CATCH_BAD_ALLOC_RET
#endif

#ifdef TRY_NO_JNI
#error Multiple definitions of TRY_NO_JNI
#endif

#ifdef TRY_NO_VERIFY_NO_JNI
#error Multiple definitions of TRY_NO_VERIFY_NO_JNI
#endif

#ifdef CATCH_BAD_ALLOC_NO_JNI
#error Multiple definitions of CATCH_BAD_ALLOC_NO_JNI
#endif

#ifdef CATCH_BAD_ALLOC_RET_NO_JNI
#error Multiple defintions of CATCH_BAD_ALLOC_RET_NO_JNI
#endif

// The unsafe versions of malloc, calloc, and realloc should not be used
#define malloc Do_Not_Use_malloc_Use_safe_Malloc_Instead
#define calloc Do_Not_Use_calloc_Use_safe_Calloc_Instead
#define realloc Do_Not_Use_realloc_Use_safe_Realloc_Instead
#define ExceptionOccurred Do_Not_Use_ExceptionOccurred_Use_safe_\
ExceptionOccurred_Instead

// These three functions throw std::bad_alloc in an out of memory condition
// instead of returning 0. safe_Realloc will return 0 if memblock is not
// NULL and size is 0. safe_Malloc and safe_Calloc will never return 0.
void *safe_Malloc(size_t size) throw (std::bad_alloc);
void *safe_Calloc(size_t num, size_t size) throw (std::bad_alloc);
void *safe_Realloc(void *memblock, size_t size) throw (std::bad_alloc);

// This function should be called instead of ExceptionOccurred. It throws
// std::bad_alloc if a java.lang.OutOfMemoryError is currently pending
// on the calling thread.
jthrowable safe_ExceptionOccurred(JNIEnv *env) throw (std::bad_alloc);

// This function is called at the beginning of an entry point.
// Entry points are functions which are declared:
//   1. CALLBACK,
//   2. JNIEXPORT,
//   3. __declspec(dllexport), or
//   4. extern "C"
// A function which returns an HRESULT (an OLE function) is also an entry
// point.
void entry_point(void);

// This function hangs indefinitely if the Toolkit is not active
void hang_if_shutdown(void);

// This function throws awt_toolkit_shutdown if the Toolkit is not active
void throw_if_shutdown(void) throw (awt_toolkit_shutdown);

// This function is called when a std::bad_alloc exception is caught
void handle_bad_alloc(void);

// Uncomment to nondeterministically test OutOfMemory errors
// #define OUTOFMEM_TEST

#ifdef OUTOFMEM_TEST
    void *safe_Malloc_outofmem(size_t size, const char *, int)
        throw (std::bad_alloc);
    void *safe_Calloc_outofmem(size_t num, size_t size, const char *, int)
        throw (std::bad_alloc);
    void *safe_Realloc_outofmem(void *memblock, size_t size, const char *, int)
        throw (std::bad_alloc);
    void * CDECL operator new(size_t size, const char *, int)
        throw (std::bad_alloc);

    #define safe_Malloc(size) \
        safe_Malloc_outofmem(size, __FILE__, __LINE__)
    #define safe_Calloc(num, size) \
        safe_Calloc_outofmem(num, size, __FILE__, __LINE__)
    #define safe_Realloc(memblock, size) \
        safe_Realloc_outofmem(memblock, size, __FILE__, __LINE__)
    #define new new(__FILE__, __LINE__)
#endif /* OUTOFMEM_TEST */

#define TRY \
    try { \
        entry_point(); \
        hang_if_shutdown();
// The _NO_HANG version of TRY causes the AWT native code to return to Java
// immediately if the Toolkit is not active. Normal AWT operations should
// never use this macro. It should only be used for cleanup routines where:
// (1) Hanging is not a valid option, because the method is called during
// execution of runFinalizersOnExit; and, (2) Execution of the method would
// generate a NullPointerException or other Exception.
#define TRY_NO_HANG \
    try { \
        entry_point(); \
        throw_if_shutdown();
// The _NO_VERIFY version of TRY does not verify that the Toolkit is still
// active before proceeding. Normal AWT operations should never use this
// macro. It should only be used for cleanup routines which can safely
// execute after the Toolkit is disposed, and then only with caution. Users
// of this macro must be able to guarantee that the code which will execute
// will not generate a NullPointerException or other Exception.
#define TRY_NO_VERIFY \
    try { \
        entry_point();
#define CATCH_BAD_ALLOC \
    } catch (std::bad_alloc&) { \
        handle_bad_alloc(); \
        return; \
    } catch (awt_toolkit_shutdown&) {\
        return; \
    }
#define CATCH_BAD_ALLOC_RET(x) \
    } catch (std::bad_alloc&) { \
        handle_bad_alloc(); \
        return (x); \
    } catch (awt_toolkit_shutdown&) {\
        return (0); \
    }

// The _NO_JNI versions of TRY and CATCH_BAD_ALLOC simply discard
// std::bad_alloc exceptions and thus should be avoided at all costs. They
// are only useful if the calling function currently holds the JNI lock
// for the thread. This lock is acquired by calling GetPrimitiveArrayCritical
// or GetStringCritical. No JNI function should be called by that thread
// until the corresponding Release function has been called.

#define TRY_NO_JNI \
    try { \
        hang_if_shutdown();
#define TRY_NO_HANG_NO_JNI \
    try { \
        throw_if_shutdown();
#define TRY_NO_VERIFY_NO_JNI \
    try {
#define CATCH_BAD_ALLOC_NO_JNI \
    } catch (std::bad_alloc&) { \
        return; \
    } catch (awt_toolkit_shutdown&) {\
        return; \
    }
#define CATCH_BAD_ALLOC_RET_NO_JNI(x) \
    } catch (std::bad_alloc&) { \
        return (x); \
    } catch (awt_toolkit_shutdown&) {\
        return (0); \
    }

#endif /* _ALLOC_H_ */
