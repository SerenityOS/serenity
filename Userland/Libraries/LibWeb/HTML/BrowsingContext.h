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
#include <LibWeb/HTML/AbstractBrowsingContext.h>
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

class BrowsingContext final
    : public AbstractBrowsingContext
    , public Weakable<BrowsingContext> {
    JS_CELL(BrowsingContext, AbstractBrowsingContext);

public:
    struct BrowsingContextAndDocument {
        JS::NonnullGCPtr<BrowsingContext> browsing_context;
        JS::NonnullGCPtr<DOM::Document> document;
    };

    static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_browsing_context_and_document(Page& page, JS::GCPtr<DOM::Document> creator, JS::GCPtr<DOM::Element> embedder, JS::NonnullGCPtr<BrowsingContextGroup> group);
    static WebIDL::ExceptionOr<BrowsingContextAndDocument> create_a_new_auxiliary_browsing_context_and_document(Page& page, JS::NonnullGCPtr<HTML::BrowsingContext> opener);

    virtual ~BrowsingContext() override;

    JS::NonnullGCPtr<HTML::TraversableNavigable> top_level_traversable() const;

    JS::GCPtr<BrowsingContext> parent() const { return m_parent; }
    void append_child(JS::NonnullGCPtr<BrowsingContext>);
    void remove_child(JS::NonnullGCPtr<BrowsingContext>);
    JS::GCPtr<BrowsingContext> first_child() const;
    JS::GCPtr<BrowsingContext> next_sibling() const;

    bool is_ancestor_of(BrowsingContext const&) const;

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
    void for_each_child(Callback callback) const
    {
        for (auto node = first_child(); node; node = node->next_sibling())
            callback(*node);
    }

    template<typename Callback>
    void for_each_child(Callback callback)
    {
        for (auto node = first_child(); node; node = node->next_sibling())
            callback(*node);
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

    virtual HTML::WindowProxy* window_proxy() override;
    virtual HTML::WindowProxy const* window_proxy() const override;

    void set_window_proxy(JS::GCPtr<WindowProxy>);

    HTML::Window* active_window();
    HTML::Window const* active_window() const;

    Page* page() { return m_page; }
    Page const* page() const { return m_page; }

    Web::EventHandler& event_handler() { return m_event_handler; }
    Web::EventHandler const& event_handler() const { return m_event_handler; }

    void scroll_to(CSSPixelPoint);

    JS::GCPtr<BrowsingContext> top_level_browsing_context() const;

    enum class WindowType {
        ExistingOrNone,
        NewAndUnrestricted,
        NewWithNoOpener,
    };

    struct ChosenBrowsingContext {
        JS::GCPtr<AbstractBrowsingContext> browsing_context;
        WindowType window_type;
    };

    ChosenBrowsingContext choose_a_browsing_context(StringView name, TokenizedFeature::NoOpener no_opener, ActivateTab = ActivateTab::Yes);

    HTML::NavigableContainer* container() { return m_container; }
    HTML::NavigableContainer const* container() const { return m_container; }

    CSSPixelPoint to_top_level_position(CSSPixelPoint);
    CSSPixelRect to_top_level_rect(CSSPixelRect const&);

    DOM::Position const& cursor_position() const { return m_cursor_position; }
    void set_cursor_position(DOM::Position);
    bool increment_cursor_position_offset();
    bool decrement_cursor_position_offset();

    bool cursor_blink_state() const { return m_cursor_blink_state; }

    DeprecatedString selected_text() const;
    void select_all();

    void did_edit(Badge<EditEventHandler>);

    bool has_a_rendering_opportunity() const;

    JS::GCPtr<DOM::Node> currently_focused_area();

    Vector<JS::NonnullGCPtr<SessionHistoryEntry>>& session_history() { return m_session_history; }
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> const& session_history() const { return m_session_history; }

    size_t session_history_index() const { return *m_session_history_index; }

    // https://html.spec.whatwg.org/multipage/dom.html#still-on-its-initial-about:blank-document
    bool still_on_its_initial_about_blank_document() const;

    BrowsingContextGroup* group();
    void set_group(BrowsingContextGroup*);

    // https://html.spec.whatwg.org/multipage/browsers.html#bcg-remove
    void remove();

    // https://html.spec.whatwg.org/multipage/origin.html#one-permitted-sandboxed-navigator
    BrowsingContext const* the_one_permitted_sandboxed_navigator() const;

    bool has_been_discarded() const { return m_has_been_discarded; }

    Optional<AK::URL> const& creator_url() const { return m_creator_url; }

    virtual String const& window_handle() const override { return m_window_handle; }
    virtual void set_window_handle(String handle) override { m_window_handle = move(handle); }

private:
    explicit BrowsingContext(Page&, HTML::NavigableContainer*);

    virtual void visit_edges(Cell::Visitor&) override;

    void reset_cursor_blink_cycle();

    void scroll_offset_did_change();

    WeakPtr<Page> m_page;

    Web::EventHandler m_event_handler;

    // https://html.spec.whatwg.org/multipage/history.html#current-entry
    SessionHistoryEntry& current_entry() { return m_session_history[*m_session_history_index]; }
    SessionHistoryEntry const& current_entry() const { return m_session_history[*m_session_history_index]; }
    Optional<size_t> m_session_history_index { 0 };

    // https://html.spec.whatwg.org/multipage/history.html#session-history
    Vector<JS::NonnullGCPtr<SessionHistoryEntry>> m_session_history;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-url
    Optional<AK::URL> m_creator_url;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-base-url
    Optional<AK::URL> m_creator_base_url;

    // https://html.spec.whatwg.org/multipage/browsers.html#creator-origin
    Optional<HTML::Origin> m_creator_origin;

    JS::GCPtr<HTML::NavigableContainer> m_container;

    // https://w3c.github.io/webdriver/#dfn-window-handles
    String m_window_handle;

    // https://html.spec.whatwg.org/multipage/browsers.html#browsing-context
    JS::GCPtr<HTML::WindowProxy> m_window_proxy;

    DOM::Position m_cursor_position;
    RefPtr<Core::Timer> m_cursor_blink_timer;
    bool m_cursor_blink_state { false };

    DeprecatedString m_name;

    // https://html.spec.whatwg.org/multipage/browsers.html#tlbc-group
    JS::GCPtr<BrowsingContextGroup> m_group;

    JS::GCPtr<BrowsingContext> m_parent;
    JS::GCPtr<BrowsingContext> m_first_child;
    JS::GCPtr<BrowsingContext> m_last_child;
    JS::GCPtr<BrowsingContext> m_next_sibling;
    JS::GCPtr<BrowsingContext> m_previous_sibling;

    bool m_has_been_discarded { false };
};

HTML::Origin determine_the_origin(AK::URL const& url, SandboxingFlagSet sandbox_flags, Optional<HTML::Origin> source_origin);

SandboxingFlagSet determine_the_creation_sandboxing_flags(BrowsingContext const&, JS::GCPtr<DOM::Element> embedder);

// FIXME: Find a better home for this
bool url_matches_about_blank(AK::URL const& url);

}
