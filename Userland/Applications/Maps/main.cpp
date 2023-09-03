/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapWidget.h"
#include <LibConfig/Client.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Window.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-hello-world"sv)); // FIXME: Create Maps icon
    auto window = TRY(GUI::Window::try_create());
    window->set_title("Maps");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(640, 480);

    // Map widget
    MapWidget::Options options {};
    options.center.latitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, "30"sv).to_double().value_or(30.0);
    options.center.longitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, "0"sv).to_double().value_or(0.0);
    options.zoom = Config::read_i32("Maps"sv, "MapView"sv, "Zoom"sv, 3);
    auto maps = TRY(MapWidget::try_create(options));
    window->set_main_widget(maps);

    // Main menu
    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) { GUI::Application::the()->quit(); }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_zoom_in_action([maps](auto&) { maps->set_zoom(maps->zoom() + 1); }, window));
    view_menu->add_action(GUI::CommonActions::make_zoom_out_action([maps](auto&) { maps->set_zoom(maps->zoom() - 1); }, window));
    view_menu->add_action(GUI::CommonActions::make_reset_zoom_action([maps](auto&) { maps->set_zoom(3); }, window));
    view_menu->add_separator();
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([window](auto&) { window->set_fullscreen(!window->is_fullscreen()); }, window));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Maps", app_icon, window));

    window->show();

    // Remember last map position
    int exec = app->exec();
    Config::write_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, TRY(String::number(maps->center().latitude)));
    Config::write_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, TRY(String::number(maps->center().longitude)));
    Config::write_i32("Maps"sv, "MapView"sv, "Zoom"sv, maps->zoom());
    return exec;
}
