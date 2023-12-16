/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Window.h"
#include "Application.h"
#include "BitmapPaintable.h"
#include "LocationEntry.h"
#include "NavigationHistory.h"
#include "NavigationHistorySlice.h"
#include "Tab.h"
#include "WebView.h"
#include <glib/gi18n.h>

struct _LadybirdWindow {
    AdwApplicationWindow parent_instance;

    AdwTabOverview* tab_overview;
    AdwTabView* tab_view;
    AdwToastOverlay* toast_overlay;
    GtkEntry* location_entry;

    GtkPopover* navigation_history_popover;
    GtkListView* navigation_history_list_view;

    AdwTabPage* menu_page;
    LadybirdWebView* last_selected_web_view;

    gulong page_url_changed_id;
    gulong activate_url_id;
    gulong can_navigate_back_changed_id;
    gulong can_navigate_forward_changed_id;
    bool incognito : 1;
};

enum {
    PROP_0,
    PROP_INCOGNITO,
    NUM_PROPS,
};

static GParamSpec* props[NUM_PROPS];

G_BEGIN_DECLS

G_DEFINE_FINAL_TYPE(LadybirdWindow, ladybird_window, ADW_TYPE_APPLICATION_WINDOW)

static void disconnect_last_selected(LadybirdWindow* self)
{
    if (self->last_selected_web_view) {
        if (self->page_url_changed_id)
            g_signal_handler_disconnect(self->last_selected_web_view, self->page_url_changed_id);
        if (self->activate_url_id)
            g_signal_handler_disconnect(self->last_selected_web_view, self->activate_url_id);

        LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
        if (self->can_navigate_back_changed_id)
            g_signal_handler_disconnect(history, self->can_navigate_back_changed_id);
        if (self->can_navigate_forward_changed_id)
            g_signal_handler_disconnect(history, self->can_navigate_forward_changed_id);
    }
    self->page_url_changed_id = 0;
    self->activate_url_id = 0;
    self->last_selected_web_view = nullptr;
    self->can_navigate_back_changed_id = 0;
    self->can_navigate_forward_changed_id = 0;
}

static void ladybird_window_dispose(GObject* object)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(object);

    disconnect_last_selected(self);
    gtk_widget_unparent(GTK_WIDGET(self->navigation_history_popover));
    gtk_widget_dispose_template(GTK_WIDGET(self), LADYBIRD_TYPE_WINDOW);

    G_OBJECT_CLASS(ladybird_window_parent_class)->dispose(object);
}

static LadybirdWebView* get_web_view_from_tab_page(AdwTabPage* tab_page)
{
    LadybirdTab* tab = LADYBIRD_TAB(adw_tab_page_get_child(tab_page));
    return ladybird_tab_get_web_view(tab);
}

static void update_favicon(LadybirdBitmapPaintable* favicon_paintable, [[maybe_unused]] GParamSpec* pspec, void* data)
{
    AdwTabPage* tab_page = ADW_TAB_PAGE(data);
    GdkTexture* texture = ladybird_bitmap_paintable_get_texture(favicon_paintable);
    adw_tab_page_set_icon(tab_page, G_ICON(texture));
}

static AdwTabPage* open_new_tab(LadybirdWindow* self, AdwTabPage* parent)
{
    LadybirdApplication* app = LADYBIRD_APPLICATION(gtk_window_get_application(GTK_WINDOW(self)));
    WebView::CookieJar* cookie_jar = self->incognito
        ? ladybird_application_get_incognito_cookie_jar(app)
        : ladybird_application_get_cookie_jar(app);

    LadybirdTab* tab = ladybird_tab_new();
    LadybirdWebView* web_view = ladybird_tab_get_web_view(tab);
    ladybird_web_view_set_cookie_jar(web_view, cookie_jar);
    ladybird_web_view_set_webdriver_content_ipc_path(web_view, ladybird_application_get_webdriver_content_ipc_path(app));

    AdwTabPage* tab_page = adw_tab_view_add_page(self->tab_view, GTK_WIDGET(tab), parent);
    adw_tab_page_set_title(tab_page, _("New tab"));
    g_object_bind_property(web_view, "page-title", tab_page, "title", G_BINDING_DEFAULT);
    g_object_bind_property(web_view, "loading", tab_page, "loading", G_BINDING_DEFAULT);

    GdkPaintable* favicon_paintable = ladybird_web_view_get_favicon(web_view);
    g_signal_connect_object(favicon_paintable, "notify::texture", G_CALLBACK(update_favicon), tab_page, G_CONNECT_DEFAULT);

    return tab_page;
}

