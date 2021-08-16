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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef enum {
    opc_iinc = 132,
    opc_tableswitch = 170,
    opc_lookupswitch = 171,
    opc_wide = 196
} opcode_type;

typedef struct {
    const char *name;
    unsigned char code;
    int length;
} opcode_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsCount = 0;
static opcode_info opcodes[] = {
    { "nop", 0, 1 },
    { "aconst_null", 1, 1 },
    { "iconst_m1", 2, 1 },
    { "iconst_0", 3, 1 },
    { "iconst_1", 4, 1 },
    { "iconst_2", 5, 1 },
    { "iconst_3", 6, 1 },
    { "iconst_4", 7, 1 },
    { "iconst_5", 8, 1 },
    { "lconst_0", 9, 1 },
    { "lconst_1", 10, 1 },
    { "fconst_0", 11, 1 },
    { "fconst_1", 12, 1 },
    { "fconst_2", 13, 1 },
    { "dconst_0", 14, 1 },
    { "dconst_1", 15, 1 },
    { "bipush", 16, 2 },
    { "sipush", 17, 3 },
    { "ldc", 18, 2 },
    { "ldc_w", 19, 3 },
    { "ldc2_w", 20, 3 },
    { "iload", 21, 2 },
    { "lload", 22, 2 },
    { "fload", 23, 2 },
    { "dload", 24, 2 },
    { "aload", 25, 2 },
    { "iload_0", 26, 1 },
    { "iload_1", 27, 1 },
    { "iload_2", 28, 1 },
    { "iload_3", 29, 1 },
    { "lload_0", 30, 1 },
    { "lload_1", 31, 1 },
    { "lload_2", 32, 1 },
    { "lload_3", 33, 1 },
    { "fload_0", 34, 1 },
    { "fload_1", 35, 1 },
    { "fload_2", 36, 1 },
    { "fload_3", 37, 1 },
    { "dload_0", 38, 1 },
    { "dload_1", 39, 1 },
    { "dload_2", 40, 1 },
    { "dload_3", 41, 1 },
    { "aload_0", 42, 1 },
    { "aload_1", 43, 1 },
    { "aload_2", 44, 1 },
    { "aload_3", 45, 1 },
    { "iaload", 46, 1 },
    { "laload", 47, 1 },
    { "faload", 48, 1 },
    { "daload", 49, 1 },
    { "aaload", 50, 1 },
    { "baload", 51, 1 },
    { "caload", 52, 1 },
    { "saload", 53, 1 },
    { "istore", 54, 2 },
    { "lstore", 55, 2 },
    { "fstore", 56, 2 },
    { "dstore", 57, 2 },
    { "astore", 58, 2 },
    { "istore_0", 59, 1 },
    { "istore_1", 60, 1 },
    { "istore_2", 61, 1 },
    { "istore_3", 62, 1 },
    { "lstore_0", 63, 1 },
    { "lstore_1", 64, 1 },
    { "lstore_2", 65, 1 },
    { "lstore_3", 66, 1 },
    { "fstore_0", 67, 1 },
    { "fstore_1", 68, 1 },
    { "fstore_2", 69, 1 },
    { "fstore_3", 70, 1 },
    { "dstore_0", 71, 1 },
    { "dstore_1", 72, 1 },
    { "dstore_2", 73, 1 },
    { "dstore_3", 74, 1 },
    { "astore_0", 75, 1 },
    { "astore_1", 76, 1 },
    { "astore_2", 77, 1 },
    { "astore_3", 78, 1 },
    { "iastore", 79, 1 },
    { "lastore", 80, 1 },
    { "fastore", 81, 1 },
    { "dastore", 82, 1 },
    { "aastore", 83, 1 },
    { "bastore", 84, 1 },
    { "castore", 85, 1 },
    { "sastore", 86, 1 },
    { "pop", 87, 1 },
    { "pop2", 88, 1 },
    { "dup", 89, 1 },
    { "dup_x1", 90, 1 },
    { "dup_x2", 91, 1 },
    { "dup2", 92, 1 },
    { "dup2_x1", 93, 1 },
    { "dup2_x2", 94, 1 },
    { "swap", 95, 1 },
    { "iadd", 96, 1 },
    { "ladd", 97, 1 },
    { "fadd", 98, 1 },
    { "dadd", 99, 1 },
    { "isub", 100, 1 },
    { "lsub", 101, 1 },
    { "fsub", 102, 1 },
    { "dsub", 103, 1 },
    { "imul", 104, 1 },
    { "lmul", 105, 1 },
    { "fmul", 106, 1 },
    { "dmul", 107, 1 },
    { "idiv", 108, 1 },
    { "ldiv", 109, 1 },
    { "fdiv", 110, 1 },
    { "ddiv", 111, 1 },
    { "irem", 112, 1 },
    { "lrem", 113, 1 },
    { "frem", 114, 1 },
    { "drem", 115, 1 },
    { "ineg", 116, 1 },
    { "lneg", 117, 1 },
    { "fneg", 118, 1 },
    { "dneg", 119, 1 },
    { "ishl", 120, 1 },
    { "lshl", 121, 1 },
    { "ishr", 122, 1 },
    { "lshr", 123, 1 },
    { "iushr", 124, 1 },
    { "lushr", 125, 1 },
    { "iand", 126, 1 },
    { "land", 127, 1 },
    { "ior", 128, 1 },
    { "lor", 129, 1 },
    { "ixor", 130, 1 },
    { "lxor", 131, 1 },
    { "iinc", 132, 3 },
    { "i2l", 133, 1 },
    { "i2f", 134, 1 },
    { "i2d", 135, 1 },
    { "l2i", 136, 1 },
    { "l2f", 137, 1 },
    { "l2d", 138, 1 },
    { "f2i", 139, 1 },
    { "f2l", 140, 1 },
    { "f2d", 141, 1 },
    { "d2i", 142, 1 },
    { "d2l", 143, 1 },
    { "d2f", 144, 1 },
    { "i2b", 145, 1 },
    { "i2c", 146, 1 },
    { "i2s", 147, 1 },
    { "lcmp", 148, 1 },
    { "fcmpl", 149, 1 },
    { "fcmpg", 150, 1 },
    { "dcmpl", 151, 1 },
    { "dcmpg", 152, 1 },
    { "ifeq", 153, 3 },
    { "ifne", 154, 3 },
    { "iflt", 155, 3 },
    { "ifge", 156, 3 },
    { "ifgt", 157, 3 },
    { "ifle", 158, 3 },
    { "if_icmpeq", 159, 3 },
    { "if_icmpne", 160, 3 },
    { "if_icmplt", 161, 3 },
    { "if_icmpge", 162, 3 },
    { "if_icmpgt", 163, 3 },
    { "if_icmple", 164, 3 },
    { "if_acmpeq", 165, 3 },
    { "if_acmpne", 166, 3 },
    { "goto", 167, 3 },
    { "jsr", 168, 3 },
    { "ret", 169, 2 },
    { "tableswitch", 170, 0 },
    { "lookupswitch", 171, 0 },
    { "ireturn", 172, 1 },
    { "lreturn", 173, 1 },
    { "freturn", 174, 1 },
    { "dreturn", 175, 1 },
    { "areturn", 176, 1 },
    { "return", 177, 1 },
    { "getstatic", 178, 3 },
    { "putstatic", 179, 3 },
    { "getfield", 180, 3 },
    { "putfield", 181, 3 },
    { "invokevirtual", 182, 3 },
    { "invokespecial", 183, 3 },
    { "invokestatic", 184, 3 },
    { "invokeinterface", 185, 5 },
    { "invokedynamic", 186, 5 },
    { "new", 187, 3 },
    { "newarray", 188, 2 },
    { "anewarray", 189, 3 },
    { "arraylength", 190, 1 },
    { "athrow", 191, 1 },
    { "checkcast", 192, 3 },
    { "instanceof", 193, 3 },
    { "monitorenter", 194, 1 },
    { "monitorexit", 195, 1 },
    { "wide", 196, 0 },
    { "multianewarray", 197, 4 },
    { "ifnull", 198, 3 },
    { "ifnonnull", 199, 3 },
    { "goto_w", 200, 5 },
    { "jsr_w", 201, 5 },
    { "breakpoint", 202, 1 },
    { "impdep1", 254, 1 },
    { "impdep2", 255, 1 }
};

