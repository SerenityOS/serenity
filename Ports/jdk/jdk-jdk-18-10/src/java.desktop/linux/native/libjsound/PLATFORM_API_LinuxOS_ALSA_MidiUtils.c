/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
#define USE_TRACE

#include "PLATFORM_API_LinuxOS_ALSA_MidiUtils.h"
#include "PLATFORM_API_LinuxOS_ALSA_CommonUtils.h"
#include <string.h>
#include <sys/time.h>

static INT64 getTimeInMicroseconds() {
    struct timeval tv;

    gettimeofday(&tv, NULL);
    return (tv.tv_sec * 1000000UL) + tv.tv_usec;
}


const char* getErrorStr(INT32 err) {
        return snd_strerror((int) err);
}



// callback for iteration through devices
// returns TRUE if iteration should continue
typedef int (*DeviceIteratorPtr)(UINT32 deviceID,
                                 snd_rawmidi_info_t* rawmidi_info,
                                 snd_ctl_card_info_t* cardinfo,
                                 void *userData);

// for each ALSA device, call iterator. userData is passed to the iterator
// returns total number of iterations
static int iterateRawmidiDevices(snd_rawmidi_stream_t direction,
                                 DeviceIteratorPtr iterator,
                                 void* userData) {
    int count = 0;
    int subdeviceCount;
    int card, dev, subDev;
    char devname[16];
    int err;
    snd_ctl_t *handle;
    snd_rawmidi_t *rawmidi;
    snd_rawmidi_info_t *rawmidi_info;
    snd_ctl_card_info_t *card_info, *defcardinfo = NULL;
    UINT32 deviceID;
    int doContinue = TRUE;

    snd_rawmidi_info_malloc(&rawmidi_info);
    snd_ctl_card_info_malloc(&card_info);

    // 1st try "default" device
    if (direction == SND_RAWMIDI_STREAM_INPUT) {
        err = snd_rawmidi_open(&rawmidi, NULL, ALSA_DEFAULT_DEVICE_NAME,
                               SND_RAWMIDI_NONBLOCK);
    } else if (direction == SND_RAWMIDI_STREAM_OUTPUT) {
        err = snd_rawmidi_open(NULL, &rawmidi, ALSA_DEFAULT_DEVICE_NAME,
                               SND_RAWMIDI_NONBLOCK);
    } else {
        ERROR0("ERROR: iterateRawmidiDevices(): direction is neither"
               " SND_RAWMIDI_STREAM_INPUT nor SND_RAWMIDI_STREAM_OUTPUT\n");
        err = MIDI_INVALID_ARGUMENT;
    }
    if (err < 0) {
        ERROR1("ERROR: snd_rawmidi_open (\"default\"): %s\n",
               snd_strerror(err));
    } else {
        err = snd_rawmidi_info(rawmidi, rawmidi_info);

        snd_rawmidi_close(rawmidi);
        if (err < 0) {
            ERROR1("ERROR: snd_rawmidi_info (\"default\"): %s\n",
                    snd_strerror(err));
        } else {
            // try to get card info
            card = snd_rawmidi_info_get_card(rawmidi_info);
            if (card >= 0) {
                sprintf(devname, ALSA_HARDWARE_CARD, card);
                if (snd_ctl_open(&handle, devname, SND_CTL_NONBLOCK) >= 0) {
                    if (snd_ctl_card_info(handle, card_info) >= 0) {
                        defcardinfo = card_info;
                    }
                    snd_ctl_close(handle);
                }
            }
            // call calback function for the device
            if (iterator != NULL) {
                doContinue = (*iterator)(ALSA_DEFAULT_DEVICE_ID, rawmidi_info,
                                         defcardinfo, userData);
            }
            count++;
        }
    }

    // iterate cards
    card = -1;
    TRACE0("testing for cards...\n");
    if (snd_card_next(&card) >= 0) {
        TRACE1("Found card %d\n", card);
        while (doContinue && (card >= 0)) {
            sprintf(devname, ALSA_HARDWARE_CARD, card);
            TRACE1("Opening control for alsa rawmidi device \"%s\"...\n", devname);
            err = snd_ctl_open(&handle, devname, SND_CTL_NONBLOCK);
            if (err < 0) {
                ERROR2("ERROR: snd_ctl_open, card=%d: %s\n", card, snd_strerror(err));
            } else {
                TRACE0("snd_ctl_open() SUCCESS\n");
                err = snd_ctl_card_info(handle, card_info);
                if (err < 0) {
                    ERROR2("ERROR: snd_ctl_card_info, card=%d: %s\n", card, snd_strerror(err));
                } else {
                    TRACE0("snd_ctl_card_info() SUCCESS\n");
                    dev = -1;
                    while (doContinue) {
                        if (snd_ctl_rawmidi_next_device(handle, &dev) < 0) {
                            ERROR0("snd_ctl_rawmidi_next_device\n");
                        }
                        TRACE0("snd_ctl_rawmidi_next_device() SUCCESS\n");
                        if (dev < 0) {
                            break;
                        }
                        snd_rawmidi_info_set_device(rawmidi_info, dev);
                        snd_rawmidi_info_set_subdevice(rawmidi_info, 0);
                        snd_rawmidi_info_set_stream(rawmidi_info, direction);
                        err = snd_ctl_rawmidi_info(handle, rawmidi_info);
                        TRACE0("after snd_ctl_rawmidi_info()\n");
                        if (err < 0) {
                            if (err != -ENOENT) {
                                ERROR2("ERROR: snd_ctl_rawmidi_info, card=%d: %s", card, snd_strerror(err));
                            }
                        } else {
                            TRACE0("snd_ctl_rawmidi_info() SUCCESS\n");
                            subdeviceCount = needEnumerateSubdevices(ALSA_RAWMIDI)
                                ? snd_rawmidi_info_get_subdevices_count(rawmidi_info)
                                : 1;
                            if (iterator!=NULL) {
                                for (subDev = 0; subDev < subdeviceCount; subDev++) {
                                    TRACE3("  Iterating %d,%d,%d\n", card, dev, subDev);
                                    deviceID = encodeDeviceID(card, dev, subDev);
                                    doContinue = (*iterator)(deviceID, rawmidi_info,
                                                             card_info, userData);
                                    count++;
                                    TRACE0("returned from iterator\n");
                                    if (!doContinue) {
                                        break;
                                    }
                                }
                            } else {
                                count += subdeviceCount;
                            }
                        }
                    } // of while(doContinue)
                }
                snd_ctl_close(handle);
            }
            if (snd_card_next(&card) < 0) {
                break;
            }
        }
    } else {
        ERROR0("No cards found!\n");
    }
    snd_ctl_card_info_free(card_info);
    snd_rawmidi_info_free(rawmidi_info);
    return count;
}



