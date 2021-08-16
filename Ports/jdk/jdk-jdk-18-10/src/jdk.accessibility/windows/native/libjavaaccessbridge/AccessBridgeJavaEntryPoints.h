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
 * A class to manage JNI calls into AccessBridge.java
 */

#include "AccessBridgePackages.h"

#include <windows.h>
#include <jni.h>

#ifndef __AccessBridgeJavaEntryPoints_H__
#define __AccessBridgeJavaEntryPoints_H__

class AccessBridgeJavaEntryPoints {
    JNIEnv *jniEnv;

    jobject accessBridgeObject;

    jclass bridgeClass;
    jclass eventHandlerClass;

    jmethodID decrementReferenceMethod;
    jmethodID getJavaVersionPropertyMethod;

    jmethodID isJavaWindowMethod;
    jmethodID isSameObjectMethod;
    jmethodID getAccessibleContextFromHWNDMethod;
    jmethodID getHWNDFromAccessibleContextMethod;

    jmethodID getAccessibleContextAtMethod;
    jmethodID getAccessibleContextWithFocusMethod;

    jmethodID getAccessibleNameFromContextMethod;
    jmethodID getAccessibleDescriptionFromContextMethod;
    jmethodID getAccessibleRoleStringFromContextMethod;
    jmethodID getAccessibleRoleStringFromContext_en_USMethod;
    jmethodID getAccessibleStatesStringFromContextMethod;
    jmethodID getAccessibleStatesStringFromContext_en_USMethod;
    jmethodID getAccessibleParentFromContextMethod;
    jmethodID getAccessibleIndexInParentFromContextMethod;
    jmethodID getAccessibleChildrenCountFromContextMethod;
    jmethodID getAccessibleChildFromContextMethod;
    jmethodID getAccessibleBoundsOnScreenFromContextMethod;
    jmethodID getAccessibleXcoordFromContextMethod;
    jmethodID getAccessibleYcoordFromContextMethod;
    jmethodID getAccessibleHeightFromContextMethod;
    jmethodID getAccessibleWidthFromContextMethod;

    jmethodID getAccessibleComponentFromContextMethod;
    jmethodID getAccessibleActionFromContextMethod;
    jmethodID getAccessibleSelectionFromContextMethod;
    jmethodID getAccessibleTextFromContextMethod;
    jmethodID getAccessibleValueFromContextMethod;

    /* begin AccessibleTable */
    jmethodID getAccessibleTableFromContextMethod;
    jmethodID getAccessibleTableRowHeaderMethod;
    jmethodID getAccessibleTableColumnHeaderMethod;
    jmethodID getAccessibleTableRowCountMethod;
    jmethodID getAccessibleTableColumnCountMethod;
    jmethodID getAccessibleTableCaptionMethod;
    jmethodID getAccessibleTableSummaryMethod;

    jmethodID getContextFromAccessibleTableMethod;
    jmethodID getAccessibleTableCellAccessibleContextMethod;
    jmethodID getAccessibleTableCellIndexMethod;
    jmethodID getAccessibleTableCellRowExtentMethod;
    jmethodID getAccessibleTableCellColumnExtentMethod;
    jmethodID isAccessibleTableCellSelectedMethod;

    jmethodID getAccessibleTableRowHeaderRowCountMethod;
    jmethodID getAccessibleTableColumnHeaderRowCountMethod;

    jmethodID getAccessibleTableRowHeaderColumnCountMethod;
    jmethodID getAccessibleTableColumnHeaderColumnCountMethod;

    jmethodID getAccessibleTableRowDescriptionMethod;
    jmethodID getAccessibleTableColumnDescriptionMethod;

    jmethodID getAccessibleTableRowSelectionCountMethod;
    jmethodID isAccessibleTableRowSelectedMethod;
    jmethodID getAccessibleTableRowSelectionsMethod;

    jmethodID getAccessibleTableColumnSelectionCountMethod;
    jmethodID isAccessibleTableColumnSelectedMethod;
    jmethodID getAccessibleTableColumnSelectionsMethod;

    jmethodID getAccessibleTableRowMethod;
    jmethodID getAccessibleTableColumnMethod;
    jmethodID getAccessibleTableIndexMethod;

    /* end AccessibleTable */

    /* begin AccessibleRelationSet */

    jmethodID getAccessibleRelationSetMethod;
    jmethodID getAccessibleRelationCountMethod;
    jmethodID getAccessibleRelationKeyMethod;
    jmethodID getAccessibleRelationTargetCountMethod;
    jmethodID getAccessibleRelationTargetMethod;

    /* end AccessibleRelationSet */

