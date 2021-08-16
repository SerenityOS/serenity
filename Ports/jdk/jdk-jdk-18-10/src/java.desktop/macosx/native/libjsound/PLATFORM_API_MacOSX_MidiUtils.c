/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
**
**    Overview:
**      Implementation of the functions used for both MIDI in and MIDI out.
**
**      Java package com.sun.media.sound defines the AbstractMidiDevice class
**      which encapsulates functionalities shared by both MidiInDevice and
**      MidiOutDevice classes in the same package.
**
**      The Java layer classes MidiInDevice and MidiOutDevice in turn map to
**      the MIDIEndpointRef data type in the CoreMIDI framework, which
**      represents a source or destination for a standard 16-channel MIDI data
**      stream.
*/
/*****************************************************************************/

//#define USE_ERROR
//#define USE_TRACE

#if (USE_PLATFORM_MIDI_IN == TRUE) || (USE_PLATFORM_MIDI_OUT == TRUE)

#include "PLATFORM_API_MacOSX_MidiUtils.h"
#include <pthread.h>
#include <assert.h>

// Constant character string definitions of CoreMIDI's corresponding error codes.

static const char* strMIDIInvalidClient =
                        "An invalid MIDIClientRef was passed.";
static const char* strMIDIInvalidPort =
                        "An invalid MIDIPortRef was passed.";
static const char* strMIDIWrongEndpointType =
                        "A source endpoint was passed to a function expecting a destination, or vice versa.";
static const char* strMIDINoConnection =
                        "Attempt to close a non-existant connection.";
static const char* strMIDIUnknownEndpoint =
                        "An invalid MIDIEndpointRef was passed.";
static const char* strMIDIUnknownProperty =
                        "Attempt to query a property not set on the object.";
static const char* strMIDIWrongPropertyType =
                        "Attempt to set a property with a value not of the correct type.";
static const char* strMIDINoCurrentSetup =
                        "Internal error; there is no current MIDI setup object.";
static const char* strMIDIMessageSendErr =
                        "Communication with MIDIServer failed.";
static const char* strMIDIServerStartErr =
                        "Unable to start MIDIServer.";
static const char* strMIDISetupFormatErr =
                        "Unable to read the saved state.";
static const char* strMIDIWrongThread =
                        "A driver is calling a non-I/O function in the server from a thread other than"
                        "the server's main thread.";
static const char* strMIDIObjectNotFound =
                        "The requested object does not exist.";
static const char* strMIDIIDNotUnique =
                        "Attempt to set a non-unique kMIDIPropertyUniqueID on an object.";

static const char* midi_strerror(int err) {
/*
    @enum           Error Constants
    @abstract       The error constants unique to Core MIDI.
    @discussion     These are the error constants that are unique to Core MIDI. Note that Core MIDI
                    functions may return other codes that are not listed here.
*/
    const char* strerr;

    switch (err) {
    case kMIDIInvalidClient:
        strerr = strMIDIInvalidClient;
        break;
    case kMIDIInvalidPort:
        strerr = strMIDIInvalidPort;
        break;
    case kMIDIWrongEndpointType:
        strerr = strMIDIWrongEndpointType;
        break;
    case kMIDINoConnection:
        strerr = strMIDINoConnection;
        break;
    case kMIDIUnknownEndpoint:
        strerr = strMIDIUnknownEndpoint;
        break;
    case kMIDIUnknownProperty:
        strerr = strMIDIUnknownProperty;
        break;
    case kMIDIWrongPropertyType:
        strerr = strMIDIWrongPropertyType;
        break;
    case kMIDINoCurrentSetup:
        strerr = strMIDINoCurrentSetup;
        break;
    case kMIDIMessageSendErr:
        strerr = strMIDIMessageSendErr;
        break;
    case kMIDIServerStartErr:
        strerr = strMIDIServerStartErr;
        break;
    case kMIDISetupFormatErr:
        strerr = strMIDISetupFormatErr;
        break;
    case kMIDIWrongThread:
        strerr = strMIDIWrongThread;
        break;
    case kMIDIObjectNotFound:
        strerr = strMIDIObjectNotFound;
        break;
    case kMIDIIDNotUnique:
        strerr = strMIDIIDNotUnique;
        break;
    default:
        strerr = "Unknown error.";
        break;
    }
    return strerr;
}

