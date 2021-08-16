/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "awt_MenuItem.h"
#include "awt_Menu.h"
#include "awt_MenuBar.h"
#include "awt_DesktopProperties.h"
#include <sun_awt_windows_WCheckboxMenuItemPeer.h>

// Begin -- Win32 SDK include files
#include <tchar.h>
#include <imm.h>
#include <ime.h>
// End -- Win32 SDK include files

//add for multifont menuitem
#include <java_awt_CheckboxMenuItem.h>
#include <java_awt_Toolkit.h>
#include <java_awt_event_InputEvent.h>

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _SetLabel() method
struct SetLabelStruct {
    jobject menuitem;
    jstring label;
};
// struct for _SetEnable() method
struct SetEnableStruct {
    jobject menuitem;
    jboolean isEnabled;
};
// struct for _setState() method
struct SetStateStruct {
    jobject menuitem;
    jboolean isChecked;
};
/************************************************************************
 * AwtMenuItem fields
 */

HBITMAP AwtMenuItem::bmpCheck;
jobject AwtMenuItem::systemFont;

jfieldID AwtMenuItem::labelID;
jfieldID AwtMenuItem::enabledID;
jfieldID AwtMenuItem::shortcutLabelID;
jfieldID AwtMenuItem::isCheckboxID;
jfieldID AwtMenuItem::stateID;

jmethodID AwtMenuItem::getDefaultFontMID;

// Added by waleed to initialize the RTL Flags
LANGID AwtMenuItem::m_idLang = LOWORD(GetKeyboardLayout(0));
UINT AwtMenuItem::m_CodePage =
    AwtMenuItem::LangToCodePage(AwtMenuItem::m_idLang);
BOOL AwtMenuItem::sm_rtl = PRIMARYLANGID(GetInputLanguage()) == LANG_ARABIC ||
                           PRIMARYLANGID(GetInputLanguage()) == LANG_HEBREW;
BOOL AwtMenuItem::sm_rtlReadingOrder =
    PRIMARYLANGID(GetInputLanguage()) == LANG_ARABIC;

/*
 * This constant holds width of the default menu
 * check-mark bitmap for default settings on XP/Vista,
 * in pixels
 */
static const int SM_CXMENUCHECK_DEFAULT_ON_XP = 13;
static const int SM_CXMENUCHECK_DEFAULT_ON_VISTA = 15;

/************************************************************************
 * AwtMenuItem methods
 */

AwtMenuItem::AwtMenuItem() {
    m_peerObject = NULL;
    m_menuContainer = NULL;
    m_Id = (UINT)-1;
    m_freeId = FALSE;
    m_isCheckbox = FALSE;
}

AwtMenuItem::~AwtMenuItem()
{
}

void AwtMenuItem::RemoveCmdID()
{
    if (m_freeId) {
        AwtToolkit::GetInstance().RemoveCmdID( GetID() );
        m_freeId = FALSE;
    }
}
void AwtMenuItem::Dispose()
{
    RemoveCmdID();

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (m_peerObject != NULL) {
        JNI_SET_DESTROYED(m_peerObject);
        JNI_SET_PDATA(m_peerObject, NULL);
        env->DeleteGlobalRef(m_peerObject);
        m_peerObject = NULL;
    }

    AwtObject::Dispose();
}

LPCTSTR AwtMenuItem::GetClassName() {
  return TEXT("SunAwtMenuItem");
}
// Convert Language ID to CodePage
UINT AwtMenuItem::LangToCodePage(LANGID idLang)
{
    TCHAR strCodePage[MAX_ACP_STR_LEN];
    // use the LANGID to create a LCID
    LCID idLocale = MAKELCID(idLang, SORT_DEFAULT);
    // get the ANSI code page associated with this locale
    if (GetLocaleInfo(idLocale, LOCALE_IDEFAULTANSICODEPAGE, strCodePage, sizeof(strCodePage)/sizeof(TCHAR)) > 0 )
        return _ttoi(strCodePage);
    else
        return GetACP();
}

