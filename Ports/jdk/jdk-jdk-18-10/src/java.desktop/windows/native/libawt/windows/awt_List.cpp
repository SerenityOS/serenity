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

#include "awt_List.h"
#include "awt_Canvas.h"
#include "awt_Dimension.h"
#include "awt_Toolkit.h"
#include "awt_Window.h"

#include "ComCtl32Util.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _AddItems() method
struct AddItemsStruct {
    jobject list;
    jobjectArray items;
    jint index;
    jint width;
};
// struct for _DelItems() method
struct DelItemsStruct {
    jobject list;
    jint start, end;
};
// struct for _IsSelected(), _Select(), _Deselect() and _MakeVisible() methods
struct SelectElementStruct {
    jobject list;
    jint index;
};
// struct for _SetMultipleSelections() method
struct SetMultipleSelectionsStruct {
    jobject list;
    jboolean on;
};
/************************************************************************
 * AwtList methods
 */

AwtList::AwtList() {
    isMultiSelect = FALSE;
    isWrapperPrint = FALSE;
}

AwtList::~AwtList()
{
}

LPCTSTR AwtList::GetClassName() {
    return TEXT("LISTBOX");
}

/* Create a new AwtList object and window.   */
AwtList* AwtList::Create(jobject peer, jobject parent)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject target = NULL;
    AwtList* c = NULL;

    try {
        if (env->EnsureLocalCapacity(1) < 0) {
            return NULL;
        }

        PDATA pData;
        AwtCanvas* awtParent;

        JNI_CHECK_PEER_GOTO(parent, done);
        awtParent = (AwtCanvas*)pData;

        /* target is Hjava_awt_List * */
        target = env->GetObjectField(peer, AwtObject::targetID);
        JNI_CHECK_NULL_GOTO(target, "null target", done);

        c = new AwtList();

        {

            /*
             * NOTE: WS_CLIPCHILDREN is excluded so that repaint requests
             * from Java will pass through the wrap to the native listbox.
             */
            DWORD wrapStyle = WS_CHILD | WS_CLIPSIBLINGS;
            DWORD wrapExStyle = 0;

            DWORD style = WS_CHILD | WS_CLIPSIBLINGS | WS_VSCROLL | WS_HSCROLL |
                LBS_NOINTEGRALHEIGHT | LBS_NOTIFY | LBS_OWNERDRAWFIXED;
            DWORD exStyle = WS_EX_CLIENTEDGE;

            /*
             * NOTE: WS_VISIBLE is always set for the listbox. Listbox
             * visibility is controlled by toggling the wrap's WS_VISIBLE bit.
             */
            style |= WS_VISIBLE;

            if (GetRTL()) {
                exStyle |= WS_EX_RIGHT | WS_EX_LEFTSCROLLBAR;
                if (GetRTLReadingOrder())
                    exStyle |= WS_EX_RTLREADING;
            }

            jint x = env->GetIntField(target, AwtComponent::xID);
            jint y = env->GetIntField(target, AwtComponent::yID);
            jint width = env->GetIntField(target, AwtComponent::widthID);
            jint height = env->GetIntField(target, AwtComponent::heightID);

            c->CreateHWnd(env, L"", style, exStyle,
                          x, y, width, height,
                          awtParent->GetHWnd(),
                          NULL,
                          ::GetSysColor(COLOR_WINDOWTEXT),
                          ::GetSysColor(COLOR_WINDOW),
                          peer
                          );

            /* suppress inheriting awtParent's color. */
            c->m_backgroundColorSet = TRUE;
            c->UpdateBackground(env, target);
        }
    } catch (...) {
        env->DeleteLocalRef(target);
        throw;
    }

done:
    env->DeleteLocalRef(target);
    return c;
}

void AwtList::SetDragCapture(UINT flags)
{
    // don't want to interfere with other controls
    if (::GetCapture() == NULL) {
        ::SetCapture(GetListHandle());
    }
}

void AwtList::ReleaseDragCapture(UINT flags)
{
    if ((::GetCapture() == GetListHandle()) && ((flags & ALL_MK_BUTTONS) == 0)) {
        ::ReleaseCapture();
    }
}

void AwtList::Reshape(int x, int y, int w, int h)
{
    AwtComponent::Reshape(x, y, w, h);

/*
    HWND hList = GetListHandle();
    if (hList != NULL) {
        long flags = SWP_NOACTIVATE | SWP_NOZORDER | SWP_NOCOPYBITS;
        /*
         * Fix for bug 4046446.
         * /
        SetWindowPos(hList, 0, 0, 0, w, h, flags);
    }
*/
}

