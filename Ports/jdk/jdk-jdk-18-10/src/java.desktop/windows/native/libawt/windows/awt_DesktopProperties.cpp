/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "mmsystem.h"
#include "jlong.h"
#include "awt_DesktopProperties.h"
#include "awt_Toolkit.h"
#include "sun_awt_windows_WDesktopProperties.h"
#include "java_awt_Font.h"
#include "awtmsg.h"
#include "zmouse.h"
#include <shellapi.h>
#include <shlobj.h>

#include "math.h"

#if defined(_MSC_VER) && _MSC_VER >= 1800
#  define ROUND_TO_INT(num)    ((int) round(num))
#else
#  define ROUND_TO_INT(num)    ((int) floor((num) + 0.5))
#endif

// WDesktopProperties fields
jfieldID AwtDesktopProperties::pDataID = 0;
jmethodID AwtDesktopProperties::setBooleanPropertyID = 0;
jmethodID AwtDesktopProperties::setIntegerPropertyID = 0;
jmethodID AwtDesktopProperties::setStringPropertyID = 0;
jmethodID AwtDesktopProperties::setColorPropertyID = 0;
jmethodID AwtDesktopProperties::setFontPropertyID = 0;
jmethodID AwtDesktopProperties::setSoundPropertyID = 0;

AwtDesktopProperties::AwtDesktopProperties(jobject self) {
    this->self = GetEnv()->NewGlobalRef(self);
    GetEnv()->SetLongField( self, AwtDesktopProperties::pDataID,
                            ptr_to_jlong(this) );
}

AwtDesktopProperties::~AwtDesktopProperties() {
    GetEnv()->DeleteGlobalRef(self);
}

//
// Reads Windows parameters and sets the corresponding values
// in WDesktopProperties
//
void AwtDesktopProperties::GetWindowsParameters() {
    if (GetEnv()->EnsureLocalCapacity(MAX_PROPERTIES) < 0) {
        DASSERT(0);
        return;
    }
    // this number defines the set of properties available, it is incremented
    // whenever more properties are added (in a public release of course)
    // for example, version 1 defines the properties available in Java SDK version 1.3.
    SetIntegerProperty( TEXT("win.properties.version"), AWT_DESKTOP_PROPERTIES_VERSION);
    GetNonClientParameters();
    GetIconParameters();
    GetColorParameters();
    GetCaretParameters();
    GetOtherParameters();
    GetSoundEvents();
    GetSystemProperties();
    if (IS_WINXP) {
        GetXPStyleProperties();
    }
}

void getInvScale(float &invScaleX, float &invScaleY) {
    static int dpiX = -1;
    static int dpiY = -1;
    if (dpiX == -1 || dpiY == -1) {
        HWND hWnd = ::GetDesktopWindow();
        HDC hDC = ::GetDC(hWnd);
        dpiX = ::GetDeviceCaps(hDC, LOGPIXELSX);
        dpiY = ::GetDeviceCaps(hDC, LOGPIXELSY);
        ::ReleaseDC(hWnd, hDC);
    }

    invScaleX = (dpiX == 0.0f) ? 1.0f : 96.0f / dpiX;
    invScaleY = (dpiY == 0.0f) ? 1.0f : 96.0f / dpiY;
}

int rescale(int value, float invScale){
    return invScale == 1.0f ? value : ROUND_TO_INT(value * invScale);
}

void AwtDesktopProperties::GetSystemProperties() {
    HDC dc = CreateDC(TEXT("DISPLAY"), NULL, NULL, NULL);

    if (dc != NULL) {
        try {
            float invScaleX;
            float invScaleY;
            getInvScale(invScaleX, invScaleY);
            SetFontProperty(dc, ANSI_FIXED_FONT, TEXT("win.ansiFixed.font"), 1.0f);
            SetFontProperty(dc, ANSI_VAR_FONT, TEXT("win.ansiVar.font"), 1.0f);
            SetFontProperty(dc, DEVICE_DEFAULT_FONT, TEXT("win.deviceDefault.font"), 1.0f);
            SetFontProperty(dc, DEFAULT_GUI_FONT, TEXT("win.defaultGUI.font"), invScaleY);
            SetFontProperty(dc, OEM_FIXED_FONT, TEXT("win.oemFixed.font"), 1.0f);
            SetFontProperty(dc, SYSTEM_FONT, TEXT("win.system.font"), 1.0f);
            SetFontProperty(dc, SYSTEM_FIXED_FONT, TEXT("win.systemFixed.font"), 1.0f);
        }
        catch (std::bad_alloc&) {
            DeleteDC(dc);
            throw;
        }
        DeleteDC(dc);
    }
}


// Does the actual lookup for shell dialog font (MS Shell Dlg).  fontName
// contains the name to lookup (either MS Shell Dlg or MS Shell Dlg 2) and
// handle contains a reference toe the registry entry to look in.
// This will return NULL or a pointer to the resolved name.
// Note that it uses malloc() and returns the pointer to allocated
// memory, so remember to use free() when you are done with its
// result.
static LPTSTR resolveShellDialogFont(LPTSTR fontName, HKEY handle) {
    DWORD valueType, valueSize;
    if (RegQueryValueEx((HKEY)handle, fontName, NULL,
                        &valueType, NULL, &valueSize) != 0) {
        // Couldn't find it
        return NULL;
    }
    if (valueType != REG_SZ) {
        // Not the expected type
        return NULL;
    }
    LPTSTR buffer = (LPTSTR)safe_Malloc(valueSize);
    if (RegQueryValueEx((HKEY)handle, fontName, NULL,
                        &valueType, (unsigned char *)buffer, &valueSize) != 0) {
        // Error fetching
        free(buffer);
        return NULL;
    }
    return buffer;
}

