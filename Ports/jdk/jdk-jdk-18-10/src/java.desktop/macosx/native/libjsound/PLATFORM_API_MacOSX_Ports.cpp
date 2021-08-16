/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <CoreAudio/CoreAudio.h>
#include <IOKit/audio/IOAudioTypes.h>

#include "PLATFORM_API_MacOSX_Utils.h"

extern "C" {
#include "Ports.h"
}

#if USE_PORTS == TRUE

/* If a device has the only AudioStream in the scope (input or output),
 * PortMixer provides a single Port, using the stream kAudioStreamPropertyTerminalType
 * property value to determine Port.Type (PORT_GetPortType function).
 * If the device has several (more than 1) AudioStreams, there are 2 ways to represent Ports:
 * 1. (HALLab-style) single Port which represents all device channels with
 *    "master volume" and (if number of channel is 2) "master balance"; if AudioDevice
 *    does not provide "master" controls, implement "virtual master" controls.
 *    Port.Type is PORT_SRC_UNKNOWN or PORT_DST_UNKNOWN.
 * 2. provide a separate Port for every AudioStream (with appropriate Port.Type);
 *
 * AudioHardware.h claims that AudioStream objects share AudioControl objects with their owning AudioDevice.
 * In practice 10.7 OSX drivers (built-in devices, USB audio) implement AudioControl only for AudioDevice.
 * For now 1st way is implemented (2nd way can be better if AudioStreams provide AudioControls).
 */

static DeviceList deviceCache;

#define FourCC2Str(n) ((char[5]){(char)(n >> 24), (char)(n >> 16), (char)(n >> 8), (char)(n), 0})


// CoreAudio's AudioControl
struct AudioControl {
    AudioObjectID controlID;
    AudioClassID classID;               // kAudioVolumeControlClassID etc.
    AudioObjectPropertyScope scope;     // input, output
    AudioObjectPropertyElement channel; // master = 0, channels = 1 2 ...
};

// Controls for Java
// PortMixer do all memory management (alloc/free audioControls)
struct PortControl {
    enum ControlType {
        Volume,     // manages single or multiple volume AudioControl
        Mute,       // manages single or multiple mute AudioControls
        Balance     // "virtual" control, manages 2 volume AudioControls (only for stereo lines)
    };
    ControlType type;

    int controlCount;
    AudioControl **audioControls;

    PortControl *next;  // to organize PortControl list
};

// represents line (port) for PortMixer
// used for PORT_GetPortCount/PORT_GetPortType/PORT_GetPortName functions
struct PortLine {
    AudioObjectPropertyScope scope;
    // if the device has several AudioStreams in the scope, streamID == 0
    AudioStreamID streamID;
};

struct PortMixer {
    AudioDeviceID deviceID;

    int portCount;
    PortLine ports[2]; // maximum 2 lines - 1 for input & 1 for output

    int deviceControlCount; // -1 means "not initialized"
    AudioControl *deviceControls;

    PortControl *portControls;  // list of port controls

    bool listenersInstalled;
};


void RemoveChangeListeners(PortMixer *mixer);   // forward declaration

