/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    Web::Frame& main_frame() { return *m_main_frame; }
    const Web::Frame& main_frame() const { return *m_main_frame; }

    Web::Frame& focused_frame();
    const Web::Frame& focused_frame() const { return const_cast<Page*>(this)->focused_frame(); }

    void set_focused_frame(Badge<EventHandler>, Frame&);

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

    RefPtr<Frame> m_main_frame;
    WeakPtr<Frame> m_focused_frame;
};

class PageClient {
public:
    virtual bool is_multi_process() const = 0;
    virtual Gfx::Palette palette() const = 0;
    virtual Gfx::IntRect screen_rect() const = 0;
    virtual void page_did_set_document_in_main_frame(DOM::Document*) { }
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
};

}