//Netscape : Override the AwtComponent method so we can set the item height
//for each item in the list.  Modified by echawkes to avoid race condition.

void AwtList::SetFont(AwtFont* font)
{
    DASSERT(font != NULL);
    if (font->GetAscent() < 0)
    {
        AwtFont::SetupAscent(font);
    }
    HANDLE hFont = font->GetHFont();
    SendListMessage(WM_SETFONT, (WPARAM)hFont, MAKELPARAM(FALSE, 0));

    HDC hDC = ::GetDC(GetHWnd());

    TEXTMETRIC tm;
    VERIFY(::SelectObject(hDC, hFont) != NULL);
    VERIFY(::GetTextMetrics(hDC, &tm));

    ::ReleaseDC(GetHWnd(), hDC);

    long h = tm.tmHeight + tm.tmExternalLeading;
    // Listbox is LBS_OWNERDRAWFIXED so the items have the same height
    VERIFY(SendListMessage(LB_SETITEMHEIGHT, 0, MAKELPARAM(h, 0)) != LB_ERR);
    VERIFY(::RedrawWindow(GetHWnd(), NULL, NULL, RDW_INVALIDATE |RDW_FRAME |RDW_ERASE));
}

void AwtList::SetMultiSelect(BOOL ms) {
    if (ms == isMultiSelect) {
        return;
    }

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    /* Copy current box's contents to string array */
    const int nCount = GetCount();
    LPTSTR * strings = new LPTSTR[nCount];
    int i;

    for(i = 0; i < nCount; i++) {
        LRESULT len = SendListMessage(LB_GETTEXTLEN, i);
        LPTSTR text = NULL;
        try {
            text = new TCHAR[len + 1];
        } catch (std::bad_alloc&) {
            // free char * already allocated
            for (int j = 0; j < i; j++) {
                delete [] strings[j];
            }
            delete [] strings;
            throw;
        }

        VERIFY(SendListMessage(LB_GETTEXT, i, (LPARAM)text) != LB_ERR);
        strings[i] = text;
    }

    // index for selected item after multi-select mode change
    int toSelect = SendListMessage(LB_GETCURSEL);
    if (!isMultiSelect)
    {
        // MSDN: for single-select lists LB_GETCURSEL returns
        // index of selected item or LB_ERR if no item is selected
        if (toSelect == LB_ERR)
        {
            toSelect = -1;
        }
    }
    else
    {
        // MSDN: for multi-select lists LB_GETCURSEL returns index
        // of the focused item or 0 if no items are selected; if
        // some item has focus and is not selected then LB_GETCURSEL
        // return its index, so we need IsItemSelected too
        if ((toSelect == LB_ERR) ||
            (SendListMessage(LB_GETSELCOUNT) == 0) ||
            (IsItemSelected(toSelect) == 0))
        {
            toSelect = -1;
        }
    }

    isMultiSelect = ms;

    HWND parentHWnd = GetParent()->GetHWnd();

    /* Save old list box's attributes */
    RECT rect;
    GetWindowRect(GetListHandle(), &rect);
    MapWindowPoints(0, parentHWnd, (LPPOINT)&rect, 2);

    HANDLE font = (HANDLE)SendListMessage(WM_GETFONT);
    LRESULT itemHeight = SendListMessage(LB_GETITEMHEIGHT, 0);
    DWORD style = ::GetWindowLong(GetListHandle(), GWL_STYLE) | WS_VSCROLL | WS_HSCROLL;
    if (isMultiSelect) {
        style |= LBS_MULTIPLESEL;
    } else {
        style &= ~LBS_MULTIPLESEL;
    }
    DWORD exStyle = ::GetWindowLong(GetListHandle(), GWL_EXSTYLE);

    jobject peer = GetPeer(env);

    UnsubclassHWND();
    AwtToolkit::DestroyComponentHWND(m_hwnd);
    CreateHWnd(env, L"", style, exStyle, 0, 0, 0, 0,
               parentHWnd,
               NULL,
               ::GetSysColor(COLOR_WINDOWTEXT),
               ::GetSysColor(COLOR_WINDOW),
               peer);

    SetWindowPos(GetHWnd(), 0,
            rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top,
            SWP_NOZORDER | SWP_NOCOPYBITS | SWP_NOACTIVATE);

    SendListMessage(WM_SETFONT, (WPARAM)font, (LPARAM)FALSE);
    SendListMessage(LB_SETITEMHEIGHT, 0, MAKELPARAM(itemHeight, 0));
    SendListMessage(LB_RESETCONTENT);
    for (i = 0; i < nCount; i++) {
        InsertString(i, strings[i]);
        delete [] strings[i];
    }
    delete[] strings;
    if (toSelect != -1) {
        Select(toSelect);
    }

    AdjustHorizontalScrollbar();
}

