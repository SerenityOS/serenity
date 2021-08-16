/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "invoker.h"
#include "eventHandler.h"
#include "threadControl.h"
#include "outStream.h"
#include "signature.h"

static jrawMonitorID invokerLock;

void
invoker_initialize(void)
{
    invokerLock = debugMonitorCreate("JDWP Invocation Lock");
}

void
invoker_reset(void)
{
}

void invoker_lock(void)
{
    debugMonitorEnter(invokerLock);
}

void invoker_unlock(void)
{
    debugMonitorExit(invokerLock);
}

/*
 * Note: argument refs may be destroyed on out-of-memory error
 */
static jvmtiError
createGlobalRefs(JNIEnv *env, InvokeRequest *request)
{
    jvmtiError error = JVMTI_ERROR_NONE;
    void *cursor = NULL;
    jbyte argumentTag = 0;
    jint argIndex = 0;
    jvalue *argument = request->arguments;
    jclass clazz = NULL;
    jobject instance = NULL;
    jobject *argRefs = NULL;

    if ( request->argumentCount > 0 ) {
        /*LINTED*/
        argRefs = jvmtiAllocate((jint)(request->argumentCount*sizeof(jobject)));
        if ( argRefs==NULL ) {
            error = AGENT_ERROR_OUT_OF_MEMORY;
        } else {
            /*LINTED*/
            (void)memset(argRefs, 0, request->argumentCount*sizeof(jobject));
        }
    }

    if ( error == JVMTI_ERROR_NONE ) {
        saveGlobalRef(env, request->clazz, &clazz);
        if (clazz == NULL) {
            error = AGENT_ERROR_OUT_OF_MEMORY;
        }
    }

    if ( error == JVMTI_ERROR_NONE && request->instance != NULL ) {
        saveGlobalRef(env, request->instance, &instance);
        if (instance == NULL) {
            error = AGENT_ERROR_OUT_OF_MEMORY;
        }
    }

    if ( error == JVMTI_ERROR_NONE && argRefs!=NULL ) {
        methodSignature_init(request->methodSignature, &cursor);
        while (methodSignature_nextArgumentExists(&cursor, &argumentTag)) {
            if ( argIndex > request->argumentCount ) {
                break;
            }
            if (isReferenceTag(argumentTag)) {
                /* Create a global ref for any non-null argument */
                if (argument->l != NULL) {
                    saveGlobalRef(env, argument->l, &argRefs[argIndex]);
                    if (argRefs[argIndex] == NULL) {
                        error = AGENT_ERROR_OUT_OF_MEMORY;
                        break;
                    }
                }
            }
            argument++;
            argIndex++;
        }
    }

#ifdef FIXUP /* Why isn't this an error? */
    /* Make sure the argument count matches */
    if ( error == JVMTI_ERROR_NONE && argIndex != request->argumentCount ) {
        error = AGENT_ERROR_INVALID_COUNT;
    }
#endif

    /* Finally, put the global refs into the request if no errors */
    if ( error == JVMTI_ERROR_NONE ) {
        request->clazz = clazz;
        request->instance = instance;
        if ( argRefs!=NULL ) {
            argIndex = 0;
            methodSignature_init(request->methodSignature, &cursor);
            argument = request->arguments;
            while ( methodSignature_nextArgumentExists(&cursor, &argumentTag) &&
                   argIndex < request->argumentCount ) {
                if ( isReferenceTag(argumentTag) ) {
                    argument->l = argRefs[argIndex];
                }
                argument++;
                argIndex++;
            }
            jvmtiDeallocate(argRefs);
        }
        return JVMTI_ERROR_NONE;

    } else {
        /* Delete global references */
        if ( clazz != NULL ) {
            tossGlobalRef(env, &clazz);
        }
        if ( instance != NULL ) {
            tossGlobalRef(env, &instance);
        }
        if ( argRefs!=NULL ) {
            for ( argIndex=0; argIndex < request->argumentCount; argIndex++ ) {
                if ( argRefs[argIndex] != NULL ) {
                    tossGlobalRef(env, &argRefs[argIndex]);
                }
            }
            jvmtiDeallocate(argRefs);
        }
    }

    return error;
}

/*
 * Delete global argument references from the request which got put there before a
 * invoke request was carried out. See fillInvokeRequest().
 */
