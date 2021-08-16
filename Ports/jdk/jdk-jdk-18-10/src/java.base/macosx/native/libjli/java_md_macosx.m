/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "java.h"
#include "jvm_md.h"
#include <dirent.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>

#include "manifest_info.h"

/* Support Cocoa event loop on the main thread */
#include <Cocoa/Cocoa.h>
#include <objc/objc-runtime.h>
#include <objc/objc-auto.h>

#include <errno.h>
#include <spawn.h>

struct NSAppArgs {
    int argc;
    char **argv;
};

#define JVM_DLL "libjvm.dylib"
#define JAVA_DLL "libjava.dylib"
/* FALLBACK avoids naming conflicts with system libraries
 * (eg, ImageIO's libJPEG.dylib) */
#define LD_LIBRARY_PATH "DYLD_FALLBACK_LIBRARY_PATH"

/*
 * If a processor / os combination has the ability to run binaries of
 * two data models and cohabitation of jre/jdk bits with both data
 * models is supported, then DUAL_MODE is defined. MacOSX is a hybrid
 * system in that, the universal library can contain all types of libraries
 * 32/64 and client/server, thus the spawn is capable of linking with the
 * appropriate library as requested.
 *
 * Notes:
 * 1. VM. DUAL_MODE is disabled, and not supported, however, it is left here in
 *    for experimentation and perhaps enable it in the future.
 * 2. At the time of this writing, the universal library contains only
 *    a server 64-bit server JVM.
 * 3. "-client" command line option is supported merely as a command line flag,
 *    for, compatibility reasons, however, a server VM will be launched.
 */

/*
 * Flowchart of launcher execs and options processing on unix
 *
 * The selection of the proper vm shared library to open depends on
 * several classes of command line options, including vm "flavor"
 * options (-client, -server) and the data model options, -d32  and
 * -d64, as well as a version specification which may have come from
 * the command line or from the manifest of an executable jar file.
 * The vm selection options are not passed to the running
 * virtual machine; they must be screened out by the launcher.
 *
 * The version specification (if any) is processed first by the
 * platform independent routine SelectVersion.  This may result in
 * the exec of the specified launcher version.
 *
 * Now, in most cases,the launcher will dlopen the target libjvm.so. All
 * required libraries are loaded by the runtime linker, using the known paths
 * baked into the shared libraries at compile time. Therefore,
 * in most cases, the launcher will only exec, if the data models are
 * mismatched, and will not set any environment variables, regardless of the
 * data models.
 *
 *
 *
 *  Main
 *  (incoming argv)
 *  |
 * \|/
 * CreateExecutionEnvironment
 * (determines desired data model)
 *  |
 *  |
 * \|/
 *  Have Desired Model ? --> NO --> Is Dual-Mode ? --> NO --> Exit(with error)
 *  |                                          |
 *  |                                          |
 *  |                                         \|/
 *  |                                         YES
 *  |                                          |
 *  |                                          |
 *  |                                         \|/
 *  |                                CheckJvmType
 *  |                               (removes -client, -server etc.)
 *  |                                          |
 *  |                                          |
 * \|/                                        \|/
 * YES                             Find the desired executable/library
 *  |                                          |
 *  |                                          |
 * \|/                                        \|/
 * CheckJvmType                             POINT A
 * (removes -client, -server, etc.)
 *  |
 *  |
 * \|/
 * TranslateDashJArgs...
 * (Prepare to pass args to vm)
 *  |
 *  |
 * \|/
 * ParseArguments
 * (processes version options,
 *  creates argument list for vm,
 *  etc.)
 *   |
 *   |
 *  \|/
 * POINT A
 *   |
 *   |
 *  \|/
 * Path is desired JRE ? YES --> Continue
 *  NO
 *   |
 *   |
 *  \|/
 * Paths have well known
 * jvm paths ?       --> NO --> Continue
 *  YES
 *   |
 *   |
 *  \|/
 *  Does libjvm.so exist
 *  in any of them ? --> NO --> Continue
 *   YES
 *   |
 *   |
 *  \|/
 * Re-exec / Spawn
 *   |
 *   |
 *  \|/
 * Main
 */