jint get_u4(unsigned char *p) {
    return (jint)p[3] | ((jint)p[2]<<8) | ((jint)p[1]<<16) | ((jint)p[0]<<24);
}

jboolean checkCode(jint bytecodeCount, unsigned char *buf) {
    unsigned char code;
    jint pc, cur_pc, length;
    size_t i;

    for (pc = 0; pc >= 0 && pc < bytecodeCount; pc += length) {
        code = buf[pc];
        for (i = 0; i < sizeof(opcodes)/sizeof(opcode_info); i++) {
            if (code == opcodes[i].code) {
                switch (code) {
                case opc_wide:
                    length = (buf[pc + 1] == opc_iinc ? 6 : 4);
                    break;
                case opc_lookupswitch:
                    cur_pc = (pc + 4) & (~3);
                    length = cur_pc - pc + 8;
                    length += get_u4(buf + cur_pc + 4) * 8;
                    break;
                case opc_tableswitch:
                    cur_pc = (pc + 4) & (~3);
                    length = cur_pc - pc + 12;
                    length += (get_u4(buf + cur_pc + 8) -
                               get_u4(buf + cur_pc + 4) + 1) * 4;
                    break;
                default:
                    length = opcodes[i].length;
                    break;
                }
                if (printdump == JNI_TRUE) {
                    printf(">>>     %4d: %s (%d)\n",
                           pc, opcodes[i].name, length);
                }
                if (length <= 0) {
                    printf("Invalid length: %d for opcode \"%s\" (%d)\n",
                           length, opcodes[i].name, code);
                    return JNI_FALSE;
                }
                break;
            }
        }
        if (i >= sizeof(opcodes)/sizeof(opcode_info)) {
            /* opcode not found */
            printf("Non-standard opcode: %d (0x%x)\n", code, code);
            return JNI_FALSE;
        }
    }
    return JNI_TRUE;
}

