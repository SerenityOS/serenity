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
    GtkWidget* scrolled_window = gtk_scrolled_window_new();
    gtk_scrolled_window_set_child(GTK_SCROLLED_WINDOW(scrolled_window), GTK_WIDGET(web_view));
    AdwTabPage* tab_page = adw_tab_view_append(self->tab_view, scrolled_window);
    adw_tab_view_set_selected_page(self->tab_view, tab_page);
    // g_object_unref(web_view);
    // g_object_unref(scrolled_window);
    adw_tab_page_set_title(tab_page, "Well hello friends! :^)");
    return tab_page;
}

static void win_new_tab_action(GtkWidget* widget, char const* action_name, GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    (void)action_name;
    (void)param;

    open_new_tab(self);
}

static void ladybird_window_init(LadybirdWindow* self)
{
    GtkWidget* widget = GTK_WIDGET(self);
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
}

LadybirdWindow* ladybird_window_new(GtkApplication* app)
{
    return LADYBIRD_WINDOW(g_object_new(LADYBIRD_TYPE_WINDOW, "application", app, nullptr));
}

G_END_DECLS
