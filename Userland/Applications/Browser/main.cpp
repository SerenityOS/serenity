/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/Browser.h>
#include <Applications/Browser/BrowserWindow.h>
#include <Applications/Browser/CookieJar.h>
#include <Applications/Browser/Database.h>
#include <Applications/Browser/Tab.h>
#include <Applications/Browser/WindowActions.h>
#include <Applications/BrowserSettings/Defaults.h>
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/FileWatcher.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/TabWidget.h>
#include <LibMain/Main.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWebView/OutOfProcessWebView.h>
#include <LibWebView/RequestServerAdapter.h>
#include <unistd.h>

namespace Browser {

DeprecatedString g_search_engine;
DeprecatedString g_home_url;
DeprecatedString g_new_tab_url;
Vector<String> g_content_filters;
bool g_content_filters_enabled { true };
Vector<String> g_autoplay_allowlist;
bool g_autoplay_allowed_on_all_websites { false };
Vector<DeprecatedString> g_proxies;
HashMap<DeprecatedString, size_t> g_proxy_mappings;
IconBag g_icon_bag;
DeprecatedString g_webdriver_content_ipc_path;

}

static ErrorOr<void> load_content_filters()
{
    auto file = TRY(Core::File::open(TRY(String::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory())), Core::File::OpenMode::Read));
    auto ad_filter_list = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    Browser::g_content_filters.clear_with_capacity();

    while (TRY(ad_filter_list->can_read_line())) {
        auto line = TRY(ad_filter_list->read_line(buffer));
        if (line.is_empty())
            continue;

        auto pattern = TRY(String::from_utf8(line));
        TRY(Browser::g_content_filters.try_append(move(pattern)));
    }

    return {};
}

