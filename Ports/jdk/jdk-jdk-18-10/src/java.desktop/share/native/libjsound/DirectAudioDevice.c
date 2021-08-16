/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

/* TODO:
 * - move all the conversion code into an own file
 */

//#define USE_TRACE
//#define USE_ERROR


#include <jni.h>
#include <jni_util.h>
// for malloc
#ifdef _ALLBSD_SOURCE
#include <stdlib.h>
#else
#include <malloc.h>
#endif
#include "SoundDefs.h"
#include "DirectAudio.h"
#include "Utilities.h"
#include "com_sun_media_sound_DirectAudioDevice.h"


typedef struct {
    void* handle;
    int encoding;
    int sampleSizeInBits;
    int frameSize;
    int channels;
    int isSigned;
    int isBigEndian;
    UINT8* conversionBuffer;
    int conversionBufferSize;
} DAUDIO_Info;


//////////////////////////////////////////// MAP Conversion stuff /////////////////////////////////

/* 16 bit signed sample, native endianness, stored in 32-bits */
typedef INT32 MAP_Sample;

static INLINE UINT16 MAP_SWAP16_impl(UINT16 a) {
    return (a>>8) | (a<<8);
}

static INLINE UINT32 MAP_SWAP32_impl(UINT32 a) {
    return (a>>24)
        | ((a>>8) & 0xFF00)
        | ((a<<8) & 0xFF0000)
        | (a<<24);
}

static INLINE UINT32 MAP_SWAP16BIT(UINT32 sh) {
    return (UINT32) ((sh & 0xFF) << 8) | ((sh & 0xFF00) >> 8);
}

static INLINE INT32 MAP_ClipAndConvertToShort(MAP_Sample sample) {
    if (sample < -32768) {
        return -32768;
    }
    else if (sample > 32767) {
        return 32767;
    }
    return (INT32) sample;
}


static INLINE INT32 MAP_ClipAndConvertToShort_Swapped(MAP_Sample sample) {
    if (sample < -32768) {
        return 0x0080;
    }
    else if (sample > 32767) {
        return 0xFF7F;
    }
    return (INT32) (INT16) MAP_SWAP16BIT(sample);
}

static INLINE INT8 MAP_ClipAndConvertToByte(MAP_Sample sample) {
    if (sample < -32768) {
        return -128;
    }
    else if (sample > 32767) {
        return 127;
    }
    return (INT8) (sample >> 8);
}


static INLINE UINT8 MAP_ClipAndConvertToUByte(MAP_Sample sample) {
    if (sample < -32768) {
        return 0;
    }
    else if (sample > 32767) {
        return 255;
    }
    return (UINT8) ((sample >> 8) + 128);
}

/* conversion from/to 16 bit signed little endian to native endian samples */
#ifdef _LITTLE_ENDIAN
#define MAP_LE_SHORT2SAMPLE(sh) ((MAP_Sample) (sh))
#define MAP_SAMPLE2LE_SHORT(sample) (sample)
#define MAP_SAMPLE2LE_SHORT_CLIP(sample) MAP_ClipAndConvertToShort(sample)
#else
#define MAP_LE_SHORT2SAMPLE(sh) ((MAP_Sample) (INT16) MAP_SWAP16BIT(sh))
#define MAP_SAMPLE2LE_SHORT(sample) (INT16) MAP_SWAP16BIT(sample)
#define MAP_SAMPLE2LE_SHORT_CLIP(sample) MAP_ClipAndConvertToShort_Swapped(sample)
#endif

/* conversion from/to 16 bit signed big endian to native endian samples */
#ifndef _LITTLE_ENDIAN
#define MAP_BE_SHORT2SAMPLE(sh) ((MAP_Sample) (sh))
#define MAP_SAMPLE2BE_SHORT(sample) (sample)
#define MAP_SAMPLE2BE_SHORT_CLIP(sample) MAP_ClipAndConvertToShort(sample)
#else
#define MAP_BE_SHORT2SAMPLE(sh) ((MAP_Sample) (INT16) MAP_SWAP16BIT(sh))
#define MAP_SAMPLE2BE_SHORT(sample) ((INT16) MAP_SWAP16BIT(sample))
#define MAP_SAMPLE2BE_SHORT_CLIP(sample) MAP_ClipAndConvertToShort_Swapped(sample)
#endif

