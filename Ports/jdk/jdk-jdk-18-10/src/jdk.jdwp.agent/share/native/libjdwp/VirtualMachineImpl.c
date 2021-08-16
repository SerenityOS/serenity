/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "util.h"
#include "VirtualMachineImpl.h"
#include "commonRef.h"
#include "inStream.h"
#include "outStream.h"
#include "eventHandler.h"
#include "eventHelper.h"
#include "threadControl.h"
#include "SDE.h"
#include "FrameID.h"

static char *versionName = "Java Debug Wire Protocol (Reference Implementation)";

static jboolean
version(PacketInputStream *in, PacketOutputStream *out)
{
    char buf[500];
    char *vmName;
    char *vmVersion;
    char *vmInfo;

    /* Now the JDWP versions are the same as JVMTI versions */
    int majorVersion = jvmtiMajorVersion();
    int minorVersion = 0;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    vmVersion = gdata->property_java_version;
    if (vmVersion == NULL) {
        vmVersion = "<unknown>";
    }
    vmName = gdata->property_java_vm_name;
    if (vmName == NULL) {
        vmName = "<unknown>";
    }
    vmInfo = gdata->property_java_vm_info;
    if (vmInfo == NULL) {
        vmInfo = "<unknown>";
    }

    /*
     * Write the descriptive version information
     */
    (void)snprintf(buf, sizeof(buf),
                "%s version %d.%d\nJVM Debug Interface version %d.%d\n"
                 "JVM version %s (%s, %s)",
                  versionName, majorVersion, minorVersion,
                  jvmtiMajorVersion(), jvmtiMinorVersion(),
                  vmVersion, vmName, vmInfo);
    (void)outStream_writeString(out, buf);

    /*
     * Write the JDWP version numbers
     */
    (void)outStream_writeInt(out, majorVersion);
    (void)outStream_writeInt(out, minorVersion);

    /*
     * Write the VM version and name
     */
    (void)outStream_writeString(out, vmVersion);
    (void)outStream_writeString(out, vmName);

    return JNI_TRUE;
}

static jboolean
classesForSignature(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    char *signature;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    signature = inStream_readString(in);
    if (signature == NULL) {
        outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        return JNI_TRUE;
    }
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jint classCount;
        jclass *theClasses;
        jvmtiError error;

        error = allLoadedClasses(&theClasses, &classCount);
        if ( error == JVMTI_ERROR_NONE ) {
            /* Count classes in theClasses which match signature */
            int matchCount = 0;
            /* Count classes written to the JDWP connection */
            int writtenCount = 0;
            int i;

            for (i = 0; i < classCount; i++) {
                jclass clazz = theClasses[i];
                jint status = classStatus(clazz);
                char *candidate_signature = NULL;
                jint wanted =
                    (JVMTI_CLASS_STATUS_PREPARED|JVMTI_CLASS_STATUS_ARRAY|
                     JVMTI_CLASS_STATUS_PRIMITIVE);

                /* We want prepared classes, primitives, and arrays only */
                if ((status & wanted) == 0) {
                    continue;
                }

                error = classSignature(clazz, &candidate_signature, NULL);
                if (error != JVMTI_ERROR_NONE) {
                  // Clazz become invalid since the time we get the class list
                  // Skip this entry
                  if (error == JVMTI_ERROR_INVALID_CLASS) {
                    continue;
                  }

                  break;
                }

                if (strcmp(candidate_signature, signature) == 0) {
                    /* Float interesting classes (those that
                     * are matching and are prepared) to the
                     * beginning of the array.
                     */
                    theClasses[i] = theClasses[matchCount];
                    theClasses[matchCount++] = clazz;
                }
                jvmtiDeallocate(candidate_signature);
            }

            /* At this point matching prepared classes occupy
             * indicies 0 thru matchCount-1 of theClasses.
             */

            if ( error ==  JVMTI_ERROR_NONE ) {
                (void)outStream_writeInt(out, matchCount);
                for (; writtenCount < matchCount; writtenCount++) {
                    jclass clazz = theClasses[writtenCount];
                    jint status = classStatus(clazz);
                    jbyte tag = referenceTypeTag(clazz);
                    (void)outStream_writeByte(out, tag);
                    (void)outStream_writeObjectRef(env, out, clazz);
                    (void)outStream_writeInt(out, map2jdwpClassStatus(status));
                    /* No point in continuing if there's an error */
                    if (outStream_error(out)) {
                        break;
                    }
                }
            }

            jvmtiDeallocate(theClasses);
        }

        if ( error != JVMTI_ERROR_NONE ) {
            outStream_setError(out, map2jdwpError(error));
        }

    } END_WITH_LOCAL_REFS(env);

    jvmtiDeallocate(signature);

    return JNI_TRUE;
}

