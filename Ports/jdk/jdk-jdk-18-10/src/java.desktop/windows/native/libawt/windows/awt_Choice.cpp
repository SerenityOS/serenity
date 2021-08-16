/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <windowsx.h>

#include "awt_Toolkit.h"
#include "awt_Choice.h"
#include "awt_Canvas.h"

#include "awt_Dimension.h"
#include "awt_Container.h"

#include "ComCtl32Util.h"

#include <java_awt_Toolkit.h>
#include <java_awt_FontMetrics.h>
#include <java_awt_event_InputEvent.h>

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/************************************************************************/
// Struct for _Reshape() method
struct ReshapeStruct {
    jobject choice;
    jint x, y;
    jint width, height;
};
// Struct for _Select() method
struct SelectStruct {
    jobject choice;
    jint index;
};
// Struct for _AddItems() method
struct AddItemsStruct {
    jobject choice;
    jobjectArray items;
    jint index;
};
// Struct for _Remove() method
struct RemoveStruct {
    jobject choice;
    jint index;
};

/************************************************************************/

/* Bug #4509045: set if SetDragCapture captured mouse */

BOOL AwtChoice::mouseCapture = FALSE;

/* Bug #4338368: consume the spurious MouseUp when the choice loses focus */

BOOL AwtChoice::skipNextMouseUp = FALSE;

BOOL AwtChoice::sm_isMouseMoveInList = FALSE;

static const UINT MINIMUM_NUMBER_OF_VISIBLE_ITEMS = 8;

namespace {
    jfieldID selectedIndexID;
}

/*************************************************************************
 * AwtChoice class methods
 */

AwtChoice::AwtChoice() {
    m_hList = NULL;
    m_listDefWindowProc = NULL;
}

LPCTSTR AwtChoice::GetClassName() {
    return TEXT("COMBOBOX");  /* System provided combobox class */
}

void AwtChoice::Dispose() {
    if (m_hList != NULL && m_listDefWindowProc != NULL) {
        ComCtl32Util::GetInstance().UnsubclassHWND(m_hList, ListWindowProc, m_listDefWindowProc);
    }
    AwtComponent::Dispose();
}

AwtChoice* AwtChoice::Create(jobject peer, jobject parent) {
    DASSERT(AwtToolkit::IsMainThread());
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtChoice* c = NULL;
    RECT rc;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }
        PDATA pData;
        AwtCanvas* awtParent;
        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        target = env->GetObjectField(peer, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        c = new AwtChoice();

        {
            DWORD style = WS_CHILD | WS_CLIPSIBLINGS | WS_VSCROLL |
                          CBS_DROPDOWNLIST | CBS_OWNERDRAWFIXED;
            DWORD exStyle = 0;
            if (GetRTL()) {
                exStyle |= WS_EX_RIGHT | WS_EX_LEFTSCROLLBAR;
                if (GetRTLReadingOrder())
                    exStyle |= WS_EX_RTLREADING;
            }

            /*
             * In OWNER_DRAW, the size of the edit control part of the
             * choice must be determinded in its creation, when the parent
             * cannot get the choice's instance from its handle.  So
             * record the pair of the ID and the instance of the choice.
             */
            UINT myId = awtParent->CreateControlID();
            DASSERT(myId > 0);
            c->m_myControlID = myId;
            awtParent->PushChild(myId, c);

            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);

            jobject dimension = JNU_CallMethodByName(env, NULL, peer,
                                                     "getPreferredSize",
                                                     "()Ljava/awt/Dimension;").l;
            DASSERT(!safe_ExceptionOccurred(env));
            if (env->ExceptionCheck()) goto done;

            if (dimension != NULL && width == 0) {
                width = env->GetIntField(dimension, AwtDimension::widthID);
            }
            c->CreateHWnd(env, L"", style, exStyle,
                          x, y, width, height,
                          awtParent->GetHWnd(),
                          reinterpret_cast<HMENU>(static_cast<INT_PTR>(myId)),
                          ::GetSysColor(COLOR_WINDOWTEXT),
                          ::GetSysColor(COLOR_WINDOW),
                          peer);

            /* suppress inheriting parent's color. */
            c->m_backgroundColorSet = TRUE;
            c->UpdateBackground(env, target);

            /* Bug 4255631 Solaris: Size returned by Choice.getSize() does not match
             * actual size
             * Fix: Set the Choice to its actual size in the component.
             */
            ::GetClientRect(c->GetHWnd(), &rc);
            env->SetIntField(target, AwtComponent::widthID, c->ScaleDownX(rc.right));
            env->SetIntField(target, AwtComponent::heightID, c->ScaleDownY(rc.bottom));

            if (IS_WINXP) {
                ::SendMessage(c->GetHWnd(), CB_SETMINVISIBLE, (WPARAM) MINIMUM_NUMBER_OF_VISIBLE_ITEMS, 0);
            }

            env->DeleteLocalRef(dimension);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);

    return c;
}

