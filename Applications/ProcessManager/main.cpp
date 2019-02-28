#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include "ProcessTableView.h"

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    auto* process_table_view = new ProcessTableView(widget);
    auto* statusbar = new GStatusBar(widget);
    process_table_view->on_status_message = [statusbar] (String message) {
        statusbar->set_text(move(message));
    };

    auto kill_action = GAction::create("Kill process", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/kill16.rgb", { 16, 16 }), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGKILL);
    });

    auto stop_action = GAction::create("Stop process", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/stop16.rgb", { 16, 16 }), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGSTOP);
    });

    auto continue_action = GAction::create("Continue process", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/continue16.rgb", { 16, 16 }), [process_table_view] (const GAction&) {
        pid_t pid = process_table_view->selected_pid();
        if (pid != -1)
            kill(pid, SIGCONT);
    });

    toolbar->add_action(kill_action.copy_ref());
    toolbar->add_action(stop_action.copy_ref());
    toolbar->add_action(continue_action.copy_ref());

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("ProcessManager");
    app_menu->add_action(GAction::create("Quit", String(), [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto process_menu = make<GMenu>("Process");
    process_menu->add_action(kill_action.copy_ref());
    process_menu->add_action(stop_action.copy_ref());
    process_menu->add_action(continue_action.copy_ref());
    menubar->add_menu(move(process_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = new GWindow;
    window->set_title("ProcessManager");
    window->set_rect(20, 200, 640, 400);
    window->set_main_widget(widget);
    window->set_should_exit_app_on_close(true);
    window->show();

    return app.exec();
}