OSStatus ChangeListenerProc(AudioObjectID inObjectID, UInt32 inNumberAddresses,
        const AudioObjectPropertyAddress inAddresses[], void *inClientData)
{
    PortMixer *mixer = (PortMixer *)inClientData;

    OSStatus err = noErr;
    UInt32 size;

    bool invalid = false;

    for (UInt32 i = 0; i < inNumberAddresses; i++) {
        switch (inAddresses[i].mSelector) {
        case kAudioHardwarePropertyDevices:
            // check if the device has been removed
            err = GetAudioObjectPropertySize(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal,
                kAudioHardwarePropertyDevices, &size);
            if (err == noErr) {
                int count = size/sizeof(AudioDeviceID);
                AudioDeviceID devices[count];
                err = GetAudioObjectProperty(kAudioObjectSystemObject, kAudioObjectPropertyScopeGlobal,
                    kAudioHardwarePropertyDevices, count*sizeof(AudioDeviceID), devices, 1);
                if (err == noErr) {
                    bool found = false;
                    for (int j = 0; j < count; j++) {
                        if (devices[j] == mixer->deviceID) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        invalid = true;
                    }
                }
            }
            break;
        case kAudioObjectPropertyOwnedObjects:
        case kAudioDevicePropertyDeviceHasChanged:
            // ensure all _used_ AudioControl are valid
            err = GetAudioObjectPropertySize(mixer->deviceID, kAudioObjectPropertyScopeGlobal,
                kAudioObjectPropertyOwnedObjects, &size);
            if (err == noErr) {
                int count = size / sizeof(AudioObjectID);
                AudioObjectID controlIDs[count];
                err = GetAudioObjectProperty(mixer->deviceID, kAudioObjectPropertyScopeGlobal,
                    kAudioObjectPropertyOwnedObjects, count * sizeof(AudioObjectID), &controlIDs, 1);
                if (err == noErr) {
                    for (PortControl *ctrl = mixer->portControls; ctrl != NULL; ctrl = ctrl->next) {
                        for (int i = 0; i < ctrl->controlCount; i++) {
                            bool found = false;
                            for (int j = 0; j < count; j++) {
                                if (ctrl->audioControls[i]->controlID == controlIDs[j]) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                invalid = true;
                                break;  // goto next control
                            }
                        }
                    }
                }
            }
        }
    }

    if (invalid) {
        TRACE1("PortMixer (deviceID=0x%x) becomes invalid", (int)mixer->deviceID);
        // invalidate all controls
        for (int i=0; i<mixer->deviceControlCount; i++) {
            mixer->deviceControls[i].controlID = 0;
        }
        RemoveChangeListeners(mixer);
    }


    return noErr;
}

const AudioObjectPropertyAddress changeListenersAddresses[] = {
    {kAudioHardwarePropertyDevices, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster},
    {kAudioObjectPropertyOwnedObjects, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster},
    {kAudioDevicePropertyDeviceHasChanged, kAudioObjectPropertyScopeGlobal, kAudioObjectPropertyElementMaster}
};

void AddChangeListeners(PortMixer *mixer) {
    if (!mixer->listenersInstalled) {
        for (size_t i=0; i<sizeof(changeListenersAddresses)/sizeof(changeListenersAddresses[0]); i++) {
            AudioObjectAddPropertyListener(mixer->deviceID, &changeListenersAddresses[i], ChangeListenerProc, mixer);
        }
        mixer->listenersInstalled = true;
    }
}

void RemoveChangeListeners(PortMixer *mixer) {
    if (mixer->listenersInstalled) {
        for (size_t i=0; i<sizeof(changeListenersAddresses)/sizeof(changeListenersAddresses[0]); i++) {
            AudioObjectRemovePropertyListener(mixer->deviceID, &changeListenersAddresses[i], ChangeListenerProc, mixer);
        }
        mixer->listenersInstalled = false;
    }
}


////////////////////////////////////////////////////////////////////////////////
// functions from Port.h

INT32 PORT_GetPortMixerCount() {
    deviceCache.Refresh();
    int count = deviceCache.GetCount();
    TRACE1("<<PORT_GetPortMixerCount = %d\n", count);
    return count;
}

INT32 PORT_GetPortMixerDescription(INT32 mixerIndex, PortMixerDescription* mixerDescription) {
    bool result = deviceCache.GetDeviceInfo(mixerIndex, NULL, PORT_STRING_LENGTH,
            mixerDescription->name, mixerDescription->vendor, mixerDescription->description, mixerDescription->version);
    return result ? TRUE : FALSE;
}

