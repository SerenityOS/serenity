/*
 * Copyright (c) 1999, 2005, Oracle and/or its affiliates. All rights reserved.
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

#include <new.h>
#include <stdio.h>
#include "awt_new.h"
#include "awt_Toolkit.h"
#include "Hashtable.h"

// Don't want to pull in the redefined allocation functions
#undef malloc
#undef calloc
#undef realloc
#undef ExceptionOccurred

#ifdef OUTOFMEM_TEST
  #undef safe_Malloc
  #undef safe_Calloc
  #undef safe_Realloc
  #undef new

  static CriticalSection *alloc_lock;
  static FILE *logfile;
  static DWORD thread_seeded = TLS_OUT_OF_INDEXES;
#endif


void
NewHandler::init() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

#ifdef OUTOFMEM_TEST
    alloc_lock = new CriticalSection();
    logfile = fopen("java.awt.outofmem.txt", "w");
    DASSERT(logfile);
    thread_seeded = TlsAlloc();
    DASSERT(thread_seeded != TLS_OUT_OF_INDEXES);
#endif

    // use new handler for operator new and malloc
    _set_new_mode(1);

    // set the function which will be called when operator new or
    // malloc runs out of memory
    _set_new_handler((_PNH)NewHandler::handler);
}

// Called when malloc or operator new runs out of memory. We try to
// compact the heap by initiating a Java GC. If the amount of free
// memory available after this operation increases, then we return
// (1) to indicate that malloc or operator new should retry the
// allocation. Returning (0) indicates that the allocation should fail.
int
NewHandler::handler(size_t) {
    fprintf(stderr, "java.lang.OutOfMemoryError\n");
    return FALSE;
}

// These three functions throw std::bad_alloc in an out of memory condition
// instead of returning 0. safe_Realloc will return 0 if memblock is not
// NULL and size is 0. safe_Malloc and safe_Calloc will never return 0.
void *safe_Malloc(size_t size) throw (std::bad_alloc) {
    register void *ret_val = malloc(size);
    if (ret_val == NULL) {
        throw std::bad_alloc();
    }

    return ret_val;
}

void *safe_Calloc(size_t num, size_t size) throw (std::bad_alloc) {
    register void *ret_val = calloc(num, size);
    if (ret_val == NULL) {
        throw std::bad_alloc();
    }

    return ret_val;
}

void *safe_Realloc(void *memblock, size_t size) throw (std::bad_alloc) {
    register void *ret_val = realloc(memblock, size);

    // Special case for realloc.
    if (memblock != NULL && size == 0) {
        return ret_val; // even if it's NULL
    }

    if (ret_val == NULL) {
        throw std::bad_alloc();
    }

    return ret_val;
}

#if !defined(DEBUG)
// This function exists because VC++ 5.0 currently does not conform to the
// Standard C++ specification which requires that operator new throw
// std::bad_alloc in an out of memory situation. Instead, VC++ 5.0 returns 0.
//
// This function can be safely removed when the problem is corrected.
void * CDECL operator new(size_t size) throw (std::bad_alloc) {
    return safe_Malloc(size);
}
#endif

// This function is called at the beginning of an entry point.
// Entry points are functions which are declared:
//   1. CALLBACK,
//   2. JNIEXPORT,
//   3. __declspec(dllexport), or
//   4. extern "C"
// A function which returns an HRESULT (an OLE function) is also an entry
// point.
void
entry_point(void) {
    if (jvm != NULL) {
        JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        if (env != NULL) {
            env->ExceptionClear();
        }
    }
}


// This function is called when a std::bad_alloc exception is caught.
void
handle_bad_alloc(void) {
    if (jvm != NULL) {
        JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        if (env != NULL && !env->ExceptionCheck()) {
            JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
        }
    }
}


// This function is called instead of ExceptionOccurred. It throws
// std::bad_alloc if a java.lang.OutOfMemoryError is currently pending
// on the calling thread.
jthrowable
safe_ExceptionOccurred(JNIEnv *env) throw (std::bad_alloc) {
    jthrowable xcp = env->ExceptionOccurred();
    if (xcp != NULL) {
        env->ExceptionClear(); // if we don't do this, isInstanceOf will fail
        jint isOutofmem = JNU_IsInstanceOfByName(env, xcp, "java/lang/OutOfMemoryError");
        if (isOutofmem > 0) {
            env->DeleteLocalRef(xcp);
            throw std::bad_alloc();
        } else {
            env->ExceptionClear();
            // rethrow exception
            env->Throw(xcp);
            // temp solution to reveal all concurrency issues in jtreg and JCK
            // we will switch it back to silent mode before the release
            env->ExceptionDescribe();
            return xcp;
        }
    }

    return NULL;
}

#ifdef OUTOFMEM_TEST

#include <time.h>
#include <limits.h>

static void
rand_alloc_fail(const char *file, int line) throw (std::bad_alloc)
{
    if (alloc_lock == NULL) { // Not yet initialized
        return;
    }

    CriticalSection::Lock l(*alloc_lock);

    // Each thread must be seeded individually
    if (!TlsGetValue(thread_seeded)) {
        TlsSetValue(thread_seeded, (LPVOID)1);
        srand((unsigned int)time(NULL));
    }

    if (rand() > (int)(RAND_MAX * .999)) { // .1% chance of alloc failure
        fprintf(stderr, "failing allocation at %s, %d\n", file, line);
        fprintf(logfile, "%s, %d\n", file, line);
        fflush(logfile);

        VERIFY(malloc(INT_MAX) == 0); // should fail

        throw std::bad_alloc();
    }
}

void *safe_Malloc_outofmem(size_t size, const char *file, int line)
    throw (std::bad_alloc)
{
    rand_alloc_fail(file, line);
    return safe_Malloc(size);
}

void *safe_Calloc_outofmem(size_t num, size_t size, const char *file, int line)
    throw (std::bad_alloc)
{
    rand_alloc_fail(file, line);
    return safe_Calloc(num, size);
}

void *safe_Realloc_outofmem(void *memblock, size_t size, const char *file,
                            int line)
    throw (std::bad_alloc)
{
    rand_alloc_fail(file, line);
    return safe_Realloc(memblock, size);
}

void * CDECL operator new(size_t size, const char *file, int line)
    throw (std::bad_alloc)
{
    rand_alloc_fail(file, line);
    return operator new(size);
}

#endif