BOOL AwtMenuItem::CheckMenuCreation(JNIEnv *env, jobject self, HMENU hMenu)
{
    // fix for 5088782
    // check if CreateMenu() returns not null value and if it does -
    //   create an InternalError or OutOfMemoryError based on GetLastError().
    //   This error is set to createError field of WObjectPeer and then
    //   checked and thrown in WMenuPeer or WMenuItemPeer constructor. We
    //   can't throw an error here because this code is invoked on Toolkit thread
    // return TRUE if menu is created successfully, FALSE otherwise
    if (hMenu == NULL)
    {
        DWORD dw = GetLastError();
        jobject createError = NULL;
        if (dw == ERROR_OUTOFMEMORY)
        {
            jstring errorMsg = JNU_NewStringPlatform(env, L"too many menu handles");
            if (errorMsg == NULL) {
                throw std::bad_alloc();
            }
            createError = JNU_NewObjectByName(env, "java/lang/OutOfMemoryError",
                                                   "(Ljava/lang/String;)V",
                                                   errorMsg);
            env->DeleteLocalRef(errorMsg);
        }
        else
        {
            TCHAR *buf;
            FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
                NULL, dw, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                (LPTSTR)&buf, 0, NULL);
            jstring s = JNU_NewStringPlatform(env, buf);
            if (s == NULL) {
                throw std::bad_alloc();
            }
            createError = JNU_NewObjectByName(env, "java/lang/InternalError",
                                                   "(Ljava/lang/String;)V", s);
            LocalFree(buf);
            env->DeleteLocalRef(s);
        }
        if (createError == NULL) {
            throw std::bad_alloc();
        }
        env->SetObjectField(self, AwtObject::createErrorID, createError);
        env->DeleteLocalRef(createError);
        return FALSE;
    }
    return TRUE;
}

/*
 * Link the C++, Java peer together
 */
void AwtMenuItem::LinkObjects(JNIEnv *env, jobject peer)
{
    m_peerObject = env->NewGlobalRef(peer);
    JNI_SET_PDATA(peer, this);
}

AwtMenuItem* AwtMenuItem::Create(jobject peer, jobject menuPeer)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtMenuItem* item = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }
        if (!AwtToolkit::GetInstance().isFreeIDAvailable()) {
            return NULL;
        }

        JNI_CHECK_NULL_RETURN_NULL(menuPeer, "peer");

        /* target is a java.awt.MenuItem  */
        target = env->GetObjectField(peer, AwtObject::targetID);

        AwtMenu* menu = (AwtMenu *)JNI_GET_PDATA(menuPeer);
        item = new AwtMenuItem();
        jboolean isCheckbox =
            (jboolean)env->GetBooleanField(peer, AwtMenuItem::isCheckboxID);
        if (isCheckbox) {
            item->SetCheckbox();
        }

        item->LinkObjects(env, peer);
        item->SetMenuContainer(menu);
        item->SetNewID();
        if (menu != NULL) {
            menu->AddItem(item);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

    env->DeleteLocalRef(target);
    return item;
}

MsgRouting AwtMenuItem::WmNotify(UINT notifyCode)
{
    return mrDoDefault;
}

// This function returns a local reference
jobject
AwtMenuItem::GetFont(JNIEnv *env)
{
    jobject self = GetPeer(env);
    jobject target = env->GetObjectField(self, AwtObject::targetID);
    jobject font = JNU_CallMethodByName(env, 0, target, "getFont_NoClientCode", "()Ljava/awt/Font;").l;
    env->DeleteLocalRef(target);
    if (env->ExceptionCheck()) {
        throw std::bad_alloc();
    }

    if (font == NULL) {
        font = env->NewLocalRef(GetDefaultFont(env));
        if (env->ExceptionCheck()) {
            throw std::bad_alloc();
        }
    }

    return font;
}

jobject
AwtMenuItem::GetDefaultFont(JNIEnv *env) {
    if (AwtMenuItem::systemFont == NULL) {
        jclass cls = env->FindClass("sun/awt/windows/WMenuItemPeer");
        if (cls == NULL) {
            throw std::bad_alloc();
        }

        AwtMenuItem::systemFont =
            env->CallStaticObjectMethod(cls, AwtMenuItem::getDefaultFontMID);
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(cls);
            throw std::bad_alloc();
        }

        AwtMenuItem::systemFont = env->NewGlobalRef(AwtMenuItem::systemFont);
        if (systemFont == NULL) {
            env->DeleteLocalRef(cls);
            throw std::bad_alloc();
        }
    }
    return AwtMenuItem::systemFont;
}

