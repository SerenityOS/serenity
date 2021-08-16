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

#ifndef _GTK3_INTERFACE_H
#define _GTK3_INTERFACE_H

#include <stdlib.h>
#include <jni.h>
#include <X11/X.h>
#include "gtk_interface.h"

#define LIGHTNESS_MULT  1.3
#define DARKNESS_MULT   0.7

#define G_PI    3.1415926535897932384626433832795028841971693993751

typedef enum
{
  GTK_STATE_FLAG_NORMAL       = 0,
  GTK_STATE_FLAG_ACTIVE       = 1 << 0,
  GTK_STATE_FLAG_PRELIGHT     = 1 << 1,
  GTK_STATE_FLAG_SELECTED     = 1 << 2,
  GTK_STATE_FLAG_INSENSITIVE  = 1 << 3,
  GTK_STATE_FLAG_INCONSISTENT = 1 << 4,
  GTK_STATE_FLAG_FOCUSED      = 1 << 5,
  GTK_STATE_FLAG_BACKDROP     = 1 << 6,
  GTK_STATE_FLAG_DIR_LTR      = 1 << 7,
  GTK_STATE_FLAG_DIR_RTL      = 1 << 8,
  GTK_STATE_FLAG_LINK         = 1 << 9,
  GTK_STATE_FLAG_VISITED      = 1 << 10,
  GTK_STATE_FLAG_CHECKED      = 1 << 11
} GtkStateFlags;

typedef enum {
  GTK_JUNCTION_NONE = 0,
  GTK_JUNCTION_CORNER_TOPLEFT = 1 << 0,
  GTK_JUNCTION_CORNER_TOPRIGHT = 1 << 1,
  GTK_JUNCTION_CORNER_BOTTOMLEFT = 1 << 2,
  GTK_JUNCTION_CORNER_BOTTOMRIGHT = 1 << 3,
  GTK_JUNCTION_TOP =
                   (GTK_JUNCTION_CORNER_TOPLEFT | GTK_JUNCTION_CORNER_TOPRIGHT),
  GTK_JUNCTION_BOTTOM =
             (GTK_JUNCTION_CORNER_BOTTOMLEFT | GTK_JUNCTION_CORNER_BOTTOMRIGHT),
  GTK_JUNCTION_LEFT =
                 (GTK_JUNCTION_CORNER_TOPLEFT | GTK_JUNCTION_CORNER_BOTTOMLEFT),
  GTK_JUNCTION_RIGHT =
               (GTK_JUNCTION_CORNER_TOPRIGHT | GTK_JUNCTION_CORNER_BOTTOMRIGHT)
} GtkJunctionSides;

typedef enum {
  GTK_REGION_EVEN    = 1 << 0,
  GTK_REGION_ODD     = 1 << 1,
  GTK_REGION_FIRST   = 1 << 2,
  GTK_REGION_LAST    = 1 << 3,
  GTK_REGION_ONLY    = 1 << 4,
  GTK_REGION_SORTED  = 1 << 5
} GtkRegionFlags;

typedef enum
{
  GTK_WINDOW_TOPLEVEL,
  GTK_WINDOW_POPUP
} GtkWindowType;

typedef enum
{
  G_PARAM_READABLE            = 1 << 0,
  G_PARAM_WRITABLE            = 1 << 1,
  G_PARAM_CONSTRUCT           = 1 << 2,
  G_PARAM_CONSTRUCT_ONLY      = 1 << 3,
  G_PARAM_LAX_VALIDATION      = 1 << 4,
  G_PARAM_STATIC_NAME         = 1 << 5
} GParamFlags;

typedef enum
{
  GTK_ICON_LOOKUP_NO_SVG           = 1 << 0,
  GTK_ICON_LOOKUP_FORCE_SVG        = 1 << 1,
  GTK_ICON_LOOKUP_USE_BUILTIN      = 1 << 2,
  GTK_ICON_LOOKUP_GENERIC_FALLBACK = 1 << 3,
  GTK_ICON_LOOKUP_FORCE_SIZE       = 1 << 4
} GtkIconLookupFlags;

