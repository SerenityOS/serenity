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


#define USE_ERROR
//#define USE_TRACE

#ifndef WIN32_EXTRA_LEAN
#define WIN32_EXTRA_LEAN
#endif
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>
#include <mmsystem.h>
#include "Ports.h"

/* include to prevent charset problem */
#include "PLATFORM_API_WinOS_Charset_Util.h"

#if USE_PORTS == TRUE

typedef struct tag_PortControlID PortControlID;

typedef struct tag_PortInfo {
    // Windows API stuff
    HMIXER handle;
    INT32 mixerIndex;
    int dstLineCount;        // how many MIXERLINE structs in dstMixerLine
    MIXERLINE* dstLines;
    int srcLineCount;        // how many MIXERLINE structs in srcMixerLine
    MIXERLINE* srcLines;     // contains all the Source Lines of dstLines
    // Java Sound mapping
    int targetPortCount;     // one port per dstLine (playback)
    int sourcePortCount;     // only WAVEIN; one port maps to one srcLine
    LPMIXERLINE* ports;      // points into dstLines and dstLines. Starts with Target Ports (Playback)
    int maxControlCount;       // upper bound of number of controls
    int usedControlIDs;        // number of items already filled in controlIDs
    PortControlID* controlIDs; // the control IDs themselves
    int usedMuxData;
    MIXERCONTROLDETAILS_BOOLEAN* muxData;
} PortInfo;

#define PORT_CONTROL_TYPE_BOOLEAN     1
#define PORT_CONTROL_TYPE_SIGNED      2
#define PORT_CONTROL_TYPE_UNSIGNED    3
//#define PORT_CONTROL_TYPE_UNSIGNED_DB 4
#define PORT_CONTROL_TYPE_FAKE_VOLUME 5
#define PORT_CONTROL_TYPE_FAKE_BALANCE 6
#define PORT_CONTROL_TYPE_MUX         5
#define PORT_CONTROL_TYPE_MIXER       6

typedef struct tag_PortControlID {
    PortInfo*           portInfo;
    INT32               controlType;  // one of PORT_CONTROL_TYPE_XX
    INT32               min;
    INT32               max;
    MIXERCONTROLDETAILS details;
    union {
        MIXERCONTROLDETAILS_BOOLEAN  boolValue;
        MIXERCONTROLDETAILS_SIGNED   signedValue;
        MIXERCONTROLDETAILS_UNSIGNED unsignedValue[2];
        INT32                        muxIndex;
    };
} PortControlID;


int getControlInfo(HMIXER handle, MIXERLINE* line, MIXERLINECONTROLS* controls);

INT32 PORT_GetPortMixerCount() {
    return (INT32) mixerGetNumDevs();
}

#ifdef USE_TRACE

char* getLineFlags(DWORD flags) {
    static char ret[100];
    ret[0]=0;
    if (flags & MIXERLINE_LINEF_ACTIVE) {
        strcat(ret, "ACTIVE ");
        flags ^= MIXERLINE_LINEF_ACTIVE;
    }
    if (flags & MIXERLINE_LINEF_DISCONNECTED) {
        strcat(ret, "DISCONNECTED ");
        flags ^= MIXERLINE_LINEF_DISCONNECTED;
    }
    if (flags & MIXERLINE_LINEF_SOURCE) {
        strcat(ret, "SOURCE ");
        flags ^= MIXERLINE_LINEF_SOURCE;
    }
    if (flags!=0) {
        UINT_PTR r = (UINT_PTR) ret;
        r += strlen(ret);
        sprintf((char*) r, "%d", flags);
    }
    return ret;
}

char* getComponentType(int componentType) {
    switch (componentType) {
        case MIXERLINE_COMPONENTTYPE_DST_HEADPHONES:   return "DST_HEADPHONES";
        case MIXERLINE_COMPONENTTYPE_DST_LINE:         return "DST_LINE";
        case MIXERLINE_COMPONENTTYPE_DST_SPEAKERS:     return "DST_SPEAKERS";
        case MIXERLINE_COMPONENTTYPE_DST_DIGITAL:      return "DST_DIGITAL";
        case MIXERLINE_COMPONENTTYPE_DST_MONITOR:      return "DST_MONITOR";
        case MIXERLINE_COMPONENTTYPE_DST_TELEPHONE:    return "DST_TELEPHONE";
        case MIXERLINE_COMPONENTTYPE_DST_UNDEFINED:    return "DST_UNDEFINED";
        case MIXERLINE_COMPONENTTYPE_DST_VOICEIN:      return "DST_VOICEIN";
        case MIXERLINE_COMPONENTTYPE_DST_WAVEIN:       return "DST_WAVEIN";

        case MIXERLINE_COMPONENTTYPE_SRC_COMPACTDISC:  return "SRC_COMPACTDISC";
        case MIXERLINE_COMPONENTTYPE_SRC_LINE:         return "SRC_LINE";
        case MIXERLINE_COMPONENTTYPE_SRC_MICROPHONE:   return "SRC_MICROPHONE";
        case MIXERLINE_COMPONENTTYPE_SRC_ANALOG:       return "SRC_ANALOG";
        case MIXERLINE_COMPONENTTYPE_SRC_AUXILIARY:    return "SRC_AUXILIARY";
        case MIXERLINE_COMPONENTTYPE_SRC_DIGITAL:      return "SRC_DIGITAL";
        case MIXERLINE_COMPONENTTYPE_SRC_PCSPEAKER:    return "SRC_PCSPEAKER";
        case MIXERLINE_COMPONENTTYPE_SRC_SYNTHESIZER:  return "SRC_SYNTHESIZER";
        case MIXERLINE_COMPONENTTYPE_SRC_TELEPHONE:    return "SRC_TELEPHONE";
        case MIXERLINE_COMPONENTTYPE_SRC_UNDEFINED:    return "SRC_UNDEFINED";
        case MIXERLINE_COMPONENTTYPE_SRC_WAVEOUT:      return "SRC_WAVEOUT";
    }
    return "";
}

void printMixerLine(MIXERLINE* mixerLine) {
    TRACE2("MIXERLINE destination=%d, source=%d, ", mixerLine->dwDestination, mixerLine->dwSource);
    TRACE3("channels=%d, connections=%d, controls=%d, ", mixerLine->cChannels, mixerLine->cConnections, mixerLine->cControls);
    TRACE3("\"%s\", fdwLine=%s, componentType=%s\n", mixerLine->szName,  getLineFlags(mixerLine->fdwLine), getComponentType(mixerLine->dwComponentType));
}

