#include "WebView.h"
#include "ViewImpl.h"

struct _LadybirdWebView {
    GtkWidget parent_instance;

    OwnPtr<LadybirdViewImpl> impl;
    GtkScrollablePolicy hscroll_policy;
    GtkAdjustment* hadjustment;
    GtkScrollablePolicy vscroll_policy;
    GtkAdjustment* vadjustment;

    char* page_title;
    int page_width;
    int page_height;
};

enum {
    PROP_0,
    PROP_PAGE_TITLE,
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

static void ladybird_web_view_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    switch (prop_id) {
    case PROP_PAGE_TITLE:
        g_value_set_string(value, self->page_title);
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

static void ladybird_web_view_set_adjustment(LadybirdWebView* self, GtkOrientation orientation, GtkAdjustment* adjustment)
{
    switch (orientation) {
    case GTK_ORIENTATION_HORIZONTAL:
        g_set_object(&self->hadjustment, adjustment);
        g_object_notify(G_OBJECT(self), "hadjustment");
        break;

    case GTK_ORIENTATION_VERTICAL:
        g_set_object(&self->vadjustment, adjustment);
        g_object_notify(G_OBJECT(self), "vadjustment");
        break;

    default:
        VERIFY_NOT_REACHED();
    }

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

    case PROP_HADJUSTMENT:
        ladybird_web_view_set_adjustment(self, GTK_ORIENTATION_HORIZONTAL, (GtkAdjustment*)g_value_get_object(value));
        break;

    case PROP_VADJUSTMENT:
        ladybird_web_view_set_adjustment(self, GTK_ORIENTATION_VERTICAL, (GtkAdjustment*)g_value_get_object(value));
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

    switch (orientation) {
    case GTK_ORIENTATION_HORIZONTAL:
        *minimum = 0;
        *natural = self->page_width;
        break;

    case GTK_ORIENTATION_VERTICAL:
        *minimum = 0;
        *natural = self->page_height;
        break;

    default:
        VERIFY_NOT_REACHED();
    }

    *minimum_baseline = *natural_baseline = -1;
}

static void ladybird_web_view_size_allocate(GtkWidget* widget, int width, int height, [[maybe_unused]] int baseline)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(widget);

    double hadj = gtk_adjustment_get_value(self->hadjustment);
    double vadj = gtk_adjustment_get_value(self->vadjustment);

    // TODO: CSS pixels?
    self->impl->set_viewport_rect(hadj, vadj, width, height);

    gtk_adjustment_configure(self->hadjustment, hadj, 0, self->page_width, width * 0.1, width * 0.9, width);
    gtk_adjustment_configure(self->vadjustment, vadj, 0, self->page_height, height * 0.1, height * 0.9, height);
}

static void ladybird_web_view_init(LadybirdWebView* self)
{
    auto impl = LadybirdViewImpl::create(self).release_value_but_fixme_should_propagate_errors();
    // Let's be good boys and properly construct an OwnPtr in place
    // instead of assuming that it can be successfully initialized
    // to a nullptr state by zero-initializing its bytes as GObject does.
    new (&self->impl) OwnPtr<LadybirdViewImpl>(move(impl));

    self->impl->load_html("<html><title>Title from HTML :^)</title><body>This is some <b>HTML</b>!</body></html>"sv, "http://example.com"sv);
}

static void ladybird_web_view_dispose(GObject* object)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    g_clear_object(&self->hadjustment);
    g_clear_object(&self->vadjustment);
    self->impl.clear();

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

    props[PROP_PAGE_TITLE] = g_param_spec_string("page-title", nullptr, nullptr, nullptr, GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    widget_class->measure = ladybird_web_view_measure;
    widget_class->size_allocate = ladybird_web_view_size_allocate;
}

G_END_DECLS
