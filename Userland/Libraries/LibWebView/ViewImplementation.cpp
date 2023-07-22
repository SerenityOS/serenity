/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibCore/StandardPaths.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {

ViewImplementation::ViewImplementation(UseJavaScriptBytecode use_javascript_bytecode)
    : m_use_javascript_bytecode(use_javascript_bytecode)
{
    m_backing_store_shrink_timer = Core::Timer::create_single_shot(3000, [this] {
        resize_backing_stores_if_needed(WindowResizeInProgress::No);
    }).release_value_but_fixme_should_propagate_errors();

    m_repeated_crash_timer = Core::Timer::create_single_shot(1000, [this] {
        // Reset the "crashing a lot" counter after 1 second in case we just
        // happen to be visiting crashy websites a lot.
        this->m_crash_count = 0;
    }).release_value_but_fixme_should_propagate_errors();
}

WebContentClient& ViewImplementation::client()
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

WebContentClient const& ViewImplementation::client() const
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

void ViewImplementation::load(AK::URL const& url)
{
    m_url = url;
    client().async_load_url(url);
}

void ViewImplementation::load_html(StringView html, AK::URL const& url)
{
    m_url = url;
    client().async_load_html(html, url);
}

void ViewImplementation::load_empty_document()
{
    load_html(""sv, {});
}

void ViewImplementation::zoom_in()
{
    if (m_zoom_level >= ZOOM_MAX_LEVEL)
        return;
    m_zoom_level += ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::zoom_out()
{
    if (m_zoom_level <= ZOOM_MIN_LEVEL)
        return;
    m_zoom_level -= ZOOM_STEP;
    update_zoom();
}

void ViewImplementation::reset_zoom()
{
    m_zoom_level = 1.0f;
    update_zoom();
}

void ViewImplementation::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    client().async_set_preferred_color_scheme(color_scheme);
}

DeprecatedString ViewImplementation::selected_text()
{
    return client().get_selected_text();
}

void ViewImplementation::select_all()
{
    client().async_select_all();
}

void ViewImplementation::get_source()
{
    client().async_get_source();
}

void ViewImplementation::inspect_dom_tree()
{
    client().async_inspect_dom_tree();
}

void ViewImplementation::inspect_accessibility_tree()
{
    client().async_inspect_accessibility_tree();
}

ErrorOr<ViewImplementation::DOMNodeProperties> ViewImplementation::inspect_dom_node(i32 node_id, Optional<Web::CSS::Selector::PseudoElement> pseudo_element)
{
    auto response = client().inspect_dom_node(node_id, pseudo_element);
    if (!response.has_style())
        return Error::from_string_view("Inspected node returned no style"sv);
    return DOMNodeProperties {
        .computed_style_json = TRY(String::from_deprecated_string(response.take_computed_style())),
        .resolved_style_json = TRY(String::from_deprecated_string(response.take_resolved_style())),
        .custom_properties_json = TRY(String::from_deprecated_string(response.take_custom_properties())),
        .node_box_sizing_json = TRY(String::from_deprecated_string(response.take_node_box_sizing())),
        .aria_properties_state_json = TRY(String::from_deprecated_string(response.take_aria_properties_state())),
    };
}

void ViewImplementation::clear_inspected_dom_node()
{
    client().inspect_dom_node(0, {});
}

i32 ViewImplementation::get_hovered_node_id()
{
    return client().get_hovered_node_id();
}

void ViewImplementation::debug_request(DeprecatedString const& request, DeprecatedString const& argument)
{
    client().async_debug_request(request, argument);
}

void ViewImplementation::run_javascript(StringView js_source)
{
    client().async_run_javascript(js_source);
}

void ViewImplementation::js_console_input(DeprecatedString const& js_source)
{
    client().async_js_console_input(js_source);
}

void ViewImplementation::js_console_request_messages(i32 start_index)
{
    client().async_js_console_request_messages(start_index);
}

void ViewImplementation::toggle_media_play_state()
{
    client().async_toggle_media_play_state();
}

void ViewImplementation::toggle_media_mute_state()
{
    client().async_toggle_media_mute_state();
}

void ViewImplementation::toggle_media_loop_state()
{
    client().async_toggle_media_loop_state();
}

void ViewImplementation::toggle_media_controls_state()
{
    client().async_toggle_media_controls_state();
}

void ViewImplementation::handle_resize()
{
    resize_backing_stores_if_needed(WindowResizeInProgress::Yes);
    m_backing_store_shrink_timer->restart();
}

#if !defined(AK_OS_SERENITY)

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> ViewImplementation::launch_web_content_process(ReadonlySpan<String> candidate_web_content_paths, EnableCallgrindProfiling enable_callgrind_profiling, IsLayoutTestMode is_layout_test_mode, UseJavaScriptBytecode use_javascript_bytecode)
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int wc_fd_passing_fd = fd_passing_socket_fds[1];

    if (auto child_pid = TRY(Core::System::fork()); child_pid == 0) {
        TRY(Core::System::close(ui_fd_passing_fd));
        TRY(Core::System::close(ui_fd));

        auto takeover_string = TRY(String::formatted("WebContent:{}", wc_fd));
        TRY(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto webcontent_fd_passing_socket_string = TRY(String::number(wc_fd_passing_fd));

        ErrorOr<void> result;
        for (auto const& path : candidate_web_content_paths) {
            constexpr auto callgrind_prefix_length = 3;

            if (Core::System::access(path, X_OK).is_error())
                continue;

            auto arguments = Vector {
                "valgrind"sv,
                "--tool=callgrind"sv,
                "--instr-atstart=no"sv,
                path.bytes_as_string_view(),
                "--webcontent-fd-passing-socket"sv,
                webcontent_fd_passing_socket_string
            };
            if (enable_callgrind_profiling == EnableCallgrindProfiling::No)
                arguments.remove(0, callgrind_prefix_length);
            if (is_layout_test_mode == IsLayoutTestMode::Yes)
                arguments.append("--layout-test-mode"sv);
            if (use_javascript_bytecode == UseJavaScriptBytecode::Yes)
                arguments.append("--use-bytecode"sv);

            result = Core::System::exec(arguments[0], arguments.span(), Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error())
            warnln("Could not launch any of {}: {}", candidate_web_content_paths, result.error());
        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(wc_fd_passing_fd));
    TRY(Core::System::close(wc_fd));

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebView::WebContentClient(move(socket), *this)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    if (enable_callgrind_profiling == EnableCallgrindProfiling::Yes) {
        dbgln();
        dbgln("\033[1;45mLaunched WebContent process under callgrind!\033[0m");
        dbgln("\033[100mRun `\033[4mcallgrind_control -i on\033[24m` to start instrumentation and `\033[4mcallgrind_control -i off\033[24m` stop it again.\033[0m");
        dbgln();
    }

    return new_client;
}

#endif

void ViewImplementation::resize_backing_stores_if_needed(WindowResizeInProgress window_resize_in_progress)
{
    if (m_client_state.has_usable_bitmap) {
        // NOTE: We keep the outgoing front bitmap as a backup so we have something to paint until we get a new one.
        m_backup_bitmap = m_client_state.front_bitmap.bitmap;
        m_backup_bitmap_size = m_client_state.front_bitmap.last_painted_size;
    }

    m_client_state.has_usable_bitmap = false;

    auto viewport_rect = this->viewport_rect();
    if (viewport_rect.is_empty())
        return;

    Gfx::IntSize minimum_needed_size;

    if (window_resize_in_progress == WindowResizeInProgress::Yes) {
        // Pad the minimum needed size so that we don't have to keep reallocating backing stores while the window is being resized.
        minimum_needed_size = { viewport_rect.width() + 256, viewport_rect.height() + 256 };
    } else {
        // If we're not in the middle of a resize, we can shrink the backing store size to match the viewport size.
        minimum_needed_size = viewport_rect.size();
        m_client_state.front_bitmap = {};
        m_client_state.back_bitmap = {};
    }

    auto reallocate_backing_store_if_needed = [&](SharedBitmap& backing_store) {
        if (!backing_store.bitmap || !backing_store.bitmap->size().contains(minimum_needed_size)) {
            if (auto new_bitmap_or_error = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::BGRx8888, minimum_needed_size); !new_bitmap_or_error.is_error()) {
                if (backing_store.bitmap)
                    client().async_remove_backing_store(backing_store.id);

                backing_store.pending_paints = 0;
                backing_store.bitmap = new_bitmap_or_error.release_value();
                backing_store.id = m_client_state.next_bitmap_id++;
                client().async_add_backing_store(backing_store.id, backing_store.bitmap->to_shareable_bitmap());
            }
            backing_store.last_painted_size = viewport_rect.size();
        }
    };

    reallocate_backing_store_if_needed(m_client_state.front_bitmap);
    reallocate_backing_store_if_needed(m_client_state.back_bitmap);

    request_repaint();
}

void ViewImplementation::request_repaint()
{
    // If this widget was instantiated but not yet added to a window,
    // it won't have a back bitmap yet, so we can just skip repaint requests.
    if (!m_client_state.back_bitmap.bitmap)
        return;
    // Don't request a repaint until pending paint requests have finished.
    if (m_client_state.back_bitmap.pending_paints) {
        m_client_state.got_repaint_requests_while_painting = true;
        return;
    }
    m_client_state.back_bitmap.pending_paints++;
    client().async_paint(viewport_rect(), m_client_state.back_bitmap.id);
}

void ViewImplementation::handle_web_content_process_crash()
{
    dbgln("WebContent process crashed!");

    ++m_crash_count;
    constexpr size_t max_reasonable_crash_count = 5U;
    if (m_crash_count >= max_reasonable_crash_count) {
        dbgln("WebContent has crashed {} times in quick succession! Not restarting...", m_crash_count);
        m_repeated_crash_timer->stop();
        return;
    }
    m_repeated_crash_timer->restart();

    create_client();
    VERIFY(m_client_state.client);

    // Don't keep a stale backup bitmap around.
    m_backup_bitmap = nullptr;

    handle_resize();
    StringBuilder builder;
    builder.append("<html><head><title>Crashed: "sv);
    builder.append(escape_html_entities(m_url.to_deprecated_string()));
    builder.append("</title></head><body>"sv);
    builder.append("<h1>Web page crashed"sv);
    if (!m_url.host().is_empty()) {
        builder.appendff(" on {}", escape_html_entities(m_url.host()));
    }
    builder.append("</h1>"sv);
    auto escaped_url = escape_html_entities(m_url.to_deprecated_string());
    builder.appendff("The web page <a href=\"{}\">{}</a> has crashed.<br><br>You can reload the page to try again.", escaped_url, escaped_url);
    builder.append("</body></html>"sv);
    load_html(builder.to_deprecated_string(), m_url);
}

ErrorOr<void> ViewImplementation::take_screenshot(ScreenshotType type)
{
    Gfx::ShareableBitmap bitmap;

    switch (type) {
    case ScreenshotType::Visible:
        if (auto* visible_bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.bitmap.ptr() : m_backup_bitmap.ptr())
            bitmap = visible_bitmap->to_shareable_bitmap();
        break;
    case ScreenshotType::Full:
        bitmap = client().take_document_screenshot();
        break;
    }

    if (!bitmap.is_valid())
        return Error::from_string_view("Failed to take a screenshot of the current tab"sv);

    LexicalPath path { Core::StandardPaths::downloads_directory() };
    path = path.append(TRY(Core::DateTime::now().to_string("screenshot-%Y-%m-%d-%H-%M-%S.png"sv)));

    auto encoded = TRY(Gfx::PNGWriter::encode(*bitmap.bitmap()));

    auto screenshot_file = TRY(Core::File::open(path.string(), Core::File::OpenMode::Write));
    TRY(screenshot_file->write_until_depleted(encoded));

    return {};
}

}
