/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <dlfcn.h>
#include "jvm_md.h"
#include <setjmp.h>
#include <string.h>

#include "jni_util.h"
#include "awt_Taskbar.h"


extern JavaVM *jvm;

#define NO_SYMBOL_EXCEPTION 1

#define UNITY_LIB_VERSIONED VERSIONED_JNI_LIB_NAME("unity", "9")
#define UNITY_LIB JNI_LIB_NAME("unity")

static jmp_buf j;

static void *unity_libhandle = NULL;

static DbusmenuMenuitem* menu = NULL;
UnityLauncherEntry* entry = NULL;

static jclass jTaskbarCls = NULL;
static jmethodID jTaskbarCallback = NULL;
static jmethodID jMenuItemGetLabel = NULL;

GList* globalRefs = NULL;

static void* dl_symbol(const char* name) {
    void* result = dlsym(unity_libhandle, name);
    if (!result)
        longjmp(j, NO_SYMBOL_EXCEPTION);

    return result;
}

static gboolean unity_load() {
    unity_libhandle = dlopen(UNITY_LIB_VERSIONED, RTLD_LAZY | RTLD_LOCAL);
    if (unity_libhandle == NULL) {
        unity_libhandle = dlopen(UNITY_LIB, RTLD_LAZY | RTLD_LOCAL);
        if (unity_libhandle == NULL) {
            return FALSE;
        }
    }
    if (setjmp(j) == 0) {
        fp_unity_launcher_entry_get_for_desktop_file = dl_symbol("unity_launcher_entry_get_for_desktop_file");
        fp_unity_launcher_entry_set_count = dl_symbol("unity_launcher_entry_set_count");
        fp_unity_launcher_entry_set_count_visible = dl_symbol("unity_launcher_entry_set_count_visible");
        fp_unity_launcher_entry_set_urgent = dl_symbol("unity_launcher_entry_set_urgent");
        fp_unity_launcher_entry_set_progress = dl_symbol("unity_launcher_entry_set_progress");
        fp_unity_launcher_entry_set_progress_visible = dl_symbol("unity_launcher_entry_set_progress_visible");

        fp_dbusmenu_menuitem_new = dl_symbol("dbusmenu_menuitem_new");
        fp_dbusmenu_menuitem_property_set = dl_symbol("dbusmenu_menuitem_property_set");
        fp_dbusmenu_menuitem_property_set_int = dl_symbol("dbusmenu_menuitem_property_set_int");
        fp_dbusmenu_menuitem_property_get_int = dl_symbol("dbusmenu_menuitem_property_get_int");
        fp_dbusmenu_menuitem_property_set = dl_symbol("dbusmenu_menuitem_property_set");
        fp_dbusmenu_menuitem_child_append = dl_symbol("dbusmenu_menuitem_child_append");
        fp_dbusmenu_menuitem_child_delete = dl_symbol("dbusmenu_menuitem_child_delete");
        fp_dbusmenu_menuitem_take_children = dl_symbol("dbusmenu_menuitem_take_children");
        fp_dbusmenu_menuitem_foreach = dl_symbol("dbusmenu_menuitem_foreach");
        fp_unity_launcher_entry_set_quicklist = dl_symbol("unity_launcher_entry_set_quicklist");
        fp_unity_launcher_entry_get_quicklist = dl_symbol("unity_launcher_entry_get_quicklist");
    } else {
        dlclose(unity_libhandle);
        unity_libhandle = NULL;
        return FALSE;
    }
    return TRUE;
}

