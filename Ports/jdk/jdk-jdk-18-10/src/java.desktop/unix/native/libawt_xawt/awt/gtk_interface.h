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

#ifndef _GTK_INTERFACE_H
#define _GTK_INTERFACE_H

#include <X11/X.h>
#include <jni.h>

#ifndef FALSE
#define FALSE           (0)
#define TRUE            (!FALSE)
#endif

#define GTHREAD_LIB_VERSIONED VERSIONED_JNI_LIB_NAME("gthread-2.0", "0")
#define GTHREAD_LIB JNI_LIB_NAME("gthread-2.0")

#define _G_TYPE_CIC(ip, gt, ct)       ((ct*) ip)
#define G_TYPE_CHECK_INSTANCE_CAST(instance, g_type, c_type)  \
                                    (_G_TYPE_CIC ((instance), (g_type), c_type))
#define GTK_TYPE_FILE_CHOOSER             (fp_gtk_file_chooser_get_type ())
#define GTK_FILE_CHOOSER(obj) \
     (G_TYPE_CHECK_INSTANCE_CAST ((obj), GTK_TYPE_FILE_CHOOSER, GtkFileChooser))
#define G_CALLBACK(f) ((GCallback) (f))
#define G_TYPE_FUNDAMENTAL_SHIFT (2)
#define G_TYPE_MAKE_FUNDAMENTAL(x) ((GType) ((x) << G_TYPE_FUNDAMENTAL_SHIFT))
#define G_TYPE_OBJECT G_TYPE_MAKE_FUNDAMENTAL (20)
#define GTK_STOCK_CANCEL           "gtk-cancel"
#define GTK_STOCK_SAVE             "gtk-save"
#define GTK_STOCK_OPEN             "gtk-open"
#define GDK_CURRENT_TIME           0L

#define G_TYPE_INVALID                  G_TYPE_MAKE_FUNDAMENTAL (0)
#define G_TYPE_NONE                     G_TYPE_MAKE_FUNDAMENTAL (1)
#define G_TYPE_INTERFACE                G_TYPE_MAKE_FUNDAMENTAL (2)
#define G_TYPE_CHAR                     G_TYPE_MAKE_FUNDAMENTAL (3)
#define G_TYPE_UCHAR                    G_TYPE_MAKE_FUNDAMENTAL (4)
#define G_TYPE_BOOLEAN                  G_TYPE_MAKE_FUNDAMENTAL (5)
#define G_TYPE_INT                      G_TYPE_MAKE_FUNDAMENTAL (6)
#define G_TYPE_UINT                     G_TYPE_MAKE_FUNDAMENTAL (7)
#define G_TYPE_LONG                     G_TYPE_MAKE_FUNDAMENTAL (8)
#define G_TYPE_ULONG                    G_TYPE_MAKE_FUNDAMENTAL (9)
#define G_TYPE_INT64                    G_TYPE_MAKE_FUNDAMENTAL (10)
#define G_TYPE_UINT64                   G_TYPE_MAKE_FUNDAMENTAL (11)
#define G_TYPE_ENUM                     G_TYPE_MAKE_FUNDAMENTAL (12)
#define G_TYPE_FLAGS                    G_TYPE_MAKE_FUNDAMENTAL (13)
#define G_TYPE_FLOAT                    G_TYPE_MAKE_FUNDAMENTAL (14)
#define G_TYPE_DOUBLE                   G_TYPE_MAKE_FUNDAMENTAL (15)
#define G_TYPE_STRING                   G_TYPE_MAKE_FUNDAMENTAL (16)
#define G_TYPE_POINTER                  G_TYPE_MAKE_FUNDAMENTAL (17)
#define G_TYPE_BOXED                    G_TYPE_MAKE_FUNDAMENTAL (18)
#define G_TYPE_PARAM                    G_TYPE_MAKE_FUNDAMENTAL (19)
#define G_TYPE_OBJECT                   G_TYPE_MAKE_FUNDAMENTAL (20)

#define GTK_TYPE_BORDER                 ((*fp_gtk_border_get_type)())

#define G_TYPE_FUNDAMENTAL_SHIFT        (2)
#define G_TYPE_MAKE_FUNDAMENTAL(x)      ((GType) ((x) << G_TYPE_FUNDAMENTAL_SHIFT))

#ifndef MIN
#define MIN(a, b)  (((a) < (b)) ? (a) : (b))
#endif

