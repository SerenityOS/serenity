/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_MenuBar.h"
#include "awt_Frame.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _DelItem() method
struct DelItemStruct {
    jobject menuitem;
    jint index;
};
/***********************************************************************/
// struct for _AddMenu() method
struct AddMenuStruct {
    jobject menubar;
    jobject menu;
};
/************************************************************************
 * AwtMenuBar fields
 */

jmethodID AwtMenuBar::getMenuMID;
jmethodID AwtMenuBar::getMenuCountMID;


/************************************************************************
 * AwtMenuBar methods
 */


AwtMenuBar::AwtMenuBar() {
    m_frame = NULL;
}

AwtMenuBar::~AwtMenuBar()
{
}

void AwtMenuBar::Dispose()
{
    if (m_frame != NULL && m_frame->GetMenuBar() == this) {
        m_frame->SetMenuBar(NULL);
    }
    m_frame = NULL;

    AwtMenu::Dispose();
}

LPCTSTR AwtMenuBar::GetClassName() {
  return TEXT("SunAwtMenuBar");
}

/* Create a new AwtMenuBar object and menu.   */
AwtMenuBar* AwtMenuBar::Create(jobject self, jobject framePeer)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtMenuBar* menuBar = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        /* target is a java.awt.MenuBar */
        target = env->GetObjectField(self, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        menuBar = new AwtMenuBar();

        SetLastError(0);
        HMENU hMenu = ::CreateMenu();
        // fix for 5088782
        if (!CheckMenuCreation(env, self, hMenu))
        {
            env->DeleteLocalRef(target);
            return NULL;
        }

        menuBar->SetHMenu(hMenu);

        menuBar->LinkObjects(env, self);
        if (framePeer != NULL) {
            PDATA pData;
            JNI_CHECK_PEER_GOTO(framePeer, done);
            menuBar->m_frame = (AwtFrame *)pData;
        } else {
            menuBar->m_frame = NULL;
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    if (target != NULL) {
        env->DeleteLocalRef(target);
    }

    return menuBar;
}

HWND AwtMenuBar::GetOwnerHWnd()
{
    AwtFrame *myFrame = m_frame;
    if (myFrame == NULL)
        return NULL;
    else
        return myFrame->GetHWnd();
}

int AwtMenuBar::CountItem(jobject menuBar)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jint nCount = env->CallIntMethod(menuBar, AwtMenuBar::getMenuCountMID);
    DASSERT(!safe_ExceptionOccurred(env));

    return nCount;
}

AwtMenuItem* AwtMenuBar::GetItem(jobject target, long index)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0) {
        return NULL;
    }

    jobject menu = env->CallObjectMethod(target, AwtMenuBar::getMenuMID,index);
    if (!menu) return NULL; // menu item was removed concurrently
    DASSERT(!safe_ExceptionOccurred(env));

    jobject menuItemPeer = GetPeerForTarget(env, menu);
    PDATA pData;
    AwtMenuItem* awtMenuItem = NULL;
    JNI_CHECK_PEER_GOTO(menuItemPeer, done);
    awtMenuItem = (AwtMenuItem*)pData;

done:
    env->DeleteLocalRef(menu);
    env->DeleteLocalRef(menuItemPeer);

    return awtMenuItem;
}

void AwtMenuBar::DrawItem(DRAWITEMSTRUCT& drawInfo)
{
    DASSERT(drawInfo.CtlType == ODT_MENU);
    AwtMenu::DrawItems(drawInfo);
}

void AwtMenuBar::MeasureItem(HDC hDC,
                             MEASUREITEMSTRUCT& measureInfo)
{
    DASSERT(measureInfo.CtlType == ODT_MENU);
    AwtMenu::MeasureItem(hDC, measureInfo);
}

void AwtMenuBar::AddItem(AwtMenuItem* item)
{
    AwtMenu::AddItem(item);
    HWND hOwnerWnd = GetOwnerHWnd();
    if (hOwnerWnd != NULL) {
        VERIFY(::InvalidateRect(hOwnerWnd,0,TRUE));
    }
}

