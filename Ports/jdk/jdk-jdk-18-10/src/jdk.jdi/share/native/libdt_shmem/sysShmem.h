/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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
#ifndef _JAVASOFT_SYSSHMEM_H

#include <jni.h>
#include "sys.h"
#include "shmem_md.h"

int sysSharedMemCreate(const char *name, int length, sys_shmem_t *, void **buffer);
int sysSharedMemOpen(const char *name,  sys_shmem_t *, void **buffer);
int sysSharedMemClose(sys_shmem_t, void *buffer);

/* Mutexes that can be used for inter-process communication */
int sysIPMutexCreate(const char *name, sys_ipmutex_t *mutex);
int sysIPMutexOpen(const char *name, sys_ipmutex_t *mutex);
int sysIPMutexEnter(sys_ipmutex_t mutex, sys_event_t event);
int sysIPMutexExit(sys_ipmutex_t mutex);
int sysIPMutexClose(sys_ipmutex_t mutex);

/* Inter-process events */
int sysEventCreate(const char *name, sys_event_t *event, jboolean manualreset);
int sysEventOpen(const char *name, sys_event_t *event);
int sysEventWait(sys_process_t otherProcess, sys_event_t event, long timeout);
int sysEventSignal(sys_event_t event);
int sysEventClose(sys_event_t event);

jlong sysProcessGetID();
int sysProcessOpen(jlong processID, sys_process_t *process);
int sysProcessClose(sys_process_t *process);

/* access to errno or equivalent */
int sysGetLastError(char *buf, int size);

/* access to thread-local storage */
int sysTlsAlloc();
void sysTlsFree(int index);
void sysTlsPut(int index, void *value);
void *sysTlsGet(int index);

/* misc. functions */
void sysSleep(long duration);

#endif
