/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file contains the code to link the Java Image I/O JPEG plug-in
 * to the IJG library used to read and write JPEG files.  Much of it has
 * been copied, updated, and annotated from the jpegdecoder.c AWT JPEG
 * decoder.  Where that code was unclear, the present author has either
 * rewritten the relevant section or commented it for the sake of future
 * maintainers.
 *
 * In particular, the way the AWT code handled progressive JPEGs seems
 * to me to be only accidentally correct and somewhat inefficient.  The
 * scheme used here represents the way I think it should work. (REV 11/00)
 */

#include <stdlib.h>
#include <setjmp.h>
#include <assert.h>
#include <string.h>
#include <limits.h>

/* java native interface headers */
#include "jni.h"
#include "jni_util.h"

#include "com_sun_imageio_plugins_jpeg_JPEGImageReader.h"
#include "com_sun_imageio_plugins_jpeg_JPEGImageWriter.h"

/* headers from the JPEG library */
#include <jpeglib.h>
#include <jerror.h>

#undef MAX
#define MAX(a,b)        ((a) > (b) ? (a) : (b))

#ifdef __APPLE__
/* use setjmp/longjmp versions that do not save/restore the signal mask */
#define setjmp _setjmp
#define longjmp _longjmp
#endif

/* Cached Java method ids */
static jmethodID JPEGImageReader_readInputDataID;
static jmethodID JPEGImageReader_skipInputBytesID;
static jmethodID JPEGImageReader_warningOccurredID;
static jmethodID JPEGImageReader_warningWithMessageID;
static jmethodID JPEGImageReader_setImageDataID;
static jmethodID JPEGImageReader_acceptPixelsID;
static jmethodID JPEGImageReader_pushBackID;
static jmethodID JPEGImageReader_passStartedID;
static jmethodID JPEGImageReader_passCompleteID;
static jmethodID JPEGImageReader_skipPastImageID;
static jmethodID JPEGImageWriter_writeOutputDataID;
static jmethodID JPEGImageWriter_warningOccurredID;
static jmethodID JPEGImageWriter_warningWithMessageID;
static jmethodID JPEGImageWriter_writeMetadataID;
static jmethodID JPEGImageWriter_grabPixelsID;
static jfieldID JPEGQTable_tableID;
static jfieldID JPEGHuffmanTable_lengthsID;
static jfieldID JPEGHuffmanTable_valuesID;

/*
 * Defined in jpegdecoder.c.  Copy code from there if and
 * when that disappears. */
extern JavaVM *the_jvm;

/*
 * The following sets of defines must match the warning messages in the
 * Java code.
 */

/* Reader warnings */
#define READ_NO_EOI          0

/* Writer warnings */

/* Return codes for various ops */
#define OK     1
#define NOT_OK 0

/*
 * First we define two objects, one for the stream and buffer and one
 * for pixels.  Both contain references to Java objects and pointers to
 * pinned arrays.  These objects can be used for either input or
 * output.  Pixels can be accessed as either INT32s or bytes.
 * Every I/O operation will have one of each these objects, one for
 * the stream and the other to hold pixels, regardless of the I/O direction.
 */

/******************** StreamBuffer definition ************************/

typedef struct streamBufferStruct {
    jweak ioRef;               // weak reference to a provider of I/O routines
    jbyteArray hstreamBuffer;  // Handle to a Java buffer for the stream
    JOCTET *buf;               // Pinned buffer pointer */
    size_t bufferOffset;          // holds offset between unpin and the next pin
    size_t bufferLength;          // Allocated, nut just used
    int suspendable;           // Set to true to suspend input
    long remaining_skip;       // Used only on input
} streamBuffer, *streamBufferPtr;

/*
 * This buffer size was set to 64K in the old classes, 4K by default in the
 * IJG library, with the comment "an efficiently freadable size", and 1K
 * in AWT.
 * Unlike in the other Java designs, these objects will persist, so 64K
 * seems too big and 1K seems too small.  If 4K was good enough for the
 * IJG folks, it's good enough for me.
 */
#define STREAMBUF_SIZE 4096

#define GET_IO_REF(io_name)                                            \
    do {                                                               \
        if ((*env)->IsSameObject(env, sb->ioRef, NULL) ||              \
            ((io_name) = (*env)->NewLocalRef(env, sb->ioRef)) == NULL) \
        {                                                              \
            cinfo->err->error_exit((j_common_ptr) cinfo);              \
        }                                                              \
    } while (0)                                                        \

/*
 * Used to signal that no data need be restored from an unpin to a pin.
 * I.e. the buffer is empty.
 */
#define NO_DATA ((size_t)-1)

// Forward reference
static void resetStreamBuffer(JNIEnv *env, streamBufferPtr sb);

/*
 * Initialize a freshly allocated StreamBuffer object.  The stream is left
 * null, as it will be set from Java by setSource, but the buffer object
 * is created and a global reference kept.  Returns OK on success, NOT_OK
 * if allocating the buffer or getting a global reference for it failed.
 */
static int initStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    /* Initialize a new buffer */
    jbyteArray hInputBuffer = (*env)->NewByteArray(env, STREAMBUF_SIZE);
    if (hInputBuffer == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Reader");
        return NOT_OK;
    }
    sb->bufferLength = (*env)->GetArrayLength(env, hInputBuffer);
    sb->hstreamBuffer = (*env)->NewGlobalRef(env, hInputBuffer);
    if (sb->hstreamBuffer == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Reader");
        return NOT_OK;
    }


    sb->ioRef = NULL;

    sb->buf = NULL;

    resetStreamBuffer(env, sb);

    return OK;
}

/*
 * Free all resources associated with this streamBuffer.  This must
 * be called to dispose the object to avoid leaking global references, as
 * resetStreamBuffer does not release the buffer reference.
 */
static void destroyStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    resetStreamBuffer(env, sb);
    if (sb->hstreamBuffer != NULL) {
        (*env)->DeleteGlobalRef(env, sb->hstreamBuffer);
    }
}

// Forward reference
static void unpinStreamBuffer(JNIEnv *env,
                              streamBufferPtr sb,
                              const JOCTET *next_byte);
/*
 * Resets the state of a streamBuffer object that has been in use.
 * The global reference to the stream is released, but the reference
 * to the buffer is retained.  The buffer is unpinned if it was pinned.
 * All other state is reset.
 */
static void resetStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    if (sb->ioRef != NULL) {
        (*env)->DeleteWeakGlobalRef(env, sb->ioRef);
        sb->ioRef = NULL;
    }
    unpinStreamBuffer(env, sb, NULL);
    sb->bufferOffset = NO_DATA;
    sb->suspendable = FALSE;
    sb->remaining_skip = 0;
}

/*
 * Pins the data buffer associated with this stream.  Returns OK on
 * success, NOT_OK on failure, as GetPrimitiveArrayCritical may fail.
 */
static int pinStreamBuffer(JNIEnv *env,
                           streamBufferPtr sb,
                           const JOCTET **next_byte) {
    if (sb->hstreamBuffer != NULL) {
        assert(sb->buf == NULL);
        sb->buf =
            (JOCTET *)(*env)->GetPrimitiveArrayCritical(env,
                                                        sb->hstreamBuffer,
                                                        NULL);
        if (sb->buf == NULL) {
            return NOT_OK;
        }
        if (sb->bufferOffset != NO_DATA) {
            *next_byte = sb->buf + sb->bufferOffset;
        }
    }
    return OK;
}

/*
 * Unpins the data buffer associated with this stream.
 */
static void unpinStreamBuffer(JNIEnv *env,
                              streamBufferPtr sb,
                              const JOCTET *next_byte) {
    if (sb->buf != NULL) {
        assert(sb->hstreamBuffer != NULL);
        if (next_byte == NULL) {
            sb->bufferOffset = NO_DATA;
        } else {
            sb->bufferOffset = next_byte - sb->buf;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,
                                              sb->hstreamBuffer,
                                              sb->buf,
                                              0);
        sb->buf = NULL;
    }
}

/*
 * Clear out the streamBuffer.  This just invalidates the data in the buffer.
 */
static void clearStreamBuffer(streamBufferPtr sb) {
    sb->bufferOffset = NO_DATA;
}

/*************************** end StreamBuffer definition *************/

/*************************** Pixel Buffer definition ******************/

typedef struct pixelBufferStruct {
    jobject hpixelObject;   // Usually a DataBuffer bank as a byte array
    unsigned int byteBufferLength;
    union pixptr {
        INT32         *ip;  // Pinned buffer pointer, as 32-bit ints
        unsigned char *bp;  // Pinned buffer pointer, as bytes
    } buf;
} pixelBuffer, *pixelBufferPtr;

/*
 * Initialize a freshly allocated PixelBuffer.  All fields are simply
 * set to NULL, as we have no idea what size buffer we will need.
 */
static void initPixelBuffer(pixelBufferPtr pb) {
    pb->hpixelObject = NULL;
    pb->byteBufferLength = 0;
    pb->buf.ip = NULL;
}

/*
 * Set the pixelBuffer to use the given buffer, acquiring a new global
 * reference for it.  Returns OK on success, NOT_OK on failure.
 */
static int setPixelBuffer(JNIEnv *env, pixelBufferPtr pb, jobject obj) {
    pb->hpixelObject = (*env)->NewGlobalRef(env, obj);
    if (pb->hpixelObject == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Setting Pixel Buffer");
        return NOT_OK;
    }
    pb->byteBufferLength = (*env)->GetArrayLength(env, pb->hpixelObject);
    return OK;
}

// Forward reference
static void unpinPixelBuffer(JNIEnv *env, pixelBufferPtr pb);

/*
 * Resets a pixel buffer to its initial state.  Unpins any pixel buffer,
 * releases the global reference, and resets fields to NULL.  Use this
 * method to dispose the object as well (there is no destroyPixelBuffer).
 */
static void resetPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {
    if (pb->hpixelObject != NULL) {
        unpinPixelBuffer(env, pb);
        (*env)->DeleteGlobalRef(env, pb->hpixelObject);
        pb->hpixelObject = NULL;
        pb->byteBufferLength = 0;
    }
}

/*
 * Pins the data buffer.  Returns OK on success, NOT_OK on failure.
 */
static int pinPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {
    if (pb->hpixelObject != NULL) {
        assert(pb->buf.ip == NULL);
        pb->buf.bp = (unsigned char *)(*env)->GetPrimitiveArrayCritical
            (env, pb->hpixelObject, NULL);
        if (pb->buf.bp == NULL) {
            return NOT_OK;
        }
    }
    return OK;
}

/*
 * Unpins the data buffer.
 */
static void unpinPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {

    if (pb->buf.ip != NULL) {
        assert(pb->hpixelObject != NULL);
        (*env)->ReleasePrimitiveArrayCritical(env,
                                              pb->hpixelObject,
                                              pb->buf.ip,
                                              0);
        pb->buf.ip = NULL;
    }
}

/********************* end PixelBuffer definition *******************/

/********************* ImageIOData definition ***********************/

#define MAX_BANDS 4
#define JPEG_BAND_SIZE 8
#define NUM_BAND_VALUES (1<<JPEG_BAND_SIZE)
#define MAX_JPEG_BAND_VALUE (NUM_BAND_VALUES-1)
#define HALF_MAX_JPEG_BAND_VALUE (MAX_JPEG_BAND_VALUE>>1)

/* The number of possible incoming values to be scaled. */
#define NUM_INPUT_VALUES (1 << 16)

