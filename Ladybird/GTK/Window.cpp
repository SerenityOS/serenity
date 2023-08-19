#include "Window.h"
#include "Application.h"
#include "BitmapPaintable.h"
#include "WebView.h"

struct _LadybirdWindow {
    AdwApplicationWindow parent_instance;

    AdwTabOverview* tab_overview;
    AdwTabView* tab_view;
    LadybirdWebView* last_selected_web_view;
    GtkEntry* url_entry;

    gulong page_url_changed_id;
    bool incognito;
};

enum {
    PROP_0,
    PROP_INCOGNITO,
    NUM_PROPS,
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdWindow, ladybird_window, ADW_TYPE_APPLICATION_WINDOW)

static void ladybird_window_dispose(GObject* object)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(object);

    if (self->last_selected_web_view && self->page_url_changed_id)
        g_signal_handler_disconnect(self->last_selected_web_view, self->page_url_changed_id);
    gtk_widget_dispose_template(GTK_WIDGET(self), LADYBIRD_TYPE_WINDOW);

    G_OBJECT_CLASS(ladybird_window_parent_class)->dispose(object);
}

static void update_favicon(LadybirdBitmapPaintable* favicon_paintable, [[maybe_unused]] GParamSpec* pspec, void* data)
{
    AdwTabPage* tab_page = ADW_TAB_PAGE(data);
    GdkTexture* texture = ladybird_bitmap_paintable_get_texture(favicon_paintable);
    adw_tab_page_set_icon(tab_page, G_ICON(texture));
}

static AdwTabPage* open_new_tab(LadybirdWindow* self)
{
    LadybirdApplication* app = LADYBIRD_APPLICATION(gtk_window_get_application(GTK_WINDOW(self)));
    Browser::CookieJar* cookie_jar = self->incognito
        ? ladybird_application_get_incognito_cookie_jar(app)
        : ladybird_application_get_cookie_jar(app);

    LadybirdWebView* web_view = (LadybirdWebView*)g_object_new(LADYBIRD_TYPE_WEB_VIEW,
        "cookie-jar", cookie_jar,
        nullptr);
    gtk_widget_add_css_class(GTK_WIDGET(web_view), "view");

    GtkWidget* scrolled_window = gtk_scrolled_window_new();
    gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled_window), GTK_WIDGET(web_view));

    AdwTabPage* tab_page = adw_tab_view_append(self->tab_view, scrolled_window);
    adw_tab_page_set_title(tab_page, "New tab");
    g_object_bind_property(web_view, "page-title", tab_page, "title", G_BINDING_DEFAULT);
    g_object_bind_property(web_view, "loading", tab_page, "loading", G_BINDING_DEFAULT);

    GdkPaintable* favicon_paintable = ladybird_web_view_get_favicon(web_view);
    g_signal_connect_object(favicon_paintable, "notify::texture", G_CALLBACK(update_favicon), tab_page, G_CONNECT_DEFAULT);

    adw_tab_view_set_selected_page(self->tab_view, tab_page);
    // g_object_unref(web_view);
    // g_object_unref(scrolled_window);
    return tab_page;
}

static void ladybird_window_get_property(GObject* object, guint prop_id, GValue* value, GParamSpec* pspec)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(object);

    switch (prop_id) {
    case PROP_INCOGNITO:
        g_value_set_boolean(value, self->incognito);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void ladybird_window_set_property(GObject* object, guint prop_id, GValue const* value, GParamSpec* pspec)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(object);

    switch (prop_id) {
    case PROP_INCOGNITO:
        self->incognito = g_value_get_boolean(value);
        // No need to emit notify, since it's construct-only.
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void win_new_tab_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);
    open_new_tab(self);
}

