/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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



/**
 * This class holds the information for a particular graphics device.
 * Since a display change can cause the creation of new devices
 * at any time, there is no referencing of the devices array allowed.
 * Instead, anyone wishing to reference a device in the array (e.g.,
 * the current default device or a device for a given hWnd) must
 * call one of the static methods of this class with the index of
 * the device in question.  Those methods will then lock the devices
 * array and forward the request to the current device at that
 * array index.
 */

#include <awt.h>
#include <sun_awt_Win32GraphicsDevice.h>
#include "awt_Canvas.h"
#include "awt_Win32GraphicsDevice.h"
#include "awt_Window.h"
#include "java_awt_Transparency.h"
#include "java_awt_color_ColorSpace.h"
#include "sun_awt_Win32GraphicsDevice.h"
#include "java_awt_image_DataBuffer.h"
#include "dither.h"
#include "img_util_md.h"
#include "Devices.h"
#include "systemScale.h"

uns_ordered_dither_array img_oda_alpha;

jclass      AwtWin32GraphicsDevice::indexCMClass;
jclass      AwtWin32GraphicsDevice::wToolkitClass;
jfieldID    AwtWin32GraphicsDevice::dynamicColorModelID;
jfieldID    AwtWin32GraphicsDevice::indexCMrgbID;
jfieldID    AwtWin32GraphicsDevice::indexCMcacheID;
jmethodID   AwtWin32GraphicsDevice::paletteChangedMID;
BOOL        AwtWin32GraphicsDevice::primaryPalettized;
int         AwtWin32GraphicsDevice::primaryIndex = 0;


/**
 * Construct this device.  Store the screen (index into the devices
 * array of this object), the array (used in static references via
 * particular device indices), the monitor/pMonitorInfo (which other
 * classes will inquire of this device), the bits per pixel of this
 * device, and information on whether the primary device is palettized.
 */
AwtWin32GraphicsDevice::AwtWin32GraphicsDevice(int screen,
                                               HMONITOR mhnd, Devices *arr)
{
    this->screen  = screen;
    this->devicesArray = arr;
    this->scaleX = 1;
    this->scaleY = 1;
    disableScaleAutoRefresh = FALSE;
    javaDevice = NULL;
    colorData = new ImgColorData;
    colorData->grayscale = GS_NOTGRAY;
    palette = NULL;
    cData = NULL;
    gpBitmapInfo = NULL;
    monitor = mhnd;
    pMonitorInfo = new MONITORINFOEX;
    pMonitorInfo->cbSize = sizeof(MONITORINFOEX);
    ::GetMonitorInfo(monitor, pMonitorInfo);

    // Set primary device info: other devices will need to know
    // whether the primary is palettized during the initialization
    // process
    HDC hDC = this->GetDC();
    colorData->bitsperpixel = ::GetDeviceCaps(hDC, BITSPIXEL);
    this->ReleaseDC(hDC);
    if (MONITORINFOF_PRIMARY & pMonitorInfo->dwFlags) {
        primaryIndex = screen;
        if (colorData->bitsperpixel > 8) {
            primaryPalettized = FALSE;
        } else {
            primaryPalettized = TRUE;
        }
    }
}

AwtWin32GraphicsDevice::~AwtWin32GraphicsDevice()
{
    delete colorData;
    if (gpBitmapInfo) {
        free(gpBitmapInfo);
    }
    if (palette) {
        delete palette;
    }
    if (pMonitorInfo) {
        delete pMonitorInfo;
    }
    if (javaDevice) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        env->DeleteWeakGlobalRef(javaDevice);
    }
    if (cData != NULL) {
        freeICMColorData(cData);
    }
}

HDC AwtWin32GraphicsDevice::MakeDCFromMonitor(HMONITOR hmMonitor) {
    HDC retCode = NULL;
    if (NULL != hmMonitor) {
        MONITORINFOEX mieInfo;

        memset((void*)(&mieInfo), 0, sizeof(MONITORINFOEX));
        mieInfo.cbSize = sizeof(MONITORINFOEX);

        if (TRUE == ::GetMonitorInfo(hmMonitor, (LPMONITORINFOEX)(&mieInfo))) {
            HDC hDC = CreateDC(mieInfo.szDevice, NULL, NULL, NULL);
            if (NULL != hDC) {
                retCode = hDC;
            }
        }
    }
    return retCode;
}

HDC AwtWin32GraphicsDevice::GetDC()
{
    return MakeDCFromMonitor(monitor);
}

void AwtWin32GraphicsDevice::ReleaseDC(HDC hDC)
{
    if (hDC != NULL) {
        ::DeleteDC(hDC);
    }
}

/**
 * Init this device.  This creates the bitmap structure
 * used to hold the device color data and initializes any
 * appropriate palette structures.
 */
