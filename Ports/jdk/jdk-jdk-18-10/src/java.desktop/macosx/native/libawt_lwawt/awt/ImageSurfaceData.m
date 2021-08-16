/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#import "ImageSurfaceData.h"

#import "java_awt_Transparency.h"
#import "java_awt_image_BufferedImage.h"
#import "sun_awt_image_BufImgSurfaceData.h"
#import "sun_java2d_OSXOffScreenSurfaceData.h"

#import "ThreadUtilities.h"
#import "JNIUtilities.h"

#import "BufImgSurfaceData.h"

//#define DEBUG 1
#if defined DEBUG
    #define IMAGE_SURFACE_INLINE
    #define PRINT(msg) {fprintf(stderr, "%s\n", msg);fflush(stderr);}
#else
    #define IMAGE_SURFACE_INLINE static inline
    #define PRINT(msg) {}
#endif

// same value as defined in Sun's own code
#define XOR_ALPHA_CUTOFF 128

// for vImage framework headers
#include <Accelerate/Accelerate.h>

static ContextInfo sDefaultContextInfo[sun_java2d_OSXOffScreenSurfaceData_TYPE_3BYTE_RGB+1] =
{
    {YES,    YES,    8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_CUSTOM            // special case
    {YES,    YES,    8,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_INT_RGB
    {YES,    YES,    8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_INT_ARGB
    {YES,    YES,    8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_INT_ARGB_PRE
    {YES,    YES,    8,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_INT_BGR
    {YES,    NO,        8,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_3BYTE_BGR        // use the default ARGB_PRE context synce we have to sync by hand anyway
    {YES,    YES,    8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_4BYTE_ABGR
    {YES,    YES,    8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_4BYTE_ABGR_PRE
#ifdef __LITTLE_ENDIAN__
    {YES,    YES,    5,        2,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder16Host,        NULL},    // TYPE_USHORT_565_RGB
    {YES,    YES,    5,        2,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder16Host,        NULL},    // TYPE_USHORT_555_RGB
#else
    {YES,    YES,    5,        2,        0,        kCGImageAlphaNoneSkipFirst,                                    NULL},    // TYPE_USHORT_565_RGB
    {YES,    YES,    5,        2,        0,        kCGImageAlphaNoneSkipFirst,                                    NULL},    // TYPE_USHORT_555_RGB
#endif
    {YES,    YES,    8,        1,        0,        kCGImageAlphaNone,                                            NULL},    // TYPE_BYTE_GRAY
    {YES,    NO,        8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_USHORT_GRAY        // use the default ARGB_PRE context synce we have to sync by hand anyway
    {NO,    NO,        8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_BYTE_BINARY        mapped to TYPE_CUSTOM
    {YES,    NO,        8,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_BYTE_INDEXED    // use the default ARGB_PRE context synce we have to sync by hand anyway
    {YES,    NO,        8,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_3BYTE_RGB
};

static ImageInfo sDefaultImageInfo[sun_java2d_OSXOffScreenSurfaceData_TYPE_3BYTE_RGB+1] =
{
    {8,        32,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_CUSTOM
    {8,        32,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_INT_RGB
    {8,        32,        4,        0,        kCGImageAlphaFirst | kCGBitmapByteOrder32Host,                NULL},    // TYPE_INT_ARGB
    {8,        32,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_INT_ARGB_PRE
    {8,        32,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_INT_BGR
    {8,        32,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_3BYTE_BGR
    {8,        32,        4,        0,        kCGImageAlphaFirst | kCGBitmapByteOrder32Host,                NULL},    // TYPE_4BYTE_ABGR
    {8,        32,        4,        0,        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Host,    NULL},    // TYPE_4BYTE_ABGR_PRE
#ifdef __LITTLE_ENDIAN__
    {5,        16,        2,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder16Host,        NULL},    // TYPE_USHORT_565_RGB
    {5,        16,        2,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder16Host,        NULL},    // TYPE_USHORT_555_RGB
#else
    {5,        16,        2,        0,        kCGImageAlphaNoneSkipFirst,                                    NULL},    // TYPE_USHORT_565_RGB
    {5,        16,        2,        0,        kCGImageAlphaNoneSkipFirst,                                    NULL},    // TYPE_USHORT_555_RGB
#endif
    {8,        8,        1,        0,        kCGImageAlphaNone,                                            NULL},    // TYPE_BYTE_GRAY
    {16,    16,        2,        0,        kCGImageAlphaNone | kCGBitmapByteOrder16Host,                NULL},    // TYPE_USHORT_GRAY
    {0,        0,        0,        0,        -1,                                                            NULL},    // TYPE_BYTE_BINARY        mapped to TYPE_CUSTOM
    {8,        32,        4,        0,        kCGImageAlphaFirst | kCGBitmapByteOrder32Host,                NULL},    // TYPE_BYTE_INDEXED  // Fully OPAQUE INDEXED images will use kCGImageAlphaNoneSkipFirst for performance reasosn. see <rdar://4224874>
    {8,        32,        4,        0,        kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host,        NULL},    // TYPE_3BYTE_RGB
};

static jfieldID        rgbID;
static jfieldID        mapSizeID;
static jfieldID        CMpDataID;
static jfieldID        allGrayID;

static jclass jc_OSXOffScreenSurfaceData = NULL;
#define GET_OSXOSD_CLASS() \
    GET_CLASS(jc_OSXOffScreenSurfaceData, "sun/java2d/OSXOffScreenSurfaceData");

CGColorSpaceRef gColorspaceRGB = NULL;
CGColorSpaceRef gColorspaceGray = NULL;

IMAGE_SURFACE_INLINE void PrintImageInfo(ImageSDOps* isdo)
{
    fprintf(stderr, "\n");
    fprintf(stderr, "PrintImageInfo:\n");
    fprintf(stderr, "\t \n");
    //fprintf(stderr, "\t magicID=%d\n", (jint)isdo->magicID);
    //fprintf(stderr, "\n");
    fprintf(stderr, "\t isdo=%p\n", isdo);
    fprintf(stderr, "\t \n");
    fprintf(stderr, "\t contextInfo:\n");
    fprintf(stderr, "\t        useWindowContextReference=%d\n", isdo->contextInfo.useWindowContextReference);
    fprintf(stderr, "\t        canUseJavaPixelsAsContext=%d\n", isdo->contextInfo.canUseJavaPixelsAsContext);
    fprintf(stderr, "\t        bitsPerComponent=%ld\n", (long)isdo->contextInfo.bitsPerComponent);
    fprintf(stderr, "\t        bytesPerPixel=%ld\n", (long)isdo->contextInfo.bytesPerPixel);
    fprintf(stderr, "\t        bytesPerRow=%ld\n", (long)isdo->contextInfo.bytesPerRow);
    fprintf(stderr, "\t        alphaInfo=%ld\n", (long)isdo->contextInfo.alphaInfo);
    fprintf(stderr, "\t \n");
    fprintf(stderr, "\t imageInfo:\n");
    fprintf(stderr, "\t        bitsPerComponent=%ld\n", (long)isdo->imageInfo.bitsPerComponent);
    fprintf(stderr, "\t        bitsPerPixel=%ld\n", (long)isdo->imageInfo.bitsPerPixel);
    fprintf(stderr, "\t        bytesPerPixel=%ld\n", (long)isdo->imageInfo.bytesPerPixel);
    fprintf(stderr, "\t        bytesPerRow=%ld\n", (long)isdo->imageInfo.bytesPerRow);
    fprintf(stderr, "\t        alphaInfo=%ld\n", (long)isdo->imageInfo.alphaInfo);
    fprintf(stderr, "\t \n");
    fprintf(stderr, "\t isSubImage=%d\n", isdo->isSubImage);
    fprintf(stderr, "\t \n");
    fprintf(stderr, "\t java info:\n");
    fprintf(stderr, "\t        array=%p\n", isdo->array);
    fprintf(stderr, "\t        offset=%d\n", (int)isdo->offset);
    fprintf(stderr, "\t        width=%d\n", (int)isdo->width);
    fprintf(stderr, "\t        height=%d\n", (int)isdo->height);
    fprintf(stderr, "\t        javaPixelBytes=%d\n", (int)isdo->javaPixelBytes);
    fprintf(stderr, "\t        javaPixelsBytesPerRow=%d\n", (int)isdo->javaPixelsBytesPerRow);
    fprintf(stderr, "\t        icm=%p\n", isdo->icm);
    fprintf(stderr, "\t        type=%d\n", (int)isdo->type);
    fprintf(stderr, "\n");
    fprintf(stderr, "\t cgRef=%p\n", isdo->qsdo.cgRef);
    fprintf(stderr, "\t nsRef=%p\n", isdo->nsRef);
    fprintf(stderr, "\n");
    fprintf(stderr, "\t pixelsLocked=%p\n", isdo->pixelsLocked);
    fprintf(stderr, "\t pixels=%p\n", isdo->pixels);
    fprintf(stderr, "\n");
    fprintf(stderr, "\t indexedColorTable=%p\n", isdo->indexedColorTable);
    fprintf(stderr, "\t lutData=%p\n", isdo->lutData);
    fprintf(stderr, "\t lutDataSize=%u\n", (unsigned)isdo->lutDataSize);
    fprintf(stderr, "\n");
    fprintf(stderr, "\t nrOfPixelsOwners=%u\n", (unsigned)isdo->nrOfPixelsOwners);
    fprintf(stderr, "\n");
}

// if there is no image created for isdo.imgRef, it creates and image using the isdo.dataProvider
// If there is an image present, this is a no-op
void makeSureImageIsCreated(ImageSDOps* isdo)
{
    if (isdo->imgRef == NULL)  // create the image
    {
        isdo->imgRef = CGImageCreate(isdo->width,
                                      isdo->height,
                                      isdo->contextInfo.bitsPerComponent,
                                      isdo->contextInfo.bytesPerPixel * 8,
                                      isdo->contextInfo.bytesPerRow,
                                      isdo->contextInfo.colorSpace,
                                      isdo->contextInfo.alphaInfo,
                                      isdo->dataProvider,
                                      NULL,
                                      NO,
                                      kCGRenderingIntentDefault);
    }
}

IMAGE_SURFACE_INLINE void customPixelsFromJava(JNIEnv *env, ImageSDOps *isdo)
{
PRINT("    customPixelsFromJava")

    SurfaceDataOps *sdo = (SurfaceDataOps*)isdo;
    GET_OSXOSD_CLASS();
    DECLARE_METHOD(jm_syncFromCustom, jc_OSXOffScreenSurfaceData, "syncFromCustom", "()V");

    (*env)->CallVoidMethod(env, sdo->sdObject, jm_syncFromCustom); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
}

IMAGE_SURFACE_INLINE void copyBits(jint w, jint h, jint javaPixelsBytesPerRow, Pixel8bit *pixelsSrc, jint dstPixelsBytesPerRow, Pixel8bit *pixelsDst)
{
PRINT("    copyBits")

    if (javaPixelsBytesPerRow == dstPixelsBytesPerRow)
    {
        memcpy(pixelsDst, pixelsSrc, h*javaPixelsBytesPerRow);
    }
    else
    {
        register jint y;
        for (y=0; y<h; y++)
        {
            memcpy(pixelsDst, pixelsSrc, dstPixelsBytesPerRow);

            pixelsSrc += javaPixelsBytesPerRow;
            pixelsDst += dstPixelsBytesPerRow;
        }
    }
}

IMAGE_SURFACE_INLINE void copySwapRandB_32bit_TYPE_4BYTE(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc, Pixel32bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copySwapRandB_32bit_TYPE_4BYTE")

    register Pixel8bit *p8Bit = NULL;
    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, red, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc++;

#ifdef __LITTLE_ENDIAN__
            pixel = CFSwapInt32BigToHost(pixel);   // the jint is in big endian format, we need to swap the bits
#endif

            red        = (pixel & 0x00ff0000) >> 16; // get original red and shift to new position
            blue    = (pixel & 0x000000ff) << 16; // get original blue and shift to new position

            pixel    = (pixel & 0xff00ff00); // erase original red&blue

            pixel    = pixel | red | blue; // construct new pixel

            *pixelsDst++ = pixel;
        }
        pixelsSrc += skip;

        p8Bit = (Pixel8bit *) pixelsDst;
        p8Bit += extraBytesPerRow;
        pixelsDst = (Pixel32bit *) p8Bit;
    }
}


IMAGE_SURFACE_INLINE void copySwapRandB_32bit_TYPE_INT(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc, Pixel32bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copySwapRandB_32bit_TYPE_INT")

    register Pixel8bit *p8Bit = NULL;
    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, red, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc++;

            red        = (pixel & 0x00ff0000) >> 16; // get original red and shift to new position
            blue    = (pixel & 0x000000ff) << 16; // get original blue and shift to new position

            pixel    = (pixel & 0xff00ff00); // erase original red&blue

            pixel    = pixel | red | blue; // construct new pixel

            *pixelsDst++ = pixel;
        }
        pixelsSrc += skip;

        p8Bit = (Pixel8bit *) pixelsDst;
        p8Bit += extraBytesPerRow;
        pixelsDst = (Pixel32bit *) p8Bit;
    }
}


IMAGE_SURFACE_INLINE void copyBGR_24bitToXRGB_32bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsSrc, Pixel32bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copyBGR_24bitToXRGB_32bit")

    register Pixel8bit *p8Bit = NULL;
    register jint skip = ((javaPixelsBytesPerRow/javaPixelBytes)-w)*javaPixelBytes; // in pixelsSrc units
    register Pixel32bit red, green, blue, pixel;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel        = *pixelsSrc++;
            blue        = pixel << 0;

            pixel        = *pixelsSrc++;
            green        = pixel << 8;

            pixel        = *pixelsSrc++;
            red            = pixel << 16;

            *pixelsDst    = red | green | blue;

            *pixelsDst = 0xff000000 | *pixelsDst;

            pixelsDst++;
        }
        pixelsSrc += skip;

        p8Bit = (Pixel8bit *) pixelsDst;
        p8Bit += extraBytesPerRow;
        pixelsDst = (Pixel32bit *) p8Bit;
    }
}

IMAGE_SURFACE_INLINE void copyRGB_24bitToXRGB_32bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsSrc, Pixel32bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copyRGB_24bitToXRGB_32bit")

    register Pixel8bit *p8Bit = NULL;
    register jint skip = ((javaPixelsBytesPerRow/javaPixelBytes)-w)*javaPixelBytes; // in pixelsSrc units
    register Pixel32bit red, green, blue, pixel;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel        = *pixelsSrc++;
            red            = pixel << 16;

            pixel        = *pixelsSrc++;
            green        = pixel << 8;

            pixel        = *pixelsSrc++;
            blue        = pixel << 0;

            *pixelsDst    = red | green | blue;

            *pixelsDst = 0xff000000 | *pixelsDst;

            pixelsDst++;
        }
        pixelsSrc += skip;

        p8Bit = (Pixel8bit *) pixelsDst;
        p8Bit += extraBytesPerRow;
        pixelsDst = (Pixel32bit *) p8Bit;
    }
}

IMAGE_SURFACE_INLINE void copyIndexed_8bitToARGB_32bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsSrc,
                                                        Pixel32bit* lutdata, Pixel32bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copyIndexed_8bitToARGB_32bit")

    //gznote: how is the performance if the extraBytesPerRow != 0 ?
    const vImage_Buffer src = {pixelsSrc, h, w, javaPixelsBytesPerRow};
    const vImage_Buffer dest = {pixelsDst, h, w, w*sizeof(Pixel32bit)+extraBytesPerRow};
    vImage_Error err = vImageLookupTable_Planar8toPlanarF(&src, &dest, (Pixel_F*)lutdata, kvImageDoNotTile);
    if (err != kvImageNoError)
    {
        fprintf(stderr, "Error in copyIndexed_8bitToARGB_32bit: vImageLookupTable_Planar8toPlanarF returns %ld\n", (long)err);
        register Pixel8bit *p8Bit = NULL;
        register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
        register jint x, y;
        for (y=0; y<h; y++)
        {
            for (x=0; x<w; x++)
            {
                *pixelsDst++ = lutdata[*pixelsSrc++];        // case 1
                //*pixelsDst++ = *(lutdata + *pixelsSrc++);    // case 2: at best ~1% better than case 1
            }
            pixelsSrc += skip;

            p8Bit = (Pixel8bit *) pixelsDst;
            p8Bit += extraBytesPerRow;
            pixelsDst = (Pixel32bit *) p8Bit;
        }
    }
}

IMAGE_SURFACE_INLINE void copy565_16bitTo555_16bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel16bit *pixelsSrc, Pixel16bit *pixelsDst, size_t extraBytesPerRow)
{
PRINT("    copy565_16bitTo555_16bit")

    register Pixel8bit *p8Bit = NULL;
    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register jint green;
    register Pixel16bit pixel;
    register jint x, y;
    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc++;

            green = ((pixel >> 5) & 63);  // rrrrrggggggbbbbb => shift 5 right = 00000rrrrrgggggg => and 63 = 0000000000gggggg
            green = ((jint) (((CGFloat) green / 63.0f) * 31.0f)) & 31; // first normalize to value between 0 and 1 and then un-normalize to 5 bit (31 = 0000000000011111)

            *pixelsDst++ = ((pixel&0xf800)>>1) | (green << 5) | (pixel&0x01f);
        }
        pixelsSrc += skip;

        p8Bit = (Pixel8bit *) pixelsDst;
        p8Bit += extraBytesPerRow;
        pixelsDst = (Pixel16bit *) p8Bit;
    }
}


IMAGE_SURFACE_INLINE void customPixelsToJava(JNIEnv *env, ImageSDOps *isdo)
{
PRINT("    customPixelsToJava")

    SurfaceDataOps *sdo = (SurfaceDataOps*)isdo;
    GET_OSXOSD_CLASS();
    DECLARE_METHOD(jm_syncToCustom, jc_OSXOffScreenSurfaceData, "syncToCustom", "()V");
    (*env)->CallVoidMethod(env, sdo->sdObject, jm_syncToCustom); // AWT_THREADING Safe (known object)
    CHECK_EXCEPTION();
}

IMAGE_SURFACE_INLINE void removeAlphaPre_32bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc)
{
PRINT("    removeAlphaPre_32bit")

    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, alpha, red, green, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            alpha        = (pixel >> 24) & 0xff;

            if (alpha != 0)
            {
                // get color components
                red            = (pixel >> 16) & 0xff;
                green        = (pixel >> 8) & 0xff;
                blue        = (pixel >> 0) & 0xff;

                // remove alpha pre
                red            = ((red * 0xff) + 0x7f) / alpha;
                green        = ((green * 0xff) + 0x7f) / alpha;
                blue        = ((blue * 0xff) + 0x7f) / alpha;

                // clamp
                red            = (red <= 0xff) ? red : 0xff;
                green        = (green <= 0xff) ? green : 0xff;
                blue        = (blue <= 0xff) ? blue : 0xff;

                *pixelsSrc++ = (alpha<<24) | (red<<16) | (green<<8) | blue; // construct new pixel
            }
            else
            {
                *pixelsSrc++ = 0;
            }
        }

        pixelsSrc += skip;
    }
}