char* getControlClass(int controlType) {
    switch (controlType & MIXERCONTROL_CT_CLASS_MASK) {
        case MIXERCONTROL_CT_CLASS_CUSTOM : return "CLASS_CUSTOM";
        case MIXERCONTROL_CT_CLASS_FADER  : return "CLASS_FADER ";
        case MIXERCONTROL_CT_CLASS_LIST   : return "CLASS_LIST  ";
        case MIXERCONTROL_CT_CLASS_METER  : return "CLASS_METER ";
        case MIXERCONTROL_CT_CLASS_NUMBER : return "CLASS_NUMBER";
        case MIXERCONTROL_CT_CLASS_SLIDER : return "CLASS_SLIDER";
        case MIXERCONTROL_CT_CLASS_SWITCH : return "CLASS_SWITCH";
        case MIXERCONTROL_CT_CLASS_TIME   : return "CLASS_TIME  ";
    }
    return "unknown class";
}

char* getControlType(int controlType) {
    switch (controlType) {
        case MIXERCONTROL_CONTROLTYPE_CUSTOM          : return "CUSTOM         ";
        case MIXERCONTROL_CONTROLTYPE_BASS            : return "BASS           ";
        case MIXERCONTROL_CONTROLTYPE_EQUALIZER       : return "EQUALIZER      ";
        case MIXERCONTROL_CONTROLTYPE_FADER           : return "FADER          ";
        case MIXERCONTROL_CONTROLTYPE_TREBLE          : return "TREBLE         ";
        case MIXERCONTROL_CONTROLTYPE_VOLUME          : return "VOLUME         ";
        case MIXERCONTROL_CONTROLTYPE_MIXER           : return "MIXER          ";
        case MIXERCONTROL_CONTROLTYPE_MULTIPLESELECT  : return "MULTIPLESELECT ";
        case MIXERCONTROL_CONTROLTYPE_MUX             : return "MUX            ";
        case MIXERCONTROL_CONTROLTYPE_SINGLESELECT    : return "SINGLESELECT   ";
        case MIXERCONTROL_CONTROLTYPE_BOOLEANMETER    : return "BOOLEANMETER   ";
        case MIXERCONTROL_CONTROLTYPE_PEAKMETER       : return "PEAKMETER      ";
        case MIXERCONTROL_CONTROLTYPE_SIGNEDMETER     : return "SIGNEDMETER    ";
        case MIXERCONTROL_CONTROLTYPE_UNSIGNEDMETER   : return "UNSIGNEDMETER  ";
        case MIXERCONTROL_CONTROLTYPE_DECIBELS        : return "DECIBELS       ";
        case MIXERCONTROL_CONTROLTYPE_PERCENT         : return "PERCENT        ";
        case MIXERCONTROL_CONTROLTYPE_SIGNED          : return "SIGNED         ";
        case MIXERCONTROL_CONTROLTYPE_UNSIGNED        : return "UNSIGNED       ";
        case MIXERCONTROL_CONTROLTYPE_PAN             : return "PAN            ";
        case MIXERCONTROL_CONTROLTYPE_QSOUNDPAN       : return "QSOUNDPAN      ";
        case MIXERCONTROL_CONTROLTYPE_SLIDER          : return "SLIDER         ";
        case MIXERCONTROL_CONTROLTYPE_BOOLEAN         : return "BOOLEAN        ";
        case MIXERCONTROL_CONTROLTYPE_BUTTON          : return "BUTTON         ";
        case MIXERCONTROL_CONTROLTYPE_LOUDNESS        : return "LOUDNESS       ";
        case MIXERCONTROL_CONTROLTYPE_MONO            : return "MONO           ";
        case MIXERCONTROL_CONTROLTYPE_MUTE            : return "MUTE           ";
        case MIXERCONTROL_CONTROLTYPE_ONOFF           : return "ONOFF          ";
        case MIXERCONTROL_CONTROLTYPE_STEREOENH       : return "STEREOENH      ";
        case MIXERCONTROL_CONTROLTYPE_MICROTIME       : return "MICROTIME      ";
        case MIXERCONTROL_CONTROLTYPE_MILLITIME       : return "MILLITIME      ";
    }
    return "unknown";
}

char* getControlState(DWORD controlState) {
    static char ret[100];
    ret[0]=0;
    if (controlState & MIXERCONTROL_CONTROLF_DISABLED) {
        strcat(ret, "DISABLED ");
        controlState ^= MIXERCONTROL_CONTROLF_DISABLED;
    }
    if (controlState & MIXERCONTROL_CONTROLF_MULTIPLE) {
        strcat(ret, "MULTIPLE ");
        controlState ^= MIXERCONTROL_CONTROLF_MULTIPLE;
    }
    if (controlState & MIXERCONTROL_CONTROLF_UNIFORM) {
        strcat(ret, "UNIFORM ");
        controlState ^= MIXERCONTROL_CONTROLF_UNIFORM;
    }
    if (controlState!=0) {
        UINT_PTR r = (UINT_PTR) ret;
        r += strlen(ret);
        sprintf((char*) r, "%d", controlState);
    }
    return ret;
}

void printControl(MIXERCONTROL* control) {
    TRACE3("    %s: dwControlType=%s/%s, ", control->szName, getControlClass(control->dwControlType), getControlType(control->dwControlType));
    TRACE3("multpleItems=%d, state=%d, %s\n", control->cMultipleItems, control->fdwControl, getControlState(control->fdwControl));
}

void printMixerLineControls(HMIXER handle, MIXERLINE* mixerLine) {
    MIXERLINECONTROLS controls;
    DWORD i;
    TRACE1("  Controls for %s:\n", mixerLine->szName);
    if (getControlInfo(handle, mixerLine, &controls)) {
        for (i = 0; i < controls.cControls; i++) {
            printControl(&controls.pamxctrl[i]);
        }
        if (controls.pamxctrl) {
            free(controls.pamxctrl);
            controls.pamxctrl = NULL;
        }
    }
}

void printInfo(PortInfo* info) {
    TRACE5(" PortInfo %p: handle=%p, mixerIndex=%d, dstLineCount=%d, dstLines=%p, ", info, (void*) info->handle, info->mixerIndex, info->dstLineCount, info->dstLines);
    TRACE5("srcLineCount=%d, srcLines=%p, targetPortCount=%d, sourcePortCount=%d, ports=%p, ", info->srcLineCount, info->srcLines, info->targetPortCount, info->sourcePortCount, info->ports);
    TRACE3("maxControlCount=%d, usedControlIDs=%d, controlIDs=%p \n", info->maxControlCount, info->usedControlIDs, info->controlIDs);
    TRACE2("usedMuxData=%d, muxData=%p, controlIDs=%p \n", info->usedMuxData, info->muxData);
}

