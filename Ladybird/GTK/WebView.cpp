#include "WebView.h"
#include "BitmapPaintable.h"
#include "ViewImpl.h"
#include "WebViewPrivate.h"

struct _LadybirdWebView {
    GtkWidget parent_instance;

    OwnPtr<LadybirdViewImpl> impl;
    LadybirdBitmapPaintable* bitmap_paintable;
    LadybirdBitmapPaintable* favicon;
    WebView::CookieJar* cookie_jar;
    GtkScrollablePolicy hscroll_policy;
    GtkAdjustment* hadjustment;
    GtkScrollablePolicy vscroll_policy;
    GtkAdjustment* vadjustment;

    char* page_url;
    char* page_title;
    char* hovered_link;
    // These two are in device pixels (same as texture size).
    int page_width;
    int page_height;

    bool loading : 1;
};

enum {
    PROP_0,
    PROP_PAGE_TITLE,
    PROP_PAGE_URL,
    PROP_LOADING,
    PROP_ZOOM_PERCENT,
    PROP_COOKIE_JAR,
    PROP_HOVERED_LINK,
    NUM_PROPS,

    PROP_HADJUSTMENT,
    PROP_VADJUSTMENT,
    PROP_HSCROLL_POLICY,
    PROP_VSCROLL_POLICY
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdWebView, ladybird_web_view, GTK_TYPE_WIDGET,
    G_IMPLEMENT_INTERFACE(GTK_TYPE_SCROLLABLE, nullptr))

char const* ladybird_web_view_get_page_title(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return self->page_title;
}

void ladybird_web_view_set_page_title(LadybirdWebView* self, char const* title)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (g_set_str(&self->page_title, title))
        g_object_notify_by_pspec(G_OBJECT(self), props[PROP_PAGE_TITLE]);
}

void ladybird_web_view_set_page_size(LadybirdWebView* self, int width, int height)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));
    g_return_if_fail(width >= 0);
    g_return_if_fail(height >= 0);

    self->page_width = width;
    self->page_height = height;
    gtk_widget_queue_resize(GTK_WIDGET(self));
}

static void scale_size_down(LadybirdWebView* self, int device_width, int device_height, int* logical_width, int* logical_height)
{
    int scale_factor = gtk_widget_get_scale_factor(GTK_WIDGET(self));
    *logical_width = ceil((double)device_width / scale_factor);
    *logical_height = ceil((double)device_height / scale_factor);
}

char const* ladybird_web_view_get_page_url(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return self->page_url;
}

void ladybird_web_view_set_page_url(LadybirdWebView* self, char const* page_url)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (g_set_str(&self->page_url, page_url))
        g_object_notify_by_pspec(G_OBJECT(self), props[PROP_PAGE_URL]);
}

void ladybird_web_view_load_url(LadybirdWebView* self, char const* url)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));
    g_return_if_fail(url != nullptr);

    AK::URL ak_url = StringView(url, strlen(url));
    self->impl->load(ak_url);
}

bool ladybird_web_view_get_loading(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), false);

    return self->loading;
}

void ladybird_web_view_set_loading(LadybirdWebView* self, bool loading)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (self->loading == loading)
        return;
    self->loading = loading;
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_LOADING]);
}

char const* ladybird_web_view_get_hovered_link(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return self->hovered_link;
}

void ladybird_web_view_set_hovered_link(LadybirdWebView* self, char const* hovered_link)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (g_set_str(&self->hovered_link, hovered_link))
        g_object_notify_by_pspec(G_OBJECT(self), props[PROP_HOVERED_LINK]);
}

void ladybird_web_view_zoom_in(LadybirdWebView* self)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    self->impl->zoom_in();
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_ZOOM_PERCENT]);
    gtk_widget_queue_allocate(GTK_WIDGET(self));
}

void ladybird_web_view_zoom_out(LadybirdWebView* self)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    self->impl->zoom_out();
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_ZOOM_PERCENT]);
    gtk_widget_queue_allocate(GTK_WIDGET(self));
}

void ladybird_web_view_zoom_reset(LadybirdWebView* self)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    self->impl->reset_zoom();
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_ZOOM_PERCENT]);
    gtk_widget_queue_allocate(GTK_WIDGET(self));
}