// Determines what the font MS Shell Dlg maps to.
// Note that it uses malloc() and returns the pointer to allocated
// memory, so remember to use free() when you are done with its
// result.
static LPTSTR resolveShellDialogFont() {
    LPTSTR subKey = TEXT("Software\\Microsoft\\Windows NT\\CurrentVersion\\FontSubstitutes");

    HKEY handle;
    if (RegOpenKeyEx(HKEY_LOCAL_MACHINE, subKey, 0, KEY_READ, &handle) != 0) {
        return NULL;
    }
    // Prefer MS Shell Dlg 2.
    LPTSTR font = resolveShellDialogFont(TEXT("MS Shell Dlg 2"), handle);
    if (font == NULL) {
        font = resolveShellDialogFont(TEXT("MS Shell Dlg"), handle);
    }
    RegCloseKey(handle);
    return font;
}

// Local function for getting values from the Windows registry
// Note that it uses malloc() and returns the pointer to allocated
// memory, so remember to use free() when you are done with its
// result.
static LPTSTR getWindowsPropFromReg(LPTSTR subKey, LPTSTR valueName, DWORD *valueType) {
    HKEY handle;
    if (RegOpenKeyEx(HKEY_CURRENT_USER, subKey, 0, KEY_READ, &handle) != 0) {
        return NULL;
    }
    // valueSize is in bytes, while valueChar is in characters.
    DWORD valueSize, valueChar;
    if (RegQueryValueEx((HKEY)handle, valueName, NULL,
                        valueType, NULL, &valueSize) != 0) {
        RegCloseKey(handle);
        return NULL;
    }
    LPTSTR buffer = (LPTSTR)safe_Malloc(valueSize);
    if (RegQueryValueEx((HKEY)handle, valueName, NULL,
                        valueType, (unsigned char *)buffer, &valueSize) != 0) {
        free(buffer);
        RegCloseKey(handle);
        return NULL;
    }
    RegCloseKey(handle);

    if (*valueType == REG_EXPAND_SZ) {
        // Pending: buffer must be null-terminated at this point
        valueChar = ExpandEnvironmentStrings(buffer, NULL, 0);
        LPTSTR buffer2 = (LPTSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, valueChar, sizeof(TCHAR));
        ExpandEnvironmentStrings(buffer, buffer2, valueChar);
        free(buffer);
        return buffer2;
    } else if (*valueType == REG_SZ) {
        return buffer;
    } else if (*valueType == REG_DWORD) {
        return buffer;
    } else {
        free(buffer);
        return NULL;
    }
}

static LPTSTR getXPStylePropFromReg(LPTSTR valueName) {
    DWORD valueType;
    return getWindowsPropFromReg(TEXT("Software\\Microsoft\\Windows\\CurrentVersion\\ThemeManager"),
                                 valueName, &valueType);
}


// Used in AwtMenuItem to determine the color of top menus,
// since they depend on XP style. ThemeActive property is
// '1' for XP Style, '0' for Windows classic style.
BOOL AwtDesktopProperties::IsXPStyle() {
    LPTSTR style = getXPStylePropFromReg(TEXT("ThemeActive"));
    BOOL result = (style != NULL && *style == _T('1'));
    free(style);
    return result;
}

void AwtDesktopProperties::GetXPStyleProperties() {
    LPTSTR value;

    value = getXPStylePropFromReg(TEXT("ThemeActive"));
    try {
        SetBooleanProperty(TEXT("win.xpstyle.themeActive"), (value != NULL && *value == _T('1')));
        if (value != NULL) {
            free(value);
            value = NULL;
        }
        value = getXPStylePropFromReg(TEXT("DllName"));
        if (value != NULL) {
            SetStringProperty(TEXT("win.xpstyle.dllName"), value);
            free(value);
            value = NULL;
        }
        value = getXPStylePropFromReg(TEXT("SizeName"));
        if (value != NULL) {
            SetStringProperty(TEXT("win.xpstyle.sizeName"), value);
            free(value);
            value = NULL;
        }
        value = getXPStylePropFromReg(TEXT("ColorName"));
        if (value != NULL) {
            SetStringProperty(TEXT("win.xpstyle.colorName"), value);
            free(value);
        }
    }
    catch (std::bad_alloc&) {
        if (value != NULL) {
            free(value);
        }
        throw;
    }
}


