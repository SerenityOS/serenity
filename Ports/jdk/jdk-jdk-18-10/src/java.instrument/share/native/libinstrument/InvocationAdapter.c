/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include    <string.h>
#include    <stdlib.h>

#include    "jni.h"

#include    "jdk_util.h"

#include    "Utilities.h"
#include    "JPLISAssert.h"
#include    "JPLISAgent.h"
#include    "JavaExceptions.h"

#include    "EncodingSupport.h"
#include    "FileSystemSupport.h"
#include    "JarFacade.h"
#include    "PathCharsValidator.h"

/**
 * This module contains the direct interface points with the JVMTI.
 * The OnLoad handler is here, along with the various event handlers.
 */

static int
appendClassPath(JPLISAgent* agent,
                const char* jarfile);

static void
appendBootClassPath(JPLISAgent* agent,
                    const char* jarfile,
                    const char* pathList);


/*
 * Parse -javaagent tail, of the form name[=options], into name
 * and options. Returned values are heap allocated and options maybe
 * NULL. Returns 0 if parse succeeds, -1 if allocation fails.
 */
static int
parseArgumentTail(char* tail, char** name, char** options) {
    int len;
    char* pos;

    pos = strchr(tail, '=');
    len = (pos == NULL) ? (int)strlen(tail) : (int)(pos - tail);

    *name = (char*)malloc(len+1);
    if (*name == NULL) {
        return -1;
    }
    memcpy(*name, tail, len);
    (*name)[len] = '\0';

    if (pos == NULL) {
        *options = NULL;
    } else {
        char * str = (char*)malloc( (int)strlen(pos + 1) + 1 );
        if (str == NULL) {
            free(*name);
            return -1;
        }
        strcpy(str, pos +1);
        *options = str;
    }
    return 0;
}

/*
 * Get the value of an attribute in an attribute list. Returns NULL
 * if attribute not found.
 */
jboolean
getBooleanAttribute(const jarAttribute* attributes, const char* name) {
    char* attributeValue = getAttribute(attributes, name);
    return attributeValue != NULL && strcasecmp(attributeValue, "true") == 0;
}

/*
 * Parse any capability settings in the JAR manifest and
 * convert them to JVM TI capabilities.
 */
void
convertCapabilityAttributes(const jarAttribute* attributes, JPLISAgent* agent) {
    /* set redefineClasses capability */
    if (getBooleanAttribute(attributes, "Can-Redefine-Classes")) {
        addRedefineClassesCapability(agent);
    }

    /* create an environment which has the retransformClasses capability */
    if (getBooleanAttribute(attributes, "Can-Retransform-Classes")) {
        retransformableEnvironment(agent);
    }

    /* set setNativeMethodPrefix capability */
    if (getBooleanAttribute(attributes, "Can-Set-Native-Method-Prefix")) {
        addNativeMethodPrefixCapability(agent);
    }

    /* for retransformClasses testing, set capability to use original method order */
    if (getBooleanAttribute(attributes, "Can-Maintain-Original-Method-Order")) {
        addOriginalMethodOrderCapability(agent);
    }
}

/*
 *  This will be called once for every -javaagent on the command line.
 *  Each call to Agent_OnLoad will create its own agent and agent data.
 *
 *  The argument tail string provided to Agent_OnLoad will be of form
 *  <jarfile>[=<options>]. The tail string is split into the jarfile and
 *  options components. The jarfile manifest is parsed and the value of the
 *  Premain-Class attribute will become the agent's premain class. The jar
 *  file is then added to the system class path, and if the Boot-Class-Path
 *  attribute is present then all relative URLs in the value are processed
 *  to create boot class path segments to append to the boot class path.
 */
