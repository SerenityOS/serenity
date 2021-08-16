/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

// copy from awt.h
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif

// copy from awt.h
#ifndef _WIN32_IE
#define _WIN32_IE 0x0600
#endif

#include "splashscreen_impl.h"
#include <windowsx.h>
#include <windows.h>
#include <winuser.h>
#include "sizecalc.h"

#ifndef WS_EX_LAYERED
#define WS_EX_LAYERED 0x80000
#endif

#ifndef ULW_ALPHA
#define ULW_ALPHA               0x00000002
#endif

#ifndef AC_SRC_OVER
#define AC_SRC_OVER                 0x00
#endif

#ifndef AC_SRC_ALPHA
#define AC_SRC_ALPHA                0x01
#endif

#define WM_SPLASHUPDATE         WM_USER+1
#define WM_SPLASHRECONFIGURE    WM_USER+2

#define BUFF_SIZE 1024

/* Could use npt but decided to cut down on linked code size */
char* SplashConvertStringAlloc(const char* in, int *size) {
    int len, outChars, rc;
    WCHAR* buf;
    if (!in) {
        return NULL;
    }
    len = strlen(in);
    outChars = MultiByteToWideChar(CP_ACP, MB_PRECOMPOSED, in, len,
                                       NULL, 0);
    buf = (WCHAR*) SAFE_SIZE_ARRAY_ALLOC(malloc, outChars, sizeof(WCHAR));
    if (!buf) {
        return NULL;
    }
    rc = MultiByteToWideChar(CP_ACP, MB_PRECOMPOSED, in, len,
                                 buf, outChars);
    if (rc==0) {
        free(buf);
        return NULL;
    } else {
        if (size) {
            *size = rc;
        }
        return (char*)buf;
    }
}

unsigned
SplashTime(void)
{
    return GetTickCount();
}

void
SplashInitFrameShape(Splash * splash, int imageIndex)
{
    RGNDATA *pRgnData;
    RGNDATAHEADER *pRgnHdr;
    ImageRect maskRect;

    if (!splash->maskRequired)
        return;

    /* reserving memory for the worst case */
    if (!IS_SAFE_SIZE_MUL(splash->width / 2 + 1, splash->height)) {
        return;
    }
    pRgnData = (RGNDATA *) SAFE_SIZE_STRUCT_ALLOC(malloc, sizeof(RGNDATAHEADER),
            sizeof(RECT), (splash->width / 2 + 1) * splash->height);
    if (!pRgnData) {
        return;
    }
    pRgnHdr = (RGNDATAHEADER *) pRgnData;
    initRect(&maskRect, 0, 0, splash->width, splash->height, 1,
            splash->width * splash->imageFormat.depthBytes,
            splash->frames[imageIndex].bitmapBits, &splash->imageFormat);

    pRgnHdr->dwSize = sizeof(RGNDATAHEADER);
    pRgnHdr->iType = RDH_RECTANGLES;
    pRgnHdr->nRgnSize = 0;
    pRgnHdr->rcBound.top = 0;
    pRgnHdr->rcBound.left = 0;
    pRgnHdr->rcBound.bottom = splash->height;
    pRgnHdr->rcBound.right = splash->width;

    pRgnHdr->nCount = BitmapToYXBandedRectangles(&maskRect,
            (RECT *) (((BYTE *) pRgnData) + sizeof(RGNDATAHEADER)));

    splash->frames[imageIndex].hRgn = ExtCreateRegion(NULL,
            sizeof(RGNDATAHEADER) + sizeof(RECT) * pRgnHdr->nCount, pRgnData);

    free(pRgnData);
}

/* paint current splash screen frame to hdc
   this function is unused in layered window mode */

