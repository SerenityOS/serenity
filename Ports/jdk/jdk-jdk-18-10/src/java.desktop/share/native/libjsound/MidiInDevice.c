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

/*****************************************************************************/
/*
**      Native functions for interfacing Java with the native implementation
**      of PlatformMidi.h's functions.
*/
/*****************************************************************************/

#define USE_ERROR
#define USE_TRACE


#include <jni.h>
/* for memcpy */
#include <string.h>
#include "SoundDefs.h"
#include "PlatformMidi.h"
#include "com_sun_media_sound_MidiInDevice.h"


JNIEXPORT jlong JNICALL
Java_com_sun_media_sound_MidiInDevice_nOpen(JNIEnv* e, jobject thisObj, jint index) {

    MidiDeviceHandle* deviceHandle = NULL;
    INT32 err = MIDI_NOT_SUPPORTED;

    TRACE1("> Java_com_sun_media_sound_MidiInDevice_nOpen: index: %d\n", index);

#if USE_PLATFORM_MIDI_IN == TRUE
    err = MIDI_IN_OpenDevice((INT32) index, &deviceHandle);
#endif

    /* $$mp 2003-08-28:
       So far, the return value (err) hasn't been taken into account.
       Now, it is also expected to be MIDI_SUCCESS (0).
       This works for Linux, but has to be checked on other platforms.

       It would be better to settle on one method of signaling error:
       either returned error codes or a NULL handle. If the latter is used,
       the return value should be removed from the signature of
       MIDI_IN_OpenDevice.
    */
    // if we didn't get a valid handle, throw a MidiUnavailableException
    if (!deviceHandle || err != MIDI_SUCCESS) {
        deviceHandle = NULL;
        ERROR0("Java_com_sun_media_sound_MidiInDevice_nOpen: ");
        ThrowJavaMessageException(e, JAVA_MIDI_PACKAGE_NAME"/MidiUnavailableException",
                                  MIDI_IN_InternalGetErrorString(err));
    } else {
        TRACE0("< Java_com_sun_media_sound_MidiInDevice_nOpen succeeded\n");
    }
    return (jlong) (UINT_PTR) deviceHandle;
}