void
AwtMenuItem::DrawSelf(DRAWITEMSTRUCT& drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(4) < 0) {
        return;
    }

    // self is sun.awt.windows.WMenuItemPeer
    jobject self = GetPeer(env);

    //  target is java.awt.MenuItem
    jobject target = env->GetObjectField(self, AwtObject::targetID);

    HDC hDC = drawInfo.hDC;
    RECT rect = drawInfo.rcItem;
    RECT textRect = rect;
    SIZE size;

    DWORD crBack,crText;
    HBRUSH hbrBack;

    jobject font;
    try {
        font = GetFont(env);
    } catch (std::bad_alloc&) {
        env->DeleteLocalRef(target);
        throw;
    }

    jstring text = GetJavaString(env);
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(target);
        throw std::bad_alloc();
    }
    size = AwtFont::getMFStringSize(hDC, font, text);

    /* 4700350: If the font size is taller than the menubar, change to the
     * default font.  Otherwise, menu text is painted over the title bar and
     * client area.  -bchristi
     */
    if (IsTopMenu() && size.cy > ::GetSystemMetrics(SM_CYMENU)) {
        env->DeleteLocalRef(font);
        try {
            font = env->NewLocalRef(GetDefaultFont(env));
        } catch (std::bad_alloc&) {
            env->DeleteLocalRef(target);
            env->DeleteLocalRef(text);
            throw;
        }
        size = AwtFont::getMFStringSize(hDC, font, text);
    }

    /* Fix for bug 4257944 by ssi@sparc.spb.su
    * check state of the parent
    */
    AwtMenu* menu = GetMenuContainer();
    DASSERT(menu != NULL && GetID() >= 0);

    //Check whether the MenuItem is disabled.
    BOOL bEnabled = (jboolean)env->GetBooleanField(target,
                                                   AwtMenuItem::enabledID);
    if (menu != NULL) {
        bEnabled = bEnabled && !menu->IsDisabledAndPopup();
    }

    if ((drawInfo.itemState) & (ODS_SELECTED)) {
        // Set background and text colors for selected item
        crBack = ::GetSysColor (COLOR_HIGHLIGHT);
        // Disabled text must be drawn in gray.
        crText = ::GetSysColor(bEnabled? COLOR_HIGHLIGHTTEXT : COLOR_GRAYTEXT);
    } else {
        // COLOR_MENUBAR is only defined on WindowsXP. Our binaries are
        // built on NT, hence the below ifdef.

#ifndef COLOR_MENUBAR
#define COLOR_MENUBAR 30
#endif
        // Set background and text colors for unselected item
        if (IS_WINXP && IsTopMenu() && AwtDesktopProperties::IsXPStyle()) {
            crBack = ::GetSysColor (COLOR_MENUBAR);
        } else {
            crBack = ::GetSysColor (COLOR_MENU);
        }
        // Disabled text must be drawn in gray.
        crText = ::GetSysColor (bEnabled ? COLOR_MENUTEXT : COLOR_GRAYTEXT);
    }

    // Fill item rectangle with background color
    hbrBack = ::CreateSolidBrush (crBack);
    DASSERT(hbrBack);
    VERIFY(::FillRect (hDC, &rect, hbrBack));
    VERIFY(::DeleteObject (hbrBack));

    // Set current background and text colors
    ::SetBkColor (hDC, crBack);
    ::SetTextColor (hDC, crText);

    int nOldBkMode = ::SetBkMode(hDC, OPAQUE);
    DASSERT(nOldBkMode != 0);

    //draw check mark
    int checkWidth = ::GetSystemMetrics(SM_CXMENUCHECK);
    // Workaround for CR#6401956
    if (IS_WINVISTA) {
        AdjustCheckWidth(checkWidth);
    }

    if (IsCheckbox()) {
        // means that target is a java.awt.CheckboxMenuItem
        jboolean state =
            (jboolean)env->GetBooleanField(target, AwtMenuItem::stateID);
        if (state) {
            DASSERT(drawInfo.itemState & ODS_CHECKED);
            RECT checkRect;
            ::CopyRect(&checkRect, &textRect);
            if (GetRTL())
                checkRect.left = checkRect.right - checkWidth;
            else
                checkRect.right = checkRect.left + checkWidth;

            DrawCheck(hDC, checkRect);
        }
    }

    ::SetBkMode(hDC, TRANSPARENT);
    int x = 0;
    //draw string
    if (!IsTopMenu()){
        textRect.left += checkWidth;
        x = (GetRTL()) ? textRect.right - checkWidth - size.cx : textRect.left;
    } else {
        x = textRect.left = (textRect.left + textRect.right - size.cx) / 2;
    }

    int y = (textRect.top+textRect.bottom-size.cy)/2;

    // Text must be drawn in emboss if the Menu is disabled and not selected.
    BOOL bEmboss = !bEnabled && !(drawInfo.itemState & ODS_SELECTED);
    if (bEmboss) {
        ::SetTextColor(hDC, GetSysColor(COLOR_BTNHILIGHT));
        AwtFont::drawMFString(hDC, font, text, x + 1, y + 1, GetCodePage());
        ::SetTextColor(hDC, GetSysColor(COLOR_BTNSHADOW));
    }
    AwtFont::drawMFString(hDC, font, text, x, y, GetCodePage());

    jstring shortcutLabel =
        (jstring)env->GetObjectField(self, AwtMenuItem::shortcutLabelID);
    if (!IsTopMenu() && shortcutLabel != NULL) {
        UINT oldAlign = 0;
        if (GetRTL()){
            oldAlign = ::SetTextAlign(hDC, TA_LEFT);
            AwtFont::drawMFString(hDC, font, shortcutLabel, textRect.left, y,
                                  GetCodePage());
        } else {
            oldAlign = ::SetTextAlign(hDC, TA_RIGHT);
            AwtFont::drawMFString(hDC, font, shortcutLabel,
                                  textRect.right - checkWidth, y,
                                  GetCodePage());
        }

        ::SetTextAlign(hDC, oldAlign);
    }

    VERIFY(::SetBkMode(hDC,nOldBkMode));

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(text);
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(shortcutLabel);
}

