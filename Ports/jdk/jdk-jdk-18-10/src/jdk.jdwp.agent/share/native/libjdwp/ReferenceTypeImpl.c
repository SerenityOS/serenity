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
#include "ReferenceTypeImpl.h"
#include "inStream.h"
#include "outStream.h"


static jboolean
signature(PacketInputStream *in, PacketOutputStream *out)
{
    char *signature = NULL;
    jclass clazz;
    jvmtiError error;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = classSignature(clazz, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeString(out, signature);
    jvmtiDeallocate(signature);

    return JNI_TRUE;
}

static jboolean
signatureWithGeneric(PacketInputStream *in, PacketOutputStream *out)
{
  /* Returns both the signature and the generic signature */
    char *signature = NULL;
    char *genericSignature = NULL;
    jclass clazz;
    jvmtiError error;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    error = classSignature(clazz, &signature, &genericSignature);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeString(out, signature);
    writeGenericSignature(out, genericSignature);
    jvmtiDeallocate(signature);
    if (genericSignature != NULL) {
      jvmtiDeallocate(genericSignature);
    }


    return JNI_TRUE;
}

static jboolean
getClassLoader(PacketInputStream *in, PacketOutputStream *out)
{
    jclass clazz;
    jobject loader;
    jvmtiError error;
    JNIEnv *env;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = classLoader(clazz, &loader);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeObjectRef(env, out, loader);
    return JNI_TRUE;
}

static jboolean
getModule(PacketInputStream *in, PacketOutputStream *out)
{
    jobject clazz;
    jobject module;
    JNIEnv *env;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    module = JNI_FUNC_PTR(env, GetModule)(env, clazz);

    (void)outStream_writeModuleRef(env, out, module);
    return JNI_TRUE;
}

static jboolean
modifiers(PacketInputStream *in, PacketOutputStream *out)
{
    jint modifiers;
    jclass clazz;
    jvmtiError error;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassModifiers)
                (gdata->jvmti, clazz, &modifiers);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, modifiers);

    return JNI_TRUE;
}

static void
writeMethodInfo(PacketOutputStream *out, jclass clazz, jmethodID method,
                int outputGenerics)
{
    char *name = NULL;
    char *signature = NULL;
    char *genericSignature = NULL;
    jint modifiers;
    jvmtiError error;
    jboolean isSynthetic;

    error = isMethodSynthetic(method, &isSynthetic);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }

    error = methodModifiers(method, &modifiers);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }

    error = methodSignature(method, &name, &signature, &genericSignature);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }

    if (isSynthetic) {
        modifiers |= MOD_SYNTHETIC;
    }
    (void)outStream_writeMethodID(out, method);
    (void)outStream_writeString(out, name);
    (void)outStream_writeString(out, signature);
    if (outputGenerics == 1) {
        writeGenericSignature(out, genericSignature);
    }
    (void)outStream_writeInt(out, modifiers);
    jvmtiDeallocate(name);
    jvmtiDeallocate(signature);
    if (genericSignature != NULL) {
      jvmtiDeallocate(genericSignature);
    }
}

static jboolean
methods1(PacketInputStream *in, PacketOutputStream *out,
         int outputGenerics)
{
    int i;
    jclass clazz;
    jint methodCount = 0;
    jmethodID *methods = NULL;
    jvmtiError error;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassMethods)
                (gdata->jvmti, clazz, &methodCount, &methods);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, methodCount);
    for (i = 0; (i < methodCount) && !outStream_error(out); i++) {
        writeMethodInfo(out, clazz, methods[i], outputGenerics);
    }

    /* Free methods array */
    if ( methods != NULL ) {
        jvmtiDeallocate(methods);
    }
    return JNI_TRUE;
}

static jboolean
methods(PacketInputStream *in, PacketOutputStream *out)
{
    return methods1(in, out, 0);
}

static jboolean
methodsWithGeneric(PacketInputStream *in, PacketOutputStream *out)
{
    return methods1(in, out, 1);
}