void AwtDesktopProperties::GetNonClientParameters() {
    //
    // general window properties
    //
    NONCLIENTMETRICS    ncmetrics;

    // Fix for 6944516: specify correct size for ncmetrics on WIN2K/XP
    // Microsoft recommend to subtract the size of  'iPaddedBorderWidth' field
    // when running on XP. However this can't be referenced at compile time
    // with the older SDK, so there use 'lfMessageFont' plus its size.
    if (!IS_WINVISTA) {
#if defined(_MSC_VER) && (_MSC_VER >= 1600)
        ncmetrics.cbSize = offsetof(NONCLIENTMETRICS, iPaddedBorderWidth);
#else
        ncmetrics.cbSize = offsetof(NONCLIENTMETRICS,lfMessageFont) + sizeof(LOGFONT);
#endif
    } else {
        ncmetrics.cbSize = sizeof(ncmetrics);
    }
    VERIFY( SystemParametersInfo(SPI_GETNONCLIENTMETRICS, ncmetrics.cbSize, &ncmetrics, FALSE) );

    float invScaleX;
    float invScaleY;
    getInvScale(invScaleX, invScaleY);

    SetFontProperty(TEXT("win.frame.captionFont"), ncmetrics.lfCaptionFont, invScaleY);
    SetIntegerProperty(TEXT("win.frame.captionHeight"), rescale(ncmetrics.iCaptionHeight, invScaleY));
    SetIntegerProperty(TEXT("win.frame.captionButtonWidth"), rescale(ncmetrics.iCaptionWidth, invScaleX));
    SetIntegerProperty(TEXT("win.frame.captionButtonHeight"), rescale(ncmetrics.iCaptionHeight, invScaleY));
    SetFontProperty(TEXT("win.frame.smallCaptionFont"), ncmetrics.lfSmCaptionFont, invScaleY);
    SetIntegerProperty(TEXT("win.frame.smallCaptionHeight"), rescale(ncmetrics.iSmCaptionHeight, invScaleY));
    SetIntegerProperty(TEXT("win.frame.smallCaptionButtonWidth"), rescale(ncmetrics.iSmCaptionWidth, invScaleX));
    SetIntegerProperty(TEXT("win.frame.smallCaptionButtonHeight"), rescale(ncmetrics.iSmCaptionHeight, invScaleY));
    SetIntegerProperty(TEXT("win.frame.sizingBorderWidth"), rescale(ncmetrics.iBorderWidth, invScaleX));

    // menu properties
    SetFontProperty(TEXT("win.menu.font"), ncmetrics.lfMenuFont, invScaleY);
    SetIntegerProperty(TEXT("win.menu.height"), rescale(ncmetrics.iMenuHeight, invScaleY));
    SetIntegerProperty(TEXT("win.menu.buttonWidth"), rescale(ncmetrics.iMenuWidth, invScaleX));

    // scrollbar properties
    SetIntegerProperty(TEXT("win.scrollbar.width"), rescale(ncmetrics.iScrollWidth, invScaleX));
    SetIntegerProperty(TEXT("win.scrollbar.height"), rescale(ncmetrics.iScrollHeight, invScaleY));

    // status bar and tooltip properties
    SetFontProperty(TEXT("win.status.font"), ncmetrics.lfStatusFont, invScaleY);
    SetFontProperty(TEXT("win.tooltip.font"), ncmetrics.lfStatusFont, invScaleY);

    // message box properties
    SetFontProperty(TEXT("win.messagebox.font"), ncmetrics.lfMessageFont, invScaleY);
}

void AwtDesktopProperties::GetIconParameters() {
    //
    // icon properties
    //
    ICONMETRICS iconmetrics;

    iconmetrics.cbSize = sizeof(iconmetrics);
    VERIFY( SystemParametersInfo(SPI_GETICONMETRICS, iconmetrics.cbSize, &iconmetrics, FALSE) );

    float invScaleX;
    float invScaleY;
    getInvScale(invScaleX, invScaleY);
    SetIntegerProperty(TEXT("win.icon.hspacing"), rescale(iconmetrics.iHorzSpacing, invScaleX));
    SetIntegerProperty(TEXT("win.icon.vspacing"), rescale(iconmetrics.iVertSpacing, invScaleY));
    SetBooleanProperty(TEXT("win.icon.titleWrappingOn"), iconmetrics.iTitleWrap != 0);
    SetFontProperty(TEXT("win.icon.font"), iconmetrics.lfFont, invScaleY);
}
/*
 Windows settings for these are also in the registry
 They exist as system wide HKLM: HKEY_LOCAL_MACHINE and
 HKCU: HKEY_CURRENT_USER.
 HKCU\Control Panel\Desktop\FontSmoothing :  "0=OFF",  "2=ON"
 HKCU\Control Panel\Desktop\FontSmoothingType: 1=Standard, 2=LCD
 HKCU\Control Panel\Desktop\FontSmoothingGamma: 1000->2200
 HKCU\Control Panel\Desktop\FontSmoothingOrientation: 0=BGR, 1=RGB

 SystemParametersInfo supplies the first three of these but does not
 however expose the Orientation. That has to come from the registry.

 We go to some small lengths in here to not make queries we don't need.
 Eg if we previously were using standard font smoothing and we still are
 then its unlikely that any change in gamma will have occurred except
 by a program which changed it, and even if it did, we don't need to pick
 it up until someone turns on the LCD option.
 To do: this loop is called once per top-level window so an app with
 N windows will get notified N times. It would save us a small amount of
 redundant work if I could identify the message as being one already processed
 for another window.
 Also presumably a repaint that specifies only a partially damaged window
 isn't one that needs this checking.
*/

#define FONTSMOOTHING_OFF 0
#define FONTSMOOTHING_ON  1
#define FONTSMOOTHING_STANDARD 1
#define FONTSMOOTHING_LCD 2
#define LCD_RGB_ORDER 1
#define LCD_BGR_ORDER 0


int GetLCDSubPixelOrder() {
    LONG order=99;
    LONG bufferSize = 4;
    HKEY hkeyDesktop;
    static LPCTSTR DESKTOPKEY = TEXT("Control Panel\\Desktop");
    LONG ret = RegOpenKeyEx(HKEY_CURRENT_USER,
                            DESKTOPKEY, 0L, KEY_READ, &hkeyDesktop);
    if (ret != ERROR_SUCCESS) {
        return LCD_RGB_ORDER;
    }
    ret = RegQueryValueEx(hkeyDesktop, TEXT("FontSmoothingOrientation"),
                          NULL, NULL, (LPBYTE)&order, (LPDWORD)&bufferSize);
    RegCloseKey(hkeyDesktop);
    if (ret != ERROR_SUCCESS) {
        return LCD_RGB_ORDER;
    } else {
        return (int)order;
    }
}

