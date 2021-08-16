/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <setjmp.h>
#include <X11/Xlib.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include "gtk2_interface.h"
#include "java_awt_Transparency.h"
#include "jvm_md.h"
#include "sizecalc.h"
#include <jni_util.h>
#include "awt.h"

#define GTK_TYPE_BORDER                 ((*fp_gtk_border_get_type)())

#define G_TYPE_FUNDAMENTAL_SHIFT        (2)
#define G_TYPE_MAKE_FUNDAMENTAL(x)      ((GType) ((x) << G_TYPE_FUNDAMENTAL_SHIFT))

#define CONV_BUFFER_SIZE 128

#define NO_SYMBOL_EXCEPTION 1

static void *gtk2_libhandle = NULL;
static void *gthread_libhandle = NULL;

static jmp_buf j;

/* Widgets */
static GtkWidget *gtk2_widget = NULL;
static GtkWidget *gtk2_window = NULL;
static GtkFixed  *gtk2_fixed  = NULL;

/* Paint system */
static GdkPixmap *gtk2_white_pixmap = NULL;
static GdkPixmap *gtk2_black_pixmap = NULL;
static GdkPixbuf *gtk2_white_pixbuf = NULL;
static GdkPixbuf *gtk2_black_pixbuf = NULL;
static int gtk2_pixbuf_width = 0;
static int gtk2_pixbuf_height = 0;

/* Static buffer for conversion from java.lang.String to UTF-8 */
static char convertionBuffer[CONV_BUFFER_SIZE];

static gboolean new_combo = TRUE;
const char ENV_PREFIX[] = "GTK_MODULES=";


static GtkWidget *gtk2_widgets[_GTK_WIDGET_TYPE_SIZE];

/*************************
 * Glib function pointers
 *************************/

static gboolean (*fp_g_main_context_iteration)(GMainContext *context,
                                             gboolean may_block);

static GValue*      (*fp_g_value_init)(GValue *value, GType g_type);
static gboolean     (*fp_g_type_is_a)(GType type, GType is_a_type);
static gboolean     (*fp_g_value_get_boolean)(const GValue *value);
static gchar        (*fp_g_value_get_char)(const GValue *value);
static guchar       (*fp_g_value_get_uchar)(const GValue *value);
static gint         (*fp_g_value_get_int)(const GValue *value);
static guint        (*fp_g_value_get_uint)(const GValue *value);
static glong        (*fp_g_value_get_long)(const GValue *value);
static gulong       (*fp_g_value_get_ulong)(const GValue *value);
static gint64       (*fp_g_value_get_int64)(const GValue *value);
static guint64      (*fp_g_value_get_uint64)(const GValue *value);
static gfloat       (*fp_g_value_get_float)(const GValue *value);
static gdouble      (*fp_g_value_get_double)(const GValue *value);
static const gchar* (*fp_g_value_get_string)(const GValue *value);
static gint         (*fp_g_value_get_enum)(const GValue *value);
static guint        (*fp_g_value_get_flags)(const GValue *value);
static GParamSpec*  (*fp_g_value_get_param)(const GValue *value);
static gpointer*    (*fp_g_value_get_boxed)(const GValue *value);
static gpointer*    (*fp_g_value_get_pointer)(const GValue *value);
static GObject*     (*fp_g_value_get_object)(const GValue *value);
static GParamSpec*  (*fp_g_param_spec_int)(const gchar *name,
        const gchar *nick, const gchar *blurb,
        gint minimum, gint maximum, gint default_value,
        GParamFlags flags);
static void         (*fp_g_object_get)(gpointer object,
                                       const gchar* fpn, ...);
static void         (*fp_g_object_set)(gpointer object,
                                       const gchar *first_property_name,
                                       ...);
/************************
 * GDK function pointers
 ************************/
static GdkPixmap *(*fp_gdk_pixmap_new)(GdkDrawable *drawable,
        gint width, gint height, gint depth);
static GdkGC *(*fp_gdk_gc_new)(GdkDrawable*);
static void (*fp_gdk_rgb_gc_set_foreground)(GdkGC*, guint32);
static void (*fp_gdk_draw_rectangle)(GdkDrawable*, GdkGC*, gboolean,
        gint, gint, gint, gint);
static GdkPixbuf *(*fp_gdk_pixbuf_new)(GdkColorspace colorspace,
        gboolean has_alpha, int bits_per_sample, int width, int height);
static void (*fp_gdk_drawable_get_size)(GdkDrawable *drawable,
        gint* width, gint* height);

/************************
 * Gtk function pointers
 ************************/
static gboolean (*fp_gtk_init_check)(int* argc, char** argv);

/* Painting */
static void (*fp_gtk_paint_hline)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x1, gint x2, gint y);
static void (*fp_gtk_paint_vline)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint y1, gint y2, gint x);
static void (*fp_gtk_paint_shadow)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_arrow)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        GtkArrowType arrow_type, gboolean fill, gint x, gint y,
        gint width, gint height);
static void (*fp_gtk_paint_diamond)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_box)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_flat_box)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_check)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_option)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_box_gap)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width);
static void (*fp_gtk_paint_extension)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side);
static void (*fp_gtk_paint_focus)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x, gint y, gint width, gint height);
static void (*fp_gtk_paint_slider)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
static void (*fp_gtk_paint_handle)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GtkShadowType shadow_type,
        GdkRectangle* area, GtkWidget* widget, const gchar* detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation);
static void (*fp_gtk_paint_expander)(GtkStyle* style, GdkWindow* window,
        GtkStateType state_type, GdkRectangle* area, GtkWidget* widget,
        const gchar* detail, gint x, gint y, GtkExpanderStyle expander_style);
static void (*fp_gtk_style_apply_default_background)(GtkStyle* style,
        GdkWindow* window, gboolean set_bg, GtkStateType state_type,
        GdkRectangle* area, gint x, gint y, gint width, gint height);

/* Widget creation */
static GtkWidget* (*fp_gtk_arrow_new)(GtkArrowType arrow_type,
                                      GtkShadowType shadow_type);
static GtkWidget* (*fp_gtk_button_new)();
static GtkWidget* (*fp_gtk_check_button_new)();
static GtkWidget* (*fp_gtk_check_menu_item_new)();
static GtkWidget* (*fp_gtk_color_selection_dialog_new)(const gchar* title);
static GtkWidget* (*fp_gtk_combo_box_new)();
static GtkWidget* (*fp_gtk_combo_box_entry_new)();
static GtkWidget* (*fp_gtk_entry_new)();
static GtkWidget* (*fp_gtk_fixed_new)();
static GtkWidget* (*fp_gtk_handle_box_new)();
static GtkWidget* (*fp_gtk_hpaned_new)();
static GtkWidget* (*fp_gtk_vpaned_new)();
static GtkWidget* (*fp_gtk_hscale_new)(GtkAdjustment* adjustment);
static GtkWidget* (*fp_gtk_vscale_new)(GtkAdjustment* adjustment);
static GtkWidget* (*fp_gtk_hscrollbar_new)(GtkAdjustment* adjustment);
static GtkWidget* (*fp_gtk_vscrollbar_new)(GtkAdjustment* adjustment);
static GtkWidget* (*fp_gtk_hseparator_new)();
static GtkWidget* (*fp_gtk_vseparator_new)();
static GtkWidget* (*fp_gtk_image_new)();
static GtkWidget* (*fp_gtk_label_new)(const gchar* str);
static GtkWidget* (*fp_gtk_menu_new)();
static GtkWidget* (*fp_gtk_menu_bar_new)();
static GtkWidget* (*fp_gtk_menu_item_new)();
static GtkWidget* (*fp_gtk_notebook_new)();
static GtkWidget* (*fp_gtk_progress_bar_new)();
static GtkWidget* (*fp_gtk_progress_bar_set_orientation)(
        GtkProgressBar *pbar,
        GtkProgressBarOrientation orientation);
static GtkWidget* (*fp_gtk_radio_button_new)(GSList *group);
static GtkWidget* (*fp_gtk_radio_menu_item_new)(GSList *group);
static GtkWidget* (*fp_gtk_scrolled_window_new)(GtkAdjustment *hadjustment,
        GtkAdjustment *vadjustment);
static GtkWidget* (*fp_gtk_separator_menu_item_new)();
static GtkWidget* (*fp_gtk_separator_tool_item_new)();
static GtkWidget* (*fp_gtk_text_view_new)();
static GtkWidget* (*fp_gtk_toggle_button_new)();
static GtkWidget* (*fp_gtk_toolbar_new)();
static GtkWidget* (*fp_gtk_tree_view_new)();
static GtkWidget* (*fp_gtk_viewport_new)(GtkAdjustment *hadjustment,
        GtkAdjustment *vadjustment);
static GtkWidget* (*fp_gtk_window_new)(GtkWindowType type);
static GtkWidget* (*fp_gtk_dialog_new)();
static GtkWidget* (*fp_gtk_spin_button_new)(GtkAdjustment *adjustment,
        gdouble climb_rate, guint digits);
static GtkWidget* (*fp_gtk_frame_new)(const gchar *label);

/* Other widget operations */
static GtkObject* (*fp_gtk_adjustment_new)(gdouble value,
        gdouble lower, gdouble upper, gdouble step_increment,
        gdouble page_increment, gdouble page_size);
static void (*fp_gtk_container_add)(GtkContainer *window, GtkWidget *widget);
static void (*fp_gtk_menu_shell_append)(GtkMenuShell *menu_shell,
        GtkWidget *child);
static void (*fp_gtk_menu_item_set_submenu)(GtkMenuItem *menu_item,
        GtkWidget *submenu);
static void (*fp_gtk_widget_realize)(GtkWidget *widget);
static GdkPixbuf* (*fp_gtk_widget_render_icon)(GtkWidget *widget,
        const gchar *stock_id, GtkIconSize size, const gchar *detail);
static void (*fp_gtk_widget_set_name)(GtkWidget *widget, const gchar *name);
static void (*fp_gtk_widget_set_parent)(GtkWidget *widget, GtkWidget *parent);
static void (*fp_gtk_widget_set_direction)(GtkWidget *widget,
        GtkTextDirection direction);
static void (*fp_gtk_widget_style_get)(GtkWidget *widget,
        const gchar *first_property_name, ...);
static void (*fp_gtk_widget_class_install_style_property)(
        GtkWidgetClass* class, GParamSpec *pspec);
static GParamSpec* (*fp_gtk_widget_class_find_style_property)(
        GtkWidgetClass* class, const gchar* property_name);
static void (*fp_gtk_widget_style_get_property)(GtkWidget* widget,
        const gchar* property_name, GValue* value);
static char* (*fp_pango_font_description_to_string)(
        const PangoFontDescription* fd);
static GtkSettings* (*fp_gtk_settings_get_default)();
static GtkSettings* (*fp_gtk_widget_get_settings)(GtkWidget *widget);
static GType        (*fp_gtk_border_get_type)();
static void (*fp_gtk_arrow_set)(GtkWidget* arrow,
                                GtkArrowType arrow_type,
                                GtkShadowType shadow_type);
static void (*fp_gtk_widget_size_request)(GtkWidget *widget,
                                          GtkRequisition *requisition);
static GtkAdjustment* (*fp_gtk_range_get_adjustment)(GtkRange* range);

/* Method bodies */

static void throw_exception(JNIEnv *env, const char* name, const char* message)
{
    jclass class = (*env)->FindClass(env, name);

    if (class != NULL)
        (*env)->ThrowNew(env, class, message);

    (*env)->DeleteLocalRef(env, class);
}

/* This is a workaround for the bug:
 * http://sourceware.org/bugzilla/show_bug.cgi?id=1814
 * (dlsym/dlopen clears dlerror state)
 * This bug is specific to Linux, but there is no harm in
 * applying this workaround on Solaris as well.
 */
static void* dl_symbol(const char* name)
{
    void* result = dlsym(gtk2_libhandle, name);
    if (!result)
        longjmp(j, NO_SYMBOL_EXCEPTION);

    return result;
}

