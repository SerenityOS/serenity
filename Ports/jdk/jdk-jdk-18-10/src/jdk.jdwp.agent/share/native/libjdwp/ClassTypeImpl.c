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
#include "ClassTypeImpl.h"
#include "inStream.h"
#include "outStream.h"
#include "signature.h"

static jboolean
superclass(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jclass clazz;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jclass superclass;

        superclass = JNI_FUNC_PTR(env,GetSuperclass)(env,clazz);
        (void)outStream_writeObjectRef(env, out, superclass);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jdwpError
readStaticFieldValue(JNIEnv *env, PacketInputStream *in, jclass clazz,
                     jfieldID field, char *signature)
{
    jvalue value;
    jbyte typeKey = jdwpTag(signature);

    if (isReferenceTag(typeKey)) {
        value.l = inStream_readObjectRef(env, in);
        JNI_FUNC_PTR(env,SetStaticObjectField)(env, clazz, field, value.l);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            return JDWP_ERROR(INTERNAL);
        }
        return JDWP_ERROR(NONE);
    }

    switch (typeKey) {
        case JDWP_TAG(BYTE):
            value.b = inStream_readByte(in);
            JNI_FUNC_PTR(env,SetStaticByteField)(env, clazz, field, value.b);
            break;

        case JDWP_TAG(CHAR):
            value.c = inStream_readChar(in);
            JNI_FUNC_PTR(env,SetStaticCharField)(env, clazz, field, value.c);
            break;

        case JDWP_TAG(FLOAT):
            value.f = inStream_readFloat(in);
            JNI_FUNC_PTR(env,SetStaticFloatField)(env, clazz, field, value.f);
            break;

        case JDWP_TAG(DOUBLE):
            value.d = inStream_readDouble(in);
            JNI_FUNC_PTR(env,SetStaticDoubleField)(env, clazz, field, value.d);
            break;

        case JDWP_TAG(INT):
            value.i = inStream_readInt(in);
            JNI_FUNC_PTR(env,SetStaticIntField)(env, clazz, field, value.i);
            break;

        case JDWP_TAG(LONG):
            value.j = inStream_readLong(in);
            JNI_FUNC_PTR(env,SetStaticLongField)(env, clazz, field, value.j);
            break;

        case JDWP_TAG(SHORT):
            value.s = inStream_readShort(in);
            JNI_FUNC_PTR(env,SetStaticShortField)(env, clazz, field, value.s);
            break;

        case JDWP_TAG(BOOLEAN):
            value.z = inStream_readBoolean(in);
            JNI_FUNC_PTR(env,SetStaticBooleanField)(env, clazz, field, value.z);
            break;
    }

    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        return JDWP_ERROR(INTERNAL);
    }
    return JDWP_ERROR(NONE);
}

static jboolean
setValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jint count;
    jclass clazz;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    count = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, count) {

        int i;

        for (i = 0; i < count; i++) {

            jfieldID field;
            char *signature = NULL;
            jvmtiError error;
            jdwpError serror;

            field = inStream_readFieldID(in);
            if (inStream_error(in)) {
                break;
            }

            error = fieldSignature(clazz, field, NULL, &signature, NULL);
            if (error != JVMTI_ERROR_NONE) {
                break;
            }

            serror = readStaticFieldValue(env, in, clazz, field, signature);

            jvmtiDeallocate(signature);

            if ( serror != JDWP_ERROR(NONE) ) {
                break;
            }

        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
invokeStatic(PacketInputStream *in, PacketOutputStream *out)
{
    return sharedInvoke(in, out);
}

Command ClassType_Commands[] = {
    {superclass, "Superclass"},
    {setValues, "SetValues"},
    {invokeStatic, "InvokeMethod"},
    {invokeStatic, "NewInstance"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ClassType)