const char* MIDI_Utils_GetErrorMsg(int err) {
    return midi_strerror(err);
}


void MIDI_Utils_PrintError(int err) {
#ifdef USE_ERROR
    const char* s = MIDI_Utils_GetErrorMsg(err);
    if (s != NULL) {
        fprintf(stderr, "%s\n", s);
    }
#endif
}


// Note direction is either MIDI_IN or MIDI_OUT.
INT32 MIDI_Utils_GetNumDevices(int direction) {
    int num_endpoints;
    if (direction == MIDI_IN) {
        num_endpoints = MIDIGetNumberOfSources();
    //fprintf(stdout, "MIDIGetNumberOfSources() returns %d\n", num_endpoints);
    } else if (direction == MIDI_OUT) {
        num_endpoints = MIDIGetNumberOfDestinations();
        //printf(stdout, "MIDIGetNumberOfDestinations() returns %d\n", num_endpoints);
    } else {
        assert((direction == MIDI_IN || direction == MIDI_OUT));
        num_endpoints = 0;
    }
    return (INT32) num_endpoints;
}

// Wraps calls to CFStringGetCStringPtr and CFStringGetCString to make sure
// we extract the c characters into the buffer and null-terminate it.
static void CFStringExtractCString(CFStringRef cfs, char* buffer, UINT32 bufferSize, CFStringEncoding encoding) {
    const char* ptr = CFStringGetCStringPtr(cfs, encoding);
    if (ptr) {
        strlcpy(buffer, ptr, bufferSize);
    } else {
        if (! CFStringGetCString(cfs, buffer, bufferSize, encoding)) {
            // There's an error in conversion, make sure we null-terminate the buffer.
            buffer[bufferSize - 1] = '\0';
        }
    }
}

//
// @see com.sun.media.sound.AbstractMidiDeviceProvider.getDeviceInfo().
static int getEndpointProperty(int direction, INT32 deviceID, char *buffer, int bufferLength, CFStringRef propertyID) {

    if (deviceID < 0) {
        return MIDI_INVALID_DEVICEID;
    }

    MIDIEndpointRef endpoint;

    if (direction == MIDI_IN) {
        endpoint = MIDIGetSource(deviceID);
    } else if (direction == MIDI_OUT) {
        endpoint = MIDIGetDestination(deviceID);
    } else {
        return MIDI_INVALID_ARGUMENT;
    }

    if (!endpoint) {
        return MIDI_INVALID_DEVICEID;
    }

    int status = MIDI_SUCCESS;
    if (propertyID == kMIDIPropertyDriverVersion) {
        SInt32 driverVersion;
        status = MIDIObjectGetIntegerProperty(endpoint, kMIDIPropertyDriverVersion, &driverVersion);
        if (status != MIDI_SUCCESS) return status;
        snprintf(buffer,
                 bufferLength,
                 "%d",
                 (int) driverVersion);
    }
    else {
        CFStringRef pname;
        status = MIDIObjectGetStringProperty(endpoint, propertyID, &pname);
        if (status != MIDI_SUCCESS) return status;
        CFStringExtractCString(pname, buffer, bufferLength, 0);
    }
    return MIDI_ERROR_NONE;
}

// A simple utility which encapsulates CoreAudio's HostTime APIs.
// It returns the current host time in nanoseconds which when subtracted from
// a previous getCurrentTimeInNanos() result produces the delta in nanos.
static UInt64 getCurrentTimeInNanos() {
    UInt64 hostTime = AudioGetCurrentHostTime();
    UInt64 nanos = AudioConvertHostTimeToNanos(hostTime);
    return nanos;
}


INT32 MIDI_Utils_GetDeviceName(int direction, INT32 deviceID, char *name, UINT32 bufferLength) {
    return getEndpointProperty(direction, deviceID, name, bufferLength, kMIDIPropertyName);
}