void* PORT_Open(INT32 mixerIndex) {
    TRACE1("\n>>PORT_Open (mixerIndex=%d)\n", (int)mixerIndex);
    PortMixer *mixer = (PortMixer *)calloc(1, sizeof(PortMixer));

    mixer->deviceID = deviceCache.GetDeviceID(mixerIndex);
    if (mixer->deviceID != 0) {
        mixer->deviceControlCount = -1; // not initialized
        // fill mixer->ports (and mixer->portCount)
        for (int i=0; i<2; i++) {
            OSStatus err;
            UInt32 size = 0;
            AudioObjectPropertyScope scope =
                (i == 0) ? kAudioDevicePropertyScopeInput : kAudioDevicePropertyScopeOutput;

            err = GetAudioObjectPropertySize(mixer->deviceID, scope, kAudioDevicePropertyStreams, &size);
            if (err || size == 0) {
                continue;
            }
            if (size / sizeof(AudioStreamID) == 1) {
                // the device has the only AudioStream
                AudioStreamID streamID;
                err = GetAudioObjectProperty(mixer->deviceID, scope, kAudioDevicePropertyStreams,
                    sizeof(streamID), &streamID, 1);
                if (err) {
                    continue;
                }
                mixer->ports[mixer->portCount].streamID = streamID;
            } else {
                // the device has several AudioStreams in the scope
                mixer->ports[mixer->portCount].streamID = 0;
            }
            mixer->ports[mixer->portCount].scope = scope;
            mixer->portCount++;
        }
    }

    TRACE2("<<PORT_Open (mixerIndex=%d) %p\n", mixerIndex, mixer);
    return mixer;
}


void PORT_Close(void* id) {
    TRACE1(">>PORT_Close %p\n", id);
    PortMixer *mixer = (PortMixer *)id;

    if (mixer) {
        RemoveChangeListeners(mixer);
        while (mixer->portControls != NULL) {
            PortControl *control2delete = mixer->portControls;
            mixer->portControls = control2delete->next;

            if (control2delete->audioControls != NULL) {
                free(control2delete->audioControls);
            }
            free(control2delete);
        }
        if (mixer->deviceControls) {
            free(mixer->deviceControls);
        }
        free(mixer);
    }
    TRACE1("<<PORT_Close %p\n", mixer);
}

INT32 PORT_GetPortCount(void* id) {
    PortMixer *mixer = (PortMixer *)id;

    int result = mixer->portCount;

    TRACE1("<<PORT_GetPortCount = %d\n", result);
    return result;
}

INT32 PORT_GetPortType(void* id, INT32 portIndex) {
    PortMixer *mixer = (PortMixer *)id;
    INT32 ret = 0;

    if (portIndex < 0 || portIndex >= mixer->portCount) {
        ERROR1("PORT_GetPortType: line (portIndex = %d) not found\n", portIndex);
        return 0;
    }

    AudioObjectPropertyScope scope = mixer->ports[portIndex].scope;
    AudioStreamID streamID = mixer->ports[portIndex].streamID;
    if (streamID != 0) {
        UInt32 terminalType;

        OSStatus err = GetAudioObjectProperty(streamID, kAudioObjectPropertyScopeGlobal,
            kAudioStreamPropertyTerminalType, sizeof(terminalType), &terminalType, 1);
        if (err) {
            OS_ERROR1(err, "PORT_GetPortType(kAudioStreamPropertyTerminalType), portIndex=%d", portIndex);
            return 0;
        }

        // Note that kAudioStreamPropertyTerminalType actually returns values from
        // IOAudioTypes.h, not the defined kAudioStreamTerminalType*.
        TRACE4("PORT_GetPortType (portIndex=%d), scope=%s, termType=0x%04x (%s)\n",
            (int)portIndex, FourCC2Str(scope), (int)terminalType, FourCC2Str(terminalType));
        switch (terminalType) {
        case INPUT_MICROPHONE:
            ret = PORT_SRC_MICROPHONE;
            break;

        case OUTPUT_SPEAKER:
            ret = PORT_DST_SPEAKER;
            break;
        case OUTPUT_HEADPHONES:
            ret = PORT_DST_HEADPHONE;
            break;

        case EXTERNAL_LINE_CONNECTOR:
            ret = scope == kAudioDevicePropertyScopeInput ? PORT_SRC_LINE_IN : PORT_DST_LINE_OUT;
            break;

        default:
            TRACE1("  unknown output terminal type %#x\n", terminalType);
        }
    } else {
        TRACE0("  PORT_GetPortType: multiple streams\n");
    }

    if (ret == 0) {
        // if the type not detected, return "common type"
        ret = scope == kAudioDevicePropertyScopeInput ? PORT_SRC_UNKNOWN : PORT_DST_UNKNOWN;
    }

    TRACE2("<<PORT_GetPortType (portIndex=%d) = %d\n", portIndex, ret);
    return ret;
}

