/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "BookmarksBarWidget.h"
#include "Browser.h"
#include "InspectorWidget.h"
#include "Tab.h"
#include "WindowActions.h"
#include <AK/StringBuilder.h>
#include <Applications/Browser/BrowserWindowUI.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <stdio.h>
#include <stdlib.h>

namespace Browser {

String g_home_url;
bool g_multi_process = false;

static String bookmarks_file_path()
{
    StringBuilder builder;
    builder.append(Core::StandardPaths::config_directory());
    builder.append("/bookmarks.json");
    return builder.to_string();
}

}

int main(int argc, char** argv)
{
    if (getuid() == 0) {
        warn() << "Refusing to run as root";
        return 1;
    }

    if (pledge("stdio shared_buffer accept unix cpath rpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* specified_url = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(Browser::g_multi_process, "Multi-process mode", "multi-process", 'm');
    args_parser.add_positional_argument(specified_url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    // Connect to the ProtocolServer immediately so we can drop the "unix" pledge.
    Web::ResourceLoader::the();

    // FIXME: Once there is a standalone Download Manager, we can drop the "unix" pledge.
    if (pledge("stdio shared_buffer accept unix cpath rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/home", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    // FIXME: Once there is a standalone Download Manager, we don't need to unveil this
    if (unveil("/tmp/portal/launch", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/image", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto m_config = Core::ConfigFile::get_for_app("Browser");
    Browser::g_home_url = m_config->read_entry("Preferences", "Home", "about:blank");

    bool bookmarksbar_enabled = true;
    auto bookmarks_bar = Browser::BookmarksBarWidget::construct(Browser::bookmarks_file_path(), bookmarksbar_enabled);

    auto window = GUI::Window::construct();
    window->resize(640, 480);
    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-browser.png"));
    window->set_title("Browser");

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_json(browser_window_ui_json);

    auto& tab_widget = static_cast<GUI::TabWidget&>(*widget.find_descendant_by_name("tab_widget"));

    auto default_favicon = Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png");
    ASSERT(default_favicon);

    tab_widget.on_change = [&](auto& active_widget) {
        auto& tab = static_cast<Browser::Tab&>(active_widget);
        window->set_title(String::formatted("{} - Browser", tab.title()));
        tab.did_become_active();
    };

    tab_widget.on_middle_click = [&](auto& clicked_widget) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.on_tab_close_request(tab);
    };

    tab_widget.on_context_menu_request = [&](auto& clicked_widget, const GUI::ContextMenuEvent& context_menu_event) {
        auto& tab = static_cast<Browser::Tab&>(clicked_widget);
        tab.context_menu_requested(context_menu_event.screen_position());
    };

    Browser::WindowActions window_actions(*window);

    Function<void(URL url, bool activate)> create_new_tab;
    create_new_tab = [&](auto url, auto activate) {
        auto type = Browser::g_multi_process ? Browser::Tab::Type::OutOfProcessWebView : Browser::Tab::Type::InProcessWebView;
        auto& new_tab = tab_widget.add_tab<Browser::Tab>("New tab", type);

        tab_widget.set_bar_visible(!window->is_fullscreen() && tab_widget.children().size() > 1);
        tab_widget.set_tab_icon(new_tab, default_favicon);

        new_tab.on_title_change = [&](auto title) {
            tab_widget.set_tab_title(new_tab, title);
            if (tab_widget.active_widget() == &new_tab)
                window->set_title(String::formatted("{} - Browser", title));
        };

        new_tab.on_favicon_change = [&](auto& bitmap) {
            tab_widget.set_tab_icon(new_tab, &bitmap);
        };

        new_tab.on_tab_open_request = [&](auto& url) {
            create_new_tab(url, true);
        };

        new_tab.on_tab_close_request = [&](auto& tab) {
            tab_widget.deferred_invoke([&](auto&) {
                tab_widget.remove_tab(tab);
                tab_widget.set_bar_visible(!window->is_fullscreen() && tab_widget.children().size() > 1);
                if (tab_widget.children().is_empty())
                    app->quit();
            });
        };

        new_tab.load(url);

        dbgln("Added new tab {:p}, loading {}", &new_tab, url);

        if (activate)
            tab_widget.set_active_widget(&new_tab);
    };

    URL first_url = Browser::g_home_url;
    if (specified_url) {
        if (Core::File::exists(specified_url)) {
            first_url = URL::create_with_file_protocol(Core::File::real_path_for(specified_url));
        } else {
            first_url = Browser::url_from_user_input(specified_url);
        }
    }

    window_actions.on_create_new_tab = [&] {
        create_new_tab(Browser::g_home_url, true);
    };

    window_actions.on_next_tab = [&] {
        tab_widget.activate_next_tab();
    };

    window_actions.on_previous_tab = [&] {
        tab_widget.activate_previous_tab();
    };

    window_actions.on_about = [&] {
        GUI::AboutDialog::show("Browser", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-browser.png"), window);
    };

    window_actions.on_show_bookmarks_bar = [&](auto& action) {
        Browser::BookmarksBarWidget::the().set_visible(action.is_checked());
    };
    window_actions.show_bookmarks_bar_action().set_checked(bookmarksbar_enabled);

    create_new_tab(first_url, true);
    window->show();

    return app->exec();
}