static jboolean
instances(PacketInputStream *in, PacketOutputStream *out)
{
    jint maxInstances;
    jclass clazz;
    JNIEnv *env;

    if (gdata->vmDead) {
        outStream_setError(out, JDWP_ERROR(VM_DEAD));
        return JNI_TRUE;
    }

    env = getEnv();
    clazz = inStream_readClassRef(env, in);
    maxInstances = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {
        jvmtiError   error;
        ObjectBatch  batch;

        error = classInstances(clazz, &batch, maxInstances);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int kk;
            jbyte typeKey;

            (void)outStream_writeInt(out, batch.count);
            if (batch.count > 0) {
                /*
                 * They are all instances of this class and will all have
                 * the same typeKey, so just compute it once.
                 */
                typeKey = specificTypeKey(env, batch.objects[0]);

                for (kk = 0; kk < batch.count; kk++) {
                  jobject inst;

                  inst = batch.objects[kk];
                  (void)outStream_writeByte(out, typeKey);
                  (void)outStream_writeObjectRef(env, out, inst);
                }
            }
            jvmtiDeallocate(batch.objects);
        }
    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
getClassVersion(PacketInputStream *in, PacketOutputStream *out)
{
    jclass clazz;
    jvmtiError error;
    jint majorVersion;
    jint minorVersion;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti, GetClassVersionNumbers)
                (gdata->jvmti, clazz, &minorVersion, &majorVersion);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, majorVersion);
    (void)outStream_writeInt(out, minorVersion);

    return JNI_TRUE;
}

static jboolean
getConstantPool(PacketInputStream *in, PacketOutputStream *out)
{

    jclass clazz;
    jvmtiError error;
    jint cpCount;
    jint cpByteCount;
    unsigned char* cpBytesPtr;


    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /* Initialize assuming no bytecodes and no error */
    error         = JVMTI_ERROR_NONE;
    cpCount       = 0;
    cpByteCount   = 0;
    cpBytesPtr    = NULL;


    error = JVMTI_FUNC_PTR(gdata->jvmti,GetConstantPool)
                (gdata->jvmti, clazz, &cpCount, &cpByteCount, &cpBytesPtr);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    } else {
        (void)outStream_writeInt(out, cpCount);
        (void)outStream_writeByteArray(out, cpByteCount, (jbyte *)cpBytesPtr);
        jvmtiDeallocate(cpBytesPtr);
    }

    return JNI_TRUE;
}

static void
writeFieldInfo(PacketOutputStream *out, jclass clazz, jfieldID fieldID,
               int outputGenerics)
{
    char *name;
    char *signature = NULL;
    char *genericSignature = NULL;
    jint modifiers;
    jboolean isSynthetic;
    jvmtiError error;

    error = isFieldSynthetic(clazz, fieldID, &isSynthetic);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }

    error = fieldModifiers(clazz, fieldID, &modifiers);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }

    error = fieldSignature(clazz, fieldID, &name, &signature, &genericSignature);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }
    if (isSynthetic) {
        modifiers |= MOD_SYNTHETIC;
    }
    (void)outStream_writeFieldID(out, fieldID);
    (void)outStream_writeString(out, name);
    (void)outStream_writeString(out, signature);
    if (outputGenerics == 1) {
        writeGenericSignature(out, genericSignature);
    }
    (void)outStream_writeInt(out, modifiers);
    jvmtiDeallocate(name);
    jvmtiDeallocate(signature);
    if (genericSignature != NULL) {
      jvmtiDeallocate(genericSignature);
    }
}

static jboolean
fields1(PacketInputStream *in, PacketOutputStream *out, int outputGenerics)
{
    int i;
    jclass clazz;
    jint fieldCount = 0;
    jfieldID *fields = NULL;
    jvmtiError error;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassFields)
                (gdata->jvmti, clazz, &fieldCount, &fields);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, fieldCount);
    for (i = 0; (i < fieldCount) && !outStream_error(out); i++) {
        writeFieldInfo(out, clazz, fields[i], outputGenerics);
    }

    /* Free fields array */
    if ( fields != NULL ) {
        jvmtiDeallocate(fields);
    }
    return JNI_TRUE;
}


