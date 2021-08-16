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
#include "commonRef.h"
#include "debugDispatch.h"
#include "eventHandler.h"
#include "eventHelper.h"
#include "threadControl.h"
#include "stepControl.h"
#include "transport.h"
#include "classTrack.h"
#include "debugLoop.h"
#include "bag.h"
#include "invoker.h"
#include "sys.h"

/* How the options get to OnLoad: */
#define XRUN "-Xrunjdwp"
#define AGENTLIB "-agentlib:jdwp"

/* Debug version defaults */
#ifdef DEBUG
    #define DEFAULT_ASSERT_ON           JNI_TRUE
    #define DEFAULT_ASSERT_FATAL        JNI_TRUE
    #define DEFAULT_LOGFILE             "jdwp.log"
#else
    #define DEFAULT_ASSERT_ON           JNI_FALSE
    #define DEFAULT_ASSERT_FATAL        JNI_FALSE
    #define DEFAULT_LOGFILE             NULL
#endif

static jboolean vmInitialized;
static jrawMonitorID initMonitor;
static jboolean initComplete;
static jbyte currentSessionID;

/*
 * Options set through the OnLoad options string. All of these values
 * are set once at VM startup and never reset.
 */
static jboolean isServer = JNI_FALSE;     /* Listens for connecting debuggers? */
static jboolean isStrict = JNI_FALSE;     /* Unused */
static jboolean useStandardAlloc = JNI_FALSE;  /* Use standard malloc/free? */
static struct bag *transports;            /* of TransportSpec */

static jboolean initOnStartup = JNI_TRUE;   /* init immediately */
static char *initOnException = NULL;        /* init when this exception thrown */
static jboolean initOnUncaught = JNI_FALSE; /* init when uncaught exc thrown */

static char *launchOnInit = NULL;           /* launch this app during init */
static jboolean suspendOnInit = JNI_TRUE;   /* suspend all app threads after init */
static jboolean dopause = JNI_FALSE;        /* pause for debugger attach */
static jboolean docoredump = JNI_FALSE;     /* core dump on exit */
static char *logfile = NULL;                /* Name of logfile (if logging) */
static unsigned logflags = 0;               /* Log flags */

static char *names;                         /* strings derived from OnLoad options */

static jboolean allowStartViaJcmd = JNI_FALSE;  /* if true we allow the debugging to be started via a jcmd */
static jboolean startedViaJcmd = JNI_FALSE;     /* if false, we have not yet started debugging via a jcmd */

/*
 * Elements of the transports bag
 */
typedef struct TransportSpec {
    char *name;
    char *address;
    long timeout;
    char *allow;
} TransportSpec;

/*
 * Forward Refs
 */
static void JNICALL cbEarlyVMInit(jvmtiEnv*, JNIEnv *, jthread);
static void JNICALL cbEarlyVMDeath(jvmtiEnv*, JNIEnv *);
static void JNICALL cbEarlyException(jvmtiEnv*, JNIEnv *,
            jthread, jmethodID, jlocation, jobject, jmethodID, jlocation);

static void initialize(JNIEnv *env, jthread thread, EventIndex triggering_ei);
static jboolean parseOptions(char *str);

/*
 * Phase 1: Initial load.
 *
 * OnLoad is called by the VM immediately after the back-end
 * library is loaded. We can do very little in this function since
 * the VM has not completed initialization. So, we parse the JDWP
 * options and set up a simple initial event callbacks for JVMTI events.
 * When a triggering event occurs, that callback will begin debugger initialization.
 */

/* Get a static area to hold the Global Data */
static BackendGlobalData *
get_gdata(void)
{
    static BackendGlobalData s;
    (void)memset(&s, 0, sizeof(BackendGlobalData));
    return &s;
}

static jvmtiError
set_event_notification(jvmtiEventMode mode, EventIndex ei)
{
    jvmtiError error;
    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventNotificationMode)
                (gdata->jvmti, mode, eventIndex2jvmti(ei), NULL);
    if (error != JVMTI_ERROR_NONE) {
        ERROR_MESSAGE(("JDWP unable to configure initial JVMTI event %s: %s(%d)",
                    eventText(ei), jvmtiErrorText(error), error));
    }
    return error;
}

typedef struct {
    int major;
    int minor;
} version_type;

typedef struct {
    version_type runtime;
    version_type compiletime;
} compatible_versions_type;

/*
 * List of explicitly compatible JVMTI versions, specified as
 * { runtime version, compile-time version } pairs. -1 is a wildcard.
 */
static int nof_compatible_versions = 3;
static compatible_versions_type compatible_versions_list[] = {
    /*
     * FIXUP: Allow version 0 to be compatible with anything
     * Special check for FCS of 1.0.
     */
    { {  0, -1 }, { -1, -1 } },
    { { -1, -1 }, {  0, -1 } },
    /*
     * 1.2 is runtime compatible with 1.1 -- just make sure to check the
     * version before using any new 1.2 features
     */
    { {  1,  1 }, {  1,  2 } }
};


/* Logic to determine JVMTI version compatibility */
static jboolean
compatible_versions(jint major_runtime,     jint minor_runtime,
                    jint major_compiletime, jint minor_compiletime)
{
    /*
     * First check to see if versions are explicitly compatible via the
     * list specified above.
     */
    int i;
    for (i = 0; i < nof_compatible_versions; ++i) {
        version_type runtime = compatible_versions_list[i].runtime;
        version_type comptime = compatible_versions_list[i].compiletime;

        if ((major_runtime     == runtime.major  || runtime.major  == -1) &&
            (minor_runtime     == runtime.minor  || runtime.minor  == -1) &&
            (major_compiletime == comptime.major || comptime.major == -1) &&
            (minor_compiletime == comptime.minor || comptime.minor == -1)) {
            return JNI_TRUE;
        }
    }

    return major_runtime == major_compiletime &&
           minor_runtime >= minor_compiletime;
}

