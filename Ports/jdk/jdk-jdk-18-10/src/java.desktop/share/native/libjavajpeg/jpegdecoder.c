/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * This file was based upon the example.c stub file included in the
 * release 6 of the Independent JPEG Group's free JPEG software.
 * It has been updated to conform to release 6b.
 */

/* First, if system header files define "boolean" map it to "system_boolean" */
#define boolean system_boolean

#include <stdio.h>
#include <setjmp.h>
#include <string.h>
#include <stdlib.h>
#include <assert.h>

#include "jni.h"
#include "jni_util.h"

/* undo "system_boolean" hack and undef FAR since we don't use it anyway */
#undef boolean
#undef FAR
#include <jpeglib.h>
#include "jerror.h"

#ifdef __APPLE__
/* use setjmp/longjmp versions that do not save/restore the signal mask */
#define setjmp _setjmp
#define longjmp _longjmp
#endif

/* The method IDs we cache. Note that the last two belongs to the
 * java.io.InputStream class.
 */
static jmethodID sendHeaderInfoID;
static jmethodID sendPixelsByteID;
static jmethodID sendPixelsIntID;
static jmethodID InputStream_readID;
static jmethodID InputStream_availableID;

/* Initialize the Java VM instance variable when the library is
   first loaded */
JavaVM *the_jvm;

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    the_jvm = vm;
    return JNI_VERSION_1_2;
}

/*
 * ERROR HANDLING:
 *
 * The JPEG library's standard error handler (jerror.c) is divided into
 * several "methods" which you can override individually.  This lets you
 * adjust the behavior without duplicating a lot of code, which you might
 * have to update with each future release.
 *
 * Our example here shows how to override the "error_exit" method so that
 * control is returned to the library's caller when a fatal error occurs,
 * rather than calling exit() as the standard error_exit method does.
 *
 * We use C's setjmp/longjmp facility to return control.  This means that the
 * routine which calls the JPEG library must first execute a setjmp() call to
 * establish the return point.  We want the replacement error_exit to do a
 * longjmp().  But we need to make the setjmp buffer accessible to the
 * error_exit routine.  To do this, we make a private extension of the
 * standard JPEG error handler object.  (If we were using C++, we'd say we
 * were making a subclass of the regular error handler.)
 *
 * Here's the extended error handler struct:
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

  /* Always display the message. */
  /* We could postpone this until after returning, if we chose. */
  /* (*cinfo->err->output_message) (cinfo); */
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

  /* Create the message */
  (*cinfo->err->format_message) (cinfo, buffer);

  /* Send it to stderr, adding a newline */
  fprintf(stderr, "%s\n", buffer);
}




/*
 * INPUT HANDLING:
 *
 * The JPEG library's input management is defined by the jpeg_source_mgr
 * structure which contains two fields to convey the information in the
 * buffer and 5 methods which perform all buffer management.  The library
 * defines a standard input manager that uses stdio for obtaining compressed
 * jpeg data, but here we need to use Java to get our data.
 *
 * We need to make the Java class information accessible to the source_mgr
 * input routines.  We also need to store a pointer to the start of the
 * Java array being used as an input buffer so that it is not moved or
 * garbage collected while the JPEG library is using it.  To store these
 * things, we make a private extension of the standard JPEG jpeg_source_mgr
 * object.
 *
 * Here's the extended source manager struct:
 */

struct sun_jpeg_source_mgr {
  struct jpeg_source_mgr pub;   /* "public" fields */

  jobject hInputStream;
  int suspendable;
  unsigned long remaining_skip;

  JOCTET *inbuf;
  jbyteArray hInputBuffer;
  size_t inbufoffset;

  /* More stuff */
  union pixptr {
      int               *ip;
      unsigned char     *bp;
  } outbuf;
  size_t outbufSize;
  jobject hOutputBuffer;
};