static jboolean
allModules(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jint count = 0;
        jint i = 0;
        jobject* modules = NULL;
        jvmtiError error = JVMTI_ERROR_NONE;

        error = JVMTI_FUNC_PTR(gdata->jvmti, GetAllModules) (gdata->jvmti, &count, &modules);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeInt(out, count);
            for (i = 0; i < count; i++) {
                (void)outStream_writeModuleRef(env, out, modules[i]);
            }
            jvmtiDeallocate(modules);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
allClasses1(PacketInputStream *in, PacketOutputStream *out, int outputGenerics)
{
    JNIEnv *env;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jint classCount;
        jclass *theClasses;
        jvmtiError error;

        error = allLoadedClasses(&theClasses, &classCount);
        if ( error != JVMTI_ERROR_NONE ) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            /* Count classes in theClasses which are prepared */
            int prepCount = 0;
            /* Count classes written to the JDWP connection */
            int writtenCount = 0;
            int i;

            for (i=0; i<classCount; i++) {
                jclass clazz = theClasses[i];
                jint status = classStatus(clazz);
                jint wanted =
                    (JVMTI_CLASS_STATUS_PREPARED|JVMTI_CLASS_STATUS_ARRAY);

                /* We want prepared classes and arrays only */
                if ((status & wanted) != 0) {
                    /* Float interesting classes (those that
                     * are prepared) to the beginning of the array.
                     */
                    theClasses[i] = theClasses[prepCount];
                    theClasses[prepCount++] = clazz;
                }
            }

            /* At this point prepared classes occupy
             * indicies 0 thru prepCount-1 of theClasses.
             */

            (void)outStream_writeInt(out, prepCount);
            for (; writtenCount < prepCount; writtenCount++) {
                char *signature = NULL;
                char *genericSignature = NULL;
                jclass clazz = theClasses[writtenCount];
                jint status = classStatus(clazz);
                jbyte tag = referenceTypeTag(clazz);
                jvmtiError error;

                error = classSignature(clazz, &signature, &genericSignature);
                if (error != JVMTI_ERROR_NONE) {
                    outStream_setError(out, map2jdwpError(error));
                    break;
                }

                (void)outStream_writeByte(out, tag);
                (void)outStream_writeObjectRef(env, out, clazz);
                (void)outStream_writeString(out, signature);
                if (outputGenerics == 1) {
                    writeGenericSignature(out, genericSignature);
                }

                (void)outStream_writeInt(out, map2jdwpClassStatus(status));
                jvmtiDeallocate(signature);
                if (genericSignature != NULL) {
                  jvmtiDeallocate(genericSignature);
                }

                /* No point in continuing if there's an error */
                if (outStream_error(out)) {
                    break;
                }
            }
            jvmtiDeallocate(theClasses);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
allClasses(PacketInputStream *in, PacketOutputStream *out)
{
    return allClasses1(in, out, 0);
}

static jboolean
allClassesWithGeneric(PacketInputStream *in, PacketOutputStream *out)
{
    return allClasses1(in, out, 1);
}

  /***********************************************************/


static jboolean
instanceCounts(PacketInputStream *in, PacketOutputStream *out)
{
    jint classCount;
    jclass *classes;
    JNIEnv *env;
    int ii;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    classCount = inStream_readInt(in);

    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    if (classCount == 0) {
        (void)outStream_writeInt(out, 0);
        return JNI_TRUE;
    }
    if (classCount < 0) {
        outStream_setError(out, JDWP_ERROR(ILLEGAL_ARGUMENT));
        return JNI_TRUE;
    }
    env = getEnv();
    classes = jvmtiAllocate(classCount * (int)sizeof(jclass));
    for (ii = 0; ii < classCount; ii++) {
        jdwpError errorCode;
        classes[ii] = inStream_readClassRef(env, in);
        errorCode = inStream_error(in);
        if (errorCode != JDWP_ERROR(NONE)) {
            /*
             * A class could have been unloaded/gc'd so
             * if we get an error, just ignore it and keep
             * going.  An instanceCount of 0 will be returned.
             */
            if (errorCode == JDWP_ERROR(INVALID_OBJECT) ||
                errorCode == JDWP_ERROR(INVALID_CLASS)) {
                inStream_clearError(in);
                classes[ii] = NULL;
                continue;
            }
            jvmtiDeallocate(classes);
            return JNI_TRUE;
        }
    }

    WITH_LOCAL_REFS(env, 1) {
        jlong      *counts;
        jvmtiError error;

        counts = jvmtiAllocate(classCount * (int)sizeof(jlong));
        /* Iterate over heap getting info on these classes */
        error = classInstanceCounts(classCount, classes, counts);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeInt(out, classCount);
            for (ii = 0; ii < classCount; ii++) {
                (void)outStream_writeLong(out, counts[ii]);
            }
        }
        jvmtiDeallocate(counts);
    } END_WITH_LOCAL_REFS(env);
    jvmtiDeallocate(classes);
    return JNI_TRUE;
}

static jboolean
redefineClasses(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiClassDefinition *classDefs;
    jboolean ok = JNI_TRUE;
    jint classCount;
    jint i;
    JNIEnv *env;

    if (gdata->vmDead) {
        /* quietly ignore */
        return JNI_TRUE;
    }

    classCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    if ( classCount == 0 ) {
        return JNI_TRUE;
    }
    /*LINTED*/
    classDefs = jvmtiAllocate(classCount*(int)sizeof(jvmtiClassDefinition));
    if (classDefs == NULL) {
        outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        return JNI_TRUE;
    }
    /*LINTED*/
    (void)memset(classDefs, 0, classCount*sizeof(jvmtiClassDefinition));

    env = getEnv();
    for (i = 0; i < classCount; ++i) {
        int byteCount;
        unsigned char * bytes;
        jclass clazz;

        clazz = inStream_readClassRef(env, in);
        if (inStream_error(in)) {
            ok = JNI_FALSE;
            break;
        }
        byteCount = inStream_readInt(in);
        if (inStream_error(in)) {
            ok = JNI_FALSE;
            break;
        }
        if ( byteCount <= 0 ) {
            outStream_setError(out, JDWP_ERROR(INVALID_CLASS_FORMAT));
            ok = JNI_FALSE;
            break;
        }
        bytes = (unsigned char *)jvmtiAllocate(byteCount);
        if (bytes == NULL) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
            ok = JNI_FALSE;
            break;
        }
        (void)inStream_readBytes(in, byteCount, (jbyte *)bytes);
        if (inStream_error(in)) {
            ok = JNI_FALSE;
            break;
        }

        classDefs[i].klass = clazz;
        classDefs[i].class_byte_count = byteCount;
        classDefs[i].class_bytes = bytes;
    }

    if (ok == JNI_TRUE) {
        jvmtiError error;

        error = JVMTI_FUNC_PTR(gdata->jvmti,RedefineClasses)
                        (gdata->jvmti, classCount, classDefs);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            /* zap our BP info */
            for ( i = 0 ; i < classCount; i++ ) {
                eventHandler_freeClassBreakpoints(classDefs[i].klass);
            }
        }
    }

    /* free up allocated memory */
    for ( i = 0 ; i < classCount; i++ ) {
        if ( classDefs[i].class_bytes != NULL ) {
            jvmtiDeallocate((void*)classDefs[i].class_bytes);
        }
    }
    jvmtiDeallocate(classDefs);

    return JNI_TRUE;
}

