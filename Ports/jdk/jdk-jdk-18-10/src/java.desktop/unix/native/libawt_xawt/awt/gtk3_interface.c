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
#include <string.h>
#include "gtk3_interface.h"
#include "java_awt_Transparency.h"
#include "sizecalc.h"
#include <jni_util.h>
#include <stdio.h>
#include "awt.h"

static void *gtk3_libhandle = NULL;
static void *gthread_libhandle = NULL;

static jmp_buf j;

/* Widgets */
static GtkWidget *gtk3_widget = NULL;
static GtkWidget *gtk3_window = NULL;
static GtkFixed  *gtk3_fixed  = NULL;
static GtkStyleProvider *gtk3_css = NULL;

/* Paint system */
static cairo_surface_t *surface = NULL;
static cairo_t *cr = NULL;

static const char ENV_PREFIX[] = "GTK_MODULES=";

static GtkWidget *gtk3_widgets[_GTK_WIDGET_TYPE_SIZE];

static void throw_exception(JNIEnv *env, const char* name, const char* message)
{
    jclass class = (*env)->FindClass(env, name);

    if (class != NULL)
        (*env)->ThrowNew(env, class, message);

    (*env)->DeleteLocalRef(env, class);
}

static void gtk3_add_state(GtkWidget *widget, GtkStateType state) {
    GtkStateType old_state = fp_gtk_widget_get_state(widget);
    fp_gtk_widget_set_state(widget, old_state | state);
}

static void gtk3_remove_state(GtkWidget *widget, GtkStateType state) {
    GtkStateType old_state = fp_gtk_widget_get_state(widget);
    fp_gtk_widget_set_state(widget, old_state & ~state);
}

/* This is a workaround for the bug:
 * http://sourceware.org/bugzilla/show_bug.cgi?id=1814
 * (dlsym/dlopen clears dlerror state)
 * This bug is specific to Linux, but there is no harm in
 * applying this workaround on Solaris as well.
 */
