/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <ctype.h>

#include "util.h"
#include "utf_util.h"
#include "transport.h"
#include "eventHandler.h"
#include "threadControl.h"
#include "outStream.h"
#include "inStream.h"
#include "invoker.h"
#include "signature.h"


/* Global data area */
BackendGlobalData *gdata = NULL;

/* Forward declarations */
static jboolean isInterface(jclass clazz);
static jboolean isArrayClass(jclass clazz);
static char * getPropertyUTF8(JNIEnv *env, char *propertyName);

/* Save an object reference for use later (create a NewGlobalRef) */
void
saveGlobalRef(JNIEnv *env, jobject obj, jobject *pobj)
{
    jobject newobj;

    if ( pobj == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"saveGlobalRef pobj");
    }
    if ( *pobj != NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"saveGlobalRef *pobj");
    }
    if ( env == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"saveGlobalRef env");
    }
    if ( obj == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"saveGlobalRef obj");
    }
    newobj = JNI_FUNC_PTR(env,NewGlobalRef)(env, obj);
    if ( newobj == NULL ) {
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"NewGlobalRef");
    }
    *pobj = newobj;
}

/* Toss a previously saved object reference */
void
tossGlobalRef(JNIEnv *env, jobject *pobj)
{
    jobject obj;

    if ( pobj == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"tossGlobalRef pobj");
    }
    obj = *pobj;
    if ( env == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"tossGlobalRef env");
    }
    if ( obj == NULL ) {
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"tossGlobalRef obj");
    }
    JNI_FUNC_PTR(env,DeleteGlobalRef)(env, obj);
    *pobj = NULL;
}

jclass
findClass(JNIEnv *env, const char * name)
{
    jclass x;

    if ( env == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"findClass env");
    }
    if ( name == NULL || name[0] == 0 ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"findClass name");
    }
    x = JNI_FUNC_PTR(env,FindClass)(env, name);
    if (x == NULL) {
        ERROR_MESSAGE(("JDWP Can't find class %s", name));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    if ( JNI_FUNC_PTR(env,ExceptionOccurred)(env) ) {
        ERROR_MESSAGE(("JDWP Exception occurred finding class %s", name));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    return x;
}

jmethodID
getMethod(JNIEnv *env, jclass clazz, const char * name, const char *signature)
{
    jmethodID method;

    if ( env == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getMethod env");
    }
    if ( clazz == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getMethod clazz");
    }
    if ( name == NULL || name[0] == 0 ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getMethod name");
    }
    if ( signature == NULL || signature[0] == 0 ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getMethod signature");
    }
    method = JNI_FUNC_PTR(env,GetMethodID)(env, clazz, name, signature);
    if (method == NULL) {
        ERROR_MESSAGE(("JDWP Can't find method %s with signature %s",
                                name, signature));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    if ( JNI_FUNC_PTR(env,ExceptionOccurred)(env) ) {
        ERROR_MESSAGE(("JDWP Exception occurred finding method %s with signature %s",
                                name, signature));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    return method;
}

static jmethodID
getStaticMethod(JNIEnv *env, jclass clazz, const char * name, const char *signature)
{
    jmethodID method;

    if ( env == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getStaticMethod env");
    }
    if ( clazz == NULL ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getStaticMethod clazz");
    }
    if ( name == NULL || name[0] == 0 ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getStaticMethod name");
    }
    if ( signature == NULL || signature[0] == 0 ) {
        EXIT_ERROR(AGENT_ERROR_ILLEGAL_ARGUMENT,"getStaticMethod signature");
    }
    method = JNI_FUNC_PTR(env,GetStaticMethodID)(env, clazz, name, signature);
    if (method == NULL) {
        ERROR_MESSAGE(("JDWP Can't find method %s with signature %s",
                                name, signature));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    if ( JNI_FUNC_PTR(env,ExceptionOccurred)(env) ) {
        ERROR_MESSAGE(("JDWP Exception occurred finding method %s with signature %s",
                                name, signature));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    return method;
}



void
util_initialize(JNIEnv *env)
{
    WITH_LOCAL_REFS(env, 6) {

        jvmtiError error;
        jclass localClassClass;
        jclass localThreadClass;
        jclass localThreadGroupClass;
        jclass localClassLoaderClass;
        jclass localStringClass;
        jclass localSystemClass;
        jclass localPropertiesClass;
        jclass localVMSupportClass;
        jobject localAgentProperties;
        jmethodID getAgentProperties;
        jint groupCount;
        jthreadGroup *groups;
        jthreadGroup localSystemThreadGroup;

        /* Find some standard classes */

        localClassClass         = findClass(env,"java/lang/Class");
        localThreadClass        = findClass(env,"java/lang/Thread");
        localThreadGroupClass   = findClass(env,"java/lang/ThreadGroup");
        localClassLoaderClass   = findClass(env,"java/lang/ClassLoader");
        localStringClass        = findClass(env,"java/lang/String");
        localSystemClass        = findClass(env,"java/lang/System");
        localPropertiesClass    = findClass(env,"java/util/Properties");

        /* Save references */

        saveGlobalRef(env, localClassClass,       &(gdata->classClass));
        saveGlobalRef(env, localThreadClass,      &(gdata->threadClass));
        saveGlobalRef(env, localThreadGroupClass, &(gdata->threadGroupClass));
        saveGlobalRef(env, localClassLoaderClass, &(gdata->classLoaderClass));
        saveGlobalRef(env, localStringClass,      &(gdata->stringClass));
        saveGlobalRef(env, localSystemClass,      &(gdata->systemClass));

        /* Find some standard methods */

        gdata->threadConstructor =
                getMethod(env, gdata->threadClass,
                    "<init>", "(Ljava/lang/ThreadGroup;Ljava/lang/String;)V");
        gdata->threadSetDaemon =
                getMethod(env, gdata->threadClass, "setDaemon", "(Z)V");
        gdata->threadResume =
                getMethod(env, gdata->threadClass, "resume", "()V");
        gdata->systemGetProperty =
                getStaticMethod(env, gdata->systemClass,
                    "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
        gdata->setProperty =
                getMethod(env, localPropertiesClass,
                    "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");

        /* Find the system thread group */

        groups = NULL;
        groupCount = 0;
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetTopThreadGroups)
                    (gdata->jvmti, &groupCount, &groups);
        if (error != JVMTI_ERROR_NONE ) {
            EXIT_ERROR(error, "Can't get system thread group");
        }
        if ( groupCount == 0 ) {
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER, "Can't get system thread group");
        }
        localSystemThreadGroup = groups[0];
        saveGlobalRef(env, localSystemThreadGroup, &(gdata->systemThreadGroup));

        /* Get some basic Java property values we will need at some point */
        gdata->property_java_version
                        = getPropertyUTF8(env, "java.version");
        gdata->property_java_vm_name
                        = getPropertyUTF8(env, "java.vm.name");
        gdata->property_java_vm_info
                        = getPropertyUTF8(env, "java.vm.info");
        gdata->property_java_class_path
                        = getPropertyUTF8(env, "java.class.path");
        gdata->property_sun_boot_library_path
                        = getPropertyUTF8(env, "sun.boot.library.path");
        gdata->property_path_separator
                        = getPropertyUTF8(env, "path.separator");
        gdata->property_user_dir
                        = getPropertyUTF8(env, "user.dir");

        /* Get agent properties: invoke VMSupport.getAgentProperties */
        localVMSupportClass = JNI_FUNC_PTR(env,FindClass)
                                          (env, "jdk/internal/vm/VMSupport");
        if (localVMSupportClass == NULL) {
            gdata->agent_properties = NULL;
            if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
                JNI_FUNC_PTR(env,ExceptionClear)(env);
            }
        } else {
            getAgentProperties  =
                getStaticMethod(env, localVMSupportClass,
                                "getAgentProperties", "()Ljava/util/Properties;");
            localAgentProperties =
                JNI_FUNC_PTR(env,CallStaticObjectMethod)
                            (env, localVMSupportClass, getAgentProperties);
            saveGlobalRef(env, localAgentProperties, &(gdata->agent_properties));
            if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
                JNI_FUNC_PTR(env,ExceptionClear)(env);
                EXIT_ERROR(AGENT_ERROR_INTERNAL,
                    "Exception occurred calling VMSupport.getAgentProperties");
            }
        }

    } END_WITH_LOCAL_REFS(env);

}

void
util_reset(void)
{
}

jboolean
isObjectTag(jbyte tag) {
    return (tag == JDWP_TAG(OBJECT)) ||
           (tag == JDWP_TAG(STRING)) ||
           (tag == JDWP_TAG(THREAD)) ||
           (tag == JDWP_TAG(THREAD_GROUP)) ||
           (tag == JDWP_TAG(CLASS_LOADER)) ||
           (tag == JDWP_TAG(CLASS_OBJECT)) ||
           (tag == JDWP_TAG(ARRAY));
}

jbyte
specificTypeKey(JNIEnv *env, jobject object)
{
    if (object == NULL) {
        return JDWP_TAG(OBJECT);
    } else if (JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->stringClass)) {
        return JDWP_TAG(STRING);
    } else if (JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->threadClass)) {
        return JDWP_TAG(THREAD);
    } else if (JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->threadGroupClass)) {
        return JDWP_TAG(THREAD_GROUP);
    } else if (JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->classLoaderClass)) {
        return JDWP_TAG(CLASS_LOADER);
    } else if (JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->classClass)) {
        return JDWP_TAG(CLASS_OBJECT);
    } else {
        jboolean classIsArray;

        WITH_LOCAL_REFS(env, 1) {
            jclass clazz;
            clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);
            classIsArray = isArrayClass(clazz);
        } END_WITH_LOCAL_REFS(env);

        return (classIsArray ? JDWP_TAG(ARRAY) : JDWP_TAG(OBJECT));
    }
}

static void
writeFieldValue(JNIEnv *env, PacketOutputStream *out, jobject object,
                jfieldID field)
{
    jclass clazz;
    char *signature = NULL;
    jvmtiError error;
    jbyte typeKey;

    clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);
    error = fieldSignature(clazz, field, NULL, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }
    typeKey = jdwpTag(signature);
    jvmtiDeallocate(signature);

    if (isReferenceTag(typeKey)) {

        jobject value = JNI_FUNC_PTR(env,GetObjectField)(env, object, field);
        (void)outStream_writeByte(out, specificTypeKey(env, value));
        (void)outStream_writeObjectRef(env, out, value);
        return;

    }

    /*
     * For primitive types, the type key is bounced back as is.
     */

    (void)outStream_writeByte(out, typeKey);

    switch (typeKey) {
        case JDWP_TAG(BYTE):
            (void)outStream_writeByte(out,
                      JNI_FUNC_PTR(env,GetByteField)(env, object, field));
            break;

        case JDWP_TAG(CHAR):
            (void)outStream_writeChar(out,
                      JNI_FUNC_PTR(env,GetCharField)(env, object, field));
            break;

        case JDWP_TAG(FLOAT):
            (void)outStream_writeFloat(out,
                      JNI_FUNC_PTR(env,GetFloatField)(env, object, field));
            break;

        case JDWP_TAG(DOUBLE):
            (void)outStream_writeDouble(out,
                      JNI_FUNC_PTR(env,GetDoubleField)(env, object, field));
            break;

        case JDWP_TAG(INT):
            (void)outStream_writeInt(out,
                      JNI_FUNC_PTR(env,GetIntField)(env, object, field));
            break;

        case JDWP_TAG(LONG):
            (void)outStream_writeLong(out,
                      JNI_FUNC_PTR(env,GetLongField)(env, object, field));
            break;

        case JDWP_TAG(SHORT):
            (void)outStream_writeShort(out,
                      JNI_FUNC_PTR(env,GetShortField)(env, object, field));
            break;

        case JDWP_TAG(BOOLEAN):
            (void)outStream_writeBoolean(out,
                      JNI_FUNC_PTR(env,GetBooleanField)(env, object, field));
            break;
    }
}

