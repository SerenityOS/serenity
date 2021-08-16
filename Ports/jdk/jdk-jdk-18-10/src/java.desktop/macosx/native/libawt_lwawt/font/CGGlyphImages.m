/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import <Accelerate/Accelerate.h> // for vImage_Buffer

#import "JNIUtilities.h"
#import "CGGlyphImages.h"
#import "CoreTextSupport.h"
#import "fontscalerdefs.h" // contains the definition of GlyphInfo struct

#import "sun_awt_SunHints.h"

//#define USE_IMAGE_ALIGNED_MEMORY 1
//#define CGGI_DEBUG 1
//#define CGGI_DEBUG_DUMP 1
//#define CGGI_DEBUG_HIT_COUNT 1

#define PRINT_TX(x) \
    NSLog(@"(%f, %f, %f, %f, %f, %f)", x.a, x.b, x.c, x.d, x.tx, x.ty);

/*
 * The GlyphCanvas is a global shared CGContext that characters are struck into.
 * For each character, the glyph is struck, copied into a GlyphInfo struct, and
 * the canvas is cleared for the next glyph.
 *
 * If the necessary canvas is too large, the shared one will not be used and a
 * temporary one will be provided.
 */
@interface CGGI_GlyphCanvas : NSObject {
@public
    CGContextRef context;
    vImage_Buffer *image;
}
@end;

@implementation CGGI_GlyphCanvas
@end


#pragma mark --- Debugging Helpers ---

/*
 * These debug functions are only compiled when CGGI_DEBUG is activated.
 * They will print out a full UInt8 canvas and any pixels struck (assuming
 * the canvas is not too big).
 *
 * As another debug feature, the entire canvas will be filled with a light
 * alpha value so it is easy to see where the glyph painting regions are
 * at runtime.
 */

#ifdef CGGI_DEBUG_DUMP
static void
DUMP_PIXELS(const char msg[], const UInt8 pixels[],
            const size_t bytesPerPixel, const int width, const int height)
{
    printf("| %s: (%d, %d)\n", msg, width, height);

    if (width > 80 || height > 80) {
        printf("| too big\n");
        return;
    }

    size_t i, j = 0, k, size = width * height;
    for (i = 0; i < size; i++) {
        for (k = 0; k < bytesPerPixel; k++) {
            if (pixels[i * bytesPerPixel + k] > 0x80) j++;
        }
    }

    if (j == 0) {
        printf("| empty\n");
        return;
    }

    printf("|_");
    int x, y;
    for (x = 0; x < width; x++) {
        printf("__");
    }
    printf("_\n");

    for (y = 0; y < height; y++) {
        printf("| ");
        for (x = 0; x < width; x++) {
            int p = 0;
            for(k = 0; k < bytesPerPixel; k++) {
                p += pixels[(y * width + x) * bytesPerPixel + k];
            }

            if (p < 0x80) {
                printf("  ");
            } else {
                printf("[]");
            }
        }
        printf(" |\n");
    }
}

static void
DUMP_IMG_PIXELS(const char msg[], const vImage_Buffer *image)
{
    const void *pixels = image->data;
    const size_t pixelSize = image->rowBytes / image->width;
    const size_t width = image->width;
    const size_t height = image->height;

    DUMP_PIXELS(msg, pixels, pixelSize, width, height);
}

static void
PRINT_CGSTATES_INFO(const CGContextRef cgRef)
{
    // TODO(cpc): lots of SPI use in this method; remove/rewrite?
#if 0
    CGRect clip = CGContextGetClipBoundingBox(cgRef);
    fprintf(stderr, "    clip: ((%f, %f), (%f, %f))\n",
            clip.origin.x, clip.origin.y, clip.size.width, clip.size.height);

    CGAffineTransform ctm = CGContextGetCTM(cgRef);
    fprintf(stderr, "    ctm: (%f, %f, %f, %f, %f, %f)\n",
            ctm.a, ctm.b, ctm.c, ctm.d, ctm.tx, ctm.ty);

    CGAffineTransform txtTx = CGContextGetTextMatrix(cgRef);
    fprintf(stderr, "    txtTx: (%f, %f, %f, %f, %f, %f)\n",
            txtTx.a, txtTx.b, txtTx.c, txtTx.d, txtTx.tx, txtTx.ty);

    if (CGContextIsPathEmpty(cgRef) == 0) {
        CGPoint pathpoint = CGContextGetPathCurrentPoint(cgRef);
        CGRect pathbbox = CGContextGetPathBoundingBox(cgRef);
        fprintf(stderr, "    [pathpoint: (%f, %f)] [pathbbox: ((%f, %f), (%f, %f))]\n",
                pathpoint.x, pathpoint.y, pathbbox.origin.x, pathbbox.origin.y,
                pathbbox.size.width, pathbbox.size.width);
    }

    CGFloat linewidth = CGContextGetLineWidth(cgRef);
    CGLineCap linecap = CGContextGetLineCap(cgRef);
    CGLineJoin linejoin = CGContextGetLineJoin(cgRef);
    CGFloat miterlimit = CGContextGetMiterLimit(cgRef);
    size_t dashcount = CGContextGetLineDashCount(cgRef);
    fprintf(stderr, "    [linewidth: %f] [linecap: %d] [linejoin: %d] [miterlimit: %f] [dashcount: %lu]\n",
            linewidth, linecap, linejoin, miterlimit, (unsigned long)dashcount);

    CGFloat smoothness = CGContextGetSmoothness(cgRef);
    bool antialias = CGContextGetShouldAntialias(cgRef);
    bool smoothfont = CGContextGetShouldSmoothFonts(cgRef);
    JRSFontRenderingStyle fRendMode = CGContextGetFontRenderingMode(cgRef);
    fprintf(stderr, "    [smoothness: %f] [antialias: %d] [smoothfont: %d] [fontrenderingmode: %d]\n",
            smoothness, antialias, smoothfont, fRendMode);
#endif
}
#endif

