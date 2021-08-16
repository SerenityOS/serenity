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

/* include Java Sound specific headers as C code */
extern "C" {
#include "PLATFORM_API_WinOS_Util.h"
}

/* include to prevent charset problem */
#include "PLATFORM_API_WinOS_Charset_Util.h"

#if USE_PLATFORM_MIDI_IN == TRUE

#ifdef USE_ERROR
#include <stdio.h>

#define MIDIIN_CHECK_ERROR { \
        if (err != MMSYSERR_NOERROR) \
            ERROR3("MIDI IN Error in %s:%d : %s\n", __FILE__, __LINE__, MIDI_IN_GetErrorStr((INT32) err)); \
    }
#else
#define MIDIIN_CHECK_ERROR
#endif

/*
 * Callback from the MIDI device for all messages.
 */
//$$fb dwParam1 holds a pointer for long messages. How can that be a DWORD then ???
void CALLBACK MIDI_IN_PutMessage( HMIDIIN hMidiIn, UINT wMsg, UINT_PTR dwInstance, UINT_PTR dwParam1, UINT_PTR dwParam2 ) {

    MidiDeviceHandle* handle = (MidiDeviceHandle*) dwInstance;

    TRACE3("> MIDI_IN_PutMessage, hMidiIn: %x, wMsg: %x, dwInstance: %x\n", hMidiIn, wMsg, dwInstance);
    TRACE2("                      dwParam1: %x, dwParam2: %x\n", dwParam1, dwParam2);

    switch(wMsg) {

    case MIM_OPEN:
        TRACE0("< MIDI_IN_PutMessage: MIM_OPEN\n");
        break;

    case MIM_CLOSE:
        TRACE0("< MIDI_IN_PutMessage: MIM_CLOSE\n");
        break;

    case MIM_MOREDATA:
    case MIM_DATA:
        TRACE3("  MIDI_IN_PutMessage: MIM_MOREDATA or MIM_DATA. status=%x  data1=%x  data2=%x\n",
               dwParam1 & 0xFF, (dwParam1 & 0xFF00)>>8, (dwParam1 & 0xFF0000)>>16);
        if (handle!=NULL && handle->queue!=NULL && handle->platformData) {
            MIDI_QueueAddShort(handle->queue,
                               // queue stores packedMsg in big endian
                               //(dwParam1 << 24) | ((dwParam1 << 8) & 0xFF0000) | ((dwParam1 >> 8) & 0xFF00),
                               (UINT32) dwParam1,
                               // queue uses microseconds
                               ((INT64) dwParam2)*1000,
                               // overwrite if queue is full
                               TRUE);
            SetEvent((HANDLE) handle->platformData);
        }
        TRACE0("< MIDI_IN_PutMessage\n");
        break;

    case MIM_LONGDATA:
        TRACE1("  MIDI_IN_PutMessage: MIM_LONGDATA (%d bytes recorded)\n", (int) (((MIDIHDR*) dwParam1)->dwBytesRecorded));
        if (handle!=NULL && handle->queue!=NULL && handle->platformData) {
            MIDIHDR* hdr = (MIDIHDR*) dwParam1;
            TRACE2("  MIDI_IN_PutMessage: Adding to queue: index %d, %d bytes\n", (INT32) hdr->dwUser, hdr->dwBytesRecorded);
            MIDI_QueueAddLong(handle->queue,
                              (UBYTE*) hdr->lpData,
                              (UINT32) hdr->dwBytesRecorded,
                              // sysex buffer index
                              (INT32) hdr->dwUser,
                              // queue uses microseconds
                              ((INT64) dwParam2)*1000,
                              // overwrite if queue is full
                              TRUE);
            SetEvent((HANDLE) handle->platformData);
        }
        TRACE0("< MIDI_IN_PutMessage\n");
        break;

    case MIM_ERROR:
        ERROR0("< MIDI_IN_PutMessage: MIM_ERROR!\n");
        break;

    case MIM_LONGERROR:
        if (dwParam1 != 0) {
            MIDIHDR* hdr = (MIDIHDR*) dwParam1;
#ifdef USE_TRACE
            if (hdr->dwBytesRecorded > 0) {
                TRACE2("  MIDI_IN_PutMessage: MIM_LONGERROR! recorded: %d bytes with status 0x%2x\n",
                        hdr->dwBytesRecorded, (int) (*((UBYTE*) hdr->lpData)));
            }
#endif
            // re-add hdr to device query
            hdr->dwBytesRecorded = 0;
            midiInAddBuffer((HMIDIIN)handle->deviceHandle, hdr, sizeof(MIDIHDR));
        }
        ERROR0("< MIDI_IN_PutMessage: MIM_LONGERROR!\n");
        break;

    default:
        ERROR1("< MIDI_IN_PutMessage: ERROR unknown message %d!\n", wMsg);
        break;

    } // switch (wMsg)
}


