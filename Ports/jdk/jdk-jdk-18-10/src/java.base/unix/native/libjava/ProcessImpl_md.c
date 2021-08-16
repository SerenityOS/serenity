/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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

#undef  _LARGEFILE64_SOURCE
#define _LARGEFILE64_SOURCE 1

#include "jni.h"
#include "jvm.h"
#include "jvm_md.h"
#include "jni_util.h"
#include "io_util.h"

/*
 * Platform-specific support for java.lang.Process
 */
#include <assert.h>
#include <stddef.h>
#include <stdlib.h>
#include <sys/types.h>
#include <ctype.h>
#include <sys/wait.h>
#include <signal.h>
#include <string.h>

#include <spawn.h>

#include "childproc.h"

/*
 *
 * When starting a child on Unix, we need to do three things:
 * - fork off
 * - in the child process, do some pre-exec work: duping/closing file
 *   descriptors to set up stdio-redirection, setting environment variables,
 *   changing paths...
 * - then exec(2) the target binary
 *
 * There are three ways to fork off:
 *
 * A) fork(2). Portable and safe (no side effects) but may fail with ENOMEM on
 *    all Unices when invoked from a VM with a high memory footprint. On Unices
 *    with strict no-overcommit policy this problem is most visible.
 *
 *    This is because forking the VM will first create a child process with
 *    theoretically the same memory footprint as the parent - even if you plan
 *    to follow up with exec'ing a tiny binary. In reality techniques like
 *    copy-on-write etc mitigate the problem somewhat but we still run the risk
 *    of hitting system limits.
 *
 *    For a Linux centric description of this problem, see the documentation on
 *    /proc/sys/vm/overcommit_memory in Linux proc(5).
 *
 * B) vfork(2): Portable and fast but very unsafe. It bypasses the memory
 *    problems related to fork(2) by starting the child in the memory image of
 *    the parent. Things that can go wrong include:
 *    - Programming errors in the child process before the exec(2) call may
 *      trash memory in the parent process, most commonly the stack of the
 *      thread invoking vfork.
 *    - Signals received by the child before the exec(2) call may be at best
 *      misdirected to the parent, at worst immediately kill child and parent.
 *
 *    This is mitigated by very strict rules about what one is allowed to do in
 *    the child process between vfork(2) and exec(2), which is basically nothing.
 *    However, we always broke this rule by doing the pre-exec work between
 *    vfork(2) and exec(2).
 *
 *    Also note that vfork(2) has been deprecated by the OpenGroup, presumably
 *    because of its many dangers.
 *
 * C) clone(2): This is a Linux specific call which gives the caller fine
 *    grained control about how exactly the process fork is executed. It is
 *    powerful, but Linux-specific.
 *
 * Aside from these three possibilities there is a forth option:  posix_spawn(3).
 * Where fork/vfork/clone all fork off the process and leave pre-exec work and
 * calling exec(2) to the user, posix_spawn(3) offers the user fork+exec-like
 * functionality in one package, similar to CreateProcess() on Windows.
 *
 * It is not a system call in itself, but usually a wrapper implemented within
 * the libc in terms of one of (fork|vfork|clone)+exec - so whether or not it
 * has advantages over calling the naked (fork|vfork|clone) functions depends
 * on how posix_spawn(3) is implemented.
 *
 * Note that when using posix_spawn(3), we exec twice: first a tiny binary called
 * the jspawnhelper, then in the jspawnhelper we do the pre-exec work and exec a
 * second time, this time the target binary (similar to the "exec-twice-technique"
 * described in http://mail.openjdk.java.net/pipermail/core-libs-dev/2018-September/055333.html).
 *
 * This is a JDK-specific implementation detail which just happens to be
 * implemented for jdk.lang.Process.launchMechanism=POSIX_SPAWN.
 *
 * --- Linux-specific ---
 *
 * How does glibc implement posix_spawn?
 * (see: sysdeps/posix/spawni.c for glibc < 2.24,
 *       sysdeps/unix/sysv/linux/spawni.c for glibc >= 2.24):
 *
 * 1) Before glibc 2.4 (released 2006), posix_spawn(3) used just fork(2)/exec(2).
 *    This would be bad for the JDK since we would risk the known memory issues with
 *    fork(2). But since this only affects glibc variants which have long been
 *    phased out by modern distributions, this is irrelevant.
 *
 * 2) Between glibc 2.4 and glibc 2.23, posix_spawn uses either fork(2) or
 *    vfork(2) depending on how exactly the user called posix_spawn(3):
 *
 * <quote>
 *       The child process is created using vfork(2) instead of fork(2) when
 *       either of the following is true:
 *
 *       * the spawn-flags element of the attributes object pointed to by
 *          attrp contains the GNU-specific flag POSIX_SPAWN_USEVFORK; or
 *
 *       * file_actions is NULL and the spawn-flags element of the attributes
 *          object pointed to by attrp does not contain
 *          POSIX_SPAWN_SETSIGMASK, POSIX_SPAWN_SETSIGDEF,
 *          POSIX_SPAWN_SETSCHEDPARAM, POSIX_SPAWN_SETSCHEDULER,
 *          POSIX_SPAWN_SETPGROUP, or POSIX_SPAWN_RESETIDS.
 * </quote>
 *
 * Due to the way the JDK calls posix_spawn(3), it would therefore call vfork(2).
 * So we would avoid the fork(2) memory problems. However, there still remains the
 * risk associated with vfork(2). But it is smaller than were we to call vfork(2)
 * directly since we use the jspawnhelper, moving all pre-exec work off to after
 * the first exec, thereby reducing the vulnerable time window.
 *
 * 3) Since glibc >= 2.24, glibc uses clone+exec:
 *
 *    new_pid = CLONE (__spawni_child, STACK (stack, stack_size), stack_size,
 *                     CLONE_VM | CLONE_VFORK | SIGCHLD, &args);
 *
 * This is even better than (2):
 *
 * CLONE_VM means we run in the parent's memory image, as with (2)
 * CLONE_VFORK means parent waits until we exec, as with (2)
 *
 * However, error possibilities are further reduced since:
 * - posix_spawn(3) passes a separate stack for the child to run on, eliminating
 *   the danger of trashing the forking thread's stack in the parent process.
 * - posix_spawn(3) takes care to temporarily block all incoming signals to the
 *   child process until the first exec(2) has been called,
 *
 * TL;DR
 * Calling posix_spawn(3) for glibc
 * (2) < 2.24 is not perfect but still better than using plain vfork(2), since
 *     the chance of an error happening is greatly reduced
 * (3) >= 2.24 is the best option - portable, fast and as safe as possible.
 *
 * ---
 *
 * How does muslc implement posix_spawn?
 *
 * They always did use the clone (.. CLONE_VM | CLONE_VFORK ...)
 * technique. So we are safe to use posix_spawn() here regardless of muslc
 * version.
 *
 * </Linux-specific>
 *
 *
 * Based on the above analysis, we are currently defaulting to posix_spawn()
 * on all Unices including Linux.
 */

