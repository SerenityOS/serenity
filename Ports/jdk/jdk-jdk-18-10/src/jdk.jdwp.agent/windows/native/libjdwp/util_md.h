/*
 * Copyright (c) 1998, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_UTIL_MD_H
#define JDWP_UTIL_MD_H

#include <stddef.h>      /* for uintptr_t */
#include <stdlib.h>      /* for _MAx_PATH */

typedef unsigned __int64 UNSIGNED_JLONG;
typedef unsigned long UNSIGNED_JINT;

#define MAXPATHLEN _MAX_PATH

/* Needed on Windows because names seem to be hidden in stdio.h. */

#define snprintf        _snprintf
#define vsnprintf       _vsnprintf

/* On little endian machines, convert java big endian numbers. */

#define HOST_TO_JAVA_CHAR(x) (((x & 0xff) << 8) | ((x >> 8) & (0xff)))
#define HOST_TO_JAVA_SHORT(x) (((x & 0xff) << 8) | ((x >> 8) & (0xff)))
#define HOST_TO_JAVA_INT(x)                                             \
                  ((x << 24) |                                          \
                   ((x & 0x0000ff00) << 8) |                            \
                   ((x & 0x00ff0000) >> 8) |                            \
                   (((UNSIGNED_JINT)(x & 0xff000000)) >> 24))
#define HOST_TO_JAVA_LONG(x)                                            \
                  ((x << 56) |                                          \
                   ((x & 0x000000000000ff00) << 40) |                   \
                   ((x & 0x0000000000ff0000) << 24) |                   \
                   ((x & 0x00000000ff000000) << 8) |                    \
                   ((x & 0x000000ff00000000) >> 8) |                    \
                   ((x & 0x0000ff0000000000) >> 24) |                   \
                   ((x & 0x00ff000000000000) >> 40) |                   \
                   (((UNSIGNED_JLONG)(x & 0xff00000000000000)) >> 56))
#define HOST_TO_JAVA_FLOAT(x) stream_encodeFloat(x)
#define HOST_TO_JAVA_DOUBLE(x) stream_encodeDouble(x)

#define JAVA_TO_HOST_CHAR(x)   HOST_TO_JAVA_CHAR(x)
#define JAVA_TO_HOST_SHORT(x)  HOST_TO_JAVA_SHORT(x)
#define JAVA_TO_HOST_INT(x)    HOST_TO_JAVA_INT(x)
#define JAVA_TO_HOST_LONG(x)   HOST_TO_JAVA_LONG(x)
#define JAVA_TO_HOST_FLOAT(x)  HOST_TO_JAVA_FLOAT(x)
#define JAVA_TO_HOST_DOUBLE(x) HOST_TO_JAVA_DOUBLE(x)

#endif
