#include "Tab.h"
#include "WebView.h"

struct _LadybirdTab {
    GtkWidget parent_instance;

    LadybirdWebView* web_view;
};

enum {
    PROP_0,
    PROP_WEB_VIEW,
    NUM_PROPS,
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdTab, ladybird_tab, GTK_TYPE_WIDGET)

LadybirdWebView* ladybird_tab_get_web_view(LadybirdTab* self)
{
    g_return_val_if_fail(LADYBIRD_IS_TAB(self), nullptr);

    return self->web_view;
}

static void ladybird_tab_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdTab* self = LADYBIRD_TAB(object);

    switch (prop_id) {
    case PROP_WEB_VIEW:
        g_value_set_object(value, self->web_view);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_tab_set_property(GObject* object, guint prop_id, [[maybe_unused]] GValue const* value, GParamSpec* pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void ladybird_tab_dispose(GObject* object)
{
    gtk_widget_dispose_template(GTK_WIDGET(object), LADYBIRD_TYPE_TAB);

    G_OBJECT_CLASS(ladybird_tab_parent_class)->dispose(object);
}

static void ladybird_tab_init(LadybirdTab* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    g_type_ensure(LADYBIRD_TYPE_WEB_VIEW);
    gtk_widget_init_template(widget);
}

static void ladybird_tab_class_init(LadybirdTabClass* klass)
{
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_tab_get_property;
    object_class->set_property = ladybird_tab_set_property;
    object_class->dispose = ladybird_tab_dispose;

    props[PROP_WEB_VIEW] = g_param_spec_object("web-view", nullptr, nullptr, LADYBIRD_TYPE_WEB_VIEW, GParamFlags(G_PARAM_READABLE | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/ladybird-gtk4/tab.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdTab, web_view);

    gtk_widget_class_set_layout_manager_type(widget_class, GTK_TYPE_BIN_LAYOUT);
}

LadybirdTab* ladybird_tab_new(void)
{
    return LADYBIRD_TAB(g_object_new(LADYBIRD_TYPE_TAB, nullptr));
}

G_END_DECLS
