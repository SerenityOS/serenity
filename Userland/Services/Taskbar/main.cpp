/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ShutdownDialog.h"
#include "TaskbarWindow.h"
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibDesktop/AppFile.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <LibGUI/WindowServerConnection.h>
#include <WindowServer/Window.h>
#include <serenity.h>
#include <signal.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

static Vector<String> discover_apps_and_categories();
static NonnullRefPtr<GUI::Menu> build_system_menu();

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd accept proc exec rpath unix cpath fattr sigaction", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    app->event_loop().register_signal(SIGCHLD, [](int) {
        // Wait all available children
        while (waitpid(-1, nullptr, WNOHANG) > 0)
            ;
    });

    // We need to obtain the WM connection here as well before the pledge shortening.
    GUI::WindowManagerServerConnection::the();

    if (pledge("stdio recvfd sendfd accept proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto menu = build_system_menu();
    menu->realize_menu_if_needed();

    auto window = TaskbarWindow::construct(move(menu));
    window->show();

    window->make_window_manager(
        WindowServer::WMEventMask::WindowStateChanges
        | WindowServer::WMEventMask::WindowRemovals
        | WindowServer::WMEventMask::WindowIconChanges);

    return app->exec();
}

struct AppMetadata {
    String executable;
    String name;
    String category;
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

Vector<String> discover_apps_and_categories()
{
    HashTable<String> seen_app_categories;
    Desktop::AppFile::for_each([&](auto af) {
        g_apps.append({ af->executable(), af->name(), af->category() });
        seen_app_categories.set(af->category());
    });
    quick_sort(g_apps, [](auto& a, auto& b) { return a.name < b.name; });

    Vector<String> sorted_app_categories;
    for (auto& category : seen_app_categories) {
        sorted_app_categories.append(category);
    }
    quick_sort(sorted_app_categories);

    return sorted_app_categories;
}

NonnullRefPtr<GUI::Menu> build_system_menu()
{
    const Vector<String> sorted_app_categories = discover_apps_and_categories();
    auto system_menu = GUI::Menu::construct("\xE2\x9A\xA1"); // HIGH VOLTAGE SIGN

    system_menu->add_action(GUI::Action::create("About SerenityOS", Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png"), [](auto&) {
        pid_t child_pid;
        const char* argv[] = { "/bin/About", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/About", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }));

    system_menu->add_separator();

    // First we construct all the necessary app category submenus.
    HashMap<String, NonnullRefPtr<GUI::Menu>> app_category_menus;
    auto category_icons = Core::ConfigFile::open("/res/icons/SystemMenu.ini");
    for (const auto& category : sorted_app_categories) {
        if (app_category_menus.contains(category))
            continue;
        auto& category_menu = system_menu->add_submenu(category);
        auto category_icon_path = category_icons->read_entry("16x16", category);
        if (!category_icon_path.is_empty()) {
            auto icon = Gfx::Bitmap::load_from_file(category_icon_path);
            category_menu.set_icon(icon);
        }
        app_category_menus.set(category, category_menu);
    }

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 0;
    for (const auto& app : g_apps) {
        auto icon = GUI::FileIconProvider::icon_for_executable(app.executable).bitmap_for_size(16);

        if constexpr (SYSTEM_MENU_DEBUG) {
            if (icon)
                dbgln("App {} has icon with size {}", app.name, icon->size());
        }

        auto parent_menu = app_category_menus.get(app.category).value_or(*system_menu);
        parent_menu->add_action(GUI::Action::create(app.name, icon, [app_identifier](auto&) {
            dbgln("Activated app with ID {}", app_identifier);
            const auto& bin = g_apps[app_identifier].executable;
            pid_t child_pid;
            const char* argv[] = { bin.characters(), nullptr };
            if ((errno = posix_spawn(&child_pid, bin.characters(), nullptr, nullptr, const_cast<char**>(argv), environ))) {
                perror("posix_spawn");
            } else {
                if (disown(child_pid) < 0)
                    perror("disown");
            }
        }));
        ++app_identifier;
    }

    system_menu->add_separator();

    g_themes_group.set_exclusive(true);
    g_themes_group.set_unchecking_allowed(false);

    g_themes_menu = &system_menu->add_submenu("Themes");
    g_themes_menu->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/themes.png"));

    {
        Core::DirIterator dt("/res/themes", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto theme_name = dt.next_path();
            auto theme_path = String::formatted("/res/themes/{}", theme_name);
            g_themes.append({ LexicalPath(theme_name).title(), theme_path });
        }
        quick_sort(g_themes, [](auto& a, auto& b) { return a.name < b.name; });
    }

    auto current_theme_name = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::GetSystemTheme>()->theme_name();

    {
        int theme_identifier = 0;
        for (auto& theme : g_themes) {
            auto action = GUI::Action::create_checkable(theme.name, [theme_identifier](auto&) {
                auto& theme = g_themes[theme_identifier];
                dbgln("Theme switched to {} at path {}", theme.name, theme.path);
                auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetSystemTheme>(theme.path, theme.name);
                VERIFY(response->success());
            });
            if (theme.name == current_theme_name)
                action->set_checked(true);
            g_themes_group.add_action(action);
            g_themes_menu->add_action(action);
            ++theme_identifier;
        }
    }

    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("Help", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-help.png"), [](auto&) {
        pid_t child_pid;
        const char* argv[] = { "/bin/Help", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/Help", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }));
    system_menu->add_action(GUI::Action::create("Run...", Gfx::Bitmap::load_from_file("/res/icons/16x16/app-run.png"), [](auto&) {
        pid_t child_pid;
        const char* argv[] = { "/bin/Run", nullptr };
        if ((errno = posix_spawn(&child_pid, "/bin/Run", nullptr, nullptr, const_cast<char**>(argv), environ))) {
            perror("posix_spawn");
        } else {
            if (disown(child_pid) < 0)
                perror("disown");
        }
    }));
    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("Exit...", Gfx::Bitmap::load_from_file("/res/icons/16x16/power.png"), [](auto&) {
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
