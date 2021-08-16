/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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


#define USE_ERROR
#define USE_TRACE

#include "PlatformMidi.h"

#include <stdlib.h>

char* GetInternalErrorStr(INT32 err) {
    switch (err) {
    case MIDI_SUCCESS:          return "";
    case MIDI_NOT_SUPPORTED:    return "feature not supported";
    case MIDI_INVALID_DEVICEID: return "invalid device ID";
    case MIDI_INVALID_HANDLE:   return "internal error: invalid handle";
    case MIDI_OUT_OF_MEMORY:    return "out of memory";
    }
    return NULL;
}

/*
 * internal implementation for getting error string
 */
char* MIDI_IN_InternalGetErrorString(INT32 err) {
    char* result = GetInternalErrorStr(err);

#if USE_PLATFORM_MIDI_IN == TRUE
    if (!result) {
        result = MIDI_IN_GetErrorStr(err);
    }
#endif
    if (!result) {
        result = GetInternalErrorStr(MIDI_NOT_SUPPORTED);
    }
    return result;
}

/*
 * internal implementation for getting error string
 */
char* MIDI_OUT_InternalGetErrorString(INT32 err) {
    char* result = GetInternalErrorStr(err);

#if USE_PLATFORM_MIDI_OUT == TRUE
    if (!result) {
        result = MIDI_OUT_GetErrorStr(err);
    }
#endif
    if (!result) {
        result = GetInternalErrorStr(MIDI_NOT_SUPPORTED);
    }
    return result;
}


#if USE_MIDI_QUEUE == TRUE

// MessageQueue implementation

MidiMessageQueue* MIDI_CreateQueue(int capacity) {
    MidiMessageQueue* queue = (MidiMessageQueue*) malloc(sizeof(MidiMessageQueue) + ((capacity-1) * sizeof(MidiMessage)));
    if (queue) {
        TRACE0("MIDI_CreateQueue\n");
        queue->lock = MIDI_CreateLock();
        queue->capacity = capacity;
        queue->size = 0;
        queue->readIndex = 0;
        queue->writeIndex = 0;
    }
    return queue;
}

void MIDI_DestroyQueue(MidiMessageQueue* queue) {
    if (queue) {
        void* lock = queue->lock;
        MIDI_Lock(lock);
        free(queue);
        MIDI_Unlock(lock);
        MIDI_DestroyLock(lock);
        TRACE0("MIDI_DestroyQueue\n");
    }
}

// if overwrite is true, oldest messages will be overwritten when the queue is full
// returns true, if message has been added
int MIDI_QueueAddShort(MidiMessageQueue* queue, UINT32 packedMsg, INT64 timestamp, int overwrite) {
    if (queue) {
        MIDI_Lock(queue->lock);
        if (queue->size == queue->capacity) {
            TRACE0("MIDI_QueueAddShort: overflow\n");
            if (!overwrite || queue->queue[queue->writeIndex].locked) {
                return FALSE; // failed
            }
            // adjust overwritten readIndex
            queue->readIndex = (queue->readIndex+1) % queue->capacity;
        } else {
            queue->size++;
        }
        TRACE2("MIDI_QueueAddShort. index=%d, size=%d\n", queue->writeIndex, queue->size);
        queue->queue[queue->writeIndex].type = SHORT_MESSAGE;
        queue->queue[queue->writeIndex].data.s.packedMsg = packedMsg;
        queue->queue[queue->writeIndex].timestamp = timestamp;
        queue->writeIndex = (queue->writeIndex+1) % queue->capacity;
        MIDI_Unlock(queue->lock);
        return TRUE;
    }
    return FALSE;
}

int MIDI_QueueAddLong(MidiMessageQueue* queue, UBYTE* data, UINT32 size,
                      INT32 sysexIndex, INT64 timestamp, int overwrite) {
    if (queue) {
        MIDI_Lock(queue->lock);
        if (queue->size == queue->capacity) {
            TRACE0("MIDI_QueueAddLong: overflow\n");
            if (!overwrite || queue->queue[queue->writeIndex].locked) {
                return FALSE; // failed
            }
            // adjust overwritten readIndex
            queue->readIndex = (queue->readIndex+1) % queue->capacity;
        } else {
            queue->size++;
        }
        TRACE2("MIDI_QueueAddLong. index=%d, size=%d\n", queue->writeIndex, queue->size);
        //fprintf(stdout, "MIDI_QueueAddLong sysex-index %d\n", sysexIndex); fflush(stdout);
        queue->queue[queue->writeIndex].type = LONG_MESSAGE;
        queue->queue[queue->writeIndex].data.l.size = size;
        queue->queue[queue->writeIndex].data.l.data = data;
        queue->queue[queue->writeIndex].data.l.index = sysexIndex;
        queue->queue[queue->writeIndex].timestamp = timestamp;
        queue->writeIndex = (queue->writeIndex+1) % queue->capacity;
        MIDI_Unlock(queue->lock);
        return TRUE;
    }
    return FALSE;
}

// returns NULL if no messages in queue.
MidiMessage* MIDI_QueueRead(MidiMessageQueue* queue) {
    MidiMessage* msg = NULL;
    if (queue) {
        MIDI_Lock(queue->lock);
        if (queue->size > 0) {
            msg = &(queue->queue[queue->readIndex]);
            TRACE2("MIDI_QueueRead. index=%d, size=%d\n", queue->readIndex, queue->size);
            msg->locked = TRUE;
        }
        MIDI_Unlock(queue->lock);
    }
    return msg;
}

void MIDI_QueueRemove(MidiMessageQueue* queue, INT32 onlyLocked) {
    if (queue) {
        MIDI_Lock(queue->lock);
        if (queue->size > 0) {
            MidiMessage* msg = &(queue->queue[queue->readIndex]);
            if (!onlyLocked || msg->locked) {
                TRACE2("MIDI_QueueRemove. index=%d, size=%d\n", queue->readIndex, queue->size);
                queue->readIndex = (queue->readIndex+1) % queue->capacity;
                queue->size--;
            }
            msg->locked = FALSE;
        }
        MIDI_Unlock(queue->lock);
    }
}

void MIDI_QueueClear(MidiMessageQueue* queue) {
    if (queue) {
        MIDI_Lock(queue->lock);
        queue->size = 0;
        queue->readIndex = 0;
        queue->writeIndex = 0;
        MIDI_Unlock(queue->lock);
    }
}

#endif