static void
setSIGCHLDHandler(JNIEnv *env)
{
    /* There is a subtle difference between having the signal handler
     * for SIGCHLD be SIG_DFL and SIG_IGN.  We cannot obtain process
     * termination information for child processes if the signal
     * handler is SIG_IGN.  It must be SIG_DFL.
     *
     * We used to set the SIGCHLD handler only on Linux, but it's
     * safest to set it unconditionally.
     *
     * Consider what happens if java's parent process sets the SIGCHLD
     * handler to SIG_IGN.  Normally signal handlers are inherited by
     * children, but SIGCHLD is a controversial case.  Solaris appears
     * to always reset it to SIG_DFL, but this behavior may be
     * non-standard-compliant, and we shouldn't rely on it.
     *
     * References:
     * http://www.opengroup.org/onlinepubs/7908799/xsh/exec.html
     * http://www.pasc.org/interps/unofficial/db/p1003.1/pasc-1003.1-132.html
     */
    struct sigaction sa;
    sa.sa_handler = SIG_DFL;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_NOCLDSTOP | SA_RESTART;
    if (sigaction(SIGCHLD, &sa, NULL) < 0)
        JNU_ThrowInternalError(env, "Can't set SIGCHLD handler");
}

static void*
xmalloc(JNIEnv *env, size_t size)
{
    void *p = malloc(size);
    if (p == NULL)
        JNU_ThrowOutOfMemoryError(env, NULL);
    return p;
}

#define NEW(type, n) ((type *) xmalloc(env, (n) * sizeof(type)))

/**
 * If PATH is not defined, the OS provides some default value.
 * Unfortunately, there's no portable way to get this value.
 * Fortunately, it's only needed if the child has PATH while we do not.
 */
static const char*
defaultPath(void)
{
    return ":/bin:/usr/bin";
}