#endif // USE_TRACE

// internal utility functions

int getMixerLineByDestination(HMIXER handle, DWORD dstIndex, MIXERLINE* mixerLine) {
    mixerLine->cbStruct = sizeof(MIXERLINE);
    mixerLine->dwDestination = dstIndex;
    if (mixerGetLineInfo((HMIXEROBJ) handle, mixerLine,
                          MIXER_GETLINEINFOF_DESTINATION | MIXER_OBJECTF_HMIXER
                         ) == MMSYSERR_NOERROR) {
        return TRUE;
    }
    mixerLine->cControls = 0;
    mixerLine->cConnections = 0;
    return FALSE;
}

int getMixerLineByType(HMIXER handle, DWORD linetype, MIXERLINE* mixerLine) {
    mixerLine->cbStruct = sizeof(MIXERLINE);
    mixerLine->dwComponentType = linetype;
    if (mixerGetLineInfo((HMIXEROBJ) handle, mixerLine,
                          MIXER_GETLINEINFOF_COMPONENTTYPE | MIXER_OBJECTF_HMIXER
                         ) == MMSYSERR_NOERROR) {
        return TRUE;
    }
    mixerLine->cControls = 0;
    mixerLine->cConnections = 0;
    return FALSE;
}

int getMixerLineBySource(HMIXER handle, DWORD dstIndex, DWORD srcIndex, MIXERLINE* mixerLine) {
    mixerLine->cbStruct = sizeof(MIXERLINE);
    mixerLine->dwDestination = dstIndex;
    mixerLine->dwSource = srcIndex;
    if (mixerGetLineInfo((HMIXEROBJ) handle, mixerLine,
                          MIXER_GETLINEINFOF_SOURCE | MIXER_OBJECTF_HMIXER
                         ) == MMSYSERR_NOERROR) {
        return TRUE;
    }
    mixerLine->cControls = 0;
    mixerLine->cConnections = 0;
    return FALSE;
}

int getControlInfo(HMIXER handle, MIXERLINE* line, MIXERLINECONTROLS* controls) {
    int ret = FALSE;

    //TRACE2(">getControlInfo for line %s with %d controls\n", line->szName, line->cControls);
    controls->pamxctrl = NULL;
    if (line->cControls > 0) {
        // line points to the requested line.
        // Reserve memory for the control infos
        controls->cbStruct = sizeof(MIXERLINECONTROLS);
        controls->dwLineID = line->dwLineID;
        controls->cControls = line->cControls;
        controls->cbmxctrl = sizeof(MIXERCONTROL);
        controls->pamxctrl = (MIXERCONTROL*) malloc(sizeof(MIXERCONTROL) * line->cControls);
        if (controls->pamxctrl) {
            //TRACE0(" calling mixerGetLineControls\n");
            ret = mixerGetLineControls((HMIXEROBJ) handle, controls,
                                       MIXER_GETLINECONTROLSF_ALL | MIXER_OBJECTF_HMIXER) == MMSYSERR_NOERROR;
        }
    }
    if (!ret) {
        if (controls->pamxctrl) {
            free(controls->pamxctrl);
            controls->pamxctrl = NULL;
        }
    }
    //TRACE0("<getControlInfo \n");
    return ret;
}

// returns TRUE if there are more than MIXER/MUX controls in this line
// if controls is non-NULL, it will be filled with the info
int lineHasControls(HMIXER handle, MIXERLINE* line, MIXERLINECONTROLS* controls) {
    MIXERLINECONTROLS localControls;
    int ret = FALSE;
    UINT i;

    localControls.pamxctrl = NULL;
    if (controls == NULL) {
        controls = &localControls;
    }
    if (getControlInfo(handle, line, controls)) {
        for (i = 0; !ret && (i < controls->cControls); i++) {
            switch (controls->pamxctrl[i].dwControlType & MIXERCONTROL_CT_CLASS_MASK) {
                case MIXERCONTROL_CT_CLASS_FADER  : // fall through
                case MIXERCONTROL_CT_CLASS_SLIDER : // fall through
                case MIXERCONTROL_CT_CLASS_SWITCH : ret = TRUE;
            }
        }
    }
    if (localControls.pamxctrl) {
        free(localControls.pamxctrl);
        localControls.pamxctrl = NULL;
    }
    return ret;
}


///// implemented functions of Ports.h

INT32 PORT_GetPortMixerDescription(INT32 mixerIndex, PortMixerDescription* description) {
    MIXERCAPSW mixerCaps;
    if (mixerGetDevCapsW(mixerIndex, &mixerCaps, sizeof(MIXERCAPSW)) == MMSYSERR_NOERROR) {
        UnicodeToUTF8AndCopy(description->name, mixerCaps.szPname, PORT_STRING_LENGTH);
        sprintf(description->version, "%d.%d", (mixerCaps.vDriverVersion & 0xFF00) >> 8, mixerCaps.vDriverVersion & 0xFF);
        strncpy(description->description, "Port Mixer", PORT_STRING_LENGTH-1);
        return TRUE;
    }
    return FALSE;
}

int getDestinationCount(HMIXER handle) {
    int ret = 0;
    MIXERCAPSW mixerCaps;

    if (mixerGetDevCapsW((UINT_PTR) handle, &mixerCaps, sizeof(MIXERCAPSW)) == MMSYSERR_NOERROR) {
        ret = mixerCaps.cDestinations;
    }
    return ret;
}

