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
 * A class to track key JVM instance info from the AT WinAccessBridge
 */

#ifndef __AccessBridgeJavaVMInstance_H__
#define __AccessBridgeJavaVMInstance_H__

#include "AccessBridgePackages.h"

#include <jni.h>
#include <windows.h>

/**
 * The AccessBridgeJavaVMInstance class.
 */
class AccessBridgeJavaVMInstance {
        friend class WinAccessBridge;

        AccessBridgeJavaVMInstance *nextJVMInstance;
        HWND ourAccessBridgeWindow;
        HWND javaAccessBridgeWindow;
        long vmID;

        // IPC variables
        HANDLE memoryMappedFileMapHandle;       // handle to file map
        char *memoryMappedView;                         // ptr to shared memory
        char memoryMappedFileName[cMemoryMappedNameSize];
        BOOL goingAway;


public:
        AccessBridgeJavaVMInstance(HWND ourABWindow, HWND javaABWindow,
                                                           long javaVMID,
                                                           AccessBridgeJavaVMInstance *next);
        ~AccessBridgeJavaVMInstance();
        LRESULT initiateIPC();
        LRESULT sendPackage(char *buffer, long bufsize);
        BOOL sendMemoryPackage(char *buffer, long bufsize);
        HWND findAccessBridgeWindow(long javaVMID);
        AccessBridgeJavaVMInstance *findABJavaVMInstanceFromJavaHWND(HWND window);
};

#endif
