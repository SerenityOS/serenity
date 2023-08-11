#include "Window.h"
#include "WebView.h"

struct _LadybirdWindow {
    AdwApplicationWindow parent_instance;

    AdwTabOverview* tab_overview;
    AdwTabView* tab_view;
    GtkEntry* url_entry;
};

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdWindow, ladybird_window, ADW_TYPE_APPLICATION_WINDOW)

static void ladybird_window_dispose(GObject* object)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(object);

    gtk_widget_dispose_template(GTK_WIDGET(self), LADYBIRD_TYPE_WINDOW);

    G_OBJECT_CLASS(ladybird_window_parent_class)->dispose(object);
}

static AdwTabPage* open_new_tab(LadybirdWindow* self)
{
    LadybirdWebView* web_view = (LadybirdWebView*)g_object_new(LADYBIRD_TYPE_WEB_VIEW, nullptr);
    gtk_widget_add_css_class(GTK_WIDGET(web_view), "view");
    GtkWidget* scrolled_window = gtk_scrolled_window_new();
    gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled_window), GTK_WIDGET(web_view));
    AdwTabPage* tab_page = adw_tab_view_append(self->tab_view, scrolled_window);
    adw_tab_page_set_title(tab_page, "New tab");
    g_object_bind_property(web_view, "page-title", tab_page, "title", G_BINDING_DEFAULT);
    g_object_bind_property(web_view, "loading", tab_page, "loading", G_BINDING_DEFAULT);
    adw_tab_view_set_selected_page(self->tab_view, tab_page);
    // g_object_unref(web_view);
    // g_object_unref(scrolled_window);
    return tab_page;
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
                char* uri = g_file_get_uri(file);
                AdwTabPage* tab_page = open_new_tab(self);
                GtkScrolledWindow* scrolled_window = GTK_SCROLLED_WINDOW(adw_tab_page_get_child(tab_page));
                LadybirdWebView* web_view = LADYBIRD_WEB_VIEW(gtk_scrolled_window_get_child(scrolled_window));
                ladybird_web_view_load_url(web_view, uri);
                g_free(uri);
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

static void ladybird_window_init(LadybirdWindow* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
    g_type_ensure(LADYBIRD_TYPE_WEB_VIEW);
    gtk_widget_init_template(widget);

    // Let's try adding a tab -- what could possibly go wrong?
    gtk_widget_activate_action(widget, "win.new-tab", NULL);
}

static void ladybird_window_class_init(LadybirdWindowClass* klass)
{
    GtkWidgetClass* widget_class = GTK_WIDGET_CLASS(klass);
    GObjectClass* object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = ladybird_window_dispose;

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/ladybird-gtk4/window.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_overview);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_view);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, url_entry);
    gtk_widget_class_bind_template_callback(widget_class, open_new_tab);

    gtk_widget_class_install_action(widget_class, "win.new-tab", NULL, win_new_tab_action);
    gtk_widget_class_install_action(widget_class, "win.open-file", nullptr, win_open_file_action);
    gtk_widget_class_install_action(widget_class, "tab.close", NULL, tab_close_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_t, GDK_CONTROL_MASK, "win.new-tab", NULL);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_o, GDK_CONTROL_MASK, "win.open-file", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_w, GDK_CONTROL_MASK, "tab.close", NULL);

    gtk_widget_class_install_action(widget_class, "page.zoom-in", NULL, page_zoom_in_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-out", NULL, page_zoom_out_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-reset", NULL, page_zoom_reset_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_equal, GDK_CONTROL_MASK, "page.zoom-in", NULL);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_minus, GDK_CONTROL_MASK, "page.zoom-out", NULL);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_0, GDK_CONTROL_MASK, "page.zoom-reset", NULL);
}

LadybirdWindow* ladybird_window_new(GtkApplication* app)
{
    return LADYBIRD_WINDOW(g_object_new(LADYBIRD_TYPE_WINDOW, "application", app, nullptr));
}

G_END_DECLS