void CheckFontSmoothingSettings(HWND hWnd) {
    static BOOL firstTime = TRUE;
    static BOOL lastFontSmoothing = FALSE;
    static UINT lastFontSmoothingType = FONTSMOOTHING_ON;
    static UINT lastFontSmoothingContrast = 1400;
    static UINT lastSubpixelOrder = LCD_RGB_ORDER;

    /* If we are called with a window handle it is because there is a
     * message to repaint at least some part of the window which typically
     * is not because of the desktop font settings change. Much more likely
     * its a normal repaint event. If it is because of the rare settings
     * change in that case the update region will be the entire window.
     * Try to as cheaply as possible determine if this is not a call
     * to repaint the whole window by assuming that all such calls will
     * have an update region whose origin is 0,0. Only in that case will
     * we take the hit of checking the settings.
     * Thus we avoid taking the hit of the other calls for most partial
     * expose events, which will never be the result of changes to desktop
     * font settings.
     */
    if (hWnd != NULL) {
        RECT r;
        if (!::GetUpdateRect(hWnd, &r, FALSE) || r.top != 0 || r.left != 0) {
            return;
        }
    }

    BOOL fontSmoothing = FALSE, settingsChanged;
    UINT fontSmoothingType=0, fontSmoothingContrast=0, subPixelOrder=0;

    if (firstTime) {
        SystemParametersInfo(SPI_GETFONTSMOOTHING, 0, &fontSmoothing, 0);
        if (IS_WINXP) {
            SystemParametersInfo(SPI_GETFONTSMOOTHINGTYPE, 0,
                                 &fontSmoothingType, 0);
            SystemParametersInfo(SPI_GETFONTSMOOTHINGCONTRAST, 0,
                                 &fontSmoothingContrast, 0);
        }
        lastFontSmoothing = fontSmoothing;
        lastFontSmoothingType = fontSmoothingType;
        lastFontSmoothingContrast = fontSmoothingContrast;
        firstTime = FALSE;
        return;
    } else {
        SystemParametersInfo(SPI_GETFONTSMOOTHING, 0, &fontSmoothing, 0);
        settingsChanged = fontSmoothing != lastFontSmoothing;
        if (!settingsChanged && fontSmoothing == FONTSMOOTHING_OFF) {
            /* no need to check the other settings in this case. */
            return;
        }
        if (IS_WINXP) {
            SystemParametersInfo(SPI_GETFONTSMOOTHINGTYPE, 0,
                                 &fontSmoothingType, 0);
            settingsChanged |= fontSmoothingType != lastFontSmoothingType;
            if (!settingsChanged &&
                fontSmoothingType == FONTSMOOTHING_STANDARD) {
                /* No need to check any LCD specific settings */
                return;
            } else {
                SystemParametersInfo(SPI_GETFONTSMOOTHINGCONTRAST, 0,
                                     &fontSmoothingContrast, 0);
                settingsChanged |=
                    fontSmoothingContrast != lastFontSmoothingContrast;
                if (fontSmoothingType == FONTSMOOTHING_LCD) {
                    // Order is a registry entry so more expensive to check.x
                    subPixelOrder = GetLCDSubPixelOrder();
                    settingsChanged |= subPixelOrder != lastSubpixelOrder;
                }
            }
        } else {
            if (settingsChanged && fontSmoothing == FONTSMOOTHING_ON) {
                fontSmoothingType = FONTSMOOTHING_STANDARD;
            }
        }
    }
    if (settingsChanged) {
        /* Some of these values may not have been queried, but it shouldn't
         * matter as what's important is to track changes in values we are
         * actually using. The up-call we make here will cause the actual
         * values for everything to get queried and set into the desktop
         * properties.
         */
        lastFontSmoothing = fontSmoothing;
        lastFontSmoothingType = fontSmoothingType;
        lastFontSmoothingContrast = fontSmoothingContrast;
        lastSubpixelOrder = subPixelOrder;

        jobject peer = AwtToolkit::GetInstance().GetPeer();
        if (peer != NULL) {
            AwtToolkit::GetEnv()->CallVoidMethod(peer,
                                     AwtToolkit::windowsSettingChangeMID);
        }
    }
}