/*
 * The principal imageioData object, opaque to I/O direction.
 * Each JPEGImageReader will have associated with it a
 * jpeg_decompress_struct, and similarly each JPEGImageWriter will
 * have associated with it a jpeg_compress_struct.  In order to
 * ensure that these associations persist from one native call to
 * the next, and to provide a central locus of imageio-specific
 * data, we define an imageioData struct containing references
 * to the Java object and the IJG structs.  The functions
 * that manipulate these objects know whether input or output is being
 * performed and therefore know how to manipulate the contents correctly.
 * If for some reason they don't, the direction can be determined by
 * checking the is_decompressor field of the jpegObj.
 * In order for lower level code to determine a
 * Java object given an IJG struct, such as for dispatching warnings,
 * we use the client_data field of the jpeg object to store a pointer
 * to the imageIOData object.  Maintenance of this pointer is performed
 * exclusively within the following access functions.  If you
 * change that, you run the risk of dangling pointers.
 */
typedef struct imageIODataStruct {
    j_common_ptr jpegObj;     // Either struct is fine
    jobject imageIOobj;       // A JPEGImageReader or a JPEGImageWriter

    streamBuffer streamBuf;   // Buffer for the stream
    pixelBuffer pixelBuf;     // Buffer for pixels

    jboolean abortFlag;       // Passed down from Java abort method
} imageIOData, *imageIODataPtr;

/*
 * Allocate and initialize a new imageIOData object to associate the
 * jpeg object and the Java object.  Returns a pointer to the new object
 * on success, NULL on failure.
 */
static imageIODataPtr initImageioData (JNIEnv *env,
                                       j_common_ptr cinfo,
                                       jobject obj) {

    imageIODataPtr data = (imageIODataPtr) malloc (sizeof(imageIOData));
    if (data == NULL) {
        return NULL;
    }

    data->jpegObj = cinfo;
    cinfo->client_data = data;

#ifdef DEBUG_IIO_JPEG
    printf("new structures: data is %p, cinfo is %p\n", data, cinfo);
#endif

    data->imageIOobj = (*env)->NewWeakGlobalRef(env, obj);
    if (data->imageIOobj == NULL) {
        free (data);
        return NULL;
    }
    if (initStreamBuffer(env, &data->streamBuf) == NOT_OK) {
        (*env)->DeleteWeakGlobalRef(env, data->imageIOobj);
        free (data);
        return NULL;
    }
    initPixelBuffer(&data->pixelBuf);

    data->abortFlag = JNI_FALSE;

    return data;
}

/*
 * Resets the imageIOData object to its initial state, as though
 * it had just been allocated and initialized.
 */
static void resetImageIOData(JNIEnv *env, imageIODataPtr data) {
    resetStreamBuffer(env, &data->streamBuf);
    resetPixelBuffer(env, &data->pixelBuf);
    data->abortFlag = JNI_FALSE;
}

/*
 * Releases all resources held by this object and its subobjects,
 * frees the object, and returns the jpeg object.  This method must
 * be called to avoid leaking global references.
 * Note that the jpeg object is not freed or destroyed, as that is
 * the client's responsibility, although the client_data field is
 * cleared.
 */
static j_common_ptr destroyImageioData(JNIEnv *env, imageIODataPtr data) {
    j_common_ptr ret = data->jpegObj;
    (*env)->DeleteWeakGlobalRef(env, data->imageIOobj);
    destroyStreamBuffer(env, &data->streamBuf);
    resetPixelBuffer(env, &data->pixelBuf);
    ret->client_data = NULL;
    free(data);
    return ret;
}

/******************** end ImageIOData definition ***********************/

/******************** Java array pinning and unpinning *****************/

/* We use Get/ReleasePrimitiveArrayCritical functions to avoid
 * the need to copy array elements for the above two objects.
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

/*
 * Release (unpin) all the arrays in use during a read.
 */
static void RELEASE_ARRAYS(JNIEnv *env, imageIODataPtr data, const JOCTET *next_byte)
{
    unpinStreamBuffer(env, &data->streamBuf, next_byte);

    unpinPixelBuffer(env, &data->pixelBuf);

}

/*
 * Get (pin) all the arrays in use during a read.
 */
static int GET_ARRAYS(JNIEnv *env, imageIODataPtr data, const JOCTET **next_byte) {
    if (pinStreamBuffer(env, &data->streamBuf, next_byte) == NOT_OK) {
        return NOT_OK;
    }

    if (pinPixelBuffer(env, &data->pixelBuf) == NOT_OK) {
        RELEASE_ARRAYS(env, data, *next_byte);
        return NOT_OK;
    }
    return OK;
}

/****** end of Java array pinning and unpinning ***********/

/****** Error Handling *******/

/*
 * Set up error handling to use setjmp/longjmp.  This is the third such
 * setup, as both the AWT jpeg decoder and the com.sun... JPEG classes
 * setup thier own.  Ultimately these should be integrated, as they all
 * do pretty much the same thing.
 */

struct sun_jpeg_error_mgr {
  struct jpeg_error_mgr pub;    /* "public" fields */

  jmp_buf setjmp_buffer;        /* for return to caller */
};

typedef struct sun_jpeg_error_mgr * sun_jpeg_error_ptr;

/*
 * Here's the routine that will replace the standard error_exit method:
 */

METHODDEF(void)
sun_jpeg_error_exit (j_common_ptr cinfo)
{
  /* cinfo->err really points to a sun_jpeg_error_mgr struct */
  sun_jpeg_error_ptr myerr = (sun_jpeg_error_ptr) cinfo->err;

  /* For Java, we will format the message and put it in the error we throw. */

  /* Return control to the setjmp point */
  longjmp(myerr->setjmp_buffer, 1);
}

/*
 * Error Message handling
 *
 * This overrides the output_message method to send JPEG messages
 *
 */

METHODDEF(void)
sun_jpeg_output_message (j_common_ptr cinfo)
{
    char buffer[JMSG_LENGTH_MAX];
    jstring string;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    jobject theObject;

    /* Create the message */
    (*cinfo->err->format_message) (cinfo, buffer);

    // Create a new java string from the message
    string = (*env)->NewStringUTF(env, buffer);
    CHECK_NULL(string);

    theObject = data->imageIOobj;

    if (cinfo->is_decompressor) {
        struct jpeg_source_mgr *src = ((j_decompress_ptr)cinfo)->src;
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env, theObject,
            JPEGImageReader_warningWithMessageID,
            string);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit(cinfo);
        }
    } else {
        struct jpeg_destination_mgr *dest = ((j_compress_ptr)cinfo)->dest;
        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
        (*env)->CallVoidMethod(env, theObject,
            JPEGImageWriter_warningWithMessageID,
            string);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data,
            (const JOCTET **)(&dest->next_output_byte))) {
            cinfo->err->error_exit(cinfo);
        }
    }
}

/* End of verbatim copy from jpegdecoder.c */

/*************** end of error handling *********************/

/*************** Shared utility code ***********************/

static void imageio_set_stream(JNIEnv *env,
                               j_common_ptr cinfo,
                               imageIODataPtr data,
                               jobject io){
    streamBufferPtr sb;
    sun_jpeg_error_ptr jerr;

    sb = &data->streamBuf;

    resetStreamBuffer(env, sb);  // Removes any old stream

    /* Now we need a new weak global reference for the I/O provider */
    if (io != NULL) { // Fix for 4411955
        sb->ioRef = (*env)->NewWeakGlobalRef(env, io);
        CHECK_NULL(sb->ioRef);
    }

    /* And finally reset state */
    data->abortFlag = JNI_FALSE;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while aborting. */
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) (cinfo,
                                           buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }
        return;
    }

    jpeg_abort(cinfo);  // Frees any markers, but not tables

}

static void imageio_reset(JNIEnv *env,
                          j_common_ptr cinfo,
                          imageIODataPtr data) {
    sun_jpeg_error_ptr jerr;

    resetImageIOData(env, data);  // Mapping to jpeg object is retained.

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while aborting. */
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) (cinfo, buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }
        return;
    }

    jpeg_abort(cinfo);  // Does not reset tables

}

static void imageio_dispose(j_common_ptr info) {

    if (info != NULL) {
        free(info->err);
        info->err = NULL;
        if (info->is_decompressor) {
            j_decompress_ptr dinfo = (j_decompress_ptr) info;
            free(dinfo->src);
            dinfo->src = NULL;
        } else {
            j_compress_ptr cinfo = (j_compress_ptr) info;
            free(cinfo->dest);
            cinfo->dest = NULL;
        }
        jpeg_destroy(info);
        free(info);
    }
}

static void imageio_abort(JNIEnv *env, jobject this,
                          imageIODataPtr data) {
    data->abortFlag = JNI_TRUE;
}

static int setQTables(JNIEnv *env,
                      j_common_ptr cinfo,
                      jobjectArray qtables,
                      boolean write) {
    jsize qlen;
    jobject table;
    jintArray qdata;
    jint *qdataBody;
    JQUANT_TBL *quant_ptr;
    int i, j;
    j_compress_ptr comp;
    j_decompress_ptr decomp;

    qlen = (*env)->GetArrayLength(env, qtables);
#ifdef DEBUG_IIO_JPEG
    printf("in setQTables, qlen = %d, write is %d\n", qlen, write);
#endif
    if (qlen > NUM_QUANT_TBLS) {
        /* Ignore extra qunterization tables. */
        qlen = NUM_QUANT_TBLS;
    }
    for (i = 0; i < qlen; i++) {
        table = (*env)->GetObjectArrayElement(env, qtables, i);
        CHECK_NULL_RETURN(table, 0);
        qdata = (*env)->GetObjectField(env, table, JPEGQTable_tableID);
        qdataBody = (*env)->GetPrimitiveArrayCritical(env, qdata, NULL);

        if (cinfo->is_decompressor) {
            decomp = (j_decompress_ptr) cinfo;
            if (decomp->quant_tbl_ptrs[i] == NULL) {
                decomp->quant_tbl_ptrs[i] =
                    jpeg_alloc_quant_table(cinfo);
            }
            quant_ptr = decomp->quant_tbl_ptrs[i];
        } else {
            comp = (j_compress_ptr) cinfo;
            if (comp->quant_tbl_ptrs[i] == NULL) {
                comp->quant_tbl_ptrs[i] =
                    jpeg_alloc_quant_table(cinfo);
            }
            quant_ptr = comp->quant_tbl_ptrs[i];
        }

        for (j = 0; j < 64; j++) {
            quant_ptr->quantval[j] = (UINT16)qdataBody[j];
        }
        quant_ptr->sent_table = !write;
        (*env)->ReleasePrimitiveArrayCritical(env,
                                              qdata,
                                              qdataBody,
                                              0);
    }
    return qlen;
}

