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

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;

/* constant names */
#define DEBUGEE_CLASS_NAME    "nsk/jvmti/GetObjectsWithTags/objwithtags001"
#define OBJECT_CLASS_NAME     "nsk/jvmti/GetObjectsWithTags/objwithtags001TestedClass"
#define OBJECT_CLASS_SIG      "L" OBJECT_CLASS_NAME ";"
#define OBJECTS_FIELD_NAME    "objects"
#define OBJECTS_FIELD_SIG     "[" OBJECT_CLASS_SIG

/* constants */
#define DEFAULT_TAGS_COUNT    4
#define DEFAULT_OBJECTS_COUNT 5

static int tagsCount = 0;
static int objectsCount = 0;

/* 2-dim indexing for flat list */
#define ITEM(list, i, j)      (((list) + (i * objectsCount))[j])

/* ============================================================================= */

/** Obtain tested objects from static field of debugee class. */
static int getTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int tagsCount, int objectsCount,
                                                jobject* *objects, jlong* *tags) {
    jclass debugeeClass = NULL;
    jfieldID objectField = NULL;
    jobjectArray arrayObject = NULL;
    int size = tagsCount * objectsCount;

    NSK_DISPLAY2("Allocate memory for lists: %d objects for %d tags\n",
                                                            objectsCount, tagsCount);
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((size * sizeof(jobject)), (unsigned char**)objects))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... allocated objects list: 0x%p\n", (void*)objects);

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((tagsCount * sizeof(jlong)), (unsigned char**)tags))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... allocated tags list: 0x%p\n", (void*)tags);

    {
        int i, k;
        for (k = 0; k < size; k++) {
            (*objects)[k] = NULL;
        }

        for (i = 0; i < tagsCount; i++) {
            (*tags)[i] = 100 * (jlong)(i + 1);
        }
    }

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (debugeeClass = jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)debugeeClass);

    NSK_DISPLAY1("Find static field: %s\n", OBJECTS_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (objectField =
            jni->GetStaticFieldID(debugeeClass, OBJECTS_FIELD_NAME, OBJECTS_FIELD_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)objectField);

    NSK_DISPLAY1("Get objects array from static field: %s\n", OBJECTS_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (arrayObject = (jobjectArray)
            jni->GetStaticObjectField(debugeeClass, objectField)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got array object: 0x%p\n", (void*)arrayObject);

    {
        jsize arrayLen = 0;
        jsize k;

        if (!NSK_JNI_VERIFY(jni, (arrayLen = jni->GetArrayLength(arrayObject)) == size)) {
            NSK_DISPLAY1("  ... got array length: %d\n", (int)size);
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }
        NSK_DISPLAY1("  ... got array length: %d\n", (int)size);

        for (k = 0; k < size; k++) {
            jobject object = NULL;

            if (!NSK_JNI_VERIFY(jni, (object = jni->GetObjectArrayElement(arrayObject, k)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            }
            if (!NSK_JNI_VERIFY(jni, (object = jni->NewGlobalRef(object)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            }

            (*objects)[k] = object;
        }
    }
    NSK_DISPLAY1("  ... object references created: %d objects\n", size);

    return NSK_TRUE;
}

/** Release references to the tested objects and free allocated memory. */
static int releaseTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int tagsCount, int objectsCount,
                                                        jobject *objects, jlong *tags) {
    int size = tagsCount * objectsCount;
    int k;

    if (objects == NULL)
        return NSK_TRUE;

    NSK_DISPLAY1("Release objects references: %d objects\n", size);
    for (k = 0; k < size; k++) {
        if (objects[k] != NULL) {
            NSK_TRACE(jni->DeleteGlobalRef(objects[k]));
        }
    }
    NSK_DISPLAY1("  ... object references released: %d objects\n", size);

    NSK_DISPLAY1("Deallocate objects list: 0x%p\n", (void*)objects);
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)objects))) {
        nsk_jvmti_setFailStatus();
    }

    if (tags == NULL)
        return NSK_FALSE;

    NSK_DISPLAY1("Deallocate tags list: 0x%p\n", (void*)tags);
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)tags))) {
        nsk_jvmti_setFailStatus();
    }

    return NSK_TRUE;
}

