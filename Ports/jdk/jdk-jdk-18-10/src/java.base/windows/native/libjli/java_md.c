/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <io.h>
#include <process.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <wtypes.h>
#include <commctrl.h>
#include <assert.h>

#include <jni.h>
#include "java.h"

#define JVM_DLL "jvm.dll"
#define JAVA_DLL "java.dll"

/*
 * Prototypes.
 */
static jboolean GetJVMPath(const char *jrepath, const char *jvmtype,
                           char *jvmpath, jint jvmpathsize);
static jboolean GetJREPath(char *path, jint pathsize);

#ifdef USE_REGISTRY_LOOKUP
jboolean GetPublicJREHome(char *buf, jint bufsize);
#endif

/* We supports warmup for UI stack that is performed in parallel
 * to VM initialization.
 * This helps to improve startup of UI application as warmup phase
 * might be long due to initialization of OS or hardware resources.
 * It is not CPU bound and therefore it does not interfere with VM init.
 * Obviously such warmup only has sense for UI apps and therefore it needs
 * to be explicitly requested by passing -Dsun.awt.warmup=true property
 * (this is always the case for plugin/javaws).
 *
 * Implementation launches new thread after VM starts and use it to perform
 * warmup code (platform dependent).
 * This thread is later reused as AWT toolkit thread as graphics toolkit
 * often assume that they are used from the same thread they were launched on.
 *
 * At the moment we only support warmup for D3D. It only possible on windows
 * and only if other flags do not prohibit this (e.g. OpenGL support requested).
 */
#undef ENABLE_AWT_PRELOAD
#ifndef JAVA_ARGS /* turn off AWT preloading for javac, jar, etc */
    /* CR6999872: fastdebug crashes if awt library is loaded before JVM is
     * initialized*/
    #if !defined(DEBUG)
        #define ENABLE_AWT_PRELOAD
    #endif
#endif

#ifdef ENABLE_AWT_PRELOAD
/* "AWT was preloaded" flag;
 * turned on by AWTPreload().
 */
int awtPreloaded = 0;

/* Calls a function with the name specified
 * the function must be int(*fn)(void).
 */
int AWTPreload(const char *funcName);
/* stops AWT preloading */
void AWTPreloadStop();

/* D3D preloading */
/* -1: not initialized; 0: OFF, 1: ON */
int awtPreloadD3D = -1;
/* command line parameter to swith D3D preloading on */
#define PARAM_PRELOAD_D3D "-Dsun.awt.warmup"
/* D3D/OpenGL management parameters */
#define PARAM_NODDRAW "-Dsun.java2d.noddraw"
#define PARAM_D3D "-Dsun.java2d.d3d"
#define PARAM_OPENGL "-Dsun.java2d.opengl"
/* funtion in awt.dll (src/windows/native/sun/java2d/d3d/D3DPipelineManager.cpp) */
#define D3D_PRELOAD_FUNC "preloadD3D"

/* Extracts value of a parameter with the specified name
 * from command line argument (returns pointer in the argument).
 * Returns NULL if the argument does not contains the parameter.
 * e.g.:
 * GetParamValue("theParam", "theParam=value") returns pointer to "value".
 */
const char * GetParamValue(const char *paramName, const char *arg) {
    size_t nameLen = JLI_StrLen(paramName);
    if (JLI_StrNCmp(paramName, arg, nameLen) == 0) {
        /* arg[nameLen] is valid (may contain final NULL) */
        if (arg[nameLen] == '=') {
            return arg + nameLen + 1;
        }
    }
    return NULL;
}

/* Checks if commandline argument contains property specified
 * and analyze it as boolean property (true/false).
 * Returns -1 if the argument does not contain the parameter;
 * Returns 1 if the argument contains the parameter and its value is "true";
 * Returns 0 if the argument contains the parameter and its value is "false".
 */
int GetBoolParamValue(const char *paramName, const char *arg) {
    const char * paramValue = GetParamValue(paramName, arg);
    if (paramValue != NULL) {
        if (JLI_StrCaseCmp(paramValue, "true") == 0) {
            return 1;
        }
        if (JLI_StrCaseCmp(paramValue, "false") == 0) {
            return 0;
        }
    }
    return -1;
}
#endif /* ENABLE_AWT_PRELOAD */


static jboolean _isjavaw = JNI_FALSE;


jboolean
IsJavaw()
{
    return _isjavaw;
}

/*
 *
 */
