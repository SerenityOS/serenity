/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
//#define USE_TRACE

#include "Ports.h"
#include "PLATFORM_API_LinuxOS_ALSA_CommonUtils.h"
#include <alsa/asoundlib.h>

#if USE_PORTS == TRUE

#define MAX_ELEMS (300)
#define MAX_CONTROLS (MAX_ELEMS * 4)

#define CHANNELS_MONO (SND_MIXER_SCHN_LAST + 1)
#define CHANNELS_STEREO (SND_MIXER_SCHN_LAST + 2)

typedef struct {
    snd_mixer_elem_t* elem;
    INT32 portType; /* one of PORT_XXX_xx */
    char* controlType; /* one of CONTROL_TYPE_xx */
    /* Values: either SND_MIXER_SCHN_FRONT_xx, CHANNELS_MONO or CHANNELS_STEREO.
       For SND_MIXER_SCHN_FRONT_xx, exactly this channel is set/retrieved directly.
       For CHANNELS_MONO, ALSA channel SND_MIXER_SCHN_MONO is set/retrieved directly.
       For CHANNELS_STEREO, ALSA channels SND_MIXER_SCHN_FRONT_LEFT and SND_MIXER_SCHN_FRONT_RIGHT
       are set after a calculation that takes balance into account. Retrieved? Average of both
       channels? (Using a cached value is not a good idea since the value in the HW may have been
       altered.) */
    INT32 channel;
} PortControl;


typedef struct tag_PortMixer {
    snd_mixer_t* mixer_handle;
    /* Number of array elements used in elems and types. */
    int numElems;
    snd_mixer_elem_t** elems;
    /* Array of port types (PORT_SRC_UNKNOWN etc.). Indices are the same as in elems. */
    INT32* types;
    /* Number of array elements used in controls. */
    int numControls;
    PortControl* controls;
} PortMixer;


///// implemented functions of Ports.h

INT32 PORT_GetPortMixerCount() {
    INT32 mixerCount;
    int card;
    char devname[16];
    int err;
    snd_ctl_t *handle;
    snd_ctl_card_info_t* info;

    TRACE0("> PORT_GetPortMixerCount\n");

    initAlsaSupport();

    snd_ctl_card_info_malloc(&info);
    card = -1;
    mixerCount = 0;
    if (snd_card_next(&card) >= 0) {
        while (card >= 0) {
            sprintf(devname, ALSA_HARDWARE_CARD, card);
            TRACE1("PORT_GetPortMixerCount: Opening alsa device \"%s\"...\n", devname);
            err = snd_ctl_open(&handle, devname, 0);
            if (err < 0) {
                ERROR2("ERROR: snd_ctl_open, card=%d: %s\n", card, snd_strerror(err));
            } else {
                mixerCount++;
                snd_ctl_close(handle);
            }
            if (snd_card_next(&card) < 0) {
                break;
            }
        }
    }
    snd_ctl_card_info_free(info);
    TRACE0("< PORT_GetPortMixerCount\n");
    return mixerCount;
}


INT32 PORT_GetPortMixerDescription(INT32 mixerIndex, PortMixerDescription* description) {
    snd_ctl_t* handle;
    snd_ctl_card_info_t* card_info;
    char devname[16];
    int err;
    char buffer[100];

    TRACE0("> PORT_GetPortMixerDescription\n");
    snd_ctl_card_info_malloc(&card_info);

    sprintf(devname, ALSA_HARDWARE_CARD, (int) mixerIndex);
    TRACE1("Opening alsa device \"%s\"...\n", devname);
    err = snd_ctl_open(&handle, devname, 0);
    if (err < 0) {
        ERROR2("ERROR: snd_ctl_open, card=%d: %s\n", (int) mixerIndex, snd_strerror(err));
        return FALSE;
    }
    err = snd_ctl_card_info(handle, card_info);
    if (err < 0) {
        ERROR2("ERROR: snd_ctl_card_info, card=%d: %s\n", (int) mixerIndex, snd_strerror(err));
    }
    strncpy(description->name, snd_ctl_card_info_get_id(card_info), PORT_STRING_LENGTH - 1);
    sprintf(buffer, " [%s]", devname);
    strncat(description->name, buffer, PORT_STRING_LENGTH - 1 - strlen(description->name));
    strncpy(description->vendor, "ALSA (http://www.alsa-project.org)", PORT_STRING_LENGTH - 1);
    strncpy(description->description, snd_ctl_card_info_get_name(card_info), PORT_STRING_LENGTH - 1);
    strncat(description->description, ", ", PORT_STRING_LENGTH - 1 - strlen(description->description));
    strncat(description->description, snd_ctl_card_info_get_mixername(card_info), PORT_STRING_LENGTH - 1 - strlen(description->description));
    getALSAVersion(description->version, PORT_STRING_LENGTH - 1);

    snd_ctl_close(handle);
    snd_ctl_card_info_free(card_info);
    TRACE0("< PORT_GetPortMixerDescription\n");
    return TRUE;
}