static void* dl_symbol(const char* name)
{
    void* result = dlsym(gtk3_libhandle, name);
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

gboolean gtk3_check(const char* lib_name, gboolean load)
{
    if (gtk3_libhandle != NULL) {
        /* We've already successfully opened the GTK libs, so return true. */
        return TRUE;
    } else {
#ifdef RTLD_NOLOAD
        void *lib = dlopen(lib_name, RTLD_LAZY | RTLD_NOLOAD);
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
        return dlopen(lib_name, RTLD_LAZY | RTLD_LOCAL) != NULL;
    }
}

#define ADD_SUPPORTED_ACTION(actionStr)                                        \
do {                                                                           \
    jfieldID fld_action = (*env)->GetStaticFieldID(env, cls_action, actionStr, \
                                                 "Ljava/awt/Desktop$Action;"); \
    if (!(*env)->ExceptionCheck(env)) {                                        \
        jobject action = (*env)->GetStaticObjectField(env, cls_action,         \
                                                                  fld_action); \
        (*env)->CallBooleanMethod(env, supportedActions, mid_arrayListAdd,     \
                                                                      action); \
    } else {                                                                   \
        (*env)->ExceptionClear(env);                                           \
    }                                                                          \
} while(0);


static void update_supported_actions(JNIEnv *env) {
    GVfs * (*fp_g_vfs_get_default) (void);
    const gchar * const * (*fp_g_vfs_get_supported_uri_schemes) (GVfs * vfs);
    const gchar * const * schemes = NULL;

    jclass cls_action = (*env)->FindClass(env, "java/awt/Desktop$Action");
    CHECK_NULL(cls_action);
    jclass cls_xDesktopPeer = (*env)->
                                     FindClass(env, "sun/awt/X11/XDesktopPeer");
    CHECK_NULL(cls_xDesktopPeer);
    jfieldID fld_supportedActions = (*env)->GetStaticFieldID(env,
                      cls_xDesktopPeer, "supportedActions", "Ljava/util/List;");
    CHECK_NULL(fld_supportedActions);
    jobject supportedActions = (*env)->GetStaticObjectField(env,
                                        cls_xDesktopPeer, fld_supportedActions);

    jclass cls_arrayList = (*env)->FindClass(env, "java/util/ArrayList");
    CHECK_NULL(cls_arrayList);
    jmethodID mid_arrayListAdd = (*env)->GetMethodID(env, cls_arrayList, "add",
                                                       "(Ljava/lang/Object;)Z");
    CHECK_NULL(mid_arrayListAdd);
    jmethodID mid_arrayListClear = (*env)->GetMethodID(env, cls_arrayList,
                                                                "clear", "()V");
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
    fp_g_vfs_get_supported_uri_schemes =
                           dl_symbol("g_vfs_get_supported_uri_schemes");
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
static gboolean gtk3_show_uri_load(JNIEnv *env) {
    gboolean success = FALSE;
    dlerror();
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
    return success;
}

/**
 * Functions for sun_awt_X11_GtkFileDialogPeer.c
 */
static void gtk3_file_chooser_load()
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
    fp_gtk_file_chooser_set_do_overwrite_confirmation = dl_symbol(
                "gtk_file_chooser_set_do_overwrite_confirmation");
    fp_gtk_file_chooser_set_select_multiple = dl_symbol(
            "gtk_file_chooser_set_select_multiple");
    fp_gtk_file_chooser_get_current_folder = dl_symbol(
            "gtk_file_chooser_get_current_folder");
    fp_gtk_file_chooser_get_filenames = dl_symbol(
            "gtk_file_chooser_get_filenames");
    fp_gtk_g_slist_length = dl_symbol("g_slist_length");
    fp_gdk_x11_drawable_get_xid = dl_symbol("gdk_x11_window_get_xid");
}

static void empty() {}

static gboolean gtk3_version_3_10 = TRUE;
static gboolean gtk3_version_3_14 = FALSE;
static gboolean gtk3_version_3_20 = FALSE;

GtkApi* gtk3_load(JNIEnv *env, const char* lib_name)
{
    gboolean result;
    int i;
    int (*handler)();
    int (*io_handler)();
    char *gtk_modules_env;
    gtk3_libhandle = dlopen(lib_name, RTLD_LAZY | RTLD_LOCAL);
    if (gtk3_libhandle == NULL) {
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

        /* GLib */
        fp_glib_check_version = dlsym(gtk3_libhandle, "glib_check_version");
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

        fp_g_object_get = dl_symbol("g_object_get");
        fp_g_object_set = dl_symbol("g_object_set");

        fp_g_str_has_prefix = dl_symbol("g_str_has_prefix");
        fp_g_strsplit = dl_symbol("g_strsplit");
        fp_g_strfreev = dl_symbol("g_strfreev");

        /* GDK */
        fp_gdk_get_default_root_window =
            dl_symbol("gdk_get_default_root_window");

        /* Pixbuf */
        fp_gdk_pixbuf_new = dl_symbol("gdk_pixbuf_new");
        fp_gdk_pixbuf_new_from_file =
                dl_symbol("gdk_pixbuf_new_from_file");
        fp_gdk_pixbuf_get_from_drawable =
                    dl_symbol("gdk_pixbuf_get_from_window");
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

        fp_cairo_image_surface_create = dl_symbol("cairo_image_surface_create");
        fp_cairo_surface_destroy = dl_symbol("cairo_surface_destroy");
        fp_cairo_surface_status = dl_symbol("cairo_surface_status");
        fp_cairo_create = dl_symbol("cairo_create");
        fp_cairo_destroy = dl_symbol("cairo_destroy");
        fp_cairo_status = dl_symbol("cairo_status");
        fp_cairo_fill = dl_symbol("cairo_fill");
        fp_cairo_rectangle = dl_symbol("cairo_rectangle");
        fp_cairo_set_source_rgb = dl_symbol("cairo_set_source_rgb");
        fp_cairo_set_source_rgba = dl_symbol("cairo_set_source_rgba");
        fp_cairo_surface_flush = dl_symbol("cairo_surface_flush");
        fp_cairo_paint = dl_symbol("cairo_paint");
        fp_cairo_clip = dl_symbol("cairo_clip");
        fp_cairo_image_surface_get_data =
                       dl_symbol("cairo_image_surface_get_data");
        fp_cairo_image_surface_get_stride =
                       dl_symbol("cairo_image_surface_get_stride");

        fp_gdk_pixbuf_get_from_surface =
                       dl_symbol("gdk_pixbuf_get_from_surface");

        fp_gtk_widget_get_state = dl_symbol("gtk_widget_get_state");
        fp_gtk_widget_set_state = dl_symbol("gtk_widget_set_state");

        fp_gtk_widget_is_focus = dl_symbol("gtk_widget_is_focus");
        fp_gtk_widget_set_allocation = dl_symbol("gtk_widget_set_allocation");
        fp_gtk_widget_get_parent = dl_symbol("gtk_widget_get_parent");
        fp_gtk_widget_get_window = dl_symbol("gtk_widget_get_window");

        fp_gtk_widget_get_style_context =
                       dl_symbol("gtk_widget_get_style_context");
        fp_gtk_style_context_get_color =
                       dl_symbol("gtk_style_context_get_color");
        fp_gtk_style_context_get_background_color =
                       dl_symbol("gtk_style_context_get_background_color");
        fp_gtk_widget_get_state_flags = dl_symbol("gtk_widget_get_state_flags");
        fp_gtk_style_context_set_state =
                       dl_symbol("gtk_style_context_set_state");
        fp_gtk_style_context_add_class =
                       dl_symbol("gtk_style_context_add_class");
        fp_gtk_style_context_save = dl_symbol("gtk_style_context_save");
        fp_gtk_style_context_restore = dl_symbol("gtk_style_context_restore");
        fp_gtk_render_check = dl_symbol("gtk_render_check");
        fp_gtk_render_option = dl_symbol("gtk_render_option");
        fp_gtk_render_extension = dl_symbol("gtk_render_extension");
        fp_gtk_render_expander = dl_symbol("gtk_render_expander");
        fp_gtk_render_frame_gap = dl_symbol("gtk_render_frame_gap");
        fp_gtk_render_line = dl_symbol("gtk_render_line");
        fp_gtk_widget_render_icon_pixbuf =
                      dl_symbol("gtk_widget_render_icon_pixbuf");
        if (fp_gtk_check_version(3, 10, 0)) {
            gtk3_version_3_10 = FALSE;
        } else {
            fp_gdk_window_create_similar_image_surface =
                       dl_symbol("gdk_window_create_similar_image_surface");
            fp_gdk_window_get_scale_factor =
                    dl_symbol("gdk_window_get_scale_factor");
        }
        gtk3_version_3_14 = !fp_gtk_check_version(3, 14, 0);

        if (!fp_gtk_check_version(3, 20, 0)) {
            gtk3_version_3_20 = TRUE;
            fp_gtk_widget_path_copy = dl_symbol("gtk_widget_path_copy");
            fp_gtk_widget_path_new = dl_symbol("gtk_widget_path_new");
            fp_gtk_widget_path_append_type = dl_symbol("gtk_widget_path_append_type");
            fp_gtk_widget_path_iter_set_object_name = dl_symbol("gtk_widget_path_iter_set_object_name");
            fp_gtk_style_context_set_path = dl_symbol("gtk_style_context_set_path");
            fp_gtk_widget_path_unref = dl_symbol("gtk_widget_path_unref");
            fp_gtk_style_context_get_path = dl_symbol("gtk_style_context_get_path");
            fp_gtk_style_context_new = dl_symbol("gtk_style_context_new");
        }

        fp_gdk_window_create_similar_surface =
                      dl_symbol("gdk_window_create_similar_surface");
        fp_gtk_settings_get_for_screen =
                      dl_symbol("gtk_settings_get_for_screen");
        fp_gtk_widget_get_screen = dl_symbol("gtk_widget_get_screen");
        fp_gtk_css_provider_get_named = dl_symbol("gtk_css_provider_get_named");
        fp_gtk_style_context_add_provider =
                      dl_symbol("gtk_style_context_add_provider");
        fp_gtk_render_frame = dl_symbol("gtk_render_frame");
        fp_gtk_render_focus = dl_symbol("gtk_render_focus");
        fp_gtk_render_handle = dl_symbol("gtk_render_handle");
        fp_gtk_render_arrow = dl_symbol("gtk_render_arrow");

        fp_gtk_style_context_get_property =
                      dl_symbol("gtk_style_context_get_property");
        fp_gtk_scrolled_window_set_shadow_type =
                      dl_symbol("gtk_scrolled_window_set_shadow_type");
        fp_gtk_render_slider = dl_symbol("gtk_render_slider");
        fp_gtk_style_context_get_padding =
                      dl_symbol("gtk_style_context_get_padding");
        fp_gtk_range_set_inverted = dl_symbol("gtk_range_set_inverted");
        fp_gtk_style_context_get_font = dl_symbol("gtk_style_context_get_font");
        fp_gtk_widget_get_allocated_width =
                      dl_symbol("gtk_widget_get_allocated_width");
        fp_gtk_widget_get_allocated_height =
                      dl_symbol("gtk_widget_get_allocated_height");
        fp_gtk_icon_theme_get_default = dl_symbol("gtk_icon_theme_get_default");
        fp_gtk_icon_theme_load_icon = dl_symbol("gtk_icon_theme_load_icon");

        fp_gtk_adjustment_set_lower = dl_symbol("gtk_adjustment_set_lower");
        fp_gtk_adjustment_set_page_increment =
                      dl_symbol("gtk_adjustment_set_page_increment");
        fp_gtk_adjustment_set_page_size =
                      dl_symbol("gtk_adjustment_set_page_size");
        fp_gtk_adjustment_set_step_increment =
                      dl_symbol("gtk_adjustment_set_step_increment");
        fp_gtk_adjustment_set_upper = dl_symbol("gtk_adjustment_set_upper");
        fp_gtk_adjustment_set_value = dl_symbol("gtk_adjustment_set_value");

        fp_gtk_render_activity = dl_symbol("gtk_render_activity");
        fp_gtk_render_background = dl_symbol("gtk_render_background");
        fp_gtk_style_context_has_class =
                      dl_symbol("gtk_style_context_has_class");

        fp_gtk_style_context_set_junction_sides =
                      dl_symbol("gtk_style_context_set_junction_sides");
        fp_gtk_style_context_add_region =
                      dl_symbol("gtk_style_context_add_region");

        fp_gtk_init_check = dl_symbol("gtk_init_check");

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
        fp_gtk_paned_new = dl_symbol("gtk_paned_new");
        fp_gtk_scale_new = dl_symbol("gtk_scale_new");
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
            dl_symbol("gtk_orientable_set_orientation");
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

        fp_gdk_threads_init = dl_symbol("gdk_threads_init");
        fp_gdk_threads_enter = dl_symbol("gdk_threads_enter");
        fp_gdk_threads_leave = dl_symbol("gdk_threads_leave");

        /**
         * Functions for sun_awt_X11_GtkFileDialogPeer.c
         */
        gtk3_file_chooser_load();

        fp_gtk_combo_box_new = dlsym(gtk3_libhandle, "gtk_combo_box_new");
        fp_gtk_combo_box_entry_new = dlsym(gtk3_libhandle,
                                                "gtk_combo_box_new_with_entry");
        fp_gtk_separator_tool_item_new = dlsym(gtk3_libhandle,
                                                 "gtk_separator_tool_item_new");
        fp_g_list_append = dl_symbol("g_list_append");
        fp_g_list_free = dl_symbol("g_list_free");
        fp_g_list_free_full = dl_symbol("g_list_free_full");
    }
    /* Now we have only one kind of exceptions: NO_SYMBOL_EXCEPTION
     * Otherwise we can check the return value of setjmp method.
     */
    else
    {
        dlclose(gtk3_libhandle);
        gtk3_libhandle = NULL;

        dlclose(gthread_libhandle);
        gthread_libhandle = NULL;

        return NULL;
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

    //According the GTK documentation, gdk_threads_init() should be
    //called before gtk_init() or gtk_init_check()
    fp_gdk_threads_init();
    result = (*fp_gtk_init_check)(NULL, NULL);

    XSetErrorHandler(handler);
    XSetIOErrorHandler(io_handler);
    AWT_UNLOCK();

    /* Initialize widget array. */
    for (i = 0; i < _GTK_WIDGET_TYPE_SIZE; i++)
    {
        gtk3_widgets[i] = NULL;
    }
    if (result) {
        GtkApi* gtk = (GtkApi*)malloc(sizeof(GtkApi));
        gtk3_init(gtk);
        return gtk;
    }
    return NULL;
}

static int gtk3_unload()
{
    int i;
    char *gtk3_error;

    if (!gtk3_libhandle)
        return TRUE;

    /* Release painting objects */
    if (surface != NULL) {
        fp_cairo_destroy(cr);
        fp_cairo_surface_destroy(surface);
        surface = NULL;
    }

    if (gtk3_window != NULL) {
        /* Destroying toplevel widget will destroy all contained widgets */
        (*fp_gtk_widget_destroy)(gtk3_window);

        /* Unset some static data so they get reinitialized on next load */
        gtk3_window = NULL;
    }

    dlerror();
    dlclose(gtk3_libhandle);
    dlclose(gthread_libhandle);
    if ((gtk3_error = dlerror()) != NULL)
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
    while((*fp_g_main_context_iteration)(NULL, FALSE));
}

/*
 * Initialize components of containment hierarchy. This creates a GtkFixed
 * inside a GtkWindow. All widgets get realized.
 */
static void init_containers()
{
    if (gtk3_window == NULL)
    {
        gtk3_window = (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
        gtk3_fixed = (GtkFixed *)(*fp_gtk_fixed_new)();
        (*fp_gtk_container_add)((GtkContainer*)gtk3_window,
                                (GtkWidget *)gtk3_fixed);
        (*fp_gtk_widget_realize)(gtk3_window);
        (*fp_gtk_widget_realize)((GtkWidget *)gtk3_fixed);

        GtkSettings* settings = fp_gtk_settings_get_for_screen(
                                         fp_gtk_widget_get_screen(gtk3_window));
        gchar*  strval = NULL;
        fp_g_object_get(settings, "gtk-theme-name", &strval, NULL);
        gtk3_css = fp_gtk_css_provider_get_named(strval, NULL);
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
*/
static void gtk3_init_painting(JNIEnv *env, gint width, gint height)
{
    init_containers();

    if (cr) {
        fp_cairo_destroy(cr);
    }

    if (surface != NULL) {
        /* free old stuff */
        fp_cairo_surface_destroy(surface);

    }

    if (gtk3_version_3_10) {
        surface = fp_gdk_window_create_similar_image_surface(
                           fp_gtk_widget_get_window(gtk3_window),
                                         CAIRO_FORMAT_ARGB32, width, height, 1);
    } else {
        surface = fp_cairo_image_surface_create(CAIRO_FORMAT_ARGB32,
                                                                 width, height);
    }

    cr = fp_cairo_create(surface);
    if (fp_cairo_surface_status(surface) || fp_cairo_status(cr)) {
        JNU_ThrowOutOfMemoryError(env, "The surface size is too big");
    }
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
static gint gtk3_copy_image(gint *dst, gint width, gint height)
{
    gint i, j, r, g, b;
    guchar *data;
    gint stride, padding;

    fp_cairo_surface_flush(surface);
    data = (*fp_cairo_image_surface_get_data)(surface);
    stride = (*fp_cairo_image_surface_get_stride)(surface);
    padding = stride - width * 4;
    if (stride > 0 && padding >= 0) {
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                int r = *data++;
                int g = *data++;
                int b = *data++;
                int a = *data++;
                *dst++ = (a << 24 | b << 16 | g << 8 | r);
            }
            data += padding;
        }
    }
    return java_awt_Transparency_TRANSLUCENT;
}

static void gtk3_set_direction(GtkWidget *widget, GtkTextDirection dir)
{
    /*
     * Some engines (inexplicably) look at the direction of the widget's
     * parent, so we need to set the direction of both the widget and its
     * parent.
     */
    (*fp_gtk_widget_set_direction)(widget, dir);
    GtkWidget* parent = fp_gtk_widget_get_parent(widget);
    if (parent != NULL) {
        fp_gtk_widget_set_direction(parent, dir);
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

static GtkStateFlags get_gtk_state_flags(gint synth_state)
{
    GtkStateFlags flags = 0;

    if ((synth_state & DISABLED) != 0) {
        flags |= GTK_STATE_FLAG_INSENSITIVE;
    }
    if (((synth_state & PRESSED) != 0 || (synth_state & SELECTED) != 0)) {
        flags |= GTK_STATE_FLAG_ACTIVE;
    }
    if ((synth_state & MOUSE_OVER) != 0) {
        flags |= GTK_STATE_FLAG_PRELIGHT;
    }
    if ((synth_state & FOCUSED) != 0) {
        flags |= GTK_STATE_FLAG_FOCUSED;
    }
    return flags;
}

static GtkStateFlags get_gtk_flags(GtkStateType state_type) {
    GtkStateFlags flags = 0;
    switch (state_type)
    {
        case GTK_STATE_PRELIGHT:
          flags |= GTK_STATE_FLAG_PRELIGHT;
          break;
        case GTK_STATE_SELECTED:
          flags |= GTK_STATE_FLAG_SELECTED;
          break;
        case GTK_STATE_INSENSITIVE:
          flags |= GTK_STATE_FLAG_INSENSITIVE;
          break;
        case GTK_STATE_ACTIVE:
          flags |= GTK_STATE_FLAG_ACTIVE;
          break;
        case GTK_STATE_FOCUSED:
          flags |= GTK_STATE_FLAG_FOCUSED;
          break;
        default:
          break;
    }
    return flags;
}

/* GTK shadow_type filter */
static GtkShadowType get_gtk_shadow_type(WidgetType widget_type,
                                                               gint synth_state)
{
    GtkShadowType result = GTK_SHADOW_OUT;

    if ((synth_state & SELECTED) != 0) {
        result = GTK_SHADOW_IN;
    }
    return result;
}


static GtkWidget* gtk3_get_arrow(GtkArrowType arrow_type,
                                                      GtkShadowType shadow_type)
{
    GtkWidget *arrow = NULL;
    if (NULL == gtk3_widgets[_GTK_ARROW_TYPE])
    {
        gtk3_widgets[_GTK_ARROW_TYPE] = (*fp_gtk_arrow_new)(arrow_type,
                                                                   shadow_type);
        (*fp_gtk_container_add)((GtkContainer *)gtk3_fixed,
                                                 gtk3_widgets[_GTK_ARROW_TYPE]);
        (*fp_gtk_widget_realize)(gtk3_widgets[_GTK_ARROW_TYPE]);
    }
    arrow = gtk3_widgets[_GTK_ARROW_TYPE];

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
static GtkWidget *gtk3_get_widget(WidgetType widget_type)
{
    gboolean init_result = FALSE;
    GtkWidget *result = NULL;
    switch (widget_type)
    {
        case BUTTON:
        case TABLE_HEADER:
            if (init_result = (NULL == gtk3_widgets[_GTK_BUTTON_TYPE]))
            {
                gtk3_widgets[_GTK_BUTTON_TYPE] = (*fp_gtk_button_new)();
            }
            result = gtk3_widgets[_GTK_BUTTON_TYPE];
            break;
        case CHECK_BOX:
            if (init_result = (NULL == gtk3_widgets[_GTK_CHECK_BUTTON_TYPE]))
            {
                gtk3_widgets[_GTK_CHECK_BUTTON_TYPE] =
                    (*fp_gtk_check_button_new)();
            }
            result = gtk3_widgets[_GTK_CHECK_BUTTON_TYPE];
            break;
        case CHECK_BOX_MENU_ITEM:
            if (init_result = (NULL == gtk3_widgets[_GTK_CHECK_MENU_ITEM_TYPE]))
            {
                gtk3_widgets[_GTK_CHECK_MENU_ITEM_TYPE] =
                    (*fp_gtk_check_menu_item_new)();
            }
            result = gtk3_widgets[_GTK_CHECK_MENU_ITEM_TYPE];
            break;
        /************************************************************
         *    Creation a dedicated color chooser is dangerous because
         * it deadlocks the EDT
         ************************************************************/
/*        case COLOR_CHOOSER:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE]))
            {
                gtk3_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE] =
                    (*fp_gtk_color_selection_dialog_new)(NULL);
            }
            result = gtk3_widgets[_GTK_COLOR_SELECTION_DIALOG_TYPE];
            break;*/
        case COMBO_BOX:
            if (init_result = (NULL == gtk3_widgets[_GTK_COMBO_BOX_TYPE]))
            {
                gtk3_widgets[_GTK_COMBO_BOX_TYPE] =
                    (*fp_gtk_combo_box_new)();
            }
            result = gtk3_widgets[_GTK_COMBO_BOX_TYPE];
            break;
        case COMBO_BOX_ARROW_BUTTON:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE]))
            {
                gtk3_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE] =
                     (*fp_gtk_toggle_button_new)();
            }
            result = gtk3_widgets[_GTK_COMBO_BOX_ARROW_BUTTON_TYPE];
            break;
        case COMBO_BOX_TEXT_FIELD:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE]))
            {
                result = gtk3_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE] =
                     (*fp_gtk_entry_new)();
            }
            result = gtk3_widgets[_GTK_COMBO_BOX_TEXT_FIELD_TYPE];
            break;
        case DESKTOP_ICON:
        case INTERNAL_FRAME_TITLE_PANE:
        case LABEL:
            if (init_result = (NULL == gtk3_widgets[_GTK_LABEL_TYPE]))
            {
                gtk3_widgets[_GTK_LABEL_TYPE] =
                    (*fp_gtk_label_new)(NULL);
            }
            result = gtk3_widgets[_GTK_LABEL_TYPE];
            break;
        case DESKTOP_PANE:
        case PANEL:
        case ROOT_PANE:
            if (init_result = (NULL == gtk3_widgets[_GTK_CONTAINER_TYPE]))
            {
                /* There is no constructor for a container type.  I've
                 * chosen GtkFixed container since it has a default
                 * constructor.
                 */
                gtk3_widgets[_GTK_CONTAINER_TYPE] =
                    (*fp_gtk_fixed_new)();
            }
            result = gtk3_widgets[_GTK_CONTAINER_TYPE];
            break;
        case EDITOR_PANE:
        case TEXT_AREA:
        case TEXT_PANE:
            if (init_result = (NULL == gtk3_widgets[_GTK_TEXT_VIEW_TYPE]))
            {
                gtk3_widgets[_GTK_TEXT_VIEW_TYPE] =
                    (*fp_gtk_text_view_new)();
            }
            result = gtk3_widgets[_GTK_TEXT_VIEW_TYPE];
            break;
        case FORMATTED_TEXT_FIELD:
        case PASSWORD_FIELD:
        case TEXT_FIELD:
            if (init_result = (NULL == gtk3_widgets[_GTK_ENTRY_TYPE]))
            {
                gtk3_widgets[_GTK_ENTRY_TYPE] =
                    (*fp_gtk_entry_new)();
            }
            result = gtk3_widgets[_GTK_ENTRY_TYPE];
            break;
        case HANDLE_BOX:
            if (init_result = (NULL == gtk3_widgets[_GTK_HANDLE_BOX_TYPE]))
            {
                gtk3_widgets[_GTK_HANDLE_BOX_TYPE] =
                    (*fp_gtk_handle_box_new)();
            }
            result = gtk3_widgets[_GTK_HANDLE_BOX_TYPE];
            break;
        case HSCROLL_BAR:
        case HSCROLL_BAR_BUTTON_LEFT:
        case HSCROLL_BAR_BUTTON_RIGHT:
        case HSCROLL_BAR_TRACK:
        case HSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk3_widgets[_GTK_HSCROLLBAR_TYPE]))
            {
                gtk3_widgets[_GTK_HSCROLLBAR_TYPE] =
                    (*fp_gtk_hscrollbar_new)(create_adjustment());
            }
            result = gtk3_widgets[_GTK_HSCROLLBAR_TYPE];
            break;
        case HSEPARATOR:
            if (init_result = (NULL == gtk3_widgets[_GTK_HSEPARATOR_TYPE]))
            {
                gtk3_widgets[_GTK_HSEPARATOR_TYPE] =
                    (*fp_gtk_hseparator_new)();
            }
            result = gtk3_widgets[_GTK_HSEPARATOR_TYPE];
            break;
        case HSLIDER:
        case HSLIDER_THUMB:
        case HSLIDER_TRACK:
            if (init_result = (NULL == gtk3_widgets[_GTK_HSCALE_TYPE]))
            {
                gtk3_widgets[_GTK_HSCALE_TYPE] =
                    (*fp_gtk_scale_new)(GTK_ORIENTATION_HORIZONTAL, NULL);
            }
            result = gtk3_widgets[_GTK_HSCALE_TYPE];
            break;
        case HSPLIT_PANE_DIVIDER:
        case SPLIT_PANE:
            if (init_result = (NULL == gtk3_widgets[_GTK_HPANED_TYPE]))
            {
                gtk3_widgets[_GTK_HPANED_TYPE] = (*fp_gtk_paned_new)(GTK_ORIENTATION_HORIZONTAL);
            }
            result = gtk3_widgets[_GTK_HPANED_TYPE];
            break;
        case IMAGE:
            if (init_result = (NULL == gtk3_widgets[_GTK_IMAGE_TYPE]))
            {
                gtk3_widgets[_GTK_IMAGE_TYPE] = (*fp_gtk_image_new)();
            }
            result = gtk3_widgets[_GTK_IMAGE_TYPE];
            break;
        case INTERNAL_FRAME:
            if (init_result = (NULL == gtk3_widgets[_GTK_WINDOW_TYPE]))
            {
                gtk3_widgets[_GTK_WINDOW_TYPE] =
                    (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
            }
            result = gtk3_widgets[_GTK_WINDOW_TYPE];
            break;
        case TOOL_TIP:
            if (init_result = (NULL == gtk3_widgets[_GTK_TOOLTIP_TYPE]))
            {
                result = (*fp_gtk_window_new)(GTK_WINDOW_TOPLEVEL);
                gtk3_widgets[_GTK_TOOLTIP_TYPE] = result;
            }
            result = gtk3_widgets[_GTK_TOOLTIP_TYPE];
            break;
        case LIST:
        case TABLE:
        case TREE:
        case TREE_CELL:
            if (init_result = (NULL == gtk3_widgets[_GTK_TREE_VIEW_TYPE]))
            {
                gtk3_widgets[_GTK_TREE_VIEW_TYPE] =
                    (*fp_gtk_tree_view_new)();
            }
            result = gtk3_widgets[_GTK_TREE_VIEW_TYPE];
            break;
        case TITLED_BORDER:
            if (init_result = (NULL == gtk3_widgets[_GTK_FRAME_TYPE]))
            {
                gtk3_widgets[_GTK_FRAME_TYPE] = fp_gtk_frame_new(NULL);
            }
            result = gtk3_widgets[_GTK_FRAME_TYPE];
            break;
        case POPUP_MENU:
            if (init_result = (NULL == gtk3_widgets[_GTK_MENU_TYPE]))
            {
                gtk3_widgets[_GTK_MENU_TYPE] =
                    (*fp_gtk_menu_new)();
            }
            result = gtk3_widgets[_GTK_MENU_TYPE];
            break;
        case MENU:
        case MENU_ITEM:
        case MENU_ITEM_ACCELERATOR:
            if (init_result = (NULL == gtk3_widgets[_GTK_MENU_ITEM_TYPE]))
            {
                gtk3_widgets[_GTK_MENU_ITEM_TYPE] =
                    (*fp_gtk_menu_item_new)();
            }
            result = gtk3_widgets[_GTK_MENU_ITEM_TYPE];
            break;
        case MENU_BAR:
            if (init_result = (NULL == gtk3_widgets[_GTK_MENU_BAR_TYPE]))
            {
                gtk3_widgets[_GTK_MENU_BAR_TYPE] =
                    (*fp_gtk_menu_bar_new)();
            }
            result = gtk3_widgets[_GTK_MENU_BAR_TYPE];
            break;
        case COLOR_CHOOSER:
        case OPTION_PANE:
            if (init_result = (NULL == gtk3_widgets[_GTK_DIALOG_TYPE]))
            {
                gtk3_widgets[_GTK_DIALOG_TYPE] =
                    (*fp_gtk_dialog_new)();
            }
            result = gtk3_widgets[_GTK_DIALOG_TYPE];
            break;
        case POPUP_MENU_SEPARATOR:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE]))
            {
                gtk3_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE] =
                    (*fp_gtk_separator_menu_item_new)();
            }
            result = gtk3_widgets[_GTK_SEPARATOR_MENU_ITEM_TYPE];
            break;
        case HPROGRESS_BAR:
            if (init_result = (NULL == gtk3_widgets[_GTK_HPROGRESS_BAR_TYPE]))
            {
                gtk3_widgets[_GTK_HPROGRESS_BAR_TYPE] =
                    (*fp_gtk_progress_bar_new)();
            }
            result = gtk3_widgets[_GTK_HPROGRESS_BAR_TYPE];
            break;
        case VPROGRESS_BAR:
            if (init_result = (NULL == gtk3_widgets[_GTK_VPROGRESS_BAR_TYPE]))
            {
                gtk3_widgets[_GTK_VPROGRESS_BAR_TYPE] =
                    (*fp_gtk_progress_bar_new)();
                /*
                 * Vertical JProgressBars always go bottom-to-top,
                 * regardless of the ComponentOrientation.
                 */
                (*fp_gtk_progress_bar_set_orientation)(
                    (GtkProgressBar *)gtk3_widgets[_GTK_VPROGRESS_BAR_TYPE],
                    GTK_PROGRESS_BOTTOM_TO_TOP);
            }
            result = gtk3_widgets[_GTK_VPROGRESS_BAR_TYPE];
            break;
        case RADIO_BUTTON:
            if (init_result = (NULL == gtk3_widgets[_GTK_RADIO_BUTTON_TYPE]))
            {
                gtk3_widgets[_GTK_RADIO_BUTTON_TYPE] =
                    (*fp_gtk_radio_button_new)(NULL);
            }
            result = gtk3_widgets[_GTK_RADIO_BUTTON_TYPE];
            break;
        case RADIO_BUTTON_MENU_ITEM:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_RADIO_MENU_ITEM_TYPE]))
            {
                gtk3_widgets[_GTK_RADIO_MENU_ITEM_TYPE] =
                    (*fp_gtk_radio_menu_item_new)(NULL);
            }
            result = gtk3_widgets[_GTK_RADIO_MENU_ITEM_TYPE];
            break;
        case SCROLL_PANE:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_SCROLLED_WINDOW_TYPE]))
            {
                gtk3_widgets[_GTK_SCROLLED_WINDOW_TYPE] =
                    (*fp_gtk_scrolled_window_new)(NULL, NULL);
            }
            result = gtk3_widgets[_GTK_SCROLLED_WINDOW_TYPE];
            break;
        case SPINNER:
        case SPINNER_ARROW_BUTTON:
        case SPINNER_TEXT_FIELD:
            if (init_result = (NULL == gtk3_widgets[_GTK_SPIN_BUTTON_TYPE]))
            {
                result = gtk3_widgets[_GTK_SPIN_BUTTON_TYPE] =
                    (*fp_gtk_spin_button_new)(NULL, 0, 0);
            }
            result = gtk3_widgets[_GTK_SPIN_BUTTON_TYPE];
            break;
        case TABBED_PANE:
        case TABBED_PANE_TAB_AREA:
        case TABBED_PANE_CONTENT:
        case TABBED_PANE_TAB:
            if (init_result = (NULL == gtk3_widgets[_GTK_NOTEBOOK_TYPE]))
            {
                gtk3_widgets[_GTK_NOTEBOOK_TYPE] =
                    (*fp_gtk_notebook_new)(NULL);
            }
            result = gtk3_widgets[_GTK_NOTEBOOK_TYPE];
            break;
        case TOGGLE_BUTTON:
            if (init_result = (NULL == gtk3_widgets[_GTK_TOGGLE_BUTTON_TYPE]))
            {
                gtk3_widgets[_GTK_TOGGLE_BUTTON_TYPE] =
                    (*fp_gtk_toggle_button_new)(NULL);
            }
            result = gtk3_widgets[_GTK_TOGGLE_BUTTON_TYPE];
            break;
        case TOOL_BAR:
        case TOOL_BAR_DRAG_WINDOW:
            if (init_result = (NULL == gtk3_widgets[_GTK_TOOLBAR_TYPE]))
            {
                gtk3_widgets[_GTK_TOOLBAR_TYPE] =
                    (*fp_gtk_toolbar_new)(NULL);
            }
            result = gtk3_widgets[_GTK_TOOLBAR_TYPE];
            break;
        case TOOL_BAR_SEPARATOR:
            if (init_result =
                    (NULL == gtk3_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE]))
            {
                gtk3_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE] =
                    (*fp_gtk_separator_tool_item_new)();
            }
            result = gtk3_widgets[_GTK_SEPARATOR_TOOL_ITEM_TYPE];
            break;
        case VIEWPORT:
            if (init_result = (NULL == gtk3_widgets[_GTK_VIEWPORT_TYPE]))
            {
                GtkAdjustment *adjustment = create_adjustment();
                gtk3_widgets[_GTK_VIEWPORT_TYPE] =
                    (*fp_gtk_viewport_new)(adjustment, adjustment);
            }
            result = gtk3_widgets[_GTK_VIEWPORT_TYPE];
            break;
        case VSCROLL_BAR:
        case VSCROLL_BAR_BUTTON_UP:
        case VSCROLL_BAR_BUTTON_DOWN:
        case VSCROLL_BAR_TRACK:
        case VSCROLL_BAR_THUMB:
            if (init_result = (NULL == gtk3_widgets[_GTK_VSCROLLBAR_TYPE]))
            {
                gtk3_widgets[_GTK_VSCROLLBAR_TYPE] =
                    (*fp_gtk_vscrollbar_new)(create_adjustment());
            }
            result = gtk3_widgets[_GTK_VSCROLLBAR_TYPE];
            break;
        case VSEPARATOR:
            if (init_result = (NULL == gtk3_widgets[_GTK_VSEPARATOR_TYPE]))
            {
                gtk3_widgets[_GTK_VSEPARATOR_TYPE] =
                    (*fp_gtk_vseparator_new)();
            }
            result = gtk3_widgets[_GTK_VSEPARATOR_TYPE];
            break;
        case VSLIDER:
        case VSLIDER_THUMB:
        case VSLIDER_TRACK:
            if (init_result = (NULL == gtk3_widgets[_GTK_VSCALE_TYPE]))
            {
                gtk3_widgets[_GTK_VSCALE_TYPE] =
                    (*fp_gtk_scale_new)(GTK_ORIENTATION_VERTICAL, NULL);
            }
            result = gtk3_widgets[_GTK_VSCALE_TYPE];
            /*
             * Vertical JSliders start at the bottom, while vertical
             * GtkVScale widgets start at the top (by default), so to fix
             * this we set the "inverted" flag to get the Swing behavior.
             */
             fp_gtk_range_set_inverted((GtkRange*)result, TRUE);
            break;
        case VSPLIT_PANE_DIVIDER:
            if (init_result = (NULL == gtk3_widgets[_GTK_VPANED_TYPE]))
            {
                gtk3_widgets[_GTK_VPANED_TYPE] = (*fp_gtk_paned_new)(GTK_ORIENTATION_VERTICAL);
            }
            result = gtk3_widgets[_GTK_VPANED_TYPE];
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
            GtkWidget *menu = gtk3_get_widget(POPUP_MENU);
            (*fp_gtk_menu_shell_append)((GtkMenuShell *)menu, result);
        }
        else if (widget_type == POPUP_MENU)
        {
            GtkWidget *menu_bar = gtk3_get_widget(MENU_BAR);
            GtkWidget *root_menu = (*fp_gtk_menu_item_new)();
            (*fp_gtk_menu_item_set_submenu)((GtkMenuItem*)root_menu, result);
            (*fp_gtk_menu_shell_append)((GtkMenuShell *)menu_bar, root_menu);
        }
        else if (widget_type == COMBO_BOX_TEXT_FIELD )
        {
            GtkWidget* combo = gtk3_get_widget(COMBO_BOX);

            /*
            * We add a regular GtkButton/GtkEntry to a GtkComboBoxEntry
            * in order to trick engines into thinking it's a real combobox
            * arrow button/text field.
            */

            fp_gtk_container_add ((GtkContainer*)(combo), result);
            GtkStyleContext* context = fp_gtk_widget_get_style_context (combo);
            fp_gtk_style_context_add_class (context, "combobox-entry");
            context = fp_gtk_widget_get_style_context (result);
            fp_gtk_style_context_add_class (context, "combobox");
            fp_gtk_style_context_add_class (context, "entry");
        }
        else if (widget_type == COMBO_BOX_ARROW_BUTTON )
        {
            GtkWidget* combo = gtk3_get_widget(COMBO_BOX);
            fp_gtk_widget_set_parent(result, combo);
        }
        else if (widget_type != TOOL_TIP &&
                 widget_type != INTERNAL_FRAME &&
                 widget_type != OPTION_PANE)
        {
            (*fp_gtk_container_add)((GtkContainer *)gtk3_fixed, result);
        }
        (*fp_gtk_widget_realize)(result);
    }
    return result;
}