/* conversion from/to 8 bit samples */
#define MAP_INT82SAMPLE(by) ((MAP_Sample) (((INT32) ((INT8) (by))) << 8))
#define MAP_UINT82SAMPLE(by) ((MAP_Sample) (((INT32) ((UINT8) (by) - 128)) << 8))
#define MAP_SAMPLE2UINT8(sample) ((UINT8) ((((MAP_Sample) (sample)) >> 8) + 128))
#define MAP_SAMPLE2INT8(sample) ((INT8) (((MAP_Sample) (sample)) >> 8))
#define MAP_SAMPLE2UINT8_CLIP(sample) MAP_ClipAndConvertToUByte(sample)
#define MAP_SAMPLE2INT8_CLIP(sample) MAP_ClipAndConvertToByte(sample)

/* macros for endian conversion */
#ifdef _LITTLE_ENDIAN
#define MAP_NATIVE2LE16(a) (a)
#define MAP_NATIVE2BE16(a) MAP_SWAP16_impl(a)
#define MAP_NATIVE2LE32(a) (a)
#define MAP_NATIVE2BE32(a) MAP_SWAP32_impl(a)
#else
#define MAP_NATIVE2LE16(a) MAP_SWAP16_impl(a)
#define MAP_NATIVE2BE16(a) (a)
#define MAP_NATIVE2LE32(a) MAP_SWAP32_impl(a)
#define MAP_NATIVE2BE32(a) (a)
#endif
#define MAP_LE2NATIVE16(a) MAP_NATIVE2LE16(a)
#define MAP_BE2NATIVE16(a) MAP_NATIVE2BE16(a)
#define MAP_LE2NATIVE32(a) MAP_NATIVE2LE32(a)
#define MAP_BE2NATIVE32(a) MAP_NATIVE2BE32(a)


////////////////////////////// Utility function /////////////////////////////////

/*
 * conversion of this buffer:
 * conversion size=1 -> each byte is converted from signed to unsigned or vice versa
 * conversion size=2,3,4: the order of bytes in a sample is reversed (endianness)
 * for sign conversion of a 24-bit sample stored in 32bits, 4 should be passed
 * as conversionSize
 */
void handleSignEndianConversion(INT8* data, INT8* output, int byteSize, int conversionSize) {
    TRACE1("conversion with size %d\n", conversionSize);
    switch (conversionSize) {
    case 1: {
        while (byteSize > 0) {
            *output = *data + (char) 128; // use wrap-around
            byteSize--;
            data++;
            output++;
        }
        break;
    }
    case 2: {
        INT8 h;
        byteSize = byteSize / 2;
        while (byteSize > 0) {
            h = *data;
            data++;
            *output = *data;
            output++;
            *output = h;
            byteSize--;
            data++; output++;
        }
        break;
    }
    case 3: {
        INT8 h;
        byteSize = byteSize / 3;
        while (byteSize > 0) {
            h = *data;
            *output = data[2];
            data++; output++;
            *output = *data;
            data++; output++;
            *output = h;
            data++; output++;
            byteSize--;
        }
        break;
    }
    case 4: {
        INT8 h1, h2;
        byteSize = byteSize / 4;
        while (byteSize > 0) {
            h1 = data[0];
            h2 = data[1];
            *output = data[3]; output++;
            *output = data[2]; output++;
            *output = h2; output++;
            *output = h1; output++;
            data += 4;
            byteSize--;
        }
        break;
    }
    default:
        ERROR1("DirectAudioDevice.c: wrong conversionSize %d!\n", conversionSize);
    }
}