void
CreateExecutionEnvironment(int *pargc, char ***pargv,
                           char *jrepath, jint so_jrepath,
                           char *jvmpath, jint so_jvmpath,
                           char *jvmcfg,  jint so_jvmcfg) {

    char *jvmtype;
    int i = 0;
    char** argv = *pargv;

    /* Find out where the JRE is that we will be using. */
    if (!GetJREPath(jrepath, so_jrepath)) {
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

    jvmtype = CheckJvmType(pargc, pargv, JNI_FALSE);
    if (JLI_StrCmp(jvmtype, "ERROR") == 0) {
        JLI_ReportErrorMessage(CFG_ERROR9);
        exit(4);
    }

    jvmpath[0] = '\0';
    if (!GetJVMPath(jrepath, jvmtype, jvmpath, so_jvmpath)) {
        JLI_ReportErrorMessage(CFG_ERROR8, jvmtype, jvmpath);
        exit(4);
    }
    /* If we got here, jvmpath has been correctly initialized. */

    /* Check if we need preload AWT */
#ifdef ENABLE_AWT_PRELOAD
    argv = *pargv;
    for (i = 0; i < *pargc ; i++) {
        /* Tests the "turn on" parameter only if not set yet. */
        if (awtPreloadD3D < 0) {
            if (GetBoolParamValue(PARAM_PRELOAD_D3D, argv[i]) == 1) {
                awtPreloadD3D = 1;
            }
        }
        /* Test parameters which can disable preloading if not already disabled. */
        if (awtPreloadD3D != 0) {
            if (GetBoolParamValue(PARAM_NODDRAW, argv[i]) == 1
                || GetBoolParamValue(PARAM_D3D, argv[i]) == 0
                || GetBoolParamValue(PARAM_OPENGL, argv[i]) == 1)
            {
                awtPreloadD3D = 0;
                /* no need to test the rest of the parameters */
                break;
            }
        }
    }
#endif /* ENABLE_AWT_PRELOAD */
}


static jboolean
LoadMSVCRT()
{
    // Only do this once
    static int loaded = 0;
    char crtpath[MAXPATHLEN];

    if (!loaded) {
        /*
         * The Microsoft C Runtime Library needs to be loaded first.  A copy is
         * assumed to be present in the "JRE path" directory.  If it is not found
         * there (or "JRE path" fails to resolve), skip the explicit load and let
         * nature take its course, which is likely to be a failure to execute.
         * The makefiles will provide the correct lib contained in quotes in the
         * macro MSVCR_DLL_NAME.
         */
#ifdef MSVCR_DLL_NAME
        if (GetJREPath(crtpath, MAXPATHLEN)) {
            if (JLI_StrLen(crtpath) + JLI_StrLen("\\bin\\") +
                    JLI_StrLen(MSVCR_DLL_NAME) >= MAXPATHLEN) {
                JLI_ReportErrorMessage(JRE_ERROR11);
                return JNI_FALSE;
            }
            (void)JLI_StrCat(crtpath, "\\bin\\" MSVCR_DLL_NAME);   /* Add crt dll */
            JLI_TraceLauncher("CRT path is %s\n", crtpath);
            if (_access(crtpath, 0) == 0) {
                if (LoadLibrary(crtpath) == 0) {
                    JLI_ReportErrorMessage(DLL_ERROR4, crtpath);
                    return JNI_FALSE;
                }
            }
        }
#endif /* MSVCR_DLL_NAME */
#ifdef VCRUNTIME_1_DLL_NAME
        if (GetJREPath(crtpath, MAXPATHLEN)) {
            if (JLI_StrLen(crtpath) + JLI_StrLen("\\bin\\") +
                    JLI_StrLen(VCRUNTIME_1_DLL_NAME) >= MAXPATHLEN) {
                JLI_ReportErrorMessage(JRE_ERROR11);
                return JNI_FALSE;
            }
            (void)JLI_StrCat(crtpath, "\\bin\\" VCRUNTIME_1_DLL_NAME);   /* Add crt dll */
            JLI_TraceLauncher("CRT path is %s\n", crtpath);
            if (_access(crtpath, 0) == 0) {
                if (LoadLibrary(crtpath) == 0) {
                    JLI_ReportErrorMessage(DLL_ERROR4, crtpath);
                    return JNI_FALSE;
                }
            }
        }
#endif /* VCRUNTIME_1_DLL_NAME */
#ifdef MSVCP_DLL_NAME
        if (GetJREPath(crtpath, MAXPATHLEN)) {
            if (JLI_StrLen(crtpath) + JLI_StrLen("\\bin\\") +
                    JLI_StrLen(MSVCP_DLL_NAME) >= MAXPATHLEN) {
                JLI_ReportErrorMessage(JRE_ERROR11);
                return JNI_FALSE;
            }
            (void)JLI_StrCat(crtpath, "\\bin\\" MSVCP_DLL_NAME);   /* Add prt dll */
            JLI_TraceLauncher("PRT path is %s\n", crtpath);
            if (_access(crtpath, 0) == 0) {
                if (LoadLibrary(crtpath) == 0) {
                    JLI_ReportErrorMessage(DLL_ERROR4, crtpath);
                    return JNI_FALSE;
                }
            }
        }
#endif /* MSVCP_DLL_NAME */
        loaded = 1;
    }
    return JNI_TRUE;
}


/*
 * Find path to JRE based on .exe's location or registry settings.
 */
jboolean
GetJREPath(char *path, jint pathsize)
{
    char javadll[MAXPATHLEN];
    struct stat s;

    if (GetApplicationHome(path, pathsize)) {
        /* Is JRE co-located with the application? */
        JLI_Snprintf(javadll, sizeof(javadll), "%s\\bin\\" JAVA_DLL, path);
        if (stat(javadll, &s) == 0) {
            JLI_TraceLauncher("JRE path is %s\n", path);
            return JNI_TRUE;
        }
        /* ensure storage for path + \jre + NULL */
        if ((JLI_StrLen(path) + 4 + 1) > (size_t) pathsize) {
            JLI_TraceLauncher("Insufficient space to store JRE path\n");
            return JNI_FALSE;
        }
        /* Does this app ship a private JRE in <apphome>\jre directory? */
        JLI_Snprintf(javadll, sizeof (javadll), "%s\\jre\\bin\\" JAVA_DLL, path);
        if (stat(javadll, &s) == 0) {
            JLI_StrCat(path, "\\jre");
            JLI_TraceLauncher("JRE path is %s\n", path);
            return JNI_TRUE;
        }
    }

    /* Try getting path to JRE from path to JLI.DLL */
    if (GetApplicationHomeFromDll(path, pathsize)) {
        JLI_Snprintf(javadll, sizeof(javadll), "%s\\bin\\" JAVA_DLL, path);
        if (stat(javadll, &s) == 0) {
            JLI_TraceLauncher("JRE path is %s\n", path);
            return JNI_TRUE;
        }
    }

#ifdef USE_REGISTRY_LOOKUP
    /* Lookup public JRE using Windows registry. */
    if (GetPublicJREHome(path, pathsize)) {
        JLI_TraceLauncher("JRE path is %s\n", path);
        return JNI_TRUE;
    }
#endif

    JLI_ReportErrorMessage(JRE_ERROR8 JAVA_DLL);
    return JNI_FALSE;
}

/*
 * Given a JRE location and a JVM type, construct what the name the
 * JVM shared library will be.  Return true, if such a library
 * exists, false otherwise.
 */
static jboolean
GetJVMPath(const char *jrepath, const char *jvmtype,
           char *jvmpath, jint jvmpathsize)
{
    struct stat s;
    if (JLI_StrChr(jvmtype, '/') || JLI_StrChr(jvmtype, '\\')) {
        JLI_Snprintf(jvmpath, jvmpathsize, "%s\\" JVM_DLL, jvmtype);
    } else {
        JLI_Snprintf(jvmpath, jvmpathsize, "%s\\bin\\%s\\" JVM_DLL,
                     jrepath, jvmtype);
    }
    if (stat(jvmpath, &s) == 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

/*
 * Load a jvm from "jvmpath" and initialize the invocation functions.
 */
jboolean
LoadJavaVM(const char *jvmpath, InvocationFunctions *ifn)
{
    HINSTANCE handle;

    JLI_TraceLauncher("JVM path is %s\n", jvmpath);

    /*
     * The Microsoft C Runtime Library needs to be loaded first.  A copy is
     * assumed to be present in the "JRE path" directory.  If it is not found
     * there (or "JRE path" fails to resolve), skip the explicit load and let
     * nature take its course, which is likely to be a failure to execute.
     *
     */
    LoadMSVCRT();

    /* Load the Java VM DLL */
    if ((handle = LoadLibrary(jvmpath)) == 0) {
        JLI_ReportErrorMessage(DLL_ERROR4, (char *)jvmpath);
        return JNI_FALSE;
    }

    /* Now get the function addresses */
    ifn->CreateJavaVM =
        (void *)GetProcAddress(handle, "JNI_CreateJavaVM");
    ifn->GetDefaultJavaVMInitArgs =
        (void *)GetProcAddress(handle, "JNI_GetDefaultJavaVMInitArgs");
    if (ifn->CreateJavaVM == 0 || ifn->GetDefaultJavaVMInitArgs == 0) {
        JLI_ReportErrorMessage(JNI_ERROR1, (char *)jvmpath);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Removes the trailing file name and one sub-folder from a path.
 * If buf is "c:\foo\bin\javac", then put "c:\foo" into buf.
 */
jboolean
TruncatePath(char *buf)
{
    char *cp;
    *JLI_StrRChr(buf, '\\') = '\0'; /* remove .exe file name */
    if ((cp = JLI_StrRChr(buf, '\\')) == 0) {
        /* This happens if the application is in a drive root, and
         * there is no bin directory. */
        buf[0] = '\0';
        return JNI_FALSE;
    }
    *cp = '\0'; /* remove the bin\ part */
    return JNI_TRUE;
}

/*
 * Retrieves the path to the JRE home by locating the executable file
 * of the current process and then truncating the path to the executable
 */
jboolean
GetApplicationHome(char *buf, jint bufsize)
{
    GetModuleFileName(NULL, buf, bufsize);
    return TruncatePath(buf);
}

/*
 * Retrieves the path to the JRE home by locating JLI.DLL and
 * then truncating the path to JLI.DLL
 */
jboolean
GetApplicationHomeFromDll(char *buf, jint bufsize)
{
    HMODULE module;
    DWORD flags = GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
                  GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT;

    if (GetModuleHandleEx(flags, (LPCSTR)&GetJREPath, &module) != 0) {
        if (GetModuleFileName(module, buf, bufsize) != 0) {
            return TruncatePath(buf);
        }
    }
    return JNI_FALSE;
}

/*
 * Support for doing cheap, accurate interval timing.
 */
static jboolean counterAvailable = JNI_FALSE;
static jboolean counterInitialized = JNI_FALSE;
static LARGE_INTEGER counterFrequency;

jlong CurrentTimeMicros()
{
    LARGE_INTEGER count;

    if (!counterInitialized) {
        counterAvailable = QueryPerformanceFrequency(&counterFrequency);
        counterInitialized = JNI_TRUE;
    }
    if (!counterAvailable) {
        return 0;
    }
    QueryPerformanceCounter(&count);

    return (jlong)(count.QuadPart * 1000 * 1000 / counterFrequency.QuadPart);
}

/*
 * windows snprintf does not guarantee a null terminator in the buffer,
 * if the computed size is equal to or greater than the buffer size,
 * as well as error conditions. This function guarantees a null terminator
 * under all these conditions. An unreasonable buffer or size will return
 * an error value. Under all other conditions this function will return the
 * size of the bytes actually written minus the null terminator, similar
 * to ansi snprintf api. Thus when calling this function the caller must
 * ensure storage for the null terminator.
 */
int
JLI_Snprintf(char* buffer, size_t size, const char* format, ...) {
    int rc;
    va_list vl;
    if (size == 0 || buffer == NULL)
        return -1;
    buffer[0] = '\0';
    va_start(vl, format);
    rc = vsnprintf(buffer, size, format, vl);
    va_end(vl);
    /* force a null terminator, if something is amiss */
    if (rc < 0) {
        /* apply ansi semantics */
        buffer[size - 1] = '\0';
        return (int)size;
    } else if (rc == size) {
        /* force a null terminator */
        buffer[size - 1] = '\0';
    }
    return rc;
}

static errno_t convert_to_unicode(const char* path, const wchar_t* prefix, wchar_t** wpath) {
    int unicode_path_len;
    size_t prefix_len, wpath_len;

    /*
     * Get required buffer size to convert to Unicode.
     * The return value includes the terminating null character.
     */
    unicode_path_len = MultiByteToWideChar(CP_ACP, MB_ERR_INVALID_CHARS,
                                           path, -1, NULL, 0);
    if (unicode_path_len == 0) {
        return EINVAL;
    }

    prefix_len = wcslen(prefix);
    wpath_len = prefix_len + unicode_path_len;
    *wpath = (wchar_t*)JLI_MemAlloc(wpath_len * sizeof(wchar_t));
    if (*wpath == NULL) {
        return ENOMEM;
    }

    wcsncpy(*wpath, prefix, prefix_len);
    if (MultiByteToWideChar(CP_ACP, MB_ERR_INVALID_CHARS,
                            path, -1, &((*wpath)[prefix_len]), (int)wpath_len) == 0) {
        JLI_MemFree(*wpath);
        *wpath = NULL;
        return EINVAL;
    }

    return ERROR_SUCCESS;
}

/* taken from hotspot and slightly adjusted for jli lib;
 * creates a UNC/ELP path from input 'path'
 * the return buffer is allocated in C heap and needs to be freed using
 * JLI_MemFree by the caller.
 */
static wchar_t* create_unc_path(const char* path, errno_t* err) {
    wchar_t* wpath = NULL;
    size_t converted_chars = 0;
    size_t path_len = strlen(path) + 1; /* includes the terminating NULL */
    if (path[0] == '\\' && path[1] == '\\') {
        if (path[2] == '?' && path[3] == '\\') {
            /* if it already has a \\?\ don't do the prefix */
            *err = convert_to_unicode(path, L"", &wpath);
        } else {
            /* only UNC pathname includes double slashes here */
            *err = convert_to_unicode(path, L"\\\\?\\UNC", &wpath);
        }
    } else {
        *err = convert_to_unicode(path, L"\\\\?\\", &wpath);
    }
    return wpath;
}

int JLI_Open(const char* name, int flags) {
    int fd;
    if (strlen(name) < MAX_PATH) {
        fd = _open(name, flags);
    } else {
        errno_t err = ERROR_SUCCESS;
        wchar_t* wpath = create_unc_path(name, &err);
        if (err != ERROR_SUCCESS) {
            if (wpath != NULL) JLI_MemFree(wpath);
            errno = err;
            return -1;
        }
        fd = _wopen(wpath, flags);
        if (fd == -1) {
            errno = GetLastError();
        }
        JLI_MemFree(wpath);
    }
    return fd;
}

JNIEXPORT void JNICALL
JLI_ReportErrorMessage(const char* fmt, ...) {
    va_list vl;
    va_start(vl,fmt);

    if (IsJavaw()) {
        char *message;

        /* get the length of the string we need */
        int n = _vscprintf(fmt, vl);

        message = (char *)JLI_MemAlloc(n + 1);
        _vsnprintf(message, n, fmt, vl);
        message[n]='\0';
        MessageBox(NULL, message, "Java Virtual Machine Launcher",
            (MB_OK|MB_ICONSTOP|MB_APPLMODAL));
        JLI_MemFree(message);
    } else {
        vfprintf(stderr, fmt, vl);
        fprintf(stderr, "\n");
    }
    va_end(vl);
}

/*
 * Just like JLI_ReportErrorMessage, except that it concatenates the system
 * error message if any, its upto the calling routine to correctly
 * format the separation of the messages.
 */
JNIEXPORT void JNICALL
JLI_ReportErrorMessageSys(const char *fmt, ...)
{
    va_list vl;

    int save_errno = errno;
    DWORD       errval;
    jboolean freeit = JNI_FALSE;
    char  *errtext = NULL;

    va_start(vl, fmt);

    if ((errval = GetLastError()) != 0) {               /* Platform SDK / DOS Error */
        int n = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM|
            FORMAT_MESSAGE_IGNORE_INSERTS|FORMAT_MESSAGE_ALLOCATE_BUFFER,
            NULL, errval, 0, (LPTSTR)&errtext, 0, NULL);
        if (errtext == NULL || n == 0) {                /* Paranoia check */
            errtext = "";
            n = 0;
        } else {
            freeit = JNI_TRUE;
            if (n > 2) {                                /* Drop final CR, LF */
                if (errtext[n - 1] == '\n') n--;
                if (errtext[n - 1] == '\r') n--;
                errtext[n] = '\0';
            }
        }
    } else {   /* C runtime error that has no corresponding DOS error code */
        errtext = strerror(save_errno);
    }

    if (IsJavaw()) {
        char *message;
        int mlen;
        /* get the length of the string we need */
        int len = mlen =  _vscprintf(fmt, vl) + 1;
        if (freeit) {
           mlen += (int)JLI_StrLen(errtext);
        }

        message = (char *)JLI_MemAlloc(mlen);
        _vsnprintf(message, len, fmt, vl);
        message[len]='\0';

        if (freeit) {
           JLI_StrCat(message, errtext);
        }

        MessageBox(NULL, message, "Java Virtual Machine Launcher",
            (MB_OK|MB_ICONSTOP|MB_APPLMODAL));

        JLI_MemFree(message);
    } else {
        vfprintf(stderr, fmt, vl);
        if (freeit) {
           fprintf(stderr, "%s", errtext);
        }
    }
    if (freeit) {
        (void)LocalFree((HLOCAL)errtext);
    }
    va_end(vl);
}

JNIEXPORT void JNICALL
JLI_ReportExceptionDescription(JNIEnv * env) {
    if (IsJavaw()) {
       /*
        * This code should be replaced by code which opens a window with
        * the exception detail message, for now atleast put a dialog up.
        */
        MessageBox(NULL, "A Java Exception has occurred.", "Java Virtual Machine Launcher",
               (MB_OK|MB_ICONSTOP|MB_APPLMODAL));
    } else {
        (*env)->ExceptionDescribe(env);
    }
}

/*
 * Wrapper for platform dependent unsetenv function.
 */
int
UnsetEnv(char *name)
{
    int ret;
    char *buf = JLI_MemAlloc(JLI_StrLen(name) + 2);
    buf = JLI_StrCat(JLI_StrCpy(buf, name), "=");
    ret = _putenv(buf);
    JLI_MemFree(buf);
    return (ret);
}

/* --- Splash Screen shared library support --- */

static const char* SPLASHSCREEN_SO = "\\bin\\splashscreen.dll";

static HMODULE hSplashLib = NULL;

void* SplashProcAddress(const char* name) {
    char libraryPath[MAXPATHLEN]; /* some extra space for JLI_StrCat'ing SPLASHSCREEN_SO */

    if (!GetJREPath(libraryPath, MAXPATHLEN)) {
        return NULL;
    }
    if (JLI_StrLen(libraryPath)+JLI_StrLen(SPLASHSCREEN_SO) >= MAXPATHLEN) {
        return NULL;
    }
    JLI_StrCat(libraryPath, SPLASHSCREEN_SO);

    if (!hSplashLib) {
        hSplashLib = LoadLibrary(libraryPath);
    }
    if (hSplashLib) {
        return GetProcAddress(hSplashLib, name);
    } else {
        return NULL;
    }
}

/*
 * Signature adapter for _beginthreadex().
 */
static unsigned __stdcall ThreadJavaMain(void* args) {
    return (unsigned)JavaMain(args);
}

/*
 * Block current thread and continue execution in a new thread.
 */
int
CallJavaMainInNewThread(jlong stack_size, void* args) {
    int rslt = 0;
    unsigned thread_id;

#ifndef STACK_SIZE_PARAM_IS_A_RESERVATION
#define STACK_SIZE_PARAM_IS_A_RESERVATION  (0x10000)
#endif

    /*
     * STACK_SIZE_PARAM_IS_A_RESERVATION is what we want, but it's not
     * supported on older version of Windows. Try first with the flag; and
     * if that fails try again without the flag. See MSDN document or HotSpot
     * source (os_win32.cpp) for details.
     */
    HANDLE thread_handle =
        (HANDLE)_beginthreadex(NULL,
                               (unsigned)stack_size,
                               ThreadJavaMain,
                               args,
                               STACK_SIZE_PARAM_IS_A_RESERVATION,
                               &thread_id);
    if (thread_handle == NULL) {
        thread_handle =
        (HANDLE)_beginthreadex(NULL,
                               (unsigned)stack_size,
                               ThreadJavaMain,
                               args,
                               0,
                               &thread_id);
    }

    /* AWT preloading (AFTER main thread start) */
#ifdef ENABLE_AWT_PRELOAD
    /* D3D preloading */
    if (awtPreloadD3D != 0) {
        char *envValue;
        /* D3D routines checks env.var J2D_D3D if no appropriate
         * command line params was specified
         */
        envValue = getenv("J2D_D3D");
        if (envValue != NULL && JLI_StrCaseCmp(envValue, "false") == 0) {
            awtPreloadD3D = 0;
        }
        /* Test that AWT preloading isn't disabled by J2D_D3D_PRELOAD env.var */
        envValue = getenv("J2D_D3D_PRELOAD");
        if (envValue != NULL && JLI_StrCaseCmp(envValue, "false") == 0) {
            awtPreloadD3D = 0;
        }
        if (awtPreloadD3D < 0) {
            /* If awtPreloadD3D is still undefined (-1), test
             * if it is turned on by J2D_D3D_PRELOAD env.var.
             * By default it's turned OFF.
             */
            awtPreloadD3D = 0;
            if (envValue != NULL && JLI_StrCaseCmp(envValue, "true") == 0) {
                awtPreloadD3D = 1;
            }
         }
    }
    if (awtPreloadD3D) {
        AWTPreload(D3D_PRELOAD_FUNC);
    }
#endif /* ENABLE_AWT_PRELOAD */

    if (thread_handle) {
        WaitForSingleObject(thread_handle, INFINITE);
        GetExitCodeThread(thread_handle, &rslt);
        CloseHandle(thread_handle);
    } else {
        rslt = JavaMain(args);
    }

#ifdef ENABLE_AWT_PRELOAD
    if (awtPreloaded) {
        AWTPreloadStop();
    }
#endif /* ENABLE_AWT_PRELOAD */

    return rslt;
}

/*
 * The implementation for finding classes from the bootstrap
 * class loader, refer to java.h
 */
static FindClassFromBootLoader_t *findBootClass = NULL;

jclass FindBootStrapClass(JNIEnv *env, const char *classname)
{
   HMODULE hJvm;

   if (findBootClass == NULL) {
       hJvm = GetModuleHandle(JVM_DLL);
       if (hJvm == NULL) return NULL;
       /* need to use the demangled entry point */
       findBootClass = (FindClassFromBootLoader_t *)GetProcAddress(hJvm,
            "JVM_FindClassFromBootLoader");
       if (findBootClass == NULL) {
          JLI_ReportErrorMessage(DLL_ERROR4, "JVM_FindClassFromBootLoader");
          return NULL;
       }
   }
   return findBootClass(env, classname);
}

void
InitLauncher(boolean javaw)
{
    INITCOMMONCONTROLSEX icx;

    /*
     * Required for javaw mode MessageBox output as well as for
     * HotSpot -XX:+ShowMessageBoxOnError in java mode, an empty
     * flag field is sufficient to perform the basic UI initialization.
     */
    memset(&icx, 0, sizeof(INITCOMMONCONTROLSEX));
    icx.dwSize = sizeof(INITCOMMONCONTROLSEX);
    InitCommonControlsEx(&icx);
    _isjavaw = javaw;
    JLI_SetTraceLauncher();
}


/* ============================== */
/* AWT preloading */
#ifdef ENABLE_AWT_PRELOAD

typedef int FnPreloadStart(void);
typedef void FnPreloadStop(void);
static FnPreloadStop *fnPreloadStop = NULL;
static HMODULE hPreloadAwt = NULL;

/*
 * Starts AWT preloading
 */
int AWTPreload(const char *funcName)
{
    int result = -1;
    /* load AWT library once (if several preload function should be called) */
    if (hPreloadAwt == NULL) {
        /* awt.dll is not loaded yet */
        char libraryPath[MAXPATHLEN];
        size_t jrePathLen = 0;
        HMODULE hJava = NULL;
        HMODULE hVerify = NULL;

        while (1) {
            /* awt.dll depends on jvm.dll & java.dll;
             * jvm.dll is already loaded, so we need only java.dll;
             * java.dll depends on MSVCRT lib & verify.dll.
             */
            if (!GetJREPath(libraryPath, MAXPATHLEN)) {
                break;
            }

            /* save path length */
            jrePathLen = JLI_StrLen(libraryPath);

            if (jrePathLen + JLI_StrLen("\\bin\\verify.dll") >= MAXPATHLEN) {
              /* jre path is too long, the library path will not fit there;
               * report and abort preloading
               */
              JLI_ReportErrorMessage(JRE_ERROR11);
              break;
            }

            /* load msvcrt 1st */
            LoadMSVCRT();

            /* load verify.dll */
            JLI_StrCat(libraryPath, "\\bin\\verify.dll");
            hVerify = LoadLibrary(libraryPath);
            if (hVerify == NULL) {
                break;
            }

            /* restore jrePath */
            libraryPath[jrePathLen] = 0;
            /* load java.dll */
            JLI_StrCat(libraryPath, "\\bin\\" JAVA_DLL);
            hJava = LoadLibrary(libraryPath);
            if (hJava == NULL) {
                break;
            }

            /* restore jrePath */
            libraryPath[jrePathLen] = 0;
            /* load awt.dll */
            JLI_StrCat(libraryPath, "\\bin\\awt.dll");
            hPreloadAwt = LoadLibrary(libraryPath);
            if (hPreloadAwt == NULL) {
                break;
            }

            /* get "preloadStop" func ptr */
            fnPreloadStop = (FnPreloadStop *)GetProcAddress(hPreloadAwt, "preloadStop");

            break;
        }
    }

    if (hPreloadAwt != NULL) {
        FnPreloadStart *fnInit = (FnPreloadStart *)GetProcAddress(hPreloadAwt, funcName);
        if (fnInit != NULL) {
            /* don't forget to stop preloading */
            awtPreloaded = 1;

            result = fnInit();
        }
    }

    return result;
}

/*
 * Terminates AWT preloading
 */
void AWTPreloadStop() {
    if (fnPreloadStop != NULL) {
        fnPreloadStop();
    }
}

#endif /* ENABLE_AWT_PRELOAD */

int
JVMInit(InvocationFunctions* ifn, jlong threadStackSize,
        int argc, char **argv,
        int mode, char *what, int ret)
{
    ShowSplashScreen();
    return ContinueInNewThread(ifn, threadStackSize, argc, argv, mode, what, ret);
}

void
PostJVMInit(JNIEnv *env, jclass mainClass, JavaVM *vm)
{
    // stubbed out for windows and *nixes.
}

void
RegisterThread()
{
    // stubbed out for windows and *nixes.
}

/*
 * on windows, we return a false to indicate this option is not applicable
 */
jboolean
ProcessPlatformOption(const char *arg)
{
    return JNI_FALSE;
}

/*
 * At this point we have the arguments to the application, and we need to
 * check with original stdargs in order to compare which of these truly
 * needs expansion. cmdtoargs will specify this if it finds a bare
 * (unquoted) argument containing a glob character(s) ie. * or ?
 */
jobjectArray
CreateApplicationArgs(JNIEnv *env, char **strv, int argc)
{
    int i, j, idx;
    size_t tlen;
    jobjectArray outArray, inArray;
    char *arg, **nargv;
    jboolean needs_expansion = JNI_FALSE;
    jmethodID mid;
    int stdargc;
    StdArg *stdargs;
    int *appArgIdx;
    int isTool;
    jclass cls = GetLauncherHelperClass(env);
    NULL_CHECK0(cls);

    if (argc == 0) {
        return NewPlatformStringArray(env, strv, argc);
    }
    // the holy grail we need to compare with.
    stdargs = JLI_GetStdArgs();
    stdargc = JLI_GetStdArgc();

    // sanity check, this should never happen
    if (argc > stdargc) {
        JLI_TraceLauncher("Warning: app args is larger than the original, %d %d\n", argc, stdargc);
        JLI_TraceLauncher("passing arguments as-is.\n");
        return NewPlatformStringArray(env, strv, argc);
    }

    // sanity check, match the args we have, to the holy grail
    idx = JLI_GetAppArgIndex();

    // First arg index is NOT_FOUND
    if (idx < 0) {
        // The only allowed value should be NOT_FOUND (-1) unless another change introduces
        // a different negative index
        assert (idx == -1);
        JLI_TraceLauncher("Warning: first app arg index not found, %d\n", idx);
        JLI_TraceLauncher("passing arguments as-is.\n");
        return NewPlatformStringArray(env, strv, argc);
    }

    isTool = (idx == 0);
    if (isTool) { idx++; } // skip tool name
    JLI_TraceLauncher("AppArgIndex: %d points to %s\n", idx, stdargs[idx].arg);

    appArgIdx = calloc(argc, sizeof(int));
    for (i = idx, j = 0; i < stdargc; i++) {
        if (isTool) { // filter -J used by tools to pass JVM options
            arg = stdargs[i].arg;
            if (arg[0] == '-' && arg[1] == 'J') {
                continue;
            }
        }
        appArgIdx[j++] = i;
    }
    // sanity check, ensure same number of arguments for application
    if (j != argc) {
        JLI_TraceLauncher("Warning: app args count doesn't match, %d %d\n", j, argc);
        JLI_TraceLauncher("passing arguments as-is.\n");
        JLI_MemFree(appArgIdx);
        return NewPlatformStringArray(env, strv, argc);
    }

    // make a copy of the args which will be expanded in java if required.
    nargv = (char **)JLI_MemAlloc(argc * sizeof(char*));
    for (i = 0; i < argc; i++) {
        jboolean arg_expand;
        j = appArgIdx[i];
        arg_expand = (JLI_StrCmp(stdargs[j].arg, strv[i]) == 0)
            ? stdargs[j].has_wildcard
            : JNI_FALSE;
        if (needs_expansion == JNI_FALSE)
            needs_expansion = arg_expand;

        // indicator char + String + NULL terminator, the java method will strip
        // out the first character, the indicator character, so no matter what
        // we add the indicator
        tlen = 1 + JLI_StrLen(strv[i]) + 1;
        nargv[i] = (char *) JLI_MemAlloc(tlen);
        if (JLI_Snprintf(nargv[i], tlen, "%c%s", arg_expand ? 'T' : 'F',
                         strv[i]) < 0) {
            return NULL;
        }
        JLI_TraceLauncher("%s\n", nargv[i]);
    }

    if (!needs_expansion) {
        // clean up any allocated memory and return back the old arguments
        for (i = 0 ; i < argc ; i++) {
            JLI_MemFree(nargv[i]);
        }
        JLI_MemFree(nargv);
        JLI_MemFree(appArgIdx);
        return NewPlatformStringArray(env, strv, argc);
    }
    NULL_CHECK0(mid = (*env)->GetStaticMethodID(env, cls,
                                                "expandArgs",
                                                "([Ljava/lang/String;)[Ljava/lang/String;"));

    // expand the arguments that require expansion, the java method will strip
    // out the indicator character.
    NULL_CHECK0(inArray = NewPlatformStringArray(env, nargv, argc));
    outArray = (*env)->CallStaticObjectMethod(env, cls, mid, inArray);
    for (i = 0; i < argc; i++) {
        JLI_MemFree(nargv[i]);
    }
    JLI_MemFree(nargv);
    JLI_MemFree(appArgIdx);
    return outArray;
}