INT32 PORT_GetPortName(void* id, INT32 portIndex, char* name, INT32 len) {
    PortMixer *mixer = (PortMixer *)id;

    name[0] = 0;    // for safety

    if (portIndex < 0 || portIndex >= mixer->portCount) {
        ERROR1("PORT_GetPortName: line (portIndex = %d) not found\n", portIndex);
        return FALSE;
    }

    AudioStreamID streamID = mixer->ports[portIndex].streamID;
    CFStringRef cfname = NULL;
    if (streamID != 0) {
        OSStatus err = GetAudioObjectProperty(streamID, kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyName, sizeof(cfname), &cfname, 1);
        if (err && err != kAudioHardwareUnknownPropertyError) {
            OS_ERROR1(err, "PORT_GetPortName(stream name), portIndex=%d", portIndex);
            return FALSE;
        }
    }

    if (!cfname) {
        // use the device's name if the stream has no name (usually the case)
        // or the device has several AudioStreams
        OSStatus err = GetAudioObjectProperty(mixer->deviceID, kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyName, sizeof(cfname), &cfname, 1);
        if (err) {
            OS_ERROR1(err, "PORT_GetPortName(device name), portIndex=%d", portIndex);
            return FALSE;
        }
    }

    if (cfname) {
        CFStringGetCString(cfname, name, len, kCFStringEncodingUTF8);
        CFRelease(cfname);
    }

    TRACE2("<<PORT_GetPortName (portIndex = %d) = %s\n", portIndex, name);
    return TRUE;
}


// counts number of valid (non-NULL) elements in the array of AudioControls
static int ValidControlCount(AudioControl **arr, int offset, int len) {
    int result = 0;
    int end = offset + len;
    for (int i=offset; i<end; i++) {
        if (arr[i] != NULL)
            result++;
    }
    return result;
}

// returns java control
static void* CreatePortControl(PortMixer *mixer, PortControlCreator *creator, PortControl::ControlType type,
                               AudioControl **audioControls, int offset, int len) {
    void *jControl = NULL;
    PortControl *control = (PortControl *)calloc(1, sizeof(PortControl));
    if (control == NULL) {
        return NULL;
    }
    float precision = 0.01;

    control->type = type;
    control->controlCount = len;
    control->audioControls = (AudioControl **)malloc(len * sizeof(AudioControl *));
    if (control->audioControls == NULL) {
        free(control);
        return NULL;
    }
    memcpy(control->audioControls, audioControls + offset, len * sizeof(AudioControl *));

    switch (control->type) {
    case PortControl::Volume:
        jControl = creator->newFloatControl(creator, control, CONTROL_TYPE_VOLUME, 0, 1, precision, "");
        break;
    case PortControl::Mute:
        jControl = creator->newBooleanControl(creator, control, CONTROL_TYPE_MUTE);
        break;
    case PortControl::Balance:
        jControl = creator->newFloatControl(creator, control, CONTROL_TYPE_BALANCE, -1, 1, precision, "");
        break;
    };

    if (jControl == NULL) {
        ERROR0("CreatePortControl: javaControl was not created\n");
        free(control->audioControls);
        free(control);
        return NULL;
    }

    // add the control to mixer control list;
    control->next = mixer->portControls;
    mixer->portControls = control;

    return jControl;
}