void AwtWin32GraphicsDevice::Initialize()
{
    unsigned int ri, gi, bi;
    if (colorData->bitsperpixel < 8) {
        // REMIND: how to handle?
    }

    // Create a BitmapInfo object for color data
    if (!gpBitmapInfo) {
        try {
            gpBitmapInfo = (BITMAPINFO *)
                safe_Malloc(sizeof(BITMAPINFOHEADER) + 256 * sizeof(RGBQUAD));
        } catch (std::bad_alloc&) {
            throw;
        }
        gpBitmapInfo->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    }
    gpBitmapInfo->bmiHeader.biBitCount = 0;
    HDC hBMDC = this->GetDC();
    HBITMAP hBM = ::CreateCompatibleBitmap(hBMDC, 1, 1);
    VERIFY(::GetDIBits(hBMDC, hBM, 0, 1, NULL, gpBitmapInfo, DIB_RGB_COLORS));

    if (colorData->bitsperpixel > 8) {
        if (MONITORINFOF_PRIMARY & pMonitorInfo->dwFlags) {
            primaryPalettized = FALSE;
        }
        if (colorData->bitsperpixel != 24) { // 15, 16, or 32 bpp
            int foo;
            gpBitmapInfo->bmiHeader.biCompression = BI_BITFIELDS;
            if (::GetDIBits(hBMDC, hBM, 0, 1, &foo, gpBitmapInfo,
                            DIB_RGB_COLORS) == 0)
            {
                // Bug 4684966: If GetDIBits returns an error, we could
                // get stuck in an infinite loop setting the colorData
                // fields.  Hardcode bitColors to reasonable values instead.
                // These values are picked according to standard masks
                // for these bit depths on win9x, according to MSDN docs.
                switch (colorData->bitsperpixel) {
                case 15:
                    ((int *)gpBitmapInfo->bmiColors)[0] = 0x7c00;
                    ((int *)gpBitmapInfo->bmiColors)[1] = 0x03e0;
                    ((int *)gpBitmapInfo->bmiColors)[2] = 0x001f;
                    break;
                case 16:
                    ((int *)gpBitmapInfo->bmiColors)[0] = 0xf800;
                    ((int *)gpBitmapInfo->bmiColors)[1] = 0x07e0;
                    ((int *)gpBitmapInfo->bmiColors)[2] = 0x001f;
                    break;
                case 32:
                default:
                    ((int *)gpBitmapInfo->bmiColors)[0] = 0xff0000;
                    ((int *)gpBitmapInfo->bmiColors)[1] = 0x00ff00;
                    ((int *)gpBitmapInfo->bmiColors)[2] = 0x0000ff;
                    break;
                }
            }
            ri = ((unsigned int *)gpBitmapInfo->bmiColors)[0];
            colorData->rOff = 0;
            while ((ri & 1) == 0) {
                colorData->rOff++;
                ri >>= 1;
            }
            colorData->rScale = 0;
            while (ri < 0x80) {
                colorData->rScale++;
                ri <<= 1;
            }
            gi = ((unsigned int *)gpBitmapInfo->bmiColors)[1];
            colorData->gOff = 0;
            while ((gi & 1) == 0) {
                colorData->gOff++;
                gi >>= 1;
            }
            colorData->gScale = 0;
            while (gi < 0x80) {
                colorData->gScale++;
                gi <<= 1;
            }
            bi = ((unsigned int *)gpBitmapInfo->bmiColors)[2];
            colorData->bOff = 0;
            while ((bi & 1) == 0) {
                colorData->bOff++;
                bi >>= 1;
            }
            colorData->bScale = 0;
            while (bi < 0x80) {
                colorData->bScale++;
                bi <<= 1;
            }
            if (   (0 == colorData->bOff)
                && (5 == colorData->gOff)
                && (10 == colorData->rOff)
                && (3 == colorData->bScale)
                && (3 == colorData->gScale)
                && (3 == colorData->rScale)) {
                colorData->bitsperpixel = 15;
                gpBitmapInfo->bmiHeader.biCompression = BI_RGB;
            }
        } else {    // 24 bpp
            gpBitmapInfo->bmiHeader.biBitCount = 24;
            gpBitmapInfo->bmiHeader.biCompression = BI_RGB;

            // Fill these values in as a convenience for the screen
            // ColorModel construction code below (see getColorModel())
            ((int *)gpBitmapInfo->bmiColors)[0] = 0x0000ff;
            ((int *)gpBitmapInfo->bmiColors)[1] = 0x00ff00;
            ((int *)gpBitmapInfo->bmiColors)[2] = 0xff0000;
        }
    } else {
        if (MONITORINFOF_PRIMARY & pMonitorInfo->dwFlags) {
            primaryPalettized = TRUE;
        }
        gpBitmapInfo->bmiHeader.biBitCount = 8;
        gpBitmapInfo->bmiHeader.biCompression = BI_RGB;
        gpBitmapInfo->bmiHeader.biClrUsed = 256;
        gpBitmapInfo->bmiHeader.biClrImportant = 256;

        // The initialization of cData is done prior to
        // calling palette->Update() since we need it
        // for calculating inverseGrayLut
        if (cData == NULL) {
            cData = (ColorData*)safe_Calloc(1, sizeof(ColorData));
            memset(cData, 0, sizeof(ColorData));
            initDitherTables(cData);
        }

        if (!palette) {
            palette = new AwtPalette(this);
        } else {
            palette->Update();
        }
        palette->UpdateLogical();
    }
    VERIFY(::DeleteObject(hBM));
    VERIFY(::DeleteDC(hBMDC));
}

/**
 * Creates a new colorModel given the current device configuration.
 * The dynamic flag determines whether we use the system palette
 * (dynamic == TRUE) or our custom palette in creating a new
 * IndexedColorModel.
 */
