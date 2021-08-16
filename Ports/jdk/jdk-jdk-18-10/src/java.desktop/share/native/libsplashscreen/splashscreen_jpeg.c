/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
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

#include "splashscreen_impl.h"

#include "jpeglib.h"
#include "jerror.h"

#include <setjmp.h>

#ifdef __APPLE__
/* use setjmp/longjmp versions that do not save/restore the signal mask */
#define setjmp _setjmp
#define longjmp _longjmp
#endif

/* stream input handling */

typedef struct
{
    struct jpeg_source_mgr pub; /* public fields */
    SplashStream * stream;      /* source stream */
    JOCTET *buffer;             /* start of buffer */
    boolean start_of_file;      /* have we gotten any data yet? */
} stream_source_mgr;

typedef stream_source_mgr *stream_src_ptr;

#define INPUT_BUF_SIZE  4096    /* choose an efficiently fread'able size */

METHODDEF(void)
stream_init_source(j_decompress_ptr cinfo)
{
    stream_src_ptr src = (stream_src_ptr) cinfo->src;

    src->start_of_file = TRUE;
}

METHODDEF(boolean)
stream_fill_input_buffer(j_decompress_ptr cinfo)
{
    stream_src_ptr src = (stream_src_ptr) cinfo->src;
    size_t nbytes;


    nbytes = src->stream->read(src->stream, src->buffer, INPUT_BUF_SIZE);

    if (nbytes <= 0) {
        if (src->start_of_file) /* Treat empty input file as fatal error */
            ERREXIT(cinfo, JERR_INPUT_EMPTY);
        WARNMS(cinfo, JWRN_JPEG_EOF);
        /* Insert a fake EOI marker */
        src->buffer[0] = (JOCTET) 0xFF;
        src->buffer[1] = (JOCTET) JPEG_EOI;
        nbytes = 2;
    }

    src->pub.next_input_byte = src->buffer;
    src->pub.bytes_in_buffer = nbytes;
    src->start_of_file = FALSE;

    return TRUE;
}

METHODDEF(void)
    stream_skip_input_data(j_decompress_ptr cinfo, long num_bytes)
{
    stream_src_ptr src = (stream_src_ptr) cinfo->src;

    if (num_bytes > 0) {
        while (num_bytes > (long) src->pub.bytes_in_buffer) {
            num_bytes -= (long) src->pub.bytes_in_buffer;
            (void) stream_fill_input_buffer(cinfo);
        }
        src->pub.next_input_byte += (size_t) num_bytes;
        src->pub.bytes_in_buffer -= (size_t) num_bytes;
    }
}

METHODDEF(void)
stream_term_source(j_decompress_ptr cinfo)
{
}

static void
set_stream_src(j_decompress_ptr cinfo, SplashStream * stream)
{
    stream_src_ptr src;

    if (cinfo->src == NULL) {   /* first time for this JPEG object? */
        cinfo->src = (struct jpeg_source_mgr *)
            (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo,
            JPOOL_PERMANENT, sizeof(stream_source_mgr));
        src = (stream_src_ptr) cinfo->src;
        src->buffer = (JOCTET *)
            (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo,
            JPOOL_PERMANENT, INPUT_BUF_SIZE * sizeof(JOCTET));
    }

    src = (stream_src_ptr) cinfo->src;
    src->pub.init_source = stream_init_source;
    src->pub.fill_input_buffer = stream_fill_input_buffer;
    src->pub.skip_input_data = stream_skip_input_data;
    src->pub.resync_to_restart = jpeg_resync_to_restart;        /* use default method */
    src->pub.term_source = stream_term_source;
    src->stream = stream;
    src->pub.bytes_in_buffer = 0;       /* forces fill_input_buffer on first read */
    src->pub.next_input_byte = NULL;    /* until buffer loaded */
}