void PORT_GetControls(void* id, INT32 portIndex, PortControlCreator* creator) {
    PortMixer *mixer = (PortMixer *)id;

    TRACE1(">>PORT_GetControls (portIndex = %d)\n", portIndex);

    if (portIndex < 0 || portIndex >= mixer->portCount) {
        ERROR1("<<PORT_GetControls: line (portIndex = %d) not found\n", portIndex);
        return;
    }

    PortLine *port = &(mixer->ports[portIndex]);

    if (mixer->deviceControlCount < 0) {    // not initialized
        OSStatus err;
        UInt32 size;
        // deviceControlCount is overestimated
        // because we don't actually filter by if the owned objects are controls
        err = GetAudioObjectPropertySize(mixer->deviceID, kAudioObjectPropertyScopeGlobal,
            kAudioObjectPropertyOwnedObjects, &size);

        if (err) {
            OS_ERROR1(err, "PORT_GetControls (portIndex = %d) get OwnedObject size", portIndex);
        } else {
            mixer->deviceControlCount = size / sizeof(AudioObjectID);
            TRACE1("  PORT_GetControls: detected %d owned objects\n", mixer->deviceControlCount);

            AudioObjectID controlIDs[mixer->deviceControlCount];

            err = GetAudioObjectProperty(mixer->deviceID, kAudioObjectPropertyScopeGlobal,
                kAudioObjectPropertyOwnedObjects, sizeof(controlIDs), controlIDs, 1);

            if (err) {
                OS_ERROR1(err, "PORT_GetControls (portIndex = %d) get OwnedObject values", portIndex);
            } else {
                mixer->deviceControls = (AudioControl *)calloc(mixer->deviceControlCount, sizeof(AudioControl));
                if (mixer->deviceControls == NULL) {
                    return;
                }

                for (int i = 0; i < mixer->deviceControlCount; i++) {
                    AudioControl *control = &mixer->deviceControls[i];

                    control->controlID = controlIDs[i];

                    OSStatus err1 = GetAudioObjectProperty(control->controlID, kAudioObjectPropertyScopeGlobal,
                        kAudioObjectPropertyClass, sizeof(control->classID), &control->classID, 1);
                    OSStatus err2 = GetAudioObjectProperty(control->controlID, kAudioObjectPropertyScopeGlobal,
                        kAudioControlPropertyScope, sizeof(control->scope), &control->scope, 1);
                    OSStatus err3 = GetAudioObjectProperty(control->controlID, kAudioObjectPropertyScopeGlobal,
                        kAudioControlPropertyElement, sizeof(control->channel), &control->channel, 1);
                    if (err1 || err2 || err3) { // not a control or other error
                        control->classID = 0;
                        continue;
                    }

                    TRACE4("- control 0x%x, class='%s', scope='%s', channel=%d\n",
                        control->controlID, FourCC2Str(control->classID), FourCC2Str(control->scope), control->channel);
                }
            }
        }
    }

    if (mixer->deviceControlCount <= 0) {
        TRACE1("<<PORT_GetControls (portIndex = %d): no owned AudioControls\n", portIndex);
        return;
    }

    int totalChannels = GetChannelCount(mixer->deviceID, port->scope == kAudioDevicePropertyScopeOutput ? 1 : 0);

    // collect volume and mute controls
    AudioControl* volumeControls[totalChannels+1];  // 0 - for master channel
    memset(&volumeControls, 0, sizeof(AudioControl *) * (totalChannels+1));
    AudioControl* muteControls[totalChannels+1];  // 0 - for master channel
    memset(&muteControls, 0, sizeof(AudioControl *) * (totalChannels+1));

    for (int i=0; i<mixer->deviceControlCount; i++) {
        AudioControl *control = &mixer->deviceControls[i];
        if (control->classID == 0 || control->scope != port->scope || control->channel > (unsigned)totalChannels) {
            continue;
        }
        if (control->classID == kAudioVolumeControlClassID) {
            if (volumeControls[control->channel] == NULL) {
                volumeControls[control->channel] = control;
            } else {
                ERROR4("WARNING: duplicate VOLUME control 0x%x, class='%s', scope='%s', channel=%d\n",
                    control->controlID, FourCC2Str(control->classID), FourCC2Str(control->scope), control->channel);
            }
        } else if (control->classID == kAudioMuteControlClassID) {
            if (muteControls[control->channel] == NULL) {
                muteControls[control->channel] = control;
            } else {
                ERROR4("WARNING: duplicate MUTE control 0x%x, class='%s', scope='%s', channel=%d\n",
                    control->controlID, FourCC2Str(control->classID), FourCC2Str(control->scope), control->channel);
            }
        } else {
#ifdef USE_ERROR
            if (control->classID != 0) {
                ERROR4("WARNING: unhandled control 0x%x, class='%s', scope='%s', channel=%d\n",
                    control->controlID, FourCC2Str(control->classID), FourCC2Str(control->scope), control->channel);
            }
#endif
        }
    }

    ////////////////////////////////////////////////////////
    // create java control hierarchy

    void *masterVolume = NULL, *masterMute = NULL, *masterBalance = NULL;
    // volumeControls[0] and muteControls[0] - master volume/mute
    // volumeControls[n] and muteControls[n] (n=1..totalChannels) - corresponding channel controls
    if (volumeControls[0] != NULL) {    // "master volume" AudioControl
        masterVolume = CreatePortControl(mixer, creator, PortControl::Volume, volumeControls, 0, 1);
    } else {
        if (ValidControlCount(volumeControls, 1, totalChannels) == totalChannels) {
            // every channel has volume control => create virtual master volume
            masterVolume = CreatePortControl(mixer, creator, PortControl::Volume, volumeControls, 1, totalChannels);
        } else {
            TRACE2("  PORT_GetControls (master volume): totalChannels = %d, valid volume controls = %d\n",
                totalChannels, ValidControlCount(volumeControls, 1, totalChannels));
        }
    }

    if (muteControls[0] != NULL) {      // "master mute"
        masterMute = CreatePortControl(mixer, creator, PortControl::Mute, muteControls, 0, 1);
    } else {
        if (ValidControlCount(muteControls, 1, totalChannels) == totalChannels) {
            // every channel has mute control => create virtual master mute control
            masterMute = CreatePortControl(mixer, creator, PortControl::Mute, muteControls, 1, totalChannels);
        } else {
            TRACE2("  PORT_GetControls (master mute): totalChannels = %d, valid volume controls = %d\n",
                totalChannels, ValidControlCount(muteControls, 1, totalChannels));
        }
    }

    // virtual balance
    if (totalChannels == 2) {
        if (ValidControlCount(volumeControls, 1, totalChannels) == totalChannels) {
            masterBalance = CreatePortControl(mixer, creator, PortControl::Balance, volumeControls, 1, totalChannels);
        } else {
            TRACE2("  PORT_GetControls (naster balance): totalChannels = %d, valid volume controls = %d\n",
                totalChannels, ValidControlCount(volumeControls, 1, totalChannels));
        }
    }

    // add "master" controls
    if (masterVolume != NULL) {
        creator->addControl(creator, masterVolume);
    }
    if (masterBalance != NULL) {
        creator->addControl(creator, masterBalance);
    }
    if (masterMute != NULL) {
        creator->addControl(creator, masterMute);
    }

    // don't add per-channel controls for mono & stereo - they are handled by "master" controls
    // TODO: this should be reviewed to handle controls other than mute & volume
    if (totalChannels > 2) {
        // add separate compound control for each channel (containing volume and mute)
        // (ensure that we have controls)
        if (ValidControlCount(volumeControls, 1, totalChannels) > 0 || ValidControlCount(muteControls, 1, totalChannels) > 0) {
            for (int ch=1; ch<=totalChannels; ch++) {
                // get the channel name
                char *channelName;
                CFStringRef cfname = NULL;
                const AudioObjectPropertyAddress address = {kAudioObjectPropertyElementName, port->scope, (unsigned)ch};
                UInt32 size = sizeof(cfname);
                OSStatus err = AudioObjectGetPropertyData(mixer->deviceID, &address, 0, NULL, &size, &cfname);
                if (err == noErr) {
                    CFIndex length = CFStringGetLength(cfname) + 1;
                    channelName = (char *)malloc(length);
                    if (channelName == NULL) {
                        return;
                    }
                    CFStringGetCString(cfname, channelName, length, kCFStringEncodingUTF8);
                    CFRelease(cfname);
                } else {
                    channelName = (char *)malloc(16);
                    if (channelName == NULL) {
                        return;
                    }
                    sprintf(channelName, "Ch %d", ch);
                }

                void* jControls[2];
                int controlCount = 0;
                if (volumeControls[ch] != NULL) {
                    jControls[controlCount++] = CreatePortControl(mixer, creator, PortControl::Volume, volumeControls, ch, 1);
                }
                if (muteControls[ch] != NULL) {
                    jControls[controlCount++] = CreatePortControl(mixer, creator, PortControl::Mute, muteControls, ch, 1);
                }
                // TODO: add any extra controls for "other" controls for the channel

                void *compoundControl = creator->newCompoundControl(creator, channelName, jControls, controlCount);
                creator->addControl(creator, compoundControl);

                free(channelName);
            }
        }
    }

    AddChangeListeners(mixer);

    TRACE1("<<PORT_GetControls (portIndex = %d)\n", portIndex);
}

