/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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

#ifndef JDWP_INVOKER_H
#define JDWP_INVOKER_H

/* Invoke types */

#define INVOKE_CONSTRUCTOR 1
#define INVOKE_STATIC      2
#define INVOKE_INSTANCE    3

typedef struct InvokeRequest {
    jboolean pending;      /* Is an invoke requested? */
    jboolean started;      /* Is an invoke happening? */
    jboolean available;    /* Is the thread in an invokable state? */
    jboolean detached;     /* Has the requesting debugger detached? */
    jint id;
    /* Input */
    jbyte invokeType;
    jbyte options;
    jclass clazz;
    jmethodID method;
    jobject instance;    /* for INVOKE_INSTANCE only */
    jvalue *arguments;
    jint argumentCount;
    char *methodSignature;
    /* Output */
    jvalue returnValue;  /* if no exception, for all but INVOKE_CONSTRUCTOR */
    jobject exception;   /* NULL if no exception was thrown */
} InvokeRequest;


void invoker_initialize(void);
void invoker_reset(void);

void invoker_lock(void);
void invoker_unlock(void);

void invoker_enableInvokeRequests(jthread thread);
jvmtiError invoker_requestInvoke(jbyte invokeType, jbyte options, jint id,
                           jthread thread, jclass clazz, jmethodID method,
                           jobject instance,
                           jvalue *arguments, jint argumentCount);
jboolean invoker_doInvoke(jthread thread);

void invoker_completeInvokeRequest(jthread thread);
jboolean invoker_isEnabled(jthread thread);
void invoker_detach(InvokeRequest *request);

#endif
