#include "Tab.h"
#include "WebView.h"

struct _LadybirdTab {
    GtkWidget parent_instance;

    GtkOverlay* overlay;
    LadybirdWebView* web_view;
    GtkLabel* hovered_link_label;
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

static void on_hovered_link_change(LadybirdTab* self)
{
    g_return_if_fail(LADYBIRD_IS_TAB(self));

    char const* hovered_link = ladybird_web_view_get_hovered_link(self->web_view);
    if (hovered_link) {
        gtk_label_set_label(self->hovered_link_label, hovered_link);
        gtk_widget_remove_css_class(GTK_WIDGET(self->hovered_link_label), "hidden");
    } else {
        // Do not unset the label.
        gtk_widget_add_css_class(GTK_WIDGET(self->hovered_link_label), "hidden");
    }
}

static void set_label_halign(LadybirdTab* self, GtkAlign new_halign)
{
    GtkWidget* label = GTK_WIDGET(self->hovered_link_label);
    // It's not enough to call gtk_widget_set_halign(), as that only calls gtk_widget_queue_allocate(),
    // not gtk_widget_queue_layout(), since normally align doesn't influence layout. Not so for the overlay!
    if (gtk_widget_get_halign(label) == new_halign)
        return;
    gtk_widget_set_halign(label, new_halign);
    gtk_widget_queue_resize(label);
}

static void on_motion(LadybirdTab* self, gdouble x, gdouble y)
{
    g_return_if_fail(LADYBIRD_IS_TAB(self));

    graphene_point_t hovered_point = GRAPHENE_POINT_INIT(static_cast<float>(x), static_cast<float>(y));
    graphene_rect_t label_bounds;
    bool converted = gtk_widget_compute_bounds(GTK_WIDGET(self->hovered_link_label), GTK_WIDGET(self), &label_bounds);
    if (!converted)
        return;

    // Force the rect to the default location (as if halign = GTK_ALIGN_START).
    bool ltr = gtk_widget_get_direction(GTK_WIDGET(self)) == GTK_TEXT_DIR_LTR;
    if (ltr) {
        label_bounds.origin.x = 0;
    } else {
        label_bounds.origin.x = gtk_widget_get_width(GTK_WIDGET(self)) - label_bounds.size.width;
    }

    GtkAlign new_align = graphene_rect_contains_point(&label_bounds, &hovered_point) ? GTK_ALIGN_END : GTK_ALIGN_START;
    set_label_halign(self, new_align);
}

static void on_leave(LadybirdTab* self)
{
    g_return_if_fail(LADYBIRD_IS_TAB(self));

    set_label_halign(self, GTK_ALIGN_START);
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

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/Ladybird-gtk4/tab.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdTab, overlay);
    gtk_widget_class_bind_template_child(widget_class, LadybirdTab, web_view);
    gtk_widget_class_bind_template_child(widget_class, LadybirdTab, hovered_link_label);
    gtk_widget_class_bind_template_callback(widget_class, on_hovered_link_change);
    gtk_widget_class_bind_template_callback(widget_class, on_motion);
    gtk_widget_class_bind_template_callback(widget_class, on_leave);

    gtk_widget_class_set_layout_manager_type(widget_class, GTK_TYPE_BIN_LAYOUT);
}

LadybirdTab* ladybird_tab_new(void)
{
    return LADYBIRD_TAB(g_object_new(LADYBIRD_TYPE_TAB, nullptr));
}

G_END_DECLS