#define CONV_BUFFER_SIZE 128
#define NO_SYMBOL_EXCEPTION 1

/* basic types */
typedef char    gchar;
typedef short   gshort;
typedef int     gint;
typedef long    glong;
typedef float   gfloat;
typedef double  gdouble;
typedef void*   gpointer;
typedef gint    gboolean;
typedef signed char  gint8;
typedef signed short gint16;
typedef signed int   gint32;
typedef unsigned char  guchar;
typedef unsigned char  guint8;
typedef unsigned short gushort;
typedef unsigned short guint16;
typedef unsigned int   guint;
typedef unsigned int   guint32;
typedef unsigned int   gsize;
typedef unsigned long  gulong;
typedef signed long long   gint64;
typedef unsigned long long guint64;
typedef gulong GType;

typedef struct _GList GList;
struct _GList
{
  gpointer data;
  GList *next;
  GList *prev;
};

typedef struct _GSList GSList;
struct _GSList {
  gpointer data;
  GSList *next;
};

typedef enum {
    BUTTON,                     /* GtkButton */
    CHECK_BOX,                  /* GtkCheckButton */
    CHECK_BOX_MENU_ITEM,        /* GtkCheckMenuItem */
    COLOR_CHOOSER,              /* GtkColorSelectionDialog */
    COMBO_BOX,                  /* GtkComboBox */
    COMBO_BOX_ARROW_BUTTON,     /* GtkComboBoxEntry */
    COMBO_BOX_TEXT_FIELD,       /* GtkComboBoxEntry */
    DESKTOP_ICON,               /* GtkLabel */
    DESKTOP_PANE,               /* GtkContainer */
    EDITOR_PANE,                /* GtkTextView */
    FORMATTED_TEXT_FIELD,       /* GtkEntry */
    HANDLE_BOX,                 /* GtkHandleBox */
    HPROGRESS_BAR,              /* GtkProgressBar */
    HSCROLL_BAR,                /* GtkHScrollbar */
    HSCROLL_BAR_BUTTON_LEFT,    /* GtkHScrollbar */
    HSCROLL_BAR_BUTTON_RIGHT,   /* GtkHScrollbar */
    HSCROLL_BAR_TRACK,          /* GtkHScrollbar */
    HSCROLL_BAR_THUMB,          /* GtkHScrollbar */
    HSEPARATOR,                 /* GtkHSeparator */
    HSLIDER,                    /* GtkHScale */
    HSLIDER_TRACK,              /* GtkHScale */
    HSLIDER_THUMB,              /* GtkHScale */
    HSPLIT_PANE_DIVIDER,        /* GtkHPaned */
    INTERNAL_FRAME,             /* GtkWindow */
    INTERNAL_FRAME_TITLE_PANE,  /* GtkLabel */
    IMAGE,                      /* GtkImage */
    LABEL,                      /* GtkLabel */
    LIST,                       /* GtkTreeView */
    MENU,                       /* GtkMenu */
    MENU_BAR,                   /* GtkMenuBar */
    MENU_ITEM,                  /* GtkMenuItem */
    MENU_ITEM_ACCELERATOR,      /* GtkLabel */
    OPTION_PANE,                /* GtkMessageDialog */
    PANEL,                      /* GtkContainer */
    PASSWORD_FIELD,             /* GtkEntry */
    POPUP_MENU,                 /* GtkMenu */
    POPUP_MENU_SEPARATOR,       /* GtkSeparatorMenuItem */
    RADIO_BUTTON,               /* GtkRadioButton */
    RADIO_BUTTON_MENU_ITEM,     /* GtkRadioMenuItem */
    ROOT_PANE,                  /* GtkContainer */
    SCROLL_PANE,                /* GtkScrolledWindow */
    SPINNER,                    /* GtkSpinButton */
    SPINNER_ARROW_BUTTON,       /* GtkSpinButton */
    SPINNER_TEXT_FIELD,         /* GtkSpinButton */
    SPLIT_PANE,                 /* GtkPaned */
    TABBED_PANE,                /* GtkNotebook */
    TABBED_PANE_TAB_AREA,       /* GtkNotebook */
    TABBED_PANE_CONTENT,        /* GtkNotebook */
    TABBED_PANE_TAB,            /* GtkNotebook */
    TABLE,                      /* GtkTreeView */
    TABLE_HEADER,               /* GtkButton */
    TEXT_AREA,                  /* GtkTextView */
    TEXT_FIELD,                 /* GtkEntry */
    TEXT_PANE,                  /* GtkTextView */
    TITLED_BORDER,              /* GtkFrame */
    TOGGLE_BUTTON,              /* GtkToggleButton */
    TOOL_BAR,                   /* GtkToolbar */
    TOOL_BAR_DRAG_WINDOW,       /* GtkToolbar */
    TOOL_BAR_SEPARATOR,         /* GtkSeparatorToolItem */
    TOOL_TIP,                   /* GtkWindow */
    TREE,                       /* GtkTreeView */
    TREE_CELL,                  /* GtkTreeView */
    VIEWPORT,                   /* GtkViewport */
    VPROGRESS_BAR,              /* GtkProgressBar */
    VSCROLL_BAR,                /* GtkVScrollbar */
    VSCROLL_BAR_BUTTON_UP,      /* GtkVScrollbar */
    VSCROLL_BAR_BUTTON_DOWN,    /* GtkVScrollbar */
    VSCROLL_BAR_TRACK,          /* GtkVScrollbar */
    VSCROLL_BAR_THUMB,          /* GtkVScrollbar */
    VSEPARATOR,                 /* GtkVSeparator */
    VSLIDER,                    /* GtkVScale */
    VSLIDER_TRACK,              /* GtkVScale */
    VSLIDER_THUMB,              /* GtkVScale */
    VSPLIT_PANE_DIVIDER,        /* GtkVPaned */
    WIDGET_TYPE_SIZE
} WidgetType;

