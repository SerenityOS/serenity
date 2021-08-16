/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    jfieldID fid;
    char *m_cls;
    char *m_name;
    char *m_sig;
    jlocation loc;
    char *f_cls;
    char *f_name;
    char *f_sig;
    jboolean is_static;
} writable_watch_info;

typedef struct {
    jfieldID fid;
    const char *m_cls;
    const char *m_name;
    const char *m_sig;
    jlocation loc;
    const char *f_cls;
    const char *f_name;
    const char *f_sig;
    jboolean is_static;
} watch_info;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsExpected = 0;
static int eventsCount = 0;
static watch_info watches[] = {
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 3,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsBoolean", "Z", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 14,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsByte", "B", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 25,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsShort", "S", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 36,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsInt", "I", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 47,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsLong", "J", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 61,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsFloat", "F", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 74,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsDouble", "D", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 88,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsChar", "C", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 100,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsObject", "Ljava/lang/Object;", JNI_FALSE },
    { NULL, "Lnsk/jvmti/FieldAccess/fieldacc003a;", "run", "()I", 111,
      "Lnsk/jvmti/FieldAccess/fieldacc003a;", "extendsArrInt", "[I", JNI_FALSE }
};

void JNICALL FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jlocation location, jclass field_klass, jobject obj, jfieldID field) {
    jvmtiError err;
    jclass cls;
    writable_watch_info watch;
    char *generic;
    size_t i;

    eventsCount++;
    if (printdump == JNI_TRUE) {
        printf(">>> retrieving access watch info ...\n");
    }
    watch.fid = field;
    watch.loc = location;
    watch.is_static = (obj == NULL) ? JNI_TRUE : JNI_FALSE;
    err = jvmti_env->GetMethodDeclaringClass(method, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassSignature(cls,
        &watch.m_cls, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetMethodName(method,
        &watch.m_name, &watch.m_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassSignature(field_klass,
        &watch.f_cls,  &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetFieldName(field_klass, field,
        &watch.f_name, &watch.f_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFieldName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (printdump == JNI_TRUE) {
        printf(">>>      class: \"%s\"\n", watch.m_cls);
        printf(">>>     method: \"%s%s\"\n", watch.m_name, watch.m_sig);
        printf(">>>   location: 0x%x%08x\n",
            (jint)(watch.loc >> 32), (jint)watch.loc);
        printf(">>>  field cls: \"%s\"\n", watch.f_cls);
        printf(">>>      field: \"%s:%s\"\n", watch.f_name, watch.f_sig);
        printf(">>>     object: 0x%p\n", obj);
        printf(">>> ... done\n");
    }
    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
        if (watch.fid == watches[i].fid) {
            if (watch.m_cls == NULL ||
                    strcmp(watch.m_cls, watches[i].m_cls) != 0) {
                printf("(watch#%" PRIuPTR ") wrong class: \"%s\", expected: \"%s\"\n",
                       i, watch.m_cls, watches[i].m_cls);
                result = STATUS_FAILED;
            }
            if (watch.m_name == NULL ||
                    strcmp(watch.m_name, watches[i].m_name) != 0) {
                printf("(watch#%" PRIuPTR ") wrong method name: \"%s\"",
                       i, watch.m_name);
                printf(", expected: \"%s\"\n", watches[i].m_name);
                result = STATUS_FAILED;
            }
            if (watch.m_sig == NULL ||
                    strcmp(watch.m_sig, watches[i].m_sig) != 0) {
                printf("(watch#%" PRIuPTR ") wrong method sig: \"%s\"",
                       i, watch.m_sig);
                printf(", expected: \"%s\"\n", watches[i].m_sig);
                result = STATUS_FAILED;
            }
            if (watch.loc != watches[i].loc) {
                printf("(watch#%" PRIuPTR ") wrong location: 0x%x%08x",
                       i, (jint)(watch.loc >> 32), (jint)watch.loc);
                printf(", expected: 0x%x%08x\n",
                       (jint)(watches[i].loc >> 32), (jint)watches[i].loc);
                result = STATUS_FAILED;
            }
            if (watch.f_name == NULL ||
                    strcmp(watch.f_name, watches[i].f_name) != 0) {
                printf("(watch#%" PRIuPTR ") wrong field name: \"%s\"",
                       i, watch.f_name);
                printf(", expected: \"%s\"\n", watches[i].f_name);
                result = STATUS_FAILED;
            }
            if (watch.f_sig == NULL ||
                    strcmp(watch.f_sig, watches[i].f_sig) != 0) {
                printf("(watch#%" PRIuPTR ") wrong field sig: \"%s\"",
                       i, watch.f_sig);
                printf(", expected: \"%s\"\n", watches[i].f_sig);
                result = STATUS_FAILED;
            }
            if (watch.is_static != watches[i].is_static) {
                printf("(watch#%" PRIuPTR ") wrong field type: %s", i,
                    (watch.is_static == JNI_TRUE) ? "static" : "instance");
                printf(", expected: %s\n",
                    (watches[i].is_static == JNI_TRUE) ? "static" : "instance");
                result = STATUS_FAILED;
            }
            return;
        }
    }
    printf("Unexpected field access catched: 0x%p\n", watch.fid);
    result = STATUS_FAILED;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_fieldacc003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_fieldacc003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_fieldacc003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (caps.can_generate_field_access_events) {
        callbacks.FieldAccess = &FieldAccess;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_FIELD_ACCESS, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable JVMTI_EVENT_FIELD_ACCESS: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FieldAccess watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_FieldAccess_fieldacc003_getReady(JNIEnv *env, jclass klass) {
    jvmtiError err;
    jclass cls;
    size_t i;

    if (!caps.can_generate_field_access_events) {
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> setting field access watches ...\n");
    }
    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
        cls = env->FindClass(watches[i].f_cls);
        if (cls == NULL) {
            printf("Cannot find %s class!\n", watches[i].f_cls);
            result = STATUS_FAILED;
            return;
        }
        if (watches[i].is_static == JNI_TRUE) {
            watches[i].fid = env->GetStaticFieldID(
                cls, watches[i].f_name, watches[i].f_sig);
        } else {
            watches[i].fid = env->GetFieldID(
                cls, watches[i].f_name, watches[i].f_sig);
        }
        if (watches[i].fid == NULL) {
            printf("Cannot get field ID for \"%s:%s\"\n",
                   watches[i].f_name, watches[i].f_sig);
            result = STATUS_FAILED;
            return;
        }
        err = jvmti->SetFieldAccessWatch(cls, watches[i].fid);
        if (err == JVMTI_ERROR_NONE) {
            eventsExpected++;
        } else {
            printf("(SetFieldAccessWatch#%" PRIuPTR ") unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_FieldAccess_fieldacc003_check(JNIEnv *env, jclass cls) {
    if (eventsCount != eventsExpected) {
        printf("Wrong number of field access events: %d, expected: %d\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
