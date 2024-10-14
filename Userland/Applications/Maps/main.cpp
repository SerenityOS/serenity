/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FavoritesPanel.h"
#include "SearchPanel.h"
#include "UsersMapWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
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
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Maps");
    TRY(Core::System::unveil("/bin/MapsSettings", "x"));
    TRY(Core::System::unveil("/home", "rwc"));
    TRY(Core::System::unveil("/res", "r"));
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
    auto& toolbar_container = root_widget->add<GUI::ToolbarContainer>();
    auto& toolbar = toolbar_container.add<GUI::Toolbar>();

    // Main Widget
    auto& main_widget = root_widget->add<GUI::HorizontalSplitter>();

    // Map widget
    Maps::UsersMapWidget::Options options {};
    options.center.latitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, "30"sv).to_number<double>().value_or(30.0);
    options.center.longitude = Config::read_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, "0"sv).to_number<double>().value_or(0.0);
    options.zoom = Config::read_i32("Maps"sv, "MapView"sv, "Zoom"sv, MAP_ZOOM_DEFAULT);
    auto& map_widget = main_widget.add<Maps::UsersMapWidget>(options);
    map_widget.set_frame_style(Gfx::FrameStyle::SunkenContainer);
    map_widget.set_show_users(Config::read_bool("Maps"sv, "MapView"sv, "ShowUsers"sv, false));

    // Panels
    String init_panel_open_name = TRY(String::from_byte_string(Config::read_string("Maps"sv, "Panel"sv, "OpenName"sv, ""sv)));
    int panel_width = Config::read_i32("Maps"sv, "Panel"sv, "Width"sv, INT_MIN);

    // Search panel
    auto search_panel = TRY(Maps::SearchPanel::try_create());
    search_panel->on_places_change = [&map_widget](auto) { map_widget.remove_markers_with_name("search"sv); };
    search_panel->on_selected_place_change = [&map_widget](auto const& place) {
        // Remove old search marker
        map_widget.remove_markers_with_name("search"sv);

        // Add new marker and zoom into it
        map_widget.add_marker({ place.latlng, place.name, {}, "search"_string });
        map_widget.set_center(place.latlng);
        map_widget.set_zoom(place.zoom);
    };
    main_widget.insert_child_before(search_panel, map_widget);

    auto show_search_panel = [&]() {
        if (panel_width != INT_MIN)
            search_panel->set_preferred_width(panel_width);
        search_panel->set_visible(true);
    };
    auto hide_search_panel = [&](bool save_width = true) {
        if (save_width)
            panel_width = search_panel->width();
        search_panel->set_visible(false);
        map_widget.remove_markers_with_name("search"sv);
        search_panel->reset();
    };
    if (init_panel_open_name == "search") {
        show_search_panel();
    } else {
        hide_search_panel(false);
    }

    // Favorites panel
    auto marker_red_image = TRY(Gfx::Bitmap::load_from_file("/res/graphics/maps/marker-red.png"sv));
    auto favorites_panel = TRY(Maps::FavoritesPanel::try_create());
    favorites_panel->on_favorites_change = [&map_widget, marker_red_image](auto const& favorites) {
        // Sync all favorites markers
        map_widget.remove_markers_with_name("favorites"sv);
        for (auto const& favorite : favorites)
            map_widget.add_marker({ favorite.latlng, favorite.name, marker_red_image, "favorites"_string });
    };
    favorites_panel->on_selected_favorite_change = [&map_widget](auto const& favorite) {
        // Zoom into favorite marker
        map_widget.set_center(favorite.latlng);
        map_widget.set_zoom(favorite.zoom);
    };
    favorites_panel->load_favorites();
    main_widget.insert_child_before(favorites_panel, map_widget);

    auto favorites_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hearts.png"sv));
    map_widget.add_context_menu_action(GUI::Action::create(
        "Add to &Favorites", favorites_icon, [favorites_panel, &map_widget](auto&) {
            favorites_panel->add_favorite({ "Unnamed place"_string, map_widget.context_menu_latlng(), map_widget.zoom() });
        },
        window));

    auto show_favorites_panel = [&]() {
        if (panel_width != INT_MIN)
            favorites_panel->set_preferred_width(panel_width);
        favorites_panel->set_visible(true);
    };
    auto hide_favorites_panel = [&](bool save_width = true) {
        if (save_width)
            panel_width = favorites_panel->width();
        favorites_panel->set_visible(false);
        favorites_panel->reset();
    };
    if (init_panel_open_name == "favorites") {
        show_favorites_panel();
    } else {
        hide_favorites_panel(false);
    }

    // Main menu actions
    auto file_menu = window->add_menu("&File"_string);
    auto open_settings_action = GUI::Action::create("Maps &Settings", { Mod_Ctrl, Key_Comma }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-settings.png"sv)), [window](GUI::Action const&) {
        GUI::Process::spawn_or_show_error(window, "/bin/MapsSettings"sv);
    });
    file_menu->add_action(open_settings_action);
    file_menu->add_separator();
    file_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) { GUI::Application::the()->quit(); }));

    auto view_menu = window->add_menu("&View"_string);

    RefPtr<GUI::Action> show_favorites_panel_action;
    auto show_search_panel_action = GUI::Action::create_checkable(
        "Show &search panel", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv)), [&](auto& action) {
            if (favorites_panel->is_visible()) {
                show_favorites_panel_action->set_checked(false);
                hide_favorites_panel();
            }
            if (action.is_checked()) {
                show_search_panel();
            } else {
                hide_search_panel();
            }
        },
        window);
    show_search_panel_action->set_checked(search_panel->is_visible());

    show_favorites_panel_action = GUI::Action::create_checkable(
        "Show &favorites panel", favorites_icon, [&](auto& action) {
            if (search_panel->is_visible()) {
                show_search_panel_action->set_checked(false);
                hide_search_panel();
            }
            if (action.is_checked()) {
                show_favorites_panel();
            } else {
                hide_favorites_panel();
            }
        },
        window);
    show_favorites_panel_action->set_checked(favorites_panel->is_visible());

    auto show_users_action = GUI::Action::create_checkable(
        "Show SerenityOS &users", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/ladyball.png"sv)), [&map_widget](auto& action) { map_widget.set_show_users(action.is_checked()); }, window);
    show_users_action->set_checked(map_widget.show_users());
    auto zoom_in_action = GUI::CommonActions::make_zoom_in_action([&map_widget](auto&) { map_widget.set_zoom(map_widget.zoom() + 1); }, window);
    auto zoom_out_action = GUI::CommonActions::make_zoom_out_action([&map_widget](auto&) { map_widget.set_zoom(map_widget.zoom() - 1); }, window);
    auto reset_zoom_action = GUI::CommonActions::make_reset_zoom_action([&map_widget](auto&) { map_widget.set_zoom(MAP_ZOOM_DEFAULT); }, window);
    auto fullscreen_action = GUI::CommonActions::make_fullscreen_action([window, &toolbar_container, &map_widget](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
        toolbar_container.set_visible(!window->is_fullscreen());
        map_widget.set_frame_style(window->is_fullscreen() ? Gfx::FrameStyle::NoFrame : Gfx::FrameStyle::SunkenContainer);
    },
        window);
    view_menu->add_action(show_search_panel_action);
    view_menu->add_action(adopt_ref(*show_favorites_panel_action));
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
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man1/Applications/Maps.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Maps"_string, app_icon, window));

    // Main toolbar actions
    toolbar.add_action(show_search_panel_action);
    toolbar.add_action(adopt_ref(*show_favorites_panel_action));
    toolbar.add_separator();
    toolbar.add_action(show_users_action);
    toolbar.add_separator();
    toolbar.add_action(zoom_in_action);
    toolbar.add_action(zoom_out_action);
    toolbar.add_action(reset_zoom_action);
    toolbar.add_separator();
    toolbar.add_action(open_settings_action);

    window->show();

    // Remember last window state
    int exec = app->exec();

    if (search_panel->is_visible()) {
        Config::write_string("Maps"sv, "Panel"sv, "OpenName"sv, "search"sv);
        Config::write_i32("Maps"sv, "Panel"sv, "Width"sv, search_panel->width());
    } else if (favorites_panel->is_visible()) {
        Config::write_string("Maps"sv, "Panel"sv, "OpenName"sv, "favorites"sv);
        Config::write_i32("Maps"sv, "Panel"sv, "Width"sv, favorites_panel->width());
    } else {
        Config::remove_key("Maps"sv, "Panel"sv, "OpenName"sv);
        Config::remove_key("Maps"sv, "Panel"sv, "Width"sv);
    }

    Config::write_string("Maps"sv, "MapView"sv, "CenterLatitude"sv, String::number(map_widget.center().latitude));
    Config::write_string("Maps"sv, "MapView"sv, "CenterLongitude"sv, String::number(map_widget.center().longitude));
    Config::write_i32("Maps"sv, "MapView"sv, "Zoom"sv, map_widget.zoom());
    Config::write_bool("Maps"sv, "MapView"sv, "ShowUsers"sv, map_widget.show_users());
    return exec;
}
