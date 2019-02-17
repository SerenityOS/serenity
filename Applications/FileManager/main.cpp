#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <unistd.h>
#include <stdio.h>
#include "DirectoryView.h"

static GWindow* make_window();

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("FileManager");
    app_menu->add_action(make<GAction>("Quit", String(), [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    menubar->add_menu(move(file_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(make<GAction>("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = make_window();
    window->set_should_exit_app_on_close(true);
    window->show();

    return app.exec();
}

GWindow* make_window()
{
    auto* window = new GWindow;
    window->set_title("FileManager");
    window->set_rect(20, 200, 240, 300);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* directory_view = new DirectoryView(widget);

    auto* statusbar = new GStatusBar(widget);
    statusbar->set_text("Welcome!");

    directory_view->on_path_change = [window] (const String& new_path) {
        window->set_title(String::format("FileManager: %s", new_path.characters()));
    };

    directory_view->on_status_message = [statusbar] (String message) {
        statusbar->set_text(move(message));
    };

    directory_view->open("/");

    return window;
}

