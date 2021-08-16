/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"
#include "jni_tools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *name;
    const char *sig;
    jint value;
} var_info;

typedef struct {
    jboolean isObsolete;
    const char *name;
    const char *sig;
    jint line;
    jint count;
    var_info *vars;
    const char *desc;
} frame_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jbyteArray classBytes;
static jmethodID midRun = NULL;
static jmethodID mid1 = NULL;
static jmethodID mid2 = NULL;
static jfieldID fid1 = NULL;
static jfieldID fid2 = NULL;
static jint stepEventsExpected = 0;
static jint bpEventsExpected = 0;
static jint popEventsExpected = 0;
static jint accessEventsExpected = 0;
static jint modificationEventsExpected = 0;
static jint exceptionEventsExpected = 0;
static jint catchEventsExpected = 0;
static jint stepEventsCount = 0;
static jint bpEventsCount = 0;
static jint popEventsCount = 0;
static jint accessEventsCount = 0;
static jint modificationEventsCount = 0;
static jint exceptionEventsCount = 0;
static jint catchEventsCount = 0;
static jint redefinesCount = 0;

static const char *cls_exp = "Lnsk/jvmti/RedefineClasses/redefclass027a;";

static var_info run[] = {
    { "this", "Lnsk/jvmti/RedefineClasses/redefclass027a;", 0 },
    { "localInt1", "I", 1 }
};

static var_info method1[] = {
    { "this", "Lnsk/jvmti/RedefineClasses/redefclass027a;", 0 },
    { "argInt1", "I", 10 },
    { "localInt2", "I", 2 },
    { "ex", "Ljava/lang/Exception;", 0 }
};

static var_info pop[] = {
    { "this", "Lnsk/jvmti/RedefineClasses/redefclass027a;", 0 },
    { "argInt1", "I", 10 },
    { "localInt2", "I", 2 },
    { "ex", "Ljava/lang/Exception;", 0 }
};

static var_info method2[] = {
    { "this", "Lnsk/jvmti/RedefineClasses/redefclass027a;", 0 },
    { "argInt2", "I", 20 },
    { "localInt4", "I", 4 }
};

static frame_info frames[] = {
    { JNI_FALSE, "run",     "()V",  97,  2, run,     "bp" },
    { JNI_FALSE, "run",     "()V",  97,  2, run,     "step" },
    { JNI_FALSE, "run",     "()V",  99,  2, run,     "mod" },
    { JNI_FALSE, "method2", "(I)V", 116, 3, method2, "acc" },
    { JNI_FALSE, "method2", "(I)V", 117, 3, method2, "exc" },
    { JNI_FALSE, "method1", "(I)V", 108, 4, method1, "catch" },
    { JNI_FALSE, "method1", "(I)V", 112, 4, pop,     "pop" },
};