// calculate height of drop-down list part of the combobox
// to show all the items up to a maximum of eight
int AwtChoice::GetDropDownHeight()
{
    int itemHeight =(int)::SendMessage(GetHWnd(), CB_GETITEMHEIGHT, (UINT)0,0);
    int numItemsToShow = (int)::SendMessage(GetHWnd(), CB_GETCOUNT, 0,0);
    numItemsToShow = min(MINIMUM_NUMBER_OF_VISIBLE_ITEMS, numItemsToShow);

    // drop-down height snaps to nearest line, so add a
    // fudge factor of 1/2 line to ensure last line shows
    return ScaleDownY(itemHeight * numItemsToShow + itemHeight / 2);
}

// get the height of the field portion of the combobox
int AwtChoice::GetFieldHeight()
{
    int fieldHeight;
    int borderHeight;
    fieldHeight =(int)::SendMessage(GetHWnd(), CB_GETITEMHEIGHT, (UINT)-1, 0);
    // add top and bottom border lines; border size is different for
    // Win 4.x (3d edge) vs 3.x (1 pixel line)
    borderHeight = ::GetSystemMetrics(SM_CYEDGE);
    fieldHeight += borderHeight*2;
    return ScaleDownY(fieldHeight);
}

// gets the total height of the combobox, including drop down
int AwtChoice::GetTotalHeight()
{
    int dropHeight = GetDropDownHeight();
    int fieldHeight = GetFieldHeight();

    // border on drop-down portion is always non-3d (so don't use SM_CYEDGE)
    int borderHeight = ScaleDownY(::GetSystemMetrics(SM_CYBORDER));
    // total height = drop down height + field height + top+bottom drop down border lines
    return dropHeight + fieldHeight + borderHeight * 2;
}

// Recalculate and set the drop-down height for the Choice.
void AwtChoice::ResetDropDownHeight()
{
    RECT    rcWindow;

    ::GetWindowRect(GetHWnd(), &rcWindow);
    // resize the drop down to accommodate added/removed items
    int totalHeight = ScaleUpY(GetTotalHeight());
    ::SetWindowPos(GetHWnd(), NULL,
                    0, 0, rcWindow.right - rcWindow.left, totalHeight,
                    SWP_NOACTIVATE|SWP_NOMOVE|SWP_NOZORDER);
}

/* Fix for the bug 4327666: set the capture for middle
   and right mouse buttons, but leave left button alone */
void AwtChoice::SetDragCapture(UINT flags)
{
    if ((flags & MK_LBUTTON) != 0) {
        if ((::GetCapture() == GetHWnd()) && mouseCapture) {
            /* On MK_LBUTTON ComboBox captures mouse itself
               so we should release capture and clear flag to
               prevent releasing capture by ReleaseDragCapture
             */
            ::ReleaseCapture();
            mouseCapture = FALSE;
        }
        return;
    }

    // don't want to interfere with other controls
    if (::GetCapture() == NULL) {
        ::SetCapture(GetHWnd());
        mouseCapture = TRUE;
    }
}