INT32 MIDI_Utils_GetDeviceVendor(int direction, INT32 deviceID, char *name, UINT32 bufferLength) {
    return getEndpointProperty(direction, deviceID, name, bufferLength, kMIDIPropertyManufacturer);
}


INT32 MIDI_Utils_GetDeviceDescription(int direction, INT32 deviceID, char *name, UINT32 bufferLength) {
    return getEndpointProperty(direction, deviceID, name, bufferLength, kMIDIPropertyDisplayName);
}


INT32 MIDI_Utils_GetDeviceVersion(int direction, INT32 deviceID, char *name, UINT32 bufferLength) {
    return getEndpointProperty(direction, deviceID, name, bufferLength, kMIDIPropertyDriverVersion);
}


static MIDIClientRef client = (MIDIClientRef) 0;
static MIDIPortRef inPort = (MIDIPortRef) 0;
static MIDIPortRef outPort = (MIDIPortRef) 0;

// Each MIDIPacket can contain more than one midi messages.
// This function processes the packet and adds the messages to the specified message queue.
// @see also src/share/native/com/sun/media/sound/PlatformMidi.h.
static void processMessagesForPacket(const MIDIPacket* packet, MacMidiDeviceHandle* handle) {
    const UInt8* data;
    UInt16 length;
    UInt8 byte;
    UInt8 pendingMessageStatus;
    UInt8 pendingData[2];
    UInt16 pendingDataIndex, pendingDataLength;
    UINT32 packedMsg;
    MIDITimeStamp ts = packet->timeStamp;

    pendingMessageStatus = 0;
    pendingDataIndex = pendingDataLength = 0;

    data = packet->data;
    length = packet->length;
    while (length--) {
        bool byteIsInvalid = FALSE;

        byte = *data++;
        packedMsg = byte;

        if (byte >= 0xF8) {
            // Each RealTime Category message (ie, Status of 0xF8 to 0xFF) consists of only 1 byte, the Status.
            // Except that 0xFD is an invalid status code.
            //
            // 0xF8 -> Midi clock
            // 0xF9 -> Midi tick
            // 0xFA -> Midi start
            // 0xFB -> Midi continue
            // 0xFC -> Midi stop
            // 0xFE -> Active sense
            // 0xFF -> Reset
            if (byte == 0xFD) {
                byteIsInvalid = TRUE;
            } else {
                pendingDataLength = 0;
            }
        } else {
            if (byte < 0x80) {
                // Not a status byte -- check our history.
                if (handle->readingSysExData) {
                    CFDataAppendBytes(handle->readingSysExData, &byte, 1);

                } else if (pendingDataIndex < pendingDataLength) {
                    pendingData[pendingDataIndex] = byte;
                    pendingDataIndex++;

                    if (pendingDataIndex == pendingDataLength) {
                        // This message is now done -- do the final processing.
                        if (pendingDataLength == 2) {
                            packedMsg = pendingMessageStatus | pendingData[0] << 8 | pendingData[1] << 16;
                        } else if (pendingDataLength == 1) {
                            packedMsg = pendingMessageStatus | pendingData[0] << 8;
                        } else {
                            fprintf(stderr, "%s: %d->internal error: pendingMessageStatus=0x%X, pendingDataLength=%d\n",
                                    __FILE__, __LINE__, pendingMessageStatus, pendingDataLength);
                            byteIsInvalid = TRUE;
                        }
                        pendingDataLength = 0;
                    }
                } else {
                    // Skip this byte -- it is invalid.
                    byteIsInvalid = TRUE;
                }
            } else {
                if (handle->readingSysExData /* && (byte == 0xF7) */) {
                    // We have reached the end of system exclusive message -- send it finally.
                    const UInt8* bytes = CFDataGetBytePtr(handle->readingSysExData);
                    CFIndex size = CFDataGetLength(handle->readingSysExData);
                    MIDI_QueueAddLong(handle->h.queue,
                                      (UBYTE*) bytes,
                                      (UINT32) size,
                                      0, // Don't care, windowish porting only.
                                      (INT64) (AudioConvertHostTimeToNanos(ts) + 500) / 1000,
                                      TRUE);
                    CFRelease(handle->readingSysExData);
                    handle->readingSysExData = NULL;
                }

                pendingMessageStatus = byte;
                pendingDataLength = 0;
                pendingDataIndex = 0;

                switch (byte & 0xF0) {
                    case 0x80:    // Note off
                    case 0x90:    // Note on
                    case 0xA0:    // Aftertouch
                    case 0xB0:    // Controller
                    case 0xE0:    // Pitch wheel
                        pendingDataLength = 2;
                        break;

                    case 0xC0:    // Program change
                    case 0xD0:    // Channel pressure
                        pendingDataLength = 1;
                        break;

                    case 0xF0: {
                        // System common message
                        switch (byte) {
                        case 0xF0:
                            // System exclusive
                            // Allocates a CFMutableData reference to accumulate the SysEx data until EOX (0xF7) is reached.
                            handle->readingSysExData = CFDataCreateMutable(NULL, 0);
                            break;

                        case 0xF7:
                            // System exclusive ends--already handled above.
                            // But if this is showing up outside of sysex, it's invalid.
                            byteIsInvalid = TRUE;
                            break;

                        case 0xF1:    // MTC quarter frame message
                        case 0xF3:    // Song select
                            pendingDataLength = 1;
                            break;

                        case 0xF2:    // Song position pointer
                            pendingDataLength = 2;
                            break;

                        case 0xF6:    // Tune request
                            pendingDataLength = 0;
                            break;

                        default:
                            // Invalid message
                            byteIsInvalid = TRUE;
                            break;
                        }
                        break;
                    }

                    default:
                        // This can't happen, but handle it anyway.
                        byteIsInvalid = TRUE;
                        break;
                }
            }
        }
        if (byteIsInvalid) continue;

        // If the byte is valid and pendingDataLength is 0, we are ready to send the message.
        if (pendingDataLength == 0) {
            MIDI_QueueAddShort(handle->h.queue, packedMsg, (INT64) (AudioConvertHostTimeToNanos(ts) + 500) / 1000, TRUE);
        }
    }
}