typedef enum
{
  GTK_UPDATE_CONTINUOUS,
  GTK_UPDATE_DISCONTINUOUS,
  GTK_UPDATE_DELAYED
} GtkUpdateType;

typedef enum
{
  GTK_PROGRESS_CONTINUOUS,
  GTK_PROGRESS_DISCRETE
} GtkProgressBarStyle;

typedef enum
{
  GTK_PROGRESS_LEFT_TO_RIGHT,
  GTK_PROGRESS_RIGHT_TO_LEFT,
  GTK_PROGRESS_BOTTOM_TO_TOP,
  GTK_PROGRESS_TOP_TO_BOTTOM
} GtkProgressBarOrientation;

typedef enum {
    CAIRO_FORMAT_INVALID   = -1,
    CAIRO_FORMAT_ARGB32    = 0,
    CAIRO_FORMAT_RGB24     = 1,
    CAIRO_FORMAT_A8        = 2,
    CAIRO_FORMAT_A1        = 3,
    CAIRO_FORMAT_RGB16_565 = 4
} cairo_format_t;

typedef enum _cairo_status {
    CAIRO_STATUS_SUCCESS = 0,

    CAIRO_STATUS_NO_MEMORY,
    CAIRO_STATUS_INVALID_RESTORE,
    CAIRO_STATUS_INVALID_POP_GROUP,
    CAIRO_STATUS_NO_CURRENT_POINT,
    CAIRO_STATUS_INVALID_MATRIX,
    CAIRO_STATUS_INVALID_STATUS,
    CAIRO_STATUS_NULL_POINTER,
    CAIRO_STATUS_INVALID_STRING,
    CAIRO_STATUS_INVALID_PATH_DATA,
    CAIRO_STATUS_READ_ERROR,
    CAIRO_STATUS_WRITE_ERROR,
    CAIRO_STATUS_SURFACE_FINISHED,
    CAIRO_STATUS_SURFACE_TYPE_MISMATCH,
    CAIRO_STATUS_PATTERN_TYPE_MISMATCH,
    CAIRO_STATUS_INVALID_CONTENT,
    CAIRO_STATUS_INVALID_FORMAT,
    CAIRO_STATUS_INVALID_VISUAL,
    CAIRO_STATUS_FILE_NOT_FOUND,
    CAIRO_STATUS_INVALID_DASH,
    CAIRO_STATUS_INVALID_DSC_COMMENT,
    CAIRO_STATUS_INVALID_INDEX,
    CAIRO_STATUS_CLIP_NOT_REPRESENTABLE,
    CAIRO_STATUS_TEMP_FILE_ERROR,
    CAIRO_STATUS_INVALID_STRIDE,
    CAIRO_STATUS_FONT_TYPE_MISMATCH,
    CAIRO_STATUS_USER_FONT_IMMUTABLE,
    CAIRO_STATUS_USER_FONT_ERROR,
    CAIRO_STATUS_NEGATIVE_COUNT,
    CAIRO_STATUS_INVALID_CLUSTERS,
    CAIRO_STATUS_INVALID_SLANT,
    CAIRO_STATUS_INVALID_WEIGHT,
    CAIRO_STATUS_INVALID_SIZE,
    CAIRO_STATUS_USER_FONT_NOT_IMPLEMENTED,
    CAIRO_STATUS_DEVICE_TYPE_MISMATCH,
    CAIRO_STATUS_DEVICE_ERROR,

    CAIRO_STATUS_LAST_STATUS
} cairo_status_t;

/* We define all structure pointers to be void* */
typedef void GdkPixbuf;
typedef void GMainContext;
typedef void GVfs;

typedef void GdkColormap;
typedef void GdkDrawable;
typedef void GdkGC;
typedef void GdkPixmap;
typedef void GtkStyleContext;
typedef void GtkFixed;
typedef void GtkMenuItem;
typedef void GtkMenuShell;
typedef void GtkWidgetClass;
typedef void PangoFontDescription;
typedef void GtkSettings;
typedef void GtkStyleProvider;
typedef void cairo_pattern_t;
typedef void cairo_t;
typedef void cairo_surface_t;
typedef void GtkScrolledWindow;
typedef void GtkIconTheme;
typedef void GtkWidget;
typedef void GtkMisc;
typedef void GtkContainer;
typedef void GtkBin;
typedef void GtkAdjustment;
typedef void GtkRange;
typedef void GtkProgressBar;
typedef void GtkProgress;
typedef void GtkWidgetPath;
typedef void GtkPaned;