guint ladybird_web_view_get_zoom_percent(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), 0);

    return round(self->impl->zoom_level() * 100.0);
}

WebView::CookieJar* ladybird_web_view_get_cookie_jar(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return self->cookie_jar;
}

void ladybird_web_view_set_cookie_jar(LadybirdWebView* self, WebView::CookieJar* cookie_jar)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (self->cookie_jar == cookie_jar)
        return;

    self->cookie_jar = cookie_jar;
    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_COOKIE_JAR]);
}

LadybirdViewImpl* ladybird_web_view_get_impl(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return self->impl.ptr();
}

static void ladybird_web_view_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    switch (prop_id) {
    case PROP_PAGE_TITLE:
        g_value_set_string(value, self->page_title);
        break;

    case PROP_PAGE_URL:
        g_value_set_string(value, self->page_url);
        break;

    case PROP_LOADING:
        g_value_set_boolean(value, self->loading);
        break;

    case PROP_ZOOM_PERCENT:
        g_value_set_uint(value, ladybird_web_view_get_zoom_percent(self));
        break;

    case PROP_COOKIE_JAR:
        g_value_set_pointer(value, self->cookie_jar);
        break;

    case PROP_HOVERED_LINK:
        g_value_set_string(value, self->hovered_link);
        break;

    case PROP_HADJUSTMENT:
        g_value_set_object(value, self->hadjustment);
        break;

    case PROP_VADJUSTMENT:
        g_value_set_object(value, self->vadjustment);
        break;

    case PROP_HSCROLL_POLICY:
        g_value_set_enum(value, self->hscroll_policy);
        break;

    case PROP_VSCROLL_POLICY:
        g_value_set_enum(value, self->vscroll_policy);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void adjustment_value_changed([[maybe_unused]] GtkAdjustment* adjustment, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    gtk_widget_queue_allocate(GTK_WIDGET(self));
}

static void ladybird_web_view_set_adjustment(LadybirdWebView* self, bool vertical, GtkAdjustment* adjustment)
{
    GtkAdjustment** self_adjustment = vertical ? &self->vadjustment : &self->hadjustment;
    if (*self_adjustment == adjustment)
        return;

    // Let go of the old adjustment.
    if (*self_adjustment)
        g_signal_handlers_disconnect_by_func(*self_adjustment, (void*)adjustment_value_changed, self);

    g_set_object(self_adjustment, adjustment);
    g_signal_connect_object(adjustment, "value-changed", G_CALLBACK(adjustment_value_changed), self, G_CONNECT_DEFAULT);
    g_object_notify(G_OBJECT(self), vertical ? "vadjustment" : "hadjustment");

    // Our size hasn't changed, but we want size_allocate() called on us.
    gtk_widget_queue_allocate(GTK_WIDGET(self));
}

static void ladybird_web_view_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    switch (prop_id) {
    case PROP_PAGE_TITLE:
        ladybird_web_view_set_page_title(self, g_value_get_string(value));
        break;

    case PROP_PAGE_URL:
        ladybird_web_view_set_page_url(self, g_value_get_string(value));
        break;

    case PROP_LOADING:
        ladybird_web_view_set_loading(self, g_value_get_boolean(value));
        break;

    case PROP_COOKIE_JAR:
        ladybird_web_view_set_cookie_jar(self, reinterpret_cast<WebView::CookieJar*>(g_value_get_pointer(value)));
        break;

    case PROP_HOVERED_LINK:
        ladybird_web_view_set_hovered_link(self, g_value_get_string(value));
        break;

    case PROP_HADJUSTMENT:
        ladybird_web_view_set_adjustment(self, false, (GtkAdjustment*)g_value_get_object(value));
        break;

    case PROP_VADJUSTMENT:
        ladybird_web_view_set_adjustment(self, true, (GtkAdjustment*)g_value_get_object(value));
        break;

    case PROP_HSCROLL_POLICY:
        self->hscroll_policy = (GtkScrollablePolicy)g_value_get_enum(value);
        break;

    case PROP_VSCROLL_POLICY:
        self->vscroll_policy = (GtkScrollablePolicy)g_value_get_enum(value);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_web_view_measure(GtkWidget* widget, GtkOrientation orientation, [[maybe_unused]] int for_size, int* minimum, int* natural, int* minimum_baseline, int* natural_baseline)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(widget);
    int page_width, page_height;
    scale_size_down(self, self->page_width, self->page_height, &page_width, &page_height);

    switch (orientation) {
    case GTK_ORIENTATION_HORIZONTAL:
        *minimum = 0;
        *natural = page_width;
        break;

    case GTK_ORIENTATION_VERTICAL:
        *minimum = 0;
        *natural = page_height;
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    *minimum_baseline = *natural_baseline = -1;
}

static void ladybird_web_view_size_allocate(GtkWidget* widget, int width, int height, [[maybe_unused]] int baseline)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(widget);

    double hadj = self->hadjustment ? gtk_adjustment_get_value(self->hadjustment) : 0.0;
    double vadj = self->vadjustment ? gtk_adjustment_get_value(self->vadjustment) : 0.0;
    int scale_factor = gtk_widget_get_scale_factor(widget);
    int full_width, full_height;
    scale_size_down(self, self->page_width, self->page_height, &full_width, &full_height);

    self->impl->set_viewport_rect(hadj * scale_factor, vadj * scale_factor, width * scale_factor, height * scale_factor);

    if (self->hadjustment)
        gtk_adjustment_configure(self->hadjustment, hadj, 0, full_width, width * 0.1, width * 0.9, width);
    if (self->vadjustment)
        gtk_adjustment_configure(self->vadjustment, vadj, 0, full_height, height * 0.1, height * 0.9, height);
}

void ladybird_web_view_scroll_by(LadybirdWebView* self, int page_x_delta, int page_y_delta)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    int x_delta, y_delta;
    scale_size_down(self, page_x_delta, page_y_delta, &x_delta, &y_delta);

    if (self->hadjustment && x_delta) {
        double adj = gtk_adjustment_get_value(self->hadjustment);
        gtk_adjustment_set_value(self->hadjustment, adj + x_delta);
    }
    if (self->vadjustment && y_delta) {
        double adj = gtk_adjustment_get_value(self->vadjustment);
        gtk_adjustment_set_value(self->vadjustment, adj + y_delta);
    }
}