static jboolean
setDefaultStratum(PacketInputStream *in, PacketOutputStream *out)
{
    char *stratumId;

    if (gdata->vmDead) {
        /* quietly ignore */
        return JNI_TRUE;
    }

    stratumId = inStream_readString(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    } else if (strcmp(stratumId, "") == 0) {
        stratumId = NULL;
    }
    setGlobalStratumId(stratumId);

    return JNI_TRUE;
}

static jboolean
getAllThreads(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        int i;
        jint threadCount;
        jthread *theThreads;

        theThreads = allThreads(&threadCount);
        if (theThreads == NULL) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        } else {
            /* Squish out all of the debugger-spawned threads */
            threadCount = filterDebugThreads(theThreads, threadCount);

            (void)outStream_writeInt(out, threadCount);
            for (i = 0; i <threadCount; i++) {
                (void)outStream_writeObjectRef(env, out, theThreads[i]);
            }

            jvmtiDeallocate(theThreads);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
topLevelThreadGroups(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jint groupCount;
        jthreadGroup *groups;

        groups = NULL;
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetTopThreadGroups)
                    (gdata->jvmti, &groupCount, &groups);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;

            (void)outStream_writeInt(out, groupCount);
            for (i = 0; i < groupCount; i++) {
                (void)outStream_writeObjectRef(env, out, groups[i]);
            }

            jvmtiDeallocate(groups);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
dispose(PacketInputStream *in, PacketOutputStream *out)
{
    return JNI_TRUE;
}

static jboolean
idSizes(PacketInputStream *in, PacketOutputStream *out)
{
    (void)outStream_writeInt(out, sizeof(jfieldID));    /* fields */
    (void)outStream_writeInt(out, sizeof(jmethodID));   /* methods */
    (void)outStream_writeInt(out, sizeof(jlong));       /* objects */
    (void)outStream_writeInt(out, sizeof(jlong));       /* referent types */
    (void)outStream_writeInt(out, sizeof(FrameID));    /* frames */
    return JNI_TRUE;
}

static jboolean
suspend(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }
    error = threadControl_suspendAll();
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
resume(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }
    error = threadControl_resumeAll();
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
doExit(PacketInputStream *in, PacketOutputStream *out)
{
    jint exitCode;

    exitCode = inStream_readInt(in);
    if (gdata->vmDead) {
        /* quietly ignore */
        return JNI_FALSE;
    }

    /* We send the reply from here because we are about to exit. */
    if (inStream_error(in)) {
        outStream_setError(out, inStream_error(in));
    }
    outStream_sendReply(out);

    forceExit(exitCode);

    /* Shouldn't get here */
    JDI_ASSERT(JNI_FALSE);

    /* Shut up the compiler */
    return JNI_FALSE;

}

static jboolean
createString(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    char *cstring;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    cstring = inStream_readString(in);
    if (cstring == NULL) {
        outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        return JNI_TRUE;
    }
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jstring string;

        string = JNI_FUNC_PTR(env,NewStringUTF)(env, cstring);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        } else {
            (void)outStream_writeObjectRef(env, out, string);
        }

    } END_WITH_LOCAL_REFS(env);

    jvmtiDeallocate(cstring);

    return JNI_TRUE;
}

