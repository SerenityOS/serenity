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
#include "ObjectReferenceImpl.h"
#include "commonRef.h"
#include "inStream.h"
#include "outStream.h"
#include "signature.h"

static jboolean
referenceType(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jobject object;

    env = getEnv();

    object = inStream_readObjectRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jbyte tag;
        jclass clazz;

        clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);
        tag = referenceTypeTag(clazz);

        (void)outStream_writeByte(out, tag);
        (void)outStream_writeObjectRef(env, out, clazz);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
getValues(PacketInputStream *in, PacketOutputStream *out)
{
    sharedGetFieldValues(in, out, JNI_FALSE);
    return JNI_TRUE;
}

static jvmtiError
readFieldValue(JNIEnv *env, PacketInputStream *in, jclass clazz,
               jobject object, jfieldID field, char *signature)
{
    jvalue value;

    jbyte typeKey = jdwpTag(signature);
    if (isReferenceTag(typeKey)) {
        value.l = inStream_readObjectRef(env, in);
        JNI_FUNC_PTR(env,SetObjectField)(env, object, field, value.l);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            return AGENT_ERROR_JNI_EXCEPTION;
        }
        return JVMTI_ERROR_NONE;
    }
    switch (typeKey) {
        case JDWP_TAG(BYTE):
            value.b = inStream_readByte(in);
            JNI_FUNC_PTR(env,SetByteField)(env, object, field, value.b);
            break;

        case JDWP_TAG(CHAR):
            value.c = inStream_readChar(in);
            JNI_FUNC_PTR(env,SetCharField)(env, object, field, value.c);
            break;

        case JDWP_TAG(FLOAT):
            value.f = inStream_readFloat(in);
            JNI_FUNC_PTR(env,SetFloatField)(env, object, field, value.f);
            break;

        case JDWP_TAG(DOUBLE):
            value.d = inStream_readDouble(in);
            JNI_FUNC_PTR(env,SetDoubleField)(env, object, field, value.d);
            break;

        case JDWP_TAG(INT):
            value.i = inStream_readInt(in);
            JNI_FUNC_PTR(env,SetIntField)(env, object, field, value.i);
            break;

        case JDWP_TAG(LONG):
            value.j = inStream_readLong(in);
            JNI_FUNC_PTR(env,SetLongField)(env, object, field, value.j);
            break;

        case JDWP_TAG(SHORT):
            value.s = inStream_readShort(in);
            JNI_FUNC_PTR(env,SetShortField)(env, object, field, value.s);
            break;

        case JDWP_TAG(BOOLEAN):
            value.z = inStream_readBoolean(in);
            JNI_FUNC_PTR(env,SetBooleanField)(env, object, field, value.z);
            break;
    }

    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        return AGENT_ERROR_JNI_EXCEPTION;
    }
    return JVMTI_ERROR_NONE;
}

static jboolean
setValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jint count;
    jvmtiError error;
    jobject object;

    env = getEnv();

    object = inStream_readObjectRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    count = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_ERROR_NONE;

    WITH_LOCAL_REFS(env, count + 1) {

        jclass clazz;

        clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);

        if (clazz != NULL ) {

            int i;

            for (i = 0; (i < count) && !inStream_error(in); i++) {

                jfieldID field;
                char *signature = NULL;

                field = inStream_readFieldID(in);
                if (inStream_error(in))
                    break;

                error = fieldSignature(clazz, field, NULL, &signature, NULL);
                if (error != JVMTI_ERROR_NONE) {
                    break;
                }

                error = readFieldValue(env, in, clazz, object, field, signature);
                jvmtiDeallocate(signature);

                if (error != JVMTI_ERROR_NONE) {
                    break;
                }
            }
        }

        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
monitorInfo(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jobject object;

    env = getEnv();

    object = inStream_readObjectRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jvmtiMonitorUsage info;

        (void)memset(&info, 0, sizeof(info));
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetObjectMonitorUsage)
                        (gdata->jvmti, object, &info);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;
            (void)outStream_writeObjectRef(env, out, info.owner);
            (void)outStream_writeInt(out, info.entry_count);
            (void)outStream_writeInt(out, info.waiter_count);
            for (i = 0; i < info.waiter_count; i++) {
                (void)outStream_writeObjectRef(env, out, info.waiters[i]);
            }
        }

        if (info.waiters != NULL )
            jvmtiDeallocate(info.waiters);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
invokeInstance(PacketInputStream *in, PacketOutputStream *out)
{
    return sharedInvoke(in, out);
}

static jboolean
disableCollection(PacketInputStream *in, PacketOutputStream *out)
{
    jlong id;
    jvmtiError error;

    id = inStream_readObjectID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = commonRef_pin(id);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }

    return JNI_TRUE;
}

static jboolean
enableCollection(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jlong id;

    id = inStream_readObjectID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = commonRef_unpin(id);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }

    return JNI_TRUE;
}

static jboolean
isCollected(PacketInputStream *in, PacketOutputStream *out)
{
    jobject ref;
    jlong id;
    JNIEnv *env;

    env = getEnv();
    id = inStream_readObjectID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (id == NULL_OBJECT_ID) {
        outStream_setError(out, JDWP_ERROR(INVALID_OBJECT));
        return JNI_TRUE;
    }

    ref = commonRef_idToRef(env, id);
    (void)outStream_writeBoolean(out, (jboolean)(ref == NULL));

    commonRef_idToRef_delete(env, ref);

    return JNI_TRUE;
}


static jboolean
referringObjects(PacketInputStream *in, PacketOutputStream *out)
{
    jobject object;
    jint    maxReferrers;
    JNIEnv *env;

    env = getEnv();

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    object = inStream_readObjectRef(env,in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    maxReferrers = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {
        jvmtiError   error;
        ObjectBatch  referrerBatch;

        error = objectReferrers(object, &referrerBatch, maxReferrers);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int kk;

            (void)outStream_writeInt(out, referrerBatch.count);
            for (kk = 0; kk < referrerBatch.count; kk++) {
                jobject ref;

                ref = referrerBatch.objects[kk];
                (void)outStream_writeByte(out, specificTypeKey(env, ref));
                (void)outStream_writeObjectRef(env, out, ref);
            }
            jvmtiDeallocate(referrerBatch.objects);
        }
    } END_WITH_LOCAL_REFS(env);
    return JNI_TRUE;
}

Command ObjectReference_Commands[] = {
    {referenceType, "ReferenceType"},
    {getValues, "GetValues"},
    {setValues, "SetValues"},
    {NULL, "<unused>"},
    {monitorInfo, "MonitorInfo"},
    {invokeInstance, "InvokeInstance"},
    {disableCollection, "DisableCollection"},
    {enableCollection, "EnableCollection"},
    {isCollected, "IsCollected"},
    {referringObjects, "ReferringObjects"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ObjectReference)