static boolean setHuffTable(JNIEnv *env,
                         JHUFF_TBL *huff_ptr,
                         jobject table) {

    jshortArray huffLens;
    jshortArray huffValues;
    jshort *hlensBody, *hvalsBody;
    jsize hlensLen, hvalsLen;
    int i;

    // lengths
    huffLens = (*env)->GetObjectField(env,
                                      table,
                                      JPEGHuffmanTable_lengthsID);
    hlensLen = (*env)->GetArrayLength(env, huffLens);
    hlensBody = (*env)->GetShortArrayElements(env,
                                              huffLens,
                                              NULL);
    CHECK_NULL_RETURN(hlensBody, FALSE);

    if (hlensLen > 16) {
        /* Ignore extra elements of bits array. Only 16 elements can be
           stored. 0-th element is not used. (see jpeglib.h, line 107)  */
        hlensLen = 16;
    }
    for (i = 1; i <= hlensLen; i++) {
        huff_ptr->bits[i] = (UINT8)hlensBody[i-1];
    }
    (*env)->ReleaseShortArrayElements(env,
                                      huffLens,
                                      hlensBody,
                                      JNI_ABORT);
    // values
    huffValues = (*env)->GetObjectField(env,
                                        table,
                                        JPEGHuffmanTable_valuesID);
    hvalsLen = (*env)->GetArrayLength(env, huffValues);
    hvalsBody = (*env)->GetShortArrayElements(env,
                                              huffValues,
                                              NULL);
    CHECK_NULL_RETURN(hvalsBody, FALSE);

    if (hvalsLen > 256) {
        /* Ignore extra elements of hufval array. Only 256 elements
           can be stored. (see jpeglib.h, line 109)                  */
        hlensLen = 256;
    }
    for (i = 0; i < hvalsLen; i++) {
        huff_ptr->huffval[i] = (UINT8)hvalsBody[i];
    }
    (*env)->ReleaseShortArrayElements(env,
                                      huffValues,
                                      hvalsBody,
                                      JNI_ABORT);
    return TRUE;
}

static int setHTables(JNIEnv *env,
                      j_common_ptr cinfo,
                      jobjectArray DCHuffmanTables,
                      jobjectArray ACHuffmanTables,
                      boolean write) {
    int i;
    jobject table;
    JHUFF_TBL *huff_ptr;
    j_compress_ptr comp;
    j_decompress_ptr decomp;
    jsize hlen = (*env)->GetArrayLength(env, DCHuffmanTables);

    if (hlen > NUM_HUFF_TBLS) {
        /* Ignore extra DC huffman tables. */
        hlen = NUM_HUFF_TBLS;
    }
    for (i = 0; i < hlen; i++) {
        if (cinfo->is_decompressor) {
            decomp = (j_decompress_ptr) cinfo;
            if (decomp->dc_huff_tbl_ptrs[i] == NULL) {
                decomp->dc_huff_tbl_ptrs[i] =
                    jpeg_alloc_huff_table(cinfo);
            }
            huff_ptr = decomp->dc_huff_tbl_ptrs[i];
        } else {
            comp = (j_compress_ptr) cinfo;
            if (comp->dc_huff_tbl_ptrs[i] == NULL) {
                comp->dc_huff_tbl_ptrs[i] =
                    jpeg_alloc_huff_table(cinfo);
            }
            huff_ptr = comp->dc_huff_tbl_ptrs[i];
        }
        table = (*env)->GetObjectArrayElement(env, DCHuffmanTables, i);
        if (table == NULL || !setHuffTable(env, huff_ptr, table)) {
            return 0;
        }
        huff_ptr->sent_table = !write;
    }
    hlen = (*env)->GetArrayLength(env, ACHuffmanTables);
    if (hlen > NUM_HUFF_TBLS) {
        /* Ignore extra AC huffman tables. */
        hlen = NUM_HUFF_TBLS;
    }
    for (i = 0; i < hlen; i++) {
        if (cinfo->is_decompressor) {
            decomp = (j_decompress_ptr) cinfo;
            if (decomp->ac_huff_tbl_ptrs[i] == NULL) {
                decomp->ac_huff_tbl_ptrs[i] =
                    jpeg_alloc_huff_table(cinfo);
            }
            huff_ptr = decomp->ac_huff_tbl_ptrs[i];
        } else {
            comp = (j_compress_ptr) cinfo;
            if (comp->ac_huff_tbl_ptrs[i] == NULL) {
                comp->ac_huff_tbl_ptrs[i] =
                    jpeg_alloc_huff_table(cinfo);
            }
            huff_ptr = comp->ac_huff_tbl_ptrs[i];
        }
        table = (*env)->GetObjectArrayElement(env, ACHuffmanTables, i);
        if(table == NULL || !setHuffTable(env, huff_ptr, table)) {
            return 0;
        }
        huff_ptr->sent_table = !write;
    }
    return hlen;
}


/*************** end of shared utility code ****************/

/********************** Reader Support **************************/

/********************** Source Management ***********************/

/*
 * INPUT HANDLING:
 *
 * The JPEG library's input management is defined by the jpeg_source_mgr
 * structure which contains two fields to convey the information in the
 * buffer and 5 methods which perform all buffer management.  The library
 * defines a standard input manager that uses stdio for obtaining compressed
 * jpeg data, but here we need to use Java to get our data.
 *
 * We use the library jpeg_source_mgr but our own routines that access
 * imageio-specific information in the imageIOData structure.
 */

/*
 * Initialize source.  This is called by jpeg_read_header() before any
 * data is actually read.  Unlike init_destination(), it may leave
 * bytes_in_buffer set to 0 (in which case a fill_input_buffer() call
 * will occur immediately).
 */

GLOBAL(void)
imageio_init_source(j_decompress_ptr cinfo)
{
    struct jpeg_source_mgr *src = cinfo->src;
    src->next_input_byte = NULL;
    src->bytes_in_buffer = 0;
}

/*
 * This is called whenever bytes_in_buffer has reached zero and more
 * data is wanted.  In typical applications, it should read fresh data
 * into the buffer (ignoring the current state of next_input_byte and
 * bytes_in_buffer), reset the pointer & count to the start of the
 * buffer, and return TRUE indicating that the buffer has been reloaded.
 * It is not necessary to fill the buffer entirely, only to obtain at
 * least one more byte.  bytes_in_buffer MUST be set to a positive value
 * if TRUE is returned.  A FALSE return should only be used when I/O
 * suspension is desired (this mode is discussed in the next section).
 */
/*
 * Note that with I/O suspension turned on, this procedure should not
 * do any work since the JPEG library has a very simple backtracking
 * mechanism which relies on the fact that the buffer will be filled
 * only when it has backed out to the top application level.  When
 * suspendable is turned on, imageio_fill_suspended_buffer will
 * do the actual work of filling the buffer.
 */

GLOBAL(boolean)
imageio_fill_input_buffer(j_decompress_ptr cinfo)
{
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    int ret;
    jobject input = NULL;

    /* This is where input suspends */
    if (sb->suspendable) {
        return FALSE;
    }

#ifdef DEBUG_IIO_JPEG
    printf("Filling input buffer, remaining skip is %ld, ",
           sb->remaining_skip);
    printf("Buffer length is %d\n", sb->bufferLength);
#endif

    /*
     * Definitively skips.  Could be left over if we tried to skip
     * more than a buffer's worth but suspended when getting the next
     * buffer.  Now we aren't suspended, so we can catch up.
     */
    if (sb->remaining_skip) {
        src->skip_input_data(cinfo, 0);
    }

    /*
     * Now fill a complete buffer, or as much of one as the stream
     * will give us if we are near the end.
     */
    RELEASE_ARRAYS(env, data, src->next_input_byte);

    GET_IO_REF(input);

    ret = (*env)->CallIntMethod(env,
                                input,
                                JPEGImageReader_readInputDataID,
                                sb->hstreamBuffer, 0,
                                sb->bufferLength);
    if ((ret > 0) && ((unsigned int)ret > sb->bufferLength)) {
         ret = (int)sb->bufferLength;
    }
    if ((*env)->ExceptionOccurred(env)
        || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
    }

#ifdef DEBUG_IIO_JPEG
      printf("Buffer filled. ret = %d\n", ret);
#endif
    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        jobject reader = data->imageIOobj;
#ifdef DEBUG_IIO_JPEG
      printf("YO! Early EOI! ret = %d\n", ret);
#endif
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env, reader,
                               JPEGImageReader_warningOccurredID,
                               READ_NO_EOI);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }

        sb->buf[0] = (JOCTET) 0xFF;
        sb->buf[1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->next_input_byte = sb->buf;
    src->bytes_in_buffer = ret;

    return TRUE;
}

/*
 * With I/O suspension turned on, the JPEG library requires that all
 * buffer filling be done at the top application level, using this
 * function.  Due to the way that backtracking works, this procedure
 * saves all of the data that was left in the buffer when suspension
 * occurred and read new data only at the end.
 */

GLOBAL(void)
imageio_fill_suspended_buffer(j_decompress_ptr cinfo)
{
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    jint ret;
    size_t offset, buflen;
    jobject input = NULL;

    /*
     * The original (jpegdecoder.c) had code here that called
     * InputStream.available and just returned if the number of bytes
     * available was less than any remaining skip.  Presumably this was
     * to avoid blocking, although the benefit was unclear, as no more
     * decompression can take place until more data is available, so
     * the code would block on input a little further along anyway.
     * ImageInputStreams don't have an available method, so we'll just
     * block in the skip if we have to.
     */

    if (sb->remaining_skip) {
        src->skip_input_data(cinfo, 0);
    }

    /* Save the data currently in the buffer */
    offset = src->bytes_in_buffer;
    if (src->next_input_byte > sb->buf) {
        memcpy(sb->buf, src->next_input_byte, offset);
    }


    RELEASE_ARRAYS(env, data, src->next_input_byte);

    GET_IO_REF(input);

    buflen = sb->bufferLength - offset;
    if (buflen <= 0) {
        if (!GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        return;
    }

    ret = (*env)->CallIntMethod(env, input,
                                JPEGImageReader_readInputDataID,
                                sb->hstreamBuffer,
                                offset, buflen);
    if ((ret > 0) && ((unsigned int)ret > buflen)) ret = (int)buflen;
    if ((*env)->ExceptionOccurred(env)
        || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
        cinfo->err->error_exit((j_common_ptr) cinfo);
    }
    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        jobject reader = data->imageIOobj;
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env, reader,
                               JPEGImageReader_warningOccurredID,
                               READ_NO_EOI);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }

        sb->buf[offset] = (JOCTET) 0xFF;
        sb->buf[offset + 1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->next_input_byte = sb->buf;
    src->bytes_in_buffer = ret + offset;

    return;
}

/*
 * Skip num_bytes worth of data.  The buffer pointer and count are
 * advanced over num_bytes input bytes, using the input stream
 * skipBytes method if the skip is greater than the number of bytes
 * in the buffer.  This is used to skip over a potentially large amount of
 * uninteresting data (such as an APPn marker).  bytes_in_buffer will be
 * zero on return if the skip is larger than the current contents of the
 * buffer.
 *
 * A negative skip count is treated as a no-op.  A zero skip count
 * skips any remaining skip from a previous skip while suspended.
 *
 * Note that with I/O suspension turned on, this procedure does not
 * call skipBytes since the JPEG library has a very simple backtracking
 * mechanism which relies on the fact that the application level has
 * exclusive control over actual I/O.
 */

GLOBAL(void)
imageio_skip_input_data(j_decompress_ptr cinfo, long num_bytes)
{
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    jlong ret;
    jobject reader;
    jobject input = NULL;

    if (num_bytes < 0) {
        return;
    }
    num_bytes += sb->remaining_skip;
    sb->remaining_skip = 0;

    /* First the easy case where we are skipping <= the current contents. */
    ret = src->bytes_in_buffer;
    if (ret >= num_bytes) {
        src->next_input_byte += num_bytes;
        src->bytes_in_buffer -= num_bytes;
        return;
    }

    /*
     * We are skipping more than is in the buffer.  We empty the buffer and,
     * if we aren't suspended, call the Java skipBytes method.  We always
     * leave the buffer empty, to be filled by either fill method above.
     */
    src->bytes_in_buffer = 0;
    src->next_input_byte = sb->buf;

    num_bytes -= (long)ret;
    if (sb->suspendable) {
        sb->remaining_skip = num_bytes;
        return;
    }

    RELEASE_ARRAYS(env, data, src->next_input_byte);

    GET_IO_REF(input);

    ret = (*env)->CallLongMethod(env,
                                 input,
                                 JPEGImageReader_skipInputBytesID,
                                 (jlong) num_bytes);
    if ((*env)->ExceptionOccurred(env)
        || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
    }

    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        reader = data->imageIOobj;
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env,
                               reader,
                               JPEGImageReader_warningOccurredID,
                               READ_NO_EOI);

        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        sb->buf[0] = (JOCTET) 0xFF;
        sb->buf[1] = (JOCTET) JPEG_EOI;
        src->bytes_in_buffer = 2;
        src->next_input_byte = sb->buf;
    }
}

