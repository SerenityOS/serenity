/*
 * Copyright (c) 2021, Robin Allen <r@foon.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <AK/URL.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>

using namespace Dictionary;

int main(int argc, char** argv)
{
    char const* initial_query = (argc > 1) ? argv[1] : "";

    auto app = GUI::Application::construct(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-dictionary");

    auto window = GUI::Window::construct();
    window->resize(640, 400);

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = window->add_menu("&Help");

    help_menu.add_action(GUI::Action::create("WordNet License", [](GUI::Action&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/res/dictionaries/wordnet.license"));
    }));

    help_menu.add_action(GUI::CommonActions::make_about_action("Dictionary", GUI::Icon::default_icon("app-dictionary"), window));

    auto& main_widget = window->set_main_widget<MainWidget>(initial_query);

    window->set_title("Dictionary");

    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    main_widget.focus_search_box();

    return app->exec();
}
