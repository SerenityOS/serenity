#include "Application.h"
#include "EventLoopImplementationGLib.h"
#include <LibCore/EventLoop.h>
#include <adwaita.h>

int main(int argc, char* argv[])
{
    Core::EventLoopManager::install(*new EventLoopManagerGLib);
    [[maybe_unused]] Core::EventLoop serenity_event_loop;

    LadybirdApplication* app = ladybird_application_new();
    int status = g_application_run(G_APPLICATION(app), argc, argv);
    g_object_unref(app);
    return status;
}