/* Some real structures */
typedef struct
{
  guint32 pixel;
  guint16 red;
  guint16 green;
  guint16 blue;
} GdkColor;

typedef struct
{
  gdouble red;
  gdouble green;
  gdouble blue;
  gdouble alpha;
} GdkRGBA;

typedef struct {
  gint      fd;
  gushort   events;
  gushort   revents;
} GPollFD;

typedef struct {
  gint x;
  gint y;
  gint width;
  gint height;
} GdkRectangle;

typedef struct {
    int x, y;
    int width, height;
} GtkAllocation;

typedef struct {
  gint width;
  gint height;
} GtkRequisition;

typedef struct {
  GtkWidgetClass *g_class;
} GTypeInstance;

typedef struct {
  gint16 left;
  gint16 right;
  gint16 top;
  gint16 bottom;
} GtkBorder;

typedef struct
{
  GType         g_type;
  union {
    gint        v_int;
    guint       v_uint;
    glong       v_long;
    gulong      v_ulong;
    gint64      v_int64;
    guint64     v_uint64;
    gfloat      v_float;
    gdouble     v_double;
    gpointer    v_pointer;
  } data[2];
} GValue;

typedef struct {
  GTypeInstance  g_type_instance;
  const gchar   *name;
  GParamFlags    flags;
  GType    value_type;
  GType    owner_type;
} GParamSpec;

static gchar* (*fp_glib_check_version)(guint required_major,
                           guint required_minor, guint required_micro);

/**
 * Returns :
 * NULL if the GTK+ library is compatible with the given version, or a string
 * describing the version mismatch.
 */
static gchar* (*fp_gtk_check_version)(guint required_major, guint
                           required_minor, guint required_micro);

static void (*fp_g_free)(gpointer mem);
static void (*fp_g_object_unref)(gpointer object);
static GdkWindow *(*fp_gdk_get_default_root_window) (void);
static int (*fp_gdk_window_get_scale_factor) (GdkWindow *window);

static int (*fp_gdk_pixbuf_get_bits_per_sample)(const GdkPixbuf *pixbuf);
static guchar *(*fp_gdk_pixbuf_get_pixels)(const GdkPixbuf *pixbuf);
static gboolean (*fp_gdk_pixbuf_get_has_alpha)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_height)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_n_channels)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_rowstride)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_width)(const GdkPixbuf *pixbuf);
static GdkPixbuf *(*fp_gdk_pixbuf_new_from_file)(const char *filename,
                                                              GError **error);
static GdkColorspace (*fp_gdk_pixbuf_get_colorspace)(const GdkPixbuf *pixbuf);

static GdkPixbuf *(*fp_gdk_pixbuf_get_from_drawable)(GdkWindow *window,
        int src_x, int src_y, int width, int height);
static GdkPixbuf *(*fp_gdk_pixbuf_scale_simple)(GdkPixbuf *src,
        int dest_width, int dest_heigh, GdkInterpType interp_type);


static void (*fp_gtk_widget_destroy)(void *widget);
static void (*fp_gtk_window_present)(GtkWindow *window);
static void (*fp_gtk_window_move)(GtkWindow *window, gint x, gint y);
static void (*fp_gtk_window_resize)(GtkWindow *window, gint width, gint height);

/**
 * Function Pointers for GtkFileChooser
 */
static gchar* (*fp_gtk_file_chooser_get_filename)(GtkFileChooser *chooser);
static void (*fp_gtk_widget_hide)(void *widget);
static void (*fp_gtk_main_quit)(void);
static void* (*fp_gtk_file_chooser_dialog_new)(const gchar *title,
    GtkWindow *parent, GtkFileChooserAction action,
    const gchar *first_button_text, ...);
