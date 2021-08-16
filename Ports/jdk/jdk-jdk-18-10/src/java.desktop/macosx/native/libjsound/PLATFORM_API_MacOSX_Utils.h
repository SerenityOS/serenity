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

#include <CoreAudio/CoreAudio.h>
#include <pthread.h>

extern "C" {
#include "Utilities.h"
}


#ifdef USE_ERROR
#define OS_ERROR_END(err) {                     \
    char errStr[32];                            \
    snprintf(errStr, 32, "%d('%c%c%c%c')>", (int)err, (char)(err >> 24), (char)(err >> 16), (char)(err >> 8), (char)err); \
    ERROR1(" ERROR %s\n", errStr);              \
}
#define OS_ERROR0(err, string)                  { ERROR0(string); OS_ERROR_END(err); }
#define OS_ERROR1(err, string, p1)              { ERROR1(string, p1); OS_ERROR_END(err); }
#define OS_ERROR2(err, string, p1, p2)          { ERROR2(string, p1, p2); OS_ERROR_END(err); }
#define OS_ERROR3(err, string, p1, p2, p3)      { ERROR3(string, p1, p2, p3); OS_ERROR_END(err); }
#define OS_ERROR4(err, string, p1, p2, p3, p4)  { ERROR4(string, p1, p2, p3, p4); OS_ERROR_END(err); }
#else
#define OS_ERROR0(err, string)
#define OS_ERROR1(err, string, p1)
#define OS_ERROR2(err, string, p1, p2)
#define OS_ERROR3(err, string, p1, p2, p3)
#define OS_ERROR4(err, string, p1, p2, p3, p4)
#endif


// Simple mutex wrapper class
class MutexLock {
private:
    pthread_mutex_t lockMutex;
public:
    MutexLock() { pthread_mutex_init(&lockMutex, NULL); }
    ~MutexLock() { pthread_mutex_destroy(&lockMutex); }

    void Lock() { pthread_mutex_lock(&lockMutex); }
    void Unlock() { pthread_mutex_unlock(&lockMutex); }

    class Locker {
    public:
        Locker(MutexLock &lock) : pLock(&lock) { pLock->Lock(); }
        ~Locker() { pLock->Unlock(); }
    private:
        MutexLock *pLock;
    };
};


// DirectAudio and Ports need own caches of the device list
class DeviceList {
public:
    DeviceList();
    ~DeviceList();

    OSStatus Refresh();

    int GetCount();
    AudioDeviceID GetDeviceID(int index);
    // stringLength specified length of name, vendor, description & version strings
    bool GetDeviceInfo(int index, AudioDeviceID *deviceID, int stringLength, char *name, char *vendor, char *description, char *version);

private:
    int count;
    AudioDeviceID *devices;
    MutexLock lock;
    void Free();

    static OSStatus NotificationCallback(AudioObjectID inObjectID,
        UInt32 inNumberAddresses, const AudioObjectPropertyAddress inAddresses[], void *inClientData);

};

int MACOSX_DAUDIO_Init();

AudioDeviceID GetDefaultDevice(int isSource);
int GetChannelCount(AudioDeviceID deviceID, int isSource);
float GetSampleRate(AudioDeviceID deviceID, int isSource);


// wrappers for AudioObjectGetPropertyDataSize/AudioObjectGetPropertyData (master element)
OSStatus GetAudioObjectPropertySize(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 *size);
OSStatus GetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 *size, void *data);
OSStatus GetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 size, void *data, int checkSize);

// wrapper for AudioObjectSetPropertyData (kAudioObjectPropertyElementMaster)
OSStatus SetAudioObjectProperty(AudioObjectID object, AudioObjectPropertyScope scope, AudioObjectPropertySelector prop, UInt32 size, void *data);