    // AccessibleHypertext methods
    jmethodID getAccessibleHypertextMethod;
    jmethodID getAccessibleHyperlinkCountMethod;
    jmethodID getAccessibleHyperlinkTextMethod;
    jmethodID getAccessibleHyperlinkURLMethod;
    jmethodID getAccessibleHyperlinkStartIndexMethod;
    jmethodID getAccessibleHyperlinkEndIndexMethod;
    jmethodID getAccessibleHypertextLinkIndexMethod;
    jmethodID getAccessibleHyperlinkMethod;
    jmethodID activateAccessibleHyperlinkMethod;

    // AccessibleKeyBinding
    jmethodID getAccessibleKeyBindingsCountMethod;
    jmethodID getAccessibleKeyBindingCharMethod;
    jmethodID getAccessibleKeyBindingModifiersMethod;

    // AccessibleIcon
    jmethodID getAccessibleIconsCountMethod;
    jmethodID getAccessibleIconDescriptionMethod;
    jmethodID getAccessibleIconHeightMethod;
    jmethodID getAccessibleIconWidthMethod;

    // AccessibleAction
    jmethodID getAccessibleActionsCountMethod;
    jmethodID getAccessibleActionNameMethod;
    jmethodID doAccessibleActionsMethod;

    // AccessibleText
    jmethodID getAccessibleCharCountFromContextMethod;
    jmethodID getAccessibleCaretPositionFromContextMethod;
    jmethodID getAccessibleIndexAtPointFromContextMethod;

    jmethodID getAccessibleLetterAtIndexFromContextMethod;
    jmethodID getAccessibleWordAtIndexFromContextMethod;
    jmethodID getAccessibleSentenceAtIndexFromContextMethod;

    jmethodID getAccessibleTextSelectionStartFromContextMethod;
    jmethodID getAccessibleTextSelectionEndFromContextMethod;
    jmethodID getAccessibleTextSelectedTextFromContextMethod;
    jmethodID getAccessibleAttributesAtIndexFromContextMethod;
    jmethodID getAccessibleAttributeSetAtIndexFromContextMethod;
    jmethodID getAccessibleTextRectAtIndexFromContextMethod;
    jmethodID getAccessibleXcoordTextRectAtIndexFromContextMethod;
    jmethodID getAccessibleYcoordTextRectAtIndexFromContextMethod;
    jmethodID getAccessibleHeightTextRectAtIndexFromContextMethod;
    jmethodID getAccessibleWidthTextRectAtIndexFromContextMethod;
    jmethodID getAccessibleTextLineLeftBoundsFromContextMethod;
    jmethodID getAccessibleTextLineRightBoundsFromContextMethod;
    jmethodID getAccessibleTextRangeFromContextMethod;

    jmethodID getCurrentAccessibleValueFromContextMethod;
    jmethodID getMaximumAccessibleValueFromContextMethod;
    jmethodID getMinimumAccessibleValueFromContextMethod;

    jmethodID addAccessibleSelectionFromContextMethod;
    jmethodID clearAccessibleSelectionFromContextMethod;
    jmethodID getAccessibleSelectionContextFromContextMethod;
    jmethodID getAccessibleSelectionCountFromContextMethod;
    jmethodID isAccessibleChildSelectedFromContextMethod;
    jmethodID removeAccessibleSelectionFromContextMethod;
    jmethodID selectAllAccessibleSelectionFromContextMethod;

    jmethodID addJavaEventNotificationMethod;
    jmethodID removeJavaEventNotificationMethod;
    jmethodID addAccessibilityEventNotificationMethod;
    jmethodID removeAccessibilityEventNotificationMethod;

    jmethodID getBoldFromAttributeSetMethod;
    jmethodID getItalicFromAttributeSetMethod;
    jmethodID getUnderlineFromAttributeSetMethod;
    jmethodID getStrikethroughFromAttributeSetMethod;
    jmethodID getSuperscriptFromAttributeSetMethod;
    jmethodID getSubscriptFromAttributeSetMethod;
    jmethodID getBackgroundColorFromAttributeSetMethod;
    jmethodID getForegroundColorFromAttributeSetMethod;
    jmethodID getFontFamilyFromAttributeSetMethod;
    jmethodID getFontSizeFromAttributeSetMethod;
    jmethodID getAlignmentFromAttributeSetMethod;
    jmethodID getBidiLevelFromAttributeSetMethod;
    jmethodID getFirstLineIndentFromAttributeSetMethod;
    jmethodID getLeftIndentFromAttributeSetMethod;
    jmethodID getRightIndentFromAttributeSetMethod;
    jmethodID getLineSpacingFromAttributeSetMethod;
    jmethodID getSpaceAboveFromAttributeSetMethod;
    jmethodID getSpaceBelowFromAttributeSetMethod;

    jmethodID setTextContentsMethod;
    jmethodID getParentWithRoleMethod;
    jmethodID getTopLevelObjectMethod;
    jmethodID getParentWithRoleElseRootMethod;
    jmethodID getObjectDepthMethod;
    jmethodID getActiveDescendentMethod;

