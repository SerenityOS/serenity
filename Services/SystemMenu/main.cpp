/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "ShutdownDialog.h"
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Menu.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <serenity.h>
#include <spawn.h>

//#define SYSTEM_MENU_DEBUG

struct AppMetadata {
    String executable;
    String name;
    String icon_path;
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

static Vector<String> discover_apps_and_categories();
static NonnullRefPtr<GUI::Menu> build_system_menu();

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    app->set_quit_when_last_window_deleted(false);

    auto menu = build_system_menu();
    menu->realize_menu_if_needed();

    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetSystemMenu>(menu->menu_id());

    if (pledge("stdio shared_buffer accept rpath proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (chdir(Core::StandardPaths::home_directory().characters()) < 0) {
        perror("chdir");
        return 1;
    }

    if (unveil("/bin", "x")) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r")) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    return app->exec();
}

Vector<String> discover_apps_and_categories()
{
    HashTable<String> seen_app_categories;
    Core::DirIterator dt("/res/apps", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto af_name = dt.next_path();
        auto af_path = String::format("/res/apps/%s", af_name.characters());
        auto af = Core::ConfigFile::open(af_path);
        if (!af->has_key("App", "Name") || !af->has_key("App", "Executable"))
            continue;
        auto app_name = af->read_entry("App", "Name");
        auto app_executable = af->read_entry("App", "Executable");
        auto app_category = af->read_entry("App", "Category");
        auto app_icon_path = af->read_entry("Icons", "16x16");
        g_apps.append({ app_executable, app_name, app_icon_path, app_category });
        seen_app_categories.set(app_category);
    }
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
        RefPtr<Gfx::Bitmap> icon;
        if (!app.icon_path.is_empty())
            icon = Gfx::Bitmap::load_from_file(app.icon_path);

#ifdef SYSTEM_MENU_DEBUG
        if (icon)
            dbg() << "App " << app.name << " has icon with size " << icon->size();
#endif

        auto parent_menu = app_category_menus.get(app.category).value_or(*system_menu);
        parent_menu->add_action(GUI::Action::create(app.name, icon.ptr(), [app_identifier](auto&) {
            dbg() << "Activated app with ID " << app_identifier;
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
            auto theme_path = String::format("/res/themes/%s", theme_name.characters());
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
                dbg() << "Theme switched to " << theme.name << " at path " << theme.path;
                auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetSystemTheme>(theme.path, theme.name);
                ASSERT(response->success());
            });
            if (theme.name == current_theme_name)
                action->set_checked(true);
            g_themes_group.add_action(action);
            g_themes_menu->add_action(action);
            ++theme_identifier;
        }
    }

    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("About...", Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png"), [](auto&) {
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
