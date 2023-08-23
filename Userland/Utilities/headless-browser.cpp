/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/LexicalPath.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibDiff/Format.h>
#include <LibDiff/Generator.h>
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
#    include <Ladybird/Utilities.h>
#endif

class HeadlessWebContentView final : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<HeadlessWebContentView>> create(Core::AnonymousBuffer theme, Gfx::IntSize const& window_size, StringView web_driver_ipc_path, WebView::IsLayoutTestMode is_layout_test_mode = WebView::IsLayoutTestMode::No)
    {
        auto view = TRY(adopt_nonnull_own_or_enomem(new (nothrow) HeadlessWebContentView));

#if defined(AK_OS_SERENITY)
        view->m_client_state.client = TRY(WebView::WebContentClient::try_create(*view));
        (void)is_layout_test_mode;
#else
        auto candidate_web_content_paths = TRY(get_paths_for_helper_process("WebContent"sv));
        view->m_client_state.client = TRY(launch_web_content_process(*view, candidate_web_content_paths, WebView::EnableCallgrindProfiling::No, is_layout_test_mode, Ladybird::UseLagomNetworking::No));
#endif

        view->client().async_update_system_theme(move(theme));
        view->client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());

        view->m_viewport_rect = { { 0, 0 }, window_size };
        view->client().async_set_viewport_rect(view->m_viewport_rect);
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

    ErrorOr<String> dump_paint_tree()
    {
        return String::from_deprecated_string(client().dump_paint_tree());
    }

    ErrorOr<String> dump_text()
    {
        return String::from_deprecated_string(client().dump_text());
    }

    void clear_content_filters()
    {
        client().async_set_content_filters({});
    }

private:
    HeadlessWebContentView() = default;

    void notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const&) override { }
    void notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>) override { }
    void notify_server_did_finish_handling_input_event(bool) override { }
    void update_zoom() override { }
    void create_client(WebView::EnableCallgrindProfiling) override { }

    virtual Gfx::IntRect viewport_rect() const override { return m_viewport_rect; }
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override { return widget_position; }
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override { return content_position; }

private:
    Gfx::IntRect m_viewport_rect;
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
        return URL::create_with_file_scheme(TRY(FileSystem::real_path(url)).to_deprecated_string());

    URL formatted_url { url };
    if (!formatted_url.is_valid())
        formatted_url = TRY(String::formatted("http://{}", url));

    return formatted_url;
}

enum class TestMode {
    Layout,
    Text,
    Ref,
};

enum class TestResult {
    Pass,
    Fail,
    Timeout,
};

static ErrorOr<TestResult> run_dump_test(HeadlessWebContentView& view, StringView input_path, StringView expectation_path, TestMode mode, int timeout_in_milliseconds = 15000)
{
    Core::EventLoop loop;
    bool did_timeout = false;

    auto timeout_timer = TRY(Core::Timer::create_single_shot(5000, [&] {
        did_timeout = true;
        loop.quit(0);
    }));

    view.load(URL::create_with_file_scheme(TRY(FileSystem::real_path(input_path)).to_deprecated_string()));

    String result;

    if (mode == TestMode::Layout) {
        view.on_load_finish = [&](auto const&) {
            // NOTE: We take a screenshot here to force the lazy layout of SVG-as-image documents to happen.
            //       It also causes a lot more code to run, which is good for finding bugs. :^)
            (void)view.take_screenshot();

            StringBuilder builder;
            builder.append(view.dump_layout_tree().release_value_but_fixme_should_propagate_errors());
            builder.append("\n"sv);
            builder.append(view.dump_paint_tree().release_value_but_fixme_should_propagate_errors());
            result = builder.to_string().release_value_but_fixme_should_propagate_errors();
            loop.quit(0);
        };
    } else if (mode == TestMode::Text) {
        view.on_load_finish = [&](auto const&) {
            result = view.dump_text().release_value_but_fixme_should_propagate_errors();
            loop.quit(0);
        };
    }

    timeout_timer->start(timeout_in_milliseconds);
    loop.exec();

    if (did_timeout)
        return TestResult::Timeout;

    auto expectation_file_or_error = Core::File::open(expectation_path, Core::File::OpenMode::Read);
    if (expectation_file_or_error.is_error()) {
        warnln("Failed opening '{}': {}", expectation_path, expectation_file_or_error.error());
        return expectation_file_or_error.release_error();
    }

    auto expectation_file = expectation_file_or_error.release_value();

    auto expectation = TRY(String::from_utf8(StringView(TRY(expectation_file->read_until_eof()).bytes())));

    auto actual = result;
    auto actual_trimmed = TRY(actual.trim("\n"sv, TrimMode::Right));
    auto expectation_trimmed = TRY(expectation.trim("\n"sv, TrimMode::Right));

    if (actual_trimmed == expectation_trimmed)
        return TestResult::Pass;

    auto const color_output = isatty(STDOUT_FILENO) ? Diff::ColorOutput::Yes : Diff::ColorOutput::No;

    if (color_output == Diff::ColorOutput::Yes)
        outln("\n\033[33;1mTest failed\033[0m: {}", input_path);
    else
        outln("\nTest failed: {}", input_path);

    auto hunks = TRY(Diff::from_text(expectation, actual, 3));
    auto out = TRY(Core::File::standard_output());

    TRY(Diff::write_unified_header(expectation_path, expectation_path, *out));
    for (auto const& hunk : hunks)
        TRY(Diff::write_unified(hunk, *out, color_output));

    return TestResult::Fail;
}