void* PORT_Open(INT32 mixerIndex) {
    char devname[16];
    snd_mixer_t* mixer_handle;
    int err;
    PortMixer* handle;

    TRACE0("> PORT_Open\n");
    sprintf(devname, ALSA_HARDWARE_CARD, (int) mixerIndex);
    if ((err = snd_mixer_open(&mixer_handle, 0)) < 0) {
        ERROR2("Mixer %s open error: %s", devname, snd_strerror(err));
        return NULL;
    }
    if ((err = snd_mixer_attach(mixer_handle, devname)) < 0) {
        ERROR2("Mixer attach %s error: %s", devname, snd_strerror(err));
        snd_mixer_close(mixer_handle);
        return NULL;
    }
    if ((err = snd_mixer_selem_register(mixer_handle, NULL, NULL)) < 0) {
        ERROR1("Mixer register error: %s", snd_strerror(err));
        snd_mixer_close(mixer_handle);
        return NULL;
    }
    err = snd_mixer_load(mixer_handle);
    if (err < 0) {
        ERROR2("Mixer %s load error: %s", devname, snd_strerror(err));
        snd_mixer_close(mixer_handle);
        return NULL;
    }
    handle = (PortMixer*) calloc(1, sizeof(PortMixer));
    if (handle == NULL) {
        ERROR0("malloc() failed.");
        snd_mixer_close(mixer_handle);
        return NULL;
    }
    handle->numElems = 0;
    handle->elems = (snd_mixer_elem_t**) calloc(MAX_ELEMS, sizeof(snd_mixer_elem_t*));
    if (handle->elems == NULL) {
        ERROR0("malloc() failed.");
        snd_mixer_close(mixer_handle);
        free(handle);
        return NULL;
    }
    handle->types = (INT32*) calloc(MAX_ELEMS, sizeof(INT32));
    if (handle->types == NULL) {
        ERROR0("malloc() failed.");
        snd_mixer_close(mixer_handle);
        free(handle->elems);
        free(handle);
        return NULL;
    }
    handle->controls = (PortControl*) calloc(MAX_CONTROLS, sizeof(PortControl));
    if (handle->controls == NULL) {
        ERROR0("malloc() failed.");
        snd_mixer_close(mixer_handle);
        free(handle->elems);
        free(handle->types);
        free(handle);
        return NULL;
    }
    handle->mixer_handle = mixer_handle;
    // necessary to initialize data structures
    PORT_GetPortCount(handle);
    TRACE0("< PORT_Open\n");
    return handle;
}


void PORT_Close(void* id) {
    TRACE0("> PORT_Close\n");
    if (id != NULL) {
        PortMixer* handle = (PortMixer*) id;
        if (handle->mixer_handle != NULL) {
            snd_mixer_close(handle->mixer_handle);
        }
        if (handle->elems != NULL) {
            free(handle->elems);
        }
        if (handle->types != NULL) {
            free(handle->types);
        }
        if (handle->controls != NULL) {
            free(handle->controls);
        }
        free(handle);
    }
    TRACE0("< PORT_Close\n");
}



