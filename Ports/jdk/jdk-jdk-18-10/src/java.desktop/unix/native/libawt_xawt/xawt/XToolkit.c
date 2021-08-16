/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>
#include <X11/Xatom.h>

#include <jvm.h>
#include <jni.h>
#include <jlong.h>
#include <jni_util.h>

#include "awt_p.h"
#include "awt_Component.h"
#include "awt_MenuComponent.h"
#include "awt_util.h"

#include "sun_awt_X11_XToolkit.h"
#include "java_awt_SystemColor.h"
#include "java_awt_TrayIcon.h"
#include <X11/extensions/XTest.h>

#include <unistd.h>

uint32_t awt_NumLockMask = 0;
Boolean  awt_ModLockIsShiftLock = False;

static int32_t num_buttons = 0;
int32_t getNumButtons();

extern JavaVM *jvm;

// Tracing level
static int tracing = 0;
#ifdef PRINT
#undef PRINT
#endif
#ifdef PRINT2
#undef PRINT2
#endif

#define PRINT if (tracing) printf
#define PRINT2 if (tracing > 1) printf


struct ComponentIDs componentIDs;

struct MenuComponentIDs menuComponentIDs;

extern Display* awt_init_Display(JNIEnv *env, jobject this);
extern void freeNativeStringArray(char **array, jsize length);
extern char** stringArrayToNative(JNIEnv *env, jobjectArray array, jsize * ret_length);

/* This function gets called from the static initializer for FileDialog.java
   to initialize the fieldIDs for fields that may be accessed from C */

JNIEXPORT void JNICALL
Java_java_awt_FileDialog_initIDs
  (JNIEnv *env, jclass cls)
{

}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XToolkit_initIDs
  (JNIEnv *env, jclass clazz)
{
    jfieldID fid = (*env)->GetStaticFieldID(env, clazz, "numLockMask", "I");
    CHECK_NULL(fid);
    awt_NumLockMask = (*env)->GetStaticIntField(env, clazz, fid);
    DTRACE_PRINTLN1("awt_NumLockMask = %u", awt_NumLockMask);
    fid = (*env)->GetStaticFieldID(env, clazz, "modLockIsShiftLock", "I");
    CHECK_NULL(fid);
    awt_ModLockIsShiftLock = (*env)->GetStaticIntField(env, clazz, fid) != 0 ? True : False;
}

