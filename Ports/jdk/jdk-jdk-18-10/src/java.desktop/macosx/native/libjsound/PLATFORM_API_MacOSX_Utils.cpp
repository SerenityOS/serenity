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

//#define USE_TRACE
//#define USE_ERROR

#include "PLATFORM_API_MacOSX_Utils.h"

int MACOSX_DAUDIO_Init() {
    static int initialized = 0;
    if (!initialized) {
        CFRunLoopRef runLoop = NULL;

        OSStatus err = SetAudioObjectProperty(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal,
            kAudioHardwarePropertyRunLoop, sizeof(CFRunLoopRef), &runLoop);

        if (err) {
            OS_ERROR0(err, "MACOSX_DAUDIO_Init(kAudioHardwarePropertyRunLoop)");
        } else {
            TRACE0("MACOSX_DAUDIO_Init(kAudioHardwarePropertyRunLoop): OK\n");
            initialized = 1;
        }
    }
    return initialized;
}

DeviceList::DeviceList(): count(0), devices(NULL) {
    MACOSX_DAUDIO_Init();

    AudioObjectPropertyAddress address = {kAudioHardwarePropertyDevices,
        kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster};
    OSStatus err = AudioObjectAddPropertyListener(kAudioObjectSystemObject, &address, NotificationCallback, this);
    if (err) {
        OS_ERROR0(err, "AudioObjectAddPropertyListener(kAudioHardwarePropertyDevices)");
    } else {
        TRACE0("AudioObjectAddPropertyListener(kAudioHardwarePropertyDevices): OK\n");
    }
}

DeviceList::~DeviceList() {
    Free();

    AudioObjectPropertyAddress address = {kAudioHardwarePropertyDevices,
        kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster};
    AudioObjectRemovePropertyListener(kAudioObjectSystemObject, &address, NotificationCallback, this);
}

OSStatus DeviceList::Refresh() {
    MutexLock::Locker locker(lock);
    Free();

    OSStatus err;
    UInt32 size;
    err = GetAudioObjectPropertySize(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal, kAudioHardwarePropertyDevices, &size);
    if (err == noErr) {
        devices = (AudioDeviceID *)malloc(size);
        err = GetAudioObjectProperty(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal, kAudioHardwarePropertyDevices, &size, devices);
        if (err == noErr) {
            count = size/sizeof(AudioDeviceID);
        }
    }
    if (err) {
        OS_ERROR0(err, "DeviceList::Refresh");
        Free();
    }
#ifdef USE_TRACE
    TRACE1("<<DeviceList::Refresh, %d devices {", count);
    for (int i=0; i<count; i++) {
        if (i > 0)
            TRACE0(", ");
        TRACE1("0x%x", (int)devices[i]);
    }
    TRACE0("}\n");
#endif

    return err;
}

int DeviceList::GetCount() {
    MutexLock::Locker locker(lock);
    return count;
}

AudioDeviceID DeviceList::GetDeviceID(int index) {
    MutexLock::Locker locker(lock);
    return index < 0 ? 0 : index >= count ? 0 : devices[index];
}

bool DeviceList::GetDeviceInfo(int index, AudioDeviceID *pDeviceID, int stringLength, char *name, char *vendor, char *description, char *version) {
    MutexLock::Locker locker(lock);
    if (index < 0 || index >= count) {
        return false;
    }

    AudioDeviceID deviceID = devices[index];
    if (pDeviceID != NULL)
        *pDeviceID = deviceID;

    OSStatus err = noErr;

    if (name != NULL || description != NULL) {
        CFStringRef cfName = NULL;
        err = GetAudioObjectProperty(deviceID, kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyName, sizeof(cfName), &cfName, 1);
        if (err == noErr) {
            if (name != NULL)
                CFStringGetCString(cfName, name, stringLength, kCFStringEncodingUTF8);
            if (description)
                CFStringGetCString(cfName, description, stringLength, kCFStringEncodingUTF8);
            CFRelease(cfName);
        }
    }

    if (vendor != NULL) {
        CFStringRef cfManufacturer = NULL;
        err = GetAudioObjectProperty(deviceID, kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyManufacturer, sizeof(cfManufacturer), &cfManufacturer, 1);
        if (err == noErr) {
            CFStringGetCString(cfManufacturer, vendor, stringLength, kCFStringEncodingUTF8);
            CFRelease(cfManufacturer);
        }
    }

    return true;
}

void DeviceList::Free() {
    if (devices != NULL) {
        free(devices);
        devices = NULL;
        count = 0;
    }
}