void
SplashPaint(Splash * splash, HDC hdc)
{
    unsigned numColors = splash->screenFormat.colorMap ?
        splash->screenFormat.numColors : 0;
    BITMAPV4HEADER *pBmi;
    HPALETTE hOldPal = NULL;

    if (!splash->frames)
        return;
    if (splash->currentFrame < 0 || splash->currentFrame >= splash->frameCount)
        return;
    pBmi = (BITMAPV4HEADER *) SAFE_SIZE_STRUCT_ALLOC(alloca, sizeof(BITMAPV4HEADER),
            sizeof(RGBQUAD), numColors);
    if (!pBmi) {
        return;
    }
    memset(pBmi, 0, sizeof(BITMAPV4HEADER));
    if (splash->screenFormat.colorMap)
        memcpy(((BYTE *) pBmi) + sizeof(BITMAPV4HEADER),
                splash->screenFormat.colorMap, sizeof(RGBQUAD) * numColors);

    pBmi->bV4Size = sizeof(BITMAPV4HEADER);
    pBmi->bV4Width = splash->width;
    pBmi->bV4Height = -splash->height;
    pBmi->bV4Planes = 1;
    pBmi->bV4BitCount = (WORD) (splash->screenFormat.depthBytes * 8);
    /* we're ALWAYS using BGRA in screenFormat */
    pBmi->bV4V4Compression = BI_RGB;
    pBmi->bV4ClrUsed = numColors;
    pBmi->bV4ClrImportant = numColors;
    pBmi->bV4AlphaMask = splash->screenFormat.mask[3];
    pBmi->bV4RedMask = splash->screenFormat.mask[2];
    pBmi->bV4GreenMask = splash->screenFormat.mask[1];
    pBmi->bV4BlueMask = splash->screenFormat.mask[0];

    /*  creating the palette in SplashInitPlatform does not work, so I'm creating it
       here on demand */
    if (!splash->hPalette) {
        unsigned i;
        LOGPALETTE *pLogPal = (LOGPALETTE *) SAFE_SIZE_STRUCT_ALLOC(malloc,
                sizeof(LOGPALETTE), sizeof(PALETTEENTRY), numColors);
        if (!pLogPal) {
            return;
        }

        pLogPal->palVersion = 0x300;
        pLogPal->palNumEntries = (WORD) numColors;
        for (i = 0; i < numColors; i++) {
            pLogPal->palPalEntry[i].peRed = (BYTE)
                QUAD_RED(splash->colorMap[i]);
            pLogPal->palPalEntry[i].peGreen = (BYTE)
                QUAD_GREEN(splash->colorMap[i]);
            pLogPal->palPalEntry[i].peBlue = (BYTE)
                QUAD_BLUE(splash->colorMap[i]);
            pLogPal->palPalEntry[i].peFlags = PC_NOCOLLAPSE;
        }
        splash->hPalette = CreatePalette(pLogPal);
        free(pLogPal);
    }
    if (splash->hPalette) {
        hOldPal = SelectPalette(hdc, splash->hPalette, FALSE);
        RealizePalette(hdc);
    }

    StretchDIBits(hdc, 0, 0, splash->width, splash->height, 0, 0,
            splash->width, splash->height, splash->screenData,
            (BITMAPINFO *) pBmi, DIB_RGB_COLORS, SRCCOPY);
    if (hOldPal)
        SelectPalette(hdc, hOldPal, FALSE);
}


/* The function makes the window visible if it is hidden
 or is not yet shown. */