jobject AwtWin32GraphicsDevice::GetColorModel(JNIEnv *env, jboolean dynamic)
{
    jobject awt_colormodel;
    int i;
    if (colorData->bitsperpixel == 24) {
        awt_colormodel =
            JNU_NewObjectByName(env, "sun/awt/Win32ColorModel24", "()V");
    } else if (colorData->bitsperpixel > 8) {
        int *masks = (int *)gpBitmapInfo->bmiColors;
        int numbits = 0;
        unsigned int bits = (masks[0] | masks[1] | masks[2]);
        while (bits) {
            numbits++;
            bits >>= 1;
        }
        awt_colormodel = JNU_NewObjectByName(env,
                                             "java/awt/image/DirectColorModel",
                                             "(IIII)V", numbits,
                                             masks[0], masks[1], masks[2]);
    } else if (colorData->grayscale == GS_STATICGRAY) {
        jclass clazz;
        jclass clazz1;
        jmethodID mid;
        jobject cspace = NULL;
        jint bits[1];
        jintArray bitsArray;

        clazz1 = env->FindClass("java/awt/color/ColorSpace");
        CHECK_NULL_RETURN(clazz1, NULL);
        mid = env->GetStaticMethodID(clazz1, "getInstance",
              "(I)Ljava/awt/color/ColorSpace;");
        CHECK_NULL_RETURN(mid, NULL);
        cspace = env->CallStaticObjectMethod(clazz1, mid,
            java_awt_color_ColorSpace_CS_GRAY);
        CHECK_NULL_RETURN(cspace, NULL);

        bits[0] = 8;
        bitsArray = env->NewIntArray(1);
        if (bitsArray == 0) {
            return NULL;
        } else {
            env->SetIntArrayRegion(bitsArray, 0, 1, bits);
        }

        clazz = env->FindClass("java/awt/image/ComponentColorModel");
        CHECK_NULL_RETURN(clazz, NULL);
        mid = env->GetMethodID(clazz,"<init>",
            "(Ljava/awt/color/ColorSpace;[IZZII)V");
        CHECK_NULL_RETURN(mid, NULL);

        awt_colormodel = env->NewObject(clazz, mid,
                                        cspace,
                                        bitsArray,
                                        JNI_FALSE,
                                        JNI_FALSE,
                                        java_awt_Transparency_OPAQUE,
                                        java_awt_image_DataBuffer_TYPE_BYTE);
    } else {
        jintArray hRGB = env->NewIntArray(256);
        unsigned int *rgb = NULL, *rgbP = NULL;
        jboolean allvalid = JNI_TRUE;
        jbyte vbits[256/8];
        jobject validBits = NULL;

        CHECK_NULL_RETURN(hRGB, NULL);
        /* Create the LUT from the color map */
        try {
            rgb = (unsigned int *) env->GetPrimitiveArrayCritical(hRGB, 0);
            CHECK_NULL_RETURN(rgb, NULL);
            rgbP = rgb;
            if (!palette) {
                palette = new AwtPalette(this);
                palette->UpdateLogical();
            }
            if (colorData->grayscale == GS_INDEXGRAY) {
                /* For IndexColorModel, pretend first 10 colors and last
                   10 colors are transparent black.  This makes
                   ICM.allgrayopaque true.
                */
                unsigned int *logicalEntries = palette->GetLogicalEntries();

                for (i=0; i < 10; i++) {
                    rgbP[i] = 0x00000000;
                    rgbP[i+246] = 0x00000000;
                }
                memcpy(&rgbP[10], &logicalEntries[10], 236 * sizeof(RGBQUAD));
                // We need to specify which entries in the colormap are
                // valid so that the transparent black entries we have
                // created do not affect the Transparency setting of the
                // IndexColorModel.  The vbits array is used to construct
                // a BigInteger such that the most significant bit of vbits[0]
                // indicates the validity of the last color (#256) and the
                // least significant bit of vbits[256/8] indicates the
                // validity of the first color (#0).  We need to fill vbits
                // with all 1's and then turn off the first and last 10 bits.
                memset(vbits, 0xff, sizeof(vbits));
                vbits[0] = 0;
                vbits[1] = (jbyte) (0xff >> 2);
                vbits[sizeof(vbits)-2] = (jbyte) (0xff << 2);
                vbits[sizeof(vbits)-1] = 0;
                allvalid = JNI_FALSE;
            } else {
                if (!dynamic) {
                    // If we plan to use our custom palette (i.e., we are
                    // not running inside another app and we are not creating
                    // a dynamic colorModel object), then setup ICM with
                    // custom palette entries
                    unsigned int *logicalEntries = palette->GetLogicalEntries();
                    memcpy(rgbP, logicalEntries, 256 * sizeof(int));
                } else {
                    // Else, use current system palette entries.
                    // REMIND: This may not give the result we want if
                    // we are running inside another app and that
                    // parent app is running in the background when we
                    // reach here.  We could at least cache an "ideal" set of
                    // system palette entries from the first time we are
                    // running in the foreground and then future ICM's will
                    // use that set instead.
                    unsigned int *systemEntries = palette->GetSystemEntries();
                    memcpy(rgbP, systemEntries, 256 * sizeof(int));
                }
            }
        } catch (...) {
            env->ReleasePrimitiveArrayCritical(hRGB, rgb, 0);
            throw;
        }

        env->ReleasePrimitiveArrayCritical(hRGB, rgb, 0);

        // Construct a new color model
        if (!allvalid) {
            jbyteArray bArray = env->NewByteArray(sizeof(vbits));
            CHECK_NULL_RETURN(bArray, NULL);
            env->SetByteArrayRegion(bArray, 0, sizeof(vbits), vbits);
            validBits = JNU_NewObjectByName(env,
                                            "java/math/BigInteger",
                                            "([B)V", bArray);
            JNU_CHECK_EXCEPTION_RETURN(env, NULL);
        }
        awt_colormodel =
            JNU_NewObjectByName(env,
                                "java/awt/image/IndexColorModel",
                                "(II[IIILjava/math/BigInteger;)V",
                                8, 256,
                                hRGB, 0,
                                java_awt_image_DataBuffer_TYPE_BYTE,
                                validBits);
    }
    return awt_colormodel;
}

/**
 * Called from AwtPalette code when it is determined what grayscale
 * value (if any) the current logical palette has
 */
void AwtWin32GraphicsDevice::SetGrayness(int grayValue)
{
    colorData->grayscale = grayValue;
}


/**
 * Update our dynamic IndexedColorModel.  This happens after
 * a change to the system palette.  Any surfaces stored in vram
 * (Win32OffScreenSurfaceData and GDIWindowSurfaceData objects)
 * refer to this colorModel and use its lookup table and inverse
 * lookup to calculate correct index values for rgb colors.  So
 * the colorModel must always reflect the current state of the
 * system palette.
 */
void AwtWin32GraphicsDevice::UpdateDynamicColorModel()
{
    if (!javaDevice) {
        // javaDevice may not be set yet.  If not, return.  In
        // this situation, we probably don't need an update anyway
        // since the colorModel will be created with the correct
        // info when the java side is initialized.
        return;
    }
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject colorModel = env->GetObjectField(javaDevice,
        dynamicColorModelID);
    if (!colorModel) {
        return;
    }
    if (env->IsInstanceOf(colorModel, indexCMClass)) {
        // If colorModel not of type ICM then we're not in 8-bit mode and
        // don't need to update it
        jboolean isCopy;
        unsigned int *newEntries = palette->GetSystemEntries();
        jintArray rgbArray = (jintArray)env->GetObjectField(colorModel,
            AwtWin32GraphicsDevice::indexCMrgbID);
        jintArray cacheArray = (jintArray)env->GetObjectField(colorModel,
            AwtWin32GraphicsDevice::indexCMcacheID);
        if (!rgbArray || !cacheArray) {
            JNU_ThrowInternalError(env, "rgb or lookupcache array of IndexColorModel null");
            return;
        }
        int rgbLength = env->GetArrayLength(rgbArray);
        int cacheLength = env->GetArrayLength(cacheArray);
        jint *cmEntries = (jint *)env->GetPrimitiveArrayCritical(rgbArray, &isCopy);
        if (!cmEntries) {
            env->ExceptionClear();
            JNU_ThrowInternalError(env, "Problem retrieving rgb critical array");
            return;
        }
        jint *cache = (jint *)env->GetPrimitiveArrayCritical(cacheArray, &isCopy);
        if (!cache) {
            env->ExceptionClear();
            env->ReleasePrimitiveArrayCritical(rgbArray, cmEntries, JNI_ABORT);
            JNU_ThrowInternalError(env, "Problem retrieving cache critical array");
            return;
        }
        // Set the new rgb values
    int i;
    for (i = 0; i < rgbLength; ++i) {
            cmEntries[i] = newEntries[i];
        }
        // clear out the old cache
        for (i = 0; i < cacheLength; ++i) {
            cache[i] = 0;
        }
        env->ReleasePrimitiveArrayCritical(cacheArray, cache, 0);
        env->ReleasePrimitiveArrayCritical(rgbArray, cmEntries, 0);

        // Call WToolkit::paletteChanged() method; this will invalidate
        // the offscreen surfaces dependent on this dynamic colorModel
        // to ensure that they get redrawn with the correct color indices
        env->CallStaticVoidMethod(AwtWin32GraphicsDevice::wToolkitClass,
            paletteChangedMID);
    }
}

