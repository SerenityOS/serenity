/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_PopupMenu.h"

#include "awt_Event.h"
#include "awt_Window.h"

#include <sun_awt_windows_WPopupMenuPeer.h>
#include <java_awt_Event.h>

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _Show method
struct ShowStruct {
    jobject self;
    jobject event;
};

/************************************************************************
 * AwtPopupMenu class methods
 */

AwtPopupMenu::AwtPopupMenu() {
    m_parent = NULL;
}

AwtPopupMenu::~AwtPopupMenu()
{
}

void AwtPopupMenu::Dispose()
{
    m_parent = NULL;

    AwtMenu::Dispose();
}

LPCTSTR AwtPopupMenu::GetClassName() {
  return TEXT("SunAwtPopupMenu");
}

/* Create a new AwtPopupMenu object and menu.   */
AwtPopupMenu* AwtPopupMenu::Create(jobject self, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtPopupMenu* popupMenu = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        JNI_CHECK_NULL_GOTO(parent, "peer", done);
        AwtComponent* awtParent = (AwtComponent*) JNI_GET_PDATA(parent);

        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        popupMenu = new AwtPopupMenu();

        SetLastError(0);
        HMENU hMenu = ::CreatePopupMenu();
        // fix for 5088782
        if (!CheckMenuCreation(env, self, hMenu))
        {
            env->DeleteLocalRef(target);
            return NULL;
        }

        popupMenu->SetHMenu(hMenu);

        popupMenu->LinkObjects(env, self);
        popupMenu->SetParent(awtParent);
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    return popupMenu;
}

void AwtPopupMenu::Show(JNIEnv *env, jobject event, BOOL isTrayIconPopup)
{
    /*
     * For not TrayIcon popup.
     * Convert the event's XY to absolute coordinates.  The XY is
     * relative to the origin component, which is passed by PopupMenu
     * as the event's target.
     */
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }
    jobject origin = (env)->GetObjectField(event, AwtEvent::targetID);
    jobject peerOrigin = GetPeerForTarget(env, origin);
    PDATA pData;
    JNI_CHECK_PEER_GOTO(peerOrigin, done);
    {
        AwtComponent* awtOrigin = (AwtComponent*)pData;
        POINT pt;
        UINT flags = 0;
        pt.x = (env)->GetIntField(event, AwtEvent::xID);
        pt.y = (env)->GetIntField(event, AwtEvent::yID);

        if (!isTrayIconPopup) {
            ::MapWindowPoints(awtOrigin->GetHWnd(), 0, (LPPOINT)&pt, 1);

            // Adjust to account for the Inset values
            RECT rctInsets;
            awtOrigin->GetInsets(&rctInsets);
            pt.x -= rctInsets.left;
            pt.y -= rctInsets.top;

            flags = TPM_LEFTALIGN | TPM_RIGHTBUTTON;

        } else {
            ::SetForegroundWindow(awtOrigin->GetHWnd());

            flags = TPM_NONOTIFY | TPM_RIGHTALIGN | TPM_RIGHTBUTTON | TPM_BOTTOMALIGN;
        }

        /* Invoke the popup. */
        ::TrackPopupMenu(GetHMenu(), flags, pt.x, pt.y, 0, awtOrigin->GetHWnd(), NULL);

        if (isTrayIconPopup) {
            ::PostMessage(awtOrigin->GetHWnd(), WM_NULL, 0, 0);
        }
    }
 done:
    env->DeleteLocalRef(origin);
    env->DeleteLocalRef(peerOrigin);
}