bool TestPortControlValidity(PortControl *control) {
    for (int i=0; i<control->controlCount; i++) {
        if (control->audioControls[i]->controlID == 0)
            return false;
    }
    return true;
}


#define DEFAULT_MUTE_VALUE 0

INT32 PORT_GetIntValue(void* controlIDV) {
    PortControl *control = (PortControl *)controlIDV;
    INT32 result = 0;

    switch (control->type) {
    case PortControl::Mute:
        if (!TestPortControlValidity(control)) {
            return DEFAULT_MUTE_VALUE;
        }
        result = 1; // default is "muted", if some channel in unmuted, then "virtual mute" is also unmuted
        for (int i=0; i<control->controlCount; i++) {
            UInt32 value;
            OSStatus err = GetAudioObjectProperty(control->audioControls[i]->controlID,
                kAudioObjectPropertyScopeGlobal, kAudioBooleanControlPropertyValue, sizeof(value), &value, 1);
            if (err) {
                OS_ERROR3(err, "PORT_GetIntValue, control %d of %d (coltrolID = 0x%x)",
                    i, control->controlCount, control->audioControls[i]->controlID);
                return DEFAULT_MUTE_VALUE;
            }
            if (value == 0) {
                result = 0;
            }
        }
        break;
    default:
        ERROR1("PORT_GetIntValue requested for non-Int control (control-type == %d)\n", control->type);
        return 0;
    }

    //TRACE1("<<PORT_GetIntValue = %d\n", result);
    return result;
}