/* OnLoad startup:
 *   Returning JNI_ERR will cause the java_g VM to core dump, be careful.
 */
JNIEXPORT jint JNICALL
DEF_Agent_OnLoad(JavaVM *vm, char *options, void *reserved)
{
    jvmtiError error;
    jvmtiCapabilities needed_capabilities;
    jvmtiCapabilities potential_capabilities;
    jint              jvmtiCompileTimeMajorVersion;
    jint              jvmtiCompileTimeMinorVersion;
    jint              jvmtiCompileTimeMicroVersion;

    /* See if it's already loaded */
    if ( gdata!=NULL && gdata->isLoaded==JNI_TRUE ) {
        ERROR_MESSAGE(("Cannot load this JVM TI agent twice, check your java command line for duplicate jdwp options."));
        return JNI_ERR;
    }

    /* If gdata is defined and the VM died, why are we here? */
    if ( gdata!=NULL && gdata->vmDead ) {
        ERROR_MESSAGE(("JDWP unable to load, VM died"));
        return JNI_ERR;
    }

    /* Get global data area */
    gdata = get_gdata();
    if (gdata == NULL) {
        ERROR_MESSAGE(("JDWP unable to allocate memory"));
        return JNI_ERR;
    }
    gdata->isLoaded = JNI_TRUE;

    /* Start filling in gdata */
    gdata->jvm = vm;
    vmInitialized = JNI_FALSE;
    gdata->vmDead = JNI_FALSE;

    /* Get the JVMTI Env, IMPORTANT: Do this first! For jvmtiAllocate(). */
    error = JVM_FUNC_PTR(vm,GetEnv)
                (vm, (void **)&(gdata->jvmti), JVMTI_VERSION_1);
    if (error != JNI_OK) {
        ERROR_MESSAGE(("JDWP unable to access JVMTI Version 1 (0x%x),"
                         " is your J2SE a 1.5 or newer version?"
                         " JNIEnv's GetEnv() returned %d",
                         JVMTI_VERSION_1, error));
        forceExit(1); /* Kill entire process, no core dump */
    }

    /* Check to make sure the version of jvmti.h we compiled with
     *      matches the runtime version we are using.
     */
    jvmtiCompileTimeMajorVersion  = ( JVMTI_VERSION & JVMTI_VERSION_MASK_MAJOR )
                                        >> JVMTI_VERSION_SHIFT_MAJOR;
    jvmtiCompileTimeMinorVersion  = ( JVMTI_VERSION & JVMTI_VERSION_MASK_MINOR )
                                        >> JVMTI_VERSION_SHIFT_MINOR;
    jvmtiCompileTimeMicroVersion  = ( JVMTI_VERSION & JVMTI_VERSION_MASK_MICRO )
                                        >> JVMTI_VERSION_SHIFT_MICRO;

    /* Check for compatibility */
    if ( !compatible_versions(jvmtiMajorVersion(), jvmtiMinorVersion(),
                jvmtiCompileTimeMajorVersion, jvmtiCompileTimeMinorVersion) ) {

        ERROR_MESSAGE(("This jdwp native library will not work with this VM's "
                       "version of JVMTI (%d.%d.%d), it needs JVMTI %d.%d[.%d].",
                       jvmtiMajorVersion(),
                       jvmtiMinorVersion(),
                       jvmtiMicroVersion(),
                       jvmtiCompileTimeMajorVersion,
                       jvmtiCompileTimeMinorVersion,
                       jvmtiCompileTimeMicroVersion));

        /* Do not let VM get a fatal error, we don't want a core dump here. */
        forceExit(1); /* Kill entire process, no core dump wanted */
    }

    /* Parse input options */
    if (!parseOptions(options)) {
        /* No message necessary, should have been printed out already */
        /* Do not let VM get a fatal error, we don't want a core dump here. */
        forceExit(1); /* Kill entire process, no core dump wanted */
    }

    LOG_MISC(("Onload: %s", options));

    /* Get potential capabilities */
    (void)memset(&potential_capabilities,0,sizeof(potential_capabilities));
    error = JVMTI_FUNC_PTR(gdata->jvmti,GetPotentialCapabilities)
                (gdata->jvmti, &potential_capabilities);
    if (error != JVMTI_ERROR_NONE) {
        ERROR_MESSAGE(("JDWP unable to get potential JVMTI capabilities: %s(%d)",
                        jvmtiErrorText(error), error));
        return JNI_ERR;
    }

    /* Fill in ones that we must have */
    (void)memset(&needed_capabilities,0,sizeof(needed_capabilities));
    needed_capabilities.can_access_local_variables              = 1;
    needed_capabilities.can_generate_single_step_events         = 1;
    needed_capabilities.can_generate_exception_events           = 1;
    needed_capabilities.can_generate_frame_pop_events           = 1;
    needed_capabilities.can_generate_breakpoint_events          = 1;
    needed_capabilities.can_suspend                             = 1;
    needed_capabilities.can_generate_method_entry_events        = 1;
    needed_capabilities.can_generate_method_exit_events         = 1;
    needed_capabilities.can_generate_garbage_collection_events  = 1;
    needed_capabilities.can_maintain_original_method_order      = 1;
    needed_capabilities.can_generate_monitor_events             = 1;
    needed_capabilities.can_tag_objects                         = 1;

    /* And what potential ones that would be nice to have */
    needed_capabilities.can_force_early_return
                = potential_capabilities.can_force_early_return;
    needed_capabilities.can_generate_field_modification_events
                = potential_capabilities.can_generate_field_modification_events;
    needed_capabilities.can_generate_field_access_events
                = potential_capabilities.can_generate_field_access_events;
    needed_capabilities.can_get_bytecodes
                = potential_capabilities.can_get_bytecodes;
    needed_capabilities.can_get_synthetic_attribute
                = potential_capabilities.can_get_synthetic_attribute;
    needed_capabilities.can_get_owned_monitor_info
                = potential_capabilities.can_get_owned_monitor_info;
    needed_capabilities.can_get_current_contended_monitor
                = potential_capabilities.can_get_current_contended_monitor;
    needed_capabilities.can_get_monitor_info
                = potential_capabilities.can_get_monitor_info;
    needed_capabilities.can_pop_frame
                = potential_capabilities.can_pop_frame;
    needed_capabilities.can_redefine_classes
                = potential_capabilities.can_redefine_classes;
    needed_capabilities.can_redefine_any_class
                = potential_capabilities.can_redefine_any_class;
    needed_capabilities.can_get_owned_monitor_stack_depth_info
        = potential_capabilities.can_get_owned_monitor_stack_depth_info;
    needed_capabilities.can_get_constant_pool
                = potential_capabilities.can_get_constant_pool;
    {
        needed_capabilities.can_get_source_debug_extension      = 1;
        needed_capabilities.can_get_source_file_name            = 1;
        needed_capabilities.can_get_line_numbers                = 1;
        needed_capabilities.can_signal_thread
                = potential_capabilities.can_signal_thread;
    }

    /* Add the capabilities */
    error = JVMTI_FUNC_PTR(gdata->jvmti,AddCapabilities)
                (gdata->jvmti, &needed_capabilities);
    if (error != JVMTI_ERROR_NONE) {
        ERROR_MESSAGE(("JDWP unable to get necessary JVMTI capabilities."));
        forceExit(1); /* Kill entire process, no core dump wanted */
    }

    /* Initialize event number mapping tables */
    eventIndexInit();

    /* Set the initial JVMTI event notifications */
    error = set_event_notification(JVMTI_ENABLE, EI_VM_DEATH);
    if (error != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }
    error = set_event_notification(JVMTI_ENABLE, EI_VM_INIT);
    if (error != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }
    if (initOnUncaught || (initOnException != NULL)) {
        error = set_event_notification(JVMTI_ENABLE, EI_EXCEPTION);
        if (error != JVMTI_ERROR_NONE) {
            return JNI_ERR;
        }
    }

    /* Set callbacks just for 3 functions */
    (void)memset(&(gdata->callbacks),0,sizeof(gdata->callbacks));
    gdata->callbacks.VMInit             = &cbEarlyVMInit;
    gdata->callbacks.VMDeath            = &cbEarlyVMDeath;
    gdata->callbacks.Exception  = &cbEarlyException;
    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventCallbacks)
                (gdata->jvmti, &(gdata->callbacks), sizeof(gdata->callbacks));
    if (error != JVMTI_ERROR_NONE) {
        ERROR_MESSAGE(("JDWP unable to set JVMTI event callbacks: %s(%d)",
                        jvmtiErrorText(error), error));
        return JNI_ERR;
    }

    LOG_MISC(("OnLoad: DONE"));
    return JNI_OK;
}