#ifdef CGGI_DEBUG

static void
DUMP_GLYPHINFO(const GlyphInfo *info)
{
    printf("size: (%d, %d) pixelSize: %d\n",
           info->width, info->height, info->rowBytes / info->width);
    printf("adv: (%f, %f) top: (%f, %f)\n",
           info->advanceX, info->advanceY, info->topLeftX, info->topLeftY);

#ifdef CGGI_DEBUG_DUMP
    DUMP_PIXELS("Glyph Info Struct",
                info->image, info->rowBytes / info->width,
                info->width, info->height);
#endif
}

#endif


#pragma mark --- Font Rendering Mode Descriptors ---
static Int32 reverseGamma = 0;

static UInt8 reverseGammaLut[256] = { 0 };

static inline UInt8* getReverseGammaLut() {
    if (reverseGamma == 0) {
        // initialize gamma lut
        double gamma;
        int i;
        const char* pGammaEnv = getenv("J2D_LCD_REVERSE_GAMMA");
        if (pGammaEnv != NULL) {
            reverseGamma = atol(pGammaEnv);
        }

        if (reverseGamma < 100 || reverseGamma > 250) {
            reverseGamma = 180;
        }

        gamma = 100.0 / reverseGamma;
        for (i = 0; i < 256; i++) {
            double x = ((double)i) / 255.0;
            reverseGammaLut[i] = (UInt8)(255 * pow(x, gamma));
        }
    }
    return reverseGammaLut;
}

static inline void
CGGI_CopyARGBPixelToRGBPixel(const UInt32 p, UInt8 *dst)
{
    UInt8* lut = getReverseGammaLut();

    *(dst + 0) = lut[0xFF - (p >> 16 & 0xFF)];  // red
    *(dst + 1) = lut[0xFF - (p >>  8 & 0xFF)];  // green
    *(dst + 2) = lut[0xFF - (p & 0xFF)];        // blue
}

static void
CGGI_CopyImageFromCanvasToRGBInfo(CGGI_GlyphCanvas *canvas, GlyphInfo *info)
{
    UInt32 *src = (UInt32 *)canvas->image->data;
    size_t srcRowWidth = canvas->image->width;

    UInt8 *dest = (UInt8 *)info->image;
    size_t destRowWidth = info->width;

    size_t height = info->height;

    size_t y;

    // fill empty glyph image with black-on-white glyph
    for (y = 0; y < height; y++) {
        size_t destRow = y * destRowWidth * 3;
        size_t srcRow = y * srcRowWidth;

        size_t x;
        for (x = 0; x < destRowWidth; x++) {
            CGGI_CopyARGBPixelToRGBPixel(src[srcRow + x],
                                         dest + destRow + x * 3);
        }
    }
}

//static void CGGI_copyImageFromCanvasToAlphaInfo
//(CGGI_GlyphCanvas *canvas, GlyphInfo *info)
//{
//    vImage_Buffer infoBuffer;
//    infoBuffer.data = info->image;
//    infoBuffer.width = info->width;
//    infoBuffer.height = info->height;
//    infoBuffer.rowBytes = info->width; // three bytes per RGB pixel
//
//    UInt8 scrapPixel[info->width * info->height];
//    vImage_Buffer scrapBuffer;
//    scrapBuffer.data = &scrapPixel;
//    scrapBuffer.width = info->width;
//    scrapBuffer.height = info->height;
//    scrapBuffer.rowBytes = info->width;
//
//    vImageConvert_ARGB8888toPlanar8(canvas->image, &infoBuffer,
//        &scrapBuffer, &scrapBuffer, &scrapBuffer, kvImageNoFlags);
//}

static inline UInt8
CGGI_ConvertBWPixelToByteGray(UInt32 p)
{
    return 0xFF - (((p >> 24 & 0xFF) + (p >> 16 & 0xFF) + (p >> 8 & 0xFF)) / 3);
}