unsigned int *AwtWin32GraphicsDevice::GetSystemPaletteEntries()
{
    // REMIND: What to do if palette NULL?  Need to throw
    // some kind of exception?
    return palette->GetSystemEntries();
}

unsigned char *AwtWin32GraphicsDevice::GetSystemInverseLUT()
{
    // REMIND: What to do if palette NULL?  Need to throw
    // some kind of exception?
    return palette->GetSystemInverseLUT();
}


BOOL AwtWin32GraphicsDevice::UpdateSystemPalette()
{
    if (colorData->bitsperpixel > 8) {
        return FALSE;
    } else {
        return palette->Update();
    }
}

HPALETTE AwtWin32GraphicsDevice::SelectPalette(HDC hDC)
{
    if (palette) {
        return palette->Select(hDC);
    } else {
        return NULL;
    }
}

void AwtWin32GraphicsDevice::RealizePalette(HDC hDC)
{
    if (palette) {
        palette->Realize(hDC);
    }
}

/**
 * Deterine which device the HWND exists on and return the
 * appropriate index into the devices array.
 */
int AwtWin32GraphicsDevice::DeviceIndexForWindow(HWND hWnd)
{
    HMONITOR mon = MonitorFromWindow(hWnd, MONITOR_DEFAULTTONEAREST);
    int screen = AwtWin32GraphicsDevice::GetScreenFromHMONITOR(mon);
    return screen;
}

/**
 * Get the HPALETTE associated with this device
 */
HPALETTE AwtWin32GraphicsDevice::GetPalette()
{
    if (palette) {
        return palette->GetPalette();
    } else {
        return NULL;
    }
}

/**
 * Object referring to this device is releasing that reference.
 * This allows the array holding all devices to be released (once
 * all references to the array have gone away).
 */
void AwtWin32GraphicsDevice::Release()
{
    devicesArray->Release();
}

/**
 * Links this native object with its java Win32GraphicsDevice.
 * Need this link because the colorModel of the java device
 * may be updated from native code.
 */
void AwtWin32GraphicsDevice::SetJavaDevice(JNIEnv *env, jobject objPtr)
{
    javaDevice = env->NewWeakGlobalRef(objPtr);
}

/**
 * Sets horizontal and vertical scale factors
 */
void AwtWin32GraphicsDevice::SetScale(float sx, float sy)
{
    scaleX = sx;
    scaleY = sy;
}

int AwtWin32GraphicsDevice::ScaleUpX(int x)
{
    return ClipRound(x * scaleX);
}

int AwtWin32GraphicsDevice::ScaleUpAbsX(int x)
{
    LONG screen = pMonitorInfo->rcMonitor.left;
    return screen + ClipRound((x - screen) * scaleX);
}

int AwtWin32GraphicsDevice::ScaleUpY(int y)
{
    return ClipRound(y * scaleY);
}

int AwtWin32GraphicsDevice::ScaleUpAbsY(int y)
{
    LONG screen = pMonitorInfo->rcMonitor.top;
    return screen + ClipRound((y - screen) * scaleY);
}

int AwtWin32GraphicsDevice::ScaleDownX(int x)
{
    return ClipRound(x / scaleX);
}

int AwtWin32GraphicsDevice::ScaleDownAbsX(int x)
{
    LONG screen = pMonitorInfo->rcMonitor.left;
    return screen + ClipRound((x - screen) / scaleX);
}

int AwtWin32GraphicsDevice::ScaleDownY(int y)
{
    return ClipRound(y / scaleY);
}

int AwtWin32GraphicsDevice::ScaleDownAbsY(int y)
{
    LONG screen = pMonitorInfo->rcMonitor.top;
    return screen + ClipRound((y - screen) / scaleY);
}

int AwtWin32GraphicsDevice::ClipRound(double value)
{
    value -= 0.5;
    if (value < INT_MIN)
    {
        return INT_MIN;
    }

    if (value > INT_MAX)
    {
        return INT_MAX;
    }

    return (int)ceil(value);
}

void AwtWin32GraphicsDevice::InitDesktopScales()
{
    if (!disableScaleAutoRefresh) {
        float dpiX = -1.0f;
        float dpiY = -1.0f;
        GetScreenDpi(GetMonitor(), &dpiX, &dpiY);
        if (dpiX > 0 && dpiY > 0) {
            SetScale(dpiX / 96, dpiY / 96);
        }
    }
}

float AwtWin32GraphicsDevice::GetScaleX()
{
    return scaleX;
}

float AwtWin32GraphicsDevice::GetScaleY()
{
    return scaleY;
}

/**
 * Disables offscreen acceleration for this device.  This
 * sets a flag in the java object that is used to determine
 * whether offscreen surfaces can be created on the device.
 */
void AwtWin32GraphicsDevice::DisableOffscreenAcceleration()
{
    // REMIND: noop for now
}

void AwtWin32GraphicsDevice::DisableScaleAutoRefresh()
{
    disableScaleAutoRefresh = TRUE;
}

/**
 * Invalidates the GraphicsDevice object associated with this
 * device by disabling offscreen acceleration and calling
 * invalidate(defIndex) on the java object.
 */
void AwtWin32GraphicsDevice::Invalidate(JNIEnv *env)
{
    int defIndex = AwtWin32GraphicsDevice::GetDefaultDeviceIndex();
    DisableOffscreenAcceleration();
    jobject javaDevice = GetJavaDevice();
    if (!JNU_IsNull(env, javaDevice)) {
        JNU_CallMethodByName(env, NULL, javaDevice, "invalidate",
                             "(I)V", defIndex);
    }
}

