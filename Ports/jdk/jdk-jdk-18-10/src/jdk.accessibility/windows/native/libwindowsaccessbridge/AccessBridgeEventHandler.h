/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * A class to manage firing Accessibility events to Windows AT
 */

#ifndef __AccessBridgeEventHandler_H__
#define __AccessBridgeEventHandler_H__

#include "AccessBridgeCallbacks.h"
#include "AccessBridgePackages.h"

class WinAccessBridge;

class AccessBridgeEventHandler {
        long javaEventMask;
        long accessibilityEventMask;

        AccessBridge_PropertyChangeFP propertyChangeFP;
        AccessBridge_JavaShutdownFP javaShutdownFP;
        AccessBridge_FocusGainedFP focusGainedFP;
        AccessBridge_FocusLostFP focusLostFP;
        AccessBridge_CaretUpdateFP caretUpdateFP;
        AccessBridge_MouseClickedFP mouseClickedFP;
        AccessBridge_MouseEnteredFP mouseEnteredFP;
        AccessBridge_MouseExitedFP mouseExitedFP;
        AccessBridge_MousePressedFP mousePressedFP;
        AccessBridge_MouseReleasedFP mouseReleasedFP;
        AccessBridge_MenuCanceledFP menuCanceledFP;
        AccessBridge_MenuDeselectedFP menuDeselectedFP;
        AccessBridge_MenuSelectedFP menuSelectedFP;
        AccessBridge_PopupMenuCanceledFP popupMenuCanceledFP;
        AccessBridge_PopupMenuWillBecomeInvisibleFP popupMenuWillBecomeInvisibleFP;
        AccessBridge_PopupMenuWillBecomeVisibleFP popupMenuWillBecomeVisibleFP;

    AccessBridge_PropertyNameChangeFP propertyNameChangeFP;
    AccessBridge_PropertyDescriptionChangeFP propertyDescriptionChangeFP;
    AccessBridge_PropertyStateChangeFP propertyStateChangeFP;
    AccessBridge_PropertyValueChangeFP propertyValueChangeFP;
    AccessBridge_PropertySelectionChangeFP propertySelectionChangeFP;
    AccessBridge_PropertyTextChangeFP propertyTextChangeFP;
    AccessBridge_PropertyCaretChangeFP propertyCaretChangeFP;
    AccessBridge_PropertyVisibleDataChangeFP propertyVisibleDataChangeFP;
    AccessBridge_PropertyChildChangeFP propertyChildChangeFP;
    AccessBridge_PropertyActiveDescendentChangeFP propertyActiveDescendentChangeFP;

        AccessBridge_PropertyTableModelChangeFP propertyTableModelChangeFP;



public:
        AccessBridgeEventHandler();
        ~AccessBridgeEventHandler();
        long getJavaEventMask() {return javaEventMask;};
        long getAccessibilityEventMask() {return accessibilityEventMask;};

        // ------- Registry methods
        void setPropertyChangeFP(AccessBridge_PropertyChangeFP fp, WinAccessBridge *wab);
        void setJavaShutdownFP(AccessBridge_JavaShutdownFP fp, WinAccessBridge *wab);
        void setFocusGainedFP(AccessBridge_FocusGainedFP fp, WinAccessBridge *wab);
        void setFocusLostFP(AccessBridge_FocusLostFP fp, WinAccessBridge *wab);
        void setCaretUpdateFP(AccessBridge_CaretUpdateFP fp, WinAccessBridge *wab);
        void setMouseClickedFP(AccessBridge_MouseClickedFP fp, WinAccessBridge *wab);
        void setMouseEnteredFP(AccessBridge_MouseEnteredFP fp, WinAccessBridge *wab);
        void setMouseExitedFP(AccessBridge_MouseExitedFP fp, WinAccessBridge *wab);
        void setMousePressedFP(AccessBridge_MousePressedFP fp, WinAccessBridge *wab);
        void setMouseReleasedFP(AccessBridge_MouseReleasedFP fp, WinAccessBridge *wab);
        void setMenuCanceledFP(AccessBridge_MenuCanceledFP fp, WinAccessBridge *wab);
        void setMenuDeselectedFP(AccessBridge_MenuDeselectedFP fp, WinAccessBridge *wab);
        void setMenuSelectedFP(AccessBridge_MenuSelectedFP fp, WinAccessBridge *wab);
        void setPopupMenuCanceledFP(AccessBridge_PopupMenuCanceledFP fp, WinAccessBridge *wab);
        void setPopupMenuWillBecomeInvisibleFP(AccessBridge_PopupMenuWillBecomeInvisibleFP fp,
                                               WinAccessBridge *wab);
        void setPopupMenuWillBecomeVisibleFP(AccessBridge_PopupMenuWillBecomeVisibleFP fp,
                                             WinAccessBridge *wab);