/*
 * There currently is no good place to cache java.awt.Dimension field
 * ids. If this method gets called a lot, one such place should be found.
 * -- br 07/18/97.
 */
jobject AwtList::PreferredItemSize(JNIEnv *env)
{
    jobject peer = GetPeer(env);
    jobject dimension = JNU_CallMethodByName(env, NULL, peer, "getPreferredSize",
                                             "(I)Ljava/awt/Dimension;",
                                             1).l;

    DASSERT(!safe_ExceptionOccurred(env));
    if (dimension == NULL) {
        return NULL;
    }
    /* This size is too big for each item height. */
    (env)->SetIntField(dimension, AwtDimension::heightID, GetFontHeight(env));

    return dimension;
}

// Every time something gets added to the list, we increase the max width
// of items that have ever been added.  If it surpasses the width of the
// listbox, we show the scrollbar.  When things get deleted, we shrink
// the scroll region back down and hide the scrollbar, if needed.
void AwtList::AdjustHorizontalScrollbar()
{
    // The border width is added to the horizontal extent to ensure that we
    // can view all of the text when we move the horz. scrollbar to the end.
    int  cxBorders = GetSystemMetrics( SM_CXBORDER ) * 2;
    RECT rect;
    VERIFY(::GetClientRect(GetListHandle(), &rect));
    LRESULT iHorzExt = SendListMessage(LB_GETHORIZONTALEXTENT, 0, 0L ) - cxBorders;
    if ( (m_nMaxWidth > rect.left)  // if strings wider than listbox
      || (iHorzExt != m_nMaxWidth) ) //   or scrollbar not needed anymore.
    {
        SendListMessage(LB_SETHORIZONTALEXTENT, m_nMaxWidth + cxBorders, 0L);
    }
}

// This function goes through all strings in the list to find the width,
// in pixels, of the longest string in the list.
void AwtList::UpdateMaxItemWidth()
{
    m_nMaxWidth = 0;

    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
    if (env->EnsureLocalCapacity(2) < 0)
        return;

    HDC hDC = ::GetDC(GetHWnd());

    jobject self = GetPeer(env);
    DASSERT(self);

    /* target is java.awt.List */
    jobject target = env->GetObjectField(self, AwtObject::targetID);
    jobject font = GET_FONT(target, self);

    int nCount = GetCount();
    for ( int i=0; i < nCount; i++ )
    {
        jstring jstr = GetItemString( env, target, i );
        SIZE size = AwtFont::getMFStringSize( hDC, font, jstr );
        if ( size.cx > m_nMaxWidth )
            m_nMaxWidth = size.cx;
        env->DeleteLocalRef( jstr );
    }

    // free up the shared DC and release local refs
    ::ReleaseDC(GetHWnd(), hDC);
    env->DeleteLocalRef( target );
    env->DeleteLocalRef( font );

    // Now adjust the horizontal scrollbar extent
    AdjustHorizontalScrollbar();
}

MsgRouting
AwtList::WmSize(UINT type, int w, int h)
{
    AdjustHorizontalScrollbar();
    return AwtComponent::WmSize(type, w, h);
}

MsgRouting
AwtList::OwnerDrawItem(UINT /*ctrlId*/, DRAWITEMSTRUCT& drawInfo)
{
    AwtComponent::DrawListItem((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), drawInfo);
    return mrConsume;
}

MsgRouting
AwtList::OwnerMeasureItem(UINT /*ctrlId*/, MEASUREITEMSTRUCT& measureInfo)
{
    AwtComponent::MeasureListItem((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), measureInfo);
    return mrConsume;
}

MsgRouting
AwtList::WmNcHitTest(UINT x, UINT y, LRESULT& retVal)
{
    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd())))) {
        retVal = HTCLIENT;
        return mrConsume;
    }
    return AwtComponent::WmNcHitTest(x, y, retVal);
}