/*static*/
OSStatus DeviceList::NotificationCallback(AudioObjectID inObjectID,
    UInt32 inNumberAddresses, const AudioObjectPropertyAddress inAddresses[], void *inClientData)
{
    DeviceList *pThis = (DeviceList *)inClientData;

    for (UInt32 i=0; i<inNumberAddresses; i++) {
        switch (inAddresses[i].mSelector) {
        case kAudioHardwarePropertyDevices:
            TRACE0("NOTIFICATION: kAudioHardwarePropertyDevices\n");
            break;
        }
    }

    return noErr;
}



AudioDeviceID GetDefaultDevice(int isSource) {
    AudioDeviceID deviceID;
    OSStatus err = GetAudioObjectProperty(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal,
        isSource ? kAudioHardwarePropertyDefaultOutputDevice : kAudioHardwarePropertyDefaultInputDevice,
        sizeof(deviceID), &deviceID, 1);
    if (err) {
        OS_ERROR1(err, "GetDefaultDevice(isSource=%d)", isSource);
        return 0;
    }
    return deviceID;
}

int GetChannelCount(AudioDeviceID deviceID, int isSource) {
    int result = 0;
    OSStatus err;
    UInt32 size, i;
    AudioObjectPropertyScope scope = isSource ? kAudioDevicePropertyScopeOutput : kAudioDevicePropertyScopeInput;

    err = GetAudioObjectPropertySize(deviceID, scope, kAudioDevicePropertyStreamConfiguration, &size);
    if (err) {
        OS_ERROR2(err, "GetChannelCount(getSize), deviceID=0x%x, isSource=%d", (int)deviceID, isSource);
    } else {
        AudioBufferList *pBufferList = (AudioBufferList *)malloc(size);
        memset(pBufferList, 0, size);
        err = GetAudioObjectProperty(deviceID, scope, kAudioDevicePropertyStreamConfiguration, &size, pBufferList);
        if (err == noErr) {
            for (i=0; i<pBufferList->mNumberBuffers; i++) {
                result += pBufferList->mBuffers[i].mNumberChannels;
            }
        } else {
            OS_ERROR2(err, "GetChannelCount(getData), deviceID=0x%x, isSource=%d", (int)deviceID, isSource);
        }
        free(pBufferList);
    }
    TRACE2("GetChannelCount (deviceID=0x%x): total %d channels\n", (int)deviceID, result);
    return result;
}

float GetSampleRate(AudioDeviceID deviceID, int isSource) {
    Float64 result;
    AudioObjectPropertyScope scope = isSource ? kAudioDevicePropertyScopeOutput : kAudioDevicePropertyScopeInput;
    OSStatus err = GetAudioObjectProperty(deviceID, scope, kAudioDevicePropertyActualSampleRate, sizeof(result), &result, 1);
    if (err) {
        OS_ERROR2(err, "GetSampleRate(ActualSampleRate), deviceID=0x%x, isSource=%d", (int)deviceID, isSource);
        // try to get NominalSampleRate
        err = GetAudioObjectProperty(deviceID, scope, kAudioDevicePropertyNominalSampleRate, sizeof(result), &result, 1);
        if (err) {
            OS_ERROR2(err, "GetSampleRate(NominalSampleRate), deviceID=0x%x, isSource=%d", (int)deviceID, isSource);
            return 0;
        }
    }
    return (float)result;
}


OSStatus GetAudioObjectPropertySize(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 *size)
{
    const AudioObjectPropertyAddress address = {prop, scope, kAudioObjectPropertyElementMaster};
    OSStatus err;

    err = AudioObjectGetPropertyDataSize(object, &address, 0, NULL, size);

    return err;
}

OSStatus GetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 *size, void *data)
{
    const AudioObjectPropertyAddress address = {prop, scope, kAudioObjectPropertyElementMaster};
    OSStatus err;

    err = AudioObjectGetPropertyData(object, &address, 0, NULL, size, data);

    return err;
}

OSStatus GetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 size, void *data, int checkSize)
{
    const AudioObjectPropertyAddress address = {prop, scope, kAudioObjectPropertyElementMaster};
    UInt32 oldSize = size;
    OSStatus err;

    err = AudioObjectGetPropertyData(object, &address, 0, NULL, &size, data);

    if (!err && checkSize && size != oldSize)
        return kAudioHardwareBadPropertySizeError;
    return err;
}

// wrapper for AudioObjectSetPropertyData (kAudioObjectPropertyElementMaster)
OSStatus SetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 size, void *data)
{
    AudioObjectPropertyAddress address = {prop, scope, kAudioObjectPropertyElementMaster};

    OSStatus err = AudioObjectSetPropertyData(object, &address, 0, NULL, size, data);

    return err;
}