JNIEXPORT jint JNICALL
DEF_Agent_OnLoad(JavaVM *vm, char *tail, void * reserved) {
    JPLISInitializationError initerror  = JPLIS_INIT_ERROR_NONE;
    jint                     result     = JNI_OK;
    JPLISAgent *             agent      = NULL;

    initerror = createNewJPLISAgent(vm, &agent);
    if ( initerror == JPLIS_INIT_ERROR_NONE ) {
        int             oldLen, newLen;
        char *          jarfile;
        char *          options;
        jarAttribute*   attributes;
        char *          premainClass;
        char *          bootClassPath;

        /*
         * Parse <jarfile>[=options] into jarfile and options
         */
        if (parseArgumentTail(tail, &jarfile, &options) != 0) {
            fprintf(stderr, "-javaagent: memory allocation failure.\n");
            return JNI_ERR;
        }

        /*
         * Agent_OnLoad is specified to provide the agent options
         * argument tail in modified UTF8. However for 1.5.0 this is
         * actually in the platform encoding - see 5049313.
         *
         * Open zip/jar file and parse archive. If can't be opened or
         * not a zip file return error. Also if Premain-Class attribute
         * isn't present we return an error.
         */
        attributes = readAttributes(jarfile);
        if (attributes == NULL) {
            fprintf(stderr, "Error opening zip file or JAR manifest missing : %s\n", jarfile);
            free(jarfile);
            if (options != NULL) free(options);
            return JNI_ERR;
        }

        premainClass = getAttribute(attributes, "Premain-Class");
        if (premainClass == NULL) {
            fprintf(stderr, "Failed to find Premain-Class manifest attribute in %s\n",
                jarfile);
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return JNI_ERR;
        }

        /* Save the jarfile name */
        agent->mJarfile = jarfile;

        /*
         * The value of the Premain-Class attribute becomes the agent
         * class name. The manifest is in UTF8 so need to convert to
         * modified UTF8 (see JNI spec).
         */
        oldLen = (int)strlen(premainClass);
        newLen = modifiedUtf8LengthOfUtf8(premainClass, oldLen);
        /*
         * According to JVMS class name is represented as CONSTANT_Utf8_info,
         * so its length is u2 (i.e. must be <= 0xFFFF).
         * Negative oldLen or newLen means we got signed integer overflow
         * (modifiedUtf8LengthOfUtf8 returns negative value if oldLen is negative).
         */
        if (oldLen < 0 || newLen < 0 || newLen > 0xFFFF) {
            fprintf(stderr, "-javaagent: Premain-Class value is too big\n");
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return JNI_ERR;
        }
        if (newLen == oldLen) {
            premainClass = strdup(premainClass);
        } else {
            char* str = (char*)malloc( newLen+1 );
            if (str != NULL) {
                convertUtf8ToModifiedUtf8(premainClass, oldLen, str, newLen);
            }
            premainClass = str;
        }
        if (premainClass == NULL) {
            fprintf(stderr, "-javaagent: memory allocation failed\n");
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return JNI_ERR;
        }

        /*
         * If the Boot-Class-Path attribute is specified then we process
         * each relative URL and add it to the bootclasspath.
         */
        bootClassPath = getAttribute(attributes, "Boot-Class-Path");
        if (bootClassPath != NULL) {
            appendBootClassPath(agent, jarfile, bootClassPath);
        }

        /*
         * Convert JAR attributes into agent capabilities
         */
        convertCapabilityAttributes(attributes, agent);

        /*
         * Track (record) the agent class name and options data
         */
        initerror = recordCommandLineData(agent, premainClass, options);

        /*
         * Clean-up
         */
        if (options != NULL) free(options);
        freeAttributes(attributes);
        free(premainClass);
    }

    switch (initerror) {
    case JPLIS_INIT_ERROR_NONE:
      result = JNI_OK;
      break;
    case JPLIS_INIT_ERROR_CANNOT_CREATE_NATIVE_AGENT:
      result = JNI_ERR;
      fprintf(stderr, "java.lang.instrument/-javaagent: cannot create native agent.\n");
      break;
    case JPLIS_INIT_ERROR_FAILURE:
      result = JNI_ERR;
      fprintf(stderr, "java.lang.instrument/-javaagent: initialization of native agent failed.\n");
      break;
    case JPLIS_INIT_ERROR_ALLOCATION_FAILURE:
      result = JNI_ERR;
      fprintf(stderr, "java.lang.instrument/-javaagent: allocation failure.\n");
      break;
    case JPLIS_INIT_ERROR_AGENT_CLASS_NOT_SPECIFIED:
      result = JNI_ERR;
      fprintf(stderr, "-javaagent: agent class not specified.\n");
      break;
    default:
      result = JNI_ERR;
      fprintf(stderr, "java.lang.instrument/-javaagent: unknown error\n");
      break;
    }
    return result;
}