void ladybird_web_view_scroll_to(LadybirdWebView* self, int page_x, int page_y)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    int x, y;
    scale_size_down(self, page_x, page_y, &x, &y);

    if (self->hadjustment && x)
        gtk_adjustment_set_value(self->hadjustment, x);
    if (self->vadjustment && y)
        gtk_adjustment_set_value(self->vadjustment, y);
}

void ladybird_web_view_scroll_into_view(LadybirdWebView* self, int page_x, int page_y, int page_width, int page_height)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    int x, y;
    scale_size_down(self, page_x, page_y, &x, &y);
    int width, height;
    scale_size_down(self, page_width, page_height, &width, &height);

    if (self->hadjustment) {
        double adj = gtk_adjustment_get_value(self->hadjustment);
        int viewport_width = gtk_widget_get_width(GTK_WIDGET(self));
        if (adj >= x) {
            // If our viewport's left border is to the right of its left border, set our left border to its.
            gtk_adjustment_set_value(self->hadjustment, x);
        } else if (adj + viewport_width <= x + width) {
            // If our viewport's right border is to the left of its right border, set our right border to its.
            gtk_adjustment_set_value(self->hadjustment, x + width - viewport_width);
        }
    }
    if (self->vadjustment) {
        double adj = gtk_adjustment_get_value(self->vadjustment);
        int viewport_height = gtk_widget_get_height(GTK_WIDGET(self));
        if (adj >= y) {
            // If our viewport's top border is below its top border, set our top border to its.
            gtk_adjustment_set_value(self->vadjustment, y);
        } else if (adj + viewport_height <= y + height) {
            // If our viewport's bottom border is above its bottom border, set our bottom border to its.
            gtk_adjustment_set_value(self->vadjustment, y + height - viewport_height);
        }
    }
}