/*
 * Terminate source --- called by jpeg_finish_decompress() after all
 * data for an image has been read.  In our case pushes back any
 * remaining data, as it will be for another image and must be available
 * for java to find out that there is another image.  Also called if
 * reseting state after reading a tables-only image.
 */

GLOBAL(void)
imageio_term_source(j_decompress_ptr cinfo)
{
    // To pushback, just seek back by src->bytes_in_buffer
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    jobject reader = data->imageIOobj;
    if (src->bytes_in_buffer > 0) {
         RELEASE_ARRAYS(env, data, src->next_input_byte);
         (*env)->CallVoidMethod(env,
                                reader,
                                JPEGImageReader_pushBackID,
                                src->bytes_in_buffer);

         if ((*env)->ExceptionOccurred(env)
             || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
             cinfo->err->error_exit((j_common_ptr) cinfo);
         }
         src->bytes_in_buffer = 0;
         //src->next_input_byte = sb->buf;
    }
}

/********************* end of source manager ******************/

/********************* ICC profile support ********************/
/*
 * The following routines are modified versions of the ICC
 * profile support routines available from the IJG website.
 * The originals were written by Todd Newman
 * <tdn@eccentric.esd.sgi.com> and modified by Tom Lane for
 * the IJG.  They are further modified to fit in the context
 * of the imageio JPEG plug-in.
 */

/*
 * Since an ICC profile can be larger than the maximum size of a JPEG marker
 * (64K), we need provisions to split it into multiple markers.  The format
 * defined by the ICC specifies one or more APP2 markers containing the
 * following data:
 *      Identifying string      ASCII "ICC_PROFILE\0"  (12 bytes)
 *      Marker sequence number  1 for first APP2, 2 for next, etc (1 byte)
 *      Number of markers       Total number of APP2's used (1 byte)
 *      Profile data            (remainder of APP2 data)
 * Decoders should use the marker sequence numbers to reassemble the profile,
 * rather than assuming that the APP2 markers appear in the correct sequence.
 */

#define ICC_MARKER  (JPEG_APP0 + 2)     /* JPEG marker code for ICC */
#define ICC_OVERHEAD_LEN  14            /* size of non-profile data in APP2 */
#define MAX_BYTES_IN_MARKER  65533      /* maximum data len of a JPEG marker */
#define MAX_DATA_BYTES_IN_ICC_MARKER  (MAX_BYTES_IN_MARKER - ICC_OVERHEAD_LEN)


/*
 * Handy subroutine to test whether a saved marker is an ICC profile marker.
 */

static boolean
marker_is_icc (jpeg_saved_marker_ptr marker)
{
  return
    marker->marker == ICC_MARKER &&
    marker->data_length >= ICC_OVERHEAD_LEN &&
    /* verify the identifying string */
    GETJOCTET(marker->data[0]) == 0x49 &&
    GETJOCTET(marker->data[1]) == 0x43 &&
    GETJOCTET(marker->data[2]) == 0x43 &&
    GETJOCTET(marker->data[3]) == 0x5F &&
    GETJOCTET(marker->data[4]) == 0x50 &&
    GETJOCTET(marker->data[5]) == 0x52 &&
    GETJOCTET(marker->data[6]) == 0x4F &&
    GETJOCTET(marker->data[7]) == 0x46 &&
    GETJOCTET(marker->data[8]) == 0x49 &&
    GETJOCTET(marker->data[9]) == 0x4C &&
    GETJOCTET(marker->data[10]) == 0x45 &&
    GETJOCTET(marker->data[11]) == 0x0;
}

/*
 * See if there was an ICC profile in the JPEG file being read;
 * if so, reassemble and return the profile data as a new Java byte array.
 * If there was no ICC profile, return NULL.
 *
 * If the file contains invalid ICC APP2 markers, we throw an IIOException
 * with an appropriate message.
 */

jbyteArray
read_icc_profile (JNIEnv *env, j_decompress_ptr cinfo)
{
    jpeg_saved_marker_ptr marker;
    int num_markers = 0;
    int num_found_markers = 0;
    int seq_no;
    JOCTET *icc_data;
    JOCTET *dst_ptr;
    unsigned int total_length;
#define MAX_SEQ_NO  255         // sufficient since marker numbers are bytes
    jpeg_saved_marker_ptr icc_markers[MAX_SEQ_NO + 1];
    int first;         // index of the first marker in the icc_markers array
    int last;          // index of the last marker in the icc_markers array
    jbyteArray data = NULL;

    /* This first pass over the saved markers discovers whether there are
     * any ICC markers and verifies the consistency of the marker numbering.
     */

    for (seq_no = 0; seq_no <= MAX_SEQ_NO; seq_no++)
        icc_markers[seq_no] = NULL;


    for (marker = cinfo->marker_list; marker != NULL; marker = marker->next) {
        if (marker_is_icc(marker)) {
            if (num_markers == 0)
                num_markers = GETJOCTET(marker->data[13]);
            else if (num_markers != GETJOCTET(marker->data[13])) {
                JNU_ThrowByName(env, "javax/imageio/IIOException",
                     "Invalid icc profile: inconsistent num_markers fields");
                return NULL;
            }
            seq_no = GETJOCTET(marker->data[12]);

            /* Some third-party tools produce images with profile chunk
             * numeration started from zero. It is inconsistent with ICC
             * spec, but seems to be recognized by majority of image
             * processing tools, so we should be more tolerant to this
             * departure from the spec.
             */
            if (seq_no < 0 || seq_no > num_markers) {
                JNU_ThrowByName(env, "javax/imageio/IIOException",
                     "Invalid icc profile: bad sequence number");
                return NULL;
            }
            if (icc_markers[seq_no] != NULL) {
                JNU_ThrowByName(env, "javax/imageio/IIOException",
                     "Invalid icc profile: duplicate sequence numbers");
                return NULL;
            }
            icc_markers[seq_no] = marker;
            num_found_markers ++;
        }
    }

    if (num_markers == 0)
        return NULL;  // There is no profile

    if (num_markers != num_found_markers) {
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Invalid icc profile: invalid number of icc markers");
        return NULL;
    }

    first = icc_markers[0] ? 0 : 1;
    last = num_found_markers + first;

    /* Check for missing markers, count total space needed.
     */
    total_length = 0;
    for (seq_no = first; seq_no < last; seq_no++) {
        unsigned int length;
        if (icc_markers[seq_no] == NULL) {
            JNU_ThrowByName(env, "javax/imageio/IIOException",
                 "Invalid icc profile: missing sequence number");
            return NULL;
        }
        /* check the data length correctness */
        length = icc_markers[seq_no]->data_length;
        if (ICC_OVERHEAD_LEN > length || length > MAX_BYTES_IN_MARKER) {
            JNU_ThrowByName(env, "javax/imageio/IIOException",
                 "Invalid icc profile: invalid data length");
            return NULL;
        }
        total_length += (length - ICC_OVERHEAD_LEN);
    }

    if (total_length <= 0) {
        JNU_ThrowByName(env, "javax/imageio/IIOException",
              "Invalid icc profile: found only empty markers");
        return NULL;
    }

    /* Allocate a Java byte array for assembled data */

    data = (*env)->NewByteArray(env, total_length);
    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/OutOfMemoryError",
                        "Reading ICC profile");
        return NULL;
    }

    icc_data = (JOCTET *)(*env)->GetPrimitiveArrayCritical(env,
                                                           data,
                                                           NULL);
    if (icc_data == NULL) {
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Unable to pin icc profile data array");
        return NULL;
    }

    /* and fill it in */
    dst_ptr = icc_data;
    for (seq_no = first; seq_no < last; seq_no++) {
        JOCTET FAR *src_ptr = icc_markers[seq_no]->data + ICC_OVERHEAD_LEN;
        unsigned int length =
            icc_markers[seq_no]->data_length - ICC_OVERHEAD_LEN;

        memcpy(dst_ptr, src_ptr, length);
        dst_ptr += length;
    }

    /* finally, unpin the array */
    (*env)->ReleasePrimitiveArrayCritical(env,
                                          data,
                                          icc_data,
                                          0);


    return data;
}

/********************* end of ICC profile support *************/

/********************* Reader JNI calls ***********************/

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_initReaderIDs
    (JNIEnv *env,
     jclass cls,
     jclass ImageInputStreamClass,
     jclass qTableClass,
     jclass huffClass) {

    CHECK_NULL(JPEGImageReader_readInputDataID = (*env)->GetMethodID(env,
                                                  cls,
                                                  "readInputData",
                                                  "([BII)I"));
    CHECK_NULL(JPEGImageReader_skipInputBytesID = (*env)->GetMethodID(env,
                                                       cls,
                                                       "skipInputBytes",
                                                       "(J)J"));
    CHECK_NULL(JPEGImageReader_warningOccurredID = (*env)->GetMethodID(env,
                                                            cls,
                                                            "warningOccurred",
                                                            "(I)V"));
    CHECK_NULL(JPEGImageReader_warningWithMessageID =
        (*env)->GetMethodID(env,
                            cls,
                            "warningWithMessage",
                            "(Ljava/lang/String;)V"));
    CHECK_NULL(JPEGImageReader_setImageDataID = (*env)->GetMethodID(env,
                                                         cls,
                                                         "setImageData",
                                                         "(IIIII[B)V"));
    CHECK_NULL(JPEGImageReader_acceptPixelsID = (*env)->GetMethodID(env,
                                                         cls,
                                                         "acceptPixels",
                                                         "(IZ)V"));
    CHECK_NULL(JPEGImageReader_passStartedID = (*env)->GetMethodID(env,
                                                        cls,
                                                        "passStarted",
                                                        "(I)V"));
    CHECK_NULL(JPEGImageReader_passCompleteID = (*env)->GetMethodID(env,
                                                         cls,
                                                         "passComplete",
                                                         "()V"));
    CHECK_NULL(JPEGImageReader_pushBackID = (*env)->GetMethodID(env,
                                                     cls,
                                                     "pushBack",
                                                     "(I)V"));
    CHECK_NULL(JPEGImageReader_skipPastImageID = (*env)->GetMethodID(env,
                                                     cls,
                                                     "skipPastImage",
                                                     "(I)V"));
    CHECK_NULL(JPEGQTable_tableID = (*env)->GetFieldID(env,
                                            qTableClass,
                                            "qTable",
                                            "[I"));

    CHECK_NULL(JPEGHuffmanTable_lengthsID = (*env)->GetFieldID(env,
                                                    huffClass,
                                                    "lengths",
                                                    "[S"));

    CHECK_NULL(JPEGHuffmanTable_valuesID = (*env)->GetFieldID(env,
                                                    huffClass,
                                                    "values",
                                                    "[S"));
}