/*
 * Agent_OnAttach returns a jint. 0/JNI_OK indicates success and non-0
 * indicates an error. To allow the attach mechanism throw an
 * AgentInitializationException with a reasonable exception message we define
 * a few specific errors here.
 */
#define AGENT_ERROR_BADJAR    ((jint)100)  /* Agent JAR not found or no Agent-Class attribute */
#define AGENT_ERROR_NOTONCP   ((jint)101)  /* Unable to add JAR file to system class path */
#define AGENT_ERROR_STARTFAIL ((jint)102)  /* No agentmain method or agentmain failed */

/*
 *  This will be called once each time a tool attaches to the VM and loads
 *  the JPLIS library.
 */
JNIEXPORT jint JNICALL
DEF_Agent_OnAttach(JavaVM* vm, char *args, void * reserved) {
    JPLISInitializationError initerror  = JPLIS_INIT_ERROR_NONE;
    jint                     result     = JNI_OK;
    JPLISAgent *             agent      = NULL;
    JNIEnv *                 jni_env    = NULL;

    /*
     * Need JNIEnv - guaranteed to be called from thread that is already
     * attached to VM
     */
    result = (*vm)->GetEnv(vm, (void**)&jni_env, JNI_VERSION_1_2);
    jplis_assert(result==JNI_OK);

    initerror = createNewJPLISAgent(vm, &agent);
    if ( initerror == JPLIS_INIT_ERROR_NONE ) {
        int             oldLen, newLen;
        char *          jarfile;
        char *          options;
        jarAttribute*   attributes;
        char *          agentClass;
        char *          bootClassPath;
        jboolean        success;

        /*
         * Parse <jarfile>[=options] into jarfile and options
         */
        if (parseArgumentTail(args, &jarfile, &options) != 0) {
            return JNI_ENOMEM;
        }

        /*
         * Open the JAR file and parse the manifest
         */
        attributes = readAttributes( jarfile );
        if (attributes == NULL) {
            fprintf(stderr, "Error opening zip file or JAR manifest missing: %s\n", jarfile);
            free(jarfile);
            if (options != NULL) free(options);
            return AGENT_ERROR_BADJAR;
        }

        agentClass = getAttribute(attributes, "Agent-Class");
        if (agentClass == NULL) {
            fprintf(stderr, "Failed to find Agent-Class manifest attribute from %s\n",
                jarfile);
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return AGENT_ERROR_BADJAR;
        }

        /*
         * Add the jarfile to the system class path
         */
        if (appendClassPath(agent, jarfile)) {
            fprintf(stderr, "Unable to add %s to system class path "
                "- not supported by system class loader or configuration error!\n",
                jarfile);
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return AGENT_ERROR_NOTONCP;
        }

        /*
         * The value of the Agent-Class attribute becomes the agent
         * class name. The manifest is in UTF8 so need to convert to
         * modified UTF8 (see JNI spec).
         */
        oldLen = (int)strlen(agentClass);
        newLen = modifiedUtf8LengthOfUtf8(agentClass, oldLen);
        /*
         * According to JVMS class name is represented as CONSTANT_Utf8_info,
         * so its length is u2 (i.e. must be <= 0xFFFF).
         * Negative oldLen or newLen means we got signed integer overflow
         * (modifiedUtf8LengthOfUtf8 returns negative value if oldLen is negative).
         */
        if (oldLen < 0 || newLen < 0 || newLen > 0xFFFF) {
            fprintf(stderr, "Agent-Class value is too big\n");
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return AGENT_ERROR_BADJAR;
        }
        if (newLen == oldLen) {
            agentClass = strdup(agentClass);
        } else {
            char* str = (char*)malloc( newLen+1 );
            if (str != NULL) {
                convertUtf8ToModifiedUtf8(agentClass, oldLen, str, newLen);
            }
            agentClass = str;
        }
        if (agentClass == NULL) {
            free(jarfile);
            if (options != NULL) free(options);
            freeAttributes(attributes);
            return JNI_ENOMEM;
        }

        /*
         * If the Boot-Class-Path attribute is specified then we process
         * each URL - in the live phase only JAR files will be added.
         */
        bootClassPath = getAttribute(attributes, "Boot-Class-Path");
        if (bootClassPath != NULL) {
            appendBootClassPath(agent, jarfile, bootClassPath);
        }

        /*
         * Convert JAR attributes into agent capabilities
         */
        convertCapabilityAttributes(attributes, agent);

        /*
         * Create the java.lang.instrument.Instrumentation instance
         */
        success = createInstrumentationImpl(jni_env, agent);
        jplis_assert(success);

        /*
         * Setup ClassFileLoadHook handler.
         */
        if (success) {
            success = setLivePhaseEventHandlers(agent);
            jplis_assert(success);
        }

        /*
         * Start the agent
         */
        if (success) {
            success = startJavaAgent(agent,
                                     jni_env,
                                     agentClass,
                                     options,
                                     agent->mAgentmainCaller);
        }

        if (!success) {
            fprintf(stderr, "Agent failed to start!\n");
            result = AGENT_ERROR_STARTFAIL;
        }

        /*
         * Clean-up
         */
        free(jarfile);
        if (options != NULL) free(options);
        free(agentClass);
        freeAttributes(attributes);
    }

    return result;
}


