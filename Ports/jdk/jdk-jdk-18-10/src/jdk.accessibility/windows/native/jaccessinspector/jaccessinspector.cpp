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
 * A sample Assistive Technology which queries the JavaVM to get the Java
 * Accessibility information available for a Java UI object, using the Java
 * Access Bridge.
 */

#include <windows.h>  // includes basic windows functionality
#include <jni.h>

#include "jaccessinspectorResource.h"
#include "AccessBridgeCalls.h"
#include "AccessBridgeCallbacks.h"

#include <stdio.h>
#include <time.h>
#include <string.h>

#include "jaccessinspector.h"
#include "AccessInfo.h"
#include "MessageHistory.h"

#define TIMER_ID 1
#define DISPLAY_INFO_MESSAGE WM_USER+1
#define DISPLAY_HWND_INFO_MESSAGE WM_USER+2

HWND theDialogWindow;
HWND theGoToDialogWindow;
HINSTANCE theInstance;
BOOL theAccessBridgeLoadedFlag;

HHOOK prevKbdHook;
HHOOK prevMouseHook;

BOOL updateMouse;
BOOL updateF1;
BOOL updateF2;

BOOL trackMouse;
BOOL trackMouseExited;
BOOL trackMouseClicked;
BOOL trackMousePressed;
BOOL trackMouseReleased;

BOOL trackFocus;
BOOL trackFocusLost;
BOOL trackCaret;
BOOL trackShutdown;

BOOL trackMenuSelected;
BOOL trackMenuDeselected;
BOOL trackMenuCanceled;

BOOL trackPopupVisible;
BOOL trackPopupInvisible;
BOOL trackPopupCanceled;

//BOOL trackPropertyChange;

BOOL trackPropertyNameChange;
BOOL trackPropertyDescriptionChange;
BOOL trackPropertyStateChange;
BOOL trackPropertyValueChange;
BOOL trackPropertySelectionChange;
BOOL trackPropertyTextChange;
BOOL trackPropertyCaretChange;
BOOL trackPropertyVisibleDataChange;
BOOL trackPropertyChildChange;
BOOL trackPropertyActiveDescendentChange;
BOOL trackPropertyTableModelChange;


FILE *logfile = NULL;

MessageHistory g_MessageHistory;

/**
 * WinMain
 *
 */
int WINAPI WinMain(HINSTANCE hInst, HINSTANCE hPrevInst, LPSTR lpCmdLine, int nShowCmd) {
    MSG msg;

    g_LogStringCallback = AddToMessageHistory;
    theInstance = hInst;
    theDialogWindow = NULL;
    theGoToDialogWindow = NULL;

    updateF1 = FALSE;
    updateF2 = FALSE;
    updateMouse = FALSE;

    theAccessBridgeLoadedFlag = FALSE;

    ReadActiveEventOptionsFromRegistry ();

    if (InitWindow(hInst)) {
        if (initializeAccessBridge() == TRUE) {
            theAccessBridgeLoadedFlag = TRUE;
            ApplyEventOptions(theDialogWindow);
            EnableMessageNavButtons();
            HACCEL hAccel =
                ::LoadAccelerators (theInstance, MAKEINTRESOURCE(IDR_ACCELERATOR));

            while (GetMessage(&msg, NULL, 0, 0)) {
                if ( FALSE == TranslateAccelerator(theDialogWindow, hAccel, &msg) ) {
                    if ( ( ( NULL == theDialogWindow ) ||
                           ( FALSE == IsDialogMessage(theDialogWindow, &msg) ) ) &&
                         ( ( NULL == theGoToDialogWindow ) ||
                           ( FALSE == IsDialogMessage(theGoToDialogWindow, &msg) ) ) ) {
                        TranslateMessage(&msg);
                        DispatchMessage(&msg);
                    }
                }
            }
            if (theAccessBridgeLoadedFlag == TRUE) {
                shutdownAccessBridge();
            }
            SaveActiveEventOptionsToRegistry ();
        }
    }
    return(0);
}

char szAppName [] = "JACCESSINSPECTORWINDOW";

/**
 * Real(tm) MS-Windows window initialization
 *
 */
BOOL InitWindow (HANDLE hInstance) {
    theDialogWindow = CreateDialog((struct HINSTANCE__ *)hInstance,
                                   szAppName,
                                   NULL,
                                   jaccessinspectorDialogProc);

    // If window could not be created, return "failure".
    if (!theDialogWindow)
        return FALSE;

    if (logfile == null) {
        logfile = fopen(JACCESSINSPECTOR_LOG, "w"); // overwrite existing log file
        logString(logfile, "Starting jaccessinspector.exe %s\n", getTimeAndDate());
    }

    // Make the window visible, update its client area, & return "success".
    SetWindowText(theDialogWindow, "jaccessinspector");
    ShowWindow(theDialogWindow, SW_SHOWNORMAL);
    UpdateWindow(theDialogWindow);

    return TRUE;
}

/**
 * Display Accessible information about the object under the mouse
 */
void displayAccessibleInfo(long vmID, AccessibleContext ac, int x, int y) {
    char buffer[HUGE_BUFSIZE];

    getAccessibleInfo(vmID, ac, x, y, buffer, (int)(sizeof(buffer)));
    displayAndLog(theDialogWindow, cjaccessinspectorText, logfile, (char *)buffer);
}

/**
 * Display Java event info
 */
void displayJavaEvent(long vmID, AccessibleContext ac, char *announcement) {
    char buffer[HUGE_BUFSIZE];
    char *bufOffset;

    strncpy(buffer, announcement, sizeof(buffer));

    bufOffset = (char *)(buffer + strlen(buffer));
    getAccessibleInfo( vmID, ac, -1, -1, bufOffset,
                       (int)(sizeof(buffer) - strlen(buffer)) );
    displayAndLog(theDialogWindow, cjaccessinspectorText, logfile, (char *)buffer);
}


/**
 * Display Accessible propertyChange event info
 */
void displayAccessiblePropertyChange(long vmID, AccessibleContext ac,
                                     char *announcement) {
    char buffer[HUGE_BUFSIZE];
    char *bufOffset;

    strncpy(buffer, announcement, sizeof(buffer));

    bufOffset = (char *) (buffer + strlen(buffer));
    getAccessibleInfo( vmID, ac, -1, -1, bufOffset,
                       (int)(sizeof(buffer) - strlen(buffer)) );
    displayAndLog(theDialogWindow, cjaccessinspectorText, logfile, (char *)buffer);
}


/**
 * Update display under mouse when it moves
 */
void echoMouseObject() {
    long vmID;
    AccessibleContext acParent;
    AccessibleContext ac;
    POINT p;
    HWND hwnd;
    RECT windowRect;

    GetCursorPos(&p);
    hwnd = WindowFromPoint(p);
    if (GetAccessibleContextFromHWND(hwnd, &vmID, &acParent)) {
        GetWindowRect(hwnd, &windowRect);
        // send the point in global coordinates; Java will handle it!
        if (GetAccessibleContextAt(vmID, acParent, (jint) p.x, (jint) p.y, &ac)) {
            displayAccessibleInfo(vmID, ac, p.x, p.y);  // can handle null
            ReleaseJavaObject(vmID, ac);
        }
    }
}


/**
 * Update display under HWND the mouse is in
 */
void echoMouseHWNDObject() {
    long vmID;
    AccessibleContext ac;
    POINT p;
    HWND hwnd;

    GetCursorPos(&p);
    hwnd = WindowFromPoint(p);

    if (GetAccessibleContextFromHWND(hwnd, &vmID, &ac)) {
        displayAccessibleInfo(vmID, ac, 0, 0);  // can handle null
        ReleaseJavaObject(vmID, ac);
    }
}

/**
 * Display Accessible information about the object that has focus in
 * the topmost Java HWND
 *
 */
