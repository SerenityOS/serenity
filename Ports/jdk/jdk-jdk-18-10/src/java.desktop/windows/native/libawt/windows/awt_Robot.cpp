/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_awt_windows_WRobotPeer.h"
#include "java_awt_event_InputEvent.h"
#include "awt_Component.h"
#include <winuser.h>

static int signum(int i) {
  // special version of signum which returns 1 when value is 0
  return i >= 0 ? 1 : -1;
}

static void MouseMove(jint x, jint y)
{
    INPUT mouseInput = {0};
    mouseInput.type = INPUT_MOUSE;
    mouseInput.mi.time = 0;
    mouseInput.mi.dwFlags = MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_MOVE;
    mouseInput.mi.dx = (x * 65536 /::GetSystemMetrics(SM_CXSCREEN)) + signum(x);
    mouseInput.mi.dy = (y * 65536 /::GetSystemMetrics(SM_CYSCREEN)) + signum(y);
    ::SendInput(1, &mouseInput, sizeof(mouseInput));
}

static void MousePress(jint buttonMask)
{
    DWORD dwFlags = 0L;
    // According to MSDN: Software Driving Software
    // application should consider SM_SWAPBUTTON to correctly emulate user with
    // left handed mouse setup
    BOOL bSwap = ::GetSystemMetrics(SM_SWAPBUTTON);

    if ( buttonMask & java_awt_event_InputEvent_BUTTON1_MASK ||
        buttonMask & java_awt_event_InputEvent_BUTTON1_DOWN_MASK)
    {
        dwFlags |= !bSwap ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_RIGHTDOWN;
    }

    if ( buttonMask & java_awt_event_InputEvent_BUTTON3_MASK ||
         buttonMask & java_awt_event_InputEvent_BUTTON3_DOWN_MASK)
    {
        dwFlags |= !bSwap ? MOUSEEVENTF_RIGHTDOWN : MOUSEEVENTF_LEFTDOWN;
    }

    if ( buttonMask & java_awt_event_InputEvent_BUTTON2_MASK ||
         buttonMask & java_awt_event_InputEvent_BUTTON2_DOWN_MASK)
    {
        dwFlags |= MOUSEEVENTF_MIDDLEDOWN;
    }

    INPUT mouseInput = {0};
    mouseInput.type = INPUT_MOUSE;
    mouseInput.mi.time = 0;
    mouseInput.mi.dwFlags = dwFlags;
    if ( buttonMask & AwtComponent::masks[3] ) {
        mouseInput.mi.dwFlags = mouseInput.mi.dwFlags | MOUSEEVENTF_XDOWN;
        mouseInput.mi.mouseData = XBUTTON1;
    }

    if ( buttonMask & AwtComponent::masks[4] ) {
        mouseInput.mi.dwFlags = mouseInput.mi.dwFlags | MOUSEEVENTF_XDOWN;
        mouseInput.mi.mouseData = XBUTTON2;
    }
    ::SendInput(1, &mouseInput, sizeof(mouseInput));
}

static void MouseRelease(jint buttonMask)
{
    DWORD dwFlags = 0L;
    // According to MSDN: Software Driving Software
    // application should consider SM_SWAPBUTTON to correctly emulate user with
    // left handed mouse setup
    BOOL bSwap = ::GetSystemMetrics(SM_SWAPBUTTON);

    if ( buttonMask & java_awt_event_InputEvent_BUTTON1_MASK ||
        buttonMask & java_awt_event_InputEvent_BUTTON1_DOWN_MASK)
    {
        dwFlags |= !bSwap ? MOUSEEVENTF_LEFTUP : MOUSEEVENTF_RIGHTUP;
    }

    if ( buttonMask & java_awt_event_InputEvent_BUTTON3_MASK ||
         buttonMask & java_awt_event_InputEvent_BUTTON3_DOWN_MASK)
    {
        dwFlags |= !bSwap ? MOUSEEVENTF_RIGHTUP : MOUSEEVENTF_LEFTUP;
    }

    if ( buttonMask & java_awt_event_InputEvent_BUTTON2_MASK ||
        buttonMask & java_awt_event_InputEvent_BUTTON2_DOWN_MASK)
    {
        dwFlags |= MOUSEEVENTF_MIDDLEUP;
    }

    INPUT mouseInput = {0};
    mouseInput.type = INPUT_MOUSE;
    mouseInput.mi.time = 0;
    mouseInput.mi.dwFlags = dwFlags;

    if ( buttonMask & AwtComponent::masks[3] ) {
        mouseInput.mi.dwFlags = mouseInput.mi.dwFlags | MOUSEEVENTF_XUP;
        mouseInput.mi.mouseData = XBUTTON1;
    }

    if ( buttonMask & AwtComponent::masks[4] ) {
        mouseInput.mi.dwFlags = mouseInput.mi.dwFlags | MOUSEEVENTF_XUP;
        mouseInput.mi.mouseData = XBUTTON2;
    }
    ::SendInput(1, &mouseInput, sizeof(mouseInput));
}

