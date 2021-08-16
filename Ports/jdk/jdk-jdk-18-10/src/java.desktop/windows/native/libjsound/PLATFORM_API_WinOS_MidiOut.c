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

#define USE_ERROR
#define USE_TRACE

#include "PLATFORM_API_WinOS_Util.h"

/* include to prevent charset problem */
#include "PLATFORM_API_WinOS_Charset_Util.h"

#if USE_PLATFORM_MIDI_OUT == TRUE


#ifdef USE_ERROR
#include <stdio.h>

#define MIDIOUT_CHECK_ERROR  { \
        if (err != MMSYSERR_NOERROR) \
            ERROR3("MIDI OUT Error in %s:%d : %s\n", __FILE__, __LINE__, MIDI_OUT_GetErrorStr((INT32) err)); \
        }
#else
#define MIDIOUT_CHECK_ERROR
#endif

/* *************************** MidiOutDeviceProvider implementation *********************************** */

/* not thread safe */
static char winMidiOutErrMsg[WIN_MAX_ERROR_LEN];

char* MIDI_OUT_GetErrorStr(INT32 err) {
    winMidiOutErrMsg[0] = 0;
    midiOutGetErrorText((MMRESULT) err, winMidiOutErrMsg, WIN_MAX_ERROR_LEN);
    return winMidiOutErrMsg;
}

INT32 MIDI_OUT_GetNumDevices() {
    // add one for the MIDI_MAPPER
    // we want to return it first so it'll be the default, so we
    // decrement each deviceID for these methods....
    return (INT32) (midiOutGetNumDevs() + 1);
}


INT32 getMidiOutCaps(INT32 deviceID, MIDIOUTCAPSW* caps, INT32* err) {
    UINT_PTR id;
    if (deviceID == 0) {
        id = MIDI_MAPPER;
    } else {
        id = (UINT_PTR)(deviceID-1);
    }
    (*err) = (INT32) midiOutGetDevCapsW(id, caps, sizeof(MIDIOUTCAPSW));
    return ((*err) == MMSYSERR_NOERROR);
}


INT32 MIDI_OUT_GetDeviceName(INT32 deviceID, char *name, UINT32 nameLength) {
    MIDIOUTCAPSW midiOutCaps;
    INT32 err;

    memset(&midiOutCaps, 0, sizeof(midiOutCaps));
    if (getMidiOutCaps(deviceID, &midiOutCaps, &err)) {
        UnicodeToUTF8AndCopy(name, midiOutCaps.szPname, nameLength);
        return MIDI_SUCCESS;
    }
    MIDIOUT_CHECK_ERROR;
    return err;
}


