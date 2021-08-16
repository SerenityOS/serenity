/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <sys/time.h>
#include "java.h"

/*
 * Find the last occurrence of a string
 */
static char* findLastPathComponent(char *buffer, const char *comp) {
    char* t = buffer;
    char* p = NULL;
    size_t l = JLI_StrLen(comp);
    t = JLI_StrStr(t, comp);

    while (t != NULL) {
        p = t;
        t += l;
        t = JLI_StrStr(t, comp);
    }
    return p;
}

/*
 * Removes the trailing file name and any intermediate platform
 * directories, if any, and its enclosing directory.
 * Second parameter is a hint about the type of a file. JNI_TRUE is for
 * shared libraries and JNI_FALSE is for executables.
 * Ex: if a buffer contains "/foo/bin/javac" or "/foo/bin/x64/javac", the
 * truncated resulting buffer will contain "/foo".
 */
static jboolean
TruncatePath(char *buf, jboolean pathisdll)
{
    /*
     * If the file is a library, try lib directory first and then bin
     * directory.
     * If the file is an executable, try bin directory first and then lib
     * directory.
     */

    char *p = findLastPathComponent(buf, pathisdll ? "/lib/" : "/bin/");
    if (p != NULL) {
        *p = '\0';
        return JNI_TRUE;
    }
    p = findLastPathComponent(buf, pathisdll ? "/bin/" : "/lib/");
    if (p != NULL) {
        *p = '\0';
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

/*
 * Retrieves the path to the JRE home by locating the executable file
 * of the current process and then truncating the path to the executable
 */
jboolean
GetApplicationHome(char *buf, jint bufsize)
{
    const char *execname = GetExecName();
    if (execname != NULL) {
        JLI_Snprintf(buf, bufsize, "%s", execname);
        buf[bufsize-1] = '\0';
    } else {
        return JNI_FALSE;
    }
    return TruncatePath(buf, JNI_FALSE);
}

/*
 * Retrieves the path to the JRE home by locating the
 * shared library and then truncating the path to it.
 */
jboolean
GetApplicationHomeFromDll(char *buf, jint bufsize)
{
    /* try to find ourselves instead */
    Dl_info info;
    if (dladdr((void*)&GetApplicationHomeFromDll, &info) != 0) {
        char *path = realpath(info.dli_fname, buf);
        if (path == buf) {
            return TruncatePath(buf, JNI_TRUE);
        }
    }
    return JNI_FALSE;
}

/*
 * Return true if the named program exists
 */
static int
ProgramExists(char *name)
{
    struct stat sb;
    if (stat(name, &sb) != 0) return 0;
    if (S_ISDIR(sb.st_mode)) return 0;
    return (sb.st_mode & S_IEXEC) != 0;
}

/*
 * Find a command in a directory, returning the path.
 */
static char *
Resolve(char *indir, char *cmd)
{
    char name[PATH_MAX + 2], *real;

    if ((JLI_StrLen(indir) + JLI_StrLen(cmd) + 1)  > PATH_MAX) return 0;
    JLI_Snprintf(name, sizeof(name), "%s%c%s", indir, FILE_SEPARATOR, cmd);
    if (!ProgramExists(name)) return 0;
    real = JLI_MemAlloc(PATH_MAX + 2);
    if (!realpath(name, real))
        JLI_StrCpy(real, name);
    return real;
}

/*
 * Find a path for the executable
 */
char *
FindExecName(char *program)
{
    char cwdbuf[PATH_MAX+2];
    char *path;
    char *tmp_path;
    char *f;
    char *result = NULL;

    /* absolute path? */
    if (*program == FILE_SEPARATOR ||
        (FILE_SEPARATOR=='\\' && JLI_StrRChr(program, ':')))
        return Resolve("", program+1);

    /* relative path? */
    if (JLI_StrRChr(program, FILE_SEPARATOR) != 0) {
        char buf[PATH_MAX+2];
        return Resolve(getcwd(cwdbuf, sizeof(cwdbuf)), program);
    }

    /* from search path? */
    path = getenv("PATH");
    if (!path || !*path) path = ".";
    tmp_path = JLI_MemAlloc(JLI_StrLen(path) + 2);
    JLI_StrCpy(tmp_path, path);

    for (f=tmp_path; *f && result==0; ) {
        char *s = f;
        while (*f && (*f != PATH_SEPARATOR)) ++f;
        if (*f) *f++ = 0;
        if (*s == FILE_SEPARATOR)
            result = Resolve(s, program);
        else {
            /* relative path element */
            char dir[2*PATH_MAX];
            JLI_Snprintf(dir, sizeof(dir), "%s%c%s", getcwd(cwdbuf, sizeof(cwdbuf)),
                    FILE_SEPARATOR, s);
            result = Resolve(dir, program);
        }
        if (result != 0) break;
    }

    JLI_MemFree(tmp_path);
    return result;
}

JNIEXPORT void JNICALL
JLI_ReportErrorMessage(const char* fmt, ...) {
    va_list vl;
    va_start(vl, fmt);
    vfprintf(stderr, fmt, vl);
    fprintf(stderr, "\n");
    va_end(vl);
}

JNIEXPORT void JNICALL
JLI_ReportErrorMessageSys(const char* fmt, ...) {
    va_list vl;
    char *emsg;

    /*
     * TODO: its safer to use strerror_r but is not available on
     * Solaris 8. Until then....
     */
    emsg = strerror(errno);
    if (emsg != NULL) {
        fprintf(stderr, "%s\n", emsg);
    }

    va_start(vl, fmt);
    vfprintf(stderr, fmt, vl);
    fprintf(stderr, "\n");
    va_end(vl);
}

JNIEXPORT void JNICALL
JLI_ReportExceptionDescription(JNIEnv * env) {
  (*env)->ExceptionDescribe(env);
}

/*
 *      Since using the file system as a registry is a bit risky, perform
 *      additional sanity checks on the identified directory to validate
 *      it as a valid jre/sdk.
 *
 *      Return 0 if the tests fail; otherwise return non-zero (true).
 *
 *      Note that checking for anything more than the existence of an
 *      executable object at bin/java relative to the path being checked
 *      will break the regression tests.
 */
static int
CheckSanity(char *path, char *dir)
{
    char    buffer[PATH_MAX];

    if (JLI_StrLen(path) + JLI_StrLen(dir) + 11 > PATH_MAX)
        return (0);     /* Silently reject "impossibly" long paths */

    JLI_Snprintf(buffer, sizeof(buffer), "%s/%s/bin/java", path, dir);
    return ((access(buffer, X_OK) == 0) ? 1 : 0);
}

/*
 * "Borrowed" from Solaris 10 where the unsetenv() function is being added
 * to libc thanks to SUSv3 (Standard Unix Specification, version 3). As
 * such, in the fullness of time this will appear in libc on all relevant
 * Solaris/Linux platforms and maybe even the Windows platform.  At that
 * time, this stub can be removed.
 *
 * This implementation removes the environment locking for multithreaded
 * applications.  (We don't have access to these mutexes within libc and
 * the launcher isn't multithreaded.)  Note that what remains is platform
 * independent, because it only relies on attributes that a POSIX environment
 * defines.
 *
 * Returns 0 on success, -1 on failure.
 *
 * Also removed was the setting of errno.  The only value of errno set
 * was EINVAL ("Invalid Argument").
 */

/*
 * s1(environ) is name=value
 * s2(name) is name(not the form of name=value).
 * if names match, return value of 1, else return 0
 */
static int
match_noeq(const char *s1, const char *s2)
{
        while (*s1 == *s2++) {
                if (*s1++ == '=')
                        return (1);
        }
        if (*s1 == '=' && s2[-1] == '\0')
                return (1);
        return (0);
}

/*
 * added for SUSv3 standard
 *
 * Delete entry from environ.
 * Do not free() memory!  Other threads may be using it.
 * Keep it around forever.
 */
static int
borrowed_unsetenv(const char *name)
{
        long    idx;            /* index into environ */

        if (name == NULL || *name == '\0' ||
            JLI_StrChr(name, '=') != NULL) {
                return (-1);
        }

        for (idx = 0; environ[idx] != NULL; idx++) {
                if (match_noeq(environ[idx], name))
                        break;
        }
        if (environ[idx] == NULL) {
                /* name not found but still a success */
                return (0);
        }
        /* squeeze up one entry */
        do {
                environ[idx] = environ[idx+1];
        } while (environ[++idx] != NULL);

        return (0);
}
/* --- End of "borrowed" code --- */

/*
 * Wrapper for unsetenv() function.
 */
int
UnsetEnv(char *name)
{
    return(borrowed_unsetenv(name));
}

jboolean
IsJavaw()
{
    /* noop on UNIX */
    return JNI_FALSE;
}

void
InitLauncher(jboolean javaw)
{
    JLI_SetTraceLauncher();
}

/*
 * The implementation for finding classes from the bootstrap
 * class loader, refer to java.h
 */
static FindClassFromBootLoader_t *findBootClass = NULL;

jclass
FindBootStrapClass(JNIEnv *env, const char* classname)
{
   if (findBootClass == NULL) {
       findBootClass = (FindClassFromBootLoader_t *)dlsym(RTLD_DEFAULT,
          "JVM_FindClassFromBootLoader");
       if (findBootClass == NULL) {
           JLI_ReportErrorMessage(DLL_ERROR4,
               "JVM_FindClassFromBootLoader");
           return NULL;
       }
   }
   return findBootClass(env, classname);
}

JNIEXPORT StdArg JNICALL
*JLI_GetStdArgs()
{
    return NULL;
}

JNIEXPORT int JNICALL
JLI_GetStdArgc() {
    return 0;
}

jobjectArray
CreateApplicationArgs(JNIEnv *env, char **strv, int argc)
{
    return NewPlatformStringArray(env, strv, argc);
}

/*
 * Provide a CurrentTimeMicros() implementation based on gettimeofday() which
 * is universally available, even though it may not be 'high resolution'
 * compared to platforms that provide gethrtime() (like Solaris). It is
 * also subject to time-of-day changes, but alternatives may not be
 * known to be available at either build time or run time.
 */
jlong CurrentTimeMicros() {
    jlong result = 0;
    struct timeval tv;
    if (gettimeofday(&tv, NULL) != -1) {
        result = 1000000LL * (jlong)tv.tv_sec;
        result += (jlong)tv.tv_usec;
    }
    return result;
}