static AdwTabPage* on_create_tab(LadybirdWindow* self)
{
    AdwTabPage* tab_page = open_new_tab(self, nullptr);
    gtk_widget_grab_focus(GTK_WIDGET(self->location_entry));
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

static void on_can_navigate_back_changed(LadybirdWindow* self)
{
    if (!self->last_selected_web_view)
        return;
    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    bool enable = ladybird_navigation_history_can_navigate_back(history);
    gtk_widget_action_set_enabled(GTK_WIDGET(self), "page.navigate-back", enable);
}

static void on_can_navigate_forward_changed(LadybirdWindow* self)
{
    if (!self->last_selected_web_view)
        return;
    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    bool enable = ladybird_navigation_history_can_navigate_forward(history);
    gtk_widget_action_set_enabled(GTK_WIDGET(self), "page.navigate-forward", enable);
}

static void claim_event(GtkGesture* gesture)
{
    gtk_gesture_set_state(gesture, GTK_EVENT_SEQUENCE_CLAIMED);
}

static void on_navigate_back_right_clicked(LadybirdWindow* self, [[maybe_unused]] gint npress, [[maybe_unused]] double x, [[maybe_unused]] double y, GtkGestureClick* gesture)
{
    GtkWidget* button = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(gesture));
    gtk_widget_unparent(GTK_WIDGET(self->navigation_history_popover));
    gtk_widget_set_parent(GTK_WIDGET(self->navigation_history_popover), button);

    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    GtkNoSelection* selection = gtk_no_selection_new(ladybird_navigation_history_slice_new_back(history));
    gtk_list_view_set_model(self->navigation_history_list_view, GTK_SELECTION_MODEL(selection));
    g_object_unref(selection);

    gtk_popover_popup(self->navigation_history_popover);
}

static void on_navigate_forward_right_clicked(LadybirdWindow* self, [[maybe_unused]] gint npress, [[maybe_unused]] double x, [[maybe_unused]] double y, GtkGestureClick* gesture)
{
    GtkWidget* button = gtk_event_controller_get_widget(GTK_EVENT_CONTROLLER(gesture));
    gtk_widget_unparent(GTK_WIDGET(self->navigation_history_popover));
    gtk_widget_set_parent(GTK_WIDGET(self->navigation_history_popover), button);

    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    GtkNoSelection* selection = gtk_no_selection_new(ladybird_navigation_history_slice_new_forward(history));
    gtk_list_view_set_model(self->navigation_history_list_view, GTK_SELECTION_MODEL(selection));
    g_object_unref(selection);

    gtk_popover_popup(self->navigation_history_popover);
}

static void on_navigation_history_activate(LadybirdWindow* self, guint position)
{
    GtkSelectionModel* selection_model = gtk_list_view_get_model(self->navigation_history_list_view);
    LadybirdNavigationHistorySlice* slice = LADYBIRD_NAVIGATION_HISTORY_SLICE(gtk_no_selection_get_model(GTK_NO_SELECTION(selection_model)));
    position = ladybird_navigation_history_slice_map_position(slice, position);

    gtk_popover_popdown(self->navigation_history_popover);
    gtk_widget_unparent(GTK_WIDGET(self->navigation_history_popover));

    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    ladybird_navigation_history_set_current_position(history, position);
}

static void on_navigation_history_popover_closed(LadybirdWindow* self)
{
    // Unref the navigation history slice model.
    gtk_list_view_set_model(self->navigation_history_list_view, nullptr);
}

static void win_new_tab_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    AdwTabPage* tab_page = open_new_tab(self, nullptr);
    adw_tab_view_set_selected_page(self->tab_view, tab_page);
    gtk_widget_grab_focus(GTK_WIDGET(self->location_entry));
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
                GtkWidget* message_dialog = adw_message_dialog_new(GTK_WINDOW(self), _("Failed to pick file"), error->message);
                g_error_free(error);
                adw_message_dialog_add_response(ADW_MESSAGE_DIALOG(message_dialog), "ok", _("OK"));
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

static void win_focus_location_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    gtk_editable_select_region(GTK_EDITABLE(self->location_entry), 0, -1);
    gtk_widget_grab_focus(GTK_WIDGET(self->location_entry));
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

    AdwTabPage* tab_page;
    if (self->menu_page)
        tab_page = self->menu_page;
    else
        tab_page = adw_tab_view_get_selected_page(self->tab_view);

    if (tab_page)
        adw_tab_view_close_page(self->tab_view, tab_page);
}

