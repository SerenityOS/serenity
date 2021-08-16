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
 * A class to manage queueing of messages for IPC
 */

#include <windows.h>

#ifndef __AccessBridgeMessageQueue_H__
#define __AccessBridgeMessageQueue_H__


enum QueueReturns {
    cQueueEmpty = 0,
    cMoreMessages = 1,
    cQueueInUse,
    cElementPushedOK,
    cQueueFull,
    cQueueOK,
    cQueueBroken                // shouldn't ever happen!
};

class AccessBridgeQueueElement {
    friend class AccessBridgeMessageQueue;
    friend class WinAccessBridge;
    char *buffer;
    int bufsize;
    AccessBridgeQueueElement *next;
    AccessBridgeQueueElement *previous;

public:
    AccessBridgeQueueElement(char *buf, int size);
    ~AccessBridgeQueueElement();
};

class AccessBridgeMessageQueue {
    BOOL queueLocked;
    BOOL queueRemoveLocked;
    AccessBridgeQueueElement *start;
    AccessBridgeQueueElement *end;
    int size;

public:
    AccessBridgeMessageQueue();
    ~AccessBridgeMessageQueue();

    int getEventsWaiting();

    QueueReturns add(AccessBridgeQueueElement *element);
    QueueReturns remove(AccessBridgeQueueElement **element);
    QueueReturns setRemoveLock(BOOL removeLockSetting);
    BOOL getRemoveLockSetting();
};


#endif