static void
writeStaticFieldValue(JNIEnv *env, PacketOutputStream *out, jclass clazz,
                      jfieldID field)
{
    jvmtiError error;
    char *signature = NULL;
    jbyte typeKey;

    error = fieldSignature(clazz, field, NULL, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return;
    }
    typeKey = jdwpTag(signature);
    jvmtiDeallocate(signature);


    if (isReferenceTag(typeKey)) {

        jobject value = JNI_FUNC_PTR(env,GetStaticObjectField)(env, clazz, field);
        (void)outStream_writeByte(out, specificTypeKey(env, value));
        (void)outStream_writeObjectRef(env, out, value);

        return;
    }

    /*
     * For primitive types, the type key is bounced back as is.
     */
    (void)outStream_writeByte(out, typeKey);
    switch (typeKey) {
        case JDWP_TAG(BYTE):
            (void)outStream_writeByte(out,
                      JNI_FUNC_PTR(env,GetStaticByteField)(env, clazz, field));
            break;

        case JDWP_TAG(CHAR):
            (void)outStream_writeChar(out,
                      JNI_FUNC_PTR(env,GetStaticCharField)(env, clazz, field));
            break;

        case JDWP_TAG(FLOAT):
            (void)outStream_writeFloat(out,
                      JNI_FUNC_PTR(env,GetStaticFloatField)(env, clazz, field));
            break;

        case JDWP_TAG(DOUBLE):
            (void)outStream_writeDouble(out,
                      JNI_FUNC_PTR(env,GetStaticDoubleField)(env, clazz, field));
            break;

        case JDWP_TAG(INT):
            (void)outStream_writeInt(out,
                      JNI_FUNC_PTR(env,GetStaticIntField)(env, clazz, field));
            break;

        case JDWP_TAG(LONG):
            (void)outStream_writeLong(out,
                      JNI_FUNC_PTR(env,GetStaticLongField)(env, clazz, field));
            break;

        case JDWP_TAG(SHORT):
            (void)outStream_writeShort(out,
                      JNI_FUNC_PTR(env,GetStaticShortField)(env, clazz, field));
            break;

        case JDWP_TAG(BOOLEAN):
            (void)outStream_writeBoolean(out,
                      JNI_FUNC_PTR(env,GetStaticBooleanField)(env, clazz, field));
            break;
    }
}

void
sharedGetFieldValues(PacketInputStream *in, PacketOutputStream *out,
                     jboolean isStatic)
{
    JNIEnv *env = getEnv();
    jint length;
    jobject object;
    jclass clazz;

    object = NULL;
    clazz  = NULL;

    if (isStatic) {
        clazz = inStream_readClassRef(env, in);
    } else {
        object = inStream_readObjectRef(env, in);
    }

    length = inStream_readInt(in);
    if (inStream_error(in)) {
        return;
    }

    WITH_LOCAL_REFS(env, length + 1) { /* +1 for class with instance fields */

        int i;

        (void)outStream_writeInt(out, length);
        for (i = 0; (i < length) && !outStream_error(out); i++) {
            jfieldID field = inStream_readFieldID(in);

            if (isStatic) {
                writeStaticFieldValue(env, out, clazz, field);
            } else {
                writeFieldValue(env, out, object, field);
            }
        }

    } END_WITH_LOCAL_REFS(env);
}

jboolean
sharedInvoke(PacketInputStream *in, PacketOutputStream *out)
{
    jvalue *arguments = NULL;
    jint options;
    jvmtiError error;
    jbyte invokeType;
    jclass clazz;
    jmethodID method;
    jint argumentCount;
    jobject instance;
    jthread thread;
    JNIEnv *env;

    /*
     * Instance methods start with the instance, thread and class,
     * and statics and constructors start with the class and then the
     * thread.
     */
    env = getEnv();
    if (inStream_command(in) == JDWP_COMMAND(ObjectReference, InvokeMethod)) {
        instance = inStream_readObjectRef(env, in);
        thread = inStream_readThreadRef(env, in);
        clazz = inStream_readClassRef(env, in);
    } else { /* static method or constructor */
        instance = NULL;
        clazz = inStream_readClassRef(env, in);
        thread = inStream_readThreadRef(env, in);
    }

    /*
     * ... and the rest of the packet is identical for all commands
     */
    method = inStream_readMethodID(in);
    argumentCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /* If count == 0, don't try and allocate 0 bytes, you'll get NULL */
    if ( argumentCount > 0 ) {
        int i;
        /*LINTED*/
        arguments = jvmtiAllocate(argumentCount * (jint)sizeof(*arguments));
        if (arguments == NULL) {
            outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
            return JNI_TRUE;
        }
        for (i = 0; (i < argumentCount) && !inStream_error(in); i++) {
            arguments[i] = inStream_readValue(in);
        }
        if (inStream_error(in)) {
            return JNI_TRUE;
        }
    }

    options = inStream_readInt(in);
    if (inStream_error(in)) {
        if ( arguments != NULL ) {
            jvmtiDeallocate(arguments);
        }
        return JNI_TRUE;
    }

    if (inStream_command(in) == JDWP_COMMAND(ClassType, NewInstance)) {
        invokeType = INVOKE_CONSTRUCTOR;
    } else if (inStream_command(in) == JDWP_COMMAND(ClassType, InvokeMethod)) {
        invokeType = INVOKE_STATIC;
    } else if (inStream_command(in) == JDWP_COMMAND(InterfaceType, InvokeMethod)) {
        invokeType = INVOKE_STATIC;
    } else if (inStream_command(in) == JDWP_COMMAND(ObjectReference, InvokeMethod)) {
        invokeType = INVOKE_INSTANCE;
    } else {
        outStream_setError(out, JDWP_ERROR(INTERNAL));
        if ( arguments != NULL ) {
            jvmtiDeallocate(arguments);
        }
        return JNI_TRUE;
    }

    /*
     * Request the invoke. If there are no errors in the request,
     * the interrupting thread will actually do the invoke and a
     * reply will be generated subsequently, so we don't reply here.
     */
    error = invoker_requestInvoke(invokeType, (jbyte)options, inStream_id(in),
                                  thread, clazz, method,
                                  instance, arguments, argumentCount);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        if ( arguments != NULL ) {
            jvmtiDeallocate(arguments);
        }
        return JNI_TRUE;
    }

    return JNI_FALSE;   /* Don't reply */
}

jint
uniqueID(void)
{
    static jint currentID = 0;
    return currentID++;
}

int
filterDebugThreads(jthread *threads, int count)
{
    int i;
    int current;

    /* Squish out all of the debugger-spawned threads */
    for (i = 0, current = 0; i < count; i++) {
        jthread thread = threads[i];
        if (!threadControl_isDebugThread(thread)) {
            if (i > current) {
                threads[current] = thread;
            }
            current++;
        }
    }
    return current;
}

jbyte
referenceTypeTag(jclass clazz)
{
    jbyte tag;

    if (isInterface(clazz)) {
        tag = JDWP_TYPE_TAG(INTERFACE);
    } else if (isArrayClass(clazz)) {
        tag = JDWP_TYPE_TAG(ARRAY);
    } else {
        tag = JDWP_TYPE_TAG(CLASS);
    }

    return tag;
}

/**
 * Get field modifiers
 */
jvmtiError
fieldModifiers(jclass clazz, jfieldID field, jint *pmodifiers)
{
    jvmtiError error;

    *pmodifiers = 0;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFieldModifiers)
            (gdata->jvmti, clazz, field, pmodifiers);
    return error;
}

/**
 * Get method modifiers
 */
jvmtiError
methodModifiers(jmethodID method, jint *pmodifiers)
{
    jvmtiError error;

    *pmodifiers = 0;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetMethodModifiers)
            (gdata->jvmti, method, pmodifiers);
    return error;
}

/* Returns a local ref to the declaring class for a method, or NULL. */
jvmtiError
methodClass(jmethodID method, jclass *pclazz)
{
    jvmtiError error;

    *pclazz = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetMethodDeclaringClass)
                                (gdata->jvmti, method, pclazz);
    return error;
}

/* Returns the start and end locations of the specified method. */
jvmtiError
methodLocation(jmethodID method, jlocation *ploc1, jlocation *ploc2)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetMethodLocation)
                                (gdata->jvmti, method, ploc1, ploc2);
    return error;
}

/**
 * Get method signature
 */
jvmtiError
methodSignature(jmethodID method,
        char **pname, char **psignature, char **pgeneric_signature)
{
    jvmtiError error;
    char *name = NULL;
    char *signature = NULL;
    char *generic_signature = NULL;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetMethodName)
            (gdata->jvmti, method, &name, &signature, &generic_signature);

    if ( pname != NULL ) {
        *pname = name;
    } else if ( name != NULL )  {
        jvmtiDeallocate(name);
    }
    if ( psignature != NULL ) {
        *psignature = signature;
    } else if ( signature != NULL ) {
        jvmtiDeallocate(signature);
    }
    if ( pgeneric_signature != NULL ) {
        *pgeneric_signature = generic_signature;
    } else if ( generic_signature != NULL )  {
        jvmtiDeallocate(generic_signature);
    }
    return error;
}

/*
 * Get the return type key of the method
 *     V or B C D F I J S Z L  [
 */
jvmtiError
methodReturnType(jmethodID method, char *typeKey)
{
    char       *signature;
    jvmtiError  error;

    signature = NULL;
    error     = methodSignature(method, NULL, &signature, NULL);
    if (error == JVMTI_ERROR_NONE) {
        if (signature == NULL ) {
            error = AGENT_ERROR_INVALID_TAG;
        } else {
            char * xx;

            xx = strchr(signature, ')');
            if (xx == NULL || *(xx + 1) == 0) {
                error = AGENT_ERROR_INVALID_TAG;
            } else {
               *typeKey = *(xx + 1);
            }
            jvmtiDeallocate(signature);
        }
    }
    return error;
}


/**
 * Return class loader for a class (must be inside a WITH_LOCAL_REFS)
 */
jvmtiError
classLoader(jclass clazz, jobject *pclazz)
{
    jvmtiError error;

    *pclazz = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassLoader)
            (gdata->jvmti, clazz, pclazz);
    return error;
}

/**
 * Get field signature
 */
jvmtiError
fieldSignature(jclass clazz, jfieldID field,
        char **pname, char **psignature, char **pgeneric_signature)
{
    jvmtiError error;
    char *name = NULL;
    char *signature = NULL;
    char *generic_signature = NULL;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFieldName)
            (gdata->jvmti, clazz, field, &name, &signature, &generic_signature);

    if ( pname != NULL ) {
        *pname = name;
    } else if ( name != NULL )  {
        jvmtiDeallocate(name);
    }
    if ( psignature != NULL ) {
        *psignature = signature;
    } else if ( signature != NULL )  {
        jvmtiDeallocate(signature);
    }
    if ( pgeneric_signature != NULL ) {
        *pgeneric_signature = generic_signature;
    } else if ( generic_signature != NULL )  {
        jvmtiDeallocate(generic_signature);
    }
    return error;
}

JNIEnv *
getEnv(void)
{
    JNIEnv *env = NULL;
    jint rc;

    rc = FUNC_PTR(gdata->jvm,GetEnv)
                (gdata->jvm, (void **)&env, JNI_VERSION_1_2);
    if (rc != JNI_OK) {
        ERROR_MESSAGE(("JDWP Unable to get JNI 1.2 environment, jvm->GetEnv() return code = %d",
                rc));
        EXIT_ERROR(AGENT_ERROR_NO_JNI_ENV,NULL);
    }
    return env;
}

