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

#include <windows.h>
#include <errno.h>

#include "shmem_md.h"
#include "sysShmem.h"
#include "shmemBase.h"  /* for exitTransportWithError */

/*
 * These functions are not completely universal. For now, they are used
 * exclusively for Jbug's shared memory transport mechanism. They have
 * been implemented on Win32 only so far, so the abstractions may not be correct
 * yet.
 */

static HANDLE memHandle = NULL;

#ifdef DEBUG
#define sysAssert(expression) {         \
    if (!(expression)) {                \
            exitTransportWithError \
            ("\"%s\", line %d: assertion failure\n", \
             __FILE__, __DATE__, __LINE__); \
    }                                   \
}
#else
#define sysAssert(expression) ((void) 0)
#endif

int
sysSharedMemCreate(const char *name, int length,
                   sys_shmem_t *mem, void **buffer)
{
    void *mappedMemory;
    HANDLE memHandle;

    sysAssert(buffer);
    sysAssert(name);
    sysAssert(length > 0);

    memHandle  =
        CreateFileMapping(INVALID_HANDLE_VALUE, /* backed by page file */
                          NULL,               /* no inheritance */
                          PAGE_READWRITE,
                          0, length,          /* hi, lo order of length */
                          name);
    if (memHandle == NULL) {
        return SYS_ERR;
    } else if (GetLastError() == ERROR_ALREADY_EXISTS) {
        /* If the call above didn't create it, consider it an error */
        CloseHandle(memHandle);
        memHandle = NULL;
        return SYS_INUSE;
    }

    mappedMemory =
        MapViewOfFile(memHandle,
                      FILE_MAP_WRITE,       /* read/write */
                      0, 0, 0);             /* map entire "file" */

    if (mappedMemory == NULL) {
        CloseHandle(memHandle);
        memHandle = NULL;
        return SYS_ERR;
    }

    *mem = memHandle;
    *buffer = mappedMemory;
    return SYS_OK;
}

int
sysSharedMemOpen(const char *name, sys_shmem_t *mem, void **buffer)
{
    void *mappedMemory;
    HANDLE memHandle;

    sysAssert(name);
    sysAssert(buffer);

    memHandle =
        OpenFileMapping(FILE_MAP_WRITE,     /* read/write */
                        FALSE,              /* no inheritance */
                        name);
    if (memHandle == NULL) {
        return SYS_ERR;
    }

    mappedMemory =
        MapViewOfFile(memHandle,
                      FILE_MAP_WRITE,       /* read/write */
                      0, 0, 0);             /* map entire "file" */

    if (mappedMemory == NULL) {
        CloseHandle(memHandle);
        memHandle = NULL;
        return SYS_ERR;
    }

    *mem = memHandle;
    *buffer = mappedMemory;
    return SYS_OK;
}

int
sysSharedMemClose(sys_shmem_t mem, void *buffer)
{
    if (buffer != NULL) {
        if (!UnmapViewOfFile(buffer)) {
            return SYS_ERR;
        }
    }

    if (!CloseHandle(mem)) {
        return SYS_ERR;
    }

    return SYS_OK;
}

int
sysIPMutexCreate(const char *name, sys_ipmutex_t *mutexPtr)
{
    HANDLE mutex;

    sysAssert(mutexPtr);
    sysAssert(name);

    mutex = CreateMutex(NULL,            /* no inheritance */
                        FALSE,           /* no initial owner */
                        name);
    if (mutex == NULL) {
        return SYS_ERR;
    } else if (GetLastError() == ERROR_ALREADY_EXISTS) {
        /* If the call above didn't create it, consider it an error */
        CloseHandle(mutex);
        return SYS_INUSE;
    }

    *mutexPtr = mutex;
    return SYS_OK;
}

int
sysIPMutexOpen(const char *name, sys_ipmutex_t *mutexPtr)
{
    HANDLE mutex;

    sysAssert(mutexPtr);
    sysAssert(name);

    mutex = OpenMutex(SYNCHRONIZE,      /* able to wait/release */
                      FALSE,            /* no inheritance */
                      name);
    if (mutex == NULL) {
        return SYS_ERR;
    }

    *mutexPtr = mutex;
    return SYS_OK;
}

int
sysIPMutexEnter(sys_ipmutex_t mutex, sys_event_t event)
{
    HANDLE handles[2] = { mutex, event };
    int count = event == NULL ? 1 : 2;
    DWORD rc;

    sysAssert(mutex);
    rc = WaitForMultipleObjects(count, handles,
                                FALSE,              /* wait for either, not both */
                                INFINITE);          /* infinite timeout */
    return (rc == WAIT_OBJECT_0) ? SYS_OK : SYS_ERR;
}

