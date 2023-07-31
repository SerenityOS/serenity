#include "WebContentView.h"
#include "ViewImpl.h"

struct _LadybirdWebContentView {
    GtkWidget parent_instance;

    OwnPtr<LadybirdViewImpl> impl;
    GtkScrollablePolicy hscroll_policy;
    GtkAdjustment* hadjustment;
    GtkScrollablePolicy vscroll_policy;
    GtkAdjustment* vadjustment;
};

enum {
    PROP_0,
    PROP_HADJUSTMENT,
    PROP_VADJUSTMENT,
    PROP_HSCROLL_POLICY,
    PROP_VSCROLL_POLICY
};

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE_WITH_CODE(LadybirdWebContentView, ladybird_web_content_view, GTK_TYPE_WIDGET,
    G_IMPLEMENT_INTERFACE(GTK_TYPE_SCROLLABLE, NULL))

static void ladybird_web_content_view_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdWebContentView* self = LADYBIRD_WEB_CONTENT_VIEW(object);

    switch (prop_id) {
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

static void ladybird_web_content_view_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdWebContentView* self = LADYBIRD_WEB_CONTENT_VIEW(object);

    switch (prop_id) {
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

static void ladybird_web_content_view_init(LadybirdWebContentView* self)
{
    auto impl = LadybirdViewImpl::create(self).release_value_but_fixme_should_propagate_errors();
    // Let's be good boys and properly construct an OwnPtr in place
    // instead of assuming that it can be successfully initialized
    // to a nullptr state by zero-initializing its bytes as GObject does.
    new (&self->impl) OwnPtr<LadybirdViewImpl>(move(impl));
}

static void ladybird_web_content_view_dispose(GObject* object)
{
    LadybirdWebContentView* self = LADYBIRD_WEB_CONTENT_VIEW(object);

    g_clear_object(&self->hadjustment);
    g_clear_object(&self->vadjustment);
    if (self->impl) {
        self->impl.clear();
    }

    G_OBJECT_CLASS(ladybird_web_content_view_parent_class)->dispose(object);
}

static void ladybird_web_content_view_class_init(LadybirdWebContentViewClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_web_content_view_get_property;
    object_class->set_property = ladybird_web_content_view_set_property;
    object_class->dispose = ladybird_web_content_view_dispose;

    g_object_class_override_property(object_class, PROP_HADJUSTMENT, "hadjustment");
    g_object_class_override_property(object_class, PROP_VADJUSTMENT, "vadjustment");
    g_object_class_override_property(object_class, PROP_HSCROLL_POLICY, "hscroll-policy");
    g_object_class_override_property(object_class, PROP_VSCROLL_POLICY, "vscroll-policy");
}

G_END_DECLS
