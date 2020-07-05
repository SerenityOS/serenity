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

#include <LibGUI/ScrollableWidget.h>
#include <LibGUI/Widget.h>

class WebContentClient;

class WebContentView final : public GUI::ScrollableWidget {
    C_OBJECT(WebContentView);

public:
    virtual ~WebContentView() override;

    void load(const URL&);

    Function<void(const String&)> on_title_change;

    void notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size);
    void notify_server_did_paint(Badge<WebContentClient>, i32 shbuf_id);
    void notify_server_did_invalidate_content_rect(Badge<WebContentClient>, const Gfx::IntRect&);
    void notify_server_did_change_selection(Badge<WebContentClient>);
    void notify_server_did_change_title(Badge<WebContentClient>, const String&);
    void notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect&);

private:
    WebContentView();

    // ^Widget
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    // ^ScrollableWidget
    virtual void did_scroll() override;

    void request_repaint();

    WebContentClient& client();

    RefPtr<WebContentClient> m_client;
    RefPtr<Gfx::Bitmap> m_bitmap;
};
