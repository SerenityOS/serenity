/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_EVENTHANDLER_H
#define JDWP_EVENTHANDLER_H

#include "bag.h"

typedef jint HandlerID;

/* structure is read-only for users */
typedef struct HandlerNode_ {
    HandlerID handlerID;
    EventIndex ei;
    jbyte suspendPolicy;
    jboolean permanent;
    int needReturnValue;
} HandlerNode;

typedef void (*HandlerFunction)(JNIEnv *env,
                                EventInfo *evinfo,
                                HandlerNode *node,
                                struct bag *eventBag);

/***** HandlerNode create = alloc + install *****/

HandlerNode *eventHandler_alloc(jint filterCount, EventIndex ei,
                                jbyte suspendPolicy);
HandlerID eventHandler_allocHandlerID(void);
jvmtiError eventHandler_installExternal(HandlerNode *node);
HandlerNode *eventHandler_createPermanentInternal(EventIndex ei,
                                                  HandlerFunction func);
HandlerNode *eventHandler_createInternalThreadOnly(EventIndex ei,
                                                   HandlerFunction func,
                                                   jthread thread);
HandlerNode *eventHandler_createInternalBreakpoint(HandlerFunction func,
                                                   jthread thread,
                                                   jclass clazz,
                                                   jmethodID method,
                                                   jlocation location);

/***** HandlerNode free *****/

jvmtiError eventHandler_freeAll(EventIndex ei);
jvmtiError eventHandler_freeByID(EventIndex ei, HandlerID handlerID);
jvmtiError eventHandler_free(HandlerNode *node);
void eventHandler_freeClassBreakpoints(jclass clazz);

/***** HandlerNode manipulation *****/

void eventHandler_initialize(jbyte sessionID);
void eventHandler_reset(jbyte sessionID);

void eventHandler_lock(void);
void eventHandler_unlock(void);


jclass getMethodClass(jvmtiEnv *jvmti_env, jmethodID method);

/***** debugging *****/

#ifdef DEBUG
void eventHandler_dumpAllHandlers(jboolean dumpPermanent);
void eventHandler_dumpHandlers(EventIndex ei, jboolean dumpPermanent);
void eventHandler_dumpHandler(HandlerNode *node);
#endif

#endif /* _EVENTHANDLER_H */