/** Get and check tagged objects. */
static int checkTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int tagsCount, int objectsCount,
                                                jlong tags[], jobject objects[],
                                                const char kind[], int expectedCount) {
    jint taggedObjectsCount = 0;
    jobject* taggedObjectsList = NULL;
    jlong* taggedObjectsTags = NULL;
    jlong expectedTag = 0;
    int k;

    NSK_DISPLAY1("Get tagged objects: %d tags\n", tagsCount);
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetObjectsWithTags(tagsCount, tags, &taggedObjectsCount, &taggedObjectsList, &taggedObjectsTags))) {
        nsk_jvmti_setFailStatus();
        return NSK_TRUE;
    }
    NSK_DISPLAY1("  ... got tagged objects: %d\n", (int)taggedObjectsCount);

    if (taggedObjectsCount != expectedCount) {
        NSK_COMPLAIN3("GetObjectsWithTags() returns unexpected number of objects %s:\n"
                      "#   got objects:  %d\n"
                      "#   expected:     %d\n",
                      kind, (int)taggedObjectsCount, (int)expectedCount);
        nsk_jvmti_setFailStatus();
    }

    if (taggedObjectsList == NULL && taggedObjectsCount > 0) {
        NSK_COMPLAIN2("GetObjectsWithTags() returns NULL list of objects %s: 0x%p\n",
                        kind, (void*)taggedObjectsList);
        nsk_jvmti_setFailStatus();
        return NSK_TRUE;
    }

    if (taggedObjectsTags == NULL && taggedObjectsCount > 0) {
        NSK_COMPLAIN2("GetObjectsWithTags() returns NULL list of tags for objects %s: 0x%p\n",
                        kind, (void*)taggedObjectsTags);
        nsk_jvmti_setFailStatus();
        return NSK_TRUE;
    }

    for (k = 0; k < taggedObjectsCount; k++) {
        jobject object = taggedObjectsList[k];
        jlong tag = taggedObjectsTags[k];
        int objectsFound = 0;
        int i, j, l;

        NSK_DISPLAY3("   #%d: object: 0x%p, tag: %ld\n", k, (void*)object, (long)tag);

        if (object == NULL) {
            NSK_COMPLAIN3("GetObjectsWithTags() returns NULL for object #%d %s: 0x%p\n",
                        k, kind, (void*)object);
            nsk_jvmti_setFailStatus();
            continue;
        }

        objectsFound = 0;
        for (l = k + 1; l < taggedObjectsCount; l++) {
            if (object == taggedObjectsList[l])
                objectsFound++;
        }
        if (objectsFound > 0) {
            NSK_COMPLAIN4("GetObjectsWithTags() returns %d duplicates for object #%d %s: 0x%p\n",
                        objectsFound, k, kind, (void*)object);
            nsk_jvmti_setFailStatus();
            continue;
        }

        objectsFound = 0;
        for (i = 0; i < tagsCount; i++) {
            for (j = 0; j < objectsCount; j++) {
                jobject foundObject = ITEM(objects, i, j);

                if (jni->IsSameObject(object, foundObject)) {
                    objectsFound++;

                    if (expectedCount > 0)
                        expectedTag = tags[i];

                    if (tag != expectedTag) {
                        NSK_COMPLAIN6("GetObjectsWithTags() returns wrong tag for object #%d %s:\n"
                                      "#   got object: 0x%p\n"
                                      "#   original:   0x%p\n"
                                      "#   got tag:    %ld\n"
                                      "#   original:   %ld\n",
                                        k, kind,
                                        (void*)object, (void*)foundObject,
                                        (long)tag, (long)expectedTag);
                        nsk_jvmti_setFailStatus();
                    }
                    break;
                }
            }

            if (objectsFound > 0)
                break;
        }

        if (objectsFound <= 0) {
            NSK_COMPLAIN4("GetObjectsWithTags() returns unexpected #%d object %s:\n"
                          "#   got object: 0x%p\n"
                          "#   got tag:    %ld\n",
                            k, kind,
                            (void*)object, (long)tag);
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY1("Deallocate got objects list: 0x%p\n", (void*)taggedObjectsList);
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)taggedObjectsList))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY1("Deallocate got tags list: 0x%p\n", (void*)taggedObjectsTags);
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)taggedObjectsTags))) {
        nsk_jvmti_setFailStatus();
    }

    return NSK_TRUE;
}

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for objects created\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    /* perform testing */
    {
        int size = tagsCount * objectsCount;
        jobject* objects = NULL;
        jlong* tags = NULL;

        NSK_DISPLAY0(">>> Obtain tested objects list from a static field of debugee class\n");
        {
            if (!NSK_VERIFY(getTestedObjects(jvmti, jni, tagsCount, objectsCount,
                                                                    &objects, &tags)))
                return;
        }

        NSK_DISPLAY0(">>> Tagging tested objects with different tags\n");
        {
            int i, j;

            for (i = 0; i < tagsCount; i++) {
                NSK_DISPLAY2("  tagging with %ld: %d objects\n", (long)tags[i], objectsCount);
                for (j = 0; j < objectsCount; j++) {
                    jobject object = ITEM(objects, i, j);

                    NSK_DISPLAY3("    #%d: object: 0x%p, tag: %ld\n",
                                            j, (void*)object, (long)tags[i]);
                    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(object, tags[i]))) {
                        nsk_jvmti_setFailStatus();
                        return;
                    }
                }
            }
            NSK_DISPLAY1("  ... objects tagged: %d objects\n", (tagsCount * objectsCount));
        }

        NSK_DISPLAY0(">>> Testcase #1: get tagged objects before objects data changed\n");
        {
            if (!NSK_VERIFY(
                    checkTestedObjects(jvmti, jni, tagsCount, objectsCount,
                                                tags, objects, "before changed", size)))
                return;
        }

        NSK_DISPLAY0(">>> Let debugee to change object data\n");
        {
            if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
                return;
            if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
                return;
        }

        NSK_DISPLAY0(">>> Testcase #2: get tagged objects after objects data are changed\n");
        {
            if (!NSK_VERIFY(
                    checkTestedObjects(jvmti, jni, tagsCount, objectsCount,
                                                tags, objects, "after changed", size)))
                return;
        }

        NSK_DISPLAY0(">>> Untagging all tested objects (i.e., tagging with zero tag)\n");
        {
            jlong tag = 0;
            int i, j;

            for (i = 0; i < tagsCount; i++) {
                NSK_DISPLAY2("  tagging with %ld: %d objects\n", (long)tag, objectsCount);
                for (j = 0; j < objectsCount; j++) {
                    jobject object = ITEM(objects, i , j);

                    NSK_DISPLAY3("    #%d: object: 0x%p, tag: %ld\n",
                                            j, (void*)object, (long)tag);
                    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(object, tag))) {
                        nsk_jvmti_setFailStatus();
                        return;
                    }
                }
            }
            NSK_DISPLAY1("  ... objects untagged: %d objects\n", (tagsCount * objectsCount));
        }

        NSK_DISPLAY0(">>> Testcase #3: get tagged objects after objects untagged\n");
        {
            if (!NSK_VERIFY(
                    checkTestedObjects(jvmti, jni, tagsCount, objectsCount,
                                                tags, objects, "after untagged", 0)))
                return;
        }

        NSK_DISPLAY0(">>> Clean used data\n");
        {
            if (!NSK_VERIFY(releaseTestedObjects(jvmti, jni, tagsCount, objectsCount,
                                                                        objects, tags)))
                return;
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_objwithtags001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_objwithtags001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_objwithtags001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* get option values */
    tagsCount = nsk_jvmti_findOptionIntValue("tags", DEFAULT_TAGS_COUNT);
    objectsCount = nsk_jvmti_findOptionIntValue("objects", DEFAULT_OBJECTS_COUNT);
    if (!NSK_VERIFY(tagsCount > 0 && objectsCount > 0))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add required capabilities */
    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_tag_objects = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
