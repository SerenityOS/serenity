/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SessionExitInhibitionDialog.h"
#include "ShutdownDialog.h"
#include "TaskbarWindow.h"
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibConfig/Client.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Process.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/AppFile.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowMangerServer.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Menu.h>
#include <LibMain/Main.h>
#include <LibSession/Session.h>
#include <WindowServer/Window.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

static ErrorOr<Vector<String>> discover_apps_and_categories();
static ErrorOr<NonnullRefPtr<GUI::Menu>> build_system_menu();

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix sigaction"));
    auto app = TRY(GUI::Application::try_create(arguments));
    Config::pledge_domain("Taskbar");
    Config::monitor_domain("Taskbar");
    app->event_loop().register_signal(SIGCHLD, [](int) {
        // Wait all available children
        while (waitpid(-1, nullptr, WNOHANG) > 0)
            ;
    });

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));

    GUI::ConnectionToWindowMangerServer::the();
    Desktop::Launcher::ensure_connection();

    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));

    auto menu = TRY(build_system_menu());
    menu->realize_menu_if_needed();

    auto window = TRY(TaskbarWindow::try_create(move(menu)));
    window->show();

    window->make_window_manager(
        WindowServer::WMEventMask::WindowStateChanges
        | WindowServer::WMEventMask::WindowRemovals
        | WindowServer::WMEventMask::WindowIconChanges
        | WindowServer::WMEventMask::WorkspaceChanges);

    return app->exec();
}

struct AppMetadata {
    String executable;
    String name;
    String category;
    GUI::Icon icon;
    bool run_in_terminal;
};
Vector<AppMetadata> g_apps;

struct ThemeMetadata {
    String name;
    String path;
};

Color g_menu_selection_color;

Vector<ThemeMetadata> g_themes;
RefPtr<GUI::Menu> g_themes_menu;
GUI::ActionGroup g_themes_group;

ErrorOr<Vector<String>> discover_apps_and_categories()
{
    HashTable<String> seen_app_categories;
    Desktop::AppFile::for_each([&](auto af) {
        if (access(af->executable().characters(), X_OK) == 0) {
            g_apps.append({ af->executable(), af->name(), af->category(), af->icon(), af->run_in_terminal() });
            seen_app_categories.set(af->category());
        }
    });
    quick_sort(g_apps, [](auto& a, auto& b) { return a.name < b.name; });

    Vector<String> sorted_app_categories;
    TRY(sorted_app_categories.try_ensure_capacity(seen_app_categories.size()));
    for (auto const& category : seen_app_categories)
        sorted_app_categories.unchecked_append(category);
    quick_sort(sorted_app_categories);

    return sorted_app_categories;
}

