/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Toolkit.h"
#include "awt_TextField.h"
#include "awt_TextComponent.h"
#include "awt_Canvas.h"

/* IMPORTANT! Read the README.JNI file for notes on JNI converted AWT code.
 */

/***********************************************************************/
// struct for _SetEchoChar() method
struct SetEchoCharStruct {
    jobject textfield;
    jchar echoChar;
};
/************************************************************************
 * AwtTextField methods
 */

AwtTextField::AwtTextField()
{
}

/* Create a new AwtTextField object and window.   */
AwtTextField* AwtTextField::Create(jobject peer, jobject parent)
{
    return (AwtTextField*) AwtTextComponent::Create(peer, parent, false);
}

void AwtTextField::EditSetSel(CHARRANGE &cr) {
    SendMessage(EM_EXSETSEL, 0, reinterpret_cast<LPARAM>(&cr));

    // 6417581: force expected drawing
    if (IS_WINVISTA && cr.cpMin == cr.cpMax) {
        ::InvalidateRect(GetHWnd(), NULL, TRUE);
    }

}

LRESULT AwtTextField::WindowProc(UINT message, WPARAM wParam, LPARAM lParam)
{
    if (message == WM_UNDO || message == EM_UNDO || message == EM_CANUNDO) {
        if (GetWindowLong(GetHWnd(), GWL_STYLE) & ES_READONLY) {
            return FALSE;
        }
    }
    return AwtTextComponent::WindowProc(message, wParam, lParam);
}

MsgRouting
AwtTextField::HandleEvent(MSG *msg, BOOL synthetic)
{
    MsgRouting returnVal;
    BOOL systemBeeperEnabled = FALSE;
    /*
     * RichEdit 1.0 control starts internal message loop if the
     * left mouse button is pressed while the cursor is not over
     * the current selection or the current selection is empty.
     * Because of this we don't receive WM_MOUSEMOVE messages
     * while the left mouse button is pressed. To work around
     * this behavior we process the relevant mouse messages
     * by ourselves.
     * By consuming WM_MOUSEMOVE messages we also don't give
     * the RichEdit control a chance to recognize a drag gesture
     * and initiate its own drag-n-drop operation.
     *
     * The workaround also allows us to implement synthetic focus mechanism.
     */
    if (IsFocusingMouseMessage(msg)) {

        LONG lCurPos = EditGetCharFromPos(msg->pt);

        /*
         * NOTE: Plain EDIT control always clears selection on mouse
         * button press. We are clearing the current selection only if
         * the mouse pointer is not over the selected region.
         * In this case we sacrifice backward compatibility
         * to allow dnd of the current selection.
         */
        if (msg->message == WM_LBUTTONDBLCLK) {
            jchar echo = SendMessage(EM_GETPASSWORDCHAR);

            if(echo == 0){
              SetStartSelectionPos(static_cast<LONG>(SendMessage(
                  EM_FINDWORDBREAK, WB_MOVEWORDLEFT, lCurPos)));
              SetEndSelectionPos(static_cast<LONG>(SendMessage(
                  EM_FINDWORDBREAK, WB_MOVEWORDRIGHT, lCurPos)));
            }else{
              SetStartSelectionPos(0);
              SetEndSelectionPos(GetTextLength());
            }

        } else {
            SetStartSelectionPos(lCurPos);
            SetEndSelectionPos(lCurPos);
        }
        CHARRANGE cr;
        cr.cpMin = GetStartSelectionPos();
        cr.cpMax = GetEndSelectionPos();
        EditSetSel(cr);

        delete msg;
        return mrConsume;
    } else if (msg->message == WM_LBUTTONUP) {

        /*
         * If the left mouse button is pressed on the selected region
         * we don't clear the current selection. We clear it on button
         * release instead. This is to allow dnd of the current selection.
         */
        if (GetStartSelectionPos() == -1 && GetEndSelectionPos() == -1) {
            CHARRANGE cr;

            LONG lCurPos = EditGetCharFromPos(msg->pt);

            cr.cpMin = lCurPos;
            cr.cpMax = lCurPos;
            EditSetSel(cr);
        }

        /*
         * Cleanup the state variables when left mouse button is released.
         * These state variables are designed to reflect the selection state
         * while the left mouse button is pressed and be set to -1 otherwise.
         */
        SetStartSelectionPos(-1);
        SetEndSelectionPos(-1);
        SetLastSelectionPos(-1);

        delete msg;
        return mrConsume;
    } else if (msg->message == WM_MOUSEMOVE && (msg->wParam & MK_LBUTTON)) {

        /*
         * We consume WM_MOUSEMOVE while the left mouse button is pressed,
         * so we have to simulate selection autoscrolling when mouse is moved
         * outside of the client area.
         */
        POINT p;
        p.x = msg->pt.x;
        p.y = msg->pt.y;
        LONG lCurPos = EditGetCharFromPos(p);

        if (GetStartSelectionPos() != -1 &&
            GetEndSelectionPos() != -1 &&
            lCurPos != GetLastSelectionPos()) {

            CHARRANGE cr;

            SetLastSelectionPos(lCurPos);

            cr.cpMin = GetStartSelectionPos();
            cr.cpMax = GetLastSelectionPos();

            EditSetSel(cr);
        }
        delete msg;
        return mrConsume;
    } else if (msg->message == WM_KEYDOWN) {
        UINT virtualKey = (UINT) msg->wParam;

        switch(virtualKey){
          case VK_RETURN:
          case VK_UP:
          case VK_DOWN:
          case VK_LEFT:
          case VK_RIGHT:
          case VK_DELETE:
          case VK_BACK:
              SystemParametersInfo(SPI_GETBEEP, 0, &systemBeeperEnabled, 0);
              if(systemBeeperEnabled){
                  // disable system beeper for the RICHEDIT control to be compatible
                  // with the EDIT control behaviour
                  SystemParametersInfo(SPI_SETBEEP, 0, NULL, 0);
              }
              break;
          }
    } else if (msg->message == WM_SETTINGCHANGE) {
        if (msg->wParam == SPI_SETBEEP) {
            SystemParametersInfo(SPI_GETBEEP, 0, &systemBeeperEnabled, 0);
            if(systemBeeperEnabled){
                SystemParametersInfo(SPI_SETBEEP, 1, NULL, 0);
            }
        }
    }

    returnVal = AwtTextComponent::HandleEvent(msg, synthetic);

    if(systemBeeperEnabled){
        SystemParametersInfo(SPI_SETBEEP, 1, NULL, 0);
    }

    return returnVal;
}