static void append_element (GtkWidgetPath *path, const gchar *selector)
{
    fp_gtk_widget_path_append_type (path, G_TYPE_NONE);
    fp_gtk_widget_path_iter_set_object_name (path, -1, selector);
}

static GtkWidgetPath* createWidgetPath(const GtkWidgetPath* path) {
    if (path == NULL) {
        return fp_gtk_widget_path_new();
    } else {
        return fp_gtk_widget_path_copy(path);
    }
}

static GtkStyleContext* get_style(WidgetType widget_type, const gchar *detail)
{
    if (!gtk3_version_3_20) {
        gtk3_widget = gtk3_get_widget(widget_type);
        GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
        fp_gtk_style_context_save (context);
        if (detail != 0) {
             transform_detail_string(detail, context);
        }
        return context;
    } else {
        gtk3_widget = gtk3_get_widget(widget_type);
        GtkStyleContext* widget_context = fp_gtk_widget_get_style_context (gtk3_widget);
        GtkWidgetPath *path = NULL;
        if (detail != 0) {
            if (strcmp(detail, "checkbutton") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "check");
            } else if (strcmp(detail, "radiobutton") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "radio");
            } else if (strcmp(detail, "vscale") == 0 || strcmp(detail, "hscale") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "slider");
            } else if (strcmp(detail, "trough") == 0) {
                //This is a fast solution to the scrollbar trough not being rendered properly
                if (widget_type == HSCROLL_BAR || widget_type == HSCROLL_BAR_TRACK ||
                    widget_type == VSCROLL_BAR || widget_type == VSCROLL_BAR_TRACK) {
                    path = createWidgetPath (NULL);
                } else {
                    path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                }
                append_element(path, detail);
            } else if (strcmp(detail, "bar") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "trough");
                append_element(path, "progress");
            } else if (strcmp(detail, "vscrollbar") == 0 || strcmp(detail, "hscrollbar") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "button");
            } else if (strcmp(detail, "check") == 0) {
                path = createWidgetPath (NULL);
                append_element(path, detail);
            } else if (strcmp(detail, "option") == 0) {
                path = createWidgetPath (NULL);
                append_element(path, "radio");
            } else if (strcmp(detail, "paned") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "paned");
                append_element(path, "separator");
            } else if (strcmp(detail, "spinbutton_down") == 0 || strcmp(detail, "spinbutton_up") == 0) {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, "spinbutton");
                append_element(path, "button");
            } else {
                path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
                append_element(path, detail);
            }
        } else {
            path = createWidgetPath (fp_gtk_style_context_get_path (widget_context));
        }

        GtkStyleContext *context = fp_gtk_style_context_new ();
        fp_gtk_style_context_set_path (context, path);
        fp_gtk_widget_path_unref (path);
        return context;
    }
}

