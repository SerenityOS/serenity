/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShutdownDialog.h"
#include "TaskbarWindow.h"
#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <AK/Try.h>
#include <LibConfig/Client.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibMain/Main.h>
#include <WindowServer/Window.h>
#include <serenity.h>
#include <signal.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

static ErrorOr<Vector<DeprecatedString>> discover_apps_and_categories();
static ErrorOr<NonnullRefPtr<GUI::Menu>> build_system_menu(GUI::Window&);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix sigaction"));
    auto app = TRY(GUI::Application::create(arguments));
    Config::pledge_domains({ "Taskbar", "Calendar" });
    Config::monitor_domain("Taskbar");
    Config::monitor_domain("Calendar");
    app->event_loop().register_signal(SIGCHLD, [](int) {
        // Wait all available children
        while (waitpid(-1, nullptr, WNOHANG) > 0)
            ;
    });

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));

    GUI::ConnectionToWindowManagerServer::the();
    Desktop::Launcher::ensure_connection();

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath"));

    auto window = TRY(TaskbarWindow::create());

    auto menu = TRY(build_system_menu(*window));
    menu->realize_menu_if_needed();
    window->add_system_menu(menu);

    window->show();

    window->make_window_manager(
        WindowServer::WMEventMask::WindowStateChanges
        | WindowServer::WMEventMask::WindowRemovals
        | WindowServer::WMEventMask::WindowIconChanges
        | WindowServer::WMEventMask::WorkspaceChanges);

    return app->exec();
}

struct AppMetadata {
    DeprecatedString executable;
    DeprecatedString name;
    DeprecatedString category;
    DeprecatedString working_directory;
    GUI::Icon icon;
    bool run_in_terminal;
    bool requires_root;
};
Vector<AppMetadata> g_apps;

Color g_menu_selection_color;

Vector<Gfx::SystemThemeMetaData> g_themes;
RefPtr<GUI::Menu> g_themes_menu;
GUI::ActionGroup g_themes_group;

ErrorOr<Vector<DeprecatedString>> discover_apps_and_categories()
{
    HashTable<DeprecatedString> seen_app_categories;
    Desktop::AppFile::for_each([&](auto af) {
        if (af->exclude_from_system_menu())
            return;
        if (access(af->executable().characters(), X_OK) == 0) {
            g_apps.append({ af->executable(), af->name(), af->category(), af->working_directory(), af->icon(), af->run_in_terminal(), af->requires_root() });
            seen_app_categories.set(af->category());
        }
    });
    quick_sort(g_apps, [](auto& a, auto& b) { return a.name < b.name; });

    Vector<DeprecatedString> sorted_app_categories;
    TRY(sorted_app_categories.try_ensure_capacity(seen_app_categories.size()));
    for (auto const& category : seen_app_categories)
        sorted_app_categories.unchecked_append(category);
    quick_sort(sorted_app_categories);

    return sorted_app_categories;
}