/* aply the gain to one sample */
#define CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE, FROM_SAMPLE, FACTOR) \
    /* convert to MAP_Sample native type */     \
    sample = TO_SAMPLE(*INPUT);                 \
    /* apply gain */                            \
    sample = (MAP_Sample) (sample * FACTOR);    \
    /* convert to output type */                \
    (*OUTPUT) = FROM_SAMPLE(sample);            \
    INPUT++; OUTPUT++


/* macro for conversion of a mono block */
#define LOOP_M(INPUT, OUTPUT, TO_SAMPLE, FROM_SAMPLE, FROM_SAMPLE_CLIP) \
    if (leftGain > 1.0) {                                               \
        for ( ; len > 0; --len) {                                       \
            CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                    \
                           FROM_SAMPLE_CLIP, leftGain);                 \
        }                                                               \
    } else {                                                            \
        for ( ; len > 0; --len) {                                       \
            CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                    \
                           FROM_SAMPLE, leftGain);                      \
        }                                                               \
    }                                                                   \
    break

/* macro for conversion of a stereo block */
#define LOOP_S(INPUT, OUTPUT, TO_SAMPLE, FROM_SAMPLE, FROM_SAMPLE_CLIP) \
    if (leftGain > 1.0) {                                               \
        if (rightGain > 1.0) {                                          \
            for ( ; len > 0; --len) {                                   \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE_CLIP, leftGain);             \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE_CLIP, rightGain);            \
            }                                                           \
        } else {                                                        \
            for ( ; len > 0; --len) {                                   \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE_CLIP, leftGain);             \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE, rightGain);                 \
            }                                                           \
        }                                                               \
    } else {                                                            \
        if (rightGain > 1.0) {                                          \
            for ( ; len > 0; --len) {                                   \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE, leftGain);                  \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE_CLIP, rightGain);            \
            }                                                           \
        } else {                                                        \
            for ( ; len > 0; --len) {                                   \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE, leftGain);                  \
                CONVERT_SAMPLE(INPUT, OUTPUT, TO_SAMPLE,                \
                               FROM_SAMPLE, rightGain);                 \
            }                                                           \
        }                                                               \
    }                                                                   \
    break

#define FORMAT2CODE(channels, bits, inSigned, outSigned, inBigEndian, outBigEndian) \
      (channels << 20)                                                  \
    | (bits << 4)                                                       \
    | ((inSigned & 1) << 3)                                             \
    | ((outSigned & 1) << 2)                                            \
    | ((inBigEndian & 1) << 1)                                          \
    | (outBigEndian & 1)

#define FORMAT2CODE8(channels, inSigned, outSigned)           \
    FORMAT2CODE(channels, 8, inSigned, outSigned, 0, 0)

#define FORMAT2CODE16(channels, inBigEndian, outBigEndian)    \
    FORMAT2CODE(channels, 16, 1, 1, inBigEndian, outBigEndian)