static ErrorOr<TestResult> run_ref_test(HeadlessWebContentView& view, StringView input_path, StringView expectation_path, int timeout_in_milliseconds = 15000)
{
    Core::EventLoop loop;
    bool did_timeout = false;

    auto timeout_timer = TRY(Core::Timer::create_single_shot(5000, [&] {
        did_timeout = true;
        loop.quit(0);
    }));

    view.load(URL::create_with_file_scheme(TRY(FileSystem::real_path(input_path)).to_deprecated_string()));
    auto expectation_real_path = TRY(FileSystem::real_path(expectation_path)).to_deprecated_string();

    RefPtr<Gfx::Bitmap> actual_screenshot, expectation_screenshot;
    view.on_load_finish = [&](auto const&) {
        if (actual_screenshot) {
            expectation_screenshot = view.take_screenshot();
            loop.quit(0);
        } else {
            actual_screenshot = view.take_screenshot();
            view.load(URL::create_with_file_scheme(expectation_real_path));
        }
    };

    timeout_timer->start(timeout_in_milliseconds);
    loop.exec();

    if (did_timeout)
        return TestResult::Timeout;

    VERIFY(actual_screenshot);
    VERIFY(expectation_screenshot);

    if (actual_screenshot->visually_equals(*expectation_screenshot))
        return TestResult::Pass;

    return TestResult::Fail;
}

static ErrorOr<TestResult> run_test(HeadlessWebContentView& view, StringView input_path, StringView expectation_path, TestMode mode)
{
    switch (mode) {
    case TestMode::Text:
    case TestMode::Layout:
        return run_dump_test(view, input_path, expectation_path, mode);
    case TestMode::Ref:
        return run_ref_test(view, input_path, expectation_path);
    default:
        VERIFY_NOT_REACHED();
    }
}

struct Test {
    String input_path;
    String expectation_path;
    TestMode mode;
    Optional<TestResult> result;
};

static ErrorOr<void> collect_dump_tests(Vector<Test>& tests, StringView path, StringView trail, TestMode mode)
{
    Core::DirIterator it(TRY(String::formatted("{}/input/{}", path, trail)).to_deprecated_string(), Core::DirIterator::Flags::SkipDots);
    while (it.has_next()) {
        auto name = it.next_path();
        auto input_path = TRY(FileSystem::real_path(TRY(String::formatted("{}/input/{}/{}", path, trail, name))));
        if (FileSystem::is_directory(input_path)) {
            TRY(collect_dump_tests(tests, path, TRY(String::formatted("{}/{}", trail, name)), mode));
            continue;
        }
        if (!name.ends_with(".html"sv))
            continue;
        auto basename = LexicalPath::title(name);
        auto expectation_path = TRY(String::formatted("{}/expected/{}/{}.txt", path, trail, basename));

        tests.append({ move(input_path), move(expectation_path), mode, {} });
    }
    return {};
}

static ErrorOr<void> collect_ref_tests(Vector<Test>& tests, StringView path)
{
    auto manifest_path = TRY(String::formatted("{}/manifest.json", path));
    auto manifest_file_or_error = Core::File::open(manifest_path, Core::File::OpenMode::Read);
    if (manifest_file_or_error.is_error()) {
        warnln("Failed opening '{}': {}", manifest_path, manifest_file_or_error.error());
        return manifest_file_or_error.release_error();
    }

    auto manifest_file = manifest_file_or_error.release_value();
    auto manifest = TRY(String::from_utf8(StringView(TRY(manifest_file->read_until_eof()).bytes())));
    auto manifest_json = TRY(JsonParser(manifest).parse());
    TRY(manifest_json.as_object().try_for_each_member([&](DeprecatedString const& key, AK::JsonValue const& value) -> ErrorOr<void> {
        TRY(String::from_deprecated_string(key));
        auto input_path = TRY(String::formatted("{}/{}", path, key));
        auto expectation_path = TRY(String::formatted("{}/{}", path, value.to_deprecated_string()));
        tests.append({ input_path, expectation_path, TestMode::Ref, {} });
        return {};
    }));

    return {};
}