static const char*
effectivePath(void)
{
    const char *s = getenv("PATH");
    return (s != NULL) ? s : defaultPath();
}

static int
countOccurrences(const char *s, char c)
{
    int count;
    for (count = 0; *s != '\0'; s++)
        count += (*s == c);
    return count;
}

static const char * const *
effectivePathv(JNIEnv *env)
{
    char *p;
    int i;
    const char *path = effectivePath();
    int count = countOccurrences(path, ':') + 1;
    size_t pathvsize = sizeof(const char *) * (count+1);
    size_t pathsize = strlen(path) + 1;
    const char **pathv = (const char **) xmalloc(env, pathvsize + pathsize);

    if (pathv == NULL)
        return NULL;
    p = (char *) pathv + pathvsize;
    memcpy(p, path, pathsize);
    /* split PATH by replacing ':' with NULs; empty components => "." */
    for (i = 0; i < count; i++) {
        char *q = p + strcspn(p, ":");
        pathv[i] = (p == q) ? "." : p;
        *q = '\0';
        p = q + 1;
    }
    pathv[count] = NULL;
    return pathv;
}

JNIEXPORT void JNICALL
Java_java_lang_ProcessImpl_init(JNIEnv *env, jclass clazz)
{
    parentPathv = effectivePathv(env);
    CHECK_NULL(parentPathv);
    setSIGCHLDHandler(env);
}


#ifndef WIFEXITED
#define WIFEXITED(status) (((status)&0xFF) == 0)
#endif

#ifndef WEXITSTATUS
#define WEXITSTATUS(status) (((status)>>8)&0xFF)
#endif

#ifndef WIFSIGNALED
#define WIFSIGNALED(status) (((status)&0xFF) > 0 && ((status)&0xFF00) == 0)
#endif

#ifndef WTERMSIG
#define WTERMSIG(status) ((status)&0x7F)
#endif

static const char *
getBytes(JNIEnv *env, jbyteArray arr)
{
    return arr == NULL ? NULL :
        (const char*) (*env)->GetByteArrayElements(env, arr, NULL);
}

static void
releaseBytes(JNIEnv *env, jbyteArray arr, const char* parr)
{
    if (parr != NULL)
        (*env)->ReleaseByteArrayElements(env, arr, (jbyte*) parr, JNI_ABORT);
}

#define IOE_FORMAT "error=%d, %s"

static void
throwIOException(JNIEnv *env, int errnum, const char *defaultDetail)
{
    const char *detail = defaultDetail;
    char *errmsg;
    size_t fmtsize;
    char tmpbuf[1024];
    jstring s;

    if (errnum != 0) {
        int ret = getErrorString(errnum, tmpbuf, sizeof(tmpbuf));
        if (ret != EINVAL)
            detail = tmpbuf;
    }
    /* ASCII Decimal representation uses 2.4 times as many bits as binary. */
    fmtsize = sizeof(IOE_FORMAT) + strlen(detail) + 3 * sizeof(errnum);
    errmsg = NEW(char, fmtsize);
    if (errmsg == NULL)
        return;

    snprintf(errmsg, fmtsize, IOE_FORMAT, errnum, detail);
    s = JNU_NewStringPlatform(env, errmsg);
    if (s != NULL) {
        jobject x = JNU_NewObjectByName(env, "java/io/IOException",
                                        "(Ljava/lang/String;)V", s);
        if (x != NULL)
            (*env)->Throw(env, x);
    }
    free(errmsg);
}

/**
 * Throws an IOException with a message composed from the result of waitpid status.
 */
static void throwExitCause(JNIEnv *env, int pid, int status) {
    char ebuf[128];
    if (WIFEXITED(status)) {
        snprintf(ebuf, sizeof ebuf,
            "Failed to exec spawn helper: pid: %d, exit value: %d",
            pid, WEXITSTATUS(status));
    } else if (WIFSIGNALED(status)) {
        snprintf(ebuf, sizeof ebuf,
            "Failed to exec spawn helper: pid: %d, signal: %d",
            pid, WTERMSIG(status));
    } else {
        snprintf(ebuf, sizeof ebuf,
            "Failed to exec spawn helper: pid: %d, status: 0x%08x",
            pid, status);
    }
    throwIOException(env, 0, ebuf);
}

#ifdef DEBUG_PROCESS
/* Debugging process code is difficult; where to write debug output? */
static void
debugPrint(char *format, ...)
{
    FILE *tty = fopen("/dev/tty", "w");
    va_list ap;
    va_start(ap, format);
    vfprintf(tty, format, ap);
    va_end(ap);
    fclose(tty);
}
#endif /* DEBUG_PROCESS */