/*
** data/routines for opening MIDI input (MidiIn) device by separate thread
** (joint into MidiIn_OpenHelper class)
** see 6415669 - MidiIn device stops work and crushes JVM after exiting
** from thread that has open the device (it looks like WinMM bug).
*/
class MidiIn_OpenHelper {
public:
    /* opens MidiIn device  */
    static MMRESULT midiInOpen(INT32 deviceID, MidiDeviceHandle* handle);
    /* checks for initialization success */
    static inline BOOL isInitialized() { return data.threadHandle != NULL; }
protected:
    MidiIn_OpenHelper() {}  // no need to create an instance

    /* data class */
    class Data {
    public:
        Data();
        ~Data();
        // public data to access from parent class
        CRITICAL_SECTION crit_sect;
        volatile HANDLE threadHandle;
        volatile HANDLE doEvent;    // event to resume thread
        volatile HANDLE doneEvent;  // processing has been completed
        volatile MMRESULT err;      // processing result
        // data to process; (handle == null) is command to thread terminating
        volatile INT32 deviceID;
        volatile MidiDeviceHandle* handle;
    } static data;

    /* StartThread function */
    static DWORD WINAPI __stdcall ThreadProc(void *param);
};

/* MidiIn_OpenHelper class implementation
*/
MidiIn_OpenHelper::Data MidiIn_OpenHelper::data;

MidiIn_OpenHelper::Data::Data() {
    threadHandle = NULL;
    ::InitializeCriticalSection(&crit_sect);
    doEvent = ::CreateEvent(NULL, FALSE, FALSE, NULL);
    doneEvent = ::CreateEvent(NULL, FALSE, FALSE, NULL);
    if (doEvent != NULL && doneEvent != NULL)
        threadHandle = ::CreateThread(NULL, 0, ThreadProc, NULL, 0, NULL);
}

MidiIn_OpenHelper::Data::~Data() {
    ::EnterCriticalSection(&crit_sect);
    if (threadHandle != NULL) {
        // terminate thread
        handle = NULL;
        ::SetEvent(doEvent);
        ::CloseHandle(threadHandle);
        threadHandle = NULL;
    }
    ::LeaveCriticalSection(&crit_sect);
    // won't delete doEvent/doneEvent/crit_sect
    // - Windows will do during process shutdown
}

DWORD WINAPI __stdcall MidiIn_OpenHelper::ThreadProc(void *param) {
    while (1) {
        // wait for something to do
        ::WaitForSingleObject(data.doEvent, INFINITE);
        if (data.handle == NULL) {
            // (data.handle == NULL) is a signal to terminate thread
            break;
        }

        data.err = ::midiInOpen((HMIDIIN*)&(data.handle->deviceHandle),
                                data.deviceID, (UINT_PTR)&(MIDI_IN_PutMessage),
                                (UINT_PTR)data.handle,
                                CALLBACK_FUNCTION|MIDI_IO_STATUS);

        ::SetEvent(data.doneEvent);
    }
    return 0;
}