MsgRouting
AwtList::WmMouseUp(UINT flags, int x, int y, int button)
{
    MsgRouting result = mrDoDefault;
    // if this list is in the modal blocked window, this message should be consumed,
    // however AwtComponent::WmMouseUp must be called anyway
    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd())))) {
        result = mrConsume;
    } else {
        if (button == LEFT_BUTTON) {
            WmCommand(0, GetListHandle(), LBN_SELCHANGE);
        }
    }
    MsgRouting compResult = AwtComponent::WmMouseUp(flags, x, y, button);
    return (result == mrConsume) ? result : compResult;
}

MsgRouting
AwtList::WmMouseDown(UINT flags, int x, int y, int button)
{
    MsgRouting mrResult = AwtComponent::WmMouseDown(flags, x, y, button);

    if (::IsWindow(AwtWindow::GetModalBlocker(AwtComponent::GetTopLevelParentForWindow(GetHWnd()))))
    {
        return mrConsume;
    }

    /*
     * As we consume WM_LBUTONDOWN the list won't trigger ActionEvent by double click.
     * We trigger it ourselves. (see also 6240202)
     */
    int clickCount = GetClickCount();
    if (button == LEFT_BUTTON && clickCount >= 2 && clickCount % 2 == 0) {
        WmCommand(0, GetListHandle(), LBN_DBLCLK);
    }
    return mrResult;
}

MsgRouting
AwtList::WmCtlColor(HDC hDC, HWND hCtrl, UINT ctlColor, HBRUSH& retBrush)
{
    DASSERT(ctlColor == CTLCOLOR_LISTBOX);
    DASSERT(hCtrl == GetListHandle());
    ::SetBkColor(hDC, GetBackgroundColor());
    ::SetTextColor(hDC, GetColor());
    retBrush = GetBackgroundBrush();
    return mrConsume;
}

BOOL AwtList::IsFocusingMouseMessage(MSG *pMsg)
{
    return pMsg->message == WM_LBUTTONDOWN || pMsg->message == WM_LBUTTONDBLCLK;
}

MsgRouting AwtList::HandleEvent(MSG *msg, BOOL synthetic)
{
    if (IsFocusingMouseMessage(msg)) {
        LONG count = GetCount();
        if (count > 0) {
            LONG item = static_cast<LONG>(SendListMessage(LB_ITEMFROMPOINT, 0, msg->lParam));
            if (HIWORD(item) == 0) {
                item = LOWORD(item);
                if (item >= 0 && item < count) {
                    if (isMultiSelect) {
                        if (IsItemSelected(item)) {
                            Deselect(item);
                        } else {
                            Select(item);
                        }
                    } else {
                        Select(item);
                    }
                }
            }
        }
        delete msg;
        return mrConsume;
    }
    if (msg->message == WM_KEYDOWN && msg->wParam == VK_RETURN) {
        WmNotify(LBN_DBLCLK);
    }
    return AwtComponent::HandleEvent(msg, synthetic);
}

// Fix for 4665745.
// Override WmPrint to catch when the list control (not wrapper) should
// operate WM_PRINT to be compatible with the "smooth scrolling" feature.
MsgRouting AwtList::WmPrint(HDC hDC, LPARAM flags)
{
    if (!isWrapperPrint &&
        (flags & PRF_CLIENT) &&
        (GetStyleEx() & WS_EX_CLIENTEDGE))
    {
        int nOriginalDC = ::SaveDC(hDC);
        DASSERT(nOriginalDC != 0);
        // Save a copy of the DC for WmPrintClient
        VERIFY(::SaveDC(hDC));
        DefWindowProc(WM_PRINT, (WPARAM) hDC,
            (flags & (PRF_CLIENT | PRF_CHECKVISIBLE | PRF_ERASEBKGND)));
        VERIFY(::RestoreDC(hDC, nOriginalDC));

        flags &= ~PRF_CLIENT;
    }

    return AwtComponent::WmPrint(hDC, flags);
}

MsgRouting
AwtList::WmNotify(UINT notifyCode)
{
    if (notifyCode == LBN_SELCHANGE || notifyCode == LBN_DBLCLK) {
        /* Fixed an asserion failure when clicking on an empty List. */
        int nCurrentSelection = SendListMessage(LB_GETCURSEL);
        if (nCurrentSelection != LB_ERR && GetCount() > 0) {
            if (notifyCode == LBN_SELCHANGE) {
                DoCallback("handleListChanged", "(I)V", nCurrentSelection);
            }
            else if (notifyCode == LBN_DBLCLK) {
                DoCallback("handleAction", "(IJI)V", nCurrentSelection,
                           ::JVM_CurrentTimeMillis(NULL, 0),
                           (jint)AwtComponent::GetActionModifiers());
            }
        }
    }
    return mrDoDefault;
}

