/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include "awt_util.h"
#include "jni.h"
#include "jlong.h"
#include "Region.h"
#include "sizecalc.h"
#include "utility/rect.h"

#include "sun_awt_X11_XlibWrapper.h"

#include <stdlib.h>
#include <string.h>
#include <X11/extensions/Xdbe.h>
#include <X11/extensions/shape.h>
#include <X11/keysym.h>
#include <X11/Sunkeysym.h>
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <X11/XKBlib.h>
#include <X11/Xos.h>
#include <X11/Xutil.h>

#if defined(AIX)
#undef X_HAVE_UTF8_STRING
extern Bool statusWindowEventHandler(XEvent event);
#endif

// From XWindow.c
extern KeySym keycodeToKeysym(Display *display, KeyCode keycode, int index);

#if defined(DEBUG)
static jmethodID lockIsHeldMID = NULL;

static void
CheckHaveAWTLock(JNIEnv *env)
{
    if (lockIsHeldMID == NULL) {
        if (tkClass == NULL) return;
        lockIsHeldMID =
            (*env)->GetStaticMethodID(env, tkClass,
                                      "isAWTLockHeldByCurrentThread", "()Z");
        if (lockIsHeldMID == NULL) return;
    }
    if (!(*env)->CallStaticBooleanMethod(env, tkClass, lockIsHeldMID)) {
        JNU_ThrowInternalError(env, "Current thread does not hold AWT_LOCK!");
    }
}

#define AWT_CHECK_HAVE_LOCK()                       \
    do {                                            \
        CheckHaveAWTLock(env);                      \
        if ((*env)->ExceptionCheck(env)) {          \
            return;                                 \
        }                                           \
    } while (0);                                    \

#define AWT_CHECK_HAVE_LOCK_RETURN(ret)             \
    do {                                            \
        CheckHaveAWTLock(env);                      \
        if ((*env)->ExceptionCheck(env)) {          \
            return (ret);                           \
        }                                           \
    } while (0);                                    \

#else
#define AWT_CHECK_HAVE_LOCK()
#define AWT_CHECK_HAVE_LOCK_RETURN(ret)
#endif

void freeNativeStringArray(char **array, jsize length) {
    int i;
    if (array == NULL) {
        return;
    }
    for (i = 0; i < length; i++) {
        free(array[i]);
    }
    free(array);
}

char** stringArrayToNative(JNIEnv *env, jobjectArray array, jsize * ret_length) {
    Bool err = FALSE;
    char ** strings;
    int index, str_index = 0;
    jsize length = (*env)->GetArrayLength(env, array);

    if (length == 0) {
        return NULL;
    }

    strings = (char**) calloc(length, sizeof (char*));

    if (strings == NULL) {
        JNU_ThrowOutOfMemoryError(env, "");
        return NULL;
    }

    for (index = 0; index < length; index++) {
        jstring str = (*env)->GetObjectArrayElement(env, array, index);
        if (str != NULL) {
            const char * str_char = JNU_GetStringPlatformChars(env, str, NULL);
            if (str_char != NULL) {
                char * dup_str = strdup(str_char);
                if (dup_str != NULL) {
                    strings[str_index++] = dup_str;
                } else {
                    JNU_ThrowOutOfMemoryError(env, "");
                    err = TRUE;
                }
                JNU_ReleaseStringPlatformChars(env, str, str_char);
            } else {
                err = TRUE;
            }
            (*env)->DeleteLocalRef(env, str);
            if (err) {
                break;
            }
        }
    }

    if (err) {
        freeNativeStringArray(strings, str_index);
        strings = NULL;
        str_index = -1;
    }
    *ret_length = str_index;

    return strings;
}

/*
 * Class:     XlibWrapper
 * Method:    XOpenDisplay
 * Signature: (J)J
 */

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XOpenDisplay
(JNIEnv *env, jclass clazz, jlong display_name)
{
    Display *dp;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    dp  =  XOpenDisplay((char *) jlong_to_ptr(display_name));

    return ptr_to_jlong(dp);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XCloseDisplay(JNIEnv *env, jclass clazz,
                       jlong display) {
    AWT_CHECK_HAVE_LOCK();
    XCloseDisplay((Display*) jlong_to_ptr(display));
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XDisplayString(JNIEnv *env, jclass clazz,
                        jlong display) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XDisplayString((Display*) jlong_to_ptr(display)));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XSetCloseDownMode(JNIEnv *env, jclass clazz,
                           jlong display, jint mode) {
    AWT_CHECK_HAVE_LOCK();
    XSetCloseDownMode((Display*) jlong_to_ptr(display), (int)mode);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DefaultScreen
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_DefaultScreen (JNIEnv *env, jclass clazz, jlong display) {

    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) DefaultScreen((Display *) jlong_to_ptr(display));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    ScreenOfDisplay
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_ScreenOfDisplay(JNIEnv *env, jclass clazz, jlong display, jlong screen_number) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(ScreenOfDisplay((Display *) jlong_to_ptr(display),
                                        screen_number));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DoesBackingStore
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_DoesBackingStore(JNIEnv *env, jclass clazz, jlong screen) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jint) DoesBackingStore((Screen*) jlong_to_ptr(screen));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DisplayWidth
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_DisplayWidth
(JNIEnv *env, jclass clazz, jlong display, jlong screen) {

    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) DisplayWidth((Display *) jlong_to_ptr(display),screen);

}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DisplayWidthMM
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_DisplayWidthMM
(JNIEnv *env, jclass clazz, jlong display, jlong screen) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) DisplayWidthMM((Display *) jlong_to_ptr(display),screen);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DisplayHeight
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_DisplayHeight
(JNIEnv *env, jclass clazz, jlong display, jlong screen) {

    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) DisplayHeight((Display *) jlong_to_ptr(display),screen);
}
/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    DisplayHeightMM
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_DisplayHeightMM
(JNIEnv *env, jclass clazz, jlong display, jlong screen) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) DisplayHeightMM((Display *) jlong_to_ptr(display),screen);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    RootWindow
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_RootWindow
(JNIEnv *env , jclass clazz, jlong display, jlong screen_number) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) RootWindow((Display *) jlong_to_ptr(display), screen_number);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    ScreenCount
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_ScreenCount
(JNIEnv *env , jclass clazz, jlong display) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ScreenCount((Display *) jlong_to_ptr(display));
}

