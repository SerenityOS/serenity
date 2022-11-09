/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Page/Page.h>
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

    virtual void paint(Gfx::IntRect const& content_rect, Gfx::Bitmap&) override;

    void set_palette_impl(Gfx::PaletteImpl const&);
    void set_viewport_rect(Gfx::IntRect const&);
    void set_screen_rects(Vector<Gfx::IntRect, 4> const& rects, size_t main_screen_index) { m_screen_rect = rects[main_screen_index]; };
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);
    void set_should_show_line_box_borders(bool b) { m_should_show_line_box_borders = b; }
    void set_has_focus(bool);
    void set_is_scripting_enabled(bool);
    void set_window_position(Gfx::IntPoint const&);
    void set_window_size(Gfx::IntSize const&);

    Gfx::IntSize const& content_size() const { return m_content_size; }

    ErrorOr<void> connect_to_webdriver(String const& webdriver_ipc_path);

    void alert_closed();
    void confirm_closed(bool accepted);
    void prompt_closed(String response);

private:
    // ^PageClient
    virtual bool is_connection_open() const override;
    virtual Gfx::Palette palette() const override;
    virtual Gfx::IntRect screen_rect() const override { return m_screen_rect; }
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override { return m_preferred_color_scheme; }
    virtual void page_did_invalidate(Gfx::IntRect const&) override;
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_layout() override;
    virtual void page_did_change_title(String const&) override;
    virtual void page_did_request_navigate_back() override;
    virtual void page_did_request_navigate_forward() override;
    virtual void page_did_request_refresh() override;
    virtual Gfx::IntSize page_did_request_resize_window(Gfx::IntSize const&) override;
    virtual Gfx::IntPoint page_did_request_reposition_window(Gfx::IntPoint const&) override;
    virtual void page_did_request_restore_window() override;
    virtual Gfx::IntRect page_did_request_maximize_window() override;
    virtual Gfx::IntRect page_did_request_minimize_window() override;
    virtual Gfx::IntRect page_did_request_fullscreen_window() override;
    virtual void page_did_request_scroll(i32, i32) override;
    virtual void page_did_request_scroll_to(Gfx::IntPoint const&) override;
    virtual void page_did_request_scroll_into_view(Gfx::IntRect const&) override;
    virtual void page_did_enter_tooltip_area(Gfx::IntPoint const&, String const&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(const URL&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_click_link(const URL&, String const& target, unsigned modifiers) override;
    virtual void page_did_middle_click_link(const URL&, String const& target, unsigned modifiers) override;
    virtual void page_did_request_context_menu(Gfx::IntPoint const&) override;
    virtual void page_did_request_link_context_menu(Gfx::IntPoint const&, const URL&, String const& target, unsigned modifiers) override;
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
    virtual void page_did_request_image_context_menu(Gfx::IntPoint const&, const URL&, String const& target, unsigned modifiers, Gfx::Bitmap const*) override;
    virtual Vector<Web::Cookie::Cookie> page_did_request_all_cookies(URL const&) override;
    virtual Optional<Web::Cookie::Cookie> page_did_request_named_cookie(URL const&, String const&) override;
    virtual String page_did_request_cookie(const URL&, Web::Cookie::Source) override;
    virtual void page_did_set_cookie(const URL&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override;
    virtual void page_did_update_cookie(URL const&, Web::Cookie::Cookie) override;
    virtual void page_did_update_resource_count(i32) override;
    virtual void request_file(NonnullRefPtr<Web::FileRequest>&) override;

    explicit PageHost(ConnectionFromClient&);

    Web::Layout::InitialContainingBlock* layout_root();
    void setup_palette();

    ConnectionFromClient& m_client;
    NonnullOwnPtr<Web::Page> m_page;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Gfx::IntRect m_screen_rect;
    Gfx::IntSize m_content_size;
    bool m_should_show_line_box_borders { false };
    bool m_has_focus { false };

    RefPtr<Web::Platform::Timer> m_invalidation_coalescing_timer;
    Gfx::IntRect m_invalidation_rect;
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };

    RefPtr<WebDriverConnection> m_webdriver;
};

}
