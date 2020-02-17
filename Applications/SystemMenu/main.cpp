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

#include <AK/FileSystemPath.h>
#include <AK/QuickSort.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <stdio.h>
#include <sys/utsname.h>
#include <unistd.h>

struct AppMetadata {
    String executable;
    String name;
    String icon_path;
    String category;
};
Vector<AppMetadata> g_apps;

HashMap<String, NonnullRefPtr<GUI::Menu>> g_app_category_menus;

struct ThemeMetadata {
    String name;
    String path;
};

Color g_menu_selection_color;

Vector<ThemeMetadata> g_themes;
RefPtr<GUI::Menu> g_themes_menu;

static NonnullRefPtr<GUI::Menu> build_system_menu();

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto menu = build_system_menu();
    menu->realize_menu_if_needed();

    GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetSystemMenu>(menu->menu_id());

    if (pledge("stdio shared_buffer accept rpath proc exec", nullptr) < 0) {
        perror("pledge");
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

    return app.exec();
}

NonnullRefPtr<GUI::Menu> build_system_menu()
{
    HashTable<String> seen_app_categories;
    {
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
    }

    Vector<String> sorted_app_categories;
    for (auto& category : seen_app_categories)
        sorted_app_categories.append(category);
    quick_sort(sorted_app_categories.begin(), sorted_app_categories.end(), [](auto& a, auto& b) { return a < b; });

    u8 system_menu_name[] = { 0xc3, 0xb8, 0 };
    auto system_menu = GUI::Menu::construct(String((const char*)system_menu_name));

    // First we construct all the necessary app category submenus.
    for (const auto& category : sorted_app_categories) {

        if (g_app_category_menus.contains(category))
            continue;
        auto category_menu = GUI::Menu::construct(category);
        system_menu->add_submenu(category_menu);
        g_app_category_menus.set(category, move(category_menu));
    }

    // Then we create and insert all the app menu items into the right place.
    int app_identifier = 0;
    for (const auto& app : g_apps) {
        RefPtr<Gfx::Bitmap> icon;
        if (!app.icon_path.is_empty())
            icon = Gfx::Bitmap::load_from_file(app.icon_path);

        if (icon)
            dbg() << "App " << app.name << " has icon with size " << icon->size();

        auto parent_menu = g_app_category_menus.get(app.category).value_or(*system_menu);
        parent_menu->add_action(GUI::Action::create(app.name, icon.ptr(), [app_identifier](auto&) {
            dbg() << "Activated app with ID " << app_identifier;
            if (fork() == 0) {
                const auto& bin = g_apps[app_identifier].executable;
                execl(bin.characters(), bin.characters(), nullptr);
                ASSERT_NOT_REACHED();
            }
        }));
        ++app_identifier;
    }

    system_menu->add_separator();

    g_themes_menu = GUI::Menu::construct("Themes");

    system_menu->add_submenu(*g_themes_menu);

    {
        Core::DirIterator dt("/res/themes", Core::DirIterator::SkipDots);
        while (dt.has_next()) {
            auto theme_name = dt.next_path();
            auto theme_path = String::format("/res/themes/%s", theme_name.characters());
            g_themes.append({ FileSystemPath(theme_name).title(), theme_path });
        }
        quick_sort(g_themes.begin(), g_themes.end(), [](auto& a, auto& b) { return a.name < b.name; });
    }

    {
        int theme_identifier = 0;
        for (auto& theme : g_themes) {
            g_themes_menu->add_action(GUI::Action::create(theme.name, [theme_identifier](auto&) {
                auto& theme = g_themes[theme_identifier];
                dbg() << "Theme switched to " << theme.name << " at path " << theme.path;
                auto response = GUI::WindowServerConnection::the().send_sync<Messages::WindowServer::SetSystemTheme>(theme.path, theme.name);
                ASSERT(response->success());
            }));
            ++theme_identifier;
        }
    }

    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("About...", Gfx::Bitmap::load_from_file("/res/icons/16x16/ladybug.png"), [](auto&) {
        if (fork() == 0) {
            execl("/bin/About", "/bin/About", nullptr);
            ASSERT_NOT_REACHED();
        }
    }));
    system_menu->add_separator();
    system_menu->add_action(GUI::Action::create("Shutdown...", [](auto&) {
        if (fork() == 0) {
            execl("/bin/SystemDialog", "/bin/SystemDialog", "--shutdown", nullptr);
            ASSERT_NOT_REACHED();
        }
    }));

    return system_menu;
}
