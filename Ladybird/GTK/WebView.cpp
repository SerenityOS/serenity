#include "WebView.h"
#include "ViewImpl.h"

struct _LadybirdWebView {
    GtkWidget parent_instance;

    OwnPtr<LadybirdViewImpl> impl;
    GdkTexture* texture;
    GtkScrollablePolicy hscroll_policy;
    GtkAdjustment* hadjustment;
    GtkScrollablePolicy vscroll_policy;
    GtkAdjustment* vadjustment;

    char* page_url;
    char* page_title;
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
    NUM_PROPS,

    PROP_HADJUSTMENT,
    PROP_VADJUSTMENT,
    PROP_HSCROLL_POLICY,
    PROP_VSCROLL_POLICY
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdWebView, ladybird_web_view, GTK_TYPE_WIDGET,
    G_IMPLEMENT_INTERFACE(GTK_TYPE_SCROLLABLE, NULL))

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

void ladybird_web_view_push_bitmap(LadybirdWebView* self, Gfx::Bitmap const* bitmap, int width, int height)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    if (bitmap == nullptr) {
        g_clear_object(&self->texture);
        gtk_widget_queue_draw(GTK_WIDGET(self));
        return;
    }

    GdkMemoryFormat format;
    switch (bitmap->format()) {
    case Gfx::BitmapFormat::BGRA8888:
        format = GDK_MEMORY_B8G8R8A8;
        break;
    case Gfx::BitmapFormat::RGBA8888:
        format = GDK_MEMORY_R8G8B8A8;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    // TODO: Try to avoid excessive copying here?
    GBytes* bytes = g_bytes_new(bitmap->scanline_u8(0), bitmap->size_in_bytes());
    GdkTexture* texture = gdk_memory_texture_new(width, height, format, bytes, bitmap->pitch());
    g_bytes_unref(bytes);
    g_set_object(&self->texture, texture);
    g_object_unref(texture);

    gtk_widget_queue_draw(GTK_WIDGET(self));
}

static void ladybird_web_view_snapshot(GtkWidget* widget, GtkSnapshot* snapshot)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(widget);

    if (self->texture == nullptr)
        return;

    int texture_width, texture_height;
    scale_size_down(self, gdk_texture_get_width(self->texture), gdk_texture_get_height(self->texture), &texture_width, &texture_height);
    graphene_rect_t bounds = GRAPHENE_RECT_INIT(0, 0, (float)texture_width, (float)texture_height);
    gtk_snapshot_append_texture(snapshot, self->texture, &bounds);
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

static void ladybird_web_view_init(LadybirdWebView* self)
{
    auto impl = LadybirdViewImpl::create(self).release_value_but_fixme_should_propagate_errors();
    // Let's be good boys and properly construct an OwnPtr in place
    // instead of assuming that it can be successfully initialized
    // to a nullptr state by zero-initializing its bytes as GObject does.
    new (&self->impl) OwnPtr<LadybirdViewImpl>(move(impl));

    // I don't know why both are required. Maybe because scale-factor happens
    // to have the right value from the start, and so :notify is never emitted?
    g_signal_connect(self, "realize", G_CALLBACK(on_scale_factor_change), nullptr);
    g_signal_connect(self, "notify::scale-factor", G_CALLBACK(on_scale_factor_change), nullptr);

    gtk_widget_set_overflow(GTK_WIDGET(self), GTK_OVERFLOW_HIDDEN);

    GtkGesture* gesture_click = gtk_gesture_click_new();
    g_signal_connect_object(gesture_click, "pressed", G_CALLBACK(on_click_pressed), self, G_CONNECT_DEFAULT);
    g_signal_connect_object(gesture_click, "released", G_CALLBACK(on_click_released), self, G_CONNECT_DEFAULT);
    gtk_widget_add_controller(GTK_WIDGET(self), GTK_EVENT_CONTROLLER(gesture_click));

    GtkEventController* motion = gtk_event_controller_motion_new();
    g_signal_connect_object(motion, "enter", G_CALLBACK(on_motion), self, G_CONNECT_DEFAULT);
    g_signal_connect_object(motion, "motion", G_CALLBACK(on_motion), self, G_CONNECT_DEFAULT);
    gtk_widget_add_controller(GTK_WIDGET(self), motion);

    self->impl->load_html("<html><title>Title from HTML :^)</title><body><p>This is some <b>HTML</b>!</p><p><font color=\"red\">This should be red</font>, <font color=\"green\">this should be green</font>, <font color=\"blue\">this should be blue</font></p><form><input type=\"text\"><br><input type=\"radio\">A radio button<br><input type=\"checkbox\">A checkbox<br><input type=\"submit\" value=\"A button (don't click tho)\"></form>Long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long</body></html>"sv, "http://example.com"sv);
}

static void ladybird_web_view_dispose(GObject* object)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    self->impl.clear();

    g_clear_object(&self->texture);
    g_clear_object(&self->hadjustment);
    g_clear_object(&self->vadjustment);
    g_clear_pointer(&self->page_url, g_free);
    g_clear_pointer(&self->page_title, g_free);

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
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    widget_class->measure = ladybird_web_view_measure;
    widget_class->size_allocate = ladybird_web_view_size_allocate;
    widget_class->snapshot = ladybird_web_view_snapshot;
}

G_END_DECLS
