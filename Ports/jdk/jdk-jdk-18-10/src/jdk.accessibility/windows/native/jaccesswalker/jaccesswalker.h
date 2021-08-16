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

#include <windows.h>   // includes basic windows functionality
#include <stdio.h>
#include <commctrl.h>
#include <jni.h>
#include "jaccesswalkerResource.h"
#include "AccessBridgeCalls.h"
#include "AccessBridgeCallbacks.h"
#include "AccessBridgeDebug.h"

#include <windows.h>
#include <stdio.h>
#include <stdlib.h>
#include <io.h>
#include <direct.h>
#include <process.h>

#include <time.h>

extern FILE *file;

#define null NULL
#define JACCESSWALKER_LOG "jaccesswalker.log"

/**
 * A node in the jaccesswalker tree
 */
class AccessibleNode {

    HWND baseHWND;
    HTREEITEM treeNodeParent;
    long vmID;
    AccessibleContext ac;
    AccessibleNode *parentNode;
    char accessibleName[MAX_STRING_SIZE];
    char accessibleRole[SHORT_STRING_SIZE];

public:
    AccessibleNode(long vmID, AccessibleContext context,
                   AccessibleNode *parent, HWND hWnd,
                   HTREEITEM parentTreeNodeItem);
    ~AccessibleNode();
    void setAccessibleName(char *name);
    void setAccessibleRole(char *role);
    BOOL displayAPIWindow();  // bring up an Accessibility API detail window
};


/**
 * The main application class
 */
class Jaccesswalker {

public:
    Jaccesswalker(int nCmdShow);
    BOOL InitWindow(int windowMode);
    char *getAccessibleInfo( long vmID, AccessibleContext ac, char *buffer,
                             int bufsize );
    void exitjaccesswalker(HWND hWnd);
    void buildAccessibilityTree();
    void addComponentNodes( long vmID, AccessibleContext context,
                            AccessibleNode *parent, HWND hWnd,
                            HTREEITEM treeNodeParent, HWND treeWnd );
};

char *getTimeAndDate();

void displayAndLogText(char *buffer, ...);

LRESULT CALLBACK WinProc (HWND, UINT, WPARAM, LPARAM);

void debugString(char *msg, ...);

LRESULT CALLBACK jaccesswalkerWindowProc( HWND hDlg, UINT message, UINT wParam,
                                          LONG lParam );

BOOL CALLBACK EnumWndProc(HWND hWnd, LPARAM lParam);
BOOL CALLBACK EnumChildProc(HWND hwnd, LPARAM lParam);

HWND CreateATreeView(HWND hwndParent);

LRESULT CALLBACK AccessInfoWindowProc( HWND hWnd, UINT message, UINT wParam,
                                       LONG lParam );

char *getAccessibleInfo( long vmID, AccessibleContext ac, char *buffer,
                         int bufsize );