GdkPaintable* ladybird_web_view_get_bitmap_paintable(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return GDK_PAINTABLE(self->bitmap_paintable);
}

GdkPaintable* ladybird_web_view_get_favicon(LadybirdWebView* self)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(self), nullptr);

    return GDK_PAINTABLE(self->favicon);
}

static void ladybird_web_view_snapshot(GtkWidget* widget, GtkSnapshot* snapshot)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(widget);

    gdk_paintable_snapshot(GDK_PAINTABLE(self->bitmap_paintable), snapshot,
        gtk_widget_get_width(widget),
        gtk_widget_get_height(widget));
}

static void on_scale_factor_change(GObject* object)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    gtk_widget_queue_allocate(GTK_WIDGET(self));
    self->impl->scale_factor_changed();
}

static void translate_coordinates(LadybirdWebView* self, double widget_x, double widget_y, int* page_x, int* page_y)
{
    int scale_factor = gtk_widget_get_scale_factor(GTK_WIDGET(self));
    double hadj = self->hadjustment ? gtk_adjustment_get_value(self->hadjustment) : 0.0;
    double vadj = self->vadjustment ? gtk_adjustment_get_value(self->vadjustment) : 0.0;
    *page_x = (widget_x + hadj) * scale_factor;
    *page_y = (widget_y + vadj) * scale_factor;
}