JNIEXPORT void JNICALL
DEF_Agent_OnUnload(JavaVM *vm) {
}

/**
 * Invoked by the java launcher to load an agent in the main executable JAR.
 * The Launcher-Agent-Class attribute in the main manifest of the JAR file
 * is the agent class.
 *
 * Returns JNI_OK if the agent is loaded and initialized; JNI_ERR if this
 * function fails, possibly with a pending exception.
 */
jint loadAgent(JNIEnv* env, jstring path) {
    JavaVM* vm;
    JPLISAgent* agent;
    const char* jarfile = NULL;
    jarAttribute* attributes = NULL;
    char* agentClass = NULL;
    char* bootClassPath;
    int oldLen, newLen;
    jint result = JNI_ERR;

    if ((*env)->GetJavaVM(env, &vm) < 0) {
        return JNI_ERR;
    }

    // create JPLISAgent with JVMTI environment
    if (createNewJPLISAgent(vm, &agent) != JPLIS_INIT_ERROR_NONE) {
        return JNI_ERR;
    }

    // get path to JAR file as UTF-8 string
    jarfile = (*env)->GetStringUTFChars(env, path, NULL);
    if (jarfile == NULL) {
        return JNI_ERR;
    }

    // read the attributes in the main section of JAR manifest
    attributes = readAttributes(jarfile);
    if (attributes == NULL) {
        goto releaseAndReturn;
    }

    // Launcher-Agent-Class is required
    agentClass = getAttribute(attributes, "Launcher-Agent-Class");
    if (agentClass == NULL) {
        goto releaseAndReturn;
    }

    // The value of Launcher-Agent-Class is in UTF-8, convert it to modified UTF-8
    oldLen = (int) strlen(agentClass);
    newLen = modifiedUtf8LengthOfUtf8(agentClass, oldLen);
    /*
     * According to JVMS class name is represented as CONSTANT_Utf8_info,
     * so its length is u2 (i.e. must be <= 0xFFFF).
     * Negative oldLen or newLen means we got signed integer overflow
     * (modifiedUtf8LengthOfUtf8 returns negative value if oldLen is negative).
     */
    if (oldLen < 0 || newLen < 0 || newLen > 0xFFFF) {
        goto releaseAndReturn;
    }
    if (newLen == oldLen) {
        agentClass = strdup(agentClass);
    } else {
        char* str = (char*) malloc(newLen + 1);
        if (str != NULL) {
            convertUtf8ToModifiedUtf8(agentClass, oldLen, str, newLen);
        }
        agentClass = str;
    }
    if (agentClass == NULL) {
         jthrowable oome = createThrowable(env, "java/lang/OutOfMemoryError", NULL);
         if (oome != NULL) (*env)->Throw(env, oome);
         goto releaseAndReturn;
    }

    // Boot-Class-Path
    bootClassPath = getAttribute(attributes, "Boot-Class-Path");
    if (bootClassPath != NULL) {
        appendBootClassPath(agent, jarfile, bootClassPath);
    }

    // Can-XXXX capabilities
    convertCapabilityAttributes(attributes, agent);

    // Create the java.lang.instrument.Instrumentation object
    if (!createInstrumentationImpl(env, agent)) {
        goto releaseAndReturn;
    }

    // Enable the ClassFileLoadHook
    if (!setLivePhaseEventHandlers(agent)) {
        goto releaseAndReturn;
    }

    // invoke the agentmain method
    if (!startJavaAgent(agent, env, agentClass, "", agent->mAgentmainCaller)) {
        goto releaseAndReturn;
    }

    // initialization complete
    result = JNI_OK;

releaseAndReturn:
    if (agentClass != NULL) {
        free(agentClass);
    }
    if (attributes != NULL) {
        freeAttributes(attributes);
    }
    if (jarfile != NULL) {
        (*env)->ReleaseStringUTFChars(env, path, jarfile);
    }

    return result;
}