static void* dl_symbol_gthread(const char* name)
{
    void* result = dlsym(gthread_libhandle, name);
    if (!result)
        longjmp(j, NO_SYMBOL_EXCEPTION);

    return result;
}

gboolean gtk2_check(const char* lib_name, gboolean load)
{
    if (gtk2_libhandle != NULL) {
        /* We've already successfully opened the GTK libs, so return true. */
        return TRUE;
    } else {
        void *lib = NULL;

#ifdef RTLD_NOLOAD
        /* Just check if gtk libs are already in the process space */
        lib = dlopen(lib_name, RTLD_LAZY | RTLD_NOLOAD);
        if (!load || lib != NULL) {
            return lib != NULL;
        }
#else
#ifdef _AIX
        /* On AIX we could implement this with the help of loadquery(L_GETINFO, ..)  */
        /* (see reload_table() in hotspot/src/os/aix/vm/loadlib_aix.cpp) but it is   */
        /* probably not worth it because most AIX servers don't have GTK libs anyway */
#endif
#endif

        lib = dlopen(lib_name, RTLD_LAZY | RTLD_LOCAL);
        if (lib == NULL) {
            return FALSE;
        }

        fp_gtk_check_version = dlsym(lib, "gtk_check_version");
        /* Check for GTK 2.2+ */
        if (!fp_gtk_check_version(2, 2, 0)) {
            return TRUE;
        }

        // 8048289: workaround for https://bugzilla.gnome.org/show_bug.cgi?id=733065
        // dlclose(lib);

        return FALSE;
    }
}

#define ADD_SUPPORTED_ACTION(actionStr) \
do { \
    jfieldID fld_action = (*env)->GetStaticFieldID(env, cls_action, actionStr, "Ljava/awt/Desktop$Action;"); \
    if (!(*env)->ExceptionCheck(env)) { \
        jobject action = (*env)->GetStaticObjectField(env, cls_action, fld_action); \
        (*env)->CallBooleanMethod(env, supportedActions, mid_arrayListAdd, action); \
    } else { \
        (*env)->ExceptionClear(env); \
    } \
} while(0);


static void update_supported_actions(JNIEnv *env) {
    GVfs * (*fp_g_vfs_get_default) (void);
    const gchar * const * (*fp_g_vfs_get_supported_uri_schemes) (GVfs * vfs);
    const gchar * const * schemes = NULL;

    jclass cls_action = (*env)->FindClass(env, "java/awt/Desktop$Action");
    CHECK_NULL(cls_action);
    jclass cls_xDesktopPeer = (*env)->FindClass(env, "sun/awt/X11/XDesktopPeer");
    CHECK_NULL(cls_xDesktopPeer);
    jfieldID fld_supportedActions = (*env)->GetStaticFieldID(env, cls_xDesktopPeer, "supportedActions", "Ljava/util/List;");
    CHECK_NULL(fld_supportedActions);
    jobject supportedActions = (*env)->GetStaticObjectField(env, cls_xDesktopPeer, fld_supportedActions);

    jclass cls_arrayList = (*env)->FindClass(env, "java/util/ArrayList");
    CHECK_NULL(cls_arrayList);
    jmethodID mid_arrayListAdd = (*env)->GetMethodID(env, cls_arrayList, "add", "(Ljava/lang/Object;)Z");
    CHECK_NULL(mid_arrayListAdd);
    jmethodID mid_arrayListClear = (*env)->GetMethodID(env, cls_arrayList, "clear", "()V");
    CHECK_NULL(mid_arrayListClear);

    (*env)->CallVoidMethod(env, supportedActions, mid_arrayListClear);

    ADD_SUPPORTED_ACTION("OPEN");

    /**
     * gtk_show_uri() documentation says:
     *
     * > you need to install gvfs to get support for uri schemes such as http://
     * > or ftp://, as only local files are handled by GIO itself.
     *
     * So OPEN action was safely added here.
     * However, it looks like Solaris 11 have gvfs support only for 32-bit
     * applications only by default.
     */

    fp_g_vfs_get_default = dl_symbol("g_vfs_get_default");
    fp_g_vfs_get_supported_uri_schemes = dl_symbol("g_vfs_get_supported_uri_schemes");
    dlerror();

    if (fp_g_vfs_get_default && fp_g_vfs_get_supported_uri_schemes) {
        GVfs * vfs = fp_g_vfs_get_default();
        schemes = vfs ? fp_g_vfs_get_supported_uri_schemes(vfs) : NULL;
        if (schemes) {
            int i = 0;
            while (schemes[i]) {
                if (strcmp(schemes[i], "http") == 0) {
                    ADD_SUPPORTED_ACTION("BROWSE");
                    ADD_SUPPORTED_ACTION("MAIL");
                    break;
                }
                i++;
            }
        }
    } else {
#ifdef DEBUG
        fprintf(stderr, "Cannot load g_vfs_get_supported_uri_schemes\n");
#endif /* DEBUG */
    }

}
/**
 * Functions for awt_Desktop.c
 */
static gboolean gtk2_show_uri_load(JNIEnv *env) {
     gboolean success = FALSE;
     dlerror();
     const char *gtk_version = fp_gtk_check_version(2, 14, 0);
     if (gtk_version != NULL) {
         // The gtk_show_uri is available from GTK+ 2.14
#ifdef DEBUG
         fprintf (stderr, "The version of GTK is %s. "
             "The gtk_show_uri function is supported "
             "since GTK+ 2.14.\n", gtk_version);
#endif /* DEBUG */
     } else {
         // Loading symbols only if the GTK version is 2.14 and higher
         fp_gtk_show_uri = dl_symbol("gtk_show_uri");
         const char *dlsym_error = dlerror();
         if (dlsym_error) {
#ifdef DEBUG
             fprintf (stderr, "Cannot load symbol: %s \n", dlsym_error);
#endif /* DEBUG */
         } else if (fp_gtk_show_uri == NULL) {
#ifdef DEBUG
             fprintf(stderr, "dlsym(gtk_show_uri) returned NULL\n");
#endif /* DEBUG */
        } else {
            gtk->gtk_show_uri = fp_gtk_show_uri;
            update_supported_actions(env);
            success = TRUE;
        }
     }
     return success;
}

/**
 * Functions for sun_awt_X11_GtkFileDialogPeer.c
 */
static void gtk2_file_chooser_load()
{
    fp_gtk_file_chooser_get_filename = dl_symbol(
            "gtk_file_chooser_get_filename");
    fp_gtk_file_chooser_dialog_new = dl_symbol("gtk_file_chooser_dialog_new");
    fp_gtk_file_chooser_set_current_folder = dl_symbol(
            "gtk_file_chooser_set_current_folder");
    fp_gtk_file_chooser_set_filename = dl_symbol(
            "gtk_file_chooser_set_filename");
    fp_gtk_file_chooser_set_current_name = dl_symbol(
            "gtk_file_chooser_set_current_name");
    fp_gtk_file_filter_add_custom = dl_symbol("gtk_file_filter_add_custom");
    fp_gtk_file_chooser_set_filter = dl_symbol("gtk_file_chooser_set_filter");
    fp_gtk_file_chooser_get_type = dl_symbol("gtk_file_chooser_get_type");
    fp_gtk_file_filter_new = dl_symbol("gtk_file_filter_new");
    if (fp_gtk_check_version(2, 8, 0) == NULL) {
        fp_gtk_file_chooser_set_do_overwrite_confirmation = dl_symbol(
                "gtk_file_chooser_set_do_overwrite_confirmation");
    }
    fp_gtk_file_chooser_set_select_multiple = dl_symbol(
            "gtk_file_chooser_set_select_multiple");
    fp_gtk_file_chooser_get_current_folder = dl_symbol(
            "gtk_file_chooser_get_current_folder");
    fp_gtk_file_chooser_get_filenames = dl_symbol(
            "gtk_file_chooser_get_filenames");
    fp_gtk_g_slist_length = dl_symbol("g_slist_length");
    fp_gdk_x11_drawable_get_xid = dl_symbol("gdk_x11_drawable_get_xid");
}