IMAGE_SURFACE_INLINE void swapRandBAndRemoveAlphaPre_32bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc)
{
PRINT("    swapRandBAndRemoveAlphaPre_32bit")

    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, alpha, red, green, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            alpha        = (pixel & 0xff000000) >> 24;

            if (alpha != 0)
            {
                // get color components
                red            = (pixel & 0x00ff0000) >> 16;
                green        = (pixel & 0x0000ff00) >> 8;
                blue        = (pixel & 0x000000ff) >> 0;

                // remove alpha pre
                red            = ((red * 0xff) + 0x7f) / alpha;
                green        = ((green * 0xff) + 0x7f) / alpha;
                blue        = ((blue * 0xff) + 0x7f) / alpha;

                // clamp
                red            = (red <= 0xff) ? red : 0xff;
                green        = (green <= 0xff) ? green : 0xff;
                blue        = (blue <= 0xff) ? blue : 0xff;

                pixel = (alpha<<24) | (blue<<16) | (green<<8) | red; // construct new pixel

#ifdef __LITTLE_ENDIAN__
                pixel = CFSwapInt32HostToBig(pixel);  // the jint is little endian, we need to swap the bits before we send it back to Java
#endif

                *pixelsSrc++ = pixel;
            }
            else
            {
                *pixelsSrc++ = 0;
            }
        }

        pixelsSrc += skip;
    }
}

