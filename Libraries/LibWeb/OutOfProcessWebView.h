/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/URL.h>
#include <LibGUI/ScrollableWidget.h>
#include <LibGUI/Widget.h>
#include <LibWeb/WebViewHooks.h>

namespace Web {

class WebContentClient;

class OutOfProcessWebView final
    : public GUI::ScrollableWidget
    , public Web::WebViewHooks {
    C_OBJECT(OutOfProcessWebView);

public:
    virtual ~OutOfProcessWebView() override;

    URL url() const { return m_url; }
    void load(const URL&);

    void load_html(const StringView&, const URL&);
    void load_empty_document();

    void notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size);
    void notify_server_did_paint(Badge<WebContentClient>, i32 shbuf_id);
    void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_change_selection(Badge<WebContentClient>);
    void notify_server_did_change_title(Badge<WebContentClient>, const String&);
    void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_hover_link(Badge<WebContentClient>, const URL&);
    void notify_server_did_unhover_link(Badge<WebContentClient>);
    void notify_server_did_click_link(Badge<WebContentClient>, const URL&, const String& target, unsigned modifiers);
    void notify_server_did_middle_click_link(Badge<WebContentClient>, const URL&, const String& target, unsigned modifiers);
    void notify_server_did_start_loading(Badge<WebContentClient>, const URL&);
    void notify_server_did_request_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&);
    void notify_server_did_request_link_context_menu(Badge<WebContentClient>, const Gfx::IntPoint&, const URL&, const String& target, unsigned modifiers);
    void notify_server_did_request_alert(Badge<WebContentClient>, const String& message);

private:
    OutOfProcessWebView();

    // ^Widget
    virtual bool accepts_focus() const override { return true; }
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void theme_change_event(GUI::ThemeChangeEvent&) override;

    // ^ScrollableWidget
    virtual void did_scroll() override;

    void request_repaint();

    WebContentClient& client();

    URL m_url;

    RefPtr<WebContentClient> m_client;
    RefPtr<Gfx::Bitmap> m_front_bitmap;
    RefPtr<Gfx::Bitmap> m_back_bitmap;
};

}