int getMidiDeviceCount(snd_rawmidi_stream_t direction) {
    int deviceCount;
    TRACE0("> getMidiDeviceCount()\n");
    initAlsaSupport();
    deviceCount = iterateRawmidiDevices(direction, NULL, NULL);
    TRACE0("< getMidiDeviceCount()\n");
    return deviceCount;
}



/*
  userData is assumed to be a pointer to ALSA_MIDIDeviceDescription.
  ALSA_MIDIDeviceDescription->index has to be set to the index of the device
  we want to get information of before this method is called the first time via
  iterateRawmidiDevices(). On each call of this method,
  ALSA_MIDIDeviceDescription->index is decremented. If it is equal to zero,
  we have reached the desired device, so action is taken.
  So after successful completion of iterateRawmidiDevices(),
  ALSA_MIDIDeviceDescription->index is zero. If it isn't, this is an
  indication of an error.
*/
static int deviceInfoIterator(UINT32 deviceID, snd_rawmidi_info_t *rawmidi_info,
                              snd_ctl_card_info_t *cardinfo, void *userData) {
    char buffer[300];
    ALSA_MIDIDeviceDescription* desc = (ALSA_MIDIDeviceDescription*)userData;
#ifdef ALSA_MIDI_USE_PLUGHW
    int usePlugHw = 1;
#else
    int usePlugHw = 0;
#endif

    TRACE0("deviceInfoIterator\n");
    initAlsaSupport();
    if (desc->index == 0) {
        // we found the device with correct index
        desc->deviceID = deviceID;

        buffer[0]=' '; buffer[1]='[';
        // buffer[300] is enough to store the actual device string w/o overrun
        getDeviceStringFromDeviceID(&buffer[2], deviceID, usePlugHw, ALSA_RAWMIDI);
        strncat(buffer, "]", sizeof(buffer) - strlen(buffer) - 1);
        strncpy(desc->name,
                (cardinfo != NULL)
                    ? snd_ctl_card_info_get_id(cardinfo)
                    : snd_rawmidi_info_get_id(rawmidi_info),
                desc->strLen - strlen(buffer));
        strncat(desc->name, buffer, desc->strLen - strlen(desc->name));
        desc->description[0] = 0;
        if (cardinfo != NULL) {
            strncpy(desc->description, snd_ctl_card_info_get_name(cardinfo),
                    desc->strLen);
            strncat(desc->description, ", ",
                    desc->strLen - strlen(desc->description));
        }
        strncat(desc->description, snd_rawmidi_info_get_id(rawmidi_info),
                desc->strLen - strlen(desc->description));
        strncat(desc->description, ", ", desc->strLen - strlen(desc->description));
        strncat(desc->description, snd_rawmidi_info_get_name(rawmidi_info),
                desc->strLen - strlen(desc->description));
        TRACE2("Returning %s, %s\n", desc->name, desc->description);
        return FALSE; // do not continue iteration
    }
    desc->index--;
    return TRUE;
}


