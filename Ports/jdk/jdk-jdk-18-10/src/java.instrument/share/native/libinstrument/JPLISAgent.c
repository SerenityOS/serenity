/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright 2003 Wily Technology, Inc.
 */

#include    <jni.h>
#include    <jvm.h>
#include    <jvmti.h>
#include    <stdlib.h>
#include    <string.h>
#include    "JPLISAgent.h"
#include    "JPLISAssert.h"
#include    "Utilities.h"
#include    "Reentrancy.h"
#include    "JavaExceptions.h"

#include    "EncodingSupport.h"
#include    "FileSystemSupport.h"    /* For MAXPATHLEN & uintptr_t */

#include    "sun_instrument_InstrumentationImpl.h"

/*
 *  The JPLISAgent manages the initialization all of the Java programming language Agents.
 *  It also supports the native method bridge between the JPLIS and the JVMTI.
 *  It maintains a single JVMTI Env that all JPL agents share.
 *  It parses command line requests and creates individual Java agents.
 */


/*
 *  private prototypes
 */

/* Allocates an unformatted JPLIS agent data structure. Returns NULL if allocation fails. */
JPLISAgent *
allocateJPLISAgent(jvmtiEnv *       jvmtiEnv);

/* Initializes an already-allocated JPLIS agent data structure. */
JPLISInitializationError
initializeJPLISAgent(   JPLISAgent *    agent,
                        JavaVM *        vm,
                        jvmtiEnv *      jvmtienv);
/* De-allocates a JPLIS agent data structure. Only used in partial-failure cases at startup;
 * in normal usage the JPLIS agent lives forever
 */
void
deallocateJPLISAgent(   jvmtiEnv *      jvmtienv,
                        JPLISAgent *    agent);

/* Does one-time work to interrogate the JVM about capabilities and cache the answers. */
void
checkCapabilities(JPLISAgent * agent);

/* Takes the elements of the command string (agent class name and options string) and
 * create java strings for them.
 * Returns true if a classname was found. Makes no promises beyond the textual; says nothing about whether
 * the class exists or can be loaded.
 * If return value is true, sets outputClassname to a non-NULL local JNI reference.
 * If return value is true, sets outputOptionsString either to NULL or to a non-NULL local JNI reference.
 * If return value is false, neither output parameter is set.
 */
jboolean
commandStringIntoJavaStrings(  JNIEnv *        jnienv,
                               const char *    classname,
                               const char *    optionsString,
                               jstring *       outputClassname,
                               jstring *       outputOptionsString);

/* Start one Java agent from the supplied parameters.
 * Most of the logic lives in a helper function that lives over in Java code--
 * we pass parameters out to Java and use our own Java helper to actually
 * load the agent and call the premain.
 * Returns true if the Java agent class is loaded and the premain/agentmain method completes
 * with no exceptions, false otherwise.
 */
jboolean
invokeJavaAgentMainMethod( JNIEnv *    jnienv,
                           jobject     instrumentationImpl,
                           jmethodID   agentMainMethod,
                           jstring     className,
                           jstring     optionsString);

/* Once we have loaded the Java agent and called the premain,
 * we can release the copies we have been keeping of the command line
 * data (agent class name and option strings).
 */
void
deallocateCommandLineData(JPLISAgent * agent);

/*
 *  Common support for various class list fetchers.
 */
typedef jvmtiError (*ClassListFetcher)
    (   jvmtiEnv *  jvmtiEnv,
        jobject     classLoader,
        jint *      classCount,
        jclass **   classes);

/* Fetcher that ignores the class loader parameter, and uses the JVMTI to get a list of all classes.
 * Returns a jvmtiError according to the underlying JVMTI service.
 */
jvmtiError
getAllLoadedClassesClassListFetcher(    jvmtiEnv *  jvmtiEnv,
                                        jobject     classLoader,
                                        jint *      classCount,
                                        jclass **   classes);

/* Fetcher that uses the class loader parameter, and uses the JVMTI to get a list of all classes
 * for which the supplied loader is the initiating loader.
 * Returns a jvmtiError according to the underlying JVMTI service.
 */
jvmtiError
getInitiatedClassesClassListFetcher(    jvmtiEnv *  jvmtiEnv,
                                        jobject     classLoader,
                                        jint *      classCount,
                                        jclass **   classes);

/*
 * Common guts for two native methods, which are the same except for the policy for fetching
 * the list of classes.
 * Either returns a local JNI reference to an array of references to java.lang.Class.
 * Can throw, if it does will alter the JNIEnv with an outstanding exception.
 */
jobjectArray
commonGetClassList( JNIEnv *            jnienv,
                    JPLISAgent *        agent,
                    jobject             classLoader,
                    ClassListFetcher    fetcher);


/*
 *  Misc. utilities.
 */

/* Checked exception mapper used by the redefine classes implementation.
 * Allows ClassNotFoundException or UnmodifiableClassException; maps others
 * to InternalError. Can return NULL in an error case.
 */
jthrowable
redefineClassMapper(    JNIEnv *    jnienv,
                        jthrowable  throwableToMap);

/* Turns a buffer of jclass * into a Java array whose elements are java.lang.Class.
 * Can throw, if it does will alter the JNIEnv with an outstanding exception.
 */
jobjectArray
getObjectArrayFromClasses(JNIEnv* jnienv, jclass* classes, jint classCount);


JPLISEnvironment *
getJPLISEnvironment(jvmtiEnv * jvmtienv) {
    JPLISEnvironment * environment  = NULL;
    jvmtiError         jvmtierror   = JVMTI_ERROR_NONE;

    jvmtierror = (*jvmtienv)->GetEnvironmentLocalStorage(
                                            jvmtienv,
                                            (void**)&environment);
    /* can be called from any phase */
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);

    if (jvmtierror == JVMTI_ERROR_NONE) {
        jplis_assert(environment != NULL);
        jplis_assert(environment->mJVMTIEnv == jvmtienv);
    } else {
        environment = NULL;
    }
    return environment;
}

/*
 *  OnLoad processing code.
 */

/*
 *  Creates a new JPLISAgent.
 *  Returns error if the agent cannot be created and initialized.
 *  The JPLISAgent* pointed to by agent_ptr is set to the new broker,
 *  or NULL if an error has occurred.
 */
JPLISInitializationError
createNewJPLISAgent(JavaVM * vm, JPLISAgent **agent_ptr) {
    JPLISInitializationError initerror       = JPLIS_INIT_ERROR_NONE;
    jvmtiEnv *               jvmtienv        = NULL;
    jint                     jnierror        = JNI_OK;

    *agent_ptr = NULL;
    jnierror = (*vm)->GetEnv(  vm,
                               (void **) &jvmtienv,
                               JVMTI_VERSION_1_1);
    if ( jnierror != JNI_OK ) {
        initerror = JPLIS_INIT_ERROR_CANNOT_CREATE_NATIVE_AGENT;
    } else {
        JPLISAgent * agent = allocateJPLISAgent(jvmtienv);
        if ( agent == NULL ) {
            initerror = JPLIS_INIT_ERROR_ALLOCATION_FAILURE;
        } else {
            initerror = initializeJPLISAgent(  agent,
                                               vm,
                                               jvmtienv);
            if ( initerror == JPLIS_INIT_ERROR_NONE ) {
                *agent_ptr = agent;
            } else {
                deallocateJPLISAgent(jvmtienv, agent);
            }
        }

        /* don't leak envs */
        if ( initerror != JPLIS_INIT_ERROR_NONE ) {
            jvmtiError jvmtierror = (*jvmtienv)->DisposeEnvironment(jvmtienv);
            /* can be called from any phase */
            jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
        }
    }

    return initerror;
}