/* Store the name of the executable once computed */
static char *execname = NULL;

/*
 * execname accessor from other parts of platform dependent logic
 */
const char *
GetExecName() {
    return execname;
}

/*
 * Exports the JNI interface from libjli
 *
 * This allows client code to link against the .jre/.jdk bundles,
 * and not worry about trying to pick a HotSpot to link against.
 *
 * Switching architectures is unsupported, since client code has
 * made that choice before the JVM was requested.
 */

static InvocationFunctions *sExportedJNIFunctions = NULL;
static char *sPreferredJVMType = NULL;

static InvocationFunctions *GetExportedJNIFunctions() {
    if (sExportedJNIFunctions != NULL) return sExportedJNIFunctions;

    char jrePath[PATH_MAX];
    jboolean gotJREPath = GetJREPath(jrePath, sizeof(jrePath), JNI_FALSE);
    if (!gotJREPath) {
        JLI_ReportErrorMessage("Failed to GetJREPath()");
        return NULL;
    }

    char *preferredJVM = sPreferredJVMType;
    if (preferredJVM == NULL) {
#if defined(__i386__)
        preferredJVM = "client";
#elif defined(__x86_64__)
        preferredJVM = "server";
#elif defined(__aarch64__)
        preferredJVM = "server";
#else
#error "Unknown architecture - needs definition"
#endif
    }

    char jvmPath[PATH_MAX];
    jboolean gotJVMPath = GetJVMPath(jrePath, preferredJVM, jvmPath, sizeof(jvmPath));
    if (!gotJVMPath) {
        JLI_ReportErrorMessage("Failed to GetJVMPath()");
        return NULL;
    }

    InvocationFunctions *fxns = malloc(sizeof(InvocationFunctions));
    jboolean vmLoaded = LoadJavaVM(jvmPath, fxns);
    if (!vmLoaded) {
        JLI_ReportErrorMessage("Failed to LoadJavaVM()");
        return NULL;
    }

    return sExportedJNIFunctions = fxns;
}

#ifndef STATIC_BUILD

JNIEXPORT jint JNICALL
JNI_GetDefaultJavaVMInitArgs(void *args) {
    InvocationFunctions *ifn = GetExportedJNIFunctions();
    if (ifn == NULL) return JNI_ERR;
    return ifn->GetDefaultJavaVMInitArgs(args);
}

JNIEXPORT jint JNICALL
JNI_CreateJavaVM(JavaVM **pvm, void **penv, void *args) {
    InvocationFunctions *ifn = GetExportedJNIFunctions();
    if (ifn == NULL) return JNI_ERR;
    return ifn->CreateJavaVM(pvm, penv, args);
}

JNIEXPORT jint JNICALL
JNI_GetCreatedJavaVMs(JavaVM **vmBuf, jsize bufLen, jsize *nVMs) {
    InvocationFunctions *ifn = GetExportedJNIFunctions();
    if (ifn == NULL) return JNI_ERR;
    return ifn->GetCreatedJavaVMs(vmBuf, bufLen, nVMs);
}
#endif

/*
 * Allow JLI-aware launchers to specify a client/server preference
 */
JNIEXPORT void JNICALL
JLI_SetPreferredJVM(const char *prefJVM) {
    if (sPreferredJVMType != NULL) {
        free(sPreferredJVMType);
        sPreferredJVMType = NULL;
    }

    if (prefJVM == NULL) return;
    sPreferredJVMType = strdup(prefJVM);
}

static BOOL awtLoaded = NO;
static pthread_mutex_t awtLoaded_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t  awtLoaded_cv = PTHREAD_COND_INITIALIZER;