JNIEXPORT void JNICALL
Java_com_sun_media_sound_MidiInDevice_nClose(JNIEnv* e, jobject thisObj, jlong deviceHandle) {

    TRACE0("> Java_com_sun_media_sound_MidiInDevice_nClose.\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    MIDI_IN_CloseDevice((MidiDeviceHandle*) (UINT_PTR) deviceHandle);
#endif

    TRACE0("< Java_com_sun_media_sound_MidiInDevice_nClose succeeded\n");
}


JNIEXPORT void JNICALL
Java_com_sun_media_sound_MidiInDevice_nStart(JNIEnv* e, jobject thisObj, jlong deviceHandle) {

    INT32 err = MIDI_NOT_SUPPORTED;

    TRACE0("> Java_com_sun_media_sound_MidiInDevice_nStart.\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    err = MIDI_IN_StartDevice((MidiDeviceHandle*) (UINT_PTR) deviceHandle);
#endif

    if (err != MIDI_SUCCESS) {
        ERROR0("Java_com_sun_media_sound_MidiInDevice_nStart: ");
        ThrowJavaMessageException(e, JAVA_MIDI_PACKAGE_NAME"/MidiUnavailableException",
                                  MIDI_IN_InternalGetErrorString(err));
    } else {
        TRACE0("< Java_com_sun_media_sound_MidiInDevice_nStart succeeded\n");
    }
}


JNIEXPORT void JNICALL
Java_com_sun_media_sound_MidiInDevice_nStop(JNIEnv* e, jobject thisObj, jlong deviceHandle) {

    TRACE0("> Java_com_sun_media_sound_MidiInDevice_nStop.\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    // stop the device and remove all queued events for this device handle
    MIDI_IN_StopDevice((MidiDeviceHandle*) (UINT_PTR) deviceHandle);
#endif

    TRACE0("< Java_com_sun_media_sound_MidiInDevice_nStop succeeded\n");
}

JNIEXPORT jlong JNICALL
Java_com_sun_media_sound_MidiInDevice_nGetTimeStamp(JNIEnv* e, jobject thisObj, jlong deviceHandle) {

    jlong ret = -1;

    TRACE0("Java_com_sun_media_sound_MidiInDevice_nGetTimeStamp.\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    ret = (jlong) MIDI_IN_GetTimeStamp((MidiDeviceHandle*) (UINT_PTR) deviceHandle);
#endif

    /* Handle error codes. */
    if (ret < -1) {
        ERROR1("Java_com_sun_media_sound_MidiInDevice_nGetTimeStamp: MIDI_IN_GetTimeStamp returned %lld\n", ret);
        ret = -1;
    }
    return ret;
}


JNIEXPORT void JNICALL
Java_com_sun_media_sound_MidiInDevice_nGetMessages(JNIEnv* e, jobject thisObj, jlong deviceHandle) {

#if USE_PLATFORM_MIDI_IN == TRUE
    MidiMessage* pMessage;
    jclass javaClass = NULL;
    jmethodID callbackShortMessageMethodID = NULL;
    jmethodID callbackLongMessageMethodID = NULL;
#endif

    TRACE0("> Java_com_sun_media_sound_MidiInDevice_nGetMessages\n");

#if USE_PLATFORM_MIDI_IN == TRUE
    while ((pMessage = MIDI_IN_GetMessage((MidiDeviceHandle*) (UINT_PTR) deviceHandle))) {
        if ((javaClass == NULL) || (callbackShortMessageMethodID == NULL)) {
            if (!thisObj) {
                ERROR0("MidiInDevice: Java_com_sun_media_sound_MidiInDevice_nGetMessages: thisObj is NULL\n");
                return;
            }

            if (javaClass == NULL) {
                javaClass = (*e)->GetObjectClass(e, thisObj);
                if (javaClass == NULL) {
                    ERROR0("MidiInDevice: Java_com_sun_media_sound_MidiInDevice_nGetMessages: javaClass is NULL\n");
                    return;
                }
            }

            if (callbackShortMessageMethodID == NULL) {
                // save the callbackShortMessage callback method id.
                // this is valid as long as the class is not unloaded.
                callbackShortMessageMethodID = (*e)->GetMethodID(e, javaClass, "callbackShortMessage", "(IJ)V");
                if (callbackShortMessageMethodID == 0) {
                    ERROR0("MidiInDevice: Java_com_sun_media_sound_MidiInDevice_nGetMessages: callbackShortMessageMethodID is 0\n");
                    return;
                }
            }
            if (callbackLongMessageMethodID == NULL) {
                // save the callbackLongMessage callback method id.
                // this is valid as long as the class is not unloaded.
                callbackLongMessageMethodID = (*e)->GetMethodID(e, javaClass, "callbackLongMessage", "([BJ)V");
                if (callbackLongMessageMethodID == 0) {
                    ERROR0("MidiInDevice: Java_com_sun_media_sound_MidiInDevice_nGetMessages: callbackLongMessageMethodID is 0\n");
                    return;
                }
            }
        }

        switch ((int)pMessage->type) {
        case SHORT_MESSAGE: {
            jint msg = (jint)pMessage->data.s.packedMsg;
            jlong ts = (jlong)pMessage->timestamp;
            TRACE0("Java_com_sun_media_sound_MidiInDevice_nGetMessages: got SHORT_MESSAGE\n");
            // now we can put this message object back in the queue
            MIDI_IN_ReleaseMessage((MidiDeviceHandle*) (UINT_PTR) deviceHandle, pMessage);
            // and notify Java space
            (*e)->CallVoidMethod(e, thisObj, callbackShortMessageMethodID, msg, ts);
            break;
        }

        case LONG_MESSAGE: {
            jlong ts = (jlong)pMessage->timestamp;
            jbyteArray jData;
            UBYTE* data;
            int isSXCont = 0;
            TRACE0("Java_com_sun_media_sound_MidiInDevice_nGetMessages: got LONG_MESSAGE\n");
            if ((*(pMessage->data.l.data) != 0xF0)
                && (*(pMessage->data.l.data) != 0xF7)) {
                // this is a continued sys ex message
                // need to prepend 0xF7
                isSXCont = 1;
            }
            jData = (*e)->NewByteArray(e, pMessage->data.l.size + isSXCont);
            if (!jData) {
                ERROR0("Java_com_sun_media_sound_MidiInDevice_nGetMessages: cannot create long byte array.\n");
                break;
            }
            data = (UBYTE*) ((*e)->GetByteArrayElements(e, jData, NULL));
            if (!data) {
                ERROR0("MidiInDevice: Java_com_sun_media_sound_MidiInDevice_nGetMessages: array data is NULL\n");
                break;
            }
            // finally copy the long message
            memcpy(data + isSXCont, pMessage->data.l.data, pMessage->data.l.size);

            // now we can put this message object back in the queue
            MIDI_IN_ReleaseMessage((MidiDeviceHandle*) (UINT_PTR) deviceHandle, pMessage);

            // if this is a patched continued sys ex message, prepend 0xF7
            if (isSXCont) {
                *data = 0xF7;
            }

            // commit the byte array
            (*e)->ReleaseByteArrayElements(e, jData, (jbyte*) data, (jint) 0);

            (*e)->CallVoidMethod(e, thisObj, callbackLongMessageMethodID, jData, ts);
            // release local reference to array: not needed anymore.
            (*e)->DeleteLocalRef(e, jData);
            break;
        }

        default:
            // put this message object back in the queue
            MIDI_IN_ReleaseMessage((MidiDeviceHandle*) (UINT_PTR) deviceHandle, pMessage);
            ERROR1("Java_com_sun_media_sound_MidiInDevice_nGetMessages: got unsupported message, type %d\n", pMessage->type);
            break;
        } // switch
    }

#endif // USE_PLATFORM_MIDI_IN

    TRACE0("< Java_com_sun_media_sound_MidiInDevice_nGetMessages returning\n");
}