/*
 * This function helps us to prevent check-mark's
 * distortion appeared due to changing of default
 * settings on Vista
 */
void AwtMenuItem::AdjustCheckWidth(int& checkWidth)
{
    if (checkWidth == SM_CXMENUCHECK_DEFAULT_ON_VISTA) {
        checkWidth = SM_CXMENUCHECK_DEFAULT_ON_XP;
    }
}

void AwtMenuItem::DrawItem(DRAWITEMSTRUCT& drawInfo)
{
    DASSERT(drawInfo.CtlType == ODT_MENU);

    if (drawInfo.itemID != m_Id)
        return;

    DrawSelf(drawInfo);
}

void AwtMenuItem::MeasureSelf(HDC hDC, MEASUREITEMSTRUCT& measureInfo)
{
    JNIEnv *env =(JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(4) < 0) {
        return;
    }

    /* self is a sun.awt.windows.WMenuItemPeer */
    jobject self = GetPeer(env);

    /* font is a java.awt.Font */
    jobject font = GetFont(env);
    jstring text = GetJavaString(env);
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(font);
        throw std::bad_alloc();
    }
    SIZE size = AwtFont::getMFStringSize(hDC, font, text);

    /* 4700350: If the font size is taller than the menubar, change to the
     * default font.  Otherwise, menu text is painted over the title bar and
     * client area.  -bchristi
     */
    if (IsTopMenu() && size.cy > ::GetSystemMetrics(SM_CYMENU)) {
        jobject defFont;
        try {
            defFont = GetDefaultFont(env);
        } catch (std::bad_alloc&) {
            env->DeleteLocalRef(text);
            env->DeleteLocalRef(font);
            throw;
        }
        env->DeleteLocalRef(font);
        font = env->NewLocalRef(defFont);
        size = AwtFont::getMFStringSize(hDC, font, text);
    }

    jstring fontName =
        (jstring)JNU_CallMethodByName(env, 0,font, "getName",
                                      "()Ljava/lang/String;").l;
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(text);
        env->DeleteLocalRef(font);
        throw std::bad_alloc();
    }

    /* fontMetrics is a Hsun_awt_windows_WFontMetrics */
    jobject fontMetrics =  GetFontMetrics(env, font);
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(text);
        env->DeleteLocalRef(font);
        env->DeleteLocalRef(fontName);
        throw std::bad_alloc();
    }

