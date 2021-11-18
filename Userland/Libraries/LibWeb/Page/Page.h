/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/URL.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <Kernel/API/KeyCode.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWeb/Forward.h>

namespace Web {

class PageClient;

class Page : public Weakable<Page> {
    AK_MAKE_NONCOPYABLE(Page);
    AK_MAKE_NONMOVABLE(Page);

public:
    explicit Page(PageClient&);
    ~Page();

    PageClient& client() { return m_client; }
    const PageClient& client() const { return m_client; }

    HTML::BrowsingContext& top_level_browsing_context() { return *m_top_level_browsing_context; }
    HTML::BrowsingContext const& top_level_browsing_context() const { return *m_top_level_browsing_context; }

    HTML::BrowsingContext& focused_context();
    HTML::BrowsingContext const& focused_context() const { return const_cast<Page*>(this)->focused_context(); }

    void set_focused_browsing_context(Badge<EventHandler>, HTML::BrowsingContext&);

    void load(const AK::URL&);
    void load(LoadRequest&);

    void load_html(StringView, const AK::URL&);

    bool handle_mouseup(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousedown(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousemove(const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);
    bool handle_mousewheel(const Gfx::IntPoint&, unsigned button, unsigned modifiers, int wheel_delta);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);
    bool handle_keyup(KeyCode, unsigned modifiers, u32 code_point);

    Gfx::Palette palette() const;
    Gfx::IntRect screen_rect() const;
    CSS::PreferredColorScheme preferred_color_scheme() const;

    bool is_same_origin_policy_enabled() const { return m_same_origin_policy_enabled; }
    void set_same_origin_policy_enabled(bool b) { m_same_origin_policy_enabled = b; }

private:
    PageClient& m_client;

    RefPtr<HTML::BrowsingContext> m_top_level_browsing_context;
    WeakPtr<HTML::BrowsingContext> m_focused_context;

    // FIXME: Enable this by default once CORS preflight checks are supported.
    bool m_same_origin_policy_enabled { false };
};

class PageClient {
public:
    virtual Gfx::Palette palette() const = 0;
    virtual Gfx::IntRect screen_rect() const = 0;
    virtual CSS::PreferredColorScheme preferred_color_scheme() const = 0;
    virtual void page_did_set_document_in_top_level_browsing_context(DOM::Document*) { }
    virtual void page_did_change_title(const String&) { }
    virtual void page_did_start_loading(const AK::URL&) { }
    virtual void page_did_finish_loading(const AK::URL&) { }
    virtual void page_did_change_selection() { }
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) { }
    virtual void page_did_request_context_menu(const Gfx::IntPoint&) { }
    virtual void page_did_request_link_context_menu(const Gfx::IntPoint&, const AK::URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_request_image_context_menu(const Gfx::IntPoint&, const AK::URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers, const Gfx::Bitmap*) { }
    virtual void page_did_click_link(const AK::URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_middle_click_link(const AK::URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_enter_tooltip_area(const Gfx::IntPoint&, const String&) { }
    virtual void page_did_leave_tooltip_area() { }
    virtual void page_did_hover_link(const AK::URL&) { }
    virtual void page_did_unhover_link() { }
    virtual void page_did_invalidate(const Gfx::IntRect&) { }
    virtual void page_did_change_favicon(const Gfx::Bitmap&) { }
    virtual void page_did_layout() { }
    virtual void page_did_request_scroll(i32, i32) { }
    virtual void page_did_request_scroll_to(Gfx::IntPoint const&) { }
    virtual void page_did_request_scroll_into_view(const Gfx::IntRect&) { }
    virtual void page_did_request_alert(const String&) { }
    virtual bool page_did_request_confirm(const String&) { return false; }
    virtual String page_did_request_prompt(const String&, const String&) { return {}; }
    virtual String page_did_request_cookie(const AK::URL&, Cookie::Source) { return {}; }
    virtual void page_did_set_cookie(const AK::URL&, const Cookie::ParsedCookie&, Cookie::Source) { }

protected:
    virtual ~PageClient() = default;
};

}
