#include "ViewImpl.h"
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/Crypto/Crypto.h>
#include <adwaita.h>

LadybirdViewImpl::LadybirdViewImpl(LadybirdWebView* widget)
    : WebView::ViewImplementation::ViewImplementation()
    , m_widget(widget)
{
    on_title_change = [this](DeprecatedString const& title) {
        ladybird_web_view_set_page_title(m_widget, title.characters());
    };
    on_load_start = [this](AK::URL const& url, [[maybe_unused]] bool is_redirect) {
        DeprecatedString url_string = url.serialize();
        ladybird_web_view_set_page_url(m_widget, url_string.characters());
        ladybird_web_view_set_loading(m_widget, true);
    };
    on_load_finish = [this](AK::URL const& url) {
        DeprecatedString url_string = url.serialize();
        ladybird_web_view_set_page_url(m_widget, url_string.characters());
        ladybird_web_view_set_loading(m_widget, false);
    };

    AdwStyleManager* style_manager = adw_style_manager_get_default();
    m_update_style_id = g_signal_connect_swapped(style_manager, "notify::dark", G_CALLBACK(+[](void* user_data) {
        LadybirdViewImpl* self = reinterpret_cast<LadybirdViewImpl*>(user_data);
        self->update_theme();
    }),
        this);
}

LadybirdViewImpl::~LadybirdViewImpl()
{
    AdwStyleManager* style_manager = adw_style_manager_get_default();
    if (m_update_style_id)
        g_signal_handler_disconnect(style_manager, m_update_style_id);
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
    candidate_web_content_paths.append("./WebContent"_string);
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
    update_theme();

    /*
        client().async_update_system_fonts(
            Gfx::FontDatabase::default_font_query(),
            Gfx::FontDatabase::fixed_width_font_query(),
            Gfx::FontDatabase::window_title_font_query());
    */
}

void LadybirdViewImpl::update_theme()
{
    auto theme = Gfx::load_system_theme(DeprecatedString::formatted("{}/Base/res/themes/Default.ini", getenv("SERENITY_SOURCE_DIR"))).release_value_but_fixme_should_propagate_errors();
    auto palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);
    auto palette = Gfx::Palette(move(palette_impl));

    AdwStyleManager* style_manager = adw_style_manager_get_default();
    bool is_dark = adw_style_manager_get_dark(style_manager);
    palette.set_flag(Gfx::FlagRole::IsDark, is_dark);

    // TODO: Once https://gitlab.gnome.org/GNOME/libadwaita/-/merge_requests/369 lands,
    // we're going to have actual libadwaita API for dynamically querying these colors
    // (in addition to easily setting them). For now, we hardcode the color values as
    // documented at https://gnome.pages.gitlab.gnome.org/libadwaita/doc/main/named-colors.html
    Gfx::Color base, accent, selection, base_text, button;
    if (!is_dark) {
        base = Gfx::Color::from_rgb(0xfafafa);
        accent = Gfx::Color::from_rgb(0x1c71d8);
        base_text = Gfx::Color(Gfx::Color::Black).with_alpha(204);
    } else {
        base = Gfx::Color::from_rgb(0x242424);
        accent = Gfx::Color::from_rgb(0x78aeed);
        base_text = Gfx::Color::White;
    }
    selection = accent.with_alpha(77);                      // gtkalpha($accent_bg_color, 0.3)
    button = base_text.with_alpha(base_text.alpha() * 0.1); // gtkalpha(currentColor, .1)
    palette.set_color(Gfx::ColorRole::Accent, accent);
    palette.set_color(Gfx::ColorRole::Selection, selection);
    palette.set_color(Gfx::ColorRole::Base, base);
    palette.set_color(Gfx::ColorRole::BaseText, base_text);
    palette.set_color(Gfx::ColorRole::SelectionText, base_text);
    palette.set_color(Gfx::ColorRole::ButtonText, base_text);
    palette.set_color(Gfx::ColorRole::Button, button);

    client().async_update_system_theme(theme);
}

void LadybirdViewImpl::set_viewport_rect(int x, int y, int width, int height)
{
    m_viewport_rect = Gfx::IntRect(x, y, width, height);
    client().async_set_viewport_rect(m_viewport_rect);
    handle_resize();
    request_repaint();
}

void LadybirdViewImpl::scale_factor_changed()
{
    update_zoom();
}

void LadybirdViewImpl::mouse_down(int x, int y, unsigned button, unsigned buttons, unsigned modifiers)
{
    Gfx::IntPoint point(x, y);
    client().async_mouse_down(point, button, buttons, modifiers);
}