static void tab_pin_action(GtkWidget* widget, char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    bool pin = !strcmp(action_name, "tab.pin");
    if (self->menu_page)
        adw_tab_view_set_page_pinned(self->tab_view, self->menu_page, pin);
}

static void tab_move_to_new_window_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    if (!self->menu_page)
        return;

    GtkApplication* app = gtk_window_get_application(GTK_WINDOW(self));
    LadybirdWindow* new_window = ladybird_window_new(LADYBIRD_APPLICATION(app), false, self->incognito);
    adw_tab_view_transfer_page(self->tab_view, self->menu_page, new_window->tab_view, 0);
    self->menu_page = nullptr;
    gtk_window_present(GTK_WINDOW(new_window));
}

static void tab_close_others_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    if (self->menu_page)
        adw_tab_view_close_other_pages(self->tab_view, self->menu_page);
}

static void tab_duplicate_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);

    if (!self->menu_page)
        return;

    LadybirdWebView* web_view = get_web_view_from_tab_page(self->menu_page);
    AdwTabPage* new_tab_page = open_new_tab(self, self->menu_page);
    LadybirdWebView* new_web_view = get_web_view_from_tab_page(new_tab_page);
    ladybird_web_view_load_url(new_web_view, ladybird_web_view_get_page_url(web_view));
}

static LadybirdWebView* ladybird_window_get_current_page(LadybirdWindow* self)
{
    AdwTabPage* tab_page = adw_tab_view_get_selected_page(self->tab_view);
    if (!tab_page)
        return nullptr;
    return get_web_view_from_tab_page(tab_page);
}

