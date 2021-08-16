/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <jvmti_aod.h>

extern "C" {

void nsk_jvmti_aod_disableEventAndFinish(const char* agentName, jvmtiEvent event, int success, jvmtiEnv *jvmti, JNIEnv* jni) {
    if (!nsk_jvmti_aod_disableEvent(jvmti, event))
        success = 0;

    nsk_aod_agentFinished(jni, agentName, success);
}

void nsk_jvmti_aod_disableEventsAndFinish(const char* agentName, jvmtiEvent events[], int eventsNumber, int success, jvmtiEnv *jvmti, JNIEnv* jni) {
    if (!nsk_jvmti_aod_disableEvents(jvmti, events, eventsNumber))
        success = 0;

    nsk_aod_agentFinished(jni, agentName, success);
}

/*
 * Work with agent options
 */

struct {
    jvmtiEnv *jvmti;
    Options *options;
} multiagentsOptions[MAX_MULTIPLE_AGENTS];

static volatile int multiagentsCount = 0;

int nsk_jvmti_aod_addMultiagentsOptions(jvmtiEnv *jvmti, Options *options) {
    if (multiagentsCount >= MAX_MULTIPLE_AGENTS) {
        NSK_COMPLAIN1("To many agents, max agents count is %d\n", MAX_MULTIPLE_AGENTS);
        return NSK_FALSE;
    }

    multiagentsOptions[multiagentsCount].jvmti = jvmti;
    multiagentsOptions[multiagentsCount].options = options;
    multiagentsCount++;

    NSK_DISPLAY3("Options for agent %s were added (jvmtiEnv: %p, agentsCount: %d)\n",
            nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION),
            jvmti,
            multiagentsCount);

    return NSK_TRUE;
}

Options* nsk_jvmti_aod_getMultiagentsOptions(jvmtiEnv *jvmti) {
    int i;
    for (i = 0; i < multiagentsCount; i++) {
        if (multiagentsOptions[i].jvmti == jvmti) {
            return multiagentsOptions[i].options;
        }
    }

    NSK_COMPLAIN1("Options for jvmtiEnv %p weren't found\n", jvmti);

    return NULL;
}

/*
 * Auxiliary functions
 */

void nsk_jvmti_aod_deallocate(jvmtiEnv *jvmti, unsigned char* mem) {
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate(mem))) {
        NSK_COMPLAIN0("Deallocate failed\n");

        /*
         * if deallocate fails it isn't critical and test execution can continue without problems,
         * just call nsk_aod_internal_error to inform framework about this error
         */
        nsk_aod_internal_error();
    }
}

int nsk_jvmti_aod_getClassName(jvmtiEnv *jvmti, jclass klass, char classNameBuffer[]) {
    char* className;

    if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(klass, &className, NULL))) {
        NSK_COMPLAIN0("Failed to get class name\n");
        classNameBuffer[0] = '\0';
        return NSK_FALSE;
    } else {
        strcpy(classNameBuffer, className);

        nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)className);

        return NSK_TRUE;
    }
}

int nsk_jvmti_aod_getThreadName(jvmtiEnv * jvmti, jthread thread, char threadNameBuffer[]) {
    jvmtiThreadInfo info;
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &info))) {
        NSK_COMPLAIN0("Failed to get thread info\n");
        threadNameBuffer[0] = '\0';
        return NSK_FALSE;
    } else {
        strcpy(threadNameBuffer, info.name);

        nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)info.name);

        return NSK_TRUE;
    }
}

// events enabling/disabling

int nsk_jvmti_aod_disableEvents(jvmtiEnv* jvmti, jvmtiEvent events[], int eventsNumber) {
    int i;
    int status = NSK_TRUE;

    for (i = 0; i < eventsNumber; i++) {
        if (!nsk_jvmti_aod_disableEvent(jvmti, events[i])) {
            status = NSK_FALSE;
        }
    }

    return status;
}

int nsk_jvmti_aod_enableEvents(jvmtiEnv* jvmti, jvmtiEvent events[], int eventsNumber) {
    int i;
    for (i = 0; i < eventsNumber; i++) {
        if (!nsk_jvmti_aod_enableEvent(jvmti, events[i]))
            return NSK_FALSE;
    }

    return NSK_TRUE;
}

// java threads creation

jthread nsk_jvmti_aod_createThread(JNIEnv *jni) {
    jclass klass ;
    jmethodID threadConstructor;
    jthread thread;

    if (!NSK_JNI_VERIFY(jni, (klass = jni->FindClass("java/lang/Thread")) != NULL)) {
        NSK_COMPLAIN0("Failed to get the java.lang.Thread class\n");
        return NULL;
    }
    if (!NSK_JNI_VERIFY(jni,
            (threadConstructor = jni->GetMethodID(klass, "<init>", "()V")) != NULL)) {
        NSK_COMPLAIN0("Failed to get java.lang.Thread constructor\n");
        return NULL;
    }

    if (!NSK_JNI_VERIFY (jni,
            (thread = jni->NewObject(klass, threadConstructor, NULL)) != NULL)) {
        NSK_COMPLAIN0("Failed to create Thread object\n");
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni, (thread = jni->NewGlobalRef(thread)) != NULL)) {
        NSK_COMPLAIN0("Failed to create global reference\n");
        return NULL;
    }

    return thread;
}