JNIEXPORT void JNICALL
DEF_Agent_OnUnload(JavaVM *vm)
{

    gdata->isLoaded = JNI_FALSE;

    /* Cleanup, but make sure VM is alive before using JNI, and
     *   make sure JVMTI environment is ok before deallocating
     *   memory allocated through JVMTI, which all of it is.
     */

    /*
     * Close transport before exit
     */
    if (transport_is_open()) {
        transport_close();
    }
}

/*
 * Phase 2: Initial events. Phase 2 consists of waiting for the
 * event that triggers full initialization. Under normal circumstances
 * (initOnStartup == TRUE) this is the JVMTI_EVENT_VM_INIT event.
 * Otherwise, we delay initialization until the app throws a
 * particular exception. The triggering event invokes
 * the bulk of the initialization, including creation of threads and
 * monitors, transport setup, and installation of a new event callback which
 * handles the complete set of events.
 *
 * Since the triggering event comes in on an application thread, some of the
 * initialization is difficult to do here. Specifically, this thread along
 * with all other app threads may need to be suspended until a debugger
 * connects. These kinds of tasks are left to the third phase which is
 * invoked by one of the spawned debugger threads, the event handler.
 */

/*
 * Wait for a triggering event; then kick off debugger
 * initialization. A different event callback will be installed by
 * debugger initialization, and this function will not be called
 * again.
 */

    /*
     * TO DO: Decide whether we need to protect this code with
     * a lock. It might be too early to create a monitor safely (?).
     */

static void JNICALL
cbEarlyVMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread)
{
    LOG_CB(("cbEarlyVMInit"));
    if ( gdata->vmDead ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"VM dead at VM_INIT time");
    }
    if (initOnStartup)
        initialize(env, thread, EI_VM_INIT);
    vmInitialized = JNI_TRUE;
    LOG_MISC(("END cbEarlyVMInit"));
}

static void
disposeEnvironment(jvmtiEnv *jvmti_env)
{
    jvmtiError error;

    error = JVMTI_FUNC_PTR(jvmti_env,DisposeEnvironment)(jvmti_env);
    if ( error == JVMTI_ERROR_MUST_POSSESS_CAPABILITY )
        error = JVMTI_ERROR_NONE;  /* Hack!  FIXUP when JVMTI has disposeEnv */
    /* What should error return say? */
    if (error != JVMTI_ERROR_NONE) {
        ERROR_MESSAGE(("JDWP unable to dispose of JVMTI environment: %s(%d)",
                        jvmtiErrorText(error), error));
    }
    gdata->jvmti = NULL;
}

