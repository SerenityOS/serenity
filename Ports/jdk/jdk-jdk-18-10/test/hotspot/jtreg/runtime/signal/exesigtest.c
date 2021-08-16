/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <signal.h>
#include <stdarg.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>

/*
 * This is the main program to test the signal chaining/ handling functionality
 * See bugs 6277077 and 6414402
 */

#define TRUE  1
#define FALSE 0
typedef int boolean;

static JNIEnv *env;
static JavaVM *vm;

// static int sigid = 0;

// Define the test pass/ fail codes, may be we can use
// nsk/share/native/native_consts.h in future
static int TEST_PASSED=0;
static int TEST_FAILED=1;

// This variable is used to notify whether signal has been received or not.
static volatile sig_atomic_t sig_received = 0;

static char *mode = 0;
static char *scenario = 0;
static char *signal_name;
static int signal_num = -1;

static JavaVMOption *options = 0;
static int numOptions = 0;

typedef struct
{
    int sigNum;
    const char* sigName;
} signalDefinition;

static signalDefinition signals[] =
{
    {SIGINT, "SIGINT"},
    {SIGQUIT, "SIGQUIT"},
    {SIGILL, "SIGILL"},
    {SIGTRAP, "SIGTRAP"},
    {SIGIOT, "SIGIOT"},
#ifdef SIGEMT
    {SIGEMT, "SIGEMT"},
#endif
    {SIGFPE, "SIGFPE"},
    {SIGBUS, "SIGBUS"},
    {SIGSEGV, "SIGSEGV"},
    {SIGSYS, "SIGSYS"},
    {SIGPIPE, "SIGPIPE"},
    {SIGALRM, "SIGALRM"},
    {SIGTERM, "SIGTERM"},
    {SIGUSR1, "SIGUSR1"},
    {SIGUSR2, "SIGUSR2"},
#ifdef SIGCLD
    {SIGCLD, "SIGCLD"},
#endif
#ifdef SIGPWR
    {SIGPWR, "SIGPWR"},
#endif
    {SIGWINCH, "SIGWINCH"},
    {SIGURG, "SIGURG"},
#ifdef SIGPOLL
    {SIGPOLL, "SIGPOLL"},
#endif
    {SIGSTOP, "SIGSTOP"},
    {SIGTSTP, "SIGTSTP"},
    {SIGCONT, "SIGCONT"},
    {SIGTTIN, "SIGTTIN"},
    {SIGTTOU, "SIGTTOU"},
    {SIGVTALRM, "SIGVTALRM"},
    {SIGPROF, "SIGPROF"},
    {SIGXCPU, "SIGXCPU"},
    {SIGXFSZ, "SIGXFSZ"},
#ifdef SIGWAITING
    {SIGWAITING, "SIGWAITING"},
#endif
#ifdef SIGLWP
    {SIGLWP, "SIGLWP"},
#endif
#ifdef SIGFREEZE
    {SIGFREEZE, "SIGFREEZE"},
#endif
#ifdef SIGTHAW
    {SIGTHAW, "SIGTHAW"},
#endif
#ifdef SIGLOST
    {SIGLOST, "SIGLOST"},
#endif
#ifdef SIGXRES
    {SIGXRES, "SIGXRES"},
#endif
    {SIGHUP, "SIGHUP"}
};

boolean isSupportedSigScenario ()
{
    if ( (!strcmp(scenario, "nojvm")) || (!strcmp(scenario, "prepre")) || (!strcmp(scenario, "prepost")) ||
                (!strcmp(scenario, "postpost")) || (!strcmp(scenario, "postpre")) )
    {
        // printf("%s is a supported scenario\n", scenario);
        return TRUE;
    }
    else
    {
        printf("ERROR: %s is not a supported scenario\n", scenario);
        return FALSE;
    }
}

boolean isSupportedSigMode ()
{
    if ( (!strcmp(mode, "sigset")) || (!strcmp(mode, "sigaction")) )
    {
        // printf("%s is a supported mode\n", mode);
        return TRUE;
    }
    else
    {
        printf("ERROR: %s is not a supported mode\n", mode);
        return FALSE;
    }
}

