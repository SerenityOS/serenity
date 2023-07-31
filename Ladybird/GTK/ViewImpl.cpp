#include "ViewImpl.h"
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/Crypto/Crypto.h>

LadybirdViewImpl::LadybirdViewImpl()
    : WebView::ViewImplementation::ViewImplementation(WebView::UseJavaScriptBytecode::Yes)
{
}

LadybirdViewImpl::~LadybirdViewImpl()
{
}

ErrorOr<NonnullOwnPtr<LadybirdViewImpl>> LadybirdViewImpl::create(LadybirdWebContentView* widget)
{
    auto impl = TRY(adopt_nonnull_own_or_enomem(new (nothrow) LadybirdViewImpl));
    impl->m_widget = widget;
    impl->create_client(WebView::EnableCallgrindProfiling::No);
    return impl;
}

void LadybirdViewImpl::create_client(WebView::EnableCallgrindProfiling enable_callgrind_profiling)
{
    (void)enable_callgrind_profiling;
    // auto candidate_web_content_paths = get_paths_for_helper_process("WebContent"sv).release_value_but_fixme_should_propagate_errors();
    Vector<String> candidate_web_content_paths;
    candidate_web_content_paths.append("./WebContent"_string.release_value_but_fixme_should_propagate_errors());
    auto new_client = launch_web_content_process(
        candidate_web_content_paths,
        enable_callgrind_profiling,
        WebView::IsLayoutTestMode::No,
        WebView::UseJavaScriptBytecode::Yes)
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
    client().async_update_system_fonts(
        Gfx::FontDatabase::default_font_query(),
        Gfx::FontDatabase::fixed_width_font_query(),
        Gfx::FontDatabase::window_title_font_query());
}

void LadybirdViewImpl::notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize content_size)
{
    dbg("LadybirdViewImpl::notify_server_did_layout {}", content_size);
}

void LadybirdViewImpl::notify_server_did_paint(Badge<WebView::WebContentClient>, i32 bitmap_id, Gfx::IntSize)
{
    dbg("LadybirdViewImpl::notify_server_did_paint {}", bitmap_id);
}

void LadybirdViewImpl::notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, Gfx::IntRect const&)
{
    dbg("LadybirdViewImpl::notify_server_did_invalidate_content_rect");
}

void LadybirdViewImpl::notify_server_did_change_selection(Badge<WebView::WebContentClient>)
{
    dbg("LadybirdViewImpl::notify_server_did_change_selection");
}

void LadybirdViewImpl::notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor)
{
    dbg("LadybirdViewImpl::notify_server_did_request_cursor_change");
}

void LadybirdViewImpl::notify_server_did_request_scroll(Badge<WebView::WebContentClient>, i32, i32)
{
    dbg("LadybirdViewImpl::notify_server_did_request_scroll");
}

void LadybirdViewImpl::notify_server_did_request_scroll_to(Badge<WebView::WebContentClient>, Gfx::IntPoint)
{
    dbg("LadybirdViewImpl::notify_server_did_request_scroll_to");
}

void LadybirdViewImpl::notify_server_did_request_scroll_into_view(Badge<WebView::WebContentClient>, Gfx::IntRect const&)
{
    dbg("LadybirdViewImpl::notify_server_did_request_scroll_into_view");
}

void LadybirdViewImpl::notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const&)
{
    dbg("LadybirdViewImpl::notify_server_did_enter_tooltip_area");
}

void LadybirdViewImpl::notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>)
{
    dbg("LadybirdViewImpl::notify_server_did_leave_tooltip_area");
}

void LadybirdViewImpl::notify_server_did_request_alert(Badge<WebView::WebContentClient>, String const& message)
{
    dbg("LadybirdViewImpl::notify_server_did_request_alert {}", message);
}

void LadybirdViewImpl::notify_server_did_request_confirm(Badge<WebView::WebContentClient>, String const& message)
{
    dbg("LadybirdViewImpl::notify_server_did_request_confirm {}", message);
}

void LadybirdViewImpl::notify_server_did_request_prompt(Badge<WebView::WebContentClient>, String const& message, String const& default_)
{
    dbg("LadybirdViewImpl::notify_server_did_request_prompt {} {}", message, default_);
}

void LadybirdViewImpl::notify_server_did_request_set_prompt_text(Badge<WebView::WebContentClient>, String const& message)
{
    dbg("LadybirdViewImpl::notify_server_did_request_set_prompt_text {}", message);
}

void LadybirdViewImpl::notify_server_did_request_accept_dialog(Badge<WebView::WebContentClient>)
{
    dbg("LadybirdViewImpl::notify_server_did_request_accept_dialog");
}

void LadybirdViewImpl::notify_server_did_request_dismiss_dialog(Badge<WebView::WebContentClient>)
{
    dbg("LadybirdViewImpl::notify_server_did_request_dismiss_dialog");
}

void LadybirdViewImpl::notify_server_did_request_file(Badge<WebView::WebContentClient>, DeprecatedString const& path, i32)
{
    dbg("LadybirdViewImpl::notify_server_did_request_file {}", path);
}

void LadybirdViewImpl::notify_server_did_finish_handling_input_event(bool event_was_accepted)
{
    dbg("LadybirdViewImpl::notify_server_did_finish_handling_input_event {}", event_was_accepted);
}

void LadybirdViewImpl::update_zoom()
{
    dbg("LadybirdViewImpl::update_zoom");
}

Gfx::IntRect LadybirdViewImpl::viewport_rect() const
{
    dbg("LadybirdViewImpl::viewport_rect");
    return {};
}

Gfx::IntPoint LadybirdViewImpl::to_content_position(Gfx::IntPoint widget_position) const
{
    (void)widget_position;
    dbg("LadybirdViewImpl::to_content_position");
    return {};
}

Gfx::IntPoint LadybirdViewImpl::to_widget_position(Gfx::IntPoint content_position) const
{
    (void)content_position;
    dbg("LadybirdViewImpl::to_widget_position");
    return {};
}
