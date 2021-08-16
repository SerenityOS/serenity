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

//#define USE_ERROR
//#define USE_TRACE

#include "PLATFORM_API_LinuxOS_ALSA_PCMUtils.h"
#include "PLATFORM_API_LinuxOS_ALSA_CommonUtils.h"



// callback for iteration through devices
// returns TRUE if iteration should continue
// NOTE: cardinfo may be NULL (for "default" device)
typedef int (*DeviceIteratorPtr)(UINT32 deviceID, snd_pcm_info_t* pcminfo,
                             snd_ctl_card_info_t* cardinfo, void *userData);

// for each ALSA device, call iterator. userData is passed to the iterator
// returns total number of iterations
int iteratePCMDevices(DeviceIteratorPtr iterator, void* userData) {
    int count = 0;
    int subdeviceCount;
    int card, dev, subDev;
    char devname[16];
    int err;
    snd_ctl_t *handle;
    snd_pcm_t *pcm;
    snd_pcm_info_t* pcminfo;
    snd_ctl_card_info_t *cardinfo, *defcardinfo = NULL;
    UINT32 deviceID;
    int doContinue = TRUE;

    snd_pcm_info_malloc(&pcminfo);
    snd_ctl_card_info_malloc(&cardinfo);

    // 1st try "default" device
    err = snd_pcm_open(&pcm, ALSA_DEFAULT_DEVICE_NAME,
                       SND_PCM_STREAM_PLAYBACK, SND_PCM_NONBLOCK);
    if (err < 0) {
        // try with the other direction
        err = snd_pcm_open(&pcm, ALSA_DEFAULT_DEVICE_NAME,
                           SND_PCM_STREAM_CAPTURE, SND_PCM_NONBLOCK);
    }
    if (err < 0) {
        ERROR1("ERROR: snd_pcm_open (\"default\"): %s\n", snd_strerror(err));
    } else {
        err = snd_pcm_info(pcm, pcminfo);
        snd_pcm_close(pcm);
        if (err < 0) {
            ERROR1("ERROR: snd_pcm_info (\"default\"): %s\n",
                    snd_strerror(err));
        } else {
            // try to get card info
            card = snd_pcm_info_get_card(pcminfo);
            if (card >= 0) {
                sprintf(devname, ALSA_HARDWARE_CARD, card);
                if (snd_ctl_open(&handle, devname, SND_CTL_NONBLOCK) >= 0) {
                    if (snd_ctl_card_info(handle, cardinfo) >= 0) {
                        defcardinfo = cardinfo;
                    }
                    snd_ctl_close(handle);
                }
            }
            // call callback function for the device
            if (iterator != NULL) {
                doContinue = (*iterator)(ALSA_DEFAULT_DEVICE_ID, pcminfo,
                                         defcardinfo, userData);
            }
            count++;
        }
    }

    // iterate cards
    card = -1;
    while (doContinue) {
        if (snd_card_next(&card) < 0) {
            break;
        }
        if (card < 0) {
            break;
        }
        sprintf(devname, ALSA_HARDWARE_CARD, card);
        TRACE1("Opening alsa device \"%s\"...\n", devname);
        err = snd_ctl_open(&handle, devname, SND_CTL_NONBLOCK);
        if (err < 0) {
            ERROR2("ERROR: snd_ctl_open, card=%d: %s\n",
                    card, snd_strerror(err));
        } else {
            err = snd_ctl_card_info(handle, cardinfo);
            if (err < 0) {
                ERROR2("ERROR: snd_ctl_card_info, card=%d: %s\n",
                        card, snd_strerror(err));
            } else {
                dev = -1;
                while (doContinue) {
                    if (snd_ctl_pcm_next_device(handle, &dev) < 0) {
                        ERROR0("snd_ctl_pcm_next_device\n");
                    }
                    if (dev < 0) {
                        break;
                    }
                    snd_pcm_info_set_device(pcminfo, dev);
                    snd_pcm_info_set_subdevice(pcminfo, 0);
                    snd_pcm_info_set_stream(pcminfo, SND_PCM_STREAM_PLAYBACK);
                    err = snd_ctl_pcm_info(handle, pcminfo);
                    if (err == -ENOENT) {
                        // try with the other direction
                        snd_pcm_info_set_stream(pcminfo, SND_PCM_STREAM_CAPTURE);
                        err = snd_ctl_pcm_info(handle, pcminfo);
                    }
                    if (err < 0) {
                        if (err != -ENOENT) {
                            ERROR2("ERROR: snd_ctl_pcm_info, card=%d: %s",
                                    card, snd_strerror(err));
                        }
                    } else {
                        subdeviceCount = needEnumerateSubdevices(ALSA_PCM) ?
                            snd_pcm_info_get_subdevices_count(pcminfo) : 1;
                        if (iterator!=NULL) {
                            for (subDev = 0; subDev < subdeviceCount; subDev++) {
                                deviceID = encodeDeviceID(card, dev, subDev);
                                doContinue = (*iterator)(deviceID, pcminfo,
                                                         cardinfo, userData);
                                count++;
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
    }
    snd_ctl_card_info_free(cardinfo);
    snd_pcm_info_free(pcminfo);
    return count;
}

int getAudioDeviceCount() {
    initAlsaSupport();
    return iteratePCMDevices(NULL, NULL);
}

int deviceInfoIterator(UINT32 deviceID, snd_pcm_info_t* pcminfo,
                       snd_ctl_card_info_t* cardinfo, void* userData) {
    char buffer[300];
    ALSA_AudioDeviceDescription* desc = (ALSA_AudioDeviceDescription*)userData;
#ifdef ALSA_PCM_USE_PLUGHW
    int usePlugHw = 1;
#else
    int usePlugHw = 0;
#endif

    initAlsaSupport();
    if (desc->index == 0) {
        // we found the device with correct index
        *(desc->maxSimultaneousLines) = needEnumerateSubdevices(ALSA_PCM) ?
                1 : snd_pcm_info_get_subdevices_count(pcminfo);
        *desc->deviceID = deviceID;
        buffer[0]=' '; buffer[1]='[';
        // buffer[300] is enough to store the actual device string w/o overrun
        getDeviceStringFromDeviceID(&buffer[2], deviceID, usePlugHw, ALSA_PCM);
        strncat(buffer, "]", sizeof(buffer) - strlen(buffer) - 1);
        strncpy(desc->name,
                (cardinfo != NULL)
                    ? snd_ctl_card_info_get_id(cardinfo)
                    : snd_pcm_info_get_id(pcminfo),
                desc->strLen - strlen(buffer));
        strncat(desc->name, buffer, desc->strLen - strlen(desc->name));
        strncpy(desc->vendor, "ALSA (http://www.alsa-project.org)", desc->strLen);
        strncpy(desc->description,
                (cardinfo != NULL)
                    ? snd_ctl_card_info_get_name(cardinfo)
                    : snd_pcm_info_get_name(pcminfo),
                desc->strLen);
        strncat(desc->description, ", ", desc->strLen - strlen(desc->description));
        strncat(desc->description, snd_pcm_info_get_id(pcminfo), desc->strLen - strlen(desc->description));
        strncat(desc->description, ", ", desc->strLen - strlen(desc->description));
        strncat(desc->description, snd_pcm_info_get_name(pcminfo), desc->strLen - strlen(desc->description));
        getALSAVersion(desc->version, desc->strLen);
        TRACE4("Returning %s, %s, %s, %s\n", desc->name, desc->vendor, desc->description, desc->version);
        return FALSE; // do not continue iteration
    }
    desc->index--;
    return TRUE;
}

// returns 0 if successful
int openPCMfromDeviceID(int deviceID, snd_pcm_t** handle, int isSource, int hardware) {
    char buffer[200];
    int ret;

    initAlsaSupport();
    getDeviceStringFromDeviceID(buffer, deviceID, !hardware, ALSA_PCM);

    TRACE1("Opening ALSA device %s\n", buffer);
    ret = snd_pcm_open(handle, buffer,
                       isSource?SND_PCM_STREAM_PLAYBACK:SND_PCM_STREAM_CAPTURE,
                       SND_PCM_NONBLOCK);
    if (ret != 0) {
        ERROR1("snd_pcm_open returned error code %d \n", ret);
        *handle = NULL;
    }
    return ret;
}


int getAudioDeviceDescriptionByIndex(ALSA_AudioDeviceDescription* desc) {
    initAlsaSupport();
    TRACE1(" getAudioDeviceDescriptionByIndex(mixerIndex = %d\n", desc->index);
    iteratePCMDevices(&deviceInfoIterator, desc);
    return (desc->index == 0)?TRUE:FALSE;
}

// returns 1 if successful
// enc: 0 for PCM, 1 for ULAW, 2 for ALAW (see DirectAudio.h)
int getFormatFromAlsaFormat(snd_pcm_format_t alsaFormat,
                            int* sampleSizeInBytes, int* significantBits,
                            int* isSigned, int* isBigEndian, int* enc) {

    *sampleSizeInBytes = (snd_pcm_format_physical_width(alsaFormat) + 7) / 8;
    *significantBits = snd_pcm_format_width(alsaFormat);

    // defaults
    *enc = 0; // PCM
    *isSigned = (snd_pcm_format_signed(alsaFormat) > 0);
    *isBigEndian = (snd_pcm_format_big_endian(alsaFormat) > 0);

    // non-PCM formats
    if (alsaFormat == SND_PCM_FORMAT_MU_LAW) { // Mu-Law
        *sampleSizeInBytes = 8; *enc = 1; *significantBits = *sampleSizeInBytes;
    }
    else if (alsaFormat == SND_PCM_FORMAT_A_LAW) {     // A-Law
        *sampleSizeInBytes = 8; *enc = 2; *significantBits = *sampleSizeInBytes;
    }
    else if (snd_pcm_format_linear(alsaFormat) < 1) {
        return 0;
    }
    return (*sampleSizeInBytes > 0);
}

// returns 1 if successful
int getAlsaFormatFromFormat(snd_pcm_format_t* alsaFormat,
                            int sampleSizeInBytes, int significantBits,
                            int isSigned, int isBigEndian, int enc) {
    *alsaFormat = SND_PCM_FORMAT_UNKNOWN;

    if (enc == 0) {
        *alsaFormat = snd_pcm_build_linear_format(significantBits,
                                                  sampleSizeInBytes * 8,
                                                  isSigned?0:1,
                                                  isBigEndian?1:0);
    }
    else if ((sampleSizeInBytes == 1) && (significantBits == 8)) {
        if (enc == 1) { // ULAW
            *alsaFormat = SND_PCM_FORMAT_MU_LAW;
        }
        else if (enc == 2) { // ALAW
            *alsaFormat = SND_PCM_FORMAT_A_LAW;
        }
    }
    return (*alsaFormat == SND_PCM_FORMAT_UNKNOWN)?0:1;
}


/* end */
