/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DeprecatedFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibGfx/Size.h>
#include <LibGfx/StandardCursor.h>
#include <LibGfx/SystemTheme.h>
#include <LibIPC/File.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWebView/ViewImplementation.h>
#include <LibWebView/WebContentClient.h>

#if !defined(AK_OS_SERENITY)
#    include <Ladybird/HelperProcess.h>
#    include <QCoreApplication>
#endif

class HeadlessWebContentView final : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<HeadlessWebContentView>> create(Core::AnonymousBuffer theme, Gfx::IntSize const& window_size, StringView web_driver_ipc_path)
    {
        auto view = TRY(adopt_nonnull_own_or_enomem(new (nothrow) HeadlessWebContentView()));

#if defined(AK_OS_SERENITY)
        view->m_client_state.client = TRY(WebView::WebContentClient::try_create(*view));
#else
        auto candidate_web_content_paths = TRY(get_paths_for_helper_process("WebContent"sv));
        view->m_client_state.client = TRY(view->launch_web_content_process(candidate_web_content_paths));
#endif

        view->client().async_update_system_theme(move(theme));
        view->client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());

        view->client().async_set_viewport_rect({ { 0, 0 }, window_size });
        view->client().async_set_window_size(window_size);

        if (!web_driver_ipc_path.is_empty())
            view->client().async_connect_to_webdriver(web_driver_ipc_path);

        return view;
    }

    RefPtr<Gfx::Bitmap> take_screenshot()
    {
        return client().take_document_screenshot().bitmap();
    }

    ErrorOr<String> dump_layout_tree()
    {
        return String::from_deprecated_string(client().dump_layout_tree());
    }

    Function<void(const URL&)> on_load_finish;