static void
deleteGlobalArgumentRefs(JNIEnv *env, InvokeRequest *request)
{
    void *cursor = NULL;
    jint argIndex = 0;
    jbyte argumentTag = 0;
    jvalue *argument = request->arguments;
    methodSignature_init(request->methodSignature, &cursor);

    if (request->clazz != NULL) {
        tossGlobalRef(env, &(request->clazz));
    }
    if (request->instance != NULL) {
        tossGlobalRef(env, &(request->instance));
    }
    /* Delete global argument references */
    while (methodSignature_nextArgumentExists(&cursor, &argumentTag) &&
           argIndex < request->argumentCount) {
        if (isReferenceTag(argumentTag)) {
            if (argument->l != NULL) {
                tossGlobalRef(env, &(argument->l));
            }
        }
        argument++;
        argIndex++;
    }
}

static jvmtiError
fillInvokeRequest(JNIEnv *env, InvokeRequest *request,
                  jbyte invokeType, jbyte options, jint id,
                  jthread thread, jclass clazz, jmethodID method,
                  jobject instance,
                  jvalue *arguments, jint argumentCount)
{
    jvmtiError error;
    if (!request->available) {
        /*
         * Thread is not at a point where it can invoke.
         */
        return AGENT_ERROR_INVALID_THREAD;
    }
    if (request->pending) {
        /*
         * Pending invoke
         */
        return AGENT_ERROR_ALREADY_INVOKING;
    }

    request->invokeType = invokeType;
    request->options = options;
    request->detached = JNI_FALSE;
    request->id = id;
    request->clazz = clazz;
    request->method = method;
    request->instance = instance;
    request->arguments = arguments;
    request->arguments = arguments;
    request->argumentCount = argumentCount;

    request->returnValue.j = 0;
    request->exception = 0;

    /*
     * Squirrel away the method signature
     */
    error = methodSignature(method, NULL, &request->methodSignature,  NULL);
    if (error != JVMTI_ERROR_NONE) {
        return error;
    }

    /*
     * The given references for class and instance are not guaranteed
     * to be around long enough for invocation, so create new ones
     * here.
     */
    error = createGlobalRefs(env, request);
    if (error != JVMTI_ERROR_NONE) {
        jvmtiDeallocate(request->methodSignature);
        return error;
    }

    request->pending = JNI_TRUE;
    request->available = JNI_FALSE;
    return JVMTI_ERROR_NONE;
}

void
invoker_enableInvokeRequests(jthread thread)
{
    InvokeRequest *request;

    JDI_ASSERT(thread);

    debugMonitorEnter(invokerLock);
    request = threadControl_getInvokeRequest(thread);
    if (request == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting thread invoke request");
    }

    request->available = JNI_TRUE;
    debugMonitorExit(invokerLock);
}

/*
 * Check that method is in the specified clazz or one of its super classes.
 * We have to enforce this check at the JDWP layer because the JNI layer
 * has different requirements.
 */
static jvmtiError check_methodClass(JNIEnv *env, jclass clazz, jmethodID method)
{
    jclass containing_class = NULL;
    jvmtiError error;

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetMethodDeclaringClass)
                (gdata->jvmti, method, &containing_class);
    if (error != JVMTI_ERROR_NONE) {
        return JVMTI_ERROR_NONE;  /* Bad jmethodID ?  This will be handled elsewhere */
    }

    if (JNI_FUNC_PTR(env,IsSameObject)(env, clazz, containing_class)) {
        return JVMTI_ERROR_NONE;
    }

    // If not the same class then check that containing_class is a superclass of
    // clazz (not a superinterface).
    if (JNI_FUNC_PTR(env,IsAssignableFrom)(env, clazz, containing_class) &&
        referenceTypeTag(containing_class) != JDWP_TYPE_TAG(INTERFACE)) {
        return JVMTI_ERROR_NONE;
    }
    return JVMTI_ERROR_INVALID_METHODID;
}