static void MouseWheel(jint wheelAmt) {
    mouse_event(MOUSEEVENTF_WHEEL, 0, 0, wheelAmt * -1 * WHEEL_DELTA, 0);
}

inline jint WinToJavaPixel(USHORT r, USHORT g, USHORT b)
{
    jint value =
            0xFF << 24 | // alpha channel is always turned all the way up
            r << 16 |
            g << 8  |
            b << 0;
    return value;
}

static void GetRGBPixels(jint x, jint y, jint width, jint height, jintArray pixelArray)
{
    DASSERT(width > 0 && height > 0);

    HDC hdcScreen = ::CreateDC(TEXT("DISPLAY"), NULL, NULL, NULL);
    HDC hdcMem = ::CreateCompatibleDC(hdcScreen);
    HBITMAP hbitmap;
    HBITMAP hOldBitmap;
    HPALETTE hOldPalette = NULL;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // create an offscreen bitmap
    hbitmap = ::CreateCompatibleBitmap(hdcScreen, width, height);
    if (hbitmap == NULL) {
        throw std::bad_alloc();
    }
    hOldBitmap = (HBITMAP)::SelectObject(hdcMem, hbitmap);

    // REMIND: not multimon-friendly...
    int primaryIndex = AwtWin32GraphicsDevice::GetDefaultDeviceIndex();
    hOldPalette =
        AwtWin32GraphicsDevice::SelectPalette(hdcMem, primaryIndex);
    AwtWin32GraphicsDevice::RealizePalette(hdcMem, primaryIndex);

    // copy screen image to offscreen bitmap
    // CAPTUREBLT flag is required to capture WS_EX_LAYERED windows' contents
    // correctly on Win2K/XP
    VERIFY(::BitBlt(hdcMem, 0, 0, width, height, hdcScreen, x, y,
           SRCCOPY | CAPTUREBLT) != 0);

    static const int BITS_PER_PIXEL = 32;
    static const int BYTES_PER_PIXEL = BITS_PER_PIXEL/8;

    if (!IS_SAFE_SIZE_MUL(width, height)) throw std::bad_alloc();
    int numPixels = width*height;
    if (!IS_SAFE_SIZE_MUL(BYTES_PER_PIXEL, numPixels)) throw std::bad_alloc();
    int pixelDataSize = BYTES_PER_PIXEL*numPixels;
    DASSERT(pixelDataSize > 0 && pixelDataSize % 4 == 0);
    // allocate memory for BITMAPINFO + pixel data
    // 4620932: When using BI_BITFIELDS, GetDIBits expects an array of 3
    // RGBQUADS to follow the BITMAPINFOHEADER, but we were only allocating the
    // 1 that is included in BITMAPINFO.  Thus, GetDIBits was writing off the
    // end of our block of memory.  Now we allocate sufficient memory.
    // See MSDN docs for BITMAPINFOHEADER -bchristi

    if (!IS_SAFE_SIZE_ADD(sizeof(BITMAPINFOHEADER) + 3 * sizeof(RGBQUAD), pixelDataSize)) {
        throw std::bad_alloc();
    }
    BITMAPINFO * pinfo = (BITMAPINFO *)(new BYTE[sizeof(BITMAPINFOHEADER) + 3 * sizeof(RGBQUAD) + pixelDataSize]);

    // pixel data starts after 3 RGBQUADS for color masks
    RGBQUAD *pixelData = &pinfo->bmiColors[3];

    // prepare BITMAPINFO for a 32-bit RGB bitmap
    ::memset(pinfo, 0, sizeof(*pinfo));
    pinfo->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    pinfo->bmiHeader.biWidth = width;
    pinfo->bmiHeader.biHeight = -height; // negative height means a top-down DIB
    pinfo->bmiHeader.biPlanes = 1;
    pinfo->bmiHeader.biBitCount = BITS_PER_PIXEL;
    pinfo->bmiHeader.biCompression = BI_BITFIELDS;

    // Setup up color masks
    static const RGBQUAD redMask =   {0, 0, 0xFF, 0};
    static const RGBQUAD greenMask = {0, 0xFF, 0, 0};
    static const RGBQUAD blueMask =  {0xFF, 0, 0, 0};

    pinfo->bmiColors[0] = redMask;
    pinfo->bmiColors[1] = greenMask;
    pinfo->bmiColors[2] = blueMask;

    // Get the bitmap data in device-independent, 32-bit packed pixel format
    ::GetDIBits(hdcMem, hbitmap, 0, height, pixelData, pinfo, DIB_RGB_COLORS);

    // convert Win32 pixel format (BGRX) to Java format (ARGB)
    DASSERT(sizeof(jint) == sizeof(RGBQUAD));
    for(int nPixel = 0; nPixel < numPixels; nPixel++) {
        RGBQUAD * prgbq = &pixelData[nPixel];
        jint jpixel = WinToJavaPixel(prgbq->rgbRed, prgbq->rgbGreen, prgbq->rgbBlue);
        // stuff the 32-bit pixel back into the 32-bit RGBQUAD
        *prgbq = *( (RGBQUAD *)(&jpixel) );
    }

    // copy pixels into Java array
    env->SetIntArrayRegion(pixelArray, 0, numPixels, (jint *)pixelData);
    delete[] pinfo;

    // free all the GDI objects we made
    ::SelectObject(hdcMem, hOldBitmap);
    if (hOldPalette != NULL) {
        ::SelectPalette(hdcMem, hOldPalette, FALSE);
    }
    ::DeleteObject(hbitmap);
    ::DeleteDC(hdcMem);
    ::DeleteDC(hdcScreen);
}

