/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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


#define USE_ERROR
//#define USE_TRACE

#include "PLATFORM_API_WinOS_Util.h"

#if (USE_PLATFORM_MIDI_IN == TRUE) || (USE_PLATFORM_MIDI_OUT == TRUE)

/* set the startTime field in MidiDeviceHandle */
void MIDI_SetStartTime(MidiDeviceHandle* handle) {
    if (handle != NULL) {
                handle->startTime = (INT64) timeGetTime();
    }
}


/* return time stamp in microseconds */
INT64 MIDI_GetTimeStamp(MidiDeviceHandle* handle) {
    INT64 res;
    if (handle == NULL) {
                return (INT64) -1;
    }
    res = ((INT64) timeGetTime()) - handle->startTime;
    if (res < 0) {
                res *= (INT64) -1000;
    } else {
                res *= (INT64) 1000;
    }
    return res;
}


void* MIDI_CreateLock() {
    CRITICAL_SECTION* lock = (CRITICAL_SECTION*) malloc(sizeof(CRITICAL_SECTION));
    InitializeCriticalSection(lock);
    TRACE0("MIDI_CreateLock\n");
    return lock;
}

void MIDI_DestroyLock(void* lock) {
    if (lock) {
        DeleteCriticalSection((CRITICAL_SECTION*) lock);
        free(lock);
        TRACE0("MIDI_DestroyLock\n");
    }
}

void MIDI_Lock(void* lock) {
    if (lock) {
        EnterCriticalSection((CRITICAL_SECTION*) lock);
    }
}

void MIDI_Unlock(void* lock) {
    if (lock) {
        LeaveCriticalSection((CRITICAL_SECTION*) lock);
    }
}
int MIDI_WinCreateEmptyLongBufferQueue(MidiDeviceHandle* handle, int count) {
    return MIDI_WinCreateLongBufferQueue(handle, count, 0, NULL);
}

int MIDI_WinCreateLongBufferQueue(MidiDeviceHandle* handle, int count, int size, UBYTE* preAllocatedMem) {
    SysExQueue* sysex;
    int i;
    UBYTE* dataPtr;
    int structSize = sizeof(SysExQueue) + ((count - 1) * sizeof(MIDIHDR));

    sysex = (SysExQueue*) malloc(structSize);
    if (!sysex) return FALSE;
    memset(sysex, 0, structSize);
    sysex->count = count;
    sysex->size = size;

    // prepare memory block which will contain the actual data
    if (!preAllocatedMem && size > 0) {
        preAllocatedMem = (UBYTE*) malloc(count*size);
        if (!preAllocatedMem) {
            free(sysex);
            return FALSE;
        }
        sysex->ownsLinearMem = 1;
    }
    sysex->linearMem = preAllocatedMem;
    handle->longBuffers = sysex;

    // set up headers
    dataPtr = preAllocatedMem;
    for (i=0; i<count; i++) {
        sysex->header[i].lpData = dataPtr;
        sysex->header[i].dwBufferLength = size;
        // user data is the index of the buffer
        sysex->header[i].dwUser = (DWORD) i;
        dataPtr += size;
    }
    return TRUE;
}

void MIDI_WinDestroyLongBufferQueue(MidiDeviceHandle* handle) {
    SysExQueue* sysex = (SysExQueue*) handle->longBuffers;
    if (sysex) {
        handle->longBuffers = NULL;
        if (sysex->ownsLinearMem && sysex->linearMem) {
            free(sysex->linearMem);
        }
        free(sysex);
    }
}

#endif // USE_PLATFORM_MIDI_IN || USE_PLATFORM_MIDI_OUT