typedef enum
{
    _GTK_ARROW_TYPE,
    _GTK_BUTTON_TYPE,
    _GTK_CHECK_BUTTON_TYPE,
    _GTK_CHECK_MENU_ITEM_TYPE,
    _GTK_COLOR_SELECTION_DIALOG_TYPE,
    _GTK_COMBO_BOX_TYPE,
    _GTK_COMBO_BOX_ARROW_BUTTON_TYPE,
    _GTK_COMBO_BOX_TEXT_FIELD_TYPE,
    _GTK_CONTAINER_TYPE,
    _GTK_ENTRY_TYPE,
    _GTK_FRAME_TYPE,
    _GTK_HANDLE_BOX_TYPE,
    _GTK_HPANED_TYPE,
    _GTK_HPROGRESS_BAR_TYPE,
    _GTK_HSCALE_TYPE,
    _GTK_HSCROLLBAR_TYPE,
    _GTK_HSEPARATOR_TYPE,
    _GTK_IMAGE_TYPE,
    _GTK_MENU_TYPE,
    _GTK_MENU_BAR_TYPE,
    _GTK_MENU_ITEM_TYPE,
    _GTK_NOTEBOOK_TYPE,
    _GTK_LABEL_TYPE,
    _GTK_RADIO_BUTTON_TYPE,
    _GTK_RADIO_MENU_ITEM_TYPE,
    _GTK_SCROLLED_WINDOW_TYPE,
    _GTK_SEPARATOR_MENU_ITEM_TYPE,
    _GTK_SEPARATOR_TOOL_ITEM_TYPE,
    _GTK_SPIN_BUTTON_TYPE,
    _GTK_TEXT_VIEW_TYPE,
    _GTK_TOGGLE_BUTTON_TYPE,
    _GTK_TOOLBAR_TYPE,
    _GTK_TOOLTIP_TYPE,
    _GTK_TREE_VIEW_TYPE,
    _GTK_VIEWPORT_TYPE,
    _GTK_VPANED_TYPE,
    _GTK_VPROGRESS_BAR_TYPE,
    _GTK_VSCALE_TYPE,
    _GTK_VSCROLLBAR_TYPE,
    _GTK_VSEPARATOR_TYPE,
    _GTK_WINDOW_TYPE,
    _GTK_DIALOG_TYPE,
    _GTK_WIDGET_TYPE_SIZE
} GtkWidgetType;

typedef enum
{
  GTK_STATE_NORMAL,
  GTK_STATE_ACTIVE,
  GTK_STATE_PRELIGHT,
  GTK_STATE_SELECTED,
  GTK_STATE_INSENSITIVE,
  GTK_STATE_INCONSISTENT,
  GTK_STATE_FOCUSED
} GtkStateType;