    /**
     * Additional methods for Teton
     */
    jmethodID getVirtualAccessibleNameFromContextMethod; // Ben Key
    jmethodID requestFocusMethod;
    jmethodID selectTextRangeMethod;
    jmethodID getTextAttributesInRangeMethod;
    jmethodID getVisibleChildrenCountMethod;
    jmethodID getVisibleChildMethod;
    jmethodID setCaretPositionMethod;

    jmethodID getCaretLocationMethod;
    jmethodID getCaretLocationXMethod;
    jmethodID getCaretLocationYMethod;
    jmethodID getCaretLocationHeightMethod;
    jmethodID getCaretLocationWidthMethod;

public:
    AccessBridgeJavaEntryPoints(JNIEnv *jniEnvironment, jobject bridgeObject);
    ~AccessBridgeJavaEntryPoints();
    BOOL BuildJavaEntryPoints();

    // HWND management methods
    BOOL isJavaWindow(jint window);
    jobject getAccessibleContextFromHWND(jint window);
    HWND getHWNDFromAccessibleContext(jobject accessibleContext);

    // version methods
    BOOL getVersionInfo(AccessBridgeVersionInfo *info);

    // verification methods
    BOOL verifyAccessibleText(jobject obj);

    /* ===== utility methods ===== */
    BOOL isSameObject(jobject obj1, jobject obj2);
    BOOL setTextContents(const jobject accessibleContext, const wchar_t *text);
    jobject getParentWithRole (const jobject accessibleContext, const wchar_t *role);
    jobject getTopLevelObject (const jobject accessibleContext);
    jobject getParentWithRoleElseRoot (const jobject accessibleContext, const wchar_t *role);
    jint getObjectDepth (const jobject accessibleContext);
    jobject getActiveDescendent (const jobject accessibleContext);

    // Accessible Context methods
    jobject getAccessibleContextAt(jint x, jint y, jobject AccessibleContext);
    jobject getAccessibleContextWithFocus();
    BOOL getAccessibleContextInfo(jobject AccessibleContext, AccessibleContextInfo *info);
    jobject getAccessibleChildFromContext(jobject AccessibleContext, jint childIndex);
    jobject getAccessibleParentFromContext(jobject AccessibleContext);

    /* begin AccessibleTable methods */

    BOOL getAccessibleTableInfo(jobject acParent, AccessibleTableInfo *tableInfo);
    BOOL getAccessibleTableCellInfo(jobject accessibleTable,jint row, jint column,
                                    AccessibleTableCellInfo *tableCellInfo);

    BOOL getAccessibleTableRowHeader(jobject acParent, AccessibleTableInfo *tableInfo);
    BOOL getAccessibleTableColumnHeader(jobject acParent, AccessibleTableInfo *tableInfo);

    jobject getAccessibleTableRowDescription(jobject acParent, jint row);
    jobject getAccessibleTableColumnDescription(jobject acParent, jint column);

    jint getAccessibleTableRowSelectionCount(jobject accessibleTable);
    BOOL isAccessibleTableRowSelected(jobject accessibleTable, jint row);
    BOOL getAccessibleTableRowSelections(jobject accessibleTable, jint count, jint *selections);

    jint getAccessibleTableColumnSelectionCount(jobject accessibleTable);
    BOOL isAccessibleTableColumnSelected(jobject accessibleTable, jint column);
    BOOL getAccessibleTableColumnSelections(jobject accessibleTable, jint count, jint *selections);

    jint getAccessibleTableRow(jobject accessibleTable, jint index);
    jint getAccessibleTableColumn(jobject accessibleTable, jint index);
    jint getAccessibleTableIndex(jobject accessibleTable, jint row, jint column);

    /* end AccessibleTable methods */

    BOOL getAccessibleRelationSet(jobject accessibleContext, AccessibleRelationSetInfo *relationSetInfo);

    // AccessibleHypertext methods
    BOOL getAccessibleHypertext(jobject accessibleContext, AccessibleHypertextInfo *hyperlink);

    BOOL activateAccessibleHyperlink(jobject accessibleContext, jobject accessibleHyperlink);

    BOOL getAccessibleHypertextExt(const jobject accessibleContext,
                                   const jint nStartIndex,
                                   /* OUT */ AccessibleHypertextInfo *hypertext);
    jint getAccessibleHyperlinkCount(const jobject accessibleContext);
    jint getAccessibleHypertextLinkIndex(const jobject accessibleContext,
                                         const jint nIndex);
    BOOL getAccessibleHyperlink(const jobject accessibleContext,
                                const jint nIndex,
                                /* OUT */ AccessibleHyperlinkInfo *hyperlinkInfo);

