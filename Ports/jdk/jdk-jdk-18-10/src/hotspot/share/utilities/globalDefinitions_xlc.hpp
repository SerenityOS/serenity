/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2021 SAP SE. All rights reserved.
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

#ifndef SHARE_UTILITIES_GLOBALDEFINITIONS_XLC_HPP
#define SHARE_UTILITIES_GLOBALDEFINITIONS_XLC_HPP

#include "jni.h"

// This file holds compiler-dependent includes,
// globally used constants & types, class (forward)
// declarations and a few frequently used utility functions.

#include <ctype.h>
#include <string.h>
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <wchar.h>

#include <math.h>
#include <time.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <pthread.h>

#include <limits.h>
#include <errno.h>

#include <stdint.h>

// check for xlc16 or higher
#ifdef __ibmxl_version__
  #if __ibmxl_version__ < 16
  #error "xlc < 16 not supported"
  #endif
#else
  #error "xlc < 16 not supported, macro __ibmxl_version__ not found"
#endif

#ifndef _AIX
#error "missing AIX-specific definition _AIX"
#endif

// Use XLC compiler builtins instead of inline assembler
#define USE_XLC_BUILTINS

#ifdef USE_XLC_BUILTINS
#include <builtins.h>
// XLC V10 and higher provide the prototype for __dcbtst (void *);
#endif // USE_XLC_BUILTINS

// NULL vs NULL_WORD:
// Some platform/tool-chain combinations can't assign NULL to an integer
// type so we define NULL_WORD to use in those contexts. For xlc they are the same.
#define NULL_WORD  NULL

// AIX also needs a 64 bit NULL to work as a null address pointer.
// Most system includes on AIX would define it as an int 0 if not already defined with one
// exception: /usr/include/dirent.h will unconditionally redefine NULL to int 0 again.
// In this case you need to copy the following defines to a position after #include <dirent.h>
#include <dirent.h>
#ifdef _LP64
  #undef NULL
  #define NULL 0L
#else
  #ifndef NULL
    #define NULL 0
  #endif
#endif

// Compiler-specific primitive types
// All defs of int (uint16_6 etc) are defined in AIX' /usr/include/stdint.h

// Additional Java basic types

typedef uint8_t  jubyte;
typedef uint16_t jushort;
typedef uint32_t juint;
typedef uint64_t julong;

// checking for nanness
inline int g_isnan(float  f) { return isnan(f); }
inline int g_isnan(double f) { return isnan(f); }

// Checking for finiteness
inline int g_isfinite(jfloat  f)                 { return finite(f); }
inline int g_isfinite(jdouble f)                 { return finite(f); }

// Wide characters
inline int wcslen(const jchar* x) { return wcslen((const wchar_t*)x); }


// Formatting.
#ifdef _LP64
#define FORMAT64_MODIFIER "l"
#else // !_LP64
#define FORMAT64_MODIFIER "ll"
#endif // _LP64

// Cannot use xlc's offsetof as implementation of hotspot's
// offset_of(), because xlc warns about applying offsetof() to non-POD
// object and xlc cannot compile the expression offsetof(DataLayout,
// _cells[index]) in DataLayout::cell_offset() .  Therefore we define
// offset_of as it is defined for gcc.
#define offset_of(klass,field) (size_t)((intx)&(((klass*)16)->field) - 16)

#define THREAD_LOCAL __thread

// Inlining support
//
// Be aware that for function/method declarations, xlC only supports the following
// syntax (i.e. the attribute must be placed AFTER the function/method declarator):
//
//   void* operator new(size_t size) throw() NOINLINE;
//
// For function/method defintions, the more common placement BEFORE the
// function/method declarator seems to be supported as well:
//
//   NOINLINE void* CHeapObj<F>::operator new(size_t size) throw() {...}

#define NOINLINE     __attribute__((__noinline__))
#define ALWAYSINLINE inline __attribute__((__always_inline__))

#endif // SHARE_UTILITIES_GLOBALDEFINITIONS_XLC_HPP
