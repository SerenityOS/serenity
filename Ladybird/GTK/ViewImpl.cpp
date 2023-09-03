#include "ViewImpl.h"
#include "BitmapPaintable.h"
#include "WebViewPrivate.h"
#include <Ladybird/HelperProcess.h>
#include <Ladybird/Utilities.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/Crypto/Crypto.h>
#include <LibWebView/CookieJar.h>
#include <adwaita.h>

LadybirdViewImpl::LadybirdViewImpl(LadybirdWebView* widget)
    : WebView::ViewImplementation::ViewImplementation()
    , m_widget(widget)
{
    on_did_layout = [this](auto content_size) {
        ladybird_web_view_set_page_size(m_widget, content_size.width(), content_size.height());
    };

    on_ready_to_paint = [this]() {
        auto size = m_client_state.front_bitmap.last_painted_size;

        ladybird_bitmap_paintable_push_bitmap(
            LADYBIRD_BITMAP_PAINTABLE(ladybird_web_view_get_bitmap_paintable(m_widget)),
            m_client_state.front_bitmap.bitmap.ptr(),
            size.width(), size.height(),
            gtk_widget_get_scale_factor(GTK_WIDGET(m_widget)),
            true);
    };

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

    on_get_all_cookies = [this](AK::URL const& url) {
        return cookie_jar().get_all_cookies(url);
    };
    on_get_named_cookie = [this](AK::URL const& url, DeprecatedString const& name) {
        return cookie_jar().get_named_cookie(url, name);
    };
    on_get_cookie = [this](AK::URL const& url, Web::Cookie::Source source) {
        return cookie_jar().get_cookie(url, source);
    };
    on_set_cookie = [this](AK::URL const& url, Web::Cookie::ParsedCookie const& cookie, Web::Cookie::Source source) {
        cookie_jar().set_cookie(url, cookie, source);
    };
    on_update_cookie = [this](Web::Cookie::Cookie const& cookie) {
        cookie_jar().update_cookie(cookie);
    };

    on_favicon_change = [this](Gfx::Bitmap const& bitmap) {
        LadybirdBitmapPaintable* favicon_paintable = LADYBIRD_BITMAP_PAINTABLE(ladybird_web_view_get_favicon(m_widget));
        ladybird_bitmap_paintable_push_bitmap(favicon_paintable, &bitmap, bitmap.width(), bitmap.height(), 1.0, false);
    };

    on_scroll_by_delta = [this](auto x_delta, auto y_delta) {
        ladybird_web_view_scroll_by(m_widget, x_delta, y_delta);
    };

    on_scroll_to_point = [this](auto position) {
        ladybird_web_view_scroll_to(m_widget, position.x(), position.y());
    };

    on_scroll_into_view = [this](auto rect) {
        ladybird_web_view_scroll_into_view(m_widget, rect.left(), rect.top(), rect.width(), rect.height());
    };

    on_cursor_change = [this](auto cursor) {
        update_cursor(cursor);
    };

    on_link_hover = [this](AK::URL const& url) {
        DeprecatedString url_string = url.serialize();
        ladybird_web_view_set_hovered_link(m_widget, url_string.characters());
    };
    on_link_unhover = [this]() {
        ladybird_web_view_set_hovered_link(m_widget, nullptr);
    };

    on_request_alert = [this](AK::String const& message) {
        ladybird_web_view_request_alert(m_widget, message.to_deprecated_string().characters());
    };
    on_request_confirm = [this](AK::String const& message) {
        ladybird_web_view_request_confirm(m_widget, message.to_deprecated_string().characters());
    };
    on_request_prompt = [this](AK::String const& message, AK::String const& text) {
        ladybird_web_view_request_prompt(m_widget, message.to_deprecated_string().characters(), text.to_deprecated_string().characters());
    };
    on_request_set_prompt_text = [this](AK::String const& text) {
        ladybird_web_view_set_prompt_text(m_widget, text.to_deprecated_string().characters());
    };
    on_request_accept_dialog = [this]() {
        ladybird_web_view_request_accept_dialog(m_widget);
    };
    on_request_dismiss_dialog = [this]() {
        ladybird_web_view_request_dismiss_dialog(m_widget);
    };

    on_link_click = [this](AK::URL const& url, DeprecatedString const& target, unsigned modifiers) {
        bool switch_to_new_tab = false;
        if (modifiers & Mod_Ctrl) {
            // switch_to_new_tab = very false;
        } else if (target == "_blank") {
            switch_to_new_tab = true;
        } else {
            // load(url);
            return;
        }
        ladybird_web_view_activate_url(m_widget, url.serialize().characters(), switch_to_new_tab);
    };
    on_link_middle_click = [this](AK::URL const& url, [[maybe_unused]] DeprecatedString const& target, [[maybe_unused]] unsigned modifiers) {
        ladybird_web_view_activate_url(m_widget, url.serialize().characters(), true);
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

WebView::CookieJar& LadybirdViewImpl::cookie_jar()
{
    WebView::CookieJar* jar = ladybird_web_view_get_cookie_jar(m_widget);
    VERIFY(jar);
    return *jar;
}

void LadybirdViewImpl::create_client(WebView::EnableCallgrindProfiling enable_callgrind_profiling)
{
    auto candidate_web_content_paths = MUST(get_paths_for_helper_process("WebContent"sv));
    auto new_client = launch_web_content_process(*this,
        candidate_web_content_paths,
        enable_callgrind_profiling,
        WebView::IsLayoutTestMode::No,
        Ladybird::UseLagomNetworking::No)
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

    // XXX: Always send the light theme colors, even when the current style is dark.
    base = Gfx::Color::from_rgb(0xfafafa);
    accent = Gfx::Color::from_rgb(0x1c71d8);
    base_text = Gfx::Color(Gfx::Color::Black).with_alpha(204);

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

void LadybirdViewImpl::key_down(KeyCode key_code, unsigned modifiers, u32 code_point)
{
    client().async_key_down(key_code, modifiers, code_point);
}

void LadybirdViewImpl::key_up(KeyCode key_code, unsigned modifiers, u32 code_point)
{
    client().async_key_up(key_code, modifiers, code_point);
}

void LadybirdViewImpl::connect_to_webdriver(char const* path)
{
    client().async_connect_to_webdriver(path);
}

void LadybirdViewImpl::update_cursor(Gfx::StandardCursor cursor)
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