/*
 * Class:     XlibWrapper
 * Method:    XCreateWindow
 * Signature: (JJIIIIIIJJJJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XCreateWindow
  (JNIEnv *env, jclass clazz, jlong display, jlong window,
   jint x, jint y, jint w, jint h , jint border_width, jint depth,
   jlong wclass, jlong visual, jlong valuemask, jlong attributes)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return  XCreateWindow((Display *) jlong_to_ptr(display),(Window) window, x, y, w, h,
              border_width, depth, wclass, (Visual *) jlong_to_ptr(visual),
              valuemask, (XSetWindowAttributes *) jlong_to_ptr(attributes));
}

/*
 * Class:     XlibWrapper
 * Method:    XConvertCase
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XConvertCase
  (JNIEnv *env, jclass clazz, jlong keysym,
   jlong keysym_lowercase, jlong keysym_uppercase)
{
    AWT_CHECK_HAVE_LOCK();
    XConvertCase(keysym, (jlong_to_ptr(keysym_lowercase)),
                         (jlong_to_ptr(keysym_uppercase)));
}

/*
 * Class:     XlibWrapper
 * Method:    XMapWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XMapWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XMapWindow( (Display *)jlong_to_ptr(display),(Window) window);
}

/*
 * Class:     XlibWrapper
 * Method:    XMapRaised
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XMapRaised
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XMapRaised( (Display *)jlong_to_ptr(display),(Window) window);
}

/*
 * Class:     XlibWrapper
 * Method:    XRaiseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XRaiseWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XRaiseWindow( (Display *)jlong_to_ptr(display),(Window) window);
}

/*
 * Class:     XlibWrapper
 * Method:    XLowerWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XLowerWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XLowerWindow( (Display *)jlong_to_ptr(display),(Window) window);
}

/*
 * Class:     XlibWrapper
 * Method:    XRestackWindows
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XRestackWindows
(JNIEnv *env, jclass clazz, jlong display, jlong windows, jint length)
{
    AWT_CHECK_HAVE_LOCK();
    XRestackWindows( (Display *) jlong_to_ptr(display), (Window *) jlong_to_ptr(windows), length);
}

/*
 * Class:     XlibWrapper
 * Method:    XConfigureWindow
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XConfigureWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong value_mask,
 jlong values)
{
    AWT_CHECK_HAVE_LOCK();
    XConfigureWindow((Display*)jlong_to_ptr(display), (Window)window,
            (unsigned int)value_mask, (XWindowChanges*)jlong_to_ptr(values));
}

/*
 * Class:     XlibWrapper
 * Method:    XSetInputFocus
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetInputFocus
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XSetInputFocus( (Display *)jlong_to_ptr(display),(Window) window, RevertToPointerRoot, CurrentTime);
}

/*
 * Class:     XlibWrapper
 * Method:    XSetInputFocus2
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetInputFocus2
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong time)
{
    AWT_CHECK_HAVE_LOCK();
    XSetInputFocus( (Display *)jlong_to_ptr(display),(Window) window, RevertToPointerRoot, time);
}

/*
 * Class:     XlibWrapper
 * Method:    XGetInputFocus
 * Signature: (JJ)V
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XGetInputFocus
(JNIEnv *env, jclass clazz, jlong display)
{
    Window focusOwner;
    int revert_to;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    XGetInputFocus( (Display *)jlong_to_ptr(display), &focusOwner, &revert_to);
    return focusOwner;
}

/*
 * Class:     XlibWrapper
 * Method:    XDestroyWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XDestroyWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XDestroyWindow( (Display *)jlong_to_ptr(display),(Window) window);
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGrabPointer
(JNIEnv *env, jclass clazz, jlong display, jlong window,
 jint owner_events, jint event_mask, jint pointer_mode,
 jint keyboard_mode, jlong confine_to, jlong cursor, jlong time)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XGrabPointer( (Display *)jlong_to_ptr(display), (Window) window,
             (Bool) owner_events, (unsigned int) event_mask, (int) pointer_mode,
             (int) keyboard_mode, (Window) confine_to, (Cursor) cursor, (Time) time);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XUngrabPointer
(JNIEnv *env, jclass clazz, jlong display, jlong time)
{
    AWT_CHECK_HAVE_LOCK();
    XUngrabPointer( (Display *)jlong_to_ptr(display), (Time) time);
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGrabKeyboard
(JNIEnv *env, jclass clazz, jlong display, jlong window,
 jint owner_events, jint pointer_mode,
 jint keyboard_mode, jlong time)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XGrabKeyboard( (Display *)jlong_to_ptr(display), (Window) window,
              (Bool) owner_events, (int) pointer_mode,
              (int) keyboard_mode, (Time) time);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XUngrabKeyboard
(JNIEnv *env, jclass clazz, jlong display, jlong time)
{
    AWT_CHECK_HAVE_LOCK();
    XUngrabKeyboard( (Display *)jlong_to_ptr(display), (Time) time);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XGrabServer(JNIEnv *env, jclass clazz,
                                         jlong display) {
     AWT_CHECK_HAVE_LOCK();
     XGrabServer((Display*)jlong_to_ptr(display));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XUngrabServer(JNIEnv *env, jclass clazz,
                                           jlong display) {
     AWT_CHECK_HAVE_LOCK();
     XUngrabServer((Display*)jlong_to_ptr(display));
     /* Workaround for bug 5039226 */
     XSync((Display*)jlong_to_ptr(display), False);
}

/*
 * Class:     XlibWrapper
 * Method:    XUnmapWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XUnmapWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{

    AWT_CHECK_HAVE_LOCK();
    XUnmapWindow( (Display *)jlong_to_ptr(display),(Window) window);

}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSelectInput
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong mask)
{
    AWT_CHECK_HAVE_LOCK();
    XSelectInput((Display *) jlong_to_ptr(display), (Window) window, mask);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XkbSelectEvents
(JNIEnv *env, jclass clazz, jlong display, jlong device, jlong bits_to_change,
              jlong values_for_bits)
{
    AWT_CHECK_HAVE_LOCK();
    XkbSelectEvents((Display *) jlong_to_ptr(display), (unsigned int)device,
                   (unsigned long)bits_to_change,
                   (unsigned long)values_for_bits);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XkbSelectEventDetails
(JNIEnv *env, jclass clazz, jlong display, jlong device, jlong event_type,
              jlong bits_to_change, jlong values_for_bits)
{
    AWT_CHECK_HAVE_LOCK();
    XkbSelectEventDetails((Display *) jlong_to_ptr(display), (unsigned int)device,
                   (unsigned int) event_type,
                   (unsigned long)bits_to_change,
                   (unsigned long)values_for_bits);
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XkbQueryExtension
(JNIEnv *env, jclass clazz, jlong display, jlong opcode_rtrn, jlong event_rtrn,
              jlong error_rtrn, jlong major_in_out, jlong minor_in_out)
{
    Bool status;
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    status = XkbQueryExtension((Display *) jlong_to_ptr(display),
                               (int *) jlong_to_ptr(opcode_rtrn),
                               (int *) jlong_to_ptr(event_rtrn),
                               (int *) jlong_to_ptr(error_rtrn),
                               (int *) jlong_to_ptr(major_in_out),
                               (int *) jlong_to_ptr(minor_in_out));
    return status ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XkbLibraryVersion
(JNIEnv *env, jclass clazz, jlong lib_major_in_out, jlong lib_minor_in_out)
{
    Bool status;
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    *((int *)jlong_to_ptr(lib_major_in_out)) =  XkbMajorVersion;
    *((int *)jlong_to_ptr(lib_minor_in_out)) =  XkbMinorVersion;
    status = XkbLibraryVersion((int *)jlong_to_ptr(lib_major_in_out),
                               (int *)jlong_to_ptr(lib_minor_in_out));
    return status ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XkbGetMap
(JNIEnv *env, jclass clazz, jlong display, jlong which, jlong device_spec)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) XkbGetMap( (Display *) jlong_to_ptr(display),
                              (unsigned int) which,
                              (unsigned int) device_spec);
}

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XkbGetUpdatedMap
(JNIEnv *env, jclass clazz, jlong display, jlong which, jlong xkb)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) XkbGetUpdatedMap( (Display *) jlong_to_ptr(display),
                              (unsigned int) which,
                              (XkbDescPtr) jlong_to_ptr(xkb));
}
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XkbFreeKeyboard
(JNIEnv *env, jclass clazz, jlong xkb, jlong which, jboolean free_all)
{
    AWT_CHECK_HAVE_LOCK();
    XkbFreeKeyboard(jlong_to_ptr(xkb), (unsigned int)which, free_all);
}
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XkbTranslateKeyCode
(JNIEnv *env, jclass clazz, jlong xkb, jint keycode, jlong mods, jlong mods_rtrn, jlong keysym_rtrn)
{
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    Bool b;
    b = XkbTranslateKeyCode((XkbDescPtr)xkb, (unsigned int)keycode, (unsigned int)mods,
                              (unsigned int *)jlong_to_ptr(mods_rtrn),
                               (KeySym *)jlong_to_ptr(keysym_rtrn));
    //printf("native,  input: keycode:0x%0X; mods:0x%0X\n", keycode, mods);
    //printf("native, output:  keysym:0x%0X; mods:0x%0X\n",
    //       *(unsigned int *)jlong_to_ptr(keysym_rtrn),
    //       *(unsigned int *)jlong_to_ptr(mods_rtrn));
    return b ? JNI_TRUE : JNI_FALSE;
}
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XkbSetDetectableAutoRepeat
(JNIEnv *env, jclass clazz, jlong display, jboolean detectable)
{
    AWT_CHECK_HAVE_LOCK();
    XkbSetDetectableAutoRepeat((Display *) jlong_to_ptr(display), detectable, NULL);
}
/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XNextEvent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XNextEvent
(JNIEnv *env, jclass clazz, jlong display, jlong ptr)
{
    AWT_CHECK_HAVE_LOCK();
    XNextEvent( (Display *) jlong_to_ptr(display), jlong_to_ptr(ptr));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XMaskEvent
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XMaskEvent
  (JNIEnv *env, jclass clazz, jlong display, jlong event_mask, jlong event_return)
{
    AWT_CHECK_HAVE_LOCK();
    XMaskEvent( (Display *) jlong_to_ptr(display), event_mask, (XEvent *) jlong_to_ptr(event_return));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XWindowEvent
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XWindowEvent
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jlong event_mask, jlong event_return)
{
    AWT_CHECK_HAVE_LOCK();
    XWindowEvent( (Display *) jlong_to_ptr(display), (Window)window, event_mask, (XEvent *) jlong_to_ptr(event_return));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFilterEvent
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XFilterEvent
(JNIEnv *env, jclass clazz, jlong ptr, jlong window)
{
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
#if defined(AIX)
    if (True == statusWindowEventHandler(*((XEvent *)(uintptr_t)ptr))) {
        return (jboolean)True;
    }
#endif
    return (jboolean) XFilterEvent((XEvent *) jlong_to_ptr(ptr), (Window) window);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSupportsLocale
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XSupportsLocale
(JNIEnv *env, jclass clazz)
{
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    return (jboolean)XSupportsLocale();
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetLocaleModifiers
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XlibWrapper_XSetLocaleModifiers
(JNIEnv *env, jclass clazz, jstring jstr)
{
    char * modifier_list = NULL;
    char * ret = NULL;

    if (!JNU_IsNull(env, jstr)) {
        modifier_list = (char *)JNU_GetStringPlatformChars(env, jstr, NULL);
        CHECK_NULL_RETURN(modifier_list, NULL);
    }

    AWT_CHECK_HAVE_LOCK_RETURN(NULL);
    if (modifier_list) {
        ret = XSetLocaleModifiers(modifier_list);
        JNU_ReleaseStringPlatformChars(env, jstr, (const char *) modifier_list);
    } else {
        ret = XSetLocaleModifiers("");
    }

    return (ret != NULL ? JNU_NewStringPlatform(env, ret): NULL);
}


/*
 * Class:     sun_awt_X11_wrappers_XlibWrapper
 * Method:    XPeekEvent
 * Signature: (JJ)V
 */


JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XPeekEvent
(JNIEnv *env, jclass clazz, jlong display, jlong ptr)
{
    AWT_CHECK_HAVE_LOCK();
    XPeekEvent((Display *) jlong_to_ptr(display),jlong_to_ptr(ptr));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XMoveResizeWindow
 * Signature: (JJIIII)V
 */
JNIEXPORT void JNICALL  Java_sun_awt_X11_XlibWrapper_XMoveResizeWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jint x , jint y , jint width, jint height) {
    AWT_CHECK_HAVE_LOCK();
    XMoveResizeWindow( (Display *) jlong_to_ptr(display), (Window) window, x, y, width, height);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XResizeWindow
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL  Java_sun_awt_X11_XlibWrapper_XResizeWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jint width, jint height) {
    AWT_CHECK_HAVE_LOCK();
    XResizeWindow( (Display *) jlong_to_ptr(display),(Window) window,width,height);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XMoveWindow
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL  Java_sun_awt_X11_XlibWrapper_XMoveWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jint width, jint height) {
    AWT_CHECK_HAVE_LOCK();
    XMoveWindow( (Display *) jlong_to_ptr(display),(Window) window,width,height);
}


/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetWindowBackground
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL  Java_sun_awt_X11_XlibWrapper_XSetWindowBackground
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong background_pixel) {
    AWT_CHECK_HAVE_LOCK();
    XSetWindowBackground((Display *) jlong_to_ptr(display),window,background_pixel);
}