JNIEXPORT jlong JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_initJPEGImageReader
    (JNIEnv *env,
     jobject this) {

    imageIODataPtr ret;
    struct sun_jpeg_error_mgr *jerr;

    /* This struct contains the JPEG decompression parameters and pointers to
     * working space (which is allocated as needed by the JPEG library).
     */
    struct jpeg_decompress_struct *cinfo =
        malloc(sizeof(struct jpeg_decompress_struct));
    if (cinfo == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Reader");
        return 0;
    }

    /* We use our private extension JPEG error handler.
     */
    jerr = malloc (sizeof(struct sun_jpeg_error_mgr));
    if (jerr == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Reader");
        free(cinfo);
        return 0;
    }

    /* We set up the normal JPEG error routines, then override error_exit. */
    cinfo->err = jpeg_std_error(&(jerr->pub));
    jerr->pub.error_exit = sun_jpeg_error_exit;
    /* We need to setup our own print routines */
    jerr->pub.output_message = sun_jpeg_output_message;
    /* Now we can setjmp before every call to the library */

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error. */
        char buffer[JMSG_LENGTH_MAX];
        (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                                      buffer);
        JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        return 0;
    }

    /* Perform library initialization */
    jpeg_create_decompress(cinfo);

    // Set up to keep any APP2 markers, as these might contain ICC profile
    // data
    jpeg_save_markers(cinfo, ICC_MARKER, 0xFFFF);

    /*
     * Now set up our source.
     */
    cinfo->src =
        (struct jpeg_source_mgr *) malloc (sizeof(struct jpeg_source_mgr));
    if (cinfo->src == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/OutOfMemoryError",
                        "Initializing Reader");
        imageio_dispose((j_common_ptr)cinfo);
        return 0;
    }
    cinfo->src->bytes_in_buffer = 0;
    cinfo->src->next_input_byte = NULL;
    cinfo->src->init_source = imageio_init_source;
    cinfo->src->fill_input_buffer = imageio_fill_input_buffer;
    cinfo->src->skip_input_data = imageio_skip_input_data;
    cinfo->src->resync_to_restart = jpeg_resync_to_restart; // use default
    cinfo->src->term_source = imageio_term_source;

    /* set up the association to persist for future calls */
    ret = initImageioData(env, (j_common_ptr) cinfo, this);
    if (ret == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName(env, "java/lang/OutOfMemoryError",
                        "Initializing Reader");
        imageio_dispose((j_common_ptr)cinfo);
        return 0;
    }
    return ptr_to_jlong(ret);
}

/*
 * When we set a source from Java, we set up the stream in the streamBuf
 * object.  If there was an old one, it is released first.
 */

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_setSource
    (JNIEnv *env,
     jobject this,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_common_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return;
    }

    cinfo = data->jpegObj;

    imageio_set_stream(env, cinfo, data, this);

    imageio_init_source((j_decompress_ptr) cinfo);
}

#define JPEG_APP1  (JPEG_APP0 + 1)  /* EXIF APP1 marker code  */

/*
 * For EXIF images, the APP1 will appear immediately after the SOI,
 * so it's safe to only look at the first marker in the list.
 * (see http://www.exif.org/Exif2-2.PDF, section 4.7, page 58)
 */
#define IS_EXIF(c) \
    (((c)->marker_list != NULL) && ((c)->marker_list->marker == JPEG_APP1))

JNIEXPORT jboolean JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_readImageHeader
    (JNIEnv *env,
     jobject this,
     jlong ptr,
     jboolean clearFirst,
     jboolean reset) {

    int ret;
    int h_samp0, h_samp1, h_samp2;
    int v_samp0, v_samp1, v_samp2;
    int cid0, cid1, cid2;
    jboolean retval = JNI_FALSE;
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_decompress_ptr cinfo;
    struct jpeg_source_mgr *src;
    sun_jpeg_error_ptr jerr;
    jbyteArray profileData = NULL;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return JNI_FALSE;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;
    src = cinfo->src;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while reading the header. */
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                                          buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }
        return retval;
    }

#ifdef DEBUG_IIO_JPEG
    printf("In readImageHeader, data is %p cinfo is %p\n", data, cinfo);
    printf("clearFirst is %d\n", clearFirst);
#endif

    if (GET_ARRAYS(env, data, &src->next_input_byte) == NOT_OK) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName(env,
                        "javax/imageio/IIOException",
                        "Array pin failed");
        return retval;
    }

    /*
     * Now clear the input buffer if the Java code has done a seek
     * on the stream since the last call, invalidating any buffer contents.
     */
    if (clearFirst) {
        clearStreamBuffer(&data->streamBuf);
        src->next_input_byte = NULL;
        src->bytes_in_buffer = 0;
    }

    ret = jpeg_read_header(cinfo, FALSE);

    if (ret == JPEG_HEADER_TABLES_ONLY) {
        retval = JNI_TRUE;
        imageio_term_source(cinfo);  // Pushback remaining buffer contents
#ifdef DEBUG_IIO_JPEG
        printf("just read tables-only image; q table 0 at %p\n",
               cinfo->quant_tbl_ptrs[0]);
#endif
        RELEASE_ARRAYS(env, data, src->next_input_byte);
    } else {
        /*
         * Now adjust the jpeg_color_space variable, which was set in
         * default_decompress_parms, to reflect our differences from IJG
         */

        switch (cinfo->jpeg_color_space) {
        default :
          break;
        case JCS_YCbCr:

            /*
             * There are several possibilities:
             *  - we got image with embeded colorspace
             *     Use it. User knows what he is doing.
             *  - we got JFIF image
             *     Must be YCbCr (see http://www.w3.org/Graphics/JPEG/jfif3.pdf, page 2)
             *  - we got EXIF image
             *     Must be YCbCr (see http://www.exif.org/Exif2-2.PDF, section 4.7, page 63)
             *  - something else
             *     Apply heuristical rules to identify actual colorspace.
             */

            if (cinfo->saw_Adobe_marker) {
                if (cinfo->Adobe_transform != 1) {
                    /*
                     * IJG guesses this is YCbCr and emits a warning
                     * We would rather not guess.  Then the user knows
                     * To read this as a Raster if at all
                     */
                    cinfo->jpeg_color_space = JCS_UNKNOWN;
                    cinfo->out_color_space = JCS_UNKNOWN;
                }
            } else if (!cinfo->saw_JFIF_marker && !IS_EXIF(cinfo)) {
                /*
                 * In the absence of certain markers, IJG has interpreted
                 * component id's of [1,2,3] as meaning YCbCr.We follow that
                 * interpretation, which is additionally described in the Image
                 * I/O JPEG metadata spec.If that condition is not met here the
                 * next step will be to examine the subsampling factors, if
                 * there is any difference in subsampling factors we also assume
                 * YCbCr, only if both horizontal and vertical subsampling
                 * is same we assume JPEG color space as RGB.
                 * This is also described in the Image I/O JPEG metadata spec.
                 */
                h_samp0 = cinfo->comp_info[0].h_samp_factor;
                h_samp1 = cinfo->comp_info[1].h_samp_factor;
                h_samp2 = cinfo->comp_info[2].h_samp_factor;

                v_samp0 = cinfo->comp_info[0].v_samp_factor;
                v_samp1 = cinfo->comp_info[1].v_samp_factor;
                v_samp2 = cinfo->comp_info[2].v_samp_factor;

                cid0 = cinfo->comp_info[0].component_id;
                cid1 = cinfo->comp_info[1].component_id;
                cid2 = cinfo->comp_info[2].component_id;

                if ((!(cid0 == 1 && cid1 == 2 && cid2 == 3)) &&
                    ((h_samp1 == h_samp0) && (h_samp2 == h_samp0) &&
                    (v_samp1 == v_samp0) && (v_samp2 == v_samp0)))
                {
                    cinfo->jpeg_color_space = JCS_RGB;
                    /* output is already RGB, so it stays the same */
                }
            }
            break;
        case JCS_YCCK:
            if ((cinfo->saw_Adobe_marker) && (cinfo->Adobe_transform != 2)) {
                /*
                 * IJG guesses this is YCCK and emits a warning
                 * We would rather not guess.  Then the user knows
                 * To read this as a Raster if at all
                 */
                cinfo->jpeg_color_space = JCS_UNKNOWN;
                cinfo->out_color_space = JCS_UNKNOWN;
            }
            break;
        case JCS_CMYK:
            /*
             * IJG assumes all unidentified 4-channels are CMYK.
             * We assume that only if the second two channels are
             * not subsampled (either horizontally or vertically).
             * If they are, we assume YCCK.
             */
            h_samp0 = cinfo->comp_info[0].h_samp_factor;
            h_samp1 = cinfo->comp_info[1].h_samp_factor;
            h_samp2 = cinfo->comp_info[2].h_samp_factor;

            v_samp0 = cinfo->comp_info[0].v_samp_factor;
            v_samp1 = cinfo->comp_info[1].v_samp_factor;
            v_samp2 = cinfo->comp_info[2].v_samp_factor;

            if (((h_samp1 > h_samp0) && (h_samp2 > h_samp0)) ||
                ((v_samp1 > v_samp0) && (v_samp2 > v_samp0)))
            {
                cinfo->jpeg_color_space = JCS_YCCK;
                /* Leave the output space as CMYK */
            }
        }
        RELEASE_ARRAYS(env, data, src->next_input_byte);

        /* read icc profile data */
        profileData = read_icc_profile(env, cinfo);

        if ((*env)->ExceptionCheck(env)) {
            return retval;
        }

        (*env)->CallVoidMethod(env, this,
                               JPEGImageReader_setImageDataID,
                               cinfo->image_width,
                               cinfo->image_height,
                               cinfo->jpeg_color_space,
                               cinfo->out_color_space,
                               cinfo->num_components,
                               profileData);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        if (reset) {
            jpeg_abort_decompress(cinfo);
        }
        RELEASE_ARRAYS(env, data, src->next_input_byte);
    }

    return retval;
}


JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_setOutColorSpace
    (JNIEnv *env,
     jobject this,
     jlong ptr,
     jint code) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_decompress_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;

    cinfo->out_color_space = code;

}