    // Accessible Keybinding methods
    BOOL getAccessibleKeyBindings(jobject accessibleContext, AccessibleKeyBindings *keyBindings);

    // AccessibleIcon methods
    BOOL getAccessibleIcons(jobject accessibleContext, AccessibleIcons *icons);

    // AccessibleActionMethods
    BOOL getAccessibleActions(jobject accessibleContext, AccessibleActions *actions);
    BOOL doAccessibleActions(jobject accessibleContext, AccessibleActionsToDo *actionsToDo, jint *failure);

    // Accessible Text methods
    BOOL getAccessibleTextInfo(jobject AccessibleContext, AccessibleTextInfo *textInfo, jint x, jint y);
    BOOL getAccessibleTextItems(jobject AccessibleContext, AccessibleTextItemsInfo *textItems, jint index);
    BOOL getAccessibleTextSelectionInfo(jobject AccessibleContext, AccessibleTextSelectionInfo *selectionInfo);
    BOOL getAccessibleTextAttributes(jobject AccessibleContext, jint index, AccessibleTextAttributesInfo *attributes);
    BOOL getAccessibleTextRect(jobject AccessibleContext, AccessibleTextRectInfo *rectInfo, jint index);
    BOOL getAccessibleCaretRect(jobject AccessibleContext, AccessibleTextRectInfo *rectInfo, jint index);
    BOOL getAccessibleTextLineBounds(jobject AccessibleContext, jint index, jint *startIndex, jint *endIndex);
    BOOL getAccessibleTextRange(jobject AccessibleContext, jint start, jint end, wchar_t *text, short len);

    // Accessible Value methods
    BOOL getCurrentAccessibleValueFromContext(jobject AccessibleContext, wchar_t *value, short len);
    BOOL getMaximumAccessibleValueFromContext(jobject AccessibleContext, wchar_t *value, short len);
    BOOL getMinimumAccessibleValueFromContext(jobject AccessibleContext, wchar_t *value, short len);

    // Accessible Selection methods
    void addAccessibleSelectionFromContext(jobject AccessibleContext, int i);
    void clearAccessibleSelectionFromContext(jobject AccessibleContext);
    jobject getAccessibleSelectionFromContext(jobject AccessibleContext, int i);
    int getAccessibleSelectionCountFromContext(jobject AccessibleContext);
    BOOL isAccessibleChildSelectedFromContext(jobject AccessibleContext, int i);
    void removeAccessibleSelectionFromContext(jobject AccessibleContext, int i);
    void selectAllAccessibleSelectionFromContext(jobject AccessibleContext);

    // Event handling methods
    BOOL addJavaEventNotification(jlong type);
    BOOL removeJavaEventNotification(jlong type);
    BOOL addAccessibilityEventNotification(jlong type);
    BOOL removeAccessibilityEventNotification(jlong type);

    /**
     * Additional methods for Teton
     */

    /**
     * Gets the AccessibleName for a component based upon the JAWS algorithm. Returns
     * whether successful.
     *
     * Bug ID 4916682 - Implement JAWS AccessibleName policy
     */
    BOOL getVirtualAccessibleName(const jobject accessibleContext, wchar_t *name, int len);

    /**
     * Request focus for a component. Returns whether successful;
     *
     * Bug ID 4944757 - requestFocus method needed
     */
    BOOL requestFocus(const jobject accessibleContext);

    /**
     * Selects text between two indices.  Selection includes the text at the start index
     * and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944758 - selectTextRange method needed
     */
    BOOL selectTextRange(const jobject accessibleContext, int startIndex, int endIndex);

    /**
     * Get text attributes between two indices.  The attribute list includes the text at the
     * start index and the text at the end index. Returns whether successful;
     *
     * Bug ID 4944761 - getTextAttributes between two indices method needed
     */
    BOOL getTextAttributesInRange(const jobject accessibleContext, int startIndex, int endIndex,
                                  AccessibleTextAttributesInfo *attributes, short *len);

    /**
     * Gets the number of visible children of a component. Returns -1 on error.
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    int getVisibleChildrenCount(const jobject accessibleContext);

    /**
     * Gets the visible children of an AccessibleContext. Returns whether successful;
     *
     * Bug ID 4944762- getVisibleChildren for list-like components needed
     */
    BOOL getVisibleChildren(const jobject accessibleContext, const int startIndex,
                            VisibleChildrenInfo *visibleChildrenInfo);

    /**
     * Set the caret to a text position. Returns whether successful;
     *
     * Bug ID 4944770 - setCaretPosition method needed
     */
    BOOL setCaretPosition(const jobject accessibleContext, int position);

    /**
     * Gets the bounding rectangle for the text caret
     */
    BOOL getCaretLocation(jobject AccessibleContext, AccessibleTextRectInfo *rectInfo, jint index);

};

#endif