void callback(DbusmenuMenuitem* mi, guint ts, jobject data) {
    JNIEnv* env = (JNIEnv*) JNU_GetEnv(jvm, JNI_VERSION_1_2);
    (*env)->CallStaticVoidMethod(env, jTaskbarCls, jTaskbarCallback, data);
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    init
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XTaskbarPeer_init
(JNIEnv *env, jclass cls, jstring jname, jint version, jboolean verbose) {
    jclass clazz;

    jTaskbarCls = (*env)->NewGlobalRef(env, cls);

    CHECK_NULL_RETURN(jTaskbarCallback =
            (*env)->GetStaticMethodID(env, cls, "menuItemCallback", "(Ljava/awt/MenuItem;)V"), JNI_FALSE);
    CHECK_NULL_RETURN(
            clazz = (*env)->FindClass(env, "java/awt/MenuItem"), JNI_FALSE);
    CHECK_NULL_RETURN(
            jMenuItemGetLabel = (*env)->GetMethodID(env, clazz, "getLabel", "()Ljava/lang/String;"), JNI_FALSE);

    if (gtk_load(env, version, verbose) && unity_load()) {
        const gchar* name = (*env)->GetStringUTFChars(env, jname, NULL);
        if (name) {
            entry = fp_unity_launcher_entry_get_for_desktop_file(name);
            (*env)->ReleaseStringUTFChars(env, jname, name);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    runloop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XTaskbarPeer_runloop
(JNIEnv *env, jclass cls) {
    gtk->gdk_threads_enter();
    gtk->gtk_main();
    gtk->gdk_threads_leave();
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    setBadge
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XTaskbarPeer_setBadge
(JNIEnv *env, jobject obj, jlong value, jboolean visible) {
    gtk->gdk_threads_enter();
    fp_unity_launcher_entry_set_count(entry, value);
    fp_unity_launcher_entry_set_count_visible(entry, visible);
    DbusmenuMenuitem* m;
    if (m = fp_unity_launcher_entry_get_quicklist(entry)) {
        fp_unity_launcher_entry_set_quicklist(entry, m);
    }
    gtk->gdk_threads_leave();
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    setUrgent
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XTaskbarPeer_setUrgent
(JNIEnv *env, jobject obj, jboolean urgent) {
    gtk->gdk_threads_enter();
    fp_unity_launcher_entry_set_urgent(entry, urgent);
    DbusmenuMenuitem* m;
    if (m = fp_unity_launcher_entry_get_quicklist(entry)) {
        fp_unity_launcher_entry_set_quicklist(entry, m);
    }
    gtk->gdk_threads_leave();
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    updateProgress
 * Signature: (DZ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XTaskbarPeer_updateProgress
(JNIEnv *env, jobject obj, jdouble value, jboolean visible) {
    gtk->gdk_threads_enter();
    fp_unity_launcher_entry_set_progress(entry, value);
    fp_unity_launcher_entry_set_progress_visible(entry, visible);
    DbusmenuMenuitem* m;
    if (m = fp_unity_launcher_entry_get_quicklist(entry)) {
        fp_unity_launcher_entry_set_quicklist(entry, m);
    }
    gtk->gdk_threads_leave();
}

void deleteGlobalRef(gpointer data) {
    JNIEnv* env = (JNIEnv*) JNU_GetEnv(jvm, JNI_VERSION_1_2);
    (*env)->DeleteGlobalRef(env, data);
}

void fill_menu(JNIEnv *env, jobjectArray items) {
    int index;
    jsize length = (*env)->GetArrayLength(env, items);
    for (index = 0; index < length; index++) {
        jobject elem = (*env)->GetObjectArrayElement(env, items, index);
        if ((*env)->ExceptionCheck(env)) {
            break;
        }
        elem = (*env)->NewGlobalRef(env, elem);

        globalRefs = gtk->g_list_append(globalRefs, elem);

        jstring jlabel = (jstring) (*env)->CallObjectMethod(env, elem, jMenuItemGetLabel);
        if (!(*env)->ExceptionCheck(env) && jlabel) {
            const gchar* label = (*env)->GetStringUTFChars(env, jlabel, NULL);
            if (label) {
                DbusmenuMenuitem* mi = fp_dbusmenu_menuitem_new();
                if (!strcmp(label, "-")) {
                    fp_dbusmenu_menuitem_property_set(mi, "type", "separator");
                } else {
                    fp_dbusmenu_menuitem_property_set(mi, "label", label);
                }

                (*env)->ReleaseStringUTFChars(env, jlabel, label);
                fp_dbusmenu_menuitem_child_append(menu, mi);
                gtk->g_signal_connect_data(mi, "item_activated",
                                           G_CALLBACK(callback), elem, NULL, 0);
            }
        }
    }
}

/*
 * Class:     sun_awt_X11_XTaskbarPeer
 * Method:    setNativeMenu
 * Signature: ([Ljava/awt/MenuItem;)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11_XTaskbarPeer_setNativeMenu
(JNIEnv *env, jobject obj, jobjectArray items) {

    gtk->gdk_threads_enter();

    if (!menu) {
        menu = fp_dbusmenu_menuitem_new();
        fp_unity_launcher_entry_set_quicklist(entry, menu);
    }

    GList* list = fp_dbusmenu_menuitem_take_children(menu);
    gtk->g_list_free_full(list, gtk->g_object_unref);

    gtk->g_list_free_full(globalRefs, deleteGlobalRef);
    globalRefs = NULL;

    if (items) {
        fill_menu(env, items);
    }

    gtk->gdk_threads_leave();
}
