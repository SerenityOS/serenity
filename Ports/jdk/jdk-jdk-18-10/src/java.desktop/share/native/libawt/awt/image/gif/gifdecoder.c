/*
 * Copyright (c) 1995, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include "jni.h"
#include "jni_util.h"

#define OUTCODELENGTH 4097

/* We use Get/ReleasePrimitiveArrayCritical functions to avoid
 * the need to copy buffer elements.
 *
 * MAKE SURE TO:
 *
 * - carefully insert pairs of RELEASE_ARRAYS and GET_ARRAYS around
 *   callbacks to Java.
 * - call RELEASE_ARRAYS before returning to Java.
 *
 * Otherwise things will go horribly wrong. There may be memory leaks,
 * excessive pinning, or even VM crashes!
 *
 * Note that GetPrimitiveArrayCritical may fail!
 */

#define GET_ARRAYS() \
    prefix  = (short *) \
        (*env)->GetPrimitiveArrayCritical(env, prefixh, 0); \
    if (prefix == 0) \
        goto out_of_memory; \
    suffix  = (unsigned char *) \
        (*env)->GetPrimitiveArrayCritical(env, suffixh, 0); \
    if (suffix == 0) \
        goto out_of_memory; \
    outCode = (unsigned char *) \
        (*env)->GetPrimitiveArrayCritical(env, outCodeh, 0); \
    if (outCode == 0) \
        goto out_of_memory; \
    rasline = (unsigned char *) \
        (*env)->GetPrimitiveArrayCritical(env, raslineh, 0); \
    if (rasline == 0) \
        goto out_of_memory; \
    block = (unsigned char *) \
        (*env)->GetPrimitiveArrayCritical(env, blockh, 0); \
    if (block == 0) \
        goto out_of_memory

/*
 * Note that it is important to check whether the arrays are NULL,
 * because GetPrimitiveArrayCritical might have failed.
 */
#define RELEASE_ARRAYS() \
if (prefix) \
    (*env)->ReleasePrimitiveArrayCritical(env, prefixh, prefix, 0); \
if (suffix) \
    (*env)->ReleasePrimitiveArrayCritical(env, suffixh, suffix, 0); \
if (outCode) \
    (*env)->ReleasePrimitiveArrayCritical(env, outCodeh, outCode, 0); \
if (rasline) \
    (*env)->ReleasePrimitiveArrayCritical(env, raslineh, rasline, 0); \
if (block) \
    (*env)->ReleasePrimitiveArrayCritical(env, blockh, block, 0)

/* Place holders for the old native interface. */

long
sun_awt_image_GifImageDecoder_parseImage()
{
  return 0;
}

void
sun_awt_image_GifImageDecoder_initIDs()
{
}

static jmethodID readID;
static jmethodID sendID;
static jfieldID prefixID;
static jfieldID suffixID;
static jfieldID outCodeID;