static void
CGGI_CopyImageFromCanvasToAlphaInfo(CGGI_GlyphCanvas *canvas, GlyphInfo *info)
{
    UInt32 *src = (UInt32 *)canvas->image->data;
    size_t srcRowWidth = canvas->image->width;

    UInt8 *dest = (UInt8 *)info->image;
    size_t destRowWidth = info->width;

    size_t height = info->height;

    size_t y;

    // fill empty glyph image with black-on-white glyph
    for (y = 0; y < height; y++) {
        size_t destRow = y * destRowWidth;
        size_t srcRow = y * srcRowWidth;
        size_t x;
        for (x = 0; x < destRowWidth; x++) {
            UInt32 p = src[srcRow + x];
            dest[destRow + x] = CGGI_ConvertBWPixelToByteGray(p);
        }
    }
}

static void
CGGI_CopyImageFromCanvasToARGBInfo(CGGI_GlyphCanvas *canvas, GlyphInfo *info)
{
    CGBitmapInfo bitmapInfo = CGBitmapContextGetBitmapInfo(canvas->context);
    bool littleEndian = (bitmapInfo & kCGBitmapByteOrderMask) == kCGBitmapByteOrder32Little;

    UInt32 *src = (UInt32 *)canvas->image->data;
    size_t srcRowWidth = canvas->image->width;

    UInt8 *dest = (UInt8 *)info->image;
    size_t destRowWidth = info->width;

    size_t height = info->height;

    size_t y;

    for (y = 0; y < height; y++) {
        size_t srcRow = y * srcRowWidth;
        if (littleEndian) {
            UInt16 destRowBytes = info->rowBytes;
            memcpy(dest, src + srcRow, destRowBytes);
            dest += destRowBytes;
        } else {
            size_t x;
            for (x = 0; x < destRowWidth; x++) {
                UInt32 p = src[srcRow + x];
                *dest++ = (p >> 24  & 0xFF); // blue  (alpha-premultiplied)
                *dest++ = (p >> 16 & 0xFF); // green (alpha-premultiplied)
                *dest++ = (p >> 8   & 0xFF); // red   (alpha-premultiplied)
                *dest++ = (p & 0xFF); // alpha
            }
        }
    }
}

#pragma mark --- Pixel Size, Modes, and Canvas Shaping Helper Functions ---

typedef struct CGGI_GlyphInfoDescriptor {
    size_t pixelSize;
    void (*copyFxnPtr)(CGGI_GlyphCanvas *canvas, GlyphInfo *info);
} CGGI_GlyphInfoDescriptor;

typedef struct CGGI_RenderingMode {
    CGGI_GlyphInfoDescriptor *glyphDescriptor;
    JRSFontRenderingStyle cgFontMode;
} CGGI_RenderingMode;

static CGGI_GlyphInfoDescriptor grey =
    { 1, &CGGI_CopyImageFromCanvasToAlphaInfo };
static CGGI_GlyphInfoDescriptor rgb =
    { 3, &CGGI_CopyImageFromCanvasToRGBInfo };
static CGGI_GlyphInfoDescriptor argb =
    { 4, &CGGI_CopyImageFromCanvasToARGBInfo };

static inline CGGI_GlyphInfoDescriptor*
CGGI_GetGlyphInfoDescriptor(const CGGI_RenderingMode *mode, CTFontRef font)
{
    return IsEmojiFont(font) ? &argb : mode->glyphDescriptor;
}

static inline CGGI_RenderingMode
CGGI_GetRenderingMode(const AWTStrike *strike)
{
    CGGI_RenderingMode mode;
    mode.cgFontMode = strike->fStyle;
    NSException *e = nil;

    switch (strike->fAAStyle) {
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_OFF:
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_ON:
        mode.glyphDescriptor = &grey;
        break;
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_LCD_HRGB:
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_LCD_HBGR:
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_LCD_VRGB:
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_LCD_VBGR:
        mode.glyphDescriptor = &rgb;
        break;
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_GASP:
    case sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_DEFAULT:
    default:
        /* we expect that text antialiasing hint has been already
         * evaluated. Report an error if we get 'unevaluated' hint here.
         */
        e = [NSException
                exceptionWithName:@"IllegalArgumentException"
                reason:@"Invalid hint value"
                userInfo:nil];
        @throw e;
    }

    return mode;
}


#pragma mark --- Canvas Managment ---

/*
 * Creates a new canvas of a fixed size, and initializes the CGContext as
 * an 32-bit ARGB BitmapContext with some generic RGB color space.
 */