JNIEXPORT void JNICALL
JLI_NotifyAWTLoaded()
{
    pthread_mutex_lock(&awtLoaded_mutex);
    awtLoaded = YES;
    pthread_cond_signal(&awtLoaded_cv);
    pthread_mutex_unlock(&awtLoaded_mutex);
}

static int (*main_fptr)(int argc, char **argv) = NULL;

/*
 * Unwrap the arguments and re-run main()
 */
static void *apple_main (void *arg)
{
    if (main_fptr == NULL) {
#ifdef STATIC_BUILD
        extern int main(int argc, char **argv);
        main_fptr = &main;
#else
        main_fptr = (int (*)())dlsym(RTLD_DEFAULT, "main");
#endif
        if (main_fptr == NULL) {
            JLI_ReportErrorMessageSys("error locating main entrypoint\n");
            exit(1);
        }
    }

    struct NSAppArgs *args = (struct NSAppArgs *) arg;
    exit(main_fptr(args->argc, args->argv));
}

static void dummyTimer(CFRunLoopTimerRef timer, void *info) {}

static void ParkEventLoop() {
    // RunLoop needs at least one source, and 1e20 is pretty far into the future
    CFRunLoopTimerRef t = CFRunLoopTimerCreate(kCFAllocatorDefault, 1.0e20, 0.0, 0, 0, dummyTimer, NULL);
    CFRunLoopAddTimer(CFRunLoopGetCurrent(), t, kCFRunLoopDefaultMode);
    CFRelease(t);

    // Park this thread in the main run loop.
    int32_t result;
    do {
        result = CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1.0e20, false);
    } while (result != kCFRunLoopRunFinished);
}

/*
 * Mac OS X mandates that the GUI event loop run on very first thread of
 * an application. This requires that we re-call Java's main() on a new
 * thread, reserving the 'main' thread for Cocoa.
 */
static void MacOSXStartup(int argc, char *argv[]) {
    // Thread already started?
    static jboolean started = false;
    if (started) {
        return;
    }
    started = true;

    // Hand off arguments
    struct NSAppArgs args;
    args.argc = argc;
    args.argv = argv;

    // Fire up the main thread
    pthread_t main_thr;
    if (pthread_create(&main_thr, NULL, &apple_main, &args) != 0) {
        JLI_ReportErrorMessageSys("Could not create main thread: %s\n", strerror(errno));
        exit(1);
    }
    if (pthread_detach(main_thr)) {
        JLI_ReportErrorMessageSys("pthread_detach() failed: %s\n", strerror(errno));
        exit(1);
    }

    ParkEventLoop();
}

void
CreateExecutionEnvironment(int *pargc, char ***pargv,
                           char jrepath[], jint so_jrepath,
                           char jvmpath[], jint so_jvmpath,
                           char jvmcfg[],  jint so_jvmcfg) {
    jboolean jvmpathExists;

    /* Compute/set the name of the executable */
    SetExecname(*pargv);

    char * jvmtype    = NULL;
    int  argc         = *pargc;
    char **argv       = *pargv;

    /* Find out where the JRE is that we will be using. */
    if (!GetJREPath(jrepath, so_jrepath, JNI_FALSE) ) {
        JLI_ReportErrorMessage(JRE_ERROR1);
        exit(2);
    }
    JLI_Snprintf(jvmcfg, so_jvmcfg, "%s%slib%sjvm.cfg",
                 jrepath, FILESEP, FILESEP);
    /* Find the specified JVM type */
    if (ReadKnownVMs(jvmcfg, JNI_FALSE) < 1) {
        JLI_ReportErrorMessage(CFG_ERROR7);
        exit(1);
    }

    jvmpath[0] = '\0';
    jvmtype = CheckJvmType(pargc, pargv, JNI_FALSE);
    if (JLI_StrCmp(jvmtype, "ERROR") == 0) {
        JLI_ReportErrorMessage(CFG_ERROR9);
        exit(4);
    }

    if (!GetJVMPath(jrepath, jvmtype, jvmpath, so_jvmpath)) {
        JLI_ReportErrorMessage(CFG_ERROR8, jvmtype, jvmpath);
        exit(4);
    }

    /*
     * Mac OS X requires the Cocoa event loop to be run on the "main"
     * thread. Spawn off a new thread to run main() and pass
     * this thread off to the Cocoa event loop.
     */
    MacOSXStartup(argc, argv);

    /*
     * we seem to have everything we need
     */
    return;
}