static gboolean (*fp_gtk_file_chooser_set_current_folder)
                              (GtkFileChooser *chooser, const gchar *filename);
static gboolean (*fp_gtk_file_chooser_set_filename)(GtkFileChooser *chooser,
    const char *filename);
static void (*fp_gtk_file_chooser_set_current_name)(GtkFileChooser *chooser,
    const gchar *name);
static void (*fp_gtk_file_filter_add_custom)(GtkFileFilter *filter,
    GtkFileFilterFlags needed, GtkFileFilterFunc func, gpointer data,
    GDestroyNotify notify);
static void (*fp_gtk_file_chooser_set_filter)(GtkFileChooser *chooser,
    GtkFileFilter *filter);
static GType (*fp_gtk_file_chooser_get_type)(void);
static GtkFileFilter* (*fp_gtk_file_filter_new)(void);
static void (*fp_gtk_file_chooser_set_do_overwrite_confirmation)(
    GtkFileChooser *chooser, gboolean do_overwrite_confirmation);
static void (*fp_gtk_file_chooser_set_select_multiple)(
    GtkFileChooser *chooser, gboolean select_multiple);
static gchar* (*fp_gtk_file_chooser_get_current_folder)
                                                      (GtkFileChooser *chooser);
static GSList* (*fp_gtk_file_chooser_get_filenames)(GtkFileChooser *chooser);
static guint (*fp_gtk_g_slist_length)(GSList *list);
static gulong (*fp_g_signal_connect_data)(gpointer instance,
    const gchar *detailed_signal, GCallback c_handler, gpointer data,
    GClosureNotify destroy_data, GConnectFlags connect_flags);
static void (*fp_gtk_widget_show)(void *widget);
static void (*fp_gtk_main)(void);
static guint (*fp_gtk_main_level)(void);
static gchar* (*fp_g_path_get_dirname) (const gchar *file_name);
static XID (*fp_gdk_x11_drawable_get_xid) (GdkWindow *drawable);

static GList* (*fp_g_list_append) (GList *list, gpointer data);
static void (*fp_g_list_free) (GList *list);
static void (*fp_g_list_free_full) (GList *list, GDestroyNotify free_func);

static void (*fp_gdk_threads_enter)(void);
static void (*fp_gdk_threads_leave)(void);

static gboolean (*fp_gtk_show_uri)(GdkScreen *screen, const gchar *uri,
    guint32 timestamp, GError **error);

// Implementation functions prototypes
static void gtk3_init(GtkApi* gtk);
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
static void         (*fp_g_object_get)(gpointer object,
                                       const gchar* fpn, ...);
static void         (*fp_g_object_set)(gpointer object,
                                       const gchar *first_property_name,
                                       ...);

static gboolean (*fp_g_main_context_iteration)(GMainContext *context, gboolean may_block);
static gboolean (*fp_g_str_has_prefix)(const gchar *str, const gchar *prefix);
static gchar** (*fp_g_strsplit)(const gchar *string, const gchar *delimiter,
           gint max_tokens);
static void (*fp_g_strfreev)(gchar **str_array);


static cairo_surface_t* (*fp_cairo_image_surface_create)(cairo_format_t format,
                               int width, int height);
static void (*fp_cairo_surface_destroy)(cairo_surface_t *surface);
static cairo_status_t (*fp_cairo_surface_status)(cairo_surface_t *surface);
static cairo_t* (*fp_cairo_create)(cairo_surface_t *target);
static void (*fp_cairo_destroy)(cairo_t *cr);
static cairo_status_t (*fp_cairo_status)(cairo_t *cr);
static void (*fp_cairo_fill)(cairo_t *cr);
static void (*fp_cairo_surface_flush)(cairo_surface_t *surface);
static void (*fp_cairo_rectangle)(cairo_t *cr, double x, double y, double width,
                double height);
static void (*fp_cairo_set_source_rgb)(cairo_t *cr, double red, double green,
                double blue);
static void (*fp_cairo_set_source_rgba)(cairo_t *cr, double red, double green,
                double blue, double alpha);