MMRESULT MidiIn_OpenHelper::midiInOpen(INT32 deviceID, MidiDeviceHandle* handle) {
    MMRESULT err;
    ::EnterCriticalSection(&data.crit_sect);
    if (!isInitialized()) {
        ::LeaveCriticalSection(&data.crit_sect);
        return MMSYSERR_ERROR;
    }
    data.deviceID = deviceID;
    data.handle = handle;
    ::SetEvent(data.doEvent);
    ::WaitForSingleObject(data.doneEvent, INFINITE);
    err = data.err;
    ::LeaveCriticalSection(&data.crit_sect);
    return err;
}


// PLATFORM_MIDI_IN method implementations

/* not thread safe */
static char winMidiInErrMsg[WIN_MAX_ERROR_LEN];

char* MIDI_IN_GetErrorStr(INT32 err) {
    winMidiInErrMsg[0] = 0;
    midiInGetErrorText((MMRESULT) err, winMidiInErrMsg, WIN_MAX_ERROR_LEN);
    return winMidiInErrMsg;
}

INT32 MIDI_IN_GetNumDevices() {
    return (INT32) midiInGetNumDevs();
}

INT32 getMidiInCaps(INT32 deviceID, MIDIINCAPSW* caps, INT32* err) {
    (*err) = midiInGetDevCapsW(deviceID, caps, sizeof(MIDIINCAPSW));
    return ((*err) == MMSYSERR_NOERROR);
}

INT32 MIDI_IN_GetDeviceName(INT32 deviceID, char *name, UINT32 nameLength) {
    MIDIINCAPSW midiInCaps;
    INT32 err;

    memset(&midiInCaps, 0, sizeof(midiInCaps));
    if (getMidiInCaps(deviceID, &midiInCaps, &err)) {
        UnicodeToUTF8AndCopy(name, midiInCaps.szPname, nameLength);
        return MIDI_SUCCESS;
    }
    MIDIIN_CHECK_ERROR;
    return err;
}


INT32 MIDI_IN_GetDeviceVendor(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}


INT32 MIDI_IN_GetDeviceDescription(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_NOT_SUPPORTED;
}



INT32 MIDI_IN_GetDeviceVersion(INT32 deviceID, char *name, UINT32 nameLength) {
    MIDIINCAPSW midiInCaps;
    INT32 err = MIDI_NOT_SUPPORTED;

    memset(&midiInCaps, 0, sizeof(midiInCaps));
    if (getMidiInCaps(deviceID, &midiInCaps, &err) && (nameLength>7)) {
        sprintf(name, "%d.%d", (midiInCaps.vDriverVersion & 0xFF00) >> 8, midiInCaps.vDriverVersion & 0xFF);
        return MIDI_SUCCESS;
    }
    MIDIIN_CHECK_ERROR;
    return err;
}