/*
 * VM choosing is done by the launcher (java.c).
 */
static jboolean
GetJVMPath(const char *jrepath, const char *jvmtype,
           char *jvmpath, jint jvmpathsize)
{
    struct stat s;

    if (JLI_StrChr(jvmtype, '/')) {
        JLI_Snprintf(jvmpath, jvmpathsize, "%s/" JVM_DLL, jvmtype);
    } else {
        /*
         * macosx client library is built thin, i386 only.
         * 64 bit client requests must load server library
         */
        JLI_Snprintf(jvmpath, jvmpathsize, "%s/lib/%s/" JVM_DLL, jrepath, jvmtype);
    }

    JLI_TraceLauncher("Does `%s' exist ... ", jvmpath);

#ifdef STATIC_BUILD
    return JNI_TRUE;
#else
    if (stat(jvmpath, &s) == 0) {
        JLI_TraceLauncher("yes.\n");
        return JNI_TRUE;
    } else {
        JLI_TraceLauncher("no.\n");
        return JNI_FALSE;
    }
#endif
}

/*
 * Find path to JRE based on .exe's location or registry settings.
 */
static jboolean
GetJREPath(char *path, jint pathsize, jboolean speculative)
{
    char libjava[MAXPATHLEN];

    if (GetApplicationHome(path, pathsize)) {
        /* Is JRE co-located with the application? */
#ifdef STATIC_BUILD
        char jvm_cfg[MAXPATHLEN];
        JLI_Snprintf(jvm_cfg, sizeof(jvm_cfg), "%s/lib/jvm.cfg", path);
        if (access(jvm_cfg, F_OK) == 0) {
            return JNI_TRUE;
        }
#else
        JLI_Snprintf(libjava, sizeof(libjava), "%s/lib/" JAVA_DLL, path);
        if (access(libjava, F_OK) == 0) {
            return JNI_TRUE;
        }
#endif
        /* ensure storage for path + /jre + NULL */
        if ((JLI_StrLen(path) + 4 + 1) > (size_t) pathsize) {
            JLI_TraceLauncher("Insufficient space to store JRE path\n");
            return JNI_FALSE;
        }
        /* Does the app ship a private JRE in <apphome>/jre directory? */
        JLI_Snprintf(libjava, sizeof(libjava), "%s/jre/lib/" JAVA_DLL, path);
        if (access(libjava, F_OK) == 0) {
            JLI_StrCat(path, "/jre");
            JLI_TraceLauncher("JRE path is %s\n", path);
            return JNI_TRUE;
        }
    }

    /* try to find ourselves instead */
    Dl_info selfInfo;
    dladdr(&GetJREPath, &selfInfo);

#ifdef STATIC_BUILD
    char jvm_cfg[MAXPATHLEN];
    char *p = NULL;
    strncpy(jvm_cfg, selfInfo.dli_fname, MAXPATHLEN);
    p = strrchr(jvm_cfg, '/'); *p = '\0';
    p = strrchr(jvm_cfg, '/');
    if (strcmp(p, "/.") == 0) {
      *p = '\0';
      p = strrchr(jvm_cfg, '/'); *p = '\0';
    }
    else *p = '\0';
    strncpy(path, jvm_cfg, pathsize);
    strncat(jvm_cfg, "/lib/jvm.cfg", MAXPATHLEN);
    if (access(jvm_cfg, F_OK) == 0) {
      return JNI_TRUE;
    }
#endif

    char *realPathToSelf = realpath(selfInfo.dli_fname, path);
    if (realPathToSelf != path) {
        return JNI_FALSE;
    }

    size_t pathLen = strlen(realPathToSelf);
    if (pathLen == 0) {
        return JNI_FALSE;
    }

    const char lastPathComponent[] = "/lib/libjli.dylib";
    size_t sizeOfLastPathComponent = sizeof(lastPathComponent) - 1;
    if (pathLen < sizeOfLastPathComponent) {
        return JNI_FALSE;
    }

    size_t indexOfLastPathComponent = pathLen - sizeOfLastPathComponent;
    if (0 == strncmp(realPathToSelf + indexOfLastPathComponent, lastPathComponent, sizeOfLastPathComponent)) {
        realPathToSelf[indexOfLastPathComponent + 1] = '\0';
        return JNI_TRUE;
    }

    // If libjli.dylib is loaded from a macos bundle MacOS dir, find the JRE dir
    // in ../Home.
    const char altLastPathComponent[] = "/MacOS/libjli.dylib";
    size_t sizeOfAltLastPathComponent = sizeof(altLastPathComponent) - 1;
    if (pathLen < sizeOfLastPathComponent) {
        return JNI_FALSE;
    }

    size_t indexOfAltLastPathComponent = pathLen - sizeOfAltLastPathComponent;
    if (0 == strncmp(realPathToSelf + indexOfAltLastPathComponent, altLastPathComponent, sizeOfAltLastPathComponent)) {
        JLI_Snprintf(realPathToSelf + indexOfAltLastPathComponent, sizeOfAltLastPathComponent, "%s", "/Home");
        if (access(realPathToSelf, F_OK) == 0) {
            return JNI_TRUE;
        }
    }

    if (!speculative)
      JLI_ReportErrorMessage(JRE_ERROR8 JAVA_DLL);
    return JNI_FALSE;
}