static void DoKeyEvent(jint jkey, DWORD dwFlags)
{
    UINT        vkey;
    UINT        modifiers;
    UINT        scancode;
    JNIEnv *    env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // convert Java key into Windows key (and modifiers too)
    AwtComponent::JavaKeyToWindowsKey(jkey, &vkey, &modifiers);
    if (vkey == 0) {
        // no equivalent Windows key found for given Java keycode
        JNU_ThrowIllegalArgumentException(env, "Invalid key code");
    } else {
        // get the scancode from the virtual key
        scancode = ::MapVirtualKey(vkey, 0);
        if (vkey == VK_RMENU ||
            vkey == VK_DELETE ||
            vkey == VK_INSERT ||
            vkey == VK_NEXT ||
            vkey == VK_PRIOR ||
            vkey == VK_HOME ||
            vkey == VK_END ||
            vkey == VK_LEFT ||
            vkey == VK_RIGHT ||
            vkey == VK_UP ||
            vkey == VK_DOWN) {
            dwFlags |= KEYEVENTF_EXTENDEDKEY;
        }
        keybd_event(vkey, scancode, dwFlags, 0);
    }
}

////////////////////////////////////////////////////////////////////////////////
// Native method declarations
//

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_mouseMoveImpl(
    JNIEnv * env, jobject self, jint x, jint y)
{
    TRY;

    MouseMove(x, y);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_mousePress(
    JNIEnv * env, jobject self, jint buttons)
{
    TRY;

    MousePress(buttons);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_mouseRelease(
    JNIEnv * env, jobject self, jint buttons)
{
    TRY;

    MouseRelease(buttons);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_mouseWheel(
    JNIEnv * env, jobject self, jint wheelAmt)
{
    TRY;

    MouseWheel(wheelAmt);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_getRGBPixels(
    JNIEnv *env, jobject self, jint x, jint y, jint width, jint height, jintArray pixelArray)
{
    TRY;

    GetRGBPixels(x, y, width, height, pixelArray);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_keyPress(
  JNIEnv *, jobject self, jint javakey )
{
    TRY;

    DoKeyEvent(javakey, 0); // no flags means key down

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL Java_sun_awt_windows_WRobotPeer_keyRelease(
  JNIEnv *, jobject self, jint javakey )
{
    TRY;

    DoKeyEvent(javakey, KEYEVENTF_KEYUP);

    CATCH_BAD_ALLOC;
}