void AwtTextField::_SetEchoChar(void *param)
{
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    SetEchoCharStruct *secs = (SetEchoCharStruct *)param;
    jobject self = secs->textfield;
    jchar echo = secs->echoChar;

    AwtTextField *c = NULL;

    PDATA pData;
    JNI_CHECK_PEER_GOTO(self, ret);
    c = (AwtTextField *)pData;
    if (::IsWindow(c->GetHWnd()))
    {
        c->SendMessage(EM_SETPASSWORDCHAR, echo);
        // Fix for 4307281: force redraw so that changes will take effect
        VERIFY(::InvalidateRect(c->GetHWnd(), NULL, FALSE));
    }
ret:
    env->DeleteGlobalRef(self);

    delete secs;
}


/************************************************************************
 * WTextFieldPeer native methods
 */

extern "C" {

/*
 * Class:     sun_awt_windows_WTextFieldPeer
 * Method:    create
 * Signature: (Lsun/awt/windows/WComponentPeer;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTextFieldPeer_create(JNIEnv *env, jobject self,
                                           jobject parent)
{
    TRY;

    AwtToolkit::CreateComponent(self, parent,
                                (AwtToolkit::ComponentFactory)
                                AwtTextField::Create);

    CATCH_BAD_ALLOC;
}

/*
 * Class:     sun_awt_windows_WTextFieldPeer
 * Method:    setEchoChar
 * Signature: (C)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_windows_WTextFieldPeer_setEchoChar(JNIEnv *env, jobject self,
                                                jchar ch)
{
    TRY;

    SetEchoCharStruct *secs = new SetEchoCharStruct;
    secs->textfield = env->NewGlobalRef(self);
    secs->echoChar = ch;

    AwtToolkit::GetInstance().SyncCall(AwtTextField::_SetEchoChar, secs);
    // global ref and secs are deleted in _SetEchoChar()

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
