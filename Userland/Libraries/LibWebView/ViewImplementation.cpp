/*
 * Copyright (c) 2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>
#include <LibWebView/ViewImplementation.h>

namespace WebView {

ViewImplementation::ViewImplementation()
{
    m_backing_store_shrink_timer = Core::Timer::create_single_shot(3000, [this] {
        resize_backing_stores_if_needed(WindowResizeInProgress::No);
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

void ViewImplementation::toggle_video_play_state()
{
    client().async_toggle_video_play_state();
}

void ViewImplementation::toggle_video_loop_state()
{
    client().async_toggle_video_loop_state();
}

void ViewImplementation::toggle_video_controls_state()
{
    client().async_toggle_video_controls_state();
}

void ViewImplementation::handle_resize()
{
    resize_backing_stores_if_needed(WindowResizeInProgress::Yes);
    m_backing_store_shrink_timer->restart();
}

#if !defined(AK_OS_SERENITY)

ErrorOr<NonnullRefPtr<WebView::WebContentClient>> ViewImplementation::launch_web_content_process(ReadonlySpan<String> candidate_web_content_paths, EnableCallgrindProfiling enable_callgrind_profiling, IsLayoutTestMode is_layout_test_mode)
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

}
