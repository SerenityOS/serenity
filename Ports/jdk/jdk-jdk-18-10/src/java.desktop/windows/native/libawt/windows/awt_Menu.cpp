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

#include "awt_Menu.h"
#include "awt_MenuBar.h"
#include "awt_Frame.h"
#include <java_awt_Menu.h>
#include <sun_awt_windows_WMenuPeer.h>
#include <java_awt_MenuBar.h>
#include <sun_awt_windows_WMenuBarPeer.h>

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _DelItem() method
struct DelItemStruct {
    jobject menuitem;
    jint index;
};

/************************************************************************
 * AwtMenuItem fields
 */

jmethodID AwtMenu::countItemsMID;
jmethodID AwtMenu::getItemMID;


/************************************************************************
 * AwtMenuItem methods
 */

AwtMenu::AwtMenu() {
    m_hMenu = NULL;
}

AwtMenu::~AwtMenu()
{
}

void AwtMenu::Dispose()
{
    if (m_hMenu != NULL) {
        /*
         * Don't verify -- may not be a valid anymore if its window
         * was disposed of first.
         */
        ::DestroyMenu(m_hMenu);
        m_hMenu = NULL;
    }
    AwtMenuItem::Dispose();
}

LPCTSTR AwtMenu::GetClassName() {
    return TEXT("SunAwtMenu");
}

/* Create a new AwtMenu object and menu.   */
AwtMenu* AwtMenu::Create(jobject self, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtMenu* menu = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        JNI_CHECK_NULL_GOTO(parent, "peer", done);
        AwtMenu* parentMenu = (AwtMenu*) JNI_GET_PDATA(parent);

        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        menu = new AwtMenu();

        SetLastError(0);
        HMENU hMenu = ::CreateMenu();
        // fix for 5088782
        if (!CheckMenuCreation(env, self, hMenu))
        {
            env->DeleteLocalRef(target);
            return NULL;
        }

        menu->SetHMenu(hMenu);

        menu->LinkObjects(env, self);
        menu->SetMenuContainer(parentMenu);
        if (parentMenu != NULL) {
            parentMenu->AddItem(menu);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    if (target != NULL) {
        env->DeleteLocalRef(target);
    }

    return menu;
}

void AwtMenu::_DelItem(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        DelItemStruct *dis = (DelItemStruct*) param;
        jobject self = dis->menuitem;
        jint index = dis->index;

        AwtMenu *m = NULL;
        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);
        m = (AwtMenu *)pData;
        m->DeleteItem(static_cast<UINT>(index));
ret:
        env->DeleteGlobalRef(self);
        delete dis;
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenu::_DelItem, param);
    }
}

void AwtMenu::UpdateLayout()
{
    UpdateLayout(GetHMenu());
    RedrawMenuBar();
}

void AwtMenu::UpdateLayout(const HMENU hmenu)
{
    const int nMenuItemCount = ::GetMenuItemCount(hmenu);
    static MENUITEMINFO  mii;
    for (int idx = 0; idx < nMenuItemCount; ++idx) {
        memset(&mii, 0, sizeof(mii));
        mii.cbSize = sizeof(mii);
        mii.fMask = MIIM_CHECKMARKS | MIIM_DATA | MIIM_ID
                  | MIIM_STATE | MIIM_SUBMENU | MIIM_TYPE;
        if (::GetMenuItemInfo(hmenu, idx, TRUE, &mii)) {
            VERIFY(::RemoveMenu(hmenu, idx, MF_BYPOSITION));
            VERIFY(::InsertMenuItem(hmenu, idx, TRUE, &mii));
            if (mii.hSubMenu !=  NULL) {
                UpdateLayout(mii.hSubMenu);
            }
        }
    }
}

void AwtMenu::UpdateContainerLayout()
{
    AwtMenu* menu = GetMenuContainer();
    if (menu != NULL) {
        menu->UpdateLayout();
    } else {
        UpdateLayout();
    }
}

AwtMenuBar* AwtMenu::GetMenuBar() {
    return (GetMenuContainer() == NULL) ? NULL : GetMenuContainer()->GetMenuBar();
}

HWND AwtMenu::GetOwnerHWnd() {
    return (GetMenuContainer() == NULL) ? NULL : GetMenuContainer()->GetOwnerHWnd();
}

void AwtMenu::AddItem(AwtMenuItem* item)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0) {
        return;
    }

    if (item->IsSeparator()) {
        VERIFY(::AppendMenu(GetHMenu(), MF_SEPARATOR, 0, 0));
    } else {
        /* jitem is a java.awt.MenuItem */
        jobject jitem = item->GetTarget(env);

        jboolean enabled =
            (jboolean)env->GetBooleanField(jitem, AwtMenuItem::enabledID);

        UINT flags = MF_STRING | (enabled ? MF_ENABLED : MF_GRAYED);
        flags |= MF_OWNERDRAW;
        LPCTSTR itemInfo = (LPCTSTR) this;

        if (_tcscmp(item->GetClassName(), TEXT("SunAwtMenu")) == 0) {
            flags |= MF_POPUP;
            itemInfo = (LPCTSTR) item;
        }

        VERIFY(::AppendMenu(GetHMenu(), flags, item->GetID(), itemInfo));
        if (GetRTL()) {
            MENUITEMINFO  mif;
            memset(&mif, 0, sizeof(MENUITEMINFO));
            mif.cbSize = sizeof(MENUITEMINFO);
            mif.fMask = MIIM_TYPE;
            ::GetMenuItemInfo(GetHMenu(), item->GetID(), FALSE, &mif);
            mif.fType |= MFT_RIGHTJUSTIFY | MFT_RIGHTORDER;
            ::SetMenuItemInfo(GetHMenu(), item->GetID(), FALSE, &mif);
        }

        env->DeleteLocalRef(jitem);
    }
}