static void
copyPipe(int from[2], int to[2])
{
    to[0] = from[0];
    to[1] = from[1];
}

/* arg is an array of pointers to 0 terminated strings. array is terminated
 * by a null element.
 *
 * *nelems and *nbytes receive the number of elements of array (incl 0)
 * and total number of bytes (incl. 0)
 * Note. An empty array will have one null element
 * But if arg is null, then *nelems set to 0, and *nbytes to 0
 */
static void arraysize(const char * const *arg, int *nelems, int *nbytes)
{
    int i, bytes, count;
    const char * const *a = arg;
    char *p;
    int *q;
    if (arg == 0) {
        *nelems = 0;
        *nbytes = 0;
        return;
    }
    /* count the array elements and number of bytes */
    for (count=0, bytes=0; *a != 0; count++, a++) {
        bytes += strlen(*a)+1;
    }
    *nbytes = bytes;
    *nelems = count+1;
}

/* copy the strings from arg[] into buf, starting at given offset
 * return new offset to next free byte
 */
static int copystrings(char *buf, int offset, const char * const *arg) {
    char *p;
    const char * const *a;
    int count=0;

    if (arg == 0) {
        return offset;
    }
    for (p=buf+offset, a=arg; *a != 0; a++) {
        int len = strlen(*a) +1;
        memcpy(p, *a, len);
        p += len;
        count += len;
    }
    return offset+count;
}

/**
 * We are unusually paranoid; use of vfork is
 * especially likely to tickle gcc/glibc bugs.
 */
#ifdef __attribute_noinline__  /* See: sys/cdefs.h */
__attribute_noinline__
#endif

/* vfork(2) is deprecated on Solaris */
static pid_t
vforkChild(ChildStuff *c) {
    volatile pid_t resultPid;

    /*
     * We separate the call to vfork into a separate function to make
     * very sure to keep stack of child from corrupting stack of parent,
     * as suggested by the scary gcc warning:
     *  warning: variable 'foo' might be clobbered by 'longjmp' or 'vfork'
     */
    resultPid = vfork();

    if (resultPid == 0) {
        childProcess(c);
    }
    assert(resultPid != 0);  /* childProcess never returns */
    return resultPid;
}

static pid_t
forkChild(ChildStuff *c) {
    pid_t resultPid;

    /*
     * From Solaris fork(2): In Solaris 10, a call to fork() is
     * identical to a call to fork1(); only the calling thread is
     * replicated in the child process. This is the POSIX-specified
     * behavior for fork().
     */
    resultPid = fork();

    if (resultPid == 0) {
        childProcess(c);
    }
    assert(resultPid != 0);  /* childProcess never returns */
    return resultPid;
}

static pid_t
spawnChild(JNIEnv *env, jobject process, ChildStuff *c, const char *helperpath) {
    pid_t resultPid;
    jboolean isCopy;
    int i, offset, rval, bufsize, magic;
    char *buf, buf1[16];
    char *hlpargs[2];
    SpawnInfo sp;

    /* need to tell helper which fd is for receiving the childstuff
     * and which fd to send response back on
     */
    snprintf(buf1, sizeof(buf1), "%d:%d", c->childenv[0], c->fail[1]);
    /* put the fd string as argument to the helper cmd */
    hlpargs[0] = buf1;
    hlpargs[1] = 0;

    /* Following items are sent down the pipe to the helper
     * after it is spawned.
     * All strings are null terminated. All arrays of strings
     * have an empty string for termination.
     * - the ChildStuff struct
     * - the SpawnInfo struct
     * - the argv strings array
     * - the envv strings array
     * - the home directory string
     * - the parentPath string
     * - the parentPathv array
     */
    /* First calculate the sizes */
    arraysize(c->argv, &sp.nargv, &sp.argvBytes);
    bufsize = sp.argvBytes;
    arraysize(c->envv, &sp.nenvv, &sp.envvBytes);
    bufsize += sp.envvBytes;
    sp.dirlen = c->pdir == 0 ? 0 : strlen(c->pdir)+1;
    bufsize += sp.dirlen;
    arraysize(parentPathv, &sp.nparentPathv, &sp.parentPathvBytes);
    bufsize += sp.parentPathvBytes;
    /* We need to clear FD_CLOEXEC if set in the fds[].
     * Files are created FD_CLOEXEC in Java.
     * Otherwise, they will be closed when the target gets exec'd */
    for (i=0; i<3; i++) {
        if (c->fds[i] != -1) {
            int flags = fcntl(c->fds[i], F_GETFD);
            if (flags & FD_CLOEXEC) {
                fcntl(c->fds[i], F_SETFD, flags & (~1));
            }
        }
    }

    rval = posix_spawn(&resultPid, helperpath, 0, 0, (char * const *) hlpargs, environ);

    if (rval != 0) {
        return -1;
    }

    /* now the lengths are known, copy the data */
    buf = NEW(char, bufsize);
    if (buf == 0) {
        return -1;
    }
    offset = copystrings(buf, 0, &c->argv[0]);
    offset = copystrings(buf, offset, &c->envv[0]);
    memcpy(buf+offset, c->pdir, sp.dirlen);
    offset += sp.dirlen;
    offset = copystrings(buf, offset, parentPathv);
    assert(offset == bufsize);

    magic = magicNumber();

    /* write the two structs and the data buffer */
    write(c->childenv[1], (char *)&magic, sizeof(magic)); // magic number first
    write(c->childenv[1], (char *)c, sizeof(*c));
    write(c->childenv[1], (char *)&sp, sizeof(sp));
    write(c->childenv[1], buf, bufsize);
    free(buf);

    /* In this mode an external main() in invoked which calls back into
     * childProcess() in this file, rather than directly
     * via the statement below */
    return resultPid;
}

