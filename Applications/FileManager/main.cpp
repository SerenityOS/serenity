#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include "DirectoryTableView.h"

int main(int argc, char** argv)
{
    struct sigaction act;
    memset(&act, 0, sizeof(act));
    act.sa_flags = SA_NOCLDWAIT;
    act.sa_handler = SIG_IGN;
    int rc = sigaction(SIGCHLD, &act, nullptr);
    if (rc < 0) {
        perror("sigaction");
        return 1;
    }

    GApplication app(argc, argv);

    auto mkdir_action = GAction::create("New directory...", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/mkdir16.rgb", { 16, 16 }), [] (const GAction&) {
        dbgprintf("'New directory' action activated!\n");
    });

    auto copy_action = GAction::create("Copy", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/copyfile16.rgb", { 16, 16 }), [] (const GAction&) {
        dbgprintf("'Copy' action activated!\n");
    });

    auto delete_action = GAction::create("Delete", GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/trash16.rgb", { 16, 16 }), [] (const GAction&) {
        dbgprintf("'Delete' action activated!\n");
    });

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("FileManager");
    app_menu->add_action(GAction::create("Quit", String(), [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(mkdir_action.copy_ref());
    file_menu->add_action(copy_action.copy_ref());
    file_menu->add_action(delete_action.copy_ref());
    menubar->add_menu(move(file_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = new GWindow;
    window->set_title("FileManager");
    window->set_rect(20, 200, 240, 300);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    toolbar->add_action(mkdir_action.copy_ref());
    toolbar->add_action(copy_action.copy_ref());
    toolbar->add_action(delete_action.copy_ref());

    auto* directory_table_view = new DirectoryTableView(widget);

    auto* statusbar = new GStatusBar(widget);
    statusbar->set_text("Welcome!");

    directory_table_view->on_path_change = [window] (const String& new_path) {
        window->set_title(String::format("FileManager: %s", new_path.characters()));
    };

    directory_table_view->on_status_message = [statusbar] (String message) {
        statusbar->set_text(move(message));
    };

    directory_table_view->open("/");
    directory_table_view->set_focus(true);

    window->set_should_exit_app_on_close(true);
    window->show();

    return app.exec();
}