jvmtiError
invoker_requestInvoke(jbyte invokeType, jbyte options, jint id,
                      jthread thread, jclass clazz, jmethodID method,
                      jobject instance,
                      jvalue *arguments, jint argumentCount)
{
    JNIEnv *env = getEnv();
    InvokeRequest *request;
    jvmtiError error = JVMTI_ERROR_NONE;

    if (invokeType == INVOKE_STATIC) {
        error = check_methodClass(env, clazz, method);
        if (error != JVMTI_ERROR_NONE) {
            return error;
        }
    }

    debugMonitorEnter(invokerLock);
    request = threadControl_getInvokeRequest(thread);
    if (request != NULL) {
        error = fillInvokeRequest(env, request, invokeType, options, id,
                                  thread, clazz, method, instance,
                                  arguments, argumentCount);
    }
    debugMonitorExit(invokerLock);

    if (error == JVMTI_ERROR_NONE) {
        if (options & JDWP_INVOKE_OPTIONS(SINGLE_THREADED) ) {
            /* true means it is okay to unblock the commandLoop thread */
            (void)threadControl_resumeThread(thread, JNI_TRUE);
        } else {
            (void)threadControl_resumeAll();
        }
    }

    return error;
}

static void
invokeConstructor(JNIEnv *env, InvokeRequest *request)
{
    jobject object;

    JDI_ASSERT_MSG(request->clazz, "Request clazz null");
    object = JNI_FUNC_PTR(env,NewObjectA)(env, request->clazz,
                                     request->method,
                                     request->arguments);
    request->returnValue.l = NULL;
    if (object != NULL) {
        saveGlobalRef(env, object, &(request->returnValue.l));
    }
}