GtkApi* gtk2_load(JNIEnv *env, const char* lib_name)
{
    gboolean result;
    int i;
    int (*handler)();
    int (*io_handler)();
    char *gtk_modules_env;

    gtk2_libhandle = dlopen(lib_name, RTLD_LAZY | RTLD_LOCAL);
    if (gtk2_libhandle == NULL) {
        return FALSE;
    }

    gthread_libhandle = dlopen(GTHREAD_LIB_VERSIONED, RTLD_LAZY | RTLD_LOCAL);
    if (gthread_libhandle == NULL) {
        gthread_libhandle = dlopen(GTHREAD_LIB, RTLD_LAZY | RTLD_LOCAL);
        if (gthread_libhandle == NULL)
            return FALSE;
    }

    if (setjmp(j) == 0)
    {
        fp_gtk_check_version = dl_symbol("gtk_check_version");
        /* Check for GTK 2.2+ */
        if (fp_gtk_check_version(2, 2, 0)) {
            longjmp(j, NO_SYMBOL_EXCEPTION);
        }

        /* GLib */
        fp_glib_check_version = dlsym(gtk2_libhandle, "glib_check_version");
        if (!fp_glib_check_version) {
            dlerror();
        }
        fp_g_free = dl_symbol("g_free");
        fp_g_object_unref = dl_symbol("g_object_unref");

        fp_g_main_context_iteration =
            dl_symbol("g_main_context_iteration");

        fp_g_value_init = dl_symbol("g_value_init");
        fp_g_type_is_a = dl_symbol("g_type_is_a");

        fp_g_value_get_boolean = dl_symbol("g_value_get_boolean");
        fp_g_value_get_char = dl_symbol("g_value_get_char");
        fp_g_value_get_uchar = dl_symbol("g_value_get_uchar");
        fp_g_value_get_int = dl_symbol("g_value_get_int");
        fp_g_value_get_uint = dl_symbol("g_value_get_uint");
        fp_g_value_get_long = dl_symbol("g_value_get_long");
        fp_g_value_get_ulong = dl_symbol("g_value_get_ulong");
        fp_g_value_get_int64 = dl_symbol("g_value_get_int64");
        fp_g_value_get_uint64 = dl_symbol("g_value_get_uint64");
        fp_g_value_get_float = dl_symbol("g_value_get_float");
        fp_g_value_get_double = dl_symbol("g_value_get_double");
        fp_g_value_get_string = dl_symbol("g_value_get_string");
        fp_g_value_get_enum = dl_symbol("g_value_get_enum");
        fp_g_value_get_flags = dl_symbol("g_value_get_flags");
        fp_g_value_get_param = dl_symbol("g_value_get_param");
        fp_g_value_get_boxed = dl_symbol("g_value_get_boxed");
        fp_g_value_get_pointer = dl_symbol("g_value_get_pointer");
        fp_g_value_get_object = dl_symbol("g_value_get_object");
        fp_g_param_spec_int = dl_symbol("g_param_spec_int");
        fp_g_object_get = dl_symbol("g_object_get");
        fp_g_object_set = dl_symbol("g_object_set");

        /* GDK */
        fp_gdk_get_default_root_window =
            dl_symbol("gdk_get_default_root_window");
        fp_gdk_pixmap_new = dl_symbol("gdk_pixmap_new");
        fp_gdk_pixbuf_get_from_drawable =
            dl_symbol("gdk_pixbuf_get_from_drawable");
        fp_gdk_pixbuf_scale_simple =
            dl_symbol("gdk_pixbuf_scale_simple");
        fp_gdk_gc_new = dl_symbol("gdk_gc_new");
        fp_gdk_rgb_gc_set_foreground =
            dl_symbol("gdk_rgb_gc_set_foreground");
        fp_gdk_draw_rectangle = dl_symbol("gdk_draw_rectangle");
        fp_gdk_drawable_get_size = dl_symbol("gdk_drawable_get_size");

        /* Pixbuf */
        fp_gdk_pixbuf_new = dl_symbol("gdk_pixbuf_new");
        fp_gdk_pixbuf_new_from_file =
                dl_symbol("gdk_pixbuf_new_from_file");
        fp_gdk_pixbuf_get_width = dl_symbol("gdk_pixbuf_get_width");
        fp_gdk_pixbuf_get_height = dl_symbol("gdk_pixbuf_get_height");
        fp_gdk_pixbuf_get_pixels = dl_symbol("gdk_pixbuf_get_pixels");
        fp_gdk_pixbuf_get_rowstride =
                dl_symbol("gdk_pixbuf_get_rowstride");
        fp_gdk_pixbuf_get_has_alpha =
                dl_symbol("gdk_pixbuf_get_has_alpha");
        fp_gdk_pixbuf_get_bits_per_sample =
                dl_symbol("gdk_pixbuf_get_bits_per_sample");
        fp_gdk_pixbuf_get_n_channels =
                dl_symbol("gdk_pixbuf_get_n_channels");
        fp_gdk_pixbuf_get_colorspace =
                dl_symbol("gdk_pixbuf_get_colorspace");

        /* GTK painting */
        fp_gtk_init_check = dl_symbol("gtk_init_check");
        fp_gtk_paint_hline = dl_symbol("gtk_paint_hline");
        fp_gtk_paint_vline = dl_symbol("gtk_paint_vline");
        fp_gtk_paint_shadow = dl_symbol("gtk_paint_shadow");
        fp_gtk_paint_arrow = dl_symbol("gtk_paint_arrow");
        fp_gtk_paint_diamond = dl_symbol("gtk_paint_diamond");
        fp_gtk_paint_box = dl_symbol("gtk_paint_box");
        fp_gtk_paint_flat_box = dl_symbol("gtk_paint_flat_box");
        fp_gtk_paint_check = dl_symbol("gtk_paint_check");
        fp_gtk_paint_option = dl_symbol("gtk_paint_option");
        fp_gtk_paint_box_gap = dl_symbol("gtk_paint_box_gap");
        fp_gtk_paint_extension = dl_symbol("gtk_paint_extension");
        fp_gtk_paint_focus = dl_symbol("gtk_paint_focus");
        fp_gtk_paint_slider = dl_symbol("gtk_paint_slider");
        fp_gtk_paint_handle = dl_symbol("gtk_paint_handle");
        fp_gtk_paint_expander = dl_symbol("gtk_paint_expander");
        fp_gtk_style_apply_default_background =
                dl_symbol("gtk_style_apply_default_background");

        /* GTK widgets */
        fp_gtk_arrow_new = dl_symbol("gtk_arrow_new");
        fp_gtk_button_new = dl_symbol("gtk_button_new");
        fp_gtk_spin_button_new = dl_symbol("gtk_spin_button_new");
        fp_gtk_check_button_new = dl_symbol("gtk_check_button_new");
        fp_gtk_check_menu_item_new =
                dl_symbol("gtk_check_menu_item_new");
        fp_gtk_color_selection_dialog_new =
                dl_symbol("gtk_color_selection_dialog_new");
        fp_gtk_entry_new = dl_symbol("gtk_entry_new");
        fp_gtk_fixed_new = dl_symbol("gtk_fixed_new");
        fp_gtk_handle_box_new = dl_symbol("gtk_handle_box_new");
        fp_gtk_image_new = dl_symbol("gtk_image_new");
        fp_gtk_hpaned_new = dl_symbol("gtk_hpaned_new");
        fp_gtk_vpaned_new = dl_symbol("gtk_vpaned_new");
        fp_gtk_hscale_new = dl_symbol("gtk_hscale_new");
        fp_gtk_vscale_new = dl_symbol("gtk_vscale_new");
        fp_gtk_hscrollbar_new = dl_symbol("gtk_hscrollbar_new");
        fp_gtk_vscrollbar_new = dl_symbol("gtk_vscrollbar_new");
        fp_gtk_hseparator_new = dl_symbol("gtk_hseparator_new");
        fp_gtk_vseparator_new = dl_symbol("gtk_vseparator_new");
        fp_gtk_label_new = dl_symbol("gtk_label_new");
        fp_gtk_menu_new = dl_symbol("gtk_menu_new");
        fp_gtk_menu_bar_new = dl_symbol("gtk_menu_bar_new");
        fp_gtk_menu_item_new = dl_symbol("gtk_menu_item_new");
        fp_gtk_menu_item_set_submenu =
                dl_symbol("gtk_menu_item_set_submenu");
        fp_gtk_notebook_new = dl_symbol("gtk_notebook_new");
        fp_gtk_progress_bar_new =
            dl_symbol("gtk_progress_bar_new");
        fp_gtk_progress_bar_set_orientation =
            dl_symbol("gtk_progress_bar_set_orientation");
        fp_gtk_radio_button_new =
            dl_symbol("gtk_radio_button_new");
        fp_gtk_radio_menu_item_new =
            dl_symbol("gtk_radio_menu_item_new");
        fp_gtk_scrolled_window_new =
            dl_symbol("gtk_scrolled_window_new");
        fp_gtk_separator_menu_item_new =
            dl_symbol("gtk_separator_menu_item_new");
        fp_gtk_text_view_new = dl_symbol("gtk_text_view_new");
        fp_gtk_toggle_button_new =
            dl_symbol("gtk_toggle_button_new");
        fp_gtk_toolbar_new = dl_symbol("gtk_toolbar_new");
        fp_gtk_tree_view_new = dl_symbol("gtk_tree_view_new");
        fp_gtk_viewport_new = dl_symbol("gtk_viewport_new");
        fp_gtk_window_new = dl_symbol("gtk_window_new");
        fp_gtk_window_present = dl_symbol("gtk_window_present");
        fp_gtk_window_move = dl_symbol("gtk_window_move");
        fp_gtk_window_resize = dl_symbol("gtk_window_resize");

          fp_gtk_dialog_new = dl_symbol("gtk_dialog_new");
        fp_gtk_frame_new = dl_symbol("gtk_frame_new");

        fp_gtk_adjustment_new = dl_symbol("gtk_adjustment_new");
        fp_gtk_container_add = dl_symbol("gtk_container_add");
        fp_gtk_menu_shell_append =
            dl_symbol("gtk_menu_shell_append");
        fp_gtk_widget_realize = dl_symbol("gtk_widget_realize");
        fp_gtk_widget_destroy = dl_symbol("gtk_widget_destroy");
        fp_gtk_widget_render_icon =
            dl_symbol("gtk_widget_render_icon");
        fp_gtk_widget_set_name =
            dl_symbol("gtk_widget_set_name");
        fp_gtk_widget_set_parent =
            dl_symbol("gtk_widget_set_parent");
        fp_gtk_widget_set_direction =
            dl_symbol("gtk_widget_set_direction");
        fp_gtk_widget_style_get =
            dl_symbol("gtk_widget_style_get");
        fp_gtk_widget_class_install_style_property =
            dl_symbol("gtk_widget_class_install_style_property");
        fp_gtk_widget_class_find_style_property =
            dl_symbol("gtk_widget_class_find_style_property");
        fp_gtk_widget_style_get_property =
            dl_symbol("gtk_widget_style_get_property");
        fp_pango_font_description_to_string =
            dl_symbol("pango_font_description_to_string");
        fp_gtk_settings_get_default =
            dl_symbol("gtk_settings_get_default");
        fp_gtk_widget_get_settings =
            dl_symbol("gtk_widget_get_settings");
        fp_gtk_border_get_type =  dl_symbol("gtk_border_get_type");
        fp_gtk_arrow_set = dl_symbol("gtk_arrow_set");
        fp_gtk_widget_size_request =
            dl_symbol("gtk_widget_size_request");
        fp_gtk_range_get_adjustment =
            dl_symbol("gtk_range_get_adjustment");

        fp_gtk_widget_hide = dl_symbol("gtk_widget_hide");
        fp_gtk_main_quit = dl_symbol("gtk_main_quit");
        fp_g_signal_connect_data = dl_symbol("g_signal_connect_data");
        fp_gtk_widget_show = dl_symbol("gtk_widget_show");
        fp_gtk_main = dl_symbol("gtk_main");

        fp_g_path_get_dirname = dl_symbol("g_path_get_dirname");

        /**
         * GLib thread system
         */
        if (GLIB_CHECK_VERSION(2, 20, 0)) {
            fp_g_thread_get_initialized = dl_symbol_gthread("g_thread_get_initialized");
        }
        fp_g_thread_init = dl_symbol_gthread("g_thread_init");
        fp_gdk_threads_init = dl_symbol("gdk_threads_init");
        fp_gdk_threads_enter = dl_symbol("gdk_threads_enter");
        fp_gdk_threads_leave = dl_symbol("gdk_threads_leave");

        /**
         * Functions for sun_awt_X11_GtkFileDialogPeer.c
         */
        if (fp_gtk_check_version(2, 4, 0) == NULL) {
            // The current GtkFileChooser is available from GTK+ 2.4
            gtk2_file_chooser_load();
        }

        /* Some functions may be missing in pre-2.4 GTK.
           We handle them specially here.
         */
        fp_gtk_combo_box_new = dlsym(gtk2_libhandle, "gtk_combo_box_new");
        if (fp_gtk_combo_box_new == NULL) {
            fp_gtk_combo_box_new = dl_symbol("gtk_combo_new");
        }

        fp_gtk_combo_box_entry_new =
            dlsym(gtk2_libhandle, "gtk_combo_box_entry_new");
        if (fp_gtk_combo_box_entry_new == NULL) {
            fp_gtk_combo_box_entry_new = dl_symbol("gtk_combo_new");
            new_combo = FALSE;
        }

        fp_gtk_separator_tool_item_new =
            dlsym(gtk2_libhandle, "gtk_separator_tool_item_new");
        if (fp_gtk_separator_tool_item_new == NULL) {
            fp_gtk_separator_tool_item_new =
                dl_symbol("gtk_vseparator_new");
        }

        fp_g_list_append = dl_symbol("g_list_append");
        fp_g_list_free = dl_symbol("g_list_free");
        fp_g_list_free_full = dl_symbol("g_list_free_full");
    }
    /* Now we have only one kind of exceptions: NO_SYMBOL_EXCEPTION
     * Otherwise we can check the return value of setjmp method.
     */
    else
    {
        dlclose(gtk2_libhandle);
        gtk2_libhandle = NULL;

        dlclose(gthread_libhandle);
        gthread_libhandle = NULL;

        return FALSE;
    }

    /*
     * Strip the AT-SPI GTK_MODULES if present
     */
    gtk_modules_env = getenv ("GTK_MODULES");
    if ((gtk_modules_env && strstr(gtk_modules_env, "atk-bridge")) ||
        (gtk_modules_env && strstr(gtk_modules_env, "gail"))) {
        /* careful, strtok modifies its args */
        gchar *tmp_env = strdup(gtk_modules_env);
        if (tmp_env) {
            /* the new env will be smaller than the old one */
            gchar *s, *new_env = SAFE_SIZE_STRUCT_ALLOC(malloc,
                    sizeof(ENV_PREFIX), 1, strlen (gtk_modules_env));

            if (new_env) {
                strcpy(new_env, ENV_PREFIX);

                /* strip out 'atk-bridge' and 'gail' */
                size_t PREFIX_LENGTH = strlen(ENV_PREFIX);
                gchar *tmp_ptr = NULL;
                for (s = strtok_r(tmp_env, ":", &tmp_ptr); s;
                     s = strtok_r(NULL, ":", &tmp_ptr)) {
                    if ((!strstr(s, "atk-bridge")) && (!strstr(s, "gail"))) {
                        if (strlen(new_env) > PREFIX_LENGTH) {
                            new_env = strcat(new_env, ":");
                        }
                        new_env = strcat(new_env, s);
                    }
                }
                if (putenv(new_env) != 0) {
                    /* no free() on success, putenv() doesn't copy string */
                    free(new_env);
                }
            }
            free(tmp_env);
        }
    }
    /*
     * GTK should be initialized with gtk_init_check() before use.
     *
     * gtk_init_check installs its own error handlers. It is critical that
     * we preserve error handler set from AWT. Otherwise we'll crash on
     * BadMatch errors which we would normally ignore. The IO error handler
     * is preserved here, too, just for consistency.
    */
    AWT_LOCK();
    handler = XSetErrorHandler(NULL);
    io_handler = XSetIOErrorHandler(NULL);

    if (fp_gtk_check_version(2, 2, 0) == NULL) {

        // Calling g_thread_init() multiple times leads to crash on GLib < 2.24
        // We can use g_thread_get_initialized () but it is available only for
        // GLib >= 2.20.
        gboolean is_g_thread_get_initialized = FALSE;
        if (GLIB_CHECK_VERSION(2, 20, 0)) {
            is_g_thread_get_initialized = fp_g_thread_get_initialized();
        }

        if (!is_g_thread_get_initialized) {
            fp_g_thread_init(NULL);
        }

        //According the GTK documentation, gdk_threads_init() should be
        //called before gtk_init() or gtk_init_check()
        fp_gdk_threads_init();
    }
    result = (*fp_gtk_init_check)(NULL, NULL);

    XSetErrorHandler(handler);
    XSetIOErrorHandler(io_handler);
    AWT_UNLOCK();

    /* Initialize widget array. */
    for (i = 0; i < _GTK_WIDGET_TYPE_SIZE; i++)
    {
        gtk2_widgets[i] = NULL;
    }
    if (result) {
        GtkApi* gtk = (GtkApi*)malloc(sizeof(GtkApi));
        gtk2_init(gtk);
        return gtk;
    }
    return NULL;
}