IMAGE_SURFACE_INLINE void swapRandB_32bit_TYPE_INT(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc)
{
PRINT("    swapRandB_32bit_TYPE_INT")

    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, red, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            red        = (pixel & 0x00ff0000) >> 16; // get original red and shift to new position
            blue    = (pixel & 0x000000ff) << 16; // get original blue and shift to new position

            pixel    = (pixel & 0xff00ff00); // erase original red&blue

            pixel    = pixel | red | blue; // construct new pixel

            *pixelsSrc++ = pixel;
        }

        pixelsSrc += skip;
    }
}

IMAGE_SURFACE_INLINE void swapRandB_32bit_TYPE_4BYTE(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel32bit *pixelsSrc)
{
PRINT("    swapRandB_32bit_TYPE_4BYTE")

    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register Pixel32bit pixel, red, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            red        = (pixel & 0x00ff0000) >> 16; // get original red and shift to new position
            blue    = (pixel & 0x000000ff) << 16; // get original blue and shift to new position

            pixel    = (pixel & 0xff00ff00); // erase original red&blue

            pixel    = pixel | red | blue; // construct new pixel

#ifdef __LITTLE_ENDIAN__
            pixel = CFSwapInt32HostToBig(pixel); // the jint is little endian, we need to swap the bits before we send it back to Java
#endif

            *pixelsSrc++ = pixel;
        }

        pixelsSrc += skip;
    }
}

IMAGE_SURFACE_INLINE void map555_16bitTo565_16bit(jint w, jint h, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel16bit *pixelsSrc)
{
PRINT("    map555_16bitTo565_16bit")
    register jint skip = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register jint green;
    register Pixel16bit pixel;
    register jint x, y;
    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            green = ((pixel >> 5)  & 31);   // rrrrrgggggbbbbb => shift 5 right = 000000rrrrrggggg => and 31 = 00000000000ggggg
            green = ((jint) (((CGFloat) green / 31.0f) * 63.0f)) & 63; // first normalize between 0 and 1 and then un-normalize to 6 bit (63 = 0000000000111111)

            *pixelsSrc++ = ((pixel&0x7c00)<<1) | (green << 5) | (pixel&0x01f);
        }

        pixelsSrc += skip;
    }
}

IMAGE_SURFACE_INLINE void copyARGB_PRE_32bitToBGR_24bit(jint w, jint h, jint nativePixelsBytesPerRow, Pixel32bit *pixelsSrc, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsDst)
{
PRINT("    copyARGB_PRE_32bitToBGR_24bit")

    static const jint mask = 0x000000ff;
    register jint skipSrc = (nativePixelsBytesPerRow/sizeof(Pixel32bit))-w; // in pixelsSrc units
    register jint skipDst = ((javaPixelsBytesPerRow/javaPixelBytes)-w)*javaPixelBytes; // in pixelsDst units
    register Pixel32bit pixel, alpha, red, green, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            alpha        = (pixel >> 24) & mask;

            if (alpha != 0)
            {
                // extract color components
                red            = (pixel >> 16) & mask;
                green        = (pixel >> 8) & mask;
                blue        = (pixel >> 0) & mask;

                // remove alpha pre
                red            = ((red * 0xff) + 0x7f) / alpha;
                green        = ((green * 0xff) + 0x7f) / alpha;
                blue        = ((blue * 0xff) + 0x7f) / alpha;

                // clamp
                *pixelsDst++ = (blue <= 0xff) ? blue : 0xff;
                *pixelsDst++ = (green <= 0xff) ? green : 0xff;
                *pixelsDst++ = (red <= 0xff) ? red : 0xff;
            }
            else
            {
                *pixelsDst++ = 0; // blue
                *pixelsDst++ = 0; // green
                *pixelsDst++ = 0; // red
            }

            pixelsSrc++;
        }

        pixelsSrc += skipSrc;
        pixelsDst += skipDst;
    }
}


IMAGE_SURFACE_INLINE void copyARGB_PRE_32bitToRGB_24bit(jint w, jint h, jint nativePixelsBytesPerRow, Pixel32bit *pixelsSrc, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsDst)
{
    PRINT("    copyARGB_PRE_32bitToRGB_24bit")

    static const jint mask = 0x000000ff;
    register jint skipSrc = (nativePixelsBytesPerRow/sizeof(Pixel32bit))-w; // in pixelsSrc units
    register jint skipDst = ((javaPixelsBytesPerRow/javaPixelBytes)-w)*javaPixelBytes; // in pixelsDst units
    register Pixel32bit pixel, alpha, red, green, blue;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel = *pixelsSrc;

            alpha        = (pixel >> 24) & mask;

            if (alpha != 0)
            {
                // extract color components
                red            = (pixel >> 16) & mask;
                green        = (pixel >> 8) & mask;
                blue        = (pixel >> 0) & mask;

                // remove alpha pre
                red            = ((red * 0xff) + 0x7f) / alpha;
                green        = ((green * 0xff) + 0x7f) / alpha;
                blue        = ((blue * 0xff) + 0x7f) / alpha;

                // clamp
                *pixelsDst++ = (red <= 0xff) ? red : 0xff;
                *pixelsDst++ = (green <= 0xff) ? green : 0xff;
                *pixelsDst++ = (blue <= 0xff) ? blue : 0xff;
            }
            else
            {
                *pixelsDst++ = 0; // blue
                *pixelsDst++ = 0; // green
                *pixelsDst++ = 0; // red
            }

            pixelsSrc++;
        }

        pixelsSrc += skipSrc;
        pixelsDst += skipDst;
    }
}


// gray = 0.3red + 0.59green + 0.11blue - NTSC standard (according to Luke Wallis)
IMAGE_SURFACE_INLINE void copyARGB_PRE_32bitToGray_16bit(jint w, jint h, jint nativePixelsBytesPerRow, Pixel32bit *pixelsSrc, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel16bit *pixelsDst)
{
PRINT("    copyARGB_PRE_32bitToGray_16bit")

    static const jint mask = 0x000000ff;
    register jint skipSrc = (nativePixelsBytesPerRow/sizeof(Pixel32bit))-w; // in pixelsSrc units
    register jint skipDst = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsDst units
    register Pixel32bit alpha;
    register Pixel32bit pixel, red, green, blue;
    register CGFloat pixelFloat;
    register jint x, y;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            pixel        = *pixelsSrc;

            // gznote: do we remove alpha pre here?
            alpha        = ((pixel >> 24) & mask); //extract

            if (alpha != 0)
            {
                red            = ((pixel >> 16) & mask); // extract
                green        = ((pixel >> 8) & mask); // extract
                blue        = ((pixel >> 0) & mask); // extract

                alpha        *= 0xff; // upsample to 16bit
                red            *= 0xff; // upsample to 16bit
                green        *= 0xff; // upsample to 16bit
                blue        *= 0xff; // upsample to 16bit

                red            = ((red * 0xffff) + 0x7fff) / alpha; // remove alpha pre
                red            = (red <= 0xffff) ? red : 0xffff;
                green        = ((green * 0xffff) + 0x7fff) / alpha; // remove alpha pre
                green        = (green <= 0xffff) ? green : 0xffff;
                blue        = ((blue * 0xffff) + 0x7fff) / alpha; // remove alpha pre
                blue        = (blue <= 0xffff) ? blue : 0xffff;

                pixelFloat    = red*0.3f + green*0.59f + blue*0.11f; // rgb->gray NTSC conversion
            }
            else
            {
                pixelFloat = 0;
            }

            *pixelsDst    = (jint)pixelFloat;
            pixelsDst++;

            pixelsSrc++;
        }

        pixelsSrc += skipSrc;
        pixelsDst += skipDst;
    }
}