static void JNICALL
cbEarlyVMDeath(jvmtiEnv *jvmti_env, JNIEnv *env)
{
    LOG_CB(("cbEarlyVMDeath"));
    if ( gdata->vmDead ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"VM died more than once");
    }
    disposeEnvironment(jvmti_env);
    gdata->jvmti = NULL;
    gdata->jvm = NULL;
    gdata->vmDead = JNI_TRUE;
    LOG_MISC(("END cbEarlyVMDeath"));
}

static void JNICALL
cbEarlyException(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location,
        jobject exception,
        jmethodID catch_method, jlocation catch_location)
{
    jvmtiError error;
    jthrowable currentException;

    LOG_CB(("cbEarlyException: thread=%p", thread));

    if ( gdata->vmDead ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"VM dead at initial Exception event");
    }
    if (!vmInitialized)  {
        LOG_MISC(("VM is not initialized yet"));
        return;
    }

    /*
     * We want to preserve any current exception that might get wiped
     * out during event handling (e.g. JNI calls). We have to rely on
     * space for the local reference on the current frame because
     * doing a PushLocalFrame here might itself generate an exception.
     */

    currentException = JNI_FUNC_PTR(env,ExceptionOccurred)(env);
    JNI_FUNC_PTR(env,ExceptionClear)(env);

    if (initOnUncaught && catch_method == NULL) {

        LOG_MISC(("Initializing on uncaught exception"));
        initialize(env, thread, EI_EXCEPTION);

    } else if (initOnException != NULL) {

        jclass clazz;

        /* Get class of exception thrown */
        clazz = JNI_FUNC_PTR(env,GetObjectClass)(env, exception);
        if ( clazz != NULL ) {
            char *signature = NULL;
            /* initing on throw, check */
            error = classSignature(clazz, &signature, NULL);
            LOG_MISC(("Checking specific exception: looking for %s, got %s",
                        initOnException, signature));
            if ( (error==JVMTI_ERROR_NONE) &&
                (strcmp(signature, initOnException) == 0)) {
                LOG_MISC(("Initializing on specific exception"));
                initialize(env, thread, EI_EXCEPTION);
            } else {
                error = AGENT_ERROR_INTERNAL; /* Just to cause restore */
            }
            if ( signature != NULL ) {
                jvmtiDeallocate(signature);
            }
        } else {
            error = AGENT_ERROR_INTERNAL; /* Just to cause restore */
        }

        /* If initialize didn't happen, we need to restore things */
        if ( error != JVMTI_ERROR_NONE ) {
            /*
             * Restore exception state from before callback call
             */
            LOG_MISC(("No initialization, didn't find right exception"));
            if (currentException != NULL) {
                JNI_FUNC_PTR(env,Throw)(env, currentException);
            } else {
                JNI_FUNC_PTR(env,ExceptionClear)(env);
            }
        }

    }

    LOG_MISC(("END cbEarlyException"));

}

typedef struct EnumerateArg {
    jboolean isServer;
    jdwpError error;
    jint startCount;
} EnumerateArg;

static jboolean
startTransport(void *item, void *arg)
{
    TransportSpec *transport = item;
    EnumerateArg *enumArg = arg;
    jdwpError serror;

    LOG_MISC(("Begin startTransport"));
    serror = transport_startTransport(enumArg->isServer, transport->name,
                                      transport->address, transport->timeout,
                                      transport->allow);
    if (serror != JDWP_ERROR(NONE)) {
        ERROR_MESSAGE(("JDWP Transport %s failed to initialize, %s(%d)",
                transport->name, jdwpErrorText(serror), serror));
        enumArg->error = serror;
    } else {
        /* (Don't overwrite any previous error) */

        enumArg->startCount++;
    }

    LOG_MISC(("End startTransport"));

    return JNI_TRUE;   /* Always continue, even if there was an error */
}

static void
signalInitComplete(void)
{
    /*
     * Initialization is complete
     */
    LOG_MISC(("signal initialization complete"));
    debugMonitorEnter(initMonitor);
    initComplete = JNI_TRUE;
    debugMonitorNotifyAll(initMonitor);
    debugMonitorExit(initMonitor);
}

/*
 * Determine if  initialization is complete.
 */
jboolean
debugInit_isInitComplete(void)
{
    return initComplete;
}

/*
 * Wait for all initialization to complete.
 */
void
debugInit_waitInitComplete(void)
{
    debugMonitorEnter(initMonitor);
    while (!initComplete) {
        debugMonitorWait(initMonitor);
    }
    debugMonitorExit(initMonitor);
}

/* All process exit() calls come from here */
void
forceExit(int exit_code)
{
    /* make sure the transport is closed down before we exit() */
    transport_close();
    exit(exit_code);
}

/* All JVM fatal error exits lead here (e.g. we need to kill the VM). */
static void
jniFatalError(JNIEnv *env, const char *msg, jvmtiError error, int exit_code)
{
    JavaVM *vm;
    char buf[512];

    gdata->vmDead = JNI_TRUE;
    if ( msg==NULL )
        msg = "UNKNOWN REASON";
    vm = gdata->jvm;
    if ( env==NULL && vm!=NULL ) {
        jint rc = (*((*vm)->GetEnv))(vm, (void **)&env, JNI_VERSION_1_2);
        if (rc != JNI_OK ) {
            env = NULL;
        }
    }
    if ( error != JVMTI_ERROR_NONE ) {
        (void)snprintf(buf, sizeof(buf), "JDWP %s, jvmtiError=%s(%d)",
                    msg, jvmtiErrorText(error), error);
    } else {
        (void)snprintf(buf, sizeof(buf), "JDWP %s", msg);
    }
    if (env != NULL) {
        (*((*env)->FatalError))(env, buf);
    } else {
        /* Should rarely ever reach here, means VM is really dead */
        print_message(stderr, "ERROR: JDWP: ", "\n",
                "Can't call JNI FatalError(NULL, \"%s\")", buf);
    }
    forceExit(exit_code);
}