void AwtDesktopProperties::GetColorParameters() {

    SetColorProperty(TEXT("win.frame.activeCaptionGradientColor"),
                     GetSysColor(COLOR_GRADIENTACTIVECAPTION));
    SetColorProperty(TEXT("win.frame.inactiveCaptionGradientColor"),
                     GetSysColor(COLOR_GRADIENTINACTIVECAPTION));
    SetColorProperty(TEXT("win.item.hotTrackedColor"),
                     GetSysColor(COLOR_HOTLIGHT));
    SetColorProperty(TEXT("win.3d.darkShadowColor"), GetSysColor(COLOR_3DDKSHADOW));
    SetColorProperty(TEXT("win.3d.backgroundColor"), GetSysColor(COLOR_3DFACE));
    SetColorProperty(TEXT("win.3d.highlightColor"), GetSysColor(COLOR_3DHIGHLIGHT));
    SetColorProperty(TEXT("win.3d.lightColor"), GetSysColor(COLOR_3DLIGHT));
    SetColorProperty(TEXT("win.3d.shadowColor"), GetSysColor(COLOR_3DSHADOW));
    SetColorProperty(TEXT("win.button.textColor"), GetSysColor(COLOR_BTNTEXT));
    SetColorProperty(TEXT("win.desktop.backgroundColor"), GetSysColor(COLOR_DESKTOP));
    SetColorProperty(TEXT("win.frame.activeCaptionColor"), GetSysColor(COLOR_ACTIVECAPTION));
    SetColorProperty(TEXT("win.frame.activeBorderColor"), GetSysColor(COLOR_ACTIVEBORDER));

    // ?? ?? ??
    SetColorProperty(TEXT("win.frame.color"), GetSysColor(COLOR_WINDOWFRAME)); // ?? WHAT THE HECK DOES THIS MEAN ??
    // ?? ?? ??

    SetColorProperty(TEXT("win.frame.backgroundColor"), GetSysColor(COLOR_WINDOW));
    SetColorProperty(TEXT("win.frame.captionTextColor"), GetSysColor(COLOR_CAPTIONTEXT));
    SetColorProperty(TEXT("win.frame.inactiveBorderColor"), GetSysColor(COLOR_INACTIVEBORDER));
    SetColorProperty(TEXT("win.frame.inactiveCaptionColor"), GetSysColor(COLOR_INACTIVECAPTION));
    SetColorProperty(TEXT("win.frame.inactiveCaptionTextColor"), GetSysColor(COLOR_INACTIVECAPTIONTEXT));
    SetColorProperty(TEXT("win.frame.textColor"), GetSysColor(COLOR_WINDOWTEXT));
    SetColorProperty(TEXT("win.item.highlightColor"), GetSysColor(COLOR_HIGHLIGHT));
    SetColorProperty(TEXT("win.item.highlightTextColor"), GetSysColor(COLOR_HIGHLIGHTTEXT));
    SetColorProperty(TEXT("win.mdi.backgroundColor"), GetSysColor(COLOR_APPWORKSPACE));
    SetColorProperty(TEXT("win.menu.backgroundColor"), GetSysColor(COLOR_MENU));
    SetColorProperty(TEXT("win.menu.textColor"), GetSysColor(COLOR_MENUTEXT));
    // COLOR_MENUBAR is only defined on WindowsXP. Our binaries are
    // built on NT, hence the below ifdef.
#ifndef COLOR_MENUBAR
#define COLOR_MENUBAR 30
#endif
    SetColorProperty(TEXT("win.menubar.backgroundColor"),
                                GetSysColor(IS_WINXP ? COLOR_MENUBAR : COLOR_MENU));
    SetColorProperty(TEXT("win.scrollbar.backgroundColor"), GetSysColor(COLOR_SCROLLBAR));
    SetColorProperty(TEXT("win.text.grayedTextColor"), GetSysColor(COLOR_GRAYTEXT));
    SetColorProperty(TEXT("win.tooltip.backgroundColor"), GetSysColor(COLOR_INFOBK));
    SetColorProperty(TEXT("win.tooltip.textColor"), GetSysColor(COLOR_INFOTEXT));
}