/*
 *  JVMTI callback support
 *
 *  We have two "stages" of callback support.
 *  At OnLoad time, we install a VMInit handler.
 *  When the VMInit handler runs, we remove the VMInit handler and install a
 *  ClassFileLoadHook handler.
 */

void JNICALL
eventHandlerVMInit( jvmtiEnv *      jvmtienv,
                    JNIEnv *        jnienv,
                    jthread         thread) {
    JPLISEnvironment * environment  = NULL;
    jboolean           success      = JNI_FALSE;

    environment = getJPLISEnvironment(jvmtienv);

    /* process the premain calls on the all the JPL agents */
    if (environment == NULL) {
        abortJVM(jnienv, JPLIS_ERRORMESSAGE_CANNOTSTART ", getting JPLIS environment failed");
    }
    jthrowable outstandingException = NULL;
    /*
     * Add the jarfile to the system class path
     */
    JPLISAgent * agent = environment->mAgent;
    if (appendClassPath(agent, agent->mJarfile)) {
        fprintf(stderr, "Unable to add %s to system class path - "
                "the system class loader does not define the "
                "appendToClassPathForInstrumentation method or the method failed\n",
                agent->mJarfile);
        free((void *)agent->mJarfile);
        abortJVM(jnienv, JPLIS_ERRORMESSAGE_CANNOTSTART ", appending to system class path failed");
    }
    free((void *)agent->mJarfile);
    agent->mJarfile = NULL;

    outstandingException = preserveThrowable(jnienv);
    success = processJavaStart( environment->mAgent, jnienv);
    restoreThrowable(jnienv, outstandingException);

    /* if we fail to start cleanly, bring down the JVM */
    if ( !success ) {
        abortJVM(jnienv, JPLIS_ERRORMESSAGE_CANNOTSTART ", processJavaStart failed");
    }
}

void JNICALL
eventHandlerClassFileLoadHook(  jvmtiEnv *              jvmtienv,
                                JNIEnv *                jnienv,
                                jclass                  class_being_redefined,
                                jobject                 loader,
                                const char*             name,
                                jobject                 protectionDomain,
                                jint                    class_data_len,
                                const unsigned char*    class_data,
                                jint*                   new_class_data_len,
                                unsigned char**         new_class_data) {
    JPLISEnvironment * environment  = NULL;

    environment = getJPLISEnvironment(jvmtienv);

    /* if something is internally inconsistent (no agent), just silently return without touching the buffer */
    if ( environment != NULL ) {
        jthrowable outstandingException = preserveThrowable(jnienv);
        transformClassFile( environment->mAgent,
                            jnienv,
                            loader,
                            name,
                            class_being_redefined,
                            protectionDomain,
                            class_data_len,
                            class_data,
                            new_class_data_len,
                            new_class_data,
                            environment->mIsRetransformer);
        restoreThrowable(jnienv, outstandingException);
    }
}




/*
 * URLs in Boot-Class-Path attributes are separated by one or more spaces.
 * This function splits the attribute value into a list of path segments.
 * The attribute value is in UTF8 but cannot contain NUL. Also non US-ASCII
 * characters must be escaped (URI syntax) so safe to iterate through the
 * value as a C string.
 */
static void
splitPathList(const char* str, int* pathCount, char*** paths) {
    int count = 0;
    char** segments = NULL;
    char** new_segments;
    char* c = (char*) str;
    while (*c != '\0') {
        while (*c == ' ') c++;          /* skip leading spaces */
        if (*c == '\0') {
            break;
        }
        new_segments = (char**)realloc(segments, (count+1)*sizeof(char*));
        if (new_segments == NULL) {
            jplis_assert(0);
            free(segments);
            count = 0;
            segments = NULL;
            break;
        }
        segments = new_segments;
        segments[count++] = c;
        c = strchr(c, ' ');
        if (c == NULL) {
            break;
        }
        *c = '\0';
        c++;
    }
    *pathCount = count;
    *paths = segments;
}