//     int height = env->GetIntField(fontMetrics, AwtFont::heightID);
    int height = (jint)JNU_CallMethodByName(env, 0, fontMetrics, "getHeight",
                                            "()I").i;
    if (env->ExceptionCheck()) {
        env->DeleteLocalRef(text);
        env->DeleteLocalRef(font);
        env->DeleteLocalRef(fontName);
        env->DeleteLocalRef(fontMetrics);
        throw std::bad_alloc();
    }

    measureInfo.itemHeight = height;
    measureInfo.itemHeight += measureInfo.itemHeight/3;
    // 3 is a heuristic number
    measureInfo.itemWidth = size.cx;
    if (!IsTopMenu()) {
        int checkWidth = ::GetSystemMetrics(SM_CXMENUCHECK);
        // Workaround for CR#6401956
        if (IS_WINVISTA) {
            AdjustCheckWidth(checkWidth);
        }
        measureInfo.itemWidth += checkWidth;

        // Add in shortcut width, if one exists.
        jstring shortcutLabel =
            (jstring)env->GetObjectField(self, AwtMenuItem::shortcutLabelID);
        if (shortcutLabel != NULL) {
            size = AwtFont::getMFStringSize(hDC, font, shortcutLabel);
            measureInfo.itemWidth += size.cx + checkWidth;
            env->DeleteLocalRef(shortcutLabel);
        }
    }
    env->DeleteLocalRef(text);
    env->DeleteLocalRef(font);
    env->DeleteLocalRef(fontName);
    env->DeleteLocalRef(fontMetrics);
}

void AwtMenuItem::MeasureItem(HDC hDC, MEASUREITEMSTRUCT& measureInfo)
{
    DASSERT(measureInfo.CtlType == ODT_MENU);

    if (measureInfo.itemID != m_Id)
        return;

    MeasureSelf(hDC, measureInfo);
}

jobject AwtMenuItem::GetFontMetrics(JNIEnv *env, jobject font)
{
    static jobject toolkit = NULL;
    if (toolkit == NULL) {
        if (env->PushLocalFrame(2) < 0)
            return NULL;
        jclass cls = env->FindClass("java/awt/Toolkit");
        CHECK_NULL_RETURN(cls, NULL);
        jobject toolkitLocal =
            env->CallStaticObjectMethod(cls, AwtToolkit::getDefaultToolkitMID);
        env->DeleteLocalRef(cls);
        CHECK_NULL_RETURN(toolkitLocal, NULL);
        toolkit = env->NewGlobalRef(toolkitLocal);
        env->DeleteLocalRef(toolkitLocal);
        CHECK_NULL_RETURN(toolkit, NULL);
        env->PopLocalFrame(0);
    }
    /*
    JNU_PrintClass(env, "toolkit", toolkit);
    JNU_PrintClass(env, "font", font);

    jclass cls = env->FindClass("java/awt/Toolkit");
    jmethodID mid = env->GetMethodID(cls, "getFontMetrics",
                                     "(Ljava/awt/Font;)Ljava/awt/FontMetrics;");
    jstring fontName =
        (jstring)JNU_CallMethodByName(env, 0,font, "getName",
                                      "()Ljava/lang/String;").l;
    JNU_PrintString(env, "font name", fontName);

    fprintf(stderr, "mid: %x\n", mid);
    fprintf(stderr, "cached mid: %x\n", AwtToolkit::getFontMetricsMID);
    DASSERT(!safe_ExceptionOccurred(env));
    */
    jobject fontMetrics =
      env->CallObjectMethod(toolkit, AwtToolkit::getFontMetricsMID, font);
    DASSERT(!safe_ExceptionOccurred(env));

    return fontMetrics;
}

BOOL AwtMenuItem::IsTopMenu()
{
    return FALSE;
}

