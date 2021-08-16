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

#include "AccessBridgeDebug.h"
#include "AccessBridgeATInstance.h"
#include "AccessBridgeMessages.h"

#include <windows.h>
#include <winbase.h>


/**
 *  AccessBridgeATInstance constructor
 */
AccessBridgeATInstance::AccessBridgeATInstance(HWND ourABWindow, HWND winABWindow,
                                               char *memoryFilename,
                                               AccessBridgeATInstance *next) {
    ourAccessBridgeWindow = ourABWindow;
    winAccessBridgeWindow = winABWindow;
    nextATInstance = next;
    javaEventMask = 0;
    accessibilityEventMask = 0;
    strncpy(memoryMappedFileName, memoryFilename, cMemoryMappedNameSize);
}

/**
 * AccessBridgeATInstance descructor
 */
AccessBridgeATInstance::~AccessBridgeATInstance() {
    PrintDebugString("[INFO]: in AccessBridgeATInstance::~AccessBridgeATInstance");

    // if IPC memory mapped file view is valid, unmap it
    if (memoryMappedView != (char *) 0) {
        PrintDebugString("[INFO]:   unmapping memoryMappedView; view = %p", memoryMappedView);
        UnmapViewOfFile(memoryMappedView);
        memoryMappedView = (char *) 0;
    }
    // if IPC memory mapped file handle map is open, close it
    if (memoryMappedFileMapHandle != (HANDLE) 0) {
        PrintDebugString("[INFO]:   closing memoryMappedFileMapHandle; handle = %p", memoryMappedFileMapHandle);
        CloseHandle(memoryMappedFileMapHandle);
        memoryMappedFileMapHandle = (HANDLE) 0;
    }
}

/**
 * Sets up the memory-mapped file to do IPC messaging
 * 1 files is created: to handle requests for information
 * initiated from Windows AT.  The package is placed into
 * the memory-mapped file (char *memoryMappedView),
 * and then a special SendMessage() is sent.  When the
 * JavaDLL returns from SendMessage() processing, the
 * data will be in memoryMappedView.  The SendMessage()
 * return value tells us if all is right with the world.
 *
 * The set-up proces involves creating the memory-mapped
 * file, and writing a special string to it so that the
 * WindowsDLL so it knows about it as well.
 */
LRESULT
AccessBridgeATInstance::initiateIPC() {
    DWORD errorCode;

    PrintDebugString("[INFO]: In AccessBridgeATInstance::initiateIPC()");

    // open Windows-initiated IPC filemap & map it to a ptr

    memoryMappedFileMapHandle = OpenFileMapping(FILE_MAP_READ | FILE_MAP_WRITE,
                                                FALSE, memoryMappedFileName);
    if (memoryMappedFileMapHandle == NULL) {
        errorCode = GetLastError();
        PrintDebugString("[ERROR]:   Failed to CreateFileMapping for %s, error: %X", memoryMappedFileName, errorCode);
        return errorCode;
    } else {
        PrintDebugString("[INFO]:   CreateFileMapping worked - filename: %s", memoryMappedFileName);
    }

    memoryMappedView = (char *) MapViewOfFile(memoryMappedFileMapHandle,
                                              FILE_MAP_READ | FILE_MAP_WRITE,
                                              0, 0, 0);
    if (memoryMappedView == NULL) {
        errorCode = GetLastError();
        PrintDebugString("[ERROR]:   Failed to MapViewOfFile for %s, error: %X", memoryMappedFileName, errorCode);
        return errorCode;
    } else {
        PrintDebugString("[INFO]:   MapViewOfFile worked - view: %p", memoryMappedView);
    }


    // look for the JavaDLL's answer to see if it could read the file
    if (strcmp(memoryMappedView, AB_MEMORY_MAPPED_FILE_OK_QUERY) != 0) {
        PrintDebugString("[ERROR]:   JavaVM failed to write to memory mapped file %s",
                         memoryMappedFileName);
        return -1;
    } else {
        PrintDebugString("[INFO]:   JavaVM successfully wrote to file!");
    }


    // write some data to the memory mapped file for WindowsDLL to verify
    strcpy(memoryMappedView, AB_MEMORY_MAPPED_FILE_OK_ANSWER);


    return 0;
}


typedef struct EVENT_STRUCT
{
    char *buffer;
    int bufsize;
    ABHWND64 winAccessBridgeWindow;
    ABHWND64 ourAccessBridgeWindow;
}EVENT_STRUCT;


#include <process.h>
#define THREAD_PROC unsigned int __stdcall
typedef unsigned int (__stdcall *THREAD_ROUTINE)(LPVOID lpThreadParameter);

