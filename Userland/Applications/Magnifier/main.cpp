/*
 * Copyright (c) 2021, Valtteri Koskivuori <vkoskiv@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MagnifierWidget.h"
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath rpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio cpath rpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    // Sneaky!
    // FIXME: Doesn't have a 32x32 icon yet, need to make one!
    auto app_icon = GUI::Icon::default_icon("find");

    // 4px on each side for padding
    constexpr int window_dimensions = 200 + 4 + 4;
    auto window = GUI::Window::construct();
    window->set_title("Magnifier");
    window->resize(window_dimensions, window_dimensions);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));
    auto& magnifier = window->set_main_widget<MagnifierWidget>();

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto size_action_group = make<GUI::ActionGroup>();

    auto two_x_action = GUI::Action::create_checkable(
        "&2x", [&](auto&) {
            magnifier.set_scale_factor(2);
        });

    auto four_x_action = GUI::Action::create_checkable(
        "&4x", [&](auto&) {
            magnifier.set_scale_factor(4);
        });

    size_action_group->add_action(two_x_action);
    size_action_group->add_action(four_x_action);
    size_action_group->set_exclusive(true);

    auto& view_menu = menubar->add_menu("&View");
    view_menu.add_action(two_x_action);
    view_menu.add_action(four_x_action);
    two_x_action->set_checked(true);

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Magnifier", app_icon, window));

    window->set_menubar(move(menubar));
    window->show();

    magnifier.track_cursor_globally();
    magnifier.start_timer(16);

    return app->exec();
}
