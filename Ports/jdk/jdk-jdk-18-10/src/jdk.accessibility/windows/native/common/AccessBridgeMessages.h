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
 * Common AccessBridge IPC message definitions
 */

#include <windows.h>
#include <winuser.h>

#ifndef __AccessBridgeMessages_H__
#define __AccessBridgeMessages_H__


// used for messages between AccessBridge dlls to manage IPC
// In the SendMessage call, the third param (WPARAM) is
// the source HWND (ourAccessBridgeWindow in this case),
// and the fourth param (LPARAM) is the size in bytes of
// the package put into shared memory.
#define AB_MEMORY_MAPPED_FILE_SETUP (WM_USER+0x1000)

// used for messages between AccessBridge dlls to manage IPC
// In the SendMessage call, the third param (WPARAM) is
// the source HWND (ourAccessBridgeWindow in this case),
// and the fourth param (LPARAM) is the size in bytes of
// the package put into shared memory.
#define AB_MESSAGE_WAITING (WM_USER+0x1001)

// used for messages from JavaDLL to itself (or perhaps later also
// for messages from WindowsDLL to itself).  Used with PostMessage,
// it is called for deferred processing of messages to send across
// to another DLL (or DLLs)
#define AB_MESSAGE_QUEUED (WM_USER+0x1002)

// used to let other AccessBridge DLLs know that one of the DLLs
// they are communicating with is going away (not reversable)
#define AB_DLL_GOING_AWAY (WM_USER+0x1003)


// used as part of the Memory-Mapped file IPC setup.  The first
// constant is the query, the second the response, that are put
// into the memory mapped file for reading by the opposite DLL
// to verify that communication is working
#define AB_MEMORY_MAPPED_FILE_OK_QUERY "OK?"
#define AB_MEMORY_MAPPED_FILE_OK_ANSWER "OK!"


BOOL initBroadcastMessageIDs();


#endif