static jboolean
capabilities(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiCapabilities caps;
    jvmtiError error;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }
    error = jvmtiGetCapabilities(&caps);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeBoolean(out, (jboolean)caps.can_generate_field_modification_events);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_generate_field_access_events);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_bytecodes);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_synthetic_attribute);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_owned_monitor_info);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_current_contended_monitor);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_monitor_info);
    return JNI_TRUE;
}

static jboolean
capabilitiesNew(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiCapabilities caps;
    jvmtiError error;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }
    error = jvmtiGetCapabilities(&caps);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeBoolean(out, (jboolean)caps.can_generate_field_modification_events);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_generate_field_access_events);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_bytecodes);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_synthetic_attribute);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_owned_monitor_info);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_current_contended_monitor);
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_monitor_info);

    /* new since JDWP version 1.4 */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_redefine_classes);
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE /* can_add_method */ );
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE /* can_unrestrictedly_redefine_classes */ );
    /* 11: canPopFrames */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_pop_frame);
    /* 12: canUseInstanceFilters */
    (void)outStream_writeBoolean(out, (jboolean)JNI_TRUE);
    /* 13: canGetSourceDebugExtension */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_source_debug_extension);
    /* 14: canRequestVMDeathEvent */
    (void)outStream_writeBoolean(out, (jboolean)JNI_TRUE);
    /* 15: canSetDefaultStratum */
    (void)outStream_writeBoolean(out, (jboolean)JNI_TRUE);
    /* 16: canGetInstanceInfo */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_tag_objects);
    /* 17: canRequestMonitorEvents */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_generate_monitor_events);
    /* 18: canGetMonitorFrameInfo */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_owned_monitor_stack_depth_info);
    /* remaining reserved */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 19 */
    /* 20 Can get constant pool information */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_get_constant_pool);
    /* 21 Can force early return */
    (void)outStream_writeBoolean(out, (jboolean)caps.can_force_early_return);

    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 22 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 23 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 24 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 25 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 26 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 27 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 28 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 29 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 30 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 31 */
    (void)outStream_writeBoolean(out, (jboolean)JNI_FALSE); /* 32 */
    return JNI_TRUE;
}