void LadybirdViewImpl::mouse_move(int x, int y, unsigned buttons, unsigned modifiers)
{
    Gfx::IntPoint point(x, y);
    client().async_mouse_move(point, 0, buttons, modifiers);
}

void LadybirdViewImpl::mouse_up(int x, int y, unsigned button, unsigned buttons, unsigned modifiers)
{
    Gfx::IntPoint point(x, y);
    client().async_mouse_up(point, button, buttons, modifiers);
}

void LadybirdViewImpl::notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize content_size)
{
    ladybird_web_view_set_page_size(m_widget, content_size.width(), content_size.height());
}

void LadybirdViewImpl::notify_server_did_paint(Badge<WebView::WebContentClient>, i32 bitmap_id, Gfx::IntSize size)
{
    // TODO: Can this not be true? Can WebContent paint into the front bitmap?
    if (m_client_state.back_bitmap.id == bitmap_id) {
        m_client_state.has_usable_bitmap = true;
        m_client_state.back_bitmap.pending_paints--;
        m_client_state.back_bitmap.last_painted_size = size;
        swap(m_client_state.back_bitmap, m_client_state.front_bitmap);
        m_backup_bitmap = nullptr;

        ladybird_web_view_push_bitmap(m_widget, m_client_state.front_bitmap.bitmap.ptr(), size.width(), size.height());

        if (m_client_state.got_repaint_requests_while_painting) {
            m_client_state.got_repaint_requests_while_painting = false;
            request_repaint();
        }
    }
}

void LadybirdViewImpl::notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, [[maybe_unused]] Gfx::IntRect const& rect)
{
    request_repaint();
}

void LadybirdViewImpl::notify_server_did_change_selection(Badge<WebView::WebContentClient>)
{
    dbgln("LadybirdViewImpl::notify_server_did_change_selection");
    request_repaint();
}

void LadybirdViewImpl::notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor cursor)
{
    char const* name;

    switch (cursor) {
    case Gfx::StandardCursor::None:
    case Gfx::StandardCursor::Hidden:
        name = "none";
        break;
    case Gfx::StandardCursor::Arrow:
    default:
        name = "default";
        break;
    case Gfx::StandardCursor::Crosshair:
        name = "crosshair";
        break;
    case Gfx::StandardCursor::IBeam:
        name = "text";
        break;
    case Gfx::StandardCursor::ResizeHorizontal:
    case Gfx::StandardCursor::ResizeColumn:
        name = "col-resize";
        break;
    case Gfx::StandardCursor::ResizeVertical:
    case Gfx::StandardCursor::ResizeRow:
        name = "row-resize";
        break;
    case Gfx::StandardCursor::ResizeDiagonalTLBR:
        name = "nwse-resize";
        break;
    case Gfx::StandardCursor::ResizeDiagonalBLTR:
        name = "nesw-resize";
        break;
    case Gfx::StandardCursor::Hand:
        name = "pointer";
        break;
    case Gfx::StandardCursor::Help:
        name = "help";
        break;
    case Gfx::StandardCursor::Drag:
        name = "grabbing";
        break;
    case Gfx::StandardCursor::DragCopy:
        name = "copy";
        break;
    case Gfx::StandardCursor::Move:
        // not "move"!
        name = "grabbing";
        break;
    case Gfx::StandardCursor::Wait:
        name = "wait";
        break;
    case Gfx::StandardCursor::Disallowed:
        name = "not-allowed";
        break;
        // case Gfx::StandardCursor::Eyedropper:
        // case Gfx::StandardCursor::Zoom:
    }

    gtk_widget_set_cursor_from_name(GTK_WIDGET(m_widget), name);
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
    // dbgln("LadybirdViewImpl::notify_server_did_leave_tooltip_area");
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

void LadybirdViewImpl::notify_server_did_finish_handling_input_event([[maybe_unused]] bool event_was_accepted)
{
    // dbgln("LadybirdViewImpl::notify_server_did_finish_handling_input_event {}", event_was_accepted);
}

void LadybirdViewImpl::update_zoom()
{
    int scale_factor = gtk_widget_get_scale_factor(GTK_WIDGET(m_widget));
    client().async_set_device_pixels_per_css_pixel(scale_factor * m_zoom_level);
    request_repaint();
    // TODO: Why do we have to do this here?
    gtk_widget_queue_allocate(GTK_WIDGET(m_widget));
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