static void disposeOrRestoreContext(GtkStyleContext *context)
{
    if (!gtk3_version_3_20) {
        fp_gtk_style_context_restore (context);
    } else {
        fp_g_object_unref (context);
    }
}

static void gtk3_paint_arrow(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkArrowType arrow_type, gboolean fill)
{
    gdouble xx, yy, a = G_PI;
    int s = width;
    gtk3_widget = gtk3_get_arrow(arrow_type, shadow_type);

    switch (widget_type)
    {
        case SPINNER_ARROW_BUTTON:
            s = (int)(0.4 * width + 0.5) + 1;
            if (arrow_type == GTK_ARROW_UP) {
                a = 0;
            } else if (arrow_type == GTK_ARROW_DOWN) {
                a = G_PI;
            }
            break;

        case HSCROLL_BAR_BUTTON_LEFT:
            s = (int)(0.5 * MIN(height, width * 2) + 0.5) + 1;
            a = 3 * G_PI / 2;
            break;

        case HSCROLL_BAR_BUTTON_RIGHT:
            s = (int)(0.5 * MIN(height, width * 2) + 0.5) + 1;
            a = G_PI / 2;
            break;

        case VSCROLL_BAR_BUTTON_UP:
            s = (int)(0.5 * MIN(height * 2, width) + 0.5) + 1;
            a = 0;
            break;

        case VSCROLL_BAR_BUTTON_DOWN:
            s = (int)(0.5 * MIN(height * 2, width) + 0.5) + 1;
            a = G_PI;
            break;

        case COMBO_BOX_ARROW_BUTTON:
            s = (int)(0.3 * height + 0.5) + 1;
            a = G_PI;
            break;

        case TABLE:
            s = (int)(0.8 * height + 0.5) + 1;
            if (arrow_type == GTK_ARROW_UP) {
                a = G_PI;
            } else if (arrow_type == GTK_ARROW_DOWN) {
                a = 0;
            }
            break;

        case MENU_ITEM:
            if (arrow_type == GTK_ARROW_UP) {
                a = G_PI;
            } else if (arrow_type == GTK_ARROW_DOWN) {
                a = 0;
            } else if (arrow_type == GTK_ARROW_RIGHT) {
                a = G_PI / 2;
            } else if (arrow_type == GTK_ARROW_LEFT) {
                a = 3 * G_PI / 2;
            }
            break;

        default:
            if (arrow_type == GTK_ARROW_UP) {
                a = G_PI;
            } else if (arrow_type == GTK_ARROW_DOWN) {
                a = 0;
            } else if (arrow_type == GTK_ARROW_RIGHT) {
                a = G_PI / 2;
            } else if (arrow_type == GTK_ARROW_LEFT) {
                a = 3 * G_PI / 2;
            }
            break;
    }

    if (s < width && s < height) {
        xx = x + (0.5 * (width - s) + 0.5);
        yy = y + (0.5 * (height - s) + 0.5);
    } else {
        xx = x;
        yy = y;
    }

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    fp_gtk_style_context_save (context);


    if (detail != NULL) {
        transform_detail_string(detail, context);
    }

    GtkStateFlags flags = get_gtk_flags(state_type);

    fp_gtk_style_context_set_state (context, flags);

    (*fp_gtk_render_arrow)(context, cr, a, xx, yy, s);

    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_box(WidgetType widget_type, GtkStateType state_type,
                    GtkShadowType shadow_type, const gchar *detail,
                    gint x, gint y, gint width, gint height,
                    gint synth_state, GtkTextDirection dir)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    if (widget_type == HSLIDER_TRACK) {
        /*
         * For horizontal JSliders with right-to-left orientation, we need
         * to set the "inverted" flag to match the native GTK behavior where
         * the foreground highlight is on the right side of the slider thumb.
         * This is needed especially for the ubuntulooks engine, which looks
         * exclusively at the "inverted" flag to determine on which side of
         * the thumb to paint the highlight...
         */
        fp_gtk_range_set_inverted((GtkRange*)gtk3_widget, dir ==
                                                              GTK_TEXT_DIR_RTL);

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
    gtk3_set_direction(gtk3_widget, dir);

    GtkStyleContext* context = get_style(widget_type, detail);

    GtkStateFlags flags = get_gtk_flags(state_type);
    if (shadow_type == GTK_SHADOW_IN && widget_type != COMBO_BOX_ARROW_BUTTON) {
        flags |= GTK_STATE_FLAG_ACTIVE;
    }

    if (synth_state & MOUSE_OVER) {
        flags |= GTK_STATE_FLAG_PRELIGHT;
    }

    if (synth_state & FOCUSED) {
        flags |= GTK_STATE_FLAG_FOCUSED;
    }

    if (synth_state & DEFAULT) {
        fp_gtk_style_context_add_class (context, "default");
    }

    if (fp_gtk_style_context_has_class(context, "trough")) {
        flags |= GTK_STATE_FLAG_BACKDROP;
    }

    fp_gtk_style_context_set_state (context, flags);

    fp_gtk_render_background (context, cr, x, y, width, height);
    if (shadow_type != GTK_SHADOW_NONE) {
        fp_gtk_render_frame(context, cr, x, y, width, height);
    }

    disposeOrRestoreContext(context);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk3_set_direction(gtk3_widget, GTK_TEXT_DIR_LTR);

    //This is a fast solution to the scrollbar trough not being rendered properly
    if ((widget_type == HSCROLL_BAR || widget_type == HSCROLL_BAR_TRACK ||
        widget_type == VSCROLL_BAR || widget_type == VSCROLL_BAR_TRACK) && detail != 0) {
        gtk3_paint_box(widget_type, state_type, shadow_type, NULL,
                    x, y, width, height, synth_state, dir);
    }
}

static void gtk3_paint_box_gap(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkPositionType gap_side, gint gap_x, gint gap_width)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);

    fp_gtk_style_context_save (context);

    GtkStateFlags flags = get_gtk_flags(state_type);
    fp_gtk_style_context_set_state(context, flags);

    if (detail != 0) {
        transform_detail_string(detail, context);
    }
    fp_gtk_render_background(context, cr, x, y, width, height);

    if (shadow_type != GTK_SHADOW_NONE) {
        fp_gtk_render_frame_gap(context, cr, x, y, width, height, gap_side,
                                    (gdouble)gap_x, (gdouble)gap_x + gap_width);
    }
    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_check(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    GtkStyleContext* context = get_style(widget_type, detail);

    GtkStateFlags flags = get_gtk_state_flags(synth_state);
    if (gtk3_version_3_14 && (synth_state & SELECTED)) {
        flags &= ~GTK_STATE_FLAG_SELECTED;
        flags |= GTK_STATE_FLAG_CHECKED;
    }
    fp_gtk_style_context_set_state(context, flags);

    fp_gtk_render_background(context, cr, x, y, width, height);
    fp_gtk_render_frame(context, cr, x, y, width, height);
    fp_gtk_render_check(context, cr, x, y, width, height);
    disposeOrRestoreContext(context);
}