int getSigNumBySigName(const char* sigName)
{
    int signals_len, sigdef_len, total_sigs, i=0;

    if (sigName == NULL) return -1;

    signals_len = sizeof(signals);
    sigdef_len = sizeof(signalDefinition);
    total_sigs = signals_len / sigdef_len;
    for (i = 0; i < total_sigs; i++)
    {
        // printf("Inside for loop, i = %d\n", i);
        if (!strcmp(sigName, signals[i].sigName))
            return signals[i].sigNum;
    }

    return -1;
}

// signal handler
void handler(int sig)
{
    printf("%s: signal handler for signal %d has been processed\n", signal_name, signal_num);
    sig_received = 1;
}

// Initialize VM with given options
void initVM()
{
    JavaVMInitArgs vm_args;
    int i =0;
    jint result;

    vm_args.nOptions = numOptions;
    vm_args.version = JNI_VERSION_1_2;
    vm_args.ignoreUnrecognized = JNI_FALSE;
    vm_args.options = options;

/* try hardcoding options
    JavaVMOption option1[2];
    option1[0].optionString="-XX:+PrintCommandLineFlags";
    option1[1].optionString="-Xrs";
*/
    vm_args.options=options;
    vm_args.nOptions=numOptions;

    // Print the VM options in use
    printf("initVM: numOptions = %d\n", vm_args.nOptions);
    for (i = 0; i < vm_args.nOptions; i++)
    {
        printf("\tvm_args.options[%d].optionString = %s\n", i, vm_args.options[i].optionString);
    }

    // Initialize VM with given options
    result = JNI_CreateJavaVM( &vm, (void **) &env, &vm_args );

    // Did the VM initialize successfully ?
    if (result != 0)
    {
        printf("ERROR: cannot create Java VM.\n");
        exit(TEST_FAILED);
    }

    (*vm)->AttachCurrentThread(vm, (void **) &env,  (void *) 0);
    printf("initVM: JVM started and attached\n");
}

// Function to set up signal handler
void setSignalHandler()
{
    int retval = 0 ;

    if (!strcmp(mode, "sigaction"))
    {
        struct sigaction act;
        act.sa_handler = handler;
        sigemptyset(&act.sa_mask);
        act.sa_flags = 0;
        retval = sigaction(signal_num, &act, 0);
        if (retval != 0) {
           printf("ERROR: failed to set signal handler using function %s, error=%s\n", mode, strerror(errno));
           exit(TEST_FAILED);
        }
    } // end - dealing with sigaction
    else if (!strcmp(mode, "sigset"))
    {
#ifdef __GNUC__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
#endif
        sigset(signal_num, handler);
#ifdef __GNUC__
#pragma GCC diagnostic pop
#endif
    } // end dealing with sigset
    printf("%s: signal handler using function '%s' has been set\n", signal_name, mode);
}

// Function to invoke given signal
void invokeSignal()
{
    int pid, retval;
    sigset_t new_set, old_set;

    pid = getpid();
    retval = 0;

    // we need to unblock the signal in case it was previously blocked by JVM
    // and as result inherited by child process
    // (this is at least the case for SIGQUIT in case -Xrs flag is not used).
    // Otherwise the test will timeout.
    sigemptyset(&new_set);
    sigaddset(&new_set, signal_num);
    sigprocmask(SIG_UNBLOCK, &new_set, &old_set);
    if (retval != 0) {
        printf("ERROR: failed to unblock signal, error=%s\n", strerror(errno));
        exit(TEST_FAILED);
    }

    // send the signal
    retval = kill(pid, signal_num);
    if (retval != 0)
    {
        printf("ERROR: failed to send signal %s, error=%s\n", signal_name, strerror(errno));
        exit(TEST_FAILED);
    }

    // set original mask for the signal
    retval = sigprocmask(SIG_SETMASK, &old_set, NULL);
    if (retval != 0) {
        printf("ERROR: failed to set original mask for signal, error=%s\n", strerror(errno));
        exit(TEST_FAILED);
    }

    printf("%s: signal has been sent successfully\n", signal_name);
}

