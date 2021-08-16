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

//#define USE_ERROR
//#define USE_TRACE

#if USE_PLATFORM_MIDI_OUT == TRUE

#include "PLATFORM_API_MacOSX_MidiUtils.h"

char* MIDI_OUT_GetErrorStr(INT32 err) {
    return (char *) MIDI_Utils_GetErrorMsg((int) err);
}


INT32 MIDI_OUT_GetNumDevices() {
    return MIDI_Utils_GetNumDevices(MIDI_OUT);
}


INT32 MIDI_OUT_GetDeviceName(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_Utils_GetDeviceName(MIDI_OUT, deviceID, name, nameLength);
}


INT32 MIDI_OUT_GetDeviceVendor(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_Utils_GetDeviceVendor(MIDI_OUT, deviceID, name, nameLength);
}


INT32 MIDI_OUT_GetDeviceDescription(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_Utils_GetDeviceDescription(MIDI_OUT, deviceID, name, nameLength);
}


INT32 MIDI_OUT_GetDeviceVersion(INT32 deviceID, char *name, UINT32 nameLength) {
    return MIDI_Utils_GetDeviceVersion(MIDI_OUT, deviceID, name, nameLength);
}


/* *************************** MidiOutDevice implementation ***************************************** */

INT32 MIDI_OUT_OpenDevice(INT32 deviceID, MidiDeviceHandle** handle) {
    TRACE1("MIDI_OUT_OpenDevice: deviceID: %d\n", (int) deviceID);
    /* queue sizes are ignored for MIDI_OUT only (uses STREAMS) */
    return MIDI_Utils_OpenDevice(MIDI_OUT, deviceID, (MacMidiDeviceHandle**) handle, 0, 0, 0);
}

INT32 MIDI_OUT_CloseDevice(MidiDeviceHandle* handle) {
    TRACE0("MIDI_OUT_CloseDevice\n");

    // issue a "SUSTAIN OFF" message to each MIDI channel, 0 to 15.
    // "CONTROL CHANGE" is 176, "SUSTAIN CONTROLLER" is 64, and the value is 0.
    // $$fb 2002-04-04: It is responsability of the application developer to
    // leave the device in a consistent state. So I put this in comments
    /*
      for (channel = 0; channel < 16; channel++)
      MIDI_OUT_SendShortMessage(deviceHandle, (unsigned char)(176 + channel),
      (unsigned char)64, (unsigned char)0, (UINT32)-1);
    */
    return MIDI_Utils_CloseDevice((MacMidiDeviceHandle*) handle);
}


INT64 MIDI_OUT_GetTimeStamp(MidiDeviceHandle* handle) {
    return MIDI_Utils_GetTimeStamp((MacMidiDeviceHandle*) handle);
}