static void gtk3_paint_expander(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height,
        GtkExpanderStyle expander_style)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);

    fp_gtk_style_context_save (context);

    GtkStateFlags flags = get_gtk_flags(state_type);
    if (expander_style == GTK_EXPANDER_EXPANDED) {
        if (gtk3_version_3_14) {
            flags |= GTK_STATE_FLAG_CHECKED;
        } else {
            flags |= GTK_STATE_FLAG_ACTIVE;
        }
    }

    fp_gtk_style_context_set_state(context, flags);

    if (detail != 0) {
        transform_detail_string(detail, context);
    }

    fp_gtk_render_expander (context, cr, x + 2, y + 2, width - 4, height - 4);

    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_extension(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkPositionType gap_side)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);

    fp_gtk_style_context_save (context);

    GtkStateFlags flags = GTK_STATE_FLAG_NORMAL;

    if (state_type == 0) {
        flags = GTK_STATE_FLAG_ACTIVE;
    }

    fp_gtk_style_context_set_state(context, flags);

    if (detail != 0) {
        transform_detail_string(detail, context);
    }
    switch(gap_side) {
      case GTK_POS_LEFT:
        fp_gtk_style_context_add_class(context, "right");
        break;
      case GTK_POS_RIGHT:
        fp_gtk_style_context_add_class(context, "left");
        break;
      case GTK_POS_TOP:
        fp_gtk_style_context_add_class(context, "bottom");
        break;
      case GTK_POS_BOTTOM:
        fp_gtk_style_context_add_class(context, "top");
        break;
      default:
        break;
    }

    fp_gtk_render_extension(context, cr, x, y, width, height, gap_side);

    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_flat_box(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, gboolean has_focus)
{
    if (state_type == GTK_STATE_PRELIGHT &&
        (widget_type == CHECK_BOX || widget_type == RADIO_BUTTON)) {
        return;
    }

    GtkStyleContext* context = NULL;
    if (widget_type == TOOL_TIP) {
        context = get_style(widget_type, detail);
        fp_gtk_style_context_add_class(context, "background");
    } else {
        gtk3_widget = gtk3_get_widget(widget_type);
        context = fp_gtk_widget_get_style_context (gtk3_widget);
        fp_gtk_style_context_save (context);
        if (detail != 0) {
            transform_detail_string(detail, context);
        }
    }

    GtkStateFlags flags = get_gtk_flags(state_type);

    if (has_focus) {
        flags |= GTK_STATE_FLAG_FOCUSED;
    }

    fp_gtk_style_context_set_state (context, flags);

    if (widget_type == COMBO_BOX_TEXT_FIELD) {
        width += height /2;
    }

    fp_gtk_render_background (context, cr, x, y, width, height);
    if (widget_type == TOOL_TIP) {
        disposeOrRestoreContext(context);
    } else {
        fp_gtk_style_context_restore (context);
    }
}