JNIEXPORT jboolean JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_readImage
    (JNIEnv *env,
     jobject this,
     jint imageIndex,
     jlong ptr,
     jbyteArray buffer,
     jint numBands,
     jintArray srcBands,
     jintArray bandSizes,
     jint sourceXStart,
     jint sourceYStart,
     jint sourceWidth,
     jint sourceHeight,
     jint stepX,
     jint stepY,
     jobjectArray qtables,
     jobjectArray DCHuffmanTables,
     jobjectArray ACHuffmanTables,
     jint minProgressivePass,  // Counts from 0
     jint maxProgressivePass,
     jboolean wantUpdates) {


    struct jpeg_source_mgr *src;
    JSAMPROW scanLinePtr = NULL;
    jint bands[MAX_BANDS];
    int i;
    jint *body;
    int scanlineLimit;
    int pixelStride;
    unsigned char *in, *out, *pixelLimit;
    int targetLine;
    int skipLines, linesLeft;
    pixelBufferPtr pb;
    sun_jpeg_error_ptr jerr;
    boolean done;
    boolean progressive = FALSE;
    boolean orderedBands = TRUE;
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_decompress_ptr cinfo;
    size_t numBytes;

    /* verify the inputs */

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return JNI_FALSE;
    }

    if ((buffer == NULL) || (srcBands == NULL))  {
        JNU_ThrowNullPointerException(env, 0);
        return JNI_FALSE;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;

    if ((numBands < 1) || (numBands > MAX_BANDS) ||
        (sourceXStart < 0) || (sourceXStart >= (jint)cinfo->image_width) ||
        (sourceYStart < 0) || (sourceYStart >= (jint)cinfo->image_height) ||
        (sourceWidth < 1) || (sourceWidth > (jint)cinfo->image_width) ||
        (sourceHeight < 1) || (sourceHeight > (jint)cinfo->image_height) ||
        (stepX < 1) || (stepY < 1) ||
        (minProgressivePass < 0) ||
        (maxProgressivePass < minProgressivePass))
    {
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Invalid argument to native readImage");
        return JNI_FALSE;
    }

    if (stepX > (jint)cinfo->image_width) {
        stepX = cinfo->image_width;
    }
    if (stepY > (jint)cinfo->image_height) {
        stepY = cinfo->image_height;
    }

    /*
     * First get the source bands array and copy it to our local array
     * so we don't have to worry about pinning and unpinning it again.
     */

    body = (*env)->GetIntArrayElements(env, srcBands, NULL);
    if (body == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Read");
        return JNI_FALSE;
    }

    for (i = 0; i < numBands; i++) {
        bands[i] = body[i];
        if (orderedBands && (bands[i] != i)) {
            orderedBands = FALSE;
        }
    }

    (*env)->ReleaseIntArrayElements(env, srcBands, body, JNI_ABORT);

#ifdef DEBUG_IIO_JPEG
    printf("---- in reader.read ----\n");
    printf("numBands is %d\n", numBands);
    printf("bands array: ");
    for (i = 0; i < numBands; i++) {
        printf("%d ", bands[i]);
    }
    printf("\n");
    printf("jq table 0 at %p\n",
               cinfo->quant_tbl_ptrs[0]);
#endif

    data = (imageIODataPtr) cinfo->client_data;
    src = cinfo->src;

    /* Set the buffer as our PixelBuffer */
    pb = &data->pixelBuf;

    if (setPixelBuffer(env, pb, buffer) == NOT_OK) {
        return data->abortFlag;  // We already threw an out of memory exception
    }

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while reading. */
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                                          buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }
        if (scanLinePtr != NULL) {
            free(scanLinePtr);
            scanLinePtr = NULL;
        }
        return data->abortFlag;
    }

    if (GET_ARRAYS(env, data, &src->next_input_byte) == NOT_OK) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName(env,
                        "javax/imageio/IIOException",
                        "Array pin failed");
        return data->abortFlag;
    }

    // If there are no tables in our structure and table arguments aren't
    // NULL, use the table arguments.
    if ((qtables != NULL) && (cinfo->quant_tbl_ptrs[0] == NULL)) {
        (void) setQTables(env, (j_common_ptr) cinfo, qtables, TRUE);
    }

    if ((DCHuffmanTables != NULL) && (cinfo->dc_huff_tbl_ptrs[0] == NULL)) {
        setHTables(env, (j_common_ptr) cinfo,
                   DCHuffmanTables,
                   ACHuffmanTables,
                   TRUE);
    }

    progressive = jpeg_has_multiple_scans(cinfo);
    if (progressive) {
        cinfo->buffered_image = TRUE;
        cinfo->input_scan_number = minProgressivePass+1; // Java count from 0
#define MAX_JAVA_INT 2147483647 // XXX Is this defined in JNI somewhere?
        if (maxProgressivePass < MAX_JAVA_INT) {
            maxProgressivePass++; // For testing
        }
    }

    data->streamBuf.suspendable = FALSE;

    jpeg_start_decompress(cinfo);

    if (numBands !=  cinfo->output_components) {
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Invalid argument to native readImage");
        return data->abortFlag;
    }

    if (cinfo->output_components <= 0 ||
        cinfo->image_width > (0xffffffffu / (unsigned int)cinfo->output_components))
    {
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Invalid number of output components");
        return data->abortFlag;
    }

    // Allocate a 1-scanline buffer
    scanLinePtr = (JSAMPROW)malloc(cinfo->image_width*cinfo->output_components);
    if (scanLinePtr == NULL) {
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Reading JPEG Stream");
        return data->abortFlag;
    }

    // loop over progressive passes
    done = FALSE;
    while (!done) {
        if (progressive) {
            // initialize the next pass.  Note that this skips up to
            // the first interesting pass.
            jpeg_start_output(cinfo, cinfo->input_scan_number);
            if (wantUpdates) {
                RELEASE_ARRAYS(env, data, src->next_input_byte);
                (*env)->CallVoidMethod(env, this,
                                       JPEGImageReader_passStartedID,
                                       cinfo->input_scan_number-1);
                if ((*env)->ExceptionOccurred(env)
                    || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
                    cinfo->err->error_exit((j_common_ptr) cinfo);
                }
            }
        } else if (wantUpdates) {
            RELEASE_ARRAYS(env, data, src->next_input_byte);
            (*env)->CallVoidMethod(env, this,
                                   JPEGImageReader_passStartedID,
                                   0);
            if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
            }
        }

        // Skip until the first interesting line
        while ((data->abortFlag == JNI_FALSE)
               && ((jint)cinfo->output_scanline < sourceYStart)) {
            jpeg_read_scanlines(cinfo, &scanLinePtr, 1);
        }

        scanlineLimit = sourceYStart+sourceHeight;
        pixelLimit = scanLinePtr
            +(sourceXStart+sourceWidth)*cinfo->output_components;

        pixelStride = stepX*cinfo->output_components;
        targetLine = 0;

        while ((data->abortFlag == JNI_FALSE)
               && ((jint)cinfo->output_scanline < scanlineLimit)) {

            jpeg_read_scanlines(cinfo, &scanLinePtr, 1);

            // Now mangle it into our buffer
            out = data->pixelBuf.buf.bp;

            if (orderedBands && (pixelStride == numBands)) {
                // Optimization: The component bands are ordered sequentially,
                // so we can simply use memcpy() to copy the intermediate
                // scanline buffer into the raster.
                in = scanLinePtr + (sourceXStart * cinfo->output_components);
                if (pixelLimit > in) {
                    numBytes = pixelLimit - in;
                    if (numBytes > data->pixelBuf.byteBufferLength) {
                        numBytes = data->pixelBuf.byteBufferLength;
                    }
                    memcpy(out, in, numBytes);
                }
            } else {
                numBytes = numBands;
                for (in = scanLinePtr+sourceXStart*cinfo->output_components;
                     in < pixelLimit &&
                       numBytes <= data->pixelBuf.byteBufferLength;
                     in += pixelStride) {
                    for (i = 0; i < numBands; i++) {
                        *out++ = *(in+bands[i]);
                    }
                    numBytes += numBands;
                }
            }

            // And call it back to Java
            RELEASE_ARRAYS(env, data, src->next_input_byte);
            (*env)->CallVoidMethod(env,
                                   this,
                                   JPEGImageReader_acceptPixelsID,
                                   targetLine++,
                                   progressive);

            if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
            }

            // And skip over uninteresting lines to the next subsampled line
            // Ensure we don't go past the end of the image

            // Lines to skip based on subsampling
            skipLines = stepY - 1;
            // Lines left in the image
            linesLeft =  scanlineLimit - cinfo->output_scanline;
            // Take the minimum
            if (skipLines > linesLeft) {
                skipLines = linesLeft;
            }
            for(i = 0; i < skipLines; i++) {
                jpeg_read_scanlines(cinfo, &scanLinePtr, 1);
            }
        }
        if (progressive) {
            jpeg_finish_output(cinfo); // Increments pass counter
            // Call Java to notify pass complete
            if (jpeg_input_complete(cinfo)
                || (cinfo->input_scan_number > maxProgressivePass)) {
                done = TRUE;
            }
        } else {
            done = TRUE;
        }
        if (wantUpdates) {
            RELEASE_ARRAYS(env, data, src->next_input_byte);
            (*env)->CallVoidMethod(env, this,
                                   JPEGImageReader_passCompleteID);
            if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
            }
        }

    }
    /*
     * We are done, but we might not have read all the lines, or all
     * the passes, so use jpeg_abort instead of jpeg_finish_decompress.
     */
    if ((cinfo->output_scanline != cinfo->output_height) ||
        data->abortFlag == JNI_TRUE)
     {
        jpeg_abort_decompress(cinfo);
     } else if ((!jpeg_input_complete(cinfo)) &&
                (progressive &&
                 (cinfo->input_scan_number > maxProgressivePass))) {
        /* We haven't reached EOI, but we need to skip to there */
        (*cinfo->src->term_source) (cinfo);
        /* We can use jpeg_abort to release memory and reset global_state */
        jpeg_abort((j_common_ptr) cinfo);
        (*env)->CallVoidMethod(env,
                               this,
                               JPEGImageReader_skipPastImageID,
                               imageIndex);
    } else {
        jpeg_finish_decompress(cinfo);
    }

    free(scanLinePtr);

    RELEASE_ARRAYS(env, data, src->next_input_byte);

    return data->abortFlag;
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_clearNativeReadAbortFlag
    (JNIEnv *env,
    jobject this,
    jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);

    if (data == NULL) {
        JNU_ThrowByName(env,
            "java/lang/IllegalStateException",
            "Attempting to use reader after dispose()");
        return;
    }

    data->abortFlag = JNI_FALSE;

}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_abortRead
    (JNIEnv *env,
     jobject this,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return;
    }

    imageio_abort(env, this, data);

}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_resetLibraryState
    (JNIEnv *env,
     jobject this,
     jlong ptr) {
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_decompress_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;

    jpeg_abort_decompress(cinfo);
}


JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_resetReader
    (JNIEnv *env,
     jobject this,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_decompress_ptr cinfo;
    sun_jpeg_error_ptr jerr;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use reader after dispose()");
        return;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;

    jerr = (sun_jpeg_error_ptr) cinfo->err;

    imageio_reset(env, (j_common_ptr) cinfo, data);

    /*
     * The tables have not been reset, and there is no way to do so
     * in IJG without leaking memory.  The only situation in which
     * this will cause a problem is if an image-only stream is read
     * with this object without initializing the correct tables first.
     * This situation, which should cause an error, might work but
     * produce garbage instead.  If the huffman tables are wrong,
     * it will fail during the decode.  If the q tables are wrong, it
     * will look strange.  This is very unlikely, so don't worry about
     * it.  To be really robust, we would keep a flag for table state
     * and consult it to catch exceptional situations.
     */

    /* above does not clean up the source, so we have to */

    /*
      We need to explicitly initialize exception handler or we may
       longjump to random address from the term_source()
     */

    if (setjmp(jerr->setjmp_buffer)) {

        /*
          We may get IOException from pushBack() here.

          However it could be legal if original input stream was closed
          earlier (for example because network connection was closed).
          Unfortunately, image inputstream API has no way to check whether
          stream is already closed or IOException was thrown because of some
          other IO problem,
          And we can not avoid call to pushBack() on closed stream for the
          same reason.

          So, for now we will silently eat this exception.

          NB: this may be changed in future when ImageInputStream API will
          become more flexible.
        */

        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionClear(env);
        }
    } else {
        cinfo->src->term_source(cinfo);
    }

    cinfo->src->bytes_in_buffer = 0;
    cinfo->src->next_input_byte = NULL;
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageReader_disposeReader
    (JNIEnv *env,
     jclass reader,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_common_ptr info = destroyImageioData(env, data);

    imageio_dispose(info);
}

/********************** end of Reader *************************/

