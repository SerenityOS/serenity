/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrowserWindow.h"
#include "EventLoopImplementationQt.h"
#include "Settings.h"
#include "WebContentView.h"
#include <AK/OwnPtr.h>
#include <Ladybird/HelperProcess.h>
#include <Ladybird/Utilities.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibMain/Main.h>
#include <LibWebView/CookieJar.h>
#include <LibWebView/Database.h>
#include <LibWebView/URL.h>
#include <QApplication>
#include <QFileOpenEvent>

namespace Ladybird {

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

class LadybirdApplication : public QApplication {
public:
    LadybirdApplication(int& argc, char** argv)
        : QApplication(argc, argv)
    {
    }

    Function<void(URL)> on_open_file;

    bool event(QEvent* event) override
    {
        switch (event->type()) {
        case QEvent::FileOpen: {
            if (!on_open_file)
                break;

            auto const& open_event = *static_cast<QFileOpenEvent const*>(event);
            auto file = ak_string_from_qstring(open_event.file());

            if (auto file_url = WebView::sanitize_url(file); file_url.has_value())
                on_open_file(file_url.release_value());
        }

        default:
            break;
        }

        return QApplication::event(event);
    }
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    LadybirdApplication app(arguments.argc, arguments.argv);

    Core::EventLoopManager::install(*new Ladybird::EventLoopManagerQt);
    Core::EventLoop event_loop;
    static_cast<Ladybird::EventLoopImplementationQt&>(event_loop.impl()).set_main_loop();

    TRY(handle_attached_debugger());

    platform_init();

    // NOTE: We only instantiate this to ensure that Gfx::FontDatabase has its default queries initialized.
    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    Vector<StringView> raw_urls;
    StringView webdriver_content_ipc_path;
    bool enable_callgrind_profiling = false;
    bool enable_sql_database = false;
    bool use_lagom_networking = false;
    bool use_gpu_painting = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("The Ladybird web browser :^)");
    args_parser.add_positional_argument(raw_urls, "URLs to open", "url", Core::ArgsParser::Required::No);
    args_parser.add_option(webdriver_content_ipc_path, "Path to WebDriver IPC for WebContent", "webdriver-content-path", 0, "path", Core::ArgsParser::OptionHideMode::CommandLineAndMarkdown);
    args_parser.add_option(enable_callgrind_profiling, "Enable Callgrind profiling", "enable-callgrind-profiling", 'P');
    args_parser.add_option(enable_sql_database, "Enable SQL database", "enable-sql-database", 0);
    args_parser.add_option(use_lagom_networking, "Enable Lagom servers for networking", "enable-lagom-networking", 0);
    args_parser.add_option(use_gpu_painting, "Enable GPU painting", "enable-gpu-painting", 0);
    args_parser.parse(arguments);

    RefPtr<WebView::Database> database;

    if (enable_sql_database) {
        auto sql_server_paths = TRY(get_paths_for_helper_process("SQLServer"sv));
        database = TRY(WebView::Database::create(move(sql_server_paths)));
    }

    auto cookie_jar = database ? TRY(WebView::CookieJar::create(*database)) : WebView::CookieJar::create();

    Vector<URL> initial_urls;

    for (auto const& raw_url : raw_urls) {
        if (auto url = WebView::sanitize_url(raw_url); url.has_value())
            initial_urls.append(url.release_value());
    }

    if (initial_urls.is_empty()) {
        auto new_tab_page = Ladybird::Settings::the()->new_tab_page();
        initial_urls.append(ak_string_from_qstring(new_tab_page));
    }

    Ladybird::WebContentOptions web_content_options {
        .enable_callgrind_profiling = enable_callgrind_profiling ? Ladybird::EnableCallgrindProfiling::Yes : Ladybird::EnableCallgrindProfiling::No,
        .enable_gpu_painting = use_gpu_painting ? Ladybird::EnableGPUPainting::Yes : Ladybird::EnableGPUPainting::No,
        .use_lagom_networking = use_lagom_networking ? Ladybird::UseLagomNetworking::Yes : Ladybird::UseLagomNetworking::No,
    };

    Ladybird::BrowserWindow window(initial_urls, cookie_jar, web_content_options, webdriver_content_ipc_path);
    window.setWindowTitle("Ladybird");

    app.on_open_file = [&](auto file_url) {
        window.view().load(file_url);
    };

    if (Ladybird::Settings::the()->is_maximized()) {
        window.showMaximized();
    } else {
        auto last_position = Ladybird::Settings::the()->last_position();
        if (last_position.has_value())
            window.move(last_position.value());
        window.resize(Ladybird::Settings::the()->last_size());
    }

    window.show();

    return event_loop.exec();
}
