/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/TreeNode.h>

namespace Web {

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create_subframe(DOM::Element& host_element, Frame& main_frame) { return adopt(*new Frame(host_element, main_frame)); }
    static NonnullRefPtr<Frame> create(Page& page) { return adopt(*new Frame(page)); }
    ~Frame();

    bool is_main_frame() const { return this == &m_main_frame; }
    bool is_focused_frame() const;

    const DOM::Document* document() const { return m_document; }
    DOM::Document* document() { return m_document; }

    void set_document(DOM::Document*);

    Page& page() { return m_page; }
    const Page& page() const { return m_page; }

    const Gfx::IntSize& size() const { return m_size; }
    void set_size(const Gfx::IntSize&);

    void set_needs_display(const Gfx::IntRect&);

    void set_viewport_rect(const Gfx::IntRect&);
    Gfx::IntRect viewport_rect() const { return m_viewport_rect; }

    void did_scroll(Badge<InProcessWebView>);

    FrameLoader& loader() { return m_loader; }
    const FrameLoader& loader() const { return m_loader; }

    EventHandler& event_handler() { return m_event_handler; }
    const EventHandler& event_handler() const { return m_event_handler; }

    void scroll_to(const Gfx::IntPoint&);
    void scroll_to_anchor(const String&);

    Frame& main_frame() { return m_main_frame; }
    const Frame& main_frame() const { return m_main_frame; }

    DOM::Element* host_element() { return m_host_element; }
    const DOM::Element* host_element() const { return m_host_element; }

    Gfx::IntPoint to_main_frame_position(const Gfx::IntPoint&);
    Gfx::IntRect to_main_frame_rect(const Gfx::IntRect&);

    DOM::Position& cursor_position() { return m_cursor_position; }
    const DOM::Position& cursor_position() const { return m_cursor_position; }
    void set_cursor_position(const DOM::Position&);

    bool cursor_blink_state() const { return m_cursor_blink_state; }

    String selected_text() const;

private:
    explicit Frame(DOM::Element& host_element, Frame& main_frame);
    explicit Frame(Page&);

    void setup();

    Page& m_page;
    Frame& m_main_frame;

    FrameLoader m_loader;
    EventHandler m_event_handler;

    WeakPtr<DOM::Element> m_host_element;
    RefPtr<DOM::Document> m_document;
    Gfx::IntSize m_size;
    Gfx::IntRect m_viewport_rect;

    DOM::Position m_cursor_position;
    RefPtr<Core::Timer> m_cursor_blink_timer;
    bool m_cursor_blink_state { false };
};

}