int
sysIPMutexExit(sys_ipmutex_t mutex)
{
    sysAssert(mutex);
    return ReleaseMutex(mutex) ? SYS_OK : SYS_ERR;
}

int
sysIPMutexClose(sys_ipmutex_t mutex)
{
    return CloseHandle(mutex) ? SYS_OK : SYS_ERR;
}

int
sysEventCreate(const char *name, sys_event_t *eventPtr, jboolean manualReset)
{
    HANDLE event;
    BOOL reset = (manualReset == JNI_TRUE) ? TRUE : FALSE;

    sysAssert(eventPtr);

    event = CreateEvent(NULL,            /* no inheritance */
                        reset,           /* manual reset */
                        FALSE,           /* initially, not signalled */
                        name);
    if (event == NULL) {
        return SYS_ERR;
    } else if (GetLastError() == ERROR_ALREADY_EXISTS) {
        /* If the call above didn't create it, consider it an error */
        CloseHandle(event);
        return SYS_INUSE;
    }

    *eventPtr = event;
    return SYS_OK;
}

int
sysEventOpen(const char *name, sys_event_t *eventPtr)
{
    HANDLE event;

    sysAssert(eventPtr);
    sysAssert(name);

    event = OpenEvent(SYNCHRONIZE | EVENT_MODIFY_STATE,
                                        /* able to wait/signal */
                      FALSE,            /* no inheritance */
                      name);
    if (event == NULL) {
        return SYS_ERR;
    }

    *eventPtr = event;
    return SYS_OK;
}

int
sysEventWait(sys_process_t otherProcess, sys_event_t event, long timeout)
{
    HANDLE handles[2];        /* process, event */
    DWORD rc;
    int count;
    DWORD dwTimeout = (timeout == 0) ? INFINITE : (DWORD)timeout;

    /*
     * If the signalling process is specified, and it dies while we wait,
     * detect it and return an error.
     */
    sysAssert(event);

    handles[0] = event;
    handles[1] = otherProcess;

    count = (otherProcess == NULL) ? 1 : 2;

    rc = WaitForMultipleObjects(count, handles,
                                FALSE,        /* wait for either, not both */
                                dwTimeout);
    if (rc == WAIT_OBJECT_0) {
        /* Signalled, return success */
        return SYS_OK;
    } else if (rc == WAIT_OBJECT_0 + 1) {
        /* Other process died, return error */
        return SYS_DIED;
    } else if (rc == WAIT_TIMEOUT) {
        /* timeout */
        return SYS_TIMEOUT;
    }
    return SYS_ERR;
}

int
sysEventSignal(sys_event_t event)
{
    sysAssert(event);
    return SetEvent(event) ? SYS_OK : SYS_ERR;
}

int
sysEventClose(sys_event_t event)
{
    return CloseHandle(event) ? SYS_OK : SYS_ERR;
}

jlong
sysProcessGetID()
{
    return GetCurrentProcessId();
}

int
sysProcessOpen(jlong processID, sys_process_t *processPtr)
{
    HANDLE process;

    sysAssert(processPtr);

    process = OpenProcess(SYNCHRONIZE,    /* able to wait on death */
                          FALSE,          /* no inheritance */
                          (DWORD)processID);
    if (process == NULL) {
        return SYS_ERR;
    }

    *processPtr = process;
    return SYS_OK;
}

int
sysProcessClose(sys_process_t *process)
{
    return CloseHandle(process) ? SYS_OK : SYS_ERR;
}

int
sysGetLastError(char *buf, int len)
{
    long errval = GetLastError();
    if (errval != 0) {
        int n = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS,
                              NULL, errval,
                              0, buf, len, NULL);
        if (n > 3) {
            /* Drop final '.', CR, LF */
            if (buf[n - 1] == '\n') n--;
            if (buf[n - 1] == '\r') n--;
            if (buf[n - 1] == '.') n--;
            buf[n] = '\0';
        }
        return SYS_OK;
    }
    buf[0] = '\0';
    return 0;
}

int
sysTlsAlloc() {
    return TlsAlloc();
}

void
sysTlsFree(int index) {
    TlsFree(index);
}

void
sysTlsPut(int index, void *value) {
    TlsSetValue(index, value);
}

void *
sysTlsGet(int index) {
    return TlsGetValue(index);
}

void
sysSleep(long duration) {
    Sleep((DWORD)duration);
}