static ErrorOr<int> run_tests(HeadlessWebContentView& view, StringView test_root_path)
{
    view.clear_content_filters();

    Vector<Test> tests;
    TRY(collect_dump_tests(tests, TRY(String::formatted("{}/Layout", test_root_path)), "."sv, TestMode::Layout));
    TRY(collect_dump_tests(tests, TRY(String::formatted("{}/Text", test_root_path)), "."sv, TestMode::Text));
    TRY(collect_ref_tests(tests, TRY(String::formatted("{}/Ref", test_root_path))));

    size_t pass_count = 0;
    size_t fail_count = 0;
    size_t timeout_count = 0;

    bool is_tty = isatty(STDOUT_FILENO);

    outln("Running {} tests...", tests.size());
    for (size_t i = 0; i < tests.size(); ++i) {
        auto& test = tests[i];

        if (is_tty) {
            // Keep clearing and reusing the same line if stdout is a TTY.
            out("\33[2K\r");
        }

        out("{}/{}: {}", i + 1, tests.size(), LexicalPath::relative_path(test.input_path, test_root_path));

        if (is_tty)
            fflush(stdout);
        else
            outln("");

        test.result = TRY(run_test(view, test.input_path, test.expectation_path, test.mode));
        switch (*test.result) {
        case TestResult::Pass:
            ++pass_count;
            break;
        case TestResult::Fail:
            ++fail_count;
            break;
        case TestResult::Timeout:
            ++timeout_count;
            break;
        }
    }

    if (is_tty)
        outln("\33[2K\rDone!");

    outln("==================================================");
    outln("Pass: {}, Fail: {}, Timeout: {}", pass_count, fail_count, timeout_count);
    outln("==================================================");
    for (auto& test : tests) {
        if (*test.result == TestResult::Pass)
            continue;
        outln("{}: {}", *test.result == TestResult::Fail ? "Fail" : "Timeout", test.input_path);
    }

    if (timeout_count == 0 && fail_count == 0)
        return 0;
    return 1;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::EventLoop event_loop;

    int screenshot_timeout = 1;
    StringView url;
    auto resources_folder = "/res"sv;
    StringView web_driver_ipc_path;
    bool dump_layout_tree = false;
    bool dump_text = false;
    bool is_layout_test_mode = false;
    StringView test_root_path;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("This utility runs the Browser in headless mode.");
    args_parser.add_option(screenshot_timeout, "Take a screenshot after [n] seconds (default: 1)", "screenshot", 's', "n");
    args_parser.add_option(dump_layout_tree, "Dump layout tree and exit", "dump-layout-tree", 'd');
    args_parser.add_option(dump_text, "Dump text and exit", "dump-text", 'T');
    args_parser.add_option(test_root_path, "Run tests in path", "run-tests", 'R', "test-root-path");
    args_parser.add_option(resources_folder, "Path of the base resources folder (defaults to /res)", "resources", 'r', "resources-root-path");
    args_parser.add_option(web_driver_ipc_path, "Path to the WebDriver IPC socket", "webdriver-ipc-path", 0, "path");
    args_parser.add_option(is_layout_test_mode, "Enable layout test mode", "layout-test-mode", 0);
    args_parser.add_positional_argument(url, "URL to open", "url", Core::ArgsParser::Required::No);
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

    if (!test_root_path.is_empty()) {
        // --run-tests implies --layout-test-mode.
        is_layout_test_mode = true;
    }

    auto view = TRY(HeadlessWebContentView::create(move(theme), window_size, web_driver_ipc_path, is_layout_test_mode ? WebView::IsLayoutTestMode::Yes : WebView::IsLayoutTestMode::No));
    RefPtr<Core::Timer> timer;

    if (!test_root_path.is_empty()) {
        return run_tests(*view, test_root_path);
    }

    if (dump_layout_tree) {
        view->on_load_finish = [&](auto const&) {
            (void)view->take_screenshot();
            auto layout_tree = view->dump_layout_tree().release_value_but_fixme_should_propagate_errors();
            auto paint_tree = view->dump_paint_tree().release_value_but_fixme_should_propagate_errors();

            out("{}\n{}", layout_tree, paint_tree);
            fflush(stdout);

            event_loop.quit(0);
        };
    } else if (dump_text) {
        view->on_load_finish = [&](auto const&) {
            auto text = view->dump_text().release_value_but_fixme_should_propagate_errors();

            out("{}", text);
            fflush(stdout);

            event_loop.quit(0);
        };
    } else if (web_driver_ipc_path.is_empty()) {
        timer = TRY(load_page_for_screenshot_and_exit(event_loop, *view, screenshot_timeout));
    }

    view->load(TRY(format_url(url)));
    return event_loop.exec();
}
