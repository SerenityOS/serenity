/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SearchPanel.h"
#include "UsersMapWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Process.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>

static int constexpr MAP_ZOOM_DEFAULT = 3;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/bin/MapsSettings", "x"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/config", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/request", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Config::monitor_domain("Maps");

    // Window
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-maps"sv));
    auto window = GUI::Window::construct();
    window->set_title("Maps");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->restore_size_and_position("Maps"sv, "Window"sv, { { 640, 480 } });
    window->save_size_and_position_on_close("Maps"sv, "Window"sv);

    // Root widget
    auto root_widget = window->set_main_widget<GUI::Widget>();
    root_widget->set_fill_with_background_color(true);
    root_widget->set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 2);

    // Toolbar
    auto toolbar_container = TRY(root_widget->try_add<GUI::ToolbarContainer>());
    auto toolbar = TRY(toolbar_container->try_add<GUI::Toolbar>());

    // Main Widget
    auto main_widget = TRY(root_widget->try_add<GUI::HorizontalSplitter>());

    // Map widget
    Maps::UsersMapWidget::Options options {};
    options.center.latitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, "30"sv).to_double().value_or(30.0);
    options.center.longitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, "0"sv).to_double().value_or(0.0);
    options.zoom = Config::read_i32("Maps"sv, "MapView"sv, "Zoom"sv, MAP_ZOOM_DEFAULT);
    auto map_widget = TRY(main_widget->try_add<Maps::UsersMapWidget>(options));
    map_widget->set_frame_style(Gfx::FrameStyle::SunkenContainer);
    map_widget->set_show_users(Config::read_bool("Maps"sv, "MapView"sv, "ShowUsers"sv, false));

    // Search panel
    auto search_panel = TRY(Maps::SearchPanel::create());
    search_panel->on_places_change = [map_widget](auto) { map_widget->remove_markers_with_name("search"sv); };
    search_panel->on_selected_place_change = [map_widget](auto const& place) {
        // Remove old search markers
        map_widget->remove_markers_with_name("search"sv);

        // Add new marker and zoom into it
        map_widget->add_marker({ place.latlng, place.name, {}, "search"_string });
        map_widget->set_center(place.latlng);
        map_widget->set_zoom(place.zoom);
    };
    if (Config::read_bool("Maps"sv, "SearchPanel"sv, "Show"sv, false))
        main_widget->insert_child_before(search_panel, map_widget);

    // Main menu actions
    auto file_menu = window->add_menu("&File"_string);
    auto open_settings_action = GUI::Action::create("Maps &Settings", { Mod_Ctrl, Key_Comma }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-settings.png"sv)), [window](GUI::Action const&) {
        GUI::Process::spawn_or_show_error(window, "/bin/MapsSettings"sv);
    });
    file_menu->add_action(open_settings_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) { GUI::Application::the()->quit(); }));

    auto view_menu = window->add_menu("&View"_string);
    auto show_search_panel_action = GUI::Action::create_checkable(
        "Show search panel", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)), [main_widget, search_panel, map_widget](auto& action) {
            if (action.is_checked()) {
                main_widget->insert_child_before(search_panel, map_widget);
            } else {
                map_widget->remove_markers_with_name("search"sv);
                search_panel->reset();
                main_widget->remove_child(search_panel);
            }
        },
        window);
    show_search_panel_action->set_checked(Config::read_bool("Maps"sv, "SearchPanel"sv, "Show"sv, false));
    auto show_users_action = GUI::Action::create_checkable(
        "Show SerenityOS users", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/ladyball.png"sv)), [map_widget](auto& action) { map_widget->set_show_users(action.is_checked()); }, window);
    show_users_action->set_checked(map_widget->show_users());
    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action([map_widget](auto&) { map_widget->set_zoom(map_widget->zoom() + 1); }, window);
    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action([map_widget](auto&) { map_widget->set_zoom(map_widget->zoom() - 1); }, window);
    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action([map_widget](auto&) { map_widget->set_zoom(MAP_ZOOM_DEFAULT); }, window);
    auto fullscreen_action = GUI::CommonActions::make_fullscreen_action([window, toolbar_container, map_widget](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container->set_visible(!window->is_fullscreen());
        map_widget->set_frame_style(window->is_fullscreen() ? Gfx::FrameStyle::NoFrame : Gfx::FrameStyle::SunkenContainer);
    },
        window);
    view_menu->add_action(show_search_panel_action);
    view_menu->add_separator();
    view_menu->add_action(show_users_action);
    view_menu->add_separator();
    view_menu->add_action(zoom_in_action);
    view_menu->add_action(zoom_out_action);
    view_menu->add_action(reset_zoom_action);
    view_menu->add_separator();
    view_menu->add_action(fullscreen_action);

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_about_action("Maps"_string, app_icon, window));

    // Main toolbar actions
    toolbar->add_action(show_search_panel_action);
    toolbar->add_separator();
    toolbar->add_action(show_users_action);
    toolbar->add_separator();
    toolbar->add_action(zoom_in_action);
    toolbar->add_action(zoom_out_action);
    toolbar->add_action(reset_zoom_action);
    toolbar->add_separator();
    toolbar->add_action(open_settings_action);

    window->show();

    // Remember last window state
    int exec = app->exec();
    Config::write_bool("Maps"sv, "SearchPanel"sv, "Show"sv, show_search_panel_action->is_checked());
    Config::write_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, TRY(String::number(map_widget->center().latitude)));
    Config::write_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, TRY(String::number(map_widget->center().longitude)));
    Config::write_i32("Maps"sv, "MapView"sv, "Zoom"sv, map_widget->zoom());
    Config::write_bool("Maps"sv, "MapView"sv, "ShowUsers"sv, map_widget->show_users());
    return exec;
}