/* URI path decoding - ported from src/share/classes/java/net/URI.java */

static int
decodeNibble(char c) {
    if ((c >= '0') && (c <= '9'))
        return c - '0';
    if ((c >= 'a') && (c <= 'f'))
        return c - 'a' + 10;
    if ((c >= 'A') && (c <= 'F'))
        return c - 'A' + 10;
    return -1;
}

static int
decodeByte(char c1, char c2) {
    return (((decodeNibble(c1) & 0xf) << 4) | ((decodeNibble(c2) & 0xf) << 0));
}

/*
 * Evaluates all escapes in s.  Assumes that escapes are well-formed
 * syntactically, i.e., of the form %XX.
 * If the path does not require decoding the original path is
 * returned. Otherwise the decoded path (heap allocated) is returned,
 * along with the length of the decoded path. Note that the return
 * string will not be null terminated after decoding.
 */
static
char *decodePath(const char *s, int* decodedLen) {
    int n;
    char *result;
    char *resultp;
    int c;
    int i;

    n = (int)strlen(s);
    if (n == 0) {
        *decodedLen = 0;
        return (char*)s;
    }
    if (strchr(s, '%') == NULL) {
        *decodedLen = n;
        return (char*)s; /* no escapes, we are done */
    }

    resultp = result = calloc(n+1, 1);
    if (result == NULL) {
        *decodedLen = 0;
        return NULL;
    }
    c = s[0];
    for (i = 0; i < n;) {
        if (c != '%') {
            *resultp++ = c;
            if (++i >= n)
                break;
            c = s[i];
            continue;
        }
        for (;;) {
            char b1 = s[++i];
            char b2 = s[++i];
            int decoded = decodeByte(b1, b2);
            *resultp++ = decoded;
            if (++i >= n)
                break;
            c = s[i];
            if (c != '%')
                break;
        }
    }
    *decodedLen = (int)(resultp - result);
    return result; // not null terminated.
}

/*
 * Append the given jar file to the system class path. This should succeed in the
 * onload phase but may fail in the live phase if the system class loader doesn't
 * support appending to the class path.
 */
static int
appendClassPath( JPLISAgent* agent,
                 const char* jarfile ) {
    jvmtiEnv* jvmtienv = jvmti(agent);
    jvmtiError jvmtierr;

    jvmtierr = (*jvmtienv)->AddToSystemClassLoaderSearch(jvmtienv, jarfile);
    check_phase_ret_1(jvmtierr);

    switch (jvmtierr) {
        case JVMTI_ERROR_NONE :
            return 0;
        case JVMTI_ERROR_CLASS_LOADER_UNSUPPORTED :
            fprintf(stderr, "System class loader does not define "
                "the appendToClassPathForInstrumentation method\n");
            break;
        default:
            fprintf(stderr, "Unexpected error (%d) returned by "
                "AddToSystemClassLoaderSearch\n", jvmtierr);
            break;
    }
    return -1;
}


/*
 * res = func, free'ing the previous value of 'res' if function
 * returns a new result.
 */
#define TRANSFORM(res,func) {    \
    char* tmp = func;            \
    if (tmp != res) {            \
        free(res);               \
        res = tmp;               \
    }                            \
    jplis_assert((void*)res != (void*)NULL);     \
}

/*
 * This function takes the value of the Boot-Class-Path attribute,
 * splits it into the individual path segments, and then combines it
 * with the path to the jar file to create the path to be added
 * to the bootclasspath.
 *
 * Each individual path segment starts out as a UTF8 string. Additionally
 * as the path is specified to use URI path syntax all non US-ASCII
 * characters are escaped. Once the URI path is decoded we get a UTF8
 * string which must then be converted to the platform encoding (as it
 * will be combined with the platform path of the jar file). Once
 * converted it is then normalized (remove duplicate slashes, etc.).
 * If the resulting path is an absolute path (starts with a slash for
 * example) then the path will be added to the bootclasspath. Otherwise
 * if it's not absolute then we get the canoncial path of the agent jar
 * file and then resolve the path in the context of the base path of
 * the agent jar.
 */