void
SplashRedrawWindow(Splash * splash)
{
    if (!SplashIsStillLooping(splash)) {
        KillTimer(splash->hWnd, 0);
    }

    if (splash->currentFrame < 0) {
        return;
    }

    SplashUpdateScreenData(splash);
    if (splash->isLayered) {
        BLENDFUNCTION bf;
        POINT ptSrc;
        HDC hdcSrc = CreateCompatibleDC(NULL), hdcDst;
        BITMAPINFOHEADER bmi;
        void *bitmapBits;
        HBITMAP hBitmap, hOldBitmap;
        RECT rect;
        POINT ptDst;
        SIZE size;

        bf.BlendOp = AC_SRC_OVER;
        bf.BlendFlags = 0;
        bf.AlphaFormat = AC_SRC_ALPHA;
        bf.SourceConstantAlpha = 0xFF;
        ptSrc.x = ptSrc.y = 0;

        memset(&bmi, 0, sizeof(bmi));
        bmi.biSize = sizeof(BITMAPINFOHEADER);
        bmi.biWidth = splash->width;
        bmi.biHeight = -splash->height;
        bmi.biPlanes = 1;
        bmi.biBitCount = 32;
        bmi.biCompression = BI_RGB;

        //      FIXME: this is somewhat ineffective
        //      maybe if we allocate memory for all frames as DIBSections,
        //      then we could select the frames into the DC directly

        hBitmap = CreateDIBSection(NULL, (BITMAPINFO *) & bmi, DIB_RGB_COLORS,
                &bitmapBits, NULL, 0);
        memcpy(bitmapBits, splash->screenData,
                splash->screenStride * splash->height);
        hOldBitmap = (HBITMAP) SelectObject(hdcSrc, hBitmap);
        hdcDst = GetDC(splash->hWnd);

        GetWindowRect(splash->hWnd, &rect);

        ptDst.x = rect.left;
        ptDst.y = rect.top;

        size.cx = splash->width;
        size.cy = splash->height;

        UpdateLayeredWindow(splash->hWnd, hdcDst, &ptDst, &size,
                hdcSrc, &ptSrc, 0, &bf, ULW_ALPHA);

        ReleaseDC(splash->hWnd, hdcDst);
        SelectObject(hdcSrc, hOldBitmap);
        DeleteObject(hBitmap);
        DeleteDC(hdcSrc);
    }
    else {
       InvalidateRect(splash->hWnd, NULL, FALSE);
       if (splash->maskRequired) {
            HRGN hRgn = CreateRectRgn(0, 0, 0, 0);

            CombineRgn(hRgn, splash->frames[splash->currentFrame].hRgn,
                    splash->frames[splash->currentFrame].hRgn, RGN_COPY);
            SetWindowRgn(splash->hWnd, hRgn, TRUE);
        } else {
            SetWindowRgn(splash->hWnd, NULL, TRUE);
        }
        UpdateWindow(splash->hWnd);
    }
    if (!IsWindowVisible(splash->hWnd)) {
        POINT cursorPos;
        ShowWindow(splash->hWnd, SW_SHOW);
        // Windows won't update the cursor after the window is shown,
        // if the cursor is already above the window. need to do this manually.
        GetCursorPos(&cursorPos);
        if (WindowFromPoint(cursorPos) == splash->hWnd) {
            // unfortunately Windows fail to understand that the window
            // thread should own the cursor, even though the mouse pointer
            // is over the window, until the mouse has been moved.
            // we're using SetCursorPos here to fake the mouse movement
            // and enable proper update of the cursor.
            SetCursorPos(cursorPos.x, cursorPos.y);
            SetCursor(LoadCursor(NULL, IDC_WAIT));
        }
    }
    if (SplashIsStillLooping(splash)) {
        int time = splash->time +
            splash->frames[splash->currentFrame].delay - SplashTime();

        if (time < 0)
            time = 0;
        SetTimer(splash->hWnd, 0, time, NULL);
    }
}

void SplashReconfigureNow(Splash * splash) {
    splash->x = (GetSystemMetrics(SM_CXSCREEN) - splash->width) / 2;
    splash->y = (GetSystemMetrics(SM_CYSCREEN) - splash->height) / 2;
    if (splash->hWnd) {
        //Fixed 6474657: splash screen image jumps towards left while
        //    setting the new image using setImageURL()
        // We may safely hide the splash window because SplashRedrawWindow()
        //    will show the window again.
        ShowWindow(splash->hWnd, SW_HIDE);
        MoveWindow(splash->hWnd, splash->x, splash->y, splash->width, splash->height, FALSE);
    }
    SplashRedrawWindow(splash);
}