/*
 *  Allocates a JPLISAgent. Returns NULL if it cannot be allocated
 */
JPLISAgent *
allocateJPLISAgent(jvmtiEnv * jvmtienv) {
  return (JPLISAgent *) allocate( jvmtienv,
                                    sizeof(JPLISAgent));
}

JPLISInitializationError
initializeJPLISAgent(   JPLISAgent *    agent,
                        JavaVM *        vm,
                        jvmtiEnv *      jvmtienv) {
    jvmtiError      jvmtierror = JVMTI_ERROR_NONE;
    jvmtiPhase      phase;

    agent->mJVM                                      = vm;
    agent->mNormalEnvironment.mJVMTIEnv              = jvmtienv;
    agent->mNormalEnvironment.mAgent                 = agent;
    agent->mNormalEnvironment.mIsRetransformer       = JNI_FALSE;
    agent->mRetransformEnvironment.mJVMTIEnv         = NULL;        /* NULL until needed */
    agent->mRetransformEnvironment.mAgent            = agent;
    agent->mRetransformEnvironment.mIsRetransformer  = JNI_FALSE;   /* JNI_FALSE until mJVMTIEnv is set */
    agent->mAgentmainCaller                          = NULL;
    agent->mInstrumentationImpl                      = NULL;
    agent->mPremainCaller                            = NULL;
    agent->mTransform                                = NULL;
    agent->mRedefineAvailable                        = JNI_FALSE;   /* assume no for now */
    agent->mRedefineAdded                            = JNI_FALSE;
    agent->mNativeMethodPrefixAvailable              = JNI_FALSE;   /* assume no for now */
    agent->mNativeMethodPrefixAdded                  = JNI_FALSE;
    agent->mAgentClassName                           = NULL;
    agent->mOptionsString                            = NULL;
    agent->mJarfile                                  = NULL;

    /* make sure we can recover either handle in either direction.
     * the agent has a ref to the jvmti; make it mutual
     */
    jvmtierror = (*jvmtienv)->SetEnvironmentLocalStorage(
                                            jvmtienv,
                                            &(agent->mNormalEnvironment));
    /* can be called from any phase */
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);

    /* check what capabilities are available */
    checkCapabilities(agent);

    /* check phase - if live phase then we don't need the VMInit event */
    jvmtierror = (*jvmtienv)->GetPhase(jvmtienv, &phase);
    /* can be called from any phase */
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    if (phase == JVMTI_PHASE_LIVE) {
        return JPLIS_INIT_ERROR_NONE;
    }

    if (phase != JVMTI_PHASE_ONLOAD) {
        /* called too early or called too late; either way bail out */
        return JPLIS_INIT_ERROR_FAILURE;
    }

    /* now turn on the VMInit event */
    if ( jvmtierror == JVMTI_ERROR_NONE ) {
        jvmtiEventCallbacks callbacks;
        memset(&callbacks, 0, sizeof(callbacks));
        callbacks.VMInit = &eventHandlerVMInit;

        jvmtierror = (*jvmtienv)->SetEventCallbacks( jvmtienv,
                                                     &callbacks,
                                                     sizeof(callbacks));
        check_phase_ret_blob(jvmtierror, JPLIS_INIT_ERROR_FAILURE);
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    }

    if ( jvmtierror == JVMTI_ERROR_NONE ) {
        jvmtierror = (*jvmtienv)->SetEventNotificationMode(
                                                jvmtienv,
                                                JVMTI_ENABLE,
                                                JVMTI_EVENT_VM_INIT,
                                                NULL /* all threads */);
        check_phase_ret_blob(jvmtierror, JPLIS_INIT_ERROR_FAILURE);
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    }

    return (jvmtierror == JVMTI_ERROR_NONE)? JPLIS_INIT_ERROR_NONE : JPLIS_INIT_ERROR_FAILURE;
}

void
deallocateJPLISAgent(jvmtiEnv * jvmtienv, JPLISAgent * agent) {
    deallocate(jvmtienv, agent);
}


JPLISInitializationError
recordCommandLineData(  JPLISAgent *    agent,
                        const char *    agentClassName,
                        const char *    optionsString ) {
    JPLISInitializationError    initerror   = JPLIS_INIT_ERROR_NONE;
    char *      ourCopyOfAgentClassName     = NULL;
    char *      ourCopyOfOptionsString      = NULL;

    /* if no actual params, bail out now */
    if ((agentClassName == NULL) || (*agentClassName == 0)) {
        initerror = JPLIS_INIT_ERROR_AGENT_CLASS_NOT_SPECIFIED;
    } else {
        ourCopyOfAgentClassName = allocate(jvmti(agent), strlen(agentClassName)+1);
        if (ourCopyOfAgentClassName == NULL) {
            initerror = JPLIS_INIT_ERROR_ALLOCATION_FAILURE;
        } else {
            if (optionsString != NULL) {
                ourCopyOfOptionsString = allocate(jvmti(agent), strlen(optionsString)+1);
                if (ourCopyOfOptionsString == NULL) {
                    deallocate(jvmti(agent), ourCopyOfAgentClassName);
                    initerror = JPLIS_INIT_ERROR_ALLOCATION_FAILURE;
                }
            }
        }
    }

    if (initerror == JPLIS_INIT_ERROR_NONE) {
        strcpy(ourCopyOfAgentClassName, agentClassName);
        if (optionsString != NULL) {
            strcpy(ourCopyOfOptionsString, optionsString);
        }
        agent->mAgentClassName = ourCopyOfAgentClassName;
        agent->mOptionsString = ourCopyOfOptionsString;
    }

    return initerror;
}

/*
 *  VMInit processing code.
 */


/*
 * If this call fails, the JVM launch will ultimately be aborted,
 * so we don't have to be super-careful to clean up in partial failure
 * cases.
 */
jboolean
processJavaStart(   JPLISAgent *    agent,
                    JNIEnv *        jnienv) {
    jboolean    result;

    /*
     *  OK, Java is up now. We can start everything that needs Java.
     */

    /*
     *  First make our fallback InternalError throwable.
     */
    result = initializeFallbackError(jnienv);
    jplis_assert_msg(result, "fallback init failed");

    /*
     *  Now make the InstrumentationImpl instance.
     */
    if ( result ) {
        result = createInstrumentationImpl(jnienv, agent);
        jplis_assert_msg(result, "instrumentation instance creation failed");
    }


    /*
     *  Register a handler for ClassFileLoadHook (without enabling this event).
     *  Turn off the VMInit handler.
     */
    if ( result ) {
        result = setLivePhaseEventHandlers(agent);
        jplis_assert_msg(result, "setting of live phase VM handlers failed");
    }

    /*
     *  Load the Java agent, and call the premain.
     */
    if ( result ) {
        result = startJavaAgent(agent, jnienv,
                                agent->mAgentClassName, agent->mOptionsString,
                                agent->mPremainCaller);
        jplis_assert_msg(result, "agent load/premain call failed");
    }

    /*
     * Finally surrender all of the tracking data that we don't need any more.
     * If something is wrong, skip it, we will be aborting the JVM anyway.
     */
    if ( result ) {
        deallocateCommandLineData(agent);
    }

    return result;
}

