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

#include "AccessBridgeDebug.h"
#include "AccessBridgeJavaVMInstance.h"
#include "AccessBridgeMessages.h"
#include "AccessBridgePackages.h"
#include "accessBridgeResource.h"       // for debugging messages

#include <winbase.h>
#include <jni.h>

// The initialization must only be done one time and to provide for that the initialization
// is now done in WinAccessBridge and the CRITICAL_SECTION memory has been moved to there.
// send memory lock
//CRITICAL_SECTION sendMemoryIPCLock;
extern CRITICAL_SECTION sendMemoryIPCLock;

// protects the javaVMs chain while in use
extern bool isVMInstanceChainInUse;

DEBUG_CODE(extern HWND theDialogWindow);
extern "C" {
    DEBUG_CODE(void AppendToCallInfo(char *s));
}


/**
 *
 *
 */
AccessBridgeJavaVMInstance::AccessBridgeJavaVMInstance(HWND ourABWindow,
                                                       HWND javaABWindow,
                                                       long javaVMID,
                                                       AccessBridgeJavaVMInstance *next) {
    goingAway = FALSE;
    // This should be called once.  Moved to WinAccessBridge c'tor
    //InitializeCriticalSection(&sendMemoryIPCLock);
    ourAccessBridgeWindow = ourABWindow;
    javaAccessBridgeWindow = javaABWindow;
    vmID = javaVMID;
    nextJVMInstance = next;
    memoryMappedFileMapHandle = (HANDLE) 0;
    memoryMappedView = (char *) 0;
    sprintf(memoryMappedFileName, "AccessBridge-%p-%p.mmf",
            ourAccessBridgeWindow, javaAccessBridgeWindow);
}

/**
 *
 *
 */
AccessBridgeJavaVMInstance::~AccessBridgeJavaVMInstance() {
    DEBUG_CODE(char buffer[256]);

    DEBUG_CODE(AppendToCallInfo("***** in AccessBridgeJavaVMInstance::~AccessBridgeJavaVMInstance\r\n"));
    EnterCriticalSection(&sendMemoryIPCLock);

    // if IPC memory mapped file view is valid, unmap it
    goingAway = TRUE;
    if (memoryMappedView != (char *) 0) {
        DEBUG_CODE(sprintf(buffer, "  unmapping memoryMappedView; view = %p\r\n", memoryMappedView));
        DEBUG_CODE(AppendToCallInfo(buffer));
        UnmapViewOfFile(memoryMappedView);
        memoryMappedView = (char *) 0;
    }
    // if IPC memory mapped file handle map is open, close it
    if (memoryMappedFileMapHandle != (HANDLE) 0) {
        DEBUG_CODE(sprintf(buffer, "  closing memoryMappedFileMapHandle; handle = %p\r\n", memoryMappedFileMapHandle));
        DEBUG_CODE(AppendToCallInfo(buffer));
        CloseHandle(memoryMappedFileMapHandle);
        memoryMappedFileMapHandle = (HANDLE) 0;
    }
    LeaveCriticalSection(&sendMemoryIPCLock);

}

/**
 * initiateIPC - sets up the memory-mapped file to do IPC messaging
 *               1 file is created: to handle requests for information
 *               initiated from Windows AT.  The package is placed into
 *               the memory-mapped file (char *memoryMappedView),
 *               and then a special SendMessage() is sent.  When the
 *               JavaDLL returns from SendMessage() processing, the
 *               data will be in memoryMappedView.  The SendMessage()
 *               return value tells us if all is right with the world.
 *
 *               The set-up proces involves creating the memory-mapped
 *               file, and handshaking with the JavaDLL so it knows
 *               about it as well.
 *
 */