static void
invokeStatic(JNIEnv *env, InvokeRequest *request)
{
    jbyte returnType = methodSignature_returnTag(request->methodSignature);

    if (isReferenceTag(returnType)) {
        jobject object;
        JDI_ASSERT_MSG(request->clazz, "Request clazz null");
        object = JNI_FUNC_PTR(env,CallStaticObjectMethodA)(env,
                                   request->clazz,
                                   request->method,
                                   request->arguments);
        request->returnValue.l = NULL;
        if (object != NULL) {
            saveGlobalRef(env, object, &(request->returnValue.l));
        }
        return;
    }

    switch (returnType) {
        case JDWP_TAG(BYTE):
            request->returnValue.b = JNI_FUNC_PTR(env,CallStaticByteMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(CHAR):
            request->returnValue.c = JNI_FUNC_PTR(env,CallStaticCharMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(FLOAT):
            request->returnValue.f = JNI_FUNC_PTR(env,CallStaticFloatMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(DOUBLE):
            request->returnValue.d = JNI_FUNC_PTR(env,CallStaticDoubleMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(INT):
            request->returnValue.i = JNI_FUNC_PTR(env,CallStaticIntMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(LONG):
            request->returnValue.j = JNI_FUNC_PTR(env,CallStaticLongMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(SHORT):
            request->returnValue.s = JNI_FUNC_PTR(env,CallStaticShortMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(BOOLEAN):
            request->returnValue.z = JNI_FUNC_PTR(env,CallStaticBooleanMethodA)(env,
                                                       request->clazz,
                                                       request->method,
                                                       request->arguments);
            break;

        case JDWP_TAG(VOID):
            JNI_FUNC_PTR(env,CallStaticVoidMethodA)(env,
                                          request->clazz,
                                          request->method,
                                          request->arguments);
            break;

        default:
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"Invalid method signature");
            break;
    }
}

static void
invokeVirtual(JNIEnv *env, InvokeRequest *request)
{
    jbyte returnType = methodSignature_returnTag(request->methodSignature);
    if (isReferenceTag(returnType)) {
        jobject object;
        JDI_ASSERT_MSG(request->instance, "Request instance null");
        object = JNI_FUNC_PTR(env,CallObjectMethodA)(env,
                             request->instance,
                             request->method,
                             request->arguments);
        request->returnValue.l = NULL;
        if (object != NULL) {
            saveGlobalRef(env, object, &(request->returnValue.l));
        }
        return;
    }

    switch (returnType) {
        case JDWP_TAG(BYTE):
            request->returnValue.b = JNI_FUNC_PTR(env,CallByteMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(CHAR):
            request->returnValue.c = JNI_FUNC_PTR(env,CallCharMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(FLOAT):
            request->returnValue.f = JNI_FUNC_PTR(env,CallFloatMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(DOUBLE):
            request->returnValue.d = JNI_FUNC_PTR(env,CallDoubleMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(INT):
            request->returnValue.i = JNI_FUNC_PTR(env,CallIntMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(LONG):
            request->returnValue.j = JNI_FUNC_PTR(env,CallLongMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(SHORT):
            request->returnValue.s = JNI_FUNC_PTR(env,CallShortMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(BOOLEAN):
            request->returnValue.z = JNI_FUNC_PTR(env,CallBooleanMethodA)(env,
                                                 request->instance,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(VOID):
            JNI_FUNC_PTR(env,CallVoidMethodA)(env,
                                    request->instance,
                                    request->method,
                                    request->arguments);
            break;

        default:
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"Invalid method signature");
            break;
    }
}

static void
invokeNonvirtual(JNIEnv *env, InvokeRequest *request)
{
    jbyte returnType = methodSignature_returnTag(request->methodSignature);
    if (isReferenceTag(returnType)) {
        jobject object;
        JDI_ASSERT_MSG(request->clazz, "Request clazz null");
        JDI_ASSERT_MSG(request->instance, "Request instance null");
        object = JNI_FUNC_PTR(env,CallNonvirtualObjectMethodA)(env,
                                       request->instance,
                                       request->clazz,
                                       request->method,
                                       request->arguments);
        request->returnValue.l = NULL;
        if (object != NULL) {
            saveGlobalRef(env, object, &(request->returnValue.l));
        }
        return;
    }

    switch (returnType) {
        case JDWP_TAG(BYTE):
            request->returnValue.b = JNI_FUNC_PTR(env,CallNonvirtualByteMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(CHAR):
            request->returnValue.c = JNI_FUNC_PTR(env,CallNonvirtualCharMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(FLOAT):
            request->returnValue.f = JNI_FUNC_PTR(env,CallNonvirtualFloatMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(DOUBLE):
            request->returnValue.d = JNI_FUNC_PTR(env,CallNonvirtualDoubleMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(INT):
            request->returnValue.i = JNI_FUNC_PTR(env,CallNonvirtualIntMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(LONG):
            request->returnValue.j = JNI_FUNC_PTR(env,CallNonvirtualLongMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(SHORT):
            request->returnValue.s = JNI_FUNC_PTR(env,CallNonvirtualShortMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(BOOLEAN):
            request->returnValue.z = JNI_FUNC_PTR(env,CallNonvirtualBooleanMethodA)(env,
                                                 request->instance,
                                                 request->clazz,
                                                 request->method,
                                                 request->arguments);
            break;

        case JDWP_TAG(VOID):
            JNI_FUNC_PTR(env,CallNonvirtualVoidMethodA)(env,
                                    request->instance,
                                    request->clazz,
                                    request->method,
                                    request->arguments);
            break;

        default:
            EXIT_ERROR(AGENT_ERROR_NULL_POINTER,"Invalid method signature");
            break;
    }
}

jboolean
invoker_doInvoke(jthread thread)
{
    JNIEnv *env;
    jboolean startNow;
    InvokeRequest *request;
    jbyte options;
    jbyte invokeType;

    JDI_ASSERT(thread);

    debugMonitorEnter(invokerLock);

    request = threadControl_getInvokeRequest(thread);
    if (request == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting thread invoke request");
    }

    request->available = JNI_FALSE;
    startNow = request->pending && !request->started;

    if (startNow) {
        request->started = JNI_TRUE;
    }
    options = request->options;
    invokeType = request->invokeType;

    debugMonitorExit(invokerLock);

    if (!startNow) {
        return JNI_FALSE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 2) {  /* 1 for obj return values, 1 for exception */

        jobject exception;

        JNI_FUNC_PTR(env,ExceptionClear)(env);

        switch (invokeType) {
            case INVOKE_CONSTRUCTOR:
                invokeConstructor(env, request);
                break;
            case INVOKE_STATIC:
                invokeStatic(env, request);
                break;
            case INVOKE_INSTANCE:
                if (options & JDWP_INVOKE_OPTIONS(NONVIRTUAL) ) {
                    invokeNonvirtual(env, request);
                } else {
                    invokeVirtual(env, request);
                }
                break;
            default:
                JDI_ASSERT(JNI_FALSE);
        }
        request->exception = NULL;
        exception = JNI_FUNC_PTR(env,ExceptionOccurred)(env);
        if (exception != NULL) {
            JNI_FUNC_PTR(env,ExceptionClear)(env);
            saveGlobalRef(env, exception, &(request->exception));
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

void
invoker_completeInvokeRequest(jthread thread)
{
    JNIEnv *env = getEnv();
    PacketOutputStream out;
    jbyte tag;
    jobject exc;
    jvalue returnValue;
    jint id;
    InvokeRequest *request;
    jboolean detached;
    jboolean mustReleaseReturnValue = JNI_FALSE;

    JDI_ASSERT(thread);

    /* Prevent gcc errors on uninitialized variables. */
    tag = 0;
    exc = NULL;
    id  = 0;

    eventHandler_lock(); /* for proper lock order */
    debugMonitorEnter(invokerLock);

    request = threadControl_getInvokeRequest(thread);
    if (request == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting thread invoke request");
    }

    JDI_ASSERT(request->pending);
    JDI_ASSERT(request->started);

    request->pending = JNI_FALSE;
    request->started = JNI_FALSE;
    request->available = JNI_TRUE; /* For next time around */

    detached = request->detached;
    if (!detached) {
        if (request->options & JDWP_INVOKE_OPTIONS(SINGLE_THREADED)) {
            (void)threadControl_suspendThread(thread, JNI_FALSE);
        } else {
            (void)threadControl_suspendAll();
        }

        if (request->invokeType == INVOKE_CONSTRUCTOR) {
            /*
             * Although constructors technically have a return type of
             * void, we return the object created.
             */
            tag = specificTypeKey(env, request->returnValue.l);
        } else {
            tag = methodSignature_returnTag(request->methodSignature);
        }
        id = request->id;
        exc = request->exception;
        returnValue = request->returnValue;

        /* Release return value and exception references, but delay the release
         * until after the return packet was sent. */
        jbyte returnType = methodSignature_returnTag(request->methodSignature);
        mustReleaseReturnValue = request->invokeType == INVOKE_CONSTRUCTOR ||
           isReferenceTag(returnType);
    }

    /*
     * At this time, there's no need to retain global references on
     * arguments since the reply is processed. No one will deal with
     * this request ID anymore, so we must call deleteGlobalArgumentRefs().
     *
     * We cannot delete saved exception or return value references
     * since otherwise a deleted handle would escape when writing
     * the response to the stream. Instead, we clean those refs up
     * after writing the respone.
     */
    deleteGlobalArgumentRefs(env, request);

    /* From now on, do not access the request structure anymore
     * for this request id, because once we give up the invokerLock it may
     * be immediately reused by a new invoke request.
     */
    request = NULL;

    /*
     * Give up the lock before I/O operation
     */
    debugMonitorExit(invokerLock);
    eventHandler_unlock();

    if (!detached) {
        outStream_initReply(&out, id);
        (void)outStream_writeValue(env, &out, tag, returnValue);
        (void)outStream_writeObjectTag(env, &out, exc);
        (void)outStream_writeObjectRef(env, &out, exc);
        outStream_sendReply(&out);
    }

    /*
     * Delete potentially saved global references of return value
     * and exception
     */
    eventHandler_lock(); // for proper lock order
    debugMonitorEnter(invokerLock);
    if (mustReleaseReturnValue && returnValue.l != NULL) {
        tossGlobalRef(env, &returnValue.l);
    }
    if (exc != NULL) {
        tossGlobalRef(env, &exc);
    }
    debugMonitorExit(invokerLock);
    eventHandler_unlock();
}

jboolean
invoker_isEnabled(jthread thread)
{
    InvokeRequest *request;
    jboolean isEnabled;

    JDI_ASSERT(thread);
    debugMonitorEnter(invokerLock);
    request = threadControl_getInvokeRequest(thread);
    if (request == NULL) {
        EXIT_ERROR(AGENT_ERROR_INVALID_THREAD, "getting thread invoke request");
    }
    isEnabled = request->available;
    debugMonitorExit(invokerLock);
    return isEnabled;
}

void
invoker_detach(InvokeRequest *request)
{
    JDI_ASSERT(request);
    debugMonitorEnter(invokerLock);
    request->detached = JNI_TRUE;
    debugMonitorExit(invokerLock);
}