INT32 MIDI_OUT_SendShortMessage(MidiDeviceHandle* handle, UINT32 packedMsg, UINT32 timestamp) {
    OSStatus err = noErr;

    TRACE2("> MIDI_OUT_SendShortMessage %x, time: %d\n", (uint) packedMsg, (int) timestamp);
    if (!handle) {
        ERROR0("< ERROR: MIDI_OUT_SendShortMessage: handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }

    MacMidiDeviceHandle* macHandle = (MacMidiDeviceHandle*) handle;
    UInt8 mBuffers[100];
    MIDIPacketList* packetList = (MIDIPacketList*) mBuffers;
    MIDIPacket* packet;
    UINT32 nData;
    Byte data[3] = {packedMsg & 0xFF, (packedMsg >> 8) & 0xFF, (packedMsg >> 16) & 0xFF};
    bool byteIsInvalid = FALSE;

    packet = MIDIPacketListInit(packetList);
    switch (data[0] & 0xF0) {
        case 0x80:    // Note off
        case 0x90:    // Note on
        case 0xA0:    // Aftertouch
        case 0xB0:    // Controller
        case 0xE0:    // Pitch wheel
            nData = 3;
            break;

        case 0xC0:    // Program change
        case 0xD0:    // Channel pressure
            nData = 2;
            break;

        case 0xF0: {
            // System common message
            switch (data[0]) {
                case 0xF0:
                case 0xF7:
                    // System exclusive
                    fprintf(stderr, "%s: %d->internal error: sysex message status=0x%X while sending short message\n",
                            __FILE__, __LINE__, data[0]);
                    byteIsInvalid = TRUE;
                    break;

                case 0xF1:    // MTC quarter frame message
                    //fprintf(stderr, ">>>MIDI_OUT_SendShortMessage: MTC quarter frame message....\n");
                    nData = 2;
                    break;
                case 0xF3:    // Song select
                    //fprintf(stderr, ">>>MIDI_OUT_SendShortMessage: Song select....\n");
                    nData = 2;
                    break;

                case 0xF2:    // Song position pointer
                    //fprintf(stderr, ">>>MIDI_OUT_SendShortMessage: Song position pointer....\n");
                    nData = 3;
                    break;

                case 0xF6:    // Tune request
                    //fprintf(stderr, ">>>MIDI_OUT_SendShortMessage: Tune request....\n");
                    nData = 1;
                    break;

                default:
                    // Invalid message
                    fprintf(stderr, "%s: %d->Invalid message: message status=0x%X while sending short message\n",
                            __FILE__, __LINE__, data[0]);
                    byteIsInvalid = TRUE;
                    break;
            }
            break;
        }

        default:
            // This can't happen, but handle it anyway.
            fprintf(stderr, "%s: %d->Invalid message: message status=0x%X while sending short message\n",
                    __FILE__, __LINE__, data[0]);
            byteIsInvalid = TRUE;
            break;
    }

    if (byteIsInvalid) return -1;

    MIDIPacketListAdd(packetList, sizeof(mBuffers), packet, 0, nData, data);
    err = MIDISend(macHandle->port, (MIDIEndpointRef) (intptr_t) handle->deviceHandle, packetList);

    MIDI_CHECK_ERROR;
    TRACE0("< MIDI_OUT_SendShortMessage\n");
    return (err == noErr ? MIDI_SUCCESS : -1);
}


INT32 MIDI_OUT_SendLongMessage(MidiDeviceHandle* handle, UBYTE* data, UINT32 size, UINT32 timestamp) {
    OSStatus err = noErr;

    TRACE2("> MIDI_OUT_SendLongMessage size %d, time: %d\n", (int) size, (int) timestamp);
    if (!handle || !data) {
        ERROR0("< ERROR: MIDI_OUT_SendLongMessage: handle, or data is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    if (size == 0) {
        return MIDI_SUCCESS;
    }

    MacMidiDeviceHandle* macHandle = (MacMidiDeviceHandle*) handle;
    UInt8 mBuffers[8196];
    MIDIPacketList* packetList = (MIDIPacketList*) mBuffers;
    MIDIPacket* packet = NULL;
    UINT32 remaining = size;
    UINT32 increment = 512;
    UINT32 nData;

    handle->isWaiting = TRUE;

    while (remaining > 0) {

        if (packet == NULL) {
            packet = MIDIPacketListInit(packetList);
        }

        if (remaining > increment) {
            nData = increment;
        } else {
            nData = remaining;
        }

        // Copies the bytes to our current packet.
        if ((packet = MIDIPacketListAdd(packetList, sizeof(mBuffers), packet, 0, nData, (const Byte*) data)) == NULL) {
            // Packet list is full, send it.
            err = MIDISend(macHandle->port, (MIDIEndpointRef) (intptr_t) handle->deviceHandle, packetList);
            if (err != noErr) {
                break;
            }
        } else {
            // Moves the data pointer to the next segment.
            data += nData;
            remaining -= nData;
            packet = MIDIPacketNext(packet);
        }
    }

    MIDI_CHECK_ERROR;
    handle->isWaiting = FALSE;
    TRACE0("< MIDI_OUT_SendLongMessage\n");
    return (err == noErr ? MIDI_SUCCESS : -1);
}

#endif /* USE_PLATFORM_MIDI_OUT */