static int
countPaths(char *string) {
    int cnt = 1; /* always have one */
    char *pos = string;
    char *ps;

    ps = gdata->property_path_separator;
    if ( ps == NULL ) {
        ps = ";";
    }
    while ((pos = strchr(pos, ps[0])) != NULL) {
        ++cnt;
        ++pos;
    }
    return cnt;
}

static void
writePaths(PacketOutputStream *out, char *string) {
    char *pos;
    char *ps;
    char *buf;
    int   npaths;
    int   i;

    buf = jvmtiAllocate((int)strlen(string)+1);

    npaths = countPaths(string);
    (void)outStream_writeInt(out, npaths);

    ps = gdata->property_path_separator;
    if ( ps == NULL ) {
        ps = ";";
    }

    pos = string;
    for ( i = 0 ; i < npaths && pos != NULL; i++ ) {
        char *psPos;
        int   plen;

        psPos = strchr(pos, ps[0]);
        if ( psPos == NULL ) {
            plen = (int)strlen(pos);
        } else {
            plen = (int)(psPos-pos);
            psPos++;
        }
        (void)memcpy(buf, pos, plen);
        buf[plen] = 0;
        (void)outStream_writeString(out, buf);
        pos = psPos;
    }

    jvmtiDeallocate(buf);
}

static jboolean
classPaths(PacketInputStream *in, PacketOutputStream *out)
{
    char *ud;
    char *cp;

    ud = gdata->property_user_dir;
    if ( ud == NULL ) {
        ud = "";
    }
    cp = gdata->property_java_class_path;
    if ( cp == NULL ) {
        cp = "";
    }
    (void)outStream_writeString(out, ud);
    writePaths(out, cp);
    (void)outStream_writeInt(out, 0); // no bootclasspath
    return JNI_TRUE;
}

static jboolean
disposeObjects(PacketInputStream *in, PacketOutputStream *out)
{
    int i;
    int refCount;
    jlong id;
    int requestCount;
    JNIEnv *env;

    if (gdata->vmDead) {
        /* quietly ignore */
        return JNI_TRUE;
    }

    requestCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    env = getEnv();
    for (i = 0; i < requestCount; i++) {
        id = inStream_readObjectID(in);
        refCount = inStream_readInt(in);
        if (inStream_error(in)) {
            return JNI_TRUE;
        }
        commonRef_releaseMultiple(env, id, refCount);
    }

    return JNI_TRUE;
}

static jboolean
holdEvents(PacketInputStream *in, PacketOutputStream *out)
{
    eventHelper_holdEvents();
    return JNI_TRUE;
}

static jboolean
releaseEvents(PacketInputStream *in, PacketOutputStream *out)
{
    eventHelper_releaseEvents();
    return JNI_TRUE;
}

Command VirtualMachine_Commands[] = {
    {version, "Version"},
    {classesForSignature, "ClassesForSignature"},
    {allClasses, "AllClasses"},
    {getAllThreads, "GetAllThreads"},
    {topLevelThreadGroups, "TopLevelThreadGroups"},
    {dispose, "Dispose"},
    {idSizes, "IDSizes"},
    {suspend, "Suspend"},
    {resume, "Resume"},
    {doExit, "DoExit"},
    {createString, "CreateString"},
    {capabilities, "Capabilities"},
    {classPaths, "ClassPaths"},
    {disposeObjects, "DisposeObjects"},
    {holdEvents, "HoldEvents"},
    {releaseEvents, "ReleaseEvents"},
    {capabilitiesNew, "CapabilitiesNew"},
    {redefineClasses, "RedefineClasses"},
    {setDefaultStratum, "SetDefaultStratum"},
    {allClassesWithGeneric, "AllClassesWithGeneric"},
    {instanceCounts, "InstanceCounts"},
    {allModules, "AllModules"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(VirtualMachine)
