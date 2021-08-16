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

#include "AccessBridgeDebug.h"
#include "AccessBridgeWindowsEntryPoints.h"
#include "WinAccessBridge.h"
#include "accessBridgeResource.h"

#include <windows.h>
#include <jni.h>


extern WinAccessBridge *theWindowsAccessBridge;
extern HWND theDialogWindow;

#ifdef __cplusplus
extern "C" {
#endif

    /**
     * Windows_run - where Windows executables will load/unload us
     *
     */
    void Windows_run() {
        // open our window
        if (theWindowsAccessBridge != (WinAccessBridge *) 0) {
            theWindowsAccessBridge->initWindow();
        }
    }

    /*
      /**
      * Windows_shutdown - where Windows executables will load/unload us
      *
      *
      void Windows_shutdown() {
      if (theWindowsAccessBridge != (WinAccessBridge *) 0) {
      theWindowsAccessBridge->initWindow();
      }
      }
    */

    /**
     * getTopLevelHWND - returns the top-level window parent of the descendent
     *
     */
    HWND getTopLevelHWND(HWND descendent) {
        HWND hwnd;
        if (descendent == NULL) {
            return NULL;
        }

        if (!IsWindow(descendent)) {
            return NULL;
        }

        hwnd = descendent;
        for(;;) {
            LONG style = GetWindowLong(hwnd, GWL_STYLE);
            if ( (style & WS_CHILD) == 0 ) {
                // found a non-child window so terminate
                break;
            }
            hwnd = GetParent(hwnd);
        }

        return hwnd;
    }

    void releaseJavaObject(long vmID, JOBJECT64 object) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->releaseJavaObject(vmID, object);
        }
    }

    void getVersionInfo(long vmID, AccessBridgeVersionInfo *info) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->getVersionInfo(vmID, info);
        }
    }


    BOOL isJavaWindow(HWND window) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->isJavaWindow(window);
        }
        return FALSE;
    }

    /*
     * Returns whether two object references refer to the same object
     */
    BOOL isSameObject(long vmID, JOBJECT64 obj1, JOBJECT64 obj2) {
#ifdef ACCESSBRIDGE_ARCH_LEGACY // JOBJECT64 is jobject (32 bit pointer)
        PrintDebugString("\r\nAccessBridgeWindowsEntryPoints::isSameObject(%p %p)", obj1, obj2);
#else // JOBJECT64 is jlong (64 bit)
        PrintDebugString("\r\nAccessBridgeWindowsEntryPoints::isSameObject(%016I64X %016I64X)", obj1, obj2);
#endif
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->isSameObject(vmID, obj1, obj2);
        }
        return FALSE;
    }

    /**
     * Sets a text field to the specified string. Returns whether successful
     */
    BOOL setTextContents (const long vmID, const AccessibleContext accessibleContext,const wchar_t *text) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->setTextContents(vmID, accessibleContext, text);
        }
        return FALSE;
    }

    /**
     * Returns the Accessible Context of an object of the specified role that is the
     * ancestor of a given object.  If the object is of the specified role
     * or an ancestor object of the specified role was found, returns the object's
     * AccessibleContext.
     * If there is no ancestor object of the specified role,
     * returns (AccessibleContext)0.
     */
    AccessibleContext getParentWithRole (const long vmID, const AccessibleContext accessibleContext, const wchar_t *role) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getParentWithRole(vmID, accessibleContext, role);
        }
        return (AccessibleContext)0;
    }


    /**
     * Returns the Accessible Context for the top level object in
     * a Java Window.  This is same Accessible Context that is obtained
     * from GetAccessibleContextFromHWND for that window.  Returns
     * (AccessibleContext)0 on error.
     */
    AccessibleContext getTopLevelObject (const long vmID, const AccessibleContext accessibleContext) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getTopLevelObject(vmID, accessibleContext);
        }
        return (AccessibleContext)0;
    }

    /**
     * If there is an Ancestor object of the specified role,
     * returns the Accessible Context of the found object.
     * Otherwise, returns the top level object for that
     * Java Window.  Returns (AccessibleContext)0 on error.
     */
    AccessibleContext getParentWithRoleElseRoot (const long vmID, const AccessibleContext accessibleContext, const wchar_t *role) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getParentWithRoleElseRoot(vmID, accessibleContext, role);
        }
        return (AccessibleContext)0;
    }

    /**
     * Returns how deep in the object hierarchy a given object is.
     * The top most object in the object hierarchy has an object depth of 0.
     * Returns -1 on error.
     */
    int getObjectDepth (const long vmID, const AccessibleContext accessibleContext) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getObjectDepth(vmID, accessibleContext);
        }
        return -1;
    }

    /**
     * Returns the Accessible Context of the currently ActiveDescendent of an object.
     * Returns (AccessibleContext)0 on error.
     */
    AccessibleContext getActiveDescendent (const long vmID, const AccessibleContext accessibleContext) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getActiveDescendent(vmID, accessibleContext);
        }
        return (AccessibleContext)0;
    }

    // -------- Accessible Context methods -------------

    BOOL getAccessibleContextFromHWND(HWND window, long *vmID, JOBJECT64 *AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleContextFromHWND(window, vmID, AccessibleContext);
        }
        return FALSE;
    }

    HWND getHWNDFromAccessibleContext(long vmID, JOBJECT64 accessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getHWNDFromAccessibleContext(vmID, accessibleContext);
        }
        return (HWND)0;
    }

    BOOL getAccessibleContextAt(long vmID, JOBJECT64 AccessibleContextParent,
                                jint x, jint y, JOBJECT64 *AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleContextAt(vmID, AccessibleContextParent,
                                                                  x, y, AccessibleContext);
        }
        return FALSE;
    }

    BOOL getAccessibleContextWithFocus(HWND window, long *vmID, JOBJECT64 *AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleContextWithFocus(window, vmID, AccessibleContext);
        }
        return FALSE;
    }

    BOOL getAccessibleContextInfo(long vmID,
                                  JOBJECT64 AccessibleContext,
                                  AccessibleContextInfo *info) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleContextInfo(
                                                                    vmID,
                                                                    AccessibleContext,
                                                                    info);
        }
        return FALSE;
    }

    JOBJECT64 getAccessibleChildFromContext(long vmID,
                                          JOBJECT64 AccessibleContext,
                                          jint childIndex) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleChildFromContext(
                                                                         vmID,
                                                                         AccessibleContext,
                                                                         childIndex);
        }
        return (JOBJECT64) 0;
    }

    JOBJECT64 getAccessibleParentFromContext(long vmID,
                                           JOBJECT64 AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleParentFromContext(
                                                                          vmID,
                                                                          AccessibleContext);
        }
        return (JOBJECT64) 0;
    }

    // -------- begin AccessibleTable routines -------------

    BOOL getAccessibleTableInfo(long vmID, JOBJECT64 ac,
                                AccessibleTableInfo *tableInfo) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTableInfo(
                                                                  vmID,
                                                                  ac,
                                                                  tableInfo);
        }
        return FALSE;
    }

    BOOL getAccessibleTableCellInfo(long vmID, JOBJECT64 accessibleTable,
                                    jint row, jint column, AccessibleTableCellInfo *tableCellInfo) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTableCellInfo(
                                                                      vmID,
                                                                      accessibleTable,
                                                                      row, column, tableCellInfo);
        }
        return FALSE;
    }

    BOOL getAccessibleTableRowHeader(long vmID, JOBJECT64 acParent, AccessibleTableInfo *tableInfo) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTableRowHeader(
                                                                       vmID,
                                                                       acParent,
                                                                       tableInfo);
        }
        return FALSE;
    }

    BOOL getAccessibleTableColumnHeader(long vmID, JOBJECT64 acParent, AccessibleTableInfo *tableInfo) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTableColumnHeader(
                                                                          vmID,
                                                                          acParent,
                                                                          tableInfo);
        }
        return FALSE;
    }

    JOBJECT64 getAccessibleTableRowDescription(long vmID, JOBJECT64 acParent, jint row) {

        if (theWindowsAccessBridge != 0) {
            return (JOBJECT64)theWindowsAccessBridge->getAccessibleTableRowDescription(
                                                                            vmID,
                                                                            acParent,
                                                                            row);
        }
        return (JOBJECT64)0;
    }

    JOBJECT64 getAccessibleTableColumnDescription(long vmID, JOBJECT64 acParent, jint column) {

        if (theWindowsAccessBridge != 0) {
            return (JOBJECT64)theWindowsAccessBridge->getAccessibleTableColumnDescription(
                                                                               vmID,
                                                                               acParent,
                                                                               column);
        }
        return (JOBJECT64)0;
    }

    jint getAccessibleTableRowSelectionCount(long vmID, JOBJECT64 accessibleTable) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTableRowSelectionCount(vmID, accessibleTable);
        }
        return -1;
    }

    BOOL isAccessibleTableRowSelected(long vmID, JOBJECT64 accessibleTable, jint row) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->isAccessibleTableRowSelected(vmID, accessibleTable, row);
        }
        return FALSE;
    }

    BOOL getAccessibleTableRowSelections(long vmID, JOBJECT64 accessibleTable, jint count, jint *selections) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableRowSelections(vmID, accessibleTable, count,
                                                                           selections);
        }
        return FALSE;
    }


    jint getAccessibleTableColumnSelectionCount(long vmID, JOBJECT64 accessibleTable) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableColumnSelectionCount(vmID, accessibleTable);
        }
        return -1;
    }

    BOOL isAccessibleTableColumnSelected(long vmID, JOBJECT64 accessibleTable, jint column) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->isAccessibleTableColumnSelected(vmID, accessibleTable, column);
        }
        return FALSE;
    }

    BOOL getAccessibleTableColumnSelections(long vmID, JOBJECT64 accessibleTable, jint count, jint *selections) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableColumnSelections(vmID, accessibleTable, count,
                                                                              selections);
        }
        return FALSE;
    }

    jint getAccessibleTableRow(long vmID, JOBJECT64 accessibleTable, jint index) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableRow(vmID, accessibleTable, index);
        }
        return -1;
    }

    jint getAccessibleTableColumn(long vmID, JOBJECT64 accessibleTable, jint index) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableColumn(vmID, accessibleTable, index);
        }
        return -1;
    }

    jint getAccessibleTableIndex(long vmID, JOBJECT64 accessibleTable, jint row, jint column) {
        if (theWindowsAccessBridge != 0 ) {
            return theWindowsAccessBridge->getAccessibleTableIndex(vmID, accessibleTable, row, column);
        }
        return -1;
    }

    /* --------- end AccessibleTable routines ------- */

    // --------- AccessibleRelationSet methods

    BOOL getAccessibleRelationSet(long vmID, JOBJECT64 accessibleContext,
                                  AccessibleRelationSetInfo *relationSetInfo) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleRelationSet(vmID, accessibleContext, relationSetInfo);
        }
        return FALSE;
    }

    // --------- AccessibleHypertext methods

    BOOL getAccessibleHypertext(long vmID, JOBJECT64 accessibleContext,
                                AccessibleHypertextInfo *accessibleHypertextInfo) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleHypertext(vmID, accessibleContext,
                                                                  accessibleHypertextInfo);
        }
        return FALSE;
    }

    BOOL activateAccessibleHyperlink(long vmID, JOBJECT64 accessibleContext, JOBJECT64 accessibleHyperlink) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->activateAccessibleHyperlink(vmID, accessibleContext,
                                                                       accessibleHyperlink);
        }
        return FALSE;
    }

    jint getAccessibleHyperlinkCount(const long vmID,
                                     const AccessibleContext accessibleContext) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleHyperlinkCount(vmID, accessibleContext);
        }
        return -1;
    }


    BOOL getAccessibleHypertextExt(const long vmID,
                                   const AccessibleContext accessibleContext,
                                   const jint nStartIndex,
                                   /* OUT */ AccessibleHypertextInfo *hypertextInfo) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleHypertextExt(vmID,
                                                                     accessibleContext,
                                                                     nStartIndex,
                                                                     hypertextInfo);
        }
        return FALSE;
    }


    jint getAccessibleHypertextLinkIndex(const long vmID,
                                         const AccessibleHypertext hypertext,
                                         const jint nIndex) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleHypertextLinkIndex(vmID,
                                                                           hypertext,
                                                                           nIndex);
        }
        return -1;
    }


    BOOL getAccessibleHyperlink(const long vmID,
                                const AccessibleHypertext hypertext,
                                const jint nIndex,
                                /* OUT */ AccessibleHyperlinkInfo *hyperlinkInfo) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleHyperlink(vmID,
                                                                  hypertext,
                                                                  nIndex,
                                                                  hyperlinkInfo);
        }
        return FALSE;
    }


    /* Accessible KeyBindings, Icons and Actions */
    BOOL getAccessibleKeyBindings(long vmID, JOBJECT64 accessibleContext, AccessibleKeyBindings *keyBindings) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleKeyBindings(vmID, accessibleContext, keyBindings);
        }
        return FALSE;
    }

    BOOL getAccessibleIcons(long vmID, JOBJECT64 accessibleContext, AccessibleIcons *icons) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleIcons(vmID, accessibleContext, icons);
        }
        return FALSE;
    }

    BOOL getAccessibleActions(long vmID, JOBJECT64 accessibleContext, AccessibleActions *actions) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleActions(vmID, accessibleContext, actions);
        }
        return FALSE;
    }

    BOOL doAccessibleActions(long vmID, JOBJECT64 accessibleContext, AccessibleActionsToDo *actionsToDo,
                             jint *failure) {

        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->doAccessibleActions(vmID, accessibleContext, actionsToDo,
                                                               failure);
        }
        return FALSE;
    }

    /**
     * Additional methods for Teton
     */

    /**
     * Gets the AccessibleName for a component based upon the JAWS algorithm. Returns
     * whether successful.
     *
     * Bug ID 4916682 - Implement JAWS AccessibleName policy
     */
    BOOL getVirtualAccessibleName(long vmID, AccessibleContext accessibleContext, wchar_t *name, int len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getVirtualAccessibleName(vmID, accessibleContext, name, len);
        }
        return FALSE;
    }

    /**
     * Request focus for a component. Returns whether successful;
     *
     * Bug ID 4944757 - requestFocus method needed
     */
    BOOL requestFocus(long vmID, AccessibleContext accessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->requestFocus(vmID, accessibleContext);
        }
        return FALSE;
    }

    /**
     * Selects text between two indices.  Selection includes the text at the start index
     * and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944758 - selectTextRange method needed
     */
    BOOL selectTextRange(long vmID, AccessibleContext accessibleContext, int startIndex, int endIndex) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->selectTextRange(vmID, accessibleContext, startIndex, endIndex);
        }
        return FALSE;
    }

    /**
     * Get text attributes between two indices.  The attribute list includes the text at the
     * start index and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944761 - getTextAttributes between two indices method needed
     */
    BOOL getTextAttributesInRange(long vmID, AccessibleContext accessibleContext, int startIndex, int endIndex,
                                  AccessibleTextAttributesInfo *attributes, short *len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getTextAttributesInRange(vmID, accessibleContext,
                                                                    startIndex, endIndex, attributes, len);
        }
        return FALSE;
    }

    /**
     * Gets the number of visible children of a component.  Returns -1 on error.
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    int getVisibleChildrenCount(long vmID, AccessibleContext accessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getVisibleChildrenCount(vmID, accessibleContext);
        }
        return FALSE;
    }

    /**
     * Gets the visible children of an AccessibleContext. Returns whether successful;
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    BOOL getVisibleChildren(long vmID, AccessibleContext accessibleContext, int startIndex,
                            VisibleChildrenInfo *visibleChildrenInfo) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getVisibleChildren(vmID, accessibleContext, startIndex,
                                                              visibleChildrenInfo);
        }
        return FALSE;
    }

    /**
     * Set the caret to a text position. Returns whether successful;
     *
     * Bug ID 4944770 - setCaretPosition method needed
     */
    BOOL setCaretPosition(const long vmID, const AccessibleContext accessibleContext,
                          const int position) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->setCaretPosition(vmID, accessibleContext, position);
        }
        return FALSE;
    }

    // -------- Accessible Text methods -------------

    BOOL getAccessibleTextInfo(long vmID, JOBJECT64 AccessibleContext,
                               AccessibleTextInfo *textInfo, jint x, jint y) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextInfo(
                                                                 vmID,
                                                                 AccessibleContext,
                                                                 textInfo, x, y);
        }
        return FALSE;
    }

    BOOL getAccessibleTextItems(long vmID, JOBJECT64 AccessibleContext,
                                AccessibleTextItemsInfo *textItems, jint index) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextItems(
                                                                  vmID,
                                                                  AccessibleContext,
                                                                  textItems, index);
        }
        return FALSE;
    }

    BOOL getAccessibleTextSelectionInfo(long vmID, JOBJECT64 AccessibleContext,
                                        AccessibleTextSelectionInfo *selectionInfo) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextSelectionInfo(
                                                                          vmID,
                                                                          AccessibleContext,
                                                                          selectionInfo);
        }
        return FALSE;
    }

    BOOL getAccessibleTextAttributes(long vmID, JOBJECT64 AccessibleContext,
                                     jint index, AccessibleTextAttributesInfo *attributes) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextAttributes(
                                                                       vmID,
                                                                       AccessibleContext,
                                                                       index, attributes);
        }
        return FALSE;
    }

    BOOL getAccessibleTextRect(long vmID, JOBJECT64 AccessibleContext,
                               AccessibleTextRectInfo *rectInfo, jint index) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextRect(
                                                                 vmID,
                                                                 AccessibleContext,
                                                                 rectInfo, index);
        }
        return FALSE;
    }

    BOOL getCaretLocation(long vmID, JOBJECT64 AccessibleContext,
                          AccessibleTextRectInfo *rectInfo, jint index) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getCaretLocation(vmID,
                                                            AccessibleContext,
                                                            rectInfo, index);
        }
        return FALSE;
    }

    int getEventsWaiting() {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getEventsWaiting();
        }
        return FALSE;
    }

    BOOL getAccessibleTextLineBounds(long vmID, JOBJECT64 AccessibleContext,
                                     jint index, jint *startIndex, jint *endIndex) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextLineBounds(
                                                                       vmID,
                                                                       AccessibleContext,
                                                                       index, startIndex, endIndex);
        }
        return FALSE;
    }

    BOOL getAccessibleTextRange(long vmID, JOBJECT64 AccessibleContext,
                                jint start, jint end, wchar_t *text, short len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleTextRange(
                                                                  vmID,
                                                                  AccessibleContext,
                                                                  start, end, text, len);
        }
        return FALSE;
    }


    // -------- Accessible Value methods -------------

    BOOL getCurrentAccessibleValueFromContext(long vmID, JOBJECT64 AccessibleContext,
                                              wchar_t *value, short len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getCurrentAccessibleValueFromContext(
                                                                                vmID, AccessibleContext, value, len);
        }
        return FALSE;
    }

    BOOL getMaximumAccessibleValueFromContext(long vmID, JOBJECT64 AccessibleContext,
                                              wchar_t *value, short len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getMaximumAccessibleValueFromContext(
                                                                                vmID, AccessibleContext, value, len);
        }
        return FALSE;
    }

    BOOL getMinimumAccessibleValueFromContext(long vmID, JOBJECT64 AccessibleContext,
                                              wchar_t *value, short len) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getMinimumAccessibleValueFromContext(
                                                                                vmID, AccessibleContext, value, len);
        }
        return FALSE;
    }

    // -------- Accessible Selection methods -------------

    void addAccessibleSelectionFromContext(long vmID, JOBJECT64 AccessibleContext, int i) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->addAccessibleSelectionFromContext(
                                                                      vmID, AccessibleContext, i);
        }
    }

    void clearAccessibleSelectionFromContext(long vmID, JOBJECT64 AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->clearAccessibleSelectionFromContext(
                                                                        vmID, AccessibleContext);
        }
    }

    JOBJECT64 getAccessibleSelectionFromContext(long vmID, JOBJECT64 AccessibleContext, int i) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleSelectionFromContext(
                                                                             vmID, AccessibleContext, i);
        }
        return (JOBJECT64) 0;
    }

    int getAccessibleSelectionCountFromContext(long vmID, JOBJECT64 AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->getAccessibleSelectionCountFromContext(
                                                                                  vmID, AccessibleContext);
        }
        return -1;
    }

    BOOL isAccessibleChildSelectedFromContext(long vmID, JOBJECT64 AccessibleContext, int i) {
        if (theWindowsAccessBridge != 0) {
            return theWindowsAccessBridge->isAccessibleChildSelectedFromContext(
                                                                                vmID, AccessibleContext, i);
        }
        return FALSE;
    }

    void removeAccessibleSelectionFromContext(long vmID, JOBJECT64 AccessibleContext, int i) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->removeAccessibleSelectionFromContext(
                                                                         vmID, AccessibleContext, i);
        }
    }

    void selectAllAccessibleSelectionFromContext(long vmID, JOBJECT64 AccessibleContext) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->selectAllAccessibleSelectionFromContext(
                                                                            vmID, AccessibleContext);
        }
    }


    // -------- Event Handler methods -------------