JNIEXPORT void JNICALL
Java_sun_awt_image_GifImageDecoder_initIDs(JNIEnv *env, jclass this)
{
    CHECK_NULL(readID = (*env)->GetMethodID(env, this, "readBytes", "([BII)I"));
    CHECK_NULL(sendID = (*env)->GetMethodID(env, this, "sendPixels",
                                 "(IIII[BLjava/awt/image/ColorModel;)I"));
    CHECK_NULL(prefixID = (*env)->GetFieldID(env, this, "prefix", "[S"));
    CHECK_NULL(suffixID = (*env)->GetFieldID(env, this, "suffix", "[B"));
    CHECK_NULL(outCodeID = (*env)->GetFieldID(env, this, "outCode", "[B"));
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_image_GifImageDecoder_parseImage(JNIEnv *env,
                                              jobject this,
                                              jint relx, jint rely,
                                              jint width, jint height,
                                              jboolean interlace,
                                              jint initCodeSize,
                                              jbyteArray blockh,
                                              jbyteArray raslineh,
                                              jobject cmh)
{
    /* Patrick Naughton:
     * Note that I ignore the possible existence of a local color map.
     * I'm told there aren't many files around that use them, and the
     * spec says it's defined for future use.  This could lead to an
     * error reading some files.
     *
     * Start reading the image data. First we get the intial code size
     * and compute decompressor constant values, based on this code
     * size.
     *
     * The GIF spec has it that the code size is the code size used to
     * compute the above values is the code size given in the file,
     * but the code size used in compression/decompression is the code
     * size given in the file plus one. (thus the ++).
     *
     * Arthur van Hoff:
     * The following narly code reads LZW compressed data blocks and
     * dumps it into the image data. The input stream is broken up into
     * blocks of 1-255 characters, each preceded by a length byte.
     * 3-12 bit codes are read from these blocks. The codes correspond to
     * entry is the hashtable (the prefix, suffix stuff), and the appropriate
     * pixels are written to the image.
     */
    static int verbose = 0;

    int clearCode = (1 << initCodeSize);
    int eofCode = clearCode + 1;
    int bitMask;
    int curCode;
    int outCount;

    /* Variables used to form reading data */
    int blockEnd = 0;
    int remain = 0;
    int byteoff = 0;
    int accumbits = 0;
    int accumdata = 0;

    /* Variables used to decompress the data */
    int codeSize = initCodeSize + 1;
    int maxCode = 1 << codeSize;
    int codeMask = maxCode - 1;
    int freeCode = clearCode + 2;
    int code = 0;
    int oldCode = 0;
    unsigned char prevChar = 0;

    /* Temproray storage for decompression */
    short *prefix;
    unsigned char *suffix = NULL;
    unsigned char *outCode = NULL;
    unsigned char *rasline = NULL;
    unsigned char *block = NULL;

    jshortArray prefixh = (*env)->GetObjectField(env, this, prefixID);
    jbyteArray suffixh = (*env)->GetObjectField(env, this, suffixID);
    jbyteArray outCodeh = (*env)->GetObjectField(env, this, outCodeID);

    int blockLength = 0;

    /* Variables used for writing pixels */
    int x = width;
    int y = 0;
    int off = 0;
    int passinc = interlace ? 8 : 1;
    int passht = passinc;
    int len;

    /* We have verified the initial code size on the java layer.
     * Here we just check bounds for particular indexes. */
    if (freeCode >= 4096 || maxCode >= 4096) {
        return 0;
    }
    if (blockh == 0 || raslineh == 0
        || prefixh == 0 || suffixh == 0
        || outCodeh == 0)
    {
        JNU_ThrowNullPointerException(env, 0);
        return 0;
    }
    if (((*env)->GetArrayLength(env, prefixh) != 4096) ||
        ((*env)->GetArrayLength(env, suffixh) != 4096) ||
        ((*env)->GetArrayLength(env, outCodeh) != OUTCODELENGTH))
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, 0);
        return 0;
    }

    if (verbose) {
        fprintf(stdout, "Decompressing...");
    }

    /* Fix for bugid 4216605 Some animated GIFs display corrupted. */
    bitMask = clearCode - 1;

    GET_ARRAYS();

    /* Read codes until the eofCode is encountered */
    for (;;) {
        if (accumbits < codeSize) {
            /* fill the buffer if needed */
            while (remain < 2) {
                if (blockEnd) {
                    /* Sometimes we have one last byte to process... */
                    if (remain == 1 && accumbits + 8 >= codeSize) {
                        remain--;
                        goto last_byte;
                    }
                    RELEASE_ARRAYS();
                    if (off > 0) {
                        (*env)->CallIntMethod(env, this, sendID,
                                              relx, rely + y,
                                              width, passht,
                                              raslineh, cmh);
                    }
                    /* quietly accept truncated GIF images */
                    return 1;
                }
                /* move remaining bytes to the beginning of the buffer */
                block[0] = block[byteoff];
                byteoff = 0;

                RELEASE_ARRAYS();
                /* fill the block */
                len = (*env)->CallIntMethod(env, this, readID,
                                            blockh, remain, blockLength + 1);
                if (len > blockLength + 1) len = blockLength + 1;
                if ((*env)->ExceptionOccurred(env)) {
                    return 0;
                }
                GET_ARRAYS();

                remain += blockLength;
                if (len > 0) {
                    remain -= (len - 1);
                    blockLength = 0;
                } else {
                    blockLength = block[remain];
                }
                if (blockLength == 0) {
                    blockEnd = 1;
                }
            }
            remain -= 2;

            /* 2 bytes at a time saves checking for accumbits < codeSize.
             * We know we'll get enough and also that we can't overflow
             * since codeSize <= 12.
             */
            accumdata += (block[byteoff++] & 0xff) << accumbits;
            accumbits += 8;
        last_byte:
            accumdata += (block[byteoff++] & 0xff) << accumbits;
            accumbits += 8;
        }

        /* Compute the code */
        code = accumdata & codeMask;
        accumdata >>= codeSize;
        accumbits -= codeSize;

        /*
         * Interpret the code
         */
        if (code == clearCode) {
            /* Clear code sets everything back to its initial value, then
             * reads the immediately subsequent code as uncompressed data.
             */
            if (verbose) {
                RELEASE_ARRAYS();
                fprintf(stdout, ".");
                fflush(stdout);
                GET_ARRAYS();
            }

            /* Note that freeCode is one less than it is supposed to be,
             * this is because it will be incremented next time round the loop
             */
            freeCode = clearCode + 1;
            codeSize = initCodeSize + 1;
            maxCode = 1 << codeSize;
            codeMask = maxCode - 1;

            /* Continue if we've NOT reached the end, some Gif images
             * contain bogus codes after the last clear code.
             */
            if (y < height) {
                continue;
            }

            /* pretend we've reached the end of the data */
            code = eofCode;
        }

        if (code == eofCode) {
            /* make sure we read the whole block of pixels. */
        flushit:
            while (!blockEnd) {
                RELEASE_ARRAYS();
                if (verbose) {
                    fprintf(stdout, "flushing %d bytes\n", blockLength);
                }
                if ((*env)->CallIntMethod(env, this, readID,
                                          blockh, 0, blockLength + 1) != 0
                    || (*env)->ExceptionOccurred(env))
                {
                    /* quietly accept truncated GIF images */
                    return (!(*env)->ExceptionOccurred(env));
                }
                GET_ARRAYS();
                blockLength = block[blockLength];
                blockEnd = (blockLength == 0);
            }
            RELEASE_ARRAYS();
            return 1;
        }

        /* It must be data: save code in CurCode */
        curCode = code;
        outCount = OUTCODELENGTH;

        /* If greater or equal to freeCode, not in the hash table
         * yet; repeat the last character decoded
         */
        if (curCode >= freeCode) {
            if (curCode > freeCode) {
                /*
                 * if we get a code too far outside our range, it
                 * could case the parser to start traversing parts
                 * of our data structure that are out of range...
                 */
                goto flushit;
            }
            curCode = oldCode;
            outCode[--outCount] = prevChar;
        }

        /* Unless this code is raw data, pursue the chain pointed
         * to by curCode through the hash table to its end; each
         * code in the chain puts its associated output code on
         * the output queue.
         */
         while (curCode > bitMask) {
             outCode[--outCount] = suffix[curCode];
             if (outCount == 0) {
                 /*
                  * In theory this should never happen since our
                  * prefix and suffix arrays are monotonically
                  * decreasing and so outCode will only be filled
                  * as much as those arrays, but I don't want to
                  * take that chance and the test is probably
                  * cheap compared to the read and write operations.
                  * If we ever do overflow the array, we will just
                  * flush the rest of the data and quietly accept
                  * the GIF as truncated here.
                  */
                 goto flushit;
             }
             curCode = prefix[curCode];
         }

        /* The last code in the chain is treated as raw data. */
        prevChar = (unsigned char)curCode;
        outCode[--outCount] = prevChar;

        /* Now we put the data out to the Output routine. It's
         * been stacked LIFO, so deal with it that way...
         *
         * Note that for some malformed images we have to skip
         * current frame and continue with rest of data
         * because we may have not enough info to interpret
         * corrupted frame correctly.
         * However, we can not skip frame without decoding it
         * and therefore we have to continue looping through data
         * but skip internal output loop.
         *
         * In particular this is possible when
         * width of the frame is set to zero. If
         * global width (i.e. width of the logical screen)
         * is zero too then zero-length scanline buffer
         * is allocated in java code and we have no buffer to
         * store decoded data in.
         */
        len = OUTCODELENGTH - outCount;
        while ((width > 0) && (--len >= 0)) {
            rasline[off++] = outCode[outCount++];

            /* Update the X-coordinate, and if it overflows, update the
             * Y-coordinate
             */
            if (--x == 0) {
                /* If a non-interlaced picture, just increment y to the next
                 * scan line.  If it's interlaced, deal with the interlace as
                 * described in the GIF spec.  Put the decoded scan line out
                 * to the screen if we haven't gone past the bottom of it
                 */
                int count;
                RELEASE_ARRAYS();
                count = (*env)->CallIntMethod(env, this, sendID,
                                              relx, rely + y,
                                              width, passht,
                                              raslineh, cmh);
                if (count <= 0 || (*env)->ExceptionOccurred(env)) {
                    /* Nobody is listening any more. */
                    if (verbose) {
                        fprintf(stdout, "Orphan gif decoder quitting\n");
                    }
                    return 0;
                }
                GET_ARRAYS();
                x = width;
                off = 0;
                /*  pass        inc     ht      ystart */
                /*   0           8      8          0   */
                /*   1           8      4          4   */
                /*   2           4      2          2   */
                /*   3           2      1          1   */
                y += passinc;
                while (y >= height) {
                    passinc = passht;
                    passht >>= 1;
                    y = passht;
                    if (passht == 0) {
                        goto flushit;
                    }
                }
            }
        }

        /* Build the hash table on-the-fly. No table is stored in the file. */
        prefix[freeCode] = (short)oldCode;
        suffix[freeCode] = prevChar;
        oldCode = code;

        /* Point to the next slot in the table.  If we exceed the
         * maxCode, increment the code size unless
         * it's already 12.  If it is, do nothing: the next code
         * decompressed better be CLEAR
         */
        if (++freeCode >= maxCode) {
            if (codeSize < 12) {
                codeSize++;
                maxCode <<= 1;
                codeMask = maxCode - 1;
            } else {
                /* Just in case */
                freeCode = maxCode - 1;
            }
        }
    }
out_of_memory:
    RELEASE_ARRAYS();
    return 0;
}
