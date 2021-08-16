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

#ifndef WIN32_EXTRA_LEAN
#define WIN32_EXTRA_LEAN
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>
#include <mmsystem.h>

/* for waveformat extensible */
#include <mmreg.h>
#include <ks.h>

#ifndef PLATFORM_API_WINOS_UTIL_INCLUDED
#define PLATFORM_API_WINOS_UTIL_INCLUDED

#define WIN_MAX_ERROR_LEN 200

#if (USE_PLATFORM_MIDI_IN == TRUE) || (USE_PLATFORM_MIDI_OUT == TRUE)

#include "PlatformMidi.h"

typedef struct tag_SysExQueue {
    int count;         // number of sys ex headers
    int size;          // data size per sys ex header
    int ownsLinearMem; // true when linearMem is to be disposed
    UBYTE* linearMem;  // where the actual sys ex data is, count*size bytes
    MIDIHDR header[1]; // Windows specific structure to hold meta info
} SysExQueue;

/* set the startTime field in MidiDeviceHandle */
void MIDI_SetStartTime(MidiDeviceHandle* handle);

/* return time stamp in microseconds */
INT64 MIDI_GetTimeStamp(MidiDeviceHandle* handle);

// the buffers do not contain memory
int MIDI_WinCreateEmptyLongBufferQueue(MidiDeviceHandle* handle, int count);
int MIDI_WinCreateLongBufferQueue(MidiDeviceHandle* handle, int count, int size, UBYTE* preAllocatedMem);
void MIDI_WinDestroyLongBufferQueue(MidiDeviceHandle* handle);

#endif // USE_PLATFORM_MIDI_IN || USE_PLATFORM_MIDI_OUT

#endif // PLATFORM_API_WINOS_UTIL_INCLUDED