/* Fix for Bug 4509045: should release capture only if it is set by SetDragCapture */
void AwtChoice::ReleaseDragCapture(UINT flags)
{
    if ((::GetCapture() == GetHWnd()) && ((flags & ALL_MK_BUTTONS) == 0) && mouseCapture) {
        ::ReleaseCapture();
        mouseCapture = FALSE;
    }
}

void AwtChoice::Reshape(int x, int y, int w, int h)
{
    // Choice component height is fixed (when rolled up)
    // so vertically center the choice in it's bounding box
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject target = GetTarget(env);
    jobject parent = env->GetObjectField(target, AwtComponent::parentID);
    RECT rc;

    int fieldHeight = GetFieldHeight();
    if ((parent != NULL && env->GetObjectField(parent, AwtContainer::layoutMgrID) != NULL) &&
        fieldHeight > 0 && fieldHeight < h) {
        y += (h - fieldHeight) / 2;
    }

    /* Fix for 4783342
     * Choice should ignore reshape on height changes,
     * as height is dependent on Font size only.
     */
    AwtComponent* awtParent = GetParent();
    BOOL bReshape = true;
    if (awtParent != NULL) {
        ::GetWindowRect(GetHWnd(), &rc);
        int oldW = ScaleDownX(rc.right - rc.left);
        RECT parentRc;
        ::GetWindowRect(awtParent->GetHWnd(), &parentRc);
        int oldX = ScaleDownX(rc.left - parentRc.left);
        int oldY = ScaleDownY(rc.top - parentRc.top);
        bReshape = (x != oldX || y != oldY || w != oldW);
    }

    if (bReshape)
    {
        int totalHeight = GetTotalHeight();
        AwtComponent::Reshape(x, y, w, totalHeight);
    }

    /* Bug 4255631 Solaris: Size returned by Choice.getSize() does not match
     * actual size
     * Fix: Set the Choice to its actual size in the component.
     */
    ::GetClientRect(GetHWnd(), &rc);
    env->SetIntField(target, AwtComponent::widthID, ScaleDownX(rc.right));
    env->SetIntField(target, AwtComponent::heightID, ScaleDownY(rc.bottom));

    env->DeleteLocalRef(target);
    env->DeleteLocalRef(parent);
}

jobject AwtChoice::PreferredItemSize(JNIEnv *env)
{
    jobject dimension = JNU_CallMethodByName(env, NULL, GetPeer(env),
                                             "getPreferredSize",
                                             "()Ljava/awt/Dimension;").l;
    DASSERT(!safe_ExceptionOccurred(env));
    CHECK_NULL_RETURN(dimension, NULL);

    /* This size is window size of choice and it's too big for each
     * drop down item height.
     */
    env->SetIntField(dimension, AwtDimension::heightID,
                       ScaleUpY(GetFontHeight(env)));
    return dimension;
}

void AwtChoice::SetFont(AwtFont* font)
{
    AwtComponent::SetFont(font);

    //Get the text metrics and change the height of each item.
    HDC hDC = ::GetDC(GetHWnd());
    DASSERT(hDC != NULL);
    TEXTMETRIC tm;

    HANDLE hFont = font->GetHFont();
    VERIFY(::SelectObject(hDC, hFont) != NULL);
    VERIFY(::GetTextMetrics(hDC, &tm));
    long h = tm.tmHeight + tm.tmExternalLeading;
    VERIFY(::ReleaseDC(GetHWnd(), hDC) != 0);

    int nCount = (int)::SendMessage(GetHWnd(), CB_GETCOUNT, 0, 0);
    for(int i = 0; i < nCount; ++i) {
        VERIFY(::SendMessage(GetHWnd(), CB_SETITEMHEIGHT, i, MAKELPARAM(h, 0)) != CB_ERR);
    }
    //Change the height of the Edit Box.
    VERIFY(::SendMessage(GetHWnd(), CB_SETITEMHEIGHT, (UINT)-1,
                         MAKELPARAM(h, 0)) != CB_ERR);

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    jobject target = GetTarget(env);
    jint height = env->GetIntField(target, AwtComponent::heightID);

    Reshape(env->GetIntField(target, AwtComponent::xID),
            env->GetIntField(target, AwtComponent::yID),
            env->GetIntField(target, AwtComponent::widthID),
            h);

    env->DeleteLocalRef(target);
}

