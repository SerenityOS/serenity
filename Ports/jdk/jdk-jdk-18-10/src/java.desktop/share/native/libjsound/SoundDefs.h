/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __SOUNDDEFS_INCLUDED__
#define __SOUNDDEFS_INCLUDED__


// types for X_PLATFORM
#define X_WINDOWS       1
#define X_LINUX         2
#define X_BSD           3
#define X_MACOSX        4

// **********************************
// Make sure you set X_PLATFORM defines correctly.
// Everything depends upon this flag being setup correctly.
// **********************************

#if (!defined(X_PLATFORM))
#error "You need to define X_PLATFORM outside of the source. Use the types above."
#endif


// following is needed for _LP64
#if ((X_PLATFORM == X_LINUX) || (X_PLATFORM == X_MACOSX))
#include <sys/types.h>
#endif

#if X_PLATFORM == X_WINDOWS
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#endif /* X_PLATFORM == X_WINDOWS */


/*
* These types are defined elsewhere for newer 32/64-bit Windows
* header files, but not on Solaris/Linux (X_PLATFORM != X_WINDOWS)
*/
#if (X_PLATFORM != X_WINDOWS)

typedef unsigned char           UINT8;
typedef char                    INT8;
typedef short                   INT16;
typedef unsigned short          UINT16;
#ifdef _LP64
typedef int                     INT32;
typedef unsigned int            UINT32;
typedef unsigned long           UINT64;
typedef long                    INT64;
#else /* _LP64 */
typedef long                    INT32;
typedef unsigned long           UINT32;
/* generic 64 bit ? */
typedef unsigned long long      UINT64;
typedef long long               INT64;
#endif /* _LP64 */

typedef unsigned long           UINT_PTR;
typedef long                    INT_PTR;

#endif /* X_PLATFORM != X_WINDOWS */


typedef unsigned char   UBYTE;
typedef char            SBYTE;


#undef TRUE
#undef FALSE

#ifndef TRUE
#define TRUE    1
#endif

#ifndef FALSE
#define FALSE   0
#endif

#undef NULL
#ifndef NULL
#define NULL    0L
#endif




#if X_PLATFORM == X_WINDOWS
#include <stdlib.h>
#define INLINE          _inline
#endif


#if X_PLATFORM == X_LINUX
#define INLINE          inline
#endif


#if (X_PLATFORM == X_BSD) || (X_PLATFORM == X_MACOSX)
#define INLINE          inline
#endif


#endif  // __SOUNDDEFS_INCLUDED__