/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFlush
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XFlush
(JNIEnv *env, jclass clazz, jlong display) {

    AWT_CHECK_HAVE_LOCK();
    XFlush((Display *)jlong_to_ptr(display));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSync
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSync
(JNIEnv *env, jclass clazz, jlong display, jint discard) {
    AWT_CHECK_HAVE_LOCK();
    XSync((Display *) jlong_to_ptr(display), discard);
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XTranslateCoordinates
(JNIEnv *env, jclass clazz, jlong display, jlong src_w, jlong dest_w,
 jlong src_x, jlong src_y, jlong dest_x_return, jlong dest_y_return,
 jlong child_return)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XTranslateCoordinates( (Display *) jlong_to_ptr(display), src_w, dest_w,
                  src_x, src_y,
                  (int *) jlong_to_ptr(dest_x_return),
                  (int *) jlong_to_ptr(dest_y_return),
                  (Window *) jlong_to_ptr(child_return));
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XEventsQueued
(JNIEnv *env, jclass clazz, jlong display, jint mode) {

    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XEventsQueued((Display *) jlong_to_ptr(display), mode);

}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    SetProperty
 * Signature: (JJJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_SetProperty
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong atom, jstring jstr) {
    char *cname;
    XTextProperty tp;
    int32_t status;

    /*
       In case there are direct support of UTF-8 declared, use UTF-8 strings.
    */
    if (!JNU_IsNull(env, jstr)) {
#ifdef X_HAVE_UTF8_STRING
        cname = (char *) (*env)->GetStringUTFChars(env, jstr, JNI_FALSE);
#else
        cname = (char *) JNU_GetStringPlatformChars(env, jstr, NULL);
#endif
        CHECK_NULL(cname);
    } else {
        cname = "";
    }

    AWT_CHECK_HAVE_LOCK();

#ifdef X_HAVE_UTF8_STRING
    status = Xutf8TextListToTextProperty((Display *)jlong_to_ptr(display), &cname, 1,
                                       XStdICCTextStyle, &tp);
#else
    status = XmbTextListToTextProperty((Display *)jlong_to_ptr(display), &cname, 1,
                                       XStdICCTextStyle, &tp);
#endif

    if (status == Success || status > 0) {
        XChangeProperty((Display *)jlong_to_ptr(display), window, atom, tp.encoding, tp.format, PropModeReplace, tp.value, tp.nitems);
        if (tp.value != NULL) {
            XFree(tp.value);
        }
    }

    if (!JNU_IsNull(env, jstr)) {
#ifdef X_HAVE_UTF8_STRING
        (*env)->ReleaseStringUTFChars(env, jstr, (const char *) cname);
#else
        JNU_ReleaseStringPlatformChars(env, jstr, (const char *) cname);
#endif
    }
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XChangeProperty
 * Signature: (JJJJJJJJJJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XChangePropertyImpl(
    JNIEnv *env, jclass clazz, jlong display, jlong window, jlong property,
    jlong type, jint format, jint mode, jlong data, jint nelements)
{
    AWT_CHECK_HAVE_LOCK();
    XChangeProperty((Display*) jlong_to_ptr(display), (Window) window, (Atom) property,
            (Atom) type, format, mode, (unsigned char*) jlong_to_ptr(data),
            nelements);
}
/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XChangePropertyS
 * Signature: (JJJJJJJJJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XChangePropertyS(
    JNIEnv *env, jclass clazz, jlong display, jlong window, jlong property,
    jlong type, jint format, jint mode, jstring value)
{
    jboolean iscopy;
    AWT_CHECK_HAVE_LOCK();
    const char * chars = JNU_GetStringPlatformChars(env, value, &iscopy);
    CHECK_NULL(chars);
    XChangeProperty((Display*)jlong_to_ptr(display), window, (Atom)property,
                    (Atom)type, format, mode, (unsigned char*)chars, strlen(chars));
    if (iscopy) {
        JNU_ReleaseStringPlatformChars(env, value, chars);
    }
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetWindowProperty
 * Signature: (JJJJJJJJJJJ)J;
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGetWindowProperty
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong property, jlong long_offset,
 jlong long_length, jlong delete, jlong req_type, jlong actual_type,
 jlong actual_format, jlong nitems_ptr, jlong bytes_after, jlong data_ptr)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XGetWindowProperty((Display*) jlong_to_ptr(display), window, property, long_offset, long_length,
                  delete, (Atom) req_type, (Atom*) jlong_to_ptr(actual_type),
                  (int *) jlong_to_ptr(actual_format), (unsigned long *) jlong_to_ptr(nitems_ptr),
                  (unsigned long*) jlong_to_ptr(bytes_after), (unsigned char**) jlong_to_ptr(data_ptr));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    GetProperty
 * Signature: (JJJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XlibWrapper_GetProperty
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong atom)
{
    /* Request status */
    int status;

    /* Returns of XGetWindowProperty */
    Atom actual_type;
    int actual_format;
    unsigned long nitems;
    unsigned long bytes_after;
    unsigned char * string;
    jstring res = NULL;
    AWT_CHECK_HAVE_LOCK_RETURN(NULL);
    status = XGetWindowProperty((Display*)jlong_to_ptr(display), window,
                                atom, 0, 0xFFFF, False, XA_STRING,
                                &actual_type, &actual_format, &nitems, &bytes_after,
                                &string);

    if (status != Success || string == NULL) {
        return NULL;
    }

    if (actual_type == XA_STRING && actual_format == 8) {
        res = JNU_NewStringPlatform(env,(char*) string);
    }
    XFree(string);
    return res;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    InternAtom
 * Signature: (JLjava/lang/String;I)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_InternAtom
(JNIEnv *env, jclass clazz, jlong display, jstring jstr, jint ife) {

    char *cname;
    unsigned long atom;

    AWT_CHECK_HAVE_LOCK_RETURN(0);

    if (!JNU_IsNull(env, jstr)) {
        cname = (char *)JNU_GetStringPlatformChars(env, jstr, NULL);
        CHECK_NULL_RETURN(cname, 0);
    } else {
        cname = "";
    }

    atom = XInternAtom((Display *) jlong_to_ptr(display), cname, ife);

    if (!JNU_IsNull(env, jstr)) {
        JNU_ReleaseStringPlatformChars(env, jstr, (const char *) cname);
    }

    return (jlong) atom;

}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XCreateFontCursor
(JNIEnv *env, jclass clazz, jlong display, jint shape) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XCreateFontCursor((Display *) jlong_to_ptr(display), (int) shape);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XCreatePixmapCursor
 * Signature: (JJJJJII)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XCreatePixmapCursor
(JNIEnv *env , jclass clazz, jlong display, jlong source, jlong mask, jlong fore, jlong back, jint x , jint y) {

    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong) XCreatePixmapCursor((Display *) jlong_to_ptr(display), (Pixmap) source, (Pixmap) mask,
                                       (XColor *) jlong_to_ptr(fore), (XColor *) jlong_to_ptr(back), x, y);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XQueryBestCursor
 * Signature: (JJIIJJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XQueryBestCursor
(JNIEnv *env, jclass clazz, jlong display, jlong drawable, jint width, jint height, jlong width_return, jlong height_return) {

    Status status;

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    status  =  XQueryBestCursor((Display *) jlong_to_ptr(display), (Drawable) drawable, width,height,
                                (unsigned int *) jlong_to_ptr(width_return), (unsigned int *) jlong_to_ptr(height_return));

    if (status == 0) return JNI_FALSE;
    else return JNI_TRUE;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFreeCursor
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XFreeCursor
(JNIEnv *env, jclass clazz, jlong display, jlong cursor) {

    AWT_CHECK_HAVE_LOCK();
    XFreeCursor( (Display *) jlong_to_ptr(display), (Cursor) cursor);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XQueryPointer
 * Signature: (JJJJJJJJJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XQueryPointer
(JNIEnv *env, jclass clazz, jlong display, jlong w, jlong root_return, jlong child_return, jlong root_x_return , jlong root_y_return, jlong win_x_return, jlong win_y_return, jlong mask_return) {

    Bool b;

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    b = XQueryPointer((Display *) jlong_to_ptr(display),
                      (Window) w, (Window *) jlong_to_ptr(root_return), (Window *) jlong_to_ptr(child_return),
                      (int *) jlong_to_ptr(root_x_return), (int *) jlong_to_ptr(root_y_return),
                      (int *) jlong_to_ptr(win_x_return), (int *) jlong_to_ptr(win_y_return),
                      (unsigned int *) jlong_to_ptr(mask_return));

    return b ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XChangeWindowAttributes
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XChangeWindowAttributes
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong valuemask, jlong attributes) {

    AWT_CHECK_HAVE_LOCK();
    XChangeWindowAttributes((Display *) jlong_to_ptr(display), (Window) window, (unsigned long) valuemask,
                            (XSetWindowAttributes *) jlong_to_ptr(attributes));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetTransientFor
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetTransientFor
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong transient_for_window)
{
    AWT_CHECK_HAVE_LOCK();
    XSetTransientForHint((Display *) jlong_to_ptr(display), window, transient_for_window);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetWMHints
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetWMHints
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong hints)
{
    AWT_CHECK_HAVE_LOCK();
    XSetWMHints((Display *) jlong_to_ptr(display), window, (XWMHints *) jlong_to_ptr(hints));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetWMHints
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XGetWMHints
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong hints)
{
    XWMHints * get_hints;
    AWT_CHECK_HAVE_LOCK();
    get_hints = XGetWMHints((Display*)jlong_to_ptr(display), window);
    if (get_hints != NULL) {
        memcpy(jlong_to_ptr(hints), get_hints, sizeof(XWMHints));
        XFree(get_hints);
    } else {
        memset(jlong_to_ptr(hints), 0, sizeof(XWMHints));
    }
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetPointerMapping
 * Signature: (JJI)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGetPointerMapping
(JNIEnv *env, jclass clazz, jlong display, jlong map, jint buttonNumber)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XGetPointerMapping((Display*)jlong_to_ptr(display), (unsigned char*) jlong_to_ptr(map), buttonNumber);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetDefault
 * Signature: (JJI)I
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XlibWrapper_XGetDefault
(JNIEnv *env, jclass clazz, jlong display, jstring program, jstring option)
{
    char * c_program = NULL;
    char * c_option = NULL;
    char * c_res = NULL;

    if (!JNU_IsNull(env, program)) {
        c_program = (char *)JNU_GetStringPlatformChars(env, program, NULL);
    }
    CHECK_NULL_RETURN(c_program, NULL);

    if (!JNU_IsNull(env, option)) {
        c_option = (char *)JNU_GetStringPlatformChars(env, option, NULL);
    }

    if (c_option == NULL) {
        JNU_ReleaseStringPlatformChars(env, program, (const char *) c_program);
        return NULL;
    }

    AWT_CHECK_HAVE_LOCK_RETURN(NULL);
    c_res = XGetDefault((Display*)jlong_to_ptr(display), c_program, c_option);
    // The strings returned by XGetDefault() are owned by Xlib and
    // should not be modified or freed by the client.

    JNU_ReleaseStringPlatformChars(env, program, (const char *) c_program);
    JNU_ReleaseStringPlatformChars(env, option, (const char *) c_option);

    if (c_res != NULL) {
        return JNU_NewStringPlatform(env, c_res);
    } else {
        return NULL;
    }
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    getScreenOfWindow
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_getScreenOfWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    XWindowAttributes attrs;
    memset(&attrs, 0, sizeof(attrs));
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    XGetWindowAttributes((Display *) jlong_to_ptr(display), window, &attrs);
    return ptr_to_jlong(attrs.screen);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XScreenNumberOfScreen
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XScreenNumberOfScreen
(JNIEnv *env, jclass clazz, jlong screen)
{
    AWT_CHECK_HAVE_LOCK_RETURN(-1);
    if(jlong_to_ptr(screen) == NULL) {
        return -1;
    }
    return XScreenNumberOfScreen((Screen*) jlong_to_ptr(screen));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XIconifyWindow
 * Signature: (JJJ)V
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XIconifyWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong screenNumber)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XIconifyWindow((Display*) jlong_to_ptr(display), window, screenNumber);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFree
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XFree
(JNIEnv *env, jclass clazz, jlong ptr)
{
    AWT_CHECK_HAVE_LOCK();
    XFree(jlong_to_ptr(ptr));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFree
 * Signature: (J)V
 */
JNIEXPORT jbyteArray JNICALL Java_sun_awt_X11_XlibWrapper_getStringBytes
(JNIEnv *env, jclass clazz, jlong str_ptr)
{
    unsigned char * str = (unsigned char*) jlong_to_ptr(str_ptr);
    long length = strlen((char*)str);
    jbyteArray res = (*env)->NewByteArray(env, length);
    CHECK_NULL_RETURN(res, NULL);
    (*env)->SetByteArrayRegion(env, res, 0, length,
                   (const signed char*) str);
    return res;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    ServerVendor
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11_XlibWrapper_ServerVendor
(JNIEnv *env, jclass clazz, jlong display)
{
    AWT_CHECK_HAVE_LOCK_RETURN(NULL);
    return JNU_NewStringPlatform(env, ServerVendor((Display*)jlong_to_ptr(display)));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    VendorRelease
 * Signature: (J)I;
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_VendorRelease
(JNIEnv *env, jclass clazz, jlong display)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return VendorRelease((Display*)jlong_to_ptr(display));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    IsXsunKPBehavior
 * Signature: (J)Z;
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_IsXsunKPBehavior
(JNIEnv *env, jclass clazz, jlong display)
{
    // Xsun without XKB uses keysymarray[2] keysym to determine if it is KP event.
    // Otherwise, it is [1] or sometimes [0].
    // This sniffer first tries to determine what is a keycode for XK_KP_7
    // using XKeysymToKeycode;
    // second, in which place in the keysymarray is XK_KP_7
    // using XKeycodeToKeysym.
    int kc7;
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    kc7 = XKeysymToKeycode((Display*)jlong_to_ptr(display), XK_KP_7);
    if( !kc7 ) {
        // keycode is not defined. Why, it's a reduced keyboard perhaps:
        // report arbitrarily false.
        return JNI_FALSE;
    } else {
        long ks2 = keycodeToKeysym((Display*)jlong_to_ptr(display), kc7, 2);
        if( ks2 == XK_KP_7 ) {
            //XXX If some Xorg server would put XK_KP_7 in keysymarray[2] as well,
            //XXX for yet unknown to me reason, the sniffer would lie.
            return JNI_TRUE;
        }else{
            return JNI_FALSE;
        }
    }
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_IsSunKeyboard
(JNIEnv *env, jclass clazz, jlong display)
{
    int xx;
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    xx = XKeysymToKeycode((Display*)jlong_to_ptr(display), SunXK_F37);
    return (!xx) ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_IsKanaKeyboard
(JNIEnv *env, jclass clazz, jlong display)
{
    int xx;
    static jboolean result = JNI_FALSE;

    int32_t minKeyCode, maxKeyCode, keySymsPerKeyCode;
    KeySym *keySyms, *keySymsStart, keySym;
    int32_t i;
    int32_t kanaCount = 0;

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);

    // There's no direct way to determine whether the keyboard has
    // a kana lock key. From available keyboard mapping tables, it looks
    // like only keyboards with the kana lock key can produce keysyms
    // for kana characters. So, as an indirect test, we check for those.
    XDisplayKeycodes((Display*)jlong_to_ptr(display), &minKeyCode, &maxKeyCode);
    keySyms = XGetKeyboardMapping((Display*)jlong_to_ptr(display), minKeyCode, maxKeyCode - minKeyCode + 1, &keySymsPerKeyCode);
    keySymsStart = keySyms;
    for (i = 0; i < (maxKeyCode - minKeyCode + 1) * keySymsPerKeyCode; i++) {
        keySym = *keySyms++;
        if ((keySym & 0xff00) == 0x0400) {
            kanaCount++;
        }
    }
    XFree(keySymsStart);

    // use a (somewhat arbitrary) minimum so we don't get confused by a stray function key
    result = kanaCount > 10;
    return result ? JNI_TRUE : JNI_FALSE;
}

JavaVM* jvm = NULL;
static int ToolkitErrorHandler(Display * dpy, XErrorEvent * event) {
    JNIEnv * env;
    // First call the native synthetic error handler declared in "awt_util.h" file.
    if (current_native_xerror_handler != NULL) {
        current_native_xerror_handler(dpy, event);
    }
    if (jvm != NULL) {
        env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        if (env) {
            return JNU_CallStaticMethodByName(env, NULL, "sun/awt/X11/XErrorHandlerUtil",
                "globalErrorHandler", "(JJ)I", ptr_to_jlong(dpy), ptr_to_jlong(event)).i;
        }
    }
    return 0;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    SetToolkitErrorHandler
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_SetToolkitErrorHandler
(JNIEnv *env, jclass clazz)
{
    if ((*env)->GetJavaVM(env, &jvm) < 0) {
        return 0;
    }
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XSetErrorHandler(ToolkitErrorHandler));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetErrorHandler
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetErrorHandler
(JNIEnv *env, jclass clazz, jlong handler)
{
    AWT_CHECK_HAVE_LOCK();
    XSetErrorHandler((XErrorHandler) jlong_to_ptr(handler));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    CallErrorHandler
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_CallErrorHandler
(JNIEnv *env, jclass clazz, jlong handler, jlong display, jlong event_ptr)
{
    return (*(XErrorHandler)jlong_to_ptr(handler))((Display*) jlong_to_ptr(display), (XErrorEvent*) jlong_to_ptr(event_ptr));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    PrintXErrorEvent
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_PrintXErrorEvent
(JNIEnv *env, jclass clazz, jlong display, jlong event_ptr)
{
    char msg[128];
    char buf[128];

    XErrorEvent* err = (XErrorEvent *)jlong_to_ptr(event_ptr);

    XGetErrorText((Display *)jlong_to_ptr(display), err->error_code, msg, sizeof(msg));
    jio_fprintf(stderr, "Xerror %s, XID %x, ser# %d\n", msg, err->resourceid, err->serial);
    jio_snprintf(buf, sizeof(buf), "%d", err->request_code);
    XGetErrorDatabaseText((Display *)jlong_to_ptr(display), "XRequest", buf, "Unknown", msg, sizeof(msg));
    jio_fprintf(stderr, "Major opcode %d (%s)\n", err->request_code, msg);
    if (err->request_code > 128) {
        jio_fprintf(stderr, "Minor opcode %d\n", err->minor_code);
    }
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XInternAtoms
 * Signature: (J[Ljava/lang/String;ZJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XInternAtoms
(JNIEnv *env, jclass clazz, jlong display, jobjectArray names_arr, jboolean only_if_exists, jlong atoms)
{
    int status = 0;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    jsize length;
    char** names = stringArrayToNative(env, names_arr, &length);
    if (names) {
        status = XInternAtoms((Display*)jlong_to_ptr(display), names, length, only_if_exists, (Atom*) jlong_to_ptr(atoms));
        freeNativeStringArray(names, length);
    }
    return status;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetWindowAttributes
 * Signature: (JJJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGetWindowAttributes
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong attr_ptr)
{
    jint status;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    memset((XWindowAttributes*) jlong_to_ptr(attr_ptr), 0, sizeof(XWindowAttributes));
    status =  XGetWindowAttributes((Display*)jlong_to_ptr(display), window, (XWindowAttributes*) jlong_to_ptr(attr_ptr));
    return status;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetGeometry
 * Signature: (JJJJJJJJJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGetGeometry
(JNIEnv *env, jclass clazz, jlong display, jlong drawable, jlong root_return,
     jlong x_return, jlong y_return, jlong width_return, jlong height_return,
     jlong border_width_return, jlong depth_return)
{
    jint status;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    status = XGetGeometry((Display *)jlong_to_ptr(display),
                          (Drawable)drawable, (Window *)jlong_to_ptr(root_return),
                          (int *)jlong_to_ptr(x_return), (int *)jlong_to_ptr(y_return),
                          (unsigned int *)jlong_to_ptr(width_return), (unsigned int *)jlong_to_ptr(height_return),
                          (unsigned int *)jlong_to_ptr(border_width_return),
                          (unsigned int *)jlong_to_ptr(depth_return));
    return status;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetWMNormalHints
 * Signature: (JJJJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XGetWMNormalHints
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong hints, jlong supplied_return)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XGetWMNormalHints((Display*) jlong_to_ptr(display),
                             window,
                             (XSizeHints*) jlong_to_ptr(hints),
                             (long*) jlong_to_ptr(supplied_return));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetWMNormalHints
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetWMNormalHints
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong hints)
{
    AWT_CHECK_HAVE_LOCK();
    XSetWMNormalHints((Display*) jlong_to_ptr(display), window, (XSizeHints*) jlong_to_ptr(hints));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XDeleteProperty
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XDeleteProperty
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong atom)
{
    AWT_CHECK_HAVE_LOCK();
    XDeleteProperty((Display*) jlong_to_ptr(display), window, (Atom)atom);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSendEvent
 * Signature: (JJZJJ)V
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XSendEvent
(JNIEnv *env, jclass clazz, jlong display, jlong window, jboolean propagate, jlong event_mask, jlong event)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XSendEvent((Display*) jlong_to_ptr(display),
                      window,
                      propagate==JNI_TRUE?True:False,
                      (long) event_mask,
                      (XEvent*) jlong_to_ptr(event));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XQueryTree
 * Signature: (JJJJJJ)I
 */
JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XQueryTree
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong root_return, jlong parent_return, jlong children_return, jlong nchildren_return)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XQueryTree((Display*) jlong_to_ptr(display),
                      window,
                      (Window *) jlong_to_ptr(root_return),
                      (Window*) jlong_to_ptr(parent_return),
                      (Window**) jlong_to_ptr(children_return),
                      (unsigned int*) jlong_to_ptr(nchildren_return));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    memcpy
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_memcpy
(JNIEnv *env, jclass clazz, jlong dest_ptr, jlong src_ptr, jlong length)
{
    memcpy(jlong_to_ptr(dest_ptr), jlong_to_ptr(src_ptr), length);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XSetMinMaxHints
(JNIEnv *env, jclass clazz, jlong display, jlong window, jint x, jint y, jint width, jint height, jlong flags) {
    XSizeHints * hints;
    AWT_CHECK_HAVE_LOCK();
    hints = XAllocSizeHints();
    hints->flags = flags;
    hints->width = width;
    hints->min_width = width;
    hints->max_width = width;
    hints->height = height;
    hints->min_height = height;
    hints->max_height = height;
    hints->x = x;
    hints->y = y;
    XSetWMNormalHints((Display*) jlong_to_ptr(display), window, hints);
    XFree(hints);
}

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XGetVisualInfo
(JNIEnv *env, jclass clazz, jlong display, jlong vinfo_mask, jlong vinfo_template,
 jlong nitems_return)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XGetVisualInfo((Display*) jlong_to_ptr(display),
                                       (long) vinfo_mask,
                                       (XVisualInfo*) jlong_to_ptr(vinfo_template),
                                       (int*) jlong_to_ptr(nitems_return)));
}

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XAllocSizeHints
  (JNIEnv *env, jclass clazz)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XAllocSizeHints());
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XIconifyWindow
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XBell
(JNIEnv *env, jclass clazz, jlong display, jint percent)
{
    AWT_CHECK_HAVE_LOCK();
    XBell((Display*)jlong_to_ptr(display), percent);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XAllocColor
 * Signature: (JJJ)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XAllocColor
(JNIEnv *env, jclass clazz, jlong display , jlong colormap, jlong xcolor) {

    Status status;
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    status = XAllocColor((Display *) jlong_to_ptr(display), (Colormap) colormap, (XColor *) jlong_to_ptr(xcolor));

    if (status == 0) return JNI_FALSE;
    else return JNI_TRUE;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XCreateBitmapFromData
 * Signature: (JJJII)J
 */
JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XCreateBitmapFromData
(JNIEnv *env, jclass clazz, jlong display, jlong drawable, jlong data, jint width, jint height) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);

    return (jlong) XCreateBitmapFromData((Display *) jlong_to_ptr(display), (Drawable) drawable,
                                         (char *) jlong_to_ptr(data), width, height);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XFreePixmap
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XFreePixmap
(JNIEnv *env, jclass clazz, jlong display, jlong pixmap) {
    AWT_CHECK_HAVE_LOCK();
    XFreePixmap((Display *)jlong_to_ptr(display), (Pixmap) pixmap);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XReparentWindow
 * Signature: (JJJII)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XReparentWindow
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong parent, jint x, jint y) {
    AWT_CHECK_HAVE_LOCK();
    XReparentWindow((Display*)jlong_to_ptr(display), window, parent, x, y);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XConvertSelection
 * Signature: (JJJJJJ)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XConvertSelection(JNIEnv *env, jclass clazz,
                           jlong display, jlong selection,
                           jlong target, jlong property,
                           jlong requestor, jlong time) {
    AWT_CHECK_HAVE_LOCK();
    XConvertSelection((Display*)jlong_to_ptr(display), selection, target, property, requestor,
              time);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XSetSelectionOwner
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XSetSelectionOwner(JNIEnv *env, jclass clazz,
                        jlong display, jlong selection,
                        jlong owner, jlong time) {
    AWT_CHECK_HAVE_LOCK();
    XSetSelectionOwner((Display*)jlong_to_ptr(display), selection, owner, time);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetSelectionOwner
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XGetSelectionOwner(JNIEnv *env, jclass clazz,
                        jlong display, jlong selection) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return (jlong)XGetSelectionOwner((Display*)jlong_to_ptr(display), selection);
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XGetAtomName
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_sun_awt_X11_XlibWrapper_XGetAtomName(JNIEnv *env, jclass clazz,
                      jlong display, jlong atom)
{
    jstring string = NULL;
    char* name;
    AWT_CHECK_HAVE_LOCK_RETURN(NULL);
    name = (char*) XGetAtomName((Display*)jlong_to_ptr(display), atom);

    if (name == NULL) {
        fprintf(stderr, "Atom was %d\n", (int)atom);
        JNU_ThrowNullPointerException(env, "Failed to retrieve atom name.");
        return NULL;
    }

    string = (*env)->NewStringUTF(env, (const char *)name);

    XFree(name);

    return string;
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XMaxRequestSize
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XMaxRequestSize(JNIEnv *env, jclass clazz,
                         jlong display) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XMaxRequestSize((Display*) jlong_to_ptr(display));
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XAllocWMHints(JNIEnv *env, jclass clazz)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XAllocWMHints());
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XCreatePixmap(JNIEnv *env, jclass clazz, jlong display, jlong drawable, jint width, jint height, jint depth)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XCreatePixmap((Display*)jlong_to_ptr(display), (Drawable)drawable, width, height, depth);
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XCreateImage
  (JNIEnv *env, jclass clazz, jlong display, jlong visual_ptr,
   jint depth, jint format, jint offset, jlong data, jint width,
   jint height, jint bitmap_pad, jint bytes_per_line)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XCreateImage((Display*) jlong_to_ptr(display), (Visual*) jlong_to_ptr(visual_ptr),
                depth, format, offset, (char*) jlong_to_ptr(data),
                width, height, bitmap_pad, bytes_per_line));
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XCreateGC
  (JNIEnv *env, jclass clazz, jlong display, jlong drawable,
   jlong valuemask, jlong values)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XCreateGC((Display*) jlong_to_ptr(display), (Drawable)drawable, valuemask, (XGCValues*) jlong_to_ptr(values)));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XDestroyImage(JNIEnv *env, jclass clazz, jlong image)
{
    XImage *img = (XImage*) jlong_to_ptr(image);
    AWT_CHECK_HAVE_LOCK();

    // Fix for bug 4903671 :
    // We should be careful to not double free the memory pointed to data
    // Since we use unsafe to allocate it, we should use unsafe to free it.
    // So we should NULL the data pointer before calling XDestroyImage so
    // that X does not free the pointer for us.
    img->data = NULL;
    XDestroyImage(img);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XPutImage(JNIEnv *env, jclass clazz, jlong display, jlong drawable, jlong gc, jlong image, jint src_x, jint src_y, jint dest_x, jint dest_y, jint width, jint height)
{
    AWT_CHECK_HAVE_LOCK();
    XPutImage((Display*)jlong_to_ptr(display), (Drawable)drawable, (GC) jlong_to_ptr(gc), (XImage*) jlong_to_ptr(image), src_x, src_y,
              dest_x, dest_y, width, height);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XFreeGC(JNIEnv *env, jclass clazz, jlong display, jlong gc)
{
    AWT_CHECK_HAVE_LOCK();
    XFreeGC((Display*) jlong_to_ptr(display), (GC) jlong_to_ptr(gc));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XSetWindowBackgroundPixmap(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong pixmap)
{
    AWT_CHECK_HAVE_LOCK();
    XSetWindowBackgroundPixmap((Display*) jlong_to_ptr(display), (Window)window, (Pixmap)pixmap);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XClearWindow(JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    AWT_CHECK_HAVE_LOCK();
    XClearWindow((Display*) jlong_to_ptr(display), (Window)window);
}

JNIEXPORT jint JNICALL
Java_sun_awt_X11_XlibWrapper_XGetIconSizes(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong ret_sizes, jlong ret_count)
{
    XIconSize** psize = (XIconSize**) jlong_to_ptr(ret_sizes);
    int * pcount = (int *) jlong_to_ptr(ret_count);
    Status res;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    res = XGetIconSizes((Display*) jlong_to_ptr(display), (Window)window, psize, pcount);
    return res;
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XdbeQueryExtension
  (JNIEnv *env, jclass clazz, jlong display, jlong major_version_return,
   jlong minor_version_return)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeQueryExtension((Display*) jlong_to_ptr(display), (int *) jlong_to_ptr(major_version_return),
                  (int *) jlong_to_ptr(minor_version_return));
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_XQueryExtension
  (JNIEnv *env, jclass clazz, jlong display, jstring jstr, jlong mop_return,
   jlong feve_return, jlong err_return)
{
    char *cname;
    Boolean bu;
    if (!JNU_IsNull(env, jstr)) {
        cname = (char *)JNU_GetStringPlatformChars(env, jstr, NULL);
        CHECK_NULL_RETURN(cname, JNI_FALSE);
    } else {
        cname = "";
    }

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    bu = XQueryExtension((Display*) jlong_to_ptr(display), cname, (int *) jlong_to_ptr(mop_return),
                (int *) jlong_to_ptr(feve_return),  (int *) jlong_to_ptr(err_return));
    if (!JNU_IsNull(env, jstr)) {
        JNU_ReleaseStringPlatformChars(env, jstr, (const char *) cname);
    }
    return bu ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XlibWrapper_IsKeypadKey
  (JNIEnv *env, jclass clazz, jlong keysym)
{
    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    if(IsKeypadKey(keysym)) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_sun_awt_X11_XlibWrapper_XdbeAllocateBackBufferName
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jint swap_action)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeAllocateBackBufferName((Display*) jlong_to_ptr(display), (Window) window,
                      (XdbeSwapAction) swap_action);
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XdbeDeallocateBackBufferName
  (JNIEnv *env, jclass clazz, jlong display, jlong buffer)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeDeallocateBackBufferName((Display*) jlong_to_ptr(display), (XdbeBackBuffer) buffer);
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XdbeBeginIdiom
  (JNIEnv *env, jclass clazz, jlong display)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeBeginIdiom((Display*) jlong_to_ptr(display));
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XdbeEndIdiom
  (JNIEnv *env, jclass clazz, jlong display)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeEndIdiom((Display*) jlong_to_ptr(display));
}

JNIEXPORT jint JNICALL Java_sun_awt_X11_XlibWrapper_XdbeSwapBuffers
  (JNIEnv *env, jclass clazz, jlong display, jlong swap_info, jint num_windows)
{
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XdbeSwapBuffers((Display*) jlong_to_ptr(display), (XdbeSwapInfo *) jlong_to_ptr(swap_info), num_windows);
}
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XQueryKeymap
(JNIEnv *env, jclass clazz, jlong display, jlong vector)
{
    AWT_CHECK_HAVE_LOCK();
    XQueryKeymap( (Display *) jlong_to_ptr(display), (char *) jlong_to_ptr(vector));
}

// XKeycodeToKeysym is deprecated but for compatibility we keep the API.
JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XKeycodeToKeysym(JNIEnv *env, jclass clazz,
                                              jlong display, jint keycode,
                                              jint index) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return keycodeToKeysym((Display*)jlong_to_ptr(display), (unsigned int)keycode, (int)index);
}

JNIEXPORT jint JNICALL
Java_sun_awt_X11_XlibWrapper_XkbGetEffectiveGroup(JNIEnv *env, jclass clazz,
                                              jlong display) {
    XkbStateRec sr;
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    memset(&sr, 0, sizeof(XkbStateRec));
    XkbGetState((Display*) jlong_to_ptr(display), XkbUseCoreKbd, &sr);
//    printf("-------------------------------------VVVV\n");
//    printf("                 group:0x%0X\n",sr.group);
//    printf("            base_group:0x%0X\n",sr.base_group);
//    printf("         latched_group:0x%0X\n",sr.latched_group);
//    printf("          locked_group:0x%0X\n",sr.locked_group);
//    printf("                  mods:0x%0X\n",sr.mods);
//    printf("             base_mods:0x%0X\n",sr.base_mods);
//    printf("          latched_mods:0x%0X\n",sr.latched_mods);
//    printf("           locked_mods:0x%0X\n",sr.locked_mods);
//    printf("          compat_state:0x%0X\n",sr.compat_state);
//    printf("             grab_mods:0x%0X\n",sr.grab_mods);
//    printf("      compat_grab_mods:0x%0X\n",sr.compat_grab_mods);
//    printf("           lookup_mods:0x%0X\n",sr.lookup_mods);
//    printf("    compat_lookup_mods:0x%0X\n",sr.compat_lookup_mods);
//    printf("           ptr_buttons:0x%0X\n",sr.ptr_buttons);
//    printf("-------------------------------------^^^^\n");
    return (jint)(sr.group);
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XkbKeycodeToKeysym(JNIEnv *env, jclass clazz,
                                              jlong display, jint keycode,
                                              jint group, jint level) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XkbKeycodeToKeysym((Display*) jlong_to_ptr(display), (unsigned int)keycode, (unsigned int)group, (unsigned int)level);
}

JNIEXPORT jint JNICALL
Java_sun_awt_X11_XlibWrapper_XKeysymToKeycode(JNIEnv *env, jclass clazz,
                                              jlong display, jlong keysym) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return XKeysymToKeycode((Display*) jlong_to_ptr(display), (KeySym)keysym);
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_XGetModifierMapping(JNIEnv *env, jclass clazz,
                                              jlong display) {
    AWT_CHECK_HAVE_LOCK_RETURN(0);
    return ptr_to_jlong(XGetModifierMapping((Display*) jlong_to_ptr(display)));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XFreeModifiermap(JNIEnv *env, jclass clazz,
                                              jlong keymap) {
    AWT_CHECK_HAVE_LOCK();
    XFreeModifiermap((XModifierKeymap*) jlong_to_ptr(keymap));
}

/*
 * Class:     sun_awt_X11_XlibWrapper
 * Method:    XRefreshKeyboardMapping
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XlibWrapper_XRefreshKeyboardMapping
(JNIEnv *env, jclass clazz, jlong event_ptr)
{
    AWT_CHECK_HAVE_LOCK();
    XRefreshKeyboardMapping((XMappingEvent*) jlong_to_ptr(event_ptr));
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XChangeActivePointerGrab(JNIEnv *env, jclass clazz,
                                                      jlong display, jint mask,
                                                      jlong cursor, jlong time) {
    AWT_CHECK_HAVE_LOCK();
    XChangeActivePointerGrab((Display*)jlong_to_ptr(display), (unsigned int)mask,
                             (Cursor)cursor, (Time)time);
}

/******************* Secondary loop support ************************************/
#define AWT_SECONDARY_LOOP_TIMEOUT 250

static Bool exitSecondaryLoop = True;

/*
 * This predicate procedure allows the Toolkit thread to process specific events
 * while it is blocked waiting for the event dispatch thread to process
 * a SunDropTargetEvent. We need this to prevent deadlock when the client code
 * processing SunDropTargetEvent sets or gets the contents of the system
 * clipboard/selection. In this case the event dispatch thread waits for the
 * Toolkit thread to process PropertyNotify or SelectionNotify events.
 */
static Bool
secondary_loop_event(Display* dpy, XEvent* event, XPointer xawt_root_window) {
    return (
                event->type == SelectionNotify ||
                event->type == SelectionClear  ||
                event->type == PropertyNotify  ||
                (event->type == ConfigureNotify
                    && event->xany.window == *(Window*) xawt_root_window)
            ) ? True : False;
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_X11_XlibWrapper_XNextSecondaryLoopEvent(JNIEnv *env, jclass clazz,
                                                     jlong display, jlong ptr) {
    uint32_t timeout = 1;

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);
    exitSecondaryLoop = False;
    Window xawt_root_window = get_xawt_root_shell(env);

    while (!exitSecondaryLoop) {
        if (XCheckIfEvent((Display*) jlong_to_ptr(display),
                (XEvent*) jlong_to_ptr(ptr), secondary_loop_event, (XPointer) &xawt_root_window)) {
            return JNI_TRUE;
        }
        timeout = (timeout < AWT_SECONDARY_LOOP_TIMEOUT) ? (timeout << 1) : AWT_SECONDARY_LOOP_TIMEOUT;
        AWT_WAIT(timeout);
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_ExitSecondaryLoop(JNIEnv *env, jclass clazz) {
    DASSERT(!exitSecondaryLoop);
    AWT_CHECK_HAVE_LOCK();
    exitSecondaryLoop = True;
    AWT_NOTIFY_ALL();
}

JNIEXPORT jobjectArray JNICALL
Java_sun_awt_X11_XlibWrapper_XTextPropertyToStringList(JNIEnv *env,
                                                       jclass clazz,
                                                       jbyteArray bytes,
                                                       jlong encodingAtom) {
    XTextProperty tp;
    jbyte         *value;

    char**        strings  = (char **)NULL;
    int32_t       nstrings = 0;
    jobjectArray  ret = NULL;
    int32_t       i;
    jsize         len;
    jboolean      isCopy = JNI_FALSE;
    static jclass stringClass = NULL;
    jclass        stringClassLocal = NULL;

    AWT_CHECK_HAVE_LOCK_RETURN(NULL);

    if (JNU_IsNull(env, stringClass)) {
        stringClassLocal = (*env)->FindClass(env, "java/lang/String");

        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            DASSERT(False);
        }

        if (JNU_IsNull(env, stringClassLocal)) {
            return NULL;
        }

        stringClass = (*env)->NewGlobalRef(env, stringClassLocal); /* never freed! */
        (*env)->DeleteLocalRef(env, stringClassLocal);

        if (JNU_IsNull(env, stringClass)) {
            JNU_ThrowOutOfMemoryError(env, "");
            return NULL;
        }
    }

    /*
     * If the length of the byte array is 0 just return a null
     */
    len = (*env)->GetArrayLength(env, bytes);
    if (len == 0) {
        return (*env)->NewObjectArray(env, 0, stringClass, NULL);
    }

    value = (*env)->GetByteArrayElements(env, bytes, &isCopy);
    if (JNU_IsNull(env, value)) {
        return NULL;
    }

    tp.encoding = encodingAtom;
    tp.value    = (unsigned char *)value;
    tp.nitems   = len;
    tp.format   = 8;

    /*
     * Convert the byte stream into a list of X11 strings
     */
    if (XTextPropertyToStringList(&tp, &strings, &nstrings) == 0) {
        (*env)->ReleaseByteArrayElements(env, bytes, value, JNI_ABORT);
        return NULL;
    }

    (*env)->ReleaseByteArrayElements(env, bytes, value, JNI_ABORT);

    if (nstrings == 0) {
        return (*env)->NewObjectArray(env, 0, stringClass, NULL);
    }

    ret = (*env)->NewObjectArray(env, nstrings, stringClass, NULL);

    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        goto wayout;
    }

    if (JNU_IsNull(env, ret)) {
        goto wayout;
    }

    for (i = 0; i < nstrings; i++) {
        jstring string = (*env)->NewStringUTF(env,
                                              (const char *)strings[i]);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            goto wayout;
        }

        if (JNU_IsNull(env, string)) {
            goto wayout;
        }

        (*env)->SetObjectArrayElement(env, ret, i, string);

        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
            goto wayout;
        }

        (*env)->DeleteLocalRef(env, string);
    }

 wayout:
    /*
     * Clean up and return
     */
    XFreeStringList(strings);
    return ret;
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_XPutBackEvent(JNIEnv *env,
                                           jclass clazz,
                                           jlong display,
                                           jlong event) {
    XPutBackEvent((Display*)jlong_to_ptr(display), (XEvent*) jlong_to_ptr(event));
}

JNIEXPORT jlong JNICALL
Java_sun_awt_X11_XlibWrapper_getAddress(JNIEnv *env,
                                           jclass clazz,
                                           jobject o) {
    return ptr_to_jlong(o);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_copyIntArray(JNIEnv *env,
                                           jclass clazz,
                                           jlong dest, jobject array, jint size) {
    jboolean isCopy = JNI_FALSE;
    jint * ints = (*env)->GetIntArrayElements(env, array, &isCopy);
    memcpy(jlong_to_ptr(dest), ints, size);
    if (isCopy) {
        (*env)->ReleaseIntArrayElements(env, array, ints, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_copyLongArray(JNIEnv *env,
                                           jclass clazz,
                                           jlong dest, jobject array, jint size) {
    jboolean isCopy = JNI_FALSE;
    jlong * longs = (*env)->GetLongArrayElements(env, array, &isCopy);
    memcpy(jlong_to_ptr(dest), longs, size);
    if (isCopy) {
        (*env)->ReleaseLongArrayElements(env, array, longs, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL
Java_sun_awt_X11_XlibWrapper_XSynchronize(JNIEnv *env, jclass clazz, jlong display, jboolean onoff)
{
    return (jint) XSynchronize((Display*)jlong_to_ptr(display), (onoff == JNI_TRUE ? True : False));
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_X11_XlibWrapper_XShapeQueryExtension
(JNIEnv *env, jclass clazz, jlong display, jlong event_base_return, jlong error_base_return)
{
    Bool status;

    AWT_CHECK_HAVE_LOCK_RETURN(JNI_FALSE);

    status = XShapeQueryExtension((Display *)jlong_to_ptr(display),
            (int *)jlong_to_ptr(event_base_return), (int *)jlong_to_ptr(error_base_return));
    return status ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     XlibWrapper
 * Method:    SetRectangularShape
 */

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_SetRectangularShape
(JNIEnv *env, jclass clazz, jlong display, jlong window,
 jint x1, jint y1, jint x2, jint y2,
 jobject region)
{
    AWT_CHECK_HAVE_LOCK();

    // If all the params are zeros, the shape must be simply reset.
    // Otherwise, the shape may be not rectangular.
    if (region || x1 || x2 || y1 || y2) {
        XRectangle rects[256];
        XRectangle *pRect = rects;

        int numrects = RegionToYXBandedRectangles(env, x1, y1, x2, y2, region,
                &pRect, 256);

        XShapeCombineRectangles((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
                ShapeClip, 0, 0, pRect, numrects, ShapeSet, YXBanded);
        XShapeCombineRectangles((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
                ShapeBounding, 0, 0, pRect, numrects, ShapeSet, YXBanded);

        if (pRect != rects) {
            free(pRect);
        }
    } else {
        // Reset the shape to a rectangular form.
        XShapeCombineMask((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
                ShapeClip, 0, 0, None, ShapeSet);
        XShapeCombineMask((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
                ShapeBounding, 0, 0, None, ShapeSet);
    }
}

/*
 * Class:     XlibWrapper
 * Method:    SetZOrder
 */

JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_SetZOrder
(JNIEnv *env, jclass clazz, jlong display, jlong window, jlong above)
{
    unsigned int value_mask = CWStackMode;

    XWindowChanges wc;
    wc.sibling = (Window)jlong_to_ptr(above);

    AWT_CHECK_HAVE_LOCK();

    if (above == 0) {
        wc.stack_mode = Above;
    } else {
        wc.stack_mode = Below;
        value_mask |= CWSibling;
    }

    XConfigureWindow((Display *)jlong_to_ptr(display),
                     (Window)jlong_to_ptr(window),
                     value_mask, &wc );
}

/*
 * Class:     XlibWrapper
 * Method:    SetBitmapShape
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11_XlibWrapper_SetBitmapShape
(JNIEnv *env, jclass clazz, jlong display, jlong window,
 jint width, jint height, jintArray bitmap)
{
    jsize len;
    jint *values;
    jboolean isCopy = JNI_FALSE;
    size_t worstBufferSize = (size_t)((width / 2 + 1) * height);
    RECT_T * pRect;
    int numrects;

    if (!IS_SAFE_SIZE_MUL(width / 2 + 1, height)) {
        return;
    }

    AWT_CHECK_HAVE_LOCK();

    len = (*env)->GetArrayLength(env, bitmap);
    if (len == 0 || len < width * height) {
        return;
    }

    values = (*env)->GetIntArrayElements(env, bitmap, &isCopy);
    if (JNU_IsNull(env, values)) {
        return;
    }

    pRect = (RECT_T *)SAFE_SIZE_ARRAY_ALLOC(malloc, worstBufferSize, sizeof(RECT_T));
    if (!pRect) {
        return;
    }

    /* Note: the values[0] and values[1] are supposed to contain the width
     * and height (see XIconInfo.getIntData() for details). So, we do +2.
     */
    numrects = BitmapToYXBandedRectangles(32, (int)width, (int)height,
            (unsigned char *)(values + 2), pRect);

    XShapeCombineRectangles((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
            ShapeClip, 0, 0, pRect, numrects, ShapeSet, YXBanded);
    XShapeCombineRectangles((Display *)jlong_to_ptr(display), (Window)jlong_to_ptr(window),
            ShapeBounding, 0, 0, pRect, numrects, ShapeSet, YXBanded);

    free(pRect);

    (*env)->ReleaseIntArrayElements(env, bitmap, values, JNI_ABORT);
}
