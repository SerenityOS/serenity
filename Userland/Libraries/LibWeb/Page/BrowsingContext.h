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
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/TreeNode.h>

namespace Web {

class BrowsingContext : public TreeNode<BrowsingContext> {
public:
    static NonnullRefPtr<BrowsingContext> create_nested(HTML::BrowsingContextContainer& container, BrowsingContext& top_level_browsing_context) { return adopt_ref(*new BrowsingContext(container, top_level_browsing_context)); }
    static NonnullRefPtr<BrowsingContext> create(Page& page) { return adopt_ref(*new BrowsingContext(page)); }
    ~BrowsingContext();

    class ViewportClient {
    public:
        virtual ~ViewportClient() { }
        virtual void browsing_context_did_set_viewport_rect(Gfx::IntRect const&) = 0;
    };
    void register_viewport_client(ViewportClient&);
    void unregister_viewport_client(ViewportClient&);

    bool is_top_level() const { return this == &top_level_browsing_context(); }
    bool is_focused_context() const;

    DOM::Document const* document() const { return m_document; }
    DOM::Document* document() { return m_document; }

    void set_document(DOM::Document*);

    Page* page() { return m_page; }
    Page const* page() const { return m_page; }

    Gfx::IntSize const& size() const { return m_size; }
    void set_size(Gfx::IntSize const&);

    void set_needs_display(Gfx::IntRect const&);

    void set_viewport_scroll_offset(Gfx::IntPoint const&);
    Gfx::IntRect viewport_rect() const { return { m_viewport_scroll_offset, m_size }; }
    void set_viewport_rect(Gfx::IntRect const&);

    FrameLoader& loader() { return m_loader; }
    FrameLoader const& loader() const { return m_loader; }

    EventHandler& event_handler() { return m_event_handler; }
    EventHandler const& event_handler() const { return m_event_handler; }

    void scroll_to_anchor(String const&);

    BrowsingContext& top_level_browsing_context() { return *m_top_level_browsing_context; }
    BrowsingContext const& top_level_browsing_context() const { return *m_top_level_browsing_context; }

    HTML::BrowsingContextContainer* container() { return m_container; }
    HTML::BrowsingContextContainer const* container() const { return m_container; }

    Gfx::IntPoint to_top_level_position(Gfx::IntPoint const&);
    Gfx::IntRect to_top_level_rect(Gfx::IntRect const&);

    DOM::Position const& cursor_position() const { return m_cursor_position; }
    void set_cursor_position(DOM::Position);
    bool increment_cursor_position_offset();
    bool decrement_cursor_position_offset();

    bool cursor_blink_state() const { return m_cursor_blink_state; }

    String selected_text() const;
    void select_all();

    void did_edit(Badge<EditEventHandler>);

    void register_frame_nesting(URL const&);
    bool is_frame_nesting_allowed(URL const&) const;

    void set_frame_nesting_levels(HashMap<URL, size_t> frame_nesting_levels) { m_frame_nesting_levels = move(frame_nesting_levels); };
    HashMap<URL, size_t> const& frame_nesting_levels() const { return m_frame_nesting_levels; }

private:
    explicit BrowsingContext(Page&, HTML::BrowsingContextContainer*, BrowsingContext& top_level_browsing_context);
    explicit BrowsingContext(HTML::BrowsingContextContainer&, BrowsingContext& top_level_browsing_context);
    explicit BrowsingContext(Page&);

    void reset_cursor_blink_cycle();

    WeakPtr<Page> m_page;

    // NOTE: We expect there to always be a top-level browsing context as long as we exist.
    //       The use of WeakPtr is for safety in case we get something wrong.
    WeakPtr<BrowsingContext> m_top_level_browsing_context;

    FrameLoader m_loader;
    EventHandler m_event_handler;

    WeakPtr<HTML::BrowsingContextContainer> m_container;
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