jvmtiError
spawnNewThread(jvmtiStartFunction func, void *arg, char *name)
{
    JNIEnv *env = getEnv();
    jvmtiError error;

    LOG_MISC(("Spawning new thread: %s", name));

    WITH_LOCAL_REFS(env, 3) {

        jthread thread;
        jstring nameString;

        nameString = JNI_FUNC_PTR(env,NewStringUTF)(env, name);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
            error = AGENT_ERROR_OUT_OF_MEMORY;
            goto err;
        }

        thread = JNI_FUNC_PTR(env,NewObject)
                        (env, gdata->threadClass, gdata->threadConstructor,
                                   gdata->systemThreadGroup, nameString);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
            error = AGENT_ERROR_OUT_OF_MEMORY;
            goto err;
        }

        /*
         * Make the debugger thread a daemon
         */
        JNI_FUNC_PTR(env,CallVoidMethod)
                        (env, thread, gdata->threadSetDaemon, JNI_TRUE);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
            error = AGENT_ERROR_JNI_EXCEPTION;
            goto err;
        }

        error = threadControl_addDebugThread(thread);
        if (error == JVMTI_ERROR_NONE) {
            /*
             * Debugger threads need cycles in all sorts of strange
             * situations (e.g. infinite cpu-bound loops), so give the
             * thread a high priority. Note that if the VM has an application
             * thread running at the max priority, there is still a chance
             * that debugger threads will be starved. (There needs to be
             * a way to give debugger threads a priority higher than any
             * application thread).
             */
            error = JVMTI_FUNC_PTR(gdata->jvmti,RunAgentThread)
                        (gdata->jvmti, thread, func, arg,
                                        JVMTI_THREAD_MAX_PRIORITY);
        }

        err: ;

    } END_WITH_LOCAL_REFS(env);

    return error;
}

jvmtiError
jvmtiGetCapabilities(jvmtiCapabilities *caps)
{
    if ( gdata->vmDead ) {
        return AGENT_ERROR_VM_DEAD;
    }
    if (!gdata->haveCachedJvmtiCapabilities) {
        jvmtiError error;

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetCapabilities)
                        (gdata->jvmti, &(gdata->cachedJvmtiCapabilities));
        if (error != JVMTI_ERROR_NONE) {
            return error;
        }
        gdata->haveCachedJvmtiCapabilities = JNI_TRUE;
    }

    *caps = gdata->cachedJvmtiCapabilities;

    return JVMTI_ERROR_NONE;
}

static jint
jvmtiVersion(void)
{
    if (gdata->cachedJvmtiVersion == 0) {
        jvmtiError error;
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetVersionNumber)
                        (gdata->jvmti, &(gdata->cachedJvmtiVersion));
        if (error != JVMTI_ERROR_NONE) {
            EXIT_ERROR(error, "on getting the JVMTI version number");
        }
    }
    return gdata->cachedJvmtiVersion;
}

jint
jvmtiMajorVersion(void)
{
    return (jvmtiVersion() & JVMTI_VERSION_MASK_MAJOR)
                    >> JVMTI_VERSION_SHIFT_MAJOR;
}

jint
jvmtiMinorVersion(void)
{
    return (jvmtiVersion() & JVMTI_VERSION_MASK_MINOR)
                    >> JVMTI_VERSION_SHIFT_MINOR;
}

jint
jvmtiMicroVersion(void)
{
    return (jvmtiVersion() & JVMTI_VERSION_MASK_MICRO)
                    >> JVMTI_VERSION_SHIFT_MICRO;
}

jvmtiError
getSourceDebugExtension(jclass clazz, char **extensionPtr)
{
    return JVMTI_FUNC_PTR(gdata->jvmti,GetSourceDebugExtension)
                (gdata->jvmti, clazz, extensionPtr);
}


static void
handleInterrupt(void)
{
    /*
     * An interrupt is handled:
     *
     * 1) for running application threads by deferring the interrupt
     * until the current event handler has concluded.
     *
     * 2) for debugger threads by ignoring the interrupt; this is the
     * most robust solution since debugger threads don't use interrupts
     * to signal any condition.
     *
     * 3) for application threads that have not started or already
     * ended by ignoring the interrupt. In the former case, the application
     * is relying on timing to determine whether or not the thread sees
     * the interrupt; in the latter case, the interrupt is meaningless.
     */
    jthread thread = threadControl_currentThread();
    if ((thread != NULL) && (!threadControl_isDebugThread(thread))) {
        threadControl_setPendingInterrupt(thread);
    }
}

static jvmtiError
ignore_vm_death(jvmtiError error)
{
    if (error == JVMTI_ERROR_WRONG_PHASE) {
        LOG_MISC(("VM_DEAD, in debugMonitor*()?"));
        return JVMTI_ERROR_NONE; /* JVMTI does this, not JVMDI? */
    }
    return error;
}

void
debugMonitorEnter(jrawMonitorID monitor)
{
    jvmtiError error;
    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorEnter)
            (gdata->jvmti, monitor);
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor enter");
    }
}

void
debugMonitorExit(jrawMonitorID monitor)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorExit)
                (gdata->jvmti, monitor);
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor exit");
    }
}

void
debugMonitorWait(jrawMonitorID monitor)
{
    jvmtiError error;
    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorWait)
        (gdata->jvmti, monitor, ((jlong)(-1)));

    /*
     * According to the JLS (17.8), here we have
     * either :
     * a- been notified
     * b- gotten a suprious wakeup
     * c- been interrupted
     * If both a and c have happened, the VM must choose
     * which way to return - a or c.  If it chooses c
     * then the notify is gone - either to some other
     * thread that is also waiting, or it is dropped
     * on the floor.
     *
     * a is what we expect.  b won't hurt us any -
     * callers should be programmed to handle
     * spurious wakeups.  In case of c,
     * then the interrupt has been cleared, but
     * we don't want to consume it.  It came from
     * user code and is intended for user code, not us.
     * So, we will remember that the interrupt has
     * occurred and re-activate it when this thread
     * goes back into user code.
     * That being said, what do we do here?  Since
     * we could have been notified too, here we will
     * just pretend that we have been.  It won't hurt
     * anything to return in the same way as if
     * we were notified since callers have to be able to
     * handle spurious wakeups anyway.
     */
    if (error == JVMTI_ERROR_INTERRUPT) {
        handleInterrupt();
        error = JVMTI_ERROR_NONE;
    }
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor wait");
    }
}

void
debugMonitorTimedWait(jrawMonitorID monitor, jlong millis)
{
    jvmtiError error;
    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorWait)
        (gdata->jvmti, monitor, millis);
    if (error == JVMTI_ERROR_INTERRUPT) {
        /* See comment above */
        handleInterrupt();
        error = JVMTI_ERROR_NONE;
    }
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor timed wait");
    }
}

void
debugMonitorNotify(jrawMonitorID monitor)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorNotify)
                (gdata->jvmti, monitor);
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor notify");
    }
}

void
debugMonitorNotifyAll(jrawMonitorID monitor)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,RawMonitorNotifyAll)
                (gdata->jvmti, monitor);
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on raw monitor notify all");
    }
}

jrawMonitorID
debugMonitorCreate(char *name)
{
    jrawMonitorID monitor;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,CreateRawMonitor)
                (gdata->jvmti, name, &monitor);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on creation of a raw monitor");
    }
    return monitor;
}

void
debugMonitorDestroy(jrawMonitorID monitor)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,DestroyRawMonitor)
                (gdata->jvmti, monitor);
    error = ignore_vm_death(error);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on destruction of raw monitor");
    }
}

/**
 * Return array of all threads (must be inside a WITH_LOCAL_REFS)
 */
jthread *
allThreads(jint *count)
{
    jthread *threads;
    jvmtiError error;

    *count = 0;
    threads = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetAllThreads)
                (gdata->jvmti, count, &threads);
    if (error == AGENT_ERROR_OUT_OF_MEMORY) {
        return NULL; /* Let caller deal with no memory? */
    }
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "getting all threads");
    }
    return threads;
}

/**
 * Fill the passed in structure with thread group info.
 * name field is JVMTI allocated.  parent is global ref.
 */
void
threadGroupInfo(jthreadGroup group, jvmtiThreadGroupInfo *info)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadGroupInfo)
                (gdata->jvmti, group, info);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on getting thread group info");
    }
}

/**
 * Return class signature string
 */
jvmtiError
classSignature(jclass clazz, char **psignature, char **pgeneric_signature)
{
    jvmtiError error;
    char *signature = NULL;

    /*
     * pgeneric_signature can be NULL, and GetClassSignature
     * accepts NULL.
     */
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassSignature)
                (gdata->jvmti, clazz, &signature, pgeneric_signature);

    if ( psignature != NULL ) {
        *psignature = signature;
    } else if ( signature != NULL )  {
        jvmtiDeallocate(signature);
    }
    return error;
}

/* Get class name (not signature) */
char *
getClassname(jclass clazz)
{
    char *classname;

    classname = NULL;
    if ( clazz != NULL ) {
        if (classSignature(clazz, &classname, NULL) != JVMTI_ERROR_NONE) {
            classname = NULL;
        } else {
            /* Convert in place */
            convertSignatureToClassname(classname);
        }
    }
    return classname; /* Caller must free this memory */
}

void
writeGenericSignature(PacketOutputStream *out, char *genericSignature)
{
    if (genericSignature == NULL) {
        (void)outStream_writeString(out, "");
    } else {
        (void)outStream_writeString(out, genericSignature);
    }
}

jint
classStatus(jclass clazz)
{
    jint status;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassStatus)
                (gdata->jvmti, clazz, &status);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on getting class status");
    }
    return status;
}

static jboolean
isArrayClass(jclass clazz)
{
    jboolean isArray = JNI_FALSE;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,IsArrayClass)
                (gdata->jvmti, clazz, &isArray);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on checking for an array class");
    }
    return isArray;
}

static jboolean
isInterface(jclass clazz)
{
    jboolean isInterface = JNI_FALSE;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,IsInterface)
                (gdata->jvmti, clazz, &isInterface);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on checking for an interface");
    }
    return isInterface;
}

jvmtiError
isFieldSynthetic(jclass clazz, jfieldID field, jboolean *psynthetic)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,IsFieldSynthetic)
                (gdata->jvmti, clazz, field, psynthetic);
    if ( error == JVMTI_ERROR_MUST_POSSESS_CAPABILITY ) {
        /* If the query is not supported, we assume it is not synthetic. */
        *psynthetic = JNI_FALSE;
        return JVMTI_ERROR_NONE;
    }
    return error;
}

jvmtiError
isMethodSynthetic(jmethodID method, jboolean *psynthetic)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,IsMethodSynthetic)
                (gdata->jvmti, method, psynthetic);
    if ( error == JVMTI_ERROR_MUST_POSSESS_CAPABILITY ) {
        /* If the query is not supported, we assume it is not synthetic. */
        *psynthetic = JNI_FALSE;
        return JVMTI_ERROR_NONE;
    }
    return error;
}

jboolean
isMethodNative(jmethodID method)
{
    jboolean isNative = JNI_FALSE;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,IsMethodNative)
                (gdata->jvmti, method, &isNative);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "on checking for a native interface");
    }
    return isNative;
}

jboolean
isSameObject(JNIEnv *env, jobject o1, jobject o2)
{
    if ( o1==o2 ) {
        return JNI_TRUE;
    }
    return FUNC_PTR(env,IsSameObject)(env, o1, o2);
}

jint
objectHashCode(jobject object)
{
    jint hashCode = 0;
    jvmtiError error;

    if ( object!=NULL ) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetObjectHashCode)
                    (gdata->jvmti, object, &hashCode);
        if (error != JVMTI_ERROR_NONE) {
            EXIT_ERROR(error, "on getting an object hash code");
        }
    }
    return hashCode;
}

/* Get all implemented interfaces (must be inside a WITH_LOCAL_REFS) */
jvmtiError
allInterfaces(jclass clazz, jclass **ppinterfaces, jint *pcount)
{
    jvmtiError error;

    *pcount = 0;
    *ppinterfaces = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetImplementedInterfaces)
                (gdata->jvmti, clazz, pcount, ppinterfaces);
    return error;
}

/* Get all loaded classes (must be inside a WITH_LOCAL_REFS) */
jvmtiError
allLoadedClasses(jclass **ppclasses, jint *pcount)
{
    jvmtiError error;

    *pcount = 0;
    *ppclasses = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLoadedClasses)
                (gdata->jvmti, pcount, ppclasses);
    return error;
}

