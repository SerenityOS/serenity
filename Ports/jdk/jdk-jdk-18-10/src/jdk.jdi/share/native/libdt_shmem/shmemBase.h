/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
#include "jdwpTransport.h"

#ifndef JAVASOFT_SHMEMBASE_H
#define JAVASOFT_SHMEMBASE_H

void exitTransportWithError(char *msg, char *fileName,
                            char *date, int lineNumber);

typedef struct SharedMemoryConnection SharedMemoryConnection;
typedef struct SharedMemoryTransport SharedMemoryTransport;

typedef void * (*SharedMemAllocFunc)(jint);
typedef void  (*SharedMemFreeFunc)(void);

jint shmemBase_initialize(JavaVM *, jdwpTransportCallback *callback);
jint shmemBase_listen(const char *address, SharedMemoryTransport **);
jint shmemBase_accept(SharedMemoryTransport *, long, SharedMemoryConnection **);
jint shmemBase_attach(const char *addressString, long, SharedMemoryConnection **);
void shmemBase_closeConnection(SharedMemoryConnection *);
void shmemBase_closeTransport(SharedMemoryTransport *);
jint shmemBase_sendByte(SharedMemoryConnection *, jbyte data);
jint shmemBase_receiveByte(SharedMemoryConnection *, jbyte *data);
jint shmemBase_sendPacket(SharedMemoryConnection *, const jdwpPacket *packet);
jint shmemBase_receivePacket(SharedMemoryConnection *, jdwpPacket *packet);
jint shmemBase_name(SharedMemoryTransport *, char **name);
jint shmemBase_getlasterror(char *msg, jint size);

#ifdef DEBUG
#define SHMEM_ASSERT(expression)  \
do {                            \
    if (!(expression)) {                \
        exitTransportWithError("assertion failed", __FILE__, __DATE__, __LINE__); \
    } \
} while (0)
#else
#define SHMEM_ASSERT(expression) ((void) 0)
#endif

#define SHMEM_GUARANTEE(expression) \
do {                            \
    if (!(expression)) {                \
        exitTransportWithError("assertion failed", __FILE__, __DATE__, __LINE__); \
    } \
} while (0)

#endif