/*
 * Initialize debugger back end modules
 */
static void
initialize(JNIEnv *env, jthread thread, EventIndex triggering_ei)
{
    jvmtiError error;
    EnumerateArg arg;
    jbyte suspendPolicy;

    LOG_MISC(("Begin initialize()"));
    currentSessionID = 0;
    initComplete = JNI_FALSE;

    if ( gdata->vmDead ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"VM dead at initialize() time");
    }

    /* Turn off the initial JVMTI event notifications */
    error = set_event_notification(JVMTI_DISABLE, EI_EXCEPTION);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "unable to disable JVMTI event notification");
    }
    error = set_event_notification(JVMTI_DISABLE, EI_VM_INIT);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "unable to disable JVMTI event notification");
    }
    error = set_event_notification(JVMTI_DISABLE, EI_VM_DEATH);
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "unable to disable JVMTI event notification");
    }

    /* Remove initial event callbacks */
    (void)memset(&(gdata->callbacks),0,sizeof(gdata->callbacks));
    error = JVMTI_FUNC_PTR(gdata->jvmti,SetEventCallbacks)
                (gdata->jvmti, &(gdata->callbacks), sizeof(gdata->callbacks));
    if (error != JVMTI_ERROR_NONE) {
        EXIT_ERROR(error, "unable to clear JVMTI callbacks");
    }

    commonRef_initialize();
    util_initialize(env);
    threadControl_initialize();
    stepControl_initialize();
    invoker_initialize();
    debugDispatch_initialize();
    classTrack_initialize(env);
    debugLoop_initialize();

    initMonitor = debugMonitorCreate("JDWP Initialization Monitor");


    /*
     * Initialize transports
     */
    arg.isServer = isServer;
    arg.error = JDWP_ERROR(NONE);
    arg.startCount = 0;

    transport_initialize();
    (void)bagEnumerateOver(transports, startTransport, &arg);

    /*
     * Exit with an error only if
     * 1) none of the transports was successfully started, and
     * 2) the application has not yet started running
     */
    if ((arg.error != JDWP_ERROR(NONE)) &&
        (arg.startCount == 0) &&
        initOnStartup) {
        EXIT_ERROR(map2jvmtiError(arg.error), "No transports initialized");
    }

    eventHandler_initialize(currentSessionID);

    signalInitComplete();

    transport_waitForConnection();

    suspendPolicy = suspendOnInit ? JDWP_SUSPEND_POLICY(ALL)
                                  : JDWP_SUSPEND_POLICY(NONE);
    if (triggering_ei == EI_VM_INIT) {
        LOG_MISC(("triggering_ei == EI_VM_INIT"));
        eventHelper_reportVMInit(env, currentSessionID, thread, suspendPolicy);
    } else {
        /*
         * TO DO: Kludgy way of getting the triggering event to the
         * just-attached debugger. It would be nice to make this a little
         * cleaner. There is also a race condition where other events
         * can get in the queue (from other not-yet-suspended threads)
         * before this one does. (Also need to handle allocation error below?)
         */
        EventInfo info;
        struct bag *initEventBag;
        LOG_MISC(("triggering_ei != EI_VM_INIT"));
        initEventBag = eventHelper_createEventBag();
        (void)memset(&info,0,sizeof(info));
        info.ei = triggering_ei;
        eventHelper_recordEvent(&info, 0, suspendPolicy, initEventBag);
        (void)eventHelper_reportEvents(currentSessionID, initEventBag);
        bagDestroyBag(initEventBag);
    }

    if ( gdata->vmDead ) {
        EXIT_ERROR(AGENT_ERROR_INTERNAL,"VM dead before initialize() completes");
    }
    LOG_MISC(("End initialize()"));
}

/*
 * Restore all static data to the initialized state so that another
 * debugger can connect properly later.
 */
void
debugInit_reset(JNIEnv *env)
{
    EnumerateArg arg;

    LOG_MISC(("debugInit_reset() beginning"));

    currentSessionID++;
    initComplete = JNI_FALSE;

    eventHandler_reset(currentSessionID);
    transport_reset();
    debugDispatch_reset();
    invoker_reset();
    stepControl_reset();
    threadControl_reset();
    util_reset();
    commonRef_reset(env);
    classTrack_reset();

    /*
     * If this is a server, we are now ready to accept another connection.
     * If it's a client, then we've cleaned up some (more should be added
     * later) and we're done.
     */
    if (isServer) {
        arg.isServer = JNI_TRUE;
        arg.error = JDWP_ERROR(NONE);
        arg.startCount = 0;
        (void)bagEnumerateOver(transports, startTransport, &arg);

        signalInitComplete();

        transport_waitForConnection();
    } else {
        signalInitComplete(); /* Why? */
    }

    LOG_MISC(("debugInit_reset() completed."));
}


char *
debugInit_launchOnInit(void)
{
    return launchOnInit;
}

jboolean
debugInit_suspendOnInit(void)
{
    return suspendOnInit;
}

/*
 * code below is shamelessly swiped from hprof.
 */

static int
get_tok(char **src, char *buf, int buflen, char sep)
{
    int i;
    char *p = *src;
    for (i = 0; i < buflen; i++) {
        if (p[i] == 0 || p[i] == sep) {
            buf[i] = 0;
            if (p[i] == sep) {
                i++;
            }
            *src += i;
            return i;
        }
        buf[i] = p[i];
    }
    /* overflow */
    return 0;
}