jboolean
startJavaAgent( JPLISAgent *    agent,
                JNIEnv *        jnienv,
                const char *    classname,
                const char *    optionsString,
                jmethodID       agentMainMethod) {
    jboolean    success = JNI_FALSE;
    jstring classNameObject = NULL;
    jstring optionsStringObject = NULL;

    success = commandStringIntoJavaStrings(    jnienv,
                                               classname,
                                               optionsString,
                                               &classNameObject,
                                               &optionsStringObject);

    if (success) {
        success = invokeJavaAgentMainMethod(   jnienv,
                                               agent->mInstrumentationImpl,
                                               agentMainMethod,
                                               classNameObject,
                                               optionsStringObject);
    }

    return success;
}

void
deallocateCommandLineData( JPLISAgent * agent) {
    deallocate(jvmti(agent), (void*)agent->mAgentClassName);
    deallocate(jvmti(agent), (void*)agent->mOptionsString);

    /* zero things out so it is easier to see what is going on */
    agent->mAgentClassName = NULL;
    agent->mOptionsString = NULL;
}

/*
 * Create the java.lang.instrument.Instrumentation instance
 * and access information for it (method IDs, etc)
 */
jboolean
createInstrumentationImpl( JNIEnv *        jnienv,
                           JPLISAgent *    agent) {
    jclass      implClass               = NULL;
    jboolean    errorOutstanding        = JNI_FALSE;
    jobject     resultImpl              = NULL;
    jmethodID   premainCallerMethodID   = NULL;
    jmethodID   agentmainCallerMethodID = NULL;
    jmethodID   transformMethodID       = NULL;
    jmethodID   constructorID           = NULL;
    jobject     localReference          = NULL;

    /* First find the class of our implementation */
    implClass = (*jnienv)->FindClass(   jnienv,
                                        JPLIS_INSTRUMENTIMPL_CLASSNAME);
    errorOutstanding = checkForAndClearThrowable(jnienv);
    errorOutstanding = errorOutstanding || (implClass == NULL);
    jplis_assert_msg(!errorOutstanding, "find class on InstrumentationImpl failed");

    if ( !errorOutstanding ) {
        constructorID = (*jnienv)->GetMethodID( jnienv,
                                                implClass,
                                                JPLIS_INSTRUMENTIMPL_CONSTRUCTOR_METHODNAME,
                                                JPLIS_INSTRUMENTIMPL_CONSTRUCTOR_METHODSIGNATURE);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        errorOutstanding = errorOutstanding || (constructorID == NULL);
        jplis_assert_msg(!errorOutstanding, "find constructor on InstrumentationImpl failed");
        }

    if ( !errorOutstanding ) {
        jlong   peerReferenceAsScalar = (jlong)(intptr_t) agent;
        localReference = (*jnienv)->NewObject(  jnienv,
                                                implClass,
                                                constructorID,
                                                peerReferenceAsScalar,
                                                agent->mRedefineAdded,
                                                agent->mNativeMethodPrefixAdded);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        errorOutstanding = errorOutstanding || (localReference == NULL);
        jplis_assert_msg(!errorOutstanding, "call constructor on InstrumentationImpl failed");
    }

    if ( !errorOutstanding ) {
        resultImpl = (*jnienv)->NewGlobalRef(jnienv, localReference);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert_msg(!errorOutstanding, "copy local ref to global ref");
    }

    /* Now look up the method ID for the pre-main caller (we will need this more than once) */
    if ( !errorOutstanding ) {
        premainCallerMethodID = (*jnienv)->GetMethodID( jnienv,
                                                        implClass,
                                                        JPLIS_INSTRUMENTIMPL_PREMAININVOKER_METHODNAME,
                                                        JPLIS_INSTRUMENTIMPL_PREMAININVOKER_METHODSIGNATURE);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        errorOutstanding = errorOutstanding || (premainCallerMethodID == NULL);
        jplis_assert_msg(!errorOutstanding, "can't find premain invoker methodID");
    }

    /* Now look up the method ID for the agent-main caller */
    if ( !errorOutstanding ) {
        agentmainCallerMethodID = (*jnienv)->GetMethodID( jnienv,
                                                          implClass,
                                                          JPLIS_INSTRUMENTIMPL_AGENTMAININVOKER_METHODNAME,
                                                          JPLIS_INSTRUMENTIMPL_AGENTMAININVOKER_METHODSIGNATURE);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        errorOutstanding = errorOutstanding || (agentmainCallerMethodID == NULL);
        jplis_assert_msg(!errorOutstanding, "can't find agentmain invoker methodID");
    }

    /* Now look up the method ID for the transform method (we will need this constantly) */
    if ( !errorOutstanding ) {
        transformMethodID = (*jnienv)->GetMethodID( jnienv,
                                                    implClass,
                                                    JPLIS_INSTRUMENTIMPL_TRANSFORM_METHODNAME,
                                                    JPLIS_INSTRUMENTIMPL_TRANSFORM_METHODSIGNATURE);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        errorOutstanding = errorOutstanding || (transformMethodID == NULL);
        jplis_assert_msg(!errorOutstanding, "can't find transform methodID");
    }

    if ( !errorOutstanding ) {
        agent->mInstrumentationImpl = resultImpl;
        agent->mPremainCaller       = premainCallerMethodID;
        agent->mAgentmainCaller     = agentmainCallerMethodID;
        agent->mTransform           = transformMethodID;
    }

    return !errorOutstanding;
}

jboolean
commandStringIntoJavaStrings(  JNIEnv *        jnienv,
                               const char *    classname,
                               const char *    optionsString,
                               jstring *       outputClassname,
                               jstring *       outputOptionsString) {
    jstring     classnameJavaString     = NULL;
    jstring     optionsJavaString       = NULL;
    jboolean    errorOutstanding        = JNI_TRUE;

    classnameJavaString = (*jnienv)->NewStringUTF(jnienv, classname);
    errorOutstanding = checkForAndClearThrowable(jnienv);
    jplis_assert_msg(!errorOutstanding, "can't create class name java string");

    if ( !errorOutstanding ) {
        if ( optionsString != NULL) {
            optionsJavaString = (*jnienv)->NewStringUTF(jnienv, optionsString);
            errorOutstanding = checkForAndClearThrowable(jnienv);
            jplis_assert_msg(!errorOutstanding, "can't create options java string");
        }

        if ( !errorOutstanding ) {
            *outputClassname        = classnameJavaString;
            *outputOptionsString    = optionsJavaString;
        }
    }

    return !errorOutstanding;
}