void* PORT_Open(INT32 mixerIndex) {
    PortInfo* info = NULL;
    MMRESULT mmres;
    HMIXER handle;
    MIXERLINE* waveInLine;
    int success = FALSE;
    int src, dst, srcIndex, waveInHasControls;
    int dstCount;

    TRACE0("PORT_Open\n");
    mmres = mixerOpen((LPHMIXER) &handle, mixerIndex, 0, 0, MIXER_OBJECTF_MIXER);
    if (mmres != MMSYSERR_NOERROR) {
        return NULL;
    }

    info = (PortInfo*) malloc(sizeof(PortInfo));
    if (info != NULL) {
        success = TRUE;
        memset(info, 0, sizeof(PortInfo));
        info->handle = handle;
        info->mixerIndex = mixerIndex;
        waveInLine = NULL;
        waveInHasControls = FALSE;
        // number of destinations
        dstCount = getDestinationCount(handle);
        if (dstCount) {
            info->dstLines = (MIXERLINE*) malloc(dstCount * sizeof(MIXERLINE));
            success = (info->dstLines != NULL);
        }
        if (success && info->dstLines) {
            // go through all destinations and fill the structures in PortInfo
            for (dst = 0; dst < dstCount; dst++) {
                if (getMixerLineByDestination(handle, dst, &info->dstLines[info->dstLineCount])) {
                    info->srcLineCount += info->dstLines[info->dstLineCount].cConnections;
                    if (info->dstLines[info->dstLineCount].dwComponentType == MIXERLINE_COMPONENTTYPE_DST_WAVEIN && !waveInLine) {
                        waveInLine = &info->dstLines[info->dstLineCount];
                        info->sourcePortCount = waveInLine->cConnections;
                        if (lineHasControls(handle, waveInLine, NULL)) {
                            // add a single port for all the controls that do not show in the MUX/MIXER controls
                            info->sourcePortCount++;
                            waveInHasControls = TRUE;
                        }
                    } else {
                        info->targetPortCount++;
                    }
                    info->dstLineCount++;
                }
            }
        }
        if (info->srcLineCount) {
            info->srcLines = (MIXERLINE*) malloc(info->srcLineCount * sizeof(MIXERLINE));
            success = (info->srcLines != NULL);
        }
        if (success && info->srcLines) {
            // go through all destinations and fill the source line structures in PortInfo
            srcIndex = 0;
            for (dst = 0; dst < info->dstLineCount; dst++) {
                // remember the srcIndex for mapping the srcLines to this destination line
                info->dstLines[dst].dwUser = srcIndex;
                for (src = 0; src < (int) info->dstLines[dst].cConnections; src++) {
                    getMixerLineBySource(handle, dst, src, &info->srcLines[srcIndex++]);
                }
            }
        }
        // now create the mapping to Java Sound
        if ((info->targetPortCount + info->sourcePortCount) > 0) {
            info->ports = (LPMIXERLINE*) malloc((info->targetPortCount + info->sourcePortCount) * sizeof(LPMIXERLINE));
            success = (info->ports != NULL);
        }
        if (success && info->ports) {
            // first add the target MIXERLINEs to the array
            srcIndex = 0;
            for (dst = 0; dst < info->dstLineCount; dst++) {
                if (waveInLine != &info->dstLines[dst]) {
                    info->ports[srcIndex++] = &info->dstLines[dst];
                }
            }
            if (srcIndex != info->targetPortCount) {
                ERROR2("srcIndex=%d is NOT targetPortCount=%d !\n", srcIndex, info->targetPortCount);
            }
            //srcIndex = info->targetPortCount; // should be automatic!
            if (waveInLine) {
                // if the recording destination line has controls, add the line
                if (waveInHasControls) {
                    info->ports[srcIndex++] = waveInLine;
                }
                for (src = 0; src < (int) waveInLine->cConnections; src++) {
                    info->ports[srcIndex++] = &info->srcLines[src + waveInLine->dwUser];
                }
            }
            if (srcIndex != (info->targetPortCount + info->sourcePortCount)) {
                ERROR2("srcIndex=%d is NOT PortCount=%d !\n", srcIndex, (info->targetPortCount + info->sourcePortCount));
            }
        }
    }
    if (!success) {
        if (handle != NULL) {
            mixerClose(handle);
        }
        PORT_Close((void*) info);
        info = NULL;
    }
    return info;
}

void PORT_Close(void* id) {
    TRACE0("PORT_Close\n");
    if (id != NULL) {
        PortInfo* info = (PortInfo*) id;
        if (info->handle) {
            mixerClose(info->handle);
            info->handle = NULL;
        }
        if (info->dstLines) {
            free(info->dstLines);
            info->dstLines = NULL;
        }
        if (info->srcLines) {
            free(info->srcLines);
            info->srcLines=NULL;
        }
        if (info->ports) {
            free(info->ports);
            info->ports = NULL;
        }
        if (info->controlIDs) {
            free(info->controlIDs);
            info->controlIDs = NULL;
        }
        if (info->muxData) {
            free(info->muxData);
            info->muxData = NULL;
        }
        free(info);
    }
}

INT32 PORT_GetPortCount(void* id) {
    int ret = 0;
    PortInfo* info = (PortInfo*) id;
    if (info != NULL) {
        ret = info->targetPortCount + info->sourcePortCount;
    }
    return ret;
}

int componentType2type(DWORD componentType) {
    int ret = 0;
    if (componentType >= MIXERLINE_COMPONENTTYPE_DST_FIRST && componentType <= MIXERLINE_COMPONENTTYPE_DST_LAST) {
        ret = PORT_DST_UNKNOWN;
    }
    else if (componentType >= MIXERLINE_COMPONENTTYPE_SRC_FIRST && componentType <= MIXERLINE_COMPONENTTYPE_SRC_LAST) {
        ret = PORT_SRC_UNKNOWN;
    }
    // handle special cases
    switch (componentType) {
        case MIXERLINE_COMPONENTTYPE_DST_HEADPHONES:  ret = PORT_DST_HEADPHONE; break;
        case MIXERLINE_COMPONENTTYPE_DST_LINE:        ret = PORT_DST_LINE_OUT; break;
        case MIXERLINE_COMPONENTTYPE_DST_SPEAKERS:    ret = PORT_DST_SPEAKER; break;
        case MIXERLINE_COMPONENTTYPE_SRC_COMPACTDISC: ret = PORT_SRC_COMPACT_DISC; break;
        case MIXERLINE_COMPONENTTYPE_SRC_LINE:        ret = PORT_SRC_LINE_IN; break;
        case MIXERLINE_COMPONENTTYPE_SRC_MICROPHONE:  ret = PORT_SRC_MICROPHONE; break;
    }
    return ret;
}

INT32 PORT_GetPortType(void* id, INT32 portIndex) {
    MIXERLINE* line;
    PortInfo* info = (PortInfo*) id;
    if ((portIndex >= 0) && (portIndex < PORT_GetPortCount(id))) {
        line = info->ports[portIndex];
        if (line) {
            return componentType2type(line->dwComponentType);
        }
    }
    return 0;
}

INT32 PORT_GetPortName(void* id, INT32 portIndex, char* name, INT32 len) {
    MIXERLINE* line;
    PortInfo* info = (PortInfo*) id;

    if ((portIndex >= 0) && (portIndex < PORT_GetPortCount(id))) {
        line = info->ports[portIndex];
        if (line) {
            strncpy(name, line->szName, len-1);
            name[len-1] = 0;
            return TRUE;
        }
    }
    return FALSE;
}

