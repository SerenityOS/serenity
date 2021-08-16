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
    jvalue val;
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
    const jboolean is_static;
    jvalue val;
} watch_info;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsExpected = 0;
static int eventsCount = 0;
static watch_info watches[] = {
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 1,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticBoolean", "Z", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 5,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticByte", "B", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 9,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticShort", "S", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 13,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticInt", "I", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 19,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticLong", "J", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 24,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticFloat", "F", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 30,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticDouble", "D", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 35,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticChar", "C", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 41,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticObject", "Ljava/lang/Object;", JNI_TRUE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 47,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "staticArrInt", "[I", JNI_TRUE, {} },

    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 52,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceBoolean", "Z", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 58,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceByte", "B", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 64,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceShort", "S", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 70,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceInt", "I", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 77,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceLong", "J", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 83,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceFloat", "F", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 90,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceDouble", "D", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 96,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceChar", "C", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 103,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceObject", "Ljava/lang/Object;", JNI_FALSE, {} },
    { NULL, "Lnsk/jvmti/FieldModification/fieldmod001a;", "run", "()V", 110,
      "Lnsk/jvmti/FieldModification/fieldmod001a;", "instanceArrInt", "[I", JNI_FALSE, {} }
};

void printValue(jvalue val, char *sig) {
    switch (*sig) {
    case 'J':
        printf("0x%x%08x", (jint)(val.j >> 32), (jint)val.j);
        break;
    case 'F':
        printf("%.3f", (double)val.f);
        break;
    case 'D':
        printf("%f", (double)val.d);
        break;
    case 'L':
    case '[':
        printf("0x%p", val.l);
        break;
    case 'Z':
        printf("0x%x", val.z);
        break;
    case 'B':
        printf("%d", val.b);
        break;
    case 'S':
        printf("%d", val.s);
        break;
    case 'C':
        printf("0x%x", val.c);
        break;
    case 'I':
        printf("%d", val.i);
        break;
    default:
        printf("0x%x%08x", (jint)(val.j >> 32), (jint)val.j);
        break;
    }
}

int isEqual(JNIEnv *env, char *sig, jvalue v1, jvalue v2) {
    switch (*sig) {
    case 'J':
        return (v1.j == v2.j);
    case 'F':
        return (v1.f == v2.f);
    case 'D':
        return (v1.d == v2.d);
    case 'L':
    case '[':
        return env->IsSameObject(v1.l, v2.l);
    case 'Z':
        return (v1.z == v2.z);
    case 'B':
        return (v1.b == v2.b);
    case 'S':
        return (v1.s == v2.s);
    case 'C':
        return (v1.c == v2.c);
    case 'I':
        return (v1.i == v2.i);
    default:
        return (1);
    }
}

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location,
        jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    jvmtiError err;
    jclass cls;
    writable_watch_info watch;
    char *generic;
    size_t i;

    eventsCount++;
    if (printdump == JNI_TRUE) {
        printf(">>> retrieving modification watch info ...\n");
    }
    watch.fid = field;
    watch.loc = location;
    watch.val = new_value;
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
        &watch.f_cls, &generic);
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
        printf(">>>  new value: ");
        printValue(watch.val, watch.f_sig);
        printf("\n");
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
            if (!isEqual((JNIEnv *)env, watch.f_sig, watch.val, watches[i].val)) {
                printf("(watch#%" PRIuPTR ") wrong new value: ", i);
                printValue(watch.val, watch.f_sig);
                printf(", expected: ");
                printValue(watches[i].val, watch.f_sig);
                printf("\n");
                result = STATUS_FAILED;
            }
            return;
        }
    }
    printf("Unexpected field modification catched: 0x%p\n", watch.fid);
    result = STATUS_FAILED;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_fieldmod001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_fieldmod001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_fieldmod001(JavaVM *jvm, char *options, void *reserved) {
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

    if (caps.can_generate_field_modification_events) {
        callbacks.FieldModification = &FieldModification;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_FIELD_MODIFICATION, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable JVMTI_EVENT_FIELD_MODIFICATION: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FieldModification watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_FieldModification_fieldmod001_getReady(JNIEnv *env, jclass klass,
        jobject obj1, jobject obj2, jobject arr1, jobject arr2) {
    jvmtiError err;
    jclass cls;
    size_t i;

    if (!caps.can_generate_field_modification_events) {
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> setting field modification watches ...\n");
    }
    cls = env->FindClass("nsk/jvmti/FieldModification/fieldmod001a");
    if (cls == NULL) {
        printf("Cannot find fieldmod001a class!\n");
        result = STATUS_FAILED;
        return;
    }
    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
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
        err = jvmti->SetFieldModificationWatch(cls, watches[i].fid);
        if (err == JVMTI_ERROR_NONE) {
            eventsExpected++;
        } else {
            printf("(SetFieldModificationWatch#%" PRIuPTR ") unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    watches[0].val.z = JNI_TRUE;
    watches[1].val.b = 1;
    watches[2].val.s = 2;
    watches[3].val.i = 3;
    watches[4].val.j = 4;
    watches[5].val.f = 0.5F;
    watches[6].val.d = 0.6;
    watches[7].val.c = 0x61;
    watches[8].val.l = env->NewGlobalRef(obj1);
    watches[9].val.l = env->NewGlobalRef(arr1);

    watches[10].val.z = JNI_FALSE;
    watches[11].val.b = 10;
    watches[12].val.s = 20;
    watches[13].val.i = 30;
    watches[14].val.j = 40;
    watches[15].val.f = 0.05F;
    watches[16].val.d = 0.06;
    watches[17].val.c = 0x7a;
    watches[18].val.l = env->NewGlobalRef(obj2);
    watches[19].val.l = env->NewGlobalRef(arr2);

    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_FieldModification_fieldmod001_check(JNIEnv *env, jclass cls) {
    if (eventsCount != eventsExpected) {
        printf("Wrong number of field modification events: %d, expected: %d\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
