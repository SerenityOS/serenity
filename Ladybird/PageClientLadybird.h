/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ConsoleClient.h"
#include "CookieJar.h"
#include <LibGfx/Rect.h>
#include <LibWeb/Page/Page.h>

class WebView;

namespace Ladybird {

class PageClientLadybird final : public Web::PageClient {
public:
    static NonnullOwnPtr<PageClientLadybird> create(WebView&);

    Web::Page& page() { return *m_page; }
    Web::Page const& page() const { return *m_page; }

    Web::Layout::InitialContainingBlock* layout_root();

    void load(AK::URL const&);

    void paint(Gfx::IntRect const& content_rect, Gfx::Bitmap& target);

    void setup_palette(Core::AnonymousBuffer theme_buffer);

    Gfx::IntRect viewport_rect() const;
    void set_viewport_rect(Gfx::IntRect);

    // ^Web::PageClient
    virtual Gfx::Palette palette() const override;
    virtual Gfx::IntRect screen_rect() const override;
    virtual Web::CSS::PreferredColorScheme preferred_color_scheme() const override;
    virtual void page_did_change_title(String const& title) override;
    virtual void page_did_start_loading(AK::URL const& url) override;
    virtual void page_did_finish_loading(AK::URL const&) override;
    void initialize_js_console();
    virtual void page_did_change_selection() override;
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) override;
    virtual void page_did_request_context_menu(Gfx::IntPoint const&) override;
    virtual void page_did_request_link_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned) override;
    virtual void page_did_request_image_context_menu(Gfx::IntPoint const&, AK::URL const&, String const&, unsigned, Gfx::Bitmap const*) override;
    virtual void page_did_click_link(AK::URL const&, String const&, unsigned) override;
    virtual void page_did_middle_click_link(AK::URL const&, String const&, unsigned) override;
    virtual void page_did_enter_tooltip_area(Gfx::IntPoint const& content_position, String const& tooltip) override;
    virtual void page_did_leave_tooltip_area() override;
    virtual void page_did_hover_link(AK::URL const& url) override;
    virtual void page_did_unhover_link() override;
    virtual void page_did_invalidate(Gfx::IntRect const&) override;
    virtual void page_did_change_favicon(Gfx::Bitmap const&) override;
    virtual void page_did_layout() override;
    virtual void page_did_request_scroll_into_view(Gfx::IntRect const&) override;
    virtual void page_did_request_alert(String const& message) override;
    virtual bool page_did_request_confirm(String const& message) override;
    virtual String page_did_request_prompt(String const&, String const&) override;
    virtual String page_did_request_cookie(AK::URL const&, Web::Cookie::Source) override;
    virtual void page_did_set_cookie(AK::URL const&, Web::Cookie::ParsedCookie const&, Web::Cookie::Source) override;

    void dump_cookies() const;

    void request_file(NonnullRefPtr<Web::FileRequest>& request) override;

    void set_should_show_line_box_borders(bool);

    explicit PageClientLadybird(WebView&);

    WebView& m_view;
    NonnullOwnPtr<Web::Page> m_page;
    Browser::CookieJar m_cookie_jar;

    OwnPtr<Ladybird::ConsoleClient> m_console_client;
    WeakPtr<JS::Realm> m_realm;
    RefPtr<Gfx::PaletteImpl> m_palette_impl;
    Gfx::IntRect m_viewport_rect { 0, 0, 800, 600 };
    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };
    bool m_should_show_line_box_borders { false };
};

}