static void on_url_entered(LadybirdWindow* self, GtkEntry* location_entry)
{
    LadybirdWebView* web_view = ladybird_window_get_current_page(self);
    if (!web_view)
        return;

    char const* url = gtk_entry_buffer_get_text(gtk_entry_get_buffer(location_entry));
    ladybird_web_view_load_url(web_view, url);
    gtk_widget_grab_focus(GTK_WIDGET(web_view));
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
    GtkEntryBuffer* entry_buffer = gtk_entry_get_buffer(self->location_entry);
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

static void on_setup_tab_menu(LadybirdWindow* self, AdwTabPage* tab_page)
{
    self->menu_page = tab_page;
    if (!tab_page) {
        gtk_widget_action_set_enabled(GTK_WIDGET(self), "tab.pin", false);
        gtk_widget_action_set_enabled(GTK_WIDGET(self), "tab.unpin", false);
        return;
    }

    bool pinned = adw_tab_page_get_pinned(tab_page);
    gtk_widget_action_set_enabled(GTK_WIDGET(self), "tab.pin", !pinned);
    gtk_widget_action_set_enabled(GTK_WIDGET(self), "tab.unpin", pinned);
}

static void on_activate_url(LadybirdWindow* self, char const* url, gboolean switch_to_new_tab)
{
    g_return_if_fail(LADYBIRD_IS_WINDOW(self));

    // Does this look like a URL that we can handle?
    char* scheme = g_uri_parse_scheme(url);
    char const* supported_schemes[] {
        "https", "http", "gemini", "file", "data",
        nullptr
    };
    bool open_internally = scheme && g_strv_contains(supported_schemes, scheme);
    g_free(scheme);
    if (open_internally) {
        AdwTabPage* tab_page = open_new_tab(self, adw_tab_view_get_selected_page(self->tab_view));
        LadybirdWebView* web_view = get_web_view_from_tab_page(tab_page);
        ladybird_web_view_load_url(web_view, url);
        if (switch_to_new_tab)
            adw_tab_view_set_selected_page(self->tab_view, tab_page);
    } else {
        // Invoke an external program to open it.
        // TODO: It would be cool to respect switch_to_new_tab for this.
        g_app_info_launch_default_for_uri(url, nullptr, nullptr);
    }
}

static void on_selected_page_changed(LadybirdWindow* self)
{
    disconnect_last_selected(self);

    AdwTabPage* tab_page = adw_tab_view_get_selected_page(self->tab_view);
    if (!tab_page)
        return;
    LadybirdTab* tab = LADYBIRD_TAB(adw_tab_page_get_child(tab_page));

    LadybirdWebView* web_view = self->last_selected_web_view = ladybird_tab_get_web_view(tab);
    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(web_view);

    self->page_url_changed_id = g_signal_connect_object(web_view, "notify::page-url", G_CALLBACK(on_page_url_changed), self, G_CONNECT_SWAPPED);
    on_page_url_changed(self);
    self->activate_url_id = g_signal_connect_object(web_view, "activate-url", G_CALLBACK(on_activate_url), self, G_CONNECT_SWAPPED);
    self->can_navigate_back_changed_id = g_signal_connect_object(history, "notify::can-navigate-back", G_CALLBACK(on_can_navigate_back_changed), self, G_CONNECT_SWAPPED);
    self->can_navigate_forward_changed_id = g_signal_connect_object(history, "notify::can-navigate-forward", G_CALLBACK(on_can_navigate_forward_changed), self, G_CONNECT_SWAPPED);
    on_can_navigate_back_changed(self);
    on_can_navigate_forward_changed(self);
}

static void on_webview_close(LadybirdWindow* self, LadybirdWebView* web_view)
{
    g_assert(LADYBIRD_IS_WINDOW(self));
    g_assert(LADYBIRD_IS_WEB_VIEW(web_view));

    // No point in calling this twice.
    g_signal_handlers_disconnect_by_func(web_view, reinterpret_cast<void*>(on_webview_close), self);

    AdwTabPage* tab_page = adw_tab_view_get_page(self->tab_view, GTK_WIDGET(web_view));
    // TODO: This will not close pinned pages. We probably should ask the user before closing anyway,
    // but this needs support from LibJS side.
    adw_tab_view_close_page(self->tab_view, tab_page);

    // Let the user know what happened.
    // TODO: Format the page host in here.
    AdwToast* toast = adw_toast_new(_("A script closed the web page"));
    adw_toast_overlay_add_toast(self->toast_overlay, toast);
}

static void on_tab_page_attached(LadybirdWindow* self, AdwTabPage* tab_page, [[maybe_unused]] int position)
{
    g_assert(LADYBIRD_IS_WINDOW(self));
    g_assert(ADW_IS_TAB_PAGE(tab_page));

    LadybirdWebView* web_view = get_web_view_from_tab_page(tab_page);
    g_signal_connect_object(web_view, "close", G_CALLBACK(on_webview_close), self, G_CONNECT_SWAPPED);
}

static void on_tab_page_detached(LadybirdWindow* self, AdwTabPage* tab_page, [[maybe_unused]] int position)
{
    g_assert(LADYBIRD_IS_WINDOW(self));
    g_assert(ADW_IS_TAB_PAGE(tab_page));

    LadybirdWebView* web_view = get_web_view_from_tab_page(tab_page);
    g_signal_handlers_disconnect_by_func(web_view, reinterpret_cast<void*>(on_webview_close), self);
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

static void page_navigate_action(GtkWidget* widget, [[maybe_unused]] char const* action_name, [[maybe_unused]] GVariant* param)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(widget);
    if (!self->last_selected_web_view)
        return;

    int delta = strcmp(action_name, "page.navigate-back") ? 1 : -1;
    LadybirdNavigationHistory* history = ladybird_web_view_get_navigation_history(self->last_selected_web_view);
    ladybird_navigation_history_navigate(history, delta);
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
    AdwTabPage* tab_page = open_new_tab(self, nullptr);
    LadybirdWebView* web_view = get_web_view_from_tab_page(tab_page);
    ladybird_web_view_load_url(web_view, uri);
    g_free(uri);
    adw_tab_view_set_selected_page(self->tab_view, tab_page);
}

static char* format_zoom_percent_label([[maybe_unused]] void* instance, int zoom_percent)
{
    // Translators: this is a format string for the zoom-percent label in the main menu.
    // For most languages, it doesn't need translating.
    return g_strdup_printf(_("%d%%"), zoom_percent);
}

static void ladybird_window_init(LadybirdWindow* self)
{
    GtkWidget* widget = GTK_WIDGET(self);

    g_type_ensure(LADYBIRD_TYPE_TAB);
    g_type_ensure(LADYBIRD_TYPE_WEB_VIEW);
    g_type_ensure(LADYBIRD_TYPE_LOCATION_ENTRY);

    gtk_widget_init_template(widget);

    g_signal_connect_object(self->tab_view, "page-attached", G_CALLBACK(on_tab_page_attached), self, G_CONNECT_SWAPPED);
    g_signal_connect_object(self->tab_view, "page-detached", G_CALLBACK(on_tab_page_detached), self, G_CONNECT_SWAPPED);
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

    gtk_widget_class_set_template_from_resource(widget_class, "/org/serenityos/Ladybird-gtk4/window.ui");
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_overview);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, tab_view);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, location_entry);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, toast_overlay);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, navigation_history_popover);
    gtk_widget_class_bind_template_child(widget_class, LadybirdWindow, navigation_history_list_view);
    gtk_widget_class_bind_template_callback(widget_class, on_create_tab);
    gtk_widget_class_bind_template_callback(widget_class, on_url_entered);
    gtk_widget_class_bind_template_callback(widget_class, on_create_window);
    gtk_widget_class_bind_template_callback(widget_class, on_setup_tab_menu);
    gtk_widget_class_bind_template_callback(widget_class, on_selected_page_changed);
    gtk_widget_class_bind_template_callback(widget_class, claim_event);
    gtk_widget_class_bind_template_callback(widget_class, on_navigate_back_right_clicked);
    gtk_widget_class_bind_template_callback(widget_class, on_navigate_forward_right_clicked);
    gtk_widget_class_bind_template_callback(widget_class, on_navigation_history_activate);
    gtk_widget_class_bind_template_callback(widget_class, on_navigation_history_popover_closed);
    gtk_widget_class_bind_template_callback(widget_class, format_zoom_percent_label);

    gtk_widget_class_install_action(widget_class, "win.new-tab", nullptr, win_new_tab_action);
    gtk_widget_class_install_action(widget_class, "win.open-file", nullptr, win_open_file_action);
    gtk_widget_class_install_action(widget_class, "win.focus-location", nullptr, win_focus_location_action);
    gtk_widget_class_install_action(widget_class, "tab.close", nullptr, tab_close_action);
    gtk_widget_class_install_action(widget_class, "tab.duplicate", nullptr, tab_duplicate_action);
    gtk_widget_class_install_action(widget_class, "tab.pin", nullptr, tab_pin_action);
    gtk_widget_class_install_action(widget_class, "tab.unpin", nullptr, tab_pin_action);
    gtk_widget_class_install_action(widget_class, "tab.move-to-new-window", nullptr, tab_move_to_new_window_action);
    gtk_widget_class_install_action(widget_class, "tab.close-others", nullptr, tab_close_others_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_t, GDK_CONTROL_MASK, "win.new-tab", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_o, GDK_CONTROL_MASK, "win.open-file", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_l, GDK_CONTROL_MASK, "win.focus-location", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_d, GDK_ALT_MASK, "win.focus-location", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_F6, GdkModifierType(0), "win.focus-location", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_w, GDK_CONTROL_MASK, "tab.close", nullptr);

    gtk_widget_class_install_action(widget_class, "page.zoom-in", nullptr, page_zoom_in_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-out", nullptr, page_zoom_out_action);
    gtk_widget_class_install_action(widget_class, "page.zoom-reset", nullptr, page_zoom_reset_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_equal, GDK_CONTROL_MASK, "page.zoom-in", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_minus, GDK_CONTROL_MASK, "page.zoom-out", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_0, GDK_CONTROL_MASK, "page.zoom-reset", nullptr);

    gtk_widget_class_install_action(widget_class, "page.navigate-back", nullptr, page_navigate_action);
    gtk_widget_class_install_action(widget_class, "page.navigate-forward", nullptr, page_navigate_action);
    gtk_widget_class_install_action(widget_class, "page.reload-page", nullptr, page_reload_action);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_Left, GDK_ALT_MASK, "page.navigate-back", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_Right, GDK_ALT_MASK, "page.navigate-forward", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_F5, GdkModifierType(0), "page.reload-page", nullptr);
    gtk_widget_class_add_binding_action(widget_class, GDK_KEY_r, GDK_CONTROL_MASK, "page.reload-page", nullptr);
}

LadybirdWindow* ladybird_window_new(LadybirdApplication* app, bool add_initial_tab, bool incognito)
{
    LadybirdWindow* self = LADYBIRD_WINDOW(g_object_new(LADYBIRD_TYPE_WINDOW,
        "application", app,
        "incognito", (gboolean)incognito,
        nullptr));

    if (add_initial_tab) {
        open_new_tab(self, nullptr);
        gtk_widget_grab_focus(GTK_WIDGET(self->location_entry));
    }

    return self;
}

G_END_DECLS