static void
appendBootClassPath( JPLISAgent* agent,
                     const char* jarfile,
                     const char* pathList ) {
    char canonicalPath[MAXPATHLEN];
    char *parent = NULL;
    int haveBasePath = 0;

    int count, i;
    char **paths;
    jvmtiEnv* jvmtienv = jvmti(agent);
    jvmtiError jvmtierr;

    /*
     * Split the attribute value into the individual path segments
     * and process each in sequence
     */
    splitPathList(pathList, &count, &paths);

    for (i=0; i<count; i++) {
        int len;
        char* path;
        char* pos;

        /*
         * The path segment at this point is a pointer into the attribute
         * value. As it will go through a number of transformation (tossing away
         * the previous results as we go along) it make it easier if the path
         * starts out as a heap allocated string.
         */
        path = strdup(paths[i]);
        jplis_assert(path != (char*)NULL);

        /*
         * The attribute is specified to be a list of relative URIs so in theory
         * there could be a query component - if so, get rid of it.
         */
        pos = strchr(path, '?');
        if (pos != NULL) {
            *pos = '\0';
        }

        /*
         * Check for characters that are not allowed in the path component of
         * a URI.
         */
        if (validatePathChars(path)) {
            fprintf(stderr, "WARNING: illegal character in Boot-Class-Path value: %s\n",
               path);
            free(path);
            continue;
        }


        /*
         * Next decode any escaped characters. The result is a UTF8 string.
         */
        TRANSFORM(path, decodePath(path,&len));

        /*
         * Convert to the platform encoding
         */
        {
            char platform[MAXPATHLEN];
            int new_len = convertUft8ToPlatformString(path, len, platform, MAXPATHLEN);
            free(path);
            if (new_len  < 0) {
                /* bogus value - exceeds maximum path size or unable to convert */
                continue;
            }
            path = strdup(platform);
            jplis_assert(path != (char*)NULL);
        }

        /*
         * Post-process the URI path - needed on Windows to transform
         * /c:/foo to c:/foo.
         */
        TRANSFORM(path, fromURIPath(path));

        /*
         * Normalize the path - no duplicate slashes (except UNCs on Windows), trailing
         * slash removed.
         */
        TRANSFORM(path, normalize(path));

        /*
         * If the path is an absolute path then add to the bootclassloader
         * search path. Otherwise we get the canonical path of the agent jar
         * and then use its base path (directory) to resolve the given path
         * segment.
         *
         * NOTE: JVMTI is specified to use modified UTF8 strings (like JNI).
         * In 1.5.0 the AddToBootstrapClassLoaderSearch takes a platform string
         * - see 5049313.
         */
        if (isAbsolute(path)) {
            jvmtierr = (*jvmtienv)->AddToBootstrapClassLoaderSearch(jvmtienv, path);
        } else {
            char* resolved;

            if (!haveBasePath) {
                if (JDK_Canonicalize((char*)jarfile, canonicalPath, sizeof(canonicalPath)) != 0) {
                    fprintf(stderr, "WARNING: unable to canonicalize %s\n", jarfile);
                    free(path);
                    continue;
                }
                parent = basePath(canonicalPath);
                jplis_assert(parent != (char*)NULL);
                haveBasePath = 1;
            }

            resolved = resolve(parent, path);
            jvmtierr = (*jvmtienv)->AddToBootstrapClassLoaderSearch(jvmtienv, resolved);
            free(resolved);
        }

        /* print warning if boot class path not updated */
        if (jvmtierr != JVMTI_ERROR_NONE) {
            check_phase_blob_ret(jvmtierr, free(path));

            fprintf(stderr, "WARNING: %s not added to bootstrap class loader search: ", path);
            switch (jvmtierr) {
                case JVMTI_ERROR_ILLEGAL_ARGUMENT :
                    fprintf(stderr, "Illegal argument or not JAR file\n");
                    break;
                default:
                    fprintf(stderr, "Unexpected error: %d\n", jvmtierr);
            }
        }

        /* finished with the path */
        free(path);
    }


    /* clean-up */
    if (haveBasePath && parent != canonicalPath) {
        free(parent);
    }
}