static void gtk3_paint_focus(WidgetType widget_type, GtkStateType state_type,
        const char *detail, gint x, gint y, gint width, gint height)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    fp_gtk_style_context_save (context);

    transform_detail_string(detail, context);
    fp_gtk_render_focus (context, cr, x, y, width, height);

    fp_gtk_style_context_restore (context);

}

static void gtk3_paint_handle(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = get_style(widget_type, detail);

    GtkStateFlags flags = get_gtk_flags(state_type);
    fp_gtk_style_context_set_state(context, GTK_STATE_FLAG_PRELIGHT);

    if (detail != 0 && !(strcmp(detail, "paned") == 0)) {
        transform_detail_string(detail, context);
        fp_gtk_style_context_add_class (context, "handlebox_bin");
    }

    if (!(strcmp(detail, "paned") == 0)) {
        fp_gtk_render_handle(context, cr, x, y, width, height);
        fp_gtk_render_background(context, cr, x, y, width, height);
    } else {
        if (orientation == GTK_ORIENTATION_VERTICAL) {
            fp_gtk_render_handle(context, cr, x+width/2, y, 2, height);
            fp_gtk_render_background(context, cr, x+width/2, y, 2, height);
        } else {
            fp_gtk_render_handle(context, cr, x, y+height/2, width, 2);
            fp_gtk_render_background(context, cr, x, y+height/2, width, 2);
        }
    }

    disposeOrRestoreContext(context);
}

static void gtk3_paint_hline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);

    fp_gtk_style_context_save (context);

    if (detail != 0) {
        transform_detail_string(detail, context);
    }

    fp_gtk_render_line(context, cr, x, y, x + width, y);

    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_vline(WidgetType widget_type, GtkStateType state_type,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
    gtk3_widget = gtk3_get_widget(widget_type);


    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);

    fp_gtk_style_context_save (context);

    if (detail != 0) {
        transform_detail_string(detail, context);
    }

    fp_gtk_render_line(context, cr, x, y, x, y + height);

    fp_gtk_style_context_restore (context);
}

static void gtk3_paint_option(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height)
{
     GtkStyleContext* context = get_style(widget_type, detail);

     GtkStateFlags flags = get_gtk_state_flags(synth_state);
     if (gtk3_version_3_14 && (synth_state & SELECTED)) {
         flags &= ~GTK_STATE_FLAG_SELECTED;
         flags |= GTK_STATE_FLAG_CHECKED;
     }
     fp_gtk_style_context_set_state(context, flags);

     fp_gtk_render_background(context, cr, x, y, width, height);
     fp_gtk_render_frame(context, cr, x, y, width, height);
     fp_gtk_render_option(context, cr, x, y, width, height);
     disposeOrRestoreContext(context);
}

static void gtk3_paint_shadow(WidgetType widget_type, GtkStateType state_type,
                       GtkShadowType shadow_type, const gchar *detail,
                       gint x, gint y, gint width, gint height,
                       gint synth_state, GtkTextDirection dir)
{
    if (shadow_type == GTK_SHADOW_NONE) {
        return;
    }
    gtk3_widget = gtk3_get_widget(widget_type);

    /*
     * Some engines (e.g. clearlooks) will paint the shadow of certain
     * widgets (e.g. COMBO_BOX_TEXT_FIELD) differently depending on the
     * the text direction.
     */
    gtk3_set_direction(gtk3_widget, dir);


    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    fp_gtk_style_context_save (context);

    if (detail) {
        transform_detail_string(detail, context);
    }

    GtkStateFlags flags = get_gtk_flags(state_type);

    if (synth_state & MOUSE_OVER) {
        flags |= GTK_STATE_FLAG_PRELIGHT;
    }

    if (synth_state & FOCUSED) {
        flags |= GTK_STATE_FLAG_FOCUSED;
    }

    fp_gtk_style_context_set_state (context, flags);

    if (widget_type == COMBO_BOX_TEXT_FIELD) {
        width += height / 2;
    }
    fp_gtk_render_frame(context, cr, x, y, width, height);

    fp_gtk_style_context_restore (context);

    /*
     * Reset the text direction to the default value so that we don't
     * accidentally affect other operations and widgets.
     */
    gtk3_set_direction(gtk3_widget, GTK_TEXT_DIR_LTR);
}

static void gtk3_paint_slider(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height, GtkOrientation orientation,
        gboolean has_focus)
{
    GtkStyleContext *context = get_style(widget_type, detail);

    GtkStateFlags flags = get_gtk_flags(state_type);

    if (state_type == GTK_STATE_ACTIVE) {
        flags |= GTK_STATE_FLAG_PRELIGHT;
    }

    if (has_focus) {
        flags |= GTK_STATE_FLAG_FOCUSED;
    }

    fp_gtk_style_context_set_state (context, flags);

    fp_gtk_render_background (context, cr, x, y, width, height);
    fp_gtk_render_frame(context, cr, x, y, width, height);
    (*fp_gtk_render_slider)(context, cr, x, y, width, height, orientation);
    disposeOrRestoreContext(context);
}

static void gtk3_paint_background(WidgetType widget_type,
             GtkStateType state_type, gint x, gint y, gint width, gint height) {
    gtk3_widget = gtk3_get_widget(widget_type);

    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    fp_gtk_style_context_save (context);

    GtkStateFlags flags = get_gtk_flags(state_type);

    fp_gtk_style_context_set_state (context, flags);

    fp_gtk_render_background (context, cr, x, y, width, height);

    fp_gtk_style_context_restore (context);
}

static GdkPixbuf *gtk3_get_stock_icon(gint widget_type, const gchar *stock_id,
        GtkIconSize size, GtkTextDirection direction, const char *detail)
{
    int sz;

    switch(size) {
      case GTK_ICON_SIZE_MENU:
        sz = 16;
        break;
      case GTK_ICON_SIZE_SMALL_TOOLBAR:
        sz = 18;
        break;
      case GTK_ICON_SIZE_LARGE_TOOLBAR:
        sz = 24;
        break;
      case GTK_ICON_SIZE_BUTTON:
        sz = 20;
        break;
      case GTK_ICON_SIZE_DND:
        sz = 32;
        break;
      case GTK_ICON_SIZE_DIALOG:
        sz = 48;
        break;
      default:
        sz = 0;
        break;
    }

    init_containers();
    gtk3_widget = gtk3_get_widget((widget_type < 0) ? IMAGE : widget_type);
    (*fp_gtk_widget_set_direction)(gtk3_widget, direction);
    GtkIconTheme *icon_theme = fp_gtk_icon_theme_get_default();
    GdkPixbuf *result = fp_gtk_icon_theme_load_icon(icon_theme, stock_id, sz,
                                             GTK_ICON_LOOKUP_USE_BUILTIN, NULL);
    return result;
}

static jboolean gtk3_get_pixbuf_data(JNIEnv *env, GdkPixbuf* pixbuf,
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

static jboolean gtk3_get_file_icon_data(JNIEnv *env, const char *filename,
                 GError **error, jmethodID icon_upcall_method, jobject this) {
    GdkPixbuf* pixbuf = fp_gdk_pixbuf_new_from_file(filename, error);
    return gtk3_get_pixbuf_data(env, pixbuf, icon_upcall_method, this);
}

static jboolean gtk3_get_icon_data(JNIEnv *env, gint widget_type,
                              const gchar *stock_id, GtkIconSize size,
                              GtkTextDirection direction, const char *detail,
                              jmethodID icon_upcall_method, jobject this) {
    GdkPixbuf* pixbuf = gtk3_get_stock_icon(widget_type, stock_id, size,
                                       direction, detail);
    return gtk3_get_pixbuf_data(env, pixbuf, icon_upcall_method, this);
}

/*************************************************/
static gint gtk3_get_xthickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk3_widget = gtk3_get_widget(widget_type);
    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    if (context) {
        GtkBorder padding;
        fp_gtk_style_context_get_padding(context, 0, &padding);
        return padding.left + 1;
    }
    return 0;
}

static gint gtk3_get_ythickness(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk3_widget = gtk3_get_widget(widget_type);
    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    if (context) {
        GtkBorder padding;
        fp_gtk_style_context_get_padding(context, 0, &padding);
        return padding.top + 1;
    }
    return 0;
}

/*************************************************/
static guint8 recode_color(gdouble channel)
{
    guint16 result = (guint16)(channel * 65535);
    if (result > 65535) {
        result = 65535;
    }
    return (guint8)( result >> 8);
}

static GtkStateFlags gtk3_get_state_flags(GtkStateType state_type) {
    switch (state_type)
    {
        case GTK_STATE_NORMAL:
            return GTK_STATE_FLAG_NORMAL;
        case GTK_STATE_ACTIVE:
            return GTK_STATE_FLAG_ACTIVE;
        case GTK_STATE_PRELIGHT:
            return GTK_STATE_FLAG_PRELIGHT;
        case GTK_STATE_SELECTED:
            return GTK_STATE_FLAG_SELECTED;
        case GTK_STATE_INSENSITIVE:
            return GTK_STATE_FLAG_INSENSITIVE;
        case GTK_STATE_INCONSISTENT:
            return GTK_STATE_FLAG_INCONSISTENT;
        case GTK_STATE_FOCUSED:
            return GTK_STATE_FLAG_FOCUSED;
    }
    return 0;
}


static void rgb_to_hls (gdouble *r, gdouble *g, gdouble *b) {
  gdouble min;
  gdouble max;
  gdouble red;
  gdouble green;
  gdouble blue;
  gdouble h, l, s;
  gdouble delta;

  red = *r;
  green = *g;
  blue = *b;

  if (red > green)
    {
      if (red > blue)
        max = red;
      else
        max = blue;

      if (green < blue)
        min = green;
      else
        min = blue;
    }
  else
    {
      if (green > blue)
        max = green;
      else
        max = blue;

      if (red < blue)
        min = red;
      else
        min = blue;
    }

  l = (max + min) / 2;
  s = 0;
  h = 0;

  if (max != min)
    {
      if (l <= 0.5)
        s = (max - min) / (max + min);
      else
        s = (max - min) / (2 - max - min);

      delta = max -min;
      if (red == max)
        h = (green - blue) / delta;
      else if (green == max)
        h = 2 + (blue - red) / delta;
      else if (blue == max)
        h = 4 + (red - green) / delta;

      h *= 60;
      if (h < 0.0)
        h += 360;
    }

  *r = h;
  *g = l;
  *b = s;
}

