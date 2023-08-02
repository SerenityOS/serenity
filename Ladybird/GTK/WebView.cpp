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

void ladybird_web_view_push_bitmap(LadybirdWebView* self, Gfx::Bitmap const* bitmap, int width, int height)
{
    g_return_if_fail(LADYBIRD_IS_WEB_VIEW(self));

    dbgln("ladybird_web_view_push_bitmap size {}x{}", width, height);

    if (bitmap == nullptr) {
        g_clear_object(&self->texture);
        gtk_widget_queue_draw(GTK_WIDGET(self));
        return;
    }

    GdkMemoryFormat format;
#if 0
    switch (bitmap->format()) {
    case Gfx::BitmapFormat::BGRx8888:
        format = GDK_MEMORY_B8G8R8;
        break;
    case Gfx::BitmapFormat::BGRA8888:
        format = GDK_MEMORY_B8G8R8A8;
        break;
    case Gfx::BitmapFormat::RGBA8888:
        format = GDK_MEMORY_R8G8B8A8;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
#else
    format = GDK_MEMORY_B8G8R8A8;
#endif

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

    self->impl->load_html("<html><title>Title from HTML :^)</title><body><p>This is some <b>HTML</b>!</p><p><font color=\"red\">This should be red</font>, <font color=\"green\">this should be green</font>, <font color=\"blue\">this should be blue</font></p>Long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long</body></html>"sv, "http://example.com"sv);
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

    props[PROP_PAGE_TITLE] = g_param_spec_string("page-title", nullptr, nullptr, nullptr, param_flags);
    props[PROP_PAGE_URL] = g_param_spec_string("page-url", nullptr, nullptr, nullptr, param_flags);
    props[PROP_LOADING] = g_param_spec_boolean("loading", nullptr, nullptr, false, param_flags);
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    widget_class->measure = ladybird_web_view_measure;
    widget_class->size_allocate = ladybird_web_view_size_allocate;
    widget_class->snapshot = ladybird_web_view_snapshot;
}

G_END_DECLS
