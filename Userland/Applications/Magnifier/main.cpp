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

    auto app_icon = GUI::Icon::default_icon("app-magnifier");

    // 4px on each side for padding
    constexpr int window_dimensions = 200 + 4 + 4;
    auto window = GUI::Window::construct();
    window->set_title("Magnifier");
    window->resize(window_dimensions, window_dimensions);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));
    auto& magnifier = window->set_main_widget<MagnifierWidget>();

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto size_action_group = make<GUI::ActionGroup>();

    auto two_x_action = GUI::Action::create_checkable(
        "&2x", { Key_2 }, [&](auto&) {
            magnifier.set_scale_factor(2);
        });

    auto four_x_action = GUI::Action::create_checkable(
        "&4x", { Key_4 }, [&](auto&) {
            magnifier.set_scale_factor(4);
        });

    auto eight_x_action = GUI::Action::create_checkable(
        "&8x", { Key_8 }, [&](auto&) {
            magnifier.set_scale_factor(8);
        });

    auto pause_action = GUI::Action::create_checkable(
        "&Pause Capture", { Key_Space }, [&](auto& action) {
            magnifier.pause_capture(action.is_checked());
        });

    size_action_group->add_action(two_x_action);
    size_action_group->add_action(four_x_action);
    size_action_group->add_action(eight_x_action);
    size_action_group->set_exclusive(true);

    auto& view_menu = window->add_menu("&View");
    view_menu.add_action(two_x_action);
    view_menu.add_action(four_x_action);
    view_menu.add_action(eight_x_action);
    two_x_action->set_checked(true);

    view_menu.add_separator();
    view_menu.add_action(pause_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Magnifier", app_icon, window));

    window->show();

    return app->exec();
}
