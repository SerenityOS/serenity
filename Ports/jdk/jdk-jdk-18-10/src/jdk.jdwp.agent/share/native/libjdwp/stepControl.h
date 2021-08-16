/*
 * Copyright (c) 1998, 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_STEPCONTROL_H
#define JDWP_STEPCONTROL_H

#include "eventFilter.h"
#include "eventHandler.h"

typedef struct {
    /* Parameters */
    jint granularity;
    jint depth;

    /* State */
    jboolean pending;
    jboolean frameExited;    /* for depth == STEP_OVER or STEP_OUT */
    jboolean fromNative;
    jint fromStackDepth;     /* for all but STEP_INTO STEP_INSTRUCTION */
    jint fromLine;           /* for granularity == STEP_LINE */
    jmethodID method;   /* Where line table came from. */
    jvmtiLineNumberEntry *lineEntries;       /* STEP_LINE */
    jint lineEntryCount;     /* for granularity == STEP_LINE */

    HandlerNode *stepHandlerNode;
    HandlerNode *catchHandlerNode;
    HandlerNode *framePopHandlerNode;
    HandlerNode *methodEnterHandlerNode;
} StepRequest;


void stepControl_initialize(void);
void stepControl_reset(void);

jboolean stepControl_handleStep(JNIEnv *env, jthread thread,
                                jclass clazz, jmethodID method);

jvmtiError stepControl_beginStep(JNIEnv *env, jthread thread,
                                jint size, jint depth, HandlerNode *node);
jvmtiError stepControl_endStep(jthread thread);

void stepControl_clearRequest(jthread thread, StepRequest *step);
void stepControl_resetRequest(jthread thread);

void stepControl_lock(void);
void stepControl_unlock(void);

#endif