static void (*fp_cairo_paint)(cairo_t *cr);
static void (*fp_cairo_clip)(cairo_t *cr);
static unsigned char* (*fp_cairo_image_surface_get_data)(
                                                 cairo_surface_t *surface);
static int (*fp_cairo_image_surface_get_stride) (cairo_surface_t *surface);
static GdkPixbuf* (*fp_gdk_pixbuf_get_from_surface)(cairo_surface_t *surface,
                            gint src_x, gint src_y, gint width, gint height);
static GtkStateType (*fp_gtk_widget_get_state)(GtkWidget *widget);
static void (*fp_gtk_widget_set_state)(GtkWidget *widget, GtkStateType state);
static gboolean (*fp_gtk_widget_is_focus)(GtkWidget *widget);
static void (*fp_gtk_widget_set_allocation)(GtkWidget *widget,
                                            const GtkAllocation *allocation);
static GtkWidget* (*fp_gtk_widget_get_parent)(GtkWidget *widget);
static GtkStyleContext* (*fp_gtk_widget_get_style_context)(GtkWidget *widget);
static void (*fp_gtk_style_context_get_color)(GtkStyleContext *context,
                                           GtkStateFlags state, GdkRGBA *color);
static void (*fp_gtk_style_context_get_background_color)
                (GtkStyleContext *context, GtkStateFlags state, GdkRGBA *color);
static void (*fp_gtk_style_context_get)(GtkStyleContext *context,
                                                      GtkStateFlags state, ...);
static GtkStateFlags (*fp_gtk_widget_get_state_flags)(GtkWidget* widget);
static void (*fp_gtk_style_context_set_state)(GtkStyleContext* style,
                                              GtkStateFlags flags);
static void (*fp_gtk_style_context_add_class)(GtkStyleContext *context,
                                                 const gchar *class_name);
static void (*fp_gtk_style_context_save)(GtkStyleContext *context);
static void (*fp_gtk_style_context_restore)(GtkStyleContext *context);
static void (*fp_gtk_render_check)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_option)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_extension)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height,
                     GtkPositionType gap_side);
static void (*fp_gtk_render_expander)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_frame_gap)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height,
                     GtkPositionType gap_side, gdouble xy0_gap,
                     gdouble xy1_gap);
static void (*fp_gtk_render_line)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x0, gdouble y0, gdouble x1, gdouble y1);
static GdkPixbuf* (*fp_gtk_widget_render_icon_pixbuf)(GtkWidget *widget,
                     const gchar *stock_id, GtkIconSize size);
static cairo_surface_t* (*fp_gdk_window_create_similar_image_surface)(
                     GdkWindow *window, cairo_format_t format, int width,
                     int height, int scale);
static cairo_surface_t* (*fp_gdk_window_create_similar_surface)(
                     GdkWindow *window, cairo_format_t format,
                     int width, int height);
static GdkWindow* (*fp_gtk_widget_get_window)(GtkWidget *widget);
static GtkSettings *(*fp_gtk_settings_get_for_screen)(GdkScreen *screen);
static GdkScreen *(*fp_gtk_widget_get_screen)(GtkWidget *widget);
static GtkStyleProvider* (*fp_gtk_css_provider_get_named)(const gchar *name,
                     const gchar *variant);
static void (*fp_gtk_style_context_add_provider)(GtkStyleContext *context,
                     GtkStyleProvider *provider, guint priority);
static void (*fp_gtk_render_frame)(GtkStyleContext *context,cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_focus)(GtkStyleContext *context,cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_handle)(GtkStyleContext *context,cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_style_context_get_property)(GtkStyleContext *context,
                     const gchar *property, GtkStateFlags state, GValue *value);
static void (*fp_gtk_render_activity)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static void (*fp_gtk_render_background)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height);
static gboolean (*fp_gtk_style_context_has_class)(GtkStyleContext *context,
                     const gchar *class_name);
static void transform_detail_string (const gchar *detail,
                     GtkStyleContext *context);
void (*fp_gtk_style_context_set_junction_sides)(GtkStyleContext  *context,
                     GtkJunctionSides  sides);
