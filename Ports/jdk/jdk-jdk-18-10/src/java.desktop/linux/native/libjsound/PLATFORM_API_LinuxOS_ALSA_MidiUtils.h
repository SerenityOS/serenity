/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

#include <alsa/asoundlib.h>
#include "Utilities.h"
#include "PlatformMidi.h"


#ifndef PLATFORM_API_LINUXOS_ALSA_MIDIUTILS_H_INCLUDED
#define PLATFORM_API_LINUXOS_ALSA_MIDIUTILS_H_INCLUDED

#define EVENT_PARSER_BUFSIZE (2048)

// if this is defined, use plughw: devices
//#define ALSA_MIDI_USE_PLUGHW
#undef ALSA_MIDI_USE_PLUGHW

typedef struct tag_ALSA_MIDIDeviceDescription {
        int index;          // in
        int strLen;         // in
        INT32 deviceID;    // out
        char* name;         // out
        char* description;  // out
} ALSA_MIDIDeviceDescription;


const char* getErrorStr(INT32 err);

/* Returns the number of devices. */
/* direction is either SND_RAWMIDI_STREAM_OUTPUT or
   SND_RAWMIDI_STREAM_INPUT. */
int getMidiDeviceCount(snd_rawmidi_stream_t direction);

/* Returns MIDI_SUCCESS or MIDI_INVALID_DEVICEID */
/* direction is either SND_RAWMIDI_STREAM_OUTPUT or
   SND_RAWMIDI_STREAM_INPUT. */
int getMidiDeviceName(snd_rawmidi_stream_t direction, int index,
                      char *name, UINT32 nameLength);

/* Returns MIDI_SUCCESS or MIDI_INVALID_DEVICEID */
int getMidiDeviceVendor(int index, char *name, UINT32 nameLength);

/* Returns MIDI_SUCCESS or MIDI_INVALID_DEVICEID */
/* direction is either SND_RAWMIDI_STREAM_OUTPUT or
   SND_RAWMIDI_STREAM_INPUT. */
int getMidiDeviceDescription(snd_rawmidi_stream_t direction, int index,
                             char *name, UINT32 nameLength);

/* Returns MIDI_SUCCESS or MIDI_INVALID_DEVICEID */
int getMidiDeviceVersion(int index, char *name, UINT32 nameLength);

// returns 0 on success, otherwise MIDI_OUT_OF_MEMORY or ALSA error code
/* direction is either SND_RAWMIDI_STREAM_OUTPUT or
   SND_RAWMIDI_STREAM_INPUT. */
INT32 openMidiDevice(snd_rawmidi_stream_t direction, INT32 deviceIndex,
                     MidiDeviceHandle** handle);

// returns 0 on success, otherwise a (negative) ALSA error code
INT32 closeMidiDevice(MidiDeviceHandle* handle);

INT64 getMidiTimestamp(MidiDeviceHandle* handle);

#endif // PLATFORM_API_LINUXOS_ALSA_MIDIUTILS_H_INCLUDED