/*
 * Class:     sun_awt_X11_XToolkit
 * Method:    getTrayIconDisplayTimeout
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XToolkit_getTrayIconDisplayTimeout
  (JNIEnv *env, jclass clazz)
{
    return (jlong) 2000;
}

/*
 * Class:     sun_awt_X11_XToolkit
 * Method:    getDefaultXColormap
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XToolkit_getDefaultXColormap
  (JNIEnv *env, jclass clazz)
{
    AWT_LOCK();
    AwtGraphicsConfigDataPtr defaultConfig =
        getDefaultConfig(DefaultScreen(awt_display));
    AWT_UNLOCK();
    return (jlong) defaultConfig->awt_cmap;
}

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jvm = vm;

    //Set the gtk backend to x11 on all the systems
    putenv("GDK_BACKEND=x11");

    return JNI_VERSION_1_2;
}

/*
 * Class:     sun_awt_X11_XToolkit
 * Method:    nativeLoadSystemColors
 * Signature: ([I)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XToolkit_nativeLoadSystemColors
  (JNIEnv *env, jobject this, jintArray systemColors)
{
    AWT_LOCK();
    AwtGraphicsConfigDataPtr defaultConfig =
        getDefaultConfig(DefaultScreen(awt_display));
    awtJNI_CreateColorData(env, defaultConfig, 1);
    AWT_UNLOCK();
}

JNIEXPORT void JNICALL
Java_java_awt_Component_initIDs
  (JNIEnv *env, jclass cls)
{
    jclass keyclass = NULL;


    componentIDs.x = (*env)->GetFieldID(env, cls, "x", "I");
    CHECK_NULL(componentIDs.x);
    componentIDs.y = (*env)->GetFieldID(env, cls, "y", "I");
    CHECK_NULL(componentIDs.y);
    componentIDs.width = (*env)->GetFieldID(env, cls, "width", "I");
    CHECK_NULL(componentIDs.width);
    componentIDs.height = (*env)->GetFieldID(env, cls, "height", "I");
    CHECK_NULL(componentIDs.height);
    componentIDs.isPacked = (*env)->GetFieldID(env, cls, "isPacked", "Z");
    CHECK_NULL(componentIDs.isPacked);
    componentIDs.peer =
      (*env)->GetFieldID(env, cls, "peer", "Ljava/awt/peer/ComponentPeer;");
    CHECK_NULL(componentIDs.peer);
    componentIDs.background =
      (*env)->GetFieldID(env, cls, "background", "Ljava/awt/Color;");
    CHECK_NULL(componentIDs.background);
    componentIDs.foreground =
      (*env)->GetFieldID(env, cls, "foreground", "Ljava/awt/Color;");
    CHECK_NULL(componentIDs.foreground);
    componentIDs.graphicsConfig =
        (*env)->GetFieldID(env, cls, "graphicsConfig",
                           "Ljava/awt/GraphicsConfiguration;");
    CHECK_NULL(componentIDs.graphicsConfig);
    componentIDs.name =
      (*env)->GetFieldID(env, cls, "name", "Ljava/lang/String;");
    CHECK_NULL(componentIDs.name);

    /* Use _NoClientCode() methods for trusted methods, so that we
     *  know that we are not invoking client code on trusted threads
     */
    componentIDs.getParent =
      (*env)->GetMethodID(env, cls, "getParent_NoClientCode",
                         "()Ljava/awt/Container;");
    CHECK_NULL(componentIDs.getParent);

    componentIDs.getLocationOnScreen =
      (*env)->GetMethodID(env, cls, "getLocationOnScreen_NoTreeLock",
                         "()Ljava/awt/Point;");
    CHECK_NULL(componentIDs.getLocationOnScreen);

    keyclass = (*env)->FindClass(env, "java/awt/event/KeyEvent");
    CHECK_NULL(keyclass);

    componentIDs.isProxyActive =
        (*env)->GetFieldID(env, keyclass, "isProxyActive",
                           "Z");
    CHECK_NULL(componentIDs.isProxyActive);

    componentIDs.appContext =
        (*env)->GetFieldID(env, cls, "appContext",
                           "Lsun/awt/AppContext;");

    (*env)->DeleteLocalRef(env, keyclass);
}


JNIEXPORT void JNICALL
Java_java_awt_Container_initIDs
  (JNIEnv *env, jclass cls)
{

}


JNIEXPORT void JNICALL
Java_java_awt_Button_initIDs
  (JNIEnv *env, jclass cls)
{

}

JNIEXPORT void JNICALL
Java_java_awt_Scrollbar_initIDs
  (JNIEnv *env, jclass cls)
{

}


JNIEXPORT void JNICALL
Java_java_awt_Window_initIDs
  (JNIEnv *env, jclass cls)
{

}

JNIEXPORT void JNICALL
Java_java_awt_Frame_initIDs
  (JNIEnv *env, jclass cls)
{

}


JNIEXPORT void JNICALL
Java_java_awt_MenuComponent_initIDs(JNIEnv *env, jclass cls)
{
    menuComponentIDs.appContext =
      (*env)->GetFieldID(env, cls, "appContext", "Lsun/awt/AppContext;");
}

JNIEXPORT void JNICALL
Java_java_awt_Cursor_initIDs(JNIEnv *env, jclass cls)
{
}


JNIEXPORT void JNICALL Java_java_awt_MenuItem_initIDs
  (JNIEnv *env, jclass cls)
{
}


JNIEXPORT void JNICALL Java_java_awt_Menu_initIDs
  (JNIEnv *env, jclass cls)
{
}

JNIEXPORT void JNICALL
Java_java_awt_TextArea_initIDs
  (JNIEnv *env, jclass cls)
{
}


JNIEXPORT void JNICALL
Java_java_awt_Checkbox_initIDs
  (JNIEnv *env, jclass cls)
{
}


JNIEXPORT void JNICALL Java_java_awt_ScrollPane_initIDs
  (JNIEnv *env, jclass cls)
{
}

JNIEXPORT void JNICALL
Java_java_awt_TextField_initIDs
  (JNIEnv *env, jclass cls)
{
}

JNIEXPORT void JNICALL Java_java_awt_Dialog_initIDs (JNIEnv *env, jclass cls)
{
}


/* ========================== Begin poll section ================================ */

// Includes

#include <sys/time.h>
#include <limits.h>
#include <locale.h>
#include <pthread.h>