void AwtMenuItem::DrawCheck(HDC hDC, RECT rect)
{
    if (bmpCheck == NULL) {
        bmpCheck = ::LoadBitmap(AwtToolkit::GetInstance().GetModuleHandle(),
                                TEXT("CHECK_BITMAP"));
        DASSERT(bmpCheck != NULL);
    }

#define BM_SIZE 26  /* height and width of check.bmp */

    // Square the rectangle, so the check is proportional.
    int width = rect.right - rect.left;
    int diff = max(rect.bottom - rect.top - width, 0) ;
    int bottom = diff / 2;
    rect.bottom -= bottom;
    rect.top += diff - bottom;

    HDC hdcBitmap = ::CreateCompatibleDC(hDC);
    DASSERT(hdcBitmap != NULL);
    HBITMAP hbmSave = (HBITMAP)::SelectObject(hdcBitmap, bmpCheck);
    VERIFY(::StretchBlt(hDC, rect.left, rect.top,
                        rect.right - rect.left, rect.bottom - rect.top,
                        hdcBitmap, 0, 0, BM_SIZE, BM_SIZE, SRCCOPY));
    ::SelectObject(hdcBitmap, hbmSave);
    VERIFY(::DeleteDC(hdcBitmap));
}

void AwtMenuItem::DoCommand()
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    // peer is sun.awt.windows.WMenuItemPeer
    jobject peer = GetPeer(env);

    if (IsCheckbox()) {
        UINT nState = ::GetMenuState(GetMenuContainer()->GetHMenu(),
                                     GetID(), MF_BYCOMMAND);
        DASSERT(nState != 0xFFFFFFFF);
        DoCallback("handleAction", "(Z)V", ((nState & MF_CHECKED) == 0));
    } else {
        DoCallback("handleAction", "(JI)V", ::JVM_CurrentTimeMillis(NULL, 0),
                   (jint)AwtComponent::GetActionModifiers());
    }
}

void AwtMenuItem::SetLabel(LPCTSTR sb)
{
    AwtMenu* menu = GetMenuContainer();
    /* Fix for bug 4257944 by ssi@sparc.spb.su
    * check parent
    */
    if (menu == NULL) return;
    DASSERT(menu != NULL && GetID() >= 0);

/*
 * SetMenuItemInfo is replaced by this code for fix bug 4261935
 */
    HMENU hMenu = menu->GetHMenu();
    MENUITEMINFO mii, mii1;

    // get full information about menu item
    memset(&mii, 0, sizeof(MENUITEMINFO));
    mii.cbSize = sizeof(MENUITEMINFO);
    mii.fMask = MIIM_CHECKMARKS | MIIM_DATA | MIIM_ID
              | MIIM_STATE | MIIM_SUBMENU | MIIM_TYPE;

    ::GetMenuItemInfo(hMenu, GetID(), FALSE, &mii);

    mii.fType = MFT_OWNERDRAW;
    mii.dwTypeData = (LPTSTR)(*sb);

    // find index by menu item id
    int nMenuItemCount = ::GetMenuItemCount(hMenu);
    int idx;
    for (idx = 0; (idx < nMenuItemCount); idx++) {
        memset(&mii1, 0, sizeof(MENUITEMINFO));
        mii1.cbSize = sizeof mii1;
        mii1.fMask = MIIM_ID;
        ::GetMenuItemInfo(hMenu, idx, TRUE, &mii1);
        if (mii.wID == mii1.wID) break;
    }

    ::RemoveMenu(hMenu, idx, MF_BYPOSITION);
    ::InsertMenuItem(hMenu, idx, TRUE, &mii);

    RedrawMenuBar();
}

void AwtMenuItem::Enable(BOOL isEnabled)
{
    AwtMenu* menu = GetMenuContainer();
    /* Fix for bug 4257944 by ssi@sparc.spb.su
    * check state of the parent
    */
    if (menu == NULL) return;
    isEnabled = isEnabled && !menu->IsDisabledAndPopup();
    DASSERT(menu != NULL && GetID() >= 0);
    VERIFY(::EnableMenuItem(menu->GetHMenu(), GetID(),
                            MF_BYCOMMAND | (isEnabled ? MF_ENABLED : MF_GRAYED))
           != 0xFFFFFFFF);

    RedrawMenuBar();
}