static void midiReadProc(const MIDIPacketList* packetList, void* refCon, void* connRefCon) {
    unsigned int i;
    const MIDIPacket* packet;
    MacMidiDeviceHandle* handle = (MacMidiDeviceHandle*) connRefCon;

    packet = packetList->packet;
    for (i = 0; i < packetList->numPackets; ++i) {
        processMessagesForPacket(packet, handle);
        packet = MIDIPacketNext(packet);
    }

    // Notify the waiting thread that there's data available.
    if (handle) {
        MIDI_SignalConditionVariable(handle->h.platformData);
    }
}

static void midiInit() {
    if (client) {
        return;
    }

    OSStatus err = noErr;

    err = MIDIClientCreate(CFSTR("MIDI Client"), NULL, NULL, &client);
    if (err != noErr) { goto Exit; }

    // This just creates an input port through which the client may receive
    // incoming MIDI messages from any MIDI source.
    err = MIDIInputPortCreate(client, CFSTR("MIDI Input Port"), midiReadProc, NULL, &inPort);
    if (err != noErr) { goto Exit; }

    err = MIDIOutputPortCreate(client, CFSTR("MIDI Output Port"), &outPort);
    if (err != noErr) { goto Exit; }

Exit:
    if (err != noErr) {
        const char* s = MIDI_Utils_GetErrorMsg(err);
        if (s != NULL) {
            printf("%s\n", s);
        }
    }
}


