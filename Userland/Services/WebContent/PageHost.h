/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>
#include <LibWeb/Page/Page.h>

namespace WebContent {

class ConnectionFromClient;

class PageHost final : public Web::PageClient {
    AK_MAKE_NONCOPYABLE(PageHost);
    AK_MAKE_NONMOVABLE(PageHost);

public:
    static NonnullOwnPtr<PageHost> create(ConnectionFromClient& client) { return adopt_own(*new PageHost(client)); }
    virtual ~PageHost();

    Web::Page& page() { return *m_page; }
    const Web::Page& page() const { return *m_page; }

    void paint(const Gfx::IntRect& content_rect, Gfx::Bitmap&);

    void set_palette_impl(const Gfx::PaletteImpl&);
    void set_viewport_rect(const Gfx::IntRect&);
    void set_screen_rects(const Vector<Gfx::IntRect, 4>& rects, size_t main_screen_index) { m_screen_rect = rects[main_screen_index]; };
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

    void set_should_show_line_box_borders(bool b) { m_should_show_line_box_borders = b; }
    void set_has_focus(bool);

private:
    // ^PageClient
    virtual Gfx::Palette palette() const override;
    virtual Gfx::IntRect screen_rect() const override { return m_screen_rect; }
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override { return m_preferred_color_scheme; }
    virtual void page_did_invalidate(const Gfx::IntRect&) override;
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_layout() override;
    virtual void page_did_change_title(const String&) override;
    virtual void page_did_request_scroll(i32, i32) override;
    virtual void page_did_request_scroll_to(Gfx::IntPoint const&) override;
    virtual void page_did_request_scroll_into_view(const Gfx::IntRect&) override;
    virtual void page_did_enter_tooltip_area(const Gfx::IntPoint&, const String&) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(const URL&) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_click_link(const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_middle_click_link(const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_request_context_menu(const Gfx::IntPoint&) override;
    virtual void page_did_request_link_context_menu(const Gfx::IntPoint&, const URL&, const String& target, unsigned modifiers) override;
    virtual void page_did_start_loading(const URL&) override;
    virtual void page_did_finish_loading(const URL&) override;
    virtual void page_did_request_alert(const String&) override;
    virtual bool page_did_request_confirm(const String&) override;
    virtual String page_did_request_prompt(const String&, const String&) override;
    virtual void page_did_change_favicon(const Gfx::Bitmap&) override;
    virtual void page_did_request_image_context_menu(const Gfx::IntPoint&, const URL&, const String& target, unsigned modifiers, const Gfx::Bitmap*) override;
    virtual String page_did_request_cookie(const URL&, Web::Cookie::Source) override;
    virtual void page_did_set_cookie(const URL&, const Web::Cookie::ParsedCookie&, Web::Cookie::Source) override;

    explicit PageHost(ConnectionFromClient&);

    Web::Layout::InitialContainingBlock* layout_root();
    void setup_palette();

    ConnectionFromClient& m_client;
    NonnullOwnPtr<Web::Page> m_page;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Gfx::IntRect m_screen_rect;
    bool m_should_show_line_box_borders { false };
    bool m_has_focus { false };

    RefPtr<Core::Timer> m_invalidation_coalescing_timer;
    Gfx::IntRect m_invalidation_rect;
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };
};

}