void handleGainAndConversion(DAUDIO_Info* info, UINT8* input, UINT8* output,
                             int len, float leftGain, float rightGain,
                             int conversionSize) {
    INT8* input8 = (INT8*) input;
    INT8* output8 = (INT8*) output;
    INT16* input16 = (INT16*) input;
    INT16* output16 = (INT16*) output;
    MAP_Sample sample;

    int inIsSigned = info->isSigned;
    int inIsBigEndian = info->isBigEndian;
    if (conversionSize == 1) {
        /* 8-bit conversion: change sign */
        inIsSigned = !inIsSigned;
    }
    else if (conversionSize > 1) {
        /* > 8-bit conversion: change endianness */
        inIsBigEndian = !inIsBigEndian;
    }
    if (info->frameSize <= 0) {
        ERROR1("DirectAudiODevice: invalid framesize=%d\n", info->frameSize);
        return;
    }
    len /= info->frameSize;
    TRACE3("handleGainAndConversion: len=%d frames, leftGain=%f, rightGain=%f, ",
           len, leftGain, rightGain);
    TRACE3("channels=%d, sampleSizeInBits=%d, frameSize=%d, ",
           (int) info->channels, (int) info->sampleSizeInBits, (int) info->frameSize);
    TRACE4("signed:%d -> %d, endian: %d -> %d",
           (int) inIsSigned, (int) info->isSigned,
           (int) inIsBigEndian, (int) info->isBigEndian);
    TRACE1("convSize=%d\n", conversionSize);

    switch (FORMAT2CODE(info->channels,
                        info->sampleSizeInBits,
                        inIsSigned,
                        info->isSigned,
                        inIsBigEndian,
                        info->isBigEndian)) {
        /* 8-bit mono */
    case FORMAT2CODE8(1, 0, 0):
        LOOP_M(input8, output8, MAP_UINT82SAMPLE,
               MAP_SAMPLE2UINT8, MAP_SAMPLE2UINT8_CLIP);
    case FORMAT2CODE8(1, 0, 1):
        LOOP_M(input8, output8, MAP_UINT82SAMPLE,
               MAP_SAMPLE2INT8, MAP_SAMPLE2INT8_CLIP);
    case FORMAT2CODE8(1, 1, 0):
        LOOP_M(input8, output8, MAP_INT82SAMPLE,
               MAP_SAMPLE2UINT8, MAP_SAMPLE2UINT8_CLIP);
    case FORMAT2CODE8(1, 1, 1):
        LOOP_M(input8, output8, MAP_INT82SAMPLE,
               MAP_SAMPLE2INT8, MAP_SAMPLE2INT8_CLIP);

    /* 8-bit stereo */
    case FORMAT2CODE8(2, 0, 0):
        LOOP_S(input8, output8, MAP_UINT82SAMPLE,
               MAP_SAMPLE2UINT8, MAP_SAMPLE2UINT8_CLIP);
    case FORMAT2CODE8(2, 0, 1):
        LOOP_S(input8, output8, MAP_UINT82SAMPLE,
               MAP_SAMPLE2INT8, MAP_SAMPLE2INT8_CLIP);
    case FORMAT2CODE8(2, 1, 0):
        LOOP_S(input8, output8, MAP_INT82SAMPLE,
               MAP_SAMPLE2UINT8, MAP_SAMPLE2UINT8_CLIP);
    case FORMAT2CODE8(2, 1, 1):
        LOOP_S(input8, output8, MAP_INT82SAMPLE,
               MAP_SAMPLE2INT8, MAP_SAMPLE2INT8_CLIP);

    /* 16-bit mono (only signed is accepted) */
    case FORMAT2CODE16(1, 0, 0):
        LOOP_M(input16, output16, MAP_LE_SHORT2SAMPLE,
               MAP_SAMPLE2LE_SHORT, MAP_SAMPLE2LE_SHORT_CLIP);
    case FORMAT2CODE16(1, 0, 1):
        LOOP_M(input16, output16, MAP_LE_SHORT2SAMPLE,
               MAP_SAMPLE2BE_SHORT, MAP_SAMPLE2BE_SHORT_CLIP);
    case FORMAT2CODE16(1, 1, 0):
        LOOP_M(input16, output16, MAP_BE_SHORT2SAMPLE,
               MAP_SAMPLE2LE_SHORT, MAP_SAMPLE2LE_SHORT_CLIP);
    case FORMAT2CODE16(1, 1, 1):
        LOOP_M(input16, output16, MAP_BE_SHORT2SAMPLE,
               MAP_SAMPLE2BE_SHORT, MAP_SAMPLE2BE_SHORT_CLIP);

    /* 16-bit stereo (only signed is accepted) */
    case FORMAT2CODE16(2, 0, 0):
        LOOP_S(input16, output16, MAP_LE_SHORT2SAMPLE,
               MAP_SAMPLE2LE_SHORT, MAP_SAMPLE2LE_SHORT_CLIP);
    case FORMAT2CODE16(2, 0, 1):
        LOOP_S(input16, output16, MAP_LE_SHORT2SAMPLE,
               MAP_SAMPLE2BE_SHORT, MAP_SAMPLE2BE_SHORT_CLIP);
    case FORMAT2CODE16(2, 1, 0):
        LOOP_S(input16, output16, MAP_BE_SHORT2SAMPLE,
               MAP_SAMPLE2LE_SHORT, MAP_SAMPLE2LE_SHORT_CLIP);
    case FORMAT2CODE16(2, 1, 1):
        LOOP_S(input16, output16, MAP_BE_SHORT2SAMPLE,
               MAP_SAMPLE2BE_SHORT, MAP_SAMPLE2BE_SHORT_CLIP);

    default:
        ERROR3("DirectAudioDevice: Cannot convert from native format: "
               "bits=%d, inSigned=%d  outSigned=%d, ",
               (int) info->sampleSizeInBits,
               (int) inIsSigned, (int) info->isSigned);
        ERROR2("inBigEndian=%d, outBigEndian=%d\n",
               (int) inIsBigEndian, (int) info->isBigEndian);
    }
}

