/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include <png.h>

#include <setjmp.h>

#define SIG_BYTES 8

void PNGAPI
my_png_read_stream(png_structp png_ptr, png_bytep data, png_size_t length)
{
    png_uint_32 check;

    SplashStream * stream = (SplashStream*)png_get_io_ptr(png_ptr);
    check = stream->read(stream, data, length);
    if (check != length)
        png_error(png_ptr, "Read Error");
}

int
SplashDecodePng(Splash * splash, png_rw_ptr read_func, void *io_ptr)
{
    int stride;
    ImageFormat srcFormat;
    png_uint_32 i, rowbytes;
    volatile png_bytepp row_pointers = NULL;
    volatile png_bytep image_data = NULL;
    int success = 0;
    double gamma;

    png_structp png_ptr = NULL;
    png_infop info_ptr = NULL;

    png_uint_32 width, height;
    int bit_depth, color_type;

    ImageRect srcRect, dstRect;

    png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (!png_ptr) {
        goto done;
    }

    info_ptr = png_create_info_struct(png_ptr);
    if (!info_ptr) {
        goto done;
    }

#ifdef __APPLE__
    /* use setjmp/longjmp versions that do not save/restore the signal mask */
    if (_setjmp(png_set_longjmp_fn(png_ptr, _longjmp, sizeof(jmp_buf)))) {
#else
    if (setjmp(png_jmpbuf(png_ptr))) {
#endif
        goto done;
    }

    png_set_read_fn(png_ptr, io_ptr, read_func);

    png_set_sig_bytes(png_ptr, SIG_BYTES);      /* we already read the 8 signature bytes */

    png_read_info(png_ptr, info_ptr);   /* read all PNG info up to image data */

    png_get_IHDR(png_ptr, info_ptr, &width, &height, &bit_depth, &color_type,
        NULL, NULL, NULL);

    /* expand palette images to RGB, low-bit-depth grayscale images to 8 bits,
     * transparency chunks to full alpha channel; strip 16-bit-per-sample
     * images to 8 bits per sample; and convert grayscale to RGB[A]
     * this may be sub-optimal but this simplifies implementation */

    png_set_expand(png_ptr);
    png_set_tRNS_to_alpha(png_ptr);
    png_set_filler(png_ptr, 0xff, PNG_FILLER_AFTER);
    png_set_strip_16(png_ptr);
    png_set_gray_to_rgb(png_ptr);

    if (png_get_gAMA(png_ptr, info_ptr, &gamma))
        png_set_gamma(png_ptr, 2.2, gamma);

    png_set_interlace_handling(png_ptr);
    png_read_update_info(png_ptr, info_ptr);

    rowbytes = png_get_rowbytes(png_ptr, info_ptr);

    if (!SAFE_TO_ALLOC(rowbytes, height)) {
        goto done;
    }

    if ((image_data = (unsigned char *) malloc(rowbytes * height)) == NULL) {
        goto done;
    }

    if (!SAFE_TO_ALLOC(height, sizeof(png_bytep))) {
        goto done;
    }
    if ((row_pointers = (png_bytepp) malloc(height * sizeof(png_bytep)))
            == NULL) {
        goto done;
    }

    for (i = 0; i < height; ++i)
        row_pointers[i] = image_data + i * rowbytes;

    png_read_image(png_ptr, row_pointers);

    SplashCleanup(splash);

    splash->width = width;
    splash->height = height;

    if (!SAFE_TO_ALLOC(splash->width, splash->imageFormat.depthBytes)) {
        goto done;
    }
    stride = splash->width * splash->imageFormat.depthBytes;

    if (!SAFE_TO_ALLOC(splash->height, stride)) {
        goto done;
    }
    splash->frameCount = 1;
    splash->frames = (SplashImage *)
        malloc(sizeof(SplashImage) * splash->frameCount);

    if (splash->frames == NULL) {
        goto done;
    }

    splash->loopCount = 1;
    splash->frames[0].bitmapBits = malloc(stride * splash->height);
    if (splash->frames[0].bitmapBits == NULL) {
        free(splash->frames);
        goto done;
    }
    splash->frames[0].delay = 0;

    /* FIXME: sort out the real format */
    initFormat(&srcFormat, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
    srcFormat.byteOrder = BYTE_ORDER_MSBFIRST;

    initRect(&srcRect, 0, 0, width, height, 1, rowbytes,
        image_data, &srcFormat);
    initRect(&dstRect, 0, 0, width, height, 1, stride,
        splash->frames[0].bitmapBits, &splash->imageFormat);
    convertRect(&srcRect, &dstRect, CVT_COPY);

    SplashInitFrameShape(splash, 0);

    png_read_end(png_ptr, NULL);
    success = 1;

  done:
    free(row_pointers);
    free(image_data);
    png_destroy_read_struct(&png_ptr, &info_ptr, NULL);
    return success;
}

int
SplashDecodePngStream(Splash * splash, SplashStream * stream)
{
    unsigned char sig[SIG_BYTES];
    int success = 0;

    stream->read(stream, sig, SIG_BYTES);
    if (png_sig_cmp(sig, 0, SIG_BYTES)) {
        goto done;
    }
    success = SplashDecodePng(splash, my_png_read_stream, stream);

  done:
    return success;
}
