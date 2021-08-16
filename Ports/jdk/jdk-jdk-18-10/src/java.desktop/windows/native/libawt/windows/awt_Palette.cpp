/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include "awt_Palette.h"
#include "awt_Component.h"
#include "img_util_md.h"
#include "awt_CustomPaletteDef.h"
#include "Trace.h"

#define ERROR_GRAY (-1)
#define NON_GRAY 0
#define LINEAR_STATIC_GRAY 1
#define NON_LINEAR_STATIC_GRAY 2

/**
 * Select the palette into the given HDC.  This will
 * allow operations using this HDC to access the palette
 * colors/indices.
 */
HPALETTE AwtPalette::Select(HDC hDC)
{
    HPALETTE prevPalette = NULL;
    if (logicalPalette) {
        prevPalette = ::SelectPalette(hDC, logicalPalette, FALSE);
    }
    return prevPalette;
}

/**
 * Realize the palette of the given HDC.  This will attempt to
 * install the palette of the HDC onto the device associated with
 * that HDC.
 */
void AwtPalette::Realize(HDC hDC)
{
    if (logicalPalette) {
        if (AwtComponent::QueryNewPaletteCalled() ||
            AwtToolkit::GetInstance().HasDisplayChanged()) {
            // Fix for bug 4178909, workaround for Windows bug.  Shouldn't
            // do a RealizePalette until the first QueryNewPalette message
            // has been processed.
            // But if we are switching the primary monitor from non-8bpp
            // to 8bpp mode, we may not get any palette messages during
            // the display change event.  Go ahead and realize the palette
            // now anyway in this situation.  This was especially noticeable
            // on win2k in multimon.  Note that there still seems to be some
            // problem with actually setting the palette on the primary
            // screen until after QNP is called, but at least the
            // secondary devices can correctly realize the palette.
            ::RealizePalette(hDC);
        }
    }
}

/**
 * Constructor.  Initialize the system and logical palettes.
 * used by this object.
 */
AwtPalette::AwtPalette(AwtWin32GraphicsDevice *device)
{
    this->device = device;
    Update();
    UpdateLogical();
}

/**
 * Retrieves system palette entries. Includes a workaround for some
 * video drivers which may not support the GSPE call but may return
 * valid values from this procedure.
 */
int AwtPalette::FetchPaletteEntries(HDC hDC, PALETTEENTRY* pPalEntries)
{
    LOGPALETTE* pLogPal = 0;
    HPALETTE hPal = 0;
    HPALETTE hPalOld = 0;
    int numEntries;

    numEntries = ::GetSystemPaletteEntries(hDC, 0, 256, pPalEntries);

    if (numEntries > 0) {
        return numEntries;
    }
    // Workaround: some drivers do not support GetSysPalEntries

    pLogPal = (LOGPALETTE*) new char[sizeof(LOGPALETTE)
                                    + 256*sizeof(PALETTEENTRY)];
    if (pLogPal == NULL) {
        return 0;
    }

    pLogPal->palVersion = 0x300;
    pLogPal->palNumEntries = 256;
    int iEntry;
    PALETTEENTRY* pEntry;
    for (iEntry = 0; iEntry < 256; iEntry++) {
        pEntry = pLogPal->palPalEntry + iEntry;
        pEntry->peRed = iEntry;
        pEntry->peGreen = pEntry->peBlue = 0;
        pEntry->peFlags = PC_EXPLICIT;
    }
    hPal = ::CreatePalette(pLogPal);
    delete[] pLogPal;
    if ( hPal == 0 ) {
        return 0;
    }

    hPalOld = ::SelectPalette(hDC, hPal, TRUE);
    if (hPalOld == 0) {
        ::DeleteObject(hPal);
        return 0;
    }
    ::RealizePalette(hDC);

    COLORREF rgb;
    for (iEntry = 0; iEntry < 256; iEntry++) {
        rgb = ::GetNearestColor(hDC, PALETTEINDEX(iEntry));
        pPalEntries[iEntry].peRed = GetRValue(rgb);
        pPalEntries[iEntry].peGreen = GetGValue(rgb);
        pPalEntries[iEntry].peBlue = GetBValue(rgb);
    }

    ::SelectPalette(hDC, hPalOld, FALSE);
    ::DeleteObject(hPal);
    ::RealizePalette(hDC);

    return 256;
}

int AwtPalette::GetGSType(PALETTEENTRY* pPalEntries)
{
    int isGray = 1;
    int isLinearStaticGray = 1;
    int isNonLinearStaticGray = 1;
    int iEntry;
    char bUsed[256];
    BYTE r, g, b;

    memset(bUsed, 0, sizeof(bUsed));
    for (iEntry = 0; iEntry < 256; iEntry++) {
        r = pPalEntries[iEntry].peRed;
        g = pPalEntries[iEntry].peGreen;
        b = pPalEntries[iEntry].peBlue;
        if (r != g || r != b) {
            isGray = 0;
            break;
        } else {
            // the values are gray
            if (r != iEntry) {
                // it's not linear
                // but it could be non-linear static gray
                isLinearStaticGray = 0;
            }
            bUsed[r] = 1;
        }
    }

    if (isGray && !isLinearStaticGray) {
        // check if all 256 grays are there
        // if that's the case, it's non-linear static gray
        for (iEntry = 0; iEntry < 256; iEntry++ ) {
            if (!bUsed[iEntry]) {
                // not non-linear (not all 256 colors are used)
                isNonLinearStaticGray = 0;
                break;
            }
        }
    }

    if (!isGray) {
        J2dTraceLn(J2D_TRACE_INFO,
                   "Detected palette: NON_GRAY/USER-MODIFIABLE");
        return NON_GRAY;
    }
    if (isLinearStaticGray) {
        J2dTraceLn(J2D_TRACE_INFO,
                   "Detected palette: LINEAR_STATIC_GRAY");
        return LINEAR_STATIC_GRAY;
    }
    if (isNonLinearStaticGray) {
        J2dTraceLn(J2D_TRACE_INFO,
                   "Detected palette: NON_LINEAR_STATIC_GRAY");
        return NON_LINEAR_STATIC_GRAY;
    }

    J2dTraceLn(J2D_TRACE_ERROR,
               "Unable to detect palette type, non-gray is assumed");
    // not supposed to be here, error
    return ERROR_GRAY;
}

