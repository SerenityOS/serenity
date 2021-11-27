/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Browser.h"
#include "BrowserWindow.h"
#include "CookieJar.h"
#include "Tab.h"
#include "WindowActions.h"
#include <AK/StringBuilder.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/TabWidget.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

namespace Browser {

String g_search_engine;
String g_home_url;
Vector<String> g_content_filters;

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (getuid() == 0) {
        warnln("Refusing to run as root");
        return 1;
    }

    TRY(Core::System::pledge("stdio recvfd sendfd unix cpath rpath wpath"));

    const char* specified_url = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(specified_url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto app = GUI::Application::construct(arguments);

    Config::pledge_domains("Browser");

    // Connect to LaunchServer immediately and let it know that we won't ask for anything other than opening
    // the user's downloads directory.
    // FIXME: This should go away with a standalone download manager at some point.
    TRY(Desktop::Launcher::add_allowed_url(URL::create_with_file_protocol(Core::StandardPaths::downloads_directory())));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::unveil("/home", "rwc"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/tmp/portal/image", "rw"));
    TRY(Core::System::unveil("/tmp/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/portal/request", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = GUI::Icon::default_icon("app-browser");

    Browser::g_home_url = Config::read_string("Browser", "Preferences", "Home", "file:///res/html/misc/welcome.html");
    Browser::g_search_engine = Config::read_string("Browser", "Preferences", "SearchEngine", {});

    auto ad_filter_list_or_error = Core::File::open(String::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory()), Core::OpenMode::ReadOnly);
    if (!ad_filter_list_or_error.is_error()) {
        auto& ad_filter_list = *ad_filter_list_or_error.value();
        while (!ad_filter_list.eof()) {
            auto line = ad_filter_list.read_line();
            if (line.is_empty())
                continue;
            Browser::g_content_filters.append(move(line));
        }
    }

    URL first_url = Browser::g_home_url;
    if (specified_url) {
        if (Core::File::exists(specified_url)) {
            first_url = URL::create_with_file_protocol(Core::File::real_path_for(specified_url));
        } else {
            first_url = Browser::url_from_user_input(specified_url);
        }
    }

    Browser::CookieJar cookie_jar;
    auto window = Browser::BrowserWindow::construct(cookie_jar, first_url);

    app->on_action_enter = [&](GUI::Action& action) {
        if (auto* browser_window = dynamic_cast<Browser::BrowserWindow*>(app->active_window())) {
            auto* tab = static_cast<Browser::Tab*>(browser_window->tab_widget().active_widget());
            if (!tab)
                return;
            tab->action_entered(action);
        }
    };

    app->on_action_leave = [&](auto& action) {
        if (auto* browser_window = dynamic_cast<Browser::BrowserWindow*>(app->active_window())) {
            auto* tab = static_cast<Browser::Tab*>(browser_window->tab_widget().active_widget());
            if (!tab)
                return;
            tab->action_left(action);
        }
    };

    window->show();

    return app->exec();
}