static LRESULT CALLBACK
SplashWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    PAINTSTRUCT ps;
    HDC hdc;


    switch (message) {

    case WM_ERASEBKGND:
        return TRUE;            // to avoid flicker

    case WM_SYSCOMMAND:
        if (wParam==SC_CLOSE||wParam==SC_DEFAULT||wParam==SC_HOTKEY||
            wParam==SC_KEYMENU||wParam==SC_MAXIMIZE||
            wParam==SC_MINIMIZE||wParam==SC_MOUSEMENU||wParam==SC_MOVE||
            wParam==SC_RESTORE||wParam==SC_SIZE)
        {
            return 0;
        }

    /* double switch to avoid prologue/epilogue duplication */
    case WM_TIMER:
    case WM_SPLASHUPDATE:
    case WM_PAINT:
    case WM_SPLASHRECONFIGURE:
        {
            Splash *splash = (Splash *) GetWindowLongPtr(hWnd, GWLP_USERDATA);

            SplashLock(splash);
            if (splash->isVisible>0) {
                switch(message) {
                case WM_TIMER:
                    SplashNextFrame(splash);
                    SplashRedrawWindow(splash);
                    break;
                case WM_SPLASHUPDATE:
                    SplashRedrawWindow(splash);
                    break;
                case WM_PAINT:
                    hdc = BeginPaint(hWnd, &ps);
                    SplashPaint(splash, hdc);
                    EndPaint(hWnd, &ps);
                    break;
                case WM_SPLASHRECONFIGURE:
                    SplashReconfigureNow(splash);
                    break;
                }
            }
            SplashUnlock(splash);
            break;
        }
    case WM_DESTROY:
        PostQuitMessage(0);
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);

    }
    return 0;
}

HWND
SplashCreateWindow(Splash * splash)
{
    WNDCLASSEX wcex;
    ATOM wndClass;
    DWORD style, exStyle;
    HWND hWnd;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));

    wcex.cbSize = sizeof(WNDCLASSEX);
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = (WNDPROC) SplashWndProc;
    wcex.hInstance = GetModuleHandle(NULL);
    wcex.lpszClassName = "JavaSplash";
    wcex.hCursor = LoadCursor(NULL, IDC_WAIT);

    wndClass = RegisterClassEx(&wcex);
    if (!wndClass) {
        return 0;
    }

    splash->x = (GetSystemMetrics(SM_CXSCREEN) - splash->width) / 2;
    splash->y = (GetSystemMetrics(SM_CYSCREEN) - splash->height) / 2;
    exStyle = splash->isLayered ? WS_EX_LAYERED : 0;
    exStyle |= WS_EX_TOOLWINDOW;        /* don't show the window on taskbar */
    style = WS_POPUP;
    hWnd = CreateWindowEx(exStyle, (LPCSTR) wndClass, "", style,
            splash->x, splash->y, splash->width, splash->height, NULL, NULL,
            wcex.hInstance, NULL);
    SetWindowLongPtr(hWnd, GWLP_USERDATA, (LONG_PTR) splash);
    return hWnd;
}

void
SplashLock(Splash * splash)
{
    EnterCriticalSection(&splash->lock);
}

void
SplashUnlock(Splash * splash)
{
    LeaveCriticalSection(&splash->lock);
}