/**
 * Updates our system palette variables to make sure they match
 * the current state of the actual system palette.  This method
 * is called during AwtPalette creation and after palette changes.
 * Return whether there were any palette changes from the previous
 * system palette.
 */
BOOL AwtPalette::Update()
{
    PALETTEENTRY pe[256];
    int numEntries = 0;
    int bitsPerPixel;
    int i;
    HDC hDC;

    hDC = device->GetDC();
    if (!hDC) {
        return FALSE;
    }
    bitsPerPixel = ::GetDeviceCaps(hDC, BITSPIXEL);
    device->ReleaseDC(hDC);
    if (8 != bitsPerPixel) {
        return FALSE;
    }

    hDC = device->GetDC();
    numEntries = FetchPaletteEntries(hDC, pe);

    device->ReleaseDC(hDC);

    if ((numEntries == numSystemEntries) &&
        (0 == memcmp(pe, systemEntriesWin32, numEntries * sizeof(PALETTEENTRY))))
    {
        return FALSE;
    }

    // make this system palette the new cached win32 palette
    numEntries = (numEntries > 256)? 256: numEntries;
    memcpy(systemEntriesWin32, pe, numEntries * sizeof(PALETTEENTRY));
    numSystemEntries = numEntries;

    // Create jdk-style system palette
    int startIndex = 0, endIndex = numEntries-1;
    int staticGrayType = GetGSType(systemEntriesWin32);

    if (staticGrayType == LINEAR_STATIC_GRAY) {
        device->SetGrayness(GS_STATICGRAY);
    } else if (staticGrayType == NON_LINEAR_STATIC_GRAY) {
        device->SetGrayness(GS_NONLINGRAY);
    } else if (getenv("FORCEGRAY")) {
        J2dTraceLn(J2D_TRACE_INFO,
                    "Gray Palette Forced via FORCEGRAY");
        // Need to zero first and last ten
        // palette entries. Otherwise in UpdateDynamicColorModel
        // we could set non-gray values to the palette.
        for (i = 0; i < 10; i++) {
            systemEntries[i] = 0x00000000;
            systemEntries[i+246] = 0x00000000;
        }
        numEntries -= 20;
        startIndex = 10;
        endIndex -= 10;
        device->SetGrayness(GS_INDEXGRAY);
    } else {
        device->SetGrayness(GS_NOTGRAY);
    }

    for (i = startIndex; i <= endIndex; i++) {
        systemEntries[i] =  0xff000000
                        | (pe[i].peRed << 16)
                        | (pe[i].peGreen << 8)
                        | (pe[i].peBlue);
    }

    systemInverseLUT =
        initCubemap((int *)systemEntries, numEntries, 32);

    ColorData *cData = device->GetColorData();
    if ((device->GetGrayness() == GS_NONLINGRAY ||
         device->GetGrayness() == GS_INDEXGRAY) &&
        cData != NULL) {

        if (cData->pGrayInverseLutData != NULL) {
            free(cData->pGrayInverseLutData);
            cData->pGrayInverseLutData = NULL;
        }
        initInverseGrayLut((int*)systemEntries, 256, device->GetColorData());
    }

    return TRUE;
}


/**
 * Creates our custom palette based on: the current system palette,
 * the grayscale-ness of the system palette, and the state of the
 * primary device.
 */
void AwtPalette::UpdateLogical()
{
    // Create and initialize a palette
    int nEntries = 256;
    char *buf = NULL;
    buf = new char[sizeof(LOGPALETTE) + nEntries *
        sizeof(PALETTEENTRY)];

    LOGPALETTE *pLogPal = (LOGPALETTE*)buf;
    PALETTEENTRY *pPalEntries = (PALETTEENTRY *)(&(pLogPal->palPalEntry[0]));

    memcpy(pPalEntries, systemEntriesWin32, 256 * sizeof(PALETTEENTRY));

    PALETTEENTRY *pPal = pPalEntries;
    int i;
    int staticGrayType = device->GetGrayness();
    if (staticGrayType == GS_INDEXGRAY) {
        float m = 255.0f / 235.0f;
        float g = 0.5f;
        pPal = &pPalEntries[10];
        for (i = 10; i < 246; i++, pPal++) {
            pPal->peRed = pPal->peGreen = pPal->peBlue =
                (int)g;
            g += m;
            pPal->peFlags = PC_NOCOLLAPSE;
        }
    } else if (staticGrayType == GS_NOTGRAY) {
        for (i = 10; i < 246; i++) {
            pPalEntries[i] = customPalette[i-10];
        }
    }
    pLogPal->palNumEntries = 256;
    pLogPal->palVersion = 0x300;
    logicalPalette = ::CreatePalette(pLogPal);

    for (i = 0; i < nEntries; i++) {
        logicalEntries[i] =  0xff000000
                        | (pPalEntries[i].peRed << 16)
                        | (pPalEntries[i].peGreen << 8)
                        | (pPalEntries[i].peBlue);
    }
    delete [] buf;
}