void AwtMenu::DeleteItem(UINT index)
{
    VERIFY(::RemoveMenu(GetHMenu(), index, MF_BYPOSITION));
}

void AwtMenu::SendDrawItem(AwtMenuItem* awtMenuItem,
                           DRAWITEMSTRUCT& drawInfo)
{
    awtMenuItem->DrawItem(drawInfo);
}

void AwtMenu::SendMeasureItem(AwtMenuItem* awtMenuItem,
                              HDC hDC, MEASUREITEMSTRUCT& measureInfo)
{
    awtMenuItem->MeasureItem(hDC, measureInfo);
}

int AwtMenu::CountItem(jobject target)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jint nCount = env->CallIntMethod(target, AwtMenu::countItemsMID);
    DASSERT(!safe_ExceptionOccurred(env));
    return nCount;
}

AwtMenuItem* AwtMenu::GetItem(jobject target, jint index)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0) {
        return NULL;
    }
    jobject menuItem = env->CallObjectMethod(target, AwtMenu::getItemMID,
                                             index);
    if (!menuItem) return NULL; // menu item was removed concurrently
    DASSERT(!safe_ExceptionOccurred(env));

    jobject wMenuItemPeer = GetPeerForTarget(env, menuItem);

    PDATA pData;
    AwtMenuItem* awtMenuItem = NULL;

    JNI_CHECK_PEER_GOTO(wMenuItemPeer, done);
    awtMenuItem = (AwtMenuItem*)pData;

 done:
    env->DeleteLocalRef(menuItem);
    env->DeleteLocalRef(wMenuItemPeer);

    return awtMenuItem;
}

void AwtMenu::DrawItems(DRAWITEMSTRUCT& drawInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
    /* target is a java.awt.Menu */
    jobject target = GetTarget(env);
    if(!target || env->ExceptionCheck()) return;
    int nCount = CountItem(target);
    for (int i = 0; i < nCount && !env->ExceptionCheck(); i++) {
        AwtMenuItem* awtMenuItem = GetItem(target, i);
        if (awtMenuItem != NULL) {
            SendDrawItem(awtMenuItem, drawInfo);
        }
    }
    env->DeleteLocalRef(target);
}

void AwtMenu::DrawItem(DRAWITEMSTRUCT& drawInfo)
{
    DASSERT(drawInfo.CtlType == ODT_MENU);

    if (drawInfo.itemID == GetID()) {
        DrawSelf(drawInfo);
        return;
    }
    DrawItems(drawInfo);
}

void AwtMenu::MeasureItems(HDC hDC, MEASUREITEMSTRUCT& measureInfo)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(1) < 0) {
        return;
    }
   /* target is a java.awt.Menu */
    jobject target = GetTarget(env);
    if(!target || env->ExceptionCheck()) return;
    int nCount = CountItem(target);
    for (int i = 0; i < nCount && !env->ExceptionCheck(); i++) {
        AwtMenuItem* awtMenuItem = GetItem(target, i);
        if (awtMenuItem != NULL) {
            SendMeasureItem(awtMenuItem, hDC, measureInfo);
        }
    }
    env->DeleteLocalRef(target);
}

void AwtMenu::MeasureItem(HDC hDC, MEASUREITEMSTRUCT& measureInfo)
{
    DASSERT(measureInfo.CtlType == ODT_MENU);

    if (measureInfo.itemID == GetID()) {
        MeasureSelf(hDC, measureInfo);
        return;
    }

    MeasureItems(hDC, measureInfo);
}

BOOL AwtMenu::IsTopMenu()
{
    return (GetMenuBar() == GetMenuContainer());
}

/************************************************************************
 * WMenuPeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Menu_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtMenu::countItemsMID = env->GetMethodID(cls, "countItemsImpl", "()I");
    DASSERT(AwtMenu::countItemsMID != NULL);
    CHECK_NULL(AwtMenu::countItemsMID);

    AwtMenu::getItemMID = env->GetMethodID(cls, "getItemImpl",
                                           "(I)Ljava/awt/MenuItem;");
    DASSERT(AwtMenu::getItemMID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WMenuPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WMenuPeer
 * Method:    delItem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuPeer_delItem(JNIEnv *env, jobject self,
                                       jint index)
{
    TRY;

    DelItemStruct *dis = new DelItemStruct;
    dis->menuitem = env->NewGlobalRef(self);
    dis->index = index;

    AwtToolkit::GetInstance().SyncCall(AwtMenu::_DelItem, dis);
    // global refs and dis are deleted in _DelItem

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuPeer
 * Method:    createMenu
 * Signature: (Lsun/awt/windows/WMenuBarPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuPeer_createMenu(JNIEnv *env, jobject self,
                                          jobject menuBar)
{
    TRY;

    AwtToolkit::CreateComponent(self, menuBar,
                                (AwtToolkit::ComponentFactory)AwtMenu::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuPeer
 * Method:    createSubMenu
 * Signature: (Lsun/awt/windows/WMenuPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuPeer_createSubMenu(JNIEnv *env, jobject self,
                                             jobject menu)
{
    TRY;

    AwtToolkit::CreateComponent(self, menu,
                                (AwtToolkit::ComponentFactory)AwtMenu::Create);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