int getControlCount(HMIXER handle, MIXERLINE* line, INT32* muxCount) {
    MIXERLINECONTROLS controls;
    int ret = 0;
    UINT i;

    controls.pamxctrl = NULL;
    if (getControlInfo(handle, line, &controls)) {
        for (i = 0; i < controls.cControls; i++) {
            switch (controls.pamxctrl[i].dwControlType & MIXERCONTROL_CT_CLASS_MASK) {
                case MIXERCONTROL_CT_CLASS_FADER   : // fall through
                case MIXERCONTROL_CT_CLASS_SLIDER  : // fall through
                case MIXERCONTROL_CT_CLASS_SWITCH  : // fall through
                case MIXERCONTROL_CT_CLASS_LIST    : ret++; break;
            }
            if ((controls.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_MIXER)
                 || (controls.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_MUX)) {
                ret += controls.pamxctrl[i].cMultipleItems;
                if (muxCount) {
                    (*muxCount) += controls.pamxctrl[i].cMultipleItems;
                }
            }
            else if ((controls.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_VOLUME)
                    && (line->cChannels == 2)) {
                ret++; // for FAKE volume/balance pairs
            }
        }
    }
    if (controls.pamxctrl) {
        free(controls.pamxctrl);
        controls.pamxctrl = NULL;
    }
    return ret;
}

MIXERLINE* findDestLine(PortInfo* info, DWORD dwDestination) {
    int i;
    TRACE0(">findDestLine\n");
    for (i = 0; i < info->dstLineCount; i++) {
        if (info->dstLines[i].dwDestination == dwDestination) {
                TRACE0("<findDestLine\n");
            return &(info->dstLines[i]);
        }
    }
    TRACE0("<findDestLine NULL\n");
    return NULL;
}

void createMuxControl(PortInfo* info, PortControlCreator* creator, MIXERLINE* dstLine, DWORD srcLineID, void** controlObjects, int* controlCount) {
    MIXERLINECONTROLS controlInfos;
    MIXERCONTROLDETAILS* details;
    MIXERCONTROLDETAILS_LISTTEXT* listTextDetails = NULL;
    UINT listTextDetailCount = 0;
    PortControlID* controlID;
    UINT i, c;
    int m;

    TRACE0(">createMuxControl\n");
    // go through all controls of dstline
    controlInfos.pamxctrl = NULL;
    if (getControlInfo(info->handle, dstLine, &controlInfos)) {
        for (i = 0; i < controlInfos.cControls; i++) {
            if (((controlInfos.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_MIXER)
                 || (controlInfos.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_MUX))
                && (controlInfos.pamxctrl[i].cMultipleItems > 0)) {
                if (info->usedControlIDs >= info->maxControlCount) {
                    ERROR1("not enough free controlIDs !! maxControlIDs = %d\n", info->maxControlCount);
                    break;
                }
                // get the details for this mux control
                controlID = &(info->controlIDs[info->usedControlIDs]);
                controlID->portInfo = info;
                if (controlInfos.pamxctrl[i].dwControlType == MIXERCONTROL_CONTROLTYPE_MIXER) {
                    controlID->controlType = PORT_CONTROL_TYPE_MIXER;
                } else {
                    controlID->controlType = PORT_CONTROL_TYPE_MUX;
                }
                details = &(controlID->details);
                details->cbStruct = sizeof(MIXERCONTROLDETAILS);
                details->dwControlID = controlInfos.pamxctrl[i].dwControlID;
                details->cChannels = 1;
                details->cMultipleItems = controlInfos.pamxctrl[i].cMultipleItems;
                details->cbDetails = sizeof(MIXERCONTROLDETAILS_LISTTEXT);
                if (!listTextDetails || (listTextDetailCount < (details->cMultipleItems * details->cChannels))) {
                    // need to allocate new listTextDetails
                    if (listTextDetails) {
                        free(listTextDetails);
                        listTextDetails = NULL;
                    }
                    listTextDetailCount = details->cMultipleItems * details->cChannels;
                    listTextDetails = (MIXERCONTROLDETAILS_LISTTEXT*) malloc(listTextDetailCount * sizeof(MIXERCONTROLDETAILS_LISTTEXT));
                    if (!listTextDetails) {
                        ERROR0("createMuxControl: unable to allocate listTextDetails!\n");
                        if (controlInfos.pamxctrl) {
                            free(controlInfos.pamxctrl);
                            controlInfos.pamxctrl = NULL;
                        }
                        TRACE0("<createMuxControl ERROR\n");
                        return;
                    }
                }
                details->paDetails = listTextDetails;
                if (mixerGetControlDetails((HMIXEROBJ) info->handle, details, MIXER_GETCONTROLDETAILSF_LISTTEXT | MIXER_OBJECTF_HMIXER) != MMSYSERR_NOERROR) {
                    ERROR0("createMuxControl: unable to get control details!\n");
                    continue;
                }
                // prevent freeing this data
                details->paDetails = NULL;
                // go through all mux items. If the line matches, then add a BOOLEAN select control
                for (c = 0; c < details->cMultipleItems; c++) {
                    if (listTextDetails[c].dwParam1 == srcLineID) {
                        // we have found the line in the MUX lines.
                        controlID->muxIndex = c;
                        details->cbDetails = sizeof(MIXERCONTROLDETAILS_BOOLEAN);
                        // now look if any other controlID was already part of this MUX line
                        for (m = 0; m < info->usedControlIDs; m++) {
                            if (info->controlIDs[m].details.dwControlID == details->dwControlID) {
                                // reuse the MUX Data
                                TRACE2("Reusing paDetails=%p of controlID[%d]\n", info->controlIDs[m].details.paDetails, m);
                                details->paDetails = info->controlIDs[m].details.paDetails;
                                break;
                            }
                        }
                        if (!details->paDetails) {
                            // first time this MUX control is used, allocate some of the muxData
                            details->paDetails = &(info->muxData[info->usedMuxData]);
                            TRACE2("Setting paDetails=%p to muxData[%d] \n", details->paDetails, info->usedMuxData);
                            info->usedMuxData += details->cMultipleItems;
                        }
                        // finally this line can be added
                        controlObjects[*controlCount] = (creator->newBooleanControl)(creator, controlID, CONTROL_TYPE_SELECT);
                        (*controlCount)++;
                        info->usedControlIDs++;
                        break;
                    }
                }
            }
        }
    }
    if (listTextDetails) {
        free(listTextDetails);
        listTextDetails = NULL;
    }
    if (controlInfos.pamxctrl) {
        free(controlInfos.pamxctrl);
        controlInfos.pamxctrl = NULL;
    }
    TRACE0("<createMuxControl\n");
}