INT32 MIDI_Utils_OpenDevice(int direction, INT32 deviceID, MacMidiDeviceHandle** handle,
                            int num_msgs, int num_long_msgs,
                            size_t lm_size)
{
    midiInit();

    int err = MIDI_ERROR_NONE;
    MIDIEndpointRef endpoint = (MIDIEndpointRef) 0;

    TRACE0("MIDI_Utils_OpenDevice\n");

    (*handle) = (MacMidiDeviceHandle*) malloc(sizeof(MacMidiDeviceHandle));
    if (!(*handle)) {
        ERROR0("ERROR: MIDI_Utils_OpenDevice: out of memory\n");
        return MIDI_OUT_OF_MEMORY;
    }
    memset(*handle, 0, sizeof(MacMidiDeviceHandle));

    // Create the infrastructure for MIDI in/out, and after that,
    // get the device's endpoint.
    if (direction == MIDI_IN) {
        // Create queue and the pthread condition variable.
        (*handle)->h.queue = MIDI_CreateQueue(num_msgs);
        (*handle)->h.platformData = MIDI_CreateConditionVariable();
        if (!(*handle)->h.queue || !(*handle)->h.platformData) {
            ERROR0("< ERROR: MIDI_IN_OpenDevice: could not create queue or condition variable\n");
            free(*handle);
            (*handle) = NULL;
            return MIDI_OUT_OF_MEMORY;
        }
        endpoint = MIDIGetSource(deviceID);
        (*handle)->port = inPort;
    } else if (direction == MIDI_OUT) {
        endpoint = MIDIGetDestination(deviceID);
        (*handle)->port = outPort;
    }

    if (!endpoint) {
        // An error occurred.
        free(*handle);
        return MIDI_INVALID_DEVICEID;
    }
    (*handle)->h.deviceHandle = (void*) (intptr_t) endpoint;
    (*handle)->h.startTime = getCurrentTimeInNanos();
    (*handle)->direction = direction;
    (*handle)->deviceID = deviceID;

    TRACE0("MIDI_Utils_OpenDevice: succeeded\n");
    return err;
}