LRESULT
AccessBridgeJavaVMInstance::initiateIPC() {
    DEBUG_CODE(char debugBuf[256]);
    DWORD errorCode;

    DEBUG_CODE(AppendToCallInfo(" in AccessBridgeJavaVMInstance::initiateIPC()\r\n"));

    // create Windows-initiated IPC file & map it to a ptr
    memoryMappedFileMapHandle = CreateFileMapping(INVALID_HANDLE_VALUE, NULL,
                                                  PAGE_READWRITE, 0,
                                                  // 8 bytes for return code
                                                  sizeof(WindowsInitiatedPackages) + 8,
                                                  memoryMappedFileName);
    if (memoryMappedFileMapHandle == NULL) {
        errorCode = GetLastError();
        DEBUG_CODE(sprintf(debugBuf, "  Failed to CreateFileMapping for %s, error: %X", memoryMappedFileName, errorCode));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
        return errorCode;
    } else {
        DEBUG_CODE(sprintf(debugBuf, "  CreateFileMapping worked - filename: %s\r\n", memoryMappedFileName));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
    }

    memoryMappedView = (char *) MapViewOfFile(memoryMappedFileMapHandle,
                                              FILE_MAP_READ | FILE_MAP_WRITE,
                                              0, 0, 0);
    if (memoryMappedView == NULL) {
        errorCode = GetLastError();
        DEBUG_CODE(sprintf(debugBuf, "  Failed to MapViewOfFile for %s, error: %X", memoryMappedFileName, errorCode));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
        return errorCode;
    } else {
        DEBUG_CODE(sprintf(debugBuf, "  MapViewOfFile worked - view: %p\r\n", memoryMappedView));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
    }


    // write some data to the memory mapped file
    strcpy(memoryMappedView, AB_MEMORY_MAPPED_FILE_OK_QUERY);


    // inform the JavaDLL that we've a memory mapped file ready for it
    char buffer[sizeof(PackageType) + sizeof(MemoryMappedFileCreatedPackage)];
    PackageType *type = (PackageType *) buffer;
    MemoryMappedFileCreatedPackage *pkg = (MemoryMappedFileCreatedPackage *) (buffer + sizeof(PackageType));
    *type = cMemoryMappedFileCreatedPackage;
    pkg->bridgeWindow = ABHandleToLong(ourAccessBridgeWindow);
    strncpy(pkg->filename, memoryMappedFileName, cMemoryMappedNameSize);
    sendPackage(buffer, sizeof(buffer));


    // look for the JavaDLL's answer to see if it could read the file
    if (strcmp(memoryMappedView, AB_MEMORY_MAPPED_FILE_OK_ANSWER) != 0) {
        DEBUG_CODE(sprintf(debugBuf, "  JavaVM failed to deal with memory mapped file %s\r\n",
                      memoryMappedFileName));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
        return -1;
    } else {
        DEBUG_CODE(sprintf(debugBuf, "  Success!  JavaVM accpeted our file\r\n"));
        DEBUG_CODE(AppendToCallInfo(debugBuf));
    }

    return 0;
}

// -----------------------

/**
 * sendPackage - uses SendMessage(WM_COPYDATA) to do IPC messaging
 *               with the Java AccessBridge DLL
 *
 *               NOTE: WM_COPYDATA is only for one-way IPC; there
 *               is no way to return parameters (especially big ones)
 *               Use sendMemoryPackage() to do that!
 */
LRESULT
AccessBridgeJavaVMInstance::sendPackage(char *buffer, long bufsize) {
    COPYDATASTRUCT toCopy;
    toCopy.dwData = 0;          // 32-bits we could use for something...
    toCopy.cbData = bufsize;
    toCopy.lpData = buffer;

    PrintDebugString("[INFO]: In AccessBridgeVMInstance::sendPackage");
    PrintDebugString("[INFO]:     javaAccessBridgeWindow: %p", javaAccessBridgeWindow);
    /* This was SendMessage.  Normally that is a blocking call.  However, if
     * SendMessage is sent to another process, e.g. another JVM and an incoming
     * SendMessage is pending, control will be passed to the DialogProc to handle
     * the incoming message.  A bug occurred where this allowed an AB_DLL_GOING_AWAY
     * message to be processed deleting an AccessBridgeJavaVMInstance object in
     * the javaVMs chain.  SendMessageTimeout with SMTO_BLOCK set will prevent the
     * calling thread from processing other requests while waiting, i.e control
     * will not be passed to the DialogProc.  Also note that PostMessage or
     * SendNotifyMessage can't be used.  Although they don't allow transfer to
     * the DialogProc they can't be used in cases where pointers are passed.  This
     * is because the referenced memory needs to be available when the other thread
     * gets control.
     */
    UINT flags = SMTO_BLOCK | SMTO_NOTIMEOUTIFNOTHUNG;
    DWORD_PTR out; // not used
    LRESULT lr = SendMessageTimeout( javaAccessBridgeWindow, WM_COPYDATA,
                                     (WPARAM)ourAccessBridgeWindow, (LPARAM)&toCopy,
                                     flags, 4000, &out );
    return lr;
}


/**
 * sendMemoryPackage - uses Memory-Mapped files to do IPC messaging
 *                     with the Java AccessBridge DLL, informing the
 *                     Java AccessBridge DLL via SendMessage that something
 *                     is waiting for it in the shared file...
 *
 *                     In the SendMessage call, the third param (WPARAM) is
 *                     the source HWND (ourAccessBridgeWindow in this case),
 *                     and the fourth param (LPARAM) is the size in bytes of
 *                     the package put into shared memory.
 *
 */