void AwtMenuBar::DeleteItem(UINT index)
{
    AwtMenu::DeleteItem(index);
    HWND hOwnerWnd = GetOwnerHWnd();
    if (hOwnerWnd != NULL) {
        VERIFY(::InvalidateRect(hOwnerWnd,0,TRUE));
    }
    RedrawMenuBar();
}

/**
 * If the menu changes after the system has created the window,
 * this function must be called to draw the changed menu bar.
 */
void AwtMenuBar::RedrawMenuBar() {
    HWND hOwnerWnd = GetOwnerHWnd();
    if (hOwnerWnd != NULL) {
        VERIFY(::DrawMenuBar(hOwnerWnd));
    }
}

void AwtMenuBar::_AddMenu(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    AddMenuStruct *ams = (AddMenuStruct *)param;
    jobject self = ams->menubar;
    jobject menu = ams->menu;

    AwtMenuBar *m = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    JNI_CHECK_NULL_GOTO(menu, "null menu", ret);
    m = (AwtMenuBar *)pData;
    if (::IsWindow(m->GetOwnerHWnd()))
    {
        /* The menu was already created and added during peer creation -- redraw */
        m->RedrawMenuBar();
    }
ret:
    env->DeleteGlobalRef(self);
    if (menu != NULL) {
        env->DeleteGlobalRef(menu);
    }

    delete ams;
}

void AwtMenuBar::_DelItem(void *param)
{
    if (AwtToolkit::IsMainThread()) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        DelItemStruct *dis = (DelItemStruct*) param;
        jobject self = dis->menuitem;
        jint index = dis->index;

        AwtMenuBar *m = NULL;
        PDATA pData;
        JNI_CHECK_PEER_GOTO(self, ret);
        m = (AwtMenuBar *)pData;
        m->DeleteItem(static_cast<UINT>(index));
ret:
        env->DeleteGlobalRef(self);
        delete dis;
    } else {
        AwtToolkit::GetInstance().InvokeFunction(AwtMenuBar::_DelItem, param);
    }
}

/************************************************************************
 * MenuBar native methods
 */

extern "C" {

/*
 * Class:     java_awt_MenuBar
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_MenuBar_initIDs(JNIEnv *env, jclass cls)
{
    TRY;

    AwtMenuBar::getMenuCountMID = env->GetMethodID(cls, "getMenuCountImpl", "()I");
    DASSERT(AwtMenuBar::getMenuCountMID != NULL);
    CHECK_NULL(AwtMenuBar::getMenuCountMID);

    AwtMenuBar::getMenuMID = env->GetMethodID(cls, "getMenuImpl",
                                              "(I)Ljava/awt/Menu;");
    DASSERT(AwtMenuBar::getMenuMID != NULL);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */


/************************************************************************
 * WMenuBarPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WMenuBarPeer
 * Method:    addMenu
 * Signature: (Ljava/awt/Menu;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuBarPeer_addMenu(JNIEnv *env, jobject self,
                                          jobject menu)
{
    TRY;

    AddMenuStruct *ams = new AddMenuStruct;
    ams->menubar = env->NewGlobalRef(self);
    ams->menu = env->NewGlobalRef(menu);

    AwtToolkit::GetInstance().SyncCall(AwtMenuBar::_AddMenu, ams);
    // global refs and ams are deleted in _AddMenu()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuBarPeer
 * Method:    delMenu
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuBarPeer_delMenu(JNIEnv *env, jobject self,
                                          jint index)
{
    TRY;

    DelItemStruct *dis = new DelItemStruct;
    dis->menuitem = env->NewGlobalRef(self);
    dis->index = index;

    AwtToolkit::GetInstance().SyncCall(AwtMenuBar::_DelItem, dis);
    // global refs and dis are deleted in _DelItem

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WMenuBarPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WFramePeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WMenuBarPeer_create(JNIEnv *env, jobject self,
                                         jobject frame)
{
    TRY;

    AwtToolkit::CreateComponent(self, frame,
                                (AwtToolkit::ComponentFactory)
                                AwtMenuBar::Create);
    CATCH_BAD_ALLOC;
}

} /* extern "C" */