static inline void
CGGI_InitCanvas(CGGI_GlyphCanvas *canvas,
                const vImagePixelCount width, const vImagePixelCount height,
                const CGGI_RenderingMode* mode)
{
    // our canvas is *always* 4-byte ARGB
    size_t bytesPerRow = width * sizeof(UInt32);
    size_t byteCount = bytesPerRow * height;

    canvas->image = malloc(sizeof(vImage_Buffer));
    canvas->image->width = width;
    canvas->image->height = height;
    canvas->image->rowBytes = bytesPerRow;

    canvas->image->data = (void *)calloc(byteCount, sizeof(UInt8));
    if (canvas->image->data == NULL) {
        [[NSException exceptionWithName:NSMallocException
            reason:@"Failed to allocate memory for the buffer which backs the CGContext for glyph strikes." userInfo:nil] raise];
    }

    uint32_t bmpInfo = kCGImageAlphaPremultipliedFirst;
    if (mode->glyphDescriptor == &rgb) {
        bmpInfo |= kCGBitmapByteOrder32Host;
    }

    CGColorSpaceRef colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
    canvas->context = CGBitmapContextCreate(canvas->image->data,
                                            width, height, 8, bytesPerRow,
                                            colorSpace,
                                            bmpInfo);

    // set foreground color
    CGContextSetRGBFillColor(canvas->context, 0.0f, 0.0f, 0.0f, 1.0f);

    CGContextSetFontSize(canvas->context, 1);
    CGContextSaveGState(canvas->context);

    CGColorSpaceRelease(colorSpace);
}

/*
 * Releases the BitmapContext and the associated memory backing it.
 */
static inline void
CGGI_FreeCanvas(CGGI_GlyphCanvas *canvas)
{
    if (canvas->context != NULL) {
        CGContextRelease(canvas->context);
    }

    if (canvas->image != NULL) {
        if (canvas->image->data != NULL) {
            free(canvas->image->data);
        }
        free(canvas->image);
    }
}

/*
 * This is the slack space that is preallocated for the global GlyphCanvas
 * when it needs to be expanded. It has been set somewhat liberally to
 * avoid re-upsizing frequently.
 */
#define CGGI_GLYPH_CANVAS_SLACK 2.5

/*
 * Quick and easy inline to check if this canvas is big enough.
 */
static inline void
CGGI_SizeCanvas(CGGI_GlyphCanvas *canvas, const vImagePixelCount width,
        const vImagePixelCount height,
        const CGGI_RenderingMode* mode)
{
    if (canvas->image != NULL &&
        width  < canvas->image->width &&
        height < canvas->image->height)
    {
        return;
    }

    // if we don't have enough space to strike the largest glyph in the
    // run, resize the canvas
    CGGI_FreeCanvas(canvas);
    CGGI_InitCanvas(canvas,
                    width * CGGI_GLYPH_CANVAS_SLACK,
                    height * CGGI_GLYPH_CANVAS_SLACK,
                    mode);
    JRSFontSetRenderingStyleOnContext(canvas->context, mode->cgFontMode);
}

/*
 * Clear the canvas by blitting white (or transparent background for color glyphs) only into the region of interest
 * (the rect which we will copy out of once the glyph is struck).
 */
static inline void
CGGI_ClearCanvas(CGGI_GlyphCanvas *canvas, GlyphInfo *info, bool transparent)
{
    vImage_Buffer canvasRectToClear;
    canvasRectToClear.data = canvas->image->data;
    canvasRectToClear.height = info->height;
    canvasRectToClear.width = info->width;
    // use the row stride of the canvas, not the info
    canvasRectToClear.rowBytes = canvas->image->rowBytes;

    // clean the canvas
#ifdef CGGI_DEBUG
    Pixel_8888 background = { 0xE0, 0xE0, 0xE0, 0xE0 };
#else
    Pixel_8888 background = { transparent ? 0 : 0xFF,
                               transparent ? 0 : 0xFF,
                               transparent ? 0 : 0xFF,
                               transparent ? 0 : 0xFF };
#endif

    // clear canvas background
    vImageBufferFill_ARGB8888(&canvasRectToClear, background, kvImageNoFlags);
}


#pragma mark --- GlyphInfo Creation & Copy Functions ---

/*
 * Creates a GlyphInfo with exactly the correct size image and measurements.
 */