// Usage function
void printUsage()
{
    printf("Usage: sigtest -sig {signal_name} -mode {signal | sigset | sigaction } -scenario {nojvm | postpre | postpost | prepre | prepost}> [-vmopt jvm_option] \n");
    printf("\n");
    exit(TEST_FAILED);
}

// signal handler BEFORE VM initialization AND
// Invoke signal BEFORE VM exits
void scen_prepre()
{
    setSignalHandler();
    initVM();
    invokeSignal();
    (*vm)->DestroyJavaVM(vm);
}

// signal handler BEFORE VM initialization AND
// Invoke signal AFTER VM exits
void scen_prepost()
{
    setSignalHandler();
    initVM();
    (*vm)->DestroyJavaVM(vm);
    invokeSignal();
}

// signal handler AFTER VM initialization AND
// Invoke signal BEFORE VM exits
void scen_postpre()
{
    initVM();
    setSignalHandler();
    invokeSignal();
    (*vm)->DestroyJavaVM(vm);
}

// signal handler AFTER VM initializationAND
// Invoke signal AFTER VM exits
void scen_postpost()
{
    initVM();
    setSignalHandler();
    (*vm)->DestroyJavaVM(vm);
    invokeSignal();
}

// signal handler with no JVM in picture
void scen_nojvm()
{
    setSignalHandler();
    invokeSignal();
}

void run()
{
    // print the current scenario
    if (!strcmp(scenario, "postpre"))
        scen_postpre();
    else if (!strcmp(scenario, "postpost"))
        scen_postpost();
    else if (!strcmp(scenario, "prepre"))
        scen_prepre();
    else if (!strcmp(scenario, "prepost"))
        scen_prepost();
    else if (!strcmp(scenario, "nojvm"))
        scen_nojvm();
}

// main main
int main(int argc, char **argv)
{
    int i=0, j;

    signal_num = -1;
    signal_name = NULL;

    // Parse the arguments and find out how many vm args we have
    for (i=1; i<argc; i++)
    {
        if (! strcmp(argv[i], "-sig") )
        {
            i++;
            if ( i >= argc )
            {
                printUsage();
            }
            signal_name = argv[i];

        }
        else if (!strcmp(argv[i], "-mode"))
        {
            i++;
            if ( i >= argc )
            {
                printUsage();
            }
            mode = argv[i];
        }
        else if (!strcmp(argv[i], "-scenario"))
        {
            i++;
            if ( i >= argc )
            {
                printUsage();
            }
            scenario = argv[i];
        }
        else if (!strcmp(argv[i], "-vmopt"))
        {
            i++;
            if ( i >= argc )
            {
                printUsage();
            }
            numOptions++;
        }
        else
        {
            printUsage();
        }
    }

    if ( !isSupportedSigScenario() || !isSupportedSigMode() )
    {
        printUsage();
    }

    // get signal number by it's name
    signal_num = getSigNumBySigName(signal_name);
    if (signal_num == -1)
    {
      printf("%s: unknown signal, perhaps is not supported on this platform, ignore\n",
            signal_name);
      exit(TEST_PASSED);
    }

    j = 0;
    // Initialize given number of VM options
    if (numOptions > 0)
    {
        options = (JavaVMOption *) malloc(numOptions * sizeof(JavaVMOption));
        for (i=0; i<argc; i++)
        {
            // parse VM options
            if (!strcmp(argv[i], "-vmopt"))
            {
                i++;
                if ( i >= argc )
                {
                    printUsage();
                }
                options[j].optionString = argv[i];
                j++;
            }
        }
    }

    // do signal invocation
    printf("%s: start testing: signal_num=%d,  mode=%s, scenario=%s\n", signal_name, signal_num, mode, scenario);
    run();

    while (!sig_received) {
      sleep(1);
      printf("%s: waiting for getting signal 1sec ...\n", signal_name);
    }

    printf("%s: signal has been received\n", signal_name);

    free(options);

    return (sig_received ? TEST_PASSED : TEST_FAILED);
}
