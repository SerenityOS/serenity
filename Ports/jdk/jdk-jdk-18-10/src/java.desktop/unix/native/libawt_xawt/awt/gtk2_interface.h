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

#ifndef _GTK2_INTERFACE_H
#define _GTK2_INTERFACE_H

#include <stdlib.h>
#include <jni.h>
#include <X11/X.h>
#include "gtk_interface.h"

#define GTK_HAS_FOCUS   (1 << 12)
#define GTK_HAS_DEFAULT (1 << 14)

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
  G_PARAM_PRIVATE             = 1 << 5
} GParamFlags;

/* We define all structure pointers to be void* */
typedef void GMainContext;
typedef void GVfs;

typedef void GdkColormap;
typedef void GdkDrawable;
typedef void GdkGC;
typedef void GdkPixbuf;
typedef void GdkPixmap;

typedef void GtkFixed;
typedef void GtkMenuItem;
typedef void GtkMenuShell;
typedef void GtkWidgetClass;
typedef void PangoFontDescription;
typedef void GtkSettings;

/* Some real structures */
typedef struct
{
  guint32 pixel;
  guint16 red;
  guint16 green;
  guint16 blue;
} GdkColor;

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
  gint x;
  gint y;
  gint width;
  gint height;
} GtkAllocation;

typedef struct {
  gint width;
  gint height;
} GtkRequisition;

typedef struct {
  GtkWidgetClass *g_class;
} GTypeInstance;

typedef struct {
  gint left;
  gint right;
  gint top;
  gint bottom;
} GtkBorder;

/******************************************************
 * FIXME: it is more safe to include gtk headers for
 * the precise type definition of GType and other
 * structures. This is a place where getting rid of gtk
 * headers may be dangerous.
 ******************************************************/

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

typedef struct
{
  GTypeInstance  g_type_instance;

  gchar         *name;
  GParamFlags    flags;
  GType          value_type;
  GType          owner_type;
} GParamSpec;

typedef struct {
  GTypeInstance g_type_instance;
  guint         ref_count;
  void         *qdata;
} GObject;

typedef struct {
  GObject parent_instance;
  guint32 flags;
} GtkObject;

typedef struct
{
  GObject parent_instance;

  GdkColor fg[5];
  GdkColor bg[5];
  GdkColor light[5];
  GdkColor dark[5];
  GdkColor mid[5];
  GdkColor text[5];
  GdkColor base[5];
  GdkColor text_aa[5];          /* Halfway between text/base */

  GdkColor black;
  GdkColor white;
  PangoFontDescription *font_desc;

  gint xthickness;
  gint ythickness;

  GdkGC *fg_gc[5];
  GdkGC *bg_gc[5];
  GdkGC *light_gc[5];
  GdkGC *dark_gc[5];
  GdkGC *mid_gc[5];
  GdkGC *text_gc[5];
  GdkGC *base_gc[5];
  GdkGC *text_aa_gc[5];
  GdkGC *black_gc;
  GdkGC *white_gc;

  GdkPixmap *bg_pixmap[5];
} GtkStyle;

typedef struct _GtkWidget GtkWidget;
struct _GtkWidget
{
  GtkObject object;
  guint16 private_flags;
  guint8 state;
  guint8 saved_state;
  gchar *name;
  GtkStyle *style;
  GtkRequisition requisition;
  GtkAllocation allocation;
  GdkWindow *window;
  GtkWidget *parent;
};

typedef struct
{
  GtkWidget widget;

  gfloat xalign;
  gfloat yalign;

  guint16 xpad;
  guint16 ypad;
} GtkMisc;

typedef struct {
  GtkWidget widget;
  GtkWidget *focus_child;
  guint border_width : 16;
  guint need_resize : 1;
  guint resize_mode : 2;
  guint reallocate_redraws : 1;
  guint has_focus_chain : 1;
} GtkContainer;

typedef struct {
  GtkContainer container;
  GtkWidget *child;
} GtkBin;

typedef struct {
  GtkBin bin;
  GdkWindow *event_window;
  gchar *label_text;
  guint activate_timeout;
  guint constructed : 1;
  guint in_button : 1;
  guint button_down : 1;
  guint relief : 2;
  guint use_underline : 1;
  guint use_stock : 1;
  guint depressed : 1;
  guint depress_on_activate : 1;
  guint focus_on_click : 1;
} GtkButton;

typedef struct {
  GtkButton button;
  guint active : 1;
  guint draw_indicator : 1;
  guint inconsistent : 1;
} GtkToggleButton;

typedef struct _GtkAdjustment GtkAdjustment;
struct _GtkAdjustment
{
  GtkObject parent_instance;