#include <dlfcn.h>
#include <fcntl.h>

#include <poll.h>
#ifndef POLLRDNORM
#define POLLRDNORM POLLIN
#endif

// Prototypes

static void     waitForEvents(JNIEnv *, jlong);
static void     awt_pipe_init();
static Boolean  performPoll(JNIEnv *, jlong);
static void     wakeUp();
static void     update_poll_timeout(int timeout_control);
static uint32_t get_poll_timeout(jlong nextTaskTime);

// Defines

#ifndef bzero
#define bzero(a,b) memset(a, 0, b)
#endif

#define AWT_POLL_BUFSIZE        100 /* bytes */
#define AWT_READPIPE            (awt_pipe_fds[0])
#define AWT_WRITEPIPE           (awt_pipe_fds[1])

#define DEF_AWT_MAX_POLL_TIMEOUT ((uint32_t)500) /* milliseconds */
#define DEF_AWT_FLUSH_TIMEOUT ((uint32_t)100) /* milliseconds */
#define AWT_MIN_POLL_TIMEOUT ((uint32_t)0) /* milliseconds */

#define TIMEOUT_TIMEDOUT 0
#define TIMEOUT_EVENTS 1

/* awt_poll_alg - AWT Poll Events Aging Algorithms */
#define AWT_POLL_FALSE        1
#define AWT_POLL_AGING_SLOW   2
#define AWT_POLL_AGING_FAST   3

#define AWT_POLL_THRESHOLD 1000  // msec, Block if delay is larger
#define AWT_POLL_BLOCK       -1  // cause poll() block

// Static fields

static int          awt_poll_alg = AWT_POLL_AGING_SLOW;

static uint32_t AWT_FLUSH_TIMEOUT  =  DEF_AWT_FLUSH_TIMEOUT; /* milliseconds */
static uint32_t AWT_MAX_POLL_TIMEOUT = DEF_AWT_MAX_POLL_TIMEOUT; /* milliseconds */
static pthread_t    awt_MainThread = 0;
static int32_t      awt_pipe_fds[2];                   /* fds for wkaeup pipe */
static Boolean      awt_pipe_inited = False;           /* make sure pipe is initialized before write */
static jlong        awt_next_flush_time = 0LL; /* 0 == no scheduled flush */
static jlong        awt_last_flush_time = 0LL; /* 0 == no scheduled flush */
static uint32_t     curPollTimeout;
static struct pollfd pollFds[2];
static jlong        poll_sleep_time = 0LL; // Used for tracing
static jlong        poll_wakeup_time = 0LL; // Used for tracing

// AWT static poll timeout.  Zero means "not set", aging algorithm is
// used.  Static poll timeout values higher than 50 cause application
// look "slow" - they don't respond to user request fast
// enough. Static poll timeout value less than 10 are usually
// considered by schedulers as zero, so this might cause unnecessary
// CPU consumption by Java.  The values between 10 - 50 are suggested
// for single client desktop configurations.  For SunRay servers, it
// is highly recomended to use aging algorithm (set static poll timeout
// to 0).
static int32_t static_poll_timeout = 0;

static Bool isMainThread() {
    return awt_MainThread == pthread_self();
}

/*
 * Creates the AWT utility pipe. This pipe exists solely so that
 * we can cause the main event thread to wake up from a poll() or
 * select() by writing to this pipe.
 */
static void
awt_pipe_init() {

    if (awt_pipe_inited) {
        return;
    }

    if ( pipe ( awt_pipe_fds ) == 0 )
    {
        /*
        ** the write wakes us up from the infinite sleep, which
        ** then we cause a delay of AWT_FLUSHTIME and then we
        ** flush.
        */
        int32_t flags = 0;
        /* set the pipe to be non-blocking */
        flags = fcntl ( AWT_READPIPE, F_GETFL, 0 );
        fcntl( AWT_READPIPE, F_SETFL, flags | O_NDELAY | O_NONBLOCK );
        flags = fcntl ( AWT_WRITEPIPE, F_GETFL, 0 );
        fcntl( AWT_WRITEPIPE, F_SETFL, flags | O_NDELAY | O_NONBLOCK );
        awt_pipe_inited = True;
    }
    else
    {
        AWT_READPIPE = -1;
        AWT_WRITEPIPE = -1;
    }


} /* awt_pipe_init() */

/**
 * Reads environment variables to initialize timeout fields.
 */
