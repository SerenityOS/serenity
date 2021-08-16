/*
 * Copyright (c) 2003, 2010, Oracle and/or its affiliates. All rights reserved.
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

#if USE_PLATFORM_MIDI_IN == TRUE


#include <alsa/asoundlib.h>
#include "PlatformMidi.h"
#include "PLATFORM_API_LinuxOS_ALSA_MidiUtils.h"
#if defined(i586)
#include <sys/utsname.h>
#endif

/*
 * Helper methods
 */

static inline UINT32 packMessage(int status, int data1, int data2) {
    return ((status & 0xFF) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16));
}


static void setShortMessage(MidiMessage* message,
                            int status, int data1, int data2) {
    message->type = SHORT_MESSAGE;
    message->data.s.packedMsg = packMessage(status, data1, data2);
}


static void setRealtimeMessage(MidiMessage* message, int status) {
    setShortMessage(message, status, 0, 0);
}


static void set14bitMessage(MidiMessage* message, int status, int value) {
    TRACE3("14bit value: %d, lsb: %d, msb: %d\n", value, value & 0x7F, (value >> 7) & 0x7F);
    value &= 0x3FFF;
    TRACE3("14bit value (2): %d, lsb: %d, msb: %d\n", value, value & 0x7F, (value >> 7) & 0x7F);
    setShortMessage(message, status,
                    value & 0x7F,
                    (value >> 7) & 0x7F);
}


/*
 * implementation of the platform-dependent
 * MIDI in functions declared in PlatformMidi.h
 */

char* MIDI_IN_GetErrorStr(INT32 err) {
    return (char*) getErrorStr(err);
}

INT32 MIDI_IN_GetNumDevices() {
/* Workaround for 6842956: 32bit app on 64bit linux
 * gets assertion failure trying to open midiIn ports.
 * Untill the issue is fixed in ALSA
 * (https://bugtrack.alsa-project.org/alsa-bug/view.php?id=4807)
 * report no midi in devices in the configuration.
 */
#if defined(i586)
    static int jre32onlinux64 = -1;
    if (jre32onlinux64 < 0) {
        jre32onlinux64 = 0;
        /* The workaround may be disabled setting "JAVASOUND_ENABLE_MIDIIN"
         * environment variable.
         */
        if (getenv("JAVASOUND_ENABLE_MIDIIN") == NULL) {
            struct utsname u;
            jre32onlinux64 = 0;
            if (uname(&u) == 0) {
                if (strstr(u.machine, "64") != NULL) {
                    TRACE0("jre32 on linux64 detected - report no midiIn devices\n");
                    jre32onlinux64 = 1;
                }
            }
        }
    }
    if (jre32onlinux64) {
        return 0;
    }
#endif

    TRACE0("MIDI_IN_GetNumDevices()\n");

    return getMidiDeviceCount(SND_RAWMIDI_STREAM_INPUT);
}


INT32 MIDI_IN_GetDeviceName(INT32 deviceIndex, char *name, UINT32 nameLength) {
    int ret = getMidiDeviceName(SND_RAWMIDI_STREAM_INPUT, deviceIndex,
                                name, nameLength);
    return ret;
}


INT32 MIDI_IN_GetDeviceVendor(INT32 deviceIndex, char *name, UINT32 nameLength) {
    int ret = getMidiDeviceVendor(deviceIndex, name, nameLength);
    return ret;
}


INT32 MIDI_IN_GetDeviceDescription(INT32 deviceIndex, char *name, UINT32 nameLength) {
    int ret = getMidiDeviceDescription(SND_RAWMIDI_STREAM_INPUT, deviceIndex,
                                       name, nameLength);
    return ret;
}


INT32 MIDI_IN_GetDeviceVersion(INT32 deviceIndex, char *name, UINT32 nameLength) {
    int ret = getMidiDeviceVersion(deviceIndex, name, nameLength);
    return ret;
}

/*************************************************************************/