/**
 * Static deviceIndex-based methods
 *
 * The following methods take a deviceIndex for the list of devices
 * and perform the appropriate action on that device.  This way of
 * dereferencing the list of devices allows us to do appropriate
 * locks around the list to ensure multi-threaded safety.
 */


jobject AwtWin32GraphicsDevice::GetColorModel(JNIEnv *env, jboolean dynamic,
                                              int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetColorModel(env, dynamic);
}

LPMONITORINFO AwtWin32GraphicsDevice::GetMonitorInfo(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetMonitorInfo();
}

/**
 * This function updates the data in the MONITORINFOEX structure pointed to by
 * pMonitorInfo for all monitors on the system.  Added for 4654713.
 */
void AwtWin32GraphicsDevice::ResetAllMonitorInfo()
{
    //IE in some circumstances generates WM_SETTINGCHANGE message on appearance
    //and thus triggers this method
    //but we may not have the devices list initialized yet.
    if (!Devices::GetInstance()){
        return;
    }
    Devices::InstanceAccess devices;
    int devicesNum = devices->GetNumDevices();
    for (int deviceIndex = 0; deviceIndex < devicesNum; deviceIndex++) {
        HMONITOR monitor = devices->GetDevice(deviceIndex)->GetMonitor();
        ::GetMonitorInfo(monitor,
                         devices->GetDevice(deviceIndex)->pMonitorInfo);
    }
}

/**
 * This function updates the scale factor for all monitors on the system.
 */
void AwtWin32GraphicsDevice::ResetAllDesktopScales()
{
    if (!Devices::GetInstance()){
        return;
    }
    Devices::InstanceAccess devices;
    int devicesNum = devices->GetNumDevices();
    for (int deviceIndex = 0; deviceIndex < devicesNum; deviceIndex++) {
        devices->GetDevice(deviceIndex)->InitDesktopScales();
    }
}

void AwtWin32GraphicsDevice::DisableOffscreenAccelerationForDevice(
    HMONITOR hMonitor)
{
    Devices::InstanceAccess devices;
    if (hMonitor == NULL) {
        devices->GetDevice(0)->DisableOffscreenAcceleration();
    } else {
        int devicesNum = devices->GetNumDevices();
        for (int i = 0; i < devicesNum; ++i) {
            if (devices->GetDevice(i)->GetMonitor() == hMonitor) {
                devices->GetDevice(i)->DisableOffscreenAcceleration();
            }
        }
    }
}

HMONITOR AwtWin32GraphicsDevice::GetMonitor(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetMonitor();
}

HPALETTE AwtWin32GraphicsDevice::GetPalette(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetPalette();
}

void AwtWin32GraphicsDevice::UpdateDynamicColorModel(int deviceIndex)
{
    Devices::InstanceAccess devices;
    devices->GetDevice(deviceIndex)->UpdateDynamicColorModel();
}

BOOL AwtWin32GraphicsDevice::UpdateSystemPalette(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->UpdateSystemPalette();
}

HPALETTE AwtWin32GraphicsDevice::SelectPalette(HDC hDC, int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->SelectPalette(hDC);
}

void AwtWin32GraphicsDevice::RealizePalette(HDC hDC, int deviceIndex)
{
    Devices::InstanceAccess devices;
    devices->GetDevice(deviceIndex)->RealizePalette(hDC);
}

ColorData *AwtWin32GraphicsDevice::GetColorData(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetColorData();
}

/**
 * Return the grayscale value for the indicated device.
 */
int AwtWin32GraphicsDevice::GetGrayness(int deviceIndex)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(deviceIndex)->GetGrayness();
}

HDC AwtWin32GraphicsDevice::GetDCFromScreen(int screen) {
    J2dTraceLn1(J2D_TRACE_INFO,
                "AwtWin32GraphicsDevice::GetDCFromScreen screen=%d", screen);
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *dev = devices->GetDevice(screen);
    return MakeDCFromMonitor(dev->GetMonitor());
}

/** Compare elements of MONITORINFOEX structures for the given HMONITORs.
 * If equal, return TRUE
 */
BOOL AwtWin32GraphicsDevice::AreSameMonitors(HMONITOR mon1, HMONITOR mon2) {
    J2dTraceLn2(J2D_TRACE_INFO,
                "AwtWin32GraphicsDevice::AreSameMonitors mhnd1=%x mhnd2=%x",
                mon1, mon2);
    DASSERT(mon1 != NULL);
    DASSERT(mon2 != NULL);

    MONITORINFOEX mi1;
    MONITORINFOEX mi2;

    memset((void*)(&mi1), 0, sizeof(MONITORINFOEX));
    mi1.cbSize = sizeof(MONITORINFOEX);
    memset((void*)(&mi2), 0, sizeof(MONITORINFOEX));
    mi2.cbSize = sizeof(MONITORINFOEX);

    if (::GetMonitorInfo(mon1, &mi1) != 0 &&
        ::GetMonitorInfo(mon2, &mi2) != 0 )
    {
        if (::EqualRect(&mi1.rcMonitor, &mi2.rcMonitor) &&
            ::EqualRect(&mi1.rcWork, &mi2.rcWork) &&
            (mi1.dwFlags  == mi1.dwFlags))
        {

            J2dTraceLn(J2D_TRACE_VERBOSE, "  the monitors are the same");
            return TRUE;
        }
    }
    J2dTraceLn(J2D_TRACE_VERBOSE, "  the monitors are not the same");
    return FALSE;
}

int AwtWin32GraphicsDevice::GetScreenFromHMONITOR(HMONITOR mon) {
    J2dTraceLn1(J2D_TRACE_INFO,
                "AwtWin32GraphicsDevice::GetScreenFromHMONITOR mhnd=%x", mon);

    DASSERT(mon != NULL);
    JNIEnv *env = (JNIEnv*) JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (!Devices::GetInstance()) {
       Devices::UpdateInstance(env);
    }
    Devices::InstanceAccess devices;

    for (int i = 0; i < devices->GetNumDevices(); i++) {
        HMONITOR mhnd = devices->GetDevice(i)->GetMonitor();
        if (AreSameMonitors(mon, mhnd)) {
            J2dTraceLn1(J2D_TRACE_VERBOSE, "  Found device: %d", i);
            return i;
        }
    }

    J2dTraceLn1(J2D_TRACE_WARNING,
                "AwtWin32GraphicsDevice::GetScreenFromHMONITOR(): "\
                "couldn't find screen for HMONITOR %x, returning default", mon);
    return AwtWin32GraphicsDevice::GetDefaultDeviceIndex();
}


