/*
 * Copyright (c) 1995, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * Common AWT definitions
 */

#ifndef _AWT_
#define _AWT_

#include "jvm.h"
#include "jni_util.h"
#include "debug_util.h"

#if !defined(HEADLESS) && !defined(MACOSX)
#include <X11/Xlib.h>
#include <X11/Xutil.h>
typedef char Boolean;
#endif /* !HEADLESS && !MACOSX */


/* The JVM instance: defined in awt_MToolkit.c */
extern JavaVM *jvm;

extern jclass tkClass;
extern jmethodID awtLockMID;
extern jmethodID awtUnlockMID;
extern jmethodID awtWaitMID;
extern jmethodID awtNotifyMID;
extern jmethodID awtNotifyAllMID;
extern jboolean awtLockInited;

/* Perform sanity and consistency checks on AWT locking */
#ifdef DEBUG
#define DEBUG_AWT_LOCK
#endif

/*
 * The following locking primitives should be defined
 *
#define AWT_LOCK()
#define AWT_NOFLUSH_UNLOCK()
#define AWT_WAIT(tm)
#define AWT_NOTIFY()
#define AWT_NOTIFY_ALL()
 */

/*
 * Convenience macros based on AWT_NOFLUSH_UNLOCK
 */
extern void awt_output_flush();
#define AWT_UNLOCK() AWT_FLUSH_UNLOCK()
#define AWT_FLUSH_UNLOCK() do {                 \
    awt_output_flush();                         \
    AWT_NOFLUSH_UNLOCK();                       \
} while (0)

#define AWT_UNLOCK_CHECK_EXCEPTION(env) \
    do { \
      AWT_UNLOCK(); \
      JNU_CHECK_EXCEPTION(env); \
    } while (0)

#define AWT_LOCK_IMPL() \
    do { \
        (*env)->CallStaticVoidMethod(env, tkClass, awtLockMID); \
        if ((*env)->ExceptionCheck(env)) { \
            (*env)->ExceptionClear(env); \
        } \
    } while(0)

#define AWT_NOFLUSH_UNLOCK_IMPL() \
    do { \
      jthrowable pendingException; \
      if ((pendingException = (*env)->ExceptionOccurred(env)) != NULL) { \
         (*env)->ExceptionClear(env); \
      } \
      (*env)->CallStaticVoidMethod(env, tkClass, awtUnlockMID); \
      if ((*env)->ExceptionCheck(env)) { \
         (*env)->ExceptionClear(env); \
      } \
      if (pendingException) { \
         (*env)->Throw(env, pendingException); \
      } \
    } while (0)
#define AWT_WAIT_IMPL(tm) \
    (*env)->CallStaticVoidMethod(env, tkClass, awtWaitMID, (jlong)(tm))
#define AWT_NOTIFY_IMPL() \
    (*env)->CallStaticVoidMethod(env, tkClass, awtNotifyMID)
#define AWT_NOTIFY_ALL_IMPL() \
    (*env)->CallStaticVoidMethod(env, tkClass, awtNotifyAllMID)

/*
 * Unfortunately AWT_LOCK debugging does not work with XAWT due to mixed
 * Java/C use of AWT lock.
 */
#define AWT_LOCK()           AWT_LOCK_IMPL()
#define AWT_NOFLUSH_UNLOCK() AWT_NOFLUSH_UNLOCK_IMPL()
#define AWT_WAIT(tm)         AWT_WAIT_IMPL(tm)
#define AWT_NOTIFY()         AWT_NOTIFY_IMPL()
#define AWT_NOTIFY_ALL()     AWT_NOTIFY_ALL_IMPL()

#if !defined(HEADLESS) && !defined(MACOSX)
extern Display         *awt_display; /* awt_GraphicsEnv.c */
extern Boolean          awt_ModLockIsShiftLock; /* XToolkit.c */
#endif /* !HEADLESS && !MACOSX */

#endif /* ! _AWT_ */
