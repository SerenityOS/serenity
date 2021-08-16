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

#define JACCESSINSPECTOR_LOG "jaccessinspector.log"

void CALLBACK TimerProc(HWND hWnd, UINT uTimerMsg, UINT uTimerID, DWORD dwTime);
LRESULT CALLBACK KeyboardProc(int code, WPARAM wParam, LPARAM lParam);
LRESULT CALLBACK MouseProc(int code, WPARAM wParam, LPARAM lParam);

BOOL InitWindow(HANDLE hInstance);
INT_PTR CALLBACK jaccessinspectorDialogProc( HWND hDlg, UINT message,
                                             WPARAM wParam, LPARAM lParam );

void HandleJavaPropertyChange( long vmID, PropertyChangeEvent event,
                               AccessibleContext ac,
                               wchar_t *name, wchar_t *oldValue,
                               wchar_t *newValue );

void HandleJavaShutdown(long vmID);
void HandleJavaFocusGained(long vmID, FocusEvent event, AccessibleContext ac);
void HandleJavaFocusLost(long vmID, FocusEvent event, AccessibleContext ac);

void HandleJavaCaretUpdate(long vmID, CaretEvent event, AccessibleContext ac);

void HandleMouseClicked(long vmID, MouseEvent event, AccessibleContext ac);
void HandleMouseEntered(long vmID, MouseEvent event, AccessibleContext ac);
void HandleMouseExited(long vmID, MouseEvent event, AccessibleContext ac);
void HandleMousePressed(long vmID, MouseEvent event, AccessibleContext ac);
void HandleMouseReleased(long vmID, MouseEvent event, AccessibleContext ac);

void HandleMenuCanceled(long vmID, MenuEvent event, AccessibleContext ac);
void HandleMenuDeselected(long vmID, MenuEvent event, AccessibleContext ac);
void HandleMenuSelected(long vmID, MenuEvent event, AccessibleContext ac);
void HandlePopupMenuCanceled(long vmID, MenuEvent event, AccessibleContext ac);
void HandlePopupMenuWillBecomeInvisible( long vmID, MenuEvent event,
                                         AccessibleContext ac );
void HandlePopupMenuWillBecomeVisible( long vmID, MenuEvent event,
                                       AccessibleContext ac );

void HandlePropertyNameChange( long vmID, PropertyChangeEvent event,
                               AccessibleContext ac,
                               wchar_t *oldName, wchar_t *newName);
void HandlePropertyDescriptionChange( long vmID, PropertyChangeEvent event,
                                      AccessibleContext ac,
                                      wchar_t *oldDescription,
                                      wchar_t *newDescription );
void HandlePropertyStateChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                wchar_t *oldState, wchar_t *newState );
void HandlePropertyValueChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                wchar_t *oldValue, wchar_t *newValue );
void HandlePropertySelectionChange( long vmID, PropertyChangeEvent event,
                                    AccessibleContext ac );
void HandlePropertyTextChange( long vmID, PropertyChangeEvent event,
                               AccessibleContext ac );
void HandlePropertyCaretChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                int oldPosition, int newPosition );
void HandlePropertyVisibleDataChange( long vmID, PropertyChangeEvent event,
                                      AccessibleContext ac );
void HandlePropertyChildChange( long vmID, PropertyChangeEvent event,
                                AccessibleContext ac,
                                jobject oldChild, jobject newChild );
void HandlePropertyActiveDescendentChange(long vmID, PropertyChangeEvent event,
                                          AccessibleContext ac,
                                          jobject oldActiveDescendent,
                                          jobject newActiveDescendent);

void HandlePropertyTableModelChange( long vmID, PropertyChangeEvent event,
                                     AccessibleContext ac,
                                     wchar_t *oldValue, wchar_t *newValue );

char *getAccessibleInfo( long vmID, AccessibleContext ac,
                         int x, int y, char *buffer, int bufsize );

char *getTimeAndDate();
void displayAndLogText(char *buffer, ...);

const char * const jaccessinspectorOptionsRegistryKey =
    "Software\\JavaSoft\\Java Development Kit\\jaccessinspector";
BOOL SaveActiveEventOptionsToRegistry();
BOOL ReadActiveEventOptionsFromRegistry();
void ApplyEventOptions(HWND hWnd);
BOOL EnableDlgItem(HWND hDlg, int nIDDlgItem, BOOL bEnable);
void EnableMessageNavButtons();
void WINAPI AddToMessageHistory(const char * message);
BOOL UpdateMessageNumber();
INT_PTR CALLBACK GoToMessageDialogProc( HWND hDlg, UINT message, WPARAM wParam,
                                        LPARAM lParam );
BOOL InitGoToMessageDialogBox(HANDLE hInstance);
BOOL ShouldCheckMonitorTheSameEventsAsJAWS();
void MaybeCheckMonitorTheSameEventsAsJAWS(HMENU menu);
BOOL ShouldCheckMonitorAllEvents();
void MaybeCheckMonitorAllEvents(HMENU menu);
