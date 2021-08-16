/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef PLATFORM_MIDI_INCLUDED
#define PLATFORM_MIDI_INCLUDED


#include "SoundDefs.h"
#include "Configure.h" // put flags for debug msgs etc. here
#include "Utilities.h"


/* do we need the queue ? */
#if (USE_PLATFORM_MIDI_IN == TRUE) || (USE_PLATFORM_MIDI_OUT == TRUE)
 #if X_PLATFORM == X_WINDOWS || X_PLATFORM == X_MACOSX
  #define USE_MIDI_QUEUE TRUE
 #endif
#endif

/* *********************** MIDI TYPES (for all platforms) ******************************* */

/* return value for functions to denote successful completion */
#define MIDI_SUCCESS 0
/* code if function is not supported */
#define MIDI_NOT_SUPPORTED -11111
/* return code for invalid handle */
#define MIDI_INVALID_DEVICEID -11112
/* return code for invalid handle */
#define MIDI_INVALID_HANDLE -11113
/* return code for invalid argument */
#define MIDI_INVALID_ARGUMENT -11114
/* return code for out of memory */
#define MIDI_OUT_OF_MEMORY -11115

// MIDI message types
typedef enum {
    SHORT_MESSAGE = 0,
    LONG_MESSAGE = 1
} MidiMessageType;

// MIDI message object
typedef struct tag_MidiMessage {
    INT64 timestamp;  // in microseconds
    INT32 locked;     // TRUE when event is currently being read
    MidiMessageType type;
    union {
        struct {
            // platform-endianness packed message:
            // status | data1<<8 | data2<<16
            UINT32 packedMsg;
        } s; // short message
        struct {
            UINT32  size;
            // this buffer is read only. It must not be freed.
            UBYTE* data;
            INT32 index; // sysex buffer number
        } l; // long message
    } data;
} MidiMessage;

/* error handling. Implemented in PlatformMidi.c */
char* MIDI_IN_InternalGetErrorString(INT32 err);
char* MIDI_OUT_InternalGetErrorString(INT32 err);


#if USE_MIDI_QUEUE == TRUE
/*
 * Native MIDI message circular buffer
 */
typedef struct tag_MidiQueue {
    void* lock;
    INT32 size;
    INT32 capacity;
    INT32 readIndex;
    INT32 writeIndex;
    MidiMessage queue[1];
} MidiMessageQueue;
#endif

// device handle, to be created and filled in MIDI_IN_OpenDevice() and MIDI_OUT_OpenDevice()
typedef struct tag_MidiDeviceHandle {
    void* deviceHandle;      // handle to the device
    void* longBuffers;       // contains platform-specific data for long buffers, e.g. list of MIDIHDR
    void* platformData;      // contains platform specific data, e.g. an Event object
    INT32 isWaiting;         // if TRUE, then waiting for new events
    INT64 startTime;         // start time
#if USE_MIDI_QUEUE == TRUE
    MidiMessageQueue* queue; // may be NULL if no queue is used
#endif
} MidiDeviceHandle;


#if USE_MIDI_QUEUE == TRUE

/*
 * Native Locking support
 */
void* MIDI_CreateLock();
void MIDI_DestroyLock(void* lock);

/* Blocks until this lock can be gotten.
 * Nop if lock is NULL */
void MIDI_Lock(void* lock);

/* Releases this lock */
void MIDI_Unlock(void* lock);

MidiMessageQueue* MIDI_CreateQueue(int capacity);
void MIDI_DestroyQueue(MidiMessageQueue* queue);
// if overwrite is true, oldest messages will be overwritten when the queue is full
// returns true, if message has been added
int MIDI_QueueAddShort(MidiMessageQueue* queue, UINT32 packedMsg, INT64 timestamp, int overwrite);
int MIDI_QueueAddLong(MidiMessageQueue* queue, UBYTE* data, UINT32 size,
                      INT32 sysexIndex, INT64 timestamp, int overwrite);

// returns NULL if no messages in queue.
MidiMessage* MIDI_QueueRead(MidiMessageQueue* queue);
// message will be removed from queue.
void MIDI_QueueRemove(MidiMessageQueue* queue, INT32 onlyLocked);
void MIDI_QueueClear(MidiMessageQueue* queue);

#endif /* USE_MIDI_QUEUE */