// 1. first "dither" the true color down by creating a 16 bit value of the real color that will serve as an index into the cache of indexes
// 2. if the cache has a valid entry use it otherwise go through 3 and 4
// 3. go through the color table and calculate Euclidian distance between the true color and the indexed colors
// 4. map the shortest distance into the one and true index color and stick it into the dst (and cache)
IMAGE_SURFACE_INLINE UInt16* copyARGB_PRE_bitToIndexed_8bit(jint w, jint h, jint nativePixelsBytesPerRow, Pixel32bit *pixelsSrc, jint javaPixelsBytesPerRow, jint javaPixelBytes, Pixel8bit *pixelsDst, Pixel32bit* lutdata, UInt32 lutDataSize, UInt16 *indexedColorTable)
{
PRINT("    copyARGB_PRE_bitToIndexed_8bit")
    static const UInt32 mask            = 0x000000ff;

    static const UInt32 indexSize        = 65536;        // 2^16 - 16 bits of precision
    static const UInt32 indexMask        = 0x000000f0;    // 00000000000000000000000011110000
    static const UInt16 invalidIndex    = 0xffff;        // 1111111111111111

    register jint skipSrc = (nativePixelsBytesPerRow/sizeof(Pixel32bit))-w; // in pixelsSrc units
    register jint skipDst = (javaPixelsBytesPerRow/javaPixelBytes)-w; // in pixelsSrc units
    register jint indexOfBest, indexOfBestCached = -1;
    register CGFloat distanceOfBest, distance;
    register UInt32 p1, p1Cached = 0, p1a, p1r, p1g, p1b, p2;
    register SInt32 da, dr, dg, db;
    register jint x, y, i;
    BOOL cachedValueReady = NO;

    if (indexedColorTable == NULL)
    {
        indexedColorTable = (UInt16*)malloc(indexSize*sizeof(UInt16));    // 15 bit precision, each entry capable of holding a 2 byte value
                                                                        // (lower byte for the actual index, higher byte to mark it valid/invalid)

        if (indexedColorTable != NULL)
        {
            memset((void*)indexedColorTable, invalidIndex, indexSize*sizeof(UInt16));
        }
        else
        {
            fprintf(stderr, "ERROR: malloc returns NULL for isdo->indexedColorTable in copyARGB_PRE_bitToIndexed_8bit");
            return NULL;
        }
    }

    register UInt16 cacheIndex;

    for (y=0; y<h; y++)
    {
        for (x=0; x<w; x++)
        {
            p1 = *pixelsSrc;

            if ((p1Cached != p1) || (cachedValueReady == NO))
            {
                p1a = ((p1 >> 24) & mask);

                if (p1a != 0)
                {
                    // extract color components
                    p1r = ((p1 >> 16) & mask);
                    p1g = ((p1 >> 8) & mask);
                    p1b = ((p1 >> 0) & mask);

                    // remove alpha pre
                    p1r = ((p1r * 0xff) + 0x7f) / p1a;
                    p1g = ((p1g * 0xff) + 0x7f) / p1a;
                    p1b = ((p1b * 0xff) + 0x7f) / p1a;

                    // clamp
                    p1r = (p1r <= 0xff) ? p1r : 0xff;
                    p1g = (p1g <= 0xff) ? p1g : 0xff;
                    p1b = (p1b <= 0xff) ? p1b : 0xff;
                }
                else
                {
                    p1r = 0;
                    p1g = 0;
                    p1b = 0;
                }

                cacheIndex = (UInt16)(((p1a & indexMask) << 8) | ((p1r & indexMask) << 4) | ((p1g & indexMask) << 0) | ((p1b & indexMask) >> 4));
                if (indexedColorTable[cacheIndex] == invalidIndex)
                {
                    indexOfBest = 0;
                    distanceOfBest = DBL_MAX;

                    for (i=0; (unsigned)i<lutDataSize; i++)
                    {
                        p2 = lutdata[i];

                        da = p1a - ((p2 >> 24) & mask);
                        dr = p1r - ((p2 >> 16) & mask);
                        dg = p1g - ((p2 >> 8) & mask);
                        db = p1b - ((p2 >> 0) & mask);

                        distance = sqrt((da*da)+(dr*dr)+(dg*dg)+(db*db));
                        if (distance < distanceOfBest)
                        {
                            distanceOfBest = distance;
                            indexOfBest = i;
                        }
                    }

                    indexedColorTable[cacheIndex] = indexOfBest;
                }
                else
                {
                    indexOfBest = indexedColorTable[cacheIndex];
                }

                cachedValueReady = YES;
                p1Cached = p1;
                indexOfBestCached = indexOfBest;
            }
            else
            {
                indexOfBest = indexOfBestCached;
            }

            *pixelsDst = indexOfBest;

            pixelsDst++;
            pixelsSrc++;
        }
        pixelsSrc += skipSrc;
        pixelsDst += skipDst;
    }

    return indexedColorTable;
}

// callback from CG telling us it's done with the data. <rdar://problem/4762033>
static void releaseDataFromProvider(void *info, const void *data, size_t size)
{
    if (data != NULL)
    {
        free((void*)data);
    }
}

IMAGE_SURFACE_INLINE void createContext(JNIEnv *env, ImageSDOps *isdo)
{
PRINT("createContext")

    QuartzSDOps *qsdo = (QuartzSDOps*)isdo;
    if (qsdo->cgRef == NULL)  // lazy creation
    {
        size_t bitsPerComponent = isdo->contextInfo.bitsPerComponent;
        CGColorSpaceRef colorSpace = isdo->contextInfo.colorSpace;
        CGImageAlphaInfo alphaInfo = isdo->contextInfo.alphaInfo;

        size_t bytesPerRow = isdo->contextInfo.bytesPerRow;
        size_t size = bytesPerRow * isdo->height;
        isdo->nativePixels = malloc(size);

        if (isdo->nativePixels == NULL)
        {
            fprintf(stderr, "malloc failed for size %d bytes in ImageSurfaceData.createContext()\n", (int) size);
        }

//fprintf(stderr, "isdo=%p isdo->type=%d, bitsPerComponent=%d, bytesPerRow=%d, colorSpace=%p, alphaInfo=%d, width=%d, height=%d, size=%d\n", isdo, type, (jint)bitsPerComponent, (jint)bytesPerRow, colorSpace, (jint)alphaInfo, (jint) isdo->width, (jint) isdo->height, (jint) size);

        qsdo->cgRef = CGBitmapContextCreate(isdo->nativePixels, isdo->width, isdo->height, bitsPerComponent, bytesPerRow, colorSpace, alphaInfo);
        isdo->dataProvider = CGDataProviderCreateWithData(NULL, isdo->nativePixels, size, releaseDataFromProvider);
    }

//fprintf(stderr, "cgRef=%p\n", qsdo->cgRef);
    if (qsdo->cgRef == NULL)
    {
        fprintf(stderr, "ERROR: (qsdo->cgRef == NULL) in createContext!\n");
    }

    // intitalize the context to match the Java coordinate system

    // BG, since the context is created above, we can just concat
    CGContextConcatCTM(qsdo->cgRef, CGAffineTransformMake(1, 0, 0, -1, 0, isdo->height));

    CGContextSaveGState(qsdo->cgRef); // this will make sure we don't go pass device context settings
    CGContextSaveGState(qsdo->cgRef); // this will put user settings on top, used by LazyStateManagement code
    qsdo->newContext = YES;
}

IMAGE_SURFACE_INLINE void holdJavaPixels(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("holdJavaPixels")

    if (isdo->type != java_awt_image_BufferedImage_TYPE_CUSTOM)
    {
        Pixel8bit* pixels = NULL;
        if (isdo->nrOfPixelsOwners == 0)
        {
            pixels = (Pixel8bit*)((*env)->GetPrimitiveArrayCritical(env, isdo->array, NULL));
            if (pixels != NULL)
            {
                isdo->pixelsLocked = pixels;

                isdo->pixels = isdo->pixelsLocked + isdo->offset;
            }
            else
            {
                fprintf(stderr, "ERROR: GetPrimitiveArrayCritical returns NULL for pixels in holdJavaPixels!\n");
            }
        }
        isdo->nrOfPixelsOwners++;
    }
    else if (isdo->pixels == NULL)
    {
        isdo->pixels = (Pixel8bit*)((*env)->GetDirectBufferAddress(env, isdo->array));
    }
}

IMAGE_SURFACE_INLINE void unholdJavaPixels(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("unholdJavaPixels")

    if (isdo->type != java_awt_image_BufferedImage_TYPE_CUSTOM)
    {
        isdo->nrOfPixelsOwners--;
        if (isdo->nrOfPixelsOwners == 0)
        {
            isdo->pixels = NULL;

            (*env)->ReleasePrimitiveArrayCritical(env, isdo->array, isdo->pixelsLocked, 0); // Do not use JNI_COMMIT, as that will not free the buffer copy when +ProtectJavaHeap is on.
            isdo->pixelsLocked = NULL;
        }
    }
}

static void imageDataProvider_UnholdJavaPixels(void *info, const void *data, size_t size)
{
PRINT("imageDataProvider_UnholdJavaPixels")

    // Currently do nothing
}

static void imageDataProvider_FreeTempPixels(void *info, const void *data, size_t size)
{
PRINT("imageDataProvider_FreeTempPixels")

    free((void *)data);
}
IMAGE_SURFACE_INLINE void syncFromJavaPixels(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("syncFromJavaPixels")

    // check to see if we have any work to do
    if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] == 1)
    {
        // if we do, lock down Java pixels, this halts GarbageCollector!
        holdJavaPixels(env, isdo);
        if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] == 1)
        {
            isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 0;

            void *dataProviderData = NULL;
            void *dataProviderInfo = NULL;
            void *dataProviderCallback = NULL;
            size_t dataProviderDataSize = 0;
            size_t width = isdo->width;
            size_t height = isdo->height;
            size_t bitsPerComponent = isdo->imageInfo.bitsPerComponent;
            size_t bitsPerPixel = isdo->imageInfo.bitsPerPixel;
            size_t bytesPerRow = 0;
            size_t extraBytesPerRow = 0; // these are the extra bytesPerRow used for alignement

            switch (isdo->type)
            {
                //case java_awt_image_BufferedImage_TYPE_BYTE_BINARY: // mapped to TYPE_CUSTOM
                case java_awt_image_BufferedImage_TYPE_CUSTOM:
                    holdJavaPixels(env, isdo);    // we lock again since we are reusing pixels, but we must ensure CGImageRef immutability
                                                // we can lock these pixels down because they are nio based, so we don't halt the GarbageCollector
                    bytesPerRow = isdo->javaPixelsBytesPerRow;
                    dataProviderDataSize = bytesPerRow*isdo->height;
                    dataProviderData = isdo->pixels;
                    dataProviderInfo = isdo;
                    dataProviderCallback = imageDataProvider_UnholdJavaPixels;
                    break;
                default:
                    bytesPerRow = isdo->imageInfo.bytesPerRow;
                    dataProviderDataSize = bytesPerRow*height;
                    dataProviderData = malloc(dataProviderDataSize);
                    dataProviderInfo = isdo;
                    dataProviderCallback = imageDataProvider_FreeTempPixels;
            }

            switch (isdo->type)
            {
                //case java_awt_image_BufferedImage_TYPE_BYTE_BINARY: // mapped to TYPE_CUSTOM
                case java_awt_image_BufferedImage_TYPE_CUSTOM:
                    customPixelsFromJava(env, isdo);
                    break;
                case java_awt_image_BufferedImage_TYPE_INT_RGB:
                case java_awt_image_BufferedImage_TYPE_INT_ARGB:
                case java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE:
                case java_awt_image_BufferedImage_TYPE_USHORT_555_RGB:
                case java_awt_image_BufferedImage_TYPE_USHORT_GRAY:
                case java_awt_image_BufferedImage_TYPE_BYTE_GRAY:
                    copyBits(width, height, isdo->javaPixelsBytesPerRow, (Pixel8bit*)isdo->pixels, bytesPerRow, dataProviderData);
                    break;
                case java_awt_image_BufferedImage_TYPE_INT_BGR:
                    copySwapRandB_32bit_TYPE_INT(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels, dataProviderData, extraBytesPerRow);
                    break;
                case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR:
                case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR_PRE:
                    copySwapRandB_32bit_TYPE_4BYTE(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels, dataProviderData, extraBytesPerRow);
                    break;
                case java_awt_image_BufferedImage_TYPE_3BYTE_BGR:
                    copyBGR_24bitToXRGB_32bit(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels, dataProviderData, extraBytesPerRow);
                    break;
                case sun_java2d_OSXOffScreenSurfaceData_TYPE_3BYTE_RGB:
                    copyRGB_24bitToXRGB_32bit(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels, dataProviderData, extraBytesPerRow);
                    break;
                case java_awt_image_BufferedImage_TYPE_USHORT_565_RGB:
                    copy565_16bitTo555_16bit(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel16bit*)isdo->pixels, dataProviderData, extraBytesPerRow);
                    break;
                case java_awt_image_BufferedImage_TYPE_BYTE_INDEXED:
                    copyIndexed_8bitToARGB_32bit(width, height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels, isdo->lutData, dataProviderData, extraBytesPerRow);
                    break;
                default:
                    break;
            }

            CGDataProviderRef provider = CGDataProviderCreateWithData(dataProviderInfo, dataProviderData, dataProviderDataSize, dataProviderCallback);
            CGImageRef javaImg = CGImageCreate(width, height, bitsPerComponent, bitsPerPixel, bytesPerRow,
                                                isdo->imageInfo.colorSpace, isdo->imageInfo.alphaInfo, provider, NULL, NO, kCGRenderingIntentDefault);