static jboolean
fields(PacketInputStream *in, PacketOutputStream *out)
{
    return fields1(in, out, 0);
}

static jboolean
fieldsWithGeneric(PacketInputStream *in, PacketOutputStream *out)
{
    return fields1(in, out, 1);

}

static jboolean
getValues(PacketInputStream *in, PacketOutputStream *out)
{
    sharedGetFieldValues(in, out, JNI_TRUE);
    return JNI_TRUE;
}

static jboolean
sourceFile(PacketInputStream *in, PacketOutputStream *out)
{
    char *fileName;
    jvmtiError error;
    jclass clazz;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetSourceFileName)
                (gdata->jvmti, clazz, &fileName);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeString(out, fileName);
    jvmtiDeallocate(fileName);
    return JNI_TRUE;
}

static jboolean
sourceDebugExtension(PacketInputStream *in, PacketOutputStream *out)
{
    char *extension;
    jvmtiError error;
    jclass clazz;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    error = getSourceDebugExtension(clazz, &extension);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeString(out, extension);
    jvmtiDeallocate(extension);
    return JNI_TRUE;
}

static jboolean
nestedTypes(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jclass clazz;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jint count;
        jclass *nested;

        error = allNestedClasses(clazz, &nested, &count);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;
            (void)outStream_writeInt(out, count);
            for (i = 0; i < count; i++) {
                (void)outStream_writeByte(out, referenceTypeTag(nested[i]));
                (void)outStream_writeObjectRef(env, out, nested[i]);
            }
            if ( nested != NULL ) {
                jvmtiDeallocate(nested);
            }
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
getClassStatus(PacketInputStream *in, PacketOutputStream *out)
{
    jint status;
    jclass clazz;

    clazz = inStream_readClassRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    status = classStatus(clazz);
    (void)outStream_writeInt(out, map2jdwpClassStatus(status));
    return JNI_TRUE;
}

static jboolean
interfaces(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jclass clazz;

    env = getEnv();

    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jint interfaceCount;
        jclass *interfaces;

        error = allInterfaces(clazz, &interfaces, &interfaceCount);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;

            (void)outStream_writeInt(out, interfaceCount);
            for (i = 0; i < interfaceCount; i++) {
                (void)outStream_writeObjectRef(env, out, interfaces[i]);
            }
            if ( interfaces != NULL ) {
                jvmtiDeallocate(interfaces);
            }
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
classObject(PacketInputStream *in, PacketOutputStream *out)
{
    jclass clazz;
    JNIEnv *env;

    env = getEnv();
    clazz = inStream_readClassRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * In our implementation, the reference type id is the same as the
     * class object id, so we bounce it right back.
     *
     */

    (void)outStream_writeObjectRef(env, out, clazz);

    return JNI_TRUE;
}

Command ReferenceType_Commands[] = {
    {signature, "Signature"},
    {getClassLoader, "GetClassLoader"},
    {modifiers, "Modifiers"},
    {fields, "Fields"},
    {methods, "Methods"},
    {getValues, "GetValues"},
    {sourceFile, "SourceFile"},
    {nestedTypes, "NestedTypes"},
    {getClassStatus, "GetClassStatus"},
    {interfaces, "Interfaces"},
    {classObject, "ClassObject"},
    {sourceDebugExtension, "SourceDebugExtension"},
    {signatureWithGeneric, "SignatureWithGeneric"},
    {fieldsWithGeneric, "FieldsWithGeneric"},
    {methodsWithGeneric, "MethodsWithGeneric"},
    {instances, "Instances"},
    {getClassVersion, "GetClassVersion"},
    {getConstantPool, "GetConstantPool"},
    {getModule, "GetModule"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ReferenceType)