void AwtPopupMenu::_Show(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    static jclass popupMenuCls;
    if (popupMenuCls == NULL) {
        jclass popupMenuClsLocal = env->FindClass("java/awt/PopupMenu");
        if (popupMenuClsLocal != NULL) {
            popupMenuCls = (jclass)env->NewGlobalRef(popupMenuClsLocal);
            env->DeleteLocalRef(popupMenuClsLocal);
        }
    }

    static jfieldID isTrayIconPopupID;
    if (popupMenuCls != NULL && isTrayIconPopupID == NULL) {
        isTrayIconPopupID = env->GetFieldID(popupMenuCls, "isTrayIconPopup", "Z");
        DASSERT(isTrayIconPopupID);
    }

    ShowStruct *ss = (ShowStruct*)param;
    if (ss->self != NULL && isTrayIconPopupID != NULL) {
        PDATA pData = JNI_GET_PDATA(ss->self);
        if (pData) {
            AwtPopupMenu *p = (AwtPopupMenu *)pData;
            jobject target = p->GetTarget(env);
            BOOL isTrayIconPopup = env->GetBooleanField(target, isTrayIconPopupID);
            env->DeleteLocalRef(target);
            p->Show(env, ss->event, isTrayIconPopup);
        }
    }
    if (ss->self != NULL) {
        env->DeleteGlobalRef(ss->self);
    }
    if (ss->event != NULL) {
        env->DeleteGlobalRef(ss->event);
    }
    delete ss;
    if (isTrayIconPopupID == NULL) {
        throw std::bad_alloc();
    }
}

void AwtPopupMenu::AddItem(AwtMenuItem *item)
{
    AwtMenu::AddItem(item);
    if (GetMenuContainer() != NULL) return;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    if (!(jboolean)env->GetBooleanField(target, AwtMenuItem::enabledID)) {
        item->Enable(FALSE);
    }
    env->DeleteLocalRef(target);
}

void AwtPopupMenu::Enable(BOOL isEnabled)
{
    AwtMenu *menu = GetMenuContainer();
    if (menu != NULL) {
        AwtMenu::Enable(isEnabled);
        return;
    }
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    jobject target = GetTarget(env);
    int nCount = CountItem(target);
    for (int i = 0; i < nCount; ++i) {
        AwtMenuItem *item = GetItem(target,i);
        jobject jitem = item->GetTarget(env);
        BOOL bItemEnabled = isEnabled && (jboolean)env->GetBooleanField(jitem,
            AwtMenuItem::enabledID);
        jstring labelStr = static_cast<jstring>(env->GetObjectField(jitem, AwtMenuItem::labelID));
        LPCWSTR labelStrW = JNU_GetStringPlatformChars(env, labelStr, NULL);
        if (labelStrW  && wcscmp(labelStrW, L"-") != 0) {
            item->Enable(bItemEnabled);
        }
        JNU_ReleaseStringPlatformChars(env, labelStr, labelStrW);
        env->DeleteLocalRef(labelStr);
        env->DeleteLocalRef(jitem);
    }
    env->DeleteLocalRef(target);
}

BOOL AwtPopupMenu::IsDisabledAndPopup()
{
    if (GetMenuContainer() != NULL) return FALSE;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return FALSE;
    }
    jobject target = GetTarget(env);
    BOOL bEnabled = (jboolean)env->GetBooleanField(target,
            AwtMenuItem::enabledID);
    env->DeleteLocalRef(target);
    return !bEnabled;
}

/************************************************************************
 * WPopupMenuPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WPopupMenuPeer
 * Method:    createMenu
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WPopupMenuPeer_createMenu(JNIEnv *env, jobject self,
                                               jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(
        self, parent, (AwtToolkit::ComponentFactory)AwtPopupMenu::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WPopupMenuPeer
 * Method:    _show
 * Signature: (Ljava/awt/Event;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WPopupMenuPeer__1show(JNIEnv *env, jobject self,
                                           jobject event)
{
    TRY;

    ShowStruct *ss = new ShowStruct;
    ss->self = env->NewGlobalRef(self);
    ss->event = env->NewGlobalRef(event);

    // fix for 6268046: invoke the function without CriticalSection's synchronization
    AwtToolkit::GetInstance().InvokeFunction(AwtPopupMenu::_Show, ss);
    // global ref and ss are deleted in _Show()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