//fprintf(stderr, "javaImg=%p\n", javaImg);
            CGDataProviderRelease(provider);

            if (javaImg != NULL)
            {
                QuartzSDOps *qsdo = (QuartzSDOps*)isdo;

                if (isdo->imgRef != NULL)
                {
                    CGImageRelease(isdo->imgRef);
                    isdo->imgRef = NULL;
                }

                if (qsdo->cgRef == NULL)
                {
                    createContext(env, isdo);
                }

                if (qsdo->cgRef != NULL)
                {
                    CGContextSaveGState(qsdo->cgRef);
                    CGAffineTransform currCTM = CGContextGetCTM(qsdo->cgRef);
                    CGAffineTransform inverse = CGAffineTransformInvert(currCTM);
                    CGContextConcatCTM(qsdo->cgRef, inverse);
                    CGContextConcatCTM(qsdo->cgRef, CGAffineTransformMake(1, 0, 0, 1, 0, 0));
                    CGContextSetBlendMode(qsdo->cgRef, kCGBlendModeCopy);
                    CGContextSetAlpha(qsdo->cgRef, 1.0f);
                    CGContextDrawImage(qsdo->cgRef, CGRectMake(0, 0, width, height), javaImg);
                    CGContextFlush(qsdo->cgRef);
                    CGContextRestoreGState(qsdo->cgRef);
                    CGImageRelease(javaImg);
                }
                else
                {
                    fprintf(stderr, "ERROR: (cgRef == NULL) in syncFromJavaPixels!\n");
                }
            }
            else
            {
//fprintf(stderr, "isdo->type=%d, isdo->width=%d, isdo->height=%d, isdo->imageInfo.bitsPerComponent=%d, isdo->imageInfo.bytesPerPixel=%d, isdo->imageInfo.bitsPerPixel=%d, isdo->imageInfo.bytesPerRow=%d, isdo->imageInfo.colorSpace=%p, isdo->imageInfo.alphaInfo=%d\n",
//(jint)isdo->type, (jint)isdo->width, (jint)isdo->height, (jint)isdo->imageInfo.bitsPerComponent, (jint)isdo->imageInfo.bytesPerPixel, (jint)isdo->imageInfo.bitsPerPixel, (jint)isdo->imageInfo.bytesPerRow, isdo->imageInfo.colorSpace, (jint)isdo->imageInfo.alphaInfo);
                fprintf(stderr, "ERROR: (javaImg == NULL) in syncFromJavaPixels!\n");
            }
        }

        unholdJavaPixels(env, isdo);
    }
}

IMAGE_SURFACE_INLINE void processPixels(ImageSDOps* isdo, jint x, jint y, jint width, jint height, void (*processPixelsCallback) (ImageSDOps *, jint, Pixel32bit *, jint, jint, jint, jint))
{
    processPixelsCallback(isdo, (jint) isdo->contextInfo.bytesPerRow, (Pixel32bit *) isdo->nativePixels, x, y, width, height);
}

IMAGE_SURFACE_INLINE void syncToJavaPixels_processPixelsCallback(ImageSDOps* isdo, jint nativePixelsBytesPerRow, Pixel32bit *dataSrc, jint x, jint y, jint width, jint height)
{
    switch (isdo->type)
    {
        case java_awt_image_BufferedImage_TYPE_3BYTE_BGR:
            copyARGB_PRE_32bitToBGR_24bit(isdo->width, isdo->height, nativePixelsBytesPerRow, dataSrc, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels);
            break;
        case sun_java2d_OSXOffScreenSurfaceData_TYPE_3BYTE_RGB:
            copyARGB_PRE_32bitToRGB_24bit(isdo->width, isdo->height, nativePixelsBytesPerRow, dataSrc, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels);
            break;
        case java_awt_image_BufferedImage_TYPE_USHORT_GRAY:
            copyARGB_PRE_32bitToGray_16bit(isdo->width, isdo->height, nativePixelsBytesPerRow, dataSrc, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel16bit*)isdo->pixels);
            break;
        case java_awt_image_BufferedImage_TYPE_BYTE_INDEXED:
            isdo->indexedColorTable = copyARGB_PRE_bitToIndexed_8bit(isdo->width, isdo->height, nativePixelsBytesPerRow, dataSrc, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, isdo->pixels, isdo->lutData, isdo->lutDataSize, isdo->indexedColorTable);
            break;
        default:
            break;
    }
}


IMAGE_SURFACE_INLINE void syncToJavaPixels(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("syncToJavaPixels")

    holdJavaPixels(env, isdo);

    QuartzSDOps *qsdo = (QuartzSDOps*)isdo;
    if (qsdo->cgRef == NULL)
    {
        createContext(env, isdo);
    }

    isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNativePixelsChangedIndex] = 0;

    if (isdo->contextInfo.canUseJavaPixelsAsContext == YES)
    {

        jint srcBytesPerRow = isdo->contextInfo.bytesPerRow;
        jint dstBytesPerRow = isdo->javaPixelsBytesPerRow;
        jint h = isdo->height;
        Pixel8bit *pixelsSrc = isdo->nativePixels;
        Pixel8bit *pixelsDst = isdo->pixels;

        if (srcBytesPerRow == dstBytesPerRow)
        {
            memcpy(pixelsDst, pixelsSrc, h * dstBytesPerRow);
        }
        else
        {
            jint widthInBytes = isdo->width * isdo->contextInfo.bytesPerPixel;
            jint y;
            for (y=0; y < h; y++)
            {
                memcpy(pixelsDst, pixelsSrc, widthInBytes);

                pixelsSrc += srcBytesPerRow;
                pixelsDst += dstBytesPerRow;
            }
        }

        switch (isdo->type)
        {
            //case java_awt_image_BufferedImage_TYPE_BYTE_BINARY: // mapped to TYPE_CUSTOM
            case java_awt_image_BufferedImage_TYPE_CUSTOM:
                customPixelsToJava(env, isdo);
                break;
            case java_awt_image_BufferedImage_TYPE_INT_ARGB:
                removeAlphaPre_32bit(isdo->width, isdo->height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels);
                break;
            case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR:
                swapRandBAndRemoveAlphaPre_32bit(isdo->width, isdo->height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels);
                break;
            case java_awt_image_BufferedImage_TYPE_INT_BGR:
                swapRandB_32bit_TYPE_INT(isdo->width, isdo->height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels);
                break;
            case java_awt_image_BufferedImage_TYPE_4BYTE_ABGR_PRE:
                swapRandB_32bit_TYPE_4BYTE(isdo->width, isdo->height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel32bit*)isdo->pixels);
                break;
            case java_awt_image_BufferedImage_TYPE_USHORT_565_RGB:
                map555_16bitTo565_16bit(isdo->width, isdo->height, isdo->javaPixelsBytesPerRow, isdo->javaPixelBytes, (Pixel16bit*)isdo->pixels);
                break;
            default:
                break;
        }
    }
    else
    {
        processPixels(isdo, 0, 0, isdo->width, isdo->height, &syncToJavaPixels_processPixelsCallback);
    }

    unholdJavaPixels(env, isdo);
}