void AwtMenuItem::SetState(BOOL isChecked)
{
    AwtMenu* menu = GetMenuContainer();
    /* Fix for bug 4257944 by ssi@sparc.spb.su
    * check parent
    */
    if (menu == NULL) return;
    DASSERT(menu != NULL && GetID() >= 0);
    VERIFY(::CheckMenuItem(menu->GetHMenu(), GetID(),
                           MF_BYCOMMAND | (isChecked ? MF_CHECKED : MF_UNCHECKED))
           != 0xFFFFFFFF);

    RedrawMenuBar();
}

/**
 * If the menu changes after the system has created the window,
 * this function must be called to draw the changed menu bar.
 */
void AwtMenuItem::RedrawMenuBar() {
    AwtMenu* menu = GetMenuContainer();
    if (menu != NULL && menu->GetMenuBar() == menu){
        menu->RedrawMenuBar();
    }
}

void AwtMenuItem::UpdateContainerLayout() {
    AwtMenu* menu = GetMenuContainer();
    if (menu != NULL) {
        DASSERT(menu != NULL && GetID() >= 0);
        menu->UpdateLayout();
    }
}

void AwtMenuItem::_SetLabel(void *param) {
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        SetLabelStruct *sls = (SetLabelStruct *)param;
        jobject self = sls->menuitem;
        jstring label = sls->label;

        int badAlloc = 0;
        AwtMenuItem *m = NULL;

        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);
        m = (AwtMenuItem *)pData;
//    if (::IsWindow(m->GetOwnerHWnd()))
        {
            // fix for bug 4251036 MenuItem setLabel(null/"") behaves differently
            // under Win32 and Solaris
            jstring empty = NULL;
            if (JNU_IsNull(env, label))
            {
                empty = JNU_NewStringPlatform(env, TEXT(""));
            }
            if (env->ExceptionCheck()) {
                badAlloc = 1;
                goto ret;
            }
            LPCTSTR labelPtr;
            if (empty != NULL)
            {
                labelPtr = JNU_GetStringPlatformChars(env, empty, 0);
            }
            else
            {
                labelPtr = JNU_GetStringPlatformChars(env, label, 0);
            }
            if (labelPtr == NULL)
            {
                badAlloc = 1;
            }
            else
            {
                DASSERT(!IsBadStringPtr(labelPtr, 20));
                m->SetLabel(labelPtr);
                if (empty != NULL)
                {
                    JNU_ReleaseStringPlatformChars(env, empty, labelPtr);
                }
                else
                {
                    JNU_ReleaseStringPlatformChars(env, label, labelPtr);
                }
            }
            if (empty != NULL)
            {
                env->DeleteLocalRef(empty);
            }
        }

ret:
        env->DeleteGlobalRef(self);
        if (label != NULL)
        {
            env->DeleteGlobalRef(label);
        }

        delete sls;

        if (badAlloc)
        {
            throw std::bad_alloc();
        }
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenuItem::_SetLabel, param);
    }
}

void AwtMenuItem::_UpdateLayout(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        jobject self = (jobject)param;

        AwtMenuItem *m = NULL;

        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);

        m = (AwtMenuItem *)pData;

        m->UpdateContainerLayout();
ret:
        env->DeleteGlobalRef(self);
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenuItem::_UpdateLayout, param);
    }
}

void AwtMenuItem::_SetEnable(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        SetEnableStruct *ses = (SetEnableStruct*) param;
        jobject self = ses->menuitem;
        jboolean isEnabled = ses->isEnabled;

        AwtMenuItem *m = NULL;

        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);

        m = (AwtMenuItem *)pData;

        m->Enable(isEnabled);
ret:
        env->DeleteGlobalRef(self);
        delete ses;
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenuItem::_SetEnable, param);
    }
}

void AwtMenuItem::_SetState(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        SetStateStruct *sts = (SetStateStruct*) param;
        jobject self = sts->menuitem;
        jboolean isChecked = sts->isChecked;

        AwtMenuItem *m = NULL;

        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);
        m = (AwtMenuItem *)pData;
        m->SetState(isChecked);
