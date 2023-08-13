#include "EventLoopImplementationGLib.h"
#include "Window.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>

static void open_new_window([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);
    LadybirdWindow* window = ladybird_window_new(app, true);
    gtk_window_present(GTK_WINDOW(window));
}

static void show_shortcuts([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);
    GtkBuilder* builder = gtk_builder_new_from_resource("/org/serenityos/ladybird-gtk4/shortcuts-dialog.ui");
    GtkWindow* dialog = GTK_WINDOW(gtk_builder_get_object(builder, "shortcuts_dialog"));
    gtk_window_set_transient_for(dialog, gtk_application_get_active_window(app));
    gtk_window_present(GTK_WINDOW(dialog));
    g_object_unref(builder);
}

static void show_about([[maybe_unused]] GSimpleAction* action, [[maybe_unused]] GVariant* state, void* user_data)
{
    GtkApplication* app = GTK_APPLICATION(user_data);

    char const* developers[] = {
        "Sergey Bugaev",
        nullptr
    };

    AdwAboutWindow* about_window = ADW_ABOUT_WINDOW(g_object_new(ADW_TYPE_ABOUT_WINDOW,
        "application-name", "Ladybird",
        "version", "WIP",
        "application-icon", "application-x-executable", // TODO: we need an icon!
        "developer-name", "SerenityOS developers",
        "website", "https://ladybird.dev",
        "issue-url", "https://github.com/SerenityOS/serenity/issues",
        "copyright", "© 2023 SerenityOS developers",
        "license-type", GTK_LICENSE_BSD,
        "developers", developers,
        "comments", "Ladybird is a browser based on LibWeb web engine and LibJS JavaScript engine,"
                    " developed by a large team of contributors as a part of the SerenityOS project.",
        nullptr));

    adw_about_window_add_link(about_window, "SerenityOS website", "https://serenityos.org");

    GtkWindow* active_window = gtk_application_get_active_window(app);
    if (active_window)
        gtk_window_set_transient_for(GTK_WINDOW(about_window), active_window);
    gtk_window_present(GTK_WINDOW(about_window));
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
        LadybirdWindow* window = ladybird_window_new(app, true);
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