static KeyCode translate_key(unsigned keyval)
{
    struct Mapping {
        unsigned gdk_key;
        KeyCode serenity_key;
    };
    static const Mapping mappings[] = {
        { GDK_KEY_BackSpace, Key_Backspace },
        { GDK_KEY_Tab, Key_Tab },
        { GDK_KEY_Linefeed, Key_Return },
        { GDK_KEY_Clear, Key_Delete },
        { GDK_KEY_Return, Key_Return },
        { GDK_KEY_Scroll_Lock, Key_ScrollLock },
        { GDK_KEY_Sys_Req, Key_SysRq },
        { GDK_KEY_Escape, Key_Escape },
        { GDK_KEY_Delete, Key_Delete },
        { GDK_KEY_Home, Key_Home },
        { GDK_KEY_Left, Key_Left },
        { GDK_KEY_Up, Key_Up },
        { GDK_KEY_Right, Key_Right },
        { GDK_KEY_Down, Key_Down },
        { GDK_KEY_Page_Up, Key_PageUp },
        { GDK_KEY_Page_Down, Key_PageDown },
        { GDK_KEY_End, Key_End },
        { GDK_KEY_Insert, Key_Insert },
        { GDK_KEY_Cancel, Key_Escape },
        { GDK_KEY_Num_Lock, Key_NumLock },
        { GDK_KEY_KP_Space, Key_Space },
        { GDK_KEY_KP_Tab, Key_Tab },
        { GDK_KEY_KP_Enter, Key_Return },
        { GDK_KEY_KP_F1, Key_F1 },
        { GDK_KEY_KP_F2, Key_F2 },
        { GDK_KEY_KP_F3, Key_F3 },
        { GDK_KEY_KP_F4, Key_F4 },
        { GDK_KEY_KP_Home, Key_Home },
        { GDK_KEY_KP_Left, Key_Left },
        { GDK_KEY_KP_Up, Key_Up },
        { GDK_KEY_KP_Right, Key_Right },
        { GDK_KEY_KP_Down, Key_Down },
        { GDK_KEY_KP_Page_Up, Key_PageUp },
        { GDK_KEY_KP_Page_Down, Key_PageDown },
        { GDK_KEY_KP_End, Key_End },
        { GDK_KEY_KP_Insert, Key_Insert },
        { GDK_KEY_KP_Delete, Key_Delete },
        { GDK_KEY_KP_Equal, Key_Equal },
        { GDK_KEY_KP_Multiply, Key_Asterisk },
        { GDK_KEY_KP_Add, Key_Plus },
        { GDK_KEY_KP_Subtract, Key_Minus },
        { GDK_KEY_KP_Decimal, Key_Period },
        { GDK_KEY_KP_Divide, Key_Slash },
        { GDK_KEY_KP_0, Key_0 },
        { GDK_KEY_KP_1, Key_1 },
        { GDK_KEY_KP_2, Key_2 },
        { GDK_KEY_KP_3, Key_3 },
        { GDK_KEY_KP_4, Key_4 },
        { GDK_KEY_KP_5, Key_5 },
        { GDK_KEY_KP_6, Key_6 },
        { GDK_KEY_KP_7, Key_7 },
        { GDK_KEY_KP_8, Key_8 },
        { GDK_KEY_KP_9, Key_9 },
        { GDK_KEY_F1, Key_F1 },
        { GDK_KEY_F2, Key_F2 },
        { GDK_KEY_F3, Key_F3 },
        { GDK_KEY_F4, Key_F4 },
        { GDK_KEY_F5, Key_F5 },
        { GDK_KEY_F6, Key_F6 },
        { GDK_KEY_F7, Key_F7 },
        { GDK_KEY_F8, Key_F8 },
        { GDK_KEY_F9, Key_F9 },
        { GDK_KEY_F10, Key_F10 },
        { GDK_KEY_F11, Key_F11 },
        { GDK_KEY_F12, Key_F12 },
        { GDK_KEY_Shift_L, Key_LeftShift },
        { GDK_KEY_Shift_R, Key_RightShift },
        { GDK_KEY_Control_L, Key_Control },
        { GDK_KEY_Control_R, Key_Control },
        { GDK_KEY_Caps_Lock, Key_CapsLock },
        { GDK_KEY_Meta_L, Key_Super },
        { GDK_KEY_Meta_R, Key_Super },
        { GDK_KEY_Alt_L, Key_Alt },
        { GDK_KEY_Alt_R, Key_Alt },
        { GDK_KEY_Super_L, Key_Super },
        { GDK_KEY_Super_R, Key_Super },
        { GDK_KEY_ISO_Enter, Key_Return },
        { GDK_KEY_3270_PrintScreen, Key_PrintScreen },
        { GDK_KEY_3270_Enter, Key_Return },
        { GDK_KEY_space, Key_Space },
        { GDK_KEY_exclam, Key_ExclamationPoint },
        { GDK_KEY_quotedbl, Key_DoubleQuote },
        { GDK_KEY_numbersign, Key_Hashtag },
        { GDK_KEY_dollar, Key_Dollar },
        { GDK_KEY_percent, Key_Percent },
        { GDK_KEY_ampersand, Key_Ampersand },
        { GDK_KEY_apostrophe, Key_Apostrophe },
        { GDK_KEY_parenleft, Key_LeftParen },
        { GDK_KEY_parenright, Key_RightParen },
        { GDK_KEY_asterisk, Key_Asterisk },
        { GDK_KEY_plus, Key_Plus },
        { GDK_KEY_comma, Key_Comma },
        { GDK_KEY_minus, Key_Minus },
        { GDK_KEY_period, Key_Period },
        { GDK_KEY_slash, Key_Slash },
        { GDK_KEY_0, Key_0 },
        { GDK_KEY_1, Key_1 },
        { GDK_KEY_2, Key_2 },
        { GDK_KEY_3, Key_3 },
        { GDK_KEY_4, Key_4 },
        { GDK_KEY_5, Key_5 },
        { GDK_KEY_6, Key_6 },
        { GDK_KEY_7, Key_7 },
        { GDK_KEY_8, Key_8 },
        { GDK_KEY_9, Key_9 },
        { GDK_KEY_colon, Key_Colon },
        { GDK_KEY_semicolon, Key_Semicolon },
        { GDK_KEY_less, Key_LessThan },
        { GDK_KEY_equal, Key_Equal },
        { GDK_KEY_greater, Key_GreaterThan },
        { GDK_KEY_question, Key_QuestionMark },
        { GDK_KEY_at, Key_AtSign },
        { GDK_KEY_A, Key_A },
        { GDK_KEY_B, Key_B },
        { GDK_KEY_C, Key_C },
        { GDK_KEY_D, Key_D },
        { GDK_KEY_E, Key_E },
        { GDK_KEY_F, Key_F },
        { GDK_KEY_G, Key_G },
        { GDK_KEY_H, Key_H },
        { GDK_KEY_I, Key_I },
        { GDK_KEY_J, Key_J },
        { GDK_KEY_K, Key_K },
        { GDK_KEY_L, Key_L },
        { GDK_KEY_M, Key_M },
        { GDK_KEY_N, Key_N },
        { GDK_KEY_O, Key_O },
        { GDK_KEY_P, Key_P },
        { GDK_KEY_Q, Key_Q },
        { GDK_KEY_R, Key_R },
        { GDK_KEY_S, Key_S },
        { GDK_KEY_T, Key_T },
        { GDK_KEY_U, Key_U },
        { GDK_KEY_V, Key_V },
        { GDK_KEY_W, Key_W },
        { GDK_KEY_X, Key_X },
        { GDK_KEY_Y, Key_Y },
        { GDK_KEY_Z, Key_Z },
        { GDK_KEY_bracketleft, Key_LeftBracket },
        { GDK_KEY_backslash, Key_Backslash },
        { GDK_KEY_bracketright, Key_RightBracket },
        { GDK_KEY_asciicircum, Key_Circumflex },
        { GDK_KEY_underscore, Key_Underscore },
        { GDK_KEY_grave, Key_Backtick },
        { GDK_KEY_a, Key_A },
        { GDK_KEY_b, Key_B },
        { GDK_KEY_c, Key_C },
        { GDK_KEY_d, Key_D },
        { GDK_KEY_e, Key_E },
        { GDK_KEY_f, Key_F },
        { GDK_KEY_g, Key_G },
        { GDK_KEY_h, Key_H },
        { GDK_KEY_i, Key_I },
        { GDK_KEY_j, Key_J },
        { GDK_KEY_k, Key_K },
        { GDK_KEY_l, Key_L },
        { GDK_KEY_m, Key_M },
        { GDK_KEY_n, Key_N },
        { GDK_KEY_o, Key_O },
        { GDK_KEY_p, Key_P },
        { GDK_KEY_q, Key_Q },
        { GDK_KEY_r, Key_R },
        { GDK_KEY_s, Key_S },
        { GDK_KEY_t, Key_T },
        { GDK_KEY_u, Key_U },
        { GDK_KEY_v, Key_V },
        { GDK_KEY_w, Key_W },
        { GDK_KEY_x, Key_X },
        { GDK_KEY_y, Key_Y },
        { GDK_KEY_z, Key_Z },
        { GDK_KEY_braceleft, Key_LeftBrace },
        { GDK_KEY_bar, Key_Pipe },
        { GDK_KEY_braceright, Key_RightBrace },
        { GDK_KEY_asciitilde, Key_Tilde },
    };

    for (Mapping const& mapping : mappings) {
        if (mapping.gdk_key == keyval)
            return mapping.serenity_key;
    }

    return Key_Invalid;
}