static int lastClickX = -1;
static int lastClickY = -1;

LRESULT CALLBACK AwtChoice::ListWindowProc(HWND hwnd, UINT message,
                                           WPARAM wParam, LPARAM lParam)
{
    /*
     * We don't pass the choice WM_LBUTTONDOWN message. As the result the choice's list
     * doesn't forward mouse messages it captures. Below we do forward what we need.
     */

    TRY;

    DASSERT(::IsWindow(hwnd));

    switch (message) {
        case WM_LBUTTONDOWN: {
            DWORD curPos = ::GetMessagePos();
            lastClickX = GET_X_LPARAM(curPos);
            lastClickY = GET_Y_LPARAM(curPos);
            break;
        }
        case WM_MOUSEMOVE: {
            RECT rect;
            ::GetClientRect(hwnd, &rect);

            POINT pt = {GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam)};
            if (::PtInRect(&rect, pt)) {
                sm_isMouseMoveInList = TRUE;
            }

            POINT lastPt = {lastClickX, lastClickY};
            ::ScreenToClient(hwnd, &lastPt);
            if (::PtInRect(&rect, lastPt)) {
                break; // ignore when dragging inside the list
            }
        }
        case WM_LBUTTONUP: {
            lastClickX = -1;
            lastClickY = -1;

            AwtChoice *c = (AwtChoice *)::GetWindowLongPtr(hwnd, GWLP_USERDATA);
            if (c != NULL) {
                // forward the msg to the choice
                c->WindowProc(message, wParam, lParam);
            }
        }
    }
    return ComCtl32Util::GetInstance().DefWindowProc(NULL, hwnd, message, wParam, lParam);

    CATCH_BAD_ALLOC_RET(0);
}


MsgRouting AwtChoice::WmNotify(UINT notifyCode)
{
    if (notifyCode == CBN_SELCHANGE) {
        int selectedIndex = (int)SendMessage(CB_GETCURSEL);

        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        jobject target = GetTarget(env);
        int previousIndex = env->GetIntField(target, selectedIndexID);

        if (selectedIndex != CB_ERR && selectedIndex != previousIndex){
            DoCallback("handleAction", "(I)V", selectedIndex);
        }
    } else if (notifyCode == CBN_DROPDOWN) {

        if (m_hList == NULL) {
            COMBOBOXINFO cbi;
            cbi.cbSize = sizeof(COMBOBOXINFO);
            ::GetComboBoxInfo(GetHWnd(), &cbi);
            m_hList = cbi.hwndList;
            m_listDefWindowProc = ComCtl32Util::GetInstance().SubclassHWND(m_hList, ListWindowProc);
            DASSERT(::GetWindowLongPtr(m_hList, GWLP_USERDATA) == NULL);
            ::SetWindowLongPtr(m_hList, GWLP_USERDATA, (LONG_PTR)this);
        }
        sm_isMouseMoveInList = FALSE;

        // Clicking in the dropdown list steals focus from the proxy.
        // So, set the focus-restore flag up.
        SetRestoreFocus(TRUE);
    } else if (notifyCode == CBN_CLOSEUP) {
        SetRestoreFocus(FALSE);
    }
    return mrDoDefault;
}

MsgRouting
AwtChoice::OwnerDrawItem(UINT /*ctrlId*/, DRAWITEMSTRUCT& drawInfo)
{
    DrawListItem((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), drawInfo);
    return mrConsume;
}

MsgRouting
AwtChoice::OwnerMeasureItem(UINT /*ctrlId*/, MEASUREITEMSTRUCT& measureInfo)
{
    MeasureListItem((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), measureInfo);
    return mrConsume;
}