        void setPropertyNameChangeFP(AccessBridge_PropertyNameChangeFP fp, WinAccessBridge *wab);
        void setPropertyDescriptionChangeFP(AccessBridge_PropertyDescriptionChangeFP fp,
                                            WinAccessBridge *wab);
        void setPropertyStateChangeFP(AccessBridge_PropertyStateChangeFP fp, WinAccessBridge *wab);
        void setPropertyValueChangeFP(AccessBridge_PropertyValueChangeFP fp, WinAccessBridge *wab);
        void setPropertySelectionChangeFP(AccessBridge_PropertySelectionChangeFP fp,
                                          WinAccessBridge *wab);
        void setPropertyTextChangeFP(AccessBridge_PropertyTextChangeFP fp, WinAccessBridge *wab);
        void setPropertyCaretChangeFP(AccessBridge_PropertyCaretChangeFP fp, WinAccessBridge *wab);
        void setPropertyVisibleDataChangeFP(AccessBridge_PropertyVisibleDataChangeFP fp,
                                            WinAccessBridge *wab);
        void setPropertyChildChangeFP(AccessBridge_PropertyChildChangeFP fp, WinAccessBridge *wab);
        void setPropertyActiveDescendentChangeFP(AccessBridge_PropertyActiveDescendentChangeFP fp,
                                                 WinAccessBridge *wab);

        void setPropertyTableModelChangeFP(AccessBridge_PropertyTableModelChangeFP fp,
                                           WinAccessBridge *wab);

        // ------- Event notification methods
        void firePropertyChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                wchar_t *property, wchar_t *oldName, wchar_t *newName);
        void fireJavaShutdown(long vmID);
        void fireFocusGained(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireFocusLost(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireCaretUpdate(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMouseClicked(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMouseEntered(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMouseExited(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMousePressed(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMouseReleased(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMenuCanceled(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMenuDeselected(long vmID, JOBJECT64 event, JOBJECT64 source);
        void fireMenuSelected(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePopupMenuCanceled(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePopupMenuWillBecomeInvisible(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePopupMenuWillBecomeVisible(long vmID, JOBJECT64 event, JOBJECT64 source);

        void firePropertyNameChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                        wchar_t *oldName, wchar_t *newName);
        void firePropertyDescriptionChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                               wchar_t *oldDescription, wchar_t *newDescription);
        void firePropertyStateChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                         wchar_t *oldState, wchar_t *newState);
        void firePropertyValueChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                         wchar_t *oldValue, wchar_t *newValue);
        void firePropertySelectionChange(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePropertyTextChange(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePropertyCaretChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                         int oldPosition, int newPosition);
        void firePropertyVisibleDataChange(long vmID, JOBJECT64 event, JOBJECT64 source);
        void firePropertyChildChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                         JOBJECT64 oldChild, JOBJECT64 newChild);
        void firePropertyActiveDescendentChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                                    JOBJECT64 oldActiveDescendent, JOBJECT64 newActiveDescendent);

        void firePropertyTableModelChange(long vmID, JOBJECT64 event, JOBJECT64 source,
                                                              wchar_t *oldValue, wchar_t *newValue);

};


#endif