static void win_open_file_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    GtkFileDialog* dialog = gtk_file_dialog_new();
    gtk_file_dialog_open_multiple(
        dialog, GTK_WINDOW(self), nullptr, +[](GObject* object, GAsyncResult* res, void* user_data) {
            LadybirdWindow* self = LADYBIRD_WINDOW(user_data);
            GtkFileDialog* dialog = GTK_FILE_DIALOG(object);
            GError* error = nullptr;
            GListModel* selected_files = gtk_file_dialog_open_multiple_finish(dialog, res, &error);

            if (g_error_matches(error, GTK_DIALOG_ERROR, GTK_DIALOG_ERROR_DISMISSED)) {
                g_error_free(error);
                return;
            } else if (error) {
                GtkWidget* message_dialog = adw_message_dialog_new(GTK_WINDOW(self), "Failed to pick file", error->message);
                g_error_free(error);
                adw_message_dialog_add_response(ADW_MESSAGE_DIALOG(message_dialog), "ok", "OK");
                gtk_window_present(GTK_WINDOW(message_dialog));
                return;
            }

            for (size_t i = 0; i < g_list_model_get_n_items(selected_files); i++) {
                GFile* file = G_FILE(g_list_model_get_item(selected_files, i));
                ladybird_window_open_file(self, file);
            }
            g_object_unref(selected_files);
        },
        self);
    g_object_unref(dialog);
}

static void tab_close_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    if (adw_tab_view_get_n_pages(self->tab_view) <= 1) {
        // If this was the last page, close the window.
        g_idle_add_once(
            +[](void* user_data) {
                gtk_window_close(GTK_WINDOW(user_data));
            },
            self);
        return;
    }

    AdwTabPage* tab_page = adw_tab_view_get_selected_page(self->tab_view);
    if (tab_page)
        adw_tab_view_close_page(self->tab_view, tab_page);
}

static LadybirdWebView* get_web_view_from_tab_page(AdwTabPage* tab_page)
{
    GtkScrolledWindow* scrolled_window = GTK_SCROLLED_WINDOW(adw_tab_page_get_child(tab_page));
    return LADYBIRD_WEB_VIEW(gtk_scrolled_window_get_child(scrolled_window));
}

static LadybirdWebView* ladybird_window_get_current_page(LadybirdWindow* self)
{
    AdwTabPage* tab_page = adw_tab_view_get_selected_page(self->tab_view);
    if (!tab_page)
        return nullptr;
    return get_web_view_from_tab_page(tab_page);
}

static void on_url_entered(LadybirdWindow* self, GtkEntry* url_entry)
{
    LadybirdWebView* web_view = ladybird_window_get_current_page(self);
    if (!web_view)
        return;

    char const* url = gtk_entry_buffer_get_text(gtk_entry_get_buffer(url_entry));
    ladybird_web_view_load_url(web_view, url);
}

static AdwTabView* on_create_window(LadybirdWindow* self)
{
    GtkApplication* app = gtk_window_get_application(GTK_WINDOW(self));
    LadybirdWindow* new_window = ladybird_window_new(LADYBIRD_APPLICATION(app), false, self->incognito);
    gtk_window_present(GTK_WINDOW(new_window));
    return new_window->tab_view;
}

static void on_page_url_changed(LadybirdWindow* self)
{
    GtkEntryBuffer* entry_buffer = gtk_entry_get_buffer(self->url_entry);
    LadybirdWebView* web_view = ladybird_window_get_current_page(self);
    if (!web_view) {
        gtk_entry_buffer_delete_text(entry_buffer, 0, -1);
        return;
    }

    char const* url = ladybird_web_view_get_page_url(web_view);
    if (url)
        gtk_entry_buffer_set_text(entry_buffer, url, -1);
    else
        gtk_entry_buffer_delete_text(entry_buffer, 0, -1);
}

static void on_selected_page_changed(LadybirdWindow* self)
{
    if (self->last_selected_web_view && self->page_url_changed_id)
        g_signal_handler_disconnect(self->last_selected_web_view, self->page_url_changed_id);
    self->page_url_changed_id = 0;
    LadybirdWebView* web_view = self->last_selected_web_view = ladybird_window_get_current_page(self);
    if (!web_view)
        return;

    self->page_url_changed_id = g_signal_connect_object(web_view, "notify::page-url", G_CALLBACK(on_page_url_changed), self, G_CONNECT_SWAPPED);
    on_page_url_changed(self);
}