  gdouble lower;
  gdouble upper;
  gdouble value;
  gdouble step_increment;
  gdouble page_increment;
  gdouble page_size;
};

typedef enum
{
  GTK_UPDATE_CONTINUOUS,
  GTK_UPDATE_DISCONTINUOUS,
  GTK_UPDATE_DELAYED
} GtkUpdateType;

typedef struct _GtkRange GtkRange;
struct _GtkRange
{
  GtkWidget widget;
  GtkAdjustment *adjustment;
  GtkUpdateType update_policy;
  guint inverted : 1;
  /*< protected >*/
  guint flippable : 1;
  guint has_stepper_a : 1;
  guint has_stepper_b : 1;
  guint has_stepper_c : 1;
  guint has_stepper_d : 1;
  guint need_recalc : 1;
  guint slider_size_fixed : 1;
  gint min_slider_size;
  GtkOrientation orientation;
  GdkRectangle range_rect;
  gint slider_start, slider_end;
  gint round_digits;
  /*< private >*/
  guint trough_click_forward : 1;
  guint update_pending : 1;
  /*GtkRangeLayout * */ void *layout;
  /*GtkRangeStepTimer * */ void* timer;
  gint slide_initial_slider_position;
  gint slide_initial_coordinate;
  guint update_timeout_id;
  GdkWindow *event_window;
};

typedef struct _GtkProgressBar       GtkProgressBar;

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

typedef struct _GtkProgress       GtkProgress;

struct _GtkProgress
{
  GtkWidget widget;
  GtkAdjustment *adjustment;
  GdkPixmap     *offscreen_pixmap;
  gchar         *format;
  gfloat         x_align;
  gfloat         y_align;
  guint          show_text : 1;
  guint          activity_mode : 1;
  guint          use_text_format : 1;
};

struct _GtkProgressBar
{
  GtkProgress progress;
  GtkProgressBarStyle bar_style;
  GtkProgressBarOrientation orientation;
  guint blocks;
  gint  in_block;
  gint  activity_pos;
  guint activity_step;
  guint activity_blocks;
  gdouble pulse_fraction;
  guint activity_dir : 1;
  guint ellipsize : 3;
};

/**
 * Returns :
 * NULL if the GLib library is compatible with the given version, or a string
 * describing the version mismatch.
 * Please note that the glib_check_version() is available since 2.6,
 * so you should use GLIB_CHECK_VERSION macro instead.
 */
static gchar* (*fp_glib_check_version)(guint required_major, guint required_minor,
                       guint required_micro);

/**
 * Returns :
 *  TRUE if the GLib library is compatible with the given version
 */
#define GLIB_CHECK_VERSION(major, minor, micro) \
    (fp_glib_check_version && fp_glib_check_version(major, minor, micro) == NULL)

/**
 * Returns :
 * NULL if the GTK+ library is compatible with the given version, or a string
 * describing the version mismatch.
 */
static gchar* (*fp_gtk_check_version)(guint required_major, guint required_minor,
                       guint required_micro);

static void gtk2_init(GtkApi* gtk);

static void (*fp_g_free)(gpointer mem);
static void (*fp_g_object_unref)(gpointer object);
static GdkWindow *(*fp_gdk_get_default_root_window) (void);

static int (*fp_gdk_pixbuf_get_bits_per_sample)(const GdkPixbuf *pixbuf);
static guchar *(*fp_gdk_pixbuf_get_pixels)(const GdkPixbuf *pixbuf);
static gboolean (*fp_gdk_pixbuf_get_has_alpha)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_height)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_n_channels)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_rowstride)(const GdkPixbuf *pixbuf);
static int (*fp_gdk_pixbuf_get_width)(const GdkPixbuf *pixbuf);
static GdkPixbuf *(*fp_gdk_pixbuf_new_from_file)(const char *filename, GError **error);
static GdkColorspace (*fp_gdk_pixbuf_get_colorspace)(const GdkPixbuf *pixbuf);

static GdkPixbuf *(*fp_gdk_pixbuf_get_from_drawable)(GdkPixbuf *dest,
        GdkDrawable *src, GdkColormap *cmap, int src_x, int src_y,
        int dest_x, int dest_y, int width, int height);
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
static gboolean (*fp_gtk_file_chooser_set_current_folder)(GtkFileChooser *chooser,
    const gchar *filename);
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
static gchar* (*fp_gtk_file_chooser_get_current_folder)(GtkFileChooser *chooser);
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

static gboolean (*fp_gtk_show_uri)(GdkScreen *screen, const gchar *uri,
    guint32 timestamp, GError **error);

#endif /* !_GTK2_INTERFACE_H */