void createPortControl(PortInfo* info, PortControlCreator* creator, MIXERCONTROL* mixerControl,
                       INT32 type, void** controlObjects, int* controlCount) {
    PortControlID* controlID;
    void* newControl = NULL;
    char* typeName = mixerControl->szName;
    float min;
    TRACE0(">createPortControl\n");

    // fill the ControlID structure and add this control
    if (info->usedControlIDs >= info->maxControlCount) {
        ERROR1("not enough free controlIDs !! maxControlIDs = %d\n", info->maxControlCount);
        return;
    }
    controlID = &(info->controlIDs[info->usedControlIDs]);
    controlID->portInfo = info;
    controlID->controlType = type;
    controlID->details.cbStruct = sizeof(MIXERCONTROLDETAILS);
    controlID->details.dwControlID = mixerControl->dwControlID;
    controlID->details.cChannels = 1; // uniform
    controlID->details.cMultipleItems = 0;
    switch (type) {
        case PORT_CONTROL_TYPE_BOOLEAN:
            TRACE0(" PORT_CONTROL_TYPE_BOOLEAN\n");
            controlID->details.cbDetails = sizeof(MIXERCONTROLDETAILS_BOOLEAN);
            controlID->details.paDetails = &(controlID->boolValue);
            if (mixerControl->dwControlType == MIXERCONTROL_CONTROLTYPE_MUTE) {
                typeName = CONTROL_TYPE_MUTE;
            }
            newControl = (creator->newBooleanControl)(creator, controlID, typeName);
            break;
        case PORT_CONTROL_TYPE_SIGNED:
            TRACE0(" PORT_CONTROL_TYPE_SIGNED\n");
            controlID->details.cbDetails = sizeof(MIXERCONTROLDETAILS_SIGNED);
            controlID->details.paDetails = &(controlID->signedValue);
            controlID->min = (INT32) mixerControl->Bounds.lMinimum;
            controlID->max = (INT32) mixerControl->Bounds.lMaximum;
            if (mixerControl->dwControlType == MIXERCONTROL_CONTROLTYPE_PAN) {
                typeName = CONTROL_TYPE_PAN;
            }
            newControl = (creator->newFloatControl)(creator, controlID, typeName,
                -1.0f, 1.0f, 2.0f / (controlID->max - controlID->min + 1), "");
            break;
        case PORT_CONTROL_TYPE_FAKE_VOLUME:  // fall through
        case PORT_CONTROL_TYPE_FAKE_BALANCE: // fall through
        case PORT_CONTROL_TYPE_UNSIGNED:
            TRACE0(" PORT_CONTROL_TYPE_UNSIGNED\n");
            controlID->details.cbDetails = sizeof(MIXERCONTROLDETAILS_UNSIGNED);
            controlID->details.paDetails = &(controlID->unsignedValue[0]);
            controlID->min = (INT32) mixerControl->Bounds.dwMinimum;
            controlID->max = (INT32) mixerControl->Bounds.dwMaximum;
            min = 0.0f;
            if ((type == PORT_CONTROL_TYPE_FAKE_VOLUME)
               || (mixerControl->dwControlType == MIXERCONTROL_CONTROLTYPE_VOLUME)) {
                typeName = CONTROL_TYPE_VOLUME;
            }
            if (type == PORT_CONTROL_TYPE_FAKE_BALANCE) {
                typeName = CONTROL_TYPE_BALANCE;
                min = -1.0f;
            }
            if ((type == PORT_CONTROL_TYPE_FAKE_VOLUME)
               || (type == PORT_CONTROL_TYPE_FAKE_BALANCE)) {
                controlID->details.cChannels = 2;
            }
            TRACE0(" ....PORT_CONTROL_TYPE_UNSIGNED\n");
            newControl = (creator->newFloatControl)(creator, controlID, typeName,
                min, 1.0f, 1.0f / (controlID->max - controlID->min + 1), "");
            break;
        default:
            ERROR1("createPortControl: unknown type %d !", type);
            break;
    }
    if (newControl) {
        controlObjects[*controlCount] = newControl;
        (*controlCount)++;
        info->usedControlIDs++;
    }
    TRACE0("<createPortControl\n");
}

void createLineControls(PortInfo* info, PortControlCreator* creator, MIXERLINE* line, void** controlObjects, int* controlCount) {
    MIXERLINECONTROLS controlInfos;
    MIXERCONTROL* mixerControl;
    UINT i;
    INT32 type;

    TRACE1(">createLineControls for line %s\n", line->szName);
    // go through all controls of line
    controlInfos.pamxctrl = NULL;
    if (getControlInfo(info->handle, line, &controlInfos)) {
        for (i = 0; i < controlInfos.cControls; i++) {
            TRACE1("  %d\n", i);
            mixerControl = &(controlInfos.pamxctrl[i]);
            type = 0;
            switch (mixerControl->dwControlType) {
                case MIXERCONTROL_CONTROLTYPE_BOOLEAN  : // fall through
                case MIXERCONTROL_CONTROLTYPE_BUTTON   : // fall through
                case MIXERCONTROL_CONTROLTYPE_LOUDNESS : // fall through
                case MIXERCONTROL_CONTROLTYPE_MONO     : // fall through
                case MIXERCONTROL_CONTROLTYPE_MUTE     : // fall through
                case MIXERCONTROL_CONTROLTYPE_ONOFF    : // fall through
                case MIXERCONTROL_CONTROLTYPE_STEREOENH: type = PORT_CONTROL_TYPE_BOOLEAN; break;

                case MIXERCONTROL_CONTROLTYPE_PAN      : // fall through
                case MIXERCONTROL_CONTROLTYPE_QSOUNDPAN: // fall through
                case MIXERCONTROL_CONTROLTYPE_SLIDER   : type = PORT_CONTROL_TYPE_SIGNED; break;

                case MIXERCONTROL_CONTROLTYPE_BASS     : // fall through
                //case MIXERCONTROL_CONTROLTYPE_EQUALIZER: // fall through
                case MIXERCONTROL_CONTROLTYPE_FADER    : // fall through
                case MIXERCONTROL_CONTROLTYPE_TREBLE   : type = PORT_CONTROL_TYPE_UNSIGNED; break;
                case MIXERCONTROL_CONTROLTYPE_VOLUME   :
                    type = PORT_CONTROL_TYPE_UNSIGNED;
                    if (line->cChannels == 2 && ((mixerControl->fdwControl & MIXERCONTROL_CONTROLF_UNIFORM) == 0)) {
                        type = PORT_CONTROL_TYPE_FAKE_VOLUME;
                    }
                    break;
            }
            if (type != 0) {
                createPortControl(info, creator, mixerControl, type, controlObjects, controlCount);
                // create fake balance for fake volume
                if (type == PORT_CONTROL_TYPE_FAKE_VOLUME) {
                    createPortControl(info, creator, mixerControl, PORT_CONTROL_TYPE_FAKE_BALANCE, controlObjects, controlCount);
                }
            }
        }
    }
    if (controlInfos.pamxctrl) {
        free(controlInfos.pamxctrl);
        controlInfos.pamxctrl = NULL;
    }
    TRACE0("<createLineControls\n");
}