/* Get all loaded classes for a loader (must be inside a WITH_LOCAL_REFS) */
jvmtiError
allClassLoaderClasses(jobject loader, jclass **ppclasses, jint *pcount)
{
    jvmtiError error;

    *pcount = 0;
    *ppclasses = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetClassLoaderClasses)
                (gdata->jvmti, loader, pcount, ppclasses);
    return error;
}

static jboolean
is_a_nested_class(char *outer_sig, int outer_sig_len, char *sig, int sep)
{
    char *inner;

    /* Assumed outer class signature is  "LOUTERCLASSNAME;"
     *         inner class signature is  "LOUTERCLASSNAME$INNERNAME;"
     *
     * INNERNAME can take the form:
     *    [0-9][1-9]*        anonymous class somewhere in the file
     *    [0-9][1-9]*NAME    local class somewhere in the OUTER class
     *    NAME               nested class in OUTER
     *
     * If NAME itself contains a $ (sep) then classname is further nested
     *    inside another class.
     *
     */

    /* Check prefix first */
    if ( strncmp(sig, outer_sig, outer_sig_len-1) != 0 ) {
        return JNI_FALSE;
    }

    /* Prefix must be followed by a $ (sep) */
    if ( sig[outer_sig_len-1] != sep ) {
        return JNI_FALSE;  /* No sep follows the match, must not be nested. */
    }

    /* Walk past any digits, if we reach the end, must be pure anonymous */
    inner = sig + outer_sig_len;
#if 1 /* We want to return local classes */
    while ( *inner && isdigit(*inner) ) {
        inner++;
    }
    /* But anonymous class names can't be trusted. */
    if ( *inner == ';' ) {
        return JNI_FALSE;  /* A pure anonymous class */
    }
#else
    if ( *inner && isdigit(*inner) ) {
        return JNI_FALSE;  /* A pure anonymous or local class */
    }
#endif

    /* Nested deeper? */
    if ( strchr(inner, sep) != NULL ) {
        return JNI_FALSE;  /* Nested deeper than we want? */
    }
    return JNI_TRUE;
}

/* Get all nested classes for a class (must be inside a WITH_LOCAL_REFS) */
jvmtiError
allNestedClasses(jclass parent_clazz, jclass **ppnested, jint *pcount)
{
    jvmtiError error;
    jobject parent_loader;
    jclass *classes;
    char *signature;
    size_t len;
    jint count;
    jint ncount;
    int i;

    *ppnested   = NULL;
    *pcount     = 0;

    parent_loader = NULL;
    classes       = NULL;
    signature     = NULL;
    count         = 0;
    ncount        = 0;

    error = classLoader(parent_clazz, &parent_loader);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    error = classSignature(parent_clazz, &signature, NULL);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }
    len = strlen(signature);

    error = allClassLoaderClasses(parent_loader, &classes, &count);
    if ( error != JVMTI_ERROR_NONE ) {
        jvmtiDeallocate(signature);
        return error;
    }

    for (i=0; i<count; i++) {
        jclass clazz;
        char *candidate_signature;

        clazz = classes[i];
        candidate_signature = NULL;
        error = classSignature(clazz, &candidate_signature, NULL);
        if (error != JVMTI_ERROR_NONE) {
            break;
        }

        if ( is_a_nested_class(signature, (int)len, candidate_signature, '$') ||
             is_a_nested_class(signature, (int)len, candidate_signature, '#') ) {
            /* Float nested classes to top */
            classes[i] = classes[ncount];
            classes[ncount++] = clazz;
        }
        jvmtiDeallocate(candidate_signature);
    }

    jvmtiDeallocate(signature);

    if ( count != 0 &&  ncount == 0 ) {
        jvmtiDeallocate(classes);
        classes = NULL;
    }

    *ppnested = classes;
    *pcount = ncount;
    return error;
}

void
createLocalRefSpace(JNIEnv *env, jint capacity)
{
    /*
     * Save current exception since it might get overwritten by
     * the calls below. Note we must depend on space in the existing
     * frame because asking for a new frame may generate an exception.
     */
    jobject throwable = JNI_FUNC_PTR(env,ExceptionOccurred)(env);

    /*
     * Use the current frame if necessary; otherwise create a new one
     */
    if (JNI_FUNC_PTR(env,PushLocalFrame)(env, capacity) < 0) {
        EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"PushLocalFrame: Unable to push JNI frame");
    }

    /*
     * TO DO: This could be more efficient if it used EnsureLocalCapacity,
     * but that would not work if two functions on the call stack
     * use this function. We would need to either track reserved
     * references on a per-thread basis or come up with a convention
     * that would prevent two functions from depending on this function
     * at the same time.
     */

    /*
     * Restore exception state from before call
     */
    if (throwable != NULL) {
        JNI_FUNC_PTR(env,Throw)(env, throwable);
    } else {
        JNI_FUNC_PTR(env,ExceptionClear)(env);
    }
}

jboolean
isClass(jobject object)
{
    JNIEnv *env = getEnv();
    return JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->classClass);
}

jboolean
isThread(jobject object)
{
    JNIEnv *env = getEnv();
    return JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->threadClass);
}

jboolean
isThreadGroup(jobject object)
{
    JNIEnv *env = getEnv();
    return JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->threadGroupClass);
}

jboolean
isString(jobject object)
{
    JNIEnv *env = getEnv();
    return JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->stringClass);
}

jboolean
isClassLoader(jobject object)
{
    JNIEnv *env = getEnv();
    return JNI_FUNC_PTR(env,IsInstanceOf)(env, object, gdata->classLoaderClass);
}

jboolean
isArray(jobject object)
{
    JNIEnv *env = getEnv();
    jboolean is;

    WITH_LOCAL_REFS(env, 1) {
        jclass clazz;
        clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, object);
        is = isArrayClass(clazz);
    } END_WITH_LOCAL_REFS(env);

    return is;
}

/**
 * Return property value as jstring
 */
static jstring
getPropertyValue(JNIEnv *env, char *propertyName)
{
    jstring valueString;
    jstring nameString;

    valueString = NULL;

    /* Create new String object to hold the property name */
    nameString = JNI_FUNC_PTR(env,NewStringUTF)(env, propertyName);
    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        JNI_FUNC_PTR(env,ExceptionClear)(env);
        /* NULL will be returned below */
    } else {
        /* Call valueString = System.getProperty(nameString) */
        valueString = JNI_FUNC_PTR(env,CallStaticObjectMethod)
            (env, gdata->systemClass, gdata->systemGetProperty, nameString);
        if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
            valueString = NULL;
        }
    }
    return valueString;
}

/**
 * Set an agent property
 */
void
setAgentPropertyValue(JNIEnv *env, char *propertyName, char* propertyValue)
{
    jstring nameString;
    jstring valueString;

    if (gdata->agent_properties == NULL) {
        /* VMSupport doesn't exist; so ignore */
        return;
    }

    /* Create jstrings for property name and value */
    nameString = JNI_FUNC_PTR(env,NewStringUTF)(env, propertyName);
    if (nameString != NULL) {
        /* convert the value to UTF8 */
        int len;
        char *utf8value;
        int utf8maxSize;

        len = (int)strlen(propertyValue);
        utf8maxSize = len * 4 + 1;
        utf8value = (char *)jvmtiAllocate(utf8maxSize);
        if (utf8value != NULL) {
            utf8FromPlatform(propertyValue, len, (jbyte *)utf8value, utf8maxSize);
            valueString = JNI_FUNC_PTR(env, NewStringUTF)(env, utf8value);
            jvmtiDeallocate(utf8value);

            if (valueString != NULL) {
                /* invoke Properties.setProperty */
                JNI_FUNC_PTR(env,CallObjectMethod)
                    (env, gdata->agent_properties,
                     gdata->setProperty,
                     nameString, valueString);
            }
        }
    }
    if (JNI_FUNC_PTR(env,ExceptionOccurred)(env)) {
        JNI_FUNC_PTR(env,ExceptionClear)(env);
    }
}

/**
 * Return property value as JDWP allocated string in UTF8 encoding
 */
static char *
getPropertyUTF8(JNIEnv *env, char *propertyName)
{
    jvmtiError  error;
    char       *value;

    value = NULL;
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetSystemProperty)
                (gdata->jvmti, (const char *)propertyName, &value);
    if (error != JVMTI_ERROR_NONE) {
        jstring valueString;

        value = NULL;
        valueString = getPropertyValue(env, propertyName);

        if (valueString != NULL) {
            const char *utf;

            /* Get the UTF8 encoding for this property value string */
            utf = JNI_FUNC_PTR(env,GetStringUTFChars)(env, valueString, NULL);
            /* Make a copy for returning, release the JNI copy */
            value = jvmtiAllocate((int)strlen(utf) + 1);
            if (value != NULL) {
                (void)strcpy(value, utf);
            }
            JNI_FUNC_PTR(env,ReleaseStringUTFChars)(env, valueString, utf);
        }
    }
    if ( value == NULL ) {
        ERROR_MESSAGE(("JDWP Can't get property value for %s", propertyName));
        EXIT_ERROR(AGENT_ERROR_NULL_POINTER,NULL);
    }
    return value;
}

jboolean
isMethodObsolete(jmethodID method)
{
    jvmtiError error;
    jboolean obsolete = JNI_TRUE;

    if ( method != NULL ) {
        error = JVMTI_FUNC_PTR(gdata->jvmti,IsMethodObsolete)
                    (gdata->jvmti, method, &obsolete);
        if (error != JVMTI_ERROR_NONE) {
            obsolete = JNI_TRUE;
        }
    }
    return obsolete;
}

/* Get the jvmti environment to be used with tags */
jvmtiEnv *
getSpecialJvmti(void)
{
    jvmtiEnv  *jvmti;
    jvmtiError error;
    int        rc;

    /* Get one time use JVMTI Env */
    jvmtiCapabilities caps;

    rc = JVM_FUNC_PTR(gdata->jvm,GetEnv)
                     (gdata->jvm, (void **)&jvmti, JVMTI_VERSION_1);
    if (rc != JNI_OK) {
        return NULL;
    }
    (void)memset(&caps, 0, (int)sizeof(caps));
    caps.can_tag_objects = 1;
    error = JVMTI_FUNC_PTR(jvmti,AddCapabilities)(jvmti, &caps);
    if ( error != JVMTI_ERROR_NONE ) {
        return NULL;
    }
    return jvmti;
}

void
writeCodeLocation(PacketOutputStream *out, jclass clazz,
                       jmethodID method, jlocation location)
{
    jbyte tag;

    if (clazz != NULL) {
        tag = referenceTypeTag(clazz);
    } else {
        tag = JDWP_TYPE_TAG(CLASS);
    }
    (void)outStream_writeByte(out, tag);
    (void)outStream_writeObjectRef(getEnv(), out, clazz);
    (void)outStream_writeMethodID(out, isMethodObsolete(method)?NULL:method);
    (void)outStream_writeLocation(out, location);
}

void *
jvmtiAllocate(jint numBytes)
{
    void *ptr;
    jvmtiError error;
    if ( numBytes == 0 ) {
        return NULL;
    }
    error = JVMTI_FUNC_PTR(gdata->jvmti,Allocate)
                (gdata->jvmti, numBytes, (unsigned char**)&ptr);
    if (error != JVMTI_ERROR_NONE ) {
        EXIT_ERROR(error, "Can't allocate jvmti memory");
    }
    return ptr;
}

void
jvmtiDeallocate(void *ptr)
{
    jvmtiError error;
    if ( ptr == NULL ) {
        return;
    }
    error = JVMTI_FUNC_PTR(gdata->jvmti,Deallocate)
                (gdata->jvmti, ptr);
    if (error != JVMTI_ERROR_NONE ) {
        EXIT_ERROR(error, "Can't deallocate jvmti memory");
    }
}

