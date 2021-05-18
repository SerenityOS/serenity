/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibWeb/Fetch/FrameLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/TreeNode.h>

namespace Web {

class Frame : public TreeNode<Frame> {
public:
    static NonnullRefPtr<Frame> create_subframe(DOM::Element& host_element, Frame& main_frame) { return adopt_ref(*new Frame(host_element, main_frame)); }
    static NonnullRefPtr<Frame> create(Page& page) { return adopt_ref(*new Frame(page)); }
    ~Frame();

    class ViewportClient {
    public:
        virtual ~ViewportClient() { }
        virtual void frame_did_set_viewport_rect(const Gfx::IntRect&) = 0;
    };
    void register_viewport_client(ViewportClient&);
    void unregister_viewport_client(ViewportClient&);

    bool is_main_frame() const { return this == &m_main_frame; }
    bool is_focused_frame() const;

    const DOM::Document* document() const { return m_document; }
    DOM::Document* document() { return m_document; }

    void set_document(DOM::Document*);

    Page* page() { return m_page; }
    const Page* page() const { return m_page; }

    const Gfx::IntSize& size() const { return m_size; }
    void set_size(const Gfx::IntSize&);

    void set_needs_display(const Gfx::IntRect&);

    void set_viewport_scroll_offset(const Gfx::IntPoint&);
    Gfx::IntRect viewport_rect() const { return { m_viewport_scroll_offset, m_size }; }
    void set_viewport_rect(const Gfx::IntRect&);

    FrameLoader& loader() { return m_loader; }
    const FrameLoader& loader() const { return m_loader; }

    EventHandler& event_handler() { return m_event_handler; }
    const EventHandler& event_handler() const { return m_event_handler; }

    void scroll_to_anchor(const String&);

    Frame& main_frame() { return m_main_frame; }
    const Frame& main_frame() const { return m_main_frame; }

    DOM::Element* host_element() { return m_host_element; }
    const DOM::Element* host_element() const { return m_host_element; }

    Gfx::IntPoint to_main_frame_position(const Gfx::IntPoint&);
    Gfx::IntRect to_main_frame_rect(const Gfx::IntRect&);

    const DOM::Position& cursor_position() const { return m_cursor_position; }
    void set_cursor_position(DOM::Position);
    bool increment_cursor_position_offset();
    bool decrement_cursor_position_offset();

    bool cursor_blink_state() const { return m_cursor_blink_state; }

    String selected_text() const;

    void did_edit(Badge<EditEventHandler>);

    void register_frame_nesting(URL const&);
    bool is_frame_nesting_allowed(URL const&) const;

    void set_frame_nesting_levels(const HashMap<URL, size_t> frame_nesting_levels) { m_frame_nesting_levels = move(frame_nesting_levels); };
    HashMap<URL, size_t> const& frame_nesting_levels() const { return m_frame_nesting_levels; }

private:
    explicit Frame(DOM::Element& host_element, Frame& main_frame);
    explicit Frame(Page&);

    void reset_cursor_blink_cycle();

    void setup();

    WeakPtr<Page> m_page;
    Frame& m_main_frame;

    FrameLoader m_loader;
    EventHandler m_event_handler;

    WeakPtr<DOM::Element> m_host_element;
    RefPtr<DOM::Document> m_document;
    Gfx::IntSize m_size;
    Gfx::IntPoint m_viewport_scroll_offset;

    DOM::Position m_cursor_position;
    RefPtr<Core::Timer> m_cursor_blink_timer;
    bool m_cursor_blink_state { false };

    HashTable<ViewportClient*> m_viewport_clients;

    HashMap<URL, size_t> m_frame_nesting_levels;
};

}
