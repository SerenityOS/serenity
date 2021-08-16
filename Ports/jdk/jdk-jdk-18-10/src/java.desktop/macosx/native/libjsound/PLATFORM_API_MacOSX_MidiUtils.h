/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

#if (USE_PLATFORM_MIDI_IN == TRUE) || (USE_PLATFORM_MIDI_OUT == TRUE)

#include "PlatformMidi.h"                  // JavaSound header for platform midi support.
#include <CoreMIDI/CoreMIDI.h>             // Umbrella header for the CoreMIDI framework.
#include <CoreAudio/CoreAudio.h>           // This provides access to the host's time base and translations to nanoseconds.
#include <CoreFoundation/CoreFoundation.h> // CFDataRef.

/* for memcpy */
#include <string.h>
/* for malloc */
#include <stdlib.h>
/* for usleep */
#include <unistd.h>

#ifdef USE_ERROR
#include <stdio.h>
#endif

#define MIDI_ERROR_NONE MIDI_SUCCESS

#ifdef USE_ERROR
#define MIDI_CHECK_ERROR  { if (err != MIDI_ERROR_NONE) MIDI_Utils_PrintError(err); }
#else
#define MIDI_CHECK_ERROR
#endif

typedef struct {
    MidiDeviceHandle h;                  /* the real handle (must be the first field!) */
    int direction;                       /* direction of the endpoint */
    int deviceID;                        /* logical index (0 .. numEndpoints-1) */
    int isStarted;                       /* whether device is "started" */
    MIDIPortRef port;                    /* input or output port associated with the endpoint */
    CFMutableDataRef readingSysExData;   /* Non-Null: in the middle of reading SysEx data; Null: otherwise */
} MacMidiDeviceHandle;

extern const char* MIDI_Utils_GetErrorMsg(int err);
extern void MIDI_Utils_PrintError(int err);

// A MIDI endpoint represents a source or a destination for a standard 16-channel MIDI data stream.
enum {
    MIDI_IN = 0, // source
    MIDI_OUT = 1 // destination
};

// The parameter "direction" is either MIDI_IN or MIDI_OUT.
// Declarations of functions required by the JavaSound MIDI Porting layer.

extern INT32 MIDI_Utils_GetNumDevices(int direction);
extern INT32 MIDI_Utils_GetDeviceName(int direction, INT32 deviceID, char *name, UINT32 nameLength);
extern INT32 MIDI_Utils_GetDeviceVendor(int direction, INT32 deviceID, char *name, UINT32 nameLength);
extern INT32 MIDI_Utils_GetDeviceDescription(int direction, INT32 deviceID, char *name, UINT32 nameLength);
extern INT32 MIDI_Utils_GetDeviceVersion(int direction, INT32 deviceID, char *name, UINT32 nameLength);
extern INT32 MIDI_Utils_OpenDevice(int direction, INT32 deviceID, MacMidiDeviceHandle** handle,
                   int num_msgs, int num_long_msgs,
                   size_t lm_size);
extern INT32 MIDI_Utils_CloseDevice(MacMidiDeviceHandle* handle);
extern INT32 MIDI_Utils_StartDevice(MacMidiDeviceHandle* handle);
extern INT32 MIDI_Utils_StopDevice(MacMidiDeviceHandle* handle);
extern INT64 MIDI_Utils_GetTimeStamp(MacMidiDeviceHandle* handle);

// Mac OS X port for locking and synchronization.

extern void* MIDI_CreateConditionVariable();
extern void  MIDI_DestroyConditionVariable(void* cond);
extern void  MIDI_WaitOnConditionVariable(void* cond, void* lock);
extern void  MIDI_SignalConditionVariable(void* cond);

#endif // USE_PLATFORM_MIDI_IN || USE_PLATFORM_MIDI_OUT