BOOL AwtList::InheritsNativeMouseWheelBehavior() {return true;}

jint AwtList::_GetMaxWidth(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    jint result = 0;
    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList *)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        result = l->GetMaxWidth();
    }
ret:
    env->DeleteGlobalRef(self);

    return result;
}

void AwtList::_UpdateMaxItemWidth(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    jobject self = (jobject)param;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList *)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->UpdateMaxItemWidth();
    }
ret:
    env->DeleteGlobalRef(self);
}

void AwtList::_AddItems(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    AddItemsStruct *ais = (AddItemsStruct *)param;
    jobject self = ais->list;
    jobjectArray items = ais->items;
    jint index = ais->index;
    jint width = ais->width;

    int badAlloc = 0;
    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    JNI_CHECK_NULL_GOTO(items, "null items", ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        int itemCount = env->GetArrayLength(items);
        if (itemCount > 0)
        {
            AwtList* l = (AwtList*)pData;
            l->SendListMessage(WM_SETREDRAW, (WPARAM)FALSE, 0);
            for (jsize i=0; i < itemCount; i++)
            {
                LPTSTR itemPtr = NULL;
                jstring item = (jstring)env->GetObjectArrayElement(items, i);
                if (env->ExceptionCheck()) goto ret;
                if (item == NULL) goto next_item;
                itemPtr = (LPTSTR)JNU_GetStringPlatformChars(env, item, 0);
                if (itemPtr == NULL)
                {
                    badAlloc = 1;
                }
                else
                {
                    l->InsertString(index+i, itemPtr);
                    JNU_ReleaseStringPlatformChars(env, item, itemPtr);
                }
                env->DeleteLocalRef(item);
next_item:
                ;
            }
            l->SendListMessage(WM_SETREDRAW, (WPARAM)TRUE, 0);
            l->InvalidateList(NULL, TRUE);
            l->CheckMaxWidth(width);
        }
    }
ret:
    env->DeleteGlobalRef(self);
    env->DeleteGlobalRef(items);

    delete ais;

    if (badAlloc)
    {
        throw std::bad_alloc();
    }
}

void AwtList::_DelItems(void *param)
{        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    DelItemsStruct *dis = (DelItemsStruct *)param;
    jobject self = dis->list;
    jint start = dis->start;
    jint end = dis->end;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        int count = l->GetCount();

        if (start == 0 && end == count)
        {
            l->SendListMessage(LB_RESETCONTENT);
        }
        else
        {
            for (int i = start; i <= end; i++)
            {
                l->SendListMessage(LB_DELETESTRING, start);
            }
        }

        l->UpdateMaxItemWidth();
    }
ret:
    env->DeleteGlobalRef(self);

    delete dis;
}

void AwtList::_Select(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SelectElementStruct *ses = (SelectElementStruct *)param;
    jobject self = ses->list;
    jint index = ses->index;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->Select(index);
    }
ret:
    env->DeleteGlobalRef(self);

    delete ses;
}

void AwtList::_Deselect(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SelectElementStruct *ses = (SelectElementStruct *)param;
    jobject self = ses->list;
    jint index = ses->index;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->Deselect(index);
    }
ret:
    env->DeleteGlobalRef(self);

    delete ses;
}

void AwtList::_MakeVisible(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SelectElementStruct *ses = (SelectElementStruct *)param;
    jobject self = ses->list;
    jint index = ses->index;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        l->SendListMessage(LB_SETTOPINDEX, index);
    }
ret:
    env->DeleteGlobalRef(self);

    delete ses;
}

jboolean AwtList::_IsSelected(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SelectElementStruct *ses = (SelectElementStruct *)param;
    jobject self = ses->list;
    jint index = ses->index;

    jboolean result = JNI_FALSE;
    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        result = l->IsItemSelected(index);
    }
ret:
    env->DeleteGlobalRef(self);

    delete ses;

    return result;
}

void AwtList::_SetMultipleSelections(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetMultipleSelectionsStruct *sms = (SetMultipleSelectionsStruct *)param;
    jobject self = sms->list;
    jboolean on = sms->on;

    AwtList *l = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    l = (AwtList*)pData;
    if (::IsWindow(l->GetHWnd()))
    {
        AwtToolkit::GetInstance().SendMessage(WM_AWT_LIST_SETMULTISELECT,
                                              (WPARAM)self, on);
    }
ret:
    env->DeleteGlobalRef(self);

    delete sms;
}