static int getMIDIDeviceDescriptionByIndex(snd_rawmidi_stream_t direction,
                                           ALSA_MIDIDeviceDescription* desc) {
    initAlsaSupport();
    TRACE1(" getMIDIDeviceDescriptionByIndex (index = %d)\n", desc->index);
    iterateRawmidiDevices(direction, &deviceInfoIterator, desc);
    return (desc->index == 0) ? MIDI_SUCCESS : MIDI_INVALID_DEVICEID;
}



int initMIDIDeviceDescription(ALSA_MIDIDeviceDescription* desc, int index) {
    int ret = MIDI_SUCCESS;
    desc->index = index;
    desc->strLen = 200;
    desc->name = (char*) calloc(desc->strLen + 1, 1);
    desc->description = (char*) calloc(desc->strLen + 1, 1);
    if (! desc->name ||
        ! desc->description) {
        ret = MIDI_OUT_OF_MEMORY;
    }
    return ret;
}


void freeMIDIDeviceDescription(ALSA_MIDIDeviceDescription* desc) {
    if (desc->name) {
        free(desc->name);
    }
    if (desc->description) {
        free(desc->description);
    }
}


int getMidiDeviceName(snd_rawmidi_stream_t direction, int index, char *name,
                      UINT32 nameLength) {
    ALSA_MIDIDeviceDescription desc;
    int ret;

    TRACE1("getMidiDeviceName: nameLength: %d\n", (int) nameLength);
    ret = initMIDIDeviceDescription(&desc, index);
    if (ret == MIDI_SUCCESS) {
        TRACE0("getMidiDeviceName: initMIDIDeviceDescription() SUCCESS\n");
        ret = getMIDIDeviceDescriptionByIndex(direction, &desc);
        if (ret == MIDI_SUCCESS) {
            TRACE1("getMidiDeviceName: desc.name: %s\n", desc.name);
            strncpy(name, desc.name, nameLength - 1);
            name[nameLength - 1] = 0;
        }
    }
    freeMIDIDeviceDescription(&desc);
    return ret;
}


int getMidiDeviceVendor(int index, char *name, UINT32 nameLength) {
    strncpy(name, ALSA_VENDOR, nameLength - 1);
    name[nameLength - 1] = 0;
    return MIDI_SUCCESS;
}


int getMidiDeviceDescription(snd_rawmidi_stream_t direction,
                             int index, char *name, UINT32 nameLength) {
    ALSA_MIDIDeviceDescription desc;
    int ret;

    ret = initMIDIDeviceDescription(&desc, index);
    if (ret == MIDI_SUCCESS) {
        ret = getMIDIDeviceDescriptionByIndex(direction, &desc);
        if (ret == MIDI_SUCCESS) {
            strncpy(name, desc.description, nameLength - 1);
            name[nameLength - 1] = 0;
        }
    }
    freeMIDIDeviceDescription(&desc);
    return ret;
}


int getMidiDeviceVersion(int index, char *name, UINT32 nameLength) {
    getALSAVersion(name, nameLength);
    return MIDI_SUCCESS;
}


static int getMidiDeviceID(snd_rawmidi_stream_t direction, int index,
                           UINT32* deviceID) {
    ALSA_MIDIDeviceDescription desc;
    int ret;

    ret = initMIDIDeviceDescription(&desc, index);
    if (ret == MIDI_SUCCESS) {
        ret = getMIDIDeviceDescriptionByIndex(direction, &desc);
        if (ret == MIDI_SUCCESS) {
            // TRACE1("getMidiDeviceName: desc.name: %s\n", desc.name);
            *deviceID = desc.deviceID;
        }
    }
    freeMIDIDeviceDescription(&desc);
    return ret;
}