static void readEnv() {
    char * value;
    int tmp_poll_alg;
    static Boolean env_read = False;
    if (env_read) return;

    env_read = True;

    value = getenv("_AWT_MAX_POLL_TIMEOUT");
    if (value != NULL) {
        AWT_MAX_POLL_TIMEOUT = atoi(value);
        if (AWT_MAX_POLL_TIMEOUT == 0) {
            AWT_MAX_POLL_TIMEOUT = DEF_AWT_MAX_POLL_TIMEOUT;
        }
    }
    curPollTimeout = AWT_MAX_POLL_TIMEOUT/2;

    value = getenv("_AWT_FLUSH_TIMEOUT");
    if (value != NULL) {
        AWT_FLUSH_TIMEOUT = atoi(value);
        if (AWT_FLUSH_TIMEOUT == 0) {
            AWT_FLUSH_TIMEOUT = DEF_AWT_FLUSH_TIMEOUT;
        }
    }

    value = getenv("_AWT_POLL_TRACING");
    if (value != NULL) {
        tracing = atoi(value);
    }

    value = getenv("_AWT_STATIC_POLL_TIMEOUT");
    if (value != NULL) {
        static_poll_timeout = atoi(value);
    }
    if (static_poll_timeout != 0) {
        curPollTimeout = static_poll_timeout;
    }

    // non-blocking poll()
    value = getenv("_AWT_POLL_ALG");
    if (value != NULL) {
        tmp_poll_alg = atoi(value);
        switch(tmp_poll_alg) {
        case AWT_POLL_FALSE:
        case AWT_POLL_AGING_SLOW:
        case AWT_POLL_AGING_FAST:
            awt_poll_alg = tmp_poll_alg;
            break;
        default:
            PRINT("Unknown value of _AWT_POLL_ALG, assuming Slow Aging Algorithm by default");
            awt_poll_alg = AWT_POLL_AGING_SLOW;
            break;
        }
    }
}

/**
 * Returns the amount of milliseconds similar to System.currentTimeMillis()
 */
static jlong
awtJNI_TimeMillis(void)
{
    struct timeval t;

    gettimeofday(&t, 0);

    return jlong_add(jlong_mul(jint_to_jlong(t.tv_sec), jint_to_jlong(1000)),
             jint_to_jlong(t.tv_usec / 1000));
}

/**
 * Updates curPollTimeout according to the aging algorithm.
 * @param timeout_control Either TIMEOUT_TIMEDOUT or TIMEOUT_EVENTS
 */
static void update_poll_timeout(int timeout_control) {
    PRINT2("tout: %d\n", timeout_control);

    // If static_poll_timeout is set, curPollTimeout has the fixed value
    if (static_poll_timeout != 0) return;

    // Update it otherwise

    switch(awt_poll_alg) {
    case AWT_POLL_AGING_SLOW:
        if (timeout_control == TIMEOUT_TIMEDOUT) {
            /* add 1/4 (plus 1, in case the division truncates to 0) */
            curPollTimeout += ((curPollTimeout>>2) + 1);
            curPollTimeout = min(AWT_MAX_POLL_TIMEOUT, curPollTimeout);
        } else if (timeout_control == TIMEOUT_EVENTS) {
            /* subtract 1/4 (plus 1, in case the division truncates to 0) */
            if (curPollTimeout > 0) {
                curPollTimeout -= ((curPollTimeout>>2) + 1);
                curPollTimeout = max(AWT_MIN_POLL_TIMEOUT, curPollTimeout);
            }
        }
        break;
    case AWT_POLL_AGING_FAST:
        if (timeout_control == TIMEOUT_TIMEDOUT) {
            curPollTimeout += ((curPollTimeout>>2) + 1);
            curPollTimeout = min(AWT_MAX_POLL_TIMEOUT, curPollTimeout);
            if((int)curPollTimeout > AWT_POLL_THRESHOLD || (int)curPollTimeout == AWT_POLL_BLOCK)
                curPollTimeout = AWT_POLL_BLOCK;
        } else if (timeout_control == TIMEOUT_EVENTS) {
            curPollTimeout = max(AWT_MIN_POLL_TIMEOUT, 1);
        }
        break;
    }
}

/*
 * Gets the best timeout for the next call to poll().
 *
 * @param nextTaskTime -1, if there are no tasks; next time when
 * timeout task needs to be run, in millis(of currentTimeMillis)
 */
