/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JPLISAGENT_H_
#define _JPLISAGENT_H_

#include    <jni.h>
#include    <jvmti.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 *  The JPLISAgent manages the initialization all of the Java programming language Agents.
 *  It also supports the native method bridge between the JPLIS and the JVMTI.
 *  It maintains a single JVMTI Env that all JPL agents share.
 *  It parses command line requests and creates individual Java agents.
 */


/*
 *  Forward definitions
 */
struct  _JPLISAgent;

typedef struct _JPLISAgent        JPLISAgent;
typedef struct _JPLISEnvironment  JPLISEnvironment;


/* constants for class names and methods names and such
    these all must stay in sync with Java code & interfaces
*/
#define JPLIS_INSTRUMENTIMPL_CLASSNAME                      "sun/instrument/InstrumentationImpl"
#define JPLIS_INSTRUMENTIMPL_CONSTRUCTOR_METHODNAME         "<init>"
#define JPLIS_INSTRUMENTIMPL_CONSTRUCTOR_METHODSIGNATURE    "(JZZ)V"
#define JPLIS_INSTRUMENTIMPL_PREMAININVOKER_METHODNAME      "loadClassAndCallPremain"
#define JPLIS_INSTRUMENTIMPL_PREMAININVOKER_METHODSIGNATURE "(Ljava/lang/String;Ljava/lang/String;)V"
#define JPLIS_INSTRUMENTIMPL_AGENTMAININVOKER_METHODNAME      "loadClassAndCallAgentmain"
#define JPLIS_INSTRUMENTIMPL_AGENTMAININVOKER_METHODSIGNATURE "(Ljava/lang/String;Ljava/lang/String;)V"
#define JPLIS_INSTRUMENTIMPL_TRANSFORM_METHODNAME           "transform"
#define JPLIS_INSTRUMENTIMPL_TRANSFORM_METHODSIGNATURE      \
    "(Ljava/lang/Module;Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;Ljava/security/ProtectionDomain;[BZ)[B"


/*
 *  Error messages
 */
#define JPLIS_ERRORMESSAGE_CANNOTSTART              "processing of -javaagent failed"


/*
 *  Our initialization errors
 */
typedef enum {
  JPLIS_INIT_ERROR_NONE,
  JPLIS_INIT_ERROR_CANNOT_CREATE_NATIVE_AGENT,
  JPLIS_INIT_ERROR_FAILURE,
  JPLIS_INIT_ERROR_ALLOCATION_FAILURE,
  JPLIS_INIT_ERROR_AGENT_CLASS_NOT_SPECIFIED
} JPLISInitializationError;


struct _JPLISEnvironment {
    jvmtiEnv *              mJVMTIEnv;              /* the JVM TI environment */
    JPLISAgent *            mAgent;                 /* corresponding agent */
    jboolean                mIsRetransformer;       /* indicates if special environment */
};

struct _JPLISAgent {
    JavaVM *                mJVM;                   /* handle to the JVM */
    JPLISEnvironment        mNormalEnvironment;     /* for every thing but retransform stuff */
    JPLISEnvironment        mRetransformEnvironment;/* for retransform stuff only */
    jobject                 mInstrumentationImpl;   /* handle to the Instrumentation instance */
    jmethodID               mPremainCaller;         /* method on the InstrumentationImpl that does the premain stuff (cached to save lots of lookups) */
    jmethodID               mAgentmainCaller;       /* method on the InstrumentationImpl for agents loaded via attach mechanism */
    jmethodID               mTransform;             /* method on the InstrumentationImpl that does the class file transform */
    jboolean                mRedefineAvailable;     /* cached answer to "does this agent support redefine" */
    jboolean                mRedefineAdded;         /* indicates if can_redefine_classes capability has been added */
    jboolean                mNativeMethodPrefixAvailable; /* cached answer to "does this agent support prefixing" */
    jboolean                mNativeMethodPrefixAdded;     /* indicates if can_set_native_method_prefix capability has been added */
    char const *            mAgentClassName;        /* agent class name */
    char const *            mOptionsString;         /* -javaagent options string */
    const char *            mJarfile;               /* agent jar file name */
};

/*
 * JVMTI event handlers
 */

/* VMInit event handler. Installed during OnLoad, then removed during VMInit. */
extern void JNICALL
eventHandlerVMInit( jvmtiEnv *      jvmtienv,
                    JNIEnv *        jnienv,
                    jthread         thread);

/*
 * ClassFileLoadHook event handler.
 * Enabled when the first transformer is added;
 * Disabled when the last transformer is removed.
 */
extern void JNICALL
eventHandlerClassFileLoadHook(  jvmtiEnv *              jvmtienv,
                                JNIEnv *                jnienv,
                                jclass                  class_being_redefined,
                                jobject                 loader,
                                const char*             name,
                                jobject                 protectionDomain,
                                jint                    class_data_len,
                                const unsigned char*    class_data,
                                jint*                   new_class_data_len,
                                unsigned char**         new_class_data);

/*
 * Main entry points for the JPLIS JVMTI agent code
 */

/* looks up the  environment instance. returns null if there isn't one */
extern JPLISEnvironment *
getJPLISEnvironment(jvmtiEnv * jvmtienv);

/*  Creates a new JPLIS agent.
 *  Returns error if the agent cannot be created and initialized.
 *  The JPLISAgent* pointed to by agent_ptr is set to the new broker,
 *  or NULL if an error has occurred.
 */