static void
printUsage(void)
{
     TTY_MESSAGE((
 "               Java Debugger JDWP Agent Library\n"
 "               --------------------------------\n"
 "\n"
 "  (See the \"VM Invocation Options\" section of the JPDA\n"
 "   \"Connection and Invocation Details\" document for more information.)\n"
 "\n"
 "jdwp usage: java " AGENTLIB "=[help]|[<option>=<value>, ...]\n"
 "\n"
 "Option Name and Value            Description                       Default\n"
 "---------------------            -----------                       -------\n"
 "suspend=y|n                      wait on startup?                  y\n"
 "transport=<name>                 transport spec                    none\n"
 "address=<listen/attach address>  transport spec                    \"\"\n"
 "server=y|n                       listen for debugger?              n\n"
 "launch=<command line>            run debugger on event             none\n"
 "onthrow=<exception name>         debug on throw                    none\n"
 "onuncaught=y|n                   debug on any uncaught?            n\n"
 "timeout=<timeout value>          for listen/attach in milliseconds n\n"
 "mutf8=y|n                        output modified utf-8             n\n"
 "quiet=y|n                        control over terminal messages    n\n"));

    TTY_MESSAGE((
 "Obsolete Options\n"
 "----------------\n"
 "strict=y|n\n"
 "stdalloc=y|n\n"
 "\n"
 "Examples\n"
 "--------\n"
 "  - Using sockets connect to a debugger at a specific address:\n"
 "    java " AGENTLIB "=transport=dt_socket,address=localhost:8000 ...\n"
 "  - Using sockets listen for a debugger to attach:\n"
 "    java " AGENTLIB "=transport=dt_socket,server=y,suspend=y ...\n"
 "\n"
 "Notes\n"
 "-----\n"
 "  - A timeout value of 0 (the default) is no timeout.\n"
 "\n"
 "Warnings\n"
 "--------\n"
 "  - The older " XRUN " interface can still be used, but will be removed in\n"
 "    a future release, for example:\n"
 "        java " XRUN ":[help]|[<option>=<value>, ...]\n"
    ));

#ifdef DEBUG

     TTY_MESSAGE((
 "\n"
 "Debugging Options            Description                       Default\n"
 "-----------------            -----------                       -------\n"
 "pause=y|n                    pause to debug PID                n\n"
 "coredump=y|n                 coredump at exit                  n\n"
 "errorexit=y|n                exit on any error                 n\n"
 "logfile=filename             name of log file                  none\n"
 "logflags=flags               log flags (bitmask)               none\n"
 "                               JVM calls     = 0x001\n"
 "                               JNI calls     = 0x002\n"
 "                               JVMTI calls   = 0x004\n"
 "                               misc events   = 0x008\n"
 "                               step logs     = 0x010\n"
 "                               locations     = 0x020\n"
 "                               callbacks     = 0x040\n"
 "                               errors        = 0x080\n"
 "                               everything    = 0xfff"));

    TTY_MESSAGE((
 "debugflags=flags             debug flags (bitmask)           none\n"
 "                               USE_ITERATE_THROUGH_HEAP 0x01\n"
 "\n"
 "Environment Variables\n"
 "---------------------\n"
 "_JAVA_JDWP_OPTIONS\n"
 "    Options can be added externally via this environment variable.\n"
 "    Anything contained in it will get a comma prepended to it (if needed),\n"
 "    then it will be added to the end of the options supplied via the\n"
 "    " XRUN " or " AGENTLIB " command line option.\n"
    ));

#endif



}

static jboolean checkAddress(void *bagItem, void *arg)
{
    TransportSpec *spec = (TransportSpec *)bagItem;
    if (spec->address == NULL) {
        ERROR_MESSAGE(("JDWP Non-server transport %s must have a connection "
                "address specified through the 'address=' option",
                spec->name));
        return JNI_FALSE;
    } else {
        return JNI_TRUE;
    }
}

static  char *
add_to_options(char *options, char *new_options)
{
    size_t originalLength;
    char *combinedOptions;

    /*
     * Allocate enough space for both strings and
     * comma in between.
     */
    originalLength = strlen(options);
    combinedOptions = jvmtiAllocate((jint)originalLength + 1 +
                                (jint)strlen(new_options) + 1);
    if (combinedOptions == NULL) {
        return NULL;
    }

    (void)strcpy(combinedOptions, options);
    (void)strcat(combinedOptions, ",");
    (void)strcat(combinedOptions, new_options);

    return combinedOptions;
}