static void hls_to_rgb (gdouble *h, gdouble *l, gdouble *s)
{
  gdouble hue;
  gdouble lightness;
  gdouble saturation;
  gdouble m1, m2;
  gdouble r, g, b;

  lightness = *l;
  saturation = *s;

  if (lightness <= 0.5)
    m2 = lightness * (1 + saturation);
  else
    m2 = lightness + saturation - lightness * saturation;
  m1 = 2 * lightness - m2;

  if (saturation == 0)
    {
      *h = lightness;
      *l = lightness;
      *s = lightness;
    }
  else
    {
      hue = *h + 120;
      while (hue > 360)
        hue -= 360;
      while (hue < 0)
        hue += 360;

      if (hue < 60)
        r = m1 + (m2 - m1) * hue / 60;
      else if (hue < 180)
        r = m2;
      else if (hue < 240)
        r = m1 + (m2 - m1) * (240 - hue) / 60;
      else
        r = m1;

      hue = *h;
      while (hue > 360)
        hue -= 360;
      while (hue < 0)
        hue += 360;

      if (hue < 60)
        g = m1 + (m2 - m1) * hue / 60;
      else if (hue < 180)
        g = m2;
      else if (hue < 240)
        g = m1 + (m2 - m1) * (240 - hue) / 60;
      else
        g = m1;

      hue = *h - 120;
      while (hue > 360)
        hue -= 360;
      while (hue < 0)
        hue += 360;

      if (hue < 60)
        b = m1 + (m2 - m1) * hue / 60;
      else if (hue < 180)
        b = m2;
      else if (hue < 240)
        b = m1 + (m2 - m1) * (240 - hue) / 60;
      else
        b = m1;

      *h = r;
      *l = g;
      *s = b;
    }
}



static void gtk3_style_shade (const GdkRGBA *a, GdkRGBA *b, gdouble k) {
  gdouble red = a->red;
  gdouble green = a->green;
  gdouble blue = a->blue;

  rgb_to_hls (&red, &green, &blue);

  green *= k;
  if (green > 1.0)
    green = 1.0;
  else if (green < 0.0)
    green = 0.0;

  blue *= k;
  if (blue > 1.0)
    blue = 1.0;
  else if (blue < 0.0)
    blue = 0.0;

  hls_to_rgb (&red, &green, &blue);

  b->red = red;
  b->green = green;
  b->blue = blue;
}

static GdkRGBA gtk3_get_color_for_flags(GtkStyleContext* context,
                                  GtkStateFlags flags, ColorType color_type) {
    GdkRGBA c, color;
    color.alpha = 1;

    switch (color_type)
    {
        case FOREGROUND:
        case TEXT_FOREGROUND:
            fp_gtk_style_context_get_color(context, flags, &color);
            break;
        case BACKGROUND:
        case TEXT_BACKGROUND:
            fp_gtk_style_context_get_background_color(context, flags, &color);
            break;
        case LIGHT:
            c = gtk3_get_color_for_flags(context, flags, BACKGROUND);
            gtk3_style_shade(&c, &color, LIGHTNESS_MULT);
            break;
        case DARK:
            c = gtk3_get_color_for_flags(context, flags, BACKGROUND);
            gtk3_style_shade (&c, &color, DARKNESS_MULT);
            break;
        case MID:
            {
                GdkRGBA c1 = gtk3_get_color_for_flags(context, flags, LIGHT);
                GdkRGBA c2 = gtk3_get_color_for_flags(context, flags, DARK);
                color.red = (c1.red + c2.red) / 2;
                color.green = (c1.green + c2.green) / 2;
                color.blue = (c1.blue + c2.blue) / 2;
            }
            break;
        case FOCUS:
        case BLACK:
            color.red = 0;
            color.green = 0;
            color.blue = 0;
            break;
        case WHITE:
            color.red = 1;
            color.green = 1;
            color.blue = 1;
            break;
    }
    return color;
}