private:
    HeadlessWebContentView() = default;

    void notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize) override { }
    void notify_server_did_paint(Badge<WebView::WebContentClient>, i32) override { }
    void notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, Gfx::IntRect const&) override { }
    void notify_server_did_change_selection(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor) override { }
    void notify_server_did_change_title(Badge<WebView::WebContentClient>, DeprecatedString const&) override { }
    void notify_server_did_request_scroll(Badge<WebView::WebContentClient>, i32, i32) override { }
    void notify_server_did_request_scroll_to(Badge<WebView::WebContentClient>, Gfx::IntPoint) override { }
    void notify_server_did_request_scroll_into_view(Badge<WebView::WebContentClient>, Gfx::IntRect const&) override { }
    void notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const&) override { }
    void notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_hover_link(Badge<WebView::WebContentClient>, const URL&) override { }
    void notify_server_did_unhover_link(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_click_link(Badge<WebView::WebContentClient>, const URL&, DeprecatedString const&, unsigned) override { }
    void notify_server_did_middle_click_link(Badge<WebView::WebContentClient>, const URL&, DeprecatedString const&, unsigned) override { }
    void notify_server_did_start_loading(Badge<WebView::WebContentClient>, const URL&, bool) override { }

    void notify_server_did_finish_loading(Badge<WebView::WebContentClient>, const URL& url) override
    {
        if (on_load_finish)
            on_load_finish(url);
    }

    void notify_server_did_request_navigate_back(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_request_navigate_forward(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_request_refresh(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_request_context_menu(Badge<WebView::WebContentClient>, Gfx::IntPoint) override { }
    void notify_server_did_request_link_context_menu(Badge<WebView::WebContentClient>, Gfx::IntPoint, const URL&, DeprecatedString const&, unsigned) override { }
    void notify_server_did_request_image_context_menu(Badge<WebView::WebContentClient>, Gfx::IntPoint, const URL&, DeprecatedString const&, unsigned, Gfx::ShareableBitmap const&) override { }
    void notify_server_did_request_alert(Badge<WebView::WebContentClient>, String const&) override { }
    void notify_server_did_request_confirm(Badge<WebView::WebContentClient>, String const&) override { }
    void notify_server_did_request_prompt(Badge<WebView::WebContentClient>, String const&, String const&) override { }
    void notify_server_did_request_set_prompt_text(Badge<WebView::WebContentClient>, String const&) override { }
    void notify_server_did_request_accept_dialog(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_request_dismiss_dialog(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_get_source(const URL&, DeprecatedString const&) override { }
    void notify_server_did_get_dom_tree(DeprecatedString const&) override { }
    void notify_server_did_get_dom_node_properties(i32, DeprecatedString const&, DeprecatedString const&, DeprecatedString const&, DeprecatedString const&) override { }
    void notify_server_did_get_accessibility_tree(DeprecatedString const&) override { }
    void notify_server_did_output_js_console_message(i32) override { }
    void notify_server_did_get_js_console_messages(i32, Vector<DeprecatedString> const&, Vector<DeprecatedString> const&) override { }
    void notify_server_did_change_favicon(Gfx::Bitmap const&) override { }
    Vector<Web::Cookie::Cookie> notify_server_did_request_all_cookies(Badge<WebView::WebContentClient>, URL const&) override { return {}; }
    Optional<Web::Cookie::Cookie> notify_server_did_request_named_cookie(Badge<WebView::WebContentClient>, URL const&, DeprecatedString const&) override { return {}; }
    DeprecatedString notify_server_did_request_cookie(Badge<WebView::WebContentClient>, const URL&, Web::Cookie::Source) override { return {}; }
    void notify_server_did_set_cookie(Badge<WebView::WebContentClient>, const URL&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override { }
    void notify_server_did_update_cookie(Badge<WebView::WebContentClient>, Web::Cookie::Cookie const&) override { }
    String notify_server_did_request_new_tab(Badge<WebView::WebContentClient>, Web::HTML::ActivateTab) override { return {}; }
    void notify_server_did_request_activate_tab(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_close_browsing_context(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_update_resource_count(i32) override { }
    void notify_server_did_request_restore_window() override { }
    Gfx::IntPoint notify_server_did_request_reposition_window(Gfx::IntPoint) override { return {}; }
    Gfx::IntSize notify_server_did_request_resize_window(Gfx::IntSize) override { return {}; }
    Gfx::IntRect notify_server_did_request_maximize_window() override { return {}; }
    Gfx::IntRect notify_server_did_request_minimize_window() override { return {}; }
    Gfx::IntRect notify_server_did_request_fullscreen_window() override { return {}; }

    void notify_server_did_request_file(Badge<WebView::WebContentClient>, DeprecatedString const& path, i32 request_id) override
    {
        auto file = Core::File::open(path, Core::File::OpenMode::Read);

        if (file.is_error())
            client().async_handle_file_return(file.error().code(), {}, request_id);
        else
            client().async_handle_file_return(0, IPC::File(*file.value()), request_id);
    }

    void notify_server_did_finish_handling_input_event(bool) override { }
    void update_zoom() override { }
    void create_client(WebView::EnableCallgrindProfiling) override { }
};

static ErrorOr<NonnullRefPtr<Core::Timer>> load_page_for_screenshot_and_exit(Core::EventLoop& event_loop, HeadlessWebContentView& view, int screenshot_timeout)
{
    // FIXME: Allow passing the output path as an argument.
    static constexpr auto output_file_path = "output.png"sv;

    if (FileSystem::exists(output_file_path))
        TRY(FileSystem::remove(output_file_path, FileSystem::RecursionMode::Disallowed));

    outln("Taking screenshot after {} seconds", screenshot_timeout);

    auto timer = TRY(Core::Timer::create_single_shot(
        screenshot_timeout * 1000,
        [&]() {
            if (auto screenshot = view.take_screenshot()) {
                outln("Saving screenshot to {}", output_file_path);

                auto output_file = MUST(Core::File::open(output_file_path, Core::File::OpenMode::Write));
                auto image_buffer = MUST(Gfx::PNGWriter::encode(*screenshot));
                MUST(output_file->write_until_depleted(image_buffer.bytes()));
            } else {
                warnln("No screenshot available");
            }

            event_loop.quit(0);
        }));

    timer->start();
    return timer;
}

static ErrorOr<URL> format_url(StringView url)
{
    if (FileSystem::exists(url))
        return URL::create_with_file_scheme(Core::DeprecatedFile::real_path_for(url));

    URL formatted_url { url };
    if (!formatted_url.is_valid())
        formatted_url = TRY(String::formatted("http://{}", url));

    return formatted_url;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
#if !defined(AK_OS_SERENITY)
    QCoreApplication app(arguments.argc, arguments.argv);
#endif
    Core::EventLoop event_loop;

    int screenshot_timeout = 1;
    StringView url;
    auto resources_folder = "/res"sv;
    StringView web_driver_ipc_path;
    bool dump_layout_tree = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This utility runs the Browser in headless mode.");
    args_parser.add_option(screenshot_timeout, "Take a screenshot after [n] seconds (default: 1)", "screenshot", 's', "n");
    args_parser.add_option(dump_layout_tree, "Dump layout tree and exit", "dump-layout-tree", 'd');
    args_parser.add_option(resources_folder, "Path of the base resources folder (defaults to /res)", "resources", 'r', "resources-root-path");
    args_parser.add_option(web_driver_ipc_path, "Path to the WebDriver IPC socket", "webdriver-ipc-path", 0, "path");
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    Gfx::FontDatabase::set_default_font_query("Katica 10 400 0");
    Gfx::FontDatabase::set_window_title_font_query("Katica 10 700 0");
    Gfx::FontDatabase::set_fixed_width_font_query("Csilla 10 400 0");

    auto fonts_path = LexicalPath::join(resources_folder, "fonts"sv);
    Gfx::FontDatabase::set_default_fonts_lookup_path(fonts_path.string());

    auto theme_path = LexicalPath::join(resources_folder, "themes"sv, "Default.ini"sv);
    auto theme = TRY(Gfx::load_system_theme(theme_path.string()));

    // FIXME: Allow passing the window size as an argument.
    static constexpr Gfx::IntSize window_size { 800, 600 };

    auto view = TRY(HeadlessWebContentView::create(move(theme), window_size, web_driver_ipc_path));
    RefPtr<Core::Timer> timer;

    if (dump_layout_tree) {
        view->on_load_finish = [&](auto const&) {
            auto layout_tree = view->dump_layout_tree().release_value_but_fixme_should_propagate_errors();

            out("{}", layout_tree);
            fflush(stdout);

            event_loop.quit(0);
        };
    } else if (web_driver_ipc_path.is_empty()) {
        timer = TRY(load_page_for_screenshot_and_exit(event_loop, *view, screenshot_timeout));
    }

    view->load(TRY(format_url(url)));
    return event_loop.exec();
}