static unsigned translate_mouse_button(guint button)
{
    switch (button) {
    case GDK_BUTTON_PRIMARY:
        return 1;
    case GDK_BUTTON_SECONDARY:
        return 2;
    case GDK_BUTTON_MIDDLE:
        return 4;
    // TODO: apparently there are values for the forward/backward mouse buttons?
    default:
        return 0;
    }
}

static unsigned translate_buttons(GdkModifierType modifiers)
{
    unsigned buttons = 0;

    if (modifiers & GDK_BUTTON1_MASK)
        buttons |= 1;
    if (modifiers & GDK_BUTTON2_MASK)
        buttons |= 2;
    if (modifiers & GDK_BUTTON3_MASK)
        buttons |= 4;
    // TODO: ditto

    return buttons;
}

static unsigned translate_modifiers(GdkModifierType gdk_modifiers)
{
    unsigned modifiers = 0;

    if (gdk_modifiers & GDK_SHIFT_MASK)
        modifiers |= KeyModifier::Mod_Shift;
    if (gdk_modifiers & GDK_CONTROL_MASK)
        modifiers |= KeyModifier::Mod_Ctrl;
    if (gdk_modifiers & GDK_ALT_MASK)
        modifiers |= KeyModifier::Mod_Alt;
    if (gdk_modifiers & GDK_SUPER_MASK)
        modifiers |= KeyModifier::Mod_Super;

    return modifiers;
}