jthread nsk_jvmti_aod_createThreadWithName(JNIEnv *jni, const char* threadName) {
    jclass klass ;
    jmethodID threadConstructor;
    jthread thread;
    jstring threadNameString;

    if (!NSK_JNI_VERIFY(jni, (threadNameString = jni->NewStringUTF(threadName)) != NULL))
        return NULL;

    if (!NSK_JNI_VERIFY(jni, (klass = jni->FindClass("java/lang/Thread")) != NULL)) {
        NSK_COMPLAIN0("Failed to get the java.lang.Thread class\n");
        return NULL;
    }
    if (!NSK_JNI_VERIFY(jni,
            (threadConstructor = jni->GetMethodID(klass, "<init>", "(Ljava/lang/String;)V")) != NULL)) {
        NSK_COMPLAIN0("Failed to get java.lang.Thread constructor\n");
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni,
            (thread = jni->NewObject(klass, threadConstructor, threadNameString)) != NULL)) {
        NSK_COMPLAIN0("Failed to create Thread object\n");
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni, (thread = jni->NewGlobalRef(thread)) != NULL)) {
        NSK_COMPLAIN0("Failed to create global reference\n");
        return NULL;
    }

    return thread;
}

// class redefinition

int nsk_jvmti_aod_redefineClass(
        Options * options,
        jvmtiEnv * jvmti,
        jclass classToRedefine,
        const char * fileName) {

    if (!nsk_aod_optionSpecified(options, PATH_TO_NEW_BYTE_CODE_OPTION)) {
        NSK_COMPLAIN1("Option '%s' isn't specified\n", PATH_TO_NEW_BYTE_CODE_OPTION);
        return NSK_FALSE;
    }
    if (fileName == NULL) {
        NSK_COMPLAIN0("File name is NULL\n");
        return NSK_FALSE;
    }
    {
        char file [1024];

        sprintf(file,"%s/%s.class",
                nsk_aod_getOptionValue(options, PATH_TO_NEW_BYTE_CODE_OPTION),
                fileName);
        NSK_DISPLAY1("File with new bytecode: '%s'\n", file);

        {
            FILE *bytecode;
            unsigned char * classBytes;
            jvmtiError error;
            jint size;

            bytecode = fopen(file, "rb");
            error= JVMTI_ERROR_NONE;
            if (bytecode == NULL) {
                NSK_COMPLAIN1("Error opening file '%s'\n", file);
                return NSK_FALSE;
            }

            NSK_DISPLAY1("Opening file '%s' \n", file);
            fseek(bytecode, 0, SEEK_END);
            size = ftell(bytecode);
            NSK_DISPLAY1("File size= %ld\n", ftell(bytecode));
            rewind(bytecode);
            error = jvmti->Allocate(size, &classBytes);
            if (error != JVMTI_ERROR_NONE) {
                NSK_DISPLAY1("Failed to create memory %s\n", TranslateError(error));
                fclose(bytecode);
                return NSK_FALSE;
            }

            if (((jint) fread(classBytes, 1, size, bytecode)) != size) {
                NSK_COMPLAIN0("Failed to read all the bytes, could be less or more\n");
                fclose(bytecode);
                return NSK_FALSE;
            } else {
                NSK_DISPLAY0("File read completely \n");
            }
            fclose(bytecode);
            {
                jvmtiClassDefinition classDef;
                classDef.klass = classToRedefine;
                classDef.class_byte_count= size;
                classDef.class_bytes = classBytes;
                NSK_DISPLAY0("Redefining\n");
                error = jvmti->RedefineClasses(1, &classDef);
                if (error != JVMTI_ERROR_NONE) {
                    NSK_DISPLAY1("# error occured while redefining %s ",
                            TranslateError(error));
                    return NSK_FALSE;
                }
            }
        }
    }
    return NSK_TRUE;
}

// capabilities

void printCapabilities(jvmtiCapabilities caps) {
    #define printCap(name) NSK_DISPLAY1(#name ": %d\n", caps.name)

    printCap(can_tag_objects);
    printCap(can_generate_field_modification_events);
    printCap(can_generate_field_access_events);
    printCap(can_get_bytecodes);
    printCap(can_get_synthetic_attribute);
    printCap(can_get_owned_monitor_info);
    printCap(can_get_current_contended_monitor);
    printCap(can_get_monitor_info);
    printCap(can_pop_frame);
    printCap(can_redefine_classes);
    printCap(can_signal_thread);
    printCap(can_get_source_file_name);
    printCap(can_get_line_numbers);
    printCap(can_get_source_debug_extension);
    printCap(can_access_local_variables);
    printCap(can_maintain_original_method_order);
    printCap(can_generate_single_step_events);
    printCap(can_generate_exception_events);
    printCap(can_generate_frame_pop_events);
    printCap(can_generate_breakpoint_events);
    printCap(can_suspend);
    printCap(can_redefine_any_class);
    printCap(can_get_current_thread_cpu_time);
    printCap(can_get_thread_cpu_time);
    printCap(can_generate_method_entry_events);
    printCap(can_generate_method_exit_events);
    printCap(can_generate_all_class_hook_events);
    printCap(can_generate_compiled_method_load_events);
    printCap(can_generate_monitor_events);
    printCap(can_generate_vm_object_alloc_events);
    printCap(can_generate_native_method_bind_events);
    printCap(can_generate_garbage_collection_events);
    printCap(can_generate_object_free_events);
    printCap(can_force_early_return);
    printCap(can_get_owned_monitor_stack_depth_info);
    printCap(can_get_constant_pool);
    printCap(can_set_native_method_prefix);
    printCap(can_retransform_classes);
    printCap(can_retransform_any_class);

    #undef printCap
}

}
