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

static void ladybird_web_view_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdWebView* self = LADYBIRD_WEB_VIEW(object);

    switch (prop_id) {
    case PROP_PAGE_TITLE:
        ladybird_web_view_set_page_title(self, g_value_get_string(value));
        break;

    case PROP_HADJUSTMENT:
        g_set_object(&self->hadjustment, (GtkAdjustment*)g_value_get_object(value));
        break;

    case PROP_VADJUSTMENT:
        g_set_object(&self->vadjustment, (GtkAdjustment*)g_value_get_object(value));
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

    object_class->get_property = ladybird_web_view_get_property;
    object_class->set_property = ladybird_web_view_set_property;
    object_class->dispose = ladybird_web_view_dispose;

    g_object_class_override_property(object_class, PROP_HADJUSTMENT, "hadjustment");
    g_object_class_override_property(object_class, PROP_VADJUSTMENT, "vadjustment");
    g_object_class_override_property(object_class, PROP_HSCROLL_POLICY, "hscroll-policy");
    g_object_class_override_property(object_class, PROP_VSCROLL_POLICY, "vscroll-policy");

    props[PROP_PAGE_TITLE] = g_param_spec_string("page-title", nullptr, nullptr, nullptr, GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);
}

G_END_DECLS