void addCompoundControl(PortInfo* info, PortControlCreator* creator, char* name, void** controlObjects, int* controlCount) {
    void* compControl;

    TRACE1(">addCompoundControl %d controls\n", *controlCount);
    if (*controlCount) {
        // create compound control and add it to the vector
        compControl = (creator->newCompoundControl)(creator, name, controlObjects, *controlCount);
        if (compControl) {
            TRACE1(" addCompoundControl: calling addControl %p\n", compControl);
            (creator->addControl)(creator, compControl);
        }
        *controlCount = 0;
    }
    TRACE0("<addCompoundControl\n");
}

void addAllControls(PortInfo* info, PortControlCreator* creator, void** controlObjects, int* controlCount) {
    int i = 0;

    TRACE0(">addAllControl\n");
    // go through all controls and add them to the vector
    for (i = 0; i < *controlCount; i++) {
        (creator->addControl)(creator, controlObjects[i]);
    }
    *controlCount = 0;
    TRACE0("<addAllControl\n");
}



void PORT_GetControls(void* id, INT32 portIndex, PortControlCreator* creator) {
    MIXERLINE* line;
    PortInfo* info = (PortInfo*) id;
    int portCount = PORT_GetPortCount(id);
    void** controls = NULL;
    int controlCount;
    UINT i;

    TRACE4(">PORT_GetControls(id=%p, portIndex=%d). controlIDs=%p, maxControlCount=%d\n", id, portIndex, info->controlIDs, info->maxControlCount);
    if ((portIndex >= 0) && (portIndex < portCount)) {
        line = info->ports[portIndex];
        if (line) {
            // if the memory isn't reserved for the control structures, allocate it
            if (!info->controlIDs) {
                int i, maxCount = 0, muxCount = 0;
                TRACE0("getControl: allocate mem\n");
                // get a maximum number of controls
                // first for all destination lines
                for (i = 0; i < info->dstLineCount; i++) {
                    MIXERLINE* thisLine = &(info->dstLines[i]);
                    maxCount += getControlCount(info->handle, thisLine, &muxCount);
                }
                // then all source lines
                for (i = 0; i < info->srcLineCount; i++) {
                    MIXERLINE* thisLine = &(info->srcLines[i]);
                    maxCount += getControlCount(info->handle, thisLine, &muxCount);
                }
                info->maxControlCount = maxCount;
                if (maxCount > 0) {
                    info->controlIDs = (PortControlID*) malloc(sizeof(PortControlID) * maxCount);
                } else {
                    // no ports: nothing to do !
                    return;
                }
                TRACE2("Creating muxData for %d elements and %d controlIDs.\n", muxCount, maxCount);
                if (muxCount > 0) {
                    info->muxData = (MIXERCONTROLDETAILS_BOOLEAN*) malloc(sizeof(MIXERCONTROLDETAILS_BOOLEAN) * muxCount);
                }
                if (!info->controlIDs || (muxCount && !info->muxData)) {
                    ERROR3("PORT_GetControls: info->controlIDs=%p, muxCount=%d,  info->muxData=%p !!\n", info->controlIDs, muxCount, info->muxData);
                    return;
                }
            }
            if (info->maxControlCount == 0) {
                return;
            }
            controls = (void*) malloc(info->maxControlCount * sizeof(void*));
            if (!controls) {
                ERROR0("PORT_GetControls: couldn't allocate controls!\n");
                return;
            }

            // add controls of this line
            controlCount = 0;
            // if this line is part of MUX, add the respective BOOLEANCONTROL as a control
            if ((line->fdwLine & MIXERLINE_LINEF_SOURCE) == MIXERLINE_LINEF_SOURCE) {
                MIXERLINE* dstLine = findDestLine(info, line->dwDestination);
                TRACE0("Port_getControls: this is a source line\n");
                if (dstLine) {
                    // selection controls (implemented as Mute control)
                    createMuxControl(info, creator, dstLine, line->dwLineID, controls, &controlCount);
                }
                // then add all controls in one compound control
                createLineControls(info, creator, line, controls, &controlCount);
                addCompoundControl(info, creator, line->szName, controls, &controlCount);
            } else {
                TRACE0("getControl: this is a dest line\n");
                // if this is a destination line, add its controls
                createLineControls(info, creator, line, controls, &controlCount);
                addAllControls(info, creator, controls, &controlCount);
                // then add all controls of its source lines as one compound control
                for (i = 0; i < line->cConnections; i++) {
                    // then add all controls
                    MIXERLINE* srcLine = &(info->srcLines[line->dwUser + i]);
                    TRACE1("PORT_getControls: add source line %d\n", i);
                    createLineControls(info, creator, srcLine, controls, &controlCount);
                    addCompoundControl(info, creator, srcLine->szName, controls, &controlCount);
                }
            }
        }
    }
    if (controls) {
        free(controls);
    }
    TRACE0("< PORT_getControls\n");
}

int getControlValue(PortControlID* controlID) {
    if (mixerGetControlDetails((HMIXEROBJ) controlID->portInfo->handle, &(controlID->details),
            MIXER_GETCONTROLDETAILSF_VALUE | MIXER_OBJECTF_HMIXER) != MMSYSERR_NOERROR) {
        ERROR0("getControlValue: unable to get control details!\n");
        //ERROR3("   cbStruct=%d, dwControlID=%d, cChannels=%d, ", controlID->details.cbStruct, controlID->details.dwControlID, controlID->details.cChannels);
        //ERROR2("   cMultipleItems=%d, cbDetails=%d\n", controlID->details.cMultipleItems, controlID->details.cbDetails);
        return FALSE;
    }
    return TRUE;
}