typedef enum
{
  GTK_SHADOW_NONE,
  GTK_SHADOW_IN,
  GTK_SHADOW_OUT,
  GTK_SHADOW_ETCHED_IN,
  GTK_SHADOW_ETCHED_OUT
} GtkShadowType;

typedef enum
{
  GTK_EXPANDER_COLLAPSED,
  GTK_EXPANDER_SEMI_COLLAPSED,
  GTK_EXPANDER_SEMI_EXPANDED,
  GTK_EXPANDER_EXPANDED
} GtkExpanderStyle;

typedef enum
{
  GTK_ICON_SIZE_INVALID,
  GTK_ICON_SIZE_MENU,
  GTK_ICON_SIZE_SMALL_TOOLBAR,
  GTK_ICON_SIZE_LARGE_TOOLBAR,
  GTK_ICON_SIZE_BUTTON,
  GTK_ICON_SIZE_DND,
  GTK_ICON_SIZE_DIALOG
} GtkIconSize;

typedef enum
{
  GTK_ORIENTATION_HORIZONTAL,
  GTK_ORIENTATION_VERTICAL
} GtkOrientation;

typedef enum
{
    FOREGROUND,
    BACKGROUND,
    TEXT_FOREGROUND,
    TEXT_BACKGROUND,
    FOCUS,
    LIGHT,
    DARK,
    MID,
    BLACK,
    WHITE
} ColorType;

typedef enum
{
    GTK_FONT_NAME,
    GTK_ICON_SIZES,
    GTK_CURSOR_BLINK,
    GTK_CURSOR_BLINK_TIME
} Setting;

typedef enum
{
  GTK_ARROW_UP,
  GTK_ARROW_DOWN,
  GTK_ARROW_LEFT,
  GTK_ARROW_RIGHT,
  GTK_ARROW_NONE
} GtkArrowType;

typedef enum
{
  GTK_TEXT_DIR_NONE,
  GTK_TEXT_DIR_LTR,
  GTK_TEXT_DIR_RTL
} GtkTextDirection;

typedef enum
{
  GTK_POS_LEFT,
  GTK_POS_RIGHT,
  GTK_POS_TOP,
  GTK_POS_BOTTOM
} GtkPositionType;

/* SynthConstants */
static const gint ENABLED    = 1 << 0;
static const gint MOUSE_OVER = 1 << 1;
static const gint PRESSED    = 1 << 2;
static const gint DISABLED   = 1 << 3;
static const gint FOCUSED    = 1 << 8;
static const gint SELECTED   = 1 << 9;
static const gint DEFAULT    = 1 << 10;

typedef enum
{
  GTK_ANY,
  GTK_1,
  GTK_2,
  GTK_3
} GtkVersion;

//------------------------------



typedef enum {
  GTK_RESPONSE_NONE = -1,
  GTK_RESPONSE_REJECT = -2,
  GTK_RESPONSE_ACCEPT = -3,
  GTK_RESPONSE_DELETE_EVENT = -4,
  GTK_RESPONSE_OK = -5,
  GTK_RESPONSE_CANCEL = -6,
  GTK_RESPONSE_CLOSE = -7,
  GTK_RESPONSE_YES = -8,
  GTK_RESPONSE_NO = -9,
  GTK_RESPONSE_APPLY = -10,
  GTK_RESPONSE_HELP = -11
} GtkResponseType;

typedef enum {
  GTK_FILE_CHOOSER_ACTION_OPEN,
  GTK_FILE_CHOOSER_ACTION_SAVE,
  GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
  GTK_FILE_CHOOSER_ACTION_CREATE_FOLDER
} GtkFileChooserAction;

typedef enum {
  GTK_FILE_FILTER_FILENAME = 1 << 0,
  GTK_FILE_FILTER_URI = 1 << 1,
  GTK_FILE_FILTER_DISPLAY_NAME = 1 << 2,
  GTK_FILE_FILTER_MIME_TYPE = 1 << 3
} GtkFileFilterFlags;

typedef enum {
  GDK_COLORSPACE_RGB
} GdkColorspace;

typedef enum {
    GDK_INTERP_NEAREST,
    GDK_INTERP_TILES,
    GDK_INTERP_BILINEAR,
    GDK_INTERP_HYPER
} GdkInterpType;

typedef enum {
  G_CONNECT_AFTER = 1 << 0, G_CONNECT_SWAPPED = 1 << 1
} GConnectFlags;
//------------------------------