void PORT_SetIntValue(void* controlIDV, INT32 value) {
    //TRACE1("> PORT_SetIntValue = %d\n", value);
    PortControl *control = (PortControl *)controlIDV;

    if (!TestPortControlValidity(control)) {
        return;
    }

    switch (control->type) {
    case PortControl::Mute:
        for (int i=0; i<control->controlCount; i++) {
            OSStatus err = SetAudioObjectProperty(control->audioControls[i]->controlID,
                kAudioObjectPropertyScopeGlobal, kAudioBooleanControlPropertyValue, sizeof(value), &value);
            if (err) {
                OS_ERROR3(err, "PORT_SetIntValue, control %d of %d (coltrolID = 0x%x)",
                    i, control->controlCount, control->audioControls[i]->controlID);
                // don't return - try to set the rest of AudioControls
            }
        }
        break;
    default:
        ERROR1("PORT_SetIntValue requested for non-Int control (control-type == %d)\n", control->type);
        return;
    }
}


// gets volume value for all AudioControls of the PortControl
static bool GetPortControlVolumes(PortControl *control, Float32 *volumes, Float32 *maxVolume) {
    *maxVolume = 0.0f;
    for (int i=0; i<control->controlCount; i++) {
        OSStatus err = GetAudioObjectProperty(control->audioControls[i]->controlID,
            kAudioObjectPropertyScopeGlobal, kAudioLevelControlPropertyScalarValue,
            sizeof(volumes[i]), &volumes[i], 1);
        if (err) {
            OS_ERROR3(err, "GetPortControlVolumes, control %d of %d (controlID = 0x%x)",
                i, control->controlCount, control->audioControls[i]->controlID);
            return false;
        }
        if (volumes[i] > *maxVolume) {
            *maxVolume = volumes[i];
        }
    }
    return true;
}