#define CGGI_GLYPH_BBOX_PADDING 2.0f
static inline GlyphInfo *
CGGI_CreateNewGlyphInfoFrom(CGSize advance, CGRect bbox,
                            const AWTStrike *strike,
                            const CGGI_GlyphInfoDescriptor *glyphDescriptor)
{
    size_t pixelSize = glyphDescriptor->pixelSize;

    // adjust the bounding box to be 1px bigger on each side than what
    // CGFont-whatever suggests - because it gives a bounding box that
    // is too tight
    bbox.size.width += CGGI_GLYPH_BBOX_PADDING * 2.0f;
    bbox.size.height += CGGI_GLYPH_BBOX_PADDING * 2.0f;
    bbox.origin.x -= CGGI_GLYPH_BBOX_PADDING;
    bbox.origin.y -= CGGI_GLYPH_BBOX_PADDING;

    vImagePixelCount width = ceilf(bbox.size.width);
    vImagePixelCount height = ceilf(bbox.size.height);

    // if the glyph is larger than 1MB, don't even try...
    // the GlyphVector path should have taken over by now
    // and zero pixels is ok
    if (width * height > 1024 * 1024) {
        width = 1;
        height = 1;
    }
    advance = CGSizeApplyAffineTransform(advance, strike->fFontTx);
    if (!JRSFontStyleUsesFractionalMetrics(strike->fStyle)) {
        advance.width = round(advance.width);
        advance.height = round(advance.height);
    }
    advance = CGSizeApplyAffineTransform(advance, strike->fDevTx);

#ifdef USE_IMAGE_ALIGNED_MEMORY
    // create separate memory
    GlyphInfo *glyphInfo = (GlyphInfo *)malloc(sizeof(GlyphInfo));
    void *image = (void *)malloc(height * width * pixelSize);
#else
    // create a GlyphInfo struct fused to the image it points to
    GlyphInfo *glyphInfo = (GlyphInfo *)malloc(sizeof(GlyphInfo) +
                                               height * width * pixelSize);
#endif

    glyphInfo->advanceX = advance.width;
    glyphInfo->advanceY = advance.height;
    glyphInfo->topLeftX = round(bbox.origin.x);
    glyphInfo->topLeftY = round(bbox.origin.y);
    glyphInfo->width = width;
    glyphInfo->height = height;
    glyphInfo->rowBytes = width * pixelSize;
    glyphInfo->cellInfo = NULL;

#ifdef USE_IMAGE_ALIGNED_MEMORY
    glyphInfo->image = image;
#else
    glyphInfo->image = ((void *)glyphInfo) + sizeof(GlyphInfo);
#endif

    return glyphInfo;
}


#pragma mark --- Glyph Striking onto Canvas ---

/*
 * Clears the canvas, strikes the glyph with CoreGraphics, and then
 * copies the struck pixels into the GlyphInfo image.
 */
static inline void
CGGI_CreateImageForGlyph
    (CGGI_GlyphCanvas *canvas, const CGGlyph glyph,
     GlyphInfo *info, const CGGI_GlyphInfoDescriptor *glyphDescriptor, const AWTStrike *strike, CTFontRef font)
{
    if (isnan(info->topLeftX) || isnan(info->topLeftY)) {
        // Explicitly set glyphInfo width/height to be 0 to ensure
        // zero length glyph image is copied into GlyphInfo from canvas
        info->width = 0;
        info->height = 0;

        // copy the "empty" glyph from the canvas into the info
        (*glyphDescriptor->copyFxnPtr)(canvas, info);
        return;
    }

    // clean the canvas
    CGGI_ClearCanvas(canvas, info, glyphDescriptor == &argb);

    // strike the glyph in the upper right corner
    CGFloat x = -info->topLeftX;
    CGFloat y = canvas->image->height + info->topLeftY;

    if (glyphDescriptor == &argb) {
        // Emoji glyphs are not rendered by CGContextShowGlyphsAtPoint.
        // Also, it's not possible to use transformation matrix to get the emoji glyph
        // rendered for the desired font size - actual-size font object is needed.
        // The logic here must match the logic in CGGlyphImages_GetGlyphMetrics,
        // which calculates glyph metrics.

        CGAffineTransform matrix = CGContextGetTextMatrix(canvas->context);
        CGFloat fontSize = sqrt(fabs(matrix.a * matrix.d - matrix.b * matrix.c));
        CTFontRef sizedFont = CTFontCreateCopyWithSymbolicTraits(font, fontSize, NULL, 0, 0);

        CGFloat normFactor = 1.0 / fontSize;
        CGAffineTransform normalizedMatrix = CGAffineTransformScale(matrix, normFactor, normFactor);
        CGContextSetTextMatrix(canvas->context, normalizedMatrix);

        CGPoint userPoint = CGPointMake(x, y);
        CGAffineTransform normalizedMatrixInv = CGAffineTransformInvert(normalizedMatrix);
        CGPoint textPoint = CGPointApplyAffineTransform(userPoint, normalizedMatrixInv);

        CTFontDrawGlyphs(sizedFont, &glyph, &textPoint, 1, canvas->context);

        CFRelease(sizedFont);
        // restore context's original state
        CGContextSetTextMatrix(canvas->context, matrix);
        CGContextSetFontSize(canvas->context, 1); // CTFontDrawGlyphs tampers with it
    } else {
        CGContextShowGlyphsAtPoint(canvas->context, x, y, &glyph, 1);
    }

    // copy the glyph from the canvas into the info
    (*glyphDescriptor->copyFxnPtr)(canvas, info);
}

/*
 * CoreText path...
 */
