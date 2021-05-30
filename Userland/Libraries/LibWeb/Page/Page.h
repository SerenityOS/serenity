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

    Web::BrowsingContext& top_level_browsing_context() { return *m_top_level_browsing_context; }
    const Web::BrowsingContext& top_level_browsing_context() const { return *m_top_level_browsing_context; }

    Web::BrowsingContext& focused_context();
    const Web::BrowsingContext& focused_context() const { return const_cast<Page*>(this)->focused_context(); }

    void set_focused_browsing_context(Badge<EventHandler>, BrowsingContext&);

    void load(const URL&);
    void load(const LoadRequest&);

    void load_html(const StringView&, const URL&);

    bool handle_mouseup(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousedown(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousemove(const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);
    bool handle_mousewheel(const Gfx::IntPoint&, unsigned button, unsigned modifiers, int wheel_delta);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);

    Gfx::Palette palette() const;
    Gfx::IntRect screen_rect() const;

private:
    PageClient& m_client;

    RefPtr<BrowsingContext> m_top_level_browsing_context;
    WeakPtr<BrowsingContext> m_focused_context;
};

class PageClient {
public:
    virtual bool is_multi_process() const = 0;
    virtual Gfx::Palette palette() const = 0;
    virtual Gfx::IntRect screen_rect() const = 0;
    virtual void page_did_set_document_in_top_level_browsing_context(DOM::Document*) { }
    virtual void page_did_change_title(const String&) { }
    virtual void page_did_start_loading(const URL&) { }
    virtual void page_did_finish_loading(const URL&) { }
    virtual void page_did_change_selection() { }
    virtual void page_did_request_cursor_change(Gfx::StandardCursor) { }
    virtual void page_did_request_context_menu(const Gfx::IntPoint&) { }
    virtual void page_did_request_link_context_menu(const Gfx::IntPoint&, const URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_request_image_context_menu(const Gfx::IntPoint&, const URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers, const Gfx::Bitmap*) { }
    virtual void page_did_click_link(const URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_middle_click_link(const URL&, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers) { }
    virtual void page_did_enter_tooltip_area(const Gfx::IntPoint&, const String&) { }
    virtual void page_did_leave_tooltip_area() { }
    virtual void page_did_hover_link(const URL&) { }
    virtual void page_did_unhover_link() { }
    virtual void page_did_invalidate(const Gfx::IntRect&) { }
    virtual void page_did_change_favicon(const Gfx::Bitmap&) { }
    virtual void page_did_layout() { }
    virtual void page_did_request_scroll(int) { }
    virtual void page_did_request_scroll_into_view(const Gfx::IntRect&) { }
    virtual void page_did_request_alert(const String&) { }
    virtual bool page_did_request_confirm(const String&) { return false; }
    virtual String page_did_request_prompt(const String&, const String&) { return {}; }
    virtual String page_did_request_cookie(const URL&, Cookie::Source) { return {}; }
    virtual void page_did_set_cookie(const URL&, const Cookie::ParsedCookie&, Cookie::Source) { }

protected:
    virtual ~PageClient() = default;
};

}