INT32 prepareBuffers(MidiDeviceHandle* handle) {
    SysExQueue* sysex;
    MMRESULT err = MMSYSERR_NOERROR;
    int i;

    if (!handle || !handle->longBuffers || !handle->deviceHandle) {
        ERROR0("MIDI_IN_prepareBuffers: handle, or longBuffers, or deviceHandle==NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    sysex = (SysExQueue*) handle->longBuffers;
    for (i = 0; i<sysex->count; i++) {
        MIDIHDR* hdr = &(sysex->header[i]);
        midiInPrepareHeader((HMIDIIN) handle->deviceHandle, hdr, sizeof(MIDIHDR));
        err = midiInAddBuffer((HMIDIIN) handle->deviceHandle, hdr, sizeof(MIDIHDR));
    }
    MIDIIN_CHECK_ERROR;
    return (INT32) err;
}

INT32 unprepareBuffers(MidiDeviceHandle* handle) {
    SysExQueue* sysex;
    MMRESULT err = MMSYSERR_NOERROR;
    int i;

    if (!handle || !handle->longBuffers || !handle->deviceHandle) {
        ERROR0("MIDI_IN_unprepareBuffers: handle, or longBuffers, or deviceHandle==NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    sysex = (SysExQueue*) handle->longBuffers;
    for (i = 0; i<sysex->count; i++) {
        err = midiInUnprepareHeader((HMIDIIN) handle->deviceHandle, &(sysex->header[i]), sizeof(MIDIHDR));
    }
    MIDIIN_CHECK_ERROR;
    return (INT32) err;
}

INT32 MIDI_IN_OpenDevice(INT32 deviceID, MidiDeviceHandle** handle) {
    MMRESULT err;

    TRACE0("> MIDI_IN_OpenDevice\n");
#ifdef USE_ERROR
    setvbuf(stdout, NULL, (int)_IONBF, 0);
    setvbuf(stderr, NULL, (int)_IONBF, 0);
#endif

    (*handle) = (MidiDeviceHandle*) malloc(sizeof(MidiDeviceHandle));
    if (!(*handle)) {
        ERROR0("< ERROR: MIDI_IN_OpenDevice: out of memory\n");
        return MIDI_OUT_OF_MEMORY;
    }
    memset(*handle, 0, sizeof(MidiDeviceHandle));

    // create queue
    (*handle)->queue = MIDI_CreateQueue(MIDI_IN_MESSAGE_QUEUE_SIZE);
    if (!(*handle)->queue) {
        ERROR0("< ERROR: MIDI_IN_OpenDevice: could not create queue\n");
        free(*handle);
        (*handle) = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    // create long buffer queue
    if (!MIDI_WinCreateLongBufferQueue(*handle, MIDI_IN_LONG_QUEUE_SIZE, MIDI_IN_LONG_MESSAGE_SIZE, NULL)) {
        ERROR0("< ERROR: MIDI_IN_OpenDevice: could not create long Buffers\n");
        MIDI_DestroyQueue((*handle)->queue);
        free(*handle);
        (*handle) = NULL;
        return MIDI_OUT_OF_MEMORY;
    }

    // finally open the device
    err = MidiIn_OpenHelper::midiInOpen(deviceID, *handle);

    if ((err != MMSYSERR_NOERROR) || (!(*handle)->deviceHandle)) {
        MIDIIN_CHECK_ERROR;
        MIDI_WinDestroyLongBufferQueue(*handle);
        MIDI_DestroyQueue((*handle)->queue);
        free(*handle);
        (*handle) = NULL;
        return (INT32) err;
    }

    prepareBuffers(*handle);
        MIDI_SetStartTime(*handle);
    TRACE0("< MIDI_IN_OpenDevice: midiInOpen succeeded\n");
    return MIDI_SUCCESS;
}


INT32 MIDI_IN_CloseDevice(MidiDeviceHandle* handle) {
    MMRESULT err;

    TRACE0("> MIDI_IN_CloseDevice: midiInClose\n");
    if (!handle) {
        ERROR0("ERROR: MIDI_IN_CloseDevice: handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    midiInReset((HMIDIIN) handle->deviceHandle);
    unprepareBuffers(handle);
    err = midiInClose((HMIDIIN) handle->deviceHandle);
    handle->deviceHandle=NULL;
    MIDIIN_CHECK_ERROR;
    MIDI_WinDestroyLongBufferQueue(handle);

    if (handle->queue!=NULL) {
        MidiMessageQueue* queue = handle->queue;
        handle->queue = NULL;
        MIDI_DestroyQueue(queue);
    }
    free(handle);

    TRACE0("< MIDI_IN_CloseDevice: midiInClose succeeded\n");
    return (INT32) err;
}


INT32 MIDI_IN_StartDevice(MidiDeviceHandle* handle) {
    MMRESULT err;

    if (!handle || !handle->deviceHandle || !handle->queue) {
        ERROR0("ERROR: MIDI_IN_StartDevice: handle or queue is NULL\n");
        return MIDI_INVALID_HANDLE;
    }

    // clear all the events from the queue
    MIDI_QueueClear(handle->queue);

    handle->platformData = (void*) CreateEvent(NULL, FALSE /*manual reset*/, FALSE /*signaled*/, NULL);
    if (!handle->platformData) {
        ERROR0("ERROR: MIDI_IN_StartDevice: could not create event\n");
        return MIDI_OUT_OF_MEMORY;
    }

    err = midiInStart((HMIDIIN) handle->deviceHandle);
        /* $$mp 200308-11: This method is already called in ...open(). It is
           unclear why it is called again. The specification says that
           MidiDevice.getMicrosecondPosition() returns the time since the
           device was opened (the spec doesn't know about start/stop).
           So I guess this call is obsolete. */
        MIDI_SetStartTime(handle);

    MIDIIN_CHECK_ERROR;
    TRACE0("MIDI_IN_StartDevice: midiInStart finished\n");
    return (INT32) err;
}


INT32 MIDI_IN_StopDevice(MidiDeviceHandle* handle) {
    MMRESULT err;
    HANDLE event;

    TRACE0("> MIDI_IN_StopDevice: midiInStop \n");
    if (!handle || !handle->platformData) {
        ERROR0("ERROR: MIDI_IN_StopDevice: handle or event is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    // encourage MIDI_IN_GetMessage to return soon
    event = handle->platformData;
    handle->platformData = NULL;
    SetEvent(event);

    err = midiInStop((HMIDIIN) handle->deviceHandle);

    // wait until the Java thread has exited
    while (handle->isWaiting) Sleep(0);
    CloseHandle(event);

    MIDIIN_CHECK_ERROR;
    TRACE0("< MIDI_IN_StopDevice: midiInStop finished\n");
    return (INT32) err;
}


/* return time stamp in microseconds */
INT64 MIDI_IN_GetTimeStamp(MidiDeviceHandle* handle) {
        return MIDI_GetTimeStamp(handle);
}


// read the next message from the queue
MidiMessage* MIDI_IN_GetMessage(MidiDeviceHandle* handle) {
    if (handle == NULL) {
        return NULL;
    }
    while (handle->queue!=NULL && handle->platformData!=NULL) {
        MidiMessage* msg = MIDI_QueueRead(handle->queue);
        DWORD res;
        if (msg != NULL) {
            //fprintf(stdout, "GetMessage returns index %d\n", msg->data.l.index); fflush(stdout);
            return msg;
        }
        TRACE0("MIDI_IN_GetMessage: before waiting\n");
        handle->isWaiting = TRUE;
        res = WaitForSingleObject((HANDLE) handle->platformData, 2000);
        handle->isWaiting = FALSE;
        if (res == WAIT_TIMEOUT) {
            // break out back to Java from time to time - just to be sure
            TRACE0("MIDI_IN_GetMessage: waiting finished with timeout\n");
            break;
        }
        TRACE0("MIDI_IN_GetMessage: waiting finished\n");
    }
    return NULL;
}

void MIDI_IN_ReleaseMessage(MidiDeviceHandle* handle, MidiMessage* msg) {
    SysExQueue* sysex;
    if (handle == NULL || handle->queue == NULL) {
        return;
    }
    sysex = (SysExQueue*) handle->longBuffers;
    if (msg->type == LONG_MESSAGE && sysex) {
        MIDIHDR* hdr = &(sysex->header[msg->data.l.index]);
        //fprintf(stdout, "ReleaseMessage index %d\n", msg->data.l.index); fflush(stdout);
        hdr->dwBytesRecorded = 0;
        midiInAddBuffer((HMIDIIN) handle->deviceHandle, hdr, sizeof(MIDIHDR));
    }
    MIDI_QueueRemove(handle->queue, TRUE /*onlyLocked*/);
}

#endif // USE_PLATFORM_MIDI_IN
