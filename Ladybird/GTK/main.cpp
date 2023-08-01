#include "EventLoopImplementationGLib.h"
#include "Window.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>

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

    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}