ErrorOr<NonnullRefPtr<GUI::Menu>> build_system_menu(GUI::Window& window)
{
    Vector<DeprecatedString> const sorted_app_categories = TRY(discover_apps_and_categories());
    auto system_menu = TRY(GUI::Menu::try_create("\xE2\x9A\xA1"_string)); // HIGH VOLTAGE SIGN

    system_menu->add_action(GUI::Action::create("&About SerenityOS", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/ladyball.png"sv)), [&](auto&) {
        GUI::Process::spawn_or_show_error(&window, "/bin/About"sv);
    }));

    system_menu->add_separator();

    // First we construct all the necessary app category submenus.
    auto category_icons = TRY(Core::ConfigFile::open("/res/icons/SystemMenu.ini"));
    HashMap<DeprecatedString, NonnullRefPtr<GUI::Menu>> app_category_menus;

    Function<void(DeprecatedString const&)> create_category_menu;
    create_category_menu = [&](DeprecatedString const& category) {
        if (app_category_menus.contains(category))
            return;
        DeprecatedString parent_category, child_category = category;
        for (ssize_t i = category.length() - 1; i >= 0; i--) {
            if (category[i] == '/') {
                parent_category = category.substring(0, i);
                child_category = category.substring(i + 1);
            }
        }
        GUI::Menu* parent_menu;
        if (parent_category.is_empty()) {
            parent_menu = system_menu;
        } else {
            parent_menu = app_category_menus.get(parent_category).value();
            if (!parent_menu) {
                create_category_menu(parent_category);
                parent_menu = app_category_menus.get(parent_category).value();
                VERIFY(parent_menu);
            }
        }
        auto category_menu = parent_menu->add_submenu(String::from_deprecated_string(child_category).release_value_but_fixme_should_propagate_errors());
        auto category_icon_path = category_icons->read_entry("16x16", category);
        if (!category_icon_path.is_empty()) {
            auto icon_or_error = Gfx::Bitmap::load_from_file(category_icon_path);
            if (!icon_or_error.is_error())
                category_menu->set_icon(icon_or_error.release_value());
        }
        app_category_menus.set(category, category_menu);
    };

    for (auto const& category : sorted_app_categories)
        create_category_menu(category);

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 0;
    for (auto const& app : g_apps) {
        auto icon = app.icon.bitmap_for_size(16);

        if constexpr (SYSTEM_MENU_DEBUG) {
            if (icon)
                dbgln("App {} has icon with size {}", app.name, icon->size());
        }

        auto parent_menu = app_category_menus.get(app.category).value_or(system_menu.ptr());
        parent_menu->add_action(GUI::Action::create(app.name, icon, [app_identifier, &window](auto&) {
            dbgln("Activated app with ID {}", app_identifier);
            auto& app = g_apps[app_identifier];
            StringView executable;
            Vector<char const*, 2> arguments;
            // FIXME: These single quotes won't be enough for executables with single quotes in their name.
            auto pls_with_executable = DeprecatedString::formatted("/bin/pls '{}'", app.executable);
            if (app.run_in_terminal && !app.requires_root) {
                executable = "/bin/Terminal"sv;
                arguments = { "-e", app.executable.characters() };
            } else if (!app.run_in_terminal && app.requires_root) {
                executable = "/bin/Escalator"sv;
                arguments = { app.executable.characters() };
            } else if (app.run_in_terminal && app.requires_root) {
                executable = "/bin/Terminal"sv;
                arguments = { "-e", pls_with_executable.characters() };
            } else {
                executable = app.executable;
            }
            GUI::Process::spawn_or_show_error(&window, executable, arguments,
                app.working_directory.is_empty() ? Core::StandardPaths::home_directory() : app.working_directory);
        }));
        ++app_identifier;
    }

    system_menu->add_separator();

    g_themes_group.set_exclusive(true);
    g_themes_group.set_unchecking_allowed(false);

    g_themes_menu = system_menu->add_submenu("&Themes"_string);
    g_themes_menu->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/themes.png"sv)));

    g_themes = TRY(Gfx::list_installed_system_themes());
    auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();

    {
        int theme_identifier = 0;
        for (auto& theme : g_themes) {
            auto action = GUI::Action::create_checkable(theme.name, [theme_identifier, &window](auto&) {
                auto& theme = g_themes[theme_identifier];
                dbgln("Theme switched to {} at path {}", theme.name, theme.path);
                if (window.main_widget()->palette().color_scheme_path() != ""sv)
                    VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(theme.path, theme.name, false, GUI::ConnectionToWindowServer::the().get_preferred_color_scheme()));
                else
                    VERIFY(GUI::ConnectionToWindowServer::the().set_system_theme(theme.path, theme.name, false, "Custom"sv));
            });
            if (theme.name == current_theme_name)
                action->set_checked(true);
            g_themes_group.add_action(action);
            g_themes_menu->add_action(action);
            ++theme_identifier;
        }
    }

    GUI::Application::the()->on_theme_change = [&]() {
        if (g_themes_menu->is_visible())
            return;
        auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();
        auto theme_overridden = GUI::ConnectionToWindowServer::the().is_system_theme_overridden();
        for (size_t index = 0; index < g_themes.size(); ++index) {
            auto* action = g_themes_menu->action_at(index);
            action->set_checked(!theme_overridden && action->text() == current_theme_name);
        }
    };

    system_menu->add_action(GUI::Action::create("&Settings", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-settings.png"sv)), [&](auto&) {
        GUI::Process::spawn_or_show_error(&window, "/bin/Settings"sv);
    }));

    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("&Help", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-help.png"sv)), [&](auto&) {
        GUI::Process::spawn_or_show_error(&window, "/bin/Help"sv);
    }));
    system_menu->add_action(GUI::Action::create("&Run...", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-run.png"sv)), [&](auto&) {
        GUI::Process::spawn_or_show_error(&window, "/bin/Run"sv, ReadonlySpan<StringView> {}, Core::StandardPaths::home_directory());
    }));
    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("E&xit...", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/power.png"sv)), [&](auto&) {
        if (auto command = ShutdownDialog::show(); command.has_value())
            GUI::Process::spawn_or_show_error(&window, command->executable, command->arguments);
    }));

    return system_menu;
}
