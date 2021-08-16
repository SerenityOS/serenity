/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
#ifndef NSK_SHARE_JVMTI_AOD_H
#define NSK_SHARE_JVMTI_AOD_H

#include <aod.h>
#include <jvmti.h>
#include <jvmti_tools.h>

extern "C" {

#define MAX_STRING_LENGTH 1024

#define PATH_TO_NEW_BYTE_CODE_OPTION "-pathToNewByteCode"

void nsk_jvmti_aod_disableEventAndFinish(const char* agentName, jvmtiEvent event, int success, jvmtiEnv *jvmti, JNIEnv* jni);

void nsk_jvmti_aod_disableEventsAndFinish(const char* agentName, jvmtiEvent events[], int eventsNumber, int success, jvmtiEnv *jvmti, JNIEnv* jni);

/*
 *  Functions which can be used to work with stored agents options when several
 *  agents using the same libary are attached (agents are identified by its jvmti environment)
 */

#define MAX_MULTIPLE_AGENTS 10

int nsk_jvmti_aod_addMultiagentsOptions(jvmtiEnv *agentEnv, Options *options);

Options* nsk_jvmti_aod_getMultiagentsOptions(jvmtiEnv *agentEnv);

/*
 * Auxiliary functions
 */

void nsk_jvmti_aod_deallocate(jvmtiEnv *jvmti, unsigned char* mem);

/*
 * Get class name of the given class and copy it to the given buffer,
 * it attempt to get class name fails buffer contains zero-length string for safety.
 */
int nsk_jvmti_aod_getClassName(jvmtiEnv *jvmti, jclass klass, char classNameBuffer[]);

/*
 * Get name of the given thread and copy it to the given buffer,
 * it attempt to get thread name fails buffer contains zero-length string for safety.
 */
int nsk_jvmti_aod_getThreadName(jvmtiEnv * jvmti, jthread thread, char threadNameBuffer[]);

// events enabling/disabling

#define nsk_jvmti_aod_enableEvent(X,Y)  NSK_JVMTI_VERIFY(X->SetEventNotificationMode(JVMTI_ENABLE, Y, NULL))
#define nsk_jvmti_aod_disableEvent(X,Y) NSK_JVMTI_VERIFY(X->SetEventNotificationMode(JVMTI_DISABLE, Y, NULL))

int nsk_jvmti_aod_enableEvents(jvmtiEnv* jvmti, jvmtiEvent events[], int eventsNumber);
int nsk_jvmti_aod_disableEvents(jvmtiEnv* jvmti, jvmtiEvent events[], int eventsNumber);

// java threads creation

jthread nsk_jvmti_aod_createThread(JNIEnv *jni);

jthread nsk_jvmti_aod_createThreadWithName(JNIEnv *jni, const char* threadName);

// class redefinition

int nsk_jvmti_aod_redefineClass(Options* options, jvmtiEnv* jvmti, jclass classToRedefine, const char* fileName);

// capabilities

void printCapabilities(jvmtiCapabilities caps);

}

#endif /* END OF NSK_SHARE_JVMTI_AOD_H */