static jboolean
get_boolean(char **pstr, jboolean *answer)
{
    char buf[80];
    *answer = JNI_FALSE;
    /*LINTED*/
    if (get_tok(pstr, buf, (int)sizeof(buf), ',')) {
        if (strcmp(buf, "y") == 0) {
            *answer = JNI_TRUE;
            return JNI_TRUE;
        } else if (strcmp(buf, "n") == 0) {
            *answer = JNI_FALSE;
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/* atexit() callback */
static void
atexit_finish_logging(void)
{
    /* Normal exit(0) (not _exit()) may only reach here */
    finish_logging();  /* Only first call matters */
}

static jboolean
parseOptions(char *options)
{
    TransportSpec *currentTransport = NULL;
    char *end;
    char *current;
    int length;
    char *str;
    char *errmsg;
    jboolean onJcmd = JNI_FALSE;

    /* Set defaults */
    gdata->assertOn     = DEFAULT_ASSERT_ON;
    gdata->assertFatal  = DEFAULT_ASSERT_FATAL;
    logfile             = DEFAULT_LOGFILE;

    /* Options being NULL will end up being an error. */
    if (options == NULL) {
        options = "";
    }

    /* Check for "help" BEFORE we add any environmental settings */
    if ((strcmp(options, "help")) == 0) {
        printUsage();
        forceExit(0); /* Kill entire process, no core dump wanted */
    }

    /* These buffers are never freed */
    {
        char *envOptions;

        /*
         * Add environmentally specified options.
         */
        envOptions = getenv("_JAVA_JDWP_OPTIONS");
        if (envOptions != NULL) {
            options = add_to_options(options, envOptions);
            if ( options==NULL ) {
                EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"options");
            }
        }

        /*
         * Allocate a buffer for names derived from option strings. It should
         * never be longer than the original options string itself.
         * Also keep a copy of the options in gdata->options.
         */
        length = (int)strlen(options);
        gdata->options = jvmtiAllocate(length + 1);
        if (gdata->options == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"options");
        }
        (void)strcpy(gdata->options, options);
        names = jvmtiAllocate(length + 1);
        if (names == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"options");
        }

        transports = bagCreateBag(sizeof(TransportSpec), 3);
        if (transports == NULL) {
            EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY,"transports");
        }
    }

    current = names;
    end = names + length;
    str = options;

    while (*str) {
        char buf[100];
        /*LINTED*/
        if (!get_tok(&str, buf, (int)sizeof(buf), '=')) {
            goto syntax_error;
        }
        if (strcmp(buf, "transport") == 0) {
            currentTransport = bagAdd(transports);
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            currentTransport->name = current;
            currentTransport->address = NULL;
            currentTransport->allow = NULL;
            currentTransport->timeout = 0L;
            current += strlen(current) + 1;
        } else if (strcmp(buf, "address") == 0) {
            if (currentTransport == NULL) {
                errmsg = "address specified without transport";
                goto bad_option_with_errmsg;
            }
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            currentTransport->address = current;
            current += strlen(current) + 1;
        } else if (strcmp(buf, "allow") == 0) {
            if (currentTransport == NULL) {
                errmsg = "allow specified without transport";
                goto bad_option_with_errmsg;
            }
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            currentTransport->allow = current;
            current += strlen(current) + 1;
         } else if (strcmp(buf, "timeout") == 0) {
            if (currentTransport == NULL) {
                errmsg = "timeout specified without transport";
                goto bad_option_with_errmsg;
            }
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            currentTransport->timeout = atol(current);
            current += strlen(current) + 1;
        } else if (strcmp(buf, "launch") == 0) {
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            launchOnInit = current;
            current += strlen(current) + 1;
        } else if (strcmp(buf, "onthrow") == 0) {
            /* Read class name and convert in place to a signature */
            *current = 'L';
            /*LINTED*/
            if (!get_tok(&str, current + 1, (int)(end - current - 1), ',')) {
                goto syntax_error;
            }
            initOnException = current;
            while (*current != '\0') {
                if (*current == '.') {
                    *current = '/';
                }
                current++;
            }
            *current++ = ';';
            *current++ = '\0';
        } else if (strcmp(buf, "assert") == 0) {
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            if (strcmp(current, "y") == 0) {
                gdata->assertOn = JNI_TRUE;
                gdata->assertFatal = JNI_FALSE;
            } else if (strcmp(current, "fatal") == 0) {
                gdata->assertOn = JNI_TRUE;
                gdata->assertFatal = JNI_TRUE;
            } else if (strcmp(current, "n") == 0) {
                gdata->assertOn = JNI_FALSE;
                gdata->assertFatal = JNI_FALSE;
            } else {
                goto syntax_error;
            }
            current += strlen(current) + 1;
        } else if (strcmp(buf, "pause") == 0) {
            if ( !get_boolean(&str, &dopause) ) {
                goto syntax_error;
            }
            if ( dopause ) {
                do_pause();
            }
        } else if (strcmp(buf, "coredump") == 0) {
            if ( !get_boolean(&str, &docoredump) ) {
                goto syntax_error;
            }
        } else if (strcmp(buf, "errorexit") == 0) {
            if ( !get_boolean(&str, &(gdata->doerrorexit)) ) {
                goto syntax_error;
            }
        } else if (strcmp(buf, "exitpause") == 0) {
            errmsg = "The exitpause option removed, use -XX:OnError";
            goto bad_option_with_errmsg;
        } else if (strcmp(buf, "precrash") == 0) {
            errmsg = "The precrash option removed, use -XX:OnError";
            goto bad_option_with_errmsg;
        } else if (strcmp(buf, "logfile") == 0) {
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            logfile = current;
            current += strlen(current) + 1;
        } else if (strcmp(buf, "logflags") == 0) {
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            /*LINTED*/
            logflags = (unsigned)strtol(current, NULL, 0);
        } else if (strcmp(buf, "debugflags") == 0) {
            /*LINTED*/
            if (!get_tok(&str, current, (int)(end - current), ',')) {
                goto syntax_error;
            }
            /*LINTED*/
            gdata->debugflags = (unsigned)strtol(current, NULL, 0);
        } else if ( strcmp(buf, "suspend")==0 ) {
            if ( !get_boolean(&str, &suspendOnInit) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "server")==0 ) {
            if ( !get_boolean(&str, &isServer) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "strict")==0 ) { /* Obsolete, but accept it */
            if ( !get_boolean(&str, &isStrict) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "quiet")==0 ) {
            if ( !get_boolean(&str, &(gdata->quiet)) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "onuncaught")==0 ) {
            if ( !get_boolean(&str, &initOnUncaught) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "mutf8")==0 ) {
            if ( !get_boolean(&str, &(gdata->modifiedUtf8)) ) {
                goto syntax_error;
            }
        } else if ( strcmp(buf, "stdalloc")==0 ) { /* Obsolete, but accept it */
            if ( !get_boolean(&str, &useStandardAlloc) ) {
                goto syntax_error;
            }
        } else if (strcmp(buf, "onjcmd") == 0) {
            if (!get_boolean(&str, &onJcmd)) {
                goto syntax_error;
            }
        } else {
            goto syntax_error;
        }
    }

    /* Setup logging now */
    if ( logfile!=NULL ) {
        setup_logging(logfile, logflags);
        (void)atexit(&atexit_finish_logging);
    }

    if (bagSize(transports) == 0) {
        errmsg = "no transport specified";
        goto bad_option_with_errmsg;
    }

    /*
     * TO DO: Remove when multiple transports are allowed. (replace with
     * check below.
     */
    if (bagSize(transports) > 1) {
        errmsg = "multiple transports are not supported in this release";
        goto bad_option_with_errmsg;
    }

    if (!isServer) {
        jboolean specified = bagEnumerateOver(transports, checkAddress, NULL);
        if (!specified) {
            /* message already printed */
            goto bad_option_no_msg;
        }
    }

    /*
     * The user has selected to wait for an exception before init happens
     */
    if ((initOnException != NULL) || (initOnUncaught)) {
        initOnStartup = JNI_FALSE;

        if (launchOnInit == NULL) {
            /*
             * These rely on the launch=/usr/bin/foo
             * suboption, so it is an error if user did not
             * provide one.
             */
            errmsg = "Specify launch=<command line> when using onthrow or onuncaught suboption";
            goto bad_option_with_errmsg;
        }
    }

    if (onJcmd) {
        if (launchOnInit != NULL) {
            errmsg = "Cannot combine onjcmd and launch suboptions";
            goto bad_option_with_errmsg;
        }
        if (!isServer) {
            errmsg = "Can only use onjcmd with server=y";
            goto bad_option_with_errmsg;
        }
        suspendOnInit = JNI_FALSE;
        initOnStartup = JNI_FALSE;
        allowStartViaJcmd = JNI_TRUE;
    }

    return JNI_TRUE;