INT32 PORT_GetPortCount(void* id) {
    PortMixer* portMixer;
    snd_mixer_elem_t *elem;

    TRACE0("> PORT_GetPortCount\n");
    if (id == NULL) {
        // $$mp: Should become a descriptive error code (invalid handle).
        return -1;
    }
    portMixer = (PortMixer*) id;
    if (portMixer->numElems == 0) {
        for (elem = snd_mixer_first_elem(portMixer->mixer_handle); elem; elem = snd_mixer_elem_next(elem)) {
            if (!snd_mixer_selem_is_active(elem))
                continue;
            TRACE2("Simple mixer control '%s',%i\n",
                   snd_mixer_selem_get_name(elem),
                   snd_mixer_selem_get_index(elem));
            if (snd_mixer_selem_has_playback_volume(elem)) {
                portMixer->elems[portMixer->numElems] = elem;
                portMixer->types[portMixer->numElems] = PORT_DST_UNKNOWN;
                portMixer->numElems++;
            }
            // to prevent buffer overflow
            if (portMixer->numElems >= MAX_ELEMS) {
                break;
            }
            /* If an element has both playback an capture volume, it is put into the arrays
               twice. */
            if (snd_mixer_selem_has_capture_volume(elem)) {
                portMixer->elems[portMixer->numElems] = elem;
                portMixer->types[portMixer->numElems] = PORT_SRC_UNKNOWN;
                portMixer->numElems++;
            }
            // to prevent buffer overflow
            if (portMixer->numElems >= MAX_ELEMS) {
                break;
            }
        }
    }
    TRACE0("< PORT_GetPortCount\n");
    return portMixer->numElems;
}


INT32 PORT_GetPortType(void* id, INT32 portIndex) {
    PortMixer* portMixer;
    INT32 type;
    TRACE0("> PORT_GetPortType\n");
    if (id == NULL) {
        // $$mp: Should become a descriptive error code (invalid handle).
        return -1;
    }
    portMixer = (PortMixer*) id;
    if (portIndex < 0 || portIndex >= portMixer->numElems) {
        // $$mp: Should become a descriptive error code (index out of bounds).
        return -1;
    }
    type = portMixer->types[portIndex];
    TRACE0("< PORT_GetPortType\n");
    return type;
}


INT32 PORT_GetPortName(void* id, INT32 portIndex, char* name, INT32 len) {
    PortMixer* portMixer;
    const char* nam;

    TRACE0("> PORT_GetPortName\n");
    if (id == NULL) {
        // $$mp: Should become a descriptive error code (invalid handle).
        return -1;
    }
    portMixer = (PortMixer*) id;
    if (portIndex < 0 || portIndex >= portMixer->numElems) {
        // $$mp: Should become a descriptive error code (index out of bounds).
        return -1;
    }
    nam = snd_mixer_selem_get_name(portMixer->elems[portIndex]);
    strncpy(name, nam, len - 1);
    name[len - 1] = 0;
    TRACE0("< PORT_GetPortName\n");
    return TRUE;
}


static int isPlaybackFunction(INT32 portType) {
        return (portType & PORT_DST_MASK);
}


/* Sets portControl to a pointer to the next free array element in the PortControl (pointer)
   array of the passed portMixer. Returns TRUE if successful. May return FALSE if there is no
   free slot. In this case, portControl is not altered */
static int getControlSlot(PortMixer* portMixer, PortControl** portControl) {
    if (portMixer->numControls >= MAX_CONTROLS) {
        return FALSE;
    } else {
        *portControl = &(portMixer->controls[portMixer->numControls]);
        portMixer->numControls++;
        return TRUE;
    }
}


/* Protect against illegal min-max values, preventing divisions by zero.
 */
inline static long getRange(long min, long max) {
    if (max > min) {
        return max - min;
    } else {
        return 1;
    }
}


/* Idea: we may specify that if unit is an empty string, the values are linear and if unit is "dB",
   the values are logarithmic.
*/
static void* createVolumeControl(PortControlCreator* creator,
                                 PortControl* portControl,
                                 snd_mixer_elem_t* elem, int isPlayback) {
    void* control;
    float precision;
    long min, max;

    if (isPlayback) {
        snd_mixer_selem_get_playback_volume_range(elem, &min, &max);
    } else {
        snd_mixer_selem_get_capture_volume_range(elem, &min, &max);
    }
    /* $$mp: The volume values retrieved with the ALSA API are strongly supposed to be logarithmic.
       So the following calculation is wrong. However, there is no correct calculation, since
       for equal-distant logarithmic steps, the precision expressed in linear varies over the
       scale. */
    precision = 1.0F / getRange(min, max);
    control = (creator->newFloatControl)(creator, portControl, CONTROL_TYPE_VOLUME, 0.0F, +1.0F, precision, "");
    return control;
}