void JNICALL ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jclass cls) {
    jvmtiError err;
    char *sig, *name, *msig;
    jint mcount;
    jmethodID *methods;
    jboolean isNative;
    jint bytecodeCount;
    unsigned char *bytecodes;
    jint i;

    sig = NULL;
    err = jvmti_env->GetClassSignature(cls, &sig, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassMethods(cls, &mcount, &methods);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassMethods#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [class prepare event #%d]", eventsCount);
        printf(" \"%s\"\n", sig);
        printf(">>>   %d methods:\n", mcount);
    }

    for (i = 0; i < mcount; i++) {
        if (methods[i] == NULL) {
            if (printdump == JNI_TRUE) {
                printf(" null");
            }
        } else {
            name = NULL;
            msig = NULL;
            bytecodes = NULL;
            err = jvmti_env->GetMethodName(methods[i], &name, &msig, NULL);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetMethodName) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                printf("  class: \"%s\"\n", sig);
                result = STATUS_FAILED;
                return;
            }
            isNative = JNI_TRUE;
            err = jvmti_env->IsMethodNative(methods[i], &isNative);
            if (err != JVMTI_ERROR_NONE) {
                printf("(IsMethodNative) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                printf("  class: \"%s\"\n", sig);
                printf("  method = \"%s%s\"\n", name, msig);
                result = STATUS_FAILED;
                return;
            }
            if (isNative == JNI_TRUE) {
                if (printdump == JNI_TRUE) {
                    printf(">>>     \"%s%s\", native\n", name, msig);
                }
            } else {
                err = jvmti_env->GetBytecodes(methods[i],
                    &bytecodeCount, &bytecodes);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(GetBytecodes#%d:%d) unexpected error: %s (%d)\n",
                           eventsCount, i, TranslateError(err), err);
                    result = STATUS_FAILED;
                    return;
                } else {
                    if (printdump == JNI_TRUE) {
                        printf(">>>     \"%s%s\", %d bytes\n",
                               name, msig, bytecodeCount);
                    }
                    if (checkCode(bytecodeCount, bytecodes) == JNI_FALSE) {
                        printf("  class: \"%s\"\n", sig);
                        printf("  method = \"%s%s\"\n", name, msig);
                        result = STATUS_FAILED;
                    }
                }
            }
            if (name != NULL) {
                jvmti_env->Deallocate((unsigned char *)name);
            }
            if (msig != NULL) {
                jvmti_env->Deallocate((unsigned char *)msig);
            }
            if (bytecodes != NULL) {
                jvmti_env->Deallocate(bytecodes);
            }
        }
    }

    if (methods != NULL) {
        jvmti_env->Deallocate((unsigned char *)methods);
    }
    if (sig != NULL) {
        jvmti_env->Deallocate((unsigned char *)sig);
    }
    eventsCount++;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_bytecodes003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_bytecodes003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_bytecodes003(JavaVM *jvm, char *options, void *reserved) {
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

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
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

    if (caps.can_get_bytecodes) {
        callbacks.ClassPrepare = &ClassPrepare;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_CLASS_PREPARE, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable ClassPrepare: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    } else {
        printf("Warning: GetBytecodes is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetBytecodes_bytecodes003_check(JNIEnv *env, jclass cls) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (caps.can_get_bytecodes) {
        err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
                JVMTI_EVENT_CLASS_PREPARE, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to disable JVMTI_EVENT_CLASS_PREPARE: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf("Total number of class prepare events: %d\n", eventsCount);
    }

    return result;
}

}