/**
 * End of static deviceIndex-based methods
 */


    const DWORD REQUIRED_FLAGS = (   //Flags which must be set in
     PFD_SUPPORT_GDI |               //in the PixelFormatDescriptor.
     PFD_DRAW_TO_WINDOW);            //Used to choose the default config
                                     //and to check formats in
                                     //isPixFmtSupported()
extern "C" {

JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsDevice_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    /* class ids */
    jclass iCMClass = env->FindClass("java/awt/image/IndexColorModel");
    CHECK_NULL(iCMClass);
    AwtWin32GraphicsDevice::indexCMClass = (jclass) env->NewGlobalRef(iCMClass);
    env->DeleteLocalRef(iCMClass);
    DASSERT(AwtWin32GraphicsDevice::indexCMClass);
    CHECK_NULL(AwtWin32GraphicsDevice::indexCMClass);

    jclass wTClass = env->FindClass("sun/awt/windows/WToolkit");
    CHECK_NULL(wTClass);
    AwtWin32GraphicsDevice::wToolkitClass = (jclass)env->NewGlobalRef(wTClass);
    env->DeleteLocalRef(wTClass);
    DASSERT(AwtWin32GraphicsDevice::wToolkitClass);
    CHECK_NULL(AwtWin32GraphicsDevice::wToolkitClass);

    /* field ids */
    AwtWin32GraphicsDevice::dynamicColorModelID = env->GetFieldID(cls,
        "dynamicColorModel", "Ljava/awt/image/ColorModel;");
    DASSERT(AwtWin32GraphicsDevice::dynamicColorModelID);
    CHECK_NULL(AwtWin32GraphicsDevice::dynamicColorModelID);

    AwtWin32GraphicsDevice::indexCMrgbID =
        env->GetFieldID(AwtWin32GraphicsDevice::indexCMClass, "rgb", "[I");
    DASSERT(AwtWin32GraphicsDevice::indexCMrgbID);
    CHECK_NULL(AwtWin32GraphicsDevice::indexCMrgbID);

    AwtWin32GraphicsDevice::indexCMcacheID =
        env->GetFieldID(AwtWin32GraphicsDevice::indexCMClass,
        "lookupcache", "[I");
    DASSERT(AwtWin32GraphicsDevice::indexCMcacheID);
    CHECK_NULL(AwtWin32GraphicsDevice::indexCMcacheID);

    /* method ids */
    AwtWin32GraphicsDevice::paletteChangedMID = env->GetStaticMethodID(
        AwtWin32GraphicsDevice::wToolkitClass, "paletteChanged", "()V");
    DASSERT(AwtWin32GraphicsDevice::paletteChangedMID);
    CHECK_NULL(AwtWin32GraphicsDevice::paletteChangedMID);

    // Only want to call this once per session
    make_uns_ordered_dither_array(img_oda_alpha, 256);

    // workaround JDK-6477756, ignore return value to keep dll in memory
    JDK_LoadSystemLibrary("opengl32.dll");

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    getMaxConfigsImpl
 * Signature: ()I
 */

JNIEXPORT jint JNICALL Java_sun_awt_Win32GraphicsDevice_getMaxConfigsImpl
    (JNIEnv* jniEnv, jobject theThis, jint screen) {
        TRY;
    HDC hDC = AwtWin32GraphicsDevice::GetDCFromScreen(screen);

    PIXELFORMATDESCRIPTOR pfd;
    int max = ::DescribePixelFormat(hDC, 1, sizeof(PIXELFORMATDESCRIPTOR),
        &pfd);
    if (hDC != NULL) {
        VERIFY(::DeleteDC(hDC));
        hDC = NULL;
    }
    //If ::DescribePixelFormat() fails, max = 0
    //In this case, we return 1 config with visual number 0
    if (max == 0) {
        max = 1;
    }
    return (jint)max;
        CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    isPixFmtSupported
 * Signature: (I)Z
 */

JNIEXPORT jboolean JNICALL Java_sun_awt_Win32GraphicsDevice_isPixFmtSupported
    (JNIEnv* env, jobject theThis, jint pixFmtID, jint screen) {
        TRY;
    jboolean suppColor = JNI_TRUE;
    HDC hDC = AwtWin32GraphicsDevice::GetDCFromScreen(screen);

    if (pixFmtID == 0) {
        return true;
    }

    PIXELFORMATDESCRIPTOR pfd;
    int max = ::DescribePixelFormat(hDC, (int)pixFmtID,
        sizeof(PIXELFORMATDESCRIPTOR), &pfd);
    DASSERT(max);

    //Check for supported ColorModel
    if ((pfd.cColorBits < 8) ||
       ((pfd.cColorBits == 8) && (pfd.iPixelType != PFD_TYPE_COLORINDEX))) {
        //Note: this still allows for PixelFormats with > 8 color bits
        //which use COLORINDEX instead of RGB.  This seems to work fine,
        //although issues may crop up involving PFD_NEED_PALETTE, which
        //is not currently taken into account.
        //If changes are made, they should also be reflected in
        //getDefaultPixID.
        suppColor = JNI_FALSE;
    }

    if (hDC != NULL) {
        VERIFY(::DeleteDC(hDC));
        hDC = NULL;
    }
    return (((pfd.dwFlags & REQUIRED_FLAGS) == REQUIRED_FLAGS) && suppColor) ?
     JNI_TRUE : JNI_FALSE;
        CATCH_BAD_ALLOC_RET(FALSE);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    getDefaultPixIDImpl
 * Signature: (I)I
 */

JNIEXPORT jint JNICALL Java_sun_awt_Win32GraphicsDevice_getDefaultPixIDImpl
    (JNIEnv* env, jobject theThis, jint screen) {
        TRY;
    int pixFmtID = 0;
    HDC hDC = AwtWin32GraphicsDevice::GetDCFromScreen(screen);

    PIXELFORMATDESCRIPTOR pfd = {
        sizeof(PIXELFORMATDESCRIPTOR),
        1,               //version
        REQUIRED_FLAGS,  //flags
        0,               //iPixelType
        0,               //cColorBits
        0,0,0,0,0,0,0,0, //cRedBits, cRedShift, green, blue, alpha
        0,0,0,0,0,       //cAccumBits, cAccumRedBits, green, blue, alpha
        0,0,0,0,0,0,0,0  //etc.
    };

    //If 8-bit mode, must use Indexed mode
    if (8 == ::GetDeviceCaps(hDC, BITSPIXEL)) {
        pfd.iPixelType = PFD_TYPE_COLORINDEX;
    }

    pixFmtID = ::ChoosePixelFormat(hDC, &pfd);
    if (pixFmtID == 0) {
        //Return 0 if GDI call fails.
        if (hDC != NULL) {
            VERIFY(::DeleteDC(hDC));
            hDC = NULL;
        }
        return pixFmtID;
    }

    if (JNI_FALSE == Java_sun_awt_Win32GraphicsDevice_isPixFmtSupported(
     env, theThis, pixFmtID, screen)) {
        /* Can't find a suitable pixel format ID.  Fall back on 0. */
        pixFmtID = 0;
    }

    VERIFY(::DeleteDC(hDC));
    hDC = NULL;
    return (jint)pixFmtID;
        CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    enterFullScreenExclusive
 * Signature: (Ljava/awt/peer/WindowPeer;)V
 */

JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsDevice_enterFullScreenExclusive(
        JNIEnv* env, jobject graphicsDevice,
        jint screen, jobject windowPeer) {

    TRY;

    PDATA pData;
    JNI_CHECK_PEER_RETURN(windowPeer);

    AwtWindow *window = (AwtWindow *)pData;  // safe cast since we are called
                                             // with the WWindowPeer object
    HWND hWnd = window->GetHWnd();

    if (!::SetWindowPos(hWnd, HWND_TOPMOST, 0, 0, 0, 0,
                        SWP_NOMOVE|SWP_NOOWNERZORDER|SWP_NOSIZE))
    {
        J2dTraceLn1(J2D_TRACE_ERROR,
                    "Error %d setting topmost attribute to fs window",
                    ::GetLastError());
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    exitFullScreenExclusive
 * Signature: (Ljava/awt/peer/WindowPeer;)V
 */

JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsDevice_exitFullScreenExclusive(
        JNIEnv* env, jobject graphicsDevice,
        jint screen, jobject windowPeer) {

    TRY;

    PDATA pData;
    JNI_CHECK_PEER_RETURN(windowPeer);

    AwtWindow *window = (AwtWindow *)pData;  // safe cast since we are called
                                             // with the WWindowPeer object
    HWND hWnd = window->GetHWnd();

    jobject target = env->GetObjectField(windowPeer, AwtObject::targetID);
    jboolean alwaysOnTop = JNU_GetFieldByName(env, NULL, target, "alwaysOnTop", "Z").z;
    env->DeleteLocalRef(target);

    if (!::SetWindowPos(hWnd, HWND_NOTOPMOST, 0, 0, 0, 0,
                        SWP_NOMOVE|SWP_NOOWNERZORDER|SWP_NOSIZE))
    {
        J2dTraceLn1(J2D_TRACE_ERROR,
                    "Error %d unsetting topmost attribute to fs window",
                    ::GetLastError());
    }

    // We should restore alwaysOnTop state as it's anyway dropped here
    Java_sun_awt_windows_WWindowPeer_setAlwaysOnTopNative(env, windowPeer, alwaysOnTop);

    CATCH_BAD_ALLOC;
}

jobject CreateDisplayMode(JNIEnv* env, jint width, jint height,
    jint bitDepth, jint refreshRate) {

    TRY;

    jclass displayModeClass = env->FindClass("java/awt/DisplayMode");
    if (JNU_IsNull(env, displayModeClass)) {
        env->ExceptionClear();
        JNU_ThrowInternalError(env, "Could not get display mode class");
        return NULL;
    }

    jmethodID cid = env->GetMethodID(displayModeClass, "<init>", "(IIII)V");
    if (cid == NULL) {
        env->ExceptionClear();
        JNU_ThrowInternalError(env, "Could not get display mode constructor");
        return NULL;
    }

    jobject displayMode = env->NewObject(displayModeClass, cid, width,
        height, bitDepth, refreshRate);
    return displayMode;

    CATCH_BAD_ALLOC_RET(NULL);
}

/**
 * A utility function which retrieves a DISPLAY_DEVICE information
 * given a screen number.
 *
 * If the function was able to find an attached device for the given screen
 * number, the lpDisplayDevice will be initialized with the data and
 * the function will return TRUE, otherwise it returns FALSE and contents
 * of the structure pointed to by lpDisplayDevice is undefined.
 */
static BOOL
GetAttachedDisplayDevice(int screen, DISPLAY_DEVICE *lpDisplayDevice)
{
    DWORD dwDeviceNum = 0;
    lpDisplayDevice->cb = sizeof(DISPLAY_DEVICE);
    while (EnumDisplayDevices(NULL, dwDeviceNum, lpDisplayDevice, 0) &&
           dwDeviceNum < 20) // avoid infinite loop with buggy drivers
    {
        if (lpDisplayDevice->StateFlags & DISPLAY_DEVICE_ATTACHED_TO_DESKTOP) {
            Devices::InstanceAccess devices;
            MONITORINFOEX *pMonInfo =
                (LPMONITORINFOEX)devices->GetDevice(screen)->GetMonitorInfo();
            // make sure the device names match
            if (wcscmp(pMonInfo->szDevice, lpDisplayDevice->DeviceName) == 0) {
                return TRUE;
            }
        }
        dwDeviceNum++;
    }
    return FALSE;
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    getCurrentDisplayMode
 * Signature: (IZ)Ljava/awt/DisplayMode;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_Win32GraphicsDevice_getCurrentDisplayMode
    (JNIEnv* env, jobject graphicsDevice, jint screen)
{
    TRY;

    DEVMODE dm;
    LPTSTR pName = NULL;

    dm.dmSize = sizeof(dm);
    dm.dmDriverExtra = 0;

    DISPLAY_DEVICE displayDevice;
    if (GetAttachedDisplayDevice(screen, &displayDevice)) {
        pName = displayDevice.DeviceName;
    }
    if (!EnumDisplaySettings(pName, ENUM_CURRENT_SETTINGS, &dm))
    {
        return NULL;
    }

    return CreateDisplayMode(env, dm.dmPelsWidth,
        dm.dmPelsHeight, dm.dmBitsPerPel, dm.dmDisplayFrequency);

    CATCH_BAD_ALLOC_RET(NULL);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    configDisplayMode
 * Signature: (IIIIZ)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsDevice_configDisplayMode
    (JNIEnv* env, jobject graphicsDevice, jint screen, jobject windowPeer,
     jint width, jint height, jint bitDepth, jint refreshRate)
{
    TRY;

        DEVMODE dm;

    dm.dmSize = sizeof(dm);
    dm.dmDriverExtra = 0;
    dm.dmPelsWidth = width;
    dm.dmPelsHeight = height;
    dm.dmBitsPerPel = bitDepth;
    dm.dmDisplayFrequency = refreshRate;
    dm.dmFields = DM_PELSWIDTH | DM_PELSHEIGHT |
        DM_BITSPERPEL | DM_DISPLAYFREQUENCY;

    // ChangeDisplaySettings works only on the primary screen.
    // ChangeDisplaySettingsEx is not available on NT,
    // so it'd be nice not to break it if we can help it.
    if (screen == AwtWin32GraphicsDevice::GetDefaultDeviceIndex()) {
        if (::ChangeDisplaySettings(&dm, CDS_FULLSCREEN) !=
            DISP_CHANGE_SUCCESSFUL)
        {
            JNU_ThrowInternalError(env,
                                   "Could not set display mode");
        }
        return;
    }

    DISPLAY_DEVICE displayDevice;
    if (!GetAttachedDisplayDevice(screen, &displayDevice) ||
        (::ChangeDisplaySettingsEx(displayDevice.DeviceName, &dm, NULL, CDS_FULLSCREEN, NULL) !=
          DISP_CHANGE_SUCCESSFUL))
    {
        JNU_ThrowInternalError(env,
                               "Could not set display mode");
    }

    CATCH_BAD_ALLOC;
}

class EnumDisplayModeParam {
public:
    EnumDisplayModeParam(JNIEnv* e, jobject a) : env(e), arrayList(a) {}
    JNIEnv* env;
    jobject arrayList;
};

void addDisplayMode(JNIEnv* env, jobject arrayList, jint width,
    jint height, jint bitDepth, jint refreshRate) {

    TRY;

    jobject displayMode = CreateDisplayMode(env, width, height,
        bitDepth, refreshRate);
    if (!JNU_IsNull(env, displayMode)) {
        jclass arrayListClass = env->GetObjectClass(arrayList);
        if (JNU_IsNull(env, arrayListClass)) {
            JNU_ThrowInternalError(env,
                "Could not get class java.util.ArrayList");
            return;
        }
        jmethodID mid = env->GetMethodID(arrayListClass, "add",
        "(Ljava/lang/Object;)Z");
        if (mid == NULL) {
            env->ExceptionClear();
            JNU_ThrowInternalError(env,
                "Could not get method java.util.ArrayList.add()");
            return;
        }
        env->CallObjectMethod(arrayList, mid, displayMode);
        env->DeleteLocalRef(displayMode);
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    enumDisplayModes
 * Signature: (Ljava/util/ArrayList;Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_Win32GraphicsDevice_enumDisplayModes
    (JNIEnv* env, jobject graphicsDevice, jint screen, jobject arrayList)
{

    TRY;

    DEVMODE dm;
    LPTSTR pName = NULL;
    DISPLAY_DEVICE displayDevice;


    if (GetAttachedDisplayDevice(screen, &displayDevice)) {
        pName = displayDevice.DeviceName;
    }

    dm.dmSize = sizeof(dm);
    dm.dmDriverExtra = 0;

    BOOL bContinue = TRUE;
    for (int i = 0; bContinue; i++) {
        bContinue = EnumDisplaySettings(pName, i, &dm);
        if (dm.dmBitsPerPel >= 8) {
            addDisplayMode(env, arrayList, dm.dmPelsWidth, dm.dmPelsHeight,
                           dm.dmBitsPerPel, dm.dmDisplayFrequency);
            JNU_CHECK_EXCEPTION(env);
        }
    }

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    makeColorModel
 * Signature: ()Ljava/awt/image/ColorModel
 */

JNIEXPORT jobject JNICALL
    Java_sun_awt_Win32GraphicsDevice_makeColorModel
    (JNIEnv *env, jobject thisPtr, jint screen, jboolean dynamic)
{
    Devices::InstanceAccess devices;
    return devices->GetDevice(screen)->GetColorModel(env, dynamic);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    initDevice
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
    Java_sun_awt_Win32GraphicsDevice_initDevice
    (JNIEnv *env, jobject thisPtr, jint screen)
{
    Devices::InstanceAccess devices;
    devices->GetDevice(screen)->SetJavaDevice(env, thisPtr);
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    setNativeScale
 * Signature: (I,F,F)V
 */
JNIEXPORT void JNICALL
    Java_sun_awt_Win32GraphicsDevice_setNativeScale
    (JNIEnv *env, jobject thisPtr, jint screen, jfloat scaleX, jfloat scaleY)
{
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(screen);

    if (device != NULL ) {
        device->DisableScaleAutoRefresh();
        device->SetScale(scaleX, scaleY);
    }
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    getNativeScaleX
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL
    Java_sun_awt_Win32GraphicsDevice_getNativeScaleX
    (JNIEnv *env, jobject thisPtr, jint screen)
{
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(screen);
    return (device == NULL) ? 1 : device->GetScaleX();
}

/*
 * Class:     sun_awt_Win32GraphicsDevice
 * Method:    getNativeScaleY
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL
    Java_sun_awt_Win32GraphicsDevice_getNativeScaleY
    (JNIEnv *env, jobject thisPtr, jint screen)
{
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(screen);
    return (device == NULL) ? 1 : device->GetScaleY();
}

/*
* Class:     sun_awt_Win32GraphicsDevice
* Method:    initNativeScale
* Signature: (I)V;
*/
JNIEXPORT void JNICALL
Java_sun_awt_Win32GraphicsDevice_initNativeScale
(JNIEnv *env, jobject thisPtr, jint screen)
{
    Devices::InstanceAccess devices;
    AwtWin32GraphicsDevice *device = devices->GetDevice(screen);

    if (device != NULL) {
        device->InitDesktopScales();
    }
}
