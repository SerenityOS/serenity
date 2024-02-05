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
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/HTML/ActivateTab.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/NavigableContainer.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/SandboxingFlagSet.h>
#include <LibWeb/HTML/SessionHistoryEntry.h>
#include <LibWeb/HTML/TokenizedFeatures.h>
#include <LibWeb/HTML/VisibilityState.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWeb/TreeNode.h>

namespace Web::HTML {

class BrowsingContext final : public JS::Cell
    , public Weakable<BrowsingContext> {
    JS_CELL(BrowsingContext, JS::Cell);
    JS_DECLARE_ALLOCATOR(BrowsingContext);

public:
    struct BrowsingContextAndDocument {
        JS::NonnullGCPtr<BrowsingContext> browsing_context;
        JS::NonnullGCPtr<DOM::Document> document;
    };

    static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_browsing_context_and_document(JS::NonnullGCPtr<Page> page, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, JS::NonnullGCPtr<BrowsingContextGroup> group);
    static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_auxiliary_browsing_context_and_document(JS::NonnullGCPtr<Page> page, JS::NonnullGCPtr<HTML::BrowsingContext> opener);

    virtual ~BrowsingContext() override;

    JS::NonnullGCPtr<HTML::TraversableNavigable> top_level_traversable() const;

    JS::GCPtr<BrowsingContext> parent() const { return m_parent; }
    JS::GCPtr<BrowsingContext> first_child() const;
    JS::GCPtr<BrowsingContext> next_sibling() const;

    bool is_ancestor_of(BrowsingContext const&) const;
    bool is_familiar_with(BrowsingContext const&) const;

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback) const
    {
        if (callback(*this) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_inclusive_subtree(Callback callback)
    {
        if (callback(*this) == IterationDecision::Break)
            return IterationDecision::Break;
        for (auto child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback) const
    {
        for (auto child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    template<typename Callback>
    IterationDecision for_each_in_subtree(Callback callback)
    {
        for (auto child = first_child(); child; child = child->next_sibling()) {
            if (child->for_each_in_inclusive_subtree(callback) == IterationDecision::Break)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    }

    bool is_top_level() const;
    bool is_focused_context() const;

    DOM::Document const* active_document() const;
    DOM::Document* active_document();

    HTML::WindowProxy* window_proxy();
    HTML::WindowProxy const* window_proxy() const;

    void set_window_proxy(JS::GCPtr<WindowProxy>);

    HTML::Window* active_window();
    HTML::Window const* active_window() const;

    Page& page() { return m_page; }
    Page const& page() const { return m_page; }

    Web::EventHandler& event_handler() { return m_event_handler; }
    Web::EventHandler const& event_handler() const { return m_event_handler; }

    JS::GCPtr<BrowsingContext> top_level_browsing_context() const;

    JS::GCPtr<DOM::Position> cursor_position() const { return m_cursor_position; }
    void set_cursor_position(JS::NonnullGCPtr<DOM::Position>);
    bool increment_cursor_position_offset();
    bool decrement_cursor_position_offset();

    bool cursor_blink_state() const { return m_cursor_blink_state; }

    String selected_text() const;
    void select_all();

    void did_edit(Badge<EditEventHandler>);

    JS::GCPtr<DOM::Node> currently_focused_area();

    BrowsingContextGroup* group();
    void set_group(BrowsingContextGroup*);

    // https://html.spec.whatwg.org/multipage/browsers.html#bcg-remove
    void remove();

    // https://html.spec.whatwg.org/multipage/origin.html#one-permitted-sandboxed-navigator
    BrowsingContext const* the_one_permitted_sandboxed_navigator() const;

    bool has_navigable_been_destroyed() const;

    JS::GCPtr<BrowsingContext> opener_browsing_context() const { return m_opener_browsing_context; }
    void set_opener_browsing_context(JS::GCPtr<BrowsingContext> browsing_context) { m_opener_browsing_context = browsing_context; }

    void set_is_popup(TokenizedFeature::Popup is_popup) { m_is_popup = is_popup; }

private:
    explicit BrowsingContext(JS::NonnullGCPtr<Page>);

    virtual void visit_edges(Cell::Visitor&) override;

    void reset_cursor_blink_cycle();

    JS::NonnullGCPtr<Page> m_page;

    // FIXME: Move EventHandler to Navigable
    Web::EventHandler m_event_handler;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#browsing-context
    JS::GCPtr<HTML::WindowProxy> m_window_proxy;

    // https://html.spec.whatwg.org/multipage/browsers.html#opener-browsing-context
    JS::GCPtr<BrowsingContext> m_opener_browsing_context;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#opener-origin-at-creation
    Optional<HTML::Origin> m_opener_origin_at_creation;

    // https://html.spec.whatwg.org/multipage/browsers.html#is-popup
    TokenizedFeature::Popup m_is_popup { TokenizedFeature::Popup::No };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#is-auxiliary
    bool m_is_auxiliary { false };

    // https://html.spec.whatwg.org/multipage/document-sequences.html#browsing-context-initial-url
    Optional<AK::URL> m_initial_url;

    // https://html.spec.whatwg.org/multipage/document-sequences.html#virtual-browsing-context-group-id
    u64 m_virtual_browsing_context_group_id = { 0 };

    // FIXME: Move cursor tracking to Navigable
    JS::GCPtr<DOM::Position> m_cursor_position;
    RefPtr<Core::Timer> m_cursor_blink_timer;
    bool m_cursor_blink_state { false };

    // https://html.spec.whatwg.org/multipage/browsers.html#tlbc-group
    JS::GCPtr<BrowsingContextGroup> m_group;

    JS::GCPtr<BrowsingContext> m_parent;
    JS::GCPtr<BrowsingContext> m_first_child;
    JS::GCPtr<BrowsingContext> m_last_child;
    JS::GCPtr<BrowsingContext> m_next_sibling;
    JS::GCPtr<BrowsingContext> m_previous_sibling;
};

HTML::Origin determine_the_origin(AK::URL const& url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> source_origin);

SandboxingFlagSet determine_the_creation_sandboxing_flags(BrowsingContext const&, JS::GCPtr<DOM::Element> embedder);

// FIXME: Find a better home for these
bool url_matches_about_blank(AK::URL const& url);
bool url_matches_about_srcdoc(AK::URL const& url);

}