static uint32_t get_poll_timeout(jlong nextTaskTime)
{
    uint32_t ret_timeout = 0;
    uint32_t timeout;
    uint32_t taskTimeout;
    uint32_t flushTimeout;

    jlong curTime = awtJNI_TimeMillis();
    timeout = curPollTimeout;
    switch(awt_poll_alg) {
    case AWT_POLL_AGING_SLOW:
    case AWT_POLL_AGING_FAST:
        taskTimeout = (nextTaskTime == -1) ? AWT_MAX_POLL_TIMEOUT : (uint32_t)max(0, (int32_t)(nextTaskTime - curTime));
        flushTimeout = (awt_next_flush_time > 0) ? (uint32_t)max(0, (int32_t)(awt_next_flush_time - curTime)) : AWT_MAX_POLL_TIMEOUT;

        PRINT2("to: %d, ft: %d, to: %d, tt: %d, mil: %d\n", taskTimeout, flushTimeout, timeout, (int)nextTaskTime, (int)curTime);

        // Adjust timeout to flush_time and task_time
        ret_timeout = min(flushTimeout, min(taskTimeout, timeout));
        if((int)curPollTimeout == AWT_POLL_BLOCK)
           ret_timeout = AWT_POLL_BLOCK;
        break;

    case AWT_POLL_FALSE:
        ret_timeout = (nextTaskTime > curTime) ?
            (nextTaskTime - curTime) :
            ((nextTaskTime == -1) ? -1 : 0);
        break;
    }

    return ret_timeout;

} /* get_poll_timeout() */

/*
 * Waits for X events to appear on the pipe. Returns only when
 * it is likely (but not definite) that there are events waiting to
 * be processed.
 *
 * This routine also flushes the outgoing X queue, when the
 * awt_next_flush_time has been reached.
 *
 * If fdAWTPipe is greater or equal than zero the routine also
 * checks if there are events pending on the putback queue.
 */
void
waitForEvents(JNIEnv *env, jlong nextTaskTime) {
    if (performPoll(env, nextTaskTime)
          && (awt_next_flush_time > 0)
          && (awtJNI_TimeMillis() >= awt_next_flush_time)) {

                XFlush(awt_display);
                awt_last_flush_time = awt_next_flush_time;
                awt_next_flush_time = 0LL;
    }
} /* waitForEvents() */