float ABS_VALUE(float a) {
    return (a < 0)?-a:a;
}


//////////////////////////////////////////// DirectAudioDevice ////////////////////////////////////////////

/* ************************************** native control creation support ********************* */

// contains all the needed references so that the platform dependent code can call JNI wrapper functions
typedef struct tag_AddFormatCreator {
    // general JNI variables
    JNIEnv *env;
    // the vector to be filled with the formats
    jobject vector;
    // the class containing the addFormat method
    jclass directAudioDeviceClass;
    // the method to be called to add the format
    jmethodID addFormat; // signature (Ljava/util/Vector;IIFIBB)V
} AddFormatCreator;

void DAUDIO_AddAudioFormat(void* creatorV, int significantBits, int frameSizeInBytes,
                           int channels, float sampleRate,
                           int encoding, int isSigned,
                           int bigEndian) {
    AddFormatCreator* creator = (AddFormatCreator*) creatorV;
    if (frameSizeInBytes <= 0) {
        if (channels > 0) {
            frameSizeInBytes = ((significantBits + 7) / 8) * channels;
        } else {
            frameSizeInBytes = -1;
        }
    }
    TRACE4("AddAudioFormat with sigBits=%d bits, frameSize=%d bytes, channels=%d, sampleRate=%d ",
           significantBits, frameSizeInBytes, channels, (int) sampleRate);
    TRACE3("enc=%d, signed=%d, bigEndian=%d\n", encoding, isSigned, bigEndian);
    (*creator->env)->CallStaticVoidMethod(creator->env, creator->directAudioDeviceClass,
                                          creator->addFormat, creator->vector, significantBits, frameSizeInBytes,
                                          channels, sampleRate, encoding, isSigned, bigEndian);
}

////////////////////////////////////// JNI /////////////////////////////////////////////////////////////////////

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nGetFormats
 * Signature: (IIZLjava/util/Vector;)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nGetFormats
(JNIEnv *env, jclass clazz, jint mixerIndex, jint deviceID, jboolean isSource, jobject formats) {

#if USE_DAUDIO == TRUE
    AddFormatCreator creator;
    creator.env = env;
    creator.vector = formats;
    creator.directAudioDeviceClass = clazz;
    creator.addFormat = (*env)->GetStaticMethodID(env, clazz, "addFormat",
                                                  "(Ljava/util/Vector;IIIFIZZ)V");
    if (creator.addFormat == NULL) {
        ERROR0("Could not get method ID for addFormat!\n");
    } else {
        DAUDIO_GetFormats((INT32) mixerIndex, (INT32) deviceID, (int) isSource, &creator);
    }
#endif
}