/* Bug #4338368: when a choice loses focus, it triggers spurious MouseUp event,
 * even if the focus was lost due to TAB key pressing
 */

MsgRouting
AwtChoice::WmKillFocus(HWND hWndGotFocus)
{
    skipNextMouseUp = TRUE;
    return AwtComponent::WmKillFocus(hWndGotFocus);
}

MsgRouting
AwtChoice::WmMouseUp(UINT flags, int x, int y, int button)
{
    if (skipNextMouseUp) {
        skipNextMouseUp = FALSE;
        return mrDoDefault;
    }
    return AwtComponent::WmMouseUp(flags, x, y, button);
}

MsgRouting AwtChoice::HandleEvent(MSG *msg, BOOL synthetic)
{
    if (IsFocusingMouseMessage(msg)) {
        SendMessage(CB_SHOWDROPDOWN, ~SendMessage(CB_GETDROPPEDSTATE, 0, 0), 0);
        delete msg;
        return mrConsume;
    }
    // To simulate the native behavior, we close the list on WM_LBUTTONUP if
    // WM_MOUSEMOVE has been dedected on the list since it has been dropped down.
    if (msg->message == WM_LBUTTONUP && SendMessage(CB_GETDROPPEDSTATE, 0, 0) &&
        sm_isMouseMoveInList)
    {
        SendMessage(CB_SHOWDROPDOWN, FALSE, 0);
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

BOOL AwtChoice::InheritsNativeMouseWheelBehavior() {return true;}

void AwtChoice::_Reshape(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    ReshapeStruct *rs = (ReshapeStruct *)param;
    jobject choice = rs->choice;
    jint x = rs->x;
    jint y = rs->y;
    jint width = rs->width;
    jint height = rs->height;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->Reshape(x, y, width, height);
        c->VerifyState();
    }

done:
    env->DeleteGlobalRef(choice);

    delete rs;
}

void AwtChoice::_Select(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SelectStruct *ss = (SelectStruct *)param;
    jobject choice = ss->choice;
    jint index = ss->index;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SendMessage(CB_SETCURSEL, index);
//        c->VerifyState();
    }

done:
    env->DeleteGlobalRef(choice);

    delete ss;
}

void AwtChoice::_AddItems(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    AddItemsStruct *ais = (AddItemsStruct *)param;
    jobject choice = ais->choice;
    jobjectArray items = ais->items;
    jint index = ais->index;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);
    JNI_CHECK_NULL_GOTO(items, "null items", done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        jsize i;
        int itemCount = env->GetArrayLength(items);
        if (itemCount > 0) {
           c->SendMessage(WM_SETREDRAW, (WPARAM)FALSE, 0);
           for (i = 0; i < itemCount; i++)
           {
               jstring item = (jstring)env->GetObjectArrayElement(items, i);
               if (env->ExceptionCheck()) goto done;
               if (item == NULL) goto next_elem;
               c->SendMessage(CB_INSERTSTRING, index + i, JavaStringBuffer(env, item));
               env->DeleteLocalRef(item);
next_elem:
               ;
           }
           c->SendMessage(WM_SETREDRAW, (WPARAM)TRUE, 0);
           InvalidateRect(c->GetHWnd(), NULL, TRUE);
           c->ResetDropDownHeight();
           c->VerifyState();
        }
    }

done:
    env->DeleteGlobalRef(choice);
    env->DeleteGlobalRef(items);

    delete ais;
}

void AwtChoice::_Remove(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    RemoveStruct *rs = (RemoveStruct *)param;
    jobject choice = rs->choice;
    jint index = rs->index;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SendMessage(CB_DELETESTRING, index, 0);
        c->ResetDropDownHeight();
        c->VerifyState();
    }

done:
    env->DeleteGlobalRef(choice);

    delete rs;
}

void AwtChoice::_RemoveAll(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject choice = (jobject)param;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SendMessage(CB_RESETCONTENT, 0, 0);
        c->ResetDropDownHeight();
        c->VerifyState();
    }

done:
    env->DeleteGlobalRef(choice);
}

