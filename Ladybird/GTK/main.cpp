#include "EventLoopImplementationGLib.h"
#include "Window.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>

static void open_new_window([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);
    LadybirdWindow* window = ladybird_window_new(app);
    gtk_window_present(GTK_WINDOW(window));
}

static void show_shortcuts([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);
    GtkWidget* dialog = adw_message_dialog_new(gtk_application_get_active_window(app), "TODO", "Somebody should implement the Keyboard Shortcuts window :^)");
    adw_message_dialog_add_response(ADW_MESSAGE_DIALOG(dialog), "i-see", "I see");
    gtk_window_present(GTK_WINDOW(dialog));
}

static void show_about([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);

    char const* developers[] = {
        "Sergey Bugaev",
        nullptr
    };

    adw_show_about_window(gtk_application_get_active_window(app),
        "application-name", "Ladybird",
        "version", "WIP",
        "website", "https://ladybird.dev",
        "copyright", "© 2023 SerenityOS developers",
        "license-type", GTK_LICENSE_BSD,
        "developers", developers,
        nullptr);
}

static void do_quit([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GApplication* app = G_APPLICATION(user_data);
    g_application_quit(app);
}

static GActionEntry const app_entries[] = {
    { "new-window", open_new_window, nullptr, nullptr, nullptr, { 0 } },
    { "shortcuts", show_shortcuts, nullptr, nullptr, nullptr, { 0 } },
    { "about", show_about, nullptr, nullptr, nullptr, { 0 } },
    { "quit", do_quit, nullptr, nullptr, nullptr, { 0 } }
};

static char const* const new_window_accels[] = { "<Primary>n", nullptr };
static char const* const shortcuts_accels[] = { "<Primary>question", nullptr };
static char const* const quit_accels[] = { "<Primary>q", nullptr };

int main(int argc, char* argv[])
{
    Core::EventLoopManager::install(*new EventLoopManagerGLib);
    [[maybe_unused]] Core::EventLoop serenity_event_loop;

    AdwApplication* app = adw_application_new("org.serenityos.ladybird-gtk4", G_APPLICATION_HANDLES_OPEN);

    g_signal_connect(app, "activate", G_CALLBACK(+[](GtkApplication* app) {
        LadybirdWindow* window = ladybird_window_new(app);
        gtk_window_present(GTK_WINDOW(window));
    }),
        nullptr);

    g_action_map_add_action_entries(G_ACTION_MAP(app), app_entries, G_N_ELEMENTS(app_entries), app);
    gtk_application_set_accels_for_action(GTK_APPLICATION(app), "app.new-window", new_window_accels);
    gtk_application_set_accels_for_action(GTK_APPLICATION(app), "app.shortcuts", shortcuts_accels);
    gtk_application_set_accels_for_action(GTK_APPLICATION(app), "app.quit", quit_accels);

    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}
