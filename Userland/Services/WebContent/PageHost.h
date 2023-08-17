/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/PixelUnits.h>
#include <WebContent/Forward.h>

namespace WebContent {

class ConnectionFromClient;

class PageHost final : public Web::PageClient {
    AK_MAKE_NONCOPYABLE(PageHost);
    AK_MAKE_NONMOVABLE(PageHost);

public:
    static NonnullOwnPtr<PageHost> create(ConnectionFromClient& client) { return adopt_own(*new PageHost(client)); }
    virtual ~PageHost();

    virtual Web::Page& page() override { return *m_page; }
    virtual Web::Page const& page() const override { return *m_page; }

    virtual void paint(Web::DevicePixelRect const& content_rect, Gfx::Bitmap&) override;

    void set_palette_impl(Gfx::PaletteImpl&);
    void set_viewport_rect(Web::DevicePixelRect const&);
    void set_screen_rects(Vector<Gfx::IntRect, 4> const& rects, size_t main_screen_index) { m_screen_rect = rects[main_screen_index].to_type<Web::DevicePixels>(); }
    void set_device_pixels_per_css_pixel(float device_pixels_per_css_pixel) { m_device_pixels_per_css_pixel = device_pixels_per_css_pixel; }
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);
    void set_should_show_line_box_borders(bool b) { m_should_show_line_box_borders = b; }
    void set_has_focus(bool);
    void set_is_scripting_enabled(bool);
    void set_window_position(Web::DevicePixelPoint);
    void set_window_size(Web::DevicePixelSize);

    Web::DevicePixelSize content_size() const { return m_content_size; }

    ErrorOr<void> connect_to_webdriver(DeprecatedString const& webdriver_ipc_path);
    Function<void(WebDriverConnection&)> on_webdriver_connection;

    void alert_closed();
    void confirm_closed(bool accepted);
    void prompt_closed(Optional<String> response);

    Web::WebIDL::ExceptionOr<void> toggle_media_play_state();
    void toggle_media_mute_state();
    Web::WebIDL::ExceptionOr<void> toggle_media_loop_state();
    Web::WebIDL::ExceptionOr<void> toggle_media_controls_state();

    [[nodiscard]] Gfx::Color background_color() const;

private:
    // ^PageClient
    virtual bool is_connection_open() const override;
    virtual Gfx::Palette palette() const override;
    virtual Web::DevicePixelRect screen_rect() const override { return m_screen_rect; }
    virtual double device_pixels_per_css_pixel() const override { return m_device_pixels_per_css_pixel; }
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override { return m_preferred_color_scheme; }
    virtual void page_did_invalidate(Web::CSSPixelRect const&) override;
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_layout() override;
    virtual void page_did_change_title(DeprecatedString const&) override;
    virtual void page_did_request_navigate_back() override;
    virtual void page_did_request_navigate_forward() override;
    virtual void page_did_request_refresh() override;
    virtual Gfx::IntSize page_did_request_resize_window(Gfx::IntSize) override;
    virtual Gfx::IntPoint page_did_request_reposition_window(Gfx::IntPoint) override;
    virtual void page_did_request_restore_window() override;
    virtual Gfx::IntRect page_did_request_maximize_window() override;
    virtual Gfx::IntRect page_did_request_minimize_window() override;
    virtual Gfx::IntRect page_did_request_fullscreen_window() override;
    virtual void page_did_request_scroll(i32, i32) override;
    virtual void page_did_request_scroll_to(Web::CSSPixelPoint) override;
    virtual void page_did_request_scroll_into_view(Web::CSSPixelRect const&) override;
    virtual void page_did_enter_tooltip_area(Web::CSSPixelPoint, DeprecatedString const&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(const URL&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_click_link(const URL&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void page_did_middle_click_link(const URL&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void page_did_request_context_menu(Web::CSSPixelPoint) override;
    virtual void page_did_request_link_context_menu(Web::CSSPixelPoint, URL const&, DeprecatedString const& target, unsigned modifiers) override;
    virtual void page_did_request_image_context_menu(Web::CSSPixelPoint, const URL&, DeprecatedString const& target, unsigned modifiers, Gfx::Bitmap const*) override;
    virtual void page_did_request_media_context_menu(Web::CSSPixelPoint, DeprecatedString const& target, unsigned modifiers, Web::Page::MediaContextMenu) override;
    virtual void page_did_start_loading(const URL&, bool) override;
    virtual void page_did_create_main_document() override;
    virtual void page_did_finish_loading(const URL&) override;
    virtual void page_did_request_alert(String const&) override;
    virtual void page_did_request_confirm(String const&) override;
    virtual void page_did_request_prompt(String const&, String const&) override;
    virtual void page_did_request_set_prompt_text(String const&) override;
    virtual void page_did_request_accept_dialog() override;
    virtual void page_did_request_dismiss_dialog() override;
    virtual void page_did_change_favicon(Gfx::Bitmap const&) override;
    virtual Vector<Web::Cookie::Cookie> page_did_request_all_cookies(URL const&) override;
    virtual Optional<Web::Cookie::Cookie> page_did_request_named_cookie(URL const&, DeprecatedString const&) override;
    virtual DeprecatedString page_did_request_cookie(const URL&, Web::Cookie::Source) override;
    virtual void page_did_set_cookie(const URL&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override;
    virtual void page_did_update_cookie(Web::Cookie::Cookie) override;
    virtual void page_did_update_resource_count(i32) override;
    virtual String page_did_request_new_tab(Web::HTML::ActivateTab activate_tab) override;
    virtual void page_did_request_activate_tab() override;
    virtual void page_did_close_browsing_context(Web::HTML::BrowsingContext const&) override;
    virtual void request_file(Web::FileRequest) override;

    explicit PageHost(ConnectionFromClient&);

    Web::Layout::Viewport* layout_root();
    void setup_palette();

    ConnectionFromClient& m_client;
    NonnullOwnPtr<Web::Page> m_page;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Web::DevicePixelRect m_screen_rect;
    Web::DevicePixelSize m_content_size;
    float m_device_pixels_per_css_pixel { 1.0f };
    bool m_should_show_line_box_borders { false };
    bool m_has_focus { false };

    RefPtr<Web::Platform::Timer> m_invalidation_coalescing_timer;
    Web::DevicePixelRect m_invalidation_rect;
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };

    RefPtr<WebDriverConnection> m_webdriver;
};

}