int
SplashInitPlatform(Splash * splash)
{
    HDC hdc;
    int paletteMode;

    InitializeCriticalSection(&splash->lock);
    splash->isLayered = FALSE;
    hdc = GetDC(NULL);
    paletteMode = (GetDeviceCaps(hdc, RASTERCAPS) & RC_PALETTE) != 0;
    if (UpdateLayeredWindow && !paletteMode) {
        splash->isLayered = TRUE;
    }
    splash->byteAlignment = 4;
    if (splash->isLayered) {
        initFormat(&splash->screenFormat,
                0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
        splash->screenFormat.premultiplied = 1;
        splash->maskRequired = 0;
    }
    else {
        splash->maskRequired = 1;
        if (paletteMode) {
            int numColors = GetDeviceCaps(hdc, SIZEPALETTE) -
                GetDeviceCaps(hdc, NUMRESERVED);
            int i;
            int numComponents[3];

            initFormat(&splash->screenFormat, 0, 0, 0, 0);
            /*      FIXME: maybe remapping to non-reserved colors would improve performance */
            for (i = 0; i < numColors; i++) {
                splash->colorIndex[i] = i;
            }
            numColors = quantizeColors(numColors, numComponents);
            initColorCube(numComponents, splash->colorMap, splash->dithers,
                    splash->colorIndex);
            splash->screenFormat.colorIndex = splash->colorIndex;
            splash->screenFormat.depthBytes = 1;
            splash->screenFormat.colorMap = splash->colorMap;
            splash->screenFormat.dithers = splash->dithers;
            splash->screenFormat.numColors = numColors;
            splash->hPalette = NULL;
        }
        else {
            initFormat(&splash->screenFormat,
                    0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
        }
    }
    ReleaseDC(NULL, hdc);
    return 1;
}

void
SplashCleanupPlatform(Splash * splash)
{
    int i;

    if (splash->frames) {
        for (i = 0; i < splash->frameCount; i++) {
            if (splash->frames[i].hRgn) {
                DeleteObject(splash->frames[i].hRgn);
                splash->frames[i].hRgn = NULL;
            }
        }
    }
    if (splash->hPalette)
        DeleteObject(splash->hPalette);
    splash->maskRequired = !splash->isLayered;
}

void
SplashDonePlatform(Splash * splash)
{
    if (splash->hWnd)
        DestroyWindow(splash->hWnd);
}

void
SplashMessagePump()
{
    MSG msg;

    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
}

DWORD WINAPI
SplashScreenThread(LPVOID param)
{
    Splash *splash = (Splash *) param;

    splash->currentFrame = 0;
    SplashLock(splash);
    splash->time = SplashTime();
    splash->hWnd = SplashCreateWindow(splash);
    if (splash->hWnd) {
        SplashRedrawWindow(splash);
        //map the splash co-ordinates as per system scale
        splash->x /= splash->scaleFactor;
        splash->y /= splash->scaleFactor;
        SplashUnlock(splash);
        SplashMessagePump();
        SplashLock(splash);
    }
    SplashDone(splash);
    splash->isVisible = -1;
    SplashUnlock(splash);
    return 0;
}

void
SplashCreateThread(Splash * splash)
{
    DWORD threadId;

    CreateThread(NULL, 0, SplashScreenThread, (LPVOID) splash, 0, &threadId);
}

void
SplashClosePlatform(Splash * splash)
{
    PostMessage(splash->hWnd, WM_QUIT, 0, 0);
}

void
SplashUpdate(Splash * splash)
{
    PostMessage(splash->hWnd, WM_SPLASHUPDATE, 0, 0);
}

void
SplashReconfigure(Splash * splash)
{
    PostMessage(splash->hWnd, WM_SPLASHRECONFIGURE, 0, 0);
}

JNIEXPORT jboolean
SplashGetScaledImageName(const char* jarName, const char* fileName,
                           float *scaleFactor, char *scaleImageName,
                           const size_t scaledImageLength)
{
    float dpiScaleX = -1.0f;
    float dpiScaleY = -1.0f;
    FILE *fp = NULL;
    *scaleFactor = 1.0;
    GetScreenDpi(getPrimaryMonitor(), &dpiScaleX, &dpiScaleY);
    *scaleFactor = dpiScaleX > 0 ? dpiScaleX / 96 : *scaleFactor;
    return GetScaledImageName(fileName, scaleImageName,
        scaleFactor, scaledImageLength);
}