int setControlValue(PortControlID* controlID) {
    if (mixerSetControlDetails((HMIXEROBJ) controlID->portInfo->handle, &(controlID->details),
            MIXER_SETCONTROLDETAILSF_VALUE | MIXER_OBJECTF_HMIXER) != MMSYSERR_NOERROR) {
        ERROR0("setControlValue: unable to set control details!\n");
        //ERROR3("   cbStruct=%d, dwControlID=%d, cChannels=%d, ", controlID->details.cbStruct, controlID->details.dwControlID, controlID->details.cChannels);
        //ERROR2("   cMultipleItems=%d, cbDetails=%d\n", controlID->details.cMultipleItems, controlID->details.cbDetails);
        return FALSE;
    }
    return TRUE;
}

INT32 PORT_GetIntValue(void* controlIDV) {
    PortControlID* controlID = (PortControlID*) controlIDV;
    MIXERCONTROLDETAILS_BOOLEAN* bools;
    int ret = 0;
    if (getControlValue(controlID)) {
        switch (controlID->controlType) {
        case PORT_CONTROL_TYPE_MUX:   // fall through
        case PORT_CONTROL_TYPE_MIXER:
                bools = (MIXERCONTROLDETAILS_BOOLEAN*) controlID->details.paDetails;
                ret = (bools[controlID->muxIndex].fValue)?TRUE:FALSE;
                break;
        case PORT_CONTROL_TYPE_BOOLEAN:
                ret = (controlID->boolValue.fValue)?TRUE:FALSE;
                break;
        default: ERROR1("PORT_GetIntValue: wrong controlType=%d !\n", controlID->controlType);
        }
    }
    return ret;
}

void PORT_SetIntValue(void* controlIDV, INT32 value) {
    PortControlID* controlID = (PortControlID*) controlIDV;
    MIXERCONTROLDETAILS_BOOLEAN* bools;
    UINT i;

    switch (controlID->controlType) {
    case PORT_CONTROL_TYPE_MUX:
        if (!value) {
            // cannot unselect a MUX line
            return;
        }
        if (!getControlValue(controlID)) {
            return;
        }
        bools = (MIXERCONTROLDETAILS_BOOLEAN*) controlID->details.paDetails;
        for (i = 0; i < controlID->details.cMultipleItems; i++) {
            bools[i].fValue = (i == (UINT) controlID->muxIndex)?TRUE:FALSE;
        }
        break;
    case PORT_CONTROL_TYPE_MIXER:
        if (!getControlValue(controlID)) {
            return;
        }
        bools = (MIXERCONTROLDETAILS_BOOLEAN*) controlID->details.paDetails;
        bools[controlID->muxIndex].fValue = (value?TRUE:FALSE);
        break;
    case PORT_CONTROL_TYPE_BOOLEAN:
        controlID->boolValue.fValue = (value?TRUE:FALSE);
        break;
    default:
        ERROR1("PORT_SetIntValue: wrong controlType=%d !\n", controlID->controlType);
        return;
    }
    setControlValue(controlID);
}

float getFakeBalance(PortControlID* controlID) {
    float volL, volR;
    float range = (float) (controlID->max - controlID->min);
    // pan is the ratio of left and right
    volL = (((float) (controlID->unsignedValue[0].dwValue - controlID->min)) / range);
    volR = (((float) (controlID->unsignedValue[1].dwValue - controlID->min)) / range);
    if (volL > volR) {
        return -1.0f + (volR / volL);
    }
    else if (volR > volL) {
        return 1.0f - (volL / volR);
    }
    return 0.0f;
}

float getFakeVolume(PortControlID* controlID) {
    // volume is the greater value of both
    UINT vol = controlID->unsignedValue[0].dwValue;
    if (controlID->unsignedValue[1].dwValue > vol) {
        vol = controlID->unsignedValue[1].dwValue;
    }
    return (((float) (vol - controlID->min)) / (controlID->max - controlID->min));
}

/*
 * sets the unsigned values for left and right volume according to
 * the given volume (0...1) and balance (-1..0..+1)
 */
void setFakeVolume(PortControlID* controlID, float vol, float bal) {
    vol = vol * (controlID->max - controlID->min);
    if (bal < 0.0f) {
        controlID->unsignedValue[0].dwValue = (UINT) (vol  + 0.5f) + controlID->min;
        controlID->unsignedValue[1].dwValue = (UINT) ((vol * (bal + 1.0f)) + 0.5f) + controlID->min;
    } else {
        controlID->unsignedValue[1].dwValue = (UINT) (vol  + 0.5f) + controlID->min;
        controlID->unsignedValue[0].dwValue = (UINT) ((vol * (1.0f - bal)) + 0.5f) + controlID->min;
    }
}

float PORT_GetFloatValue(void* controlIDV) {
    PortControlID* controlID = (PortControlID*) controlIDV;
    float ret = 0.0f;
    float range = (float) (controlID->max - controlID->min);
    if (getControlValue(controlID)) {
        switch (controlID->controlType) {
        case PORT_CONTROL_TYPE_SIGNED:
                ret = ((float) controlID->signedValue.lValue) / controlID->max;
                break;
        case PORT_CONTROL_TYPE_UNSIGNED:
                ret = (((float) (controlID->unsignedValue[0].dwValue - controlID->min)) / range);
                break;
        case PORT_CONTROL_TYPE_FAKE_VOLUME:
                ret = getFakeVolume(controlID);
                break;
        case PORT_CONTROL_TYPE_FAKE_BALANCE:
                ret = getFakeBalance(controlID);
                break;
        default: ERROR1("PORT_GetFloatValue: wrong controlType=%d !\n", controlID->controlType);
        }
    }
    return ret;
}

void PORT_SetFloatValue(void* controlIDV, float value) {
    PortControlID* controlID = (PortControlID*) controlIDV;
    float range = (float) (controlID->max - controlID->min);
    switch (controlID->controlType) {
    case PORT_CONTROL_TYPE_SIGNED:
        controlID->signedValue.lValue = (INT32) ((value * controlID->max) + 0.5f);
        break;
    case PORT_CONTROL_TYPE_UNSIGNED:
        controlID->unsignedValue[0].dwValue = (INT32) ((value * range) + 0.5f) + controlID->min;
        break;
    case PORT_CONTROL_TYPE_FAKE_VOLUME:
        if (!getControlValue(controlID)) {
            return;
        }
        setFakeVolume(controlID, value, getFakeBalance(controlID));
        break;
    case PORT_CONTROL_TYPE_FAKE_BALANCE:
        if (!getControlValue(controlID)) {
            return;
        }
        setFakeVolume(controlID, getFakeVolume(controlID), value);
        break;
    default:
        ERROR1("PORT_SetFloatValue: wrong controlType=%d !\n", controlID->controlType);
        return;
    }
    setControlValue(controlID);
}

#endif // USE_PORTS