INT32 MIDI_Utils_CloseDevice(MacMidiDeviceHandle* handle) {
    int err = MIDI_ERROR_NONE;
    bool midiIn = (handle->direction == MIDI_IN);

    TRACE0("> MIDI_Utils_CloseDevice\n");
    if (!handle) {
        ERROR0("< ERROR: MIDI_Utils_CloseDevice: handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    if (!handle->h.deviceHandle) {
        ERROR0("< ERROR: MIDI_Utils_CloseDevice: native handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    handle->isStarted = FALSE;
    handle->h.deviceHandle = NULL;

    if (midiIn) {
        if (handle->h.queue != NULL) {
            MidiMessageQueue* queue = handle->h.queue;
            handle->h.queue = NULL;
            MIDI_DestroyQueue(queue);
        }
        if (handle->h.platformData) {
            MIDI_DestroyConditionVariable(handle->h.platformData);
        }
    }
    free(handle);

    TRACE0("< MIDI_Utils_CloseDevice: succeeded\n");
    return err;
}


INT32 MIDI_Utils_StartDevice(MacMidiDeviceHandle* handle) {
    OSStatus err = noErr;

    if (!handle || !handle->h.deviceHandle) {
        ERROR0("ERROR: MIDI_Utils_StartDevice: handle or native is NULL\n");
        return MIDI_INVALID_HANDLE;
    }

    // Clears all the events from the queue.
    MIDI_QueueClear(handle->h.queue);

    if (!handle->isStarted) {
        /* set the flag that we can now receive messages */
        handle->isStarted = TRUE;

        if (handle->direction == MIDI_IN) {
            // The handle->h.platformData field contains the (pthread_cond_t*)
            // associated with the source of the MIDI input stream, and is
            // used in the CoreMIDI's callback to signal the arrival of new
            // data.
            //
            // Similarly, handle->h.queue is used in the CoreMDID's callback
            // to dispatch the incoming messages to the appropriate queue.
            //
            err = MIDIPortConnectSource(inPort, (MIDIEndpointRef) (intptr_t) (handle->h.deviceHandle), (void*) handle);
        } else if (handle->direction == MIDI_OUT) {
            // Unschedules previous-sent packets.
            err = MIDIFlushOutput((MIDIEndpointRef) (intptr_t) handle->h.deviceHandle);
        }

        MIDI_CHECK_ERROR;
    }
    return MIDI_SUCCESS; /* don't fail */
}


INT32 MIDI_Utils_StopDevice(MacMidiDeviceHandle* handle) {
    OSStatus err = noErr;

    if (!handle || !handle->h.deviceHandle) {
        ERROR0("ERROR: MIDI_Utils_StopDevice: handle or native handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }

    if (handle->isStarted) {
        /* set the flag that we don't want to receive messages anymore */
        handle->isStarted = FALSE;

        if (handle->direction == MIDI_IN) {
            err = MIDIPortDisconnectSource(inPort, (MIDIEndpointRef) (intptr_t) (handle->h.deviceHandle));
        } else if (handle->direction == MIDI_OUT) {
            // Unschedules previously-sent packets.
            err = MIDIFlushOutput((MIDIEndpointRef) (intptr_t) handle->h.deviceHandle);
        }

        MIDI_CHECK_ERROR;
    }
    return MIDI_SUCCESS;
}


INT64 MIDI_Utils_GetTimeStamp(MacMidiDeviceHandle* handle) {

    if (!handle || !handle->h.deviceHandle) {
        ERROR0("ERROR: MIDI_Utils_GetTimeStamp: handle or native handle is NULL\n");
        return (INT64) -1; /* failure */
    }

    UInt64 delta = getCurrentTimeInNanos() - handle->h.startTime;
    return (INT64) ((delta + 500) / 1000);
}


/***************************************************************************/
/*            Condition Variable Support for Mac OS X Port                 */
/*                                                                         */
/* This works with the Native Locking Support defined below.  We are using */
/* POSIX pthread_cond_t/pthread_mutex_t to do locking and synchronization. */
/*                                                                         */
/* For MidiDeviceHandle* handle, the mutex reference is stored as handle-> */
/* queue->lock while the condition variabale reference is stored as handle */
/* ->platformData.                                                         */
/***************************************************************************/

// Called from Midi_Utils_Opendevice(...) to create a condition variable
// used to synchronize between the receive thread created by the CoreMIDI
// and the Java-initiated MidiInDevice run loop.
void* MIDI_CreateConditionVariable() {
    pthread_cond_t* cond = (pthread_cond_t*) malloc(sizeof(pthread_cond_t));
    pthread_cond_init(cond, NULL);
    return (void*) cond;
}

void MIDI_DestroyConditionVariable(void* cond) {
    while (pthread_cond_destroy((pthread_cond_t*) cond) == EBUSY) {
        pthread_cond_broadcast((pthread_cond_t*) cond);
        sched_yield();
    }
    return;
}

// Called from MIDI_IN_GetMessage(...) to wait for MIDI messages to become
// available via delivery from the CoreMIDI receive thread
void MIDI_WaitOnConditionVariable(void* cond, void* lock) {
    if (cond && lock) {
        pthread_mutex_lock(lock);
        pthread_cond_wait((pthread_cond_t*) cond, (pthread_mutex_t*) lock);
        pthread_mutex_unlock(lock);
    }
    return;
}

// Called from midiReadProc(...) to notify the waiting thread to unblock on
// the condition variable.
void MIDI_SignalConditionVariable(void* cond) {
    if (cond) {
        pthread_cond_signal((pthread_cond_t*) cond);
    }
    return;
}


/**************************************************************************/
/*                     Native Locking Support                             */
/*                                                                        */
/* @see src/share/natve/com/sun/media/sound/PlatformMidi.c which contains */
/* utility functions for platform midi support where the section of code  */
/* for MessageQueue implementation calls out to these functions.          */
/**************************************************************************/

void* MIDI_CreateLock() {
    pthread_mutex_t* lock = (pthread_mutex_t*) malloc(sizeof(pthread_mutex_t));
    pthread_mutex_init(lock, NULL);
    TRACE0("MIDI_CreateLock\n");
    return (void *)lock;
}

void MIDI_DestroyLock(void* lock) {
    if (lock) {
        pthread_mutex_destroy((pthread_mutex_t*) lock);
        free(lock);
        TRACE0("MIDI_DestroyLock\n");
    }
}

void MIDI_Lock(void* lock) {
    if (lock) {
        pthread_mutex_lock((pthread_mutex_t*) lock);
    }
}

void MIDI_Unlock(void* lock) {
    if (lock) {
        pthread_mutex_unlock((pthread_mutex_t*) lock);
    }
}


#endif // USE_PLATFORM_MIDI_IN || USE_PLATFORM_MIDI_OUT