void AwtDesktopProperties::GetOtherParameters() {
    // TODO BEGIN: On NT4, some setttings don't trigger WM_SETTINGCHANGE --
    // check whether this has been fixed on Windows 2000 and Windows 98
    // ECH 10/6/2000 seems to be fixed on NT4 SP5, but not on 98
    SetBooleanProperty(TEXT("win.frame.fullWindowDragsOn"), GetBooleanParameter(SPI_GETDRAGFULLWINDOWS));
    SetBooleanProperty(TEXT("win.text.fontSmoothingOn"), GetBooleanParameter(SPI_GETFONTSMOOTHING));
    // TODO END

    if (IS_WINXP) {
        SetIntegerProperty(TEXT("win.text.fontSmoothingType"),
                           GetIntegerParameter(SPI_GETFONTSMOOTHINGTYPE));
        SetIntegerProperty(TEXT("win.text.fontSmoothingContrast"),
                           GetIntegerParameter(SPI_GETFONTSMOOTHINGCONTRAST));
        SetIntegerProperty(TEXT("win.text.fontSmoothingOrientation"),
                           GetLCDSubPixelOrder());
    }

    int cxdrag = GetSystemMetrics(SM_CXDRAG);
    int cydrag = GetSystemMetrics(SM_CYDRAG);
    SetIntegerProperty(TEXT("win.drag.width"), cxdrag);
    SetIntegerProperty(TEXT("win.drag.height"), cydrag);
    SetIntegerProperty(TEXT("DnD.gestureMotionThreshold"), max(cxdrag, cydrag)/2);
    SetIntegerProperty(TEXT("awt.mouse.numButtons"), AwtToolkit::GetNumberOfButtons());

    SetIntegerProperty(TEXT("awt.multiClickInterval"), GetDoubleClickTime());

    // BEGIN cross-platform properties
    // Note that these are cross-platform properties, but are being stuck into
    // WDesktopProperties.  WToolkit.lazilyLoadDesktopProperty() can find them,
    // but if a Toolkit subclass uses the desktopProperties
    // member, these properties won't be there. -bchristi, echawkes
    // This property is called "win.frame.fullWindowDragsOn" above
    // This is one of the properties that don't trigger WM_SETTINGCHANGE
    SetBooleanProperty(TEXT("awt.dynamicLayoutSupported"), GetBooleanParameter(SPI_GETDRAGFULLWINDOWS));
    SetBooleanProperty(TEXT("awt.wheelMousePresent"),
                       ::GetSystemMetrics(SM_MOUSEWHEELPRESENT));

    // END cross-platform properties

    //DWORD   menuShowDelay;
    //SystemParametersInfo(SPI_GETMENUSHOWDELAY, 0, &menuShowDelay, 0);
    // SetIntegerProperty(TEXT("win.menu.showDelay"), menuShowDelay);
    SetBooleanProperty(TEXT("win.frame.captionGradientsOn"), GetBooleanParameter(SPI_GETGRADIENTCAPTIONS));
    SetBooleanProperty(TEXT("win.item.hotTrackingOn"), GetBooleanParameter(SPI_GETHOTTRACKING));

    SetBooleanProperty(TEXT("win.menu.keyboardCuesOn"), GetBooleanParameter(SPI_GETKEYBOARDCUES));

    // High contrast accessibility property
    HIGHCONTRAST contrast;
    contrast.cbSize = sizeof(HIGHCONTRAST);
    if (SystemParametersInfo(SPI_GETHIGHCONTRAST, sizeof(HIGHCONTRAST),
                             &contrast, 0) != 0 &&
              (contrast.dwFlags & HCF_HIGHCONTRASTON) == HCF_HIGHCONTRASTON) {
      SetBooleanProperty(TEXT("win.highContrast.on"), TRUE);
    }
    else {
      SetBooleanProperty(TEXT("win.highContrast.on"), FALSE);
    }

    SHELLFLAGSTATE sfs;
    ::SHGetSettings(&sfs, SSF_SHOWALLOBJECTS | SSF_SHOWATTRIBCOL);
    if (sfs.fShowAllObjects) {
        SetBooleanProperty(TEXT("awt.file.showHiddenFiles"), TRUE);
    }
    else {
        SetBooleanProperty(TEXT("awt.file.showHiddenFiles"), FALSE);
    }
    if (sfs.fShowAttribCol) {
        SetBooleanProperty(TEXT("awt.file.showAttribCol"), TRUE);
    }
    else {
        SetBooleanProperty(TEXT("awt.file.showAttribCol"), FALSE);
    }

    LPTSTR value;
    DWORD valueType;

    // Shell Icon BPP - only honored on platforms before XP
    value = getWindowsPropFromReg(TEXT("Control Panel\\Desktop\\WindowMetrics"),
                                  TEXT("Shell Icon BPP"), &valueType);

    try {
        if (value != NULL) {
            if (valueType == REG_SZ) {
                SetStringProperty(TEXT("win.icon.shellIconBPP"), value);
            }
            free(value);
            value = NULL;
        }


        // The following registry settings control the file chooser places bar
        // under the Windows L&F. These settings are not present by default, but
        // can be enabled using the TweakUI tool from Microsoft. For more info,
        // see http://msdn.microsoft.com/msdnmag/issues/1100/Registry/

        // NoPlacesBar is a REG_DWORD, with values 0 or 1
        value = getWindowsPropFromReg(TEXT("Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\comdlg32"),
                                      TEXT("NoPlacesBar"), &valueType);
        if (value != NULL) {
            if (valueType == REG_DWORD) {
                SetBooleanProperty(TEXT("win.comdlg.noPlacesBar"), (BOOL)((int)*value != 0));
            }
            free(value);
        }
    }
    catch (std::bad_alloc&) {
        if (value != NULL) {
            free(value);
        }
        throw;
    }

    LPTSTR valueName = TEXT("PlaceN");
    LPTSTR valueNameBuf = (LPTSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, (lstrlen(valueName) + 1), sizeof(TCHAR));
    lstrcpy(valueNameBuf, valueName);

    LPTSTR propKey = TEXT("win.comdlg.placesBarPlaceN");

    LPTSTR propKeyBuf;
    try {
        propKeyBuf = (LPTSTR)SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, (lstrlen(propKey) + 1), sizeof(TCHAR));
    }
    catch (std::bad_alloc&) {
        free(valueNameBuf);
        throw;
    }
    lstrcpy(propKeyBuf, propKey);

    int i = 0;
    do {
        valueNameBuf[5] = _T('0' + i++);
        propKeyBuf[25] = valueNameBuf[5];

        LPTSTR key = TEXT("Software\\Microsoft\\Windows\\CurrentVersion\\Policies\\comdlg32\\PlacesBar");
        try {
            value = NULL;
            if ((value = getWindowsPropFromReg(key, valueNameBuf, &valueType)) != NULL) {
                if (valueType == REG_DWORD) {
                    // Value is a CSIDL
                    SetIntegerProperty(propKeyBuf, (int)*value);
                } else {
                    // Value is a path
                    SetStringProperty(propKeyBuf, value);
                }
                free(value);
            }
        }
        catch (std::bad_alloc&) {
            if (value != NULL) {
                free(value);
            }
            free(propKeyBuf);
            free(valueNameBuf);
            throw;
        }
    } while (value != NULL);

    free(propKeyBuf);
    free(valueNameBuf);
}