static void page_zoom_in_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    ladybird_web_view_zoom_in(ladybird_window_get_current_page(self));
}

static void page_zoom_out_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    ladybird_web_view_zoom_out(ladybird_window_get_current_page(self));
}

static void page_zoom_reset_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    ladybird_web_view_zoom_reset(ladybird_window_get_current_page(self));
}

static void page_reload_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    LadybirdWebView* web_view = ladybird_window_get_current_page(self);
    if (!web_view)
        return;

    char const* url = ladybird_web_view_get_page_url(web_view);
    ladybird_web_view_load_url(web_view, url);
}

void ladybird_window_open_file(LadybirdWindow* self, GFile* file)
{
    g_return_if_fail(LADYBIRD_IS_WINDOW(self));
    g_return_if_fail(G_IS_FILE(file));

    char* uri = g_file_get_uri(file);
    AdwTabPage* tab_page = open_new_tab(self);
    GtkScrolledWindow* scrolled_window = GTK_SCROLLED_WINDOW(adw_tab_page_get_child(tab_page));
    LadybirdWebView* web_view = LADYBIRD_WEB_VIEW(gtk_scrolled_window_get_child(scrolled_window));
    ladybird_web_view_load_url(web_view, uri);
    g_free(uri);
}

static void ladybird_window_init(LadybirdWindow* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    g_type_ensure(LADYBIRD_TYPE_WEB_VIEW);
    gtk_widget_init_template(widget);
}

static void ladybird_window_class_init(LadybirdWindowClass* klass)
{
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->get_property = ladybird_window_get_property;
    object_class->set_property = ladybird_window_set_property;
    object_class->dispose = ladybird_window_dispose;

    props[PROP_INCOGNITO] = g_param_spec_boolean("incognito", nullptr, nullptr, false,
        GParamFlags(G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS | G_PARAM_CONSTRUCT_ONLY));
    g_object_class_install_properties(object_class, NUM_PROPS, props);

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/ladybird-gtk4/window.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_overview);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_view);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, url_entry);
    gtk_widget_class_bind_template_callback(widget_class, open_new_tab);
    gtk_widget_class_bind_template_callback(widget_class, on_url_entered);
    gtk_widget_class_bind_template_callback(widget_class, on_create_window);
    gtk_widget_class_bind_template_callback(widget_class, on_selected_page_changed);

    gtk_widget_class_install_action(widget_class, "win.new-tab", nullptr, win_new_tab_action);
    gtk_widget_class_install_action(widget_class, "win.open-file", nullptr, win_open_file_action);
    gtk_widget_class_install_action(widget_class, "tab.close", nullptr, tab_close_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_t, GDK_CONTROL_MASK, "win.new-tab", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_o, GDK_CONTROL_MASK, "win.open-file", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_w, GDK_CONTROL_MASK, "tab.close", nullptr);

    gtk_widget_class_install_action(widget_class, "page.zoom-in", nullptr, page_zoom_in_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-out", nullptr, page_zoom_out_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-reset", nullptr, page_zoom_reset_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_equal, GDK_CONTROL_MASK, "page.zoom-in", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_minus, GDK_CONTROL_MASK, "page.zoom-out", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_0, GDK_CONTROL_MASK, "page.zoom-reset", nullptr);

    gtk_widget_class_install_action(widget_class, "page.reload-page", nullptr, page_reload_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_F5, GdkModifierType(0), "page.reload-page", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_r, GDK_CONTROL_MASK, "page.reload-page", nullptr);
}

LadybirdWindow* ladybird_window_new(LadybirdApplication* app, bool add_initial_tab, bool incognito)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(g_object_new(LADYBIRD_TYPE_WINDOW,
        "application", app,
        "incognito", (gboolean)incognito,
        nullptr));

    if (add_initial_tab)
        open_new_tab(self);

    return self;
}

G_END_DECLS