INT32 MIDI_OUT_GetDeviceVendor(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_OUT_GetDeviceDescription(INT32 deviceID, char *name, UINT32 nameLength) {
    MIDIOUTCAPSW midiOutCaps;
    char *desc;
    INT32 err;

    memset(&midiOutCaps, 0, sizeof(midiOutCaps));
    if (getMidiOutCaps(deviceID, &midiOutCaps, &err)) {
        int tech = (int)midiOutCaps.wTechnology;
        switch(tech) {
        case MOD_MIDIPORT:
            desc = "External MIDI Port";
            break;
        case MOD_SQSYNTH:
            desc = "Internal square wave synthesizer";
            break;
        case MOD_FMSYNTH:
            desc = "Internal FM synthesizer";
            break;
        case MOD_SYNTH:
            desc = "Internal synthesizer (generic)";
            break;
        case MOD_MAPPER:
            desc = "Windows MIDI_MAPPER";
            break;
        case 7 /* MOD_SWSYNTH*/:
            desc = "Internal software synthesizer";
            break;
        default:
            return MIDI_NOT_SUPPORTED;
        }
        strncpy(name, desc, nameLength-1);
        name[nameLength-1] = 0;
        return MIDI_SUCCESS;
    }
    return err;
}


INT32 MIDI_OUT_GetDeviceVersion(INT32 deviceID, char *name, UINT32 nameLength) {
    MIDIOUTCAPSW midiOutCaps;
    INT32 err;

    memset(&midiOutCaps, 0, sizeof(midiOutCaps));
    if (getMidiOutCaps(deviceID, &midiOutCaps, &err) && nameLength>7) {
        sprintf(name, "%d.%d", (midiOutCaps.vDriverVersion & 0xFF00) >> 8, midiOutCaps.vDriverVersion & 0xFF);
        return MIDI_SUCCESS;
    }
    MIDIOUT_CHECK_ERROR;
    return err;
}


/* *************************** MidiOutDevice implementation ***************************************** */


INT32 unprepareLongBuffers(MidiDeviceHandle* handle) {
    SysExQueue* sysex;
    MMRESULT err = MMSYSERR_NOERROR;
    int i;

    if (!handle || !handle->deviceHandle || !handle->longBuffers) {
        ERROR0("MIDI_OUT_unprepareLongBuffers: handle, deviceHandle, or longBuffers == NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    sysex = (SysExQueue*) handle->longBuffers;
    for (i = 0; i<sysex->count; i++) {
        MIDIHDR* hdr = &(sysex->header[i]);
        if (hdr->dwFlags) {
            err = midiOutUnprepareHeader((HMIDIOUT) handle->deviceHandle, hdr, sizeof(MIDIHDR));
        }
    }
    MIDIOUT_CHECK_ERROR;
    return (INT32) err;
}

INT32 freeLongBuffer(MIDIHDR* hdr, HMIDIOUT deviceHandle, INT32 minToLeaveData) {
    MMRESULT err = MMSYSERR_NOERROR;

    if (!hdr) {
        ERROR0("MIDI_OUT_freeLongBuffer: hdr == NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    if (hdr->dwFlags && deviceHandle) {
        err = midiOutUnprepareHeader(deviceHandle, hdr, sizeof(MIDIHDR));
    }
    if (hdr->lpData && (((INT32) hdr->dwBufferLength) < minToLeaveData || minToLeaveData < 0)) {
        free(hdr->lpData);
        hdr->lpData=NULL;
        hdr->dwBufferLength=0;
    }
    hdr->dwBytesRecorded=0;
    hdr->dwFlags=0;
    return (INT32) err;
}

INT32 freeLongBuffers(MidiDeviceHandle* handle) {
    SysExQueue* sysex;
    MMRESULT err = MMSYSERR_NOERROR;
    int i;

    if (!handle || !handle->longBuffers) {
        ERROR0("MIDI_OUT_freeLongBuffers: handle or longBuffers == NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    sysex = (SysExQueue*) handle->longBuffers;
    for (i = 0; i<sysex->count; i++) {
        err = freeLongBuffer(&(sysex->header[i]), (HMIDIOUT) handle->deviceHandle, -1);
    }
    MIDIOUT_CHECK_ERROR;
    return (INT32) err;
}

INT32 MIDI_OUT_OpenDevice(INT32 deviceID, MidiDeviceHandle** handle) {
    MMRESULT err;

    TRACE1(">> MIDI_OUT_OpenDevice: deviceID: %d\n", deviceID);

    if (deviceID == 0) {
        deviceID = MIDI_MAPPER;
    } else {
        deviceID--;
    }
#ifdef USE_ERROR
    setvbuf(stdout, NULL, (int)_IONBF, 0);
    setvbuf(stderr, NULL, (int)_IONBF, 0);
#endif

    (*handle) = (MidiDeviceHandle*) malloc(sizeof(MidiDeviceHandle));
    if (!(*handle)) {
        ERROR0("ERROR: MIDI_OUT_OpenDevice: out of memory\n");
        return MIDI_OUT_OF_MEMORY;
    }
    memset(*handle, 0, sizeof(MidiDeviceHandle));

    // create long buffer queue
    if (!MIDI_WinCreateEmptyLongBufferQueue(*handle, MIDI_OUT_LONG_QUEUE_SIZE)) {
        ERROR0("ERROR: MIDI_OUT_OpenDevice: could not create long Buffers\n");
        free(*handle);
        (*handle) = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    // create notification event
    (*handle)->platformData = (void*) CreateEvent(NULL, FALSE /*manual reset*/, FALSE /*signaled*/, NULL);
    if (!(*handle)->platformData) {
        ERROR0("ERROR: MIDI_OUT_StartDevice: could not create event\n");
        MIDI_WinDestroyLongBufferQueue(*handle);
        free(*handle);
        (*handle) = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    // finally open the device
    err = midiOutOpen((HMIDIOUT*) &((*handle)->deviceHandle), deviceID,
                      (UINT_PTR) (*handle)->platformData, (UINT_PTR) (*handle), CALLBACK_EVENT);

    if ((err != MMSYSERR_NOERROR) || (!(*handle)->deviceHandle)) {
        /* some devices return non zero, but no error! */
        if (midiOutShortMsg((HMIDIOUT) ((*handle)->deviceHandle),0) == MMSYSERR_INVALHANDLE) {
            MIDIOUT_CHECK_ERROR;
            CloseHandle((HANDLE) (*handle)->platformData);
            MIDI_WinDestroyLongBufferQueue(*handle);
            free(*handle);
            (*handle) = NULL;
            return (INT32) err;
        }
    }
    //$$fb enable high resolution time
    timeBeginPeriod(1);
    MIDI_SetStartTime(*handle);
    TRACE0("<< MIDI_OUT_OpenDevice: succeeded\n");
    return MIDI_SUCCESS;
}

INT32 MIDI_OUT_CloseDevice(MidiDeviceHandle* handle) {
    MMRESULT err = MMSYSERR_NOERROR;
    HANDLE event;

    TRACE0("> MIDI_OUT_CloseDevice\n");
    if (!handle) {
        ERROR0("ERROR: MIDI_OUT_StopDevice: handle is NULL\n");
        return MIDI_INVALID_HANDLE; // failure
    }
    // encourage MIDI_OUT_SendLongMessage to return soon
    event = handle->platformData;
    handle->platformData = NULL;
    if (event) {
        SetEvent(event);
    } else {
        ERROR0("ERROR: MIDI_OUT_StopDevice: event is NULL\n");
    }

    if (handle->deviceHandle) {
        //$$fb disable high resolution time
        timeEndPeriod(1);
        err = midiOutReset((HMIDIOUT) handle->deviceHandle);
    } else {
        ERROR0("ERROR: MIDI_OUT_CloseDevice: deviceHandle is NULL\n");
    }

    // issue a "SUSTAIN OFF" message to each MIDI channel, 0 to 15.
    // "CONTROL CHANGE" is 176, "SUSTAIN CONTROLLER" is 64, and the value is 0.
    // $$fb 2002-04-04: It is responsability of the application developer to
    // leave the device in a consistent state. So I put this in comments
    /*
      for (channel = 0; channel < 16; channel++)
      MIDI_OUT_SendShortMessage(deviceHandle, (unsigned char)(176 + channel), (unsigned char)64, (unsigned char)0, (UINT32)-1);
    */

    if (event) {
        // wait until MIDI_OUT_SendLongMessage has finished
        while (handle->isWaiting) Sleep(0);
    }

    unprepareLongBuffers(handle);

    if (handle->deviceHandle) {
        err = midiOutClose((HMIDIOUT) handle->deviceHandle);
        MIDIOUT_CHECK_ERROR;
        handle->deviceHandle = NULL;
    }
    freeLongBuffers(handle);

    if (event) {
        CloseHandle(event);
    }
    MIDI_WinDestroyLongBufferQueue(handle);
    free(handle);

    TRACE0("< MIDI_OUT_CloseDevice\n");
    return (INT32) err;
}


/* return time stamp in microseconds */
INT64 MIDI_OUT_GetTimeStamp(MidiDeviceHandle* handle) {
    return MIDI_GetTimeStamp(handle);
}


INT32 MIDI_OUT_SendShortMessage(MidiDeviceHandle* handle, UINT32 packedMsg, UINT32 timestamp) {
    MMRESULT err = MMSYSERR_NOERROR;

    TRACE2("> MIDI_OUT_SendShortMessage %x, time: %d\n", packedMsg, timestamp);
    if (!handle) {
        ERROR0("ERROR: MIDI_OUT_SendShortMessage: handle is NULL\n");
        return MIDI_INVALID_HANDLE; // failure
    }
    err = midiOutShortMsg((HMIDIOUT) handle->deviceHandle, packedMsg);
    MIDIOUT_CHECK_ERROR;
    TRACE0("< MIDI_OUT_SendShortMessage\n");
    return (INT32) err;
}

INT32 MIDI_OUT_SendLongMessage(MidiDeviceHandle* handle, UBYTE* data, UINT32 size, UINT32 timestamp) {
    MMRESULT err;
    SysExQueue* sysex;
    MIDIHDR* hdr = NULL;
    INT32 remainingSize;
    int i;

    TRACE2("> MIDI_OUT_SendLongMessage size %d, time: %d\n", size, timestamp);
    if (!handle || !data || !handle->longBuffers) {
        ERROR0("< ERROR: MIDI_OUT_SendLongMessage: handle, data, or longBuffers is NULL\n");
        return MIDI_INVALID_HANDLE; // failure
    }
    if (size == 0) {
        return MIDI_SUCCESS;
    }

    sysex = (SysExQueue*) handle->longBuffers;
    remainingSize = size;

    // send in chunks of 512 bytes
    size = 512;
    while (remainingSize > 0) {
        if (remainingSize < (INT32) size) {
            size = (UINT32) remainingSize;
        }

        while (!hdr && handle->platformData) {
            /* find a non-queued header */
            for (i = 0; i < sysex->count; i++) {
                hdr = &(sysex->header[i]);
                if ((hdr->dwFlags & MHDR_DONE) || (hdr->dwFlags == 0)) {
                    break;
                }
                hdr = NULL;
            }
            /* wait for a buffer to free up */
            if (!hdr && handle->platformData) {
                DWORD res;
                TRACE0(" Need to wait for free buffer\n");
                handle->isWaiting = TRUE;
                res = WaitForSingleObject((HANDLE) handle->platformData, 700);
                handle->isWaiting = FALSE;
                if (res == WAIT_TIMEOUT) {
                    // break out back to Java if no buffer freed up after 700 milliseconds
                    TRACE0("-> TIMEOUT. Need to go back to Java\n");
                    break;
                }
            }
        }
        if (!hdr) {
            // no free buffer
            return MIDI_NOT_SUPPORTED;
        }

        TRACE2("-> sending %d bytes with buffer index=%d\n", (int) size, (int) hdr->dwUser);
        freeLongBuffer(hdr, handle->deviceHandle, (INT32) size);
        if (hdr->lpData == NULL) {
            hdr->lpData = malloc(size);
            hdr->dwBufferLength = size;
        }
        hdr->dwBytesRecorded = size;
        memcpy(hdr->lpData, data, size);
        err = midiOutPrepareHeader((HMIDIOUT) handle->deviceHandle, hdr, sizeof(MIDIHDR));
        if (err != MMSYSERR_NOERROR) {
            freeLongBuffer(hdr, handle->deviceHandle, -1);
            MIDIOUT_CHECK_ERROR;
            return (INT32) err;
        }
        err = midiOutLongMsg((HMIDIOUT) handle->deviceHandle, hdr, sizeof(MIDIHDR));
        if (err != MMSYSERR_NOERROR) {
            freeLongBuffer(hdr, handle->deviceHandle, -1);
            ERROR0("ERROR: MIDI_OUT_SendLongMessage: midiOutLongMsg returned error:\n");
            MIDIOUT_CHECK_ERROR;
            return (INT32) err;
        }
        remainingSize -= size;
        data += size;
    }
    TRACE0("< MIDI_OUT_SendLongMessage success\n");
    return MIDI_SUCCESS;
}

#endif // USE_PLATFORM_MIDI_OUT