/********************** Writer Support ************************/

/********************** Destination Manager *******************/

METHODDEF(void)
/*
 * Initialize destination --- called by jpeg_start_compress
 * before any data is actually written.  The data arrays
 * must be pinned before this is called.
 */
imageio_init_destination (j_compress_ptr cinfo)
{
    struct jpeg_destination_mgr *dest = cinfo->dest;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);

    if (sb->buf == NULL) {
        // We forgot to pin the array
        (*env)->FatalError(env, "Output buffer not pinned!");
    }

    dest->next_output_byte = sb->buf;
    dest->free_in_buffer = sb->bufferLength;
}

/*
 * Empty the output buffer --- called whenever buffer fills up.
 *
 * This routine writes the entire output buffer
 * (ignoring the current state of next_output_byte & free_in_buffer),
 * resets the pointer & count to the start of the buffer, and returns TRUE
 * indicating that the buffer has been dumped.
 */

METHODDEF(boolean)
imageio_empty_output_buffer (j_compress_ptr cinfo)
{
    struct jpeg_destination_mgr *dest = cinfo->dest;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    jobject output = NULL;

    RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));

    GET_IO_REF(output);

    (*env)->CallVoidMethod(env,
                           output,
                           JPEGImageWriter_writeOutputDataID,
                           sb->hstreamBuffer,
                           0,
                           sb->bufferLength);
    if ((*env)->ExceptionOccurred(env)
        || !GET_ARRAYS(env, data,
                       (const JOCTET **)(&dest->next_output_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
    }

    dest->next_output_byte = sb->buf;
    dest->free_in_buffer = sb->bufferLength;

    return TRUE;
}

/*
 * After all of the data has been encoded there may still be some
 * more left over in some of the working buffers.  Now is the
 * time to clear them out.
 */
METHODDEF(void)
imageio_term_destination (j_compress_ptr cinfo)
{
    struct jpeg_destination_mgr *dest = cinfo->dest;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);

    /* find out how much needs to be written */
    /* this conversion from size_t to jint is safe, because the lenght of the buffer is limited by jint */
    jint datacount = (jint)(sb->bufferLength - dest->free_in_buffer);

    if (datacount != 0) {
        jobject output = NULL;

        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));

        GET_IO_REF(output);

        (*env)->CallVoidMethod(env,
                               output,
                               JPEGImageWriter_writeOutputDataID,
                               sb->hstreamBuffer,
                               0,
                               datacount);

        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data,
                           (const JOCTET **)(&dest->next_output_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
    }

    dest->next_output_byte = NULL;
    dest->free_in_buffer = 0;

}

/*
 * Flush the destination buffer.  This is not called by the library,
 * but by our code below.  This is the simplest implementation, though
 * certainly not the most efficient.
 */
METHODDEF(void)
imageio_flush_destination(j_compress_ptr cinfo)
{
    imageio_term_destination(cinfo);
    imageio_init_destination(cinfo);
}

/********************** end of destination manager ************/

/********************** Writer JNI calls **********************/


JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_initWriterIDs
    (JNIEnv *env,
     jclass cls,
     jclass qTableClass,
     jclass huffClass) {

    CHECK_NULL(JPEGImageWriter_writeOutputDataID = (*env)->GetMethodID(env,
                                                    cls,
                                                    "writeOutputData",
                                                    "([BII)V"));
    CHECK_NULL(JPEGImageWriter_warningOccurredID = (*env)->GetMethodID(env,
                                                            cls,
                                                            "warningOccurred",
                                                            "(I)V"));
    CHECK_NULL(JPEGImageWriter_warningWithMessageID =
                                        (*env)->GetMethodID(env,
                                                            cls,
                                                            "warningWithMessage",
                                                            "(Ljava/lang/String;)V"));
    CHECK_NULL(JPEGImageWriter_writeMetadataID = (*env)->GetMethodID(env,
                                                          cls,
                                                          "writeMetadata",
                                                          "()V"));
    CHECK_NULL(JPEGImageWriter_grabPixelsID = (*env)->GetMethodID(env,
                                                       cls,
                                                       "grabPixels",
                                                       "(I)V"));
    CHECK_NULL(JPEGQTable_tableID = (*env)->GetFieldID(env,
                                            qTableClass,
                                            "qTable",
                                            "[I"));
    CHECK_NULL(JPEGHuffmanTable_lengthsID = (*env)->GetFieldID(env,
                                                    huffClass,
                                                    "lengths",
                                                    "[S"));
    CHECK_NULL(JPEGHuffmanTable_valuesID = (*env)->GetFieldID(env,
                                                    huffClass,
                                                    "values",
                                                    "[S"));
}

JNIEXPORT jlong JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_initJPEGImageWriter
    (JNIEnv *env,
     jobject this) {

    imageIODataPtr ret;
    struct sun_jpeg_error_mgr *jerr;
    struct jpeg_destination_mgr *dest;

    /* This struct contains the JPEG compression parameters and pointers to
     * working space (which is allocated as needed by the JPEG library).
     */
    struct jpeg_compress_struct *cinfo =
        malloc(sizeof(struct jpeg_compress_struct));
    if (cinfo == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Writer");
        return 0;
    }

    /* We use our private extension JPEG error handler.
     */
    jerr = malloc (sizeof(struct sun_jpeg_error_mgr));
    if (jerr == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Writer");
        free(cinfo);
        return 0;
    }

    /* We set up the normal JPEG error routines, then override error_exit. */
    cinfo->err = jpeg_std_error(&(jerr->pub));
    jerr->pub.error_exit = sun_jpeg_error_exit;
    /* We need to setup our own print routines */
    jerr->pub.output_message = sun_jpeg_output_message;
    /* Now we can setjmp before every call to the library */

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error. */
        char buffer[JMSG_LENGTH_MAX];
        (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                                      buffer);
        JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        return 0;
    }

    /* Perform library initialization */
    jpeg_create_compress(cinfo);

    /* Now set up the destination  */
    dest = malloc(sizeof(struct jpeg_destination_mgr));
    if (dest == NULL) {
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Writer");
        imageio_dispose((j_common_ptr)cinfo);
        return 0;
    }

    dest->init_destination = imageio_init_destination;
    dest->empty_output_buffer = imageio_empty_output_buffer;
    dest->term_destination = imageio_term_destination;
    dest->next_output_byte = NULL;
    dest->free_in_buffer = 0;

    cinfo->dest = dest;

    /* set up the association to persist for future calls */
    ret = initImageioData(env, (j_common_ptr) cinfo, this);
    if (ret == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Initializing Writer");
        imageio_dispose((j_common_ptr)cinfo);
        return 0;
    }
    return ptr_to_jlong(ret);
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_setDest
    (JNIEnv *env,
     jobject this,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_compress_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use writer after dispose()");
        return;
    }

    cinfo = (j_compress_ptr) data->jpegObj;

    imageio_set_stream(env, data->jpegObj, data, this);


    // Don't call the init method, as that depends on pinned arrays
    cinfo->dest->next_output_byte = NULL;
    cinfo->dest->free_in_buffer = 0;
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_writeTables
    (JNIEnv *env,
     jobject this,
     jlong ptr,
     jobjectArray qtables,
     jobjectArray DCHuffmanTables,
     jobjectArray ACHuffmanTables) {

    struct jpeg_destination_mgr *dest;
    sun_jpeg_error_ptr jerr;
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_compress_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use writer after dispose()");
        return;
    }

    cinfo = (j_compress_ptr) data->jpegObj;
    dest = cinfo->dest;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while writing. */
        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((j_common_ptr) cinfo,
                                          buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }
        return;
    }

    if (GET_ARRAYS(env, data,
                   (const JOCTET **)(&dest->next_output_byte)) == NOT_OK) {
        (*env)->ExceptionClear(env);
        JNU_ThrowByName(env,
                        "javax/imageio/IIOException",
                        "Array pin failed");
        return;
    }

    jpeg_suppress_tables(cinfo, TRUE);  // Suppress writing of any current

    data->streamBuf.suspendable = FALSE;
    if (qtables != NULL) {
#ifdef DEBUG_IIO_JPEG
        printf("in writeTables: qtables not NULL\n");
#endif
        setQTables(env, (j_common_ptr) cinfo, qtables, TRUE);
    }

    if (DCHuffmanTables != NULL) {
        setHTables(env, (j_common_ptr) cinfo,
                   DCHuffmanTables, ACHuffmanTables, TRUE);
    }

    jpeg_write_tables(cinfo); // Flushes the buffer for you
    RELEASE_ARRAYS(env, data, NULL);
}

