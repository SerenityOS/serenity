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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* min and max values defined in jvmtiParamTypes */
#define PARAM_TYPE_MIN_VALUE 101
#define PARAM_TYPE_MAX_VALUE 117

/* min and max values defined in jvmtiParamKind */
#define PARAM_KIND_MIN_VALUE  91
#define PARAM_KIND_MAX_VALUE  97

#define NAME_PREFIX "com.sun.hotspot"

/* ============================================================================= */

static jlong timeout = 0;

/* ============================================================================= */

/** Check extension functions. */
static int checkExtensions(jvmtiEnv* jvmti, const char phase[]) {
    int success = NSK_TRUE;
    jint extCount = 0;
    jvmtiExtensionFunctionInfo* extList = NULL;
    int i;

    NSK_DISPLAY0("Get extension functions list\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetExtensionFunctions(&extCount, &extList))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got count: %d\n", (int)extCount);
    NSK_DISPLAY1("  ... got list:  0x%p\n", (void*)extList);

    if (extCount > 0) {
        if (extList == NULL) {
            NSK_COMPLAIN3("In %s phase GetExtensionFunctions() returned NULL pointer:\n"
                          "#   extensions pointer: 0x%p\n"
                          "#   extensions count:   %d\n",
                            phase, (void*)extList, (int)extCount);
            return NSK_FALSE;
        }

        NSK_DISPLAY1("Check each extension functions: %d functions\n", (int)extCount);
        for (i = 0; i < extCount; i++) {
            int j;

            NSK_DISPLAY1("  function #%d:\n", i);
            NSK_DISPLAY1("    func:        0x%p\n", (void*)extList[i].func);
            NSK_DISPLAY1("    id:          \"%s\"\n", nsk_null_string(extList[i].id));
            NSK_DISPLAY1("    short_desc:  \"%s\"\n", nsk_null_string(extList[i].short_description));
            NSK_DISPLAY1("    param_count: %d\n", (int)extList[i].param_count);
            NSK_DISPLAY1("    params:      0x%p\n", (void*)extList[i].params);

            if (extList[i].params != NULL) {
                for (j = 0; j < extList[i].param_count; j++) {
                    NSK_DISPLAY1("      param #%d:\n", j);
                    NSK_DISPLAY1("        name:      \"%s\"\n",
                                            nsk_null_string(extList[i].params[j].name));
                    NSK_DISPLAY1("        kind:      %d\n",
                                            (int)extList[i].params[j].kind);
                    NSK_DISPLAY1("        base_type: %d\n",
                                            (int)extList[i].params[j].base_type);
                    NSK_DISPLAY1("        null_ok:   %d\n",
                                            (int)extList[i].params[j].null_ok);
                }
            }

            NSK_DISPLAY1("    error_count: %d\n", (int)extList[i].error_count);
            NSK_DISPLAY1("    errors:      0x%p\n", (void*)extList[i].errors);

            if (extList[i].errors != NULL) {
                for (j = 0; j < extList[i].error_count; j++) {
                    NSK_DISPLAY2("      error #%d: %d\n",
                                            j, (int)extList[i].errors[j]);
                }
            }

            if (extList[i].func == NULL
                    || extList[i].id == NULL
                    || extList[i].short_description == NULL
                    || (extList[i].params == NULL && extList[i].param_count > 0)
                    || (extList[i].errors == NULL && extList[i].error_count > 0)) {
                NSK_COMPLAIN9("In %s phase GetExtensionFunctions() returned function #%d with NULL attribute(s):\n"
                              "#   func:        0x%p\n"
                              "#   id:          0x%p (%s)\n"
                              "#   short_desc:  0x%p (%s)\n"
                              "#   params:      0x%p\n"
                              "#   errors:      0x%p\n",
                                phase, i,
                                (void*)extList[i].func,
                                (void*)extList[i].id, nsk_null_string(extList[i].id),
                                (void*)extList[i].short_description, nsk_null_string(extList[i].short_description),
                                (void*)extList[i].params, (void*)extList[i].errors);
                success = NSK_FALSE;
            }

            if (extList[i].id != NULL && strlen(extList[i].id) <= 0) {
                NSK_COMPLAIN6("In %s phase GetExtensionFunctions() returned function #%d with empty id:\n"
                              "#   func:        0x%p\n"
                              "#   id:          \"%s\"\n"
                              "#   short_desc:  \"%s\"\n"
                              "#   param_count: %d\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count);
                success = NSK_FALSE;
            } else if (strstr(extList[i].id, NAME_PREFIX) == NULL) {
                NSK_COMPLAIN6("In %s phase GetExtensionFunctions() returned function #%d with unexpected id:\n"
                              "#   func:        0x%p\n"
                              "#   id:          \"%s\"\n"
                              "#   short_desc:  \"%s\"\n"
                              "#   param_count: %d\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count);
                success = NSK_FALSE;
            }

            if (extList[i].short_description != NULL && strlen(extList[i].short_description) <= 0) {
                NSK_COMPLAIN6("In %s phase GetExtensionFunctions() returned function #%d with empty desc:\n"
                              "#   func:        0x%p\n"
                              "#   id:          \"%s\"\n"
                              "#   short_desc:  \"%s\"\n"
                              "#   param_count: %d\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count);
                success = NSK_FALSE;
            }

            if (extList[i].param_count > 0 && extList[i].params != NULL) {
                for (j = 0; j < extList[i].param_count; j++) {
                    if (extList[i].params[j].name == NULL
                            || strlen(extList[i].params[j].name) <= 0) {
                        NSK_COMPLAIN9("In %s phase GetExtensionFunctions() returned function #%d with empty desc:\n"
                                      "#   func:        0x%p\n"
                                      "#   id:          \"%s\"\n"
                                      "#   short_desc:  \"%s\"\n"
                                      "#   param_count: %d\n"
                                      "#     param #%d: \n"
                                      "#       name:    0x%p (%s)\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count,
                                j,
                                (void*)extList[i].params[j].name,
                                nsk_null_string(extList[i].params[j].name));
                        success = NSK_FALSE;
                    }

                    if (extList[i].params[j].kind < PARAM_KIND_MIN_VALUE
                            || extList[i].params[j].kind > PARAM_KIND_MAX_VALUE) {
                        NSK_COMPLAIN9("In %s phase GetExtensionFunctions() returned function #%d with incorrect parameter kind:\n"
                                      "#   func:        0x%p\n"
                                      "#   id:          \"%s\"\n"
                                      "#   short_desc:  \"%s\"\n"
                                      "#   param_count: %d\n"
                                      "#     param #%d: \n"
                                      "#       name:    0x%p (%s)\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count,
                                j,
                                (void*)extList[i].params[j].name,
                                nsk_null_string(extList[i].params[j].name));
                        success = NSK_FALSE;
                    }

                    if (extList[i].params[j].base_type < PARAM_TYPE_MIN_VALUE
                            || extList[i].params[j].base_type > PARAM_TYPE_MAX_VALUE) {
                        NSK_COMPLAIN9("In %s phase GetExtensionFunctions() returned function #%d with incorrect parameter type:\n"
                                      "#   func:        0x%p\n"
                                      "#   id:          \"%s\"\n"
                                      "#   short_desc:  \"%s\"\n"
                                      "#   param_count: %d\n"
                                      "#     param #%d: \n"
                                      "#       name:    0x%p (%s)\n",
                                phase, i,
                                extList[i].func,
                                nsk_null_string(extList[i].id),
                                nsk_null_string(extList[i].short_description),
                                (int)extList[i].param_count,
                                j,
                                (void*)extList[i].params[j].name,
                                nsk_null_string(extList[i].params[j].name));
                        success = NSK_FALSE;
                    }
                }
            }
        }
    }

    NSK_DISPLAY1("Deallocate extension functions list: 0x%p\n", (void*)extList);
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)extList))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... deallocated\n");

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee class ready\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY0(">>> Testcase #2: Check extension functions in live phase\n");
    {
        if (!checkExtensions(jvmti, "live")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_extfuncs001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_extfuncs001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_extfuncs001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    NSK_DISPLAY0(">>> Testcase #1: Check extension functions in OnLoad phase\n");
    {
        if (!checkExtensions(jvmti, "OnLoad")) {
            nsk_jvmti_setFailStatus();
        }
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
