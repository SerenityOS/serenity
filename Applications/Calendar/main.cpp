#include "CalendarWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>

int main(int argc, char** argv)
{

    if (pledge("stdio shared_buffer rpath accept unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio shared_buffer rpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto window = GUI::Window::construct();
    window->set_title("Calendar");
    window->set_rect(20, 200, 596, 475);

    window->set_main_widget<CalendarWidget>();
    window->show();
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-calendar.png"));

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Calendar");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));
    app.set_menubar(move(menubar));

    app.exec();
}
