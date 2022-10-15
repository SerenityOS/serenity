/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefPtr.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/VisibilityState.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWeb/TreeNode.h>

namespace Web::HTML {

class BrowsingContext : public TreeNode<BrowsingContext> {
public:
    static NonnullRefPtr<BrowsingContext> create_a_new_browsing_context(Page&, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, BrowsingContextGroup&);
    static NonnullRefPtr<BrowsingContext> create_a_new_top_level_browsing_context(Page&);

    ~BrowsingContext();

    class ViewportClient {
    public:
        virtual ~ViewportClient() = default;
        virtual void browsing_context_did_set_viewport_rect(Gfx::IntRect const&) = 0;
    };
    void register_viewport_client(ViewportClient&);
    void unregister_viewport_client(ViewportClient&);

    bool is_top_level() const;
    bool is_focused_context() const;

    DOM::Document const* active_document() const;
    DOM::Document* active_document();

    void set_active_document(JS::NonnullGCPtr<DOM::Document>);

    HTML::WindowProxy* window_proxy();
    HTML::WindowProxy const* window_proxy() const;

    HTML::Window* active_window();
    HTML::Window const* active_window() const;

    Page* page() { return m_page; }
    Page const* page() const { return m_page; }

    Gfx::IntSize const& size() const { return m_size; }
    void set_size(Gfx::IntSize const&);

    void set_needs_display();
    void set_needs_display(Gfx::IntRect const&);

    Gfx::IntPoint const& viewport_scroll_offset() const { return m_viewport_scroll_offset; }
    Gfx::IntRect viewport_rect() const { return { m_viewport_scroll_offset, m_size }; }
    void set_viewport_rect(Gfx::IntRect const&);

    FrameLoader& loader() { return m_loader; }
    FrameLoader const& loader() const { return m_loader; }

    Web::EventHandler& event_handler() { return m_event_handler; }
    Web::EventHandler const& event_handler() const { return m_event_handler; }

    void scroll_to(Gfx::IntPoint const&);
    void scroll_to_anchor(String const&);

    BrowsingContext& top_level_browsing_context()
    {
        BrowsingContext* context = this;
        while (context->parent())
            context = context->parent();
        return *context;
    }

    BrowsingContext const& top_level_browsing_context() const { return const_cast<BrowsingContext*>(this)->top_level_browsing_context(); }

    BrowsingContext* choose_a_browsing_context(StringView name, bool noopener);

    size_t document_tree_child_browsing_context_count() const;

    bool is_child_of(BrowsingContext const&) const;

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

    void register_frame_nesting(AK::URL const&);
    bool is_frame_nesting_allowed(AK::URL const&) const;

    void set_frame_nesting_levels(HashMap<AK::URL, size_t> frame_nesting_levels) { m_frame_nesting_levels = move(frame_nesting_levels); };
    HashMap<AK::URL, size_t> const& frame_nesting_levels() const { return m_frame_nesting_levels; }

    DOM::Document* container_document();
    DOM::Document const* container_document() const;

    bool has_a_rendering_opportunity() const;

    JS::GCPtr<DOM::Node> currently_focused_area();

    String const& name() const { return m_name; }
    void set_name(String const& name) { m_name = name; }

    Vector<SessionHistoryEntry>& session_history() { return m_session_history; }
    Vector<SessionHistoryEntry> const& session_history() const { return m_session_history; }
    size_t session_history_index() const { return *m_session_history_index; }

    // https://html.spec.whatwg.org/multipage/dom.html#still-on-its-initial-about:blank-document
    bool still_on_its_initial_about_blank_document() const;

    BrowsingContextGroup* group();
    void set_group(BrowsingContextGroup*);

    // https://html.spec.whatwg.org/multipage/browsers.html#bcg-remove
    void remove();

    // https://html.spec.whatwg.org/multipage/browsers.html#allowed-to-navigate
    bool is_allowed_to_navigate(BrowsingContext const&) const;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate
    WebIDL::ExceptionOr<void> navigate(
        NonnullRefPtr<Fetch::Infrastructure::Request> resource,
        BrowsingContext& source_browsing_context,
        bool exceptions_enabled = false,
        HistoryHandlingBehavior history_handling = HistoryHandlingBehavior::Default,
        Optional<PolicyContainer> history_policy_container = {},
        String navigation_type = "other",
        Optional<String> navigation_id = {},
        Function<void(NonnullRefPtr<Fetch::Infrastructure::Response>)> process_response_end_of_body = {});

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate-fragid
    WebIDL::ExceptionOr<void> navigate_to_a_fragment(AK::URL const&, HistoryHandlingBehavior, String navigation_id);

    // https://html.spec.whatwg.org/multipage/origin.html#one-permitted-sandboxed-navigator
    BrowsingContext const* the_one_permitted_sandboxed_navigator() const;

    // https://html.spec.whatwg.org/multipage/browsing-the-web.html#traverse-the-history
    WebIDL::ExceptionOr<void> traverse_the_history(size_t entry_index, HistoryHandlingBehavior = HistoryHandlingBehavior::Default, bool explicit_history_navigation = false);

    Vector<JS::Handle<DOM::Document>> document_family() const;
    bool document_family_contains(DOM::Document const&) const;

    VisibilityState system_visibility_state() const;
    void set_system_visibility_state(VisibilityState);

    // https://html.spec.whatwg.org/multipage/window-object.html#a-browsing-context-is-discarded
    void discard();

    // https://html.spec.whatwg.org/multipage/window-object.html#close-a-browsing-context
    void close();

private:
    explicit BrowsingContext(Page&, HTML::BrowsingContextContainer*);

    void reset_cursor_blink_cycle();

    void scroll_offset_did_change();

    WeakPtr<Page> m_page;

    FrameLoader m_loader;
    Web::EventHandler m_event_handler;

    // https://html.spec.whatwg.org/multipage/history.html#current-entry
    SessionHistoryEntry& current_entry() { return m_session_history[*m_session_history_index]; }
    SessionHistoryEntry const& current_entry() const { return m_session_history[*m_session_history_index]; }
    Optional<size_t> m_session_history_index { 0 };

    // https://html.spec.whatwg.org/multipage/history.html#session-history
    Vector<SessionHistoryEntry> m_session_history;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-url
    Optional<AK::URL> m_creator_url;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-base-url
    Optional<AK::URL> m_creator_base_url;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-origin
    Optional<HTML::Origin> m_creator_origin;

    WeakPtr<HTML::BrowsingContextContainer> m_container;
    Gfx::IntSize m_size;
    Gfx::IntPoint m_viewport_scroll_offset;

    // https://html.spec.whatwg.org/multipage/browsers.html#browsing-context
    JS::Handle<HTML::WindowProxy> m_window_proxy;

    DOM::Position m_cursor_position;
    RefPtr<Platform::Timer> m_cursor_blink_timer;
    bool m_cursor_blink_state { false };

    HashTable<ViewportClient*> m_viewport_clients;

    HashMap<AK::URL, size_t> m_frame_nesting_levels;
    String m_name;

    // https://html.spec.whatwg.org/multipage/browsers.html#tlbc-group
    RefPtr<BrowsingContextGroup> m_group;

    // https://html.spec.whatwg.org/multipage/interaction.html#system-visibility-state
    VisibilityState m_system_visibility_state { VisibilityState::Hidden };
};

HTML::Origin determine_the_origin(BrowsingContext const& browsing_context, Optional<AK::URL> url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> invocation_origin);

}