int
SplashDecodeJpeg(Splash * splash, struct jpeg_decompress_struct *cinfo)
{
    int rowStride, stride;
    JSAMPARRAY buffer;
    ImageFormat srcFormat;

    jpeg_read_header(cinfo, TRUE);

    // SplashScreen jpeg converter expects data in RGB format only
    cinfo->out_color_space = JCS_RGB;

    jpeg_start_decompress(cinfo);

    SplashCleanup(splash);

    splash->width = cinfo->output_width;
    splash->height = cinfo->output_height;

    if (!SAFE_TO_ALLOC(splash->imageFormat.depthBytes, splash->width)) {
        return 0;
    }
    stride = splash->width * splash->imageFormat.depthBytes;

    if (!SAFE_TO_ALLOC(stride, splash->height)) {
        return 0;
    }
    if (!SAFE_TO_ALLOC(cinfo->output_width, cinfo->output_components)) {
        return 0;
    }

    splash->frameCount = 1;
    splash->frames = (SplashImage *) malloc(sizeof(SplashImage) *
        splash->frameCount);
    if (splash->frames == NULL) {
        return 0;
    }
    memset(splash->frames, 0, sizeof(SplashImage) *
        splash->frameCount);

    splash->loopCount = 1;
    splash->frames[0].delay = 0;
    splash->frames[0].bitmapBits = malloc(stride * splash->height);
    if (splash->frames[0].bitmapBits == NULL) {
        free(splash->frames);
        return 0;
    }

    rowStride = cinfo->output_width * cinfo->output_components;

    buffer = (*cinfo->mem->alloc_sarray)
        ((j_common_ptr) cinfo, JPOOL_IMAGE, rowStride, 1);
    if (buffer == NULL) {
        free(splash->frames[0].bitmapBits);
        free(splash->frames);
        return 0;
    }

    initFormat(&srcFormat, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000);
    srcFormat.byteOrder = BYTE_ORDER_LSBFIRST;
    srcFormat.depthBytes = 3;
    srcFormat.fixedBits = 0xFF000000;

    splash->maskRequired = 0;   // reset maskRequired as JPEG can't be transparent

    while (cinfo->output_scanline < cinfo->output_height) {
        rgbquad_t *out =
            (rgbquad_t *) ((byte_t *) splash->frames[0].bitmapBits +
                cinfo->output_scanline * stride);

        jpeg_read_scanlines(cinfo, buffer, 1);
        convertLine(buffer[0], sizeof(JSAMPLE) * 3, out,
            splash->imageFormat.depthBytes, cinfo->output_width, &srcFormat,
            &splash->imageFormat, CVT_COPY, NULL, 0, NULL,
            cinfo->output_scanline, 0);
    }
    jpeg_finish_decompress(cinfo);

    return 1;
}

struct my_error_mgr
{
    struct jpeg_error_mgr pub;  /* "public" fields */
    jmp_buf setjmp_buffer;      /* for return to caller */
};

typedef struct my_error_mgr *my_error_ptr;

static void
my_error_exit(j_common_ptr cinfo)
{
    /* cinfo->err really points to a my_error_mgr struct, so coerce pointer */
    my_error_ptr myerr = (my_error_ptr) cinfo->err;

    /* Always display the message. */
    /* We could postpone this until after returning, if we chose. */
    (*cinfo->err->output_message) (cinfo);

    /* Return control to the setjmp point */
    longjmp(myerr->setjmp_buffer, 1);
}

int
SplashDecodeJpegStream(Splash * splash, SplashStream * stream)
{
    struct jpeg_decompress_struct cinfo;
    int success;
    struct my_error_mgr jerr;

    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;

    if (setjmp(jerr.setjmp_buffer)) {
        success = 0;
        goto done;
    }
    jpeg_create_decompress(&cinfo);
    set_stream_src(&cinfo, stream);
    success = SplashDecodeJpeg(splash, &cinfo);

  done:
    jpeg_destroy_decompress(&cinfo);
    return success;
}