int gtk2_unload()
{
    int i;
    char *gtk2_error;

    if (!gtk2_libhandle)
        return TRUE;

    /* Release painting objects */
    if (gtk2_white_pixmap != NULL) {
        (*fp_g_object_unref)(gtk2_white_pixmap);
        (*fp_g_object_unref)(gtk2_black_pixmap);
        (*fp_g_object_unref)(gtk2_white_pixbuf);
        (*fp_g_object_unref)(gtk2_black_pixbuf);
        gtk2_white_pixmap = gtk2_black_pixmap =
            gtk2_white_pixbuf = gtk2_black_pixbuf = NULL;
    }
    gtk2_pixbuf_width = 0;
    gtk2_pixbuf_height = 0;

    if (gtk2_window != NULL) {
        /* Destroying toplevel widget will destroy all contained widgets */
        (*fp_gtk_widget_destroy)(gtk2_window);

        /* Unset some static data so they get reinitialized on next load */
        gtk2_window = NULL;
    }

    dlerror();
    dlclose(gtk2_libhandle);
    dlclose(gthread_libhandle);
    if ((gtk2_error = dlerror()) != NULL)
    {
        return FALSE;
    }
    return TRUE;
}

/* Dispatch all pending events from the GTK event loop.
 * This is needed to catch theme change and update widgets' style.
 */
static void flush_gtk_event_loop()
{
    while( (*fp_g_main_context_iteration)(NULL, FALSE));
}

/*
 * Initialize components of containment hierarchy. This creates a GtkFixed
 * inside a GtkWindow. All widgets get realized.
 */
static void init_containers()
{
    if (gtk2_window == NULL)
    {
        gtk2_window = (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
        gtk2_fixed = (GtkFixed *)(*fp_gtk_fixed_new)();
        (*fp_gtk_container_add)((GtkContainer*)gtk2_window,
                                (GtkWidget *)gtk2_fixed);
        (*fp_gtk_widget_realize)(gtk2_window);
        (*fp_gtk_widget_realize)((GtkWidget *)gtk2_fixed);
    }
}

/*
 * Ensure everything is ready for drawing an element of the specified width
 * and height.
 *
 * We should somehow handle translucent images. GTK can draw to X Drawables
 * only, which don't support alpha. When we retrieve the image back from
 * the server, translucency information is lost. There're several ways to
 * work around this:
 * 1) Subclass GdkPixmap and cache translucent objects on client side. This
 * requires us to implement parts of X server drawing logic on client side.
 * Many X requests can potentially be "translucent"; e.g. XDrawLine with
 * fill=tile and a translucent tile is a "translucent" operation, whereas
 * XDrawLine with fill=solid is an "opaque" one. Moreover themes can (and some
 * do) intermix transparent and opaque operations which makes caching even
 * more problematic.
 * 2) Use Xorg 32bit ARGB visual when available. GDK has no native support
 * for it (as of version 2.6). Also even in JDS 3 Xorg does not support
 * these visuals by default, which makes optimizing for them pointless.
 * We can consider doing this at a later point when ARGB visuals become more
 * popular.
 * 3') GTK has plans to use Cairo as its graphical backend (presumably in
 * 2.8), and Cairo supports alpha. With it we could also get rid of the
 * unnecessary round trip to server and do all the drawing on client side.
 * 4) For now we draw to two different pixmaps and restore alpha channel by
 * comparing results. This can be optimized by using subclassed pixmap and
 * doing the second drawing only if necessary.
*/
static void gtk2_init_painting(JNIEnv *env, gint width, gint height)
{
    GdkGC *gc;
    GdkPixbuf *white, *black;

    init_containers();

    if (gtk2_pixbuf_width < width || gtk2_pixbuf_height < height)
    {
        white = (*fp_gdk_pixbuf_new)(GDK_COLORSPACE_RGB, TRUE, 8, width, height);
        black = (*fp_gdk_pixbuf_new)(GDK_COLORSPACE_RGB, TRUE, 8, width, height);

        if (white == NULL || black == NULL)
        {
            snprintf(convertionBuffer, CONV_BUFFER_SIZE, "Couldn't create pixbuf of size %dx%d", width, height);
            throw_exception(env, "java/lang/RuntimeException", convertionBuffer);
            fp_gdk_threads_leave();
            return;
        }

        if (gtk2_white_pixmap != NULL) {
            /* free old stuff */
            (*fp_g_object_unref)(gtk2_white_pixmap);
            (*fp_g_object_unref)(gtk2_black_pixmap);
            (*fp_g_object_unref)(gtk2_white_pixbuf);
            (*fp_g_object_unref)(gtk2_black_pixbuf);
        }

        gtk2_white_pixmap = (*fp_gdk_pixmap_new)(gtk2_window->window, width, height, -1);
        gtk2_black_pixmap = (*fp_gdk_pixmap_new)(gtk2_window->window, width, height, -1);

        gtk2_white_pixbuf = white;
        gtk2_black_pixbuf = black;

        gtk2_pixbuf_width = width;
        gtk2_pixbuf_height = height;
    }

    /* clear the pixmaps */
    gc = (*fp_gdk_gc_new)(gtk2_white_pixmap);
    (*fp_gdk_rgb_gc_set_foreground)(gc, 0xffffff);
    (*fp_gdk_draw_rectangle)(gtk2_white_pixmap, gc, TRUE, 0, 0, width, height);
    (*fp_g_object_unref)(gc);

    gc = (*fp_gdk_gc_new)(gtk2_black_pixmap);
    (*fp_gdk_rgb_gc_set_foreground)(gc, 0x000000);
    (*fp_gdk_draw_rectangle)(gtk2_black_pixmap, gc, TRUE, 0, 0, width, height);
    (*fp_g_object_unref)(gc);
}

/*
 * Restore image from white and black pixmaps and copy it into destination
 * buffer. This method compares two pixbufs taken from white and black
 * pixmaps and decodes color and alpha components. Pixbufs are RGB without
 * alpha, destination buffer is ABGR.
 *
 * The return value is the transparency type of the resulting image, either
 * one of java_awt_Transparency_OPAQUE, java_awt_Transparency_BITMASK, and
 * java_awt_Transparency_TRANSLUCENT.
 */
static gint gtk2_copy_image(gint *dst, gint width, gint height)
{
    gint i, j, r, g, b;
    guchar *white, *black;
    gint stride, padding;
    gboolean is_opaque = TRUE;
    gboolean is_bitmask = TRUE;

    (*fp_gdk_pixbuf_get_from_drawable)(gtk2_white_pixbuf, gtk2_white_pixmap,
            NULL, 0, 0, 0, 0, width, height);
    (*fp_gdk_pixbuf_get_from_drawable)(gtk2_black_pixbuf, gtk2_black_pixmap,
            NULL, 0, 0, 0, 0, width, height);

    white = (*fp_gdk_pixbuf_get_pixels)(gtk2_white_pixbuf);
    black = (*fp_gdk_pixbuf_get_pixels)(gtk2_black_pixbuf);
    stride = (*fp_gdk_pixbuf_get_rowstride)(gtk2_black_pixbuf);
    padding = stride - width * 4;
    if (padding >= 0 && stride > 0) {
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                int r1 = *white++;
                int r2 = *black++;
                int alpha = 0xff + r2 - r1;

                switch (alpha) {
                    case 0:       /* transparent pixel */
                        r = g = b = 0;
                        black += 3;
                        white += 3;
                        is_opaque = FALSE;
                        break;

                    case 0xff:    /* opaque pixel */
                        r = r2;
                        g = *black++;
                        b = *black++;
                        black++;
                        white += 3;
                        break;

                    default:      /* translucent pixel */
                        r = 0xff * r2 / alpha;
                        g = 0xff * *black++ / alpha;
                        b = 0xff * *black++ / alpha;
                        black++;
                        white += 3;
                        is_opaque = FALSE;
                        is_bitmask = FALSE;
                        break;
                }

                *dst++ = (alpha << 24 | r << 16 | g << 8 | b);
            }

            white += padding;
            black += padding;
        }
    }
    return is_opaque ? java_awt_Transparency_OPAQUE :
                       (is_bitmask ? java_awt_Transparency_BITMASK :
                                     java_awt_Transparency_TRANSLUCENT);
}

static void
gtk2_set_direction(GtkWidget *widget, GtkTextDirection dir)
{
    /*
     * Some engines (inexplicably) look at the direction of the widget's
     * parent, so we need to set the direction of both the widget and its
     * parent.
     */
    (*fp_gtk_widget_set_direction)(widget, dir);
    if (widget->parent != NULL) {
        (*fp_gtk_widget_set_direction)(widget->parent, dir);
    }
}

/*
 * Initializes the widget to correct state for some engines.
 * This is a pure empirical method.
 */
static void init_toggle_widget(WidgetType widget_type, gint synth_state)
{
    gboolean is_active = ((synth_state & SELECTED) != 0);

    if (widget_type == RADIO_BUTTON ||
        widget_type == CHECK_BOX ||
        widget_type == TOGGLE_BUTTON) {
        ((GtkToggleButton*)gtk2_widget)->active = is_active;
    }

    if ((synth_state & FOCUSED) != 0) {
        ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
    } else {
        ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;
    }

    if ((synth_state & MOUSE_OVER) != 0 && (synth_state & PRESSED) == 0 ||
           (synth_state & FOCUSED) != 0 && (synth_state & PRESSED) != 0) {
        gtk2_widget->state = GTK_STATE_PRELIGHT;
    } else if ((synth_state & DISABLED) != 0) {
        gtk2_widget->state = GTK_STATE_INSENSITIVE;
    } else {
        gtk2_widget->state = is_active ? GTK_STATE_ACTIVE : GTK_STATE_NORMAL;
    }
}

/* GTK state_type filter */
static GtkStateType get_gtk_state_type(WidgetType widget_type, gint synth_state)
{
    GtkStateType result = GTK_STATE_NORMAL;

    if ((synth_state & DISABLED) != 0) {
        result = GTK_STATE_INSENSITIVE;
    } else if ((synth_state & PRESSED) != 0) {
        result = GTK_STATE_ACTIVE;
    } else if ((synth_state & MOUSE_OVER) != 0) {
        result = GTK_STATE_PRELIGHT;
    }
    return result;
}

/* GTK shadow_type filter */
static GtkShadowType get_gtk_shadow_type(WidgetType widget_type, gint synth_state)
{
    GtkShadowType result = GTK_SHADOW_OUT;

    if ((synth_state & SELECTED) != 0) {
        result = GTK_SHADOW_IN;
    }
    return result;
}