ErrorOr<NonnullRefPtr<GUI::Menu>> build_system_menu()
{
    Vector<String> const sorted_app_categories = TRY(discover_apps_and_categories());
    auto system_menu = TRY(GUI::Menu::try_create("\xE2\x9A\xA1")); // HIGH VOLTAGE SIGN

    system_menu->add_action(GUI::Action::create("&About SerenityOS", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/ladyball.png").release_value_but_fixme_should_propagate_errors(), [](auto&) {
        Core::Process::spawn("/bin/About"sv);
    }));

    system_menu->add_separator();

    // First we construct all the necessary app category submenus.
    auto category_icons = TRY(Core::ConfigFile::open("/res/icons/SystemMenu.ini"));
    HashMap<String, NonnullRefPtr<GUI::Menu>> app_category_menus;

    Function<void(String const&)> create_category_menu;
    create_category_menu = [&](String const& category) {
        if (app_category_menus.contains(category))
            return;
        String parent_category, child_category = category;
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
        auto& category_menu = parent_menu->add_submenu(child_category);
        auto category_icon_path = category_icons->read_entry("16x16", category);
        if (!category_icon_path.is_empty()) {
            auto icon_or_error = Gfx::Bitmap::try_load_from_file(category_icon_path);
            if (!icon_or_error.is_error())
                category_menu.set_icon(icon_or_error.release_value());
        }
        app_category_menus.set(category, category_menu);
    };

    for (const auto& category : sorted_app_categories) {
        if (category != "Settings"sv)
            create_category_menu(category);
    }

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 0;
    for (const auto& app : g_apps) {
        if (app.category == "Settings"sv) {
            ++app_identifier;
            continue;
        }

        auto icon = app.icon.bitmap_for_size(16);

        if constexpr (SYSTEM_MENU_DEBUG) {
            if (icon)
                dbgln("App {} has icon with size {}", app.name, icon->size());
        }

        auto parent_menu = app_category_menus.get(app.category).value_or(system_menu.ptr());
        parent_menu->add_action(GUI::Action::create(app.name, icon, [app_identifier](auto&) {
            dbgln("Activated app with ID {}", app_identifier);
            auto& app = g_apps[app_identifier];
            char const* argv[4] { nullptr, nullptr, nullptr, nullptr };
            if (app.run_in_terminal) {
                argv[0] = "/bin/Terminal";
                argv[1] = "-e";
                argv[2] = app.executable.characters();
            } else {
                argv[0] = app.executable.characters();
            }

            posix_spawn_file_actions_t spawn_actions;
            posix_spawn_file_actions_init(&spawn_actions);
            auto home_directory = Core::StandardPaths::home_directory();
            posix_spawn_file_actions_addchdir(&spawn_actions, home_directory.characters());

            pid_t child_pid;
            if ((errno = posix_spawn(&child_pid, argv[0], &spawn_actions, nullptr, const_cast<char**>(argv), environ))) {
                perror("posix_spawn");
            } else {
                if (disown(child_pid) < 0)
                    perror("disown");
            }
            posix_spawn_file_actions_destroy(&spawn_actions);
        }));
        ++app_identifier;
    }

    system_menu->add_separator();

    g_themes_group.set_exclusive(true);
    g_themes_group.set_unchecking_allowed(false);

    g_themes_menu = &system_menu->add_submenu("&Themes");
    g_themes_menu->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/themes.png").release_value_but_fixme_should_propagate_errors());

    {
        Core::DirIterator dt("/res/themes", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto theme_name = dt.next_path();
            auto theme_path = String::formatted("/res/themes/{}", theme_name);
            g_themes.append({ LexicalPath::title(theme_name), theme_path });
        }
        quick_sort(g_themes, [](auto& a, auto& b) { return a.name < b.name; });
    }

    auto current_theme_name = GUI::ConnectionToWindowServer::the().get_system_theme();

    {
        int theme_identifier = 0;
        for (auto& theme : g_themes) {
            auto action = GUI::Action::create_checkable(theme.name, [theme_identifier](auto&) {
                auto& theme = g_themes[theme_identifier];
                dbgln("Theme switched to {} at path {}", theme.name, theme.path);
                auto success = GUI::ConnectionToWindowServer::the().set_system_theme(theme.path, theme.name);
                VERIFY(success);
            });
            if (theme.name == current_theme_name)
                action->set_checked(true);
            g_themes_group.add_action(action);
            g_themes_menu->add_action(action);
            ++theme_identifier;
        }
    }

    system_menu->add_action(GUI::Action::create("&Settings", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-settings.png").release_value_but_fixme_should_propagate_errors(), [](auto&) {
        Core::Process::spawn("/bin/Settings"sv);
    }));

    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("&Help", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-help.png").release_value_but_fixme_should_propagate_errors(), [](auto&) {
        Core::Process::spawn("/bin/Help"sv);
    }));
    system_menu->add_action(GUI::Action::create("&Run...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-run.png").release_value_but_fixme_should_propagate_errors(), [](auto&) {
        posix_spawn_file_actions_t spawn_actions;
        posix_spawn_file_actions_init(&spawn_actions);
        auto home_directory = Core::StandardPaths::home_directory();
        posix_spawn_file_actions_addchdir(&spawn_actions, home_directory.characters());

        pid_t child_pid;
        const char* argv[] = { "/bin/Run", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/Run", &spawn_actions, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }

        posix_spawn_file_actions_destroy(&spawn_actions);
    }));
    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("E&xit...", Gfx::Bitmap::try_load_from_file("/res/icons/16x16/power.png").release_value_but_fixme_should_propagate_errors(), [](auto&) {
        if (Session::Session::the().is_exit_inhibited()) {
            auto result = SessionExitInhibitionDialog::show();
            if (result == SessionExitInhibitionDialog::ExecResult::ExecCancel)
                return;
        }

        auto command = ShutdownDialog::show();

        if (command.size() == 0)
            return;

        pid_t child_pid;
        if ((errno = posix_spawn(&child_pid, command[0], nullptr, nullptr, const_cast<char**>(command.data()), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }));

    return system_menu;
}