typedef struct sun_jpeg_source_mgr * sun_jpeg_source_ptr;

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
static void RELEASE_ARRAYS(JNIEnv *env, sun_jpeg_source_ptr src)
{
    if (src->inbuf) {
        if (src->pub.next_input_byte == 0) {
            src->inbufoffset = -1;
        } else {
            src->inbufoffset = src->pub.next_input_byte - src->inbuf;
        }
        (*env)->ReleasePrimitiveArrayCritical(env, src->hInputBuffer,
                                              src->inbuf, 0);
        src->inbuf = 0;
    }
    if (src->outbuf.ip) {
        (*env)->ReleasePrimitiveArrayCritical(env, src->hOutputBuffer,
                                              src->outbuf.ip, 0);
        src->outbuf.ip = 0;
    }
}

static int GET_ARRAYS(JNIEnv *env, sun_jpeg_source_ptr src)
{
    if (src->hOutputBuffer) {
        assert(src->outbuf.ip == 0);
        src->outbufSize = (*env)->GetArrayLength(env, src->hOutputBuffer);
        src->outbuf.ip = (int *)(*env)->GetPrimitiveArrayCritical
            (env, src->hOutputBuffer, 0);
        if (src->outbuf.ip == 0) {
            return 0;
        }
    }
    if (src->hInputBuffer) {
        assert(src->inbuf == 0);
        src->inbuf = (JOCTET *)(*env)->GetPrimitiveArrayCritical
            (env, src->hInputBuffer, 0);
        if (src->inbuf == 0) {
            RELEASE_ARRAYS(env, src);
            return 0;
        }
        if ((int)(src->inbufoffset) >= 0) {
            src->pub.next_input_byte = src->inbuf + src->inbufoffset;
        }
    }
    return 1;
}

/*
 * Initialize source.  This is called by jpeg_read_header() before any
 * data is actually read.  Unlike init_destination(), it may leave
 * bytes_in_buffer set to 0 (in which case a fill_input_buffer() call
 * will occur immediately).
 */

