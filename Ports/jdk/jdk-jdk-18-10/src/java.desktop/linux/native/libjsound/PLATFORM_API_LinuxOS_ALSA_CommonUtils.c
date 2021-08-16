/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "PLATFORM_API_LinuxOS_ALSA_CommonUtils.h"

static void alsaDebugOutput(const char *file, int line, const char *function, int err, const char *fmt, ...) {
#ifdef USE_ERROR
    va_list args;
    va_start(args, fmt);
    printf("%s:%d function %s: error %d: %s\n", file, line, function, err, snd_strerror(err));
    if (strlen(fmt) > 0) {
        vprintf(fmt, args);
    }
    va_end(args);
#endif
}

static int alsa_inited = 0;
static int alsa_enumerate_pcm_subdevices = FALSE; // default: no
static int alsa_enumerate_midi_subdevices = FALSE; // default: no

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

void initAlsaSupport() {
    char* enumerate;
    if (!alsa_inited) {
        alsa_inited = TRUE;
        snd_lib_error_set_handler(&alsaDebugOutput);

        enumerate = getenv(ENV_ENUMERATE_PCM_SUBDEVICES);
        if (enumerate != NULL && strlen(enumerate) > 0
            && (enumerate[0] != 'f')   // false
            && (enumerate[0] != 'F')   // False
            && (enumerate[0] != 'n')   // no
            && (enumerate[0] != 'N')) { // NO
            alsa_enumerate_pcm_subdevices = TRUE;
        }
#ifdef ALSA_MIDI_ENUMERATE_SUBDEVICES
        alsa_enumerate_midi_subdevices = TRUE;
#endif
    }
}


/* if true (non-zero), ALSA sub devices should be listed as separate devices
 */
int needEnumerateSubdevices(int isMidi) {
    initAlsaSupport();
    return isMidi ? alsa_enumerate_midi_subdevices
                  : alsa_enumerate_pcm_subdevices;
}


/*
 * deviceID contains packed card, device and subdevice numbers
 * each number takes 10 bits
 * "default" device has id == ALSA_DEFAULT_DEVICE_ID
 */
UINT32 encodeDeviceID(int card, int device, int subdevice) {
    return (((card & 0x3FF) << 20) | ((device & 0x3FF) << 10)
           | (subdevice & 0x3FF)) + 1;
}


void decodeDeviceID(UINT32 deviceID, int* card, int* device, int* subdevice,
                    int isMidi) {
    deviceID--;
    *card = (deviceID >> 20) & 0x3FF;
    *device = (deviceID >> 10) & 0x3FF;
    if (needEnumerateSubdevices(isMidi)) {
        *subdevice = deviceID  & 0x3FF;
    } else {
        *subdevice = -1; // ALSA will choose any subdevices
    }
}


void getDeviceString(char* buffer, int card, int device, int subdevice,
                     int usePlugHw, int isMidi) {
    if (needEnumerateSubdevices(isMidi)) {
        sprintf(buffer, "%s:%d,%d,%d",
                        usePlugHw ? ALSA_PLUGHARDWARE : ALSA_HARDWARE,
                        card, device, subdevice);
    } else {
        sprintf(buffer, "%s:%d,%d",
                        usePlugHw ? ALSA_PLUGHARDWARE : ALSA_HARDWARE,
                        card, device);
    }
}


void getDeviceStringFromDeviceID(char* buffer, UINT32 deviceID,
                                 int usePlugHw, int isMidi) {
    int card, device, subdevice;

    if (deviceID == ALSA_DEFAULT_DEVICE_ID) {
        strcpy(buffer, ALSA_DEFAULT_DEVICE_NAME);
    } else {
        decodeDeviceID(deviceID, &card, &device, &subdevice, isMidi);
        getDeviceString(buffer, card, device, subdevice, usePlugHw, isMidi);
    }
}


static int hasGottenALSAVersion = FALSE;
#define ALSAVersionString_LENGTH 200
static char ALSAVersionString[ALSAVersionString_LENGTH];

void getALSAVersion(char* buffer, int len) {
    if (!hasGottenALSAVersion) {
        // get alsa version from proc interface
        FILE* file;
        int curr, len, totalLen, inVersionString;
        file = fopen(ALSA_VERSION_PROC_FILE, "r");
        ALSAVersionString[0] = 0;
        if (file) {
            if (NULL != fgets(ALSAVersionString, ALSAVersionString_LENGTH, file)) {
                // parse for version number
                totalLen = strlen(ALSAVersionString);
                inVersionString = FALSE;
                len = 0;
                curr = 0;
                while (curr < totalLen) {
                    if (!inVersionString) {
                        // is this char the beginning of a version string ?
                        if (ALSAVersionString[curr] >= '0'
                            && ALSAVersionString[curr] <= '9') {
                            inVersionString = TRUE;
                        }
                    }
                    if (inVersionString) {
                        // the version string ends with white space
                        if (ALSAVersionString[curr] <= 32) {
                            break;
                        }
                        if (curr != len) {
                            // copy this char to the beginning of the string
                            ALSAVersionString[len] = ALSAVersionString[curr];
                        }
                        len++;
                    }
                    curr++;
                }
                // remove trailing dots
                while ((len > 0) && (ALSAVersionString[len - 1] == '.')) {
                    len--;
                }
                // null terminate
                ALSAVersionString[len] = 0;
            }
            fclose(file);
            hasGottenALSAVersion = TRUE;
        }
    }
    strncpy(buffer, ALSAVersionString, len);
}


/* end */