void PORT_GetControls(void* id, INT32 portIndex, PortControlCreator* creator) {
    PortMixer* portMixer;
    snd_mixer_elem_t* elem;
    void* control;
    PortControl* portControl;
    void* controls[10];
    int numControls;
    char* portName;
    int isPlayback = 0;
    int isMono;
    int isStereo;
    char* type;
    snd_mixer_selem_channel_id_t channel;
    memset(controls, 0, sizeof(controls));

    TRACE0("> PORT_GetControls\n");
    if (id == NULL) {
        ERROR0("Invalid handle!");
        // $$mp: an error code should be returned.
        return;
    }
    portMixer = (PortMixer*) id;
    if (portIndex < 0 || portIndex >= portMixer->numElems) {
        ERROR0("Port index out of range!");
        // $$mp: an error code should be returned.
        return;
    }
    numControls = 0;
    elem = portMixer->elems[portIndex];
    if (snd_mixer_selem_has_playback_volume(elem) || snd_mixer_selem_has_capture_volume(elem)) {
        /* Since we've split/duplicated elements with both playback and capture on the recovery
           of elements, we now can assume that we handle only to deal with either playback or
           capture. */
        isPlayback = isPlaybackFunction(portMixer->types[portIndex]);
        isMono = (isPlayback && snd_mixer_selem_is_playback_mono(elem)) ||
            (!isPlayback && snd_mixer_selem_is_capture_mono(elem));
        isStereo = (isPlayback &&
                    snd_mixer_selem_has_playback_channel(elem, SND_MIXER_SCHN_FRONT_LEFT) &&
                    snd_mixer_selem_has_playback_channel(elem, SND_MIXER_SCHN_FRONT_RIGHT)) ||
            (!isPlayback &&
             snd_mixer_selem_has_capture_channel(elem, SND_MIXER_SCHN_FRONT_LEFT) &&
             snd_mixer_selem_has_capture_channel(elem, SND_MIXER_SCHN_FRONT_RIGHT));
        // single volume control
        if (isMono || isStereo) {
            if (getControlSlot(portMixer, &portControl)) {
                portControl->elem = elem;
                portControl->portType = portMixer->types[portIndex];
                portControl->controlType = CONTROL_TYPE_VOLUME;
                if (isMono) {
                    portControl->channel = CHANNELS_MONO;
                } else {
                    portControl->channel = CHANNELS_STEREO;
                }
                control = createVolumeControl(creator, portControl, elem, isPlayback);
                if (control != NULL) {
                    controls[numControls++] = control;
                }
            }
        } else { // more than two channels, each channels has its own control.
            for (channel = SND_MIXER_SCHN_FRONT_LEFT; channel <= SND_MIXER_SCHN_LAST; channel++) {
                if ((isPlayback && snd_mixer_selem_has_playback_channel(elem, channel)) ||
                    (!isPlayback && snd_mixer_selem_has_capture_channel(elem, channel))) {
                    if (getControlSlot(portMixer, &portControl)) {
                        portControl->elem = elem;
                        portControl->portType = portMixer->types[portIndex];
                        portControl->controlType = CONTROL_TYPE_VOLUME;
                        portControl->channel = channel;
                        control = createVolumeControl(creator, portControl, elem, isPlayback);
                        // We wrap in a compound control to provide the channel name.
                        if (control != NULL) {
                            /* $$mp 2003-09-14: The following cast shouln't be necessary. Instead, the
                               declaration of PORT_NewCompoundControlPtr in Ports.h should be changed
                               to take a const char* parameter. */
                            control = (creator->newCompoundControl)(creator, (char*) snd_mixer_selem_channel_name(channel), &control, 1);
                        }
                        if (control != NULL) {
                            controls[numControls++] = control;
                        }
                    }
                }
            }
        }
        // BALANCE control
        if (isStereo) {
            if (getControlSlot(portMixer, &portControl)) {
                portControl->elem = elem;
                portControl->portType = portMixer->types[portIndex];
                portControl->controlType = CONTROL_TYPE_BALANCE;
                portControl->channel = CHANNELS_STEREO;
                /* $$mp: The value for precision is chosen more or less arbitrarily. */
                control = (creator->newFloatControl)(creator, portControl, CONTROL_TYPE_BALANCE, -1.0F, 1.0F, 0.01F, "");
                if (control != NULL) {
                    controls[numControls++] = control;
                }
            }
        }
    }
    if (snd_mixer_selem_has_playback_switch(elem) || snd_mixer_selem_has_capture_switch(elem)) {
        if (getControlSlot(portMixer, &portControl)) {
            type = isPlayback ? CONTROL_TYPE_MUTE : CONTROL_TYPE_SELECT;
            portControl->elem = elem;
            portControl->portType = portMixer->types[portIndex];
            portControl->controlType = type;
            control = (creator->newBooleanControl)(creator, portControl, type);
            if (control != NULL) {
                controls[numControls++] = control;
            }
        }
    }
    /* $$mp 2003-09-14: The following cast shouln't be necessary. Instead, the
       declaration of PORT_NewCompoundControlPtr in Ports.h should be changed
       to take a const char* parameter. */
    portName = (char*) snd_mixer_selem_get_name(elem);
    control = (creator->newCompoundControl)(creator, portName, controls, numControls);
    if (control != NULL) {
        (creator->addControl)(creator, control);
    }
    TRACE0("< PORT_GetControls\n");
}


