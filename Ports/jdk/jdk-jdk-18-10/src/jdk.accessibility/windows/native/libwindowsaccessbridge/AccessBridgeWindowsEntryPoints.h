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
 * Glue routines called by Windows AT into the WindowsAccessBridge dll
 */

#ifndef __AccessBridgeWindowsEntryPoints_H__
#define __AccessBridgeWindowsEntryPoints_H__

#include <windows.h>
#include <jni.h>

#include "AccessBridgePackages.h"
#include "AccessBridgeCallbacks.h"

#ifdef __cplusplus
extern "C" {
#endif

    void Windows_run();

    void releaseJavaObject(long vmID, JOBJECT64 object);
    void getVersionInfo(long vmID, AccessBridgeVersionInfo *info);

    // Window related functions
    HWND getTopLevelHWND(HWND descendent);
    BOOL isJavaWindow(HWND window);
    BOOL getAccessibleContextFromHWND(HWND window, long *vmID, JOBJECT64 *AccessibleContext);
    HWND getHWNDFromAccessibleContext(long vmID, JOBJECT64 accessibleContext);

    // returns whether two objects are the same
    BOOL isSameObject(long vmID, JOBJECT64 obj1, JOBJECT64 obj2);

    // Accessible Context functions
    BOOL getAccessibleContextAt(long vmID, JOBJECT64 AccessibleContextParent,
                                jint x, jint y, JOBJECT64 *AccessibleContext);
    BOOL getAccessibleContextWithFocus(HWND window, long *vmID, JOBJECT64 *AccessibleContext);
    BOOL getAccessibleContextInfo(long vmID, JOBJECT64 AccessibleContext, AccessibleContextInfo *info);
    JOBJECT64 getAccessibleChildFromContext(long vmID, JOBJECT64 AccessibleContext, jint childIndex);
    JOBJECT64 getAccessibleParentFromContext(long vmID, JOBJECT64 AccessibleContext);

    /* begin AccessibleTable */
    BOOL getAccessibleTableInfo(long vmID, JOBJECT64 acParent, AccessibleTableInfo *tableInfo);
    BOOL getAccessibleTableCellInfo(long vmID, JOBJECT64 accessibleTable, jint row, jint column,
                                    AccessibleTableCellInfo *tableCellInfo);

    BOOL getAccessibleTableRowHeader(long vmID, JOBJECT64 acParent, AccessibleTableInfo *tableInfo);
    BOOL getAccessibleTableColumnHeader(long vmID, JOBJECT64 acParent, AccessibleTableInfo *tableInfo);

    JOBJECT64 getAccessibleTableRowDescription(long vmID, JOBJECT64 acParent, jint row);
    JOBJECT64 getAccessibleTableColumnDescription(long vmID, JOBJECT64 acParent, jint column);

    jint getAccessibleTableRowSelectionCount(long vmID, JOBJECT64 accessibleTable);
    BOOL isAccessibleTableRowSelected(long vmID, JOBJECT64 accessibleTable, jint row);
    BOOL getAccessibleTableRowSelections(long vmID, JOBJECT64 accessibleTable, jint count,
                                         jint *selections);

    jint getAccessibleTableColumnSelectionCount(long vmID, JOBJECT64 accessibleTable);
    BOOL isAccessibleTableColumnSelected(long vmID, JOBJECT64 accessibleTable, jint column);
    BOOL getAccessibleTableColumnSelections(long vmID, JOBJECT64 accessibleTable, jint count,
                                            jint *selections);

    jint getAccessibleTableRow(long vmID, JOBJECT64 accessibleTable, jint index);
    jint getAccessibleTableColumn(long vmID, JOBJECT64 accessibleTable, jint index);
    jint getAccessibleTableIndex(long vmID, JOBJECT64 accessibleTable, jint row, jint column);

    /* end AccessibleTable */

    BOOL getAccessibleRelationSet(long vmID, JOBJECT64 accessibleContext,
                                  AccessibleRelationSetInfo *relationSetInfo);

    // AccessibleHypertext methods
    BOOL getAccessibleHypertext(long vmID, JOBJECT64 accessibleContext, AccessibleHypertextInfo *hypertextInfo);

    BOOL activateAccessibleHyperlink(long vmID, JOBJECT64 accessibleContext, JOBJECT64 accessibleHyperlink);

    jint getAccessibleHyperlinkCount(const long vmID,
                                     const AccessibleContext accessibleContext);

    BOOL getAccessibleHypertextExt(const long vmID,
                                   const AccessibleContext accessibleContext,
                                   const jint nStartIndex,
                                   /* OUT */ AccessibleHypertextInfo *hypertextInfo);

    jint getAccessibleHypertextLinkIndex(const long vmID,
                                         const AccessibleHypertext hypertext,
                                         const jint nIndex);

    BOOL getAccessibleHyperlink(const long vmID,
                                const AccessibleHypertext hypertext,
                                const jint nIndex,
                                /* OUT */ AccessibleHyperlinkInfo *hyperlinkInfo);


    /* Accessible KeyBindings, Icons and Actions */
    BOOL getAccessibleKeyBindings(long vmID, JOBJECT64 accessibleContext,
                                  AccessibleKeyBindings *keyBindings);

    BOOL getAccessibleIcons(long vmID, JOBJECT64 accessibleContext,
                            AccessibleIcons *icons);

    BOOL getAccessibleActions(long vmID, JOBJECT64 accessibleContext,
                              AccessibleActions *actions);

    BOOL doAccessibleActions(long vmID, JOBJECT64 accessibleContext,
                             AccessibleActionsToDo *actionsToDo, jint *failure);

    /* ----- Additional AccessibleHypertext methods for Teton */


    jint getAccessibleHypertextLinkCount(const long vmID,
                                         const AccessibleContext accessibleContext);

    BOOL getAccessibleHypertextExt(const long vmID,
                                   const AccessibleContext accessibleContext,
                                   const jint nStartIndex,
                                   /* OUT */ AccessibleHypertextInfo *hypertextInfo);

    jint getAccessibleHypertextLinkIndex(const long vmID,
                                         const AccessibleContext accessibleContext,
                                         const jint nIndex);

    BOOL getAccessibleHyperlink(const long vmID,
                                const AccessibleContext accessibleContext,
                                const jint nIndex,
                                /* OUT */ AccessibleHyperlinkInfo *hyperlinkInfo);


    /* Additional utility methods */
    BOOL setTextContents (const long vmID, const AccessibleContext accessibleContext, const wchar_t *text);

    AccessibleContext getParentWithRole (const long vmID, const AccessibleContext accessibleContext, const wchar_t *role);

    AccessibleContext getTopLevelObject (const long vmID, const AccessibleContext accessibleContext);

    AccessibleContext getParentWithRoleElseRoot (const long vmID, const AccessibleContext accessibleContext, const wchar_t *role);

    int getObjectDepth (const long vmID, const AccessibleContext accessibleContext);

    AccessibleContext getActiveDescendent (const long vmID, const AccessibleContext accessibleContext);

    /**
     * Additional methods for Teton
     */

    /**
     * Gets the AccessibleName for a component based upon the JAWS algorithm. Returns
     * whether successful.
     *
     * Bug ID 4916682 - Implement JAWS AccessibleName policy
     */
    BOOL getVirtualAccessibleName(long vmID, AccessibleContext accessibleContext, wchar_t *name, int len);

    /**
     * Request focus for a component. Returns whether successful;
     *
     * Bug ID 4944757 - requestFocus method needed
     */
    BOOL requestFocus(long vmID, AccessibleContext accessibleContext);

    /**
     * Selects text between two indices.  Selection includes the text at the start index
     * and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944758 - selectTextRange method needed
     */
    BOOL selectTextRange(long vmID, AccessibleContext accessibleContext, int startIndex, int endIndex);

    /**
     * Get text attributes between two indices.  The attribute list includes the text at the
     * start index and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944761 - getTextAttributes between two indices method needed
     */
    BOOL getTextAttributesInRange(long vmID, AccessibleContext accessibleContext, int startIndex, int endIndex,
                                  AccessibleTextAttributesInfo *attributes, short *len);

    /**
     * Returns the number of visible children of a component. Returns -1 on error.
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    int getVisibleChildrenCount(long vmID, AccessibleContext accessibleContext);

    /**
     * Gets the visible children of an AccessibleContext. Returns whether successful;
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    BOOL getVisibleChildren(long vmID, AccessibleContext accessibleContext, int startIndex,
                            VisibleChildrenInfo *visibleChildrenInfo);

    /**
     * Set the caret to a text position. Returns whether successful;
     *
     * Bug ID 4944770 - setCaretPosition method needed
     */
    BOOL setCaretPosition(long vmID, AccessibleContext accessibleContext, int position);

    /**
     * Gets the text caret bounding rectangle
     */
    BOOL getCaretLocation(long vmID, JOBJECT64 AccessibleContext, AccessibleTextRectInfo *rectInfo, jint index);

    // Accessible Text functions
    BOOL getAccessibleTextInfo(long vmID, JOBJECT64 AccessibleContext, AccessibleTextInfo *textInfo, jint x, jint y);
    BOOL getAccessibleTextItems(long vmID, JOBJECT64 AccessibleContext, AccessibleTextItemsInfo *textItems, jint index);
    BOOL getAccessibleTextSelectionInfo(long vmID, JOBJECT64 AccessibleContext, AccessibleTextSelectionInfo *selectionInfo);
    BOOL getAccessibleTextAttributes(long vmID, JOBJECT64 AccessibleContext, jint index, AccessibleTextAttributesInfo *attributes);
    BOOL getAccessibleTextRect(long vmID, JOBJECT64 AccessibleContext, AccessibleTextRectInfo *rectInfo, jint index);
    BOOL getAccessibleTextLineBounds(long vmID, JOBJECT64 AccessibleContext, jint index, jint *startIndex, jint *endIndex);
    BOOL getAccessibleTextRange(long vmID, JOBJECT64 AccessibleContext, jint start, jint end, wchar_t *text, short len);

    // Accessible Value methods
    BOOL getCurrentAccessibleValueFromContext(long vmID,JOBJECT64 AccessibleContext, wchar_t *value, short len);
    BOOL getMaximumAccessibleValueFromContext(long vmID,JOBJECT64 AccessibleContext, wchar_t *value, short len);
    BOOL getMinimumAccessibleValueFromContext(long vmID,JOBJECT64 AccessibleContext, wchar_t *value, short len);

    // Accessible Selection methods
    void addAccessibleSelectionFromContext(long vmID,JOBJECT64 AccessibleContext, int i);
    void clearAccessibleSelectionFromContext(long vmID,JOBJECT64 AccessibleContext);
    JOBJECT64 getAccessibleSelectionFromContext(long vmID,JOBJECT64 AccessibleContext, int i);
    int getAccessibleSelectionCountFromContext(long vmID,JOBJECT64 AccessibleContext);
    BOOL isAccessibleChildSelectedFromContext(long vmID,JOBJECT64 AccessibleContext, int i);
    void removeAccessibleSelectionFromContext(long vmID,JOBJECT64 AccessibleContext, int i);
    void selectAllAccessibleSelectionFromContext(long vmID,JOBJECT64 AccessibleContext);


    // PropertyChange Event registry routines
    void setPropertyChangeFP(AccessBridge_PropertyChangeFP fp);

    // Java application shutdown
    void setJavaShutdownFP(AccessBridge_JavaShutdownFP fp);

    // Focus Event registry routines
    void setFocusGainedFP(AccessBridge_FocusGainedFP fp);
    void setFocusLostFP(AccessBridge_FocusLostFP fp);

    // Caret Event registry routines
    void setCaretUpdateFP(AccessBridge_CaretUpdateFP fp);

    // Mouse Event registry routines
    void setMouseClickedFP(AccessBridge_MouseClickedFP fp);
    void setMouseEnteredFP(AccessBridge_MouseEnteredFP fp);
    void setMouseExitedFP(AccessBridge_MouseExitedFP fp);
    void setMousePressedFP(AccessBridge_MousePressedFP fp);
    void setMouseReleasedFP(AccessBridge_MouseReleasedFP fp);

    // Menu/PopupMenu Event registry routines
    void setMenuCanceledFP(AccessBridge_MenuCanceledFP fp);
    void setMenuDeselectedFP(AccessBridge_MenuDeselectedFP fp);
    void setMenuSelectedFP(AccessBridge_MenuSelectedFP fp);
    void setPopupMenuCanceledFP(AccessBridge_PopupMenuCanceledFP fp);
    void setPopupMenuWillBecomeInvisibleFP(AccessBridge_PopupMenuWillBecomeInvisibleFP fp);
    void setPopupMenuWillBecomeVisibleFP(AccessBridge_PopupMenuWillBecomeVisibleFP fp);

    // Accessibility PropertyChange Event registry routines
    void setPropertyNameChangeFP(AccessBridge_PropertyNameChangeFP fp);
    void setPropertyDescriptionChangeFP(AccessBridge_PropertyDescriptionChangeFP fp);
    void setPropertyStateChangeFP(AccessBridge_PropertyStateChangeFP fp);
    void setPropertyValueChangeFP(AccessBridge_PropertyValueChangeFP fp);
    void setPropertySelectionChangeFP(AccessBridge_PropertySelectionChangeFP fp);
    void setPropertyTextChangeFP(AccessBridge_PropertyTextChangeFP fp);
    void setPropertyCaretChangeFP(AccessBridge_PropertyCaretChangeFP fp);
    void setPropertyVisibleDataChangeFP(AccessBridge_PropertyVisibleDataChangeFP fp);
    void setPropertyChildChangeFP(AccessBridge_PropertyChildChangeFP fp);
    void setPropertyActiveDescendentChangeFP(AccessBridge_PropertyActiveDescendentChangeFP fp);

    void setPropertyTableModelChangeFP(AccessBridge_PropertyTableModelChangeFP fp);



#ifdef __cplusplus
}
#endif

#endif