static GtkWidget* gtk2_get_arrow(GtkArrowType arrow_type, GtkShadowType shadow_type)
{
    GtkWidget *arrow = NULL;
    if (NULL == gtk2_widgets[_GTK_ARROW_TYPE])
    {
        gtk2_widgets[_GTK_ARROW_TYPE] = (*fp_gtk_arrow_new)(arrow_type, shadow_type);
        (*fp_gtk_container_add)((GtkContainer *)gtk2_fixed, gtk2_widgets[_GTK_ARROW_TYPE]);
        (*fp_gtk_widget_realize)(gtk2_widgets[_GTK_ARROW_TYPE]);
    }
    arrow = gtk2_widgets[_GTK_ARROW_TYPE];

    (*fp_gtk_arrow_set)(arrow, arrow_type, shadow_type);
    return arrow;
}

static GtkAdjustment* create_adjustment()
{
    return (GtkAdjustment *)
            (*fp_gtk_adjustment_new)(50.0, 0.0, 100.0, 10.0, 20.0, 20.0);
}

/**
 * Returns a pointer to the cached native widget for the specified widget
 * type.
 */
static GtkWidget *gtk2_get_widget(WidgetType widget_type)
{
    gboolean init_result = FALSE;
    GtkWidget *result = NULL;
    switch (widget_type)
    {
        case BUTTON:
        case TABLE_HEADER:
            if (init_result = (NULL == gtk2_widgets[_GTK_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_BUTTON_TYPE] = (*fp_gtk_button_new)();
            }
            result = gtk2_widgets[_GTK_BUTTON_TYPE];
            break;
        case CHECK_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_CHECK_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_CHECK_BUTTON_TYPE] =
                    (*fp_gtk_check_button_new)();
            }
            result = gtk2_widgets[_GTK_CHECK_BUTTON_TYPE];
            break;
        case CHECK_BOX_MENU_ITEM:
            if (init_result = (NULL == gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE] =
                    (*fp_gtk_check_menu_item_new)();
            }
            result = gtk2_widgets[_GTK_CHECK_MENU_ITEM_TYPE];
            break;
        /************************************************************
         *    Creation a dedicated color chooser is dangerous because
         * it deadlocks the EDT
         ************************************************************/
/*        case COLOR_CHOOSER:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE]))
            {
                gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE] =
                    (*fp_gtk_color_selection_dialog_new)(NULL);
            }
            result = gtk2_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE];
            break;*/
        case COMBO_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_COMBO_BOX_TYPE]))
            {
                gtk2_widgets[_GTK_COMBO_BOX_TYPE] =
                    (*fp_gtk_combo_box_new)();
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_TYPE];
            break;
        case COMBO_BOX_ARROW_BUTTON:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE] =
                     (*fp_gtk_toggle_button_new)();
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE];
            break;
        case COMBO_BOX_TEXT_FIELD:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE]))
            {
                result = gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE] =
                     (*fp_gtk_entry_new)();
            }
            result = gtk2_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE];
            break;
        case DESKTOP_ICON:
        case INTERNAL_FRAME_TITLE_PANE:
        case LABEL:
            if (init_result = (NULL == gtk2_widgets[_GTK_LABEL_TYPE]))
            {
                gtk2_widgets[_GTK_LABEL_TYPE] =
                    (*fp_gtk_label_new)(NULL);
            }
            result = gtk2_widgets[_GTK_LABEL_TYPE];
            break;
        case DESKTOP_PANE:
        case PANEL:
        case ROOT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_CONTAINER_TYPE]))
            {
                /* There is no constructor for a container type.  I've
                 * chosen GtkFixed container since it has a default
                 * constructor.
                 */
                gtk2_widgets[_GTK_CONTAINER_TYPE] =
                    (*fp_gtk_fixed_new)();
            }
            result = gtk2_widgets[_GTK_CONTAINER_TYPE];
            break;
        case EDITOR_PANE:
        case TEXT_AREA:
        case TEXT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_TEXT_VIEW_TYPE]))
            {
                gtk2_widgets[_GTK_TEXT_VIEW_TYPE] =
                    (*fp_gtk_text_view_new)();
            }
            result = gtk2_widgets[_GTK_TEXT_VIEW_TYPE];
            break;
        case FORMATTED_TEXT_FIELD:
        case PASSWORD_FIELD:
        case TEXT_FIELD:
            if (init_result = (NULL == gtk2_widgets[_GTK_ENTRY_TYPE]))
            {
                gtk2_widgets[_GTK_ENTRY_TYPE] =
                    (*fp_gtk_entry_new)();
            }
            result = gtk2_widgets[_GTK_ENTRY_TYPE];
            break;
        case HANDLE_BOX:
            if (init_result = (NULL == gtk2_widgets[_GTK_HANDLE_BOX_TYPE]))
            {
                gtk2_widgets[_GTK_HANDLE_BOX_TYPE] =
                    (*fp_gtk_handle_box_new)();
            }
            result = gtk2_widgets[_GTK_HANDLE_BOX_TYPE];
            break;
        case HSCROLL_BAR:
        case HSCROLL_BAR_BUTTON_LEFT:
        case HSCROLL_BAR_BUTTON_RIGHT:
        case HSCROLL_BAR_TRACK:
        case HSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSCROLLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_HSCROLLBAR_TYPE] =
                    (*fp_gtk_hscrollbar_new)(create_adjustment());
            }
            result = gtk2_widgets[_GTK_HSCROLLBAR_TYPE];
            break;
        case HSEPARATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSEPARATOR_TYPE]))
            {
                gtk2_widgets[_GTK_HSEPARATOR_TYPE] =
                    (*fp_gtk_hseparator_new)();
            }
            result = gtk2_widgets[_GTK_HSEPARATOR_TYPE];
            break;
        case HSLIDER:
        case HSLIDER_THUMB:
        case HSLIDER_TRACK:
            if (init_result = (NULL == gtk2_widgets[_GTK_HSCALE_TYPE]))
            {
                gtk2_widgets[_GTK_HSCALE_TYPE] =
                    (*fp_gtk_hscale_new)(NULL);
            }
            result = gtk2_widgets[_GTK_HSCALE_TYPE];
            break;
        case HSPLIT_PANE_DIVIDER:
        case SPLIT_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_HPANED_TYPE]))
            {
                gtk2_widgets[_GTK_HPANED_TYPE] = (*fp_gtk_hpaned_new)();
            }
            result = gtk2_widgets[_GTK_HPANED_TYPE];
            break;
        case IMAGE:
            if (init_result = (NULL == gtk2_widgets[_GTK_IMAGE_TYPE]))
            {
                gtk2_widgets[_GTK_IMAGE_TYPE] = (*fp_gtk_image_new)();
            }
            result = gtk2_widgets[_GTK_IMAGE_TYPE];
            break;
        case INTERNAL_FRAME:
            if (init_result = (NULL == gtk2_widgets[_GTK_WINDOW_TYPE]))
            {
                gtk2_widgets[_GTK_WINDOW_TYPE] =
                    (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
            }
            result = gtk2_widgets[_GTK_WINDOW_TYPE];
            break;
        case TOOL_TIP:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOOLTIP_TYPE]))
            {
                result = (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
                (*fp_gtk_widget_set_name)(result, "gtk-tooltips");
                gtk2_widgets[_GTK_TOOLTIP_TYPE] = result;
            }
            result = gtk2_widgets[_GTK_TOOLTIP_TYPE];
            break;
        case LIST:
        case TABLE:
        case TREE:
        case TREE_CELL:
            if (init_result = (NULL == gtk2_widgets[_GTK_TREE_VIEW_TYPE]))
            {
                gtk2_widgets[_GTK_TREE_VIEW_TYPE] =
                    (*fp_gtk_tree_view_new)();
            }
            result = gtk2_widgets[_GTK_TREE_VIEW_TYPE];
            break;
        case TITLED_BORDER:
            if (init_result = (NULL == gtk2_widgets[_GTK_FRAME_TYPE]))
            {
                gtk2_widgets[_GTK_FRAME_TYPE] = fp_gtk_frame_new(NULL);
            }
            result = gtk2_widgets[_GTK_FRAME_TYPE];
            break;
        case POPUP_MENU:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_TYPE] =
                    (*fp_gtk_menu_new)();
            }
            result = gtk2_widgets[_GTK_MENU_TYPE];
            break;
        case MENU:
        case MENU_ITEM:
        case MENU_ITEM_ACCELERATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_ITEM_TYPE] =
                    (*fp_gtk_menu_item_new)();
            }
            result = gtk2_widgets[_GTK_MENU_ITEM_TYPE];
            break;
        case MENU_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_MENU_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_MENU_BAR_TYPE] =
                    (*fp_gtk_menu_bar_new)();
            }
            result = gtk2_widgets[_GTK_MENU_BAR_TYPE];
            break;
        case COLOR_CHOOSER:
        case OPTION_PANE:
            if (init_result = (NULL == gtk2_widgets[_GTK_DIALOG_TYPE]))
            {
                gtk2_widgets[_GTK_DIALOG_TYPE] =
                    (*fp_gtk_dialog_new)();
            }
            result = gtk2_widgets[_GTK_DIALOG_TYPE];
            break;
        case POPUP_MENU_SEPARATOR:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE] =
                    (*fp_gtk_separator_menu_item_new)();
            }
            result = gtk2_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE];
            break;
        case HPROGRESS_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE] =
                    (*fp_gtk_progress_bar_new)();
            }
            result = gtk2_widgets[_GTK_HPROGRESS_BAR_TYPE];
            break;
        case VPROGRESS_BAR:
            if (init_result = (NULL == gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE]))
            {
                gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE] =
                    (*fp_gtk_progress_bar_new)();
                /*
                 * Vertical JProgressBars always go bottom-to-top,
                 * regardless of the ComponentOrientation.
                 */
                (*fp_gtk_progress_bar_set_orientation)(
                    (GtkProgressBar *)gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE],
                    GTK_PROGRESS_BOTTOM_TO_TOP);
            }
            result = gtk2_widgets[_GTK_VPROGRESS_BAR_TYPE];
            break;
        case RADIO_BUTTON:
            if (init_result = (NULL == gtk2_widgets[_GTK_RADIO_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_RADIO_BUTTON_TYPE] =
                    (*fp_gtk_radio_button_new)(NULL);
            }
            result = gtk2_widgets[_GTK_RADIO_BUTTON_TYPE];
            break;
        case RADIO_BUTTON_MENU_ITEM:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE] =
                    (*fp_gtk_radio_menu_item_new)(NULL);
            }
            result = gtk2_widgets[_GTK_RADIO_MENU_ITEM_TYPE];
            break;
        case SCROLL_PANE:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE]))
            {
                gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE] =
                    (*fp_gtk_scrolled_window_new)(NULL, NULL);
            }
            result = gtk2_widgets[_GTK_SCROLLED_WINDOW_TYPE];
            break;
        case SPINNER:
        case SPINNER_ARROW_BUTTON:
        case SPINNER_TEXT_FIELD:
            if (init_result = (NULL == gtk2_widgets[_GTK_SPIN_BUTTON_TYPE]))
            {
                result = gtk2_widgets[_GTK_SPIN_BUTTON_TYPE] =
                    (*fp_gtk_spin_button_new)(NULL, 0, 0);
            }
            result = gtk2_widgets[_GTK_SPIN_BUTTON_TYPE];
            break;
        case TABBED_PANE:
        case TABBED_PANE_TAB_AREA:
        case TABBED_PANE_CONTENT:
        case TABBED_PANE_TAB:
            if (init_result = (NULL == gtk2_widgets[_GTK_NOTEBOOK_TYPE]))
            {
                gtk2_widgets[_GTK_NOTEBOOK_TYPE] =
                    (*fp_gtk_notebook_new)(NULL);
            }
            result = gtk2_widgets[_GTK_NOTEBOOK_TYPE];
            break;
        case TOGGLE_BUTTON:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE]))
            {
                gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE] =
                    (*fp_gtk_toggle_button_new)(NULL);
            }
            result = gtk2_widgets[_GTK_TOGGLE_BUTTON_TYPE];
            break;
        case TOOL_BAR:
        case TOOL_BAR_DRAG_WINDOW:
            if (init_result = (NULL == gtk2_widgets[_GTK_TOOLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_TOOLBAR_TYPE] =
                    (*fp_gtk_toolbar_new)(NULL);
            }
            result = gtk2_widgets[_GTK_TOOLBAR_TYPE];
            break;
        case TOOL_BAR_SEPARATOR:
            if (init_result =
                    (NULL == gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE]))
            {
                gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE] =
                    (*fp_gtk_separator_tool_item_new)();
            }
            result = gtk2_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE];
            break;
        case VIEWPORT:
            if (init_result = (NULL == gtk2_widgets[_GTK_VIEWPORT_TYPE]))
            {
                GtkAdjustment *adjustment = create_adjustment();
                gtk2_widgets[_GTK_VIEWPORT_TYPE] =
                    (*fp_gtk_viewport_new)(adjustment, adjustment);
            }
            result = gtk2_widgets[_GTK_VIEWPORT_TYPE];
            break;
        case VSCROLL_BAR:
        case VSCROLL_BAR_BUTTON_UP:
        case VSCROLL_BAR_BUTTON_DOWN:
        case VSCROLL_BAR_TRACK:
        case VSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSCROLLBAR_TYPE]))
            {
                gtk2_widgets[_GTK_VSCROLLBAR_TYPE] =
                    (*fp_gtk_vscrollbar_new)(create_adjustment());
            }
            result = gtk2_widgets[_GTK_VSCROLLBAR_TYPE];
            break;
        case VSEPARATOR:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSEPARATOR_TYPE]))
            {
                gtk2_widgets[_GTK_VSEPARATOR_TYPE] =
                    (*fp_gtk_vseparator_new)();
            }
            result = gtk2_widgets[_GTK_VSEPARATOR_TYPE];
            break;
        case VSLIDER:
        case VSLIDER_THUMB:
        case VSLIDER_TRACK:
            if (init_result = (NULL == gtk2_widgets[_GTK_VSCALE_TYPE]))
            {
                gtk2_widgets[_GTK_VSCALE_TYPE] =
                    (*fp_gtk_vscale_new)(NULL);
            }
            result = gtk2_widgets[_GTK_VSCALE_TYPE];
            /*
             * Vertical JSliders start at the bottom, while vertical
             * GtkVScale widgets start at the top (by default), so to fix
             * this we set the "inverted" flag to get the Swing behavior.
             */
            ((GtkRange*)result)->inverted = 1;
            break;
        case VSPLIT_PANE_DIVIDER:
            if (init_result = (NULL == gtk2_widgets[_GTK_VPANED_TYPE]))
            {
                gtk2_widgets[_GTK_VPANED_TYPE] = (*fp_gtk_vpaned_new)();
            }
            result = gtk2_widgets[_GTK_VPANED_TYPE];
            break;
        default:
            result = NULL;
            break;
    }

    if (result != NULL && init_result)
    {
        if (widget_type == RADIO_BUTTON_MENU_ITEM ||
                widget_type == CHECK_BOX_MENU_ITEM ||
                widget_type == MENU_ITEM ||
                widget_type == MENU ||
                widget_type == POPUP_MENU_SEPARATOR)
        {
            GtkWidget *menu = gtk2_get_widget(POPUP_MENU);
            (*fp_gtk_menu_shell_append)((GtkMenuShell *)menu, result);
        }
        else if (widget_type == POPUP_MENU)
        {
            GtkWidget *menu_bar = gtk2_get_widget(MENU_BAR);
            GtkWidget *root_menu = (*fp_gtk_menu_item_new)();
            (*fp_gtk_menu_item_set_submenu)((GtkMenuItem*)root_menu, result);
            (*fp_gtk_menu_shell_append)((GtkMenuShell *)menu_bar, root_menu);
        }
        else if (widget_type == COMBO_BOX_ARROW_BUTTON ||
                 widget_type == COMBO_BOX_TEXT_FIELD)
        {
            /*
            * We add a regular GtkButton/GtkEntry to a GtkComboBoxEntry
            * in order to trick engines into thinking it's a real combobox
            * arrow button/text field.
            */
            GtkWidget *combo = (*fp_gtk_combo_box_entry_new)();

            if (new_combo && widget_type == COMBO_BOX_ARROW_BUTTON) {
                (*fp_gtk_widget_set_parent)(result, combo);
                ((GtkBin*)combo)->child = result;
            } else {
                (*fp_gtk_container_add)((GtkContainer *)combo, result);
            }
            (*fp_gtk_container_add)((GtkContainer *)gtk2_fixed, combo);
        }
        else if (widget_type != TOOL_TIP &&
                 widget_type != INTERNAL_FRAME &&
                 widget_type != OPTION_PANE)
        {
            (*fp_gtk_container_add)((GtkContainer *)gtk2_fixed, result);
        }
        (*fp_gtk_widget_realize)(result);
    }
    return result;
}