jboolean
LoadJavaVM(const char *jvmpath, InvocationFunctions *ifn)
{
    Dl_info dlinfo;
    void *libjvm;

    JLI_TraceLauncher("JVM path is %s\n", jvmpath);

#ifndef STATIC_BUILD
    libjvm = dlopen(jvmpath, RTLD_NOW + RTLD_GLOBAL);
#else
    libjvm = dlopen(NULL, RTLD_FIRST);
#endif
    if (libjvm == NULL) {
        JLI_ReportErrorMessage(DLL_ERROR1, __LINE__);
        JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
        return JNI_FALSE;
    }

    ifn->CreateJavaVM = (CreateJavaVM_t)
        dlsym(libjvm, "JNI_CreateJavaVM");
    if (ifn->CreateJavaVM == NULL) {
        JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
        return JNI_FALSE;
    }

    ifn->GetDefaultJavaVMInitArgs = (GetDefaultJavaVMInitArgs_t)
        dlsym(libjvm, "JNI_GetDefaultJavaVMInitArgs");
    if (ifn->GetDefaultJavaVMInitArgs == NULL) {
        JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
        return JNI_FALSE;
    }

    ifn->GetCreatedJavaVMs = (GetCreatedJavaVMs_t)
    dlsym(libjvm, "JNI_GetCreatedJavaVMs");
    if (ifn->GetCreatedJavaVMs == NULL) {
        JLI_ReportErrorMessage(DLL_ERROR2, jvmpath, dlerror());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Compute the name of the executable
 *
 * In order to re-exec securely we need the absolute path of the
 * executable. On Solaris getexecname(3c) may not return an absolute
 * path so we use dladdr to get the filename of the executable and
 * then use realpath to derive an absolute path. From Solaris 9
 * onwards the filename returned in DL_info structure from dladdr is
 * an absolute pathname so technically realpath isn't required.
 * On Linux we read the executable name from /proc/self/exe.
 * As a fallback, and for platforms other than Solaris and Linux,
 * we use FindExecName to compute the executable name.
 */
const char*
SetExecname(char **argv)
{
    char* exec_path = NULL;
    {
        Dl_info dlinfo;

#ifdef STATIC_BUILD
        void *fptr;
        fptr = (void *)&SetExecname;
#else
        int (*fptr)();
        fptr = (int (*)())dlsym(RTLD_DEFAULT, "main");
#endif
        if (fptr == NULL) {
            JLI_ReportErrorMessage(DLL_ERROR3, dlerror());
            return JNI_FALSE;
        }

        if (dladdr((void*)fptr, &dlinfo)) {
            char *resolved = (char*)JLI_MemAlloc(PATH_MAX+1);
            if (resolved != NULL) {
                exec_path = realpath(dlinfo.dli_fname, resolved);
                if (exec_path == NULL) {
                    JLI_MemFree(resolved);
                }
            }
        }
    }
    if (exec_path == NULL) {
        exec_path = FindExecName(argv[0]);
    }
    execname = exec_path;
    return exec_path;
}

/* --- Splash Screen shared library support --- */

static JavaVM* SetJavaVMValue()
{
    JavaVM * jvm = NULL;

    // The handle is good for both the launcher and the libosxapp.dylib
    void * handle = dlopen(NULL, RTLD_LAZY | RTLD_GLOBAL);
    if (handle) {
        typedef JavaVM* (*JLI_GetJavaVMInstance_t)();

        JLI_GetJavaVMInstance_t JLI_GetJavaVMInstance =
            (JLI_GetJavaVMInstance_t)dlsym(handle,
                    "JLI_GetJavaVMInstance");
        if (JLI_GetJavaVMInstance) {
            jvm = JLI_GetJavaVMInstance();
        }

        if (jvm) {
            typedef void (*OSXAPP_SetJavaVM_t)(JavaVM*);

            OSXAPP_SetJavaVM_t OSXAPP_SetJavaVM =
                (OSXAPP_SetJavaVM_t)dlsym(handle, "OSXAPP_SetJavaVM");
            if (OSXAPP_SetJavaVM) {
                OSXAPP_SetJavaVM(jvm);
            } else {
                jvm = NULL;
            }
        }

        dlclose(handle);
    }

    return jvm;
}

static const char* SPLASHSCREEN_SO = JNI_LIB_NAME("splashscreen");

static void* hSplashLib = NULL;

void* SplashProcAddress(const char* name) {
    if (!hSplashLib) {
        char jrePath[PATH_MAX];
        if (!GetJREPath(jrePath, sizeof(jrePath), JNI_FALSE)) {
            JLI_ReportErrorMessage(JRE_ERROR1);
            return NULL;
        }

        char splashPath[PATH_MAX];
        const int ret = JLI_Snprintf(splashPath, sizeof(splashPath),
                "%s/lib/%s", jrePath, SPLASHSCREEN_SO);
        if (ret >= (int)sizeof(splashPath)) {
            JLI_ReportErrorMessage(JRE_ERROR11);
            return NULL;
        }
        if (ret < 0) {
            JLI_ReportErrorMessage(JRE_ERROR13);
            return NULL;
        }

        hSplashLib = dlopen(splashPath, RTLD_LAZY | RTLD_GLOBAL);
        // It's OK if dlopen() fails. The splash screen library binary file
        // might have been stripped out from the JRE image to reduce its size
        // (e.g. on embedded platforms).

        if (hSplashLib) {
            if (!SetJavaVMValue()) {
                dlclose(hSplashLib);
                hSplashLib = NULL;
            }
        }
    }
    if (hSplashLib) {
        void* sym = dlsym(hSplashLib, name);
        return sym;
    } else {
        return NULL;
    }
}

/*
 * Signature adapter for pthread_create().
 */
static void* ThreadJavaMain(void* args) {
    return (void*)(intptr_t)JavaMain(args);
}

/*
 * Block current thread and continue execution in a new thread.
 */
int
CallJavaMainInNewThread(jlong stack_size, void* args) {
    int rslt;
    pthread_t tid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    if (stack_size > 0) {
        pthread_attr_setstacksize(&attr, stack_size);
    }
    pthread_attr_setguardsize(&attr, 0); // no pthread guard page on java threads

    if (pthread_create(&tid, &attr, ThreadJavaMain, args) == 0) {
        void* tmp;
        pthread_join(tid, &tmp);
        rslt = (int)(intptr_t)tmp;
    } else {
       /*
        * Continue execution in current thread if for some reason (e.g. out of
        * memory/LWP)  a new thread can't be created. This will likely fail
        * later in JavaMain as JNI_CreateJavaVM needs to create quite a
        * few new threads, anyway, just give it a try..
        */
        rslt = JavaMain(args);
    }

    pthread_attr_destroy(&attr);
    return rslt;
}

static JavaVM* jvmInstance = NULL;
static jboolean sameThread = JNI_FALSE; /* start VM in current thread */

/*
 * Note there is a callback on this function from the splashscreen logic,
 * this as well SetJavaVMValue() needs to be simplified.
 */
JNIEXPORT JavaVM* JNICALL
JLI_GetJavaVMInstance()
{
    return jvmInstance;
}

void
RegisterThread()
{
    // stubbed out for windows and *nixes.
}

static void
SetXDockArgForAWT(const char *arg)
{
    char envVar[80];
    if (strstr(arg, "-Xdock:name=") == arg) {
        /*
         * The APP_NAME_<pid> environment variable is used to pass
         * an application name as specified with the -Xdock:name command
         * line option from Java launcher code to the AWT code in order
         * to assign this name to the app's dock tile on the Mac.
         * The _<pid> part is added to avoid collisions with child processes.
         *
         * WARNING: This environment variable is an implementation detail and
         * isn't meant for use outside of the core platform. The mechanism for
         * passing this information from Java launcher to other modules may
         * change drastically between update release, and it may even be
         * removed or replaced with another mechanism.
         *
         * NOTE: It is used by SWT, and JavaFX.
         */
        snprintf(envVar, sizeof(envVar), "APP_NAME_%d", getpid());
        setenv(envVar, (arg + 12), 1);
    }

    if (strstr(arg, "-Xdock:icon=") == arg) {
        /*
         * The APP_ICON_<pid> environment variable is used to pass
         * an application icon as specified with the -Xdock:icon command
         * line option from Java launcher code to the AWT code in order
         * to assign this icon to the app's dock tile on the Mac.
         * The _<pid> part is added to avoid collisions with child processes.
         *
         * WARNING: This environment variable is an implementation detail and
         * isn't meant for use outside of the core platform. The mechanism for
         * passing this information from Java launcher to other modules may
         * change drastically between update release, and it may even be
         * removed or replaced with another mechanism.
         *
         * NOTE: It is used by SWT, and JavaFX.
         */
        snprintf(envVar, sizeof(envVar), "APP_ICON_%d", getpid());
        setenv(envVar, (arg + 12), 1);
    }
}

static void
SetMainClassForAWT(JNIEnv *env, jclass mainClass) {
    jclass classClass = NULL;
    NULL_CHECK(classClass = FindBootStrapClass(env, "java/lang/Class"));

    jmethodID getCanonicalNameMID = NULL;
    NULL_CHECK(getCanonicalNameMID = (*env)->GetMethodID(env, classClass, "getCanonicalName", "()Ljava/lang/String;"));

    jstring mainClassString = (*env)->CallObjectMethod(env, mainClass, getCanonicalNameMID);
    if ((*env)->ExceptionCheck(env) || NULL == mainClassString) {
        /*
         * Clears all errors caused by getCanonicalName() on the mainclass and
         * leaves the JAVA_MAIN_CLASS__<pid> empty.
         */
        (*env)->ExceptionClear(env);
        return;
    }

    const char *mainClassName = NULL;
    NULL_CHECK(mainClassName = (*env)->GetStringUTFChars(env, mainClassString, NULL));

    char envVar[80];
    /*
     * The JAVA_MAIN_CLASS_<pid> environment variable is used to pass
     * the name of a Java class whose main() method is invoked by
     * the Java launcher code to start the application, to the AWT code
     * in order to assign the name to the Apple menu bar when the app
     * is active on the Mac.
     * The _<pid> part is added to avoid collisions with child processes.
     *
     * WARNING: This environment variable is an implementation detail and
     * isn't meant for use outside of the core platform. The mechanism for
     * passing this information from Java launcher to other modules may
     * change drastically between update release, and it may even be
     * removed or replaced with another mechanism.
     *
     * NOTE: It is used by SWT, and JavaFX.
     */
    snprintf(envVar, sizeof(envVar), "JAVA_MAIN_CLASS_%d", getpid());
    setenv(envVar, mainClassName, 1);

    (*env)->ReleaseStringUTFChars(env, mainClassString, mainClassName);
}

void
SetXStartOnFirstThreadArg()
{
    // XXX: BEGIN HACK
    // short circuit hack for <https://bugs.eclipse.org/bugs/show_bug.cgi?id=211625>
    // need a way to get AWT/Swing apps launched when spawned from Eclipse,
    // which currently has no UI to not pass the -XstartOnFirstThread option
    if (getenv("HACK_IGNORE_START_ON_FIRST_THREAD") != NULL) return;
    // XXX: END HACK

    sameThread = JNI_TRUE;
    // Set a variable that tells us we started on the main thread.
    // This is used by the AWT during startup. (See LWCToolkit.m)
    char envVar[80];
    snprintf(envVar, sizeof(envVar), "JAVA_STARTED_ON_FIRST_THREAD_%d", getpid());
    setenv(envVar, "1", 1);
}

// MacOSX we may continue in the same thread
int
JVMInit(InvocationFunctions* ifn, jlong threadStackSize,
                 int argc, char **argv,
                 int mode, char *what, int ret) {
    if (sameThread) {
        JLI_TraceLauncher("In same thread\n");
        // need to block this thread against the main thread
        // so signals get caught correctly
        __block int rslt = 0;
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        {
            NSBlockOperation *op = [NSBlockOperation blockOperationWithBlock: ^{
                JavaMainArgs args;
                args.argc = argc;
                args.argv = argv;
                args.mode = mode;
                args.what = what;
                args.ifn  = *ifn;
                rslt = JavaMain(&args);
            }];

            /*
             * We cannot use dispatch_sync here, because it blocks the main dispatch queue.
             * Using the main NSRunLoop allows the dispatch queue to run properly once
             * SWT (or whatever toolkit this is needed for) kicks off it's own NSRunLoop
             * and starts running.
             */
            [op performSelectorOnMainThread:@selector(start) withObject:nil waitUntilDone:YES];
        }
        [pool drain];
        return rslt;
    } else {
        return ContinueInNewThread(ifn, threadStackSize, argc, argv, mode, what, ret);
    }
}

/*
 * Note the jvmInstance must be initialized first before entering into
 * ShowSplashScreen, as there is a callback into the JLI_GetJavaVMInstance.
 */
void PostJVMInit(JNIEnv *env, jclass mainClass, JavaVM *vm) {
    jvmInstance = vm;
    SetMainClassForAWT(env, mainClass);
    CHECK_EXCEPTION_RETURN();
    ShowSplashScreen();
}

jboolean
ProcessPlatformOption(const char* arg)
{
    if (JLI_StrCmp(arg, "-XstartOnFirstThread") == 0) {
       SetXStartOnFirstThreadArg();
       return JNI_TRUE;
    } else if (JLI_StrCCmp(arg, "-Xdock:") == 0) {
       SetXDockArgForAWT(arg);
       return JNI_TRUE;
    }
    // arguments we know not
    return JNI_FALSE;
}