static void translate_state(GtkEventController* controller, unsigned* button, unsigned* buttons, unsigned* modifiers)
{
    if (GTK_IS_GESTURE_SINGLE(controller))
        *button = translate_mouse_button(gtk_gesture_single_get_current_button(GTK_GESTURE_SINGLE(controller)));
    else
        *button = 0;

    GdkModifierType gdk_modifiers = gtk_event_controller_get_current_event_state(controller);
    *buttons = translate_buttons(gdk_modifiers);
    *modifiers = translate_modifiers(gdk_modifiers);
}

static void on_click_pressed(GtkGestureClick* gesture_click, gint n_press, gdouble x, gdouble y, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    gtk_widget_grab_focus(GTK_WIDGET(self));

    unsigned button, buttons, modifiers;
    translate_state(GTK_EVENT_CONTROLLER(gesture_click), &button, &buttons, &modifiers);
    int page_x, page_y;
    translate_coordinates(self, x, y, &page_x, &page_y);

    if (button == 0) {
        gtk_gesture_set_state(GTK_GESTURE(gesture_click), GTK_EVENT_SEQUENCE_DENIED);
        return;
    }
    // gtk_gesture_set_state(GTK_GESTURE(gesture_click), GTK_EVENT_SEQUENCE_CLAIMED);

    if (n_press > 1) {
        // TODO doubleclick
    } else {
        self->impl->mouse_down(page_x, page_y, button, buttons, modifiers);
    }
}

static void on_click_released(GtkGestureClick* gesture_click, [[maybe_unused]] gint n_press, gdouble x, gdouble y, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    unsigned button, buttons, modifiers;
    translate_state(GTK_EVENT_CONTROLLER(gesture_click), &button, &buttons, &modifiers);
    int page_x, page_y;
    translate_coordinates(self, x, y, &page_x, &page_y);

    self->impl->mouse_up(page_x, page_y, button, buttons, modifiers);
}

static void on_motion(GtkEventControllerMotion* motion, gdouble x, gdouble y, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    unsigned button, buttons, modifiers;
    translate_state(GTK_EVENT_CONTROLLER(motion), &button, &buttons, &modifiers);
    int page_x, page_y;
    translate_coordinates(self, x, y, &page_x, &page_y);

    self->impl->mouse_move(page_x, page_y, buttons, modifiers);
}

static gboolean on_key_pressed(GtkEventControllerKey* controller_key, guint keyval, [[maybe_unused]] guint keycode, [[maybe_unused]] GdkModifierType state, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    KeyCode key = translate_key(keyval);
    gunichar point = gdk_keyval_to_unicode(keyval);
    unsigned button, buttons, modifiers;
    translate_state(GTK_EVENT_CONTROLLER(controller_key), &button, &buttons, &modifiers);

    self->impl->key_down(key, modifiers, point);
    // TODO: Propagate whether the web page has handled it.
    return false;
}

static gboolean on_key_released(GtkEventControllerKey* controller_key, guint keyval, [[maybe_unused]] guint keycode, [[maybe_unused]] GdkModifierType state, void* user_data)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(user_data);

    KeyCode key = translate_key(keyval);
    gunichar point = gdk_keyval_to_unicode(keyval);
    unsigned button, buttons, modifiers;
    translate_state(GTK_EVENT_CONTROLLER(controller_key), &button, &buttons, &modifiers);

    self->impl->key_up(key, modifiers, point);
    // TODO: Propagate whether the web page has handled it.
    return false;
}

