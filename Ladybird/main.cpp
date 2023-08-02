/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "EventLoopImplementationQt.h"
#include "HelperProcess.h"
#include "Settings.h"
#include "Utilities.h"
#include "WebContentView.h"
#include <AK/OwnPtr.h>
#include <Browser/CookieJar.h>
#include <Browser/Database.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibSQL/SQLClient.h>
#include <QApplication>

namespace Ladybird {

OwnPtr<Ladybird::Settings> s_settings;

bool is_using_dark_system_theme(QWidget& widget)
{
    // FIXME: Qt does not provide any method to query if the system is using a dark theme. We will have to implement
    //        platform-specific methods if we wish to have better detection. For now, this inspects if Qt is using a
    //        dark color for widget backgrounds using Rec. 709 luma coefficients.
    //        https://en.wikipedia.org/wiki/Rec._709#Luma_coefficients

    auto color = widget.palette().color(widget.backgroundRole());
    auto luma = 0.2126f * color.redF() + 0.7152f * color.greenF() + 0.0722f * color.blueF();

    return luma <= 0.5f;
}

}

static ErrorOr<void> handle_attached_debugger()
{
#ifdef AK_OS_LINUX
    // Let's ignore SIGINT if we're being debugged because GDB
    // incorrectly forwards the signal to us even when it's set to
    // "nopass". See https://sourceware.org/bugzilla/show_bug.cgi?id=9425
    // for details.
    if (TRY(Core::Process::is_being_debugged())) {
        dbgln("Debugger is attached, ignoring SIGINT");
        TRY(Core::System::signal(SIGINT, SIG_IGN));
    }
#endif
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    QApplication app(arguments.argc, arguments.argv);

    Core::EventLoopManager::install(*new Ladybird::EventLoopManagerQt);
    Core::EventLoop event_loop;
    static_cast<Ladybird::EventLoopImplementationQt&>(event_loop.impl()).set_main_loop();

    TRY(handle_attached_debugger());

    platform_init();

    // NOTE: We only instantiate this to ensure that Gfx::FontDatabase has its default queries initialized.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    StringView raw_url;
    StringView webdriver_content_ipc_path;
    bool enable_callgrind_profiling = false;
    bool enable_sql_database = false;
    bool use_ast_interpreter = false;
    bool use_lagom_networking = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser :^)");
    args_parser.add_positional_argument(raw_url, "URL to open", "url", Core::ArgsParser::Required::No);
    args_parser.add_option(webdriver_content_ipc_path, "Path to WebDriver IPC for WebContent", "webdriver-content-path", 0, "path");
    args_parser.add_option(enable_callgrind_profiling, "Enable Callgrind profiling", "enable-callgrind-profiling", 'P');
    args_parser.add_option(enable_sql_database, "Enable SQL database", "enable-sql-database", 0);
    args_parser.add_option(use_ast_interpreter, "Enable JavaScript AST interpreter (deprecated)", "ast", 0);
    args_parser.add_option(use_lagom_networking, "Enable Lagom servers for networking", "enable-lagom-networking", 0);
    args_parser.parse(arguments);

    auto get_formatted_url = [&](StringView const& raw_url) -> ErrorOr<URL> {
        URL url = raw_url;
        if (FileSystem::exists(raw_url))
            url = URL::create_with_file_scheme(TRY(FileSystem::real_path(raw_url)).to_deprecated_string());
        else if (!url.is_valid())
            url = DeprecatedString::formatted("https://{}", raw_url);
        return url;
    };

    RefPtr<Browser::Database> database;

    if (enable_sql_database) {
        auto sql_server_paths = TRY(get_paths_for_helper_process("SQLServer"sv));
        auto sql_client = TRY(SQL::SQLClient::launch_server_and_create_client(move(sql_server_paths)));
        database = TRY(Browser::Database::create(move(sql_client)));
    }

    auto cookie_jar = database ? TRY(Browser::CookieJar::create(*database)) : Browser::CookieJar::create();

    Ladybird::s_settings = adopt_own_if_nonnull(new Ladybird::Settings());
    Ladybird::BrowserWindow window(cookie_jar, webdriver_content_ipc_path, enable_callgrind_profiling ? WebView::EnableCallgrindProfiling::Yes : WebView::EnableCallgrindProfiling::No, use_ast_interpreter ? WebView::UseJavaScriptBytecode::No : WebView::UseJavaScriptBytecode::Yes, use_lagom_networking ? Ladybird::UseLagomNetworking::Yes : Ladybird::UseLagomNetworking::No);
    window.setWindowTitle("Ladybird");
    window.resize(800, 600);
    window.show();

    if (auto url = TRY(get_formatted_url(raw_url)); url.is_valid()) {
        window.view().load(url);
    } else {
        window.view().load("about:blank"sv);
    }

    return event_loop.exec();
}
