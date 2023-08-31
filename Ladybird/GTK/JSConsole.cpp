#include "JSConsole.h"
#include "ViewImpl.h"
#include "WebView.h"
#include <AK/OwnPtr.h>
#include <LibWebView/ConsoleClient.h>

struct _LadybirdJSConsole {
    GtkWidget parent_instance;

    LadybirdWebView* web_view;
    OwnPtr<WebView::ConsoleClient> console_client;

    GtkScrolledWindow* scrolled_window;
    GtkEntry* entry;
    LadybirdWebView* console_web_view;
};

enum {
    PROP_0,
    PROP_WEB_VIEW,
    NUM_PROPS
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdJSConsole, ladybird_js_console, GTK_TYPE_WIDGET)

LadybirdWebView* ladybird_js_console_get_web_view(LadybirdJSConsole* self);

static void on_entered(LadybirdJSConsole* self)
{
    g_return_if_fail(LADYBIRD_IS_JS_CONSOLE(self));

    char const* input = gtk_entry_buffer_get_text(gtk_entry_get_buffer(self->entry));
    String ak_input = MUST(String::from_utf8(AK::StringView(input, strlen(input))));
    self->console_client->execute(move(ak_input));

    GtkEntryBuffer* entry_buffer = gtk_entry_get_buffer(self->entry);
    gtk_entry_buffer_delete_text(entry_buffer, 0, -1);
}

static void ladybird_js_console_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdJSConsole* self = LADYBIRD_JS_CONSOLE(object);

    switch (prop_id) {
    case PROP_WEB_VIEW:
        g_value_set_object(value, self->web_view);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_js_console_set_web_view(LadybirdJSConsole* self, LadybirdWebView* web_view)
{
    g_assert(self->web_view == nullptr);
    self->web_view = web_view;
    g_object_bind_property(web_view, "cookie-jar", self->console_web_view, "cookie-jar", G_BINDING_SYNC_CREATE);

    WebView::ViewImplementation* impl = ladybird_web_view_get_impl(web_view);
    WebView::ViewImplementation* console_impl = ladybird_web_view_get_impl(self->console_web_view);
    self->console_client = make<WebView::ConsoleClient>(*impl, *console_impl);

    g_object_notify_by_pspec(G_OBJECT(self), props[PROP_WEB_VIEW]);
}

static void ladybird_js_console_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdJSConsole* self = LADYBIRD_JS_CONSOLE(object);

    switch (prop_id) {
    case PROP_WEB_VIEW:
        ladybird_js_console_set_web_view(self, LADYBIRD_WEB_VIEW(g_value_get_object(value)));
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_js_console_init(LadybirdJSConsole* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    gtk_orientable_set_orientation(GTK_ORIENTABLE(gtk_widget_get_layout_manager(widget)), GTK_ORIENTATION_VERTICAL);
    g_type_ensure(LADYBIRD_TYPE_WEB_VIEW);
    gtk_widget_init_template(widget);
    new (&self->console_client) OwnPtr<WebView::ConsoleClient>;
}

static void ladybird_js_console_dispose(GObject* object)
{
    LadybirdJSConsole* self = LADYBIRD_JS_CONSOLE(object);

    self->console_client.clear();
    gtk_widget_dispose_template(GTK_WIDGET(object), LADYBIRD_TYPE_JS_CONSOLE);

    G_OBJECT_CLASS(ladybird_js_console_parent_class)->dispose(object);
}

static void ladybird_js_console_class_init(LadybirdJSConsoleClass* klass)
{
    GObjectClass* object_class = G_OBJECT_CLASS(klass);
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);

    object_class->get_property = ladybird_js_console_get_property;
    object_class->set_property = ladybird_js_console_set_property;
    object_class->dispose = ladybird_js_console_dispose;

    props[PROP_WEB_VIEW] = g_param_spec_object("web-view", nullptr, nullptr, LADYBIRD_TYPE_WEB_VIEW, GParamFlags(G_PARAM_READWRITE | G_PARAM_CONSTRUCT_ONLY | G_PARAM_STATIC_STRINGS | G_PARAM_EXPLICIT_NOTIFY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    gtk_widget_class_set_css_name(widget_class, "js-console");

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/Ladybird-gtk4/js-console.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdJSConsole, entry);
    gtk_widget_class_bind_template_child(widget_class, LadybirdJSConsole, console_web_view);
    gtk_widget_class_bind_template_child(widget_class, LadybirdJSConsole, scrolled_window);
    gtk_widget_class_bind_template_callback(widget_class, on_entered);

    gtk_widget_class_set_layout_manager_type(widget_class, GTK_TYPE_BOX_LAYOUT);
}

LadybirdJSConsole* ladybird_js_console_new(LadybirdWebView* web_view)
{
    g_return_val_if_fail(LADYBIRD_IS_WEB_VIEW(web_view), nullptr);

    return LADYBIRD_JS_CONSOLE(g_object_new(LADYBIRD_TYPE_JS_CONSOLE,
        "web-view", web_view,
        nullptr));
}

G_END_DECLS
