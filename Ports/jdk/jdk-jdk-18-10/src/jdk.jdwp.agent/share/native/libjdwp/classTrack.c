/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This module tracks classes that have been prepared, so as to
 * be able to report which have been unloaded. On VM start-up
 * and whenever new classes are loaded, all prepared classes'
 * signatures are attached as JVMTI tag to the class object.
 * Class unloading is tracked by registering
 * ObjectFree callback on class objects. When this happens, we find
 * the signature of the unloaded class(es) and report them back
 * to the event handler to synthesize class-unload-events.
 */

#include "util.h"
#include "bag.h"
#include "classTrack.h"

#define NOT_TAGGED 0

/*
 * The JVMTI tracking env to keep track of klass tags for class-unloads
 */
static jvmtiEnv* trackingEnv;

/*
 * A bag containing all the deleted classes' signatures. Must be accessed under
 * classTrackLock.
 */
struct bag* deletedSignatures;

/*
 * Lock to keep integrity of deletedSignatures.
 */
static jrawMonitorID classTrackLock;

/*
 * Invoke the callback when classes are freed, find and record the signature
 * in deletedSignatures. Those are only used in addPreparedClass() by the
 * same thread.
 */
static void JNICALL
cbTrackingObjectFree(jvmtiEnv* jvmti_env, jlong tag)
{
    debugMonitorEnter(classTrackLock);
    if (deletedSignatures == NULL) {
      debugMonitorExit(classTrackLock);
      return;
    }
    *(char**)bagAdd(deletedSignatures) = (char*)jlong_to_ptr(tag);

    debugMonitorExit(classTrackLock);
}

/*
 * Called after class unloads have occurred.
 * The signatures of classes which were unloaded are returned.
 */
struct bag *
classTrack_processUnloads(JNIEnv *env)
{
    debugMonitorEnter(classTrackLock);
    if (deletedSignatures == NULL) {
        // Class tracking not initialized, nobody's interested.
        debugMonitorExit(classTrackLock);
        return NULL;
    }
    struct bag* deleted = deletedSignatures;
    deletedSignatures = bagCreateBag(sizeof(char*), 10);
    debugMonitorExit(classTrackLock);
    return deleted;
}

/*
 * Add a class to the prepared class table.
 */
void
classTrack_addPreparedClass(JNIEnv *env_unused, jclass klass)
{
    jvmtiError error;
    jvmtiEnv* env = trackingEnv;

    if (gdata && gdata->assertOn) {
        // Check this is not already tagged.
        jlong tag;
        error = JVMTI_FUNC_PTR(trackingEnv, GetTag)(env, klass, &tag);
        if (error != JVMTI_ERROR_NONE) {
            EXIT_ERROR(error, "Unable to GetTag with class trackingEnv");
        }
        JDI_ASSERT(tag == NOT_TAGGED);
    }

    char* signature;
    error = classSignature(klass, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error,"signature");
    }
    error = JVMTI_FUNC_PTR(trackingEnv, SetTag)(env, klass, ptr_to_jlong(signature));
    if (error != JVMTI_ERROR_NONE) {
        jvmtiDeallocate(signature);
        EXIT_ERROR(error,"SetTag");
    }
}

static jboolean
setupEvents()
{
    jvmtiCapabilities caps;
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_object_free_events = 1;
    jvmtiError error = JVMTI_FUNC_PTR(trackingEnv, AddCapabilities)(trackingEnv, &caps);
    if (error != JVMTI_ERROR_NONE) {
        return JNI_FALSE;
    }
    jvmtiEventCallbacks cb;
    memset(&cb, 0, sizeof(cb));
    cb.ObjectFree = cbTrackingObjectFree;
    error = JVMTI_FUNC_PTR(trackingEnv, SetEventCallbacks)(trackingEnv, &cb, sizeof(cb));
    if (error != JVMTI_ERROR_NONE) {
        return JNI_FALSE;
    }
    error = JVMTI_FUNC_PTR(trackingEnv, SetEventNotificationMode)(trackingEnv, JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL);
    if (error != JVMTI_ERROR_NONE) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Called once to initialize class-tracking.
 */
void
classTrack_initialize(JNIEnv *env)
{
    deletedSignatures = NULL;
    classTrackLock = debugMonitorCreate("Deleted class tag lock");
    trackingEnv = getSpecialJvmti();
    if (trackingEnv == NULL) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL, "Failed to allocate tag-tracking jvmtiEnv");
    }


    if (!setupEvents()) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL, "Unable to setup ObjectFree tracking");
    }

    jint classCount;
    jclass *classes;
    jvmtiError error;
    jint i;

    error = allLoadedClasses(&classes, &classCount);
    if ( error == JVMTI_ERROR_NONE ) {
        for (i = 0; i < classCount; i++) {
            jclass klass = classes[i];
            jint status;
            jint wanted = JVMTI_CLASS_STATUS_PREPARED | JVMTI_CLASS_STATUS_ARRAY;
            status = classStatus(klass);
            if ((status & wanted) != 0) {
                classTrack_addPreparedClass(env, klass);
            }
        }
        jvmtiDeallocate(classes);
    } else {
        EXIT_ERROR(error,"loaded classes array");
    }
}

/*
 * Called to activate class-tracking when a listener registers for EI_GC_FINISH.
 */
void
classTrack_activate(JNIEnv *env)
{
    debugMonitorEnter(classTrackLock);
    deletedSignatures = bagCreateBag(sizeof(char*), 1000);
    debugMonitorExit(classTrackLock);
}

static jboolean
cleanDeleted(void *signatureVoid, void *arg)
{
    char* sig = *(char**)signatureVoid;
    jvmtiDeallocate(sig);
    return JNI_TRUE;
}

/*
 * Called when agent detaches.
 */
void
classTrack_reset(void)
{
    debugMonitorEnter(classTrackLock);

    if (deletedSignatures != NULL) {
        bagEnumerateOver(deletedSignatures, cleanDeleted, NULL);
        bagDestroyBag(deletedSignatures);
        deletedSignatures = NULL;
    }

    debugMonitorExit(classTrackLock);
}