static void freeArray(UINT8** arr, jint size) {
    int i;
    if (arr != NULL) {
        for (i = 0; i < size; i++) {
            if (arr[i] != NULL) {
                free(arr[i]);
            }
        }
        free(arr);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_writeImage
    (JNIEnv *env,
     jobject this,
     jlong ptr,
     jbyteArray buffer,
     jint inCs, jint outCs,
     jint numBands,
     jintArray bandSizes,
     jint srcWidth,
     jint destWidth, jint destHeight,
     jint stepX, jint stepY,
     jobjectArray qtables,
     jboolean writeDQT,
     jobjectArray DCHuffmanTables,
     jobjectArray ACHuffmanTables,
     jboolean writeDHT,
     jboolean optimize,
     jboolean progressive,
     jint numScans,
     jintArray scanInfo,
     jintArray componentIds,
     jintArray HsamplingFactors,
     jintArray VsamplingFactors,
     jintArray QtableSelectors,
     jboolean haveMetadata,
     jint restartInterval) {

    struct jpeg_destination_mgr *dest;
    JSAMPROW scanLinePtr;
    int i, j;
    int pixelStride;
    unsigned char *in, *out, *pixelLimit, *scanLineLimit;
    unsigned int scanLineSize, pixelBufferSize;
    int targetLine;
    pixelBufferPtr pb;
    sun_jpeg_error_ptr jerr;
    jint *ids, *hfactors, *vfactors, *qsels;
    jsize qlen, hlen;
    int *scanptr;
    jint *scanData;
    jint *bandSize;
    int maxBandValue, halfMaxBandValue;
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_compress_ptr cinfo;
    UINT8** scale = NULL;
    boolean success = TRUE;


    /* verify the inputs */

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use writer after dispose()");
        return JNI_FALSE;
    }

    if ((buffer == NULL) ||
        (qtables == NULL) ||
        // H tables can be null if optimizing
        (componentIds == NULL) ||
        (HsamplingFactors == NULL) || (VsamplingFactors == NULL) ||
        (QtableSelectors == NULL) ||
        ((numScans != 0) && (scanInfo != NULL))) {

        JNU_ThrowNullPointerException(env, 0);
        return JNI_FALSE;

    }

    scanLineSize = destWidth * numBands;
    if ((inCs < 0) || (inCs > JCS_YCCK) ||
        (outCs < 0) || (outCs > JCS_YCCK) ||
        (numBands < 1) || (numBands > MAX_BANDS) ||
        (srcWidth < 0) ||
        (destWidth < 0) || (destWidth > srcWidth) ||
        (destHeight < 0) ||
        (stepX < 0) || (stepY < 0) ||
        ((INT_MAX / numBands) < destWidth))  /* destWidth causes an integer overflow */
    {
        JNU_ThrowByName(env, "javax/imageio/IIOException",
                        "Invalid argument to native writeImage");
        return JNI_FALSE;
    }

    if (stepX > srcWidth) {
        stepX = srcWidth;
    }

    bandSize = (*env)->GetIntArrayElements(env, bandSizes, NULL);
    CHECK_NULL_RETURN(bandSize, JNI_FALSE);

    for (i = 0; i < numBands; i++) {
        if (bandSize[i] <= 0 || bandSize[i] > JPEG_BAND_SIZE) {
            (*env)->ReleaseIntArrayElements(env, bandSizes,
                                            bandSize, JNI_ABORT);
            JNU_ThrowByName(env, "javax/imageio/IIOException", "Invalid Image");
            return JNI_FALSE;
        }
    }

    for (i = 0; i < numBands; i++) {
        if (bandSize[i] != JPEG_BAND_SIZE) {
            if (scale == NULL) {
                scale = (UINT8**) calloc(numBands, sizeof(UINT8*));

                if (scale == NULL) {
                    (*env)->ReleaseIntArrayElements(env, bandSizes,
                                                    bandSize, JNI_ABORT);
                    JNU_ThrowByName( env, "java/lang/OutOfMemoryError",
                                     "Writing JPEG Stream");
                    return JNI_FALSE;
                }
            }

            maxBandValue = (1 << bandSize[i]) - 1;

            scale[i] = (UINT8*) malloc((maxBandValue + 1) * sizeof(UINT8));

            if (scale[i] == NULL) {
                // Cleanup before throwing an out of memory exception
                for (j = 0; j < i; j++) {
                    free(scale[j]);
                }
                free(scale);
                (*env)->ReleaseIntArrayElements(env, bandSizes,
                                                bandSize, JNI_ABORT);
                JNU_ThrowByName( env, "java/lang/OutOfMemoryError",
                                 "Writing JPEG Stream");
                return JNI_FALSE;
            }

            halfMaxBandValue = maxBandValue >> 1;

            for (j = 0; j <= maxBandValue; j++) {
                scale[i][j] = (UINT8)
                    ((j*MAX_JPEG_BAND_VALUE + halfMaxBandValue)/maxBandValue);
            }
        }
    }

    (*env)->ReleaseIntArrayElements(env, bandSizes,
                                    bandSize, JNI_ABORT);

    cinfo = (j_compress_ptr) data->jpegObj;
    dest = cinfo->dest;

    /* Set the buffer as our PixelBuffer */
    pb = &data->pixelBuf;

    if (setPixelBuffer(env, pb, buffer) == NOT_OK) {
        freeArray(scale, numBands);
        return data->abortFlag;  // We already threw an out of memory exception
    }

    // Allocate a 1-scanline buffer
    scanLinePtr = (JSAMPROW)malloc(scanLineSize);
    if (scanLinePtr == NULL) {
        freeArray(scale, numBands);
        JNU_ThrowByName( env,
                         "java/lang/OutOfMemoryError",
                         "Writing JPEG Stream");
        return data->abortFlag;
    }
    scanLineLimit = scanLinePtr + scanLineSize;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while writing. */
        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((j_common_ptr) cinfo,
                                          buffer);
            JNU_ThrowByName(env, "javax/imageio/IIOException", buffer);
        }

        freeArray(scale, numBands);
        free(scanLinePtr);
        return data->abortFlag;
    }

    // set up parameters
    cinfo->image_width = destWidth;
    cinfo->image_height = destHeight;
    cinfo->input_components = numBands;
    cinfo->in_color_space = inCs;

    jpeg_set_defaults(cinfo);

    jpeg_set_colorspace(cinfo, outCs);

    cinfo->optimize_coding = optimize;

    cinfo->write_JFIF_header = FALSE;
    cinfo->write_Adobe_marker = FALSE;
    // copy componentIds
    ids = (*env)->GetIntArrayElements(env, componentIds, NULL);
    hfactors = (*env)->GetIntArrayElements(env, HsamplingFactors, NULL);
    vfactors = (*env)->GetIntArrayElements(env, VsamplingFactors, NULL);
    qsels = (*env)->GetIntArrayElements(env, QtableSelectors, NULL);

    if (ids && hfactors && vfactors && qsels) {
        for (i = 0; i < numBands; i++) {
            cinfo->comp_info[i].component_id = ids[i];
            cinfo->comp_info[i].h_samp_factor = hfactors[i];
            cinfo->comp_info[i].v_samp_factor = vfactors[i];
            cinfo->comp_info[i].quant_tbl_no = qsels[i];
        }
    } else {
        success = FALSE;
    }

    if (ids) {
        (*env)->ReleaseIntArrayElements(env, componentIds, ids, JNI_ABORT);
    }
    if (hfactors) {
        (*env)->ReleaseIntArrayElements(env, HsamplingFactors, hfactors, JNI_ABORT);
    }
    if (vfactors) {
        (*env)->ReleaseIntArrayElements(env, VsamplingFactors, vfactors, JNI_ABORT);
    }
    if (qsels) {
        (*env)->ReleaseIntArrayElements(env, QtableSelectors, qsels, JNI_ABORT);
    }
    if (!success) {
        freeArray(scale, numBands);
        free(scanLinePtr);
        return data->abortFlag;
    }

    jpeg_suppress_tables(cinfo, TRUE);  // Disable writing any current

    qlen = setQTables(env, (j_common_ptr) cinfo, qtables, writeDQT);

    if (!optimize) {
        // Set the h tables
        hlen = setHTables(env,
                          (j_common_ptr) cinfo,
                          DCHuffmanTables,
                          ACHuffmanTables,
                          writeDHT);
    }

    if (GET_ARRAYS(env, data,
                   (const JOCTET **)(&dest->next_output_byte)) == NOT_OK) {
        (*env)->ExceptionClear(env);
        freeArray(scale, numBands);
        free(scanLinePtr);
        JNU_ThrowByName(env,
                        "javax/imageio/IIOException",
                        "Array pin failed");
        return data->abortFlag;
    }

    data->streamBuf.suspendable = FALSE;

    if (progressive) {
        if (numScans == 0) { // then use default scans
            jpeg_simple_progression(cinfo);
        } else {
            cinfo->num_scans = numScans;
            // Copy the scanInfo to a local array
            // The following is copied from jpeg_simple_progression:
  /* Allocate space for script.
   * We need to put it in the permanent pool in case the application performs
   * multiple compressions without changing the settings.  To avoid a memory
   * leak if jpeg_simple_progression is called repeatedly for the same JPEG
   * object, we try to re-use previously allocated space, and we allocate
   * enough space to handle YCbCr even if initially asked for grayscale.
   */
            if (cinfo->script_space == NULL
                || cinfo->script_space_size < numScans) {
                cinfo->script_space_size = MAX(numScans, 10);
                cinfo->script_space = (jpeg_scan_info *)
                    (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo,
                                                JPOOL_PERMANENT,
                                                cinfo->script_space_size
                                                * sizeof(jpeg_scan_info));
            }
            cinfo->scan_info = cinfo->script_space;
            scanptr = (int *) cinfo->script_space;
            scanData = (*env)->GetIntArrayElements(env, scanInfo, NULL);
            if (scanData == NULL) {
                RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
                freeArray(scale, numBands);
                free(scanLinePtr);
                return data->abortFlag;
            }
            // number of jints per scan is 9
            // We avoid a memcpy to handle different size ints
            for (i = 0; i < numScans*9; i++) {
                scanptr[i] = scanData[i];
            }
            (*env)->ReleaseIntArrayElements(env, scanInfo,
                                            scanData, JNI_ABORT);

        }
    }

    cinfo->restart_interval = restartInterval;

#ifdef DEBUG_IIO_JPEG
    printf("writer setup complete, starting compressor\n");
#endif

    // start the compressor; tables must already be set
    jpeg_start_compress(cinfo, FALSE); // Leaves sent_table alone

    if (haveMetadata) {
        // Flush the buffer
        imageio_flush_destination(cinfo);
        // Call Java to write the metadata
        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
        (*env)->CallVoidMethod(env,
                               this,
                               JPEGImageWriter_writeMetadataID);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data,
                           (const JOCTET **)(&dest->next_output_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
         }
    }

    targetLine = 0;
    pixelBufferSize = srcWidth * numBands;
    pixelStride = numBands * stepX;

    // for each line in destHeight
    while ((data->abortFlag == JNI_FALSE)
           && (cinfo->next_scanline < cinfo->image_height)) {
        // get the line from Java
        RELEASE_ARRAYS(env, data, (const JOCTET *)(dest->next_output_byte));
        (*env)->CallVoidMethod(env,
                               this,
                               JPEGImageWriter_grabPixelsID,
                               targetLine);
        if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data,
                           (const JOCTET **)(&dest->next_output_byte))) {
                cinfo->err->error_exit((j_common_ptr) cinfo);
         }

        // subsample it into our buffer

        in = data->pixelBuf.buf.bp;
        out = scanLinePtr;
        pixelLimit = in + ((pixelBufferSize > data->pixelBuf.byteBufferLength) ?
                           data->pixelBuf.byteBufferLength : pixelBufferSize);
        for (; (in < pixelLimit) && (out < scanLineLimit); in += pixelStride) {
            for (i = 0; i < numBands; i++) {
                if (scale !=NULL && scale[i] != NULL) {
                    *out++ = scale[i][*(in+i)];
#ifdef DEBUG_IIO_JPEG
                    if (in == data->pixelBuf.buf.bp){ // Just the first pixel
                        printf("in %d -> out %d, ", *(in+i), *(out-i-1));
                    }
#endif

#ifdef DEBUG_IIO_JPEG
                    if (in == data->pixelBuf.buf.bp){ // Just the first pixel
                        printf("\n");
                    }
#endif
                } else {
                    *out++ = *(in+i);
                }
            }
        }
        // write it out
        jpeg_write_scanlines(cinfo, (JSAMPARRAY)&scanLinePtr, 1);
        targetLine += stepY;
    }

    /*
     * We are done, but we might not have done all the lines,
     * so use jpeg_abort instead of jpeg_finish_compress.
     */
    if (cinfo->next_scanline == cinfo->image_height) {
        jpeg_finish_compress(cinfo);  // Flushes buffer with term_dest
    } else {
        jpeg_abort((j_common_ptr)cinfo);
    }

    freeArray(scale, numBands);
    free(scanLinePtr);
    RELEASE_ARRAYS(env, data, NULL);
    return data->abortFlag;
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_abortWrite
    (JNIEnv *env,
     jobject this,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use writer after dispose()");
        return;
    }

    imageio_abort(env, this, data);
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_resetWriter
    (JNIEnv *env,
     jobject this,
     jlong ptr) {
    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_compress_ptr cinfo;

    if (data == NULL) {
        JNU_ThrowByName(env,
                        "java/lang/IllegalStateException",
                        "Attempting to use writer after dispose()");
        return;
    }

    cinfo = (j_compress_ptr) data->jpegObj;

    imageio_reset(env, (j_common_ptr) cinfo, data);

    /*
     * The tables have not been reset, and there is no way to do so
     * in IJG without leaking memory.  The only situation in which
     * this will cause a problem is if an image-only stream is written
     * with this object without initializing the correct tables first,
     * which should not be possible.
     */

    cinfo->dest->next_output_byte = NULL;
    cinfo->dest->free_in_buffer = 0;
}

JNIEXPORT void JNICALL
Java_com_sun_imageio_plugins_jpeg_JPEGImageWriter_disposeWriter
    (JNIEnv *env,
     jclass writer,
     jlong ptr) {

    imageIODataPtr data = (imageIODataPtr)jlong_to_ptr(ptr);
    j_common_ptr info = destroyImageioData(env, data);

    imageio_dispose(info);
}