static HANDLE BeginThread(THREAD_ROUTINE thread_func,DWORD *id,DWORD param)
{
    HANDLE ret;
    ret = (HANDLE) _beginthreadex(NULL,0,thread_func,(void *)param,0,(unsigned int *)id);
    if(ret == INVALID_HANDLE_VALUE)
        ret = NULL;
    return(ret);
}

DWORD JavaBridgeThreadId = 0;

static THREAD_PROC JavaBridgeThread(LPVOID param1)
{
    MSG msg;
    DWORD rc = 0;
    while (GetMessage(&msg,        // message structure
                      NULL,                  // handle of window receiving the message
                      0,                  // lowest message to examine
                      0))                 // highest message to examine
        {
            if(msg.message == WM_USER)
                {
                    EVENT_STRUCT *event_struct = (EVENT_STRUCT *)msg.wParam;
                    COPYDATASTRUCT toCopy;
                    toCopy.dwData = 0;          // 32-bits we could use for something...
                    toCopy.cbData = event_struct->bufsize;
                    toCopy.lpData = event_struct->buffer;

                    LRESULT ret = SendMessage((HWND)ABLongToHandle(event_struct->winAccessBridgeWindow), WM_COPYDATA,
                                              (WPARAM)event_struct->ourAccessBridgeWindow, (LPARAM) &toCopy);
                    delete event_struct->buffer;
                    delete event_struct;
                }
            if(msg.message == (WM_USER+1))
                PostQuitMessage(0);
        }
    JavaBridgeThreadId = 0;
    return(0);
}

/*
 * Handles one event
 */
static void do_event(char *buffer, int bufsize,HWND ourAccessBridgeWindow,HWND winAccessBridgeWindow)
{
    EVENT_STRUCT *event_struct = new EVENT_STRUCT;
    event_struct->bufsize = bufsize;
    event_struct->buffer = new char[bufsize];
    memcpy(event_struct->buffer,buffer,bufsize);
    event_struct->ourAccessBridgeWindow = ABHandleToLong(ourAccessBridgeWindow);
    event_struct->winAccessBridgeWindow = ABHandleToLong(winAccessBridgeWindow);
    if(!JavaBridgeThreadId)
        {
            HANDLE JavaBridgeThreadHandle = BeginThread(JavaBridgeThread,&JavaBridgeThreadId,(DWORD)event_struct);
            CloseHandle(JavaBridgeThreadHandle);
        }
    PostThreadMessage(JavaBridgeThreadId,WM_USER,(WPARAM)event_struct,0);
}


/**
 * sendJavaEventPackage - uses SendMessage(WM_COPYDATA) to do
 *                        IPC messaging with the Java AccessBridge DLL
 *                        to propogate events to those ATs that want 'em
 *
 */
LRESULT
AccessBridgeATInstance::sendJavaEventPackage(char *buffer, int bufsize, long eventID) {

    PrintDebugString("[INFO]: AccessBridgeATInstance::sendJavaEventPackage() eventID = %X", eventID);
    PrintDebugString("[INFO]: AccessBridgeATInstance::sendJavaEventPackage() (using PostMessage) eventID = %X", eventID);

    if (eventID & javaEventMask) {
        do_event(buffer,bufsize,ourAccessBridgeWindow,winAccessBridgeWindow);
        return(0);
    } else {
        return -1;
    }
}


/**
 * uses SendMessage(WM_COPYDATA) to do
 * IPC messaging with the Java AccessBridge DLL
 * to propogate events to those ATs that want 'em
 *
 */
LRESULT
AccessBridgeATInstance::sendAccessibilityEventPackage(char *buffer, int bufsize, long eventID) {

    PrintDebugString("[INFO]: AccessBridgeATInstance::sendAccessibilityEventPackage() eventID = %X", eventID);

    if (eventID & accessibilityEventMask) {
        do_event(buffer,bufsize,ourAccessBridgeWindow,winAccessBridgeWindow);
        return(0);
    } else {
        return -1;
    }
}


/**
 * findABATInstanceFromATHWND - walk through linked list from
 *                              where we are.  Return the
 *                              AccessBridgeATInstance
 *                              of the ABATInstance that
 *                              matches the passed in vmID;
 *                              no match: return 0
 */
AccessBridgeATInstance *
AccessBridgeATInstance::findABATInstanceFromATHWND(HWND window) {
    // no need to recurse really
    if (winAccessBridgeWindow == window) {
        return this;
    } else {
        AccessBridgeATInstance *current = nextATInstance;
        while (current != (AccessBridgeATInstance *) 0) {
            if (current->winAccessBridgeWindow == window) {
                return current;
            }
            current = current->nextATInstance;
        }
    }
    return (AccessBridgeATInstance *) 0;
}