void AwtDesktopProperties::GetSoundEvents() {
    /////
    SetSoundProperty(TEXT("win.sound.default"), TEXT(".Default"));
    SetSoundProperty(TEXT("win.sound.close"), TEXT("Close"));
    SetSoundProperty(TEXT("win.sound.maximize"), TEXT("Maximize"));
    SetSoundProperty(TEXT("win.sound.minimize"), TEXT("Minimize"));
    SetSoundProperty(TEXT("win.sound.menuCommand"), TEXT("MenuCommand"));
    SetSoundProperty(TEXT("win.sound.menuPopup"), TEXT("MenuPopup"));
    SetSoundProperty(TEXT("win.sound.open"), TEXT("Open"));
    SetSoundProperty(TEXT("win.sound.restoreDown"), TEXT("RestoreDown"));
    SetSoundProperty(TEXT("win.sound.restoreUp"), TEXT("RestoreUp"));
    /////
    SetSoundProperty(TEXT("win.sound.asterisk"), TEXT("SystemAsterisk"));
    SetSoundProperty(TEXT("win.sound.exclamation"), TEXT("SystemExclamation"));
    SetSoundProperty(TEXT("win.sound.exit"), TEXT("SystemExit"));
    SetSoundProperty(TEXT("win.sound.hand"), TEXT("SystemHand"));
    SetSoundProperty(TEXT("win.sound.question"), TEXT("SystemQuestion"));
    SetSoundProperty(TEXT("win.sound.start"), TEXT("SystemStart"));
}

void AwtDesktopProperties::GetCaretParameters() {
    SetIntegerProperty(TEXT("win.caret.width"), GetIntegerParameter(SPI_GETCARETWIDTH));
}

BOOL AwtDesktopProperties::GetBooleanParameter(UINT spi) {
    BOOL        flag;
    SystemParametersInfo(spi, 0, &flag, 0);
    DASSERT(flag == TRUE || flag == FALSE); // should be simple boolean value
    return flag;
}

UINT AwtDesktopProperties::GetIntegerParameter(UINT spi) {
    UINT retValue;
    SystemParametersInfo(spi, 0, &retValue, 0);
    return retValue;
}

void AwtDesktopProperties::SetStringProperty(LPCTSTR propName, LPTSTR value) {
    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        throw std::bad_alloc();
    }
    jstring jValue = JNU_NewStringPlatform(GetEnv(), value);
    if (jValue == NULL) {
        GetEnv()->DeleteLocalRef(key);
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self,
                             AwtDesktopProperties::setStringPropertyID,
                             key, jValue);
    GetEnv()->DeleteLocalRef(jValue);
    GetEnv()->DeleteLocalRef(key);
}

void AwtDesktopProperties::SetIntegerProperty(LPCTSTR propName, int value) {

    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self,
                             AwtDesktopProperties::setIntegerPropertyID,
                             key, (jint)value);
    GetEnv()->DeleteLocalRef(key);
}

void AwtDesktopProperties::SetBooleanProperty(LPCTSTR propName, BOOL value) {
    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self,
                             AwtDesktopProperties::setBooleanPropertyID,
                             key, value ? JNI_TRUE : JNI_FALSE);
    GetEnv()->DeleteLocalRef(key);
}

void AwtDesktopProperties::SetColorProperty(LPCTSTR propName, DWORD value) {
    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self,
                             AwtDesktopProperties::setColorPropertyID,
                             key, GetRValue(value), GetGValue(value),
                             GetBValue(value));
    GetEnv()->DeleteLocalRef(key);
}

void AwtDesktopProperties::SetFontProperty(HDC dc, int fontID,
    LPCTSTR propName, float invScale) {
        HGDIOBJ font = GetStockObject(fontID);
    if (font != NULL && SelectObject(dc, font) != NULL) {
        int length = GetTextFace(dc, 0, NULL);

        if (length > 0) {
            LPTSTR face = new TCHAR[length];

            if (GetTextFace(dc, length, face) > 0) {
                TEXTMETRIC metrics;

                if (GetTextMetrics(dc, &metrics) > 0) {
                    jstring fontName = NULL;
                    if (!wcscmp(face, L"MS Shell Dlg")) {
                        // MS Shell Dlg is an indirect font name, find the
                        // real face name from the registry.
                        LPTSTR shellDialogFace = resolveShellDialogFont();
                        if (shellDialogFace != NULL) {
                            fontName = JNU_NewStringPlatform(GetEnv(),
                                                             shellDialogFace);
                            free(shellDialogFace);
                        }
                        else {
                            // Couldn't determine mapping for MS Shell Dlg,
                            // fall back to Microsoft Sans Serif
                            fontName = JNU_NewStringPlatform(GetEnv(),
                                                    L"Microsoft Sans Serif");
                        }
                    }
                    else {
                        fontName = JNU_NewStringPlatform(GetEnv(), face);
                    }
                    if (fontName == NULL) {
                        delete[] face;
                        throw std::bad_alloc();
                    }

                    jint pointSize = rescale(metrics.tmHeight -
                                     metrics.tmInternalLeading, invScale);
                    jint style = java_awt_Font_PLAIN;

                    if (metrics.tmWeight >= FW_BOLD) {
                        style =  java_awt_Font_BOLD;
                    }
                    if (metrics.tmItalic ) {
                        style |= java_awt_Font_ITALIC;
                    }

                    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
                    if (key == NULL) {
                        GetEnv()->DeleteLocalRef(fontName);
                        delete[] face;
                        throw std::bad_alloc();
                    }
                    GetEnv()->CallVoidMethod(self,
                              AwtDesktopProperties::setFontPropertyID,
                              key, fontName, style, pointSize);
                    GetEnv()->DeleteLocalRef(key);
                    GetEnv()->DeleteLocalRef(fontName);
                }
            }
            delete[] face;
        }
    }
}