/* Rarely needed, transport library uses JDWP errors, only use? */
jvmtiError
map2jvmtiError(jdwpError error)
{
    switch ( error ) {
        case JDWP_ERROR(NONE):
            return JVMTI_ERROR_NONE;
        case JDWP_ERROR(INVALID_THREAD):
            return JVMTI_ERROR_INVALID_THREAD;
        case JDWP_ERROR(INVALID_THREAD_GROUP):
            return JVMTI_ERROR_INVALID_THREAD_GROUP;
        case JDWP_ERROR(INVALID_PRIORITY):
            return JVMTI_ERROR_INVALID_PRIORITY;
        case JDWP_ERROR(THREAD_NOT_SUSPENDED):
            return JVMTI_ERROR_THREAD_NOT_SUSPENDED;
        case JDWP_ERROR(THREAD_SUSPENDED):
            return JVMTI_ERROR_THREAD_SUSPENDED;
        case JDWP_ERROR(INVALID_OBJECT):
            return JVMTI_ERROR_INVALID_OBJECT;
        case JDWP_ERROR(INVALID_CLASS):
            return JVMTI_ERROR_INVALID_CLASS;
        case JDWP_ERROR(CLASS_NOT_PREPARED):
            return JVMTI_ERROR_CLASS_NOT_PREPARED;
        case JDWP_ERROR(INVALID_METHODID):
            return JVMTI_ERROR_INVALID_METHODID;
        case JDWP_ERROR(INVALID_LOCATION):
            return JVMTI_ERROR_INVALID_LOCATION;
        case JDWP_ERROR(INVALID_FIELDID):
            return JVMTI_ERROR_INVALID_FIELDID;
        case JDWP_ERROR(INVALID_FRAMEID):
            return AGENT_ERROR_INVALID_FRAMEID;
        case JDWP_ERROR(NO_MORE_FRAMES):
            return JVMTI_ERROR_NO_MORE_FRAMES;
        case JDWP_ERROR(OPAQUE_FRAME):
            return JVMTI_ERROR_OPAQUE_FRAME;
        case JDWP_ERROR(NOT_CURRENT_FRAME):
            return AGENT_ERROR_NOT_CURRENT_FRAME;
        case JDWP_ERROR(TYPE_MISMATCH):
            return JVMTI_ERROR_TYPE_MISMATCH;
        case JDWP_ERROR(INVALID_SLOT):
            return JVMTI_ERROR_INVALID_SLOT;
        case JDWP_ERROR(DUPLICATE):
            return JVMTI_ERROR_DUPLICATE;
        case JDWP_ERROR(NOT_FOUND):
            return JVMTI_ERROR_NOT_FOUND;
        case JDWP_ERROR(INVALID_MONITOR):
            return JVMTI_ERROR_INVALID_MONITOR;
        case JDWP_ERROR(NOT_MONITOR_OWNER):
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        case JDWP_ERROR(INTERRUPT):
            return JVMTI_ERROR_INTERRUPT;
        case JDWP_ERROR(INVALID_CLASS_FORMAT):
            return JVMTI_ERROR_INVALID_CLASS_FORMAT;
        case JDWP_ERROR(CIRCULAR_CLASS_DEFINITION):
            return JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION;
        case JDWP_ERROR(FAILS_VERIFICATION):
            return JVMTI_ERROR_FAILS_VERIFICATION;
        case JDWP_ERROR(ADD_METHOD_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED;
        case JDWP_ERROR(SCHEMA_CHANGE_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED;
        case JDWP_ERROR(INVALID_TYPESTATE):
            return JVMTI_ERROR_INVALID_TYPESTATE;
        case JDWP_ERROR(HIERARCHY_CHANGE_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED;
        case JDWP_ERROR(DELETE_METHOD_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED;
        case JDWP_ERROR(UNSUPPORTED_VERSION):
            return JVMTI_ERROR_UNSUPPORTED_VERSION;
        case JDWP_ERROR(NAMES_DONT_MATCH):
            return JVMTI_ERROR_NAMES_DONT_MATCH;
        case JDWP_ERROR(CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED;
        case JDWP_ERROR(METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED;
        case JDWP_ERROR(CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED):
            return JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED;
        case JDWP_ERROR(NOT_IMPLEMENTED):
            return JVMTI_ERROR_NOT_AVAILABLE;
        case JDWP_ERROR(NULL_POINTER):
            return JVMTI_ERROR_NULL_POINTER;
        case JDWP_ERROR(ABSENT_INFORMATION):
            return JVMTI_ERROR_ABSENT_INFORMATION;
        case JDWP_ERROR(INVALID_EVENT_TYPE):
            return JVMTI_ERROR_INVALID_EVENT_TYPE;
        case JDWP_ERROR(ILLEGAL_ARGUMENT):
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        case JDWP_ERROR(OUT_OF_MEMORY):
            return JVMTI_ERROR_OUT_OF_MEMORY;
        case JDWP_ERROR(ACCESS_DENIED):
            return JVMTI_ERROR_ACCESS_DENIED;
        case JDWP_ERROR(VM_DEAD):
            return JVMTI_ERROR_WRONG_PHASE;
        case JDWP_ERROR(UNATTACHED_THREAD):
            return JVMTI_ERROR_UNATTACHED_THREAD;
        case JDWP_ERROR(INVALID_TAG):
            return AGENT_ERROR_INVALID_TAG;
        case JDWP_ERROR(ALREADY_INVOKING):
            return AGENT_ERROR_ALREADY_INVOKING;
        case JDWP_ERROR(INVALID_INDEX):
            return AGENT_ERROR_INVALID_INDEX;
        case JDWP_ERROR(INVALID_LENGTH):
            return AGENT_ERROR_INVALID_LENGTH;
        case JDWP_ERROR(INVALID_STRING):
            return AGENT_ERROR_INVALID_STRING;
        case JDWP_ERROR(INVALID_CLASS_LOADER):
            return AGENT_ERROR_INVALID_CLASS_LOADER;
        case JDWP_ERROR(INVALID_ARRAY):
            return AGENT_ERROR_INVALID_ARRAY;
        case JDWP_ERROR(TRANSPORT_LOAD):
            return AGENT_ERROR_TRANSPORT_LOAD;
        case JDWP_ERROR(TRANSPORT_INIT):
            return AGENT_ERROR_TRANSPORT_INIT;
        case JDWP_ERROR(NATIVE_METHOD):
            return AGENT_ERROR_NATIVE_METHOD;
        case JDWP_ERROR(INVALID_COUNT):
            return AGENT_ERROR_INVALID_COUNT;
        case JDWP_ERROR(INTERNAL):
            return AGENT_ERROR_JDWP_INTERNAL;
    }
    return AGENT_ERROR_INTERNAL;
}

static jvmtiEvent index2jvmti[EI_max-EI_min+1];
static jdwpEvent  index2jdwp [EI_max-EI_min+1];

void
eventIndexInit(void)
{
    (void)memset(index2jvmti, 0, (int)sizeof(index2jvmti));
    (void)memset(index2jdwp,  0, (int)sizeof(index2jdwp));

    index2jvmti[EI_SINGLE_STEP        -EI_min] = JVMTI_EVENT_SINGLE_STEP;
    index2jvmti[EI_BREAKPOINT         -EI_min] = JVMTI_EVENT_BREAKPOINT;
    index2jvmti[EI_FRAME_POP          -EI_min] = JVMTI_EVENT_FRAME_POP;
    index2jvmti[EI_EXCEPTION          -EI_min] = JVMTI_EVENT_EXCEPTION;
    index2jvmti[EI_THREAD_START       -EI_min] = JVMTI_EVENT_THREAD_START;
    index2jvmti[EI_THREAD_END         -EI_min] = JVMTI_EVENT_THREAD_END;
    index2jvmti[EI_CLASS_PREPARE      -EI_min] = JVMTI_EVENT_CLASS_PREPARE;
    index2jvmti[EI_GC_FINISH          -EI_min] = JVMTI_EVENT_GARBAGE_COLLECTION_FINISH;
    index2jvmti[EI_CLASS_LOAD         -EI_min] = JVMTI_EVENT_CLASS_LOAD;
    index2jvmti[EI_FIELD_ACCESS       -EI_min] = JVMTI_EVENT_FIELD_ACCESS;
    index2jvmti[EI_FIELD_MODIFICATION -EI_min] = JVMTI_EVENT_FIELD_MODIFICATION;
    index2jvmti[EI_EXCEPTION_CATCH    -EI_min] = JVMTI_EVENT_EXCEPTION_CATCH;
    index2jvmti[EI_METHOD_ENTRY       -EI_min] = JVMTI_EVENT_METHOD_ENTRY;
    index2jvmti[EI_METHOD_EXIT        -EI_min] = JVMTI_EVENT_METHOD_EXIT;
    index2jvmti[EI_MONITOR_CONTENDED_ENTER      -EI_min] = JVMTI_EVENT_MONITOR_CONTENDED_ENTER;
    index2jvmti[EI_MONITOR_CONTENDED_ENTERED    -EI_min] = JVMTI_EVENT_MONITOR_CONTENDED_ENTERED;
    index2jvmti[EI_MONITOR_WAIT       -EI_min] = JVMTI_EVENT_MONITOR_WAIT;
    index2jvmti[EI_MONITOR_WAITED     -EI_min] = JVMTI_EVENT_MONITOR_WAITED;
    index2jvmti[EI_VM_INIT            -EI_min] = JVMTI_EVENT_VM_INIT;
    index2jvmti[EI_VM_DEATH           -EI_min] = JVMTI_EVENT_VM_DEATH;

    index2jdwp[EI_SINGLE_STEP         -EI_min] = JDWP_EVENT(SINGLE_STEP);
    index2jdwp[EI_BREAKPOINT          -EI_min] = JDWP_EVENT(BREAKPOINT);
    index2jdwp[EI_FRAME_POP           -EI_min] = JDWP_EVENT(FRAME_POP);
    index2jdwp[EI_EXCEPTION           -EI_min] = JDWP_EVENT(EXCEPTION);
    index2jdwp[EI_THREAD_START        -EI_min] = JDWP_EVENT(THREAD_START);
    index2jdwp[EI_THREAD_END          -EI_min] = JDWP_EVENT(THREAD_END);
    index2jdwp[EI_CLASS_PREPARE       -EI_min] = JDWP_EVENT(CLASS_PREPARE);
    index2jdwp[EI_GC_FINISH           -EI_min] = JDWP_EVENT(CLASS_UNLOAD);
    index2jdwp[EI_CLASS_LOAD          -EI_min] = JDWP_EVENT(CLASS_LOAD);
    index2jdwp[EI_FIELD_ACCESS        -EI_min] = JDWP_EVENT(FIELD_ACCESS);
    index2jdwp[EI_FIELD_MODIFICATION  -EI_min] = JDWP_EVENT(FIELD_MODIFICATION);
    index2jdwp[EI_EXCEPTION_CATCH     -EI_min] = JDWP_EVENT(EXCEPTION_CATCH);
    index2jdwp[EI_METHOD_ENTRY        -EI_min] = JDWP_EVENT(METHOD_ENTRY);
    index2jdwp[EI_METHOD_EXIT         -EI_min] = JDWP_EVENT(METHOD_EXIT);
    index2jdwp[EI_MONITOR_CONTENDED_ENTER             -EI_min] = JDWP_EVENT(MONITOR_CONTENDED_ENTER);
    index2jdwp[EI_MONITOR_CONTENDED_ENTERED           -EI_min] = JDWP_EVENT(MONITOR_CONTENDED_ENTERED);
    index2jdwp[EI_MONITOR_WAIT        -EI_min] = JDWP_EVENT(MONITOR_WAIT);
    index2jdwp[EI_MONITOR_WAITED      -EI_min] = JDWP_EVENT(MONITOR_WAITED);
    index2jdwp[EI_VM_INIT             -EI_min] = JDWP_EVENT(VM_INIT);
    index2jdwp[EI_VM_DEATH            -EI_min] = JDWP_EVENT(VM_DEATH);
}

jdwpEvent
eventIndex2jdwp(EventIndex i)
{
    if ( i < EI_min || i > EI_max ) {
        EXIT_ERROR(AGENT_ERROR_INVALID_INDEX,"bad EventIndex");
    }
    return index2jdwp[i-EI_min];
}

jvmtiEvent
eventIndex2jvmti(EventIndex i)
{
    if ( i < EI_min || i > EI_max ) {
        EXIT_ERROR(AGENT_ERROR_INVALID_INDEX,"bad EventIndex");
    }
    return index2jvmti[i-EI_min];
}

#ifdef DEBUG

char*
eventIndex2EventName(EventIndex ei)
{
    switch ( ei ) {
        case EI_SINGLE_STEP:
            return "EI_SINGLE_STEP";
        case EI_BREAKPOINT:
            return "EI_BREAKPOINT";
        case EI_FRAME_POP:
            return "EI_FRAME_POP";
        case EI_EXCEPTION:
            return "EI_EXCEPTION";
        case EI_THREAD_START:
            return "EI_THREAD_START";
        case EI_THREAD_END:
            return "EI_THREAD_END";
        case EI_CLASS_PREPARE:
            return "EI_CLASS_PREPARE";
        case EI_GC_FINISH:
            return "EI_GC_FINISH";
        case EI_CLASS_LOAD:
            return "EI_CLASS_LOAD";
        case EI_FIELD_ACCESS:
            return "EI_FIELD_ACCESS";
        case EI_FIELD_MODIFICATION:
            return "EI_FIELD_MODIFICATION";
        case EI_EXCEPTION_CATCH:
            return "EI_EXCEPTION_CATCH";
        case EI_METHOD_ENTRY:
            return "EI_METHOD_ENTRY";
        case EI_METHOD_EXIT:
            return "EI_METHOD_EXIT";
        case EI_MONITOR_CONTENDED_ENTER:
            return "EI_MONITOR_CONTENDED_ENTER";
        case EI_MONITOR_CONTENDED_ENTERED:
            return "EI_MONITOR_CONTENDED_ENTERED";
        case EI_MONITOR_WAIT:
            return "EI_MONITOR_WAIT";
        case EI_MONITOR_WAITED:
            return "EI_MONITOR_WAITED";
        case EI_VM_INIT:
            return "EI_VM_INIT";
        case EI_VM_DEATH:
            return "EI_VM_DEATH";
        default:
            JDI_ASSERT(JNI_FALSE);
            return "Bad EI";
    }
}

#endif

EventIndex
jdwp2EventIndex(jdwpEvent eventType)
{
    switch ( eventType ) {
        case JDWP_EVENT(SINGLE_STEP):
            return EI_SINGLE_STEP;
        case JDWP_EVENT(BREAKPOINT):
            return EI_BREAKPOINT;
        case JDWP_EVENT(FRAME_POP):
            return EI_FRAME_POP;
        case JDWP_EVENT(EXCEPTION):
            return EI_EXCEPTION;
        case JDWP_EVENT(THREAD_START):
            return EI_THREAD_START;
        case JDWP_EVENT(THREAD_END):
            return EI_THREAD_END;
        case JDWP_EVENT(CLASS_PREPARE):
            return EI_CLASS_PREPARE;
        case JDWP_EVENT(CLASS_UNLOAD):
            return EI_GC_FINISH;
        case JDWP_EVENT(CLASS_LOAD):
            return EI_CLASS_LOAD;
        case JDWP_EVENT(FIELD_ACCESS):
            return EI_FIELD_ACCESS;
        case JDWP_EVENT(FIELD_MODIFICATION):
            return EI_FIELD_MODIFICATION;
        case JDWP_EVENT(EXCEPTION_CATCH):
            return EI_EXCEPTION_CATCH;
        case JDWP_EVENT(METHOD_ENTRY):
            return EI_METHOD_ENTRY;
        case JDWP_EVENT(METHOD_EXIT):
            return EI_METHOD_EXIT;
        case JDWP_EVENT(METHOD_EXIT_WITH_RETURN_VALUE):
            return EI_METHOD_EXIT;
        case JDWP_EVENT(MONITOR_CONTENDED_ENTER):
            return EI_MONITOR_CONTENDED_ENTER;
        case JDWP_EVENT(MONITOR_CONTENDED_ENTERED):
            return EI_MONITOR_CONTENDED_ENTERED;
        case JDWP_EVENT(MONITOR_WAIT):
            return EI_MONITOR_WAIT;
        case JDWP_EVENT(MONITOR_WAITED):
            return EI_MONITOR_WAITED;
        case JDWP_EVENT(VM_INIT):
            return EI_VM_INIT;
        case JDWP_EVENT(VM_DEATH):
            return EI_VM_DEATH;
        default:
            break;
    }

    /*
     * Event type not recognized - don't exit with error as caller
     * may wish to return error to debugger.
     */
    return (EventIndex)0;
}

EventIndex
jvmti2EventIndex(jvmtiEvent kind)
{
    switch ( kind ) {
        case JVMTI_EVENT_SINGLE_STEP:
            return EI_SINGLE_STEP;
        case JVMTI_EVENT_BREAKPOINT:
            return EI_BREAKPOINT;
        case JVMTI_EVENT_FRAME_POP:
            return EI_FRAME_POP;
        case JVMTI_EVENT_EXCEPTION:
            return EI_EXCEPTION;
        case JVMTI_EVENT_THREAD_START:
            return EI_THREAD_START;
        case JVMTI_EVENT_THREAD_END:
            return EI_THREAD_END;
        case JVMTI_EVENT_CLASS_PREPARE:
            return EI_CLASS_PREPARE;
        case JVMTI_EVENT_GARBAGE_COLLECTION_FINISH:
            return EI_GC_FINISH;
        case JVMTI_EVENT_CLASS_LOAD:
            return EI_CLASS_LOAD;
        case JVMTI_EVENT_FIELD_ACCESS:
            return EI_FIELD_ACCESS;
        case JVMTI_EVENT_FIELD_MODIFICATION:
            return EI_FIELD_MODIFICATION;
        case JVMTI_EVENT_EXCEPTION_CATCH:
            return EI_EXCEPTION_CATCH;
        case JVMTI_EVENT_METHOD_ENTRY:
            return EI_METHOD_ENTRY;
        case JVMTI_EVENT_METHOD_EXIT:
            return EI_METHOD_EXIT;
        /*
         * There is no JVMTI_EVENT_METHOD_EXIT_WITH_RETURN_VALUE.
         * The normal JVMTI_EVENT_METHOD_EXIT always contains the return value.
         */
        case JVMTI_EVENT_MONITOR_CONTENDED_ENTER:
            return EI_MONITOR_CONTENDED_ENTER;
        case JVMTI_EVENT_MONITOR_CONTENDED_ENTERED:
            return EI_MONITOR_CONTENDED_ENTERED;
        case JVMTI_EVENT_MONITOR_WAIT:
            return EI_MONITOR_WAIT;
        case JVMTI_EVENT_MONITOR_WAITED:
            return EI_MONITOR_WAITED;
        case JVMTI_EVENT_VM_INIT:
            return EI_VM_INIT;
        case JVMTI_EVENT_VM_DEATH:
            return EI_VM_DEATH;
        default:
            EXIT_ERROR(AGENT_ERROR_INVALID_INDEX,"JVMTI to EventIndex mapping");
            break;
    }
    return (EventIndex)0;
}

/* This routine is commonly used, maps jvmti and agent errors to the best
 *    jdwp error code we can map to.
 */
jdwpError
map2jdwpError(jvmtiError error)
{
    switch ( (int)error ) {
        case JVMTI_ERROR_NONE:
            return JDWP_ERROR(NONE);
        case AGENT_ERROR_INVALID_THREAD:
        case JVMTI_ERROR_INVALID_THREAD:
            return JDWP_ERROR(INVALID_THREAD);
        case JVMTI_ERROR_INVALID_THREAD_GROUP:
            return JDWP_ERROR(INVALID_THREAD_GROUP);
        case JVMTI_ERROR_INVALID_PRIORITY:
            return JDWP_ERROR(INVALID_PRIORITY);
        case JVMTI_ERROR_THREAD_NOT_SUSPENDED:
            return JDWP_ERROR(THREAD_NOT_SUSPENDED);
        case JVMTI_ERROR_THREAD_SUSPENDED:
            return JDWP_ERROR(THREAD_SUSPENDED);
        case JVMTI_ERROR_THREAD_NOT_ALIVE:
            return JDWP_ERROR(INVALID_THREAD);
        case AGENT_ERROR_INVALID_OBJECT:
        case JVMTI_ERROR_INVALID_OBJECT:
            return JDWP_ERROR(INVALID_OBJECT);
        case JVMTI_ERROR_INVALID_CLASS:
            return JDWP_ERROR(INVALID_CLASS);
        case JVMTI_ERROR_CLASS_NOT_PREPARED:
            return JDWP_ERROR(CLASS_NOT_PREPARED);
        case JVMTI_ERROR_INVALID_METHODID:
            return JDWP_ERROR(INVALID_METHODID);
        case JVMTI_ERROR_INVALID_LOCATION:
            return JDWP_ERROR(INVALID_LOCATION);
        case JVMTI_ERROR_INVALID_FIELDID:
            return JDWP_ERROR(INVALID_FIELDID);
        case AGENT_ERROR_NO_MORE_FRAMES:
        case JVMTI_ERROR_NO_MORE_FRAMES:
            return JDWP_ERROR(NO_MORE_FRAMES);
        case JVMTI_ERROR_OPAQUE_FRAME:
            return JDWP_ERROR(OPAQUE_FRAME);
        case JVMTI_ERROR_TYPE_MISMATCH:
            return JDWP_ERROR(TYPE_MISMATCH);
        case JVMTI_ERROR_INVALID_SLOT:
            return JDWP_ERROR(INVALID_SLOT);
        case JVMTI_ERROR_DUPLICATE:
            return JDWP_ERROR(DUPLICATE);
        case JVMTI_ERROR_NOT_FOUND:
            return JDWP_ERROR(NOT_FOUND);
        case JVMTI_ERROR_INVALID_MONITOR:
            return JDWP_ERROR(INVALID_MONITOR);
        case JVMTI_ERROR_NOT_MONITOR_OWNER:
            return JDWP_ERROR(NOT_MONITOR_OWNER);
        case JVMTI_ERROR_INTERRUPT:
            return JDWP_ERROR(INTERRUPT);
        case JVMTI_ERROR_INVALID_CLASS_FORMAT:
            return JDWP_ERROR(INVALID_CLASS_FORMAT);
        case JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION:
            return JDWP_ERROR(CIRCULAR_CLASS_DEFINITION);
        case JVMTI_ERROR_FAILS_VERIFICATION:
            return JDWP_ERROR(FAILS_VERIFICATION);
        case JVMTI_ERROR_INVALID_TYPESTATE:
            return JDWP_ERROR(INVALID_TYPESTATE);
        case JVMTI_ERROR_UNSUPPORTED_VERSION:
            return JDWP_ERROR(UNSUPPORTED_VERSION);
        case JVMTI_ERROR_NAMES_DONT_MATCH:
            return JDWP_ERROR(NAMES_DONT_MATCH);
        case AGENT_ERROR_NULL_POINTER:
        case JVMTI_ERROR_NULL_POINTER:
            return JDWP_ERROR(NULL_POINTER);
        case JVMTI_ERROR_ABSENT_INFORMATION:
            return JDWP_ERROR(ABSENT_INFORMATION);
        case AGENT_ERROR_INVALID_EVENT_TYPE:
        case JVMTI_ERROR_INVALID_EVENT_TYPE:
            return JDWP_ERROR(INVALID_EVENT_TYPE);
        case AGENT_ERROR_ILLEGAL_ARGUMENT:
        case JVMTI_ERROR_ILLEGAL_ARGUMENT:
            return JDWP_ERROR(ILLEGAL_ARGUMENT);
        case JVMTI_ERROR_OUT_OF_MEMORY:
        case AGENT_ERROR_OUT_OF_MEMORY:
            return JDWP_ERROR(OUT_OF_MEMORY);
        case JVMTI_ERROR_ACCESS_DENIED:
            return JDWP_ERROR(ACCESS_DENIED);
        case JVMTI_ERROR_WRONG_PHASE:
        case AGENT_ERROR_VM_DEAD:
        case AGENT_ERROR_NO_JNI_ENV:
            return JDWP_ERROR(VM_DEAD);
        case AGENT_ERROR_JNI_EXCEPTION:
        case JVMTI_ERROR_UNATTACHED_THREAD:
            return JDWP_ERROR(UNATTACHED_THREAD);
        case JVMTI_ERROR_NOT_AVAILABLE:
        case JVMTI_ERROR_MUST_POSSESS_CAPABILITY:
            return JDWP_ERROR(NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED:
            return JDWP_ERROR(HIERARCHY_CHANGE_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED:
            return JDWP_ERROR(DELETE_METHOD_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED:
            return JDWP_ERROR(ADD_METHOD_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED:
            return JDWP_ERROR(SCHEMA_CHANGE_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED:
            return JDWP_ERROR(CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED:
            return JDWP_ERROR(METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED);
        case JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_ATTRIBUTE_CHANGED:
            return JDWP_ERROR(CLASS_ATTRIBUTE_CHANGE_NOT_IMPLEMENTED);
        case AGENT_ERROR_NOT_CURRENT_FRAME:
            return JDWP_ERROR(NOT_CURRENT_FRAME);
        case AGENT_ERROR_INVALID_TAG:
            return JDWP_ERROR(INVALID_TAG);
        case AGENT_ERROR_ALREADY_INVOKING:
            return JDWP_ERROR(ALREADY_INVOKING);
        case AGENT_ERROR_INVALID_INDEX:
            return JDWP_ERROR(INVALID_INDEX);
        case AGENT_ERROR_INVALID_LENGTH:
            return JDWP_ERROR(INVALID_LENGTH);
        case AGENT_ERROR_INVALID_STRING:
            return JDWP_ERROR(INVALID_STRING);
        case AGENT_ERROR_INVALID_CLASS_LOADER:
            return JDWP_ERROR(INVALID_CLASS_LOADER);
        case AGENT_ERROR_INVALID_ARRAY:
            return JDWP_ERROR(INVALID_ARRAY);
        case AGENT_ERROR_TRANSPORT_LOAD:
            return JDWP_ERROR(TRANSPORT_LOAD);
        case AGENT_ERROR_TRANSPORT_INIT:
            return JDWP_ERROR(TRANSPORT_INIT);
        case AGENT_ERROR_NATIVE_METHOD:
            return JDWP_ERROR(NATIVE_METHOD);
        case AGENT_ERROR_INVALID_COUNT:
            return JDWP_ERROR(INVALID_COUNT);
        case AGENT_ERROR_INVALID_FRAMEID:
            return JDWP_ERROR(INVALID_FRAMEID);
        case JVMTI_ERROR_INTERNAL:
        case JVMTI_ERROR_INVALID_ENVIRONMENT:
        case AGENT_ERROR_INTERNAL:
        case AGENT_ERROR_JVMTI_INTERNAL:
        case AGENT_ERROR_JDWP_INTERNAL:
            return JDWP_ERROR(INTERNAL);
        default:
            break;
    }
    return JDWP_ERROR(INTERNAL);
}

jint
map2jdwpSuspendStatus(jint state)
{
    jint status = 0;
    if ( ( state & JVMTI_THREAD_STATE_SUSPENDED ) != 0 )  {
        status = JDWP_SUSPEND_STATUS(SUSPENDED);
    }
    return status;
}

jdwpThreadStatus
map2jdwpThreadStatus(jint state)
{
    jdwpThreadStatus status;

    status = (jdwpThreadStatus)(-1);

    if ( ! ( state & JVMTI_THREAD_STATE_ALIVE ) ) {
        if ( state & JVMTI_THREAD_STATE_TERMINATED ) {
            status = JDWP_THREAD_STATUS(ZOMBIE);
        } else {
            /* FIXUP? New JDWP #define for not started? */
            status = (jdwpThreadStatus)(-1);
        }
    } else {
        if ( state & JVMTI_THREAD_STATE_SLEEPING ) {
            status = JDWP_THREAD_STATUS(SLEEPING);
        } else if ( state & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER ) {
            status = JDWP_THREAD_STATUS(MONITOR);
        } else if ( state & JVMTI_THREAD_STATE_WAITING ) {
            status = JDWP_THREAD_STATUS(WAIT);
        } else if ( state & JVMTI_THREAD_STATE_RUNNABLE ) {
            status = JDWP_THREAD_STATUS(RUNNING);
        }
    }
    return status;
}

jint
map2jdwpClassStatus(jint classStatus)
{
    jint status = 0;
    if ( ( classStatus & JVMTI_CLASS_STATUS_VERIFIED ) != 0 ) {
        status |= JDWP_CLASS_STATUS(VERIFIED);
    }
    if ( ( classStatus & JVMTI_CLASS_STATUS_PREPARED ) != 0 ) {
        status |= JDWP_CLASS_STATUS(PREPARED);
    }
    if ( ( classStatus & JVMTI_CLASS_STATUS_INITIALIZED ) != 0 ) {
        status |= JDWP_CLASS_STATUS(INITIALIZED);
    }
    if ( ( classStatus & JVMTI_CLASS_STATUS_ERROR ) != 0 ) {
        status |= JDWP_CLASS_STATUS(ERROR);
    }
    return status;
}

void
log_debugee_location(const char *func,
        jthread thread, jmethodID method, jlocation location)
{
    int logging_locations = LOG_TEST(JDWP_LOG_LOC);

    if ( logging_locations ) {
        char *method_name;
        char *class_sig;
        jvmtiError error;
        jvmtiThreadInfo info;
        jint state;

        /* Get thread information */
        info.name = NULL;
        error = FUNC_PTR(gdata->jvmti,GetThreadInfo)
                                (gdata->jvmti, thread, &info);
        if ( error != JVMTI_ERROR_NONE) {
            info.name = NULL;
        }
        error = FUNC_PTR(gdata->jvmti,GetThreadState)
                                (gdata->jvmti, thread, &state);
        if ( error != JVMTI_ERROR_NONE) {
            state = 0;
        }

        /* Get method if necessary */
        if ( method==NULL ) {
            error = FUNC_PTR(gdata->jvmti,GetFrameLocation)
                        (gdata->jvmti, thread, 0, &method, &location);
            if ( error != JVMTI_ERROR_NONE ) {
                method = NULL;
                location = 0;
            }
        }

        /* Get method name */
        method_name = NULL;
        if ( method != NULL ) {
            error = methodSignature(method, &method_name, NULL, NULL);
            if ( error != JVMTI_ERROR_NONE ) {
                method_name = NULL;
            }
        }

        /* Get class signature */
        class_sig = NULL;
        if ( method != NULL ) {
            jclass clazz;

            error = methodClass(method, &clazz);
            if ( error == JVMTI_ERROR_NONE ) {
                error = classSignature(clazz, &class_sig, NULL);
                if ( error != JVMTI_ERROR_NONE ) {
                    class_sig = NULL;
                }
            }
        }

        /* Issue log message */
        LOG_LOC(("%s: debugee: thread=%p(%s:0x%x),method=%p(%s@%d;%s)",
                func,
                thread, info.name==NULL ? "?" : info.name, state,
                method, method_name==NULL ? "?" : method_name,
                (int)location, class_sig==NULL ? "?" : class_sig));

        /* Free memory */
        if ( class_sig != NULL ) {
            jvmtiDeallocate(class_sig);
        }
        if ( method_name != NULL ) {
            jvmtiDeallocate(method_name);
        }
        if ( info.name != NULL ) {
            jvmtiDeallocate(info.name);
        }
    }
}

/* ********************************************************************* */
/* JDK 6.0: Use of new Heap Iteration functions */
/* ********************************************************************* */

/* ********************************************************************* */
/* Instances */

/* Structure to hold class instances heap iteration data (arg user_data) */
typedef struct ClassInstancesData {
    jint         instCount;
    jint         maxInstances;
    jlong        objTag;
    jvmtiError   error;
} ClassInstancesData;

/* Callback for instance object tagging (heap_reference_callback). */
static jint JNICALL
cbObjectTagInstance(jvmtiHeapReferenceKind reference_kind,
     const jvmtiHeapReferenceInfo* reference_info, jlong class_tag,
     jlong referrer_class_tag, jlong size,
     jlong* tag_ptr, jlong* referrer_tag_ptr, jint length, void* user_data)
{
    ClassInstancesData  *data;

    /* Check data structure */
    data = (ClassInstancesData*)user_data;
    if (data == NULL) {
        return JVMTI_VISIT_ABORT;
    }

    /* If we have tagged enough objects, just abort */
    if ( data->maxInstances != 0 && data->instCount >= data->maxInstances ) {
        return JVMTI_VISIT_ABORT;
    }

    /* If tagged already, just continue */
    if ( (*tag_ptr) != (jlong)0 ) {
        return JVMTI_VISIT_OBJECTS;
    }

    /* Tag the object so we don't count it again, and so we can retrieve it */
    (*tag_ptr) = data->objTag;
    data->instCount++;
    return JVMTI_VISIT_OBJECTS;
}

/* Get instances for one class */
jvmtiError
classInstances(jclass klass, ObjectBatch *instances, int maxInstances)
{
    ClassInstancesData data;
    jvmtiHeapCallbacks heap_callbacks;
    jvmtiError         error;
    jvmtiEnv          *jvmti;

    /* Check interface assumptions */

    if (klass == NULL) {
        return AGENT_ERROR_INVALID_OBJECT;
    }

    if ( maxInstances < 0 || instances == NULL) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Initialize return information */
    instances->count   = 0;
    instances->objects = NULL;

    /* Get jvmti environment to use */
    jvmti = getSpecialJvmti();
    if ( jvmti == NULL ) {
        return AGENT_ERROR_INTERNAL;
    }

    /* Setup data to passed around the callbacks */
    data.instCount    = 0;
    data.maxInstances = maxInstances;
    data.objTag       = (jlong)1;
    data.error        = JVMTI_ERROR_NONE;

    /* Clear out callbacks structure */
    (void)memset(&heap_callbacks,0,sizeof(heap_callbacks));

    /* Set the callbacks we want */
    heap_callbacks.heap_reference_callback = &cbObjectTagInstance;

    /* Follow references, no initiating object, just this class, all objects */
    error = JVMTI_FUNC_PTR(jvmti,FollowReferences)
                 (jvmti, 0, klass, NULL, &heap_callbacks, &data);
    if ( error == JVMTI_ERROR_NONE ) {
        error = data.error;
    }

    /* Get all the instances now that they are tagged */
    if ( error == JVMTI_ERROR_NONE ) {
        error = JVMTI_FUNC_PTR(jvmti,GetObjectsWithTags)
                      (jvmti, 1, &(data.objTag), &(instances->count),
                       &(instances->objects), NULL);
        /* Verify we got the count we expected */
        if ( data.instCount != instances->count ) {
            error = AGENT_ERROR_INTERNAL;
        }
    }

    /* Dispose of any special jvmti environment */
    (void)JVMTI_FUNC_PTR(jvmti,DisposeEnvironment)(jvmti);
    return error;
}

/* ********************************************************************* */
/* Instance counts. */

/* Macros to convert a class or instance tag to an index and back again */
#define INDEX2CLASSTAG(i)      ((jlong)((i)+1))
#define CLASSTAG2INDEX(t)      (((int)(t))-1)
#define JLONG_ABS(x)           (((x)<(jlong)0)?-(x):(x))

/* Structure to hold class count heap traversal data (arg user_data) */
typedef struct ClassCountData {
    int          classCount;
    jlong       *counts;
    jlong        negObjTag;
    jvmtiError   error;
} ClassCountData;

/* Two different cbObjectCounter's, one for FollowReferences, one for
 *    IterateThroughHeap. Pick a card, any card.
 */

/* Callback for object count heap traversal (heap_reference_callback) */
static jint JNICALL
cbObjectCounterFromRef(jvmtiHeapReferenceKind reference_kind,
     const jvmtiHeapReferenceInfo* reference_info, jlong class_tag,
     jlong referrer_class_tag, jlong size,
     jlong* tag_ptr, jlong* referrer_tag_ptr, jint length, void* user_data)
{
    ClassCountData  *data;
    int              index;
    jlong            jindex;
    jlong            tag;

    /* Check data structure */
    data = (ClassCountData*)user_data;
    if (data == NULL) {
        return JVMTI_VISIT_ABORT;
    }

    /* Classes with no class_tag should have been filtered out. */
    if ( class_tag == (jlong)0 ) {
        data->error = AGENT_ERROR_INTERNAL;
        return JVMTI_VISIT_ABORT;
    }

    /* Class tag not one we really want (jclass not in supplied list) */
    if ( class_tag == data->negObjTag ) {
        return JVMTI_VISIT_OBJECTS;
    }

    /* If object tag is negative, just continue, we counted it */
    tag = (*tag_ptr);
    if ( tag < (jlong)0 ) {
        return JVMTI_VISIT_OBJECTS;
    }

    /* Tag the object with a negative value just so we don't count it again */
    if ( tag == (jlong)0 ) {
        /* This object had no tag value, so we give it the negObjTag value */
        (*tag_ptr) = data->negObjTag;
    } else {
        /* If this object had a positive tag value, it must be one of the
         *    jclass objects we tagged. We need to preserve the value of
         *    this tag for later objects that might have this as a class
         *    tag, so we just make the existing tag value negative.
         */
        (*tag_ptr) = -tag;
    }

    /* Absolute value of class tag is an index into the counts[] array */
    jindex = JLONG_ABS(class_tag);
    index = CLASSTAG2INDEX(jindex);
    if (index < 0 || index >= data->classCount) {
        data->error = AGENT_ERROR_ILLEGAL_ARGUMENT;
        return JVMTI_VISIT_ABORT;
    }

    /* Bump instance count on this class */
    data->counts[index]++;
    return JVMTI_VISIT_OBJECTS;
}

/* Callback for instance count heap traversal (heap_iteration_callback) */
static jint JNICALL
cbObjectCounter(jlong class_tag, jlong size, jlong* tag_ptr, jint length,
                        void* user_data)
{
    ClassCountData  *data;
    int              index;

    /* Check data structure */
    data = (ClassCountData*)user_data;
    if (data == NULL) {
        return JVMTI_VISIT_ABORT;
    }

    /* Classes with no tag should be filtered out. */
    if ( class_tag == (jlong)0 ) {
        data->error = AGENT_ERROR_INTERNAL;
        return JVMTI_VISIT_ABORT;
    }

    /* Class tag is actually an index into data arrays */
    index = CLASSTAG2INDEX(class_tag);
    if (index < 0 || index >= data->classCount) {
        data->error = AGENT_ERROR_ILLEGAL_ARGUMENT;
        return JVMTI_VISIT_ABORT;
    }

    /* Bump instance count on this class */
    data->counts[index]++;
    return JVMTI_VISIT_OBJECTS;
}

/* Get instance counts for a set of classes */
jvmtiError
classInstanceCounts(jint classCount, jclass *classes, jlong *counts)
{
    jvmtiHeapCallbacks heap_callbacks;
    ClassCountData     data;
    jvmtiError         error;
    jvmtiEnv          *jvmti;
    int                i;

    /* Check interface assumptions */
    if ( classes == NULL || classCount <= 0 || counts == NULL ) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Initialize return information */
    for ( i = 0 ; i < classCount ; i++ ) {
        counts[i] = (jlong)0;
    }

    /* Get jvmti environment to use */
    jvmti = getSpecialJvmti();
    if ( jvmti == NULL ) {
        return AGENT_ERROR_INTERNAL;
    }

    /* Setup class data structure */
    data.error        = JVMTI_ERROR_NONE;
    data.classCount   = classCount;
    data.counts       = counts;

    error = JVMTI_ERROR_NONE;
    /* Set tags on classes, use index in classes[] as the tag value. */
    error             = JVMTI_ERROR_NONE;
    for ( i = 0 ; i < classCount ; i++ ) {
        if (classes[i] != NULL) {
            jlong tag;

            tag = INDEX2CLASSTAG(i);
            error = JVMTI_FUNC_PTR(jvmti,SetTag) (jvmti, classes[i], tag);
            if ( error != JVMTI_ERROR_NONE ) {
                break;
            }
        }
    }

    /* Traverse heap, two ways to do this for instance counts. */
    if ( error == JVMTI_ERROR_NONE ) {

        /* Clear out callbacks structure */
        (void)memset(&heap_callbacks,0,sizeof(heap_callbacks));

        /* Check debug flags to see how to do this. */
        if ( (gdata->debugflags & USE_ITERATE_THROUGH_HEAP) == 0 ) {

            /* Using FollowReferences only gives us live objects, but we
             *   need to tag the objects to avoid counting them twice since
             *   the callback is per reference.
             *   The jclass objects have been tagged with their index in the
             *   supplied list, and that tag may flip to negative if it
             *   is also an object of interest.
             *   All other objects being counted that weren't in the
             *   supplied classes list will have a negative classCount
             *   tag value. So all objects counted will have negative tags.
             *   If the absolute tag value is an index in the supplied
             *   list, then it's one of the supplied classes.
             */
            data.negObjTag = -INDEX2CLASSTAG(classCount);

            /* Setup callbacks, only using object reference callback */
            heap_callbacks.heap_reference_callback = &cbObjectCounterFromRef;

            /* Follow references, no initiating object, tagged classes only */
            error = JVMTI_FUNC_PTR(jvmti,FollowReferences)
                          (jvmti, JVMTI_HEAP_FILTER_CLASS_UNTAGGED,
                           NULL, NULL, &heap_callbacks, &data);

        } else {

            /* Using IterateThroughHeap means that we will visit each object
             *   once, so no special tag tricks here. Just simple counting.
             *   However in this case the object might not be live, so we do
             *   a GC beforehand to make sure we minimize this.
             */

            /* FIXUP: Need some kind of trigger here to avoid excessive GC's? */
            error = JVMTI_FUNC_PTR(jvmti,ForceGarbageCollection)(jvmti);
            if ( error != JVMTI_ERROR_NONE ) {

                /* Setup callbacks, just need object callback */
                heap_callbacks.heap_iteration_callback = &cbObjectCounter;

                /* Iterate through entire heap, tagged classes only */
                error = JVMTI_FUNC_PTR(jvmti,IterateThroughHeap)
                              (jvmti, JVMTI_HEAP_FILTER_CLASS_UNTAGGED,
                               NULL, &heap_callbacks, &data);

            }
        }

        /* Use data error if needed */
        if ( error == JVMTI_ERROR_NONE ) {
            error = data.error;
        }

    }

    /* Dispose of any special jvmti environment */
    (void)JVMTI_FUNC_PTR(jvmti,DisposeEnvironment)(jvmti);
    return error;
}

/* ********************************************************************* */
/* Referrers */

/* Structure to hold object referrer heap traversal data (arg user_data) */
typedef struct ReferrerData {
  int        refCount;
  int        maxObjects;
  jlong      refTag;
  jlong      objTag;
  jboolean   selfRef;
  jvmtiError error;
} ReferrerData;

/* Callback for referrers object tagging (heap_reference_callback). */
static jint JNICALL
cbObjectTagReferrer(jvmtiHeapReferenceKind reference_kind,
     const jvmtiHeapReferenceInfo* reference_info, jlong class_tag,
     jlong referrer_class_tag, jlong size,
     jlong* tag_ptr, jlong* referrer_tag_ptr, jint length, void* user_data)
{
    ReferrerData  *data;

    /* Check data structure */
    data = (ReferrerData*)user_data;
    if (data == NULL) {
        return JVMTI_VISIT_ABORT;
    }

    /* If we have tagged enough objects, just abort */
    if ( data->maxObjects != 0 && data->refCount >= data->maxObjects ) {
        return JVMTI_VISIT_ABORT;
    }

    /* If not of interest, just continue */
    if ( (*tag_ptr) != data->objTag ) {
        return JVMTI_VISIT_OBJECTS;
    }

    /* Self reference that we haven't counted? */
    if ( tag_ptr == referrer_tag_ptr ) {
        if ( data->selfRef == JNI_FALSE ) {
            data->selfRef = JNI_TRUE;
            data->refCount++;
        }
        return JVMTI_VISIT_OBJECTS;
    }

    /* If the referrer can be tagged, and hasn't been tagged, tag it */
    if ( referrer_tag_ptr != NULL ) {
        if ( (*referrer_tag_ptr) == (jlong)0 ) {
            *referrer_tag_ptr = data->refTag;
            data->refCount++;
        }
    }
    return JVMTI_VISIT_OBJECTS;
}

/* Heap traversal to find referrers of an object */
jvmtiError
objectReferrers(jobject obj, ObjectBatch *referrers, int maxObjects)
{
    jvmtiHeapCallbacks heap_callbacks;
    ReferrerData       data;
    jvmtiError         error;
    jvmtiEnv          *jvmti;

    /* Check interface assumptions */
    if (obj == NULL) {
        return AGENT_ERROR_INVALID_OBJECT;
    }
    if (referrers == NULL || maxObjects < 0 ) {
        return AGENT_ERROR_ILLEGAL_ARGUMENT;
    }

    /* Initialize return information */
    referrers->count = 0;
    referrers->objects = NULL;

    /* Get jvmti environment to use */
    jvmti = getSpecialJvmti();
    if ( jvmti == NULL ) {
        return AGENT_ERROR_INTERNAL;
    }

    /* Fill in the data structure passed around the callbacks */
    data.refCount   = 0;
    data.maxObjects = maxObjects;
    data.objTag     = (jlong)1;
    data.refTag     = (jlong)2;
    data.selfRef    = JNI_FALSE;
    data.error      = JVMTI_ERROR_NONE;

    /* Tag the object of interest */
    error = JVMTI_FUNC_PTR(jvmti,SetTag) (jvmti, obj, data.objTag);

    /* No need to go any further if we can't tag the object */
    if ( error == JVMTI_ERROR_NONE ) {

        /* Clear out callbacks structure */
        (void)memset(&heap_callbacks,0,sizeof(heap_callbacks));

        /* Setup callbacks we want */
        heap_callbacks.heap_reference_callback = &cbObjectTagReferrer;

        /* Follow references, no initiating object, all classes, 1 tagged objs */
        error = JVMTI_FUNC_PTR(jvmti,FollowReferences)
                      (jvmti, JVMTI_HEAP_FILTER_UNTAGGED,
                       NULL, NULL, &heap_callbacks, &data);

        /* Use data error if needed */
        if ( error == JVMTI_ERROR_NONE ) {
            error = data.error;
        }

    }

    /* Watch out for self-reference */
    if ( error == JVMTI_ERROR_NONE && data.selfRef == JNI_TRUE ) {
        /* Tag itself as a referer */
        error = JVMTI_FUNC_PTR(jvmti,SetTag) (jvmti, obj, data.refTag);
    }

    /* Get the jobjects for the tagged referrer objects.  */
    if ( error == JVMTI_ERROR_NONE ) {
        error = JVMTI_FUNC_PTR(jvmti,GetObjectsWithTags)
                    (jvmti, 1, &(data.refTag), &(referrers->count),
                          &(referrers->objects), NULL);
        /* Verify we got the count we expected */
        if ( data.refCount != referrers->count ) {
            error = AGENT_ERROR_INTERNAL;
        }
    }

    /* Dispose of any special jvmti environment */
    (void)JVMTI_FUNC_PTR(jvmti,DisposeEnvironment)(jvmti);
    return error;
}