void gtk2_paint_arrow(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkArrowType arrow_type, gboolean fill)
{
    static int w, h;
    static GtkRequisition size;

    if (widget_type == COMBO_BOX_ARROW_BUTTON || widget_type == TABLE)
        gtk2_widget = gtk2_get_arrow(arrow_type, shadow_type);
    else
        gtk2_widget = gtk2_get_widget(widget_type);

    switch (widget_type)
    {
        case SPINNER_ARROW_BUTTON:
            x = 1;
            y = ((arrow_type == GTK_ARROW_UP) ? 2 : 0);
            height -= 2;
            width -= 3;

            w = width / 2;
            w -= w % 2 - 1;
            h = (w + 1) / 2;
            break;

        case HSCROLL_BAR_BUTTON_LEFT:
        case HSCROLL_BAR_BUTTON_RIGHT:
        case VSCROLL_BAR_BUTTON_UP:
        case VSCROLL_BAR_BUTTON_DOWN:
            w = width / 2;
            h = height / 2;
            break;

        case COMBO_BOX_ARROW_BUTTON:
        case TABLE:
            x = 1;
            (*fp_gtk_widget_size_request)(gtk2_widget, &size);
            w = size.width - ((GtkMisc*)gtk2_widget)->xpad * 2;
            h = size.height - ((GtkMisc*)gtk2_widget)->ypad * 2;
            w = h = MIN(MIN(w, h), MIN(width,height)) * 0.7;
            break;

        default:
            w = width;
            h = height;
            break;
    }
    x += (width - w) / 2;
    y += (height - h) / 2;

    (*fp_gtk_paint_arrow)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, arrow_type, fill,
            x, y, w, h);
    (*fp_gtk_paint_arrow)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, arrow_type, fill,
            x, y, w, h);
}

static void gtk2_paint_box(WidgetType widget_type, GtkStateType state_type,
                    GtkShadowType shadow_type, const gchar *detail,
                    gint x, gint y, gint width, gint height,
                    gint synth_state, GtkTextDirection dir)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    /*
     * The clearlooks engine sometimes looks at the widget's state field
     * instead of just the state_type variable that we pass in, so to account
     * for those cases we set the widget's state field accordingly.  The
     * flags field is similarly important for things like focus/default state.
     */
    gtk2_widget->state = state_type;

    if (widget_type == HSLIDER_TRACK) {
        /*
         * For horizontal JSliders with right-to-left orientation, we need
         * to set the "inverted" flag to match the native GTK behavior where
         * the foreground highlight is on the right side of the slider thumb.
         * This is needed especially for the ubuntulooks engine, which looks
         * exclusively at the "inverted" flag to determine on which side of
         * the thumb to paint the highlight...
         */
        ((GtkRange*)gtk2_widget)->inverted = (dir == GTK_TEXT_DIR_RTL);

        /*
         * Note however that other engines like clearlooks will look at both
         * the "inverted" field and the text direction to determine how
         * the foreground highlight is painted:
         *     !inverted && ltr --> paint highlight on left side
         *     !inverted && rtl --> paint highlight on right side
         *      inverted && ltr --> paint highlight on right side
         *      inverted && rtl --> paint highlight on left side
         * So the only way to reliably get the desired results for horizontal
         * JSlider (i.e., highlight on left side for LTR ComponentOrientation
         * and highlight on right side for RTL ComponentOrientation) is to
         * always override text direction as LTR, and then set the "inverted"
         * flag accordingly (as we have done above).
         */
        dir = GTK_TEXT_DIR_LTR;
    }

    /*
     * Some engines (e.g. clearlooks) will paint the shadow of certain
     * widgets (e.g. COMBO_BOX_ARROW_BUTTON) differently depending on the
     * the text direction.
     */
    gtk2_set_direction(gtk2_widget, dir);

    switch (widget_type) {
    case BUTTON:
        if (synth_state & DEFAULT) {
            ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_DEFAULT;
        } else {
            ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_DEFAULT;
        }
        break;
    case TOGGLE_BUTTON:
        init_toggle_widget(widget_type, synth_state);
        break;
    case HSCROLL_BAR_BUTTON_LEFT:
        /*
         * The clearlooks engine will draw a "left" button when:
         *   x == w->allocation.x
         *
         * The ubuntulooks engine will draw a "left" button when:
         *   [x,y,width,height]
         *     intersects
         *   [w->alloc.x,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that a "left"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = height;
        break;
    case HSCROLL_BAR_BUTTON_RIGHT:
        /*
         * The clearlooks engine will draw a "right" button when:
         *   x + width == w->allocation.x + w->allocation.width
         *
         * The ubuntulooks engine will draw a "right" button when:
         *   [x,y,width,height]
         *     does not intersect
         *   [w->alloc.x,w->alloc.y,width,height]
         *     but does intersect
         *   [w->alloc.x+width,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that a "right"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x+width;
        gtk2_widget->allocation.y = 0;
        gtk2_widget->allocation.width = 0;
        gtk2_widget->allocation.height = height;
        break;
    case VSCROLL_BAR_BUTTON_UP:
        /*
         * The clearlooks engine will draw an "up" button when:
         *   y == w->allocation.y
         *
         * The ubuntulooks engine will draw an "up" button when:
         *   [x,y,width,height]
         *     intersects
         *   [w->alloc.x,w->alloc.y,width,height]
         *
         * The values that are set below should ensure that an "up"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = height;
        break;
    case VSCROLL_BAR_BUTTON_DOWN:
        /*
         * The clearlooks engine will draw a "down" button when:
         *   y + height == w->allocation.y + w->allocation.height
         *
         * The ubuntulooks engine will draw a "down" button when:
         *   [x,y,width,height]
         *     does not intersect
         *   [w->alloc.x,w->alloc.y,width,height]
         *     but does intersect
         *   [w->alloc.x,w->alloc.y+height,width,height]
         *
         * The values that are set below should ensure that a "down"
         * button is rendered for both of these (and other) engines.
         */
        gtk2_widget->allocation.x = x;
        gtk2_widget->allocation.y = y+height;
        gtk2_widget->allocation.width = width;
        gtk2_widget->allocation.height = 0;
        break;
    default:
        break;
    }

    (*fp_gtk_paint_box)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);
    (*fp_gtk_paint_box)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk2_set_direction(gtk2_widget, GTK_TEXT_DIR_LTR);
}

void gtk2_paint_box_gap(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width)
{
    /* Clearlooks needs a real clip area to paint the gap properly */
    GdkRectangle area = { x, y, width, height };

    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_box_gap)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, &area, gtk2_widget, detail,
            x, y, width, height, gap_side, gap_x, gap_width);
    (*fp_gtk_paint_box_gap)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, &area, gtk2_widget, detail,
            x, y, width, height, gap_side, gap_x, gap_width);
}

static void gtk2_paint_check(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    GtkStateType state_type = get_gtk_state_type(widget_type, synth_state);
    GtkShadowType shadow_type = get_gtk_shadow_type(widget_type, synth_state);

    gtk2_widget = gtk2_get_widget(widget_type);
    init_toggle_widget(widget_type, synth_state);

    (*fp_gtk_paint_check)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    (*fp_gtk_paint_check)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

static void gtk2_paint_diamond(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_diamond)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    (*fp_gtk_paint_diamond)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

static void gtk2_paint_expander(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height,
        GtkExpanderStyle expander_style)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_expander)(gtk2_widget->style, gtk2_white_pixmap,
            state_type, NULL, gtk2_widget, detail,
            x + width / 2, y + height / 2, expander_style);
    (*fp_gtk_paint_expander)(gtk2_widget->style, gtk2_black_pixmap,
            state_type, NULL, gtk2_widget, detail,
            x + width / 2, y + height / 2, expander_style);
}

