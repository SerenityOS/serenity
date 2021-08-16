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

#include "AccessBridgeCalls.h"
#include "AccessInfo.h"
#include <stdio.h>
#include <time.h>

LogStringCallbackFP g_LogStringCallback = NULL;

/*
 * returns formatted date and time
 */
char *getTimeAndDate() {
    static char datebuf[64];
    struct tm *newtime;
    char am_pm[] = "AM";
    time_t long_time;

    time( &long_time );                /* Get time as long integer. */
    newtime = localtime( &long_time ); /* Convert to local time. */

    if( newtime->tm_hour > 12 )        /* Set up extension. */
        strcpy( am_pm, "PM" );
    if( newtime->tm_hour > 12 )        /* Convert from 24-hour */
                newtime->tm_hour -= 12;    /*   to 12-hour clock.  */
    if( newtime->tm_hour == 0 )        /*Set hour to 12 if midnight. */
        newtime->tm_hour = 12;

    sprintf(datebuf, "%.19s %s\n", asctime( newtime ), am_pm );
    return (char *)datebuf;
}


/*
 * displays a message in a dialog and writes the message to a logfile
 */
void displayAndLog(HWND hDlg, int nIDDlgItem, FILE *logfile, char *msg, ...) {

    if (hDlg == NULL || msg == NULL) {
        return;
    }

    char tmpbuf[HUGE_BUFSIZE];
    va_list argprt;

    va_start(argprt, msg);
    vsprintf(tmpbuf, msg, argprt);

    SetDlgItemText(hDlg, nIDDlgItem, tmpbuf);

    fprintf(logfile, "\n****************************************\n");
    fprintf(logfile, "%s\n", getTimeAndDate());
    fprintf(logfile, "%s\n", tmpbuf);
    fflush(logfile);

    if ( NULL != g_LogStringCallback )
    {
        g_LogStringCallback (tmpbuf);
    }
}

/*
 * writes a text string to a logfile
 */
void logString(FILE *logfile, char *msg, ...) {

    if (logfile == NULL || msg == NULL) {
        return;
    }

    char tmpbuf[LINE_BUFSIZE];
    va_list argprt;

    va_start(argprt, msg);
    vsprintf(tmpbuf, msg, argprt);

    fprintf(logfile, tmpbuf);
    fprintf(logfile, "\n");
    fflush(logfile);
}

/*
 * safely appends a message to a buffer
 */
BOOL appendToBuffer(char *buf, size_t buflen, char *msg, ...) {

    static char warning[] =
        "\nNot enough buffer space; remaining information truncated.\n";
    size_t warningLength = strlen(warning) + 1;

    if (buf == NULL || msg == NULL) {
        return FALSE;
    }

    char tmpbuf[LARGE_BUFSIZE];
    va_list argprt;

    va_start(argprt, msg);
    vsprintf(tmpbuf, msg, argprt);

    // verify there's enough space in the buffer
    size_t spaceRemaining = buflen - strlen(buf) - 1;
    if (spaceRemaining <= warningLength) {
        strncat(buf, warning, spaceRemaining);
        return FALSE;
    }
    strncat(buf, tmpbuf, spaceRemaining);
    return TRUE;
}

/**
 * returns accessibility information for an AccessibleContext
 */
char *getAccessibleInfo(long vmID, AccessibleContext ac, char *buffer,
                        int bufsize) {
    return getAccessibleInfo(vmID, ac, 0, 0, buffer, bufsize);
}

/**
 * returns accessibility information at the specified coordinates in an
 * AccessibleContext
 */
