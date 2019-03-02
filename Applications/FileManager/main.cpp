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

    auto* window = new GWindow;
    window->set_title("FileManager");
    window->set_rect(20, 200, 640, 480);
    window->set_should_exit_app_on_close(true);

    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    auto* directory_table_view = new DirectoryTableView(widget);
    auto* statusbar = new GStatusBar(widget);

    auto open_parent_directory_action = GAction::create("Open parent directory", { Mod_Alt, Key_Up }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/parentdirectory16.rgb", { 16, 16 }), [directory_table_view] (const GAction&) {
        directory_table_view->open_parent_directory();
    });

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
    file_menu->add_action(open_parent_directory_action.copy_ref());
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

    toolbar->add_action(open_parent_directory_action.copy_ref());
    toolbar->add_action(mkdir_action.copy_ref());
    toolbar->add_action(copy_action.copy_ref());
    toolbar->add_action(delete_action.copy_ref());

    directory_table_view->on_path_change = [window] (const String& new_path) {
        window->set_title(String::format("FileManager: %s", new_path.characters()));
    };

    directory_table_view->on_status_message = [statusbar] (String message) {
        statusbar->set_text(move(message));
    };

    directory_table_view->open("/");
    directory_table_view->set_focus(true);

    window->set_main_widget(widget);
    window->show();

    return app.exec();
}