GLOBAL(void)
sun_jpeg_init_source(j_decompress_ptr cinfo)
{
    sun_jpeg_source_ptr src = (sun_jpeg_source_ptr) cinfo->src;
    src->pub.next_input_byte = 0;
    src->pub.bytes_in_buffer = 0;
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
 * suspendable is turned on, the sun_jpeg_fill_suspended_buffer will
 * do the actual work of filling the buffer.
 */

GLOBAL(boolean)
sun_jpeg_fill_input_buffer(j_decompress_ptr cinfo)
{
    sun_jpeg_source_ptr src = (sun_jpeg_source_ptr) cinfo->src;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    int ret, buflen;

    if (src->suspendable) {
        return FALSE;
    }
    if (src->remaining_skip) {
        src->pub.skip_input_data(cinfo, 0);
    }
    RELEASE_ARRAYS(env, src);
    buflen = (*env)->GetArrayLength(env, src->hInputBuffer);
    ret = (*env)->CallIntMethod(env, src->hInputStream, InputStream_readID,
                                src->hInputBuffer, 0, buflen);
    if (ret > buflen) ret = buflen;
    if ((*env)->ExceptionOccurred(env) || !GET_ARRAYS(env, src)) {
        cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
    }
    if (ret <= 0) {
        /* Silently accept truncated JPEG files */
        WARNMS(cinfo, JWRN_JPEG_EOF);
        src->inbuf[0] = (JOCTET) 0xFF;
        src->inbuf[1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->pub.next_input_byte = src->inbuf;
    src->pub.bytes_in_buffer = ret;

    return TRUE;
}

/*
 * Note that with I/O suspension turned on, the JPEG library requires
 * that all buffer filling be done at the top application level.  Due
 * to the way that backtracking works, this procedure should save all
 * of the data that was left in the buffer when suspension occurred and
 * only read new data at the end.
 */

GLOBAL(void)
sun_jpeg_fill_suspended_buffer(j_decompress_ptr cinfo)
{
    sun_jpeg_source_ptr src = (sun_jpeg_source_ptr) cinfo->src;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    size_t offset, buflen;
    int ret;

    RELEASE_ARRAYS(env, src);
    ret = (*env)->CallIntMethod(env, src->hInputStream,
                                InputStream_availableID);
    if ((*env)->ExceptionOccurred(env) || !GET_ARRAYS(env, src)) {
        cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
    }
    if (ret < 0 || (unsigned int)ret <= src->remaining_skip) {
        return;
    }
    if (src->remaining_skip) {
        src->pub.skip_input_data(cinfo, 0);
    }
    /* Save the data currently in the buffer */
    offset = src->pub.bytes_in_buffer;
    if (src->pub.next_input_byte > src->inbuf) {
        memmove(src->inbuf, src->pub.next_input_byte, offset);
    }
    RELEASE_ARRAYS(env, src);
    buflen = (*env)->GetArrayLength(env, src->hInputBuffer) - offset;
    if (buflen <= 0) {
        if (!GET_ARRAYS(env, src)) {
            cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
        }
        return;
    }
    ret = (*env)->CallIntMethod(env, src->hInputStream, InputStream_readID,
                                src->hInputBuffer, offset, buflen);
    if ((ret > 0) && ((unsigned int)ret > buflen)) ret = (int)buflen;
    if ((*env)->ExceptionOccurred(env) || !GET_ARRAYS(env, src)) {
        cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
    }
    if (ret <= 0) {
        /* Silently accept truncated JPEG files */
        WARNMS(cinfo, JWRN_JPEG_EOF);
        src->inbuf[offset] = (JOCTET) 0xFF;
        src->inbuf[offset + 1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->pub.next_input_byte = src->inbuf;
    src->pub.bytes_in_buffer = ret + offset;

    return;
}

/*
 * Skip num_bytes worth of data.  The buffer pointer and count should
 * be advanced over num_bytes input bytes, refilling the buffer as
 * needed.  This is used to skip over a potentially large amount of
 * uninteresting data (such as an APPn marker).  In some applications
 * it may be possible to optimize away the reading of the skipped data,
 * but it's not clear that being smart is worth much trouble; large
 * skips are uncommon.  bytes_in_buffer may be zero on return.
 * A zero or negative skip count should be treated as a no-op.
 */
/*
 * Note that with I/O suspension turned on, this procedure should not
 * do any I/O since the JPEG library has a very simple backtracking
 * mechanism which relies on the fact that the buffer will be filled
 * only when it has backed out to the top application level.
 */

GLOBAL(void)
sun_jpeg_skip_input_data(j_decompress_ptr cinfo, long num_bytes)
{
    sun_jpeg_source_ptr src = (sun_jpeg_source_ptr) cinfo->src;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(the_jvm, JNI_VERSION_1_2);
    int ret;
    int buflen;


    if (num_bytes < 0) {
        return;
    }
    num_bytes += src->remaining_skip;
    src->remaining_skip = 0;
    ret = (int)src->pub.bytes_in_buffer; /* this conversion is safe, because capacity of the buffer is limited by jnit */
    if (ret >= num_bytes) {
        src->pub.next_input_byte += num_bytes;
        src->pub.bytes_in_buffer -= num_bytes;
        return;
    }
    num_bytes -= ret;
    if (src->suspendable) {
        src->remaining_skip = num_bytes;
        src->pub.bytes_in_buffer = 0;
        src->pub.next_input_byte = src->inbuf;
        return;
    }

    /* Note that the signature for the method indicates that it takes
     * and returns a long.  Casting the int num_bytes to a long on
     * the input should work well enough, and if we assume that the
     * return value for this particular method should always be less
     * than the argument value (or -1), then the return value coerced
     * to an int should return us the information we need...
     */
    RELEASE_ARRAYS(env, src);
    buflen =  (*env)->GetArrayLength(env, src->hInputBuffer);
    while (num_bytes > 0) {
        ret = (*env)->CallIntMethod(env, src->hInputStream,
                                    InputStream_readID,
                                    src->hInputBuffer, 0, buflen);
        if (ret > buflen) ret = buflen;
        if ((*env)->ExceptionOccurred(env)) {
            cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
        }
        if (ret < 0) {
            break;
        }
        num_bytes -= ret;
    }
    if (!GET_ARRAYS(env, src)) {
        cinfo->err->error_exit((struct jpeg_common_struct *) cinfo);
    }
    if (num_bytes > 0) {
        /* Silently accept truncated JPEG files */
        WARNMS(cinfo, JWRN_JPEG_EOF);
        src->inbuf[0] = (JOCTET) 0xFF;
        src->inbuf[1] = (JOCTET) JPEG_EOI;
        src->pub.bytes_in_buffer = 2;
        src->pub.next_input_byte = src->inbuf;
    } else {
        src->pub.bytes_in_buffer = -num_bytes;
        src->pub.next_input_byte = src->inbuf + ret + num_bytes;
    }
}

/*
 * Terminate source --- called by jpeg_finish_decompress() after all
 * data has been read.  Often a no-op.
 */

GLOBAL(void)
sun_jpeg_term_source(j_decompress_ptr cinfo)
{
}

JNIEXPORT void JNICALL
Java_sun_awt_image_JPEGImageDecoder_initIDs(JNIEnv *env, jclass cls,
                                            jclass InputStreamClass)
{
    CHECK_NULL(sendHeaderInfoID = (*env)->GetMethodID(env, cls, "sendHeaderInfo",
                                           "(IIZZZ)Z"));
    CHECK_NULL(sendPixelsByteID = (*env)->GetMethodID(env, cls, "sendPixels", "([BI)Z"));
    CHECK_NULL(sendPixelsIntID = (*env)->GetMethodID(env, cls, "sendPixels", "([II)Z"));
    CHECK_NULL(InputStream_readID = (*env)->GetMethodID(env, InputStreamClass,
                                             "read", "([BII)I"));
    CHECK_NULL(InputStream_availableID = (*env)->GetMethodID(env, InputStreamClass,
                                                  "available", "()I"));
}

JNIEXPORT void JNICALL
Java_sun_awt_image_JPEGImageDecoder_readImage(JNIEnv *env,
                                              jobject this,
                                              jobject hInputStream,
                                              jbyteArray hInputBuffer)
{
  /* This struct contains the JPEG decompression parameters and pointers to
   * working space (which is allocated as needed by the JPEG library).
   */
  struct jpeg_decompress_struct cinfo;
  /* We use our private extension JPEG error handler.
   * Note that this struct must live as long as the main JPEG parameter
   * struct, to avoid dangling-pointer problems.
   */
  struct sun_jpeg_error_mgr jerr;
  struct sun_jpeg_source_mgr jsrc;

  int ret;
  unsigned char *bp;
  int *ip, pixel;
  int grayscale;
  int hasalpha;
  int buffered_mode;
  int final_pass;

  /* Step 0: verify the inputs. */

  if (hInputBuffer == 0 || hInputStream == 0) {
    JNU_ThrowNullPointerException(env, 0);
    return;
  }

  jsrc.outbuf.ip = 0;
  jsrc.inbuf = 0;

  /* Step 1: allocate and initialize JPEG decompression object */

  /* We set up the normal JPEG error routines, then override error_exit. */
  cinfo.err = jpeg_std_error(&jerr.pub);
  jerr.pub.error_exit = sun_jpeg_error_exit;

  /* We need to setup our own print routines */
  jerr.pub.output_message = sun_jpeg_output_message;

  /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
  if (setjmp(jerr.setjmp_buffer)) {
    /* If we get here, the JPEG code has signaled an error.
     * We need to clean up the JPEG object, close the input file, and return.
     */
    jpeg_destroy_decompress(&cinfo);
    RELEASE_ARRAYS(env, &jsrc);
    if (!(*env)->ExceptionOccurred(env)) {
        char buffer[JMSG_LENGTH_MAX];
        (*cinfo.err->format_message) ((struct jpeg_common_struct *) &cinfo,
                                      buffer);
        JNU_ThrowByName(env, "sun/awt/image/ImageFormatException", buffer);
    }
    return;
  }
  /* Now we can initialize the JPEG decompression object. */
  jpeg_create_decompress(&cinfo);

  /* Step 2: specify data source (eg, a file) */

  cinfo.src = &jsrc.pub;
  jsrc.hInputStream = hInputStream;
  jsrc.hInputBuffer = hInputBuffer;
  jsrc.hOutputBuffer = 0;
  jsrc.suspendable = FALSE;
  jsrc.remaining_skip = 0;
  jsrc.inbufoffset = -1;
  jsrc.pub.init_source = sun_jpeg_init_source;
  jsrc.pub.fill_input_buffer = sun_jpeg_fill_input_buffer;
  jsrc.pub.skip_input_data = sun_jpeg_skip_input_data;
  jsrc.pub.resync_to_restart = jpeg_resync_to_restart; /* use default method */
  jsrc.pub.term_source = sun_jpeg_term_source;
  if (!GET_ARRAYS(env, &jsrc)) {
    jpeg_destroy_decompress(&cinfo);
    return;
  }
  /* Step 3: read file parameters with jpeg_read_header() */

  (void) jpeg_read_header(&cinfo, TRUE);
  /* select buffered-image mode if it is a progressive JPEG only */
  buffered_mode = cinfo.buffered_image = jpeg_has_multiple_scans(&cinfo);
  grayscale = (cinfo.out_color_space == JCS_GRAYSCALE);
  hasalpha = 0;
  /* We can ignore the return value from jpeg_read_header since
   *   (a) suspension is not possible with the stdio data source, and
   *                                    (nor with the Java input source)
   *   (b) we passed TRUE to reject a tables-only JPEG file as an error.
   * See libjpeg.doc for more info.
   */
  RELEASE_ARRAYS(env, &jsrc);
  ret = (*env)->CallBooleanMethod(env, this, sendHeaderInfoID,
                                  cinfo.image_width, cinfo.image_height,
                                  grayscale, hasalpha, buffered_mode);
  if ((*env)->ExceptionOccurred(env) || !ret) {
    /* No more interest in this image... */
    jpeg_destroy_decompress(&cinfo);
    return;
  }
  /* Make a one-row-high sample array with enough room to expand to ints */
  if (grayscale) {
      jsrc.hOutputBuffer = (*env)->NewByteArray(env, cinfo.image_width);
  } else {
      jsrc.hOutputBuffer = (*env)->NewIntArray(env, cinfo.image_width);
  }

  if (jsrc.hOutputBuffer == 0 || !GET_ARRAYS(env, &jsrc)) {
    jpeg_destroy_decompress(&cinfo);
    return;
  }

  /* Step 4: set parameters for decompression */

  /* In this example, we don't need to change any of the defaults set by
   * jpeg_read_header(), so we do nothing here.
   */
  /* For the first pass for Java, we want to deal with RGB for simplicity */
  /* Unfortunately, the JPEG code does not automatically convert Grayscale */
  /* to RGB, so we have to deal with Grayscale explicitly. */
  if (!grayscale && !hasalpha) {
      cinfo.out_color_space = JCS_RGB;
  }

  /* Step 5: Start decompressor */

  jpeg_start_decompress(&cinfo);

  /* We may need to do some setup of our own at this point before reading
   * the data.  After jpeg_start_decompress() we have the correct scaled
   * output image dimensions available, as well as the output colormap
   * if we asked for color quantization.
   */

  /* Step 6: while (scan lines remain to be read) */
  /*           jpeg_read_scanlines(...); */

  /* Here we use the library's state variable cinfo.output_scanline as the
   * loop counter, so that we don't have to keep track ourselves.
   */
  if (buffered_mode) {
      final_pass = FALSE;
      cinfo.dct_method = JDCT_IFAST;
  } else {
      final_pass = TRUE;
  }
  do {
      if (buffered_mode) {
          do {
              sun_jpeg_fill_suspended_buffer(&cinfo);
              jsrc.suspendable = TRUE;
              ret = jpeg_consume_input(&cinfo);
              jsrc.suspendable = FALSE;
          } while (ret != JPEG_SUSPENDED && ret != JPEG_REACHED_EOI);
          if (ret == JPEG_REACHED_EOI) {
              final_pass = TRUE;
              cinfo.dct_method = JDCT_ISLOW;
          }
          jpeg_start_output(&cinfo, cinfo.input_scan_number);
      }
      while (cinfo.output_scanline < cinfo.output_height) {
          if (! final_pass) {
              do {
                  sun_jpeg_fill_suspended_buffer(&cinfo);
                  jsrc.suspendable = TRUE;
                  ret = jpeg_consume_input(&cinfo);
                  jsrc.suspendable = FALSE;
              } while (ret != JPEG_SUSPENDED && ret != JPEG_REACHED_EOI);
              if (ret == JPEG_REACHED_EOI) {
                  break;
              }
          }
          (void) jpeg_read_scanlines(&cinfo, (JSAMPARRAY) &(jsrc.outbuf), 1);

          if (grayscale) {
              RELEASE_ARRAYS(env, &jsrc);
              ret = (*env)->CallBooleanMethod(env, this, sendPixelsByteID,
                                              jsrc.hOutputBuffer,
                                              cinfo.output_scanline - 1);
          } else {
              if (hasalpha) {
                  ip = jsrc.outbuf.ip + jsrc.outbufSize;
                  bp = jsrc.outbuf.bp + jsrc.outbufSize * 4;
                  while (ip > jsrc.outbuf.ip) {
                      pixel = (*--bp) << 24;
                      pixel |= (*--bp);
                      pixel |= (*--bp) << 8;
                      pixel |= (*--bp) << 16;
                      *--ip = pixel;
                  }
              } else {
                  ip = jsrc.outbuf.ip + jsrc.outbufSize;
                  bp = jsrc.outbuf.bp + jsrc.outbufSize * 3;
                  while (ip > jsrc.outbuf.ip) {
                      pixel = (*--bp);
                      pixel |= (*--bp) << 8;
                      pixel |= (*--bp) << 16;
                      *--ip = pixel;
                  }
              }
              RELEASE_ARRAYS(env, &jsrc);
              ret = (*env)->CallBooleanMethod(env, this, sendPixelsIntID,
                                              jsrc.hOutputBuffer,
                                              cinfo.output_scanline - 1);
          }
          if ((*env)->ExceptionOccurred(env) || !ret ||
              !GET_ARRAYS(env, &jsrc)) {
              /* No more interest in this image... */
              jpeg_destroy_decompress(&cinfo);
              return;
          }
      }
      if (buffered_mode) {
          jpeg_finish_output(&cinfo);
      }
  } while (! final_pass);

  /* Step 7: Finish decompression */

  (void) jpeg_finish_decompress(&cinfo);
  /* We can ignore the return value since suspension is not possible
   * with the stdio data source.
   * (nor with the Java data source)
   */

  /* Step 8: Release JPEG decompression object */

  /* This is an important step since it will release a good deal of memory. */
  jpeg_destroy_decompress(&cinfo);

  /* After finish_decompress, we can close the input file.
   * Here we postpone it until after no more JPEG errors are possible,
   * so as to simplify the setjmp error logic above.  (Actually, I don't
   * think that jpeg_destroy can do an error exit, but why assume anything...)
   */
  /* Not needed for Java - the Java code will close the file */
  /* fclose(infile); */

  /* At this point you may want to check to see whether any corrupt-data
   * warnings occurred (test whether jerr.pub.num_warnings is nonzero).
   */

  /* And we're done! */

  RELEASE_ARRAYS(env, &jsrc);
  return;
}

/*
 * SOME FINE POINTS:
 *
 * In the above code, we ignored the return value of jpeg_read_scanlines,
 * which is the number of scanlines actually read.  We could get away with
 * this because we asked for only one line at a time and we weren't using
 * a suspending data source.  See libjpeg.doc for more info.
 *
 * We cheated a bit by calling alloc_sarray() after jpeg_start_decompress();
 * we should have done it beforehand to ensure that the space would be
 * counted against the JPEG max_memory setting.  In some systems the above
 * code would risk an out-of-memory error.  However, in general we don't
 * know the output image dimensions before jpeg_start_decompress(), unless we
 * call jpeg_calc_output_dimensions().  See libjpeg.doc for more about this.
 *
 * Scanlines are returned in the same order as they appear in the JPEG file,
 * which is standardly top-to-bottom.  If you must emit data bottom-to-top,
 * you can use one of the virtual arrays provided by the JPEG memory manager
 * to invert the data.  See wrbmp.c for an example.
 *
 * As with compression, some operating modes may require temporary files.
 * On some systems you may need to set up a signal handler to ensure that
 * temporary files are deleted if the program is interrupted.  See libjpeg.doc.
 */