void AwtDesktopProperties::SetFontProperty(LPCTSTR propName, const LOGFONT & font,
    float invScale) {
    jstring fontName;
    jint pointSize;
    jint style;

    fontName = JNU_NewStringPlatform(GetEnv(), font.lfFaceName);
    if (fontName == NULL) {
        throw std::bad_alloc();
    }
#if 0
    HDC         hdc;
    int         pixelsPerInch = GetDeviceCaps(hdc, LOGPIXELSY);
    // convert font size specified in pixels to font size in points
    hdc = GetDC(NULL);
    pointSize = (-font.lfHeight)*72/pixelsPerInch;
    ReleaseDC(NULL, hdc);
#endif
    // Java uses point sizes, but assumes 1 pixel = 1 point
    pointSize = rescale(-font.lfHeight, invScale);

    // convert Windows font style to Java style
    style = java_awt_Font_PLAIN;
    DTRACE_PRINTLN1("weight=%d", font.lfWeight);
    if ( font.lfWeight >= FW_BOLD ) {
        style =  java_awt_Font_BOLD;
    }
    if ( font.lfItalic ) {
        style |= java_awt_Font_ITALIC;
    }

    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        GetEnv()->DeleteLocalRef(fontName);
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self, AwtDesktopProperties::setFontPropertyID,
                             key, fontName, style, pointSize);
    GetEnv()->DeleteLocalRef(key);
    GetEnv()->DeleteLocalRef(fontName);
}

void AwtDesktopProperties::SetSoundProperty(LPCTSTR propName, LPCTSTR winEventName) {
    jstring key = JNU_NewStringPlatform(GetEnv(), propName);
    if (key == NULL) {
        throw std::bad_alloc();
    }
    jstring event = JNU_NewStringPlatform(GetEnv(), winEventName);
    if (event == NULL) {
        GetEnv()->DeleteLocalRef(key);
        throw std::bad_alloc();
    }
    GetEnv()->CallVoidMethod(self,
                             AwtDesktopProperties::setSoundPropertyID,
                             key, event);
    GetEnv()->DeleteLocalRef(event);
    GetEnv()->DeleteLocalRef(key);
}

void AwtDesktopProperties::PlayWindowsSound(LPCTSTR event) {
    // stop any currently playing sounds
    ::PlaySound(NULL, NULL, SND_PURGE);
    // play the sound for the given event name
    ::PlaySound(event, NULL, SND_ASYNC|SND_ALIAS|SND_NODEFAULT);
}

///////////////////////////////////////////////////////////////////////////////////////////////////

static AwtDesktopProperties * GetCppThis(JNIEnv *env, jobject self) {
    jlong longProps = env->GetLongField(self, AwtDesktopProperties::pDataID);
    AwtDesktopProperties * props =
        (AwtDesktopProperties *)jlong_to_ptr(longProps);
    DASSERT( !IsBadReadPtr(props, sizeof(*props)) );
    return props;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDesktopProperties_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtDesktopProperties::pDataID = env->GetFieldID(cls, "pData", "J");
    DASSERT(AwtDesktopProperties::pDataID != 0);
    CHECK_NULL(AwtDesktopProperties::pDataID);

    AwtDesktopProperties::setBooleanPropertyID =
        env->GetMethodID(cls, "setBooleanProperty", "(Ljava/lang/String;Z)V");
    DASSERT(AwtDesktopProperties::setBooleanPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setBooleanPropertyID);

    AwtDesktopProperties::setIntegerPropertyID =
        env->GetMethodID(cls, "setIntegerProperty", "(Ljava/lang/String;I)V");
    DASSERT(AwtDesktopProperties::setIntegerPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setIntegerPropertyID);

    AwtDesktopProperties::setStringPropertyID =
        env->GetMethodID(cls, "setStringProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
    DASSERT(AwtDesktopProperties::setStringPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setStringPropertyID);

    AwtDesktopProperties::setColorPropertyID =
        env->GetMethodID(cls, "setColorProperty", "(Ljava/lang/String;III)V");
    DASSERT(AwtDesktopProperties::setColorPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setColorPropertyID);

    AwtDesktopProperties::setFontPropertyID =
        env->GetMethodID(cls, "setFontProperty", "(Ljava/lang/String;Ljava/lang/String;II)V");
    DASSERT(AwtDesktopProperties::setFontPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setFontPropertyID);

    AwtDesktopProperties::setSoundPropertyID =
        env->GetMethodID(cls, "setSoundProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
    DASSERT(AwtDesktopProperties::setSoundPropertyID != 0);
    CHECK_NULL(AwtDesktopProperties::setSoundPropertyID);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDesktopProperties_init(JNIEnv *env, jobject self) {
    TRY;

    new AwtDesktopProperties(self);

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDesktopProperties_getWindowsParameters(JNIEnv *env, jobject self) {
    TRY;

    GetCppThis(env, self)->GetWindowsParameters();

    CATCH_BAD_ALLOC;
}

JNIEXPORT void JNICALL
Java_sun_awt_windows_WDesktopProperties_playWindowsSound(JNIEnv *env, jobject self, jstring event) {
    TRY;

    LPCTSTR winEventName;
    winEventName = JNU_GetStringPlatformChars(env, event, NULL);
    if ( winEventName == NULL ) {
        return;
    }
    GetCppThis(env, self)->PlayWindowsSound(winEventName);
    JNU_ReleaseStringPlatformChars(env, event, winEventName);

    CATCH_BAD_ALLOC;
}
