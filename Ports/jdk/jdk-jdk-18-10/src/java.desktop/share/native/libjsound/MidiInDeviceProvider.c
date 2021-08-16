/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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


#include <jni.h>
#include "SoundDefs.h"
#include "PlatformMidi.h"
#include "Utilities.h"
// for strcpy
#include <string.h>
#include "com_sun_media_sound_MidiInDeviceProvider.h"


#define MAX_STRING_LENGTH 128


JNIEXPORT jint JNICALL
Java_com_sun_media_sound_MidiInDeviceProvider_nGetNumDevices(JNIEnv* e, jobject thisObj) {

    INT32 numDevices = 0;

    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetNumDevices.\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    numDevices = MIDI_IN_GetNumDevices();
#endif

    TRACE1("Java_com_sun_media_sound_MidiInDeviceProvider_nGetNumDevices returning %d.\n", numDevices);
    return (jint) numDevices;
}


JNIEXPORT jstring JNICALL
Java_com_sun_media_sound_MidiInDeviceProvider_nGetName(JNIEnv* e, jobject thisObj, jint index) {

    char name[MAX_STRING_LENGTH + 1];
    jstring jString = NULL;

    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetName.\n");
    name[0] = 0;

#if USE_PLATFORM_MIDI_IN == TRUE
    MIDI_IN_GetDeviceName((INT32)index, name, (UINT32)MAX_STRING_LENGTH);
#endif

    if (name[0] == 0) {
        strcpy(name, "Unknown name");
    }
    jString = (*e)->NewStringUTF(e, name);
    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetName completed.\n");
    return jString;
}


JNIEXPORT jstring JNICALL
Java_com_sun_media_sound_MidiInDeviceProvider_nGetVendor(JNIEnv* e, jobject thisObj, jint index) {

    char name[MAX_STRING_LENGTH + 1];
    jstring jString = NULL;

    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetVendor.\n");
    name[0] = 0;

#if USE_PLATFORM_MIDI_IN == TRUE
    MIDI_IN_GetDeviceVendor((INT32)index, name, (UINT32)MAX_STRING_LENGTH);
#endif

    if (name[0] == 0) {
        strcpy(name, "Unknown vendor");
    }
    jString = (*e)->NewStringUTF(e, name);
    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetVendor completed.\n");
    return jString;
}


JNIEXPORT jstring JNICALL
Java_com_sun_media_sound_MidiInDeviceProvider_nGetDescription(JNIEnv* e, jobject thisObj, jint index) {

    char name[MAX_STRING_LENGTH + 1];
    jstring jString = NULL;

    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetDescription.\n");
    name[0] = 0;

#if USE_PLATFORM_MIDI_IN == TRUE
    MIDI_IN_GetDeviceDescription((INT32)index, name, (UINT32)MAX_STRING_LENGTH);
#endif

    if (name[0] == 0) {
        strcpy(name, "No details available");
    }
    jString = (*e)->NewStringUTF(e, name);
    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetDescription completed.\n");
    return jString;
}


JNIEXPORT jstring JNICALL
Java_com_sun_media_sound_MidiInDeviceProvider_nGetVersion(JNIEnv* e, jobject thisObj, jint index) {

    char name[MAX_STRING_LENGTH + 1];
    jstring jString = NULL;

    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetVersion.\n");
    name[0] = 0;

#if USE_PLATFORM_MIDI_IN == TRUE
    MIDI_IN_GetDeviceVersion((INT32)index, name, (UINT32)MAX_STRING_LENGTH);
#endif

    if (name[0] == 0) {
        strcpy(name, "Unknown version");
    }
    jString = (*e)->NewStringUTF(e, name);
    TRACE0("Java_com_sun_media_sound_MidiInDeviceProvider_nGetVersion completed.\n");
    return jString;
}