static inline GlyphInfo *
CGGI_CreateImageForUnicode
    (CGGI_GlyphCanvas *canvas, const AWTStrike *strike,
     const CGGI_RenderingMode *mode, const UnicodeScalarValue uniChar)
{
    // save the state of the world
    CGContextSaveGState(canvas->context);

    // get the glyph, measure it using CG
    CGGlyph glyph;
    CTFontRef fallback;
    if (uniChar > 0xFFFF) {
        UTF16Char charRef[2];
        CTS_BreakupUnicodeIntoSurrogatePairs(uniChar, charRef);
        CGGlyph glyphTmp[2];
        fallback = CTS_CopyCTFallbackFontAndGlyphForUnicode(strike->fAWTFont, (const UTF16Char *)&charRef, (CGGlyph *)&glyphTmp, 2);
        glyph = glyphTmp[0];
    } else {
        UTF16Char charRef;
        charRef = (UTF16Char) uniChar; // truncate.
        fallback = CTS_CopyCTFallbackFontAndGlyphForUnicode(strike->fAWTFont, (const UTF16Char *)&charRef, &glyph, 1);
    }

    CGAffineTransform tx = strike->fTx;
    JRSFontRenderingStyle style = JRSFontAlignStyleForFractionalMeasurement(strike->fStyle);

    CGRect bbox;
    CGSize advance;
    CGGlyphImages_GetGlyphMetrics(fallback, &tx, style, &glyph, 1, &bbox, &advance);

    CGGI_GlyphInfoDescriptor *glyphDescriptor = CGGI_GetGlyphInfoDescriptor(mode, fallback);

    // create the Sun2D GlyphInfo we are going to strike into
    GlyphInfo *info = CGGI_CreateNewGlyphInfoFrom(advance, bbox, strike, glyphDescriptor);

    // fix the context size, just in case the substituted character is unexpectedly large
    CGGI_SizeCanvas(canvas, info->width, info->height, mode);

    // align the transform for the real CoreText strike
    CGContextSetTextMatrix(canvas->context, strike->fAltTx);

    const CGFontRef cgFallback = CTFontCopyGraphicsFont(fallback, NULL);
    CGContextSetFont(canvas->context, cgFallback);
    CFRelease(cgFallback);

    // clean the canvas - align, strike, and copy the glyph from the canvas into the info
    CGGI_CreateImageForGlyph(canvas, glyph, info, glyphDescriptor, strike, fallback);

    // restore the state of the world
    CGContextRestoreGState(canvas->context);

    CFRelease(fallback);
#ifdef CGGI_DEBUG
    DUMP_GLYPHINFO(info);
#endif

#ifdef CGGI_DEBUG_DUMP
    DUMP_IMG_PIXELS("CGGI Canvas", canvas->image);
#if 0
    PRINT_CGSTATES_INFO(NULL);
#endif
#endif

    return info;
}


#pragma mark --- GlyphInfo Filling and Canvas Managment ---

/*
 * Sets all the per-run properties for the canvas, and then iterates through
 * the character run, and creates images in the GlyphInfo structs.
 *
 * Not inlined because it would create two copies in the function below
 */
static void
CGGI_FillImagesForGlyphsWithSizedCanvas(CGGI_GlyphCanvas *canvas,
                                        const AWTStrike *strike,
                                        const CGGI_RenderingMode *mode,
                                        jlong glyphInfos[],
                                        const UnicodeScalarValue uniChars[],
                                        const CGGlyph glyphs[],
                                        const CFIndex len)
{
    CGContextSetTextMatrix(canvas->context, strike->fAltTx);

    CGContextSetFont(canvas->context, strike->fAWTFont->fNativeCGFont);
    JRSFontSetRenderingStyleOnContext(canvas->context, strike->fStyle);

    CTFontRef mainFont = (CTFontRef)strike->fAWTFont->fFont;
    CGGI_GlyphInfoDescriptor* mainFontDescriptor = CGGI_GetGlyphInfoDescriptor(mode, mainFont);

    CFIndex i;
    for (i = 0; i < len; i++) {
        GlyphInfo *info = (GlyphInfo *)jlong_to_ptr(glyphInfos[i]);
        if (info != NULL) {
            CGGI_CreateImageForGlyph(canvas, glyphs[i], info, mainFontDescriptor, strike, mainFont);
        } else {
            info = CGGI_CreateImageForUnicode(canvas, strike, mode, uniChars[i]);
            glyphInfos[i] = ptr_to_jlong(info);
        }
#ifdef CGGI_DEBUG
        DUMP_GLYPHINFO(info);
#endif

#ifdef CGGI_DEBUG_DUMP
        DUMP_IMG_PIXELS("CGGI Canvas", canvas->image);
#endif
    }
#ifdef CGGI_DEBUG_DUMP
    DUMP_IMG_PIXELS("CGGI Canvas", canvas->image);
    PRINT_CGSTATES_INFO(canvas->context);
#endif
}

static NSString *threadLocalAACanvasKey =
    @"Java CoreGraphics Text Renderer Cached Canvas for AA";

static NSString *threadLocalLCDCanvasKey =
    @"Java CoreGraphics Text Renderer Cached Canvas for LCD";

/*
 * This is the maximum length and height times the above slack squared
 * to determine if we go with the global canvas, or malloc one on the spot.
 */
#define CGGI_GLYPH_CANVAS_MAX 100

/*
 * Based on the space needed to strike the largest character in the run,
 * either use the global shared canvas, or make one up on the spot, strike
 * the glyphs, and destroy it.
 */