ret:
        env->DeleteGlobalRef(self);
        delete sts;
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenuItem::_SetState, param);
    }
}
BOOL AwtMenuItem::IsSeparator() {
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0) {
        return FALSE;
    }
    jobject jitem = GetTarget(env);
    jstring label  =
        (jstring)(env)->GetObjectField(jitem, AwtMenuItem::labelID);
    if (label == NULL) {
        env->DeleteLocalRef(label);
        env->DeleteLocalRef(jitem);
        return FALSE; //separator must has '-' as label.
    }
    LPCWSTR labelW = JNU_GetStringPlatformChars(env, label, NULL);
    BOOL isSeparator = (labelW && (wcscmp(labelW, L"-") == 0));
    JNU_ReleaseStringPlatformChars(env, label, labelW);

    env->DeleteLocalRef(label);
    env->DeleteLocalRef(jitem);

    return isSeparator;
}

/************************************************************************
 * MenuComponent native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_MenuComponent_initIDs(JNIEnv *env, jclass cls)
{
}

} /* extern "C" */


/************************************************************************
 * MenuItem native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_MenuItem_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtMenuItem::labelID = env->GetFieldID(cls, "label", "Ljava/lang/String;");
    CHECK_NULL(AwtMenuItem::labelID);
    AwtMenuItem::enabledID = env->GetFieldID(cls, "enabled", "Z");

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * CheckboxMenuItem fields
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_CheckboxMenuItem_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtMenuItem::stateID = env->GetFieldID(cls, "state", "Z");

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WMenuItemPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtMenuItem::isCheckboxID = env->GetFieldID(cls, "isCheckbox", "Z");
    CHECK_NULL(AwtMenuItem::isCheckboxID);
    AwtMenuItem::shortcutLabelID = env->GetFieldID(cls, "shortcutLabel",
                                                   "Ljava/lang/String;");
    CHECK_NULL(AwtMenuItem::shortcutLabelID);
    AwtMenuItem::getDefaultFontMID =
        env->GetStaticMethodID(cls, "getDefaultFont", "()Ljava/awt/Font;");

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    _setLabel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer__1setLabel(JNIEnv *env, jobject self,
                                              jstring label)
{
    TRY;

    SetLabelStruct *sls = new SetLabelStruct;
    sls->menuitem = env->NewGlobalRef(self);
    sls->label = (label == NULL) ? NULL : (jstring)env->NewGlobalRef(label);

    AwtToolkit::GetInstance().SyncCall(AwtMenuItem::_SetLabel, sls);
    // global refs and sls are deleted in _SetLabel

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    _setFont
 * Signature: (Ljava/awt/Font;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer__1setFont(JNIEnv *env, jobject self, jobject)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    // Current implementation of AwtMenuItem get font attribute from the peer
    // directly, so we ignore it here, but update current menu layout.
    AwtToolkit::GetInstance().SyncCall(AwtMenuItem::_UpdateLayout, selfGlobalRef);
    // selfGlobalRef is deleted in _UpdateLayout

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WMenuPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer_create(JNIEnv *env, jobject self,
                                          jobject menu)
{
    TRY;

    AwtToolkit::CreateComponent(self, menu,
                                (AwtToolkit::ComponentFactory)
                                AwtMenuItem::Create);
    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    enable
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer_enable(JNIEnv *env, jobject self,
                                          jboolean on)
{
    TRY;

    SetEnableStruct *ses = new SetEnableStruct;
    ses->menuitem = env->NewGlobalRef(self);
    ses->isEnabled = on;

    AwtToolkit::GetInstance().SyncCall(AwtMenuItem::_SetEnable, ses);
    // global refs and ses are deleted in _SetEnable

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuItemPeer
 * Method:    _dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuItemPeer__1dispose(JNIEnv *env, jobject self)
{
    TRY_NO_HANG;

    AwtObject::_Dispose(self);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */

/************************************************************************
 * WCheckboxMenuItemPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WCheckboxMenuItemPeer
 * Method:    setState
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WCheckboxMenuItemPeer_setState(JNIEnv *env, jobject self,
                                                    jboolean on)
{
    TRY;

    SetStateStruct *sts = new SetStateStruct;
    sts->menuitem = env->NewGlobalRef(self);
    sts->isChecked = on;

    AwtToolkit::GetInstance().SyncCall(AwtMenuItem::_SetState, sts);
    // global refs and sts are deleted in _SetState

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