jboolean
invokeJavaAgentMainMethod( JNIEnv *    jnienv,
                           jobject     instrumentationImpl,
                           jmethodID   mainCallingMethod,
                           jstring     className,
                           jstring     optionsString) {
    jboolean errorOutstanding = JNI_FALSE;

    jplis_assert(mainCallingMethod != NULL);
    if ( mainCallingMethod != NULL ) {
        (*jnienv)->CallVoidMethod(  jnienv,
                                    instrumentationImpl,
                                    mainCallingMethod,
                                    className,
                                    optionsString);
        errorOutstanding = checkForThrowable(jnienv);
        if ( errorOutstanding ) {
            logThrowable(jnienv);
        }
        checkForAndClearThrowable(jnienv);
    }
    return !errorOutstanding;
}

jboolean
setLivePhaseEventHandlers(  JPLISAgent * agent) {
    jvmtiEventCallbacks callbacks;
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiError          jvmtierror;

    /* first swap out the handlers (switch from the VMInit handler, which we do not need,
     * to the ClassFileLoadHook handler, which is what the agents need from now on)
     */
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &eventHandlerClassFileLoadHook;

    jvmtierror = (*jvmtienv)->SetEventCallbacks( jvmtienv,
                                                 &callbacks,
                                                 sizeof(callbacks));
    check_phase_ret_false(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);


    if ( jvmtierror == JVMTI_ERROR_NONE ) {
        /* turn off VMInit */
        jvmtierror = (*jvmtienv)->SetEventNotificationMode(
                                                    jvmtienv,
                                                    JVMTI_DISABLE,
                                                    JVMTI_EVENT_VM_INIT,
                                                    NULL /* all threads */);
        check_phase_ret_false(jvmtierror);
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    }

    return (jvmtierror == JVMTI_ERROR_NONE);
}

/**
 *  Check if the can_redefine_classes capability is available.
 */
void
checkCapabilities(JPLISAgent * agent) {
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiCapabilities   potentialCapabilities;
    jvmtiError          jvmtierror;

    memset(&potentialCapabilities, 0, sizeof(potentialCapabilities));

    jvmtierror = (*jvmtienv)->GetPotentialCapabilities(jvmtienv, &potentialCapabilities);
    check_phase_ret(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);

    if ( jvmtierror == JVMTI_ERROR_NONE ) {
        if ( potentialCapabilities.can_redefine_classes == 1 ) {
            agent->mRedefineAvailable = JNI_TRUE;
        }
        if ( potentialCapabilities.can_set_native_method_prefix == 1 ) {
            agent->mNativeMethodPrefixAvailable = JNI_TRUE;
        }
    }
}

/**
 * Enable native method prefix in one JVM TI environment
 */
void
enableNativeMethodPrefixCapability(jvmtiEnv * jvmtienv) {
    jvmtiCapabilities   desiredCapabilities;
    jvmtiError          jvmtierror;

        jvmtierror = (*jvmtienv)->GetCapabilities(jvmtienv, &desiredCapabilities);
        /* can be called from any phase */
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
        desiredCapabilities.can_set_native_method_prefix = 1;
        jvmtierror = (*jvmtienv)->AddCapabilities(jvmtienv, &desiredCapabilities);
        check_phase_ret(jvmtierror);
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
}


/**
 * Add the can_set_native_method_prefix capability
 */
void
addNativeMethodPrefixCapability(JPLISAgent * agent) {
    if (agent->mNativeMethodPrefixAvailable && !agent->mNativeMethodPrefixAdded) {
        jvmtiEnv * jvmtienv = agent->mNormalEnvironment.mJVMTIEnv;
        enableNativeMethodPrefixCapability(jvmtienv);

        jvmtienv = agent->mRetransformEnvironment.mJVMTIEnv;
        if (jvmtienv != NULL) {
            enableNativeMethodPrefixCapability(jvmtienv);
        }
        agent->mNativeMethodPrefixAdded = JNI_TRUE;
    }
}

/**
 * Add the can_maintain_original_method_order capability (for testing)
 */
void
addOriginalMethodOrderCapability(JPLISAgent * agent) {
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiCapabilities   desiredCapabilities;
    jvmtiError          jvmtierror;

    jvmtierror = (*jvmtienv)->GetCapabilities(jvmtienv, &desiredCapabilities);
    /* can be called from any phase */
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    desiredCapabilities.can_maintain_original_method_order = 1;
    jvmtierror = (*jvmtienv)->AddCapabilities(jvmtienv, &desiredCapabilities);
    check_phase_ret(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
}

/**
 * Add the can_redefine_classes capability
 */
void
addRedefineClassesCapability(JPLISAgent * agent) {
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiCapabilities   desiredCapabilities;
    jvmtiError          jvmtierror;

    if (agent->mRedefineAvailable && !agent->mRedefineAdded) {
        jvmtierror = (*jvmtienv)->GetCapabilities(jvmtienv, &desiredCapabilities);
        /* can be called from any phase */
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
        desiredCapabilities.can_redefine_classes = 1;
        jvmtierror = (*jvmtienv)->AddCapabilities(jvmtienv, &desiredCapabilities);
        check_phase_ret(jvmtierror);

        /*
         * With mixed premain/agentmain agents then it's possible that the
         * capability was potentially available in the onload phase but
         * subsequently unavailable in the live phase.
         */
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE ||
                     jvmtierror == JVMTI_ERROR_NOT_AVAILABLE);
        if (jvmtierror == JVMTI_ERROR_NONE) {
            agent->mRedefineAdded = JNI_TRUE;
        }
    }
}

static jobject
getModuleObject(jvmtiEnv*               jvmti,
                jobject                 loaderObject,
                const char*             cname) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jobject moduleObject = NULL;

    /* find last slash in the class name */
    char* last_slash = (cname == NULL) ? NULL : strrchr(cname, '/');
    int len = (last_slash == NULL) ? 0 : (int)(last_slash - cname);
    char* pkg_name_buf = (char*)malloc(len + 1);

    if (pkg_name_buf == NULL) {
        fprintf(stderr, "OOM error in native tmp buffer allocation");
        return NULL;
    }
    if (last_slash != NULL) {
        strncpy(pkg_name_buf, cname, len);
    }
    pkg_name_buf[len] = '\0';

    err = (*jvmti)->GetNamedModule(jvmti, loaderObject, pkg_name_buf, &moduleObject);
    free((void*)pkg_name_buf);
    check_phase_ret_blob(err, NULL);
    jplis_assert_msg(err == JVMTI_ERROR_NONE, "error in the JVMTI GetNamedModule");

    return moduleObject;
}

/*
 *  Support for the JVMTI callbacks
 */

