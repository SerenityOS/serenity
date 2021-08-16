/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <stdio.h>

#include "jvmti.h"

#define CLASS_NAME "Lcompiler/jsr292/cr8026328/Test8026328;"
#define METHOD_NAME "main"

static jvmtiEnv *jvmti = NULL;


void JNICALL
classprepare(jvmtiEnv* jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jclass klass) {

    char* buf;
    (*jvmti)->GetClassSignature(jvmti,
                klass,
                &buf,
                NULL);
    if (strcmp(CLASS_NAME, buf) == 0) {
        jint nMethods;
        jmethodID* methods;
        int i;
        (*jvmti)->GetClassMethods(jvmti,
                    klass,
                    &nMethods,
                    &methods);
        for (i = 0; i < nMethods; i++) {
            char* name;
            (*jvmti)->GetMethodName(jvmti,
                        methods[i],
                        &name,
                        NULL,
                        NULL);
            if (strcmp(METHOD_NAME, name) == 0) {
                printf("Setting breakpoint\n");
                fflush(stdout);
                (*jvmti)->SetBreakpoint(jvmti, methods[i], 0);
            }
        }
    }
}


void JNICALL
breakpoint(jvmtiEnv* jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jlocation location) {

    jclass declaring_class;
    char* name;
    char* cname;
    (*jvmti)->GetMethodName(jvmti,
                method,
                &name,
                NULL,
                NULL);
    (*jvmti)->GetMethodDeclaringClass(jvmti,
                method,
                &declaring_class);
    (*jvmti)->GetClassSignature(jvmti,
                declaring_class,
                &cname,
                NULL);
    printf("Hit breakpoint at %s::%s:%d\n", cname, name, (int) location);
    fflush(stdout);
}


JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM* vm,
             char* options,
             void* reserved) {

    jvmtiCapabilities capa;
    jvmtiEventCallbacks cbs = {0};

    (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION_1_0);

    memset(&capa, 0, sizeof(capa));
    capa.can_generate_breakpoint_events = 1;
    capa.can_generate_single_step_events = 1;
    (*jvmti)->AddCapabilities(jvmti, &capa);

    cbs.ClassPrepare = classprepare;
    cbs.Breakpoint = breakpoint;
    (*jvmti)->SetEventCallbacks(jvmti, &cbs, sizeof(cbs));
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL);
    printf("Loaded agent\n");
    fflush(stdout);

    return 0;
}
