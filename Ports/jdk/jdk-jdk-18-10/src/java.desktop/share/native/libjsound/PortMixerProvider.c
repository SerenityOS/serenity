/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

//#define USE_TRACE


#include <jni.h>
#include <jni_util.h>
#include "SoundDefs.h"
#include "Ports.h"
#include "Utilities.h"
#include "com_sun_media_sound_PortMixerProvider.h"


//////////////////////////////////////////// PortMixerProvider ////////////////////////////////////////////

int getPortMixerDescription(int mixerIndex, PortMixerDescription* desc) {
    strcpy(desc->name, "Unknown Name");
    strcpy(desc->vendor, "Unknown Vendor");
    strcpy(desc->description, "Port Mixer");
    strcpy(desc->version, "Unknown Version");
#if USE_PORTS == TRUE
    PORT_GetPortMixerDescription(mixerIndex, desc);
#endif // USE_PORTS
    return TRUE;
}

JNIEXPORT jint JNICALL Java_com_sun_media_sound_PortMixerProvider_nGetNumDevices(JNIEnv *env, jclass cls) {
    INT32 numDevices = 0;

    TRACE0("Java_com_sun_media_sound_PortMixerProvider_nGetNumDevices.\n");

#if USE_PORTS == TRUE
    numDevices = PORT_GetPortMixerCount();
#endif // USE_PORTS

    TRACE1("Java_com_sun_media_sound_PortMixerProvider_nGetNumDevices returning %d.\n", (int) numDevices);

    return (jint)numDevices;
}

JNIEXPORT jobject JNICALL Java_com_sun_media_sound_PortMixerProvider_nNewPortMixerInfo(JNIEnv *env, jclass cls, jint mixerIndex) {
    jclass portMixerInfoClass;
    jmethodID portMixerInfoConstructor;
    PortMixerDescription desc;
    jobject info = NULL;
    jstring name;
    jstring vendor;
    jstring description;
    jstring version;

    TRACE1("Java_com_sun_media_sound_PortMixerProvider_nNewPortMixerInfo(%d).\n", mixerIndex);

    // retrieve class and constructor of PortMixerProvider.PortMixerInfo
    portMixerInfoClass = (*env)->FindClass(env, IMPLEMENTATION_PACKAGE_NAME"/PortMixerProvider$PortMixerInfo");
    if (portMixerInfoClass == NULL) {
        ERROR0("Java_com_sun_media_sound_PortMixerProvider_nNewPortMixerInfo: portMixerInfoClass is NULL\n");
        return NULL;
    }
    portMixerInfoConstructor = (*env)->GetMethodID(env, portMixerInfoClass, "<init>",
                  "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (portMixerInfoConstructor == NULL) {
        ERROR0("Java_com_sun_media_sound_PortMixerProvider_nNewPortMixerInfo: portMixerInfoConstructor is NULL\n");
        return NULL;
    }

    if (getPortMixerDescription(mixerIndex, &desc)) {
        // create a new PortMixerInfo object and return it
        name = (*env)->NewStringUTF(env, desc.name);
        CHECK_NULL_RETURN(name, info);
        vendor = (*env)->NewStringUTF(env, desc.vendor);
        CHECK_NULL_RETURN(vendor, info);
        description = (*env)->NewStringUTF(env, desc.description);
        CHECK_NULL_RETURN(description, info);
        version = (*env)->NewStringUTF(env, desc.version);
        CHECK_NULL_RETURN(version, info);
        info = (*env)->NewObject(env, portMixerInfoClass,
                                 portMixerInfoConstructor, mixerIndex,
                                 name, vendor, description, version);
    }

    TRACE0("Java_com_sun_media_sound_PortMixerProvider_nNewPortMixerInfo succeeded.\n");
    return info;
}