INT32 MIDI_IN_OpenDevice(INT32 deviceIndex, MidiDeviceHandle** handle) {
    INT32 ret;
    TRACE0("> MIDI_IN_OpenDevice\n");
    ret = openMidiDevice(SND_RAWMIDI_STREAM_INPUT, deviceIndex, handle);
    TRACE1("< MIDI_IN_OpenDevice: returning %d\n", (int) ret);
    return ret;
}


INT32 MIDI_IN_CloseDevice(MidiDeviceHandle* handle) {
    INT32 ret;
    TRACE0("> MIDI_IN_CloseDevice\n");
    ret = closeMidiDevice(handle);
    TRACE1("< MIDI_IN_CloseDevice: returning %d\n", (int) ret);
    return ret;
}


INT32 MIDI_IN_StartDevice(MidiDeviceHandle* handle) {
    TRACE0("MIDI_IN_StartDevice\n");
    return MIDI_SUCCESS;
}


INT32 MIDI_IN_StopDevice(MidiDeviceHandle* handle) {
    TRACE0("MIDI_IN_StopDevice\n");
    return MIDI_SUCCESS;
}


INT64 MIDI_IN_GetTimeStamp(MidiDeviceHandle* handle) {
    return getMidiTimestamp(handle);
}


/* read the next message from the queue */
MidiMessage* MIDI_IN_GetMessage(MidiDeviceHandle* handle) {
    snd_seq_event_t alsa_message;
    MidiMessage* jdk_message;
    int err;
    char buffer[1];
    int status;

    TRACE0("> MIDI_IN_GetMessage\n");
    if (!handle) {
        ERROR0("< ERROR: MIDI_IN_GetMessage(): handle is NULL\n");
        return NULL;
    }
    if (!handle->deviceHandle) {
        ERROR0("< ERROR: MIDI_IN_GetMessage(): native handle is NULL\n");
        return NULL;
    }
    if (!handle->platformData) {
        ERROR0("< ERROR: MIDI_IN_GetMessage(): platformData is NULL\n");
        return NULL;
    }

    /* For MIDI In, the device is left in non blocking mode. So if there is
       no data from the device, snd_rawmidi_read() returns with -11 (EAGAIN).
       This results in jumping back to the Java layer. */
    while (TRUE) {
        TRACE0("before snd_rawmidi_read()\n");
        err = snd_rawmidi_read((snd_rawmidi_t*) handle->deviceHandle, buffer, 1);
        TRACE0("after snd_rawmidi_read()\n");
        if (err != 1) {
            ERROR2("< ERROR: MIDI_IN_GetMessage(): snd_rawmidi_read() returned %d : %s\n", err, snd_strerror(err));
            return NULL;
        }
        // printf("received byte: %d\n", buffer[0]);
        err = snd_midi_event_encode_byte((snd_midi_event_t*) handle->platformData,
                                         (int) buffer[0],
                                         &alsa_message);
        if (err == 1) {
            break;
        } else if (err < 0) {
            ERROR1("< ERROR: MIDI_IN_GetMessage(): snd_midi_event_encode_byte() returned %d\n", err);
            return NULL;
        }
    }
    jdk_message = (MidiMessage*) calloc(sizeof(MidiMessage), 1);
    if (!jdk_message) {
        ERROR0("< ERROR: MIDI_IN_GetMessage(): out of memory\n");
        return NULL;
    }
    // TODO: tra
    switch (alsa_message.type) {
    case SND_SEQ_EVENT_NOTEON:
    case SND_SEQ_EVENT_NOTEOFF:
    case SND_SEQ_EVENT_KEYPRESS:
        status = (alsa_message.type == SND_SEQ_EVENT_KEYPRESS) ? 0xA0 :
            (alsa_message.type == SND_SEQ_EVENT_NOTEON) ? 0x90 : 0x80;
        status |= alsa_message.data.note.channel;
        setShortMessage(jdk_message, status,
                        alsa_message.data.note.note,
                        alsa_message.data.note.velocity);
        break;

    case SND_SEQ_EVENT_CONTROLLER:
        status = 0xB0 | alsa_message.data.control.channel;
        setShortMessage(jdk_message, status,
                        alsa_message.data.control.param,
                        alsa_message.data.control.value);
        break;

    case SND_SEQ_EVENT_PGMCHANGE:
    case SND_SEQ_EVENT_CHANPRESS:
        status = (alsa_message.type == SND_SEQ_EVENT_PGMCHANGE) ? 0xC0 : 0xD0;
        status |= alsa_message.data.control.channel;
        setShortMessage(jdk_message, status,
                        alsa_message.data.control.value, 0);
        break;

    case SND_SEQ_EVENT_PITCHBEND:
        status = 0xE0 | alsa_message.data.control.channel;
        // $$mp 2003-09-23:
        // possible hack to work around a bug in ALSA. Necessary for
        // ALSA 0.9.2. May be fixed in newer versions of ALSA.
        // alsa_message.data.control.value ^= 0x2000;
        // TRACE1("pitchbend value: %d\n", alsa_message.data.control.value);
        set14bitMessage(jdk_message, status,
                        alsa_message.data.control.value);
        break;

        /* System exclusive messages */

    case SND_SEQ_EVENT_SYSEX:
        jdk_message->type = LONG_MESSAGE;
        jdk_message->data.l.size = alsa_message.data.ext.len;
        jdk_message->data.l.data = malloc(alsa_message.data.ext.len);
        if (jdk_message->data.l.data == NULL) {
            ERROR0("< ERROR: MIDI_IN_GetMessage(): out of memory\n");
            free(jdk_message);
            jdk_message = NULL;
        } else {
            memcpy(jdk_message->data.l.data, alsa_message.data.ext.ptr, alsa_message.data.ext.len);
        }
        break;

        /* System common messages */

    case SND_SEQ_EVENT_QFRAME:
        setShortMessage(jdk_message, 0xF1,
                        alsa_message.data.control.value & 0x7F, 0);
        break;

    case SND_SEQ_EVENT_SONGPOS:
        set14bitMessage(jdk_message, 0xF2,
                        alsa_message.data.control.value);
        break;

    case SND_SEQ_EVENT_SONGSEL:
        setShortMessage(jdk_message, 0xF3,
                        alsa_message.data.control.value & 0x7F, 0);
        break;

    case SND_SEQ_EVENT_TUNE_REQUEST:
        setRealtimeMessage(jdk_message, 0xF6);
        break;

        /* System realtime messages */

    case SND_SEQ_EVENT_CLOCK:
        setRealtimeMessage(jdk_message, 0xF8);
        break;

    case SND_SEQ_EVENT_START:
        setRealtimeMessage(jdk_message, 0xFA);
        break;

    case SND_SEQ_EVENT_CONTINUE:
        setRealtimeMessage(jdk_message, 0xFB);
        break;

    case SND_SEQ_EVENT_STOP:
        setRealtimeMessage(jdk_message, 0xFC);
        break;

    case SND_SEQ_EVENT_SENSING:
        setRealtimeMessage(jdk_message, 0xFE);
        break;

    case SND_SEQ_EVENT_RESET:
        setRealtimeMessage(jdk_message, 0xFF);
        break;

    default:
        ERROR0("< ERROR: MIDI_IN_GetMessage(): unhandled ALSA MIDI message type\n");
        free(jdk_message);
        jdk_message = NULL;

    }

    // set timestamp
    if (jdk_message != NULL) {
        jdk_message->timestamp = getMidiTimestamp(handle);
    }
    TRACE1("< MIDI_IN_GetMessage: returning %p\n", jdk_message);
    return jdk_message;
}


void MIDI_IN_ReleaseMessage(MidiDeviceHandle* handle, MidiMessage* msg) {
    if (!msg) {
        ERROR0("< ERROR: MIDI_IN_ReleaseMessage(): message is NULL\n");
        return;
    }
    if (msg->type == LONG_MESSAGE && msg->data.l.data) {
        free(msg->data.l.data);
    }
    free(msg);
}

#endif /* USE_PLATFORM_MIDI_IN */