#define SET_EVENT_FP(function, callbackFP)          \
    void function(callbackFP fp) {                  \
        if (theWindowsAccessBridge != 0) {          \
            theWindowsAccessBridge->function(fp);   \
        }                                           \
}

    void setJavaShutdownFP(AccessBridge_JavaShutdownFP fp) {
        if (theWindowsAccessBridge != 0) {
            theWindowsAccessBridge->setJavaShutdownFP(fp);
        }
    }

        SET_EVENT_FP(setPropertyChangeFP, AccessBridge_PropertyChangeFP)
        SET_EVENT_FP(setFocusGainedFP, AccessBridge_FocusGainedFP)
        SET_EVENT_FP(setFocusLostFP, AccessBridge_FocusLostFP)
        SET_EVENT_FP(setCaretUpdateFP, AccessBridge_CaretUpdateFP)
        SET_EVENT_FP(setMouseClickedFP, AccessBridge_MouseClickedFP)
        SET_EVENT_FP(setMouseEnteredFP, AccessBridge_MouseEnteredFP)
        SET_EVENT_FP(setMouseExitedFP, AccessBridge_MouseExitedFP)
        SET_EVENT_FP(setMousePressedFP, AccessBridge_MousePressedFP)
        SET_EVENT_FP(setMouseReleasedFP, AccessBridge_MouseReleasedFP)
        SET_EVENT_FP(setMenuCanceledFP, AccessBridge_MenuCanceledFP)
        SET_EVENT_FP(setMenuDeselectedFP, AccessBridge_MenuDeselectedFP)
        SET_EVENT_FP(setMenuSelectedFP, AccessBridge_MenuSelectedFP)
        SET_EVENT_FP(setPopupMenuCanceledFP, AccessBridge_PopupMenuCanceledFP)
        SET_EVENT_FP(setPopupMenuWillBecomeInvisibleFP, AccessBridge_PopupMenuWillBecomeInvisibleFP)
        SET_EVENT_FP(setPopupMenuWillBecomeVisibleFP, AccessBridge_PopupMenuWillBecomeVisibleFP)

        SET_EVENT_FP(setPropertyNameChangeFP, AccessBridge_PropertyNameChangeFP)
        SET_EVENT_FP(setPropertyDescriptionChangeFP, AccessBridge_PropertyDescriptionChangeFP)
        SET_EVENT_FP(setPropertyStateChangeFP, AccessBridge_PropertyStateChangeFP)
        SET_EVENT_FP(setPropertyValueChangeFP, AccessBridge_PropertyValueChangeFP)
        SET_EVENT_FP(setPropertySelectionChangeFP, AccessBridge_PropertySelectionChangeFP)
        SET_EVENT_FP(setPropertyTextChangeFP, AccessBridge_PropertyTextChangeFP)
        SET_EVENT_FP(setPropertyCaretChangeFP, AccessBridge_PropertyCaretChangeFP)
        SET_EVENT_FP(setPropertyVisibleDataChangeFP, AccessBridge_PropertyVisibleDataChangeFP)
        SET_EVENT_FP(setPropertyChildChangeFP, AccessBridge_PropertyChildChangeFP)
        SET_EVENT_FP(setPropertyActiveDescendentChangeFP, AccessBridge_PropertyActiveDescendentChangeFP)

        SET_EVENT_FP(setPropertyTableModelChangeFP, AccessBridge_PropertyTableModelChangeFP)

#ifdef __cplusplus
        }
#endif