static ErrorOr<void> load_autoplay_allowlist()
{
    auto file = TRY(Core::File::open(TRY(String::formatted("{}/BrowserAutoplayAllowlist.txt", Core::StandardPaths::config_directory())), Core::File::OpenMode::Read));
    auto allowlist = TRY(Core::InputBufferedFile::create(move(file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));

    Browser::g_autoplay_allowlist.clear_with_capacity();

    while (TRY(allowlist->can_read_line())) {
        auto line = TRY(allowlist->read_line(buffer));
        if (line.is_empty())
            continue;

        auto domain = TRY(String::from_utf8(line));
        TRY(Browser::g_autoplay_allowlist.try_append(move(domain)));
    }

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (getuid() == 0) {
        warnln("Refusing to run as root");
        return 1;
    }

    TRY(Core::System::pledge("stdio recvfd sendfd unix fattr cpath rpath wpath proc exec"));

    Vector<DeprecatedString> specified_urls;
    bool use_ast_interpreter = false;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(specified_urls, "URLs to open", "url", Core::ArgsParser::Required::No);
    args_parser.add_option(Browser::g_webdriver_content_ipc_path, "Path to WebDriver IPC for WebContent", "webdriver-content-path", 0, "path");
    args_parser.add_option(use_ast_interpreter, "Enable JavaScript AST interpreter (deprecated)", "ast", 0);

    args_parser.parse(arguments);

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Browser");
    Config::monitor_domain("Browser");

    // Connect to LaunchServer immediately and let it know that we won't ask for anything other than opening
    // the user's downloads directory.
    // FIXME: This should go away with a standalone download manager at some point.
    TRY(Desktop::Launcher::add_allowed_url(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory())));
    TRY(Desktop::Launcher::seal_allowlist());

    if (!Browser::g_webdriver_content_ipc_path.is_empty())
        specified_urls.empend("about:blank");

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/image", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/webcontent", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/request", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/sql", "rw"));
    TRY(Core::System::unveil("/home", "rwc"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/bin/BrowserSettings", "x"));
    TRY(Core::System::unveil("/bin/Browser", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Web::ResourceLoader::initialize(TRY(WebView::RequestServerAdapter::try_create()));

    auto app_icon = GUI::Icon::default_icon("app-browser"sv);

    Browser::g_home_url = Config::read_string("Browser"sv, "Preferences"sv, "Home"sv, Browser::default_homepage_url);
    Browser::g_new_tab_url = Config::read_string("Browser"sv, "Preferences"sv, "NewTab"sv, Browser::default_new_tab_url);
    Browser::g_search_engine = Config::read_string("Browser"sv, "Preferences"sv, "SearchEngine"sv, Browser::default_search_engine);
    Browser::g_content_filters_enabled = Config::read_bool("Browser"sv, "Preferences"sv, "EnableContentFilters"sv, Browser::default_enable_content_filters);
    Browser::g_autoplay_allowed_on_all_websites = Config::read_bool("Browser"sv, "Preferences"sv, "AllowAutoplayOnAllWebsites"sv, Browser::default_allow_autoplay_on_all_websites);

    Browser::g_icon_bag = TRY(Browser::IconBag::try_create());

    auto database = TRY(Browser::Database::create());
    TRY(load_content_filters());
    TRY(load_autoplay_allowlist());

    for (auto& group : Config::list_groups("Browser"sv)) {
        if (!group.starts_with("Proxy:"sv))
            continue;

        for (auto& key : Config::list_keys("Browser"sv, group)) {
            auto proxy_spec = group.substring_view(6);
            auto existing_proxy = Browser::g_proxies.find(proxy_spec);
            if (existing_proxy.is_end())
                Browser::g_proxies.append(proxy_spec);

            Browser::g_proxy_mappings.set(key, existing_proxy.index());
        }
    }

    auto url_from_argument_string = [](DeprecatedString const& string) -> ErrorOr<URL> {
        if (FileSystem::exists(string)) {
            return URL::create_with_file_scheme(TRY(FileSystem::real_path(string)).to_deprecated_string());
        }
        return Browser::url_from_user_input(string);
    };

    URL first_url = Browser::url_from_user_input(Browser::g_home_url);
    if (!specified_urls.is_empty())
        first_url = TRY(url_from_argument_string(specified_urls.first()));

    auto cookie_jar = TRY(Browser::CookieJar::create(*database));
    auto window = Browser::BrowserWindow::construct(cookie_jar, first_url, use_ast_interpreter ? WebView::UseJavaScriptBytecode::No : WebView::UseJavaScriptBytecode::Yes);

    auto content_filters_watcher = TRY(Core::FileWatcher::create());
    content_filters_watcher->on_change = [&](Core::FileWatcherEvent const&) {
        dbgln("Reloading content filters because config file changed");
        auto error = load_content_filters();
        if (error.is_error()) {
            dbgln("Reloading content filters failed: {}", error.release_error());
            return;
        }
        window->content_filters_changed();
    };
    TRY(content_filters_watcher->add_watch(DeprecatedString::formatted("{}/BrowserContentFilters.txt", Core::StandardPaths::config_directory()), Core::FileWatcherEvent::Type::ContentModified));

    auto autoplay_allowlist_watcher = TRY(Core::FileWatcher::create());
    autoplay_allowlist_watcher->on_change = [&](Core::FileWatcherEvent const&) {
        dbgln("Reloading autoplay allowlist because config file changed");
        if (auto error = load_autoplay_allowlist(); error.is_error()) {
            dbgln("Reloading autoplay allowlist failed: {}", error.release_error());
            return;
        }
        window->autoplay_allowlist_changed();
    };
    TRY(autoplay_allowlist_watcher->add_watch(DeprecatedString::formatted("{}/BrowserAutoplayAllowlist.txt", Core::StandardPaths::config_directory()), Core::FileWatcherEvent::Type::ContentModified));

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

    for (size_t i = 1; i < specified_urls.size(); ++i)
        window->create_new_tab(TRY(url_from_argument_string(specified_urls[i])), Web::HTML::ActivateTab::No);

    window->show();

    window->broadcast_window_position(window->position());
    window->broadcast_window_size(window->size());

    return app->exec();
}
