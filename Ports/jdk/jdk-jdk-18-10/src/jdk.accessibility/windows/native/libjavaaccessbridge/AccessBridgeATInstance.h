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
 * A class to track key AT instance info from the JavaAccessBridge
 */

#include <windows.h>
#include "AccessBridgePackages.h"

#ifndef __AccessBridgeATInstance_H__
#define __AccessBridgeATInstance_H__


/**
 * The AccessBridgeATInstance class.
 */
class AccessBridgeATInstance {
        friend class JavaAccessBridge;

        AccessBridgeATInstance *nextATInstance;
        HWND ourAccessBridgeWindow;
        HWND winAccessBridgeWindow;
        long javaEventMask;
        long accessibilityEventMask;

        // IPC variables
        HANDLE memoryMappedFileMapHandle;       // handle to file map
        char *memoryMappedView;                         // ptr to shared memory
        char memoryMappedFileName[cMemoryMappedNameSize];

public:
        AccessBridgeATInstance(HWND ourABWindow, HWND winABWindow,
                                                   char *memoryFilename,
                                                   AccessBridgeATInstance *next);
        ~AccessBridgeATInstance();
        LRESULT initiateIPC();
        LRESULT sendJavaEventPackage(char *buffer, int bufsize, long eventID);
        LRESULT sendAccessibilityEventPackage(char *buffer, int bufsize, long eventID);
        AccessBridgeATInstance *findABATInstanceFromATHWND(HWND window);
};

#endif
