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
#include "ArrayReferenceImpl.h"
#include "inStream.h"
#include "outStream.h"
#include "signature.h"

static jboolean
length(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env = getEnv();
    jsize arrayLength;

    jarray  array = inStream_readArrayRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    arrayLength = JNI_FUNC_PTR(env,GetArrayLength)(env, array);
    (void)outStream_writeInt(out, arrayLength);
    return JNI_TRUE;
}

static void *
newComponents(PacketOutputStream *out, jint length, size_t nbytes)
{
    void *ptr = NULL;

    if ( length > 0 ) {
        ptr = jvmtiAllocate(length*((jint)nbytes));
        if ( ptr == NULL ) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        } else {
            (void)memset(ptr, 0, length*nbytes);
        }
    }
    return ptr;
}

static void
deleteComponents(void *ptr)
{
    jvmtiDeallocate(ptr);
}

static void
writeBooleanComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jboolean *components;

    components = newComponents(out, length, sizeof(jboolean));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetBooleanArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeBoolean(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeByteComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jbyte *components;

    components = newComponents(out, length, sizeof(jbyte));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetByteArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeByte(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeCharComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jchar *components;

    components = newComponents(out, length, sizeof(jchar));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetCharArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeChar(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeShortComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jshort *components;

    components = newComponents(out, length, sizeof(jshort));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetShortArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeShort(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeIntComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jint *components;

    components = newComponents(out, length, sizeof(jint));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetIntArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeInt(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeLongComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jlong *components;

    components = newComponents(out, length, sizeof(jlong));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetLongArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeLong(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeFloatComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jfloat *components;

    components = newComponents(out, length, sizeof(jfloat));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetFloatArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeFloat(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeDoubleComponents(JNIEnv *env, PacketOutputStream *out,
                    jarray array, jint index, jint length)
{
    jdouble *components;

    components = newComponents(out, length, sizeof(jdouble));
    if (components != NULL) {
        jint i;
        JNI_FUNC_PTR(env,GetDoubleArrayRegion)(env, array, index, length, components);
        for (i = 0; i < length; i++) {
            (void)outStream_writeDouble(out, components[i]);
        }
        deleteComponents(components);
    }
}

static void
writeObjectComponents(JNIEnv *env, PacketOutputStream *out,
                      jarray array, jint index, jint length)
{

    WITH_LOCAL_REFS(env, length) {

        int i;
        jobject component;

        for (i = 0; i < length; i++) {
            component = JNI_FUNC_PTR(env,GetObjectArrayElement)(env, array, index + i);
            if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
                /* cleared by caller */
                break;
            }
            (void)outStream_writeByte(out, specificTypeKey(env, component));
            (void)outStream_writeObjectRef(env, out, component);
        }

    } END_WITH_LOCAL_REFS(env);
}

static void writeComponents(JNIEnv *env, PacketOutputStream *out, char *signature,
                            jarray array, jint index, jint length);

static jboolean
getValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env = getEnv();
    jint arrayLength;
    jarray array;
    jint index;
    jint length;

    array = inStream_readArrayRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    index = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    length = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    arrayLength = JNI_FUNC_PTR(env,GetArrayLength)(env, array);

    if (length == -1) {
        length = arrayLength - index;
    }

    if ((index < 0) || (index > arrayLength - 1)) {
        outStream_setError(out, JDWP_ERROR(INVALID_INDEX));
        return JNI_TRUE;
    }

    if ((length < 0) || (length + index > arrayLength)) {
        outStream_setError(out, JDWP_ERROR(INVALID_LENGTH));
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        char *signature = NULL;

        jclass arrayClass = JNI_FUNC_PTR(env,GetObjectClass)(env, array);
        jvmtiError error = classSignature(arrayClass, &signature, NULL);
        if (error == JVMTI_ERROR_NONE) {
            writeComponents(env, out, signature, array, index, length);
            jvmtiDeallocate(signature);
        }

    } END_WITH_LOCAL_REFS(env);

    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        outStream_setError(out, JDWP_ERROR(INTERNAL));
        JNI_FUNC_PTR(env,ExceptionClear)(env);
    }

    return JNI_TRUE;
}

static void writeComponents(JNIEnv *env, PacketOutputStream *out, char *signature,
                            jarray array, jint index, jint length) {

    char * componentSignature = componentTypeSignature(signature);
    jbyte typeKey = jdwpTag(componentSignature);

    (void)outStream_writeByte(out, typeKey);
    (void)outStream_writeInt(out, length);

    if (isReferenceTag(typeKey)) {
        writeObjectComponents(env, out, array, index, length);
        return;
    }
    switch (typeKey) {
        case JDWP_TAG(BYTE):
            writeByteComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(CHAR):
            writeCharComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(FLOAT):
            writeFloatComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(DOUBLE):
            writeDoubleComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(INT):
            writeIntComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(LONG):
            writeLongComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(SHORT):
            writeShortComponents(env, out, array, index, length);
            break;

        case JDWP_TAG(BOOLEAN):
            writeBooleanComponents(env, out, array, index, length);
            break;

        default:
            outStream_setError(out, JDWP_ERROR(INVALID_TAG));
            break;
    }
}

static jdwpError
readBooleanComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jboolean component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readBoolean(in);
        JNI_FUNC_PTR(env,SetBooleanArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readByteComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jbyte component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readByte(in);
        JNI_FUNC_PTR(env,SetByteArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readCharComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jchar component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readChar(in);
        JNI_FUNC_PTR(env,SetCharArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readShortComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jshort component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readShort(in);
        JNI_FUNC_PTR(env,SetShortArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readIntComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jint component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readInt(in);
        JNI_FUNC_PTR(env,SetIntArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readLongComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
#if defined (_WIN32) && defined (_MSC_VER)
    __declspec(align(8))
#endif
    jlong component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readLong(in);
        JNI_FUNC_PTR(env,SetLongArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readFloatComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
    jfloat component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readFloat(in);
        JNI_FUNC_PTR(env,SetFloatArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}

static jdwpError
readDoubleComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
{
    int i;
#if defined (_WIN32) && defined (_MSC_VER)
    __declspec(align(8))
#endif
    jdouble component;

    for (i = 0; (i < length) && !inStream_error(in); i++) {
        component = inStream_readDouble(in);
        JNI_FUNC_PTR(env,SetDoubleArrayRegion)(env, array, index + i, 1, &component);
    }
    return inStream_error(in);
}


static jdwpError
readObjectComponents(JNIEnv *env, PacketInputStream *in,
                   jarray array, int index, int length)
                   /* char *componentSignature) */
{
    int i;

    for (i = 0; i < length; i++) {
        jobject object = inStream_readObjectRef(env, in);

        JNI_FUNC_PTR(env,SetObjectArrayElement)(env, array, index + i, object);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            /* caller will clear */
            break;
        }
    }

    return JDWP_ERROR(NONE);
}

static jdwpError readComponents(JNIEnv *env, PacketInputStream *in, char *signature,
                                jarray array, jint index, jint length);

static jboolean
setValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env = getEnv();
    jdwpError serror = JDWP_ERROR(NONE);
    int arrayLength;
    jarray array;
    jint index;
    jint length;

    array = inStream_readArrayRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    index = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    length = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    arrayLength = JNI_FUNC_PTR(env,GetArrayLength)(env, array);

    if ((index < 0) || (index > arrayLength - 1)) {
        outStream_setError(out, JDWP_ERROR(INVALID_INDEX));
        return JNI_TRUE;
    }

    if ((length < 0) || (length + index > arrayLength)) {
        outStream_setError(out, JDWP_ERROR(INVALID_LENGTH));
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1)  {

        char *signature = NULL;

        jclass arrayClass = JNI_FUNC_PTR(env,GetObjectClass)(env, array);
        jvmtiError error = classSignature(arrayClass, &signature, NULL);
        if (error == JVMTI_ERROR_NONE) {
            serror = readComponents(env, in, signature, array, index, length);
            jvmtiDeallocate(signature);
        }
    } END_WITH_LOCAL_REFS(env);

    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        /*
         * TO DO: Check exception type
         */
        serror = JDWP_ERROR(TYPE_MISMATCH);
        JNI_FUNC_PTR(env,ExceptionClear)(env);
    }

    outStream_setError(out, serror);
    return JNI_TRUE;
}

static jdwpError readComponents(JNIEnv *env, PacketInputStream *in, char *signature,
jarray array, jint index, jint length) {
    jdwpError serror = JDWP_ERROR(NONE);

    char *componentSignature = componentTypeSignature(signature);
    jbyte typeKey = jdwpTag(componentSignature);
    if (isReferenceTag(typeKey)) {
        serror = readObjectComponents(env, in, array, index, length);
        return serror;
    }
    switch (typeKey) {
        case JDWP_TAG(BYTE):
            serror = readByteComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(CHAR):
            serror = readCharComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(FLOAT):
            serror = readFloatComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(DOUBLE):
            serror = readDoubleComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(INT):
            serror = readIntComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(LONG):
            serror = readLongComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(SHORT):
            serror = readShortComponents(env, in, array, index, length);
            break;

        case JDWP_TAG(BOOLEAN):
            serror = readBooleanComponents(env, in, array, index, length);
            break;

        default:
            {
                ERROR_MESSAGE(("Invalid array component signature: %s",
                                    componentSignature));
                EXIT_ERROR(AGENT_ERROR_INVALID_OBJECT,NULL);
            }
            break;
    }

    return serror;
}

Command ArrayReference_Commands[] = {
    {length, "Length"},
    {getValues, "GetValues"},
    {setValues, "SetValues"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ArrayReference)