typedef void GError;
typedef void GdkScreen;
typedef void GtkWindow;
typedef void GdkWindow;
typedef void GClosure;
typedef void GtkFileChooser;
typedef void GtkFileFilter;
typedef struct {
    GtkFileFilterFlags contains;
    const gchar *filename;
    const gchar *uri;
    const gchar *display_name;
    const gchar *mime_type;
} GtkFileFilterInfo;
typedef gboolean (*GtkFileFilterFunc)(const GtkFileFilterInfo *filter_info,
                                                                 gpointer data);
typedef void (*GClosureNotify)(gpointer data, GClosure *closure);
typedef void (*GDestroyNotify)(gpointer data);
typedef void (*GCallback)(void);


typedef struct GtkApi {
    int version;
    gboolean (*show_uri_load)(JNIEnv *env);
    gboolean (*unload)();
    void (*flush_event_loop)();
    gchar* (*gtk_check_version)(guint required_major, guint required_minor,
                                guint required_micro);
    jobject (*get_setting)(JNIEnv *env, Setting property);

    void (*paint_arrow)(WidgetType widget_type, GtkStateType state_type,
        GtkShadowType shadow_type, const gchar *detail,
        gint x, gint y, gint width, gint height,
        GtkArrowType arrow_type, gboolean fill);
    void (*paint_box)(WidgetType widget_type, GtkStateType state_type,
                        GtkShadowType shadow_type, const gchar *detail,
                        gint x, gint y, gint width, gint height,
                        gint synth_state, GtkTextDirection dir);
    void (*paint_box_gap)(WidgetType widget_type, GtkStateType state_type,
            GtkShadowType shadow_type, const gchar *detail,
            gint x, gint y, gint width, gint height,
            GtkPositionType gap_side, gint gap_x, gint gap_width);
    void (*paint_expander)(WidgetType widget_type, GtkStateType state_type,
            const gchar *detail, gint x, gint y, gint width, gint height,
            GtkExpanderStyle expander_style);
    void (*paint_extension)(WidgetType widget_type, GtkStateType state_type,
            GtkShadowType shadow_type, const gchar *detail,
            gint x, gint y, gint width, gint height, GtkPositionType gap_side);
    void (*paint_flat_box)(WidgetType widget_type, GtkStateType state_type,
            GtkShadowType shadow_type, const gchar *detail,
            gint x, gint y, gint width, gint height, gboolean has_focus);
    void (*paint_focus)(WidgetType widget_type, GtkStateType state_type,
            const char *detail, gint x, gint y, gint width, gint height);
    void (*paint_handle)(WidgetType widget_type, GtkStateType state_type,
           GtkShadowType shadow_type, const gchar *detail,
           gint x, gint y, gint width, gint height, GtkOrientation orientation);
    void (*paint_hline)(WidgetType widget_type, GtkStateType state_type,
            const gchar *detail, gint x, gint y, gint width, gint height);
    void (*paint_vline)(WidgetType widget_type, GtkStateType state_type,
            const gchar *detail, gint x, gint y, gint width, gint height);
    void (*paint_option)(WidgetType widget_type, gint synth_state,
             const gchar *detail, gint x, gint y, gint width, gint height);
    void (*paint_shadow)(WidgetType widget_type, GtkStateType state_type,
                           GtkShadowType shadow_type, const gchar *detail,
                           gint x, gint y, gint width, gint height,
                           gint synth_state, GtkTextDirection dir);
    void (*paint_slider)(WidgetType widget_type, GtkStateType state_type,
            GtkShadowType shadow_type, const gchar *detail,
            gint x, gint y, gint width, gint height, GtkOrientation orientation,
            gboolean has_focus);
    void (*paint_background)(WidgetType widget_type, GtkStateType state_type,
            gint x, gint y, gint width, gint height);
    void (*paint_check)(WidgetType widget_type, gint synth_state,
        const gchar *detail, gint x, gint y, gint width, gint height);
    void (*set_range_value)(WidgetType widget_type, jdouble value,
                              jdouble min, jdouble max, jdouble visible);

    void (*init_painting)(JNIEnv *env, gint w, gint h);
    gint (*copy_image)(gint *dest, gint width, gint height);

    gint (*get_xthickness)(JNIEnv *env, WidgetType widget_type);
    gint (*get_ythickness)(JNIEnv *env, WidgetType widget_type);
    gint (*get_color_for_state)(JNIEnv *env, WidgetType widget_type,
                                  GtkStateType state_type, ColorType color_type);
    jobject (*get_class_value)(JNIEnv *env, WidgetType widget_type,
                               const char* key);

    jstring (*get_pango_font_name)(JNIEnv *env, WidgetType widget_type);
    jboolean (*get_icon_data)(JNIEnv *env, gint widget_type,
                    const gchar *stock_id, GtkIconSize size,
                    GtkTextDirection direction, const char *detail,
                    jmethodID icon_upcall_method, jobject this);
    jboolean (*get_file_icon_data)(JNIEnv *env, const char *filename,
                    GError **error, jmethodID icon_upcall_method, jobject this);
    void (*gdk_threads_enter)(void);
    void (*gdk_threads_leave)(void);
    gboolean (*gtk_show_uri)(GdkScreen *screen, const gchar *uri,
                                             guint32 timestamp, GError **error);
    gboolean (*get_drawable_data)(JNIEnv *env, jintArray pixelArray,
                                       jint x, jint y, jint width, jint height,
                                       jint jwidth, int dx, int dy, jint scale);
    void (*g_free)(gpointer mem);


    gchar* (*gtk_file_chooser_get_filename)(GtkFileChooser *chooser);
    void (*gtk_widget_hide)(void* widget);
    void (*gtk_main_quit)(void);
    void* (*gtk_file_chooser_dialog_new)(const gchar *title,
        GtkWindow *parent, GtkFileChooserAction action,
        const gchar *first_button_text, ...);
    gboolean (*gtk_file_chooser_set_current_folder)(GtkFileChooser *chooser,
        const gchar *filename);
    gboolean (*gtk_file_chooser_set_filename)(GtkFileChooser *chooser,
        const char *filename);
    void (*gtk_file_chooser_set_current_name)(GtkFileChooser *chooser,
        const gchar *name);
    void (*gtk_file_filter_add_custom)(GtkFileFilter *filter,
        GtkFileFilterFlags needed, GtkFileFilterFunc func, gpointer data,
        GDestroyNotify notify);
    void (*gtk_file_chooser_set_filter)(GtkFileChooser *chooser,
        GtkFileFilter *filter);
    GType (*gtk_file_chooser_get_type)(void);
    GtkFileFilter* (*gtk_file_filter_new)(void);
    void (*gtk_file_chooser_set_do_overwrite_confirmation)(
        GtkFileChooser *chooser, gboolean do_overwrite_confirmation);
    void (*gtk_file_chooser_set_select_multiple)(
        GtkFileChooser *chooser, gboolean select_multiple);
    gchar* (*gtk_file_chooser_get_current_folder)(GtkFileChooser *chooser);
    GSList* (*gtk_file_chooser_get_filenames)(GtkFileChooser *chooser);
    guint (*gtk_g_slist_length)(GSList *list);
    gulong (*g_signal_connect_data)(gpointer instance,
        const gchar *detailed_signal, GCallback c_handler, gpointer data,
        GClosureNotify destroy_data, GConnectFlags connect_flags);
    void (*gtk_widget_show)(void *widget);
    void (*gtk_main)(void);
    guint (*gtk_main_level)(void);
    gchar* (*g_path_get_dirname) (const gchar *file_name);
    XID (*gdk_x11_drawable_get_xid) (void *drawable);
    void (*gtk_widget_destroy)(void *widget);
    void (*gtk_window_present)(void *window);
    void (*gtk_window_move)(void *window, gint x, gint y);
    void (*gtk_window_resize)(void *window, gint width, gint height);
    GdkWindow *(*get_window)(void *widget);

    void (*g_object_unref)(gpointer object);
    GList* (*g_list_append) (GList *list, gpointer data);
    void (*g_list_free) (GList *list);
    void (*g_list_free_full) (GList *list, GDestroyNotify free_func);
} GtkApi;

gboolean gtk_load(JNIEnv *env, GtkVersion version, gboolean verbose);
gboolean gtk_check_version(GtkVersion version);

typedef struct _GThreadFunctions GThreadFunctions;
static gboolean (*fp_g_thread_get_initialized)(void);
static void (*fp_g_thread_init)(GThreadFunctions *vtable);
static void (*fp_gdk_threads_init)(void);
static void (*fp_gdk_threads_enter)(void);
static void (*fp_gdk_threads_leave)(void);

extern GtkApi* gtk;

#endif /* !_GTK_INTERFACE_H */