void check(jvmtiEnv *jvmti_env, jthread thr, jclass cls, jmethodID mid,
        jlocation loc, jint i) {
    jvmtiError err;
    char *sigClass, *name = NULL, *sig = NULL, *generic;
    const char *desc;
    jboolean is_obsolete;
    jvmtiLineNumberEntry *lines = NULL;
    jvmtiLocalVariableEntry *table = NULL;
    jint line = -1;
    jint entryCount = 0;
    jint varValue = -1;
    jint j, k;
    char buffer[32];

    if (i >= (jint) (sizeof(frames)/sizeof(frame_info))) {
        printf("%d -- too many frames\n", i);
        result = STATUS_FAILED;
        return;
    }

    desc = frames[i].desc;

    err = jvmti_env->GetClassSignature(cls, &sigClass, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%s) unexpected error: %s (%d)\n",
               desc, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (sigClass == NULL || strcmp(sigClass, cls_exp) != 0) {
        printf("(%s) wrong class sig: \"%s\",", desc, sigClass);
        printf(" expected: \"%s\"\n", cls_exp);
        if (sigClass != NULL) {
            jvmti_env->Deallocate((unsigned char *)sigClass);
        }
        result = STATUS_FAILED;
    } else {
        err = jvmti_env->GetMethodName(mid, &name, &sig, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodName#%s) unexpected error: %s (%d)\n",
                   desc, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti_env->IsMethodObsolete(mid, &is_obsolete);
        if (err != JVMTI_ERROR_NONE) {
            printf("(IsMethodObsolete#%s) unexpected error: %s (%d)\n",
                   desc, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (printdump == JNI_TRUE) {
            printf(">>> %s: \"%s.%s%s\"%s", desc, sigClass, name, sig,
                (is_obsolete == JNI_TRUE) ? " (obsolete)" : "");
            printf(", location=%s\n", jlong_to_string(loc, buffer));
        }
        if (frames[i].isObsolete != is_obsolete) {
            printf("(%s) %s obsolete method\n", desc,
                (is_obsolete == JNI_TRUE) ? "unexpected" : "should be");
            result = STATUS_FAILED;
        }
        if (name == NULL || strcmp(name, frames[i].name) != 0) {
            printf("(%s) wrong method name: \"%s\",", desc, name);
            printf(" expected: \"%s\"\n", frames[i].name);
            result = STATUS_FAILED;
        }
        if (sig == NULL || strcmp(sig, frames[i].sig) != 0) {
            printf("(%s) wrong method sig: \"%s\",", desc, sig);
            printf(" expected: \"%s\"\n", frames[i].sig);
            result = STATUS_FAILED;
        }

        err = jvmti_env->GetLineNumberTable(mid,
            &entryCount, &lines);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetLineNumberTable#%s) unexpected error: %s (%d)\n",
                   desc, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (lines != NULL && entryCount > 0) {
            for (k = 0; k < entryCount; k++) {
                if (loc < lines[k].start_location) {
                    break;
                }
            }
            line = lines[k-1].line_number;
        }
        if (line != frames[i].line) {
            printf("(%s) wrong line number: %d, expected: %d\n",
                   desc, line, frames[i].line);
            result = STATUS_FAILED;
        }

        err = jvmti_env->GetLocalVariableTable(mid,
            &entryCount, &table);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetLocalVariableTable#%s) unexpected error: %s (%d)\n",
                   desc, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (frames[i].count != entryCount) {
            printf("(%s) wrong number of locals: %d, expected: %d\n",
                   desc, entryCount, frames[i].count);
            result = STATUS_FAILED;
        }

        if (table != NULL) {
          for (k = 0; k < frames[i].count; k++) {
            for (j = 0; j < entryCount; j++) {
              if (strcmp(table[j].name, frames[i].vars[k].name) == 0 &&
                  strcmp(table[j].signature, frames[i].vars[k].sig) == 0) {
                if (printdump == JNI_TRUE) {
                    printf(">>>   var \"%s:%s\": ",
                           table[j].name, table[j].signature);
                    printf("start_location=%s, length=%d",
                        jlong_to_string(table[j].start_location, buffer),
                        table[j].length);
                }
                if (table[j].signature[0] == 'I' &&
                    loc >= table[j].start_location &&
                    loc <= (table[j].start_location + table[j].length)) {
                  err = jvmti_env->GetLocalInt(thr,
                      0, table[j].slot, &varValue);
                  if (err != JVMTI_ERROR_NONE) {
                    printf("(GetLocalInt#%s) unexpected error: %s (%d)\n",
                           desc, TranslateError(err), err);
                    result = STATUS_FAILED;
                  }
                  else {
                    if (printdump == JNI_TRUE) {
                        printf(", tvalue=%d\n", varValue);
                    }
                    if (varValue != frames[i].vars[k].value) {
                        printf("(%s) wrong local var \"%s:%s\" value: %d,",
                               desc, table[j].name, table[j].signature,
                               varValue);
                        printf(" expected: %d\n", frames[i].vars[k].value);
                        result = STATUS_FAILED;
                    }
                  }
                } else if (printdump == JNI_TRUE) {
                    printf("\n");
                }
                break;
              }
            }
            if (j == entryCount) {
                printf("(%s) var \"%s %s\" not found\n",
                       desc, frames[i].vars[k].name, frames[i].vars[k].sig);
                result = STATUS_FAILED;
            }
          }
        }
    }

    if (sigClass != NULL) {
        jvmti_env->Deallocate((unsigned char *)sigClass);
    }
    if (name != NULL) {
        jvmti_env->Deallocate((unsigned char *)name);
    }
    if (sig != NULL) {
        jvmti_env->Deallocate((unsigned char *)sig);
    }
    if (lines != NULL) {
        jvmti_env->Deallocate((unsigned char *)lines);
    }
    if (table != NULL) {
        for (j = 0; j < entryCount; j++) {
            jvmti_env->Deallocate((unsigned char *)(table[j].name));
            jvmti_env->Deallocate((unsigned char *)(table[j].signature));
        }
        jvmti_env->Deallocate((unsigned char *)table);
    }
}

void redefine(jvmtiEnv *jvmti_env, JNIEnv *env, jclass cls) {
    jvmtiClassDefinition classDef;
    jvmtiError err;

    classDef.klass = cls;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    if (printdump == JNI_TRUE) {
        printf(">>> about to call RedefineClasses %d\n", redefinesCount);
    }

    err = jvmti_env->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RedefineClasses#%d) unexpected error: %s (%d)\n",
               redefinesCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }
    redefinesCount++;
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;
    jclass klass;

    if (midRun != method) {
        printf("bp: don't know where we get called from\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> breakpoint in \"run\"\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 0);
    bpEventsCount++;

    err = jvmti_env->ClearBreakpoint(midRun, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ClearBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    redefine(jvmti_env, (JNIEnv *)env, klass);

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_SINGLE_STEP, thread);
    if (err == JVMTI_ERROR_NONE) {
        stepEventsExpected++;
    } else {
        printf("Cannot enable single step: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jlocation location) {
    jvmtiError err;
    jclass klass;

    if (midRun != method) {
        printf("step: don't know where we get called from\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> single step in \"run\"\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 1);
    stepEventsCount++;

    err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_SINGLE_STEP, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot disable single step: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    redefine(jvmti_env, (JNIEnv *)env, klass);

    if (caps.can_generate_exception_events) {
        err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_EXCEPTION, thread);
        if (err == JVMTI_ERROR_NONE) {
            exceptionEventsExpected++;
        } else {
            printf("Cannot enable exception: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location,
        jclass field_klass, jobject obj, jfieldID field, char sig, jvalue new_value) {
    jvmtiError err;
    jclass klass;

    if (printdump == JNI_TRUE) {
        printf(">>> field modification\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 2);
    modificationEventsCount++;

    redefine(jvmti_env, (JNIEnv *)env, klass);
}

void JNICALL FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location,
        jclass field_klass, jobject obj, jfieldID field) {
    jvmtiError err;
    jclass klass;

    if (printdump == JNI_TRUE) {
        printf(">>> field access\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 3);
    accessEventsCount++;

    redefine(jvmti_env, (JNIEnv *)env, klass);
}

void JNICALL Exception(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {
    jvmtiError err;
    jclass klass;

    if (printdump == JNI_TRUE) {
        printf(">>> exception\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 4);
    exceptionEventsCount++;

    err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_EXCEPTION, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot disable exception: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    redefine(jvmti_env, (JNIEnv *)env, klass);

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_EXCEPTION_CATCH, thread);
    if (err == JVMTI_ERROR_NONE) {
        catchEventsExpected++;
    } else {
        printf("Cannot enable exception catch: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location, jobject exception) {
    jvmtiError err;
    jclass klass;

    if (printdump == JNI_TRUE) {
        printf(">>> catch\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, location, 5);
    catchEventsCount++;

    redefine(jvmti_env, (JNIEnv *)env, klass);

    err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_EXCEPTION_CATCH, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot disable exception catch: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (caps.can_generate_frame_pop_events) {
        err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_FRAME_POP, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable FRAME_POP event: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            err = jvmti_env->NotifyFramePop(thread, 0);
            if (err != JVMTI_ERROR_NONE) {
                printf("(NotifyFramePop) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                popEventsExpected++;
            }
        }
    }
}

void JNICALL FramePop(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jboolean wasPopedByException) {
    jvmtiError err;
    jclass klass;
    jmethodID mid;
    jlocation loc;

    if (printdump == JNI_TRUE) {
        printf(">>> frame pop\n");
    }

    err = jvmti_env->GetFrameLocation(thread, 0, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation#pop) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, loc, 6);
    popEventsCount++;

    redefine(jvmti_env, (JNIEnv *)env, klass);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass027(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass027(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass027(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_redefine_classes) {
        printf("Warning: RedefineClasses is not implemented\n");
    }

    if (!caps.can_get_line_numbers) {
        printf("Warning: GetLineNumberTable is not implemented\n");
    }

    if (!caps.can_access_local_variables) {
        printf("Warning: access to local variables is not implemented\n");
    }

    if (caps.can_redefine_classes) {
        if (caps.can_generate_breakpoint_events) {
            callbacks.Breakpoint = &Breakpoint;
        } else {
            printf("Warning: Breakpoint event is not implemented\n");
        }
        if (caps.can_generate_single_step_events) {
            callbacks.SingleStep = &SingleStep;
        } else {
            printf("Warning: SingleStep event is not implemented\n");
        }
        if (caps.can_generate_exception_events) {
            callbacks.Exception = &Exception;
            callbacks.ExceptionCatch = &ExceptionCatch;
        } else {
            printf("Warning: exception events are not implemented\n");
        }
        if (caps.can_generate_frame_pop_events) {
            callbacks.FramePop = &FramePop;
        } else {
            printf("Warning: FramePop event is not implemented\n");
        }
        if (caps.can_generate_field_access_events) {
            callbacks.FieldAccess = &FieldAccess;
        } else {
            printf("Warning: FieldAccess event is not implemented\n");
        }
        if (caps.can_generate_field_modification_events) {
            callbacks.FieldModification = &FieldModification;
        } else {
            printf("Warning: FieldModification event is not implemented\n");
        }
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass027_getReady(JNIEnv *env, jclass cls,
        jclass clazz, jbyteArray bytes) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_redefine_classes ||
        !caps.can_get_line_numbers ||
        !caps.can_access_local_variables) return;

    classBytes = (jbyteArray) env->NewGlobalRef(bytes);

    midRun = env->GetMethodID(clazz, "run", "()V");
    if (midRun == NULL) {
        printf("Cannot find Method ID for method run\n");
        result = STATUS_FAILED;
    }

    mid1 = env->GetMethodID(clazz, "method1", "(I)V");
    if (mid1 == NULL) {
        printf("Cannot find Method ID for method1\n");
        result = STATUS_FAILED;
    }

    mid2 = env->GetMethodID(clazz, "method2", "(I)V");
    if (mid2 == NULL) {
        printf("Cannot find Method ID for method2\n");
        result = STATUS_FAILED;
    }

    if (caps.can_generate_breakpoint_events && midRun != NULL) {
        err = jvmti->SetBreakpoint(midRun, 0);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetBreakpoint) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_BREAKPOINT, NULL);
            if (err != JVMTI_ERROR_NONE) {
                printf("Failed to enable BREAKPOINT event: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                bpEventsExpected++;
            }
        }
    }

    fid1 = env->GetStaticFieldID(clazz, "staticInt", "I");
    if (fid1 == NULL) {
        printf("Cannot find Field ID for staticInt\n");
        result = STATUS_FAILED;
    }

    if (caps.can_generate_field_modification_events && fid1 != NULL) {
        err = jvmti->SetFieldModificationWatch(clazz, fid1);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetFieldModificationWatch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_FIELD_MODIFICATION, NULL);
            if (err != JVMTI_ERROR_NONE) {
                printf("Failed to enable FIELD_MODIFICATION event: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                modificationEventsExpected++;
            }
        }
    }

    fid2 = env->GetFieldID(clazz, "instanceInt", "I");
    if (fid2 == NULL) {
        printf("Cannot find Field ID for instanceInt\n");
        result = STATUS_FAILED;
    }

    if (caps.can_generate_field_access_events && fid2 != NULL) {
        err = jvmti->SetFieldAccessWatch(clazz, fid2);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetFieldAccessWatch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_FIELD_ACCESS, NULL);
            if (err != JVMTI_ERROR_NONE) {
                printf("Failed to enable FIELD_ACCESS event: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                accessEventsExpected++;
            }
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass027_check(JNIEnv *env, jclass cls) {
    if (bpEventsCount != bpEventsExpected) {
        printf("Wrong number of breakpoint events: %d, expected: %d\n",
            bpEventsCount, bpEventsExpected);
        result = STATUS_FAILED;
    }
    if (stepEventsCount != stepEventsExpected) {
        printf("Wrong number of step events: %d, expected: %d\n",
            stepEventsCount, stepEventsExpected);
        result = STATUS_FAILED;
    }
    if (modificationEventsCount != modificationEventsExpected) {
        printf("Wrong number of modification watch events: %d, expected: %d\n",
            modificationEventsCount, modificationEventsExpected);
        result = STATUS_FAILED;
    }
    if (accessEventsCount != accessEventsExpected) {
        printf("Wrong number of access watch events: %d, expected: %d\n",
            accessEventsCount, accessEventsExpected);
        result = STATUS_FAILED;
    }
    if (exceptionEventsCount != exceptionEventsExpected) {
        printf("Wrong number of exception events: %d, expected: %d\n",
            exceptionEventsCount, exceptionEventsExpected);
        result = STATUS_FAILED;
    }
    if (catchEventsCount != catchEventsExpected) {
        printf("Wrong number of catch exception events: %d, expected: %d\n",
            catchEventsCount, catchEventsExpected);
        result = STATUS_FAILED;
    }
    if (popEventsCount != popEventsExpected) {
        printf("Wrong number of frame pop events: %d, expected: %d\n",
            popEventsCount, popEventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