/*
 * Start a child process running function childProcess.
 * This function only returns in the parent.
 */
static pid_t
startChild(JNIEnv *env, jobject process, ChildStuff *c, const char *helperpath) {
    switch (c->mode) {
/* vfork(2) is deprecated on Solaris */
      case MODE_VFORK:
        return vforkChild(c);
      case MODE_FORK:
        return forkChild(c);
      case MODE_POSIX_SPAWN:
        return spawnChild(env, process, c, helperpath);
      default:
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_java_lang_ProcessImpl_forkAndExec(JNIEnv *env,
                                       jobject process,
                                       jint mode,
                                       jbyteArray helperpath,
                                       jbyteArray prog,
                                       jbyteArray argBlock, jint argc,
                                       jbyteArray envBlock, jint envc,
                                       jbyteArray dir,
                                       jintArray std_fds,
                                       jboolean redirectErrorStream)
{
    int errnum;
    int resultPid = -1;
    int in[2], out[2], err[2], fail[2], childenv[2];
    jint *fds = NULL;
    const char *phelperpath = NULL;
    const char *pprog = NULL;
    const char *pargBlock = NULL;
    const char *penvBlock = NULL;
    ChildStuff *c;

    in[0] = in[1] = out[0] = out[1] = err[0] = err[1] = fail[0] = fail[1] = -1;
    childenv[0] = childenv[1] = -1;

    if ((c = NEW(ChildStuff, 1)) == NULL) return -1;
    c->argv = NULL;
    c->envv = NULL;
    c->pdir = NULL;

    /* Convert prog + argBlock into a char ** argv.
     * Add one word room for expansion of argv for use by
     * execve_as_traditional_shell_script.
     * This word is also used when using posix_spawn mode
     */
    assert(prog != NULL && argBlock != NULL);
    if ((phelperpath = getBytes(env, helperpath))   == NULL) goto Catch;
    if ((pprog       = getBytes(env, prog))         == NULL) goto Catch;
    if ((pargBlock   = getBytes(env, argBlock))     == NULL) goto Catch;
    if ((c->argv     = NEW(const char *, argc + 3)) == NULL) goto Catch;
    c->argv[0] = pprog;
    c->argc = argc + 2;
    initVectorFromBlock(c->argv+1, pargBlock, argc);

    if (envBlock != NULL) {
        /* Convert envBlock into a char ** envv */
        if ((penvBlock = getBytes(env, envBlock))   == NULL) goto Catch;
        if ((c->envv = NEW(const char *, envc + 1)) == NULL) goto Catch;
        initVectorFromBlock(c->envv, penvBlock, envc);
    }

    if (dir != NULL) {
        if ((c->pdir = getBytes(env, dir)) == NULL) goto Catch;
    }

    assert(std_fds != NULL);
    fds = (*env)->GetIntArrayElements(env, std_fds, NULL);
    if (fds == NULL) goto Catch;

    if ((fds[0] == -1 && pipe(in)  < 0) ||
        (fds[1] == -1 && pipe(out) < 0) ||
        (fds[2] == -1 && pipe(err) < 0) ||
        (pipe(childenv) < 0) ||
        (pipe(fail) < 0)) {
        throwIOException(env, errno, "Bad file descriptor");
        goto Catch;
    }
    c->fds[0] = fds[0];
    c->fds[1] = fds[1];
    c->fds[2] = fds[2];

    copyPipe(in,   c->in);
    copyPipe(out,  c->out);
    copyPipe(err,  c->err);
    copyPipe(fail, c->fail);
    copyPipe(childenv, c->childenv);

    c->redirectErrorStream = redirectErrorStream;
    c->mode = mode;

    /* In posix_spawn mode, require the child process to signal aliveness
     * right after it comes up. This is because there are implementations of
     * posix_spawn() which do not report failed exec()s back to the caller
     * (e.g. glibc, see JDK-8223777). In those cases, the fork() will have
     * worked and successfully started the child process, but the exec() will
     * have failed. There is no way for us to distinguish this from a target
     * binary just exiting right after start.
     *
     * Note that we could do this additional handshake in all modes but for
     * prudence only do it when it is needed (in posix_spawn mode). */
    c->sendAlivePing = (mode == MODE_POSIX_SPAWN) ? 1 : 0;

    resultPid = startChild(env, process, c, phelperpath);
    assert(resultPid != 0);

    if (resultPid < 0) {
        switch (c->mode) {
          case MODE_VFORK:
            throwIOException(env, errno, "vfork failed");
            break;
          case MODE_FORK:
            throwIOException(env, errno, "fork failed");
            break;
          case MODE_POSIX_SPAWN:
            throwIOException(env, errno, "posix_spawn failed");
            break;
        }
        goto Catch;
    }
    close(fail[1]); fail[1] = -1; /* See: WhyCantJohnnyExec  (childproc.c)  */

    /* If we expect the child to ping aliveness, wait for it. */
    if (c->sendAlivePing) {
        switch(readFully(fail[0], &errnum, sizeof(errnum))) {
        case 0: /* First exec failed; */
            {
                int tmpStatus = 0;
                int p = waitpid(resultPid, &tmpStatus, 0);
                throwExitCause(env, p, tmpStatus);
                goto Catch;
            }
        case sizeof(errnum):
            assert(errnum == CHILD_IS_ALIVE);
            if (errnum != CHILD_IS_ALIVE) {
                /* Should never happen since the first thing the spawn
                 * helper should do is to send an alive ping to the parent,
                 * before doing any subsequent work. */
                throwIOException(env, 0, "Bad code from spawn helper "
                                         "(Failed to exec spawn helper)");
                goto Catch;
            }
            break;
        default:
            throwIOException(env, errno, "Read failed");
            goto Catch;
        }
    }

    switch (readFully(fail[0], &errnum, sizeof(errnum))) {
    case 0: break; /* Exec succeeded */
    case sizeof(errnum):
        waitpid(resultPid, NULL, 0);
        throwIOException(env, errnum, "Exec failed");
        goto Catch;
    default:
        throwIOException(env, errno, "Read failed");
        goto Catch;
    }

    fds[0] = (in [1] != -1) ? in [1] : -1;
    fds[1] = (out[0] != -1) ? out[0] : -1;
    fds[2] = (err[0] != -1) ? err[0] : -1;

 Finally:
    /* Always clean up the child's side of the pipes */
    closeSafely(in [0]);
    closeSafely(out[1]);
    closeSafely(err[1]);

    /* Always clean up fail and childEnv descriptors */
    closeSafely(fail[0]);
    closeSafely(fail[1]);
    closeSafely(childenv[0]);
    closeSafely(childenv[1]);

    releaseBytes(env, helperpath, phelperpath);
    releaseBytes(env, prog,       pprog);
    releaseBytes(env, argBlock,   pargBlock);
    releaseBytes(env, envBlock,   penvBlock);
    releaseBytes(env, dir,        c->pdir);

    free(c->argv);
    free(c->envv);
    free(c);

    if (fds != NULL)
        (*env)->ReleaseIntArrayElements(env, std_fds, fds, 0);

    return resultPid;

 Catch:
    /* Clean up the parent's side of the pipes in case of failure only */
    closeSafely(in [1]); in[1] = -1;
    closeSafely(out[0]); out[0] = -1;
    closeSafely(err[0]); err[0] = -1;
    goto Finally;
}