/*
  direction has to be either SND_RAWMIDI_STREAM_INPUT or
  SND_RAWMIDI_STREAM_OUTPUT.
  Returns 0 on success. Otherwise, MIDI_OUT_OF_MEMORY, MIDI_INVALID_ARGUMENT
   or a negative ALSA error code is returned.
*/
INT32 openMidiDevice(snd_rawmidi_stream_t direction, INT32 deviceIndex,
                     MidiDeviceHandle** handle) {
    snd_rawmidi_t* native_handle;
    snd_midi_event_t* event_parser = NULL;
    int err;
    UINT32 deviceID = 0;
    char devicename[100];
#ifdef ALSA_MIDI_USE_PLUGHW
    int usePlugHw = 1;
#else
    int usePlugHw = 0;
#endif

    TRACE0("> openMidiDevice()\n");

    (*handle) = (MidiDeviceHandle*) calloc(sizeof(MidiDeviceHandle), 1);
    if (!(*handle)) {
        ERROR0("ERROR: openDevice: out of memory\n");
        return MIDI_OUT_OF_MEMORY;
    }

    // TODO: iterate to get dev ID from index
    err = getMidiDeviceID(direction, deviceIndex, &deviceID);
    TRACE1("  openMidiDevice(): deviceID: %d\n", (int) deviceID);
    getDeviceStringFromDeviceID(devicename, deviceID,
                                usePlugHw, ALSA_RAWMIDI);
    TRACE1("  openMidiDevice(): deviceString: %s\n", devicename);

    // finally open the device
    if (direction == SND_RAWMIDI_STREAM_INPUT) {
        err = snd_rawmidi_open(&native_handle, NULL, devicename,
                               SND_RAWMIDI_NONBLOCK);
    } else if (direction == SND_RAWMIDI_STREAM_OUTPUT) {
        err = snd_rawmidi_open(NULL, &native_handle, devicename,
                               SND_RAWMIDI_NONBLOCK);
    } else {
        ERROR0("  ERROR: openMidiDevice(): direction is neither SND_RAWMIDI_STREAM_INPUT nor SND_RAWMIDI_STREAM_OUTPUT\n");
        err = MIDI_INVALID_ARGUMENT;
    }
    if (err < 0) {
        ERROR1("<  ERROR: openMidiDevice(): snd_rawmidi_open() returned %d\n", err);
        free(*handle);
        (*handle) = NULL;
        return err;
    }
    /* We opened with non-blocking behaviour to not get hung if the device
       is used by a different process. Writing, however, should
       be blocking. So we change it here. */
    if (direction == SND_RAWMIDI_STREAM_OUTPUT) {
        err = snd_rawmidi_nonblock(native_handle, 0);
        if (err < 0) {
            ERROR1("  ERROR: openMidiDevice(): snd_rawmidi_nonblock() returned %d\n", err);
            snd_rawmidi_close(native_handle);
            free(*handle);
            (*handle) = NULL;
            return err;
        }
    }
    if (direction == SND_RAWMIDI_STREAM_INPUT) {
        err = snd_midi_event_new(EVENT_PARSER_BUFSIZE, &event_parser);
        if (err < 0) {
            ERROR1("  ERROR: openMidiDevice(): snd_midi_event_new() returned %d\n", err);
            snd_rawmidi_close(native_handle);
            free(*handle);
            (*handle) = NULL;
            return err;
        }
    }

    (*handle)->deviceHandle = (void*) native_handle;
    (*handle)->startTime = getTimeInMicroseconds();
    (*handle)->platformData = event_parser;
    TRACE0("< openMidiDevice(): succeeded\n");
    return err;
}



INT32 closeMidiDevice(MidiDeviceHandle* handle) {
    int err;

    TRACE0("> closeMidiDevice()\n");
    if (!handle) {
        ERROR0("< ERROR: closeMidiDevice(): handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    if (!handle->deviceHandle) {
        ERROR0("< ERROR: closeMidiDevice(): native handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    err = snd_rawmidi_close((snd_rawmidi_t*) handle->deviceHandle);
    TRACE1("  snd_rawmidi_close() returns %d\n", err);
    if (handle->platformData) {
        snd_midi_event_free((snd_midi_event_t*) handle->platformData);
    }
    free(handle);
    TRACE0("< closeMidiDevice: succeeded\n");
    return err;
}


INT64 getMidiTimestamp(MidiDeviceHandle* handle) {
    if (!handle) {
        ERROR0("< ERROR: closeMidiDevice(): handle is NULL\n");
        return MIDI_INVALID_HANDLE;
    }
    return getTimeInMicroseconds() - handle->startTime;
}


/* end */