char *getAccessibleInfo(long vmID, AccessibleContext ac, int x, int y,
                        char *buffer, int bufsize) {

    wchar_t tmpBuf[LINE_BUFSIZE];
    wchar_t name[LINE_BUFSIZE];
    int i, j;
    long start;
    long end;

    if (buffer == NULL || bufsize <= 0) {
        return NULL;
    }
    buffer[0] = NULL;

    /* ===== AccessBridge / J2SE version information ===== */

    AccessBridgeVersionInfo versionInfo;
    BOOL result = GetVersionInfo(vmID, &versionInfo);

    if (result == FALSE) {
        appendToBuffer( buffer, bufsize,
                        "\r\nERROR: cannot get version information", bufsize );
    } else {
        appendToBuffer(buffer, bufsize, "Version Information:");
        appendToBuffer( buffer, bufsize,
                        "\r\n    Java virtual machine version: %ls",
                        versionInfo.VMversion );
        appendToBuffer( buffer, bufsize,
                        "\r\n    Access Bridge Java class version: %ls",
                        versionInfo.bridgeJavaClassVersion );
        appendToBuffer( buffer, bufsize,
                        "\r\n    Access Bridge Java DLL version: %ls",
                        versionInfo.bridgeJavaDLLVersion );
        appendToBuffer( buffer, bufsize,
                        "\r\n    Access Bridge Windows DLL version: %ls",
                        versionInfo.bridgeWinDLLVersion );
    }

    if (ac == (AccessibleContext) 0) {
        return buffer;
    }


    /* ===== core AccessibleContext information ===== */

    AccessibleContextInfo info;
    if (GetAccessibleContextInfo(vmID, ac, &info) == FALSE) {
        appendToBuffer( buffer, bufsize,
                        "\r\nERROR: GetAccessibleContextInfo failed ", bufsize );
    } else {
        appendToBuffer( buffer, bufsize,
                        "\r\n\r\nAccessibleContext information", bufsize );
        if (x >= 0 && y >= 0) {
            appendToBuffer(buffer, bufsize, " at mouse point [%d, %d]:", x, y);
        } else {
            appendToBuffer(buffer, bufsize, ":", bufsize);
        }

        appendToBuffer(buffer, bufsize, "\r\n    Name:  %ls", info.name);
        if ( getVirtualAccessibleName( vmID, ac, name,
                                       (sizeof(name) / sizeof(wchar_t)) ) == FALSE ) {
            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nERROR: getVirtualAccessibleName", bufsize );
        } else {
            appendToBuffer(buffer, bufsize, "\r\n    Virtual Name:  %ls", name);
        }
        appendToBuffer( buffer, bufsize, "\r\n    Description:  %ls",
                        info.description );
        appendToBuffer(buffer, bufsize, "\r\n    Role:  %ls", info.role);
        appendToBuffer( buffer, bufsize, "\r\n    Role in en_US locale:  %ls",
                        info.role_en_US );
        appendToBuffer(buffer, bufsize, "\r\n    States:  %ls", info.states);
        appendToBuffer( buffer, bufsize, "\r\n    States in en_US locale:  %ls",
                        info.states_en_US );
        appendToBuffer( buffer, bufsize, "\r\n    Index in parent:  %d",
                        info.indexInParent );
        appendToBuffer( buffer, bufsize, "\r\n    Children count:  %d",
                        info.childrenCount );
        appendToBuffer( buffer, bufsize,
                        "\r\n    Bounding rectangle:  [%d, %d, %d, %d]",
                        info.x, info.y, info.x + info.width,
                        info.y + info.height );

        /*  top-level window info */
        AccessibleContext topAC  = getTopLevelObject(vmID, ac);
        if (topAC == NULL) {
            appendToBuffer( buffer, bufsize,
                            "\r\nERROR: getTopLevelObject failed", bufsize );
        } else {
            AccessibleContextInfo topInfo;
            if (GetAccessibleContextInfo(vmID, topAC, &topInfo) == FALSE) {
                appendToBuffer(
                    buffer, bufsize,
                    "\r\nERROR: GetAccessibleContextInfo failed for top-level window ",
                    bufsize );
            } else {
                if (getVirtualAccessibleName(vmID, topAC, name,
                        (sizeof(name) / sizeof(wchar_t)) ) == FALSE) {
                    appendToBuffer( buffer, bufsize,
                                    "\r\n\r\nERROR: getVirtualAccessibleName",
                                    bufsize );
                } else {
                    appendToBuffer( buffer, bufsize,
                                    "\r\n    Top-level window name:  %ls",
                                    name );
                }
                appendToBuffer( buffer, bufsize,
                                "\r\n    Top-level window role:  %ls",
                                topInfo.role ) ;
            }
            ReleaseJavaObject(vmID, topAC);

        }

        /* ===== AccessibleParent information ===== */

        AccessibleContext parentAC = GetAccessibleParentFromContext(vmID, ac);
        if (parentAC == NULL) {
            appendToBuffer(buffer, bufsize, "\r\n    No parent", bufsize);
        } else {
            AccessibleContextInfo parentInfo;
            if (GetAccessibleContextInfo(vmID, parentAC, &parentInfo) == FALSE) {
                appendToBuffer( buffer, bufsize,
                    "\r\nERROR: GetAccessibleContextInfo failed for parent",
                    bufsize );
            } else {
                appendToBuffer( buffer, bufsize, "\r\n    Parent name:  %ls",
                                parentInfo.name );
                if ( getVirtualAccessibleName( vmID, parentAC, name,
                        (sizeof(name) / sizeof(wchar_t)) ) == FALSE ) {
                    appendToBuffer( buffer, bufsize,
                                    "\r\n\r\nERROR: getVirtualAccessibleName",
                                    bufsize );
                } else {
                    appendToBuffer( buffer, bufsize,
                                    "\r\n    Parent virtual name:  %ls", name );
                }
                appendToBuffer( buffer, bufsize, "\r\n    Parent role:  %ls",
                                parentInfo.role );
            }
            ReleaseJavaObject(vmID, parentAC);
        }


        /* ====== visible children ===== */
        int nChildren = getVisibleChildrenCount(vmID, ac);
        if (nChildren == -1) {
            appendToBuffer( buffer, bufsize,
                            "\r\nERROR: GetVisibleChildrenCount failed",
                            bufsize );
        } else {
            appendToBuffer( buffer, bufsize,
                            "\r\n    Visible descendents count:  %d",
                            nChildren );
        }

        if (nChildren > 0) {
            VisibleChildrenInfo visibleChildrenInfo;
            if (getVisibleChildren(vmID, ac, 0, &visibleChildrenInfo) == FALSE) {
                appendToBuffer( buffer, bufsize,
                                "\r\nERROR: GetVisibleChildren failed",
                                bufsize );
            } else {
                AccessibleContextInfo childACInfo;
                for ( int child = 0;
                      child < visibleChildrenInfo.returnedChildrenCount;
                      child++ ) {
                    AccessibleContext childAC =
                        visibleChildrenInfo.children[child];
                    if (GetAccessibleContextInfo(vmID, childAC, &childACInfo)) {
                        if ( getVirtualAccessibleName( vmID, childAC, name,
                                (sizeof(name) / sizeof(wchar_t))) == FALSE) {
                            appendToBuffer( buffer, bufsize,
                                "\r\n\r\nERROR: getVirtualAccessibleName",
                                bufsize );
                        } else {
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Descendent %d name:  %ls", child,
                                name );
                        }
                        appendToBuffer( buffer, bufsize,
                                        "\r\n    Descendent %d role:  %ls",
                                        child, childACInfo.role );
                    }
                    ReleaseJavaObject(vmID, childAC);
                }
            }
        }

        /* ===== AccessibleSelection ===== */

        if (info.accessibleSelection == TRUE) {

            int selCount;
            AccessibleContext selectedAC;

            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nAccessible Selection information:",
                            bufsize );

            if ((selCount = GetAccessibleSelectionCountFromContext(vmID, ac)) != -1) {
                appendToBuffer( buffer, bufsize, "\r\n    Selection count:  %d",
                                selCount );

                for (i = 0; i < selCount; i++) {
                    if ( ( selectedAC =
                            GetAccessibleSelectionFromContext(vmID, ac, i) ) == NULL ) {
                        appendToBuffer( buffer, bufsize,
                            "\r\nERROR: GetAccessibleSelectionFromContext failed forselection %d",
                            i );
                    } else {
                        if (GetAccessibleContextInfo(vmID, selectedAC, &info) == FALSE) {
                            appendToBuffer( buffer, bufsize,
                                "\r\nERROR: GetAccessibleContextInfo failed for selection %d", i);
                        } else {
                            if ( getVirtualAccessibleName( vmID, selectedAC, name,
                                    (sizeof(name) / sizeof(wchar_t)) ) == FALSE ) {
                                appendToBuffer( buffer, bufsize,
                                    "\r\n\r\nERROR: getVirtualAccessibleName", bufsize);
                            } else {
                                appendToBuffer( buffer, bufsize,
                                    "\r\n    Selection %d name: %ls", i, name );
                            }
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Selection %d role: %ls", i, info.role);
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Index in parent of selection %d: %d",
                                i, info.indexInParent );
                        }
                        ReleaseJavaObject(vmID, selectedAC);
                    }
                }
            }
        }

        // ====== Accessible KeyBindings, Icons and Actions ======

        AccessibleKeyBindings keyBindings;
        if (getAccessibleKeyBindings(vmID, ac, &keyBindings) == TRUE &&
            keyBindings.keyBindingsCount > 0) {

            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nAccessibleKeyBinding info:", bufsize );
            appendToBuffer( buffer, bufsize,
                            "\r\n    Number of key bindings:  %d",
                            keyBindings.keyBindingsCount );

            for (j = 0; j < keyBindings.keyBindingsCount; j++) {

                appendToBuffer( buffer, bufsize,
                                "\r\n    Key binding %d character: %c", j,
                                keyBindings.keyBindingInfo[j].character );
                appendToBuffer( buffer, bufsize,
                                "\r\n    Key binding %d modifiers: %d", j,
                                keyBindings.keyBindingInfo[j].modifiers );
            }
        }

        AccessibleIcons icons;
        if (getAccessibleIcons(vmID, ac, &icons) == TRUE &&
            icons.iconsCount > 0) {

            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nAccessibleIcons info:", bufsize );
            appendToBuffer( buffer, bufsize,
                            "\r\n    Number of icons:  %d", icons.iconsCount );

            for (j = 0; j < icons.iconsCount; j++) {

                appendToBuffer( buffer, bufsize,
                                "\r\n    Icon %d description: %ls", j,
                                icons.iconInfo[j].description );
                appendToBuffer( buffer, bufsize,
                                "\r\n    Icon %d height: %d", j,
                                icons.iconInfo[j].height );
                appendToBuffer( buffer, bufsize,
                                "\r\n    Icon %d width: %d", j,
                                icons.iconInfo[j].width );
            }
        }

        AccessibleActions actions;
        if (getAccessibleActions(vmID, ac, &actions) == TRUE &&
            actions.actionsCount > 0) {

            appendToBuffer( buffer, bufsize, "\r\n\r\nAccessibleActions info:",
                            bufsize );
            appendToBuffer( buffer, bufsize, "\r\n    Number of actions:  %d",
                            actions.actionsCount );

            for (j = 0; j < actions.actionsCount; j++) {
                appendToBuffer( buffer, bufsize, "\r\n    Action %d name: %ls",
                                j, actions.actionInfo[j].name );
            }
        }

        /* ===== AccessibleRelationSet ===== */

        AccessibleRelationSetInfo relationSetInfo;
        if (getAccessibleRelationSet(vmID, ac, &relationSetInfo) == FALSE) {
            appendToBuffer( buffer, bufsize,
                            "\r\nGetAccessibleRelationSet failed.", bufsize );
        } else {
            int i;
            AccessibleContextInfo relInfo;

            if (relationSetInfo.relationCount > 0) {
                appendToBuffer( buffer, bufsize,
                                "\r\n\r\nAccessibleRelationSet information:",
                                bufsize );
                appendToBuffer( buffer, bufsize,
                                "\r\n    Number of relations:  %d",
                                relationSetInfo.relationCount );
            }
            for (i = 0; i < relationSetInfo.relationCount; i++) {
                AccessibleRelationInfo relationInfo =
                    relationSetInfo.relations[i];

                appendToBuffer( buffer, bufsize,
                                "\r\n    Relation %d key:  %ls", i,
                                relationInfo.key );
                appendToBuffer( buffer, bufsize,
                                "\r\n    Relation %d target count:  %d", i,
                                relationInfo.targetCount );
                for (j = 0; j < relationInfo.targetCount; j++) {
                    if (GetAccessibleContextInfo(
                            vmID, relationInfo.targets[j], &relInfo ) == FALSE) {
                        appendToBuffer( buffer, bufsize,
                            "\r\nERROR: GetAccessibleContextInfo failed.",
                            bufsize );
                    } else {
                        // Core AccessibleContext information for relation target
                        if ( getVirtualAccessibleName( vmID, relationInfo.targets[j],
                                name, (sizeof(name) / sizeof(wchar_t)) ) == FALSE ) {
                            appendToBuffer( buffer, bufsize,
                                "\r\n\r\nERROR: getVirtualAccessibleName", bufsize );
                        } else {
                            appendToBuffer( buffer, bufsize,
                                            "\r\n        Target %d name:  %ls",
                                            j, name );
                        }
                        appendToBuffer( buffer, bufsize,
                                        "\r\n        Target %d role:  %ls", j,
                                        relInfo.role);
                    }
                    ReleaseJavaObject(vmID, relationInfo.targets[j]);

                }
            }
        }

        /* ===== AccessibleValue ===== */

        if (info.accessibleInterfaces & cAccessibleValueInterface) {

            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nAccessible Value information:", bufsize);

            if ( GetCurrentAccessibleValueFromContext( vmID, ac, tmpBuf,
                    (sizeof(tmpBuf) / sizeof(wchar_t)) ) == TRUE ) {
                appendToBuffer( buffer, bufsize, "\r\n    Current Value:  %ls",
                                tmpBuf );
            }
            if ( GetMaximumAccessibleValueFromContext( vmID, ac, tmpBuf,
                    (sizeof(tmpBuf) / sizeof(wchar_t))) == TRUE ) {
                appendToBuffer( buffer, bufsize,
                                "\r\n    Maximum Value:  %ls", tmpBuf );
            }
            if ( GetMinimumAccessibleValueFromContext( vmID, ac, tmpBuf,
                 (sizeof(tmpBuf) / sizeof(wchar_t)) ) == TRUE ) {
                appendToBuffer( buffer, bufsize,
                                "\r\n    Minimum Value:  %ls", tmpBuf );
            }
        }


        /* ===== AccessibleTable ===== */

        AccessibleTableInfo tableInfo;

        if ( (info.accessibleInterfaces & cAccessibleTableInterface) ==
             cAccessibleTableInterface ) {
            if (getAccessibleTableInfo(vmID, ac, &tableInfo) != TRUE) {
                appendToBuffer( buffer, bufsize,
                                "\r\nERROR: getAccessibleTableInfo failed",
                                bufsize );
            } else {
                appendToBuffer( buffer, bufsize,
                                "\r\n\r\nAccessibleTable info:", bufsize );

                int trow = getAccessibleTableRow( vmID,
                                                  tableInfo.accessibleTable, 3 );
                appendToBuffer( buffer, bufsize,
                                "\r\n    getAccessibleTableRow:  %d", trow);

                int tcol =
                    getAccessibleTableColumn(vmID, tableInfo.accessibleTable, 2);
                appendToBuffer( buffer, bufsize,
                                "\r\n    getAccessibleTableColumn:  %d", tcol );

                int tindex = getAccessibleTableIndex( vmID,
                                                      tableInfo.accessibleTable,
                                                      2, 3 );
                appendToBuffer( buffer, bufsize,
                                "\r\n    getAccessibleTableIndex:  %d",
                                tindex );

                // Core info
                appendToBuffer( buffer, bufsize,
                                "\r\n    table row count:  %d",
                                tableInfo.rowCount );
                appendToBuffer( buffer, bufsize,
                                "\r\n    table column count:  %d",
                                tableInfo.columnCount );

                AccessibleTableCellInfo tableCellInfo;
                for (int i = 0; i < tableInfo.rowCount; i++) {
                    for (int j = 0; j < tableInfo.columnCount; j++) {

                        if ( !getAccessibleTableCellInfo( vmID,
                                                         tableInfo.accessibleTable,
                                                         i, j,
                                                         &tableCellInfo ) ) {

                            appendToBuffer(
                                buffer, bufsize,
                                "\r\nERROR: GetAccessibleTableCellInfo failed.",
                                bufsize );
                        } else {
                            appendToBuffer( buffer, bufsize,
                                "\r\n\r\n    AccessibleTable cell[%d,%d] info:",
                                i, j );
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Index: %ld", tableCellInfo.index );
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Row extent: %ld",
                                tableCellInfo.rowExtent );
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Column extent: %ld",
                                tableCellInfo.columnExtent );
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Is selected?: %ld",
                                tableCellInfo.isSelected );

                            AccessibleContextInfo cellACInfo;
                            if ( !GetAccessibleContextInfo(
                                    vmID,
                                    tableCellInfo.accessibleContext,
                                    &cellACInfo ) ) {
                                appendToBuffer( buffer, bufsize,
                                    "\r\nERROR: GetAccessibleContextInfo failed for table cell [%d,%d].",
                                    i, j );
                            } else {
                                if ( !getVirtualAccessibleName( vmID,
                                        tableCellInfo.accessibleContext, name,
                                        (sizeof(name) / sizeof(wchar_t)) ) ) {
                                    appendToBuffer( buffer, bufsize,
                                        "\r\n\r\nERROR: getVirtualAccessibleName",
                                        bufsize );
                                } else {
                                    appendToBuffer( buffer, bufsize,
                                                    "\r\n    Name:  %ls", name );
                                }
                                appendToBuffer( buffer, bufsize,
                                                "\r\n    Role:  %ls",
                                                cellACInfo.role );
                            }
                            ReleaseJavaObject( vmID,
                                               tableCellInfo.accessibleContext );
                        }
                    }
                }

                // Get the column headers
                AccessibleTableInfo columnInfo;
                if ( !getAccessibleTableColumnHeader(vmID, ac, &columnInfo)) {
                    appendToBuffer( buffer, bufsize,
                        "\r\nERROR: getAccessibleTableColumnHeader failed.",
                        bufsize );
                } else {
                    appendToBuffer( buffer, bufsize,
                        "\r\n\r\nAccessibleTable column header info:", bufsize );

                    // Core info
                    appendToBuffer( buffer, bufsize,
                                    "\r\n    Column header row count:  %d",
                                    columnInfo.rowCount );
                    appendToBuffer( buffer, bufsize,
                                    "\r\n    Column header column count:  %d",
                                    columnInfo.columnCount );

                }

                // Get the selected rows
                int numSelections =
                    getAccessibleTableRowSelectionCount( vmID,
                                                         tableInfo.accessibleTable );
                appendToBuffer( buffer, bufsize,
                                "\r\n\r\nRow selection count:  %d",
                                numSelections );
                jint *selections = new jint[numSelections];

                if ( !getAccessibleTableRowSelections( vmID,
                                                       tableInfo.accessibleTable,
                                                       numSelections,
                                                       selections ) ) {
                    appendToBuffer( buffer, bufsize,
                        "\r\nERROR: getAccessibleTableRowSelections failed.",
                        bufsize );
                } else {
                    appendToBuffer(buffer, bufsize, "  \r\n  Row selections: ");
                    for (int j = 0; j < numSelections; j++) {
                        appendToBuffer(buffer, bufsize, " %d", selections[j]);
                    }
                }

                // Get column header info
                for (int i = 0; i < columnInfo.columnCount; i++) {
                    if ( !getAccessibleTableCellInfo( vmID,
                                                      columnInfo.accessibleTable,
                                                      0, i, &tableCellInfo ) ) {

                        appendToBuffer( buffer, bufsize,
                            "\r\nERROR: GetAccessibleTableCellInfo failed.",
                            bufsize );
                    } else {
                        appendToBuffer( buffer, bufsize,
                            "\r\n\r\nColumn header [0,%d] cell info.", i );
                        appendToBuffer( buffer, bufsize,
                            "\r\n    Index: %ld", tableCellInfo.index );
                        appendToBuffer( buffer, bufsize,
                            "\r\n    Row extent: %ld",
                            tableCellInfo.rowExtent );
                        appendToBuffer( buffer, bufsize,
                            "\r\n    Column extent: %ld",
                            tableCellInfo.columnExtent );
                        appendToBuffer( buffer, bufsize,
                            "\r\n    Is selected: %ld",
                            tableCellInfo.isSelected );

                        AccessibleContextInfo cellACInfo;
                        if ( !GetAccessibleContextInfo( vmID,
                                tableCellInfo.accessibleContext, &cellACInfo ) ) {
                            appendToBuffer( buffer, bufsize,
                                "\r\nERROR: GetAccessibleContextInfo failed.",
                                bufsize );
                        } else {
                            if ( !getVirtualAccessibleName( vmID,
                                    tableCellInfo.accessibleContext, name,
                                    (sizeof(name) / sizeof(wchar_t)) ) ) {
                                appendToBuffer( buffer, bufsize,
                                    "\r\n\r\nERROR: getVirtualAccessibleName",
                                    bufsize );
                            } else {
                                appendToBuffer( buffer, bufsize,
                                    "\r\n    Name:  %ls", name );
                            }
                            appendToBuffer( buffer, bufsize,
                                "\r\n    Role:  %ls", cellACInfo.role );
                        }
                        ReleaseJavaObject(vmID, tableCellInfo.accessibleContext);
                    }
                }
            }
        }

        /* ===== AccessibleText ===== */

        if (info.accessibleText == TRUE) {
            AccessibleTextInfo textInfo;
            AccessibleTextItemsInfo textItems;
            AccessibleTextSelectionInfo textSelection;
            AccessibleTextRectInfo rectInfo;
            AccessibleTextAttributesInfo attributeInfo;

            appendToBuffer( buffer, bufsize,
                            "\r\n\r\nAccessible Text information:", bufsize);

            if (GetAccessibleTextInfo(vmID, ac, &textInfo, x, y) == TRUE) {
                appendToBuffer( buffer, bufsize,
                    "\r\n    Mouse point at text index:  %d",
                    textInfo.indexAtPoint );
                appendToBuffer( buffer, bufsize,
                    "\r\n    Caret at text index:  %d",
                    textInfo.caretIndex );
                appendToBuffer( buffer, bufsize,
                    "\r\n    Char count:  %d",
                    textInfo.charCount );
            }
            if ( GetAccessibleTextSelectionInfo(vmID, ac, &textSelection) ) {

                appendToBuffer( buffer, bufsize,
                    "\r\n    Selection start index:  %d",
                    textSelection.selectionStartIndex );
                appendToBuffer( buffer, bufsize,
                    "\r\n    Selection end index:  %d",
                    textSelection.selectionEndIndex );
                appendToBuffer( buffer, bufsize,
                    "\r\n    Selected text:  %ls",
                    textSelection.selectedText );
            }

            /* ===== AccessibleText information at the mouse point ===== */

            appendToBuffer( buffer, bufsize,
                "\r\n\r\n    At mouse point index: %d", textInfo.indexAtPoint);

            if (GetAccessibleTextRect(vmID, ac, &rectInfo, textInfo.indexAtPoint)) {

                appendToBuffer( buffer, bufsize,
                    "\r\n        Character bounding rectangle: [%d, %d, %d, %d]",
                    rectInfo.x, rectInfo.y, rectInfo.width, rectInfo.height );
            }

            if ( GetAccessibleTextLineBounds( vmID, ac, textInfo.indexAtPoint,
                                              &start, &end ) ) {
                if ( GetAccessibleTextRange( vmID, ac, start, end, tmpBuf,
                        (sizeof(tmpBuf) / sizeof(wchar_t)) ) ) {
                    appendToBuffer( buffer, bufsize,
                        "\r\n        Line bounds: [%d, %d]", start, end);
                }
            }
            if ( GetAccessibleTextItems( vmID, ac, &textItems,
                                         textInfo.indexAtPoint ) ) {

                appendToBuffer( buffer, bufsize,
                    "\r\n        Character:  %lc", textItems.letter );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Word:  %ls", textItems.word );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Sentence:  %ls", textItems.sentence );
            }

            /* ===== AccessibleText attributes ===== */

            if (GetAccessibleTextAttributes(vmID, ac,
                                            textInfo.indexAtPoint,
                                            &attributeInfo)) {

                appendToBuffer( buffer, bufsize, "\r\n        Core attributes: %s",
                        attributeInfo.bold ? "bold" : "not bold" );
                appendToBuffer( buffer, bufsize, ", %s",
                        attributeInfo.italic ? "italic" : "not italic" );
                appendToBuffer( buffer, bufsize, ", %s",
                        attributeInfo.underline ? "underline" : "not underline" );
                appendToBuffer( buffer, bufsize, ", %s",
                        attributeInfo.strikethrough ? "strikethrough" :
                                                      "not strikethrough" );
                appendToBuffer( buffer, bufsize, ",  %s",
                        attributeInfo.superscript ? "superscript" :
                                                    "not superscript" );
                appendToBuffer( buffer, bufsize, ", %s",
                        attributeInfo.subscript ? "subscript" : "not subscript" );

                appendToBuffer( buffer, bufsize,
                    "\r\n        Background color:  %ls",
                    attributeInfo.backgroundColor );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Foreground color:  %ls",
                    attributeInfo.foregroundColor );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Font family:  %ls",
                    attributeInfo.fontFamily );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Font size:  %d",
                    attributeInfo.fontSize );

                appendToBuffer( buffer, bufsize,
                    "\r\n        First line indent:  %f",
                    attributeInfo.firstLineIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Left indent:  %f",
                    attributeInfo.leftIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Right indent:  %f",
                    attributeInfo.rightIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Line spacing:  %f",
                    attributeInfo.lineSpacing );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Space above:  %f",
                    attributeInfo.spaceAbove );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Space below:  %f",
                    attributeInfo.spaceBelow );

                appendToBuffer( buffer, bufsize,
                    "\r\n        Full attribute string:  %ls",
                    attributeInfo.fullAttributesString );

                // get the attribute run length
                short runLength = -1;
                if ( getTextAttributesInRange( vmID, ac, textInfo.indexAtPoint,
                                               textInfo.indexAtPoint + 100,
                                               &attributeInfo, &runLength ) ) {
                    appendToBuffer( buffer, bufsize,
                        "\r\n        Attribute run:  %d", runLength );
                } else {
                    appendToBuffer( buffer, bufsize,
                        "\r\n        getTextAttributesInRangeFailed" );
                }
            }

            /* ===== AccessibleText information at the caret index ===== */

            appendToBuffer( buffer, bufsize,
                "\r\n\r\n    At caret index: %d", textInfo.caretIndex);

            if (getCaretLocation(vmID, ac, &rectInfo, textInfo.caretIndex)) {
                appendToBuffer( buffer, bufsize,
                    "\r\n        Caret bounding rectangle: [%d, %d, %d, %d]",
                    rectInfo.x, rectInfo.y, rectInfo.width, rectInfo.height );
            }

            if (GetAccessibleTextRect(vmID, ac, &rectInfo, textInfo.caretIndex)) {

                appendToBuffer( buffer, bufsize,
                    "\r\n        Character bounding rectangle: [%d, %d, %d, %d]",
                    rectInfo.x, rectInfo.y, rectInfo.width, rectInfo.height );
            }

            if ( GetAccessibleTextLineBounds( vmID, ac, textInfo.caretIndex,
                                              &start, &end ) ) {
                if ( GetAccessibleTextRange( vmID, ac, start, end, tmpBuf,
                                             (sizeof(tmpBuf) / sizeof(wchar_t)) ) ) {

                    appendToBuffer( buffer, bufsize,
                        "\r\n        Line bounds: [%d, %d]", start, end);
                }
            }
            if (GetAccessibleTextItems(vmID, ac, &textItems, textInfo.caretIndex)) {

                appendToBuffer( buffer, bufsize,
                    "\r\n        Character:  %lc", textItems.letter );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Word:  %ls", textItems.word );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Sentence:  %ls", textItems.sentence );
            }

            /* ===== AccessibleText attributes ===== */

            if (GetAccessibleTextAttributes(vmID, ac, textInfo.caretIndex, &attributeInfo)) {

                appendToBuffer( buffer, bufsize, "\r\n        Core attributes: %s",
                    attributeInfo.bold ? "bold" : "not bold" );
                appendToBuffer( buffer, bufsize, ", %s",
                    attributeInfo.italic ? "italic" : "not italic" );
                appendToBuffer( buffer, bufsize, ", %s",
                    attributeInfo.underline ? "underline" : "not underline" );
                appendToBuffer( buffer, bufsize, ", %s",
                    attributeInfo.strikethrough ? "strikethrough" :
                                                  "not strikethrough" );
                appendToBuffer( buffer, bufsize, ",  %s",
                    attributeInfo.superscript ? "superscript" :
                                                "not superscript" );
                appendToBuffer( buffer, bufsize, ", %s",
                    attributeInfo.subscript ? "subscript" : "not subscript");

                appendToBuffer( buffer, bufsize,
                    "\r\n        Background color:  %ls",
                    attributeInfo.backgroundColor );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Foreground color:  %ls",
                    attributeInfo.foregroundColor );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Font family:  %ls", attributeInfo.fontFamily );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Font size:  %d", attributeInfo.fontSize);


                appendToBuffer( buffer, bufsize,
                    "\r\n        First line indent:  %f",
                    attributeInfo.firstLineIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Left indent:  %f", attributeInfo.leftIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Right indent:  %f", attributeInfo.rightIndent );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Line spacing:  %f", attributeInfo.lineSpacing );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Space above:  %f", attributeInfo.spaceAbove );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Space below:  %f", attributeInfo.spaceBelow );
                appendToBuffer( buffer, bufsize,
                    "\r\n        Full attribute string:  %ls",
                    attributeInfo.fullAttributesString );
                // get the attribute run length
                short runLength = -1;
                if ( getTextAttributesInRange( vmID, ac, textInfo.caretIndex,
                                               textInfo.caretIndex + 100,
                                               &attributeInfo, &runLength ) ) {
                    appendToBuffer( buffer, bufsize,
                        "\r\n        Attribute run:  %d", runLength);
                } else {
                    appendToBuffer( buffer, bufsize,
                        "\r\n        getTextAttributesInRangeFailed" );
                }
            }
        }
    }
    return buffer;
}