/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nOpen
 * Signature: (IIZIFIIZZI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_sound_DirectAudioDevice_nOpen
(JNIEnv* env, jclass clazz, jint mixerIndex, jint deviceID, jboolean isSource,
 jint encoding, jfloat sampleRate, jint sampleSizeInBits, jint frameSize, jint channels,
 jboolean isSigned, jboolean isBigendian, jint bufferSizeInBytes) {

    DAUDIO_Info* info = NULL;
#if USE_DAUDIO == TRUE

    info = (DAUDIO_Info*) malloc(sizeof(DAUDIO_Info));
    if (info == NULL) {
        ERROR0("DirectAudioDevice_nOpen: Out of memory!\n");
    } else {
        info->handle =DAUDIO_Open((int) mixerIndex, (INT32) deviceID, (int) isSource,
                                  (int) encoding, (float) sampleRate, (int) sampleSizeInBits,
                                  (int) frameSize, (int) channels,
                                  (int) isSigned, (int) isBigendian, (int) bufferSizeInBytes);
        if (!info->handle) {
            free(info);
            info = NULL;
        } else {
            info->encoding = encoding;
            info->sampleSizeInBits = sampleSizeInBits;
            info->frameSize = frameSize;
            info->channels = channels;
            info->isSigned = isSigned;
            info->isBigEndian = isBigendian && (sampleSizeInBits > 8);
            /* will be populated on demand */
            info->conversionBuffer = NULL;
            info->conversionBufferSize = 0;
        }
    }
#endif
    return (jlong) (UINT_PTR) info;
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nStart
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nStart
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_Start(info->handle, (int) isSource);
    }
#endif
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nStop
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nStop
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_Stop(info->handle, (int) isSource);
    }
#endif
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nClose
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nClose
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_Close(info->handle, (int) isSource);
        if (info->conversionBuffer) {
            free(info->conversionBuffer);
        }
        free(info);
    }