static void gtk2_paint_extension(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_extension)(gtk2_widget->style, gtk2_white_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, gap_side);
    (*fp_gtk_paint_extension)(gtk2_widget->style, gtk2_black_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, gap_side);
}

static void gtk2_paint_flat_box(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, gboolean has_focus)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    if (has_focus)
        ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
    else
        ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;

    (*fp_gtk_paint_flat_box)(gtk2_widget->style, gtk2_white_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    (*fp_gtk_paint_flat_box)(gtk2_widget->style, gtk2_black_pixmap,
            state_type, shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

static void gtk2_paint_focus(WidgetType widget_type, GtkStateType state_type,
        const char *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_focus)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, x, y, width, height);
    (*fp_gtk_paint_focus)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, x, y, width, height);
}

static void gtk2_paint_handle(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_handle)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
    (*fp_gtk_paint_handle)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
}

static void gtk2_paint_hline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_hline)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, x, x + width, y);
    (*fp_gtk_paint_hline)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, x, x + width, y);
}

static void gtk2_paint_option(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    GtkStateType state_type = get_gtk_state_type(widget_type, synth_state);
    GtkShadowType shadow_type = get_gtk_shadow_type(widget_type, synth_state);

    gtk2_widget = gtk2_get_widget(widget_type);
    init_toggle_widget(widget_type, synth_state);

    (*fp_gtk_paint_option)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
    (*fp_gtk_paint_option)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height);
}

static void gtk2_paint_shadow(WidgetType widget_type, GtkStateType state_type,
                       GtkShadowType shadow_type, const gchar *detail,
                       gint x, gint y, gint width, gint height,
                       gint synth_state, GtkTextDirection dir)
{
    gtk2_widget = gtk2_get_widget(widget_type);

    /*
     * The clearlooks engine sometimes looks at the widget's state field
     * instead of just the state_type variable that we pass in, so to account
     * for those cases we set the widget's state field accordingly.  The
     * flags field is similarly important for things like focus state.
     */
    gtk2_widget->state = state_type;

    /*
     * Some engines (e.g. clearlooks) will paint the shadow of certain
     * widgets (e.g. COMBO_BOX_TEXT_FIELD) differently depending on the
     * the text direction.
     */
    gtk2_set_direction(gtk2_widget, dir);

    switch (widget_type) {
    case COMBO_BOX_TEXT_FIELD:
    case FORMATTED_TEXT_FIELD:
    case PASSWORD_FIELD:
    case SPINNER_TEXT_FIELD:
    case TEXT_FIELD:
        if (synth_state & FOCUSED) {
            ((GtkObject*)gtk2_widget)->flags |= GTK_HAS_FOCUS;
        } else {
            ((GtkObject*)gtk2_widget)->flags &= ~GTK_HAS_FOCUS;
        }
        break;
    default:
        break;
    }

    (*fp_gtk_paint_shadow)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);
    (*fp_gtk_paint_shadow)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail, x, y, width, height);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk2_set_direction(gtk2_widget, GTK_TEXT_DIR_LTR);
}

static void gtk2_paint_slider(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation,
        gboolean has_focus)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_slider)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
    (*fp_gtk_paint_slider)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            shadow_type, NULL, gtk2_widget, detail,
            x, y, width, height, orientation);
}

static void gtk2_paint_vline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_paint_vline)(gtk2_widget->style, gtk2_white_pixmap, state_type,
            NULL, gtk2_widget, detail, y, y + height, x);
    (*fp_gtk_paint_vline)(gtk2_widget->style, gtk2_black_pixmap, state_type,
            NULL, gtk2_widget, detail, y, y + height, x);
}

static void gtk_paint_background(WidgetType widget_type, GtkStateType state_type,
        gint x, gint y, gint width, gint height)
{
    gtk2_widget = gtk2_get_widget(widget_type);
    (*fp_gtk_style_apply_default_background)(gtk2_widget->style,
            gtk2_white_pixmap, TRUE, state_type, NULL, x, y, width, height);
    (*fp_gtk_style_apply_default_background)(gtk2_widget->style,
            gtk2_black_pixmap, TRUE, state_type, NULL, x, y, width, height);
}

static GdkPixbuf *gtk2_get_stock_icon(gint widget_type, const gchar *stock_id,
        GtkIconSize size, GtkTextDirection direction, const char *detail)
{
    init_containers();
    gtk2_widget = gtk2_get_widget((widget_type < 0) ? IMAGE : widget_type);
    gtk2_widget->state = GTK_STATE_NORMAL;
    (*fp_gtk_widget_set_direction)(gtk2_widget, direction);
    return (*fp_gtk_widget_render_icon)(gtk2_widget, stock_id, size, detail);
}

static jboolean gtk2_get_pixbuf_data(JNIEnv *env, GdkPixbuf* pixbuf,
                              jmethodID icon_upcall_method, jobject this) {
    if (!pixbuf) {
        return JNI_FALSE;
    }
    guchar *pixbuf_data = (*fp_gdk_pixbuf_get_pixels)(pixbuf);
    if (pixbuf_data) {
        int row_stride = (*fp_gdk_pixbuf_get_rowstride)(pixbuf);
        int width = (*fp_gdk_pixbuf_get_width)(pixbuf);
        int height = (*fp_gdk_pixbuf_get_height)(pixbuf);
        int bps = (*fp_gdk_pixbuf_get_bits_per_sample)(pixbuf);
        int channels = (*fp_gdk_pixbuf_get_n_channels)(pixbuf);
        gboolean alpha = (*fp_gdk_pixbuf_get_has_alpha)(pixbuf);

        jbyteArray data = (*env)->NewByteArray(env, (row_stride * height));
        JNU_CHECK_EXCEPTION_RETURN(env, JNI_FALSE);

        (*env)->SetByteArrayRegion(env, data, 0, (row_stride * height),
                                   (jbyte *)pixbuf_data);
        (*fp_g_object_unref)(pixbuf);

        /* Call the callback method to create the image on the Java side. */
        (*env)->CallVoidMethod(env, this, icon_upcall_method, data,
                width, height, row_stride, bps, channels, alpha);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

static jboolean gtk2_get_file_icon_data(JNIEnv *env, const char *filename,
                 GError **error, jmethodID icon_upcall_method, jobject this) {
    GdkPixbuf* pixbuf = fp_gdk_pixbuf_new_from_file(filename, error);
    return gtk2_get_pixbuf_data(env, pixbuf, icon_upcall_method, this);
}

static jboolean gtk2_get_icon_data(JNIEnv *env, gint widget_type,
                              const gchar *stock_id, GtkIconSize size,
                              GtkTextDirection direction, const char *detail,
                              jmethodID icon_upcall_method, jobject this) {
    GdkPixbuf* pixbuf = gtk2_get_stock_icon(widget_type, stock_id, size,
                                       direction, detail);
    return gtk2_get_pixbuf_data(env, pixbuf, icon_upcall_method, this);
}

/*************************************************/
static gint gtk2_get_xthickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;
    return style->xthickness;
}

static gint gtk2_get_ythickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;
    return style->ythickness;
}

/*************************************************/
static guint8 recode_color(guint16 channel)
{
    return (guint8)(channel>>8);
}

static gint gtk2_get_color_for_state(JNIEnv *env, WidgetType widget_type,
                              GtkStateType state_type, ColorType color_type)
{
    gint result = 0;
    GdkColor *color = NULL;

    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    GtkStyle* style = gtk2_widget->style;

    switch (color_type)
    {
        case FOREGROUND:
            color = &(style->fg[state_type]);
            break;
        case BACKGROUND:
            color = &(style->bg[state_type]);
            break;
        case TEXT_FOREGROUND:
            color = &(style->text[state_type]);
            break;
        case TEXT_BACKGROUND:
            color = &(style->base[state_type]);
            break;
        case LIGHT:
            color = &(style->light[state_type]);
            break;
        case DARK:
            color = &(style->dark[state_type]);
            break;
        case MID:
            color = &(style->mid[state_type]);
            break;
        case FOCUS:
        case BLACK:
            color = &(style->black);
            break;
        case WHITE:
            color = &(style->white);
            break;
    }

    if (color)
        result = recode_color(color->red)   << 16 |
                 recode_color(color->green) << 8  |
                 recode_color(color->blue);

    return result;
}

/*************************************************/
static jobject create_Boolean(JNIEnv *env, jboolean boolean_value);
static jobject create_Integer(JNIEnv *env, jint int_value);
static jobject create_Long(JNIEnv *env, jlong long_value);
static jobject create_Float(JNIEnv *env, jfloat float_value);
static jobject create_Double(JNIEnv *env, jdouble double_value);
static jobject create_Character(JNIEnv *env, jchar char_value);
static jobject create_Insets(JNIEnv *env, GtkBorder *border);

static jobject gtk2_get_class_value(JNIEnv *env, WidgetType widget_type,
                              const char* key)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);

    GValue value;
    value.g_type = 0;

    GParamSpec* param = (*fp_gtk_widget_class_find_style_property)(
                                    ((GTypeInstance*)gtk2_widget)->g_class, key);
    if( param )
    {
        (*fp_g_value_init)( &value, param->value_type );
        (*fp_gtk_widget_style_get_property)(gtk2_widget, key, &value);

        if( (*fp_g_type_is_a)( param->value_type, G_TYPE_BOOLEAN ))
        {
            gboolean val = (*fp_g_value_get_boolean)(&value);
            return create_Boolean(env, (jboolean)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_CHAR ))
        {
            gchar val = (*fp_g_value_get_char)(&value);
            return create_Character(env, (jchar)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_UCHAR ))
        {
            guchar val = (*fp_g_value_get_uchar)(&value);
            return create_Character(env, (jchar)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_INT ))
        {
            gint val = (*fp_g_value_get_int)(&value);
            return create_Integer(env, (jint)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_UINT ))
        {
            guint val = (*fp_g_value_get_uint)(&value);
            return create_Integer(env, (jint)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_LONG ))
        {
            glong val = (*fp_g_value_get_long)(&value);
            return create_Long(env, (jlong)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_ULONG ))
        {
            gulong val = (*fp_g_value_get_ulong)(&value);
            return create_Long(env, (jlong)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_INT64 ))
        {
            gint64 val = (*fp_g_value_get_int64)(&value);
            return create_Long(env, (jlong)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_UINT64 ))
        {
            guint64 val = (*fp_g_value_get_uint64)(&value);
            return create_Long(env, (jlong)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_FLOAT ))
        {
            gfloat val = (*fp_g_value_get_float)(&value);
            return create_Float(env, (jfloat)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_DOUBLE ))
        {
            gdouble val = (*fp_g_value_get_double)(&value);
            return create_Double(env, (jdouble)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_ENUM ))
        {
            gint val = (*fp_g_value_get_enum)(&value);
            return create_Integer(env, (jint)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_FLAGS ))
        {
            guint val = (*fp_g_value_get_flags)(&value);
            return create_Integer(env, (jint)val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_STRING ))
        {
            const gchar* val = (*fp_g_value_get_string)(&value);

            /* We suppose that all values come in C locale and
             * utf-8 representation of a string is the same as
             * the string itself. If this isn't so we should
             * use g_convert.
             */
            return (*env)->NewStringUTF(env, val);
        }
        else if( (*fp_g_type_is_a)( param->value_type, GTK_TYPE_BORDER ))
        {
            GtkBorder *border = (GtkBorder*)(*fp_g_value_get_boxed)(&value);
            return border ? create_Insets(env, border) : NULL;
        }

        /*      TODO: Other types are not supported yet.*/
/*        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_PARAM ))
        {
            GParamSpec* val = (*fp_g_value_get_param)(&value);
            printf( "Param: %p\n", val );
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_BOXED ))
        {
            gpointer* val = (*fp_g_value_get_boxed)(&value);
            printf( "Boxed: %p\n", val );
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_POINTER ))
        {
            gpointer* val = (*fp_g_value_get_pointer)(&value);
            printf( "Pointer: %p\n", val );
        }
        else if( (*fp_g_type_is_a)( param->value_type, G_TYPE_OBJECT ))
        {
            GObject* val = (GObject*)(*fp_g_value_get_object)(&value);
            printf( "Object: %p\n", val );
        }*/
    }

    return NULL;
}