IMAGE_SURFACE_INLINE jboolean xorSurfacePixels(JNIEnv *env, jobject dstIsd, jobject srcIsd, jint colorXOR, jint x, jint y, jint w, jint h)
{
PRINT("xorSurfacePixels")

    jboolean handled = JNI_FALSE;

JNI_COCOA_ENTER(env);
    ImageSDOps* srcIsdo = LockImagePixels(env, srcIsd);
    ImageSDOps* dstIsdo = LockImagePixels(env, dstIsd);

    if ((x < 0) || (y < 0) || (x+w > dstIsdo->width) || (y+h > dstIsdo->height) || (w > srcIsdo->width) || (h > srcIsdo->height))
    {
#ifdef PRINT_WARNINGS
fprintf(stderr, "xorSurfacePixels INVALID parameters: x=%d, y=%d, w=%d, h=%d\n", x, y, w, h);
fprintf(stderr, "   dstIsdo->width=%d, dstIsdo->height=%d, biqsdoPixels->width=%d, biqsdoPixels->height=%d\n",
                        dstIsdo->width, dstIsdo->height, srcIsdo->width, srcIsdo->height);
#endif
        UnlockImagePixels(env, srcIsdo);
        UnlockImagePixels(env, dstIsdo);

        return JNI_FALSE;
    }

    jint offset = (dstIsdo->width*y)+x;
    register Pixel32bit* dstPixels = (Pixel32bit*)dstIsdo->pixels;
    register jint skip = dstIsdo->width - w;
    register Pixel32bit* srcPixels = (Pixel32bit*)srcIsdo->pixels;
    register jint skipPixels = srcIsdo->width - w;
    register jint i, j;

    dstPixels += offset;

    switch (dstIsdo->type)
    {
        case java_awt_image_BufferedImage_TYPE_INT_RGB:
        case java_awt_image_BufferedImage_TYPE_INT_ARGB:
        case java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE:
        {
            dstIsdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;

            if (dstIsdo->type == java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE)
            {
                Pixel8bit alpha = (colorXOR>>24)&0xff;
                Pixel8bit red = (colorXOR>>16)&0xff;
                red = (jint)(((CGFloat)red/255.0f * (CGFloat)alpha/255.0f)*255.0f);
                Pixel8bit green = (colorXOR>>8)&0xff;
                green = (jint)(((CGFloat)green/255.0f * (CGFloat)alpha/255.0f)*255.0f);
                Pixel8bit blue = (colorXOR>>0)&0xff;
                blue = (jint)(((CGFloat)blue/255.0f * (CGFloat)alpha/255.0f)*255.0f);
                colorXOR = (alpha<<24) | (red<<16) | (green<<8) | blue; // the color is now alpha premultiplied
            }

            for (i=0; i<h; i++)
            {
                for (j=0; j<w; j++)
                {
                    Pixel32bit srcPixel = *srcPixels;
                    Pixel8bit pixelAlpha = (srcPixel>>24);
                    if (pixelAlpha > XOR_ALPHA_CUTOFF)
                    {
                        *dstPixels = (*dstPixels ^ (srcPixel ^ colorXOR));
                    }
                    dstPixels++; srcPixels++;
                }

                dstPixels += skip;
                srcPixels += skipPixels;
            }

            handled = JNI_TRUE;
            break;
        }
        default:
        {
            handled = JNI_FALSE;
#if defined(PRINT_WARNINGS)
            fprintf(stderr, "WARNING: unknown type (%d) in compositeXOR\n", dstIsdo->type);
            PrintImageInfo(dstIsdo);
#endif
        }
    }

    UnlockImagePixels(env, srcIsdo);
    UnlockImagePixels(env, dstIsdo);

JNI_COCOA_EXIT(env);
    return handled;
}

IMAGE_SURFACE_INLINE jboolean clearSurfacePixels(JNIEnv *env, jobject bisd, jint w, jint h)
{
PRINT("clearSurfacePixels")
    jboolean handled = JNI_FALSE;

JNI_COCOA_ENTER(env);

    ImageSDOps *isdo = LockImagePixels(env, bisd);

    if (isdo->type == java_awt_image_BufferedImage_TYPE_INT_ARGB_PRE)
    {
        isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;

        w = (w < isdo->width) ? w : isdo->width;
        h = (h < isdo->height) ? h : isdo->height;

        register Pixel32bit* data = (Pixel32bit*)isdo->pixels;
        register jint i;
        if ((w < isdo->width) || (h < isdo->height)) //cmcnote: necessary to special-case for small height? wouldn't 4*w*h do it?
        {
            register jint skip = isdo->width;
            register jint row = 4*w;
            for (i=0; i<h; i++)
            {
                bzero(data, row);
                data += skip;
            }
        }
        else
        {
            bzero(data, 4*w*h);
        }

        handled = JNI_TRUE;
    }
    UnlockImagePixels(env, isdo);

JNI_COCOA_EXIT(env);

    return handled;
}

static void ImageSD_startCGContext(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType)
{
PRINT("ImageSD_startCGContext")

    ImageSDOps *isdo = (ImageSDOps*)qsdo;

    pthread_mutex_lock(&isdo->lock);

    if (isdo->imgRef != NULL)
    {
        CGImageRelease(isdo->imgRef);
        isdo->imgRef = NULL;
    }

    if (qsdo->cgRef == NULL)
    {
        createContext(env, isdo);
    }
    else
    {
        qsdo->newContext = NO;
    }

    if (qsdo->cgRef != NULL)
    {
        if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kImageStolenIndex] == 1)
        {
            isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;
        }

        // sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex can be set right above or somewhere else
        if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] == 1)
        {
            syncFromJavaPixels(env, isdo);
        }

        isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNativePixelsChangedIndex] = 1;

        SetUpCGContext(env, qsdo, renderType);
    }
}
static void ImageSD_finishCGContext(JNIEnv *env, QuartzSDOps *qsdo)
{
PRINT("ImageSD_finishCGContext")

    ImageSDOps *isdo = (ImageSDOps*)qsdo;

    if (qsdo->cgRef != NULL)
    {
        CompleteCGContext(env, qsdo);

        if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kImageStolenIndex] == 1)
        {
            syncToJavaPixels(env, isdo);
            isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;
        }
    }

    pthread_mutex_unlock(&isdo->lock);
}

static void ImageSD_dispose(JNIEnv *env, SurfaceDataOps *ops)
{
PRINT("ImageSD_dispose")

    // copied from BufImg_Dispose in BufImgSurfaceData.c
    {
        /* ops is assumed non-null as it is checked in SurfaceData_DisposeOps */
        BufImgSDOps *bisdo = (BufImgSDOps *)ops;
        (*env)->DeleteWeakGlobalRef(env, bisdo->array);
        if (bisdo->lutarray != NULL) {
        (*env)->DeleteWeakGlobalRef(env, bisdo->lutarray);
        }
        if (bisdo->icm != NULL) {
        (*env)->DeleteWeakGlobalRef(env, bisdo->icm);
        }
    }

    QuartzSDOps *qsdo = (QuartzSDOps *)ops;

    if (qsdo->graphicsStateInfo.batchedLines != NULL)
    {
        free(qsdo->graphicsStateInfo.batchedLines);
        qsdo->graphicsStateInfo.batchedLines = NULL;
    }

    (*env)->DeleteGlobalRef(env, qsdo->javaGraphicsStatesObjects);

    if (qsdo->cgRef != NULL)
    {
        CGContextRelease(qsdo->cgRef);
        qsdo->cgRef = NULL;
    }

    ImageSDOps *isdo = (ImageSDOps *)ops;

    if (isdo->dataProvider != NULL)
    {
        CGDataProviderRelease(isdo->dataProvider);
        isdo->dataProvider = NULL;
    }
    if (isdo->imgRef != NULL)
    {
        CGImageRelease(isdo->imgRef);
        isdo->imgRef = NULL;
    }
    if (isdo->indexedColorTable != NULL)
    {
        free(isdo->indexedColorTable);
        isdo->indexedColorTable = NULL;
    }
    if (isdo->lutData != NULL)
    {
        free(isdo->lutData);
        isdo->indexedColorTable = NULL;
    }
    if (isdo->array != NULL)
    {
        (*env)->DeleteGlobalRef(env, isdo->array);
        isdo->array = NULL;
    }
    if (isdo->icm != NULL)
    {
        (*env)->DeleteGlobalRef(env, isdo->icm);
        isdo->icm = NULL;
    }

    if (isdo->nsRef) {
        [isdo->nsRef release];
        isdo->nsRef = nil;
    }

    pthread_mutex_destroy(&isdo->lock);
}

// used by XOR (Java pixels must be up to date)
ImageSDOps* LockImagePixels(JNIEnv* env, jobject imageSurfaceData)
{
PRINT("LockImagePixels")

    ImageSDOps* isdo = (ImageSDOps*)SurfaceData_GetOps(env, imageSurfaceData);

    pthread_mutex_lock(&isdo->lock);

    holdJavaPixels(env, isdo);

    // if we need to access this image's pixels we need to convert native pixels (if any) back to Java
    if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNativePixelsChangedIndex] == 1)
    {
        syncToJavaPixels(env, isdo);
        isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;
    }

    return isdo;
}
void UnlockImagePixels(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("UnlockImagePixels")
    // don't do that since the native pixels haven't changed (Java pixels == native pixels)
    //syncToJavaPixels(env, isdo);

    unholdJavaPixels(env, isdo);

    pthread_mutex_unlock(&isdo->lock);
}

// used by drawImage (native pixels must be up to date)
ImageSDOps* LockImage(JNIEnv* env, jobject imageSurfaceData)
{
PRINT("LockImage")

    ImageSDOps* isdo = (ImageSDOps*)SurfaceData_GetOps(env, imageSurfaceData);

    pthread_mutex_lock(&isdo->lock);

    // if we need to access this image's pixels we need to convert native pixels (if any) back to Java
    // for those images whose context type doesn't match layer type or is a custom image
    if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kImageStolenIndex] == 1)
    {
        isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;
    }

    // sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex can be set right above or somewhere else
    if (isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] == 1)
    {
        syncFromJavaPixels(env, isdo);
    }

    return isdo;
}
void UnlockImage(JNIEnv* env, ImageSDOps* isdo)
{
PRINT("UnlockImage")

    // don't do that since the native pixels haven't changed (Java pixels == native pixels)
    //syncToJavaPixels(env, isdo);

    pthread_mutex_unlock(&isdo->lock);
}