/************************************************************************
 * WListPeer native methods
 *
 * This class seems to have numerous bugs in it, but they are all bugs
 * which were present before conversion to JNI. -br.
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    getMaxWidth
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_windows_WListPeer_getMaxWidth(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    return (jint)((intptr_t)AwtToolkit::GetInstance().SyncCall(
        (void *(*)(void *))AwtList::_GetMaxWidth,
        (void *)selfGlobalRef));
    // selfGlobalRef is deleted in _GetMaxWidth

    CATCH_BAD_ALLOC_RET(0);
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    updateMaxItemWidth
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_updateMaxItemWidth(JNIEnv *env, jobject self)
{
    TRY;

    jobject selfGlobalRef = env->NewGlobalRef(self);

    AwtToolkit::GetInstance().SyncCall(AwtList::_UpdateMaxItemWidth,
        (void *)selfGlobalRef);
    // selfGlobalRef is deleted in _UpdateMaxItemWidth

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    addItems
 * Signature: ([Ljava/lang/String;II)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_addItems(JNIEnv *env, jobject self,
                                        jobjectArray items, jint index, jint width)
{
    TRY;

    AddItemsStruct *ais = new AddItemsStruct;
    ais->list = env->NewGlobalRef(self);
    ais->items = (jobjectArray)env->NewGlobalRef(items);
    ais->index = index;
    ais->width = width;

    AwtToolkit::GetInstance().SyncCall(AwtList::_AddItems, ais);
    // global refs and ais are deleted in _AddItems()

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    delItems
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_delItems(JNIEnv *env, jobject self,
                                        jint start, jint end)
{
    TRY;

    DelItemsStruct *dis = new DelItemsStruct;
    dis->list = env->NewGlobalRef(self);
    dis->start = start;
    dis->end = end;

    AwtToolkit::GetInstance().SyncCall(AwtList::_DelItems, dis);
    // global ref and dis are deleted in _DelItems

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    select
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_select(JNIEnv *env, jobject self,
                                      jint pos)
{
    TRY;

    SelectElementStruct *ses = new SelectElementStruct;
    ses->list = env->NewGlobalRef(self);
    ses->index = pos;

    AwtToolkit::GetInstance().SyncCall(AwtList::_Select, ses);
    // global ref and ses are deleted in _Select

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    deselect
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_deselect(JNIEnv *env, jobject self,
                                        jint pos)
{
    TRY;

    SelectElementStruct *ses = new SelectElementStruct;
    ses->list = env->NewGlobalRef(self);
    ses->index = pos;

    AwtToolkit::GetInstance().SyncCall(AwtList::_Deselect, ses);
    // global ref and ses are deleted in _Deselect

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    makeVisible
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_makeVisible(JNIEnv *env, jobject self,
                                           jint pos)
{
    TRY;

    SelectElementStruct *ses = new SelectElementStruct;
    ses->list = env->NewGlobalRef(self);
    ses->index = pos;

    AwtToolkit::GetInstance().SyncCall(AwtList::_MakeVisible, ses);
    // global ref and ses are deleted in _MakeVisible

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    setMultipleSelections
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_setMultipleSelections(JNIEnv *env, jobject self,
                                                     jboolean on)
{
    TRY;

    SetMultipleSelectionsStruct *sms = new SetMultipleSelectionsStruct;
    sms->list = env->NewGlobalRef(self);
    sms->on = on;

    AwtToolkit::GetInstance().SyncCall(AwtList::_SetMultipleSelections, sms);
    // global ref and sms are deleted in AwtList::_SetMultipleSelections

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WListPeer_create(JNIEnv *env, jobject self,
                                      jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)AwtList::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WListPeer
 * Method:    isSelected
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_windows_WListPeer_isSelected(JNIEnv *env, jobject self,
                                          jint index)
{
    TRY;

    SelectElementStruct *ses = new SelectElementStruct;
    ses->list = env->NewGlobalRef(self);
    ses->index = index;

    return (jboolean)((intptr_t)AwtToolkit::GetInstance().SyncCall(
        (void *(*)(void *))AwtList::_IsSelected, ses));
    // global ref and ses are deleted in _IsSelected

    CATCH_BAD_ALLOC_RET(FALSE);
}

} /* extern "C" */