#endif
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nWrite
 * Signature: (J[BII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_sound_DirectAudioDevice_nWrite
(JNIEnv *env, jclass clazz, jlong id, jbyteArray jData,
 jint offset, jint len, jint conversionSize, jfloat leftGain, jfloat rightGain) {
    int ret = -1;
#if USE_DAUDIO == TRUE
    UINT8* data;
    UINT8* dataOffset;
    UINT8* convertedData;
    jboolean didCopy;
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;

    /* a little sanity */
    if (offset < 0 || len < 0) {
        ERROR2("nWrite: wrong parameters: offset=%d, len=%d\n", offset, len);
        return ret;
    }
    if (len == 0) return 0;
    if (info && info->handle) {
        data = (UINT8*) ((*env)->GetByteArrayElements(env, jData, &didCopy));
        CHECK_NULL_RETURN(data, ret);
        dataOffset = data;
        dataOffset += (int) offset;
        convertedData = dataOffset;

        if (conversionSize > 0 || leftGain != 1.0f || rightGain != 1.0f) {
            /* make sure we have a buffer for the intermediate data */
            if (didCopy == JNI_FALSE) {
                /* let's do our own copy */
                if (info->conversionBuffer
                    && info->conversionBufferSize < len) {
                    free(info->conversionBuffer);
                    info->conversionBuffer = NULL;
                    info->conversionBufferSize = 0;
                }
                if (!info->conversionBuffer) {
                    info->conversionBuffer = (UINT8*) malloc(len);
                    if (!info->conversionBuffer) {
                        // do not commit the native array
                        (*env)->ReleaseByteArrayElements(env, jData, (jbyte*) data, JNI_ABORT);
                        return -1;
                    }
                    info->conversionBufferSize = len;
                }
                convertedData = info->conversionBuffer;
            }
            if (((ABS_VALUE(leftGain - 1.0f) < 0.01)
                 && (ABS_VALUE(rightGain - 1.0f) < 0.01))
                || info->encoding!=DAUDIO_PCM
                || ((info->channels * info->sampleSizeInBits / 8) != info->frameSize)
                || (info->sampleSizeInBits != 8 && info->sampleSizeInBits != 16)) {
                handleSignEndianConversion((INT8*) dataOffset, (INT8*) convertedData, (int) len,
                                           (int) conversionSize);
            } else {
                handleGainAndConversion(info, dataOffset, convertedData,
                                        (int) len, (float) leftGain, (float) rightGain,
                                        (int) conversionSize);
            }
        }

        ret = DAUDIO_Write(info->handle, (INT8*) convertedData, (int) len);

        // do not commit the native array
        (*env)->ReleaseByteArrayElements(env, jData, (jbyte*) data, JNI_ABORT);
    }
#endif
    return (jint) ret;
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nRead
 * Signature: (J[BII)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_sound_DirectAudioDevice_nRead
(JNIEnv* env, jclass clazz, jlong id, jbyteArray jData, jint offset, jint len, jint conversionSize) {
    int ret = -1;
#if USE_DAUDIO == TRUE
    char* data;
    char* dataOffset;
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;

    /* a little sanity */
    if (offset < 0 || len < 0) {
        ERROR2("nRead: wrong parameters: offset=%d, len=%d\n", offset, len);
        return ret;
    }
    if (info && info->handle) {
        data = (char*) ((*env)->GetByteArrayElements(env, jData, NULL));
        CHECK_NULL_RETURN(data, ret);
        dataOffset = data;
        dataOffset += (int) offset;
        ret = DAUDIO_Read(info->handle, dataOffset, (int) len);
        if (conversionSize > 0) {
            handleSignEndianConversion(dataOffset, dataOffset, (int) len, (int) conversionSize);
        }
        // commit the native array
        (*env)->ReleaseByteArrayElements(env, jData, (jbyte*) data, 0);
    }
#endif
    return (jint) ret;
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nGetBufferSize
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_sound_DirectAudioDevice_nGetBufferSize
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
    int ret = -1;
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        ret = DAUDIO_GetBufferSize(info->handle, (int) isSource);
    }
#endif
    return (jint) ret;
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nIsStillDraining
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_sound_DirectAudioDevice_nIsStillDraining
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
    int ret = FALSE;
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        ret = DAUDIO_StillDraining(info->handle, (int) isSource)?TRUE:FALSE;
    }
#endif
    return (jboolean) ret;
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nFlush
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nFlush
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_Flush(info->handle, (int) isSource);
    }
#endif
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nAvailable
 * Signature: (JZ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_media_sound_DirectAudioDevice_nAvailable
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
    int ret = -1;
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        ret = DAUDIO_GetAvailable(info->handle, (int) isSource);
    }
#endif
    return (jint) ret;
}


/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nGetBytePosition
 * Signature: (JZJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_sound_DirectAudioDevice_nGetBytePosition
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource, jlong javaBytePos) {
    INT64 ret = (INT64) javaBytePos;
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        ret = DAUDIO_GetBytePosition(info->handle, (int) isSource, (INT64) javaBytePos);
    }
#endif
    return (jlong) ret;
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nSetBytePosition
 * Signature: (JZJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nSetBytePosition
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource, jlong pos) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_SetBytePosition(info->handle, (int) isSource, (INT64) pos);
    }
#endif
}

/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nRequiresServicing
 * Signature: (JZ)B
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_sound_DirectAudioDevice_nRequiresServicing
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
    int ret = FALSE;
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        ret = DAUDIO_RequiresServicing(info->handle, (int) isSource);
    }
#endif
    return (jboolean) ret;
}
/*
 * Class:     com_sun_media_sound_DirectAudioDevice
 * Method:    nService
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_sound_DirectAudioDevice_nService
(JNIEnv* env, jclass clazz, jlong id, jboolean isSource) {
#if USE_DAUDIO == TRUE
    DAUDIO_Info* info = (DAUDIO_Info*) (UINT_PTR) id;
    if (info && info->handle) {
        DAUDIO_Service(info->handle, (int) isSource);
    }
#endif
}
