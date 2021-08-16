/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_EVENTHELPER_H
#define JDWP_EVENTHELPER_H

#include "bag.h"
#include "invoker.h"

void eventHelper_initialize(jbyte sessionID);
void eventHelper_reset(jbyte sessionID);
struct bag *eventHelper_createEventBag(void);

void eventHelper_recordEvent(EventInfo *evinfo, jint id,
                             jbyte suspendPolicy, struct bag *eventBag);
void eventHelper_recordClassUnload(jint id, char *signature, struct bag *eventBag);
void eventHelper_recordFrameEvent(jint id, jbyte suspendPolicy, EventIndex ei,
                                  jthread thread, jclass clazz,
                                  jmethodID method, jlocation location,
                                  int needReturnValue,
                                  jvalue returnValue,
                                  struct bag *eventBag);

jbyte eventHelper_reportEvents(jbyte sessionID, struct bag *eventBag);
void eventHelper_reportInvokeDone(jbyte sessionID, jthread thread);
void eventHelper_reportVMInit(JNIEnv *env, jbyte sessionID, jthread thread, jbyte suspendPolicy);
void eventHelper_suspendThread(jbyte sessionID, jthread thread);

void eventHelper_holdEvents(void);
void eventHelper_releaseEvents(void);

void eventHelper_lock(void);
void eventHelper_unlock(void);

void commandLoop_sync(void); /* commandLoop sync with cbVMDeath */
void commandLoop_exitVmDeathLockOnError(void);

/*
 * Private interface for coordinating between eventHelper.c: commandLoop()
 * and ThreadReferenceImpl.c: resume() and VirtualMachineImpl.c: resume().
 */
void unblockCommandLoop(void);

#endif