JNIEXPORT void JNICALL Java_sun_awt_X11_XToolkit_waitForEvents (JNIEnv *env, jclass class, jlong nextTaskTime) {
    waitForEvents(env, nextTaskTime);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XToolkit_awt_1toolkit_1init (JNIEnv *env, jclass class) {
    awt_MainThread = pthread_self();

    awt_pipe_init();
    readEnv();
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XToolkit_awt_1output_1flush (JNIEnv *env, jclass class) {
    awt_output_flush();
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XToolkit_wakeup_1poll (JNIEnv *env, jclass class) {
    wakeUp();
}

/*
 * Polls both the X pipe and our AWT utility pipe. Returns
 * when there is data on one of the pipes, or the operation times
 * out.
 *
 * Not all Xt events come across the X pipe (e.g., timers
 * and alternate inputs), so we must time out every now and
 * then to check the Xt event queue.
 *
 * The fdAWTPipe will be empty when this returns.
 */
static Boolean
performPoll(JNIEnv *env, jlong nextTaskTime) {
    static Bool pollFdsInited = False;
    static char read_buf[AWT_POLL_BUFSIZE + 1];    /* dummy buf to empty pipe */

    uint32_t timeout = get_poll_timeout(nextTaskTime);
    int32_t result;

    if (!pollFdsInited) {
        pollFds[0].fd = ConnectionNumber(awt_display);
        pollFds[0].events = POLLRDNORM;
        pollFds[0].revents = 0;

        pollFds[1].fd = AWT_READPIPE;
        pollFds[1].events = POLLRDNORM;
        pollFds[1].revents = 0;
        pollFdsInited = True;
    } else {
        pollFds[0].revents = 0;
        pollFds[1].revents = 0;
    }

    AWT_NOFLUSH_UNLOCK();

    /* ACTUALLY DO THE POLL() */
    if (timeout == 0) {
        // be sure other threads get a chance
        if (!awtJNI_ThreadYield(env)) {
            return FALSE;
        }
    }

    if (tracing) poll_sleep_time = awtJNI_TimeMillis();
    result = poll( pollFds, 2, (int32_t) timeout );
    if (tracing) poll_wakeup_time = awtJNI_TimeMillis();
    PRINT("%d of %d, res: %d\n", (int)(poll_wakeup_time-poll_sleep_time), (int)timeout, result);

    AWT_LOCK();
    if (result == 0) {
        /* poll() timed out -- update timeout value */
        update_poll_timeout(TIMEOUT_TIMEDOUT);
        PRINT2("performPoll(): TIMEOUT_TIMEDOUT curPollTimeout = %d \n", curPollTimeout);
    }
    if (pollFds[1].revents) {
        int count;
        PRINT("Woke up\n");
        /* There is data on the AWT pipe - empty it */
        do {
            count = read(AWT_READPIPE, read_buf, AWT_POLL_BUFSIZE );
        } while (count == AWT_POLL_BUFSIZE );
        PRINT2("performPoll():  data on the AWT pipe: curPollTimeout = %d \n", curPollTimeout);
    }
    if (pollFds[0].revents) {
        // Events in X pipe
        update_poll_timeout(TIMEOUT_EVENTS);
        PRINT2("performPoll(): TIMEOUT_EVENTS curPollTimeout = %d \n", curPollTimeout);
    }
    return TRUE;

} /* performPoll() */

/**
 * Schedules next auto-flush event or performs forced flush depending
 * on the time of the previous flush.
 */
void awt_output_flush() {
    if (awt_next_flush_time == 0) {
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

        jlong curTime = awtJNI_TimeMillis(); // current time
        jlong l_awt_last_flush_time = awt_last_flush_time; // last time we flushed queue
        jlong next_flush_time = l_awt_last_flush_time + AWT_FLUSH_TIMEOUT;

        if (curTime >= next_flush_time) {
            // Enough time passed from last flush
            PRINT("f1\n");
            AWT_LOCK();
            XFlush(awt_display);
            awt_last_flush_time = curTime;
            AWT_NOFLUSH_UNLOCK();
        } else {
            awt_next_flush_time = next_flush_time;
            PRINT("f2\n");
            wakeUp();
        }
    }
}


/**
 * Wakes-up poll() in performPoll
 */
static void wakeUp() {
    static char wakeUp_char = 'p';
    if (!isMainThread() && awt_pipe_inited) {
        write ( AWT_WRITEPIPE, &wakeUp_char, 1 );
    }
}


/* ========================== End poll section ================================= */

/*
 * Class:     java_awt_KeyboardFocusManager
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_KeyboardFocusManager_initIDs
    (JNIEnv *env, jclass cls)
{
}

/*
 * Class:     sun_awt_X11_XToolkit
 * Method:    getEnv
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XToolkit_getEnv
(JNIEnv *env , jclass clazz, jstring key) {
    char *ptr = NULL;
    const char *keystr = NULL;
    jstring ret = NULL;

    keystr = JNU_GetStringPlatformChars(env, key, NULL);
    if (keystr) {
        ptr = getenv(keystr);
        if (ptr) {
            ret = JNU_NewStringPlatform(env, (const char *) ptr);
        }
        JNU_ReleaseStringPlatformChars(env, key, (const char*)keystr);
    }
    return ret;
}

Window get_xawt_root_shell(JNIEnv *env) {
  static jclass classXRootWindow = NULL;
  static jmethodID methodGetXRootWindow = NULL;
  static Window xawt_root_shell = None;

  if (xawt_root_shell == None){
      if (classXRootWindow == NULL){
          jclass cls_tmp = (*env)->FindClass(env, "sun/awt/X11/XRootWindow");
          if (!JNU_IsNull(env, cls_tmp)) {
              classXRootWindow = (jclass)(*env)->NewGlobalRef(env, cls_tmp);
              (*env)->DeleteLocalRef(env, cls_tmp);
          }
      }
      if( classXRootWindow != NULL) {
          methodGetXRootWindow = (*env)->GetStaticMethodID(env, classXRootWindow, "getXRootWindow", "()J");
      }
      if( classXRootWindow != NULL && methodGetXRootWindow !=NULL ) {
          xawt_root_shell = (Window) (*env)->CallStaticLongMethod(env, classXRootWindow, methodGetXRootWindow);
      }
      if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
      }
  }
  return xawt_root_shell;
}

/*
 * Class:     java_awt_TrayIcon
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_java_awt_TrayIcon_initIDs(JNIEnv *env , jclass clazz)
{
}


/*
 * Class:     java_awt_Cursor
 * Method:    finalizeImpl
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_java_awt_Cursor_finalizeImpl(JNIEnv *env, jclass clazz, jlong pData)
{
    Cursor xcursor;

    xcursor = (Cursor)pData;
    if (xcursor != None) {
        AWT_LOCK();
        XFreeCursor(awt_display, xcursor);
        AWT_UNLOCK();
    }
}


/*
 * Class:     sun_awt_X11_XToolkit
 * Method:    getNumberOfButtonsImpl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XToolkit_getNumberOfButtonsImpl
(JNIEnv * env, jobject cls){
    if (num_buttons == 0) {
        num_buttons = getNumButtons();
    }
    return num_buttons;
}

int32_t getNumButtons() {
    int32_t major_opcode, first_event, first_error;
    int32_t xinputAvailable;
    int32_t numDevices, devIdx, clsIdx;
    XDeviceInfo* devices;
    XDeviceInfo* aDevice;
    XButtonInfo* bInfo;
    int32_t local_num_buttons = 0;

    /* 4700242:
     * If XTest is asked to press a non-existant mouse button
     * (i.e. press Button3 on a system configured with a 2-button mouse),
     * then a crash may happen.  To avoid this, we use the XInput
     * extension to query for the number of buttons on the XPointer, and check
     * before calling XTestFakeButtonEvent().
     */
    xinputAvailable = XQueryExtension(awt_display, INAME, &major_opcode, &first_event, &first_error);
    if (xinputAvailable) {
        DTRACE_PRINTLN3("RobotPeer: XQueryExtension(XINPUT) returns major_opcode = %d, first_event = %d, first_error = %d",
                        major_opcode, first_event, first_error);
        devices = XListInputDevices(awt_display, &numDevices);
        for (devIdx = 0; devIdx < numDevices; devIdx++) {
            aDevice = &(devices[devIdx]);
#ifdef IsXExtensionPointer
            if (aDevice->use == IsXExtensionPointer) {
                for (clsIdx = 0; clsIdx < aDevice->num_classes; clsIdx++) {
                    if (aDevice->inputclassinfo[clsIdx].class == ButtonClass) {
                        bInfo = (XButtonInfo*)(&(aDevice->inputclassinfo[clsIdx]));
                        local_num_buttons = bInfo->num_buttons;
                        DTRACE_PRINTLN1("RobotPeer: XPointer has %d buttons", num_buttons);
                        break;
                    }
                }
                break;
            }
#endif
            if (local_num_buttons <= 0 ) {
                if (aDevice->use == IsXPointer) {
                    for (clsIdx = 0; clsIdx < aDevice->num_classes; clsIdx++) {
                        if (aDevice->inputclassinfo[clsIdx].class == ButtonClass) {
                            bInfo = (XButtonInfo*)(&(aDevice->inputclassinfo[clsIdx]));
                            local_num_buttons = bInfo->num_buttons;
                            DTRACE_PRINTLN1("RobotPeer: XPointer has %d buttons", num_buttons);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        XFreeDeviceList(devices);
    }
    else {
        DTRACE_PRINTLN1("RobotPeer: XINPUT extension is unavailable, assuming %d mouse buttons", num_buttons);
    }
    if (local_num_buttons == 0 ) {
        local_num_buttons = 3;
    }

    return local_num_buttons;
}

/*
 * Class:     sun_awt_X11_XWindowPeer
 * Method:    getJvmPID
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XWindowPeer_getJvmPID
(JNIEnv *env, jclass cls)
{
    /* Return the JVM's PID. */
    return getpid();
}

#ifndef HOST_NAME_MAX
#define HOST_NAME_MAX 1024 /* Overestimated */
#endif

/*
 * Class:     sun_awt_X11_XWindowPeer
 * Method:    getLocalHostname
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XWindowPeer_getLocalHostname
(JNIEnv *env, jclass cls)
{
    /* Return the machine's FQDN. */
    char hostname[HOST_NAME_MAX + 1];
    if (gethostname(hostname, HOST_NAME_MAX + 1) == 0) {
        hostname[HOST_NAME_MAX] = '\0';
        jstring res = (*env)->NewStringUTF(env, hostname);
        return res;
    }

    return (jstring)NULL;
}