// sets volume value for all AudioControls of the PortControl
static void SetPortControlVolumes(PortControl *control, Float32 *volumes) {
    for (int i=0; i<control->controlCount; i++) {
        OSStatus err = SetAudioObjectProperty(control->audioControls[i]->controlID,
            kAudioObjectPropertyScopeGlobal, kAudioLevelControlPropertyScalarValue,
            sizeof(volumes[i]), &volumes[i]);
        if (err) {
            OS_ERROR3(err, "SetPortControlVolumes , control %d of %d (coltrolID = 0x%x)",
                i, control->controlCount, control->audioControls[i]->controlID);
            // don't return - try to set the rest of AudioControls
        }
    }
}

#define DEFAULT_VOLUME_VALUE    1.0f
#define DEFAULT_BALANCE_VALUE   0.0f

float PORT_GetFloatValue(void* controlIDV) {
    PortControl *control = (PortControl *)controlIDV;
    Float32 result = 0;

    Float32 subVolumes[control->controlCount];
    Float32 maxVolume;

    switch (control->type) {
    case PortControl::Volume:
        if (!TestPortControlValidity(control)) {
            return DEFAULT_VOLUME_VALUE;
        }

        if (!GetPortControlVolumes(control, subVolumes, &maxVolume)) {
            return DEFAULT_VOLUME_VALUE;
        }
        result = maxVolume;
        break;
    case PortControl::Balance:
        if (!TestPortControlValidity(control)) {
            return DEFAULT_BALANCE_VALUE;
        }

        // balance control always has 2 volume controls
        if (!GetPortControlVolumes(control, subVolumes, &maxVolume)) {
            return DEFAULT_VOLUME_VALUE;
        }
        // calculate balance value
        if (subVolumes[0] > subVolumes[1]) {
            result = -1.0f + (subVolumes[1] / subVolumes[0]);
        } else if (subVolumes[1] > subVolumes[0]) {
            result = 1.0f - (subVolumes[0] / subVolumes[1]);
        } else {
            result = 0.0f;
        }
        break;
    default:
        ERROR1("GetFloatValue requested for non-Float control (control-type == %d)\n", control->type);
        return 0;
    }

    TRACE1("<<PORT_GetFloatValue = %f\n", result);
    return result;
}

void PORT_SetFloatValue(void* controlIDV, float value) {
    TRACE1("> PORT_SetFloatValue = %f\n", value);
    PortControl *control = (PortControl *)controlIDV;

    if (!TestPortControlValidity(control)) {
        return;
    }

    Float32 subVolumes[control->controlCount];
    Float32 maxVolume;

    switch (control->type) {
    case PortControl::Volume:
        if (!GetPortControlVolumes(control, subVolumes, &maxVolume)) {
            return;
        }
        // update the values
        if (maxVolume > 0.001) {
            float multiplicator = value/maxVolume;
            for (int i=0; i<control->controlCount; i++)
                subVolumes[i] *= multiplicator;
        } else {
            // volume for all channels == 0, so set all channels to "value"
            for (int i=0; i<control->controlCount; i++)
                subVolumes[i] = value;
        }
        // set new values
        SetPortControlVolumes(control, subVolumes);
        break;
    case PortControl::Balance:
        // balance control always has 2 volume controls
        if (!GetPortControlVolumes(control, subVolumes, &maxVolume)) {
            return;
        }
        // calculate new values
        if (value < 0.0f) {
            subVolumes[0] = maxVolume;
            subVolumes[1] = maxVolume * (value + 1.0f);
        } else {
            // this case also handles value == 0
            subVolumes[0] = maxVolume * (1.0f - value);
            subVolumes[1] = maxVolume;
        }
        // set new values
        SetPortControlVolumes(control, subVolumes);
        break;
    default:
        ERROR1("PORT_SetFloatValue requested for non-Float control (control-type == %d)\n", control->type);
        return;
    }
}

#endif // USE_PORTS
