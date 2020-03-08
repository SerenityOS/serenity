#include "CalendarWidget.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_title("Calendar");
    window->set_rect(20, 200, 596, 476);
    //TODO: Allow proper resize
    window->set_resizable(false);

    window->set_main_widget<CalendarWidget>();

    window->show();

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Calendar");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto add_event_menu = GUI::Menu::construct("Add Event");
    add_event_menu->add_action(GUI::Action::create("Add Event", [&](const GUI::Action&) {

    }));

    app.set_menubar(move(menubar));

    app.exec();
}