INT32 PORT_GetIntValue(void* controlIDV) {
    PortControl* portControl = (PortControl*) controlIDV;
    int value = 0;
    snd_mixer_selem_channel_id_t channel;

    if (portControl != NULL) {
        switch (portControl->channel) {
        case CHANNELS_MONO:
            channel = SND_MIXER_SCHN_MONO;
            break;

        case CHANNELS_STEREO:
            channel = SND_MIXER_SCHN_FRONT_LEFT;
            break;

        default:
            channel = portControl->channel;
        }
        if (portControl->controlType == CONTROL_TYPE_MUTE ||
            portControl->controlType == CONTROL_TYPE_SELECT) {
            if (isPlaybackFunction(portControl->portType)) {
                snd_mixer_selem_get_playback_switch(portControl->elem, channel, &value);
            } else {
                snd_mixer_selem_get_capture_switch(portControl->elem, channel, &value);
            }
            if (portControl->controlType == CONTROL_TYPE_MUTE) {
                value = ! value;
            }
        } else {
            ERROR1("PORT_GetIntValue(): inappropriate control type: %s\n",
                   portControl->controlType);
        }
    }
    return (INT32) value;
}


void PORT_SetIntValue(void* controlIDV, INT32 value) {
    PortControl* portControl = (PortControl*) controlIDV;
    snd_mixer_selem_channel_id_t channel;

    if (portControl != NULL) {
        if (portControl->controlType == CONTROL_TYPE_MUTE) {
            value = ! value;
        }
        if (portControl->controlType == CONTROL_TYPE_MUTE ||
            portControl->controlType == CONTROL_TYPE_SELECT) {
            if (isPlaybackFunction(portControl->portType)) {
                snd_mixer_selem_set_playback_switch_all(portControl->elem, value);
            } else {
                snd_mixer_selem_set_capture_switch_all(portControl->elem, value);
            }
        } else {
            ERROR1("PORT_SetIntValue(): inappropriate control type: %s\n",
                   portControl->controlType);
        }
    }
}


static float scaleVolumeValueToNormalized(long value, long min, long max) {
    return (float) (value - min) / getRange(min, max);
}


static long scaleVolumeValueToHardware(float value, long min, long max) {
    return (long)(value * getRange(min, max) + min);
}


float getRealVolume(PortControl* portControl,
                    snd_mixer_selem_channel_id_t channel) {
    float fValue;
    long lValue = 0;
    long min = 0;
    long max = 0;

    if (isPlaybackFunction(portControl->portType)) {
        snd_mixer_selem_get_playback_volume_range(portControl->elem,
                                                  &min, &max);
        snd_mixer_selem_get_playback_volume(portControl->elem,
                                            channel, &lValue);
    } else {
        snd_mixer_selem_get_capture_volume_range(portControl->elem,
                                                 &min, &max);
        snd_mixer_selem_get_capture_volume(portControl->elem,
                                           channel, &lValue);
    }
    fValue = scaleVolumeValueToNormalized(lValue, min, max);
    return fValue;
}