JNIEXPORT jobject JNICALL Java_sun_awt_image_BufImgSurfaceData_getSurfaceData
    (JNIEnv *env, jclass bisd, jobject bufImg)
{
    static jfieldID sDataID = 0;
    if (sDataID == 0)
    {
        static char *bimgName = "java/awt/image/BufferedImage";
        jclass bimg = (*env)->FindClass(env, bimgName);
        CHECK_NULL_RETURN(bimg, NULL);
        sDataID = (*env)->GetFieldID(env, bimg, "sData", "Lsun/java2d/SurfaceData;");
        CHECK_NULL_RETURN(sDataID, NULL);
    }

    return (*env)->GetObjectField(env, bufImg, sDataID);
}

JNIEXPORT void JNICALL Java_sun_awt_image_BufImgSurfaceData_setSurfaceData
    (JNIEnv *env, jclass bisd, jobject bufImg, jobject sData)
{
    static jfieldID sDataID = 0;
    if (sDataID == 0)
    {
        static char *bimgName = "java/awt/image/BufferedImage";
        jclass bimg = (*env)->FindClass(env, bimgName);
        CHECK_NULL(bimg);
        sDataID = (*env)->GetFieldID(env, bimg, "sData", "Lsun/java2d/SurfaceData;");
        CHECK_NULL(sDataID);
    }

    (*env)->SetObjectField(env, bufImg, sDataID, sData);
}

JNIEXPORT void JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_initIDs(JNIEnv *env, jclass bisd)
{
//PRINT("initIDs")
    // copied from Java_sun_awt_image_BufImgSurfaceData_initIDs in BufImgSurfaceData.c
    {
        static char *icmName = "java/awt/image/IndexColorModel";
        jclass icm;

        if (sizeof(BufImgRIPrivate) > SD_RASINFO_PRIVATE_SIZE) {
        JNU_ThrowInternalError(env, "Private RasInfo structure too large!");
        return;
        }

        CHECK_NULL(icm = (*env)->FindClass(env, icmName));
        CHECK_NULL(rgbID = (*env)->GetFieldID(env, icm, "rgb", "[I"));
        CHECK_NULL(allGrayID = (*env)->GetFieldID(env, icm, "allgrayopaque", "Z"));
        CHECK_NULL(mapSizeID = (*env)->GetFieldID(env, icm, "map_size", "I"));
        CHECK_NULL(CMpDataID = (*env)->GetFieldID(env, icm, "pData", "J"));
    }

    gColorspaceRGB = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    gColorspaceGray = CGColorSpaceCreateWithName(kCGColorSpaceGenericGray);
//fprintf(stderr, "gColorspaceRGB=%p, gColorspaceGray=%p\n", gColorspaceRGB, gColorspaceGray);
}

static jclass jc_BufferedImage = NULL;
static jfieldID jm_SurfaceData = NULL;

JNIEXPORT jobject JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_getSurfaceData
    (JNIEnv *env, jclass bisd, jobject bufImg)
{
PRINT("getSurfaceData")
    GET_CLASS_RETURN(jc_BufferedImage, "java/awt/image/BufferedImage", NULL);
    GET_FIELD_RETURN(jm_SurfaceData, jc_BufferedImage, "sData", "Lsun/java2d/SurfaceData;", NULL);
    return (*env)->GetObjectField(env, bufImg, jm_SurfaceData);
}

JNIEXPORT void JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_setSurfaceData
    (JNIEnv *env, jclass bisd, jobject bufImg, jobject sData)
{
PRINT("setSurfaceData")

    GET_CLASS(jc_BufferedImage, "java/awt/image/BufferedImage");
    GET_FIELD(jm_SurfaceData, jc_BufferedImage, "sData", "Lsun/java2d/SurfaceData;");
    (*env)->SetObjectField(env, bufImg, jm_SurfaceData, sData);
}

static jint ImageSD_Lock(JNIEnv *env, SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo, jint lockflags)
{
    ImageSDOps *isdo = (ImageSDOps*)ops;
    pthread_mutex_lock(&isdo->lock);

    // copied from BufImg_Lock in BufImgSurfaceData.c
    {
        BufImgSDOps *bisdo = (BufImgSDOps *)ops;
        BufImgRIPrivate *bipriv = (BufImgRIPrivate *) &(pRasInfo->priv);

        if ((lockflags & (SD_LOCK_LUT)) != 0 && !bisdo->lutarray) {
            /* REMIND: Should this be an InvalidPipe exception? */
            JNU_ThrowNullPointerException(env, "Attempt to lock missing colormap");
            return SD_FAILURE;
        }
// TODO:BG
        /*
        if ((lockflags & SD_LOCK_INVCOLOR) != 0 ||
            (lockflags & SD_LOCK_INVGRAY) != 0)
        {
            bipriv->cData = BufImg_SetupICM(env, bisdo);
            if (bipriv->cData == NULL) {
                JNU_ThrowNullPointerException(env, "Could not initialize "
                                              "inverse tables");
                return SD_FAILURE;
            }
        } else {
            bipriv->cData = NULL;
        }
        */
        bipriv->cData = NULL;

        bipriv->lockFlags = lockflags;
        bipriv->base = NULL;
        bipriv->lutbase = NULL;

        SurfaceData_IntersectBounds(&pRasInfo->bounds, &bisdo->rasbounds);

        /* TODO:BG
        if ((bipriv->lockFlags & SD_LOCK_WRITE) &&
            bisdo->sdOps.dirty != TRUE) {
            SurfaceData_MarkDirty(env, &bisdo->sdOps);
        } */
        return SD_SUCCESS;
    }
}
static void ImageSD_Unlock(JNIEnv *env, SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo)
{
    ImageSDOps *isdo = (ImageSDOps*)ops;

    // For every ImageSD_Unlock, we need to be conservative and mark the pixels
    // as modified by the Sun2D renderer.
    isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kNeedToSyncFromJavaPixelsIndex] = 1;

    pthread_mutex_unlock(&isdo->lock);
}
static void ImageSD_GetRasInfo(JNIEnv *env, SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo)
{
    // copied from BufImg_GetRasInfo in BufImgSurfaceData.c
    {
        BufImgSDOps *bisdo = (BufImgSDOps *)ops;
        BufImgRIPrivate *bipriv = (BufImgRIPrivate *) &(pRasInfo->priv);

        if ((bipriv->lockFlags & (SD_LOCK_RD_WR)) != 0) {
            bipriv->base =
                (*env)->GetPrimitiveArrayCritical(env, bisdo->array, NULL);
        }
        if ((bipriv->lockFlags & (SD_LOCK_LUT)) != 0) {
            bipriv->lutbase =
                (*env)->GetPrimitiveArrayCritical(env, bisdo->lutarray, NULL);
        }

        if (bipriv->base == NULL) {
            pRasInfo->rasBase = NULL;
            pRasInfo->pixelStride = 0;
            pRasInfo->scanStride = 0;
        } else {
            pRasInfo->rasBase = (void *)
                (((uintptr_t) bipriv->base) + bisdo->offset);
            pRasInfo->pixelStride = bisdo->pixStr;
            pRasInfo->scanStride = bisdo->scanStr;
        }
        if (bipriv->lutbase == NULL) {
            pRasInfo->lutBase = NULL;
            pRasInfo->lutSize = 0;
        } else {
            pRasInfo->lutBase = bipriv->lutbase;
            pRasInfo->lutSize = bisdo->lutsize;
        }
        if (bipriv->cData == NULL) {
            pRasInfo->invColorTable = NULL;
            pRasInfo->redErrTable = NULL;
            pRasInfo->grnErrTable = NULL;
            pRasInfo->bluErrTable = NULL;
        } else {
            pRasInfo->invColorTable = bipriv->cData->img_clr_tbl;
            pRasInfo->redErrTable = bipriv->cData->img_oda_red;
            pRasInfo->grnErrTable = bipriv->cData->img_oda_green;
            pRasInfo->bluErrTable = bipriv->cData->img_oda_blue;
            pRasInfo->invGrayTable = bipriv->cData->pGrayInverseLutData;
        }
    }
}
static void ImageSD_Release(JNIEnv *env, SurfaceDataOps *ops, SurfaceDataRasInfo *pRasInfo)
{
    // copied from BufImg_Release in BufImgSurfaceData.c
    {
        BufImgSDOps *bisdo = (BufImgSDOps *)ops;
        BufImgRIPrivate *bipriv = (BufImgRIPrivate *) &(pRasInfo->priv);

        if (bipriv->base != NULL) {
            jint mode = (((bipriv->lockFlags & (SD_LOCK_WRITE)) != 0)
                         ? 0 : JNI_ABORT);
            (*env)->ReleasePrimitiveArrayCritical(env, bisdo->array,
                                                  bipriv->base, mode);
        }
        if (bipriv->lutbase != NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, bisdo->lutarray,
                                                  bipriv->lutbase, JNI_ABORT);
        }
    }
}