void
transformClassFile(             JPLISAgent *            agent,
                                JNIEnv *                jnienv,
                                jobject                 loaderObject,
                                const char*             name,
                                jclass                  classBeingRedefined,
                                jobject                 protectionDomain,
                                jint                    class_data_len,
                                const unsigned char*    class_data,
                                jint*                   new_class_data_len,
                                unsigned char**         new_class_data,
                                jboolean                is_retransformer) {
    jboolean        errorOutstanding        = JNI_FALSE;
    jstring         classNameStringObject   = NULL;
    jarray          classFileBufferObject   = NULL;
    jarray          transformedBufferObject = NULL;
    jsize           transformedBufferSize   = 0;
    unsigned char * resultBuffer            = NULL;
    jboolean        shouldRun               = JNI_FALSE;

    /* only do this if we aren't already in the middle of processing a class on this thread */
    shouldRun = tryToAcquireReentrancyToken(
                                jvmti(agent),
                                NULL);  /* this thread */

    if ( shouldRun ) {
        /* first marshall all the parameters */
        classNameStringObject = (*jnienv)->NewStringUTF(jnienv,
                                                        name);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert_msg(!errorOutstanding, "can't create name string");

        if ( !errorOutstanding ) {
            classFileBufferObject = (*jnienv)->NewByteArray(jnienv,
                                                            class_data_len);
            errorOutstanding = checkForAndClearThrowable(jnienv);
            jplis_assert_msg(!errorOutstanding, "can't create byte array");
        }

        if ( !errorOutstanding ) {
            jbyte * typedBuffer = (jbyte *) class_data; /* nasty cast, dumb JNI interface, const missing */
                                                        /* The sign cast is safe. The const cast is dumb. */
            (*jnienv)->SetByteArrayRegion(  jnienv,
                                            classFileBufferObject,
                                            0,
                                            class_data_len,
                                            typedBuffer);
            errorOutstanding = checkForAndClearThrowable(jnienv);
            jplis_assert_msg(!errorOutstanding, "can't set byte array region");
        }

        /*  now call the JPL agents to do the transforming */
        /*  potential future optimization: may want to skip this if there are none */
        if ( !errorOutstanding ) {
            jobject moduleObject = NULL;

            if (classBeingRedefined == NULL) {
                moduleObject = getModuleObject(jvmti(agent), loaderObject, name);
            } else {
                // Redefine or retransform, InstrumentationImpl.transform() will use
                // classBeingRedefined.getModule() to get the module.
            }
            jplis_assert(agent->mInstrumentationImpl != NULL);
            jplis_assert(agent->mTransform != NULL);
            transformedBufferObject = (*jnienv)->CallObjectMethod(
                                                jnienv,
                                                agent->mInstrumentationImpl,
                                                agent->mTransform,
                                                moduleObject,
                                                loaderObject,
                                                classNameStringObject,
                                                classBeingRedefined,
                                                protectionDomain,
                                                classFileBufferObject,
                                                is_retransformer);
            errorOutstanding = checkForAndClearThrowable(jnienv);
            jplis_assert_msg(!errorOutstanding, "transform method call failed");
        }

        /* Finally, unmarshall the parameters (if someone touched the buffer, tell the JVM) */
        if ( !errorOutstanding ) {
            if ( transformedBufferObject != NULL ) {
                transformedBufferSize = (*jnienv)->GetArrayLength(  jnienv,
                                                                    transformedBufferObject);
                errorOutstanding = checkForAndClearThrowable(jnienv);
                jplis_assert_msg(!errorOutstanding, "can't get array length");

                if ( !errorOutstanding ) {
                    /* allocate the response buffer with the JVMTI allocate call.
                     *  This is what the JVMTI spec says to do for Class File Load hook responses
                     */
                    jvmtiError  allocError = (*(jvmti(agent)))->Allocate(jvmti(agent),
                                                                             transformedBufferSize,
                                                                             &resultBuffer);
                    errorOutstanding = (allocError != JVMTI_ERROR_NONE);
                    jplis_assert_msg(!errorOutstanding, "can't allocate result buffer");
                }

                if ( !errorOutstanding ) {
                    (*jnienv)->GetByteArrayRegion(  jnienv,
                                                    transformedBufferObject,
                                                    0,
                                                    transformedBufferSize,
                                                    (jbyte *) resultBuffer);
                    errorOutstanding = checkForAndClearThrowable(jnienv);
                    jplis_assert_msg(!errorOutstanding, "can't get byte array region");

                    /* in this case, we will not return the buffer to the JVMTI,
                     * so we need to deallocate it ourselves
                     */
                    if ( errorOutstanding ) {
                        deallocate( jvmti(agent),
                                   (void*)resultBuffer);
                    }
                }

                if ( !errorOutstanding ) {
                    *new_class_data_len = (transformedBufferSize);
                    *new_class_data     = resultBuffer;
                }
            }
        }

        /* release the token */
        releaseReentrancyToken( jvmti(agent),
                                NULL);      /* this thread */

    }

    return;
}

/*
 *  Misc. internal utilities.
 */

/*
 *  The only checked exceptions we can throw are ClassNotFoundException and
 *  UnmodifiableClassException. All others map to InternalError.
 */
jthrowable
redefineClassMapper(    JNIEnv *    jnienv,
                        jthrowable  throwableToMap) {
    jthrowable  mappedThrowable = NULL;

    jplis_assert(isSafeForJNICalls(jnienv));
    jplis_assert(!isUnchecked(jnienv, throwableToMap));

    if ( isInstanceofClassName( jnienv,
                                throwableToMap,
                                "java/lang/ClassNotFoundException") ) {
        mappedThrowable = throwableToMap;
    } else {
        if ( isInstanceofClassName( jnienv,
                                throwableToMap,
                                "java/lang/instrument/UnmodifiableClassException")) {
            mappedThrowable = throwableToMap;
        } else {
            jstring message = NULL;

            message = getMessageFromThrowable(jnienv, throwableToMap);
            mappedThrowable = createInternalError(jnienv, message);
        }
    }

    jplis_assert(isSafeForJNICalls(jnienv));
    return mappedThrowable;
}

jobjectArray
getObjectArrayFromClasses(JNIEnv* jnienv, jclass* classes, jint classCount) {
    jclass          classArrayClass = NULL;
    jobjectArray    localArray      = NULL;
    jint            classIndex      = 0;
    jboolean        errorOccurred   = JNI_FALSE;

    /* get the class array class */
    classArrayClass = (*jnienv)->FindClass(jnienv, "java/lang/Class");
    errorOccurred = checkForThrowable(jnienv);

    if (!errorOccurred) {
        jplis_assert_msg(classArrayClass != NULL, "FindClass returned null class");

        /* create the array for the classes */
        localArray = (*jnienv)->NewObjectArray(jnienv, classCount, classArrayClass, NULL);
        errorOccurred = checkForThrowable(jnienv);

        if (!errorOccurred) {
            jplis_assert_msg(localArray != NULL, "NewObjectArray returned null array");

            /* now copy refs to all the classes and put them into the array */
            for (classIndex = 0; classIndex < classCount; classIndex++) {
                /* put class into array */
                (*jnienv)->SetObjectArrayElement(jnienv, localArray, classIndex, classes[classIndex]);
                errorOccurred = checkForThrowable(jnienv);

                if (errorOccurred) {
                    localArray = NULL;
                    break;
                }
            }
        }
    }

    return localArray;
}


/* Return the environment with the retransformation capability.
 * Create it if it doesn't exist.
 * Return NULL if it can't be created.
 */