void (*fp_gtk_style_context_add_region)(GtkStyleContext *context,
                     const gchar *region_name, GtkRegionFlags flags);
void (*fp_gtk_render_arrow)(GtkStyleContext *context, cairo_t *cr,
                     gdouble angle, gdouble x, gdouble y, gdouble size);
void (*fp_gtk_bin_set_child)(GtkBin *bin, GtkWidget *widget);
void (*fp_gtk_scrolled_window_set_shadow_type)(
                     GtkScrolledWindow *scrolled_window, GtkShadowType type);
static void (*fp_gtk_render_slider)(GtkStyleContext *context, cairo_t *cr,
                     gdouble x, gdouble y, gdouble width, gdouble height,
                     GtkOrientation orientation);
static void (*fp_gtk_style_context_get_padding)(GtkStyleContext *self,
                     GtkStateFlags state, GtkBorder* padding);
static void (*fp_gtk_range_set_inverted)(GtkRange *range, gboolean  setting);
static PangoFontDescription* (*fp_gtk_style_context_get_font)(
                     GtkStyleContext *context, GtkStateFlags state);
static int (*fp_gtk_widget_get_allocated_width)(GtkWidget *widget);
static int (*fp_gtk_widget_get_allocated_height)(GtkWidget *widget);
static GtkIconTheme* (*fp_gtk_icon_theme_get_default)(void);
static GdkPixbuf* (*fp_gtk_icon_theme_load_icon)(GtkIconTheme *icon_theme,
                     const gchar *icon_name, gint size,
                     GtkIconLookupFlags flags, GError **error);
static void (*fp_gtk_adjustment_set_lower)(GtkAdjustment *adjustment,
                     gdouble lower);
static void (*fp_gtk_adjustment_set_page_increment)(GtkAdjustment *adjustment,
                     gdouble page_increment);
static void (*fp_gtk_adjustment_set_page_size)(GtkAdjustment *adjustment,
                     gdouble page_size);
static void (*fp_gtk_adjustment_set_step_increment)(GtkAdjustment *adjustment,
                     gdouble step_increment);
static void (*fp_gtk_adjustment_set_upper)(GtkAdjustment *adjustment,
                     gdouble upper);
static void (*fp_gtk_adjustment_set_value)(GtkAdjustment *adjustment,
                     gdouble value);
static GdkGC *(*fp_gdk_gc_new)(GdkDrawable*);
static void (*fp_gdk_rgb_gc_set_foreground)(GdkGC*, guint32);
static void (*fp_gdk_draw_rectangle)(GdkDrawable*, GdkGC*, gboolean,
        gint, gint, gint, gint);
static GdkPixbuf *(*fp_gdk_pixbuf_new)(GdkColorspace colorspace,
        gboolean has_alpha, int bits_per_sample, int width, int height);
static void (*fp_gdk_drawable_get_size)(GdkDrawable *drawable,
        gint* width, gint* height);
static gboolean (*fp_gtk_init_check)(int* argc, char** argv);

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
static GtkWidget* (*fp_gtk_paned_new)(GtkOrientation orientation);
static GtkWidget* (*fp_gtk_scale_new)(GtkOrientation  orientation,
                                       GtkAdjustment* adjustment);
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
static GtkAdjustment* (*fp_gtk_adjustment_new)(gdouble value,
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
static GtkWidgetPath* (*fp_gtk_widget_path_copy)
        (const GtkWidgetPath *path);
static const GtkWidgetPath* (*fp_gtk_style_context_get_path)
        (GtkStyleContext *context);
static GtkWidgetPath* (*fp_gtk_widget_path_new) (void);
static gint (*fp_gtk_widget_path_append_type)
        (GtkWidgetPath *path, GType type);
static void (*fp_gtk_widget_path_iter_set_object_name)
        (GtkWidgetPath *path, gint pos, const char *name);
static void (*fp_gtk_style_context_set_path)
        (GtkStyleContext *context, GtkWidgetPath *path);
static void (*fp_gtk_widget_path_unref) (GtkWidgetPath *path);
static GtkStyleContext* (*fp_gtk_style_context_new) (void);

#endif /* !_GTK3_INTERFACE_H */
