/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Browser.h"
#include "BrowserWindow.h"
#include "CookieJar.h"
#include "Tab.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/TabWidget.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <stdio.h>
#include <unistd.h>

namespace Browser {

String g_search_engine;
String g_home_url;

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
    args_parser.add_positional_argument(specified_url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("Browser");

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

    if (unveil("/tmp/portal/request", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto app_icon = GUI::Icon::default_icon("app-browser");

    Browser::g_home_url = Config::read_string("Browser", "Preferences", "Home", "about:blank");
    Browser::g_search_engine = Config::read_string("Browser", "Preferences", "SearchEngine", {});

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
