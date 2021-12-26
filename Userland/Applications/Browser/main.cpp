/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BookmarksBarWidget.h"
#include "Browser.h"
#include "CookieJar.h"
#include "Tab.h"
#include "WindowActions.h"
#include <AK/StringBuilder.h>
#include <Applications/Browser/BrowserWindowGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/HTML/WebSocket.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

namespace Browser {

String g_home_url;
static bool s_single_process = false;

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
        warnln("Refusing to run as root");
        return 1;
    }

    if (pledge("stdio recvfd sendfd unix cpath rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    const char* specified_url = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(Browser::s_single_process, "Single-process mode", "single-process", 's');
    args_parser.add_positional_argument(specified_url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    if (Browser::s_single_process) {
        // Connect to the RequestServer and the WebSocket service immediately so we don't need to unveil their portals.
        Web::ResourceLoader::the();
        Web::HTML::WebSocketClientManager::the();
    }

    // Connect to LaunchServer immediately and let it know that we won't ask for anything other than opening
    // the user's downloads directory.
    // FIXME: This should go away with a standalone download manager at some point.
    if (!Desktop::Launcher::add_allowed_url(URL::create_with_file_protocol(Core::StandardPaths::downloads_directory()))
        || !Desktop::Launcher::seal_allowlist()) {
        warnln("Failed to set up allowed launch URLs");
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

    if (unveil("/tmp/portal/image", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto app_icon = GUI::Icon::default_icon("app-browser");

    auto m_config = Core::ConfigFile::get_for_app("Browser");
    Browser::g_home_url = m_config->read_entry("Preferences", "Home", "about:blank");
    Browser::g_search_engine = m_config->read_entry("Preferences", "SearchEngine", {});

    auto ad_filter_list_or_error = Core::File::open(String::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory()), Core::OpenMode::ReadOnly);
    if (!ad_filter_list_or_error.is_error()) {
        auto& ad_filter_list = *ad_filter_list_or_error.value();
        while (!ad_filter_list.eof()) {
            auto line = ad_filter_list.read_line();
            if (line.is_empty())
                continue;
            Web::ContentFilter::the().add_pattern(line);
        }
    }

    bool bookmarksbar_enabled = true;
    auto bookmarks_bar = Browser::BookmarksBarWidget::construct(Browser::bookmarks_file_path(), bookmarksbar_enabled);

    Browser::CookieJar cookie_jar;

    auto window = GUI::Window::construct();
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_title("Browser");

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.load_from_gml(browser_window_gml);

    auto& top_line = *widget.find_descendant_of_type_named<GUI::HorizontalSeparator>("top_line");

    auto& tab_widget = *widget.find_descendant_of_type_named<GUI::TabWidget>("tab_widget");

    tab_widget.on_tab_count_change = [&](size_t tab_count) {
        top_line.set_visible(tab_count > 1);
    };

    auto default_favicon = Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png");
    VERIFY(default_favicon);

    auto set_window_title_for_tab = [&window](auto& tab) {
        auto& title = tab.title();
        auto url = tab.url();
        window->set_title(String::formatted("{} - Browser", title.is_empty() ? url.to_string() : title));
    };

    tab_widget.on_change = [&](auto& active_widget) {
        auto& tab = static_cast<Browser::Tab&>(active_widget);
        set_window_title_for_tab(tab);
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
        auto type = Browser::s_single_process ? Browser::Tab::Type::InProcessWebView : Browser::Tab::Type::OutOfProcessWebView;
        auto& new_tab = tab_widget.add_tab<Browser::Tab>("New tab", type);

        tab_widget.set_bar_visible(!window->is_fullscreen() && tab_widget.children().size() > 1);
        tab_widget.set_tab_icon(new_tab, default_favicon);

        new_tab.on_title_change = [&](auto title) {
            tab_widget.set_tab_title(new_tab, title);
            if (tab_widget.active_widget() == &new_tab)
                set_window_title_for_tab(new_tab);
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

        new_tab.on_get_cookie = [&](auto& url, auto source) -> String {
            return cookie_jar.get_cookie(url, source);
        };

        new_tab.on_set_cookie = [&](auto& url, auto& cookie, auto source) {
            cookie_jar.set_cookie(url, cookie, source);
        };

        new_tab.on_dump_cookies = [&]() {
            cookie_jar.dump_cookies();
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

    app->on_action_enter = [&](GUI::Action& action) {
        auto* tab = static_cast<Browser::Tab*>(tab_widget.active_widget());
        if (!tab)
            return;
        tab->action_entered(action);
    };

    app->on_action_leave = [&](auto& action) {
        auto* tab = static_cast<Browser::Tab*>(tab_widget.active_widget());
        if (!tab)
            return;
        tab->action_left(action);
    };

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
        GUI::AboutDialog::show("Browser", app_icon.bitmap_for_size(32), window);
    };

    window_actions.on_show_bookmarks_bar = [&](auto& action) {
        Browser::BookmarksBarWidget::the().set_visible(action.is_checked());
    };
    window_actions.show_bookmarks_bar_action().set_checked(bookmarksbar_enabled);

    create_new_tab(first_url, true);
    window->show();

    return app->exec();
}