jvmtiEnv *
retransformableEnvironment(JPLISAgent * agent) {
    jvmtiEnv *          retransformerEnv     = NULL;
    jint                jnierror             = JNI_OK;
    jvmtiCapabilities   desiredCapabilities;
    jvmtiEventCallbacks callbacks;
    jvmtiError          jvmtierror;

    if (agent->mRetransformEnvironment.mJVMTIEnv != NULL) {
        return agent->mRetransformEnvironment.mJVMTIEnv;
    }
    jnierror = (*agent->mJVM)->GetEnv(  agent->mJVM,
                               (void **) &retransformerEnv,
                               JVMTI_VERSION_1_1);
    if ( jnierror != JNI_OK ) {
        return NULL;
    }
    jvmtierror = (*retransformerEnv)->GetCapabilities(retransformerEnv, &desiredCapabilities);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    desiredCapabilities.can_retransform_classes = 1;
    if (agent->mNativeMethodPrefixAdded) {
        desiredCapabilities.can_set_native_method_prefix = 1;
    }

    jvmtierror = (*retransformerEnv)->AddCapabilities(retransformerEnv, &desiredCapabilities);
    if (jvmtierror != JVMTI_ERROR_NONE) {
         /* cannot get the capability, dispose of the retransforming environment */
        jvmtierror = (*retransformerEnv)->DisposeEnvironment(retransformerEnv);
        jplis_assert(jvmtierror == JVMTI_ERROR_NOT_AVAILABLE);
        return NULL;
    }
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &eventHandlerClassFileLoadHook;

    jvmtierror = (*retransformerEnv)->SetEventCallbacks(retransformerEnv,
                                                        &callbacks,
                                                        sizeof(callbacks));
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    if (jvmtierror == JVMTI_ERROR_NONE) {
        // install the retransforming environment
        agent->mRetransformEnvironment.mJVMTIEnv = retransformerEnv;
        agent->mRetransformEnvironment.mIsRetransformer = JNI_TRUE;

        // Make it for ClassFileLoadHook handling
        jvmtierror = (*retransformerEnv)->SetEnvironmentLocalStorage(
                                                       retransformerEnv,
                                                       &(agent->mRetransformEnvironment));
        jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
        if (jvmtierror == JVMTI_ERROR_NONE) {
            return retransformerEnv;
        }
    }
    return NULL;
}


/*
 *  Underpinnings for native methods
 */

jboolean
isModifiableClass(JNIEnv * jnienv, JPLISAgent * agent, jclass clazz) {
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiError          jvmtierror;
    jboolean            is_modifiable = JNI_FALSE;

    jvmtierror = (*jvmtienv)->IsModifiableClass( jvmtienv,
                                                 clazz,
                                                 &is_modifiable);
    check_phase_ret_false(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);

    return is_modifiable;
}

jboolean
isRetransformClassesSupported(JNIEnv * jnienv, JPLISAgent * agent) {
    return agent->mRetransformEnvironment.mIsRetransformer;
}

