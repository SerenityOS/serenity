#include "EventLoopImplementationGLib.h"
#include "Window.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>

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

static GActionEntry app_entries[] = {
    { "about", show_about, nullptr, nullptr, nullptr, 0 },
};

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

    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}