void setRealVolume(PortControl* portControl,
                   snd_mixer_selem_channel_id_t channel, float value) {
    long lValue = 0;
    long min = 0;
    long max = 0;

    if (isPlaybackFunction(portControl->portType)) {
        snd_mixer_selem_get_playback_volume_range(portControl->elem,
                                                  &min, &max);
        lValue = scaleVolumeValueToHardware(value, min, max);
        snd_mixer_selem_set_playback_volume(portControl->elem,
                                            channel, lValue);
    } else {
        snd_mixer_selem_get_capture_volume_range(portControl->elem,
                                                 &min, &max);
        lValue = scaleVolumeValueToHardware(value, min, max);
        snd_mixer_selem_set_capture_volume(portControl->elem,
                                           channel, lValue);
    }
}


static float getFakeBalance(PortControl* portControl) {
    float volL, volR;

    // pan is the ratio of left and right
    volL = getRealVolume(portControl, SND_MIXER_SCHN_FRONT_LEFT);
    volR = getRealVolume(portControl, SND_MIXER_SCHN_FRONT_RIGHT);
    if (volL > volR) {
        return -1.0f + (volR / volL);
    }
    else if (volR > volL) {
        return 1.0f - (volL / volR);
    }
    return 0.0f;
}


static float getFakeVolume(PortControl* portControl) {
    float valueL;
    float valueR;
    float value;

    valueL = getRealVolume(portControl, SND_MIXER_SCHN_FRONT_LEFT);
    valueR = getRealVolume(portControl, SND_MIXER_SCHN_FRONT_RIGHT);
    // volume is the greater value of both
    value = valueL > valueR ? valueL : valueR ;
    return value;
}


/*
 * sets the unsigned values for left and right volume according to
 * the given volume (0...1) and balance (-1..0..+1)
 */
static void setFakeVolume(PortControl* portControl, float vol, float bal) {
    float volumeLeft;
    float volumeRight;

    if (bal < 0.0f) {
        volumeLeft = vol;
        volumeRight = vol * (bal + 1.0f);
    } else {
        volumeLeft = vol * (1.0f - bal);
        volumeRight = vol;
    }
    setRealVolume(portControl, SND_MIXER_SCHN_FRONT_LEFT, volumeLeft);
    setRealVolume(portControl, SND_MIXER_SCHN_FRONT_RIGHT, volumeRight);
}


float PORT_GetFloatValue(void* controlIDV) {
    PortControl* portControl = (PortControl*) controlIDV;
    float value = 0.0F;

    if (portControl != NULL) {
        if (portControl->controlType == CONTROL_TYPE_VOLUME) {
            switch (portControl->channel) {
            case CHANNELS_MONO:
                value = getRealVolume(portControl, SND_MIXER_SCHN_MONO);
                break;

            case CHANNELS_STEREO:
                value = getFakeVolume(portControl);
                break;

            default:
                value = getRealVolume(portControl, portControl->channel);
            }
        } else if (portControl->controlType == CONTROL_TYPE_BALANCE) {
            if (portControl->channel == CHANNELS_STEREO) {
                value = getFakeBalance(portControl);
            } else {
                ERROR0("PORT_GetFloatValue(): Balance only allowed for stereo channels!\n");
            }
        } else {
            ERROR1("PORT_GetFloatValue(): inappropriate control type: %s!\n",
                   portControl->controlType);
        }
    }
    return value;
}


void PORT_SetFloatValue(void* controlIDV, float value) {
    PortControl* portControl = (PortControl*) controlIDV;

    if (portControl != NULL) {
        if (portControl->controlType == CONTROL_TYPE_VOLUME) {
            switch (portControl->channel) {
            case CHANNELS_MONO:
                setRealVolume(portControl, SND_MIXER_SCHN_MONO, value);
                break;

            case CHANNELS_STEREO:
                setFakeVolume(portControl, value, getFakeBalance(portControl));
                break;

            default:
                setRealVolume(portControl, portControl->channel, value);
            }
        } else if (portControl->controlType == CONTROL_TYPE_BALANCE) {
            if (portControl->channel == CHANNELS_STEREO) {
                setFakeVolume(portControl, getFakeVolume(portControl), value);
            } else {
                ERROR0("PORT_SetFloatValue(): Balance only allowed for stereo channels!\n");
            }
        } else {
            ERROR1("PORT_SetFloatValue(): inappropriate control type: %s!\n",
                   portControl->controlType);
        }
    }
}


#endif // USE_PORTS
