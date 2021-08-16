/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
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

#ifndef PORTS_INCLUDED
#define PORTS_INCLUDED


#include "SoundDefs.h"
// for memset
#include <string.h>
#include "Configure.h"  // put flags for debug msgs etc. here
#include "Utilities.h"
#include <com_sun_media_sound_PortMixer.h>


/* *********************** PORT TYPES (for all platforms) ******************************* */

#define PORT_SRC_UNKNOWN      (com_sun_media_sound_PortMixer_SRC_UNKNOWN)
#define PORT_SRC_MICROPHONE   (com_sun_media_sound_PortMixer_SRC_MICROPHONE)
#define PORT_SRC_LINE_IN      (com_sun_media_sound_PortMixer_SRC_LINE_IN)
#define PORT_SRC_COMPACT_DISC (com_sun_media_sound_PortMixer_SRC_COMPACT_DISC)
#define PORT_SRC_MASK         (com_sun_media_sound_PortMixer_SRC_MASK)
#define PORT_DST_UNKNOWN      (com_sun_media_sound_PortMixer_DST_UNKNOWN)
#define PORT_DST_SPEAKER      (com_sun_media_sound_PortMixer_DST_SPEAKER)
#define PORT_DST_HEADPHONE    (com_sun_media_sound_PortMixer_DST_HEADPHONE)
#define PORT_DST_LINE_OUT     (com_sun_media_sound_PortMixer_DST_LINE_OUT)
#define PORT_DST_MASK         (com_sun_media_sound_PortMixer_DST_MASK)

#define PORT_STRING_LENGTH 200

typedef struct tag_PortMixerDescription {
    char name[PORT_STRING_LENGTH];
    char vendor[PORT_STRING_LENGTH];
    char description[PORT_STRING_LENGTH];
    char version[PORT_STRING_LENGTH];
} PortMixerDescription;


// for BooleanControl.Type
#define CONTROL_TYPE_MUTE        ((char*) 1)
#define CONTROL_TYPE_SELECT      ((char*) 2)

// for FloatControl.Type
#define CONTROL_TYPE_BALANCE     ((char*) 1)
#define CONTROL_TYPE_MASTER_GAIN ((char*) 2)
#define CONTROL_TYPE_PAN         ((char*) 3)
#define CONTROL_TYPE_VOLUME      ((char*) 4)
#define CONTROL_TYPE_MAX         4

// method definitions

/* controlID: unique ID for this control
 * type: string that is used to construct the BooleanControl.Type, or CONTROL_TYPE_MUTE
 * creator: pointer to the creator struct provided by PORT_GetControls
 * returns an opaque pointer to the created control
 */
typedef void* (*PORT_NewBooleanControlPtr)(void* creator, void* controlID, char* type);

/* type: string that is used to construct the CompoundControl.Type
 * controls: an array of opaque controls returned by the CreateXXXControlPtr functions
 * controlCount: number of elements in controls
 * creator: pointer to the creator struct provided by PORT_GetControls
 * returns an opaque pointer to the created control
 */
typedef void* (*PORT_NewCompoundControlPtr)(void* creator, char* type, void** controls, int controlCount);

/* controlID: unique ID for this control
 * type: string that is used to construct the FloatControl.Type, or one of
 *       CONTROL_TYPE_BALANCE, CONTROL_TYPE_MASTER_GAIN, CONTROL_TYPE_PAN, CONTROL_TYPE_VOLUME
 * creator: pointer to the creator struct provided by PORT_GetControls
 * returns an opaque pointer to the created control
 */
typedef void* (*PORT_NewFloatControlPtr)(void* creator, void* controlID, char* type,
              float min, float max, float precision, const char* units);

/* control: The control to add to current port
 * creator: pointer to the creator struct provided by PORT_GetControls
 * returns TRUE or FALSE
 */
typedef int (*PORT_AddControlPtr)(void* creator, void* control);

// struct for dynamically instantiating the controls from platform dependent code
// without creating a dependency from the platform code to JNI

typedef struct tag_PortControlCreator {
    PORT_NewBooleanControlPtr newBooleanControl;
    PORT_NewCompoundControlPtr newCompoundControl;
    PORT_NewFloatControlPtr newFloatControl;
    PORT_AddControlPtr addControl;
} PortControlCreator;

#if (USE_PORTS == TRUE)

// the following methods need to be implemented by the platform dependent code
INT32 PORT_GetPortMixerCount();
INT32 PORT_GetPortMixerDescription(INT32 mixerIndex, PortMixerDescription* description);
void* PORT_Open(INT32 mixerIndex);
void  PORT_Close(void* id);

INT32 PORT_GetPortCount(void* id);
INT32 PORT_GetPortType(void* id, INT32 portIndex);
INT32 PORT_GetPortName(void* id, INT32 portIndex, char* name, INT32 len);
void  PORT_GetControls(void* id, INT32 portIndex, PortControlCreator* creator);
float PORT_GetFloatValue(void* controlID);
INT32 PORT_GetIntValue(void* controlIDV);
void  PORT_SetFloatValue(void* controlID, float value);
void  PORT_SetIntValue(void* controlIDV, INT32 value);

#endif // USE_PORTS

#endif // PORTS_INCLUDED