extern JPLISInitializationError
createNewJPLISAgent(JavaVM * vm, JPLISAgent **agent_ptr);

/* Adds can_redefine_classes capability */
extern void
addRedefineClassesCapability(JPLISAgent * agent);

/* Add the can_set_native_method_prefix capability */
extern void
addNativeMethodPrefixCapability(JPLISAgent * agent);

/* Add the can_maintain_original_method_order capability (for testing) */
extern void
addOriginalMethodOrderCapability(JPLISAgent * agent);


/* Our JPLIS agent is paralleled by a Java InstrumentationImpl instance.
 * This routine uses JNI to create and initialized the Java instance.
 * Returns true if it succeeds, false otherwise.
 */
extern jboolean
createInstrumentationImpl( JNIEnv *        jnienv,
                           JPLISAgent *    agent);


/* during OnLoad phase (command line parsing)
 *  record the parameters of -javaagent
 */
extern JPLISInitializationError
recordCommandLineData(  JPLISAgent *    agent,
                        const char *    agentClass,
                        const char *    optionsString );

/* Swaps the start phase event handlers out and the live phase event handlers in.
 * Also used in attach to enabled live phase event handlers.
 * Returns true if it succeeds, false otherwise.
 */
extern jboolean
setLivePhaseEventHandlers(  JPLISAgent * agent);

/* Loads the Java agent according to the already processed command line. For each,
 * loads the Java agent class, then calls the premain method.
 * Returns true if all Java agent classes are loaded and all premain methods complete with no exceptions,
 * false otherwise.
 */
extern jboolean
startJavaAgent( JPLISAgent *    agent,
                JNIEnv *        jnienv,
                const char *    classname,
                const char *    optionsString,
                jmethodID       agentMainMethod);


/* during VMInit processing
 *  this is how the invocation engine (callback wrapper) tells us to start up all the javaagents
 */
extern jboolean
processJavaStart(   JPLISAgent *    agent,
                    JNIEnv *        jnienv);

/* on an ongoing basis,
 *  this is how the invocation engine (callback wrapper) tells us to process a class file
 */
extern void
transformClassFile(             JPLISAgent *            agent,
                                JNIEnv *                jnienv,
                                jobject                 loader,
                                const char*             name,
                                jclass                  classBeingRedefined,
                                jobject                 protectionDomain,
                                jint                    class_data_len,
                                const unsigned char*    class_data,
                                jint*                   new_class_data_len,
                                unsigned char**         new_class_data,
                                jboolean                is_retransformer);

/* on an ongoing basis,
 *  Return the environment with the retransformation capability.
 *  Create it if it doesn't exist.
 */
extern jvmtiEnv *
retransformableEnvironment(JPLISAgent * agent);

/* on an ongoing basis,
 *  these are implementations of the Instrumentation services.
 *  Most are simple covers for JVMTI access services. These are the guts of the InstrumentationImpl
 *  native methods.
 */
extern jboolean
isModifiableClass(JNIEnv * jnienv, JPLISAgent * agent, jclass clazz);

extern jboolean
isRetransformClassesSupported(JNIEnv * jnienv, JPLISAgent * agent);

extern void
setHasTransformers(JNIEnv * jnienv, JPLISAgent * agent, jboolean has);

extern void
setHasRetransformableTransformers(JNIEnv * jnienv, JPLISAgent * agent, jboolean has);

extern void
retransformClasses(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray classes);

extern void
redefineClasses(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray classDefinitions);

extern jobjectArray
getAllLoadedClasses(JNIEnv * jnienv, JPLISAgent * agent);

extern jobjectArray
getInitiatedClasses(JNIEnv * jnienv, JPLISAgent * agent, jobject classLoader);

extern jlong
getObjectSize(JNIEnv * jnienv, JPLISAgent * agent, jobject objectToSize);

extern void
appendToClassLoaderSearch(JNIEnv * jnienv, JPLISAgent * agent, jstring jarFile, jboolean isBootLoader);

extern void
setNativeMethodPrefixes(JNIEnv * jnienv, JPLISAgent * agent, jobjectArray prefixArray,
                        jboolean isRetransformable);

#define jvmti(a) a->mNormalEnvironment.mJVMTIEnv

/*
 * A set of macros for insulating the JLI method callers from
 * JVMTI_ERROR_WRONG_PHASE return codes.
 */

/* for a JLI method where "blob" is executed before simply returning */
#define check_phase_blob_ret(ret, blob)      \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        blob;                                \
        return;                              \
    }

/* for a JLI method where simply returning is benign */
#define check_phase_ret(ret)                 \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        return;                              \
    }

/* for a JLI method where returning zero (0) is benign */
#define check_phase_ret_0(ret)               \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        return 0;                            \
    }

/* for a JLI method where returning one (1) is benign */
#define check_phase_ret_1(ret)               \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        return 1;                            \
    }

/* for a case where a specific "blob" must be returned */
#define check_phase_ret_blob(ret, blob)      \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        return (blob);                       \
    }

/* for a JLI method where returning false is benign */
#define check_phase_ret_false(ret)           \
    if ((ret) == JVMTI_ERROR_WRONG_PHASE) {  \
        return (jboolean) 0;                 \
    }

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */


#endif