static void ladybird_web_view_init(LadybirdWebView* self)
{
    gtk_widget_set_focusable(GTK_WIDGET(self), true);

    auto impl = LadybirdViewImpl::create(self).release_value_but_fixme_should_propagate_errors();
    // Let's be good boys and properly construct an OwnPtr in place
    // instead of assuming that it can be successfully initialized
    // to a nullptr state by zero-initializing its bytes as GObject does.
    new (&self->impl) OwnPtr<LadybirdViewImpl>(move(impl));

    // I don't know why both are required. Maybe because scale-factor happens
    // to have the right value from the start, and so :notify is never emitted?
    g_signal_connect(self, "realize", G_CALLBACK(on_scale_factor_change), nullptr);
    g_signal_connect(self, "notify::scale-factor", G_CALLBACK(on_scale_factor_change), nullptr);

    GtkGesture* gesture_click = gtk_gesture_click_new();
    g_signal_connect_object(gesture_click, "pressed", G_CALLBACK(on_click_pressed), self, G_CONNECT_DEFAULT);
    g_signal_connect_object(gesture_click, "released", G_CALLBACK(on_click_released), self, G_CONNECT_DEFAULT);
    gtk_widget_add_controller(GTK_WIDGET(self), GTK_EVENT_CONTROLLER(gesture_click));

    GtkEventController* motion = gtk_event_controller_motion_new();
    g_signal_connect_object(motion, "enter", G_CALLBACK(on_motion), self, G_CONNECT_DEFAULT);
    g_signal_connect_object(motion, "motion", G_CALLBACK(on_motion), self, G_CONNECT_DEFAULT);
    gtk_widget_add_controller(GTK_WIDGET(self), motion);

    GtkEventController* controller_key = gtk_event_controller_key_new();
    g_signal_connect_object(controller_key, "key-pressed", G_CALLBACK(on_key_pressed), self, G_CONNECT_DEFAULT);
    g_signal_connect_object(controller_key, "key-released", G_CALLBACK(on_key_released), self, G_CONNECT_DEFAULT);
    gtk_widget_add_controller(GTK_WIDGET(self), controller_key);

    self->bitmap_paintable = ladybird_bitmap_paintable_new();
    g_signal_connect_object(self->bitmap_paintable, "invalidate-contents", G_CALLBACK(gtk_widget_queue_draw), self, G_CONNECT_SWAPPED);
    self->favicon = ladybird_bitmap_paintable_new();
}

static void ladybird_web_view_dispose(GObject* object)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    self->impl.clear();

    g_clear_object(&self->bitmap_paintable);
    g_clear_object(&self->favicon);
    g_clear_object(&self->hadjustment);
    g_clear_object(&self->vadjustment);
    g_clear_pointer(&self->page_url, g_free);
    g_clear_pointer(&self->page_title, g_free);
    g_clear_pointer(&self->hovered_link, g_free);

    G_OBJECT_CLASS(ladybird_web_view_parent_class)->dispose(object);
}

static void ladybird_web_view_class_init(LadybirdWebViewClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);

    object_class->get_property = ladybird_web_view_get_property;
    object_class->set_property = ladybird_web_view_set_property;
    object_class->dispose = ladybird_web_view_dispose;

    g_object_class_override_property(object_class, PROP_HADJUSTMENT, "hadjustment");
    g_object_class_override_property(object_class, PROP_VADJUSTMENT, "vadjustment");
    g_object_class_override_property(object_class, PROP_HSCROLL_POLICY, "hscroll-policy");
    g_object_class_override_property(object_class, PROP_VSCROLL_POLICY, "vscroll-policy");

    constexpr GParamFlags param_flags = GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);
    constexpr GParamFlags ro_param_flags = GParamFlags(G_PARAM_READABLE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY);

    props[PROP_PAGE_TITLE] = g_param_spec_string("page-title", nullptr, nullptr, nullptr, param_flags);
    props[PROP_PAGE_URL] = g_param_spec_string("page-url", nullptr, nullptr, nullptr, param_flags);
    props[PROP_LOADING] = g_param_spec_boolean("loading", nullptr, nullptr, false, param_flags);
    props[PROP_ZOOM_PERCENT] = g_param_spec_uint("zoom-percent", nullptr, nullptr, 30, 500, 100, ro_param_flags);
    props[PROP_COOKIE_JAR] = g_param_spec_pointer("cookie-jar", nullptr, nullptr, param_flags);
    props[PROP_HOVERED_LINK] = g_param_spec_string("hovered-link", nullptr, nullptr, nullptr, param_flags);
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    widget_class->measure = ladybird_web_view_measure;
    widget_class->size_allocate = ladybird_web_view_size_allocate;
    widget_class->snapshot = ladybird_web_view_snapshot;
}

G_END_DECLS
