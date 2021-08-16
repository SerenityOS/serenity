/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

#ifndef NSK_JVMTI_TOOLS_DEFINED
#define NSK_JVMTI_TOOLS_DEFINED

/*************************************************************/

#include "jvmti.h"

/*************************************************************/

#include "nsk_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"


extern "C" {


/******************** Diagnostics errors *********************/

/**
 * Call JVMTI function in action, check error code to be
 * JVMTI_ERROR_NONE and complain error otherwise.
 * Also trace action execution if tracing mode is on.
 */
#define NSK_JVMTI_VERIFY(action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        nsk_jvmti_lverify(NSK_TRUE,action,JVMTI_ERROR_NONE, \
                            __FILE__,__LINE__,"%s\n",#action))

/**
 * Call JVMTI function in action, check error code to be
 * not JVMTI_ERROR_NONE and complain error otherwise.
 * Also trace action execution if tracing mode is on.
 */
#define NSK_JVMTI_VERIFY_NEGATIVE(action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        nsk_jvmti_lverify(NSK_FALSE,action,JVMTI_ERROR_NONE, \
                            __FILE__,__LINE__,"%s\n",#action))

/**
 * Call JVMTI function in action, check error code to be
 * equal to 'code' and complain error otherwise.
 * Also trace action execution if tracing mode is on.
 */
#define NSK_JVMTI_VERIFY_CODE(code, action)  \
    (nsk_ltrace(NSK_TRACE_BEFORE,__FILE__,__LINE__,"%s\n",#action), \
        nsk_jvmti_lverify(NSK_TRUE,action,code,__FILE__,__LINE__,"%s\n",#action))


/********************* Initialization ************************/

/**
 * Initialize framework and setup command line options for the JVMTI test.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_parseOptions(const char options[]);

/**
 * Creates JVMTI environment for the JVMTI test.
 * If something fails, complains an error and returns NULL.
 */
jvmtiEnv* nsk_jvmti_createJVMTIEnv(JavaVM* jvm, void* reserved);

/**
 * Register function to be run in agent thread.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_setAgentProc(jvmtiStartFunction proc, void* arg);

/**
 * Initialize multiple agent
 */
int nsk_jvmti_init_MA(jvmtiEventCallbacks* callbacks);


/********************** Agent thread *************************/

/**
 * Returns thread object associated with agent thread..
 * If something fails, complains an error and returns NULL.
 */
jthread nsk_jvmti_getAgentThread();

/**
 * Returns JNI environment constructed for agent thread.
 * If something fails, complains an error and returns NULL.
 */
JNIEnv* nsk_jvmti_getAgentJNIEnv();

/**
 * Returns JVMTI environment constructed for agent.
 * If something fails, complains an error and returns NULL.
 */
jvmtiEnv* nsk_jvmti_getAgentJVMTIEnv();

/**
 * Waits for next synchronization point with debuggee class,
 * Then synchronizes current status and pauses debuggee thread.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_waitForSync(jlong timeout);

/**
 * Allow debuggee thread to continue execution after pausing on synchronization.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_resumeSync();

/**
 * Sleep current thread for given timeout in milliseconds.
 */
void nsk_jvmti_sleep(jlong timeout);

/**
 * Reset agent data to prepare for another run.
 */
void nsk_jvmti_resetAgentData();

/*********************** Agent status ************************/

#define NSK_STATUS_PASSED       0
#define NSK_STATUS_FAILED       2
#define NSK_STATUS_BASE         95

/**
 * Sets NSK_STATUS_FAILED as current agent status.
 */
void nsk_jvmti_setFailStatus();

/**
 * Returns 1 (NSK_TRUE) is current agent status is not NSK_STATUS_PASSED.
 * Returns 0 (NSK_FALSE) otherwise.
 */
int  nsk_jvmti_isFailStatus();

/**
 * Returns current agent status.
 */
jint nsk_jvmti_getStatus();


/********************* Classes and threads ******************/

/**
 * Finds first class with given signatire among loaded classes.
 * If no class found or something fails, complains an error and returns NULL.
 * On success creates and returns global reference to the found class.
 */
jclass nsk_jvmti_classBySignature(const char signature[]);

/**
 * Finds first thread with given name among alive threads.
 * If no thread found or something fails, complains an error and returns NULL.
 * On success creates and returns global reference to the found thread.
 */
jthread nsk_jvmti_threadByName(const char name[]);


/******************* Breakpoints and locations ***************/

/**
 * Requests all capabilities needed for finding line location.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_addLocationCapabilities();

/**
 * Requests all capabilities needed for setting breakpoints.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 * On success returns 1 (NSK_TRUE).
 */
int nsk_jvmti_addBreakpointCapabilities();

#define NSK_JVMTI_INVALID_JLOCATION     -2

/**
 * Returns jlocation for given method line.
 * If something fails, complains an error and returns NSK_JVMTI_INVALID_JLOCATION.
 */
jlocation nsk_jvmti_getLineLocation(jclass cls, jmethodID method, int line);

/**
 * Sets breakpoint to the given method line and return breakpoint location.
 * If something fails, complains an error and returns NSK_JVMTI_INVALID_JLOCATION.
 */
jlocation nsk_jvmti_setLineBreakpoint(jclass cls, jmethodID method, int line);

/**
 * Removes breakpoint from the given method line and return breakpoint location.
 * If something fails, complains an error and returns NSK_JVMTI_INVALID_JLOCATION.
 */
jlocation nsk_jvmti_clearLineBreakpoint(jclass cls, jmethodID method, int line);


/********************* Events management *********************/

/**
 * Enables or disables all events of given list for given thread or NULL.
 * If something fails, complains an error and returns 0 (NSK_FALSE).
 */
int nsk_jvmti_enableEvents(jvmtiEventMode enable, int size,
                            jvmtiEvent list[], jthread thread);

/**
 * Returns:
 *      NSK_TRUE if given event is of optional functionality.
 *      NSK_FALSE if given event is of required functionality.
 */
int nsk_jvmti_isOptionalEvent(jvmtiEvent event);

/**
 * Shows possessed capabilities
 */
void nsk_jvmti_showPossessedCapabilities(jvmtiEnv *jvmti);

/**
 * This method enables a single event
 * Return NSK_TRUE when on success and NSK_FALSE on failure.
 */
int nsk_jvmti_enableNotification(jvmtiEnv *jvmti, jvmtiEvent event, jthread thread);

/**
 * This method disables a single event
 * Return NSK_TRUE when on success and NSK_FALSE on failure.
 */

int nsk_jvmti_disableNotification(jvmtiEnv *jvmti, jvmtiEvent event, jthread thread);


/******************** Access test options ********************/

/**
 * Returns value of given option name; or NULL if no such option found.
 * If search name is NULL then complains an error and returns NULL.
 */
const char* nsk_jvmti_findOptionValue(const char name[]);

/**
 * Returns string value of given option; or defaultValue if no such option found.
 * If options is specified but has empty value then complains an error and returns NULL.
 */
const char* nsk_jvmti_findOptionStringValue(const char name[], const char* defaultValue);

/**
 * Returns integer value of given option; or defaultValue if no such option found.
 * If options is specified but has no integer value then complains an error and returns -1.
 */
int nsk_jvmti_findOptionIntValue(const char name[], int defaultValue);

/**
 * Returns number of parsed options.
 */
int nsk_jvmti_getOptionsCount();

/**
 * Returns name of i-th parsed option.
 * If no such option then complains an error and returns NULL.
 */
const char* nsk_jvmti_getOptionName(int i);

/**
 * Returns value of i-th parsed option.
 * If no such option then complains an error and returns NULL.
 */
const char* nsk_jvmti_getOptionValue(int i);


/******************** Access system options ******************/

/**
 * Returns value of -waittime option or default value if not specified.
 */
int  nsk_jvmti_getWaitTime();

/**
 * Sets specified waittime value.
 */
void nsk_jvmti_setWaitTime(int waittime);


/*************************************************************/

/**
 * If positive, assert jvmtiError is equal to expected; or
 * if !positive, assert jvmtiError is not equal to expected.
 * Assert means: complain if the assertion is false.
 * Return the assertion value, either NSK_TRUE or NSK_FALSE.
 * Anyway, trace if "nsk_tools" mode is verbose.
 */
int nsk_jvmti_lverify(int positive, jvmtiError code, jvmtiError expected,
                        const char file[], int line, const char format[], ...);

/************************************************************/


/**
 * This method could be useful for hotswap testcases developed under.`
 * nsk/jvmti/scenarios/hotswap.
 *
 */
 /**
  * This method will try to redefine the class (classToRedefine) by loading
  * physical file.  <b>pathToNewByteCode</b> option which is passed
  * on OnLoad Phase also used.
  *
  * So This method will do a file read pathToByteCode+fileName+.class (total path).
  * Constrcuts a class objects and does a redefine of the class.
  * On successfull redefine this method will return eaither JNI_TRUE or JNI_FALSE
  *
  * Hint::
  *     1)
  *      If there are many redefine on same testcase, then please try to use
  *      integer value (newclass00, newclass01, newclass02 , ....) way.
  *
  *     2) When you compile these please do keep, a metatag on testcase as
  *     # build : native classes classes.redef
  *
  *     3) When you do build these classes are psysically located in build as.
  *
  * TESTBASE/bin/newclass0* directory.
  * eg: for nks/jvmti/scenarios/hotswap/HS204/hs204t001 you should see
  * TESTBASE/bin/newclass0* /nsk/hotswap/HS204/hs204t001/MyClass.class
  *
  */

int nsk_jvmti_redefineClass(jvmtiEnv * jvmti,
        jclass classToRedefine,
        const char * fileName);
/**
 * changed this signature with Ekaterina's suggestion to move.
 *
 */
void nsk_jvmti_getFileName(int redefineCnt, const char * dir,  char * buf, size_t bufsize);

/**
 * This method sets agent status to failed, This would enables native agent to set its status.
 * There is <b>nsk_jvmti_setFailStatus()</b> method which is in sync with debugge/debugger combination.
 * For non-debugger agents, this method can be used. There is java wrapper for this status,
 * defined in java nsk.share.jvmti.RedefineAgent class as boolean : agentStatus().
 *
 */
void nsk_jvmti_agentFailed();

int isThreadExpected(jvmtiEnv *jvmti, jthread thread);

/**
* This method makes the thread to be suspended at the right place when the top frame
* belongs to the test rather than to incidental Java code (classloading, JVMCI, etc).
*/
int suspendThreadAtMethod(jvmtiEnv *jvmti, jclass cls, jobject thread, jmethodID method);

jint createRawMonitor(jvmtiEnv *env, const char *name, jrawMonitorID *monitor);

void exitOnError(jvmtiError error);

/**
   Wrappers for corresponded JVMTI functions check error code and force exit on error
 */
void rawMonitorEnter(jvmtiEnv *env, jrawMonitorID monitor);
void rawMonitorExit(jvmtiEnv *env, jrawMonitorID monitor);
void rawMonitorNotify(jvmtiEnv *env, jrawMonitorID monitor);
void rawMonitorWait(jvmtiEnv *env, jrawMonitorID monitor, jlong millis);
void getPhase(jvmtiEnv *env, jvmtiPhase *phase);

/*******************************************************************/

#if (defined(WIN32) || defined(_WIN32))
#define snprintf _snprintf
#endif


}

#endif