BOOL
AccessBridgeJavaVMInstance::sendMemoryPackage(char *buffer, long bufsize) {

    // Protect against race condition where the memory mapped file is
    // deallocated before the memory package is being sent
    if (goingAway) {
        return FALSE;
    }
    BOOL retval = FALSE;

    DEBUG_CODE(char outputBuf[256]);
    DEBUG_CODE(sprintf(outputBuf, "AccessBridgeJavaVMInstance::sendMemoryPackage(, %d)", bufsize));
    DEBUG_CODE(AppendToCallInfo(outputBuf));

    DEBUG_CODE(PackageType *type = (PackageType *) buffer);
    DEBUG_CODE(if (*type == cGetAccessibleTextRangePackage) {)
        DEBUG_CODE(AppendToCallInfo("  'buffer' contains:"));
        DEBUG_CODE(GetAccessibleTextRangePackage *pkg = (GetAccessibleTextRangePackage *) (buffer + sizeof(PackageType)));
        DEBUG_CODE(sprintf(outputBuf, "    PackageType = %X", *type));
        DEBUG_CODE(AppendToCallInfo(outputBuf));
        DEBUG_CODE(sprintf(outputBuf, "    GetAccessibleTextRange: start = %d, end = %d, rText = %ls",
            pkg->start, pkg->end, pkg->rText));
        DEBUG_CODE(AppendToCallInfo(outputBuf));
    DEBUG_CODE(})

    EnterCriticalSection(&sendMemoryIPCLock);
    {
        // copy the package into shared memory
        if (!goingAway) {
            memcpy(memoryMappedView, buffer, bufsize);

            DEBUG_CODE(PackageType *type = (PackageType *) memoryMappedView);
            DEBUG_CODE(if (*type == cGetAccessibleTextItemsPackage) {)
                DEBUG_CODE(AppendToCallInfo("  'memoryMappedView' now contains:"));
                DEBUG_CODE(GetAccessibleTextItemsPackage *pkg = (GetAccessibleTextItemsPackage *) (buffer + sizeof(PackageType)));
                DEBUG_CODE(sprintf(outputBuf, "    PackageType = %X", *type));
                DEBUG_CODE(AppendToCallInfo(outputBuf));
            DEBUG_CODE(})
        }

        if (!goingAway) {
            // Let the recipient know there is a package waiting for them. The unset byte
            // at end of buffer which will only be set if message is properly received
            char *done = &memoryMappedView[bufsize];
            *done = 0;

            PrintDebugString("[INFO]:     javaAccessBridgeWindow: %p", javaAccessBridgeWindow);
            // See the comment above the call to SendMessageTimeout in SendPackage method above.
            UINT flags = SMTO_BLOCK | SMTO_NOTIMEOUTIFNOTHUNG;
            DWORD_PTR out; // not used
            SendMessageTimeout( javaAccessBridgeWindow, AB_MESSAGE_WAITING, (WPARAM)ourAccessBridgeWindow, (LPARAM)bufsize,
                                flags, 4000, &out );

            // only succeed if message has been properly received
            if(!goingAway) retval = (*done == 1);
        }

        // copy the package back from shared memory
        if (!goingAway) {
            memcpy(buffer, memoryMappedView, bufsize);
        }
    }
    LeaveCriticalSection(&sendMemoryIPCLock);
    return retval;
}


/**
 * findAccessBridgeWindow - walk through linked list from where we are,
 *                          return the HWND of the ABJavaVMInstance that
 *                          matches the passed in vmID; no match: return 0
 *
 */
HWND
AccessBridgeJavaVMInstance::findAccessBridgeWindow(long javaVMID) {
    PrintDebugString("[INFO]: In findAccessBridgeWindow");
    // no need to recurse really
    if (vmID == javaVMID) {
        return javaAccessBridgeWindow;
    } else {
        isVMInstanceChainInUse = true;
        AccessBridgeJavaVMInstance *current = nextJVMInstance;
        while (current != (AccessBridgeJavaVMInstance *) 0) {
            if (current->vmID == javaVMID) {
                isVMInstanceChainInUse = false;
                return current->javaAccessBridgeWindow;
            }
            current = current->nextJVMInstance;
        }
        isVMInstanceChainInUse = false;
    }
    return 0;
}

/**
 * findABJavaVMInstanceFromJavaHWND - walk through linked list from
 *                                    where we are.  Return the
 *                                    AccessBridgeJavaVMInstance
 *                                    of the ABJavaVMInstance that
 *                                    matches the passed in vmID;
 *                                    no match: return 0
 */
AccessBridgeJavaVMInstance *
AccessBridgeJavaVMInstance::findABJavaVMInstanceFromJavaHWND(HWND window) {
    PrintDebugString("[INFO]: In findABJavaInstanceFromJavaHWND");
    // no need to recurse really
    if (javaAccessBridgeWindow == window) {
        return this;
    } else {
        isVMInstanceChainInUse = true;
        AccessBridgeJavaVMInstance *current = nextJVMInstance;
        while (current != (AccessBridgeJavaVMInstance *) 0) {
            if (current->javaAccessBridgeWindow == window) {
                isVMInstanceChainInUse = false;
                return current;
            }
            current = current->nextJVMInstance;
        }
    }
    isVMInstanceChainInUse = false;
    return (AccessBridgeJavaVMInstance *) 0;
}