static gint gtk3_get_color_for_state(JNIEnv *env, WidgetType widget_type,
                              GtkStateType state_type, ColorType color_type)
{

    gint result = 0;

    GtkStateFlags flags = gtk3_get_state_flags(state_type);

    init_containers();

    if (gtk3_version_3_20) {
        if ((widget_type == TEXT_FIELD || widget_type == PASSWORD_FIELD || widget_type == SPINNER_TEXT_FIELD ||
            widget_type == FORMATTED_TEXT_FIELD) && state_type == GTK_STATE_SELECTED && color_type == TEXT_BACKGROUND) {
            widget_type = TEXT_AREA;
        }
    }

    GtkStyleContext* context = NULL;
    if (widget_type == TOOL_TIP) {
        context = get_style(widget_type, "tooltip");
    } else {
        gtk3_widget = gtk3_get_widget(widget_type);
        context = fp_gtk_widget_get_style_context(gtk3_widget);
    }
    if (widget_type == CHECK_BOX_MENU_ITEM
     || widget_type == RADIO_BUTTON_MENU_ITEM) {
        flags &= GTK_STATE_FLAG_NORMAL | GTK_STATE_FLAG_SELECTED
                  | GTK_STATE_FLAG_INSENSITIVE | GTK_STATE_FLAG_FOCUSED;
    }

    GdkRGBA color = gtk3_get_color_for_flags(context, flags, color_type);

    if (recode_color(color.alpha) == 0) {
        color = gtk3_get_color_for_flags(
        fp_gtk_widget_get_style_context(gtk3_get_widget(INTERNAL_FRAME)),
        0, BACKGROUND);
    }

    result = recode_color(color.alpha) << 24 | recode_color(color.red) << 16 |
             recode_color(color.green) << 8 | recode_color(color.blue);
    if (widget_type == TOOL_TIP) {
        disposeOrRestoreContext(context);
    }
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

static jobject gtk3_get_class_value(JNIEnv *env, WidgetType widget_type,
                                                     const char* key)
{
    init_containers();

    gtk3_widget = gtk3_get_widget(widget_type);

    GValue value = { 0, { { 0 } } };

    GParamSpec* param = (*fp_gtk_widget_class_find_style_property)(
                                    ((GTypeInstance*)gtk3_widget)->g_class, key);
    if ( param )
    {
        (*fp_g_value_init)( &value, param->value_type );
        (*fp_gtk_widget_style_get_property)(gtk3_widget, key, &value);

        if ((*fp_g_type_is_a)( param->value_type, G_TYPE_BOOLEAN ))
        {
            gboolean val = (*fp_g_value_get_boolean)(&value);
            return create_Boolean(env, (jboolean)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_CHAR ))
        {
            gchar val = (*fp_g_value_get_char)(&value);
            return create_Character(env, (jchar)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_UCHAR ))
        {
            guchar val = (*fp_g_value_get_uchar)(&value);
            return create_Character(env, (jchar)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_INT ))
        {
            gint val = (*fp_g_value_get_int)(&value);
            return create_Integer(env, (jint)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_UINT ))
        {
            guint val = (*fp_g_value_get_uint)(&value);
                    return create_Integer(env, (jint)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_LONG ))
        {
            glong val = (*fp_g_value_get_long)(&value);
            return create_Long(env, (jlong)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_ULONG ))
        {
            gulong val = (*fp_g_value_get_ulong)(&value);
            return create_Long(env, (jlong)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_INT64 ))
        {
            gint64 val = (*fp_g_value_get_int64)(&value);
            return create_Long(env, (jlong)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_UINT64 ))
        {
            guint64 val = (*fp_g_value_get_uint64)(&value);
            return create_Long(env, (jlong)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_FLOAT ))
        {
            gfloat val = (*fp_g_value_get_float)(&value);
            return create_Float(env, (jfloat)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_DOUBLE ))
        {
            gdouble val = (*fp_g_value_get_double)(&value);
            return create_Double(env, (jdouble)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_ENUM ))
        {
            gint val = (*fp_g_value_get_enum)(&value);
            return create_Integer(env, (jint)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_FLAGS ))
        {
            guint val = (*fp_g_value_get_flags)(&value);
            return create_Integer(env, (jint)val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, G_TYPE_STRING ))
        {
            const gchar* val = (*fp_g_value_get_string)(&value);

            /* We suppose that all values come in C locale and
             * utf-8 representation of a string is the same as
             * the string itself. If this isn't so we should
             * use g_convert.
             */
            return (*env)->NewStringUTF(env, val);
        }
        else if ((*fp_g_type_is_a)( param->value_type, GTK_TYPE_BORDER ))
        {
            GtkBorder *border = (GtkBorder*)(*fp_g_value_get_boxed)(&value);
            return border ? create_Insets(env, border) : NULL;
        }

        /*      TODO: Other types are not supported yet.*/
/*        else if((*fp_g_type_is_a)( param->value_type, G_TYPE_PARAM ))
        {
            GParamSpec* val = (*fp_g_value_get_param)(&value);
            printf( "Param: %p\n", val );
        }
        else if((*fp_g_type_is_a)( param->value_type, G_TYPE_BOXED ))
        {
            gpointer* val = (*fp_g_value_get_boxed)(&value);
            printf( "Boxed: %p\n", val );
        }
        else if((*fp_g_type_is_a)( param->value_type, G_TYPE_POINTER ))
        {
            gpointer* val = (*fp_g_value_get_pointer)(&value);
            printf( "Pointer: %p\n", val );
        }
        else if((*fp_g_type_is_a)( param->value_type, G_TYPE_OBJECT ))
        {
            GObject* val = (GObject*)(*fp_g_value_get_object)(&value);
            printf( "Object: %p\n", val );
        }*/
    }

    return NULL;
}

static void gtk3_set_range_value(WidgetType widget_type, jdouble value,
                          jdouble min, jdouble max, jdouble visible)
{
    GtkAdjustment *adj;

    gtk3_widget = gtk3_get_widget(widget_type);

    adj = (*fp_gtk_range_get_adjustment)((GtkRange *)gtk3_widget);

    fp_gtk_adjustment_set_value(adj, value);
    fp_gtk_adjustment_set_lower(adj, min);
    fp_gtk_adjustment_set_upper(adj, max);
    fp_gtk_adjustment_set_page_size(adj, visible);
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
    if (class == NULL)
        return NULL; /* can't find/load the class, exception thrown */

    if (*cid == NULL)
    {
        *cid = (*env)->GetMethodID(env, class, "<init>", signature);
        if (*cid == NULL)
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
static jstring gtk3_get_pango_font_name(JNIEnv *env, WidgetType widget_type)
{
    init_containers();

    gtk3_widget = gtk3_get_widget(widget_type);
    jstring  result = NULL;
    GtkStyleContext* context = fp_gtk_widget_get_style_context (gtk3_widget);
    if (context)
    {
        PangoFontDescription* fd = fp_gtk_style_context_get_font(context, 0);
        gchar* val = (*fp_pango_font_description_to_string)(fd);
        result = (*env)->NewStringUTF(env, val);
        (*fp_g_free)( val );
    }

    return result;
}

/***********************************************/
static jobject get_string_property(JNIEnv *env, GtkSettings* settings,
                                                             const gchar* key) {
    jobject result = NULL;
    gchar*  strval = NULL;

    (*fp_g_object_get)(settings, key, &strval, NULL);
    result = (*env)->NewStringUTF(env, strval);
    (*fp_g_free)(strval);

    return result;
}

static jobject get_integer_property(JNIEnv *env, GtkSettings* settings,
                                                             const gchar* key) {
    gint intval = 0;
    (*fp_g_object_get)(settings, key, &intval, NULL);
    return create_Integer(env, intval);
}

static jobject get_boolean_property(JNIEnv *env, GtkSettings* settings,
                                                             const gchar* key) {
    gint intval = 0;
    (*fp_g_object_get)(settings, key, &intval, NULL);
    return create_Boolean(env, intval);
}

static jobject gtk3_get_setting(JNIEnv *env, Setting property)
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

static void transform_detail_string (const gchar *detail,
                                                     GtkStyleContext *context) {
  if (!detail)
    return;

  if (strcmp (detail, "arrow") == 0)
    fp_gtk_style_context_add_class (context, "arrow");
  else if (strcmp (detail, "button") == 0)
    fp_gtk_style_context_add_class (context, "button");
  else if (strcmp (detail, "buttondefault") == 0)
    {
      fp_gtk_style_context_add_class (context, "button");
      fp_gtk_style_context_add_class (context, "default");
    }
  else if (strcmp (detail, "calendar") == 0)
    fp_gtk_style_context_add_class (context, "calendar");
  else if (strcmp (detail, "cellcheck") == 0)
    {
      fp_gtk_style_context_add_class (context, "cell");
      fp_gtk_style_context_add_class (context, "check");
    }
  else if (strcmp (detail, "cellradio") == 0)
    {
      fp_gtk_style_context_add_class (context, "cell");
      fp_gtk_style_context_add_class (context, "radio");
    }
  else if (strcmp (detail, "checkbutton") == 0)
    fp_gtk_style_context_add_class (context, "check");
  else if (strcmp (detail, "check") == 0)
    {
      fp_gtk_style_context_add_class (context, "check");
      fp_gtk_style_context_add_class (context, "menu");
    }
  else if (strcmp (detail, "radiobutton") == 0)
    {
      fp_gtk_style_context_add_class (context, "radio");
    }
  else if (strcmp (detail, "option") == 0)
    {
      fp_gtk_style_context_add_class (context, "radio");
      fp_gtk_style_context_add_class (context, "menu");
    }
  else if (strcmp (detail, "entry") == 0 ||
           strcmp (detail, "entry_bg") == 0)
    fp_gtk_style_context_add_class (context, "entry");
  else if (strcmp (detail, "expander") == 0)
    fp_gtk_style_context_add_class (context, "expander");
  else if (strcmp (detail, "tooltip") == 0)
    fp_gtk_style_context_add_class (context, "tooltip");
  else if (strcmp (detail, "frame") == 0)
    fp_gtk_style_context_add_class (context, "frame");
  else if (strcmp (detail, "scrolled_window") == 0)
    fp_gtk_style_context_add_class (context, "scrolled-window");
  else if (strcmp (detail, "viewport") == 0 ||
           strcmp (detail, "viewportbin") == 0)
    fp_gtk_style_context_add_class (context, "viewport");
  else if (strncmp (detail, "trough", 6) == 0)
    fp_gtk_style_context_add_class (context, "trough");
  else if (strcmp (detail, "spinbutton") == 0)
    fp_gtk_style_context_add_class (context, "spinbutton");
  else if (strcmp (detail, "spinbutton_up") == 0)
    {
      fp_gtk_style_context_add_class (context, "spinbutton");
      fp_gtk_style_context_add_class (context, "button");
      fp_gtk_style_context_set_junction_sides (context, GTK_JUNCTION_BOTTOM);
    }
  else if (strcmp (detail, "spinbutton_down") == 0)
    {
      fp_gtk_style_context_add_class (context, "spinbutton");
      fp_gtk_style_context_add_class (context, "button");
      fp_gtk_style_context_set_junction_sides (context, GTK_JUNCTION_TOP);
    }
  else if ((detail[0] == 'h' || detail[0] == 'v') &&
           strncmp (&detail[1], "scrollbar_", 9) == 0)
    {
      fp_gtk_style_context_add_class (context, "button");
      fp_gtk_style_context_add_class (context, "scrollbar");
    }
  else if (strcmp (detail, "slider") == 0)
    {
      fp_gtk_style_context_add_class (context, "slider");
      fp_gtk_style_context_add_class (context, "scrollbar");
    }
  else if (strcmp (detail, "vscale") == 0 ||
           strcmp (detail, "hscale") == 0)
    {
      fp_gtk_style_context_add_class (context, "slider");
      fp_gtk_style_context_add_class (context, "scale");
    }
  else if (strcmp (detail, "menuitem") == 0)
    {
      fp_gtk_style_context_add_class (context, "menuitem");
      fp_gtk_style_context_add_class (context, "menu");
    }
  else if (strcmp (detail, "menu") == 0)
    {
      fp_gtk_style_context_add_class (context, "popup");
      fp_gtk_style_context_add_class (context, "menu");
    }
  else if (strcmp (detail, "accellabel") == 0)
    fp_gtk_style_context_add_class (context, "accelerator");
  else if (strcmp (detail, "menubar") == 0)
    fp_gtk_style_context_add_class (context, "menubar");
  else if (strcmp (detail, "base") == 0)
    fp_gtk_style_context_add_class (context, "background");
  else if (strcmp (detail, "bar") == 0 ||
           strcmp (detail, "progressbar") == 0)
    fp_gtk_style_context_add_class (context, "progressbar");
  else if (strcmp (detail, "toolbar") == 0)
    fp_gtk_style_context_add_class (context, "toolbar");
  else if (strcmp (detail, "handlebox_bin") == 0)
    fp_gtk_style_context_add_class (context, "dock");
  else if (strcmp (detail, "notebook") == 0)
    fp_gtk_style_context_add_class (context, "notebook");
  else if (strcmp (detail, "tab") == 0)
  {
      fp_gtk_style_context_add_class (context, "notebook");
      fp_gtk_style_context_add_region (context, "tab", 0);
  } else if (strcmp (detail, "paned") == 0) {
      fp_gtk_style_context_add_class (context, "pane-separator");
  }
  else if (fp_g_str_has_prefix (detail, "cell"))
    {
      GtkRegionFlags row, col;
      gboolean ruled = FALSE;
      gchar** tokens;
      guint i;

      tokens = fp_g_strsplit (detail, "_", -1);
      row = col = 0;
      i = 0;

      while (tokens[i])
        {
          if (strcmp (tokens[i], "even") == 0)
            row |= GTK_REGION_EVEN;
          else if (strcmp (tokens[i], "odd") == 0)
            row |= GTK_REGION_ODD;
          else if (strcmp (tokens[i], "start") == 0)
            col |= GTK_REGION_FIRST;
          else if (strcmp (tokens[i], "end") == 0)
            col |= GTK_REGION_LAST;
          else if (strcmp (tokens[i], "ruled") == 0)
            ruled = TRUE;
          else if (strcmp (tokens[i], "sorted") == 0)
            col |= GTK_REGION_SORTED;

          i++;
        }

      if (!ruled)
        row &= ~(GTK_REGION_EVEN | GTK_REGION_ODD);

      fp_gtk_style_context_add_class (context, "cell");
      fp_gtk_style_context_add_region (context, "row", row);
      fp_gtk_style_context_add_region (context, "column", col);

      fp_g_strfreev (tokens);
    }
}

static gboolean gtk3_get_drawable_data(JNIEnv *env, jintArray pixelArray,
     int x, jint y, jint width, jint height, jint jwidth, int dx, int dy,
                                                                   jint scale) {
    GdkPixbuf *pixbuf;
    jint *ary;

    GdkWindow *root = (*fp_gdk_get_default_root_window)();
    if (gtk3_version_3_10) {
        int win_scale = (*fp_gdk_window_get_scale_factor)(root);
        pixbuf = (*fp_gdk_pixbuf_get_from_drawable)(
            root, x, y, (int) (width / (float) win_scale + 0.5), (int) (height / (float) win_scale + 0.5));
    } else {
        pixbuf = (*fp_gdk_pixbuf_get_from_drawable)(root, x, y, width, height);
    }

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
        if ((*fp_gdk_pixbuf_get_width)(pixbuf) >= width
                && (*fp_gdk_pixbuf_get_height)(pixbuf) >= height
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

static GdkWindow* gtk3_get_window(void *widget) {
    return fp_gtk_widget_get_window((GtkWidget*)widget);
}

static void gtk3_init(GtkApi* gtk) {
    gtk->version = GTK_3;

    gtk->show_uri_load = &gtk3_show_uri_load;
    gtk->unload = &gtk3_unload;
    gtk->flush_event_loop = &flush_gtk_event_loop;
    gtk->gtk_check_version = fp_gtk_check_version;
    gtk->get_setting = &gtk3_get_setting;

    gtk->paint_arrow = &gtk3_paint_arrow;
    gtk->paint_box = &gtk3_paint_box;
    gtk->paint_box_gap = &gtk3_paint_box_gap;
    gtk->paint_expander = &gtk3_paint_expander;
    gtk->paint_extension = &gtk3_paint_extension;
    gtk->paint_flat_box = &gtk3_paint_flat_box;
    gtk->paint_focus = &gtk3_paint_focus;
    gtk->paint_handle = &gtk3_paint_handle;
    gtk->paint_hline = &gtk3_paint_hline;
    gtk->paint_vline = &gtk3_paint_vline;
    gtk->paint_option = &gtk3_paint_option;
    gtk->paint_shadow = &gtk3_paint_shadow;
    gtk->paint_slider = &gtk3_paint_slider;
    gtk->paint_background = &gtk3_paint_background;
    gtk->paint_check = &gtk3_paint_check;
    gtk->set_range_value = &gtk3_set_range_value;

    gtk->init_painting = &gtk3_init_painting;
    gtk->copy_image = &gtk3_copy_image;

    gtk->get_xthickness = &gtk3_get_xthickness;
    gtk->get_ythickness = &gtk3_get_ythickness;
    gtk->get_color_for_state = &gtk3_get_color_for_state;
    gtk->get_class_value = &gtk3_get_class_value;

    gtk->get_pango_font_name = &gtk3_get_pango_font_name;
    gtk->get_icon_data = &gtk3_get_icon_data;
    gtk->get_file_icon_data = &gtk3_get_file_icon_data;
    gtk->gdk_threads_enter = fp_gdk_threads_enter;
    gtk->gdk_threads_leave = fp_gdk_threads_leave;
    gtk->gtk_show_uri = fp_gtk_show_uri;
    gtk->get_drawable_data = &gtk3_get_drawable_data;
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
    gtk->get_window = &gtk3_get_window;

    gtk->g_object_unref = fp_g_object_unref;
    gtk->g_list_append = fp_g_list_append;
    gtk->g_list_free = fp_g_list_free;
    gtk->g_list_free_full = fp_g_list_free_full;
}