static void gtk2_set_range_value(WidgetType widget_type, jdouble value,
                          jdouble min, jdouble max, jdouble visible)
{
    GtkAdjustment *adj;

    gtk2_widget = gtk2_get_widget(widget_type);

    adj = (*fp_gtk_range_get_adjustment)((GtkRange *)gtk2_widget);
    adj->value = (gdouble)value;
    adj->lower = (gdouble)min;
    adj->upper = (gdouble)max;
    adj->page_size = (gdouble)visible;
}

/*************************************************/
static jobject create_Object(JNIEnv *env, jmethodID *cid,
                             const char* class_name,
                             const char* signature,
                             jvalue* value)
{
    jclass  class;
    jobject result;

    class = (*env)->FindClass(env, class_name);
    if( class == NULL )
        return NULL; /* can't find/load the class, exception thrown */

    if( *cid == NULL)
    {
        *cid = (*env)->GetMethodID(env, class, "<init>", signature);
        if( *cid == NULL )
        {
            (*env)->DeleteLocalRef(env, class);
            return NULL; /* can't find/get the method, exception thrown */
        }
    }

    result = (*env)->NewObjectA(env, class, *cid, value);

    (*env)->DeleteLocalRef(env, class);
    return result;
}

jobject create_Boolean(JNIEnv *env, jboolean boolean_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.z = boolean_value;

    return create_Object(env, &cid, "java/lang/Boolean", "(Z)V", &value);
}

jobject create_Integer(JNIEnv *env, jint int_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.i = int_value;

    return create_Object(env, &cid, "java/lang/Integer", "(I)V", &value);
}

jobject create_Long(JNIEnv *env, jlong long_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.j = long_value;

    return create_Object(env, &cid, "java/lang/Long", "(J)V", &value);
}

jobject create_Float(JNIEnv *env, jfloat float_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.f = float_value;

    return create_Object(env, &cid, "java/lang/Float", "(F)V", &value);
}

jobject create_Double(JNIEnv *env, jdouble double_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.d = double_value;

    return create_Object(env, &cid, "java/lang/Double", "(D)V", &value);
}

jobject create_Character(JNIEnv *env, jchar char_value)
{
    static jmethodID cid = NULL;
    jvalue value;

    value.c = char_value;

    return create_Object(env, &cid, "java/lang/Character", "(C)V", &value);
}


jobject create_Insets(JNIEnv *env, GtkBorder *border)
{
    static jmethodID cid = NULL;
    jvalue values[4];

    values[0].i = border->top;
    values[1].i = border->left;
    values[2].i = border->bottom;
    values[3].i = border->right;

    return create_Object(env, &cid, "java/awt/Insets", "(IIII)V", values);
}

/*********************************************/
static jstring gtk2_get_pango_font_name(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk2_widget = gtk2_get_widget(widget_type);
    jstring  result = NULL;
    GtkStyle* style = gtk2_widget->style;

    if (style && style->font_desc)
    {
        gchar* val = (*fp_pango_font_description_to_string)(style->font_desc);
        result = (*env)->NewStringUTF(env, val);
        (*fp_g_free)( val );
    }

    return result;
}

/***********************************************/
static jobject get_string_property(JNIEnv *env, GtkSettings* settings, const gchar* key)
{
    jobject result = NULL;
    gchar*  strval = NULL;

    (*fp_g_object_get)(settings, key, &strval, NULL);
    result = (*env)->NewStringUTF(env, strval);
    (*fp_g_free)(strval);

    return result;
}

static jobject get_integer_property(JNIEnv *env, GtkSettings* settings, const gchar* key)
{
    gint intval = 0;
    (*fp_g_object_get)(settings, key, &intval, NULL);
    return create_Integer(env, intval);
}

static jobject get_boolean_property(JNIEnv *env, GtkSettings* settings, const gchar* key)
{
    gint intval = 0;
    (*fp_g_object_get)(settings, key, &intval, NULL);
    return create_Boolean(env, intval);
}

static jobject gtk2_get_setting(JNIEnv *env, Setting property)
{
    GtkSettings* settings = (*fp_gtk_settings_get_default)();

    switch (property)
    {
        case GTK_FONT_NAME:
            return get_string_property(env, settings, "gtk-font-name");
        case GTK_ICON_SIZES:
            return get_string_property(env, settings, "gtk-icon-sizes");
        case GTK_CURSOR_BLINK:
            return get_boolean_property(env, settings, "gtk-cursor-blink");
        case GTK_CURSOR_BLINK_TIME:
            return get_integer_property(env, settings, "gtk-cursor-blink-time");
    }

    return NULL;
}

static gboolean gtk2_get_drawable_data(JNIEnv *env, jintArray pixelArray, jint x,
     jint y, jint width, jint height, jint jwidth, int dx, int dy, jint scale) {
    GdkPixbuf *pixbuf;
    jint *ary;

    GdkWindow *root = (*fp_gdk_get_default_root_window)();

    pixbuf = (*fp_gdk_pixbuf_get_from_drawable)(NULL, root, NULL, x, y,
                                                    0, 0, width, height);
    if (pixbuf && scale != 1) {
        GdkPixbuf *scaledPixbuf;
        x /= scale;
        y /= scale;
        width /= scale;
        height /= scale;
        dx /= scale;
        dy /= scale;
        scaledPixbuf = (*fp_gdk_pixbuf_scale_simple)(pixbuf, width, height,
                                                     GDK_INTERP_BILINEAR);
        (*fp_g_object_unref)(pixbuf);
        pixbuf = scaledPixbuf;
    }

    if (pixbuf) {
        int nchan = (*fp_gdk_pixbuf_get_n_channels)(pixbuf);
        int stride = (*fp_gdk_pixbuf_get_rowstride)(pixbuf);

        if ((*fp_gdk_pixbuf_get_width)(pixbuf) == width
                && (*fp_gdk_pixbuf_get_height)(pixbuf) == height
                && (*fp_gdk_pixbuf_get_bits_per_sample)(pixbuf) == 8
                && (*fp_gdk_pixbuf_get_colorspace)(pixbuf) == GDK_COLORSPACE_RGB
                && nchan >= 3
                ) {
            guchar *p, *pix = (*fp_gdk_pixbuf_get_pixels)(pixbuf);

            ary = (*env)->GetPrimitiveArrayCritical(env, pixelArray, NULL);
            if (ary) {
                jint _x, _y;
                int index;
                for (_y = 0; _y < height; _y++) {
                    for (_x = 0; _x < width; _x++) {
                        p = pix + (intptr_t) _y * stride + _x * nchan;

                        index = (_y + dy) * jwidth + (_x + dx);
                        ary[index] = 0xff000000
                                        | (p[0] << 16)
                                        | (p[1] << 8)
                                        | (p[2]);

                    }
                }
                (*env)->ReleasePrimitiveArrayCritical(env, pixelArray, ary, 0);
            }
        }
        (*fp_g_object_unref)(pixbuf);
    }
    return JNI_FALSE;
}

static GdkWindow* gtk2_get_window(void *widget) {
    return ((GtkWidget*)widget)->window;
}

void gtk2_init(GtkApi* gtk) {
    gtk->version = GTK_2;

    gtk->show_uri_load = &gtk2_show_uri_load;
    gtk->unload = &gtk2_unload;
    gtk->flush_event_loop = &flush_gtk_event_loop;
    gtk->gtk_check_version = fp_gtk_check_version;
    gtk->get_setting = &gtk2_get_setting;

    gtk->paint_arrow = &gtk2_paint_arrow;
    gtk->paint_box = &gtk2_paint_box;
    gtk->paint_box_gap = &gtk2_paint_box_gap;
    gtk->paint_expander = &gtk2_paint_expander;
    gtk->paint_extension = &gtk2_paint_extension;
    gtk->paint_flat_box = &gtk2_paint_flat_box;
    gtk->paint_focus = &gtk2_paint_focus;
    gtk->paint_handle = &gtk2_paint_handle;
    gtk->paint_hline = &gtk2_paint_hline;
    gtk->paint_vline = &gtk2_paint_vline;
    gtk->paint_option = &gtk2_paint_option;
    gtk->paint_shadow = &gtk2_paint_shadow;
    gtk->paint_slider = &gtk2_paint_slider;
    gtk->paint_background = &gtk_paint_background;
    gtk->paint_check = &gtk2_paint_check;
    gtk->set_range_value = &gtk2_set_range_value;

    gtk->init_painting = &gtk2_init_painting;
    gtk->copy_image = &gtk2_copy_image;

    gtk->get_xthickness = &gtk2_get_xthickness;
    gtk->get_ythickness = &gtk2_get_ythickness;
    gtk->get_color_for_state = &gtk2_get_color_for_state;
    gtk->get_class_value = &gtk2_get_class_value;

    gtk->get_pango_font_name = &gtk2_get_pango_font_name;
    gtk->get_icon_data = &gtk2_get_icon_data;
    gtk->get_file_icon_data = &gtk2_get_file_icon_data;
    gtk->gdk_threads_enter = fp_gdk_threads_enter;
    gtk->gdk_threads_leave = fp_gdk_threads_leave;
    gtk->gtk_show_uri = fp_gtk_show_uri;
    gtk->get_drawable_data = &gtk2_get_drawable_data;
    gtk->g_free = fp_g_free;

    gtk->gtk_file_chooser_get_filename = fp_gtk_file_chooser_get_filename;
    gtk->gtk_widget_hide = fp_gtk_widget_hide;
    gtk->gtk_main_quit = fp_gtk_main_quit;
    gtk->gtk_file_chooser_dialog_new = fp_gtk_file_chooser_dialog_new;
    gtk->gtk_file_chooser_set_current_folder =
                          fp_gtk_file_chooser_set_current_folder;
    gtk->gtk_file_chooser_set_filename = fp_gtk_file_chooser_set_filename;
    gtk->gtk_file_chooser_set_current_name =
                          fp_gtk_file_chooser_set_current_name;
    gtk->gtk_file_filter_add_custom = fp_gtk_file_filter_add_custom;
    gtk->gtk_file_chooser_set_filter = fp_gtk_file_chooser_set_filter;
    gtk->gtk_file_chooser_get_type = fp_gtk_file_chooser_get_type;
    gtk->gtk_file_filter_new = fp_gtk_file_filter_new;
    gtk->gtk_file_chooser_set_do_overwrite_confirmation =
                          fp_gtk_file_chooser_set_do_overwrite_confirmation;
    gtk->gtk_file_chooser_set_select_multiple =
                          fp_gtk_file_chooser_set_select_multiple;
    gtk->gtk_file_chooser_get_current_folder =
                          fp_gtk_file_chooser_get_current_folder;
    gtk->gtk_file_chooser_get_filenames = fp_gtk_file_chooser_get_filenames;
    gtk->gtk_g_slist_length = fp_gtk_g_slist_length;
    gtk->g_signal_connect_data = fp_g_signal_connect_data;
    gtk->gtk_widget_show = fp_gtk_widget_show;
    gtk->gtk_main = fp_gtk_main;
    gtk->gtk_main_level = fp_gtk_main_level;
    gtk->g_path_get_dirname = fp_g_path_get_dirname;
    gtk->gdk_x11_drawable_get_xid = fp_gdk_x11_drawable_get_xid;
    gtk->gtk_widget_destroy = fp_gtk_widget_destroy;
    gtk->gtk_window_present = fp_gtk_window_present;
    gtk->gtk_window_move = fp_gtk_window_move;
    gtk->gtk_window_resize = fp_gtk_window_resize;
    gtk->get_window = &gtk2_get_window;

    gtk->g_object_unref = fp_g_object_unref;
    gtk->g_list_append = fp_g_list_append;
    gtk->g_list_free = fp_g_list_free;
    gtk->g_list_free_full = fp_g_list_free_full;
}