/*
 * Platform MIDI IN support.
 * deviceId:            device-by-number
 * deviceHandle:        native device handle
 */

#if USE_PLATFORM_MIDI_IN == TRUE

// number of messages to be buffered
#define MIDI_IN_MESSAGE_QUEUE_SIZE 64
// number of sysex to be buffered
#define MIDI_IN_LONG_QUEUE_SIZE 20
// maximum number of bytes in one sys ex message
#define MIDI_IN_LONG_MESSAGE_SIZE 1024


/*
 * Return an error message for the error code
 */
char* MIDI_IN_GetErrorStr(INT32 err);


/*
 * Get the number of MIDI IN devices on the system.
 */
INT32 MIDI_IN_GetNumDevices();

/*
 * Get the name of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_GetDeviceName(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the vendor of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_GetDeviceVendor(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the description of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_GetDeviceDescription(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the version of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_GetDeviceVersion(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Open the device with this id.
 * Returns a device handle in handle*.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_OpenDevice(INT32 deviceID, MidiDeviceHandle** handle);

/*
 * Close the device handle.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_CloseDevice(MidiDeviceHandle* handle);

/*
 * Start the device with this handle.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_StartDevice(MidiDeviceHandle* handle);

/*
 * Stop the device with this handle.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_IN_StopDevice(MidiDeviceHandle* handle);

/*
 * Return the current time stamp in microseconds.
 * If not supported, or problem occurred, returns -1
 */
INT64 MIDI_IN_GetTimeStamp(MidiDeviceHandle* handle);

/*
 * Get the next message from the queue.
 * This call blocks until the device is stopped
 * or a message is received.
 * The returned message is READ ONLY.
 * The message will be returned into the message
 * queue by calling MIDI_IN_ReleaseMessage.
 */
MidiMessage* MIDI_IN_GetMessage(MidiDeviceHandle* handle);

/*
 * Put a message, which was taken
 * out of the queue, back into the queue.
 */
void MIDI_IN_ReleaseMessage(MidiDeviceHandle* handle, MidiMessage* msg);

#endif // USE_PLATFORM_MIDI_IN


/*
 * Platform MIDI OUT support.
 * deviceId:            device-by-number
 * deviceHandle:        native device handle
 */

#if USE_PLATFORM_MIDI_OUT == TRUE

// number of messages to be buffered
#define MIDI_OUT_MESSAGE_QUEUE_SIZE 32
// number of sysex to be buffered
#define MIDI_OUT_LONG_QUEUE_SIZE 16
// maximum number of bytes in one sys ex message
#define MIDI_OUT_LONG_MESSAGE_SIZE 1024

/*
 * Return an error message for the error code
 */
char* MIDI_OUT_GetErrorStr(INT32 err);


/*
 * Get the number of MIDI OUT devices on the system.
 */
INT32 MIDI_OUT_GetNumDevices();

/*
 * Get the name of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_GetDeviceName(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the vendor of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_GetDeviceVendor(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the description of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_GetDeviceDescription(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Get the version of the device with this id
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_GetDeviceVersion(INT32 deviceID, char *name, UINT32 nameLength);

/*
 * Open the device with this id.
 * Returns a device handle in handle*.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_OpenDevice(INT32 deviceID, MidiDeviceHandle** handle);

/*
 * Close the device handle.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_CloseDevice(MidiDeviceHandle* handle);

/*
 * Return the current time stamp in microseconds (the time since the device
 * was opened).
 * If not supported, or problem occurred, returns -1
 */
INT64 MIDI_OUT_GetTimeStamp(MidiDeviceHandle* handle);

/*
 * Send a short message to the hardware.
 * packedMsg: (status | data1<<8 | data2<<16) in platform-endianness
 * Timestamp is in microseconds.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_SendShortMessage(MidiDeviceHandle* handle, UINT32 packedMsg, UINT32 timestamp);

/*
 * Send a long message to the hardware.  Timestamp is in microseconds.
 * This blocks until a slot to send a message is free.
 * Returns MIDI_SUCCESS or an error code
 */
INT32 MIDI_OUT_SendLongMessage(MidiDeviceHandle* handle, UBYTE* data, UINT32 size, UINT32 timestamp);

#endif // USE_PLATFORM_MIDI_OUT

#endif // PLATFORM_MIDI_INCLUDED