void displayFocusedObject() {
    HWND hWnd;
    long vmID;
    AccessibleContext ac;

    hWnd = GetTopWindow(NULL);
    while (hWnd != NULL) {
        if (IsJavaWindow(hWnd)) {
            if (GetAccessibleContextWithFocus(hWnd, &vmID, &ac) == TRUE) {
                displayAccessibleInfo(vmID, ac, 0, 0);
                ReleaseJavaObject(vmID, ac);
            }
            return;
        } else {
            hWnd = GetNextWindow(hWnd, GW_HWNDNEXT);
        }
    }
}

/*
 * Handle notification of the Java application shutting down
 */
void HandleJavaShutdown(long vmID) {
    char s[1024];
    wsprintf(s, "Java VM 0x%X terminated\r\n\r\n", vmID);

    displayJavaEvent(vmID, null, s); // intentially passing null AccessibleContext
    displayAndLog(theDialogWindow, cjaccessinspectorText, logfile, (char *)s);
}

/**
 * Handle a FocusGained event
 */
void HandleJavaFocusGained(long vmID, FocusEvent event, AccessibleContext ac) {

    char s[1024];
    wsprintf(s, "FocusGained\r\n\r\n");

    if (ac != (AccessibleContext) 0) {
        displayJavaEvent(vmID, ac, s);
    }

    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a FocusLost event
 */
void HandleJavaFocusLost(long vmID, FocusEvent event, AccessibleContext ac) {

    // NOTE: calling GetAccessibleContextWithFocus() after a FocusLost event
    //       would result in a null AccessibleContext being returned, since
    //       at that point, no object has the focus.  If the topmost window
    //       does not belong to a JavaVM, then no component within a JavaVM
    //       will have the focus (and again, GetAccessibleContextWithFocus()
    //       will return a null AccessibleContext).  You should always get
    //       a FocusLost event when a window not belonging to a JavaVM becomes
    //       topmost.

    char s[1024];
    wsprintf(s, "FocusLost\r\n\r\n");

    if (ac != (AccessibleContext) 0) {
        displayJavaEvent(vmID, ac, s);
    }
    /*
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    */
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a CaretUpdate event
 */
void HandleJavaCaretUpdate(long vmID, CaretEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MouseClicked event
 */
void HandleMouseClicked(long vmID, MouseEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MouseEntered event
 */
void HandleMouseEntered(long vmID, MouseEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }

    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MouseExited event
 */
void HandleMouseExited(long vmID, MouseEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MousePressed event
 */
void HandleMousePressed(long vmID, MouseEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MouseReleased event
 */
void HandleMouseReleased(long vmID, MouseEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MenuCanceled event
 */
void HandleMenuCanceled(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);    // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event); // must always release, to stave off memory leaks
}

/**
 * Handle a MenuDeselected event
 */
void HandleMenuDeselected(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a MenuSelected event
 */
void HandleMenuSelected(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a PopupMenuCanceled event
 */
void HandlePopupMenuCanceled(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a PopupMenuWillBecomeInvisible event
 */
void HandlePopupMenuWillBecomeInvisible(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a PopupMenuWillBecomeVisible event
 */
void HandlePopupMenuWillBecomeVisible(long vmID, MenuEvent event, AccessibleContext ac) {
    if (ac != (AccessibleContext) 0) {
        displayAccessibleInfo(vmID, ac, 0, 0);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}




/**
 * Handle a HandlePropertyNameChange event
 */
void HandlePropertyNameChange(long vmID, PropertyChangeEvent event, AccessibleContext ac,
                              wchar_t *oldName, wchar_t *newName) {
    char s[1024];
    wsprintf(s, "Name changed event: old = %ls; new = %ls\r\n\r\n", oldName, newName);

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyDescriptionChange event
 */
void HandlePropertyDescriptionChange( long vmID,
                                      PropertyChangeEvent event,
                                      AccessibleContext ac,
                                      wchar_t *oldDescription,
                                      wchar_t *newDescription ) {
    char s[1024];
    wsprintf( s, "Description changed event: old = %ls; new = %ls\r\n\r\n",
              oldDescription, newDescription );

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyStateChange event
 */
void HandlePropertyStateChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                wchar_t *oldState, wchar_t *newState ) {
    char s[1024];
    wsprintf( s, "State changed event: old = %ls; new = %ls\r\n\r\n",
              oldState, newState );

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyValueChange event
 */
void HandlePropertyValueChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                wchar_t *oldValue, wchar_t *newValue ) {
    char s[1024];
    wsprintf( s, "Value changed event: old = %ls; new = %ls\r\n\r\n",
              oldValue, newValue );

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertySelectionChange event
 */
void HandlePropertySelectionChange( long vmID, PropertyChangeEvent event,
                                    AccessibleContext ac ) {
    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange( vmID, ac,
                                         "Selection changed event\r\n\r\n" );
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyTextChange event
 */
void HandlePropertyTextChange( long vmID, PropertyChangeEvent event,
                               AccessibleContext ac ) {
    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, "Text changed event\r\n\r\n");
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyCaretChange event
 */
void HandlePropertyCaretChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                int oldPosition, int newPosition ) {
    char s[1024];

    wsprintf( s, "Caret changed event: oldPosition = %d; newPosition = %d\r\n\r\n",
              oldPosition, newPosition );

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyVisibleDataChange event
 */
void HandlePropertyVisibleDataChange( long vmID, PropertyChangeEvent event,
                                      AccessibleContext ac ) {
    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange( vmID, ac,
                                         "VisibleData changed event\r\n\r\n" );
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyChildChange event
 */
void HandlePropertyChildChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                JOBJECT64 oldChild, JOBJECT64 newChild ) {
    char buffer[HUGE_BUFSIZE];
    char *bufOffset;

    sprintf( buffer,
             "Child property changed event:\r\n=======================\r\n\r\n" );

    if (oldChild != 0) {
        strncat(buffer, "Old Accessible Child info:\r\n\r\n", sizeof(buffer));
        bufOffset = (char *) (buffer + strlen(buffer));
        getAccessibleInfo( vmID, oldChild, 0, 0, bufOffset,
                           (int)(sizeof(buffer) - strlen(buffer)) );
        strncat(buffer, "\r\n\r\n", sizeof(buffer));
    }

    if (newChild != 0) {
        strncat(buffer, "New Accessible Child info:\r\n\r\n", sizeof(buffer));
        bufOffset = (char *) (buffer + strlen(buffer));
        getAccessibleInfo( vmID, newChild, 0, 0, bufOffset,
                          (int)(sizeof(buffer) - strlen(buffer)) );
        strncat(buffer, "\r\n\r\n", sizeof(buffer));
    }

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, buffer);
    }

    ReleaseJavaObject(vmID, ac);        // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, oldChild);  // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, newChild);  // must always release, to stave off memory leaks
}

/**
 * Handle a HandlePropertyActiveDescendentChange event
 */
void HandlePropertyActiveDescendentChange( long vmID, PropertyChangeEvent event,
                                           AccessibleContext ac,
                                           JOBJECT64 oldActiveDescendent,
                                           JOBJECT64 newActiveDescendent ) {
    char buffer[HUGE_BUFSIZE];

    sprintf( buffer,
             "ActiveDescendent property changed event:\r\n=======================\r\n\r\n" );

#ifdef _notdef
    char *bufOffset;
    if (oldActiveDescendent != 0) {
        strncat(buffer, "Old Accessible ActiveDescendent info:\r\n\r\n", sizeof(buffer));
        bufOffset = (char *) (buffer + strlen(buffer));
        getAccessibleInfo( vmID, oldActiveDescendent, 0, 0, bufOffset,
                           (int)(sizeof(buffer) - strlen(buffer)) );
        strncat(buffer, "\r\n\r\n", sizeof(buffer));
    }

    if (newActiveDescendent != 0) {
        strncat( buffer, "New Accessible ActiveDescendent info:\r\n\r\n",
                 sizeof(buffer) );
        bufOffset = (char *) (buffer + strlen(buffer));
        getAccessibleInfo( vmID, newActiveDescendent, 0, 0, bufOffset,
                           (int)(sizeof(buffer) - strlen(buffer)) );
        strncat(buffer, "\r\n\r\n", sizeof(buffer));
    }
#endif _notdef

    if (newActiveDescendent != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, newActiveDescendent, buffer);
    }

    ReleaseJavaObject(vmID, ac);  // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, oldActiveDescendent);  // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, newActiveDescendent);  // must always release, to stave off memory leaks
}


/**
 * Handle a HandlePropertyTableModelChange event
 */
void HandlePropertyTableModelChange( long vmID, PropertyChangeEvent event,
                                     AccessibleContext ac,
                                     wchar_t *oldValue, wchar_t *newValue ) {

    char s[1024];
    wsprintf( s, "Table Model Change: old = %ls; new = %ls\r\n\r\n",
              oldValue, newValue );

    if (ac != (AccessibleContext) 0) {
        displayAccessiblePropertyChange(vmID, ac, s);
    }
    ReleaseJavaObject(vmID, ac);     // must always release, to stave off memory leaks
    ReleaseJavaObject(vmID, event);  // must always release, to stave off memory leaks
}



#define DOWN_UP_FLAG 1<<31

void CALLBACK TimerProc(HWND hWnd, UINT uTimerMsg, UINT uTimerID, DWORD dwTime) {
    // when mouse settles from movement
    KillTimer(hWnd, uTimerID);
    if (updateMouse == TRUE) {
        PostMessage(theDialogWindow, DISPLAY_INFO_MESSAGE, 0, 0);
    }
}

LRESULT CALLBACK KeyboardProc(int code, WPARAM wParam, LPARAM lParam) {
    // on mouse-up of F1
    if (code < 0) {
        CallNextHookEx(prevKbdHook, code, wParam, lParam);
    } else if (wParam == VK_F1 && lParam & DOWN_UP_FLAG && updateF1) {
        PostMessage(theDialogWindow, DISPLAY_INFO_MESSAGE, wParam, lParam);
    } else if (wParam == VK_F2 && lParam & DOWN_UP_FLAG && updateF2) {
        PostMessage(theDialogWindow, DISPLAY_HWND_INFO_MESSAGE, wParam, lParam);
    }
    return 0;
}

LRESULT CALLBACK MouseProc(int code, WPARAM wParam, LPARAM lParam) {
    // when mouse settles from movement
    if (code < 0) {
        CallNextHookEx(prevMouseHook, code, wParam, lParam);
    } else {
        // reset the timer every time the mouse moves
        KillTimer(theDialogWindow, TIMER_ID);
        SetTimer(theDialogWindow, TIMER_ID, 1000, (TIMERPROC)TimerProc);
    }
    return 0;
}

void exitjaccessinspector(HWND hWnd) {
    EndDialog(hWnd, TRUE);
    PostQuitMessage (0);
}

#define INSTALL_EVENT_LISTENER(toggleVal, setFP, handler)   \
    if (toggleVal) {                                        \
        setFP(handler);                                     \
    }

void reinstallEventListeners() {
    INSTALL_EVENT_LISTENER(trackMouse, SetMouseEntered, HandleMouseEntered);
    INSTALL_EVENT_LISTENER(trackMouseExited, SetMouseExited, HandleMouseExited);
    INSTALL_EVENT_LISTENER(trackMouseClicked, SetMouseClicked, HandleMouseClicked);
    INSTALL_EVENT_LISTENER(trackMousePressed, SetMousePressed, HandleMousePressed);
    INSTALL_EVENT_LISTENER(trackMouseReleased, SetMouseReleased, HandleMouseReleased);
    INSTALL_EVENT_LISTENER(trackShutdown, SetJavaShutdown, HandleJavaShutdown);
    INSTALL_EVENT_LISTENER(trackFocus, SetFocusGained, HandleJavaFocusGained);
    INSTALL_EVENT_LISTENER(trackFocusLost, SetFocusLost, HandleJavaFocusLost);
    INSTALL_EVENT_LISTENER(trackCaret, SetCaretUpdate, HandleJavaCaretUpdate);

    INSTALL_EVENT_LISTENER(trackMenuSelected, SetMenuSelected, HandleMenuSelected);
    INSTALL_EVENT_LISTENER(trackMenuDeselected, SetMenuDeselected, HandleMenuDeselected);
    INSTALL_EVENT_LISTENER(trackMenuCanceled, SetMenuCanceled, HandleMenuCanceled);

    INSTALL_EVENT_LISTENER( trackPopupVisible, SetPopupMenuWillBecomeVisible,
                            HandlePopupMenuWillBecomeVisible );
    INSTALL_EVENT_LISTENER( trackPopupInvisible, SetPopupMenuWillBecomeInvisible,
                            HandlePopupMenuWillBecomeInvisible );
    INSTALL_EVENT_LISTENER( trackPopupCanceled, SetPopupMenuCanceled,
                            HandlePopupMenuCanceled );

    INSTALL_EVENT_LISTENER( trackPropertyNameChange, SetPropertyNameChange,
                            HandlePropertyNameChange);
    INSTALL_EVENT_LISTENER( trackPropertyDescriptionChange,
                            SetPropertyDescriptionChange,
                            HandlePropertyDescriptionChange );
    INSTALL_EVENT_LISTENER( trackPropertyStateChange, SetPropertyStateChange,
                            HandlePropertyStateChange );
    INSTALL_EVENT_LISTENER( trackPropertyValueChange, SetPropertyValueChange,
                            HandlePropertyValueChange );
    INSTALL_EVENT_LISTENER( trackPropertySelectionChange,
                            SetPropertySelectionChange,
                            HandlePropertySelectionChange );
    INSTALL_EVENT_LISTENER( trackPropertyTextChange, SetPropertyTextChange,
                            HandlePropertyTextChange );
    INSTALL_EVENT_LISTENER( trackPropertyCaretChange, SetPropertyCaretChange,
                            HandlePropertyCaretChange );
    INSTALL_EVENT_LISTENER( trackPropertyVisibleDataChange,
                            SetPropertyVisibleDataChange,
                            HandlePropertyVisibleDataChange );
    INSTALL_EVENT_LISTENER( trackPropertyChildChange, SetPropertyChildChange,
                            HandlePropertyChildChange );
    INSTALL_EVENT_LISTENER( trackPropertyActiveDescendentChange,
                            SetPropertyActiveDescendentChange,
                            HandlePropertyActiveDescendentChange );
    INSTALL_EVENT_LISTENER( trackPropertyTableModelChange,
                            SetPropertyTableModelChange,
                            HandlePropertyTableModelChange );
}


#define TRACK_EVENT_TOGGLE(menuItem, toggleVal, setFP, handler) \
    case menuItem:                                              \
        menu = GetMenu(hWnd);                                   \
        if (toggleVal) {                                        \
            toggleVal = FALSE;                                  \
            CheckMenuItem(menu, menuItem,                       \
                          MF_BYCOMMAND | MF_UNCHECKED);         \
            setFP(NULL);                                        \
        } else {                                                \
            toggleVal = TRUE;                                   \
            CheckMenuItem(menu, menuItem,                       \
                          MF_BYCOMMAND | MF_CHECKED);           \
            setFP(handler);                                     \
        }                                                       \
        MaybeCheckMonitorTheSameEventsAsJAWS(menu);             \
        MaybeCheckMonitorAllEvents(menu);                       \
        return TRUE

INT_PTR CALLBACK jaccessinspectorDialogProc( HWND hWnd, UINT message,
                                             WPARAM wParam, LPARAM lParam ) {
    const int minWindowWidth = 540;
    const int minWindowHeight = 300;
    static int titleBarHeight = ::GetSystemMetrics(SM_CYSIZE);
    static int menuBarHeight = ::GetSystemMetrics(SM_CYMENU);
    static int borderHeight = ::GetSystemMetrics(SM_CYBORDER);
    static int borderWidth = ::GetSystemMetrics(SM_CXBORDER);
    static int verticalScrollBarWidth = ::GetSystemMetrics(SM_CXVSCROLL);
    int command;
    short width, height;
    HWND dlgItem;
    RECT dlgItemRect;
    RECT dialogBoxRect;
    LONG lT;
    HMENU menu;
    DWORD lastError = 0;

    switch (message) {

    case WM_CREATE:
        return 0;

    case WM_INITDIALOG:
        CheckMenuItem(GetMenu(hWnd), cAccessBridgeDLLLoaded, MF_BYCOMMAND | MF_CHECKED);
        return TRUE;

    case WM_CLOSE:
        exitjaccessinspector(hWnd);
        return TRUE;

    case WM_SIZE:
        width = LOWORD(lParam);
        height = HIWORD(lParam);
        dlgItem = GetDlgItem(theDialogWindow, cjaccessinspectorText);
        ::GetWindowRect(dlgItem, &dlgItemRect);
        ::GetWindowRect(theDialogWindow, &dialogBoxRect);
        lT = dlgItemRect.top - dialogBoxRect.top - titleBarHeight -
             menuBarHeight + (borderHeight * 4);
        SetWindowPos( dlgItem, NULL, 0, 0,
                      width - (borderWidth * 2) - verticalScrollBarWidth,
                      height - lT, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);
        return FALSE;  // let windows finish handling this

    case WM_GETMINMAXINFO:
        {
            LPMINMAXINFO lpMMI = (LPMINMAXINFO)lParam;
            lpMMI->ptMinTrackSize.x = minWindowWidth;
            lpMMI->ptMinTrackSize.y = minWindowHeight;
            return TRUE;
        }
        break;

    case WM_COMMAND:
        command = LOWORD(wParam);

        switch(command) {
        case cAccessBridgeDLLLoaded:    // toggle; unload or load AccessBridge
            if (theAccessBridgeLoadedFlag) {
                shutdownAccessBridge();
                theAccessBridgeLoadedFlag = FALSE;
                CheckMenuItem( GetMenu(hWnd), cAccessBridgeDLLLoaded,
                               MF_BYCOMMAND | MF_UNCHECKED );
            } else {
                theAccessBridgeLoadedFlag = initializeAccessBridge();
                if (theAccessBridgeLoadedFlag) {
                    CheckMenuItem( GetMenu(hWnd), cAccessBridgeDLLLoaded,
                                   MF_BYCOMMAND | MF_CHECKED );
                    reinstallEventListeners();
                }
            }
            return TRUE;

        case cExitMenuItem:
            exitjaccessinspector(hWnd);
            return TRUE;

        TRACK_EVENT_TOGGLE( cTrackMouseMenuItem, trackMouse, SetMouseEntered,
                            HandleMouseEntered );
        TRACK_EVENT_TOGGLE( cTrackMouseExitedMenuItem, trackMouseExited,
                            SetMouseExited, HandleMouseExited );
        TRACK_EVENT_TOGGLE( cTrackMouseClickedMenuItem, trackMouseClicked,
                            SetMouseClicked, HandleMouseClicked );
        TRACK_EVENT_TOGGLE( cTrackMousePressedMenuItem, trackMousePressed,
                            SetMousePressed, HandleMousePressed );
        TRACK_EVENT_TOGGLE( cTrackMouseReleasedMenuItem, trackMouseReleased,
                            SetMouseReleased, HandleMouseReleased );
        TRACK_EVENT_TOGGLE( cTrackShutdownMenuItem, trackShutdown,
                            SetJavaShutdown, HandleJavaShutdown );
        TRACK_EVENT_TOGGLE( cTrackFocusMenuItem, trackFocus, SetFocusGained,
                            HandleJavaFocusGained );
        TRACK_EVENT_TOGGLE( cTrackFocusLostMenuItem, trackFocusLost,
                            SetFocusLost, HandleJavaFocusLost );
        TRACK_EVENT_TOGGLE( cTrackCaretMenuItem, trackCaret, SetCaretUpdate,
                            HandleJavaCaretUpdate );

        TRACK_EVENT_TOGGLE( cTrackMenuSelectedMenuItem, trackMenuSelected,
                            SetMenuSelected, HandleMenuSelected );
        TRACK_EVENT_TOGGLE( cTrackMenuDeselectedMenuItem, trackMenuDeselected,
                            SetMenuDeselected, HandleMenuDeselected );
        TRACK_EVENT_TOGGLE( cTrackMenuCanceledItem, trackMenuCanceled,
                            SetMenuCanceled, HandleMenuCanceled );

        TRACK_EVENT_TOGGLE( cTrackPopupBecomeVisibleMenuItem, trackPopupVisible,
                            SetPopupMenuWillBecomeVisible,
                            HandlePopupMenuWillBecomeVisible );
        TRACK_EVENT_TOGGLE( cTrackPopupBecomeInvisibleMenuItem,
                            trackPopupInvisible,
                            SetPopupMenuWillBecomeInvisible,
                            HandlePopupMenuWillBecomeInvisible );
        TRACK_EVENT_TOGGLE( cTrackPopupCanceledItem, trackPopupCanceled,
                            SetPopupMenuCanceled, HandlePopupMenuCanceled );

        TRACK_EVENT_TOGGLE( cTrackPropertyNameItem, trackPropertyNameChange,
                            SetPropertyNameChange, HandlePropertyNameChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyDescriptionItem,
                            trackPropertyDescriptionChange,
                            SetPropertyDescriptionChange,
                            HandlePropertyDescriptionChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyStateItem, trackPropertyStateChange,
                            SetPropertyStateChange, HandlePropertyStateChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyValueItem, trackPropertyValueChange,
                            SetPropertyValueChange, HandlePropertyValueChange );
        TRACK_EVENT_TOGGLE( cTrackPropertySelectionItem,
                            trackPropertySelectionChange,
                            SetPropertySelectionChange,
                            HandlePropertySelectionChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyTextItem, trackPropertyTextChange,
                            SetPropertyTextChange, HandlePropertyTextChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyCaretItem, trackPropertyCaretChange,
                            SetPropertyCaretChange, HandlePropertyCaretChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyVisibleDataItem,
                            trackPropertyVisibleDataChange,
                            SetPropertyVisibleDataChange,
                            HandlePropertyVisibleDataChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyChildItem, trackPropertyChildChange,
                            SetPropertyChildChange, HandlePropertyChildChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyActiveDescendentItem,
                            trackPropertyActiveDescendentChange,
                            SetPropertyActiveDescendentChange,
                            HandlePropertyActiveDescendentChange );
        TRACK_EVENT_TOGGLE( cTrackPropertyTableModelChangeItem,
                            trackPropertyTableModelChange,
                            SetPropertyTableModelChange,
                            HandlePropertyTableModelChange );

        case cUpdateFromMouseMenuItem:
            menu = GetMenu(hWnd);
            if (updateMouse) {
                updateMouse = FALSE;
                CheckMenuItem( menu, cUpdateFromMouseMenuItem,
                               MF_BYCOMMAND | MF_UNCHECKED );
                UnhookWindowsHookEx((HHOOK)MouseProc);
                KillTimer(hWnd, TIMER_ID);
            } else {
                updateMouse = TRUE;
                CheckMenuItem( menu, cUpdateFromMouseMenuItem,
                               MF_BYCOMMAND | MF_CHECKED );
                prevMouseHook = SetWindowsHookEx( WH_MOUSE, MouseProc,
                                                  theInstance,
                                                  ::GetCurrentThreadId() );
                if (! prevMouseHook) {
                    lastError = ::GetLastError();
                }
            }
            return TRUE;

        case cUpdateWithF1Item:
            menu = GetMenu(hWnd);
            if (updateF1) {
                updateF1 = FALSE;
                CheckMenuItem( menu, cUpdateWithF1Item,
                               MF_BYCOMMAND | MFS_UNCHECKED );
                UnhookWindowsHookEx((HHOOK)KeyboardProc);
            } else {
                updateF1 = TRUE;
                CheckMenuItem( menu, cUpdateWithF1Item,
                               MF_BYCOMMAND | MFS_CHECKED );
                prevKbdHook = SetWindowsHookEx( WH_KEYBOARD, KeyboardProc,
                                                theInstance,
                                                ::GetCurrentThreadId() );
                if (! prevKbdHook) {
                    lastError = ::GetLastError();
                }
            }
            return TRUE;

        case cUpdateWithF2Item:
            menu = GetMenu(hWnd);
            if (updateF2) {
                updateF2 = FALSE;
                CheckMenuItem( menu, cUpdateWithF2Item,
                               MF_BYCOMMAND | MFS_UNCHECKED );
                UnhookWindowsHookEx((HHOOK)KeyboardProc);
            } else {
                updateF2 = TRUE;
                CheckMenuItem(menu, cUpdateWithF2Item,
                              MF_BYCOMMAND | MFS_CHECKED);
                prevKbdHook = SetWindowsHookEx( WH_KEYBOARD, KeyboardProc,
                                                theInstance, ::GetCurrentThreadId() );
                if (! prevKbdHook) {
                    lastError = ::GetLastError();
                }
            }
            return TRUE;

        case cMonitorTheSameEventsAsJAWS:
            /*
            Causes jaccessinspetor to monitor the same events as JAWS.  Useful
            when testing to determine if a bug is specific to JAWS or if it can
            be reproduced in jaccessinspector as well.
            */
            trackMouse = FALSE;
            trackMouseExited = FALSE;
            trackMouseClicked = FALSE;
            trackMousePressed = FALSE;
            trackMouseReleased = FALSE;
            trackFocus = TRUE;
            trackFocusLost = TRUE;
            trackCaret = FALSE;
            trackShutdown = FALSE;

            trackMenuSelected = FALSE;
            trackMenuDeselected = FALSE;
            trackMenuCanceled = FALSE;

            trackPopupVisible = FALSE;
            trackPopupInvisible = FALSE;
            trackPopupCanceled = FALSE;

            trackPropertyNameChange = TRUE;
            trackPropertyDescriptionChange = TRUE;
            trackPropertyStateChange = TRUE;
            trackPropertyValueChange = TRUE;
            trackPropertySelectionChange = TRUE;
            trackPropertyTextChange = TRUE;
            trackPropertyCaretChange = TRUE;
            trackPropertyVisibleDataChange = FALSE;
            trackPropertyChildChange = TRUE;
            trackPropertyActiveDescendentChange = TRUE;
            trackPropertyTableModelChange = FALSE;

            ApplyEventOptions(hWnd);

            return TRUE;

        case cMonitorAllEvents:
            /*
            Causes jaccessinspector to monitor all Java Events and all
            Accessibility Events.
            */
            trackMouse = TRUE;
            trackMouseExited = TRUE;
            trackMouseClicked = TRUE;
            trackMousePressed = TRUE;
            trackMouseReleased = TRUE;
            trackFocus = TRUE;
            trackFocusLost = TRUE;
            trackCaret = TRUE;
            trackShutdown = TRUE;

            trackMenuSelected = TRUE;
            trackMenuDeselected = TRUE;
            trackMenuCanceled = TRUE;

            trackPopupVisible = TRUE;
            trackPopupInvisible = TRUE;
            trackPopupCanceled = TRUE;

            trackPropertyNameChange = TRUE;
            trackPropertyDescriptionChange = TRUE;
            trackPropertyStateChange = TRUE;
            trackPropertyValueChange = TRUE;
            trackPropertySelectionChange = TRUE;
            trackPropertyTextChange = TRUE;
            trackPropertyCaretChange = TRUE;
            trackPropertyVisibleDataChange = TRUE;
            trackPropertyChildChange = TRUE;
            trackPropertyActiveDescendentChange = TRUE;
            trackPropertyTableModelChange = TRUE;

            ApplyEventOptions(hWnd);

            return TRUE;

        case cFirstMessage:
            {
                const char * messageText = g_MessageHistory.GetFirstMessage ();
                if ( ( NULL != messageText ) && ( 0 != messageText [0] ) ) {
                    ::SetDlgItemText( theDialogWindow, cjaccessinspectorText,
                                      messageText );
                }
                EnableMessageNavButtons();
                UpdateMessageNumber();
                return TRUE;
            }
            break;

        case cPreviousMessage:
            {
                const char * messageText = g_MessageHistory.GetPreviousMessage ();
                if ( ( NULL != messageText ) && ( 0 != messageText [0] ) ) {
                    ::SetDlgItemText( theDialogWindow, cjaccessinspectorText,
                                      messageText );
                }
                EnableMessageNavButtons();
                UpdateMessageNumber();
                return TRUE;
            }
            break;

        case cNextMessage:
            {
                const char * messageText = g_MessageHistory.GetNextMessage ();
                if ( ( NULL != messageText ) && ( 0 != messageText [0] ) ) {
                    ::SetDlgItemText( theDialogWindow, cjaccessinspectorText,
                                      messageText );
                }
                EnableMessageNavButtons();
                UpdateMessageNumber();
                return TRUE;
            }
            break;

        case cLastMessage:
            {
                const char * messageText = g_MessageHistory.GetLastMessage();
                if ( ( NULL != messageText ) && ( 0 != messageText [0] ) ) {
                    ::SetDlgItemText( theDialogWindow, cjaccessinspectorText,
                                      messageText );
                }
                EnableMessageNavButtons();
                UpdateMessageNumber();
                return TRUE;
            }
            break;

        case cResetAllEvents:
            trackMouse = FALSE;
            trackMouseExited = FALSE;
            trackMouseClicked = FALSE;
            trackMousePressed = FALSE;
            trackMouseReleased = FALSE;
            trackFocus = FALSE;
            trackFocusLost = FALSE;
            trackCaret = FALSE;
            trackShutdown = FALSE;

            trackMenuSelected = FALSE;
            trackMenuDeselected = FALSE;
            trackMenuCanceled = FALSE;

            trackPopupVisible = FALSE;
            trackPopupInvisible = FALSE;
            trackPopupCanceled = FALSE;

            trackPropertyNameChange = FALSE;
            trackPropertyDescriptionChange = FALSE;
            trackPropertyStateChange = FALSE;
            trackPropertyValueChange = FALSE;
            trackPropertySelectionChange = FALSE;
            trackPropertyTextChange = FALSE;
            trackPropertyCaretChange = FALSE;
            trackPropertyVisibleDataChange = FALSE;
            trackPropertyChildChange = FALSE;
            trackPropertyActiveDescendentChange = FALSE;
            trackPropertyTableModelChange = FALSE;

            ApplyEventOptions(hWnd);

            return TRUE;

        case cGoToMessage:
            InitGoToMessageDialogBox(theInstance);
            break;

        case cClearMessageHistory:
            g_MessageHistory.clear();
            ::SetDlgItemText(theDialogWindow, cjaccessinspectorText, NULL);
            EnableMessageNavButtons();
            UpdateMessageNumber();
            break;
        }
        break;

    case DISPLAY_INFO_MESSAGE:
        echoMouseObject();
        return TRUE;

    case DISPLAY_HWND_INFO_MESSAGE:
        echoMouseHWNDObject();
        return TRUE;
    }

    return FALSE;
}

#define SaveOptionToRegistry(optionVar) { \
    SetValue = RegSetValueEx( hKey, #optionVar, 0, REG_DWORD, \
                              (LPBYTE)(&optionVar), sizeof(DWORD)); \
    if ( ERROR_SUCCESS != SetValue ) { \
        ++ failureCount; \
    } \
}

BOOL SaveActiveEventOptionsToRegistry() {
    LONG CreateKey = ERROR_SUCCESS;
    HKEY hKey = NULL;
    DWORD Disposition = 0;

    CreateKey = ::RegCreateKeyEx( HKEY_CURRENT_USER,
                                  jaccessinspectorOptionsRegistryKey, 0, 0, 0,
                                  KEY_READ|KEY_WRITE, 0, &hKey, &Disposition );
    if ( ( ERROR_SUCCESS != CreateKey ) || ( NULL == hKey ) ) {
        return FALSE;
    }

    LONG SetValue = ERROR_SUCCESS;
    unsigned long failureCount = 0;

    SaveOptionToRegistry(trackMouse);
    SaveOptionToRegistry(trackMouseExited);
    SaveOptionToRegistry(trackMouseClicked);
    SaveOptionToRegistry(trackMousePressed);
    SaveOptionToRegistry(trackMouseReleased);
    SaveOptionToRegistry(trackShutdown);
    SaveOptionToRegistry(trackFocus);
    SaveOptionToRegistry(trackFocusLost);
    SaveOptionToRegistry(trackCaret);
    SaveOptionToRegistry(trackMenuSelected);
    SaveOptionToRegistry(trackMenuDeselected);
    SaveOptionToRegistry(trackMenuCanceled);
    SaveOptionToRegistry(trackPopupVisible);
    SaveOptionToRegistry(trackPopupInvisible);
    SaveOptionToRegistry(trackPopupCanceled);
    SaveOptionToRegistry(trackPropertyNameChange);
    SaveOptionToRegistry(trackPropertyDescriptionChange);
    SaveOptionToRegistry(trackPropertyStateChange);
    SaveOptionToRegistry(trackPropertyValueChange);
    SaveOptionToRegistry(trackPropertySelectionChange);
    SaveOptionToRegistry(trackPropertyTextChange);
    SaveOptionToRegistry(trackPropertyCaretChange);
    SaveOptionToRegistry(trackPropertyVisibleDataChange);
    SaveOptionToRegistry(trackPropertyChildChange);
    SaveOptionToRegistry(trackPropertyActiveDescendentChange);
    SaveOptionToRegistry(trackPropertyTableModelChange);

    ::RegFlushKey(hKey);
    ::RegCloseKey(hKey);

    if ( 0 == failureCount ) {
        return TRUE;
    }
    return FALSE;
}

#define ReadOptionFromRegistry(optionVar) { \
    Type = Value = 0; \
    ValueSize = sizeof(DWORD); \
    QueryValue = ::RegQueryValueEx( hKey, #optionVar, NULL, &Type, \
                                   (LPBYTE)(&Value), &ValueSize); \
    if ( ( ERROR_SUCCESS == QueryValue ) && ( REG_DWORD == Type ) ) { \
        optionVar = static_cast<BOOL>(Value); \
    } else { \
        ++ failureCount; \
    } \
}

BOOL ReadActiveEventOptionsFromRegistry() {

    trackMouse = FALSE;
    trackMouseExited = FALSE;
    trackMouseClicked = FALSE;
    trackMousePressed = FALSE;
    trackMouseReleased = FALSE;

    trackShutdown = FALSE;
    trackFocus = FALSE;
    trackFocusLost = FALSE;
    trackCaret = FALSE;
    trackMenuSelected = FALSE;
    trackMenuDeselected = FALSE;
    trackMenuCanceled = FALSE;
    trackPopupVisible = FALSE;
    trackPopupInvisible = FALSE;
    trackPopupCanceled = FALSE;

    trackPropertyNameChange = FALSE;
    trackPropertyDescriptionChange = FALSE;
    trackPropertyStateChange = FALSE;
    trackPropertyValueChange = FALSE;
    trackPropertySelectionChange = FALSE;
    trackPropertyTextChange = FALSE;
    trackPropertyCaretChange = FALSE;
    trackPropertyVisibleDataChange = FALSE;
    trackPropertyChildChange = FALSE;
    trackPropertyActiveDescendentChange = FALSE;
    trackPropertyTableModelChange = FALSE;

    LONG OpenKey = ERROR_SUCCESS;
    HKEY hKey = NULL;
    OpenKey = ::RegOpenKeyEx( HKEY_CURRENT_USER,
                              jaccessinspectorOptionsRegistryKey, 0,
                              KEY_READ, &hKey );
    if ( ( ERROR_SUCCESS != OpenKey ) || ( NULL == hKey ) ) {
        return FALSE;
    }

    LONG QueryValue = ERROR_SUCCESS;
    unsigned long failureCount = 0;
    DWORD Type, Value, ValueSize;

    ReadOptionFromRegistry(trackMouse);
    ReadOptionFromRegistry(trackMouseExited);
    ReadOptionFromRegistry(trackMouseClicked);
    ReadOptionFromRegistry(trackMousePressed);
    ReadOptionFromRegistry(trackMouseReleased);
    ReadOptionFromRegistry(trackShutdown);
    ReadOptionFromRegistry(trackFocus);
    ReadOptionFromRegistry(trackFocusLost);
    ReadOptionFromRegistry(trackCaret);
    ReadOptionFromRegistry(trackMenuSelected);
    ReadOptionFromRegistry(trackMenuDeselected);
    ReadOptionFromRegistry(trackMenuCanceled);
    ReadOptionFromRegistry(trackPopupVisible);
    ReadOptionFromRegistry(trackPopupInvisible);
    ReadOptionFromRegistry(trackPopupCanceled);
    ReadOptionFromRegistry(trackPropertyNameChange);
    ReadOptionFromRegistry(trackPropertyDescriptionChange);
    ReadOptionFromRegistry(trackPropertyStateChange);
    ReadOptionFromRegistry(trackPropertyValueChange);
    ReadOptionFromRegistry(trackPropertySelectionChange);
    ReadOptionFromRegistry(trackPropertyTextChange);
    ReadOptionFromRegistry(trackPropertyCaretChange);
    ReadOptionFromRegistry(trackPropertyVisibleDataChange);
    ReadOptionFromRegistry(trackPropertyChildChange);
    ReadOptionFromRegistry(trackPropertyActiveDescendentChange);
    ReadOptionFromRegistry(trackPropertyTableModelChange);

    ::RegCloseKey(hKey);

    if ( 0 == failureCount ) {
        return TRUE;
    }
    return FALSE;
}

#define APPLY_EVENT_OPTION(menuItem, optionVar, setFP, handler) \
{ \
    if ( optionVar ) { \
        ::CheckMenuItem(menu, menuItem, MF_BYCOMMAND | MF_CHECKED); \
        setFP (handler); \
    } else { \
        ::CheckMenuItem(menu, menuItem, MF_BYCOMMAND | MF_UNCHECKED); \
        setFP (NULL); \
    } \
}

void ApplyEventOptions (HWND hWnd) {

    HMENU menu = ::GetMenu (hWnd);
    APPLY_EVENT_OPTION( cTrackMouseMenuItem, trackMouse, SetMouseEntered,
                        HandleMouseEntered );
    APPLY_EVENT_OPTION( cTrackMouseExitedMenuItem, trackMouseExited,
                        SetMouseExited, HandleMouseExited );
    APPLY_EVENT_OPTION( cTrackMouseClickedMenuItem, trackMouseClicked,
                        SetMouseClicked, HandleMouseClicked );
    APPLY_EVENT_OPTION( cTrackMousePressedMenuItem, trackMousePressed,
                        SetMousePressed, HandleMousePressed );
    APPLY_EVENT_OPTION( cTrackMouseReleasedMenuItem, trackMouseReleased,
                        SetMouseReleased, HandleMouseReleased );

    APPLY_EVENT_OPTION( cTrackShutdownMenuItem, trackShutdown, SetJavaShutdown,
                        HandleJavaShutdown );
    APPLY_EVENT_OPTION( cTrackFocusMenuItem, trackFocus, SetFocusGained,
                        HandleJavaFocusGained );
    APPLY_EVENT_OPTION( cTrackFocusLostMenuItem, trackFocusLost, SetFocusLost,
                        HandleJavaFocusLost );
    APPLY_EVENT_OPTION( cTrackCaretMenuItem, trackCaret, SetCaretUpdate,
                        HandleJavaCaretUpdate );

    APPLY_EVENT_OPTION( cTrackMenuSelectedMenuItem, trackMenuSelected,
                        SetMenuSelected, HandleMenuSelected );
    APPLY_EVENT_OPTION( cTrackMenuDeselectedMenuItem, trackMenuDeselected,
                        SetMenuDeselected, HandleMenuDeselected );
    APPLY_EVENT_OPTION( cTrackMenuCanceledItem, trackMenuCanceled,
                        SetMenuCanceled, HandleMenuCanceled );

    APPLY_EVENT_OPTION( cTrackPopupBecomeVisibleMenuItem, trackPopupVisible,
                        SetPopupMenuWillBecomeVisible,
                        HandlePopupMenuWillBecomeVisible );
    APPLY_EVENT_OPTION( cTrackPopupBecomeInvisibleMenuItem, trackPopupInvisible,
                        SetPopupMenuWillBecomeInvisible,
                        HandlePopupMenuWillBecomeInvisible );
    APPLY_EVENT_OPTION( cTrackPopupCanceledItem, trackPopupCanceled,
                        SetPopupMenuCanceled, HandlePopupMenuCanceled );

    APPLY_EVENT_OPTION( cTrackPropertyNameItem, trackPropertyNameChange,
                        SetPropertyNameChange, HandlePropertyNameChange );
    APPLY_EVENT_OPTION( cTrackPropertyDescriptionItem,
                        trackPropertyDescriptionChange,
                        SetPropertyDescriptionChange,
                        HandlePropertyDescriptionChange );
    APPLY_EVENT_OPTION( cTrackPropertyStateItem, trackPropertyStateChange,
                        SetPropertyStateChange, HandlePropertyStateChange );
    APPLY_EVENT_OPTION( cTrackPropertyValueItem, trackPropertyValueChange,
                        SetPropertyValueChange, HandlePropertyValueChange );
    APPLY_EVENT_OPTION( cTrackPropertySelectionItem,
                        trackPropertySelectionChange,
                        SetPropertySelectionChange,
                        HandlePropertySelectionChange);
    APPLY_EVENT_OPTION( cTrackPropertyTextItem, trackPropertyTextChange,
                        SetPropertyTextChange, HandlePropertyTextChange );
    APPLY_EVENT_OPTION( cTrackPropertyCaretItem, trackPropertyCaretChange,
                        SetPropertyCaretChange, HandlePropertyCaretChange );
    APPLY_EVENT_OPTION( cTrackPropertyVisibleDataItem,
                        trackPropertyVisibleDataChange,
                        SetPropertyVisibleDataChange,
                        HandlePropertyVisibleDataChange );
    APPLY_EVENT_OPTION( cTrackPropertyChildItem, trackPropertyChildChange,
                        SetPropertyChildChange, HandlePropertyChildChange );
    APPLY_EVENT_OPTION( cTrackPropertyActiveDescendentItem,
                        trackPropertyActiveDescendentChange,
                        SetPropertyActiveDescendentChange,
                        HandlePropertyActiveDescendentChange );
    APPLY_EVENT_OPTION( cTrackPropertyTableModelChangeItem,
                        trackPropertyTableModelChange,
                        SetPropertyTableModelChange,
                        HandlePropertyTableModelChange );

    MaybeCheckMonitorTheSameEventsAsJAWS(menu);
    MaybeCheckMonitorAllEvents(menu);
}

BOOL EnableDlgItem(HWND hDlg, int nIDDlgItem, BOOL bEnable) {
    HWND dlgItem = ::GetDlgItem(hDlg, nIDDlgItem);
    if ( NULL == dlgItem ) {
        return FALSE;
    }
    return ::EnableWindow (dlgItem, bEnable);
}

void EnableMessageNavButtons() {
    HWND FocusWindow = ::GetFocus();
    int FocusCtrlID = ::GetDlgCtrlID(FocusWindow);
    BOOL DisabledFocusWindow = FALSE;
    if ( 0 == g_MessageHistory.GetMessageCount () ) {
        EnableDlgItem(theDialogWindow, cFirstMessage, FALSE);
        EnableDlgItem(theDialogWindow, cPreviousMessage, FALSE);
        EnableDlgItem(theDialogWindow, cMessageNumber, FALSE);
        EnableDlgItem(theDialogWindow, cNextMessage, FALSE);
        EnableDlgItem(theDialogWindow, cLastMessage, FALSE);
    } else if ( g_MessageHistory.IsFirstMessage () ) {
        EnableDlgItem(theDialogWindow, cFirstMessage, FALSE);
        EnableDlgItem(theDialogWindow, cPreviousMessage, FALSE);
        EnableDlgItem(theDialogWindow, cMessageNumber, TRUE);
        EnableDlgItem(theDialogWindow, cNextMessage, TRUE);
        EnableDlgItem(theDialogWindow, cLastMessage, TRUE);
        if ( ( cFirstMessage == FocusCtrlID ) ||
             ( cPreviousMessage == FocusCtrlID ) ) {
            DisabledFocusWindow = TRUE;
        }
    } else if ( g_MessageHistory.IsLastMessage () ) {
        EnableDlgItem(theDialogWindow, cFirstMessage, TRUE);
        EnableDlgItem(theDialogWindow, cPreviousMessage, TRUE);
        EnableDlgItem(theDialogWindow, cMessageNumber, TRUE);
        EnableDlgItem(theDialogWindow, cNextMessage, FALSE);
        EnableDlgItem(theDialogWindow, cLastMessage, FALSE);

        if ( ( cNextMessage == FocusCtrlID ) ||
             ( cLastMessage == FocusCtrlID ) ) {
            DisabledFocusWindow = TRUE;
        }
    } else {
        EnableDlgItem(theDialogWindow, cFirstMessage, TRUE);
        EnableDlgItem(theDialogWindow, cPreviousMessage, TRUE);
        EnableDlgItem(theDialogWindow, cMessageNumber, TRUE);
        EnableDlgItem(theDialogWindow, cNextMessage, TRUE);
        EnableDlgItem(theDialogWindow, cLastMessage, TRUE);
    }

    if ( DisabledFocusWindow ) {
        /*
        We just disabled the window that had the focus.  Set focus to the
        cjaccessinspectorText window.  Otherwise it will no longer be possible
        to tab through the controls in jaccessinspector.
        */
        HWND jaccessinspectorText =
            ::GetDlgItem(theDialogWindow, cjaccessinspectorText);
        if ( jaccessinspectorText ) {
            ::SetFocus(jaccessinspectorText);
        }
    }
}

void WINAPI AddToMessageHistory(const char * message) {
    g_MessageHistory.AddMessage(message);
    EnableMessageNavButtons();
    UpdateMessageNumber();
}

BOOL UpdateMessageNumber () {
    HWND dlgItem = ::GetDlgItem(theDialogWindow, cMessageNumber);
    if ( NULL == dlgItem ) {
        return FALSE;
    }

    size_t messageCount = g_MessageHistory.GetMessageCount();
    size_t messageNumber = g_MessageHistory.GetCurrentMessageIndex() + 1;
    char text [32] = {0};
    if ( 0 != messageCount ) {
        ::_snprintf(text, sizeof(text), "%d of %d", (int)messageNumber,
                                                    (int) messageCount);
    }
    return ::SetWindowText(dlgItem, text);
}

INT_PTR CALLBACK GoToMessageDialogProc( HWND hDlg, UINT message, WPARAM wParam,
                                        LPARAM lParam ) {
    BOOL ret_val = FALSE;
    switch ( message ) {
    case WM_INITDIALOG:
        {
            /*
            This code to center the Go To Message dialog box in the
            jaccessinspector window was taken from
            <http://msdn.microsoft.com/en-us/library/ms644996(VS.85).aspx>.
            */
            HWND hwndOwner = NULL;
            RECT rcOwner = { 0, 0, 0, 0 };
            RECT rc = { 0, 0, 0, 0 };
            RECT rcDlg = { 0, 0, 0, 0 };

            // Get the owner window and dialog box rectangles.
            if ( NULL == (hwndOwner = GetParent(hDlg)) ) {
                hwndOwner = GetDesktopWindow();
            }

            GetWindowRect(hwndOwner, &rcOwner);
            GetWindowRect(hDlg, &rcDlg);
            CopyRect(&rc, &rcOwner);

            // Offset the owner and dialog box rectangles so that right and
            // bottom values represent the width and height, and then offset
            // the owner again to discard space taken up by the dialog box.
            OffsetRect(&rcDlg, -rcDlg.left, -rcDlg.top);
            OffsetRect(&rc, -rc.left, -rc.top);
            OffsetRect(&rc, -rcDlg.right, -rcDlg.bottom);

            // The new position is the sum of half the remaining space and the
            // owner's original position.
            SetWindowPos (hDlg,
                HWND_TOP,
                rcOwner.left + (rc.right / 2),
                rcOwner.top + (rc.bottom / 2),
                0, 0,  // Ignores size arguments.
                SWP_NOSIZE);
        }
        break;

    case WM_COMMAND:
        switch ( LOWORD (wParam) ) {
        case IDOK:
            {
                size_t GoToMessageNumber = 0;
                BOOL Translated = FALSE;
                GoToMessageNumber = GetDlgItemInt( hDlg, IDC_MESSAGE_NUMBER_EDIT,
                                                   &Translated, FALSE );
                EndDialog (hDlg, IDOK);
                theGoToDialogWindow = NULL;

                if ( ( Translated ) && ( GoToMessageNumber > 0 ) ) {
                    const char * messageText = NULL;
                    if ( (GoToMessageNumber - 1) <
                         g_MessageHistory.GetMessageCount() ) {
                        messageText =
                            g_MessageHistory.GetMessage(GoToMessageNumber - 1);
                    } else if ( (GoToMessageNumber - 1) >=
                                g_MessageHistory.GetMessageCount() ) {
                        messageText = g_MessageHistory.GetLastMessage();
                    }
                    if ( ( NULL != messageText ) && ( 0 != messageText [0] ) ) {
                        ::SetDlgItemText( theDialogWindow, cjaccessinspectorText,
                                          messageText );
                    }
                    EnableMessageNavButtons();
                    UpdateMessageNumber();
                }
            }
            break;
        case IDCANCEL:
            EndDialog(hDlg, IDCANCEL);
            theGoToDialogWindow = NULL;
            break;
        }
        break;
    }
    return ret_val;
}

BOOL InitGoToMessageDialogBox (HANDLE hInstance) {
    theGoToDialogWindow = CreateDialog (
        (struct HINSTANCE__ *)hInstance, MAKEINTRESOURCE(IDD_GO_TO_MESSAGE),
        theDialogWindow, GoToMessageDialogProc);

    if ( NULL == theGoToDialogWindow ) {
        return FALSE;
    }

    ShowWindow (theGoToDialogWindow, SW_SHOW);
    return TRUE;
}

BOOL ShouldCheckMonitorTheSameEventsAsJAWS () {
    if (
        ( FALSE == trackMouse )
        && ( FALSE == trackMouseExited )
        && ( FALSE == trackMouseClicked )
        && ( FALSE == trackMousePressed )
        && ( FALSE == trackMouseReleased )
        && ( TRUE == trackFocus )
        && ( TRUE == trackFocusLost )
        && ( FALSE == trackCaret )
        && ( FALSE == trackShutdown )
        && ( FALSE == trackMenuSelected )
        && ( FALSE == trackMenuDeselected )
        && ( FALSE == trackMenuCanceled )
        && ( FALSE == trackPopupVisible )
        && ( FALSE == trackPopupInvisible )
        && ( FALSE == trackPopupCanceled )
        && ( TRUE == trackPropertyNameChange )
        && ( TRUE == trackPropertyDescriptionChange )
        && ( TRUE == trackPropertyStateChange )
        && ( TRUE == trackPropertyValueChange )
        && ( TRUE == trackPropertySelectionChange )
        && ( TRUE == trackPropertyTextChange )
        && ( TRUE == trackPropertyCaretChange )
        && ( FALSE == trackPropertyVisibleDataChange )
        && ( TRUE == trackPropertyChildChange )
        && ( TRUE == trackPropertyActiveDescendentChange )
        && ( FALSE == trackPropertyTableModelChange )
        )
    {
        return TRUE;
    }

    return FALSE;
}

void MaybeCheckMonitorTheSameEventsAsJAWS(HMENU menu) {
    UINT uCheck = MF_BYCOMMAND | MF_UNCHECKED;
    if ( ShouldCheckMonitorTheSameEventsAsJAWS() ) {
        uCheck = MF_BYCOMMAND | MF_CHECKED;
    }
    ::CheckMenuItem(menu, cMonitorTheSameEventsAsJAWS, uCheck);
}

BOOL ShouldCheckMonitorAllEvents() {
    if (
        ( TRUE == trackMouse )
        && ( TRUE == trackMouseExited )
        && ( TRUE == trackMouseClicked )
        && ( TRUE == trackMousePressed )
        && ( TRUE == trackMouseReleased )
        && ( TRUE == trackFocus )
        && ( TRUE == trackFocusLost )
        && ( TRUE == trackCaret )
        && ( TRUE == trackShutdown )
        && ( TRUE == trackMenuSelected )
        && ( TRUE == trackMenuDeselected )
        && ( TRUE == trackMenuCanceled )
        && ( TRUE == trackPopupVisible )
        && ( TRUE == trackPopupInvisible )
        && ( TRUE == trackPopupCanceled )
        && ( TRUE == trackPropertyNameChange )
        && ( TRUE == trackPropertyDescriptionChange )
        && ( TRUE == trackPropertyStateChange )
        && ( TRUE == trackPropertyValueChange )
        && ( TRUE == trackPropertySelectionChange )
        && ( TRUE == trackPropertyTextChange )
        && ( TRUE == trackPropertyCaretChange )
        && ( TRUE == trackPropertyVisibleDataChange )
        && ( TRUE == trackPropertyChildChange )
        && ( TRUE == trackPropertyActiveDescendentChange )
        && ( TRUE == trackPropertyTableModelChange )
        )
    {
        return TRUE;
    }

    return FALSE;
}

void MaybeCheckMonitorAllEvents(HMENU menu) {
    UINT uCheck = MF_BYCOMMAND | MF_UNCHECKED;
    if ( ShouldCheckMonitorAllEvents() ) {
        uCheck = MF_BYCOMMAND | MF_CHECKED;
    }
    ::CheckMenuItem(menu, cMonitorAllEvents, uCheck);
}