void AwtChoice::_CloseList(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject choice = (jobject)param;

    AwtChoice *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(choice, done);

    c = (AwtChoice *)pData;
    if (::IsWindow(c->GetHWnd()) && c->SendMessage(CB_GETDROPPEDSTATE, 0, 0)) {
        c->SendMessage(CB_SHOWDROPDOWN, FALSE, 0);
    }

done:
    env->DeleteGlobalRef(choice);
}

/************************************************************************
 * WChoicePeer native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Choice_initIDs(JNIEnv *env, jclass cls)
{
    TRY;
    selectedIndexID = env->GetFieldID(cls, "selectedIndex", "I");
    DASSERT(selectedIndexID);
    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    select
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_select(JNIEnv *env, jobject self,
                                        jint index)
{
    TRY;

    SelectStruct *ss = new SelectStruct;
    ss->choice = env->NewGlobalRef(self);
    ss->index = index;

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_Select, ss);
    // global refs and ss are removed in _Select

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    remove
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_remove(JNIEnv *env, jobject self,
                                        jint index)
{
    TRY;

    RemoveStruct *rs = new RemoveStruct;
    rs->choice = env->NewGlobalRef(self);
    rs->index = index;

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_Remove, rs);
    // global ref and rs are deleted in _Remove

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    removeAll
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_removeAll(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_RemoveAll, (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _RemoveAll

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    addItems
 * Signature: ([Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_addItems(JNIEnv *env, jobject self,
                                          jobjectArray items, jint index)
{
    TRY;

    AddItemsStruct *ais = new AddItemsStruct;
    ais->choice = env->NewGlobalRef(self);
    ais->items = (jobjectArray)env->NewGlobalRef(items);
    ais->index = index;

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_AddItems, ais);
    // global refs and ais are deleted in _AddItems

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    reshape
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_reshape(JNIEnv *env, jobject self,
                                         jint x, jint y,
                                         jint width, jint height)
{
    TRY;

    ReshapeStruct *rs = new ReshapeStruct;
    rs->choice = env->NewGlobalRef(self);
    rs->x = x;
    rs->y = y;
    rs->width = width;
    rs->height = height;

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_Reshape, rs);
    // global ref and rs are deleted in _Reshape

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_create(JNIEnv *env, jobject self,
                                        jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtChoice::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WChoicePeer
 * Method:    closeList
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WChoicePeer_closeList(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtChoice::_CloseList, (void *)selfGlobalRef);
    // global ref is deleted in _CloseList

    CATCH_BAD_ALLOC;
}
} /* extern "C" */


/************************************************************************
 * Diagnostic routines
 */

#ifdef DEBUG

void AwtChoice::VerifyState()
{
    if (AwtToolkit::GetInstance().VerifyComponents() == FALSE) {
        return;
    }

    if (m_callbacksEnabled == FALSE) {
        /* Component is not fully setup yet. */
        return;
    }

    AwtComponent::VerifyState();
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->PushLocalFrame(1) < 0)
        return;

    jobject target = GetTarget(env);

    // To avoid possibly running client code on the toolkit thread, don't
    // do the following checks if we're running on the toolkit thread.
    if (AwtToolkit::MainThread() != ::GetCurrentThreadId()) {
        // Compare number of items.
        int nTargetItems = JNU_CallMethodByName(env, NULL, target,
                                                "countItems", "()I").i;
        DASSERT(!safe_ExceptionOccurred(env));
        int nPeerItems = (int)::SendMessage(GetHWnd(), CB_GETCOUNT, 0, 0);
        DASSERT(nTargetItems == nPeerItems);

        // Compare selection
        int targetIndex = JNU_CallMethodByName(env, NULL, target,
                                               "getSelectedIndex", "()I").i;
        DASSERT(!safe_ExceptionOccurred(env));
        int peerCurSel = (int)::SendMessage(GetHWnd(), CB_GETCURSEL, 0, 0);
        DASSERT(targetIndex == peerCurSel);
    }
    env->PopLocalFrame(0);
}
#endif //DEBUG
