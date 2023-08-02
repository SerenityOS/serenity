#include "ViewImpl.h"
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/Crypto/Crypto.h>

LadybirdViewImpl::LadybirdViewImpl(LadybirdWebView* widget)
    : WebView::ViewImplementation::ViewImplementation(WebView::UseJavaScriptBytecode::Yes)
    , m_widget(widget)
{
    on_title_change = [this](DeprecatedString const& title) {
        ladybird_web_view_set_page_title(m_widget, title.characters());
    };
}

LadybirdViewImpl::~LadybirdViewImpl()
{
}

ErrorOr<NonnullOwnPtr<LadybirdViewImpl>> LadybirdViewImpl::create(LadybirdWebView* widget)
{
    auto impl = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LadybirdViewImpl(widget)));
    impl->create_client(WebView::EnableCallgrindProfiling::No);
    return impl;
}

static ErrorOr<NonnullRefPtr<WebView::WebContentClient>> launch_web_content_process(LadybirdViewImpl& view_impl,
    ReadonlySpan<String> candidate_web_content_paths,
    WebView::EnableCallgrindProfiling enable_callgrind_profiling)
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
            if (enable_callgrind_profiling == WebView::EnableCallgrindProfiling::No)
                arguments.remove(0, callgrind_prefix_length);

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

    auto new_client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebView::WebContentClient(move(socket), view_impl)));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    if (enable_callgrind_profiling == WebView::EnableCallgrindProfiling::Yes) {
        dbgln();
        dbgln("\033[1;45mLaunched WebContent process under callgrind!\033[0m");
        dbgln("\033[100mRun `\033[4mcallgrind_control -i on\033[24m` to start instrumentation and `\033[4mcallgrind_control -i off\033[24m` stop it again.\033[0m");
        dbgln();
    }

    return new_client;
}

void LadybirdViewImpl::create_client(WebView::EnableCallgrindProfiling enable_callgrind_profiling)
{
    (void)enable_callgrind_profiling;
    // auto candidate_web_content_paths = get_paths_for_helper_process("WebContent"sv).release_value_but_fixme_should_propagate_errors();
    Vector<String> candidate_web_content_paths;
    candidate_web_content_paths.append("./WebContent"_string.release_value_but_fixme_should_propagate_errors());
    auto new_client = launch_web_content_process(*this,
        candidate_web_content_paths,
        enable_callgrind_profiling)
                          .release_value_but_fixme_should_propagate_errors();

    m_client_state.client = new_client;
    m_client_state.client->on_web_content_process_crash = [this] {
        Core::deferred_invoke([this] {
            handle_web_content_process_crash();
        });
    };
    m_client_state.client_handle = Web::Crypto::generate_random_uuid().release_value_but_fixme_should_propagate_errors();
    client().async_set_window_handle(m_client_state.client_handle);

    if (m_widget) {
        int scale = gtk_widget_get_scale_factor(GTK_WIDGET(m_widget));
        client().async_set_device_pixels_per_css_pixel(scale);
    }
    /*
        client().async_update_system_fonts(
            Gfx::FontDatabase::default_font_query(),
            Gfx::FontDatabase::fixed_width_font_query(),
            Gfx::FontDatabase::window_title_font_query());
    */
}

void LadybirdViewImpl::set_viewport_rect(int x, int y, int width, int height)
{
    m_viewport_rect = Gfx::IntRect(x, y, width, height);
    dbgln("LadybirdViewImpl::set_viewport_rect {}", m_viewport_rect);
    client().async_set_viewport_rect(m_viewport_rect);
    // handle_resize();
    // TODO: do we need to call this here?
    request_repaint();
}

void LadybirdViewImpl::notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize content_size)
{
    dbgln("LadybirdViewImpl::notify_server_did_layout {}", content_size);
    ladybird_web_view_set_page_size(m_widget, content_size.width(), content_size.height());
}

void LadybirdViewImpl::notify_server_did_paint(Badge<WebView::WebContentClient>, i32 bitmap_id, Gfx::IntSize)
{
    dbgln("LadybirdViewImpl::notify_server_did_paint {}", bitmap_id);
}

void LadybirdViewImpl::notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, Gfx::IntRect const& rect)
{
    dbgln("LadybirdViewImpl::notify_server_did_invalidate_content_rect {}", rect);
}

void LadybirdViewImpl::notify_server_did_change_selection(Badge<WebView::WebContentClient>)
{
    dbgln("LadybirdViewImpl::notify_server_did_change_selection");
}

void LadybirdViewImpl::notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_cursor_change");
}

void LadybirdViewImpl::notify_server_did_request_scroll(Badge<WebView::WebContentClient>, i32, i32)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_scroll");
}

void LadybirdViewImpl::notify_server_did_request_scroll_to(Badge<WebView::WebContentClient>, Gfx::IntPoint)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_scroll_to");
}

void LadybirdViewImpl::notify_server_did_request_scroll_into_view(Badge<WebView::WebContentClient>, Gfx::IntRect const&)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_scroll_into_view");
}

void LadybirdViewImpl::notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const&)
{
    dbgln("LadybirdViewImpl::notify_server_did_enter_tooltip_area");
}

void LadybirdViewImpl::notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>)
{
    dbgln("LadybirdViewImpl::notify_server_did_leave_tooltip_area");
}

void LadybirdViewImpl::notify_server_did_request_alert(Badge<WebView::WebContentClient>, String const& message)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_alert {}", message);
}

void LadybirdViewImpl::notify_server_did_request_confirm(Badge<WebView::WebContentClient>, String const& message)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_confirm {}", message);
}

void LadybirdViewImpl::notify_server_did_request_prompt(Badge<WebView::WebContentClient>, String const& message, String const& default_)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_prompt {} {}", message, default_);
}

void LadybirdViewImpl::notify_server_did_request_set_prompt_text(Badge<WebView::WebContentClient>, String const& message)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_set_prompt_text {}", message);
}

void LadybirdViewImpl::notify_server_did_request_accept_dialog(Badge<WebView::WebContentClient>)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_accept_dialog");
}

void LadybirdViewImpl::notify_server_did_request_dismiss_dialog(Badge<WebView::WebContentClient>)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_dismiss_dialog");
}

void LadybirdViewImpl::notify_server_did_request_file(Badge<WebView::WebContentClient>, DeprecatedString const& path, i32)
{
    dbgln("LadybirdViewImpl::notify_server_did_request_file {}", path);
}

void LadybirdViewImpl::notify_server_did_finish_handling_input_event(bool event_was_accepted)
{
    dbgln("LadybirdViewImpl::notify_server_did_finish_handling_input_event {}", event_was_accepted);
}

void LadybirdViewImpl::update_zoom()
{
    dbgln("LadybirdViewImpl::update_zoom");
}

Gfx::IntRect LadybirdViewImpl::viewport_rect() const
{
    return m_viewport_rect;
}

Gfx::IntPoint LadybirdViewImpl::to_content_position(Gfx::IntPoint) const
{
    // This seems unused.
    VERIFY_NOT_REACHED();
}

Gfx::IntPoint LadybirdViewImpl::to_widget_position(Gfx::IntPoint) const
{
    // This seems unused.
    VERIFY_NOT_REACHED();
}