JNIEXPORT void JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_initRaster(JNIEnv *env, jobject bisd, jobject array, jint offset, jint width, jint height,
                                                                                jint pixelStride, jint scanStride, jobject icm, jint type,
                                                                                    jobject jGraphicsState, jobjectArray jGraphicsStateObject, jobject jImageInfo)
{
PRINT("Java_sun_java2d_OSXOffScreenSurfaceData_initRaster")

    ImageSDOps* isdo = (ImageSDOps*)SurfaceData_InitOps(env, bisd, sizeof(ImageSDOps));
    if (isdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }

    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&isdo->lock, &attr);
    pthread_mutex_lock(&isdo->lock);
    pthread_mutexattr_destroy(&attr);

    // copied (and modified) from Java_sun_awt_image_BufImgSurfaceData_initRaster in BufImgSurfaceData.c
    {
        BufImgSDOps *bisdo =
        //(BufImgSDOps*)SurfaceData_InitOps(env, bisd, sizeof(BufImgSDOps));
        (BufImgSDOps*)isdo;
        //bisdo->sdOps.Lock = BufImg_Lock;
        //bisdo->sdOps.GetRasInfo = BufImg_GetRasInfo;
        //bisdo->sdOps.Release = BufImg_Release;
        //bisdo->sdOps.Unlock = NULL;
        //bisdo->sdOps.Dispose = BufImg_Dispose;

        bisdo->array = (*env)->NewWeakGlobalRef(env, array);
        if (array != NULL) CHECK_NULL(bisdo->array);
        bisdo->offset = offset;
        //bisdo->scanStr = scanStr;
        bisdo->scanStr = scanStride;
        //bisdo->pixStr = pixStr;
        bisdo->pixStr = pixelStride;
        if (!icm) {
            bisdo->lutarray = NULL;
            bisdo->lutsize = 0;
            bisdo->icm = NULL;
        } else {
            jobject lutarray = (*env)->GetObjectField(env, icm, rgbID);
            bisdo->lutarray = (*env)->NewWeakGlobalRef(env, lutarray);
            if (lutarray != NULL) CHECK_NULL(bisdo->lutarray);
            bisdo->lutsize = (*env)->GetIntField(env, icm, mapSizeID);
            bisdo->icm = (*env)->NewWeakGlobalRef(env, icm);
            if (icm != NULL) CHECK_NULL(bisdo->icm);
        }
        bisdo->rasbounds.x1 = 0;
        bisdo->rasbounds.y1 = 0;
        bisdo->rasbounds.x2 = width;
        bisdo->rasbounds.y2 = height;
    }

    isdo->nrOfPixelsOwners = 0;

    isdo->contextInfo                    = sDefaultContextInfo[type];
    isdo->imageInfo                        = sDefaultImageInfo[type];

    isdo->contextInfo.bytesPerRow        = width*isdo->contextInfo.bytesPerPixel;
    isdo->imageInfo.bytesPerRow            = width*isdo->imageInfo.bytesPerPixel;

    switch (type)
    {
        case java_awt_image_BufferedImage_TYPE_BYTE_GRAY:
            isdo->contextInfo.colorSpace = isdo->imageInfo.colorSpace = gColorspaceGray;
            break;
        case java_awt_image_BufferedImage_TYPE_USHORT_GRAY:
            isdo->contextInfo.colorSpace = gColorspaceRGB;
            isdo->imageInfo.colorSpace = gColorspaceGray;
            break;
        default:
            isdo->contextInfo.colorSpace = isdo->imageInfo.colorSpace = gColorspaceRGB;
            break;
    }
    isdo->isSubImage                    = (offset%scanStride != 0) || (scanStride != (pixelStride*width));

    // parameters specifying this image given to us from Java
    isdo->javaImageInfo                    = (jint*)((*env)->GetDirectBufferAddress(env, jImageInfo));
    isdo->array                            = (array != NULL) ? (*env)->NewGlobalRef(env, array) : NULL;
    isdo->offset                        = offset;
    isdo->width                            = width;
    isdo->height                        = height;
    isdo->javaPixelBytes                = pixelStride;
    isdo->javaPixelsBytesPerRow            = scanStride;
    isdo->icm                            = (icm != NULL) ? (*env)->NewGlobalRef(env, icm) : NULL;
    isdo->type                            = type;

    if ((isdo->javaImageInfo[sun_java2d_OSXOffScreenSurfaceData_kImageStolenIndex] == 1) ||
        (isdo->type == java_awt_image_BufferedImage_TYPE_CUSTOM))
    {
        // don't waste (precious, precious) VRAM on stolen or custom images that will be slow no matter what
        isdo->contextInfo.useWindowContextReference = NO;
    }

    // needed by TYPE_BYTE_INDEXED
    isdo->indexedColorTable                = NULL;
    isdo->lutData                        = NULL;
    isdo->lutDataSize                    = 0;
    if ((type == java_awt_image_BufferedImage_TYPE_BYTE_INDEXED) && ((*env)->IsSameObject(env, icm, NULL) == NO))
    {
        static jclass jc_IndexColorModel = NULL;
        if (jc_IndexColorModel == NULL) {
            jc_IndexColorModel = (*env)->FindClass(env, "java/awt/image/IndexColorModel");
        }
        CHECK_NULL(jc_IndexColorModel);
        DECLARE_FIELD(jm_rgb, jc_IndexColorModel, "rgb", "[I");
        DECLARE_FIELD(jm_transparency, jc_IndexColorModel, "transparency", "I");
        DECLARE_FIELD(jm_transparent_index, jc_IndexColorModel, "transparent_index", "I");

        jarray lutarray = (*env)->GetObjectField(env, icm, jm_rgb);
        isdo->lutDataSize = (*env)->GetArrayLength(env, lutarray);
        if (isdo->lutDataSize > 0)
        {
            jint transparency = (*env)->GetIntField(env, icm, jm_transparency);
            jint transparent_index = -1;
            if (transparency == java_awt_Transparency_BITMASK)
            {
                transparent_index = (*env)->GetIntField(env, icm, jm_transparent_index);
            }

            Pixel32bit* lutdata = (Pixel32bit*)((*env)->GetPrimitiveArrayCritical(env, lutarray, NULL));
            if (lutdata != NULL)
            {
                isdo->lutData = NULL;

                isdo->lutData = malloc(isdo->lutDataSize * sizeof(Pixel32bit));
                if (isdo->lutData != NULL)
                {
                    if (transparency == java_awt_Transparency_BITMASK)
                    {
                        Pixel32bit* src = lutdata;
                        Pixel32bit* dst = isdo->lutData;
                        jint i;
                        for (i=0; (unsigned)i<isdo->lutDataSize; i++)
                        {
                            if (i != transparent_index)
                            {
                                *dst = *src;
                                // rdar://problem/3390518 - don't force all indexed colors
                                // to be fully opaque. They could be set up for us.
                                // we used to call:  *dst = 0xff000000 | *src;
                                // but that was forcing colors to be opaque when developers
                                // could have set the alpha.
                            }
                            else
                            {
                                *dst = 0x00000000; // mark as translucent color
                            }
                            dst++; src++;
                        }
                    }
                    else //if ((transparency == java_awt_Transparency_OPAQUE) || (transparency == java_awt_Transparency_TRANSLUCENT))
                    {
                        jint mask = 0x00000000;
                        // <rdar://4224874> If the color model is OPAQUE than we need to create an opaque image for performance purposes.
                        // the default alphaInfo for INDEXED images is kCGImageAlphaFirst. Therefore we need to special case this.
                        if ((transparency == java_awt_Transparency_OPAQUE))
                        {
                            isdo->imageInfo.alphaInfo = kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Host;
                            mask = 0xff000000; // this is just a safeguard to make sure we fill the alpha
                        }

                        Pixel32bit* src = lutdata;
                        Pixel32bit* dst = isdo->lutData;
                        jint i;
                        for (i=0; (unsigned)i<isdo->lutDataSize; i++)
                        {
                            *dst = *src | mask;
                            dst++; src++;
                        }
                    }

                    (*env)->ReleasePrimitiveArrayCritical(env, lutarray, lutdata, 0);
                }
                else
                {
                    fprintf(stderr, "ERROR: malloc returns NULL for isdo->lutData in initRaster!\n");
                }
            }
            else
            {
                fprintf(stderr, "ERROR: GetPrimitiveArrayCritical returns NULL for lutdata in initRaster!\n");
            }
        }
        (*env)->DeleteLocalRef(env, lutarray);
    }

    QuartzSDOps *qsdo = (QuartzSDOps*)isdo;
    qsdo->BeginSurface                    = ImageSD_startCGContext;
    qsdo->FinishSurface                    = ImageSD_finishCGContext;

    qsdo->javaGraphicsStates            = (jint*)((*env)->GetDirectBufferAddress(env, jGraphicsState));
    qsdo->javaGraphicsStatesObjects        = (*env)->NewGlobalRef(env, jGraphicsStateObject);

    qsdo->graphicsStateInfo.batchedLines = NULL;
    qsdo->graphicsStateInfo.batchedLinesCount = 0;

    SurfaceDataOps *sdo = (SurfaceDataOps*)qsdo;
    sdo->Lock        = ImageSD_Lock;
    sdo->Unlock        = ImageSD_Unlock;
    sdo->GetRasInfo    = ImageSD_GetRasInfo;
    sdo->Release    = ImageSD_Release;
    sdo->Setup        = NULL;
    sdo->Dispose    = ImageSD_dispose;

    pthread_mutex_unlock(&isdo->lock);

//PrintImageInfo(isdo);
}

JNIEXPORT void JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_initCustomRaster(JNIEnv* env, jobject bisd, jobject array, jint width, jint height,
                                                                                    jobject jGraphicsState, jobject jGraphicsStateObject, jobject jImageInfo)
{
PRINT("Java_sun_java2d_OSXOffScreenSurfaceData_initCustomRaster")
    jint offset = 0;
    jint pixelStride = 4;
    jint scanStride = pixelStride*width;
    jobject icm = NULL;
    jint type = java_awt_image_BufferedImage_TYPE_CUSTOM;

    Java_sun_java2d_OSXOffScreenSurfaceData_initRaster(env, bisd, array, offset, width, height, pixelStride, scanStride, icm, type, jGraphicsState, jGraphicsStateObject, jImageInfo);
}

JNIEXPORT void JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_syncToJavaPixels(JNIEnv *env, jobject bisd)
{
PRINT("Java_sun_java2d_OSXOffScreenSurfaceData_syncToJavaPixels")

    syncToJavaPixels(env, (ImageSDOps*)SurfaceData_GetOps(env, bisd));
}

JNIEXPORT jboolean JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_xorSurfacePixels
  (JNIEnv *env, jobject dstIsd, jobject srcIsd, jint colorXOR, jint x, jint y, jint w, jint h)
{
PRINT("Java_sun_java2d_OSXOffScreenSurfaceData_xorSurfacePixels")
    return xorSurfacePixels(env, dstIsd, srcIsd, colorXOR, x, y, w, h);
}

JNIEXPORT jboolean JNICALL Java_sun_java2d_OSXOffScreenSurfaceData_clearSurfacePixels
  (JNIEnv *env, jobject bisd, jint w, jint h)
{
PRINT("Java_sun_java2d_OSXOffScreenSurfaceData_clearSurfacePixels")
    return clearSurfacePixels(env, bisd, w, h);

}
