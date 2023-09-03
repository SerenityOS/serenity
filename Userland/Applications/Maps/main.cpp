/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MapWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>

static int constexpr MAP_ZOOM_DEFAULT = 3;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/config", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/request", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-maps"sv));
    auto window = TRY(GUI::Window::try_create());
    window->set_title("Maps");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->restore_size_and_position("Maps"sv, "Window"sv, { { 640, 480 } });
    window->save_size_and_position_on_close("Maps"sv, "Window"sv);

    // Root widget
    auto root_widget = TRY(window->set_main_widget<GUI::Widget>());
    root_widget->set_fill_with_background_color(true);
    root_widget->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 2);

    // Toolbar
    auto toolbar_container = TRY(root_widget->try_add<GUI::ToolbarContainer>());
    auto toolbar = TRY(toolbar_container->try_add<GUI::Toolbar>());

    // Map widget
    MapWidget::Options options {};
    options.center.latitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, "30"sv).to_double().value_or(30.0);
    options.center.longitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, "0"sv).to_double().value_or(0.0);
    options.zoom = Config::read_i32("Maps"sv, "MapView"sv, "Zoom"sv, MAP_ZOOM_DEFAULT);
    auto maps = TRY(root_widget->try_add<MapWidget>(options));
    maps->set_frame_style(Gfx::FrameStyle::SunkenContainer);

    // Main menu actions
    auto file_menu = window->add_menu("&File"_string);
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) { GUI::Application::the()->quit(); }));

    auto view_menu = window->add_menu("&View"_string);
    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action([maps](auto&) { maps->set_zoom(maps->zoom() + 1); }, window);
    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action([maps](auto&) { maps->set_zoom(maps->zoom() - 1); }, window);
    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action([maps](auto&) { maps->set_zoom(MAP_ZOOM_DEFAULT); }, window);
    auto fullscreen_action = GUI::CommonActions::make_fullscreen_action([window, toolbar_container, maps](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container->set_visible(!window->is_fullscreen());
        maps->set_frame_style(window->is_fullscreen() ? Gfx::FrameStyle::NoFrame : Gfx::FrameStyle::SunkenContainer);
    },
        window);
    view_menu->add_action(zoom_in_action);
    view_menu->add_action(zoom_out_action);
    view_menu->add_action(reset_zoom_action);
    view_menu->add_separator();
    view_menu->add_action(fullscreen_action);

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Maps", app_icon, window));

    // Main toolbar actions
    toolbar->add_action(zoom_in_action);
    toolbar->add_action(zoom_out_action);
    toolbar->add_action(reset_zoom_action);

    window->show();

    // Remember last map position
    int exec = app->exec();
    Config::write_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, TRY(String::number(maps->center().latitude)));
    Config::write_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, TRY(String::number(maps->center().longitude)));
    Config::write_i32("Maps"sv, "MapView"sv, "Zoom"sv, maps->zoom());
    return exec;
}