syntax_error:
    ERROR_MESSAGE(("JDWP option syntax error: %s=%s", AGENTLIB, options));
    return JNI_FALSE;

bad_option_with_errmsg:
    ERROR_MESSAGE(("JDWP %s: %s=%s", errmsg, AGENTLIB, options));
    return JNI_FALSE;

bad_option_no_msg:
    ERROR_MESSAGE(("JDWP %s: %s=%s", "invalid option", AGENTLIB, options));
    return JNI_FALSE;
}

/* All normal exit doors lead here */
void
debugInit_exit(jvmtiError error, const char *msg)
{
    enum exit_codes { EXIT_NO_ERRORS = 0, EXIT_JVMTI_ERROR = 1, EXIT_TRANSPORT_ERROR = 2 };

    // Release commandLoop vmDeathLock if necessary
    commandLoop_exitVmDeathLockOnError();

    // Prepare to exit. Log error and finish logging
    LOG_MISC(("Exiting with error %s(%d): %s", jvmtiErrorText(error), error,
                                               ((msg == NULL) ? "" : msg)));

    // coredump requested by command line. Keep JVMTI data dirty
    if (error != JVMTI_ERROR_NONE && docoredump) {
        LOG_MISC(("Dumping core as requested by command line"));
        finish_logging();
        abort();
    }

    finish_logging();

    // Cleanup the JVMTI if we have one
    if (gdata != NULL) {
        gdata->vmDead = JNI_TRUE;
        if (gdata->jvmti != NULL) {
            // Dispose of jvmti (gdata->jvmti becomes NULL)
            disposeEnvironment(gdata->jvmti);
        }
    }

    // We are here with no errors. Kill entire process and exit with zero exit code
    if (error == JVMTI_ERROR_NONE) {
        forceExit(EXIT_NO_ERRORS);
        return;
    }

    // No transport initilized.
    // As we don't have any details here exiting with separate exit code
    if (error == AGENT_ERROR_TRANSPORT_INIT) {
        forceExit(EXIT_TRANSPORT_ERROR);
        return;
    }

    // We have JVMTI error. Call hotspot jni_FatalError handler
    jniFatalError(NULL, msg, error, EXIT_JVMTI_ERROR);

    // hotspot calls os:abort() so we should never reach code below,
    // but guard against possible hotspot changes

    // Last chance to die, this kills the entire process.
    forceExit(EXIT_JVMTI_ERROR);
}

static jboolean getFirstTransport(void *item, void *arg)
{
    TransportSpec** store = arg;
    *store = item;

    return JNI_FALSE; /* Want the first */
}

/* Call to start up debugging. */
JNIEXPORT char const* JNICALL debugInit_startDebuggingViaCommand(JNIEnv* env, jthread thread, char const** transport_name,
                                                                char const** address, jboolean* first_start) {
    jboolean is_first_start = JNI_FALSE;
    TransportSpec* spec = NULL;

    if (!vmInitialized) {
        return "Not yet initialized. Try again later.";
    }

    if (!allowStartViaJcmd) {
        return "Starting debugging via jcmd was not enabled via the onjcmd option of the jdwp agent.";
    }

    if (!startedViaJcmd) {
        startedViaJcmd = JNI_TRUE;
        is_first_start = JNI_TRUE;
        initialize(env, thread, EI_VM_INIT);
    }

    bagEnumerateOver(transports, getFirstTransport, &spec);

    if ((spec != NULL) && (transport_name != NULL) && (address != NULL)) {
        *transport_name = spec->name;
        *address = spec->address;
    }

    if (first_start != NULL) {
        *first_start = is_first_start;
    }

    return NULL;
}
