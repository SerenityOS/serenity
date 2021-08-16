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

#ifndef PLATFORM_API_LINUXOS_ALSA_COMMONUTILS_H_INCLUDED
#define PLATFORM_API_LINUXOS_ALSA_COMMONUTILS_H_INCLUDED

#define ALSA_VERSION_PROC_FILE "/proc/asound/version"
#define ALSA_HARDWARE "hw"
#define ALSA_HARDWARE_CARD ALSA_HARDWARE":%d"
#define ALSA_HARDWARE_DEVICE ALSA_HARDWARE_CARD",%d"
#define ALSA_HARDWARE_SUBDEVICE ALSA_HARDWARE_DEVICE",%d"

#define ALSA_PLUGHARDWARE "plughw"
#define ALSA_DEFAULT_DEVICE_NAME "default"

#define ALSA_DEFAULT_DEVICE_ID (0)

#define ALSA_PCM     (0)
#define ALSA_RAWMIDI (1)

// for use in info objects
#define ALSA_VENDOR "ALSA (http://www.alsa-project.org)"

// Environment variable for inclusion of subdevices in device listing.
// If this variable is unset or "no", then subdevices are ignored, and
// it's ALSA's choice which one to use (enables hardware mixing)
#define ENV_ENUMERATE_PCM_SUBDEVICES "ALSA_ENUMERATE_PCM_SUBDEVICES"

// if defined, subdevices are listed.
//#undef ALSA_MIDI_ENUMERATE_SUBDEVICES
#define ALSA_MIDI_ENUMERATE_SUBDEVICES

// must be called before any ALSA calls
void initAlsaSupport();

/* if true (non-zero), ALSA sub devices should be listed as separate devices
 */
int needEnumerateSubdevices(int isMidi);


/*
 * deviceID contains packed card, device and subdevice numbers
 * each number takes 10 bits
 * "default" device has id == ALSA_DEFAULT_DEVICE_ID
 */
UINT32 encodeDeviceID(int card, int device, int subdevice);

void decodeDeviceID(UINT32 deviceID, int* card, int* device, int* subdevice,
                    int isMidi);

void getDeviceStringFromDeviceID(char* buffer, UINT32 deviceID,
                                 int usePlugHw, int isMidi);

void getALSAVersion(char* buffer, int len);


#endif // PLATFORM_API_LINUXOS_ALSA_COMMONUTILS_H_INCLUDED