void
setHasTransformers(JNIEnv * jnienv, JPLISAgent * agent, jboolean has) {
    jvmtiEnv *          jvmtienv = jvmti(agent);
    jvmtiError          jvmtierror;

    jplis_assert(jvmtienv != NULL);
    jvmtierror = (*jvmtienv)->SetEventNotificationMode(
                                            jvmtienv,
                                            has? JVMTI_ENABLE : JVMTI_DISABLE,
                                            JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                            NULL /* all threads */);
    check_phase_ret(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
}

void
setHasRetransformableTransformers(JNIEnv * jnienv, JPLISAgent * agent, jboolean has) {
    jvmtiEnv *          retransformerEnv     = retransformableEnvironment(agent);
    jvmtiError          jvmtierror;

    jplis_assert(retransformerEnv != NULL);
    jvmtierror = (*retransformerEnv)->SetEventNotificationMode(
                                                    retransformerEnv,
                                                    has? JVMTI_ENABLE : JVMTI_DISABLE,
                                                    JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                    NULL /* all threads */);
    check_phase_ret(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
}

void
retransformClasses(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray classes) {
    jvmtiEnv *  retransformerEnv     = retransformableEnvironment(agent);
    jboolean    errorOccurred        = JNI_FALSE;
    jvmtiError  errorCode            = JVMTI_ERROR_NONE;
    jsize       numClasses           = 0;
    jclass *    classArray           = NULL;

    /* This is supposed to be checked by caller, but just to be sure */
    if (retransformerEnv == NULL) {
        jplis_assert(retransformerEnv != NULL);
        errorOccurred = JNI_TRUE;
        errorCode = JVMTI_ERROR_MUST_POSSESS_CAPABILITY;
    }

    /* This was supposed to be checked by caller too */
    if (!errorOccurred && classes == NULL) {
        jplis_assert(classes != NULL);
        errorOccurred = JNI_TRUE;
        errorCode = JVMTI_ERROR_NULL_POINTER;
    }

    if (!errorOccurred) {
        numClasses = (*jnienv)->GetArrayLength(jnienv, classes);
        errorOccurred = checkForThrowable(jnienv);
        jplis_assert(!errorOccurred);

        if (!errorOccurred && numClasses == 0) {
            jplis_assert(numClasses != 0);
            errorOccurred = JNI_TRUE;
            errorCode = JVMTI_ERROR_NULL_POINTER;
        }
    }

    if (!errorOccurred) {
        classArray = (jclass *) allocate(retransformerEnv,
                                         numClasses * sizeof(jclass));
        errorOccurred = (classArray == NULL);
        jplis_assert(!errorOccurred);
        if (errorOccurred) {
            errorCode = JVMTI_ERROR_OUT_OF_MEMORY;
        }
    }

    if (!errorOccurred) {
        jint index;
        for (index = 0; index < numClasses; index++) {
            classArray[index] = (*jnienv)->GetObjectArrayElement(jnienv, classes, index);
            errorOccurred = checkForThrowable(jnienv);
            jplis_assert(!errorOccurred);
            if (errorOccurred) {
                break;
            }

            if (classArray[index] == NULL) {
                jplis_assert(classArray[index] != NULL);
                errorOccurred = JNI_TRUE;
                errorCode = JVMTI_ERROR_NULL_POINTER;
                break;
            }
        }
    }

    if (!errorOccurred) {
        errorCode = (*retransformerEnv)->RetransformClasses(retransformerEnv,
                                                            numClasses, classArray);
        errorOccurred = (errorCode != JVMTI_ERROR_NONE);
    }

    /* Give back the buffer if we allocated it.  Throw any exceptions after.
     */
    if (classArray != NULL) {
        deallocate(retransformerEnv, (void*)classArray);
    }

    /* Return back if we executed the JVMTI API in a wrong phase
     */
    check_phase_ret(errorCode);

    if (errorCode != JVMTI_ERROR_NONE) {
        createAndThrowThrowableFromJVMTIErrorCode(jnienv, errorCode);
    }

    mapThrownThrowableIfNecessary(jnienv, redefineClassMapper);
}

/*
 *  Java code must not call this with a null list or a zero-length list.
 */
void
redefineClasses(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray classDefinitions) {
    jvmtiEnv*   jvmtienv                        = jvmti(agent);
    jboolean    errorOccurred                   = JNI_FALSE;
    jclass      classDefClass                   = NULL;
    jmethodID   getDefinitionClassMethodID      = NULL;
    jmethodID   getDefinitionClassFileMethodID  = NULL;
    jvmtiClassDefinition* classDefs             = NULL;
    jbyteArray* targetFiles                     = NULL;
    jsize       numDefs                         = 0;

    jplis_assert(classDefinitions != NULL);

    numDefs = (*jnienv)->GetArrayLength(jnienv, classDefinitions);
    errorOccurred = checkForThrowable(jnienv);
    jplis_assert(!errorOccurred);

    if (!errorOccurred) {
        jplis_assert(numDefs > 0);
        /* get method IDs for methods to call on class definitions */
        classDefClass = (*jnienv)->FindClass(jnienv, "java/lang/instrument/ClassDefinition");
        errorOccurred = checkForThrowable(jnienv);
        jplis_assert(!errorOccurred);
    }

    if (!errorOccurred) {
        getDefinitionClassMethodID = (*jnienv)->GetMethodID(    jnienv,
                                                classDefClass,
                                                "getDefinitionClass",
                                                "()Ljava/lang/Class;");
        errorOccurred = checkForThrowable(jnienv);
        jplis_assert(!errorOccurred);
    }

    if (!errorOccurred) {
        getDefinitionClassFileMethodID = (*jnienv)->GetMethodID(    jnienv,
                                                    classDefClass,
                                                    "getDefinitionClassFile",
                                                    "()[B");
        errorOccurred = checkForThrowable(jnienv);
        jplis_assert(!errorOccurred);
    }

    if (!errorOccurred) {
        classDefs = (jvmtiClassDefinition *) allocate(
                                                jvmtienv,
                                                numDefs * sizeof(jvmtiClassDefinition));
        errorOccurred = (classDefs == NULL);
        jplis_assert(!errorOccurred);
        if ( errorOccurred ) {
            createAndThrowThrowableFromJVMTIErrorCode(jnienv, JVMTI_ERROR_OUT_OF_MEMORY);
        }

        else {
            /*
             * We have to save the targetFile values that we compute so
             * that we can release the class_bytes arrays that are
             * returned by GetByteArrayElements(). In case of a JNI
             * error, we can't (easily) recompute the targetFile values
             * and we still want to free any memory we allocated.
             */
            targetFiles = (jbyteArray *) allocate(jvmtienv,
                                                  numDefs * sizeof(jbyteArray));
            errorOccurred = (targetFiles == NULL);
            jplis_assert(!errorOccurred);
            if ( errorOccurred ) {
                deallocate(jvmtienv, (void*)classDefs);
                createAndThrowThrowableFromJVMTIErrorCode(jnienv,
                    JVMTI_ERROR_OUT_OF_MEMORY);
            }
            else {
                jint i, j;

                // clear classDefs so we can correctly free memory during errors
                memset(classDefs, 0, numDefs * sizeof(jvmtiClassDefinition));

                for (i = 0; i < numDefs; i++) {
                    jclass      classDef    = NULL;

                    classDef = (*jnienv)->GetObjectArrayElement(jnienv, classDefinitions, i);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (errorOccurred) {
                        break;
                    }

                    classDefs[i].klass = (*jnienv)->CallObjectMethod(jnienv, classDef, getDefinitionClassMethodID);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (errorOccurred) {
                        break;
                    }

                    targetFiles[i] = (*jnienv)->CallObjectMethod(jnienv, classDef, getDefinitionClassFileMethodID);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (errorOccurred) {
                        break;
                    }

                    classDefs[i].class_byte_count = (*jnienv)->GetArrayLength(jnienv, targetFiles[i]);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (errorOccurred) {
                        break;
                    }

                    /*
                     * Allocate class_bytes last so we don't have to free
                     * memory on a partial row error.
                     */
                    classDefs[i].class_bytes = (unsigned char*)(*jnienv)->GetByteArrayElements(jnienv, targetFiles[i], NULL);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (errorOccurred) {
                        break;
                    }
                }

                if (!errorOccurred) {
                    jvmtiError  errorCode = JVMTI_ERROR_NONE;
                    errorCode = (*jvmtienv)->RedefineClasses(jvmtienv, numDefs, classDefs);
                    if (errorCode == JVMTI_ERROR_WRONG_PHASE) {
                        /* insulate caller from the wrong phase error */
                        errorCode = JVMTI_ERROR_NONE;
                    } else {
                        errorOccurred = (errorCode != JVMTI_ERROR_NONE);
                        if ( errorOccurred ) {
                            createAndThrowThrowableFromJVMTIErrorCode(jnienv, errorCode);
                        }
                    }
                }

                /*
                 * Cleanup memory that we allocated above. If we had a
                 * JNI error, a JVM/TI error or no errors, index 'i'
                 * tracks how far we got in processing the classDefs
                 * array. Note:  ReleaseByteArrayElements() is safe to
                 * call with a JNI exception pending.
                 */
                for (j = 0; j < i; j++) {
                    if ((jbyte *)classDefs[j].class_bytes != NULL) {
                        (*jnienv)->ReleaseByteArrayElements(jnienv,
                            targetFiles[j], (jbyte *)classDefs[j].class_bytes,
                            0 /* copy back and free */);
                        /*
                         * Only check for error if we didn't already have one
                         * so we don't overwrite errorOccurred.
                         */
                        if (!errorOccurred) {
                            errorOccurred = checkForThrowable(jnienv);
                            jplis_assert(!errorOccurred);
                        }
                    }
                }
                deallocate(jvmtienv, (void*)targetFiles);
                deallocate(jvmtienv, (void*)classDefs);
            }
        }
    }

    mapThrownThrowableIfNecessary(jnienv, redefineClassMapper);
}

/* Cheesy sharing. ClassLoader may be null. */
jobjectArray
commonGetClassList( JNIEnv *            jnienv,
                    JPLISAgent *        agent,
                    jobject             classLoader,
                    ClassListFetcher    fetcher) {
    jvmtiEnv *      jvmtienv        = jvmti(agent);
    jboolean        errorOccurred   = JNI_FALSE;
    jvmtiError      jvmtierror      = JVMTI_ERROR_NONE;
    jint            classCount      = 0;
    jclass *        classes         = NULL;
    jobjectArray    localArray      = NULL;

    /* retrieve the classes from the JVMTI agent */
    jvmtierror = (*fetcher)( jvmtienv,
                        classLoader,
                        &classCount,
                        &classes);
    check_phase_ret_blob(jvmtierror, localArray);
    errorOccurred = (jvmtierror != JVMTI_ERROR_NONE);
    jplis_assert(!errorOccurred);

    if ( errorOccurred ) {
        createAndThrowThrowableFromJVMTIErrorCode(jnienv, jvmtierror);
    } else {
        localArray = getObjectArrayFromClasses( jnienv,
                                                classes,
                                                classCount);
        errorOccurred = checkForThrowable(jnienv);
        jplis_assert(!errorOccurred);

        /* do this whether or not we saw a problem */
        deallocate(jvmtienv, (void*)classes);
    }

    mapThrownThrowableIfNecessary(jnienv, mapAllCheckedToInternalErrorMapper);
    return localArray;

}

jvmtiError
getAllLoadedClassesClassListFetcher(    jvmtiEnv *  jvmtienv,
                                        jobject     classLoader,
                                        jint *      classCount,
                                        jclass **   classes) {
    return (*jvmtienv)->GetLoadedClasses(jvmtienv, classCount, classes);
}

jobjectArray
getAllLoadedClasses(JNIEnv * jnienv, JPLISAgent * agent) {
    return commonGetClassList(  jnienv,
                                agent,
                                NULL,
                                getAllLoadedClassesClassListFetcher);
}

jvmtiError
getInitiatedClassesClassListFetcher(    jvmtiEnv *  jvmtienv,
                                        jobject     classLoader,
                                        jint *      classCount,
                                        jclass **   classes) {
    return (*jvmtienv)->GetClassLoaderClasses(jvmtienv, classLoader, classCount, classes);
}


jobjectArray
getInitiatedClasses(JNIEnv * jnienv, JPLISAgent * agent, jobject classLoader) {
    return commonGetClassList(  jnienv,
                                agent,
                                classLoader,
                                getInitiatedClassesClassListFetcher);
}

jlong
getObjectSize(JNIEnv * jnienv, JPLISAgent * agent, jobject objectToSize) {
    jvmtiEnv *  jvmtienv    = jvmti(agent);
    jlong       objectSize  = -1;
    jvmtiError  jvmtierror  = JVMTI_ERROR_NONE;

    jvmtierror = (*jvmtienv)->GetObjectSize(jvmtienv, objectToSize, &objectSize);
    check_phase_ret_0(jvmtierror);
    jplis_assert(jvmtierror == JVMTI_ERROR_NONE);
    if ( jvmtierror != JVMTI_ERROR_NONE ) {
        createAndThrowThrowableFromJVMTIErrorCode(jnienv, jvmtierror);
    }

    mapThrownThrowableIfNecessary(jnienv, mapAllCheckedToInternalErrorMapper);
    return objectSize;
}

void
appendToClassLoaderSearch(JNIEnv * jnienv, JPLISAgent * agent, jstring jarFile, jboolean isBootLoader)
{
    jvmtiEnv *  jvmtienv    = jvmti(agent);
    jboolean    errorOutstanding;
    jvmtiError  jvmtierror;
    const char* utf8Chars;
    jsize       utf8Len;
    jboolean    isCopy;
    char        platformChars[MAXPATHLEN];
    int         platformLen;

    utf8Len = (*jnienv)->GetStringUTFLength(jnienv, jarFile);
    errorOutstanding = checkForAndClearThrowable(jnienv);

    if (!errorOutstanding) {
        utf8Chars = (*jnienv)->GetStringUTFChars(jnienv, jarFile, &isCopy);
        errorOutstanding = checkForAndClearThrowable(jnienv);

        if (!errorOutstanding && utf8Chars != NULL) {
            /*
             * JVMTI spec'ed to use modified UTF8. At this time this is not implemented
             * the platform encoding is used.
             */
            platformLen = convertUft8ToPlatformString((char*)utf8Chars, utf8Len, platformChars, MAXPATHLEN);
            if (platformLen < 0) {
                createAndThrowInternalError(jnienv);
                (*jnienv)->ReleaseStringUTFChars(jnienv, jarFile, utf8Chars);
                return;
            }

            (*jnienv)->ReleaseStringUTFChars(jnienv, jarFile, utf8Chars);
            errorOutstanding = checkForAndClearThrowable(jnienv);

            if (!errorOutstanding) {

                if (isBootLoader) {
                    jvmtierror = (*jvmtienv)->AddToBootstrapClassLoaderSearch(jvmtienv, platformChars);
                } else {
                    jvmtierror = (*jvmtienv)->AddToSystemClassLoaderSearch(jvmtienv, platformChars);
                }
                check_phase_ret(jvmtierror);

                if ( jvmtierror != JVMTI_ERROR_NONE ) {
                    createAndThrowThrowableFromJVMTIErrorCode(jnienv, jvmtierror);
                }
            }
        }
    }

    mapThrownThrowableIfNecessary(jnienv, mapAllCheckedToInternalErrorMapper);
}

/*
 *  Set the prefixes used to wrap native methods (so they can be instrumented).
 *  Each transform can set a prefix, any that have been set come in as prefixArray.
 *  Convert them in native strings in a native array then call JVM TI.
 *  One a given call, this function handles either the prefixes for retransformable
 *  transforms or for normal transforms.
 */
void
setNativeMethodPrefixes(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray prefixArray,
                        jboolean isRetransformable) {
    jvmtiEnv*   jvmtienv;
    jvmtiError  err                             = JVMTI_ERROR_NONE;
    jsize       arraySize;
    jboolean    errorOccurred                   = JNI_FALSE;

    jplis_assert(prefixArray != NULL);

    if (isRetransformable) {
        jvmtienv = agent->mRetransformEnvironment.mJVMTIEnv;
    } else {
        jvmtienv = agent->mNormalEnvironment.mJVMTIEnv;
    }
    arraySize = (*jnienv)->GetArrayLength(jnienv, prefixArray);
    errorOccurred = checkForThrowable(jnienv);
    jplis_assert(!errorOccurred);

    if (!errorOccurred) {
        /* allocate the native to hold the native prefixes */
        const char** prefixes = (const char**) allocate(jvmtienv,
                                                        arraySize * sizeof(char*));
        /* since JNI ReleaseStringUTFChars needs the jstring from which the native
         * string was allocated, we store them in a parallel array */
        jstring* originForRelease = (jstring*) allocate(jvmtienv,
                                                        arraySize * sizeof(jstring));
        errorOccurred = (prefixes == NULL || originForRelease == NULL);
        jplis_assert(!errorOccurred);
        if ( errorOccurred ) {
            createAndThrowThrowableFromJVMTIErrorCode(jnienv, JVMTI_ERROR_OUT_OF_MEMORY);
        }
        else {
            jint inx = 0;
            jint i;
            for (i = 0; i < arraySize; i++) {
                jstring      prefixStr  = NULL;
                const char*  prefix;
                jsize        prefixLen;
                jboolean     isCopy;

                prefixStr = (jstring) ((*jnienv)->GetObjectArrayElement(jnienv,
                                                                        prefixArray, i));
                errorOccurred = checkForThrowable(jnienv);
                jplis_assert(!errorOccurred);
                if (errorOccurred) {
                    break;
                }
                if (prefixStr == NULL) {
                    continue;
                }

                prefixLen = (*jnienv)->GetStringUTFLength(jnienv, prefixStr);
                errorOccurred = checkForThrowable(jnienv);
                jplis_assert(!errorOccurred);
                if (errorOccurred) {
                    break;
                }

                if (prefixLen > 0) {
                    prefix = (*jnienv)->GetStringUTFChars(jnienv, prefixStr, &isCopy);
                    errorOccurred = checkForThrowable(jnienv);
                    jplis_assert(!errorOccurred);
                    if (!errorOccurred && prefix != NULL) {
                        prefixes[inx] = prefix;
                        originForRelease[inx] = prefixStr;
                        ++inx;
                    }
                }
            }

            err = (*jvmtienv)->SetNativeMethodPrefixes(jvmtienv, inx, (char**)prefixes);
            /* can be called from any phase */
            jplis_assert(err == JVMTI_ERROR_NONE);

            for (i = 0; i < inx; i++) {
              (*jnienv)->ReleaseStringUTFChars(jnienv, originForRelease[i], prefixes[i]);
            }
        }
        deallocate(jvmtienv, (void*)prefixes);
        deallocate(jvmtienv, (void*)originForRelease);
    }
}