static inline void
CGGI_FillImagesForGlyphs(jlong *glyphInfos, const AWTStrike *strike,
                         const CGGI_RenderingMode *mode,
                         const UnicodeScalarValue uniChars[], const CGGlyph glyphs[],
                         const size_t maxWidth, const size_t maxHeight,
                         const CFIndex len)
{
    if (maxWidth*maxHeight*CGGI_GLYPH_CANVAS_SLACK*CGGI_GLYPH_CANVAS_SLACK >
        CGGI_GLYPH_CANVAS_MAX*CGGI_GLYPH_CANVAS_MAX*CGGI_GLYPH_CANVAS_SLACK*CGGI_GLYPH_CANVAS_SLACK)
    {
        CGGI_GlyphCanvas *tmpCanvas = [[CGGI_GlyphCanvas alloc] init];
        CGGI_InitCanvas(tmpCanvas, maxWidth, maxHeight, mode);
        CGGI_FillImagesForGlyphsWithSizedCanvas(tmpCanvas, strike,
                mode, glyphInfos, uniChars,
                glyphs, len);
        CGGI_FreeCanvas(tmpCanvas);

        [tmpCanvas release];
        return;
    }
    NSMutableDictionary *threadDict =
        [[NSThread currentThread] threadDictionary];

    NSString* theKey = (mode->glyphDescriptor == &rgb) ?
        threadLocalLCDCanvasKey : threadLocalAACanvasKey;

    CGGI_GlyphCanvas *canvas = [threadDict objectForKey:theKey];
    if (canvas == nil) {
        canvas = [[CGGI_GlyphCanvas alloc] init];
        [threadDict setObject:canvas forKey:theKey];
    }

    CGGI_SizeCanvas(canvas, maxWidth, maxHeight, mode);
    CGGI_FillImagesForGlyphsWithSizedCanvas(canvas, strike, mode,
                                            glyphInfos, uniChars, glyphs, len);
}

/*
 * Finds the advances and bounding boxes of the characters in the run,
 * cycles through all the bounds and calculates the maximum canvas space
 * required by the largest glyph.
 *
 * Creates a GlyphInfo struct with a malloc that also encapsulates the
 * image the struct points to.  This is done to meet memory layout
 * expectations in the Sun text rasterizer memory managment code.
 * The image immediately follows the struct physically in memory.
 */
static inline void
CGGI_CreateGlyphInfos(jlong *glyphInfos, const AWTStrike *strike,
                      const CGGI_RenderingMode *mode,
                      const UnicodeScalarValue uniChars[], const CGGlyph glyphs[],
                      CGSize advances[], CGRect bboxes[], const CFIndex len)
{
    AWTFont *font = strike->fAWTFont;
    CGAffineTransform tx = strike->fTx;
    JRSFontRenderingStyle bboxCGMode = JRSFontAlignStyleForFractionalMeasurement(strike->fStyle);

    CTFontRef fontRef = (CTFontRef)font->fFont;
    CGGlyphImages_GetGlyphMetrics(fontRef, &tx, bboxCGMode, glyphs, len, bboxes, advances);
    CGGI_GlyphInfoDescriptor* mainFontDescriptor = CGGI_GetGlyphInfoDescriptor(mode, fontRef);

    size_t maxWidth = 1;
    size_t maxHeight = 1;

    CFIndex i;
    for (i = 0; i < len; i++)
    {
        if (uniChars[i] != 0)
        {
            glyphInfos[i] = 0L;
            continue; // will be handled later
        }

        CGSize advance = advances[i];
        CGRect bbox = bboxes[i];

        GlyphInfo *glyphInfo = CGGI_CreateNewGlyphInfoFrom(advance, bbox, strike, mainFontDescriptor);

        if (maxWidth < glyphInfo->width)   maxWidth = glyphInfo->width;
        if (maxHeight < glyphInfo->height) maxHeight = glyphInfo->height;

        glyphInfos[i] = ptr_to_jlong(glyphInfo);
    }

    CGGI_FillImagesForGlyphs(glyphInfos, strike, mode, uniChars,
                             glyphs, maxWidth, maxHeight, len);
}


#pragma mark --- Temporary Buffer Allocations and Initialization ---

/*
 * This stage separates the already valid glyph codes from the unicode values
 * that need special handling - the rawGlyphCodes array is no longer used
 * after this stage.
 */
static void
CGGI_CreateGlyphsAndScanForComplexities(jlong *glyphInfos,
                                        const AWTStrike *strike,
                                        const CGGI_RenderingMode *mode,
                                        jint rawGlyphCodes[],
                                        UnicodeScalarValue uniChars[], CGGlyph glyphs[],
                                        CGSize advances[], CGRect bboxes[],
                                        const CFIndex len)
{
    CFIndex i;
    for (i = 0; i < len; i++) {
        jint code = rawGlyphCodes[i];
        if (code < 0) {
            glyphs[i] = 0;
            uniChars[i] = -code;
        } else {
            glyphs[i] = code;
            uniChars[i] = 0;
        }
    }

    CGGI_CreateGlyphInfos(glyphInfos, strike, mode,
                          uniChars, glyphs, advances, bboxes, len);

#ifdef CGGI_DEBUG_HIT_COUNT
    static size_t hitCount = 0;
    hitCount++;
    printf("%d\n", (int)hitCount);
#endif
}

/*
 * Conditionally stack allocates buffers for glyphs, bounding boxes,
 * and advances.  Unfortunately to use CG or CT in bulk runs (which is
 * faster than calling them per character), we have to copy into and out
 * of these buffers. Still a net win though.
 */
void
CGGlyphImages_GetGlyphImagePtrs(jlong glyphInfos[],
                                const AWTStrike *strike,
                                jint rawGlyphCodes[], const CFIndex len)
{
    const CGGI_RenderingMode mode = CGGI_GetRenderingMode(strike);

    if (len < MAX_STACK_ALLOC_GLYPH_BUFFER_SIZE) {
        CGRect bboxes[len];
        CGSize advances[len];
        CGGlyph glyphs[len];
        UnicodeScalarValue uniChars[len];

        CGGI_CreateGlyphsAndScanForComplexities(glyphInfos, strike, &mode,
                                                rawGlyphCodes, uniChars, glyphs,
                                                advances, bboxes, len);

        return;
    }

    // just do one malloc, and carve it up for all the buffers
    void *buffer = malloc(sizeof(CGRect) * sizeof(CGSize) *
                          sizeof(CGGlyph) * sizeof(UnicodeScalarValue) * len);
    if (buffer == NULL) {
        [[NSException exceptionWithName:NSMallocException
            reason:@"Failed to allocate memory for the temporary glyph strike and measurement buffers." userInfo:nil] raise];
    }

    CGRect *bboxes = (CGRect *)(buffer);
    CGSize *advances = (CGSize *)(bboxes + sizeof(CGRect) * len);
    CGGlyph *glyphs = (CGGlyph *)(advances + sizeof(CGGlyph) * len);
    UnicodeScalarValue *uniChars = (UnicodeScalarValue *)(glyphs + sizeof(UnicodeScalarValue) * len);

    CGGI_CreateGlyphsAndScanForComplexities(glyphInfos, strike, &mode,
                                            rawGlyphCodes, uniChars, glyphs,
                                            advances, bboxes, len);

    free(buffer);
}

/*
 * Calculates bounding boxes (for given transform) and advance (for untransformed 1pt-size font) for specified glyphs.
 */
void
CGGlyphImages_GetGlyphMetrics(const CTFontRef font,
                              const CGAffineTransform *tx,
                              const JRSFontRenderingStyle style,
                              const CGGlyph glyphs[],
                              size_t count,
                              CGRect bboxes[],
                              CGSize advances[]) {
    if (IsEmojiFont(font)) {
        // Glyph metrics for emoji font are not strictly proportional to font size,
        // so we need to construct real-sized font object to calculate them.
        // The logic here must match the logic in CGGI_CreateImageForGlyph,
        // which performs glyph drawing.

        CGFloat fontSize = sqrt(fabs(tx->a * tx->d - tx->b * tx->c));
        CTFontRef sizedFont = CTFontCreateCopyWithSymbolicTraits(font, fontSize, NULL, 0, 0);

        if (bboxes) {
            // JRSFontGetBoundingBoxesForGlyphsAndStyle works incorrectly for AppleColorEmoji font:
            // it uses bottom left corner of the glyph's bounding box as a fixed point of transformation
            // instead of glyph's origin point (used at drawing). So, as a workaround,
            // we request a bounding box for the untransformed glyph, and apply the transform ourselves.
            JRSFontGetBoundingBoxesForGlyphsAndStyle(sizedFont, &CGAffineTransformIdentity, style, glyphs, count, bboxes);
            CGAffineTransform txNormalized = CGAffineTransformMake(tx->a / fontSize,
                                                                   tx->b / fontSize,
                                                                   tx->c / fontSize,
                                                                   tx->d / fontSize,
                                                                   0, 0);
            for (int i = 0; i < count; i++) {
                bboxes[i] = CGRectApplyAffineTransform(bboxes[i], txNormalized);
            }
        }

        if (advances) {
            CTFontGetAdvancesForGlyphs(sizedFont, kCTFontDefaultOrientation, glyphs, advances, count);
            for (int i = 0; i < count; i++) {
                // Calling code will scale the result back
                advances[i].width /= fontSize;
                advances[i].height /= fontSize;
            }
        }

        CFRelease(sizedFont);
    } else {
        if (bboxes) {
            JRSFontGetBoundingBoxesForGlyphsAndStyle(font, tx, style, glyphs, count, bboxes);
        }
        if (advances) {
            CTFontGetAdvancesForGlyphs(font, kCTFontDefaultOrientation, glyphs, advances, count);
        }
    }
}